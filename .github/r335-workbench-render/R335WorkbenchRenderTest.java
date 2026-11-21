package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.content.Context;
import android.os.Looper;
import com.ethk3.novaonepro.r952.ResearchChartViewR82;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.TargetIssueResolver;
@org.junit.runner.RunWith(androidx.test.ext.junit.runners.AndroidJUnit4.class)
public final class R335WorkbenchRenderTest {
 @org.junit.Test @SuppressWarnings("unchecked")
 public void realRenderedPanelsDoNotWriteBirthsOrResearchActions() throws Exception {
  android.app.Instrumentation inst=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation();
  Context context=inst.getTargetContext();
  java.io.File copy=java.io.File.createTempFile("controlled-render-",".db",context.getCacheDir());
  try(java.io.InputStream in=inst.getContext().getAssets().open("r335-generated-render.db");java.io.OutputStream out=new java.io.FileOutputStream(copy)) {
   byte[] buf=new byte[32768];int len;while((len=in.read(buf))!=-1)out.write(buf,0,len);
  }
  try (SQLiteDatabase conn=SQLiteDatabase.openDatabase(copy.getAbsolutePath(),null,SQLiteDatabase.OPEN_READWRITE)) {
  for (String ddl : ResearchActionSchemaR33.DDL) conn.execSQL(ddl);
  conn.execSQL("CREATE TABLE r30r1_proposal(id TEXT PRIMARY KEY)");
  PolicySpecR27 policy=new PolicySpecR27(PolicyAstR27.constant(0.5),1,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.001,.99,
        new PolicySpecR27.Meta("boundary","fixture",Collections.emptyList(),1,7L,"empty-source-guard"));
  String dataset=StatsR27.sha256("controlled-empty-frame");
  int bad=0;
  Class<?> reader=Class.forName("com.ethk3.novaonepro.ResearchWorkbenchRepositoryR30R1");
  java.lang.reflect.Method load=reader.getDeclaredMethod("load",SQLiteDatabase.class,int.class,android.os.CancellationSignal.class); load.setAccessible(true);
  List<com.ethk3.novaonepro.ResearchVisualDataR82.Panel> panels=(List<com.ethk3.novaonepro.ResearchVisualDataR82.Panel>)load.invoke(null,conn,0,null);
  String[] expected={"HEATMAP","LINES","GRAPH"};for(int i=0;i<3;i++){com.ethk3.novaonepro.ResearchVisualDataR82.Panel v=panels.get(i);System.out.println("R0"+(i+1)+" kind="+v.kind+" points="+v.points.size()+" status="+v.status+" expected="+expected[i]);if(!v.kind.equals(expected[i])||v.points.isEmpty())bad++;}
  if(bad>0)throw new AssertionError("R335_RUNTIME_RESEARCH_CHART_READBACK_MISMATCH="+bad);
  int initial = scalar(conn, "SELECT total_changes()");
  int before = scalar(conn, "SELECT count(*) FROM r33_research_applied");
  int bornBefore = scalar(conn, "SELECT count(*) FROM r30r1_proposal");
  if (before!=0 || bornBefore!=0) throw new AssertionError("DIRTY_RESEARCH_FIXTURE");
  final java.util.List<com.ethk3.novaonepro.ResearchVisualDataR82.Panel> nativePanels=panels;
  final int[] renderedCount = {0};
  final Throwable[] drawingFailure = {null};
  inst.runOnMainSync(() -> {try {
  java.lang.reflect.Field counter = ResearchChartViewR82.class.getDeclaredField("drawCount");
  java.lang.reflect.Field error = ResearchChartViewR82.class.getDeclaredField("lastDrawingFailure");
  counter.setAccessible(true);
  error.setAccessible(true);
  for (int i=0; i<3; i++) {
   ResearchChartViewR82 view = new ResearchChartViewR82(context,nativePanels.get(i));
   view.layout(0,0,640,384);
   Bitmap image = Bitmap.createBitmap(640,384,Bitmap.Config.ARGB_8888);
   view.draw(new Canvas(image));
   if (counter.getLong(view)!=1 || error.get(view)!=null) throw new AssertionError("ACTUAL_VIEW_DRAW_FAILED");
   int[] pixels = new int[640*384]; image.getPixels(pixels,0,640,0,0,640,384);
   int different=0; for (int color:pixels) if (color!=pixels[0]) different++;
   if (different<20) throw new AssertionError("EMPTY_NATIVE_BITMAP");
   image.recycle(); renderedCount[0]++;
  }
  } catch(Throwable ex) {drawingFailure[0]=ex;} });
  if(drawingFailure[0]!=null) throw new AssertionError("ACTUAL_DRAW",drawingFailure[0]);
  int rendered=renderedCount[0];
  if(rendered!=3) throw new AssertionError("MISSING_VIEWS");
  if (scalar(conn,"SELECT total_changes()")!=initial
      || scalar(conn,"SELECT count(*) FROM r33_research_applied")!=before
      || scalar(conn,"SELECT count(*) FROM r30r1_proposal")!=bornBefore) throw new AssertionError("RENDER_MUTATED_ACTIONS");
  System.out.println("C04 nativeViews="+rendered+" renderOnlyChanges=0 actionsBefore=0 actionsAfter=0 birthsBefore=0 birthsAfter=0");
  java.lang.reflect.Constructor<ResearchContextR30R1> create = ResearchContextR30R1.class.getDeclaredConstructor(String.class,long.class);
  create.setAccessible(true);
  ResearchContextR30R1 empty = create.newInstance(dataset,1L);
  java.lang.reflect.Field f=ResearchContextR30R1.class.getDeclaredField("frames"), runs=ResearchContextR30R1.class.getDeclaredField("runs");
  f.setAccessible(true);runs.setAccessible(true);
  Map<String,Object> emptyFrame=new LinkedHashMap<>();emptyFrame.put("cells",Collections.emptyList());
  String runHash=StatsR27.sha256("R335-EMPTY-OBSERVED-MEASURES");
  ((Map<String,Object>) f.get(empty)).put("R01",emptyFrame);
  ((Map<String,String>) runs.get(empty)).put("R01",runHash);
  empty.begin();
  for(int i=0;i<64;i++) {
   if(empty.window(new StatsR27.SplitMix(i),32)!=32 || empty.annotate(policy)!=policy)throw new AssertionError("EMPTY_FRAME_MARKED_CONSUMED");
  }
  ResearchWriteQueueR27.Batch emptyBatch=new ResearchWriteQueueR27.Batch();
  if(empty.stageBoundR31(emptyBatch,policy,null,1,10)!=null || !emptyBatch.ops.isEmpty() || emptyBatch.after!=null || emptyBatch.before!=null)
   throw new AssertionError("EMPTY_FRAME_CREATED_ACTION");
  System.out.println("C02 inputFrameCells=0 attributedSources=0 queuedOperations=0 triedSeeds=64");
  conn.execSQL("UPDATE r30r1_chart_cache SET payload_hash='corrupt' WHERE view_id='R01'");
  panels=(List<com.ethk3.novaonepro.ResearchVisualDataR82.Panel>)load.invoke(null,conn,0,null);
  if(!"READ_ERROR".equals(panels.get(0).status))throw new AssertionError("changed chart was accepted");
  System.out.println("ORIGINAL_GRAPH_READER_REJECTED_TAMPER=true");}
 }
 private static int scalar(SQLiteDatabase db,String sql) {
  try (Cursor rows=db.rawQuery(sql,null)) {
   if(!rows.moveToFirst())throw new AssertionError("NO_SQLITE_ROW");
   return rows.getInt(0);
  }
 }
}
