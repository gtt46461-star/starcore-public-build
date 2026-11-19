# R33.5 批次移植和终审取件勘误

本目录随仓库修订发布。解压的方案目录记作 `PLAN`，本 README 所在目录记作 `REPLAY`。以下命令使用两个变量的实际绝对路径。

## 九个版本依次交接

原 `verification/contractor/stage.py::prepare` 查当前批的前后 SHA 与 033 祖先。B06、B08 的生产文件数都是零，从原033直接请求这两批也能生成空差异。原测试 `test_b06_empty_key_diff_is_not_gate_acceptance` 体现了它的定位。把工具退出码用作下一批开工凭证，会跳过已有批次。

每批统一调用本目录的移植入口。P00 使用实际033或包含该根且受控字节相符的工程。后续每批从上一批完成原四文件版本事务、审阅并提交后的工作树继续，下一批自行分配全新输出目录。

```bash
python3 -B "$REPLAY/stage_checked.py" P00 --plan "$PLAN" --repo "$SOURCE_033" --apply-to "$P00_OUT"
python3 -B "$REPLAY/stage_checked.py" B01 --plan "$PLAN" --repo "$P00_COMMITTED" --apply-to "$B01_OUT"
```

`stage_checked.py::ancestry` 对 HEAD 确认原033祖先，核验紧前版本，再沿真实 Git 图回查 P00 至紧前批每个阶段的有序祖先及严格递增的 versionCode。每个阶段都从已提交的 release_manifest.json 取原始字节摘要，用已固定的原 R926 生成器逐字比对 properties、ReleaseManifestR926.java 和 BuildConfig.java。B06 完成独立构建和验证后同样运行 `tools/r33/advance_release_version.py` 并提交四文件，B07 才能据此前进。原 `prepare` 继续负责当前批的文件、摘要、冲突和独立工作树。若当前工程使用人工三方移植，保留用户的改动与原提交，按施工卡复核移植结果；另须保留真实的逐批版本提交和回执。只提供源码 ZIP 时可执行离线结构检查；生产祖先检查须使用带对应对象的 Git 工程。

B05 的 `DiagnosisEvidenceR335.java` 安装本次完整后态（640bd8c8…）。同一批再装原 R967 生成器复核的 `release/r82_training_invalidation.json`（4cd79138…）。B07 以前述新 R82 为前态，安装本批的 b58acd80…。原 NUMERIC 角色只登记一次。B06 至 B08 核输入 HEAD 和版本祖先中的有效 Java 与对应 R82；旧包已提交到多个阶段时，从保留的 B04 工程提交开新的工作树重放，或逐批完成带新版本、当前证据的移植，保留原分支。普通 B05 的 `--emit`、`--apply-to` 和部分已移植重试均走同一组新摘要。若多个接收方同时施工，各自由新输出目录接收。

移植回执增加 `orderedCommittedVersions`、`originalLedgerSha256`、`reviewManifestSha256`。正式入口校验仓库复核目录全部登记文件，并将清单字节摘要绑定到回执。对 B05 和 B07，`ledgerSha256` 识别实际生效的前后态账本，原包账本身份另存。检查覆盖源码版本链。业务门仍以当批测试、同提交构建、原签 APK、相应运行与设备原件签收。对同一阶段的重复检查应使用当时的已提交输入和相同输出目录。

## 三次报告的终审输入

`B08/终审计分与证据协议.md` 描述 nested `zip` 引用。现行 `verification/score_final.py::validate` 实际把 `repeatExports` 每条直接传给 `one`，因此规范字段取平铺的 `path,sha256,jobId,profile`。另外，`B01/scripts/verify_export_artifacts_r334.py::validate_export` 读取 ZIP 根层的 ARCHIVE_MANIFEST、source-manifest、R31_SEMANTIC_RECEIPT 等；手机分享的外层 ZIP 内只有 index.json 和 STARCORE_FULL_REPORT.zip。直接投递外层文件会拒绝。

对手机一个作业的单卷 `SCREPORT_SINGLE_ZIP_R992` 外包，先保留原包，然后运行以下命令。参数取本次发布候选和作业实际身份。

```bash
python3 -B "$REPLAY/prepare_export.py" --plan "$PLAN" --evidence-root "$EVIDENCE" \
  --outer "$PHONE_ZIP" --job "$JOB_ID" --commit "$CURRENT_COMMIT" \
  --apk-sha "$CURRENT_APK_SHA256" --profile DIAGNOSTIC
```

