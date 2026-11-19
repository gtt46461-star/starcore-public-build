# B01–B04 施工工具校正与本批签收

前提是两分卷与原方案逐文件 seal 已通过复核。用校正目录准备一份独立的验收工具副本。原方案路径的 P00→B08 安装器继续读取其封存的 after_full，无须重新生成 Android 数值身份。三个验收脚本的前、后 SHA-256 在 prepare_tools.py 和 host-results.json 中逐一列出。

```bash
# PLAN 为已核验的 STARCORE_R33_5_20261118 目录，REVIEW 为本说明所在目录，
# TOOLS 为方案外的新目录，RAW 为本次候选 APK 的取证目录。
python3 -B "$REVIEW/prepare_tools.py" "$PLAN" "$TOOLS"
python3 -B "$REVIEW/test_review.py" "$TOOLS"
```

| 工单 | 问题及修改 | 本阶段签收 |
|---|---|---|
| B01 / RT04 | prepare_exact_intake.Parts.read 原先使用第二次 stat 大小计算读取步长。分卷在清单校验后被截到当前偏移，单次读取会取得零字节，循环无法前进。改按已核分卷边界确定步长，遇短读立即退出并记录错误。 | 用 TOOLS/B01/recovery/prepare_exact_intake.py 替换原命令中的脚本路径。全部原 SHA、重复分卷、缺失子件和部分恢复规则继续执行。恢复成功另取 Android 按原作业和 checkpoint 哈希读回的收据。 |
| B02 / RE21、RT05–RT07 | verification/mainline/check_trace.py 的 REAL 概率与交易收据仅比较实际开奖和首次发现时刻。推迟开奖时，两个提交时刻都可能晚于固定截止。两处均增加严格“小于原冻结 deadline_at”。另用 check_window.py 逐期合并原独立启用日记、日历、SQLite 全链和时延。 | 用 TOOLS 中的 collect_enabled_window.py、check_trace.py 替换原相应调用。对 B02 及 B08 整体运行验收执行下面的合并命令。 |
| B03 / C02 | 核对本批 ReplayDatasetR27 前态、后态及回放测试。5000 条有效行、两处断档、重复/非法行、原件往返、取消与严格可见时点在本次运行验证。单独运行出生事务夹具，保留其宿主范围。 | 运行 TOOLS/B03/tests/run_replay_parity.py 和 test_birth_contract.py。真机仍须沿 FoundryCampaignR26、StoreR27 和 RuntimeR27 取得同一出生事务、父子血缘、精确样本与实际 checkpoint 的收据，以及 writer 拒绝/恢复时重复请求的独立读回。 |
| B04 / C03、RE16 | run_session_numerics.py 之前对十二个类优先选择本批 payload，再回退 033 baseline，因而组合中 ReplayDatasetR27 用了旧版本。改为本批 after_full、上一批 B03 after_full、已验证的 baseline 顺序，给每个实际编入的源文件记录路径与 SHA。 | 运行 TOOLS/B04/tests/run_session_numerics.py。在相同测试输入下验证 17,763 次断言、166 片、60 梯度片、102 评估片、20 个 epoch 和 10 个在线位置更新。保留目标机同批 dataset→checkpoint→十位置发布→consumer 的单独验收。 |

```bash
python3 -B "$REVIEW/check_window.py" --tools "$TOOLS" \
  --bundle "$RAW/trace.json" --evidence "$RAW" --start "$START" --end "$END" \
  --commit "$COMMIT" --apk "$APK_SHA" > "$RAW/B02-window-review.json"
```

START、END 和 trace 使用原 B02/enabled_window_r335/README.md 指定的真实只读原件。预注册窗口需含至少 100 个启用机会才能签本项；明确停用、漏冻、缺结算、迟到与未知期各保留计数。跨越明确停用导致数量不足时，保留原窗口并另行预登记观察。每个适用期核对上一期源开奖→冻结 ≤5000ms、当期源开奖→结算成功 ≤1000ms，逐段输出 P95、最大值、样本数和所有越界期。原端业务时刻与持久提交完成时刻不同的实现，须另出 writer 成功提交后的读回/单调时钟原件，采集轮询间隔也须独立交日志。工程机会覆盖与时延证据不计入自然胜率、REAL 解锁或资格证明。

host-results.json 记录七组本轮测试的退出码、输出、脚本变更身份及本次 B04 数值输入。未在本轮产生实体机安装包、在线作业或自然样本。
