# R343-002 / B01：补齐原 R967 / R82 语义身份的施工步骤

## 确认的遗漏与原因

173 的原 `scripts/r967_semantic_identity.py::controlled_paths/build` 对 `app/src/main/{java,assets,res,cpp}` 及固定构建输入执行**精确闭集**。本次方案在 B02—B10 加入 12 个生产 Java 叶子，却原先没有更新 `release/research_dependencies_r967.json` 和当前 `release/r82_training_invalidation.json` 的生成工具或施工步骤。原 `prepare_field_batch.py` 只负责原四文件版本事务。原 Gradle 的 `generateR967Semantics` 必须在 Java 编译前通过，因此这不是可以等到真机再排查的问题：只应用原 B02 补丁，原验证器会报 `R967_UNCLASSIFIED_OR_MISSING_PATHS`。

即使人工只加 12 个 manifest 条目，后续还会遇到动态依赖精确字节、原 R82 当前来源摘要、逐路径差异审阅、角色集摘要不一致。必须在原门内生成完整的一对当前输入；不能删门、让通配规则自动接纳新增文件，也不能把现有 NUMERIC 文件降为 PRESENTATION 来逃避失效。

本补足作为 `semantic_support` 组加入 B01：`field_binding -> delivery -> semantic_support -> runtime`，后续依赖链不变。它增加两个 `tools/r343/*.py` 文件，不增加生产 Java，不修改原 R967 验证器、不修改策略、统计门或 REAL 权限。两工具自身不属于原 R967 的固定生产闭集；它们由本次封印计划的 PATCHSET 精确路径、前后哈希和本地/外部双份计划验证绑定，不能把它们加入生产闭集来形成自指输入。

## 输入与写入边界

`rebind_semantic_inputs.py` 从 Git 固定对象 `4d1de9d83692635b5c5ce39144fece1c8a31c8ac` 读取 173 原始字节，禁用 Git replace 和 GIT_DIR 等环境重定向。它只接受本次已绑定计划所声明的累计补丁精确状态；不接受“文件名相同即可”、未知生产叶子、后批提前出现、既有角色更改或源码的额外空格改动。

对 31 个既有修改叶子，`REVIEWED_EXISTING` 逐路径保留原角色并说明修改的语义意义；对 12 个新增叶子，`NEW_ROLES` 逐路径明确 NUMERIC/PRESENTATION 及理由。完整前后 SHA、批次和角色见 `ROLE_REVIEW.json`。原 1589 个角色逐项保持，最终闭集为 1601 个文件。

唯二可写输出：

1. `release/research_dependencies_r967.json`：当前精确角色集合及需要的动态依赖精确字节审阅。
2. `release/r82_training_invalidation.json`：当前来源摘要、原 R81 到当前源码的精确差异及新增角色审阅；保留 `INVALIDATE_OLD_RULE`、空 `predecessors` 和无资格迁移的原 scope。

不修改原 `release/strategy_su1_reviewed_inputs.json`、两份不可变 R81 基线、原策略文件、此前 R33/R34/R34.1/R34.2 的封印与绑定。旧 SU1 reviewed_inputs 在精确 173 上已有 190 个受控路径与历史 pin 不同，是历史施工工具输入，不能冒充本次可更新清单；本流程不调用旧 `prepare_strategy_su1_identity.py`、`build_r13.sh` 或 `opt299_stage_build.py` 来“修复”历史文件。

原 R82 验证器仍独立校验所有原字段、不可变基线摘要、角色集合和逐文件差异。新工具先在外部临时树上用实际源码和候选两文件执行**未修改的原 `build(root=...)`**，成功后才能落盘。

## 每批必须执行的顺序

以下变量应由施工方指向既定的实际路径，不能指向旧计划。`STARCORE_EXTERNAL_EVIDENCE` 必须在源码仓库和计划之外；原四文件版本 journal 与语义 journal 使用不同的新目录。`Bxx` 替换为当前一个批次，不跨批跳过验收。

1. 从已公开固定计划 commit 恢复完整计划到源码内既定 `construction/plans/STARCORE_R34_3_REPAIR_20261126`，核对外部计划同一封印；按 PATCHSET 顺序只应用截至当前批次的组，执行原修改前 SHA 检查。
2. 用本方案 `tools/r343/prepare_field_batch.py` 和原四文件原子事务生成当批版本。必须保留精确原 generator，版本号高于实际已装/已发布最高值；此工具已经负责原安装 173 下界，不能人工只改一个版本字段。
3. 对当前累计源码执行只读预检：

```bash
python3 -B tools/r343/rebind_semantic_inputs.py \
  --repo "$STARCORE_SOURCE_ROOT" \
  --plan "$STARCORE_FIELD173_PLAN_ROOT" \
  --batch Bxx
```

4. 预检通过后，应用唯一两文件语义事务：

