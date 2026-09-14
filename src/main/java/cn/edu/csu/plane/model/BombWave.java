package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.GameConfig;

/**
 * 全屏炸弹的冲击波：一整条横贯战场的波带，从底边向上扫。
 *
 * <p>它只负责"移动 + 生命周期"：扫到敌机/敌弹的销毁与计分在
 * {@code GameModelImpl#resolveBombWaves} 里结算。波带宽度取整个战场宽，
 * 高度与扫速取配置常量（{@code bomb.wave.*}），便于调参。</p>
 */
public class BombWave extends Entity {

    /** 单颗炸弹的剩余得分预算：封顶一关的分，防止高密度下连跳多关。 */
    private int scoreBudget = GameConfig.BOMB_WAVE_SCORE_CAP;

    public BombWave() {
        super(0, GameConfig.WINDOW_HEIGHT, GameConfig.WINDOW_WIDTH, GameConfig.BOMB_WAVE_HEIGHT);
        this.velY = -GameConfig.BOMB_WAVE_SPEED;   // 负速度 = 向上扫
    }

    /** 从得分预算里扣一块，返回实际能加的分；预算耗尽后返回 0。 */
    public int consumeScore(int amount) {
        int granted = Math.min(amount, scoreBudget);
        scoreBudget -= granted;
        return granted;
    }

    @Override
    public void update(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;   // 整条波扫出顶边就销毁
        }
    }
}
