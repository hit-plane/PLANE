package cn.edu.csu.plane.model;

import java.util.List;

/**
 * 核心游戏模型接口（接口先行）：向控制层暴露操作与查询，
 * 聚合玩家、敌机、子弹、道具及游戏状态的整体数据。model 层零 javafx 依赖。
 */
public interface GameModel {

    /** 初始化一局：分数归零、血量回满、清空场上实体。 */
    void initGame();

    /** 推进一帧：实体移动、敌机生成、碰撞结算。状态非 PLAYING 时整帧冻结。 */
    void update(double deltaTime);

    /** 暂停：冻结实体运动、生成与计分（F12）。 */
    void pause();

    /** 恢复：从暂停回到 PLAYING（F12）。 */
    void resume();

    /** 移动玩家战机，并限制其在战场边界内。 */
    void movePlayer(double dx, double dy);

    /** 返回玩家战机（供视图渲染）。 */
    Player getPlayer();

    /** 返回场上敌机列表。 */
    List<Enemy> getEnemies();

    /** 返回场上子弹列表。 */
    List<Bullet> getBullets();

    /** 返回场上道具列表。 */
    List<Item> getItems();

    /** 返回当前得分。 */
    int getScore();

    /** 返回玩家当前血量。 */
    int getHealth();

    /** 返回当前关卡。 */
    int getLevel();

    /** 返回当前游戏状态。 */
    GameStatus getStatus();

    /** 累加得分并触发关卡判定。 */
    void addScore(int amount);
}
