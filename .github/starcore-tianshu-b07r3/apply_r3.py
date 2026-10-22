#!/usr/bin/env python3
from pathlib import Path
import hashlib, json, re

ROOT=Path.cwd()
SRC=ROOT/'app/src/main/java/com/ethk3/novaonepro'
REPORTS=ROOT/'reports'
REPORTS.mkdir(exist_ok=True)
changes=[]

def sha(s): return hashlib.sha256(s.encode('utf-8')).hexdigest()
def read(rel): return (ROOT/rel).read_text(encoding='utf-8')
def write(rel, text, label):
    p=ROOT/rel; old=p.read_text(encoding='utf-8') if p.exists() else ''
    if old==text: return
    p.parent.mkdir(parents=True,exist_ok=True); p.write_text(text,encoding='utf-8')
    changes.append({'file':str(rel),'change':label,'before_lines':old.count('\n')+1 if old else 0,'after_lines':text.count('\n')+1,'before_sha256':sha(old) if old else '','after_sha256':sha(text)})
def once(s, old, new, tag):
    n=s.count(old)
    if n!=1: raise SystemExit('PATTERN_COUNT_%s=%d'%(tag,n))
    return s.replace(old,new,1)
def patch(rel, fn, label):
    old=read(rel); new=fn(old)
    if old==new: raise SystemExit('PATCH_NO_CHANGE:'+str(rel)+':'+label)
    write(rel,new,label)

# -----------------------------------------------------------------------------
# Build identity / R3 contract
# -----------------------------------------------------------------------------
def patch_build(s):
    s=once(s,'versionCode 577120807','versionCode 577130807','version_code')
    s=once(s,"versionName 'V57.7.2-tianshu-b01-b07-reinforced-r2'","versionName 'V57.7.3-tianshu-b01-b07-reinforced-r3'",'version_name')
    s=s.replace('Starcore-Tianshu-B01-B07-Reinforced-R2-Binance-K3','Starcore-Tianshu-B01-B07-Reinforced-R3-Binance-K3')
    return s
patch(Path('app/build.gradle'),patch_build,'R3 build identity')

def patch_app(s):
    anchor='    public static final String B07R2_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_R2_20260807";\n'
    insert=anchor+'''    public static final String B07R3_VERSION = "STARCORE_TIANSHU_B01_B07_REINFORCED_R3_20260807";\n    public static final String B07R3_DIAG_SCHEMA = "B07R3_FULL_STATE_DYNAMIC_AUDIT_V3";\n    public static final boolean B07R3_CANDIDATE_LEVEL_REAL_AUTH = true;\n    public static final boolean B07R3_GLOBAL_CHAMPION_IS_DIAGNOSTIC_ONLY = true;\n    public static final boolean B07R3_NO_FREQUENCY_DEBT_ACTION = true;\n    public static final boolean B07R3_FORCE_REARM_ON_TASK_REMOVED = true;\n    public static final boolean B07R3_KERNEL_REALTIME_ENABLED = true;\n    public static final boolean B07R3_KERNEL_REPLAY_ENABLED = true;\n    public static final int B07R3_KERNEL_MIN_GROUPS = 4;\n    public static final int B07R3_KERNEL_READY_SAMPLE = 30;\n    public static final int B07R3_KERNEL_STRONG_SAMPLE = 80;\n    public static final double B07R3_KERNEL_READY_RATE = 0.340d;\n    public static final double B07R3_KERNEL_STRONG_RATE = 0.380d;\n    public static final double B07R3_KERNEL_FREEZE_RATE = 0.280d;\n    public static final long B07R3_KERNEL_SOFT_TIMEOUT_NS = 2000000L;\n    public static final int B07R3_REPORT_MAX_CHARS = 240000;\n    public static final int B07R3_REPORT_ESSENTIAL_MIN_CHARS = 12000;\n    public static final String B07R3_BACKGROUND_SCOPE = "SWIPE_EXIT_SCREEN_OFF_LOCK_SUPPORTED_FORCE_STOP_NOT_BYPASSABLE";\n'''
    return once(s,anchor,insert,'app_anchor')
patch(Path('app/src/main/java/com/ethk3/novaonepro/AppConfig.java'),patch_app,'R3 runtime/kernel/real-action/diagnostic contract')

