package cn.edu.csu.plane.model;

/**
 * 道具：向下匀速移动，落在底线闪烁数秒后消失，玩家接触即可拾取。
 */
public class Item extends Entity {

    private final ItemType type;
    private double flashTimer;

    public Item(double x, double y, ItemType type) {
        super(x, y, 30, 30);
        this.type = type;
    }

    @Override
    public void update(double deltaTime) {
        // TODO: 底线闪烁倒计时
        move();
    }

    public ItemType getType() { return type; }
    public double getFlashTimer() { return flashTimer; }
}
