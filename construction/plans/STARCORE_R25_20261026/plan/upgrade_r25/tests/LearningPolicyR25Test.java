package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class LearningPolicyR25Test {
    static int checks;
    static void ok(boolean x){checks++;if(!x)throw new AssertionError();}
    static void rejects(Runnable r){checks++;try{r.run();}catch(IllegalArgumentException e){return;}throw new AssertionError("not rejected");}
    public static void main(String[] args){
        Map<String,String> m=new HashMap<>();for(String k:LearningPolicyR25.FIELDS)m.put(k,k+"_v1");
        String id=LearningPolicyR25.identity(m);ok(id.length()==64);
        Map<String,String> copy=new TreeMap<>(m);ok(id.equals(LearningPolicyR25.identity(copy)));
        copy.put("gate","v2");ok(!id.equals(LearningPolicyR25.identity(copy)));
        copy.remove("rng");rejects(()->LearningPolicyR25.identity(copy));
        double[] p=new double[10];Arrays.fill(p,.1);ok(Math.abs(LearningPolicyR25.mass(p,new int[]{1,2,3})-.3)<1e-12);
        rejects(()->LearningPolicyR25.mass(p,new int[]{1,1,2}));
        rejects(()->LearningPolicyR25.mass(p,new int[]{1,2,3,4}));
        double[] bad=p.clone();bad[0]=Double.NaN;rejects(()->LearningPolicyR25.mass(bad,new int[]{1,2,3}));
        LearningPolicyR25.validate(new LearningPolicyR25.Transition(id,"parent","child",100,101,100,1),id,"parent",0,99);checks++;
        rejects(()->LearningPolicyR25.validate(new LearningPolicyR25.Transition(id,"parent","child",101,101,101,1),id,"parent",0,99));
        rejects(()->LearningPolicyR25.validate(new LearningPolicyR25.Transition(id,"parent","child",100,101,99,1),id,"parent",0,99));
        rejects(()->LearningPolicyR25.validate(new LearningPolicyR25.Transition(id,"wrong","child",100,101,100,1),id,"parent",0,99));
        rejects(()->LearningPolicyR25.validate(new LearningPolicyR25.Transition(id,"parent","child",100,101,100,2),id,"parent",0,99));
        rejects(()->LearningPolicyR25.validate(new LearningPolicyR25.Transition("other","parent","child",100,101,100,1),id,"parent",0,99));
        rejects(()->LearningPolicyR25.validate(new LearningPolicyR25.Transition(id,"parent","child",99,101,100,1),id,"parent",0,99));
        System.out.println("LearningPolicyR25 HOST checks="+checks+" PASS; Android integration NOT_RUN");
    }
}
