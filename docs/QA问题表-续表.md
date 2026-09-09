# QA 问题表（飞机大战 · Day03 类与接口布局评审）

- 项目：飞机大战（JavaFX 单人桌面版）
- 日期：2026-09-09
- 提问人：廖尉钧，孙浩凡，孙嘉晟，艾奕舟，霍睿深
- 说明：本表为 Day02 需求澄清（Q1–Q8）落地到"类与接口布局"后的**设计评审续表**。顶部为批判性研读类布局源码与 UML 图（类图/数据流图/时序图）后得到的**疑点清单**（共 15 条，含矛盾/含糊/隐含三类）；下方为与"设计者"（开发组）澄清后录入的**问答闭环表**；第三节为疑点到代码/需求落点对照。

---

## 一、疑点清单（研读"类与接口布局"产出，共 15 条）

> 标记说明：❌矛盾 = 设计与已确认需求（Q1–Q8）打架或类内部自相冲突；❓含糊 = 写到了但没有可判定/可实现的语义；💤隐含 = 已确认需求明确要、但类布局里根本没落地的功能。

| 序号 | 类别 | 设计现状（代码 / 图） | 疑点（为什么必须问清楚） | 去向 |
| -- | -- | -- | -- | -- |
| 1 | ❌矛盾 | `GameConfig.DEFAULT_HEALTH = 5` | Q2 已确认"初始血量 100、敌弹 -10、撞击 -20、无敌 1 秒"，类里默认血量却写成 5，且 GameConfig 只有血量/窗口/帧率，缺敌弹伤害、撞击伤害、无敌时长等已确认数值 | Q9 |
| 2 | ❌矛盾 | `ShootingEnemy` score=300、`BomberEnemy` score=500 | Q3 已确认分值表为"普通 100 / 横移 150 / 射击 200 / 自爆 150"，射击与自爆两处分值写反，普通 100、横移 150 则一致 | Q10 |
| 3 | ❌矛盾 | `InputHandler` 保留 `shooting` 字段 + `isShooting()` | Q1 已确认"自动连发、不做手动按键发射"，`shooting` 布尔与鼠标事件绑定暗示仍要做手动发射 | Q11 |
| 4 | ❌矛盾 | `GameController(GameState, GameView)`，类图 controller 包无 GameModel | `GameModel`/`GameModelImpl` 聚合了玩家/敌机/子弹/道具，但控制器只依赖纯数据类 `GameState`，类图整段缺失 GameModel；控制器与 CollisionManager 拿不到 enemies/bullets/items 列表，架构断层 | Q12 |
| 5 | ❌矛盾 | `GameStatus.VICTORY`、`EnemyType.BOSS` 枚举存在 | Q8 已确认"本期不做 Boss"，无 Boss 则无"通关"，VICTORY/BOSS 两个枚举值何时触发、是否本期删除？ | Q13 |
| 6 | ❓含糊 | `BomberEnemy` 注释"碰撞造成高额伤害"，但类内无伤害字段 | Q2 确认机体碰撞统一扣 20，自爆机是沿用 20 还是另有"高额"数值？伤害值应在哪个类定义？ | Q14 |
| 7 | ❓含糊 | `Entity.move()` 为 `x += velX; y += velY`，不乘 deltaTime | `velX/velY` 到底是"每秒速度"还是"每帧位移"？若按帧算，帧率波动会改变实际速度，与 NF-01（60FPS）冲突 | Q15 |
| 8 | ❓含糊 | `Bullet` 构造 `super(x, y, 0, 0)` 把宽高设为 0 | 碰撞盒面积为零，后续 `checkBulletEnemy`/`checkBulletPlayer` 永远判不到命中；子弹实际尺寸应为多少、在哪统一设置？ | Q16 |
| 9 | ❓含糊 | `ShootingEnemy.movePattern()` 需发射子弹，但 Enemy/ShootingEnemy 无子弹集合引用 | 射击敌机发的敌弹如何加入 `GameModelImpl.bullets`？模型层无 GameModel/SpawnManager 引用，发射子弹的数据通路缺失 | Q17 |
| 10 | ❓含糊 | `GameState.score` 为 `long`，`GameModel.getScore()/addScore()` 为 `int`（`getScore()` 强转 `(int)`），HUD/GameOver 用 `long` | 得分类型 int/long 三处不一致，高分时强转会溢出截断，应统一为哪种类型？ | Q18 |
| 11 | ❓含糊 | `PlaneApp` 硬编码 `new Scene(root, 900, 700)`，DFD 有 `config.properties`(D3)，GameConfig 却是 static final 硬编码 | 窗口尺寸重复硬编码未复用 GameConfig；Q2/Q3 说"数值写入配置文件"，但 GameConfig 无法外部配置，配置化与硬编码相矛盾 | Q19 |
| 12 | ❓含糊 | `GameModelImpl` 中 `new Player(0, 0, 50, 50)` | 玩家初始在窗口左上角 (0,0)，时序图 1 却写"玩家位于战场底部中央"，出生点语义不一致 | Q20 |
| 13 | 💤隐含 | `GameState` 仅 status/score/level/elapsedTime，无最高分字段与读写方法 | Q7 已确认"历史最高分关机保留"、DFD 有 D2 存档，但类布局里最高分的存取完全没落地 | Q21 |
| 14 | 💤隐含 | `GameModel` 接口只有 initGame/update/movePlayer/getters/addScore | Q7 已确认"P 键暂停/恢复"、GameStatus 有 PAUSED，但模型接口没有 pause()/resume()，暂停语义只有 GameController 有，模型如何冻结更新？ | Q22 |
| 15 | 💤隐含 | `Player` 有 firePower/fireRate，但无"火力强化剩余时间"计时字段 | Q6 已确认"火力强化持续 10 秒后恢复单发"，Player 缺倒计时字段就无法到期还原；护盾/回血/炸弹的即时效果状态也未见落地 | Q23 |

