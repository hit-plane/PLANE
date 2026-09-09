package cn.edu.csu.plane.model;

/**
 * 自爆敌机：移速快，碰撞玩家造成高额伤害后自身销毁。
 */
public class BomberEnemy extends Enemy {

    public BomberEnemy(double x, double y) {
        super(x, y, 50, 50, EnemyType.BOMBER, 1, 500);
    }

    @Override
    protected void movePattern(double deltaTime) {
        // TODO: 高速向下冲撞
    }
}
