package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.Bullet;
import cn.edu.csu.plane.model.Enemy;
import cn.edu.csu.plane.model.Item;
import cn.edu.csu.plane.model.Player;

import java.util.List;

/**
 * 游戏视图：负责背景与所有游戏实体（战机、敌机、子弹、道具）的渲染。
 */
public class GameView {

    public void render(Player player, List<Enemy> enemies,
                       List<Bullet> bullets, List<Item> items) {
        drawBackground();
        drawEntities(player, enemies, bullets, items);
    }

    private void drawBackground() {
        // TODO: 绘制背景（含背景滚动）
    }

    private void drawEntities(Player player, List<Enemy> enemies,
                              List<Bullet> bullets, List<Item> items) {
        // TODO: 绘制战机、敌机、子弹、道具
    }
}
