package cn.edu.csu.plane.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 全局配置：战场与帧率、玩家血量与射击节奏、四类敌机的血量与分值、
 * 关卡阈值与难度递增幅度、道具掉落与效果数值。
 *
 * <p>数值统一从 classpath 下的 {@code config.properties} 读取，改完配置文件重启即生效，
 * 不用重新编译（NF-04）。文件缺失、读不动、某项写错或写不成数字时，逐项退回下面
 * 括号里的默认值，程序不崩。工程里没有把 {@code MAX_FRAME_DELTA} 放进配置文件——
 * 它是防穿透的工程约束（NF-03），不是可调的平衡数值。</p>
 *
 * <p>取值口径以《需求规格说明书》v1.0 为准：F07（血量 100、伤害 10/20、无敌 1 秒）、
 * F08（分值 100/150/200/150）、F11（每 1000 分升 1 关）、F03+Q1（射击间隔 300ms）、
 * Q6（掉落 20%、火力强化持续 10 秒）。敌机血量为 SRS 未定义项，本类中明确取值以便追溯。</p>
 */
public final class GameConfig {

    private static final String CONFIG_PATH = "/config.properties";

    /** 得先加载，下面那些常量才能从里面取值，所以声明在最前面。 */
    private static final Properties PROPS = load();

    // ---------- 战场与帧率 ----------
    public static final int WINDOW_WIDTH = getInt("window.width", 900);
    public static final int WINDOW_HEIGHT = getInt("window.height", 700);
    public static final double FPS = getDouble("fps", 60.0);

    /**
     * 单帧最大步长（秒）。主循环用它截断 deltaTime：
     * 窗口拖拽或 GC 停顿会让相邻两帧间隔突然变大，若不截断，
     * 高速自爆机（430 px/s）单帧位移可能超过其碰撞盒最短边（50 px）而出现穿透（NF-03）。
     */
    public static final double MAX_FRAME_DELTA = 1.0 / 30.0;

    // ---------- 玩家（F07 / F03 / Q6） ----------
    /** 玩家初始血量（F07/Q2）。 */
    public static final int DEFAULT_HEALTH = getInt("player.health", 100);
    /** 玩家移动速度（像素/秒）。输入层只给 -1/0/1 方向，主循环负责乘速度和步长。 */
    public static final double PLAYER_SPEED = getDouble("player.speed", 360.0);
    /** 自动射击间隔（秒）：300ms（F03 + Q1）。 */
    public static final double PLAYER_FIRE_INTERVAL = getDouble("player.fire.interval", 0.3);
    /** 火力强化（双发）的持续秒数（Q6）。 */
    public static final double FIREPOWER_DURATION = getDouble("player.firepower.duration", 10.0);
    /** 火力等级上限：2 级即双发，再叠加无额外效果。 */
    public static final int MAX_FIRE_POWER = getInt("player.firepower.max", 2);
    /** 受击后的无敌时长（秒）（F07/Q2）。 */
    public static final double PLAYER_INVINCIBLE_TIME = getDouble("player.invincible.time", 1.0);

    // ---------- 伤害（F07 / Q2） ----------
    /** 被敌弹命中的扣血量。 */
    public static final int BULLET_DAMAGE = getInt("damage.bullet", 10);
    /** 与普通敌机碰撞的扣血量。 */
    public static final int COLLISION_DAMAGE = getInt("damage.collision", 20);
    /** 与自爆敌机碰撞的扣血量：自爆机定位为高额伤害（SRS 3.2 敌机系统）。 */
    public static final int BOMBER_COLLISION_DAMAGE = getInt("damage.bomber.collision", 40);

    // ---------- 敌机血量与分值（F08；血量为 SRS 未定义项，取值附实现） ----------
    public static final int NORMAL_ENEMY_HEALTH = getInt("enemy.normal.health", 2);
    public static final int NORMAL_ENEMY_SCORE = getInt("enemy.normal.score", 100);
    public static final int MOVING_ENEMY_HEALTH = getInt("enemy.moving.health", 1);
    public static final int MOVING_ENEMY_SCORE = getInt("enemy.moving.score", 150);
    public static final int SHOOTING_ENEMY_HEALTH = getInt("enemy.shooting.health", 3);
    public static final int SHOOTING_ENEMY_SCORE = getInt("enemy.shooting.score", 200);
    public static final int BOMBER_ENEMY_HEALTH = getInt("enemy.bomber.health", 1);
    public static final int BOMBER_ENEMY_SCORE = getInt("enemy.bomber.score", 150);

