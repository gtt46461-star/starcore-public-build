# R31Q21 运行恢复、完整报告与APK交付

原31.4的58卡、222项验收、统计门槛及有用能力继续保留。本目录记录2026-11-12快检引出的运行反例和真实代码施工，不把旧LOCAL收据当成新APK真机通过。

## 已查实的问题与代码修改

1. 报告长诊断文本写入遇到COORDINATOR_BUSY即失败，尚未入队；两级失败ACK又依赖同一把锁。现改为有时限、可中断的跨进程等待，且按requestId先发布有界失败状态，界面不再把无ACK的失败显示为排队。它不冒充持久ACK或成功报告。
2. 独立DDL连接未贯穿日志及主链步骤，旧库全量升级占一个写者回调。现改为按组/按表的持久日志单元，每单元释放写者后在原队列续跑，选定连接贯穿迁移、日志和校验。
3. 加列/列读取异常被吞，失败空列集缓存。现记录迁移列错误、拒绝READY，不缓存失败或空列集；每语句忙等待2秒后可观测重试，避免每列占写者60秒。
4. 全部批次关门后CI缺open_batch字段即退出，且旧摘要可能误导。现继续完整回归，验证当前source_head、Gradle、单测、HOST；不改历史关门收据。

## 完整联测

- 真实独立JVM与OS锁：短锁后恢复、长锁期限、取消、账本不越权修改、容量保护、请求隔离。
- Android34真实主进程请求 → diagnostics Service → 完整ZIP发布 → 分享Provider读取 → 字节和SHA256回算。
- Android34启用生产AutoFetchService后，必须取得新API抓取、新赛前冻结及随后结算，commit_at早于deadline_at；不以启动或心跳代替进展，不宣称这些样本证明胜率优势。
- 原单测、lint、注册HOST、API29/35协议与故障矩阵继续执行。失败继续修，不删断言，不降统计门槛，不把SHADOW记REAL。
- 两个旧守卫要求重新登记迁移字节身份和DbStore方法图。SCHEMA_BLOCK_REVIEW.json验证登记的schema/缓存/调度块能精确还原原9edd4e0f…整文件；PnL/Gate原哈希及篡改反例不变，逐方法图无删除项。

## 逐项文件

- STARCORE_R31Q21_SOURCE_AUDIT.html：逐卡、逐指标、逐图、逐逻辑表、逐用户要求。
- SOURCE_ACCEPTANCE_AUDIT.json：源文件SHA256、精确LOCAL项目、绑定测试、延后项目和历史收据。
- REPAIR_WORK_ITEMS.json：本次集成修复项。
- SCHEMA_BLOCK_REVIEW.json：schema块字节复核，不是资格放行。

## 交付与证据范围

当前源码施工和CI继续。APK须由同一干净提交构建，保持com.starcore.research.r13及既有d8d8cee7…证书，提高版本号并保留数据库；APK/ZIP、SHA256、源码和实际结果由交付回执绑定。

快检约在本次启动22秒后采集，目标期开奖在启动前；不得补造该期赛前预测。job.lock文件存在或很旧不证明OS锁被占，不能靠删除锁文件修复。心跳不证明业务进展，crashCount=0不证明未被系统终止。

原机670MB数据库未在云端取得，模拟器通过不冒充该旧库真机通过。24/72小时与自然质量未实测时保持NOT_RUN/NOT_PROVEN。
