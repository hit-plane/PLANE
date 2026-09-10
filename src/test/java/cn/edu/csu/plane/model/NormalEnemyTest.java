package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link NormalEnemy} 的单元测试：覆盖构造属性、下落移动与越界销毁。
 */
class NormalEnemyTest {

    @Test
    void constructorSetsFields() {
        NormalEnemy e = new NormalEnemy(100, 100, 40, 40, EnemyType.NORMAL, 2, 50);
        assertEquals(EnemyType.NORMAL, e.getType());
        assertEquals(2, e.getHealth());
        assertEquals(50, e.getScore());
        assertEquals(150, e.getVelY(), 0.001);
        assertTrue(e.isAlive());
    }

    @Test
    void movesDownwardByVelocityTimesDeltaTime() {
        NormalEnemy e = new NormalEnemy(100, 0, 40, 40, EnemyType.NORMAL, 2, 50);
        e.update(1.0);
        assertEquals(150, e.getY(), 0.001); // 0 + 150 * 1.0
        assertEquals(100, e.getX(), 0.001);
    }

    @Test
    void diesWhenOutOfBottomBoundary() {
        NormalEnemy e = new NormalEnemy(100, GameConfig.WINDOW_HEIGHT - 10, 40, 40, EnemyType.NORMAL, 2, 50);
        e.update(1.0); // y = 690 + 150 = 840 > 700
        assertFalse(e.isAlive());
    }
}
