package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Portable fixed RELSTATE10 graph plus exact plain-SGD continuation state. No evidence is transportable. */
public final class TrainingCheckpointSU1 {
    public static final String FORMAT = "SU1_RELSTATE10_CHECKPOINT_V2";
    public static final String ADAM_FORMAT = "SU1_RELSTATE10_ADAM_CHECKPOINT_V3";
    public final RelationalStateInference.Weights weights;
    public final TrainingObjectiveSU1 objective;
    public final OptimizerStateSU1 optimizer;
    public final int window;
    public final long steps;
    public final double learningRate, clipNorm;
    public final String datasetHash, throughIssue, parentHash, weightsWire, weightsHash, wire, hash;

    public TrainingCheckpointSU1(RelationalStateInference.Weights weights, int window, long steps,
            double learningRate, double clipNorm, TrainingObjectiveSU1 objective,
            String datasetHash, String throughIssue, String parentHash) {
        this(weights,window,steps,learningRate,clipNorm,objective,datasetHash,throughIssue,parentHash,OptimizerStateSU1.sgd(steps));
    }
    public TrainingCheckpointSU1(RelationalStateInference.Weights weights, int window, long steps,
            double learningRate, double clipNorm, TrainingObjectiveSU1 objective,
            String datasetHash, String throughIssue, String parentHash,OptimizerStateSU1 optimizer) {
        if (weights == null || weights.d > 16 || window < 1 || window > 256 || steps < 0 || steps > 100000000
                || !Double.isFinite(learningRate) || learningRate <= 0 || learningRate > 1
                || !Double.isFinite(clipNorm) || clipNorm <= 0 || clipNorm > 100 || objective == null
                || throughIssue == null || !throughIssue.matches("20[0-9]{10}"))
            throw new IllegalArgumentException("SU1_TRAIN_CHECKPOINT_LIMIT");
        this.weights = weights; this.window = window; this.steps = steps; this.learningRate = learningRate;
        this.clipNorm = clipNorm; this.objective = objective; this.datasetHash = HashingSU1.sha(datasetHash);
        if(optimizer==null||optimizer.steps!=steps||optimizer.first!=null&&optimizer.first.d!=weights.d)throw new IllegalArgumentException("SU1_CHECKPOINT_OPTIMIZER_BINDING");this.optimizer=optimizer;
        this.throughIssue = throughIssue; this.parentHash = HashingSU1.sha(parentHash);
        weightsWire = RelationalModelCodecSU1.encode(weights); weightsHash = HashingSU1.text(weightsWire);
        Map<String,Object> m = new TreeMap<>();
        m.put("format", optimizer.kind==OptimizerStateSU1.Kind.SGD_NO_MOMENTUM_V1?FORMAT:ADAM_FORMAT); m.put("graph", "EMBED_ADD_POSITION>SOFTMAX_RELATION>MESSAGE_SUM>TANH_RECURRENT>LINEAR>SOFTMAX");
        m.put("dtype", "FLOAT64"); m.put("normalizer", "CATEGORICAL_ONE_HOT_NO_FIT");
        m.put("state", "WINDOW_RESET_ZERO"); m.put("axes", "ISSUE,POSITION,NUMBER");
        m.put("shapes", shapes(weights.d)); m.put("weights", weightsWire); m.put("weightsHash", weightsHash);
        m.put("window", window); m.put("steps", steps); m.put("optimizer", optimizer.kind.name());
        if(optimizer.first!=null){String first=RelationalModelCodecSU1.encode(optimizer.first),second=RelationalModelCodecSU1.encode(optimizer.second);m.put("firstMoment",first);m.put("firstMomentHash",HashingSU1.text(first));m.put("secondMoment",second);m.put("secondMomentHash",HashingSU1.text(second));}
        m.put("learningRateHex", Double.toHexString(learningRate)); m.put("clipNormHex", Double.toHexString(clipNorm));
        m.put("objective", Arrays.asList(Double.toHexString(objective.brier),Double.toHexString(objective.smoothness),Double.toHexString(objective.l2),Double.toHexString(objective.resource)));
        m.put("objectiveHash", objective.hash); m.put("datasetHash", this.datasetHash);
        m.put("trainedThroughIssue", throughIssue); m.put("parentCheckpointHash", this.parentHash);
        wire = StrictCanonicalJson.writeContext(m); hash = HashingSU1.text(wire);
        if (wire.length() > 262144) throw new IllegalArgumentException("SU1_MODEL_MANIFEST_BYTES");
    }
    private static String shapes(int d) {
        return "E:10x"+d+";U:10x"+d+";R:10x10;Wx:"+d+"x"+d+";Wm:"+d+"x"+d+";Wh:"+d+"x"+d+";b:"+d+";Wo:"+d+"x10;c:10";
    }
    public static TrainingCheckpointSU1 decode(String wire) {
        if (wire == null || wire.length() > 262144) throw new IllegalArgumentException("SU1_MODEL_MANIFEST_BYTES");
        Map<String,Object> m = StrictCanonicalJson.readContextObject(wire);
        if ((!FORMAT.equals(m.get("format"))&&!ADAM_FORMAT.equals(m.get("format"))) || !(m.get("objective") instanceof List))
            throw new IllegalArgumentException("SU1_MODEL_MANIFEST_FORMAT");
        List<?> coefficients = (List<?>)m.get("objective");
        if (coefficients.size() != 4) throw new IllegalArgumentException("SU1_MODEL_OBJECTIVE");
        TrainingObjectiveSU1 objective = new TrainingObjectiveSU1(Double.valueOf((String)coefficients.get(0)), Double.valueOf((String)coefficients.get(1)), Double.valueOf((String)coefficients.get(2)), Double.valueOf((String)coefficients.get(3)));
        RelationalStateInference.Weights weights = RelationalModelCodecSU1.decode((String)m.get("weights"),(String)m.get("weightsHash"));
        OptimizerStateSU1 optimizer=FORMAT.equals(m.get("format"))?OptimizerStateSU1.sgd(exactLong(m.get("steps"))):OptimizerStateSU1.adam(exactLong(m.get("steps")),RelationalModelCodecSU1.decode((String)m.get("firstMoment"),(String)m.get("firstMomentHash")),RelationalModelCodecSU1.decode((String)m.get("secondMoment"),(String)m.get("secondMomentHash")));
        TrainingCheckpointSU1 out = new TrainingCheckpointSU1(weights,exactInt(m.get("window")),exactLong(m.get("steps")),
                Double.valueOf((String)m.get("learningRateHex")),Double.valueOf((String)m.get("clipNormHex")),objective,
                (String)m.get("datasetHash"),(String)m.get("trainedThroughIssue"),(String)m.get("parentCheckpointHash"),optimizer);
        // Rebuilding the closed manifest validates all operator, shape, normalization and optimizer fields.
        if (!out.wire.equals(wire)) throw new IllegalArgumentException("SU1_MODEL_MANIFEST_NONCANONICAL_OR_UNKNOWN_FIELDS");
        return out;
    }
    private static long exactLong(Object v) { if (!(v instanceof Number) || !v.toString().matches("[0-9]+")) throw new IllegalArgumentException("SU1_MODEL_INTEGER"); return Long.parseLong(v.toString()); }
    private static int exactInt(Object v) { return Math.toIntExact(exactLong(v)); }

    public TrainingCheckpointSU1 continueOne(int[][] history, int[] label, String newDataHash, String labelIssue, BudgetTokenSU1 budget) {
        if (history.length != window || labelIssue == null || labelIssue.compareTo(throughIssue) <= 0)
            throw new IllegalArgumentException("SU1_CONTINUATION_DATA_BOUNDARY");
        RelationalTrainerSU1.Gradient gradient = RelationalTrainerSU1.gradient(weights,history,label,budget,objective);
        OptimizerStateSU1.Update next=optimizer.apply(weights,gradient,learningRate,clipNorm);
        return new TrainingCheckpointSU1(next.weights,window,steps+1,
                learningRate,clipNorm,objective,newDataHash,labelIssue,hash,next.state);
    }
}
