package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 普通敌机：一路直冲，撞到底部就没了。
 */
public class NormalEnemy extends Enemy {

    public NormalEnemy(double x, double y, double width, double height, EnemyType type, int health, int score) {
        super(x, y, width, height, type, health, score);
        this.velY = GameConfig.NORMAL_ENEMY_SPEED;
    }

    /** 按 SRS F08 的普通机血量/分值直接构造（供 GameModelImpl 生成时使用）。 */
    public NormalEnemy(double x, double y) {
        this(x, y, GameConfig.NORMAL_ENEMY_SIZE, GameConfig.NORMAL_ENEMY_SIZE,
                EnemyType.NORMAL, GameConfig.NORMAL_ENEMY_HEALTH, GameConfig.NORMAL_ENEMY_SCORE);
    }

    @Override
    protected void movePattern(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;   // 飞出底部直接消失，不给分
        }
    }
}
