package cn.edu.csu.plane.util;

/**
 * 游戏难度：简单 / 普通 / 困难三档（F16）。
 *
 * <p>三档只在四个维度上有差别，且一律表现为"在普通档算出的基础值上乘一个倍率"：
 * 敌机血量、每关血量成长率、道具掉落概率、敌机生成间隔。基础配置项
 * （{@code enemy.normal.health}、{@link GameConfig#ITEM_DROP_RATE} 等）本身不改，
 * 因此{@code NORMAL} 档四个倍率全是 1.0，三档曲线化简后与旧版逐位相同——
 * 当前手感原样保留，简单与困难都只是它的偏移。</p>
 *
 * <p>倍率值同样从 {@code config.properties} 读（NF-04），改完重启即生效。
 * 这里读配置用的是 {@link GameConfig#getDouble}：枚举静态初始化时若 {@code GameConfig}
 * 还没初始化，会先把那个类初始化完再取值；而 {@code GameConfig} 的静态块从不引用本枚举，
 * 两个类之间不存在初始化环。</p>
 *
 * <p>档位名（简单/普通/困难）是界面文案，与其它界面字符串一样直接写在代码里，不进配置文件。</p>
 */
public enum Difficulty {

    /** 简单：敌机更脆、血量涨得更慢、道具掉得更多、敌机也更稀疏。 */
    EASY("简单",
            GameConfig.getDouble("difficulty.easy.enemy.health.mult", 0.8),
            GameConfig.getDouble("difficulty.easy.enemy.health.growth.mult", 0.7),
            GameConfig.getDouble("difficulty.easy.item.drop.rate.mult", 1.4),
            GameConfig.getDouble("difficulty.easy.spawn.interval.mult", 1.2)),

    /** 普通：倍率全为 1.0，即当前版本的数值，一点没动。 */
    NORMAL("普通",
            GameConfig.getDouble("difficulty.normal.enemy.health.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.enemy.health.growth.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.item.drop.rate.mult", 1.0),
            GameConfig.getDouble("difficulty.normal.spawn.interval.mult", 1.0)),

    /** 困难：敌机更厚、血量涨得更陡、道具掉得更少、敌机也更密。 */
    HARD("困难",
            GameConfig.getDouble("difficulty.hard.enemy.health.mult", 1.25),
            GameConfig.getDouble("difficulty.hard.enemy.health.growth.mult", 1.5),
            GameConfig.getDouble("difficulty.hard.item.drop.rate.mult", 0.6),
            GameConfig.getDouble("difficulty.hard.spawn.interval.mult", 0.8));

    private final String label;
    private final double enemyHealthMultiplier;
    private final double healthGrowthMultiplier;
    private final double itemDropRateMultiplier;
    private final double spawnIntervalMultiplier;

    Difficulty(String label, double enemyHealthMultiplier, double healthGrowthMultiplier,
               double itemDropRateMultiplier, double spawnIntervalMultiplier) {
        this.label = label;
        this.enemyHealthMultiplier = enemyHealthMultiplier;
        this.healthGrowthMultiplier = healthGrowthMultiplier;
        this.itemDropRateMultiplier = itemDropRateMultiplier;
        this.spawnIntervalMultiplier = spawnIntervalMultiplier;
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
}
