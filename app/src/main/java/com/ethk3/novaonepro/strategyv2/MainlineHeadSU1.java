package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import java.util.*;

/** One durable deployment head, with append-only renewal/fallback receipts and no authority of its own. */
public final class MainlineHeadSU1 {
    public static final String POLICY=HashingSU1.text("SU1_MAINLINE_HEAD_V1|DIRECT_CERTIFIED_CHILD_ONLY|PARENT_PRESERVED|FRESH_CERT_RENEWAL|ANCESTOR_FALLBACK|NO_EXPIRED_AUTHORITY");
    private static final String ZERO="0000000000000000000000000000000000000000000000000000000000000000";
    private MainlineHeadSU1() {}
    public static final class Head {
        public final long generation;public final String hash,strategy,certificate;
        Head(long generation,String hash,String strategy,String certificate){this.generation=generation;this.hash=hash;this.strategy=strategy;this.certificate=certificate;}
    }
    public static Head read(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT generation,event_hash,strategy_id,certificate_id,previous_hash,operation,policy_hash,recorded_at FROM su1_mainline_head_event ORDER BY generation DESC LIMIT 1",null)){
        if(!c.moveToFirst())return new Head(0,ZERO,null,null);
        String expected=HashingSU1.fields(POLICY,String.valueOf(c.getLong(0)),c.getString(4),String.valueOf(c.getString(2)),String.valueOf(c.getString(3)),c.getString(5),String.valueOf(c.getLong(7)));
        if(!POLICY.equals(c.getString(6))||!expected.equals(c.getString(1)))throw new IllegalStateException("SU1_MAINLINE_HEAD_HASH");
        return new Head(c.getLong(0),c.getString(1),c.getString(2),c.getString(3));
    }}
    public static boolean isHead(SQLiteDatabase db,String strategy,String certificate){Head head=read(db);return strategy.equals(head.strategy)&&certificate.equals(head.certificate);}
    private static String parent(SQLiteDatabase db,String id){try(Cursor c=db.rawQuery("SELECT parent_id FROM su1_runtime_program WHERE strategy_id=?",new String[]{id})){return c.moveToFirst()&&!c.isNull(0)?c.getString(0):null;}}
    private static List<MainlineChoiceSU1.Candidate> valid(SQLiteDatabase db,long now){
        List<String> ids=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT certificate_id FROM su1_certificate WHERE issued_at<=? AND expires_at>? ORDER BY issued_at DESC LIMIT 64",new String[]{String.valueOf(now),String.valueOf(now)})){while(c.moveToNext())ids.add(c.getString(0));}
        List<MainlineChoiceSU1.Candidate> out=new ArrayList<>();for(String id:ids){CertificateSU1.Authority a=CertificateSU1.read(db,id,now,false);if(a!=null)out.add(new MainlineChoiceSU1.Candidate(a.strategy,a.certificate,parent(db,a.strategy),a.issued,a.expires));}return out;
    }
    private static void append(SQLiteDatabase db,Head old,MainlineChoiceSU1.Candidate candidate,String operation,long now){
        if(!db.inTransaction()||!read(db).hash.equals(old.hash))throw new IllegalStateException("SU1_MAINLINE_HEAD_CAS");
        if(candidate!=null){CertificateSU1.Authority a=CertificateSU1.read(db,candidate.certificate,now,false);if(a==null||!a.strategy.equals(candidate.strategy))throw new IllegalStateException("SU1_MAINLINE_HEAD_AUTHORITY_CHANGED");}
        long generation=old.generation+1;String strategy=candidate==null?null:candidate.strategy,cert=candidate==null?null:candidate.certificate;
        String hash=HashingSU1.fields(POLICY,String.valueOf(generation),old.hash,String.valueOf(strategy),String.valueOf(cert),operation,String.valueOf(now));
        StrategyRepositorySU1.row(db,"su1_mainline_head_event","generation",generation,"event_hash",hash,"previous_hash",old.hash,"strategy_id",strategy,"certificate_id",cert,"operation",operation,"policy_hash",POLICY,"recorded_at",now);
    }
    public static void maintain(DbStore store){
        long now=System.currentTimeMillis();Head old=read(store.readable());List<MainlineChoiceSU1.Candidate> valid=valid(store.readable(),now);
        boolean hold=StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_runtime_control WHERE singleton=1 AND reason='USER_MODEL_ROLLBACK'");
        MainlineChoiceSU1.Choice choice=MainlineChoiceSU1.choose(old.strategy,old.certificate,valid,id->parent(store.readable(),id),hold);
        if(choice==null||choice.candidate==null&&old.strategy==null)return;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_MAINLINE_HEAD_ADVANCE",db->{append(db,old,choice.candidate,choice.operation,now);return 0;});
    }
    public static boolean rollback(SQLiteDatabase db,long now){
        Head old=read(db);if(old.strategy==null)return false;List<MainlineChoiceSU1.Candidate> valid=valid(db,now);
        Set<String> seen=new HashSet<>();String p=parent(db,old.strategy);
        for(int depth=0;p!=null&&seen.add(p)&&depth<64;depth++,p=parent(db,p))for(MainlineChoiceSU1.Candidate c:valid)if(p.equals(c.strategy)){
            append(db,old,c,"USER_ROLLBACK",now);db.execSQL("UPDATE su1_runtime_control SET reason='USER_MODEL_ROLLBACK',updated_at=? WHERE singleton=1",new Object[]{now});return true;
        }
        return false;
    }
}
