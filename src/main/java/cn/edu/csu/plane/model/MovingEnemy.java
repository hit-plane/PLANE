package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 横移敌机：左右摆动下落，血量低，移速稍快。
 */
public class MovingEnemy extends Enemy {

    private double direction = 1;

    public MovingEnemy(double x, double y) {
        this(x, y, GameConfig.MOVING_ENEMY_HEALTH);
    }

    /** 指定血量构造：血量随关卡成长，由 GameModelImpl 按当前关卡算好传进来。 */
    public MovingEnemy(double x, double y, int health) {
        super(x, y, GameConfig.ENEMY_SIZE, GameConfig.ENEMY_SIZE,
                EnemyType.MOVING, health, GameConfig.MOVING_ENEMY_SCORE);
        this.velY = GameConfig.MOVING_ENEMY_SPEED;
    }

    @Override
    protected void movePattern(double deltaTime) {
        // 横着走，撞到窗口两边就掉头
        x += GameConfig.MOVING_ENEMY_SWAY_SPEED * direction * deltaTime;
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
