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
        // TODO: 子类固定行为（如射击敌机发射子弹）
        movePattern(deltaTime);
    }

    /** 由具体敌机类型实现移动轨迹。 */
    protected abstract void movePattern(double deltaTime);

    public void takeDamage(int damage) {
        // TODO: 扣血，血量归零则销毁
    }

    public EnemyType getType() { return type; }
    public int getHealth() { return health; }
    public int getScore() { return score; }
}
