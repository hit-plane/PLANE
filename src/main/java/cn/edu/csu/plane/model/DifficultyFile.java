package cn.edu.csu.plane.model;

import cn.edu.csu.plane.util.Difficulty;

import java.nio.file.Path;

/**
 * 按难度派生存档文件路径的公共逻辑：普通档用原文件本身，简单/困难/折磨在同目录下派生
 * {@code 主干_档位.txt}。
 *
 * <p>最高分存档与通关用时存档共用这一套命名规则，所以抽在这里，两处各写一份容易改歪。</p>
 */
final class DifficultyFile {

    private static final String EXTENSION = ".txt";

    private DifficultyFile() {
    }

    /** 普通档返回原路径；其余档返回同目录、扩展名前插了档位后缀的路径。 */
    static Path forDifficulty(Path base, Difficulty difficulty) {
        if (difficulty == Difficulty.NORMAL) {
            return base;
        }
        String name = base.getFileName().toString();
        String stem = name.endsWith(EXTENSION) ? name.substring(0, name.length() - EXTENSION.length()) : name;
        String derived = stem + "_" + suffixOf(difficulty) + EXTENSION;
        Path parent = base.getParent();
        return (parent == null) ? Path.of(derived) : parent.resolve(derived);
    }

    /** 档位后缀：简单 easy、困难 hard、折磨 torment；普通档不走派生，不该问到这里。 */
    private static String suffixOf(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> "easy";
            case HARD -> "hard";
            case TORMENT -> "torment";
            case NORMAL -> throw new IllegalArgumentException("普通档复用原存档，不该派生新文件");
        };
    }
}
