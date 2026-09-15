package cn.edu.csu.plane.controller;

import cn.edu.csu.plane.model.GameModel;
import cn.edu.csu.plane.model.GameStatus;
import cn.edu.csu.plane.util.GameConfig;
import cn.edu.csu.plane.view.GameView;
import javafx.scene.Scene;

/**
 * 主控制器：驱动游戏主循环，协调模型（Model）与视图（View），并管理状态流转。
 *
 * <p>主循环采用 JavaFX {@code AnimationTimer} 在应用线程逐帧回调
 * （生产实现见 {@link AnimationFrameLoop}），不新起线程、不使用 Timer，
 * 因此不存在跨线程中间态，也不会把阻塞调用放到界面线程之外。</p>
 *
 * <p>帧循环藏在 {@link FrameLoop} 之后，可注入替换：测试注入一个手动触发的假循环，
 * 再直接调用 {@link #onFrame(long)}，就能在没有 JavaFX 工具箱的环境下验证单帧编排。</p>
 */
public class GameController {

    private final GameModel model;
    private final GameView view;
    private final InputHandler input = new InputHandler();
    private final FrameLoop loop;

    private long lastFrameNanos;
    private double lastDelta;
    private boolean awaitingResume;

    /** 生产构造：帧循环用 JavaFX AnimationTimer。 */
    public GameController(GameModel model, GameView view) {
        this(model, view, null);
    }

    /**
     * 可注入构造：{@code loop} 传 null 时用生产实现 {@link AnimationFrameLoop}。
     * 测试注入手动触发的 {@link FrameLoop} 后，可直接调 {@link #onFrame(long)} 驱动单帧，
     * 无需启动 JavaFX 工具箱。
     */
    public GameController(GameModel model, GameView view, FrameLoop loop) {
        this.model = model;
        this.view = view;
        this.loop = (loop != null) ? loop : new AnimationFrameLoop(this::onFrame);
    }

    /**
     * 把键盘事件绑到控制器自己持有的那个 {@link InputHandler} 上。
     *
     * <p>按键状态（上/下/左/右）是 {@code InputHandler} 的实例字段，谁绑定、谁读取
     * 必须是同一个对象。调用方另 new 一个 {@code InputHandler} 去 attach 场景，
     * 绑上的是另一个对象的状态，主循环读到的永远是 0，方向键与 WASD 会全部失灵
     * —— 所以绑定入口只留这一个，由持有者绑自己读的那个实例。</p>
     */
    public void attachInput(Scene scene) {
        input.attach(scene);
    }

    /** 启动游戏主循环（进入一局）。 */
    public void start() {
        model.initGame();
        startLoop();
    }

    /** 只启动帧循环，不重置本局。重复调用幂等。 */
    public void startLoop() {
        loop.start();
        lastFrameNanos = 0;
    }

    /** 停止帧循环。未启动时调用无副作用。 */
    public void stop() {
        loop.stop();
        lastFrameNanos = 0;
    }

    /**
     * 单帧回调：算步长 → 截断 → 读输入 → 推模型 → 渲染。
     *
     * <p>步长截断见 {@link GameConfig#MAX_FRAME_DELTA}：窗口拖拽或 GC 停顿会让相邻帧
     * 间隔突然变大，不截断则高速自爆机单帧位移可能超过碰撞盒最短边而产生穿透（NF-03）。</p>
     *
     * <p>包内可见：生产由 {@link AnimationFrameLoop} 每帧调用，测试可直接喂合成时间戳驱动。</p>
     */
    void onFrame(long nowNanos) {
        double deltaTime = 0;
        if (lastFrameNanos != 0) {
            deltaTime = (nowNanos - lastFrameNanos) / 1_000_000_000.0;
            if (deltaTime > GameConfig.MAX_FRAME_DELTA) {
                deltaTime = GameConfig.MAX_FRAME_DELTA;
            }
        }
        lastFrameNanos = nowNanos;
        lastDelta = deltaTime;

        if (model.getStatus() == GameStatus.PLAYING) {
            // 输入层给的是 -1/0/1 方向，得乘上速度和这一帧的步长才是位移像素；
            // 直接把方向当位移传的话，战机一帧只挪 1 像素，而且帧率一变速度就变。
            model.movePlayer(input.getMoveX() * GameConfig.PLAYER_SPEED * deltaTime,
                    input.getMoveY() * GameConfig.PLAYER_SPEED * deltaTime);
            model.update(deltaTime);
        }

        if (isRunOver(model.getStatus()) && !awaitingResume) {
            awaitingResume = true;
            onRunOver();
        } else if (!isRunOver(model.getStatus())) {
            awaitingResume = false;
        }

        render();
    }