    // ---------- 敌机尺寸与速度 ----------
    public static final double NORMAL_ENEMY_SIZE = getDouble("enemy.normal.size", 40);
    /** 横移/射击/自爆三种机型的统一尺寸。 */
    public static final double ENEMY_SIZE = getDouble("enemy.size", 50);
    public static final double NORMAL_ENEMY_SPEED = getDouble("enemy.normal.speed", 150);
    public static final double MOVING_ENEMY_SPEED = getDouble("enemy.moving.speed", 110);
    /** 横移敌机的横向摆动速度。 */
    public static final double MOVING_ENEMY_SWAY_SPEED = getDouble("enemy.moving.sway.speed", 130);
    public static final double SHOOTING_ENEMY_SPEED = getDouble("enemy.shooting.speed", 90);
    public static final double BOMBER_ENEMY_SPEED = getDouble("enemy.bomber.speed", 430);

    // ---------- 敌机射击（F05） ----------
    public static final double SHOOTING_ENEMY_FIRE_INTERVAL = getDouble("enemy.shooting.fire.interval", 2.0);
    public static final double ENEMY_BULLET_SPEED = getDouble("enemy.bullet.speed", 260);

    // ---------- 关卡与难度递增（F11） ----------
    /** 每升 1 关所需的累计得分（F11/Q3：每 1000 分升 1 关）。 */
    public static final int SCORE_PER_LEVEL = getInt("level.score.per.level", 1000);
    /** 敌机生成间隔的起始值（秒）。 */
    public static final double SPAWN_INTERVAL_BASE = getDouble("spawn.interval.base", 2.0);
    /** 每升 1 关缩短的生成间隔。 */
    public static final double SPAWN_INTERVAL_STEP = getDouble("spawn.interval.step", 0.15);
    /** 生成间隔下限，防止高关卡瞬间刷满屏。 */
    public static final double SPAWN_INTERVAL_MIN = getDouble("spawn.interval.min", 0.6);
    /** 普通敌机的固定权重，决定高级机型的出现占比。 */
    public static final double NORMAL_TYPE_WEIGHT = getDouble("spawn.normal.weight", 3.0);
    /** 高级机型权重上限（每升 1 关给三种高级机各加 1 份），封顶后难度不再上升。 */
    public static final double MAX_ADVANCED_WEIGHT = getDouble("spawn.advanced.weight.max", 4.0);

    /** 通关分数（F17）：累计得分达到它就判胜。 */
    public static final int VICTORY_SCORE = getInt("level.victory.score", 5000);

    // ---------- 道具（F10 / Q6） ----------
    /** 击毁敌机掉落道具的概率。 */
    public static final double ITEM_DROP_RATE = getDouble("item.drop.rate", 0.2);
    /** 回血道具的回复量。 */
    public static final int HEAL_AMOUNT = getInt("item.heal.amount", 30);
    /** 道具尺寸。 */
    public static final double ITEM_SIZE = getDouble("item.size", 30);
    /** 道具下落速度（像素/秒）。 */
    public static final double ITEM_FALL_SPEED = getDouble("item.fall.speed", 110);
    /** 道具触底后的闪烁时长（秒）。 */
    public static final double ITEM_FLASH_DURATION = getDouble("item.flash.duration", 3.0);

    private GameConfig() {
    }

    private static Properties load() {
        Properties props = new Properties();
        try (InputStream in = GameConfig.class.getResourceAsStream(CONFIG_PATH)) {
            if (in == null) {
                System.err.println("没找到 " + CONFIG_PATH + "，全部用默认值");
                return props;
            }
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
        } catch (IOException e) {
            System.err.println("读 " + CONFIG_PATH + " 出错，全部用默认值：" + e.getMessage());
        }
        return props;
    }

    private static int getInt(String key, int fallback) {
        String raw = PROPS.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            System.err.println("配置项 " + key + " 的值 '" + raw + "' 不是整数，改用默认值 " + fallback);
            return fallback;
        }
    }

    private static double getDouble(String key, double fallback) {
        String raw = PROPS.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            System.err.println("配置项 " + key + " 的值 '" + raw + "' 不是小数，改用默认值 " + fallback);
            return fallback;
        }
    }
}
