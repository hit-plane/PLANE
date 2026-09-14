package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link MovingEnemy} 的单元测试：覆盖构造属性、向右摆动下落与右边界掉头。
 * 边界断言一律用 {@link GameConfig#ENEMY_SIZE} 表示，不把机宽写死。
 */
class MovingEnemyTest {

    @Test
    void constructorSetsFields() {
        MovingEnemy e = new MovingEnemy(100, 100);
        assertEquals(EnemyType.MOVING, e.getType());
        assertEquals(GameConfig.MOVING_ENEMY_HEALTH, e.getHealth());
        assertEquals(150, e.getScore());
        assertEquals(GameConfig.ENEMY_SIZE, e.getWidth(), 0.001);
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
        double size = GameConfig.ENEMY_SIZE;
        MovingEnemy e = new MovingEnemy(GameConfig.WINDOW_WIDTH - 20, 100);
        e.update(1.0);
        // 向右摆 130 px 就顶到右边界，被夹到"窗口宽 - 机宽"并掉头
        assertEquals(GameConfig.WINDOW_WIDTH - size, e.getX(), 0.001);

        e.update(1.0);
        // 掉头后向左移动
        assertEquals(GameConfig.WINDOW_WIDTH - size - 130, e.getX(), 0.001);
    }
}
