package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * R930 D09/E04F frequency-quality feasibility contract. It reports planned/legal/eligible/selected/
 * REAL denominators separately. It never authorizes BET and never compensates a coverage shortfall.
 */
public final class PredictabilityPolicyR930 {
    public static final String REVISION="R930_PREDICTABILITY_POLICY_V1";
    public static final double TARGET_COVERAGE=0.15d;
    public static final double TARGET_HIT_RATE=0.40d;
    public static final double TARGET_ECE=0.04d;
    public static final int MIN_JOINT_SUPPORT=120;
    private PredictabilityPolicyR930(){}

    public enum WindowKind { POLICY,SEALED,FUTURE }
    public enum Status { PASS_EFFECT_WITH_CONFIDENCE,POINT_TARGET_MET_CONFIDENCE_INSUFFICIENT,FAIL_EFFECT,WAIT_SUPPORT }
    public enum BlockReason { NONE,DATA,SIGNAL,SUPPORT,RESOURCE,CALIBRATION,DOMAIN,SYSTEM,DECISION_GATE }

    public static final class FrozenPolicy {
        public final double threshold;public final String rankerHash,calibrationHash,selectionPolicyHash,policyHash;
        public final int selectedOnPolicy,eligibleOnPolicy;public final double policyHitRate,policyCoverage,policyBrier;
        public FrozenPolicy(double threshold,String rankerHash,String calibrationHash,String selectionPolicyHash,
                            int eligibleOnPolicy,int selectedOnPolicy,double hitRate,double brier){
            if(!finite(threshold)||threshold<0||threshold>1||eligibleOnPolicy<=0||selectedOnPolicy<0||selectedOnPolicy>eligibleOnPolicy)throw new IllegalArgumentException("R930_POLICY_INPUT");
            this.threshold=threshold;this.rankerHash=hash(rankerHash);this.calibrationHash=hash(calibrationHash);this.selectionPolicyHash=hash(selectionPolicyHash);
            this.eligibleOnPolicy=eligibleOnPolicy;this.selectedOnPolicy=selectedOnPolicy;this.policyHitRate=boundedOrNaN(hitRate);this.policyCoverage=(double)selectedOnPolicy/eligibleOnPolicy;this.policyBrier=nonNegativeOrNaN(brier);
            this.policyHash=V51ChecksumEngine.sha256(REVISION+"|threshold="+fmt(threshold)+"|ranker="+this.rankerHash+"|cal="+this.calibrationHash+"|selection="+this.selectionPolicyHash+"|eligible="+eligibleOnPolicy+"|selected="+selectedOnPolicy+"|hr="+fmt(this.policyHitRate)+"|coverage="+fmt(this.policyCoverage)+"|brier="+fmt(this.policyBrier));
        }
    }

    public static final class Observation {
        public final String targetIssue;public final boolean planned,legalAvailable,eligible,selected,real,hit;
        public final double calibratedP;public final BlockReason blockReason;public final String evidenceHash;
        public Observation(String targetIssue,boolean planned,boolean legalAvailable,boolean eligible,boolean selected,boolean real,
                           boolean hit,double calibratedP,BlockReason blockReason,String evidenceHash){
            this.targetIssue=required(targetIssue);this.planned=planned;this.legalAvailable=legalAvailable;this.eligible=eligible;this.selected=selected;this.real=real;this.hit=hit;
            this.calibratedP=boundedOrNaN(calibratedP);this.blockReason=blockReason==null?BlockReason.NONE:blockReason;this.evidenceHash=hash(evidenceHash);
            if(real&&!selected)throw new IllegalArgumentException("R930_REAL_NOT_SELECTED");if(selected&&!eligible)throw new IllegalArgumentException("R930_SELECTED_NOT_ELIGIBLE");if(eligible&&!legalAvailable)throw new IllegalArgumentException("R930_ELIGIBLE_NOT_LEGAL");if(legalAvailable&&!planned)throw new IllegalArgumentException("R930_LEGAL_NOT_PLANNED");
        }
    }

