# 新授权完整资料上传：恢复包完整性复核

## 结论与本轮范围

**恢复完整包的完整性通过：`RESTORED_COMPLETE_PACKAGE_INTEGRITY_PASS`。** 本轮只核保存归档、恢复文件、原 manifest、原诊断 56 个 payload 与 5 组候选描述符，并运行随包原验证器；没有重新审全部问题，没有修改恢复包、应用或远端。

用户新增的完整公开授权由根任务确认。本报告不再沿用此前仅公开精简方案的发布边界；但原归档里的旧边界说明、旧发布回执属于当时历史，须在本次发布入口明确更新其适用范围。当前完整性通过不代表新远端上传已完成。

核验路径：

- 保存交付归档：`/workspace/scratch/189d5130b5de/recovered/STARCORE_R34_4_211_20261128_Repair_Plan.zip`。
- 恢复完整方案：`/workspace/scratch/189d5130b5de/recovered/STARCORE_R34_4_211_Delivery/complete_plan`。
- 本轮新验证结果：`/workspace/scratch/189d5130b5de/audit/authorized_upload_verify.json`。该结果写在恢复包外，未覆盖包内历史 `validation/PLAN_CHECK.json`。

## 实际执行及结果

执行的是随包原有只读工具，未指定 `--source-root`：

```bash
python recovered/STARCORE_R34_4_211_Delivery/complete_plan/tools/verify_plan.py \
  --output audit/authorized_upload_verify.json
```

退出码为 0。结果为 `verification_pass=true`、`plan_integrity_pass=true`、`failure_count=0`。8 项 PLAN 检查及 PATCHSET、MANIFEST 两项包检查均通过。`source_verification=NOT_RUN`，原因是 `SOURCE_ROOT_NOT_PROVIDED`；`candidate_execution=NOT_RUN`。本轮没有重跑补丁累计应用、Android 构建、JUnit、设备运行或自然质量测试。包内已有的源码 PASS 和主机日志保留为原交付时的历史验证，不能改称本轮重新执行。

|对象|本轮实际核验|结果|
|---|---|---|
|保存交付 ZIP|文件长度、完整 SHA-256、ZIP CRC、263 个文件成员与恢复磁盘逐成员散列比对|通过；缺失 0、内容变化 0|
|完整方案文件集合|155 个实际文件对照原 manifest 152 个条目及 3 个明确排除项|通过；未列文件 0、缺文件 0|
|原 manifest|152 条 byte count 与 SHA-256、`MANIFEST.sha256`|通过|
|原诊断外层与内层 ZIP|外/内 ZIP 文件成员完整读取并校验 CRC、各自 SHA-256、原 manifest 的 56 个 payload|通过|
|覆盖与原报告对应关系|原验证器对照原报告，不将 FAIL/UNKNOWN/READ_ERROR 改为产品 PASS|通过|
|5 组候选|C01–C05 路径均存在，补丁字节 SHA-256 与 PATCHSET 一致|通过；只验证文件完整性，未应用或执行|
|源码与产品|未提供 source-root；未构建或运行 Android|明确 NOT_RUN，不扩大结论|

原 manifest 的 3 个排除项是 `MANIFEST.json`、`MANIFEST.sha256`、`validation/PLAN_CHECK.json`，用于避免自身循环散列。文件集合与这一约定完全相符。

### 三层归档身份

|层级|字节数|SHA-256|
|---|---:|---|
|完整交付 ZIP|20,814,251|`4770e865a14cb3c4411e24d180368077c4585cdaa40255945a72742d615f9915`|
|原诊断外层 ZIP|19,471,817|`e0e3ffdb5a58a33175d6ee305d788bfaea3bd6186495c1828a0e1edddb6112b0`|
|原诊断内层 `STARCORE_FULL_REPORT.zip`|19,744,957|`7a1bfecfb1499e511d1dabf7d01bc70d291464da07d17433f477672faae73f94`|

完整方案原 `MANIFEST.json` SHA-256 为 `9eab1f2fecfb98ec69d67b5b93634cbfe521e2ad4cf5e11212eb5e59b6fe6790`。

原诊断保留 `checksComplete=false`、`allChecksPassed=false`。归档校验通过只是原件完好，不表示诊断业务检查全部通过。原 zstd 分卷描述符仍保留；未附的原分卷字节不声称已重新回放。原 `.scdev`、完整用户数据库及模型/训练原字节未因这次恢复或新增授权而自动补齐。

### 五组候选恢复状态

|候选|补丁|完整性|本轮执行范围|
|---|---|---|---|
|C01|`candidates/runtime/heartbeat_scope_identity.patch`|SHA-256 与原 PATCHSET 一致|未应用、未重跑|
|C02|`candidates/runtime/coverage_scope_join.patch`|SHA-256 与原 PATCHSET 一致|未应用、未重跑|
|C03|`candidates/strategy/ST02_queue_category.patch`|SHA-256 与原 PATCHSET 一致|未应用、未重跑|
|C04|`candidates/strategy/ST06_fifth_training_recovery.patch`|SHA-256 与原 PATCHSET 一致；相关精确身份候选文件恢复|未应用、未重跑新增 JUnit|
|C05|`candidates/charts/R18_exact_receipt_projection.patch`|SHA-256 与原 PATCHSET 一致|未应用、未重跑|

