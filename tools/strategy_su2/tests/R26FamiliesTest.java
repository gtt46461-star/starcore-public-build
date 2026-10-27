package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Distinct mechanism state/learning, legal probabilities and exact next-update recovery. */
public final class R26FamiliesTest {
    static void check(boolean yes,String message){if(!yes)throw new AssertionError(message);}
    static double loss(OnlineModelsSU2.Model m,int[] d){float[][] p=new float[10][10];m.predict(p,new float[m.stateWidth()]);double loss=0;for(int i=0;i<10;i++){double row=0;for(int j=0;j<10;j++){check(Float.isFinite(p[i][j])&&p[i][j]>=0&&p[i][j]<=1,"probability");row+=p[i][j];}check(Math.abs(row-1)<1e-5,"row normalization");loss-=Math.log(Math.max(1e-12,p[i][d[i]-1]));}return loss/10;}
    static byte[] save(OnlineModelsSU2.Model m)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();m.write(new DataOutputStream(b));return b.toByteArray();}
    public static void main(String[] args)throws Exception{
        int[] fixed={1,2,3,4,5,6,7,8,9,10};Set<String> states=new HashSet<>();
        for(String id:FamilyRegistryR26.EXECUTABLE){OnlineModelsSU2.Model m=FamilyRegistryR26.build(id);double before=loss(m,fixed);long start=System.nanoTime();int steps=id.equals("F19")?1200:220;
            for(int t=0;t<steps;t++){loss(m,fixed);m.update(fixed);}double after=loss(m,fixed);check(after<before-.01,id+" did not learn planted signal: "+before+" -> "+after);
            byte[] bytes=save(m);check(bytes.length<2<<20,id+" state budget");states.add(PolicyIdentityR26.sha256(Base64.getEncoder().encodeToString(bytes)));
            OnlineModelsSU2.Model restored=FamilyRegistryR26.build(id);restored.read(new DataInputStream(new ByteArrayInputStream(bytes)));int[] shifted={2,3,4,5,6,7,8,9,10,1};m.update(shifted);restored.update(shifted);check(Arrays.equals(save(m),save(restored)),id+" next-update restoration");
            ModelGenomeSU2 g=FamilyRegistryR26.genome(id);check(ModelGenomeSU2.parse(g.text()).key().equals(g.key()),id+" codec");
            System.out.println(id+" PASS loss "+before+" -> "+after+" bytes="+bytes.length+" ms="+(System.nanoTime()-start)/1000000);
        }
        for(String id:Arrays.asList("F03","F04","F05","F06","F07")){
            NeuralFamilyR26 m=new NeuralFamilyR26(id);for(int i=0;i<3;i++)m.update(fixed);
            java.lang.reflect.Field wf=NeuralFamilyR26.class.getDeclaredField("weights"),mf=NeuralFamilyR26.class.getDeclaredField("momentum");wf.setAccessible(true);mf.setAccessible(true);double[] w=(double[])wf.get(m),momentum=(double[])mf.get(m);int[] indices={0,400,1000};double[] numerical=new double[3],oldW=new double[3],oldM=new double[3];
            for(int j=0;j<indices.length;j++){int i=indices[j];oldW[j]=w[i];oldM[j]=momentum[i];double eps=1e-5;w[i]=oldW[j]+eps;double plus=m.objective(fixed);w[i]=oldW[j]-eps;double minus=m.objective(fixed);w[i]=oldW[j];numerical[j]=(plus-minus)/(2*eps);}
            m.update(fixed);float[] stats=new float[3];m.predict(new float[10][10],stats);double scale=stats[2]>5?5/stats[2]:1;
            for(int j=0;j<indices.length;j++){double observed=(oldW[j]-w[indices[j]])/.03,expected=.9*oldM[j]+.1*numerical[j]*scale;check(Math.abs(observed-expected)<1e-7,id+" finite-difference chain rule "+indices[j]+":"+observed+"/"+expected);}
            byte[] beforeCancel=save(m);ExecutionBudgetR26.enter(new BudgetTokenSU1(1000,100000,()->true));try{m.update(fixed);throw new AssertionError("uncancellable tape");}catch(BudgetTokenSU1.Yield expected){}finally{ExecutionBudgetR26.leave();}check(Arrays.equals(beforeCancel,save(m)),id+" immediate cancellation changed state");
        }
        check(states.size()==FamilyRegistryR26.EXECUTABLE.size(),"families share identical state");
        for(String id:Arrays.asList("F15","F16")){
            ModelGenomeSU2 g=FamilyRegistryR26.genome(id);int[][] draws=new int[160][10];for(int t=0;t<160;t++)for(int p=0;p<10;p++)draws[t][p]=(p+t%3)%10+1;
            ModelReplaySU2.Result replay=ModelReplaySU2.evaluate(g,new ModelReplaySU2.Library(draws,4<<20),32,256,64);ModelLiveSU2.Hub hub=new ModelLiveSU2.Hub();ModelLiveSU2 live=new ModelLiveSU2(g,hub,32,256,64);
            for(int t=0;t<160;t++){ModelLiveSU2.Decision d=live.decide();float[][] probabilities=live.pendingTable(t);double mass=0;for(int n=0;n<10;n++)if((d.mask&(1<<n))!=0)mass+=probabilities[d.position-1][n];check(Math.abs(d.phat-mass)<1e-6,"utility relabelled as probability");check(d.mask==replay.mask[t]&&d.position==replay.position[t]&&d.act==replay.act[t],"set replay/live mismatch");boolean hit=live.settle(draws[t]);check(hit==replay.hit[t],"set hit mismatch");hub.update(draws[t]);}
        }
        try{FamilyRegistryR26.build("F23");throw new AssertionError("missing original teacher silently substituted");}catch(IllegalArgumentException expected){}
        Map<String,String> fixture=new TreeMap<>();fixture.put("abi","1");fixture.put("teacher",PolicyIdentityR26.sha256("SYNTHETIC_TEACHER_ONLY"));fixture.put("state",PolicyIdentityR26.sha256("SYNTHETIC_STATE_ONLY"));fixture.put("scope","SYNTHETIC_TEST_ONLY");
        FamilyRegistryR26.bindLegacyTeacher(fixture,params->new DistillationFamilyR26(new DistillationFamilyR26.Teacher(){public String identity(){return "SYNTHETIC_TEST_ONLY";}public double[][] marginal(){double[][] p=new double[10][10];for(double[] row:p)Arrays.fill(row,.1);return p;}}));
        OnlineModelsSU2.Model distilled=FamilyRegistryR26.build("F23");double initial=loss(distilled,fixed);for(int i=0;i<220;i++)distilled.update(fixed);check(loss(distilled,fixed)<initial-.01,"distillation did not learn");byte[] state=save(distilled);OnlineModelsSU2.Model restored=FamilyRegistryR26.build("F23");restored.read(new DataInputStream(new ByteArrayInputStream(state)));distilled.update(fixed);restored.update(fixed);check(Arrays.equals(save(distilled),save(restored)),"distillation resume");check(FamilyRegistryR26.EXECUTABLE.size()==24,"family denominator");
        System.out.println("R26_FAMILIES_PASS");
    }
}
