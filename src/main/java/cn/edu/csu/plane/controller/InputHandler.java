package cn.edu.csu.plane.controller;

import javafx.scene.Scene;

/**
 * 输入处理：把键盘按键状态转换为归一化的移动指令，供主循环每帧读取。
 *
 * <p>只负责"位移"一个职责：玩家进入一局后自动射击（F03/Q1），
 * 因此本类不提供射击开关，避免出现与需求矛盾的死代码。</p>
 */
public class InputHandler {

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    /** 绑定键盘事件：WASD 与方向键均可控制移动。 */
    public void attach(Scene scene) {
        scene.setOnKeyPressed(event -> {
            switch (event.getCode()) {
                case W, UP -> up = true;
                case S, DOWN -> down = true;
                case A, LEFT -> left = true;
                case D, RIGHT -> right = true;
                default -> { }
            }
        });
        scene.setOnKeyReleased(event -> {
            switch (event.getCode()) {
                case W, UP -> up = false;
                case S, DOWN -> down = false;
                case A, LEFT -> left = false;
                case D, RIGHT -> right = false;
                default -> { }
            }
        });
    }

    /** 横向位移方向：-1 左 / 0 不动 / 1 右。同时按下相反方向时相互抵消。 */
    public double getMoveX() {
        return (right ? 1 : 0) - (left ? 1 : 0);
    }

    /** 纵向位移方向：-1 上 / 0 不动 / 1 下（窗口 y 轴向下）。 */
    public double getMoveY() {
        return (down ? 1 : 0) - (up ? 1 : 0);
    }

    /** 清空全部按键状态，用于窗口失焦，避免丢焦点后战机持续移动。 */
    public void clearAll() {
        up = false;
        down = false;
        left = false;
        right = false;
    }
}
