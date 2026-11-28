# 修订2：施工可执行性补漏与闭合记录

2026-11-28（Asia/Shanghai）。本次接续用户“继续，查缺补漏完善方案”的要求，对已发布 `7947177eb26a5508e8c34c1159cb179ccbe15a89` 做第二轮定向复核。**确认并修订14项资料工具或施工合同问题，补充1项B00边界说明；这些是方案/工具的闭合，不是应用返修或自然效果已经通过。**

本修订保留46张卡、50个原发现和12批依赖，补齐原有owner的职责与验收。应用源码、原始诊断、原完整交付ZIP、五候选补丁、1407文件基线、旧公开计划和历史审计原件保持原内容。

## 一、14项确定问题的具体处置

|编号|已确认问题及后果|本次修订|取证/验收|
|---|---|---|---|
|T01|候选检查的`--output`可覆盖传入源码，却输出`application_source_changed=false`。|在任何检查前验证输出位置；禁止写入source/冻结plan，已有文件与symlink拒绝覆盖；新结果原子创建。|隔离源码夹具先复现旧覆盖；新工具拒绝且原字节不变。|
|T02|候选哈希门使用`assert`，Python优化模式会跳过。|改为不可被`-O`关闭的显式校验，强制五候选及声明后态范围；Git子进程隔离继承的工作树环境。|正常与优化模式都拒绝错误patch hash；五真实候选仍累计应用通过。|
|T03|报告解包的哈希/长度门也可被优化模式跳过。|显式验证完整原报告、manifest/56载荷、路径与成员；只向新的外部目录写，逐文件排他创建。|错误报告摘要在两种模式均拒绝；真实原件在优化模式解包并逐载荷读回一致。|
|T04|主验证器将MANIFEST与PATCHSET视为可选，缺失仍可能返回总PASS。|完整分发必须有MANIFEST、SHA封印、五候选目录；缺失/空候选/错摘要均失败。|缺两份必需文件、空候选及错封印实际反例全部拒绝。|
|T05|主验证器输出黑名单不完整，可在验证后覆写README等冻结输入。|所有`--output`都使用同一新文件/保护目录规则，不再允许覆写冻结计划中的任意文件。|README/source/已有回执/symlink的保护实测通过。|
|SR01|旧Bash前置SHA失败仍继续解包，最后exit0。|合并成一个`set -euo pipefail`执行单元，顺序依赖失败立即停止。|准确旧代码顺序已在临时Git夹具复现；新前置脚本做故障注入验证。|
|SR02|把当前HEAD赋成PLAN_COMMIT、或只检查git status，不能证明使用指定计划字节。|外部传入完整expected commit并核HEAD；执行任何计划Python前直接从固定Git对象核plan/package所有跟踪blob，保留本地编辑而拒绝继续。|覆盖错误pin及`assume-unchanged`隐藏字节变化；不靠reset/清文件求通过。|
|SR03|稀疏计划checkout的新worktree可能仍无app/src，即使Git状态干净。|新211基线与施工工作树显式关闭稀疏检出，再核完整源树与精确基线。|实际Git夹具中复现缺源，disable后源出现且工作树干净。|
|SR04|B01导出10场景提前要求B02才实现的fault排空，造成验收循环。|场景不删，子断言按实现归属：B01保全读取、取消、正常导出及既有清理；B02关闭新增快照/总盘/配额/真实drain。B01相关子断言记PENDING_B02交接。|CODEX、B00附约、导出合同、B01/B02 JSON及卡片同步；原DAG不改，B02须用自己的签名APK实证。|
|SR05|“取消、低空间均可分享”的出口要求违反原CANCELLED/PAUSED/READY状态机。|取消未发布作业真实退出并终态CANCELLED；新合法请求重试；空间不足诚实PAUSED/失败；已有READY引用保护不退化。|精确211源码取证并同步卡005及B01出口；禁止发布半成品或复活终态。|
|S2-ST01|两份旧合同仍把首次冻结串在rolling之后，与新指令矛盾。|统一为首次FREEZE_CANDIDATE与后续rolling两条已有分支；开发回执后按各自真实条件推进。|两合同精确替换及R034处理顺序经独立合入复核。|
|S2-ST02|首次冻结只验入队；R043/044缺现代AST3校准、正式窗口、签发及消费者的实际接线验收。|R031验R958/R967实际冻结；R043/044补既有R932预约/raw CAL241/immutable quality/正式窗口及R929签发、唯一消费者。R27与现代门槛、分母分别保留。|补11个已存在的211 owner源码锚；16卡字段、3批字段、3合同段、相关指标证明责任同步。工程和自然结果分开。|
|S2-ST03|恢复正例误要求重启继续同lease，可能诱导删除现有租约保护。|新进程合法claim后使用新owner/递增task generation续同任务；旧lease单列拒绝反例，不混淆checkpoint generation。|卡034、ACCEPTANCE及指令摘要同步，保留PROCESS/到期/CAS。|
|CV2-01|20矩阵统一归给MatrixResearch，漏COUNTERFACTUAL与SU2实际生产链。|卡038/B08/M25按18普通R930＋1配对COUNTERFACTUAL＋1SU2 checkpoint分路，补准确producer/consumer、输入门、原件和反例。|保留20原ID，新增5个既有owner锚；完整路由见[矩阵合同](coverage/MATRIX_20_EXECUTION_CONTRACTS.json)。|

