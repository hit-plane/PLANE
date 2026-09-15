package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.ClearTime;
import cn.edu.csu.plane.util.GameConfig;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * HUD 视图：在画布顶部实时绘制得分、关卡、难度、用时、最高分、火力等级与血量条。
 *
 * <p>竖版战场只有 {@link GameConfig#WINDOW_WIDTH} 宽，一行塞不下七组信息，
 * 因此排成两行：第一行放分数/关卡/难度/用时/最高分，第二行放火力等级与血量条。
 * 横向位置按窗口宽度取比例，改窗口宽度不用再手调坐标。</p>
 *
 * <p>用时（F24）显示为 {@code mm:ss.mm}，达到计时上限后改为"超时"；
 * 暂停时模型整帧不推进，计时自然冻住，恢复后从原处继续。</p>
 */
public class HUDView {

    private static final double BAR_HEIGHT = 60;
    private static final double FONT_SIZE = 16;
    private static final double TEXT_Y = 22;          // 第一行文字基线
    private static final double SECOND_ROW_Y = 48;    // 第二行文字基线
    private static final double HEALTH_BAR_HEIGHT = 14;

    private final GraphicsContext gc;

    public HUDView(GraphicsContext gc) {
        this.gc = gc;
    }

    /**
     * 每帧重画一次 HUD（覆盖在画布最上层）。{@code difficultyLabel} 是当前难度名，
     * {@code elapsedTime} 是本局已进行秒数（F24，暂停时由模型冻结）。
     */
    public void draw(long score, int level, int highScore, int health, int maxHealth, int firePower,
                     String difficultyLabel, double elapsedTime) {
        double w = GameConfig.WINDOW_WIDTH;

        // 道具文字用的是居中/居中对齐，这里显式改回左对齐+基线对齐，
        // 否则 HUD 的坐标会被当成文字中心，整行跑位。
        gc.setTextAlign(TextAlignment.LEFT);
        gc.setTextBaseline(VPos.BASELINE);

        // 顶栏半透明背景
        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillRect(0, 0, w, BAR_HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("SansSerif", FONT_SIZE));
        gc.fillText("分数 " + score, w * 0.02, TEXT_Y);
        gc.fillText("关卡 " + level, w * 0.20, TEXT_Y);
        gc.fillText("难度 " + difficultyLabel, w * 0.36, TEXT_Y);
        gc.fillText("用时 " + formatElapsed(elapsedTime), w * 0.56, TEXT_Y);
        gc.fillText("最高 " + highScore, w * 0.80, TEXT_Y);

        gc.fillText("火力 " + firePower + "/" + GameConfig.MAX_FIRE_POWER, w * 0.02, SECOND_ROW_Y);

        drawHealthBar(w * 0.32, SECOND_ROW_Y - 12, w * 0.62, HEALTH_BAR_HEIGHT, health, maxHealth);
    }

    /** 实时计时文案：未到上限是 {@code mm:ss.mm}，到了就显示"超时"。 */
    private static String formatElapsed(double elapsedTime) {
        return ClearTime.isTimedOut(elapsedTime)
                ? "超时"
                : ClearTime.format(Math.round(elapsedTime * 1000));
    }

    /** 血量条：底色 + 按比例填充的绿色（低血量转红）+ 白色描边。 */
    private void drawHealthBar(double x, double y, double width, double height, int health, int maxHealth) {
        gc.setFill(Color.rgb(60, 60, 60));
        gc.fillRect(x, y, width, height);

        double ratio = maxHealth > 0 ? (double) health / maxHealth : 0;
        gc.setFill(ratio > 0.3 ? Color.LIMEGREEN : Color.RED);
        gc.fillRect(x, y, width * ratio, height);

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRect(x, y, width, height);
    }
}