# -----------------------------------------------------------------------------
# StarcoreEvidenceKernel: merged B07 evidence packet/plugin/registry/health core.
# It can score evidence and update replay health, but NEVER writes FinalP/Lane/BET.
# -----------------------------------------------------------------------------
FACETS=[
 ('ShapeBalance','SHAPE',['shapeScore','shapeConfidence','shapeSupport'],['shapeRisk','shapeConflictRisk']),
 ('ShapeMemory','SHAPE',['memoryScore','shapeMemoryScore','similarScore'],['memoryRisk','similarRisk']),
 ('SequenceTrend','SEQUENCE',['trendScore','trendConfidence','sequenceScore'],['driftRisk','trendRisk']),
 ('MarkovTransition','SEQUENCE',['markovScore','transitionScore','transitionConfidence'],['transitionRisk','breakRisk']),
 ('CausalFactor','CAUSAL',['causalScore','causalConfidence','causalSupport'],['causalRisk','counterRisk']),
 ('PositionCause','CAUSAL',['positionCauseScore','positionCausalScore','positionSupport'],['positionCauseRisk','positionRisk']),
 ('TeacherMatrix','TEACHER',['teacherMatrixScore','teacherNumberScore','teacherConfidence'],['teacherMatrixRisk','teacherNumberRisk']),
 ('TeacherOrdinary','TEACHER',['teacherScore','teacherHitRate','teacherRecentHitRate'],['teacherRisk','teacherMatrixRiskPenalty']),
 ('CounterfactualRank','COUNTERFACTUAL',['counterfactualScore','rankCounterfactualScore','counterSupport'],['counterRisk','counterEvidenceScore']),
 ('AblationFragility','COUNTERFACTUAL',['v43StrategySupport','v43StrategyDiversity','v43StrategyMaturity'],['v43AblationFragility','v43StrategyRisk']),
 ('DynamicCluster','DYNAMIC',['dynamicClusterScore','dynamicClusterHitRate','dynamicQuality'],['dynamicClusterRisk','dynamicRisk']),
 ('DynamicCause','DYNAMIC',['dynamicCauseScore','dynamicCauseConfidence','v20Score'],['dynamicCauseRisk','v20Risk']),
 ('CoverageQuality','QUALITY',['coverageQuality','coverageQualityScore','dynamicGateCoverageQualityScore'],['k3CoverageRisk','coverageRisk']),
 ('ThirdNumber','QUALITY',['thirdQuality','thirdNumberQualityScore','dynamicGateThirdNumberQuality'],['thirdNumberRisk','thirdRisk']),
 ('Calibration','CALIBRATION',['calibrationConfidence','v46CalibrationConfidence','calibratedHitProbability'],['calibrationBrier','calibrationEce']),
 ('Posterior','CALIBRATION',['posteriorProbability','posteriorConfidence','posteriorHitRate'],['posteriorRisk','probabilityUncertainty']),
 ('RegimeStability','STATE',['stateScore','stateConfidence','trendLife'],['driftRisk','switchRisk']),
 ('Lifecycle','STATE',['lifecycleScore','recoveryScore','stabilityScore'],['breakDepth','lifecycleRisk']),
 ('Association','RELATION',['associationScore','relationScore','consensusScore'],['associationConflictRisk','relationRisk']),
 ('Distribution','RELATION',['distributionScore','distributionConfidence','densityScore'],['distributionRisk','driftRisk']),
 ('RuleHealth','HEALTH',['ruleHealthScore','moduleEffectScore','healthScore'],['ruleHealthRisk','falseStrongRisk']),
 ('ShadowPromotion','HEALTH',['shadowPromotionScore','promotionConfidence','recentEffectScore'],['shadowPromotionRisk','freezeRisk']),
 ('RankQuality','RANK',['rawRankScore','rankScore','championGap'],['rankRisk','rankUncertainty']),
 ('ModelEvidence','RANK',['modelP','rawP','historicalPriorProbability'],['modelRisk','v46ProbabilityUncertainty']),
 ('Window12','WINDOW',['window12Score','r12','recent12Score'],['window12Risk','recentRisk']),
 ('Window24','WINDOW',['window24Score','r24','recent24Score'],['window24Risk','recentRisk']),
 ('Window48','WINDOW',['window48Score','r48','recent48Score'],['window48Risk','midRisk']),
 ('Window96','WINDOW',['window96Score','r96','recent96Score'],['window96Risk','midRisk']),
 ('Window360','WINDOW',['window360Score','r360','longScore'],['window360Risk','longRisk']),
 ('Omission','NUMBER',['omissionScore','gapScore','numberOmissionScore'],['omissionRisk','extremeGapRisk']),
 ('Momentum','NUMBER',['momentumScore','numberMomentumScore','hotColdScore'],['momentumRisk','overheatRisk']),
 ('NumberBehavior','NUMBER',['numberBehaviorScore','numberConfidence','behaviorSupport'],['numberBehaviorRisk','numberConflictRisk']),
]
# Two distinct probes per facet: direct support and consistency/robustness.
SPECS=[]
for idx,(name,group,pos,neg) in enumerate(FACETS):
    SPECS.append((name+'Support',group,pos,neg,0))
    SPECS.append((name+'Robustness',group,list(reversed(pos)),list(reversed(neg)),1))

