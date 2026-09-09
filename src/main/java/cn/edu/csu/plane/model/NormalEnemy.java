package cn.edu.csu.plane.model;

/**
 * 普通敌机：直线向下俯冲，血量低，移速中等，击毁得基础分。
 */
public class NormalEnemy extends Enemy {

    public NormalEnemy(double x, double y) {
        super(x, y, 50, 50, EnemyType.NORMAL, 1, 100);
    }

    @Override
    protected void movePattern(double deltaTime) {
        // TODO: 直线向下
    }
}
