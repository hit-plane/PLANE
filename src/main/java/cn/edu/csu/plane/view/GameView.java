package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.Bullet;
import cn.edu.csu.plane.model.Enemy;
import cn.edu.csu.plane.model.Item;
import cn.edu.csu.plane.model.Player;

import java.util.List;

/**
 * 游戏视图：负责背景与所有游戏实体（战机、敌机、子弹、道具）的渲染。
 *
 * <p>v1.0 只固定渲染契约（签名与调用时机），实际绘制待接入 JavaFX Canvas 与
 * {@code resource/pictures} 下的贴图后补齐；此时 render 是空操作，
 * 因此主循环可运转、状态可迁移，但画面尚不显示实体。</p>
 */
public class GameView {

    public void render(Player player, List<Enemy> enemies,
                       List<Bullet> bullets, List<Item> items) {
        drawBackground();
        drawEntities(player, enemies, bullets, items);
    }

    /** 一局结束：展示本局得分（结算面板待实现）。 */
    public void showGameOver(long score) {
        // TODO: 弹出结算界面，显示本局得分与"重新开始/返回主菜单"
    }

    private void drawBackground() {
        // TODO: 绘制背景（含背景滚动）
    }

    private void drawEntities(Player player, List<Enemy> enemies,
                              List<Bullet> bullets, List<Item> items) {
        // TODO: 绘制战机、敌机、子弹、道具
    }
}
