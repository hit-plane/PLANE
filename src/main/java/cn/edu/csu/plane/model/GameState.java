package cn.edu.csu.plane.model;

/**
 * 游戏整体状态：当前状态流转、得分、等级、关卡进度与对局时长。
 */
public class GameState {

    private GameStatus status;
    private long score;
    private int level;
    private double elapsedTime;

    public GameState() {
        this.status = GameStatus.MENU;
    }

    public void addScore(int score) {
        // TODO: 累加得分并触发关卡判定
    }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public long getScore() { return score; }
    public int getLevel() { return level; }
    public double getElapsedTime() { return elapsedTime; }
}
