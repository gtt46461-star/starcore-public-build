package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import java.util.*;

/** Explicit synthetic original captures on real SQLite and actual Gradle product classes.
 * This is integrity/recovery acceptance, never natural Future, model quality or REAL evidence. */
public final class LegacySettlementAcceptanceR79 {
    static int checks;
    static final String AUDIT=LegacySettlementProofR79.TABLE;
    static String issue(int n){return SettlementProgressR72.issue(n);}
    static String h(String s){return V51ChecksumEngine.sha256(s);}
    static void ok(boolean value,String why){checks++;if(!value)throw new AssertionError(why);System.out.println("PASS "+why);}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static long n(SQLiteDatabase db,String sql){return Long.parseLong(text(db,sql));}
    static String aw(GenesisAssetGenomeR915 g){return " WHERE asset_id='"+g.assetId+"'";}
    static String cw(GenesisAssetGenomeR915 g){return " WHERE candidate_key='genesis:"+g.assetId+"'";}
    static GenesisAssetGenomeR915 asset(long seed){
        AlgorithmGenomeR908 kernel=AlgorithmGenomeR908.seed(seed,1,"EXPLICIT_SYNTHETIC_LEGACY_ACCEPTANCE");
        int[] atoms=new int[8];int k=0;Set<Integer> groups=new HashSet<>();
        for(int i=0;i<GenesisSchemaRegistryR915.atomCount()&&k<8;i++)if(GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()&&groups.add(i/20))atoms[k++]=i;
        if(k!=8)throw new AssertionError("asset atom coverage");
        return new GenesisAssetGenomeR915("R79_SYNTHETIC_"+seed,1,"","SYNTHETIC_TEST_ONLY",issue(1),GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,atoms,kernel);
    }
    static void register(SQLiteDatabase db,GenesisAssetGenomeR915 g){AssetGenesisEngineR915.registerDirected(db,g,issue(1),issue(2));}
    static void fixture(SQLiteDatabase db,GenesisAssetGenomeR915 g,int i,boolean proof){
        db.beginTransaction();try{register(db,g);SettlementProgressR72.assetFreeze(db,g,i,proof);SettlementProgressR72.draw(db,i);db.setTransactionSuccessful();}finally{db.endTransaction();}
    }
    static void mutateProof(SQLiteDatabase db,GenesisAssetGenomeR915 g,String field,Object value)throws Exception{
        String where=cw(g),payload=text(db,"SELECT payload FROM "+CandidateExecutionAuditR932.TABLE+where);
        JSONObject obj=new JSONObject(payload);obj.put(field,value);payload=obj.toString();
        db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET payload=?,proof_hash=?"+where,new Object[]{payload,h(payload)});
    }
    static void negative(SQLiteDatabase db,DbStore store,int i,String mode,String expected)throws Exception{
        GenesisAssetGenomeR915 g=asset(79000+i);fixture(db,g,i,!"NO_PROOF".equals(mode));
        Models.Draw draw=store.getDrawByIssue(issue(i));String where=aw(g),key="genesis:"+g.assetId;
        if("NO_CAPTURE".equals(mode))db.execSQL("DELETE FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key=?",new Object[]{key});
        if("NO_EVENT".equals(mode))db.execSQL("DELETE FROM "+ResearchCaptureLedgerR932.EVENT+" WHERE candidate_key=?",new Object[]{key});
        if("BAD_PAYLOAD".equals(mode))db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET payload='corrupt' WHERE candidate_key=?",new Object[]{key});
        if("BAD_KEY".equals(mode))mutateProof(db,g,"candidate_key","genesis:OTHER_SYNTHETIC");
        if("BAD_TARGET".equals(mode))mutateProof(db,g,"target_issue",issue(i+1));
        if("BAD_ACTION".equals(mode))mutateProof(db,g,"selected_ordinal",1);
        if("BAD_FULL1200".equals(mode))mutateProof(db,g,"action_count",1199);
        if("BAD_ERROR".equals(mode))db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET max_error=0.001 WHERE candidate_key=?",new Object[]{key});
        if("BAD_SHAPE".equals(mode))db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" SET output_blob='SCALAR_SCORE|1|0'"+where);
        if("BAD_INPUT".equals(mode))db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET input_hash=? WHERE candidate_key=?",new Object[]{h("wrong input"),key});
        if("BAD_STRUCTURE".equals(mode))db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET structural_hash=? WHERE candidate_key=?",new Object[]{h("wrong structure"),key});
        if("BAD_RECORD".equals(mode))db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" SET record_hash=?"+where,new Object[]{h("wrong record")});
        if("POSTDRAW_FREEZE".equals(mode))db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" SET frozen_ms=?"+where,new Object[]{draw.openTime});
        if("POSTDRAW_PROOF".equals(mode))db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET frozen_at=? WHERE candidate_key=?",new Object[]{draw.openTime,key});
        if("POSTDRAW_TERMINAL".equals(mode))db.execSQL("UPDATE "+ResearchCaptureLedgerR932.TABLE+" SET updated_at=? WHERE candidate_key=?",new Object[]{draw.openTime,key});
        if("NONCANONICAL".equals(mode))db.execSQL("UPDATE "+DbHelper.TABLE_R811_DRAW_CANONICAL+" SET status='UNCONFIRMED_SYNTHETIC' WHERE target_issue=?",new Object[]{draw.issue});
        if("INVALID_DRAW".equals(mode))draw.numbers[0]=draw.numbers[1];
        if("POSTDRAW_EVENT".equals(mode))db.execSQL("UPDATE "+ResearchCaptureLedgerR932.EVENT+" SET at_ms=? WHERE candidate_key=?",new Object[]{draw.openTime,key});
        if("DIRTY".equals(mode))db.execSQL("UPDATE draws SET dirty=1 WHERE issue=?",new Object[]{draw.issue});
        if("DRAW_CHANGED".equals(mode))db.execSQL("UPDATE draws SET n1=2,n2=1 WHERE issue=?",new Object[]{draw.issue});
        if("SUPPLIED_CHANGED".equals(mode)){draw.numbers[0]=2;draw.numbers[1]=1;}
        if("NO_DRAW".equals(mode))db.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{draw.issue});
        String state=text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where);
        ok(AssetGenesisEngineR915.settleDraw(store,draw)==0,mode+" no label");
        ok(n(db,"SELECT hit_label FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+where)==-1,mode+" original retained");
        ok(state.equals(text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)),mode+" state unchanged");
        ok(n(db,"SELECT state_updates+future_n FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)==0,mode+" no updates/Future");
        ok(expected.equals(text(db,"SELECT reason FROM "+AUDIT+cw(g)+" ORDER BY rowid LIMIT 1")),mode+" exact classification "+expected);
        long receipts=n(db,"SELECT COUNT(*) FROM "+AUDIT+cw(g));
        ok(AssetGenesisEngineR915.settleDraw(HostProductR974.store(db),draw)==0&&receipts==n(db,"SELECT COUNT(*) FROM "+AUDIT+cw(g)),mode+" restart retry idempotent");
        if("POSTDRAW_FREEZE".equals(mode)){
            db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" SET frozen_ms=1"+where);
            ok(AssetGenesisEngineR915.settleDraw(store,draw)==0,"changing a rejected original clock cannot authorize settlement");
            ok("STRICT_PREDRAW_CLOCK_REQUIRED".equals(text(db,"SELECT reason FROM "+AUDIT+cw(g)+" ORDER BY rowid LIMIT 1")),"first original-clock classification stays immutable");
            ok("ORIGINAL_CAPTURE_CHANGED_AFTER_CLASSIFICATION".equals(text(db,"SELECT reason FROM "+AUDIT+cw(g)+" ORDER BY rowid DESC LIMIT 1")),"changed original identity is explicitly rejected");
        }
        if("BAD_INPUT".equals(mode)) {
            db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET input_hash=? WHERE candidate_key=?",new Object[]{h("surface"),key});
            ok(AssetGenesisEngineR915.settleDraw(store,draw)==0,"repairing an originally rejected execution column cannot authorize settlement");
            ok("ORIGINAL_CAPTURE_CHANGED_AFTER_CLASSIFICATION".equals(text(db,"SELECT reason FROM "+AUDIT+cw(g)+" ORDER BY rowid DESC LIMIT 1")),"execution column mutation changes original identity");
        }
        if("NO_PROOF".equals(mode))ok(text(db,"SELECT proof_hash FROM "+AUDIT+cw(g)).isEmpty()&&text(db,"SELECT record_hash FROM "+AUDIT+cw(g)).equals(text(db,"SELECT record_hash FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+where)),"missing proof retains original hash and empty proof identity");
    }
    static void positiveAndRollback(SQLiteDatabase db,DbStore store)throws Exception{
        GenesisAssetGenomeR915 g=asset(79999);db.beginTransaction();try{register(db,g);for(int i=240;i<=245;i++){SettlementProgressR72.assetFreeze(db,g,i);SettlementProgressR72.draw(db,i);}db.setTransactionSuccessful();}finally{db.endTransaction();}
        String where=aw(g);ok(AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(241)))==1,"complete original asset proof settles");
        String state=text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where);
        ok(AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(240)))==1,"older valid label recovered once");
        ok(state.equals(text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where))&&issue(241).equals(text(db,"SELECT trained_until_issue FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)),"late label does not rewind state/watermark");
        ok(n(db,"SELECT state_updates FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)==1&&n(db,"SELECT future_n FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)==2,"exact outcomes distinct from chronological updates");
        long receipts=n(db,"SELECT COUNT(*) FROM "+AUDIT+cw(g));
        ok(AssetGenesisEngineR915.settleDraw(HostProductR974.store(db),store.getDrawByIssue(issue(240)))==0&&receipts==n(db,"SELECT COUNT(*) FROM "+AUDIT+cw(g)),"accepted restart duplicate does not classify/update twice");
        String[] failures={"label","state","summary"};
        for(int j=0;j<failures.length;j++){
            int target=242+j;String lane=failures[j];String table=lane.equals("label")?AssetGenesisEngineR915.TABLE_ASSET_FREEZE:AssetGenesisEngineR915.TABLE_ASSET_GENOME;
            String when="NEW.asset_id='"+g.assetId+"'"+(lane.equals("summary")?" AND NEW.future_n<>OLD.future_n":"");
            db.execSQL("CREATE TRIGGER r79_inject_failure BEFORE UPDATE ON "+table+" WHEN "+when+" BEGIN SELECT RAISE(ABORT,'EXPLICIT_SYNTHETIC_R79_"+lane+"_FAILURE'); END");
            String before=text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where);long count=n(db,"SELECT COUNT(*) FROM "+AUDIT);
            AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(target)));
            ok(n(db,"SELECT hit_label FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+where+" AND target_issue='"+issue(target)+"'")==-1,lane+" failure rolls back label");
            ok(before.equals(text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where))&&count==n(db,"SELECT COUNT(*) FROM "+AUDIT),lane+" failure rolls back state and positive classification");
            db.execSQL("DROP TRIGGER r79_inject_failure");
            ok(AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(target)))==1,lane+" retry commits once");
        }
        db.beginTransaction();try{
            long row=n(db,"SELECT rowid FROM draws WHERE issue='"+issue(245)+"'");DrawDependencyInputCP12.preserveIfReferencedWithinWrite(db,row,issue(245));
            db.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{issue(245)});db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        ok(store.getDrawByIssue(issue(245))!=null&&AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(245)))==1,"retained immutable authoritative draw settles without hot-history expansion");
        ok(n(db,"SELECT COUNT(*) FROM draws WHERE issue='"+issue(245)+"'")==0,"retained draw stays outside hot history");
    }
    static void oldUnprovenHead(SQLiteDatabase db,DbStore store)throws Exception{
        GenesisAssetGenomeR915 g=asset(79800);db.beginTransaction();try{
            register(db,g);SettlementProgressR72.assetFreeze(db,g,260,false);SettlementProgressR72.draw(db,260);
            db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" SET hit_label=1,actual_number=1,outcome_hash=?"+aw(g),new Object[]{h("EXPLICIT_SYNTHETIC_PREEXISTING_UNPROVEN_LABEL")});
            db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" SET state_updates=1,future_n=42,future_hit=42,trained_until_issue=?"+aw(g),new Object[]{issue(260)});
            SettlementProgressR72.assetFreeze(db,g,261);SettlementProgressR72.draw(db,261);db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        String before=text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+aw(g));
        ok(!LegacySettlementProofR79.assetHeadEligible(db,g.assetId),"preexisting settled head is not automatically proven");
        ok(AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(261)))==1,"valid pending record on unproven head gets historical outcome only");
        ok(before.equals(text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+aw(g)))&&n(db,"SELECT state_updates FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+aw(g))==1&&n(db,"SELECT future_n FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+aw(g))==42,"historical-only asset preserves unproven head, does not bless or extend prior metrics");
        ok("HISTORICAL_OUTCOME_ONLY".equals(text(db,"SELECT disposition FROM "+AUDIT+cw(g))),"unproven head outcome disposition is explicit");
        ok(!LegacySettlementProofR79.assetHeadEligible(db,g.assetId),"recovered label cannot unlock unproven head captures");
    }
    static void algorithms(SQLiteDatabase db,DbStore store)throws Exception{
        AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(79777,1,"EXPLICIT_SYNTHETIC_R79_ALGORITHM");
        db.beginTransaction();try{
            AlgorithmInventionLabR908.registerDirected(db,g,issue(1),issue(2),"R79_TEST","SYNTHETIC_ONLY");
            for(int i=279;i<=281;i++){SettlementProgressR72.algorithmFreeze(db,g,i,i!=279);SettlementProgressR72.draw(db,i);}
            db.execSQL("UPDATE "+AlgorithmProofRecoveryR948.HEAD+" SET status='RESET_CLEAN',epoch_start_issue=? WHERE genome_id=?",new Object[]{issue(281),g.genomeId});
            AlgorithmProofRecoveryR948.quarantine(db,g.genomeId,issue(280),"SETTLEMENT_REJECTED:VALID");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        String where=" WHERE genome_id='"+g.genomeId+"'",head=text(db,"SELECT state_blob FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+where);
        long outcomes=n(db,"SELECT COUNT(*) FROM v57_research_candidate_outcome_r811");
        ok(AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(issue(280)))==1,"remote valid pre-epoch recovery preserved");
        ok(head.equals(text(db,"SELECT state_blob FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+where))&&n(db,"SELECT state_updates+future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+where)==0,"pre-epoch no state or current Future");
        ok(ResearchShadowChallengerRuntimeR817.settle(store,store.getDrawByIssue(issue(280)))==1&&n(db,"SELECT COUNT(*) FROM v57_research_candidate_outcome_r811")==outcomes,"historical mirror never enters current feedback");
        ok("SETTLEMENT_REJECTED:VALID".equals(text(db,"SELECT reason FROM "+AlgorithmProofRecoveryR948.QUARANTINE+where+" AND invalid_issue='"+issue(280)+"'")),"old quarantine evidence retained");
        ok(AlgorithmInventionLabR908.settleDraw(HostProductR974.store(db),store.getDrawByIssue(issue(280)))==0&&ResearchShadowChallengerRuntimeR817.settle(store,store.getDrawByIssue(issue(280)))==0,"historical algorithm and mirror restart exactly once");
        ok(AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(issue(281)))==1&&n(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+where)==1&&n(db,"SELECT future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+where)==1,"valid same-epoch training/outcome retained");
        ok(AlgorithmInventionLabR908.settleDraw(store,store.getDrawByIssue(issue(279)))==0,"missing algorithm proof never labels");
        ok("EXECUTION_PROOF_MISSING".equals(text(db,"SELECT reason FROM "+AUDIT+" WHERE candidate_key='algorithm:"+g.genomeId+"' AND target_issue='"+issue(279)+"'")),"missing algorithm proof explicitly classified through existing owner");
    }
    static void currentQuarantine(SQLiteDatabase db,DbStore store)throws Exception {
        AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(79779,1,"EXPLICIT_SYNTHETIC_CURRENT_QUARANTINE");
        GenesisAssetGenomeR915 asset=asset(79779);
        db.beginTransaction();try {
            AlgorithmInventionLabR908.registerDirected(db,g,issue(1),issue(2),"R79_TEST","SYNTHETIC_ONLY");
            register(db,asset);SettlementProgressR72.algorithmFreeze(db,g,201);SettlementProgressR72.assetFreeze(db,asset,201);SettlementProgressR72.draw(db,201);
            AlgorithmProofRecoveryR948.quarantine(db,g.genomeId,issue(201),"EXPLICIT_SYNTHETIC_CURRENT_QUARANTINE");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        Models.Draw draw=store.getDrawByIssue(issue(201));
        ok("VALID".equals(CandidateExecutionAuditR932.verifyAlgorithmSettlementR79(db,g.genomeId,draw)),"current quarantined row retains otherwise valid original proof");
        ResearchPredrawSettlementPumpR908.drainBatch(store,12,1600);
        ok(n(db,"SELECT hit_label FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+aw(asset))==1,"different valid asset admits shared issue through public pump");
        ok(n(db,"SELECT hit_label FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id='"+g.genomeId+"'")==-1,"shared issue cannot bypass algorithm quarantine");
        ok(n(db,"SELECT hit_label FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id='"+g.genomeId+"'")==-1,"shared issue cannot bypass mirror quarantine");
        ok(n(db,"SELECT state_updates+future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+g.genomeId+"'")==0&&n(db,"SELECT COUNT(*) FROM v57_research_candidate_outcome_r811")==0,"quarantined current row adds no state, Future, or feedback");
        ok(AlgorithmInventionLabR908.settleDraw(store,draw)==0&&ResearchShadowChallengerRuntimeR817.settle(HostProductR974.store(db),draw)==0,"direct sidecar/restart retries remain quarantine-blocked");
        ok("EXPLICIT_SYNTHETIC_CURRENT_QUARANTINE".equals(text(db,"SELECT reason FROM "+AlgorithmProofRecoveryR948.QUARANTINE+" WHERE genome_id='"+g.genomeId+"'")),"original quarantine receipt preserved");
    }
    static void mirrorMutation(SQLiteDatabase db,DbStore store)throws Exception {
        AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(79778,1,"EXPLICIT_SYNTHETIC_MIRROR_MUTATION");
        db.beginTransaction();try {
            AlgorithmInventionLabR908.registerDirected(db,g,issue(1),issue(2),"R79_TEST","SYNTHETIC_ONLY");
            SettlementProgressR72.algorithmFreeze(db,g,201);SettlementProgressR72.draw(db,201);
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        Models.Draw draw=store.getDrawByIssue(issue(201));String key="algorithm:"+g.genomeId;
        db.execSQL("UPDATE "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" SET frozen_at=? WHERE candidate_id=?",new Object[]{draw.openTime,g.genomeId});
        ok(ResearchShadowChallengerRuntimeR817.settle(store,draw)==0,"postdraw mirror clock rejected by existing mirror owner");
        ok("STRICT_PREDRAW_CLOCK_REQUIRED".equals(text(db,"SELECT reason FROM "+AUDIT+" WHERE candidate_key='"+key+"' ORDER BY rowid LIMIT 1")),"original mirror clock rejection classified");
        db.execSQL("UPDATE "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" SET frozen_at=1 WHERE candidate_id=?",new Object[]{g.genomeId});
        ok(AlgorithmInventionLabR908.settleDraw(store,draw)==0,"changing rejected mirror clock cannot train algorithm");
        ok(ResearchShadowChallengerRuntimeR817.settle(HostProductR974.store(db),draw)==0,"changing rejected mirror clock cannot label mirror after restart");
        ok(n(db,"SELECT state_updates+future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+g.genomeId+"'")==0,"rejected mirror restoration adds no state or Future");
        ok(n(db,"SELECT COUNT(*) FROM "+AUDIT+" WHERE candidate_key='"+key+"' AND reason='ORIGINAL_CAPTURE_CHANGED_AFTER_CLASSIFICATION'")==2,"both existing owners preserve changed-original rejection");
    }
    static void immutable(SQLiteDatabase db){
        long count=n(db,"SELECT COUNT(*) FROM "+AUDIT);String first=text(db,"SELECT receipt_hash FROM "+AUDIT+" ORDER BY rowid LIMIT 1");
        for(String sql:new String[]{"UPDATE "+AUDIT+" SET reason='REWRITTEN' WHERE receipt_hash='"+first+"'","DELETE FROM "+AUDIT+" WHERE receipt_hash='"+first+"'","INSERT OR REPLACE INTO "+AUDIT+" SELECT * FROM "+AUDIT+" WHERE receipt_hash='"+first+"'"}){
            boolean rejected=false;try{db.execSQL(sql);}catch(RuntimeException expected){rejected=true;}ok(rejected,"immutable receipt rejects "+sql.split(" ")[0]);
        }
        ok(n(db,"SELECT COUNT(*) FROM "+AUDIT)==count,"immutable receipt history preserved");
    }
    static void backlog(SQLiteDatabase db,DbStore store)throws Exception{
        for(int i=201;i<=214;i++)fixture(db,asset(80000+i),i,i==214);
        ResearchPredrawSettlementPumpR908.Result first=ResearchPredrawSettlementPumpR908.drainBatch(store,12,1600);
        ok(first.rows==0,"first bounded page classifies invalid old backlog");
        long committed=n(db,"SELECT COUNT(*) FROM "+AUDIT);
        RealtimePressureGateR811.beginAnalysis();try{ok(ResearchPredrawSettlementPumpR908.drainBatch(store,12,1600).rows==0,"P0 interrupts backlog safely");}finally{RealtimePressureGateR811.endAnalysis();}
        ok(committed==n(db,"SELECT COUNT(*) FROM "+AUDIT),"P0 preserves committed prefix without invented receipts");
        int recovered=0;for(int j=0;j<4&&recovered==0;j++)recovered+=ResearchPredrawSettlementPumpR908.drainBatch(HostProductR974.store(db),12,1600).rows;
        ok(recovered==1,"recreated circular owner reaches valid row after invalid backlog");
        ok(n(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE)==14&&n(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" WHERE hit_label=-1")==13,"backlog originals preserved; only original proven outcome labeled");
        ok(!db.inTransaction(),"P0/restart leaves no transaction running");
    }
    static void migration(SQLiteDatabase db,DbStore store)throws Exception{
        ok(!LegacySettlementProofR79.ready(db),"remote READY schema lacks additive classification");
        db.beginTransaction();try{LegacySettlementProofR79.schema(db);ok(LegacySettlementProofR79.ready(db),"classification visible only in migration transaction");}finally{db.endTransaction();}
        ok(!LegacySettlementProofR79.ready(db),"rollback does not publish false classification readiness");
        for(int i=0;i<128&&!LegacySettlementProofR79.ready(db);i++)ResearchSchemaCoordinatorR926.ensureOneStep(HostProductR974.store(db).coordinatorR36());
        ok(LegacySettlementProofR79.ready(db),"existing Research owner upgrades prior READY schema");
        ok(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"upgraded owner reaches READY");
        ok(ResearchSchemaCoordinatorR926.ensureToReady(HostProductR974.store(db).coordinatorR36(),8),"recreated schema owner is idempotent");
    }
    static GenesisAssetGenomeR915 legacyHead(SQLiteDatabase db,DbStore store,int count)throws Exception {
        GenesisAssetGenomeR915 g=asset(80101);db.beginTransaction();try{register(db,g);
        for(int i=201;i<201+count;i++){SettlementProgressR72.assetFreeze(db,g,i);SettlementProgressR72.draw(db,i);}
        GenesisAssetVmR915.State state=new GenesisAssetVmR915.State();
        // Last pair is deliberately settled newer then older. The original outcome hashes,
        // not issue sorting, prove which update happened and which was only a late label.
        for(int j=0;j<count;j++) {
            int i=201+j;if(j==count-2)i++;else if(j==count-1)i--;
            String where=aw(g)+" AND target_issue='"+issue(i)+"'";
            GenesisAssetVmR915.PredrawContext ctx=GenesisAssetVmR915.PredrawContext.decodeFrozen(text(db,"SELECT context_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+where));
            GenesisAssetVmR915.SettlementResult update=GenesisAssetVmR915.settle(g,state,ctx,issue(i),1,1,.3);
            String outcome=h("R915_ASSET_OUTCOME|"+text(db,"SELECT record_hash FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+where)+'|'+issue(i)+"|1|1|-1|-1.0|"+update.afterHash);
            Thread.sleep(2L);
            db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" SET actual_number=1,hit_label=1,base_hit=-1,brier=-1,settled_ms=?,outcome_hash=?"+where,new Object[]{System.currentTimeMillis(),outcome});
        }
        db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" SET state_blob=?,state_updates=?,trained_until_issue=?,earliest_use_issue=?,future_n=777,future_hit=777"+aw(g),new Object[]{state.encode(),state.updates,state.lastSettledIssue,TargetIssueResolver.next(state.lastSettledIssue).targetIssue});
        db.setTransactionSuccessful();return g;}finally{db.endTransaction();}
    }
    static String headStep(SQLiteDatabase db,GenesisAssetGenomeR915 g,boolean commit) {
        db.beginTransaction();try{String result=LegacySettlementProofR79.auditAssetHead(db,g.assetId);if(commit)db.setTransactionSuccessful();return result;}finally{db.endTransaction();}
    }
    static void headAudit(SQLiteDatabase db,DbStore store,String mode)throws Exception {
        int count="head-long".equals(mode)?270:20;
        GenesisAssetGenomeR915 g=legacyHead(db,store,count);String where=aw(g);
        String originalState=text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where);
        ok(!LegacySettlementProofR79.assetHeadEligible(db,g.assetId),"pre-upgrade settled head starts unverified");
        if("head-owner".equals(mode)) {
            RealtimePressureGateR811.beginAnalysis();try{ok(AssetGenesisEngineR915.auditLegacyHeads(store)==0&&n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_AUDIT)==0,"P0 defers audit without cursor or state writes");}finally{RealtimePressureGateR811.endAnalysis();}
            for(int i=0;i<100&&!LegacySettlementProofR79.assetHeadEligible(db,g.assetId);i++)AssetGenesisEngineR915.auditLegacyHeads(HostProductR974.store(db));
            ok(LegacySettlementProofR79.assetHeadEligible(db,g.assetId),"existing R915 writer owner completes bounded audit across recreated store");
            ok(n(db,"SELECT future_n FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)==0&&originalState.equals(text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)),"owner reconciles stale Future without changing original numerical state");
            long temporary=n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_SNAPSHOT),receipts=n(db,"SELECT COUNT(*) FROM "+AUDIT);
            AssetGenesisEngineR915.auditLegacyHeads(store);
            ok(temporary-n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_SNAPSHOT)==Math.min(8,temporary),"terminal snapshot cleanup releases at most eight temporary copies per owner call");
            for(int i=0;i<10&&n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_SNAPSHOT)>0;i++)AssetGenesisEngineR915.auditLegacyHeads(store);
            ok(n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_SNAPSHOT)==0&&n(db,"SELECT COUNT(*) FROM "+AUDIT)==receipts&&n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_BASELINE)==1&&n(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE)==count,"terminal cleanup preserves all originals, immutable classifications and verified baseline");return;
        }
        if("head-ambiguous".equals(mode))db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" SET settled_ms=(SELECT MIN(settled_ms) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+where+")"+where);
        if("head-state".equals(mode))db.execSQL("UPDATE "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" SET state_blob=?"+where,new Object[]{new GenesisAssetVmR915.State().encode()});
        if("head-proof".equals(mode))db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET input_hash=? WHERE candidate_key=? AND target_issue=?",new Object[]{h("SYNTHETIC_INVALID_ORIGINAL"),"genesis:"+g.assetId,issue(201)});
        headStep(db,g,false);
        ok(n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_AUDIT)==0&&n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_SNAPSHOT)==0,"copy rollback leaves no cursor or snapshot");
        ok("COPYING".equals(headStep(db,g,true)),"first step copies at most eight original labels");
        ok(n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_SNAPSHOT)==8,"bounded snapshot page size");
        db.execSQL("UPDATE draws SET fetch_time=fetch_time WHERE issue=?",new Object[]{issue(1)});
        ok("COPYING".equals(text(db,"SELECT status FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)),"unrelated authoritative draw write leaves active audit unchanged");
        if("head-retained".equals(mode)) {
            db.beginTransaction();try {
                long rowid=n(db,"SELECT rowid FROM draws WHERE issue='"+issue(201)+"'");
                DrawDependencyInputCP12.preserveIfReferencedWithinWrite(db,rowid,issue(201));
                ok(n(db,"SELECT COUNT(*) FROM "+DrawDependencyInputCP12.TABLE+" WHERE issue='"+issue(201)+"'")==1,"active historical audit pins exact settled draw dependency");
                db.execSQL("DELETE FROM draws WHERE issue=?",new Object[]{issue(201)});
                ok(DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32)==0,"active audit dependency cannot be released before replay");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            ok("STALE".equals(text(db,"SELECT status FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)),"hot-to-retained transfer fences and restarts active original audit");
            headStep(db,g,true);
        }
        if("head-canonical-crud".equals(mode)) {
            for(String mutation:new String[]{"UPDATE "+DbHelper.TABLE_R811_DRAW_CANONICAL+" SET status=status WHERE target_issue=?","DELETE FROM "+DbHelper.TABLE_R811_DRAW_CANONICAL+" WHERE target_issue=?"}) {
                db.beginTransaction();try{db.execSQL(mutation,new Object[]{issue(201)});ok("STALE".equals(text(db,"SELECT status FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)),"canonical actual-row mutation invalidates affected active audit");}finally{db.endTransaction();}
                ok("COPYING".equals(text(db,"SELECT status FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)),"canonical rollback restores audit fence and original authority");
            }
            db.beginTransaction();try{
                android.content.ContentValues value=new android.content.ContentValues();
                try(Cursor row=db.rawQuery("SELECT * FROM "+DbHelper.TABLE_R811_DRAW_CANONICAL+" WHERE target_issue=?",new String[]{issue(201)})) {
                    if(!row.moveToFirst())throw new AssertionError("canonical source row");
                    for(int i=0;i<row.getColumnCount();i++)value.put(row.getColumnName(i),row.getString(i));
                }
                value.put("target_issue",issue(999));db.insertOrThrow(DbHelper.TABLE_R811_DRAW_CANONICAL,null,value);
                ok("COPYING".equals(text(db,"SELECT status FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)),"canonical actual insert uses target_issue and leaves unrelated head unchanged");
            }finally{db.endTransaction();}
        }
        if("head-restart".equals(mode)) {
            long gen=n(db,"SELECT generation FROM "+LegacySettlementProofR79.HEAD_AUDIT+where);
            // A valid pending settlement is allowed while the old head is being audited.
            fixture(db,g,221,true);
            ok(AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(221)))==1,"live valid outcome commits while audit is active");
            ok("STALE".equals(text(db,"SELECT status FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)),"candidate mutation invalidates snapshot cursor");
            headStep(db,g,true);
            ok(n(db,"SELECT generation FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)==gen+1,"restart uses a new snapshot generation");
        }
        while("COPYING".equals(text(db,"SELECT status FROM "+LegacySettlementProofR79.HEAD_AUDIT+where)))headStep(db,g,true);
        long receiptsBefore=n(db,"SELECT COUNT(*) FROM "+AUDIT),cursorBefore=n(db,"SELECT cursor_n FROM "+LegacySettlementProofR79.HEAD_AUDIT+where);
        headStep(db,g,false);
        ok(receiptsBefore==n(db,"SELECT COUNT(*) FROM "+AUDIT)&&cursorBefore==n(db,"SELECT cursor_n FROM "+LegacySettlementProofR79.HEAD_AUDIT+where),"replay rollback preserves cursor and immutable classifications atomically");
        String result="";for(int i=0;i<100;i++){result=headStep(db,g,true);if(!"COPYING".equals(result)&&!"RUNNING".equals(result))break;}
        if("head-ambiguous".equals(mode)||"head-state".equals(mode)||"head-proof".equals(mode)) {
            String expected="head-ambiguous".equals(mode)?"SETTLEMENT_ORDER_UNAVAILABLE":"head-state".equals(mode)?"FINAL_STATE_WATERMARK_MISMATCH":"PROOF_INPUT_JOIN_MISMATCH";
            ok(expected.equals(result),"unverifiable legacy chain rejected explicitly: "+expected);
            ok(n(db,"SELECT COUNT(*) FROM "+LegacySettlementProofR79.HEAD_BASELINE)==0&&!LegacySettlementProofR79.assetHeadEligible(db,g.assetId),"failed audit cannot publish eligibility");
            return;
        }
        ok("VERIFIED".equals(result)&&LegacySettlementProofR79.assetHeadEligible(db,g.assetId),"complete original numerical chain recovers head");
        ok(originalState.equals(text(db,"SELECT state_blob FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where))&&n(db,"SELECT state_updates FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)==count-1,"audit preserves state and distinguishes late outcome from update");
        ok(n(db,"SELECT COUNT(*) FROM "+AUDIT+cw(g)+" AND disposition='AUDITED_HISTORICAL_UPDATE'")==count-1&&n(db,"SELECT COUNT(*) FROM "+AUDIT+cw(g)+" AND disposition='AUDITED_HISTORICAL_LATE'")>=1,"immutable historical classifications preserve actual update semantics");
        ok(n(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_FREEZE+" f WHERE "+LegacySettlementProofR79.assetOutcomeEligibleSql("f.asset_id","f.target_issue","f.record_hash"))==0,"audited old outcomes supply no fresh Future");
        boolean immutable=false;try{db.execSQL("UPDATE "+LegacySettlementProofR79.HEAD_BASELINE+" SET update_credit=0");}catch(RuntimeException expected){immutable=true;}ok(immutable,"verified baseline is immutable");
        ok("ALREADY_ELIGIBLE".equals(headStep(db,g,true)),"verified head restart is idempotent");
        if("head-retained".equals(mode)) {
            db.beginTransaction();try{ok(DrawDependencyInputCP12.releaseConsumedWithinWrite(db,32)==1,"terminal audit releases retained input after immutable baseline exists");db.setTransactionSuccessful();}finally{db.endTransaction();}
            ok(n(db,"SELECT COUNT(*) FROM draws WHERE issue='"+issue(201)+"'")==0,"historical audit never expands hot history");return;
        }
        if(!"head-restart".equals(mode))SettlementProgressR72.draw(db,201+count);
        fixture(db,g,202+count,true);
        ok(AssetGenesisEngineR915.settleDraw(store,store.getDrawByIssue(issue(202+count)))==1,"next genuine prospective record trains recovered head");
        ok(n(db,"SELECT state_updates FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)==count&&n(db,"SELECT future_n FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+where)==1,"only post-audit prospective outcome counts as fresh Future");
    }

    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            String mode=args.length>2?args[2]:"all";
            if("migration".equals(mode)){migration(db,store);}
            else {
                for(int i=0;i<128&&!LegacySettlementProofR79.ready(db);i++)ResearchSchemaCoordinatorR926.ensureOneStep(store.coordinatorR36());
                ok(LegacySettlementProofR79.ready(db),"classification schema installed by existing owner");
                for(int i=1;i<=200;i++)SettlementProgressR72.draw(db,i);
                if(mode.startsWith("head-"))headAudit(db,store,mode);
                else if("backlog".equals(mode))backlog(db,store);
                else {
                    String[][] cases={{"NO_PROOF","EXECUTION_PROOF_MISSING"},{"NO_CAPTURE","CAPTURE_TERMINAL_MISSING"},{"NO_EVENT","FROZEN_EVENT_MISSING"},{"BAD_PAYLOAD","PROOF_PAYLOAD_HASH_MISMATCH"},{"BAD_KEY","PROOF_PAYLOAD_IDENTITY_MISMATCH"},{"BAD_TARGET","PROOF_PAYLOAD_IDENTITY_MISMATCH"},{"BAD_ACTION","PROOF_PAYLOAD_IDENTITY_MISMATCH"},{"BAD_FULL1200","PROOF_PAYLOAD_IDENTITY_MISMATCH"},{"BAD_ERROR","FULL1200_TERMINAL_INVALID"},{"BAD_SHAPE","FROZEN_OUTPUT_SHAPE_MISMATCH"},{"BAD_INPUT","PROOF_INPUT_JOIN_MISMATCH"},{"BAD_STRUCTURE","STRUCTURE_JOIN_MISMATCH"},{"BAD_RECORD","FROZEN_RECORD_HASH_MISMATCH"},{"POSTDRAW_FREEZE","STRICT_PREDRAW_CLOCK_REQUIRED"},{"POSTDRAW_PROOF","STRICT_PREDRAW_CLOCK_REQUIRED"},{"POSTDRAW_EVENT","STRICT_CAPTURE_EVENT_CLOCK_REQUIRED"},{"POSTDRAW_TERMINAL","STRICT_CAPTURE_TERMINAL_CLOCK_REQUIRED"},{"NONCANONICAL","AUTHORITATIVE_DRAW_REQUIRED"},{"INVALID_DRAW",""},{"DIRTY","EXACT_DRAW_CLOCK_REQUIRED"},{"DRAW_CHANGED","AUTHORITATIVE_DRAW_VALUES_CHANGED"},{"SUPPLIED_CHANGED","AUTHORITATIVE_DRAW_REQUIRED"},{"NO_DRAW","EXACT_DRAW_CLOCK_REQUIRED"}};
                    // Mutating one scenario's authoritative draw must not poison the next
                    // scenario's CP4 predraw history. The runner copies the sealed schema per case.
                    if(mode.startsWith("negative:")) {
                        int index=Integer.parseInt(mode.substring(9));
                        negative(db,store,201,cases[index][0],cases[index][1]);
                    } else if("current-quarantine".equals(mode)) {
                        currentQuarantine(db,store);immutable(db);
                    } else if("mirror-mutation".equals(mode)) {
                        mirrorMutation(db,store);immutable(db);
                    } else if("positive".equals(mode)) {
                        for(int i=201;i<240;i++)SettlementProgressR72.draw(db,i);
                        positiveAndRollback(db,store);immutable(db);
                    } else if("unproven".equals(mode)) {
                        for(int i=201;i<260;i++)SettlementProgressR72.draw(db,i);
                        oldUnprovenHead(db,store);immutable(db);
                    } else if("algorithms".equals(mode)) {
                        for(int i=201;i<279;i++)SettlementProgressR72.draw(db,i);
                        algorithms(db,store);immutable(db);
                    } else throw new IllegalArgumentException("explicit isolated fixture mode required");
                }
                ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)==0&&n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"zero new certificate/REAL/trade rows");
            }
            System.out.println("{\"status\":\"PASS\",\"mode\":\""+mode+"\",\"checks\":"+checks+",\"scope\":\"ACTUAL_GRADLE_PRODUCT_SYNTHETIC_SQLITE_NO_NATURAL_FUTURE_OR_REAL\"}");
        }
    }
}
