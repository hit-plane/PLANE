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
 * 音效事件（F14）的单元测试：核对模型在什么情况下攒下哪一条 {@link SoundEvent}。
 *
 * <p>模型只负责"记一声"，真正放不放得出声是 View 层的事，所以这里断言的全是
 * {@link GameModel#consumeSoundEvents()} 的内容，不碰任何音频 API——本类**不启动 JavaFX 工具箱**。</p>
 *
 * <p>碰撞结算的几个方法是私有的，沿用 {@link GameModelImplTest} 的做法用反射调用，
 * 不为测试放宽生产可见性。随机源注入固定种子，机型掷点与道具掉落才可复现。</p>
 */
class GameModelSoundTest {

    private static final long SEED = 20260915L;

    @TempDir
    Path tempDir;

    private GameModelImpl model;

    @BeforeEach
    void setUp() {
        model = new GameModelImpl(new Random(SEED));
        model.initGame();
    }

    /** 反射调用私有无参方法。 */
    private static void invoke(GameModelImpl target, String name) throws Exception {
        Method method = GameModelImpl.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(target);
    }

    /** 反射调用私有的单参方法。 */
    private static void invoke(GameModelImpl target, String name, Class<?> paramType, Object arg)
            throws Exception {
        Method method = GameModelImpl.class.getDeclaredMethod(name, paramType);
        method.setAccessible(true);
        method.invoke(target, arg);
    }

    /** 把敌机摆到玩家身上，构造一次真实的机体碰撞。 */
    private void placeEnemyOnPlayer(Enemy enemy) {
        enemy.moveTo(model.getPlayer().getX(), model.getPlayer().getY() - enemy.getHeight() + 5);
    }

    /** 把子弹摆到玩家身上，构造一次真实的命中判定。 */
    private void placeBulletOnPlayer(Bullet bullet) {
        bullet.moveTo(model.getPlayer().getX(), model.getPlayer().getY());
    }

    // ========== 击中 / 击毁敌机 ==========

    /**
     * 打中但没打死 → 命中音，<b>且不带击毁音</b>。
     * 两个事件的素材目前是同一份音频，两个都发会叠成双倍音量，所以这条与下一条是一对。
     */
    @Test
    void survivingEnemySoundsHitOnly() throws Exception {
        NormalEnemy enemy = new NormalEnemy(440, 200);
        model.getEnemies().add(enemy);
        model.getBullets().add(new Bullet(450, 200, 0, GameConfig.playerBulletDamage(1), true));

        invoke(model, "checkBulletEnemy");

        assertTrue(enemy.isAlive(), "前置条件：这一下不该打死满血普通机");
        assertEquals(List.of(SoundEvent.ENEMY_HIT), model.consumeSoundEvents());
    }

    /** 一发打死 → 只有击毁音，不再发命中音。 */
    @Test
    void destroyedEnemySoundsDestructionOnly() throws Exception {
        int damage = GameConfig.playerBulletDamage(1);
        NormalEnemy enemy = new NormalEnemy(440, 200);
        enemy.takeDamage(GameConfig.NORMAL_ENEMY_HEALTH - damage);   // 只剩恰好一发子弹的血
        model.getEnemies().add(enemy);
        model.getBullets().add(new Bullet(450, 200, 0, damage, true));

        invoke(model, "checkBulletEnemy");

        assertFalse(enemy.isAlive(), "前置条件：这一下应击毁敌机");
        assertEquals(List.of(SoundEvent.ENEMY_DESTROYED), model.consumeSoundEvents());
    }

    /**
     * 炸弹清屏不发击毁音：那是扫场不是击毁，逐架发会变成一串密集的爆豆声。
     * 只守住"不发 ENEMY_DESTROYED"；扫掉的敌机按比例计分，跨过关卡阈值时该响升关音（另有专测）。
     */
    @Test
    void bombSweepDoesNotSoundDestructionPerEnemy() throws Exception {
        NormalEnemy enemy = new NormalEnemy(300, 400);
        model.getEnemies().add(enemy);
        BombWave wave = new BombWave();
        model.getWaves().add(wave);
        wave.moveTo(enemy.getX(), enemy.getY());   // 直接把波带挪到敌机身上，省去飞行的那几十帧
        model.consumeSoundEvents();

        invoke(model, "resolveBombWaves");

        assertFalse(enemy.isAlive(), "前置条件：波带应扫掉敌机");
        assertFalse(model.consumeSoundEvents().contains(SoundEvent.ENEMY_DESTROYED),
                "炸弹清屏不该发击毁音");
    }

    // ========== 拾取道具 / 炸弹 ==========

    /** 拾取回血道具 → 一声拾取音。 */
    @Test
    void pickingUpItemSoundsPickup() throws Exception {
        pickUp(ItemType.HEAL);

        assertEquals(List.of(SoundEvent.ITEM_PICKUP), model.consumeSoundEvents());
    }

    /** 拾取炸弹 → 拾取音 + 炸弹音各一声（冲击波下一帧才结算，两声不会同时挤在一帧里）。 */
    @Test
    void pickingUpBombSoundsPickupAndBomb() throws Exception {
        pickUp(ItemType.BOMB);

        assertEquals(List.of(SoundEvent.ITEM_PICKUP, SoundEvent.BOMB), model.consumeSoundEvents());
    }

    /** 把一个道具摆到玩家身上并跑一次拾取判定。 */
    private void pickUp(ItemType type) throws Exception {
        Item item = new Item(model.getPlayer().getX(), model.getPlayer().getY(), type);
        model.getItems().add(item);

        invoke(model, "checkPlayerItem");

        assertFalse(item.isAlive(), "前置条件：道具应被拾取");
    }

    // ========== 玩家受击的三种结果 ==========

    /** 敌弹打中玩家且真扣血 → 受击音。 */
    @Test
    void enemyBulletDamageSoundsPlayerHit() throws Exception {
        Bullet bullet = new Bullet(0, 0, 0, GameConfig.BULLET_DAMAGE, false);
        placeBulletOnPlayer(bullet);
        model.getBullets().add(bullet);

        invoke(model, "checkBulletPlayer");

        assertEquals(List.of(SoundEvent.PLAYER_HIT), model.consumeSoundEvents());
    }

    /** 机体相撞且真扣血 → 撞机音（与敌弹命中共用"扣血"判定，但音效不同）。 */
    @Test
    void collisionDamageSoundsPlayerCrash() throws Exception {
        Enemy enemy = new NormalEnemy(0, 0);
        placeEnemyOnPlayer(enemy);
        model.getEnemies().add(enemy);

        invoke(model, "checkPlayerEnemy");

        assertEquals(List.of(SoundEvent.PLAYER_CRASH), model.consumeSoundEvents());
    }

    /** 有盾时挡下那一下 → 格挡音，且血量分毫未动、不发受击音。 */
    @Test
    void shieldBlockSoundsBlockInsteadOfHit() throws Exception {
        Player player = model.getPlayer();
        player.activateShield();
        int healthBefore = player.getHealth();

        Bullet bullet = new Bullet(0, 0, 0, GameConfig.BULLET_DAMAGE, false);
        placeBulletOnPlayer(bullet);
        model.getBullets().add(bullet);

        invoke(model, "checkBulletPlayer");

        assertEquals(healthBefore, player.getHealth(), "有盾时不该掉血");
        assertFalse(player.isShielded(), "前置条件：盾该被这一下消耗掉");
        assertEquals(List.of(SoundEvent.SHIELD_BLOCK), model.consumeSoundEvents());
    }

    /** 无敌帧内挨的那一下被完全忽略 → 一声不响（盾没破、血没掉，响反而误导玩家）。 */
    @Test
    void invinciblePlayerSoundsNothing() throws Exception {
        Player player = model.getPlayer();
        player.takeDamage(1);   // 直接扣一下血换来无敌帧；这一步本身不走碰撞判定，不产生音效
        assertTrue(player.isInvincible(), "前置条件：玩家应处于无敌帧");
        model.consumeSoundEvents();

        Bullet bullet = new Bullet(0, 0, 0, GameConfig.BULLET_DAMAGE, false);
        placeBulletOnPlayer(bullet);
        model.getBullets().add(bullet);

        invoke(model, "checkBulletPlayer");

        assertEquals(List.of(), model.consumeSoundEvents(), "无敌帧内受击不该响");
    }

    // ========== 升关 / 终局 ==========

    /** 分数没跨过关卡阈值 → 不响；跨过去了 → 响一声。 */
    @Test
    void levelUpSoundsOnlyWhenLevelActuallyRises() {
        model.addScore(GameConfig.SCORE_PER_LEVEL - 1);
        assertEquals(List.of(), model.consumeSoundEvents(), "还没升关不该响");

        model.addScore(1);   // 正好顶到第 2 关阈值
        assertEquals(2, model.getLevel(), "前置条件：应升到第 2 关");
        assertEquals(List.of(SoundEvent.LEVEL_UP), model.consumeSoundEvents());
    }

    /** 一次大额加分连跳多级（炸弹清屏）仍只响一声，不是一级一声。 */
    @Test
    void multiLevelJumpSoundsOnce() {
        model.addScore(GameConfig.SCORE_PER_LEVEL * 3);

        assertEquals(4, model.getLevel(), "前置条件：一次加满 3 关的分应连跳 3 级");
        assertEquals(List.of(SoundEvent.LEVEL_UP), model.consumeSoundEvents());
    }

    /** 通关：跑到通关分后推一帧，结算时响胜利音。 */
    @Test
    void victorySoundsVictory() {
        model.addScore(GameConfig.VICTORY_SCORE);
        model.consumeSoundEvents();

        model.update(GameConfig.MAX_FRAME_DELTA);

        assertEquals(GameStatus.VICTORY, model.getStatus(), "前置条件：应判通关");
        assertEquals(List.of(SoundEvent.VICTORY), model.consumeSoundEvents());
    }

    /** 阵亡：血被打光后推一帧，结算时响失败音。 */
    @Test
    void defeatSoundsDefeat() {
        model.getPlayer().takeDamage(GameConfig.DEFAULT_HEALTH);
        model.consumeSoundEvents();

        model.update(GameConfig.MAX_FRAME_DELTA);

        assertEquals(GameStatus.GAME_OVER, model.getStatus(), "前置条件：应判阵亡");
        assertEquals(List.of(SoundEvent.DEFEAT), model.consumeSoundEvents());
    }

    // ========== 队列语义 ==========

    /** 取走即清空：控制层每帧只取一次，取过的不该在下一帧又响一遍。 */
    @Test
    void consumingDrainsTheQueue() throws Exception {
        pickUp(ItemType.HEAL);

        assertEquals(List.of(SoundEvent.ITEM_PICKUP), model.consumeSoundEvents());
        assertEquals(List.of(), model.consumeSoundEvents(), "取过一次之后队列该是空的");
    }

    /** 开新一局要把上一局没来得及播的事件清掉，免得开局就响上一局的一声。 */
    @Test
    void initGameClearsPendingSounds() throws Exception {
        pickUp(ItemType.HEAL);

        model.initGame();

        assertEquals(List.of(), model.consumeSoundEvents(), "新一局不该带上上一局的音效");
    }

    /** 回主菜单同样清空：菜单里不该再冒出上局的残响。 */
    @Test
    void toMenuClearsPendingSounds() throws Exception {
        pickUp(ItemType.HEAL);

        model.toMenu();

        assertEquals(List.of(), model.consumeSoundEvents(), "回主菜单不该带上上一局的音效");
    }

    /** 队列无事件时返回空表而不是 null，控制层可以直接遍历。 */
    @Test
    void emptyQueueReturnsEmptyList() {
        assertEquals(List.of(), model.consumeSoundEvents());
    }
}
