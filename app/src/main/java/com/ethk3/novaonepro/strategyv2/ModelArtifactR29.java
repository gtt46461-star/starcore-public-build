package com.ethk3.novaonepro.strategyv2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

/** Full continuation state of the existing rotating-position AtomNet/FTRL experiment. This is not a ten-position
 * probability-surface model. Weights AND Adam, replay samples, dropout/replay RNG, learning rate, risk checkpoint,
 * FTRL z/n/w, and the last learned issue are published together with their checksum. No Java object deserialization.
 * RuntimeR27/AtomWorldR27 use this codec before learning and after a successful database commit. */
public final class ModelArtifactR29 {
    public static final String ABI = "ATOMREPLAY_R27_ROTATING_POSITION_HIT_V1";
    private static final int MAGIC = 0x29A70002, LEGACY_MAGIC = 0x29A70001, MAX_BYTES = 64 << 20, MAX_INPUT = 4096;
    public static final String DDL = "CREATE TABLE IF NOT EXISTS r29_model_checkpoint(owner TEXT PRIMARY KEY,feature_abi TEXT NOT NULL,dataset_hash TEXT NOT NULL,trained_through TEXT NOT NULL,state_hash TEXT NOT NULL,artifact BLOB NOT NULL,updated_at INTEGER NOT NULL)";
    public static final String LEGACY_DDL = "CREATE TABLE IF NOT EXISTS r29_legacy_model_checkpoint(owner TEXT NOT NULL,artifact_hash TEXT NOT NULL,artifact BLOB NOT NULL,trained_through TEXT NOT NULL,online_steps INTEGER NOT NULL,learning_rate REAL NOT NULL,last_check_logloss REAL,archived_at INTEGER NOT NULL,reason TEXT NOT NULL,PRIMARY KEY(owner,artifact_hash))";
    public static final String PRESERVE_LEGACY = "INSERT OR IGNORE INTO r29_legacy_model_checkpoint(owner,artifact_hash,artifact,trained_through,online_steps,learning_rate,last_check_logloss,archived_at,reason) SELECT owner,artifact_hash,artifact,trained_through,online_steps,learning_rate,last_check_logloss,updated_at,'LEGACY_PARTIAL_STATE_PRESERVED_BEFORE_R29_RETRAIN' FROM r27_atomnet_state WHERE owner='ATOMNET' AND NOT EXISTS(SELECT 1 FROM r29_model_checkpoint WHERE owner='ATOMNET_FTRL')";
    public static final String LEGACY_NO_UPDATE = "CREATE TRIGGER IF NOT EXISTS r29_legacy_model_no_update BEFORE UPDATE ON r29_legacy_model_checkpoint BEGIN SELECT RAISE(ABORT,'R29_LEGACY_MODEL_IMMUTABLE'); END";
    public static final String LEGACY_NO_DELETE = "CREATE TRIGGER IF NOT EXISTS r29_legacy_model_no_delete BEFORE DELETE ON r29_legacy_model_checkpoint BEGIN SELECT RAISE(ABORT,'R29_LEGACY_MODEL_IMMUTABLE'); END";
    public final AtomNetTrainerR27 trainer;
    public final FtrlR27 ftrl;
    public final String datasetHash, trainedThrough;

    public ModelArtifactR29(AtomNetTrainerR27 trainer, FtrlR27 ftrl, String datasetHash, String trainedThrough) {
        if (trainer == null || ftrl == null || datasetHash == null || trainedThrough == null) throw new IllegalArgumentException("R29_MODEL_STATE_REQUIRED");
        this.trainer = trainer; this.ftrl = ftrl; this.datasetHash = datasetHash; this.trainedThrough = trainedThrough;
    }

