# R32.5 / P2.06：归档失败与学习主任务隔离

## 已复现的缺陷

R32.4 的 `AtomNetRuntimeR30.step()` 在冷启动恢复及训练之前同步调用 `AtomNetSampleArchiveR324.maintenance()`。归档查询以 `publication_id` 固定选择第一个旧发布；若其 `sample_count=10` 而实际原件只有9行，生产 `source()` 每次抛出 `R324_SAMPLE_ARCHIVE_SOURCE_INCOMPLETE`。同一个学习任务无法继续读取完好的当前模型，重复失败还计入整条 `r27-atomnet` 的隔离预算。

本目录的反例**直接调用未经修改的 R32.4 生产 step**，连续三次重现当前模型恢复被同一旧坏对象阻断；并未用测试替身重写业务逻辑。

## 同批修改

1. `AtomNetRuntimeR30`：删除归档前置调用，冷启动/训练不再以旧发布归档成功为前提。
2. `RuntimeR27`：注册独立 `r27-atomnet-archive` 任务；仍使用原池、原任务fence、单写队列和CPU/内存预算。该任务的取消或故障不会清空学习器草稿。
3. `ResearchTaskRegistryR30R1`：在 ADDITIONAL 登记非训练任务；原57项登记保持。
4. `AtomNetSampleArchiveR324`：持久化每个发布的尝试次数、上次尝试、下次重试、状态、失败类/代码及成功时间；本轮一次只处理一个到期对象。到期时间排序保证坏对象不会重复占据唯一候选槽。候选从当前热样本身份进入，不枚举全部已归档发布。
5. `AtomNetPublicationR31`：schema域修订3→4，升级仍由原迁移owner执行；原 `storageHealthR32()` 接出归档任务和对象故障明细。

## 状态与异常边界

- CLAIMED：先提交尝试及下一次重试时间，再进行解码和文件准备。进程中断不会丢失重试期限。该状态本身不是业务成功。
- FAULT_PRESERVED：仅精确列出的旧样本源数据/codec错误隔离在对象上；保留全部原始行和发布身份，不合成缺失数据。
- ARCHIVED：成功状态与归档清单、受控退役许可、样本删除在同一个事务提交。
- 取消、未知异常、数据库损坏、磁盘写满、未知提交结果、嵌套进程致命异常继续抛给原任务机制。没有通用 `catch` 放行。
- 重试最短60秒，指数增加，上限30分钟；时钟回拨允许一次新的有界尝试，不把旧未来时间当永久禁令。
- 故障记录落库失败会继续抛出，不伪造“已隔离”。任务仍只影响归档owner；不会将异常偷记为训练成功。

## 回归命令

在当前阶段源码已应用、全项目宿主类型编译产物已得到后执行：

```bash
python3 tools/r325/archive_liveness/run_archive_liveness.py \
  --repo /absolute/project \
  --classes /absolute/current-stage-classes \
  --android-jar /absolute/android.jar \
  --jdbc-jar /absolute/sqlite-jdbc.jar \
  --slf4j-jar /absolute/slf4j-api.jar \
  --out /absolute/new-result-directory
```

`--baseline-classes /absolute/unmodified-r324-classes` 可选，用于重演旧版本反例。正向验收只能使用当前P2及以前源码编译的类；不得取P5未来类给P2签收。

当前专项41项宿主断言包括：实际生产冷启动恢复、当前完整模型字节保持、坏对象隔离与下一个对象推进、SQLite关闭/重开、持久退避与到期重试、真实原件补齐后关闭故障、取消/未知IO传播、嵌套存储/致命错误不降格、回拨时钟、受控事务失败后原始行/清单/成功状态一起回滚及重放。此前R32.4反例为连续3次失败。

这些回执证明所列生产Java/原生SQLite/实际文件路径的宿主行为，不证明 Android Worker 的实际调度、Binder、设备后台72小时、自然未来命中率或APK已构建。

## 报告接线

`storageHealthR32.archiveMaintenance` 提供 `task`、`learnerPrerequisite=false`、`maxRetryMs`、`statusCounts` 与最多16条 `boundedFailureExamples`（出版ID、次数、上次/下次时间、异常类、精确代码、原件保全）。报告应将故障归为归档退化，单独展示实际学习任务状态；不得由一个坏归档推断学习算法全体失败，也不得把 CLAIMED 一律当故障或成功。
