package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 一局的全局状态与数值：游戏状态、累计得分、当前关卡与已进行时长。
 * 关卡按累计得分推进，规则集中在本类，不与实体列表耦合。
 */
public class GameState {

    /** 每升 1 关所需的累计得分（SRS F11：每 1000 分升 1 关）。 */
    public static final int SCORE_PER_LEVEL = GameConfig.SCORE_PER_LEVEL;

    /**
     * 关卡上限：防止连续大额加分（如全屏炸弹）把关卡推到无意义的高度。
     *
     * <p>对外可见是为了让"分数上限"能跟它对账：{@code MAX_LEVEL × SCORE_PER_LEVEL}
     * 正好等于配置里的 {@link GameConfig#VICTORY_SCORE}（10 × 1000 = 10000），
     * 也就是打满 10 关才算通关。</p>
     */
    public static final int MAX_LEVEL = 10;

    private GameStatus status;
    private long score;
    private int level;
    private double elapsedTime;

    public GameState() {
        this.status = GameStatus.MENU;
        this.score = 0;
        this.level = 1;
        this.elapsedTime = 0;
    }

    /** 开始新一局时的数值重置：分数与关卡归零、计时清零。 */
    public void resetScore() {
        this.score = 0;
        this.level = 1;
        this.elapsedTime = 0;
    }

    /**
     * 累加得分并按阈值推进关卡。
     * 用 while 而不是 if：一次加成（例如炸弹清屏逐架补分）可能跨过多个关卡阈值，
     * 只升 1 级会让关卡数偏小。
     */
    public void addScore(int score) {
        this.score += score;
        while (level < MAX_LEVEL && this.score >= (long) level * SCORE_PER_LEVEL) {
            level++;
        }
    }

    /**
     * 推进本局已进行时长，封顶在 {@link GameConfig#MAX_TRACKED_TIME}：
     * 到达上限后不再累加，界面据此显示"超时"（F24）。暂停时本方法根本不会被调到
     * （模型在非 PLAYING 态整帧提前返回），所以暂停的时长天然不计入。
     */
    public void advanceTime(double deltaTime) {
        if (elapsedTime >= GameConfig.MAX_TRACKED_TIME) {
            return;
        }
        this.elapsedTime = Math.min(this.elapsedTime + deltaTime, GameConfig.MAX_TRACKED_TIME);
    }

    /** 本局是否已到计时上限（超过一小时不再计时）。 */
    public boolean isTimedOut() {
        return elapsedTime >= GameConfig.MAX_TRACKED_TIME;
    }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public long getScore() { return score; }
    public int getLevel() { return level; }
    public double getElapsedTime() { return elapsedTime; }
}