    /** 暂停/恢复：由暂停键（P）或窗口失焦触发。 */
    public void togglePause() {
        if (model.getStatus() == GameStatus.PLAYING) {
            pause();
        } else if (model.getStatus() == GameStatus.PAUSED) {
            resume();
        }
    }

    public void pause() {
        model.pause();
        input.clearAll();   // 暂停时清掉按键状态，避免恢复后战机自己走
    }

    public void resume() {
        model.resume();
        lastFrameNanos = 0;   // 恢复后重新计步，避免把暂停的时长算成一帧
    }

    /** 重新开始：复位模型并继续帧循环（循环若已被 {@link #stop()} 停掉，这里重新启动）。 */
    public void restart() {
        model.initGame();
        awaitingResume = false;
        startLoop();
    }

    /** 返回主菜单：清空本局并停止帧循环，菜单态不再逐帧回调。 */
    public void toMenu() {
        model.toMenu();
        awaitingResume = false;
        stop();
    }

    /** 每帧渲染画面：把渲染所需的本局数据（含当前难度与本局用时）一并交给视图。 */
    public void render() {
        view.render(model.getPlayer(), model.getEnemies(), model.getBullets(), model.getItems(),
                model.getWaves(), model.getHitEffects(), model.getScore(), model.getLevel(),
                model.getHighScore(), model.getStatus(), model.getDifficulty(),
                model.getElapsedTime(), lastDelta);
    }

    /** 一局是否已经结束。通关与阵亡都是终局，都得弹结算。 */
    private static boolean isRunOver(GameStatus status) {
        return status == GameStatus.GAME_OVER || status == GameStatus.VICTORY;
    }

    /** 一局结束：把终局状态、本局得分、本局难度、本局用时与"是否刷新纪录"交给视图展示结算。 */
    private void onRunOver() {
        view.showGameOver(model.getStatus(), model.getScore(), model.getDifficulty(),
                model.getRunClearTime(), model.isNewClearRecord());
    }

    /** 暂停键位：Esc，按一下暂停、再按一下恢复（键位在此绑定）。 */
    public static final javafx.scene.input.KeyCode PAUSE_KEY = javafx.scene.input.KeyCode.ESCAPE;

    InputHandler getInput() {
        return input;
    }

    /**
     * 生产用帧循环：用 JavaFX AnimationTimer 挂到应用线程，替代 Timer/自定义线程。
     * 实现 {@link FrameLoop} 是为了让 GameController 不直接依赖具体定时器，便于测试注入。
     */
    static final class AnimationFrameLoop implements FrameLoop {
        private final java.util.function.LongConsumer frameHandler;
        private javafx.animation.AnimationTimer timer;

        AnimationFrameLoop(java.util.function.LongConsumer frameHandler) {
            this.frameHandler = frameHandler;
        }

        @Override
        public void start() {
            if (timer == null) {
                timer = new javafx.animation.AnimationTimer() {
                    @Override
                    public void handle(long now) {
                        frameHandler.accept(now);
                    }
                };
            }
            timer.start();
        }

        @Override
        public void stop() {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}
