package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.ClearTime;
import cn.edu.csu.plane.model.GameStatus;
import cn.edu.csu.plane.util.Difficulty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 结算界面：展示通关/结束文案、最终得分、本局用时与所在难度，
 * 通关并刷新了本档最短用时时加一行"新纪录！"，提供"重新开始"与"返回主菜单"。
 * 标出难度是为了让成绩有参照——同一分数（或同一次用时）在困难档的分量完全不同。
 */
public class GameOverView {

    private final StackPane root = new StackPane();
    private final Label resultLabel = new Label();
    private final Label scoreLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label difficultyLabel = new Label();
    private final Label recordLabel = new Label();
    private final Button restartButton = new Button("重新开始");
    private final Button menuButton = new Button("返回主菜单");

    public GameOverView() {
        root.setStyle("-fx-background-color: rgba(0,0,0,0.72);");
        SoundPlayer.attachMenuClickSound(root);

        resultLabel.setFont(Font.font("SansSerif", 40));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setFont(Font.font("SansSerif", 22));
        timeLabel.setTextFill(Color.WHITE);
        timeLabel.setFont(Font.font("SansSerif", 22));
        difficultyLabel.setTextFill(Color.LIGHTGRAY);
        difficultyLabel.setFont(Font.font("SansSerif", 18));
        recordLabel.setTextFill(Color.GOLD);
        recordLabel.setFont(Font.font("SansSerif", 20));
        restartButton.setFont(Font.font("SansSerif", 18));
        menuButton.setFont(Font.font("SansSerif", 18));

        VBox box = new VBox(20, resultLabel, scoreLabel, timeLabel, difficultyLabel, recordLabel,
                restartButton, menuButton);
        box.setAlignment(Pos.CENTER);
        root.getChildren().add(box);
    }

    public Node getNode() {
        return root;
    }

    /**
     * 按终局状态、难度、本局用时展示对应文案。
     *
     * @param runClearTime 本局用时（{@code mm:ss.mm}，超时则显示"超时"）
     * @param newClearRecord 本局是否刷新了本档最短用时；只在通关时才提示
     */
    public void show(GameStatus status, int score, Difficulty difficulty,
                     ClearTime runClearTime, boolean newClearRecord) {
        boolean victory = status == GameStatus.VICTORY;
        resultLabel.setText(victory ? "通关！" : "游戏结束");
        resultLabel.setTextFill(victory ? Color.GOLD : Color.RED);
        scoreLabel.setText("最终得分：" + score);
        timeLabel.setText("用时：" + runClearTime.format());
        difficultyLabel.setText("难度：" + difficulty.getLabel());

        // 没刷新纪录时整行不占位，免得"难度"与按钮之间空出一块
        boolean showRecord = victory && newClearRecord;
        recordLabel.setText(showRecord ? "新纪录！" : "");
        recordLabel.setVisible(showRecord);
        recordLabel.setManaged(showRecord);
    }

    public void setOnRestart(Runnable r) {
        restartButton.setOnAction(event -> r.run());
    }

    public void setOnMenu(Runnable r) {
        menuButton.setOnAction(event -> r.run());
    }
}
