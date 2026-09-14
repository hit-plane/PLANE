package cn.edu.csu.plane.view;

import cn.edu.csu.plane.model.BombWave;
import cn.edu.csu.plane.model.Bullet;
import cn.edu.csu.plane.model.Enemy;
import cn.edu.csu.plane.model.EnemyType;
import cn.edu.csu.plane.model.GameStatus;
import cn.edu.csu.plane.model.HitEffect;
import cn.edu.csu.plane.model.Item;
import cn.edu.csu.plane.model.ItemType;
import cn.edu.csu.plane.model.Player;
import cn.edu.csu.plane.util.AssetLoader;
import cn.edu.csu.plane.util.Difficulty;
import cn.edu.csu.plane.util.GameConfig;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
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
    /** 关卡过渡动画时长（秒）。 */
    private static final double LEVEL_TRANSITION_DURATION = 1.5;

    private final Canvas canvas;
    private final GraphicsContext gc;
    private final HUDView hud;

    // 敌机/敌弹贴图（固定）
    private final Image enemy1Image;
    private final Image enemy2Image;
    private final Image enemy3Image;
    private final Image enemy4Image;
    private final Image enemyBulletImage;

    // 背景贴图
    private final Image backgroundImage;
    /** 背景缩放后的渲染高度（按窗口宽度等比缩放），用于循环拼接计算。 */
    private final double bgScaledHeight;
    /** 背景纵向滚动偏移量（像素），每帧累加，超过缩放高度后归零实现无缝循环。 */
    private double backgroundOffset;
    // 道具贴图（火力强化、炸弹、回血）
    private final Image firepowerItemImage;
    private final Image bombItemImage;
    private final Image healItemImage;
    private final Image shieldItemImage;

    /** 炸弹冲击波贴图：按配置高度等比缩放后横向平铺。 */
    private final Image waveImage;

    // 玩家皮肤（可变，默认 plane3 / bullet3）
    private Image playerImage;
    private Image playerBulletImage;

    private GameOverHandler gameOverHandler;

    // 关卡过渡动画状态
    private int lastLevel = -1;
    private double levelTransitionTimer = 0;
    private int transitionLevel = 0;

    public GameView() {
        this.canvas = new Canvas(GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        this.gc = canvas.getGraphicsContext2D();
        this.hud = new HUDView(gc);

        enemy1Image = AssetLoader.prepare("enemies/enemy1.png", ENEMY_NORMAL_H, 0);
        enemy2Image = AssetLoader.prepare("enemies/enemy2.png", ENEMY_H, 0);
        enemy3Image = AssetLoader.prepare("enemies/enemy3.png", ENEMY_H, ENEMY3_ROTATION);
        enemy4Image = AssetLoader.prepare("enemies/enemy4.png", ENEMY_H, 0);
        enemyBulletImage = AssetLoader.prepare("BulletOfEnemy/bullet4.png", BULLET_H, BULLET4_ROTATION);

        backgroundImage = AssetLoader.load("background/background.png");
        // 背景图按窗口宽度等比缩放，高度按原始宽高比计算
        if (backgroundImage != null) {
            bgScaledHeight = GameConfig.WINDOW_WIDTH * backgroundImage.getHeight() / backgroundImage.getWidth();
        } else {
            bgScaledHeight = GameConfig.WINDOW_HEIGHT;
        }
        backgroundOffset = 0;
        // 道具贴图（火力强化、炸弹、回血）
        firepowerItemImage = AssetLoader.prepare("prop/bullet plus.png", GameConfig.ITEM_SIZE, 0);
        bombItemImage = AssetLoader.prepare("prop/bomb.jpg", GameConfig.ITEM_SIZE, 0);
        healItemImage = AssetLoader.prepare("prop/heal.png", GameConfig.ITEM_SIZE, 0);
        shieldItemImage = AssetLoader.prepare("prop/shield.png", GameConfig.ITEM_SIZE, 0);
        waveImage = AssetLoader.prepare("effect/explosionWave.png", GameConfig.BOMB_WAVE_HEIGHT, 0);

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

    /** 渲染一帧：背景 → 道具 → 敌机 → 子弹 → 玩家 → 冲击波 → 受击特效 → HUD（HUD 里带当前难度）。 */
    public void render(Player player, List<Enemy> enemies, List<Bullet> bullets, List<Item> items,
                       List<BombWave> waves, List<HitEffect> hitEffects, int score, int level,
                       int highScore, GameStatus status, Difficulty difficulty, double deltaTime) {
        // 检测关卡变化（升级或重开新局），触发过渡动画
        if (lastLevel != -1 && level != lastLevel) {
            transitionLevel = level;
            levelTransitionTimer = LEVEL_TRANSITION_DURATION;
        }
        lastLevel = level;

        updateBackground(deltaTime);
        drawBackground();
        drawItems(items);
        drawEnemies(enemies);
        drawBullets(bullets);
        drawPlayer(player);
        drawWaves(waves);
        drawHitEffects(hitEffects);
        hud.draw(score, level, highScore, player.getHealth(), player.getMaxHealth(), player.getFirePower(),
                difficulty.getLabel());

        // 关卡过渡动画（覆盖在最上层）
        if (levelTransitionTimer > 0) {
            drawLevelTransition();
            levelTransitionTimer -= deltaTime;
            if (levelTransitionTimer < 0) {
                levelTransitionTimer = 0;
            }
        }
    }

    /** 一局结束：把终局状态、本局得分与本局难度抛给装配层，由其弹出结算面板。 */
    public void showGameOver(GameStatus status, long score, Difficulty difficulty) {
        if (gameOverHandler != null) {
            gameOverHandler.onGameOver(status, (int) score, difficulty);
        }
    }

    public void setGameOverHandler(GameOverHandler handler) {
        this.gameOverHandler = handler;
    }

    @FunctionalInterface
    public interface GameOverHandler {
        void onGameOver(GameStatus status, int score, Difficulty difficulty);
    }

    /**
     * 更新背景滚动偏移：按速度和步长累加，超过缩放高度后归零，实现无缝循环。
     * 仅 PLAYING 状态时滚动，暂停/结算时背景静止。
     */
    private void updateBackground(double deltaTime) {
        if (backgroundImage == null) return;
        backgroundOffset += GameConfig.BACKGROUND_SCROLL_SPEED * deltaTime;
        if (backgroundOffset >= bgScaledHeight) {
            backgroundOffset -= bgScaledHeight;
        }
    }

    /**
     * 绘制纵向循环滚动背景：两张缩放图上下拼接，偏移量驱动无缝流动。
     * 背景图原始尺寸 1080×1920，按窗口宽度等比缩放后高度约 1067px，
     * 大于窗口高度 900px，因此任意偏移下两张图即可填满画面。
     */
    private void drawBackground() {
        if (backgroundImage != null) {
            double y = backgroundOffset - bgScaledHeight;
            gc.drawImage(backgroundImage, 0, y, GameConfig.WINDOW_WIDTH, bgScaledHeight);
            gc.drawImage(backgroundImage, 0, y + bgScaledHeight, GameConfig.WINDOW_WIDTH, bgScaledHeight);
        } else {
            gc.setFill(Color.WHITE);
            gc.fillRect(0, 0, GameConfig.WINDOW_WIDTH, GameConfig.WINDOW_HEIGHT);
        }
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

            // 有专属贴图的道具用贴图，其余用彩色圆+汉字
            Image itemImage = itemImageOf(item.getType());
            if (itemImage != null) {
                gc.drawImage(itemImage, x, y, w, h);
            } else {
                gc.setFill(colorOf(item.getType()));
                gc.fillOval(x, y, w, h);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font("SansSerif", ITEM_LABEL_FONT_SIZE));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.setTextBaseline(VPos.CENTER);
                gc.fillText(labelOf(item.getType()), x + w / 2, y + h / 2);
            }
        }
    }

    /**
     * 绘制炸弹冲击波：一条横贯战场的波带，贴图按 {@link GameConfig#BOMB_WAVE_HEIGHT}
     * 等比缩放后沿横向平铺满整宽。贴图本身是"上实下透"的渐变，平铺后随波带一起向上扫。
     */
    private void drawWaves(List<BombWave> waves) {
        if (waveImage == null || waves.isEmpty()) {
            return;
        }
        double tileW = waveImage.getWidth();
        double tileH = waveImage.getHeight();
        for (BombWave wave : waves) {
            double y = wave.getY();
            for (double x = 0; x < GameConfig.WINDOW_WIDTH; x += tileW) {
                gc.drawImage(waveImage, x, y, tileW, tileH);
            }
        }
    }

    /**
     * 绘制受击特效：根据特效类型选择不同的颜色与扩散速度，
     * 以碰撞点为中心画一个随进度扩散并淡出的圆环。
     *
     * <ul>
     *   <li>ENEMY_HIT — 白黄色小闪光，半径 5→20px</li>
     *   <li>ENEMY_EXPLODE — 橙红色爆炸，半径 10→45px</li>
     *   <li>PLAYER_HIT — 红色警示闪烁，半径 8→35px</li>
     * </ul>
     */
    private void drawHitEffects(List<HitEffect> hitEffects) {
        if (hitEffects.isEmpty()) {
            return;
        }
        gc.save();
        for (HitEffect effect : hitEffects) {
            double progress = effect.getProgress();
            double alpha = 1.0 - progress;   // 越往后越透明

            double radius;
            Color color;
            switch (effect.getType()) {
                case ENEMY_HIT -> {
                    radius = 5 + 15 * progress;
                    color = Color.rgb(255, 255, 180, alpha);   // 白黄色
                }
                case ENEMY_EXPLODE -> {
                    radius = 10 + 35 * progress;
                    color = Color.rgb(255, (int) (140 - 80 * progress), 0, alpha);  // 橙→红
                }
                case PLAYER_HIT -> {
                    radius = 8 + 27 * progress;
                    color = Color.rgb(255, 60, 60, alpha);     // 红色
                }
                default -> { continue; }
            }

            // 外圈扩散环
            gc.setStroke(color);
            gc.setLineWidth(2.5 * (1.0 - progress));
            gc.strokeOval(
                    effect.getX() - radius, effect.getY() - radius,
                    radius * 2, radius * 2);

            // 内圈填充（更淡）
            gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), alpha * 0.3));
            gc.fillOval(
                    effect.getX() - radius, effect.getY() - radius,
                    radius * 2, radius * 2);
        }
        gc.restore();
    }

    /** 把贴图按中心对齐绘制到给定坐标。 */
    private void drawCentered(Image img, double cx, double cy) {
        if (img == null) {
            return;
        }
        gc.drawImage(img, cx - img.getWidth() / 2, cy - img.getHeight() / 2);
    }

    /** 关卡过渡动画：屏幕中央淡入淡出显示"第 X 关"。 */
    private void drawLevelTransition() {
        double elapsed = LEVEL_TRANSITION_DURATION - levelTransitionTimer;
        double fade = 0.35;   // 淡入淡出各 0.35 秒
        double alpha;
        if (elapsed < fade) {
            alpha = elapsed / fade;
        } else if (elapsed > LEVEL_TRANSITION_DURATION - fade) {
            alpha = (LEVEL_TRANSITION_DURATION - elapsed) / fade;
        } else {
            alpha = 1.0;
        }
        alpha = Math.max(0, Math.min(1, alpha));

        double w = GameConfig.WINDOW_WIDTH;
        double h = GameConfig.WINDOW_HEIGHT;

        gc.save();
        gc.setGlobalAlpha(alpha);

        // 半透明背景条
        gc.setFill(Color.rgb(0, 0, 0, 0.55));
        gc.fillRect(0, h / 2 - 60, w, 120);

        // 大字
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("SansSerif", FontWeight.BOLD, 52));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setTextBaseline(VPos.CENTER);
        gc.fillText("第 " + transitionLevel + " 关", w / 2, h / 2);

        gc.restore();
    }

    /** 道具贴图：火力强化、炸弹、回血、护盾各有专属图片。 */
    private Image itemImageOf(ItemType type) {
        return switch (type) {
            case FIREPOWER -> firepowerItemImage;
            case BOMB -> bombItemImage;
            case HEAL -> healItemImage;
            case SHIELD -> shieldItemImage;
        };
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
