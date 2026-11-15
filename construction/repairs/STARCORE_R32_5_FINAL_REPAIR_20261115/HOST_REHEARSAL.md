> 当前C1复验：36条顺序命令全部通过，410张文件卡覆盖312个受控路径；下文保留前轮35条记录的说明。完整文件与改法见BATCH_FILE_EXPLANATION.md。

# R32.5.1 当前五批复验入口

本次35条命令与回执位于[verification/r3251/staged/RESULTS.json](verification/r3251/staged/RESULTS.json)，当前工具同时检查显式POSIX权限；原R32.5记录只保全为历史记录。新增权限/恢复回归已并入同一run_staged_rehearsal入口，39个应用器及13个续施工用例不能代替222条业务验收。可先运行tools/verify_report_inputs.py重新核两份输入来源。

# R32.5 五批独立宿主复演

此入口从包内精确 `SOURCE_BASELINE.zip` 新建独立工作树，顺序应用 P1—P5，每批重新生成身份、编译当前阶段生产 Java，并运行该阶段声明的宿主合同。它不改现有软件工作树。正式 Android/Gradle、签名 APK、物理设备长测及自然资产效果仍按各自原验收执行。

## 环境和依赖

使用具备 Python 3、Git、JDK 21 的 Linux 环境。`tools/prepare_host_toolchain.py` 从固定的官方 Google SDK 和 Maven Central 地址准备依赖，并按 `audit/r325/TOOLCHAIN.json` 校验 SHA256；已有但哈希不匹配的文件会拒绝使用。依赖不包含在总包中，需要网络或事先放入相同哈希的原件。

在解压后的总包目录执行以下命令，工作目录名称可自选；复演输出目录必须尚不存在。

```bash
PACKAGE_PATH="$(pwd)"
TOOLCHAIN_DIR="$(dirname "$PACKAGE_PATH")/r325-host-toolchain"
REHEARSAL_OUT="$(dirname "$PACKAGE_PATH")/r325-host-rehearsal-new"

python3 "$PACKAGE_PATH/tools/verify_package.py" --package "$PACKAGE_PATH" --sealed
python3 "$PACKAGE_PATH/tools/prepare_host_toolchain.py" --out "$TOOLCHAIN_DIR"
python3 "$PACKAGE_PATH/tools/run_staged_rehearsal.py" \
  --package "$PACKAGE_PATH" \
  --out "$REHEARSAL_OUT" \
  --android-jar "$TOOLCHAIN_DIR/android.jar" \
  --sqlite-jdbc "$TOOLCHAIN_DIR/sqlite-jdbc-3.46.1.3.jar" \
  --slf4j-api "$TOOLCHAIN_DIR/slf4j-api-2.0.13.jar" \
  --json-jar "$TOOLCHAIN_DIR/json-20240303.jar"
```

命令顺序执行；任一步失败时先检查其日志，不带着失败继续签收。运行器在每个失败命令处停止，并保存 `RESULTS.json`、各命令日志、当前阶段 class 和数据库夹具。`PASS_SEQUENTIAL_STAGE_HOST_NOT_ANDROID_OR_FULL_PROJECT_CLOSURE` 仅表示声明的源码与宿主合同通过，不能据此签完 222 条原业务验收。

## 本轮已完成的复演

[当前总账](VERIFICATION.json)和[完整执行记录](verification/staged/RESULTS.json)保存本轮 35 个命令及退出码；五批生产 Java 数依次为 1309、1314、1314、1321、1321。没有借用后批 classes 完成前批验收。新归档活性 41 条、Q08/报告出口/真实 SQLite 60 条和设备采证脚本 30 条属于各自专项回归，不能相加后冒称 222 条业务验收通过。

[精确候选组成](verification/COMPOSITION_RESULTS.json)另核基线、五批应用、幂等性与逐文件后态；[二进制补丁回放](verification/PATCH_REPLAY.json)另核完整字节及可执行权限。[外部项目](OPEN_ITEMS.md)列出本轮没有取得的 Android、设备、原库和自然样本证据。


本次逐文件说明复核补齐C1：StorageGcR31.java与V51RuntimeOrchestrator.java原有源码及哈希完整，但各漏一张问题文件卡。现补入P1.06/P1.04，覆盖312个唯一受控路径，共410张文件卡；新增覆盖校验拒绝再次漏卡。原312路径、120实际改动及全部生产代码字节均未改变。当前复验以VERIFICATION.json所指的C1记录为准。
