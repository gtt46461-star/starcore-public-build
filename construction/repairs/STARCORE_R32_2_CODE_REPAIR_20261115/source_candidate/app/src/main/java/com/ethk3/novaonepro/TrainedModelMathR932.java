package com.ethk3.novaonepro;

import org.json.*;

/** Immutable inference adapters for the exact R929 training exports. No authorization lives here. */
public final class TrainedModelMathR932 {
    private TrainedModelMathR932() {}
    public static final class Predictability {
        private final double[] mean,scale,weight;
        private final double bias;
        public Predictability(JSONObject model)throws Exception {
            if(!"PREDRAW_TRUST_LOGISTIC_12".equals(model.getString("family")))throw new IllegalArgumentException("R932_PREDICTABILITY_FAMILY");
            mean=vector(model.getJSONArray("mean"),12,false);scale=vector(model.getJSONArray("scale"),12,true);
            weight=vector(model.getJSONArray("weight"),12,false);bias=finite(model.getDouble("bias"));
        }
        public double predict(double[] x) {
            if(x==null||x.length!=12)throw new IllegalArgumentException("R932_PREDICTABILITY_INPUT");
            double z=bias;for(int i=0;i<12;i++)z+=(finite(x[i])-mean[i])/scale[i]*weight[i];
            return sigmoid(finite(z));
        }
    }
    public static final class Ranker {
        public final String family;
        private final double[] mean, scale, weight, b1, w2;
        private final double[][] w1;
        private final double bias;
        public Ranker(JSONObject model) throws Exception {
            family=model.getString("family");
            if("RAW_ACTION_P".equals(family)) {mean=scale=weight=b1=w2=null;w1=null;bias=0;return;}
            mean=vector(model.getJSONArray("mean"),96,false);scale=vector(model.getJSONArray("scale"),96,true);
            if("SMALL_MLP_1200".equals(family)) {
                int hidden=model.getJSONArray("b1").length();if(hidden<1||hidden>64)throw new IllegalArgumentException("R932_RANKER_HIDDEN");
                b1=vector(model.getJSONArray("b1"),hidden,false);w2=vector(model.getJSONArray("w2"),hidden,false);
                w1=matrix(model.getJSONArray("w1"),96,hidden);bias=finite(model.getDouble("b2"));weight=null;
            } else {
                if(!family.equals("PAIRWISE_LINEAR_1200")&&!family.equals("LISTWISE_LINEAR_1200")&&!family.equals("TOP_WEIGHTED_LINEAR_1200"))
                    throw new IllegalArgumentException("R932_RANKER_FAMILY");
                weight=vector(model.getJSONArray("weight"),96,false);bias=finite(model.getDouble("bias"));b1=w2=null;w1=null;
            }
        }
        public double score(double[] x,double rawActionP) {
            if(x==null||x.length!=96)throw new IllegalArgumentException("R932_RANKER_INPUT");
            for(double v:x)finite(v);
            if(family.equals("RAW_ACTION_P"))return finite(rawActionP);
            double result=bias;
            if(w1==null)for(int i=0;i<96;i++)result+=((x[i]-mean[i])/Math.max(1e-9,scale[i]))*weight[i];
            else for(int j=0;j<b1.length;j++) {
                double z=b1[j];for(int i=0;i<96;i++)z+=((x[i]-mean[i])/scale[i])*w1[i][j];
                result+=StrictMath.tanh(z)*w2[j];
            }
            return finite(result);
        }
    }
    public static final class Calibration {
        public final String family;
        private final double[] weights, centers, probabilities;
        public Calibration(JSONObject model) throws Exception {
            family=model.getString("family");
            if(family.equals("LOGISTIC")) {weights=new double[]{finite(model.getDouble("bias")),finite(model.getDouble("weight"))};centers=probabilities=null;}
            else if(family.equals("BETA")){weights=vector(model.getJSONArray("weight"),3,false);centers=probabilities=null;}
            else if(family.equals("ISOTONIC")) {
                JSONArray blocks=model.getJSONArray("blocks");if(blocks.length()<1||blocks.length()>256)throw new IllegalArgumentException("R932_CAL_BLOCKS");
                centers=new double[blocks.length()];probabilities=new double[blocks.length()];weights=null;
                for(int i=0;i<blocks.length();i++) {
                    double[] b=vector(blocks.getJSONArray(i),3,false);
                    if(b[1]<0||b[1]>1||b[2]<1||b[2]!=Math.rint(b[2])||(i>0&&(b[0]<=centers[i-1]||b[1]<probabilities[i-1])))
                        throw new IllegalArgumentException("R932_CAL_BLOCK_CONTRACT");
                    centers[i]=b[0];probabilities[i]=b[1];
                }
            } else throw new IllegalArgumentException("R932_CAL_FAMILY");
        }
        public double predict(double rawScore) {
            finite(rawScore);
            if(family.equals("LOGISTIC"))return sigmoid(weights[0]+weights[1]*rawScore);
            if(family.equals("BETA")) {
                double p=Math.max(1e-6,Math.min(1-1e-6,sigmoid(rawScore)));
                return sigmoid(weights[0]+weights[1]*StrictMath.log(p)+weights[2]*StrictMath.log1p(-p));
            }
            int nearest=0;for(int i=1;i<centers.length;i++)if(Math.abs(rawScore-centers[i])<Math.abs(rawScore-centers[nearest]))nearest=i;
            return probabilities[nearest];
        }
    }
    static double sigmoid(double z) {return 1/(1+StrictMath.exp(-Math.max(-35,Math.min(35,z))));}
    static double finite(double v) {if(!Double.isFinite(v))throw new IllegalArgumentException("R932_MODEL_NONFINITE");return v;}
    static double[] vector(JSONArray a,int n,boolean positive) throws Exception {
        if(a.length()!=n)throw new IllegalArgumentException("R932_MODEL_DIMENSION");
        double[] out=new double[n];for(int i=0;i<n;i++){out[i]=finite(a.getDouble(i));if(positive&&out[i]<=0)throw new IllegalArgumentException("R932_MODEL_SCALE");}return out;
    }
    static double[][] matrix(JSONArray a,int rows,int columns)throws Exception {
        if(a.length()!=rows)throw new IllegalArgumentException("R932_MODEL_MATRIX_DIMENSION");
        double[][] out=new double[rows][];for(int i=0;i<rows;i++)out[i]=vector(a.getJSONArray(i),columns,false);return out;
    }
}
