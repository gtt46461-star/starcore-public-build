package com.ethk3.novaonepro;

import com.ethk3.novaonepro.r952.CandidateCompilerR952;
import java.util.*;
import org.json.*;

/** Read-only local sensitivity of the actual frozen inference input and immutable model. */
public final class FrozenInputStabilityR82 {
    public static final String REVISION="R82_FROZEN_FEATURE_1PCT_EXPERT_DELETE_V1";
    public static final String POLICY_HASH=V51ChecksumEngine.sha256(REVISION+"|FEATURES<=8|BOTH_SIGNS|MAX_EXPERTS8|DIAGNOSTIC_ONLY");
    private FrozenInputStabilityR82(){}
    public static JSONObject evaluate(CandidateCompilerR952.Model model,double[] frozen)throws JSONException {
        if(model==null||frozen==null||frozen.length!=model.inputDimension())throw new IllegalArgumentException("R82_STABILITY_INPUT");
        String before=model.payload();double[] original=frozen.clone(),base=model.probabilities(original);JSONArray rows=new JSONArray();
        int attempted=0;double maxL1=0,maxSelectedDelta=0;int[] top=top3(base);String missing="";
        if(!model.isLegacy()&&model.modern().kind==CandidateCompilerR952.ModelKind.EXPERT_HEDGE){
            CandidateCompilerR952.Modern modern=model.modern();double[] weights=modern.weights();
            for(int excluded=0;excluded<modern.expertN;excluded++){
                if(ResearchWorkBudgetR927.remainingMs()<80){missing="DIAGNOSTIC_BUDGET_EXHAUSTED";break;}
                double maximum=Double.NEGATIVE_INFINITY;for(int j=0;j<weights.length;j++)if(j!=excluded)maximum=Math.max(maximum,weights[j]);double total=0;for(int j=0;j<weights.length;j++)if(j!=excluded)total+=Math.exp(weights[j]-maximum);
                double[] result=new double[10];for(int j=0;j<weights.length;j++)if(j!=excluded){double w=Math.exp(weights[j]-maximum)/total;for(int d=0;d<10;d++)result[d]+=w*original[j*10+d];}
                JSONObject row=metric(base,result,top);row.put("kind","DELETE_EXPERT_RENORMALIZED");row.put("expert_index",excluded);row.put("expert_freeze_hash",modern.experts.get(excluded).freezeHash);rows.put(row);attempted++;maxL1=Math.max(maxL1,row.getDouble("l1"));maxSelectedDelta=Math.max(maxSelectedDelta,row.getDouble("selected_k3_mass_abs_delta"));
            }
        }else{
            int features=model.isLegacy()?frozen.length:model.modern().featureN;
            if(!model.isLegacy()&&model.modern().kind==CandidateCompilerR952.ModelKind.FIXED_GRAPH)features=frozen.length;
            int included=0;
            for(int i=0;i<features&&included<8;i++){
                if(!Double.isFinite(original[i]))throw new IllegalArgumentException("R82_STABILITY_NONFINITE");
                if(!model.isLegacy()&&model.modern().featureN>0&&original[model.modern().featureN+i]==0)continue;
                included++;
                for(int sign:new int[]{-1,1}){
                    if(ResearchWorkBudgetR927.remainingMs()<80){missing="DIAGNOSTIC_BUDGET_EXHAUSTED";break;}
                    double[] perturbed=original.clone();double delta=.01*Math.max(Math.abs(original[i]),1e-6);perturbed[i]+=sign*delta;
                    if(!model.isLegacy()&&model.modern().kind==CandidateCompilerR952.ModelKind.FIXED_GRAPH)perturbed[i]=Math.max(0,perturbed[i]);
                    JSONObject row=metric(base,model.probabilities(perturbed),top);row.put("kind","FEATURE_PLUS_MINUS_ONE_PERCENT");row.put("feature_index",i);row.put("sign",sign);rows.put(row);attempted++;maxL1=Math.max(maxL1,row.getDouble("l1"));maxSelectedDelta=Math.max(maxSelectedDelta,row.getDouble("selected_k3_mass_abs_delta"));
                }
                if(!missing.isEmpty())break;
            }
            if(included==0)missing="NO_OBSERVED_PERTURBABLE_FEATURE";
        }
        if(!Arrays.equals(frozen,original)||!before.equals(model.payload()))throw new IllegalStateException("R82_STABILITY_FROZEN_MUTATION");
        JSONObject out=new JSONObject();out.put("revision",REVISION);out.put("policy_hash",POLICY_HASH);out.put("model_hash",model.hash());out.put("input_hash",inputHash(original));out.put("attempted_n",attempted);out.put("missing_reason",missing);out.put("status",attempted==0?"MISSING":missing.isEmpty()?"COMPLETE":"PARTIAL");out.put("max_l1",attempted==0?JSONObject.NULL:maxL1);out.put("max_selected_k3_mass_abs_delta",attempted==0?JSONObject.NULL:maxSelectedDelta);out.put("observations",rows);out.put("may_authorize_real",false);return out;
    }
    static String inputHash(double[] x){StringBuilder s=new StringBuilder("R82_STABILITY_INPUT|");for(double v:x)s.append(Double.toHexString(v)).append(';');return V51ChecksumEngine.sha256(s.toString());}
    private static JSONObject metric(double[] base,double[] p,int[] selected)throws JSONException {
        double l1=0,a=0,b=0;for(int i=0;i<10;i++){if(!Double.isFinite(p[i])||p[i]<0||p[i]>1)throw new IllegalArgumentException("R82_STABILITY_PROBABILITY");l1+=Math.abs(base[i]-p[i]);}for(int i:selected){a+=base[i];b+=p[i];}int overlap=0;for(int x:top3(p))for(int y:selected)if(x==y)overlap++;
        JSONObject row=new JSONObject();row.put("l1",l1);row.put("selected_k3_mass_abs_delta",Math.abs(a-b));row.put("top_k3_overlap",overlap);return row;
    }
    private static int[] top3(double[] p){Integer[] order=new Integer[10];for(int i=0;i<10;i++)order[i]=i;Arrays.sort(order,(a,b)->{int d=Double.compare(p[b],p[a]);return d==0?Integer.compare(a,b):d;});return new int[]{order[0],order[1],order[2]};}
}