    public byte[] encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes);
            out.writeInt(MAGIC); out.writeUTF(ABI); out.writeUTF(datasetHash); out.writeUTF(trainedThrough);
            blob(out, trainer.net.encode()); blob(out, trainer.checkpoint == null ? new byte[0] : trainer.checkpoint);
            out.writeLong(trainer.rng.state); out.writeDouble(trainer.onlineLr); out.writeLong(trainer.onlineSteps); out.writeLong(trainer.sinceCheck);
            out.writeDouble(trainer.lastCheckLogloss); out.writeDouble(trainer.lastCheckSe);
            out.writeInt(trainer.nextEpochReceipt);
            out.writeInt(trainer.buffer.size());
            for (AtomNetTrainerR27.Sample s : trainer.buffer) {
                out.writeUTF(s.issue); out.writeInt(s.y); out.writeInt(s.cls); out.writeInt(s.x.length);
                for (double value : s.x) out.writeDouble(value);
            }
            out.writeInt(ftrl.d); out.writeDouble(ftrl.alpha); out.writeDouble(ftrl.beta); out.writeDouble(ftrl.l1); out.writeDouble(ftrl.l2); out.writeLong(ftrl.updates);
            for (double[] a : new double[][] {ftrl.z, ftrl.n, ftrl.w}) for (double value : a) out.writeDouble(value);
            out.close(); byte[] result = bytes.toByteArray();
            if (result.length > MAX_BYTES) throw new IOException("checkpoint exceeds budget");
            return result;
        } catch (IOException failure) { throw new IllegalStateException("R29_MODEL_ENCODE", failure); }
    }

    public static ModelArtifactR29 decode(byte[] bytes, String expectedHash) {
        if (bytes == null || bytes.length > MAX_BYTES || !StatsR27.sha256(bytes).equals(expectedHash)) throw new IllegalArgumentException("R29_MODEL_CHECKSUM");
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            int version = in.readInt();
            if ((version != MAGIC && version != LEGACY_MAGIC) || !ABI.equals(in.readUTF())) throw new IOException("feature ABI");
            String dataset = in.readUTF(), through = in.readUTF(); byte[] net = blob(in), checkpoint = blob(in);
            AtomNetR27 restored = network(net); AtomNetTrainerR27 tr = new AtomNetTrainerR27(restored.in, 0); tr.net = restored;
            if (checkpoint.length > 0 && network(checkpoint).in != restored.in) throw new IOException("rollback shape");
            tr.checkpoint = checkpoint.length == 0 ? null : checkpoint; tr.rng.state = in.readLong();
            tr.onlineLr = finite(in.readDouble()); tr.onlineSteps = in.readLong(); tr.sinceCheck = in.readLong();
            if (tr.onlineLr < 0 || tr.onlineSteps < 0 || tr.sinceCheck < 0) throw new IOException("training cursor");
            tr.lastCheckLogloss = in.readDouble(); tr.lastCheckSe = in.readDouble();
            if (Double.isInfinite(tr.lastCheckLogloss) || Double.isInfinite(tr.lastCheckSe) || tr.lastCheckSe < 0) throw new IOException("check state");
            tr.nextEpochReceipt = version == MAGIC ? bounded(in.readInt(), 0, Integer.MAX_VALUE) : 0;
            int n = bounded(in.readInt(), 0, AtomNetTrainerR27.REPLAY);
            for (int i = 0; i < n; i++) {
                String issue = in.readUTF(); int y = bounded(in.readInt(), 0, 1), cls = bounded(in.readInt(), 0, 9);
                int dim = bounded(in.readInt(), 1, MAX_INPUT); if (dim != restored.in) throw new IOException("sample shape");
                double[] x = new double[dim]; for (int j = 0; j < dim; j++) x[j] = finite(in.readDouble());
                tr.buffer.add(new AtomNetTrainerR27.Sample(x, y, cls, issue));
            }
            int dim = bounded(in.readInt(), 1, MAX_INPUT);
            double alpha = finite(in.readDouble()), beta = finite(in.readDouble()), l1 = finite(in.readDouble()), l2 = finite(in.readDouble());
            if (alpha <= 0 || beta < 0 || l1 < 0 || l2 < 0) throw new IOException("FTRL parameters");
            FtrlR27 f = new FtrlR27(dim, alpha, beta, l1, l2); f.updates = in.readLong(); if (f.updates < 0) throw new IOException("FTRL cursor");
            for (double[] a : new double[][] {f.z, f.n, f.w}) for (int i = 0; i < a.length; i++) a[i] = finite(in.readDouble());
            for (double value : f.n) if (value < 0) throw new IOException("negative accumulator");
            if (in.read() != -1) throw new IOException("trailing bytes");
            return new ModelArtifactR29(tr, f, dataset, through);
        } catch (IOException | RuntimeException failure) { throw new IllegalArgumentException("R29_MODEL_DECODE", failure); }
    }

    static AtomNetR27 network(byte[] bytes) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
        if (in.readInt() != 0x27A70001) throw new IOException("network ABI");
        bounded(in.readInt(), 1, MAX_INPUT);
        if (in.readLong() < 0) throw new IOException("optimizer step");
        while (in.available() >= 8) finite(in.readDouble());
        if (in.available() != 0) throw new IOException("network length");
        AtomNetR27 net = AtomNetR27.decode(bytes);
        // Check the complete canonical encoding, including optimizer slots, not just the weights.
        if (!java.util.Arrays.equals(net.encode(), bytes)) throw new IOException("network encoding");
        return net;
    }
    static double finite(double d) throws IOException { if (!Double.isFinite(d)) throw new IOException("nonfinite state"); return d; }
    static int bounded(int n, int lo, int hi) throws IOException { if (n < lo || n > hi) throw new IOException("state length"); return n; }
    static void blob(DataOutputStream out, byte[] data) throws IOException { out.writeInt(data.length); out.write(data); }
    static byte[] blob(DataInputStream in) throws IOException {
        int n = bounded(in.readInt(), 0, Math.min(MAX_BYTES, in.available())); byte[] data = new byte[n]; in.readFully(data); return data;
    }
}
