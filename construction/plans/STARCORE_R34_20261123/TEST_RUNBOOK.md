# 验证入口与结果含义

先取得原始095完整Git检出和当前完整方案目录。下面使用绝对路径示意，按实际目录替换；所有输出应放工程外的独立工作目录。执行测试只改变临时夹具，不写用户设备数据库。

## 1. 文件、映射、前态与原ZIP

在本计划根目录运行：

```bash
python3 tools/r34/verify_plan.py --source /absolute/path/to/actual095 --reports /absolute/path/to/construction/evidence/STARCORE_R34_20261123/raw
python3 verification/verify_supplied_patch_set.py --plan . --source /absolute/path/to/actual095
python3 tools/r34/test_plan_guards.py
```

verify_plan核sealed文件、SHA256、66问题/66旧要求、文件反链、材料适用批、18图/20矩阵、导航链接和可选原件/源码。seal本身不是数字签名；须与已审阅Git提交或包外receipt里的manifest SHA256比较。没有指定source或reports时明确NOT_CHECKED，而非暗示已核对。

## 2. 局部Java与SQLite验证

```bash
python3 payload/runtime/tests/verify_runtime_payload.py --baseline /absolute/path/to/actual095
python3 payload/runtime/supplement/tests/verify_schema_unit_plan.py --baseline /absolute/path/to/actual095
python3 payload/learning/tools/r34/verify_cold_rebirth.py --source-root /absolute/path/to/actual095 --output /absolute/path/to/isolated-r34-tests/cold
```

运行与schema脚本会更新其payload回执，重跑请在方案副本内进行，避免修改已sealed字节。使用真实JDK compiler与Python sqlite3；有环境差异需据实际结果说明。具体测试边界在各脚本及CODE_PAYLOAD中，不得把“native SQLite合同”扩写成“Android数据库设备故障通过”。

## 3. 实施后的交付检查

```bash
python3 tools/r34/check_acceptance.py --scope FINAL_SCOPE.json --plan-manifest MANIFEST_SHA256.json --receipts /absolute/path/to/batch-receipts.json --root /absolute/path/to/artifacts --apksigner /absolute/path/to/android-sdk/build-tools/apksigner --aapt2 /absolute/path/to/android-sdk/build-tools/aapt2
python3 tools/r34/verify_period_trace.py /absolute/path/to/read-only-period-trace.json
```

当前control/DELIVERY_RECEIPTS_OPEN.json故意没有应用批次回执，检查器应返回OPEN/非零退出。这是缺证状态，不是执行器故障。真实APK安装applicationId为`com.starcore.research.r13`；`com.ethk3.novaonepro`是Java namespace，不能拿它判安装身份。签名、版本、大小与hash核验仍不等同于设备/自然有效。

周期检查器只核归一化记录内部一致性：它不能验证有人提供的JSON是否真的来自手机。生产导出器必须将每行联接至原SQLite行/commitReceipt/原始数据，并按B04/B12协议核全机会分母、服务缺口和长期质量；输入标SYNTHETIC的案例永远不得作自然证据。

## 4. 继承回归

R335_CROSSWALK.json每旧ID都有test_contracts；EXISTING_ENTRY_REQUIRES_REVALIDATION只表示真实095文件/方法存在，不表示本轮运行；NEW_TEST_REQUIRED是逐case实施合同，指定的新runner/JUnit尚待施工。每case都有正输入、拒例、断言和责任新工单。不得创建空方法返回PASS，不得把人工插入成功receipt当生产闭环。

完整Android/Gradle/NDK构建、APK签名安装、100期及24/72小时目标机观察、未来资格验证仍分别执行，不能由上述脚本合并宣布应用已完成。
