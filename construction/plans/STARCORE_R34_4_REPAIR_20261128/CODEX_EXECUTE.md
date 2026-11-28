> 公共仓库版。原诊断与逐条证据须从用户完整交付包读取，不能公开提交这些原件。公开版4候选与完整包5候选分开验证。

# 可直接交付施工方的完整执行指令

请完整实施 STARCORE R34.4-211 返修方案。目标是把已有策略创造、数值训练、精确结算、反馈研究、变异融合、冻结发布和独立未来接成真实闭环，同时保留原有有效逻辑、文件、数据、报告导出和唯一真实主链。按本包逐卡逐批施工，不以有文档、有heartbeat、有任务计数代替功能完成。

## 先锁定底座与读取方案

1. 仓库为 `gtt46461-star/starcore-public-build`，本计划路径为 `construction/plans/STARCORE_R34_4_REPAIR_20261128`。main只存本轮计划，应用底座必须是 `5c7f1b36eaee6036df280e85835bb0fb4ce0b40d` / versionCode `577311211`，或已逐文件核验的合法后代。
2. 读取 README、CONSTRUCTION_PLAN、ISSUES及46张卡、12个batch、三份contracts、ACCEPTANCE_MATRIX、PATCHSET、SELF_CHECK、SOURCE_ANCHORS、coverage和inheritance索引。旧173数学说明仅作历史合同，当前实现以211源码和本轮合同为准。
3. 从211建立隔离工作树；先运行本包只读验证器、候选累计检查和原报告验证。保留干净基线，不在main旧应用目录上试修，不重套173整文件后态。

可用命令如下，`PLAN_DIR`为新计划目录的绝对路径，`APP_DIR`为211工作树绝对路径：

```bash
python3 "$PLAN_DIR/tools/verify_plan.py" --source-root "$APP_DIR"
python3 "$PLAN_DIR/tools/check_candidates.py" --source-root "$APP_DIR"
```

`check_candidates.py`只在临时副本应用4组公开候选，不修改APP_DIR。实际施工按所属批次审阅后用原git小补丁流程应用；这些候选只覆盖4个局部缺陷，不代表已实现全部46项。C04含用户内部精确任务身份，仅用户完整交付包提供；拿不到该身份时不得泛化白名单或猜测。

## 必须执行的路径

按 B00–B11 的输入门与工程交接执行。先保全身份和报告，再修存储/fault drain/公平writer，保障下一期赛前窗口并逐条处置163个LIVE_RETRY，随后打通各研究路径自己的输入与真实TRAIN。

不要把所有旧root恢复作为新训练前置。旧pin缺失只隔离相应R27实验；旧state缺失时保留ColdRebirth拒绝，完全独立实验通过现有ModernFamilyFactory→Director开工。必须做REAL=0、证书=0、全部旧期NO_PREDRAW但历史合法的开发训练正例；rolling没有COMPLETED时维持0，不造事件。

现代TRAIN应实跑至少一个FIXED_GRAPH和一个学习内核，并继续覆盖每个已启用内核的真实数值状态与中断恢复。保留CP9训练/验证切分、标准化、embargo和校验。第五个RULE恢复只增加精确五元组资格，所有旧原件验证继续；恢复失败不能改旧task成功。

持续创造沿既有MutationGraph/Director、typed rule和失败研究action链，保留父代、冻结教师和谱系。参数更新、结构变异、交叉、融合、机制扩展分别计量。复制父代、同构NO_OP、改名字和清variant游标不算创造。新结构需实际编译/TRAIN/开发产物，优劣按证据评价，允许失败与淘汰。

18研究图、20注册矩阵、29旧图和16逻辑表按各自对象验；82能力“可调用”不算已生产。BORN/ACTION原hash和帧COMMITTED检查保留。R06沿独立生产者；R11/R14/R18正式读层不可回流训练。AtomNet、校准、自然父子claim必须由各自真实生产owner出原件，不能拿rolling或表行数顶替。

## 不可违反的保全条件

- 原应用所有有效文件、唯一REAL/结算owner、单位置K3、PIT截止、SHADOW/REAL分账、冻结版本和既有资格门保留。
- 研究等待仍走原协调器，不全局忽略实时、容量、fence、取消或原件背压。未知提交先回查回执，不能重复数值学习。
- 不删库、不清失败队列、不修改原FAILED_TERMINAL、不补造过去预测、不把0/空/null/UNKNOWN相互替换。
- 检测报告保持原96检查/44指标/附件/索引/单ZIP与分卷/取消/重试/SAF分享。检查失败仍可导出；不增加全PASS门。内部新状态不得破坏外部catalog枚举。
- 快照回收区分作业、profile、pin与唯一原件；只释放已有合法替代的工作副本。物理磁盘耗尽时诚实暂停，不虚构实时保证。
- 保留包名、签名、历史封印与原field173工具；新增本轮绑定而非改写历史封印。新版本号高于实际已发布和安装最高值，不在方案里预占固定code。

## 每批都要交付并继续推进

对本批所有卡实际实现、验正例与反例，记录前后文件hash、真实输入输出、source/APK/plan/时间和验收层级。沿211现存 `.github/workflows/build-apk.yml`、`tools/r343/prepare_field_batch.py`、`tools/r343/rebind_semantic_inputs.py`、`tools/r343/field_delivery_contract.py` 及原版本事务工具核当前用法再构建；已有工具名的173标识不授权回退源版本。适配本轮绑定必须增量兼容，不能跳过原交付门。

每批应构建、签名验证、保留数据覆盖安装并核同APK的报告导出；当前R967/R82语义身份随该批更新，不拖到最后。来源原件不具备时记录阻断的具体卡和路径，继续可独立完成的工程，不把未执行写通过。自然未来未积累到门槛时交付可持续运行的工程结果，资格仍NOT_PROVEN，不降低门槛求出手。

最终回填46项验收、96检查、44指标、165对象、1832需求与299卡的实际证据；未经实际验收的旧需求保留原状态。提交每批源码、APK、验证原件、差异和回退方案；最终说明未通过项及真实原因。完成定义是实际生产和消费可追溯、导出可靠、原功能不退化，不是把报告改成全绿。
