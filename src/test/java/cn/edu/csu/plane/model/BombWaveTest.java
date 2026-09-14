package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link BombWave} 的单元测试：生成位置与尺寸、向上扫的速度、出屏销毁。
 */
class BombWaveTest {

    private static final double DT = 0.1;

    @Test
    void spawnsAcrossFullWidthAtBottom() {
        BombWave wave = new BombWave();
        assertEquals(0, wave.getX(), 0.001);
        assertEquals(GameConfig.WINDOW_HEIGHT, wave.getY(), 0.001, "从战场底边生成");
        assertEquals(GameConfig.WINDOW_WIDTH, wave.getWidth(), 0.001, "横贯整个战场宽");
        assertEquals(GameConfig.BOMB_WAVE_HEIGHT, wave.getHeight(), 0.001);
        assertTrue(wave.isAlive());
    }

    @Test
    void sweepsUpwardAtConfiguredSpeed() {
        BombWave wave = new BombWave();
        wave.update(DT);
        assertEquals(GameConfig.WINDOW_HEIGHT - GameConfig.BOMB_WAVE_SPEED * DT, wave.getY(), 0.001,
                "每帧按配置速度向上位移");
    }

    @Test
    void diesAfterLeavingTop() {
        BombWave wave = new BombWave();
        // 走到"整条波扫出顶边"所需步数，多给几步余量；速度可配置，步数跟着算。
        int steps = (int) Math.ceil((GameConfig.WINDOW_HEIGHT + GameConfig.BOMB_WAVE_HEIGHT)
                / (GameConfig.BOMB_WAVE_SPEED * DT)) + 5;
        for (int i = 0; i < steps && wave.isAlive(); i++) {
            wave.update(DT);
        }
        assertFalse(wave.isAlive(), "扫出顶边后应销毁");
    }
}
