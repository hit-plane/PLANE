package cn.edu.csu.plane.model;

/**
 * 游戏状态流转枚举。
 *
 * <p>五个状态：菜单、进行中、暂停、阵亡、通关。阵亡与通关都是终局，都会冻结整帧并弹结算。
 * 通关分数是配置项 {@code level.victory.score}，判定见 {@code GameModelImpl#update}；
 * 同一帧里两者都满足时按阵亡算（死亡判定优先）。</p>
 */
public enum GameStatus {
    MENU, PLAYING, PAUSED, GAME_OVER, VICTORY
}
