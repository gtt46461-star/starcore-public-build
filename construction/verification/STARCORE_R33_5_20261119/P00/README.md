# P00 实际源码预演与构建前修订

在正式 033 源码 bb7b49bbcc9266bad548ec37e71403256fc06a4d 上应用了 P00 的 16 项文件操作，创建方案绑定，按两份现场报告中的最后可证安装版 577311033，执行版本四文件事务，得到候选 577311036。正式安装前应读回设备实际版本。用于比对的正式 033 APK 来自 [原签名运行](https://github.com/gtt46461-star/starcore-public-build/actions/runs/43796780479) 的附件 14195206594，实取 28,909,143 字节，SHA-256 为 8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763。

## 先提交两个受控生成文件

实际 Gradle 的 preBuild 调用 generateR967Semantics。它会写入 Git 已跟踪的两个文件。原 P00 后态带着 033 的旧生成文件时，单独执行原生成器会改变这两个文件，正式构建收尾的源码检查因工作树变化而拒绝。此次已复现 `R334_DIRTY_SOURCE_NOT_PUBLISHABLE`。修正后，同样的生成、原始校验和严格工作树检查均通过。逐文件核对覆盖 26,399 个已跟踪文件。

在各批完成实际源码移植和版本四文件更新后，审阅并提交源码时同时运行以下命令。使用独立候选工作树，`SOURCE` 指向本批 Android 工程。

```bash
cd "$SOURCE"
python3 -B scripts/r967_semantic_identity.py
python3 -B scripts/r967_semantic_identity.py --check
git add -u -- app/build/generated/r967/semantic_manifest.json \
  app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java
git diff --cached --check
```

将它们和本批其余已审核文件一并提交，再执行 `tools/r33/verify_plan_binding.py --repo "$SOURCE" --plan "$PLAN" --require-clean`。使用 [P00 生成文件前后差异](P00-tracked-R967.patch) 可复核 036 的精确后态。后续批次应从本批源码生成。[九阶段逐文件摘要](r967-by-stage.json) 显示 P00、B01、B02、B03、B04、B05、B07 各有两项变化，B06、B08 继承前一批字节。

## 分清编译前提及可交付前提

原 P00 检查的 EXACT_GIT_HISTORY 确认 033 祖先可见。真实浅克隆在该项得到 PASS，但最终源码 bundle 工具会拒绝浅历史。新增检查分别核原构建依赖、R967 已提交后态、完整历史标记以及原 APK 的实际字节。在正式交付环境使用完整历史 checkout，例如 Actions checkout 的 fetch-depth 取 0；完整性仍由最终 bundle 在空仓库恢复验证。构建与签名沿用仓库受权环境，版本取现场实测，证书与 APK 均核实际文件。

```bash
python3 -B "$P00_REVIEW/preflight_delivery.py" --repo "$SOURCE" \
  --android-sdk "$ANDROID_HOME" --baseline-apk "$ORIGINAL_033_APK"
```

本次预演的 JDK17 全源类型检查通过，共 1323 个 Java 源（含生成源码），另有两个资源/构建夹具。原件见 [版本记录](P00-version.json)、[严格工作树记录](clean-after-r967.json)、[生成与类型检查输出](generators-and-types.log)；其中源码提交和树哈希对应本次隔离预演。完整 Android 编译、原签名、设备覆盖安装、24/72 小时与自然资格各自等待正式运行。

[修正前](P00-original-candidate-readiness.json) 与 [修正后](P00-readiness.json) 的同环境复验均用同一正式 033 APK。修正后的生成字节已通过，交付依赖仍报告浅克隆和缺少配置完整的 SDK。当前工作区有单独的 API 36 类型库；预检按实际传入或配置的 SDK 根目录核查完整工具链。预检不会读取签名凭据。[预检回归结果](host-tests.log)

## 给 Codex 的追加施工指令

从仓库的 CURRENT_FINAL_PLAN.md 和 R33.5 原施工任务书开工。P00 至 B08 每批提交前按本页运行 R967、审阅两个已跟踪输出并随批提交；提交后重验 clean HEAD。在获准的正式签名环境中执行本页交付预检，使用原 033 APK、完整 Git 历史和受权原签名执行 run_release_build.py 及 package_ci_candidate.py，再按原方案交付 APK、完整源码及可从空仓恢复的 bundle。已归档的宿主预演作为施工前证据，当前版本的发布与设备证据从实际候选采集。
