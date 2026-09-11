package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * GameModel 的实现：一帧按"自动射击 → 生成敌机 → 实体移动 → 碰撞结算 → 结束判定"推进。
 * 规则全部落在 model 层，不依赖界面即可单独运行。
 */
public class GameModelImpl implements GameModel {

    /** 玩家尺寸与出生点：战场底部居中（GWT-01）。尺寸来自配置，已含图标缩放。 */
    private static final double PLAYER_SIZE = GameConfig.PLAYER_SIZE;
    private static final double PLAYER_START_X = (GameConfig.WINDOW_WIDTH - PLAYER_SIZE) / 2.0;
    private static final double PLAYER_START_Y = GameConfig.WINDOW_HEIGHT - PLAYER_SIZE - 20;

    /** 非火力道具：火力强化之外的三种，掉落时在这三个里等概率挑（F10）。 */
    private static final ItemType[] OTHER_ITEM_TYPES = {ItemType.BOMB, ItemType.SHIELD, ItemType.HEAL};

    private final GameState gameState;
    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final Random random;
    private final HighScoreStore highScoreStore;
    private int highScore;
    private double spawnTimer;

    /** 生产用构造：随机源为默认种子，最高分存到工作目录下的 highscore.txt。 */
    public GameModelImpl() {
        this(new Random(), new HighScoreStore());
    }

    /**
     * 供测试注入固定种子的随机源（同包可见），使机型掷点与道具掉落可复现。
     * 存档仍走默认路径。
     */
    GameModelImpl(Random random) {
        this(random, new HighScoreStore());
    }

    /** 随机源与存档都可注入；测试把存档指到临时目录，免得往工作目录写文件。 */
    GameModelImpl(Random random, HighScoreStore highScoreStore) {
        this.gameState = new GameState();
        this.player = new Player(PLAYER_START_X, PLAYER_START_Y, PLAYER_SIZE, PLAYER_SIZE);
        this.random = random;
        this.highScoreStore = highScoreStore;
        this.highScore = highScoreStore.load();
        this.spawnTimer = 0;
    }

    @Override
    public void initGame() {
        gameState.resetScore();
        gameState.setStatus(GameStatus.PLAYING);
        player.reset(PLAYER_START_X, PLAYER_START_Y);
        enemies.clear();
        bullets.clear();
        items.clear();
        spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime) {
        // 暂停 / 结束 / 还没开局：整帧不动，也不计分（F12/F14）
        if (gameState.getStatus() != GameStatus.PLAYING) {
            return;
        }

        gameState.advanceTime(deltaTime);

        player.update(deltaTime);

        autoShoot();
        updateSpawn(deltaTime);

        updateEntities(enemies, deltaTime);
        collectEnemyBullets();
        updateEntities(bullets, deltaTime);
        updateEntities(items, deltaTime);

        checkCollisions();
        removeDeadEntities();

        // 死亡优先于通关：同一帧里既被打光血、又刚好够通关分时，按失败算
        if (!player.isAlive()) {
            finishGame(GameStatus.GAME_OVER);
            return;
        }

        if (gameState.getScore() >= GameConfig.VICTORY_SCORE) {
            finishGame(GameStatus.VICTORY);
        }
    }

    /** 一局结束：冻结状态，并把本局成绩刷进最高分存档（F09 / F13）。 */
    private void finishGame(GameStatus status) {
        gameState.setStatus(status);
        if (gameState.getScore() > highScore) {
            highScore = (int) gameState.getScore();
            highScoreStore.save(highScore);
        }
    }

    @Override
    public void pause() {
        if (gameState.getStatus() == GameStatus.PLAYING) {
            gameState.setStatus(GameStatus.PAUSED);
        }
    }

    @Override
    public void resume() {
        if (gameState.getStatus() == GameStatus.PAUSED) {
            gameState.setStatus(GameStatus.PLAYING);
        }
    }

    /** 回到主菜单：清空场上实体并释放本局，供状态机 T10 使用。 */
    @Override
    public void toMenu() {
        enemies.clear();
        bullets.clear();
        items.clear();
        spawnTimer = 0;
        gameState.setStatus(GameStatus.MENU);
    }

