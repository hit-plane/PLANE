package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;
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

    /** 受击特效时长（秒）：敌机命中闪光 0.15s、敌机爆炸 0.35s、玩家受击 0.3s。 */
    private static final double HIT_EFFECT_DURATION = 0.15;
    private static final double EXPLODE_EFFECT_DURATION = 0.35;
    private static final double PLAYER_HIT_EFFECT_DURATION = 0.3;

    private final GameState gameState;
    private final Player player;
    private final List<Enemy> enemies = new ArrayList<>();
    private final List<Bullet> bullets = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<BombWave> waves = new ArrayList<>();
    private final List<HitEffect> hitEffects = new ArrayList<>();
    private final Random random;
    private final HighScoreStore highScoreStore;
    /** 通关最短用时存档（F24）：与最高分一样按难度分档。 */
    private final ClearTimeStore clearTimeStore;
    private int highScore;
    /** 刚结束的这一局是否刷新了本档最短用时，供结算界面提示"新纪录"。 */
    private boolean newClearRecord;
    private double spawnTimer;
    /**
     * 当前难度（F16）：默认普通档，由主菜单在开局前设定，只影响本局数值，不随重开重置。
     * 没有写成字段初始化式，是因为玩家要在构造里按它建机（血量上限/起始火力/子弹速度），
     * 而字段初始化式会在构造体之后才执行、把构造里赋的值又覆盖回普通档。
     */
    private Difficulty difficulty;
    /**
     * 作弊开关（主菜单暗号解锁后可切换）：关闭时玩家按本档常规数值建机，
     * 开启后才享受本档的作弊加成（折磨档的五连发 / 9999 血 / 双倍弹速）。
     * 与难度一样只影响本局数值，不随重开重置。
     */
    private boolean cheatEnabled;

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
        this(random, highScoreStore, ClearTimeStore.beside(highScoreStore.getFile()));
    }

    /** 三个依赖都可注入：用时存档默认落在最高分存档同目录，也可单独指定（测试用）。 */
    GameModelImpl(Random random, HighScoreStore highScoreStore, ClearTimeStore clearTimeStore) {
        this.gameState = new GameState();
        this.difficulty = Difficulty.NORMAL;
        this.cheatEnabled = false;
        // 玩家按难度建机：血量上限、起始火力与子弹速度都取本档参数（常规三档即旧版数值）。
        // 必须在 difficulty 赋值之后，否则建出来的是普通档机体。
        this.player = new Player(PLAYER_START_X, PLAYER_START_Y, PLAYER_SIZE, PLAYER_SIZE,
                difficulty, cheatEnabled);
        this.random = random;
        this.highScoreStore = highScoreStore;
        this.clearTimeStore = clearTimeStore;
        // 默认普通档：普通档的存档就是原文件本身，老存档照旧能读到（F13/F16）。
        this.highScore = highScoreStore.forDifficulty(difficulty).load();
        this.newClearRecord = false;
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
        waves.clear();
        hitEffects.clear();
        spawnTimer = 0;
        newClearRecord = false;
    }

    @Override
    public void update(double deltaTime) {
        // 暂停 / 结束 / 还没开局：整帧不动，也不计分（F12/F14）
        if (gameState.getStatus() != GameStatus.PLAYING) {
            return;
        }

        gameState.advanceTime(deltaTime);

        // 把当前得分一并喂给玩家：额外弹道的寿命按得分算（"3 关后消失"）
        player.update(deltaTime, (int) gameState.getScore());

        autoShoot();
        updateSpawn(deltaTime);

        updateEntities(enemies, deltaTime);
        collectEnemyBullets();
        updateEntities(bullets, deltaTime);
        updateEntities(items, deltaTime);
        updateEntities(waves, deltaTime);
        updateHitEffects(deltaTime);

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

    /**
     * 一局结束：冻结状态，清除玩家临时效果与场上道具，并把本局成绩刷进最高分存档（F09 / F13）。
     * 成绩按难度分档存（F16）：写的是当前档位那一份，不会盖掉别的档。
     */
    private void finishGame(GameStatus status) {
        gameState.setStatus(status);
        // 清除火力强化等临时效果，避免结算界面仍显示多发子弹
        player.clearPowerUps();
        // 清除场上残留道具与冲击波，避免结算画面仍显示飞行中的东西
        items.clear();
        waves.clear();
        hitEffects.clear();
        if (gameState.getScore() > highScore) {
            highScore = (int) gameState.getScore();
            highScoreStore.forDifficulty(difficulty).save(highScore);
        }
        if (status == GameStatus.VICTORY) {
            recordClearTime();
        }
    }

    /**
     * 通关时刷新本档的最短用时（F24）：只有比已存档的成绩更优才覆盖，并把"新纪录"标记立起来。
     * 优劣次序见 {@link ClearTime#isBetterThan(ClearTime)}——"超时"盖不掉一条具体用时，
     * 所以超时通关不算刷新纪录，玩家仍看得到自己更快的那次成绩。
     */
    private void recordClearTime() {
        ClearTime run = getRunClearTime();
        ClearTime best = clearTimeStore.forDifficulty(difficulty).load();
        if (run.isBetterThan(best)) {
            clearTimeStore.forDifficulty(difficulty).save(run);
            newClearRecord = true;
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
        waves.clear();
        hitEffects.clear();
        spawnTimer = 0;
        newClearRecord = false;
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
     * 关卡越高，敌机出得越密（F11 难度递增），难度再整体缩放这条曲线（F16）：
     * 简单档更稀疏、困难档更密。起始间隔比 v1.0 定的 2.0 秒更短，
     * 同屏敌机数大约翻一倍——怪密度整体上调。曲线与下限都在配置里，这里只取当前档的值。
     */
    private double currentSpawnInterval() {
        return GameConfig.spawnIntervalAt(gameState.getLevel(), difficulty);
    }

    /**
     * 从顶部随机横坐标生成一架随机机型的敌机（F04）。
     * 血量按当前关卡与当前难度放大（{@link GameConfig#enemyHealthAt}），关卡越高、档位越硬，单机越耐打。
     */
    private Enemy spawnEnemy() {
        EnemyType type = randomType(difficulty);
        double size = (type == EnemyType.NORMAL) ? GameConfig.NORMAL_ENEMY_SIZE : GameConfig.ENEMY_SIZE;
        double x = random.nextDouble() * (GameConfig.WINDOW_WIDTH - size);
        double y = -size;
        int level = gameState.getLevel();

        return switch (type) {
            case MOVING -> new MovingEnemy(x, y,
                    GameConfig.enemyHealthAt(GameConfig.MOVING_ENEMY_HEALTH, level, difficulty));
            case SHOOTING -> new ShootingEnemy(x, y,
                    GameConfig.enemyHealthAt(GameConfig.SHOOTING_ENEMY_HEALTH, level, difficulty));
            case BOMBER -> new BomberEnemy(x, y,
                    GameConfig.enemyHealthAt(GameConfig.BOMBER_ENEMY_HEALTH, level, difficulty));
            default -> new NormalEnemy(x, y,
                    GameConfig.enemyHealthAt(GameConfig.NORMAL_ENEMY_HEALTH, level, difficulty));
        };
    }

    /**
     * 按当前关卡掷机型（普通档口径，供旧测试与不关心难度的调用方使用）：
     * 第 1 关清一色普通敌机，每升 1 关给横移/射击/自爆各加一份权重（F11）。
     */
    private EnemyType randomType() {
        return randomType(Difficulty.NORMAL);
    }

    /**
     * 按当前关卡与难度掷机型：第 1 关清一色普通敌机，
     * 每升 1 关给横移/射击/自爆各加一份权重，高级机型出现得越来越频繁（F11）。
     *
     * <p>三种高级机型的权重再各自乘本档的 {@link Difficulty#getTypeWeightMultiplier}：
     * 困难档把射击机压到四成（高密度下"每 2 秒一发的敌弹"最容易变成弹幕墙），
     * 横移与自爆机保持原样；简单/普通档三个倍率都是 1.0，权重与旧版逐位相同。</p>
     */
    private EnemyType randomType(Difficulty difficulty) {
        double advanced = Math.min(gameState.getLevel() - 1, GameConfig.MAX_ADVANCED_WEIGHT);
        double normalWeight = GameConfig.NORMAL_TYPE_WEIGHT;
        double movingWeight = advanced * difficulty.getTypeWeightMultiplier(EnemyType.MOVING);
        double shootingWeight = advanced * difficulty.getTypeWeightMultiplier(EnemyType.SHOOTING);
        double bomberWeight = advanced * difficulty.getTypeWeightMultiplier(EnemyType.BOMBER);
        double total = normalWeight + movingWeight + shootingWeight + bomberWeight;

        double roll = random.nextDouble() * total;
        if (roll < normalWeight) {
            return EnemyType.NORMAL;
        }
        if (roll < normalWeight + movingWeight) {
            return EnemyType.MOVING;
        }
        if (roll < normalWeight + movingWeight + shootingWeight) {
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
        waves.removeIf(wave -> !wave.isAlive());
        hitEffects.removeIf(effect -> !effect.isAlive());
    }

    /** 更新受击特效计时器。 */
    private void updateHitEffects(double deltaTime) {
        for (HitEffect effect : hitEffects) {
            effect.update(deltaTime);
        }
    }

    private void checkCollisions() {
        resolveBombWaves();
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
                // 命中闪光：在子弹与敌机的接触点生成
                hitEffects.add(new HitEffect(
                        bullet.getX() + bullet.getWidth() / 2,
                        bullet.getY() + bullet.getHeight() / 2,
                        HitEffect.Type.ENEMY_HIT, HIT_EFFECT_DURATION));
                if (!enemy.isAlive()) {
                    // 击毁爆炸：在敌机中心生成更大的爆炸特效
                    hitEffects.add(new HitEffect(
                            enemy.getX() + enemy.getWidth() / 2,
                            enemy.getY() + enemy.getHeight() / 2,
                            HitEffect.Type.ENEMY_EXPLODE, EXPLODE_EFFECT_DURATION));
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
                int hpBefore = player.getHealth();
                player.takeDamage(bullet.getDamage());
                if (player.getHealth() < hpBefore) {
                    hitEffects.add(new HitEffect(
                            player.getX() + player.getWidth() / 2,
                            player.getY() + player.getHeight() / 2,
                            HitEffect.Type.PLAYER_HIT, PLAYER_HIT_EFFECT_DURATION));
                }
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
            int hpBefore = player.getHealth();
            player.takeDamage(enemy.getCollisionDamage());
            if (player.getHealth() < hpBefore) {
                hitEffects.add(new HitEffect(
                        player.getX() + player.getWidth() / 2,
                        player.getY() + player.getHeight() / 2,
                        HitEffect.Type.PLAYER_HIT, PLAYER_HIT_EFFECT_DURATION));
            }
            // 撞击同时也在敌机位置生成爆炸
            hitEffects.add(new HitEffect(
                    enemy.getX() + enemy.getWidth() / 2,
                    enemy.getY() + enemy.getHeight() / 2,
                    HitEffect.Type.ENEMY_EXPLODE, EXPLODE_EFFECT_DURATION));
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

    /**
     * 击毁敌机时按概率掉一件道具（F10）。
     * 概率随关卡递减（普通档第 1 关 50%，第 10 关只剩 15%），再按难度整体缩放（F16）：
     * 简单档掉得更多、困难档更少。越到后期补给越稀罕。
     */
    private void dropItem(Enemy enemy) {
        if (random.nextDouble() >= GameConfig.itemDropRateAt(gameState.getLevel(), difficulty)) {
            return;
        }
        items.add(new Item(enemy.getX(), enemy.getY(), randomItemType()));
    }

    /**
     * 掷道具类型：先按 {@code item.firepower.rate}（25%）决定是不是"火力强化"，
     * 不是的话再在炸弹/护盾/回血三种里等概率挑一个。
     * 四种道具因此各占 25%，火力强化不再是掉得最多的一种（Q6）。
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
            case BOMB -> waves.add(new BombWave());
            default -> throw new IllegalStateException("未处理的道具类型: " + type);
        }
    }

    /**
     * 冲击波扫过的结算：被扫到的敌机直接消失，按 {@link GameConfig#BOMB_WAVE_SCORE_RATE}
     * 的比例计入得分（不掉道具）；被扫到的敌弹一并清除，玩家自己的子弹不受影响（F10）。
     */
    private void resolveBombWaves() {
        if (waves.isEmpty()) {
            return;
        }
        for (BombWave wave : waves) {
            if (!wave.isAlive()) {
                continue;
            }
            for (Enemy enemy : enemies) {
                if (!enemy.isAlive() || !isColliding(wave, enemy)) {
                    continue;
                }
                enemy.setAlive(false);
                // 得分走"炸弹预算"：单颗炸弹封顶一关的分，扫到再多敌机也不再加
                int gained = wave.consumeScore((int) (enemy.getScore() * GameConfig.BOMB_WAVE_SCORE_RATE));
                if (gained > 0) {
                    addScore(gained);
                }
            }
            for (Bullet bullet : bullets) {
                if (bullet.isAlive() && !bullet.isPlayerBullet() && isColliding(wave, bullet)) {
                    bullet.setAlive(false);
                }
            }
        }
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
    public List<BombWave> getWaves() { return waves; }

    @Override
    public List<HitEffect> getHitEffects() { return hitEffects; }

    @Override
    public int getScore() { return (int) gameState.getScore(); }

    @Override
    public int getHealth() { return player.getHealth(); }

    @Override
    public int getLevel() { return gameState.getLevel(); }

    @Override
    public double getLevelProgress() { return gameState.getLevelProgress(); }

    @Override
    public int getHighScore() { return highScore; }

    /** 按档位读存档：三档各记各的，取哪一档就把哪一档那份读出来（F16）。 */
    @Override
    public int getHighScore(Difficulty difficulty) {
        return highScoreStore.forDifficulty(difficulty).load();
    }

    /** 按档位读通关最短用时，同样各记各的（F24）。 */
    @Override
    public ClearTime getBestClearTime(Difficulty difficulty) {
        return clearTimeStore.forDifficulty(difficulty).load();
    }

    /** 本局用时：已到上限就是"超时"，否则按毫秒记（F24）。 */
    @Override
    public ClearTime getRunClearTime() {
        return gameState.isTimedOut()
                ? ClearTime.timeout()
                : ClearTime.of(Math.round(gameState.getElapsedTime() * 1000));
    }

    @Override
    public boolean isNewClearRecord() {
        return newClearRecord;
    }

    @Override
    public Difficulty getDifficulty() { return difficulty; }

    /**
     * 切换难度（F16）：敌机血量、生成间隔与掉落概率都改按新档位算，
     * 最高分也一并换成这一档的存档值（否则会拿上一档成绩去比，低分永远刷不进去）。
     * 传 null 忽略，难度保持不变。
     */
    @Override
    public void setDifficulty(Difficulty difficulty) {
        if (difficulty == null) {
            return;
        }
        this.difficulty = difficulty;
        // 玩家机在构造时就建好了，换档要把血量上限/起始火力/子弹速度按新档重算一遍，
        // 否则会出现"选了折磨档却仍是 100 血、单发"。
        player.configureFor(difficulty, cheatEnabled);
        this.highScore = highScoreStore.forDifficulty(difficulty).load();
    }

    @Override
    public GameStatus getStatus() { return gameState.getStatus(); }

    /**
     * 开关作弊（主菜单暗号解锁后可切换）：切完立刻按当前难度重算玩家机，
     * 因此无需重开一局就能看到效果（血量会回满到新的上限）。
     */
    @Override
    public void setCheatEnabled(boolean cheatEnabled) {
        this.cheatEnabled = cheatEnabled;
        player.configureFor(difficulty, cheatEnabled);
    }

    @Override
    public boolean isCheatEnabled() { return cheatEnabled; }

    /** 本局已进行时长（秒）。 */
    @Override
    public double getElapsedTime() { return gameState.getElapsedTime(); }

    @Override
    public void addScore(int amount) {
        gameState.addScore(amount);
    }
}
