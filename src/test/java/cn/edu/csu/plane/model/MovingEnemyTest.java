package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MovingEnemy} 的单元测试：覆盖构造属性、向右摆动下落与右边界掉头。
 */
class MovingEnemyTest {

    @Test
    void constructorSetsFields() {
        MovingEnemy e = new MovingEnemy(100, 100);
        assertEquals(EnemyType.MOVING, e.getType());
        assertEquals(1, e.getHealth());
        assertEquals(150, e.getScore());
        assertEquals(110, e.getVelY(), 0.001);
        assertTrue(e.isAlive());
    }

    @Test
    void swaysRightAndFalls() {
        MovingEnemy e = new MovingEnemy(100, 100);
        e.update(1.0);
        assertEquals(230, e.getX(), 0.001); // 100 + 130 * 1 * 1.0
        assertEquals(210, e.getY(), 0.001); // 100 + 110 * 1.0
    }

    @Test
    void turnsAroundAtRightBoundary() {
        MovingEnemy e = new MovingEnemy(GameConfig.WINDOW_WIDTH - 20, 100);
        e.update(1.0);
        // x = 880 + 130 = 1010，超过右边界，被夹到 850 并掉头
        assertEquals(GameConfig.WINDOW_WIDTH - 50, e.getX(), 0.001);

        e.update(1.0);
        // 掉头后向左移动
        assertEquals(GameConfig.WINDOW_WIDTH - 50 - 130, e.getX(), 0.001);
    }
}
