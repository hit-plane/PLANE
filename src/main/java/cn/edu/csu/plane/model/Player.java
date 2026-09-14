package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家战机：血量、火力、射击节奏、无敌帧与护盾。
 * 位置由控制层的位移指令驱动（不是按速度自动前进），四边夹紧在战场内。
 */
public class Player extends Entity {

    /** 玩家子弹速度：保证子弹恰好在一个射击间隔内飞完一屏（时间 × 速度 = 距离），
     *  即子弹出屏时刻与下一发子弹生成时刻一致，不会出现同屏两颗玩家弹。 */
    private static final double BULLET_SPEED = GameConfig.WINDOW_HEIGHT / GameConfig.PLAYER_FIRE_INTERVAL;

    /** 机翼两侧留出的最小边距（像素）：多发子弹从这儿开始往机翼中间排，不会贴着图边飞出去。 */
    private static final double WING_MARGIN = 8;

    private int health;
    private int maxHealth;
    private int firePower;
    private double firePowerTimer;    // 火力强化还剩几秒（Q6：10 秒后回落单发）
    private double fireRate;          // 两次开火的间隔（秒）
    private double fireCooldown;      // 距离下次能开火还剩多久
    private double invincibleTimer;
    private boolean shielded;

    public Player(double x, double y, double width, double height) {
        super(x, y, width, height);
        this.maxHealth = GameConfig.DEFAULT_HEALTH;
        this.health = maxHealth;
        this.firePower = 1;
        this.firePowerTimer = 0;
        this.fireRate = GameConfig.PLAYER_FIRE_INTERVAL;
        this.fireCooldown = 0;
        this.invincibleTimer = 0;
        this.shielded = false;
    }

    /** 回到一局的初始状态：满血、单发、无盾、无冷却，坐标复位到底部中央（F01）。 */
    public void reset(double x, double y) {
        moveTo(x, y);
        this.health = maxHealth;
        this.firePower = 1;
        this.firePowerTimer = 0;
        this.fireCooldown = 0;
        this.invincibleTimer = 0;
        this.shielded = false;
        this.alive = true;
    }

    @Override
    public void update(double deltaTime) {
        if (invincibleTimer > 0) {
            invincibleTimer -= deltaTime;
        }
        if (fireCooldown > 0) {
            fireCooldown -= deltaTime;
        }
        // 火力强化是有时限的，到点掉回单发（F10 / Q6）
        if (firePowerTimer > 0) {
            firePowerTimer -= deltaTime;
            if (firePowerTimer <= 0) {
                firePowerTimer = 0;
                firePower = 1;
            }
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

    /** 把战机放到指定坐标并夹紧在战场内（开局与重开时复位用）。 */
    @Override
    public void moveTo(double x, double y) {
        super.moveTo(x, y);
        move(0, 0);   // 复用四边夹紧逻辑
    }

    /** 冷却结束了没，结束了才能开下一枪。 */
    public boolean isReadyToShoot() {
        return fireCooldown <= 0;
    }

    /**
     * 开火，返回这一枪打出去的子弹：火力几级就打几发，最多 {@link GameConfig#MAX_FIRE_POWER} 发。
     *
     * <p>1 级机头正中一发；2 级及以上把 n 发等间距铺在左右机翼之间，所以 2 级恰好是
     * "左右各一发"，等级越高弹幕越宽。子弹伤害不随等级变——提火力靠的是多发覆盖，
     * 不是单发变强，这样每发子弹的伤害账目始终是 1 点。</p>
     *
     * <p>外面（GameModelImpl）拿这个返回值往 bullets 里一塞就行，开火后自动进冷却。</p>
     */
    public List<Bullet> shoot() {
        fireCooldown = fireRate;

        int shotsToFire = Math.max(1, Math.min(firePower, GameConfig.MAX_FIRE_POWER));
        if (shotsToFire == 1) {
            return List.of(new Bullet(x + width / 2 - Bullet.WIDTH / 2, y, -BULLET_SPEED, 1, true));
        }

        // 从"左机翼 + 边距"等间距排到"右机翼 - 边距"；n = 2 时正好落回左右各一发
        double step = (width - WING_MARGIN * 2 - Bullet.WIDTH) / (shotsToFire - 1);
        List<Bullet> shots = new ArrayList<>(shotsToFire);
        for (int i = 0; i < shotsToFire; i++) {
            shots.add(new Bullet(x + WING_MARGIN + step * i, y, -BULLET_SPEED, 1, true));
        }
        return shots;
    }

    /**
     * 受击结算，优先级链：护盾 → 无敌帧 → 扣血。
     * 有盾则消耗盾且不进入无敌（盾是独立的一次资源）；无敌期间伤害被完全忽略且
     * 不刷新计时（1 秒是硬上限，不能靠连续受击延长）。
     */
    public void takeDamage(int damage) {
        if (shielded) {
            shielded = false;
            return;
        }

        if (invincibleTimer <= 0) {
            health -= damage;
            invincibleTimer = GameConfig.PLAYER_INVINCIBLE_TIME;
            if (health <= 0) {
                health = 0;
                alive = false;
            }
        }
    }

    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }

    /**
     * 火力强化：升到上限后不再累加，避免出现既无效果又无限增长的等级。
     * 同时把 10 秒时限重新计时——重复吃道具是刷新时间，不是叠加时长（Q6）。
     */
    public void enhanceFirePower() {
        firePower = Math.min(firePower + 1, GameConfig.MAX_FIRE_POWER);
        firePowerTimer = GameConfig.FIREPOWER_DURATION;
    }

    public void activateShield() {
        shielded = true;
    }

    /** 清除所有临时效果（火力强化、护盾、无敌帧），一局结束时调用。 */
    public void clearPowerUps() {
        firePower = 1;
        firePowerTimer = 0;
        shielded = false;
        invincibleTimer = 0;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getFirePower() { return firePower; }
    public double getFireRate() { return fireRate; }
    public boolean isInvincible() { return invincibleTimer > 0; }
    public boolean isShielded() { return shielded; }
}
