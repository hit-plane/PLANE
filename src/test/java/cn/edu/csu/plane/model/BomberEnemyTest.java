package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BomberEnemy} 的单元测试：覆盖构造属性、高速俯冲与越界销毁。
 */
class BomberEnemyTest {

    @Test
    void constructorSetsFields() {
        BomberEnemy e = new BomberEnemy(100, 100);
        assertEquals(EnemyType.BOMBER, e.getType());
        assertEquals(1, e.getHealth());
        assertEquals(500, e.getScore());
        assertEquals(430, e.getVelY(), 0.001);
        assertTrue(e.isAlive());
    }

    @Test
    void divesDownward() {
        BomberEnemy e = new BomberEnemy(100, 0);
        e.update(1.0);
        assertEquals(430, e.getY(), 0.001); // 0 + 430 * 1.0
    }

    @Test
    void diesWhenOutOfBottomBoundary() {
        BomberEnemy e = new BomberEnemy(100, GameConfig.WINDOW_HEIGHT - 10);
        e.update(1.0); // y = 690 + 430 = 1120 > 700
        assertFalse(e.isAlive());
    }
}
