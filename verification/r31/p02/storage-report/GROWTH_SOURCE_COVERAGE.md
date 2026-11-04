# B05 增长来源与真实覆盖

| 来源/owner | 本阶段真实消费者 | 状态/边界 |
| --- | --- | --- |
| FULL_REPORT backup/native | ReportExport→Control.beforeBackupChunk→StorageBudget | 已接；真实host JNI，Android设备未跑 |
| 固定附件/源索引/asset body、计划、seed与归档观察 | AttachmentClosure→ReportIo→当前job grant | 已接输出；真实copy/sink测试，AndroidContext/Assets全链未跑 |
| Catalog page/诊断scope/file inventory | Catalog/DiagnosticEvidence→ReportIo | 已接；FilePages/流式inventory host；96业务检查未运行 |
| 外排与renderer | MemberPlan/ReadContext/ReportText→ReportIo | 已接实际run/merge/spool/manifest；失败移除仅自身无消费者nonce临时物 |
| Archive/manifest/ZIP/parts | Archive/Verifier/Publication→ReportIo→当前job grant | 已接；CRC/SHA/分卷/取消真实JNI回归 |
| metadata/request/ack/control | Journal.writeAtomic→已知bytes AtomicPermit；Report区外当前report输出也route独立grant | 已接；AtomicFile替换前准入。报告组件外使用该旧utility且不在report作用域者保持原API语义，不宣称一并管控 |
| 恢复ZIP及receipt | Journal→Artifact独立grant/share pin/UI取消 | 已接；原件/真实FD关闭/CRC/SHA与同job重试验证 |
| 已发布workspace cleanup | Cleanup→exact coverage/hash→share exclusive | 已接；HOST实际regular-file减少。allocated/free-space释放及无新导出OS alarm时间界待设备证据 |
| 外部SAF provider payload | ReportSafCopy期望byte上限、受控流关闭、确切回读 | 不是app私有路径，provider物理空间和跨provider reservation UNKNOWN，不宣称app budget已管到远端provider |
| RETENTION_PAGE/shared retention cache/CAS | 原已有边界与source pins保留 | 本轮不启P06删除；统一StorageBudget消费仍属跨owner后续接线，不能混成FULL_REPORT已完成 |
| 热DB/WAL/冻结/结算写入 | DatabaseCoordinator/ingest/settlement | P01真实事务协议保留；本StorageBudget未接全部增长，关键链预留与类别预算尚待跨owner联验 |
| training CAS/checkpoint/model bundles | 既有128MiB CAS、checkpoint和model原owner | 保留原cap/lock/引用/证据；未接共享global reservation，不把本地旧cap算统一闭环 |
| chart/cache/log/backup/diagnostic采样控制文件 | 各原owner/StorageLedger固定ring | 本轮不删除业务cache；sampler固定288+720并观测实际大小。全部producer的写前接口/类别预算/字节归因还未接齐 |
| Memory | 每PID MemoryBudget native32/4 stage lease；Java summary有界 | 分别记Java/native/PSS/RSS，不把磁盘算heap。跨PID总额度NOT_COORDINATED，全部native owner/实际峰值/图表等仍待整合和设备校准 |

共同预算只对真正使用其协议的producer有效。全局2/4GiB和256MiB floor不证明上表未接producer受控；分类cap、关键链预留、公平性与整个5000期数据保护闭包仍需联合实现/验收。保护数据超预算时拒绝新增并记录debt，不通过扩大quota、移动目录、清库或删mainline/choice解决。

实际物理口径：Android Os.stat st_blocks*512；regular file.length另列；HOST Java census的allocated为UNKNOWN，native Linux回归使用真实stat。cleanup receipt明确REGULAR_FILE_LENGTH/allocated UNKNOWN/receipt写排除，不能把其reclaimedBytes字段冒充全设备free-space增加。恢复检验另外产生的HOST restored.db是独立可恢复性fixture，未当成原件搬家或全盘净回收证据。
