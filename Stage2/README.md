# AI Used-Car Evaluation Assistant — Stage 2 Foundation

这是 EECS3311 Fall 2026 项目的**初始脚手架**。目前可以通过 CLI 和 Swing GUI 执行真实的车辆录入、会话偏好保存、费用估算和预算/里程检查；不是完整的 Stage 2 应用，也没有接通 Gemini。

本次实际工作区为 `C:\Users\yixin\Desktop\eecs3311`，课程项目位于其中的 `project`，本目录是现有 `project\Stage2`。原有 `Project-Stage2-Instruction.pdf` 已保留。没有修改 `Stage1`、根目录 README 或 Git 配置。

## 前置条件

- Windows PowerShell 5.1 或 PowerShell 7，Java **21 JDK**（不仅是 JRE）。
- GUI 需要 Windows 桌面显示环境。
- 日常编译、CLI、GUI 均不需要 Maven、网络或 API key。
- 验证脚本优先使用已安装 VS Code Java 扩展中的 JUnit 4.13.2 与 Hamcrest；没有这些库时会明确改用无依赖验证器。

脚本依次检查当前进程的 `STAGE2_JAVA_HOME`、`JAVA_HOME`、PATH 上的 javac 以及常见 JDK 安装位置，选择 Java 21。不会修改系统环境。当前机器 PATH 中的 `java` 是 Java 8，但脚本已经验证可定位本机 Adoptium Java 21。

## 在 Windows VS Code 中运行

打开本 Stage2 文件夹，或在 VS Code 终端切换到这里：

```powershell
cd 'C:\Users\yixin\Desktop\eecs3311\project\Stage2'
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\build.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode demo
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode cli
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\run.ps1 -Mode gui
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\verify.ps1
```

`-ExecutionPolicy Bypass` 仅作用于新启动的 PowerShell 进程，不修改机器执行策略。已有允许执行脚本的终端可直接使用 `& .\scripts\build.ps1` 等命令。上述路径只是当前机器示例，脚本本身不依赖固定盘符或启动目录。

GUI 中先点击 **Load fictional example**，检查 Vehicle、Preferences、Cost assumptions 三个页签，再点击 **Save and calculate**；也可以直接输入自己的数据。结果区支持滚动；错误会显示在同一区域。改变输入后需要重新点击计算。

CLI 输入 `prepare` 按提示录入/替换当前车辆，输入 `:cancel` 可取消；`demo` 使用虚构数据；`show` 显示已保存的当前车辆和偏好；`status` 显示 LLM 状态；`help` 查看命令；`quit` 退出。全部会话信息仅存内存。

修改源码后重新运行 `build.ps1`。`run.ps1` 仅在 JAR 不存在时自动构建，不会自动检测源码改动。

若脚本无法找到 JDK，可只在当前终端指定自己已安装的 JDK 路径：

```powershell
$env:STAGE2_JAVA_HOME = '你的 JDK 21 安装目录'
& .\scripts\build.ps1
```

## 示例输出及计算约定

示例来自 `src/main/resources/example.properties`，车辆品牌和型号均为虚构。示例税率、燃油价、保险和维护费用都是演示输入，不代表当前市场报价或实际税务规则。

- 年耗油量：15,000 × 7.2 ÷ 100 = **1,080 L**。
- 年燃油费：1,080 × 1.65 = **CAD 1,782.00**。
- 购车税额：18,000 × 0.13 = **CAD 2,340.00**。
- 简单首年估算：18,000 + 2,340 + 1,782 + 1,000 + 1,800 = **CAD 24,922.00**。
- 报价 CAD 18,000 超过预算 CAD 17,000，产生明确预算冲突。

费用采用 BigDecimal；货币分项以 HALF_UP 舍入到分，首年估算累加已舍入分项。缺少油耗时燃油结果不可用；缺少任一税率/维护/保险假设时不生成完整首年总额。明确输入 0 与留空不同。未列入的融资、折旧、登记等费用不计入。

