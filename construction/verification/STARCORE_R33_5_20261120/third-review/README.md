# R33.5 第三次复核的验证原件

验收起点为仓库提交 `041ba168a0da48e4b0479db34cbaa2e420a7d31d`。本轮按[七组修订](../../../plans/STARCORE_R33_5_REVIEW_20261119/REVIEW_THIRD_20261120.md)验证。源码或物料的身份均以[本轮汇总](VERIFICATION.json)和[文件 SHA](SHA256SUMS)为准。

| 环节 | 实测证据 |
|---|---|
| 认证发布原包、两份现场报告和当前组合 | [原卷重组](assembly.log)、[重组后的 23 项宿主命令](check-plan.log)。前次原件字节身份可追溯到[两报告 SHA](../second-review/original-report-sha256.txt)。 |
| 百期判定 | [同一快照的修前修后对照](timing-comparison.json)。墙钟最大 500ms；注入的实际处理耗时 1,500ms，从开奖锚定的时延最大 1,801ms。旧版 PASS，本版 INCOMPLETE。100 期均为可重放宿主输入。 |
| 批次安装、版本和方案绑定 | [九批逐次安装](nine-stage.log)、[最终已提交的九批源码](final-source.json)、[最终 P00 方案绑定回读](final-plan-binding.json)。从 033 的真实 Git 工作树执行 P00 至 B08，每批提交与核原 R967。实际运行环境为浅历史，版本观察用预演输入。正式交付需要完整 Git 历史。 |
| 工单、签名接收、全案验收 | [定向运行和复现原件](regressions.zip)包含工单范围、时序、历史与文件模式、错误内嵌身份与 ZIP、缺报告的“改善”、不全条款、九批收件的正反例及其原输出。签名收件测试使用构造的边界材料。 |
| 全源码编译与报告链 | 比对本次和前次的生产 overlay、安装 manifest 和生产身份相同。对应的[十次全源码类型检查](../second-review/prefix.log)、[396 项报告断言](../second-review/report-closure.json)和[115 项原件保全检查](../second-review/custody.json)保留其原执行身份。本次重新从 B06 实际源码编译五份正式资格类，原资格宿主 31 个断言通过。 |

本次产生方案、自检和源码预演的回执。尚待正式施工产生九份新原签交付物、同构建现场原件、设备 24/72 小时观察及独立自然数据。B08 须用完整模式分别交代工程与原始义务，再按真实采样给出策略质量。
