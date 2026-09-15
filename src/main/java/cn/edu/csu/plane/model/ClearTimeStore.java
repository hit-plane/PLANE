package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 通关最短用时存档（F24）：按难度分档，各档各记各的，简单档刷出来的成绩不会盖掉困难档的。
 *
 * <p>存的是 {@link ClearTime#toStored()} 那个整数（0 = 未通关、-1 = 超时、正数 = 用时毫秒）。
 * 文件不存在、内容读不出来、不是数字，一律按"未通关"处理，绝不能因为这个把游戏搞崩（NF-07）。</p>
 *
 * <p>命名规则与最高分存档一致（普通档用原文件，其余档派生 {@code besttime_easy.txt} 等），
 * 且默认落在最高分存档的同一个目录下：测试把最高分存档指到临时目录时，用时存档也跟着进去，
 * 不会往工作目录里写文件。</p>
 */
public class ClearTimeStore {

    private static final String DEFAULT_FILE_NAME = "besttime.txt";

    private final Path file;

    /** 默认存到程序工作目录下的 besttime.txt。 */
    public ClearTimeStore() {
        this(Path.of(DEFAULT_FILE_NAME));
    }

    /** 指定存档路径。测试用它指到临时目录，免得往工作目录里写文件。 */
    public ClearTimeStore(Path file) {
        this.file = file;
    }

    /** 与给定的最高分存档同目录的用时存档，供模型按最高分存档的位置一并定位。 */
    static ClearTimeStore beside(Path highScoreFile) {
        Path parent = highScoreFile.getParent();
        return new ClearTimeStore(parent == null ? Path.of(DEFAULT_FILE_NAME)
                : parent.resolve(DEFAULT_FILE_NAME));
    }

    /**
     * 返回指定难度的存档：普通档就是本对象自己（沿用 besttime.txt），
     * 简单/困难/折磨在同目录下派生 {@code besttime_<档位>.txt}。
     */
    public ClearTimeStore forDifficulty(Difficulty difficulty) {
        if (difficulty == Difficulty.NORMAL) {
            return this;
        }
        return new ClearTimeStore(DifficultyFile.forDifficulty(file, difficulty));
    }

    /** 读本档最短通关用时；没有记录或存档损坏都按"未通关"算。 */
    public ClearTime load() {
        try {
            if (!Files.exists(file)) {
                return ClearTime.notCleared();
            }
            String text = Files.readString(file).trim();
            return ClearTime.fromStored(Long.parseLong(text));
        } catch (IOException | NumberFormatException e) {
            System.err.println("通关用时存档读不了，按未通关算：" + e.getMessage());
            return ClearTime.notCleared();
        }
    }

    /** 覆盖写入新的记录。写失败就提示一声，不影响这一局继续玩。 */
    public void save(ClearTime time) {
        try {
            Files.writeString(file, String.valueOf(time.toStored()));
        } catch (IOException e) {
            System.err.println("通关用时存档写不进去：" + e.getMessage());
        }
    }
}
