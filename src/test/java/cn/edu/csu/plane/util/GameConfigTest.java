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

    /** 第 1 关的掉落概率仍是 50%：它是逐关递减的起点，杀两个掉一个。 */
    @Test
    void itemDropRateStartsAtHalf() {
        assertEquals(0.5, GameConfig.ITEM_DROP_RATE, 0.001, "第 1 关击毁敌机应有 50% 概率掉道具");
        assertTrue(GameConfig.ITEM_DROP_RATE <= 1.0 && GameConfig.ITEM_DROP_RATE > 0,
                "概率得落在 (0, 1] 区间内，实际 " + GameConfig.ITEM_DROP_RATE);
    }

    /** 掉落概率随关卡递减且不跌破下限：第 1 关 50% → 第 10 关 15%。 */
    @Test
    void itemDropRateShrinksAsLevelRises() {
        assertEquals(GameConfig.ITEM_DROP_RATE, GameConfig.itemDropRateAt(1), 0.001, "第 1 关就用基础概率");
        assertTrue(GameConfig.itemDropRateAt(10) < GameConfig.itemDropRateAt(1),
                "高关卡掉落应更少：第 1 关 " + GameConfig.itemDropRateAt(1)
                        + " vs 第 10 关 " + GameConfig.itemDropRateAt(10));

        double previous = Double.MAX_VALUE;
        for (int level = 1; level <= 10; level++) {
            double rate = GameConfig.itemDropRateAt(level);
            assertTrue(rate <= previous, "第 " + level + " 关的掉落概率不该高于上一关：" + rate + " > " + previous);
            assertTrue(rate > 0 && rate <= 1, "概率得落在 (0, 1] 内，实际 " + rate);
            assertTrue(rate >= GameConfig.ITEM_DROP_RATE_MIN - 0.001,
                    "概率不该跌破下限 " + GameConfig.ITEM_DROP_RATE_MIN + "，实际 " + rate);
            previous = rate;
        }
        assertEquals(GameConfig.ITEM_DROP_RATE_MIN, GameConfig.itemDropRateAt(99), 0.001, "关卡再高也贴着下限，不会掉到 0");
    }

    /** 掉落里"火力强化（加弹道）"占 25%：与炸弹/护盾/回血的均分概率持平，不再一家独大。 */
    @Test
    void firepowerDropRateIsAQuarter() {
        assertEquals(0.25, GameConfig.ITEM_FIREPOWER_RATE, 0.001, "加弹道的火力强化占比应为 25%");
        assertTrue(GameConfig.ITEM_FIREPOWER_RATE > 0 && GameConfig.ITEM_FIREPOWER_RATE < 1.0,
                "概率得落在 (0, 1) 内，炸弹/护盾/回血还得能出现，实际 " + GameConfig.ITEM_FIREPOWER_RATE);
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

    /** 单发子弹伤害按火力等级递减：1 级 100%、2 级 75%、3 级 65%、4/5 级 50%。 */
    @Test
    void bulletDamageDropsAsFirePowerRises() {
        int base = GameConfig.BULLET_DAMAGE_BASE;
        assertEquals(base, GameConfig.playerBulletDamage(1), "1 级就是 100% 基准伤害");
        assertEquals(Math.round(base * 0.75), GameConfig.playerBulletDamage(2), "2 级 75%");
        assertEquals(Math.round(base * 0.65), GameConfig.playerBulletDamage(3), "3 级 65%");
        assertEquals(Math.round(base * 0.50), GameConfig.playerBulletDamage(4), "4 级 50%");
        assertEquals(GameConfig.playerBulletDamage(4), GameConfig.playerBulletDamage(5), "5 级也是 50%");

        for (int level = 1; level < GameConfig.MAX_FIRE_POWER; level++) {
            assertTrue(GameConfig.playerBulletDamage(level) >= GameConfig.playerBulletDamage(level + 1),
                    "等级越高单发伤害不该更高：" + level + " 级 " + GameConfig.playerBulletDamage(level)
                            + " < " + (level + 1) + " 级 " + GameConfig.playerBulletDamage(level + 1));
        }
        assertTrue(GameConfig.playerBulletDamage(0) > 0, "越界等级也要给个正数伤害，不能打出 0 伤害的子弹");
        assertTrue(GameConfig.playerBulletDamage(99) > 0, "超过百分比表的等级沿用最高档，不越界");
    }

    /** 敌机血量随关卡变厚：第 1 关就是基准值，之后逐关递增。 */
    @Test
    void enemyHealthGrowsWithLevel() {
        assertEquals(GameConfig.NORMAL_ENEMY_HEALTH, GameConfig.enemyHealthAt(GameConfig.NORMAL_ENEMY_HEALTH, 1),
                "第 1 关用基准血量");
        assertTrue(GameConfig.ENEMY_HEALTH_GROWTH_PER_LEVEL > 0, "成长率得是正数，否则难度不会涨");

        int previous = 0;
        for (int level = 1; level <= 10; level++) {
            int health = GameConfig.enemyHealthAt(GameConfig.NORMAL_ENEMY_HEALTH, level);
            assertTrue(health > previous, "第 " + level + " 关的血量应比上一关厚：" + health + " <= " + previous);
            previous = health;
        }
        assertTrue(GameConfig.enemyHealthAt(GameConfig.SHOOTING_ENEMY_HEALTH, 10) > GameConfig.SHOOTING_ENEMY_HEALTH,
                "第 10 关的射击机也该比第 1 关厚");
        assertTrue(GameConfig.enemyHealthAt(0, 10) >= 1, "极小的基准血量也得兜到至少 1 点");
    }
}
