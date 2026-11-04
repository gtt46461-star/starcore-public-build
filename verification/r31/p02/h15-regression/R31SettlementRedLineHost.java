package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.util.concurrent.Callable;

/** Actual production REAL/OOS routines, native SQLite engineering fixtures. No future quality evidence. */
public final class R31SettlementRedLineHost {
    private static int assertions,realCalls;
    private static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    private static double decimal(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getDouble(0);}}
    private static Object field(Object object,String name)throws Exception{Field f=object.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(object);}
    private static SettlementEngine.OwnerToken token(String target)throws Exception{Constructor<SettlementEngine.OwnerToken> c=SettlementEngine.OwnerToken.class.getDeclaredConstructor(String.class,long.class);c.setAccessible(true);return c.newInstance(target,System.currentTimeMillis());}
    private static Object real(SQLiteDatabase db,SettlementEngine.OwnerToken token,Models.Draw draw)throws Exception{
        realCalls++;Method method=SettlementEngine.class.getDeclaredMethod("settleRealWithinTransactionR36",SQLiteDatabase.class,SettlementEngine.OwnerToken.class,Models.Draw.class);method.setAccessible(true);
        try{return method.invoke(null,db,token,draw);}catch(InvocationTargetException wrapped){Throwable cause=wrapped.getCause();if(cause instanceof Error)throw (Error)cause;throw (Exception)cause;}
    }
    private static <T>T tx(SQLiteDatabase db,Callable<T> body,boolean commit)throws Exception{db.beginTransaction();try{T value=body.call();if(commit)db.setTransactionSuccessful();return value;}finally{db.endTransaction();}}
    private static Models.Draw draw(String issue){Models.Draw d=new Models.Draw();d.issue=issue;d.openTime=10000;d.fetchTime=10010;for(int i=0;i<10;i++)d.numbers[i]=i+1;return d;}
    private static void insertTrade(SQLiteDatabase db,String issue,String scope,int a,int b,int c){int mask=AppConfig.buildK3Mask(a,b,c);db.execSQL("INSERT INTO trades(target_issue,action,stat_scope,position,n1,n2,n3,k3_mask,settled) VALUES(?,'BET',?,1,?,?,?,?,0)",new Object[]{issue,scope,a,b,c,mask});db.execSQL("INSERT INTO decisions(target_issue,decision,stat_scope,settled) VALUES(?,'BET',?,0)",new Object[]{issue,scope});}
    private static void schema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE trades(_id INTEGER PRIMARY KEY,target_issue TEXT,action TEXT,stat_scope TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,k3_mask INTEGER,settled INTEGER DEFAULT 0,actual_number INTEGER,hit INTEGER,result TEXT,pnl REAL,settle_time INTEGER,settled_at INTEGER,miss_reason TEXT,settlement_reason TEXT)");
        db.execSQL("CREATE TABLE decisions(target_issue TEXT,decision TEXT,stat_scope TEXT,settled INTEGER,actual_number INTEGER,hit INTEGER,result TEXT,pnl REAL,settle_time INTEGER,settled_at INTEGER)");
        db.execSQL("CREATE TABLE host_effects(kind TEXT PRIMARY KEY,updates INTEGER)");db.execSQL("INSERT INTO host_effects VALUES('trade',0),('decision',0),('shadow',0)");
        db.execSQL("CREATE TRIGGER host_trade_effect AFTER UPDATE ON trades BEGIN UPDATE host_effects SET updates=updates+1 WHERE kind='trade'; END");db.execSQL("CREATE TRIGGER host_decision_effect AFTER UPDATE ON decisions BEGIN UPDATE host_effects SET updates=updates+1 WHERE kind='decision'; END");
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,first_seen_at INTEGER)");db.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT * FROM draws");
        db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT PRIMARY KEY,base_issue TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,selected_hit_p REAL,created_at INTEGER,commit_at INTEGER,record_hash TEXT,pre_draw_frozen INTEGER,model_proven INTEGER,settled INTEGER,history_hash TEXT,snapshot_hash TEXT,action_space_hash TEXT,game_code TEXT,history_max_issue TEXT,actual_number INTEGER,hit_label INTEGER,settled_at INTEGER,settlement_hash TEXT,error_code TEXT)");
        db.execSQL("CREATE TRIGGER host_shadow_effect AFTER UPDATE ON "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" BEGIN UPDATE host_effects SET updates=updates+1 WHERE kind='shadow'; END");
        db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_OOS_LABEL+"(target_issue TEXT PRIMARY KEY,game_code TEXT,base_issue TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,selected_hit_p REAL,actual_number INTEGER,hit_label INTEGER,strict_oos INTEGER,pre_draw_frozen INTEGER,prediction_created_at INTEGER,draw_open_time INTEGER,settled_at INTEGER,brier REAL,provenance_hash TEXT,record_hash TEXT,error_code TEXT)");
    }
    private static void realReplay(SQLiteDatabase db)throws Exception{
        Models.Draw win=draw("202611040100"),miss=draw("202611040101");insertTrade(db,win.issue,AppConfig.STAT_SCOPE_REAL_TRADE,1,2,3);insertTrade(db,miss.issue,AppConfig.STAT_SCOPE_REAL_TRADE,2,3,4);
        SettlementEngine.OwnerToken winOwner=token(win.issue),missOwner=token(miss.issue);
        Object rolled=tx(db,()->real(db,winOwner,win),false);need(Boolean.FALSE.equals(field(rolled,"replay")),"rollback fixture executes actual first settlement");need(scalar(db,"SELECT updates FROM host_effects WHERE kind='trade'")==0,"rolled-back production trade effect not counted");need(scalar(db,"SELECT settled FROM trades WHERE target_issue='202611040100'")==0,"trade settlement rolls back");
        for(int i=0;i<100;i++){
            Object a=tx(db,()->real(db,winOwner,win),true),b=tx(db,()->real(db,missOwner,miss),true);
            need(Boolean.valueOf(i>0).equals(field(a,"replay"))&&Boolean.valueOf(i>0).equals(field(b,"replay")),"first settlement then99 immutable replays");
            need(Boolean.TRUE.equals(field(a,"hit"))&&Boolean.FALSE.equals(field(b,"hit")),"fixed position K3 hit; other-position matches cannot count as hit");
            need(((Double)field(a,"pnl"))==AppConfig.WIN_PNL_K3&&((Double)field(b,"pnl"))==AppConfig.MISS_PNL_K3,"original fixed K3 PnL on every replay");
        }
        need(scalar(db,"SELECT updates FROM host_effects WHERE kind='trade'")==2&&scalar(db,"SELECT updates FROM host_effects WHERE kind='decision'")==2,"each REAL row and decision updated exactly once despite100 deliveries");
        need(Math.abs(decimal(db,"SELECT sum(pnl) FROM trades")-(6.9-3.0))<1e-12,"aggregate profit is one win plus one miss, not100 copies");
        Models.Draw corrected=draw(win.issue);corrected.numbers[0]=2;corrected.numbers[1]=1;
        try{tx(db,()->real(db,winOwner,corrected),true);throw new AssertionError("changed actual overwrite accepted");}catch(IllegalStateException expected){need("R36_REAL_SETTLEMENT_CONFLICT".equals(expected.getMessage()),"changed-draw conflict reason");}
        need(scalar(db,"SELECT actual_number FROM trades WHERE target_issue='202611040100'")==1,"old actual remains immutable on conflicting replay");
        insertTrade(db,"202611040102",AppConfig.STAT_SCOPE_SHADOW,1,2,3);Object shadow=tx(db,()->real(db,token("202611040102"),draw("202611040102")),true);need(!((Boolean)field(shadow,"settled"))&&scalar(db,"SELECT settled FROM trades WHERE target_issue='202611040102'")==0,"SHADOW cannot promote to REAL or settle as REAL");
        insertTrade(db,"202611040103",AppConfig.STAT_SCOPE_REAL_TRADE,1,1,2);
        try{tx(db,()->real(db,token("202611040103"),draw("202611040103")),true);throw new AssertionError("invalid K3 accepted");}catch(IllegalStateException expected){need("R36_REAL_TRADE_K3_INVALID".equals(expected.getMessage()),"invalid fixedK3 rejected");}
        try{tx(db,()->real(db,null,win),true);throw new AssertionError("ownerless settlement accepted");}catch(SecurityException expected){need("R36_REAL_SETTLEMENT_OWNER_REQUIRED".equals(expected.getMessage()),"owner token required");}
        insertTrade(db,"202611040104",AppConfig.STAT_SCOPE_REAL_TRADE,1,2,3);insertTrade(db,"202611040104",AppConfig.STAT_SCOPE_REAL_TRADE,1,2,3);
        try{tx(db,()->real(db,token("202611040104"),draw("202611040104")),true);throw new AssertionError("duplicate REAL rows accepted");}catch(IllegalStateException expected){need("R36_MULTIPLE_REAL_ROWS_FOR_ISSUE".equals(expected.getMessage()),"duplicate REAL authority rows rejected");}
        need(scalar(db,"SELECT updates FROM host_effects WHERE kind='trade'")==2,"all negative controls leave committed effects unchanged");
        System.out.println("R31_H15_REAL_SQLITE_PASS production_REAL_calls="+realCalls+" first_commit_rows=2 trade_updates=2 decision_updates=2 win_pnl="+AppConfig.WIN_PNL_K3+" miss_pnl="+AppConfig.MISS_PNL_K3+" repeat_deliveries_per_row=100 source_correction_rejected=true K3_owner_duplicate_scope_redlines=true");
    }
    private static void oosReplay(SQLiteDatabase db)throws Exception{
        Models.Draw d=draw("202611040106");String base="202611040105",hash=V51ChecksumEngine.sha256("HOST_CONTROLLED_ENGINEERING_FREEZE_NOT_NATURAL");db.execSQL("INSERT INTO draws VALUES(?,10010)",new Object[]{d.issue});
        db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue,base_issue,position,n1,n2,n3,selected_hit_p,created_at,commit_at,record_hash,pre_draw_frozen,model_proven,settled,history_hash,snapshot_hash,action_space_hash,game_code,history_max_issue) VALUES(?,?,1,1,2,3,0.4,9000,9000,?,1,0,0,?,?,?,?,?)",new Object[]{d.issue,base,hash,hash,hash,hash,V51Repository.GAME_CODE,base});
        SettlementEngine.OwnerToken owner=token(d.issue);String record=null;
        for(int i=0;i<100;i++){OosAfterDrawWriterR36.LabelResult r=tx(db,()->OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,owner,d),true);need(r.success&&r.replay==(i>0),"actual OOS first commit then99 replays");if(record==null)record=r.recordHash;need(record.equals(r.recordHash)&&hash.equals(r.canonicalHash)&&r.frozenAt==9000,"original canonical/time/label hash retained");need(r.hitLabel==1&&Math.abs(r.brier-0.36)<1e-12,"actual OOS fixed input numerical label");}
        need(scalar(db,"SELECT count(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL)==1&&scalar(db,"SELECT updates FROM host_effects WHERE kind='shadow'")==1,"OOS one label andone shadow effect for100 deliveries");
        Models.Draw missing=draw("202611040107");OosAfterDrawWriterR36.LabelResult no=tx(db,()->OosAfterDrawWriterR36.settleFrozenWithinTransaction(db,token(missing.issue),missing),true);need(!no.success&&"FROZEN_OBSERVATION_MISSING".equals(no.status),"missing freeze never backfilled");
        need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name IN('r26_feedback','r31_feedback_event')")==0,"production REAL/OOS routines operate without research schema");
        System.out.println("R31_H15_OOS_SQLITE_PASS actual_OOS_calls=101 duplicate_deliveries=100 labels=1 shadow_updates=1 original_hash_time_retained=true missing_freeze_refused=true research_schema_absent=true fixture_scope=ENGINEERING_NOT_FUTURE_REAL_QUALITY");
    }
    public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");try(java.sql.Connection c=DriverManager.getConnection("jdbc:sqlite:"+args[0])){SQLiteDatabase db=new SQLiteDatabase(c);schema(db);realReplay(db);oosReplay(db);System.out.println("R31_H15_NATIVE_REDLINE_HOST_PASS assertions="+assertions+" scope=ACTUAL_PRIVATE_REAL_AND_OOS_ROUTINES_NATIVE_SQLITE_NOT_FULL_COORDINATOR_NOT_DEVICE_NOT_QUALITY");}}
}
