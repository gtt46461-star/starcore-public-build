package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * R908 autonomous historical strategy laboratory.
 *
 * <p>This is deliberately not a parameter wrapper around Bayesian/KNN/forest/neural models.  Its
 * raw material is only the immutable pre-draw history.  For every new history boundary it mines
 * empirical transition, cross-position migration, short/long residual, motif successor, gap and
 * slope surfaces, measures each primitive with chronological walk-forward checks, then synthesizes
 * several structurally different executable scoring programs.  Each exact program has a stable
 * method signature, Chinese semantic name, frozen trained-until issue and independent OOS ledger.
 * Production/REAL authority is always zero.</p>
 */
public final class HistoricalStrategyInventionR908 {
    private static final Object PROFILE_CACHE_LOCK = new Object();
    private static volatile CachedProfile cachedProfile;
    public static final String OWNER = "RESEARCH_ONLY_HISTORICAL_PROGRAM_SYNTHESIS";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    static final int HISTORY_MAX = 480;
    private static final int MIN_HISTORY = 72;
    private static final int WALK_FORWARD_MAX = 48;
    private static final Pattern TERM = Pattern.compile("([+-]?[0-9]+(?:\\.[0-9]+)?)\\*([a-z_]+)");
    private static final String[] ATOMS = new String[] {
        "position_transition_lift", "cross_position_migration", "short_long_residual",
        "motif_similarity", "gap_pressure", "frequency_slope"
    };

    private HistoricalStrategyInventionR908() {}

    /** Called only from the isolated R908 Research looper. */
    public static String maintain(final DbStore store) {
        if (store == null) return "STORE_NULL";
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return "YIELD_P0_PRESSURE";
        List<Models.Draw> history = clean(store.getRecentDraws(HISTORY_MAX));
        if (history.size() < MIN_HISTORY) return "WAIT_HISTORY_" + history.size();
        String until = history.get(history.size() - 1).issue;
        int strictN = strictOosCount(store);
        int lastN = parseInt(store.getState("r908_invention_last_oos_n", "-1"), -1);
        int existing = empiricalAssetCount(store);
        if (existing > 0 && lastN >= 0 && strictN - lastN < 8)
            return "WAIT_NEW_STRICT_OOS|delta=" + (strictN-lastN) + "|need=8|N=" + strictN;

        Reliability reliability = walkForward(history);
        List<ResearchMethodPlanR906> plans = synthesize(reliability, until, nextGeneration(store));
        final ArrayList<String> registered = new ArrayList<String>();
        Boolean ok = store.coordinatorR36().runWrite(
                "R908_HISTORY_INVENTION_REGISTER",
                new DatabaseCoordinator.WriteCallable<Boolean>() {
                    public Boolean call(SQLiteDatabase db) {
                        for (int i = 0; i < plans.size(); i++) {
                            ResearchAssetRegistryR906.Asset a = ResearchAssetRegistryR906.register(db, plans.get(i), null);
                            if (a != null) registered.add(a.assetId + ':' + a.displayNameZh);
                        }
                        return Boolean.TRUE;
                    }
                }, Boolean.FALSE);
        if (ok == null || !ok.booleanValue()) return "REGISTER_FAILED";
        store.setState("r908_invention_last_history_issue", until);
        store.setState("r908_invention_last_oos_n", String.valueOf(strictN));
        store.setState("r908_invention_last_reliability", reliability.canonical());
        store.setState("r908_invention_last_assets", registered.toString());
        store.setState("r908_invention_last_count", String.valueOf(registered.size()));
        store.setState("r908_invention_last_at", String.valueOf(System.currentTimeMillis()));
        return "CREATED_OR_REUSED=" + registered.size() + "|until=" + until + "|" + reliability.canonical();
    }

    public static Profile buildProfile(DbStore store) {
        return store == null ? Profile.empty() : Profile.from(clean(store.getRecentDraws(HISTORY_MAX)));
    }

