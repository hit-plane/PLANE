package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 横移敌机：左右摆动下落，血量低，移速稍快。
 */
public class MovingEnemy extends Enemy {

    private static final double SWAY_SPEED = 130;   // 横向摆动速度

    private double direction = 1;

    public MovingEnemy(double x, double y) {
        super(x, y, 50, 50, EnemyType.MOVING, 1, 150);
        this.velY = 110;
    }

    @Override
    protected void movePattern(double deltaTime) {
        // 横着走，撞到窗口两边就掉头
        x += SWAY_SPEED * direction * deltaTime;
        if (x < 0) {
            x = 0;
            direction = 1;
        } else if (x + width > GameConfig.WINDOW_WIDTH) {
            x = GameConfig.WINDOW_WIDTH - width;
            direction = -1;
        }

        y += velY * deltaTime;
        if (isOutsideScreen()) {
            alive = false;
        }
    }
}
