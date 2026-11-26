from pathlib import Path
import subprocess,json,sqlite3
root=Path(__file__).resolve().parent; project=root.parents[2]; src=project/'source-installed/app/src/main/java/com/ethk3/novaonepro/strategyv2'; proposed=root.parent/'after/app/src/main/java/com/ethk3/novaonepro/strategyv2'; stubs=root/'syntax_stubs'; cls=root/'receipt_classes'; cls.mkdir(exist_ok=True)
package='com/ethk3/novaonepro/strategyv2/'
sources={
'android/database/Cursor.java':'''package android.database; public interface Cursor extends AutoCloseable { int FIELD_TYPE_INTEGER=1; boolean moveToNext(); int getColumnCount(); boolean isNull(int i); int getType(int i); long getLong(int i); String getString(int i); String getColumnName(int i); void close(); }''',
'android/database/sqlite/SQLiteDatabase.java':'''package android.database.sqlite; public class SQLiteDatabase { public android.database.Cursor rawQuery(String q,String[] p){throw new UnsupportedOperationException("HOST_NO_ANDROID_DB");} }''',
'com/ethk3/novaonepro/BuildConfig.java':'''package com.ethk3.novaonepro; public final class BuildConfig {public static final String SOURCE_COMMIT_R26="4d1de9d83692635b5c5ce39144fece1c8a31c8ac";}''',
package+'FrozenConfirmationR30.java':'''package com.ethk3.novaonepro.strategyv2; public class FrozenConfirmationR30 {public static String currentSemantics(){return "HOST_FIXTURE_ABI";}}''',
package+'ResearchConsumptionStoreR31.java':'''package com.ethk3.novaonepro.strategyv2; public class ResearchConsumptionStoreR31 {public static final String VERSION="R31_RESEARCH_ACTION_CONSUMER_V1";}''',
package+'PolicySpecR27.java':'''package com.ethk3.novaonepro.strategyv2; public class PolicySpecR27 {public int position; public Meta meta; public static class Meta {public java.util.List<String> parents;} public static PolicySpecR27 parse(String s){throw new UnsupportedOperationException("COMPILE_ONLY_POLICY_STUB_BORN_NOT_HOST_EXECUTED");} public String policyHash(){throw new UnsupportedOperationException();}}''',
package+'ResearchGraphSpecsR30R1.java':'''package com.ethk3.novaonepro.strategyv2; public class ResearchGraphSpecsR30R1 {public static class Spec {public String id;} public static final java.util.List<Spec> ALL=java.util.Collections.emptyList();}''',
package+'MatrixRegistryR30.java':'''package com.ethk3.novaonepro.strategyv2; public class MatrixRegistryR30 {public static class Definition {public String namespace,kind;} public static java.util.List<Definition> all(){return java.util.Collections.emptyList();}}''',
package+'ResearchMeasureKindR31.java':'''package com.ethk3.novaonepro.strategyv2; public class ResearchMeasureKindR31 { public static Object fromViewId(String s){if(!s.matches("R(0[1-9]|1[0-8])"))throw new IllegalArgumentException();return s;} }''',
package+'ResearchGainR30R1.java':'''package com.ethk3.novaonepro.strategyv2; public class ResearchGainR30R1 {public static class Estimate {public double lower95;public boolean supported(){return false;}}}'''
}
for path,text in sources.items(): p=stubs/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
harness=stubs/(package+'ReceiptHostMain.java');harness.write_text('''package com.ethk3.novaonepro.strategyv2;
import java.util.*;import com.ethk3.novaonepro.BuildConfig;
public class ReceiptHostMain {
 static Map<String,Object> m(Object... x){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<x.length;i+=2)m.put((String)x[i],x[i+1]);return m;}
 static String h(String x){return StatsR27.sha256(x);}
 public static void main(String[] args){
  Map<String,Object> c=m("source",BuildConfig.SOURCE_COMMIT_R26,"dataset",h("d"),"revision",3L,"role","SELECT","abi",FrozenConfirmationR30.currentSemantics(),"input",h("input"),"through",100L,"available",1000L);
  String original=JsonR27.write(m("sourceContract",c,"viewId","R01","inputHash",c.get("input"))),run=h("run"),after="{\\\"budget\\\":2}";
  Map<String,Object> row=m("run",run,"source",c.get("source"),"dataset",c.get("dataset"),"revision",3L,"role","SELECT","original",original,"view","R01","kind","BUDGET_APPLIED","product",h(original),"input",c.get("input"),"abi",c.get("abi"),"before_hash",h("before"),"after_hash",h(after),"applied",after,"object_key","budget","budget",1L,"latest_label",100L,"decision_ordinal",101L,"applied_at",1001L,"action_key",h("R33_ACTION1|"+run+"|R01|BUDGET_APPLIED|budget|"+h(after)),"original_views","R01");
  if(!"R01".equals(ReportResearchConsumptionR343.validateAction(row)))throw new AssertionError();
  for(String key:Arrays.asList("source","product","after_hash","action_key","input","abi","original_views")){
   Map<String,Object> bad=new LinkedHashMap<>(row);bad.put(key,"invalid");boolean refused=false;try{ReportResearchConsumptionR343.validateAction(bad);}catch(IllegalArgumentException expected){refused=true;}if(!refused)throw new AssertionError(key);
  }
  Map<String,Object> partial=m("bornLowerBound",0L,"nonBornLowerBound",0L,"bornComplete",true,"nonBornComplete",false);
  if(ReportResearchConsumptionR343.consumed(partial)!=null)throw new AssertionError("partial zero");partial.put("nonBornLowerBound",1L);if(!Boolean.TRUE.equals(ReportResearchConsumptionR343.consumed(partial)))throw new AssertionError("validated lower bound");
  for(double value:new double[]{1.5,Double.NaN,Double.POSITIVE_INFINITY,0x1.0p63}){boolean refused=false;try{ReportResearchConsumptionR343.num(m("n",value),"n");}catch(IllegalArgumentException expected){refused=true;}if(!refused)throw new AssertionError("non-integral number");}
  if(!ReportResearchConsumptionR343.viewKeysPartial(m("original_views","#OVERSIZE_VIEW_KEY#")))throw new AssertionError("view bound");
  System.out.println(JsonR27.write(m("bornSql",ReportResearchConsumptionR343.BORN_SQL,"actionSql",ReportResearchConsumptionR343.ACTION_SQL,"actualValidatorChecks",15,"boundary","Actual new reader and existing JSON/hash/nonBorn/typed contracts compiled; Android DB, ABI provider and PolicySpec are compile fixtures; BORN validator execution remains Android/JUnit gate")));
 }
}''')
files=list(stubs.rglob('*.java'))+[proposed/'ReportResearchConsumptionR343.java']+[src/(x+'.java') for x in ['JsonR27','StatsR27','ResearchActionPolicyR33','ResearchResultContractR31']]
command=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-source','17','-target','17','-d',str(cls)]+list(map(str,files));compile=subprocess.run(command,capture_output=True,text=True);(root/'receipt_compile.log').write_text(compile.stdout+compile.stderr)
if compile.returncode: raise SystemExit(compile.stderr)
run=subprocess.run(['java','-cp',str(cls),'com.ethk3.novaonepro.strategyv2.ReceiptHostMain'],capture_output=True,text=True);(root/'receipt_host.log').write_text(run.stdout+run.stderr)
if run.returncode:raise SystemExit(run.stderr)
payload=json.loads(run.stdout);db=sqlite3.connect(':memory:')
ddl=['CREATE TABLE r30r1_analysis_run(run_id TEXT,source_commit TEXT,dataset_hash TEXT,source_revision INTEGER,role TEXT,status TEXT,summary_json TEXT,recorded_at INTEGER)','CREATE TABLE r30r1_research_measure(run_id TEXT,measure_key TEXT,kind TEXT,parent_hash TEXT,payload_json TEXT)','CREATE TABLE r30r1_consume(proposal_id TEXT,child_policy_hash TEXT,source_generation INTEGER,consumer_version TEXT,receipt_hash TEXT)','CREATE TABLE r30r1_proposal(proposal_id TEXT,state TEXT,payload_json TEXT,payload_hash TEXT)','CREATE TABLE r27_policy(policy_hash TEXT,spec_json TEXT,dataset_hash TEXT,birth_generation INTEGER)','CREATE TABLE r33_research_applied(consume_key TEXT,source_run TEXT,view_id TEXT,action_kind TEXT,role TEXT,dataset_hash TEXT,source_revision INTEGER,source_commit TEXT,source_abi TEXT,input_hash TEXT,product_hash TEXT,before_hash TEXT,after_hash TEXT,object_key TEXT,action_budget INTEGER,action_ordinal INTEGER,latest_label_ordinal INTEGER,applied_json TEXT,applied_at INTEGER)','CREATE TABLE r30r1_chart_cache(view_id TEXT,frame_id TEXT,run_id TEXT,payload_hash TEXT)']
for sql in ddl:db.execute(sql)
commit='4d1de9d83692635b5c5ce39144fece1c8a31c8ac'
for sql in [payload['bornSql'],payload['actionSql']]: assert db.execute(sql,[commit]).fetchall()==[]
db.execute('INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?)',('run',commit,'data',3,'SELECT','COMMITTED','{}',1));db.execute('INSERT INTO r30r1_research_measure VALUES(?,?,?,?,?)',('run','CONSUMED_BY:proposal','RESEARCH_SOURCE_CONSUMPTION','child','{}'));db.execute('INSERT INTO r30r1_research_measure VALUES(?,?,?,?,?)',('run','R01','STRUCTURE_STATE','','{}'));db.execute('INSERT INTO r30r1_consume VALUES(?,?,?,?,?)',('proposal','child',1,'consumer','hash'));db.execute('INSERT INTO r30r1_proposal VALUES(?,?,?,?)',('proposal','CONSUMED','{}','hash'));db.execute('INSERT INTO r27_policy VALUES(?,?,?,?)',('child','{}','data',1))
assert len(db.execute(payload['bornSql'],[commit]).fetchall())==1
assert len(db.execute(payload['actionSql'],[commit]).fetchall())==0
# SQL aggregation is bounded before CursorWindow; oversize/too-many keys become explicit partial markers.
db.execute('INSERT INTO r30r1_research_measure VALUES(?,?,?,?,?)',('run','MATRIX:'+('X'*300000),'','',''))
cur=db.execute(payload['bornSql'],[commit]);record=dict(zip([d[0] for d in cur.description],cur.fetchone()));assert '#OVERSIZE_VIEW_KEY#' in record['original_views'];assert len(record['original_views'])<7000
for i in range(70):db.execute('INSERT INTO r30r1_research_measure VALUES(?,?,?,?,?)',('run','MATRIX:EXTRA'+str(i),'','',''))
cur=db.execute(payload['bornSql'],[commit]);record=dict(zip([d[0] for d in cur.description],cur.fetchone()));assert len(record['original_views'].split('|'))==65;assert len(record['original_views'])<7000

