package cn.edu.csu.plane.model;

import java.util.Collections;
import java.util.List;

/**
 * 射击敌机：下落的同时发射子弹，血量中等，分值更高。
 */
public class ShootingEnemy extends Enemy {

    private static final double FIRE_INTERVAL = 2.0;    // 每隔几秒打一发
    private static final double BULLET_SPEED = 260;
    private static final int BULLET_DAMAGE = 10;

    private double fireCooldown;

    public ShootingEnemy(double x, double y) {
        super(x, y, 50, 50, EnemyType.SHOOTING, 3, 300);
        this.velY = 90;
        this.fireCooldown = FIRE_INTERVAL;
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
        fireCooldown = FIRE_INTERVAL;
        Bullet bullet = new Bullet(x + width / 2 - Bullet.WIDTH / 2, y + height, BULLET_SPEED, BULLET_DAMAGE, false);
        return List.of(bullet);
    }
}
