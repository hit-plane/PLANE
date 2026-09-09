package cn.edu.csu.plane.model;

import java.util.Scanner;

/**
 * 控制台版主游戏入口：实现完整的游戏循环（开始、进行、结束），
 * 在控制台输出游戏状态和实时信息。
 */
public class ConsoleMain {

    private static final double FRAME_TIME = 1.0 / 60.0; // 60 FPS
    private static final int TARGET_SCORE_TO_WIN = 500; // 通关分数

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("       飞机大战 - 控制台版");
        System.out.println("========================================");

        GameModel gameModel = new cn.edu.csu.plane.model.GameModelImpl();
        Scanner scanner = new Scanner(System.in);

        boolean running = true;

        while (running) {
            showMainMenu(scanner);

            String choice = scanner.nextLine().trim().toLowerCase();

            if (choice.equals("1") || choice.equals("start")) {
                gameModel.initGame();
                runGameLoop(gameModel, scanner);

                System.out.println("\n返回主菜单？(y/n)");
                String backChoice = scanner.nextLine().trim().toLowerCase();
                if (!backChoice.equals("y") && !backChoice.equals("yes")) {
                    running = false;
                }
            } else if (choice.equals("2") || choice.equals("help")) {
                showHelp();
            } else if (choice.equals("q") || choice.equals("quit") || choice.equals("exit")) {
                running = false;
                System.out.println("感谢游玩，再见！");
            } else {
                System.out.println("无效输入，请重新选择！");
            }
        }

        scanner.close();
    }

    /**
     * 显示主菜单
     */
    private static void showMainMenu(Scanner scanner) {
        System.out.println("\n========================================");
        System.out.println("         主菜单");
        System.out.println("========================================");
        System.out.println("  1. 开始游戏 (start)");
        System.out.println("  2. 帮助说明 (help)");
        System.out.println("  Q. 退出游戏 (quit)");
        System.out.println("========================================");
        System.out.print("请选择: ");
    }

    /**
     * 运行游戏主循环
     */
    private static void runGameLoop(GameModel gameModel, Scanner scanner) {
        System.out.println("\n>>> 游戏开始！");
        System.out.println("提示: 每按一次回车推进一帧，输入 'q' 退出当前局\n");

        double totalTime = 0;
        boolean gameRunning = true;

        while (gameRunning) {
            GameStatus status = gameModel.getStatus();

            if (status == GameStatus.GAME_OVER || status == GameStatus.VICTORY) {
                printGameState(gameModel);
                System.out.println("\n*** 游戏结束 ***");
                if (status == GameStatus.VICTORY) {
                    System.out.println("恭喜通关！最终得分: " + gameModel.getScore());
                } else {
                    System.out.println("很遗憾，战机被击毁！最终得分: " + gameModel.getScore());
                }
                break;
            }

            if (status == GameStatus.PLAYING) {
                gameModel.update(FRAME_TIME);
                totalTime += FRAME_TIME;

                printGameState(gameModel);

                System.out.print("\n按回车继续 (输入 'q' 退出): ");
                String input = scanner.nextLine().trim();
                if (input.equalsIgnoreCase("q")) {
                    gameRunning = false;
                    System.out.println("已退出当前游戏");
                }
            } else {
                System.out.println("游戏状态异常: " + status);
                break;
            }
        }
    }

    /**
     * 打印当前游戏状态到控制台
     */
    private static void printGameState(GameModel gameModel) {
        System.out.println("\n----------------------------------------");
        System.out.printf("状态: %-10s | 分数: %-6d | 关卡: %-2d%n",
                gameModel.getStatus(),
                gameModel.getScore(),
                gameModel.getLevel());

        Player player = gameModel.getPlayer();
        System.out.printf("玩家血量: %d/%d | 位置: (%.0f, %.0f)%n",
                player.getHealth(),
                player.getMaxHealth(),
                player.getX(),
                player.getY());

        System.out.printf("敌机数量: %-3d | 子弹数量: %-3d | 道具数量: %-3d%n",
                gameModel.getEnemies().size(),
                gameModel.getBullets().size(),
                gameModel.getItems().size());

        if (!gameModel.getEnemies().isEmpty()) {
            System.out.print("敌机列表: ");
            for (int i = 0; i < Math.min(5, gameModel.getEnemies().size()); i++) {
                Enemy enemy = gameModel.getEnemies().get(i);
                System.out.printf("[%s HP:%d (%.0f,%.0f)] ",
                        enemy.getType(),
                        enemy.getHealth(),
                        enemy.getX(),
                        enemy.getY());
            }
            if (gameModel.getEnemies().size() > 5) {
                System.out.print("...");
            }
            System.out.println();
        }

        System.out.println("----------------------------------------");
    }

    /**
     * 显示帮助信息
     */
    private static void showHelp() {
        System.out.println("\n========================================");
        System.out.println("         游戏说明");
        System.out.println("========================================");
        System.out.println("1. 目标: 击败敌机，获得分数");
        System.out.println("2. 胜利条件: 达到 " + TARGET_SCORE_TO_WIN + " 分");
        System.out.println("3. 失败条件: 玩家血量归零");
        System.out.println("4. 游戏会自动生成敌机和道具");
        System.out.println("5. 目前版本为演示版，自动运行游戏逻辑");
        System.out.println("========================================");
    }
}