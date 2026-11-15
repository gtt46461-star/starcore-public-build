package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * Live, incremental execution of R13 strategies (P06/P07): one call before each
 * draw (decide) and one after settlement (settle). Every strategy is computed and
 * settled every period whether it acts or not, its applicability learner keeps
 * training, and the shared models are updated once per draw.
 *
 * Contract: feeding draws 0..T-1 through Hub/ModelLiveSU2 produces exactly the
 * same act/hit/position/score sequence as ModelReplaySU2.evaluate on the same
 * draws (ModelLiveSU2Test, including a save/restore in the middle). History
 * training and live training are one code path.
 */
public final class ModelLiveSU2 implements LiveStrategySU2 {

    /** Shared online models, one instance per (kind, parameters), predicted once per period. */
    public static final class Hub {
        final LinkedHashMap<String, OnlineModelsSU2.Model> models = new LinkedHashMap<>();
        final Map<String, float[][]> p = new HashMap<>(); final Map<String, float[]> st = new HashMap<>();
        /** R14 (P36): online temperature calibrators for members used by calibrated strategies, and their tables. */
        final LinkedHashMap<String, CalibrationSU2.Live> cal = new LinkedHashMap<>(); final Map<String, float[][]> pc = new HashMap<>();
        long t; boolean predicted;
        public void calibrate(ModelGenomeSU2.Member m) {
            model(m); String k = m.modelKey();
            if (cal.containsKey(k)) return;
            if (t != 0) throw new IllegalStateException("SU2_HUB_CALIBRATOR_BORN_LATE:" + k);
            cal.put(k, new CalibrationSU2.Live());
        }
        /** Late calibrated member: replay the model and its calibrator over the stored history. */
        public void adoptCalibrated(ModelGenomeSU2.Member m, int[][] history) {
            if(WarmModelSeedR26.has(m.params))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED_FOR_REPLAY");
            String k = m.modelKey();
            if (cal.containsKey(k)) return;
            if (history.length != t) throw new IllegalStateException("SU2_HUB_ADOPT_HISTORY_LENGTH");
            OnlineModelsSU2.Model x = ModelReplaySU2.build(m.kind, m.params); CalibrationSU2.Live c = new CalibrationSU2.Live();
            float[][] q = new float[10][10]; float[] s = new float[x.stateWidth()];
            for (int[] d : history) { x.predict(q, s); c.predict(q); c.settle(q, d); x.update(d); }
            if (!models.containsKey(k)) models.put(k, x);
            cal.put(k, c); predicted = false; p.clear(); st.clear(); pc.clear();
        }
        public OnlineModelsSU2.Model model(ModelGenomeSU2.Member m) {
            String k = m.modelKey();
            OnlineModelsSU2.Model x = models.get(k);
            if (x == null) {
                if (t != 0) throw new IllegalStateException("SU2_HUB_MODEL_BORN_LATE:" + k); // late members bootstrap by replay (ModelReplaySU2)
                x = ModelReplaySU2.build(m.kind, m.params); models.put(k, x);
            }
            return x;
        }
        /** A member first used after genesis is rebuilt by replaying the stored history (same code path). */
        public void adopt(ModelGenomeSU2.Member m, int[][] history) {
            if(WarmModelSeedR26.has(m.params))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED_FOR_REPLAY");
            String k = m.modelKey();
            if (models.containsKey(k)) return;
            if (history.length != t) throw new IllegalStateException("SU2_HUB_ADOPT_HISTORY_LENGTH");
            OnlineModelsSU2.Model x = ModelReplaySU2.build(m.kind, m.params);
            float[][] q = new float[10][10]; float[] s = new float[x.stateWidth()];
            for (int[] d : history) { x.predict(q, s); x.update(d); }
            models.put(k, x); predicted = false; p.clear(); st.clear();
        }
        public void predict() {
            if (predicted) return;
            for (Map.Entry<String, OnlineModelsSU2.Model> e : models.entrySet()) {
                ExecutionBudgetR26.poll();float[][] q = new float[10][10]; float[] s = new float[e.getValue().stateWidth()];
                e.getValue().predict(q, s); p.put(e.getKey(), q); st.put(e.getKey(), s);
                CalibrationSU2.Live c = cal.get(e.getKey()); if (c != null) pc.put(e.getKey(), c.predict(q));
            }
            predicted = true;
        }
        public void update(int[] draw) {
            if (!predicted) predict();
            for (Map.Entry<String, CalibrationSU2.Live> e : cal.entrySet()) e.getValue().settle(p.get(e.getKey()), draw);
            for (OnlineModelsSU2.Model m : models.values()){ExecutionBudgetR26.poll();m.update(draw);} predicted = false; t++;
        }
        public void write(DataOutput o) throws IOException {
            o.writeLong(t); o.writeInt(models.size()); for (Map.Entry<String, OnlineModelsSU2.Model> e : models.entrySet()) { o.writeUTF(e.getKey()); e.getValue().write(o); }
            o.writeInt(cal.size()); for (Map.Entry<String, CalibrationSU2.Live> e : cal.entrySet()) { o.writeUTF(e.getKey()); e.getValue().write(o); }
        }
        /** P50: the models only; each calibration ring (~1 MB) is checkpointed as its own row (writeCal / readCal). */
        public void writeModels(DataOutput o) throws IOException {
            o.writeLong(t); o.writeInt(models.size()); for (Map.Entry<String, OnlineModelsSU2.Model> e : models.entrySet()) { o.writeUTF(e.getKey()); e.getValue().write(o); }
        }
        public void readModelsInto(DataInput in) throws IOException {
            long tt = in.readLong(); int n = in.readInt(); if (n != models.size()) throw new IOException("SU2_HUB_SHAPE");
            for (int i = 0; i < n; i++) { String k = in.readUTF(); OnlineModelsSU2.Model m = models.get(k); if (m == null) throw new IOException("SU2_HUB_KEY"); m.read(in); }
            t = tt; predicted = false;
        }
        public Set<String> calKeys() { return Collections.unmodifiableSet(cal.keySet()); }
        public void writeCal(String k, DataOutput o) throws IOException { cal.get(k).write(o); }
        public void readCal(String k, DataInput in) throws IOException { CalibrationSU2.Live c = cal.get(k); if (c == null) throw new IOException("SU2_HUB_CAL_KEY"); c.read(in); predicted = false; }
        public static String calOwner(String k) { return "CAL:" + HashingSU1.text("SU2_HUB_CAL_V1|" + k); }
        public void readInto(DataInput in) throws IOException {
            long tt = in.readLong(); int n = in.readInt(); if (n != models.size()) throw new IOException("SU2_HUB_SHAPE");
            for (int i = 0; i < n; i++) { String k = in.readUTF(); OnlineModelsSU2.Model m = models.get(k); if (m == null) throw new IOException("SU2_HUB_KEY"); m.read(in); }
            int nc = in.readInt(); if (nc != cal.size()) throw new IOException("SU2_HUB_CAL_SHAPE");
            for (int i = 0; i < nc; i++) { String k = in.readUTF(); CalibrationSU2.Live c = cal.get(k); if (c == null) throw new IOException("SU2_HUB_CAL_KEY"); c.read(in); }
            t = tt; predicted = false;
        }
    }

