package com.ethk3.novaonepro.strategyv2;

import java.util.*;

public final class AuditControlsSU1Test {
    private static int checks;
    private static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    private static int[][] history(){Random random=new Random(22);int[][] out=new int[5000][10];for(int t=0;t<out.length;t++){for(int p=0;p<10;p++)out[t][p]=p+1;for(int p=9;p>0;p--){int q=random.nextInt(p+1),v=out[t][p];out[t][p]=out[t][q];out[t][q]=v;}}return out;}
    public static void main(String[] args){
        int[][] raw=history();int original=Arrays.deepHashCode(raw);String root=HashingSU1.text("fixed source dataset");
        int[][] shuffled=CampaignControlsSU1.transform(raw,CampaignControlsSU1.Variant.TIME_SHUFFLED,41);List<String> a=new ArrayList<>(),b=new ArrayList<>();for(int[] row:raw)a.add(Arrays.toString(row));for(int[] row:shuffled)b.add(Arrays.toString(row));Collections.sort(a);Collections.sort(b);check(a.equals(b),"time shuffle changed the draw multiset");check(!Arrays.deepEquals(raw,shuffled),"time shuffle was a no-op");
        int[][] planted=CampaignControlsSU1.transform(raw,CampaignControlsSU1.Variant.PLANTED_SIGNAL,41);int repeats=0;for(int t=1;t<planted.length;t++){RelationalStateInference.validateDraw(planted[t]);if(planted[t][3]==planted[t-1][3])repeats++;}check(repeats>850,"declared planted signal was not injected");
        for(CampaignControlsSU1.Variant variant:CampaignControlsSU1.Variant.values()){
            try(CampaignAuditSessionSU1 session=new CampaignAuditSessionSU1(raw,root,variant,41)){
                check(session.trainEnd==3560&&session.phase.equals("SEARCH"),"audit split is not disjoint");
                session.advance(new BudgetTokenSU1(60000,100000000,null));check(session.attempt!=null&&session.attempt.ranked!=null,"audit did not execute real replay for "+variant);
                check(session.attempt.ranked.devActions>0&&session.attempt.ranked.valActions>0,"empty audit outcome presented as evaluated");
                String checkpoint=session.checkpoint();try(CampaignAuditSessionSU1 restored=CampaignAuditSessionSU1.restore(raw,root,checkpoint)){
                    check(checkpoint.equals(restored.checkpoint()),"audit resume changed its state "+variant);check(restored.attempts==1,"audit attempts not resumed");
                }
                check(session.selectedHash()==null,"champion selected before search finished");
                check(!session.foundry.fullScan,"historical audit did not use the declared matched refresh budget");
            }
        }
        check(original==Arrays.deepHashCode(raw),"a control run mutated the real source");
        System.out.println("SU1_AUDIT_CONTROLS_PASS checks="+checks+" variants=5 scope=actual_first_replay_and_restart_not_completed_campaign_effectiveness");
    }
}