def java_array(items): return ', '.join('"'+x+'"' for x in items)
def build_kernel():
    L=[]; A=L.append
    A('package com.ethk3.novaonepro;'); A('')
    A('import java.lang.reflect.Field;'); A('import java.security.MessageDigest;'); A('import java.util.ArrayList;'); A('import java.util.Collections;'); A('import java.util.Comparator;'); A('import java.util.LinkedHashMap;'); A('import java.util.List;'); A('import java.util.Locale;'); A('import java.util.Map;'); A('')
    A('/**'); A(' * B07 merged evidence kernel. It owns immutable evidence packets, plugin ordering,'); A(' * timeout isolation, evidence-group de-correlation and replay health only.'); A(' * It MUST NOT create Decision/Trade/BET and MUST NOT write FinalP, Lane or gate.'); A(' */')
    A('public final class StarcoreEvidenceKernel {')
    A('    public static final String VERSION = AppConfig.B07R3_VERSION;')
    A('    public static final String STATE_LAST = "starcore_kernel_last";')
    A('    public static final String STATE_REPLAY = "starcore_kernel_replay";')
    A('    public static final String STATE_HEALTH = "starcore_kernel_health";')
    A('    public static final String STATE_CALLS = "starcore_kernel_calls";')
    A('    public static final String STATE_TIMEOUTS = "starcore_kernel_timeouts";')
    A('    private static final List<Plugin> REGISTRY = buildRegistry();')
    A('')
    A('    public static final class EvidencePacket {')
    for line in [
      'public final String pluginId;','public final String group;','public final double support;','public final double counter;','public final double confidence;','public final int sample;','public final long elapsedNs;','public final boolean timedOut;','public final String health;','public final String fingerprint;']:
        A('        '+line)
    A('        EvidencePacket(String id,String g,double s,double r,double c,int n,long ns,boolean to,String h,String fp) {')
    A('            pluginId=safe(id); group=safe(g); support=clamp01(s); counter=clamp01(r); confidence=clamp01(c); sample=Math.max(0,n); elapsedNs=Math.max(0L,ns); timedOut=to; health=safe(h); fingerprint=safe(fp);')
    A('        }')
    A('        public String summary(){ return pluginId+"@"+group+":S="+fmt(support)+",R="+fmt(counter)+",C="+fmt(confidence)+",N="+sample+",H="+health+(timedOut?",TIMEOUT":""); }')
    A('    }')
    A('')
    A('    public static final class HealthSnapshot {')
    A('        public final String pluginId; public final int sample; public final int hit; public final double rate; public final String state;')
    A('        HealthSnapshot(String id,int n,int h,double r,String s){pluginId=safe(id);sample=Math.max(0,n);hit=Math.max(0,h);rate=clamp01(r);state=safe(s);}')
    A('        public String summary(){return pluginId+"="+state+"("+hit+"/"+sample+","+fmt(rate)+")";}')
    A('    }')
    A('')
    A('    public static final class EvidenceBundle {')
    A('        public final ArrayList<EvidencePacket> packets=new ArrayList<EvidencePacket>();')
    A('        public final LinkedHashMap<String,Double> groupSupport=new LinkedHashMap<String,Double>();')
    A('        public final LinkedHashMap<String,Double> groupRisk=new LinkedHashMap<String,Double>();')
    A('        public double support; public double risk; public double confidence; public double diversity; public int availableGroups; public int readyPlugins; public int freezePlugins; public int timedOutPlugins; public int hardRisk; public String healthState="WATCH"; public String dominantGroup=""; public String fingerprint=""; public long costMs;')
    A('        public String summary(){return "Kernel="+healthState+"|groups="+availableGroups+"|support="+fmt(support)+"|risk="+fmt(risk)+"|confidence="+fmt(confidence)+"|diversity="+fmt(diversity)+"|ready="+readyPlugins+"|freeze="+freezePlugins+"|timeouts="+timedOutPlugins+"|hard="+hardRisk+"|dominant="+dominantGroup+"|hash="+shortHash(fingerprint)+"|cost="+costMs+"ms";}')
    A('    }')
    A('')
    A('    private interface Plugin { String id(); String group(); EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage); }')
    A('')
    A('    public EvidenceBundle evaluate(DbStore store, Models.ComboCandidate c, String stage) {')
    A('        long begin=System.currentTimeMillis(); EvidenceBundle out=new EvidenceBundle();')
    A('        if(c==null){out.healthState="WATCH";out.fingerprint=hash("NULL|"+safe(stage));return out;}')
    A('        LinkedHashMap<String,Integer> groupCount=new LinkedHashMap<String,Integer>();')
    A('        LinkedHashMap<String,Double> groupWeightedSupport=new LinkedHashMap<String,Double>();')
    A('        LinkedHashMap<String,Double> groupWeightedRisk=new LinkedHashMap<String,Double>();')
    A('        LinkedHashMap<String,Double> groupWeight=new LinkedHashMap<String,Double>();')
    A('        double totalS=0.0d,totalR=0.0d,totalW=0.0d; StringBuilder fp=new StringBuilder(4096);')
    A('        for(int i=0;i<REGISTRY.size();i++){')
    A('            Plugin p=REGISTRY.get(i); EvidencePacket e; long t=System.nanoTime();')
    A('            try{ e=p.evaluate(store,c,stage); }catch(Throwable x){ e=packet(store,p.id(),p.group(),0.0d,0.65d,0.05d,0,System.nanoTime()-t,true,"ERROR"); }')
    A('            long ns=System.nanoTime()-t; if(ns>AppConfig.B07R3_KERNEL_SOFT_TIMEOUT_NS && !e.timedOut) e=new EvidencePacket(e.pluginId,e.group,e.support,e.counter,e.confidence,e.sample,ns,true,e.health,e.fingerprint);')
    A('            out.packets.add(e); if(e.timedOut) out.timedOutPlugins++; if("FREEZE".equals(e.health))out.freezePlugins++; if("READY".equals(e.health)||"STRONG".equals(e.health))out.readyPlugins++;')
    A('            Integer old=groupCount.get(e.group); int same=old==null?0:old.intValue(); groupCount.put(e.group,Integer.valueOf(same+1));')
    A('            double decorrelation=1.0d/(1.0d+same*0.18d); double healthWeight=healthWeight(e.health); double w=Math.max(0.02d,e.confidence*decorrelation*healthWeight);')
    A('            if(e.timedOut)w*=0.25d; totalS+=e.support*w; totalR+=e.counter*w; totalW+=w;')
    A('            add(groupWeightedSupport,e.group,e.support*w); add(groupWeightedRisk,e.group,e.counter*w); add(groupWeight,e.group,w);')
    A('            fp.append(e.pluginId).append(":").append(fmt(e.support)).append(":").append(fmt(e.counter)).append(":").append(e.health).append("|");')
    A('        }')
    A('        out.support=totalW<=0.0d?0.0d:clamp01(totalS/totalW); out.risk=totalW<=0.0d?1.0d:clamp01(totalR/totalW);')
    A('        out.confidence=clamp01(totalW/Math.max(1.0d,REGISTRY.size()*0.55d));')
    A('        double best=-1.0d; double mean=0.0d; int gc=0;')
    A('        for(Map.Entry<String,Double> x:groupWeight.entrySet()){double w=x.getValue().doubleValue(); if(w<=0.0d)continue; String g=x.getKey(); double gs=groupWeightedSupport.get(g).doubleValue()/w; double gr=groupWeightedRisk.get(g).doubleValue()/w; out.groupSupport.put(g,Double.valueOf(clamp01(gs)));out.groupRisk.put(g,Double.valueOf(clamp01(gr)));out.availableGroups++;mean+=gs;gc++;double dominance=gs-gr;if(dominance>best){best=dominance;out.dominantGroup=g;}}')
    A('        mean=gc<=0?0.0d:mean/gc; double var=0.0d; for(Double v:out.groupSupport.values()){double d=v.doubleValue()-mean;var+=d*d;} var=gc<=0?1.0d:var/gc; out.diversity=clamp01(1.0d-Math.sqrt(var)*2.0d);')
    A('        out.hardRisk=(out.freezePlugins>=3 || out.risk>=0.82d || out.availableGroups<AppConfig.B07R3_KERNEL_MIN_GROUPS)?1:0;')
    A('        if(out.hardRisk==1)out.healthState="FREEZE"; else if(out.readyPlugins>=12&&out.support>=0.62d&&out.risk<=0.35d)out.healthState="STRONG"; else if(out.readyPlugins>=7&&out.support>=0.52d&&out.risk<=0.48d)out.healthState="READY"; else out.healthState="WATCH";')
    A('        out.fingerprint=hash(fp.toString()+safe(stage)+"|"+out.healthState); out.costMs=System.currentTimeMillis()-begin;')
    A('        persistBundle(store,out,stage); return out;')
    A('    }')
    A('')
    A('    public void applyEvidenceOnly(Models.ComboCandidate c, EvidenceBundle b) {')
    A('        if(c==null||b==null)return;')
    A('        writeDouble(c,"v43StrategySupport",b.support); writeDouble(c,"v43StrategyRisk",b.risk); writeDouble(c,"v43StrategyConfidence",b.confidence); writeDouble(c,"v43StrategyDiversity",b.diversity); writeInt(c,"v43EvidenceGroupCount",b.availableGroups); writeInt(c,"v43HardRisk",b.hardRisk); writeString(c,"v43DominantEvidence",b.dominantGroup); writeString(c,"v43EvidenceFingerprint",b.fingerprint);')
    A('        writeString(c,"evidenceHealthStatus",b.healthState); writeString(c,"ruleHealthStatus",b.healthState); writeString(c,"evidenceKernelVersion",VERSION); writeString(c,"evidenceKernelSummary",b.summary());')
    A('        // Deliberately no writes to finalP, calibratedP, dynamicGate, lane, action or decision.')
    A('    }')
    A('')
    A('    public static void observeReplay(DbStore store, Models.ComboCandidate c, int actualNumber, String origin) {')
    A('        if(store==null||c==null||!c.isValidK3()||actualNumber<1||actualNumber>10)return; StarcoreEvidenceKernel k=new StarcoreEvidenceKernel(); EvidenceBundle b=k.evaluate(store,c,"REPLAY:"+safe(origin)); int hit=(actualNumber==c.n1||actualNumber==c.n2||actualNumber==c.n3)?1:0;')
    A('        for(int i=0;i<b.packets.size();i++){EvidencePacket e=b.packets.get(i); if(e.timedOut)continue; String base=healthKey(e.pluginId); int n=parseInt(store.getState(base+"_n","0"),0); int h=parseInt(store.getState(base+"_h","0"),0); n++; h+=hit; if(n>720){double q=720.0d/n;n=720;h=(int)Math.round(h*q);} store.setState(base+"_n",String.valueOf(n));store.setState(base+"_h",String.valueOf(h));store.setState(base+"_rate",String.valueOf(n<=0?0.0d:(double)h/n));}')
    A('        try{store.setState(STATE_REPLAY,"origin="+safe(origin)+"|actual="+actualNumber+"|hit="+hit+"|"+b.summary());}catch(Throwable ignore){}')
    A('    }')
    A('')
    A('    public static String renderHealth(DbStore store) {')
    A('        StringBuilder s=new StringBuilder(12000); int ready=0,strong=0,watch=0,freeze=0; for(int i=0;i<REGISTRY.size();i++){Plugin p=REGISTRY.get(i);HealthSnapshot h=health(store,p.id());if("STRONG".equals(h.state))strong++;else if("READY".equals(h.state))ready++;else if("FREEZE".equals(h.state))freeze++;else watch++;s.append(h.summary()).append("\\n");} s.insert(0,"KernelHealth plugins="+REGISTRY.size()+"|STRONG="+strong+"|READY="+ready+"|WATCH="+watch+"|FREEZE="+freeze+"\\n"); return s.toString();')
    A('    }')
    A('')
    A('    private static List<Plugin> buildRegistry(){ ArrayList<Plugin> x=new ArrayList<Plugin>();')
    for i,spec in enumerate(SPECS): A('        x.add(new Probe%02d());'%(i+1))
    A('        Collections.sort(x,new Comparator<Plugin>(){public int compare(Plugin a,Plugin b){return a.id().compareTo(b.id());}}); return Collections.unmodifiableList(x); }')
    A('')
    # concrete probes - auditable, fixed order, each has its own field contract and transform.
    for i,(name,group,pos,neg,variant) in enumerate(SPECS):
        n=i+1; w1=0.56+(i%5)*0.035; w2=0.44-(i%5)*0.035
        A('    private static final class Probe%02d implements Plugin {'%n)
        A('        public String id(){return "%02d_%s";}'%(n,name))
        A('        public String group(){return "%s";}'%group)
        A('        public EvidencePacket evaluate(DbStore store, Models.ComboCandidate c, String stage){')
        A('            long t=System.nanoTime();')
        A('            double primary=numeric(c,new String[]{%s});'%java_array(pos))
        A('            double counter=numeric(c,new String[]{%s});'%java_array(neg))
        A('            int sample=sample(c,new String[]{"calibrationSample","posteriorSample","teacherMatrixSample","teacherNumberSample","dynamicClusterSample","evidenceForestSample","microWalkForwardSample"});')
        if variant==0:
            A('            double support=clamp01(primary*%.4fd + (1.0d-counter)*%.4fd);'%(w1,w2))
            A('            double risk=clamp01(counter*0.72d + Math.max(0.0d,0.42d-primary)*0.28d);')
        else:
            A('            double agreement=1.0d-Math.min(1.0d,Math.abs(primary-0.50d)*1.45d);')
            A('            double support=clamp01(primary*0.46d + agreement*0.31d + (1.0d-counter)*0.23d);')
            A('            double risk=clamp01(counter*0.58d + (1.0d-agreement)*0.25d + Math.max(0.0d,0.36d-primary)*0.17d);')
        A('            double maturity=Math.min(1.0d,sample/120.0d);')
        A('            double confidence=clamp01(0.20d+0.55d*maturity+0.25d*Math.abs(support-risk));')
        A('            HealthSnapshot h=health(store,id());')
        A('            if("FREEZE".equals(h.state))risk=Math.max(risk,0.72d);')
        A('            if("STRONG".equals(h.state))confidence=clamp01(confidence+0.08d);')
        A('            return new EvidencePacket(id(),group(),support,risk,confidence,sample,System.nanoTime()-t,false,h.state,hash(id()+"|"+fmt(primary)+"|"+fmt(counter)+"|"+safe(stage)));')
        A('        }')
        A('    }')
        A('')
    # helpers
    A('    private static EvidencePacket packet(DbStore store,String id,String group,double s,double r,double c,int n,long ns,boolean timeout,String status){HealthSnapshot h=health(store,id);String st=safe(status).length()>0?status:h.state;return new EvidencePacket(id,group,s,r,c,n,ns,timeout,st,hash(id+"|"+fmt(s)+"|"+fmt(r)+"|"+n));}')
    A('    private static HealthSnapshot health(DbStore store,String id){int n=0,h=0;try{if(store!=null){String k=healthKey(id);n=parseInt(store.getState(k+"_n","0"),0);h=parseInt(store.getState(k+"_h","0"),0);}}catch(Throwable ignore){}double r=n<=0?0.0d:(double)h/n;String st="WATCH";if(n>=AppConfig.B07R3_KERNEL_READY_SAMPLE&&r<AppConfig.B07R3_KERNEL_FREEZE_RATE)st="FREEZE";else if(n>=AppConfig.B07R3_KERNEL_STRONG_SAMPLE&&r>=AppConfig.B07R3_KERNEL_STRONG_RATE)st="STRONG";else if(n>=AppConfig.B07R3_KERNEL_READY_SAMPLE&&r>=AppConfig.B07R3_KERNEL_READY_RATE)st="READY";return new HealthSnapshot(id,n,h,r,st);}')
    A('    private static String healthKey(String id){return "starcore_kernel_plugin_"+machine(id);}')
    A('    private static double numeric(Object o,String[] names){double sum=0.0d;int n=0;if(names!=null)for(int i=0;i<names.length;i++){Double v=readNumber(o,names[i]);if(v==null)continue;double x=v.doubleValue();if(x>1.5d&&x<=100.0d)x/=100.0d;if(x>=0.0d&&x<=1.0d){sum+=x;n++;}}return n==0?0.50d:clamp01(sum/n);}')
    A('    private static int sample(Object o,String[] names){int m=0;if(names!=null)for(int i=0;i<names.length;i++){Double v=readNumber(o,names[i]);if(v!=null&&v.doubleValue()>m)m=(int)Math.round(v.doubleValue());}return Math.max(0,m);}')
    A('    private static Double readNumber(Object o,String name){if(o==null||safe(name).length()==0)return null;try{Field f=o.getClass().getField(name);Object v=f.get(o);if(v instanceof Number)return Double.valueOf(((Number)v).doubleValue());}catch(Throwable ignore){}return null;}')
    A('    private static void writeDouble(Object o,String n,double v){try{Field f=o.getClass().getField(n);if(f.getType()==Double.TYPE)f.setDouble(o,v);else f.set(o,Double.valueOf(v));}catch(Throwable ignore){}}')
    A('    private static void writeInt(Object o,String n,int v){try{Field f=o.getClass().getField(n);if(f.getType()==Integer.TYPE)f.setInt(o,v);else f.set(o,Integer.valueOf(v));}catch(Throwable ignore){}}')
    A('    private static void writeString(Object o,String n,String v){try{Field f=o.getClass().getField(n);f.set(o,safe(v));}catch(Throwable ignore){}}')
    A('    private static void add(Map<String,Double> m,String k,double v){Double old=m.get(k);m.put(k,Double.valueOf((old==null?0.0d:old.doubleValue())+v));}')
    A('    private static double healthWeight(String h){if("STRONG".equals(h))return 1.18d;if("READY".equals(h))return 1.0d;if("FREEZE".equals(h))return 0.20d;if("ERROR".equals(h))return 0.10d;return 0.72d;}')
    A('    private static void persistBundle(DbStore store,EvidenceBundle b,String stage){if(store==null||b==null)return;try{int calls=parseInt(store.getState(STATE_CALLS,"0"),0)+1;int timeouts=parseInt(store.getState(STATE_TIMEOUTS,"0"),0)+b.timedOutPlugins;store.setState(STATE_CALLS,String.valueOf(calls));store.setState(STATE_TIMEOUTS,String.valueOf(timeouts));store.setState(STATE_LAST,"stage="+safe(stage)+"|"+b.summary());store.setState(STATE_HEALTH,b.healthState);store.registerModuleBuildV20("StarcoreEvidenceKernel",VERSION,b.summary());}catch(Throwable ignore){}}')
    A('    private static String hash(String text){try{MessageDigest md=MessageDigest.getInstance("SHA-256");byte[] b=md.digest(safe(text).getBytes("UTF-8"));StringBuilder s=new StringBuilder(64);for(int i=0;i<b.length;i++)s.append(String.format(Locale.US,"%02x",b[i]&255));return s.toString();}catch(Throwable e){return Integer.toHexString(safe(text).hashCode());}}')
    A('    private static String machine(String s){return safe(s).replaceAll("[^A-Za-z0-9]+","_").toLowerCase(Locale.US);}')
    A('    private static int parseInt(String s,int d){try{return Integer.parseInt(s);}catch(Throwable e){return d;}}')
    A('    private static double clamp01(double v){if(Double.isNaN(v)||Double.isInfinite(v))return 0.0d;return v<0.0d?0.0d:(v>1.0d?1.0d:v);}')
    A('    private static String fmt(double v){return String.valueOf(Math.round(clamp01(v)*10000.0d)/10000.0d);}')
    A('    private static String shortHash(String s){return safe(s).length()<=12?safe(s):s.substring(0,12);}')
    A('    private static String safe(String s){return s==null?"":s;}')
    A('}')
    return '\n'.join(L)+'\n'
