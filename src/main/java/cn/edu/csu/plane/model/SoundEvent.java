package cn.edu.csu.plane.model;

/**
 * 音效事件：一局里"该响一声"的瞬时事件（F14）。
 *
 * <p>模型只负责说明<b>发生了什么</b>，不含文件名、不含路径、更不碰任何音频 API——
 * 与 {@link HitEffect}（受击特效）同一路数：模型把瞬时事件攒成一张表，
 * 外层每帧用 {@link GameModel#consumeSoundEvents()} 取走，由 View 层决定放哪个素材。
 * 这样 model 层仍然零 javafx 依赖，纯逻辑测试也不必加载音频。</p>
 *
 * <p>事件与素材的对应关系集中在 {@code view.SoundPlayer}，改素材不用动模型。
 * 名称一律用"事件"而不是"素材"（{@code ENEMY_HIT} 而不是 {@code BIT_OGG}），
 * 将来换素材时这份枚举不用跟着改。</p>
 */
public enum SoundEvent {

    /** 菜单 / 暂停 / 结算界面按下按钮。纯界面事件，由 View 自己发，不经过模型。 */
    MENU_CLICK,

    /** 玩家子弹打中敌机，但这一下没打死它。同一下打死则改发 {@link #ENEMY_DESTROYED}，两者互斥。 */
    ENEMY_HIT,

    /** 敌机被玩家子弹击毁。炸弹清屏不算击毁（那是清场），不发本事件。 */
    ENEMY_DESTROYED,

    /** 拾取道具（四种道具都算）。 */
    ITEM_PICKUP,

    /** 全屏炸弹生效、冲击波出现。与拾取音可能同帧相邻（冲击波在下一帧才结算），不是同一件事。 */
    BOMB,

    /** 敌弹命中玩家并实际扣血。护盾挡下的那一下改发 {@link #SHIELD_BLOCK}，无敌帧内被忽略则一声不响。 */
    PLAYER_HIT,

    /** 机体相撞、玩家实际扣血。护盾/无敌帧的判定同 {@link #PLAYER_HIT}。 */
    PLAYER_CRASH,

    /** 护盾替玩家挡下一次伤害（盾被消耗）。 */
    SHIELD_BLOCK,

    /** 关卡提升。一次大额加分（如炸弹清屏）连跳两级也只发一声。 */
    LEVEL_UP,

    /** 通关。 */
    VICTORY,

    /** 阵亡。 */
    DEFEAT
}
