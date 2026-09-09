package cn.edu.csu.plane.model;

import java.util.ArrayList;
import java.util.List;

/**
 * GameModel 的实现类：持有实体集合与游戏状态，实现模型的更新与查询逻辑。
 */
public class GameModelImpl implements GameModel {

    private final GameState gameState = new GameState();
    private final Player player = new Player(0, 0, 50, 50);
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();

    @Override
    public void initGame() {
        // TODO: 分数归零、血量回满、清空实体集合
    }

    @Override
    public void update(double deltaTime) {
        // TODO: 移动玩家/敌机/子弹/道具，敌机生成，碰撞结算
    }

    @Override
    public void movePlayer(double dx, double dy) {
        // TODO: 平移玩家并限制边界
    }

    @Override
    public Player getPlayer() { return player; }

    @Override
    public List<Enemy> getEnemies() { return enemies; }

    @Override
    public List<Bullet> getBullets() { return bullets; }

    @Override
    public List<Item> getItems() { return items; }

    @Override
    public int getScore() { return (int) gameState.getScore(); }

    @Override
    public int getHealth() { return player.getHealth(); }

    @Override
    public int getLevel() { return gameState.getLevel(); }

    @Override
    public GameStatus getStatus() { return gameState.getStatus(); }

    @Override
    public void addScore(int amount) {
        // TODO: 累加得分并触发关卡判定
    }
}
