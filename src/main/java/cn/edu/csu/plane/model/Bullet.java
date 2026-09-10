package cn.edu.csu.plane.model;

/**
 * 子弹：区分玩家子弹与敌方子弹，携带伤害值，沿竖直方向飞行。
 */
public class Bullet extends Entity {

    /** 子弹尺寸。之前这里传的是 0，碰撞盒面积为 0，永远判定不上，所以单独拎出来当常量。 */
    public static final double WIDTH = 6;
    public static final double HEIGHT = 14;

    private final int damage;
    private final boolean playerBullet;

    public Bullet(double x, double y, double velY, int damage, boolean playerBullet) {
        super(x, y, WIDTH, HEIGHT);
        this.velY = velY;
        this.damage = damage;
        this.playerBullet = playerBullet;
    }

    @Override
    public void update(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;   // 玩家子弹往上飞、敌弹往下飞，都是出屏就销毁
        }
    }

    public int getDamage() { return damage; }
    public boolean isPlayerBullet() { return playerBullet; }
}
