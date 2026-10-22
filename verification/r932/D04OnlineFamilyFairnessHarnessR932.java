package com.ethk3.novaonepro;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * D04 engineering acceptance for the two actual online research families.
 *
 * <p>This harness deliberately uses the production AdaptiveShadowLearnerEngine and the production
 * ResearchTinyDeepLearnerR909.Network. It compares them on the same five chronological D04 folds,
 * with one whole issue embargo before each TEST block. It is synthetic engineering evidence only;
 * it does not authorize REAL and does not claim Future-OOS effect.</p>
 *
 * <p>Host execution intentionally does not depend on android.jar's org.json classes. Those classes
 * are compile-time Android API stubs on a desktop JVM and throw RuntimeException("Stub!") when
 * instantiated. The tiny deterministic serializer below is verification-only and preserves the
 * exact receipt schema while keeping all production learner, fold and parity logic unchanged.</p>
 */
public final class D04OnlineFamilyFairnessHarnessR932 {
    private static final int N = 100;
    private static final int TRAIN_START = 0;
    private static final int TRAIN_STOP = 80;
    private static final int FOLDS = 5;

    private D04OnlineFamilyFairnessHarnessR932() {}

    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        System.out.println(name + "=PASS");
    }

    static String issue(int i) {
        return String.format(Locale.US, "20260911%04d", i + 1);
    }

    /** Seven legal predraw values matching the FTRL/TinyDeep research input width. */
    static double[] x(int i) {
        double phase = (i % 17) / 16.0d;
        double raw = 0.10d + 0.80d * phase;
        return new double[] {
            raw,
            Math.sin(i * 0.17d) * 1.8d,
            0.02d + (i % 5) * 0.025d,
            0.12d + (i % 7) * 0.10d,
            0.25d + (i % 6) * 0.11d,
            0.30d + (i % 4) * 0.15d,
            0.18d + (i % 9) * 0.075d
        };
    }

    static int y(int i) {
        double[] v = x(i);
        return (v[0] + 0.10d * v[1] + 0.12d * v[5] > 0.58d) ? 1 : 0;
    }

    private static int[] edges() {
        int first = Math.max(TRAIN_START + 21, TRAIN_START + (TRAIN_STOP - TRAIN_START) / 3);
        int[] e = new int[FOLDS + 1];
        for (int i = 0; i <= FOLDS; i++)
            e[i] = (int) Math.floor(first + (TRAIN_STOP - first) * (i / (double) FOLDS));
        e[FOLDS] = TRAIN_STOP;
        return e;
    }

    private static double clamp(double p) {
        return Math.max(1e-9d, Math.min(1.0d - 1e-9d, p));
    }

    /** Verification-only deterministic JSON writer; no Android runtime dependency. */
    private static final class Json {
        private Json() {}

        static String stringify(Object value) {
            StringBuilder out = new StringBuilder(4096);
            append(value, out, 0);
            return out.toString();
        }

        private static void append(Object value, StringBuilder out, int depth) {
            if (value == null) {
                out.append("null");
            } else if (value instanceof String) {
                quote((String) value, out);
            } else if (value instanceof Boolean || value instanceof Integer || value instanceof Long
                    || value instanceof Short || value instanceof Byte) {
                out.append(value.toString());
            } else if (value instanceof Number) {
                double d = ((Number) value).doubleValue();
                if (!Double.isFinite(d)) throw new AssertionError("D04_JSON_NONFINITE_NUMBER:" + value);
                out.append(value.toString());
            } else if (value instanceof Map) {
                appendMap((Map<?, ?>) value, out, depth);
            } else if (value instanceof Iterable) {
                appendIterable((Iterable<?>) value, out, depth);
            } else {
                throw new AssertionError("D04_JSON_UNSUPPORTED_TYPE:" + value.getClass().getName());
            }
        }

        private static void appendMap(Map<?, ?> map, StringBuilder out, int depth) {
            out.append('{');
            if (!map.isEmpty()) out.append('\n');
            int index = 0;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!(entry.getKey() instanceof String))
                    throw new AssertionError("D04_JSON_NON_STRING_KEY");
                indent(out, depth + 1);
                quote((String) entry.getKey(), out);
                out.append(": ");
                append(entry.getValue(), out, depth + 1);
                if (++index < map.size()) out.append(',');
                out.append('\n');
            }
            if (!map.isEmpty()) indent(out, depth);
            out.append('}');
        }

        private static void appendIterable(Iterable<?> values, StringBuilder out, int depth) {
            Iterator<?> it = values.iterator();
            out.append('[');
            if (!it.hasNext()) {
                out.append(']');
                return;
            }
            out.append('\n');
            while (it.hasNext()) {
                indent(out, depth + 1);
                append(it.next(), out, depth + 1);
                if (it.hasNext()) out.append(',');
                out.append('\n');
            }
            indent(out, depth);
            out.append(']');
        }

        private static void indent(StringBuilder out, int depth) {
            for (int i = 0; i < depth * 2; i++) out.append(' ');
        }

        private static void quote(String value, StringBuilder out) {
            out.append('"');
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                switch (c) {
                    case '"': out.append("\\\""); break;
                    case '\\': out.append("\\\\"); break;
                    case '\b': out.append("\\b"); break;
                    case '\f': out.append("\\f"); break;
                    case '\n': out.append("\\n"); break;
                    case '\r': out.append("\\r"); break;
                    case '\t': out.append("\\t"); break;
                    default:
                        if (c < 0x20) {
                            String hex = Integer.toHexString(c);
                            out.append("\\u");
                            for (int p = hex.length(); p < 4; p++) out.append('0');
                            out.append(hex);
                        } else {
                            out.append(c);
                        }
                }
            }
            out.append('"');
        }
    }

    private static final class Metric {
        final int n, hits;
        final double logloss, brier, p95Ms;
        Metric(int n, int hits, double logloss, double brier, double p95Ms) {
            this.n=n;this.hits=hits;this.logloss=logloss;this.brier=brier;this.p95Ms=p95Ms;
        }
        Map<String,Object> json() {
            Map<String,Object> o=new LinkedHashMap<String,Object>();
            o.put("n_issues",n);o.put("hits",hits);
            o.put("accuracy",n==0?0.0d:hits/(double)n);o.put("log_loss",logloss);
            o.put("brier",brier);o.put("host_p95_ms",p95Ms);return o;
        }
    }

    private interface Predictor { double predict(int index); }

    private static Metric metric(int start, int stop, Predictor predictor) {
        ArrayList<Long> nanos=new ArrayList<Long>();double ll=0,brier=0;int hit=0;
        for(int i=start;i<stop;i++){
            long t=System.nanoTime();double p=clamp(predictor.predict(i));nanos.add(System.nanoTime()-t);
            int label=y(i),guess=p>=0.5d?1:0;if(guess==label)hit++;
            ll+=-(label*Math.log(p)+(1-label)*Math.log(1-p));double d=p-label;brier+=d*d;
        }
        Collections.sort(nanos);int n=stop-start;int q=Math.max(0,(int)Math.ceil(.95*nanos.size())-1);
        double p95=nanos.isEmpty()?0:nanos.get(q)/1e6;
        return new Metric(n,hit,ll/Math.max(1,n),brier/Math.max(1,n),p95);
    }

    private static String choose(Metric baseline, Metric ftrl, Metric tiny) {
        String chosen="BASELINE_RAW_ACTION_P";double best=baseline.logloss;
        // Strict improvement only. Equal or worse results retain the simple baseline.
        if(ftrl.logloss < best-1e-12){best=ftrl.logloss;chosen="ONLINE_FTRL";}
        if(tiny.logloss < best-1e-12){chosen="ONLINE_TINY_DEEP";}
        return chosen;
    }

    static AdaptiveShadowLearnerEngine trainFtrl(int stopExclusive) {
        AdaptiveShadowLearnerEngine learner=new AdaptiveShadowLearnerEngine();
        for(int i=TRAIN_START;i<stopExclusive;i++){
            long frozen=1_000_000L+i*10L+1L,open=frozen+5L;
            AdaptiveShadowLearnerEngine.Update u=learner.updateAfterSettlement(
                    new AdaptiveShadowLearnerEngine.OosRow(issue(i),frozen,open,true,y(i),x(i)));
            if(!u.updated)throw new AssertionError("FTRL_TRAIN:"+i+":"+u.status);
        }
        ResearchArtifactCodecR811.Artifact artifact=learner.snapshotResearchArtifact();
        byte[] encoded=ResearchArtifactCodecR811.encode(artifact);
        String hash=ResearchArtifactCodecR811.hash(artifact);
        if(!ResearchArtifactCodecR811.verify(encoded,hash))throw new AssertionError("FTRL_ARTIFACT_VERIFY");
        AdaptiveShadowLearnerEngine restored=AdaptiveShadowLearnerEngine.restore(ResearchArtifactCodecR811.decode(encoded));
        if(!hash.equals(ResearchArtifactCodecR811.hash(restored.snapshotResearchArtifact())))
            throw new AssertionError("FTRL_ARTIFACT_ROUNDTRIP");
        return restored;
    }

    static ResearchTinyDeepLearnerR909.Network trainTiny(int stopExclusive) {
        ResearchTinyDeepLearnerR909.Network n=ResearchTinyDeepLearnerR909.Network.initial();
        for(int i=TRAIN_START;i<stopExclusive;i++)n.update(x(i),y(i));
        ResearchArtifactCodecR811.Artifact artifact=n.toArtifact(issue(stopExclusive-1));
        byte[] encoded=ResearchArtifactCodecR811.encode(artifact);String hash=ResearchArtifactCodecR811.hash(artifact);
        if(!ResearchArtifactCodecR811.verify(encoded,hash))throw new AssertionError("TINY_ARTIFACT_VERIFY");
        ResearchTinyDeepLearnerR909.Network restored=ResearchTinyDeepLearnerR909.Network.from(ResearchArtifactCodecR811.decode(encoded));
        if(Math.abs(restored.predict(x(stopExclusive))-n.predict(x(stopExclusive)))>1e-12)
            throw new AssertionError("TINY_ARTIFACT_ROUNDTRIP");
        return restored;
    }

    private static void writeParityVector(String path) throws Exception {
        final AdaptiveShadowLearnerEngine ftrl=trainFtrl(30);
        final ResearchTinyDeepLearnerR909.Network tiny=trainTiny(30);
        DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)));
        try {
            for(int i=30;i<60;i++)out.writeDouble(ftrl.predictResearch(x(i)));
            for(int i=30;i<60;i++)out.writeDouble(tiny.predict(x(i)));
        } finally { out.close(); }
    }

    public static void main(String[] args) throws Exception {
        check(!ResearchTinyDeepLearnerR909.MAY_AUTHORIZE_REAL,"TINY_REAL_AUTHORITY_ZERO");
        int[] e=edges();List<Map<String,Object>> foldRows=new ArrayList<Map<String,Object>>();
        double maxHostP95=0;int baselineRetained=0;
        for(int f=0;f<FOLDS;f++){
            int start=e[f],stop=e[f+1],cut=start-1; // [cut,start) is one whole issue embargo.
            if(cut<=TRAIN_START||start>=stop)throw new AssertionError("D04_FOLD_SUPPORT");
            final AdaptiveShadowLearnerEngine ftrl=trainFtrl(cut);
            final ResearchTinyDeepLearnerR909.Network tiny=trainTiny(cut);
            Metric base=metric(start,stop,new Predictor(){public double predict(int i){return clamp(x(i)[0]);}});
            Metric fm=metric(start,stop,new Predictor(){public double predict(int i){return ftrl.predictResearch(x(i));}});
            Metric tm=metric(start,stop,new Predictor(){public double predict(int i){return tiny.predict(x(i));}});
            maxHostP95=Math.max(maxHostP95,Math.max(fm.p95Ms,tm.p95Ms));
            String selected=choose(base,fm,tm);if(selected.startsWith("BASELINE"))baselineRetained++;
            Map<String,Object> row=new LinkedHashMap<String,Object>();
            row.put("fold",f);row.put("train",Arrays.asList(TRAIN_START,cut));
            row.put("embargo",Arrays.asList(cut,start));row.put("test",Arrays.asList(start,stop));
            row.put("BASELINE_RAW_ACTION_P",base.json());row.put("ONLINE_FTRL",fm.json());
            row.put("ONLINE_TINY_DEEP",tm.json());row.put("selected",selected);
            foldRows.add(row);
        }
        check(foldRows.size()==5,"D04_ONLINE_FIVE_FOLDS");
        check(e[0]==26&&e[1]==36&&e[2]==47&&e[3]==58&&e[4]==69&&e[5]==80,"D04_FOLD_MANIFEST_MATCHES_DEEP_CANDIDATES");
        check(maxHostP95<5000.0d,"D04_ONLINE_HOST_INFERENCE_WITHIN_5S_BUDGET");
        Metric equal=new Metric(10,5,.7,.25,0);check("BASELINE_RAW_ACTION_P".equals(choose(equal,equal,equal)),"D04_NO_GAIN_RETAINS_BASELINE");

        AdaptiveShadowLearnerEngine negative=trainFtrl(20);String before=ResearchArtifactCodecR811.hash(negative.snapshotResearchArtifact());
        AdaptiveShadowLearnerEngine.Update invalid=negative.updateAfterSettlement(new AdaptiveShadowLearnerEngine.OosRow(issue(20),1,2,true,1,new double[]{Double.NaN,0,0,0,0,0,0}));
        check(!invalid.updated&&before.equals(ResearchArtifactCodecR811.hash(negative.snapshotResearchArtifact())),"D04_FAILED_FTRL_ATTEMPT_RETAINED_WITHOUT_MUTATION");
        Map<String,double[]> brokenFields=new LinkedHashMap<String,double[]>();brokenFields.put("w1",new double[]{1});
        ResearchArtifactCodecR811.Artifact broken=new ResearchArtifactCodecR811.Artifact(ResearchTinyDeepLearnerR909.ARTIFACT_TYPE,issue(19),brokenFields);
        boolean rejected=false;try{ResearchTinyDeepLearnerR909.Network.from(broken);}catch(RuntimeException expected){rejected=true;}
        check(rejected,"D04_FAILED_TINY_ARTIFACT_RECORDED_REJECTED");

        Map<String,Object> report=new LinkedHashMap<String,Object>();
        report.put("status","PASS");report.put("scope","SYNTHETIC_SAME_FOLD_ACTUAL_PRODUCTION_ONLINE_LEARNERS_NOT_FUTURE_EFFECT");
        report.put("statistical_unit","issue");report.put("same_folds",true);report.put("whole_issue_embargo",true);report.put("folds",foldRows);
        report.put("max_host_p95_ms",maxHostP95);report.put("mainline_budget_ms",5000);report.put("baseline_retained_fold_count",baselineRetained);
        report.put("may_authorize_real",false);report.put("effect_proven",false);
        if(args.length>0)Files.write(Paths.get(args[0]),(Json.stringify(report)+"\n").getBytes(StandardCharsets.UTF_8));
        if(args.length>1)writeParityVector(args[1]);
        System.out.println("R932_D04_ONLINE_FAIRNESS_PASS|folds=5|max_host_p95_ms="+maxHostP95);
    }
}
