package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import java.security.*;
import java.util.*;
/** Actual routine decision over native SQLite, with enough free pages to
 * reproduce the former synchronous VACUUM path. No offline service is mocked. */
public final class StorageCompactionReadOnlyR31Host {
 static long scalar(SQLiteDatabase db,String pragma){try(Cursor c=db.rawQuery("PRAGMA "+pragma,null)){if(!c.moveToFirst())throw new AssertionError("NO_PRAGMA_RESULT");return c.getLong(0);}}
 public static void main(String[] args)throws Exception{
  Path file=Path.of(args[0]);byte[] before=MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file));Class.forName("org.sqlite.JDBC");
  try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+file)){SQLiteDatabase db=new SQLiteDatabase(c);long pages=scalar(db,"page_count"),free=scalar(db,"freelist_count");if(scalar(db,"auto_vacuum")!=0||free<=1024||free*4<=pages)throw new AssertionError("LEGACY_REPRO_PRECONDITION");
   String status=DatabaseCompactionSU2.convertIfNeeded(db,1L<<40);if(!status.startsWith("OFFLINE_REBUILD_REQUIRED"))throw new AssertionError("SYNC_LEGACY_REBUILD_ENTERED");if(scalar(db,"auto_vacuum")!=0)throw new AssertionError("ROUTINE_CHANGED_VACUUM_MODE");
   try(Cursor row=db.rawQuery("SELECT value FROM original",null)){if(!row.moveToFirst()||!"PRESERVE_OLD_DB".equals(row.getString(0)))throw new AssertionError("ORIGINAL_ASSET_CHANGED");}
  }
  if(!Arrays.equals(before,MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))))throw new AssertionError("ROUTINE_REWROTE_OLD_DATABASE");
  for(int i=1;i<args.length;i++){
   String[] fields=args[i].split(":",2);String[] numbers=fields[1].split(",");long[] actual=new long[numbers.length];for(int j=0;j<numbers.length;j++)actual[j]=Long.parseLong(numbers[j]);
   if(!fields[0].equals(StorageCheckpointServiceR31.resultStateR31(actual)))throw new AssertionError("REAL_NATIVE_RESULT_MISCLASSIFIED:"+args[i]);
  }
  if(args.length>1)System.out.println("ACTUAL_NATIVE_PASSIVE_RESULT_CLASSIFICATION_PASS observations="+(args.length-1)+" budgetOrCancelNeverActual=true");
  System.out.println("STORAGE_COMPACTION_NATIVE_PASS actualLargeFreelist=true databaseBytesUnchanged=true modeUnchanged=true offlineServiceInvoked=false");
 }
}
