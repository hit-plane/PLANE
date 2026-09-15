package cn.edu.csu.plane.view;

import cn.edu.csu.plane.util.Difficulty;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.event.EventType;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

    /** 常规三档按钮都显示在菜单上，文字就是档位名，由易到难；隐藏的折磨档不显示。 */
    @Test
    void menuShowsThreeDifficultyButtons() {
        MainMenuView menu = onFxThread(MainMenuView::new);
        List<String> labels = onFxThread(() ->
                Arrays.stream(difficultyButtonsOf(menu))
                        .filter(Node::isVisible)
                        .map(Button::getText).toList());

        assertEquals(List.of("简单", "普通", "困难"), labels);
        assertFalse(menu.isTormentUnlocked(), "没输暗号时折磨档不该出现");
    }

    /** 暗号解锁后折磨档按钮出现并被自动选中（暗号本身的匹配逻辑在 PlaneApp 里）。 */
    @Test
    void unlockTormentShowsAndSelectsTheHiddenTier() {
        MainMenuView menu = onFxThread(MainMenuView::new);
        onFxThread(() -> {
            menu.unlockTorment();
            return null;
        });

        assertTrue(menu.isTormentUnlocked(), "解锁后折磨档按钮该可见");
        assertEquals(Difficulty.TORMENT, menu.getSelectedDifficulty(), "解锁后该自动选中折磨档");
    }

    /**
     * 作弊开关：解锁前不存在（按钮不显示、状态为关），解锁后可以自己按，
     * 而且解锁本身<b>不会</b>顺手把作弊打开——要不要用由玩家决定。
     */
    @Test
    void cheatToggleAppearsAfterUnlockAndNotifies() {
        AtomicReference<Boolean> notified = new AtomicReference<>();
        MainMenuView menu = onFxThread(() -> {
            MainMenuView view = new MainMenuView();
            view.setOnCheatChange(notified::set);
            return view;
        });

        assertFalse(menu.isCheatEnabled(), "默认不该开着作弊");
        assertFalse(cheatButtonOf(menu).isVisible(), "没输暗号时作弊开关不该出现");

        onFxThread(() -> {
            menu.unlockTorment();
            return null;
        });
        assertTrue(cheatButtonOf(menu).isVisible(), "解锁后作弊开关该出现");
        assertFalse(menu.isCheatEnabled(), "解锁只是提供入口，不该自动开启作弊");
        assertEquals("可滥权", cheatButtonOf(menu).getText(), "未开启时按钮写的是当前状态");

        onFxThread(() -> {
            cheatButtonOf(menu).fire();
            return null;
        });
        assertTrue(menu.isCheatEnabled(), "按一下该打开");
        assertEquals(Boolean.TRUE, notified.get(), "状态变化该通知装配层写进模型");
        assertEquals("滥权中", cheatButtonOf(menu).getText(), "按钮文字要能看出当前状态");

        onFxThread(() -> {
            cheatButtonOf(menu).fire();
            return null;
        });
        assertFalse(menu.isCheatEnabled(), "再按一下该关掉");
        assertEquals(Boolean.FALSE, notified.get());
    }

    /**
     * 左下角的音效按钮：显示的必须是<b>当前状态</b>（音效开 / 音效关）而不是动作，
     * 按一下切换 {@link SoundPlayer} 的玩家开关并改回状态文案。
     */
    @Test
    void soundButtonShowsStateAndToggles() {
        boolean before = SoundPlayer.isPlayerEnabled();
        try {
            onFxThread(() -> {
                SoundPlayer.setPlayerEnabled(true);
                return null;
            });
            MainMenuView menu = onFxThread(MainMenuView::new);
            assertEquals("音效开", soundButtonOf(menu).getText(), "开着时按钮写'音效开'");

            onFxThread(() -> {
                soundButtonOf(menu).fire();
                return null;
            });
            assertFalse(SoundPlayer.isPlayerEnabled(), "按一下该静音");
            assertEquals("音效关", soundButtonOf(menu).getText(), "关掉后按钮改写成'音效关'");

            onFxThread(() -> {
                soundButtonOf(menu).fire();
                return null;
            });
            assertTrue(SoundPlayer.isPlayerEnabled(), "再按一下恢复出声");
            assertEquals("音效开", soundButtonOf(menu).getText());
        } finally {
            // 静态开关是全局的，测完还原，免得污染同一 JVM 里的其它用例
            onFxThread(() -> {
                SoundPlayer.setPlayerEnabled(before);
                return null;
            });
        }
    }

    /**
     * 两个角按钮必须压在中间那一列内容<b>之上</b>。
     *
     * <p>回归防线：那一列内容（VBox）被 StackPane 拉伸铺满整个根、命中测试按整块矩形走，
     * 若角按钮排在它前面，角上的点击会被它先接走——现象就是"按钮按了没反应"。
     * 这里断言的是场景图里的先后顺序，也就是命中测试的优先级。</p>
     */
    @Test
    void cornerButtonsSitAboveMenuContent() {
        MainMenuView menu = onFxThread(MainMenuView::new);
        onFxThread(() -> {
            StackPane root = (StackPane) menu.getNode();
            int content = -1;
            for (int i = 0; i < root.getChildren().size(); i++) {
                if (root.getChildren().get(i) instanceof VBox) {
                    content = i;
                }
            }
            assertTrue(content >= 0, "前置条件：根里应有中间那一列内容");
            assertTrue(root.getChildren().indexOf(soundButtonOf(menu)) > content,
                    "音效按钮必须排在内容层之后，否则角上的点击会被内容层吞掉");
            assertTrue(root.getChildren().indexOf(cheatButtonOf(menu)) > content,
                    "滥权按钮必须排在内容层之后");
            return null;
        });
    }

    /** 悬停时套一层描边；离开后要还原成该按钮<b>当前状态</b>的样式（滥权开着时是红的，不能被冲掉）。 */
    @Test
    void cornerButtonsShowHoverBorderAndRestoreState() {
        MainMenuView menu = onFxThread(MainMenuView::new);
        onFxThread(() -> {
            Button sound = soundButtonOf(menu);
            String base = sound.getStyle();
            Event.fireEvent(sound, mouseEvent(MouseEvent.MOUSE_ENTERED));
            assertNotEquals(base, sound.getStyle(), "悬停该换一套样式");
            assertTrue(sound.getStyle().contains("#00bfff"), "悬停应出现描边（蓝色）");
            Event.fireEvent(sound, mouseEvent(MouseEvent.MOUSE_EXITED));
            assertEquals(base, sound.getStyle(), "离开后应还原原样式");

            menu.setCheatEnabled(true);
            Button abuse = cheatButtonOf(menu);
            String onStyle = abuse.getStyle();
            assertTrue(onStyle.contains("#ff5252"), "前置条件：开启时是红色描边");
            Event.fireEvent(abuse, mouseEvent(MouseEvent.MOUSE_ENTERED));
            assertTrue(abuse.getStyle().contains("#00bfff"), "悬停应出现描边");
            Event.fireEvent(abuse, mouseEvent(MouseEvent.MOUSE_EXITED));
            assertEquals(onStyle, abuse.getStyle(), "离开后要还原成开启态的红色，而不是未选中态");
            return null;
        });
    }

    private static MouseEvent mouseEvent(EventType<MouseEvent> type) {
        return new MouseEvent(type, 0, 0, 0, 0, MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false, null);
    }

    /** 反射取私有的音效按钮。 */
    private static Button soundButtonOf(MainMenuView menu) {
        try {
            Field field = MainMenuView.class.getDeclaredField("soundButton");
            field.setAccessible(true);
            return (Button) field.get(menu);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("取不到音效按钮", e);
        }
    }

    /** 反射取私有的作弊开关按钮。 */
    private static Button cheatButtonOf(MainMenuView menu) {
        try {
            Field field = MainMenuView.class.getDeclaredField("cheatButton");
            field.setAccessible(true);
            return (Button) field.get(menu);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("取不到作弊开关按钮", e);
        }
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

    /**
     * 反射取私有的难度按钮数组，顺序与界面一致（简单/普通/困难/折磨）。
     * 数组有四项，但隐藏档默认 {@code visible=false}，所以界面上仍是三档；
     * 索引 0/1/2 恒为 简单/普通/困难，索引 3 是暗号解锁的折磨档。
     */
    private static Button[] difficultyButtonsOf(MainMenuView menu) {
        try {
            Field field = MainMenuView.class.getDeclaredField("difficultyButtons");
            field.setAccessible(true);
            Button[] buttons = (Button[]) field.get(menu);
            assertEquals(4, buttons.length, "难度按钮该有简单/普通/困难三档 + 隐藏的折磨档");
            return buttons;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("取不到难度按钮数组", e);
        }
    }
}
