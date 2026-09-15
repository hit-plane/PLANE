package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;

import java.util.List;

/**
 * 核心游戏模型接口（接口先行）：向控制层暴露操作与查询，
 * 聚合玩家、敌机、子弹、道具及游戏状态的整体数据。model 层零 javafx 依赖。
 */
public interface GameModel {

    /** 初始化一局：分数归零、血量回满、战机复位、清空场上实体。 */
    void initGame();

    /** 推进一帧：实体移动、敌机生成、碰撞结算。状态非 PLAYING 时整帧冻结。 */
    void update(double deltaTime);

    /** 暂停：冻结实体运动、生成与计分（F12）。 */
    void pause();

    /** 恢复：从暂停回到 PLAYING（F12）。 */
    void resume();

    /** 返回主菜单：清空场上实体并释放本局。 */
    void toMenu();

    /** 移动玩家战机，并限制其在战场边界内。非 PLAYING 状态不响应。 */
    void movePlayer(double dx, double dy);

    /** 返回玩家战机（供视图渲染）。 */
    Player getPlayer();

    /** 返回场上敌机列表。 */
    List<Enemy> getEnemies();

    /** 返回场上子弹列表。 */
    List<Bullet> getBullets();

    /** 返回场上道具列表。 */
    List<Item> getItems();

    /** 返回场上正在扫的炸弹冲击波列表（供视图渲染）。 */
    List<BombWave> getWaves();

    /** 返回场上正在播放的受击特效列表（供视图渲染）。 */
    List<HitEffect> getHitEffects();

    /** 返回当前得分。 */
    int getScore();

    /** 返回玩家当前血量。 */
    int getHealth();

    /** 返回当前关卡。 */
    int getLevel();

    /** 返回历史最高分（F13）；从没打过或存档损坏时为 0。 */
    int getHighScore();

    /** 返回指定难度的历史最高分（F16）。主菜单切换档位时按所选档位取。 */
    int getHighScore(Difficulty difficulty);

    /**
     * 返回指定难度的通关最短用时记录（F24）。三态：未通关 / 超时 / 具体用时。
     * 主菜单切换档位时按所选档位取，与最高分一样各档各记各的。
     */
    ClearTime getBestClearTime(Difficulty difficulty);

    /**
     * 返回本局用时（F24），结果界面据此显示本局成绩；未通关的局同样可用。
     * 已达计时上限时返回"超时"。结算后本局不再推进，所以这个值会定格。
     */
    ClearTime getRunClearTime();

    /** 刚结束的这一局是否刷新了本档的通关最短用时（结算界面据此提示"新纪录"）。 */
    boolean isNewClearRecord();

    /** 返回当前难度（F16），默认普通档。 */
    Difficulty getDifficulty();

    /**
     * 切换难度（F16）。设置后立即生效：敌机血量、掉落概率、生成密度、玩家血量上限/起始火力/
     * 弹速以及最高分显示都按新档位算。
     * 由装配层保证调用时机——主菜单选好后、{@link #initGame()} 开局之前。
     */
    void setDifficulty(Difficulty difficulty);

    /**
     * 开关作弊：关闭时玩家按本档常规数值建机，开启后才享受本档的作弊加成
     * （折磨档的五连发 / 9999 血 / 双倍弹速）。切换后立即重算玩家机，
     * 无需重开一局；血量会回满到新的上限。同样由主菜单在开局前设定。
     */
    void setCheatEnabled(boolean cheatEnabled);

    /** 返回作弊是否已开启。 */
    boolean isCheatEnabled();

    /** 返回当前游戏状态。 */
    GameStatus getStatus();

    /** 返回本局已进行时长（秒）。 */
    double getElapsedTime();

    /** 累加得分并触发关卡判定。 */
    void addScore(int amount);
}
