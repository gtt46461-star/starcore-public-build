package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;
public final class LegacyReceiptRaceProofR31 {
 public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");Path folder=Paths.get(args[0]);Files.createDirectories(folder);try(LiveBudgetHostR31.World w=new LiveBudgetHostR31.World(folder.resolve("same-count-race.db"))){
  String id=LiveBudgetHostR31.ID;long base=LiveBudgetHostR31.BASE;w.fixture(256,false,true);w.db.execSQL("UPDATE draws SET open_time=20000,fetch_time=20001");w.db.execSQL("UPDATE r29_qual_calendar SET origin='REGISTERED_BEFORE_FUTURE',registered_at=0");w.qualify(256);
  w.db.execSQL("DELETE FROM r29_qual_receipt WHERE target_ordinal<=?",new Object[]{base+16});w.db.execSQL("INSERT INTO r27_certificate(policy_hash,status,gates_json,evidence_digest,issued_at,expires_at) VALUES(?,'ACTIVE','{}','ORIGINAL',1,999999)",new Object[]{id});
  ResearchWriteQueueR27.Batch b=LegacyCompatibilityHostR31.prepare(w);w.db.execSQL("UPDATE r29_qual_receipt SET digest='RACE_CHANGED_EXISTING_RECEIPT' WHERE target_ordinal=?",new Object[]{base+17});SQLiteDatabase.scenario="LEGACY_SAME_COUNT_RECEIPT_RACE";w.commit("R29_QUAL_LEGACY_COMPAT",b);
  boolean pending=QualificationInboxR29.needsRebuild(w.ro,id),blocked=QualificationInboxR29.blockedForCertificate(w.ro,id);System.out.println("RESULT {\"test\":\"LEGACY_SAME_COUNT_RECEIPT_RACE\",\"needsRebuildAfterCommit\":"+pending+",\"blockedForCertificateAfterCommit\":"+blocked+"}");if(!pending&&!blocked)throw new AssertionError("BASELINE_FAIL_REPRODUCED_LEGACY_RECEIPT_MUTATION_AFTER_PROOF");
 }}
}
