package com.ethk3.novaonepro;
import android.database.Cursor;
/** Evidence rendering only. Numeric cells/SQL predicates are unchanged; huge raw values use exact snapshot row references. */
public final class ReportBoundedSqlCP12 {
 private ReportBoundedSqlCP12(){}
 public static String projection(ReportReadContextCP4 context,String table,String alias){
  if(!table.matches("[A-Za-z_][A-Za-z0-9_]*")||!alias.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("REPORT_PROJECTION_IDENTIFIER");
  StringBuilder out=new StringBuilder(4096);int count=0;
  try(Cursor columns=context.select("PRAGMA table_info(\""+table+"\")",null)){
   while(columns.moveToNext()){
    if(++count>256)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_SCHEMA_COLUMN_LIMIT");String column=columns.getString(1);if(column.indexOf('\0')>=0||column.length()>128)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_SCHEMA_COLUMN_NAME");
    if(count>1)out.append(',');String q="\""+column.replace("\"","\"\"")+"\"",cell=alias+"."+q;
    out.append("CASE WHEN typeof(").append(cell).append(") IN ('text','blob') AND length(CAST(").append(cell).append(" AS BLOB))>4096 THEN 'RAW_SQLITE_REFERENCE|snapshot=").append(context.snapshotSha256()).append("|table=").append(table).append("|rowid='||").append(alias).append(".rowid||'|column=").append(column.replace("'","''")).append("|bytes='||length(CAST(").append(cell).append(" AS BLOB)) ELSE ").append(cell).append(" END AS ").append(q);
   }
  }
  if(count==0)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_SCHEMA_ABSENT");return out.toString();
 }
 public static String fieldReference(ReportReadContextCP4 context,String table,String alias,String column){
  for(String s:new String[]{table,alias,column})if(!s.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalArgumentException("REPORT_FIELD_REFERENCE_IDENTIFIER");
  return "'RAW_SQLITE_REFERENCE|snapshot="+context.snapshotSha256()+"|table="+table+"|rowid='||"+alias+".rowid||'|column="+column+"|bytes='||length(CAST("+alias+"."+column+" AS BLOB))";
 }
}
