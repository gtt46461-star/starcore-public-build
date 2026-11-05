package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.sql.*;
import java.util.*;
/** One existing B02-C01 report contract; actual capture/native SQLite, controlled ledger row. */
public final class WriterReportContractHost {
 public static void main(String[] args)throws Exception{
  File f=new File(args[0]);f.getParentFile().mkdirs();Class.forName("org.sqlite.JDBC");
  try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+f);SQLiteDatabase db=new SQLiteDatabase(connection)){
   db.execSQL(HoldLedgerFlushR30.DDL);long now=System.currentTimeMillis();
   db.execSQL(HoldLedgerFlushR30.INSERT,new Object[]{now-1000,now,"CONTROLLED_WRITER_ROW",0,10,1,2,3,80,5,100,3,0,1,"{}","CONTROLLED_PROCESS"});
   db.execSQL("PRAGMA query_only=ON");ReportR27.R30Input actual=ReportSnapshotR30.capture(db);
   if(!actual.errors.isEmpty())throw new AssertionError("HOST_CAPTURE_BOUNDARY_FAILURE:"+actual.errors);
   List<?> rows=(List<?>)actual.data.get("writerWindows");if(rows==null||rows.size()!=1)throw new AssertionError("DID_NOT_REACH_ACTUAL_WRITER_REPORT_ROW");
   Map<?,?> row=(Map<?,?>)rows.get(0);System.out.println("ACTUAL_WRITER_REPORT_ROW "+JsonR27.write(row));
   System.out.println("ROLLBACK_SOURCE_EXPORTED="+(row.containsKey("rolled_back")||row.containsKey("rolledBack"))+"; failures remains an attempt-failure count, not relabeled rollback");
   if(!row.containsKey("over_budget"))throw new AssertionError("PERSISTED_OVER_BUDGET_COUNTER_DROPPED_BY_REPORT");
   if(((Number)row.get("over_budget")).longValue()!=3)throw new AssertionError("WRITER_OVER_BUDGET_VALUE_CHANGED");
  }
 }
}