    public static final class Decision {
        public final long t; public final int position, mask; public final boolean act; public final double gscore, phat;
        public Decision(long t, int position, int mask, boolean act, double gscore, double phat) { this.t = t; this.position = position; this.mask = mask; this.act = act; this.gscore = gscore; this.phat = phat; }
    }

    final ModelGenomeSU2 g; Hub hub; final int startMeta, window, refit, M, F;
    final String[] keys; final double[] w; final double[] bmaLogw; final ModelOpsSU2.GatedState gated;
    /** R15 (P45): state-conditional teacher matrix pool (pool TEACHER:<j>). */
    final TeacherPoolSU2.Live teacher;
    long t;
    // rolling buffers (ring) for the applicability learner and the frequency gate
    final double[][] feat; final boolean[] lab; final double[] gsRing; final double[] c1Ring, c2Ring; double c1, c2;
    double ema12 = 0.3, ema48 = 0.3, ema200 = 0.3; double thr = Double.NaN; boolean thrValid;
    double[] wt, mu, sd; long last = Long.MIN_VALUE / 2;
    // pending decision for the current target
    long gateHistoryStartR26;
    int curP1 = -1; int[] curK3 = new int[3]; double[] curRow; double curGs;
    /** Pooled table of the current pending decision (read by MainlineAdapterSU2 to rank the 1200 K3 actions). */
    float[][] lastPooled; long lastPooledT = -1;

