# R34 运行修复候选代码载荷

基线：`273641e838e0b8dd842b42a67a674e184942a30a`，分支 `construction/r335-field-095`。这是施工方案中的候选后态，不是已修改、已发布或已真机验收的应用。

## 已包含的实际接线

|批次|文件|接入位置|修复边界|
|---|---|---|---|
|B01|`DeferredWaitR34.java`|由 `DeferredEvidenceWriterR36.transientResourceFailure` 调用|只识别明确 DEPENDENCY / ADMISSION / ROLLED_BACK 的 writer backlog；不吞未知提交、不放过完整性错误|
|B01|`DeferredEvidenceWriterR36.java`|正常分类、旧错误恢复查询、retry bookkeeping|背压不再被当成普通故障耗尽尝试/计入 caught crash；原快照、游标、lease CAS保留|
|B01|`DatabaseCoordinator.java`|原操作注册表|失败状态回写和既有故障重开改为 RESEARCH_CONTROL，继续经过同一把writer锁；修正动态creation操作族注册|
|B02|`RawHttpIdentityR34.java`|由 FetchLedger transport、raw binding、receipt更新调用|原body字节UTF-8 hash，不trim、不重写非空冲突hash|
|B02|`FetchLedgerR925.java`|`complete`、`bindRawResultR82`、`bindParsedResultR82`、`ensureReceiptWithinR31`、`checkedBindingR82`|忙写导致transport receipt尚未入库时，可在原draw事务中补同一request的exact pending receipt；缺原件继续DEPENDENCY|
|B06|`DirectedResearchCreationR932.java`|`maintain`两处原事务入口|用原 F01 typed API保留commit outcome，消除WAIT fallback被重包为无cause的BUG；数值函数/种群规则不改|

## 文件与执行

- `after_full/`：6份完整后态文件，4修改、2新增。
- `patches/B01_runtime.patch`、`B02_runtime.patch`、`B06_runtime.patch`：按所属批次应用，前态hash见manifest。
- `patches/ALL_RUNTIME.patch`：同一组合的完整差异；不能同时应用分批patch和ALL。
- `tests/verify_runtime_payload.py --baseline <095干净检出>`：校验6份hash、`git apply --check`、编译并运行2个接线helper/原始checksum/提取出的实际生产分类函数、SQLite事务与过期owner合同。
- `VERIFICATION.json`：本次运行结果。

## 已执行与未执行

已执行：6份前后态hash、补丁干运行、15项Java断言、3项SQLite合同场景。编译使用宿主Java21编译模块，source/target17；测试覆盖字节hash和背压分类。无 Android SDK，四份完整生产类尚未进行全Android编译，也没有构建APK或执行真机测试。SQLite fixture验证事务合同，不替代生产SQLite适配器集成测试。

**施工必须补足**：095完整工程上所有受影响调用者编译；注入complete和payload均BUSY、随后原draw成功提交；真正冲突的receipt拒绝且原hash不变；进程重启后无pending原件不得补造；背压故障退出后游标继续且只提交一次；B01持续故障drain和schema切片必须连同此局部修复施工，局部patch不等同整条主链恢复。

## 安装后态的身份规则

施工方将修改纳入新版本后，使用工程原有生成器更新受影响source/dependency身份；不得改写旧report的 sourceCommit、旧检查点ABI或旧证书身份。R34方案应持有本代码hash和095基线。生产前态如果已不同，先对具体差异重审，禁止force覆盖。
