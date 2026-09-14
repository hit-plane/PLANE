package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;
import cn.edu.csu.plane.util.GameConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 玩家战机：血量、火力、射击节奏、无敌帧与护盾。
 * 位置由控制层的位移指令驱动（不是按速度自动前进），四边夹紧在战场内。
 *
 * <p>火力等级是"本局永久"的成长：吃到火力强化就 +1，本局内不会再随时间回落，
 * 只有开新一局（{@link #reset}）或一局结束（{@link #clearPowerUps}）才回到 1 级。</p>
 *
 * <p>三项起始数值随难度走（见 {@link GameConfig#playerMaxHealthAt}、
 * {@link GameConfig#playerStartFirePowerAt}、{@link GameConfig#playerBulletSpeedAt}）：
 * 常规三档取到的就是旧版数值（单发、100 血、原速），隐藏的折磨档则是
 * 五连发起步、9999 血、子弹快一倍。难度在构造时定下，一局内不变。</p>
 */
public class Player extends Entity {

    /** 机翼两侧留出的最小边距（像素）：多发子弹从这儿开始往机翼中间排，不会贴着图边飞出去。 */
    private static final double WING_MARGIN = 8;

    /** 本档的子弹速度（像素/秒）：基线值再乘本档倍率，见 {@link GameConfig#playerBulletSpeedAt}。 */
    private double bulletSpeedValue;
    /** 本档一局的起始火力等级（折磨档为 5）：{@link #reset} 与一局结束都回到它。 */
    private int startFirePower;
    /** 本档的难度与作弊开关：决定单发伤害要不要乘作弊倍率（见配置 configureFor）。 */
    private Difficulty difficulty;
    private boolean cheatEnabled;

    private int health;
    private int maxHealth;
    private int firePower;            // 当前这一枪打几发（保底 1、最多 MAX_FIRE_POWER）
    private double fireRate;          // 两次开火的间隔（秒）
    private double fireCooldown;      // 距离下次能开火还剩多久
    private double invincibleTimer;
    private boolean shielded;
    /** 当前累计得分，用来判断额外弹道到没到期（由模型每帧喂进来，见 {@link #update}）。 */
    private int score;

    /**
     * 捡道具得来的"额外弹道"各自的<b>到期得分</b>，一条弹道一个阈值。
     *
     * <p>吃到火力强化时记下"当前得分 + {@link GameConfig#BONUS_PATH_DECAY_SCORE}"；之后得分一旦越过
     * 那个阈值，这条弹道就消失。因为得分只增不减，列表天然按到期先后排列，逐条到期、逐条消失，
     * 而不是"到点全部掉回单发"。开局自带的弹道（常规 1 条、作弊 5 条）不进这个列表、永久保留，
     * 所以永远保底 1 条。按得分而不是按时间计，是为了让"3 关"在任何难度下都是字面意义的 3 关——
     * 关卡本来就按累计得分推进。</p>
     */
    private final List<Integer> bonusPathScores = new ArrayList<>();

    /** 按普通档建机：常规玩法与既有测试用的口径，行为与旧版一致。 */
    public Player(double x, double y, double width, double height) {
        this(x, y, width, height, Difficulty.NORMAL, false);
    }

    /** 按指定难度建机（作弊关闭）：血量上限、起始火力与子弹速度都取该档的常规参数。 */
    public Player(double x, double y, double width, double height, Difficulty difficulty) {
        this(x, y, width, height, difficulty, false);
    }

    /**
     * 按指定难度建机，并指定是否开启作弊。
     *
     * @param cheatEnabled true 时改用本档的作弊加成（如折磨档的五连发、9999 血、双倍弹速）
     */
    public Player(double x, double y, double width, double height, Difficulty difficulty,
                  boolean cheatEnabled) {
        super(x, y, width, height);
        this.fireRate = GameConfig.PLAYER_FIRE_INTERVAL;
        configureFor(difficulty, cheatEnabled);
    }

    /**
     * 按难度与作弊开关重设血量上限、起始火力与子弹速度，并立刻回到本档的一局起点状态。
     *
     * <p>难度与作弊开关都是开局前由主菜单定的，而玩家机在模型构造时就建好了，所以换档或
     * 切开关都要把这几项重算一遍；否则会出现"选了折磨档、开了作弊却仍是 100 血、单发"。
     * 子弹速度是每发子弹发射时读取的，重算后下一枪即生效。
     * 对象本身不重建，视图与控制器持有的引用继续有效。</p>
     */
    public void configureFor(Difficulty difficulty, boolean cheatEnabled) {
        this.difficulty = difficulty;
        this.cheatEnabled = cheatEnabled;
        this.maxHealth = GameConfig.playerMaxHealthAt(difficulty, cheatEnabled);
        this.startFirePower = Math.min(GameConfig.playerStartFirePowerAt(difficulty, cheatEnabled),
                GameConfig.MAX_FIRE_POWER);
        this.bulletSpeedValue = GameConfig.playerBulletSpeedAt(difficulty, cheatEnabled);
        applyRoundStartState();
    }

    /** 回到一局的初始状态：满血、起始火力、无盾、无冷却，坐标复位到底部中央（F01）。 */
    public void reset(double x, double y) {
        moveTo(x, y);
        applyRoundStartState();
        this.alive = true;
    }

    /** 一局开始（含重开）时的共同状态：血量回满、火力回到本档起点、清掉盾与所有额外弹道。 */
    private void applyRoundStartState() {
        this.health = maxHealth;
        this.fireCooldown = 0;
        this.invincibleTimer = 0;
        this.shielded = false;
        this.bonusPathScores.clear();
        this.firePower = startFirePower;
    }

    /**
     * 逐帧推进：无敌帧、开火冷却，以及<b>每一条额外弹道各自的到期得分</b>。
     *
     * <p>额外弹道的寿命按得分计（{@link GameConfig#BONUS_PATH_DECAY_SCORE} = 3 个关卡阈值），
     * 得分越过某条的阈值就把那一条摘掉；摘到只剩开局自带的那些（至少 1 条）就停住，
     * 所以打不出"零弹道"。暂停时整帧不推进（{@link GameModelImpl} 在非 PLAYING 态直接 return），
     * 得分也不涨，弹道自然跟着"冻结"。</p>
     *
     * @param currentScore 本局当前累计得分，用来判断哪些额外弹道已经到期
     */
    public void update(double deltaTime, int currentScore) {
        this.score = currentScore;
        if (invincibleTimer > 0) {
            invincibleTimer -= deltaTime;
        }
        if (fireCooldown > 0) {
            fireCooldown -= deltaTime;
        }
        if (!bonusPathScores.isEmpty()) {
            boolean expired = bonusPathScores.removeIf(threshold -> currentScore >= threshold);
            if (expired) {
                refreshFirePower();
            }
        }
    }

    /**
     * {@link Entity} 的帧更新契约：玩家需要知道当前得分才能判断额外弹道到期，
     * 所以正式入口是 {@link #update(double, int)}；这里沿用"上一次已知的得分"，
     * 供不关心得分的调用方（测试、通用实体遍历）使用。
     */
    @Override
    public void update(double deltaTime) {
        update(deltaTime, score);
    }

    /** 当前这一枪该打几发 = 开局自带 + 还没到期的额外弹道，夹在 [1, MAX_FIRE_POWER]。 */
    private void refreshFirePower() {
        firePower = Math.max(1, Math.min(startFirePower + bonusPathScores.size(),
                GameConfig.MAX_FIRE_POWER));
    }

    /** 按输入的位移量移动战机，顺带把坐标卡在窗口里。 */
    public void move(double deltaX, double deltaY) {
        this.x += deltaX;
        this.y += deltaY;

        if (this.x < 0) this.x = 0;
        if (this.y < 0) this.y = 0;
        if (this.x + this.width > GameConfig.WINDOW_WIDTH) {
            this.x = GameConfig.WINDOW_WIDTH - this.width;
        }
        if (this.y + this.height > GameConfig.WINDOW_HEIGHT) {
            this.y = GameConfig.WINDOW_HEIGHT - this.height;
        }
    }

    /** 把战机放到指定坐标并夹紧在战场内（开局与重开时复位用）。 */
    @Override
    public void moveTo(double x, double y) {
        super.moveTo(x, y);
        move(0, 0);   // 复用四边夹紧逻辑
    }

    /** 冷却结束了没，结束了才能开下一枪。 */
    public boolean isReadyToShoot() {
        return fireCooldown <= 0;
    }

    /**
     * 开火，返回这一枪打出去的子弹：火力几级就打几发，最多 {@link GameConfig#MAX_FIRE_POWER} 发。
     *
     * <p>1 级机头正中一发；2 级及以上把 n 发等间距铺在左右机翼之间，所以 2 级恰好是
     * "左右各一发"，等级越高弹幕越宽。</p>
     *
     * <p>单发伤害随等级递减（{@link GameConfig#playerBulletDamage}）：1 级 100%、2 级 75%、
     * 3 级 65%、4/5 级 50%。弹道变多、单发变轻是有意为之——五连发若还按 100% 结算，
     * 总伤害会随等级指数上涨，火力道具一吃就直接无敌了。</p>
     *
     * <p>作弊开启时单发伤害再乘本档的作弊伤害倍率（折磨档 99.99 → 9999），一枪即可击毁任何敌机。</p>
     *
     * <p>外面（GameModelImpl）拿这个返回值往 bullets 里一塞就行，开火后自动进冷却。</p>
     */
    public List<Bullet> shoot() {
        fireCooldown = fireRate;

        int damage = GameConfig.playerBulletDamage(firePower, difficulty, cheatEnabled);
        int shotsToFire = Math.max(1, Math.min(firePower, GameConfig.MAX_FIRE_POWER));
        if (shotsToFire == 1) {
            return List.of(new Bullet(x + width / 2 - Bullet.WIDTH / 2, y, -bulletSpeedValue, damage, true));
        }

        // 从"左机翼 + 边距"等间距排到"右机翼 - 边距"；n = 2 时正好落回左右各一发
        double step = (width - WING_MARGIN * 2 - Bullet.WIDTH) / (shotsToFire - 1);
        List<Bullet> shots = new ArrayList<>(shotsToFire);
        for (int i = 0; i < shotsToFire; i++) {
            shots.add(new Bullet(x + WING_MARGIN + step * i, y, -bulletSpeedValue, damage, true));
        }
        return shots;
    }

    /**
     * 受击结算，优先级链：护盾 → 无敌帧 → 扣血。
     * 有盾则消耗盾，并进入与受击相同的无敌时长（盾破后不会紧接着再吃一发）；
     * 无敌期间伤害被完全忽略且不刷新计时（时长是硬上限，不能靠连续受击延长）。
     */
    public void takeDamage(int damage) {
        if (shielded) {
            shielded = false;
            invincibleTimer = GameConfig.PLAYER_INVINCIBLE_TIME;   // 盾破后同样进入无敌
            return;
        }

        if (invincibleTimer <= 0) {
            health -= damage;
            invincibleTimer = GameConfig.PLAYER_INVINCIBLE_TIME;
            if (health <= 0) {
                health = 0;
                alive = false;
            }
        }
    }

    public void heal(int amount) {
        health = Math.min(health + amount, maxHealth);
    }

    /**
     * 火力强化：多一条<b>带独立到期得分</b>的额外弹道，各条分别在"再过 3 关"之后消失
     * （见 {@link GameConfig#BONUS_PATH_DECAY_SCORE}）。
     *
     * <p>已经打满（开局自带 + 未到期的额外弹道 = {@link GameConfig#MAX_FIRE_POWER}）时不再叠加，
     * 而是把<b>所有仍未到期的额外弹道</b>一起续到"当前得分 + 3 关"——满弹幕下再吃一个道具，
     * 效果就是"整套弹幕续命 3 关"，而不是白白吃掉。注意只动额外弹道，
     * 开局自带的那条与它无关。</p>
     */
    public void enhanceFirePower() {
        int threshold = GameConfig.bonusPathExpiryScore(score);
        if (startFirePower + bonusPathScores.size() < GameConfig.MAX_FIRE_POWER) {
            bonusPathScores.add(threshold);
        } else if (!bonusPathScores.isEmpty()) {
            for (int i = 0; i < bonusPathScores.size(); i++) {
                bonusPathScores.set(i, threshold);
            }
        }
        refreshFirePower();
    }

    public void activateShield() {
        shielded = true;
    }

    /** 清除本局积累的状态（额外弹道、护盾、无敌帧），一局结束时调用。 */
    public void clearPowerUps() {
        bonusPathScores.clear();
        firePower = 1;
        shielded = false;
        invincibleTimer = 0;
    }

    public int getHealth() { return health; }
    public int getMaxHealth() { return maxHealth; }
    public int getFirePower() { return firePower; }
    public double getFireRate() { return fireRate; }
    public boolean isInvincible() { return invincibleTimer > 0; }
    public boolean isShielded() { return shielded; }

    /** 仍未到期的额外弹道条数（供测试与界面观察，不参与判定）。 */
    public int getBonusPathCount() {
        return bonusPathScores.size();
    }
}
