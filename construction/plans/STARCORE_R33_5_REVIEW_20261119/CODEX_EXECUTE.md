# 给 Codex 的 R33.5 完整施工指令（2026-11-20 第二次复核）

在 `gtt46461-star/starcore-public-build` 仓库执行。当前方案已在仓库，入口是根目录 `CURRENT_FINAL_PLAN.md`；本轮完整修订在 `construction/plans/STARCORE_R33_5_REVIEW_20261119/`，先读 `README.md`、`REVIEW_SECOND_20261120.md`、`WORK_ORDERS.json`、`FINAL_SCOPE.json`、`HANDOFF.json`、`ISSUES.md`。记录实际方案Git提交、`REVIEW_SHA256.json`摘要和`production/OVERLAY.json`摘要。原有66项业务工单、P00与B01—B08九阶段继续执行，累计V01—V25均按所属工单执行，不另开互相冲突的施工路线。

## 取得可以执行的完整方案

从仓库当前复核目录运行 `prepare.py`，使用Release标签 `r33.5-plan` 的 `33.5.zip`，按照README核对发布附件的SHA256。工具自动还原手机包缺失/旧版成员，并带出本轮全部生产后态、安装工具、补正卡和验证脚本。打开输出目录的 `START_HERE.md`。原封存方案为 `STARCORE_R33_5_20261118`（PLAN），同级 `current-review` 为当前修订（REVIEW）。不要只执行发布原包里的旧stage入口，也不要使用未上传完的历史75分片路线。

取得正式033提交 `bb7b49bbcc9266bad548ec37e71403256fc06a4d` 的真实Git对象和完整源码。对实际输入创建独立checkout/worktree，保护原分支和已有改动。用 `$PLAN/verification/verify_baseline_source_r335.py` 所属原核验逻辑核实际来源；从033起步，或从已保存的本方案合法前序阶段继续。发布阶段必须有完整Git历史，不能用只有文件的ZIP或浅克隆代替可恢复bundle。方案目录放在工程外，避免把施工材料当应用文件编入。

## 逐批实施

1. 先核原66项问题清单、该批问题卡、完整前后源码和本轮补正；`production/OVERLAY.json`是新增生产字节的统一入口。依P00→B01→B02→B03→B04→B05→B06→B07→B08顺序，用 `python3 -B "$REVIEW/replay/stage_checked.py" Bxx --plan "$PLAN" --repo "$PREVIOUS_COMMITTED_SOURCE" --apply-to "$NEW_OUTPUT"` 施工。P00填写P00。输出目录须全新、位于工程外。
2. 对照本批真实差异审阅。执行入口核前序每个版本实际提交和累计生产文件、当前工作树/索引，不可只抄版本号。B02装入七份Java生产后态和Android清单，B04采用本轮AtomNet完整owner；B05保留配对修订；B07采用7份报告生产全文。B06/B08独立签收。若已有较老R33.5阶段，先保全原分支，从最后相符阶段重新移植；不强行略过哈希不符。
3. 按 `tools/r33/advance_release_version.py` 完成版本四文件事务，版本严格高于当前源码、设备实际安装版和033。`production`已将R967两项已跟踪生成文件纳入逐批安装；提交前执行原 `scripts/r967_semantic_identity.py --check`，将本批所有生效源码及生成文件一并审阅提交。工作树、索引和已提交字节必须一致。使用原语义/来源生成器，不削弱validator、不降数值角色，不借用旧资格。
4. 逐批运行原验收与本轮POS/NEG/TRACE：B01原件/存储/导出；B02完整启用机会和严格冻结/结算；B03真实家族训练与恢复；B04真实梯度/检查点/十位置发布消费；B05分析、动作、进化/变异/融合/淘汰及配对；B06原独立自然资格、撤销与DecisionGate；B07真实报告读取、需求分类、后续复报；B08全链同构建终审。失败原因写入对应问题、批次和文件，修好再验。
5. 构建前执行 `source/preflight_delivery.py`，使用完整SDK、原033 APK、真实Git历史。按原 `run_release_build.py` 与 `package_ci_candidate.py` 的受权签名流程构建，核包名、实际版本、原证书、源码提交和构建回执。每批输出可直接下载的APK、完整源码及可恢复Git bundle，记录SHA256和真实链接。设备实际安装、重启与后台持续运行需要现场回执。

## 主链目标和验收

将来源→预登记→冻结→结算→反馈→训练→研究动作→影子/独立未来→资格→主链授权→后续反馈接通。严格保持单位置K=3、原固定截止、PIT与原资格/撤销协议。按原授权把达标证书路由进唯一DecisionGate；没有合格证据时记录原因并维持正常影子与后续采样。生成策略方法不设终身数量上限，同时对当前驻留、队列、模型和原件实施预算与淘汰。高胜率目标用独立未来与同机会对照检验，历史高值仅作为研究输入。

B08先用 `replay/verify_completed_source.py` 核完整九批源码，再以真实候选与现场原件运行 `replay/final_review.py`。百期窗口自身核合法标签时间和明确停用分母；三次导出走 `replay/prepare_export.py`；真实复报链绑定最终候选与实际实验时段。原32组、96断言、288场景、1832需求和222条款分别给出真实分母。源码预演、JDK类型检查、APK、真机24/72小时、自然策略质量分别报告结果。

## 必须回传的交付

给出本批/总进度、解决的问题ID、修改文件与前后身份、原件与测试结果、Git提交、APK和源码下载链接、签名和SHA256、设备与自然样本状态。发现任何缺件/失败，继续可完成的已授权工作，并把未验证项与获取办法写入账本。不要将历史收据、新版号或宿主测试当作本次真实应用已全部完成，也不要在没有实际下载链接时声称已交付APK。

## 第二次复核的逐项执行

在准备好的 `current-review` 执行 `check_plan.py "$PLAN"`。查 `WORK_ORDERS.json` 按工单 ID 取得当前批次和文件版本。需要审阅完整代码时执行 `verification/check_work_orders.py "$PLAN" --emit-to "$NEW_EFFECTIVE"`，其输出与安装器的字节相同。B02 按 V19—V22 落实施工并保留正反例，特别核验早标签、同哈希重试、事务内时间与提交返回时间相差、延迟开奖固定截止，以及原签非调试机 START/END 两次独立日记读回。B08 的短链、百期和正式交付共同使用本轮入口。向接收方交付后，在空 Git 库恢复收到的 bundle，并交回匹配该 APK 提交的源码 ZIP 核验记录。V24—V25 的版本历史和工单字节由安装器与索引复核。接收标准、按问题的文件和复现命令见[第二次复核](REVIEW_SECOND_20261120.md)。
