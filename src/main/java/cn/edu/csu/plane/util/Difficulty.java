package cn.edu.csu.plane.util;

import cn.edu.csu.plane.model.EnemyType;

/**
 * 游戏难度：简单 / 普通 / 困难三档（F16）。
 *
 * <p>三档的差别一律表现为"在普通档算出的基础值上乘一个倍率"，分五个维度：
 * 敌机血量、每关血量成长率、道具掉落概率、敌机生成间隔，以及**单机型刷新权重**
 * （逐机型调整，不是所有机型共用一个数）。基础配置项
 * （{@code enemy.normal.health}、{@link GameConfig#ITEM_DROP_RATE} 等）本身不改，
 * 因此 {@code NORMAL} 档的血量/成长/掉落/间隔四个倍率全是 1.0、机型权重也一律 1.0，
 * 三档曲线化简后与旧版逐位相同——当前手感原样保留，简单与困难都只是它的偏移。</p>
 *
 * <p>倍率值同样从 {@code config.properties} 读（NF-04），改完重启即生效。
 * 这里读配置用的是 {@link GameConfig#getDouble}：枚举静态初始化时若 {@code GameConfig}
 * 还没初始化，会先把那个类初始化完再取值；而 {@code GameConfig} 的静态块从不引用本枚举，
 * 两个类之间不存在初始化环。</p>
 *
 * <p>档位名（简单/普通/困难）是界面文案，与其它界面字符串一样直接写在代码里，不进配置文件。</p>
 */
public enum Difficulty {

    /** 简单：敌机更脆、血量涨得更慢、道具掉得更多、敌机也更稀疏；机型权重与普通档一致。 */
    EASY("简单",
            GameConfig.getDouble("difficulty.easy.enemy.health.mult", 0.8),
            GameConfig.getDouble("difficulty.easy.enemy.health.growth.mult", 0.7),
            GameConfig.getDouble("difficulty.easy.item.drop.rate.mult", 1.4),
            GameConfig.getDouble("difficulty.easy.spawn.interval.mult", 1.2),
            GameConfig.getDouble("difficulty.easy.type.weight.mult", 1.0)),

    /** 普通：倍率全为 1.0，即当前版本的数值，一点没动。 */
    NORMAL("普通",
            GameConfig.getDouble("difficulty.normal.enemy.health.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.enemy.health.growth.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.item.drop.rate.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.spawn.interval.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.type.weight.mult", 1.0)),

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
            GameConfig.getDouble("difficulty.hard.type.weight.mult", 1.0));

    /** 机型权重倍率的配置项前缀：{@code difficulty.<档>.type.weight.mult.<机型>}。 */
    private static final String WEIGHT_KEY_PREFIX = "difficulty.%s.type.weight.mult.";

    private final String label;
    private final double enemyHealthMultiplier;
    private final double healthGrowthMultiplier;
    private final double itemDropRateMultiplier;
    private final double spawnIntervalMultiplier;
    private final double defaultTypeWeightMultiplier;

    Difficulty(String label, double enemyHealthMultiplier, double healthGrowthMultiplier,
               double itemDropRateMultiplier, double spawnIntervalMultiplier,
               double defaultTypeWeightMultiplier) {
        this.label = label;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        this.healthGrowthMultiplier = healthGrowthMultiplier;
        this.itemDropRateMultiplier = itemDropRateMultiplier;
        this.spawnIntervalMultiplier = spawnIntervalMultiplier;
        this.defaultTypeWeightMultiplier = defaultTypeWeightMultiplier;
    }

    /** 界面展示用的档位名（简单 / 普通 / 困难）。 */
    public String getLabel() {
        return label;
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
     * 敌机生成间隔的倍率：越大出得越稀疏（简单档），越小出得越密（困难档）。
     *
     * <p>注意这只作用于曲线本身，{@link GameConfig#SPAWN_INTERVAL_MIN} 那个防止高关卡
     * 一帧糊满屏的下限三档共用、不乘倍率，所以到后期三档会收敛到同一密度上限。</p>
     */
    public double getSpawnIntervalMultiplier() {
        return spawnIntervalMultiplier;
    }

    /**
     * 指定机型在本档下的刷新权重倍率，1.0 即与普通档一致。
     *
     * <p>这里改的是按关卡成长的那部分机型权重（横移 / 射击 / 自爆的权重 =
     * {@code advanced} × 本值），所以它只调"某一种机型出现的多寡"，不动整体生成间隔。
     * 逐机型取值来自配置项 {@code difficulty.<档>.type.weight.mult.<机型>}；
     * 没单独配的机型退回本档的默认倍率 {@code difficulty.<档>.type.weight.mult}
     * （三档默认都是 1.0，即与普通档一致）。</p>
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
