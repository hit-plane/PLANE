package cn.edu.csu.plane.controller;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

/**
 * InputHandler 单元测试：直接喂 KeyCode，不启动 JavaFX 工具箱。
 *
 * <p>覆盖 F02 的键位口径（WASD 与方向键等效）、相反方向抵消、失焦清空，
 * 以及"绑定入口只能由持有者调用"这条设计约束。</p>
 */
class InputHandlerTest {

    private InputHandler input;

    @BeforeEach
    void setUp() {
        input = new InputHandler();
    }

    // ========== 1. 键位映射 ==========

    @Test
    void wasdAndArrowKeys_shouldMapToSameDirection() {
        // 验证点：WASD 与方向键完全等效，按下置位、松开复位
        assertEquivalent(KeyCode.W, KeyCode.UP, 0, -1);
        assertEquivalent(KeyCode.S, KeyCode.DOWN, 0, 1);
        assertEquivalent(KeyCode.A, KeyCode.LEFT, -1, 0);
        assertEquivalent(KeyCode.D, KeyCode.RIGHT, 1, 0);
    }

    /** 按下 wasd 与 arrow 应得到同一个方向，松开后归零。 */
    private void assertEquivalent(KeyCode wasd, KeyCode arrow, double expectedX, double expectedY) {
        input.handleKeyPressed(wasd);
        assertEquals(expectedX, input.getMoveX(), wasd + " 的横向方向不对");
        assertEquals(expectedY, input.getMoveY(), wasd + " 的纵向方向不对");
        input.handleKeyReleased(wasd);
        assertEquals(0.0, input.getMoveX(), wasd + " 松开后横向应归零");
        assertEquals(0.0, input.getMoveY(), wasd + " 松开后纵向应归零");

        input.handleKeyPressed(arrow);
        assertEquals(expectedX, input.getMoveX(), arrow + " 应与 " + wasd + " 等效");
        assertEquals(expectedY, input.getMoveY(), arrow + " 应与 " + wasd + " 等效");
        input.handleKeyReleased(arrow);
    }

    @Test
    void noKeyPressed_shouldMeanNoMovement() {
        // 验证点：初始状态不动，且方向分量恒在 -1/0/1 三档内
        assertEquals(0.0, input.getMoveX(), "没按键时横向应为 0");
        assertEquals(0.0, input.getMoveY(), "没按键时纵向应为 0");
    }

    @Test
    void horizontalAndVertical_shouldBeIndependent() {
        // 验证点：同时按 D + W 得到右上方向的 (-1 纵向)，两轴互不干扰
        input.handleKeyPressed(KeyCode.D);
        input.handleKeyPressed(KeyCode.W);
        assertEquals(1.0, input.getMoveX(), "右上时横向应为右");
        assertEquals(-1.0, input.getMoveY(), "右上时纵向应为上");
    }

    // ========== 2. 多键组合 ==========

    @Test
    void oppositeKeys_shouldCancelOut() {
        // 验证点：左右同按不动；松开其中一个后另一个立即恢复生效
        input.handleKeyPressed(KeyCode.A);
        input.handleKeyPressed(KeyCode.D);
        assertEquals(0.0, input.getMoveX(), "左右同时按下应互相抵消");

        input.handleKeyReleased(KeyCode.A);
        assertEquals(1.0, input.getMoveX(), "松开左键后应只剩右键生效");
    }

    @Test
    void repeatedPressThenSingleRelease_shouldStopMovement() {
        // 验证点：按住不放会反复触发 keyPressed，不能把状态累加成"按了两次"
        input.handleKeyPressed(KeyCode.D);
        input.handleKeyPressed(KeyCode.D);
        input.handleKeyPressed(KeyCode.D);
        assertEquals(1.0, input.getMoveX(), "重复按下仍是同一档方向");

        input.handleKeyReleased(KeyCode.D);
        assertEquals(0.0, input.getMoveX(), "松开一次就该停下");
    }

    @Test
    void releaseWithoutPress_shouldBeHarmless() {
        // 验证点：没按过的键松开不应产生反向位移
        input.handleKeyReleased(KeyCode.A);
        input.handleKeyReleased(KeyCode.UP);
        assertEquals(0.0, input.getMoveX(), "空松键不该产生横向位移");
        assertEquals(0.0, input.getMoveY(), "空松键不该产生纵向位移");
    }

    // ========== 3. 无关键位 ==========

    @Test
    void unboundKeys_shouldNotMovePlayer() {
        // 验证点：射击是自动的，空格/暂停键等都不该产生位移（F03/Q1）
        KeyCode[] unbound = {KeyCode.SPACE, KeyCode.P, KeyCode.ESCAPE, KeyCode.ENTER, KeyCode.SHIFT};
        for (KeyCode code : unbound) {
            input.handleKeyPressed(code);
            assertEquals(0.0, input.getMoveX(), code + " 不该产生横向位移");
            assertEquals(0.0, input.getMoveY(), code + " 不该产生纵向位移");
            input.handleKeyReleased(code);
        }
    }

    @Test
    void unboundKey_shouldNotClearHeldDirection() {
        // 验证点：按无关键不能把正在按住的方向抹掉（切输入法、点暂停键时战机不能停）
        input.handleKeyPressed(KeyCode.D);
        input.handleKeyPressed(KeyCode.ESCAPE);
        input.handleKeyReleased(KeyCode.ESCAPE);
        assertEquals(1.0, input.getMoveX(), "按无关键不该影响已按住的方向键");
    }

    // ========== 4. 清空（窗口失焦） ==========

    @Test
    void clearAll_shouldDropEveryDirection() {
        // 验证点：失焦后方向键的 keyReleased 收不到，必须能整体清空，否则战机持续移动。
        // 四个方向逐个验（不能四个一起按：左右会互相抵消，验不出到底是清零了还是抵消了）
        assertClearedBy(KeyCode.W, 0, -1);
        assertClearedBy(KeyCode.S, 0, 1);
        assertClearedBy(KeyCode.A, -1, 0);
        assertClearedBy(KeyCode.D, 1, 0);
    }

    /** 单独按住 key，确认方向已生效，再 clearAll 确认它被清掉。 */
    private void assertClearedBy(KeyCode key, double expectedX, double expectedY) {
        input.handleKeyPressed(key);
        assertEquals(expectedX, input.getMoveX(), "前置条件：" + key + " 的横向方向应已生效");
        assertEquals(expectedY, input.getMoveY(), "前置条件：" + key + " 的纵向方向应已生效");

        input.clearAll();
        assertEquals(0.0, input.getMoveX(), "clearAll 后 " + key + " 的横向方向应被清掉");
        assertEquals(0.0, input.getMoveY(), "clearAll 后 " + key + " 的纵向方向应被清掉");
    }

    // ========== 5. 绑定入口可见性（设计约束） ==========

    @Test
    void attach_shouldStayPackagePrivate() {
        // 验证点：attach 故意包内可见，外部 new 不出"绑了却没人读"的第二份输入状态
        // （键位状态是实例字段，谁绑定谁读取必须是同一个对象）。
        Method attach = assertDoesNotThrow(
                () -> InputHandler.class.getDeclaredMethod("attach", Scene.class),
                "attach(Scene) 这个唯一绑定入口不该被删掉");
        assertFalse(Modifier.isPublic(attach.getModifiers()),
                "attach(Scene) 一旦公开，外部就能绕开 GameController.attachInput 自绑一份没人读的输入状态");
    }
}
