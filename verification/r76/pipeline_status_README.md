# R76 中文研究状态快照

`ResearchPipelineStatusR76.read(DbStore)` 仅供既有后台界面 worker 调用，返回 immutable `Snapshot`：`readAt/sourceIssue/summary/training/evolution/atomsMatrices/mainline`。MainActivity 由独立界面修改接入同一缓存；本类不更新 Owner、数据库或资格。

- 逐期显示最近三个干净开奖中的 FTRL 产物、R952 赛前候选、结算、精确 delivery 对应数值 artifact。两条在线学习分开列出。
- 现代开发训练先取最近 32 个协议，再按自然任务身份、checkpoint、协议唯一收据、artifact 身份/训练验证样本数和 learner 冻结索引联查。任务标记完成而无匹配收据明确显示缺证据；特征行数不是训练完成率。
- 反馈、创造/变异/融合提案、子代生成、独立评价分别显示。近期范围不足时不将页面样本冒充全局总量。
- 原子/分子读当前资产哈希的导入游标。矩阵定义与数值投影分开；目录完成不等于原子准入。
- 当前活动校准预约中按已结算/捕获进度选候选，不称最佳胜率。无活动预约时仅取状态索引尾部四个完成采样记录；资格由现有 `qualificationForStudyCP12` 验哈希/策略。证书只显示最近两个登记记录中匹配冻结身份的状态，不自行授予有效性。

查询无全表 COUNT，不读取训练模型/特征 BLOB。按 rowid 的扫描在先行子查询中最多 33 行，逐期最多 65 个候选，其余为主键/索引查询。校准模型复核沿用原 64 KiB 上限。每段开始检查 350 ms 总读取预算；这是协作预算，不是 SQLite 锁等待的硬取消时限。后台读取失败或超预算显示未知/等待，不能填零。每次快照标时间，特征/数值进度也显示各自最后提交时间。

## 可重跑验证

```sh
python3 verification/r76/pipeline_status_run.py \
  --jdk /workspace/scratch/cd9274eff1c6/jdk/jdk-17.0.20.1+1 \
  --schema /workspace/scratch/7a6032548233/r72-final-schema \
  --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes \
  --report /workspace/scratch/e2544c9906f6/r76-input/report/DIAGNOSTIC_SCOPE.json \
  --repair /workspace/scratch/e2544c9906f6/r75-final-training-452854cc/repair/repair.db \
  --out /workspace/scratch/e2544c9906f6/r76-pipeline-final \
  --final-classes
```

开发验证省略 `--final-classes`，仅额外编译本次新增 helper。最终模式只编译测试和 Android 时钟适配器，直接运行 release helper，并记录其全部 class 哈希及不变性。每个数据库运行前后完整文件哈希必须不变。

测试包括报告元数据、公共训练 Owner 已完成的维修子代数据库、空库、缺表、虚假队列完成、独立 FTRL 产物和超过页面上限的动作。报告重建不包含原二进制；长度占位不能用于数值验收。报告没有导出 FTRL artifact 行，该 fixture 中的空表不表示用户手机没有 FTRL 学习；单独在线正例是明确标记的 UI 元数据 fixture。公共 repair 库则保留实际 Owner 产出的训练验证收据和冻结。所有测试仅验收只读显示，不增加手机的训练、证书、Future 或 REAL 证据。

输出 JSON 保留每条实际 SQL 的 `EXPLAIN QUERY PLAN`。宿主耗时不代表 Android 或用户手机耗时。