    /** 玩家不做手动发射，进一局后按 fireRate 自动连发（F03）。 */
    private void autoShoot() {
        if (player.isReadyToShoot()) {
            bullets.addAll(player.shoot());
        }
    }

    private void updateSpawn(double deltaTime) {
        spawnTimer += deltaTime;
        if (spawnTimer >= currentSpawnInterval()) {
            spawnTimer = 0;
            enemies.add(spawnEnemy());
        }
    }

    /**
     * 关卡越高，敌机出得越密（F11 难度递增）。
     * 起始间隔比 v1.0 定的 2.0 秒更短，同屏敌机数大约翻一倍——怪密度整体上调。
     */
    private double currentSpawnInterval() {
        double interval = GameConfig.SPAWN_INTERVAL_BASE
                - (gameState.getLevel() - 1) * GameConfig.SPAWN_INTERVAL_STEP;
        return Math.max(interval, GameConfig.SPAWN_INTERVAL_MIN);
    }

    /** 从顶部随机横坐标生成一架随机机型的敌机（F04）。 */
    private Enemy spawnEnemy() {
        EnemyType type = randomType();
        double size = (type == EnemyType.NORMAL) ? GameConfig.NORMAL_ENEMY_SIZE : GameConfig.ENEMY_SIZE;
        double x = random.nextDouble() * (GameConfig.WINDOW_WIDTH - size);
        double y = -size;

        return switch (type) {
            case MOVING -> new MovingEnemy(x, y);
            case SHOOTING -> new ShootingEnemy(x, y);
            case BOMBER -> new BomberEnemy(x, y);
            default -> new NormalEnemy(x, y);
        };
    }

    /**
     * 按当前关卡掷机型：第 1 关清一色普通敌机，
     * 每升 1 关给横移/射击/自爆各加一份权重，高级机型出现得越来越频繁（F11）。
     */
    private EnemyType randomType() {
        double advanced = Math.min(gameState.getLevel() - 1, GameConfig.MAX_ADVANCED_WEIGHT);
        double normalWeight = GameConfig.NORMAL_TYPE_WEIGHT;
        double total = normalWeight + advanced * 3;

        double roll = random.nextDouble() * total;
        if (roll < normalWeight) {
            return EnemyType.NORMAL;
        }
        if (roll < normalWeight + advanced) {
            return EnemyType.MOVING;
        }
        if (roll < normalWeight + advanced * 2) {
            return EnemyType.SHOOTING;
        }
        return EnemyType.BOMBER;
    }

    /** 射击敌机开出的敌弹要交回统一子弹列表保管，敌机自己不存子弹。 */
    private void collectEnemyBullets() {
        for (Enemy enemy : enemies) {
            if (enemy instanceof ShootingEnemy shootingEnemy) {
                bullets.addAll(shootingEnemy.shoot());
            }
        }
    }

    private void updateEntities(List<? extends Entity> entities, double deltaTime) {
        for (Entity entity : entities) {
            entity.update(deltaTime);
        }
    }

    private void removeDeadEntities() {
        enemies.removeIf(enemy -> !enemy.isAlive());
        bullets.removeIf(bullet -> !bullet.isAlive());
        items.removeIf(item -> !item.isAlive());
    }

    private void checkCollisions() {
        checkBulletEnemy();
        checkBulletPlayer();
        checkPlayerEnemy();
        checkPlayerItem();
    }

    /**
     * 玩家子弹命中敌机：敌机扣血、子弹销毁，击毁则计分并按概率掉落道具（F06/F08/F10）。
     * 一发子弹只结算一架敌机（命中即 break），保证计分账目与 E06 的"子弹销毁"语义一一对应。
     */
    private void checkBulletEnemy() {
        for (Bullet bullet : bullets) {
            if (!bullet.isPlayerBullet() || !bullet.isAlive()) {
                continue;
            }
            for (Enemy enemy : enemies) {
                if (!enemy.isAlive() || !isColliding(bullet, enemy)) {
                    continue;
                }
                bullet.setAlive(false);
                enemy.takeDamage(bullet.getDamage());
                if (!enemy.isAlive()) {
                    addScore(enemy.getScore());
                    dropItem(enemy);
                }
                break;   // 一发子弹只结算一架敌机
            }
        }
    }

