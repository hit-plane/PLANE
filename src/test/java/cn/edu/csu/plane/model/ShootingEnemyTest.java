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
        assertEquals(3, e.getHealth());
        assertEquals(300, e.getScore());
        assertEquals(90, e.getVelY(), 0.001);
        assertTrue(e.isAlive());
    }

    @Test
    void movesDownward() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        e.update(1.0);
        assertEquals(190, e.getY(), 0.001); // 100 + 90 * 1.0
    }

    @Test
    void shootReturnsEmptyBeforeCooldownElapses() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        assertTrue(e.shoot().isEmpty()); // fireCooldown 初始为 2.0
    }

    @Test
    void shootReturnsEnemyBulletAfterCooldown() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        e.update(2.0); // y = 280，fireCooldown 归零

        List<Bullet> shots = e.shoot();
        assertEquals(1, shots.size());

        Bullet b = shots.get(0);
        assertFalse(b.isPlayerBullet());
        assertEquals(10, b.getDamage());
        assertEquals(260, b.getVelY(), 0.001);
        assertEquals(122, b.getX(), 0.001); // 100 + 50/2 - 6/2
        assertEquals(330, b.getY(), 0.001); // y(280) + height(50)
    }

    @Test
    void shootResetsCooldown() {
        ShootingEnemy e = new ShootingEnemy(100, 100);
        e.update(2.0);
        e.shoot();
        assertTrue(e.shoot().isEmpty()); // 开火后重新进入冷却
    }
}
