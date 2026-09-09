package cn.edu.csu.plane.controller;

import cn.edu.csu.plane.model.Bullet;
import cn.edu.csu.plane.model.Enemy;
import cn.edu.csu.plane.model.Item;
import cn.edu.csu.plane.model.Player;

import java.util.List;

/**
 * 碰撞检测：处理玩家子弹命中敌机、敌方子弹命中玩家、机体碰撞与道具拾取。
 */
public class CollisionManager {

    public void resolve(Player player, List<Enemy> enemies,
                        List<Bullet> bullets, List<Item> items) {
        checkBulletEnemy(bullets, enemies);
        checkBulletPlayer(bullets, player);
        checkPlayerEnemy(player, enemies);
        checkPlayerItem(player, items);
    }

    private void checkBulletEnemy(List<Bullet> bullets, List<Enemy> enemies) {
        // TODO: 玩家子弹命中敌机，扣除血量并加分
    }

    private void checkBulletPlayer(List<Bullet> bullets, Player player) {
        // TODO: 敌方子弹命中玩家，扣除玩家血量
    }

    private void checkPlayerEnemy(Player player, List<Enemy> enemies) {
        // TODO: 机体碰撞，玩家扣血、敌机销毁
    }

    private void checkPlayerItem(Player player, List<Item> items) {
        // TODO: 玩家拾取道具，应用效果
    }
}
