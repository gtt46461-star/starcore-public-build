# P5.01 集成故障、升级和签名交付

## 根因与现场证据

032对应源工程已有653单测通过；并没有由此得到24/72小时合格、真机质量或闭环运行证据。

## 本批修改与事务边界

按正常入口构建，保留同包/同签名、数值ABI与版本迁移记录，versionCode高于032及仓库现有版本；clean安装+从032覆盖，无清数据，验证冷启/断网/重启/锁屏/导出/主链。输出APK和内含同字节APK的ZIP及SHA256、证书摘要、源码提交、设备/自然证据状态。

每批只运行受改路径的最短回归；完整联测集中一次同提交运行，失败按首个失败定位最小修复集；release签收严格执行原DEVICE规则。外部原机不可用时可以产测试交付，但发布状态明确留空或待验。

## 本批验收与故障反例

验证当前SHA的Gradle、关键host/Android路径、apksigner、安装升级、ZIP读回。24h/72h实测与自然样本单列；条件未到不冒领。 加跑 verify/check_upgrade_rows.py：先固定032的32个开奖和32个已结算冻结，再对照候选导出与真机装前装后字节收据，另对模型/证书/CAS原件跨升级分别重放。

## 文件与完整源码

- [app/build.gradle](51a0b488f963.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/app/build.gradle)
- [.github/workflows/r31-verification.yml](3e9833e32807.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/.github/workflows/r31-verification.yml)
- [tools/r31/package_release.py](f0ed401874b6.md)：REPLACE；[完整后态代码](../../code/P5/files/tools/r31/package_release.py)
- [tools/r31/upgrade_matrix.py](ebaad8b7aae6.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/tools/r31/upgrade_matrix.py)
- [tools/r31/run_current_acceptance.py](cc159ccc2450.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/tools/r31/run_current_acceptance.py)
- [app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java](ddca867c7159.md)：REPLACE；[完整后态代码](../../code/P1/files/app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java)
- [app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java](8ac156fb01f8.md)：REPLACE；[完整后态代码](../../code/P3/files/app/src/main/java/com/ethk3/novaonepro/strategyv2/CertificateGateR27.java)
- [app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java](34e801bcb32d.md)：REPLACE；[完整后态代码](../../code/P4/files/app/src/main/java/com/ethk3/novaonepro/ReportExportServiceCP4.java)
- [tools/r31/test_package_release.py](c8f0fd46681e.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/tools/r31/test_package_release.py)
- [.github/workflows/r31-current-live.yml](affd3f2fb1d9.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/.github/workflows/r31-current-live.yml)
- [tools/r31/run_final_runtime.sh](9f87062953ad.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/tools/r31/run_final_runtime.sh)
- [tools/r31/run_phone_report_android.sh](1fb23e6a764e.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/tools/r31/run_phone_report_android.sh)
- [.github/workflows/r31-publish-release.yml](5fb305df9f49.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/.github/workflows/r31-publish-release.yml)
- [.github/workflows/r31-report-publish.yml](29e263dbfedd.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/.github/workflows/r31-report-publish.yml)
- [tools/r31/publish_report_package.py](f416b3173a78.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/tools/r31/publish_report_package.py)
- [tools/r31/run_emulator_upgrade_matrix.py](0970034b4455.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/tools/r31/run_emulator_upgrade_matrix.py)
- [release/release_identity.properties](1864e244ed8f.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/release/release_identity.properties)
- [release/R31_REPORT_PUBLISH_REQUEST.json](0f1a24188429.md)：PRESERVE_REFERENCE；[完整后态代码](../../code/P5/files/release/R31_REPORT_PUBLISH_REQUEST.json)
