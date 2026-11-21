package com.ethk3.novaonepro;

/** Executes the shipped pure production policies. Synthetic inputs never become phone or Future evidence. */
public final class R335RuntimeContractTest {
    static int checks;
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    static void rejects(Runnable r){checks++;try{r.run();throw new AssertionError("invalid accepted");}catch(IllegalArgumentException expected){}}
    static final String TARGET="202611170089",BASE="202611170088",H="b".repeat(64),F="c".repeat(64);
    static final int[] DRAW={1,2,3,4,5,6,7,8,9,10},PICKS={1,2,3};
    static final String DH=FrozenFeedbackProofR335.sha("R815_DRAW_V2|"+TARGET+"|1|2|3|4|5|6|7|8|9|10");
    static final String PROV=FrozenFeedbackProofR335.sha("R36_OOS_PROVENANCE|"+F+'|'+H+'|'+H+'|'+H+'|'+DH+"|31");
    static final String LABEL=FrozenFeedbackProofR335.sha(PROV+"|1|1");
    static void frozen(long commit,long deadline,String base,int[] draw,int[] picks,int actual,int hit,String label){
        FrozenFeedbackProofR335.verify(TARGET,base,base,commit,deadline,30,31,1,picks,draw,actual,hit,F,H,H,H,DH,PROV,label);
    }
    public static void main(String[] args){
        ReportResourceWaitR335.State empty=ReportResourceWaitR335.State.empty();
        check(!empty.active(),"new journal empty");
        check(ReportResourceWaitR335.isResourceWait("REPORT_RESOURCE_WAIT|BLOCKED_PROTECTED_DEBT"),"budget classified");
        check(ReportResourceWaitR335.isResourceWait("REPORT_INPUT_LEASE_BUSY"),"lease classified");
        check(!ReportResourceWaitR335.isResourceWait("REPORT_NATIVE_BACKUP_CORRUPT"),"functional failure retained");
        String reason="REPORT_RESOURCE_WAIT|BLOCKED_PROTECTED_DEBT";
        ReportResourceWaitR335.State state=ReportResourceWaitR335.rejected(empty,reason,"same-dependency","boot-a",10000,1000);
        check(state.attempt==1&&state.nextElapsedMs==6000,"first 5 second wait");
        check(ReportResourceWaitR335.remaining(state,"boot-a",1001)==4999,"same boot gate");
        check(ReportResourceWaitR335.remaining(state,"boot-b",1)==0,"boot changes reobserve");
        check(ReportResourceWaitR335.remaining(state,"UNKNOWN",1)==0,"unknown boot cannot borrow elapsed");
        check(ReportResourceWaitR335.remaining(state,"boot-a",6000)==0,"exact due allowed");
        long elapsed=6000;
        for(int i=0;i<100;i++){state=ReportResourceWaitR335.rejected(state,reason,"same-dependency","boot-a",5,elapsed);elapsed=state.nextElapsedMs;}
        check(state.attempt==101,"100 waits never exhaust functional retries");
        check(state.sinceWallMs==10000,"wall rollback does not reset origin");
        check(ReportResourceWaitR335.retryDelay(state.attempt)==60000,"bounded backoff");
        ReportResourceWaitR335.State changed=ReportResourceWaitR335.rejected(state,reason,"new-dependency","boot-a",20,elapsed);
        check(changed.attempt==1&&changed.sinceWallMs==20,"changed dependency reset");
        ReportResourceWaitR335.State huge=ReportResourceWaitR335.rejected(empty,reason,"x","boot-a",1,Long.MAX_VALUE-1);
        check(huge.nextElapsedMs==Long.MAX_VALUE,"overflow saturates");
        rejects(()->new ReportResourceWaitR335.State("","bad","",0,0,0));
        rejects(()->ReportResourceWaitR335.rejected(empty,"NOT_RESOURCE","x","boot",1,1));
        rejects(()->ReportResourceWaitR335.retryDelay(0));
        frozen(20,30,BASE,DRAW,PICKS,1,1,LABEL);checks++;
        frozen(20,21,BASE,DRAW,PICKS,1,1,LABEL);checks++;
        rejects(()->frozen(20,20,BASE,DRAW,PICKS,1,1,LABEL));
        rejects(()->frozen(20,19,BASE,DRAW,PICKS,1,1,LABEL));
        rejects(()->frozen(20,0,BASE,DRAW,PICKS,1,1,LABEL));
        rejects(()->frozen(30,40,BASE,DRAW,PICKS,1,1,LABEL));
        rejects(()->frozen(0,30,BASE,DRAW,PICKS,1,1,LABEL));
        rejects(()->frozen(20,30,TARGET,DRAW,PICKS,1,1,LABEL));
        rejects(()->frozen(20,30,BASE,DRAW,new int[]{1,1,2},1,1,LABEL));
        rejects(()->frozen(20,30,BASE,new int[]{1,1,3,4,5,6,7,8,9,10},PICKS,1,1,LABEL));
        rejects(()->frozen(20,30,BASE,DRAW,PICKS,2,1,LABEL));
        rejects(()->frozen(20,30,BASE,DRAW,PICKS,1,0,LABEL));
        rejects(()->frozen(20,30,BASE,DRAW,PICKS,1,1,H));
        System.out.println("PASS R335_RUNTIME_CONTRACT checks="+checks+" scope=HOST_PRODUCTION_POLICY_ONLY");
    }
}
