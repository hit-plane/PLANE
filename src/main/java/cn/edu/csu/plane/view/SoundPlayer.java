package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.SoundEvent;
import cn.edu.csu.plane.util.GameConfig;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.media.AudioClip;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 音效播放器（F14）：把 {@link SoundEvent} 播成 {@code resource/sounds/} 下的素材。
 *
 * <p>这是"View 层新增音频播放模块、对应事件触发播放"的落地处：事件由模型/界面发出，
 * 素材与事件怎么对应集中在 {@link #FILES} 一张表里，换素材不用改别的代码。</p>
 *
 * <p>素材与贴图一样放在项目根目录的 {@code resource/sounds}（不在 src/main/resources），
 * 因此用文件路径加载，保证 IntelliJ 与 mvn javafx:run（工作目录均为项目根）都能找到。</p>
 *
 * <p><b>静音与容错</b>：总开关是配置项 {@link GameConfig#SOUND_ENABLED}；单元测试另外通过
 * 系统属性 {@code -Dsound.disabled=true} 全局静音（见 pom 的 surefire 配置），
 * 两者是"与"的关系。素材缺失或音频子系统不可用时只打一行日志、这一声不响，
 * 绝不让游戏因为放不出声而崩掉——与 {@link cn.edu.csu.plane.util.AssetLoader}
 * 找不到贴图只告警的处理口径一致。</p>
 *
 * <p>用 {@link AudioClip} 而不是 MediaPlayer：音效都是短促的一次性声音，
 * AudioClip 可以直接叠加播放（连续命中时不会互相打断），也不需要在视图里持有播放器对象。</p>
 *
 * <p><b>延迟</b>：{@link #play} 是懒加载的，第一次响某一声时才解码——读盘、解码、
 * 打开输出设备与播放管线都挤在那一瞬间，而且就发生在 JavaFX 应用线程上（见 {@link #preload}）。
 * 所以装配层要在开局前调一次 {@link #preload()}，把这份成本挪到后台、挪到玩家还没动手的时候。</p>
 *
 * <p><b>音量</b>：全局音量一个（{@link GameConfig#SOUND_VOLUME}），单个事件还能各自再乘一个倍率
 * （配置项 {@code sound.volume.<事件名>}，见 {@link GameConfig#soundVolumeOf}），最终在
 * {@link #volumeOf} 里夹到 0.0～1.0。分的理由是"有几声天生该轻一点"：击中/击毁敌机用的是
 * 同一份音频，又是一局里响得最密的，配得比别的音效轻才不会把通关、拾取这些更要紧的声音盖住。</p>
 */
public final class SoundPlayer {

    /** 素材目录：与 AssetLoader 同款相对路径，相对的是"项目根"。 */
    private static final String BASE = "resource" + File.separator + "sounds" + File.separator;

    /**
     * 事件 → 素材文件名。全平铺在 {@code resource/sounds} 下，用英文名，代码里不出现中文路径。
     *
     * <p><b>必须是 WAV/MP3/AAC/AIFF</b>：JavaFX 的媒体引擎（{@link AudioClip} 与 MediaPlayer 共用）
     * 不支持 Ogg Vorbis，喂它 .ogg 会在构造时抛
     * {@code MediaException: MEDIA_UNSUPPORTED : Unrecognized file signature}。
     * 候选素材原本是 ogg，已统一转成 16bit PCM WAV 后落位；换素材时也要照这个格式来。</p>
     *
     * <p>{@link SoundEvent#ENEMY_HIT} 与 {@link SoundEvent#ENEMY_DESTROYED} 目前是同一份音频
     * （候选素材里两边放的就是同一个 bit.ogg），仍按两个文件落位：将来想把其中一个换成独立的
     * 爆炸音，替换文件即可，枚举与模型都不用动。两者在运行期是互斥触发的（命中即死只发击毁），
     * 所以不会出现同一份音频叠成双倍音量。</p>
     *
     * <p>这两声的音量也另配得更轻（{@code sound.volume.enemy_hit} / {@code sound.volume.enemy_destroyed}）：
     * 一局里响得最密的就是它们，压在别的音效下面才不吵。</p>
     */
    private static final Map<SoundEvent, String> FILES = Map.ofEntries(
            Map.entry(SoundEvent.MENU_CLICK, "menu_click.wav"),
            Map.entry(SoundEvent.ENEMY_HIT, "enemy_hit.wav"),
            Map.entry(SoundEvent.ENEMY_DESTROYED, "enemy_destroyed.wav"),
            Map.entry(SoundEvent.ITEM_PICKUP, "item_pickup.wav"),
            Map.entry(SoundEvent.BOMB, "bomb.wav"),
            Map.entry(SoundEvent.PLAYER_HIT, "player_hit.wav"),
            Map.entry(SoundEvent.PLAYER_CRASH, "player_crash.wav"),
            Map.entry(SoundEvent.SHIELD_BLOCK, "shield_block.wav"),
            Map.entry(SoundEvent.LEVEL_UP, "level_up.wav"),
            Map.entry(SoundEvent.VICTORY, "victory.wav"),
            Map.entry(SoundEvent.DEFEAT, "defeat.wav"));

    /**
     * 按文件名缓存的已解码音频。
     *
     * <p>是 {@link ConcurrentHashMap} 而不是 HashMap：预加载线程与 FX 线程会同时摸它
     * （见 {@link #preload}）。竞态下最坏是同一个文件被解码两遍——各得到一个能用的对象，
     * 谁先放进去都一样，不影响结果。</p>
     */
    private static final Map<String, AudioClip> CLIPS = new ConcurrentHashMap<>();

    /** 解码失败过的素材：记一笔就不再重试，免得每次事件都读一次盘、刷一行日志。 */
    private static final Set<String> UNLOADABLE = ConcurrentHashMap.newKeySet();

    /** 整个音频子系统不可用（缺 javafx-media 原生库等）时置位，之后一律不再尝试。 */
    private static volatile boolean soundSystemUnavailable;

    /** 预加载是否已经排上（或已跑完）：只允许起一条加载线程，重复调用幂等。 */
    private static final AtomicBoolean PRELOAD_STARTED = new AtomicBoolean();

    private SoundPlayer() {
    }

    /**
     * 按事件放一声。已静音、事件没有对应素材、或素材加载失败时静默跳过。
     *
     * <p>音量按事件取（见 {@link #volumeOf}）：全局音量是同一个，各事件还能各自轻重，
     * 于是"打中敌机"这种一局响上百次的声音可以单独压轻，不必去动其它音效。</p>
     */
    public static void play(SoundEvent event) {
        if (event == null || !isEnabled()) {
            return;
        }
        AudioClip clip = clipFor(event);
        if (clip != null) {
            clip.play(volumeOf(event));
        }
    }

    /**
     * 启动预加载：在后台线程把全部素材解码好，再回 FX 线程把播放管线暖起来。
     *
     * <p><b>为什么要有这个方法</b>：{@link #play} 是懒加载——某一声第一次响的时候才
     * {@code new AudioClip(...)}。那一步里最贵的是首次初始化媒体引擎并打开输出设备
     * （本机实测约 150 ms，与素材大小无关：紧接着解码一份 6 倍大的素材只要 2 ms），
     * 而它偏偏发生在 JavaFX 应用线程上、发生在"第一次击毁敌机""第一次按下开始按钮"
     * 这种最不该卡的瞬间。听起来就是<b>每种音效头一次响都慢半拍</b>，重的时候画面还会跟着顿一下。
     * 开局前先在后台把这份成本付掉，之后每一声就只剩 {@code play()} 本身的延迟。</p>
     *
     * <p>由 JavaFX 应用线程调用（预热要回到 FX 线程），但绝不阻塞它：解码全在
     * {@code sound-preload} 守护线程上跑。已静音或没有工具箱（单元测试）时直接返回，
     * 连线程都不起；重复调用幂等。</p>
     */
    public static void preload() {
        if (!isEnabled() || !PRELOAD_STARTED.compareAndSet(false, true)) {
            return;
        }
        Thread loader = new Thread(SoundPlayer::loadAllThenWarmUp, "sound-preload");
        loader.setDaemon(true);
        loader.start();
    }

    /** 加载线程：逐个解码，完事后回 FX 线程预热。 */
    private static void loadAllThenWarmUp() {
        for (String fileName : FILES.values()) {
            if (soundSystemUnavailable) {
                return;   // 整个音频子系统都废了，剩下的不用再试（loadClip 已置位并打过日志）
            }
            clipOf(fileName);
        }
        try {
            Platform.runLater(SoundPlayer::warmUp);
        } catch (IllegalStateException noToolkit) {
            // 没有 JavaFX 工具箱（无界面环境）：不预热就是了，往后照常按需加载、按需播放
        }
    }

    /**
     * 静音预热：把每个已加载素材都以 0 音量各放一次。
     *
     * <p>光把 {@code AudioClip} 构造出来还不算完——输出设备和播放管线是第一次
     * {@code play()} 时才真正打开的。这里趁玩家还没开局先把这份"冷启动"成本付掉，
     * 于是玩家听到的第一声就已经是热态的管线了。</p>
     *
     * <p>音量 0，听不见；<b>故意不去 {@code stop()}</b>：stop 停的是"这一份素材名下的全部播放"，
     * 万一同一时刻真有别的一声在响会被一起掐掉。素材都只有几秒，各自放完自停。</p>
     */
    private static void warmUp() {
        for (AudioClip clip : CLIPS.values()) {
            try {
                clip.play(0.0);
            } catch (RuntimeException | LinkageError e) {
                // 预热没成不要紧：往后真有事件要响时照常 play，这一下失败不代表音频废了
                System.err.println("音效预热失败，跳过：" + e);
            }
        }
    }

    /**
     * 给一个界面的根节点装上点击音：界面里任何按钮被按下都响一声。
     *
     * <p>利用的是 {@code ActionEvent} 会沿"根 → 按钮"的整条派发链走一趟——
     * 在根节点挂一个过滤器即可覆盖该界面的所有按钮，包括那些在 View 内部自己
     * {@code setOnAction} 的（皮肤按钮、难度按钮、作弊开关……），不必逐个回调去插播放语句。
     * 这些界面里只有 Button 会发 ActionEvent（Label 之类不发），所以不会误响。</p>
     *
     * <p>用 {@code addEventFilter} 而不是 {@code addEventHandler}：过滤器在<b>捕获</b>阶段跑，
     * 早于按钮自己的 {@code setOnAction}；处理器在<b>冒泡</b>阶段跑，晚于它。而"开始游戏"
     * "重开一局"这类回调要读皮肤贴图、重建本局，几百毫秒的活儿干完了才轮到点击音响，
     * 那一下是能听出来的。改用过滤器，点击音在按钮干活之前就发出去。</p>
     */
    public static void attachMenuClickSound(Parent root) {
        root.addEventFilter(ActionEvent.ACTION, event -> play(SoundEvent.MENU_CLICK));
    }

    /** 按顺序放一串事件（控制层每帧把模型攒下的事件交过来）。 */
    public static void playAll(List<SoundEvent> events) {
        if (events == null || events.isEmpty() || !isEnabled()) {
            return;
        }
        for (SoundEvent event : events) {
            play(event);
        }
    }

    /**
     * 是否出声：配置里的总开关开着，且没有测试用的静音系统属性，且音频子系统没被判死。
     * 包内可见是为了让 {@code SoundPlayerTest} 也能查这个口径。
     */
    static boolean isEnabled() {
        return GameConfig.SOUND_ENABLED
                && !Boolean.getBoolean("sound.disabled")
                && !soundSystemUnavailable;
    }

    /**
     * 事件对应的素材文件（不判断存在与否）；没有登记的事件返回 null。
     * 包内可见是为了让 {@code SoundPlayerTest} 能逐个核对素材真的在盘上，不必为测试放宽到 public。
     */
    static File fileOf(SoundEvent event) {
        String fileName = FILES.get(event);
        return fileName == null ? null : new File(BASE + fileName);
    }

    /** 取事件对应的音频：预加载过就直接命中缓存，没预加载过（或没赶上）就现解码一份。 */
    private static AudioClip clipFor(SoundEvent event) {
        String fileName = FILES.get(event);
        if (fileName == null) {
            return null;   // 没登记素材的事件（将来新加事件忘了配）就当静音，不影响游戏
        }
        return clipOf(fileName);
    }

    /** 按文件名取已解码的音频，缓存里没有就解码一份放进去；解码失败过的不再重试。 */
    private static AudioClip clipOf(String fileName) {
        AudioClip cached = CLIPS.get(fileName);
        if (cached != null) {
            return cached;
        }
        if (UNLOADABLE.contains(fileName)) {
            return null;
        }
        AudioClip clip = loadClip(fileName);
        if (clip == null) {
            UNLOADABLE.add(fileName);
            return null;
        }
        CLIPS.put(fileName, clip);
        return clip;
    }

    /**
     * 解码一个素材文件；失败返回 null 并打一行日志。
     *
     * <p>只兜 {@code Exception} 与 {@code LinkageError}：前者是找不到文件、解码失败等常规情况，
     * 后者是缺 javafx-media 原生库（整个子系统都废了，置位后彻底不再尝试）。
     * 故意不兜 {@code Throwable}——那样会把 OOM 一类的错误也吞掉。</p>
     *
     * <p>注意音频真正<em>播放</em>时的错误发生在后台播放线程上，这里兜不住，
     * 那种情况表现为"没声音但游戏照常"，同样不会崩。</p>
     */
    private static AudioClip loadClip(String fileName) {
        File file = new File(BASE + fileName);
        if (!file.exists()) {
            System.err.println("找不到音效素材：" + file.getAbsolutePath());
            return null;
        }
        try {
            // 音量不在这里定：同一份素材将来可能换到别的事件上，而音量是"按事件"而不是
            // "按文件"配的，所以每次 play 时现取（见 volumeOf），load 只管把数据解出来。
            return new AudioClip(file.toURI().toString());
        } catch (LinkageError e) {
            soundSystemUnavailable = true;
            System.err.println("音频子系统不可用，本次运行将保持静音：" + e);
            return null;
        } catch (Exception e) {
            System.err.println("音效 " + fileName + " 加载失败，这一声跳过：" + e);
            return null;
        }
    }

    /**
     * 事件这一声的实际音量 = 全局音量 {@link GameConfig#SOUND_VOLUME} ×
     * 该事件的倍率 {@link GameConfig#soundVolumeOf}，再夹紧到 0.0～1.0。
     * 配置写超范围也不报错，夹一下就是了。
     *
     * <p>包内可见是为了让 {@code SoundPlayerTest} 能核对"哪几声该轻、轻重有没有夹在量程里"，
     * 不必为了测试把音量口径放宽到 public。</p>
     */
    static double volumeOf(SoundEvent event) {
        double volume = GameConfig.SOUND_VOLUME * GameConfig.soundVolumeOf(event);
        return Math.max(0.0, Math.min(1.0, volume));
    }
}
