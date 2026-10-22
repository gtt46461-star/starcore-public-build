# 星核天枢 V57.15.3-R3.6.5 先读说明

## 交付身份

- 包名：`com.ethk3.novaonepro`
- 修订：`V57.15.3-R3.6.5`
- versionCode：`577180817`
- versionName：`V57.15.3-tianshu-b01-b15-runtime-closure-hf5-20260811`
- 数据库：`binance_k3_nova_one_pro_v57.db`，`DB_VERSION=36`（仅加列，不删表、不清库）
- Java：源码/目标 17，classfile major 61
- DEX：D8 9.2.23，minSdk 21，legacy dx=0
- 姿态：`SHADOW_ONLY=true`、`MODEL_PROVEN=false`、REAL 硬锁

## R3.6.4 为什么始终不出号

设备报告已经给出确定证据：抓取和结算在前进，但每个目标期在正式计算前以
`R36_DURABLE_CLAIM_FAILED_OR_EXPIRED` 失败，运行账本始终 `NEVER_COMMITTED`，所以 canonical、
100 号码与 1200 动作必然都是 0。这不是 240 期门槛：最低可运行历史仍是 12，100 是新安装优先补齐量，
240 只是冻结输入上限。

旧实现同时存在四个租约错误：同一请求的多次执行复用同一个 Owner；查询会把仍由其他执行持有的
RUNNING 行再次投递；claim 失败后的异常路径会无 Owner 围栏地清掉别人的租约；55 秒 deadline 被当成
PIT 请求的业务有效期。任一错误都能制造报告里的失败，组合后会形成持续抢占和紧密重试。

## R3.6.5 已关闭的路径

1. 每次 `AnalysisSingleFlightR36.Lease` 生成唯一执行 Owner，重试不再复用 attempt ID。
2. claim 在事务内核对 target、generation、requestHash、状态、Owner 与 lease；活跃外部 Owner 只返回
   `DEFER_ACTIVE_OWNER`，不会写失败账本，也不会清租约。
3. 只有过期租约允许 `CLAIM_STALE_TAKEOVER`；同一 Owner 可 `RESUME_SAME_OWNER`。
4. 心跳、历史哈希绑定、重试、失败、过期和 canonical 提交全部要求 Owner 围栏。
5. 重试写入 `next_attempt_at`，采用 1–30 秒有界退避，禁止零等待自旋。
6. 目标已开奖时禁止补算；目标未开奖时不再因为本地 55 秒壁钟而丢弃合法请求。
7. 覆盖安装只对 R3.6.4 租约竞态留下的终态行执行一次 R3.6.5 rearm；R3.6.5 自身终态不会循环重置。
8. B12 与统计诊断只读；REQUESTED/RUNNING/RETRY 显示为进行中，实际失败不能被心跳显示成 HEALTHY。

## 安装

保留现有数据库时直接覆盖安装，不要先卸载：

```bash
adb install -r Starcore-Tianshu-V57.15.3-R3.6.5-release.apk
```

手机文件管理器也可直接点 APK 并选择更新。总包解压后，只需安装
`APK/Starcore-Tianshu-V57.15.3-R3.6.5-release.apk`；其余目录是完整源码、Git 历史、方案、工具链、
签名连续性材料和可复建证据，不需要复制到手机。

## 安装后的明确验收点

覆盖安装后的第一个仍未开奖目标期应出现：

1. `analysisStage=ANALYSIS_RUNNING`，随后 `ANALYSIS_CANONICAL_COMMITTED`；
2. 运行账本 `events>=1`、`last=ANALYSIS_CANONICAL_COMMITTED`，不再出现
   `R36_DURABLE_CLAIM_FAILED_OR_EXPIRED`；
3. 当前目标期同步核心存在 issue=1、champion=1，随后延后证据达到 number=100、full=1200；
4. 下一期开奖后由唯一 Settlement Owner 写严格 OOS 标签，`modelCalls=0`；
5. 如果另一个执行仍持有有效租约，状态应为等待/DEFER，而不是失败或清租约。

已有 30 个缺失目标是旧版本从未生成 pre-draw canonical 的历史缺口。不能在开奖后伪造回补；R3.6.5
只从安装后的合法未开奖目标开始积累，因此旧 missing 列表可能保留，新增目标的覆盖率应开始增长。

## 真实性边界

Java 17 全量编译、主机合同、APK ABI、签名与离线复建可以证明工程产物一致，不能替代手机上的
T29–T34（亮屏、息屏、划掉任务、杀进程、网络切换、24 小时）验证。模型 Artifact 仍未训练/未证明，
严格 OOS 样本不足 120，所以 REAL 必须继续锁定；禁止追期、马丁、倍投、频率债务、动态 K 和多位置 REAL。

APK 是压缩后的 DEX 与资源，大小不等于源码数量。完整性由源码 ZIP、Git bundle、逐文件 SHA-256、
114/114 合同审计、DEX 类/方法门与离线重建共同证明，不使用空壳、占位代码或无效填充来制造体积。
