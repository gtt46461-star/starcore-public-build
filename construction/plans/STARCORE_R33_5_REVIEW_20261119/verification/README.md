# 当前修订的复演与签收证据

当前全批入口见[第三次复核](../REVIEW_THIRD_20261120.md)与[一条完整施工指令](../CODEX_EXECUTE.md)。实际签署与现场原件按本次修订验收。

本页对应2026-11-20的最终组合：9阶段、66项原业务问题、164次完整文件操作、138条生产路径、累计15份Java后态及1份Android清单。当前原始回执集中在仓库 [`construction/verification/STARCORE_R33_5_20261120/third-review/`](https://github.com/gtt46461-star/starcore-public-build/blob/main/construction/verification/STARCORE_R33_5_20261120/third-review/README.md)，状态汇总在上级 [HANDOFF.json](../HANDOFF.json)。旧53套宿主和2026-11-19回执保留原始范围，不能替代本轮组合源码的检查。

## 1. 原件和23项复核命令

```bash
python3 -B "$REVIEW/verify_review.py"
python3 -B "$REVIEW/check_plan.py" "$PLAN"
```

PLAN为prepare.py还原的认证原卷，REVIEW为同级current-review。check_plan运行23条实际命令，覆盖封包、两份033原报告、版本/累计文件、B04数值、严格结算窗口、交付预检、导出及终审拒签。它不产生APK或自然资格。B04数值回归的17763项是数值检查点计数；报告链另有下面的396项断言。

## 2. 与安装器相同清单的十个全源码前缀

```bash
python3 -B "$REVIEW/verification/verify_reviewed_prefix.py" \
  --plan-root "$PLAN" --baseline-source "$SOURCE_033" --work-dir "$PREFIX_WORK" \
  --android-jar "$ANDROID_JAR" --annotation-jar "$ANNOTATION_JAR" \
  --jdk-mode jdk17 --java "$JAVA_HOME/bin/java"
```

SOURCE_033是正式提交`bb7b49bbcc9266bad548ec37e71403256fc06a4d`的完整源码，PREFIX_WORK是工程外新目录。工具验证基线全部源码，组合原完整清单、原B05配对修订和当前`production/OVERLAY.json`，调用原安装清单验证器。它从BASELINE033到P00、B01—B08逐次执行7个原工程生成器和全Java类型编译，不调整原生成器校验。最终源应包含1360个Java文件；另有2个明确的资源/BuildConfig类型夹具。

`prefix_final/EFFECTIVE_APPLY_MANIFEST.json`记录实际完整有效清单，`REVIEW_BINDING.json`记录开始时修订身份及productionReview；`PREFIX_INPUT_MANIFEST_R335.json`保留原认证清单，供原宿主核原卷字节。最终规则SHA由原R967从实际源码验证，见`production/SEMANTIC_RESULTS.json`。Java编译结果不等同于Gradle、DEX、native、签名或Android设备执行。

## 3. 115项原custody检查绑定完整当前前缀

```bash
export R335_CUSTODY_PREFIX_WORKDIR="$PREFIX_WORK"
export R335_ANDROID_JAR="$ANDROID_JAR"
export R335_ANNOTATIONS_JAR="$ANNOTATION_JAR"
python3 -B "$REVIEW/verification/check_reviewed_custody.py" \
  --plan "$PLAN" --out "$NEW_CUSTODY_OUTPUT"
```

当前包装器保留原115项Java检查、全部原卷载荷、十份编译回执、依赖、生成器日志和每个已编译Java输入的校验。最终候选侧核同一有效清单的全部138路径，包含本轮新增类；同时核已保存的有效清单和productionReview。回执同时保留前缀开跑时的seal和实际检查时的seal，文档后续编辑不能掩盖生产字节或overlay不符。

## 4. 报告闭环396断言与两份真实原报告

```bash
python3 -B "$REVIEW/reports/closure/run_report_closure.py" \
  --source "$PREFIX_WORK/prefix_candidate" --plan "$PLAN" \
  --android-jar "$ANDROID_JAR" --annotation-jar "$ANNOTATION_JAR" \
  --java "$JAVA_HOME/bin/java" --out "$NEW_REPORT_OUTPUT"
```

此命令核7份生产全文、8条需求注册表及其源码契约，从完整最终源重新编译，再读原包中两份固定SHA的033报告运行Java断言。输出包含同SELECT原行关联、owner与时钟/源码身份拒绝、声明存在/缺口/缺证分类和Markdown输出。共同owner只能提供有关联的运行观察，不能证明对象专属根因；旧033报告不能获得新源码的执行或效果结论。Android实际SQL/设备、最终报告长期采集和自然质量另行验收。

## 5. 真实Git逐批源码回放和终态缺件负例

正式施工按`replay/stage_checked.py`逐批进行，再用下列入口复验完成源：

```bash
python3 -B "$REVIEW/replay/verify_completed_source.py" \
  --plan "$PLAN" --repo "$COMMITTED_B08_SOURCE" --review "$REVIEW"
```

本轮独立回放在真实033 Git对象上生成九次版本提交，逐阶段核R967、当前字节和干净工作树。完成源再次核每个实际引入版本的提交及累计生产文件。隔离副本删除本轮新增`CapabilityNeedRegistryR335.java`并提交后，终验必须拒绝；正例源保持不动。保存的回放使用可见033及九阶段提交的浅历史，因此只证明源码安装/继承闭合，完整Git bundle仍需正式交付前检验。

## 6. APK、现场和自然效果签收

执行`source/preflight_delivery.py`核原033 APK、完整工具链和Git历史；构建及交付依原方案并逐批产生真实APK、源码、可恢复bundle及摘要。B08以`replay/final_review.py`收同一候选的现场原件。32组/96断言/288场景、1832需求/222条款、V2353A目标机24/72小时和独立自然资格分别登记，不用宿主结果补填。

## 历史回执

本目录既有`host-receipts.zip`属于还原后原算法的53套宿主；`prefix-results.json`、`reviewed-prefix-receipts.zip`、`base-custody-recheck.log`、`reviewed-custody.log`属于2026-11-19两文件修订。文件原样保留，当前组合证据统一看2026-11-20目录与HANDOFF。历史命令和旧R82值仅用于追溯。
