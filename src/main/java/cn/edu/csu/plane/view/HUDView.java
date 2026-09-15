package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.ClearTime;
import cn.edu.csu.plane.util.AssetLoader;
import cn.edu.csu.plane.util.GameConfig;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * HUD 视图（F25）：画面顶部叠一条半透明信息带，共两行。
 *
 * <ul>
 *   <li>第一行：左边「第 N 关」、中间经验条（本关得分进度）、右边本局计时；</li>
 *   <li>第二行：左边十颗心（剩余血量比例）、右边火力图标（几级画几个）。</li>
 * </ul>
 *
 * <p>分数与最高分不再显示——分数由经验条表达；难度改由窗口标题呈现（见 {@code PlaneApp}）。
 * 素材为 MC 像素画，故预处理时关掉平滑（{@link AssetLoader#prepareSized}）。</p>
 */
public class HUDView {

    /** 信息带高度：两行内容的容身之处。 */
    private static final double STRIP_HEIGHT = 72;
    private static final double FONT_SIZE = 20;
    /** 第一行文字基线；经验条按 {@link #ROW1_CENTER_Y} 与文字垂直居中对齐。 */
    private static final double ROW1_TEXT_Y = 28;
    private static final double ROW1_CENTER_Y = 21;

    /**
     * 经验条：素材 182×5，按布局尺寸拉伸到 400×14（不保持宽高比——横条拉伸看不出来，
     * 加高反而更醒目）。
     *
     * <p>宽度取 400 是算过的：条居中后左右各余 (600−400)/2 = 100 px，
     * 而右侧计时最长约 77 px（{@code 00:00.00}）、左侧「第 10 关」约 72 px，
     * 各留出 {@link #TEXT_BAR_GAP} 的间距后仍不出画（最坏情况左右各余约 12 px）。</p>
     */
    private static final double XP_BAR_WIDTH = 400;
    private static final double XP_BAR_HEIGHT = 14;

    /**
     * 条与左右文字的间距。<b>两侧文字都贴着条的边缘往外摆</b>（左侧右对齐到"条左边 − 间距"，
     * 右侧左对齐到"条右边 + 间距"），所以无论文字多宽多窄，两侧间距恒等于本值、条也恒居中。
     */
    private static final double TEXT_BAR_GAP = 16;

    /**
     * 第二行的行内边距：心靠左、火力图标靠右，两边各内缩这一段，都别贴着窗口边。
     * 与第一行的「第 N 关」不对齐是刻意的——文字贴边、第二行内缩一段，视觉上更稳。
     */
    private static final double ROW2_MARGIN = 24;

    /** 心：素材 9×9，按整数 3 倍放大到 27（整数倍才不会让像素粗细不均）；十颗横排共占 10×27 + 9×3 = 297 px。 */
    private static final double HEART_SIZE = 27;
    private static final double HEART_GAP = 3;
    private static final int HEART_COUNT = 10;
    /** 第二行（心与火力图标）的顶边。 */
    private static final double ROW2_Y = 38;

    /**
     * 火力图标：每个表示一级火力，火力几级就画几个、右对齐往左排。
     *
     * <p><b>尺寸按"可见长度"定，不是按方框高度定</b>：源图里子弹是 45° 斜跨整个画面的，
     * 贴进 N×N 方框后它的可见长度 ≈ 方框对角线 ≈ 1.41N（实测：方框 27 时内容包围盒 27×27、
     * 对角线 38，而红心的可见尺寸就是 27）。所以要让子弹看起来和红心一样大，方框要收成
     * 27 ÷ 1.41 ≈ 20——按 27 贴会明显比心长一截。</p>
     */
    private static final double FIREPOWER_ICON_SIZE = 24;
    private static final double FIREPOWER_GAP = 4;

    private final GraphicsContext gc;
    private final Image xpBarBackground;
    private final Image xpBarProgress;
    private final Image heartContainer;
    private final Image heartFull;
    private final Image heartHalf;
    private final Image firepowerIcon;

    public HUDView(GraphicsContext gc) {
        this.gc = gc;
        // 像素画素材：最近邻放大，保持硬边（照片类贴图才用双线性）。
        // 经验条与火力图标都按最终尺寸一次性生成，绘制时 1:1 贴出，不做二次重采样。
        xpBarBackground = AssetLoader.prepareSized(
                "hud/experience_bar_background.png", XP_BAR_WIDTH, XP_BAR_HEIGHT, false);
        xpBarProgress = AssetLoader.prepareSized(
                "hud/experience_bar_progress.png", XP_BAR_WIDTH, XP_BAR_HEIGHT, false);
        heartContainer = AssetLoader.prepare("hud/health_container.png", HEART_SIZE, 0, false);
        heartFull = AssetLoader.prepare("hud/health_full.png", HEART_SIZE, 0, false);
        heartHalf = AssetLoader.prepare("hud/health_half.png", HEART_SIZE, 0, false);
        firepowerIcon = AssetLoader.prepareSized(
                "hud/firepower.png", FIREPOWER_ICON_SIZE, FIREPOWER_ICON_SIZE, false);
    }

    /**
     * 每帧重画一次 HUD（覆盖在画布最上层）。
     *
     * @param level            当前关卡，显示为「第 N 关」
     * @param levelProgress    本关进度 0..1，经验条按它填充
     * @param health           当前血量
     * @param regularMaxHealth 本档<b>不计作弊</b>的血量上限，作为十颗心的分母
     * @param firePower        当前火力等级，决定右侧画几个火力图标
     * @param elapsedTime      本局已进行秒数；暂停时由模型冻结，故无需在此特判
     */
    public void draw(int level, double levelProgress, int health, int regularMaxHealth, int firePower,
                     double elapsedTime) {
        double w = GameConfig.WINDOW_WIDTH;

        // 道具文字用的是居中/居中对齐，这里显式改回左对齐+基线对齐，
        // 否则 HUD 的坐标会被当成文字中心，整行跑位。
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);

        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillRect(0, 0, w, STRIP_HEIGHT);

        // 经验条居中，两侧文字各贴条的边缘往外摆，间距因此恒等（见 TEXT_BAR_GAP）
        double barX = (w - XP_BAR_WIDTH) / 2;
        drawXpBar(barX, ROW1_CENTER_Y - XP_BAR_HEIGHT / 2, levelProgress);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("SansSerif", FONT_SIZE));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("第 " + level + " 关", barX - TEXT_BAR_GAP, ROW1_TEXT_Y);
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText(formatElapsed(elapsedTime), barX + XP_BAR_WIDTH + TEXT_BAR_GAP, ROW1_TEXT_Y);

        drawHearts(ROW2_MARGIN, ROW2_Y, health, regularMaxHealth);
        drawFirePower(w - ROW2_MARGIN, ROW2_Y, firePower);
    }

    /**
     * 火力图标：火力几级就画几个，<b>右对齐</b>——最右边一个挨着右边距，
     * 每多一级就往左多长一个，所以等级变化时右侧的图标不动。
     *
     * @param heartRowTop 心那一行的顶边；图标比心小，按中心对齐到这一行
     */
    private void drawFirePower(double rightEdge, double heartRowTop, int firePower) {
        if (firepowerIcon == null) {
            return;
        }
        int count = Math.max(0, Math.min(GameConfig.MAX_FIRE_POWER, firePower));
        // 取整，免得半像素落点让图标被重采样糊掉
        double y = heartRowTop + Math.round((HEART_SIZE - FIREPOWER_ICON_SIZE) / 2);
        for (int i = 0; i < count; i++) {
            double x = rightEdge - (i + 1) * FIREPOWER_ICON_SIZE - i * FIREPOWER_GAP;
            gc.drawImage(firepowerIcon, x, y, FIREPOWER_ICON_SIZE, FIREPOWER_ICON_SIZE);
        }
    }

    /** 实时计时文案：未到上限是 {@code mm:ss.mm}，到了就显示"超时"。 */
    private static String formatElapsed(double elapsedTime) {
        return ClearTime.isTimedOut(elapsedTime)
                ? "超时"
                : ClearTime.format(Math.round(elapsedTime * 1000));
    }

    /**
     * 经验条：先铺底图，再把整条进度图裁到"已填充宽度"叠上去。
     * 裁切边是硬切口，正是 MC 里经验条的样子。
     */
    private void drawXpBar(double x, double y, double progress) {
        if (xpBarBackground == null || xpBarProgress == null) {
            return;
        }
        gc.drawImage(xpBarBackground, x, y, XP_BAR_WIDTH, XP_BAR_HEIGHT);

        double filled = XP_BAR_WIDTH * Math.max(0, Math.min(1, progress));
        if (filled <= 0) {
            return;
        }
        gc.save();
        gc.beginPath();
        gc.rect(x, y, filled, XP_BAR_HEIGHT);
        gc.clip();
        gc.drawImage(xpBarProgress, x, y, XP_BAR_WIDTH, XP_BAR_HEIGHT);
        gc.restore();
    }

    /**
     * 十颗心：先逐格铺空心底，再按血量比例覆盖满心 / 半心。
     *
     * <p>比例的分母是 {@code regularMaxHealth}（本档不计作弊的上限），所以作弊档 9999 血
     * 也照常显示十颗满心，不会溢出；不足半颗的零头舍去，与 MC 的半心表现一致。</p>
     */
    private void drawHearts(double x, double y, int health, int regularMaxHealth) {
        if (heartContainer == null || heartFull == null || heartHalf == null) {
            return;
        }
        double ratio = regularMaxHealth > 0 ? (double) health / regularMaxHealth : 0;
        double filledHearts = Math.max(0, Math.min(HEART_COUNT, ratio * HEART_COUNT));

        for (int i = 0; i < HEART_COUNT; i++) {
            double heartX = x + i * (HEART_SIZE + HEART_GAP);
            gc.drawImage(heartContainer, heartX, y, HEART_SIZE, HEART_SIZE);

            double filled = filledHearts - i;
            if (filled >= 1.0) {
                gc.drawImage(heartFull, heartX, y, HEART_SIZE, HEART_SIZE);
            } else if (filled >= 0.5) {
                gc.drawImage(heartHalf, heartX, y, HEART_SIZE, HEART_SIZE);
            }
        }
    }
}
