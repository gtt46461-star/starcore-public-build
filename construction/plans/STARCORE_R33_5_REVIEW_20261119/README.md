# STARCORE R33.5 当前施工方案 · 2026-11-20 再次复核

从[本轮自检与施工补正](REVIEW_SECOND_20261120.md)和[完整Codex任务书](CODEX_EXECUTE.md)开始。原业务范围为 **9阶段、66项问题**；当前有效安装范围为 **164次文件操作、138条不同生产路径**。V19—V25补正归入既有工单。累计审阅15份Java全文和1份Android清单，逐项归属与可提取的完整文件见[66项工单与当前代码](WORK_ORDERS.json)。逐项数量与哈希见[FINAL_SCOPE.json](FINAL_SCOPE.json)及[生产补正清单](production/OVERLAY.json)。

## 一次准备完整施工材料

在仓库当前修订上运行，Python要求3.11或更高。REVIEW指向当前仓库目录，WORK是工程外的新工作目录。已有同SHA的发布ZIP时可直接使用本地原件。

```bash
REVIEW="$(git rev-parse --show-toplevel)/construction/plans/STARCORE_R33_5_REVIEW_20261119"
WORK="$(mktemp -d)"
curl --fail --location --retry 3 -o "$WORK/33.5.zip" \
  https://github.com/gtt46461-star/starcore-public-build/releases/download/r33.5-plan/33.5.zip
python3 -B "$REVIEW/prepare.py" "$WORK/33.5.zip" "$WORK/prepared"
```

打开 `$WORK/prepared/START_HERE.md`。PLAN为 `STARCORE_R33_5_20261118/`，当前REVIEW为同级 `current-review/`；两者均在工程外。准备工具核外包SHA、6214件原始手机展开树、第一卷79项差量和最终6276件认证原件，再核封包并带出当前全部修订。原卷内较早的PART2_PENDING记录有其历史范围；历史Git的75分片路线不作为当前取件入口。

原发布附件为626717930字节，SHA256为`01af8e98ae4d0917b7dd3abe4bff2c44ef8e47cd80136df635cbb0441d6b3a79`。第一卷补62件、替换17件的原始恢复事实见[逐件差量](packaging/release-comparison.json)。本次生产补正通过当前仓库目录统一应用，取件后直接进入下面的逐批入口。

## 九阶段与各批责任

| 阶段 | 原问题数 | 当前文件操作 | 当批施工与签收责任 |
|---|---:|---:|---|
| P00 | 9 | 18 | 正式033来源、工程与方案绑定、版本四文件、构建桥；R967两项已跟踪输出正式入清单。完整交付输入用source/preflight_delivery.py核。 |
| B01 | 7 | 37 | 存储预算等待后恢复同作业、冷热原件、导出/取件；分卷截短立即拒绝，原件与其他作业保留。 |
| B02 | 10 | 40 | 单飞调度、启用机会、固定截止、逐期影子冻结、唯一结算与反馈；同时核生产标签、启用分母、固定截止和持久化返回观察。 |
| B03 | 2 | 9 | 家族训练、回放取消/断点、ABI隔离、真实父子出生事务和消费回执。 |
| B04 | 3 | 8 | 真实梯度、checkpoint、十位置发布消费；补正真实训练历史期去重计数及缺shuffle错误恢复。 |
| B05 | 25 | 37 | 有界分析、进化/变异/融合/淘汰、动作消费及父子配对；保留上次配对时序、缺冻/待结算分母修正。 |
| B06 | 1 | 0 | 当前B05生产身份继承，独立自然资格、证书/撤销与DecisionGate正反例；独立版本和APK/设备验收。 |
| B07 | 5 | 15 | 同SELECT故障原行→对象→诊断、8条有限需求与独立能力提案、表页/图表语义、真实修改→复报。 |
| B08 | 4 | 0 | 全九批累计源码、实际版本/原签APK/可恢复bundle、三次真实导出、同构建32组终审与目标机长测。 |

操作数按每批一次新增或替换计，跨批重复路径重复计操作。B06/B08业务安装差异为零，版本事务、测试、签名与现场签收各自完成。原142次/129路径为原封包口径；当前增加22次操作、9条不同路径，详见FINAL_SCOPE。原66问题全文仍在PLAN的 `control/ISSUE_REGISTER_R335.json`、`control/逐批问题与文件_R335.csv` 和各 `Bxx/issues/`；本轮补正对应关系见[自检记录](REVIEW_SECOND_20261120.md)和[报告补正工单](reports/closure/REPORT_GAP_WORK_ORDERS.json)。

## 唯一批次安装入口

从正式033提交 `bb7b49bbcc9266bad548ec37e71403256fc06a4d` 的真实Git工程开始。当前复核目录在main，应用源码基线取该确切提交；先记录方案提交再在独立源码检出施工。SOURCE指本批已提交输入，OUT须是外部全新目录。

```bash
python3 -B "$REVIEW/verify_review.py"
python3 -B "$REVIEW/replay/stage_checked.py" P00 \
  --plan "$PLAN" --repo "$SOURCE_033" --apply-to "$P00_OUT"
```

P00完成版本四文件事务、提交和当批验收后，同样调用B01，依序至B08。安装器自动组成全部旧后态、B05配对修订及 `production/OVERLAY.json`；每批核真实版本落点、累计生产字节、当前工作树与索引。新生成文件随本批提交，提交前仍执行原 `scripts/r967_semantic_identity.py --check`。新R82/R967由完整源码生成，原policy与原validator照常执行，旧资格兼容列表为空。

详见[批次与终审操作](replay/README.md)、[B01–B04运行校正](runtime/README.md)、[研究/自然资格](research/README.md)、[正式签名交付](source/README.md)。累计15个Java后态及Android清单在 `production/sources/`；正式安装用 `production/images/` 的逐批完整后态和清单，不手抄片段。

## 自检与证据

```bash
python3 -B "$REVIEW/check_plan.py" "$PLAN"
python3 -B "$REVIEW/verification/verify_reviewed_prefix.py" \
  --plan-root "$PLAN" --baseline-source "$SOURCE_033" --work-dir "$NEW_EMPTY_WORK" \
  --android-jar "$ANDROID_JAR" --annotation-jar "$ANNOTATION_JAR" \
  --jdk-mode jdk17 --java "$JAVA_HOME/bin/java"
```

第一条运行当前封包、双报告、执行链、B04数值、百期标签和交付负例。第二条使用与安装器相同的有效清单，在完整033源码上逐阶段运行7个原生成器与全Java类型检查。新的源码正向回放、报告396断言和目标机签收命令见[验证说明](verification/README.md)。[HANDOFF.json](HANDOFF.json)记录每一层实际状态。

两份设备报告始终作为施工前033事实。旧53套宿主记录、当前源码预演、正式APK、真机24/72小时、独立自然效果分别有自己的输入身份与结论。真实胜率、稳定性和可用资格由新候选的合法自然样本评估；当前自检结果不替代这些原件。
