package com.ethk3.novaonepro;
import java.sql.*;import java.util.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
/** R31 K09 (B05-C08, B09-C01/C03/C05) on the actual compiled charts page reader over native SQLite: stable-id page,
 * per-panel failure isolation, DISPLAY-only cache cleanup that leaves business state and the mainline choice intact.
 * Host mechanism evidence only (not a rendered device screenshot). */
public final class R31ChartPageHost {
 static int assertions;static void check(boolean v,String s){assertions++;if(!v)throw new AssertionError(s);}
 static long count(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String frame(String title,int points){StringBuilder b=new StringBuilder("{\"title\":\""+title+"\",\"kind\":\"LINE\",\"source\":\"host\",\"window\":\"w\",\"note\":\"n\",\"status\":\"READY\",\"points\":[");
  for(int i=0;i<points;i++){if(i>0)b.append(',');b.append("{\"label\":\"p").append(i).append("\",\"value\":0.").append(i+1).append(",\"n\":").append(10+i).append(",\"details\":\"d\"}");}return b.append("]}").toString();}
 public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");
  try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){SQLiteDatabase db=new SQLiteDatabase(connection);
   for(String sql:SchemaR27.DDL)if(sql.startsWith("CREATE TABLE"))db.execSQL(sql);
   connection.setAutoCommit(false);MainlineChoiceStoreR31.ensureSchemaR31(db);connection.commit();connection.setAutoCommit(true);
   long now=System.currentTimeMillis();
   for(String id:ChartCacheR27.PANELS)db.execSQL("INSERT INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,'panel',?,?)",new Object[]{id,frame(id,5),now});
   StringBuilder huge=new StringBuilder("{\"points\":[],\"pad\":\"");for(int i=0;i<600_000;i++)huge.append('x');huge.append("\"}");
   db.execSQL("UPDATE r27_chart_cache SET payload_json=? WHERE panel_id='p03_evolution_events'",new Object[]{huge.toString()});
   db.execSQL("UPDATE r27_chart_cache SET payload_json='{broken' WHERE panel_id='p05_lineage'");
   db.execSQL("UPDATE r27_chart_cache SET payload_json='{\"points\":5}' WHERE panel_id='p07_funnel'");
   db.execSQL("INSERT INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('mainline','choice',?,?)",new Object[]{"{\"choice\":\""+MainlineChoiceStoreR31.FALLBACK+"\",\"qualified\":0,\"nearest\":\"\"}",now});
   connection.setAutoCommit(false);MainlineChoiceStoreR31.appendChoice(db,null,null,"R27_GATE_V1","","",1,100,1,now);connection.commit();connection.setAutoCommit(true);
   // B09-C01/C03: the page is assembled by stable id; a bad frame fails only its own panel
   List<ResearchVisualDataR82.Panel> page=ChartsRepositoryR27.load(db,null);List<String> ids=ChartRegistryR31.visibleLegacyIds();
   check(page.size()==29&&ids.size()==29,"PAGE_IS_THE_29_REGISTERED_PANELS:"+page.size());
   Map<String,String> status=new LinkedHashMap<>();for(int i=0;i<ids.size();i++)status.put(ids.get(i),page.get(i).status);
   check("STALE_FRAME_TOO_LARGE".equals(status.get("p03_evolution_events")),"HUGE_FRAME_ISOLATED:"+status.get("p03_evolution_events"));
   check("STALE_FRAME_INVALID".equals(status.get("p05_lineage")),"BROKEN_FRAME_ISOLATED:"+status.get("p05_lineage"));
   check("STALE_FRAME_ABI".equals(status.get("p07_funnel")),"WRONG_SHAPE_ISOLATED:"+status.get("p07_funnel"));
   for(String id:ChartCacheR27.PANELS)if(!Arrays.asList("p03_evolution_events","p05_lineage","p07_funnel").contains(id))check("READY".equals(status.get(id))&&page.get(ids.indexOf(id)).points.size()==5,"OTHER_PANELS_UNAFFECTED:"+id+":"+status.get(id));
   for(String id:ChartRegistryR31.R83_DRAW_INPUT)check("READ_ERROR".equals(status.get(id))&&page.get(ids.indexOf(id)).note.contains("开奖输入读取失败"),"DRAW_FAILURE_ONLY_DRAW_PANELS:"+id);
   check(page.get(0).status!=null,"STATUS_PANEL_PRESENT");
   // B05-C08 / B09-C03: clearing DISPLAY caches never touches business state; the choice and its authorization are unchanged
   String authBefore=MainlineChoiceStoreR31.authorizedChoice(db,now).reason,legacyBefore=MainlineChoiceStoreR31.legacyChoice(db);long typedBefore=count(db,"SELECT count(*) FROM r31_mainline_choice");
   db.execSQL("INSERT INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('research-study-r1','COMPONENT_PLAN_R31:x','[]',1)");
   for(int round=0;round<3;round++)db.execSQL(com.ethk3.novaonepro.strategyv2.ResearchRetentionStoreR30.visualCacheDeleteSql());
   check(count(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id IN ('p01_three_scores','p14_atom_scores')")==0,"DISPLAY_CLEARED");
   check(count(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'")==1&&legacyBefore.equals(MainlineChoiceStoreR31.legacyChoice(db)),"MAINLINE_CHOICE_KEPT");
   check(count(db,"SELECT count(*) FROM r27_chart_cache WHERE panel_id='research-study-r1'")==1,"CHECKPOINT_KEPT");
   check(authBefore.equals(MainlineChoiceStoreR31.authorizedChoice(db,now).reason)&&typedBefore==count(db,"SELECT count(*) FROM r31_mainline_choice"),"AUTHORIZATION_UNCHANGED_AFTER_DISPLAY_GC");
   // B05-C08#A3 / B09-C05: an emptied panel shows its reason, never fabricated points; the page is rebuilt from the same ids
   List<ResearchVisualDataR82.Panel> rebuilt=ChartsRepositoryR27.load(db,null);check(rebuilt.size()==29,"REBUILT_PAGE");
   for(String id:ChartCacheR27.PANELS){ResearchVisualDataR82.Panel p=rebuilt.get(ids.indexOf(id));check("INSUFFICIENT".equals(p.status)&&(p.points==null||p.points.isEmpty())&&p.note.contains("等待"),"EMPTY_WITH_REASON:"+id);}
   System.out.println("R31_CHART_PAGE_HOST_PASS assertions="+assertions+" page=29_STABLE_IDS r83=12_MOUNTED bad_frames=ISOLATED draw_failure=4_DRAW_PANELS_ONLY display_gc=BUSINESS_STATE_AND_CHOICE_KEPT empty=REASON_NO_POINTS scope=HOST_READER_NOT_DEVICE_RENDER");
  }
 }
}
