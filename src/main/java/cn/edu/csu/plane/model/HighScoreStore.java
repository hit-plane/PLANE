package cn.edu.csu.plane.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 最高分存档（F13）：就一个整数，直接存成文本文件。
 * 存档不存在、内容是垃圾、读不出来，一律当 0 处理，绝不能因为这个把游戏搞崩（NF-07）。
 */
public class HighScoreStore {

    private static final String DEFAULT_FILE_NAME = "highscore.txt";

    private final Path file;

    /** 默认存到程序工作目录下的 highscore.txt。 */
    public HighScoreStore() {
        this(Path.of(DEFAULT_FILE_NAME));
    }

    /** 指定存档路径。测试用它指到临时目录，免得往工作目录里写文件。 */
    public HighScoreStore(Path file) {
        this.file = file;
    }

    /** 读历史最高分；没有记录或存档损坏都返回 0。 */
    public int load() {
        try {
            if (!Files.exists(file)) {
                return 0;
            }
            String text = Files.readString(file).trim();
            return Math.max(Integer.parseInt(text), 0);
        } catch (IOException | NumberFormatException e) {
            System.err.println("最高分存档读不了，按 0 算：" + e.getMessage());
            return 0;
        }
    }

    /** 覆盖写入新的最高分。写失败就提示一声，不影响这一局继续玩。 */
    public void save(int score) {
        try {
            Files.writeString(file, String.valueOf(score));
        } catch (IOException e) {
            System.err.println("最高分存档写不进去：" + e.getMessage());
        }
    }
}
