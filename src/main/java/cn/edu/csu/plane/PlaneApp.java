package cn.edu.csu.plane;

import cn.edu.csu.plane.controller.GameController;
import cn.edu.csu.plane.controller.InputHandler;
import cn.edu.csu.plane.model.GameModel;
import cn.edu.csu.plane.model.GameModelImpl;
import cn.edu.csu.plane.util.GameConfig;
import cn.edu.csu.plane.view.GameView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * 程序入口：装配 model / view / controller 三层，并把键盘事件接到主循环。
 */
public class PlaneApp extends Application {

    @Override
    public void start(Stage stage) {
        StackPane root = new StackPane();

        Scene scene = new Scene(root, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);

        GameModel model = new GameModelImpl();
        GameView view = new GameView();
        GameController controller = new GameController(model, view);

        InputHandler input = new InputHandler();
        input.attach(scene);
        // 暂停键（P）与失焦自动暂停/恢复（F12）
        // 必须用 addEventHandler 追加：setOnKeyPressed 是"单值"处理器，
        // 会把 InputHandler.attach 装的移动键监听整个顶掉，导致 WASD/方向键失效。
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

        // TODO: 主菜单界面接入后改为由"开始游戏"触发 controller.start()
        controller.start();

        stage.setTitle("Plane");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
