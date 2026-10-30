package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;
import java.util.*;

/** Two real SQLite databases: a Root choice protects its retained research member without a fake RP choice. */
public final class ShadowRetirementAuthorityR29HostTest {
    static final String PROTECTED=StatsR27.sha256("root-choice"),OTHER=StatsR27.sha256("eligible-challenger"),PARENT=StatsR27.sha256("family-parent");
    static final long EXPIRES=QualificationWindowR26.LENGTH+1,NEXT=EXPIRES+10;static int checks;
    static void check(String name,boolean good){System.out.println("R29_SHADOW_AUTHORITY|"+name+"|"+(good?"OK":"FAIL"));if(!good)throw new AssertionError(name);checks++;}
    static SQLiteDatabase open()throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void root(SQLiteDatabase db){for(String ddl:StrategySchemaSU2.DDL)if(ddl.startsWith("CREATE TABLE IF NOT EXISTS su2_mainline_choice("))db.execSQL(ddl);db.execSQL("INSERT INTO su2_mainline_choice VALUES(1,?,0,0,'SYNTHETIC_ROOT_AUTHORITY',0,1)",new Object[]{"SU2:"+PROTECTED});}
    static void research(SQLiteDatabase db){
        db.beginTransactionNonExclusive();try{RuntimeSchemaR26.ensure(db);for(String ddl:StrategySchemaSU2.DDL)if(!ddl.contains("su2_mainline_choice")&&!ddl.contains("su2_mainline_binding")&&!ddl.contains("su2_live_decision"))db.execSQL(ddl);db.setTransactionSuccessful();}finally{db.endTransaction();}
        for(String hash:new String[]{PROTECTED,OTHER,PARENT}){
            db.execSQL("INSERT INTO r26_live_model VALUES(?,? ,X'0001FF','retained-state-hash','retained-checkpoint','parent',100,1,101,'ACTIVE',1,'01')",new Object[]{hash,"SU2|SYNTHETIC_RETAINED_SHADOW_STATE"});
            db.execSQL("INSERT INTO su2_model_genome(genome_key,canonical_text,formula,tier,origin_op,born_campaign,born_sequence,state,created_at) VALUES(?,?,'synthetic','F15','SYNTHETIC','fixture',1,'LEAGUE',1)",new Object[]{hash,"SU2|SYNTHETIC_RETAINED_SHADOW_STATE"});
            db.execSQL("INSERT INTO su2_league(member_id,kind,name,genome_key,joined_sequence) VALUES(?,'SU2','fixture',?,1)",new Object[]{"SU2:"+hash,hash});
            db.execSQL("INSERT INTO r26_cohort(genome_key,from_sequence,to_sequence,incumbent,protocol,checked_sequence) VALUES(?,1,?,'fixture-benchmark',?,?)",new Object[]{hash,EXPIRES,QualificationWindowR26.PROTOCOL,EXPIRES});
            db.execSQL("INSERT INTO r26_shadow_admission VALUES(?,'01',?,1,?,'parent')",new Object[]{hash,hash.equals(PARENT)?"BASE":"CHALLENGER",hash.equals(PARENT)?0:EXPIRES});
        }
    }
    static long retire(SQLiteDatabase rp,SQLiteDatabase root){rp.beginTransactionNonExclusive();try{long result=ShadowAdmissionR26.retireOne(rp,root,NEXT,1000);rp.setTransactionSuccessful();return result;}finally{rp.endTransaction();}}
    public static void main(String[]args)throws Exception{
        try(SQLiteDatabase root=open();SQLiteDatabase rp=open()){
            root(root);research(rp);long before=n(root,"SELECT total_changes()");
            check("S1_RP_HAS_NO_MAINLINE_CHOICE_AUTHORITY",n(rp,"SELECT count(*) FROM sqlite_master WHERE name='su2_mainline_choice'")==0);
            check("S2_ONLY_UNPROTECTED_ELIGIBLE_CHALLENGER_RETIRES",retire(rp,root)==1&&"ACTIVE".equals(s(rp,"SELECT status FROM r26_live_model WHERE policy_hash='"+PROTECTED+"'"))&&s(rp,"SELECT status FROM r26_live_model WHERE policy_hash='"+OTHER+"'").startsWith("RETIRED_"));
            check("S3_ROOT_CURRENT_MEMBER_STAYS_PROTECTED",retire(rp,root)==0&&"ACTIVE".equals(s(rp,"SELECT state FROM su2_league WHERE genome_key='"+PROTECTED+"'")));
            check("S4_PARENT_LEARNED_STATE_AND_CHECKPOINTS_REMAIN",n(rp,"SELECT count(*) FROM r26_live_model WHERE state=X'0001FF' AND state_hash='retained-state-hash' AND checkpoint_hash='retained-checkpoint'")==3&&"ACTIVE".equals(s(rp,"SELECT status FROM r26_live_model WHERE policy_hash='"+PARENT+"'")));
            check("S5_ROOT_AUTHORITY_NEVER_MUTATED",before==n(root,"SELECT total_changes()")&&("SU2:"+PROTECTED).equals(s(root,"SELECT member_id FROM su2_mainline_choice")));
            check("S6_RETIREMENT_UPDATES_ONLY_RESEARCH_RECORDS",n(rp,"SELECT count(*) FROM r26_live_failure")==1&&"RETIRED".equals(s(rp,"SELECT state FROM su2_model_genome WHERE genome_key='"+OTHER+"'"))&&"RETIRED_CHALLENGER_WINDOW_COMPLETE".equals(s(rp,"SELECT state FROM r26_cohort WHERE genome_key='"+OTHER+"'")));
            root.execSQL("DROP TABLE su2_mainline_choice");boolean failed=false;try{retire(rp,root);}catch(android.database.sqlite.SQLiteException genuine){failed=genuine.getMessage().contains("no such table");}
            check("S7_MISSING_REAL_ROOT_AUTHORITY_REMAINS_SQL_ERROR",failed&&"ACTIVE".equals(s(rp,"SELECT status FROM r26_live_model WHERE policy_hash='"+PROTECTED+"'")));
        }
        try(SQLiteDatabase legacy=open()){
            root(legacy);research(legacy);legacy.beginTransactionNonExclusive();try{long retired=ShadowAdmissionR26.retireOne(legacy,NEXT,1000);check("S8_ORIGINAL_SINGLE_DATABASE_PATH_STILL_WORKS",retired==1);legacy.setTransactionSuccessful();}finally{legacy.endTransaction();}
        }
        System.out.println("R29_SHADOW_AUTHORITY|ALL_PASS|checks="+checks+"|HOST_SQL_ONLY_NATIVE_ROOT_HELD_PENDING");
    }
}
