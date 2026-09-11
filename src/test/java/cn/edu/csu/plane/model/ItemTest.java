package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link Item} 的单元测试：覆盖构造属性、下落、触底闪烁与超时消失。
 * 尺寸一律以 {@link GameConfig#ITEM_SIZE} 为准（它已经含 icon.scale），方便以后调图标大小。
 */
class ItemTest {

    @Test
    void constructorSetsFields() {
        Item item = new Item(100, 200, ItemType.FIREPOWER);
        assertEquals(100, item.getX(), 0.001);
        assertEquals(200, item.getY(), 0.001);
        assertEquals(GameConfig.ITEM_SIZE, item.getWidth(), 0.001);
        assertEquals(GameConfig.ITEM_SIZE, item.getHeight(), 0.001);
        assertEquals(ItemType.FIREPOWER, item.getType());
        assertEquals(110, item.getVelY(), 0.001);
        assertEquals(0, item.getFlashTimer(), 0.001);
        assertTrue(item.isAlive());
    }

    @Test
    void fallsDownwardBeforeLanding() {
        Item item = new Item(100, 0, ItemType.HEAL);
        item.update(1.0);
        assertEquals(110, item.getY(), 0.001); // 0 + 110 * 1.0
        assertTrue(item.isAlive());
    }

    @Test
    void landsAndStartsFlashing() {
        Item item = new Item(100, GameConfig.WINDOW_HEIGHT - 40, ItemType.HEAL);
        item.update(1.0); // 再下落 110 px 早已越过底线，被夹到底边
        assertEquals(GameConfig.WINDOW_HEIGHT - GameConfig.ITEM_SIZE, item.getY(), 0.001);
        assertEquals(3.0, item.getFlashTimer(), 0.001);
        assertTrue(item.isAlive());
    }

    @Test
    void disappearsAfterFlashDuration() {
        Item item = new Item(100, GameConfig.WINDOW_HEIGHT - 40, ItemType.HEAL);
        item.update(1.0); // 触底，flashTimer = 3.0
        item.update(3.0); // flashTimer 归零
        assertFalse(item.isAlive());
    }
}
