package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The same result object is exported to JSON/HTML. Missing evidence never becomes a number or PASS. */
public final class MetricEvaluatorR29 {
    private MetricEvaluatorR29() {}
    public static final class Probe {
        public final String status, reason, population;
        public final Number numerator, denominator, value;
        public final long samples;
        public final List<String> evidence;
        public Probe(String status, String reason, String population, Number numerator, Number denominator, Number value, long samples, List<String> evidence) {
            if (samples < 0) throw new IllegalArgumentException("R29_NEGATIVE_SAMPLE_COUNT");
            if ("PASS".equals(status) && (value == null || samples == 0 || evidence == null || evidence.isEmpty())) throw new IllegalArgumentException("R29_PASS_WITHOUT_EVIDENCE");
            this.status=status; this.reason=reason; this.population=population; this.numerator=numerator; this.denominator=denominator; this.value=value; this.samples=samples; this.evidence=evidence;
        }
    }
    @SuppressWarnings("unchecked")
    public static List<Map<String,Object>> evaluate(Map<String,Probe> probes, Map<String,Object> snapshot) {
        List<Map<String,Object>> out=new ArrayList<>();
        for(Object obj:(List<Object>)JsonR27.parse(MetricDefinitionsR29.JSON)) {
            Map<String,Object> def=(Map<String,Object>)obj; String id=(String)def.get("id"); Probe p=probes.get(id);
            Map<String,Object> r=new LinkedHashMap<>(def);
            r.put("metricId", id); r.put("definitionVersion", "R29.1"); r.put("sourceSnapshot", snapshot);
            // R30-B00-05: a missing source (no probe) or a probe without samples/value is INSUFFICIENT with a null value;
            // it never becomes 0 or PASS. FAIL is negative evidence and stays FAIL.
            boolean insufficient = p == null || ((p.samples == 0 || p.value == null) && !"FAIL".equals(p.status));
            r.put("status",p==null&&id.equals("Q02") ? "NOT_DEPLOYED" : insufficient ? "INSUFFICIENT" : p.status);
            if (p != null && insufficient && !p.status.equals("INSUFFICIENT")) r.put("probeStatus", p.status);
            r.put("population",p==null?null:p.population); r.put("numerator",p==null?null:p.numerator); r.put("denominator",p==null?null:p.denominator);
            r.put("value",p==null||insufficient?null:p.value); r.put("sampleCount",p==null?null:p.samples);
            r.put("missingReason",p==null?"No source-bound execution receipt for "+def.get("source"):p.reason);
            r.put("evidenceRefs",p==null||p.evidence==null?new ArrayList<>():p.evidence);
            r.put("timeRange",snapshot.get("timeRange")); r.put("uncertainty",null);
            r.put("nextTrigger",p==null?"Collect the specified runtime/device/quality source; do not infer completion from compilation":"Re-evaluate on the next committed snapshot");
            List<String> repair=new ArrayList<>();
            for(int i=1;i<=52;i++) if (cardMetrics(i).contains("|"+id+"|")) repair.add(String.format(java.util.Locale.ROOT,"R29-%03d",i));
            r.put("repairCardIds",repair); out.add(r);
        }
        return out;
    }
    private static String cardMetrics(int card) { return MetricCardMapR29.VALUES[card-1]; }

