本轮继续施工及接口打包修复详见 [CONTINUATION.md](CONTINUATION.md)。当前分支的新构建仍须以其 CI 结果为准；下文保留上一交付的验收范围。

# 星核创研 SU1 工程交付

这是实际修改源码、可编译并可独立安装的研究版。**不声明方案 100/100 已完成验收**。100 条要求逐项记录在 [requirements-delivered.csv](requirements-delivered.csv)，其中 `PARTIAL` 明确指出尚缺的代码或验证，不能把“已有类名”当作能力验收通过。

安装包名为 `com.starcore.research.su1`，显示名称“星核创研 SU1”，采用新星形图标，与原软件共存。Java namespace/JNI 名称保持兼容。交付的是已签名 debug APK；Actions 缓存 SU1 调试签名供后续研究构建使用，正式发布签名没有被伪造或替换。最终包中的 `signature.txt`、`package.txt` 和 `SHA256SUMS.txt` 可复核签名、系统/架构要求及文件完整性。

源码在 `construction/su1-20261022` 分支。公开仓库基线是 `f3e249ce3aef9a8975abe0be38cbe2d1183e4078`，方案的私有基线是 `9ec590cafbd7cbcd1265a11b63a59df53ba78fcd`；没有用签名归档分支代替源码基线。旧合同逐文件保留/修改情况见 [compatibility-delivered.json](compatibility-delivered.json)，原施工对照表保留在 [compatibility_map.csv](compatibility_map.csv)。所有受控源码修改由 `release/strategy_su1_reviewed_inputs.json` 显式固定；构建仍运行原身份生成器、原变异自测和标准 Android 检查。

实际接入的路径：

- `ResearchDirectorR952` 在开奖前调用新的 PIT 捕获；`LiveShadowLoopSU1` 对最多 32 个研究程序记录冻结、拒绝或预算跳过。只有原结算任务已为 `COMPLETED`，才写入新研究结算；重复结算不增加次数。
- `AtomLiftSU1` 调用原 `AtomEvaluatorCP4`，提升 168 个评分原子族和 150 个条件原子；GRAMMAR、八种种子家族、变异/交叉/融合、宏提炼和支持度达到 3 的新家族实际执行。手机 DEEP、XI、HEDGE 是显式版本化的因果窗口算法，与主机实验实现不冒充同一个模型。
- `RelationalTrainerSU1` 对 E/U/R/Wx/Wm/Wh/b/Wo/c 实际执行 BPTT；单测检查有限差分、更新后损失和推理变化。DEEP 每个固定块按过去数据重新训练，冻结身份绑定其算法、种子、代码和完整 PIT；不是把训练命中率当成未来证据。
- `StrategyFoundrySU1` 保存逐期回放游标、尝试、DEV 稳定评分、VAL 冠军曲线及研究记忆。充电且空闲时由 JobScheduler 运行，计算切片受操作量、时限、结算/P0 抢占及 CAS 检查点控制。
- `SealedPlannerSU1` 在看见测试结果之前锁定唯一冠军与未来 1,440 期；数据库限制只开一次。校准使用独立的 120/1/120 实际冻结样本，产生一个新策略版本，证据从零开始。V1 正式确认只支持全覆盖 K3 政策；选择性策略仍留在研究轨。
- `EvidenceLoopSU1` 的固定机会分母包括双方都没冻结的情况；缺失按保守损失处理。父子比较、均匀伪基线、独立 sham 与真正的历史标签置乱对照使用相同预登记日历区间。任何负对照 PASS 都会阻断该家族。
- `CertificateSU1` 复核两条声明、半径/alpha、校准、四块稳定性、对照、资源探针、代码身份和有效期；`MainlineAdapterSU1` 默认关闭。只有有效证书加用户在研究页申请启用后，才把完整 1,200 动作投影交回原 `CertifiedPredictionR974`、`DecisionGateEngine`、`DbStore` 唯一 owner；研究器没有新建 REAL 写入器。提交前还复核冻结动作及来源 generation。
- `ArchiveStoreSU1` 对活动、开放队列、未结算及检查点依赖做传递 pin；文件 flush/fsync、回读、内容/语法重放及 generation/CAS 验证后才释放本地 blob。旧研究日明细须先有匹配日汇总和已验证归档才能压缩。逻辑 blob 配额是 512 MiB，超额停止新的 SU1 工作；没有承诺无限存储。

