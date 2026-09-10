package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 道具：向下匀速移动，落在底线闪烁数秒后消失，玩家接触即可拾取。
 *
 * <p>生命周期两态由 {@code landed} 布尔区分：未着陆（下落中）与已着陆（倒计时闪烁），
 * 两个终态（被拾取 / 超时消失）统一以 {@code alive = false} 表示，由帧末清理移除。</p>
 */
public class Item extends Entity {

    private final ItemType type;
    private double flashTimer;
    private boolean landed;

    public Item(double x, double y, ItemType type) {
        super(x, y, GameConfig.ITEM_SIZE, GameConfig.ITEM_SIZE);
        this.type = type;
        this.velY = GameConfig.ITEM_FALL_SPEED;
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
                flashTimer = GameConfig.ITEM_FLASH_DURATION;
            }
            return;
        }

        flashTimer -= deltaTime;
        if (flashTimer <= 0) {
            alive = false;   // 闪够时间还没人捡，自己消失
        }
    }

    /** 是否已着陆（进入闪烁倒计时阶段）。 */
    public boolean isLanded() { return landed; }

    public ItemType getType() { return type; }
    public double getFlashTimer() { return flashTimer; }
}
