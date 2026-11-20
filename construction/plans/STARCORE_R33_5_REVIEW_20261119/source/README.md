# R33.5 源码身份、构建预检与各批APK验收

本页使用2026-11-20当前修订：**9阶段、66项施工问题、164次文件操作、138条不同生产路径**。完整后态与跨批前态以[生产补正清单](../production/OVERLAY.json)和[最终范围](../FINAL_SCOPE.json)为准；批次安装统一使用[当前执行入口](../replay/README.md)。

## 正式源码与两份报告的身份

正式033构建提交为 `bb7b49bbcc9266bad548ec37e71403256fc06a4d`，树为 `91481826c9db1fb1d39439b799770c7336c83fea`。当前main保存施工方案；应用工作树从该确切033提交建立，方案放在应用工作树外。已有改动保留原分支和字节，按施工卡做独立检出或受控三方移植，不把当前方案发布分支自动当成已完成的应用源码。

两份历史设备报告对应正式033源码/APK。下表的另一033候选用于识别相同versionCode的不同源码身份，不能代替正式导入基线。applicationId均为 `com.starcore.research.r13`，versionCode均为 `577311033`；比较时必须同时核sourceCommit及实际APK摘要。

| 033交付 | 真实源提交 | APK SHA-256 | 原证据 |
|---|---|---|---|
| 正式构建 | bb7b49bbcc9266bad548ec37e71403256fc06a4d | 8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763 | [正式交付收据](https://github.com/gtt46461-star/starcore-public-build/blob/0fcf8b9e6286e24dc185328af0d02a0458aaee03/DELIVERY.json) |
| 历史早期候选，收据记录UI_REPORT_INCOMPLETE | 9df4ba2ee35023f911a4de05569d2555b6416e47 | eb5956d4103d71d56f99fe3856d5255c90908154c3cc4f8812aae9bb442831bd | [早期候选收据](https://github.com/gtt46461-star/starcore-public-build/blob/dc07a12df689cdd9186f4d0e635d4a656457816e/DELIVERY.json) |

识别报告自身身份时从该报告独立提取sourceCommit及可用安装包身份，缺项按实际列出。新候选只使用本次源码提交及实际构建回执；无关debug产物、固定历史作业号和旧APK都不能替代当前发布。

### 历史源绑定与当前完整清单

2026-11-19的 `BASELINE_TREE_PATHS.json` 保存正式033的220个相关路径blob或缺席状态；`SOURCE_CHAIN_RESULT.json` 核原方案140条源码绑定、129条路径的142次变更（103替换、39新增）。这些是原封包的源身份记录，其历史核查命令仍可复验：

```bash
python3 -B "$REVIEW/source/check_source_chain.py" "$PLAN"
```

该命令不包含本轮新增路径。当前完整范围通过 `replay/production_overlay.py` 组成有效清单，再由原 `validate_install_manifest` 核全部前后态，安装和完成源入口共同消费该清单。B02/B04/B07修订、R82身份及两个已跟踪R967生成文件均在当前范围内；旧双文件继承结果及旧R82常数不得作为本轮权威后态。

每批源身份须同时保留原方案seal、当前review seal、有效overlay/清单摘要、实际源提交与版本。最终B08执行 `replay/verify_completed_source.py`，核每批实际引入版本的提交、最终HEAD、工作树和索引的累计生产后态。详见[源码继承与终审](../replay/README.md)。

## 版本四文件与R967输出随批提交

P00至B08每批都执行原 `tools/r33/advance_release_version.py`，根据当前已提交版本和目标设备真实安装版本递增，用原R926生成器一致更新release_manifest.json、release_identity.properties、ReleaseManifestR926.java和宿主BuildConfig.java。版本事务的journal保存到工程外，真实安装观察留原件。零生产差异的B06/B08同样完成该事务与独立验收。

本轮安装器已经把以下两项R967输出纳入受影响批次的正式完整后态，施工时随批审阅并提交：

- `app/build/generated/r967/semantic_manifest.json`
- `app/build/generated/source/r967/com/ethk3/novaonepro/ResearchSemanticsGeneratedR967.java`

提交前运行原生成器的 `--check`；提交后必须验证实际生成字节与HEAD一致。若生成结果与登记后态不同，先查明源文件或依赖变化并重新完成受控身份审阅，不能忽略生成文件、只提交版本号或沿用旧资格。旧的手工补提说明保留历史价值，当前安装以完整清单为入口。

## 可移植的正式交付预检

预检已收录在本轮 `source/preflight_delivery.py`，经 `prepare.py` 展开后即可随 `current-review` 使用，无需引用旧会话临时目录或历史P00工作目录。P00生产工具和原R967生成器都按审阅摘要核实后才执行。

```bash
python3 -B "$REVIEW/source/preflight_delivery.py" \
  --repo "$SOURCE" --android-sdk "$ANDROID_HOME" \
  --baseline-apk "$BASELINE_APK" > "$DELIVERY_PREFLIGHT_JSON"
```

它分别检查原P00构建依赖、完整Android SDK、已验证且已提交的R967输出、Git浅历史标记，以及上述正式033原APK实际SHA。只有这些输入齐备才返回 `READY_TO_ATTEMPT_DELIVERY`；否则输出 `DELIVERY_INPUTS_INCOMPLETE`、具体缺项并以退出码2结束。它不读取签名凭据，也不把输入齐备当成构建或设备已经完成。

正式构建检出必须具有完整历史，例如在实际受权构建作业中将checkout的fetch-depth设为0。`FULL_HISTORY_REPORTED` 只是预检条件；最终源码bundle仍须在空仓库实际恢复并核源提交。已有本地Git对象可见033祖先，与可交付完整bundle是两项不同的验证。

通过预检后，在已审阅、已提交且clean的候选上，按原施工卡执行 `tools/r33/run_release_build.py` 与 `tools/r33/package_ci_candidate.py` 的真实Gradle/原签流程。保存完整构建日志、CI_BUILD_RECEIPT、实际APK、源码提交、签名身份、完整源码和可恢复bundle，再做下述独立APK核验。仓库提供的受权签名环境继续承担签名，当前预检不产生替代证书。

## 每批及B08的动态版本APK验收

原包B08示例曾固定035，部分原验证器默认值也为035。这些仅属历史示例。当前每批由原版本事务实际递增，APK验收从已提交的release manifest与实际候选取版本，不手填固定035或本次源码预演的044。

在完成同一候选的真实构建和候选打包后，先保存目标设备安装前的原始输出。连接多台设备时指定本次设备：

```bash
adb -s "$DEVICE_SERIAL" shell dumpsys package com.starcore.research.r13 > "$OUTPUT/device-before-upgrade.txt"
python3 -B "$REVIEW/source/verify_delivery.py" \
  --plan "$PLAN" --repo "$SOURCE" \
  --candidate "$OUTPUT/candidate" --dumpsys "$OUTPUT/device-before-upgrade.txt" \
  --baseline-apk "$BASELINE_APK" \
  --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt" \
  --apksigner "$ANDROID_HOME/build-tools/36.0.0/apksigner" \
  --out "$OUTPUT/artifact-verification.json"
```

工具核本批已提交源HEAD、发布manifest、候选交付收据和APK实际SHA，只采用带目标包名且无矛盾的安装版观察；再调用原验证器实际读取候选与基线APK，核原签名及严格升级版本，保留原终审收据格式。未知包名、矛盾安装号或候选字节被替换均拒绝。新设备尚未安装时记录为全新安装测试，覆盖升级验收仍在保有旧版的设备上完成。

```bash
(cd "$REVIEW/source" && python3 -B -m unittest -v test_verify_delivery test_preflight_delivery)
```

这些宿主回归检查动态取号、候选/输入错配及预检条件。签名APK必须来自本次真实作业；升级、重启、后台与24/72小时设备运行必须保留现场回执。自然未来样本、资格证书、撤销及DecisionGate出手按各自合同单独评估。

## 本轮真实源码预演的证据边界

本轮最终有效后态已从真实033 Git对象依次回放九阶段，实际通过164次操作/138路径的版本继承、原R967校验与逐批clean检查；独立终验再次覆盖九阶段。最终隔离提交 `6bba61d08cf3494817082010b2410050144509d0`，训练规则SHA为 `619fbc6ba89b6448e8eb663fd303c951b5fe7628ce0f91bfa6f18015ee629bac`。删除B07新增生产类的隔离负例被终验拒绝。

该预演检出是浅克隆，报告明确记录这一事实。它不能充作完整bundle、正式APK、原签覆盖安装、设备长测或自然策略质量证据。正式发布继续执行上面的完整历史预检、实际构建签名、空仓bundle恢复和现场验收；本轮源码回放版本只标识预演，不据此推定目标手机当前版本。

本轮接收方的 `verify_delivery.py` 同时检查收到的源码 ZIP、完整 Git bundle 和 APK。需要单列源码回执时加 `--source-out "$RAW/received-sources.json"`。检查在新建空 bare 仓独立恢复 bundle，重读 ZIP 全部成员和模式，核对源码树、提交以及正式 033 祖先。B08 终审自动把结果写入 `receivedSources`。[接收端校验与拒绝用例](test_check_source_delivery.py)
