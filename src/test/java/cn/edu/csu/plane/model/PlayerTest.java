package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Player} 的单元测试：覆盖初始状态、移动边界、射击、冷却、受伤/无敌帧、
 * 护盾、治疗、火力强化与死亡判定。
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
        player.takeDamage(1); // 盾已消耗
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

    // ---------------- 火力强化时限（F10 / Q6） ----------------

    @Test
    void firePowerRevertsAfterPowerUpDuration() {
        player.enhanceFirePower();
        assertEquals(2, player.getFirePower());

        player.update(GameConfig.FIREPOWER_DURATION - 0.1);
        assertEquals(2, player.getFirePower(), "时限内应保持强化");

        player.update(0.2);
        assertEquals(1, player.getFirePower(), "过了时限应回落单发");
    }

    @Test
    void pickingUpFirePowerAgainRefreshesTimer() {
        player.enhanceFirePower();
        player.update(GameConfig.FIREPOWER_DURATION - 0.5);

        player.enhanceFirePower();                              // 再吃一个：刷新计时，不是叠加时长
        player.update(GameConfig.FIREPOWER_DURATION - 0.5);
        assertEquals(3, player.getFirePower(), "再吃一个升到 3 级，且时限重新开始算");

        player.update(0.6);
        assertEquals(1, player.getFirePower(), "刷新后的时限过了照样回落单发");
    }

    // ---------------- 开局复位（F01） ----------------

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
