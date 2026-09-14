package cn.edu.csu.plane.view;

import cn.edu.csu.plane.util.Difficulty;
import javafx.application.Platform;
import javafx.scene.control.Button;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 主菜单难度选择（F16）的单元测试：确认三个档位按钮建出来了、默认落在普通档、
 * 点按钮能换档并通知装配层去刷新最高分。
 *
 * <p>菜单要 new JavaFX 控件，所以和 {@code GameControllerTest} 一样先起工具箱，
 * 所有控件构造与操作都放到 FX 应用线程上执行。难度按钮数组是私有字段，
 * 通过反射读取，不为测试放宽生产可见性（同 {@code GameModelImplTest} 的做法）。</p>
 */
class MainMenuViewTest {

    @BeforeAll
    static void startJavaFxToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // 同一个 JVM 里别的测试类已经起过了，直接用
        }
    }

    @AfterAll
    static void stopJavaFxToolkit() {
        Platform.exit();
    }

    /**
     * 把构造与操作丢到 FX 应用线程上跑（JavaFX 控件有线程校验）。
     * 捕获 {@link Throwable} 而不是 RuntimeException：断言失败抛的是 Error，
     * 只接 RuntimeException 会让它逃逸到 FX 线程，测试这边拿到 null 后误报成另一个错。
     */
    private static <T> T onFxThread(Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) {
            return supplier.get();
        }
        AtomicReference<T> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                result.set(supplier.get());
            } catch (Throwable t) {
                failure.set(t);
            } finally {
                done.countDown();
            }
        });
        try {
            if (!done.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等 JavaFX 线程建菜单超时");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("等 JavaFX 线程建菜单被中断", e);
        }
        Throwable thrown = failure.get();
        if (thrown instanceof RuntimeException runtime) {
            throw runtime;
        }
        if (thrown instanceof Error error) {
            throw error;
        }
        if (thrown != null) {
            throw new IllegalStateException("FX 线程里抛了非预期异常", thrown);
        }
        return result.get();
    }

    /** 默认必须是普通档：不做任何选择时的行为要和旧版一致。 */
    @Test
    void defaultsToNormalDifficulty() {
        MainMenuView menu = onFxThread(MainMenuView::new);
        assertEquals(Difficulty.NORMAL, menu.getSelectedDifficulty());
    }

    /** 三档按钮都在菜单上，文字就是档位名，从上到下由易到难。 */
    @Test
    void menuShowsThreeDifficultyButtons() {
        MainMenuView menu = onFxThread(MainMenuView::new);
        List<String> labels = onFxThread(() ->
                Arrays.stream(difficultyButtonsOf(menu)).map(Button::getText).toList());

        assertEquals(List.of("简单", "普通", "困难"), labels);
    }

    /** 点按钮换档，并把新档位通知给装配层（由它去刷新对应难度的最高分）。 */
    @Test
    void clickingDifficultyButtonChangesSelectionAndNotifies() {
        AtomicReference<Difficulty> notified = new AtomicReference<>();
        MainMenuView menu = onFxThread(() -> {
            MainMenuView view = new MainMenuView();
            view.setOnDifficultyChange(notified::set);
            return view;
        });

        onFxThread(() -> {
            difficultyButtonsOf(menu)[0].fire();   // 简单
            return null;
        });
        assertEquals(Difficulty.EASY, menu.getSelectedDifficulty());
        assertEquals(Difficulty.EASY, notified.get(), "换档该通知装配层刷新最高分");

        onFxThread(() -> {
            difficultyButtonsOf(menu)[2].fire();   // 困难
            return null;
        });
        assertEquals(Difficulty.HARD, menu.getSelectedDifficulty());
        assertEquals(Difficulty.HARD, notified.get());
    }

    /** 反射取私有的难度按钮数组，顺序与界面一致（简单/普通/困难）。 */
    private static Button[] difficultyButtonsOf(MainMenuView menu) {
        try {
            Field field = MainMenuView.class.getDeclaredField("difficultyButtons");
            field.setAccessible(true);
            Button[] buttons = (Button[]) field.get(menu);
            assertEquals(3, buttons.length, "难度按钮该有简单/普通/困难三个");
            return buttons;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("取不到难度按钮数组", e);
        }
    }
}
