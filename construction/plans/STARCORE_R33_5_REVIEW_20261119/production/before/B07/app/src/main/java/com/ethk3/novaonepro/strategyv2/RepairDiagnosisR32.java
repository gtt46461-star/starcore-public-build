package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import com.ethk3.novaonepro.ChartRegistryR31;

/** Projects the exported snapshot. No database writes, second snapshot or chart payload copies. */
public final class RepairDiagnosisR32 {
    private RepairDiagnosisR32() {}
    private static final String[] GROUPS = {"capabilities","matrices","researchCharts","legacyCharts","logicalTables"};
    private static final int MAX_GAP_DETAILS = 24;
    private static Set<String> expectedIds(String group) {
        Set<String> ids = new LinkedHashSet<>();
        switch (group) {
            case "capabilities":
                for (CapabilityRegistryR30.Definition d : CapabilityRegistryR30.all()) ids.add(d.id);
                break;
            case "matrices":
                for (MatrixRegistryR30.Definition d : MatrixRegistryR30.all()) ids.add(d.id);
                break;
            case "logicalTables":
                ids.addAll(ResearchTableAdaptersR30R1.ids());
                break;
            default:
                boolean research = group.equals("researchCharts");
                for (ChartRegistryR31.Entry chart : ChartRegistryR31.all()) {
                    if ((chart.group == ChartRegistryR31.Group.RESEARCH) == research) ids.add(chart.id);
                }
        }
        return ids;
    }
    private static void markGap(List<String> gaps, String key) {
        if (gaps.size() < MAX_GAP_DETAILS) gaps.add(key);
        else if (gaps.size() == MAX_GAP_DETAILS) gaps.add("FURTHER_COVERAGE_DETAILS_OMITTED");
    }
    private static String tableAction(String id) {
        switch (id) {
            case "T01": return "按 dataset_hash 和 role 连接训练运行水位，逐期拒绝晚到标签，报告第一处可见时间冲突。";
            case "T02": return "核查 r31_atom_score_visible 已提交的评分版本与 n_select，缺失率过高的原子暂停供给，回放同一输入。";
            case "T03": return "用 target_ordinal 对齐赛前 frozen_at、deadline_at 与开奖结算。按策略列缺失冻结和逾期，并保留只读未来角色。";
            case "T04": return "把失败窗口绑定到具体 parent_hash、输出和误差，再登记可复验的修复操作，追踪下一次出生。";
            case "T05": return "在同一 SELECT 原始期比较保留或移除原子的增益与下界，无配对支持的原子返回等样本。";
            case "T06": return "从同一父程序提供两个合法加法组件，分别消融A、B及组合，记录配对分母、协同差和成本。";
            case "T07": return "固定 dataset_hash 与 probe_set_hash，重演父子行为；同一签名合并来源，保留去重和释放槽的记录。";
            case "T08": return "把专家、提案和出生后的可执行 policy_hash 对齐，统计真正被接纳、拒绝和待处理的提案。";
            case "T09": return "按 role、kind、tier 读取 frame.points 和 unit，在同预算下比较算子净增益，再调整下一代份额。";
            case "T10": return "提供同目标同角色的两个已冻结父策略，分别与子代、移除一方和均匀组合比较，写独立子代证据。";
            case "T11": return "追踪权重版本到逐位置 model_surface、训练水位和真实预测的同一模型哈希；追查缺失的位置与发布事务。";
            case "T12": return "返回构成校准缓存的冻结概率和揭晓结果，按同机会重算分箱、ECE与支持数，错误缓存应失效。";
            case "T13": return "核对锁定块、策略哈希与 registration_experiment_id；未通过确认和登记失败分别计数，保留未来统计角色。";
            case "T14": return "从最老待办查进展、lane及阻因，比较重启前后已提交游标，修复有输入而永久得不到服务的任务。";
            case "T15": return "以 proposal_id 关联 consume.receipt_hash、child_policy_hash 和独立验证，给出未消费的具体阻因。";
            case "T16": return "从决策说明追到同一期权威冻结、trades 与结算，分别列真实出手和HOLD，缺结算保持未观察。";
            default: return "对照当前版本的查询合同、输入水位和适用的业务消费者，给新增表补独立复验。";
        }
    }
    private static String tableStage(Map<?,?> x, Map<String,Object> out, String id) {
        String status = small(x.get("status"));
        if (!"READ_OK".equals(status)) {
            if ("EMPTY_NO_ROWS".equals(status)) return "EMPTY_READ_PAGE";
            if ("READ_ERROR".equals(status)) return "QUERY_FAILED";
            return "QUERY_STATUS_UNKNOWN";
        }
        if (!id.equals("T06") && !id.equals("T10")) return "READ_PROJECTION_ONLY";
        Object rawRows = x.get("rows");
        if (!(rawRows instanceof List)) return "FRAME_DETAIL_UNAVAILABLE";
        List<?> page = (List<?>)rawRows;
        int blocked = 0, nonempty = 0, inspected = 0;
        Set<String> reasons = new LinkedHashSet<>();
        for (Object item : page) {
            Object raw = map(item).get("payload_json");
            if (!(raw instanceof String) || ((String)raw).length() > 262144) continue;
            Map<?,?> frame;
            try { frame = map(map(JsonR27.parse((String)raw)).get("frame")); }
            catch (RuntimeException malformed) { continue; }
            if (!(frame.get("points") instanceof List)) continue;
            if ("BLOCKED_DEPENDENCY".equals(frame.get("status"))) {
                blocked++;
                String reason = small(frame.get("reason"));
                if (!reason.isEmpty() && reasons.size() < 4) reasons.add(reason);
            } else if (!((List<?>)frame.get("points")).isEmpty()) {
                nonempty++;
            }
            inspected++;
        }
        out.put("inspectedPageRows",inspected);
        out.put("blockedPageRows",blocked);
        out.put("pageRowsWithPoints",nonempty);
        out.put("frameReasons",new ArrayList<>(reasons));
        if (blocked > 0 && blocked == page.size()) return "PAGE_DEPENDENCY_BLOCKED";
        if (nonempty > 0 && blocked > 0) return "MIXED_PAGE_CHECK_PAIRED_EVIDENCE";
        if (nonempty > 0) return "PAGE_HAS_POINTS_CHECK_PAIRED_EVIDENCE";
        return "FRAME_DETAIL_INSUFFICIENT";
    }
    private static String javaFile(Object type) {
        String name = small(type);
        if (!name.startsWith("com.ethk3.novaonepro.")) return "";
        int nested = name.indexOf('$');
        if (nested >= 0) name = name.substring(0,nested);
        return "app/src/main/java/" + name.replace('.','/') + ".java";
    }
    /** Registry group determines the existing code route. Location is never
     * promoted to evidence that the panel executed or has valid observations. */
    private static void legacyChartLocation(String id, Map<String,Object> row) {
        ChartRegistryR31.Entry chart=ChartRegistryR31.find(id);
        String producer,method,consumerMethod;
        switch(chart.group) {
            case LEGACY_STATUS: producer="com.ethk3.novaonepro.ChartsRepositoryR27";method="headlineSnapshotR30";consumerMethod="load";break;
            case LEGACY_CACHED: producer="com.ethk3.novaonepro.strategyv2.ChartCacheR27";method="build";consumerMethod="cached";break;
            case LEGACY_SNAPSHOT: producer="com.ethk3.novaonepro.strategyv2.ReportChartsR30";method="panels";consumerMethod="snapshotPanelR30";break;
            case LEGACY_R83: producer="com.ethk3.novaonepro.ChartsRepositoryR83";method="loadPanel";consumerMethod="load";break;
            default: throw new IllegalArgumentException("R325_NOT_LEGACY_CHART:"+id);
        }
        row.put("sourceFile",javaFile(producer));row.put("sourceMethod",method);
        row.put("consumerFile",javaFile("com.ethk3.novaonepro.ChartsRepositoryR27"));row.put("consumerMethod",consumerMethod);
        row.put("registeredSourceOwner",chart.sourceOwner);
        row.put("locationScope","REGISTERED_PRODUCER_CONSUMER_CODE_LOCATION_ONLY; NOT_EXECUTION_OR_EFFECT_EVIDENCE");
        if(chart.group==ChartRegistryR31.Group.LEGACY_STATUS)row.put("alternateSourceMethod","headline");
    }
    private static Map<?,?> map(Object x) { return x instanceof Map ? (Map<?,?>) x : Collections.emptyMap(); }
    private static boolean yes(Object x) { return Boolean.TRUE.equals(x) || "YES".equals(x); }
    private static String small(Object x) {
        if (!(x instanceof String)) return "";
        String s = (String) x;
        return s.substring(0, Math.min(s.length(), 320));
    }
    private static String experiment(String id) {
        switch (id) {
            case "F01": return "稀疏跨位置滞后组。固定目标期，切掉跨位置边并保留本位置，检验组收缩后独立 SELECT 的概率损失、K3 动作和成本。";
            case "F02": return "排列链相邻因子。将同一冻结图的相邻成对因子归零，保留一元项，检查合法排列边际和同期差。";
            case "F03": return "类型化三元消息。屏蔽一种三元消息通路，保持参数预算，核验真实梯度、十位置输出及差值。";
            case "F04": return "膨胀时间卷积。分别屏蔽长滞后卷积层与短滞后层，对同一序列做断点重置和未来输入扰动测试。";
            case "F05": return "时间注意力。对过去可见行比较已学权重和均匀权重，延迟新特征一步并验证未来标签不改变旧输出。";
            case "F06": return "选择性状态空间模型。分别冻结选择门与重置时序状态，跟踪梯度、代价及跨断期后的恢复值。";
            case "F07": return "图结构 GRU。分别切图边和循环门，保持训练窗口及资源相同，对照独立期概率损失和行为变化。";
            case "F08": return "因果 HMM 过滤。按原图变体切除状态转移记忆，比较过滤概率与重启前后状态。";
            case "F09": return "显式时长半马尔可夫。固定发射分布，进行移除时长项的对照，按停留时长分层检查校准及稀疏支持。";
            case "F10": return "在线变点。记录每次变点判断的可见数据末期，与冻结危害率的对照计算延迟、误报和换窗后损失。";
            case "F11": return "仅历史片段检索。对每个被选邻居验证结束期早于目标，固定或打乱距离权重比较 SELECT 概率损失。";
            case "F12": return "关系后缀自动机。比较有支持的状态分裂与退回父状态，列最小支持、失败片段和跨期预测差。";
            case "F13": return "类型化符号公式。固定公式树逐叶消融，保留函数类型、系数和合法滞后，记录真正改变 K3 的子树。";
            case "F14": return "共享状态宏。用相同单头特征比较共享宏启用与关闭，验证十位置共用状态重启后可复演。";
            case "F15": return "单路集合风险。固定概率面比较风险评分与简单 top-3，记录 1200 个候选动作中最终一个位置和三个号码。";
            case "F16": return "双路集合路由。比较原软路由、左右单路和固定等权，用同机会出手集合检查风险校准收益。";
            case "F17": return "跨位置滞后一的排列读出。打乱源位置或切滞后项，验证输出仍是合法边际且选出的三号码可复算。";
            case "F18": return "掩码自回归。保持已用号码约束，比较移除顺序依赖的较简模型；验证重复号码拒绝和边际归一。";
            case "F19": return "CP 关系张量。按现有变体移除位置或状态模式，保持 AdaGrad 累积量可恢复，在相同训练步比较十位置损失。";
            case "F20": return "因果状态片段。比较匹配检索、错配检索与过期记忆，核对检索只用旧片段和覆盖变化。";
            case "F21": return "折扣关系统计。冻结折扣因子并打乱源位置，与边际频率配对，报告有效支持和真实增量更新。";
            case "F22": return "冻结父代后的残差学习。固定父参数和分段边界，比较父单体、关闭残差门和子代，子代证据从零开始。";
            case "F23": return "教师蒸馏。先由 FamilyRegistryR26.bindLegacyTeacher 校验 asset、teacher、state 和 ABI，再对同输入比较学生与去教师学生。缺教师只挂起 F23。";
            case "F24": return "三模型组合。对 F01、F09、F11 各做单体和等权对照，再比较学习路由，记录权重、选择范围和同期净差。";
            case "I00": return "预注册随机对照的种子和机会集，记录误报及配对下界，仅作负对照。";
            case "I01": return "比较短长窗频率与同位置平滑频率，固定收缩规则后只在新窗口计分。";
            case "I02": return "只用上次出现以前的遗漏序列，和无周期项的规则同机会比较。";
            case "I03": return "冻结转移阶数，稀疏转移退回低阶，检验退回前后概率损失。";
            case "I04": return "保留目标位置，打乱其他位置或切跨位边，检验非边际信息。";
            case "I05": return "在同一概率面上开关排列约束，检查输出是否改变 K3 及校准。";
            case "I06": return "固定状态切换和失效条件，按切换前后独立窗口核对延迟和误率。";
            case "I07": return "历史候选片段的结束期先于 t，去掉相似检索后比较同目标损失。";
            case "I08": return "只接受已发布的同 ABI 权重面，按 model_hash 找到调用后预测，与去表示模型对照。";
            case "I09": return "记录类型化 AST、可用特征与滞后，等价表达式规范化去重，逐子树消融。";
            case "I10": return "按时间冻结失败簇，对同一失败机制比较原父代和指定修复子树。";
            case "I11": return "先验证双亲在同一期有互补误差，再与优良单亲和等权融合比较。";
            case "I12": return "对旧资产重放冻结输入，核对语义 ABI 和输出，再登记新子代独立证据。";
            case "I13": return "预注册出手分数阈值；分别报告全机会、共同出手、动作并集的质量与频率。";
            case "I14": return "记录围号两个来源位置、号码集合、交集与投影规则；用单来源对照复验固定位置 K3。";
            case "I15": return "只用两个截止于 t 以前的窗口，冻结差分符号与窗口长度，比较去差分基线。";
            case "I16": return "以预登记的位置轮换顺序和固定位置作配对，分解覆盖变化与真实收益。";
            case "I17": return "设置遗漏回补触发与失效期限，在相同遗漏长度桶对照简单频率，记录稀疏支持。";
            case "I18": return "拆分邻号与原位转移贡献，打乱邻接关系后重新冻结并比较。";
            case "I19": return "只用已揭晓期组合奇偶大小状态，在同支持数下比较去状态与平滑边际。";
            case "I20": return "把条件发生、交集产生及不足而 HOLD 分别记账；条件在 t 以后出现不得改变已冻结结果。";
            case "E01": return "逐条扰动 t 或 t 以后的标签，赛前输入哈希和当期动作应恒定；审计回执关联违规原件。";
            case "E02": return "同一旧资产恢复模型和状态并重放，记录首次分歧，兼容声明通过后才可提出 warm 子代。";
            case "E03": return "在 DEV 登记短期窗口的出现与失效，SELECT 作同机会去窗口对照，未来只验证。";
            case "E04": return "固定不同尺度的特征终点，删除长窗复验，分开报告滞后与信号增量。";
            case "E05": return "切除跨位边并保持单位置边际，只有改变实际选择或损失才记录贡献。";
            case "E06": return "记录号码组围号及其合法支持，取消组结构重放，区分更窄覆盖和真实增益。";
            case "E07": return "将表示学习提案绑定可消费权重哈希，对相同输入移除表示分支，记预测差。";
            case "E08": return "产出可执行的类型化公式，拒绝改名与同构；完成首步数值训练再计有效出生。";
            case "E09": return "把具体失败簇绑定到父规则和拟替换节点，测试修复在独立段是否过度专门化。";
            case "E10": return "选择同时期且残差互补的双亲，缺父代时留原因，融合后与两个单亲配对。";
            case "E11": return "核对继承的参数与优化器 ABI，错版本只允许合规冷启，子代不得继承父胜率。";
            case "E12": return "从冻结概率重算校准和门控，与固定阈值比较覆盖、ECE、Brier 及机会损失。";
            case "E13": return "固定漂移触发点，检查分块损失、最差块和错误报警，并提出降权或退役的依据。";
            case "E14": return "按原资源与编译审计职责记录真实 CPU、内存和可执行 ABI；审计回执单独计数。";
            case "E15": return "审计从资格原件到主链的冻结键，列撤销竞态和拒绝原因；只有 DecisionGate 可签 REAL。";
            case "E16": return "冻结输入和复现实验版本，给首个阻塞、反例与保全原件；审计记录无需制造一个新子代。";
            default: return null;
        }
    }
    private static String instruction(String group, String id, Map<?,?> x) {
        String experiment = experiment(id);
        if (group.equals("capabilities") && experiment != null) return experiment;
        if (group.equals("logicalTables")) return tableAction(id);
        if ("ATOMNET".equals(id)) return "核对十位置同一epoch、optimizer、非零更新及权重哈希。完成一轮后回读发布的模型，再追踪在线预测的模型引用。";
        if (group.equals("matrices") || id.startsWith("MATRIX:")) return "写入带role、目标期、可见时间、版本、shape及artifact hash的矩阵；沿本行消费者重演并移除矩阵比较输出。";
        if (group.equals("researchCharts")) return "按本行measure生产同源frame，经所列算法消费者记录inputHash与输出差；显示同目标配对数及不确定性。";
        if (group.equals("legacyCharts")) return "对照本行source的只读查询，记录分页、空集、取消、快照版本与新鲜度；展示用途和数据范围。";
        if (id.startsWith("I")) return "回读思路族生成的typed AST、父代和变异算子；在固定DEV探针重放行为，再以SELECT同机会比较。";
        if (id.startsWith("E")) return "记录专家输入残差、可执行提案、实际生成的子代和下游接受或拒绝原因；冻结消融对照。";
        return "以本行实现类重演一个训练步，核对输入、前后状态哈希、checkpoint和下游预测；用同机会父代及消融对照。";
    }
    private static String nextCheck(String group, String id) {
        if (id.equals("F23")) return "FamilyRegistryR26.build(F23)与教师绑定回执";
        if (id.equals("E01") || id.equals("E14") || id.equals("E15") || id.equals("E16"))
            return "对照本行专家实现和审计回执，以对应违规或故障复核审计结果";
        if (id.equals("I00")) return "按登记的同期对照、冻结键和独立期检查比较分母";
        if (id.equals("ATOMNET")) return "AtomNetRuntimeR30.step → AtomNetPublicationR31 → ModelSurfaceStoreR30 的发布/消费";
        if (group.equals("matrices") || id.startsWith("MATRIX:")) return "按本行producerMethod/algorithmConsumer比较原始矩阵与消融";
        if (id.equals("T06") || id.equals("T10")) return "PairedComponentStudyR30R1：同目标两个组件/父代";
        if (id.equals("T15")) return "ResearchObservatoryR30R1：处方、child与消费引用";
        if (group.equals("researchCharts") || group.equals("legacyCharts")) return "ChartsRepositoryR27 / ReportObjectIndexR30：核对同快照的版本、源行及消费";
        if (group.equals("logicalTables")) return "ResearchTablesActivityR31 与原表的只读查询、角色及业务消费者";
        return id.startsWith("I") ? "EvolutionLoopR27 与 BirthGateR27：出生、行为判重和配对验证"
                : id.startsWith("E") ? "ExpertProposalsR27：提案与下游出生的关联"
                : "FoundryCampaignR26.step：当前家族训练、checkpoint和消费者";
    }
    public static Map<String,Object> build(Object index) {
        Map<?,?> source = map(index);
        List<Map<String,Object>> rows = new ArrayList<>();
        List<String> gaps = new ArrayList<>();
        for (String group : GROUPS) {
            Set<String> expected = expectedIds(group);
            Map<?,?> section = map(source.get(group));
            Object raw = section.get("objects");
            List<?> objects = raw instanceof List ? (List<?>)raw : Collections.emptyList();
            Set<String> ids = new HashSet<>();
            if (objects.size()!=expected.size()) markGap(gaps,group+" expected="+expected.size()+" captured="+objects.size());
            for (Object entry : objects) {
                Map<?,?> x = map(entry), life = map(x.get("lifecycleR31"));
                String id = small(x.get("id"));
                if (!expected.contains(id)) { markGap(gaps,group+" unexpected="+id); continue; }
                if (!ids.add(id)) { markGap(gaps,group+" duplicate="+id); continue; }
                Map<String,Object> row = new LinkedHashMap<>();
                Map<?,?> runtimeEvidence=map(x.get("runtimeEvidenceR33"));
                if(!runtimeEvidence.isEmpty())row.put("runtimeEvidenceR33",runtimeEvidence);
                row.put("id",id); row.put("group",group);
                for (String k : new String[]{"implemented","callable","inputAvailable","produced","consumed","effective","retainedCount","shown","currentSourceProducedFrames","actualSourceConsumptionReceipts"})
                    row.put(k,x.get(k));
                for (String k : new String[]{"entryClass","implementationClass","entryMethod","producerClass","producerMethod","consumerClass","consumer","algorithmConsumer","source","measure","notProducedReason","executionBlockedReason","status","verification","reason","receiptSha256","title","label","name","sourceTable"})
                    if (x.get(k) instanceof String) row.put(k,small(x.get(k)));
                if (group.equals("logicalTables")) {
                    row.put("sourceFile",javaFile("com.ethk3.novaonepro.strategyv2.ResearchTableAdaptersR30R1"));
                    row.put("sourceMethod","readPageR31");
                    row.put("registeredSqlHash",StatsR27.sha256(ResearchTableAdaptersR30R1.query(id)));
                    row.put("pageEvidenceAudit",ResearchTableAuditR32.audit(id,x));
                } else if (group.equals("legacyCharts")) {
                    legacyChartLocation(id,row);
                } else {
                    String path = javaFile(x.get("producerClass"));
                    if (path.isEmpty()) path = javaFile(x.get("entryClass"));
                    if (path.isEmpty() && group.equals("researchCharts"))
                        path = javaFile("com.ethk3.novaonepro.strategyv2.ResearchObservatoryR30R1");
                    row.put("sourceFile",path.isEmpty() ? null : path);
                    Object method = x.get("producerMethod");
                    if (method == null) method = x.get("entryMethod");
                    row.put("sourceMethod",method == null && group.equals("researchCharts") ? "step" : method);
                }
                row.put("ran",life.get("ran"));
                row.put("verificationEntry",nextCheck(group,id));
                row.put("sourceSnapshot",source.get("sourceSnapshot"));
                String stage;
                if (group.equals("logicalTables")) stage = tableStage(x,row,id);
                else if ("PERSISTED_OBJECT_BLOCKER_RECORDED".equals(CapabilityRuntimeEvidenceR33.firstObservedGap(runtimeEvidence))) stage="PERSISTED_OBJECT_BLOCKER_RECORDED";
                else if (Boolean.FALSE.equals(x.get("callable"))) stage=group.equals("capabilities")?"CURRENT_REPORT_PROCESS_BINDING_UNAVAILABLE_RUNTIME_NOT_INFERRED":"BINDING_UNAVAILABLE";
                else if (Boolean.FALSE.equals(x.get("inputAvailable"))) stage="INPUT_UNAVAILABLE";
                else if (!yes(x.get("produced"))) stage=Boolean.FALSE.equals(x.get("produced"))?"NO_RECORDED_OUTPUT":"OUTPUT_UNKNOWN";
                else if (!yes(x.get("consumed"))) stage=Boolean.FALSE.equals(x.get("consumed"))?"NO_RECORDED_CONSUMPTION":"CONSUMPTION_UNKNOWN";
                else stage=yes(x.get("effective"))?"RECHECK_PAIRED_EVIDENCE":"PAIRED_EFFECT_UNPROVEN";
                row.put("firstKnownGap",stage); row.put("nextAction",instruction(group,id,x));
                if(!runtimeEvidence.isEmpty()){row.put("runtimeEvidenceStage",CapabilityRuntimeEvidenceR33.firstObservedGap(runtimeEvidence));Object blocked=runtimeEvidence.get("firstBlockedStatus");if(blocked instanceof String&&((String)blocked).startsWith("BLOCKED_WARM_NUMERICAL_ABI"))row.put("nextAction","先按候选原state/checkpoint与当前数值ABI逐键核验；保全旧父代字节与证据，不能清零或冒充兼容。仅由已验证的冷启重生事务建立独立新子代、重新训练并从零积累确认/未来证据。随后执行："+instruction(group,id,x));}
                if(group.equals("logicalTables")){Map<?,?> pageAudit=map(row.get("pageEvidenceAudit"));if("PAGE_INVARIANT_VIOLATION".equals(pageAudit.get("status"))||"PAGE_HAS_RECORDED_BLOCKER".equals(pageAudit.get("status"))){stage=String.valueOf(pageAudit.get("status"));row.put("firstKnownGap",stage);}}
                Map<String,Object> evidence = new LinkedHashMap<>();
                evidence.put("sourceObjectSha256", StatsR27.sha256(JsonR27.write(x)));
                evidence.put("sourceReference", "chapters[11].objectIndex."+group+".objects[id="+id+"]");
                evidence.put("sourceSnapshot",source.get("sourceSnapshot"));
                evidence.put("sourceCommit",source.get("sourceCommit"));
                evidence.put("rootCauseStatus","UNCONFIRMED_REPLAY_REQUIRED");
                evidence.put("observationScope",group.equals("logicalTables")?"EXPORTED_BOUNDED_PAGE":"SAME_REPORT_SNAPSHOT_OBJECT");
                evidence.put("firstObservedGap",stage);
                List<String> missing = new ArrayList<>();
                for (String k : new String[]{"inputAvailable","produced","consumed","effective"})
                    if (x.get(k)==null) missing.add(k);
                evidence.put("unobservedFields",missing);
                Map<String,Object> effect = new LinkedHashMap<>();
                effect.put("status","PAIRED_ORIGINAL_NOT_ATTACHED_TO_OBJECT");
                effect.put("estimate",null); effect.put("lower95",null); effect.put("upper95",null);
                effect.put("pairedCalendarIssues",null); effect.put("before",null); effect.put("after",null);
                effect.put("reason","本对象索引未附同目标配对原件，不能由产出、消费计数或图形反推提升；逻辑表页原数值另在pageEvidenceAudit中核验。");
                evidence.put("effectComparison",effect);
                row.put("evidenceContract",evidence);
                row.put("diagnosisSourceFile",group.equals("logicalTables")
                    ? javaFile("com.ethk3.novaonepro.strategyv2.ResearchTableAuditR32")
                    : javaFile("com.ethk3.novaonepro.strategyv2.RepairDiagnosisR32"));
                row.put("diagnosisSourceMethod",group.equals("logicalTables")?"audit/inspect":"build/instruction");
                rows.add(row);
            }
            for (String id : expected) if (!ids.contains(id)) {
                markGap(gaps,group+" missing="+id);
                Map<String,Object> absent = new LinkedHashMap<>();
                absent.put("id",id); absent.put("group",group);
                absent.put("firstKnownGap","SOURCE_OBJECT_MISSING");
                for (String k : new String[]{"implemented","callable","inputAvailable","produced","consumed","effective"}) absent.put(k,null);
                absent.put("nextAction","本快照未载入该登记对象，先恢复ReportObjectIndexR30的原对象采集，再执行："+instruction(group,id,Collections.emptyMap()));
                absent.put("verificationEntry",nextCheck(group,id));
                absent.put("sourceFile",javaFile("com.ethk3.novaonepro.strategyv2.ReportObjectIndexR30"));
                absent.put("sourceMethod","buildProductionR31");
                Map<String,Object> missingEvidence = new LinkedHashMap<>();
                missingEvidence.put("sourceObjectSha256",null);missingEvidence.put("sourceSnapshot",source.get("sourceSnapshot"));
                missingEvidence.put("sourceCommit",source.get("sourceCommit"));missingEvidence.put("rootCauseStatus","SOURCE_ABSENT_UNCONFIRMED");
                missingEvidence.put("observationScope","REGISTERED_OBJECT_WITHOUT_SOURCE_OBJECT");missingEvidence.put("unobservedFields",Arrays.asList("implemented","callable","inputAvailable","produced","consumed","effective"));
                absent.put("evidenceContract",missingEvidence);
                if (group.equals("logicalTables")) absent.put("pageEvidenceAudit",ResearchTableAuditR32.audit(id,Collections.emptyMap()));
                rows.add(absent);
            }
        }
        Map<String,Object> out = new LinkedHashMap<>();
        out.put("version","R32_CAPABILITY_DIAGNOSIS_V4"); out.put("sourceCommit",source.get("sourceCommit"));
        out.put("sourceSnapshot",source.get("sourceSnapshot")); out.put("rows",rows); out.put("coverageGaps",gaps);
        return out;
    }
    private static Map<?,?> chapter(List<? extends Map<?,?>> chapters, String id) {
        for (Map<?,?> entry : chapters) {
            if (id.equals(entry.get("sectionId"))) return entry;
        }
        return Collections.emptyMap();
    }

