package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GameModelImpl} 的单元测试：覆盖整局编排逻辑——开局初始化、冻结守卫、
 * 暂停/恢复、碰撞结算与计分、炸弹清屏、结束判定。
 *
 * <p>随机源由构造注入固定种子（同包可见构造），使机型掷点与道具掉落可复现；
 * 无种子时"是否掉落道具"不确定，因此涉及随机的断言写成区间而非确定值。
 * 碰撞结算的四个判定方法是私有的，通过反射调用，避免为了测试放宽生产可见性。</p>
 */
class GameModelImplTest {

    private static final long SEED = 20260910L;

    @TempDir
    Path tempDir;

    private GameModelImpl model;

    @BeforeEach
    void setUp() {
        model = new GameModelImpl(new Random(SEED));
    }

    /** 反射调用私有无参方法。 */
    private static void invoke(GameModelImpl target, String name) throws Exception {
        Method method = GameModelImpl.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    private static void checkBulletEnemy(GameModelImpl target) throws Exception {
        invoke(target, "checkBulletEnemy");
    }

    private static void checkBulletPlayer(GameModelImpl target) throws Exception {
        invoke(target, "checkBulletPlayer");
    }

    private static void checkPlayerEnemy(GameModelImpl target) throws Exception {
        invoke(target, "checkPlayerEnemy");
    }

    /**
     * 放一架敌机与玩家纵向重叠（用敌机自己的宽高摆位，避免把机型尺寸写死）。
     * 玩家在底部、敌机贴在其上方，两者必须真正相交。
     */
    private void placeEnemyOnPlayer(Enemy enemy) {
        enemy.moveTo(model.getPlayer().getX(), model.getPlayer().getY() - enemy.getHeight() + 5);
    }

    private void placeBulletOnPlayer(Bullet bullet) {
        bullet.moveTo(model.getPlayer().getX(), model.getPlayer().getY());
    }

    /**
     * 在固定坐标生成一发玩家子弹并跑一次子弹-敌机结算，返回该子弹是否命中被销毁。
     * 子弹坐标硬编码为 (450, 200)，调用方需把敌机放在能与其相交的位置。
     */
    private boolean fireOneBulletAndResolve() throws Exception {
        Bullet bullet = new Bullet(450, 200, 0, 1, true);
        model.getBullets().add(bullet);
        checkBulletEnemy(model);
        return !bullet.isAlive();
    }

    // ---------------- 开局与状态（GWT-01 / F01 / F12 / F14） ----------------

    @Test
    void initialStatusIsMenu() {
        assertEquals(GameStatus.MENU, model.getStatus());
    }

    @Test
    void initGameResetsEverything() {
        model.addScore(500);
        model.initGame();

        assertEquals(GameStatus.PLAYING, model.getStatus());
        assertEquals(0, model.getScore());
        assertEquals(1, model.getLevel());
        assertEquals(GameConfig.DEFAULT_HEALTH, model.getHealth());
        assertTrue(model.getPlayer().isAlive());
        assertTrue(model.getEnemies().isEmpty());
        assertTrue(model.getBullets().isEmpty());
        assertTrue(model.getItems().isEmpty());
    }

    /** 重开时战机必须回到战场底部中央，而不是停在上局的死亡位置（P7 回归）。 */
    @Test
    void initGameRepositionsPlayerToBottomCenter() {
        model.initGame();
        model.movePlayer(0, -10000);   // 撞到顶部
        model.initGame();

        Player player = model.getPlayer();
        assertEquals((GameConfig.WINDOW_WIDTH - player.getWidth()) / 2.0, player.getX(), 0.001);
        assertEquals(GameConfig.WINDOW_HEIGHT - player.getHeight() - 20, player.getY(), 0.001);
    }

    /** 非 PLAYING 状态整帧冻结：不移动、不计分、不生成（F12/F14）。 */
    @Test
    void updateDoesNothingWhenNotPlaying() {
        model.update(1.0);   // 还在 MENU

        assertEquals(GameStatus.MENU, model.getStatus());
        assertTrue(model.getEnemies().isEmpty());
        assertTrue(model.getBullets().isEmpty());
        assertEquals(0, model.getScore());
        assertEquals(0, model.getElapsedTime(), 0.001);
    }

    @Test
    void updateFreezesWhilePaused() {
        model.initGame();
        model.update(0.5);
        model.pause();
        assertEquals(GameStatus.PAUSED, model.getStatus());

        double scoreWhenPaused = model.getScore();
        double elapsedWhenPaused = model.getElapsedTime();
        model.update(5.0);   // 暂停期间推 5 秒

        assertEquals(GameStatus.PAUSED, model.getStatus());
        assertEquals(elapsedWhenPaused, model.getElapsedTime(), 0.001);
        assertEquals(scoreWhenPaused, model.getScore());
    }

    @Test
    void pauseAndResumeAreGuarded() {
        model.resume();                                  // MENU 下无效
        assertEquals(GameStatus.MENU, model.getStatus());

        model.pause();                                   // MENU 下无效
        assertEquals(GameStatus.MENU, model.getStatus());

        model.initGame();
        model.pause();
        assertEquals(GameStatus.PAUSED, model.getStatus());
        model.pause();                                   // 重复暂停幂等
        assertEquals(GameStatus.PAUSED, model.getStatus());
        model.resume();
        assertEquals(GameStatus.PLAYING, model.getStatus());
        model.resume();                                  // 重复恢复幂等
        assertEquals(GameStatus.PLAYING, model.getStatus());
    }

    @Test
    void toMenuClearsBattlefield() {
        model.initGame();
        model.update(3.0);                               // 至少生成一架敌机
        assertFalse(model.getEnemies().isEmpty());

        model.toMenu();
        assertEquals(GameStatus.MENU, model.getStatus());
        assertTrue(model.getEnemies().isEmpty());
        assertTrue(model.getBullets().isEmpty());
    }

    /** 暂停/结束时不再响应移动（P6 回归）。 */
    @Test
    void movePlayerIsIgnoredWhenNotPlaying() {
        model.initGame();
        double x = model.getPlayer().getX();
        model.pause();
        model.movePlayer(50, 0);
        assertEquals(x, model.getPlayer().getX(), 0.001);
    }

    @Test
    void movePlayerClampsInsideBattlefield() {
        model.initGame();
        model.movePlayer(-10000, -10000);
        assertEquals(0, model.getPlayer().getX(), 0.001);
        assertEquals(0, model.getPlayer().getY(), 0.001);

        model.movePlayer(10000, 10000);
        Player player = model.getPlayer();
        assertEquals(GameConfig.WINDOW_WIDTH - player.getWidth(), player.getX(), 0.001);
        assertEquals(GameConfig.WINDOW_HEIGHT - player.getHeight(), player.getY(), 0.001);
    }

    @Test
    void elapsedTimeAdvancesWhilePlaying() {
        model.initGame();
        model.update(0.25);
        model.update(0.25);
        assertEquals(0.5, model.getElapsedTime(), 0.001);
    }

    // ---------------- 碰撞与计分（GWT-06 / GWT-07b / GWT-08 / GWT-10） ----------------

    /**
     * 子弹伤害按发累积，击毁时只计一次分（对应 GWT-06 / GWT-10）。
     * 射击敌机 3 血：每次结算消耗一发子弹，打第 3 发才击毁、此时才计分。
     */
    @Test
    void bulletDamageAccumulatesAndScoresOnceOnKill() throws Exception {
        model.initGame();

        ShootingEnemy enemy = new ShootingEnemy(440, 200);
        model.getEnemies().add(enemy);

        for (int i = 1; i < GameConfig.SHOOTING_ENEMY_HEALTH; i++) {
            assertTrue(fireOneBulletAndResolve(), "第 " + i + " 发命中");
            assertEquals(GameConfig.SHOOTING_ENEMY_HEALTH - i, enemy.getHealth());
            assertTrue(enemy.isAlive(), "还没打光血，不应击毁");
            assertEquals(0, model.getScore(), "未击毁不计分");
        }

        assertTrue(fireOneBulletAndResolve(), "最后一发命中");
        assertFalse(enemy.isAlive(), "血量归零应击毁");
        assertEquals(GameConfig.SHOOTING_ENEMY_SCORE, model.getScore());
    }

    // ---------------- 道具（F10） ----------------

    /** 道具掉落按 50% 概率：固定种子下多次击毁中大约一半掉落。 */
    @Test
    void itemDropRateIsAboutHalf() throws Exception {
        model.initGame();
        Method dropItem = GameModelImpl.class.getDeclaredMethod("dropItem", Enemy.class);
        dropItem.setAccessible(true);

        int drops = 0;
        int trials = 1000;
        for (int i = 0; i < trials; i++) {
            int before = model.getItems().size();
            dropItem.invoke(model, new NormalEnemy(100, 100));
            if (model.getItems().size() > before) drops++;
        }
        // 50% ± 5% 的宽区间，避免固定种子造成的偶然偏差导致偶发失败
        assertTrue(drops > trials * 0.45 && drops < trials * 0.55,
                "1000 次击毁应掉约 500 件，实际 " + drops);
    }

    /** 掉落的道具里火力强化占大头（item.firepower.rate），不是四选一均分。 */
    @Test
    void firepowerIsTheMostFrequentDrop() throws Exception {
        model.initGame();
        Method dropItem = GameModelImpl.class.getDeclaredMethod("dropItem", Enemy.class);
        dropItem.setAccessible(true);

        int trials = 2000;
        for (int i = 0; i < trials; i++) {
            dropItem.invoke(model, new NormalEnemy(100, 100));
        }

        List<Item> dropped = model.getItems();
        long firepower = dropped.stream().filter(item -> item.getType() == ItemType.FIREPOWER).count();
        assertFalse(dropped.isEmpty(), "2000 次击毁不该一件都不掉");
        assertTrue(firepower > dropped.size() / 2.0,
                "火力强化应占掉落的一半以上，实际 " + firepower + "/" + dropped.size());
    }

    /** 全屏炸弹：清掉全部敌机与敌弹、按各机分值补分，且不动玩家自己的子弹。 */
    @Test
    void bombClearsBattlefieldAndScoresByEnemyValue() throws Exception {
        model.initGame();

        model.getEnemies().add(new NormalEnemy(100, 100));   // 100 分
        model.getEnemies().add(new MovingEnemy(200, 100));   // 150 分
        Bullet enemyBullet = new Bullet(10, 10, 100, GameConfig.BULLET_DAMAGE, false);
        Bullet playerBullet = new Bullet(10, 10, -100, 1, true);
        model.getBullets().add(enemyBullet);
        model.getBullets().add(playerBullet);

        Method clear = GameModelImpl.class.getDeclaredMethod("clearBattlefield");
        clear.setAccessible(true);
        clear.invoke(model);

        assertTrue(model.getEnemies().isEmpty());
        assertEquals(GameConfig.NORMAL_ENEMY_SCORE + GameConfig.MOVING_ENEMY_SCORE, model.getScore());
        assertEquals(1, model.getBullets().size(), "只清敌弹，保留玩家子弹");
        assertTrue(model.getBullets().get(0).isPlayerBullet());
    }

    /** 道具效果：回血受上限约束、护盾置位、火力强化至上限（F10）。 */
    @Test
    void applyItemEffectsTakeEffect() throws Exception {
        model.initGame();
        Method applyItem = GameModelImpl.class.getDeclaredMethod("applyItem", ItemType.class);
        applyItem.setAccessible(true);
        Player player = model.getPlayer();

        player.takeDamage(GameConfig.COLLISION_DAMAGE);
        player.update(GameConfig.PLAYER_INVINCIBLE_TIME);
        int damaged = player.getHealth();
        applyItem.invoke(model, ItemType.HEAL);
        assertEquals(Math.min(damaged + GameConfig.HEAL_AMOUNT, player.getMaxHealth()), player.getHealth());

        applyItem.invoke(model, ItemType.SHIELD);
        assertTrue(player.isShielded());

        applyItem.invoke(model, ItemType.FIREPOWER);
        assertEquals(2, player.getFirePower(), "吃一个火力道具应升到 2 级");
        for (int level = 2; level < GameConfig.MAX_FIRE_POWER; level++) {
            applyItem.invoke(model, ItemType.FIREPOWER);
        }
        assertEquals(GameConfig.MAX_FIRE_POWER, player.getFirePower(), "吃到上限后不再往上加");
    }

    /**
     * 同一帧内多发子弹可以打中同一架敌机（break 只退出内层敌人循环）。
     * 这是双发火力连打同一目标的基础；计分仍只发生一次。
     */
    @Test
    void multipleBulletsMayHitSameEnemyInOnePass() throws Exception {
        model.initGame();

        ShootingEnemy enemy = new ShootingEnemy(440, 200);   // 3 血
        model.getEnemies().add(enemy);
        model.getBullets().add(new Bullet(450, 200, 0, 1, true));
        model.getBullets().add(new Bullet(450, 200, 0, 1, true));

        checkBulletEnemy(model);

        assertEquals(GameConfig.SHOOTING_ENEMY_HEALTH - 2, enemy.getHealth(), "两发都命中同一架");
        assertTrue(enemy.isAlive());
        assertEquals(0, model.getScore(), "未击毁不计分");
    }

    /** 一发子弹命中后即 break，不会顺带伤害同帧另一架敌机（设计取舍见 5.5）。 */
    @Test
    void oneBulletNeverDamagesTwoEnemies() throws Exception {
        model.initGame();

        NormalEnemy front = new NormalEnemy(440, 200);
        NormalEnemy behind = new NormalEnemy(440, 200);   // 完全重叠
        model.getEnemies().add(front);
        model.getEnemies().add(behind);
        model.getBullets().add(new Bullet(450, 200, 0, 1, true));

        checkBulletEnemy(model);

        assertEquals(GameConfig.NORMAL_ENEMY_HEALTH - 1, front.getHealth());
        assertEquals(GameConfig.NORMAL_ENEMY_HEALTH, behind.getHealth(), "第二架不应被同一发命中");
        assertTrue(behind.isAlive());
        assertEquals(0, model.getScore());
    }

    /** 一帧内两对碰撞都要结算，不能漏（GWT-10）。 */
    @Test
    void twoBulletsKillTwoEnemiesInOnePass() throws Exception {
        model.initGame();

        NormalEnemy e1 = new NormalEnemy(440, 200);
        NormalEnemy e2 = new NormalEnemy(300, 200);
        e1.takeDamage(1);   // 各剩 1 血
        e2.takeDamage(1);
        model.getEnemies().add(e1);
        model.getEnemies().add(e2);
        model.getBullets().add(new Bullet(450, 200, 0, 1, true));
        model.getBullets().add(new Bullet(310, 200, 0, 1, true));

        checkBulletEnemy(model);

        assertFalse(e1.isAlive());
        assertFalse(e2.isAlive());
        assertEquals(GameConfig.NORMAL_ENEMY_SCORE * 2, model.getScore());
    }

    /** 敌弹命中玩家：扣 10 血并进入无敌（GWT-07a）。 */
    @Test
    void enemyBulletHitsPlayer() throws Exception {
        model.initGame();

        Bullet enemyBullet = new Bullet(0, 0, 100, GameConfig.BULLET_DAMAGE, false);
        placeBulletOnPlayer(enemyBullet);
        model.getBullets().add(enemyBullet);

        checkBulletPlayer(model);

        assertEquals(GameConfig.DEFAULT_HEALTH - GameConfig.BULLET_DAMAGE, model.getHealth());
        assertFalse(enemyBullet.isAlive());
        assertTrue(model.getPlayer().isInvincible());
    }

    /** 无敌期间再中弹不扣血（GWT-07b）。 */
    @Test
    void secondHitDuringInvincibilityIsIgnored() throws Exception {
        model.initGame();

        Bullet first = new Bullet(0, 0, 100, GameConfig.BULLET_DAMAGE, false);
        placeBulletOnPlayer(first);
        model.getBullets().add(first);
        checkBulletPlayer(model);

        int healthAfterFirst = model.getHealth();
        Bullet second = new Bullet(0, 0, 100, GameConfig.BULLET_DAMAGE, false);
        placeBulletOnPlayer(second);
        model.getBullets().add(second);
        checkBulletPlayer(model);

        assertEquals(healthAfterFirst, model.getHealth());
    }

    /** 玩家子弹不会打到玩家自己。 */
    @Test
    void playerBulletNeverHitsPlayer() throws Exception {
        model.initGame();

        Bullet ownBullet = new Bullet(0, 0, -100, 1, true);
        placeBulletOnPlayer(ownBullet);
        model.getBullets().add(ownBullet);

        checkBulletPlayer(model);

        assertEquals(GameConfig.DEFAULT_HEALTH, model.getHealth());
        assertTrue(ownBullet.isAlive());
    }

    /** 机体碰撞：玩家按机型受伤、敌机销毁（GWT-07c 与自爆机高额伤害）。 */
    @Test
    void enemyContactDamagesPlayerByType() throws Exception {
        model.initGame();

        NormalEnemy normal = new NormalEnemy(0, 0);
        placeEnemyOnPlayer(normal);
        model.getEnemies().add(normal);
        checkPlayerEnemy(model);

        assertFalse(normal.isAlive(), "撞上的敌机应被销毁");
        assertEquals(GameConfig.DEFAULT_HEALTH - GameConfig.COLLISION_DAMAGE, model.getHealth());

        // 跳过无敌帧后换自爆机撞一次
        model.getPlayer().update(GameConfig.PLAYER_INVINCIBLE_TIME);
        int healthBefore = model.getHealth();
        BomberEnemy bomber = new BomberEnemy(0, 0);
        placeEnemyOnPlayer(bomber);
        model.getEnemies().add(bomber);
        checkPlayerEnemy(model);

        assertEquals(healthBefore - GameConfig.BOMBER_COLLISION_DAMAGE, model.getHealth());
    }

    @Test
    void deadEntitiesAreRemovedAtFrameEnd() throws Exception {
        model.initGame();

        NormalEnemy enemy = new NormalEnemy(440, 200);
        enemy.setAlive(false);
        Bullet bullet = new Bullet(10, 10, 0, 1, true);
        bullet.setAlive(false);
        Item item = new Item(10, 10, ItemType.HEAL);
        item.setAlive(false);
        model.getEnemies().add(enemy);
        model.getBullets().add(bullet);
        model.getItems().add(item);

        invoke(model, "removeDeadEntities");

        assertTrue(model.getEnemies().isEmpty());
        assertTrue(model.getBullets().isEmpty());
        assertTrue(model.getItems().isEmpty());
    }

    // ---------------- 结束判定（GWT-09） ----------------

    @Test
    void gameOverWhenHealthReachesZero() throws Exception {
        model.initGame();

        int hits = GameConfig.DEFAULT_HEALTH / GameConfig.COLLISION_DAMAGE;  // 100 / 20 = 5，正好归零
        for (int i = 0; i < hits; i++) {
            NormalEnemy enemy = new NormalEnemy(0, 0);
            placeEnemyOnPlayer(enemy);
            model.getEnemies().add(enemy);
            checkPlayerEnemy(model);
            model.getPlayer().update(GameConfig.PLAYER_INVINCIBLE_TIME + 0.1);  // 跳过无敌帧
        }
        assertFalse(model.getPlayer().isAlive());
        assertEquals(0, model.getHealth());

        model.update(0.016);   // 帧末判定
        assertEquals(GameStatus.GAME_OVER, model.getStatus());

        // 结束后整帧冻结
        double elapsed = model.getElapsedTime();
        model.update(1.0);
        assertEquals(elapsed, model.getElapsedTime(), 0.001);
    }

    // ---------------- 开局复位与最高分（F01 / F13） ----------------

    /** 存档指到临时目录，避免测试往工作目录里写 highscore.txt。 */
    private GameModelImpl modelWithStore(HighScoreStore store) {
        GameModelImpl fresh = new GameModelImpl(new Random(SEED), store);
        fresh.initGame();
        return fresh;
    }

    private void killPlayer(GameModelImpl target) {
        target.getPlayer().takeDamage(GameConfig.DEFAULT_HEALTH);
        target.update(0.016);
    }

    /** 重开一局要把上局的火力、护盾、无敌帧一并清掉，不能只回血回位（P7 回归）。 */
    @Test
    void initGameClearsRunScopedState() {
        model.initGame();
        Player player = model.getPlayer();
        player.enhanceFirePower();
        player.activateShield();
        player.takeDamage(GameConfig.BULLET_DAMAGE);
        model.movePlayer(-300, -300);

        model.initGame();

        assertEquals(1, player.getFirePower(), "火力强化不该带到新一局");
        assertFalse(player.isShielded(), "护盾不该带到新一局");
        assertFalse(player.isInvincible(), "无敌帧不该带到新一局");
        assertEquals(GameConfig.DEFAULT_HEALTH, player.getHealth());
        assertEquals((GameConfig.WINDOW_WIDTH - player.getWidth()) / 2.0, player.getX(), 0.001);
    }

    @Test
    void highScoreIsWrittenWhenRunEnds() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("highscore.txt"));
        GameModelImpl fresh = modelWithStore(store);