> 补充疑点（登记待后续确认，不阻塞本轮）：`MainMenuView` 注释提到"排行榜、难度设置"，但 DFD 中 D2 最高分/难度设置仅在菜单读取，类布局尚未定义排行榜数据结构与难度档位枚举——待 UI 细化时一并澄清（Q24 待开）。

---

## 二、QA 问答闭环表（设计评审问题已登记，答复待开发组确认）

| 编号 | 日期 | 提问人 | 对象 | 问题 | 答复 | 状态 |
| -- | -- | -- | -- | -- | -- | -- |
| Q9 | 2026-09-09 | 廖尉钧 | 设计者 | 默认血量 5 与已确认的 100 矛盾，GameConfig 缺敌弹/撞击伤害与无敌时长等数值，怎么统一？ | 待澄清：`DEFAULT_HEALTH` 应改回 100，并按 Q2 补齐 `ENEMY_BULLET_DAMAGE=10`、`COLLISION_DAMAGE=20`、`INVINCIBLE_DURATION=1.0` 等配置项 | 待确认 |
| Q10 | 2026-09-09 | 廖尉钧 | 设计者 | 射击/自爆敌机分值 300/500 与已确认的 200/150 冲突，按哪个执行？ | 待澄清：以 Q3 为准，`ShootingEnemy` 改 200、`BomberEnemy` 改 150，保持"射击 200 / 自爆 150" | 待确认 |
| Q11 | 2026-09-09 | 廖尉钧 | 设计者 | 自动连发还需要 InputHandler 的 shooting 手动射击标记吗？ | 待澄清：按 Q1 删除 `shooting`/`isShooting()`，射击由定时器按 fireRate 驱动，输入只负责移动与暂停 | 待确认 |
| Q12 | 2026-09-09 | 廖尉钧 | 设计者 | 控制器依赖 GameState 而非 GameModel，类图也缺 GameModel，实体列表怎么到控制器？ | 待澄清：`GameController` 改持 `GameModel`（或同时持 GameModel 与 GameState），类图补上 GameModel/GameModelImpl 及其聚合关系 | 待确认 |
| Q13 | 2026-09-09 | 廖尉钧 | 设计者 | 不做 Boss，VICTORY 与 BOSS 枚举还保留吗？ | 待澄清：本期从枚举剔除 BOSS 与 VICTORY，或保留并标注为 P2 扩展占位（与 Q8 一致需明确） | 待确认 |
| Q14 | 2026-09-09 | 廖尉钧 | 设计者 | 自爆机碰撞伤害是统一 20 还是另有高额值？ | 待澄清：若沿用 Q2 则删除"高额"措辞；若特殊则需给出数值并写入配置 | 待确认 |
| Q15 | 2026-09-09 | 廖尉钧 | 设计者 | move() 不乘 deltaTime，vel 是每秒速度还是每帧位移？ | 待澄清：明确 vel 单位为"每秒"，`move()` 改为 `x += velX * deltaTime`，保证帧率无关 | 待确认 |
| Q16 | 2026-09-09 | 廖尉钧 | 设计者 | 子弹宽高为 0，碰撞盒无法命中，实际尺寸多少？ | 待澄清：给 `Bullet` 设置非零宽高（如与素材一致），或碰撞改点/射线判定并统一口径 | 待确认 |
| Q17 | 2026-09-09 | 廖尉钧 | 设计者 | 射击敌机发的子弹如何加入 bullets 集合？ | 待澄清：敌弹生成通过 `SpawnManager`/模型回调写回 `GameModelImpl.bullets`，明确数据通路 | 待确认 |
| Q18 | 2026-09-09 | 廖尉钧 | 设计者 | 得分 int/long 三处不一致，统一为哪种？ | 待澄清：统一为 `long`，去掉 `(int)` 强转，HUD/GameOver 保持 long | 待确认 |
| Q19 | 2026-09-09 | 廖尉钧 | 设计者 | 窗口尺寸硬编码，配置化与硬编码矛盾怎么处理？ | 待澄清：`PlaneApp` 复用 `GameConfig.WINDOW_WIDTH/HEIGHT`；config.properties 落地为可读配置还是暂用常量需定 | 待确认 |
| Q20 | 2026-09-09 | 廖尉钧 | 设计者 | 玩家出生点在 (0,0) 还是底部中央？ | 待澄清：`GameModelImpl` 初始化玩家为底部中央，与时序图 1 一致 | 待确认 |
| Q21 | 2026-09-09 | 廖尉钧 | 设计者 | 最高分存档在类布局里没有字段和方法，怎么落地 F13？ | 待澄清：`GameState` 增 highScore 字段，模型增 loadHighScore()/saveHighScore()，对应 D2 存档 | 待确认 |
| Q22 | 2026-09-09 | 廖尉钧 | 设计者 | GameModel 缺 pause()/resume()，暂停时模型如何冻结？ | 待澄清：`GameModel` 接口补 pause()/resume()，update 内在 PAUSED 状态跳过实体推进 | 待确认 |
| Q23 | 2026-09-09 | 廖尉钧 | 设计者 | 火力强化 10 秒倒计时没有字段承载，到期如何还原？ | 待澄清：`Player` 增 firePowerTimer 字段，update 内递减并在归零时还原单发；护盾/回血/炸弹即时效果同步补状态 | 待确认 |

