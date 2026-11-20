# B01–B04 工具校正与 B02/B08 运行窗口签收

前提是已还原的原方案逐文件 seal 与当前仓库修订清单均已通过复核。`PLAN` 指已核验的 `STARCORE_R33_5_20261118`，`REVIEW` 指本页上一级的当前修订根目录，即准备结果中的 `current-review`。`TOOLS` 是方案外的新工具目录，`RAW` 是本次候选 APK 的取证目录。保持当前修订目录完整，长窗检查还会调用相邻 `research/check_research.py` 的共同时间校验。

生产施工按 [replay/stage_checked.py](../replay/stage_checked.py) 统一入口逐批安装原后态和 [production/OVERLAY.json](../production/OVERLAY.json)，包括本轮B02的写入、反馈、持久化返回和日记采集，以及 B04 owner、B05 配对与 B07 报告修改。当前逐阶段数值身份来自 [production/SEMANTIC_RESULTS.json](../production/SEMANTIC_RESULTS.json) 的原 R967 完整源码重算；B07/B08 最终训练规则为 `619fbc6ba89b6448e8eb663fd303c951b5fe7628ce0f91bfa6f18015ee629bac`，旧资格兼容列表为空。`prepare_tools.py` 只生成原验收工具的校正副本；它不安装生产代码，也不能代替当前累计 R82/R967 身份及正式构建核验。四个工具的当前前、后SHA-256统一列在[prepare_tools.py](prepare_tools.py)。

```bash
python3 -B "$REVIEW/verify_review.py"
python3 -B "$PLAN/verification/verify_package.py"
python3 -B "$REVIEW/runtime/prepare_tools.py" "$PLAN" "$TOOLS"
python3 -B "$REVIEW/runtime/test_review.py" "$TOOLS"
python3 -B "$REVIEW/runtime/test_settlement_window.py" "$TOOLS"
python3 -B "$REVIEW/research/test_research.py" "$PLAN"
```

| 工单 | 问题及修改 | 本阶段签收 |
|---|---|---|
| B01 / RT04 | prepare_exact_intake.Parts.read 原先使用第二次 stat 大小计算读取步长。分卷在清单校验后被截到当前偏移，单次读取会取得零字节，循环无法前进。改按已核分卷边界确定步长，遇短读立即退出并记录错误。 | 用 TOOLS/B01/recovery/prepare_exact_intake.py 替换原命令中的脚本路径。全部原 SHA、重复分卷、缺失子件和部分恢复规则继续执行。恢复成功另取 Android 按原作业和 checkpoint 哈希读回的收据。 |
| B02 / RE21、RT05–RT07 | verification/mainline/check_trace.py 的 REAL 概率与交易收据仅比较实际开奖和首次发现时刻。推迟开奖时，两个提交时刻都可能晚于固定截止。两处均增加严格“小于原冻结 deadline_at”。另用 check_window.py 逐期合并原独立启用日记、日历、SQLite 全链、标签实际结算时刻和完成时延。 | 用 TOOLS 中的 collect_enabled_window.py、check_trace.py 替换原相应调用。B02 及 B08 窗口执行下述当前 check_window 入口；独立 window bundle 自身也必须满足标签不早于开奖与首次观测。 |
| B03 / C02 | 核对本批 ReplayDatasetR27 前态、后态及回放测试。5000 条有效行、两处断档、重复/非法行、原件往返、取消与严格可见时点在本次运行验证。单独运行出生事务夹具，保留其宿主范围。 | 运行 TOOLS/B03/tests/run_replay_parity.py 和 test_birth_contract.py。真机仍须沿 FoundryCampaignR26、StoreR27 和 RuntimeR27 取得同一出生事务、父子血缘、精确样本与实际 checkpoint 的收据，以及 writer 拒绝/恢复时重复请求的独立读回。 |
| B04 / C03、RE16 | 原 run_session_numerics.py 对十二个类优先选择本批 payload，再回退 033 baseline，误选旧 ReplayDatasetR27；工具已改为本批 after_full、上一批 B03 after_full、已验证 baseline 的顺序并记录输入 SHA。本轮生产 owner 另修复训练历史期提前计满，以及非零 TRAIN 游标缺 shuffle 仍可恢复的问题。 | 原 TOOLS/B04/tests/run_session_numerics.py 保留为原数值与工具来源检查；当前生产 owner 必须再运行 research/b04/run_review.py。当前完整 owner 的原数值探针同样通过 17,763 项检查，新增计数/恢复反例见下文。目标机 dataset→checkpoint→十位置发布→consumer 继续独立验收。 |

