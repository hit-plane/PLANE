package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link ClearTimeStore} 的单元测试：读写往返、按难度分档隔离，
 * 以及存档缺失/损坏/为空时按"未通关"处理且不崩（F24 / NF-07）。
 */
class ClearTimeStoreTest {

    @TempDir
    Path tempDir;

    private Path saveFile() {
        return tempDir.resolve("besttime.txt");
    }

    @Test
    void missingFileMeansNotCleared() {
        assertEquals(ClearTime.notCleared(), new ClearTimeStore(saveFile()).load());
    }

    @Test
    void savesAndLoadsAClearTime() {
        ClearTimeStore store = new ClearTimeStore(saveFile());
        store.save(ClearTime.of(83_450));
        assertEquals(ClearTime.of(83_450), new ClearTimeStore(saveFile()).load());
    }

    @Test
    void savesAndLoadsTimeout() {
        ClearTimeStore store = new ClearTimeStore(saveFile());
        store.save(ClearTime.timeout());
        assertEquals(ClearTime.timeout(), new ClearTimeStore(saveFile()).load());
    }

    @Test
    void corruptContentFallsBackToNotCleared() throws IOException {
        Files.writeString(saveFile(), "这不是一个数字");
        assertEquals(ClearTime.notCleared(), new ClearTimeStore(saveFile()).load(),
                "存档损坏该按未通关算，不该抛异常");
    }

    @Test
    void emptyContentFallsBackToNotCleared() throws IOException {
        Files.writeString(saveFile(), "   ");
        assertEquals(ClearTime.notCleared(), new ClearTimeStore(saveFile()).load());
    }

    /** 各档各记各的：简单档的成绩落在自己的存档里，不碰普通档那条记录。 */
    @Test
    void recordsAreKeptPerDifficulty() {
        ClearTimeStore store = new ClearTimeStore(saveFile());

        store.forDifficulty(Difficulty.EASY).save(ClearTime.of(50_000));

        assertEquals(ClearTime.of(50_000), store.forDifficulty(Difficulty.EASY).load());
        assertEquals(ClearTime.notCleared(), store.load(), "普通档不该被简单档的成绩污染");
        assertTrue(Files.exists(tempDir.resolve("besttime_easy.txt")),
                "简单档该派生自己的存档文件");
        assertFalse(Files.exists(saveFile()), "没写过普通档，就不该凭空建出普通档的存档");
    }

    /** 用时存档默认落在最高分存档同一个目录下，测试注入临时目录时不会污染工作目录。 */
    @Test
    void besideHighScoreStoreUsesSameDirectory() {
        Path highScoreFile = tempDir.resolve("highscore.txt");
        ClearTimeStore store = ClearTimeStore.beside(highScoreFile);

        store.save(ClearTime.of(12_340));

        assertTrue(Files.exists(tempDir.resolve("besttime.txt")));
        assertEquals(ClearTime.of(12_340), store.load());
    }
}
