package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class R30ConfirmationQueueTest {
    static EvolutionLoopR27.Member member(int n) {
        EvolutionLoopR27.Member m=new EvolutionLoopR27.Member();
        m.spec=new PolicySpecR27(PolicyAstR27.leaf("freq",n%10+1,32+(n/10)*8),n%10+1,
                PolicySpecR27.Rule.ALWAYS,0,0,1,false,.01,.01,IdeaFamilyR27.meta("I01","TEST",0,30,"queue fixture"));
        m.tau=1;m.u=100-n;return m;
    }
    @Test public void queueOwnsSlotsAndDuplicateBaseCannotReenterExploration() {
        List<EvolutionLoopR27.Member> ranked=new ArrayList<>();for(int i=0;i<60;i++)ranked.add(member(i));
        Map<String,String> confirmation=new LinkedHashMap<>();for(int i=0;i<4;i++)confirmation.put("future-"+i,ranked.get(i).hash());
        Map<String,EvolutionLoopR27.Member> found=RuntimeR27.explorationR30(ranked,confirmation);
        assertEquals(20,found.size());for(EvolutionLoopR27.Member m:found.values())assertFalse(confirmation.containsValue(m.hash()));
        assertEquals(RuntimeR27.observerId(ranked.get(4).hash()),found.keySet().iterator().next());
    }
    @Test public void existingQueueOverCapacityKeepsEveryRegistrationAndAdmitsNoExploration() {
        Map<String,String> confirmation=new LinkedHashMap<>();for(int i=0;i<32;i++)confirmation.put("future-"+i,"base-"+i);
        assertTrue(RuntimeR27.explorationR30(Arrays.asList(member(0),member(1)),confirmation).isEmpty());
        assertEquals(32,confirmation.size());
    }
}
