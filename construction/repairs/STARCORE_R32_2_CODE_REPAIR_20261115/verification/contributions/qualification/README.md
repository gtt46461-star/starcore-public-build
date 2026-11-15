# P3 主链真实入口的工程回归

本次没有另造证书规则或降低门槛。对应用 R32.1 的 P1–P5 后态执行了现有生产入口回归，运行 SQLite JDBC 的真实 SQL，替换的是 Android 平台边界。测试数据库使用明确标注的合成工程输入，不能当成自然未来成绩、Android 实测或外部交易。

`actual-path-results-v2/RECEIPT.json` 的全部 11 个阶段均以 exitCode 0 完成，状态为 `PASS_HOST_ACTUAL_PATH_NOT_ANDROID_OR_NATURAL`。其中：

|实际调用或验证|结果|
|---|---|
|原迁移 owner 安装与恢复|156 个实际迁移单元；未声明的索引被拒绝，原就绪域保全|
|热点授权读取|5 个真实索引查询计划|
|LiveTask 两次执行|证书时间与冻结原件不变|
|LiveTask → MainlineFreezeProof → UnifiedResearchChoice → CertifiedPrediction → DecisionGate → DbStore|116 条断言、15 个故障反例，通过|
|同一终态的三表产品|每表一行；关闭并从新 SQLite 连接读回，摘要一致|
|实际语义重生与未知身份|10 条断言，通过；原资格暴露保全|
|新未来分段的冻结、注册与身份拒绝|32 条断言，通过；新段有独立索引和零初始暴露|
|原 typed 主链选择权迁移|25 条断言，通过；旧新投影不一致、旧 fence、过期和撤销均拒绝|
|原 SU2 授权和证书桥|完整冻结、原始协议/证书、默认关闭、重复真实终态、过期/源版本等用例通过|
|四类源修订撤销|号码、hash、dirty、canonical：同事务撤销与回滚；后续读取拒绝；保留原证据和旧模型|

15 个主路径故障反例包括：用户开关关闭，证书丢失/过期/撤销，模型状态改变，校准改变，源代数改变，资格原件出现孤立追加，资格保护触发器删除重建，typed choice 撤销，proof 表丢失，授权域未就绪，资格协议戳陈旧，唯一选择丢失，跨通道证书冒充。还检查原冻结号码与调用者号码不一致时的拒绝及最终写入前撤销。

## 本次实际修改

`full_target/tools/r31/run_typed_mainline_host.py` 补全测试平台的 `SQLiteDatabase.delete` 适配，使用绑定参数执行真实 JDBC `DELETE`。P1 新增的 `HypothesisStoreR31.recoverQuotaSliceR32` 使用该 Android API，旧 HOST 适配遗漏它，首次回归因此 `NoSuchMethodError`；首次失败保留在 `actual-path-results/`。补适配后运行通过。这里没有更改生产删除逻辑。

`full_target/tools/r32/run_mainline_host.py` 为可重放入口，参数显式指定编译 classes、SDK、sqlite-jdbc、slf4j。没有系统 javac 可执行文件时用同一 JDK 的编译模块；不安装依赖，不改变系统环境。补丁与前后 SHA-256 见 `qualification.patch`、`MANIFEST.json`。

## 重放命令

在已经应用候选源码并完成全量编译后运行，所有 PATH 值换成实际绝对路径：

```bash
python3 tools/r32/run_mainline_host.py \
  --repo PATH_TO_CANDIDATE \
  --classes PATH_TO_CURRENT_COMPILED_CLASSES \
  --android-jar PATH_TO_SDK36_ANDROID_JAR \
  --sqlite-jdbc PATH_TO_SQLITE_JDBC_JAR \
  --slf4j PATH_TO_SLF4J_API_JAR \
  --out PATH_TO_NEW_EMPTY_RESULTS_DIRECTORY
```

测试支持文件包括仓库的 `verification/r932/android-json`、`tools/r29/qual_host/shim`、`tools/r30/qual_host`、`tools/r30-repair/qual_host`、`tools/r31/unified_host`。它们属于工程测试，不打进 APK。

本次冻结源基于父任务的 P1–P5 候选后态。其他新增生产修复合并后须重新编译并重放受影响入口；本回归不能替代结算事件消费、完整 Android 生命周期、真实后台时序和自然未来质量验收。
