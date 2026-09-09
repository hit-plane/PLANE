package cn.edu.csu.plane.model;

/**
 * 横移敌机：左右摆动下落，血量低，移速稍快。
 */
public class MovingEnemy extends Enemy {

    private double direction = 1;

    public MovingEnemy(double x, double y) {
        super(x, y, 50, 50, EnemyType.MOVING, 1, 150);
    }

    @Override
    protected void movePattern(double deltaTime) {
        // TODO: 左右摆动 + 下落
    }
}
