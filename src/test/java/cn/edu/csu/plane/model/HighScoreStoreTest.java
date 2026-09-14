package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link HighScoreStore} 的单元测试：正常读写，以及存档缺失/损坏/为空时按 0 处理且不崩（F13 / NF-07）。
 */
class HighScoreStoreTest {

    @TempDir
    Path tempDir;

    private Path saveFile() {
        return tempDir.resolve("highscore.txt");
    }

    @Test
    void returnsZeroWhenNoSaveFile() {
        assertEquals(0, new HighScoreStore(saveFile()).load());
    }

    @Test
    void savesAndLoadsBack() {
        HighScoreStore store = new HighScoreStore(saveFile());
        store.save(1234);
        assertEquals(1234, store.load());
    }

    @Test
    void overwritesPreviousSave() {
        HighScoreStore store = new HighScoreStore(saveFile());
        store.save(500);
        store.save(900);
        assertEquals(900, store.load());
    }

    @Test
    void corruptedSaveIsTreatedAsZero() throws IOException {
        Files.writeString(saveFile(), "这不是一个数字");
        assertEquals(0, new HighScoreStore(saveFile()).load());
    }

    @Test
    void emptySaveIsTreatedAsZero() throws IOException {
        Files.createFile(saveFile());
        assertEquals(0, new HighScoreStore(saveFile()).load());
    }

    @Test
    void negativeSaveIsTreatedAsZero() throws IOException {
        Files.writeString(saveFile(), "-5");
        assertEquals(0, new HighScoreStore(saveFile()).load());
    }

    @Test
    void toleratesSurroundingWhitespace() throws IOException {
        Files.writeString(saveFile(), "  777  ");
        assertEquals(777, new HighScoreStore(saveFile()).load());
    }

    // ---------- 按难度分档（F16） ----------

    /** 普通档沿用原来的 highscore.txt：老存档不用迁移就继续有效。 */
    @Test
    void normalDifficultyReusesTheSameFile() {
        HighScoreStore store = new HighScoreStore(saveFile());
        assertSame(store, store.forDifficulty(Difficulty.NORMAL), "普通档该复用同一个存档对象");
    }

    /** 简单/困难各自落在同目录下的独立文件里，文件名带档位后缀。 */
    @Test
    void otherDifficultiesUseDerivedFiles() {
        HighScoreStore store = new HighScoreStore(saveFile());

        store.forDifficulty(Difficulty.EASY).save(111);
        store.forDifficulty(Difficulty.HARD).save(222);

        assertTrue(Files.exists(tempDir.resolve("highscore_easy.txt")), "简单档该派生 highscore_easy.txt");
        assertTrue(Files.exists(tempDir.resolve("highscore_hard.txt")), "困难档该派生 highscore_hard.txt");
        assertEquals(111, store.forDifficulty(Difficulty.EASY).load());
        assertEquals(222, store.forDifficulty(Difficulty.HARD).load());
    }

    /** 三档互不串档：写一档不会碰到另外两档。 */
    @Test
    void difficultiesDoNotOverwriteEachOther() {
        HighScoreStore store = new HighScoreStore(saveFile());
        store.save(300);                                   // 普通档

        store.forDifficulty(Difficulty.EASY).save(900);
        store.forDifficulty(Difficulty.HARD).save(50);

        assertEquals(300, store.load(), "普通档成绩不该被简单档盖掉");
        assertEquals(900, store.forDifficulty(Difficulty.EASY).load());
        assertEquals(50, store.forDifficulty(Difficulty.HARD).load());
    }

    /** 派生出来的存档同样容错：文件损坏按 0 算，不抛异常。 */
    @Test
    void derivedSaveIsAlsoFaultTolerant() throws IOException {
        HighScoreStore store = new HighScoreStore(saveFile());
        Files.writeString(tempDir.resolve("highscore_hard.txt"), "这不是一个数字");
        assertEquals(0, store.forDifficulty(Difficulty.HARD).load());
    }
}
