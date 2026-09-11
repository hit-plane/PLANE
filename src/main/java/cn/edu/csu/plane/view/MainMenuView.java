package cn.edu.csu.plane.view;

import cn.edu.csu.plane.util.AssetLoader;
import cn.edu.csu.plane.util.GameConfig;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * 主菜单界面：标题、开始按钮、玩家飞机皮肤与子弹皮肤的图形化选择、历史最高分。
 * 皮肤用图片按钮直接展示，选中项以蓝色边框高亮。
 *
 * <p>开始按钮用贴图 {@code resource/pictures/menu/start.png} 当作按钮内容，
 * 按钮自身的底色与描边全部去掉，看起来就是一张图，点击区域仍是整张图。
 * 贴图缺席时退回原来那个文字按钮，不会出现"点了没反应的空按钮"。</p>
 *
 * <p>预览图与按钮尺寸都乘 {@link GameConfig#ICON_SCALE}，和游戏里的图标放大倍数保持一致，
 * 菜单上看到的机有多大，进游戏就有多大。</p>
 */
public class MainMenuView {

    /** 原始预览高度 × 图标缩放：菜单预览也要跟着图标一起放大。 */
    private static final double PLANE_PREVIEW_H = 50 * GameConfig.ICON_SCALE;
    private static final double BULLET_PREVIEW_H = 40 * GameConfig.ICON_SCALE;
    /** 皮肤按钮的边长，放大后仍要留出边框余量。 */
    private static final double SKIN_BUTTON_SIZE = 64 * GameConfig.ICON_SCALE;

    /** 开始按钮贴图的路径（相对 resource/pictures）。 */
    private static final String START_IMAGE = "menu/start.png";
    /** 开始按钮贴图的宽度占窗口宽度的比例，高度按图比例自动（原图 306×81）。 */
    private static final double START_IMAGE_WIDTH_RATIO = 0.5;

    private static final String UNSELECTED_STYLE =
            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-border-width: 2; -fx-cursor: hand;";
    private static final String SELECTED_STYLE =
            "-fx-background-color: rgba(0,191,255,0.25); -fx-border-color: #00bfff; -fx-border-width: 2; -fx-cursor: hand;";
    /** 图片按钮：连内边距都清掉，图片之外不留按钮自带的灰底与边线。 */
    private static final String IMAGE_BUTTON_STYLE =
            "-fx-background-color: transparent; -fx-border-color: transparent; -fx-background-insets: 0; "
            + "-fx-padding: 0; -fx-cursor: hand;";

    private final StackPane root = new StackPane();
    private final Label highScoreLabel = new Label();
    private final Button startButton = new Button();

    private final Button[] planeButtons;
    private final Button[] bulletButtons;
    private int selectedPlane;
    private int selectedBullet;

    public MainMenuView() {
        root.setStyle("-fx-background-color: rgb(12,16,30);");

        Label title = new Label("飞机大战");
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("SansSerif", 48));

        highScoreLabel.setTextFill(Color.LIGHTGRAY);
        highScoreLabel.setFont(Font.font("SansSerif", 18));

        setUpStartButton();

        // 飞机皮肤图片按钮
        planeButtons = new Button[SkinCatalog.PLANE_SKINS.length];
        for (int i = 0; i < planeButtons.length; i++) {
            Image img = SkinCatalog.loadPlane(SkinCatalog.PLANE_SKINS[i], PLANE_PREVIEW_H);
            planeButtons[i] = makeSkinButton(img);
            int idx = i;
            planeButtons[i].setOnAction(e -> selectPlane(idx));
        }

        // 子弹皮肤图片按钮
        bulletButtons = new Button[SkinCatalog.BULLET_SKINS.length];
        for (int i = 0; i < bulletButtons.length; i++) {
            Image img = SkinCatalog.loadBullet(SkinCatalog.BULLET_SKINS[i], BULLET_PREVIEW_H);
            bulletButtons[i] = makeSkinButton(img);
            int idx = i;
            bulletButtons[i].setOnAction(e -> selectBullet(idx));
        }

        Label planeLabel = new Label("玩家飞机");
        planeLabel.setTextFill(Color.WHITE);
        HBox planeRow = new HBox(10);
        planeRow.getChildren().add(planeLabel);
        planeRow.getChildren().addAll(planeButtons);
        planeRow.setAlignment(Pos.CENTER);

        Label bulletLabel = new Label("玩家子弹");
        bulletLabel.setTextFill(Color.WHITE);
        HBox bulletRow = new HBox(10);
        bulletRow.getChildren().add(bulletLabel);
        bulletRow.getChildren().addAll(bulletButtons);
        bulletRow.setAlignment(Pos.CENTER);

        VBox box = new VBox(20, title, startButton, planeRow, bulletRow, highScoreLabel);
        box.setAlignment(Pos.CENTER);
        root.getChildren().add(box);

        // 默认选中 plane3 / bullet3
        selectPlane(2);
        selectBullet(2);
    }

    /**
     * 装开始按钮：有 start.png 就用图，没有就退回文字。
     * 图按窗口宽度的一半等比缩放，换窗口宽度不用再手调尺寸。
     */
    private void setUpStartButton() {
        Image startImage = AssetLoader.load(START_IMAGE);
        if (startImage == null) {
            startButton.setText("开始游戏");
            startButton.setFont(Font.font("SansSerif", 20));
            System.err.println("没找到开始按钮贴图 " + START_IMAGE + "，改用文字按钮");
            return;
        }
        ImageView view = new ImageView(startImage);
        view.setFitWidth(GameConfig.WINDOW_WIDTH * START_IMAGE_WIDTH_RATIO);
        view.setPreserveRatio(true);
        startButton.setGraphic(view);
        startButton.setStyle(IMAGE_BUTTON_STYLE);
    }

    private Button makeSkinButton(Image img) {
        ImageView iv = new ImageView(img);
        Button button = new Button();
        button.setGraphic(iv);
        button.setPrefSize(SKIN_BUTTON_SIZE, SKIN_BUTTON_SIZE);
        button.setStyle(UNSELECTED_STYLE);
        return button;
    }

    private void selectPlane(int idx) {
        selectedPlane = idx;
        for (int i = 0; i < planeButtons.length; i++) {
            planeButtons[i].setStyle(i == idx ? SELECTED_STYLE : UNSELECTED_STYLE);
        }
    }

    private void selectBullet(int idx) {
        selectedBullet = idx;
        for (int i = 0; i < bulletButtons.length; i++) {
            bulletButtons[i].setStyle(i == idx ? SELECTED_STYLE : UNSELECTED_STYLE);
        }
    }

    public Node getNode() {
        return root;
    }

    /** 开始按钮回调，由装配层绑定到 controller.start()。 */
    public void setOnStart(Runnable onStart) {
        startButton.setOnAction(event -> onStart.run());
    }

    /** 刷新历史最高分显示。 */
    public void setHighScore(int score) {
        highScoreLabel.setText("最高分：" + score);
    }

    /** 返回选中的飞机皮肤文件名（plane1~plane4）。 */
    public String getSelectedPlaneSkin() {
        return SkinCatalog.PLANE_SKINS[selectedPlane];
    }

    /** 返回选中的子弹皮肤文件名（bullet1~bullet3）。 */
    public String getSelectedBulletSkin() {
        return SkinCatalog.BULLET_SKINS[selectedBullet];
    }
}
