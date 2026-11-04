这是只读 HOST 工程反例，不是自然策略质量、Android 设备或持续运行验收。

使用公开 `leaf("family", position, familyIndex, 0)` 接口，从 seed=31 的合法排列合成数据取 250 行前缀，再通过真实 `ReplayDatasetR27.append` 加入第 251 行。输入只读，没有改动开奖、标签或数据角色。分别验证 F08、F11、F12、F20、F21。

五个家族均复现同一问题：旧缓存先读取位置 1，子缓存调用 `extendFrom(older)` 后，其 `FamilyStream`、`model`、`st`、`p`、`values`、`lastFedDraw`、`verifiedData` 与旧缓存为同一对象。仅读取子缓存便使旧流 `fed` 从 249 变成 250，序列化模型 SHA256 改变，`values.length` 从 25000 变成 25100，`verifiedData` 改为子缓存的数据集。旧缓存自身的 `data` 仍指向 250 行输入。

旧缓存再次读取尚未缓存的位置 2，会经过真实生产 `family` → `FamilyStream.advance`，记录 `R27_FAMILY_STREAM_SHRUNK` 并重建一次。错误被生产 `family` 的既有恢复分支捕获，因此公开调用没有外抛异常栈；原始重建事件、原始数据集 hash、游标、对象身份和模型状态 hash 均保存在 `RESULTS.json`。五次重建中没有输入前缀改写。

全部 5 家族 × 10 位置的新旧缓存，合计 250500 个 double，与各自独立全量重建按 `Double.doubleToLongBits` 比较，差异为零。本反例证明可变模型状态跨 owner 共享和真实额外重建，尚未证明预测数值错误。已经缓存的位置 1 会掩盖该问题，因为其原数组仍直接命中。

实际编译支持的 `BuildConfig` 是 e056206af83a8fc094b81d97f67a1310b8cc2e46、dirty=true；它不是 2ab clean APK。父任务确认 2ab 只补身份生成输出和证据，本探针另核实 e056→2ab 的 `app/src/main`、`app/build.gradle` 差异为空，并记录 14 个被测生产源与 2ab git blob/SHA256 对应关系。运行前复制实际 runtime classes jar、Android HOST mock 和 R.jar，保存原件及副本 SHA256；未运行 Gradle，也未编译生产覆盖类。

`RECEIPT.json` 保存完整 javac/java 命令和退出码。执行前首次定位 javac 使用了不存在的候选路径，启动失败为 `FileNotFoundError: javac`，未执行测试；随后使用环境既有 `/workspace/starcore-environment/tools/jdk-17.0.17+10/bin/javac` 编译探针成功。最终探针 exit=0 只表示测量完成，`SUMMARY.json` 的生产隔离门仍为 FAILED。

原有 52 项 golden 指纹文件以及 `golden_512.bin` 前后 SHA256 不变。本任务只核对文件完整性，没有重复执行或替换已有 golden 验收。生产、测试和构建文件均未修改。

已向 FeatureCache 单一负责人建议：修改 `extendFrom` 和 `FamilyStream`，在源流同步域内建立独立模型快照，使用实际 `model.write/read` 复制训练状态，并复制 `fed`、`lastFedOrdinal`、`lastFedDraw`、`values`、`p`、`st`、`checkedFed`、`checkedHash` 等自有状态。避免把源 `FamilyStream` 本身放进另一个缓存。不可变输入可按既有合同引用；`verifiedData` 的每个流引用槽必须独立。随后需复测单向增量、前缀改写、旧缓存继续消费、分叉子缓存和并发读；不能以删除增量能力或只加共享锁代替状态隔离。
