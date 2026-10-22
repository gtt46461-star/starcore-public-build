package com.ethk3.novaonepro;

import org.json.JSONObject;
import org.json.JSONArray;

/** Validated, request-owned model parameters. Static seeds are used only as one complete bundle. */
public final class ModelBundleParametersR932 {
    private ModelBundleParametersR932() {}
    public static final class Number {
        public final UpstreamModelMathR932.Number TRAINED_MODEL;
        public final double[] SLOW_INITIAL;
        public final String SLOW_CHECKPOINT_ISSUE;
        public final String MODEL_ID;
        public final String DATA_SHA256;
        public final boolean PROVEN;
        public final double TEMPERATURE;
        public final double[] FEATURE_MEAN;
        public final double[] FEATURE_SCALE;
        public final double[] FEATURE_WEIGHT;
        public final double[][] POSITION_NUMBER_BIAS;
        Number(JSONObject p) {
            try {
                TRAINED_MODEL=p!=null&&p.has("TRAINED_MODEL")?new UpstreamModelMathR932.Number(p.getJSONObject("TRAINED_MODEL")):null;
                SLOW_INITIAL=TRAINED_MODEL!=null&&TRAINED_MODEL.kind.equals("D2")?vector(p.getJSONArray("SLOW_INITIAL"),UpstreamModelMathR932.NUMBER_INPUT,"SLOW_INITIAL"):null;
                SLOW_CHECKPOINT_ISSUE=SLOW_INITIAL==null?"":p.getString("SLOW_CHECKPOINT_ISSUE");
                MODEL_ID=p==null?V51NumberModelArtifact.MODEL_ID:p.getString("MODEL_ID");
                DATA_SHA256=p==null?V51NumberModelArtifact.DATA_SHA256:p.getString("DATA_SHA256");
                PROVEN=p==null?V51NumberModelArtifact.PROVEN:p.getBoolean("PROVEN");
                TEMPERATURE=p==null?V51NumberModelArtifact.TEMPERATURE:number(p,"TEMPERATURE");
                FEATURE_MEAN=p==null?V51NumberModelArtifact.FEATURE_MEAN:vector(p.getJSONArray("FEATURE_MEAN"),V51NumberModelArtifact.FEATURE_MEAN.length,"FEATURE_MEAN");
                FEATURE_SCALE=p==null?V51NumberModelArtifact.FEATURE_SCALE:vector(p.getJSONArray("FEATURE_SCALE"),V51NumberModelArtifact.FEATURE_SCALE.length,"FEATURE_SCALE");
                FEATURE_WEIGHT=p==null?V51NumberModelArtifact.FEATURE_WEIGHT:vector(p.getJSONArray("FEATURE_WEIGHT"),V51NumberModelArtifact.FEATURE_WEIGHT.length,"FEATURE_WEIGHT");
                POSITION_NUMBER_BIAS=p==null?V51NumberModelArtifact.POSITION_NUMBER_BIAS:matrix(p.getJSONArray("POSITION_NUMBER_BIAS"),V51NumberModelArtifact.POSITION_NUMBER_BIAS,"POSITION_NUMBER_BIAS");
            } catch(Exception e) {throw new IllegalArgumentException("R932_NUMBER_PARAMETERS",e);}
        }
    }
    public static final class Gate {
        public final UpstreamModelMathR932.Gate TRAINED_MODEL;
        public final String ARTIFACT_ID;
        public final String DATA_SHA256;
        public final boolean PROVEN;
        public final double[][] PROTOTYPE_WEIGHT;
        Gate(JSONObject p) {
            try {
                TRAINED_MODEL=p!=null&&p.has("TRAINED_MODEL")?new UpstreamModelMathR932.Gate(p.getJSONObject("TRAINED_MODEL")):null;
                ARTIFACT_ID=p==null?V51ExpertGateArtifact.ARTIFACT_ID:p.getString("ARTIFACT_ID");
                DATA_SHA256=p==null?V51ExpertGateArtifact.DATA_SHA256:p.getString("DATA_SHA256");
                PROVEN=p==null?V51ExpertGateArtifact.PROVEN:p.getBoolean("PROVEN");
                PROTOTYPE_WEIGHT=p==null?V51ExpertGateArtifact.PROTOTYPE_WEIGHT:matrix(p.getJSONArray("PROTOTYPE_WEIGHT"),V51ExpertGateArtifact.PROTOTYPE_WEIGHT,"PROTOTYPE_WEIGHT");
            } catch(Exception e) {throw new IllegalArgumentException("R932_GATE_PARAMETERS",e);}
        }
    }
    public static final class Ranker {
        public final TrainedModelMathR932.Ranker TRAINED_MODEL;
        public final String ARTIFACT_ID;
        public final String DATA_SHA256;
        public final boolean PROVEN;
        public final boolean RANK_ORDER_PASSED;
        public final int STABLE_FOLDS;
        public final double[] MEAN;
        public final double[] SCALE;
        public final double[] WEIGHT;
        public final double BIAS;
        Ranker(JSONObject p) {
            try {
                TRAINED_MODEL=p!=null&&p.has("TRAINED_MODEL")?new TrainedModelMathR932.Ranker(p.getJSONObject("TRAINED_MODEL")):null;
                ARTIFACT_ID=p==null?V51ActionRankerArtifact.ARTIFACT_ID:p.getString("ARTIFACT_ID");
                DATA_SHA256=p==null?V51ActionRankerArtifact.DATA_SHA256:p.getString("DATA_SHA256");
                PROVEN=p==null?V51ActionRankerArtifact.PROVEN:p.getBoolean("PROVEN");
                RANK_ORDER_PASSED=p==null?V51ActionRankerArtifact.RANK_ORDER_PASSED:p.getBoolean("RANK_ORDER_PASSED");
                STABLE_FOLDS=p==null?V51ActionRankerArtifact.STABLE_FOLDS:integer(p,"STABLE_FOLDS");
                MEAN=p==null?V51ActionRankerArtifact.MEAN:vector(p.getJSONArray("MEAN"),V51ActionRankerArtifact.MEAN.length,"MEAN");
                SCALE=p==null?V51ActionRankerArtifact.SCALE:vector(p.getJSONArray("SCALE"),V51ActionRankerArtifact.SCALE.length,"SCALE");
                WEIGHT=p==null?V51ActionRankerArtifact.WEIGHT:vector(p.getJSONArray("WEIGHT"),V51ActionRankerArtifact.WEIGHT.length,"WEIGHT");
                BIAS=p==null?V51ActionRankerArtifact.BIAS:number(p,"BIAS");
            } catch(Exception e) {throw new IllegalArgumentException("R932_RANKER_PARAMETERS",e);}
        }
    }
    public static final class Calibration {
        public final TrainedModelMathR932.Calibration TRAINED_MODEL;
        public final String ARTIFACT_ID;
        public final String DATA_SHA256;
        public final String INPUT_SCHEMA_SHA256;
        public final String SELECTION_POLICY_SHA256;
        public final boolean PROVEN;
        public final double OOS_ECE;
        public final double OOS_BRIER;
        public final int CALIBRATION_SUPPORT_N;
        public final int RELIABILITY_BIN_MIN_N;
        public final int CALIBRATION_HITS;
        public final double[] MEAN;
        public final double[] SCALE;
        public final double[] WEIGHT;
        public final double BIAS;
        Calibration(JSONObject p) {
            try {
                TRAINED_MODEL=p!=null&&p.has("TRAINED_MODEL")?new TrainedModelMathR932.Calibration(p.getJSONObject("TRAINED_MODEL")):null;
                ARTIFACT_ID=p==null?V51SelectedCalibrationArtifact.ARTIFACT_ID:p.getString("ARTIFACT_ID");
                DATA_SHA256=p==null?V51SelectedCalibrationArtifact.DATA_SHA256:p.getString("DATA_SHA256");
                INPUT_SCHEMA_SHA256=p==null?V51SelectedCalibrationArtifact.INPUT_SCHEMA_SHA256:p.getString("INPUT_SCHEMA_SHA256");
                SELECTION_POLICY_SHA256=p==null?V51SelectedCalibrationArtifact.SELECTION_POLICY_SHA256:p.getString("SELECTION_POLICY_SHA256");
                PROVEN=p==null?V51SelectedCalibrationArtifact.PROVEN:p.getBoolean("PROVEN");
                OOS_ECE=p==null?V51SelectedCalibrationArtifact.OOS_ECE:number(p,"OOS_ECE");
                OOS_BRIER=p==null?V51SelectedCalibrationArtifact.OOS_BRIER:number(p,"OOS_BRIER");
                CALIBRATION_SUPPORT_N=p==null?V51SelectedCalibrationArtifact.CALIBRATION_SUPPORT_N:integer(p,"CALIBRATION_SUPPORT_N");
                RELIABILITY_BIN_MIN_N=p==null?V51SelectedCalibrationArtifact.RELIABILITY_BIN_MIN_N:integer(p,"RELIABILITY_BIN_MIN_N");
                CALIBRATION_HITS=p==null?V51SelectedCalibrationArtifact.CALIBRATION_HITS:integer(p,"CALIBRATION_HITS");
                MEAN=p==null?V51SelectedCalibrationArtifact.MEAN:vector(p.getJSONArray("MEAN"),V51SelectedCalibrationArtifact.MEAN.length,"MEAN");
                SCALE=p==null?V51SelectedCalibrationArtifact.SCALE:vector(p.getJSONArray("SCALE"),V51SelectedCalibrationArtifact.SCALE.length,"SCALE");
                WEIGHT=p==null?V51SelectedCalibrationArtifact.WEIGHT:vector(p.getJSONArray("WEIGHT"),V51SelectedCalibrationArtifact.WEIGHT.length,"WEIGHT");
                BIAS=p==null?V51SelectedCalibrationArtifact.BIAS:number(p,"BIAS");
            } catch(Exception e) {throw new IllegalArgumentException("R932_CALIBRATION_PARAMETERS",e);}
        }
    }
    public static final class Predictability {
        public final TrainedModelMathR932.Predictability TRAINED_MODEL;
        public final String SELECTION_POLICY_SHA256;
        public final String ARTIFACT_ID;
        public final String DATA_SHA256;
        public final boolean PROVEN;
        public final double[] WEIGHT;
        public final double BIAS;
        public final double QUALIFIED_THRESHOLD;
        Predictability(JSONObject p) {
            try {
                TRAINED_MODEL=p!=null&&p.has("TRAINED_MODEL")?new TrainedModelMathR932.Predictability(p.getJSONObject("TRAINED_MODEL")):null;
                SELECTION_POLICY_SHA256=p==null?"":p.optString("SELECTION_POLICY_SHA256","");
                if(TRAINED_MODEL!=null&&!SELECTION_POLICY_SHA256.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R932_PREDICTABILITY_SELECTOR");
                ARTIFACT_ID=p==null?V51PredictabilityArtifact.ARTIFACT_ID:p.getString("ARTIFACT_ID");
                DATA_SHA256=p==null?V51PredictabilityArtifact.DATA_SHA256:p.getString("DATA_SHA256");
                PROVEN=p==null?V51PredictabilityArtifact.PROVEN:p.getBoolean("PROVEN");
                WEIGHT=p==null?V51PredictabilityArtifact.WEIGHT:vector(p.getJSONArray("WEIGHT"),V51PredictabilityArtifact.WEIGHT.length,"WEIGHT");
                BIAS=p==null?V51PredictabilityArtifact.BIAS:number(p,"BIAS");
                QUALIFIED_THRESHOLD=p==null?V51PredictabilityArtifact.QUALIFIED_THRESHOLD:number(p,"QUALIFIED_THRESHOLD");
            } catch(Exception e) {throw new IllegalArgumentException("R932_PREDICTABILITY_PARAMETERS",e);}
        }
    }
    public static final class Policy {
        public final String ARTIFACT_ID;
        public final String DATA_SHA256;
        public final boolean MODEL_GATE_PASSED;
        public final double OOS_COVERAGE;
        public final double OOS_HIT_RATE;
        public final double OOS_ECE;
        public final V51PolicyCell[] CELLS;
        Policy(JSONObject p) {
            try {
                ARTIFACT_ID=p==null?V51PolicySurfaceArtifact.ARTIFACT_ID:p.getString("ARTIFACT_ID");
                DATA_SHA256=p==null?V51PolicySurfaceArtifact.DATA_SHA256:p.getString("DATA_SHA256");
                MODEL_GATE_PASSED=p==null?V51PolicySurfaceArtifact.MODEL_GATE_PASSED:p.getBoolean("MODEL_GATE_PASSED");
                OOS_COVERAGE=p==null?V51PolicySurfaceArtifact.OOS_COVERAGE:number(p,"OOS_COVERAGE");
                OOS_HIT_RATE=p==null?V51PolicySurfaceArtifact.OOS_HIT_RATE:number(p,"OOS_HIT_RATE");
                OOS_ECE=p==null?V51PolicySurfaceArtifact.OOS_ECE:number(p,"OOS_ECE");
                CELLS=p==null?V51PolicySurfaceArtifact.CELLS:cells(p.getJSONArray("CELLS"));
            } catch(Exception e) {throw new IllegalArgumentException("R932_POLICY_PARAMETERS",e);}
        }
    }
    private static int integer(JSONObject p,String k) throws Exception {
        double n=number(p,k);if(n<0||n>Integer.MAX_VALUE||n!=Math.rint(n))throw new IllegalArgumentException("R932_INTEGER:"+k);return (int)n;
    }
    private static double number(JSONObject p,String k) throws Exception {
        double n=p.getDouble(k);if(!Double.isFinite(n))throw new IllegalArgumentException("R932_NONFINITE:"+k);return n;
    }
    private static double[] vector(JSONArray a,int size,String name) throws Exception {
        if(a.length()!=size)throw new IllegalArgumentException("R932_DIMENSION:"+name);
        double[] out=new double[size];for(int i=0;i<size;i++){
            out[i]=a.getDouble(i);if(!Double.isFinite(out[i])||(name.contains("SCALE")&&out[i]<=0))
                throw new IllegalArgumentException("R932_VECTOR_VALUE:"+name);}
        return out;
    }
    private static double[][] matrix(JSONArray a,double[][] shape,String name) throws Exception {
        if(a.length()!=shape.length)throw new IllegalArgumentException("R932_MATRIX_ROWS:"+name);
        double[][] out=new double[shape.length][];for(int i=0;i<out.length;i++)out[i]=vector(a.getJSONArray(i),shape[i].length,name);return out;
    }
    private static V51PolicyCell[] cells(JSONArray a) throws Exception {
        if(a.length()>512)throw new IllegalArgumentException("R932_TOO_MANY_POLICY_CELLS");
        V51PolicyCell[] out=new V51PolicyCell[a.length()];java.util.HashSet<String> ids=new java.util.HashSet<String>();
        for(int i=0;i<out.length;i++) {JSONObject x=a.getJSONObject(i);String id=x.getString("id");
            if(id.length()==0||!ids.add(id))throw new IllegalArgumentException("R932_POLICY_CELL_ID");
            int position=integer(x,"position");if(position>10)throw new IllegalArgumentException("R932_POLICY_POSITION");
            out[i]=new V51PolicyCell(id,x.getString("domain_prefix"),V51Enums.Lifecycle.valueOf(x.getString("lifecycle")),position,
                number(x,"selected_p_min"),number(x,"predictability_min"),number(x,"rank_margin_min"),number(x,"third_margin_min"),
                number(x,"consensus_min"),number(x,"uncertainty_max"),number(x,"falsifier_max"),integer(x,"sample"),
                number(x,"hit_rate"),number(x,"credible_low"),number(x,"ece"),number(x,"block_minimum"),number(x,"max_drift"),x.getBoolean("active"));
        }return out;
    }
}
