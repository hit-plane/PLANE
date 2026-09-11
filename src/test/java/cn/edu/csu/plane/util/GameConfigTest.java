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

    /** 火力上限提到 5 级：每一级都要真的多打出一发，不能只是数字变大。 */
    @Test
    void firepowerCapIsFive() {
        assertEquals(5, GameConfig.MAX_FIRE_POWER, "火力上限应为 5 级");
        assertTrue(GameConfig.MAX_FIRE_POWER > 2, "上限要比原来的双发更高，否则这次调整没意义");
    }

    @Test
    void difficultyValuesMatchSpec() {
        assertEquals(1000, GameConfig.SCORE_PER_LEVEL, "SRS Q3：每累计 1000 分升 1 关");
        assertEquals(30, GameConfig.HEAL_AMOUNT, "SRS Q6：回血 30 点");
    }

    /** 掉落概率由 20% 上调到 50%：杀两个掉一个是本次调整的明确口径。 */
    @Test
    void itemDropRateIsHalf() {
        assertEquals(0.5, GameConfig.ITEM_DROP_RATE, 0.001, "击毁敌机应有 50% 概率掉道具");
        assertTrue(GameConfig.ITEM_DROP_RATE <= 1.0 && GameConfig.ITEM_DROP_RATE > 0,
                "概率得落在 (0, 1] 区间内，实际 " + GameConfig.ITEM_DROP_RATE);
    }

    /** 掉落里火力强化占比最高：必须高于四选一均分的 25%，但也不能包圆。 */
    @Test
    void firepowerIsTheMostLikelyDrop() {
        double uniform = 0.25;
        assertTrue(GameConfig.ITEM_FIREPOWER_RATE > uniform,
                "火力强化占比应高于四选一均分的 25%，实际 " + GameConfig.ITEM_FIREPOWER_RATE);
        assertTrue(GameConfig.ITEM_FIREPOWER_RATE < 1.0,
                "不能 100% 只掉火力，炸弹/护盾/回血还得能出现，实际 " + GameConfig.ITEM_FIREPOWER_RATE);
    }

    /** 竖版窗口：高度必须大于宽度，否则又变回横版了。 */
    @Test
    void windowIsPortrait() {
        assertTrue(GameConfig.WINDOW_HEIGHT > GameConfig.WINDOW_WIDTH,
                "窗口应为竖版，实际 " + GameConfig.WINDOW_WIDTH + "×" + GameConfig.WINDOW_HEIGHT);
    }

    /** 图标放大 50%：尺寸类常量都得是"原始尺寸 × icon.scale"。 */
    @Test
    void iconScaleEnlargesEverySprite() {
        assertEquals(1.5, GameConfig.ICON_SCALE, 0.001, "图标应整体放大 50%");
        assertEquals(50 * GameConfig.ICON_SCALE, GameConfig.PLAYER_SIZE, 0.001, "玩家机也要放大");
        assertEquals(40 * GameConfig.ICON_SCALE, GameConfig.NORMAL_ENEMY_SIZE, 0.001, "普通敌机尺寸要乘缩放");
        assertEquals(50 * GameConfig.ICON_SCALE, GameConfig.ENEMY_SIZE, 0.001, "高级敌机尺寸要乘缩放");
        assertEquals(30 * GameConfig.ICON_SCALE, GameConfig.ITEM_SIZE, 0.001, "道具尺寸要乘缩放");
    }

    /** 怪密度两轮翻倍：v1.0 基础 2.0 秒 → 1.0 秒 → 0.5 秒，下限同步收到 0.175 秒。 */
    @Test
    void spawnIntervalsAreDenserThanBefore() {
        assertTrue(GameConfig.SPAWN_INTERVAL_BASE <= 0.5,
                "基础生成间隔应不大于 0.5 秒（v1.0 是 2.0 秒，翻倍两次），实际 " + GameConfig.SPAWN_INTERVAL_BASE);
        assertTrue(GameConfig.SPAWN_INTERVAL_MIN < 0.35,
                "生成间隔下限应短于上一版的 0.35 秒，实际 " + GameConfig.SPAWN_INTERVAL_MIN);
        assertTrue(GameConfig.SPAWN_INTERVAL_MIN <= GameConfig.SPAWN_INTERVAL_BASE,
                "下限不该高于起始间隔，否则初始就顶着下限跑");
    }

    /** 分数上限提到 10000：正好是关卡封顶的 10 级 × 每级 1000 分。 */
    @Test
    void scoreCeilingIsTenThousand() {
        assertEquals(10000, GameConfig.VICTORY_SCORE, "分数上限应为 10000");
        assertEquals(0, GameConfig.VICTORY_SCORE % GameConfig.SCORE_PER_LEVEL,
                "分数上限应正好落在关卡线上，免得刚升一关就通关");
    }

    @Test
    void movementAndVictoryValuesAreUsable() {
        assertTrue(GameConfig.PLAYER_SPEED > 0, "玩家速度得是正数，否则战机走不动");
        assertTrue(GameConfig.VICTORY_SCORE > GameConfig.SCORE_PER_LEVEL,
                "通关分数要是高于一个关卡线，不然一升关就通关了");
    }
}