## B04 当前 owner 的复测与计数边界

本轮完整 owner 随 `production` 在 B04 安装并保留到 B08；其 SHA-256 为 `f61d69c3e879a8aff104b39a0efeaea47b0ae8852170d59db2be12d0fecc18d6`。使用带编译器的 JDK17，并将其 `java`、`javac` 放入 PATH 后执行：

```bash
python3 -B "$REVIEW/research/b04/run_review.py" "$PLAN" \
  "$REVIEW/research/b04/AtomNetSessionR30.java" --mode fixed \
  > "$RAW/B04-current-owner-review.json"
```

实际十二个数值 owner 重新编译后的 [fixed-result.json](../research/b04/fixed-result.json) 记录：17,763 项检查、166 个 work slice、60 个梯度 slice、102 个评估 slice、20 个 epoch、10 个在线位置更新，权重 SHA-256 为 `5b17ecac4f369f0071031aa07e156232b5894be03363202146fa55308242705b`。这不是 17,763 次实际设备训练或独立开奖；数值探针的 `pretrainSamples=3200` 也包含跨 epoch 重复访问。数据库边界使用公开的抛错或类型接缝，该证据不代替数据库发布与现场消费验收。

另一个实际-owner夹具在 1,600 个 TRAIN 位置中执行首批 64 个位置，实际涉及 55 个历史期：旧版报告 160，修后报告 55。首轮按实际 shuffle 前缀去重，完成一轮后至多计 `ceil(trainCount/10)`；重复 epoch、SELECT 开发验证和 ONLINE 更新不增加该字段。`trainCount=69` 等非 10 整倍数、有效恢复和无法还原的部分训练历程均有边界测试。摘要正确但 `TRAIN/offset=64/order=null` 的旧 checkpoint 会造成恢复后权重分歧，当前 owner 明确拒绝，并拒绝不可能的终端 TRAIN 游标。详细字段语义、前后原件和生产 SHA 见 [research/README.md](../research/README.md) 与 [B04 复测说明](../research/b04/README.md)。

## 同候选的百期运行窗口

```bash
python3 -B "$REVIEW/runtime/check_window.py" --tools "$TOOLS" \
  --bundle "$RAW/trace.json" --evidence "$RAW" --start "$START" --end "$END" \
  --commit "$COMMIT" --apk "$APK_SHA" > "$RAW/B02-window-review.json"
```

START、END 和 trace 使用原 `B02/enabled_window_r335/README.md` 指定的真实只读原件，绑定同一候选 commit 与 APK SHA。预登记窗口保留全部日历期与独立启用日记，至少有 100 个启用机会才能签收。明确停用、漏冻、缺结算、迟到与未知各自计数；跨停用区间导致启用机会不足时，保留原窗口并另行预登记观察，不事后挑选成功期补成百期。

校验顺序为原启用日记与完整日历对账、原 trace 全链联接、同库标签时序、再逐机会计算时延。`check_research.py::settlement_times()` 读取当前 window manifest 绑定的同一冻结数据库，拒绝 WAL、SHM、journal 残留，检查前后数据库 SHA；对全部已联接标签要求时间为正整数且 **`settled_at >= max(open_time, first_seen_at)`**。当期任务在开奖后才 `completed_at`，不能掩盖标签已经在开奖前写入。B08.2 短链与百期长窗即使使用不同 manifest，也各自执行该严格规则；B08 终审通过 `--window-bundle` 提交独立长窗时同样适用。

每个启用适用期继续核对上一期源开奖→冻结 ≤5,000 ms、当期源开奖→任务结算完成 ≤1,000 ms，并输出每段 P95、最大值、样本数与全部越界期。后一个时延按原任务 `completed_at` 计算，与标签自身 `settled_at` 的合法性是两个都要通过的检查。原端业务时刻与持久提交完成时刻不同的实现，须另出 writer 成功提交后的读回及单调时钟原件；采集轮询间隔另交日志。

