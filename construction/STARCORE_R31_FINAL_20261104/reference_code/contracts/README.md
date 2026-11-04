# R31 可执行参考合同与故障注入

**验收范围：`REFERENCE_CONTRACT_ONLY`。本目录是施工方案中的独立合同代码，尚未接入 Android 生产代码，未编译 APK，未执行目标手机测试，未验证真实开奖优势。30 项测试通过只能证明这些参考合同在本次 Python/SQLite 环境中的指定行为。**

## 复跑

在总包根目录执行：

```bash
python3 reference_code/contracts/test_contracts.py
python3 reference_code/contracts/run_examples.py
```

也可以进入本目录执行 `python3 test_contracts.py` 与 `python3 run_examples.py`。仅使用 Python 3 标准库，不下载依赖。测试在临时目录创建自己的数据库并清理，不读写软件原数据库、其他审计目录或公共库源码。

- `schema.sql`：隔离的参考 schema。表名刻意不等同于生产表，不可在用户设备直接执行。
- `contracts.py`：任务领取、冻结/结算/outbox、引用闭包/预算、有界队列缓存。
- `test_contracts.py`：并发、故障、幂等、PIT、生命周期行为测试。
- `test_results.json`：本次真实执行结果及环境版本。
- `run_examples.py` / `demonstration_results.json`：逻辑膨胀与有界 checkpoint 生命周期的可复跑对照。
- `integration_map.json`：接入文件、必须保留的边界和后续测试。

## 1. 任务领取、租约与错误分类

一笔 `BEGIN IMMEDIATE` 事务内读取任务，检查状态和租约，使用 `WHERE fence=旧值` 更新 owner 与递增 fence。提交结果时再次核对 `owner/fence/boot_id/state/lease`，旧 worker 即使还在计算也不能提交。用户停止同时递增 fence，因此停止不是只设置线程布尔量。

任务提交事件主键为 `(task_id,event_key)`。相同事件、相同 payload hash 和 cursor 重送返回 `ALREADY_COMMITTED`；同主键不同内容必须拒绝。幂等的是**同一已经提交的事实**，不允许旧 worker 用新的 event key 绕过 fence。

`BusyWait`、`DependencyWait` 和取消不累加缺陷计数；SQL 语法错误不会仅因 `OperationalError` 类型就被当成繁忙。SQLite 错误按原生 code 的基础码识别 BUSY/LOCKED。生产 Android 适配必须沿异常 cause 链识别真实 SQLite 异常，不能只匹配外层 `F01_WRITE_FAILED` 字符串。

真实缺陷在固定错误窗口触发隔离，冷却到期仅放行一个 half-open probe。参考策略为 10 分钟 3 次缺陷，冷却 60 秒起指数退避，累计 3 轮进入 `NEEDS_FIX`；这些是**参考参数**，最终参数由主方案和真机试验确认，不能将测试阈值当成已验证最优值。成功的提交清除当前连续错误，但不会伪造业务产物。取消异常重新抛出；即使记录取消回执的写者繁忙，也优先传播取消。

租约计时使用 `(boot_id,elapsed_ms)`，不使用墙上时间、时区或夏令时。重启更换 boot_id 后旧单调时钟值失效；新 owner 取得更大 fence。生产 Java 应使用启动世代标识和 `SystemClock.elapsedRealtime()`，并把“用户启用”“schema就绪”“owner仍有效”作为同一事务条件。不要拿训练 work_cursor 与日历 ordinal 相减估计积压。

## 2. 赛前冻结、结算与反馈

同 `(stream,issue,model_hash)` 只能冻结一次。即使开奖前新模型结果晚到，也不覆盖原预测；若产品确实允许多次预提交，须另建版本记录并在开奖前单次封存选择，不能事后挑最好版本。

冻结合同拒绝：特征最大 ordinal ≥ 目标 ordinal、非固定三个不同号码、非法位置、已观察到目标开奖结果、已过冻结截止。LIVE/REPLAY 必须分别保存、分别统计。参考 position 为 0–9，生产若使用 1–10，适配器必须只转换一次并测试首末位置。

**提交前读到的时间不等于持久提交时间。** 本合同保存提交前时间，在 SQLite commit 返回后再读同一 boot 的单调时钟，形成提交时刻的保守包围区间。只有提交回执上界严格早于 deadline 才允许 `PREDRAW`。提交跨截止写 `LATE_OR_UNKNOWN`；预测提交后进程被杀、尚无回执，保留 `UNKNOWN`，重试不能用较晚时间补造赛前资格。该保守设计会牺牲一部分无法证明的覆盖率，但不会把开奖后记录混入赛前证据。

实际 app 需将服务端开奖时间映射成保守本机 deadline，携带服务器时间偏差/不确定度。期限必须减去可证实的误差边界。只有本机时间而没有可信时间校正时，报告应声明时点证据不足，不将时间戳排序当成绝对证明。

结算与 `feedback_outbox` 同事务提交；结算后、outbox前失败，两者一起回滚。重复开奖/重复结算不会增加样本。开奖结果同 issue 不同 hash 进入显式修订冲突，不能 `REPLACE` 原记录。

