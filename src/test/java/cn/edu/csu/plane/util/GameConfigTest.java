package cn.edu.csu.plane.util;

import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 校验 config.properties 确实在 classpath 上，并且读出来的数值就是 SRS 定的口径（NF-04）。
 * 只要配置文件没被打进包里，第一个断言就会红——这是"配置没生效"的第一道哨兵。
 */
class GameConfigTest {

    @Test
    void configFileIsOnClasspath() {
        try (InputStream in = GameConfig.class.getResourceAsStream("/config.properties")) {
            assertNotNull(in, "config.properties 没进 classpath，所有参数会退回默认值");
        } catch (Exception e) {
            fail("读取 config.properties 出错：" + e.getMessage());
        }
    }

    @Test
    void playerValuesMatchSpec() {
        assertEquals(100, GameConfig.DEFAULT_HEALTH, "SRS Q2：玩家初始血量 100");
        assertEquals(20, GameConfig.COLLISION_DAMAGE, "SRS Q2：撞击扣 20");
        assertEquals(10, GameConfig.BULLET_DAMAGE, "SRS Q2：敌弹扣 10");
        assertEquals(1.0, GameConfig.PLAYER_INVINCIBLE_TIME, 0.001, "SRS Q2：受击后 1 秒无敌");
        assertEquals(0.3, GameConfig.PLAYER_FIRE_INTERVAL, 0.001, "GWT-03：射击间隔 300ms");
        assertEquals(10.0, GameConfig.FIREPOWER_DURATION, 0.001, "SRS Q6：火力强化持续 10 秒");
    }

    @Test
    void enemyScoresMatchSpec() {
        assertEquals(100, GameConfig.NORMAL_ENEMY_SCORE, "SRS F08：普通敌机 100 分");
        assertEquals(150, GameConfig.MOVING_ENEMY_SCORE, "SRS F08：横移敌机 150 分");
        assertEquals(200, GameConfig.SHOOTING_ENEMY_SCORE, "SRS F08：射击敌机 200 分");
        assertEquals(150, GameConfig.BOMBER_ENEMY_SCORE, "SRS F08：自爆敌机 150 分");
    }

    @Test
    void difficultyValuesMatchSpec() {
        assertEquals(1000, GameConfig.SCORE_PER_LEVEL, "SRS Q3：每累计 1000 分升 1 关");
        assertEquals(0.2, GameConfig.ITEM_DROP_RATE, 0.001, "SRS Q6：击毁敌机 20% 掉道具");
        assertEquals(30, GameConfig.HEAL_AMOUNT, "SRS Q6：回血 30 点");
    }

    @Test
    void movementAndVictoryValuesAreUsable() {
        assertTrue(GameConfig.PLAYER_SPEED > 0, "玩家速度得是正数，否则战机走不动");
        assertTrue(GameConfig.VICTORY_SCORE > GameConfig.SCORE_PER_LEVEL,
                "通关分数要是高于一个关卡线，不然一升关就通关了");
    }
}
