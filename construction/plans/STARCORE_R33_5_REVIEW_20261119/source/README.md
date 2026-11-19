# 033 源码身份与各批 APK 验收修订

## 施工源与报告身份

仓库的方案发布分支和构建分支分开。正式 033 的构建提交为 `bb7b49bbcc9266bad548ec37e71403256fc06a4d`，树为 `91481826c9db1fb1d39439b799770c7336c83fea`。将方案放在应用工作树外，从这个提交建立应用工作树。已有后续改动依原方案做三方移植，保留用户修改。在各批独立提交后执行 P00 提供的对应阶段构建和版本生成器。当前 main 的 `.github/workflows/build-apk.yml` 产出 C024 debug，`.github/workflows/r13-startup.yml` 使用固定历史 run ID，两者均不能提供该新候选的源码、签名和运行回执。

两份历史交付的 applicationId 都是 `com.starcore.research.r13`，versionCode 都是 `577311033`。绑定导入基线时校验 APK SHA-256。识别检测报告自身的构建身份时，从该报告独立提取 sourceCommit 和可用的安装包身份，缺失项如实列出。

| 033 交付 | 真实源提交 | APK SHA-256 | 原证据 |
|---|---|---|---|
| 正式构建 | bb7b49bbcc9266bad548ec37e71403256fc06a4d | 8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763 | [正式交付收据](https://github.com/gtt46461-star/starcore-public-build/blob/0fcf8b9e6286e24dc185328af0d02a0458aaee03/DELIVERY.json) |
| 早期候选，收据记录 UI_REPORT_INCOMPLETE | 9df4ba2ee35023f911a4de05569d2555b6416e47 | eb5956d4103d71d56f99fe3856d5255c90908154c3cc4f8812aae9bb442831bd | [早期候选收据](https://github.com/gtt46461-star/starcore-public-build/blob/dc07a12df689cdd9186f4d0e635d4a656457816e/DELIVERY.json) |

本轮通过 GitHub Git Trees/Contents 重新读取正式 033 对象，保留 220 个相关路径的原 Git blob 或缺席状态。先逐一比对原方案 140 个源码绑定，再逐批核对 129 个安装路径的 142 次变更（103 个替换、39 个新增），共享路径以上一批的真实后态作为本批前态。结果见 SOURCE_CHAIN_RESULT.json。此项核验范围为已登记路径和字节。是否符合 Android 运行要求仍需真实 Gradle 与设备收据。

```bash
python3 -B "$REVIEW/source/check_source_chain.py" "$R335_PLAN"
```

## B08 动态版本验收

原包 `B08/R335_施工与交付.md` 最终验收示例显式传入 035。另一份 B08 入口省略版本参数，验证器默认也为 035。P00 安装的 `advance_release_version.py` 每批实际递增并生成新的 versionName。在完成原方案的 `run_release_build.py` 和 `package_ci_candidate.py` 后，B08 独立 APK 验收使用以下命令，替换固定 035 示例。任何阶段申请安装验证时都可复用。安装前保留目标设备的原始输出，且连接多台设备时用 `adb -s` 指定该次设备。

```bash
adb -s "$DEVICE_SERIAL" shell dumpsys package com.starcore.research.r13 > "$R335_OUTPUT/device-before-upgrade.txt"
python3 -B "$REVIEW/source/verify_delivery.py" \
  --plan "$R335_PLAN" --repo "$R335_REPO" \
  --candidate "$R335_OUTPUT/candidate" --dumpsys "$R335_OUTPUT/device-before-upgrade.txt" \
  --baseline-apk "$R335_BASELINE_APK" \
  --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt" \
  --apksigner "$ANDROID_HOME/build-tools/36.0.0/apksigner" \
  --out "$R335_OUTPUT/artifact-verification.json"
```

工具读取本批已提交源 HEAD、发布 manifest 和候选交付收据，核对 APK 文件名与实际 SHA；只采用带有目标包名的明确安装版观察，再交由原验证器重新读取候选与基线 APK、验证签名和严格高版，保留原终审需要的收据格式。多个矛盾的安装号、未知包名、候选被替换均须复查。全新设备尚未安装时，应记录为全新安装测试，继续在确实保有旧版的设备上做升级验收。发布收据、APK 安装验证、真机 24/72 小时及自然未来胜率分别签收。

```bash
(cd "$REVIEW/source" && python3 -B -m unittest -v test_verify_delivery)
```

本轮在宿主环境验证动态取号和错误输入处理，包含高于 035 的合法候选、无目标包、矛盾的安装号和篡改候选字节。未在本次修订中运行该候选 APK 的现场签名与设备链。
