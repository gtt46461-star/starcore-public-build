package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** CP12 revision backfill and rollback through the production unique writer.
 * Uses a copy of the real host Creator/training replay plus two explicitly
 * synthetic metadata rows; those rows are never model/effect evidence. */
public final class RevisionOwnerCurrent {
    private static int assertions;
    private static final String H="a".repeat(64);
    private static final String[] TABLES={"research_training_feature_row_cp11",
        "research_training_feature_cursor_cp12","research_training_registration_row_cp12",
        "research_formal_observation_cp12"};
    private static void require(boolean value,String detail){assertions++;if(!value)throw new AssertionError(detail);}
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),"scalar row");return c.getLong(0);}}
    private static long first(SQLiteDatabase db,String table){return scalar(db,"SELECT rowid FROM "+table+" ORDER BY rowid LIMIT 1");}
    private static long generation(SQLiteDatabase db,String table,long row){return scalar(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='"+table+"' AND source_rowid="+row);}
    private static String schema(SQLiteDatabase db){StringBuilder result=new StringBuilder();try(Cursor c=db.rawQuery("SELECT type,name,tbl_name,sql FROM sqlite_master ORDER BY type,name",null)){while(c.moveToNext())for(int i=0;i<4;i++)result.append(c.getString(i)).append('\n');}return result.toString();}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try {
            db.execSQL("PRAGMA foreign_keys=ON");
            DatabaseCoordinator owner=HostProductR974.store(db).coordinatorR36();
            require(scalar(db,"SELECT count(*) FROM research_training_feature_row_cp11")==849,"current computed feature prerequisite");
            try{RetentionRevisionSchemaCP12.schema(db);throw new AssertionError("outside Owner TX accepted");}
            catch(IllegalStateException expected){require("RETENTION_SCHEMA_OWNER".equals(expected.getMessage()),"wrong Owner code");}
            require(owner.runAtomicWrite("NUMBERED_REVISION_METADATA_FIXTURE",x->{
                x.execSQL("INSERT INTO research_training_registration_row_cp12 SELECT action_id,?,0,'EXPLICIT_SYNTHETIC_REVISION_ONLY',?,1 FROM research_action_v2 LIMIT 1",new Object[]{H,H});
                x.execSQL("INSERT INTO research_cp4_frozen_lifetime VALUES('explicit-revision-lifetime','explicit-revision-candidate','explicit-revision-generation',?,'{}',?,'{}','explicit-revision-hypothesis','{}','202609230001','202609230002',1)",new Object[]{H,H});
                x.execSQL("INSERT INTO research_cp4_window_binding VALUES('explicit-revision-window','explicit-revision-lifetime',1,'202609230001','202609230002',2,?)",new Object[]{H});
                x.execSQL("INSERT INTO research_formal_observation_cp12 VALUES(?,'explicit-revision-window','explicit-revision-lifetime','explicit-revision-candidate','explicit-revision-learner','202609230001',1,1,7,0.3,1,?,?,1,?,1)",new Object[]{H,H,H,H});
                for(String table:TABLES){
                    for(String operation:new String[]{"insert","update","delete"})x.execSQL("DROP TRIGGER cp12_revision_"+table+'_'+operation);
                    x.execSQL("DELETE FROM research_row_revision_v2 WHERE source_table=?",new Object[]{table});
                }
            }),"explicit pre019 fixture failed");
            String schemaBeforeFailure=schema(db);
            require(!owner.runAtomicWrite("NUMBERED_REVISION_INSTALL_FAILURE",x->{
                RetentionRevisionSchemaCP12.schema(x);
                x.execSQL("INSERT INTO app_state VALUES('numbered_revision_provisional','must rollback',1)");
                throw new IllegalStateException("EXPLICIT_NUMBERED_REVISION_ROLLBACK");
            }),"failed install committed");
            require(schemaBeforeFailure.equals(schema(db)),"failed install changed full schema");
            for(String table:TABLES)require(scalar(db,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name LIKE 'cp12_revision_%' AND tbl_name='"+table+"'")==0,"DDL rollback leaked "+table);
            require(scalar(db,"SELECT count(*) FROM app_state WHERE state_key='numbered_revision_provisional'")==0,"marker rollback leaked");
            for(String table:TABLES)require(scalar(db,"SELECT count(*) FROM research_row_revision_v2 WHERE source_table='"+table+"'")==0,"backfill rollback leaked");
            require(owner.runAtomicWrite("NUMBERED_REVISION_INSTALL",RetentionRevisionSchemaCP12::schema),"install failed");
            long[] before=new long[4];
            for(int i=0;i<TABLES.length;i++){
                String table=TABLES[i];before[i]=generation(db,table,first(db,table));
                require(scalar(db,"SELECT count(*) FROM \""+table+"\"")==scalar(db,"SELECT count(*) FROM research_row_revision_v2 WHERE source_table='"+table+"'"),"incomplete backfill "+table);
            }
            require(owner.runAtomicWrite("NUMBERED_REVISION_RERUN",RetentionRevisionSchemaCP12::schema),"rerun failed");
            for(int i=0;i<TABLES.length;i++)require(before[i]==generation(db,TABLES[i],first(db,TABLES[i])),"rerun changed source generation");
            require(owner.runAtomicWrite("NUMBERED_REVISION_CURSOR_UPDATE",x->x.execSQL("UPDATE research_training_feature_cursor_cp12 SET updated_at=updated_at+1")),"legal cursor update");
            long revised=generation(db,TABLES[1],first(db,TABLES[1]));require(revised>before[1],"cursor update did not advance revision");
            require(!owner.runAtomicWrite("NUMBERED_REVISION_CURSOR_ROLLBACK",x->{x.execSQL("UPDATE research_training_feature_cursor_cp12 SET updated_at=updated_at+1");throw new IllegalStateException("EXPLICIT_NUMBERED_CURSOR_ROLLBACK");}),"rollback update committed");
            require(generation(db,TABLES[1],first(db,TABLES[1]))==revised,"rollback changed revision");
            for(int i:new int[]{0,2,3}){
                String table=TABLES[i];long row=first(db,table),gen=generation(db,table,row);
                require(!owner.runAtomicWrite("NUMBERED_REVISION_IMMUTABLE",x->x.execSQL("UPDATE "+table+" SET created_at=created_at+1 WHERE rowid="+row)),"immutable update accepted");
                require(generation(db,table,row)==gen,"rejected immutable update changed revision");
            }
            for(String table:TABLES){
                long row=first(db,table),gen=generation(db,table,row);
                final boolean[] typed={false};
                require(!owner.runAtomicWrite("NUMBERED_REVISION_RAW_DELETE",x->{try{x.execSQL("DELETE FROM "+table+" WHERE rowid="+row);}catch(IllegalStateException expected){typed[0]=expected.getMessage().contains("CP12_RAW_DELETE_REQUIRES_VERIFIED_ARCHIVE");throw expected;}}),"unverified raw delete accepted");
                require(typed[0],"wrong raw delete blocker "+table);require(generation(db,table,row)==gen,"failed delete lost row revision");
            }
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){require(!c.moveToFirst(),"FK failed");}
            require(scalar(db,"SELECT count(*) FROM research_retention_permit_v2")==0,"orphan permit");
            System.out.println("PASS|assertions="+assertions+"|4 generation tables|actual unique writer DDL/backfill rollback, idempotent rerun, cursor revision/rollback, immutable-source rejection, no-proof raw-delete rejection|host SQLite; explicit metadata is not Future/qualification evidence");
        }finally{db.shutdown();}
    }
}
