# 给施工方的完整执行提示词 · STARCORE R34.3-173

你负责实施，不是再输出一份泛泛建议。使用本计划目录的封印、44项问题、12批施工合同、完整补丁/after、验收矩阵与原始检测报告，修复173现场版本并交付可安装、可追溯的实际成果。所有可执行工程工作持续推进；无法满足的设备或自然未来门必须有原件说明，不能假报完成。

## 一、固定事实与开工基线

仓库：`gtt46461-star/starcore-public-build`。

应用基线：`4d1de9d83692635b5c5ce39144fece1c8a31c8ac`，已安装`577311173`，APK SHA256 `9304472262455c12b4600708fb23480d698acf9f3ba710ddee86cda214ae8750`。

计划目录：`construction/plans/STARCORE_R34_3_REPAIR_20261126`。

main维护方案，不是应用173源码底座。先检查当前工作树和用户未提交修改，使用已包含173的合法施工分支或新隔离工作树；禁止reset、覆盖用户改动或将旧152/122整文件后态再套入。新施工分支使用`construction/r343-repair-20261126`，对应CI授权ref已在交付适配中明确注册。

计划名R34.3-173与APK系列分开。继续使用`R34.2-B01..B12-YYYY-MM-DD-versionCode`及原包名、签名；versionCode从已安装、远端发布索引和本地manifest最高值递增，不写死174，不把旧APK改名。

## 二、先核验完整材料

阅读README、CONSTRUCTION_PLAN、ISSUES、BATCH_INDEX、PATCHSET、FILE_CARDS、ACCEPTANCE_MATRIX、SELF_CHECK和specs。旧57问题、62工单、152验收、295卡，以及当前1832需求/299卡全部保留原断言，不能以本轮44问题代替全部软件需求。

原报告`report/ORIGINAL_DIAGNOSTIC_173.zip` SHA256必须为`3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86`。本包只是DIAGNOSTIC，原SQLite/全部模型/APK字节不在包内；需要真机回放的场景先取得经用户授权的完整原件及可恢复备份，缺原件不能伪造。

将已经发布的本轮计划完整导入应用树，同时保留一份计划外只读副本。计划commit从实际更新MANIFEST_SHA256的仓库提交解析并锁定，不从示例猜SHA。先运行：

```bash
python3 -B "$PLAN_ROOT/tools/verify_plan.py" --plan "$PLAN_ROOT" --source-repo "$APP_REPO" --out "$EVIDENCE_ROOT/plan-check.json"
python3 -B "$PLAN_ROOT/tools/apply_patchset.py" --plan "$PLAN_ROOT" --repo "$APP_REPO" --group field_binding
python3 -B "$PLAN_ROOT/tools/apply_patchset.py" --plan "$PLAN_ROOT" --repo "$APP_REPO" --group field_binding --apply
python3 -B "$PLAN_ROOT/tools/apply_patchset.py" --plan "$PLAN_ROOT" --repo "$APP_REPO" --group delivery --apply
python3 -B "$PLAN_ROOT/tools/apply_patchset.py" --plan "$PLAN_ROOT" --repo "$APP_REPO" --group semantic_support --apply
```

`PLAN_ROOT`、`APP_REPO`、`EVIDENCE_ROOT`是本机实际绝对目录，证据输出必须在封印计划和应用仓库之外。以上是B01的三组；后续每批只应用截至当前批次的组，不提前混入后批。按脚本help输入本轮真实plan commit和已知最高versionCode，生成补充field173 pin并推进批次。旧R33/R34/R341/R342绑定字节不动。第一次新批构建前必须同时导入delivery与semantic_support，不能等B12。

版本准备使用以下完整参数；`PLAN_COMMIT`必须为该封印实际所在提交，`BATCH`为当前B01…B12，`INSTALLED_VERSION`和`HIGHEST_KNOWN_VERSION_CODE`来自当前真实安装与发布观察，不是示例常量：

