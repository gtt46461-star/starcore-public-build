# 已执行的生产方法反例与数值恢复

最新完整执行：[`runs/20261104T102314Z/receipt.json`](runs/20261104T102314Z/receipt.json)。状态为 `COUNTEREXAMPLES_REPRODUCED_AND_NUMERICAL_RECOVERY_EXECUTED_NOT_PRODUCT_PASS`，不是 R31/B06 完成或发布门通过。

|已实际运行的检查|结果|证据|
|---|---|---|
|TRAIN、EVAL_TRAIN、EVAL_SELECT、EVAL_DEVELOPMENT_VALIDATION、FILL 每个 work 边界 encode/decode|23 个边界：NN/Adam、RNG、FTRL、完整 outer 状态及十位置后续数值预测精确一致；只归零对模型无影响的 trainingMs/materializeMs 作对照|[`pretrain-boundary-resume.log`](runs/20261104T102314Z/pretrain-boundary-resume.log)|
|位置 1–9 分别写 fixture checkpoint，真实 `Runtime.halt(83)`，新 JVM decode 后继续至位置 10|9 次 halt 均 exit=83；9 次恢复均 exit=0；全部与不中断黄金 full state/hash、NN/FTRL 后续预测精确一致；重复 issue 仍拒绝|[`halt-position-9.log`](runs/20261104T102314Z/halt-position-9.log)、[`restore-position-9.log`](runs/20261104T102314Z/restore-position-9.log)；其余位置同目录|
|位置 1–9 的完整发布必须保留上一完整状态|**9/9 baseline FAIL，exit=1，原 AssertionError 留存**。位置 1 已出现 complete=999、partial=1000、nextPosition=2，但 publication through=1000；真实 ModelSurfaceProvider.predict 接受该 partial publication 并输出变化的概率|[`partial-publication-position-1.log`](runs/20261104T102314Z/partial-publication-position-1.log)、[`partial-publication-position-9.log`](runs/20261104T102314Z/partial-publication-position-9.log)|
|相同完整 checkpoint 重建 publication 应有稳定 identity|**baseline FAIL，exit=1**。完整 checkpoint bytes、net bytes/hash、trainedThrough 都相同，只有恢复时新取 availableAt，最终 modelHash 改变|[`restart-publication-identity.log`](runs/20261104T102314Z/restart-publication-identity.log)|
|未调用 publish 的数值 draft 不污染 committed 对象|host 单项通过；draft 有实际数值变化，原对象 bytes 与 publication identity 不变；这是对象隔离，不是数据库 rollback 测试|[`draft-isolation.log`](runs/20261104T102314Z/draft-isolation.log)|

执行使用 OpenJDK 21.0.12.1 的真实 compiler module，没有 Gradle。每轮将 25 个当前生产 Java 文件精确复制至独立 `/tmp/r31-p03-publication-repro-*` 后编译，加上本目录的 harness；关键训练、codec、Runtime publication、descriptor、surface/predict 方法均执行 freshly compiled 字节码。只为 Runtime 其他未执行方法链接而使用复制到 `/tmp` 的现有 P01 debug classes 和 Android 36 API jar。receipt 记录各生产文件 SHA-256、关键方法的完整原文与 SHA-256、harness/runner hash、编译命令、所有 exit code、原输出及实际加载的 31 个 app class 身份。最新轮 `source_changed_after_snapshot=[]`。

关键当前生产方法摘要：

* `RuntimeR27.publishAtomStateR30`：`da48255177dcf7ea746b6bd66ef3a9544d439e161670afa71d3186d6e468b9b7`
* `AtomNetSessionR30.online`：`4ee99ce2d120010670506105d7f494ca9bed84dfb292982cdf80bb8e10357e13`
* `AtomNetSessionR30.ready`：`13d21fb5031fbbaf2569f2eb6314dfa0153ee04f882a8ea71555e6106a900549`

fixture 使用真实 182 维网络、80 TRAIN + 40 SELECT + 20 隔离 HOLDOUT 样本容量。samples 回调若读取 index >=120 直接失败，formal holdout 没有被优化器读取。非零学习率使真实权重/输出变化，Adam step 从初始可用状态的 2 增至完整在线期的 12。最终 normalized golden state hash 为 `406baf7186dd1c478a125877ff42402e81db8b83878e4ce5db6bf6f3d9e1a43b`；九条恢复路径全部得到该 hash。

这个只有 120 条 replay 样本的合法完整 seed 外层已为 **652656 bytes**，大于 acceptance_config 的 bind bytes 最大上限 262144；因此完整 BLOB 写入不是已完成的有界 writer 方案。这里直接测得的是 codec 大小，未执行真实 writer admission/延迟测试；仍需 CAS 引用和 owner staging。

真实性边界：Unsafe 仅跳过 Android runtime 构造，不替换所执行的训练/codec/publication/predict 方法。本轮没有调用 owner claim、fence、ResearchWriteQueue 或 SQLite，没有模拟 DB 提交成功；文件是合成 host fixture，不是原设备模型。真实子 JVM halt 在已强制写出的 checkpoint 之后，证明跨进程 codec 恢复，不证明 Android 生命周期、CAS rename 中途或数据库提交原子性。surface prediction 是真实方法执行，不宣称产生 live DB model_call/frozen receipt。没有 Android、自然质量、act/K3 全主链或 24h/72h 结果。

保留了准备期间的原始失败，没有删除后只留下绿灯：

* `20261104T101944Z`：首次独立编译缺 P02 新增 DirectoryCursorR31 linkage source，原 compiler error 留存；扩充实际源文件后重编译。
* `20261104T102009Z`：首轮全部九位置反例及跨 JVM 恢复真实执行；尚未增加每个预训练 work 边界测试。
* `20261104T102144Z`：P02 并行更新 Runtime 新增 FeatureCache 方法，独立 compile 缺其对应新 source；原 compiler error 留存。随后精确纳入 FeatureCache/SharedFeatureStore 源码。
* `20261104T102250Z`：预训练数值比较已走完，但 harness 将 TreeSet 传给生产 JsonR27 日志序列化，产生 R27_JSON_TYPE；该轮整体明确记为 HARNESS_OR_EXECUTION_FAILURE。已将日志字段转 List，未改变训练或断言。
* `20261104T102314Z`：包含新增预训练边界检查的完整执行，结果见上表。十个产品合同断言依旧失败，保留原异常。

重跑命令（始终只在 `/tmp` 编译，输出本准备目录）：

```bash
python3 verification/r31/p03_preparation/repro/run_counterexamples.py
```

这是 baseline 反例 runner，期望九个 partial assertion 和一个 identity assertion 为 exit=1；若修复后这些合同通过，该 runner 会报告“与 baseline 失败预期不同”，届时应新增明确的修后回归 runner/receipt，不得覆盖这里的失败证据或把 expected failure 名义改成产品 PASS。
