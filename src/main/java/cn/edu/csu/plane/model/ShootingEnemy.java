package cn.edu.csu.plane.model;

/**
 * 射击敌机：下落的同时发射子弹，血量中等，分值更高。
 */
public class ShootingEnemy extends Enemy {

    public ShootingEnemy(double x, double y) {
        super(x, y, 50, 50, EnemyType.SHOOTING, 3, 300);
    }

    @Override
    protected void movePattern(double deltaTime) {
        // TODO: 下落 + 周期性发射子弹
    }
}