        fresh.addScore(300);
        killPlayer(fresh);

        assertEquals(GameStatus.GAME_OVER, fresh.getStatus());
        assertEquals(300, fresh.getHighScore());
        assertEquals(300, store.load(), "结算后成绩应落盘");
    }

    @Test
    void lowerScoreDoesNotOverwriteHighScore() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("highscore.txt"));
        GameModelImpl fresh = modelWithStore(store);

        fresh.addScore(300);
        killPlayer(fresh);
        assertEquals(300, fresh.getHighScore());

        fresh.initGame();
        fresh.addScore(100);
        killPlayer(fresh);

        assertEquals(300, fresh.getHighScore(), "分数更低的一局不该覆盖最高分");
        assertEquals(300, store.load());
    }

    @Test
    void highScoreLoadsFromExistingSave() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("highscore.txt"));
        store.save(1234);

        GameModelImpl fresh = new GameModelImpl(new Random(SEED), store);

        assertEquals(1234, fresh.getHighScore());
        assertEquals(0, fresh.getScore(), "读存档不该影响本局分数");
        assertEquals(GameStatus.MENU, fresh.getStatus());
    }

    // ---------------- 通关（F17） ----------------

    @Test
    void reachingVictoryScoreEndsRunAsVictory() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("highscore.txt"));
        GameModelImpl fresh = modelWithStore(store);

        fresh.addScore(GameConfig.VICTORY_SCORE);
        fresh.update(0.016);

        assertEquals(GameStatus.VICTORY, fresh.getStatus());
        assertEquals(GameConfig.VICTORY_SCORE, fresh.getHighScore());
        assertEquals(GameConfig.VICTORY_SCORE, store.load(), "通关成绩也要落盘");
    }

    /** 同一帧里既够通关分又被打死时按失败算（死亡判定优先）。 */
    @Test
    void deathTakesPrecedenceOverVictoryInSameFrame() {
        HighScoreStore store = new HighScoreStore(tempDir.resolve("highscore.txt"));
        GameModelImpl fresh = modelWithStore(store);

        fresh.addScore(GameConfig.VICTORY_SCORE);
        killPlayer(fresh);

        assertEquals(GameStatus.GAME_OVER, fresh.getStatus(), "同帧竞争时应判失败");
    }
}
