package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ClearTime} 的单元测试：三态文案与格式、优劣次序、存档编码往返、计时上限判定（F24）。
 */
class ClearTimeTest {

    @Test
    void notClearedState() {
        ClearTime record = ClearTime.notCleared();
        assertFalse(record.isCleared(), "未通关不该算通关过");
        assertFalse(record.isTimeout());
        assertEquals(0, record.getMillis());
        assertEquals("未通关", record.format());
    }

    @Test
    void timeoutState() {
        ClearTime record = ClearTime.timeout();
        assertTrue(record.isCleared(), "超时通关也算通关过");
        assertTrue(record.isTimeout());
        assertEquals(0, record.getMillis(), "超时没有具体用时");
        assertEquals("超时", record.format());
    }

    @Test
    void formatsMillisecondsAsMinutesSecondsHundredths() {
        assertEquals("00:00.00", ClearTime.format(0));
        assertEquals("00:01.23", ClearTime.format(1_230));
        assertEquals("01:23.45", ClearTime.format(83_450));
        assertEquals("59:59.99", ClearTime.format(3_599_990), "计时上限前一瞬仍是正常用时");
    }

    /** 到达上限即算超时：这样"超时"不会在界面上显示成一个 60:00 以上的数字。 */
    @Test
    void atOrBeyondTimeoutBecomesTimeoutState() {
        assertTrue(ClearTime.of(ClearTime.TIMEOUT_MILLIS).isTimeout());
        assertTrue(ClearTime.of(ClearTime.TIMEOUT_MILLIS + 5_000).isTimeout());
        assertFalse(ClearTime.of(ClearTime.TIMEOUT_MILLIS - 1).isTimeout());
    }

    @Test
    void isTimedOutFollowsConfiguredCap() {
        assertFalse(ClearTime.isTimedOut(0));
        assertFalse(ClearTime.isTimedOut(GameConfig.MAX_TRACKED_TIME - 1));
        assertTrue(ClearTime.isTimedOut(GameConfig.MAX_TRACKED_TIME));
    }

    /** 优劣次序：未通关最差，超时居中，用时越短越好；相等不算更优（免得白写一次存档）。 */
    @Test
    void betterThanOrdersTheThreeStates() {
        ClearTime none = ClearTime.notCleared();
        ClearTime timeout = ClearTime.timeout();
        ClearTime fast = ClearTime.of(60_000);
        ClearTime slow = ClearTime.of(120_000);

        assertTrue(fast.isBetterThan(none), "通关过的胜过没通关的");
        assertTrue(timeout.isBetterThan(none), "超时通关也胜过没通关的");
        assertFalse(none.isBetterThan(timeout), "未通关不可能是更好的成绩");
        assertFalse(none.isBetterThan(none));

        assertTrue(fast.isBetterThan(timeout), "有具体用时的胜过超时");
        assertFalse(timeout.isBetterThan(fast), "超时盖不掉一条已有的具体用时");
        assertFalse(timeout.isBetterThan(timeout));

        assertTrue(fast.isBetterThan(slow), "用时更短更好");
        assertFalse(slow.isBetterThan(fast));
        assertFalse(fast.isBetterThan(ClearTime.of(60_000)), "成绩相同不必重写");
    }

    @Test
    void storedEncodingRoundTrips() {
        ClearTime[] samples = {ClearTime.notCleared(), ClearTime.timeout(), ClearTime.of(83_450)};
        for (ClearTime sample : samples) {
            assertEquals(sample, ClearTime.fromStored(sample.toStored()),
                    sample + " 存下来再读出来应还原");
        }
    }

    /** 存档缺失/损坏都落到"未通关"：0 与负数都按没成绩处理。 */
    @Test
    void corruptedStoredValuesFallBackToNotCleared() {
        assertEquals(ClearTime.notCleared(), ClearTime.fromStored(0));
        assertEquals(ClearTime.notCleared(), ClearTime.fromStored(-7));
        assertEquals(ClearTime.timeout(), ClearTime.fromStored(-1));
        assertEquals(ClearTime.of(1234), ClearTime.fromStored(1234));
    }
}
