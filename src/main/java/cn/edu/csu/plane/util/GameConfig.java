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
 * <p>难度递增不只体现在"敌机更多、高级机型更常见"上，还有三条随关卡走的曲线：
 * 一是敌机血量逐关加厚（{@link #ENEMY_HEALTH_GROWTH_PER_LEVEL}），
 * 二是道具掉落概率逐关走低（{@link #ITEM_DROP_RATE_STEP}）——后期补给更稀罕，
 * 玩家不能靠捡道具一路躺过，三是敌机生成间隔逐关缩短（{@link #SPAWN_INTERVAL_STEP}）。
 * 与之配套，玩家单发子弹伤害随火力等级递减（{@link #playerBulletDamage}），
 * 多一条弹道就轻一分，五连发的总伤害才不会失控。</p>
 *
 * <p>上面三条曲线都按难度（{@link Difficulty}）再乘一层倍率，见
 * {@link #enemyHealthAt(int, int, Difficulty)}、{@link #itemDropRateAt(int, Difficulty)}
 * 与 {@link #spawnIntervalAt(int, Difficulty)}。倍率只做偏移，不动本类里任何一个基础数值：
 * 普通档倍率全为 1.0，取的就是原始口径，与没有难度概念时逐位相同。
 * 不传难度的旧版重载一律按普通档算，行为不变。</p>
 *
 * <p>难度里还有一组<b>直接改玩家自身</b>的参数（不算"偏移"而是"起点"）：
 * {@link #playerStartFirePowerAt(Difficulty)}（开局火力等级）、
 * {@link #playerMaxHealthAt(Difficulty)}（血量上限）、
 * {@link #playerBulletSpeedAt(Difficulty)}（子弹速度）。三档常规难度取到的都是旧版数值
 * （1 级、100 血、原速），只有隐藏的折磨档把它们抬高。</p>
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
    /**
     * 火力等级上限：火力几级就打几发子弹，5 级即五连发，再叠加无额外效果。
     * 火力强化没有时限，本局内永久生效，只有开新一局才回到 1 级。
     */
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
     * 捡道具得来的额外弹道的寿命与判定见 {@link #BONUS_PATH_DECAY_LEVELS}、{@link #BONUS_PATH_DECAY_SCORE}
     * 与 {@link #bonusPathExpiryScore(int)}——它们依赖 {@code SCORE_PER_LEVEL}，所以声明在下方的关卡一节里。
     */

    /**
     * 指定火力等级下单发子弹的伤害：{@link #BULLET_DAMAGE_BASE} × 该级百分比，四舍五入到整数。
     *
     * <p>等级低于 1 按 1 级算，高于百分比表长度（当前 5 级）按最高档算，
     * 因此改大 {@code player.firepower.max} 时不会越界，只是超出部分沿用 5 级的伤害。</p>
     *
     * <p>这是<b>常规口径</b>（作弊关闭）。开着作弊时走
     * {@link #playerBulletDamage(int, Difficulty, boolean)}，会再乘本档的作弊伤害倍率。</p>
     */
    public static int playerBulletDamage(int firePower) {
        int index = Math.min(Math.max(firePower, 1), BULLET_DAMAGE_PERCENT.length) - 1;
        return Math.max(1, (int) Math.round(BULLET_DAMAGE_BASE * BULLET_DAMAGE_PERCENT[index] / 100.0));
    }

    /**
     * 指定火力等级、难度与作弊开关下的单发子弹伤害。
     *
     * <p>先按等级算出常规伤害（见 {@link #playerBulletDamage(int)}），作弊开启时再乘本档的
     * {@code difficulty.<档>.player.cheat.bullet.damage.mult}（缺省 1.0，即等于没加）。
     * 折磨档配 99.99，于是 100 × 99.99 = <b>9999</b>——一枪秒掉任何敌机
     * （第 10 关最厚的射击机约 940 血）。</p>
     *
     * <p>伤害是在乘倍率<b>之后</b>才取整的，所以 9999 这种"整乘整"的目标值不会被
     * 各火力档的百分比先四舍五入吃掉（1 级 100% 档：100 × 99.99 = 9999；
     * 5 级 50% 档：50 × 99.99 = 4999.5 → 5000）。</p>
     */
    public static int playerBulletDamage(int firePower, Difficulty difficulty, boolean cheatEnabled) {
        if (!cheatEnabled) {
            return playerBulletDamage(firePower);
        }
        int index = Math.min(Math.max(firePower, 1), BULLET_DAMAGE_PERCENT.length) - 1;
        double base = BULLET_DAMAGE_BASE * BULLET_DAMAGE_PERCENT[index] / 100.0;
        double mult = getDouble(
                "difficulty." + keyOf(difficulty) + ".player.cheat.bullet.damage.mult", 1.0);
        return Math.max(1, (int) Math.round(base * mult));
    }

    // ---------- 伤害（F07 / Q2） ----------
    /** 被敌弹命中的扣血量。 */
    public static final int BULLET_DAMAGE = getInt("damage.bullet", 10);
    /** 与普通敌机碰撞的扣血量。 */
    public static final int COLLISION_DAMAGE = getInt("damage.collision", 20);
    /** 与自爆敌机碰撞的扣血量：自爆机定位为高额伤害（SRS 3.2 敌机系统）。 */
    public static final int BOMBER_COLLISION_DAMAGE = getInt("damage.bomber.collision", 40);

    /**
     * 指定难度下玩家子弹的飞行速度（像素/秒）。
     *
     * <p>基线值 {@code 窗口高度 / 射击间隔} 保证子弹恰好在一个射击间隔内飞完一屏
     * （时间 × 速度 = 距离），即子弹出屏时刻与下一发子弹生成时刻一致，同屏只有一波子弹。
     * 各档再乘一个倍率（缺省 1.0）：常规口径读
     * {@code difficulty.<档>.player.bullet.speed.mult}，作弊开启时改读
     * {@code difficulty.<档>.player.cheat.bullet.speed.mult}（折磨档配 2.0，子弹快一倍）。</p>
     */
    public static double playerBulletSpeedAt(Difficulty difficulty, boolean cheatEnabled) {
        String suffix = cheatEnabled ? ".player.cheat.bullet.speed.mult" : ".player.bullet.speed.mult";
        double base = WINDOW_HEIGHT / PLAYER_FIRE_INTERVAL;
        return base * getDouble("difficulty." + keyOf(difficulty) + suffix, 1.0);
    }

    /** 常规（未开作弊）口径的子弹速度，等价于 {@code playerBulletSpeedAt(difficulty, false)}。 */
    public static double playerBulletSpeedAt(Difficulty difficulty) {
        return playerBulletSpeedAt(difficulty, false);
    }

    /**
     * 指定难度下玩家一局的起始火力等级（几级就打几发，仍受 {@link #MAX_FIRE_POWER} 封顶）。
     *
     * <p>常规三档都是 1（单发起步）；作弊开启时再叠加
     * {@code difficulty.<档>.player.cheat.start.firepower}（缺省 0，即等于没加），
     * 折磨档配 4，于是 1 + 4 = 5 级，开局就是五连发。超上限的部分由
     * {@link Player} 侧夹到 {@link #MAX_FIRE_POWER}，不会打出超规格弹幕。</p>
     */
    public static int playerStartFirePowerAt(Difficulty difficulty, boolean cheatEnabled) {
        int base = getInt("difficulty." + keyOf(difficulty) + ".player.start.firepower", 1);
        if (!cheatEnabled) {
            return base;
        }
        return base + getInt("difficulty." + keyOf(difficulty) + ".player.cheat.start.firepower", 0);
    }

    /** 常规（未开作弊）口径的起始火力，等价于 {@code playerStartFirePowerAt(difficulty, false)}。 */
    public static int playerStartFirePowerAt(Difficulty difficulty) {
        return playerStartFirePowerAt(difficulty, false);
    }

    /**
     * 指定难度下玩家的血量上限，取值范围钳在 [1, {@link #DEFAULT_HEALTH} × 100]。
     *
     * <p>常规口径读 {@code difficulty.<档>.player.health.mult}（缺省 1.0，三档常规难度即原来的 100）；
     * 作弊开启时改读 {@code difficulty.<档>.player.cheat.health.mult}（缺省 1.0），
     * 折磨档配 99.99，于是 100 × 99.99 = <b>9999</b>（别配 100——100 × 100 = 10000
     * 正好贴在封顶值上，等于没封）。上限护栏是防手滑写天文数字倍率。
     * 开局与重开都会回满到这个上限。</p>
     */
    public static int playerMaxHealthAt(Difficulty difficulty, boolean cheatEnabled) {
        String suffix = cheatEnabled ? ".player.cheat.health.mult" : ".player.health.mult";
        double mult = getDouble("difficulty." + keyOf(difficulty) + suffix, 1.0);
        int capped = (int) Math.min(Math.round(DEFAULT_HEALTH * mult), (double) DEFAULT_HEALTH * 100);
        return Math.max(1, capped);
    }

    /** 常规（未开作弊）口径的血量上限，等价于 {@code playerMaxHealthAt(difficulty, false)}。 */
    public static int playerMaxHealthAt(Difficulty difficulty) {
        return playerMaxHealthAt(difficulty, false);
    }

    /** 难度对应的配置项前缀片段：{@code difficulty.<档>.<后缀>}，档名一律小写。 */
    private static String keyOf(Difficulty difficulty) {
        return difficulty.name().toLowerCase();
    }

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
     * 指定关卡下某机型（按基准血量）的实际血量，按普通档算：
     * {@code 基准 × (1 + (关卡 - 1) × 成长率)}，四舍五入到整数，至少 1 点。
     * 关卡越高血压条越厚，玩家得靠火力强化才跟得上。
     */
    public static int enemyHealthAt(int baseHealth, int level) {
        return enemyHealthAt(baseHealth, level, Difficulty.NORMAL);
    }

    /**
     * 指定关卡、指定难度下某机型的实际血量：
     * {@code 基准 × 血量倍率 × (1 + (关卡 - 1) × 成长率 × 成长倍率)}，四舍五入到整数，至少 1 点。
     *
     * <p>难度从两头调血量：血量倍率抬高/压低整条曲线的起点，成长倍率改变曲线爬升的快慢。
     * 两者都乘在基准值上，普通档（两个倍率都是 1.0）化简后就是老公式。</p>
     */
    public static int enemyHealthAt(int baseHealth, int level, Difficulty difficulty) {
        double growth = ENEMY_HEALTH_GROWTH_PER_LEVEL * difficulty.getHealthGrowthMultiplier();
        double factor = 1 + Math.max(0, level - 1) * growth;
        return Math.max(1, (int) Math.round(baseHealth * difficulty.getEnemyHealthMultiplier() * factor));
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
     * 捡道具得来的<b>额外弹道</b>能活过几个关卡：默认 3 关。
     *
     * <p>按关卡数而不是按秒数计，是因为关卡本来就按累计得分推进（{@link #SCORE_PER_LEVEL}），
     * 换成秒数会随难度/火力变化而"同样的 60 秒在不同档等于不同关数"。按关卡折算后，
     * 各难度下都是字面意义的"3 关后消失"。声明在这里而不是玩家一节，是因为它依赖
     * {@link #SCORE_PER_LEVEL}，放前面会构成非法的前向引用。</p>
     */
    public static final int BONUS_PATH_DECAY_LEVELS = getInt("player.bonus.path.decay.levels", 3);

    /** 额外弹道的寿命折算成得分：{@link #BONUS_PATH_DECAY_LEVELS} × {@link #SCORE_PER_LEVEL}。 */
    public static final int BONUS_PATH_DECAY_SCORE = BONUS_PATH_DECAY_LEVELS * SCORE_PER_LEVEL;

    /**
     * 在累计得分为 {@code currentScore} 时吃到道具，这条额外弹道到哪个得分点消失：
     * {@code currentScore + BONUS_PATH_DECAY_SCORE}。
     */
    public static int bonusPathExpiryScore(int currentScore) {
        return currentScore + BONUS_PATH_DECAY_SCORE;
    }
    /**
     * 敌机生成间隔的起始值（秒）。由 2.0 收到 1.0，再翻倍收到 0.5：
     * 敌机以约 150 px/s 下落、竖版战场纵深 900 px，单机存活约 6 秒，
     * 0.5 秒一架即同屏 10～12 架，是 v1.0 的四倍密度。
     */
    public static final double SPAWN_INTERVAL_BASE = getDouble("spawn.interval.base", 0.5);
    /** 每升 1 关缩短的生成间隔。 */
    public static final double SPAWN_INTERVAL_STEP = getDouble("spawn.interval.step", 0.04);
    /**
     * 生成间隔下限的默认值，防止高关卡瞬间刷满屏。
     * 它是 {@code difficulty.<档>.spawn.interval.min} 缺省时的兜底：简单/普通/困难三档都取它，
     * 折磨档在配置里单独放宽到 0.12 秒。
     */
    public static final double SPAWN_INTERVAL_MIN = getDouble("spawn.interval.min", 0.175);
    /** 普通敌机的固定权重，决定高级机型的出现占比。 */
    public static final double NORMAL_TYPE_WEIGHT = getDouble("spawn.normal.weight", 3.0);
    /** 高级机型权重上限（每升 1 关给三种高级机各加 1 份），封顶后难度不再上升。 */
    public static final double MAX_ADVANCED_WEIGHT = getDouble("spawn.advanced.weight.max", 4.0);

    /**
     * 指定关卡、指定难度下的敌机生成间隔（秒）：{@code (基础间隔 - (关卡 - 1) × 每关缩短量) × 难度倍率}，
     * 不低于 {@link #SPAWN_INTERVAL_MIN}。
     *
     * <p>难度倍率越大出得越稀疏（简单档 1.2，困难档 0.8，折磨档 0.6）。下限不乘倍率、
     * 由各档自己的 {@link Difficulty#getSpawnIntervalFloor()} 决定：简单/普通/困难三档共用
     * 0.175 秒，防止高关卡一帧糊满屏，代价是这三档后期会收敛到同一个密度上限；
     * 折磨档单独放宽到 0.12 秒，后期仍比其它档更密。</p>
     */
    public static double spawnIntervalAt(int level, Difficulty difficulty) {
        double interval = SPAWN_INTERVAL_BASE - Math.max(0, level - 1) * SPAWN_INTERVAL_STEP;
        return Math.max(interval * difficulty.getSpawnIntervalMultiplier(),
                difficulty.getSpawnIntervalFloor());
    }

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
     * 指定关卡下击毁敌机掉落道具的概率，按普通档算：
     * {@code 基础概率 - (关卡 - 1) × 每关递减量}，不低于 {@link #ITEM_DROP_RATE_MIN}。
     * 第 1 关 50%，第 10 关 15%。
     */
    public static double itemDropRateAt(int level) {
        return itemDropRateAt(level, Difficulty.NORMAL);
    }

    /**
     * 指定关卡、指定难度下的掉落概率：先按普通档那条曲线取值（含
     * {@link #ITEM_DROP_RATE_MIN} 下限），再整体乘难度倍率，最后封顶到 1.0。
     *
     * <p>倍率乘在一次算完的曲线值上（而不是只乘基础概率），所以下限也跟着走：
     * 简单档后期仍有 21%，困难档后期还剩 12%——难度差异从中期一直保持到终局，
     * 但困难档始终严格低于普通档（50% → 40%、15% → 12%）。</p>
     */
    public static double itemDropRateAt(int level, Difficulty difficulty) {
        double rate = ITEM_DROP_RATE - Math.max(0, level - 1) * ITEM_DROP_RATE_STEP;
        double floored = Math.max(rate, ITEM_DROP_RATE_MIN) * difficulty.getItemDropRateMultiplier();
        return Math.min(floored, 1.0);
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
    /** 冲击波扫掉敌机时按该比例计分（0.4 = 原分值的 40%）。 */
    public static final double BOMB_WAVE_SCORE_RATE = getDouble("bomb.wave.score.rate", 0.4);
    /** 单颗炸弹（一条冲击波）的得分上限：封顶 1 关的分，防止高密度下炸弹连跳多关。 */
    public static final int BOMB_WAVE_SCORE_CAP = getInt("bomb.wave.score.cap", SCORE_PER_LEVEL);

    // ---------- 计时（F24） ----------
    /**
     * 计时上限（秒）：达到它就不再计时，界面把用时显示为"超时"，此时通关的记录也记为"超时"
     * （而不是一个天文数字）。默认 1 小时。
     */
    public static final double MAX_TRACKED_TIME = getDouble("time.max.tracked", 3600.0);

    // ---------- 背景滚动 ----------
    /** 背景纵向循环滚动速度（像素/秒），模拟飞机向前飞行。 */
    public static final double BACKGROUND_SCROLL_SPEED = getDouble("background.scroll.speed", 40.0);

    // ---------- 音效（F14） ----------
    /**
     * 音效总开关：关掉后一个音都不放，游戏其余部分完全不受影响。
     *
     * <p>这里只读配置文件；单元测试另外还有一个系统属性开关
     * （{@code -Dsound.disabled=true}，见 pom 的 surefire 配置），
     * 两者是"与"的关系——测试期一律静音，不必去改这份给玩家看的配置。</p>
     */
    public static final boolean SOUND_ENABLED = getBoolean("sound.enabled", true);

    /** 全局音量，取值 0.0～1.0；越界不报错，播放时夹紧。 */
    public static final double SOUND_VOLUME = getDouble("sound.volume", 0.7);

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

    /**
     * 读一个开关配置项：只认 true / false（大小写不敏感），读到别的字样就告警并退回默认值。
     * 故意不用 {@code Boolean.parseBoolean}——那样任何拼错的字样（如 "ture"）都会被静默当成 false，
     * 与"某项写错就退回默认值"的容错口径不符。
     */
    private static boolean getBoolean(String key, boolean fallback) {
        String raw = PROPS.getProperty(key);
        if (raw == null) {
            return fallback;
        }
        String value = raw.trim();
        if (value.equalsIgnoreCase("true")) {
            return true;
        }
        if (value.equalsIgnoreCase("false")) {
            return false;
        }
        System.err.println("配置项 " + key + " 的值 '" + raw + "' 不是 true/false，改用默认值 " + fallback);
        return fallback;
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

    /**
     * 读一个小数配置项，读不到或写错就退回默认值。
     * 包内可见是为了让同包的 {@link Difficulty} 也走这条带容错的读取路径，不必各写一份。
     */
    static double getDouble(String key, double fallback) {
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