    /** One detached fact object per current metric; historical evaluate() is deliberately unchanged. */
    public static List<Map<String,Object>> evaluateR30(ReportR27.R30Input in) {
        List<Map<String,Object>> out=new ArrayList<>();
        for(Map<String,Object> definition:MetricDefinitionsR29.currentR30()) {
            String id=(String)definition.get("id");Map<String,Object> r=new LinkedHashMap<>(definition);
            r.put("status",in.errors.isEmpty()?"INSUFFICIENT":"UNKNOWN_CAPTURE_FAILED");r.put("value",null);
            r.put("numerator",null);r.put("denominator",null);r.put("sampleCount",null);
            r.put("population","SOURCE_NOT_YET_MEASURED");r.put("role","ENGINEERING".equals(definition.get("kind"))?"ENGINEERING":"FUTURE");
            r.put("sourceSnapshot",in.watermark);r.put("timeRange",in.watermark.get("factsReadAt"));
            r.put("uncertainty",null);r.put("gateAuthority",CertificateGateR27.GATE_VERSION);
            r.put("missingReason","缺少同版本、同角色的测量原件："+definition.get("measurementSource"));
            r.put("nextTrigger","采集 "+definition.get("measurementSource")+"；按 "+definition.get("repairCardIds")+" 验证，禁止用编译通过代替");
            r.put("evidenceRefs",java.util.Collections.singletonList(definition.get("measurementSource")));
            if(in.errors.isEmpty())currentMeasurement(in,id,r);
            out.add(ReportR27.freezeMap(r));
        }
        return java.util.Collections.unmodifiableList(out);
    }
    @SuppressWarnings("unchecked") static Map<String,Object> map(Object x){return x instanceof Map?(Map<String,Object>)x:java.util.Collections.emptyMap();}
    static Number number(Object x){return x instanceof Number&&Double.isFinite(((Number)x).doubleValue())?(Number)x:null;}
    static long count(Object x){Number n=number(x);return n==null?-1:n.longValue();}
    static List<?> rows(ReportR27.R30Input in,String key){Object x=in.data.get(key);return x instanceof List?(List<?>)x:java.util.Collections.emptyList();}
    static void observed(Map<String,Object> r,Object value,Object numerator,Object denominator,long samples,String population,String status,String reason){
        r.put("value",value);r.put("numerator",numerator);r.put("denominator",denominator);r.put("sampleCount",samples<0?null:samples);r.put("population",population);r.put("status",status);r.put("missingReason",reason);
    }
    static void source(ReportR27.R30Input in,Map<String,Object> r,String... keys){Map<String,Object> sources=new LinkedHashMap<>();for(String key:keys)sources.put(key,in.data.containsKey(key)?in.data.get(key):in.facts.get(key));r.put("observations",sources);}
    static String zeroState(Number value){return value==null?"INSUFFICIENT":value.doubleValue()>0?"FAIL":"RECORDED_UNVALIDATED";}
    static void currentMeasurement(ReportR27.R30Input in,String id,Map<String,Object> r){
        switch(id){
            case "M01": case "M02": {
                Map<String,Probe> p=ReportR29.probes(map(in.data.get("baselineIdentity")).isEmpty()?null:map(in.data.get("baselineIdentity")),map(in.data.get("legacyPreservation")).isEmpty()?null:map(in.data.get("legacyPreservation")));
                Probe x=p.get(id);if(x!=null)observed(r,x.value,x.numerator,x.denominator,x.samples,x.population,x.status,x.reason);
                source(in,r,"baselineIdentity","legacyPreservation");break;
            }
            case "M03":case "M04":case "M05":timeline(in,id,r);break;
            case "M06": {
                Number n=number(in.facts.get("duplicateRealIssues"));observed(r,n,n,in.facts.get("realRows"),count(in.facts.get("realRows")),"ORIGINAL_REAL_LEDGER_ONLY",zeroState(n),n==null?"原REAL账不可读；不能用影子行替代":"仅原REAL逐期唯一性；正式结算唯一键仍需原结算账联合回执");break;
            }
            case "M07":writer(in,r);break;
            case "M10":source(in,r,"outbox","qualificationReceipts","qualificationConflicts");break;
            case "M11":case "M12": {
                Map<String,Object> live=map(in.data.get("taskLiveness")),summary=map(live.get("summary"));source(in,r,"taskLiveness","tasks");
                Number n=number(summary.get(id.equals("M12")?"m12":"oldestBacklogIssues"));
                if(n!=null)observed(r,n,null,summary.get("tasks"),count(summary.get("tasks")),"READY_CALENDAR_ISSUE_TASKS",n.doubleValue()>(id.equals("M12")?0:5)?"FAIL":"RECORDED_UNVALIDATED","有类型的任务窗口；等待新数据、依赖、工作项单位分列，未运行场景另验");break;
            }
            case "M15":source(in,r,"draws","storage","retentionRun","retentionArchives");break;
            case "M17":source(in,r,"objectIndex");break;
            case "M18":source(in,r,"pit","pitViolations");r.put("missingReason","PIT拒绝记录不等于泄漏；需当前/未来标签扰动原件，注入与产品来源分列");break;
            case "M19":source(in,r,"confirmation","confirmationReservations");break;
            case "M20":source(in,r,"checkpoints","abi","neuralTraining");break;
            case "M21":source(in,r,"birthRejects","policyObjects");break;
            case "M22": {
                Number n=number(in.facts.get("behaviorDuplicates"));if(n!=null)observed(r,n,n,n,n.longValue(),"PERSISTED_BEHAVIOR_DUPLICATE_LEDGER","RECORDED_UNVALIDATED","原账全量计数；结构/参数去重及生成链一致性需分别复算");break;
            }
            case "M23": {
                List<?> all=rows(in,"capabilities");long executable=0;for(Object x:all)if(Boolean.TRUE.equals(map(x).get("executable")))executable++;
                if(!all.isEmpty())observed(r,executable,executable,all.size(),all.size(),"STATIC_DECLARED_HANDLERS_NOT_RUNTIME_EFFECT",executable==all.size()?"RECORDED_UNVALIDATED":"BLOCKED","可调用绑定与输入可用、真实产出、被消费、有效性分别记录，禁止由此推出质量");source(in,r,"capabilities");break;
            }
            case "M24":{source(in,r,"modelCalls","checkpoints","modelSurfaceRows","modelConsumption");Map<String,Object> census=map(in.data.get("modelConsumption"));Number ratio=number(census.get("matchingRatio"));r.put("observations",census);if("FAIL".equals(census.get("status")))observed(r,ratio,census.get("applicableMatchedCalls"),census.get("expectedCallsFromActualAstPositions"),count(census.get("expectedCallsFromActualAstPositions")),"RETAINED_FROZEN_MODEL_POSITIONS_FROM_ACTUAL_AST","FAIL","原策略需要的位置与同一次冻结的model/source/probability回执不匹配");else if(ratio!=null)observed(r,ratio,census.get("applicableMatchedCalls"),census.get("expectedCallsFromActualAstPositions"),count(census.get("expectedCallsFromActualAstPositions")),"RETAINED_FROZEN_MODEL_POSITIONS_FROM_ACTUAL_AST","RECORDED_UNVALIDATED","同一只读事务内的完整结构匹配；原件重算、设备效果与自然质量另验");else if("BLOCKED".equals(census.get("status"))){r.put("status","BLOCKED");r.put("missingReason","没有适用冻结消费，保留缺面/来源阻断；0/0不显示100%");}break;}
            case "M25":source(in,r,"objectIndex","matrices","nativeMatrixRows","su2MatrixRows");break;
            case "M26":source(in,r,"calibration");break;
            case "M27": {
                Number n=number(in.facts.get("openQualificationConflicts"));source(in,r,"qualificationConflicts","qualification");
                if(n!=null)observed(r,n,n,in.facts.get("qualificationReceipts"),count(in.facts.get("qualificationReceipts")),"CURRENT_QUALIFICATION_CONFLICT_LEDGER",zeroState(n),"未解决冲突数；全局序号、预算不重置与连续游标需联合原件");break;
            }
            case "M28":case "M30":source(in,r,"decisions","unsafeDecisionMismatches");if(count(in.facts.get("unsafeDecisionMismatches"))>0)observed(r,in.facts.get("unsafeDecisionMismatches"),null,null,count(in.facts.get("decisionRecords")),"ORIGINAL_REAL_AUTHORITY_COMPARISON","FAIL","原授权比逐项解释更宽；保持唯一原授权，必须逐期修复和复测");break;
            case "M29":source(in,r,"confirmation");break;
            case "M31": {
                Map<String,Object> d=DiagnosisRulesR30.evaluate(in.facts,in.data,in.watermark,in.errors);source(in,r,"upgradeSnapshots","upgradeState");
                Number a=number(d.get("actionableRules")),n=number(d.get("expectedRules"));if(a!=null&&n!=null&&n.longValue()>0)observed(r,a.doubleValue()/n.doubleValue(),a,n,n.longValue(),"DIAGNOSTIC_RULE_ACTION_COVERAGE","RECORDED_UNVALIDATED","行动字段覆盖，不能代替诊断结论正确性及修复验收");break;
            }
            case "M32":observed(r,0,0,44,44,"SINGLE_IMMUTABLE_METRIC_OBJECT_PER_CAPTURE","RECORDED_UNVALIDATED","12章与同源图引用同一MetricResult；跨设备UI对照和历史缓存必须另验，不算真机PASS");break;
            case "M35":source(in,r,"revocations","revocationEvents");break;
            case "M36":case "Q08":source(in,r,"syntheticControl");r.put("role","SYNTHETIC");r.put("missingReason","原合成对照可复算性与多轮功效区间单独验收；合成不计入自然质量");break;
            case "Q01": {
                Number hits=number(in.facts.get("futureHits")),acts=number(in.facts.get("futureActs"));source(in,r,"qualification");
                if(hits!=null&&acts!=null&&acts.longValue()>0)observed(r,hits.doubleValue()/acts.doubleValue(),hits,acts,acts.longValue(),"ACTIVE_EXPERIMENT_FUTURE_ACT_ROWS_NOT_INDEPENDENT_ISSUES","DESCRIPTIVE_ONLY","多策略重叠期不能合并为独立样本或证明自然质量；按每个预登记策略原件验证Q01");break;
            }
            case "Q02": {
                source(in,r,"realRows","activeCertificates");if(count(in.facts.get("realRows"))==0&&count(in.facts.get("activeCertificates"))==0){r.put("status","NOT_DEPLOYED");r.put("missingReason","当前没有原REAL行且没有有效ACTIVE证书；FUTURE_ACT_RATE与正式REAL频率分列");}break;
            }
            case "Q03":source(in,r,"qualification","certificates");break;
            case "Q04":source(in,r,"qualification");r.put("missingReason","需事前固定块规则及逐策略同角色日历块；不从本次展示子集事后挑好窗口");break;
            case "Q05":source(in,r,"calibration");break;
            case "Q06":source(in,r,"evolution","confirmation","ablation");break;
            case "Q07": {
                Number n=number(in.facts.get("activeCertificates"));if(n!=null)observed(r,n,n,in.facts.get("qualifiedExperiments"),count(in.facts.get("qualifiedExperiments")),"CURRENT_R27_GATE_V1_ACTIVE_CERTIFICATES","RECORDED_UNVALIDATED",n.longValue()==0?"当前有效证书为0；不隐去负结果、不补配额，Q01/03/04/05逐资产独立验收":"当前原门禁证书计数；Q01/03/04/05逐资产质量还需独立原件");source(in,r,"certificates");break;
            }
            default:break; // Device, migration and export receipts are unavailable in the database read view.
        }
    }
    static void writer(ReportR27.R30Input in,Map<String,Object> r){
        double max=Double.NEGATIVE_INFINITY;long n=0,failures=0;boolean knownFailures=true,openUpper=false;List<Object> windows=new ArrayList<>();
        for(Object x:rows(in,"writerWindows")){Map<String,Object> row=map(x);if(count(row.get("critical"))!=1)continue;windows.add(row);Number wait=number(row.get("wait_p95"));if(wait!=null){if(wait.longValue()==Long.MAX_VALUE)openUpper=true;else max=Math.max(max,wait.doubleValue());}Object raw=row.get("histogram_json");if(raw instanceof String)try{Map<String,Object> hist=map(JsonR27.parse((String)raw));Object buckets=hist.get("waitCounts");if(buckets instanceof List){long total=0,before=0;List<?> b=(List<?>)buckets;for(int i=0;i<b.size();i++){long v=count(b.get(i));if(v>=0){total+=v;if(i<b.size()-1)before+=v;}}if(total>0&&Math.ceil(total*0.95)>before)openUpper=true;}}catch(RuntimeException unreadable){r.put("histogramReadStatus","UNKNOWN_MALFORMED_STORED_HISTOGRAM");}long c=count(row.get("count"));if(c>=0)n+=c;long f=count(row.get("failures"));if(f<0)knownFailures=false;else failures+=f;}
        r.put("observations",windows);r.put("windowDefinition","captured critical writer windows; bounded display with total/omitted index");
        if(Double.isFinite(max)&&n>0)observed(r,max,knownFailures?failures:null,n,n,"CRITICAL_WRITER_WAIT_MS",max>200||knownFailures&&failures>0?"FAIL":"RECORDED_UNVALIDATED",knownFailures?"窗口最大wait_p95及真实失败数；完整24h/SLO覆盖另验":"旧窗口缺failures，不能把缺失写成0，不能宣称24h失败0");
        if(openUpper){observed(r,null,knownFailures?failures:null,n,n,"CRITICAL_WRITER_WAIT_MS","FAIL","P95落在>30000ms开口桶；保持真实失败，以区间表达，不显示Long.MAX_VALUE为实测耗时");r.put("lowerExclusiveMs",30000L);r.put("upperInclusiveMs",null);}
    }
    static void timeline(ReportR27.R30Input in,String id,Map<String,Object> r){
        List<IssueTimelineR30.Row> live=new ArrayList<>();long untyped=0;
        java.util.Set<String> instances=new java.util.HashSet<>();long duplicates=0,missingDurable=0,pendingTargets=0,eventConflicts=0;
        for(Object x:rows(in,"timeline")){Map<String,Object> row=map(x);if(!"LIVE".equals(row.get("event_role"))){untyped++;continue;}
            if(row.get("event_kind")!=null&&!java.util.Objects.equals(row.get("event_kind"),id.equals("M04")?"DRAW":"PREDICTION"))continue;
            if("EVENT_IDENTITY_CONFLICT".equals(row.get("outcome"))){eventConflicts++;continue;}
            if(id.equals("M05")&&row.get("event_kind")!=null){Number deadline=number(row.get("wall_next_deadline")),at=number(in.watermark.get("factsReadAt"));if(deadline!=null&&at!=null&&deadline.doubleValue()>at.doubleValue()){pendingTargets++;continue;}if("UNKNOWN_NO_PERSISTED_POSTCOMMIT_OBSERVATION".equals(row.get("durableObservationState")))missingDurable++;}
            if(row.get("event_id") instanceof String&&!instances.add((String)row.get("event_id"))){duplicates++;continue;}
            IssueTimelineR30.Row value=new IssueTimelineR30.Row(String.valueOf(row.get("issue")));
            for(IssueTimelineR30.Stage stage:IssueTimelineR30.Stage.values()){String k=stage.name().toLowerCase(java.util.Locale.ROOT);Number wall=number(row.get("wall_"+k)),mono=number(row.get("mono_"+k));Object boot=row.get("boot_"+k);value.set(stage,wall==null?null:wall.longValue(),mono==null?null:mono.longValue(),boot instanceof String?(String)boot:null);}live.add(value);
        }
        long conflictingDeadlines=0;if(id.equals("M05")){Map<String,IssueTimelineR30.Row> opportunities=new LinkedHashMap<>();for(IssueTimelineR30.Row row:live){IssueTimelineR30.Row first=opportunities.get(row.issue);if(first==null){opportunities.put(row.issue,row);continue;}Long a=first.wall(IssueTimelineR30.Stage.NEXT_DEADLINE),b=row.wall(IssueTimelineR30.Stage.NEXT_DEADLINE);if(a!=null&&b!=null&&!a.equals(b))conflictingDeadlines++;Long frozen=first.wall(IssueTimelineR30.Stage.NEXT_FROZEN),next=row.wall(IssueTimelineR30.Stage.NEXT_FROZEN);if(a==null&&b!=null||next!=null&&(frozen==null||next<frozen))opportunities.put(row.issue,row);}r.put("predictionAttempts",live.size());live=new ArrayList<>(opportunities.values());r.put("independentTargetIssues",live.size());r.put("conflictingDeadlineTargets",conflictingDeadlines);}
        Map<String,Object> summary=IssueTimelineR30.summarize(live);r.put("observations",summary);r.put("legacyOrNonLiveRowsExcluded",untyped);r.put("duplicateEventInstancesExcluded",duplicates);r.put("windowDefinition","captured LIVE event instances started within the declared window; same-boot monotonic duration ends after actual COMMIT return; missing durable observations and full calendar denominator separate");
        if(id.equals("M05")){Map<String,Object> s=map(summary.get("M05"));Number v=number(s.get("coverage"));if(v!=null)observed(r,v,s.get("frozenAtLeast5sBefore"),s.get("periodsWithDeadline"),count(s.get("periodsWithDeadline")),"LIVE_WITH_DECLARED_DEADLINE",v.doubleValue()<0.995?"FAIL":"RECORDED_UNVALIDATED","LIVE已记录截止期窗口；漏记事件的完整日历分母和受支持设备场景仍需核对");}
        else {Map<String,Object> a=map(summary.get(id.equals("M03")?"request_to_frozen":"observe_to_settled"));Number v=number(a.get("p95"));if(v!=null){Number other=id.equals("M03")?number(map(summary.get("settled_to_frozen")).get("p95")):v;double max=other==null?v.doubleValue():Math.max(v.doubleValue(),other.doubleValue());observed(r,max,null,a.get("n"),count(a.get("n")),"SAME_BOOT_LIVE_EVENT_INSTANCES",max>(id.equals("M03")?5000:1000)?"FAIL":"RECORDED_UNVALIDATED","原分段P95；缺阶段与跨重启数量分别保留，不以墙钟替代单调钟");}}
        if(conflictingDeadlines>0)observed(r,null,null,null,live.size(),"LIVE_DISTINCT_TARGET_ISSUES","DATA_CONFLICT","同一目标的已登记截止冲突；不挑较有利截止，不签收M05");
        if(id.equals("M05")){r.put("pendingTargetsExcluded",pendingTargets);r.put("missingDurableObservationTargets",missingDurable);if(conflictingDeadlines==0&&missingDurable>0){r.put("coverageLowerBound",r.get("value"));r.put("value",null);r.put("status","UNKNOWN_DURABLE_OBSERVATION_MISSING");r.put("missingReason","缺少已持久化的提交后时刻，无法区分准时与超时；不以0或成功替代，已记录迟到和下界仍保留");}}
        r.put("eventIdentityConflicts",eventConflicts);if(eventConflicts>0&&!"FAIL".equals(r.get("status"))){r.put("status","DATA_CONFLICT");r.put("missingReason","事件身份冲突已隔离；不借其他实例填补阶段，保持真实冲突等待核对");}
    }
    // ---------------------------------------------------------------- R31-B11-C01
    /** One measurement receipt (hashed); a missing source has no receipt at all. */
    public static final class MeasurementR31 {
        public final String receiptHash, role; public final long samples; public final Double value; public final boolean met;
        public MeasurementR31(String receiptHash, String role, long samples, Double value, boolean met) { this.receiptHash = receiptHash; this.role = role; this.samples = samples; this.value = value; this.met = met; }
    }
    /** Evaluates one R31 definition: missing source = UNKNOWN (no value), zero/too few samples = INSUFFICIENT, failure = FAIL,
     * success = PASS. Quality metrics only consume natural future-role receipts; value and coverage are separate fields. */
    public static Map<String,Object> evaluateR31(Map<String,Object> def, MeasurementR31 m) {
        Map<String,Object> r = new LinkedHashMap<>(); String id = String.valueOf(def.get("id")); long min = ((Number) def.get("minimumSample")).longValue();
        r.put("id", id); r.put("version", def.get("version")); r.put("denominator", def.get("denominator")); r.put("minimumSample", min);
        String status; Object value = null; Object coverage = null;
        if (m == null || m.receiptHash == null || !m.receiptHash.matches("[0-9a-f]{64}")) status = "UNKNOWN_SOURCE_MISSING";
        else if ("QUALITY".equals(def.get("kind")) && !"NATURAL_FUTURE".equals(m.role)) status = "NOT_PROVEN_NOT_NATURAL_ROLE";
        else if (m.samples <= 0) { status = "INSUFFICIENT_ZERO_SAMPLES"; coverage = 0.0; }
        else { coverage = Math.min(1.0, m.samples / (double) min); value = m.value; status = m.samples < min ? "INSUFFICIENT_BELOW_MINIMUM_SAMPLE" : m.met ? "PASS" : "FAIL"; }
        r.put("status", status); r.put("value", value); r.put("coverage", coverage); r.put("samples", m == null ? null : (Object) m.samples); r.put("receiptHash", m == null ? null : m.receiptHash);
        return r;
    }
    /** The same row, rendered for the HTML report; the JSON and HTML carry identical id/value/status/denominator. */
    public static String htmlRowR31(Map<String,Object> row) {
        StringBuilder b = new StringBuilder("<tr");
        for (String k : new String[]{"id", "status", "value", "denominator"}) b.append(" data-").append(k).append("=\"").append(escapeR31(String.valueOf(row.get(k)))).append('"');
        return b.append("><td>").append(escapeR31(String.valueOf(row.get("id")))).append("</td><td>").append(escapeR31(String.valueOf(row.get("status")))).append("</td><td>").append(escapeR31(String.valueOf(row.get("value")))).append("</td></tr>").toString();
    }
    static String escapeR31(String s) { return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;"); }
    /** M32: one cross-source observation of the same business fact. */
    public static final class SourceValueR31 {
        public final String source, owner, role, scope, watermark; public final long capturedAt; public final String value;
        public SourceValueR31(String source, String owner, String role, String scope, long capturedAt, String watermark, String value) { this.source = source; this.owner = owner; this.role = role; this.scope = scope; this.capturedAt = capturedAt; this.watermark = watermark; this.value = value; }
    }
    /** Values are compared only within the same scope and watermark; different watermarks or scopes are a reported
     * STALE_OR_SCOPE_CONFLICT with every source listed, never a numeric contradiction and never a claim that settlement stopped. */
    public static Map<String,Object> crossSourceR31(List<SourceValueR31> values) {
        Map<String,Object> r = new LinkedHashMap<>(); List<Object> conflicts = new ArrayList<>(), scopes = new ArrayList<>(); Map<String, SourceValueR31> byKey = new LinkedHashMap<>();
        for (SourceValueR31 v : values) {
            for (String f : new String[]{v.source, v.owner, v.role, v.scope, v.watermark}) if (f == null || f.isEmpty()) throw new IllegalArgumentException("R31_M32_SOURCE_FIELDS_REQUIRED:" + v.source);
            String key = v.scope + "@" + v.watermark; SourceValueR31 first = byKey.putIfAbsent(key, v);
            if (first != null && !first.value.equals(v.value)) conflicts.add(first.source + "=" + first.value + " vs " + v.source + "=" + v.value + " @" + key);
        }
        java.util.Set<String> watermarks = new java.util.TreeSet<>(); for (SourceValueR31 v : values) { watermarks.add(v.scope + "@" + v.watermark); Map<String,Object> x = new LinkedHashMap<>(); x.put("source", v.source); x.put("owner", v.owner); x.put("role", v.role); x.put("scope", v.scope); x.put("capturedAt", v.capturedAt); x.put("watermark", v.watermark); x.put("value", v.value); scopes.add(x); }
        r.put("status", !conflicts.isEmpty() ? "VALUE_CONFLICT_SAME_SCOPE_AND_WATERMARK" : watermarks.size() > 1 ? "STALE_OR_SCOPE_CONFLICT" : "CONSISTENT");
        r.put("conflicts", conflicts); r.put("sources", scopes); r.put("settlementStoppedInferred", false); return r;
    }
}
