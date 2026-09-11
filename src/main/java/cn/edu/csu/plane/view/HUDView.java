package cn.edu.csu.plane.view;

import cn.edu.csu.plane.util.GameConfig;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * HUD 视图：在画布顶部实时绘制得分、关卡、最高分、火力等级与血量条。
 */
public class HUDView {

    private static final double BAR_HEIGHT = 36;

    private final GraphicsContext gc;

    public HUDView(GraphicsContext gc) {
        this.gc = gc;
    }

    /** 每帧重画一次 HUD（覆盖在画布最上层）。 */
    public void draw(long score, int level, int highScore, int health, int maxHealth, int firePower) {
        double w = GameConfig.WINDOW_WIDTH;

        // 顶栏半透明背景
        gc.setFill(Color.rgb(0, 0, 0, 0.45));
        gc.fillRect(0, 0, w, BAR_HEIGHT);

        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("SansSerif", 16));
        gc.fillText("分数 " + score, 12, 24);
        gc.fillText("关卡 " + level, 130, 24);
        gc.fillText("最高 " + highScore, 230, 24);
        gc.fillText("火力 " + (firePower >= 2 ? "双发" : "单发"), w - 260, 24);

        drawHealthBar(w - 180, 11, 150, 14, health, maxHealth);
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
