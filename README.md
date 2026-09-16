# 飞机大战

基于 JavaFX 的 2D 竖版单人飞行射击桌面游戏（中南大学 Java 实训课程小组作业）。

## 环境要求

| 项 | 版本 |
| --- | --- |
| JDK | 17 |
| Maven | 3.x |
| JavaFX | 17.0.8（`org.openjfx:javafx-controls` / `javafx-fxml`） |
| 测试 | JUnit 5.10.2 + maven-surefire-plugin 3.2.5 + JaCoCo 0.8.12 |

## 运行

```bash
mvn javafx:run
```

入口类：`cn.edu.csu.plane.PlaneApp`（在 `pom.xml` 中通过 `javafx-maven-plugin` 的 `mainClass` 配置，以 module-path 方式启动）。

> **工作目录必须是项目根目录。** 贴图存放在根目录的 `resource/pictures/` 下（**不在** `src/main/resources`），
> `AssetLoader` 按**相对路径**读取。从别处启动会找不到贴图（控制台打印"找不到贴图"，画面退回占位画法）。
> 用 `mvn javafx:run` 或 IDE 直接运行（IDE 默认工作目录为项目根）均为正确用法。

操作：方向键或 WASD 移动，战机自动开火；`Esc` 暂停/恢复（窗口失焦也会自动暂停）。

## 构建与测试

```bash
mvn clean test        # 编译 + 运行全部单元测试 + 生成 JaCoCo 覆盖率报告
mvn clean package     # 打包
```

单元测试覆盖 model / util / controller 与可脱离界面的 view 部分，当前全部通过。

覆盖率报告生成在 `target/site/jacoco/index.html`。测试规模与逐包覆盖率的**具体数字以该报告和
`docs/markdown/测试报告.md` 为准**，本文件不再重复，以免与工具输出脱节。

（`view` 层覆盖率天然偏低——JavaFX 渲染路径难以脱离界面纯单测，已通过运行联调补验。）

## 打包与发行

```bash
mvn clean package
```

除覆盖率报告外产出两样东西：

| 产物 | 说明 |
| --- | --- |
| `target/plane-1.0-SNAPSHOT.jar` | **可运行 fat jar**：JavaFX（含当前平台原生库）与主程序已并进同一个 jar，`java -jar` 直接启动 |
| `target/plane-1.0-SNAPSHOT-dist.zip` | **发行包**：解压即玩，内含 `plane.jar`、`resource/` 与 `run.bat` |

发行包解压后**双击 `run.bat`** 即可。解压目录结构：

```
plane-1.0-SNAPSHOT/
├── plane.jar      可运行 jar
├── resource/      贴图与音效
└── run.bat        双击启动
```

> **工作目录这事在发行包里同样成立**：`resource/` 必须与 `plane.jar` 同级。`run.bat` 内已用
> `cd /d "%~dp0"` 切到自身所在目录，所以双击就对；若手动敲 `java -jar`，务必先 `cd` 到解压目录，
> 否则会退回占位画法并静音（控制台可见"找不到贴图"）。运行期生成的 `highscore*.txt` /
> `besttime*.txt` 也落在该目录。

> **产物绑定构建平台**：JavaFX 原生库按构建平台打进 jar，在 Windows 上构建的包只能在 Windows 上跑；
> 要出其他平台的包必须在该平台上构建。运行需已装 JDK 17（`run.bat` 会先检查并提示）。

## 持续集成

[`.github/workflows/build.yml`](.github/workflows/build.yml)：推送到 `master`/`main`、提 PR
或手动触发时，自动执行 `mvn -B clean verify`，并把**发行包**、JaCoCo 覆盖率报告与 surefire
用例报告作为构建产物上传（在 Actions 页面下载）。

跑在 `windows-latest` 上，两个原因缺一不可：`GameControllerTest` / `MainMenuViewTest` 要起
JavaFX 图形环境（Windows runner 自带桌面会话，免去额外架 Xvfb）；且发行包必须建在目标平台上。

## 当前实现状态

游戏可完整游玩一局（主菜单 → 战斗 → 通关/阵亡 → 重开或回菜单）。要点：

- **战斗**：方向键/WASD 移动，战机自动连发；四类敌机（普通/横移/射击/自爆）、矩形碰撞结算与计分；
  玩家有血量、受击无敌帧与护盾。
- **成长与道具**：关卡按累计得分推进（每 1000 分 1 关，上限 10 关，10000 分通关）；四种道具——
  火力强化（最多五连发）、全屏炸弹、护盾、回血。
