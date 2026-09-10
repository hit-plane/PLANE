package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

import java.util.ArrayList;
import java.util.List;

public class Player extends Entity {

    private static final double BULLET_SPEED = 600;   // 玩家子弹比敌弹快
    private static final double INVINCIBLE_TIME = 1.0;

    private int health;
    private int maxHealth;
    private int firePower;
    private double fireRate;          // 两次开火的间隔（秒）
    private double fireCooldown;      // 距离下次能开火还剩多久
    private double invincibleTimer;
    private boolean shielded;

    public Player(double x, double y, double width, double height) {
        super(x, y, width, height);
        this.maxHealth = GameConfig.DEFAULT_HEALTH;
        this.health = maxHealth;
        this.firePower = 1;
        this.fireRate = 0.5;
        this.fireCooldown = 0;
        this.invincibleTimer = 0;
        this.shielded = false;
    }

    @Override
    public void update(double deltaTime) {
        if (invincibleTimer > 0) {
            invincibleTimer -= deltaTime;
        }
        if (fireCooldown > 0) {
            fireCooldown -= deltaTime;
        }
    }

    /** 按输入的位移量移动战机，顺带把坐标卡在窗口里。 */
    public void move(double deltaX, double deltaY) {
        this.x += deltaX;
        this.y += deltaY;

        if (this.x < 0) this.x = 0;
        if (this.y < 0) this.y = 0;
        if (this.x + this.width > GameConfig.WINDOW_WIDTH) {
            this.x = GameConfig.WINDOW_WIDTH - this.width;
        }
        if (this.y + this.height > GameConfig.WINDOW_HEIGHT) {
            this.y = GameConfig.WINDOW_HEIGHT - this.height;
        }
    }

    /** 冷却结束了没，结束了才能开下一枪。 */
    public boolean isReadyToShoot() {
        return fireCooldown <= 0;
    }

    /**
     * 开火，返回这一枪打出去的子弹。
     * 火力 1 级单发，被火力强化堆到 2 级以上就是左右各一发。
     * 外面（GameModelImpl）拿这个返回值往 bullets 里一塞就行，开火后自动进冷却。
     */
    public List<Bullet> shoot() {
        fireCooldown = fireRate;

        List<Bullet> shots = new ArrayList<>();
        if (firePower <= 1) {
            shots.add(new Bullet(x + width / 2 - Bullet.WIDTH / 2, y, -BULLET_SPEED, 1, true));
        } else {
            shots.add(new Bullet(x + 8, y, -BULLET_SPEED, 1, true));
            shots.add(new Bullet(x + width - 8 - Bullet.WIDTH, y, -BULLET_SPEED, 1, true));
        }
        return shots;
    }

    public void takeDamage(int damage) {
        // 有盾先拿盾顶掉这一次，盾只挡一下
        if (shielded) {
            shielded = false;
            return;
        }

        if (invincibleTimer <= 0) {
            health -= damage;
            invincibleTimer = INVINCIBLE_TIME;
            if (health <= 0) {
                health = 0;
                alive = false;
            }
        }
    }

    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }

    public void enhanceFirePower() {
        firePower++;
    }

    public void activateShield() {
        shielded = true;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getFirePower() { return firePower; }
    public double getFireRate() { return fireRate; }
    public boolean isInvincible() { return invincibleTimer > 0; }
    public boolean isShielded() { return shielded; }
}