```bash
python3 -B "$APP_REPO/tools/r343/prepare_field_batch.py" \
  --repo "$APP_REPO" --plan "$PLAN_ROOT" --plan-commit "$PLAN_COMMIT" \
  --batch "$BATCH" --installed-version "$INSTALLED_VERSION" \
  --highest-known-version-code "$HIGHEST_KNOWN_VERSION_CODE" \
  --journal "$EVIDENCE_ROOT/version-$BATCH"
```

每批首次使用全新外部journal；只在恢复同一次中断版本事务时复用同一journal并添加`--resume`。脚本同时推进manifest/properties/ReleaseManifestR926/host BuildConfig四文件，检查全部前态后沿原可恢复事务写入；禁止自己再改其中一个版本文件。

**每一批在当批补丁和版本准备之后、提交及构建之前，执行当前语义身份重绑定。** 原R967是封闭集合；新增生产文件必须显式登记角色，不能删除原门或用旧SU1工具覆盖历史证明。完整两文件事务代码和逐路径角色审阅见`patches/semantic_support/REVIEW_AND_TESTS.md`与`ROLE_REVIEW.json`。

```bash
python3 -B "$APP_REPO/tools/r343/rebind_semantic_inputs.py" \
  --repo "$APP_REPO" --plan "$PLAN_ROOT" --batch "$BATCH"
python3 -B "$APP_REPO/tools/r343/rebind_semantic_inputs.py" \
  --repo "$APP_REPO" --plan "$PLAN_ROOT" --batch "$BATCH" \
  --apply --journal "$EVIDENCE_ROOT/semantic-$BATCH"
python3 -B "$APP_REPO/tools/r343/rebind_semantic_inputs.py" \
  --repo "$APP_REPO" --plan "$PLAN_ROOT" --batch "$BATCH" --check
```

该工具只写`release/research_dependencies_r967.json`和`release/r82_training_invalidation.json`，保留原1589角色及旧证明，不继承旧质量资格。语义journal也必须每批新建，中断恢复才以原journal和`--resume`重试；不得与四文件版本journal共用目录。随后在应用仓库运行原`scripts/r967_semantic_identity.py --self-test`，提交当批源码、版本、两份当前identity、field173 pin及原封印计划，再走原clean-source构建/CI。纯版本或工具批的训练rule必须保持稳定。

## 三、按依赖完成B01—B12

1. B01：身份、原件、精确前态、field pin、构建/CI/封包/173覆盖安装适配，以及原R967/R82封闭依赖身份的精确重绑定工具。
2. B02：四个合法TEMP合同、main限定查询、有界启动恢复探测、正确资源异常类别、故障排空阶段/主writer真实时钟；由原Owner完成三表迁移。
3. B03：核心保存的物理预算和分段计时、精确结算coverage读模型、当前赛前覆盖、100/1200延后原件完成；不开奖后重算。
4. B04：合法连续历史/缺口、原件闭包、受控保留/存储、真实抓取时隙与设备后台；保留已经实现的后缀、pin和分页扫描。
5. B05：typed训练异常分类、四个误终止任务的不可变继任恢复、分族局部已验证种子、真实历史开发训练。
6. B06：独立rolling版本、每结算事件、原PIT输入/数值训练/检查点/头CAS、独立冻结publisher、旧冻结窗继续评价、输入引用保留；事务内正常等待必须经原队列yield，持续有任务时也给有界回收机会，分页游标跨越前16个pin，全部调用点一起合入。
7. B07：真实typed结构创建、参数/结构/交叉/融合分类、失败研究、规则归纳/跨父代迁移、Director唯一child+TRAIN提交。
8. B08：真实教师、bound/long原件、20typed矩阵、R952矩阵流、多horizon与同目标融合各自闭环。
9. B09：四证书域校准和独立未来质量门、确切发布、唯一主链当期自然消费和结算。
10. B10：47图/16表逐项生产/查询/过滤/ABI/预算/渲染与BORN/nonBORN消费原件。
11. B11：96检测、44指标、165对象、旧/新施工账本逐项核销；不足状态真实保留。
12. B12：当前源码实际APK、覆盖安装、所有原件/源码/包的下载回读、24/72h及自然质量的分层最终交付。