---

## 三、QA → 代码/需求落地对照（疑点→改设计的闭环）

| QA 编号 | 落到代码 / 需求的位置 |
| -- | -- |
| Q9 | `GameConfig.DEFAULT_HEALTH`（5→100）并补敌弹伤害/撞击伤害/无敌时长；对应需求 Q2、F07、NF-04 |
| Q10 | `ShootingEnemy`（300→200）、`BomberEnemy`（500→150）；对应需求 Q3、F08 分值表 |
| Q11 | 删除 `InputHandler.shooting/isShooting()`；对应需求 Q1、F03 |
| Q12 | `GameController` 构造改持 `GameModel`；类图补 `GameModel/GameModelImpl` 与聚合关系 |
| Q13 | `GameStatus.VICTORY`、`EnemyType.BOSS` 本期剔除或标注 P2；对应需求 Q8、F16 |
| Q14 | `BomberEnemy` 伤害值 + `CollisionManager.checkPlayerEnemy`；对应需求 Q2、F07 |
| Q15 | `Entity.move()` 乘 deltaTime，统一 vel 语义；对应 NF-01（60FPS） |
| Q16 | `Bullet` 非零宽高；对应 `CollisionManager.checkBulletEnemy/checkBulletPlayer`、F06 |
| Q17 | `ShootingEnemy` 发弹数据通路（写回 bullets）；对应需求 F04 |
| Q18 | `GameState.score`、`GameModel.getScore/addScore`、HUD/GameOver 统一 long |
| Q19 | `PlaneApp.start()` 复用 GameConfig；DFD D3 配置化落地；对应 NF-04 |
| Q20 | `GameModelImpl` 玩家出生点改底部中央；对应时序图 1、F02 |
| Q21 | `GameState` 增 highScore + 存取方法；对应需求 Q7、F13、DFD D2 |
| Q22 | `GameModel` 补 pause()/resume()；对应需求 Q7、F12 |
| Q23 | `Player` 增 firePowerTimer；对应需求 Q6、F10 |
