package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Player} 的单元测试：覆盖初始状态、移动边界、射击、冷却、受伤/无敌帧、
 * 护盾、治疗、火力强化与死亡判定。
 */
class PlayerTest {

    private Player player;

    @BeforeEach
    void setUp() {
        player = new Player(100, 100, 50, 50);
    }

    @Test
    void initialState() {
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getMaxHealth());
        assertEquals(1, player.getFirePower());
        assertEquals(0.5, player.getFireRate(), 0.001);
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
        assertEquals(1, b.getDamage());
        assertEquals(-600, b.getVelY(), 0.001);
        assertEquals(122, b.getX(), 0.001); // 100 + 50/2 - 6/2
        assertEquals(100, b.getY(), 0.001);

        assertFalse(player.isReadyToShoot()); // 开火后进入冷却
    }

    @Test
    void shootReturnsTwoBulletsAfterEnhance() {
        player.enhanceFirePower(); // firePower = 2
        List<Bullet> shots = player.shoot();
        assertEquals(2, shots.size());
        assertEquals(108, shots.get(0).getX(), 0.001); // x + 8
        assertEquals(136, shots.get(1).getX(), 0.001); // x + width - 8 - Bullet.WIDTH
    }

    @Test
    void shootEntersCooldownThenRecovers() {
        player.shoot();
        assertFalse(player.isReadyToShoot());

        player.update(0.5); // fireCooldown 0.5 -> 0
        assertTrue(player.isReadyToShoot());
    }

    @Test
    void takeDamageReducesHealthAndGrantsInvincibility() {
        player.takeDamage(1);
        assertEquals(GameConfig.DEFAULT_HEALTH - 1, player.getHealth());
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
        player.takeDamage(5);
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
        player.takeDamage(3);
        player.heal(1);
        assertEquals(GameConfig.DEFAULT_HEALTH - 2, player.getHealth());

        player.heal(100);
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
    }

    @Test
    void enhanceFirePowerIncrements() {
        player.enhanceFirePower();
        assertEquals(2, player.getFirePower());
        player.enhanceFirePower();
        assertEquals(3, player.getFirePower());
    }

    @Test
    void diesWhenHealthReachesZero() {
        for (int i = 0; i < GameConfig.DEFAULT_HEALTH; i++) {
            player.takeDamage(1);
            player.update(2.0); // 跳过无敌帧
        }
        assertFalse(player.isAlive());
        assertEquals(0, player.getHealth());
    }
}
