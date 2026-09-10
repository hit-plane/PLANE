package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GameModelImpl 的 Model 层核心业务单元测试。
 * 仅通过 GameModel 公共接口进行测试，不依赖 JavaFX 界面。
 */
class PlaneGameModelTest {

    private GameModel model;

    @BeforeEach
    void setUp() {
        model = new GameModelImpl();
        model.initGame();
    }

    // ========== 1. 初始化校验 ==========

    @Test
    void initGame_shouldSetDefaultScoreHealthAndStatus() {
        // 验证点：initGame() 后分数为 0、血量为满、状态为 PLAYING
        assertEquals(0, model.getScore(), "初始化后分数应为 0");
        assertEquals(GameConfig.DEFAULT_HEALTH, model.getHealth(), "初始化后血量应为默认最大值");
        assertEquals(GameStatus.PLAYING, model.getStatus(), "初始化后状态应为 PLAYING");
        assertEquals(1, model.getLevel(), "初始化后关卡应为 1");
        assertTrue(model.getEnemies().isEmpty(), "初始化后场上无敌机");
        assertTrue(model.getBullets().isEmpty(), "初始化后场上无子弹");
    }

    // ========== 2. 玩家操作 ==========

    @Test
    void movePlayer_shouldExecuteWithoutException() {
        // 验证点：左右移动不抛异常，玩家位置发生变化
        double originalX = model.getPlayer().getX();
        assertDoesNotThrow(() -> model.movePlayer(50, 0), "右移不应抛异常");
        assertEquals(originalX + 50, model.getPlayer().getX(), "右移 50 后 X 坐标应增加 50");

        assertDoesNotThrow(() -> model.movePlayer(-30, 0), "左移不应抛异常");
        assertEquals(originalX + 20, model.getPlayer().getX(), "左移 30 后 X 坐标应回到 originalX+20");
    }

    @Test
    void movePlayer_shouldClampAtBoundary() {
        // 验证点：玩家不会移出窗口边界
        model.movePlayer(-9999, 0);
        assertTrue(model.getPlayer().getX() >= 0, "X 坐标不应小于 0");

        model.movePlayer(9999, 0);
        assertTrue(model.getPlayer().getX() + model.getPlayer().getWidth() <= GameConfig.WINDOW_WIDTH,
                "玩家右边界不应超出窗口宽度");
    }

    // ========== 3. 帧推进 ==========

    @Test
    void update_singleTick_shouldRunWithoutException() {
        // 验证点：单帧 tick 正常运行，状态仍为 PLAYING
        assertDoesNotThrow(() -> model.update(1.0 / 60), "单帧 update 不应抛异常");
        assertEquals(GameStatus.PLAYING, model.getStatus(), "单帧推进后状态应保持 PLAYING");
    }

    @Test
    void update_multipleTicks_shouldAdvanceGameState() {
        // 验证点：多帧推进后游戏正常运行，时间累计触发敌机生成
        double frameDelta = 1.0 / 60;
        for (int i = 0; i < 180; i++) { // 约 3 秒
            model.update(frameDelta);
        }
        assertEquals(GameStatus.PLAYING, model.getStatus(), "3 秒后状态应仍为 PLAYING");
        assertFalse(model.getEnemies().isEmpty(), "3 秒后应至少生成了一架敌机");
    }

    // ========== 4. 碰撞计分 ==========

    @Test
    void addScore_shouldIncreaseScoreAndTriggerLevelUp() {
        // 验证点：通过 addScore 模拟击毁得分，分数正确累加并在满 1000 分时升关
        model.addScore(GameConfig.SCORE_PER_LEVEL - 1);
        assertEquals(GameConfig.SCORE_PER_LEVEL - 1, model.getScore(), "未到关卡线时分数照常累加");
        assertEquals(1, model.getLevel(), "差 1 分不该升关");

        model.addScore(1);
        assertEquals(GameConfig.SCORE_PER_LEVEL, model.getScore(), "再加 1 分总分应为 1000");
        assertEquals(2, model.getLevel(), "累计满 1000 分应升到第 2 关");
    }

    @Test
    void addScore_reachVictoryThreshold_shouldSetVictoryStatus() {
        // 验证点：累计得分达到通关分数（配置项）后状态变为 VICTORY
        model.addScore(GameConfig.VICTORY_SCORE);
        model.update(1.0 / 60);
        assertEquals(GameStatus.VICTORY, model.getStatus(), "达到通关分数应判定胜利");
    }

    // ========== 5. 生命扣除 ==========

    @Test
    void playerTakeDamage_healthShouldDecrease() {
        // 验证点：玩家受伤后血量正确减少
        int initialHealth = model.getHealth();
        model.getPlayer().takeDamage(2);
        assertEquals(initialHealth - 2, model.getHealth(), "受到 2 点伤害后血量应减少 2");
    }

    @Test
    void playerHealthZero_shouldTriggerGameOver() {
        // 验证点：玩家血量归零后，update 中状态切换为 GAME_OVER
        model.getPlayer().takeDamage(GameConfig.DEFAULT_HEALTH);
        assertFalse(model.getPlayer().isAlive(), "血量归零后玩家应死亡");
        model.update(1.0 / 60);
        assertEquals(GameStatus.GAME_OVER, model.getStatus(), "玩家死亡后状态应为 GAME_OVER");
    }

    // ========== 6. 结束守卫 ==========

    @Test
    void update_afterGameOver_shouldNotChangeState() {
        // 验证点：游戏结束后，update 不再推进任何逻辑
        model.getPlayer().takeDamage(GameConfig.DEFAULT_HEALTH);
        model.update(1.0 / 60);
        assertEquals(GameStatus.GAME_OVER, model.getStatus());

        int scoreAtGameOver = model.getScore();
        int healthAtGameOver = model.getHealth();

        for (int i = 0; i < 60; i++) {
            model.update(1.0 / 60);
        }
        assertEquals(GameStatus.GAME_OVER, model.getStatus(), "结束后状态应保持 GAME_OVER");
        assertEquals(scoreAtGameOver, model.getScore(), "结束后分数不应变化");
        assertEquals(healthAtGameOver, model.getHealth(), "结束后血量不应变化");
        assertTrue(model.getEnemies().isEmpty(), "结束后不应再生成新敌机");
    }

    // ========== 7. 重置功能 ==========

    @Test
    void initGame_afterGameOver_shouldFullyResetState() {
        // 验证点：游戏结束后重新调用 initGame() 可完全重置所有状态
        model.getPlayer().takeDamage(GameConfig.DEFAULT_HEALTH);
        model.update(1.0 / 60);
        assertEquals(GameStatus.GAME_OVER, model.getStatus());

        model.initGame();
        assertEquals(GameStatus.PLAYING, model.getStatus(), "重置后状态应为 PLAYING");
        assertEquals(0, model.getScore(), "重置后分数应为 0");
        assertEquals(GameConfig.DEFAULT_HEALTH, model.getHealth(), "重置后血量应回满");
        assertEquals(1, model.getLevel(), "重置后关卡应为 1");
        assertTrue(model.getPlayer().isAlive(), "重置后玩家应存活");
        assertTrue(model.getEnemies().isEmpty(), "重置后敌机列表应清空");
        assertTrue(model.getBullets().isEmpty(), "重置后子弹列表应清空");
    }
}
