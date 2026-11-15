package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Independent fixed-input count-head objective, finite differences and sufficient-statistic changes. */
public final class R26FragmentDiagnosticsTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static double[] signature(FamilyF20KernelR26 model)throws Exception{Method method=FamilyF20KernelR26.class.getDeclaredMethod("signature");method.setAccessible(true);return (double[])method.invoke(model);}
    static int[] draw(boolean first){int[] d=new int[10];for(int p=0;p<10;p++)d[p]=first?p+1:10-p;return d;}
    static double distance(double[] a,double[] b){double d=0;for(int i=0;i<a.length;i++){double v=a[i]-b[i];d+=v*v;}return d/20;}
    static double objective(FamilyF20KernelR26 model,double[] input,boolean available,long clock,int[] draw)throws Exception{
        Object[] fragments=(Object[])field(model,"fragments");FamilyF20KernelR26.Retrieval mode=(FamilyF20KernelR26.Retrieval)field(model,"mode");Object nearest=null,furthest=null;double best=Double.POSITIVE_INFINITY,worst=-1;
        if(available&&mode!=FamilyF20KernelR26.Retrieval.NONE)for(Object fragment:fragments)if(fragment!=null&&(long)field(fragment,"updates")>0&&clock-(long)field(fragment,"lastUsed")<=(mode==FamilyF20KernelR26.Retrieval.EXPIRED?32:5000)){
            double d=distance(input,(double[])field(fragment,"signature"));if(d<best){best=d;nearest=fragment;}if(d>worst){worst=d;furthest=fragment;}
        }
        Object selected=mode==FamilyF20KernelR26.Retrieval.MISMATCH?furthest:nearest;double blend=0;if(selected!=null&&best<=.35){long n=(long)field(selected,"updates");blend=.9*n/(n+8.0)*Math.exp(-3*best);}
        double[] global=(double[])field(model,"global"),expert=selected==null?null:(double[])field(selected,"expert");double loss=0;
        for(int p=0;p<10;p++){double sum=0,local=0;for(int n=0;n<10;n++){sum+=global[p*10+n];if(expert!=null)local+=expert[p*10+n];}int y=draw[p]-1;double probability=(1-blend)*global[p*10+y]/sum+(blend>0?blend*expert[p*10+y]/local:0);loss-=Math.log(Math.max(1e-12,probability))/10;}return loss;
    }
    static List<double[]> vectors(FamilyF20KernelR26 model)throws Exception{List<double[]> values=new ArrayList<>();values.add((double[])field(model,"global"));for(Object fragment:(Object[])field(model,"fragments"))values.add(fragment==null?new double[100]:(double[])field(fragment,"expert"));return values;}
    static double[] counts(FamilyF20KernelR26 model)throws Exception{List<double[]> vectors=vectors(model);double[] flat=new double[vectors.size()*100];for(int slot=0;slot<vectors.size();slot++)System.arraycopy(vectors.get(slot),0,flat,slot*100,100);return flat;}
    static byte[] save(FamilyF20KernelR26 model)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.write(new DataOutputStream(bytes));return bytes.toByteArray();}
    public static void main(String[] args)throws Exception{
        double maximumGradientError=0;
        for(FamilyF20KernelR26.Retrieval mode:FamilyF20KernelR26.Retrieval.values()){
            FamilyF20KernelR26 model=new FamilyF20KernelR26(16,mode);for(int t=0;t<160;t++)model.update(draw((t/32)%2==0));byte[] original=save(model);double[] input=signature(model),beforeCounts=counts(model);long clock=model.completedTrainingUpdates();boolean available=model.availableScaleMask()!=0;int[] target=draw(false);double before=objective(model,input,available,clock,target),norm=0;
            Object[] fragments=(Object[])field(model,"fragments");List<double[]> measured=new ArrayList<>();measured.add((double[])field(model,"global"));for(Object fragment:fragments)if(fragment!=null)measured.add((double[])field(fragment,"expert"));
            for(double[] vector:measured)for(int n=0;n<vector.length;n++){double value=vector[n],eps=Math.min(1e-4,value*.001);check(eps>0,"Unexpected zero test count");vector[n]=value+eps;double plus=objective(model,input,available,clock,target);vector[n]=value-eps;double minus=objective(model,input,available,clock,target);vector[n]=value;double gradient=(plus-minus)/(2*eps);norm+=gradient*gradient;}
            check(Arrays.equals(original,save(model)),"Finite-difference oracle changed its input model");model.update(target);TrainingDiagnosticsR26.Step step=model.lastTrainingStep();double error=Math.abs(step.gradientNorm-Math.sqrt(norm));maximumGradientError=Math.max(maximumGradientError,error);check(error<2e-5,"Local count gradient differs from finite differences: "+mode+" "+error);check(Math.abs(step.lossBefore-before)<1e-12,"Count loss differs from independent readout");check(Math.abs(step.lossAfter-objective(model,input,available,clock,target))<1e-12,"After loss used a revealed-label context or a different clock");double[] after=counts(model);double delta=0;for(int n=0;n<after.length;n++){double d=after[n]-beforeCounts[n];delta+=d*d;}check(Math.abs(step.parameterDeltaNorm-Math.sqrt(delta))<1e-12,"Sufficient-statistic change differs");
            FamilyF20KernelR26 other=new FamilyF20KernelR26(16,mode);other.read(new DataInputStream(new ByteArrayInputStream(original)));check(other.lastTrainingStep()==null,"Restore invented a new measurement");other.update(draw(true));check(step.inputHash.equals(other.lastTrainingStep().inputHash),"Input digest observed its target label");long updates=other.completedTrainingUpdates();other.resetTemporalContext();check(other.completedTrainingUpdates()==updates&&other.lastTrainingStep()==null,"Gap reset learned lifetime or reused measurement");
        }
        System.out.println("R26_FRAGMENT_DIAGNOSTICS_PASS|retrievalModes=4|maxFiniteDifferenceError="+maximumGradientError+"|fixedInputLossBeforeAfter=true|actualCountVectorDelta=true|inputDigestExcludesTarget=true|restoreAndGapNoInventedMeasurement=true|scope=SYNTHETIC_LOCAL_COUNT_COORDINATES_NOT_DEPLOYMENT_QUALITY");
    }
}