    /**
     * Point-in-time profile for a durable pre-draw snapshot. Draws newer than {@code baseIssue} are
     * removed before any atom is computed, so a delayed Research retry cannot leak outcomes.
     */
    public static Profile buildProfileAt(DbStore store, String baseIssue) {
        if (store == null || !TargetIssueResolver.isValid(baseIssue)) return Profile.empty();
        List<Models.Draw> recent = store.getRecentDraws(HISTORY_MAX + 64);
        ArrayList<Models.Draw> visible = new ArrayList<Models.Draw>();
        if (recent != null) {
            for (int i = 0; i < recent.size(); i++) {
                Models.Draw draw = recent.get(i);
                if (draw != null
                        && draw.isValid()
                        && TargetIssueResolver.isValid(draw.issue)
                        && TargetIssueResolver.compare(draw.issue, baseIssue) <= 0) {
                    visible.add(draw);
                }
            }
        }
        Collections.sort(visible, new Comparator<Models.Draw>() {
            @Override public int compare(Models.Draw left, Models.Draw right) {
                return TargetIssueResolver.compare(left.issue, right.issue);
            }
        });
        ArrayList<Models.Draw> dedup = new ArrayList<Models.Draw>();
        String previous = "";
        for (int i = 0; i < visible.size(); i++) {
            Models.Draw draw = visible.get(i);
            if (!draw.issue.equals(previous)) {
                dedup.add(draw);
                previous = draw.issue;
            }
        }
        if (dedup.isEmpty() || !baseIssue.equals(dedup.get(dedup.size() - 1).issue))
            return Profile.empty();
        if (dedup.size() > HISTORY_MAX)
            dedup = new ArrayList<Models.Draw>(
                    dedup.subList(dedup.size() - HISTORY_MAX, dedup.size()));
        // An issue alone is not an input identity: a second database or a corrected
        // unlearned draw can have the same issue and different upstream values.
        StringBuilder source = new StringBuilder("R945_PROFILE_V1|").append(baseIssue);
        for (Models.Draw draw : dedup) {
            source.append('|').append(draw.issue);
            for (int number : draw.numbers) source.append(',').append(number);
        }
        String inputHash = V51ChecksumEngine.sha256(source.toString());
        CachedProfile cached = cachedProfile;
        if (cached != null && inputHash.equals(cached.inputHash)) return cached.profile;
        Profile built = Profile.from(dedup);
        if (built.ready) {
            synchronized (PROFILE_CACHE_LOCK) {
                cachedProfile = new CachedProfile(inputHash, built);
            }
        }
        return built;
    }

    /** Executes one invented program over the exact 1200 immutable actions using one shared profile. */
    public static double[] evaluateAll(Profile profile, V51Models.PositionAction[] actions, ResearchMethodPlanR906 plan) {
        if (actions == null) return new double[0];
        double[] out = new double[actions.length];
        for (int i = 0; i < actions.length; i++) out[i] = evaluate(profile, actions[i], plan);
        return out;
    }

