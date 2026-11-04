本次是源码只读诊断；没有运行新的 JVM、Gradle、长测或设备测试，没有改生产、测试、构建文件。原 `working-family-original-six.log` 为另一负责人已执行的工作版记录：6 项中 3 项资源等待失败，H06/H12 通过。不能将该日志归为本次源码或自然质量通过。

当前合法 AST 家族仅 F08/F11/F12/F20/F21（`PolicyAstR27:141-143`）。不存在 `TemporalModelR27.java`，实际接口是 `TemporalModelR26`；其约定是只清时序上下文、保留学习参数/优化器，不负责释放整个模型。

下表为源码可计算的 primitive payload，未包含对象头、引用数组、字符串、诊断对象；不是 profiler 实测，也不代表同时存活字节或泄漏。

| 位置 | 确定分配 | 规模和可达范围 |
| --- | --- | --- |
| `FamilyF11KernelR26:11-13,17,21-27` | `predict` 新建 weights/table；成熟 `update` 又创建 weights/table、gw、grad、proposed，并两次 `loss` 重建 weights/table | size=256 时 predict 2848 B，update 10872 B，合计 13720 B/期；3000 期最多约 41.16 MB payload。原 H07/Null 可达，episode 存储上限 256，属于重复分配，未证无限保留。 |
| `FamilyF20KernelR26:20-22,78,87` | 每 update `countVector` 拷贝 global + 全 capacity 个专家的更新前计数 | 默认 capacity=16 时 13600 B/期，5000 期 68 MB payload；capacity 上限 32 时 26400 B/期。另 `signature` 每次 1600 B。原 AST 可达；计数前后差分证明必须保留，不能直接删除。 |
| `FamilyF21KernelR26:14,16-17,24-27` | 每 update `fresh/copy` 整个 RelBayes，前后 loss 在内层循环创建 predicted[4] | `s/n` 两数组共 `6400*L` B，central L=2 为12800 B/期，5000 期64 MB，仅此两数组。两次 loss 另有 `3200*L` B 的 predicted payload 和1600 B score；属于原子更新 staging，不能改成半更新状态。 |
| `OnlineModelsSU2:149-152` | BOCPD 每个位置新建 grow[R]、nr[R] | 10 位置共 `160*R` B/update；central R=160 为25600 B，5000 期128 MB。F10 不在原 AST 允许列表，不能归为 H07 直接失败原因。 |
| `FamilyF01KernelR26:6-7,17,25,27` | 每 update clone 全 weights 和 biases，之后再 copy 回原参数 | 默认 L=3，仅参数 staging 为240800 B/update，5000 期1.204 GB 累计 payload。它用于原子训练，不是无限存储；F01 不在原 AST 允许列表。 |
| `ExactPermutationR26:18,21` | 每次 infer 分配两个 double[1024] 和 double[10][10] | n=10 时至少17184 B payload/infer。Neural predict 一次、update 前后各一次；复用必须由单模型独占 workspace，不能跨模型共享可变状态。 |

Neural 的固定 workspace 还有预算 shape 问题。`AutoDiffR26:5` 的 CAP=64000，两个 double 和四个 int 数组为2048000 B；paramNode[4096] 为16384 B；`NeuralFamilyR26:8` 的 weights/momentum 为65536 B，合计至少2129920 B primitive，尚未包含 recurrent/ring/对象头。模型 codec 仅约66 KB，不能把 codec 字节数当作对象工作集。`FeatureCacheR27.forkR31` 当时使用 `2*codec+256KiB` 的模型余量约395 KB，对神经模型不足。F03-F07 不在原 H07 的合法 AST leaf 列表，因此这不是已证的当前失败原因。已给单一负责人指出该写前预算缺口。

`SetIntersectionPolicyR30:15` 的 mask 每次创建 int[10]+double[10]，120 B payload；`FeatureCacheR27:191,193` 每合法行调用两次，3000 行累计约720 KB，再加数组头。`BirthGateR27:78-83` 对最后最多500行的真实弃权检查还调用 support 构造完整 Map。可考虑独占调用内 scratch 或纯数值状态接口，保留公开解释报告和同序 tie 处理；本次没有改写。

生命周期与新修复应分开判断。H07 的100×3仍完整，每 trial 已 try-close 自有 loop/cache；H12 在 snapshot 后关闭旧 loop 及其自有恢复 cache，再 restore；Q08 每步保存后关闭被模拟杀死的旧 simulation。NullEvolution 每一代的独立 shuffled 输入和 cache 有真实角色差异，不能跨输入复用数值结果。恢复时 `EvolutionLoopR27:653,657,662` 重新做 gate/trace 验证是当前公开恢复合同的实际成本，不能为了低分配跳过。当前源码未发现这些路径新的未关闭 cache 引用；关闭引用不等于证明堆已回收。

最新 working `FeatureCacheR27:445` 已对 `known==n` 返回本流 values，之前每个位置重复复制3000/5000行的2.4/4MB矩阵热点已在源码中消除。当前 `PolicyAstR27:148` 的非 leaf 公开入口也已使用 scope 后只复制最终输出；核心 temperature、replay、correlation 使用真实 scope/finally。这里仅确认代码变化，未替负责人执行或宣布通过。

初次 family 流的预算仍需统一：当时 `family:378,386` 正常初次/前缀重建调用 `new FamilyStream`，`advance:447-450` 仅 forkLease 非空时为100列矩阵申请 grow/overlap。3000/5000行矩阵为2.4/4MB，而外层 raw leaf 的输出预算约240/400KB，不能覆盖模型和整个矩阵。合法5家族矩阵分别约12/20MB，虽然有界，但初次和重建峰值仍应有所有者与写前准入。此项已交 FeatureCache 单一负责人，未重叠编辑。

优先措施应先完成当前 backing pooling、初次/重建/fork 预算和独占所有权，再用同一实际源码重跑原断言。模型内核 staging 可用每模型私有、有界、计入预算的 workspace 减少 churn，保留全部计算顺序、诊断、取消前检查和原子 commit；这是后续需独立证明的改进建议，当前状态为 NOT_IMPLEMENTED/NOT_RUN。
