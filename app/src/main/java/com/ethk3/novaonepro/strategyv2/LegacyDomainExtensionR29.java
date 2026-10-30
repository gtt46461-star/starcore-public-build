package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/** Imports only revision 2's newly owned legacy stores. An old FULL receipt cannot prove that those
 * stores were copied, and re-importing the original domain could overwrite already advanced RP learning. */
public final class LegacyDomainExtensionR29 {
    private LegacyDomainExtensionR29(){}
    public static final int REVISION=2;
    public static final String DDL="CREATE TABLE IF NOT EXISTS r29_legacy_domain_extension(singleton INTEGER PRIMARY KEY CHECK(singleton=1),revision INTEGER NOT NULL,phase TEXT NOT NULL,route_epoch INTEGER NOT NULL,source_hash TEXT NOT NULL,completed_at INTEGER NOT NULL)";
    private static final Map<String,ReentrantLock> LOCKS=new HashMap<>();
    private static ReentrantLock lock(String path){synchronized(LOCKS){return LOCKS.computeIfAbsent(path,key->new ReentrantLock());}}

    static boolean readyWithin(SQLiteDatabase rp,long epoch){
        if(!LegacyDomainUpgradeR29.table(rp,"r29_legacy_domain_extension"))return false;
        try(Cursor c=rp.rawQuery("SELECT revision,phase,route_epoch,source_hash,completed_at FROM r29_legacy_domain_extension WHERE singleton=1",null)){
            if(!c.moveToFirst())return false;
            if(c.getInt(0)!=REVISION||!"FULL".equals(c.getString(1))||c.getLong(2)!=epoch||c.isNull(3)||!c.getString(3).matches("[a-f0-9]{64}")||c.getLong(4)<=0)
                throw new IllegalStateException("R29_LEGACY_EXTENSION_MARKER_CONFLICT");
            return true;
        }
    }
    private static void requireFull(SQLiteDatabase rp,long epoch){
        if(epoch<1||!LegacyDomainUpgradeR29.table(rp,"r29_legacy_domain_owner"))throw new IllegalStateException("R29_LEGACY_EXTENSION_REQUIRES_FULL_DOMAIN");
        try(Cursor c=rp.rawQuery("SELECT phase,route_epoch,source_hash,completed_at FROM r29_legacy_domain_owner WHERE singleton=1",null)){
            if(!c.moveToFirst()||!"FULL".equals(c.getString(0))||c.getLong(1)!=epoch||c.isNull(2)||!c.getString(2).matches("[a-f0-9]{64}")||c.getLong(3)<=0)
                throw new IllegalStateException("R29_LEGACY_EXTENSION_REQUIRES_FULL_DOMAIN");
        }
    }
    public static boolean ready(SQLiteDatabase root){
        return LegacyDomainUpgradeR29.ready(root)&&readyWithin(ResearchRouteR29.reader(root),LegacyDomainUpgradeR29.epoch(root));
    }
    /** Called at the end of an actual verified FULL copy in the same RP transaction. sourceHash is that
     * copy's real typed capture hash; a delta-only upgrade instead passes its own verified capture hash. */
    public static void includedInFull(SQLiteDatabase rp,long epoch,String sourceHash){
        if(!rp.inTransaction())throw new IllegalStateException("R29_LEGACY_EXTENSION_TRANSACTION_REQUIRED");
        requireFull(rp,epoch);
        if(sourceHash==null||!sourceHash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("R29_LEGACY_EXTENSION_SOURCE_HASH");
        if(readyWithin(rp,epoch))return;
        rp.execSQL(DDL);
        rp.execSQL("INSERT INTO r29_legacy_domain_extension VALUES(1,?,'FULL',?,?,?)",new Object[]{REVISION,epoch,sourceHash,Math.max(1,System.currentTimeMillis())});
    }
    static void mergeWithin(SQLiteDatabase rp,LegacyDomainUpgradeR29.Snapshot capture,long epoch,LegacyDomainUpgradeR29.Check check,LegacyDomainUpgradeR29.Fault fault){
        if(!rp.inTransaction())throw new IllegalStateException("R29_LEGACY_EXTENSION_TRANSACTION_REQUIRED");
        requireFull(rp,epoch);check.run();if(readyWithin(rp,epoch))return;
        for(LegacyDomainUpgradeR29.State state:capture.tables)if(!LegacyResearchOwnershipR29.newlyOwnedResearch(state.table.name))throw new IllegalStateException("R29_LEGACY_EXTENSION_SCOPE_CONFLICT:"+state.table.name);
        LegacyDomainUpgradeR29.mergeDataWithin(rp,capture,check,fault);
        LegacyLearnedInputR29.installResearch(rp);
        com.ethk3.novaonepro.LegacyLearningCommitR29.installResearch(rp);
        check.run();includedInFull(rp,epoch,capture.hash);
    }
    public static void ensure(DbStore store)throws Exception{
        SQLiteDatabase authority=LegacyResearchScopeR29.rawRealtime(store);
        SQLiteDatabase root=ResearchRouteR29.realtimeReadOnly(authority);
        if(!ResearchRouteR29.active(root)||ready(root))return;
        if(authority.inTransaction()||root.inTransaction()||ResearchRouteR29.writerHeldByCurrentThread())throw new IllegalStateException("R29_LEGACY_EXTENSION_REQUIRES_NO_WRITER_LOCK");
        if(!LegacyDomainUpgradeR29.ready(root))throw new IllegalStateException("R29_LEGACY_EXTENSION_REQUIRES_FULL_DOMAIN");
        ReentrantLock serial=lock(root.getPath());if(!serial.tryLock(200,TimeUnit.MILLISECONDS))throw new IllegalStateException("CP3_DB_BUSY_LEGACY_DOMAIN_EXTENSION");
        try{
            if(ready(root))return;
            RuntimeR27 runtime=RuntimeR27.active();if(runtime!=null&&!runtime.pool().isClosed())throw new IllegalStateException("R29_LEGACY_EXTENSION_REQUIRES_STOPPED_RESEARCH_POOL");
            try(ResearchRouteR29.Handoff handoff=ResearchRouteR29.handoff(store)){
                if(handoff==null)throw new IllegalStateException("CP3_DB_BUSY_LEGACY_EXTENSION_HANDOFF");
                String blocked=LegacyCapabilityAdapterR29.enterContinuity(store,false);if(blocked!=null)throw new IllegalStateException("CP3_DB_BUSY_LEGACY_EXTENSION_DRAIN:"+blocked);
                try{
                    LegacyDomainUpgradeR29.Check check=new LegacyDomainUpgradeR29.Check(System::nanoTime,System.nanoTime()+LegacyDomainUpgradeR29.BUDGET_MS*1_000_000L,()->Thread.currentThread().isInterrupted());
                    LegacyDomainUpgradeR29.Snapshot capture=LegacyDomainUpgradeR29.capture(root,check,LegacyDomainUpgradeR29.MAX_CAPTURE_BYTES,LegacyResearchOwnershipR29::newlyOwnedResearch,()->{});
                    long epoch=LegacyDomainUpgradeR29.epoch(root);check.run();
                    ResearchRouteR29.write(store,"R29_EXTEND_RETAINED_LEGACY_DOMAIN_2",rp->{mergeWithin(rp,capture,epoch,check,name->{});return 0;});
                }finally{LegacyCapabilityAdapterR29.leaveContinuity();}
            }
        }finally{serial.unlock();}
    }
}
