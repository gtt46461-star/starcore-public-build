package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.nio.file.*;
import java.util.*;

/** Golden gradients use only an independently vectorized forward and finite differences. */
public final class RelGraphParitySU1Test {
    private static int checks;
    private static void same(double actual,Object expected,double tolerance){checks++;double value=Double.valueOf((String)expected);if(Math.abs(actual-value)>tolerance*(1+Math.abs(value)))throw new AssertionError("relgraph parity "+actual+" vs "+value);}
    private static void vector(double[] values,List<?> expected,double tolerance){if(values.length!=expected.size())throw new AssertionError("shape mismatch");for(int i=0;i<values.length;i++)same(values[i],expected.get(i),tolerance);}
    public static void main(String[] args)throws Exception{
        Map<String,Object> f=StrictCanonicalJson.readContextObject(Files.readString(Paths.get("reference/fixtures/relgraph_parity.json")));
        RelGraphModelSU1.Input input=RelGraphModelSU1.decodeInput((String)f.get("input"),(String)f.get("inputHash"));RelGraphOptimizerSU1 state=RelGraphCodecSU1.decode((String)f.get("state"),(String)f.get("stateHash"));Map<?,?> layers=(Map<?,?>)f.get("layers");
        double[][] p=RelGraphModelSU1.predict(state.weights,input,new BudgetTokenSU1(60000,1000000,null),(name,step,values)->vector(values,(List<?>)layers.get(name+":"+step),1e-10));List<?> expected=(List<?>)f.get("probability");for(int pos=0;pos<10;pos++)for(int n=0;n<10;n++)same(p[pos][n],expected.get(pos*10+n),1e-10);
        List<?> labels=(List<?>)f.get("label");int[] actual=new int[10];for(int i=0;i<10;i++)actual[i]=((Number)labels.get(i)).intValue();RelGraphModelSU1.Gradient gradient=RelGraphModelSU1.gradient(state.weights,input,actual,new BudgetTokenSU1(60000,1000000,null));same(gradient.loss,f.get("loss"),1e-10);
        Map<?,?> golden=(Map<?,?>)f.get("gradients");for(String key:RelGraphModelSU1.GROUPS)vector(gradient.tensors.get(key),(List<?>)golden.get(key),2e-7);
        List<?> coefficients=(List<?>)f.get("jointObjective");TrainingObjectiveSU1 joint=new TrainingObjectiveSU1(Double.valueOf((String)coefficients.get(0)),Double.valueOf((String)coefficients.get(1)),Double.valueOf((String)coefficients.get(2)),Double.valueOf((String)coefficients.get(3)));
        RelGraphModelSU1.Gradient jointGradient=RelGraphModelSU1.gradient(state.weights,input,actual,new BudgetTokenSU1(60000,1000000,null),joint);same(jointGradient.loss,f.get("jointLoss"),1e-10);
        golden=(Map<?,?>)f.get("jointGradients");for(String key:RelGraphModelSU1.GROUPS)vector(jointGradient.tensors.get(key),(List<?>)golden.get(key),2e-7);
        vector(new double[]{jointGradient.crossEntropyLoss,jointGradient.brierLoss,jointGradient.smoothnessLoss,jointGradient.l2Loss,jointGradient.resourceLoss},(List<?>)f.get("jointTerms"),1e-10);
        PortableArtifactSU1 jointAsset=PortableArtifactSU1.decode((String)f.get("jointPortableGraph"));
        if(!joint.hash.equals(jointAsset.graph.objective.hash)||!RelGraphCheckpointSU1.JOINT_FORMAT.equals(jointAsset.graph.format))throw new AssertionError("joint graph objective was not transported");
        RelGraphModelSU1.Weights updated=RelGraphModelSU1.update(state.weights,gradient,.03,1);golden=(Map<?,?>)f.get("updated");for(String key:RelGraphModelSU1.GROUPS)vector(updated.get(key),(List<?>)golden.get(key),1e-8);
        if(!input.hash.equals(RelGraphModelSU1.decodeInput(RelGraphModelSU1.encodeInput(input),HashingSU1.text(RelGraphModelSU1.encodeInput(input))).hash))throw new AssertionError("input transport changed facts");
        PortableArtifactSU1 imported=PortableArtifactSU1.decode((String)f.get("portableGraph"));if(imported.graph==null||!imported.program.equals("GMODEL("+imported.graph.hash+",4)"))throw new AssertionError("graph portable manifest not consumed");
        int[][] history=new int[120][10];for(int t=0;t<120;t++)for(int pos=0;pos<10;pos++)history[t][pos]=(t+pos)%10+1;double[][] raw=RelGraphModelSU1.predict(imported.graph.state.weights,new RelationTensorAdapterSU1.Factory(history).input(100,4,new BudgetTokenSU1(60000,1000000,null)),new BudgetTokenSU1(60000,1000000,null));double[][] scores=ProgramTreeSU1.parse(imported.program).score(new ProgramTreeSU1.Context(history,100).withGraphFitted(Collections.singletonMap(imported.graph.hash,new ProgramTreeSU1.GraphFitted(imported.graph.state.weights,1))));for(int pos=0;pos<10;pos++)for(int n=0;n<10;n++)same(scores[pos][n],Double.toHexString(ProgramTreeSU1.clean(Math.log(Math.max(1e-12,raw[pos][n])))),0);
        RelGraphCheckpointSU1 originalJoint=jointAsset.graph,resumedJoint=RelGraphCheckpointSU1.decode(originalJoint.wire);RelGraphModelSU1.Input continuationInput=new RelationTensorAdapterSU1.Factory(history).input(100,4,new BudgetTokenSU1(60000,1000000,null));
        RelGraphCheckpointSU1 jointChild=originalJoint.continueOne(continuationInput,history[100],originalJoint.datasetHash,"202610210002",new BudgetTokenSU1(60000,1000000,null));
        RelGraphCheckpointSU1 resumedChild=resumedJoint.continueOne(continuationInput,history[100],originalJoint.datasetHash,"202610210002",new BudgetTokenSU1(60000,1000000,null));
        RelGraphOptimizerSU1 expectedJoint=originalJoint.state.apply(RelGraphModelSU1.gradient(originalJoint.state.weights,continuationInput,history[100],new BudgetTokenSU1(60000,1000000,null),joint),originalJoint.learningRate,originalJoint.clipNorm);
        if(!jointChild.wire.equals(resumedChild.wire)||!jointChild.stateWire.equals(RelGraphCodecSU1.encode(expectedJoint))||!jointChild.objective.hash.equals(joint.hash))throw new AssertionError("joint objective/optimizer continuation was not consumed exactly");
        System.out.println("SU1_RELGRAPH_PARITY_PASS values="+checks+" groups=18");
    }
}
