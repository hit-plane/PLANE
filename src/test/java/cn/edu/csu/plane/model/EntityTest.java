package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Entity} 抽象基类的单元测试：借助匿名子类验证构造、位置推进与出屏判定。
 */
class EntityTest {

    /** 构造一个仅用于测试的匿名实体，update 为空实现。 */
    private Entity entity(double x, double y, double width, double height) {
        return new Entity(x, y, width, height) {
            @Override
            public void update(double deltaTime) {
            }
        };
    }

    @Test
    void constructorSetsPositionSizeAndAlive() {
        Entity e = entity(10, 20, 30, 40);
        assertEquals(10, e.getX(), 0.001);
        assertEquals(20, e.getY(), 0.001);
        assertEquals(30, e.getWidth(), 0.001);
        assertEquals(40, e.getHeight(), 0.001);
        assertTrue(e.isAlive());
    }

    @Test
    void moveAdvancesPositionByVelocityTimesDeltaTime() {
        Entity e = entity(0, 0, 10, 10);
        e.velX = 100;
        e.velY = -50;
        e.move(0.5);
        assertEquals(50, e.getX(), 0.001);
        assertEquals(-25, e.getY(), 0.001);
    }

    @Test
    void isOutsideScreenAboveTop() {
        Entity e = entity(0, -11, 10, 10); // y + height = -1 < 0
        assertTrue(e.isOutsideScreen());
    }

    @Test
    void isOutsideScreenBelowBottom() {
        Entity e = entity(0, GameConfig.WINDOW_HEIGHT + 1, 10, 10);
        assertTrue(e.isOutsideScreen());
    }

    @Test
    void isInsideScreen() {
        Entity e = entity(0, 100, 10, 10);
        assertFalse(e.isOutsideScreen());
    }

    @Test
    void setAliveTogglesState() {
        Entity e = entity(0, 0, 10, 10);
        e.setAlive(false);
        assertFalse(e.isAlive());
        e.setAlive(true);
        assertTrue(e.isAlive());
    }
}
