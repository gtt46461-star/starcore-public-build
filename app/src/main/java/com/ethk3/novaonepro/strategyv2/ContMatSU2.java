package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/**
 * P49 atomic continuation & direction matrix (contmat.py in the Python reference), streaming and phone-feasible.
 *
 * Patterns, all formed from draws already settled (causal):
 *   ATOM i TOP / BOTTOM      score atom i (AtomStreamSU2, z-scored per position, clipped) >= +1.645 / <= -1.645 at (p, n)
 *   BIG/SMALL/ODD/EVEN_RUN_k position p showed a big / small / odd / even number in each of the last k draws
 *                            (k = 3, 5, 7, 10) -> active for those numbers at p ("does the streak continue?")
 *   UNDER_W / OVER_W         number n at position p under / over represented over the last W draws (z <= -2 / >= 2),
 *                            W = 100, 200 ("does it catch up / cool down?")
 * For every (pattern, scope = all positions | one position) the matrix keeps, per 500-period block, how many cells were
 * active and how many came out. At every refit a pattern is VERIFIED only when both halves of the settled blocks deviate
 * from 10% in the same direction with |z| >= 2.5 (all positions) / 3.5 (one position), >= 200 cells each. The table for
 * the next block is softmax_n(sum of log(rate / 0.1) over the verified active patterns); nothing verified -> uniform, and
 * the calibrated table never passes the INFORMATIVE guard. verified() is the human-readable continuation / reversal report.
 * State: (verified patterns / 100, largest added logit, share of positions with an active verified pattern).
 */
public final class ContMatSU2 implements TemporalModelR26 {
    static final int START = 1500, BLOCK = 500, MIN_N = 200, MAX_BLOCKS = 40;   // verification uses the last 40 settled blocks
    static final double Z_ALL = 2.5, Z_POS = 3.5;
    static final float THR = 1.645f;
    static final int[] RUNS = {3, 5, 7, 10};
    static final int[] WINDOWS = {100, 200};
    static final String[] RUN_KINDS = {"BIG", "SMALL", "ODD", "EVEN"};

