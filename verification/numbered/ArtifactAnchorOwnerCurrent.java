package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Anchor migration/register failures through the actual unique writer on an old-FK test copy. */
public final class ArtifactAnchorOwnerCurrent {
    private static int assertions;
    private static void require(boolean condition,String detail){assertions++;if(!condition)throw new AssertionError(detail);}
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){require(c.moveToFirst(),"count row");return c.getLong(0);}}
    private static boolean fk(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("PRAGMA foreign_key_list(research_development_receipt_v2)",null)){while(c.moveToNext())if(table.equals(c.getString(2)))return true;return false;}}
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try {
            db.execSQL("PRAGMA foreign_keys=ON");
            DbStore store=HostProductR974.store(db);DatabaseCoordinator owner=store.coordinatorR36();
            String artifact;try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r952_artifact",null)){require(c.moveToFirst(),"old raw artifact required");artifact=c.getString(0);}
            require(fk(db,"r952_artifact"),"old receipt FK fixture required");
            require(count(db,"SELECT count(*) FROM research_artifact_identity_v2")==0,"empty initial anchor required");
            try{RetentionArtifactIdentityCP12.register(db,artifact);throw new AssertionError("register outside transaction");}
            catch(IllegalStateException expected){require("ARTIFACT_ANCHOR_TRANSACTION".equals(expected.getMessage()),"typed transaction rejection");}
            require(!owner.runAtomicWrite("NUMBERED_ANCHOR_MISSING_SOURCE",x->{
                x.execSQL("INSERT INTO app_state VALUES('numbered_anchor_provisional','missing',1)");
                RetentionArtifactIdentityCP12.register(x,"0000000000000000000000000000000000000000000000000000000000000000");
            }),"missing source owner accepted");
            require(count(db,"SELECT count(*) FROM app_state WHERE state_key='numbered_anchor_provisional'")==0,"missing-source provisional committed");
            require(count(db,"SELECT count(*) FROM research_artifact_identity_v2")==0,"missing-source anchor committed");
            require(owner.runAtomicWrite("NUMBERED_ANCHOR_CONFLICT_PREREQUISITE",x->{
                x.execSQL("INSERT INTO research_artifact_identity_v2 SELECT artifact_hash,'explicit_conflicting_candidate',target_issue,delivery_hash,payload_hash,development_receipt_hash,validation_hash,training_kind,development_sample_n,validation_sample_n,validation_pass,created_at FROM r952_artifact");
            }),"conflict fixture insert failed");
            final boolean[] typedConflict={false};
            require(!owner.runAtomicWrite("NUMBERED_ANCHOR_MIGRATION_CONFLICT",x->{
                x.execSQL("INSERT INTO app_state VALUES('numbered_anchor_provisional','conflict',1)");
                try{RetentionArtifactIdentityCP12.install(x);}catch(IllegalStateException expected){typedConflict[0]="ARTIFACT_ANCHOR_MIGRATION_CONFLICT".equals(expected.getMessage());throw expected;}
            }),"conflicting identity migration accepted");
            require(typedConflict[0],"wrong migration conflict code");
            require(fk(db,"r952_artifact")&&!fk(db,"research_artifact_identity_v2"),"conflict advanced old FK");
            require(count(db,"SELECT count(*) FROM research_development_receipt_v2")==1,"old receipt lost after failed migration");
            require(count(db,"SELECT count(*) FROM app_state WHERE state_key='numbered_anchor_provisional'")==0,"conflict provisional committed");
            require(count(db,"SELECT count(*) FROM research_artifact_identity_v2 WHERE candidate_id='explicit_conflicting_candidate'")==1,"original conflicting anchor changed");
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){require(!c.moveToFirst(),"FK after rollback");}
            System.out.println("PASS|assertions="+assertions+"|actual unique writer missing-input/conflict rollback and old receipt preservation; explicit synthetic prerequisite, host SQLite adapters");
        }finally{db.shutdown();}
    }
}
