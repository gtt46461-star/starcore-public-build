package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.LongScaleSummaryR80;
import java.util.*;

/** Real production Root canonical DDL, actual seven-source mirror and LongScale numerical/CAS code.
 * All rows are explicit isolated synthetic controls, never natural training or formal authority receipts. */
public final class CanonicalInputProductionFixtureR29 {
    private CanonicalInputProductionFixtureR29(){}
    static long n(SQLiteDatabase db,String sql){return CriticalInputMirrorR29.scalar(db,sql);}
    static void flag(Map<String,Object> result,String name,boolean ok){CriticalInputProductionTriggerFixtureR29.require(ok,name);result.put(name,ok);}
    static String issue(int value){return String.format(Locale.ROOT,"20261030%04d",value);}
    static Models.Draw draw(int value){Models.Draw d=new Models.Draw();d.issue=issue(value);d.openTime=60000L*value;d.fetchTime=d.openTime+1000;for(int i=0;i<10;i++)d.numbers[i]=i+1;return d;}
    static void insertDraw(SQLiteDatabase db,int value){Models.Draw d=draw(value);db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,draw_hash_v2,dirty) VALUES(?,?,?,1,2,3,4,5,6,7,8,9,10,?,0)",new Object[]{d.issue,d.openTime,d.fetchTime,DrawHashR36.sha256V2(d)});}
    static void canonical(SQLiteDatabase root,int value){Models.Draw d=draw(value);root.execSQL("INSERT INTO "+CriticalInputMirrorR29.CANONICAL_SOURCE+" VALUES(?,'CONFIRMED_CANONICAL_DRAW',?,'SYNTHETIC_CANONICAL_INPUT_CONTROL',2,0,1)",new Object[]{d.issue,DrawHashR36.sha256V2(d)});}
    static long token(SQLiteDatabase db){return n(db,"SELECT generation FROM "+LongScaleSummaryR80.TOKEN+" WHERE singleton=1");}
    static boolean active(SQLiteDatabase db){return n(db,"SELECT count(*) FROM "+LongScaleSummaryR80.HEAD+" WHERE status='ACTIVE'")==1;}
    static LongScaleSummaryR80.Prepared prepare(SQLiteDatabase db){return LongScaleSummaryR80.prepare(db,System.nanoTime()+10_000_000_000L);}
    static void advance(SQLiteDatabase db)throws Exception{LongScaleSummaryR80.Prepared p=prepare(db);CriticalInputProductionTriggerFixtureR29.tx(db,()->LongScaleSummaryR80.commit(db,p));}
    public static Map<String,Object> exercise(SQLiteDatabase root,SQLiteDatabase research)throws Exception{
        Map<String,Object> r=new LinkedHashMap<>();
        CriticalInputProductionTriggerFixtureR29.tx(root,()->{root.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_OOS_ACTION_LOG+"(_id INTEGER PRIMARY KEY AUTOINCREMENT,contract_decision_id TEXT NOT NULL UNIQUE)");DbHelper.ensureR811CriticalSchema(root);for(int i=10;i<=12;i++){insertDraw(root,i);canonical(root,i);}CriticalInputMirrorR29.ensureRealtime(root);});
        // A copied baseline containing six inputs has no canonical alias and must remain non-ready.
        CriticalInputProductionTriggerFixtureR29.tx(research,()->CriticalInputMirrorR29.initialized(research,CriticalInputMirrorR29.sourceEpoch(root),CriticalInputMirrorR29.sourceSequence(root),CriticalInputMirrorR29.sourceViewSql(root)));
        boolean pending=false;try{CriticalInputMirrorR29.ensureCanonicalReady(research);}catch(IllegalStateException expected){pending=String.valueOf(expected.getMessage()).contains("CP3_DB_BUSY");}
        flag(r,"canonicalAliasMissingFromInitialCopyCannotBeReady",pending&&!CriticalInputMirrorR29.tables().contains(CriticalInputMirrorR29.CANONICAL_SOURCE)&&CriticalInputMirrorR29.sourceNames().contains(CriticalInputMirrorR29.CANONICAL_SOURCE));
        CriticalInputProductionTriggerFixtureR29.sync(root,research);CriticalInputMirrorR29.ensureCanonicalReady(research);
        flag(r,"canonicalUsesRealRootRowsDistinctPhysicalAliasAndLogicalReadOnlyView",n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE)==3&&n(research,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE)==3&&ResearchInputMirrorR29.table(research,CriticalInputMirrorR29.CANONICAL_INPUT)&&!ResearchInputMirrorR29.table(research,CriticalInputMirrorR29.CANONICAL_SOURCE));
        boolean physical=false,view=false;try{research.execSQL("UPDATE "+CriticalInputMirrorR29.CANONICAL_INPUT+" SET source_count=99");}catch(RuntimeException expected){physical=String.valueOf(expected.getMessage()).contains("ROOT_AUTHORITY_INPUT_IS_READ_ONLY_REPLICA");}try{research.execSQL("UPDATE "+CriticalInputMirrorR29.CANONICAL_SOURCE+" SET source_count=99");}catch(RuntimeException expected){view=String.valueOf(expected.getMessage()).contains("view");}flag(r,"canonicalPhysicalAndLogicalReplicasRejectLegacyWrites",physical&&view&&n(root,"SELECT max(source_count) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE)==2);
        CriticalInputProductionTriggerFixtureR29.tx(research,()->{
            for(int i=10;i<=12;i++)insertDraw(research,i);
            research.execSQL("INSERT OR REPLACE INTO app_state VALUES('cp12_training_history_generation','1',1)");
            // Exact production revision table DDL from ResearchMigrationRegistryR952.
            research.execSQL("CREATE TABLE IF NOT EXISTS research_row_revision_v2(generation INTEGER PRIMARY KEY AUTOINCREMENT,source_table TEXT NOT NULL,source_rowid INTEGER NOT NULL,UNIQUE(source_table,source_rowid))");
            LongScaleSummaryR80.schema(research);
        });advance(research);
        flag(r,"longScaleActualFiveCanonicalTriggersTargetReplica",n(research,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND tbl_name='"+CriticalInputMirrorR29.CANONICAL_INPUT+"' AND name IN('long_source_token_insert_r80','long_source_token_delete_r80','long_source_token_update_r80','long_canonical_changed_r80','long_canonical_insert_r81')")==5&&active(research));
        long before=token(research);CriticalInputProductionTriggerFixtureR29.tx(root,()->root.execSQL("UPDATE "+CriticalInputMirrorR29.CANONICAL_SOURCE+" SET updated_at=2,source_count=3,evidence_hash='NEW_METADATA_ONLY' WHERE target_issue=?",new Object[]{issue(10)}));CriticalInputProductionTriggerFixtureR29.sync(root,research);
        flag(r,"canonicalMetadataOnlyUpdatePreservesLongScaleHeadAndToken",token(research)==before&&active(research)&&n(research,"SELECT source_count FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE+" WHERE target_issue='"+issue(10)+"'")==3);
        // A repeated complete bootstrap must UPDATE existing canonical rows, never manufacture late inserts.
        CriticalInputProductionTriggerFixtureR29.tx(research,()->CriticalInputMirrorR29.initialized(research,CriticalInputMirrorR29.sourceEpoch(root),CriticalInputMirrorR29.sourceSequence(root),CriticalInputMirrorR29.sourceViewSql(root)));CriticalInputProductionTriggerFixtureR29.sync(root,research);
        flag(r,"canonicalRepeatBootstrapPreservesIdenticalLongScaleHeadAndToken",token(research)==before&&active(research));
        CriticalInputProductionTriggerFixtureR29.tx(root,()->{insertDraw(root,13);canonical(root,13);});CriticalInputProductionTriggerFixtureR29.tx(research,()->insertDraw(research,13));CriticalInputProductionTriggerFixtureR29.sync(root,research);LongScaleSummaryR80.Prepared stale=prepare(research);long beforeCorrection=token(research);
        CriticalInputProductionTriggerFixtureR29.tx(root,()->root.execSQL("UPDATE "+CriticalInputMirrorR29.CANONICAL_SOURCE+" SET status='DISPUTED',canonical_draw_hash=?,updated_at=3 WHERE target_issue=?",new Object[]{StatsR27.sha256("SYNTHETIC_TRUE_CANONICAL_CORRECTION"),issue(10)}));CriticalInputProductionTriggerFixtureR29.sync(root,research);
        boolean cas=false;try{CriticalInputProductionTriggerFixtureR29.tx(research,()->LongScaleSummaryR80.commit(research,stale));}catch(IllegalStateException expected){cas=String.valueOf(expected.getMessage()).contains("LONG_PREDECESSOR_CAS");}
        flag(r,"canonicalRootCorrectionInvalidatesLongScaleTokenHeadAndPreparedCas",token(research)==beforeCorrection+1&&!active(research)&&cas&&n(research,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE+" WHERE status='DISPUTED'")==1);
        CriticalInputProductionTriggerFixtureR29.tx(root,()->root.execSQL("UPDATE "+CriticalInputMirrorR29.CANONICAL_SOURCE+" SET status='CONFIRMED_CANONICAL_DRAW',canonical_draw_hash=? WHERE target_issue=?",new Object[]{DrawHashR36.sha256V2(draw(10)),issue(10)}));CriticalInputProductionTriggerFixtureR29.sync(root,research);advance(research);long beforeInsert=token(research);
        CriticalInputProductionTriggerFixtureR29.tx(root,()->{insertDraw(root,14);canonical(root,14);});CriticalInputProductionTriggerFixtureR29.sync(root,research);
        flag(r,"canonicalNewFutureInsertChangesTokenWithoutInvalidatingAcceptedPrefix",token(research)==beforeInsert+1&&active(research));long beforeDelete=token(research);
        CriticalInputProductionTriggerFixtureR29.tx(root,()->root.execSQL("DELETE FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE+" WHERE target_issue=?",new Object[]{issue(11)}));CriticalInputProductionTriggerFixtureR29.sync(root,research);
        flag(r,"canonicalTrueDeleteRetainsOriginalTokenAndPrefixSemantics",token(research)==beforeDelete+1&&active(research)&&n(research,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE+" WHERE target_issue='"+issue(11)+"'")==0);long beforeLate=token(research);
        CriticalInputProductionTriggerFixtureR29.tx(root,()->canonical(root,11));CriticalInputProductionTriggerFixtureR29.sync(root,research);
        flag(r,"canonicalTrueLateInsertInvalidatesAcceptedPrefix",token(research)==beforeLate+1&&!active(research));
        // Simulate an acknowledged/lost journal prefix: the complete paged source capture, rather than a
        // synthetic delete/insert rebuild, discovers the one truly removed Root key and prunes that replica.
        CriticalInputProductionTriggerFixtureR29.tx(root,()->{root.execSQL("DELETE FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE+" WHERE target_issue=?",new Object[]{issue(11)});CriticalInputMirrorR29.acknowledge(root,CriticalInputMirrorR29.sourceSequence(root));});
        CriticalInputProductionTriggerFixtureR29.sync(root,research);
        flag(r,"canonicalBootstrapPrunesOnlyActuallyMissingRootKeysAfterLostJournalPrefix",CriticalInputMirrorR29.canonicalReady(research)&&n(research,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE+" WHERE target_issue='"+issue(11)+"'")==0&&n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE)==4);
        CriticalInputProductionTriggerFixtureR29.tx(root,()->canonical(root,11));CriticalInputProductionTriggerFixtureR29.sync(root,research);
        // A Root schema-signature change is not accepted by a stale READY cursor.
        CriticalInputProductionTriggerFixtureR29.tx(root,()->{root.execSQL("ALTER TABLE "+CriticalInputMirrorR29.CANONICAL_SOURCE+" ADD COLUMN synthetic_future_abi TEXT");CriticalInputMirrorR29.ensureRealtime(root);});CriticalInputMirrorR29.Step changed=CriticalInputMirrorR29.capture(root,research);boolean migrationBlocked=false;try{CriticalInputProductionTriggerFixtureR29.tx(research,()->CriticalInputMirrorR29.apply(research,changed));}catch(IllegalStateException expected){migrationBlocked=String.valueOf(expected.getMessage()).contains("SCHEMA_MIGRATION_REQUIRED");}
        flag(r,"canonicalRootSchemaChangeForcesCompleteBootstrapOrExplicitAbiMigration",changed.reset&&"BOOTSTRAP".equals(changed.next.phase)&&migrationBlocked&&n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29.CANONICAL_SOURCE)==5);
        return r;
    }
}
