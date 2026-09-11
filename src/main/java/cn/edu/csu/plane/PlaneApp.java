package cn.edu.csu.plane;

import cn.edu.csu.plane.controller.GameController;
import cn.edu.csu.plane.model.GameModel;
import cn.edu.csu.plane.model.GameModelImpl;
import cn.edu.csu.plane.util.GameConfig;
import cn.edu.csu.plane.view.GameOverView;
import cn.edu.csu.plane.view.GameView;
import cn.edu.csu.plane.view.MainMenuView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * 程序入口：装配 model / view / controller 三层，叠加画布、主菜单与结算界面，
 * 并做界面状态切换（主菜单 → 进行中 → 结算）。
 */
public class PlaneApp extends Application {

    @Override
    public void start(Stage stage) {
        GameModel model = new GameModelImpl();
        GameView gameView = new GameView();
        MainMenuView menuView = new MainMenuView();
        GameOverView gameOverView = new GameOverView();
        GameController controller = new GameController(model, gameView);

        // 画布在底层，菜单与结算界面按需覆盖在其上
        StackPane root = new StackPane();
        root.getChildren().addAll(gameView.getCanvas(), menuView.getNode(), gameOverView.getNode());

        Scene scene = new Scene(root, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        // 必须让 controller 绑定它自己持有的输入实例：InputHandler 的按键状态是实例字段。
        controller.attachInput(scene);
        // 暂停键（P）：用 addEventHandler 追加，避免顶掉 InputHandler 装的移动键监听。
        scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == GameController.PAUSE_KEY) {
                controller.togglePause();
            }
        });
        stage.focusedProperty().addListener((observable, wasFocused, isFocused) -> {
            if (isFocused) {
                controller.resume();
            } else {
                controller.pause();
            }
        });

        // 一局结束：弹出结算面板
        gameView.setGameOverHandler((status, score) -> {
            gameOverView.show(status, score);
            gameOverView.getNode().setVisible(true);
        });

        // 主菜单：按所选皮肤开始游戏
        menuView.setHighScore(model.getHighScore());
        menuView.setOnStart(() -> {
            gameView.setPlayerSkin(menuView.getSelectedPlaneSkin());
            gameView.setPlayerBulletSkin(menuView.getSelectedBulletSkin());
            menuView.getNode().setVisible(false);
            gameOverView.getNode().setVisible(false);
            controller.start();
        });

        // 结算面板按钮
        gameOverView.setOnRestart(() -> {
            gameOverView.getNode().setVisible(false);
            controller.restart();
        });
        gameOverView.setOnMenu(() -> {
            gameOverView.getNode().setVisible(false);
            menuView.setHighScore(model.getHighScore());
            menuView.getNode().setVisible(true);
            controller.toMenu();
        });

        // 初始状态：只显示主菜单
        gameOverView.getNode().setVisible(false);

        stage.setTitle("飞机大战");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
