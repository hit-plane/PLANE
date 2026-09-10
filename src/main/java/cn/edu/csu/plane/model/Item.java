package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 道具：向下匀速移动，落在底线闪烁数秒后消失，玩家接触即可拾取。
 */
public class Item extends Entity {

    private static final double FLASH_DURATION = 3.0;   // 到线之后闪几秒

    private final ItemType type;
    private double flashTimer;
    private boolean landed;

    public Item(double x, double y, ItemType type) {
        super(x, y, 30, 30);
        this.type = type;
        this.velY = 110;
        this.flashTimer = 0;
        this.landed = false;
    }

    @Override
    public void update(double deltaTime) {
        if (!landed) {
            move(deltaTime);
            // 掉到底边就停在那儿，开始倒计时
            if (y + height >= GameConfig.WINDOW_HEIGHT) {
                y = GameConfig.WINDOW_HEIGHT - height;
                landed = true;
                flashTimer = FLASH_DURATION;
            }
            return;
        }

        flashTimer -= deltaTime;
        if (flashTimer <= 0) {
            alive = false;   // 闪够时间还没人捡，自己消失
        }
    }

    public ItemType getType() { return type; }
    public double getFlashTimer() { return flashTimer; }
}
