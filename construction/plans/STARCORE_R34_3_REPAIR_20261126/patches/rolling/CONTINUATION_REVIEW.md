# R34.3 研究链续接复核与候选代码修正

## 结论

已安装173中的现代持续学习断点、训练错误终结与部分种子就绪误阻断，原44问题清单已经覆盖；没有必要为增加代码量重写现有数学内核。恢复方案已经保留原K3、PIT、唯一主链、原模型/检查点与旧冻结身份。39个家族标签中包含共享数值内核，原数学清单对这一点已明确区分。固定图无优化器权重不能被描述为已经训练出新的数学机制。此次复核没有发现必须扩大质量门或伪造未来样本的理由。

但对**尚未部署的R343候选方案代码**，发现3个可以阻断未来持续运行的设计缺陷，已修在同一B06/同一owner中。它们不是本次手机报告已经观察到的173故障，建议作为R343-026附属自检发现登记，不混入“报告现场事实”。

## 1. 事务内正常等待被fallback writer吞掉

- 安装版源码：`DatabaseCoordinator.java` `runAtomicWrite` 调用 `runWrite(context, atomicCallableF01(...), fallback)`；`runWrite`的catch(Throwable)记录错误后返回fallback。
- 原候选：`RollingFrozenPublisherR343.commit` 在target变化、已开奖、source epoch变化、per-learner/global冻结容量满时抛`DevelopmentProductionCP12.Wait`，但其调用是`runAtomicWrite(..., false)`。外层catch Wait无法接到，45秒RUNNING租约仍被占用，正常等待还会产生writer故障记录。
- 原候选learner：特征页工作预算满、数值完成时source epoch变化/预算满同样存在。
- 修正：在同一事务、写新特征/模型/冻结之前，通过原`ResearchTaskQueueR967.yield`释放原generation/owner/cursor，并保存准确reason与重试时间。新增`waitWithinR343`、`writeWaitReasonR343`供两文件共用。真实完整性错误、失效CAS、未知异常继续失败回滚；没有把所有RuntimeException改成等待。
- 回归：真实Java方法抽取+SQLite桥接，六种旧callback正常等待均复现异常与RUNNING；新callback均产生YIELDED、精确reason、相同cursor、零新增publication。真实完整性错误与stale lease仍失败。

## 2. 原件回收饥饿

- 原候选：`RollingLearnerR343.cleanup`只在无可claim任务且无可计划任务时运行；活跃容量等待任务可以挤掉清理机会。
- 同时固定选择`ORDER BY updated_at LIMIT 16`，若前16个有有效冻结pin，后来已拒绝/可清理的版本永远不会被看到。64MiB工作预算满后会持续等待。
- 修正：既有TRAIN owner的计划轮先给一个有界清理机会；清理最多扫描16个版本、删64行feature或一项终态payload。持久`r343_rolling_cleanup_after`按version_id推进并到尾回绕，经过pin对象仍推进；不新增线程、调度器或数据库owner。HEAD模型、活跃版本、未发布offer、活跃冻结原件仍按原pin合同保留。
- 回归：原方法连续三次卡在相同16个pin；新方法下一次清理能到第17个，单次恰64行、游标回绕、后续清完可回收原件。16个受保护冻结对象逐字节一致，HEAD模型与活动input不变。

## 3. 新任务入队前缺全局payload容量预检

- 原队列`ResearchTaskQueueR967.enqueue`在4096个非MAINTENANCE未终态payload满时抛Capacity。原learner先写VERSION/HEAD才enqueue；正常背压会回滚并进入writer故障统计。
- 修正：learner和publisher在原writer同一事务中、业务插入之前使用原4096边界预检。继续保留22个MAINTENANCE控制槽；不扩容、不删除任务、不回填证据。
- 回归：4096 payload时拒绝，终态减少一条后可继续，22 maintenance不侵占payload界限。

## 交付文件与合并方式

- `after/app/src/main/java/com/ethk3/novaonepro/r952/RollingLearnerR343.java`
- `after/app/src/main/java/com/ethk3/novaonepro/r952/RollingFrozenPublisherR343.java`
- `review_delta.patch`：相对恢复包rolling候选after的增量diff，不是相对已安装173。
- 完整新后态与前后SHA：本组after、FILES.json、change.patch以及总PATCHSET、FILE_CARDS。
- 上次候选两文件：`host_validation/rolling_patch/review_before/`，仅作回归反例夹具，不是生产before或after。
- 自检脚本：`host_validation/rolling_patch/tests/test_rolling_waits.py`。
- 初始51项回执：`control/ROLLING_WAIT_AND_CLEANUP_REVIEW.json`；总装重跑见`control/revision2_host/`。

独立重跑命令（所有路径均从计划根定位，输出目录必须是新的计划外目录）：

```bash
python3 -B "$PLAN_ROOT/host_validation/rolling_patch/tests/test_rolling_waits.py" \
  --after "$PLAN_ROOT/patches/rolling/after" \
  --before "$PLAN_ROOT/host_validation/rolling_patch/review_before" \
  --source "$APP_REPO" \
  --out "$EVIDENCE_ROOT/rolling-waits-cleanup"
```

也可运行总入口tools/run_host_checks.py，由它装配精确173依赖。实际修改包括两个rolling生产文件与ReportDiagnosticEvidenceR64的cleanup游标白名单；R343-026、B06、ACCEPTANCE_MATRIX、完整after及全部补丁哈希已同步。脚本的出版元数据/模型codec/eligibility替身只作为前置条件；这51项没有替代此前六kernel/lineage测试，也没有替代Android构建、真实数据库、设备尾延迟或自然未来质量。