- **难度**：四档——简单 / 普通 / 困难，外加在主菜单输入暗号 `kskbl` 解锁的隐藏档「折磨」及其「作弊」开关。
- **计时与记录**：本局计时（暂停不计时）与通关最短用时（按难度分档持久化）；最高分持久化。
- **表现**：战斗信息带（第 N 关 / 经验条 / 用时 / 十颗生命心 / 火力图标）、关卡过渡横幅、受击与爆炸特效、
  炸弹冲击波、滚动背景；主菜单提供换肤、难度选择与最快通关记录。

数值与开关统一放在 `src/main/resources/config.properties`，改完重启即生效；写错或删项会退回内置默认值。
运行时在工作目录生成存档 `highscore*.txt` 与 `besttime*.txt`（已加入 `.gitignore`）。

> 实现与需求的**逐项偏差**（含尚未落地的部分）登记在《需求规格说明书》附录 E/F/G，不在此重复以免过期；
> 遇有出入**以代码现状为准**。

## 项目结构

```
plane/
├── pom.xml                                  Maven 配置与依赖
├── LICENSE                                  MIT 许可证（第三方素材除外，见文末）
├── README.md
├── resource/                                资源文件（图片、音效等；均为第三方素材，见文末声明）
├── src/main/java/cn/edu/csu/plane/
│   ├── PlaneApp.java                        应用入口，装配 model/view/controller 三层
│   ├── Launcher.java                        fat jar 启动入口，仅转发给 PlaneApp（原因见类注释）
│   ├── controller/                          主控与帧循环、键盘输入
│   ├── model/                               游戏规则与状态（零 JavaFX 依赖，可脱离界面单独运行）
│   ├── util/                                配置读取、难度枚举、素材加载
│   └── view/                                画布渲染、HUD 与各界面
├── src/main/resources/config.properties     全部可调数值
├── src/assembly/                            发行包组装描述（dist.xml）与启动脚本（run.bat）
├── src/test/java/cn/edu/csu/plane/          23 个测试类（model 层零界面依赖，可纯单测）
├── .github/workflows/build.yml              CI：构建 + 测试 + 出包
└── docs/                                    需求/设计/测试/答辩文档与图表源文件
```

## 文档

| 文档 | 说明 |
| --- | --- |
| `docs/markdown/需求规格说明书.md` | 功能清单（F01–F26）、GWT 验收标准、非功能需求 |
| `docs/markdown/概要设计说明书.md`、`详细设计说明书.md` | 分层架构、类与接口设计、状态机 |
| `docs/markdown/测试报告.md` | 测试规模与 JaCoCo 覆盖率 |
| `docs/markdown/优化报告.md` | 迭代优化项汇总与验证方式 |
| `docs/markdown/AI使用与核对说明.md` | AI 生成内容清单与核对过程（答辩依据） |
| `docs/markdown/QA问题表*.md` | 需求澄清与设计评审的问答记录（历史存档） |
| `docs/markdown/requirements.md` | 早期需求分析稿（已被《需求规格说明书》取代，留档） |
| `docs/drawio/`、`docs/puml/` | 用例图、活动图、时序图、类图、状态机的源文件与导出图 |

> 文档与代码存在已知的同步滞后（尤其《详细设计说明书》正文仍是早期基线），
> 各处偏差已在《需求规格说明书》附录 E/F/G 逐条登记；**以代码现状为准**。

## AI 使用与核对声明

本项目开发过程中使用了 AI 编程助手辅助（需求与设计文档、代码、测试、文档校订）。

AI 仅用于**起草与辅助**，不替代理解：生成内容均经人工逐段阅读、修改与补充注释，并通过编码规范走查、
`mvn clean test` 与 JaCoCo 覆盖率核验后采纳；文档中的数字只取自工具真实输出。

逐项清单、核对过程与证据见 **`docs/markdown/AI使用与核对说明.md`**。

本人对代码的正确性与可运行性负责。

## 许可证

本项目采用 **MIT 许可证**，全文见 [LICENSE](LICENSE)。

### 第三方素材声明

**本项目包含从网络搜集的第三方素材（图片、音效等），这些素材不归本项目作者所有，也不适用上述 MIT 许可证。**

- **涉及范围**：`resource/` 目录下的**全部资源文件**——图片、音效及其他媒体资源均属此列
  （当前实际存在的是 `resource/pictures/` 下的图片；日后新增的音效等资源同样受本声明覆盖）。
- **权利归属与使用限制**：这些素材的版权归各自原作者或权利人所有。本项目仅将它们用于**课程学习与演示**，
  对素材本身**不主张任何权利，也未获得任何形式的授权或再分发许可**；
  **MIT 许可证的授权范围不覆盖这些素材**。
- 若需复制、再分发、公开部署或商用本项目，请自行核实每个素材的许可条款并取得原作者授权，
  或替换为可自由使用的素材。
- 本项目**未记录**这些素材的原始出处与作者，因此无法代为确认其许可状态。

除 `resource/` 下的第三方素材外，本项目其余源码与文档均按 MIT 许可证授权。