消费者使用 `(consumer_id,event_id)` 幂等：模型 checkpoint 的小写入与 consumed 标记同事务。**昂贵训练不得放在此事务内**；应在事务外读取冻结输入与旧模型版本计算，再以 owner/fence、旧模型 hash、输入水位 CAS 发布新 checkpoint，记录已应用事件。测试中的计数器只用于验证事务和重送语义，不表示实现了学习模型。

## 3. 有用资产保留与预算回收

预算按**现有对象 + 尚未释放的写入预留 + 待删除但物理删除未成功的对象**计数。新报告或 checkpoint 必须先预留旧、新、临时文件同时存在时的峰值成本；检查物理可用空间与紧急保留空间后才允许写入。预算不足时拒绝可推迟的新增工作，不能为了让数字达标删除活 pin、有效证书或未结算预测依赖。

根引用包括活训练 pin、有效证书、未结算冻结、当前模型与明确需要保留的报告/审计证据；沿依赖边求闭包，所有可达对象保留。`UNION` 防止共享引用和环使递归不终止。这里只演示算法及原子状态，不按名字、年龄或“空表”猜是否有用。

新 checkpoint 经独立文件校验/原子发布后，**在同一 SQL 事务里登记新对象并切换当前根指针**，旧对象随后由 GC 判断；切换失败时旧根与写入预留均保持。文件发布/目录 fsync/启动恢复由生产文件 owner 实现，本目录只提供目录账本合同。

GC 分为：标记不可达为 `DELETE_PENDING` → 事务外尝试物理删除 → 成功后删除目录项并写回执。标记后禁止新根或边指向待删除对象。删除失败仍占预算；崩溃后重试删除必须幂等。**文件只是不在数据库中引用，但尚有未提交文件发布日志/外部句柄时，生产实现必须先把它登记为根。** 不允许 GC 与发布者之间有无保护窗口。

`mark_unreachable` 在本参考规模内用单事务证明图的一致性；它**不是大库实时热路径的性能实现**。Android 接入时应使用固定图 revision 的 mark generation、分页 edge frontier、可中断检查点、短写事务和 generation fencing 完成标记，再分批 sweep。边和根发生变化必须记录写屏障或使该轮失效；不得简单把本方法的单事务拆成任意批次后继续删除。`sweep` 已按最多 128 个对象/次获取工作，文件 I/O 不在写事务内。所有 DDL 只在安装/迁移/专属连接启动时执行，不能在保留循环里 CREATE/DROP/ALTER。

`release_root` 仅是 domain owner 明确确认消费结束/证书撤销/结算结束后的接口；不可根据墙上时间过期直接释放证书或未结算根。目录账本里的 `bytes` 是**传入的已测量字节**，不是 SQLite 文件实际占用；生产须另测 DB/WAL/SHM、索引、已发布文件、临时文件和空闲页。本合同不能证明 DELETE 后 SQLite 文件自动缩小，也不执行 VACUUM。

## 4. 有界内存结构

`BoundedCoalescingQueue` 同时限制 item 数和 key/value 字节；满时返回拒绝，不能静默删最旧关键事件。同 key 允许合并最新诊断，但结算/训练事件不可丢弃，应以持久 outbox 承载。`BoundedCache` 使用 LRU，活 pin 不可淘汰，无法腾出空间时拒绝新缓存。

这两个结构是**单 owner 线程**的逻辑参考，不是多线程安全容器；Android 版必须在统一 owner 或 synchronized 区域内调用。Python 对象头与容器开销不在 key/value 字节中，生产需要实际 heap/PSS采样和对象开销余量，不能据此声称精确堆上限。

## 5. 已执行测试与尚未执行的验收

已执行 30 项行为测试，含两连接争锁、两个线程竞争领取、子进程 `os._exit` 崩溃、过期 fence、用户停止、取消回执繁忙、跨截止提交、时钟重启、缺失提交回执、同 issue 不同开奖结果、重复结算与反馈、GC引用环、低磁盘拒绝、删除失败重试、checkpoint 200 次更新回收，以及搬家式 keep 表膨胀复现。

尚未执行：Android Java适配、现有 schema迁移、全工程编译、原数据库升级、真机后台/熄屏/网络波动、主线程时延、72小时存储浸泡、完整实盘预测、未来样本质量和晋升。所有这些必须由对应施工卡的独立回执关闭。

SQLite 语法兼容性：本机运行的 SQLite 版本记录于结果 JSON。`ON CONFLICT ... DO UPDATE` 等语法需检查目标 Android SQLite 版本；旧系统适配可在同一 writer事务内使用 `INSERT OR IGNORE` + 有条件 `UPDATE`，不能未经兼容检查直接移植本 SQL。生产所有写入必须经现有 `DatabaseCoordinator`，禁止为参考 SQL 私建第二个不受控写者。

## 6. 为什么不写“百分百软件通过”

测试是有限输入空间内的可复验行为证据。它能让施工人员明确哪些断点必须保持原子性，不能替代真实 app 集成，也不能证明不存在其他缺陷。最终施工包应分别呈现合同测试通过、生产迁移通过、整机通过、自然质量通过四个状态，缺失回执保持 `NOT_RUN` 或 `UNKNOWN`。
