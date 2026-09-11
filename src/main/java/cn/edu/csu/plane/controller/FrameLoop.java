package cn.edu.csu.plane.controller;

/**
 * 帧循环抽象：把「逐帧回调」与具体定时器实现分开。
 *
 * <p>生产环境用 JavaFX {@code AnimationTimer}（见 {@link GameController.AnimationFrameLoop}）；
 * 测试可注入一个手动触发的假实现，从而在没有 JavaFX 工具箱的环境下驱动
 * {@link GameController#onFrame(long)} 验证单帧编排。包内可见：只服务本控制器与同包测试。</p>
 */
interface FrameLoop {

    /** 开始逐帧回调。重复调用应幂等，不叠加定时器。 */
    void start();

    /** 停止逐帧回调。未启动时调用无副作用。 */
    void stop();
}
