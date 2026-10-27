package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;

/** Causal planted-regime mechanism checks; no natural or prospective quality claim. */
public final class R26F08MechanismTest {
    static void require(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static byte[] encode(OnlineModelsSU2.Model m)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(b)){m.write(out);}return b.toByteArray();}
    static float[][] prediction(OnlineModelsSU2.Model m){float[][] p=new float[10][10];m.predict(p,new float[m.stateWidth()]);return p;}
    static void same(float[][] a,float[][] b){for(int p=0;p<10;p++)for(int n=0;n<10;n++)require(Float.floatToIntBits(a[p][n])==Float.floatToIntBits(b[p][n]),"forecast changed");}
    static int[] draw(Random r){int[] d={1,2,3,4,5,6,7,8,9,10};for(int p=9;p>0;p--){int q=r.nextInt(p+1),x=d[p];d[p]=d[q];d[q]=x;}return d;}
    static Map<String,String> graph(int states,boolean transition){Map<String,String> p=new TreeMap<>();p.put("abi","1");p.put("K",Integer.toString(states));p.put("transition",Boolean.toString(transition));return p;}
    static double[] experiment(boolean persistent)throws Exception{
        OnlineModelsSU2.Model full=ModelReplaySU2.build("R26F08",graph(3,true)),cut=ModelReplaySU2.build("R26F08",graph(3,false));
        Random random=new Random(persistent?440081:440082);double a=0,b=0;int samples=0;
        for(int t=0;t<10000;t++){
            float[][] pa=ModelOpsSU2.sink(new float[][][]{prediction(full)},20)[0],pb=ModelOpsSU2.sink(new float[][][]{prediction(cut)},20)[0];
            int regime=persistent?(t/96)%2:random.nextInt(2);int[] d=new int[10];for(int p=0;p<10;p++)d[p]=(p+regime*5)%10+1;
            if(random.nextDouble()<.12){int p=random.nextInt(10),q=random.nextInt(10),x=d[p];d[p]=d[q];d[q]=x;}
            if(t>=6000){double x=0,y=0;for(int p=0;p<10;p++){x-=Math.log(Math.max(1e-12,pa[p][d[p]-1]));y-=Math.log(Math.max(1e-12,pb[p][d[p]-1]));}a+=x/10;b+=y/10;samples++;}
            full.update(d);cut.update(d);
        }return new double[]{a/samples,b/samples};
    }
    public static void main(String[] args)throws Exception{
        OnlineModelsSU2.Model old=ModelReplaySU2.build("HMM",ModelEvolutionSU2.centralParams("HMM")),current=FamilyRegistryR26.build("F08");Random random=new Random(926008);
        for(int t=0;t<512;t++){same(prediction(old),prediction(current));int[] d=draw(random);old.update(d);current.update(d);}
        require(Arrays.equals(encode(old),encode(current)),"default F08 lost old exact weights/codec");
        OnlineModelsSU2.Model restored=FamilyRegistryR26.build("F08");restored.read(new DataInputStream(new ByteArrayInputStream(encode(old))));same(prediction(current),prediction(restored));int[] next=draw(random);current.update(next);restored.update(next);require(Arrays.equals(encode(current),encode(restored)),"default next update changed");
        ModelGenomeSU2 parent=FamilyRegistryR26.genome("F08");List<StructuralProposalR26.Proposal> children=StructuralProposalR26.mechanismChildren(parent);
        require(children.size()==2,"missing F08 state edit/control");require("STRUCTURAL_CANDIDATE".equals(children.get(0).category)&&"MECHANISM_ABLATION".equals(children.get(1).category),"F08 operation mislabeled");
        require(!StructuralProposalR26.structure(parent).equals(StructuralProposalR26.structure(children.get(0).genome)),"state count is cosmetic");require(StructuralProposalR26.control(children.get(1).genome),"transition cut can enter live pool");
        for(StructuralProposalR26.Proposal child:children){ModelGenomeSU2.Member member=child.genome.members.get(0);OnlineModelsSU2.Model model=ModelReplaySU2.build(member.kind,member.params);for(int t=0;t<128;t++)model.update(draw(random));byte[] state=encode(model);OnlineModelsSU2.Model copy=ModelReplaySU2.build(member.kind,member.params);DataInputStream in=new DataInputStream(new ByteArrayInputStream(state));copy.read(in);require(in.read()==-1,"trailing state");same(prediction(model),prediction(copy));next=draw(random);model.update(next);copy.update(next);require(Arrays.equals(encode(model),encode(copy)),"variant next update changed");
            Map<String,String> changed=new TreeMap<>(member.params);changed.put("transition",Boolean.toString(!Boolean.parseBoolean(changed.get("transition"))));try{ModelReplaySU2.build(member.kind,changed).read(new DataInputStream(new ByteArrayInputStream(state)));throw new AssertionError("changed router accepted old state");}catch(IOException expected){require(expected.getMessage().contains("F08_CHECKPOINT_GRAPH"),"wrong graph failure");}
        }
        try{ModelReplaySU2.build("R26F08",graph(7,true));throw new AssertionError("unbounded state graph");}catch(IllegalArgumentException expected){}
        double[] planted=experiment(true),iid=experiment(false);require(planted[0]+.05<planted[1],"removing causal transition memory did not remove the planted mechanism advantage");require(iid[0]>=iid[1]-.05,"negative control reported a large spurious mechanism advantage");
        System.out.println("R26_F08_MECHANISM_PASS|heldoutNll="+planted[0]+"|noTransitionNll="+planted[1]+"|iidFull="+iid[0]+"|iidControl="+iid[1]+"|periodsScored=4000|notNaturalQuality=true");
    }
}
