# R31 输入快照容量准入子范围

方案基线为 `5237b265a27fe5ad27fd842901c445d5f8c27749`；本隔离施工从 Shared 提交 `e8786a4b0cd4a42a3cbaf4becb153aa244d72d06` 开始，分支 `construction/r31-pin-capacity-20261104`。继承 d88 的完整输入、角色、来源日志、精确成员及无 fanout 协议，没有重写来源事件、旧 pin 或 invalid 账本。此项只补 B02-C03、B05-C02/C11 的输入 pin 子范围，整卡保持 IMPLEMENTING。

## 先测量，再修改

最新可复现入口：

```sh
python3 tools/r31/verify_pin_capacity.py \
  --support-jar /workspace/r31-shared-bootstrap-30ff-frozen/classes.jar
```

固定实际 Android 编译产物支持 jar SHA256 为 `582f7475b43d218c50d34f8f24d4399ae4916b53579807c22c162bd25b7fea56`。工具 fresh 编译本模块四份生产源码，使用真实 SQLite JDBC / AOSP JSON 的 HOST 边界；不替换业务类、不替换内存 Probe、不显式 GC。最终证据为 [212120Z 收据](runs/20261104T212120Z/RECEIPT.json)。所有 origin 源及支持 jar 在执行前后逐字节稳定。

输入为真实合法的 1–10 全排列，分别使用固定排列与固定种子的均匀洗牌；这些是工程数据，不能作为策略质量样本。直接调用原始生产 `append` 与修改后生产 `append`，每次均反解原值、datasetHash 及 Split 原角色。

| 期数 | 排列 | bind 字节 | 逻辑 DB 增长字节 |
| --- | --- | ---: | ---: |
| 5000 | 固定 | 17810 | 16384 |
| 5000 | 均匀洗牌 | 56342 | 53248 |
| 10000 | 固定 | 34707 | 32768 |
| 10000 | 均匀洗牌 | 110799 | 110592 |

四项实际 bind 均低于本 JVM 的 262144 字节上限，压缩与角色逐项保持一致，因此保留单行完整压缩快照。最终每项事务真实观察到 1 次 execSQL、53 次 rawQuery、前后顶层回调各调用 1 次，含 pin/fence 共改变 2 行；没有把 callback 存在当作实际执行。该数字是直接执行生产 Batch、无 F01 prepared transaction cache 的测量，不是整个应用所有 writer 的 SQL 数量或延迟承诺。

JVM 实际最大堆为 536870912 字节。当前四项 prepare 累计线程分配为 8749208、7595712、13912688、15182384 字节；1ms 采样总进程已用堆峰为 18179928、55404376、112698856、87688592 字节。累计分配不是同时存活的内存，采样可能错过瞬时峰；采样值也不是 post-major-GC live set。每项前后实际 Java/native/PSS/RSS 口径保存在原日志中，HOST native Android Debug 缺源保留 UNKNOWN。独立 JVM 的自然 GC 时机不同，不据这些测量宣称性能改善。

## 实际生产修改

`PinPublicationR31` 在准备前申请 Kind.RESEARCH 的 `2MiB+4096*rows` 声明 working lease，另保留 `4*bindCap+64KiB` 的 bind/hash scratch 与可逃逸 JSON 声明。这是有依据的保守工作量估算，尚无设备峰值容量证明。准备 scratch 在真正 append 退栈的 finally 释放。JSON 仍被 Batch/闭包持有时不归还声明；WeakReference 的 ReferenceQueue 实际观察到不可达后才归还，1024 个 tracker/owner 上界与现有全局 lease 上界一致。没有声称关闭声明等于物理堆已释放。

新增 `ResearchInputPinR30R1.appendAdmitted(Batch, SQLiteDatabase, File filesRoot, ReplayDatasetR27, long sourceRevision, long now)` 返回 `PinPublicationR31.Scope`。Scope 申请既有跨 PID `StorageBudgetR31` 的 INPUT_RESEARCH 预算：常态 2GiB、瞬态合同 4GiB、可用空间保留 256MiB；新 pin 使用常态上限。数据库必须绑定到相同 app data root 的 databases 目录。写前在事务外读取真实 page size，承诺 `3*(bindCap+16*pageSize)+当前WAL字节+2MiB` 的目录增长，覆盖该 bounded pin、页/WAL/rollback 复制及有限并发余量。其他未接入 writer 仍可能同时增长，不能把这个承诺当作所有 canonical writer 的总容量证明。

Scope 必须由实际 owner 在同步 submit/retry 完成或真实退栈后 finally 关闭；Future.cancel 不触发归还，外线程关闭被拒绝。managed Batch 在 writer 内校验 scope 存活和原 source CAS。既有公开 `append` 保留兼容的内存准入，明确未取得磁盘预算。压缩每 64 行调用既有 poll，取消/让片不发布半成品。Batch 总 op<=64，UTF8 bind<=`min(262144,M/512)`，检查失败不改已有 Batch。

容量清理的真实 IOException 只记录 cleanup pending，不能把已提交 pin 翻译成未提交并重训；既有磁盘 owner lifetime lock 负责后续回收元数据。没有删除模型、输入、旧快照或来源日志。

## 实际验证与边界

最终命名 HOST 准入 53 断言 PASS：实际 5000 pin 恢复、2GiB sparse 逻辑容量债务拒绝新增 pin、既有 pin 保全、债务下 canonical 来源事件仍提交、255MiB free 负控、UTF8/64op 超限、真实 BudgetToken preempt、Future 已取消但 body 未退出、外线程提前释放拒绝、提交后真实 metadata I/O 异常以及真实默认 Probe 的强引用堆压力拒绝。SourceJournal 原 3631 断言与原 3000 期 RepairInputPin 原值/角色/retention 恢复断言均 PASS。

HOST 无 Android allocated blocks；正控明确注入与生产相同 2/4GiB、256MiB 门槛的逻辑字节配置，绝不称为物理占用证明。生产默认 storage config 在该 HOST 正确拒绝 `ALLOCATED_BYTES_UNKNOWN`。没有降低阈值、扩大内存、清账或让 canonical draw 跟随低优先级 pin 配额停写。

最早 `baseline/measurement.json` 的 `heapAfterPrepare` 在 SQL/load 后取样，不能作为 prepare 出口证据；后续 runner 已在 append 返回后即时采样。`211212Z` 的旧 HOST close 签名编译失败原日志保留；仅将复制的旧 HOST adapter 改成当前 SQLite 边界兼容签名，未改其业务断言。`211455Z` 回调字段只代表存在；最终 `212120Z` 已用委托计数观察实际调用。

以下保持 OPEN / NOT_RUN：

- Root 的真实 Runtime 消费点接 `appendAdmitted` 并持有 Scope 跨 submit/retry/finally，尚未由本模块验收；未发布 FeatureCache 失败后关闭也由 Root 整合。
- 来源 append-only journal 保留/回收、import members 和 online fence 的新增磁盘预算，任意 SQL REPLACE 边界，尚无完整覆盖；关键来源事件不因 pin 债务被阻断。
- native zlib workspace 容量与实际退出包络、恢复输入 decode/返回 dataset 的新生命周期准入尚未覆盖。
- 实际 Android 物理块、ART 新调用链、全源码/全量测试、可安装 APK、实体设备、24/72 小时及自然未来质量，本模块未执行。

本证据支持本子范围的 CODE_COMPLETE 与命名 HOST 检查通过；不支持整卡、G02、设备容量或自然策略质量闭环。