    public static double evaluate(Profile p, V51Models.PositionAction a, ResearchMethodPlanR906 plan) {
        if (p == null || !p.ready || a == null || plan == null) return 0.0d;
        Terms terms = Terms.parse(plan.feature);
        double[] x = new double[terms.names.size()];
        double weighted = 0.0d, weightAbs = 0.0d;
        for (int i = 0; i < x.length; i++) {
            x[i] = comboAtom(p, terms.names.get(i), a.position, a.n1, a.n2, a.n3);
            double w = terms.weights.get(i).doubleValue();
            weighted += w * x[i]; weightAbs += Math.abs(w);
        }
        double avg = weightAbs > 0.0d ? weighted / weightAbs : AppConfig.RANDOM_K3_BASELINE;
        String rep = upper(plan.representation);
        double score;
        if (rep.contains("INTERACTION")) {
            double interaction = x.length >= 2 ? Math.sqrt(clamp01(x[0]) * clamp01(x[1])) : avg;
            score = 0.58d * avg + 0.42d * interaction;
        } else if (rep.contains("BOTTLENECK")) {
            double min = 1.0d;
            for (int i = 0; i < x.length; i++) min = Math.min(min, clamp01(x[i]));
            score = 0.66d * avg + 0.34d * min;
        } else if (rep.contains("CONSENSUS")) {
            double mean = mean(x), sd = std(x, mean);
            score = 0.70d * avg + 0.30d * clamp01(mean - 0.55d * sd);
        } else if (rep.contains("REGIME_SWITCH")) {
            double regime = p.regime[Math.max(0, Math.min(9, a.position - 1))];
            double fast = valueOf(terms, x, "short_long_residual", avg);
            fast = 0.55d * fast + 0.45d * valueOf(terms, x, "frequency_slope", fast);
            double structural = valueOf(terms, x, "position_transition_lift", avg);
            structural = 0.5d * structural + 0.5d * valueOf(terms, x, "cross_position_migration", structural);
            score = regime >= 0.18d ? 0.70d * fast + 0.30d * avg : 0.68d * structural + 0.32d * avg;
        } else if (rep.contains("CONTRADICTION")) {
            double mean = mean(x), sd = std(x, mean);
            score = avg - 0.42d * sd;
        } else if (rep.contains("CASCADE")) {
            double lead = x.length > 0 ? x[0] : avg;
            double second = x.length > 1 ? x[1] : avg;
            score = lead >= 0.35d ? 0.62d * lead + 0.38d * second : 0.52d * avg + 0.48d * lead;
        } else if (rep.contains("MAX_SUPPORT")) {
            double mx = 0.0d; for (int i=0;i<x.length;i++) mx=Math.max(mx,clamp01(x[i]));
            score = 0.68d * avg + 0.32d * mx;
        } else {
            score = avg;
        }
        // This is a ranking score, not an asserted calibrated probability.
        return clamp01(score);
    }

    private static List<ResearchMethodPlanR906> synthesize(Reliability r, String until, int generation) {
        ArrayList<AtomWeight> ranked = new ArrayList<AtomWeight>();
        for (int i = 0; i < ATOMS.length; i++) ranked.add(new AtomWeight(ATOMS[i], r.weight[i], r.direction[i]));
        Collections.sort(ranked, new Comparator<AtomWeight>() {
            public int compare(AtomWeight a, AtomWeight b) { return Double.compare(b.weight, a.weight); }
        });
        // Keep a broad alphabet even when one primitive currently dominates; weak primitives get a
        // small coefficient and are later falsified by OOS/ablation rather than silently removed.
        String expression = expression(ranked, 5);
        String parent = "AUTO_HISTORY_WF_WF" + r.sample;
        String[] structures = new String[] {
            "HISTORICAL_SYNTHESIS_WEIGHTED", "HISTORICAL_SYNTHESIS_INTERACTION",
            "HISTORICAL_SYNTHESIS_BOTTLENECK", "HISTORICAL_SYNTHESIS_CONSENSUS",
            "HISTORICAL_SYNTHESIS_REGIME_SWITCH", "HISTORICAL_SYNTHESIS_CONTRADICTION",
            "HISTORICAL_SYNTHESIS_CASCADE", "HISTORICAL_SYNTHESIS_MAX_SUPPORT"
        };
        ArrayList<ResearchMethodPlanR906> out = new ArrayList<ResearchMethodPlanR906>();
        for (int i=0;i<structures.length;i++) {
            out.add(new ResearchMethodPlanR906(
                    structures[i], expression, "EMPIRICAL_SYMBOLIC_PROGRAM",
                    "OOS_EVOLVE_KEEP_MUTATE_PRUNE", "HIT_STABILITY_COVERAGE_PARETO",
                    "STRICT_OOS_ISOTONIC_IF_ELIGIBLE", "BLOCK_REGIME_AND_MAX_STREAK",
                    "WALK_FORWARD_ABLATION_COUNTERFACTUAL", parent, generation,
                    "HISTORY_DISCOVERY_THEN_STRUCTURE_SEARCH_" + i, true));
        }
        return out;
    }

