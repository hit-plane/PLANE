package cn.edu.csu.plane.model;

public class GameState {

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

    public void resetScore() {
        this.score = 0;
        this.level = 1;
    }

    public void addScore(int score) {
        this.score += score;
        if (this.score >= level * 200) {
            level++;
        }
    }

    public GameStatus getStatus() { return status; }
    public void setStatus(GameStatus status) { this.status = status; }
    public long getScore() { return score; }
    public int getLevel() { return level; }
    public double getElapsedTime() { return elapsedTime; }
}
