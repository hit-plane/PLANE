package cn.edu.csu.plane.model;

/**
 * 敌机抽象基类：定义血量、分值、类型与移动模式，具体移动轨迹由子类实现。
 */
public abstract class Enemy extends Entity {

    protected EnemyType type;
    protected int health;
    protected int maxHealth;
    protected int score;

    public Enemy(double x, double y, double width, double height, EnemyType type, int health, int score) {
        super(x, y, width, height);
        this.type = type;
        this.health = health;
        this.maxHealth = health;
        this.score = score;
    }

    @Override
    public void update(double deltaTime) {
        movePattern(deltaTime);
    }

    /** 由具体敌机类型实现移动轨迹。 */
    protected abstract void movePattern(double deltaTime);

    /** 挨一发子弹。血扣光就标记为销毁，剩下的交给碰撞结算那边去加分。 */
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            health = 0;
            alive = false;
        }
    }

    public EnemyType getType() { return type; }
    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getScore() { return score; }
}