工具先核外层 index 的作业、卷数、内 ZIP 字节数和 SHA，再核 index 中的 manifestSha256 与内层 ARCHIVE_MANIFEST.jsonl。随后调用原核验器，逐项核当前 sourceCommit、APK、jobId、实际 profile、必需成员和原始摘要。通过后才在证据根内原子保留内 ZIP 并打印可直接放入 `repeatExports` 的 JSON 行。FULL_RAW 取实际 profile。原始外层和 index 随交付保留，`allChecksPassed` 原值另列在业务报告上。报告字节结构通过与研究性能达标分别记录。

如已拿到应用直接导出的内 ZIP，用原 `verify_export_artifacts_r334.py` 按同样身份核验，登记该文件的实际相对路径和 SHA 即可。多卷格式先按实际导出的卷清单/各卷 SHA 完整还原，经原收件逻辑形成完整报告后再验。三个不同作业各保存一行，POS/TRACE 按原终审器和固定合同提交；新候选和033历史分别登记。

## 可重复的核查

```bash
python3 -B "$PLAN/verification/contractor/test_stage.py"
python3 -B "$PLAN/verification/test_final_envelopes.py"
python3 -B "$REPLAY/test_stage_checked.py" --plan "$PLAN"
python3 -B "$REPLAY/test_export.py" --plan "$PLAN"
```

`tests-run/` 保留本次四条命令的实际输出。原安装器 12 个用例、原终审器 14 个用例通过。新增版本链测试在原包的实际前后态和版本生成器上构造隔离 Git 历史，遍历九阶段并调用实际修订入口。测试覆盖无差异跳批、四文件身份漂移、缺历史阶段、B05 已装旧源升级、B05 补丁回放、B05–B08 同源重试、B06 已有输出上文件漂移，以及 B06/B07/B08 各自拒绝旧 Java 或旧 R82 的六组回退。新增报告取件测试重开两份033原始外 ZIP，拒绝错误 jobId，实际核对内件 SHA，按历史模式复现两份的已知 schema 缺陷，按新候选模式均拒绝。实际033生产 Git 的逐批签名构建、三份新候选手机报告及 V2353A 运行仍需对应原件。

## B08 修订终审入口

最终同源工程验收运行下列命令。用本次 B08 的移植命令将 stdout 保存为 `STAGE_RECEIPT`，并随交付保留其输入提交。EXECUTION 是原32组终审输入，EVIDENCE 为其原件目录。B07.4.TRACE 在实际证据封套的 rawArtifacts 中登记每份要签的后续对照链 JSON（firstCase、work、nextCase）。对应的 nextCase.commit 填最终实际候选；若只取得中间批复报，保留其阶段证据，并继续获取最终候选复报。

```bash
python3 -B "$REPLAY/final_review.py" --plan "$PLAN" --repo "$FINAL_SOURCE" \
  --stage-receipt "$STAGE_RECEIPT" --candidate "$CANDIDATE" --dumpsys "$DEVICE_BEFORE_UPGRADE" \
  --baseline-apk "$BASELINE_APK" --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt" \
  --apksigner "$ANDROID_HOME/build-tools/36.0.0/apksigner" --execution "$EXECUTION" \
  --evidence "$EVIDENCE" --start "$START_CAPTURE" --end "$END_CAPTURE" \
  --window-bundle "$ENABLED_WINDOW_MANIFEST" --followup "$FOLLOWUP_CHAIN" > "$FINAL_JSON"
```

有多份研究回访时重复传入 `--followup`；程序比对本次 B07.4.TRACE 中登记的全部后续对照链。B08.2 的原始短链从 POS 和 TRACE 原件取回。百期时窗若共用同一 manifest 可省略 `--window-bundle`。运行器核仓库修订清单 SHA、原方案 seal、已提交 B08 源码及其 B05 Java/R82 后态，再次实际调用动态签名/构建核验、原 score_final、受控百期时窗和研究 trace/followup。当前候选 REAL 授权和自然质量各依原协议另验。输出 `VERIFIED_32_ENGINEERING_GROUPS_AND_REVIEW_CHECKS` 只计算工程及上述复核项，并列出 222、1832 实际通过数与各次回访结论。半套原件输出 INCOMPLETE 和缺项或首个失败原因，退出码非零。只提交以前的 score 会明确列出缺失的现场输入。本次已在 `test_final_review.py` 实测上述拒绝路径。完整签名 APK 与真机原件齐备后才可实跑该正向验收。
