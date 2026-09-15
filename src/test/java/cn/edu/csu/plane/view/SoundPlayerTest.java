package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.SoundEvent;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * {@link SoundPlayer} 的单元测试：只核对"事件 → 素材文件"这张对照表，不去解码音频。
 *
 * <p>本类**故意不启动 JavaFX 工具箱**，也**故意不构造 AudioClip**：音频素材能不能解码、
 * 机器有没有声卡，都不该是单元测试的命题；能在这里守住的是"文件名没写错、素材没漏放、
 * 每个事件都有人管"——这恰恰是最容易在改素材时出错、又最难在跑游戏时发现的一类问题。</p>
 */
class SoundPlayerTest {

    /** 每个事件都得有对应素材，且文件真的在盘上（路径口径与 AssetLoader 一致：相对项目根）。 */
    @Test
    void everyEventMapsToAnExistingSoundFile() {
        for (SoundEvent event : SoundEvent.values()) {
            File file = SoundPlayer.fileOf(event);
            assertNotNull(file, "事件 " + event + " 没有登记素材文件，改素材时漏了？");
            assertTrue(file.isFile(),
                    "事件 " + event + " 的素材不存在：" + file.getAbsolutePath()
                            + "（音效素材放在项目根下的 resource/sounds，且工作目录必须是项目根）");
        }
    }

    /** 11 个事件各管各的素材：没有两个事件挤到同一个文件上（挤了说明表里复制粘贴写重了）。 */
    @Test
    void eventsDoNotShareSoundFiles() {
        Set<File> files = new HashSet<>();
        for (SoundEvent event : SoundEvent.values()) {
            assertTrue(files.add(SoundPlayer.fileOf(event)),
                    "事件 " + event + " 与别的音效事件指向了同一份素材，对照表里写重了");
        }
        assertEquals(SoundEvent.values().length, files.size());
    }

    /**
     * 没有登记素材的事件不该崩，只是不响。
     *
     * <p>{@link SoundEvent#MENU_CLICK} 由界面直接播，不经过模型；这里借它验证的是
     * "播放接口对任何入参都不抛异常"——真放出声与否由下面的静音测试与手动试听负责。</p>
     */
    @Test
    void playingIsSafeEvenWhenMuted() {
        assertDoesNotThrow(() -> SoundPlayer.play(SoundEvent.MENU_CLICK));
        assertDoesNotThrow(() -> SoundPlayer.play(null));
        assertDoesNotThrow(() -> SoundPlayer.playAll(null));
        assertDoesNotThrow(() -> SoundPlayer.playAll(Arrays.asList(SoundEvent.values())));
    }

    /**
     * 击中敌机那一声要明显比别的音效轻（F14 的调音口径，见配置里的 {@code sound.volume.enemy_hit}）。
     *
     * <p>它一局里响得最密——每次打中、每次打死都各来一声，跟别的音效一个音量就会盖住
     * 通关、拾取这些更要紧的声音。所以这里把"它比谁轻"钉住：日后谁把那一行配置删了或改回 1.0，
     * 不是改个数字没人管，而是有一条测试会告诉他这是刻意的。</p>
     */
    @Test
    void enemyHitSoundIsQuieterThanTheRest() {
        assertTrue(SoundPlayer.volumeOf(SoundEvent.ENEMY_HIT) < SoundPlayer.volumeOf(SoundEvent.VICTORY),
                "击中音该比通关音轻");
        assertTrue(SoundPlayer.volumeOf(SoundEvent.ENEMY_HIT) < SoundPlayer.volumeOf(SoundEvent.MENU_CLICK),
                "击中音该比菜单点击音轻");
        assertTrue(SoundPlayer.volumeOf(SoundEvent.ENEMY_DESTROYED) < SoundPlayer.volumeOf(SoundEvent.ITEM_PICKUP),
                "击毁音与击中音用的是同一份音频，也该一起轻下来");
    }

    /** 每个事件的音量都得落在 0.0～1.0 里：配置写超范围（哪怕是负数）也不该把播放搞崩。 */
    @Test
    void everyEventVolumeIsClampedToUnitRange() {
        for (SoundEvent event : SoundEvent.values()) {
            double volume = SoundPlayer.volumeOf(event);
            assertTrue(volume >= 0.0 && volume <= 1.0,
                    "事件 " + event + " 的音量 " + volume + " 超出了 0.0～1.0");
        }
    }

    /**
     * 预加载在静音（测试期）下必须是空操作：不解码、不起线程、更不能把调用线程卡住。
     *
     * <p>守的是 {@code preload()} 的实现口径——<b>先看开关、再干活</b>。真放出声与否不必在这里验，
     * 预加载的意义只是把将来的活儿提前做掉，做没做只影响延迟、不影响响不响。</p>
     */
    @Test
    void preloadStaysOutOfTheWayWhileMuted() {
        assertDoesNotThrow(SoundPlayer::preload);
        assertDoesNotThrow(SoundPlayer::preload);   // 重复调用幂等，不该起第二条加载线程
        assertFalse(Thread.getAllStackTraces().keySet().stream()
                        .anyMatch(t -> "sound-preload".equals(t.getName())),
                "静音时不该起音效预加载线程");
    }

    /**
     * 测试期必须静音：pom 里 surefire 配了 {@code -Dsound.disabled=true}。
     * 这条守的是那个配置被误删——删了的话 {@code MainMenuViewTest} 每按一次按钮
     * 都会真的去解码并播放一次点击音（没声卡的机器上还会报错）。
     *
     * <p>用 assume 而不是 assert：在 IDE 里直接跑单个测试类时不会有这个系统属性，
     * 那种情况下跳过即可，不该把一个环境差异算成失败。</p>
     */
    @Test
    void mutedWhileTestsRun() {
        assumeTrue(Boolean.getBoolean("sound.disabled"),
                "不在 surefire 下跑（没有 -Dsound.disabled），跳过这条");
        assertFalse(SoundPlayer.isEnabled(), "测试期不该出声");
    }
}
