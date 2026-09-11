package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.Bullet;
import cn.edu.csu.plane.model.Enemy;
import cn.edu.csu.plane.model.EnemyType;
import cn.edu.csu.plane.model.GameStatus;
import cn.edu.csu.plane.model.Item;
import cn.edu.csu.plane.model.ItemType;
import cn.edu.csu.plane.model.Player;
import cn.edu.csu.plane.util.AssetLoader;
import cn.edu.csu.plane.util.GameConfig;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * 游戏视图：用真实贴图绘制玩家、敌机、子弹与道具，并在顶栏绘制 HUD。
 * 玩家飞机与子弹支持换肤（皮肤由主界面选择后经 {@link #setPlayerSkin} /
 * {@link #setPlayerBulletSkin} 注入）。
 *
 * <p>贴图旋转角度集中在此，作为可调项：不同素材的原始朝向不同，
 * 若画面朝向不对，改对应的角度常量即可。</p>
 *
 * <p>贴图的渲染高度不再各自写死，而是直接取模型侧的碰撞盒尺寸（已经含
 * {@link GameConfig#ICON_SCALE} 图标缩放）。两边同源的好处是：调大图标时
 * 只要动配置里的 {@code icon.scale}，画的图和判定的框一起变大，不会错位。</p>
 */
public class GameView {

    // ---------- 渲染目标高度（像素），宽度按贴图比例自动 ----------
    private static final double PLAYER_H = GameConfig.PLAYER_SIZE;
    private static final double ENEMY_NORMAL_H = GameConfig.NORMAL_ENEMY_SIZE;
    private static final double ENEMY_H = GameConfig.ENEMY_SIZE;
    /** 子弹贴图高度：原始 24 px × 图标缩放，和子弹碰撞盒一起放大。 */
    private static final double BULLET_H = 24 * GameConfig.ICON_SCALE;

    /** 道具圈里的字（火/炸/盾/血）字号：跟着道具一起放大。 */
    private static final double ITEM_LABEL_FONT_SIZE = 14 * GameConfig.ICON_SCALE;

    // ---------- 旋转角度（度，顺时针） ----------
    private static final double ENEMY3_ROTATION = 90;   // 剑刃向下（俯冲机）
    private static final double BULLET4_ROTATION = 90;  // 敌弹竖版

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final HUDView hud;

    // 敌机/敌弹贴图（固定）
    private final Image enemy1Image;
    private final Image enemy2Image;
    private final Image enemy3Image;
    private final Image enemy4Image;
    private final Image enemyBulletImage;

    // 玩家皮肤（可变，默认 plane3 / bullet3）
    private Image playerImage;
    private Image playerBulletImage;

    private GameOverHandler gameOverHandler;

    public GameView() {
        this.canvas = new Canvas(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        this.gc = canvas.getGraphicsContext2D();
        this.hud = new HUDView(gc);

        enemy1Image = AssetLoader.prepare("enemies/enemy1.png", ENEMY_NORMAL_H, 0);
        enemy2Image = AssetLoader.prepare("enemies/enemy2.png", ENEMY_H, 0);
        enemy3Image = AssetLoader.prepare("enemies/enemy3.png", ENEMY_H, ENEMY3_ROTATION);
        enemy4Image = AssetLoader.prepare("enemies/enemy4.png", ENEMY_H, 0);
        enemyBulletImage = AssetLoader.prepare("BulletOfEnemy/bullet4.png", BULLET_H, BULLET4_ROTATION);

        setPlayerSkin("plane3");
        setPlayerBulletSkin("bullet3");
    }

    public Canvas getCanvas() {
        return canvas;
    }

    /** 切换玩家飞机皮肤（plane1~plane4）。 */
    public void setPlayerSkin(String name) {
        playerImage = SkinCatalog.loadPlane(name, PLAYER_H);
    }

    /** 切换玩家子弹皮肤（bullet1~bullet3）。 */
    public void setPlayerBulletSkin(String name) {
        playerBulletImage = SkinCatalog.loadBullet(name, BULLET_H);
    }

    /** 渲染一帧：背景 → 道具 → 敌机 → 子弹 → 玩家 → HUD。 */
    public void render(Player player, List<Enemy> enemies, List<Bullet> bullets, List<Item> items,
                       int score, int level, int highScore, GameStatus status) {
        drawBackground();
        drawItems(items);
        drawEnemies(enemies);
        drawBullets(bullets);
        drawPlayer(player);
        hud.draw(score, level, highScore, player.getHealth(), player.getMaxHealth(), player.getFirePower());
    }

    /** 一局结束：把终局状态与本局得分抛给装配层，由其弹出结算面板。 */
    public void showGameOver(GameStatus status, long score) {
        if (gameOverHandler != null) {
            gameOverHandler.onGameOver(status, (int) score);
        }
    }

    public void setGameOverHandler(GameOverHandler handler) {
        this.gameOverHandler = handler;
    }

    @FunctionalInterface
    public interface GameOverHandler {
        void onGameOver(GameStatus status, int score);
    }

    private void drawBackground() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
    }

    private void drawPlayer(Player player) {
        double cx = player.getX() + player.getWidth() / 2;
        double cy = player.getY() + player.getHeight() / 2;

        if (player.isInvincible()) {
            gc.setGlobalAlpha(0.45);
        }

        drawCentered(playerImage, cx, cy);

        gc.setGlobalAlpha(1.0);

        if (player.isShielded()) {
            gc.setStroke(Color.CYAN);
            gc.setLineWidth(3);
            gc.strokeOval(player.getX() - 4, player.getY() - 4, player.getWidth() + 8, player.getHeight() + 8);
        }
    }

    private void drawEnemies(List<Enemy> enemies) {
        for (Enemy enemy : enemies) {
            double cx = enemy.getX() + enemy.getWidth() / 2;
            double cy = enemy.getY() + enemy.getHeight() / 2;
            drawCentered(enemyImageOf(enemy.getType()), cx, cy);

            // 顶部血条
            double ratio = enemy.getMaxHealth() > 0 ? (double) enemy.getHealth() / enemy.getMaxHealth() : 0;
            gc.setFill(Color.rgb(255, 255, 255, 0.8));
            gc.fillRect(enemy.getX(), enemy.getY() - 6, enemy.getWidth(), 3);
            gc.setFill(Color.RED);
            gc.fillRect(enemy.getX(), enemy.getY() - 6, enemy.getWidth() * ratio, 3);
        }
    }

    private Image enemyImageOf(EnemyType type) {
        return switch (type) {
            case NORMAL -> enemy1Image;
            case MOVING -> enemy2Image;
            case BOMBER -> enemy3Image;
            case SHOOTING -> enemy4Image;
            case BOSS -> enemy4Image;   // Boss 本期不做，占位复用射击机贴图
        };
    }

    private void drawBullets(List<Bullet> bullets) {
        for (Bullet bullet : bullets) {
            Image img = bullet.isPlayerBullet() ? playerBulletImage : enemyBulletImage;
            double cx = bullet.getX() + bullet.getWidth() / 2;
            double cy = bullet.getY() + bullet.getHeight() / 2;
            drawCentered(img, cx, cy);
        }
    }

    private void drawItems(List<Item> items) {
        for (Item item : items) {
            // 着陆后闪烁：每 0.1 秒切换一次可见性
            if (item.isLanded() && ((long) (item.getFlashTimer() * 10)) % 2 != 0) {
                continue;
            }

            double x = item.getX();
            double y = item.getY();
            double w = item.getWidth();
            double h = item.getHeight();

            gc.setFill(colorOf(item.getType()));
            gc.fillOval(x, y, w, h);

            // 字画在圆心：对齐方式按圆心算，省得跟着半径改偏移量
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("SansSerif", ITEM_LABEL_FONT_SIZE));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.setTextBaseline(VPos.CENTER);
            gc.fillText(labelOf(item.getType()), x + w / 2, y + h / 2);
        }
    }

    /** 把贴图按中心对齐绘制到给定坐标。 */
    private void drawCentered(Image img, double cx, double cy) {
        if (img == null) {
            return;
        }
        gc.drawImage(img, cx - img.getWidth() / 2, cy - img.getHeight() / 2);
    }

    private Color colorOf(ItemType type) {
        return switch (type) {
            case FIREPOWER -> Color.ORANGE;
            case BOMB -> Color.DARKGRAY;
            case SHIELD -> Color.CYAN;
            case HEAL -> Color.LIMEGREEN;
        };
    }

    private String labelOf(ItemType type) {
        return switch (type) {
            case FIREPOWER -> "火";
            case BOMB -> "炸";
            case SHIELD -> "盾";
            case HEAL -> "血";
        };
    }
}
