package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 所有游戏实体（战机、敌机、子弹、道具）的抽象基类，封装位置、尺寸、速度与存活状态。
 */
public abstract class Entity {

    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected double velX;   // 单位：像素/秒
    protected double velY;
    protected boolean alive;

    public Entity(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.alive = true;
    }

    /** 每帧更新实体的位置与状态逻辑。 */
    public abstract void update(double deltaTime);

    /**
     * 按当前速度推进一帧的位置。
     * 速度是按秒算的，所以必须乘 deltaTime，不然换个帧率游戏速度就变了。
     */
    protected void move(double deltaTime) {
        x += velX * deltaTime;
        y += velY * deltaTime;
    }

    /** 是否已经飞出窗口上下边界（子弹、敌机、道具用来判断自己该不该销毁）。 */
    protected boolean isOutsideScreen() {
        return y + height < 0 || y > GameConfig.WINDOW_HEIGHT;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public double getWidth() { return width; }
    public double getHeight() { return height; }
    public double getVelX() { return velX; }
    public double getVelY() { return velY; }
    public boolean isAlive() { return alive; }
    public void setAlive(boolean alive) { this.alive = alive; }
}
