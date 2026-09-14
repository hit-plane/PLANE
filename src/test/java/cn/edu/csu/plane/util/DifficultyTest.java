package cn.edu.csu.plane.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 三档难度（F16）的单元测试。
 *
 * <p>最要紧的一条是"普通档没被动过"：{@link GameConfig} 里不传难度的旧方法一律按普通档算，
 * 因此它必须与显式传 {@link Difficulty#NORMAL} 的结果逐值相等。这条钉住了，
 * 加难度这件事就没有改动任何既有手感。</p>
 */
class DifficultyTest {

    /** 三档都要检查的血量基准：普通 200 / 横移 100 / 射击 300 / 自爆 100。 */
    private static final int[] BASE_HEALTHS = {
            GameConfig.NORMAL_ENEMY_HEALTH, GameConfig.MOVING_ENEMY_HEALTH,
            GameConfig.SHOOTING_ENEMY_HEALTH, GameConfig.BOMBER_ENEMY_HEALTH,
    };

    /** 关卡上限 10：够覆盖配置里那几条成长曲线的全段。 */
    private static final int MAX_LEVEL = 10;

    private static final double EPS = 0.001;

    /** 普通档 = 旧版：不传难度的重载与显式传 NORMAL 必须处处一致，一个数都不能差。 */
    @Test
    void normalDifficultyIsIdenticalToLegacyBehavior() {
        for (int level = 1; level <= MAX_LEVEL; level++) {
            for (int base : BASE_HEALTHS) {
                assertEquals(GameConfig.enemyHealthAt(base, level),
                        GameConfig.enemyHealthAt(base, level, Difficulty.NORMAL),
                        "普通档血量必须与旧口径一致：基准 " + base + "，第 " + level + " 关");
            }
            assertEquals(GameConfig.itemDropRateAt(level),
                    GameConfig.itemDropRateAt(level, Difficulty.NORMAL), EPS,
                    "普通档掉落率必须与旧口径一致：第 " + level + " 关");
        }
    }

    /** 普通档的密度曲线也与旧算式一致：基础间隔 - (关卡-1) × 每关缩短，卡在下限之上。 */
    @Test
    void normalSpawnIntervalMatchesLegacyFormula() {
        for (int level = 1; level <= MAX_LEVEL; level++) {
            double legacy = Math.max(
                    GameConfig.SPAWN_INTERVAL_BASE - (level - 1) * GameConfig.SPAWN_INTERVAL_STEP,
                    GameConfig.SPAWN_INTERVAL_MIN);
            assertEquals(legacy, GameConfig.spawnIntervalAt(level, Difficulty.NORMAL), EPS,
                    "普通档生成间隔必须与旧算式一致：第 " + level + " 关");
        }
    }

    /** 档位名就是界面上要显示的三个词。 */
    @Test
    void labelsAreTheChineseNamesShownInMenu() {
        assertEquals("简单", Difficulty.EASY.getLabel());
        assertEquals("普通", Difficulty.NORMAL.getLabel());
        assertEquals("困难", Difficulty.HARD.getLabel());
    }

    /** 三档确实分开了：简单更脆、困难更厚，普通夹在中间；成长曲线同理。 */
    @Test
    void healthAndGrowthAreOrderedAcrossDifficulties() {
        for (int level = 1; level <= MAX_LEVEL; level++) {
            int base = GameConfig.NORMAL_ENEMY_HEALTH;
            int easy = GameConfig.enemyHealthAt(base, level, Difficulty.EASY);
            int normal = GameConfig.enemyHealthAt(base, level, Difficulty.NORMAL);
            int hard = GameConfig.enemyHealthAt(base, level, Difficulty.HARD);
            assertTrue(easy < normal, "第 " + level + " 关：简单档该比普通档脆：" + easy + " vs " + normal);
            assertTrue(hard > normal, "第 " + level + " 关：困难档该比普通档厚：" + hard + " vs " + normal);
        }

        // "成长曲线"这一维单独生效：三档的血量差距必须随关卡拉开，而不是恒定倍数
        int easyGapAtLevel1 = GameConfig.NORMAL_ENEMY_HEALTH
                - GameConfig.enemyHealthAt(GameConfig.NORMAL_ENEMY_HEALTH, 1, Difficulty.EASY);
        int easyGapAtLevel10 = GameConfig.enemyHealthAt(GameConfig.NORMAL_ENEMY_HEALTH, 10, Difficulty.NORMAL)
                - GameConfig.enemyHealthAt(GameConfig.NORMAL_ENEMY_HEALTH, 10, Difficulty.EASY);
        assertTrue(easyGapAtLevel10 > easyGapAtLevel1,
                "简单档的成长更缓，差距该越到后面越大：" + easyGapAtLevel1 + " → " + easyGapAtLevel10);
    }

    /** 第 1 关的击毁枪数：简单与普通完全一样（2/1/3/1 发），只有困难抬到 3/2/4/2 发。 */
    @Test
    void firstLevelKillShotsOnlyChangeOnHard() {
        for (int base : BASE_HEALTHS) {
            assertEquals(shots(base, 1, Difficulty.NORMAL), shots(base, 1, Difficulty.EASY),
                    "简单档不该改变第 1 关的击毁枪数：基准 " + base);
        }

        assertEquals(2, shots(GameConfig.NORMAL_ENEMY_HEALTH, 1, Difficulty.NORMAL), "普通档第 1 关普通机 2 发");
        assertEquals(1, shots(GameConfig.MOVING_ENEMY_HEALTH, 1, Difficulty.NORMAL), "普通档第 1 关横移机 1 发");
        assertEquals(3, shots(GameConfig.SHOOTING_ENEMY_HEALTH, 1, Difficulty.NORMAL), "普通档第 1 关射击机 3 发");
        assertEquals(1, shots(GameConfig.BOMBER_ENEMY_HEALTH, 1, Difficulty.NORMAL), "普通档第 1 关自爆机 1 发");

        assertEquals(3, shots(GameConfig.NORMAL_ENEMY_HEALTH, 1, Difficulty.HARD), "困难档普通机 3 发");
        assertEquals(2, shots(GameConfig.MOVING_ENEMY_HEALTH, 1, Difficulty.HARD), "困难档横移机 2 发");
        assertEquals(4, shots(GameConfig.SHOOTING_ENEMY_HEALTH, 1, Difficulty.HARD), "困难档射击机 4 发");
        assertEquals(2, shots(GameConfig.BOMBER_ENEMY_HEALTH, 1, Difficulty.HARD), "困难档自爆机 2 发");
    }

    /** 用单发伤害把血量折成"几发能打掉"，向上取整。 */
    private static int shots(int baseHealth, int level, Difficulty difficulty) {
        int damage = GameConfig.playerBulletDamage(1);
        int health = GameConfig.enemyHealthAt(baseHealth, level, difficulty);
        return (health + damage - 1) / damage;
    }

    /** 掉落率：简单更慷慨、困难更抠门，且三档都随关卡递减、都落在 (0, 1]。 */
    @Test
    void dropRateDiffersByDifficultyAndStaysMonotonic() {
        for (Difficulty difficulty : Difficulty.values()) {
            double previous = Double.MAX_VALUE;
            for (int level = 1; level <= MAX_LEVEL; level++) {
                double rate = GameConfig.itemDropRateAt(level, difficulty);
                assertTrue(rate > 0 && rate <= 1, difficulty.getLabel() + " 第 " + level + " 关概率越界：" + rate);
                assertTrue(rate <= previous,
                        difficulty.getLabel() + " 第 " + level + " 关概率不该高于上一关：" + rate + " > " + previous);
                previous = rate;
            }
        }

        for (int level = 1; level <= MAX_LEVEL; level++) {
            double easy = GameConfig.itemDropRateAt(level, Difficulty.EASY);
            double normal = GameConfig.itemDropRateAt(level, Difficulty.NORMAL);
            double hard = GameConfig.itemDropRateAt(level, Difficulty.HARD);
            assertTrue(easy > normal, "第 " + level + " 关：简单档该掉得更多：" + easy + " vs " + normal);
            assertTrue(hard < normal, "第 " + level + " 关：困难档该掉得更少：" + hard + " vs " + normal);
        }

        // 第 1 关与后期（已贴到普通档下限的那一关）各看一眼具体数值
        assertEquals(0.70, GameConfig.itemDropRateAt(1, Difficulty.EASY), EPS, "简单档第 1 关 70%");
        assertEquals(0.30, GameConfig.itemDropRateAt(1, Difficulty.HARD), EPS, "困难档第 1 关 30%");
        assertEquals(0.21, GameConfig.itemDropRateAt(10, Difficulty.EASY), EPS, "简单档第 10 关 21%");
        assertEquals(0.09, GameConfig.itemDropRateAt(10, Difficulty.HARD), EPS, "困难档第 10 关 9%");
    }

    /** 生成间隔：简单更稀疏、困难更密，但三档都不跌破共用的安全下限。 */
    @Test
    void spawnIntervalDiffersByDifficultyAndRespectsSharedFloor() {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int level = 1; level <= MAX_LEVEL; level++) {
                double interval = GameConfig.spawnIntervalAt(level, difficulty);
                assertTrue(interval >= GameConfig.SPAWN_INTERVAL_MIN - EPS,
                        difficulty.getLabel() + " 第 " + level + " 关间隔跌破了共用下限：" + interval);
                assertTrue(interval <= GameConfig.spawnIntervalAt(level, Difficulty.EASY) + EPS,
                        "简单档该是三者中最稀疏的");
            }
        }

        for (int level = 1; level <= MAX_LEVEL; level++) {
            double easy = GameConfig.spawnIntervalAt(level, Difficulty.EASY);
            double normal = GameConfig.spawnIntervalAt(level, Difficulty.NORMAL);
            double hard = GameConfig.spawnIntervalAt(level, Difficulty.HARD);
            assertTrue(easy >= normal, "第 " + level + " 关：简单档不该比普通档更密：" + easy + " vs " + normal);
            assertTrue(hard <= normal, "第 " + level + " 关：困难档不该比普通档更稀疏：" + hard + " vs " + normal);
        }

        assertEquals(0.6, GameConfig.spawnIntervalAt(1, Difficulty.EASY), EPS, "简单档第 1 关 0.6 秒一架");
        assertEquals(0.4, GameConfig.spawnIntervalAt(1, Difficulty.HARD), EPS, "困难档第 1 关 0.4 秒一架");
        // 高关卡三档收敛到同一个密度上限：下限不乘倍率，是防止一帧糊满屏的安全阀
        assertEquals(GameConfig.SPAWN_INTERVAL_MIN, GameConfig.spawnIntervalAt(10, Difficulty.EASY), EPS);
        assertEquals(GameConfig.SPAWN_INTERVAL_MIN, GameConfig.spawnIntervalAt(10, Difficulty.HARD), EPS);
    }

    /** 血量下限：再小的基准值也得兜到至少 1 点，任何难度都一样。 */
    @Test
    void healthNeverDropsBelowOne() {
        for (Difficulty difficulty : Difficulty.values()) {
            for (int level = 1; level <= MAX_LEVEL; level++) {
                assertTrue(GameConfig.enemyHealthAt(1, level, difficulty) >= 1,
                        difficulty.getLabel() + " 第 " + level + " 关血量该至少 1 点");
            }
        }
    }
}
