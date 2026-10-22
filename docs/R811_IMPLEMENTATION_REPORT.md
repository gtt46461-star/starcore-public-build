# R811 FINAL LOCKED 实施与复核报告（Engineering Candidate）

## 结论

R811 方案中的 P0 运行闭环与 P1 Research 基础设施已落到完整源码，并通过全量编译、旧版回归、
R811 主机契约、100 组策略零漂移、文件处置、APK 构建和签名连续性检查。P2 的 action archive/
删除、targetSdk 大迁移和任何 Production Gate/Rank/Calibration 替换按方案保持
`POST_CLOSURE/NOT_APPLICABLE`。

本报告只声明可由现有证据证明的工程事实。没有真机或真实业务数据的测试不会被写成 PASS；
`MODEL_PROVEN=false`、`SHADOW_ONLY=true`、REAL hard lock 均未解除。

## 关键施工结果

| 领域 | 实施结果 |
|---|---|
| 身份 | `com.ethk3.novaonepro` / `577190821` / `V57.15.3-R3.6.9-812` |
| 数据库 | 保持 DB36；增加 R811 revision 1 critical schema；Research schema quiet/lazy |
| Settlement | `LIVE_CURRENT(100) > LIVE_RETRY(50) > BACKLOG(0)`；current promotion 与 history enqueue 分离 |
| OOS | 结算仍由 `SettlementEngine` + `OosAfterDrawWriterR36` 单一 owner；严格 PIT/exactly-once 合同保留；冲突开奖号持久封锁 OOS |
| Deferred | 持久 cursor，单次 120 action；exact 100/1200；`MODEL_INVOCATIONS=0`；实时压力立即 yield |
| 实时隔离 | `RealtimePressureGateR811` 接入 analysis/settlement/live pulse、DB/Deferred/Research |
| 诊断 Truth | ServiceAlive 与 BusinessHealthy 分离；期号 lag、terminal code、SHADOW 统计、N/A 概率、revision fence |
| API | 最新接口仍权威；仅真实落后至少 2 期才触发一次 bounded history-20；跨日真实期距回归已补 |
| 策略冻结 | Production cohort 冻结；100×11 黄金字段 byte exact；诊断 timing 不进入策略 fingerprint |
| Research | 34 primitive DSL、Formula、FTRL、TinyMLP/attention、按期 grouped walk-forward、BH-FDR、Pareto、holdout、1200 grouped label、expert sidecar、cross-position assignment |
| 安全边界 | canonical codec、防篡改；危险 token/import 拒绝；无动态加载；结构 Java 仅 BUILD_REQUIRED |
| Promotion | 只允许带 parity+PIT 证书的 Research→SHADOW；Production hot replacement 永久拒绝 |
| Android | compileSdk 36 / targetSdk 28 / minSdk 21；dataSync；未声明 specialUse；运行时能力真实观测 |

## 复核数据

| 检查 | 结果 |
|---|---|
| 官方 API-36 全量 Java 编译 | PASS；420 source / 1,815 class / Java 17；374,904 行 |
| R36 回归 | PASS |
| R811 主机契约 | PASS；170 assertions |
| 策略黄金门禁 | PASS；100 cases × 11 fields；exact byte compare |
| 黄金文件 SHA-256 | `fd2660829c3d05df7d655e2996afb597145b16ad3b9775e7774c41bf7362fd15` |
| 文件处置审计 | PASS；165 rows；53 MODIFY / 29 ADD / 60 FREEZE / 2 OPTIONAL |
| 文件处置审计 SHA-256 | `178dc6eecc29141b98ae6a5386c21bdab13315a1f50f2ca924f63feb0aa16368` |
| T01–T210 验收矩阵 | 210/210 已建档；166 工程 PASS；7 P2 N/A；37 外部证据待补 |
| 方案矛盾扫描 | PASS；DB36/rev1、R366 cohort 零漂移、无动态 Java、P2 archive 禁用 |
| 源码敏感信息门禁 | PASS；明文 API key 0、签名口令 0、私钥文件 0 |
| APK identity | PASS；package/version/min/target 均匹配 |
| APK ABI | PASS；239 Android method descriptors；禁止描述符 0 |
| APK 签名 | PASS；v1/v2/v3；与 R3.6.7 证书连续 |
| APK SHA-256 | `1312d9f1c28848acfa45c546afd36b27d2228934ad8594c94eec03166616a253` |
| APK 大小 | 3,154,798 bytes；单 dex；未压缩 dex 5,834,128 bytes |

## 施工中发现并闭合的问题

1. API 落后判断若使用纯数字相减，会把 `202608101440 → 202608110001` 错算成巨大期距。
   已改为 `TargetIssueResolver.distance`，并增加跨日 1 期不回补、跨日 2 期才回补断言。
2. 初次黄金比较显示只有 `featureContractHash` 随新 cohort 身份变化，其他 10 字段不变。
   R811 是运行返修而不是模型升级，因此 Production strategy cohort 恢复为 R3.6.7 冻结值；
   再跑 100 组后 11/11 字段全部 byte exact。
3. 初次 APK ABI 审计识别出 R811 能力观测新增的 6 个 Android API 描述符。逐项确认 API level/
   minSdk/runtime guard 后纳入 R811 明确白名单，未放宽通用 ABI 门禁。
4. 锁定方案的统计验收要求按“期”隔离，原有实验骨架不足以证明同一期所有位置不跨 split/fold。
   已改为 immutable frozen feature、issue-grouped walk-forward，并在进入 Pareto 前强制有限值、BH-FDR、
   coverage CV 与 block minimum 门禁。
5. 原内存级开奖号冲突判断不足以跨进程保持硬封锁。已新增 DB36 revision 1 的 raw provenance 与
   canonical 表；相同期号冲突会持久化标记、终止 settlement、撤销 strict OOS，且只有 confirmed
   canonical draw 才能进入结算队列。
6. 旧证书逻辑可能把重复位置样本当作 proof 分母。现以 distinct issue 为 proof unit，并增加 Beta、
   champion Brier、coverage CV、block minimum、regime、lose-streak、resource、frozen、FDR 与 fresh
   holdout 的 all-and guards；没有任何无门禁 `PROVEN_45` 路径。

## 未伪造的待真机项

- T29–T34：screen-on/off、task removed、process kill、主备网络、24h soak。
- T46：真实 R3.6.7 DB36 覆盖安装与 revision 1 数据保留验证。
- T48/T52/T56/T60/T62：连续业务流下的 starvation/exactly-once/crash-resume/P95/coverage。
- T117/T142–T146/T187：真实 SQLite 迁移、故障注入、冲突重启与 rollback pointer 恢复。
- T113/T119/T152/T153/T182：需要未来 strict OOS/live 数据的统计有效性证据。
- T120/T160：24 小时 Research 与 Production 共存 soak。
- Analysis P95 ≤ 5s、DB live lock P95 < 100ms、settlement lag ≤ 1、Coverage 30/30 和 120/120。

这些项目需要真机和时间序列业务数据。发布前应按 `R811_ACCEPTANCE_MATRIX.csv` 的未执行项补证；
在此之前继续 SHADOW 是正确且强制的安全状态。
