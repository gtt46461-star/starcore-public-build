import com.ethk3.novaonepro.strategyv2.*;
/** 把 R30 Java 类里的 SQL 原样导出为 JSON，供 tools/sql_contract_tests.py 在 sqlite3 中执行（保证测的就是施工代码里的字符串）。 */
public class SqlContractDumpR30 {
  static String j(String s){return "\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\"";}
  public static void main(String[] a){
    StringBuilder b=new StringBuilder("{");
    b.append("\"timeline_ddl\":").append(j(IssueTimelineR30.DDL)).append(',');
    b.append("\"timeline_index\":").append(j(IssueTimelineR30.INDEX)).append(',');
    b.append("\"timeline_ensure\":").append(j(IssueTimelineR30.SQL_ENSURE)).append(',');
    b.append("\"timeline_mark\":{");
    IssueTimelineR30.Stage[] st=IssueTimelineR30.Stage.values();
    for(int i=0;i<st.length;i++){if(i>0)b.append(',');b.append(j(st[i].name())).append(':').append(j(IssueTimelineR30.sqlMark(st[i])));}
    b.append("},");
    b.append("\"hold_ddl\":").append(j(HoldLedgerFlushR30.DDL)).append(',');
    b.append("\"hold_insert\":").append(j(HoldLedgerFlushR30.INSERT)).append(',');
    b.append("\"hold_prune\":").append(j(HoldLedgerFlushR30.PRUNE)).append(',');
    b.append("\"hold_report_top\":").append(j(HoldLedgerFlushR30.REPORT_TOP)).append(',');
    b.append("\"hold_report_critical\":").append(j(HoldLedgerFlushR30.REPORT_CRITICAL)).append(',');
    b.append("\"freeze_ddl\":").append(j(OfflineCutoverR30.FREEZE_DDL)).append(',');
    b.append("\"route_ddl\":").append(j(OfflineCutoverR30.ROUTE_DDL)).append(',');
    b.append("\"freeze_trigger_insert\":").append(j(OfflineCutoverR30.freezeTrigger("r27_demo","INSERT"))).append(',');
    b.append("\"freeze_trigger_update\":").append(j(OfflineCutoverR30.freezeTrigger("r27_demo","UPDATE"))).append(',');
    b.append("\"freeze_trigger_delete\":").append(j(OfflineCutoverR30.freezeTrigger("r27_demo","DELETE"))).append(',');
    b.append("\"copy_log_ddl\":").append(j(TableCopierR30.LOG_DDL)).append(',');
    b.append("\"foldback_state_ddl\":").append(j(R29FoldBackR30.STATE_DDL)).append(',');
    b.append("\"foldback_drop_prefixes\":[");for(int i=0;i<R29FoldBackR30.DROP_TRIGGER_PREFIXES.length;i++){if(i>0)b.append(',');b.append(j(R29FoldBackR30.DROP_TRIGGER_PREFIXES[i]));}b.append("],");
    b.append("\"foldback_keep\":[");for(int i=0;i<R29FoldBackR30.KEEP_TRIGGERS.length;i++){if(i>0)b.append(',');b.append(j(R29FoldBackR30.KEEP_TRIGGERS[i]));}b.append("]");
    b.append('}');System.out.println(b);
  }
}
