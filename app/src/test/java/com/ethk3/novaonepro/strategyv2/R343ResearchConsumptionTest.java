package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.BuildConfig;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R343ResearchConsumptionTest {
    static Map<String,Object> map(Object... pairs){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<pairs.length;i+=2)m.put((String)pairs[i],pairs[i+1]);return m;}
    static String hash(String x){return StatsR27.sha256(x);}
    static Map<String,Object> contract(){return map("source",BuildConfig.SOURCE_COMMIT_R26,"dataset",hash("dataset"),"revision",3L,"role","SELECT","abi",FrozenConfirmationR30.currentSemantics(),"input",hash("input"),"through",100L,"available",1000L);}
    static Map<String,Object> born(){
        Map<String,Object> c=contract();String original=JsonR27.write(map("sourceContract",c,"viewId","R01"));
        PolicySpecR27 policy=new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),2,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.01,0,new PolicySpecR27.Meta("I04","TEST",Collections.emptyList(),1,0,"fixture"));
        String child=policy.policyHash(),proposal=hash("proposal"),consumer="fixture",run=hash("run");
        String receipt=hash(proposal+'|'+child+"|1|"+consumer),before=hash("R31_ABSENT_CANDIDATE_BEFORE_BIRTH|"+child);
        String proposalJson=JsonR27.write(map("candidateHash",child));
        Map<String,Object> receipts=map("source_run",run,"input_hash",c.get("input"),"role","SELECT","abi",c.get("abi"),"source_revision",3L,
            "consumer_version",ResearchConsumptionStoreR31.VERSION+":"+consumer,"before_hash",before,"after_hash",child,"budget",1L,"product_hash",hash(original));
        String key=hash(ResearchResultContractR31.VERSION+'|'+run+'|'+consumer+'|'+child+'|'+hash(original));
        Map<String,Object> typed=map("validated",true,"state","ACTUAL_BORN_CONSUMPTION_NOT_EFFECTIVENESS","view","R01","receipts",receipts,"consumeKey",key,
            "target","POSITION:2","datasetHash",c.get("dataset"),"decisionOrdinal",101L,"latestLabelOrdinal",100L);
        Map<String,Object> link=map("scope","ACTUAL_ORIGINAL_BORN_CHILD_SAME_TRANSACTION_NOT_QUALITY_PROOF","proposalId",proposal,"childPolicyHash",child,
            "consumer",consumer,"generation",1L,"receiptHash",receipt,"resultContractR31",typed);
        return map("run",run,"source",c.get("source"),"dataset",c.get("dataset"),"revision",3L,"role","SELECT","original",original,"payload",JsonR27.write(link),
            "proposal",proposal,"child",child,"consumer",consumer,"generation",1L,"receipt",receipt,"proposal_state","CONSUMED","proposal_payload",proposalJson,
            "proposal_hash",hash(proposalJson),"policy_spec",policy.fullJson(),"policy_dataset",c.get("dataset"),"policy_generation",1L,"original_views","R01");
    }
    static Map<String,Object> action(){
        Map<String,Object> c=contract();String original=JsonR27.write(map("sourceContract",c,"viewId","R01","inputHash",c.get("input")));
        String after="{\"budget\":2}",before=hash("{\"budget\":1}"),run=hash("run");
        return map("run",run,"source",c.get("source"),"dataset",c.get("dataset"),"revision",3L,"role","SELECT","original",original,"view","R01","kind","BUDGET_APPLIED",
            "product",hash(original),"input",c.get("input"),"abi",c.get("abi"),"before_hash",before,"after_hash",hash(after),"applied",after,"object_key","budget",
            "budget",1L,"latest_label",100L,"decision_ordinal",101L,"applied_at",1001L,"action_key",hash("R33_ACTION1|"+run+"|R01|BUDGET_APPLIED|budget|"+hash(after)),"original_views","R01");
    }
    @Test public void validBornRequiresOriginalHashSourceAndActualChild(){assertEquals("R01",ReportResearchConsumptionR343.validateBorn(born()));}
    @Test public void validNonBornRemainsIndependent(){assertEquals("R01",ReportResearchConsumptionR343.validateAction(action()));}
    @Test public void wrongOriginalHashCannotCount(){Map<String,Object> r=born();r.put("original",r.get("original")+" ");reject(()->ReportResearchConsumptionR343.validateBorn(r));}
    @Test public void oldSourceCannotCount(){Map<String,Object> r=born();r.put("source",hash("old source"));reject(()->ReportResearchConsumptionR343.validateBorn(r));}
    @Test public void missingActualChildCannotCount(){Map<String,Object> r=born();r.remove("policy_spec");reject(()->ReportResearchConsumptionR343.validateBorn(r));}
    @Test public void unchangedNonBornActionCannotCount(){Map<String,Object> r=action();r.put("before_hash",r.get("after_hash"));reject(()->ReportResearchConsumptionR343.validateAction(r));}
    @Test public void partialZeroIsUnknownButAValidatedLowerBoundIsEvidence(){
        Map<String,Object> partial=map("bornLowerBound",0L,"nonBornLowerBound",0L,"bornComplete",true,"nonBornComplete",false);
        assertNull(ReportResearchConsumptionR343.consumed(partial));partial.put("nonBornLowerBound",1L);assertEquals(Boolean.TRUE,ReportResearchConsumptionR343.consumed(partial));
        partial.put("nonBornLowerBound",0L);partial.put("nonBornComplete",true);assertEquals(Boolean.FALSE,ReportResearchConsumptionR343.consumed(partial));
    }
    @Test public void receiptIntegerFieldsRejectFractionsNonfiniteAndOverflow(){
        for(double v:new double[]{1.5,Double.NaN,Double.POSITIVE_INFINITY,0x1.0p63})reject(()->ReportResearchConsumptionR343.num(map("n",v),"n"));
        assertEquals(1L,ReportResearchConsumptionR343.num(map("n",1.0),"n"));
        assertTrue(ReportResearchConsumptionR343.viewKeysPartial(map("original_views","#OVERSIZE_VIEW_KEY#")));
        assertTrue(ReportResearchConsumptionR343.viewKeysPartial(map("original_views",String.join("|",Collections.nCopies(65,"R01")))));
    }
    static void reject(Runnable r){try{r.run();fail("invalid evidence counted");}catch(IllegalArgumentException expected){}}
}