    public ModelLiveSU2(ModelGenomeSU2 g, Hub hub, int startMeta, int window, int refit) {
        this(g,hub,startMeta,window,refit,false);
    }
    /** Structural allocation only; caller must read a verified complete state or kernel initializer. */
    ModelLiveSU2(ModelGenomeSU2 g, Hub hub, int startMeta, int window, int refit,boolean verifiedRestore) {
        if(!verifiedRestore&&WarmModelSeedR26.has(g))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED");
        this.g = g; this.hub = hub; this.startMeta = g.gateWarmup(startMeta); this.window = window; this.refit = refit;
        M = g.members.size(); keys = new String[M]; w = new double[M];
        int stw = 0;
        double ws = 0;
        for (int m = 0; m < M; m++) { keys[m] = g.members.get(m).modelKey(); w[m] = g.members.get(m).weight; ws += w[m]; stw += hub.model(g.members.get(m)).stateWidth(); if (g.calibrated) hub.calibrate(g.members.get(m)); }
        if (g.calibrated) for (int m = 0; m < M; m++) w[m] /= ws;          // R14: normalised static weights
        bmaLogw = new double[M * 10];
        gated = M > 1 && g.pool.equals("GATED") ? new ModelOpsSU2.GatedState(M, stw) : null;
        teacher = M > 1 && TeacherPoolSU2.isTeacher(g.pool) ? new TeacherPoolSU2.Live(M, TeacherPoolSU2.stateOf(g.pool)) : null;
        F = 4 + 4 + stw + 3;
        feat = new double[window][F]; lab = new boolean[window]; gsRing = new double[720];
        c1Ring = new double[721]; c2Ring = new double[721];
        t = hub.t;
        if (t != 0) throw new IllegalStateException("SU2_LIVE_START_AT_GENESIS_OR_RESTORE");
    }

    /** Member table for this period: calibrated when the strategy is (R14). */
    float[][] tab(int m) { return g.calibrated ? hub.pc.get(keys[m]) : hub.p.get(keys[m]); }

    private float[][] pooled() {
        float[][] P = new float[10][10];
        if (M == 1) { float[][] q = tab(0); for (int p = 0; p < 10; p++) P[p] = q[p].clone(); }
        else if (gated != null) { P = gated.predict(memberTables(), memberStates()); }
        else if (teacher != null) { P = teacher.predict(memberTables()); }
        else if (g.pool.equals("BMA")) {
            for (int p = 0; p < 10; p++) {
                double mx = Double.NEGATIVE_INFINITY; for (int m = 0; m < M; m++) mx = Math.max(mx, bmaLogw[m * 10 + p]);
                double z = 0; double[] ww = new double[M]; for (int m = 0; m < M; m++) { ww[m] = Math.exp(bmaLogw[m * 10 + p] - mx); z += ww[m]; }
                for (int m = 0; m < M; m++) ww[m] /= z;
                for (int n = 0; n < 10; n++) { double v = 0; for (int m = 0; m < M; m++) v += ww[m] * (double) tab(m)[p][n]; P[p][n] = (float) v; }
            }
        } else {
            for (int p = 0; p < 10; p++) {
                double[] L = new double[10]; double mx = Double.NEGATIVE_INFINITY;
                for (int n = 0; n < 10; n++) { double v = 0; for (int m = 0; m < M; m++) v += w[m] * Math.log(Math.max((double) tab(m)[p][n], ModelOpsSU2.EPS)); L[n] = v; mx = Math.max(mx, v); }
                double s = 0; for (int n = 0; n < 10; n++) { L[n] = Math.exp(L[n] - mx); s += L[n]; }
                for (int n = 0; n < 10; n++) P[p][n] = (float) (L[n] / s);
            }
        }
        if (g.legalReadoutR26())P=LegalReadoutR26.project(P);else if (g.sink) P = ModelOpsSU2.sink(new float[][][]{P}, 20)[0];
        return P;
    }

    float[][][] memberTables() { float[][][] x = new float[M][][]; for (int m = 0; m < M; m++) x[m] = tab(m); return x; }
    float[][] memberStates() { float[][] x = new float[M][]; for (int m = 0; m < M; m++) x[m] = hub.st.get(keys[m]); return x; }