数据库保持原 `user_version`，新表单独增量安装。新迁移使用 V2 journal，保留预览版 V1 receipts 和数据；仅识别出的旧列/触发器走前向升级。READY 会比较实际表、索引、触发器及 SQL 内容。回退应用不删除证据、不回收 alpha；不同内核旧版本停止接收新的正式机会。

[INSTALL.txt](INSTALL.txt) 说明安装与入口。首页“持续策略研究”可刷新、安排任务、暂停、继续、导入电脑工件和申请使用已验证策略。没有足够数据时显示等待；Brier/ECE 没有测量时保持 UNKNOWN。独立包名不会自动读取原软件私有数据库。**数据库内终身 alpha 不可退款；卸载删除数据、独立包沙箱之间的全局终身预算需要额外持久账本，本版本不能保证这一点。**

## 可复核测试与实验

`.github/workflows/strategy-su1.yml` 执行实际 Android SDK/NDK 构建、标准 unit/lint、原身份变异自测、独立 Python/Java 对拍，随后在 Android API 29 x86_64 模拟器安装 APK 并运行数据库/结算/迁移和内核对拍。最终是否 PASS、确切源码 SHA 及原始日志以该次 Actions 记录为准。

独立对拍数据包含 16,000 个 GRAMMAR 评分、155 个可比较动作（原协议排除 5 个数值近并列动作），以及 84,000 个原子评分和 750 个条件结果。条件协议明确为 `SU1_ATOM_CONDITION_V2_1E12`；对熵等标量按 1e-12 量化后比较中位数，没有放宽 1e-4 评分容差。

本次实际执行了完整 10 代、种群 32 的五组主机实验，结果和执行来源 hash 在 [validation/](validation/)；没有把施工包原报告冒充本次运行：

| 数据集 | 封存段命中/行动 | 命中率 |
| --- | ---: | ---: |
| 真实历史 | 302/1096 | 27.55% |
| 时间打乱 | 356/1098 | 32.42% |
| 植入重复信号 | 227/527 | 43.07% |
| 植入跨位置状态 | 131/275 | 47.64% |
| 植入条件信号 | 290/545 | 53.21% |

双轨主机实验的真实数据系统输出为 516/1739 = 29.67%；条件植入为 300/562 = 53.38%，跨位置植入为 226/487 = 46.41%。这些都是历史实验，不是自然 Future，也不构成证书。时间打乱组的未经终身多重比较修正 p 值不能作为晋级资格。真实数据实验没有证明稳定优势。

## 电脑端

在仓库根目录安装 `numpy==2.3.5`，可执行：

```sh
python tools/strategy_su1/scripts/run_historical_replay.py --history research_data/ethpk10_draws_20260316_201059.csv --generations 10 --population 32 --out build/historical-controls.json
python tools/strategy_su1/scripts/run_dual_track.py --history research_data/ethpk10_draws_20260316_201059.csv --out build/dual-track.json
python tools/strategy_su1/scripts/export_grammar_artifact.py --history research_data/ethpk10_draws_20260316_201059.csv --trained-through-issue 202603160001 --program 'XT(0)' --coverage 1 --out build/xt0.su1
```

最后一条是手工种子导出示例，日期必须改成该数据实际的训练截止期，不能冒充工厂冠军。新的历史 runner 会在 GRAMMAR 冠军报告中输出可移植工件；其他主机家族不会被错误转换成手机模型。导入器只接受封闭字段、合法/受限语法和正确 hash，核对本地历史截止期，创建新的研究身份；不能导入命中次数、alpha 或证书。任意联合训练张量图/优化器续训的完整互通还没有实现。

当前主要未完成部分是完整关系支持度审计与增量缓存、通用状态/张量图联合产物互通、残差互补驱动的融合、完整消融/关闭记忆对照、每次手机活动三组控制以及全部旧进化资产重测。逐条边界见验收 CSV。实体手机的持续热负载/性能、真实覆盖安装与独立自然样本资格也仍是 WAIT；模拟器或历史实验不能代替这些项目。
