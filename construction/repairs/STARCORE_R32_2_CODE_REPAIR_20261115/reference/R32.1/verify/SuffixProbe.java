package com.ethk3.novaonepro;
import java.util.*;
public final class SuffixProbe {
    static Models.Draw draw(long ord) {
        Models.Draw d=new Models.Draw();
        d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(ord);d.openTime=1700000000000L+ord*60000L;d.fetchTime=d.openTime;
        for(int i=0;i<10;i++)d.numbers[i]=i+1;
        return d;
    }
    static void require(boolean b,String label){if(!b)throw new AssertionError(label);System.out.println("PASS "+label);}
    public static void main(String[] args) {
        long base=TargetIssueResolver.sequenceOrdinalSU1("202611150005");
        List<Models.Draw> page=new ArrayList<>();
        for(long n=base+1;n>=base-16;n--)page.add(draw(n));
        String issue=TargetIssueResolver.fromSequenceOrdinalSU1(base);
        List<Models.Draw> clean=R36AnalysisHistoryPolicyR364.bootstrapSuffixR32(page,issue);
        require(clean.size()==12 && issue.equals(clean.get(11).issue),"cross-midnight exact suffix, excludes target and older rows");
        page.add(draw(base));require(R36AnalysisHistoryPolicyR364.bootstrapSuffixR32(page,issue).size()==12,"identical duplicate tolerated");
        Models.Draw conflict=draw(base);conflict.numbers[0]=2;conflict.numbers[1]=1;page.add(conflict);
        require(R36AnalysisHistoryPolicyR364.bootstrapSuffixR32(page,issue).isEmpty(),"contradictory provider values blocked");
        page.remove(conflict);page.removeIf(d->d.issue.equals(TargetIssueResolver.fromSequenceOrdinalSU1(base-4)));
        require(R36AnalysisHistoryPolicyR364.bootstrapSuffixR32(page,issue).isEmpty(),"an internal gap cannot pass");
    }
}
