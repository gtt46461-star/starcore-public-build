package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.sql.*;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.TargetIssueResolver;
public class PrepareChartInput {
 @SuppressWarnings("unchecked") public static void main(String[] args) throws Exception {
  try (Connection db=DriverManager.getConnection("jdbc:sqlite:" + args[0])) {
  SQLiteDatabase conn=new SQLiteDatabase(db);
  conn.execSQL("CREATE TABLE r30r1_analysis_run(run_id TEXT PRIMARY KEY,dataset_hash TEXT,policy TEXT,role TEXT,source_commit TEXT,revision INTEGER,from_ordinal INTEGER,through_ordinal INTEGER,contract_hash TEXT,status TEXT,summary_json TEXT,updated_at INTEGER)");
  conn.execSQL("CREATE TABLE r30r1_chart_cache(view_id TEXT,frame_id TEXT,run_id TEXT,payload_json TEXT,payload_hash TEXT,updated_at INTEGER)");
  ReplayDatasetR27 random=ReplayDatasetR27.synthetic(5000,333,new int[0]);
  List<ReplayDatasetR27.Row> rows=new ArrayList<>();
  long start=TargetIssueResolver.sequenceOrdinalSU1("202610240001");
  for(int i=0;i<random.size();i++)rows.add(new ReplayDatasetR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(start+i),start+i,random.draws[i]));
  ReplayDatasetR27 data=ReplayDatasetR27.of(rows);
  EvalContextR27 ctx=new EvalContextR27(new FeatureCacheR27(data),SplitR27.of(data,null),null);
  PolicySpecR27 policy=new PolicySpecR27(PolicyAstR27.op("add",PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",2,1))),1,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.01,0,IdeaFamilyR27.meta("I09","ANDROID_SYNTHETIC",0,1,"SYNTHETIC_RENDER_MECHANISM_NOT_NATURAL"));
  List<PolicySpecR27> policies=Arrays.asList(policy,policy.withAst(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,128)),policy.meta));
  int bad=0;
  for(ResearchGraphSpecsR30R1.Spec spec:ResearchGraphSpecsR30R1.ALL) {
   if(!Arrays.asList("R01","R02","R03").contains(spec.id))continue;
   Map<String,Object> frame=ResearchObservatoryR30R1.compute(spec.id,ctx,policies,1);
   frame.put("fixtureScope","SYNTHETIC_PRODUCTION_COMPUTATION_OR_TRUTHFUL_MISSING_ORIGINAL_RECEIPTS; NOT_NATURAL_OR_EFFECT_PROOF");
   ResearchWriteQueueR27.Batch batch=ResearchObservatoryR30R1.product(spec.id,spec.measure,ctx,1,frame,0,4);
   String original=(String)batch.ops.get(0).args[10], hash=(String)batch.ops.get(0).args[8], page=(String)batch.ops.get(2).args[3];
   Map<?,?> s=(Map<?,?>)JsonR27.parse(original),p=(Map<?,?>)JsonR27.parse(page);
   boolean sourceOk=ResearchStoreR30R1.currentSource(BuildConfig.SOURCE_COMMIT_R26,original);
   boolean machineHash=hash.equals(StatsR27.sha256(JsonR27.write(s.get("frame")))),bounded=hash.equals(p.get("fullMachineFrameSha256")),requested=hash.equals(p.get("originalMachineFrameSha256"));
   System.out.println(spec.id+" originalBytes="+original.getBytes(StandardCharsets.UTF_8).length+" pageBytes="+page.getBytes(StandardCharsets.UTF_8).length+" currentSource="+sourceOk+" machineHashMatches="+machineHash+" boundedFieldMatches="+bounded+" readerFieldMatches="+requested);
   if(!sourceOk||!machineHash||!bounded) throw new AssertionError(spec.id);
   for(int index:new int[]{0,2})conn.execSQL(batch.ops.get(index).sql,batch.ops.get(index).args);
  }
  // Android opens the generated file and verifies HEATMAP, LINES and GRAPH with the real blob descriptor.
  if(args.length==2 && "ANDROID_READER".equals(args[1])){ conn.close(); return; }
  Class<?> reader=Class.forName("com.ethk3.novaonepro.ResearchWorkbenchRepositoryR30R1");
  java.lang.reflect.Method load=reader.getDeclaredMethod("load",SQLiteDatabase.class,int.class,android.os.CancellationSignal.class); load.setAccessible(true);
  List<com.ethk3.novaonepro.ResearchVisualDataR82.Panel> panels=(List<com.ethk3.novaonepro.ResearchVisualDataR82.Panel>)load.invoke(null,conn,0,null);
  String[] expected={"HEATMAP","LINES","GRAPH"};for(int i=0;i<3;i++){com.ethk3.novaonepro.ResearchVisualDataR82.Panel v=panels.get(i);System.out.println("R0"+(i+1)+" kind="+v.kind+" points="+v.points.size()+" status="+v.status+" expected="+expected[i]);if(!v.kind.equals(expected[i])||v.points.isEmpty())bad++;}
  if(bad>0)throw new AssertionError("R335_RUNTIME_RESEARCH_CHART_READBACK_MISMATCH="+bad);
  System.out.println("Three production charts persisted for independent Android rendering");}
 }
}
