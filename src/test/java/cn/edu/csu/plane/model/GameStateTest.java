package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link GameState} 的单元测试：覆盖初始状态、分数累加与关卡推进规则（F11）。
 *
 * <p>关卡阈值取自 SRS F11「每累计 1000 分升 1 关」。重点覆盖"一次大额加分跨越多级"
 * 这一边界——旧实现用 if 只升 1 级，会让关卡数偏小。</p>
 */
class GameStateTest {

    private GameState state;

    @BeforeEach
    void setUp() {
        state = new GameState();
    }

    @Test
    void initialStateIsMenuWithZeroScoreAndLevelOne() {
        assertEquals(GameStatus.MENU, state.getStatus());
        assertEquals(0, state.getScore());
        assertEquals(1, state.getLevel());
        assertEquals(0, state.getElapsedTime(), 0.001);
    }

    @Test
    void resetScoreClearsScoreLevelAndTime() {
        state.addScore(2500);
        state.advanceTime(12.5);
        state.resetScore();
        assertEquals(0, state.getScore());
        assertEquals(1, state.getLevel());
        assertEquals(0, state.getElapsedTime(), 0.001);
    }

    @Test
    void statusIsWritable() {
        state.setStatus(GameStatus.PLAYING);
        assertEquals(GameStatus.PLAYING, state.getStatus());
    }

    @Test
    void levelStaysOneBelowThreshold() {
        state.addScore(GameConfig.SCORE_PER_LEVEL - 1);
        assertEquals(1, state.getLevel());
    }

    @Test
    void levelAdvancesExactlyAtThreshold() {
        state.addScore(GameConfig.SCORE_PER_LEVEL);
        assertEquals(2, state.getLevel());
    }

    /** 关键回归：一次加分量跨过多个阈值时必须连升多级（GWT-08 的变体）。 */
    @Test
    void oneLargeGainAdvancesMultipleLevels() {
        state.addScore(GameConfig.SCORE_PER_LEVEL * 3);
        assertEquals(4, state.getLevel());
    }

    @Test
    void levelAccumulatesAcrossSeveralGains() {
        state.addScore(GameConfig.SCORE_PER_LEVEL - 1);
        assertEquals(1, state.getLevel());
        state.addScore(1);
        assertEquals(2, state.getLevel());
        state.addScore(GameConfig.SCORE_PER_LEVEL);
        assertEquals(3, state.getLevel());
    }

    @Test
    void levelIsCappedSoScoreCannotRunAway() {
        state.addScore(GameConfig.SCORE_PER_LEVEL * 1_000_000);
        assertEquals(GameState.MAX_LEVEL, state.getLevel(), "关卡应顶到上限，实际为 " + state.getLevel());
    }

    /**
     * 分数上限与关卡封顶对齐：打满 {@link GameConfig#VICTORY_SCORE} 时关卡正好顶到
     * {@link GameState#MAX_LEVEL}，也就是"打满 10 关才算通关"。
     * 改了其中一个常量却忘了另一个，这条会红。
     */
    @Test
    void victoryScoreLandsExactlyOnTheLevelCap() {
        state.addScore(GameConfig.VICTORY_SCORE);
        assertEquals(GameState.MAX_LEVEL, state.getLevel(),
                "分数上限应落在关卡封顶那一级，实际关卡 " + state.getLevel());
        assertEquals(GameState.MAX_LEVEL * GameConfig.SCORE_PER_LEVEL, GameConfig.VICTORY_SCORE,
                "分数上限与关卡封顶要对得上账");
    }

    @Test
    void elapsedTimeAccumulates() {
        state.advanceTime(0.016);
        state.advanceTime(0.016);
        assertEquals(0.032, state.getElapsedTime(), 0.0001);
    }

    /** 本关进度（F25）：分子是"本关已得的分"，分母恒为一关的跨度。 */
    @Test
    void levelProgressMeasuresScoreWithinCurrentLevel() {
        assertEquals(0.0, state.getLevelProgress(), 0.0001, "开局尚未得分");

        state.addScore(GameConfig.SCORE_PER_LEVEL / 2);
        assertEquals(0.5, state.getLevelProgress(), 0.0001, "500 分正好是本关一半");

        state.addScore(GameConfig.SCORE_PER_LEVEL / 2);   // 满 1000 分 → 升到第 2 关
        assertEquals(2, state.getLevel());
        assertEquals(0.0, state.getLevelProgress(), 0.0001, "刚升关时进度归零，重新开始攒");

        state.addScore(GameConfig.SCORE_PER_LEVEL / 4);
        assertEquals(0.25, state.getLevelProgress(), 0.0001);
    }

    /** 关卡封顶后仍按"距通关分还差多少"推进，并且一次大额加分越过阈值时夹在 1.0。 */
    @Test
    void levelProgressIsClampedAtOne() {
        state.addScore(GameConfig.SCORE_PER_LEVEL * (GameState.MAX_LEVEL - 1) + 900);
        assertEquals(GameState.MAX_LEVEL, state.getLevel());
        assertEquals(0.9, state.getLevelProgress(), 0.0001, "第 10 关 9900 分即进度 0.9");

        state.addScore(1000);   // 冲破通关阈值，进度会算到 1.5
        assertEquals(1.0, state.getLevelProgress(), 0.0001, "越过阈值时夹到 1.0，经验条不会溢出");
    }

    /** 计时封顶（F24）：超过一小时后不再累计，此后一律按"超时"看待。 */
    @Test
    void elapsedTimeIsCappedAtConfiguredLimit() {
        state.advanceTime(GameConfig.MAX_TRACKED_TIME - 1);
        assertFalse(state.isTimedOut(), "还没到上限时不算超时");

        state.advanceTime(1);
        assertEquals(GameConfig.MAX_TRACKED_TIME, state.getElapsedTime(), 0.0001);
        assertTrue(state.isTimedOut());

        state.advanceTime(100);
        assertEquals(GameConfig.MAX_TRACKED_TIME, state.getElapsedTime(), 0.0001,
                "到了上限就不再计时");
        assertTrue(state.isTimedOut());
    }
}
