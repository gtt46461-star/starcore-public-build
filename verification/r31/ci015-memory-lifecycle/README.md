# CI015 Context、资源所有权与 H15 检测器修复

源码提交 `fac317133a9b821a7342d9fb8cf981c2732147bf`，施工基线 `4e395662838a69d3b784eee8f05af11fa94460ac`。本子项源码完成；完整工程回归仍失败，没有宣布批次闭环，也没有产出 APK。

CI015 原始运行是 [41946575566](https://github.com/gtt46461-star/starcore-public-build/actions/runs/41946575566)，产品 checkout 为 `01c65875f294362dbc6f7fce39ea4d103b43eefa`，workflow checkout 为 `6ce27b8`。按 Root 授权采用后续 `4e` 修复树。两者之间保留 AtomNetRuntimeR30、AtomNetSessionR30、ReportSnapshotR30 三份有效后续生产修改，差异和哈希见 `INHERITED_SOURCE_DELTA.json`；不能把本地 4e 复跑称作同一 CI 源码复跑。

| 实际检查 | 结果 | 证据 |
|---|---|---|
| CI015 原始记录 | 485 项 / 8 失败；两个 StaticFieldLeak 加一个 UsableSpace | CI015_BUILD.raw.log |
| 本地原 4e 完整 Gradle | 485 项 / 2 失败，无跳过 | full-before/EXIT.json |
| Context + 三类 fixture 修复后完整 Gradle | 485 项 / 13 失败，无跳过 | full-after-before-h15/EXIT.json |
| 实际 Debug、Release lint | 两个 Context 泄漏均消失；各剩一个其他 owner 的 ModelPublication UsableSpace | full-after-before-h15/lint-results-*.txt |
| 原件资产、VM、模型重载边界 | 原源码确切失败；修复后 14 断言通过 | teacher-boundary/RESULTS.json |
| 52 项原黄金指纹 | 前后逐项完全相同 | teacher-boundary/RESULTS.json |
| H15 最小修复后的实际 Gradle | 原保护检查 1 项通过 | h15-target/EXIT.json |
| 实际 H15 helper 的长文本/等价负控 | 5015 断言通过；原 regex 真实 StackOverflow；5000 短边界输出相同 | h15-equivalence/RESULTS.json |

两轮完整测试均为原 485 项、一个默认 `-Xmx512m` Gradle worker。 原 4e 套件完成编译后才修改 Context 和三个 fixture 的源文件文本；运行中的 4632 个 class 未再编译或改动，`full-before/RUNNING_BYTECODE_STABILITY.json` 记录逐文件零变化，原源码也已在编译时封存。因此该 before 证据绑定原编译字节，不能声称其整个运行期间工作树文本一直干净。没有扩大堆、栈、预算或阈值，没有按类拆分完整套件，没有强制 GC、跳过或降低原断言。四个原测试文件的 245 条 assert/fail 调用内容逐字保留，见 `FIXTURE_ASSERTION_PRESERVATION.json`。独立资产和 scanner fixture 只证明各自子项，不替代完整 485 项。

生产修改仅把 LegacyTeacherOwner 的长期静态 Context 改为强引用的应用 AssetManager。调用方 Context 不再被保留，资产管理器保持可重载供应；每次文件流仍独立关闭，应用的 AssetManager 不由此 owner 关闭。原 125 资产、选择、VM、训练时点、参数、模型数学和资格口径均未改变。HOST 资产适配器用真实 FileInputStream 读取仓库原件；调用方 Context 在确定性生命周期边界停止提供服务后，原源码重载失败，修复源码仍从相同管理器完成重载，VM 输出及训练 codec 同原件。

三类旧 fixture 的 loop、cache、替换掉的 EvalContext 和独立确认视图现在明确关闭。H15 的互斥字符串分支使用 possessive repetition，保持相同匹配输出并消除递归栈增长。所有 owner 哈希、禁用调用检测和原 PnL/K3 负控保留。

完整回归失败没有被修饰：原 4e 在 h10/h11 发生 DECLARED_CAPACITY；修复后旧 H15 冷执行出现 StackOverflow，另有 12 次真实内存等待。最终 H15 子项通过后未再次把完整 485 写为通过。源码 MemoryBudgetR31、FeatureCacheR27 保持原 4e 字节。

只读 JDK attach observer 没有转换类、替换 probe 或关闭 owner。它记录真实进程现有预算和 lease；observer 自身也会少量分配，收据注明观测范围。原失败时观察到 heap=332590896、pending=73984、MODEL 请求 28672；修复后另见 heap=413378984、pending=100144，随后堆观测降低。实测余量不足，不能把合法等待改为通行。lease 是已登记所有者记录，不能据此推断完整可达堆、垃圾量或实际释放字节，也未将堆降低归因于某次回收。详细原样 JSONL 无损压缩，解压 SHA256 见 `LOSSLESS_COMPRESSION.json`。

最初沙箱运行因 Gradle 无可用 wildcard IP 失败，获准的相同本地命令随后执行。初次复用冻结 classes 且排除 javac 的尝试另遇 AGP provider 与旧 R967 约束，unit 未运行；这些原错保留在 baseline-gates。后续正常源码编译、完整 unit 与 lint 使用明确列出的开发身份门排除，未用于 APK 或最终不排除门验收。最终源码身份重绑及签名统一交付由 Root 集成。

设备、24/72 小时和自然未来质量分别为 NOT_RUN / NOT_EVALUATED；它们不阻止本子项源码交付，不能用本 HOST 证据替代。数据库为 NO_NEW_SQL_SCHEMA，应用身份、版本、签名未修改。剩余工作是 Root 集成后处理完整资源等待验收、其他 owner 的容量 lint 及最终统一构建。