for view in ['R06','R11','R14','R18']:db.execute('INSERT INTO r30r1_chart_cache VALUES(?,?,?,?)',(view,'frame-'+view,'run','a'*64))
for view in ['R06','R11','R14','R18']:
 assert db.execute("SELECT count(*) FROM r30r1_chart_cache f JOIN r30r1_analysis_run r ON r.run_id=f.run_id WHERE f.view_id=? AND length(f.payload_hash)=64 AND r.status='COMMITTED' AND r.source_commit=?",(view,commit)).fetchone()[0]==1
 assert db.execute("SELECT count(*) FROM r30r1_chart_cache f JOIN r30r1_analysis_run r ON r.run_id=f.run_id WHERE f.view_id=? AND length(f.payload_hash)=64 AND r.status='COMMITTED' AND r.source_commit=?",(view,'old-source')).fetchone()[0]==0
out={'actualReaderSyntaxCompile':'PASS_WITH_EXPLICIT_PLATFORM_STUBS','actualNonBornValidatorCases':'PASS_15','exactJavaSqlOnSqlite':'PASS_EMPTY_AND_BORN_JOIN_NO_JSON1','bornNonBornDenominators':'PASS_SEPARATE','sqlOriginalViewAggregation':'PASS_LARGE_TEXT_AND_65_KEYS_BOUND_BEFORE_CURSOR','exactReceiptIntegers':'PASS_FRACTION_NAN_INFINITY_OVERFLOW_REJECTED','fourPreviouslyOmittedViews':'PASS_SOURCE_COMMITTED_FRAME_JOIN_WITHOUT_HEAD','notRun':['Android SQLite and lifecycle read scope','full production PolicySpec BORN validation JUnit','UI rendering all47 panels','full app compile and on-device integration'],'boundary':payload['boundary']};(root/'receipt_verification.json').write_text(json.dumps(out,ensure_ascii=False,indent=2)+'\n');print(json.dumps(out,ensure_ascii=False))
