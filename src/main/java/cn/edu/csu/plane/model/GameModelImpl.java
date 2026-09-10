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

    /** 机体碰撞伤害：与敌机相撞玩家扣这么多，敌机同时销毁。 */
    private static final int COLLISION_DAMAGE = 20;
    /** 回血道具的回复量。 */
    private static final int HEAL_AMOUNT = 30;
    /** 击毁敌机掉落道具的概率。 */
    private static final double ITEM_DROP_RATE = 0.2;

    /** 敌机生成间隔：随关卡缩短，但不快过下限。 */
    private static final double SPAWN_INTERVAL_BASE = 2.0;
    private static final double SPAWN_INTERVAL_MIN = 0.6;
    private static final double SPAWN_INTERVAL_STEP = 0.15;
    /** 关卡的机型权重：普通机固定权重，每升 1 关给三种高级机型各加一份。 */
    private static final double NORMAL_WEIGHT = 3.0;
    private static final double MAX_ADVANCED_WEIGHT = 4.0;

    /** 普通敌机由外部传入尺寸/血量/分值，这里给一组默认值。 */
    private static final double NORMAL_ENEMY_SIZE = 40;
    private static final int NORMAL_ENEMY_HEALTH = 2;
    private static final int NORMAL_ENEMY_SCORE = 50;
    /** 横移/射击/自爆三种高级机型的固定尺寸。 */
    private static final double ENEMY_SIZE = 50;

    private final GameState gameState = new GameState();
    private final Player player = new Player(400, 600, 50, 50);
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final Random random = new Random();
    private double spawnTimer = 0;

    @Override
    public void initGame() {
        gameState.resetScore();
        gameState.setStatus(GameStatus.PLAYING);
        player.heal(player.getMaxHealth());
        player.setAlive(true);
        enemies.clear();
        bullets.clear();
        items.clear();
        spawnTimer = 0;
    }

    @Override
    public void update(double deltaTime) {
        // 暂停 / 结束 / 还没开局：整帧不动，也不计分（F12）
        if (gameState.getStatus() != GameStatus.PLAYING) {
            return;
        }

        player.update(deltaTime);

        autoShoot();
        updateSpawn(deltaTime);

        updateEntities(enemies, deltaTime);
        collectEnemyBullets();
        updateEntities(bullets, deltaTime);
        updateEntities(items, deltaTime);

        checkCollisions();
        removeDeadEntities();

        if (!player.isAlive()) {
            gameState.setStatus(GameStatus.GAME_OVER);
            return;
        }

        if (gameState.getScore() >= 500) {
            gameState.setStatus(GameStatus.VICTORY);
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

    /** 关卡越高，敌机出得越密（F11 难度递增）。 */
    private double currentSpawnInterval() {
        double interval = SPAWN_INTERVAL_BASE
                - (gameState.getLevel() - 1) * SPAWN_INTERVAL_STEP;
        return Math.max(interval, SPAWN_INTERVAL_MIN);
    }

    /** 从顶部随机横坐标生成一架随机机型的敌机（F04）。 */
    private Enemy spawnEnemy() {
        EnemyType type = randomType();
        double size = (type == EnemyType.NORMAL) ? NORMAL_ENEMY_SIZE : ENEMY_SIZE;
        double x = random.nextDouble() * (GameConfig.WINDOW_WIDTH - size);
        double y = -size;

        return switch (type) {
            case MOVING -> new MovingEnemy(x, y);
            case SHOOTING -> new ShootingEnemy(x, y);
            case BOMBER -> new BomberEnemy(x, y);
            default -> new NormalEnemy(x, y, NORMAL_ENEMY_SIZE, NORMAL_ENEMY_SIZE,
                    EnemyType.NORMAL, NORMAL_ENEMY_HEALTH, NORMAL_ENEMY_SCORE);
        };
    }

    /**
     * 按当前关卡掷机型：第 1 关清一色普通敌机，
     * 每升 1 关给横移/射击/自爆各加一份权重，高级机型出现得越来越频繁（F11）。
     */
    private EnemyType randomType() {
        double advanced = Math.min(gameState.getLevel() - 1, MAX_ADVANCED_WEIGHT);
        double total = NORMAL_WEIGHT + advanced * 3;

        double roll = random.nextDouble() * total;
        if (roll < NORMAL_WEIGHT) {
            return EnemyType.NORMAL;
        }
        if (roll < NORMAL_WEIGHT + advanced) {
            return EnemyType.MOVING;
        }
        if (roll < NORMAL_WEIGHT + advanced * 2) {
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

    /** 玩家子弹命中敌机：敌机扣血、子弹销毁，击毁则计分并按概率掉落道具（F06/F08/F10）。 */
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

    /** 机体碰撞：玩家扣血、敌机销毁；护盾与无敌帧在 Player 内部消化（F07）。 */
    private void checkPlayerEnemy() {
        if (!player.isAlive()) {
            return;
        }
        for (Enemy enemy : enemies) {
            if (!enemy.isAlive() || !isColliding(player, enemy)) {
                continue;
            }
            enemy.setAlive(false);
            player.takeDamage(COLLISION_DAMAGE);
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

    /** 击毁敌机时按概率掉一种随机道具（F10）。 */
    private void dropItem(Enemy enemy) {
        if (random.nextDouble() >= ITEM_DROP_RATE) {
            return;
        }
        ItemType[] types = ItemType.values();
        ItemType type = types[random.nextInt(types.length)];
        items.add(new Item(enemy.getX(), enemy.getY(), type));
    }

    /** 道具即时生效：炸弹清屏、护盾挡一次、回血、火力强化升双发（F10）。 */
    private void applyItem(ItemType type) {
        switch (type) {
            case FIREPOWER -> player.enhanceFirePower();
            case HEAL -> player.heal(HEAL_AMOUNT);
            case SHIELD -> player.activateShield();
            case BOMB -> clearBattlefield();
        }
    }

    /** 全屏炸弹：清掉场上所有敌机与敌弹，并按各机分值计入得分。 */
    private void clearBattlefield() {
        for (Enemy enemy : enemies) {
            if (enemy.isAlive()) {
                addScore(enemy.getScore());
            }
        }
        enemies.clear();
        bullets.removeIf(bullet -> !bullet.isPlayerBullet());
    }

    private boolean isColliding(Entity a, Entity b) {
        return a.getX() < b.getX() + b.getWidth() &&
               a.getX() + a.getWidth() > b.getX() &&
               a.getY() < b.getY() + b.getHeight() &&
               a.getY() + a.getHeight() > b.getY();
    }

    @Override
    public void movePlayer(double dx, double dy) {
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
    public GameStatus getStatus() { return gameState.getStatus(); }

    @Override
    public void addScore(int amount) {
        gameState.addScore(amount);
    }
}
