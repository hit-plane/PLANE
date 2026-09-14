package cn.edu.csu.plane.util;

import cn.edu.csu.plane.model.EnemyType;

/**
 * 游戏难度：简单 / 普通 / 困难三档，外加隐藏的第四档「折磨」。
 *
 * <p>各档的差别一律表现为"在普通档算出的基础值上乘一个倍率"，分五个维度：
 * 敌机血量、每关血量成长率、道具掉落概率、敌机生成间隔，以及**单机型刷新权重**
 * （逐机型调整，不是所有机型共用一个数）。基础配置项
 * （{@code enemy.normal.health}、{@link GameConfig#ITEM_DROP_RATE} 等）本身不改，
 * 因此 {@code NORMAL} 档的血量/成长/掉落/间隔四个倍率全是 1.0、机型权重也一律 1.0，
 * 加难度这件事没有改动任何既有手感。</p>
 *
 * <p><b>折磨档是隐藏档</b>：不在主菜单的常规三档里显示，要在主菜单输入暗号
 * {@code kskbl} 才会出现（见 {@code PlaneApp} 的暗号监听）。它不是"第四种普通选择"，
 * 而是给开发者/挑战者准备的极端难度：血量与成长成倍抬高、生成间隔大幅压缩、
 * 掉落概率砍半；生成间隔的<b>下限也单独放宽</b>（{@link #getSpawnIntervalFloor()}），
 * 否则会和其它三档一样在后期收敛到同一密度上限，压迫感打折。</p>
 *
 * <p>倍率值同样从 {@code config.properties} 读（NF-04），改完重启即生效。
 * 这里读配置用的是 {@link GameConfig#getDouble}：枚举静态初始化时若 {@code GameConfig}
 * 还没初始化，会先把那个类初始化完再取值；而 {@code GameConfig} 的静态块从不引用本枚举，
 * 两个类之间不存在初始化环。</p>
 *
 * <p>档位名（简单/普通/困难/折磨）是界面文案，与其它界面字符串一样直接写在代码里，不进配置文件。</p>
 */
public enum Difficulty {

    /** 简单：敌机更脆、血量涨得更慢、道具掉得更多、敌机也更稀疏；机型权重与普通档一致。 */
    EASY("简单",
            GameConfig.getDouble("difficulty.easy.enemy.health.mult", 0.8),
            GameConfig.getDouble("difficulty.easy.enemy.health.growth.mult", 0.7),
            GameConfig.getDouble("difficulty.easy.item.drop.rate.mult", 1.4),
            GameConfig.getDouble("difficulty.easy.spawn.interval.mult", 1.2),
            GameConfig.getDouble("difficulty.easy.type.weight.mult", 1.0),
            GameConfig.getDouble("difficulty.easy.spawn.interval.min", 0.175),
            GameConfig.getDouble("difficulty.easy.player.cheat.health.mult", 1.0),
            GameConfig.getDouble("difficulty.easy.player.cheat.bullet.speed.mult", 1.0)),

    /** 普通：倍率全为 1.0，即当前版本的数值，一点没动。 */
    NORMAL("普通",
            GameConfig.getDouble("difficulty.normal.enemy.health.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.enemy.health.growth.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.item.drop.rate.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.spawn.interval.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.type.weight.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.spawn.interval.min", 0.175),
            GameConfig.getDouble("difficulty.normal.player.cheat.health.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.player.cheat.bullet.speed.mult", 1.0)),

    /**
     * 困难：敌机更厚、血量涨得更陡、敌机更密，但补给与"会开火的敌机"都留了余地——
     * 掉落概率只比普通档略低（难关本来就更需要道具），射击机权重单独压到四成，
     * 免得高密度叠上"每 2 秒一发的敌弹"变成躲无可躲的弹幕墙。
     */
    HARD("困难",
            GameConfig.getDouble("difficulty.hard.enemy.health.mult", 1.25),
            GameConfig.getDouble("difficulty.hard.enemy.health.growth.mult", 1.5),
            GameConfig.getDouble("difficulty.hard.item.drop.rate.mult", 0.8),
            GameConfig.getDouble("difficulty.hard.spawn.interval.mult", 0.8),
            GameConfig.getDouble("difficulty.hard.type.weight.mult", 1.0),
            GameConfig.getDouble("difficulty.hard.spawn.interval.min", 0.175),
            GameConfig.getDouble("difficulty.hard.player.cheat.health.mult", 1.0),
            GameConfig.getDouble("difficulty.hard.player.cheat.bullet.speed.mult", 1.0)),

    /**
     * 折磨（隐藏档）：血量 2 倍、每关成长 2.5 倍、生成间隔 0.6 倍、掉落概率 0.5 倍，
     * 且生成间隔下限放宽到 0.12 秒——第 4 关起就压到 0.12～0.3 秒一架，
     * 后期不再和其它档收敛。机型权重不动（折磨档不该"少一种敌人"，该是全都来）。
     */
    TORMENT("折磨",
            GameConfig.getDouble("difficulty.torment.enemy.health.mult", 2.0),
            GameConfig.getDouble("difficulty.torment.enemy.health.growth.mult", 2.5),
            GameConfig.getDouble("difficulty.torment.item.drop.rate.mult", 0.5),
            GameConfig.getDouble("difficulty.torment.spawn.interval.mult", 0.6),
            GameConfig.getDouble("difficulty.torment.type.weight.mult", 1.0),
            GameConfig.getDouble("difficulty.torment.spawn.interval.min", 0.12),
            GameConfig.getDouble("difficulty.torment.player.health.mult", 99.99),
            GameConfig.getDouble("difficulty.torment.player.bullet.speed.mult", 2.0));

