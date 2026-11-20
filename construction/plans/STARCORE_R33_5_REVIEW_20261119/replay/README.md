# R33.5 当前批次安装、源码继承与终审取件

本页以2026-11-20的完整生产修订为准。范围为 **9阶段、66项施工问题、164次文件操作、138条不同生产路径**，逐项数量见[最终范围](../FINAL_SCOPE.json)，问题归属见[本轮复核记录](../REVIEW_SECOND_20261120.md)。原封包142次/129路径及2026-11-19的双文件继承检查属于历史输入，不能单独作为当前完成证明。

`PLAN` 指恢复并核验后的原 `STARCORE_R33_5_20261118`；`REVIEW` 指同级的当前 `current-review`，或仓库中本轮复核目录。两者均放在应用工作树外。以下路径变量须取本次作业的实际绝对路径。

## 九阶段使用同一个完整后态入口

安装顺序固定为 P00 → B01 → B02 → B03 → B04 → B05 → B06 → B07 → B08。从正式033提交 `bb7b49bbcc9266bad548ec37e71403256fc06a4d` 的真实Git工程开始；后续批以紧前批已完成版本事务、审阅并提交的工程为输入。每批输出到新的外部目录，输入工程保持原样。

```bash
python3 -B "$REVIEW/verify_review.py" "$REVIEW"
python3 -B "$REVIEW/replay/stage_checked.py" P00 \
  --plan "$PLAN" --repo "$SOURCE_033" --apply-to "$P00_OUT" > "$P00_STAGE_RECEIPT"
python3 -B "$REVIEW/replay/stage_checked.py" B01 \
  --plan "$PLAN" --repo "$P00_COMMITTED" --apply-to "$B01_OUT" > "$B01_STAGE_RECEIPT"
```

两个示例分别在该批实际开工时执行。P00的版本、提交及当批签收完成后再运行B01，依此类推。B06/B08的业务安装差异为零，但各自的版本、测试、签名、设备和原方案验收职责仍然存在。前一批按自己的合同签收，不借用尚未施工的后一批代码。

入口自动调用 `production_overlay.py`，先组成原批次完整后态与上次B05配对修订，再应用[当前生产补正清单](../production/OVERLAY.json)及所引用的完整前后源码。临时有效清单覆盖全部九批，原 `validate_install_manifest` 检查每个文件摘要、全部成员、操作数和共享路径前后关系。当前各批操作数为18、37、40、9、8、37、0、15、0，总计164次；跨批重复路径重复计操作。

清单中的 `originalAfterSha256` 标识被修订的旧批次有效后态；`beforeSha256/beforeFile` 标识本次实际前序工程字节，二者不可混用。本批原未登记的路径，其 `originalAfterSha256` 为null；目标在前序工程已存在时仍须使用REPLACE和实际前态，真正新增才使用ADD/NEW。不得绕过清单只手抄Java片段或沿用历史R82摘要。

B02生产时序和启用日记、B04训练计数/恢复修订、B07诊断和能力提案全文，以及每个相关阶段的R82/R967完整身份，都在同一有效清单中。两个已跟踪的R967生成文件从P00起随受影响批次安装和提交。B05旧配对Java修正继续保留；2026-11-19记录的 `4cd79138…`、`b58acd80…` 等R82摘要仅用于历史原件辨识，当前身份以本轮完整源码生成的清单和原R967校验为准。

## 每批核版本落点、HEAD、工作树和索引

`ancestry` 先核033祖先、紧前阶段、全部有序版本祖先及严格递增的versionCode，用固定的原R926生成器逐字核对版本四文件。`registered_prefix` 随后累加有效批次的所有生产路径，并分别检查：

- 实际引入该阶段版本的Git提交中的累计生产后态；后批覆盖代码不能掩盖前批版本提交时漏交。
- 当前入参HEAD中的累计生产后态；仅允许当前待施工批的精确后态作为已部分移植的重试。
- 工作树与索引真实字节；`assume-unchanged`、隐藏本地改动或已暂存差异不能绕过继承检查。

四个动态版本文件由原R926生成器检查，其余已登记生产路径按本轮有效后态逐项检查。发现未知字节、漏交、回退或用户改动时拒绝并保留原件。若某批历史版本确实漏交，先补齐该批、重新执行同批版本事务并提交可核实后态，再进入后续批；不得只复制阶段标签。已经施工过旧修订的分支保留原样，从可证正式基线建立独立检出按本版重放，或按工单完成保留原代码的三方移植和完整复验。

回执保存 `orderedCommittedVersions`、`registeredPredecessorChecks`、`originalLedgerSha256`、`reviewManifestSha256` 和 `productionReview`。后者记录当前overlay、有效完整清单的SHA及实际文件数量。重复检查须使用当次原输入提交和对应输出目录，不能将旧收据改写成新候选。

每批安装之后，按该批施工卡完成原方案绑定、实际安装版观察和版本四文件事务；将全部已审阅后态与动态版本文件提交。提交前执行原 `scripts/r967_semantic_identity.py --check`，提交后进入[可移植交付预检](../source/README.md)。测试、同源构建、原签APK和现场原件各按该批合同签收，安装器成功只证明其声明的源码范围。

## 最终B08必须再验全部九批

B08完成实际版本事务与提交后，运行：

```bash
python3 -B "$REVIEW/replay/verify_completed_source.py" \
  --plan "$PLAN" --repo "$FINAL_SOURCE" --review "$REVIEW" > "$COMPLETED_SOURCE_RECEIPT"
```

