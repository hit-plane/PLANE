package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 自爆敌机：移速快，碰撞玩家造成高额伤害后自身销毁。
 */
public class BomberEnemy extends Enemy {

    public BomberEnemy(double x, double y) {
        super(x, y, GameConfig.ENEMY_SIZE, GameConfig.ENEMY_SIZE,
                EnemyType.BOMBER, GameConfig.BOMBER_ENEMY_HEALTH, GameConfig.BOMBER_ENEMY_SCORE);
        this.velY = GameConfig.BOMBER_ENEMY_SPEED;   // 俯冲，基本一路冲到底
    }

    /** 自爆机撞人的高额伤害，由碰撞结算方读取（与普通机区分）。 */
    public int getCollisionDamage() {
        return GameConfig.BOMBER_COLLISION_DAMAGE;
    }

    @Override
    protected void movePattern(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;
        }
    }
}
