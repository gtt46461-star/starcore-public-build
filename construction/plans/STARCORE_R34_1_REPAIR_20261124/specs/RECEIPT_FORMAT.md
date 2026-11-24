# 实际执行回执和72小时原件格式

空模板在 `control/RECEIPT_TEMPLATE.json`。字段名是 `tools/verify_receipt.py` 的真实接口；本文件说明如何形成可核验的输入，不提供虚构的通过记录。

## 普通批次回执

`plan_manifest_sha256`填当前 `control/PLAN_SEAL.json`的manifest摘要；`source_before/source_after`填真实40位Git提交。两者都须在122应用底座之后且顺序可证；ENGINEERING回执不能只有版本元数据、测试文件或生成产物变化。已有真实实现只做重验时，`revalidation_only=true`且附原实施回执字节/摘要，仍须本次全部场景重新执行。

`issue_ids`列本批完整工单；`test_results`逐项列本批相应level全部验收ID。每个结果记录真实argv、整数退出码0、hash绑定原始日志和非空测量断言。断言支持eq、ge、gt、le、lt、in；null不变成0，布尔不冒充计数，NaN/Infinity不能通过。

`artifacts`每项为相对 `evidence-root` 的文件路径和真实SHA256；不允许绝对路径、越界、重复项、摘要不符。每条断言的 `evidence_artifact` 必须指向列出的原件。测试命令由施工者审阅后实际执行，验证器不执行回执中的命令。

验证器返回 `PASS_RECEIPT_STRUCTURE_AND_DIGESTS`，其含义只限于结构、身份、范围、数值断言和原件摘要通过。需审阅生产测试、输入真实性和业务语义后，才能在实际台账填写相应工程结果。APK构建/签名/安装另有证据，不能因该状态自动生成APK_VERIFIED。

## B12的DEVICE回执

本批DEVICE覆盖 `A-B12-02a` 真实覆盖安装以及 `A-B12-04a` 完整后台campaign。最终窗口必须至少 **259200秒真实墙钟**；24小时截点为中期证据，不能代替72小时最终窗口。窗口带时区，例如 `+08:00`；同APK、source、设备身份，不拼接换包前后。

顶层 `background_campaign_artifact` 指向hash绑定的JSON原件，字段为：

```text
source_commit, apk_sha256, device_identity, window_start, window_end, fixture=false,
preregistered_calendar_artifact, opportunity_ledger_artifact,
boot_segments_artifact, interventions_artifact, raw_events_artifact,
checkpoint_24h_artifact, coverage_evaluator_artifact
```

这些 `*_artifact` 均指向同回执artifacts中的文件。原始boot分段保存单调时钟与墙钟锚点，跨boot不能相减；interventions保存每次手动打开页面、装包、网络/电源操作的发生时间与目的，不删不利干预。

机会日历JSON含 `targets`数组；每项含唯一 `target_key`及实际预登记时点/窗口/来源。机会账本JSON含 `opportunities`数组；每项至少含相同 `target_key`、`terminal_state`、`reason`、`role`，以及B04定义的时点/输入/提交证据。两者键集合必须一一相等且非空。没有业务产物的目标也须保留为明确missing/失败状态，不能从账本消失。

`coverage_evaluator_artifact`包含实际B04/B05预登记SLO、逐目标评价、全部机会分母、成功样本分母和最终判定。结构完整不等于性能达标，验证器不会靠存在一个JSON就替评价器判定性能。晚到、被替代、未开始、不可用、期限失守等仍计入各自失败统计。

## NATURAL回执

必须 `fixture=false`，绑定真实设备/APK/源码和观察窗口，并有hash绑定的 `preregistration_artifact`、`future_calendar_artifact`、`natural_evaluator_artifact`。自然评价需实际B09冻结评价器结果；原V1资格和质量投影分别报告。

这里的用例可以验证系统如实产生QUALIFIED、REJECTED或样本不足；验证器结构通过不意味着候选已QUALIFIED。应用是否获得高品质资产，应读取真正的评价结果、所有条件和合法证书，不能把“完成评价”解释为“具有未来优势”。