    private static String expression(List<AtomWeight> ranked, int count) {
        double sum=0.0d; for(int i=0;i<count && i<ranked.size();i++) sum+=Math.max(0.02d,ranked.get(i).weight);
        StringBuilder b=new StringBuilder();
        for(int i=0;i<count && i<ranked.size();i++){
            AtomWeight a=ranked.get(i); double w=Math.max(0.02d,a.weight)/Math.max(0.0001d,sum);
            if(b.length()>0)b.append('+');
            if(a.direction<0) w=-w;
            b.append(String.format(Locale.US,"%.6f",w)).append('*').append(a.name);
        }
        return b.toString();
    }

    /** Chronological per-primitive top-K walk-forward; no target row enters its own profile. */
    private static Reliability walkForward(List<Models.Draw> history) {
        int start = Math.max(36, history.size() - WALK_FORWARD_MAX);
        double[] posHit=new double[ATOMS.length], negHit=new double[ATOMS.length];
        int tests=0;
        for(int t=start;t<history.size();t++){
            Profile p=Profile.from(history.subList(0,t));
            if(!p.ready)continue;
            Models.Draw actual=history.get(t);
            for(int pos=1;pos<=10;pos++){
                int y=actual.getNumberAtPosition(pos); tests++;
                for(int a=0;a<ATOMS.length;a++){
                    double[] v=new double[10];for(int n=1;n<=10;n++)v[n-1]=p.atom(ATOMS[a],pos,n);
                    if(inTop3(v,y,true))posHit[a]++;
                    if(inTop3(v,y,false))negHit[a]++;
                }
            }
        }
        Reliability r=new Reliability();r.sample=tests;
        for(int a=0;a<ATOMS.length;a++){
            double hp=tests==0?0.30d:posHit[a]/tests, hn=tests==0?0.30d:negHit[a]/tests;
            r.hitRate[a]=Math.max(hp,hn);r.direction[a]=hp>=hn?1:-1;
            // zero means indistinguishable from random; positive weights grow only with observed WF lift.
            r.weight[a]=0.02d+Math.max(0.0d,r.hitRate[a]-0.30d);
        }
        return r;
    }

    private static boolean inTop3(double[] v,int number,boolean high){
        int[] idx=new int[]{0,1,2,3,4,5,6,7,8,9};
        for(int i=0;i<3;i++){int best=i;for(int j=i+1;j<10;j++)if(high?v[idx[j]]>v[idx[best]]:v[idx[j]]<v[idx[best]])best=j;int tmp=idx[i];idx[i]=idx[best];idx[best]=tmp;}
        return idx[0]==number-1||idx[1]==number-1||idx[2]==number-1;
    }

    private static double comboAtom(Profile p,String atom,int position,int n1,int n2,int n3){
        return clamp01(p.atom(atom,position,n1)+p.atom(atom,position,n2)+p.atom(atom,position,n3));
    }

    private static int strictOosCount(DbStore store){Cursor c=null;try{c=store.readable().rawQuery("SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1",null);return c.moveToFirst()?c.getInt(0):0;}finally{if(c!=null)c.close();}}
    private static int empiricalAssetCount(DbStore store){Cursor c=null;try{c=store.readable().rawQuery("SELECT COUNT(1) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE learner_family='EMPIRICAL_SYMBOLIC_PROGRAM'",null);return c.moveToFirst()?c.getInt(0):0;}finally{if(c!=null)c.close();}}
    private static int parseInt(String x,int d){try{return Integer.parseInt(x);}catch(Throwable e){return d;}}

    private static int nextGeneration(DbStore store){
        Cursor c=null;try{c=store.readable().rawQuery("SELECT COALESCE(MAX(generation),0) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE learner_family='EMPIRICAL_SYMBOLIC_PROGRAM'",null);return c.moveToFirst()?Math.max(1,c.getInt(0)+1):1;}finally{if(c!=null)c.close();}
    }

    private static List<Models.Draw> clean(List<Models.Draw> input){
        ArrayList<Models.Draw> out=new ArrayList<Models.Draw>();if(input==null)return out;
        String last="";for(Models.Draw d:input)if(d!=null&&d.isValid()&&TargetIssueResolver.isValid(d.issue)){if(!d.issue.equals(last)){out.add(d);last=d.issue;}}
        return out;
    }

