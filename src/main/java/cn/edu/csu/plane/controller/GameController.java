package cn.edu.csu.plane.controller;

import cn.edu.csu.plane.model.GameModel;
import cn.edu.csu.plane.model.GameStatus;
import cn.edu.csu.plane.util.GameConfig;
import cn.edu.csu.plane.view.GameView;

/**
 * 主控制器：驱动游戏主循环，协调模型（Model）与视图（View），并管理状态流转。
 *
 * <p>主循环采用 JavaFX 的 {@code GameLoop} 在应用线程逐帧回调，不新起线程、
 * 不使用 Timer，因此不存在跨线程中间态，也不会把阻塞调用放到界面线程之外。</p>
 */
public class GameController {

    private final GameModel model;
    private final GameView view;
    private final InputHandler input = new InputHandler();

    private GameLoop loop;
    private long lastFrameNanos;
    private boolean awaitingResume;

    public GameController(GameModel model, GameView view) {
        this.model = model;
        this.view = view;
    }

    /** 启动游戏主循环（进入一局）。 */
    public void start() {
        model.initGame();
        startLoop();
    }

    /** 只启动帧循环，不重置本局（主菜单已在 PLAYING 态的续跑场景）。 */
    public void startLoop() {
        if (loop == null) {
            loop = new GameLoop(this::onFrame);
        }
        lastFrameNanos = 0;
        loop.start();
    }

    /** 停止帧循环。 */
    public void stop() {
        if (loop != null) {
            loop.stop();
        }
        lastFrameNanos = 0;
    }

    /**
     * 单帧回调：算步长 → 截断 → 读输入 → 推模型 → 渲染。
     *
     * <p>步长截断见 {@link GameConfig#MAX_FRAME_DELTA}：窗口拖拽或 GC 停顿会让相邻帧
     * 间隔突然变大，不截断则高速自爆机单帧位移可能超过碰撞盒最短边而产生穿透（NF-03）。</p>
     */
    private void onFrame(long nowNanos) {
        double deltaTime = 0;
        if (lastFrameNanos != 0) {
            deltaTime = (nowNanos - lastFrameNanos) / 1_000_000_000.0;
            if (deltaTime > GameConfig.MAX_FRAME_DELTA) {
                deltaTime = GameConfig.MAX_FRAME_DELTA;
            }
        }
        lastFrameNanos = nowNanos;

        if (model.getStatus() == GameStatus.PLAYING) {
            model.movePlayer(input.getMoveX(), input.getMoveY());
            model.update(deltaTime);
        }

        if (model.getStatus() == GameStatus.GAME_OVER && !awaitingResume) {
            awaitingResume = true;
            onGameOver();
        } else if (model.getStatus() != GameStatus.GAME_OVER) {
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

    /** 重新开始：复位模型并继续同一个帧循环。 */
    public void restart() {
        model.initGame();
        awaitingResume = false;
        lastFrameNanos = 0;
        if (loop == null) {
            startLoop();
        }
    }

    /** 返回主菜单。 */
    public void toMenu() {
        model.toMenu();
        awaitingResume = false;
    }

    /** 每帧渲染画面。 */
    public void render() {
        view.render(model.getPlayer(), model.getEnemies(), model.getBullets(), model.getItems());
    }

    /** 一局结束：交给视图展示本局得分。 */
    private void onGameOver() {
        view.showGameOver(model.getScore());
    }

    /** 暂停键位（F12 条目规定为 P 键，键位在此绑定）。 */
    public static final javafx.scene.input.KeyCode PAUSE_KEY = javafx.scene.input.KeyCode.P;

    InputHandler getInput() {
        return input;
    }

    /**
     * 帧循环：用 JavaFX AnimationTimer 挂到应用线程，替代 Timer/自定义线程。
     * 单独抽出是为了让 GameController 不直接依赖具体定时器实现，便于测试注入。
     */
    static class GameLoop {
        private final java.util.function.LongConsumer frameHandler;
        private javafx.animation.AnimationTimer timer;

        GameLoop(java.util.function.LongConsumer frameHandler) {
            this.frameHandler = frameHandler;
        }

        void start() {
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

        void stop() {
            if (timer != null) {
                timer.stop();
            }
        }
    }
}
