package cn.edu.csu.plane.controller;

import cn.edu.csu.plane.model.BombWave;
import cn.edu.csu.plane.model.Bullet;
import cn.edu.csu.plane.model.Enemy;
import cn.edu.csu.plane.model.GameModel;
import cn.edu.csu.plane.model.GameStatus;
import cn.edu.csu.plane.model.HitEffect;
import cn.edu.csu.plane.model.Item;
import cn.edu.csu.plane.model.Player;
import cn.edu.csu.plane.util.Difficulty;
import cn.edu.csu.plane.util.GameConfig;
import cn.edu.csu.plane.view.GameView;
import javafx.application.Platform;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * GameController 单元测试：注入假帧循环与假模型/假视图，直接喂合成时间戳调 onFrame，
 * 从而在不启动 JavaFX 工具箱、不依赖真实计时器的前提下验证单帧编排。
 *
 * <p>覆盖：初始化与循环启停、步长计算与截断（NF-03）、输入到位移的换算、
 * 非 PLAYING 状态冻结、渲染时机、终局结算只弹一次（含通关）、暂停/恢复/重开/回主菜单。</p>
 */
class GameControllerTest {

    private static final long SECOND = 1_000_000_000L;
    /** 一帧 ≈ 16.67ms，对应 60 FPS。 */
    private static final long FRAME_NANOS = 16_666_666L;
    private static final double FRAME_SECONDS = FRAME_NANOS / 1e9;

    /** 合成时钟：从 1 秒起步，绕开 lastFrameNanos 用 0 当"无基准"的哨兵值。 */
    private long clock = SECOND;
    /** 上一帧喂进去的时间戳，用来精确构造"距上一帧多久"。 */
    private long lastFrameAt;

    private FakeModel model;
    private RecordingView view;
    private FakeFrameLoop loop;
    private GameController controller;

