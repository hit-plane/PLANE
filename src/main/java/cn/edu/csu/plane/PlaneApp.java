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
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * 程序入口：装配 model / view / controller 三层，叠加画布、主菜单、暂停与结算界面，
 * 并做界面状态切换（主菜单 → 进行中 → 暂停 / 结算）。
 *
 * <p>逻辑坐标系固定为配置里的窗口尺寸（模型边界与之绑定），实际显示交给一层缩放容器：
 * 窗口放不下时整体等比缩小并居中，放得下就保持原始尺寸，两边多出的空间留白（letterbox）。
 * 这样小屏也能完整显示，且画面比例与命中判定都不会变形。</p>
 */
public class PlaneApp extends Application {

    /** 逻辑画布尺寸：模型边界、渲染坐标都基于它，缩放只发生在外层容器上。 */
    private static final double LOGICAL_W = GameConfig.WINDOW_WIDTH;
    private static final double LOGICAL_H = GameConfig.WINDOW_HEIGHT;
    /** 估算的窗口装饰（标题栏/边框）占用，避免按屏幕满高摆放时被标题栏顶出屏幕。 */
    private static final double SCREEN_MARGIN_W = 24;
    private static final double SCREEN_MARGIN_H = 64;

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
        refreshMenuHighScore.run();
        menuView.setOnStart(() -> {
            gameView.setPlayerSkin(menuView.getSelectedPlaneSkin());
            gameView.setPlayerBulletSkin(menuView.getSelectedBulletSkin());
            model.setDifficulty(menuView.getSelectedDifficulty());
            menuView.getNode().setVisible(false);
            gameOverView.getNode().setVisible(false);
            pauseView.getNode().setVisible(false);
            controller.start();
        });

        // 结算面板按钮
        gameOverView.setOnRestart(() -> {
            gameOverView.getNode().setVisible(false);
            controller.restart();
        });
        gameOverView.setOnMenu(() -> {
            gameOverView.getNode().setVisible(false);
            refreshMenuHighScore.run();
            menuView.getNode().setVisible(true);
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
