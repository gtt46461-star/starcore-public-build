package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Two pure chart series derived from the very same immutable input used by the twelve report chapters. */
public final class ReportChartsR30 {
    private ReportChartsR30(){}
    public static final String[] IDS={"p15_paired_improvement_r30","p16_capability_coverage_r30"};
    static final String[] COMMON={"futurePeriods","futureActs","futureHits","independentFutureIssues","qualifiedExperiments","activeCertificates","evolutionAttempts","policyObjects"};
    static Double finite(Object value){if(!(value instanceof Number))return null;double d=((Number)value).doubleValue();return Double.isFinite(d)?d:null;}
    static Map<String,Object> point(String label,Object value,String detail){Map<String,Object> p=new LinkedHashMap<>();p.put("label",label);p.put("value",value);p.put("lower",null);p.put("upper",null);p.put("n",-1L);p.put("details",detail);return p;}
    static Map<String,Object> panel(ReportR27.R30Input in,String id,String title,String kind,String source,String note,List<Object> points){
        Map<String,Object> m=new LinkedHashMap<>();m.put("id",id);m.put("title",title);m.put("kind",kind);m.put("source",source);m.put("window","报告快照期号 "+in.watermark.get("latestIssue"));m.put("note",note);m.put("points",points);
        m.put("status",points.isEmpty()?"INSUFFICIENT":"READY");m.put("snapshotWatermark",in.watermark);Map<String,Object> common=new LinkedHashMap<>();for(String key:COMMON)common.put(key,in.facts.get(key));m.put("commonMetrics",common);m.put("metricResults",in.metricResults);m.put("metricRegistryVersion",MetricDefinitionsR29.CURRENT_VERSION);return m;
    }
    @SuppressWarnings("unchecked") public static Map<String,Object> evolution(ReportR27.R30Input in){
        List<Object> points=new ArrayList<>();Set<String> tasks=new HashSet<>();Object raw=in.data.get("evolution");int drawn=0,conflicts=0,reused=0,verified=0;
        if(raw instanceof List){List<?> rows=new ArrayList<>((List<?>)raw);Collections.reverse(rows);
            for(Object item:rows){if(!(item instanceof Map))continue;Map<?,?> row=(Map<?,?>)item;if(!"MAIN".equals(row.get("loop")))continue;
                String generation=String.valueOf(row.get("generation")),state="INSUFFICIENT_CONFIRMATION",task="";Double lower=finite(row.get("paired_lcb")),value=null;Object stored=row.get("detail_json");Map<?,?> holdout=null;
                if(stored instanceof String)try{Object detail=JsonR27.parse((String)stored);Object h=detail instanceof Map?((Map<?,?>)detail).get("holdout"):null;if(h instanceof Map)holdout=(Map<?,?>)h;}catch(RuntimeException malformed){state="UNKNOWN_UNREADABLE_GENERATION_DETAIL";}
                if(holdout!=null){
                    Object key=holdout.get("taskKey");task=key instanceof String?(String)key:"";Double original=finite(holdout.get("pairedLcbVsPrevious"));
                    boolean sameGeneration=!holdout.containsKey("sourceGeneration")||Objects.equals(String.valueOf(holdout.get("sourceGeneration")),generation);
                    if(!sameGeneration||!task.isEmpty()&&tasks.contains(task)){state="REUSED_FROZEN_EVIDENCE_NOT_NEW_IMPROVEMENT";reused++;}
                    else if(!Boolean.TRUE.equals(holdout.get("confirmationOutsideEvolution")))state="LEGACY_IN_LOOP_OR_UNKNOWN_CONFIRMATION";
                    else if(!Objects.equals(row.get("dataset_hash"),holdout.get("datasetHash"))) {state="DATA_CONFLICT_DATASET";conflicts++;}
                    else if("COMPLETED".equals(holdout.get("status"))&&!task.isEmpty()&&lower!=null&&original!=null){
                        boolean marked=row.get("verified") instanceof Number&&((Number)row.get("verified")).intValue()==1;
                        if(lower.doubleValue()!=original.doubleValue()||marked!=Boolean.TRUE.equals(holdout.get("verified"))||marked&&lower<=0){state="DATA_CONFLICT_CONFIRMATION";conflicts++;}
                        else{tasks.add(task);value=lower;drawn++;if(marked)verified++;state=marked?"VERIFIED_INDEPENDENT_CONFIRMATION":"RECORDED_UNVERIFIED_ALL_CONTROLS";}
                    }
                }
                Map<String,Object> p=point("G"+generation,value,state+"；单侧95%父子配对收益下界="+value+"；确认任务="+task+"；配对样本数未单列，保持未知；非自然ACT达标证明");p.put("confirmationState",state);p.put("taskKey",task);points.add(p);
            }
        }
        Map<String,Object> out=panel(in,IDS[0],"代际提升：独立确认的父子配对收益下界","LINE","r27_generation.paired_lcb + immutable confirmation detail","只画原独立确认回写且口径一致的下界；未确认、旧循环、重复冻结证据留空，不补零。正下界也不代替全部对照或自然质量验收。",points);
        out.put("plottedGenerations",drawn);out.put("verifiedIndependentImprovements",verified);out.put("reusedEvidenceNotCounted",reused);out.put("sourceConflicts",conflicts);out.put("refs",Collections.singletonList(0.0));out.put("pairedSampleCount",null);
        out.put("sourceRecordsAvailable",raw instanceof List);
        if(!(raw instanceof List)){out.put("plottedGenerations",null);out.put("verifiedIndependentImprovements",null);out.put("sourceConflicts",null);out.put("status","UNKNOWN_SOURCE_MISSING");}
        else if(drawn==0){out.put("unplottedRecords",new ArrayList<>(points));points.clear();out.put("status",conflicts>0?"DATA_CONFLICT":"INSUFFICIENT");}out.put("collectionAction","FrozenConfirmationStoreR30.generationReport：采集当前代/数据集/原taskKey的完成确认回执与父子配对原始证据，不重复计旧确认");return out;
    }
    public static Map<String,Object> capabilities(ReportR27.R30Input in){
        LinkedHashMap<String,long[]> groups=new LinkedHashMap<>();for(String kind:new String[]{"FAMILY","EXPERT","IDEA","MODEL","MATRIX"})groups.put(kind,new long[3]);
        Object raw=in.data.get("capabilities");int rows=0,unknown=0;List<String> blocked=new ArrayList<>();
        if(raw instanceof List)for(Object item:(List<?>)raw){if(!(item instanceof Map)){unknown++;continue;}Map<?,?> row=(Map<?,?>)item;long[] counts=groups.get(row.get("kind"));if(counts==null||!(row.get("implemented") instanceof Boolean)||!(row.get("executable") instanceof Boolean)){unknown++;continue;}
            boolean implemented=Boolean.TRUE.equals(row.get("implemented")),executable=Boolean.TRUE.equals(row.get("executable"));if(executable&&!implemented){unknown++;continue;}rows++;counts[!implemented?2:executable?0:1]++;
            if(!executable)blocked.add(row.get("id")+":"+row.get("executionBlockedReason"));
        }
        List<Object> points=new ArrayList<>();if(raw instanceof List&&!((List<?>)raw).isEmpty())for(Map.Entry<String,long[]> entry:groups.entrySet()){
            long[] c=entry.getValue();Map<String,Object> p=point(entry.getKey(),c[0]+c[1]+c[2],"代码入口接线对象数；可执行/受阻/未找到实现互斥；不是独立样本，统计有效数量UNKNOWN");p.put("stack",Arrays.asList(c[0],c[1],c[2]));points.add(p);
        }
        Map<String,Object> out=panel(in,IDS[1],"能力覆盖：代码入口与阻塞分列（有效数量未知）","STACK","same captured capability registry","可执行只表示当前代码入口与已知绑定条件成立；运行产出、模型数据依赖和统计有效证据分别验收。未将未验证画成有效0。",points);
        out.put("series",Arrays.asList("代码可执行","有实现但受阻","未找到实现"));out.put("registeredObjects",in.facts.get("registeredCapabilities"));out.put("classifiedObjects",rows);out.put("unknownObjects",unknown);out.put("blocked",blocked);out.put("validatedEffectiveCount",null);out.put("effectiveStatus","UNKNOWN_NOT_VALIDATED");out.put("collectionAction","CapabilityRegistryR30.all：对照原82对象与实际生产者/消费者；运行产出、模型数据依赖和有效证据另行采集，不以代码覆盖代替质量");
        Object total=in.facts.get("registeredCapabilities");if(!(total instanceof Number)||((Number)total).longValue()!=rows||unknown>0)out.put("status","UNKNOWN_INCOMPLETE_REGISTRY");return out;
    }
    public static List<Map<String,Object>> panels(ReportR27.R30Input in){return Arrays.asList(evolution(in),capabilities(in));}
    public static Map<String,Object> packet(ReportR27.R30Input in){List<Map<String,Object>> panels=panels(in);Map<String,Object> out=new LinkedHashMap<>();out.put("version","R30_CHART_SNAPSHOT_1");out.put("snapshotWatermark",in.watermark);out.put("commonMetrics",panels.get(0).get("commonMetrics"));out.put("panels",panels);out.put("researchWorkbench",in.data.get("objectIndex") instanceof Map?((Map<?,?>)in.data.get("objectIndex")).get("researchCharts"):null);out.put("researchFrameIndex",in.data.get("repairFrameIndex"));out.put("chartTotalContract",47);out.put("mountedViewLimit",40);return out;}
}
