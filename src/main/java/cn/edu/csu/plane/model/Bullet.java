package cn.edu.csu.plane.model;

/**
 * 子弹：区分玩家子弹与敌方子弹，携带伤害值，沿竖直方向飞行。
 */
public class Bullet extends Entity {

    private final int damage;
    private final boolean playerBullet;

    public Bullet(double x, double y, double velY, int damage, boolean playerBullet) {
        super(x, y, 0, 0);
        this.velY = velY;
        this.damage = damage;
        this.playerBullet = playerBullet;
    }

    @Override
    public void update(double deltaTime) {
        // TODO: 越界判定
        move();
    }

    public int getDamage() { return damage; }
    public boolean isPlayerBullet() { return playerBullet; }
}
