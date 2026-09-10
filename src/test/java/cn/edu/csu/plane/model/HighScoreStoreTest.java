package cn.edu.csu.plane.model;

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
}
