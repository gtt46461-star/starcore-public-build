# R34.3-173 运行恢复精确补丁

基线：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`。这是供施工审核的完整源码替换包与统一diff；未修改共享安装基线、未安装APK、未在用户设备运行。APK发布继续原R34.2-B01~B12命名/签名身份链，计划标识R34.3-173不改应用安装身份。

## 应用

在该基线的独立施工分支执行：

```sh
git apply --check /absolute/path/runtime.patch
git apply /absolute/path/runtime.patch
```

`patch_manifest.json`含每个修改前/后SHA-256；任何before不同先做语义合并，不直接覆盖。`after/`是每个涉及文件完整最终内容；不需要猜测省略部分。根方案组合patch后要再次检查相邻改动，特别是DatabaseCoordinator研究操作登记。

## 具体内容

1. `StartupCheckpointIdentityR31` + `StartupTempSchemaR343`：精确允许4个自有TEMP scratch定义，所有主数据查询main限定，保留完整CAS/source/schema/column保护。修复应用自己创建r31q表后启动必拒绝的问题。
2. `StartupStageJournalR341` + `StartupProbePolicyR343` + `AutoFetchService`：同锁内持久预约半开，5min冷却、稳定source/dependency最多3次，预算用尽需依赖变化；同BUG不变READY。日志轮转保留最近3个owner结果及最后probe计数。
3. `WriteBudgetR341`：未注册的RESOURCE_WAIT改为实际支持的RESOURCE_QUEUE；不增加写预算。
4. `DatabaseCoordinator`：明确fault背压归RESOURCE_QUEUE；登记`R343_TRAIN_INFRA_RECOVERY`为RESEARCH_DATA，并累计登记rolling learner/publication全部明确writer操作（claim/wait控制类，实际数据/回执变更研究数据类）。B02可先登记未启用的B06操作。原唯一writer、live优先、fault队列限制保留。
5. `WriterHoldFaultsR31`：保留4行上限与旧durable envelope/receipt幂等；新增阶段/耗时/错误类别/行数；区分控制tick与真正drain尝试，冷却早退不刷新lastDrainWall。该观测补丁不声称已经证明设备排空卡点或收敛。
6. `MainWriterFaultProjectionR342` + `WriterFaultSnapshotViewR343` + `ReportAttachmentClosureCP12`：冻结库里的样本没有可信冻结mono锚点就保持UNKNOWN、pending=null，但保留精确hash验证过的历史原文及snapshot绑定。不能以现在的elapsed过期判断掩盖可分析的原数据，不能拿样本自身时间证明fresh。纯helper可校验独立冻结区间；当前生产调用明确没有锚点，as-of producer增强另列可选批次，未声称完成。
7. `R343RuntimeRepairInstrumentedTest`：可供Android隔离runner执行的真实SQLite WAL/TEMP、journal半开/轮转/上限回归。

## 已执行验证

```sh
python tests/run_runtime_policy_checks.py --baseline /absolute/path/installed-source
```

实际输出见`verification.json`：

- 原TEMP目录拒绝谓词在真实宿主SQLite中重现；新Java精确合约接受4表，拒绝意外shadow。
- 13项TEMP/单调时钟/有限probe预算检查通过。
- 实际基线FailureDisposition分类器验证资源类别、UNKNOWN回执恢复、COMMITTED不重放通过。
- 9项冻结样本时钟/身份/原文保留检查通过；明确生产无anchor时保持UNKNOWN。
- 所有after Java文件完成语法解析。**语法解析不是Android符号/类型编译**。
- `git apply --check`对安装基线通过。

未执行：完整Gradle/APK编译、设备instrumentation、用户规模4.3GB数据库、屏幕/网络/Doze矩阵、24/72h浸泡。不得把这些写为PASS。

## Android施工验收命令与隔离要求

仅在可丢弃的测试设备/模拟器与原库的授权副本，使用现有`R30FaultAndroidRunner`隔离上下文；不在用户生产安装上直接跑Instrumentation。完成该仓库规定的身份/生成源码/依赖准备后构建：

```sh
./gradlew -Pr30Instrumentation=true :app:assembleDebug :app:assembleDebugAndroidTest
adb shell am instrument -w -e r30Sandbox r343_runtime -e class com.ethk3.novaonepro.R343RuntimeRepairInstrumentedTest com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner
```

基线ReleaseManifestR926.APPLICATION_ID为com.starcore.research.r13，Gradle未设testApplicationId或applicationIdSuffix，故上述默认测试包为com.starcore.research.r13.test；构建后核对merged test manifest与该身份一致。不得猜测用户设备选择；施工设备序列号必须明确。

补充设备用例：非空checkpoint CAS、冷原件缺失仍保留热行、未知TEMP拒绝、prepare后source generation变化拒绝；pending>=24时fault drain仍独立获得控制机会；commit之后ACK丢失先查receipt，旧4行outbox重启完整恢复；同BUG3次probe用尽后不继续自重试；source/dependency改变才重新armed。覆盖/质量/REAL门的验收由总方案后续批次完成。

## 当前证据边界

本报告能确认fault admission拒绝，无法确定主进程当前排空究竟卡在哪一步，因此本补丁补可核实阶段数据，不把某一假设写成已证实根因。当前coveredSettled异域hash比较由根补丁修复，prepared存储预算与全阶段trace由根补丁承接。CP12训练、rolling publication与图表补丁分别合并，不能仅应用本包就宣称软件全部闭环。