按`PATCHSET.json`顺序逐组应用，默认检查，显式`--apply`修改；每个before必须是173或前组精确after。合法后代若已有不同修改，先做三方差异和适配，并重新记录source_after/hash/回归；禁止强行覆盖、补丁reject后凭猜拼文件。对已有正确实现按具体问题卡恢复和验证，不重写成另一套Owner。

## 四、不可省略的实现和回归规则

保留唯一分析、决策、结算写入者；保留下一期请求先行、上一期结算交接的现有顺序。网络、训练、报告和完整证据展开不在预测writer中等待。所有新增版本/队列/发布有明确键、状态、事务、lease/fence、PIT水位、原件hash、资源预算、重试/取消/恢复及不可变终态。

四个错误终结训练任务不能改回PENDING，必须精确原输入证明+继任回执。现代AST3不得塞回不支持它的legacy update。学习头与冻结头分离，旧窗不得被新训练作废。不同角色、source/lifetime、数据分母和时钟不能混合。

原40模板/17操作/typed grammar存在，不把重命名、参数微调或维护任务计为新机制；按数学清单真实运行公式，保留失败/NO_OP和消融。训练数据、开发验证、校准、未来、SHADOW和REAL分别统计。没有证书继续合法SHADOW，满足完整门后自动由原主链读取；不翻PROVEN、不删门、不转借证书。

不删库、不清空原件、不中断保护中的父代/冻结/输入引用、不伪造缺失字节、不为图表漂亮填假点、不把UNKNOWN改0、不降低K3/100/1200等既有合同。资源不足用有界片和可恢复等待，不以无限重试或全局等待代替。

## 五、执行真实测试和证据门

132个工程场景是44问题各自正例、反例、保留合同，不是已经运行的声明。把每项接到真实生产入口和已存在/本轮新增的实际runner，完成`release/field173_test_runners.json`的当前批次精确映射；runner执行原始日志、CASE_ENTER/CASE_ASSERTIONS_COMPLETE、非零断言数、输入输出原件和source/plan/APK/CI身份必须可复核。缺runner或真实原件时发布门应拒绝，不能把旧R342回归贴成新R343通过。

运行补丁附带主机/Java/SQLite测试及本批必要现有Android tests；完整Android编译、数据库迁移/重启/并发、原件回读和用户设备场景分别取证。仅语法parse、stub compile、helper单测、零用例、只编译androidTest或源码字符串扫描不能签生产接入。示例/合成/模拟器数据明确标记，不能进入自然质量分母。

每批输出工程receipt、实际命令/runner/日志与文件SHA、前后数据/行为差分、APK/hash/签名/source/plan/数据库/role和未完成项。不得编辑原计划封印或伪造PASS；发现计划/代码反例，先记录问题与原件并提交明确修正，不让施工方靠猜测绕过校验。

对报告无法确定的故障排空阶段、39秒未归因时延、UI实际空白和缺失原件，严格按问题卡阶段采集真实原件后定位。可执行工程工作继续做；只有确需用户设备/原始数据/未来样本的部分保留等待，不因一个依赖暂停所有家族或所有施工。

## 六、最终回传

逐项给44问题与全部继承账本的实际状态和证据链接，提供每批commit、最新实际APK、同源ZIP/bundle、原始/新检测报告、完整方案和精确diff、全部运行/安装/恢复/下载回读记录。发布后重新下载校验字节，确保安装身份和源码同一提交。

工程完成、设备连续运行、自然质量合格、真实主链贡献分别写结论。没有未来质量证据时不得写高胜率/每代提高/软件完美完成；必须保持持续研究和合法SHADOW，并让后续真实合格证据自然进入唯一主链。


## 本次续接复核的范围修订

本轮文档修订2将每个指标和检测项对应到实际负责生产的批次、源码与验收场景；B11只是汇总验收归口。predraw早于开奖覆盖与M05要求的至少5秒裕量分别测量。冻结快照没有独立单调时钟原件时保持UNKNOWN并保留历史原件，不把纯helper能力写成生产证明。详细续审记录见`CONTINUATION_REVIEW.md`。
