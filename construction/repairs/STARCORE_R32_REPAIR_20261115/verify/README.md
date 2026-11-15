# 自检与复验

在含 032 源码对象的仓库根目录运行。结构检查使用 Python 3.9+ 与 Git，在临时目录重建原始文件。它验证五批、单文件写入归属、卡片与原文、编译方法引用、当前各表合同，并在原始 Git 对象上对五份补丁逐一正向应用和逆向恢复。

```bash
P=construction/repairs/STARCORE_R32_REPAIR_20261115/verify
python3 "$P/verify_plan.py" --repo .
```

从已应用 P1–P4 安卓补丁的生产 classes 运行四个宿主入口。CLASSES 替换为当前 Gradle 产生的实际目录。测试读取经裁减的 032 真机对象索引和两个阻断帧的字段。16 个 SQL 与过滤器由真实生产类重新导出，然后逐项对照计划。

```bash
P=construction/repairs/STARCORE_R32_REPAIR_20261115/verify
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
SDK36="$ANDROID_HOME/platforms/android-36/android.jar"
OUT=$(mktemp -d)
javac -source 17 -target 17 -cp "$CLASSES:$SDK36" -d "$OUT" "$P/ConstructionProbe.java" "$P/SuffixProbe.java" "$P/FieldContractProbe.java" "$P/ContinuationProbe.java"
java -cp "$OUT:$CLASSES:$SDK36" com.ethk3.novaonepro.strategyv2.ConstructionProbe "$P/index_fixture.json" "$OUT/ddl.json"
java -cp "$OUT:$CLASSES:$SDK36" com.ethk3.novaonepro.SuffixProbe
java -cp "$OUT:$CLASSES:$SDK36" com.ethk3.novaonepro.strategyv2.FieldContractProbe "$P/index_fixture.json" "$OUT/sql.json" "$OUT/streak.json"
java -cp "$OUT:$CLASSES:$SDK36" com.ethk3.novaonepro.strategyv2.ContinuationProbe "$P/summary_fixture_032.json" "$OUT/quota_queries.json"
python3 "$P/sqlite_probe.py" "$OUT/ddl.json"
python3 "$P/quota_pages_probe.py" "$OUT/ddl.json" "$OUT/quota_queries.json"
python3 "$P/verify_continuation.py"
python3 "$P/verify_plan.py" --repo . --query-dump "$OUT/sql.json"
```

P5.packaging 的宿主用例需要已应用 P5.patch 的打包器。两份原始现场 ZIP 由使用者持有时，可重算每一期、每张研究表和训练账的差异。字段复算只读原 ZIP，结果与已纳入方案的汇总逐字段匹配。

```bash
python3 "$P/delivery_probe.py" --repo .
python3 "$P/check_field_exports.py" EARLIER_032.zip LATER_032.zip --verify-baseline
```

只有在 Android 测试设备或模拟器上完成实际升级，才会产生该设备的安装收据。使用测试专用串号，预先准备同签名的两份 APK 和可访问测试应用数据目录的 shell。以下步骤会在所选设备安装两份 APK 并创建保全标记。

```bash
python3 "$P/verify_overlay_032.py" --serial SERIAL --old OLD_032.apk --candidate NEW.apk --out overlay.json
```

本次主机使用 JDK 21、Java 17 源码及目标与 SDK36 类型声明，编译了候选补丁后的 1309 个生产 Java 文件。BuildConfig、资源 ID、构建来源及两种 AndroidX 注解采用宿主编译用声明，数值生成类来自该工程。四个 Java 入口执行 66 项断言（原35项、本次新增31项），另有真实表声明的 SQLite 旧库回归、12 项发布身份及基线判断用例。五类反例专查循环、重复文件所有者、提前签收、未来读依赖与错误方法归属。本次结果见 [机器回执](RESULTS.json) 和 [宿主日志](HOST_PROBE.log)。今后的 Android 正式构建、完整旧库调度、上一版无损覆盖、后台 24/72 小时和自然未来质量各有独立取证要求，见 [现场交接](../guide/现场复验与批次交接.md)。


R32.1 新增语义核验覆盖全222项验收的实际读者及18个独立生产用例，另有4种篡改检查。SQLite用产品Java发出的同一组SELECT及DDL，重算4表共524条已有原件，分别跨过64、128两页；再测试找回单例后的写入、重试计费及保留原触发器。在两份相同032版的原报告验证32个开奖和32个原冻结，并故意少一条、改一条、冒充升级。正式P5覆盖安装须有另一次真正装上更高版本的收据。

```bash
python3 "$P/upgrade_pair_probe.py" EARLIER_032.zip LATER_032.zip
python3 "$P/check_upgrade_rows.py" IMMEDIATE_BEFORE_UPGRADE.zip IMMEDIATE_AFTER_UPGRADE.zip --overlay overlay.json
```

需要单独复现宿主类型编译时，将补丁应用到独立工作树，然后运行 `python3 "$P/compile_rehearsal.py" --repo PATH_TO_PATCHED_CHECKOUT --android-jar "$SDK36" --out EMPTY_DIRECTORY`。可用 `--expected-sdk-sha256` 精确锁定本次参考文件。宿主声明仅用于类型核对，安装包仍由正式 Android 构建和签名流程产生。
