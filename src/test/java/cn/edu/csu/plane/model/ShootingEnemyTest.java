package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ShootingEnemy} 的单元测试：覆盖构造属性、下落移动、射击冷却与发射子弹。
 */
class ShootingEnemyTest {

    @Test
    void constructorSetsFields() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        assertEquals(EnemyType.SHOOTING, e.getType());
        assertEquals(GameConfig.SHOOTING_ENEMY_HEALTH, e.getHealth());
        assertEquals(GameConfig.SHOOTING_ENEMY_SCORE, e.getScore());
        assertEquals(GameConfig.SHOOTING_ENEMY_SPEED, e.getVelY(), 0.001);
        assertTrue(e.isAlive());
    }

    @Test
    void movesDownward() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        e.update(1.0);
        assertEquals(100 + GameConfig.SHOOTING_ENEMY_SPEED, e.getY(), 0.001);
    }

    @Test
    void shootReturnsEmptyBeforeCooldownElapses() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        assertTrue(e.shoot().isEmpty()); // 初始就处于冷却中
    }

    @Test
    void shootReturnsEnemyBulletAfterCooldown() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        e.update(GameConfig.SHOOTING_ENEMY_FIRE_INTERVAL); // 冷却走完

        List<Bullet> shots = e.shoot();
        assertEquals(1, shots.size());

        Bullet b = shots.get(0);
        assertFalse(b.isPlayerBullet());
        assertEquals(GameConfig.BULLET_DAMAGE, b.getDamage());
        assertEquals(GameConfig.ENEMY_BULLET_SPEED, b.getVelY(), 0.001);
        assertEquals(122, b.getX(), 0.001); // 100 + 50/2 - 6/2
        assertEquals(e.getY() + GameConfig.ENEMY_SIZE, b.getY(), 0.001); // 机头下方
    }

    @Test
    void shootResetsCooldown() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        e.update(GameConfig.SHOOTING_ENEMY_FIRE_INTERVAL);
        e.shoot();
        assertTrue(e.shoot().isEmpty()); // 开火后重新进入冷却
    }
}
