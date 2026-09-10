package cn.edu.csu.plane.model;

/**
 * 自爆敌机：移速快，碰撞玩家造成高额伤害后自身销毁。
 */
public class BomberEnemy extends Enemy {

    public BomberEnemy(double x, double y) {
        super(x, y, 50, 50, EnemyType.BOMBER, 1, 500);
        this.velY = 430;   // 俯冲，基本一路冲到底
    }

    @Override
    protected void movePattern(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;
        }
    }
}
