package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Bullet} 的单元测试：覆盖构造属性、玩家/敌方子弹区分、移动与上下越界销毁。
 */
class BulletTest {

    @Test
    void playerBulletConstructor() {
        Bullet b = new Bullet(100, 200, -300, 2, true);
        assertEquals(100, b.getX(), 0.001);
        assertEquals(200, b.getY(), 0.001);
        assertEquals(Bullet.WIDTH, b.getWidth(), 0.001);
        assertEquals(Bullet.HEIGHT, b.getHeight(), 0.001);
        assertEquals(-300, b.getVelY(), 0.001);
        assertEquals(2, b.getDamage());
        assertTrue(b.isPlayerBullet());
        assertTrue(b.isAlive());
    }

    @Test
    void enemyBulletConstructor() {
        Bullet b = new Bullet(100, 200, 300, 1, false);
        assertFalse(b.isPlayerBullet());
        assertEquals(1, b.getDamage());
    }

    @Test
    void movesAlongVelocity() {
        Bullet up = new Bullet(100, 200, -300, 2, true);
        up.update(1.0);
        assertEquals(-100, up.getY(), 0.001); // 200 + (-300) * 1.0
    }

    @Test
    void diesWhenOutOfTop() {
        Bullet up = new Bullet(100, 0, -300, 2, true);
        up.update(1.0); // y = -300，y + height = -286 < 0
        assertFalse(up.isAlive());
    }

    @Test
    void diesWhenOutOfBottom() {
        Bullet down = new Bullet(100, GameConfig.WINDOW_HEIGHT, 300, 1, false);
        down.update(1.0); // y = 700 + 300 = 1000 > 700
        assertFalse(down.isAlive());
    }
}
