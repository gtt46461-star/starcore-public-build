package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Read-only audit of the exact bounded logical-table page exported from the report snapshot.
 * These observations never grant strategy qualification, mutate a model, or infer a full-source denominator. */
public final class ResearchTableAuditR32 {
    private ResearchTableAuditR32() {}
    private static Map<?,?> map(Object value) { return value instanceof Map ? (Map<?,?>)value : Collections.emptyMap(); }
    private static String str(Object value) { return value instanceof String ? (String)value : ""; }
    private static Double number(Object value) {
        if (!(value instanceof Number)) return null;
        double n=((Number)value).doubleValue(); return Double.isFinite(n) ? n : null;
    }
    private static boolean nonempty(Object value) { return value instanceof String && !((String)value).trim().isEmpty(); }
    private static boolean yes(Object value) {
        Double numeric=number(value);
        return Boolean.TRUE.equals(value) || numeric!=null&&numeric==1.0 || "YES".equals(value);
    }
    private static boolean knownId(String id) {
        if(id==null||!id.matches("T[0-9]{2}"))return false;
        int n=Integer.parseInt(id.substring(1));return n>=1&&n<=16;
    }
    private static final class Scan {
        final Map<String,Long> counts=new LinkedHashMap<>();
        final List<Object> examples=new ArrayList<>();
        final List<Object> numericObservations=new ArrayList<>();
        long rows,invalid,blocked,unobserved;
        void add(String code,String severity,int row,String sourceHash,String action) {
            counts.put(code,counts.getOrDefault(code,0L)+1);
            if ("INVALID".equals(severity)) invalid++;
            if ("BLOCKED".equals(severity)) blocked++;
            if ("UNOBSERVED".equals(severity)) unobserved++;
            if (examples.size()<16) {
                Map<String,Object> detail=new LinkedHashMap<>();
                detail.put("code",code);detail.put("severity",severity);detail.put("pageRow",row);
                detail.put("sourceRowSha256",sourceHash);detail.put("nextAction",action);
                examples.add(detail);
            }
        }
    }
    private static void temporal(Map<?,?> r,String earlier,String later,Scan scan,int row,String hash,boolean strict) {
        Double a=number(r.get(earlier)),b=number(r.get(later));
        if(a==null||b==null) {scan.add("TEMPORAL_FIELDS_UNAVAILABLE:"+earlier+":"+later,"UNOBSERVED",row,hash,"回读同一原表行补齐可见时间；缺值不按零推导PIT通过。");return;}
        if(strict?a>=b:a>b) scan.add("TEMPORAL_ORDER_VIOLATION:"+earlier+":"+later,"INVALID",row,hash,"隔离本行，不用于晋升；按冻结、标签可见和发布事务逐键复演。");
    }
    private static Map<?,?> payload(Map<?,?> r,String field,Scan scan,int row,String hash) {
        String raw=str(r.get(field));
        if(raw.isEmpty()) {scan.add("PAYLOAD_UNAVAILABLE:"+field,"UNOBSERVED",row,hash,"回读原始有界分析产物；不能用页面行数代替数值原件。");return Collections.emptyMap();}
        if(raw.length()>262144) {scan.add("PAYLOAD_AUDIT_BUDGET:"+field,"UNOBSERVED",row,hash,"按原表分页检查原件；本诊断不复制超预算载荷。");return Collections.emptyMap();}
        try {
            Object value=JsonR27.parse(raw);
            if(!(value instanceof Map)) throw new IllegalArgumentException("OBJECT_REQUIRED");
            return map(value);
        } catch(RuntimeException malformed) {scan.add("PAYLOAD_MALFORMED:"+field,"INVALID",row,hash,"保全坏原件并核对生产者序列化；不得清空为成功或零值。");return Collections.emptyMap();}
    }
    private static void frame(Map<?,?> r,Scan scan,int row,String hash) {
        Map<?,?> root=payload(r,"payload_json",scan,row,hash),frame=map(root.get("frame"));
        if(frame.isEmpty()) {if(!root.isEmpty())scan.add("FRAME_UNAVAILABLE","UNOBSERVED",row,hash,"按当前measure版本回读frame结构与生产者；旧结构保留历史标签。");return;}
        if("BLOCKED_DEPENDENCY".equals(frame.get("status"))) scan.add("FRAME_BLOCKED_DEPENDENCY","BLOCKED",row,hash,"读取frame.reason，先补齐同目标父代或组件；阻断帧不能记融合或协同已生效。");
        else if(!(frame.get("points") instanceof List)||((List<?>)frame.get("points")).isEmpty()) scan.add("FRAME_WITHOUT_POINTS","UNOBSERVED",row,hash,"核对可执行输入、配对分母和算法输出；空frame只计运行回执。");
    }
    private static void inspect(String id,Map<?,?> r,Scan scan,int row,String hash) {
        switch(id) {
            case "T01":
                temporal(r,"from_ordinal","through_ordinal",scan,row,hash,false);
                if(!nonempty(r.get("dataset_hash"))||!nonempty(r.get("role")))scan.add("DATASET_OR_ROLE_UNAVAILABLE","UNOBSERVED",row,hash,"补齐run的dataset_hash和角色后联查训练水位与特征血缘。");
                String state=str(r.get("status"));
                if(state.contains("FAIL")||state.contains("BLOCK"))scan.add("ANALYSIS_RUN_NOT_COMPLETED","BLOCKED",row,hash,"按run_id回读summary_json与任务停点，只重试未提交的分析段。");
                break;
            case "T02":
                Double missing=number(r.get("missing_rate")),n=number(r.get("n_select"));
                if(missing!=null&&(missing<0||missing>1)||n!=null&&(n<0||n!=Math.rint(n)))scan.add("ATOM_SUPPORT_VALUE_INVALID","INVALID",row,hash,"修复原子评分生产者数值及支持分母，失效对应缓存再复算。");
                if(n==null||n==0)scan.add("ATOM_SELECT_SUPPORT_UNAVAILABLE","UNOBSERVED",row,hash,"等待独立SELECT原始期，先重放评分游标；不得把无支持原子评为高质量。");
                break;
            case "T03":
                temporal(r,"frozen_at","deadline_at",scan,row,hash,false);
                Double freeze=number(r.get("frozen_at")),deadline=number(r.get("deadline_at"));
                if(freeze!=null&&deadline!=null&&freeze<=deadline&&deadline-freeze<5000)
                    scan.add("FROZEN_LEAD_BELOW_5000MS","OBSERVED",row,hash,"原冻结发生在截止之前但未达到赛前5秒目标；与完全缺冻结分列，不补写过去冻结。");
                Double settled=number(r.get("settled_at"));
                if(settled==null||settled<=0)scan.add("FROZEN_NOT_SETTLED","UNOBSERVED",row,hash,"按target_issue追踪开奖与唯一结算任务；schema默认settled_at=0为未结算，不能进入命中率分母。");
                else {
                    temporal(r,"frozen_at","settled_at",scan,row,hash,false);
                    Double hit=number(r.get("hit"));
                    if(hit==null)scan.add("SETTLED_HIT_UNAVAILABLE","UNOBSERVED",row,hash,"回读唯一结算事务的hit字段，缺失结果不进入命中率分母。");
                    else if(hit!=0.0&&hit!=1.0)scan.add("SETTLED_HIT_INVALID","INVALID",row,hash,"已结算hit只能是0或1；隔离坏结算并按原冻结与揭晓重放，禁止改填命中。");
                }
                break;
            case "T04":
                if(!nonempty(r.get("parent_hash")))scan.add("FAILURE_PARENT_UNAVAILABLE","UNOBSERVED",row,hash,"把失败窗口、实际父代和错误输出绑定后再生成修复处方。");
                payload(r,"payload_json",scan,row,hash);
                break;
            case "T05":
                Double lower=number(r.get("lower95")),upper=number(r.get("upper95")),mean=number(r.get("mean_gain")),support=number(r.get("evaluated_calendar_issues"));
                if(lower!=null&&upper!=null&&(lower>upper||mean!=null&&(mean<lower||mean>upper)))scan.add("PAIRED_INTERVAL_INVALID","INVALID",row,hash,"按同机会原始配对重算均值、区间和聚类单位，保全错误版本。");
                if(support!=null&&(support<0||support!=Math.rint(support)))scan.add("PAIRED_SUPPORT_INVALID","INVALID",row,hash,"独立配对期数必须为非负整数，按同目标期唯一键重算。");
                if(support==null||support<=0)scan.add("PAIRED_SUPPORT_UNAVAILABLE","UNOBSERVED",row,hash,"补齐同目标原始期与消融配对，缺分母不估计收益。");
                if(lower!=null&&lower<=0)scan.add("PAIRED_ADVANTAGE_UNPROVEN","OBSERVED",row,hash,"保留负结果，按失败机制选择新结构；本页区间不授资格。");
                break;
            case "T06":case "T09":case "T10": frame(r,scan,row,hash);break;
            case "T07":
                if(nonempty(r.get("kept_policy"))&&Objects.equals(r.get("kept_policy"),r.get("dropped_policy")))scan.add("DEDUP_SELF_DROP","INVALID",row,hash,"修复行为去重事务，禁止资产删除自身并复验引用保护。");
                break;
            case "T08":
                if(!nonempty(r.get("policy_hash")))scan.add("EXPERT_PROPOSAL_WITHOUT_CHILD","UNOBSERVED",row,hash,"按generation与expert追到出生接受或拒绝回执；提案不能记为有效子代。");
                break;
            case "T11":
                temporal(r,"trained_through_ordinal","target_ordinal",scan,row,hash,true);
                temporal(r,"feature_through_ordinal","target_ordinal",scan,row,hash,true);
                temporal(r,"model_available_at","frozen_at",scan,row,hash,false);
                temporal(r,"frozen_at","deadline_at",scan,row,hash,false);
                if(!nonempty(r.get("model_hash")))scan.add("CONSUMED_MODEL_HASH_UNAVAILABLE","UNOBSERVED",row,hash,"沿权重发布原件、ModelSurface与概率hash查同一模型的真实消费。");
                break;
            case "T12":payload(r,"payload_json",scan,row,hash);break;
            case "T13":
                if(!yes(r.get("verified")))scan.add("CONFIRMATION_NOT_VERIFIED","UNOBSERVED",row,hash,"回读task_key的锁定块和result_json；未验证样本不授证。");
                if("REGISTERED".equals(r.get("registration_state"))&&!nonempty(r.get("registration_experiment_id")))scan.add("REGISTERED_EXPERIMENT_REFERENCE_MISSING","INVALID",row,hash,"回读注册事务与实验唯一键，禁止孤立登记状态进入主链。");
                break;
            case "T14":
                if(nonempty(r.get("blocked_reason")))scan.add("TASK_BLOCKED_REASON_RECORDED","BLOCKED",row,hash,"按task_id读取blocked_reason与已提交游标，补前置输入后从游标耐久重投。");
                break;
            case "T15":
                if(!nonempty(r.get("receipt_hash")))scan.add("PROPOSAL_CONSUMPTION_NOT_RECORDED","UNOBSERVED",row,hash,"按proposal_id查接受/拒绝；只有child_policy_hash与receipt_hash落账才记消费。");
                else if(!nonempty(r.get("child_policy_hash")))scan.add("CONSUMPTION_CHILD_REFERENCE_MISSING","INVALID",row,hash,"回读consume事务与受保护子代，恢复引用后复验训练接线。");
                break;
            case "T16":
                if(yes(r.get("original_allow"))&&!yes(r.get("certificate_active")))scan.add("ALLOW_WITHOUT_ACTIVE_CERTIFICATE_RECHECK_GATE","OBSERVED",row,hash,"沿decision_key查最终Gate与trades；original_allow不是REAL签收，不把本观察直接认定重复出手。");
                break;
            default:scan.add("UNKNOWN_TABLE_ID","UNOBSERVED",row,hash,"先登记逻辑表字段合同与审计语义。");
        }
    }
    public static Map<String,Object> audit(String id,Object pageObject) {
        Map<?,?> page=map(pageObject);Scan scan=new Scan();
        Object value=page.get("rows");List<?> rows=value instanceof List?(List<?>)value:Collections.emptyList();
        if(!knownId(id))scan.add("UNKNOWN_TABLE_ID","INVALID",-1,null,"先登记逻辑表ID、字段合同与审计语义；未知对象不能输出检查清晰。");
        if(!(value instanceof List))scan.add("PAGE_ROWS_UNAVAILABLE","UNOBSERVED",-1,null,"恢复该页原始rows数组；缺失或错误形状不是空集，也不代表审计完成。");
        if("READ_OK".equals(page.get("status"))&&rows.isEmpty())scan.add("READ_OK_WITHOUT_ROWS","UNOBSERVED",-1,null,"核对查询状态与页内容；实际空集应为EMPTY_NO_ROWS，不把零次检查记为检查清晰。");
        if("EMPTY_NO_ROWS".equals(page.get("status"))&&!rows.isEmpty())scan.add("PAGE_STATUS_ROWS_CONFLICT","INVALID",-1,null,"修复查询结果封装，状态和原始页必须来自同一读回执。");
        Double shown=number(page.get("shown"));
        if(page.containsKey("shown")&&(shown==null||shown<0||shown!=Math.rint(shown)||shown!=rows.size()))scan.add("PAGE_SHOWN_ROWS_MISMATCH","INVALID",-1,null,"将shown绑定本页实际rows长度，保留原错误回执，不补造缺失行。");
        if(rows.size()>200)scan.add("PAGE_AUDIT_ROW_BUDGET","UNOBSERVED",-1,null,"按每页至多200行继续读取；本页超界尾部未审计，不得报告全页已清晰。");
        List<Object> hashes=new ArrayList<>();
        for(int i=0;i<Math.min(rows.size(),200);i++) {
            if(!(rows.get(i) instanceof Map)) {scan.add("ROW_SHAPE_INVALID","INVALID",i,null,"回读原逻辑表字段对象；null、字符串和数组不能转换为空对象通过审计。");continue;}
            Map<?,?> r=map(rows.get(i));String hash;
            try {hash=StatsR27.sha256(JsonR27.write(r));}
            catch(RuntimeException invalid) {scan.add("ROW_NOT_CANONICAL_JSON","INVALID",i,null,"回读同一原表行，修复非有限数或不支持的序列化类型。");continue;}
            hashes.add(hash);scan.rows++;inspect(id,r,scan,i,hash);
            if(scan.numericObservations.size()<16) {
                Map<String,Object> observed=new LinkedHashMap<>();
                observed.put("pageRow",i);observed.put("sourceRowSha256",hash);
                for(String key:new String[]{"run_id","measure_key","parent_hash","policy_hash","atom_id","model_hash",
                        "target_ordinal","position","n_select","missing_rate","evaluated_calendar_issues",
                        "mean_gain","lower95","upper95","frozen_at","deadline_at","settled_at",
                        "trained_through_ordinal","feature_through_ordinal","model_available_at"})
                    if(r.containsKey(key)&&(r.get(key)==null||r.get(key) instanceof Number||r.get(key) instanceof String))observed.put(key,r.get(key));
                if(observed.size()>2)scan.numericObservations.add(observed);
            }
        }
        Map<String,Object> out=new LinkedHashMap<>();
        out.put("version","R32_RESEARCH_TABLE_PAGE_AUDIT_2");out.put("id",id);out.put("readStatus",page.get("status"));
        out.put("scope","EXPORTED_PAGE_ONLY_NOT_FULL_SOURCE_OR_QUALIFICATION");out.put("denominator",scan.rows);
        out.put("providedRows",value instanceof List?rows.size():null);out.put("auditLimit",200);
        out.put("rowsNotInspected",value instanceof List?Math.max(0L,rows.size()-scan.rows):null);
        out.put("sourcePageReceiptSha256",page.get("receiptSha256"));out.put("sourcePageOffset",page.get("offset"));
        out.put("sourcePageHasMore",page.get("hasMore"));out.put("sourceRowsSha256",StatsR27.sha256(JsonR27.write(hashes)));
        long observations=0;for(Long count:scan.counts.values())observations+=count;
        out.put("observationCounts",scan.counts);out.put("examples",scan.examples);out.put("examplesOmitted",Math.max(0L,observations-scan.examples.size()));
        out.put("numericObservations",scan.numericObservations);
        out.put("numericObservationScope","ORIGINAL_VALUES_FROM_THIS_BOUNDED_PAGE; NOT_FULL_DENOMINATOR_OR_ADVANTAGE");
        String status=scan.invalid>0?"PAGE_INVARIANT_VIOLATION":
                "EMPTY_NO_ROWS".equals(page.get("status"))&&value instanceof List&&rows.isEmpty()?"EMPTY_PAGE_NO_MEASURED_EFFECT":
                !"READ_OK".equals(page.get("status"))?"PAGE_NOT_READABLE":
                scan.blocked>0?"PAGE_HAS_RECORDED_BLOCKER":scan.unobserved>0?"PAGE_EVIDENCE_INCOMPLETE":"PAGE_CHECKS_CLEAR_EFFECT_NOT_PROVEN";
        out.put("status",status);out.put("confidence","DIRECT_PAGE_OBSERVATIONS_ROOT_CAUSE_UNCONFIRMED");
        out.put("firstObservation",scan.examples.isEmpty()?null:scan.examples.get(0));
        out.put("qualityGateEffect","NONE_READ_ONLY");return Collections.unmodifiableMap(out);
    }
}
