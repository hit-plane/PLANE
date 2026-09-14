package cn.edu.csu.plane.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/**
 * 全局配置：战场与帧率、图标缩放、玩家血量与射击节奏、四类敌机的血量与分值、
 * 关卡阈值与难度递增幅度、道具掉落与效果数值。
 *
 * <p>数值统一从 classpath 下的 {@code config.properties} 读取，改完配置文件重启即生效，
 * 不用重新编译（NF-04）。文件缺失、读不动、某项写错或写不成数字时，逐项退回下面
 * 括号里的默认值，程序不崩。工程里没有把 {@code MAX_FRAME_DELTA} 放进配置文件——
 * 它是防穿透的工程约束（NF-03），不是可调的平衡数值。</p>
 *
 * <p>尺寸类数值分两层：{@code player.size / enemy.*.size / item.size} 填的是原始尺寸，
 * 实际尺寸一律再乘 {@link #ICON_SCALE}。这样"图标整体放大多少"只有 {@code icon.scale}
 * 一个旋钮，贴图渲染与碰撞盒两头都从这里取值，不会各改各的。</p>
 *
 * <p>取值口径以《需求规格说明书》v1.0 为准：F07（血量 100、伤害 10/20、无敌 1 秒）、
 * F08（分值 100/150/200/150）、F11（每 1000 分升 1 关）、F03+Q1（射击间隔 300ms）。
 * 相对 v1.0 做过几处刻意调整，都落在配置文件里：战场由横版 900×700 改为竖版 600×900、
 * 图标整体放大 50%、怪密度两轮翻倍（生成间隔 2.0 → 1.0 → 0.5 秒）、
 * 火力上限由双发提到五连发、分数上限由 5000 提到 10000（正好打满关卡封顶的 10 级）。
 * 敌机血量为 SRS 未定义项，本类中明确取值以便追溯。</p>
 *
 * <p>难度递增不只体现在"敌机更多、高级机型更常见"上，还有两条随关卡走的曲线：
 * 一是敌机血量逐关加厚（{@link #ENEMY_HEALTH_GROWTH_PER_LEVEL}），
 * 二是道具掉落概率逐关走低（{@link #ITEM_DROP_RATE_STEP}）——后期补给更稀罕，
 * 玩家不能靠捡道具一路躺过。与之配套，玩家单发子弹伤害随火力等级递减
 * （{@link #playerBulletDamage}），多一条弹道就轻一分，五连发的总伤害才不会失控。</p>
 */
public final class GameConfig {

    private static final String CONFIG_PATH = "/config.properties";

    /** 得先加载，下面那些常量才能从里面取值，所以声明在最前面。 */
    private static final Properties PROPS = load();

    // ---------- 战场与帧率 ----------
    /** 竖版战场宽度（像素），小于高度；飞机大战是上下推进的玩法，视野以纵深为主。 */
    public static final int WINDOW_WIDTH = getInt("window.width", 600);
    public static final int WINDOW_HEIGHT = getInt("window.height", 900);
    public static final double FPS = getDouble("fps", 60.0);

    // ---------- 图标缩放 ----------
    /**
     * 图标（玩家机、敌机、子弹、道具）整体缩放系数：1.0 为原始尺寸，1.5 即增大 50%。
     *
     * <p>贴图渲染高度与实体碰撞盒都乘这个系数，两边同源——看着多大就判定多大，
     * 不会出现"贴图盖住了敌机、子弹却从图边上穿过去"的错位。</p>
     */
    public static final double ICON_SCALE = getDouble("icon.scale", 1.5);

    /**
     * 单帧最大步长（秒）。主循环用它截断 deltaTime：
     * 窗口拖拽或 GC 停顿会让相邻两帧间隔突然变大，若不截断，
     * 高速自爆机（430 px/s）单帧位移可能超过其碰撞盒最短边（放大后普通机 60 px）而出现穿透（NF-03）。
     */
    public static final double MAX_FRAME_DELTA = 1.0 / 30.0;

