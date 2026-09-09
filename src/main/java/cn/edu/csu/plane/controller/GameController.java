package cn.edu.csu.plane.controller;

import cn.edu.csu.plane.model.GameState;
import cn.edu.csu.plane.view.GameView;

/**
 * 主控制器：驱动游戏主循环，协调模型（Model）与视图（View），并管理状态流转。
 */
public class GameController {

    private final GameState gameState;
    private final GameView gameView;

    public GameController(GameState gameState, GameView gameView) {
        this.gameState = gameState;
        this.gameView = gameView;
    }

    /** 启动游戏主循环。 */
    public void start() {
        // TODO: 设置定时器驱动 update/render
    }

    /** 每帧更新所有模型逻辑（玩家、敌机、子弹、道具、碰撞、生成）。 */
    public void update(double deltaTime) {
        // TODO: 更新实体、碰撞、生成、关卡难度判定
    }

    /** 每帧渲染画面。 */
    public void render() {
        // TODO: 调用视图渲染
    }

    public void pause() {
        // TODO: 暂停
    }

    public void resume() {
        // TODO: 恢复
    }

    public void restart() {
        // TODO: 重新开始
    }

    private void onGameOver() {
        // TODO: 触发结束逻辑
    }
}