    final AtomStreamSU2 atoms = new AtomStreamSU2();
    final int nAtoms = AtomStreamSU2.size(), nPat;
    final String[] names;
    final int[][] runLen = new int[4][10];
    final int[][] ring = new int[WINDOWS[WINDOWS.length - 1]][10];
    final int[][][] win = new int[WINDOWS.length][10][10];
    final List<double[]> blkN = new ArrayList<>(), blkK = new ArrayList<>();   // per block: [pattern * 11 + scope]
    int blockBase;                                                               // index of blkN.get(0)
    final List<int[]> verPat = new ArrayList<>();                                // {pattern, scope}
    final List<Double> verLog = new ArrayList<>(), verRate = new ArrayList<>();
    final List<Long> verCells = new ArrayList<>();
    long t,contextStart;
    // Only executions completed by this numeric owner are counted. An imported ABI 1/2
    // checkpoint has an unknown lifetime count; later observed executions do not invent it.
    private boolean refitLifetimeKnownR335 = true;
    private long refitsObservedR335, lastRefitCursorR335 = -1;
    private String lastRefitOriginalR335 = "";
    static final int REFIT_ORIGINAL_MAX_BYTES_R335 = 4 * 1024 * 1024;
    public Long successfulRefitsR335() { return refitLifetimeKnownR335 ? refitsObservedR335 : null; }
    public long observedRefitsSinceEvidenceR335() { return refitsObservedR335; }
    public long lastRefitCursorR335() { return lastRefitCursorR335; }
    public String refitOriginalR335() { return lastRefitOriginalR335; }
    public String refitOriginalSha256R335() { return shaR335(lastRefitOriginalR335); }
    private static boolean finiteR335(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}
    // Math.log may differ by one ulp on each conforming runtime. Validate that
    // bounded agreement without rewriting the persisted value or numeric path.
    // log(1) is specified as positive zero, so its special case stays exact.
    private static boolean sameLogR335(double stored,double computed){
        if(!finiteR335(stored)||!finiteR335(computed))return false;
        if(stored==0.0||computed==0.0)return Double.doubleToLongBits(stored)==0L&&Double.doubleToLongBits(computed)==0L;
        return Math.abs(stored-computed)<=Math.ulp(stored)+Math.ulp(computed);
    }
    private static String shaR335(String value) {
        try { byte[] bytes=java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(64);for(byte b:bytes)out.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return out.toString();
        } catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    boolean[][][] active;                                                        // [pattern][p][n] for the current target
    final double[] zraw = new double[100];

    public ContMatSU2() {
        List<String> n = new ArrayList<>();
        for (int i = 0; i < nAtoms; i++) { n.add("ATOM:" + AtomStreamSU2.CATALOG[i].name() + ":TOP"); n.add("ATOM:" + AtomStreamSU2.CATALOG[i].name() + ":BOTTOM"); }
        for (String k : RUN_KINDS) for (int r : RUNS) n.add(k + "_RUN_" + r);
        for (int w : WINDOWS) { n.add("UNDER_" + w); n.add("OVER_" + w); }
        names = n.toArray(new String[0]); nPat = names.length;
    }
    public int stateWidth() { return 3; }

    static boolean sel(int kind, int n) { return kind == 0 ? n >= 5 : kind == 1 ? n < 5 : kind == 2 ? n % 2 == 0 : n % 2 == 1; }

    private void masks() {
        active = new boolean[nPat][10][10];
        for (int i = 0; i < nAtoms; i++) {
            float[][] a = atoms.atom(i);
            for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) { active[2 * i][p][n] = a[p][n] >= THR; active[2 * i + 1][p][n] = a[p][n] <= -THR; }
        }
        int j = 2 * nAtoms;
        for (int k = 0; k < 4; k++) for (int r : RUNS) { for (int p = 0; p < 10; p++) if (runLen[k][p] >= r) for (int n = 0; n < 10; n++) active[j][p][n] = sel(k, n); j++; }
        for (int wi = 0; wi < WINDOWS.length; wi++) {
            int W = WINDOWS[wi];
            if (t-contextStart >= W) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) {
                double z = (win[wi][p][n] - W / 10.0) / Math.sqrt(W * 0.09);
                active[j][p][n] = z <= -2; active[j + 1][p][n] = z >= 2;
            }
            j += 2;
        }
    }

    public void predict(float[][] out, float[] st) {
        if (active == null) masks();
        Arrays.fill(zraw, 0.0);
        for (int v = 0; v < verPat.size(); v++) {
            int j = verPat.get(v)[0], sc = verPat.get(v)[1]; double lg = verLog.get(v);
            for (int p = 0; p < 10; p++) { if (sc > 0 && p != sc - 1) continue; for (int n = 0; n < 10; n++) if (active[j][p][n]) zraw[p * 10 + n] += lg; }
        }
        double mx = Double.NEGATIVE_INFINITY; int posAct = 0;
        for (int p = 0; p < 10; p++) { boolean any = false; for (int n = 0; n < 10; n++) { mx = Math.max(mx, zraw[p * 10 + n]); any |= zraw[p * 10 + n] != 0; } if (any) posAct++; }
        double[] e = new double[10];
        for (int p = 0; p < 10; p++) {
            double m = Double.NEGATIVE_INFINITY; for (int n = 0; n < 10; n++) m = Math.max(m, zraw[p * 10 + n]);
            for (int n = 0; n < 10; n++) e[n] = Math.exp(zraw[p * 10 + n] - m);
            double s = ModelOpsSU2.pairwise(e, 0, 10);
            for (int n = 0; n < 10; n++) out[p][n] = (float) (e[n] / s);
        }
        st[0] = (float) (verPat.size() / 100.0); st[1] = (float) mx; st[2] = (float) (posAct / 10.0);
    }

    public void update(int[] d) {
        OnlineModelsSU2.requireDraw(d);
        if (active == null) masks();
        int bi = (int) (t / BLOCK);
        while (blockBase + blkN.size() <= bi) { blkN.add(new double[nPat * 11]); blkK.add(new double[nPat * 11]); }
        double[] bn = blkN.get(bi - blockBase), bk = blkK.get(bi - blockBase);
        for (int j = 0; j < nPat; j++) for (int p = 0; p < 10; p++) for (int n = 0; n < 10; n++) if (active[j][p][n]) {
            boolean h = d[p] - 1 == n;
            bn[j * 11] += 1; bn[j * 11 + p + 1] += 1;
            if (h) { bk[j * 11] += 1; bk[j * 11 + p + 1] += 1; }
        }
        // direction state
        for (int p = 0; p < 10; p++) {
            int x = d[p] - 1;
            boolean[] ind = {x >= 5, x < 5, x % 2 == 0, x % 2 == 1};
            for (int k = 0; k < 4; k++) runLen[k][p] = ind[k] ? runLen[k][p] + 1 : 0;
        }
        int R = ring.length;
        for (int wi = 0; wi < WINDOWS.length; wi++) {
            int W = WINDOWS[wi];
            if (t-contextStart >= W) { int[] old = ring[(int) ((t - W) % R)]; for (int p = 0; p < 10; p++) win[wi][p][old[p] - 1]--; }
            for (int p = 0; p < 10; p++) win[wi][p][d[p] - 1]++;
        }
        System.arraycopy(d, 0, ring[(int) (t % R)], 0, 10);
        atoms.update(d);
        t++;
        while (blockBase < (int) (t / BLOCK) - MAX_BLOCKS) { blkN.remove(0); blkK.remove(0); blockBase++; }   // keep the window + the open block
        if (t >= START + BLOCK && t % BLOCK == 0) refit();
        active = null;
    }

    private void refit() {
        int end = (int) (t / BLOCK), first = Math.max(START / BLOCK, end - MAX_BLOCKS), nbk = end - first, h1 = first + nbk / 2;
        List<int[]> nextPat=new ArrayList<>();List<Double> nextLog=new ArrayList<>(),nextRate=new ArrayList<>();List<Long> nextCells=new ArrayList<>();
        StringBuilder original=new StringBuilder("CONTMAT_REFIT_R335\t1\t").append(t).append('\t').append(first).append('\t').append(h1).append('\t').append(end).append('\t').append(nPat).append('\n');
        for (int j = 0; j < nPat; j++) for (int sc = 0; sc < 11; sc++) {
            double n1 = 0, k1 = 0, n2 = 0, k2 = 0;
            for (int b = first; b < h1; b++) { n1 += blkN.get(b - blockBase)[j * 11 + sc]; k1 += blkK.get(b - blockBase)[j * 11 + sc]; }
            for (int b = h1; b < end; b++) { n2 += blkN.get(b - blockBase)[j * 11 + sc]; k2 += blkK.get(b - blockBase)[j * 11 + sc]; }
            if(!finiteR335(n1)||!finiteR335(k1)||!finiteR335(n2)||!finiteR335(k2)||n1<0||n2<0||k1<0||k2<0||k1>n1||k2>n2)throw new IllegalStateException("R335_REFIT_COUNTS_INVALID");
            double z1 = n1 > 0 ? (k1 / n1 - 0.1) / Math.sqrt(0.09 / n1) : Double.NaN;
            double z2 = n2 > 0 ? (k2 / n2 - 0.1) / Math.sqrt(0.09 / n2) : Double.NaN, zt = sc == 0 ? Z_ALL : Z_POS;
            String verdict=n1<MIN_N||n2<MIN_N?"MIN_HALF_SUPPORT":z1*z2<=0?"DIRECTION_NOT_EQUAL":Math.abs(z1)<zt||Math.abs(z2)<zt?"BELOW_Z":"VERIFIED";
            original.append("R\t").append(j).append('\t').append(sc).append('\t').append(names[j]).append('\t').append(n1).append('\t').append(k1).append('\t').append(n2).append('\t').append(k2).append('\t')
                    .append(Double.isNaN(z1)?"-":String.valueOf(z1)).append('\t').append(Double.isNaN(z2)?"-":String.valueOf(z2)).append('\t').append(zt).append('\t').append(verdict).append('\n');
            if ("VERIFIED".equals(verdict)) {
                double r = (k1 + k2) / (n1 + n2);
                nextPat.add(new int[]{j, sc}); nextLog.add(Math.log(Math.max(r, 1e-3) / 0.1)); nextRate.add(r); nextCells.add((long) (n1 + n2));
            }
        }
        String completed=original.toString();
        if(completed.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>REFIT_ORIGINAL_MAX_BYTES_R335)throw new IllegalStateException("R335_REFIT_ORIGINAL_BOUND");
        if(t<=lastRefitCursorR335||refitsObservedR335==Long.MAX_VALUE)throw new IllegalStateException("R335_REFIT_CURSOR_OR_COUNT");
        verPat.clear();verPat.addAll(nextPat);verLog.clear();verLog.addAll(nextLog);verRate.clear();verRate.addAll(nextRate);verCells.clear();verCells.addAll(nextCells);
        lastRefitOriginalR335=completed;lastRefitCursorR335=t;refitsObservedR335++;
    }

    /** The current continuation / reversal report: pattern, scope (0 = all positions, p = position p), rate, cells. */
    public List<String> verified() {
        List<String> out = new ArrayList<>();
        for (int v = 0; v < verPat.size(); v++)
            out.add(String.format(Locale.ROOT, "%s|%s|%.4f|%d", names[verPat.get(v)[0]], verPat.get(v)[1] == 0 ? "all" : "pos" + verPat.get(v)[1], verRate.get(v), verCells.get(v)));
        return out;
    }

    public void resetTemporalContext(){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();new AtomStreamSU2().write(new DataOutputStream(bytes));atoms.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));}catch(IOException impossible){throw new IllegalStateException("R26_CONTMAT_CONTEXT_RESET",impossible);}
        contextStart=t;for(int[] row:runLen)Arrays.fill(row,0);for(int[] row:ring)Arrays.fill(row,0);for(int[][] window:win)for(int[] row:window)Arrays.fill(row,0);active=null;Arrays.fill(zraw,0);
    }
    public void write(DataOutput o) throws IOException {
        o.writeLong(Long.MIN_VALUE);o.writeInt(3);o.writeLong(t);o.writeLong(contextStart);atoms.write(o);
        for (int[] r : runLen) for (int v : r) o.writeInt(v);
        for (int[] r : ring) for (int v : r) o.writeByte(v);
        for (int[][] w : win) for (int[] r : w) for (int v : r) o.writeInt(v);
        o.writeInt(blockBase); o.writeInt(blkN.size());
        for (int b = 0; b < blkN.size(); b++) { for (double v : blkN.get(b)) o.writeDouble(v); for (double v : blkK.get(b)) o.writeDouble(v); }
        o.writeInt(verPat.size());
        for (int v = 0; v < verPat.size(); v++) { o.writeInt(verPat.get(v)[0]); o.writeInt(verPat.get(v)[1]); o.writeDouble(verLog.get(v)); o.writeDouble(verRate.get(v)); o.writeLong(verCells.get(v)); }
        o.writeBoolean(refitLifetimeKnownR335);o.writeLong(refitsObservedR335);o.writeLong(lastRefitCursorR335);
        byte[] original=lastRefitOriginalR335.getBytes(java.nio.charset.StandardCharsets.UTF_8);o.writeInt(original.length);o.write(original);
    }
    public void read(DataInput in) throws IOException {
        long header=in.readLong();int version=1;if(header==Long.MIN_VALUE){version=in.readInt();if(version!=2&&version!=3)throw new IOException("R26_CONTMAT_STATE_ABI");t=in.readLong();contextStart=in.readLong();}else{t=header;contextStart=0;}if(t<0||contextStart<0||contextStart>t)throw new IOException("R26_CONTMAT_STATE_CURSOR");atoms.read(in);
        for (int[] r : runLen) for (int i = 0; i < r.length; i++) r[i] = in.readInt();
        for (int[] r : ring) for (int i = 0; i < r.length; i++) r[i] = in.readByte();
        for (int[][] w : win) for (int[] r : w) for (int i = 0; i < r.length; i++) r[i] = in.readInt();
        validateTemporalStateR335();
        blockBase = in.readInt(); int nb = in.readInt();
        // R33.4: fixed 033 could write 42 blocks between refit boundaries. Accept only
        // cursor-consistent legacy coverage, then discard the one obsolete block below.
        long blockEnd = t / BLOCK + (t % BLOCK == 0 ? 0 : 1);
        long retainedFirst = Math.max(0L, t / BLOCK - MAX_BLOCKS);
        if (nb < 0 || nb > MAX_BLOCKS + 2) throw new IOException("R26_CONTMAT_BLOCK_BOUND");
        if (blockEnd > Integer.MAX_VALUE || blockBase < 0 || blockBase > retainedFirst
                || (long) blockBase + nb != blockEnd) throw new IOException("R33_CONTMAT_BLOCK_CURSOR");
        blkN.clear(); blkK.clear();
        for (int b = 0; b < nb; b++) { double[] a = new double[nPat * 11], c = new double[nPat * 11]; for (int i = 0; i < a.length; i++) a[i] = in.readDouble(); for (int i = 0; i < c.length; i++) {c[i] = in.readDouble();if(!finiteR335(a[i])||!finiteR335(c[i])||a[i]<0||c[i]<0||c[i]>a[i]||a[i]!=Math.rint(a[i])||c[i]!=Math.rint(c[i]))throw new IOException("R335_CONTMAT_BLOCK_COUNTS");} validateBlockCountsR335(a,c,(long)blockBase+b);blkN.add(a); blkK.add(c); }
        while (blockBase < retainedFirst) { blkN.remove(0); blkK.remove(0); blockBase++; }
        int nv = in.readInt();if(nv<0||nv>nPat*11)throw new IOException("R26_CONTMAT_VERIFICATION_BOUND"); verPat.clear(); verLog.clear(); verRate.clear(); verCells.clear();
        int previousPattern=-1;
        for (int v = 0; v < nv; v++) { int j=in.readInt(),sc=in.readInt();double log=in.readDouble(),rate=in.readDouble();long cells=in.readLong();
            long closed=Math.min(MAX_BLOCKS,t/BLOCK-START/BLOCK),maximumCells=Math.max(0L,closed)*BLOCK*(sc==0?100L:10L);
            if(j<0||j>=nPat||sc<0||sc>10||j*11+sc<=previousPattern||closed<2||!finiteR335(log)||!finiteR335(rate)||rate<0||rate>1||cells<2L*MIN_N||cells>maximumCells||!sameLogR335(log,Math.log(Math.max(rate,1e-3)/.1)))throw new IOException("R335_CONTMAT_VERIFIED_VALUE");
            previousPattern=j*11+sc;verPat.add(new int[]{j,sc});verLog.add(log);verRate.add(rate);verCells.add(cells); }
        refitLifetimeKnownR335=false;refitsObservedR335=0;lastRefitCursorR335=-1;lastRefitOriginalR335="";
        if(version>=3){
            refitLifetimeKnownR335=in.readBoolean();refitsObservedR335=in.readLong();lastRefitCursorR335=in.readLong();int length=in.readInt();
            if(length<0||length>REFIT_ORIGINAL_MAX_BYTES_R335||refitsObservedR335<0||lastRefitCursorR335>t||lastRefitCursorR335 < -1)throw new IOException("R335_REFIT_EVIDENCE_BOUND");
            byte[] raw=new byte[length];in.readFully(raw);lastRefitOriginalR335=new String(raw,java.nio.charset.StandardCharsets.UTF_8);
            if(!java.util.Arrays.equals(raw,lastRefitOriginalR335.getBytes(java.nio.charset.StandardCharsets.UTF_8)))throw new IOException("R335_REFIT_EVIDENCE_UTF8");
            if(refitsObservedR335==0){if(lastRefitCursorR335!=-1||length!=0||refitLifetimeKnownR335&&nv>0)throw new IOException("R335_REFIT_ZERO_WITH_RECEIPT");}
            else validateRefitOriginalR335();
        }
        active = null;
    }
    private void validateBlockCountsR335(double[] n,double[] k,long block) throws IOException {
        long steps=Math.max(0L,Math.min(BLOCK,t-block*BLOCK));
        for(int pattern=0;pattern<nPat;pattern++){
            double allN=0,allK=0;for(int p=1;p<=10;p++){int index=pattern*11+p;if(n[index]>10L*steps||k[index]>steps)throw new IOException("R335_CONTMAT_BLOCK_SUPPORT_BOUND");allN+=n[index];allK+=k[index];}
            if(n[pattern*11]!=allN||k[pattern*11]!=allK)throw new IOException("R335_CONTMAT_BLOCK_SCOPE_SUM");
        }
    }
    private void validateTemporalStateR335() throws IOException {
        long available=t-contextStart;if(atoms.time()!=available)throw new IOException("R335_CONTMAT_ATOM_CLOCK");int present=(int)Math.min(available,ring.length);boolean[] used=new boolean[ring.length];
        for(int back=1;back<=present;back++)used[(int)((t-back)%ring.length)]=true;
        for(int row=0;row<ring.length;row++){
            if(used[row]){try{OnlineModelsSU2.requireDraw(ring[row]);}catch(IllegalArgumentException bad){throw new IOException("R335_CONTMAT_RING_DRAW",bad);}}
            else for(int value:ring[row])if(value!=0)throw new IOException("R335_CONTMAT_UNUSED_RING");
        }
        for(int wi=0;wi<WINDOWS.length;wi++){
            int[][] expected=new int[10][10];int count=(int)Math.min(available,WINDOWS[wi]);
            for(int back=1;back<=count;back++){int[] draw=ring[(int)((t-back)%ring.length)];for(int p=0;p<10;p++)expected[p][draw[p]-1]++;}
            for(int p=0;p<10;p++)for(int n=0;n<10;n++)if(win[wi][p][n]!=expected[p][n])throw new IOException("R335_CONTMAT_WINDOW_COUNTS");
        }
        for(int kind=0;kind<4;kind++)for(int p=0;p<10;p++){
            int suffix=0;for(int back=1;back<=present;back++){if(!sel(kind,ring[(int)((t-back)%ring.length)][p]-1))break;suffix++;}
            int observed=runLen[kind][p];if(observed<0||observed>available||observed<suffix||(suffix<present||available<=ring.length)&&observed!=suffix)throw new IOException("R335_CONTMAT_RUN_LENGTH");
        }
    }
    private void validateRefitOriginalR335() throws IOException {
        try {
            if(lastRefitCursorR335<START+BLOCK||lastRefitCursorR335%BLOCK!=0||refitsObservedR335>lastRefitCursorR335/BLOCK)throw new IllegalArgumentException("cursor");
            String[] lines=lastRefitOriginalR335.split("\n");String[] header=lines[0].split("\t",-1);
            if(header.length!=7||!"CONTMAT_REFIT_R335".equals(header[0])||!"1".equals(header[1])||Long.parseLong(header[2])!=lastRefitCursorR335||Integer.parseInt(header[6])!=nPat||lines.length!=1+nPat*11)throw new IllegalArgumentException("header");
            int end=(int)(lastRefitCursorR335/BLOCK),first=Math.max(START/BLOCK,end-MAX_BLOCKS),half=first+(end-first)/2;
            if(Integer.parseInt(header[3])!=first||Integer.parseInt(header[4])!=half||Integer.parseInt(header[5])!=end)throw new IllegalArgumentException("halves");
            int verified=0;
            for(int row=0;row<nPat*11;row++){
                String[] f=lines[row+1].split("\t",-1);int j=row/11,sc=row%11;
                if(f.length!=12||!"R".equals(f[0])||Integer.parseInt(f[1])!=j||Integer.parseInt(f[2])!=sc||!names[j].equals(f[3]))throw new IllegalArgumentException("row");
                double n1=Double.parseDouble(f[4]),k1=Double.parseDouble(f[5]),n2=Double.parseDouble(f[6]),k2=Double.parseDouble(f[7]);
                if(!finiteR335(n1)||!finiteR335(k1)||!finiteR335(n2)||!finiteR335(k2)||n1<0||n2<0||k1<0||k2<0||k1>n1||k2>n2)throw new IllegalArgumentException("counts");
                double z1=n1>0?(k1/n1-.1)/Math.sqrt(.09/n1):Double.NaN,z2=n2>0?(k2/n2-.1)/Math.sqrt(.09/n2):Double.NaN,zt=sc==0?Z_ALL:Z_POS;
                if(!(Double.isNaN(z1)?"-":String.valueOf(z1)).equals(f[8])||!(Double.isNaN(z2)?"-":String.valueOf(z2)).equals(f[9])||Double.parseDouble(f[10])!=zt)throw new IllegalArgumentException("z");
                String result=n1<MIN_N||n2<MIN_N?"MIN_HALF_SUPPORT":z1*z2<=0?"DIRECTION_NOT_EQUAL":Math.abs(z1)<zt||Math.abs(z2)<zt?"BELOW_Z":"VERIFIED";
                if(!result.equals(f[11]))throw new IllegalArgumentException("verdict");
                if("VERIFIED".equals(result)){
                    if(verified>=verPat.size()||verPat.get(verified)[0]!=j||verPat.get(verified)[1]!=sc||Double.doubleToLongBits(verRate.get(verified))!=Double.doubleToLongBits((k1+k2)/(n1+n2))||!sameLogR335(verLog.get(verified),Math.log(Math.max((k1+k2)/(n1+n2),1e-3)/.1))||verCells.get(verified)!=(long)(n1+n2))throw new IllegalArgumentException("verified");verified++;
                }
            }
            if(verified!=verPat.size())throw new IllegalArgumentException("verified_count");
        } catch(RuntimeException invalid){throw new IOException("R335_REFIT_EVIDENCE_INVALID",invalid);}
    }
}
