# STARCORE TIANSHU R815 完工交付说明

## 1. 发布身份

| 项目 | 最终值 |
| --- | --- |
| applicationId | `com.ethk3.novaonepro` |
| versionName | `V57.15.3-R3.6.12-815` |
| versionCode | `577190824` |
| compile / min / target SDK | `36 / 21 / 28` |
| 数据库 | `DB 36`，R815 schema revision `3`，保留 R811 revision `1` |
| 游戏与正式动作约束 | `Binance1-PK10`、单位置、固定 `K=3`、100 个号码概率、1200 个唯一动作 |
| 运行边界 | `SHADOW_ONLY=true`、`MODEL_PROVEN=false`、`REAL_BET_HARD_DISABLED=true` |

## 2. 已落实的核心施工

### 2.1 身份、构建与发布溯源

- `release/release_identity.properties`、`app/build.gradle`、`ReleaseIdentityR815.java`、`V51BuildManifest.java`、`AppConfig.java` 已统一为 R815 身份。
- 正式 Gradle release 任务必须获得格式正确的 `DUOQU_API_KEY`；空值、非字母数字、长度不在 8–128 范围内会停止构建。
- `SourceManifestR815.java` 由 `scripts/r815_generate_provenance.sh` 生成源码、资源和工具链哈希。
- `BuildProvenanceR815.java` 在安装后计算 APK、所有 `classes*.dex` 和签名证书的实际指纹。最终 APK 自身哈希写入交付侧 `RELEASE_MANIFEST.json`，而不是塞入 APK 本体，避免不可能成立的自哈希循环。

### 2.2 数据完整性与数据库迁移

- `DbHelper.java`、`SchemaUpgradeCoordinatorR815.java`、`DatabaseCoordinator.java` 实现备份、`wal_checkpoint`、`quick_check`、事务迁移、校验失败回退。
- Draws 表新增不可变首次见到时间、V2 业务哈希版本和 V2 哈希；R815 表包括关键错误账本、分块长报告、恢复世代、SHADOW sidecar、研究候选/评估/工件/队列。
- `DrawHashR36.java` 的 V2 只以“期号 + 有序十位排列”定义业务身份；V1 仅用于历史已存记录的兼容读取。
- `DbStore.java` 保证首次见到时间不被随后请求覆写；保留窗口至少 1440 期。
- `IssueFreshnessResolverR815.java` 仅按合法 12 位业务期号选取新数据，不把 HTTP 成功、服务端 open time 或同一期重复响应当成新开奖。
- `HistoryIntegrityVerifierR815.java` 区分精确重复和同一期冲突；不足连续 12 期、有缺口或冲突时不会发出正式号码输出。

### 2.3 唯一运行入口与结算闭环

- `ProductionPredictionDispatcherR815.java` 是唯一直接调用 `V51RuntimeOrchestrator#runR36` 的生产入口。
- `PredictionOutputContractR815.java` 在提交前验证 10×10 概率矩阵、1200 个唯一动作、唯一 rank=1、无 `BET` 授权和完整历史边界。
- `SettlementCoordinatorR815.java` 是唯一能激活 `SettlementEngine#executeFromCoordinatorR815` 的结算协调器；遗留 V51 报告接口也只经该协调器兼容转换。
- `ShadowObservationSidecarR815.java` 只在生产结算终态以后写入幂等 SHADOW 观察，并异步排入研究队列。

### 2.4 后台恢复、诊断与研究边界

- `RecoveryCoordinatorR815.java` 对恢复请求按世代去重，短窗口连续失败会降级为 `FETCH_SETTLEMENT_ONLY`。
- `InternalWatchdogReceiverR815.java` 为显式、非导出的内部接收器；`BootReceiver.java` 只调度它，不把看门狗暴露为外部广播入口。
- `CriticalPathErrorLedgerR815.java` 提供持久错误账本和进程内有界回退环。
- `LongTextStoreR815.java` 与 `DiagnosticSnapshotR815.java` 提供分块长报告与可校验诊断快照。
- `ResearchFeatureSchemaR815.java` 现为真实完整的 96 项不可变 schema（修复了原先“声明 96、实际仅 90”的索引漂移缺陷）；`ResearchFeatureMaterializerR815.java` 对每项写入实际数据，并经 `ResearchFeatureVectorR815` 编码/解码校验。
- `DatasetIdentityGateR815.java`、`ResearchRepositoryR815.java`、`ResearchSchedulerR815.java`、`ResearchPromotionVerifierR815.java`、`ResearchAblationR815.java` 保证数据集身份、期级评估和研究候选均不能改变 REAL 锁。

## 3. 自动验证结果

完整原始日志在 `build/r815-release/evidence/`（交付总包另含同一份 `BUILD_EVIDENCE/`）。已执行并通过：

1. R815 静态红线、非导出组件、无签名材料/嵌入 API key、唯一预测/结算调用链门禁。
2. 官方 Android API 36 全量 Java 17 编译。
3. R36 回归主机契约、R811 回归主机契约、R815 主机契约；R815 为 131 条断言。
4. D8 release Dex 生成。
5. APK 资源与 Manifest 重编、zipalign、v1/v2/v3 签名校验、压缩包完整性校验、版本 badging 校验。
6. 签名证书连续性校验。

最终 APK 的 SHA-256、DEX 集哈希、源码/资源/工具链哈希和证书哈希均写入 `RELEASE_MANIFEST.json` 与 `R815_RELEASE_BUILD_SUMMARY.txt`。

## 4. 真实边界（不得改写为“已证明”）

- 本交付通过主机编译、契约、签名和 APK 结构验证，但**没有声称已完成物理设备 24 小时运行、断网恢复或未来 live OOS 验收**。
- 因此 `MODEL_PROVEN` 仍为 `false`；号码输出是 SHADOW 研究输出，不能生成 REAL BET。
- 设备验收应使用本交付的 APK，在目标设备上执行安装、首次启动、前后台切换、重启/锁屏/网络切换、历史迁移和至少 24 小时运行观察，并把结果作为新的独立证据保存。

## 5. 可复现构建

```bash
cd <本源码根目录>
export DUOQU_API_KEY='<已配置的真实密钥>'
./scripts/r815_behavior_gate.sh
./scripts/r815_build_release.sh
```

构建脚本不会把 API key、PKCS#12、密码或签名凭据写入源码树、日志交付包或 APK 交付说明。对于同签名升级构建，脚本可在临时目录内使用已受信任基线 APK 的 split key；该值同样不会被输出。