    public static final class Profile {
        public final boolean ready;
        private final Map<String,double[][]> atom;
        public final double[] regime;
        /** Immutable PIT draw sequence retained for the R920 named-atom materializer. */
        private final List<Models.Draw> history;
        private Profile(boolean ready,Map<String,double[][]> atom,double[] regime,List<Models.Draw> history){
            this.ready=ready;this.atom=atom;this.regime=regime;
            this.history=history==null?Collections.<Models.Draw>emptyList():history;
        }
        static Profile empty(){return new Profile(false,new HashMap<String,double[][]>(),new double[10],Collections.<Models.Draw>emptyList());}
        static Profile from(List<Models.Draw> h){
            if(h==null||h.size()<24)return empty();
            HashMap<String,double[][]> m=new HashMap<String,double[][]>();for(String a:ATOMS)m.put(a,new double[10][10]);
            double[] regime=new double[10];Models.Draw last=h.get(h.size()-1);
            // Position transition: P(next number | latest number at same position).
            for(int p=1;p<=10;p++){
                int condition=last.getNumberAtPosition(p);double[] count=new double[10];for(int n=0;n<10;n++)count[n]=0.6d;double total=6.0d;
                for(int i=1;i<h.size();i++)if(h.get(i-1).getNumberAtPosition(p)==condition){int y=h.get(i).getNumberAtPosition(p);count[y-1]++;total++;}
                for(int n=0;n<10;n++)m.get("position_transition_lift")[p-1][n]=count[n]/total;
            }
            // Cross-position migration: where a number moves next, conditioned on its current position.
            for(int n=1;n<=10;n++){
                int currentPos=findPosition(last,n);double[] dst=new double[10];for(int p=0;p<10;p++)dst[p]=0.6d;double total=6.0d;
                for(int i=1;i<h.size();i++)if(findPosition(h.get(i-1),n)==currentPos){int d=findPosition(h.get(i),n);if(d>0){dst[d-1]++;total++;}}
                for(int p=1;p<=10;p++)m.get("cross_position_migration")[p-1][n-1]=dst[p-1]/total;
            }
            // Short/long residual, frequency slope and gap surface directly from raw history.
            for(int p=1;p<=10;p++){
                double tv=0.0d;double[] longF=freq(h,p,Math.min(120,h.size()),0);double[] shortF=freq(h,p,Math.min(24,h.size()),0);double[] fast=freq(h,p,Math.min(12,h.size()),0);double[] prior=freq(h,p,Math.min(36,Math.max(0,h.size()-12)),12);
                for(int n=1;n<=10;n++){
                    m.get("short_long_residual")[p-1][n-1]=clamp01(0.5d+3.0d*(shortF[n-1]-longF[n-1]));
                    m.get("frequency_slope")[p-1][n-1]=clamp01(0.5d+3.0d*(fast[n-1]-prior[n-1]));
                    int gap=gap(h,p,n,60);m.get("gap_pressure")[p-1][n-1]=clamp01(gap/30.0d);
                    tv+=Math.abs(shortF[n-1]-longF[n-1]);
                }
                regime[p-1]=Math.min(1.0d,0.5d*tv);
            }
            // Motif successor: historical draws most similar to current permutation vote for the next value.
            for(int p=1;p<=10;p++){
                double[] vote=new double[10];double total=0.0d;
                for(int i=0;i<h.size()-1;i++){
                    double sim=permutationSimilarity(last,h.get(i));if(sim<0.30d)continue;
                    int y=h.get(i+1).getNumberAtPosition(p);double w=sim*sim;vote[y-1]+=w;total+=w;
                }
                if(total<=0.0d){for(int n=0;n<10;n++)vote[n]=0.10d;}else for(int n=0;n<10;n++)vote[n]=(vote[n]+0.1d)/(total+1.0d);
                for(int n=0;n<10;n++)m.get("motif_similarity")[p-1][n]=vote[n];
            }
            ArrayList<Models.Draw> frozenHistory=new ArrayList<Models.Draw>(h.size());
            for(Models.Draw draw:h)if(draw!=null)frozenHistory.add(draw.copy());
            return new Profile(true,m,regime,Collections.unmodifiableList(frozenHistory));
        }
        public double atom(String name,int position,int number){double[][] x=atom.get(name);if(x==null||position<1||position>10||number<1||number>10)return 0.0d;return clamp01(x[position-1][number-1]);}
        /** Package-private, read-only and already bounded to the legal PIT history window. */
        List<Models.Draw> historyForGenesisR920(){return history;}
    }

