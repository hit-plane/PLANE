package cn.edu.csu.plane.model;

/**
 * 受击特效：纯视觉反馈，不参与碰撞或计分。
 *
 * <p>在碰撞结算时于碰撞点生成，经过一段短促的生命周期后自动消亡。
 * 视图层根据 {@link Type} 选择不同的颜色与扩散速度渲染。</p>
 *
 * <p>三种类型：
 * <ul>
 *   <li>{@link Type#ENEMY_HIT} — 敌机被子弹命中，小而亮的白黄色闪光</li>
 *   <li>{@link Type#ENEMY_EXPLODE} — 敌机被击毁，较大的橙红色爆炸扩散</li>
 *   <li>{@link Type#PLAYER_HIT} — 玩家受击，红色警示闪烁</li>
 * </ul>
 */
public class HitEffect {

    /** 特效类型，视图层据此选颜色与扩散速度。 */
    public enum Type {
        ENEMY_HIT,
        ENEMY_EXPLODE,
        PLAYER_HIT
    }

    private final double x;
    private final double y;
    private final Type type;
    private double timer;
    private final double maxTime;
    private boolean alive;

    /**
     * 在指定坐标生成一个受击特效。
     *
     * @param x       碰撞点 x（特效中心）
     * @param y       碰撞点 y（特效中心）
     * @param type    特效类型
     * @param maxTime 生命周期总时长（秒）
     */
    public HitEffect(double x, double y, Type type, double maxTime) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.maxTime = maxTime;
        this.timer = maxTime;
        this.alive = true;
    }

    /**
     * 每帧递减计时，时间耗尽后标记为死亡，由帧末清理移除。
     */
    public void update(double deltaTime) {
        if (!alive) return;
        timer -= deltaTime;
        if (timer <= 0) {
            timer = 0;
            alive = false;
        }
    }

    /** 生命周期进度：0 = 刚生成，1 = 已消亡。视图层据此算扩散半径与透明度。 */
    public double getProgress() {
        return 1.0 - timer / maxTime;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public Type getType() { return type; }
    public boolean isAlive() { return alive; }
}
