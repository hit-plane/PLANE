package cn.edu.csu.plane;

import cn.edu.csu.plane.controller.GameController;
import cn.edu.csu.plane.model.GameModel;
import cn.edu.csu.plane.model.GameModelImpl;
import cn.edu.csu.plane.model.GameStatus;
import cn.edu.csu.plane.util.GameConfig;
import cn.edu.csu.plane.view.GameOverView;
import cn.edu.csu.plane.view.GameView;
import cn.edu.csu.plane.view.MainMenuView;
import cn.edu.csu.plane.view.PauseView;
import javafx.application.Application;
import javafx.event.EventHandler;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 程序入口：装配 model / view / controller 三层，叠加画布、主菜单、暂停与结算界面，
 * 并做界面状态切换（主菜单 → 进行中 → 暂停 / 结算）。
 *
 * <p>逻辑坐标系固定为配置里的窗口尺寸（模型边界与之绑定），实际显示交给一层缩放容器：
 * 窗口放不下时整体等比缩小并居中，放得下就保持原始尺寸，两边多出的空间留白（letterbox）。
 * 这样小屏也能完整显示，且画面比例与命中判定都不会变形。</p>
 *
 * <p><b>隐藏难度</b>：主菜单里依次敲下暗号 {@link #TORMENT_CODE} 会解锁第四档「折磨」
 * （见 {@link MainMenuView#unlockTorment()}），它不在常规三档里出现。暗号监听只在菜单态
 * 挂载，开局即摘除。</p>
 */
public class PlaneApp extends Application {

    /** 逻辑画布尺寸：模型边界、渲染坐标都基于它，缩放只发生在外层容器上。 */
    private static final double LOGICAL_W = GameConfig.WINDOW_WIDTH;
    private static final double LOGICAL_H = GameConfig.WINDOW_HEIGHT;
    /** 估算的窗口装饰（标题栏/边框）占用，避免按屏幕满高摆放时被标题栏顶出屏幕。 */
    private static final double SCREEN_MARGIN_W = 24;
    private static final double SCREEN_MARGIN_H = 64;

    /**
     * 隐藏难度「折磨」的解锁暗号：在主菜单依次敲 k-s-k-b-l 即出现第四档难度。
     *
     * <p>只在主菜单生效：一局进行中按这些键，{@code s} 会被
     * {@link cn.edu.csu.plane.controller.InputHandler} 当成下移指令，而暗号里恰好含 s，
     * 所以把监听限制在菜单态可以彻底避免"输暗号时战机乱窜"；开局的瞬间就把监听摘掉
     * （见 {@code onStart} 里的 {@code detachSecretCode()}），一局进行中再敲也不会触发。</p>
     *
     * <p>缓冲只保留"最近 N 个字母"（N = 暗号长度），所以不必区分长按重复与输入超时：
     * 只要最近 5 次字母键正好是暗号就解锁。数字键、方向键等非字母键不参与，
     * 玩家正常操作不会被算进来。</p>
     */
    private static final List<KeyCode> TORMENT_CODE =
            List.of(KeyCode.K, KeyCode.S, KeyCode.K, KeyCode.B, KeyCode.L);

    /** 暗号输入缓冲：只存最近的字母键，长度封顶为暗号长度。 */
    private final Deque<KeyCode> codeBuffer = new ArrayDeque<>();
    /** 暗号监听本身：开局时要能摘掉，否则一局进行中再敲会误触发（s 还是下移键）。 */
    private EventHandler<KeyEvent> secretCodeHandler;
    /** 主菜单是否正显示，决定暗号是否在监听状态。 */
    private boolean menuShown = true;

    @Override
    public void start(Stage stage) {
        GameModel model = new GameModelImpl();
        GameView gameView = new GameView();
        MainMenuView menuView = new MainMenuView();
        GameOverView gameOverView = new GameOverView();
        PauseView pauseView = new PauseView();
        GameController controller = new GameController(model, gameView);

        // 逻辑层：固定 600×900，画布在底层，菜单/暂停/结算界面按需覆盖在其上。
        // 固定 min=pref=max，子节点仍会被拉伸到整块逻辑尺寸，只是整块再被缩放。
        StackPane board = new StackPane();
        board.getChildren().addAll(gameView.getCanvas(), menuView.getNode(),
                gameOverView.getNode(), pauseView.getNode());
        board.setMinSize(LOGICAL_W, LOGICAL_H);
        board.setPrefSize(LOGICAL_W, LOGICAL_H);
        board.setMaxSize(LOGICAL_W, LOGICAL_H);

        // 外层只负责居中承载被缩放的 board，多余空间自然成为留白。
        StackPane root = new StackPane(board);

        double initialScale = initialScale();
        Scene scene = new Scene(root, LOGICAL_W * initialScale, LOGICAL_H * initialScale);

        // 必须让 controller 绑定它自己持有的输入实例：InputHandler 的按键状态是实例字段。
        controller.attachInput(scene);
        // 暂停键（P）：用 addEventHandler 追加，避免顶掉 InputHandler 装的移动键监听。
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == GameController.PAUSE_KEY) {
                controller.togglePause();
                // 暂停后显示暂停面板，恢复后隐藏
                pauseView.getNode().setVisible(model.getStatus() == GameStatus.PAUSED);
            }
        });
        stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (isFocused) {
                controller.resume();
                pauseView.getNode().setVisible(false);
            } else {
                controller.pause();
                if (model.getStatus() == GameStatus.PAUSED) {
                    pauseView.getNode().setVisible(true);
                }
            }
        });

        // 窗口尺寸一变就重算缩放，保证逻辑内容始终等比、居中、完整可见。
        Runnable fitBoard = () -> {
            double w = root.getWidth();
            double h = root.getHeight();
            if (w <= 0 || h <= 0) {
                return;
            }
            double scale = Math.min(w / LOGICAL_W, h / LOGICAL_H);
            board.setScaleX(scale);
            board.setScaleY(scale);
        };
        root.widthProperty().addListener((observable, oldValue, newValue) -> fitBoard.run());
        root.heightProperty().addListener((observable, oldValue, newValue) -> fitBoard.run());

        // 暂停面板按钮
        pauseView.setOnResume(() -> {
            pauseView.getNode().setVisible(false);
            controller.resume();
        });
        // 最高分按难度分档：每次回到菜单都按当前所选档位显示，切换难度也跟着刷新。
        Runnable refreshMenuHighScore =
                () -> menuView.setHighScore(model.getHighScore(menuView.getSelectedDifficulty()));

        pauseView.setOnQuit(() -> {
            pauseView.getNode().setVisible(false);
            refreshMenuHighScore.run();
            menuView.getNode().setVisible(true);
            attachSecretCode(scene);
            controller.toMenu();
        });

        // 一局结束：弹出结算面板
        gameView.setGameOverHandler((status, score, difficulty) -> {
            gameOverView.show(status, score, difficulty);
            gameOverView.getNode().setVisible(true);
        });

        // 主菜单：按所选皮肤与难度开始游戏。难度要在 start() 之前写进模型，
        // start() 内部会 initGame()，生成敌机时读的就是这个值。
        menuView.setOnDifficultyChange(difficulty -> refreshMenuHighScore.run());
        // 作弊开关（暗号解锁后出现）：切一下就写进模型并重算玩家机，开局前定好即可；
        // 模型侧对"未开局"状态是安全的（只是重建本档数值，不影响菜单态）。
        menuView.setOnCheatChange(cheat -> model.setCheatEnabled(cheat));
        refreshMenuHighScore.run();
        menuView.setOnStart(() -> {
            gameView.setPlayerSkin(menuView.getSelectedPlaneSkin());
            gameView.setPlayerBulletSkin(menuView.getSelectedBulletSkin());
            model.setDifficulty(menuView.getSelectedDifficulty());
            model.setCheatEnabled(menuView.isCheatEnabled());
            detachSecretCode(scene);
            menuShown = false;
            menuView.getNode().setVisible(false);
            gameOverView.getNode().setVisible(false);
            pauseView.getNode().setVisible(false);
            controller.start();
        });

        // 暗号监听只在菜单态生效：开局时摘掉（见上面的 setOnStart），回到菜单再挂上。
        secretCodeHandler = event -> {
            if (menuShown && handleSecretKey(event.getCode())) {
                menuView.unlockTorment();
                refreshMenuHighScore.run();
            }
        };
        attachSecretCode(scene);

        // 结算面板按钮
        gameOverView.setOnRestart(() -> {
            gameOverView.getNode().setVisible(false);
            controller.restart();
        });
        gameOverView.setOnMenu(() -> {
            gameOverView.getNode().setVisible(false);
            refreshMenuHighScore.run();
            menuView.getNode().setVisible(true);
            attachSecretCode(scene);
            controller.toMenu();
        });

        // 初始状态：只显示主菜单
        gameOverView.getNode().setVisible(false);
        pauseView.getNode().setVisible(false);

        stage.setTitle("飞机大战");
        stage.setScene(scene);
        stage.setMinWidth(LOGICAL_W * 0.4);
        stage.setMinHeight(LOGICAL_H * 0.4);
        stage.centerOnScreen();
        stage.show();
    }

    /** 挂上暗号监听（只在主菜单期间挂着）。已经在挂时不重复添加。 */
    private void attachSecretCode(Scene scene) {
        if (secretCodeHandler == null) {
            return;
        }
        scene.removeEventHandler(KeyEvent.KEY_PRESSED, secretCodeHandler);   // 去重，避免重挂多次
        scene.addEventHandler(KeyEvent.KEY_PRESSED, secretCodeHandler);
        codeBuffer.clear();
        menuShown = true;
    }

    /** 摘掉暗号监听并清空缓冲：一局进行中不该再响应暗号（s 同时还是下移键）。 */
    private void detachSecretCode(Scene scene) {
        menuShown = false;
        if (secretCodeHandler != null) {
            scene.removeEventHandler(KeyEvent.KEY_PRESSED, secretCodeHandler);
        }
        codeBuffer.clear();
    }

    /**
     * 把一次按键喂进暗号缓冲，命中则返回 true（由调用方去解锁隐藏档）。
     *
     * <p>只认字母键：数字键、方向键、空格等一律忽略，也不会清空缓冲——玩家的正常操作
     * 不该把已经敲了一半的暗号作废。缓冲区长度封顶为暗号长度，超出的旧键自动滚出。</p>
     */
    private boolean handleSecretKey(KeyCode code) {
        if (!code.isLetterKey()) {
            return false;
        }
        codeBuffer.addLast(code);
        while (codeBuffer.size() > TORMENT_CODE.size()) {
            codeBuffer.removeFirst();
        }
        return codeBuffer.size() == TORMENT_CODE.size()
                && List.copyOf(codeBuffer).equals(TORMENT_CODE);
    }

    /**
     * 启动时的缩放系数：按屏幕可用区域的四边比例取较小者。
     * 放得下就用 1.0（不放大——放大只会让贴图发糊），放不下才等比缩小。
     */
    private static double initialScale() {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double fit = Math.min(
                (screen.getWidth() - SCREEN_MARGIN_W) / LOGICAL_W,
                (screen.getHeight() - SCREEN_MARGIN_H) / LOGICAL_H);
        return Math.min(1.0, fit);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
