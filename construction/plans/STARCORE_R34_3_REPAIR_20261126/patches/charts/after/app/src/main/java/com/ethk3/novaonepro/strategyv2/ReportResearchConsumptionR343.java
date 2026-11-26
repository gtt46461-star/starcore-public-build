package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import java.util.*;

/** Read-only, bounded original-receipt validation. Never writes receipts or authorizes REAL.
 * BORN receipts and non-BORN actions retain separate denominators. A retained receipt is
 * evidence of its original transaction, not proof of a current object state or quality. */
final class ReportResearchConsumptionR343 {
    private ReportResearchConsumptionR343() {}
    static final int MAX_ROWS=512, MAX_CELL_CHARS=262144, MAX_TOTAL_CHARS=2*1024*1024;
    static Map<String,Object> map(){return new LinkedHashMap<>();}
    @SuppressWarnings("unchecked") static Map<String,Object> object(Object value){
        if(!(value instanceof Map))throw new IllegalArgumentException("RECEIPT_OBJECT");
        return (Map<String,Object>)value;
    }
    static String str(Map<?,?> m,String key){Object v=m.get(key);if(!(v instanceof String)||((String)v).isEmpty())throw new IllegalArgumentException("RECEIPT_STRING:"+key);return (String)v;}
    static long num(Map<?,?> m,String key){
        Object v=m.get(key);if(v instanceof Byte||v instanceof Short||v instanceof Integer||v instanceof Long)return ((Number)v).longValue();
        if(v instanceof Float||v instanceof Double){double d=((Number)v).doubleValue();
            if(Double.isFinite(d)&&d>=-0x1.0p63&&d<0x1.0p63&&d==Math.rint(d))return (long)d;}
        throw new IllegalArgumentException("RECEIPT_EXACT_INTEGER:"+key);
    }
    static void require(boolean ok,String code){if(!ok)throw new IllegalArgumentException(code);}
    static String reason(RuntimeException error){String text=error.getClass().getSimpleName()+":"+error.getMessage();return text.length()<=512?text:text.substring(0,512);}
    static Map<String,Object> json(Map<?,?> row,String key){return object(JsonR27.parse(str(row,key)));}
    static void equal(Object a,Object b,String code){require(Objects.equals(a,b),code);}
    static String bounded(String expression,String alias){return "CASE WHEN length("+expression+")<="+MAX_CELL_CHARS+" THEN "+expression+" ELSE NULL END AS "+alias;}
    static String viewKeys(){
        // The nested projection bounds the value before CursorWindow sees it, even with malformed source rows.
        return "(SELECT group_concat(bounded_key,'|') FROM (SELECT DISTINCT CASE WHEN length(v.measure_key)<=96 THEN v.measure_key ELSE '#OVERSIZE_VIEW_KEY#' END AS bounded_key"
            +" FROM r30r1_research_measure v WHERE v.run_id=r.run_id AND (v.measure_key GLOB 'R[0-9][0-9]' OR v.measure_key LIKE 'MATRIX:%')"
            +" ORDER BY bounded_key LIMIT 65)) AS original_views";
    }
    static boolean viewKeysPartial(Map<?,?> row){Object raw=row.get("original_views");if(!(raw instanceof String))return false;
        String keys=(String)raw;return keys.contains("#OVERSIZE_VIEW_KEY#")||keys.split("\\|",-1).length>64;}

    static String oversize(String... columns){List<String> parts=new ArrayList<>();for(String c:columns)parts.add("coalesce(length("+c+"),0)>"+MAX_CELL_CHARS);return "("+String.join(" OR ",parts)+") AS oversized";}

