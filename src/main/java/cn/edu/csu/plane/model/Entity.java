package cn.edu.csu.plane.model;

/**
 * 所有游戏实体（战机、敌机、子弹、道具）的抽象基类，封装位置、尺寸、速度与存活状态。
 */
public abstract class Entity {

    protected double x;
    protected double y;
    protected double width;
    protected double height;
    protected double velX;
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

    protected void move() {
        x += velX;
        y += velY;
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
