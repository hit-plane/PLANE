package cn.edu.csu.plane.model;

/**
 * 普通敌机：一路直冲，撞到底部就没了。
 */
public class NormalEnemy extends Enemy {

    public NormalEnemy(double x, double y, double width, double height, EnemyType type, int health, int score) {
        super(x, y, width, height, type, health, score);
        this.velY = 150;
    }

    @Override
    protected void movePattern(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;   // 飞出底部直接消失，不给分
        }
    }
}