    static final String BORN_SQL="SELECT m.run_id AS run,"+bounded("m.payload_json","payload")
        +",r.source_commit AS source,r.dataset_hash AS dataset,r.source_revision AS revision,r.role AS role,"
        +bounded("r.summary_json","original")+",c.proposal_id AS proposal,c.child_policy_hash AS child,c.source_generation AS generation,"
        +"c.consumer_version AS consumer,c.receipt_hash AS receipt,p.state AS proposal_state,"
        +bounded("p.payload_json","proposal_payload")+",p.payload_hash AS proposal_hash,"
        +bounded("s.spec_json","policy_spec")+",s.dataset_hash AS policy_dataset,s.birth_generation AS policy_generation,"
        +viewKeys()+","+oversize("m.payload_json","r.summary_json","p.payload_json","s.spec_json")
        +" FROM r30r1_research_measure m JOIN r30r1_analysis_run r ON r.run_id=m.run_id"
        +" LEFT JOIN r30r1_consume c ON m.measure_key='CONSUMED_BY:'||c.proposal_id AND m.parent_hash=c.child_policy_hash"
        +" LEFT JOIN r30r1_proposal p ON p.proposal_id=c.proposal_id"
        +" LEFT JOIN r27_policy s ON s.policy_hash=c.child_policy_hash"
        +" WHERE m.kind='RESEARCH_SOURCE_CONSUMPTION' AND r.source_commit=? AND r.status='COMMITTED'"
        +" ORDER BY r.recorded_at DESC,m.run_id,m.measure_key LIMIT "+(MAX_ROWS+1);
    static final String ACTION_SQL="SELECT a.consume_key AS action_key,a.source_run AS run,a.view_id AS view,a.action_kind AS kind,"
        +"a.role AS role,a.dataset_hash AS dataset,a.source_revision AS revision,a.source_commit AS source,a.source_abi AS abi,"
        +"a.input_hash AS input,a.product_hash AS product,a.before_hash AS before_hash,a.after_hash AS after_hash,"
        +"a.object_key AS object_key,a.action_budget AS budget,a.action_ordinal AS decision_ordinal,a.latest_label_ordinal AS latest_label,"
        +bounded("a.applied_json","applied")+",a.applied_at AS applied_at,"+bounded("r.summary_json","original")+","+viewKeys()+","
        +oversize("a.applied_json","r.summary_json")
        +" FROM r33_research_applied a JOIN r30r1_analysis_run r ON r.run_id=a.source_run AND r.source_commit=a.source_commit"
        +" AND r.dataset_hash=a.dataset_hash AND r.source_revision=a.source_revision AND r.role=a.role"
        +" WHERE a.source_commit=? AND r.status='COMMITTED' ORDER BY a.applied_at DESC,a.consume_key LIMIT "+(MAX_ROWS+1);