    /** 敌方子弹命中玩家：扣玩家血量并销毁子弹（F07）。 */
    private void checkBulletPlayer() {
        if (!player.isAlive()) {
            return;
        }
        for (Bullet bullet : bullets) {
            if (bullet.isPlayerBullet() || !bullet.isAlive()) {
                continue;
            }
            if (isColliding(bullet, player)) {
                bullet.setAlive(false);
                player.takeDamage(bullet.getDamage());
            }
        }
    }

    /** 机体碰撞：玩家按机型受对应伤害、敌机销毁；护盾与无敌帧在 Player 内部消化（F07）。 */
    private void checkPlayerEnemy() {
        if (!player.isAlive()) {
            return;
        }
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || !isColliding(player, enemy)) {
                continue;
            }
            enemy.setAlive(false);
            player.takeDamage(enemy.getCollisionDamage());
        }
    }

    /** 玩家碰到道具即拾取（F10）。 */
    private void checkPlayerItem() {
        if (!player.isAlive()) {
            return;
        }
        for (Item item : items) {
            if (!item.isAlive() || !isColliding(player, item)) {
                continue;
            }
            item.setAlive(false);
            applyItem(item.getType());
        }
    }

    /** 击毁敌机时按概率掉一件道具（F10）。 */
    private void dropItem(Enemy enemy) {
        if (random.nextDouble() >= GameConfig.ITEM_DROP_RATE) {
            return;
        }
        items.add(new Item(enemy.getX(), enemy.getY(), randomItemType()));
    }

    /**
     * 掷道具类型：先按 {@code item.firepower.rate} 决定是不是"火力强化"，
     * 不是的话再在炸弹/护盾/回血三种里等概率挑一个。
     * 所以火力强化是四种道具里最常见的一种，不会出现"想升火力却老掉血包"（Q6）。
     */
    private ItemType randomItemType() {
        if (random.nextDouble() < GameConfig.ITEM_FIREPOWER_RATE) {
            return ItemType.FIREPOWER;
        }
        return OTHER_ITEM_TYPES[random.nextInt(OTHER_ITEM_TYPES.length)];
    }

    /** 道具即时生效：炸弹清屏、护盾挡一次、回血、火力强化升双发（F10）。 */
    private void applyItem(ItemType type) {
        switch (type) {
            case FIREPOWER -> player.enhanceFirePower();
            case HEAL -> player.heal(GameConfig.HEAL_AMOUNT);
            case SHIELD -> player.activateShield();
            case BOMB -> clearBattlefield();
            default -> throw new IllegalStateException("未处理的道具类型: " + type);
        }
    }

    /** 全屏炸弹：清掉场上所有敌机与敌弹，并按各机分值计入得分（不掉落道具）。 */
    private void clearBattlefield() {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                addScore(enemy.getScore());
            }
        }
        enemies.clear();
        bullets.removeIf(bullet -> !bullet.isPlayerBullet());
    }

    /** AABB 相交判定：四类碰撞共用一条判据。 */
    boolean isColliding(Entity a, Entity b) {
        return a.getX() < b.getX() + b.getWidth() &&
               a.getX() + a.getWidth() > b.getX() &&
               a.getY() < b.getY() + b.getHeight() &&
               a.getY() + a.getHeight() > b.getY();
    }

    @Override
    public void movePlayer(double dx, double dy) {
        if (gameState.getStatus() != GameStatus.PLAYING) {
            return;   // 暂停或已结束时不再响应移动（F12/F14）
        }
        player.move(dx, dy);
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
    public int getHighScore() { return highScore; }

    @Override
    public GameStatus getStatus() { return gameState.getStatus(); }

    /** 本局已进行时长（秒）。 */
    @Override
    public double getElapsedTime() { return gameState.getElapsedTime(); }

    @Override
    public void addScore(int amount) {
        gameState.addScore(amount);
    }
}