    /** Before draw t: compute the strategy, its gate score and whether it acts. Pure w.r.t. persisted state. */
    public Decision decide() {
        if (hub.t != t) throw new IllegalStateException("SU2_LIVE_HUB_DESYNC");
        hub.predict();
        float[][] P = pooled();
        lastPooled = P; lastPooledT = t;
        double[] mass = new double[10]; int[][] top = new int[10][3]; Integer[] ord = new Integer[10];
        for (int p = 0; p < 10; p++) {
            final float[] row = P[p]; for (int i = 0; i < 10; i++) ord[i] = i;
            Arrays.sort(ord, (a, b) -> Double.compare((double) row[b] - b * 1e-12, (double) row[a] - a * 1e-12));
            double s = 0; for (int i = 0; i < 3; i++) { top[p][i] = ord[i]; s += (double) row[ord[i]]; }
            mass[p] = Math.rint(s * 1e9) / 1e9;
        }
        int p1 = 0; for (int p = 1; p < 10; p++) if (mass[p] > mass[p1]) p1 = p;
        int p2 = p1 == 0 ? 1 : 0; for (int p = 0; p < 10; p++) if (p != p1 && mass[p] > mass[p2]) p2 = p;
        curP1 = p1; curK3 = top[p1].clone();
        double conf = mass[p1];
        if(M==1&&hub.models.get(keys[0]) instanceof SetReadoutR26){
            SetReadoutR26.Action selected=((SetReadoutR26)hub.models.get(keys[0])).choose(P);
            p1=selected.position;curP1=p1;curK3=new int[]{selected.a,selected.b,selected.c};conf=selected.probability;
        }
        double gs, phat;
        if (g.gate.equals("META")) {
            double[] row = new double[F]; int j = 0;
            double e = 0; for (int n = 0; n < 10; n++) { double v = P[p1][n]; e -= v * Math.log(v + 1e-12); }
            row[j++] = conf; row[j++] = mass[p1] - mass[p2]; row[j++] = e / Math.log(10); row[j++] = rollingZ(conf);
            for (int k : new int[]{5, 7}) {
                double s = 0, mn = 1e9;
                for (int m = 0; m < M; m++) {
                    final float[] r = tab(m)[p1]; for (int i = 0; i < 10; i++) ord[i] = i;
                    Arrays.sort(ord, (a, b) -> Float.compare(r[b], r[a]));
                    int in = 0; for (int q = 0; q < 3; q++) for (int i = 0; i < k; i++) if (ord[i] == curK3[q]) { in++; break; }
                    double sup = in / 3.0; s += sup; mn = Math.min(mn, sup);
                }
                row[j++] = s / M; row[j++] = mn;
            }
            for (int m = 0; m < M; m++) for (float v : hub.st.get(keys[m])) row[j++] = v;
            row[j++] = ema12; row[j++] = ema48; row[j++] = ema200;
            for (int q = 0; q < F; q++) if (!Double.isFinite(row[q])) row[q] = 0.0;
            curRow = row;
            if (t >= startMeta && (wt == null || t - last >= refit)) refitLogistic();
            if (t >= startMeta && wt != null) {
                double z = wt[0]; for (int q = 0; q < F; q++) z += (row[q] - mu[q]) / sd[q] * wt[q + 1];
                z = Math.max(-30, Math.min(30, z)); phat = 1.0 / (1.0 + Math.exp(-z)); gs = phat;
            } else { phat = Double.NaN; gs = -1.0; }
        } else { curRow = null; gs = conf; phat = conf; }
        curGs = gs;
        boolean act = false;
        boolean guard = g.calibrated && g.gate.equals("MASS");                 // R14: never act on a chance-level table
        if (t >= startMeta) {
            if (g.coverage >= .999) act = !guard || gs > ModelReplaySU2.INFORMATIVE;
            else {
                if ((t - startMeta) % 50 == 0) blockThreshold();
                act = thrValid && gs >= thr && gs > -1 && (!guard || gs > ModelReplaySU2.INFORMATIVE);
            }
        }
        int mask = (1 << curK3[0]) | (1 << curK3[1]) | (1 << curK3[2]);
        return new Decision(t, p1 + 1, mask, act, gs, g.legalReadoutR26()?conf:phat);
    }

    private double rollingZ(double v) {
        // window [t-720, t) through cumulative sums, exactly like the replay (cumsum differences)
        long lo = Math.max(t - 720, 0); int n = (int) Math.max(t - lo, 1);
        double clo = c1Ring[(int) (lo % 721)], c2lo = c2Ring[(int) (lo % 721)];
        double m = (c1 - clo) / n, var = Math.max((c2 - c2lo) / n - m * m, 1e-12);
        double s = (v - m) / Math.sqrt(var);
        return t < 30 ? 0 : Math.max(-6, Math.min(6, s));
    }

