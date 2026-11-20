# 给 Codex 的单次全批施工指令

将下面整段作为一个任务执行。验收原件的参数和目录示例见文末。

```text
在 GitHub 仓库 gtt46461-star/starcore-public-build 执行 STARCORE R33.5 第三次复核版的全部 Android 施工。授权范围是 P00、B01、B02、B03、B04、B05、B06、B07、B08 九批的源码实施、复测、逐批构建、按现有授权签名、同步仓库与交付。一个任务从开工负责到最终交接。每批保存检查点后自动推进，无须等我逐批发“继续”。

1. 从仓库根目录 CURRENT_FINAL_PLAN.md 取得方案，以 construction/plans/STARCORE_R33_5_REVIEW_20261119/ 为当前修订目录。先记录实际方案提交，核 FINAL_SCOPE.json 为 R33.5-REVIEW-20261120-R3，阅读 REVIEW_THIRD_20261120.md、ISSUES.md、WORK_ORDERS.json 和 HANDOFF.json。下载 Release 标签 r33.5-plan 的 33.5.zip，用当前 prepare.py 在工程外的新目录还原。在还原出的 current-review 下运行 check_plan.py，核对同级原卷 6,276 件。以 WORK_ORDERS.json 的当前来源、附件与完整前后态施工，逐件解决 66 个工单。基线为正式 033 提交 bb7b49bbcc9266bad548ec37e71403256fc06a4d，正式发包取完整 Git 历史。

2. 从该基线或已核验的同版前序检查点开始，每批走 current-review/replay/stage_checked.py 的 --apply-to 入口，从上一批真实提交移植到全新隔离目录。按工单从真实调用处贯穿生产、数据库、后台、消费、报告和 UI。P00 用 tools/r33/verify_plan_binding.py --init-binding 生成 release/r33_plan_binding.json，并与版本四文件一起提交；已有绑定则核原件。每批保存升级前本机 dumpsys 原件，以实际已安装版本递增，用 advance_release_version.py 执行版本事务，再运行原 R967 --check。核工作树、暂存区、文件模式、Git 提交和前序继承。已有用户改动或旧候选另行保全，修复失败项后在对应工单复测。

3. P00 完成构建、版本和签名基座。B01 打通存储限额、原作业恢复、原件保全和导出，验证热历史不超过 5,000 期。B02 打通每秒抓取、后台、每个启用期赛前影子冻结、唯一结算、反馈和重启恢复；原签设备实采启用日记 START/END，逐期核源头时钟、固定截止、提交返回及单调时钟。用至少 100 个应启用机会核前期开奖至正式冻结可观测 ≤5 秒、本期开奖至结算返回 ≤1 秒，列出缺期和全部超限。B03 验证真实家族、出生、回放、断点和消费。B04 验证真实梯度更新、检查点、十位置发布及线上读回。B05 接通原子、矩阵、策略族生成、反馈学习、进化、变异、融合和淘汰，保存父子配对、实际动作和消费证据。B06 严格分开 R27 与 SU2 自然协议，核独立未来、证书、撤销与唯一 DecisionGate。保持单位置固定 K=3 和 PIT；合格者依现有授权进入主链，证据未足继续影子采样。B07 逐项验证图表及逻辑表的数据来源、真实空集、缺源、分页、能力缺口和处方，用两份真实报告、对应 Git 改动与原始日志验证修复效果。B08 在同一最终候选上复验全部。

4. 每批独立执行原工单的正例、反例、重启或恢复用例。跑 source/preflight_delivery.py，再用受权流程 run_release_build.py、package_ci_candidate.py 构建。每批交付已有真实链接的原签 APK、源码 ZIP、可从空库恢复的 Git bundle、APK-SOURCE-EVIDENCE 总 ZIP、Git 提交和 SHA-256。上传后从交付地址重新取得原件，在接收端核包名、实际版本、原证书、APK 内嵌源码身份、原始 Gradle 日志、打包件和源码树。签名私钥只留在原授权环境。B06/B08 的业务文件操作数为零，各自的版本、构建、现场和交付仍须完成。

5. 在工程外保留九批可续跑台账，逐批记工单 ID、输入/输出提交、修改路径、原件、命令与退出码、下载链接、剩余现场窗口。构建或上传中断就诊断、重试、回读，并从最后验证过的检查点恢复；有可执行的独立工单就继续。用 replay/verify_completed_source.py 核九批源码，用 replay/verify_all_deliveries.py 从收到的九份材料逐一验收。最终用 replay/final_review.py --deliveries ... --require-full-closure 对同一 B08 的短链、百期、三次报告导出、真实复报、升级和 vivo V2353A 的 24/72 小时原件终审。对 32 组、96 断言、288 场景、222 条款和 1,832 项需求分别列出通过数、分母及剩余 ID。重新构建改变 APK 或源码身份后，按合同重新采集受影响的同候选证据。

6. 最后交一张九批总表、66 项工单的真实状态、仓库提交、可直接下载的最终 APK 与完整总 ZIP，以及全部回执的路径和 SHA。另列工程签收、设备实测和独立自然表现，给出出手/机会分母、命中、区间、稳定性与证书。真实设备、原签授权或自然时长确实不足时，写明具体缺件、已完成部分和可直接恢复的步骤；在环境允许时持续采集。签收严格依据可重算原件，任何待验项都保留，禁止填造数据、事后补冻、降门槛或将历史胜率写成本次结果。
```

