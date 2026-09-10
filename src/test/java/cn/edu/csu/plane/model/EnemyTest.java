package cn.edu.csu.plane.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Enemy} 抽象基类的单元测试：借助匿名子类验证构造属性与扣血逻辑。
 * 具体移动轨迹由各子类单独测试。
 */
class EnemyTest {

    /** 构造一个仅用于测试的匿名敌机，movePattern 为空实现。 */
    private Enemy enemy(int health, int score) {
        return new Enemy(0, 0, 40, 40, EnemyType.NORMAL, health, score) {
            @Override
            protected void movePattern(double deltaTime) {
            }
        };
    }

    @Test
    void constructorSetsTypeHealthScoreAndMaxHealth() {
        Enemy e = enemy(3, 100);
        assertEquals(EnemyType.NORMAL, e.getType());
        assertEquals(3, e.getHealth());
        assertEquals(3, e.getMaxHealth());
        assertEquals(100, e.getScore());
        assertTrue(e.isAlive());
    }

    @Test
    void takeDamageReducesHealthWithoutDying() {
        Enemy e = enemy(3, 100);
        e.takeDamage(1);
        assertEquals(2, e.getHealth());
        assertTrue(e.isAlive());
    }

    @Test
    void takeDamageClampsHealthToZeroAndDies() {
        Enemy e = enemy(2, 100);
        e.takeDamage(5);
        assertEquals(0, e.getHealth());
        assertFalse(e.isAlive());
    }
}