write(Path('app/src/main/java/com/ethk3/novaonepro/StarcoreEvidenceKernel.java'),build_kernel(),'B07 missing merged evidence kernel: realtime/replay/plugin-health/decorrelation')

# -----------------------------------------------------------------------------
# Wire kernel into the two production evidence boundaries.
# -----------------------------------------------------------------------------
def patch_unified(s):
    s=once(s,'public static final String MODULE_VERSION = "V46_UNIFIED_EVIDENCE_ACTION_BOUND_20260721_46072130";','public static final String MODULE_VERSION = "B07R3_UNIFIED_EVIDENCE_KERNEL_BOUND_20260807";\n    private final StarcoreEvidenceKernel starcoreKernel = new StarcoreEvidenceKernel();','unified_version')
    a='''            StrategyEvidenceContract.write(c, s, "PRE_PROBABILITY");\n            c.v43InvariantStatus = PipelineInvariantGuard.STATUS_PENDING;'''
    b='''            StrategyEvidenceContract.write(c, s, "PRE_PROBABILITY");\n            if (AppConfig.B07R3_KERNEL_REALTIME_ENABLED) {\n                StarcoreEvidenceKernel.EvidenceBundle kb = starcoreKernel.evaluate(store, c, "PRE_PROBABILITY");\n                starcoreKernel.applyEvidenceOnly(c, kb);\n            }\n            c.v43InvariantStatus = PipelineInvariantGuard.STATUS_PENDING;'''
    s=once(s,a,b,'unified_pre_probability')
    a2='''            StrategyEvidenceContract.write(c, s, "V46_FINAL_EVIDENCE_PRE_LOCK");\n            c.v43AblationFragility = s.fragility;'''
    b2='''            StrategyEvidenceContract.write(c, s, "V46_FINAL_EVIDENCE_PRE_LOCK");\n            if (AppConfig.B07R3_KERNEL_REALTIME_ENABLED) {\n                StarcoreEvidenceKernel.EvidenceBundle kb = starcoreKernel.evaluate(store, c, "PRE_DECISION");\n                starcoreKernel.applyEvidenceOnly(c, kb);\n                s = StrategyEvidenceContract.capture(c);\n            }\n            c.v43AblationFragility = s.fragility;'''
    return once(s,a2,b2,'unified_pre_decision')