    // ---------- 玩家（F07 / F03 / Q6） ----------
    /** 玩家机边长：原始尺寸 × 图标缩放。 */
    public static final double PLAYER_SIZE = getDouble("player.size", 50) * ICON_SCALE;
    /** 玩家初始血量（F07/Q2）。 */
    public static final int DEFAULT_HEALTH = getInt("player.health", 100);
    /** 玩家移动速度（像素/秒）。输入层只给 -1/0/1 方向，主循环负责乘速度和步长。 */
    public static final double PLAYER_SPEED = getDouble("player.speed", 360.0);
    /** 自动射击间隔（秒）：300ms（F03 + Q1）。 */
    public static final double PLAYER_FIRE_INTERVAL = getDouble("player.fire.interval", 0.3);
    /** 火力强化（双发）的持续秒数（Q6）。 */
    public static final double FIREPOWER_DURATION = getDouble("player.firepower.duration", 10.0);
    /** 火力等级上限：火力几级就打几发子弹，5 级即五连发，再叠加无额外效果。 */
    public static final int MAX_FIRE_POWER = getInt("player.firepower.max", 5);

    /**
     * 玩家子弹的基准伤害：火力 1 级（100%）那一档的单发伤害。
     *
     * <p>火力等级越高弹道越多，单发伤害按 {@link #BULLET_DAMAGE_PERCENT} 逐级递减，
     * 免得五连发时总伤害失控。基准值取 100，与敌机血量共用同一刻度（100 = 原来 1 点伤害），
     * 这样 100% / 75% / 65% / 50% 各档都能落在整数上，血量和伤害都不会出现小数。</p>
     */
    public static final int BULLET_DAMAGE_BASE = getInt("player.bullet.damage.base", 100);

    /**
     * 各火力等级的子弹伤害百分比，下标 = 火力等级 - 1：100 / 75 / 65 / 50 / 50。
     * 一级 100%（基准），二级 75%，三级 65%，四级起 50%——火力越高弹道越多，单发越轻。
     */
    private static final int[] BULLET_DAMAGE_PERCENT = {
            getInt("player.bullet.damage.percent.lv1", 100),
            getInt("player.bullet.damage.percent.lv2", 75),
            getInt("player.bullet.damage.percent.lv3", 65),
            getInt("player.bullet.damage.percent.lv4", 50),
            getInt("player.bullet.damage.percent.lv5", 50),
    };

    /** 受击后的无敌时长（秒）（F07/Q2）。 */
    public static final double PLAYER_INVINCIBLE_TIME = getDouble("player.invincible.time", 1.0);

    /**
     * 指定火力等级下单发子弹的伤害：{@link #BULLET_DAMAGE_BASE} × 该级百分比，四舍五入到整数。
     *
     * <p>等级低于 1 按 1 级算，高于百分比表长度（当前 5 级）按最高档算，
     * 因此改大 {@code player.firepower.max} 时不会越界，只是超出部分沿用 5 级的伤害。</p>
     */
    public static int playerBulletDamage(int firePower) {
        int index = Math.min(Math.max(firePower, 1), BULLET_DAMAGE_PERCENT.length) - 1;
        return Math.max(1, (int) Math.round(BULLET_DAMAGE_BASE * BULLET_DAMAGE_PERCENT[index] / 100.0));
    }

    // ---------- 伤害（F07 / Q2） ----------
    /** 被敌弹命中的扣血量。 */
    public static final int BULLET_DAMAGE = getInt("damage.bullet", 10);
    /** 与普通敌机碰撞的扣血量。 */
    public static final int COLLISION_DAMAGE = getInt("damage.collision", 20);
    /** 与自爆敌机碰撞的扣血量：自爆机定位为高额伤害（SRS 3.2 敌机系统）。 */
    public static final int BOMBER_COLLISION_DAMAGE = getInt("damage.bomber.collision", 40);

