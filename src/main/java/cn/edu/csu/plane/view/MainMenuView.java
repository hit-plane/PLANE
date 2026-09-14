package cn.edu.csu.plane.view;

import cn.edu.csu.plane.util.AssetLoader;
import cn.edu.csu.plane.util.Difficulty;
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

import java.util.function.Consumer;

/**
 * 主菜单界面：标题、开始按钮、玩家飞机皮肤与子弹皮肤的图形化选择、难度选择、历史最高分。
 * 皮肤用图片按钮直接展示，选中项以蓝色边框高亮；难度用一行文字按钮，高亮规则相同。
 *
 * <p>开始按钮用贴图 {@code resource/pictures/menu/start.png} 当作按钮内容，
 * 按钮自身的底色与描边全部去掉，看起来就是一张图，点击区域仍是整张图。
 * 贴图缺席时退回原来那个文字按钮，不会出现"点了没反应的空按钮"。</p>
 *
 * <p>预览图与按钮尺寸都乘 {@link GameConfig#ICON_SCALE}，和游戏里的图标放大倍数保持一致，
 * 菜单上看到的机有多大，进游戏就有多大。</p>
 *
 * <p>难度默认选中普通档——与旧版手感一致的那一档。切换难度只是告诉装配层，
 * 由它去刷新最高分显示（最高分按难度分档），菜单自己不碰模型。</p>
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
    /** 难度文字按钮：没有图片撑尺寸，得自己定宽高，并补上文字颜色与内边距。 */
    private static final String DIFFICULTY_BUTTON_BASE =
            "-fx-background-insets: 0; -fx-text-fill: white; -fx-padding: 6 18 6 18; ";
    private static final double DIFFICULTY_BUTTON_WIDTH = 96;
    private static final double DIFFICULTY_BUTTON_HEIGHT = 40;
    private static final double DIFFICULTY_FONT_SIZE = 18;

    /** 难度选项的排列顺序：由易到难。 */
    private static final Difficulty[] DIFFICULTIES =
            {Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};

    private final StackPane root = new StackPane();
    private final Label highScoreLabel = new Label();
    private final Button startButton = new Button();

    private final Button[] planeButtons;
    private final Button[] bulletButtons;
    private final Button[] difficultyButtons = new Button[DIFFICULTIES.length];
    private int selectedPlane;
    private int selectedBullet;
    /** 选中的难度下标，默认落在普通档上（{@link #DIFFICULTIES} 的中间一项）。 */
    private int selectedDifficulty = 1;
    private Consumer<Difficulty> onDifficultyChange;

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

        // 难度按钮行：文字按钮，选中项与皮肤一样用蓝框高亮
        for (int i = 0; i < difficultyButtons.length; i++) {
            Difficulty difficulty = DIFFICULTIES[i];
            Button button = new Button(difficulty.getLabel());
            button.setPrefSize(DIFFICULTY_BUTTON_WIDTH, DIFFICULTY_BUTTON_HEIGHT);
            button.setFont(Font.font("SansSerif", DIFFICULTY_FONT_SIZE));
            button.setStyle(difficultyStyle(false));
            int idx = i;
            button.setOnAction(e -> selectDifficulty(idx));
            difficultyButtons[i] = button;
        }

        Label difficultyLabel = new Label("难度");
        difficultyLabel.setTextFill(Color.WHITE);
        HBox difficultyRow = new HBox(10);
        difficultyRow.getChildren().add(difficultyLabel);
        difficultyRow.getChildren().addAll(difficultyButtons);
        difficultyRow.setAlignment(Pos.CENTER);

        // 难度排在开始按钮正下方：它是开局前最先要定的事，也离窗口下沿最远——
        // 小屏上窗口底部可能被裁掉一截，这一行不该放在最底下（最高分标签本身没有交互，压在最下面无妨）
        VBox box = new VBox(20, title, startButton, difficultyRow, planeRow, bulletRow, highScoreLabel);
        box.setAlignment(Pos.CENTER);
        root.getChildren().add(box);

        // 默认选中 plane3 / bullet3 / 普通难度
        selectPlane(2);
        selectBullet(2);
        selectDifficulty(1);
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

    /**
     * 选中某个难度并高亮，同时通知装配层刷新最高分（最高分按难度分档）。
     * 构造期间 {@code onDifficultyChange} 还是 null，默认选中那一次不会回调。
     */
    private void selectDifficulty(int idx) {
        selectedDifficulty = idx;
        for (int i = 0; i < difficultyButtons.length; i++) {
            difficultyButtons[i].setStyle(difficultyStyle(i == idx));
        }
        if (onDifficultyChange != null) {
            onDifficultyChange.accept(DIFFICULTIES[idx]);
        }
    }

    /** 难度按钮的样式：文字按钮的公共部分 + 选中/未选中两种底色与描边。 */
    private static String difficultyStyle(boolean selected) {
        return (selected ? SELECTED_STYLE : UNSELECTED_STYLE) + DIFFICULTY_BUTTON_BASE;
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

    /** 返回选中的难度，默认普通档。 */
    public Difficulty getSelectedDifficulty() {
        return DIFFICULTIES[selectedDifficulty];
    }

    /** 注册难度切换回调，由装配层绑定到"刷新最高分显示"。 */
    public void setOnDifficultyChange(Consumer<Difficulty> onChange) {
        this.onDifficultyChange = onChange;
    }
}