```bash
python3 -B tools/r343/rebind_semantic_inputs.py \
  --repo "$STARCORE_SOURCE_ROOT" \
  --plan "$STARCORE_FIELD173_PLAN_ROOT" \
  --batch Bxx \
  --apply \
  --journal "$STARCORE_EXTERNAL_EVIDENCE/Bxx-semantic-journal"
```

5. 再执行只读结果核对和原语义变异回归：

```bash
python3 -B tools/r343/rebind_semantic_inputs.py \
  --repo "$STARCORE_SOURCE_ROOT" \
  --plan "$STARCORE_FIELD173_PLAN_ROOT" \
  --batch Bxx --check
python3 -B scripts/r967_semantic_identity.py --self-test
```

6. 将当批源码、四文件版本、一对当前语义输入、原计划绑定及完整封印计划一起提交到当批独立 source commit。再走原 `tools/r33/run_release_build.py` / 原 CI 全部 release/debug/androidTest 构建、当前批 host 及 Android 验收流程。**先完整提交再构建**，以满足原 clean-source、源码树/计划/APK/CI run 绑定。Gradle 继续由原任务生成 `app/build/generated/...` 的语义 Java/manifest，并独立 `--check` 和末端核验；不能提交手造生成值替代原任务。当前编译后的 R982 `--from-classes` 链继续运行，不重新冻结历史 call-index 档案。

每一批都做步骤 3—5。B01 仅工具变更应保留 173 原语义 rule；没有语义源码变化的 B04/B08/B09/B11/B12 以及同批只增加 versionCode 的重建也应保持前一语义 rule。这是原版本元数据与数值规则的既有边界，不是跳过执行。含实际受控源码变更的批次会得到新的精确 rule，旧质量证书不自动迁移。

## 中断、并发、权限与人工修改

两文件事务复用原 `advance_release_version.repo_version_lock` 的同一 Git 管理目录锁，与原四文件版本事务互斥。没有二套版本锁或生产写入 Owner。默认预检和 `--check` 不写源码。

应用前保存外部 journal 的完整 before/after 字节及摘要，写入持久 JOURNAL 后才开始替换。每个替换使用临时文件、fsync、原子 rename 和目录 fsync，保留已有源文件权限。新 journal 目录为 0700，新日志/备份默认 0600，不把历史只读封印改成可写。

如果进程中断，用**原同一个** journal 恢复：

```bash
python3 -B tools/r343/rebind_semantic_inputs.py \
  --repo "$STARCORE_SOURCE_ROOT" \
  --plan "$STARCORE_FIELD173_PLAN_ROOT" \
  --batch Bxx --resume \
  --journal "$STARCORE_EXTERNAL_EVIDENCE/Bxx-semantic-journal"
```

恢复必须匹配同一批、同一版本、同一计划和同一规则；journal 的整对 before 必须是 173 或本轮已审阅累计前态；每个当前输出只能等于其 before 或 after。合法混合态可以完成，用户额外编辑、伪造 before、篡改 journal、错误 seal、源代码漂移均拒绝。所有成员在任何替换前核查。中断时不会产生 APPLIED 成功声明；只有两文件完整写入才记录身份事务结果，此结果仍不是构建/设备/策略质量 PASS。

## 配套主机验证命令

两个测试均在外部临时目录复制固定基线及已审阅 payload，不修改用户源码、不推送测试 commit。输出路径必须尚未存在。

```bash
python3 -B tools/r343/test_rebind_semantic_inputs.py \
  --source-repo "$STARCORE_SOURCE_ROOT" \
  --plan "$STARCORE_FIELD173_PLAN_ROOT" \
  --out "$STARCORE_EXTERNAL_EVIDENCE/semantic-host.json"
python3 -B tools/r343/test_rebind_semantic_inputs.py \
  --source-repo "$STARCORE_SOURCE_ROOT" \
  --plan "$STARCORE_FIELD173_PLAN_ROOT" \
  --cli-only \
  --out "$STARCORE_EXTERNAL_EVIDENCE/semantic-cli.json"
```

累计主机测试：70 断言、12 批，包含精确 173 rule、原门缺失叶子错误复现、每批用原 `build()` 真正验证、12 新叶子/1589 旧角色、不可变 R81/SU1/策略保留、纯版本稳定、未知叶子/字节/提前批次/角色/前驱资格/重复 JSON/符号链接拒绝、共享锁、半写恢复、用户修改保留和 journal 篡改拒绝。交叉审查已在此前封印计划上独立重复 70 断言通过。

本组另用当前私有 reviewed fixture 的 11 条断言验证 main CLI 的真实 plan pin、Git commit 读取、双份封印、只读预检、apply/check/resume、错误 seal 和本地/外部成员漂移。fixture commit 仅存在临时仓库，不是公开交付 commit，不能作为现场 APK 证据。

最终公开计划必须由总装方重新执行这两个命令，保存最终封印所对应的新结果。本文及主机 receipt 不声称 Android release 已构建、真机 24/72 小时已通过、自然数据质量已达标或已有 REAL 出手。
