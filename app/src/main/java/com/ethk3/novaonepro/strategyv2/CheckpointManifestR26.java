package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
/** The same owner transaction binds a durable artifact, parent, policy, source and trained-through cursor. */
public final class CheckpointManifestR26 {
    private CheckpointManifestR26() {}
    public static void record(SQLiteDatabase db,String checkpoint,String policy,String source,String parent,long through,byte[] reference){
        if(!db.inTransaction())throw new IllegalStateException("R26_ARTIFACT_MANIFEST_WRITER");String hash=CheckpointArtifactsR26.referenceHash(reference);if(hash.isEmpty())throw new IllegalArgumentException("R26_ARTIFACT_REFERENCE_REQUIRED");
        try(Cursor c=db.rawQuery("SELECT artifact_hash,policy_hash,source_hash,parent_hash,trained_through FROM r26_checkpoint_artifact WHERE checkpoint_hash=?",new String[]{checkpoint})){
            if(c.moveToFirst()){if(!hash.equals(c.getString(0))||!policy.equals(c.getString(1))||!source.equals(c.getString(2))||!parent.equals(c.getString(3))||through!=c.getLong(4))throw new IllegalStateException("R26_ARTIFACT_MANIFEST_CONFLICT");return;}}
        db.execSQL("INSERT INTO r26_checkpoint_artifact VALUES(?,?,?,?,?,?,?)",new Object[]{checkpoint,hash,policy,source,parent,through,System.currentTimeMillis()});
    }
}
