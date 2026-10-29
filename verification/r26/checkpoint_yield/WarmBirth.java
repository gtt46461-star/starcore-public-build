package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.nio.file.*;
import java.util.*;

/** Production warm fork and its SQL/files on the documented host transports. */
public final class WarmBirth {
    static void check(boolean condition,String why){if(!condition)throw new AssertionError(why);}
    static long value(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){check(c.moveToFirst(),"Missing scalar");return c.getLong(0);}}
    static void append(List<Models.Draw> draws,SQLiteDatabase db,long first,int offset){
        long now=System.currentTimeMillis();for(int i=0;i<128;i++){Models.Draw d=new Models.Draw();d.issue=TargetIssueResolver.fromSequenceOrdinalSU1(first+offset+i);d.numbers=new int[10];for(int p=0;p<10;p++)d.numbers[p]=(p+i%7)%10+1;d.openTime=now-(129-i)*60000L;d.fetchTime=d.openTime+500;draws.add(d);db.execSQL("INSERT INTO draws VALUES(?,0)",new Object[]{d.issue});}
    }
    public static void main(String[] args)throws Exception{
        Path directory=Files.createTempDirectory("r26-warm-birth-");
        try(SQLiteDatabase db=new SQLiteDatabase(args[0])){
            db.beginTransaction();try{RuntimeSchemaR26.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER NOT NULL)");
            long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001"),now=System.currentTimeMillis();List<Models.Draw> draws=new ArrayList<>();append(draws,db,first,0);DbStore store=new DbStore(db,new android.content.Context(directory.toFile()),draws);
            RuntimeCoordinatorR26.Claim claim=new RuntimeCoordinatorR26.Claim("warm-test","SYNTHETIC",1,0);BudgetTokenSU1 budget=new BudgetTokenSU1(30000,100000000,()->false);
            db.execSQL("INSERT INTO r26_session VALUES('SYNTHETIC',1,'SYNTHETIC','RUNNING',?,0)",new Object[]{now});db.execSQL("INSERT INTO r26_task(task_id,lane,mechanism,fence,lease_until,owner,status) VALUES('warm-test','P2','test',1,?,'SYNTHETIC','RUNNING')",new Object[]{now+600000});
            FoundryCampaignR26.Snapshot source=FoundryCampaignR26.capture(store);byte[] snapshot=source.encode();String sourceHash=LiveStateR26.hash(snapshot);ModelGenomeSU2 parent=FamilyRegistryR26.genome("F20").bindBirth(sourceHash,source.through);LiveStateR26 initial=new LiveStateR26(parent);for(int i=0;i<40;i++)initial.learn(source.draws[i]);byte[] original=initial.encode();
            db.execSQL("INSERT INTO r26_foundry VALUES('SYNTHETIC_PARENT',?,?,?,'RUNNING',?,?,?)",new Object[]{sourceHash,source.through,snapshot,now,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,source.ordinal[0]});
            db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,status,operation) VALUES('SYNTHETIC_PARENT',?,?,40,?,?,'SYNTHETIC_PREVIOUS','READY','SYNTHETIC_WARM')",new Object[]{parent.key(),parent.text(),original,LiveStateR26.hash(original)});
            check(FoundryCampaignR26.step(store,()->claim,budget)>0,"Real parent learning failed");byte[] oldReference;String parameters;
            try(Cursor c=db.rawQuery("SELECT state,state_hash FROM r26_candidate",null)){check(c.moveToFirst(),"No parent state");oldReference=c.getBlob(0);LiveStateR26 old=LiveStateR26.decode(parent.text(),CheckpointArtifactsR26.resolve(store,oldReference),c.getString(1));old.onGap();parameters=((FamilyF20KernelR26)old.hub.models.values().iterator().next()).learnedParameterHash();}
            append(draws,db,first,6000);check(FoundryCampaignR26.step(store,()->claim,budget)==0,"Expired source continued");check(value(db,"SELECT length(snapshot) FROM r26_foundry")==0,"Expired raw retained");check(value(db,"SELECT count(*) FROM r26_candidate WHERE status='WAITING_WARM_FORK'")==1,"Learned parent deleted");
            RuntimeCoordinatorR26.Claim stale=new RuntimeCoordinatorR26.Claim("warm-test","SYNTHETIC",2,0);try{WarmCandidateR26.step(store,()->stale,budget);throw new AssertionError("Stale owner transferred weights");}catch(IllegalStateException expected){}
            check(value(db,"SELECT count(*) FROM r26_warm_initialization")==0,"Stale birth left a row");check(WarmCandidateR26.step(store,()->claim,budget)==1,"Warm birth failed");check(WarmCandidateR26.step(store,()->claim,budget)==0,"Warm birth repeated");String firstInitializer;
            try(Cursor c=db.rawQuery("SELECT c.canonical_text,c.state,c.state_hash,c.cursor,i.initializer_hash,i.parent_trained_through,i.parent_exposed_through,f.source_first FROM r26_candidate c JOIN r26_warm_initialization i ON i.policy_hash=c.policy_hash JOIN r26_foundry f ON f.campaign_id=c.campaign_id",null)){
                check(c.moveToFirst()&&c.getInt(3)==0,"No fresh sample clock");LiveStateR26 child=LiveStateR26.decode(c.getString(0),CheckpointArtifactsR26.resolve(store,c.getBlob(1)),c.getString(2));check(child.samples()==0,"Inherited sample evidence");check(parameters.equals(((FamilyF20KernelR26)child.hub.models.values().iterator().next()).learnedParameterHash()),"Lost learned kernel");firstInitializer=c.getString(4);check(c.getLong(5)<=first+127&&c.getLong(6)==first+127&&c.getLong(7)>c.getLong(6),"Parent exposure leaked into child source");
            }
            check(value(db,"SELECT count(*) FROM r26_cohort")==0,"Warm birth allocated qualification");
            try{db.execSQL("UPDATE r26_warm_initialization SET initializer_hash='changed'");throw new AssertionError("Mutable initializer");}catch(IllegalStateException expected){}
            append(draws,db,first,12000);check(FoundryCampaignR26.step(store,()->claim,budget)==0,"Second expiry trained stale raw");check(value(db,"SELECT count(*) FROM r26_candidate WHERE cursor=0 AND status='WAITING_WARM_FORK'")==1,"Unserved prior discarded");check(WarmCandidateR26.step(store,()->claim,budget)==1,"Second warm birth failed");
            try(Cursor c=db.rawQuery("SELECT i.initializer_hash,i.parent_exposed_through,f.source_first FROM r26_warm_initialization i JOIN r26_candidate c ON c.policy_hash=i.policy_hash JOIN r26_foundry f ON f.campaign_id=c.campaign_id",null)){check(c.moveToFirst()&&firstInitializer.equals(c.getString(0))&&c.getLong(1)==first+6127&&c.getLong(2)>c.getLong(1),"Second continuation changed prior or source ordering");}
            db.beginTransaction();try{WarmCandidateR26.pruneInitializers(db);db.setTransactionSuccessful();}finally{db.endTransaction();}check(value(db,"SELECT count(*) FROM r26_warm_initialization")==1,"Orphan initializer remained");check(CheckpointReferencesR26.hashes(db).size()==2,"Reference closure omitted seed or initializer");
            CheckpointArtifactsR26.forStore(store).prune(()->CheckpointReferencesR26.hashes(db),System.currentTimeMillis()+120000,0,256);try{CheckpointArtifactsR26.resolve(store,oldReference);throw new AssertionError("Expired raw parent checkpoint remained pinned");}catch(java.io.IOException expected){}
            check(FoundryCampaignR26.step(store,()->claim,budget)>0,"Warm initialized child did not actually learn");
            System.out.println("R26_WARM_BIRTH_PASS|actualFoundryAndWarmSteps=true|staleFenceRejected=true|twoExpiriesIncludingUnservedSeed=true|parametersPreserved=true|sourceStrictlyAfterFullExposure=true|freshQualification=true|immutableInitializerAndGcClosure=true|expiredRawCheckpointUnpinned=true|subsequentLearning=true|scope=HOST_PRODUCTION_METHOD_SQLITE_AND_FILES_WITH_ANDROID_TRANSPORT_ADAPTERS");
        }finally{try(java.util.stream.Stream<Path> paths=Files.walk(directory)){for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}
    }
}