    /**
     * GameView 的构造会 new 一个 JavaFX Canvas，必须先起 JavaFX 工具箱，
     * 否则报 "Internal graphics not initialized yet"。测试不打开任何窗口，
     * 整类跑完统一关掉工具箱，免得 Application Thread 挂着不退。
     */
    @BeforeAll
    static void startJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // 同一个 JVM 里已经被别的测试类起过了，直接用
        }
    }

    /**
     * 故意不在测完关掉工具箱：JavaFX 的 {@code Platform.exit()} 关掉后就再也起不来了，
     * 同一个 JVM 里后面的测试类（如 {@code MainMenuViewTest}）还要用它，一 exit 就会
     * 卡在等 FX 线程上。工具箱子进程由 Surefire 跑完整个 JVM 时统一收掉。
     */
    @AfterAll
    static void keepJavaFxToolkitAlive() {
        // 什么都不做，留在这里是为了说明"不关"是有意为之
    }

    @BeforeEach
    void setUp() {
        model = new FakeModel();
        view = onFxThread(RecordingView::new);
        loop = new FakeFrameLoop();
        controller = new GameController(model, view, loop);
    }

    /**
     * 在 FX 应用线程上建对象，并等它建完再返回。
     * GameView 的构造里会 new 一个 Canvas，JavaFX 对 Canvas 有线程校验，
     * 直接在测试线程上 new 会抛 "Not on FX application thread"。
     */
    private static <T> T onFxThread(Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<RuntimeException> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(supplier.get());
            } catch (RuntimeException e) {
                failure.set(e);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等 JavaFX 线程建视图超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等 JavaFX 线程建视图被中断", e);
        }
        if (failure.get() != null) {
            throw failure.get();
        }
        return result.get();
    }

    /** 推进一帧，并让合成时钟往前走一帧。 */
    private void step() {
        lastFrameAt = clock;
        controller.onFrame(clock);
        clock += FRAME_NANOS;
    }

    // ========== 1. 初始化与循环启停 ==========

    @Test
    void start_shouldInitGameAndStartLoop() {
        // 验证点：start() 既初始化一局，也启动帧循环
        controller.start();
        assertEquals(1, model.initGameCount, "start() 应初始化一局");
        assertEquals(1, loop.startCount, "start() 应启动帧循环");
        assertEquals(GameStatus.PLAYING, model.getStatus(), "开局后状态应为 PLAYING");
    }

    @Test
    void firstFrameAfterStartLoop_shouldHaveZeroDelta() {
        // 验证点：开循环后的第一帧没有上一帧作基准，步长必须是 0，
        // 否则会把"从程序启动到开局"的时长当成一步位移
        controller.start();
        step();
        assertEquals(0.0, model.lastDelta, 1e-9, "第一帧步长应为 0");
        assertEquals(0.0, model.lastMoveDx, 1e-9, "步长为 0 时不该产生位移");
        assertEquals(0.0, model.lastMoveDy, 1e-9, "步长为 0 时不该产生位移");
    }

    @Test
    void stop_shouldStopLoopAndResetFrameClock() {
        // 验证点：停循环后步长基准清零，重新开循环的第一帧不会把停机时长算成一步
        controller.start();
        step();
        controller.stop();
        assertEquals(1, loop.stopCount, "stop() 应停掉帧循环");

        controller.startLoop();
        step();
        assertEquals(0.0, model.lastDelta, 1e-9, "重开循环后的第一帧步长应为 0");
    }

    // ========== 2. 步长计算与截断 ==========

    @Test
    void deltaTime_shouldBeActualSecondsBetweenFrames() {
        // 验证点：步长按相邻两帧的纳秒差折算成秒
        controller.start();
        step();
        controller.onFrame(lastFrameAt + 10_000_000L);   // 距上一帧 10ms
        assertEquals(0.01, model.lastDelta, 1e-9, "10ms 的帧间隔应换算成 0.01 秒");
    }

    @Test
    void deltaTime_shouldBeClampedToMaxFrameDelta() {
        // 验证点：窗口拖拽/GC 停顿造成的长间隔必须截断（NF-03 防穿透），不能被当一步
        controller.start();
        step();
        controller.onFrame(lastFrameAt + 5 * SECOND);
        assertEquals(GameConfig.MAX_FRAME_DELTA, model.lastDelta, 1e-9,
                "单帧步长超过 MAX_FRAME_DELTA 时必须截断，否则高速自爆机可能穿透碰撞盒");
    }

    // ========== 3. 输入 → 位移 ==========

    @Test
    void eachDirection_shouldMovePlayerThatWay() {
        // 验证点：输入层给的是 -1/0/1 方向，主循环负责乘速度与步长；
        // 这里同时验证"控制器读的就是自己持有的那份输入状态"
        controller.start();
        step();   // 基准帧
        assertMove(KeyCode.D, 1, 0);
        assertMove(KeyCode.A, -1, 0);
        assertMove(KeyCode.S, 0, 1);
        assertMove(KeyCode.W, 0, -1);
    }

    /** 按住 key 走一帧，断言这一帧的位移 = 方向 × 速度 × 步长。 */
    private void assertMove(KeyCode key, int dirX, int dirY) {
        controller.getInput().handleKeyPressed(key);
        step();
        controller.getInput().handleKeyReleased(key);

        double expectedDx = dirX * GameConfig.PLAYER_SPEED * FRAME_SECONDS;
        double expectedDy = dirY * GameConfig.PLAYER_SPEED * FRAME_SECONDS;
        assertEquals(expectedDx, model.lastMoveDx, 1e-9, key + " 的横向位移应为 方向×速度×步长");
        assertEquals(expectedDy, model.lastMoveDy, 1e-9, key + " 的纵向位移应为 方向×速度×步长");
    }

    @Test
    void aSecondInputHandler_shouldNotDriveTheController() {
        // 验证点：按键状态是 InputHandler 的实例字段，控制器只认自己持有的那一份；
        // 若有人自己 new 一个去绑定场景，主循环读到的永远是 0（历史上方向键全失灵的成因）
        controller.start();
        step();
        InputHandler rogue = new InputHandler();
        rogue.handleKeyPressed(KeyCode.D);
        step();
        assertEquals(0.0, model.lastMoveDx, 1e-9, "控制器不该读到别的 InputHandler 实例的按键状态");
        assertNotSame(rogue, controller.getInput(), "控制器持有一个独立的 InputHandler");
    }

    // ========== 4. 非 PLAYING 状态冻结 ==========

    @Test
    void pausedRun_shouldNotAdvanceModelButStillRender() {
        // 验证点：暂停时实体运动、生成、计分全部冻结（F12），但画面仍要渲染
        controller.start();
        controller.pause();
        step();
        assertEquals(0, model.updateCount, "暂停时不该推模型");
        assertEquals(0, model.moveCount, "暂停时不该移动战机");
        assertEquals(1, view.renderCount, "暂停时仍要渲染，否则画面会停止刷新");
    }

    @Test
    void menuState_shouldNotAdvanceModel() {
        // 验证点：菜单态没有本局，帧回调不该推模型
        controller.toMenu();
        step();
        assertEquals(0, model.updateCount, "主菜单态不该推模型");
        assertEquals(0, model.moveCount, "主菜单态不该移动战机");
    }

    @Test
    void everyFrame_shouldRenderOnceWithCurrentSnapshot() {
        // 验证点：每帧渲染一次，且渲染的是模型当前的实体列表（不是缓存的旧快照）
        controller.start();
        step();
        step();
        assertEquals(2, view.renderCount, "两帧应渲染两次");
        assertSame(model.getPlayer(), view.lastPlayer, "渲染的应是模型当前的战机");
        assertSame(model.getEnemies(), view.lastEnemies, "渲染的应是模型当前的敌机列表");
        assertSame(model.getBullets(), view.lastBullets, "渲染的应是模型当前的子弹列表");
        assertSame(model.getItems(), view.lastItems, "渲染的应是模型当前的道具列表");
        assertSame(model.getHitEffects(), view.lastHitEffects, "渲染的应是模型当前的受击特效列表");
    }

    // ========== 5. 终局结算 ==========

    /** 难度（F16）要一路传到视图：渲染与结算都得带上当前档位，界面才显示得出来。 */
    @Test
    void difficultyIsForwardedToView() {
        model.setDifficulty(Difficulty.HARD);
        controller.start();

        step();
        assertEquals(Difficulty.HARD, view.lastRenderedDifficulty, "每帧渲染该带上当前难度");

        model.setScore(500);
        model.setStatus(GameStatus.GAME_OVER);
        step();
        assertEquals(Difficulty.HARD, view.lastShownDifficulty, "结算面板该显示本局难度");
    }

    @Test
    void gameOver_shouldShowResultExactlyOnce() {
        // 验证点：阵亡后结算只弹一次，不能每帧都弹
        controller.start();
        model.setScore(1234);
        model.setStatus(GameStatus.GAME_OVER);

        step();
        assertEquals(1, view.showGameOverCount, "阵亡当帧应弹结算");
        assertEquals(1234, view.lastShownScore, "结算必须显示本局得分");

        step();
        step();
        assertEquals(1, view.showGameOverCount, "结算只弹一次，后续帧不该重复弹");
    }

    @Test
    void victory_shouldAlsoShowResult() {
        // 验证点：通关与阵亡都是终局，都要弹结算（F17）
        controller.start();
        model.setScore(GameConfig.VICTORY_SCORE);
        model.setStatus(GameStatus.VICTORY);

        step();
        assertEquals(1, view.showGameOverCount, "通关是终局，同样要弹结算");
        assertEquals(GameConfig.VICTORY_SCORE, view.lastShownScore, "结算应显示通关得分");
    }

    @Test
    void restart_shouldRearmResultPanel() {
        // 验证点：重开后新一局不能立刻弹上一局的结算，再次终局时要重新弹
        controller.start();
        model.setStatus(GameStatus.GAME_OVER);
        step();
        assertEquals(1, view.showGameOverCount, "前置条件：第一局已弹结算");

        controller.restart();
        assertEquals(GameStatus.PLAYING, model.getStatus(), "重开后应回到 PLAYING");
        assertEquals(2, model.initGameCount, "重开应重新初始化一局");
        assertEquals(2, loop.startCount, "重开应确保帧循环在跑");
        step();
        assertEquals(1, view.showGameOverCount, "重开的新一局不该立刻弹上一局的结算");

        model.setStatus(GameStatus.GAME_OVER);
        step();
        assertEquals(2, view.showGameOverCount, "重开后再次终局应重新弹结算");
    }

    // ========== 6. 暂停 / 恢复 ==========

    @Test
    void pause_shouldClearHeldKeys() {
        // 验证点：暂停时清掉按键状态，避免恢复后战机自己走
        controller.start();
        controller.getInput().handleKeyPressed(KeyCode.D);
        controller.pause();

        assertEquals(1, model.pauseCount, "pause() 应交给模型处理");
        assertEquals(GameStatus.PAUSED, model.getStatus(), "暂停后状态应为 PAUSED");
        assertEquals(0.0, controller.getInput().getMoveX(), "暂停时必须清空按键状态");
    }

    @Test
    void resume_shouldRestartFrameClock() {
        // 验证点：恢复后重新计步，不能把暂停的时长算成一帧
        controller.start();
        step();
        controller.pause();
        controller.resume();
        assertEquals(GameStatus.PLAYING, model.getStatus(), "恢复后应回到 PLAYING");

        controller.onFrame(clock + 30 * SECOND);
        assertEquals(0.0, model.lastDelta, 1e-9, "恢复后的第一帧不该把暂停的 30 秒算成一步");
    }

    @Test
    void togglePause_shouldOnlyFlipBetweenPlayingAndPaused() {
        // 验证点：暂停键只在进行中/暂停之间切换，终局与菜单态按它无副作用
        controller.start();
        controller.togglePause();
        assertEquals(GameStatus.PAUSED, model.getStatus(), "进行中按暂停键应转为 PAUSED");
        controller.togglePause();
        assertEquals(GameStatus.PLAYING, model.getStatus(), "暂停中按暂停键应转为 PLAYING");

        model.setStatus(GameStatus.GAME_OVER);
        controller.togglePause();
        assertEquals(GameStatus.GAME_OVER, model.getStatus(), "终局时按暂停键不该改变状态");
        assertEquals(1, model.pauseCount, "终局时按暂停键不该调用模型 pause");

        controller.toMenu();
        controller.togglePause();
        assertEquals(GameStatus.MENU, model.getStatus(), "主菜单按暂停键不该改变状态");
    }

    // ========== 7. 回主菜单 ==========

    @Test
    void toMenu_shouldClearRunAndStopLoop() {
        // 验证点：回主菜单要清空本局并停止逐帧回调（菜单态不逐帧跑）
        controller.start();
        step();
        controller.toMenu();

        assertEquals(1, model.toMenuCount, "toMenu() 应交给模型处理");
        assertEquals(GameStatus.MENU, model.getStatus(), "回主菜单后状态应为 MENU");
        assertEquals(1, loop.stopCount, "回主菜单后应停止帧循环");

        int updatesBefore = model.updateCount;
        step();
        assertEquals(updatesBefore, model.updateCount, "菜单态不该再推模型");
    }

    @Test
    void toMenu_shouldResetFrameClock() {
        // 验证点：菜单停循环后步长基准清零，重新开局的第一帧步长为 0
        controller.start();
        step();
        controller.toMenu();
        controller.start();

        step();
        assertEquals(0.0, model.lastDelta, 1e-9, "重新开局的第一帧步长应为 0");
    }

    // ========== 8. 埋点常量与生产帧循环 ==========

    @Test
    void pauseKey_shouldBeP() {
        // 验证点：F12 条目规定暂停键为 P
        assertEquals(KeyCode.P, GameController.PAUSE_KEY, "F12 条目规定暂停键为 P");
    }

    @Test
    void productionFrameLoop_stopBeforeStart_shouldBeNoOp() {
        // 验证点：FrameLoop 契约要求"未启动时调用无副作用"。
        // 只验这一条路径：start() 会真的构造 AnimationTimer，需要 JavaFX 工具箱，
        // 那是集成测试的活；纯单测能保证的是它不因重复/提前 stop 而崩。
        FrameLoop production = new GameController.AnimationFrameLoop(now -> { });
        assertDoesNotThrow(production::stop, "没启动就 stop 不该抛异常");
        assertDoesNotThrow(production::stop, "重复 stop 不该抛异常");
    }

    // ========== 测试替身 ==========

    /** 手写假模型：只记录控制层喂进来的调用，不跑任何游戏逻辑。 */
    private static final class FakeModel implements GameModel {

        private final Player player = new Player(0, 0, 40, 40);
        private final List<Enemy> enemies = new ArrayList<>();
        private final List<Bullet> bullets = new ArrayList<>();
        private final List<Item> items = new ArrayList<>();
        private final List<BombWave> waves = new ArrayList<>();
        private final List<HitEffect> hitEffects = new ArrayList<>();

        private GameStatus status = GameStatus.MENU;
        private int score;
        private int highScore;
        private Difficulty difficulty = Difficulty.NORMAL;

        private int initGameCount;
        private int updateCount;
        private int moveCount;
        private int pauseCount;
        private int resumeCount;
        private int toMenuCount;
        private double lastDelta;
        private double lastMoveDx;
        private double lastMoveDy;

        void setStatus(GameStatus status) {
            this.status = status;
        }

        void setScore(int score) {
            this.score = score;
        }

        @Override
        public void initGame() {
            initGameCount++;
            score = 0;
            status = GameStatus.PLAYING;
        }

        @Override
        public void update(double deltaTime) {
            updateCount++;
            lastDelta = deltaTime;
        }

        @Override
        public void pause() {
            pauseCount++;
            status = GameStatus.PAUSED;
        }

        @Override
        public void resume() {
            resumeCount++;
            status = GameStatus.PLAYING;
        }

        @Override
        public void toMenu() {
            toMenuCount++;
            status = GameStatus.MENU;
        }

        @Override
        public void movePlayer(double dx, double dy) {
            moveCount++;
            lastMoveDx = dx;
            lastMoveDy = dy;
            player.move(dx, dy);
        }

        @Override
        public Player getPlayer() {
            return player;
        }

        @Override
        public List<Enemy> getEnemies() {
            return enemies;
        }

        @Override
        public List<Bullet> getBullets() {
            return bullets;
        }

        @Override
        public List<Item> getItems() {
            return items;
        }

        @Override
        public List<BombWave> getWaves() {
            return waves;
        }

        @Override
        public List<HitEffect> getHitEffects() {
            return hitEffects;
        }

        @Override
        public int getScore() {
            return score;
        }

        @Override
        public int getHealth() {
            return GameConfig.DEFAULT_HEALTH;
        }

        @Override
        public int getLevel() {
            return 1;
        }

        @Override
        public int getHighScore() {
            return highScore;
        }

        @Override
        public int getHighScore(Difficulty difficulty) {
            return highScore;
        }

        @Override
        public Difficulty getDifficulty() {
            return difficulty;
        }

        @Override
        public void setDifficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
        }

        @Override
        public GameStatus getStatus() {
            return status;
        }

        @Override
        public double getElapsedTime() {
            return 0;
        }

        @Override
        public void addScore(int amount) {
            score += amount;
        }
    }

    /** 假视图：只记录被渲染了几次、渲染的是谁、结算弹了几次与得分。 */
    private static final class RecordingView extends GameView {

        private int renderCount;
        private int showGameOverCount;
        private long lastShownScore;
        private Difficulty lastShownDifficulty;
        private Difficulty lastRenderedDifficulty;
        private Player lastPlayer;
        private List<Enemy> lastEnemies;
        private List<Bullet> lastBullets;
        private List<Item> lastItems;
        private List<HitEffect> lastHitEffects;

        @Override
        public void render(Player player, List<Enemy> enemies, List<Bullet> bullets, List<Item> items,
                           List<BombWave> waves, List<HitEffect> hitEffects, int score, int level,
                           int highScore, GameStatus status, Difficulty difficulty, double deltaTime) {
            renderCount++;
            lastPlayer = player;
            lastEnemies = enemies;
            lastBullets = bullets;
            lastItems = items;
            lastHitEffects = hitEffects;
            lastRenderedDifficulty = difficulty;
        }

        @Override
        public void showGameOver(GameStatus status, long score, Difficulty difficulty) {
            showGameOverCount++;
            lastShownScore = score;
            lastShownDifficulty = difficulty;
        }
    }

    /** 假帧循环：只记 start/stop 次数，回调由测试手动触发（调 onFrame）。 */
    private static final class FakeFrameLoop implements FrameLoop {

        private int startCount;
        private int stopCount;

        @Override
        public void start() {
            startCount++;
        }

        @Override
        public void stop() {
            stopCount++;
        }
    }
}
