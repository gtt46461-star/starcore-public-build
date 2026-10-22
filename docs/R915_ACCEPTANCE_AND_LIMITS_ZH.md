# R915 验收、风险边界与实机步骤

## 容器内发布门

- 524 个 Java 全量 Java 17 编译；
- 历史 R906–R914 合同回归；
- 110 个开奖任务全部终态、100/100 赛前冻结结算回归；
- NULL 租约崩溃恢复、任务 claim、事务回滚重放；
- OOS、真实 Trade、任务终态同事务；
- 一期最多一条未结 REAL；
- Research 无直接 REAL 权限；
- 15 器官 AssetGenome 与 16 器官 ResearchGenome；
- 中文机制命名、赛前只读、结算后状态更新、重复结算拒绝；
- N240/paired120/FDR/校准/稳定性证书门；
- 133 锁定 Owner 当前交付哈希一致，未声明 Legacy 改动为 0；
- 524 文件无空文件、无 TODO/FIXME、无无法解释的短具体 Owner；
- APK applicationId、version、原证书连续性、zipalign、Dex 核心类、覆盖安装 provider key。

## 实体 Android 必须补做

1. 在有真实网络和系统省电策略的目标手机覆盖安装，保留原数据库。
2. 打开一次应用后返回桌面，连续 200 个自然未来期；不要使用设置页“强行停止”。
3. 每 20 期复制一次完整真值报告，最终核对：
   - 开奖任务终态/全部开奖任务 ≥98%；
   - 有赛前冻结且开奖结果存在的记录结算覆盖 ≥98%；
   - duplicate update=0；
   - 当前期游标、最新预测、最新结算连续推进；
   - 研究 P95 不阻塞 P0，OOM=0；
   - 每个 active 资产/算法 N 持续增长，不再长期停在 1；
   - REAL 只在 Gate 全通过时出现，一期最多一个未结仓。
4. 测试划走任务、系统普通回收和重启；分别确认 Alarm/Job/Boot 恢复源。
5. 厂商若启用极端省电/自启动限制，需要在系统设置允许后台、自启动和通知；APK 无法合法绕过厂商用户策略。

## 失败判定

- 服务显示存活但业务游标 3 期以上不前进；
- 有赛前冻结和权威开奖却未结算；
- 同一 asset/algorithm/target 重复更新；
- Research 异常导致生产结算回滚；
- `LegacyIntegrity=FAIL` 或 Legacy mutation/write/evolution 非 0；
- 没有证书却出现新研究贡献；
- `MODEL_PROVEN=false` 时界面声称模型已证明；
- 训练/回放样本被计入 Future OOS；
- 不满足 Gate 却写 REAL，或同一期出现多个未结 REAL。

## 安全回滚

- 新资产或算法撤证后下一期贡献为 0；
- Genesis 总开关关闭时保留完整 Legacy baseline；
- 不删除历史 EvidenceLedger，不后验改赛前冻结；
- 覆盖安装前保留原签名资料；若必须回退 APK，数据库只允许兼容版本读取，禁止用卸载作为“修复”。

## 胜率解释

命中率不是单一验收目标。高 HR 若 N 小、Wilson 低、Coverage 差、Brier/ECE 差、块/域不稳定或经过大量候选筛选仍无 FDR，都不允许晋级。R915 提供“创造—验证—淘汰—限幅贡献”的能力，不提供脱离未来证据的收益保证。