    private void refitLogistic() {
        long lo = Math.max(gateHistoryStartR26, Math.max(0, t - window)); int n = (int) (t - lo);
        if (n < 400) return;
        double[] m = new double[F], s = new double[F];
        for (int j = 0; j < F; j++) { double a = 0; for (long i = lo; i < t; i++) a += feat[(int) (i % window)][j]; m[j] = a / n; double v = 0; for (long i = lo; i < t; i++) { double q = feat[(int) (i % window)][j] - m[j]; v += q * q; } s[j] = Math.sqrt(v / n) + 1e-9; }
        double[][] Z = new double[n][F]; double[] y = new double[n];
        for (int i = 0; i < n; i++) { double[] r = feat[(int) ((lo + i) % window)]; for (int j = 0; j < F; j++) Z[i][j] = (r[j] - m[j]) / s[j]; y[i] = lab[(int) ((lo + i) % window)] ? 1 : 0; }
        wt = LogisticRidgeSU2.fit(Z, y, n, g.metaC, 12); mu = m; sd = s; last = t;
    }

    private void blockThreshold() {
        long lo = Math.max(gateHistoryStartR26, Math.max(0, Math.max(g.gate.equals("META") ? startMeta : startMeta - 720, t - 720)));
        double[] hist = new double[(int) (t - lo)]; int n = 0;
        for (long u = lo; u < t; u++) { double v = gsRing[(int) (u % 720)]; if (v > -1) hist[n++] = v; }
        if (n < 100) { thrValid = false; return; }
        thr = ModelReplaySU2.quantileLinear(Arrays.copyOf(hist, n), 1 - g.coverage); thrValid = true;
    }

    long gateContextStartR26(){return gateHistoryStartR26;}
    void restoreGateContextStartR26(long start){if(start<0||start>t)throw new IllegalArgumentException("R26_GATE_CONTEXT_CURSOR");gateHistoryStartR26=start;}
    void resetGateContextR26(){gateHistoryStartR26=t;curP1=-1;lastPooled=null;lastPooledT=-1;thrValid=false;wt=null;mu=null;sd=null;ema12=ema48=ema200=0;c1=c2=0;java.util.Arrays.fill(gsRing,-1);java.util.Arrays.fill(c1Ring,0);java.util.Arrays.fill(c2Ring,0);java.util.Arrays.fill(lab,false);for(double[] row:feat)java.util.Arrays.fill(row,0);}
    public boolean hasPending() { return curP1 >= 0; }
    public String key() { return g.key(); }
    public float[][] pendingTable(long tt) { return lastPooledT == tt ? lastPooled : null; }

    /** After draw t is settled: label, feedback, gate history; then the hub updates the shared models. */
    public boolean settle(int[] draw) {
        if (curP1 < 0 || hub.t != t) throw new IllegalStateException("SU2_LIVE_SETTLE_WITHOUT_DECISION");
        OnlineModelsSU2.requireDraw(draw);
        int x = draw[curP1] - 1;
        boolean hit = curK3[0] == x || curK3[1] == x || curK3[2] == x;
        if (g.gate.equals("META")) { feat[(int) (t % window)] = curRow; lab[(int) (t % window)] = hit; }
        else lab[(int) (t % window)] = hit;
        gsRing[(int) (t % 720)] = curGs;
        // cumulative sums for the confidence rolling z (conf = mass at the chosen position)
        double conf = g.gate.equals("META") ? curRow[0] : curGs;
        c1Ring[(int) (t % 721)] = c1; c2Ring[(int) (t % 721)] = c2;
        c1 += conf; c2 += conf * conf;
        c1Ring[(int) ((t + 1) % 721)] = c1; c2Ring[(int) ((t + 1) % 721)] = c2;
        double a12 = 1.0 - Math.pow(0.5, 1.0 / 12), a48 = 1.0 - Math.pow(0.5, 1.0 / 48), a200 = 1.0 - Math.pow(0.5, 1.0 / 200), h = hit ? 1.0 : 0.0;
        ema12 = (1 - a12) * ema12 + a12 * h; ema48 = (1 - a48) * ema48 + a48 * h; ema200 = (1 - a200) * ema200 + a200 * h;
        if (gated != null) gated.update(memberTables(), memberStates(), draw, 0.03, 1e-3);
        if (teacher != null) teacher.settle(memberTables(), draw);
        if (M > 1 && g.pool.equals("BMA")) {
            for (int p = 0; p < 10; p++) {
                int xx = draw[p] - 1;
                for (int m = 0; m < M; m++) bmaLogw[m * 10 + p] = 0.995 * bmaLogw[m * 10 + p] + Math.log(Math.max((double) tab(m)[p][xx], ModelOpsSU2.EPS));
                double mx = Double.NEGATIVE_INFINITY; for (int m = 0; m < M; m++) mx = Math.max(mx, bmaLogw[m * 10 + p]);
                double z = 0; double[] ww = new double[M]; for (int m = 0; m < M; m++) { ww[m] = Math.exp(bmaLogw[m * 10 + p] - mx); z += ww[m]; }
                for (int m = 0; m < M; m++) { ww[m] = (1 - 0.02) * (ww[m] / z) + 0.02 / M; bmaLogw[m * 10 + p] = Math.log(ww[m]); }
            }
        }
        curP1 = -1; t++;
        return hit;
    }