## 项目结构

```text
Stage2/
  pom.xml, README.md, .gitignore, .env.example
  Project-Stage2-Instruction.pdf          原有文件
  src/main/java/ca/yorku/eecs3311/usedcar/
    app/ agent/ command/ domain/ llm/ result/ tool/ ui/
  src/main/resources/example.properties
  src/test/java/ca/yorku/eecs3311/usedcar/
  scripts/common.ps1, build.ps1, run.ps1, verify.ps1
  docs/                                 设计、追踪、状态、日志、后续计划
  build/                                被忽略的编译/验证/文档检查产物
```

主入口为 `ca.yorku.eecs3311.usedcar.app.Main`，支持 `--demo`、`--cli`、`--gui`、`--help`。构建后也可用 **Java 21** 直接执行：

```powershell
java -jar .\build\used-car-assistant.jar --demo
```

这条命令使用 PATH 上的 Java；当前机器默认 Java 8，建议优先使用上述脚本。

## 验证

```powershell
& .\scripts\verify.ps1                  # 优先 JUnit；找不到则明确使用离线检查
& .\scripts\verify.ps1 -DependencyFree  # 强制无依赖检查
& .\scripts\verify.ps1 -Gui             # 额外启动真实窗口，测试按钮，再自动关闭
```

10 个测试覆盖负数/非法输入、必填项/年份、精确计算、可选值、未知油耗、约束边界、零值/舍入、拒绝更新后的会话、Tool/LLM 边界、交互式 CLI。GUI smoke 检查启动、计算、错误显示和重新计算，并把本应用面板图保存到 `build/verification/gui.png`。这不是完整 Stage 3 测试，也没有运行 KUMA。

## 可选 Maven 路径

`pom.xml` 配置 Java 21、JUnit 4.13.2 和可执行 JAR。当前环境没有可调用的 Maven CLI，因此本次没有验证 Maven 构建。若你已有 Maven，使用 Java 21，并将本地依赖缓存限制在 Stage2：

```powershell
mvn "-Dmaven.repo.local=$PWD\build\maven-repository" test package
java -jar .\target\used-car-assistant-0.1.0-SNAPSHOT.jar --demo
```

首次 Maven 构建可能需要下载声明的插件/测试依赖。本次任务没有执行下载，也没有安装 Maven。Maven 的 `test` 使用 `FoundationTest`；GUI smoke 是显式本地检查，不自动加入 Maven 测试。

## Gemini 配置与当前状态

当前 `LLMClient` 是抽象边界，`GeminiAdapter` 是明确不可用的架构骨架：`generate()` 抛出带 PLANNED 提示的异常。没有 HTTP 实现，不读取任何 API key，不加载 `.env`，不返回模拟 AI 答案。填入 key **不会**使本版本的 AI 功能可用。

未来实现适配器和配置加载后，才在本目录复制 `.env.example` 为 `.env` 并仅在本地填写 key。忽略规则已覆盖 `.env`、`.env.*`、`config.local.json`，保留占位示例。不要把真实 key 写入源码、截图或日志。本轮无需创建或提供 key。

## 实现边界

F01/F03/F04/F06/F07 均只完成明确限定的初始子集；F02/F05/F08/F09/F10/F11/F12 为 PLANNED。只有 Facade 与 Command 已有工作流证明；Adapter 仅有接口骨架，Strategy/Factory 尚在规划。没有可靠性评分、购买建议、知识库检索、磁盘持久化或完整报告功能。

详见 [实施状态](docs/IMPLEMENTATION_STATUS.md)、[Stage 1 追踪表](docs/STAGE1_TRACEABILITY.md)、[设计差异](docs/DESIGN_DECISIONS.md)、[开发与验证记录](docs/DEVELOPMENT_LOG.md) 和 [下一步](docs/NEXT_STEPS.md)。
