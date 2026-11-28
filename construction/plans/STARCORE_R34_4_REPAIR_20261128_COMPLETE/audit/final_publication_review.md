# COMPLETE 最终出版自检

## 结论

**当前出版内容与执行合同复核通过：`PASS_CONTENT_AND_CONTRACTS_PENDING_FINAL_MANIFEST`。本审计范围内没有未关闭的正文、卡表同步、物化合同或新入口链接问题。** MANIFEST及PLAN_CHECK由根任务最后生成，本轮按要求未将旧散列当作缺陷，也没有预签最终封包通过。

本轮只读检查 `publication`；审计结果写在 publication 外。没有运行产品测试、修改应用或 publication，也没有操作远端。

## 实际核验结果

|范围|结果|
|---|---|
|46卡/原发现|46个规范ID、50个origin/alias一致；全部original_audit_records与原完整包一致|
|验收矩阵|46行的title、batch、preservation、acceptance与ISSUES逐对象完全一致；各卡均保留对应preserve/repair/acceptance文字|
|批次|12批、卡数与台账对应，无DAG循环|
|源锚|136锚文件及1407生产Java基线清单与恢复原件逐字节相同；136锚仍恰等于所有卡source_files并集|
|执行模板|46卡、12批成员对应；NOT_STARTED/NOT_RUN，无默认完成或假PASS|
|候选|C01–C05共5组；PATCHSET与原完整包相同，五补丁散列匹配|
|归档分片|27片的顺序、长度、SHA256及Git blob SHA全部重新只读核验通过|
|整体归档|拼接内存字节为20,814,251，SHA256为4770e865…15f9915；263文件成员CRC全部通过|
|诊断物化|REPOSITORY_PACKAGE固定成员精确对应原19,471,817字节诊断ZIP及原SHA，不混淆交付ZIP与诊断ZIP|
|当前链接|369个当前入口/计划内部相对Markdown链接均有效；代码和数学表达式不当链接|
|检查总数|33项断言全部通过|

本轮没有211源码工作树，因此“136锚/1407基线不变”是与原交付清单字节的比较，不是重新验证1417个应用源文件。所有46项产品状态仍是`PLAN_DEFINED_PRODUCT_ACCEPTANCE_PENDING`。

## 三个根入口与历史边界

已纳入 `CURRENT_FINAL_PLAN.md`、新增更新后的根 `README.md` 以及 `STARCORE_最终施工入口.md`。当前首段均指向 `_COMPLETE`、准确211底座和5组完整候选，明确资料上传没有产生新返修APK。

根README中的152入口、R29/R13记录，及最终入口中的173记录均已明确置于历史标题下。本轮只核新增活动链接和历史分界，没有扩展审查原历史文档。原 archive 内旧四候选/旧PUBLICATION_RECEIPT也明确作为旧发布事实，不再被当成当前完整授权限制。

## 执行与物化合同

CODEX中的`PLAN_DIR`指向新COMPLETE目录，现有CLI参数与随包脚本匹配；先prepare_evidence，再核manifest及运行verify_plan，解包和执行回执输出到方案外。prepare_evidence写回的唯一受控原件在方案内，但已有不同字节会拒绝覆盖，说明已明确这一例外。

无source-root时现行结果字段写为`source_verification="NOT_RUN"`及`SOURCE_ROOT_NOT_PROVIDED`；另建准确211基线做原锚和候选验证，实际施工后代另走增量合同。B00先做驱动基础设施及本批runner，不把未来B01—B11业务实现反设为B00前置。每批独立升级路径和用户设备不得反复降级、两套不同的10场景验收也已分别说明。

本轮发现并已关闭两处表述：

1. 包README现明确prepare只恢复并核原诊断外层身份，之后必须另运行原verify；不会把物化成功称为56 payload和manifest已全验。
2. SELF_CHECK历史表已改为“原交付时执行（历史）”；1417源文件核验指向原ZIP内历史PLAN_CHECK，而当前PLAN_CHECK的源码层保持未重跑。

## 最终封包应纳入的新增文件

当前计划manifest重建时须包含下列新增项及本次已修改正文，保留原诊断的物化后descriptor。当前MANIFEST、MANIFEST.sha256和最终PLAN_CHECK仍按避免循环散列的原合同排除自身：

- `.gitignore`
- `B00_EXECUTION_ADDENDUM.md`
- `CREATION_EXECUTION_ADDENDUM.md`
- `EXECUTION_LEDGER_TEMPLATE.json`
- `PUBLICATION_AUTHORIZATION.json`
- `REPOSITORY_PACKAGE.json`
- `audit/authorized_upload_review.md`
- `audit/authorized_upload_verify.json`
- `audit/instruction_runtime_review.md`
- `audit/instruction_strategy_review.md`
- `tools/prepare_evidence.py`
- `validation/INSTRUCTION_REVIEW_CLOSURE.json`
- `validation/PUBLICATION_CHECK.json`

若将本审计JSON/Markdown复制进计划的audit目录，也须先复制再重生成manifest；不要在封板后又追加未列文件。

计划外的三个根入口，以及packages目录的README、还原脚本、PACKAGE_PARTS和27分片，不在计划内MANIFEST的自然范围。须由仓库出版文件清单/提交树和远端回读另行覆盖，不能声称计划内manifest单独证明整个仓库新增集合。

## 尚待根任务的封板步骤

- 重建当前MANIFEST与摘要；原archive身份和SOURCE_ANCHORS不改。
- 在原诊断已物化的最终目录运行原verify_plan，写新的PLAN_CHECK；没有source-root仍如实NOT_RUN。
- 对最终仓库新增/修改集合与原27分片进行提交及远端回读核验，才签已上传。

这些是已计划的封包收尾，未被列为内容缺陷。本轮没有宣称Android、设备、自然质量或新远端发布通过。
