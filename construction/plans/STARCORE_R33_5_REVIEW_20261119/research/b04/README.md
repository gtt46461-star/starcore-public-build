# B04：真实训练期计数和检查点续训补正

本补正归入既有 B04 的 `R335-C03` 与 `R335-RE16`，沿用原阶段顺序；不是自然效果证明。唯一生产文件是 `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`。

## 1. 首个梯度切片提前宣称已训练全部历史期

原 `report()` 在 `pretrainSamples>0` 时直接返回 `trainCount/10`。实际 JDK17 数值 owner 试验登记 160 个历史 TRAIN 期、1600 个位置样本，完成首个 64 样本梯度切片时只访问了 55 个不同历史期，却报告 160。

修后 `independentTrainingCalendarIssuesR335()` 从第一轮已执行的 shuffle 前缀去重 `sampleIndex/10`，实际返回 55；同一期多个位置只计一次。完整一轮后才返回整个 TRAIN 集期数，多轮反复训练不增加这个分母。无梯度时为 0；历史不一致无法重建时为 null。报告增加明确范围：这是历史 TRAIN 期去重计数，不是独立自然 FUTURE 样本。当前编码格式、学习率、网络、优化器、64 样本预算和数值算法不变。

## 2. 缺失 shuffle 的非零 TRAIN 游标可错误续训

原 `validateCheckpointR335()` 允许 `phase=TRAIN, offset=64, order=null`。为这个无效状态重新计算正确 SHA 后，旧 decode 接受它；下次 `work()` 重新洗牌，却从旧 offset 继续。实际运行与未损坏检查点比较，下一梯度的权重不同，说明跳过/重复了已登记样本顺序。

修后拒绝非零 TRAIN 游标缺失 shuffle、TRAIN 游标已达 trainCount、TRAIN epoch 已达 MAX_EPOCHS 的非法相位。合法 epoch 末尾由原 `transition()` 进入 EVAL，照常恢复；不得将坏原件静默清零、重建随机顺序或标记已完成。全部原件保持不变，实际恢复与错误处置仍由原 owner 负责。

## 施工与身份绑定

| 项目 | 内容 |
|---|---|
| 原 B04 完整后态 SHA256 | `7bff4f5d67818acd808dfa09b09a0c2d2987c1a28042bb3a2d92fbeb66ef4342` |
| 本次完整后态 SHA256 | `f61d69c3e879a8aff104b39a0efeaea47b0ae8852170d59db2be12d0fecc18d6` |
| 安装位置 | 原 B04 替换文件的完整后态，由新的 production overlay 接管 |
| 后续保持 | B05、B06、B07、B08 保留相同 owner 字节 |
| 数值身份 | 原角色保留 NUMERIC；使用实际累计源码调用原 identity()/R967 重算 B04 及后续阶段的 R82，`predecessors=[]`，不继承旧资格 |
| 共享旧修订 | B05 的 DiagnosisEvidenceR335 配对补正继续叠加；不能拿旧 B05 R82 常量覆盖新累计身份 |
| 不变内容 | 原网络与优化器数值公式、训练相位转换、协议门、资格门、授权、数据库格式 |

本目录保留修前完整文件、修后完整文件、差异补丁、测试输入和运行原件。正式安装以仓库当前阶段 overlay 清单为准；不要手动复制源码后跳过 R82/R967 与批次清单。

## 重跑

使用 JDK17，将本目录设为 `B04_REVIEW`，`PLAN` 为已验证原方案：

```bash
python3 -B "$B04_REVIEW/run_review.py" "$PLAN" "$B04_REVIEW/AtomNetSessionR30.before.java" --mode legacy
python3 -B "$B04_REVIEW/run_review.py" "$PLAN" "$B04_REVIEW/AtomNetSessionR30.java" --mode fixed
```

`legacy` 必须复现原错误；`fixed` 必须准确计数并拒绝三个非法 TRAIN 游标，还运行原完整 `AtomNetSessionNumericalProbe`。修后该原测试实测 17,763 个检查、166 个 work 切片、60 个梯度切片、102 个评估切片、3,200 次预训练样本访问、20 epoch 和 10 个在线位置更新，断点续训保持原数值字节一致（按原测试只排除两项实测耗时）。这些是固定样本的实际生产数值 owner 证据。DB 发布、完整 APK 和设备运行另验。
