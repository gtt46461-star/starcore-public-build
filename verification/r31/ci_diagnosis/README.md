# R31 P01 CI 实际故障与复核

固定产品源码：`844a78488f396189c2db98591782b7bb214799d2`。签名支持分支的构建请求提交与产品源码提交分别记录，不能混为 APK 源码。

第二次 run `41811278444` / job `126510864590` 的原始错误已由 GitHub job 日志读取：SU1 的前四项检查 PASS，随后 `HOST_COMPILED_PRODUCT_DEPENDENCIES_MISSING: activate JDK17/SDK36 and compile current Android source first`，退出码 1。见 `run41811278444-verifier-failure.log`。该 run 已成功恢复既有公有证书并通过 R31 源码校验；失败发生在首次 Android 编译之前，尚未执行 APK 或模拟器验收。

根因是 `scripts/host_product_classpath.py` 要求真实 `EvolutionLoopR27.class` 与 SDK36，但原 CI 在首次 Gradle 编译前调用 SU1、plan1025 和 R26 验证器。本地已有 class 的工作目录不能代表 CI 的干净 checkout。修复仅为在这些验证器前增加实际、未排除任何任务的 `./gradlew --no-daemon --max-workers=4 -Pr30Instrumentation=true :app:compileDebugJavaWithJavac`，保留后续全部验证器、canonical、lint、签名构建与 Android 测试。

已在 `/tmp/r31-ci-844a-frozen` 完整、隔离、固定 HEAD 的 checkout 实际复核：

- 冻结调用资产验证 PASS，37,456,148 bytes，SHA256 `2149b032f75929e2875ef8de8df59bf6029d2e1103342cb6a3e0b25d93116f03`。
- 完全无排除的首次 Android Java compile PASS，42 秒，27 tasks；真正执行编译及 `generateR982SourceCallAsset`，没有 rebind。
- 编译后 SU1 21 checks PASS、plan1025 20 checks PASS、R26 32 checks PASS，原始日志及完整结果 JSON 已保留。
- 每次结束后该隔离源码 `git status --porcelain` 均为空。默认沙箱 Gradle socket 失败和正常提权后的真实结果分别保存；未修改代理、网络策略或产品字节。

root 已在签名支持分支提交 `7e65517e935282fd6343511f0febf6f70990c2fa`，产品源码仍固定 844a，触发 run `41812391330`。其运行状态单独保存在 `DIAGNOSIS.json`；本地通过不自动成为远程通过。运行中的 job 日志端点曾返回 BlobNotFound 404，表示日志尚未生成，不能写成测试失败。

本目录不证明已取得 APK、模拟器通过、用户真机同签升级、24/72 小时运行或自然策略质量。只能在对应新 CI 日志和实际产物核验到位后更新这些状态。签名私钥和凭据没有导出；GitHub artifact 若返回 FileService 引用，只作为用户可取得的文件链接，不交给 Sediment `download_file`。


第四次 run `41814981145` 固定源码为 `31fea28c5723ac70e47ae4cc7542344b16fb3f91`，签名支持提交为 `a86d595f212658e2a6a0b5a1556e81d83211c333`。其 canonical/unit/lint 与 release/debug/androidTest APK 编译实际 PASS（13m43s，159 tasks）。随后第 15 步 legacy native 回归 exit 1；原 workflow 重定向的该步日志未由 ERR trap 打印，不能根据空白终端猜测错误。独立签名与嵌入源码身份核验尚未运行，Android 模拟器 job 因上游失败跳过，所以此 run 不能作为可交付 APK 验收。完整 Actions job 原始日志见 `run41814981145-full.log`。

隔离 `31fea` 的生产 task-runtime/writer native 验证已另外真实执行并保留 `exact31fea-native/`，两个 receipt 的生产源码哈希均逐字节核对一致。legacy native 原始失败及后续适配保留于 `exact31fea-legacy/`；后续有 root 的 `LiveShadowsR27` PIT 运行期 DDL 最小修复及合法 claim fixture，必须按该组合单独记录，不能写作原 `31fea` 全部通过。
