# 持续生产与重启后的积压对账

第 3 版三代联调能检查单条公式的训练、赛前读取和两次出生。本条补测另一个故障：这条链曾经通了一次，调度器随后仍有心跳，却始终消费同一旧产物，或每轮只登记等待。

B12 使用既有 Android 隔离工程入口启动生产服务。开跑前将受控源流、权威期序、每一目标的相对开奖与截段时刻、工作的最晚时隙、调度周期边界和重启边界写入测试源码内版本化的 JSON 场景。整个窗口由同一设备、同一 boot 的外部单调时钟原点定标，包括进程退出的间隔，各期号与业务 ordinal 一一对应。每个完整周期预置可区分的公式、训练样本和研究刺激，选取能覆盖全部九条工作通道的周期。重启前和重启后各覆盖至少两个完整调度周期。周期按实际最慢的受测计划及其资源预算确定。这些频次和产物差异只用于持续供给输入的工程正例。

| 采集点 | 施工方导出与核对 |
|---|---|
| 服务、生命周期、开奖 | 从 AutoFetchService 生产入口、权威 draws、服务代际与外部进程/Activity 观察器取证。保留完整观察区间、旧进程死亡、新进程启动、PID 与进程出生时间，期间无 Activity。冻结、结算按相同角色与目标联结。 |
| 创造、训练、发布 | 对每个登记工作保存生产表及唯一业务键、成功提交时刻和实际业务内容。出生、变异、融合取规范化可执行树；训练和发布取数值权重；研究取被后续队列实际消费的动作或实验配置。剥离日志时间、随机 ID 和封装后另存业务字节。微批训练可从同一物理批读回其实际消费的样本工作键。 |
| 赛前冻结、结算、反馈 | 导出 R35 赛前写入成功回执。结算绑定同一期已提交的冻结及同期开奖排列，反馈绑定该结算原件。冻结内容、训练标签等其余业务校验沿用 B04/B07 及三代联调。 |
| 研究与下一轮 | 记录 research、mutation、fusion 的实际提交与业务原件，每个受控周期都应能从生产输出看到预置可区分输入引起的变化。无注册机会、只读旧对象、只写 WAIT 的环节分别报出首个欠账工作的期号、阶段和到期时隙。 |
| 待办与重试 | 每期提供开奖和冻结、结算、反馈、训练机会，另外按预登记日程安排其余工作。按键重算“上期未完 + 本期释放 − 本期唯一提交”，与实际待办快照一致。故意在一项工作到期前杀进程。恢复后保留原待办、完成一次；对已提交工作的重复投递返回同一原件。待办从实际任务队列及游标读取。 |

## B12 工程回执与复算

在 A-B12-03b 的执行原件加入 `continuity_index_artifact`。在固定 `source_after` 的工程源码中跟踪固定测试原件 `tools/r341/fixtures/continuity_b12.json`，将其路径填入 `scenario_source_path`。源码内的场景文件字节须与索引 `scenario` 的 SHA-256 一致。索引带 `scope=ENGINEERING_FIXTURE`、`source_commit`、`apk_sha256`、`run_id` 和 `time_scope`。与三代联调用同一施工 APK；若采用不同隔离运行，每个运行保留自己的 run_id 和时基。执行器保存 Android 采集器、校时锚点及其原件。工程通过的含义是给定受控流可以持续处理。实际优劣由 B09 独立前瞻实验判断。

`scenario`、`ledger`、`restart` 均为相对索引目录的 `{path, sha256}`。场景 `slots` 逐行登记 `issue, ordinal, draw_ms, close_ms, work_due, input_sha256, draw_numbers`，`work_due[stage]` 为该工作的最晚零基时隙。九个阶段为 create、train、publish、freeze、settle、feedback、research、mutation、fusion。预置受控输入单独保存 `input_original`，各工作绑定该期输入摘要。预期开奖号码只交给隔离测试驱动；开奖之前不得让引擎读取未来标签、预置答案或完整测试剧本。周期末时隙列于 `cycle_ends`，`restart_after` 指定一个存在未完成工作的周期结束点。ledger 保存完整生命周期观测及逐期 `draw_original, events, pending_after`。每个 COMMIT 引用正规化业务原件及 `effect` 字节，REPLAY 引用已完成的原件。restart 保留外部观察器记录的进程事件、重启前后的真实待办。待办键按“阶段、工作期号”排序。字段实例见 `tools/test_continuity4.py`。

```bash
python3 -B tools/continuity4.py --index "$continuity_index" --evidence-root "$continuity_directory"
python3 -B tools/test_continuity4.py
```

批回执在既有学习回执验证后调用 `check_continuity_receipt(run, receipt, originals, repo)`，由其向 Git 读取固定场景、核对同版 APK、重新执行 `verify(index, directory)` 并用 `referenced_files(index, directory)` 返回的“索引相对路径 → SHA-256”对照全部申报原件。工具拒绝截短日历、跨范围拼接、重启丢待办、重复提交、重启未经重放测试、整周期等待、循环复用旧内容、界面补救、错期结算和漏失开奖。设备 24/72h 仍按 B05 完整机会日历与无界面观察评定。B09 独立前瞻质量、唯一主链资格与结算须另行签收。
