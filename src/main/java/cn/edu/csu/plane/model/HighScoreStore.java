package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 最高分存档（F13）：就一个整数，直接存成文本文件。
 * 存档不存在、内容是垃圾、读不出来，一律当 0 处理，绝不能因为这个把游戏搞崩（NF-07）。
 *
 * <p>最高分按难度分档（F16）：{@link #forDifficulty} 按档位派生不同的存档文件——
 * 普通档沿用原来的 {@code highscore.txt}（老存档继续有效），简单/困难另存
 * {@code highscore_easy.txt} / {@code highscore_hard.txt}，隐藏的折磨档存
 * {@code highscore_torment.txt}。各档各记各的，简单档刷出来的高分不会盖掉困难档的成绩。</p>
 */
public class HighScoreStore {

    private static final String DEFAULT_FILE_NAME = "highscore.txt";
    /** 派生档位存档时的文件名主干与后缀：highscore + _easy/_hard + .txt。 */
    private static final String BASE_NAME = "highscore";
    private static final String EXTENSION = ".txt";

    private final Path file;

    /** 默认存到程序工作目录下的 highscore.txt。 */
    public HighScoreStore() {
        this(Path.of(DEFAULT_FILE_NAME));
    }

    /** 指定存档路径。测试用它指到临时目录，免得往工作目录里写文件。 */
    public HighScoreStore(Path file) {
        this.file = file;
    }

    /**
     * 返回指定难度的存档：普通档就是本对象自己（沿用老文件名），
     * 简单/困难在同目录下派生 {@code highscore_easy.txt} / {@code highscore_hard.txt}。
     *
     * <p>文件名从本存档的实际路径推导，因此测试把存档指到临时目录时，
     * 派生出来的档位存档也落在同一个临时目录里，不会污染工作目录。</p>
     */
    public HighScoreStore forDifficulty(Difficulty difficulty) {
        if (difficulty == Difficulty.NORMAL) {
            return this;
        }
        return new HighScoreStore(siblingPath(suffixOf(difficulty)));
    }

    /** 档位后缀：简单 easy、困难 hard、折磨 torment；普通不走派生，不该问到这里。 */
    private static String suffixOf(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "easy";
            case HARD -> "hard";
            case TORMENT -> "torment";
            case NORMAL -> throw new IllegalArgumentException("普通档复用原存档，不该派生新文件");
        };
    }

    /** 在扩展名前插入后缀：{@code …/highscore.txt} → {@code …/highscore_easy.txt}。 */
    private Path siblingPath(String suffix) {
        String name = file.getFileName().toString();
        String stem = name.endsWith(EXTENSION) ? name.substring(0, name.length() - EXTENSION.length()) : name;
        String derived = stem + "_" + suffix + EXTENSION;
        Path parent = file.getParent();
        return (parent == null) ? Path.of(derived) : parent.resolve(derived);
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