    private static Map<String,Object> fields(Map<?,?> input, String... keys) {
        Map<String,Object> output = new LinkedHashMap<>();
        for (String key : keys) output.put(key, input.get(key));
        return output;
    }

    static Map<String,Object> operatingSummaryR32(List<? extends Map<?,?>> chapters) {
        Map<?,?> quality = map(chapter(chapters, "R30_01").get("commonMetrics"));
        Map<?,?> runtime = map(chapter(chapters, "R30_04").get("data"));
        Map<?,?> models = map(chapter(chapters, "R30_06").get("data"));
        Map<?,?> neural = map(map(models.get("neuralTraining")).get("payload"));
        Map<String,Object> result = new LinkedHashMap<>();
        Map<String,Object> future = fields(quality, "policyObjects", "evolutionAttempts", "qualifiedExperiments",
                "independentFutureIssues", "futureActs", "futureHits", "activeCertificates");
        Object acts = quality.get("futureActs"), hits = quality.get("futureHits");
        Double hitRate = null;
        String status = "COUNTS_UNAVAILABLE_OR_INVALID";
        if (acts instanceof Number && hits instanceof Number) {
            double n = ((Number)acts).doubleValue(), h = ((Number)hits).doubleValue();
            if (Double.isFinite(n) && Double.isFinite(h) && n == Math.rint(n) && h == Math.rint(h)
                    && n >= 0 && h >= 0 && h <= n) {
                status = n == 0 ? "NO_SETTLED_FUTURE_ACT" : "OBSERVED_FUTURE_ONLY";
                if (n > 0) hitRate = h / n;
            }
        }
        future.put("futureHitRate", hitRate);
        future.put("rateStatus", status);
        result.put("independentFuture", future);
        result.put("atomNet", fields(neural, "materializedSamples", "trainingSamplesProcessed", "onlineSamples",
                "completeTrainedThroughOrdinal", "publicationReady", "consumable"));
        Map<String,Object> tasks = new LinkedHashMap<>();
        for (String wanted : new String[]{"foundry-su2", "r27-replay-dataset", "r27-evolution", "r27-atomnet", "r27-atomnet-archive"}) {
            Map<String,Object> fact = null;
            if (runtime.get("tasks") instanceof List) {
                for (Object item : (List<?>)runtime.get("tasks")) {
                    Map<?,?> task = map(item);
                    if (wanted.equals(task.get("task_id"))) {
                        fact = fields(task, "task_id", "steps", "cursor", "status", "blocked_reason");
                        if (fact.get("blocked_reason") instanceof String)
                            fact.put("blocked_reason", small(fact.get("blocked_reason")));
                        break;
                    }
                }
            }
            tasks.put(wanted, fact);
        }
        result.put("tasks", tasks);
        Map<?,?> retention=map(chapter(chapters,"R30_11").get("retention"));
        Map<?,?> lifecycle=map(retention.get("lifecycleEvidenceR31"));
        result.put("atomNetPublicationCapacity",lifecycle.get("atomNetPublicationCapacityR33"));
        Map<?,?> capacityReceipt=map(lifecycle.get("atomNetPublicationCapacityR33"));
        Map<?,?> archive=map(map(capacityReceipt.get("value")).get("archiveMaintenance"));
        Map<String,Object> archiveView=new LinkedHashMap<>();
        archiveView.put("task","r27-atomnet-archive");archiveView.put("learnerPrerequisite",false);
        archiveView.put("original",archive);archiveView.put("sourceReadScope",capacityReceipt.get("sourceScope"));
        long isolated=0;boolean known="READ_OK_NOT_A_HEALTH_VERDICT".equals(capacityReceipt.get("status"))&&archive.get("statusCounts") instanceof List;
        if(known)for(Object entry:(List<?>)archive.get("statusCounts")){
            Map<?,?> count=map(entry);Object value=count.get("objects");
            if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue())||((Number)value).doubleValue()<0||((Number)value).doubleValue()!=Math.rint(((Number)value).doubleValue())){known=false;break;}
            if("FAULT_PRESERVED".equals(count.get("status")))isolated+=((Number)value).longValue();
        }
        archiveView.put("status",!known?"UNKNOWN_ARCHIVE_SOURCE":isolated>0?"DEGRADED_ARCHIVE_ONLY":"OBSERVED_NO_ISOLATED_ARCHIVE_FAILURE");
        archiveView.put("isolatedOriginalObjects",known?isolated:null);
        archiveView.put("prescription","逐 publicationId 核对 boundedFailureExamples 的 failureCode、attempts 和 nextAttemptAt；保留原样本，修复对应源完整性后由独立 r27-atomnet-archive 到期重试。CLAIMED 未到期只表示已占次，不等于故障；FAULT_PRESERVED 只表示该归档对象隔离。训练是否推进另看 r27-atomnet 的提交游标、实训样本及发布回执，不能由归档失败推出全部训练失败。");
        archiveView.put("verification","毒对象保留且其它 publication 仍可归档；归档与训练任务故障预算独立；重启后 attempts/nextAttemptAt 连续；同一对象归档提交与样本退役原子一致。");
        result.put("atomNetArchiveMaintenance",archiveView);
        Map<?,?> foundry = map(tasks.get("foundry-su2"));
        boolean missingQuota = String.valueOf(foundry.get("blocked_reason")).contains("QUOTA_MISSING");
        result.put("foundryRepair", missingQuota
                ? "LifecycleSchemaR31.advanceWithinMigration，核对四张假设表、触发器和已占字节，迁移事务补齐配额后重试 FoundryCampaignR26.step。核对新候选出生、首个训练游标和重启后的状态。"
                : "按 foundry-su2 的任务回执读取最新停点，追踪同一策略从出生到 SELECT 和未来登记的第一处缺件。");
        result.put("qualityCheck", "按策略和资格协议查原始未来账、证书选择、真实冻结与结算。这里的未来样本和 REAL 结算分开计数。");
        return result;
    }

    private static List<Map<String,Object>> explainLegacyFindings(Map<String,Object> json) {
        List<Map<String,Object>> result = new ArrayList<>();
        if (!(json.get("findings") instanceof List)) return result;
        for (Object entry : (List<?>)json.get("findings")) {
            Map<?,?> original = map(entry);
            String code = small(original.get("code"));
            Map<String,Object> row = fields(original, "code", "scope", "severity", "value", "prescription");
            if ("S4".equals(code)) {
                row.put("meaning", DiagnosisTextSU2.T.get("F.S4.title"));
                row.put("nextAction", DiagnosisTextSU2.T.get("F.S4.action"));
                row.put("verify", "在下一预登记窗口核对重拟合次数、两半方向及支持数，保留每个位置的原始对照和未通过结果。");
            } else {
                row.put("meaning", DiagnosisTextSU2.T.get("F." + code + ".title"));
                row.put("nextAction", small(original.get("prescription")).isEmpty()
                        ? "按诊断码回读同一快照的升级诊断正文、生成方法和试验原件，标记尚待核实的原因。"
                        : small(original.get("prescription")));
                row.put("verify", "按原码和策略版本，回读对应窗口和可重放的独立证据。");
            }
            result.add(row);
        }
        return result;
    }

    public static String[] attach(String[] legacy, Map<String,Object> detail) {
        Map<String,Object> json = new LinkedHashMap<>();
        Object parsed = JsonR27.parse(legacy[1]);
        if (!(parsed instanceof Map)) throw new IllegalArgumentException("R32_LEGACY_DIAGNOSIS_SHAPE");
        for (Map.Entry<?,?> e: ((Map<?,?>)parsed).entrySet()) json.put(String.valueOf(e.getKey()),e.getValue());
        json.put("capabilityRepairR32",detail);
        StringBuilder md = new StringBuilder().append("\n\n## 能力逐项诊断与下一步核验\n");
        List<Map<String,Object>> explanations = explainLegacyFindings(json);
        json.put("legacyFindingExplanationsR32", explanations);
        if (detail.get("operatingSummary") instanceof Map) {
            Map<?,?> summary = map(detail.get("operatingSummary"));
            Map<?,?> q = map(summary.get("independentFuture")), n = map(summary.get("atomNet"));
            md.append("\n独立未来出手 / 命中 / 有效证书  ").append(q.get("futureActs")).append(" / ")
              .append(q.get("futureHits")).append(" / ").append(q.get("activeCertificates"));
            md.append("\n十位置网络已备样本 / 实训样本 / 完整发布  ").append(n.get("materializedSamples"))
              .append(" / ").append(n.get("trainingSamplesProcessed")).append(" / ").append(n.get("publicationReady"));
            Map<?,?> archive=map(summary.get("atomNetArchiveMaintenance"));
            md.append("\n归档维护状态 / 隔离对象  ").append(archive.get("status")).append(" / ").append(archive.get("isolatedOriginalObjects"))
              .append("\n").append(archive.get("prescription")).append("\n").append(archive.get("verification"));
            for (Map.Entry<?,?> e : map(summary.get("tasks")).entrySet())
                md.append("\n- ").append(e.getKey()).append("  ").append(JsonR27.write(e.getValue()));
            md.append("\n").append(summary.get("foundryRepair")).append("\n").append(summary.get("qualityCheck")).append("\n");
        }
        for (Map<?,?> finding : explanations)
            md.append("\n- ").append(finding.get("code")).append("  ").append(finding.get("nextAction"))
              .append("  ").append(finding.get("verify"));
        md.append("\n对象清单缺项：").append(detail.get("coverageGaps")).append('\n');
        md.append("快照 ").append(detail.get("sourceSnapshot")).append("，源码 ").append(detail.get("sourceCommit")).append('\n');
        for (Object entry : (List<?>)detail.get("rows")) {
            Map<?,?> row = map(entry);
            md.append("\n- ").append(row.get("group")).append('/').append(row.get("id")).append(" · ")
              .append(row.get("firstKnownGap")).append(" · ").append(row.get("nextAction")).append(" · ").append(row.get("verificationEntry"));
            Map<?,?> evidenceContract = map(row.get("evidenceContract"));
            md.append(" · 原件hash=").append(evidenceContract.get("sourceObjectSha256"))
              .append(" · 原因状态=").append(evidenceContract.get("rootCauseStatus"));
            Map<?,?> pageAudit = map(row.get("pageEvidenceAudit"));
            if (!pageAudit.isEmpty()) {
                md.append(" · 页内审计=").append(pageAudit.get("status"))
                  .append(" · 页分母=").append(pageAudit.get("denominator"))
                  .append(" · 问题计数=").append(JsonR27.write(pageAudit.get("observationCounts")))
                  .append(" · 首个观察=").append(JsonR27.write(pageAudit.get("firstObservation")));
            }
            for (String field : new String[]{"shown","retainedCount","currentSourceProducedFrames","actualSourceConsumptionReceipts","frameReasons","reason","receiptSha256","sourceFile","sourceMethod","consumerFile","consumerMethod","locationScope"}) {
                Object evidence = row.get(field);
                if (evidence != null) md.append(" · ").append(field).append("=").append(String.valueOf(evidence).replace('\n',' ').replace('\r',' '));
            }
        }
        return new String[]{md.append("\n\n").append(legacy[0]).toString(),JsonR27.write(json)};
    }
}
