package com.ethk3.novaonepro;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MultiHorizonProbeR80Test {
    private static final long NOW=1800000000000L;
    static List<Models.Draw> history(int n,long seed) {
        Random random=new Random(seed);List<Models.Draw> out=new ArrayList<>();String issue="202610140001";
        for(int i=0;i<n;i++){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=NOW-(n-i)*60000L;d.fetchTime=d.openTime+1;
            int start=random.nextInt(10);for(int k=0;k<10;k++)d.numbers[k]=(start+k)%10+1;out.add(d);issue=TargetIssueResolver.next(issue).targetIssue;}
        return out;
    }
    private static double[] oracle(List<Models.Draw> rows,int horizon) {
        double[] result=new double[10];int last=rows.get(rows.size()-1).numbers[0]-1;
        for(int window:new int[]{48,240,960}) {
            double[][] t=new double[10][10];for(double[] row:t)Arrays.fill(row,1d);
            for(int i=rows.size()-window;i<rows.size()-1;i++)t[rows.get(i).numbers[0]-1][rows.get(i+1).numbers[0]-1]++;
            for(double[] row:t){double sum=0;for(double v:row)sum+=v;for(int c=0;c<10;c++)row[c]/=sum;}
            double[][] power=new double[10][10];for(int i=0;i<10;i++)power[i][i]=1d;
            // Independent full-matrix oracle, rather than the production row-vector recurrence.
            for(int step=0;step<horizon;step++){double[][] next=new double[10][10];for(int r=0;r<10;r++)for(int c=0;c<10;c++)for(int k=0;k<10;k++)next[r][c]+=power[r][k]*t[k][c];power=next;}
            for(int i=0;i<10;i++)result[i]+=power[last][i]/3d;
        }
        return result;
    }
    @Test public void fixedMathMatchesIndependentFullMatrixOracle(){
        for(int seed=0;seed<8;seed++){List<Models.Draw> rows=history(960,seed);MultiHorizonProbeR80.Model m=MultiHorizonProbeR80.fit(rows,rows.get(959).issue,NOW);
            for(int h:new int[]{1,2,4,8})assertArrayEquals(oracle(rows,h),m.distribution(h),1e-14);}
    }
    @Test public void exact1440RolloverIncludingLeapDay(){assertEquals("202603010001",MultiHorizonProbeR80.target("202602281440",1));assertEquals("202402290004",MultiHorizonProbeR80.target("202402281440",4));assertEquals("202610160006",MultiHorizonProbeR80.target("202610151438",8));}
    @Test public void deterministicTieUsesSmallestThreeNumbers(){double[] q=new double[10];Arrays.fill(q,.1d);assertEquals(7,MultiHorizonProbeR80.mask(q));assertEquals(.3,MultiHorizonProbeR80.mass(q,7),1e-15);}
    @Test public void prefixOutsideFixed960DoesNotAffectModel(){List<Models.Draw> rows=history(1000,42);String anchor=rows.get(999).issue;String original=MultiHorizonProbeR80.fit(rows,anchor,NOW).modelHash;rows.get(0).numbers[0]=99;assertEquals(original,MultiHorizonProbeR80.fit(rows,anchor,NOW).modelHash);}
    @Test public void changedObservedPrefixChangesSourceAndModel(){List<Models.Draw> rows=history(960,42);String anchor=rows.get(959).issue;MultiHorizonProbeR80.Model old=MultiHorizonProbeR80.fit(rows,anchor,NOW);int a=rows.get(17).numbers[0];rows.get(17).numbers[0]=rows.get(17).numbers[1];rows.get(17).numbers[1]=a;MultiHorizonProbeR80.Model changed=MultiHorizonProbeR80.fit(rows,anchor,NOW);assertNotEquals(old.sourceHash,changed.sourceHash);assertNotEquals(old.modelHash,changed.modelHash);}
    @Test(expected=IllegalArgumentException.class) public void futureObservationCannotEnterFrozenPrefix(){List<Models.Draw> rows=history(960,1);rows.get(12).fetchTime=NOW+1;MultiHorizonProbeR80.fit(rows,rows.get(959).issue,NOW);}
    @Test(expected=IllegalArgumentException.class) public void targetOrLaterCannotEnterHistory(){List<Models.Draw> rows=history(960,1);MultiHorizonProbeR80.fit(rows,rows.get(958).issue,NOW);}
    @Test(expected=IllegalArgumentException.class) public void missingHistoryIsNotZero(){List<Models.Draw> rows=history(959,1);MultiHorizonProbeR80.fit(rows,rows.get(958).issue,NOW);}
    @Test(expected=IllegalArgumentException.class) public void gapIsNotFilledWithZero(){List<Models.Draw> rows=history(960,1);rows.get(45).issue=rows.get(44).issue;MultiHorizonProbeR80.fit(rows,rows.get(959).issue,NOW);}
    @Test(expected=IllegalArgumentException.class) public void noGrowingHistoryBeyond5000(){List<Models.Draw> rows=history(5001,1);MultiHorizonProbeR80.fit(rows,rows.get(5000).issue,NOW);}
    @Test(expected=IllegalArgumentException.class) public void noRetrospectiveHorizonSelection(){List<Models.Draw> rows=history(960,1);MultiHorizonProbeR80.fit(rows,rows.get(959).issue,NOW).distribution(3);}
}