    /** One immutable point-in-time profile shared by formula, asset and algorithm consumers. */
    private static final class CachedProfile {
        final String inputHash;
        final Profile profile;
        CachedProfile(String inputHash, Profile profile) {
            this.inputHash = inputHash;
            this.profile = profile;
        }
    }

    private static double[] freq(List<Models.Draw> h,int p,int take,int skipNewest){double[] c=new double[10];if(take<=0)return c;int end=Math.max(0,h.size()-skipNewest);int start=Math.max(0,end-take);double total=0;for(int i=start;i<end;i++){int n=h.get(i).getNumberAtPosition(p);if(n>=1&&n<=10){c[n-1]++;total++;}}if(total<=0)return c;for(int n=0;n<10;n++)c[n]/=total;return c;}
    private static int gap(List<Models.Draw>h,int p,int n,int max){int g=0;for(int i=h.size()-1;i>=0&&g<max;i--,g++)if(h.get(i).getNumberAtPosition(p)==n)return g;return max;}
    private static int findPosition(Models.Draw d,int n){if(d==null)return 0;for(int p=1;p<=10;p++)if(d.getNumberAtPosition(p)==n)return p;return 0;}
    private static double permutationSimilarity(Models.Draw a,Models.Draw b){if(a==null||b==null)return 0;double s=0;for(int p=1;p<=10;p++){int x=a.getNumberAtPosition(p),y=b.getNumberAtPosition(p);if(x==y)s+=1.0d;else{int bx=findPosition(b,x);s+=Math.max(0.0d,1.0d-Math.abs(bx-p)/5.0d)*0.25d;}}return s/10.0d;}
    private static double valueOf(Terms t,double[]x,String name,double fallback){for(int i=0;i<t.names.size();i++)if(name.equals(t.names.get(i)))return x[i];return fallback;}
    private static double mean(double[]x){if(x==null||x.length==0)return 0;double s=0;for(double v:x)s+=v;return s/x.length;}
    private static double std(double[]x,double m){if(x==null||x.length==0)return 0;double s=0;for(double v:x){double d=v-m;s+=d*d;}return Math.sqrt(s/x.length);}
    private static double clamp01(double x){return Math.max(0.0d,Math.min(1.0d,Double.isNaN(x)||Double.isInfinite(x)?0.0d:x));}
    private static String upper(String x){return x==null?"":x.toUpperCase(Locale.US);}

    private static final class Terms{
        final ArrayList<String>names=new ArrayList<String>();final ArrayList<Double>weights=new ArrayList<Double>();
        static Terms parse(String expression){Terms t=new Terms();Matcher m=TERM.matcher(expression==null?"":expression.replace(" ",""));while(m.find()){try{double w=Double.parseDouble(m.group(1));String n=m.group(2);if(ResearchFeatureGrammarR906.allowed(n)){t.names.add(n);t.weights.add(Double.valueOf(w));}}catch(Throwable ignore){}}return t;}
    }
    private static final class AtomWeight{final String name;final double weight;final int direction;AtomWeight(String n,double w,int d){name=n;weight=w;direction=d;}}
    private static final class Reliability{
        int sample;final double[]hitRate=new double[ATOMS.length];final double[]weight=new double[ATOMS.length];final int[]direction=new int[ATOMS.length];
        String canonical(){StringBuilder b=new StringBuilder("WF_N=").append(sample);for(int i=0;i<ATOMS.length;i++)b.append('|').append(ATOMS[i]).append('=').append(String.format(Locale.US,"%.4f",hitRate[i])).append(direction[i]>=0?"+":"-");return b.toString();}
    }
}
