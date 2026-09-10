package cn.edu.csu.plane.util;

/**
 * 全局常量配置：窗口尺寸、玩家血量与射击节奏、四类敌机的血量与分值、
 * 关卡阈值与难度递增幅度、道具掉落与效果数值。
 *
 * <p>取值口径以《需求规格说明书》v1.0 为准：F07（血量 100、伤害 10/20、无敌 1 秒）、
 * F08（分值 100/150/200/150）、F11（每 1000 分升 1 关）、F03+Q1（射击间隔 300ms）、
 * Q6（掉落 20%、火力强化持续 10 秒）。敌机血量为 SRS 未定义项，本类中明确取值以便追溯。</p>
 */
public final class GameConfig {

    // ---------- 战场与帧率 ----------
    public static final int WINDOW_WIDTH = 900;
    public static final int WINDOW_HEIGHT = 700;
    public static final double FPS = 60.0;

    /**
     * 单帧最大步长（秒）。主循环用它截断 deltaTime：
     * 窗口拖拽或 GC 停顿会让相邻两帧间隔突然变大，若不截断，
     * 高速自爆机（430 px/s）单帧位移可能超过其碰撞盒最短边（50 px）而出现穿透（NF-03）。
     */
    public static final double MAX_FRAME_DELTA = 1.0 / 30.0;

    // ---------- 玩家（F07 / F03 / Q6） ----------
    /** 玩家初始血量（F07/Q2）。 */
    public static final int DEFAULT_HEALTH = 100;
    /** 自动射击间隔（秒）：300ms（F03 + Q1）。 */
    public static final double PLAYER_FIRE_INTERVAL = 0.3;
    /** 火力强化（双发）的持续秒数（Q6）。 */
    public static final double FIREPOWER_DURATION = 10.0;
    /** 火力等级上限：2 级即双发，再叠加无额外效果。 */
    public static final int MAX_FIRE_POWER = 2;
    /** 受击后的无敌时长（秒）（F07/Q2）。 */
    public static final double PLAYER_INVINCIBLE_TIME = 1.0;

    // ---------- 伤害（F07 / Q2） ----------
    /** 被敌弹命中的扣血量。 */
    public static final int BULLET_DAMAGE = 10;
    /** 与普通敌机碰撞的扣血量。 */
    public static final int COLLISION_DAMAGE = 20;
    /** 与自爆敌机碰撞的扣血量：自爆机定位为高额伤害（SRS 3.2 敌机系统）。 */
    public static final int BOMBER_COLLISION_DAMAGE = 40;

    // ---------- 敌机血量与分值（F08；血量为 SRS 未定义项，取值附实现） ----------
    public static final int NORMAL_ENEMY_HEALTH = 2;
    public static final int NORMAL_ENEMY_SCORE = 100;
    public static final int MOVING_ENEMY_HEALTH = 1;
    public static final int MOVING_ENEMY_SCORE = 150;
    public static final int SHOOTING_ENEMY_HEALTH = 3;
    public static final int SHOOTING_ENEMY_SCORE = 200;
    public static final int BOMBER_ENEMY_HEALTH = 1;
    public static final int BOMBER_ENEMY_SCORE = 150;

    // ---------- 敌机尺寸与速度 ----------
    public static final double NORMAL_ENEMY_SIZE = 40;
    /** 横移/射击/自爆三种机型的统一尺寸。 */
    public static final double ENEMY_SIZE = 50;
    public static final double NORMAL_ENEMY_SPEED = 150;
    public static final double MOVING_ENEMY_SPEED = 110;
    /** 横移敌机的横向摆动速度。 */
    public static final double MOVING_ENEMY_SWAY_SPEED = 130;
    public static final double SHOOTING_ENEMY_SPEED = 90;
    public static final double BOMBER_ENEMY_SPEED = 430;

    // ---------- 敌机射击（F05） ----------
    public static final double SHOOTING_ENEMY_FIRE_INTERVAL = 2.0;
    public static final double ENEMY_BULLET_SPEED = 260;

    // ---------- 关卡与难度递增（F11） ----------
    /** 每升 1 关所需的累计得分（F11/Q3：每 1000 分升 1 关）。 */
    public static final int SCORE_PER_LEVEL = 1000;
    /** 敌机生成间隔的起始值（秒）。 */
    public static final double SPAWN_INTERVAL_BASE = 2.0;
    /** 每升 1 关缩短的生成间隔。 */
    public static final double SPAWN_INTERVAL_STEP = 0.15;
    /** 生成间隔下限，防止高关卡瞬间刷满屏。 */
    public static final double SPAWN_INTERVAL_MIN = 0.6;
    /** 普通敌机的固定权重，决定高级机型的出现占比。 */
    public static final double NORMAL_TYPE_WEIGHT = 3.0;
    /** 高级机型权重上限（每升 1 关给三种高级机各加 1 份），封顶后难度不再上升。 */
    public static final double MAX_ADVANCED_WEIGHT = 4.0;

    // ---------- 道具（F10 / Q6） ----------
    /** 击毁敌机掉落道具的概率。 */
    public static final double ITEM_DROP_RATE = 0.2;
    /** 回血道具的回复量。 */
    public static final int HEAL_AMOUNT = 30;
    /** 道具尺寸。 */
    public static final double ITEM_SIZE = 30;
    /** 道具下落速度（像素/秒）。 */
    public static final double ITEM_FALL_SPEED = 110;
    /** 道具触底后的闪烁时长（秒）。 */
    public static final double ITEM_FLASH_DURATION = 3.0;

    private GameConfig() {
    }
}
