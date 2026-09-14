package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

import java.util.Collections;
import java.util.List;

/**
 * 射击敌机：下落的同时发射子弹，血量中等，分值更高。
 */
public class ShootingEnemy extends Enemy {

    private double fireCooldown;

    public ShootingEnemy(double x, double y) {
        this(x, y, GameConfig.SHOOTING_ENEMY_HEALTH);
    }

    /** 指定血量构造：血量随关卡成长，由 GameModelImpl 按当前关卡算好传进来。 */
    public ShootingEnemy(double x, double y, int health) {
        super(x, y, GameConfig.ENEMY_SIZE, GameConfig.ENEMY_SIZE,
                EnemyType.SHOOTING, health, GameConfig.SHOOTING_ENEMY_SCORE);
        this.velY = GameConfig.SHOOTING_ENEMY_SPEED;
        this.fireCooldown = GameConfig.SHOOTING_ENEMY_FIRE_INTERVAL;
    }

    @Override
    protected void movePattern(double deltaTime) {
        y += velY * deltaTime;
        if (isOutsideScreen()) {
            alive = false;
            return;
        }
        if (fireCooldown > 0) {
            fireCooldown -= deltaTime;
        }
    }

    /**
     * 冷却好了就往正下方打一发，否则返回空列表。
     * 子弹要交给 model 统一收着，所以这里是"谁开火谁交子弹"，敌机自己不保管子弹列表。
     */
    public List<Bullet> shoot() {
        if (fireCooldown > 0) {
            return Collections.emptyList();
        }
        fireCooldown = GameConfig.SHOOTING_ENEMY_FIRE_INTERVAL;
        Bullet bullet = new Bullet(x + width / 2 - Bullet.WIDTH / 2, y + height,
                GameConfig.ENEMY_BULLET_SPEED, GameConfig.BULLET_DAMAGE, false);
        return List.of(bullet);
    }
}
