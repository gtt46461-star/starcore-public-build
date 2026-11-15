package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;

/** Held-later causal recurrence and destructive controls; synthetic, never natural quality. */
public final class R26F20MechanismTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static byte[] encode(OnlineModelsSU2.Model model)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));return b.toByteArray();}
    static float[][] forecast(OnlineModelsSU2.Model model){float[][] p=new float[10][10];model.predict(p,new float[model.stateWidth()]);for(float[] row:p){double sum=0;for(float value:row){require(Float.isFinite(value)&&value>=0&&value<=1,"invalid probability");sum+=value;}require(Math.abs(sum-1)<1e-5,"unnormalized probability");}return p;}
    static void same(float[][] a,float[][] b){for(int p=0;p<10;p++)for(int n=0;n<10;n++)require(Float.floatToIntBits(a[p][n])==Float.floatToIntBits(b[p][n]),"forecast changed");}
    static int[] randomDraw(Random r){int[] d={1,2,3,4,5,6,7,8,9,10};for(int p=9;p>0;p--){int q=r.nextInt(p+1),v=d[p];d[p]=d[q];d[q]=v;}return d;}
    static Map<String,String> parameters(int slots,String mode){Map<String,String> p=new TreeMap<>();p.put("abi","1");p.put("fragments","1");p.put("slots",Integer.toString(slots));p.put("retrieval",mode);return p;}
    static double[] experiment(boolean recurring,long seed)throws Exception{
        OnlineModelsSU2.Model[] models=new OnlineModelsSU2.Model[4];String[] modes={"MATCH","NONE","MISMATCH","EXPIRED"};for(int i=0;i<4;i++)models[i]=ModelReplaySU2.build("R26F20",parameters(16,modes[i]));
        double[] loss=new double[4];Random random=new Random(seed);int samples=0;
        for(int t=0;t<10000;t++){
            float[][][] forecasts=new float[4][][];for(int i=0;i<4;i++)forecasts[i]=forecast(models[i]);
            int[] draw;if(recurring){draw=new int[10];int regime=(t/64)%3;for(int p=0;p<10;p++)draw[p]=(p+regime*3)%10+1;if(random.nextDouble()<.03){int p=random.nextInt(10),q=random.nextInt(10),v=draw[p];draw[p]=draw[q];draw[q]=v;}}else draw=randomDraw(random);
            if(t>=6000){for(int i=0;i<4;i++)for(int p=0;p<10;p++)loss[i]-=Math.log(Math.max(1e-12,forecasts[i][p][draw[p]-1]))/10;samples++;}
            for(OnlineModelsSU2.Model model:models)model.update(draw);
        }
        for(int i=0;i<4;i++)loss[i]/=samples;return loss;
    }
    public static void main(String[] args)throws Exception{
        Random random=new Random(2620001);OnlineModelsSU2.Model legacy=ModelReplaySU2.build("REGMEM",ModelEvolutionSU2.centralParams("REGMEM")),oldPolicy=ModelReplaySU2.build("R26F20",Collections.singletonMap("abi","1"));
        for(int t=0;t<256;t++){same(forecast(legacy),forecast(oldPolicy));int[] draw=randomDraw(random);legacy.update(draw);oldPolicy.update(draw);}require(Arrays.equals(encode(legacy),encode(oldPolicy)),"old F20 numerical state/codec changed");
        ModelGenomeSU2 parent=FamilyRegistryR26.genome("F20");require(!parent.key().equals(new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("R26F20",Collections.singletonMap("abi","1"),1)),"STATIC",true,"MASS",.15,.2,false).key()),"new mechanism aliases the old policy");
        List<StructuralProposalR26.Proposal> edits=StructuralProposalR26.mechanismChildren(parent);require(edits.size()==4,"missing slot edit or memory controls");
        require("STRUCTURAL_CANDIDATE".equals(edits.get(0).category)&&!StructuralProposalR26.structure(parent).equals(StructuralProposalR26.structure(edits.get(0).genome)),"slot graph edit is cosmetic");
        for(int i=1;i<4;i++)require("MECHANISM_ABLATION".equals(edits.get(i).category)&&StructuralProposalR26.control(edits.get(i).genome),"destructive memory control can enter the live family pool");
        for(String mode:new String[]{"MATCH","NONE","MISMATCH","EXPIRED"}){
            FamilyF20KernelR26 model=(FamilyF20KernelR26)ModelReplaySU2.build("R26F20",parameters(16,mode));
            for(int t=0;t<400;t++){forecast(model);int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=(p+3*((t/64)%3))%10+1;model.update(draw);}
            require(model.trainedPeriods()==400&&model.occupied()<=16,"unbounded memory or lost clock");require(mode.equals("NONE")?model.occupied()==0:model.occupied()>1,"memory mechanism was not executed");
            byte[] saved=encode(model);require(saved.length<100000,"checkpoint memory budget");FamilyF20KernelR26 copy=(FamilyF20KernelR26)ModelReplaySU2.build("R26F20",parameters(16,mode));DataInputStream in=new DataInputStream(new ByteArrayInputStream(saved));copy.read(in);require(in.read()==-1,"trailing checkpoint");same(forecast(model),forecast(copy));
            if(mode.equals("MATCH")&&args.length==1){java.nio.file.Path sample=java.nio.file.Paths.get(args[0]);java.nio.file.Files.write(sample,saved);Map<String,Object> manifest=new TreeMap<>();manifest.put("scope","SYNTHETIC_KERNEL_STATE_ONLY; NO_FUTURE_QUALIFICATION");manifest.put("bytes",saved.length);manifest.put("stateSha256",PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(saved)));manifest.put("hashEncoding","SHA256_OF_BASE64_STATE; SAME_AS_R26_CHECKPOINT_STATE_HASH");manifest.put("parameters",parameters(16,mode));manifest.put("trainedPeriods",model.trainedPeriods());manifest.put("occupiedFragments",model.occupied());manifest.put("validFragments",model.validFragments());java.nio.file.Files.write(java.nio.file.Paths.get(args[0]+".json"),PolicyIdentityR26.canonical(manifest).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
            int[] next=randomDraw(random);model.update(next);copy.update(next);require(Arrays.equals(encode(model),encode(copy)),"next update lost exact continuation");
            String learned=model.learnedParameterHash();long clock=model.trainedPeriods();TemporalResetR26.reset(model);TemporalResetR26.reset(copy);require(clock==model.trainedPeriods()&&learned.equals(model.learnedParameterHash()),"gap discarded learned memory or lifetime clock");model.update(next);copy.update(next);require(Arrays.equals(encode(model),encode(copy)),"gap continuation changed");
            byte[] before=encode(model);ExecutionBudgetR26.enter(new BudgetTokenSU1(1000,100000,()->true));try{model.update(next);throw new AssertionError("uncancellable memory operation");}catch(BudgetTokenSU1.Yield expected){}finally{ExecutionBudgetR26.leave();}require(Arrays.equals(before,encode(model)),"cancelled update changed state");
            try{ModelReplaySU2.build("R26F20",parameters(24,mode)).read(new DataInputStream(new ByteArrayInputStream(saved)));throw new AssertionError("changed graph accepted checkpoint");}catch(IOException expected){require(expected.getMessage().contains("F20_CHECKPOINT_GRAPH"),"wrong graph error");}
            byte[] corrupt=saved.clone();int clockOffset=10+mode.length();Arrays.fill(corrupt,clockOffset,clockOffset+8,(byte)0xff);byte[] unchanged=encode(copy);try{copy.read(new DataInputStream(new ByteArrayInputStream(corrupt)));throw new AssertionError("corrupt clock accepted");}catch(IOException expected){}require(Arrays.equals(unchanged,encode(copy)),"failed restore partially changed learner");
        }
        try{ModelReplaySU2.build("R26F20",parameters(64,"MATCH"));throw new AssertionError("unbounded memory graph");}catch(IllegalArgumentException expected){}
        double[] planted=experiment(true,2620201),iid=experiment(false,2620202);System.out.println("F20_EXPERIMENT|recurrence="+Arrays.toString(planted)+"|iid="+Arrays.toString(iid));
        require(planted[0]+.02<planted[1],"removing memory did not remove its recurrence advantage");require(planted[0]+.05<planted[2],"wrong state retrieval did not destroy the mechanism");require(planted[0]+.02<planted[3],"expired fragments retained the recurrence advantage");require(iid[0]>=iid[1]-.03,"IID negative control acquired a large false advantage");
        System.out.println("R26_F20_MECHANISM_PASS|heldoutPeriods=4000|memoryNll="+planted[0]+"|noMemoryNll="+planted[1]+"|mismatchNll="+planted[2]+"|expiredNll="+planted[3]+"|iidFullNll="+iid[0]+"|iidNoMemoryNll="+iid[1]+"|notNaturalQuality=true");
    }
}