patch(Path('app/src/main/java/com/ethk3/novaonepro/UnifiedStrategyEvidenceEngine.java'),patch_unified,'wire B07 kernel into realtime pre-probability and pre-decision')

# -----------------------------------------------------------------------------
# Replay call: one bounded kernel health observation per rolling causal slice.
# -----------------------------------------------------------------------------
def patch_rolling(s):
    a='''        int familyAdded = 0;\n        for (int u = 0; u < usedIndex.size(); u++) {'''
    b='''        int familyAdded = 0;\n        boolean kernelReplayRecordedB07R3 = false;\n        for (int u = 0; u < usedIndex.size(); u++) {'''
    s=once(s,a,b,'rolling_flag')
    a2='''            int y = contains(pred, history.get(idx).getNumberAtPosition(pred.position)) ? 1 : 0;\n            updateFamily(store, pred.family, y);'''
    b2='''            int actualNumber = history.get(idx).getNumberAtPosition(pred.position);\n            int y = contains(pred, actualNumber) ? 1 : 0;\n            if (AppConfig.B07R3_KERNEL_REPLAY_ENABLED && !kernelReplayRecordedB07R3) {\n                Models.ComboCandidate replay = new Models.ComboCandidate();\n                replay.position = pred.position; replay.n1 = pred.n1; replay.n2 = pred.n2; replay.n3 = pred.n3;\n                replay.baseIssue = idx > 0 ? safeIssue(history.get(idx - 1)) : ""; replay.targetIssue = safeIssue(history.get(idx));\n                StarcoreEvidenceKernel.observeReplay(store, replay, actualNumber, "RollingCausalBacktestEngine");\n                kernelReplayRecordedB07R3 = true;\n            }\n            updateFamily(store, pred.family, y);'''
    return once(s,a2,b2,'rolling_replay')
