# 还原、前缀编译和签收证据

**认证原卷的宿主检查。** host-receipts.zip 是本轮用 JDK17 重跑原 53 套宿主的完整 HOST_RUNS、106 份输出及最终成员清单。先用 seal_package_r335.py --before-host 封执行输入，按 run_host.py 在独立副本逐套执行，重建最终 seal，verify_package.py 复验成员与实际执行输入。它对应材料恢复后的原算法版本；本轮新增的 Java 和签收条件另有下列检查。115 项 custody 宿主检查另以本轮重新编译的基础 B08 前缀验证，结果见 base-custody-recheck.log。

**当前修订的工程源码。** verify_reviewed_prefix.py 复用原包的全源验证、7 个原工程生成器和全源码类型编译，在 B05 安装修正后的完整 Java 和对应 R82 身份。每个后续阶段比对实际后态，B07 按经核实的前态安装新的 R82。在全新临时目录验证固定 033 和 P00–B08；真实工程施工使用 replay/stage_checked.py。prefix-results.json 按阶段分别给出基础和当前修订的源码计数、输入摘要、退出码与编译回执摘要。reviewed-prefix-receipts.zip 保存本轮修订的十份全量源码编译回执、生成器及原始编译日志。

```bash
python3 -B "$REVIEW/verify_review.py"
python3 -B "$REVIEW/check_plan.py" "$PLAN"
python3 -B "$REVIEW/verification/verify_reviewed_prefix.py" \
  --plan-root "$PLAN" --baseline-source "$SOURCE_033" --work-dir "$NEW_EMPTY_WORK" \
  --android-jar "$ANDROID_JAR" --annotation-jar "$ANNOTATION_JAR" \
  --jdk-mode jdk17 --java "$JAVA_HOME/bin/java"
```

SOURCE_033 为正式提交的完整 033 工作树。需要核验 Git 祖先并发布时应保留完整 Git 历史。ANDROID_JAR 为 Android 36 平台的 android.jar，ANNOTATION_JAR 为原 AndroidX annotation 1.3.0。宿主数值回归需 JDK 有 javac，恢复工具用 git apply。执行包的 CHECK、全源码编译、签名 APK、真机及自然样本分别记录。check_plan.py 消费已组装基础方案，可重复运行。

**现场签收。** 按 replay/README.md 收件和运行 final_review.py。验收工具按实际候选的版本、包名、证书和源提交联查。没有新签名构建、原设备长期活动或足够自然窗口时，分别保留相应待项。实际达标程度可按原 32/96/288 及 1832/222 登记。

在修订版 B08 编译前缀上也重跑了相同 115 项 custody 检查。复核工具对候选侧的两条已改路径采用当前 SHA，逐项保留原包载荷、生成器、所有编译输入与其余最终文件的校验。复演时设置 R335_CUSTODY_PREFIX_WORKDIR 为本次 verify_reviewed_prefix.py 的输出目录，R335_ANDROID_JAR、R335_ANNOTATIONS_JAR 为相同的真实依赖，然后运行下列命令。

```bash
python3 -B "$REVIEW/verification/check_reviewed_custody.py" --plan "$PLAN"
```

结果见 reviewed-custody.log，最终源码输入摘要可与 prefix-results.json 的 reviewed/B08 核对。
