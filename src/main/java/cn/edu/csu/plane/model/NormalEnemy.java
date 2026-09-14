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

    /** 按 SRS F08 的普通机基准血量/分值构造（不随关卡成长的场合用，如测试）。 */
    public NormalEnemy(double x, double y) {
        this(x, y, GameConfig.NORMAL_ENEMY_HEALTH);
    }

    /**
     * 指定血量构造：血量随关卡成长，由 GameModelImpl 按当前关卡算好传进来
     * （{@link GameConfig#enemyHealthAt}）。
     */
    public NormalEnemy(double x, double y, int health) {
        this(x, y, GameConfig.NORMAL_ENEMY_SIZE, GameConfig.NORMAL_ENEMY_SIZE,
                EnemyType.NORMAL, health, GameConfig.NORMAL_ENEMY_SCORE);
    }

    @Override
    protected void movePattern(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;   // 飞出底部直接消失，不给分
        }
    }
}
