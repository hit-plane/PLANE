package cn.edu.csu.plane.controller;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;

/**
 * 输入处理：把键盘按键状态转换为归一化的移动指令，供主循环每帧读取。
 *
 * <p>只负责"位移"一个职责：玩家进入一局后自动射击（F03/Q1），
 * 因此本类不提供射击开关，避免出现与需求矛盾的死代码。</p>
 *
 * <p><b>按键状态是实例字段</b>：绑定事件的对象与主循环读取的对象必须是同一个实例。
 * 因此外部不要自己 new 本类去 attach 场景，一律走
 * {@link GameController#attachInput(Scene)} —— 由持有者绑定它自己读取的那个实例。</p>
 */
public class InputHandler {

    private boolean up;
    private boolean down;
    private boolean left;
    private boolean right;

    /**
     * 绑定键盘事件：WASD 与方向键均可控制移动。
     *
     * <p><b>故意是包内可见</b>：只有同包的 {@link GameController} 能调它，
     * 从类外根本拿不到绑定入口，也就 new 不出"绑了却没人读"的第二份输入状态。</p>
     */
    void attach(Scene scene) {
        scene.setOnKeyPressed(event -> handleKeyPressed(event.getCode()));
        scene.setOnKeyReleased(event -> handleKeyReleased(event.getCode()));
    }

    /**
     * 处理一次按下：把键位映射成方向开关。
     *
     * <p>与 {@link #attach(Scene)} 共用同一套映射，因此本方法可以脱离 JavaFX 场景
     * 单独测试（测试不启动工具箱，直接喂 KeyCode）。</p>
     */
    void handleKeyPressed(KeyCode code) {
        switch (code) {
            case W, UP -> up = true;
            case S, DOWN -> down = true;
            case A, LEFT -> left = true;
            case D, RIGHT -> right = true;
            default -> { }
        }
    }

    /** 处理一次松开：清掉对应方向开关，未绑定键位不产生影响。 */
    void handleKeyReleased(KeyCode code) {
        switch (code) {
            case W, UP -> up = false;
            case S, DOWN -> down = false;
            case A, LEFT -> left = false;
            case D, RIGHT -> right = false;
            default -> { }
        }
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
