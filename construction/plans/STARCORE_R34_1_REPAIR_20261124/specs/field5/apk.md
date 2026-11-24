# 每批 APK 与工程回执对账

122 的 `tools/r33/run_release_build.py` 实际调用 Gradle 的 release、debug、androidTest 三个组装任务，保留构建前后的干净源码身份、退出码、Gradle 日志摘要和 release APK 字节摘要。独立包校验使用 `tools/r33/package_ci_candidate.py::inspect` 所用的 Android SDK 工具与原公开证书指纹。第 4 版 `verify_receipt.validate` 复验单批工程测试，B02 等工程路径没有统一读取或验证该批的独立发布 APK。用例全部通过后，构建和下载仍需分别取证。

**B01 就交付构建回执。** B01 在原发布 wrapper 中继续记录原 `planManifestSha256`，同时新增 `r341PlanManifestSha256, r341PlanCommit, r341Batch`。固定 122 的 `verify_plan_binding.verify_plan` 将旧 `verification/SHA256.json` 实际摘要与 `release/r33_plan_binding.json` 的 `planManifestSha256` 核对并返回 `plan_manifest_sha256`；`identity` 将 R34 结果另存 `result["r34"]`。因此前缀工具还核对应用提交中 R33.5 绑定与 122 的 Git blob 一致，旧回执摘要与该固定绑定完全相等。后者分别取本次已提交的封印、方案提交和当前源码批次。保留原 `sourceCommit/sourceTree/sourceClean/sourceCleanBeforeBuild/sourceUnchangedDuringBuild/command/exitCode/wrapperExitCode/gradleRelease/gradleLogSha256/durationNs/apkSha256/identityError/buildLaunchError` 与原 Gradle 日志。本版验收在干净产物写出后另取 APK；不在同一个源码提交内写本提交的 SHA。

批次前缀验收从 B01 到本次 `--through` 的每批按顺序验证。应用施工仓库须包含已取得的确切方案提交；校验器用该提交的 MANIFEST blob 核对本地封印。每批的工程回执和它声明的证据仍按原验收工具复验。将各批回执各自放在对应证据根目录下，所有索引引用使用相对索引目录的 `{path,sha256}`。一份最小索引的结构如下，摘要等字段由脚本读取真实产物后填写。

```json
{
  "schema": "STARCORE_R341_BUILD_PREFIX_1",
  "planCommit": "<实际方案提交>",
  "batches": [{
    "batch": "B01",
    "engineering_receipt": {"path":"B01/engineering/receipt.json","sha256":"<实际摘要>"},
    "build_receipt": {"path":"B01/build/CI_BUILD_RECEIPT.json","sha256":"<实际摘要>"},
    "gradle_log": {"path":"B01/build/gradle-build.log","sha256":"<实际摘要>"},
    "apk": {"path":"B01/build/app-release.apk","sha256":"<实际摘要>"},
    "apk_identity_member":"assets/<B01实际生成的身份成员>",
    "download_url":"<授权发布端的直接HTTPS地址>"
  }]
}
```

APK 中 B01 既定的 `batch/sourceCommit/sourceTree/sourceBaseline/planCommit/planSha256/versionCode/versionName` 与工程回执的实际源码、`release/r341_plan_binding.json`、当前 Git 树及 `release/release_manifest.json` 核对，版本码须高于 122 和前次已建的包。原有已安装版本、发布索引及冲突检查继续在 B01 构建前和现场覆盖验收执行。只有 SDK 读取到正确包名、版本和原签名时才可签收 `ORIGINAL_SIGNED`。从已有合规构建成功取得 APK 但本次缺 SDK，记录 `LOCAL_CANDIDATE_SDK_NOT_CHECKED`；未知或错误签名不签正式发布。没有原密钥且安装包组装受原构建门阻塞时，在索引行保留工程回执，`apk/build_receipt/download_url` 留空并填写可复验的 `reason`。工具单独输出 `BUILD_PENDING`，后续批次的已授权开发和工程回归继续推进。不要输出私钥、令牌、密码或临时签名 URL。

```bash
python3 -B "$p/tools/verify_batch_builds.py" --plan "$p" --repo "$APP_REPO" \
  --plan-commit "$PLAN_COMMIT" --index "$BUILD_PREFIX_INDEX" --through B01 --require signed \
  --aapt "$ANDROID_AAPT" --apksigner "$ANDROID_APKSIGNER"
python3 -B "$p/tools/verify_batch_builds.py" --plan "$p" --repo "$APP_REPO" \
  --plan-commit "$PLAN_COMMIT" --index "$FINAL_BUILD_PREFIX_INDEX" --through B12 \
  --require published --verify-downloads --aapt "$ANDROID_AAPT" --apksigner "$ANDROID_APKSIGNER"
```

`--require local` 只用于核对本地已构建字节、工程来源与内嵌身份。`--require signed` 在原签名发布能力齐备时使用。对授权发布地址启用 `--verify-downloads` 后，逐份通过 HTTPS 读取 APK，和本地原包逐字节长度、SHA-256 对账，`published` 要求此前缀每批均已签名且回下载成功。发布链接暂缺时署明待回读。授权端提供 ZIP 时，填写该 ZIP 的下载地址，并增加 `download_container={bytes,sha256,apk_member}`。工具先核对完整容器字节，再流式读取唯一指定 APK 成员与本地原包核对，避免把一个正确的 ZIP 名称当成同版 APK。发布端需登录时保留该项待办，按既有授权发布流程提供可访问的制品；密钥不得写入公共清单。退出码 0 表示满足所选层级，2 表示已核的工程之外仍有待交付项，1 表示证据错误。第 n 批验证不依赖第 n+1 批；B12 的工程回执先生成，本工具从其外部核全十二批，防止自引用。设备安装、24/72 小时和自然成绩继续使用各自原件。

可在方案目录运行 `python3 -B tools/test_batch_builds.py`。宿主测试创建临时 Git 历史、测试 ZIP，并模拟 Android 工具输出和下载响应，覆盖跳批、错包、旧方案摘要冒充新封印、错源码、失败的构建、日志缺成功行、错证书、版本码未递增、假下载和未通过的工程票。真实发布验收仍由实际 SDK、签名产物和发布地址执行。
