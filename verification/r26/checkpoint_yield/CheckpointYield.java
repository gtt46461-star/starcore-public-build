package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Executes the production Foundry step, codec, file owner, fence and SQL. */
public final class CheckpointYield {
    static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    public static void main(String[] args)throws Exception{
        AtomicBoolean yielded=new AtomicBoolean();
        ModelReplaySU2.ADAPTERS.put("R26F18",parameters->new OnlineModelsSU2.Model(){
            final FamilyF18KernelR26 delegate=new FamilyF18KernelR26();int updates;
            public int stateWidth(){return delegate.stateWidth();}public void predict(float[][] p,float[] state){delegate.predict(p,state);}
            public void update(int[] draw){delegate.update(draw);updates++;}
            public void write(DataOutput out)throws IOException{delegate.write(out);if(updates==42&&yielded.compareAndSet(false,true))throw new BudgetTokenSU1.Yield("SYNTHETIC_SERIALIZATION_PREEMPTION");out.writeInt(updates);}
            public void read(DataInput in)throws IOException{delegate.read(in);updates=in.readInt();}
        });
        Path directory=Files.createTempDirectory("r26-checkpoint-yield-");
        try(SQLiteDatabase db=new SQLiteDatabase(args[0])){
            db.beginTransaction();try{RuntimeSchemaR26.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001"),now=System.currentTimeMillis();List<Models.Draw> draws=new ArrayList<>();
            for(int i=0;i<128;i++){Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+i);d.numbers=new int[10];for(int p=0;p<10;p++)d.numbers[p]=(p+i%7)%10+1;d.openTime=now-(129-i)*60000L;d.fetchTime=d.openTime+500;draws.add(d);}
            DbStore store=new DbStore(db,new android.content.Context(directory.toFile()),draws);FoundryCampaignR26.Snapshot source=FoundryCampaignR26.capture(store);byte[] snapshot=source.encode();String sourceHash=LiveStateR26.hash(snapshot),campaign="SYNTHETIC_YIELD";
            db.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC',1,'SYNTHETIC','RUNNING',?,0)",new Object[]{now});
            db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('yield-test','P2','test',1,?,'SYNTHETIC','RUNNING')",new Object[]{now+600000});
            db.execSQL("INSERT INTO r26_foundry VALUES(?,?,?,?,'RUNNING',?,?,?)",new Object[]{campaign,sourceHash,source.through,snapshot,now,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,source.ordinal[0]});
            ModelGenomeSU2 genome=FamilyRegistryR26.genome("F18").bindBirth(sourceHash,source.through);LiveStateR26 expected=new LiveStateR26(genome);for(int i=0;i<40;i++)expected.learn(source.draws[i]);byte[] original=expected.encode();
            db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,status,operation) VALUES(?,?,?,40,?,?,'SYNTHETIC_PARENT','READY','SYNTHETIC_YIELD')",new Object[]{campaign,genome.key(),genome.text(),original,LiveStateR26.hash(original)});
            ModelLiveSU2.Decision decision=expected.forecast();float[][] table=expected.table();double loss=0;for(int p=0;p<10;p++)loss-=Math.log(Math.max(1e-12,table[p][source.draws[40][p]-1]))/10;expected.learn(source.draws[40]);byte[] complete=expected.encode();
            RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("yield-test","SYNTHETIC",1,0);BudgetTokenSU1 budget=new BudgetTokenSU1(30000,100000000,()->false);
            check(FoundryCampaignR26.step(store,()->claim,budget)==1,"Failed sample was counted as progress");check(yielded.get(),"Serialization yield was not injected");
            try(Cursor c=db.rawQuery("SELECT cursor,state,state_hash,loss_sum,dev_n,dev_k,checkpoint_hash FROM r26_candidate",null)){
                check(c.moveToFirst()&&c.getInt(0)==41,"Cursor advanced beyond complete state");byte[] actual=CheckpointArtifactsR26.resolve(store,c.getBlob(1));check(Arrays.equals(complete,actual),"Serialized bytes differ from uninterrupted sample");check(LiveStateR26.decode(genome.text(),actual,c.getString(2)).samples()==41,"Restored sample clock");check(Math.abs(loss-c.getDouble(3))<1e-12,"Incomplete sample affected loss");check(c.getInt(4)==(decision.act?1:0),"Incomplete sample affected actions");
                check(c.getInt(5)==(decision.act&&(decision.mask&(1<<(source.draws[40][decision.position-1]-1)))!=0?1:0),"Incomplete sample affected hits");
                try(Cursor manifest=db.rawQuery("SELECT trained_through FROM r26_checkpoint_artifact WHERE checkpoint_hash=?",new String[]{c.getString(6)})){check(manifest.moveToFirst()&&manifest.getLong(0)==source.ordinal[40],"Manifest ran ahead of weights");}
            }
            check(FoundryCampaignR26.step(store,()->claim,budget)>0,"Continuation did not resume");try(Cursor c=db.rawQuery("SELECT cursor,state FROM r26_candidate",null)){check(c.moveToFirst()&&c.getInt(0)>41,"No next samples");for(int i=41;i<c.getInt(0);i++)expected.learn(source.draws[i]);check(Arrays.equals(expected.encode(),CheckpointArtifactsR26.resolve(store,c.getBlob(1))),"Subsequent learning differs");}
            System.out.println("R26_CHECKPOINT_YIELD_PASS|interruptedSample=42|committedCursor=41|stateMetricsManifestAligned=true|subsequentUpdatesExact=true|scope=HOST_PRODUCTION_METHOD_SQLITE_AND_FILES_WITH_ANDROID_TRANSPORT_ADAPTERS");
        }finally{ModelReplaySU2.ADAPTERS.remove("R26F18");try(java.util.stream.Stream<Path> paths=Files.walk(directory)){for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
    }
}
