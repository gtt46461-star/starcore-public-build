# 自检与复验

从该源码仓库的根目录执行以下命令。Python要求3.9以上，git需要能读取032应用提交；程序在临时目录应用补丁并逆序还原，仓库中的应用文件保持原版本。

```bash
python3 construction/repairs/STARCORE_R32_REPAIR_20261115/verify/verify_plan.py --repo .
```

本包附了两份032报告中的精简对象索引和实际Java测试入口。index_fixture.json 是后包的受限字段投影，不包含原机数据库、号码历史或完整私有附件。若已用正常Gradle构建得到打过P1至P4补丁的production classes，可用JDK17+编译以下两个宿主入口（Android SDK 36的android.jar用于Android类型解析）。

```bash
P=construction/repairs/STARCORE_R32_REPAIR_20261115/verify
CLASSES=app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes
SDK36="$ANDROID_HOME/platforms/android-36/android.jar"
OUT=$(mktemp -d)
javac -source 17 -target 17 -cp "$CLASSES:$SDK36" -d "$OUT" "$P/ConstructionProbe.java" "$P/SuffixProbe.java"
java -cp "$OUT:$CLASSES:$SDK36" com.ethk3.novaonepro.strategyv2.ConstructionProbe "$P/index_fixture.json" "$OUT/ddl.json"
java -cp "$OUT:$CLASSES:$SDK36" com.ethk3.novaonepro.SuffixProbe
python3 "$P/sqlite_probe.py" "$OUT/ddl.json"
```

本次实做包括隔离工作树4份补丁正向应用与反向还原、静态拓扑/所有权/继承范围自检、编译打补丁后的1309个生产Java文件、运行20项直接调用实际生产类的断言、SQLite按真实Hypothesis DDL执行故障/恢复。宿主编译采用Java 21的编译器、Java17 source/target和SDK36类型声明；BuildConfig、资源ID、构建来源字段及两种AndroidX注解在宿主用编译专用替身。测试没有使用或生成手机签名成品。SQLite用例调用实际表声明，迁移步骤用有针对性的旧表夹具重放，完整Android迁移调度仍需P1施工验证。自然命中、手机后台、24/72小时、032覆盖安装仍待实施后的设备数据。机器结果、日志与验证边界见RESULTS.json。
