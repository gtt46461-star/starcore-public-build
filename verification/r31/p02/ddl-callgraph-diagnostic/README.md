# 运行期 DDL 调用链诊断

这是修复前源码的只读诊断快照。`CALLGRAPH.json` 保存 12 条人工逐跳核对的源码调用链、条件、现有启动迁移覆盖、最小修改建议、实际行号和 SHA256；`PATHS.tsv` 便于比较。起点是 P01 的 1,998 条 DDL 字面量清单，其数量不代表执行次数或修复完成度。

最优先的直链为：

1. 开奖规范冲突 → SettlementEngine 撤销钩子 → CertificateRevocationR30.revokeWithin → ensure → 建表/触发器/索引。
2. 延迟证据持久化 → V51Repository.persistWithReport → DatabaseCoordinator.runExistingTransaction → 两套 DbHelper ensure；callback 内又调用 V51Repository.ensure。
3. 主链候选保存 → DbStore.saveMainChainCandidateSnapshot → 无缓存且吞异常的 ensureMainChainClosedLoopReady → CREATE/ALTER/INDEX。
4. 实时 bundle 激活/冻结 → ResearchPlatformStoreR929 → CREATE/ALTER 分支；这里有事务缓存，但冷缓存、schema_version 改变或无 scope 仍可进入 DDL。WriteClass.MIGRATION 的名字不能改变调用者来自业务冻结的事实。

旧研究轮询的 task claim、固定窗口校准和 proof audit 也可调用 schema；这些路径还受 R26 单 owner 退役门和 legacy switch 控制。公开的 ResearchEvolutionStore 写入/diagnosticCounts 与旧 B03 存储状态 API 有直接 DDL 链，但本次没有找到其活跃生产调用者，已明确标为 caller NOT_ESTABLISHED。

没有以 `Schema` 文件名豁免任何路径。反例：ReportSnapshotR30 的 `schema()` 只是读取 PRAGMA schema_version；模型/AtomNet/确认包的 DDL 常量由已列出的启动入口执行，不能仅凭常量存在就声称 checkpoint 保存执行建表。当前 R26 反馈与矩阵保存的已审方法没有直接业务 DDL，这也不等于完成全应用传递调用证明。

后续 root 已授权对 D01/D02/D03 及同链 R35 job/health 做生产修复；其修复后源码、真实 native SQLite API 调用记录和原撤销/主链权威回归在 `../runtime-ddl-hotspots/`。DbStore、平台/实时选择等其它路径由 root 另行分配。不要把本目录修复前的静态可达性当作修复后运行日志，也不要将局部 DDL0 推广为全应用 DDL0。

Android 实际 SQL trace、完整动态 SQL/反射/native 覆盖以及设备持续运行尚未运行。复验需要包含冷/热缓存、迁移中断、缺表/缺列/缺触发器、并发业务、真实事务回滚和已有数据保全；仅 schema_version 没变不能证明未执行 CREATE IF NOT EXISTS。
