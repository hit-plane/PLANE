package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.SoundEvent;
import cn.edu.csu.plane.util.GameConfig;
import javafx.event.ActionEvent;
import javafx.scene.Parent;
import javafx.scene.media.AudioClip;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** 按文件名缓存的已解码音频；加载失败也记一个 null，免得每次事件都重试一遍、把日志刷满。 */
    private static final Map<String, AudioClip> CLIPS = new HashMap<>();

    /** 整个音频子系统不可用（缺 javafx-media 原生库等）时置位，之后一律不再尝试。 */
    private static boolean soundSystemUnavailable;

    private SoundPlayer() {
    }

    /** 按事件放一声。已静音、事件没有对应素材、或素材加载失败时静默跳过。 */
    public static void play(SoundEvent event) {
        if (event == null || !isEnabled()) {
            return;
        }
        AudioClip clip = clipFor(event);
        if (clip != null) {
            clip.play();
        }
    }

    /**
     * 给一个界面的根节点装上点击音：界面里任何按钮被按下都响一声。
     *
     * <p>利用的是 {@code ActionEvent} 会从按钮沿父节点一路冒泡到根节点——
     * 在根节点挂一个处理器即可覆盖该界面的所有按钮，包括那些在 View 内部自己
     * {@code setOnAction} 的（皮肤按钮、难度按钮、作弊开关……），不必逐个回调去插播放语句。
     * 这些界面里只有 Button 会发 ActionEvent（Label 之类不发），所以不会误响。</p>
     */
    public static void attachMenuClickSound(Parent root) {
        root.addEventHandler(ActionEvent.ACTION, event -> play(SoundEvent.MENU_CLICK));
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

    /** 取事件对应的音频，第一次用到才解码，之后走缓存。 */
    private static AudioClip clipFor(SoundEvent event) {
        String fileName = FILES.get(event);
        if (fileName == null) {
            return null;   // 没登记素材的事件（将来新加事件忘了配）就当静音，不影响游戏
        }
        if (CLIPS.containsKey(fileName)) {
            return CLIPS.get(fileName);
        }
        AudioClip clip = loadClip(fileName);
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
            AudioClip clip = new AudioClip(file.toURI().toString());
            clip.setVolume(volume());
            return clip;
        } catch (LinkageError e) {
            soundSystemUnavailable = true;
            System.err.println("音频子系统不可用，本次运行将保持静音：" + e);
            return null;
        } catch (Exception e) {
            System.err.println("音效 " + fileName + " 加载失败，这一声跳过：" + e);
            return null;
        }
    }

    /** 配置里的音量，写超范围也不报错，播放时夹紧到 0.0～1.0。 */
    private static double volume() {
        return Math.max(0.0, Math.min(1.0, GameConfig.SOUND_VOLUME));
    }
}
