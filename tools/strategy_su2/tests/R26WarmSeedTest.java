package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;

/** A new child gets a verified past-only kernel prior, not its parent's evidence or raw ring. */
public final class R26WarmSeedTest {
    static void require(boolean value,String message){if(!value)throw new AssertionError(message);}
    static byte[] kernel(OnlineModelsSU2.Model model)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));return b.toByteArray();}
    static int[] draw(int t){int[] d=new int[10];for(int p=0;p<10;p++)d[p]=(p+t%3)%10+1;return d;}
    static void reject(Runnable work,String code){try{work.run();throw new AssertionError("Accepted "+code);}catch(IllegalArgumentException expected){require(expected.getMessage().contains(code),expected.toString());}}
    static void exercise(ModelGenomeSU2 genome)throws Exception{
        String source=PolicyIdentityR26.sha256("OLD_SOURCE"),next=PolicyIdentityR26.sha256("NEW_SOURCE");LiveStateR26 parent=new LiveStateR26(genome.bindBirth(source,1000));
        for(int t=0;t<48;t++)parent.learn(draw(t));byte[] before=parent.encode();String hash=LiveStateR26.hash(before),checkpoint=PolicyIdentityR26.sha256("OLD_CP:"+parent.genome.key());
        WarmModelSeedR26.Seed seed=WarmModelSeedR26.prepare(parent.genome.text(),before,hash,checkpoint,1000,next,2000,2127);
        require(Arrays.equals(before,parent.encode()),"Initializer changed its durable parent");require(!parent.genome.key().equals(seed.genome.key()),"New policy inherited the old identity");require(ModelGenomeSU2.parse(seed.genome.text()).key().equals(seed.genome.key()),"Warm canonical identity did not round-trip");
        require(WarmModelSeedR26.has(seed.genome),"Warm initialization was omitted from identity");
        LiveStateR26 expected=LiveStateR26.decode(parent.genome.text(),before,hash);expected.onGap();LiveStateR26 child=WarmModelSeedR26.restore(seed.genome,seed.initializer());
        require(child.samples()==0&&child.live.t==0&&!child.live.thrValid&&child.live.wt==null,"Inherited gate/sample clock");for(boolean label:child.live.lab)require(!label,"Inherited applicability outcomes");
        for(int member=0;member<genome.members.size();member++){
            OnlineModelsSU2.Model old=expected.hub.models.get(expected.genome.members.get(member).modelKey()),current=child.hub.models.get(child.genome.members.get(member).modelKey());require(Arrays.equals(kernel(old),kernel(current)),"Kernel parameters/optimizer/context reset drifted: "+genome.members.get(member).kind);
        }
        LiveStateR26 restored=LiveStateR26.decode(seed.genome.text(),seed.state(),LiveStateR26.hash(seed.state()));require(Arrays.equals(child.encode(),restored.encode()),"Complete seeded checkpoint differs from initializer path");
        for(int t=0;t<12;t++){child.learn(draw(t+80));restored.learn(draw(t+80));}require(Arrays.equals(child.encode(),restored.encode()),"Next updates lost initialized state");
        reject(()->new LiveStateR26(seed.genome),"WARM_INITIALIZER_REQUIRED");reject(()->new ModelLiveSU2(seed.genome,new ModelLiveSU2.Hub(),32,128,16),"WARM_INITIALIZER_REQUIRED");
        reject(()->new ModelReplaySU2.Library(new int[][]{draw(0)},1<<20).bind(seed.genome),"WARM_INITIALIZER_REQUIRED_FOR_REPLAY");
        ModelGenomeSU2.Member member=seed.genome.members.get(0);reject(()->ModelReplaySU2.compute(new int[][]{draw(0)},member.kind,member.params),"WARM_INITIALIZER_REQUIRED_FOR_REPLAY");
        require(StructuralProposalR26.mechanismChildren(seed.genome).isEmpty()&&StructuralProposalR26.neuralChildren(seed.genome).isEmpty()&&StructuralProposalR26.from(seed.genome,member).isEmpty(),"Cold mutation retained an unfulfilled warm-state claim");
        byte[] changed=seed.initializer();changed[changed.length-1]^=1;try{WarmModelSeedR26.restore(seed.genome,changed);throw new AssertionError("Corrupt initializer accepted");}catch(IOException expectedFailure){require(expectedFailure.getMessage().contains("INITIALIZER_HASH"),expectedFailure.toString());}
        require(LiveStateR26.hash(seed.initializer()).equals(seed.initializerHash),"Initializer accessor exposed mutable state");
        try{WarmModelSeedR26.prepare(parent.genome.text(),before,hash,checkpoint,2000,next,2000,2127);throw new AssertionError("Initializer knew a future bootstrap label");}catch(IllegalArgumentException expectedFailure){require(expectedFailure.getMessage().contains("SOURCE_NOT_STRICTLY_AFTER_PARENT"),expectedFailure.toString());}
        try{WarmModelSeedR26.prepare(parent.genome.text(),before,hash,checkpoint,900,next,950,1127);throw new AssertionError("Parent architecture had seen part of the new bootstrap");}catch(IllegalArgumentException expectedFailure){require(expectedFailure.getMessage().contains("SOURCE_NOT_AFTER_PARENT_EXPOSURE"),expectedFailure.toString());}
        // A seeded child that got no new CPU service still owns learned parameters.
        WarmModelSeedR26.Seed another=WarmModelSeedR26.prepare(seed.genome.text(),seed.state(),LiveStateR26.hash(seed.state()),PolicyIdentityR26.sha256("SEED_CP"),1000,PolicyIdentityR26.sha256("LATER_SOURCE"),4000,4127);
        require(WarmModelSeedR26.restore(another.genome,another.initializer()).samples()==0,"Unserved learned seed cannot be carried");
    }
    public static void main(String[] args)throws Exception{
        Map<String,String> teacher=new TreeMap<>();teacher.put("abi","1");teacher.put("teacher",PolicyIdentityR26.sha256("SYNTHETIC_WARM_TEACHER"));teacher.put("state",PolicyIdentityR26.sha256("SYNTHETIC_WARM_STATE"));teacher.put("scope","SYNTHETIC_ONLY");FamilyRegistryR26.bindLegacyTeacher(teacher,parameters->new DistillationFamilyR26(new DistillationFamilyR26.Teacher(){public String identity(){return "SYNTHETIC_WARM_TEACHER";}public double[][] marginal(){double[][] p=new double[10][10];for(double[] row:p)Arrays.fill(row,.1);return p;}}));
        int families=0;for(String family:FamilyRegistryR26.EXECUTABLE){exercise(FamilyRegistryR26.genome(family));families++;}
        ModelGenomeSU2 a=FamilyRegistryR26.genome("F01"),b=FamilyRegistryR26.genome("F03");exercise(new ModelGenomeSU2(Arrays.asList(a.members.get(0),b.members.get(0)),"GATED",true,"MASS",.15,.2,true));
        ModelGenomeSU2 f20=FamilyRegistryR26.genome("F20");LiveStateR26 parent=new LiveStateR26(f20.bindBirth(PolicyIdentityR26.sha256("F20_PARENT"),100));for(int t=0;t<32;t++)parent.learn(draw(t));byte[] state=parent.encode();WarmModelSeedR26.Seed seed=WarmModelSeedR26.prepare(parent.genome.text(),state,LiveStateR26.hash(state),PolicyIdentityR26.sha256("F20_CP"),100,PolicyIdentityR26.sha256("F20_CHILD"),200,327);
        Map<String,String> wrong=new TreeMap<>(seed.genome.members.get(0).params);wrong.put("slots","24");ModelGenomeSU2 altered=new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("R26F20",wrong,1)),"STATIC",true,"MASS",.15,.2,false);
        try{WarmModelSeedR26.restore(altered,seed.initializer());throw new AssertionError("Initializer silently adapted to another graph");}catch(IOException expected){require(expected.getMessage().contains("INITIALIZER_MEMBER"),expected.toString());}
        System.out.println("R26_WARM_SEED_PASS|families="+families+"|compositeOptimizerAndCalibratorReset=true|oldRawContextCleared=true|strictlyLaterBootstrap=true|newIdentityNoEvidenceInheritance=true|corruptAndMissingInitializerRejected=true|scope=SYNTHETIC_PURE_MODEL_ONLY");
    }
}
