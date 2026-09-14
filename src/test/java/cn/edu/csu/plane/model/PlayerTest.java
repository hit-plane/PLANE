package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;
import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Player} 的单元测试：覆盖初始状态、移动边界、射击、冷却、受伤/无敌帧、
 * 护盾、治疗、火力强化（本局永久）、按难度决定的起始数值（折磨档五连发/9999 血/双倍弹速）与死亡判定。
 *
 * <p>这一层故意用 50×50 的自造机体，跟游戏里配置的玩家尺寸解耦：
 * 射击落点按"机身宽 50 + {@link Bullet#WIDTH}"算，子弹变宽时断言跟着走。</p>
 */
class PlayerTest {

    private static final double BODY = 50;

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(100, 100, BODY, BODY);
    }

    @Test
    void initialState() {
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getMaxHealth());
        assertEquals(1, player.getFirePower());
        assertEquals(GameConfig.PLAYER_FIRE_INTERVAL, player.getFireRate(), 0.001);
        assertTrue(player.isAlive());
        assertFalse(player.isShielded());
        assertFalse(player.isInvincible());
        assertTrue(player.isReadyToShoot());
    }

    @Test
    void movesNormally() {
        player.move(10, 20);
        assertEquals(110, player.getX(), 0.001);
        assertEquals(120, player.getY(), 0.001);
    }

    @Test
    void clampsToTopLeftBoundary() {
        player.move(-200, -200);
        assertEquals(0, player.getX(), 0.001);
        assertEquals(0, player.getY(), 0.001);
    }

    @Test
    void clampsToBottomRightBoundary() {
        player.move(10000, 10000);
        assertEquals(GameConfig.WINDOW_WIDTH - player.getWidth(), player.getX(), 0.001);
        assertEquals(GameConfig.WINDOW_HEIGHT - player.getHeight(), player.getY(), 0.001);
    }

    @Test
    void shootReturnsSingleBulletAtBaseFirePower() {
        List<Bullet> shots = player.shoot();
        assertEquals(1, shots.size());

        Bullet b = shots.get(0);
        assertTrue(b.isPlayerBullet());
        assertEquals(GameConfig.playerBulletDamage(1), b.getDamage(), "1 级火力是 100% 基准伤害");
        assertEquals(-GameConfig.WINDOW_HEIGHT / GameConfig.PLAYER_FIRE_INTERVAL, b.getVelY(), 0.001);
        assertEquals(100 + BODY / 2 - Bullet.WIDTH / 2, b.getX(), 0.001); // 机头正中
        assertEquals(100, b.getY(), 0.001);

        assertFalse(player.isReadyToShoot()); // 开火后进入冷却
    }

    @Test
    void shootReturnsTwoBulletsAfterEnhance() {
        player.enhanceFirePower(); // firePower = 2
        List<Bullet> shots = player.shoot();
        assertEquals(2, shots.size());
        assertEquals(108, shots.get(0).getX(), 0.001); // x + 8
        assertEquals(100 + BODY - 8 - Bullet.WIDTH, shots.get(1).getX(), 0.001); // x + 宽 - 8 - 弹宽
    }

    /**
     * 单发伤害随火力等级递减：1 级 100%、2 级 75%、3 级 65%、4/5 级 50%。
     * 弹道越多单发越轻，这样"多吃几个火力道具"不会让总伤害失控。
     */
    @Test
    void shootDamageDropsAsFirePowerRises() {
        int previous = Integer.MAX_VALUE;
        for (int level = 1; level <= GameConfig.MAX_FIRE_POWER; level++) {
            if (level > 1) {
                player.enhanceFirePower();
            }
            player.update(GameConfig.PLAYER_FIRE_INTERVAL);   // 走完冷却才能开下一枪

            List<Bullet> shots = player.shoot();
            assertEquals(level, shots.size(), "火力 " + level + " 级就该打出 " + level + " 发");

            int damage = shots.get(0).getDamage();
            assertEquals(GameConfig.playerBulletDamage(level), damage, "火力 " + level + " 级的单发伤害");
            assertTrue(damage <= previous, "等级越高单发伤害不该更高：" + level + " 级 " + damage + " > 上一级 " + previous);
            previous = damage;
        }

        assertEquals(GameConfig.BULLET_DAMAGE_BASE, GameConfig.playerBulletDamage(1), "1 级是基准伤害（100%）");
        assertEquals(GameConfig.playerBulletDamage(5), GameConfig.playerBulletDamage(4), "4、5 级单发伤害相同（都是 50%）");
    }

    @Test
    void shootEntersCooldownThenRecovers() {
        player.shoot();
        assertFalse(player.isReadyToShoot());

        player.update(GameConfig.PLAYER_FIRE_INTERVAL); // 冷却走完
        assertTrue(player.isReadyToShoot());
    }

    @Test
    void takeDamageReducesHealthAndGrantsInvincibility() {
        player.takeDamage(GameConfig.BULLET_DAMAGE);
        assertEquals(GameConfig.DEFAULT_HEALTH - GameConfig.BULLET_DAMAGE, player.getHealth());
        assertTrue(player.isInvincible());
    }

    @Test
    void immuneDuringInvincibility() {
        player.takeDamage(1);
        int hp = player.getHealth();
        player.takeDamage(1);
        assertEquals(hp, player.getHealth());
    }

    @Test
    void invincibilityExpiresOverTime() {
        player.takeDamage(1);
        int hp = player.getHealth();
        player.update(2.0); // 无敌帧 1 秒，2 秒后消失
        player.takeDamage(1);
        assertEquals(hp - 1, player.getHealth());
    }

    @Test
    void shieldBlocksOneHit() {
        player.activateShield();
        player.takeDamage(50);
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
        assertFalse(player.isShielded()); // 盾只挡一下
    }

    @Test
    void shieldOnlyBlocksOnce() {
        player.activateShield();
        player.takeDamage(1);
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
        assertFalse(player.isShielded(), "盾已消耗");
        assertTrue(player.isInvincible(), "盾破后进入无敌");

        // 无敌期间再受击仍不掉血
        player.takeDamage(1);
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());

        // 无敌结束后再受击才正常扣血
        player.update(GameConfig.PLAYER_INVINCIBLE_TIME + 0.1);
        player.takeDamage(1);
        assertEquals(GameConfig.DEFAULT_HEALTH - 1, player.getHealth());
    }

    @Test
    void healDoesNotExceedMax() {
        player.takeDamage(GameConfig.BULLET_DAMAGE);
        player.heal(1);
        assertEquals(GameConfig.DEFAULT_HEALTH - GameConfig.BULLET_DAMAGE + 1, player.getHealth());

        player.heal(10000);
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
    }

    /** 火力上限 5 级：每吃一个升一级，每一级都真的多打出一发；到顶后不再往上加。 */
    @Test
    void enhanceFirePowerIsCappedAtMax() {
        for (int level = 2; level <= GameConfig.MAX_FIRE_POWER; level++) {
            player.enhanceFirePower();
            assertEquals(level, player.getFirePower());
            assertEquals(level, player.shoot().size(), "火力 " + level + " 级就该打出 " + level + " 发");
        }

        player.enhanceFirePower();          // 已到上限
        assertEquals(GameConfig.MAX_FIRE_POWER, player.getFirePower());
        assertEquals(GameConfig.MAX_FIRE_POWER, player.shoot().size(), "到顶后仍是满级弹幕，不会无限加");
    }

    /** 多发子弹等间距铺在左右机翼之间：既不挤在同一点，也不越出机身外。 */
    @Test
    void shootSpreadsBulletsAcrossTheWings() {
        for (int level = 2; level <= GameConfig.MAX_FIRE_POWER; level++) {
            player.enhanceFirePower();
        }

        List<Bullet> shots = player.shoot();
        assertEquals(GameConfig.MAX_FIRE_POWER, shots.size());
        assertEquals(player.getX() + 8, shots.get(0).getX(), 0.001, "最左一发贴在左机翼内侧");

        double previous = Double.NEGATIVE_INFINITY;
        for (Bullet b : shots) {
            assertTrue(b.getX() > previous, "子弹应自左向右排开，不能重叠在同一点");
            assertTrue(b.getX() >= player.getX(), "子弹不该跑到机身左侧外面");
            assertTrue(b.getX() + Bullet.WIDTH <= player.getX() + player.getWidth(),
                    "子弹不该跑到机身右侧外面");
            previous = b.getX();
        }
    }

    @Test
    void diesWhenHealthReachesZero() {
        int expectedHits = GameConfig.DEFAULT_HEALTH / GameConfig.COLLISION_DAMAGE;  // 100 / 20 = 5，正好归零
        int hits = 0;
        while (player.isAlive() && hits < 100) {
            player.takeDamage(GameConfig.COLLISION_DAMAGE);
            player.update(2.0); // 跳过无敌帧
            hits++;
        }
        assertFalse(player.isAlive());
        assertEquals(0, player.getHealth());
        assertEquals(expectedHits, hits);
    }

    @Test
    void moveToClampsInsideBattlefield() {
        player.moveTo(-500, 9999);
        assertEquals(0, player.getX(), 0.001);
        assertEquals(GameConfig.WINDOW_HEIGHT - player.getHeight(), player.getY(), 0.001);
    }

    // ---------------- 火力强化永久生效（F10） ----------------

    /** 火力强化本局永久：过了旧版 10 秒时限也不会回落单发。 */
    @Test
    void firePowerBoostIsPermanentForTheRun() {
        player.enhanceFirePower();
        assertEquals(2, player.getFirePower());

        player.update(10.0);
        assertEquals(2, player.getFirePower(), "10 秒后不该回落，火力强化是本局永久的");

        player.update(600.0);
        assertEquals(2, player.getFirePower(), "再久也不衰减");
        assertEquals(2, player.shoot().size(), "永久双发：之后每一枪都是两发");
    }

    /** 重复拾取是"永久叠加等级"，不是刷新时限：吃到几次就一直打几发。 */
    @Test
    void pickingUpFirePowerAgainStacksPermanently() {
        player.enhanceFirePower();
        player.update(10.0);

        player.enhanceFirePower();
        assertEquals(3, player.getFirePower(), "再吃一个升到 3 级，且之前那一级不会被时间吃掉");

        player.update(60.0);
        assertEquals(3, player.getFirePower(), "时限机制已删除，等级不再随时间回落");
        assertEquals(3, player.shoot().size(), "3 级就该一直是三发");
    }

    // ---------------- 开局复位（F01） ----------------

    /**
     * 作弊开启后的折磨档玩家侧加成：五连发、9999 血、子弹快一倍——而且每次复位
     * （进入/重开一局）都给回这份底子，所以"所有关卡"都是这个起点，不会打到某一关就掉回常态。
     */
    @Test
    void tormentWithCheatStartsWithFiveShotsMaxHealthAndDoubleBulletSpeed() {
        Player torment = new Player(100, 100, BODY, BODY, Difficulty.TORMENT, true);

        assertEquals(5, torment.getFirePower(), "开作弊的折磨档开局就是 5 级火力");
        assertEquals(5, torment.shoot().size(), "开局第一枪就该是五连发");
        assertEquals(9999, torment.getMaxHealth(), "开作弊的折磨档血量上限为 9999");
        assertEquals(9999, torment.getHealth(), "开局满血");
        assertEquals(5000, torment.shoot().get(0).getDamage(),
                "开作弊的折磨档 5 级单发伤害为 5000（50 × 99.99）");

        // 弹速 2 倍：一发子弹的位移量是一帧内 速度 × 时间，拿相邻两帧的 y 差来量
        double frame = 1.0 / 60.0;
        List<Bullet> shots = new ArrayList<>();
        torment.update(GameConfig.PLAYER_FIRE_INTERVAL);   // 跳过长冷却，保证能开火
        shots.addAll(torment.shoot());
        double before = shots.get(0).getY();
        torment.update(frame);
        for (Bullet b : shots) {
            b.update(frame);
        }
        double moved = before - shots.get(0).getY();
        assertEquals(2 * GameConfig.WINDOW_HEIGHT / GameConfig.PLAYER_FIRE_INTERVAL * frame, moved, 0.5,
                "开作弊的折磨档子弹速度该是基线的两倍");

        // 复位（重开一局 / 进入下一关用的都是它）之后仍是这份底子
        torment.takeDamage(5000);
        torment.reset(100, 100);
        assertEquals(9999, torment.getHealth(), "复位后该回满到 9999");
        assertEquals(5, torment.getFirePower(), "复位后该回到 5 级起步，而不是掉回单发");
    }

    /** 作弊关闭时，折磨档也只是"敌人更狠"：玩家仍是单发、100 血、原速。 */
    @Test
    void tormentWithoutCheatKeepsLegacyPlayerStats() {
        Player torment = new Player(100, 100, BODY, BODY, Difficulty.TORMENT, false);

        assertEquals(1, torment.getFirePower(), "没开作弊就没有五连发");
        assertEquals(1, torment.shoot().size());
        assertEquals(GameConfig.DEFAULT_HEALTH, torment.getMaxHealth(), "没开作弊就没有 9999 血");
        assertEquals(GameConfig.DEFAULT_HEALTH, torment.getHealth());
    }

    /** 切作弊开关立刻生效：同一对象重算后就能打出五连发。 */
    @Test
    void cheatCanBeToggledOnAnExistingPlayer() {
        Player player = new Player(100, 100, BODY, BODY, Difficulty.TORMENT, false);
        assertEquals(1, player.getFirePower());

        player.configureFor(Difficulty.TORMENT, true);
        assertEquals(5, player.getFirePower(), "开作弊后该立刻变五连发");
        assertEquals(9999, player.getMaxHealth());
        assertEquals(9999, player.getHealth(), "重算会把血回满到新上限");

        player.configureFor(Difficulty.TORMENT, false);
        assertEquals(1, player.getFirePower(), "关掉作弊就回到单发");
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getMaxHealth());
    }

    /** 作弊开启后单发伤害 9999：一枪就能击毁任何敌机（含第 10 关最厚的射击机）。 */
    @Test
    void cheatDamageOneShotsTheToughestEnemy() {
        Player cheater = new Player(100, 100, BODY, BODY, Difficulty.TORMENT, true);
        // 1 级口径的伤害就是 9999；这里用满级（50% 档）也远高于任何敌机血量
        int damage = cheater.shoot().get(0).getDamage();
        assertEquals(5000, damage, "5 级单发伤害 5000");

        Player single = new Player(100, 100, BODY, BODY, Difficulty.TORMENT, false);
        assertEquals(GameConfig.BULLET_DAMAGE_BASE, single.shoot().get(0).getDamage(),
                "没开作弊时仍是常规伤害");

        // 用 1 级火力（100%）的作弊伤害做"一枪秒杀"的判定
        int toughHealth = GameConfig.enemyHealthAt(
                GameConfig.SHOOTING_ENEMY_HEALTH, 10, Difficulty.TORMENT);
        assertTrue(GameConfig.playerBulletDamage(1, Difficulty.TORMENT, true) >= toughHealth,
                "9999 伤害该一枪击毁最厚的敌机（血量 " + toughHealth + "）");
    }

    /** 常规三档与旧版一致：单发、100 血、原速。 */
    @Test
    void normalDifficultyKeepsLegacyPlayerStats() {
        Player normal = new Player(100, 100, BODY, BODY, Difficulty.NORMAL);

        assertEquals(1, normal.getFirePower());
        assertEquals(GameConfig.DEFAULT_HEALTH, normal.getMaxHealth());
        assertEquals(GameConfig.DEFAULT_HEALTH, normal.getHealth());

        double frame = 1.0 / 60.0;
        List<Bullet> shots = new ArrayList<>();
        normal.update(GameConfig.PLAYER_FIRE_INTERVAL);
        shots.addAll(normal.shoot());
        double before = shots.get(0).getY();
        normal.update(frame);
        for (Bullet b : shots) {
            b.update(frame);
        }
        assertEquals(GameConfig.WINDOW_HEIGHT / GameConfig.PLAYER_FIRE_INTERVAL * frame,
                before - shots.get(0).getY(), 0.5, "普通档弹速仍是基线（一屏一个射击间隔）");
    }

    @Test
    void resetRestoresRunScopedState() {
        player.enhanceFirePower();
        player.activateShield();
        player.takeDamage(GameConfig.BULLET_DAMAGE);
        player.move(200, 200);

        player.reset(300, 400);

        assertEquals(300, player.getX(), 0.001);
        assertEquals(400, player.getY(), 0.001);
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
        assertEquals(1, player.getFirePower(), "火力强化不该带到新一局");
        assertFalse(player.isShielded(), "护盾不该带到新一局");
        assertFalse(player.isInvincible());
        assertTrue(player.isAlive());
        assertTrue(player.isReadyToShoot());
    }
}
