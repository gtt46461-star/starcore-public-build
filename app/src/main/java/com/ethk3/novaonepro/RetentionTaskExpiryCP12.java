package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
/** Compact permanent reason receipt; deliberately has no FK to prunable raw scheduler rows. */
public final class RetentionTaskExpiryCP12 {
 private RetentionTaskExpiryCP12(){}
 public static void schema(SQLiteDatabase db){if(db==null||!db.inTransaction())throw new IllegalStateException("RETENTION_SCHEMA_OWNER");
db.execSQL("CREATE TABLE IF NOT EXISTS research_task_retention_expiry_cp12(\n task_id TEXT PRIMARY KEY,kind TEXT NOT NULL CHECK(kind='POST_TRAIN'),target_issue TEXT NOT NULL,\n input_hash TEXT NOT NULL,authority_ref TEXT NOT NULL,cursor_hash TEXT NOT NULL CHECK(length(cursor_hash)=64),\n prior_generation INTEGER NOT NULL CHECK(prior_generation>=0),cutoff_issue TEXT NOT NULL,\n reason TEXT NOT NULL CHECK(reason='POST_TRAIN_INPUT_EXPIRED_WITHOUT_REVIEW'),\n terminal_hash TEXT NOT NULL UNIQUE CHECK(length(terminal_hash)=64),created_at INTEGER NOT NULL);");
db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_task_expiry_no_update BEFORE UPDATE ON research_task_retention_expiry_cp12 BEGIN SELECT RAISE(ABORT,'TASK_EXPIRY_IMMUTABLE'); END;");
db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_task_expiry_no_delete BEFORE DELETE ON research_task_retention_expiry_cp12 BEGIN SELECT RAISE(ABORT,'TASK_EXPIRY_PERMANENT'); END;");
db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_task_expiry_no_replace BEFORE INSERT ON research_task_retention_expiry_cp12 WHEN EXISTS(SELECT 1 FROM research_task_retention_expiry_cp12 WHERE task_id=NEW.task_id OR terminal_hash=NEW.terminal_hash) BEGIN SELECT RAISE(ABORT,'TASK_EXPIRY_REPLACE_FORBIDDEN'); END;");
 }
 public static String record(SQLiteDatabase db,String task,String issue,String input,String authority,String cursor,long generation,String cutoff){
  if(db==null||!db.inTransaction()||generation<0)throw new IllegalStateException("RETENTION_TASK_OWNER_TRANSACTION");
  String cursorHash=V51ChecksumEngine.sha256(cursor),reason="POST_TRAIN_INPUT_EXPIRED_WITHOUT_REVIEW";
  String hash=V51ChecksumEngine.sha256("TASK_RETENTION_EXPIRY_CP12_1"+part(task)+part(issue)+part(input)+part(authority)+part(cursorHash)+part(Long.toString(generation))+part(cutoff)+part(reason));
  try(Cursor c=db.rawQuery("SELECT terminal_hash FROM research_task_retention_expiry_cp12 WHERE task_id=?",new String[]{task})){if(c.moveToFirst()){if(!hash.equals(c.getString(0)))throw new IllegalStateException("RETENTION_TASK_EXPIRY_CONFLICT");return hash;}}
  db.execSQL("INSERT INTO research_task_retention_expiry_cp12(task_id,kind,target_issue,input_hash,authority_ref,cursor_hash,prior_generation,cutoff_issue,reason,terminal_hash,created_at) VALUES(?,'POST_TRAIN',?,?,?,?,?,?,?,?,?)",new Object[]{task,issue,input,authority,cursorHash,generation,cutoff,reason,hash,System.currentTimeMillis()});return hash;
 }
 private static String part(String value){if(value==null)throw new IllegalArgumentException("RETENTION_TASK_FIELD_NULL");return value.length()+":"+value;}
}
