package cn.edu.csu.plane.model;

/**
 * 玩家战机：负责移动、自动射击、血量、无敌帧、护盾与火力强化。
 */
public class Player extends Entity {

    private int health;
    private int maxHealth;
    private int firePower;
    private double fireRate;
    private double invincibleTimer;
    private boolean shielded;

    public Player(double x, double y, double width, double height) {
        super(x, y, width, height);
        // TODO: 从 GameConfig 初始化默认血量/火力
    }

    @Override
    public void update(double deltaTime) {
        // TODO: 更新移动、无敌帧倒计时、护盾等
    }

    public void move(double deltaX, double deltaY) {
        // TODO: 限制在窗口边界内移动
    }

    public void shoot() {
        // TODO: 依据火力等级生成一颗或多颗子弹
    }

    public void takeDamage(int damage) {
        // TODO: 扣除血量、触发无敌帧、护盾抵扣
    }

    public void heal(int amount) {
        // TODO: 回复血量（不超过上限）
    }

    public void enhanceFirePower() {
        // TODO: 提升火力等级
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getFirePower() { return firePower; }
    public double getFireRate() { return fireRate; }
    public boolean isInvincible() { return invincibleTimer > 0; }
    public boolean isShielded() { return shielded; }
}
