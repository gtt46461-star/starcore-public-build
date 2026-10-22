package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;import java.util.*;import java.nio.charset.StandardCharsets;
public final class RetentionOwnerContractCP12 {
 static SQLiteDatabase fixtureDb;static int checks;static final String H="a".repeat(64),H2="b".repeat(64);
 static void ok(boolean value){checks++;if(!value)throw new AssertionError("check"+checks);}
 static void bad(Runnable body){checks++;try{body.run();}catch(IllegalArgumentException|IllegalStateException expected){return;}throw new AssertionError("bad accepted"+checks);}
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 static RetentionArchiveProtocolCP12.Receipt receipt(RetentionArchiveProtocolCP12.Page page,String hash){List<RetentionArchiveProtocolCP12.Member> members=new ArrayList<>();for(RetentionArchiveProtocolCP12.RowVersion r:page.rows)members.add(new RetentionArchiveProtocolCP12.Member(r.rowId,r.generation,DataRetentionGovernorR900.canonicalLiveKeyCP12(fixtureDb,page.table,r.rowId,H),H));return new RetentionArchiveProtocolCP12.Receipt(page,hash,100,H,H,1,members);}
 static int commit(SQLiteDatabase db,RetentionArchiveProtocolCP12.Receipt r,boolean commit){db.beginTransaction();try{int deleted=DataRetentionGovernorR900.publishAndPruneCP12(db,r,"host-contract",Long.MAX_VALUE);if(commit)db.setTransactionSuccessful();return deleted;}finally{db.endTransaction();}}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);db.execSQL("PRAGMA foreign_keys=ON");fixtureDb=db;try{
   ok(RetentionTableRegistryCP12.ALL.size()==406&&RetentionTableRegistryCP12.ALL_INSTALLED.size()==415);
   List<RetentionArchiveProtocolCP12.RowVersion> versions=Arrays.asList(new RetentionArchiveProtocolCP12.RowVersion(-1,0),new RetentionArchiveProtocolCP12.RowVersion(2,7));
   RetentionArchiveProtocolCP12.Page p=new RetentionArchiveProtocolCP12.Page("draws","202601010001",1,H,H,RetentionTableRegistryCP12.HASH,versions);ok(p.requestId.equals(RetentionArchiveProtocolCP12.Page.parse(p.requestJson).requestId));
   bad(()->RetentionArchiveProtocolCP12.Page.parse(" "+p.requestJson));bad(()->RetentionArchiveProtocolCP12.Page.parse(p.requestJson.replace("RETENTION_PAGE\"","FULL_REPORT\"")));bad(()->new RetentionArchiveProtocolCP12.Page("draws;delete","202601010001",1,H,H,H,versions));bad(()->new RetentionArchiveProtocolCP12.RowVersion(1,-1));bad(()->new RetentionArchiveProtocolCP12.Page("draws","202601010001",1,H,H,H,Arrays.asList(versions.get(1),versions.get(0))));
   String issue="202609230001";db.beginTransaction();try{StringBuilder values=new StringBuilder();for(int i=0;i<5001;i++){if(values.length()>0)values.append(',');values.append("('").append(issue).append("',").append(10000+i).append(",0,1,2,3,4,5,6,7,8,9,10)");issue=TargetIssueResolver.next(issue).targetIssue;if(i%100==99||i==5000){db.execSQL("INSERT INTO draws(issue,open_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES "+values);values.setLength(0);}}db.setTransactionSuccessful();}finally{db.endTransaction();}
   db.execSQL("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('log-1',1,'I','test','test','test','big old payload'),('log-2',2,'I','test','test','test','same')");
   RetentionArchiveProtocolCP12.Page page=DataRetentionGovernorR900.prepareArchivePageCP12(db,"diagnostic_log");ok(page!=null&&page.rows.size()==2);ok(page.schemaHash.matches("[0-9a-f]{64}"));RetentionArchiveProtocolCP12.Receipt r=receipt(page,H);
   ok(commit(db,r,false)==2);ok(count(db,"diagnostic_log")==2);ok(count(db,"research_evidence_archive_v2")==0);ok(count(db,"research_retention_permit_v2")==0);
   db.execSQL("UPDATE diagnostic_log SET detail='changed' WHERE log_key='log-1'");ok(commit(db,r,true)==1);ok(count(db,"diagnostic_log")==1);ok(count(db,"research_evidence_archive_v2")==1);ok(count(db,"research_retention_permit_v2")==0);
   ok(commit(db,r,true)==0);ok(count(db,"diagnostic_log")==1);ok(count(db,"research_evidence_archive_v2")==1);
   RetentionArchiveProtocolCP12.Page next=DataRetentionGovernorR900.prepareArchivePageCP12(db,"diagnostic_log");ok(next!=null&&next.rows.size()==1&&next.rows.get(0).generation>page.rows.get(0).generation);
   ok(commit(db,receipt(next,H2),true)==1);ok(count(db,"diagnostic_log")==0);
   RetentionArchiveProtocolCP12.Page draws=DataRetentionGovernorR900.prepareArchivePageCP12(db,"draws");ok(draws!=null&&draws.rows.size()==128);ok(commit(db,receipt(draws,"c".repeat(64)),true)==1);ok(count(db,"draws")==5000);
   String pinTask;String oldest;try(Cursor c=db.rawQuery("SELECT issue FROM draws ORDER BY issue LIMIT 1",null)){c.moveToFirst();oldest=c.getString(0);}db.beginTransaction();try{pinTask=ResearchTaskQueueR967.enqueue(db,"POST_TRAIN","pin-contract",oldest,H,H,H);ResearchTaskQueueR967.pin(db,pinTask,"draws",oldest);db.setTransactionSuccessful();}finally{db.endTransaction();}
   db.beginTransaction();try{ok(DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,issue));ok(count(db,"draws")==4999);ok(count(db,DrawDependencyInputCP12.TABLE)==1);}finally{db.endTransaction();}
   db.execSQL("UPDATE v57_research_task_r967 SET status='EXPIRED' WHERE task_id=?",new Object[]{pinTask});db.execSQL("DELETE FROM v57_research_input_pin_r967 WHERE task_id=?",new Object[]{pinTask});
   db.beginTransaction();try{ok(DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,issue));ok(count(db,"draws")==4999);}finally{db.endTransaction();}ok(count(db,"draws")==5000);
   db.beginTransaction();try{ok(DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,issue));db.execSQL("INSERT INTO draws(issue,open_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,20000,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue});db.setTransactionSuccessful();}finally{db.endTransaction();}ok(count(db,"draws")==5000);
   db.beginTransaction();try{ok(DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,TargetIssueResolver.next(issue).targetIssue));}finally{db.endTransaction();}
   db.execSQL("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('deadline-row',3,'I','test','test','test','deadline')");
   RetentionArchiveProtocolCP12.Page yielded=DataRetentionGovernorR900.prepareArchivePageCP12(db,"diagnostic_log");RetentionArchiveProtocolCP12.Receipt yieldedReceipt=receipt(yielded,"d".repeat(64));
   db.beginTransaction();try{ok(DataRetentionGovernorR900.publishAndPruneCP12(db,yieldedReceipt,"yield",0)==-2);db.setTransactionSuccessful();}finally{db.endTransaction();}ok(count(db,"diagnostic_log")==1);ok(commit(db,yieldedReceipt,true)==1);
   db.execSQL("UPDATE research_evidence_archive_v2 SET expires_at=1 WHERE archive_id=?",new Object[]{yieldedReceipt.archiveId});
   db.beginTransaction();try{ok(DataRetentionGovernorR900.summarizeArchiveWithinWriteCP12(db,yieldedReceipt.archiveId,2));db.setTransactionSuccessful();}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT state FROM research_evidence_archive_v2 WHERE archive_id=?",new String[]{yieldedReceipt.archiveId})){ok(c.moveToFirst()&&c.getString(0).equals("EXPIRED_SUMMARIZED"));}
   db.beginTransaction();try{for(int i=0;i<32;i++)db.execSQL("INSERT INTO causal_research_feedback_r947(lane,module,support,score,through_issue,evidence_hash,reason,updated_at) VALUES(?,'test',1,0.1,'202601010001','h','fixture',1)",new Object[]{"large-key-"+i+"x".repeat(15000)});db.setTransactionSuccessful();}finally{db.endTransaction();}
   RetentionArchiveProtocolCP12.Page boundedKeys=DataRetentionGovernorR900.prepareArchivePageCP12(db,"causal_research_feedback_r947");ok(boundedKeys.rows.size()>0&&boundedKeys.rows.size()<32);int keyTotal=0;for(RetentionArchiveProtocolCP12.RowVersion version:boundedKeys.rows)keyTotal+=DataRetentionGovernorR900.canonicalLiveKeyCP12(db,"causal_research_feedback_r947",version.rowId,H).length;ok(keyTotal<=RetentionArchiveProtocolCP12.MAX_KEY_TOTAL);
   RetentionArchiveProtocolCP12.Receipt largeProof=receipt(boundedKeys,"e".repeat(64));List<RetentionArchiveProtocolCP12.Member> badMembers=new ArrayList<>(largeProof.members);RetentionArchiveProtocolCP12.Member original=badMembers.get(0);badMembers.set(0,new RetentionArchiveProtocolCP12.Member(original.rowId,original.generation,new byte[]{1,2,3},original.rowHash));RetentionArchiveProtocolCP12.Receipt wrongKey=new RetentionArchiveProtocolCP12.Receipt(boundedKeys,"f".repeat(64),largeProof.byteCount,H,H,1,badMembers);bad(()->commit(db,wrongKey,true));ok(count(db,"causal_research_feedback_r947")==32);
   db.execSQL("INSERT INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',99999999)");db.beginTransaction();try{ok(DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,Long.MAX_VALUE)>=1);db.setTransactionSuccessful();}finally{db.endTransaction();}
   bad(()->DataRetentionGovernorR900.publishAndPruneCP12(db,r,"x",Long.MAX_VALUE));bad(()->DataRetentionGovernorR900.deletePage(db,"draws","issue","202609230001",1));
   db.execSQL("DELETE FROM app_state WHERE state_key='retention_cp12_cleanup_request'");
   db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('retention_cp12_inflight_request',?,1)",new Object[]{H});
   db.beginTransaction();try{ok(!DataRetentionGovernorR900.retryFailedRequestWithinWriteCP12(db,H2,"STALE_PAGE",100));ok(DataRetentionGovernorR900.retryFailedRequestWithinWriteCP12(db,H,"STALE_PAGE",100));}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='retention_cp12_inflight_request'",null)){ok(c.moveToFirst()&&c.getString(0).equals(H));}
   db.beginTransaction();try{ok(DataRetentionGovernorR900.retryFailedRequestWithinWriteCP12(db,H,"STALE_PAGE",100));db.setTransactionSuccessful();}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='retention_cp12_cleanup_request'",null)){ok(c.moveToFirst()&&c.getString(0).equals(H));}
   try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='retention_cp12_retry_until'",null)){ok(c.moveToFirst()&&c.getString(0).equals("1100"));}
   try(Cursor c=db.rawQuery("SELECT 1 FROM app_state WHERE state_key='retention_cp12_inflight_request'",null)){ok(!c.moveToFirst());}
   db.beginTransaction();try{ok(!DataRetentionGovernorR900.retryFailedRequestWithinWriteCP12(db,H,"CANCELLED",100));db.setTransactionSuccessful();}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
   Files.writeString(Path.of(args[2]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"registered_table_count\":415,\"original_registry_count\":406,\"scope\":\"Actual generatedregistry Pageidentity and Governor publication/per-membergenerationCAS/rollback/idempotency/strict5000tail reservation on hostSQLite; includes publication deadline resume, DB summaryexpiry, terminal failed-ACK sameTX cleanup/retry cursor CAS and rollback; native Service Android IO outside this test execution.\"}\n");
  }finally{db.shutdown();}
 }
}
