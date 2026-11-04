# Evolution 写协议：生产冻结与 HOST 验证

本目录是 R31-B02-C02 的 EvolutionSink 大 batch 子范围证据，不是整卡、Android、设备持续运行或自然质量完成证明。生产实现提交为 `8a1296f72a707ad63295b753163084be4b5229b2`，基于 `de9a30834025c18ce291c241a722abe437717539`；ROOT 负责合并到公开 R31 主施工分支、startup MIGRATION、Runtime 精确接线及最终完整工程门。

## 实际修复

原始实现已经把 canonical 输出和 cursor 放在同一事务；问题是默认 128 候选产生 389 项写入、约 296 KiB bind，超出协议起始 64 logical ops / 256 KiB bind。没有把原版描述为非原子，没有削减 128 候选，也没有假定全为 BORN。真实 fixture 为 120 BORN、7 REJECTED、1 DUPLICATE_BEHAVIOR。

`StoreR27.EvolutionSink.preparePublicationR31` 进入新增 `EvolutionPublicationStoreR31`。事务外冻结 SQL、参数、source、owner/fence、旧 cursor、checkpoint/RNG 与 hash；私有 staging 每片最多 64 logical ops、bind 上限 min(256 KiB, maxHeap/512)。新增 `EvolutionPublicationSqlR31` 把有限、可证明兼容的原写入分组为 set-based canonical SQL，预先冻结行数/字节/guard/trigger/receipt 放大 envelope。最终同一短事务验证 fence/source/旧 cursor、执行原 guard 与有限 canonical 投影、发布 COMPLETE 与 checkpoint 指针。大 checkpoint 内容由有 hash 的 8 KiB 块承载；既有 canonical 表不分片曝光，正常单个 SQLite reader 不会看到半代。跨多次独立 SELECT 的 UI/report bundle 仍需事务或 publicationToken 比对，不能自行假定全程同一代。

成功后 Runtime 必须先发布已提交 loop/cache/RNG 内存状态；新 scratch 清理由独立 Health dispatch 执行，异常不能让已提交数值工作重跑。每 dispatch 最多一个清理 batch、最多 62 行；不按 1Hz 假定真实 dispatch。COMPLETE 后保留 checkpoint 与旧 cursor/CAS；同 id 重入、已删除 scratch 的 receipt replay 不重新投影。只回收本模块新私有 scratch；不涉及 P06、mainline choice、旧业务证据、VACUUM 或 WAL 删除。

startup MIGRATION 使用 `ensureSchemaR31(db)` / `schemaObjectsR31()`：3 表、2 索引、6 immutable/protected triggers，共 11 对象；无运行期 DDL。Root 已在共享集成树接 rev1；本子分支不更改 SchemaDomain/SchemaR27。Runtime 两份 patch 以实际 `961a6b8a74b9624a2b195e9562fbdfc9283b29fc` 为基底，仅供精确 hunk 审核，不能拿旧全文覆盖后续修复。

## 验证与复现

正式冻结验证收据：`host-20261104T194441Z/RECEIPT.json`，5 组全部 PASS，源码前后 hash 不变。此轮 HEAD 为生产提交 8a1296f7，测试/证据当时尚未提交，故 dirtyAtRun=true 如实保留。运行于 JVM/native SQLite JDBC，平台层使用明确标注的已有 HOST adapters；生产 Store/EvolutionLoop/helper、原数值计算、真实 claim/fence/sole writer/receipt/SQLite 事务均实际执行。

```sh
source /workspace/starcore-environment/activate.sh
export R31_HOST_CLASSES=/tmp/r31-ba35-host-integration-mifthdj3/frozen-debug-classes.jar
export R31_HOST_RJAR=/tmp/r31-ba35-host-integration-mifthdj3/R.jar
export SQLITE_JDBC=/tmp/r31-baseline-deps/sqlite-jdbc.jar
python3 tools/r31/run_evolution_publication_host.py
```