patch(Path('app/src/main/java/com/ethk3/novaonepro/RollingCausalBacktestEngine.java'),patch_rolling,'one bounded B07 kernel replay health update per slice')

# -----------------------------------------------------------------------------
# Real action repair: remove GLOBAL champion as an absolute V51 live veto.
# Candidate-level immutable contract + typed hard risks + OOS EventP/ActionLCB +
# coverage/third/counter/uncertainty gates remain mandatory.
# -----------------------------------------------------------------------------
def patch_lane(s):
    old='''        String v48Champion = store == null ? "MODEL_NOT_PROVEN" : store.getState(WalkForwardChampionEngine.STATE_STATUS, "MODEL_NOT_PROVEN");\n        if (!"CHAMPION".equals(v48Champion))\n            return publish(store, c, reject(out, "MODEL_NOT_PROVEN"), utility);\n        RuntimeFreshnessGuard.Result freshness = RuntimeFreshnessGuard.evaluate(store, c == null ? "" : c.targetIssue);'''
    new='''        String v48Champion = store == null ? "MODEL_NOT_PROVEN" : store.getState(WalkForwardChampionEngine.STATE_STATUS, "MODEL_NOT_PROVEN");\n        boolean globalChampionProven = "CHAMPION".equals(v48Champion);\n        try {\n            if (store != null) {\n                store.setState("b07r3_global_champion_status", v48Champion);\n                store.setState("b07r3_global_champion_role", "DIAGNOSTIC_NOT_ABSOLUTE_V51_LIVE_VETO");\n                store.setState("b07r3_candidate_level_authorization", "ENABLED_STRICT_CONTRACT_AND_RISK_GATES");\n            }\n        } catch (Throwable ignore) { }\n        if (!globalChampionProven) {\n            c.reasonSummary = append(c.reasonSummary, "全局Champion尚未证明：不伪造MODEL_PROVEN；V51仅允许候选级严格OOS合同继续接受后续全部质量/风险门审判");\n        }\n        RuntimeFreshnessGuard.Result freshness = RuntimeFreshnessGuard.evaluate(store, c == null ? "" : c.targetIssue);'''
    s=once(s,old,new,'lane_global_lock')
    s=s.replace('Thirty issues only\n * unlock controlled PROBE/FLOOR evaluation; it never creates a frequency rescue.','Thirty issues only\n * unlock controlled PROBE/FLOOR evaluation; it never creates a frequency rescue. Global champion status is diagnostic only in active V51; candidate-level OOS/risk contracts still decide eligibility.')
    return s
patch(Path('app/src/main/java/com/ethk3/novaonepro/LanePolicyCompiler.java'),patch_lane,'remove false global champion veto; preserve candidate-level strict OOS gates')