B00身份边界另明确：APK/source/签名等可核交付身份必须真实；旧业务scope截断或SQL pin锚缺失如实导出，不能填造，也不能把B01才修复的字段提前设为B00全局前置。原外部报告catalog仍只使用原枚举，PENDING_B02仅为施工台账状态。

## 二、避免把有效代码误当缺陷

本轮曾核查“typed矩阵只写measure、报告读native，因此未来索引必为0”的候选假设。继续读完整源码后确认 `MatrixTensorR30R1.append` 已在同一Batch写入analysis、measure和native原件，原报告可以读取该桥接。该假设已撤回并记录反证，**不据此重建表、重写投影或补造数据**。

新增16个源锚是把211已经存在的owner补进施工责任，当前锚数为152。不是新增16个产品类，也不是要求修改清单内全部文件。原136个源字节身份及1407生产Java基线保留。

## 三、这次实际跑了什么

|验证|真实范围|结果位置|
|---|---|---|
|旧工具缺口复现|一次性plan/source夹具，无用户应用改动；确认5个旧工具问题|[工具复现记录](audit/second_tool_gap_reproduction.json)|
|新工具回归19项|实际子进程、真实文件保护、正常/优化模式反例、必需清单、真实报告56载荷读回；所有输入保留|[19项回归](validation/SECOND_PLAN_TOOL_REGRESSION.json)|
|真实五候选再检查|从精确211取得并核验6个前态文件；临时累计应用5patch，核7个声明后态，输入不变|[候选再检查](validation/SECOND_CANDIDATE_CUMULATIVE.json)|
|计划启动正反例|当前CODEX前置原文；固定Git夹具中核pin、隐藏工作字节变动、失败即停与原27片物化；不执行真实应用checkout/构建阶段|[启动回归](validation/SECOND_BOOTSTRAP_REGRESSION.json)|
|合入忠实性|策略13组、覆盖16组；46卡当前条款与验收矩阵同步，原报告与ID集合保留|[策略合入](audit/second_strategy_merge_check.json)、[覆盖合入](audit/second_coverage_merge_check.json)|
|最终清单与报告完整性|新清单、152源锚目录关系、原报告、五候选描述和全部当前包字节；无完整源码目录时SOURCE明确NOT_RUN|[最终PLAN_CHECK](validation/PLAN_CHECK.json)|

工具回归记录绑定实际脚本SHA及测试时的临时清单；最后出版清单还会重新核验，避免把添加回执前的hash当最终hash。旧历史日志继续保持原范围，不把旧源核验冒写为这次重跑。

可复跑工具回归（输出路径必须全新且在计划/源码之外）：

```bash
python3 -B "$PLAN_DIR/tools/test_plan_tools.py" \
  --candidate-source-root "$BASELINE_DIR" \
  --output "$NEW_EXTERNAL_OUTPUT/plan-tool-regression.json"
```

新开工以[CODEX_EXECUTE.md](CODEX_EXECUTE.md)第3节完整脚本为准，需要本次实际交付commit作为外部 `EXPECTED_PLAN_COMMIT`。

## 四、严格保留的待验边界

本次没有编译Android、生成正式返修APK、安装用户设备、重放原5GB库或积累自然Future。原安装APK、特定.scdev和用户库缺件不能从诊断文字补造。五候选可应用、资料工具安全或方案检查通过，不等于产品功能、设备稳定性或自然优势通过。

下一施工执行者仍须逐卡逐批完成真实实现、同签名release导出、升级保全、现场和自然证据。已完成的方案修订不会把任何产品待验状态改成PASS，也不允许因未达自然质量而停止独立可执行工程。