### 明确停用的分母与保留原件

**100 个启用机会全部闭环，加 20 个被日记证明全程明确停用的机会，可以得到 120 日历期窗口的 PASS。** 其中 `calendarPeriods=120`、`expectedEnabledDue=100`、`explicitlyDisabled=20`；20 个停用期只从应启用机会分母排除，不能算作训练、成功结算、命中或自然资格样本。

原 trace 在停用且未冻结的期上可以留下缺链。当前窗口保留 `originalTraceFailures` 全集，将这些已获明确停用证明的失败另列为 `explicitlyDisabledTraceFailures`；其余失败保留在 `enabledTraceFailures` 并阻断通过。不能用原 trace 的总 outcome 直接否决合法停用窗口，也不能删除原日历或原失败行来凑出通过。UNKNOWN、部分启用或机会开始后才停用的期不能当成全程明确停用；回归中下一机会开始后 1 秒才停用，会保留 101 个应启用机会，其中缺链仍输出 INCOMPLETE。所有已有的闭环标签，包括明确停用区间中留有原件的标签，仍须通过共同的严格时间检查。

工程机会覆盖和时延通过不计入自然胜率、REAL 解锁或资格证明；独立自然资格沿用 [研究验收说明](../research/README.md) 中 B06 的原协议与分母。

## 当前正反回归与历史记录

[SETTLEMENT_WINDOW_RESULT.json](SETTLEMENT_WINDOW_RESULT.json) 记录前次复核的共享时间检查与 window checker 的 SHA，以及 5 个新增用例、8 个研究用例和 3 个既有运行用例的实际退出码与输出。上方工具准备命令可复跑这些检查。

| 新增窗口用例 | 已验证结果 |
|---|---|
| 100 个完整启用闭环期 | PASS，100 条标签接受严格时序检查。 |
| 重算全部原件与 manifest 哈希；首次观测早于开奖、标签在开奖前、任务在开奖后完成 | 原 trace 可通过；当前窗口以 `LABEL_BEFORE_OPEN_OR_OBSERVATION` 拒绝。 |
| 冻结快照旁仍有 WAL sidecar | 拒绝把该输入作为完成冻结的验收快照。 |
| 120 日历期中 100 启用闭环＋20 明确停用缺链 | PASS；保留 20 条停用缺链和完整原失败集，启用失败为空。 |
| 下一机会开始后 1 秒才停用且没有冻结 | 仍计 101 个应启用机会，保留缺链并输出 INCOMPLETE。 |

[host-results.json](host-results.json) 留存先前七组工具修正测试、脚本身份与当时 B04 输入；当前生产 owner 的依据是上述 `research/b04/fixed-result.json`，本次窗口与短链回执见[当前验证目录](../../../verification/STARCORE_R33_5_20261120/second-review/README.md)。分别保留其输入与适用范围，不能以历史宿主回执替代当前生产后态或现场原件。本页记录的是宿主与离线复测，未产生实体机安装包、在线作业或自然样本。

## 原签设备和提交返回的本轮验收

用当前 `prepare_tools.py` 准备工具。调用其中 `B02/enabled_window_r335/collect_enabled_window.py` 的 `capture` 子命令，在原设备、原签 APK 上各取得一次 START 和 END。默认采集来源为 `APP_OWNED`，从应用已落盘的日记中读取三个字段。原采集命令的设备序号、APK SHA、部署区间与日历原件须齐全；双读失配、来源改变或缺件应重采原件。支持 run-as 的安装可明确选择 `--capture-source ADB_PRIVATE` 并在两端使用同一路径。

百期签收同时读取 `previousOpenToDurableFreezeObservation` 与 `targetOpenToDurableSettlementReturn`。逐期追到相同输入请求、canonical hash 和结算 hash，保留两个原事务内时延供排障。时限分别为 5,000ms 和 1,000ms，等于上限合格，任何应启用期缺链或超限须回传原因。B08 短链必须通过当前固定截止检查。取证源码、工单、测试及具体反例见[第二次复核](../REVIEW_SECOND_20261120.md)。
