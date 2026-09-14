package cn.edu.csu.plane.view;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 暂停界面：半透明遮罩覆盖在游戏画面上方，显示"暂停"提示，
 * 提供"继续游戏"与"退出游戏"两个按钮。
 */
public class PauseView {

    private final StackPane root = new StackPane();
    private final Button resumeButton = new Button("继续游戏");
    private final Button quitButton = new Button("退出游戏");

    public PauseView() {
        root.setStyle("-fx-background-color: rgba(0,0,0,0.6);");
        root.setVisible(false);

        Label pauseLabel = new Label("暂停");
        pauseLabel.setTextFill(Color.WHITE);
        pauseLabel.setFont(Font.font("SansSerif", 40));

        resumeButton.setFont(Font.font("SansSerif", 20));
        quitButton.setFont(Font.font("SansSerif", 20));

        VBox box = new VBox(20, pauseLabel, resumeButton, quitButton);
        box.setAlignment(Pos.CENTER);
        root.getChildren().add(box);
    }

    public Node getNode() {
        return root;
    }

    public void setOnResume(Runnable r) {
        resumeButton.setOnAction(event -> r.run());
    }

    public void setOnQuit(Runnable r) {
        quitButton.setOnAction(event -> r.run());
    }
}
