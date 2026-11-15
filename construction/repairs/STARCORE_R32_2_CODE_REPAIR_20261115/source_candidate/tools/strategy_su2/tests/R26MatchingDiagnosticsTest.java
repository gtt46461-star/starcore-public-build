package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/** Independent fixed-context regularized objective, every active finite difference and real deltas. */
public final class R26MatchingDiagnosticsTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    static int[] draw(Random random){int[] d=new int[10];for(int i=0;i<10;i++)d[i]=i+1;for(int i=9;i>0;i--){int q=random.nextInt(i+1),v=d[q];d[q]=d[i];d[i]=v;}return d;}
    static byte[] save(PermutationModelR26 model)throws IOException{ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.write(new DataOutputStream(bytes));return bytes.toByteArray();}
    static double objective(PermutationModelR26 model,int[] context,int[] target)throws Exception{
        double[][] bias=(double[][])field(model,"bias");double[][][] weights=(double[][][])field(model,"weights");double[][] scores=new double[10][10];double penalty=0;for(int p=0;p<10;p++)for(int n=0;n<10;n++){scores[p][n]=bias[p][n];penalty+=bias[p][n]*bias[p][n];if(context!=null)for(int q=0;q<10;q++){double value=weights[p*10+n][q][context[q]-1];scores[p][n]+=value/10;penalty+=value*value;}}
        double loss=ExactPermutationR26.infer(scores,null).logZ;for(int p=0;p<10;p++)loss-=scores[p][target[p]-1];return loss+.00005*penalty;
    }
    static double[] parameters(PermutationModelR26 model)throws Exception{double[] flat=new double[10100];int at=0;for(double[] row:(double[][])field(model,"bias"))for(double value:row)flat[at++]=value;for(double[][] matrix:(double[][][])field(model,"weights"))for(double[] row:matrix)for(double value:row)flat[at++]=value;return flat;}
    public static void main(String[] args)throws Exception{
        double maxError=0;for(boolean hasContext:new boolean[]{false,true}){
            PermutationModelR26 model=new PermutationModelR26(.02);Random random=new Random(261717);if(hasContext)for(int i=0;i<20;i++)model.update(draw(random));int[] context=(int[])field(model,"previous");context=context==null?null:context.clone();int[] target=draw(random);byte[] original=save(model);double[] beforeParameters=parameters(model);double before=objective(model,context,target),gradientSquared=0,eps=1e-4;
            double[][] bias=(double[][])field(model,"bias");double[][][] weights=(double[][][])field(model,"weights");for(int p=0;p<10;p++)for(int n=0;n<10;n++){
                double value=bias[p][n];bias[p][n]=value+eps;double plus=objective(model,context,target);bias[p][n]=value-eps;double minus=objective(model,context,target);bias[p][n]=value;double gradient=(plus-minus)/(2*eps);gradientSquared+=gradient*gradient;
                if(context!=null)for(int q=0;q<10;q++){int index=context[q]-1;value=weights[p*10+n][q][index];weights[p*10+n][q][index]=value+eps;plus=objective(model,context,target);weights[p*10+n][q][index]=value-eps;minus=objective(model,context,target);weights[p*10+n][q][index]=value;gradient=(plus-minus)/(2*eps);gradientSquared+=gradient*gradient;}
            }
            check(Arrays.equals(original,save(model)),"Oracle changed its input model");model.update(target);TrainingDiagnosticsR26.Step step=model.lastTrainingStep();check(Math.abs(step.lossBefore-before)<1e-10,"Before objective/active regularizer differs");check(Math.abs(step.lossAfter-objective(model,context,target))<1e-10,"After objective changed pre-label context");double error=Math.abs(step.gradientNorm-Math.sqrt(gradientSquared));maxError=Math.max(maxError,error);check(error<2e-5,"Active-coordinate finite difference gradient mismatch");double[] after=parameters(model);double delta=0;for(int i=0;i<after.length;i++){double d=after[i]-beforeParameters[i];delta+=d*d;}check(Math.abs(step.parameterDeltaNorm-Math.sqrt(delta))<1e-12,"Actual parameter delta differs");
            PermutationModelR26 other=new PermutationModelR26(.02);other.read(new DataInputStream(new ByteArrayInputStream(original)));check(other.lastTrainingStep()==null,"Restore invented a measured step");other.update(draw(random));check(step.inputHash.equals(other.lastTrainingStep().inputHash),"Input digest observed target");long updates=model.completedTrainingUpdates();model.resetTemporalContext();check(model.completedTrainingUpdates()==updates&&model.lastTrainingStep()==null,"Gap reset lifetime or reused a measurement");
        }
        PermutationModelR26 model=new PermutationModelR26(.02);Random random=new Random(17001);for(int i=0;i<8;i++)model.update(draw(random));byte[] original=save(model);int[] target=draw(random);boolean canceled=false;
        for(int operations:new int[]{1,100,1000,10000,100000}){model.read(new DataInputStream(new ByteArrayInputStream(original)));ExecutionBudgetR26.enter(new BudgetTokenSU1(30000,operations,()->false));try{model.update(target);}catch(BudgetTokenSU1.Yield expected){canceled=true;check(Arrays.equals(original,save(model))&&model.lastTrainingStep()==null,"Interrupted staged update mutated persistent state");}finally{ExecutionBudgetR26.leave();}}check(canceled,"Cancellation never reached an actual inference poll");
        model.read(new DataInputStream(new ByteArrayInputStream(original)));byte[] corrupt=original.clone();Arrays.fill(corrupt,corrupt.length-8,corrupt.length,(byte)0xff);try{model.read(new DataInputStream(new ByteArrayInputStream(corrupt)));throw new AssertionError("Invalid weight restored");}catch(IOException expected){}check(Arrays.equals(original,save(model)),"Rejected restore damaged learned state");PermutationModelR26 copy=new PermutationModelR26(.02);copy.read(new DataInputStream(new ByteArrayInputStream(original)));model.update(target);copy.update(target);check(Arrays.equals(save(model),save(copy)),"Full checkpoint/next update differs");
        if(args.length>0)java.nio.file.Files.write(java.nio.file.Paths.get(args[0]),save(model));
        System.out.println("R26_MATCHING_DIAGNOSTICS_PASS|maxFiniteDifferenceError="+maxError+"|allActiveCoordinates=true|fixedContextNllAndActiveL2=true|actualParameterDelta=true|inputExcludesTarget=true|lifetimeSurvivesGap=true|restoreNextAndCancellationAtomic=true|scope=SYNTHETIC_KERNEL_NOT_NATURAL_QUALITY");
    }
}