    /** 机型权重倍率的配置项前缀：{@code difficulty.<档>.type.weight.mult.<机型>}。 */
    private static final String WEIGHT_KEY_PREFIX = "difficulty.%s.type.weight.mult.";

    private final String label;
    private final double enemyHealthMultiplier;
    private final double healthGrowthMultiplier;
    private final double itemDropRateMultiplier;
    private final double spawnIntervalMultiplier;
    private final double defaultTypeWeightMultiplier;
    private final double spawnIntervalFloor;
    private final double cheatHealthMultiplier;
    private final double cheatBulletSpeedMultiplier;

    Difficulty(String label, double enemyHealthMultiplier, double healthGrowthMultiplier,
               double itemDropRateMultiplier, double spawnIntervalMultiplier,
               double defaultTypeWeightMultiplier, double spawnIntervalFloor,
               double cheatHealthMultiplier, double cheatBulletSpeedMultiplier) {
        this.label = label;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        this.healthGrowthMultiplier = healthGrowthMultiplier;
        this.itemDropRateMultiplier = itemDropRateMultiplier;
        this.spawnIntervalMultiplier = spawnIntervalMultiplier;
        this.defaultTypeWeightMultiplier = defaultTypeWeightMultiplier;
        this.spawnIntervalFloor = spawnIntervalFloor;
        this.cheatHealthMultiplier = cheatHealthMultiplier;
        this.cheatBulletSpeedMultiplier = cheatBulletSpeedMultiplier;
    }

    /** 界面展示用的档位名（简单 / 普通 / 困难 / 折磨）。 */
    public String getLabel() {
        return label;
    }

    /**
     * 本档能否直接在主菜单里选：三档常规难度可以，隐藏的折磨档不行，
     * 必须先输入暗号解锁（见 {@code PlaneApp}）。
     */
    public boolean isSelectableInMenu() {
        return this != TORMENT;
    }

    /** 敌机血量倍率：简单更脆（&lt; 1），困难更厚（&gt; 1）。 */
    public double getEnemyHealthMultiplier() {
        return enemyHealthMultiplier;
    }

    /** 每关血量成长率的倍率：越小，血量随关卡涨得越慢。 */
    public double getHealthGrowthMultiplier() {
        return healthGrowthMultiplier;
    }

    /** 道具掉落概率的倍率：越大掉得越多。 */
    public double getItemDropRateMultiplier() {
        return itemDropRateMultiplier;
    }

    /**
     * 敌机生成间隔的倍率：越大出得越稀疏（简单档），越小出得越密（困难/折磨档）。
     *
     * <p>它只作用于关卡曲线本身，压缩到多密还要看 {@link #getSpawnIntervalFloor()}
     * 那个本档的下限。</p>
     */
    public double getSpawnIntervalMultiplier() {
        return spawnIntervalMultiplier;
    }

    /**
     * 本档的生成间隔下限（秒）：曲线乘完倍率后不低于它。
     *
     * <p>简单/普通/困难三档共用 0.175 秒——那是"防止高关卡一帧糊满屏"的安全阀，
     * 所以这三档到后期会收敛到同一密度上限。折磨档单独放宽到 0.12 秒，
     * 让它后期仍比其它档更密（代价是这一档确实更容易糊屏，属于刻意的）。</p>
     */
    public double getSpawnIntervalFloor() {
        return spawnIntervalFloor;
    }

    /**
     * 作弊开启时本档的血量上限倍率（× {@link GameConfig#DEFAULT_HEALTH}）。
     * 常规三档为 1.0（等于没加），折磨档为 99.99（100 × 99.99 = 9999）。
     */
    public double getCheatHealthMultiplier() {
        return cheatHealthMultiplier;
    }

    /** 作弊开启时本档的子弹速度倍率。常规三档为 1.0，折磨档为 2.0（快一倍）。 */
    public double getCheatBulletSpeedMultiplier() {
        return cheatBulletSpeedMultiplier;
    }

    /**
     * 指定机型在本档下的刷新权重倍率，1.0 即与普通档一致。
     *
     * <p>这里改的是按关卡成长的那部分机型权重（横移 / 射击 / 自爆的权重 =
     * {@code advanced} × 本值），所以它只调"某一种机型出现的多寡"，不动整体生成间隔。
     * 逐机型取值来自配置项 {@code difficulty.<档>.type.weight.mult.<机型>}；
     * 没单独配的机型退回本档的默认倍率 {@code difficulty.<档>.type.weight.mult}
     * （四档默认都是 1.0，即与普通档一致）。</p>
     *
     * <p>普通机没有这一项：它的权重是固定值 {@link GameConfig#NORMAL_TYPE_WEIGHT}，
     * 压低它等价于"整体变密"，那是 {@link #getSpawnIntervalMultiplier()} 的职责。</p>
     *
     * @param type 机型；传 {@code null} 时按 1.0 处理
     */
    public double getTypeWeightMultiplier(EnemyType type) {
        if (type == null) {
            return 1.0;
        }
        String key = String.format(WEIGHT_KEY_PREFIX, name().toLowerCase()) + type.name().toLowerCase();
        return Math.max(0, GameConfig.getDouble(key, defaultTypeWeightMultiplier));
    }
}
