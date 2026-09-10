package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link BomberEnemy} 的单元测试：覆盖构造属性、高速俯冲、越界销毁与高额碰撞伤害。
 */
class BomberEnemyTest {

    @Test
    void constructorSetsFields() {
        BomberEnemy e = new BomberEnemy(100, 100);
        assertEquals(EnemyType.BOMBER, e.getType());
        assertEquals(GameConfig.BOMBER_ENEMY_HEALTH, e.getHealth());
        assertEquals(GameConfig.BOMBER_ENEMY_SCORE, e.getScore());
        assertEquals(GameConfig.BOMBER_ENEMY_SPEED, e.getVelY(), 0.001);
        assertTrue(e.isAlive());
    }

    @Test
    void divesDownward() {
        BomberEnemy e = new BomberEnemy(100, 0);
        e.update(1.0);
        assertEquals(GameConfig.BOMBER_ENEMY_SPEED, e.getY(), 0.001);
    }

    @Test
    void diesWhenOutOfBottomBoundary() {
        BomberEnemy e = new BomberEnemy(100, GameConfig.WINDOW_HEIGHT - 10);
        e.update(1.0); // y = 690 + 430 = 1120 > 700
        assertFalse(e.isAlive());
    }

    /** 自爆机撞玩家的伤害必须高于普通机（SRS 3.2：自爆敌机造成高额伤害）。 */
    @Test
    void dealsHigherCollisionDamageThanNormalEnemy() {
        BomberEnemy bomber = new BomberEnemy(100, 100);
        NormalEnemy normal = new NormalEnemy(100, 100);
        assertEquals(GameConfig.BOMBER_COLLISION_DAMAGE, bomber.getCollisionDamage());
        assertTrue(bomber.getCollisionDamage() > normal.getCollisionDamage());
    }
}