    /** A strategy born after genesis: replay the stored history through a private hub, then join the shared hub. */
    public static ModelLiveSU2 bootstrap(ModelGenomeSU2 g, Hub shared, int[][] history, int startMeta, int window, int refit) {
        if(WarmModelSeedR26.has(g))throw new IllegalArgumentException("R26_WARM_INITIALIZER_REQUIRED_FOR_REPLAY");
        for (ModelGenomeSU2.Member m : g.members) { shared.adopt(m, history); if (g.calibrated) shared.adoptCalibrated(m, history); }
        Hub own = new Hub();
        ModelLiveSU2 live = new ModelLiveSU2(g, own, startMeta, window, refit);
        for (int[] d : history) { live.decide(); live.settle(d); own.update(d); }
        if (own.t != shared.t) throw new IllegalStateException("SU2_BOOTSTRAP_DESYNC");
        live.hub = shared;
        return live;
    }

    public void write(DataOutput o) throws IOException {
        o.writeUTF(g.key()); o.writeLong(t);
        for (double v : bmaLogw) o.writeDouble(v);
        for (double[] r : feat) for (double v : r) o.writeDouble(v);
        for (boolean b : lab) o.writeBoolean(b);
        for (double v : gsRing) o.writeDouble(v);
        for (double v : c1Ring) o.writeDouble(v); for (double v : c2Ring) o.writeDouble(v); o.writeDouble(c1); o.writeDouble(c2);
        o.writeDouble(ema12); o.writeDouble(ema48); o.writeDouble(ema200); o.writeDouble(thr); o.writeBoolean(thrValid);
        o.writeBoolean(wt != null); if (wt != null) { for (double v : wt) o.writeDouble(v); for (double v : mu) o.writeDouble(v); for (double v : sd) o.writeDouble(v); }
        o.writeLong(last);
        if (gated != null) gated.write(o);
        if (teacher != null) teacher.write(o);
        if(!g.legacyIdentity())o.writeLong(gateHistoryStartR26);
    }

    public void readInto(DataInput in) throws IOException {
        if (!g.key().equals(in.readUTF())) throw new IOException("SU2_LIVE_STATE_GENOME");
        t = in.readLong();
        for (int i = 0; i < bmaLogw.length; i++) bmaLogw[i] = in.readDouble();
        for (double[] r : feat) for (int j = 0; j < F; j++) r[j] = in.readDouble();
        for (int i = 0; i < lab.length; i++) lab[i] = in.readBoolean();
        for (int i = 0; i < gsRing.length; i++) gsRing[i] = in.readDouble();
        for (int i = 0; i < c1Ring.length; i++) c1Ring[i] = in.readDouble(); for (int i = 0; i < c2Ring.length; i++) c2Ring[i] = in.readDouble(); c1 = in.readDouble(); c2 = in.readDouble();
        ema12 = in.readDouble(); ema48 = in.readDouble(); ema200 = in.readDouble(); thr = in.readDouble(); thrValid = in.readBoolean();
        if (in.readBoolean()) { wt = new double[F + 1]; mu = new double[F]; sd = new double[F]; for (int i = 0; i <= F; i++) wt[i] = in.readDouble(); for (int i = 0; i < F; i++) mu[i] = in.readDouble(); for (int i = 0; i < F; i++) sd[i] = in.readDouble(); } else wt = null;
        last = in.readLong(); curP1 = -1;
        if (gated != null) gated.read(in);
        if (teacher != null) teacher.read(in);
        gateHistoryStartR26=g.legacyIdentity()?0:in.readLong();if(gateHistoryStartR26<0||gateHistoryStartR26>t)throw new IOException("R26_GATE_CONTEXT_CURSOR");
    }
}