## 开工命令

```bash
REVIEW="$(git rev-parse --show-toplevel)/construction/plans/STARCORE_R33_5_REVIEW_20261119"
WORK="$(mktemp -d)"
curl --fail --location --retry 3 -o "$WORK/33.5.zip" \
  https://github.com/gtt46461-star/starcore-public-build/releases/download/r33.5-plan/33.5.zip
python3 -B "$REVIEW/prepare.py" "$WORK/33.5.zip" "$WORK/prepared"
PLAN="$WORK/prepared/STARCORE_R33_5_20261118"
REVIEW="$WORK/prepared/current-review"
python3 -B "$REVIEW/check_plan.py" "$PLAN"
python3 -B "$REVIEW/verification/check_work_orders.py" "$PLAN" --emit-to "$WORK/effective"
```

在已核实的正式 033 完整 Git 来源上执行批次安装。首次将 STAGE 设为 P00；SOURCE 是实际输入检出，OUT 是尚不存在的输出目录。先创建其父目录。

```bash
python3 -B "$REVIEW/replay/stage_checked.py" "$STAGE" \
  --plan "$PLAN" --repo "$SOURCE" --apply-to "$OUT" > "$EVIDENCE/stage-receipt.json"
```

## 九批实物与全量验收

复制 [deliveries-template.json](replay/deliveries-template.json)，按 P00 到 B08 填实。每行的 `candidate` 是收到的总包解开后的候选目录，`dumpsys` 是该批升级前的原始包观察。路径相对于索引文件所在目录，也可用绝对路径。`deliveryZip` 可指定另存的收到的总 ZIP；默认按打包器原名寻找。设备命令用 `adb -s "$DEVICE_SERIAL" shell dumpsys package com.starcore.research.r13`。总包 ZIP 须保有原件。

```bash
python3 -B "$REVIEW/replay/verify_completed_source.py" \
  --plan "$PLAN" --repo "$B08_SOURCE" --review "$REVIEW"
python3 -B "$REVIEW/replay/verify_all_deliveries.py" \
  --plan "$PLAN" --repo "$B08_SOURCE" --deliveries "$DELIVERIES" \
  --baseline-apk "$APK033" --aapt "$AAPT" --apksigner "$APKSIGNER" --out "$NINE_RECEIPT"
python3 -B "$REVIEW/replay/final_review.py" --require-full-closure \
  --plan "$PLAN" --repo "$B08_SOURCE" --candidate "$B08_CANDIDATE" \
  --deliveries "$DELIVERIES" --dumpsys "$B08_DUMP_BEFORE" --baseline-apk "$APK033" \
  --aapt "$AAPT" --apksigner "$APKSIGNER" --execution "$EXECUTION_JSON" \
  --evidence "$RAW" --stage-receipt "$B08_STAGE_RECEIPT" --start "$START" --end "$END" \
  --window-bundle "$WINDOW" --followup "$FOLLOWUP" > "$FINAL_RECEIPT"
```

B08 的总 ZIP 另存时也给终审传 `--delivery-zip "$B08_DELIVERY_ZIP"`。每条已登记复报链各传一次 `--followup`。三次导出使用 `replay/prepare_export.py`；研究原件格式见[复报合同](research/README.md)。全量交付额外检查九份真实签名物料。工程全项、长测与自然效果各按受检身份输出；累计自然样本以实际采集进度为准。
