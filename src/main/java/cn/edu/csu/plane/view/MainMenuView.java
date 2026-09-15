package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.ClearTime;
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
 * 主菜单界面：标题、开始按钮、玩家飞机皮肤与子弹皮肤的图形化选择、难度选择、最快通关记录。
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
 * 由它去刷新最快通关记录显示（记录按难度分档），菜单自己不碰模型。</p>
 *
 * <p><b>隐藏档</b>：{@link Difficulty#TORMENT} 的按钮一开始既不显示也不占位
 * （{@code visible=false} + {@code managed=false}），所以常规三档的按钮组是居中在窗口上的；
 * 装配层检测到暗号后调用 {@link #unlockTorment()}，它才恢复占位、按钮组以四档重新居中，
 * 并自动选中它；同时弹出一行提示，让玩家明白现在选中的不是常规难度，
 * 不至于误以为默认难度变了。</p>
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
    /** 作弊开关"已开启"的配色：与难度选中色区分开（红），避免误认成"选中了某个难度"。 */
    private static final String CHEAT_ON_STYLE =
            "-fx-background-color: rgba(255,82,82,0.22); -fx-border-color: #ff5252; -fx-border-width: 2; -fx-cursor: hand;";
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
    /** 作弊开关比难度按钮宽一些：要放得下"作弊：已开启"/"作弊：未开启"两行字。 */
    private static final double CHEAT_BUTTON_WIDTH = 168;

    /**
     * 难度选项的排列顺序：由易到难，隐藏档排最后。
     * 隐藏档的按钮一开始 {@code visible=false}，所以常规状态下看上去仍是简单/普通/困难三档。
     */
    private static final Difficulty[] DIFFICULTIES = Difficulty.values();

    private final StackPane root = new StackPane();
    /** 本档最快通关记录：未通关 / 超时 / mm:ss.mm，由装配层按所选难度刷新（F24）。 */
    private final Label recordLabel = new Label();
    private final Button startButton = new Button();

    private final Button[] planeButtons;
    private final Button[] bulletButtons;
    private final Button[] difficultyButtons = new Button[DIFFICULTIES.length];
    /** 隐藏档（折磨）在 {@link #DIFFICULTIES} 里的下标。 */
    private static final int TORMENT_INDEX = indexOfTorment();
    /** 解锁后才显示的"折磨模式"提示标签。 */
    private final Label tormentHint = new Label("折磨模式已开启");
    /** 解锁后才显示的作弊开关按钮：关=按常规数值建机，开=享受本档作弊加成。 */
    private final Button cheatButton = new Button();
    /** 作弊开关状态：默认关闭，只有玩家自己按下去才生效。 */
    private boolean cheatEnabled;
    /** 作弊开关变化时的回调，由装配层绑定到模型（实时重算玩家机）。 */
    private Consumer<Boolean> onCheatChange;

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

        recordLabel.setTextFill(Color.LIGHTGRAY);
        recordLabel.setFont(Font.font("SansSerif", 18));

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

        // 难度按钮行：文字按钮，选中项与皮肤一样用蓝框高亮；隐藏档先不显示也不占位
        for (int i = 0; i < difficultyButtons.length; i++) {
            Difficulty difficulty = DIFFICULTIES[i];
            Button button = new Button(difficulty.getLabel());
            button.setPrefSize(DIFFICULTY_BUTTON_WIDTH, DIFFICULTY_BUTTON_HEIGHT);
            button.setFont(Font.font("SansSerif", DIFFICULTY_FONT_SIZE));
            button.setStyle(difficultyStyle(false));
            int idx = i;
            button.setOnAction(e -> selectDifficulty(idx));
            if (!difficulty.isSelectableInMenu()) {
                // 隐藏档同时摘掉布局占位（managed），按钮组才会因为"跟着少一个"而居中；
                // 若只 setVisible(false)，那一个空位仍占着宽度，三种可见按钮会整体偏左。
                // 代价是解锁时这一行会重新居中一次（可接受）。
                button.setVisible(false);
                button.setManaged(false);
            }
            difficultyButtons[i] = button;
        }

        // 难度按钮行：不带左侧标题，按钮组自身居中
        HBox difficultyRow = new HBox(10);
        difficultyRow.getChildren().addAll(difficultyButtons);
        difficultyRow.setAlignment(Pos.CENTER);

        tormentHint.setTextFill(Color.web("#ff5252"));
        tormentHint.setFont(Font.font("SansSerif", 16));
        tormentHint.setVisible(false);

        // 作弊开关：默认关闭，解锁隐藏档后才出现。文字直接写状态，避免"按了不知道现在是开还是关"
        cheatButton.setPrefSize(CHEAT_BUTTON_WIDTH, DIFFICULTY_BUTTON_HEIGHT);
        cheatButton.setFont(Font.font("SansSerif", DIFFICULTY_FONT_SIZE));
        cheatButton.setOnAction(e -> setCheatEnabled(!cheatEnabled));
        cheatButton.setVisible(false);
        refreshCheatButton();
        HBox cheatRow = new HBox(10, cheatButton);
        cheatRow.setAlignment(Pos.CENTER);

        // 难度排在开始按钮正下方：它是开局前最先要定的事，也离窗口下沿最远——
        // 小屏上窗口底部可能被裁掉一截，这一行不该放在最底下（通关记录标签本身没有交互，压在最下面无妨）
        VBox box = new VBox(20, title, startButton, difficultyRow, tormentHint, cheatRow,
                planeRow, bulletRow, recordLabel);
        box.setAlignment(Pos.CENTER);
        root.getChildren().add(box);

        // 默认选中 plane3 / bullet3 / 普通难度
        selectPlane(2);
        selectBullet(2);
        selectDifficulty(1);
        // 记录先按"未通关"占位，装配层随后会按当前所选难度刷新
        setClearRecord(ClearTime.notCleared());
    }

    /** 隐藏档在 {@link #DIFFICULTIES} 中的下标；枚举里没有隐藏档时返回 -1。 */
    private static int indexOfTorment() {
        for (int i = 0; i < DIFFICULTIES.length; i++) {
            if (!DIFFICULTIES[i].isSelectableInMenu()) {
                return i;
            }
        }
        return -1;
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
     * 选中某个难度并高亮，同时通知装配层刷新最快通关记录（记录按难度分档）。
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

    /**
     * 解锁隐藏档（折磨）：显示它的按钮与作弊开关、弹出提示，并自动选中它。
     *
     * <p>由装配层在暗号命中后调用。重复调用无副作用（已经解锁时只是再选一次）。</p>
     * <p>注意作弊开关<b>不会</b>被顺手打开：解锁只是"提供了入口"，要不要用由玩家自己按。</p>
     */
    public void unlockTorment() {
        if (TORMENT_INDEX < 0) {
            return;
        }
        difficultyButtons[TORMENT_INDEX].setVisible(true);
        difficultyButtons[TORMENT_INDEX].setManaged(true);   // 恢复占位，四档按钮组重新居中
        tormentHint.setVisible(true);
        cheatButton.setVisible(true);
        selectDifficulty(TORMENT_INDEX);
    }

    /** 作弊开关的当前状态。 */
    public boolean isCheatEnabled() {
        return cheatEnabled;
    }

    /**
     * 切换作弊开关：按钮文字与配色跟着变，并通知装配层（由它把开关写进模型、实时重算玩家机）。
     * 未解锁（按钮还没出现）时也允许调用，只是玩家看不到入口。
     */
    public void setCheatEnabled(boolean enabled) {
        this.cheatEnabled = enabled;
        refreshCheatButton();
        if (onCheatChange != null) {
            onCheatChange.accept(enabled);
        }
    }

    /** 开关按钮的外观：文字直接写明状态，开启时用红色描边，一眼能看出现在是开还是关。 */
    private void refreshCheatButton() {
        cheatButton.setText(cheatEnabled ? "作弊：已开启" : "作弊：未开启");
        cheatButton.setStyle((cheatEnabled ? CHEAT_ON_STYLE : UNSELECTED_STYLE) + DIFFICULTY_BUTTON_BASE);
    }

    /** 注册作弊开关回调，由装配层绑定到模型的 {@code setCheatEnabled}。 */
    public void setOnCheatChange(Consumer<Boolean> onChange) {
        this.onCheatChange = onChange;
    }

    /** 隐藏档是否已解锁（按钮可见）。测试与装配层用它判断暗号是否生效。 */
    public boolean isTormentUnlocked() {
        return TORMENT_INDEX >= 0 && difficultyButtons[TORMENT_INDEX].isVisible();
    }

    public Node getNode() {
        return root;
    }

    /** 开始按钮回调，由装配层绑定到 controller.start()。 */
    public void setOnStart(Runnable onStart) {
        startButton.setOnAction(event -> onStart.run());
    }

    /**
     * 刷新本档最快通关记录显示：未通关 / 超时 / {@code mm:ss.mm}（F24）。
     * 由装配层按当前所选难度取记录后调用。
     */
    public void setClearRecord(ClearTime record) {
        recordLabel.setText("最快通关：" + record.format());
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

    /** 注册难度切换回调，由装配层绑定到"刷新最快通关记录显示"。 */
    public void setOnDifficultyChange(Consumer<Difficulty> onChange) {
        this.onDifficultyChange = onChange;
    }
}
