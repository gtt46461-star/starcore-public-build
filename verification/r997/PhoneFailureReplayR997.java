package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.lang.reflect.Constructor;

/** Runs original product methods against a disposable copy of the supplied phone snapshot. */
public final class PhoneFailureReplayR997 {
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0], args[1]);
        try {
            DbStore store = HostProductR974.store(db);
            if (args[2].equals("roster")) {
                String target = TargetIssueResolver.next(store.getLatestDraw().issue).targetIssue;
                try {
                    StagedResearchRosterR943.prepare(store, target, V51ChecksumEngine.sha256("R997_PHONE_REPLAY"));
                    System.out.println("ROSTER_COMPLETED " + target);
                } catch (Throwable failure) {
                    System.out.println("ROSTER_FAILURE " + failure);
                    java.lang.reflect.Field error = DatabaseCoordinator.class.getDeclaredField("lastError");
                    error.setAccessible(true);
                    System.out.println("WRITER_CAUSE " + error.get(store.coordinatorR36()));
                    throw failure;
                }
            } else if (args[2].equals("label")) {
                String target = "202610050541";
                Models.Draw draw = new Models.Draw();
                try (Cursor row = db.rawQuery("SELECT issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10 FROM draws WHERE issue=?",new String[]{target})) {
                    if (!row.moveToFirst()) throw new AssertionError("phone draw missing");
                    draw.issue=row.getString(0); draw.openTime=row.getLong(1); draw.fetchTime=row.getLong(2); draw.dirty=row.getInt(3);
                    for(int i=0;i<10;i++) draw.numbers[i]=row.getInt(i+4);
                }
                Constructor<SettlementEngine.OwnerToken> ctor = SettlementEngine.OwnerToken.class.getDeclaredConstructor(String.class, long.class);
                ctor.setAccessible(true);
                SettlementEngine.OwnerToken token = ctor.newInstance(target, System.currentTimeMillis());
                db.beginTransaction();
                try {
                    OosAfterDrawWriterR36.LabelResult label = OosAfterDrawWriterR36.settleFrozenWithinTransaction(db, token, draw);
                    System.out.println("PHONE_LABEL " + label.summary());
                    if (!label.success) throw new AssertionError("original phone SHADOW observation must settle without inventing a probability");
                    if (label.selectedHitP != -1 || label.brier != -1) throw new AssertionError("unknown probability stays unknown");
                    OosAfterDrawWriterR36.LabelResult replay = OosAfterDrawWriterR36.settleFrozenWithinTransaction(db, token, draw);
                    if (!replay.success || !replay.replay) throw new AssertionError("label replay must be idempotent");
                    db.setTransactionSuccessful();
                } finally { db.endTransaction(); }
            } else if (args[2].equals("recovery")) {
                long eligible=count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION
                        +" s JOIN draws d ON d.issue=s.target_issue AND d.dirty=0 WHERE s.settled=0");
                long realBefore=count(db,"SELECT COUNT(*) FROM trades WHERE action='BET' AND stat_scope='REAL_TRADE'");
                int reopened=0;
                for(int batch=0;batch<10;batch++) {
                    int changed=SettlementTaskStoreR36.reopenRecoverableFailedTerminals(store,64);
                    reopened+=changed;
                    if(changed==0) break;
                }
                System.out.println("RECOVERY_REOPENED "+reopened+" cleanExistingPredraw="+eligible);
                if(reopened!=eligible) throw new AssertionError("only exact eligible phone failures reopened");
                java.util.ArrayList<String> targets=new java.util.ArrayList<>();
                try(Cursor rows=db.rawQuery("SELECT s.target_issue FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION
                        +" s JOIN draws d ON d.issue=s.target_issue AND d.dirty=0 WHERE s.settled=0 ORDER BY s.target_issue",null)) {
                    while(rows.moveToNext()) targets.add(rows.getString(0));
                }
                for(String target:targets) {
                    Models.Draw draw=store.getDrawByIssue(target);
                    if(draw==null) throw new AssertionError("missing clean phone draw "+target);
                    SettlementEngine.SettlementReport settled=new SettlementEngine().settleAfterDrawSaved(store,draw);
                    SettlementTaskStoreR36.Terminal terminal=SettlementTaskStoreR36.terminal(store,target);
                    if(terminal==null||!SettlementTaskStoreR36.COMPLETED.equals(terminal.status)) {
                        java.lang.reflect.Field error=DatabaseCoordinator.class.getDeclaredField("lastError");
                        error.setAccessible(true);System.out.println("WRITER_CAUSE "+error.get(store.coordinatorR36()));
                        throw new AssertionError("real settlement owner failed "+target+" "+settled.detail);
                    }
                    System.out.println("OWNER_COMPLETED "+target+" "+settled.detail);
                }
                if(count(db,"SELECT COUNT(*) FROM trades WHERE action='BET' AND stat_scope='REAL_TRADE'")!=realBefore)
                    throw new AssertionError("SHADOW recovery created REAL");
                if(SettlementTaskStoreR36.reopenRecoverableFailedTerminals(store,64)!=0)
                    throw new AssertionError("recovery not idempotent");
                for(String target:targets) {
                    SettlementEngine.SettlementReport replay=new SettlementEngine().settleAfterDrawSaved(store,store.getDrawByIssue(target));
                    if(replay.ok!=1)throw new AssertionError("settlement replay failed "+target);
                }
                if(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE selected_hit_p=-1 AND brier=-1")!=eligible)
                    throw new AssertionError("unknown probability was fabricated or labels duplicated");
                System.out.println("RECOVERY_OWNER_REPLAY_PASS labels="+eligible+" realBefore="+realBefore
                        +" scope=SUPPLIED_PHONE_DATABASE_COPY_HOST_NOT_ANDROID_DEVICE");
            } else if (args[2].equals("names")) {
                ResearchMethodPlanR906 original;
                try(Cursor c=db.rawQuery("SELECT canonical_spec FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" LIMIT 1",null)) {
                    if(!c.moveToFirst())throw new AssertionError("phone asset absent");
                    original=ResearchMethodPlanR906.parseCanonical(c.getString(0));
                }
                java.util.Map<String,ResearchMethodPlanR906> seen=new java.util.HashMap<>();
                ResearchMethodPlanR906 first=null,second=null;
                for(int i=0;i<100;i++) {
                    ResearchMethodPlanR906 p=new ResearchMethodPlanR906(original.representation,original.feature,
                            original.learner,original.updateRule,original.objective,original.calibration,
                            original.risk,"EXPLICIT_SYNTHETIC_COLLISION_FALSIFIER_"+i,"",997,
                            original.geneticOperator,original.runtimeExecutable);
                    String name=ResearchChineseNamingR906.name(p);
                    if(seen.containsKey(name)){first=seen.get(name);second=p;break;}
                    seen.put(name,p);
                }
                if(first==null||ResearchMethodSignatureR906.of(first).equals(ResearchMethodSignatureR906.of(second)))
                    throw new AssertionError("fixture must contain different methods with same Chinese name");
                db.beginTransaction();
                try {
                    ResearchAssetRegistryR906.Asset a=ResearchAssetRegistryR906.register(db,first,null);
                    ResearchAssetRegistryR906.Asset b=ResearchAssetRegistryR906.register(db,second,null);
                    ResearchAssetRegistryR906.Asset replay=ResearchAssetRegistryR906.register(db,second,null);
                    if(a.assetId.equals(b.assetId)||a.displayNameZh.equals(b.displayNameZh)
                            ||!b.assetId.equals(replay.assetId)||!b.displayNameZh.equals(replay.displayNameZh))
                        throw new AssertionError("different methods distinct; same method replay stable");
                    db.setTransactionSuccessful();
                    System.out.println("NAME_COLLISION_PASS first="+a.displayNameZh+" second="+b.displayNameZh
                            +" scope=EXPLICIT_SYNTHETIC_METHOD_COLLISION_IN_PHONE_SCHEMA_NOT_NEW_RESEARCH_RESULT");
                } finally { db.endTransaction(); }
            }
        } finally { db.shutdown(); }
    }
    private static long count(SQLiteDatabase db,String sql) {
        try(Cursor c=db.rawQuery(sql,null)) { return c.moveToFirst()?c.getLong(0):0; }
    }
}