    public static final class Report {
        public final WindowKind window;public final String policyHash,startIssue,endIssue,reportHash;
        public final int plannedN,legalN,eligibleN,selectedN,realN,selectedHits,realHits,calibratedN;
        public final double coverage,selectedHitRate,realHitRate,brier,ece;
        public final SelectionEvidenceContractR929.WilsonInterval coverageWilson,hitWilson;
        public final Status status;public final String reason;public final boolean frequencyDebt=false,forcedBet=false,chase=false;
        Report(WindowKind window,FrozenPolicy policy,List<Observation> observations){
            this.window=window;this.policyHash=policy.policyHash;if(observations==null||observations.isEmpty())throw new IllegalArgumentException("R930_REPORT_EMPTY");
            int planned=0,legal=0,eligible=0,selected=0,real=0,hits=0,realHits0=0,calN=0;double brierSum=0;ArrayList<Double> probs=new ArrayList<Double>();ArrayList<Integer> labels=new ArrayList<Integer>();
            String start=null,end=null;for(Observation o:observations){if(o==null)continue;if(start==null||o.targetIssue.compareTo(start)<0)start=o.targetIssue;if(end==null||o.targetIssue.compareTo(end)>0)end=o.targetIssue;if(o.planned)planned++;if(o.legalAvailable)legal++;if(o.eligible)eligible++;if(o.selected){selected++;if(o.hit)hits++;if(finite(o.calibratedP)){calN++;probs.add(o.calibratedP);labels.add(o.hit?1:0);double d=o.calibratedP-(o.hit?1:0);brierSum+=d*d;}}if(o.real){real++;if(o.hit)realHits0++;}}
            startIssue=required(start);endIssue=required(end);plannedN=planned;legalN=legal;eligibleN=eligible;selectedN=selected;realN=real;selectedHits=hits;realHits=realHits0;calibratedN=calN;
            coverage=eligible==0?0.0d:(double)selected/eligible;selectedHitRate=selected==0?0.0d:(double)hits/selected;realHitRate=real==0?Double.NaN:(double)realHits0/real;brier=calN==0?Double.NaN:brierSum/calN;ece=calN==0?Double.NaN:ece10(probs,labels);
            coverageWilson=SelectionEvidenceContractR929.wilson95(selected,eligible);hitWilson=SelectionEvidenceContractR929.wilson95(hits,selected);
            boolean point=eligible>0&&selected>=MIN_JOINT_SUPPORT&&coverage>=TARGET_COVERAGE&&selectedHitRate>=TARGET_HIT_RATE&&finite(ece)&&ece<=TARGET_ECE;
            boolean confidence=point&&finite(coverageWilson.lower)&&coverageWilson.lower>=TARGET_COVERAGE&&finite(hitWilson.lower)&&hitWilson.lower>=TARGET_HIT_RATE;
            if(selected<MIN_JOINT_SUPPORT){status=Status.WAIT_SUPPORT;reason="SELECTED_SUPPORT_"+selected+"_LT_"+MIN_JOINT_SUPPORT;}
            else if(confidence){status=Status.PASS_EFFECT_WITH_CONFIDENCE;reason="SAME_WINDOW_COVERAGE_HR_ECE_AND_WILSON_PASS";}
            else if(point){status=Status.POINT_TARGET_MET_CONFIDENCE_INSUFFICIENT;reason="POINT_TARGET_ONLY_WILSON_LOWER_INSUFFICIENT";}
            else{status=Status.FAIL_EFFECT;reason=diagnose(observations,eligible,selected,calN);}
            StringBuilder b=new StringBuilder(4096).append(REVISION).append('|').append(window).append('|').append(policyHash).append('|').append(startIssue).append('|').append(endIssue).append("|planned=").append(plannedN).append("|legal=").append(legalN).append("|eligible=").append(eligibleN).append("|selected=").append(selectedN).append("|real=").append(realN).append("|hits=").append(selectedHits).append("|realHits=").append(realHits).append("|coverage=").append(fmt(coverage)).append("|hr=").append(fmt(selectedHitRate)).append("|brier=").append(fmt(brier)).append("|ece=").append(fmt(ece)).append('|').append(status).append('|').append(reason);for(Observation o:observations)b.append('|').append(o.targetIssue).append(':').append(o.evidenceHash);reportHash=V51ChecksumEngine.sha256(b.toString());
        }
    }

