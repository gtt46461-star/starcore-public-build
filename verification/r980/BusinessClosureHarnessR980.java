package com.ethk3.novaonepro;
import android.database.*;
import android.database.sqlite.*;
import java.nio.file.*;
import com.ethk3.novaonepro.r952.*;
import static com.ethk3.novaonepro.EndToEndHarnessR975.*;
/** Extends the actual V3 publisher test into the production canonical/REAL writers,
 * DbStore.saveDraw, sole SettlementEngine, strict OOS labels and next numeric update.
 * Uses a disposable COPY of the synthetic V3 test database; never edits the phone DB.
 * Canonical record input is a test adapter; no Android render or service-start claim. */
public final class BusinessClosureHarnessR980 {
 static int passed;
 static void verify(boolean ok,String label){if(!ok)throw new AssertionError(label);passed++;System.out.println("PASS "+label);}
 static String error(DbStore s)throws Exception { java.lang.reflect.Field f=DatabaseCoordinator.class.getDeclaredField("lastError");f.setAccessible(true);return String.valueOf(f.get(s.coordinatorR36())); }
 static void prepareSchema(SQLiteDatabase db)throws Exception {
  // The older V3 fixture used a four-column terminal table for its warm callbacks.
  // Keep it as test evidence, then initialize the real production schema for NEW draws.
  // This is fixture preparation, NOT an application migration or production DROP.
  db.execSQL("ALTER TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" RENAME TO fixture_warm_terminal_r980");
  // Initialize the actual base-table/column owners: the old numeric fixture had no decisions table.
  DbHelper helper=(DbHelper)HostProductR974.U.allocateInstance(DbHelper.class);
  for(String method:new String[]{"createBaseTables","migrateColumns"}) {
   java.lang.reflect.Method m=DbHelper.class.getDeclaredMethod(method,SQLiteDatabase.class);m.setAccessible(true);m.invoke(helper,db);
  }
  DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);
  db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_OOS_ACTION_LOG+"(id INTEGER PRIMARY KEY)");
  DbHelper.ensureR811CriticalSchema(db);DbHelper.ensureR815CriticalSchema(db);DbHelper.ensureR906CriticalSchema(db);
  RuntimeEventLedgerR926.ensureSchema(db);V51Repository.ensure(db);
  RealtimePressureGateR811.resetForTests();
 }
 static void round(final SQLiteDatabase db,final DbStore store,int ordinal)throws Exception {
  final String target=issue(ordinal),base=issue(ordinal-1);
  try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,target,System.currentTimeMillis())) {
   id=text(db,"SELECT candidate_id FROM r967_frozen_evaluation_v2 ORDER BY created_issue LIMIT 1");
   final V51RuntimeResult runtime=predict(store,ordinal,false);
   final V51Models.PositionAction action=runtime.selected.action;
   final long now=System.currentTimeMillis();
   final String proof=V51ChecksumEngine.sha256("R980_SYNTHETIC_CANONICAL|"+target+'|'+action.checksum+'|'+runtime.authorized.authorizationHash);
   final CanonicalObservationWriterR36.CanonicalRecord record=new CanonicalObservationWriterR36.CanonicalRecord(
       base,target,action.position,action.n1,action.n2,action.n3,-1,0,
       runtime.selected.lane.name(),runtime.authorized.action.name(),runtime.authorized.rejectCode.name(),
       ModelBundleRuntimeR932.current().hash,runtime.selected.contractHash,runtime.authorized.authorizationHash,
       proof,"R980_TEST_"+target,h,h,proof,now,now,now,now+120000);
   DatabaseCoordinator.TransactionResult<Long> result=store.coordinatorR36().runCanonicalTransactionR36(target,"R980_TEST_"+target,
       new DatabaseCoordinator.TransactionCallable<Long>() {
        public Long call(SQLiteDatabase d,DatabaseCoordinator.TransactionContext tx) {
         CanonicalObservationWriterR36.insertWithinTransaction(d,tx,record);
         return store.insertAuthorizedTradeWithinCanonicalR915(d,runtime,now);
        }
       },Long.valueOf(-1));
   verify(result.success&&result.value!=null&&result.value>0,"CANONICAL_AND_AUTHORIZED_REAL_ATOMIC_"+target+":"+result.code);
   CanonicalObservationWriterR36.CanonicalView view=CanonicalObservationWriterR36.readView(store,target);
   verify(view!=null&&view.position==action.position&&view.n1==action.n1&&view.n2==action.n2&&view.n3==action.n3,"UI_DATA_SOURCE_READS_COMMITTED_PREDICTION_"+target);
   long before=n(db,"SELECT COUNT(*) FROM r952_artifact");
   verify(ResearchDirectorR952.capturePredraw(store,target,h,16)==1,"NEXT_LEARNER_FREEZES_BEFORE_LABEL_"+target);
   Thread.sleep(5); // Ordering only; no fake first-seen timestamp or future label is injected.
   Models.Draw draw=new Models.Draw();draw.issue=target;for(int j=0;j<10;j++)draw.numbers[j]=j+1;
   draw.openTime=System.currentTimeMillis();draw.fetchTime=draw.openTime;
   verify(store.saveDraw(draw)>0,"ACTUAL_DRAW_OWNER_VALIDATES_AND_SAVES_"+target);
   SettlementEngine.SettlementReport settlement=SettlementCoordinatorR815.settleNow(store,draw,SettlementCoordinatorR815.Reason.AUTHORITATIVE_LATEST);
   verify(settlement.ok==1&&settlement.settledCount==1,"SOLE_SETTLEMENT_OWNER_CLOSES_REAL_AND_OOS_"+target+":"+settlement.detail+" coordinator="+error(store));
   verify(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue='"+target+"' AND strict_oos=1")==1,"STRICT_OOS_SAMPLE_ACTUALLY_WRITTEN_"+target);
   verify(n(db,"SELECT COUNT(*) FROM trades WHERE target_issue='"+target+"' AND settled=1")==1,"ACTUAL_TRADE_SETTLED_ONCE_"+target);
   SettlementTaskStoreR36.Terminal terminal=SettlementTaskStoreR36.terminal(store,target);
   verify(terminal!=null&&terminal.status.equals("COMPLETED"),"REAL_TERMINAL_RECEIPT_"+target);
   verify(n(db,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE target_issue='"+target+"' AND settled=1 AND proof_error=''")==1
       &&n(db,"SELECT COUNT(*) FROM r952_research_outbox WHERE target_issue='"+target+"' AND state='PENDING'")==1,
       "ACTUAL_COORDINATOR_DISPATCHES_TRAINING_CALLBACK_WITHOUT_MANUAL_HOOK_"+target);
   verify(ResearchDirectorR952.onSettlementCompleted(store,draw,terminal.settlementHash,terminal.completedAt)==0,"DUPLICATE_CALLBACK_IS_IDEMPOTENT_"+target);
   verify(ResearchOutboxR952.drainOne(store)&&PostSettlementResearchLoopR952.drainOne(store),"NEXT_NUMERIC_AND_POST_TRAINING_ACTUALLY_EXECUTE_"+target);
   verify(n(db,"SELECT COUNT(*) FROM r952_artifact")==before+1,"ONE_NEW_NUMERIC_ARTIFACT_NO_FORMAL_LABEL_TRAINING_"+target);
   SettlementEngine.SettlementReport replay=SettlementCoordinatorR815.settleNow(store,draw,SettlementCoordinatorR815.Reason.RECONCILER);
   verify(replay.detail.contains("REPLAY")&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE target_issue='"+target+"'")==1,"DUPLICATE_DRAW_REPLAY_NO_DOUBLE_SAMPLE_"+target);
   view=CanonicalObservationWriterR36.readView(store,target);
   verify(view!=null&&view.settled==1,"UI_DATA_SOURCE_OBSERVES_SETTLED_RESULT_"+target);
  }
 }
 public static void main(String[] args)throws Exception {
  Path source=Path.of(args[1]),copy=Path.of(args[2]);
  if(!Files.isRegularFile(source)||source.toAbsolutePath().equals(copy.toAbsolutePath()))throw new IllegalArgumentException("V3_COPY_INPUT");
  Files.copy(source,copy,StandardCopyOption.REPLACE_EXISTING);
  SQLiteDatabase db=new SQLiteDatabase(args[0],copy.toString());
  try {
   android.os.SystemClock.setForTest(1000000,0);DbStore store=HostProductR974.store(db);
   verify(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE all_gates_pass=1")==1,"INPUT_CERTIFICATE_FROM_ACTUAL_V3_PUBLISHER_NOT_SEEDED_HERE");
   prepareSchema(db);round(db,store,924);round(db,store,925);
   System.out.println("R980_BUSINESS_CLOSURE_PASS checks="+passed+" synthetic_rounds=2 physical_UI=NOT_RUN physical_device=NOT_RUN");
  }finally{db.shutdown();}
 }
}
