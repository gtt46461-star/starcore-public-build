package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Executes the actual merged R13 kernels, including checkpoint continuation. */
public final class ProductionR13Test {
    static void check(boolean okay,String why){if(!okay)throw new AssertionError(why);}
    static int[][] draws(int n){int[][] d=new int[n][10];Random random=new Random(13024);for(int[] row:d){for(int p=0;p<10;p++)row[p]=p+1;for(int p=9;p>0;p--){int k=random.nextInt(p+1),v=row[p];row[p]=row[k];row[k]=v;}}return d;}
    public static void main(String[] ignored)throws Exception{
        int[][] d=draws(2200);List<ModelGenomeSU2> genomes=ModelEvolutionSU2.screenSeeds();
        ModelReplaySU2.Library library=new ModelReplaySU2.Library(d,96L<<20);
        ModelLiveSU2.Hub hub=new ModelLiveSU2.Hub();List<ModelLiveSU2> lives=new ArrayList<>();List<ModelReplaySU2.Result> replay=new ArrayList<>();
        for(ModelGenomeSU2 g:genomes){lives.add(new ModelLiveSU2(g,hub,1500,1500,100));replay.add(ModelReplaySU2.evaluate(g,library,1500,1500,100));check(g.key().equals(ModelGenomeSU2.parse(g.text()).key()),"genome transport");}
        for(int t=0;t<d.length;t++){
            if(t==1800){
                ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);hub.write(out);for(ModelLiveSU2 live:lives)live.write(out);out.flush();
                hub=new ModelLiveSU2.Hub();lives.clear();for(ModelGenomeSU2 g:genomes)lives.add(new ModelLiveSU2(g,hub,1500,1500,100));
                DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));hub.readInto(in);for(ModelLiveSU2 live:lives)live.readInto(in);check(in.available()==0,"checkpoint consumed");
            }
            for(int i=0;i<lives.size();i++){
                ModelLiveSU2.Decision x=lives.get(i).decide();ModelReplaySU2.Result r=replay.get(i);
                check(x.act==r.act[t]&&x.position==r.position[t]&&x.mask==r.mask[t],"live/replay at "+t+" model "+i);
                check(Integer.bitCount(x.mask)==3,"exact K3");
                check(lives.get(i).settle(d[t])==r.hit[t],"settlement parity");
            }
            hub.update(d[t]);
        }
        boolean[] act=new boolean[4000],hit=new boolean[4000],missing=new boolean[4000];
        for(int t=0;t<4000;t++){act[t]=t%4==0;hit[t]=t%8==0;missing[t]=t%2==1;}
        SelectiveEvidenceSU2.Verdict complete=SelectiveEvidenceSU2.evaluate(act,hit,null,null,null,0,4000,1,.05,600,.28,.15);
        SelectiveEvidenceSU2.Verdict partial=SelectiveEvidenceSU2.evaluate(act,hit,missing,null,null,0,4000,1,.05,600,.28,.15);
        check(complete.pass,"strong synthetic fixture");check(!partial.pass&&partial.lower<complete.lower&&partial.missing==2000,"missing predictions cannot improve certificate");
        double spent=0;for(int look=1;look<=10000;look++)spent+=SelectiveEvidenceSU2.alphaForLook(.05,look);check(spent<.05&&spent>.049,"lifetime alpha bound");
        System.out.println("R13_PRODUCTION_PASS: 24 strategies, six model families, 2200 draws, checkpoint/live/replay/K3, missing-evidence penalty and alpha bound; synthetic contracts only");
    }
}
