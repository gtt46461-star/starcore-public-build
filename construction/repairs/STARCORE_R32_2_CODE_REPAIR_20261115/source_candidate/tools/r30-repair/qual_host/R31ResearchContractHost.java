package com.ethk3.novaonepro.strategyv2;
import java.sql.*;import java.util.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.TargetIssueResolver;
/** R31 K07 (B08-C02/C05) on the actual compiled research store, schema and 16 logical views over native SQLite.
 * Mechanism evidence only: synthetic data, no device, no natural quality claim. */
public final class R31ResearchContractHost {
 static int assertions;static void check(boolean v,String s){assertions++;if(!v)throw new AssertionError(s);}
 static long count(SQLiteDatabase d,String sql,String... args){try(Cursor c=d.rawQuery(sql,args.length==0?null:args)){return c.moveToFirst()?c.getLong(0):0;}}
 static void tx(Connection c,SQLiteDatabase d,ResearchWriteQueueR27.Batch b)throws Exception{c.setAutoCommit(false);try{ResearchWriteQueueR27.execute(d,b,"HOST_R31_RESEARCH_CONTRACT");c.commit();}catch(Exception e){c.rollback();throw e;}finally{c.setAutoCommit(true);}}
 static void schema(SQLiteDatabase db,String[] ddl,boolean views){for(String sql:ddl)if(sql.startsWith("CREATE TABLE")||views&&(sql.startsWith("CREATE VIEW")||sql.startsWith("CREATE INDEX")))db.execSQL(sql);}
 public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){SQLiteDatabase db=new SQLiteDatabase(connection);
  for(String[] ddl:new String[][]{SchemaR27.DDL,ResearchSchemaR30R1.DDL,RuntimeSchemaR26.DDL,ModelSurfaceStoreR30.DDL,FrozenConfirmationStoreR30.DDL,DecisionExplanationStoreR30.DDL})schema(db,ddl,false);
  schema(db,AtomPublicationStoreR31.DDL,true);db.execSQL(BehaviorSignatureR30.DDL);db.execSQL(HoldoutLedgerR27.CONFIRM_DDL);
  db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,dirty INTEGER,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT)");
  connection.setAutoCommit(false);SchemaR27.ensureLiveSourceRevision(db);ResearchInputPinR30R1.ensure(db);connection.commit();connection.setAutoCommit(true);
  ReplayDatasetR27 random=ReplayDatasetR27.synthetic(5000,333,new int[0]).planted(.8,444);List<ReplayDatasetR27.Row> rows=new ArrayList<>();long start=TargetIssueResolver.sequenceOrdinalSU1("202610240001");
  for(int i=0;i<random.size();i++)rows.add(new ReplayDatasetR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(start+i),start+i,random.draws[i]));
  ReplayDatasetR27 data=ReplayDatasetR27.of(rows);EvalContextR27 c=new EvalContextR27(new FeatureCacheR27(data),SplitR27.of(data,null),null);ResearchWriteQueueR27.Batch pin=new ResearchWriteQueueR27.Batch();ResearchInputPinR30R1.append(pin,data,0,1);tx(connection,db,pin);
  PolicySpecR27 parent=new PolicySpecR27(PolicyAstR27.op("add",PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32)),PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",2,1))),1,PolicySpecR27.Rule.ALWAYS,0,1,1,false,.01,0,IdeaFamilyR27.meta("I09","HOST_SYNTHETIC",0,1,"MECHANISM_FIXTURE_NOT_NATURAL_QUALITY"));List<PolicySpecR27> ps=Collections.singletonList(parent);
  for(ResearchGraphSpecsR30R1.Spec spec:ResearchGraphSpecsR30R1.ALL){if(spec.id.equals("R06"))continue;Map<String,Object> frame=ResearchObservatoryR30R1.stored(spec.id,c,db,null);if(frame==null)frame=ResearchObservatoryR30R1.compute(spec.id,c,ps,0);tx(connection,db,ResearchObservatoryR30R1.product(spec.id,spec.measure,c,0,frame,123,100));}

  // C05#A1: what R10 wrote is what T09 shows (same run and payload hash); the pre-R31 filter misses it.
  String r10Run=null,r10Payload=null;try(Cursor q=db.rawQuery("SELECT run_id,payload_json FROM r30r1_research_measure WHERE measure_key='R10'",null)){check(q.moveToFirst(),"R10_PRODUCT_MISSING");r10Run=q.getString(0);r10Payload=q.getString(1);}
  check(count(db,"SELECT count(*) FROM r30r1_research_measure WHERE measure_key='R10' AND kind='OPERATOR_GAIN_COST'")==1,"R10_KIND_IS_NOT_OPERATOR_GAIN_COST");
  boolean seen=false;try(Cursor q=db.rawQuery(ResearchTableAdaptersR30R1.query("T09"),null)){while(q.moveToNext())if(r10Run.equals(q.getString(0))){seen=true;check("OPERATOR_GAIN_COST".equals(q.getString(4))&&ResearchMeasureKindR31.OPERATOR_GAIN_COST.unit.equals(q.getString(5))&&"CURRENT".equals(q.getString(6)),"T09_KIND_UNIT_TIER");check(StatsR27.sha256(q.getString(8)).equals(StatsR27.sha256(r10Payload)),"T09_PAYLOAD_HASH");check(q.isNull(7),"T09_TOP_LEVEL_MEAN_GAIN_STAYS_NULL");}}
  check(seen,"T09_DOES_NOT_SHOW_R10");
  String old="SELECT r.run_id FROM r30r1_analysis_run r JOIN r30r1_research_measure m ON m.run_id=r.run_id WHERE m.kind IN ('OPERATOR_COST','PARENT_CHILD_GAIN','RESEARCH_BUDGET') AND r.run_id=?";
  check(count(db,"SELECT count(*) FROM ("+old+")",r10Run)==0,"REGRESSION_FIXTURE_OLD_FILTER_SHOULD_MISS_R10");
  List<ResearchMeasureProjectionR31.ObjectRow> objects=ResearchMeasureProjectionR31.operatorGainCost("OPERATOR_GAIN_COST",r10Payload);for(ResearchMeasureProjectionR31.ObjectRow o:objects)check("UNKNOWN".equals(o.perObjectCpuNs)&&o.nodeCost!=null&&o.nodeCost>0,"R10_OBJECT_COST_UNITS");
  // C05#A2: every logical view prepares and pages on this commit's schema
  int views=0;for(String id:ResearchTableAdaptersR30R1.ids()){for(long offset:new long[]{0,50}){ResearchTableAdaptersR30R1.Page p=ResearchTableAdaptersR30R1.page(id,null,offset,50);try(Cursor q=db.rawQuery(p.sql,p.args)){int n=0;while(q.moveToNext())n++;check(n<=50,"PAGE_BOUND:"+id);}}views++;}
  check(views==16,"SIXTEEN_VIEWS");
  ResearchTableAdaptersR30R1.Page filtered=ResearchTableAdaptersR30R1.page("T01",Collections.singletonMap("dataset_hash",data.datasetHash),0,200);check(count(db,"SELECT count(*) FROM ("+filtered.sql.replace(" LIMIT ? OFFSET ?","")+")",data.datasetHash)>0,"T01_DATASET_FILTER");

  // C02#A2: research reads the original measures, not UI caches
  ResearchContextR30R1 before=ResearchContextR30R1.read(db,c);Set<String> runs=before.sourceRunsR31();check(!runs.isEmpty(),"NO_RESEARCH_INPUT");
  db.execSQL("DELETE FROM r30r1_chart_cache");db.execSQL("DELETE FROM r27_chart_cache");
  ResearchContextR30R1 noUi=ResearchContextR30R1.read(db,c);check(runs.equals(noUi.sourceRunsR31()),"UI_CACHE_DELETION_CHANGED_RESEARCH_INPUT");
  // C02#A3: other revision, other dataset and CONFIRM role are never read
  try(Cursor q=db.rawQuery("SELECT run_id,dataset_hash,policy_hash,role,source_commit,source_revision,from_ordinal,through_ordinal,contract_hash,status,summary_json,recorded_at FROM r30r1_analysis_run WHERE run_id=?",new String[]{runs.iterator().next()})){check(q.moveToFirst(),"RUN_ROW");
   String[][] variants={{"CONFIRM",q.getString(1),String.valueOf(q.getLong(5))},{"SELECT",StatsR27.sha256("other-dataset"),String.valueOf(q.getLong(5))},{"SELECT",q.getString(1),String.valueOf(q.getLong(5)+7)}};int v=0;
   for(String[] x:variants){String id=StatsR27.sha256("variant"+(v++));db.execSQL("INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,x[1],q.getString(2),x[0],q.getString(4),Long.parseLong(x[2]),q.getLong(6),q.getLong(7),q.getString(8),"COMMITTED",q.getString(10),q.getLong(11)+1000});
    db.execSQL("INSERT INTO r30r1_research_measure SELECT ?,measure_key,kind,parent_hash,block_id,from_ordinal,to_ordinal,evaluated_calendar_issues,mean_gain,lower95,upper95,payload_json FROM r30r1_research_measure WHERE run_id=?",new Object[]{id,q.getString(0)});}}
  ResearchContextR30R1 after=ResearchContextR30R1.read(db,c);check(runs.equals(after.sourceRunsR31()),"FOREIGN_ROLE_DATASET_OR_REVISION_WAS_READ");
  // C02#A1: a chart point traces raw rows -> research run -> child -> consume receipt
  after.begin();int[] guided=null;StatsR27.SplitMix rng=new StatsR27.SplitMix(99);for(int i=0;i<100&&guided==null;i++)guided=after.cross(rng,1);check(guided!=null,"CROSS_NOT_CONSUMED");
  PolicySpecR27 child=after.annotate(new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",guided[0],guided[1])),1,PolicySpecR27.Rule.ALWAYS,0,1,1,false,0,0,IdeaFamilyR27.meta("I04","RESEARCH_CROSS",0,1,"ACTUAL_ORIGINAL_MEASURE_INPUT")));
  EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;StoreR27.EvolutionSink sink=new StoreR27.EvolutionSink("HOST_R31",data.issues[data.size()-1]);EvolutionLoopR27 loop=new EvolutionLoopR27(cfg,c.cache,sink,()->1);loop.addPendingForTest(child);loop.researchContext=after;loop.stageResearchHypotheses(sink.batch,101);tx(connection,db,sink.batch);
  StoreR27.EvolutionSink born=new StoreR27.EvolutionSink("HOST_R31",data.issues[data.size()-1]);loop.setSink(born);loop.step=3;loop.step();check("BORN".equals(loop.pendingState(child.policyHash())),"CHILD_NOT_BORN");loop.commitResearchAdmissions(born.batch,102);tx(connection,db,born.batch);
  String source=ResearchContextR30R1.references(child).get(0);
  check(count(db,"SELECT count(*) FROM r30r1_consume c JOIN r30r1_proposal p ON p.proposal_id=c.proposal_id JOIN r30r1_analysis_run r ON r.run_id=p.run_id WHERE c.child_policy_hash=?",child.policyHash())==1,"CHILD_TO_PROPOSAL_TO_RUN");
  check(count(db,"SELECT count(*) FROM r30r1_research_measure WHERE run_id=? AND kind='RESEARCH_SOURCE_CONSUMPTION' AND parent_hash=?",source,child.policyHash())==1,"SOURCE_RUN_TO_CHILD_RECEIPT");
  check(count(db,"SELECT count(*) FROM r30r1_analysis_run WHERE run_id=? AND role='SELECT' AND dataset_hash=? AND from_ordinal>=? AND through_ordinal<=?",source,data.datasetHash,String.valueOf(data.ordinals[0]),String.valueOf(data.ordinals[data.size()-1]))==1,"RUN_TO_RAW_ROW_WINDOW");
  check(after.usagesR31().size()==1&&child.policyHash().equals(after.usagesR31().get(0).get("childHash")),"USAGE_LEDGER");
  System.out.println("R31_RESEARCH_CONTRACT_HOST_PASS assertions="+assertions+" t09=OPERATOR_GAIN_COST_SAME_RUN_AND_HASH old_filter=MISSES views=16_PREPARED_AND_PAGED ui_cache_deleted=RESEARCH_INPUT_UNCHANGED foreign_role_dataset_revision=NOT_READ trace=RAW_RUN_PROPOSAL_CHILD_RECEIPT scope=HOST_SYNTHETIC_NOT_DEVICE_NOT_QUALITY");
 }}
}