    // ---------- 敌机血量与分值（F08；血量为 SRS 未定义项，取值附实现） ----------
    /**
     * 四种敌机在第 1 关的血量（基准值）。刻度与 {@link #BULLET_DAMAGE_BASE} 一致：
     * 100 相当于原来 1 点伤害，所以第 1 关的击毁枪数与旧版完全相同
     * （普通 2 发、横移 1 发、射击 3 发、自爆 1 发），只是数字换了刻度。
     * 实际血量按关卡再乘成长系数，见 {@link #enemyHealthAt}。
     */
    public static final int NORMAL_ENEMY_HEALTH = getInt("enemy.normal.health", 200);
    public static final int NORMAL_ENEMY_SCORE = getInt("enemy.normal.score", 100);
    public static final int MOVING_ENEMY_HEALTH = getInt("enemy.moving.health", 100);
    public static final int MOVING_ENEMY_SCORE = getInt("enemy.moving.score", 150);
    public static final int SHOOTING_ENEMY_HEALTH = getInt("enemy.shooting.health", 300);
    public static final int SHOOTING_ENEMY_SCORE = getInt("enemy.shooting.score", 200);
    public static final int BOMBER_ENEMY_HEALTH = getInt("enemy.bomber.health", 100);
    public static final int BOMBER_ENEMY_SCORE = getInt("enemy.bomber.score", 150);

    /**
     * 每升 1 关，敌机血量线性增加的百分比：第 1 关是基准值，第 10 关约为基准的 2.35 倍。
     * 难度递增因此是三管齐下——敌机更密、高级机型更多、单机更耐打。
     */
    public static final double ENEMY_HEALTH_GROWTH_PER_LEVEL = getDouble("enemy.health.growth.per.level", 0.15);

    /**
     * 指定关卡下某机型（按基准血量）的实际血量：{@code 基准 × (1 + (关卡 - 1) × 成长率)}，
     * 四舍五入到整数，至少 1 点。关卡越高血压条越厚，玩家得靠火力强化才跟得上。
     */
    public static int enemyHealthAt(int baseHealth, int level) {
        double factor = 1 + Math.max(0, level - 1) * ENEMY_HEALTH_GROWTH_PER_LEVEL;
        return Math.max(1, (int) Math.round(baseHealth * factor));
    }

    // ---------- 敌机尺寸与速度 ----------
    /** 普通敌机边长：原始尺寸 × 图标缩放。 */
    public static final double NORMAL_ENEMY_SIZE = getDouble("enemy.normal.size", 40) * ICON_SCALE;
    /** 横移/射击/自爆三种机型的统一尺寸：原始尺寸 × 图标缩放。 */
    public static final double ENEMY_SIZE = getDouble("enemy.size", 50) * ICON_SCALE;
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
    /**
     * 敌机生成间隔的起始值（秒）。由 2.0 收到 1.0，再翻倍收到 0.5：
     * 敌机以约 150 px/s 下落、竖版战场纵深 900 px，单机存活约 6 秒，
     * 0.5 秒一架即同屏 10～12 架，是 v1.0 的四倍密度。
     */
    public static final double SPAWN_INTERVAL_BASE = getDouble("spawn.interval.base", 0.5);
    /** 每升 1 关缩短的生成间隔。 */
    public static final double SPAWN_INTERVAL_STEP = getDouble("spawn.interval.step", 0.04);
    /** 生成间隔下限，防止高关卡瞬间刷满屏。 */
    public static final double SPAWN_INTERVAL_MIN = getDouble("spawn.interval.min", 0.175);
    /** 普通敌机的固定权重，决定高级机型的出现占比。 */
    public static final double NORMAL_TYPE_WEIGHT = getDouble("spawn.normal.weight", 3.0);
    /** 高级机型权重上限（每升 1 关给三种高级机各加 1 份），封顶后难度不再上升。 */
    public static final double MAX_ADVANCED_WEIGHT = getDouble("spawn.advanced.weight.max", 4.0);

