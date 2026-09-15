package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

import java.util.Locale;

/**
 * 通关用时记录：三态值类型——未通关 / 超时 / 具体用时（毫秒）。
 *
 * <p>主菜单的"最快通关"与结算界面共用它：展示文案由 {@link #format()} 统一给出，
 * 存档里的编码由 {@link #toStored()}/{@link #fromStored(long)} 统一负责，
 * 比较优劣（哪次成绩更该留下）由 {@link #isBetterThan(ClearTime)} 统一判定。
 * 三处口径各写一份很容易互相打架，所以全部收在这里。</p>
 *
 * <p><b>优劣次序</b>：未通关最差（还没有成绩），超时居中，具体用时越短越好。
 * 所以"超时"不会盖掉一条已有的具体用时，而一条具体用时会盖掉"超时"。</p>
 *
 * <p>计时上限（{@link #TIMEOUT_MILLIS}）来自配置项 {@code time.max.tracked}（默认 1 小时）：
 * 到达上限后不再计时、界面显示"超时"，此时通关也记为"超时"而不是一个数字。</p>
 */
public final class ClearTime {

    /** 计时上限（毫秒）：达到它就不再计时，显示为"超时"。 */
    public static final long TIMEOUT_MILLIS = Math.round(GameConfig.MAX_TRACKED_TIME * 1000);

    /** 存档里的编码：0 = 未通关（也代表文件缺失/损坏），-1 = 超时，正数 = 用时毫秒。 */
    private static final long STORED_NOT_CLEARED = 0;
    private static final long STORED_TIMEOUT = -1;

    private static final ClearTime NOT_CLEARED = new ClearTime(STORED_NOT_CLEARED);
    private static final ClearTime TIMEOUT = new ClearTime(STORED_TIMEOUT);

    /** 内部就用存档编码表示，省得再存一份枚举：0 未通关 / -1 超时 / 正数用时毫秒。 */
    private final long raw;

    private ClearTime(long raw) {
        this.raw = raw;
    }

    /** 未通关：还没有任何一次通关记录。 */
    public static ClearTime notCleared() {
        return NOT_CLEARED;
    }

    /** 超时：通关了，但本局用时已达计时上限。 */
    public static ClearTime timeout() {
        return TIMEOUT;
    }

    /** 按实际用时（毫秒）构造；达到上限时自动按"超时"处理。 */
    public static ClearTime of(long millis) {
        return millis >= TIMEOUT_MILLIS ? TIMEOUT : new ClearTime(Math.max(0, millis));
    }

    /** 把存档里读到的原始值还原成记录。 */
    public static ClearTime fromStored(long raw) {
        if (raw == STORED_TIMEOUT) {
            return TIMEOUT;
        }
        if (raw <= STORED_NOT_CLEARED) {
            return NOT_CLEARED;
        }
        return of(raw);
    }

    /** 写回存档的原始值。 */
    public long toStored() {
        return raw;
    }

    /** 是否通关过（含"超时通关"）。 */
    public boolean isCleared() {
        return raw != STORED_NOT_CLEARED;
    }

    /** 是否为"超时通关"。 */
    public boolean isTimeout() {
        return raw == STORED_TIMEOUT;
    }

    /** 实际用时（毫秒）；未通关或超时时为 0。 */
    public long getMillis() {
        return isCleared() && !isTimeout() ? raw : 0;
    }

    /**
     * 本记录是否优于 {@code other}，即"该不该用本记录覆盖它"。
     * 未通关最差、超时居中、用时越短越好；两者相等时返回 false（不必重写存档）。
     */
    public boolean isBetterThan(ClearTime other) {
        if (this.isCleared() != other.isCleared()) {
            return this.isCleared();          // 通关过的胜过没通关的
        }
        if (!this.isCleared()) {
            return false;                     // 都是未通关
        }
        if (this.isTimeout() != other.isTimeout()) {
            return other.isTimeout();         // 有具体用时的胜过超时
        }
        if (this.isTimeout()) {
            return false;                     // 都是超时
        }
        return this.raw < other.raw;          // 都有用时：更短更好
    }

    /** 把毫秒格式化成 {@code mm:ss.mm}（分钟:秒.百分秒）。 */
    public static String format(long millis) {
        long total = Math.max(0, millis);
        long minutes = total / 60_000;
        long seconds = (total / 1_000) % 60;
        long hundredths = (total / 10) % 100;
        return String.format(Locale.ROOT, "%02d:%02d.%02d", minutes, seconds, hundredths);
    }

    /** 展示文案：未通关 / 超时 / mm:ss.mm。 */
    public String format() {
        if (!isCleared()) {
            return "未通关";
        }
        if (isTimeout()) {
            return "超时";
        }
        return format(raw);
    }

    /** 一局已进行的秒数是否已达计时上限（HUD 据此把实时计时切成"超时"）。 */
    public static boolean isTimedOut(double elapsedSeconds) {
        return elapsedSeconds >= GameConfig.MAX_TRACKED_TIME;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ClearTime time && time.raw == this.raw;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(raw);
    }

    @Override
    public String toString() {
        return format();
    }
}
