# STARCORE 当前施工方案 · R33.5 2026-11-20复核

当前完整入口为 [R33.5施工方案](construction/plans/STARCORE_R33_5_REVIEW_20261119/README.md)，直接执行的任务书为 [CODEX_EXECUTE.md](construction/plans/STARCORE_R33_5_REVIEW_20261119/CODEX_EXECUTE.md)。[本轮逐项审计](construction/plans/STARCORE_R33_5_REVIEW_20261119/REVIEW_20261120.md) · [当前证据与验收状态](construction/verification/STARCORE_R33_5_20261120/README.md) · [机器可读范围](construction/plans/STARCORE_R33_5_REVIEW_20261119/FINAL_SCOPE.json)。

本轮在已上传方案上补正，**9阶段、66项原业务问题，最终161次文件操作、135条不同生产路径**。V12—V18新增审计记录归入既有工单；8份本轮Java完整后态、安装前后字节、跨批语义身份、检测与能力需求接线已统一进入批次工具。目录沿用20261119名称以保持入口稳定，内容版本为2026-11-20。

1. [下载33.5.zip](https://github.com/gtt46461-star/starcore-public-build/releases/download/r33.5-plan/33.5.zip)，运行当前复核目录的prepare.py。工具还原第一卷62个缺件、17个旧件，验证6276件认证原件并带出本轮全部修订。无需继续历史75分片上传路线。
2. 在工程外保存方案，从正式033源码`bb7b49bbcc9266bad548ec37e71403256fc06a4d`的独立Git检出开始，或从已核对的本方案合法前序阶段继续。
3. 统一使用replay/stage_checked.py，依P00→B01→B02→B03→B04→B05→B06→B07→B08实施。工具组合原卷与当前完整补正，逐批核实际版本提交、累计文件、工作树、索引和生成文件；不要直接调用发布原包的旧stage入口。
4. 每批完成版本四文件、真实提交、APK构建签名、下载件、原件与现场验收；构建前运行source/preflight_delivery.py核工具链、原033 APK和完整Git历史。R967已跟踪输出已正式列入安装清单，提交前仍用原生成器--check。
5. B08运行完整源终验和同候选现场终审。源码预演与编译、正式APK、目标机24/72小时、独立自然资格分别记录。

本轮审计已修订方案与生产载荷，并保存当前组合的实际测试回执；应用实施、签名APK、真机和自然效果按任务书继续逐批完成。已有2026-11-19 P00补正中的有效要求已纳入当前工具和说明，历史记录保留用于追溯。

## 历史方案索引

R32.5.1总包、问题清单与验证范围见[历史交付目录](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/README.md)。该历史版为5批、18个联合问题、312条受控路径和410份完整文件卡；其[复核](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/FINAL_REVIEW_R3251.md)、[交付清单](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/DELIVERY.json)与[既有验证](construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/VERIFICATION.json)均保留历史身份。
