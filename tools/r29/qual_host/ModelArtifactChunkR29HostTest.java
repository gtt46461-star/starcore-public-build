package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Real SQLite BLOB reads and actual numerical continuation. The host guard rejects whole-BLOB queries;
 * Android's actual CursorWindow negative control is in R29LargeModelCheckpointDeviceTest. */
public final class ModelArtifactChunkR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_MODEL_CHUNK|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static final class GuardDb extends SQLiteDatabase{
        int chunks,maxChunk;Runnable beforeSecondChunk;
        GuardDb()throws Exception{super(DriverManager.getConnection("jdbc:sqlite::memory:"));execSQL(ModelArtifactR29.DDL);}
        public Cursor rawQuery(String sql,String[] args){
            if(sql.matches("(?is).*\\b(?:SELECT|,)\\s*artifact\\b.*"))throw new IllegalStateException("HOST_GUARD_WHOLE_BLOB_QUERY");
            if(sql.startsWith("SELECT substr(artifact")){chunks++;maxChunk=Math.max(maxChunk,Integer.parseInt(args[1]));if(chunks==2&&beforeSecondChunk!=null)beforeSecondChunk.run();}
            return super.rawQuery(sql,args);
        }
    }
    static ModelArtifactR29 fixture(){
        AtomNetTrainerR27 trainer=new AtomNetTrainerR27(172,29008);trainer.onlineLr=.0001;
        for(int t=0;t<40;t++){double[] x=new double[172];for(int k=0;k<x.length;k++)x[k]=((t*31+k*17)%101)/100.0;trainer.online(new AtomNetTrainerR27.Sample(x,t%2,t%10,"WARM"+t));}trainer.checkpoint=trainer.net.encode();
        for(int t=0;t<AtomNetTrainerR27.REPLAY;t++){double[] x=new double[172];for(int k=0;k<x.length;k++)x[k]=((t*31+k*17)%101)/100.0;trainer.remember(new AtomNetTrainerR27.Sample(x,t%2,t%10,String.format(Locale.ROOT,"S%011d",t)));}
        FtrlR27 f=new FtrlR27(7,.05,1,.0001,.001);for(int i=0;i<32;i++)f.update(Arrays.copyOf(trainer.buffer.get(i).x,7),i%2);
        return new ModelArtifactR29(trainer,f,StatsR27.sha256("synthetic full replay fixture"),"202610300100");
    }
    static void store(GuardDb db,ModelArtifactR29 state){byte[] bytes=state.encode();db.execSQL("INSERT OR REPLACE INTO r29_model_checkpoint VALUES('ATOMNET_FTRL',?,?,?,?,?,1)",new Object[]{ModelArtifactR29.ABI,state.datasetHash,state.trainedThrough,StatsR27.sha256(bytes),bytes});}
    static void chunkReads()throws Exception{
        ModelArtifactR29 original=fixture();byte[] full=original.encode();
        check("C1_REAL_FULL_REPLAY_ARTIFACT_EXCEEDS_TWO_MIB",full.length>2<<20);
        try(GuardDb db=new GuardDb()){
            store(db,original);ModelArtifactR29.Stored restored=ModelArtifactR29.restore(db,"ATOMNET_FTRL");
            check("C2_ALL_BYTES_AND_2000_REPLAY_SAMPLES_RESTORE",restored.byteLength==full.length&&restored.artifact.trainer.buffer.size()==2000&&Arrays.equals(full,restored.artifact.encode()));
            check("C3_EVERY_SQL_BLOB_ROW_IS_AT_MOST_128_KIB",db.chunks>16&&db.maxChunk<=128<<10);
            AtomNetTrainerR27.Sample next=original.trainer.buffer.get(0);original.trainer.online(next);original.ftrl.update(Arrays.copyOf(next.x,7),next.y);restored.artifact.trainer.online(next);restored.artifact.ftrl.update(Arrays.copyOf(next.x,7),next.y);
            check("C4_NEXT_OPTIMIZER_RNG_REPLAY_UPDATE_REMAINS_BITWISE_EQUAL",Arrays.equals(original.encode(),restored.artifact.encode()));
            byte[] corrupt=full.clone();corrupt[corrupt.length/2]^=1;db.execSQL("UPDATE r29_model_checkpoint SET artifact=?",new Object[]{corrupt});boolean blocked=false;
            try{ModelArtifactR29.restore(db,"ATOMNET_FTRL");}catch(IllegalArgumentException expected){blocked=true;}
            check("C5_CHUNKING_STILL_REJECTS_A_FULL_CHECKSUM_CONFLICT",blocked);
            db.execSQL("UPDATE r29_model_checkpoint SET artifact=?",new Object[]{full});db.chunks=0;db.beforeSecondChunk=()->db.execSQL("UPDATE r29_model_checkpoint SET trained_through='202610300101'");blocked=false;
            try{ModelArtifactR29.restore(db,"ATOMNET_FTRL");}catch(IllegalStateException expected){blocked=expected.getMessage().contains("CHECKPOINT_CHANGED");}
            check("C6_CONCURRENT_METADATA_CHANGE_CANNOT_MIX_CHECKPOINT_GENERATIONS",blocked);
            db.beforeSecondChunk=null;db.execSQL("UPDATE r29_model_checkpoint SET artifact=zeroblob(67108865)");blocked=false;int before=db.chunks;
            try{ModelArtifactR29.restore(db,"ATOMNET_FTRL");}catch(IllegalArgumentException expected){blocked=true;}
            boolean oversizedBlockedBeforeRead=blocked&&db.chunks==before;
            db.execSQL("UPDATE r29_model_checkpoint SET artifact=X''");blocked=false;
            try{ModelArtifactR29.restore(db,"ATOMNET_FTRL");}catch(IllegalArgumentException expected){blocked=true;}
            check("C7_EMPTY_OR_UNBOUNDED_METADATA_IS_NOT_A_RECOVERED_MODEL",blocked&&oversizedBlockedBeforeRead);
            check("C8_ABSENT_OWNER_IS_DISTINCT_FROM_CORRUPTION",ModelArtifactR29.restore(db,"ABSENT")==null);
        }
    }
    static List<AtomNetTrainerR27.Sample> samples(int n){List<AtomNetTrainerR27.Sample> rows=new ArrayList<>();for(int t=0;t<n;t++){double[] x=new double[16];for(int k=0;k<x.length;k++)x[k]=((t*7+k*11)%37)/37.0;rows.add(new AtomNetTrainerR27.Sample(x,t%2,t%10,"S"+t));}return rows;}
    static void slicedPretrain()throws Exception{
        List<AtomNetTrainerR27.Sample> train=samples(160),select=samples(80);AtomNetTrainerR27 direct=new AtomNetTrainerR27(16,29);direct.pretrain(train,select);
        String owner="HOST_ATOM_PRETRAIN_"+UUID.randomUUID();CooperativeContinuationR26<AtomNetTrainerR27> job=new CooperativeContinuationR26<>("host-real-atom-pretrain",()->{AtomNetTrainerR27 t=new AtomNetTrainerR27(16,29);t.pretrain(train,select);return t;},()->false,null,System::nanoTime,owner);
        int turns=0;CooperativeContinuationR26.Slice<AtomNetTrainerR27> slice=null;
        try{do{slice=job.slice(1);turns++;if(turns>10000)throw new AssertionError("pretrain failed to complete");}while(!slice.done);
            if(slice.error!=null)throw new AssertionError(slice.error);
            FtrlR27 f1=new FtrlR27(7,.05,1,.0001,.001),f2=new FtrlR27(7,.05,1,.0001,.001);
            check("C9_SLICED_PRETRAIN_MATCHES_ALL_UNSLICED_WEIGHTS_ADAM_RNG_AND_REPLAY",turns>1&&Arrays.equals(new ModelArtifactR29(direct,f1,"dataset","through").encode(),new ModelArtifactR29(slice.result,f2,"dataset","through").encode()));
        }finally{job.close();}
        check("C10_COMPLETED_PRETRAIN_OWNER_DRAINS",CooperativeContinuationR26.awaitOwnerStopped(owner,1000));
        String stopOwner=owner+"STOP";AtomicLong visits=new AtomicLong();CooperativeContinuationR26<Void> stopped=new CooperativeContinuationR26<>("host-pretrain-close",()->{for(;;){CooperativeContinuationR26.poll();visits.incrementAndGet();}},()->false,null,System::nanoTime,stopOwner);
        stopped.slice(1);stopped.close();boolean drained=CooperativeContinuationR26.awaitOwnerStopped(stopOwner,1000);long count=visits.get();Thread.sleep(10);
        check("C11_CANCELLED_PURE_COMPUTE_OWNER_CANNOT_OUTLIVE_CLOSE",drained&&visits.get()==count);
    }
    public static void main(String[]args)throws Exception{chunkReads();slicedPretrain();System.out.println("R29_MODEL_CHUNK|ALL_PASS|checks="+checks+"|HOST_SQL_AND_NUMERICAL_ONLY_ANDROID_WINDOW_TEST_SEPARATE");}
}
