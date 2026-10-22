package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;import java.util.*;
/** Actual retention selector and necessary-range query; no archive authority is inferred from a probe. */
public final class RetentionRangeR77 {
 static int checks;static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;System.out.println("PASS|"+why);}
 static long scalar(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] a)throws Exception{
  RealtimePressureGateR811.resetForTests();SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);String mode=a[2],table="v57_k3_action_space_1200";
  try{
   long before=scalar(db,"SELECT COUNT(*) FROM "+table);
   RetentionArchiveProtocolCP12.Page p=DataRetentionGovernorR900.prepareArchivePageCP12(db,table);
   if(mode.equals("protected"))ok(p==null,"PROTECTED_RANGE_RETURNS_NO_ARCHIVE_PAGE");
   else if(mode.equals("pinned"))ok(p==null,"ALL_OLD_INPUT_PINNED_NO_ARCHIVE_PAGE");
   else{
    ok(p!=null&&p.rows.size()==128,"MIXED_FIRST_PAGE_BOUNDED_128");
    for(RetentionArchiveProtocolCP12.RowVersion r:p.rows){try(Cursor c=db.rawQuery("SELECT target_issue FROM "+table+" WHERE rowid=?",new String[]{Long.toString(r.rowId)})){c.moveToFirst();String issue=c.getString(0);ok(issue.equals("202610090001"),"ONLY_UNPINNED_OLD_VALID_ROW_"+r.rowId);}}
    RetentionArchiveProtocolCP12.Page restored=RetentionArchiveProtocolCP12.Page.parse(p.requestJson);ok(restored.requestId.equals(p.requestId),"DURABLE_PAGE_IDENTITY_SURVIVES_REOPEN");Files.writeString(Path.of(a[3]+".page.json"),p.requestJson);
   }
   ok(scalar(db,"SELECT COUNT(*) FROM "+table)==before,"SELECTION_NEVER_DELETES_SOURCE_ROWS");
   if(mode.equals("protected")){
    RetentionTableRegistryCP12.Rule rule=new RetentionTableRegistryCP12.Rule("range_specimen","ARCHIVE_RAW_TAIL","issue","","length(\"range_specimen\".\"issue\")=12 AND \"range_specimen\".\"issue\"<?");
    ok(!DataRetentionGovernorR900.hasRowsBeforeCutoffR77(db,rule,"202610100014",1),"NULL_EQUAL_AND_NEWER_DO_NOT_PROVE_OLD_RANGE");
    RetentionTableRegistryCP12.Rule special=new RetentionTableRegistryCP12.Rule("range_specimen","ARCHIVE_RAW_TAIL","issue","","issue='special' OR issue<?");
    ok(DataRetentionGovernorR900.hasRowsBeforeCutoffR77(db,special,"202610100014",1),"NON_LEADING_CUSTOM_PREDICATE_NOT_FACTORED");
    RetentionTableRegistryCP12.Rule or=new RetentionTableRegistryCP12.Rule("range_specimen","ARCHIVE_RAW_TAIL","issue","",rule.eligibleSql+" OR issue='202610130693'");
    ok(DataRetentionGovernorR900.hasRowsBeforeCutoffR77(db,or,"202610100014",1),"TRAILING_OR_IS_NOT_A_NECESSARY_RANGE");
    RetentionTableRegistryCP12.Rule unindexed=new RetentionTableRegistryCP12.Rule("range_unindexed","ARCHIVE_RAW_TAIL","issue","","length(\"range_unindexed\".\"issue\")=12 AND \"range_unindexed\".\"issue\"<?");
    ok(DataRetentionGovernorR900.hasRowsBeforeCutoffR77(db,unindexed,"202610100014",1),"MISSING_INDEX_NEVER_FAKES_EMPTY_OLD_RANGE");
    ok(scalar(db,"SELECT COUNT(*) FROM range_unindexed WHERE length(issue)=12 AND issue<'202610100014'")==1,"NULL_EMPTY_MALFORMED_EQUAL_KEEP_ORIGINAL_ELIGIBILITY");
    RetentionTableRegistryCP12.Rule time=new RetentionTableRegistryCP12.Rule("range_time","ARCHIVE_RAW_TAIL","","at","\"range_time\".\"at\">0 AND \"range_time\".\"at\"<?");
    ok(DataRetentionGovernorR900.hasRowsBeforeCutoffR77(db,time,"",100),"TIME_RANGE_PRESERVES_OLD_POSITIVE_VALUE");
    ok(scalar(db,"SELECT COUNT(*) FROM range_time WHERE at>0 AND at<100")==1,"TIME_ZERO_NEGATIVE_NULL_EQUAL_KEEP_ORIGINAL_ELIGIBILITY");
    try{DataRetentionGovernorR900.hasRowsBeforeCutoffR77(db,new RetentionTableRegistryCP12.Rule("missing_range_table","ARCHIVE_RAW_TAIL","issue","","length(\"missing_range_table\".\"issue\")=12 AND \"missing_range_table\".\"issue\"<?"),"202610100014",1);throw new AssertionError("MISSING_TABLE_ACCEPTED");}catch(IllegalStateException expected){ok(expected.getMessage().contains("no such table"),"RANGE_READ_ERROR_PROPAGATES_NEVER_FAKE_SUCCESS");}
   }
   Files.writeString(Path.of(a[3]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"mode\":\""+mode+"\",\"scope\":\"ACTUAL_RETENTION_SELECTION_READ_ONLY_REAL_SQLITE_NOT_PHONE\"}\n");
  }finally{db.shutdown();}
 }
}
