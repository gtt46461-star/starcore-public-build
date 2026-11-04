package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import com.ethk3.novaonepro.TargetIssueResolver;
/** Current-production read-only counterexamples; synthetic legal permutations, not quality evidence. */
public final class InputIdentityPreparationR31 {
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    public static void main(String[] args){
        String first="202611010001";long ordinal=TargetIssueResolver.sequenceOrdinalSU1(first);
        int[] draw={1,2,3,4,5,6,7,8,9,10};List<ReplayDatasetR27.Row> canonical=new ArrayList<>(),shifted=new ArrayList<>();
        for(int i=0;i<6000;i++){String issue=TargetIssueResolver.fromSequenceOrdinalSU1(ordinal+i);canonical.add(new ReplayDatasetR27.Row(issue,ordinal+i,draw));shifted.add(new ReplayDatasetR27.Row(issue,ordinal+i+1,draw));}
        ReplayDatasetR27 full=ReplayDatasetR27.of(canonical),oneOffset=ReplayDatasetR27.of(shifted),tail=full.tail(5000);
        require(full.datasetHash.equals(oneOffset.datasetHash),"ordinal offset counterexample changed hash");
        SplitR27 original=SplitR27.of(full,null),changed=SplitR27.of(oneOffset,null);
        require(original.holdoutFrom!=changed.holdoutFrom,"offset did not shift role ranges");
        Map<String,Object> facts=new LinkedHashMap<>();facts.put("calendarIssue",first);facts.put("readerOrdinal",ReplayDatasetR27.ordinalOf(first));facts.put("canonicalOrdinal",ordinal);facts.put("readerEpochOffset",ReplayDatasetR27.ordinalOf(first)-ordinal);facts.put("canonicalRolloverDistance",TargetIssueResolver.sequenceOrdinalSU1("202611020001")-TargetIssueResolver.sequenceOrdinalSU1("202611011440"));
        facts.put("sameHashDifferentOrdinals",full.datasetHash.equals(oneOffset.datasetHash));facts.put("sameHashOriginalHoldoutFrom",original.holdoutFrom);facts.put("sameHashShiftedHoldoutFrom",changed.holdoutFrom);facts.put("tailRows",tail.size());
        String frozen=tail.datasetHash;int before=tail.draws[0][0];int swap=full.draws[1000][0];full.draws[1000][0]=full.draws[1000][1];full.draws[1000][1]=swap;
        require(tail.draws[0][0]!=before&&frozen.equals(tail.datasetHash),"tail alias counterexample missing");facts.put("tailAliasChangesFrozenPayloadWithoutHash",!ReplayDatasetR27.hash(tail.issues,tail.draws,tail.segment).equals(tail.datasetHash));
        HoldoutLedgerR27 ledger=new HoldoutLedgerR27();ledger.confirm("same-policy","dataset-A",123,100,820);ledger.confirm("same-policy","dataset-B",123,100,820);
        facts.put("samePolicySameBlockNewDatasetAllowsSecondRead",ledger.confirmed("same-policy","dataset-A",123)&&ledger.confirmed("same-policy","dataset-B",123));facts.put("countedDistinctPolicyExposuresAfterTwoReads",ledger.exposures(123));
        List<ReplayDatasetR27.Row> duplicate=Arrays.asList(new ReplayDatasetR27.Row("202611010001",ordinal,draw),new ReplayDatasetR27.Row("202611010001",ordinal+1,draw));ReplayDatasetR27 accepted=ReplayDatasetR27.of(duplicate);facts.put("duplicateIssueDistinctOrdinalAcceptedRows",accepted.size());require(accepted.size()==2,"duplicate issue fixture no longer accepted");
        facts.put("scope","CURRENT_PRODUCTION_SYNTHETIC_COUNTEREXAMPLES_NOT_ANDROID_OR_NATURAL_QUALITY");facts.put("pinTriggerSql",Arrays.asList(ResearchInputPinR30R1.triggerSql()));facts.put("sourceTriggerSql",Arrays.asList(SchemaR27.LIVE_SOURCE_TRIGGERS));facts.put("researchDdl",Arrays.asList(ResearchSchemaR30R1.DDL));System.out.println(JsonR27.write(facts));
    }
}