    static Map<String,Object> capture(SQLiteDatabase db){
        Map<String,Object> out=map();out.put("schema","R343_CURRENT_SOURCE_ORIGINAL_CONSUMPTION_1");out.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);
        out.put("scope","CURRENT_SOURCE_RETAINED_ORIGINAL_RECEIPTS; BORN_AND_NON_BORN_SEPARATE; NO_EFFECTIVENESS_OR_REAL_AUTHORITY");
        out.put("maximumRowsPerKind",MAX_ROWS);out.put("maximumCellChars",MAX_CELL_CHARS);out.put("maximumTotalCharsPerKind",MAX_TOTAL_CHARS);
        out.put("born",read(db,true));out.put("nonBorn",read(db,false));return out;
    }
    private static Map<String,Object> read(SQLiteDatabase db,boolean born){
        Map<String,Object> out=map();Map<String,Long> counts=new LinkedHashMap<>();
        for(ResearchGraphSpecsR30R1.Spec view:ResearchGraphSpecsR30R1.ALL)counts.put(view.id,0L);
        int scanned=0,invalid=0,oversized=0,chars=0,otherBorn=0;boolean complete=true;Set<String> seen=new HashSet<>();List<Object> errors=new ArrayList<>();
        try(Cursor c=db.rawQuery(born?BORN_SQL:ACTION_SQL,new String[]{BuildConfig.SOURCE_COMMIT_R26})){
            while(c.moveToNext()){
                if(scanned==MAX_ROWS){complete=false;out.put("limitReason","ROW_BOUND");break;}
                scanned++;Map<String,Object> row=map();for(int i=0;i<c.getColumnCount();i++){
                    Object value=c.isNull(i)?null:c.getType(i)==Cursor.FIELD_TYPE_INTEGER?c.getLong(i):c.getString(i);row.put(c.getColumnName(i),value);
                    if(value instanceof String)chars+=((String)value).length();
                }
                if(chars>MAX_TOTAL_CHARS){complete=false;out.put("limitReason","CHAR_BOUND");break;}
                if(num(row,"oversized")!=0||viewKeysPartial(row)){oversized++;complete=false;continue;}
                try{
                    // Typed generation-rule receipts have their own owner and no R01-R18 source-view contract.
                    // Retain their raw count separately; never attribute them to an arbitrary chart.
                    if(born&&json(row,"payload").containsKey("typedRule")){otherBorn++;continue;}
                    String view=born?validateBorn(row):validateAction(row);
                    String key=born?str(object(json(row,"payload").get("resultContractR31")),"consumeKey"):str(row,"action_key");
                    if(seen.add(key))counts.put(view,counts.getOrDefault(view,0L)+1L);
                }catch(RuntimeException rejected){invalid++;if(errors.size()<8){Map<String,Object> e=map();e.put("run",row.get("run"));e.put("reason",reason(rejected));errors.add(e);}}
            }
            out.put("status",complete?(invalid==0?"READ_OK":"INVALID_OR_UNVALIDATED_RECEIPTS_PRESENT"):"PARTIAL_VALIDATION");
        }catch(RuntimeException unavailable){complete=false;out.put("status","UNKNOWN_SOURCE_UNAVAILABLE");out.put("reason",reason(unavailable));}
        out.put("complete",complete);out.put("scannedRows",scanned);out.put("invalidOrUnvalidatedRows",invalid);out.put("oversizedRows",oversized);out.put("rejections",errors);
        out.put("validatedReceiptLowerBoundsByView",counts);out.put("exactValidatedReceiptsByView",complete?counts:null);
        out.put("unvalidatedTypedRuleRowsOutsideChartViews",otherBorn);out.put("qualityEffective",null);out.put("originalExecutionReplayed",false);return out;
    }
    static Map<String,Object> contract(Map<String,Object> row){
        equal(BuildConfig.SOURCE_COMMIT_R26,row.get("source"),"SOURCE_COMMIT");
        Map<String,Object> root=json(row,"original"),contract=object(root.get("sourceContract"));
        equal(row.get("source"),contract.get("source"),"CONTRACT_COMMIT");equal(row.get("dataset"),contract.get("dataset"),"CONTRACT_DATASET");
        require(num(row,"revision")==num(contract,"revision"),"CONTRACT_REVISION");equal(row.get("role"),contract.get("role"),"CONTRACT_ROLE");
        equal(FrozenConfirmationR30.currentSemantics(),contract.get("abi"),"CONTRACT_ABI");return contract;
    }
    static void originalView(Map<String,Object> row,String view){
        require(Arrays.asList(str(row,"original_views").split("\\|",-1)).contains(view),"ORIGINAL_VIEW_MISSING");
        require(ResearchResultContractR31.consumptionKindOf(view)!=ResearchResultContractR31.ConsumptionKind.FORMAL_READ_ONLY,"FORMAL_VIEW_READ_ONLY");
    }
    static String validateBorn(Map<String,Object> row){
        Map<String,Object> contract=contract(row),link=json(row,"payload"),typed=object(link.get("resultContractR31"));
        require(Boolean.TRUE.equals(typed.get("validated")),"BORN_TYPED_RECEIPT_UNVALIDATED");
        equal("ACTUAL_BORN_CONSUMPTION_NOT_EFFECTIVENESS",typed.get("state"),"BORN_TYPED_STATE");
        equal("ACTUAL_ORIGINAL_BORN_CHILD_SAME_TRANSACTION_NOT_QUALITY_PROOF",link.get("scope"),"BORN_SCOPE");
        String view=str(typed,"view"),proposal=str(row,"proposal"),child=str(row,"child"),consumer=str(row,"consumer");
        originalView(row,view);equal("SELECT",row.get("role"),"BORN_SELECT_ROLE");
        equal(proposal,link.get("proposalId"),"PROPOSAL_JOIN");equal(child,link.get("childPolicyHash"),"CHILD_JOIN");equal(consumer,link.get("consumer"),"CONSUMER_JOIN");
        require(num(row,"generation")==num(link,"generation"),"GENERATION_JOIN");
        String receipt=StatsR27.sha256(proposal+'|'+child+'|'+num(row,"generation")+'|'+consumer);
        equal(receipt,row.get("receipt"),"BORN_RECEIPT_HASH");equal(receipt,link.get("receiptHash"),"BORN_LINK_HASH");
        equal("CONSUMED",row.get("proposal_state"),"PROPOSAL_NOT_CONSUMED");equal(StatsR27.sha256(str(row,"proposal_payload")),row.get("proposal_hash"),"PROPOSAL_HASH");
        equal(child,json(row,"proposal_payload").get("candidateHash"),"PROPOSAL_CANDIDATE_HASH");
        PolicySpecR27 policy=PolicySpecR27.parse(str(row,"policy_spec"));equal(child,policy.policyHash(),"POLICY_HASH");
        equal(row.get("dataset"),row.get("policy_dataset"),"POLICY_DATASET");require(num(row,"generation")==num(row,"policy_generation"),"POLICY_GENERATION");
        Map<String,Object> receipts=object(typed.get("receipts"));equal(row.get("run"),receipts.get("source_run"),"SOURCE_RUN_JOIN");
        equal(StatsR27.sha256(str(row,"original")),receipts.get("product_hash"),"SOURCE_ORIGINAL_HASH");equal(contract.get("input"),receipts.get("input_hash"),"SOURCE_INPUT_HASH");
        equal(child,receipts.get("after_hash"),"AFTER_CHILD_HASH");equal(ResearchConsumptionStoreR31.VERSION+":"+consumer,receipts.get("consumer_version"),"TYPED_CONSUMER_VERSION");
        String before=str(receipts,"before_hash");equal(policy.meta!=null&&!policy.meta.parents.isEmpty()?policy.meta.parents.get(0):StatsR27.sha256("R31_ABSENT_CANDIDATE_BEFORE_BIRTH|"+child),before,"ORIGINAL_PARENT_HASH");
        equal("POSITION:"+policy.position,typed.get("target"),"TARGET_IDENTITY");equal(row.get("dataset"),typed.get("datasetHash"),"TYPED_DATASET");
        require(num(typed,"latestLabelOrdinal")==num(contract,"through"),"LABEL_ORDINAL");
        ResearchResultContractR31.Result result=new ResearchResultContractR31.Result(view,ResearchResultContractR31.ProductState.PRODUCED,
            str(row,"dataset"),num(row,"revision"),str(contract,"abi"),str(row,"role"),str(typed,"target"),before,num(contract,"through"),receipts);
        ResearchResultContractR31.Expect expected=new ResearchResultContractR31.Expect(str(row,"dataset"),num(row,"revision"),str(contract,"abi"),str(typed,"target"),before,num(typed,"decisionOrdinal"));
        equal(ResearchResultContractR31.validateForConsumption(result,expected,consumer,child,Collections.emptySet()),typed.get("consumeKey"),"TYPED_CONSUME_KEY");
        return view;
    }
    static String validateAction(Map<String,Object> row){
        Map<String,Object> contract=contract(row);String view=str(row,"view"),run=str(row,"run"),kind=str(row,"kind");originalView(row,view);
        require(Arrays.asList("BUDGET_APPLIED","DUPLICATE_REJECTED","REPLAY_PLAN_CREATED","MECHANISM_SKIPPED").contains(kind),"NON_BORN_ACTION_KIND");
        equal(StatsR27.sha256(str(row,"original")),row.get("product"),"ACTION_ORIGINAL_HASH");equal(StatsR27.sha256(str(row,"applied")),row.get("after_hash"),"ACTION_APPLIED_HASH");
        require(!str(row,"before_hash").equals(str(row,"after_hash"))&&num(row,"budget")>0,"ACTION_CHANGE_AND_BUDGET");
        ResearchActionPolicyR33.hash(str(row,"before_hash"));ResearchActionPolicyR33.hash(str(row,"after_hash"));
        equal(contract.get("input"),row.get("input"),"ACTION_INPUT");equal(contract.get("abi"),row.get("abi"),"ACTION_ABI");require(num(contract,"through")==num(row,"latest_label"),"ACTION_LABEL_ORDINAL");
        Map<String,Object> original=json(row,"original");equal(view.startsWith("MATRIX:")?row.get("input"):view,original.get(view.startsWith("MATRIX:")?"matrixHash":"viewId"),"ACTION_VIEW_ORIGINAL");
        equal(row.get("input"),original.get("inputHash"),"ACTION_ORIGINAL_INPUT");
        ResearchActionPolicyR33.Source source=new ResearchActionPolicyR33.Source(view,run,str(row,"dataset"),str(row,"abi"),str(row,"source"),str(row,"role"),str(row,"input"),str(row,"product"),num(row,"revision"),num(row,"latest_label"),num(contract,"available"));
        source.requireAction(str(row,"dataset"),num(row,"revision"),FrozenConfirmationR30.currentSemantics(),BuildConfig.SOURCE_COMMIT_R26,num(row,"decision_ordinal"),num(row,"applied_at"));
        equal(StatsR27.sha256("R33_ACTION1|"+run+'|'+view+'|'+kind+'|'+str(row,"object_key")+'|'+str(row,"after_hash")),row.get("action_key"),"ACTION_KEY");return view;
    }
    static Map<String,Object> view(Map<String,Object> data,String id){
        Object lifecycle=data.get("lifecycleEvidenceR31");if(!(lifecycle instanceof Map))return map();
        Object receipt=((Map<?,?>)lifecycle).get("researchConsumptionR343");if(!(receipt instanceof Map))return map();
        Object value=((Map<?,?>)receipt).get("value");if(!(value instanceof Map))return map();Map<String,Object> out=map();
        for(String kind:new String[]{"born","nonBorn"}){
            Object raw=((Map<?,?>)value).get(kind);if(!(raw instanceof Map))continue;Map<?,?> section=(Map<?,?>)raw;
            Object exact=section.get("exactValidatedReceiptsByView"),lower=section.get("validatedReceiptLowerBoundsByView");
            out.put(kind+"Exact",exact instanceof Map?((Map<?,?>)exact).get(id):null);out.put(kind+"LowerBound",lower instanceof Map?((Map<?,?>)lower).get(id):null);
            out.put(kind+"Complete",section.get("complete"));out.put(kind+"Status",section.get("status"));
        }
        out.put("sourceScope",((Map<?,?>)receipt).get("sourceScope"));out.put("readStartedAt",((Map<?,?>)receipt).get("readStartedAt"));
        out.put("projectionSha256",((Map<?,?>)receipt).get("originalProjectionSha256"));return out;
    }
    static Boolean consumed(Map<String,Object> evidence){
        for(String key:new String[]{"bornLowerBound","nonBornLowerBound"}){Object n=evidence.get(key);if(n instanceof Number&&((Number)n).longValue()>0)return Boolean.TRUE;}
        return Boolean.TRUE.equals(evidence.get("bornComplete"))&&Boolean.TRUE.equals(evidence.get("nonBornComplete"))?Boolean.FALSE:null;
    }
}
