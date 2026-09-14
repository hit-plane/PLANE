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

    public BombWave() {
        super(0, GameConfig.WINDOW_HEIGHT, GameConfig.WINDOW_WIDTH, GameConfig.BOMB_WAVE_HEIGHT);
        this.velY = -GameConfig.BOMB_WAVE_SPEED;   // 负速度 = 向上扫
    }

    @Override
    public void update(double deltaTime) {
        move(deltaTime);
        if (isOutsideScreen()) {
            alive = false;   // 整条波扫出顶边就销毁
        }
    }
}