    /**
     * 分数上限（F17）：累计得分达到它就判通关，由 v1.0 的 5000 提高到 10000。
     *
     * <p>10000 正好是"关卡上限 10 级 × {@link #SCORE_PER_LEVEL} 1000 分"，与
     * {@code GameState.MAX_LEVEL} 对齐——即打满 10 关才算赢，最后一关是在难度封顶的
     * 状态下度过的。两个数值要对账，改动其一记得改另一个。</p>
     */
    public static final int VICTORY_SCORE = getInt("level.victory.score", 10000);

    // ---------- 道具（F10 / Q6） ----------
    /** 击毁敌机掉落道具的基础概率：第 1 关为 50%，随后逐关递减（见 {@link #itemDropRateAt}）。 */
    public static final double ITEM_DROP_RATE = getDouble("item.drop.rate", 0.5);
    /**
     * 每升 1 关，掉落概率减少的绝对值：关卡越高敌人越强，补给反而越少，
     * 逼玩家在前几关就把火力攒起来，而不是一路捡道具碾压。
     */
    public static final double ITEM_DROP_RATE_STEP = getDouble("item.drop.rate.step", 0.05);
    /** 掉落概率下限：难度再高也还留一点补给，不至于中后期一件道具都不掉。 */
    public static final double ITEM_DROP_RATE_MIN = getDouble("item.drop.rate.min", 0.15);

    /**
     * 指定关卡下击毁敌机掉落道具的概率：{@code 基础概率 - (关卡 - 1) × 每关递减量}，
     * 不低于 {@link #ITEM_DROP_RATE_MIN}。第 1 关 50%，第 10 关 15%。
     */
    public static double itemDropRateAt(int level) {
        double rate = ITEM_DROP_RATE - Math.max(0, level - 1) * ITEM_DROP_RATE_STEP;
        return Math.max(rate, ITEM_DROP_RATE_MIN);
    }

    /**
     * 掉落的道具中"火力强化（加弹道）"的占比：25%，与炸弹/护盾/回血三选一的均分概率持平。
     * 剩下 {@code 1 - 本值} 的概率由炸弹/护盾/回血三选一均分，因此四种道具各占 25%。
     */
    public static final double ITEM_FIREPOWER_RATE = getDouble("item.firepower.rate", 0.25);
    /** 回血道具的回复量。 */
    public static final int HEAL_AMOUNT = getInt("item.heal.amount", 30);
    /** 道具直径：原始尺寸 × 图标缩放。 */
    public static final double ITEM_SIZE = getDouble("item.size", 30) * ICON_SCALE;
    /** 道具下落速度（像素/秒）。 */
    public static final double ITEM_FALL_SPEED = getDouble("item.fall.speed", 110);
    /** 道具触底后的闪烁时长（秒）。 */
    public static final double ITEM_FLASH_DURATION = getDouble("item.flash.duration", 3.0);

    // ---------- 炸弹冲击波（F10） ----------
    /** 冲击波从底边向上扫的速度（像素/秒）：调这个值改扫地快慢。 */
    public static final double BOMB_WAVE_SPEED = getDouble("bomb.wave.speed", 900);
    /** 冲击波带在屏幕上的高度（像素）；贴图按此高度等比缩放后横向平铺。 */
    public static final double BOMB_WAVE_HEIGHT = getDouble("bomb.wave.height", 128);
    /** 冲击波扫掉敌机时按该比例计分（0.6 = 原分值的 60%）。 */
    public static final double BOMB_WAVE_SCORE_RATE = getDouble("bomb.wave.score.rate", 0.6);

    // ---------- 背景滚动 ----------
    /** 背景纵向循环滚动速度（像素/秒），模拟飞机向前飞行。 */
    public static final double BACKGROUND_SCROLL_SPEED = getDouble("background.scroll.speed", 40.0);

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