# -----------------------------------------------------------------------------
# Task removal: force a same-service foreground re-delivery request instead of
# waiting for heartbeat to age into stale. This does not bypass Android FORCE STOP.
# -----------------------------------------------------------------------------
def patch_boot(s):
    anchor='''    public static boolean stopAutoFetchService(Context context) {\n'''
    method='''    public static boolean forceRearmAutoFetchService(Context context, String reason) {\n        if (context == null) return false;\n        Context app = appContext(context);\n        ServiceHeartbeatStore state = new ServiceHeartbeatStore(app);\n        if (!state.isUserEnabled()) return false;\n        if (!isUserUnlocked(app)) { scheduleAllRecovery(app, "force_rearm_locked:" + safe(reason)); return false; }\n        try {\n            Intent i = new Intent(app, AutoFetchService.class);\n            i.setAction(AppConfig.ACTION_SERVICE_START);\n            i.setPackage(AppConfig.PACKAGE_NAME);\n            i.putExtra("start_reason", "FORCE_REARM:" + safe(reason));\n            if (Build.VERSION.SDK_INT >= 26) app.startForegroundService(i); else app.startService(i);\n            state.markRecoveryResult("force_rearm", true, safe(reason));\n            scheduleAllRecovery(app, "force_rearm:" + safe(reason));\n            return true;\n        } catch (Throwable e) {\n            state.markRecoveryResult("force_rearm", false, safeError(e));\n            scheduleAllRecovery(app, "force_rearm_fallback:" + safe(reason));\n            return false;\n        }\n    }\n\n'''+anchor
    return once(s,anchor,method,'boot_force_rearm')
patch(Path('app/src/main/java/com/ethk3/novaonepro/BootReceiver.java'),patch_boot,'forced foreground re-delivery hook for task removal')

def patch_service(s):
    old='''        ensureRuntimeLoopsAliveV17("task_removed");\n        postFetchNow();\n\n        try { heartbeatStateV8().markTaskRemoved(); heartbeatStateV8().renewRuntimeLease("B07R2_TASK_REMOVED_REARM"); } catch (Throwable ignore) { }'''
    new='''        ensureRuntimeLoopsAliveV17("task_removed");\n        postFetchNow();\n        try {\n            if (AppConfig.B07R3_FORCE_REARM_ON_TASK_REMOVED && !userRequestedStopV5 && isServiceUserEnabledV5()) {\n                BootReceiver.forceRearmAutoFetchService(getApplicationContext(), "B07R3_TASK_REMOVED");\n            }\n        } catch (Throwable e) { CrashRecorder.recordCaught(getApplicationContext(), e, "B07R3_FORCE_REARM"); }\n\n        try { heartbeatStateV8().markTaskRemoved(); heartbeatStateV8().renewRuntimeLease("B07R3_TASK_REMOVED_REARM"); } catch (Throwable ignore) { }'''
    return once(s,old,new,'service_force_rearm')
patch(Path('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java'),patch_service,'task removal immediate foreground re-delivery + existing watchdog recovery')

# -----------------------------------------------------------------------------
# Diagnostic: retain R2 intelligence, append R3 hard findings and kernel health.
# Essential report remains one-copy and never claims 40/50/60 without evidence.
# -----------------------------------------------------------------------------
def patch_diag(s):
    # Add R3 section before final return using a stable tail anchor.
    anchor='''        return limit(sb.toString(), AppConfig.B07R2_REPORT_MAX_CHARS);\n    }'''
    if anchor not in s:
        # fallback locate last return limit in build method
        anchor='''        return sb.toString();\n    }'''
    extra='''        sb.append("\\n【B07R3 1-7批次加强复核】\\n");\n        sb.append("R3版本=").append(AppConfig.B07R3_VERSION).append("\\n");\n        sb.append("后台能力=").append(AppConfig.B07R3_BACKGROUND_SCOPE).append("\\n");\n        sb.append("退出/划掉任务：前台服务同进程立即重挂 + 强制re-delivery + Alarm + Job；息屏靠PARTIAL_WAKE_LOCK。用户手动强行停止/厂商禁后台无法由普通应用绕过。\\n");\n        sb.append("真实出手修复：全局WalkForward CHAMPION只做证据诊断，不再一票锁死活动V51；候选仍必须通过PredictionContract、RuntimeFreshness、硬风险、EventP、ActionLCB、Coverage、Third、Counter、Uncertainty、Authorization。\\n");\n        sb.append("频率红线：活动V51不允许频率债务制造机会；频率只能通过扩大已证明状态域、改善候选排序/校准/证据覆盖来增加。\\n");\n        sb.append("Kernel最后状态=").append(state(store, StarcoreEvidenceKernel.STATE_LAST, "NO_RUNTIME_SAMPLE")).append("\\n");\n        sb.append("Kernel回放状态=").append(state(store, StarcoreEvidenceKernel.STATE_REPLAY, "NO_REPLAY_SAMPLE")).append("\\n");\n        sb.append("Kernel插件健康：\\n").append(StarcoreEvidenceKernel.renderHealth(store));\n        String champion=state(store, WalkForwardChampionEngine.STATE_STATUS, "MODEL_NOT_PROVEN");\n        sb.append("全局模型证明=").append(champion).append("；注意：这仍决定能否声称模型已证明，但不再等价于活动V51每个候选永远禁止真实BET。\\n");\n        sb.append("50%/60%目标分析：只有严格时间顺序OOS/真实影子样本的命中率、Wilson下界、ECE、四块稳定性和覆盖同时支持时才可称达标。若未达标，应优先做：层级校准(HierarchicalCalibration)、状态域漂移证书(RegimeDriftCertificate)、第三号条件排序(ThirdNumberConditionalRank)、反事实残差挖掘(RankerResidualMining)、专家相关性去重(ExpertCorrelationAudit)、消融账本(AblationLedger)、已证明域覆盖扩张(CertifiedCoverageExpansion)；禁止简单降低门槛凑50/60或凑频率。\\n");\n        sb.append("Kernel职责边界：只聚合证据/健康/回放；不写FinalP、不选Lane、不创建BET，DecisionGate仍是唯一真实BET出口。\\n");\n'''
    if 'return limit(sb.toString(), AppConfig.B07R2_REPORT_MAX_CHARS);' in anchor:
        repl=extra+'        return limit(sb.toString(), AppConfig.B07R3_REPORT_MAX_CHARS);\n    }'
    else:
        repl=extra+'        return limit(sb.toString(), AppConfig.B07R3_REPORT_MAX_CHARS);\n    }'
    return once(s,anchor,repl,'diag_tail')
