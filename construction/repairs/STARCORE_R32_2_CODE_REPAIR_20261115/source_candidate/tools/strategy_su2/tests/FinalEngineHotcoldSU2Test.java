package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/** Required follow-up to the plan's failed intermediate R15 HOTCOLD comparison; no post-hoc threshold changes. */
public final class FinalEngineHotcoldSU2Test {
    static int[] forward(int[][] draws, boolean finalEngine) {
        ModelReplaySU2.Library lib = new ModelReplaySU2.Library(draws, 640L << 20);
        ModelEvolutionSU2 evo = new ModelEvolutionSU2(lib, 3, 1500, 2500, 250, true, true, finalEngine, finalEngine);
        ModelEvolutionSU2.State state = new ModelEvolutionSU2.State(); int[] total = new int[6];
        for (int checkpoint : new int[]{6000,8000}) {
            state = evo.campaign(state, new ModelEvolutionSU2.Eras(1500,checkpoint-1500,checkpoint-1500,checkpoint),8,24,12);
            int tier=0;
            for (String name : ModelEvolutionSU2.TIERS) {
                ModelReplaySU2.Result r = CommitteeSU2.evaluate(state.committees.get(name),lib,1500);
                int n=r.actions(checkpoint,checkpoint+2000),k=r.hits(checkpoint,checkpoint+2000);
                total[2*tier]+=n;total[2*tier+1]+=k;tier++;
                System.out.println((finalEngine?"R16":"R14b")+" HOTCOLD checkpoint="+checkpoint+" tier="+name+" hits="+k+" acts="+n);
            }
        }
        return total;
    }
    public static void main(String[] args) throws Exception {
        int[][] draws=ModelEvolutionR14Test.load(Paths.get(args[0],"HOTCOLD_seed1.txt"));
        int[] current=forward(draws,true),old=forward(draws,false);
        double now=current[1]/(double)current[0],before=old[1]/(double)old[0];
        boolean ok=now>=before-.01;
        System.out.printf(Locale.ROOT,"HOTCOLD F15 R16 %.6f (%d/%d), R14b %.6f (%d/%d), fixed margin -0.01: %s%n",now,current[1],current[0],before,old[1],old[0],ok?"PASS":"FAIL");
        System.out.println(ok?"FINAL_HOTCOLD_PASS":"FINAL_HOTCOLD_FAIL");
        if(!ok)System.exit(1);
    }
}
