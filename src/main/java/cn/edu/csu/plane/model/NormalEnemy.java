package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

public class NormalEnemy extends Enemy {

    public NormalEnemy(double x, double y, double width, double height, EnemyType type, int health, int score) {
        super(x, y, width, height, type, health, score);
        this.velY = 150;
    }

    @Override
    protected void movePattern(double deltaTime) {
        move();
        if (y > GameConfig.WINDOW_HEIGHT) {
            alive = false;
        }
    }

    @Override
    public void takeDamage(int damage) {
        health -= damage;
        if (health <= 0) {
            alive = false;
        }
    }
}
