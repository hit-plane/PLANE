package cn.edu.csu.plane.controller;

import javafx.scene.Scene;

/**
 * 输入处理：绑定键盘与鼠标事件，并将原始输入转换为移动/射击指令。
 */
public class InputHandler {

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;
    private boolean shooting;

    public void attach(Scene scene) {
        // TODO: 绑定键盘(WASD/方向键)与鼠标事件
    }

    public double getMoveX() {
        // TODO: 根据按键返回 -1/0/1
        return 0;
    }

    public double getMoveY() {
        // TODO: 根据按键返回 -1/0/1
        return 0;
    }

    public boolean isShooting() { return shooting; }
}
