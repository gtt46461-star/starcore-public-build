# 给 Codex 的 STARCORE R33.5 施工指令

在 gtt46461-star/starcore-public-build 仓库执行 R33.5。先读取 main 当前提交的 CURRENT_FINAL_PLAN.md 和 construction/plans/STARCORE_R33_5_REVIEW_20261119/README.md，记录实际方案提交及 REVIEW_SHA256.json 的摘要。下载发布标签 r33.5-plan 的 33.5.zip，运行 README 的 prepare.py。它恢复手机包缺失的修订、核验整包并带出当次宿主收据。打开输出的 START_HERE.md。若执行环境下载受限，可把同一 SHA 的 33.5.zip 放进本地再运行同一命令。

使用 033 正式源码 bb7b49bbcc9266bad548ec37e71403256fc06a4d 的真实 Git 祖先与当前工程历史。在隔离分支作业，保护现有改动。先读复核目录 reports/REPORT_RECONCILIATION.md、source/README.md，以及方案内 control/ISSUE_REGISTER_R335.json、control/APPLY_MANIFEST_R335.json、当批 README_R335.md 和对应 issues/。按 P00、B01、B02、B03、B04、B05、B06、B07、B08 顺序逐问题施工。每批用 replay/stage_checked.py，从前一批已提交的版本生成本批工程。执行 runtime、research、source、replay 四份复核卡对应的原件检查。B05 必须采用本版修正的父子统计 Java 源码及其 R82 身份，B06–B08 核对继承身份。B06 与 B08 都需独立版本、测试和签收。

逐期贯通来源、预登记、冻结、结算、消费回执、训练、研究、影子、资格、主链授权及后续反馈。单位置 K=3、截止时钟、PIT、原资格和撤销协议全程保留。按原件分别记录输入、执行、输出、消费、效果；样本缺失时标明缺哪一项、由谁采集。证明候选在合法独立未来达到相应规则后，按用户授权走原 DecisionGate。合成、重放、调参榜和图表行数各按其真实含义计量。

每批修复、回归、递增版本、提交完整源码并运行正式 Gradle/签名流程，交付可下载 APK、源 commit、签名摘要、设备升级及本批验收回执。用 source/verify_delivery.py 核现场安装号。B08 以同一候选及真实作业重跑 32 组、96 断言、288 场景，三次导出走 replay/prepare_export.py，再按 replay/README.md 运行 replay/final_review.py，由它调用原 score_final.py 并联查本次新增的证据条件。工程、24/72 小时真机和独立自然质量分别填写实测分母。无法取得的设备、签名或自然样本精确标记待验证，继续其他已授权工作。交付按问题、批次、文件和前后源码身份列结果，公开 APK 下载地址和核验摘要。