| 组 | 真实结果 |
| --- | --- |
| baseline | git show 精确 de9 Store/Loop；128 源候选；独立 reader 验证原版事务中不可见、完整 commit 与中途 rollback |
| protocol | 101 assertions：原 research consume guards、4 测量写入、完整数值、blob/UTF-8、有限投影 401 实际 changes <=405 envelope、提交后诊断异常、清理失败 rollback、同 id 与 ACK 丢失重入 |
| phases | 101 assertions：9 个实际阶段，完整各 canonical 表与 checkpoint 逐项相同，最终 generation=1/step=1 |
| fault | 38 assertions：真实 ResearchPool.retry 改 fence、旧 owner 拒绝；实际子 JVM halt74/75、父进程确认退出、原生产恢复/重入；未提交 staging 不被 canonical reader 消费 |
| capacity | 29 assertions：21 个完整 128 源包后私有 scratch 8169 行；第22包写前 DEPENDENCY；7 个实际 SQL 清理 dispatch 后同包重新准入。重复源包是容量负载，不是独立研究样本 |

所有精确命令、exit、support/JDBC hash、日志 hash 在正式收据。原始草稿与失败日志另存 `draft-observations/`，不当作最终 PASS。曾观察草稿 GATE 约 100 ms，保留原日志；后续优化 O(N) 准备与仅对必要形状扩展 CLOB，不删除慢样本。正式 9 阶段只有稀疏 HOST hold 样本（GATE 20ms、phase5 17ms），不构成 P99 或 Android 验收证书。

## 真实边界和剩余项

- 本子范围状态：CODE_COMPLETE、TARGETED_HOST_PASS；完整 Android 构建/集成仍由 ROOT 最终同提交门证明。本目录不颁 INTEGRATION_PASS、DEVICE_PASS 或全卡完成。
- ART/真机低内存、真实锁竞争/P99、24h/72h、断网/时钟/设备杀进程：本子范围 NOT_RUN。HOST 进程故障不能替代 Android 生命周期。
- 私有准入是有界本地 encoded 行/字节：scratch 8192 行/32 MiB、受保护 checkpoint payload 192 MiB/8192 manifests。它不等于 physical DB/index/WAL，也没有冒充与其他 owner 的全局配额。有效 checkpoint 保留会形成真实容量债务并 DEPENDENCY 拒绝新增，后续需要 P03/P06 明确退休引用合同。
- 实测 cleanup 前后 page_count 582->582、freelist 0->114、WAL 3057072->3872832。证明可复用页，未证明物理文件字节下降；WAL 实际增长如实保留。
- 当前新模块全局物理数据库/WAL/共享模型总控 NOT_COORDINATED；只在本组件真实默认 per-PID MemoryBudget 取得准备缓冲 lease。
- Root 的最终 Runtime/Startup 集成必须运行完整工程及设备测试；任何后续 Runtime 改动不被本目录旧 class 快照自动认证。
- fixture 是工程合成输入，REAL/FUTURE 自然策略质量 NOT_EVALUATED，不颁合格策略、不增加独立样本、不证明胜率、频率或代际提升。

## 补充当前 Runtime Health 调度测试尚未通过

`health-runtime62c46-supplementary/` 保存独立的 12 秒真实 scheduler 尝试（当前 Runtime class source62c46，不是最终整合 tree）。第一次实际启动因 AtomPublicationStoreR31 依赖缺类退出；真实014 schema 闭包重试进入调度，最终 `awaitResourcesTerminatedR31(3000)` 为 false，资源退出断言失败，尚待定位。状态 FAILED，不能从它宣称实际 Health 周期、释放或积压排空通过，也不能用手工清理 PASS 替代。两轮原命令/原日志/class scope 均保存。此补充 fixture 不在正式五组 runner 的 glob 中，避免把它当前 WIP 外部闭包伪装成便携默认测试。
