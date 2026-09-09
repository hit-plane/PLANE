package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

public class Player extends Entity {

    private int health;
    private int maxHealth;
    private int firePower;
    private double fireRate;
    private double invincibleTimer;
    private boolean shielded;

    public Player(double x, double y, double width, double height) {
        super(x, y, width, height);
        this.maxHealth = GameConfig.DEFAULT_HEALTH;
        this.health = maxHealth;
        this.firePower = 1;
        this.fireRate = 0.5;
        this.invincibleTimer = 0;
        this.shielded = false;
    }

    @Override
    public void update(double deltaTime) {
        if (invincibleTimer > 0) {
            invincibleTimer -= deltaTime;
        }
    }

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

    public void shoot() {
    }

    public void takeDamage(int damage) {
        if (shielded) {
            shielded = false;
            return;
        }
        
        if (invincibleTimer <= 0) {
            health -= damage;
            invincibleTimer = 1.0;
            if (health <= 0) {
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

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getFirePower() { return firePower; }
    public double getFireRate() { return fireRate; }
    public boolean isInvincible() { return invincibleTimer > 0; }
    public boolean isShielded() { return shielded; }
}
