package com.ethk3.novaonepro;
import org.json.*;

/** Direct state4/expert9 inference. No META96 inversion, downstream inputs, or target labels. */
public final class UpstreamModelMathR932 {
    public static final int NUMBER_INPUT=1049,GATE_INPUT=68;
    public static final String CONTRACT="R932_DIRECT_STATE4_EXPERT9_V1";
    private UpstreamModelMathR932() {}
    public static double[] features(double[][] states,double[][][] experts) {
        validate(states,experts);double[] out=new double[NUMBER_INPUT];int i=0;
        for(double[] p:states)for(double x:p)out[i++]=x;
        for(double[][] e:experts)for(double[] p:e)for(double x:p)out[i++]=x;
        for(int p=0;p<10;p++)for(int n=0;n<10;n++)out[i++]=stdAt(experts,p,n);
        for(int e=0;e<9;e++){double sum=0;for(double[] p:experts[e])for(double x:p)sum+=x;out[i++]=sum/100;}
        return out;
    }
    public static double[] gateFeatures(double[][] states,double[][][] experts) {
        validate(states,experts);double[] out=new double[GATE_INPUT];int i=0;
        for(double[] p:states)for(double x:p)out[i++]=x;
        double[] means=new double[9];for(int e=0;e<9;e++){double sum=0;for(double[] p:experts[e])for(double x:p)sum+=x;means[e]=sum/100;out[i++]=means[e];}
        for(int e=0;e<9;e++){double sum=0;for(double[] p:experts[e])for(double x:p){double d=x-means[e];sum+=d*d;}out[i++]=StrictMath.sqrt(sum/100);}
        for(int p=0;p<10;p++){double sum=0;for(int n=0;n<10;n++)sum+=stdAt(experts,p,n);out[i++]=sum/10;}
        return out;
    }
    private static double stdAt(double[][][] experts,int p,int n) {
        double mean=0;for(int e=0;e<9;e++)mean+=experts[e][p][n];mean/=9;
        double variance=0;for(int e=0;e<9;e++){double d=experts[e][p][n]-mean;variance+=d*d;}return StrictMath.sqrt(variance/9);
    }
    public static double[][] stateValues(V51Models.StateTrajectorySnapshot[] states) {
        if(states==null||states.length!=10)throw new IllegalArgumentException("R932_UPSTREAM_STATE_COUNT");double[][] out=new double[10][4];
        for(int p=0;p<10;p++){V51Models.StateTrajectorySnapshot s=states[p];if(s==null)throw new IllegalArgumentException("R932_UPSTREAM_STATE_MISSING");out[p]=new double[]{s.confidence,s.changePointRisk,s.remainingLife,s.posterior};}return out;
    }
    public static double[][][] expertValues(V51Models.ExpertEvidence[] experts) {
        if(experts==null||experts.length!=9)throw new IllegalArgumentException("R932_EXPERT_COUNT");double[][][] out=new double[9][][];
        for(int e=0;e<9;e++){if(experts[e]==null||!V51ExpertGateArtifact.EXPERT_IDS[e].equals(experts[e].expertId))throw new IllegalArgumentException("R932_EXPERT_ORDER");out[e]=experts[e].rawScore;}return out;
    }
    private static void validate(double[][] states,double[][][] experts) {
        if(states.length!=10||experts.length!=9)throw new IllegalArgumentException("R932_UPSTREAM_SHAPE");
        for(double[] row:states){if(row.length!=4)throw new IllegalArgumentException("R932_STATE_WIDTH");for(double x:row)TrainedModelMathR932.finite(x);}
        for(double[][] e:experts){if(e.length!=10)throw new IllegalArgumentException("R932_EXPERT_HEIGHT");for(double[] row:e){if(row.length!=10)throw new IllegalArgumentException("R932_EXPERT_WIDTH");for(double x:row)TrainedModelMathR932.finite(x);}}
    }
    public static final class Number {
        public final String kind;
        private final double[] mean,scale;
        private final double[][][] weights;
        private final double[][] biases;
        public Number(JSONObject model)throws Exception {
            kind=model.getString("kind");
            if(kind.equals("BASE_NUMBER_Q_9_EXPERT_MEAN")||kind.equals("GATE9")){mean=scale=null;weights=null;biases=null;return;}
            if(!kind.matches("D[0-3]"))throw new IllegalArgumentException("R932_NUMBER_FAMILY");
            int input=kind.equals("D2")?2*NUMBER_INPUT:NUMBER_INPUT;
            mean=TrainedModelMathR932.vector(model.getJSONArray("mean"),input,false);scale=TrainedModelMathR932.vector(model.getJSONArray("scale"),input,true);
            JSONArray w=model.getJSONArray("weights"),b=model.getJSONArray("biases");if(w.length()!=3||b.length()!=3)throw new IllegalArgumentException("R932_NUMBER_LAYERS");
            int[] widths={64,48,100};weights=new double[3][][];biases=new double[3][];
            for(int layer=0;layer<3;layer++){weights[layer]=TrainedModelMathR932.matrix(w.getJSONArray(layer),widths[layer],input);biases[layer]=TrainedModelMathR932.vector(b.getJSONArray(layer),widths[layer],false);input=widths[layer];}
        }
        public double[][] logits(double[] features,double[] pastSlow,double[][][] experts) {return logits(features,pastSlow,experts,null);}
        public double[][] logits(double[] features,double[] pastSlow,double[][][] experts,double[] gate) {
            if(kind.equals("BASE_NUMBER_Q_9_EXPERT_MEAN")||kind.equals("GATE9")) {
                if(kind.equals("GATE9")&&(gate==null||gate.length!=9))throw new IllegalArgumentException("R932_GATE9_NUMBER_WEIGHTS");
                double[][] out=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++){
                    double sum=0;for(int e=0;e<9;e++)sum+=TrainedModelMathR932.finite(experts[e][p][n])*(gate==null?1:gate[e]);out[p][n]=gate==null?sum/9:sum;
                }return out;
            }
            if(features.length!=NUMBER_INPUT||(kind.equals("D2")&&(pastSlow==null||pastSlow.length!=NUMBER_INPUT)))throw new IllegalArgumentException("R932_NUMBER_INPUT");
            double[] a=new double[mean.length];for(int i=0;i<a.length;i++){double value=i<NUMBER_INPUT?features[i]:pastSlow[i-NUMBER_INPUT];a[i]=Math.max(-8,Math.min(8,(TrainedModelMathR932.finite(value)-mean[i])/scale[i]));}
            for(int k=0;k<3;k++){
                double[] next=new double[biases[k].length];for(int o=0;o<next.length;o++){double sum=biases[k][o];for(int i=0;i<a.length;i++)sum+=a[i]*weights[k][o][i];next[o]=TrainedModelMathR932.finite(k<2?Math.max(0,sum):sum);}a=next;
            }
            double[][] out=new double[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++){
                double value=a[p*10+n];if(kind.equals("D1")){double base=0;for(int e=0;e<9;e++)base+=experts[e][p][n];value+=base/9;}out[p][n]=TrainedModelMathR932.finite(value);
            }return out;
        }
    }
    public static final class Gate {
        private final double[] mean,scale,bias;
        private final double[][] weight;
        public Gate(JSONObject model)throws Exception {
            mean=TrainedModelMathR932.vector(model.getJSONArray("mean"),GATE_INPUT,false);scale=TrainedModelMathR932.vector(model.getJSONArray("scale"),GATE_INPUT,true);
            weight=TrainedModelMathR932.matrix(model.getJSONArray("weight"),9,GATE_INPUT);bias=TrainedModelMathR932.vector(model.getJSONArray("bias"),9,false);
        }
        public double[] weights(double[][] states,double[][][] experts) {
            double[] x=gateFeatures(states,experts),out=new double[9];double maximum=-Double.MAX_VALUE,sum=0;
            for(int e=0;e<9;e++){double z=bias[e];for(int i=0;i<x.length;i++)z+=Math.max(-8,Math.min(8,(x[i]-mean[i])/scale[i]))*weight[e][i];out[e]=TrainedModelMathR932.finite(z);maximum=Math.max(maximum,z);}
            for(int e=0;e<9;e++){out[e]=StrictMath.exp(out[e]-maximum);sum+=out[e];}for(int e=0;e<9;e++)out[e]/=sum;return out;
        }
    }
}
