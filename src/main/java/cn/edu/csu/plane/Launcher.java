package cn.edu.csu.plane;

/**
 * fat jar 的启动入口：本身不继承 {@code Application}，只转发给 {@link PlaneApp}。
 *
 * <p>不能直接把 {@code PlaneApp} 设为 jar 主类——JavaFX 的启动器检测到主类继承
 * {@code Application} 且缺少 module-path 时，会抛"JavaFX runtime components are missing"。
 * 隔一层普通类即可绕开该检测，依赖已在 classpath 上，{@code Application.launch} 正常走通。</p>
 */
public final class Launcher {

    private Launcher() {
    }

    public static void main(String[] args) {
        PlaneApp.main(args);
    }
}
