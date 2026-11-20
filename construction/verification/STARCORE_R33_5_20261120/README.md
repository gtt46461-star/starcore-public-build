# R33.5 2026-11-20方案复核：实际验证回执

**当前方案、完整生产后态、逐批源码安装和本页宿主检查均已完成本轮复核。** 当前范围为9阶段、66项原业务问题、161次文件操作、135条不同生产路径；V12—V18为新增审计补正。正式应用实施、签名APK、完整Git bundle恢复、目标机长测与独立自然效果分别按施工任务书取得原件。

[当前施工入口](../../plans/STARCORE_R33_5_REVIEW_20261119/README.md) · [本轮问题与代码](../../plans/STARCORE_R33_5_REVIEW_20261119/REVIEW_20261120.md) · [机器可读验证汇总](VERIFICATION.json) · [全部原始回执](RAW_SOURCE_RECEIPTS.zip) · [回执成员SHA清单](RAW_RECEIPT_MEMBERS.json)。

## 实际验证结果

| 检查 | 本轮实测结果 | 直接证据 |
|---|---|---|
| 重新下载并恢复发布附件 | 同一626717930字节原ZIP及SHA；6276件认证原卷；本轮修订完整带出 | [还原日志](final-assembly.log) |
| 从最终展开材料执行完整复核 | 16/16命令通过，实际采用最终发布seal | [最终封包自检日志](final-packaged-check-plan.log) |
| 固定033加九阶段全源码类型检查 | 10/10前缀通过；每个前缀7次原生成器，共70次；最终1359个Java源，另2个明确类型夹具 | [逐前缀摘要](PREFIX_SUMMARY.json)、[完整运行日志](final-prefix.log) |
| 原custody检查绑定最终源码 | 115项实际检查通过，完整核135路径及全部Java输入 | [绑定回执](custody/FINAL_PREFIX_BINDING.json)、[执行回执](custody/RECEIPT.json) |
| 两份033原报告与新增生产诊断链 | 396项Java断言通过；7份报告全文核验；完整最终源重编译 | [报告回执](reports/REPORT_CLOSURE_RECEIPT.json)、[实际读取结果](reports/host-report-readback.json) |
| 真033 Git对象逐批安装 | P00—B08九阶段通过；逐批原R967、递增版本、累计文件和clean；161次操作/135路径 | [九批汇总](execution/FINAL_EXECUTION_REVIEW.json)、[原始过程结果](execution/RESULT.json) |
| 独立终态复验与缺件负例 | 全九批复验通过；删掉新增B07类并提交后，终验明确拒绝 | [完整源终验](execution/COMPLETED_SOURCE.json)、[缺件负例](execution/NEGATIVE_SOURCE.json) |
| B04真实数值与恢复 | 17763项数值检查、166切片、60梯度切片、102评估切片、20轮、10位置在线更新；合法恢复继续通过，缺shuffle拒绝 | [B04修前/修后原件](../../plans/STARCORE_R33_5_REVIEW_20261119/research/b04/README.md)、最终封包日志 |
| 百期严格结算和分母 | 5项新回归；早标签拒绝，100启用+20明确停用通过，部分启用漏链拒绝 | [窗口回执](regressions/SETTLEMENT_WINDOW_RESULT.json) |
| 累计继承与清单防漏 | 7项继承回归、3项overlay回归通过；错写BO4批次实际被拒绝 | 最终封包日志、[独立负例](independent-review/OVERLAY_GUARD_RESULT.json) |

B04的17763是数值检查点数量，报告的396是断言数量，均不当作独立业务工单数。原66项工单的施工与现场签收分母保留。

## 同一最终源码身份

报告396断言、custody115检查和最终B08全源编译都绑定以下输入SHA：

`2095d3858b8dfaedb2548cebad6bb2c1eb1d8d607fdb6a567f40764094691697`

最终原R967验证的训练规则SHA：

`32875e6348372f9720fc9eec15091eee54c9c7b609e44085ac32e1bf49d8130c`

[有效完整清单](prefix/EFFECTIVE_APPLY_MANIFEST.json)的SHA为`c7c2877542543906b4323faf5a8e2c1c85d2f6d57fe28490885b2bfd5f766036`，与真实Git逐批回放的productionReview相同。最终隔离回放提交为`91a37801ab538d09eddf991378151078e0f292d5`；这只是源码预演提交，正式发布按每批真实受权构建执行。

最终发布review seal为`aec61040f174fdb91056e7549cec96c0a2b2d98f5175760f4f273078afc91257`，共161个封存成员，副本见[PUBLISHED_REVIEW_INPUTS.json](PUBLISHED_REVIEW_INPUTS.json)。这里的161个review成员与161次生产操作是不同计数，恰好同数。

全源前缀与九批Git回放的实际起始seal为`a6d103e1…`，115项custody为`ae90cfbd…`；这些收据保留开跑时的真实输入身份。本次随后完成文档收口与custody包装器更新，生产全文、overlay、有效清单和最终Java输入保持相同。最终取件及16命令已从新的seal完整复跑，不能改写旧收据的seal来假装同一时点。

## 原始证据归档

`RAW_SOURCE_RECEIPTS.zip`保存244个原始成员：十份全源码编译收据、全部生成器日志与生成输入副本、原/有效清单、基线绑定、逐批版本和安装收据、最终报告与custody收据、各项运行日志。仓库外临时目录名保留在原件中，用于解释实际运行来源；重跑用当前任务书的路径变量。编译class目录、完整重复工程和原设备数据没有纳入此回执ZIP。

ZIP为3581854字节，其SHA与每个成员的SHA见VERIFICATION.json及RAW_RECEIPT_MEMBERS.json。直接可读的关键收据与ZIP内原件保持相同字节；独立复核记录见[independent-review/CROSS_REVIEW.md](independent-review/CROSS_REVIEW.md)。

## 下一层签收

本次全源编译使用JDK17、Android API36类型库及明确的资源/BuildConfig夹具。报告测试实际执行生产Java读取与分类，但Android SQL、界面和目标机任务调度尚需现场验证。真实Git回放保留033和九次版本对象，所用检出为浅历史，不能作为完整bundle恢复证据。

施工方应按[CODEX_EXECUTE.md](../../plans/STARCORE_R33_5_REVIEW_20261119/CODEX_EXECUTE.md)逐批产生正式签名APK、源码、可恢复bundle和下载链接，核目标机24/72小时及独立自然资格。声明存在、发生梯度、工程检查通过与真实策略有效分别记录；本轮没有生成新的自然资格证书或替代设备记录。
