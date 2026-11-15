package com.ethk3.novaonepro;
import java.util.*;

/** Calls the actual candidate reducer and existing production issue/history owners. */
public final class TailRecoveryProbe {
    private static int passed;
    private static void check(boolean value,String label){if(!value)throw new AssertionError(label);passed++;System.out.println("PASS "+label);}
    private static Models.Draw draw(long ordinal){Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(ordinal);d.openTime=1700000000000L+ordinal*60000;d.fetchTime=d.openTime;for(int i=0;i<10;i++)d.numbers[i]=i+1;return d;}
    private static List<Models.Draw> tail(long ordinal,int count){List<Models.Draw> rows=new ArrayList<>();for(int i=0;i<count;i++)rows.add(draw(ordinal-i));return rows;}
    public static void main(String[] args){
        long base=TargetIssueResolver.sequenceOrdinalSU1("202611150005");Models.Draw head=draw(base);
        PredrawTailRecoveryR32.Intent empty=PredrawTailRecoveryR32.reconstruct(null,Collections.emptyList());
        check(empty.requestHistory&&empty.targetIssue.isEmpty(),"empty durable DB requests sole history lane");
        List<Models.Draw> partial=tail(base,2);
        PredrawTailRecoveryR32.Intent first=PredrawTailRecoveryR32.reconstruct(head,partial);
        check(first.requestHistory&&first.contiguousRows==2&&first.targetIssue.equals("202611150006"),"gapped durable tail reconstructs next target without callback");
        PredrawTailRecoveryR32.Intent restart=PredrawTailRecoveryR32.reconstruct(head.copy(),new ArrayList<>(partial));
        check(restart.requestHistory&&restart.reason.equals(first.reason),"new process with identical committed rows reconstructs same intent");
        PredrawTailRecoveryR32.Intent merged=PredrawTailRecoveryR32.reconstruct(draw(base+1),tail(base+1,3));
        check(merged.targetIssue.equals("202611150007")&&!merged.targetIssue.equals(first.targetIssue),"new durable head supersedes old target");
        PredrawTailRecoveryR32.Intent ready=PredrawTailRecoveryR32.reconstruct(head,tail(base,12));
        check(!ready.requestHistory&&ready.contiguousRows==12,"exact minimum suffix ends recovery");
        List<Models.Draw> gap=tail(base,12);gap.remove(4);gap.add(draw(base-20));
        check(PredrawTailRecoveryR32.reconstruct(head,gap).requestHistory,"twelve scattered rows do not masquerade as continuous suffix");
        List<Models.Draw> conflict=tail(base,12);Models.Draw altered=draw(base);altered.numbers[0]=2;altered.numbers[1]=1;conflict.add(altered);
        check(PredrawTailRecoveryR32.reconstruct(head,conflict).requestHistory,"conflicting provider rows retain recovery instead of certifying readiness");
        long midnight=TargetIssueResolver.sequenceOrdinalSU1("202611151440");
        check(PredrawTailRecoveryR32.reconstruct(draw(midnight),tail(midnight,1)).targetIssue.equals("202611160001"),"cross-day latest target follows production resolver");
        check(head.issue.equals("202611150005")&&partial.size()==2,"reconstruction does not mutate supplied evidence");
        System.out.println("PASS_COUNT="+passed);
    }
}
