package com.ethk3.novaonepro.strategyv2;

/** Run with main; no JUnit, Android, network, or database is needed. */
public final class PairedEvidenceTest {
    private static int checks;
    private static final String EPOCH=h("epoch");
    private static final String FAMILY=h("global-family");
    private static final PairedEvidence.Identity P=new PairedEvidence.Identity("parent",h("parent-policy"),h("parent-structure"),h("parent-run"));
    private static final PairedEvidence.Identity C=new PairedEvidence.Identity("child",h("child-policy"),h("child-structure"),h("child-run"));
    private static String h(String s){return PairedEvidence.digest(s);}
    private static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    private static void near(double a,double b,String message){check(Math.abs(a-b)<1e-12,message+": "+a+" != "+b);}
    private static void rejects(Runnable r,String message){checks++;try{r.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("not rejected: "+message);}
    private static PairedEvidence.Protocol protocol(PairedEvidence.Scope scope,int claim,int... looks){
        return new PairedEvidence.Protocol(h("claim-"+claim),FAMILY,EPOCH,P,C,scope,100,.05,claim,looks,1.0,0.0,2);
    }
    private static PairedEvidence.Opportunity opportunity(int index){return new PairedEvidence.Opportunity("I"+index,1000+index,200+index*100,250+index*100,h("eligible-"+index));}
    private static PairedEvidence.Freeze freeze(PairedEvidence.Protocol p,int index,boolean parent,boolean hit,int position){
        PairedEvidence.Identity id=parent?P:C;
        int actual=position;
        int a=hit?actual:(actual%10)+1,b=hit?(actual%10)+1:((actual+1)%10)+1,c=hit?((actual+1)%10)+1:((actual+2)%10)+1;
        return new PairedEvidence.Freeze("I"+index,id.candidateId,p.hash,id.policyHash,id.structureHash,id.runHash,
                h("state-"+id.candidateId+"-"+index),h("input-"+index),h("prediction-"+id.candidateId+"-"+index),
                220+index*100,1000+index-1,1000+index-1,position,a,b,c);
    }
    private static PairedEvidence.Settlement outcome(int index){return new PairedEvidence.Settlement("I"+index,EPOCH,h("label-"+index),h("settlement-"+index),260+index*100,270+index*100,new int[]{1,2,3,4,5,6,7,8,9,10});}
    private static void add(PairedEvidence e,int i,Boolean parent,Boolean child){e.declare(opportunity(i));if(parent!=null)e.freeze(freeze(e.protocol,i,true,parent,1));if(child!=null)e.freeze(freeze(e.protocol,i,false,child,1));e.settle(outcome(i));}

    public static void main(String[] args){
        testProtocol();testPairingAndMissing();testFreezeIdentityAndTiming();testScopesAndPermutations();
        testReplayAndQuarantine();testLooksAndAlpha();testConfirmationAndNoAutoAuthority();
        System.out.println("PairedEvidenceTest PASS checks="+checks+" scope=PURE_JAVA_REFERENCE_NOT_ANDROID_NOT_REAL_PERFORMANCE");
    }
    private static void testProtocol(){
        rejects(()->protocol(PairedEvidence.Scope.POLICY_K3,0,2),"zero global claim ordinal");
        rejects(()->protocol(PairedEvidence.Scope.POLICY_K3,1,2,2),"duplicate endpoint");
        rejects(()->protocol(PairedEvidence.Scope.POLICY_K3,1,4,2),"decreasing endpoint");
        rejects(()->new PairedEvidence.Protocol(h("x"),FAMILY,EPOCH,P,C,PairedEvidence.Scope.POLICY_K3,100,Double.NaN,1,new int[]{2},1,0,1),"NaN alpha");
        rejects(()->new PairedEvidence.Protocol(h("x"),FAMILY,EPOCH,P,P,PairedEvidence.Scope.POLICY_K3,100,.05,1,new int[]{2},1,0,1),"same parent child");
        check(!protocol(PairedEvidence.Scope.POLICY_K3,1,2).hash.equals(protocol(PairedEvidence.Scope.SAME_POSITION_K3,1,2).hash),"scope participates in protocol identity");
        int[] schedule={2};PairedEvidence.Protocol p=new PairedEvidence.Protocol(h("x"),FAMILY,EPOCH,P,C,PairedEvidence.Scope.POLICY_K3,100,.05,1,schedule,1,0,1);schedule[0]=99;
        check(p.plannedAtLook(1)==2,"defensive schedule copy");
        check(!p.hash.equals(new PairedEvidence.Protocol(h("x"),h("other-family"),EPOCH,P,C,PairedEvidence.Scope.POLICY_K3,100,.05,1,new int[]{2},1,0,1).hash),"global family participates in protocol identity");
        rejects(()->new PairedEvidence.Protocol("not-sha",FAMILY,EPOCH,P,C,PairedEvidence.Scope.POLICY_K3,100,.05,1,new int[]{2},1,0,1),"claim identity matches SQL hash contract");
        near(p.alphaForLook(1),.0125,"candidate x look alpha");
        rejects(()->p.alphaForLook(0),"look zero");rejects(()->p.alphaForLook(2),"unregistered look");
    }
    private static void testPairingAndMissing(){
        PairedEvidence e=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,4));
        add(e,1,false,true);add(e,2,true,false);add(e,3,false,null);add(e,4,null,true);
        PairedEvidence.Result r=e.look(1);
        check(r.opportunities==4&&r.paired==2&&r.unpaired==2,"pair by exact issue");
        check(r.parentMissing==1&&r.childMissing==1,"missing counted separately");
        check(r.childOnlyWins==1&&r.parentOnlyWins==1,"paired 2x2 discordant counts");
        near(r.pairedDelta,0,"complete case descriptive delta");near(r.conservativeMean,-.5,"missing has worst-case delta, not dropped");
        near(r.coverage,.5,"coverage uses all registered opportunities");
        check(r.parentHits==1&&r.childHits==1,"paired hit totals use the same denominator");
        check(r.parentCapturedHits==1&&r.childCapturedHits==2,"valid side hits retained as descriptive extras");
        check("INSUFFICIENT_COVERAGE".equals(r.state),"missing cannot certify");
        check(r.descriptiveBlocks.size()==2,"predefined chronological blocks");
        check(Double.isNaN(r.descriptiveBlocks.get(1).pairedDelta),"no pairs remains unknown, not zero");
        check(r.lowerBound<=r.conservativeMean,"lower bound direction");
        rejects(()->e.freeze(freeze(e.protocol,3,false,true,1)),"cannot fill missing after outcome");
    }
    private static void testFreezeIdentityAndTiming(){
        PairedEvidence.Protocol p=protocol(PairedEvidence.Scope.POLICY_K3,1,2);PairedEvidence e=new PairedEvidence(p);e.declare(opportunity(1));
        PairedEvidence.Freeze good=freeze(p,1,false,true,1);
        rejects(()->e.freeze(new PairedEvidence.Freeze("I1",C.candidateId,p.hash,C.policyHash,C.structureHash,C.runHash,h("s"),h("i"),h("p"),350,1000,1000,1,1,2,3)),"capture exactly deadline");
        rejects(()->e.freeze(new PairedEvidence.Freeze("I1",C.candidateId,p.hash,C.policyHash,C.structureHash,C.runHash,h("s"),h("i"),h("p"),320,1001,1000,1,1,2,3)),"future input cutoff");
        rejects(()->e.freeze(new PairedEvidence.Freeze("I1",C.candidateId,p.hash,C.policyHash,C.structureHash,C.runHash,h("s"),h("i"),h("p"),320,1000,1001,1,1,2,3)),"future training cutoff");
        rejects(()->e.freeze(new PairedEvidence.Freeze("I1",C.candidateId,h("other-protocol"),C.policyHash,C.structureHash,C.runHash,h("s"),h("i"),h("p"),320,1000,1000,1,1,2,3)),"protocol drift");
        rejects(()->e.freeze(new PairedEvidence.Freeze("I1",C.candidateId,p.hash,h("new-policy"),C.structureHash,C.runHash,h("s"),h("i"),h("p"),320,1000,1000,1,1,2,3)),"silent update policy change");
        rejects(()->new PairedEvidence.Freeze("I1",C.candidateId,p.hash,C.policyHash,C.structureHash,C.runHash,h("s"),h("i"),h("p"),320,1000,1000,1,1,1,3),"K3 distinct");
        check(e.freeze(good),"valid prospective freeze accepted");check(!e.freeze(good),"identical freeze idempotent");
        e.settle(outcome(1));check(!e.freeze(good),"valid immutable replay allowed after settlement");
        check(!e.quarantined(),"expected rejected inputs do not forge success or corrupt existing record");
    }
    private static void testScopesAndPermutations(){
        PairedEvidence e=new PairedEvidence(protocol(PairedEvidence.Scope.SAME_POSITION_K3,1,1));e.declare(opportunity(1));e.freeze(freeze(e.protocol,1,true,true,1));
        rejects(()->e.freeze(freeze(e.protocol,1,false,true,2)),"same-position claim rejects different target position");
        PairedEvidence policy=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,1));policy.declare(opportunity(1));policy.freeze(freeze(policy.protocol,1,true,true,1));policy.freeze(freeze(policy.protocol,1,false,true,2));policy.settle(outcome(1));
        near(policy.look(1).pairedDelta,0,"whole-policy comparison permits different positions on same full draw");
        rejects(()->new PairedEvidence.Settlement("I1",EPOCH,h("l"),h("s"),360,370,new int[]{1,2,3,4,5,6,7,8,9,9}),"not a permutation");
        PairedEvidence epoch=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,1));epoch.declare(opportunity(1));
        rejects(()->epoch.settle(new PairedEvidence.Settlement("I1",h("other-epoch"),h("l"),h("s"),360,370,new int[]{1,2,3,4,5,6,7,8,9,10})),"wrong draw epoch");
        rejects(()->epoch.settle(new PairedEvidence.Settlement("I1",EPOCH,h("l"),h("s"),349,370,new int[]{1,2,3,4,5,6,7,8,9,10})),"actual draw before cutoff");
    }
    private static void testReplayAndQuarantine(){
        PairedEvidence e=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,2));check(e.declare(opportunity(1)),"opportunity insert");check(!e.declare(opportunity(1)),"opportunity replay");e.freeze(freeze(e.protocol,1,true,false,1));
        rejects(()->e.freeze(freeze(e.protocol,1,true,true,1)),"conflicting immutable freeze");check(e.quarantined(),"freeze conflict quarantines claim");rejects(()->e.look(1),"quarantine refuses metrics");
        PairedEvidence settlement=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,1));settlement.declare(opportunity(1));settlement.settle(outcome(1));check(!settlement.settle(outcome(1)),"settlement replay");
        rejects(()->settlement.settle(new PairedEvidence.Settlement("I1",EPOCH,h("corrected-label"),h("changed-settlement"),360,370,new int[]{2,1,3,4,5,6,7,8,9,10})),"draw correction cannot silently rewrite evidence");check(settlement.quarantined(),"draw conflict quarantines");
        PairedEvidence a=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,2)),b=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,2));
        for(int i=1;i<=2;i++){add(a,i,false,true);add(b,i,false,true);}check(a.look(1).evidenceHash.equals(b.look(1).evidenceHash),"restart replay yields identical evidence");
        check(a.look(1).sourceChainHash.length()==64,"source chain has complete identity hash");
    }
    private static void testLooksAndAlpha(){
        PairedEvidence e=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,2,4));check(e.look(1)==null,"not ready without declared prefix");near(e.alphaSpent(),0,"not-ready look spends nothing");
        e.declare(opportunity(1));e.freeze(freeze(e.protocol,1,true,false,1));e.freeze(freeze(e.protocol,1,false,true,1));check(e.look(1)==null,"pending canonical settlement is not miss");
        e.settle(outcome(1));add(e,2,false,true);rejects(()->e.look(2),"must consume registered looks in order");
        PairedEvidence.Result first=e.look(1);double spent=e.alphaSpent();check(e.look(1)==first,"same look replay returns same object");near(e.alphaSpent(),spent,"look replay does not re-spend alpha");
        add(e,3,false,true);add(e,4,false,true);PairedEvidence.Result second=e.look(2);near(second.cumulativeAlphaSpent,first.allocatedAlpha+second.allocatedAlpha,"alpha ledger accumulates");
        int[] looks=new int[30];for(int j=0;j<looks.length;j++)looks[j]=j+1;double total=0;
        for(int i=1;i<=30;i++){PairedEvidence.Protocol p=protocol(PairedEvidence.Scope.POLICY_K3,i,looks);for(int j=1;j<=looks.length;j++)total+=p.alphaForLook(j);}
        near(total,.05*Math.pow(30.0/31,2),"candidate x look budget telescopes");check(total<.05,"all tested candidate/look allocations below family budget");
    }
    private static void testConfirmationAndNoAutoAuthority(){
        PairedEvidence e=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,64));for(int i=1;i<=64;i++)add(e,i,false,true);
        PairedEvidence.Result r=e.look(1);check("COMPARISON_CONFIRMED".equals(r.state)&&r.lowerBound>0,"strong positive paired evidence can confirm comparison");check(!r.mayAuthorizeReal(),"comparison result is not production authority");
        PairedEvidence slight=new PairedEvidence(protocol(PairedEvidence.Scope.POLICY_K3,1,360));for(int i=1;i<=360;i++)add(slight,i,false,i<=18);
        PairedEvidence.Result small=slight.look(1);near(small.pairedDelta,.05,"five percentage point improvement represented exactly");check(small.lowerBound<0&&"EXHAUSTED_NOT_CONFIRMED".equals(small.state),"no manufactured statistical strength at 360");
        check(protocol(PairedEvidence.Scope.POLICY_K3,1,360,720,1440,2880,5760,11520).plannedAtLook(6)==11520,"no hard 360 endpoint in reference protocol");
    }
}
