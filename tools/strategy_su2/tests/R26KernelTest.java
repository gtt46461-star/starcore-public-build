package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Independent tiny brute force, gradient finite differences, precision and resume regressions. */
public final class R26KernelTest {
    static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
    static void near(double a,double b,double eps,String why){check(Math.abs(a-b)<eps,why+":"+a+"/"+b);}
    static void perm(double[][] s,int p,int mask,int[] draw,double[] z,double[][] mass){if(p==s.length){double e=0;for(int i=0;i<s.length;i++)e+=s[i][draw[i]];double w=Math.exp(e);z[0]+=w;for(int i=0;i<s.length;i++)mass[i][draw[i]]+=w;return;}for(int j=0;j<s.length;j++)if((mask&(1<<j))==0){draw[p]=j;perm(s,p+1,mask|1<<j,draw,z,mass);}}
    public static void main(String[] args)throws Exception{
        Object[] values={null,true,1,1.0,-0.0,0.0,1.23441,1.23449,new TreeMap<String,Object>(){{put("中",1);put("a",2);}},new TreeMap<String,Object>(){{put("\uD800\uDC00",1);put("\uE000",2);}},Arrays.asList(1,"1",false)};
        for(Object v:values)System.out.println("GOLDEN\t"+PolicyIdentityR26.canonical(v)+"\t"+PolicyIdentityR26.sha256(PolicyIdentityR26.canonical(v)));
        try{PolicyIdentityR26.canonical(Double.NaN);throw new AssertionError("NaN accepted");}catch(IllegalArgumentException good){}
        try{PolicyIdentityR26.canonical("\uD800");throw new AssertionError("surrogate accepted");}catch(IllegalArgumentException good){}
        Map<String,String> source=new TreeMap<>();source.put("lr","0.02");ModelGenomeSU2.Member m=new ModelGenomeSU2.Member("MATCHING",source,1.23441);source.put("lr","0.03");check(m.params.get("lr").equals("0.02"),"caller mutation");
        try{m.params.put("lr","0.04");throw new AssertionError("public mutable map");}catch(UnsupportedOperationException good){}
        ModelGenomeSU2 g=new ModelGenomeSU2(Collections.singletonList(m),"STATIC",false,"META",.15,1.23441);
        ModelGenomeSU2 g2=new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MATCHING",m.params,1.23449)),"STATIC",false,"META",.15,1.23449);
        check(!g.key().equals(g2.key()),"lossy collision");check(ModelGenomeSU2.parse(g.text()).key().equals(g.key()),"roundtrip identity");
        List<StructuralProposalR26.Proposal> same=StructuralProposalR26.from(g,m);check(same.size()==1&&same.get(0).category.equals("NO_OP"),"duplicate mechanism counted as structure");
        ModelGenomeSU2.Member extra=new ModelGenomeSU2.Member("MSDIR",ModelEvolutionSU2.centralParams("MSDIR"),1);
        List<StructuralProposalR26.Proposal> changed=StructuralProposalR26.from(g,extra);check(changed.size()==2&&changed.get(1).category.equals("STRUCTURAL_CANDIDATE"),"real member graph not distinguished");check(!changed.get(1).genome.key().equals(g.key()),"structural child same policy");
        ModelGenomeSU2 massPrecision=new ModelGenomeSU2(Collections.singletonList(m),"STATIC",false,"MASS",.15,.123456789);
        check(ModelGenomeSU2.parse(massPrecision.text()).key().equals(massPrecision.key()),"inactive META parameter lost during MASS serialization");
        try{new ModelGenomeSU2.Member("MATCHING",Collections.singletonMap("lr","0.02,other=1"),1);throw new AssertionError("parameter boundary impersonation accepted");}catch(IllegalArgumentException expected){}
        List<ModelGenomeSU2.Member> longMembers=new ArrayList<>();for(int i=0;i<4;i++){Map<String,String> params=new TreeMap<>();params.put("lr","0.02");params.put("metadata",String.join("",Collections.nCopies(600,"a")));longMembers.add(new ModelGenomeSU2.Member("MATCHING",params,1));}ModelGenomeSU2 longProgram=new ModelGenomeSU2(longMembers,"STATIC",false,"MASS",.15,.2);check(longProgram.text().length()>2048&&ModelGenomeSU2.parse(longProgram.text()).key().equals(longProgram.key()),"birth-bound multi-member program cannot use the 8192-byte storage contract");
        String legacy="SU2|POOL=STATIC|SINK=0|GATE=MASS|COV=0.15|M=HMM{K=3,lr=0.01,seed=0,stay=0.995,temper=0.6}*1";check(ModelGenomeSU2.parse(legacy).text().equals(legacy),"legacy preservation");
        double[][] s={{.2,-.1,.8},{.7,.5,-.8},{-.4,.2,.1}};double[] z={0};double[][] mass=new double[3][3];perm(s,0,0,new int[3],z,mass);ExactPermutationR26.Result r=ExactPermutationR26.infer(s,null);near(r.logZ,Math.log(z[0]),1e-12,"brute logZ");
        for(int i=0;i<3;i++)for(int j=0;j<3;j++){near(r.marginal[i][j],mass[i][j]/z[0],1e-12,"brute marg");double eps=1e-5;s[i][j]+=eps;double plus=ExactPermutationR26.infer(s,null).logZ;s[i][j]-=2*eps;double minus=ExactPermutationR26.infer(s,null).logZ;s[i][j]+=eps;near((plus-minus)/(2*eps),r.marginal[i][j],1e-8,"NLL gradient");}
        ExactPermutationR26.Result uniform=ExactPermutationR26.infer(new double[10][10],null);for(int i=0;i<10;i++){double row=0,col=0;for(int j=0;j<10;j++){row+=uniform.marginal[i][j];col+=uniform.marginal[j][i];}near(row,1,1e-12,"row");near(col,1,1e-12,"col");}near(uniform.k3(0,0,1,2),.3,1e-12,"K3 union");
        PermutationModelR26 model=new PermutationModelR26(.02);int[] draw={1,2,3,4,5,6,7,8,9,10};float[][] pred=new float[10][10];float[] st=new float[3];model.predict(pred,st);double initial=pred[0][0];for(int i=0;i<60;i++)model.update(draw);model.predict(pred,st);check(pred[0][0]>initial+.02,"planted mechanism not learned");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();model.write(new DataOutputStream(bytes));PermutationModelR26 restored=new PermutationModelR26(.02);restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));model.update(draw);restored.update(draw);float[][] p2=new float[10][10];model.predict(pred,st);restored.predict(p2,new float[3]);for(int i=0;i<10;i++)check(Arrays.equals(pred[i],p2[i]),"resume next step");
        int n=1000;boolean[] act=new boolean[n],hit=new boolean[n],incHit=new boolean[n];Arrays.fill(act,true);for(int i=0;i<n;i++){hit[i]=i%20<8;incHit[i]=i%20<9;}SelectiveEvidenceSU2.Verdict v=SelectiveEvidenceSU2.evaluate(act,hit,null,act,incHit,0,n,1,.05,250,.28,.15);check(!v.pass&&v.pairedLower<-.01,"40 vs45 regression");check(v.pairedUpper>0,"counterexample must expose old UCB defect");
        Arrays.fill(hit,true);v=SelectiveEvidenceSU2.evaluate(act,hit,null,null,null,0,n,1,.05,250,.28,.15);check(v.pass,"qualified synthetic path unreachable");boolean[] missing=new boolean[n];missing[3]=true;check(!SelectiveEvidenceSU2.evaluate(act,hit,missing,null,null,0,n,1,.05,250,.28,.15).pass,"missing integrity");
        check(QualificationWindowR26.completed(100,4099)==null,"qualification used unavailable future endpoint");
        QualificationWindowR26.Window w1=QualificationWindowR26.completed(100,4100),w2=QualificationWindowR26.completed(100,4820),w4=QualificationWindowR26.completed(100,6260);
        check(w1.look==1&&w1.from==100&&w1.to==4100,"first fixed window");
        check(w2.look==2&&w2.from==820&&w2.to==4820,"fixed renewal window");
        check(w4.look==4&&w4.from==2260,"failed or missed looks recycled alpha");
        near(SelectiveEvidenceSU2.alphaForLook(.01,w4.look),.0005,1e-15,"predeclared alpha index");
        System.out.println("R26_KERNEL_PASS");
    }
}
