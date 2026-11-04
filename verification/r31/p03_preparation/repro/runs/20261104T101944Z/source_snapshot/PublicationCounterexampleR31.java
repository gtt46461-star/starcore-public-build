package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import sun.misc.Unsafe;

/** Host counterexamples against freshly compiled, unmodified production methods.
 * Unsafe skips Android runtime construction only; it does not replace any numerical,
 * codec, publication, descriptor, or model-surface method executed by this harness.
 * Files below are isolated host fixture checkpoints, not production DB commits.
 */
public final class PublicationCounterexampleR31 {
    static final String PRIOR="202611040999", ISSUE="202611041000";
    static final long PRIOR_ORDINAL=999, ISSUE_ORDINAL=1000;
    static final int INPUT=182, TRAIN=80, SELECT=40, HOLDOUT=20;

    static void require(boolean ok,String why) { if(!ok)throw new AssertionError(why); }
    static void observation(Map<String,Object> values) { System.out.println("OBSERVATION "+JsonR27.write(values)); }
    static Map<String,Object> row(String test) { Map<String,Object> m=new LinkedHashMap<>();m.put("test",test);m.put("scope","CONTROLLED_HOST_PRODUCTION_KERNEL_NOT_DEVICE_OR_NATURAL_QUALITY");return m; }
    static double[] features(int sample,int position) {
        double[] base=new double[172];
        for(int j=0;j<base.length;j++)base[j]=((sample+3L)*(j+7L)%29)/29.0;
        return AtomNetR27.positionedInput(base,position);
    }
    static AtomNetTrainerR27.Sample historical(int index) {
        require(index>=0&&index<TRAIN+SELECT,"FORMAL_HOLDOUT_OPTIMIZER_READ:"+index);
        int position=index%10+1;
        return new AtomNetTrainerR27.Sample(features(index,position),index%3==0?1:0,index%10,
                AtomNetSessionR30.eventKey(String.format(Locale.ROOT,"20261104%04d",100+index/10),position));
    }
    static AtomNetTrainerR27.Sample online(int position) {
        return new AtomNetTrainerR27.Sample(features(1000+position,position),position%3==0?1:0,position-1,
                AtomNetSessionR30.eventKey(ISSUE,position));
    }
    static AtomNetSessionR30 decode(byte[] bytes) { return AtomNetSessionR30.decode(bytes,StatsR27.sha256(bytes)); }
    static AtomNetSessionR30 read(Path file)throws IOException { return decode(Files.readAllBytes(file)); }
    static byte[] normalized(AtomNetSessionR30 s) {
        AtomNetSessionR30 c=decode(s.encode());c.trainingMs=0;c.materializeMs=0;return c.encode();
    }
    static void write(Path file,byte[] bytes)throws IOException {
        try(FileChannel channel=FileChannel.open(file,StandardOpenOption.CREATE,StandardOpenOption.TRUNCATE_EXISTING,StandardOpenOption.WRITE)) {
            ByteBuffer data=ByteBuffer.wrap(bytes);while(data.hasRemaining())channel.write(data);channel.force(true);
        }
    }
    static long adam(AtomNetSessionR30 s) { return ByteBuffer.wrap(s.trainer.net.encode()).getLong(8); }
    static void advance(AtomNetSessionR30 s,int first,int through) {
        for(int position=first;position<=through;position++)s.online(online(position),ISSUE,ISSUE_ORDINAL,position);
    }
    static void exact(AtomNetSessionR30 a,AtomNetSessionR30 b,String where) {
        require(Arrays.equals(a.trainer.net.encode(),b.trainer.net.encode()),"NN_WEIGHTS_ADAM_NOT_EXACT:"+where);
        require(a.trainer.rng.state==b.trainer.rng.state,"RNG_NOT_EXACT:"+where);
        require(a.ftrl.updates==b.ftrl.updates&&Arrays.equals(a.ftrl.z,b.ftrl.z)&&Arrays.equals(a.ftrl.n,b.ftrl.n)&&Arrays.equals(a.ftrl.w,b.ftrl.w),"FTRL_NOT_EXACT:"+where);
        require(Arrays.equals(normalized(a),normalized(b)),"FULL_NUMERICAL_CODEC_NOT_EXACT:"+where);
        for(int position=1;position<=10;position++) {
            double[] x=features(2000,position);
            AtomNetR27.Out left=a.trainer.net.forward(x,null),right=b.trainer.net.forward(x,null);
            require(Double.doubleToLongBits(left.pHit)==Double.doubleToLongBits(right.pHit)&&Arrays.equals(left.pClass,right.pClass),"NEXT_FORECAST_NOT_EXACT:"+where);
            require(Double.doubleToLongBits(a.ftrl.predict(Arrays.copyOf(x,7)))==Double.doubleToLongBits(b.ftrl.predict(Arrays.copyOf(x,7))),"NEXT_FTRL_FORECAST_NOT_EXACT:"+where);
        }
    }
    static RuntimeR27 runtime()throws Exception {
        Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        return (RuntimeR27)((Unsafe)field.get(null)).allocateInstance(RuntimeR27.class);
    }
    static ModelSurfaceProviderR30.Surface predict(ModelSurfaceProviderR30.FrozenModelRef ref,int position) {
        long now=Math.max(System.currentTimeMillis(),ref.availableAt);
        return ModelSurfaceProviderR30.predict(new ModelSurfaceProviderR30.Pit(1001,1000,position,
                StatsR27.sha256("CONTROLLED_PAST_FEATURE_FIXTURE"),features(2000,position),now,now,now,now+60000),ref);
    }
    static void prepare(Path dir)throws Exception {
        AtomNetSessionR30 seed=new AtomNetSessionR30(StatsR27.sha256("R31_P03_NUMERICAL_FIXTURE"),PRIOR,PRIOR_ORDINAL,INPUT,TRAIN,SELECT,HOLDOUT);
        byte[] initial=seed.trainer.net.encode();int units=0,maxSamples=0;
        while(!seed.ready()) { maxSamples=Math.max(maxSamples,seed.work(PublicationCounterexampleR31::historical));require(++units<1000,"PRETRAIN_DID_NOT_TERMINATE"); }
        require(maxSamples<=64,"PRETRAIN_EXCEEDS_64");require(!Arrays.equals(initial,seed.trainer.net.encode()),"NO_PRETRAIN_GRADIENT");
        require(seed.trainer.buffer.size()==TRAIN+SELECT,"TRAIN_SELECT_REPLAY_BUFFER_NOT_PRESERVED");
        write(dir.resolve("seed.session"),seed.encode());AtomNetSessionR30 golden=decode(seed.encode());advance(golden,1,10);write(dir.resolve("golden.session"),golden.encode());
        Map<String,Object> m=row("prepare_exact_kernel_fixture");m.put("input",INPUT);m.put("trainingUnits",units);m.put("maxSamplesPerWork",maxSamples);m.put("formalHoldoutRead",false);m.put("seedBytes",seed.encode().length);m.put("seedHash",StatsR27.sha256(seed.encode()));m.put("goldenHash",StatsR27.sha256(normalized(golden)));m.put("gradientChangedNetwork",true);m.put("seedAdamStep",adam(seed));m.put("goldenAdamStep",adam(golden));m.put("seedRng",seed.trainer.rng.state);m.put("goldenRng",golden.trainer.rng.state);m.put("verdict","FIXTURE_PREPARED_NOT_PRODUCT_ACCEPTANCE");observation(m);
    }
    static void checkpointAndHalt(Path dir,int stop)throws Exception {
        AtomNetSessionR30 s=read(dir.resolve("seed.session"));advance(s,1,stop);write(dir.resolve("position-"+stop+".session"),s.encode());
        Map<String,Object> m=row("write_checkpoint_then_actual_jvm_halt");m.put("position",stop);m.put("checkpointHash",StatsR27.sha256(s.encode()));m.put("completeIssue",s.completeIssue);m.put("partialIssue",s.partialIssue);m.put("nextPosition",s.nextPosition);m.put("adamStep",adam(s));m.put("onlineSteps",s.trainer.onlineSteps);m.put("expectedHaltExit",83);observation(m);System.out.flush();System.err.flush();Runtime.getRuntime().halt(83);
    }
    static void restore(Path dir,int stop)throws Exception {
        AtomNetSessionR30 resumed=read(dir.resolve("position-"+stop+".session"));AtomNetSessionR30 uninterrupted=read(dir.resolve("seed.session"));advance(uninterrupted,1,stop);
        exact(uninterrupted,resumed,"prefix="+stop);require(resumed.nextPosition==stop+1,"PERSISTED_POSITION_CURSOR_WRONG");
        advance(resumed,resumed.nextPosition,10);AtomNetSessionR30 golden=read(dir.resolve("golden.session"));exact(golden,resumed,"finished_from="+stop);
        require(resumed.trainer.onlineSteps==10&&resumed.nextPosition==1&&resumed.partialIssue.isEmpty()&&ISSUE.equals(resumed.completeIssue),"COMPLETION_CURSOR_WRONG");
        try { resumed.online(online(1),ISSUE,ISSUE_ORDINAL,1);throw new AssertionError("DUPLICATE_ISSUE_RETRAINED"); }catch(IllegalStateException expected) { require(expected.getMessage().contains("R30_POSITION_CURSOR"),"WRONG_DUPLICATE_REJECTION"); }
        Map<String,Object> m=row("restore_after_actual_jvm_halt");m.put("position",stop);m.put("weightsAdamExact",true);m.put("rngExact",true);m.put("ftrlExact",true);m.put("fullCodecExactIgnoringTimingOnly",true);m.put("allTenNextPredictionsExact",true);m.put("duplicateRejected",true);m.put("completedHash",StatsR27.sha256(normalized(resumed)));m.put("adamStep",adam(resumed));m.put("onlineSteps",resumed.trainer.onlineSteps);m.put("verdict","HOST_NUMERICAL_RECOVERY_PASS");observation(m);
    }
    static void assertNoPartialPublication(Path dir,int stop)throws Exception {
        RuntimeR27 rt=runtime();AtomNetSessionR30 complete=read(dir.resolve("seed.session"));rt.publishAtomStateR30(complete);
        RuntimeR27.AtomPublicationR30 prior=rt.atomPublicationR30;require(prior!=null&&prior.model!=null,"NO_INITIAL_COMPLETE_MODEL");
        AtomNetSessionR30 partial=decode(complete.encode());advance(partial,1,stop);rt.publishAtomStateR30(partial);RuntimeR27.AtomPublicationR30 actual=rt.atomPublicationR30;
        ModelSurfaceProviderR30.Surface before=predict(prior.model,10),after=predict(actual.model,10);
        Map<String,Object> m=row("complete_publication_must_not_advance_for_partial");m.put("position",stop);m.put("completeIssue",partial.completeIssue);m.put("partialIssue",partial.partialIssue);m.put("nextPosition",partial.nextPosition);m.put("actualReady",partial.ready());m.put("actualReportPublicationReady",partial.report().get("publicationReady"));m.put("priorPublishedThrough",prior.model.trainedThroughOrdinal);m.put("actualPublishedThrough",actual.model.trainedThroughOrdinal);m.put("priorWeightsHash",prior.model.weightsHash);m.put("actualWeightsHash",actual.model.weightsHash);m.put("priorModelHash",prior.model.modelHash);m.put("actualModelHash",actual.model.modelHash);m.put("actualSurfaceModelHash",after.modelHash);m.put("actualSurfaceThrough",after.trainedThroughOrdinal);m.put("priorProbabilityHash",before.probabilityHash);m.put("actualProbabilityHash",after.probabilityHash);m.put("surfaceAcceptedPartialModel",true);m.put("networkAdamChanged",!Arrays.equals(complete.trainer.net.encode(),partial.trainer.net.encode()));m.put("priorAdamStep",adam(complete));m.put("partialAdamStep",adam(partial));m.put("actualPublicationStateIsPartial",actual.state==partial);m.put("expectedCompleteThrough",PRIOR_ORDINAL);m.put("expectedContract","Keep the last complete publication until position 10 commits");observation(m);
        require(actual.model.trainedThroughOrdinal==PRIOR_ORDINAL&&actual.model.weightsHash.equals(prior.model.weightsHash),
                "R31_PARTIAL_PUBLICATION_VIOLATION: position="+stop+" nextPosition="+partial.nextPosition+" completeIssue="+partial.completeIssue+" partialIssue="+partial.partialIssue+" expectedPublishedThrough="+PRIOR_ORDINAL+" actualPublishedThrough="+actual.model.trainedThroughOrdinal+" partialSurfaceAccepted=true");
    }
    static void assertStableRestartIdentity(Path dir)throws Exception {
        AtomNetSessionR30 seed=read(dir.resolve("seed.session"));RuntimeR27 first=runtime();first.publishAtomStateR30(seed);ModelSurfaceProviderR30.FrozenModelRef before=first.atomPublicationR30.model;
        Thread.sleep(5);AtomNetSessionR30 restored=read(dir.resolve("seed.session"));RuntimeR27 second=runtime();second.publishAtomStateR30(restored);ModelSurfaceProviderR30.FrozenModelRef after=second.atomPublicationR30.model;
        exact(seed,restored,"same_complete_checkpoint_publication");Map<String,Object> m=row("same_complete_checkpoint_restart_identity_must_be_stable");m.put("sameCheckpointBytes",Arrays.equals(seed.encode(),restored.encode()));m.put("sameWeightsHash",before.weightsHash.equals(after.weightsHash));m.put("beforeAvailableAt",before.availableAt);m.put("afterAvailableAt",after.availableAt);m.put("beforeModelHash",before.modelHash);m.put("afterModelHash",after.modelHash);m.put("sameTrainedThrough",before.trainedThroughOrdinal==after.trainedThroughOrdinal);observation(m);
        require(before.modelHash.equals(after.modelHash),"R31_RESTART_PUBLICATION_IDENTITY_VIOLATION: exactCheckpointAndWeights=true beforeAvailableAt="+before.availableAt+" afterAvailableAt="+after.availableAt);
    }
    static void checkFailedDraftIsolation(Path dir)throws Exception {
        AtomNetSessionR30 saved=read(dir.resolve("seed.session"));byte[] committed=saved.encode();RuntimeR27 rt=runtime();rt.publishAtomStateR30(saved);String original=rt.atomPublicationR30.model.modelHash;
        AtomNetSessionR30 uncommitted=decode(committed);advance(uncommitted,1,4);
        require(!Arrays.equals(saved.trainer.net.encode(),uncommitted.trainer.net.encode()),"FAILURE_FIXTURE_HAS_NO_GRADIENT");
        require(Arrays.equals(committed,saved.encode())&&original.equals(rt.atomPublicationR30.model.modelHash),"ABANDONED_DRAFT_MUTATED_COMMITTED");
        Map<String,Object> m=row("unpublished_training_draft_does_not_mutate_saved_state");m.put("numericDraftChanged",true);m.put("savedBytesUnchanged",true);m.put("publicationUnchangedWhenNotInvoked",true);m.put("verdict","HOST_DRAFT_ISOLATION_PASS_NOT_DB_ROLLBACK_TEST");observation(m);
    }
    public static void main(String[] args)throws Exception {
        String mode=args[0];Path directory=Paths.get(args[1]);Files.createDirectories(directory);
        if(mode.equals("prepare"))prepare(directory);
        else if(mode.equals("checkpoint-halt"))checkpointAndHalt(directory,Integer.parseInt(args[2]));
        else if(mode.equals("restore"))restore(directory,Integer.parseInt(args[2]));
        else if(mode.equals("assert-no-partial-publication"))assertNoPartialPublication(directory,Integer.parseInt(args[2]));
        else if(mode.equals("assert-stable-restart-identity"))assertStableRestartIdentity(directory);
        else if(mode.equals("draft-isolation"))checkFailedDraftIsolation(directory);
        else throw new IllegalArgumentException(mode);
    }
}
