# 冻结选择器的第二阶段训练

新 Number/Gate/Ranker 改变最终选择器，不能复用旧选择器的可信度样本。R932 提供两阶段真实路径：

1. 用 `training.r929.pipeline` 完成上游 OOF 训练，用 `training.r932.export_bundle` 生成研究模型包。导入后默认不是 PROVEN。
2. 保留导入的原始签名 ZIP 和对应模型所有者公钥 DER。让 APK 继续积累新的赛前冻结数据；新数据包含最终选中 action 的 rank score、ordinal、选择器 hash、模型 bundle hash 和独立可信度12维输入。旧缺字段数据不能补造。
3. 将新数据导出并运行 dataset 阶段。所有使用样本必须晚于父包全部组件的训练终点，且选择器和父 bundle 一致。
4. 运行下列命令，固定父包前三组件的原始字节，仅训练 CAL、独立 Predictability 和 POLICY。CAL 内训练/验证之间隔离整期，POLICY 联合选择概率与可信度阈值，SEALED 使用全局一次性登记。

```bash
python3 -m training.r932.frozen_selector \
  --models NEW_CAPTURE_DATASET_DIRECTORY \
  --parent-bundle ORIGINAL_SIGNED_RESEARCH_BUNDLE.zip \
  --owner-public-key MODEL_OWNER_PUBLIC.der \
  --runtime-contract EXPORTED_RUNTIME_CONTRACT.json \
  --owner-key PRIVATE_MODEL_OWNER_KEY.pem \
  --output SECOND_PASS_RESEARCH_BUNDLE.zip
```

公钥和私钥必须属于原模型所有者；私钥路径只在本机读取，不写入产物。父包的 Number/Gate/Ranker 字节、各自 DATA hash、expiry 保持不变；新的后段 DATA hash 与三份父组件 hash 进入签名证书依赖声明，客户端逐项验证。原应用 APK 签名密钥与模型所有者密钥属于两个独立用途。

无可行策略明确 FAIL_EFFECT，eligible/selected/REAL 分母分列，离线 REAL 恒为0。缺 score、缺可信度、父模型漂移或选择器变化直接阻断。导出的策略研究单元为 inactive；此流程不伪造 Android、资源或真实 Future 证据，不授予 BET/REAL。已经 PROVEN 的父组件需要匹配的新证明链，不能通过改写父模型字节绕过客户端门。

验证：`scripts/r932_frozen_selector_regression.py` 检查更早冻结父选择器→新独立训练→所有者签名→真实 Java/SQLite 导入；前三组件逐字节相同，错误父绑定即使重新签名也拒绝，SEALED 标签变化不改变拟合/阈值。合成测试不是设备和真实 Future 效果。