五组候选都仍为 `CANDIDATE_ONLY_NOT_APPLIED_TO_APPLICATION`。不能把新增上传授权解释为已经执行产品返修。

## 新授权发布须同步的旧说明

**原保存 ZIP 必须原样保留。** 对外的新发布入口和新的发布回执承担“当前授权范围”的说明；归档内原 manifest、旧回执与旧文案作为历史字节保留。若另生成修订后的可读文本副本，应使用新 manifest，并明确区分“原件复现”与“当前发布说明”，不能继续引用旧 manifest 声称修订文件字节未变。

|位置|现存旧表述|本次应同步的当前发布语义|
|---|---|---|
|归档根 `README_DELIVERY.md`|public_plan 为 106 文件/4 候选；完整诊断只留在交付包，不再公开上传|作为旧交付说明保留；新入口明确本次已获完整公开授权，包含原交付归档、完整方案文本、5 候选及原诊断恢复方式|
|归档根 `PUBLICATION_RECEIPT.json`|`raw_diagnostics_publicly_uploaded=false`、public_candidates 仅 C01/C02/C03/C05、旧 commit/tree、此前审批处理说明|保留为历史发布回执；另写本次实际上传后回读的新 receipt，记录新 commit/tree/路径/归档 SHA/5 候选及真实远端核验结果|
|完整方案 `README.md:1`、`CONSTRUCTION_PLAN.md:1`、`CODEX_EXECUTE.md:1`、`SELF_CHECK.md:1`|只有 4 组公开候选，C04 仅在完整包；公开位置以包外旧 receipt 为准|当前入口说明这些是归档时的旧范围。本次完整资料与 C04 已获授权，活动链接指向新完整交付，而不是旧精简版|
|`README.md:24`、`CONSTRUCTION_PLAN.md:11`、`CODEX_EXECUTE.md:9`|独立分支只发布不含原始诊断的公开版；main 未变|新发布页据本次实际目标分支和提交说明；不能把旧发布事实当作本次事实。原应用底座仍保持精确 211，不因公开范围变化而改动|
|`PLAN_IDENTITY.json`|`edition=COMPLETE_USER_DELIVERY_WITH_PRIVATE_ORIGINALS`、`public_edition=SEPARATE_PLAN_ONLY_BRANCH_WITH_FOUR_CANDIDATES`|旧身份保留在原件；新的发布身份应明确 `AUTHORIZED_COMPLETE_PUBLIC_DELIVERY` 或等价语义及 5 候选|
|`MANIFEST.json`|`edition=COMPLETE_USER_DELIVERY_NOT_FOR_PUBLIC_REPOSITORY`|该字段是历史封包标签。新发布 wrapper/manifest 明示新增授权及原 archive 原样保全，不通过篡改原 manifest 来消除旧标签|
|此前公开版 README、CANDIDATES、PUBLIC_SCOPE、SELF_CHECK、coverage/inheritance 说明及分支入口|不含原诊断/完整台账/C04，只有 4 候选|保留旧版本历史，但活动入口应标为“已被完整授权交付补充/取代”，链接到新的完整范围；不得继续将这些精简边界描述成当前总交付范围|
|当前自检记录|包内旧 source PASS 与历史主机 PASS|当前恢复自检明确 `source_verification=NOT_RUN`、`candidate_execution=NOT_RUN`，原 56 payload 和恢复字节核验已实际重跑|

完整 `PATCHSET.json` 和 `CANDIDATES.md` 已正确包含 C01–C05，无需重写。公开范围变化也不要求重写全部 46 项问题、任何应用源码或有效逻辑。

## 原 archive 一份入库、原诊断一键恢复的验收合同

根任务拟以完整交付 archive 原样入库，并提供解压文本与一键恢复原诊断脚本，避免同一大型二进制重复保存。这一结构可保留全部原件，但新入口应准确说明 clone 后的状态：

1. 仓库内保存完整交付 archive 的原字节；恢复前校验其长度与上述 SHA-256。
2. 一键恢复脚本从固定成员 `STARCORE_R34_4_211_Delivery/complete_plan/evidence/ORIGINAL_DIAGNOSTIC_211.zip` 取出原诊断，不以重新压缩替代原字节。输出必须是 19,471,817 字节及上述原诊断 SHA-256。
3. 原诊断在解压文本树中尚未还原时，原 `verify_plan.py` 的完整原件验证应诚实报告缺件。说明书先给恢复命令，再给原验证命令，不能声称“clone 后原完整 manifest 已通过”而省略这一步。
4. 若解压文本保持原样，则恢复原诊断后可继续使用原 manifest 验证 155 文件；新入口、授权说明、恢复脚本及新验证结果放在该原件树之外并另列当前发布清单。
5. 恢复脚本应固定安全成员、流式校验、临时文件后原子落位；同路径已有正确文件可幂等复用，遇到不一致原件应明确报错并保全，不静默覆盖。
6. 上传后才生成新的远端发布回执；本报告没有执行或证明任何远端写入。

## 最终判定

恢复包、原诊断 56 个 payload、5 候选及原 manifest **完整性通过，无恢复损坏或缺失待修项**。当前剩余工作属于根任务的授权发布说明、归档/可读文本入口、一键恢复脚本以及真实远端上传回读；不需要重新审查全部问题，也不需要改动恢复包原字节。
