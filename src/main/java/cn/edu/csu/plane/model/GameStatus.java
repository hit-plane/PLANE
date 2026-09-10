package cn.edu.csu.plane.model;

/**
 * 游戏状态流转枚举。
 *
 * <p>四个状态：菜单、进行中、暂停、已结束。SRS v1.0 只定义了"血量归零判负"，
 * 未定义胜利条件，因此不设 VICTORY 态；分数仅作为一局的成绩记录。</p>
 */
public enum GameStatus {
    MENU, PLAYING, PAUSED, GAME_OVER
}