    public static FrozenPolicy freezePolicy(List<FrozenPolicy> policyCandidates){
        if(policyCandidates==null||policyCandidates.isEmpty())throw new IllegalArgumentException("R930_POLICY_CANDIDATES");ArrayList<FrozenPolicy> p=new ArrayList<FrozenPolicy>(policyCandidates);
        Collections.sort(p,new Comparator<FrozenPolicy>(){public int compare(FrozenPolicy a,FrozenPolicy b){boolean fa=a.policyCoverage>=TARGET_COVERAGE&&finite(a.policyHitRate)&&a.policyHitRate>=TARGET_HIT_RATE;boolean fb=b.policyCoverage>=TARGET_COVERAGE&&finite(b.policyHitRate)&&b.policyHitRate>=TARGET_HIT_RATE;if(fa!=fb)return fa?-1:1;int c=Double.compare(a.policyBrier,b.policyBrier);if(c!=0)return c;c=Double.compare(b.policyHitRate,a.policyHitRate);if(c!=0)return c;c=Double.compare(b.policyCoverage,a.policyCoverage);if(c!=0)return c;return Double.compare(b.threshold,a.threshold);}});return p.get(0);
    }
    public static Report evaluate(WindowKind kind,FrozenPolicy policy,List<Observation> observations){if(kind==WindowKind.POLICY)throw new IllegalArgumentException("R930_POLICY_SPLIT_CANNOT_CERTIFY_EFFECT");return new Report(kind,policy,observations);}

    /** This object is evidence only. There is intentionally no authorize()/BET API. */
    public static boolean mayRequestExistingDecisionGate(Report r){return r!=null&&r.window==WindowKind.FUTURE&&r.status==Status.PASS_EFFECT_WITH_CONFIDENCE;}

    private static String diagnose(List<Observation> rows,int eligible,int selected,int calN){int data=0,signal=0,support=0,resource=0,cal=0,domain=0,system=0,gate=0;for(Observation o:rows){if(o==null)continue;switch(o.blockReason){case DATA:data++;break;case SIGNAL:signal++;break;case SUPPORT:support++;break;case RESOURCE:resource++;break;case CALIBRATION:cal++;break;case DOMAIN:domain++;break;case SYSTEM:system++;break;case DECISION_GATE:gate++;break;default:break;}}String best="SIGNAL";int max=signal;if(data>max){max=data;best="DATA";}if(support>max){max=support;best="SUPPORT";}if(resource>max){max=resource;best="RESOURCE";}if(cal>max){max=cal;best="CALIBRATION";}if(domain>max){max=domain;best="DOMAIN";}if(system>max){max=system;best="SYSTEM";}if(gate>max){best="DECISION_GATE";}return "FAIL_EFFECT|dominant="+best+"|eligible="+eligible+"|selected="+selected+"|calN="+calN;}
    private static double ece10(List<Double> p,List<Integer> y){if(p.isEmpty())return Double.NaN;double e=0;for(int bin=0;bin<10;bin++){int n=0,h=0;double ps=0,lo=bin/10.0d,hi=(bin+1)/10.0d;for(int i=0;i<p.size();i++){double v=p.get(i);if(v>=lo&&(bin==9?v<=1:v<hi)){n++;ps+=v;h+=y.get(i);}}if(n>0)e+=(double)n/p.size()*Math.abs(ps/n-(double)h/n);}return e;}
    private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R930_POLICY_REQUIRED");return v;}private static String hash(String x){String v=required(x).toLowerCase(Locale.US);if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R930_POLICY_HASH");return v;}private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}private static double boundedOrNaN(double x){return finite(x)&&x>=0&&x<=1?x:Double.NaN;}private static double nonNegativeOrNaN(double x){return finite(x)&&x>=0?x:Double.NaN;}private static String fmt(double x){return finite(x)?String.format(Locale.US,"%.8f",x):"N/A";}
}
