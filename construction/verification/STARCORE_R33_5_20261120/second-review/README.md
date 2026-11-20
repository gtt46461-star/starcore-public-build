# R33.5 第二次复核的实际回执

本次把正式 033 的两份检测报告、源码提交、发布原包和仓库修订放在同一追溯链上。当前方案、批次和处置说明见[第二次复核](../../../plans/STARCORE_R33_5_REVIEW_20261119/REVIEW_SECOND_20261120.md)。[验证汇总](VERIFICATION.json)列出复验输入身份、最终方案封存摘要、工单数和未执行现场项目。

| 证据 | 实际执行及结果 |
|---|---|
| [原件还原](assembly.log)及[两份设备报告 SHA](original-report-sha256.txt) | 从 626,717,930 字节发布原包组装 6,276 个认证文件。独立取得的两份外层 ZIP 与组装方案中同名作业的 SHA 分别完全一致。 |
| [组装后的总验收](check-plan.log) | 从 `prepared-final/current-review` 运行 21 条命令，复读 406 条报告处方和 2,474 项原字段，同时覆盖时间边界、启用分母、短链、签收、版本继承和逐工单索引。 |
| [全量源码前缀](prefix.log) | BASELINE033、P00、B01 到 B08 均通过每个阶段 7 条原生成器命令和 JDK 17 / Android API 36 的全 Java 类型检查。 |
| [九批安装记录](stages.json)及[按最终封存重验的源码](final-source.json) | 从关联浅工作树沿施工方的 `--apply-to` 逐批安装，核版本、原 R967 及累计字节。删除 B07 新类的独立提交被[完成源校验拒绝](missing-file-negative.log)。另用正式预演入口从同一基线重新运行，生成[第二组源码回执](source-rehearsal.log)。 |
| [标签与日记生产源码](production-probes.log) | 在实际 B02 Java 上执行 24 个宿主断言。SQLite 游标、ContentValues 和 XML 读取使用夹具；业务逻辑来自候选源码。 |
| [报告链](report-closure.json)和[原件保全](custody.json) | 396 项报告断言及 115 项保全检查通过；其中报告链复核 7 份实际生产源码。 |
| [当前工单索引检查](work-orders.log) | 66 原工单、9 阶段、164 次操作、138 条不同路径。从安装器再导出当前所有前后完整文件，得到的 manifest 与全量编译使用的 manifest 逐字节一致。 |
| [原始回执包](execution-receipts.zip) | 含每阶段全量 Java 输入摘要、生成器命令和日志、两组九批源码收据，以及 B02/B08 定向回归。成员完整性见[SHA 清单](SHA256SUMS)。 |

前缀编译及首组 Git 回放保留各自开始时的方案封存 SHA。单独比对生产 overlay、有效安装清单、最终源码输入及第二次组装后的最终封存，具体身份见 VERIFICATION.json。033 旧报告、宿主测试、现场监控和独立自然未来分账。新候选原签安装、真实 Android IPC、百期实际时延、vivo V2353A 的 24/72 小时连续运行及策略自然效果仍待正式施工及设备原件。
