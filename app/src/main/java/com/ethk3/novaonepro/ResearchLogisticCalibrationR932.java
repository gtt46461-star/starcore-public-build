package com.ethk3.novaonepro;
import org.json.*;

/** Shared numerical fit for fixed 120/1/120 research calibration. No authority or storage. */
public final class ResearchLogisticCalibrationR932 {
    private ResearchLogisticCalibrationR932(){}
    static JSONObject fit(double[] scores,double[] hits)throws JSONException {
        if(scores==null||hits==null||scores.length<120||hits.length!=scores.length)throw new IllegalArgumentException("R932_CAL_FIT_DIMENSION");
        for(int i=0;i<120;i++)if(!Double.isFinite(scores[i])||(hits[i]!=0&&hits[i]!=1))throw new IllegalArgumentException("R932_CAL_FIT_NONFINITE_OR_LABEL");
        double mean=0,variance=0,success=0;for(int i=0;i<120;i++){mean+=scores[i]/120;success+=hits[i];}for(int i=0;i<120;i++)variance+=(scores[i]-mean)*(scores[i]-mean)/120;
        double scale=Math.max(1e-6,Math.sqrt(variance)),weight=0,bias=Math.log((success+.5)/(120-success+.5));
        for(int epoch=0;epoch<160;epoch++) {
            ResearchWorkBudgetR927.checkpoint();double dw=0,dbias=0;
            for(int i=0;i<120;i++){double x=Math.max(-8,Math.min(8,(scores[i]-mean)/scale));double residual=1/(1+Math.exp(-Math.max(-35,Math.min(35,bias+weight*x))))-hits[i];dw+=residual*x/120;dbias+=residual/120;}
            weight-=.05*(dw+.1*weight);bias-=.05*dbias;
        }
            JSONObject model=new JSONObject();model.put("revision","R932_SCORE_CAL_LOGISTIC_V1");model.put("mean",mean);model.put("scale",scale);model.put("weight",weight);model.put("bias",bias);
        if(!Double.isFinite(mean)||!Double.isFinite(scale)||!Double.isFinite(weight)||!Double.isFinite(bias))throw new IllegalArgumentException("R932_CAL_FIT_OVERFLOW");
        return model;
    }
    static double predict(JSONObject model,double score)throws JSONException {
        if(model==null||!Double.isFinite(score))throw new IllegalArgumentException("R932_CAL_SCORE_NONFINITE");
        double mean=model.getDouble("mean"),scale=model.getDouble("scale"),bias=model.getDouble("bias"),weight=model.getDouble("weight");
        if(!Double.isFinite(mean)||!Double.isFinite(scale)||scale<=0||!Double.isFinite(bias)||!Double.isFinite(weight))throw new IllegalArgumentException("R932_CAL_MODEL_PARAMETERS");
        double x=Math.max(-8,Math.min(8,(score-mean)/scale));
        return 1/(1+Math.exp(-Math.max(-35,Math.min(35,bias+weight*x))));
    }
    static void validate(JSONObject model,double[] scores,double[] hits)throws JSONException {
        if(scores==null||hits==null||scores.length!=241||hits.length!=241)throw new IllegalArgumentException("R932_CAL_HOLDOUT_DIMENSION");
        double brier=0,ece=0,baseline=0;int[] bins=new int[10];double[] ps=new double[10],ys=new double[10];
        for(int i=121;i<241;i++){if(hits[i]!=0&&hits[i]!=1)throw new IllegalArgumentException("R932_CAL_HOLDOUT_LABEL");double p=predict(model,scores[i]);brier+=(p-hits[i])*(p-hits[i])/120;baseline+=(.3-hits[i])*(.3-hits[i])/120;int bin=Math.min(9,(int)(p*10));bins[bin]++;ps[bin]+=p;ys[bin]+=hits[i];}
        for(int i=0;i<10;i++)ece+=Math.abs(ps[i]-ys[i])/120;
        model.put("validation_brier",brier);model.put("validation_ece",ece);model.put("validation_bin_counts",new JSONArray(bins));
        model.put("validation_bin_sum_p",new JSONArray(ps));model.put("validation_bin_sum_y",new JSONArray(ys));
        model.put("validation_baseline_brier",baseline);
    }
}