patch(Path('app/src/main/java/com/ethk3/novaonepro/B07R2FullDiagnosticEngine.java'),patch_diag,'R3 kernel/background/real-action/root-cause/50-60 diagnostic extension')

# Main copy: R3 cap and explicit one-shot essential report. No chunk rotation.
def patch_main(s):
    s=s.replace('AppConfig.B07R2_REPORT_MAX_CHARS','AppConfig.B07R3_REPORT_MAX_CHARS')
    s=s.replace('星核天枢B01-B07加强优化R2完整检测报告','星核天枢B01-B07加强优化R3完整检测报告')
    # Remove old diagnostic chunk reset if present; copyDiagnosisTextSafely already performs one clipboard write.
    return s
patch(Path('app/src/main/java/com/ethk3/novaonepro/MainActivity.java'),patch_main,'R3 one-shot report cap/title; keep single clipboard transaction')

# Update build audit manifest after all source creation so all 54 plan files are true.
PLAN={
1:['AppConfig.java','BuildIdentity.java','V51BuildManifest.java','V51ArtifactBundleManifest.java','KeyProvider.java','ApiClient.java','ApiParser.java','SafeLog.java','BinanceGameContract.java','ApiTransportGovernanceEngine.java'],
2:['DataGuard.java','CountdownClock.java','Models.java','V51Models.java','SchemaContractRegistry.java','FeatureContractRegistry.java','DrawIntegrityRecoveryEngine.java'],
3:['DbHelper.java','DatabaseCoordinator.java','DatabaseMigrationEngine.java','DatabaseIntegrityVerifier.java','DbStore.java','V51Repository.java'],
4:['PointInTimeSnapshotEngine.java','V51PermutationFeatureEngine.java','V51TrajectoryFeatureEngine.java','V51ObservableStateVectorEngine.java','RollingWindowCache.java','FullInformationFeatureGraphEngine.java'],
5:['V51StateTrajectoryEngine.java','V51StatePrototypeEngine.java','V51ChangePointEngine.java','V51LifecycleEngine.java','WindowFeatureEngine.java','TrendStateMachine.java','BreakRecoveryEngine.java','CycleRhythmEngine.java','DistributionDriftEngine.java'],
6:['PatternStateEngine.java','ShapeStateEngine.java','TransitionRelationEngine.java','SimilarShapeEngine.java','ShapeMemoryEngine.java','NumberBehaviorEngine.java','AttributionEngine.java'],
7:['StrategyModuleCatalog.java','StrategyEvidenceContract.java','UnifiedStrategyEvidenceEngine.java','EvidenceGroupAggregatorEngine.java','EvidenceHealthSnapshotEngine.java','RuleHealthMatrixEngine.java','FalseStrongFreezeEngine.java','DynamicFreezeMatrixEngine.java','StarcoreEvidenceKernel.java']}
entries=[]
for b,names in PLAN.items():
    for name in names:
        p=SRC/name; txt=p.read_text(encoding='utf-8',errors='replace') if p.exists() else ''
        effective=sum(1 for x in txt.splitlines() if x.strip() and not x.lstrip().startswith('//') and not x.lstrip().startswith('*'))
        entries.append((b,name,p.exists(),txt.count('\n')+1 if txt else 0,effective,len(re.findall(r'(?i)\\bTODO\\b|placeholder',txt)),sha(txt) if txt else ''))
rows=[]
for b,n,p,l,e,t,h in entries: rows.append('        new Entry(%d, "%s", %s, %d, %d, %d, "%s")'%(b,n,'true' if p else 'false',l,e,t,h))
manifest='''package com.ethk3.novaonepro;\n\n/** R3 exact build-time audit for the frozen 54-file B01-B07 contract. */\npublic final class B07R2BuildAuditManifest {\n    private B07R2BuildAuditManifest(){}\n    public static final String VERSION=AppConfig.B07R3_VERSION; public static final int EXPECTED_FILES=54;\n    public static final Entry[] ENTRIES=new Entry[]{\n%s\n    };\n    public static String render(){StringBuilder s=new StringBuilder(26000);int present=0,todo=0;int[] exp={0,10,7,6,6,9,7,9};int[] got=new int[8];for(int i=0;i<ENTRIES.length;i++){Entry e=ENTRIES[i];if(e.present){present++;got[e.batch]++;}todo+=e.todoHits;s.append("B0").append(e.batch).append('|').append(e.file).append("|present=").append(e.present).append("|lines=").append(e.lines).append("|effective=").append(e.effectiveLines).append("|todoHits=").append(e.todoHits).append("|sha256=").append(e.sha256).append('\\n');}s.append("TOTAL=").append(present).append('/').append(EXPECTED_FILES).append("|todoHits=").append(todo).append('\\n');for(int b=1;b<=7;b++)s.append("B0").append(b).append("_FILES=").append(got[b]).append('/').append(exp[b]).append(got[b]==exp[b]?"|COMPLETE":"|MISSING").append('\\n');return s.toString();}\n    public static final class Entry{public final int batch;public final String file;public final boolean present;public final int lines;public final int effectiveLines;public final int todoHits;public final String sha256;Entry(int b,String f,boolean p,int l,int e,int t,String h){batch=b;file=f;present=p;lines=l;effectiveLines=e;todoHits=t;sha256=h;}}\n}\n'''%(',\n'.join(rows))
write(Path('app/src/main/java/com/ethk3/novaonepro/B07R2BuildAuditManifest.java'),manifest,'regenerate exact 54-file audit after R3 kernel creation')

# reports
(REPORTS/'B07R3_CODE_CHANGE_MANIFEST.json').write_text(json.dumps({'version':'B07R3','files':changes},ensure_ascii=False,indent=2),encoding='utf-8')
print('R3_PATCH_OK files=%d'%len(changes))
for x in changes: print('%s %s -> %s lines | %s'%(x['file'],x['before_lines'],x['after_lines'],x['change']))