该入口重新组成当前164次操作/138路径的有效清单，核P00至B08九个真实版本落点、最终HEAD、工作树与索引全部累计后态。它不再只核B05 Java/R82两件。最终 `final_review.py` 同样调用此入口，并要求重新生成的 `productionReview` 与本次B08移植收据一致。

本轮已在真实033 Git对象上完成九阶段隔离源码回放，随后独立完成源核验；在另一克隆中删除本轮B07新增的 `CapabilityNeedRegistryR335.java` 并提交后，终验以 `REGISTERED_PREDECESSOR_BYTES_MISSING:B08` 拒绝，原正例工程保持clean。最终隔离提交为 `6bba61d08cf3494817082010b2410050144509d0`，回放版本为036至044；这些版本值标识本次预演，正式安装仍依据现场真实安装版递增。

本次回放使用浅克隆，033根及九阶段提交均可见。因此它证明已列源码、版本与身份继承，不证明完整历史bundle、正式APK、原签覆盖安装、设备长测或自然策略资格。证据范围见[验证说明](../verification/README.md)。正式交付仍须用完整历史并在空仓库恢复bundle。

## 可重复的源码检查

```bash
python3 -B "$REVIEW/replay/test_registered_prefix.py" --plan "$PLAN"
python3 -B "$REVIEW/replay/test_production_overlay.py" --plan "$PLAN"
python3 -B "$REVIEW/replay/verify_effective_stages.py" \
  --plan "$PLAN" --review "$REVIEW" --source "$SOURCE_033" --out "$NEW_SOURCE_REHEARSAL"
```

前两项分别覆盖7项累计继承回归及3项有效清单回归。第三项从真实033对象建立一个新的隔离检出，逐批应用本轮有效后态、执行版本事务、提交并核原R967及clean状态，输出实际九阶段收据。输入源码不被修改。它仍是源码预演，输出版本观察不替代目标手机的真实安装观察。

`test_stage_checked.py` 显式使用无生产overlay的legacy夹具，保留旧封包/pair的九阶段兼容回归。`tests-run/` 中原安装器12用例、原终审器14用例和旧报告取件记录均是历史宿主证据，不能代替上面本轮完整overlay正向回放。原方案的Android、设备与自然效果验收继续执行。

## 三次报告的终审输入

原 `B08/终审计分与证据协议.md` 使用嵌套zip示例，但现行原 `score_final.py` 对 `repeatExports` 每条读取平铺的 `path,sha256,jobId,profile`。原导出核验器读取内ZIP根层的ARCHIVE_MANIFEST、source-manifest和语义收据；手机单卷外包内的index.json与STARCORE_FULL_REPORT.zip需先核实并取件。

```bash
python3 -B "$REVIEW/replay/prepare_export.py" --plan "$PLAN" --evidence-root "$EVIDENCE" \
  --outer "$PHONE_ZIP" --job "$JOB_ID" --commit "$CURRENT_COMMIT" \
  --apk-sha "$CURRENT_APK_SHA256" --profile DIAGNOSTIC
```

工具先核单卷外层index的作业、卷数、内ZIP字节数、SHA及manifest身份，再调用原核验器核当前sourceCommit、APK、jobId、实际profile和必需成员。成功后原子保存内ZIP并输出可直接登记的JSON行。FULL_RAW按实际profile填写；外层原包和index随交付保留。直接导出的内ZIP按相同身份走原核验器。多卷先按实际卷清单和每卷SHA完整还原，不拼接缺卷或截短数据。

三个不同作业各保存一行；历史033报告与新候选分别登记。报告字节结构、研究功能和策略质量各用对应证据评价。

## 同源工程终审

本次B08移植命令的stdout保存为 `STAGE_RECEIPT`，保留其输入提交。`EXECUTION` 是原32组终审输入，`EVIDENCE` 为原件目录。B07.4.TRACE的rawArtifacts登记全部真实后续对照链JSON；nextCase.commit绑定最终候选。仅有中间批复报时保留阶段证据，继续取得最终候选复报。

```bash
python3 -B "$REVIEW/replay/final_review.py" --plan "$PLAN" --repo "$FINAL_SOURCE" \
  --stage-receipt "$STAGE_RECEIPT" --candidate "$CANDIDATE" --dumpsys "$DEVICE_BEFORE_UPGRADE" \
  --baseline-apk "$BASELINE_APK" --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt" \
  --apksigner "$ANDROID_HOME/build-tools/36.0.0/apksigner" --execution "$EXECUTION" \
  --evidence "$EVIDENCE" --start "$START_CAPTURE" --end "$END_CAPTURE" \
  --window-bundle "$ENABLED_WINDOW_MANIFEST" --followup "$FOLLOWUP_CHAIN" > "$FINAL_JSON"
```

有多份研究回访时重复传入 `--followup`；所交集合须与本次B07.4.TRACE登记集合一致。B08.2原始短链取POS/TRACE原件。百期时窗共用同一manifest时可省略 `--window-bundle`。运行器核当前review与原封包身份、完整九批已提交源码，再实际调用动态版本/签名/构建验收、原score、百期合法标签时窗和研究trace/followup。每次原件不足输出INCOMPLETE和原因，退出码非零。

`VERIFIED_32_ENGINEERING_GROUPS_AND_REVIEW_CHECKS` 只计算工程及本页复核范围；222条款、1832需求和各次回访分别报告实际分母。当前候选REAL授权、独立自然资格、真机24/72小时依原协议分别签收。当前源码预演不提供尚未取得的APK与设备原件。
