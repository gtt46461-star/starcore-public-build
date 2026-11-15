package com.ethk3.novaonepro;
import java.util.*;
import org.json.*;

/** Pure bounded numerical calibration. Selection sees earlier FIT rows only. */
public final class ResearchLogisticCalibrationR932 {
    private ResearchLogisticCalibrationR932(){}
    static final String COMPARED="R82_CAL_COMPARE_80_40_REFIT120_V1";
    private static final String[] KINDS={"LOGISTIC","BETA","ISOTONIC","DOMAIN_SHRINK"};
    private static double clip(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    private static double sigmoid(double x){return 1/(1+Math.exp(-clip(x,-35,35)));}
    private static double nll(double p,double y){p=clip(p,1e-12,1-1e-12);return -(y*Math.log(p)+(1-y)*Math.log1p(-p));}
    private static void require(double[] s,double[] y,int n){
        if(s==null||y==null||s.length<n||s.length!=y.length)throw new IllegalArgumentException("R932_CAL_FIT_DIMENSION");
        for(int i=0;i<n;i++)if(!Double.isFinite(s[i])||(y[i]!=0&&y[i]!=1))throw new IllegalArgumentException("R932_CAL_FIT_NONFINITE_OR_LABEL");
    }
    /** Compatibility entry: existing module calibrators keep their original logistic protocol. */
    static JSONObject fit(double[] scores,double[] hits)throws JSONException {require(scores,hits,120);JSONObject m=fitKind("LOGISTIC",scores,hits,120);m.remove("kind");m.remove("fit_n");m.remove("weight2");return m;}
    static JSONObject fitCompared(double[] scores,double[] hits)throws JSONException {
        require(scores,hits,120);JSONArray comparison=new JSONArray();int best=0;double bestB=Double.POSITIVE_INFINITY,bestL=Double.POSITIVE_INFINITY;
        for(int k=0;k<KINDS.length;k++){
            ResearchWorkBudgetR927.checkpoint();JSONObject candidate=fitKind(KINDS[k],scores,hits,80);double b=0,l=0;
            for(int i=80;i<120;i++){double p=predict(candidate,scores[i]);b+=(p-hits[i])*(p-hits[i])/40;l+=nll(p,hits[i])/40;}
            JSONObject row=new JSONObject();row.put("kind",KINDS[k]);row.put("fit_n",80);row.put("selection_n",40);row.put("selection_brier",b);row.put("selection_nll",l);comparison.put(row);
            // Stable order favours the simpler logistic when the losses are numerically tied.
            if(b<bestB-1e-12||Math.abs(b-bestB)<=1e-12&&l<bestL-1e-12){best=k;bestB=b;bestL=l;}
        }
        JSONObject model=fitKind(KINDS[best],scores,hits,120);model.put("comparison_revision",COMPARED);model.put("comparison",comparison);
        model.put("selected_kind",KINDS[best]);model.put("selection_scope","EARLIER_FIT_80_TRAIN_40_SELECT_THEN_REFIT120");
        model.put("domain_definition","FOUR_EARLIER_TRAIN_SCORE_QUANTILE_STRATA_NOT_EXTERNAL_REGIME_LABELS");
        if(model.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("R932_CAL_FIT_MODEL_BOUND");return model;
    }
    private static JSONObject fitKind(String kind,double[] s,double[] y,int n)throws JSONException {
        double mean=0,variance=0,success=0;for(int i=0;i<n;i++){mean+=s[i]/n;success+=y[i];}for(int i=0;i<n;i++)variance+=(s[i]-mean)*(s[i]-mean)/n;
        double scale=Math.max(1e-6,Math.sqrt(variance));if(!Double.isFinite(mean)||!Double.isFinite(scale))throw new IllegalArgumentException("R932_CAL_FIT_OVERFLOW");
        JSONObject m=new JSONObject();m.put("revision","R932_SCORE_CAL_LOGISTIC_V1");m.put("kind",kind);m.put("fit_n",n);m.put("mean",mean);m.put("scale",scale);
        if("LOGISTIC".equals(kind)||"BETA".equals(kind)){
            double w=0,w2=0,b=Math.log((success+.5)/(n-success+.5));
            for(int epoch=0;epoch<160;epoch++){
                ResearchWorkBudgetR927.checkpoint();double dw=0,dw2=0,db=0;
                for(int i=0;i<n;i++){double x=clip((s[i]-mean)/scale,-8,8),a=x,c=0;
                    if("BETA".equals(kind)){double q=clip(sigmoid(x),1e-6,1-1e-6);a=Math.log(q);c=-Math.log1p(-q);}
                    double residual=sigmoid(b+w*a+w2*c)-y[i];dw+=residual*a/n;dw2+=residual*c/n;db+=residual/n;
                }
                w-=.05*(dw+.1*w);w2-=.05*(dw2+.1*w2);b-=.05*db;
                if("BETA".equals(kind)){w=Math.max(0,w);w2=Math.max(0,w2);}
            }
            if(!Double.isFinite(w)||!Double.isFinite(w2)||!Double.isFinite(b))throw new IllegalArgumentException("R932_CAL_FIT_OVERFLOW");m.put("weight",w);m.put("weight2",w2);m.put("bias",b);
        }else{
            Integer[] order=new Integer[n];for(int i=0;i<n;i++)order[i]=i;Arrays.sort(order,(a,b)->Double.compare(s[a],s[b]));
            ArrayList<Double> upper=new ArrayList<>(),sum=new ArrayList<>();ArrayList<Integer> count=new ArrayList<>();
            if("ISOTONIC".equals(kind)){
                // Merge equal x before PAVA so serialization has no ambiguous duplicate knot.
                for(int at=0;at<n;){double x=s[order[at]],sy=0;int nn=0;do{sy+=y[order[at++]];nn++;}while(at<n&&Double.compare(s[order[at]],x)==0);upper.add(x);sum.add(sy);count.add(nn);
                    while(sum.size()>1){int j=sum.size()-1;if(sum.get(j-1)/count.get(j-1)<=sum.get(j)/count.get(j))break;sum.set(j-1,sum.get(j-1)+sum.remove(j));count.set(j-1,count.get(j-1)+count.remove(j));upper.set(j-1,upper.remove(j));}
                }
            }else if("DOMAIN_SHRINK".equals(kind)){
                // Domains are declared score strata; each receives shrinkage to the earlier global mean.
                for(int at=0;at<n;){int end=Math.min(n,((at*4/n)+1)*n/4);end=Math.max(at+1,end);while(end<n&&Double.compare(s[order[end]],s[order[end-1]])==0)end++;double sy=0;for(int j=at;j<end;j++)sy+=y[order[j]];upper.add(s[order[end-1]]);sum.add(sy);count.add(end-at);at=end;}
            }else throw new IllegalArgumentException("R932_CAL_FIT_KIND");
            JSONArray knots=new JSONArray(),probabilities=new JSONArray();for(int j=0;j<upper.size();j++){knots.put(upper.get(j));probabilities.put("DOMAIN_SHRINK".equals(kind)?(sum.get(j)+20*(success+.5)/(n+1))/(count.get(j)+20):clip(sum.get(j)/count.get(j),1e-6,1-1e-6));}m.put("knots",knots);m.put("probabilities",probabilities);
        }
        return m;
    }
    static double predict(JSONObject model,double score)throws JSONException {
        if(model==null||!Double.isFinite(score))throw new IllegalArgumentException("R932_CAL_SCORE_NONFINITE");String kind=model.optString("kind","LOGISTIC");
        double mean=model.getDouble("mean"),scale=model.getDouble("scale");if(!Double.isFinite(mean)||!Double.isFinite(scale)||scale<=0)throw new IllegalArgumentException("R932_CAL_MODEL_PARAMETERS");double p;
        if("LOGISTIC".equals(kind)||"BETA".equals(kind)){double x=clip((score-mean)/scale,-8,8),b=model.getDouble("bias"),w=model.getDouble("weight"),w2=model.optDouble("weight2",0);if(!Double.isFinite(b)||!Double.isFinite(w)||!Double.isFinite(w2))throw new IllegalArgumentException("R932_CAL_MODEL_PARAMETERS");if("BETA".equals(kind)){double q=clip(sigmoid(x),1e-6,1-1e-6);p=sigmoid(b+w*Math.log(q)-w2*Math.log1p(-q));}else p=sigmoid(b+w*x);
        }else if("ISOTONIC".equals(kind)||"DOMAIN_SHRINK".equals(kind)){JSONArray knots=model.getJSONArray("knots"),ps=model.getJSONArray("probabilities");if(knots.length()<1||knots.length()>120||ps.length()!=knots.length())throw new IllegalArgumentException("R932_CAL_MODEL_PARAMETERS");int at=knots.length()-1;boolean selected=false;double prior=Double.NEGATIVE_INFINITY;for(int i=0;i<knots.length();i++){double v=knots.getDouble(i),q=ps.getDouble(i);if(!Double.isFinite(v)||v<=prior||!Double.isFinite(q)||q<0||q>1)throw new IllegalArgumentException("R932_CAL_MODEL_PARAMETERS");prior=v;if(!selected&&score<=v){at=i;selected=true;}}p=ps.getDouble(at);
        }else throw new IllegalArgumentException("R932_CAL_MODEL_PARAMETERS");if(!Double.isFinite(p)||p<0||p>1)throw new IllegalArgumentException("R932_CAL_MODEL_PARAMETERS");return p;
    }
    static void validate(JSONObject model,double[] scores,double[] hits)throws JSONException {
        if(scores==null||hits==null||scores.length!=241||hits.length!=241)throw new IllegalArgumentException("R932_CAL_HOLDOUT_DIMENSION");
        require(scores,hits,241);double earlier=0;for(int i=0;i<120;i++)earlier+=hits[i];earlier=(earlier+.5)/121;
        double brier=0,ece=0,baseline=0,loss=0,baseLoss=0,simpleB=0,simpleL=0;int[] bins=new int[10];double[] ps=new double[10],ys=new double[10];
        for(int i=121;i<241;i++){double p=predict(model,scores[i]),y=hits[i];brier+=(p-y)*(p-y)/120;baseline+=(.3-y)*(.3-y)/120;loss+=nll(p,y)/120;baseLoss+=nll(.3,y)/120;simpleB+=(earlier-y)*(earlier-y)/120;simpleL+=nll(earlier,y)/120;int bin=Math.min(9,(int)(p*10));bins[bin]++;ps[bin]+=p;ys[bin]+=y;}
        JSONArray lower=new JSONArray(),upper=new JSONArray();for(int i=0;i<10;i++){ece+=Math.abs(ps[i]-ys[i])/120;if(bins[i]==0){lower.put(JSONObject.NULL);upper.put(JSONObject.NULL);}else{double n=bins[i],ph=ys[i]/n,z=1.959963984540054,d=1+z*z/n,c=(ph+z*z/(2*n))/d,r=z*Math.sqrt(ph*(1-ph)/n+z*z/(4*n*n))/d;lower.put(Math.max(0,c-r));upper.put(Math.min(1,c+r));}}
        model.put("validation_brier",brier);model.put("validation_nll",loss);model.put("validation_ece",ece);model.put("validation_n",120);model.put("validation_bin_counts",new JSONArray(bins));model.put("validation_bin_sum_p",new JSONArray(ps));model.put("validation_bin_sum_y",new JSONArray(ys));model.put("validation_bin_wilson95_lower",lower);model.put("validation_bin_wilson95_upper",upper);model.put("validation_baseline_brier",baseline);model.put("validation_baseline_nll",baseLoss);model.put("validation_earlier_constant_p",earlier);model.put("validation_earlier_constant_brier",simpleB);model.put("validation_earlier_constant_nll",simpleL);
    }
}
