package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/** Executes the deployed Java source, never the copied reference implementation. */
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public final class AtomKernelDeviceSU1Test {
    private static int checks;
    private static void check(boolean ok,String reason){checks++;if(!ok)throw new AssertionError(reason);}
    private static void rejects(Runnable run){try{run.run();}catch(IllegalArgumentException expected){checks++;return;}throw new AssertionError("accepted invalid input");}
    @org.junit.Test public void atomGoldenAndCausalReplayOnAndroid()throws Exception{
        checks=0;List<String> lines=new ArrayList<>();try(java.io.BufferedReader reader=new java.io.BufferedReader(new java.io.InputStreamReader(androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("su1_atom_parity.txt"),java.nio.charset.StandardCharsets.UTF_8))){String line;while((line=reader.readLine())!=null)lines.add(line);}int n=Integer.parseInt(lines.get(0));int[][] d=new int[n][10];
        for(int t=0;t<n;t++){String[] row=lines.get(t+1).split(" ");for(int p=0;p<10;p++)d[t][p]=Integer.parseInt(row[p]);}
        double worst=0;int values=0,conditions=0;
        BudgetTokenSU1 budget=new BudgetTokenSU1(600000,100000000,null);
        try(AtomLiftSU1 atoms=new AtomLiftSU1(d)){
            for(int i=n+1;i<lines.size();i++){
                String[] f=lines.get(i).split(" ");
                if(f[0].equals("S")){
                    double[][] v=atoms.score(f[1],f[2],Integer.parseInt(f[3]),budget);
                    for(int k=0;k<100;k++){double expected=Double.parseDouble(f[k+4]);double error=Math.abs(v[k/10][k%10]-expected)/(1+Math.abs(expected));worst=Math.max(worst,error);check(error<=1e-4,"atom parity "+f[1]+" "+f[2]+" t="+f[3]+" k="+k+" err="+error);values++;}
                }else{boolean actual=atoms.condition(f[1],Integer.parseInt(f[2]),f[3],Integer.parseInt(f[4]),budget);check(actual==f[5].equals("1"),"condition parity "+lines.get(i));conditions++;}
            }
        }
        check(values==84000&&conditions==750,"catalog coverage");
        for(String bad:new String[]{"XT(10)","NUMIS(-1)","REL(SAME,0,0,1)","ODD(0,1)","IF(EXPERT(1),EXPERT(2),EXPERT(3))","EXPERT(0,1)","IF(ODD(11,1),XT(0),XT(1))","ADD(EXPERT(1),BIG(0,1))","ATOM(FREQUENCY,window=999999)"})rejects(()->ProgramTreeSU1.parse(bad));
        String zero=HashingSU1.text("zero"),one=HashingSU1.text("one");
        StrategyIdentitySU1 a=new StrategyIdentitySU1(zero,zero,zero,zero,zero,zero,zero),b=new StrategyIdentitySU1(zero,zero,zero,one,zero,zero,zero);check(!a.hash.equals(b.hash),"update semantics changed identity");
        int[][] small=new int[20][];for(int i=0;i<20;i++)small[i]=d[i].clone();long[] issues=new long[20],available=new long[20];for(int i=0;i<20;i++){issues[i]=i+1;available[i]=i+1;}
        EventFrameSU1 frame=new EventFrameSU1(21,100,30,"TEST",issues,available,small);int first=frame.history()[0][0];small[0][0]=99;check(frame.history()[0][0]==first,"defensive frame");small[0]=d[1].clone();
        available[0]=31;rejects(()->new EventFrameSU1(21,100,30,"TEST",issues,available,small));available[0]=1;issues[0]=21;rejects(()->new EventFrameSU1(21,100,30,"TEST",issues,available,small));
        check(EraManagerSU1.plan(10000,0).sealedFirst==7876,"era length");check(!EraManagerSU1.plan(11000,10000).hasSealed(),"not enough unopened data");
        BudgetTokenSU1 cancelled=new BudgetTokenSU1(1000,10,()->true);try{cancelled.checkpointOrYield();throw new AssertionError("preemption ignored");}catch(BudgetTokenSU1.Yield expected){checks++;}
        check(Math.abs(HistoricalReplayEngineSU1.quantileLinear(new double[]{0,10,20,30},0,4,.25)-7.5)<1e-12,"linear quantile");
        int[][] causal=Arrays.copyOf(d,1100);HistoricalReplayEngineSU1.Replay r=HistoricalReplayEngineSU1.run(causal,"XT(3)",.25,false,800,1000,new BudgetTokenSU1(60000,1000000,null));
        int[][] changed=new int[1100][];for(int i=0;i<1100;i++)changed[i]=causal[i].clone();for(int i=1000;i<1100;i++){int q=changed[i][0];changed[i][0]=changed[i][1];changed[i][1]=q;}
        HistoricalReplayEngineSU1.Replay same=HistoricalReplayEngineSU1.run(changed,"XT(3)",.25,false,800,1000,new BudgetTokenSU1(60000,1000000,null));
        for(int i=800;i<1000;i++)check(r.act[i]==same.act[i]&&r.mask[i]==same.mask[i]&&r.position[i]==same.position[i],"future replay leakage");
        System.out.println("SU1_PRODUCTION_KERNEL_PASS checks="+checks+" atom_values="+values+" atom_conditions="+conditions+" worst="+worst);
    }
}
