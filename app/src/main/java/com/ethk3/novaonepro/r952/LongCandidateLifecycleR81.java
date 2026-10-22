package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.IOException;
import java.util.*;

/** Bounded derivative registration inside the existing Research Director.
 * Every registered data-only learned foundation has at most one long child per
 * source epoch (five families x the existing forty variants). No original family
 * slot, parent model, Future count, calibration or certificate is reused. */
public final class LongCandidateLifecycleR81 {
    private LongCandidateLifecycleR81() {}
    public static final String CREATION="research_long_creation_r81";
    public static final String SPEC="research_long_spec_source_r81";
    public static final String FROZEN_SOURCE="research_long_frozen_source_r81";
    public static final String SCHEMA_STEP="R81_LONG_FIXED_ACTION_V1";
    private static final String FAMILIES="('LINEAR','LOGISTIC','TREE','NEURAL','HYBRID')";
    private static final Map<String,String> DDL=new LinkedHashMap<>();
    static {
        ddl("CREATE TABLE "+CREATION+"(source_hash TEXT PRIMARY KEY CHECK(length(source_hash)=64),epoch_hash TEXT NOT NULL CHECK(length(epoch_hash)=64),family TEXT NOT NULL CHECK(family IN"+FAMILIES+"),variant INTEGER NOT NULL CHECK(variant BETWEEN 0 AND 39),parent_id TEXT NOT NULL,parent_hash TEXT NOT NULL CHECK(length(parent_hash)=64),ast_hash TEXT NOT NULL CHECK(length(ast_hash)=64),ast TEXT NOT NULL CHECK(length(CAST(ast AS BLOB)) BETWEEN 1 AND 32768),source_issue TEXT NOT NULL,intent_id TEXT NOT NULL,action_id TEXT NOT NULL UNIQUE REFERENCES research_action_v2(action_id),candidate_id TEXT NOT NULL UNIQUE,coverage_hash TEXT NOT NULL CHECK(length(coverage_hash)=64),state TEXT NOT NULL CHECK(state IN('PROPOSED','REGISTERED','NO_OP','HELD')),last_reason TEXT NOT NULL,generation INTEGER NOT NULL CHECK(generation>=0),created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,UNIQUE(epoch_hash,family,variant))");
        ddl("CREATE UNIQUE INDEX r81_one_long_proposal ON "+CREATION+"((1)) WHERE state='PROPOSED'");
        ddl("CREATE TABLE "+SPEC+"(atom_id TEXT PRIMARY KEY REFERENCES research_atom_spec_v2(atom_id),epoch_hash TEXT NOT NULL CHECK(length(epoch_hash)=64),spec_hash TEXT NOT NULL UNIQUE CHECK(length(spec_hash)=64),registered_at INTEGER NOT NULL CHECK(registered_at>0))");
        ddl("CREATE TRIGGER r81_long_creation_identity BEFORE UPDATE ON "+CREATION+" WHEN OLD.source_hash<>NEW.source_hash OR OLD.epoch_hash<>NEW.epoch_hash OR OLD.family<>NEW.family OR OLD.variant<>NEW.variant OR OLD.parent_id<>NEW.parent_id OR OLD.parent_hash<>NEW.parent_hash OR OLD.ast_hash<>NEW.ast_hash OR OLD.ast<>NEW.ast OR OLD.source_issue<>NEW.source_issue OR OLD.intent_id<>NEW.intent_id OR OLD.action_id<>NEW.action_id OR OLD.candidate_id<>NEW.candidate_id OR OLD.coverage_hash<>NEW.coverage_hash OR OLD.created_at<>NEW.created_at BEGIN SELECT RAISE(ABORT,'LONG_CREATION_IMMUTABLE'); END");
        ddl("CREATE TRIGGER r81_long_creation_transition BEFORE UPDATE ON "+CREATION+" WHEN OLD.state<>'PROPOSED' OR NEW.generation<>OLD.generation+1 BEGIN SELECT RAISE(ABORT,'LONG_CREATION_TERMINAL_OR_GENERATION'); END");
        for(String table:new String[]{CREATION,SPEC}) {
            String key=table.equals(CREATION)?"source_hash":"atom_id";
            ddl("CREATE TRIGGER "+table+"_no_replace BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" x WHERE x."+key+"=NEW."+key+") BEGIN SELECT RAISE(ABORT,'LONG_IDENTITY_REPLACE_FORBIDDEN'); END");
            ddl("CREATE TRIGGER "+table+"_no_delete BEFORE DELETE ON "+table+" BEGIN SELECT RAISE(ABORT,'LONG_IDENTITY_PERMANENT'); END");
        }
        ddl("CREATE TABLE "+FROZEN_SOURCE+"(freeze_hash TEXT NOT NULL REFERENCES r967_frozen_evaluation_v2(structural_hash),epoch_hash TEXT NOT NULL CHECK(length(epoch_hash)=64),PRIMARY KEY(freeze_hash,epoch_hash))");
        ddl("CREATE TRIGGER r81_long_frozen_immutable BEFORE UPDATE ON "+FROZEN_SOURCE+" BEGIN SELECT RAISE(ABORT,'LONG_FROZEN_SOURCE_IMMUTABLE'); END");
        ddl("CREATE TRIGGER r81_long_frozen_no_delete BEFORE DELETE ON "+FROZEN_SOURCE+" BEGIN SELECT RAISE(ABORT,'LONG_FROZEN_SOURCE_PERMANENT'); END");
        ddl("CREATE TRIGGER r81_long_frozen_no_replace BEFORE INSERT ON "+FROZEN_SOURCE+" WHEN EXISTS(SELECT 1 FROM "+FROZEN_SOURCE+" x WHERE x.freeze_hash=NEW.freeze_hash AND x.epoch_hash=NEW.epoch_hash) OR (SELECT COUNT(*) FROM "+FROZEN_SOURCE+" WHERE freeze_hash=NEW.freeze_hash)>=16 BEGIN SELECT RAISE(ABORT,'LONG_FROZEN_SOURCE_REPLACE_OR_CAPACITY'); END");
        ddl("CREATE TRIGGER r81_long_spec_immutable BEFORE UPDATE ON "+SPEC+" BEGIN SELECT RAISE(ABORT,'LONG_SPEC_PROVENANCE_IMMUTABLE'); END");
    }
    private static void ddl(String sql){
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("^CREATE (?:TABLE|INDEX|UNIQUE INDEX|TRIGGER) ([A-Za-z_][A-Za-z0-9_]*)[ (]").matcher(sql);
        need(m.find()&&DDL.put(m.group(1),sql)==null,"LONG_REGISTRATION_DDL");
    }
    public static boolean ready(SQLiteDatabase db){
        if(db==null)return false;
        if(TransactionSchemaCacheR941.readyCommitted(db,SCHEMA_STEP))return true;
        for(Map.Entry<String,String> e:DDL.entrySet())try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{e.getKey()})){
            if(!c.moveToFirst()||!e.getValue().equals(c.getString(0))||c.moveToNext())return false;
        }
        TransactionSchemaCacheR941.markCommitted(db,SCHEMA_STEP);return true;
    }
    public static void schema(SQLiteDatabase db){
        need(db!=null&&db.inTransaction(),"LONG_REGISTRATION_SCHEMA_TX");
        for(Map.Entry<String,String> e:DDL.entrySet())try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{e.getKey()})){
            if(c.moveToFirst()){need(e.getValue().equals(c.getString(0)),"LONG_REGISTRATION_SCHEMA_DRIFT:"+e.getKey());continue;}
            db.execSQL(e.getValue());
        }
        need(ready(db),"LONG_REGISTRATION_SCHEMA_NOT_READY");
    }
    private static String latest(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){return c.moveToFirst()?c.getString(0):"";}}
    private static String activeEpoch(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT h.epoch_hash FROM "+LongScaleSummaryR80.HEAD+" h JOIN "+LongScaleSummaryR80.EPOCH+" e ON e.epoch_hash=h.epoch_hash WHERE h.singleton=1 AND h.status='ACTIVE' AND e.protocol_hash=?",new String[]{LongScaleSummaryR80.PROTOCOL})){return c.moveToFirst()?c.getString(0):"";}}
    private static boolean eligible(CandidateAstR952 ast){return ast.formatVersion==3&&ast.graphSpec.nodes.size()<=122&&ast.graphSpec.experts.isEmpty()&&ast.graphSpec.manifest.bindingHashes.isEmpty()&&ast.graphSpec.manifest.summarySpecRefs.isEmpty()&&!MatrixHistoricalDrawCP12.requiresStoredReceiptsCP12(ast,ast.graphSpec.manifest);}
    private static List<AtomSpecCP4> specs(SQLiteDatabase db,CandidateAstR952 ast){
        TreeMap<String,AtomSpecCP4> result=new TreeMap<>();
        for(TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes)if(node.kind==TypedAtomMoleculeGraphR930.NodeKind.ATOM&&!result.containsKey(node.source))try(Cursor c=db.rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{node.source})){
            need(c.moveToFirst(),"LONG_PARENT_SPEC_MISSING");AtomSpecCP4 s=AtomSpecCP4.fromRegistry(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getString(5),c.getString(6));need(!s.state.equals("RETIRED")&&!s.state.equals("DOWNWEIGHTED"),"LONG_PARENT_SPEC_RETIRED");result.put(s.atomId,s);
        }
        return new ArrayList<>(result.values());
    }
    /** Coverage precedes the immutable action. Prepared payloads still undergo
     * full value/hash/availability verification in the original receipt writer. */
    public static final class Coverage {
        public final String epoch,source,firstSource,hash;
        private final long history,canonical;
        private Coverage(String epoch,String source,String first,String hash,long history,long canonical){this.epoch=epoch;this.source=source;firstSource=first;this.hash=hash;this.history=history;this.canonical=canonical;}
    }
    public static Coverage prepareCoverage(DbStore store,CandidateAstR952 parent,String epoch,String source){
        SQLiteDatabase db=store.readable();need(!db.inTransaction()&&eligible(parent)&&TargetIssueResolver.isValid(source),"LONG_COVERAGE_INPUT");
        long history=DevelopmentProductionCP12.historyGeneration(db),canonical=LongScaleSummaryR80.sourceGenerationR81(db);
        need(LongScaleSummaryR80.activeEpoch(db,epoch),"LONG_SOURCE_EPOCH_INVALID");
        List<Models.Draw> rows=DevelopmentProductionCP12.selectHistoryCP12(store.getDrawsBeforeIssue(TargetIssueResolver.next(source).targetIssue,DevelopmentProductionCP12.historyReadLimitR72(parent.graphSpec.manifest.requiredHistory,true)),parent.graphSpec.manifest.requiredHistory,true);
        need(rows.get(rows.size()-1).issue.equals(source),"LONG_COVERAGE_SOURCE_CUTOFF");DevelopmentProductionCP12.verifyDevelopmentHistoryCP12(db,rows,true);
        int start=rows.size()-421;String first=rows.get(start-1).issue,last=rows.get(rows.size()-2).issue,epochFirst="";
        try(Cursor c=db.rawQuery("SELECT first_issue FROM "+LongScaleSummaryR80.EPOCH+" WHERE epoch_hash=? AND protocol_hash=?",new String[]{epoch,LongScaleSummaryR80.PROTOCOL})){need(c.moveToFirst(),"LONG_SOURCE_EPOCH_INVALID");epochFirst=c.getString(0);}
        if(TargetIssueResolver.distance(epochFirst,first)<43199)throw new DevelopmentProductionCP12.Wait("WAIT_LONG_MATURE_COVERAGE");
        String digest=V51ChecksumEngine.sha256("R81_LONG_COVERAGE|"+epoch+'|'+parent.structuralHash+'|'+source);int n=0;
        try(Cursor c=db.rawQuery("SELECT s.through_issue,s.snapshot_hash,s.prepared_at,p.through_issue,p.available_at,p.mode,p.clock_session,p.receipt_hash FROM "+LongScaleSummaryR80.SNAPSHOT+" s JOIN "+LongScaleSummaryR80.PUBLICATION+" p ON p.snapshot_hash=s.snapshot_hash WHERE s.epoch_hash=? AND s.through_issue>=? AND s.through_issue<=? ORDER BY s.through_issue",new String[]{epoch,first,last})){
            while(c.moveToNext()){
                ResearchWorkBudgetR927.checkpoint();if(n>=421||!rows.get(start+n-1).issue.equals(c.getString(0)))throw new DevelopmentProductionCP12.Wait("WAIT_LONG_ORIGINAL_COVERAGE");
                Models.Draw target=rows.get(start+n);String snapshot=c.getString(1),mode=c.getString(5),session=c.getString(6),receipt=c.getString(7);long available=c.getLong(4);
                need(snapshot.matches("[0-9a-f]{64}")&&c.getString(0).equals(c.getString(3))&&c.getLong(2)>0&&c.getLong(2)<=available&&(mode.equals("ORIGINAL_RETURN")||mode.equals("RECOVERED_AVAILABLE_NOW"))&&!session.isEmpty()&&session.length()<=256&&V51ChecksumEngine.sha256("LONG_AVAILABILITY_R80|"+snapshot+'|'+available+'|'+mode+'|'+session).equals(receipt),"LONG_COVERAGE_PUBLICATION_IDENTITY");
                if(available>=target.openTime)throw new DevelopmentProductionCP12.Wait("WAIT_LONG_ORIGINAL_COVERAGE");
                digest=V51ChecksumEngine.sha256(digest+'|'+target.issue+'|'+target.openTime+'|'+snapshot+'|'+receipt);n++;
            }
        }
        if(n!=421)throw new DevelopmentProductionCP12.Wait("WAIT_LONG_ORIGINAL_COVERAGE");
        if(history!=DevelopmentProductionCP12.historyGeneration(db)||canonical!=LongScaleSummaryR80.sourceGenerationR81(db)||!LongScaleSummaryR80.activeEpoch(db,epoch))throw new DevelopmentProductionCP12.Wait("LONG_COVERAGE_SOURCE_CHANGED");
        return new Coverage(epoch,source,first,digest,history,canonical);
    }
    private static final class Creation {
        final String source,epoch,family,parent,parentHash,astHash,encoded,sourceIssue,intent,action,candidate,coverage,state;final int variant;final long generation;
        Creation(Cursor c){source=c.getString(0);epoch=c.getString(1);family=c.getString(2);variant=c.getInt(3);parent=c.getString(4);parentHash=c.getString(5);astHash=c.getString(6);encoded=c.getString(7);sourceIssue=c.getString(8);intent=c.getString(9);action=c.getString(10);candidate=c.getString(11);coverage=c.getString(12);state=c.getString(13);generation=c.getLong(14);}
    }
    private static final String COLUMNS="source_hash,epoch_hash,family,variant,parent_id,parent_hash,ast_hash,ast,source_issue,intent_id,action_id,candidate_id,coverage_hash,state,generation";
    private static Creation pending(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT "+COLUMNS+" FROM "+CREATION+" WHERE state='PROPOSED' LIMIT 2",null)){if(!c.moveToFirst())return null;Creation row=new Creation(c);need(!c.moveToNext(),"LONG_PROPOSAL_CAPACITY");return row;}}
    private static void exactParent(SQLiteDatabase db,String id,CandidateAstR952 parent){try(Cursor c=db.rawQuery("SELECT ast,structural_hash,state FROM r952_candidate WHERE candidate_id=?",new String[]{id})){
        need(c.moveToFirst()&&parent.canonical.equals(c.getString(0))&&parent.structuralHash.equals(c.getString(1))&&!Arrays.asList("QUARANTINED","QUALITY_PRUNED","RESOURCE_RETIRED").contains(c.getString(2)),"LONG_PARENT_IDENTITY_CHANGED");
    }}
    private static void registerSpecs(SQLiteDatabase db,ModernFamilyFactoryCP12.Proposal p,String epoch){
        for(AtomSpecCP4 s:p.specs){
            if(s.requiresSummary){
                need(s.summaryEpochHash.equals(epoch)&&s.state.equals("OBSERVING"),"LONG_DYNAMIC_SPEC_SOURCE");
                try(Cursor old=db.rawQuery("SELECT 1 FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{s.atomId})){if(!old.moveToFirst())db.execSQL("INSERT INTO research_atom_spec_v2(atom_id,family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state) VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{s.atomId,s.family,s.version,s.canonicalSpec,s.position,s.outputType,s.minimumHistory,s.specHash,s.missingPolicy,s.state});}
                try(Cursor old=db.rawQuery("SELECT epoch_hash,spec_hash FROM "+SPEC+" WHERE atom_id=?",new String[]{s.atomId})){if(old.moveToFirst())need(epoch.equals(old.getString(0))&&s.specHash.equals(old.getString(1)),"LONG_DYNAMIC_SPEC_PROVENANCE");else db.execSQL("INSERT INTO "+SPEC+" VALUES(?,?,?,?)",new Object[]{s.atomId,epoch,s.specHash,System.currentTimeMillis()});}
            }
            try(Cursor c=db.rawQuery("SELECT family,spec_version,params_json,position,output_type,min_history,spec_hash,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{s.atomId})){
                need(c.moveToFirst()&&s.family.equals(c.getString(0))&&s.version.equals(c.getString(1))&&s.canonicalSpec.equals(c.getString(2))&&s.position==c.getInt(3)&&s.outputType.equals(c.getString(4))&&s.minimumHistory==c.getInt(5)&&s.specHash.equals(c.getString(6))&&s.missingPolicy.equals(c.getString(7))&&!Arrays.asList("RETIRED","DOWNWEIGHTED").contains(c.getString(8)),"LONG_REGISTER_SPEC_CAS");
            }
        }
    }
    public static String propose(SQLiteDatabase db,ModernFamilyFactoryCP12.Proposal p,CandidateAstR952 parent,String parentId,String family,int variant,Coverage coverage){
        need(db.inTransaction()&&coverage!=null&&variant>=0&&variant<40&&family.equals(parent.family.name()),"LONG_PROPOSAL_INPUT");
        if(coverage.history!=DevelopmentProductionCP12.historyGeneration(db)||coverage.canonical!=LongScaleSummaryR80.sourceGenerationR81(db)||!coverage.source.equals(latest(db))||!LongScaleSummaryR80.activeEpoch(db,coverage.epoch))throw new DevelopmentProductionCP12.Wait("LONG_COVERAGE_COMMIT_CAS");
        exactParent(db,parentId,parent);
        try(Cursor c=db.rawQuery("SELECT ast,ast_hash,candidate_id FROM "+ModernFamilyFactoryCP12.CREATION+" WHERE family=? AND variant=? AND state='REGISTERED'",new String[]{family,Integer.toString(variant)})){need(c.moveToFirst()&&parent.canonical.equals(c.getString(0))&&parent.structuralHash.equals(c.getString(1))&&parentId.equals(c.getString(2)),"LONG_FOUNDATION_SOURCE_CAS");}
        ModernFamilyFactoryCP12.Proposal expected=ModernFamilyFactoryCP12.longSummaryFromDataParentR81(parent,specs(db,parent),coverage.epoch,variant%10);
        need(expected.sourceHash.equals(p.sourceHash)&&expected.ast.canonical.equals(p.ast.canonical),"LONG_PROPOSAL_GRAPH_CAS");
        registerSpecs(db,p,coverage.epoch);
        String evidence=V51ChecksumEngine.sha256("R81_LONG_EVIDENCE|"+p.sourceHash+'|'+p.ast.structuralHash+'|'+coverage.hash),action=V51ChecksumEngine.sha256("R81_LONG_ACTION|"+evidence),finding=V51ChecksumEngine.sha256("R81_LONG_FINDING|"+evidence),candidate="R81-LONG-"+p.ast.structuralHash.substring(0,24),nowIssue=coverage.source;long now=System.currentTimeMillis();
        String intent=ResearchIntentLedgerR952.put(db,"LONG_SUMMARY_CHILD_R81",p.ast.canonical,"UNTRAINED_NO_FUTURE","INDEPENDENT_DEVELOPMENT_AND_FUTURE",family,1,nowIssue);
        db.execSQL("INSERT INTO research_finding_v2(finding_id,game_code,candidate_version,source_issue,episode_start_issue,problem_type,severity,evidence_hash,evidence_json,hypothesis,falsification_test,state,created_at,updated_at) VALUES(?,'PK10',?,?,?,'LONG_SUMMARY_CHILD_R81','P2',?,?,?,'DEVELOPMENT_AND_PROSPECTIVE_FUTURE','PLANNED',?,?)",new Object[]{finding,candidate,nowIssue,nowIssue,evidence,p.ast.canonical,"Preregistered long-frequency and entropy inputs may improve independent performance",now,now});
        db.execSQL("INSERT INTO research_action_v2(action_id,finding_id,action_type,operator,proposal_json,evidence_hash,seed,parent_hashes_json,protocol_hash,planned_endpoint,alpha_identity,state) VALUES(?,?,'NEW_FAMILY','CREATE',?,?,0,?,?,360,'','PROPOSED')",new Object[]{action,finding,StrictCanonicalJson.write(fields("source","LONG_SUMMARY_CHILD_R81","epochHash",coverage.epoch,"coverageHash",coverage.hash)),evidence,StrictCanonicalJson.writeContext(Collections.singletonList(parent.structuralHash)),V51ChecksumEngine.sha256("R81_LONG_EXPERIMENT|"+evidence)});
        String boundary=TargetIssueResolver.next(nowIssue).targetIssue;
        db.execSQL("INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,?)",new Object[]{action,nowIssue,boundary,evidence,V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|"+action+'|'+nowIssue+'|'+boundary+'|'+evidence),now});
        db.execSQL("INSERT INTO "+CREATION+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,'PROPOSED','',0,?,?)",new Object[]{p.sourceHash,coverage.epoch,family,variant,parentId,parent.structuralHash,p.ast.structuralHash,p.ast.canonical,nowIssue,intent,action,candidate,coverage.hash,now,now});
        return candidate;
    }
    private static void update(SQLiteDatabase db,Creation c,String state,String reason){db.execSQL("UPDATE "+CREATION+" SET state=?,last_reason=?,generation=generation+1,updated_at=? WHERE source_hash=? AND state='PROPOSED' AND generation=?",new Object[]{state,reason,System.currentTimeMillis(),c.source,c.generation});need(changes(db)==1,"LONG_CREATION_CAS");}
    private static String hold(DbStore store,Creation c,String reason){
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R81_LONG_FIXED_INPUT_HOLD",db->{
            db.execSQL("UPDATE research_action_v2 SET state='REJECTED',last_error=? WHERE action_id=? AND state='PROPOSED'",new Object[]{reason,c.action});need(changes(db)==1,"LONG_ACTION_HOLD_CAS");
            db.execSQL("UPDATE research_finding_v2 SET state='HOLD',updated_at=? WHERE finding_id=(SELECT finding_id FROM research_action_v2 WHERE action_id=?) AND state='PLANNED'",new Object[]{System.currentTimeMillis(),c.action});need(changes(db)==1,"LONG_FINDING_HOLD_CAS");update(db,c,"HELD",reason);return true;
        });return "LONG_HELD:"+reason;
    }
    private static String fixedFailure(Throwable error){for(Throwable c=error;c!=null;c=c.getCause()){
        String code=c instanceof DevelopmentProductionCP12.Wait?((DevelopmentProductionCP12.Wait)c).reason:c.getMessage();
        if(Arrays.asList("WAIT_HISTORY","STALE_HISTORY","WAIT_LONG_ORIGINAL_SUMMARY","WAIT_LONG_HISTORICAL_SOURCE_LATE","WAIT_LONG_SCALE_HISTORY","WAIT_BOUND_FEATURE_HISTORY","LONG_SOURCE_EPOCH_INVALID","LONG_PARENT_IDENTITY_CHANGED","REGISTER_PARENT_CAS").contains(code))return code;
        if(code!=null&&(code.startsWith("LONG_SNAPSHOT_")||code.startsWith("LONG_PUBLICATION_")||code.startsWith("LONG_FROZEN_")||code.startsWith("TRAIN_BOUND_")||code.startsWith("TRAIN_REGISTRATION_ROW_")))return code.length()>256?code.substring(0,256):code;
    }return ModernFamilyFactoryCP12.terminalPreparationReasonCP12(error);}
    private static String resume(DbStore store,Creation c){
        SQLiteDatabase db=store.readable();if(!LongScaleSummaryR80.activeEpoch(db,c.epoch))return hold(store,c,"LONG_SOURCE_EPOCH_INVALID");
        try {
            CandidateAstR952 ast=CandidateAstR952.parse(c.encoded);need(ast.structuralHash.equals(c.astHash),"LONG_CREATION_AST_HASH");
            String parentText;try(Cursor parent=db.rawQuery("SELECT ast FROM r952_candidate WHERE candidate_id=? AND structural_hash=?",new String[]{c.parent,c.parentHash})){need(parent.moveToFirst(),"LONG_PARENT_IDENTITY_CHANGED");parentText=parent.getString(0);}exactParent(db,c.parent,CandidateAstR952.parse(parentText));
            String target=TargetIssueResolver.next(latest(db)).targetIssue;
            try(DevelopmentProductionCP12.PreparedProtocol protocol=ResearchDirectorR952.prepareTrainingProtocolCP5(store,ast,c.candidate,c.action,target)){
                String evidence;try(Cursor action=db.rawQuery("SELECT evidence_hash FROM research_action_v2 WHERE action_id=? AND state='PROPOSED'",new String[]{c.action})){need(action.moveToFirst(),"LONG_ACTION_IDENTITY");evidence=action.getString(0);}
                ResearchDirectorR952.PreparedEvolution child=new ResearchDirectorR952.PreparedEvolution(c.candidate,c.action,c.parent,c.intent,c.parentHash,evidence);
                return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R81_LONG_REGISTER",tx->{
                    String result=ResearchDirectorR952.commitPreparedCandidateCP5(tx,protocol,child);update(tx,c,result.startsWith("NO_OP:")?"NO_OP":"REGISTERED","");
                    if(result.startsWith("NO_OP:"))tx.execSQL("UPDATE research_finding_v2 SET state='HOLD',updated_at=? WHERE finding_id=(SELECT finding_id FROM research_action_v2 WHERE action_id=?) AND state='PLANNED'",new Object[]{System.currentTimeMillis(),c.action});return result;
                });
            }
        }catch(ResearchWorkBudgetR927.Yield|RuntimeResourceAuditR927.BudgetExceeded yielded){throw yielded;}
        catch(RuntimeException failure){String terminal=fixedFailure(failure);if(terminal!=null)return hold(store,c,terminal);
            if(failure instanceof DevelopmentProductionCP12.Wait)return "LONG_WAIT:"+((DevelopmentProductionCP12.Wait)failure).reason;
            if("ALGORITHM_MEMORY_BUDGET".equals(failure.getMessage()))return "LONG_WAIT:WAIT_RESOURCE";throw failure;
        }catch(IOException io){return "LONG_WAIT:WAIT_INPUT_IO";}
    }
    /** One existing RESEARCH subturn performs one bounded unit. */
    public static String maintain(DbStore store,long deadlineNs){
        if(store==null||RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive()||System.nanoTime()+75_000_000L>=deadlineNs)return "LONG_YIELD";
        SQLiteDatabase db=store.readable();need(!db.inTransaction()&&ready(db)&&LongScaleSummaryR80.ready(db),"LONG_LIFECYCLE_SCHEMA_NOT_READY");
        Creation existing=pending(db);if(existing!=null)return resume(store,existing);
        if(!ResearchDirectorR952.trainingQueueHasCapacityR65(db))return "LONG_WAIT:TRAIN_INPUT_CAPACITY";
        String epoch=activeEpoch(db),source=latest(db);if(epoch.isEmpty()||!TargetIssueResolver.isValid(source))return "LONG_WAIT:SOURCE";
        String cursor=store.getState("r81_long_foundation_cursor",""),after=cursor.startsWith(epoch+"|")?cursor.substring(65):"";
        String[] selected=null;String scanned=after;
        try(Cursor c=db.rawQuery("SELECT f.family,f.variant,p.candidate_id,p.ast,p.structural_hash FROM "+ModernFamilyFactoryCP12.CREATION+" f JOIN r952_candidate p ON p.candidate_id=f.candidate_id AND p.ast=f.ast AND p.structural_hash=f.ast_hash WHERE f.state='REGISTERED' AND f.family IN"+FAMILIES+" AND p.state NOT IN('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED') AND (f.family||printf('%02d',f.variant))>? AND NOT EXISTS(SELECT 1 FROM "+CREATION+" l WHERE l.epoch_hash=? AND l.family=f.family AND l.variant=f.variant) ORDER BY f.family,f.variant LIMIT 16",new String[]{after,epoch})){
            while(c.moveToNext()){ResearchWorkBudgetR927.checkpoint();scanned=c.getString(0)+String.format(java.util.Locale.ROOT,"%02d",c.getInt(1));CandidateAstR952 parent=CandidateAstR952.parse(c.getString(3));if(eligible(parent)){selected=new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)};break;}}
        }
        if(selected==null){store.setState("r81_long_foundation_cursor",epoch+"|"+(scanned.equals(after)?"":scanned));return "LONG_WAIT:ELIGIBLE_FOUNDATION";}
        final String[] row=selected;final CandidateAstR952 parent=CandidateAstR952.parse(row[3]);
        try(MemoryBudgetCP4.Lease scratch=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(4L<<20)){
            Coverage coverage=prepareCoverage(store,parent,epoch,source);ModernFamilyFactoryCP12.Proposal p=ModernFamilyFactoryCP12.longSummaryFromDataParentR81(parent,specs(db,parent),epoch,Integer.parseInt(row[1])%10);
            if(System.nanoTime()+75_000_000L>=deadlineNs)return "LONG_YIELD";
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R81_LONG_PROPOSE",tx->{String id=propose(tx,p,parent,row[2],row[0],Integer.parseInt(row[1]),coverage);tx.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r81_long_foundation_cursor',?,?)",new Object[]{epoch+'|'+row[0]+String.format(java.util.Locale.ROOT,"%02d",Integer.parseInt(row[1])),System.currentTimeMillis()});return id;});return "LONG_PROPOSAL_READY";
        }catch(ResearchWorkBudgetR927.Yield|RuntimeResourceAuditR927.BudgetExceeded yielded){throw yielded;}
        catch(RuntimeException failure){
            if("ALGORITHM_MEMORY_BUDGET".equals(failure.getMessage()))return "LONG_WAIT:WAIT_RESOURCE";
            for(Throwable cause=failure;cause!=null;cause=cause.getCause())if(cause instanceof DevelopmentProductionCP12.Wait)return "LONG_WAIT:"+((DevelopmentProductionCP12.Wait)cause).reason;
            throw failure;
        }
    }
    private static int changes(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){need(c.moveToFirst(),"LONG_CHANGES");return c.getInt(0);}}
    private static Map<String,Object> fields(Object... pairs){Map<String,Object> out=new TreeMap<>();for(int i=0;i<pairs.length;i+=2)out.put((String)pairs[i],pairs[i+1]);return out;}
    private static void need(boolean value,String reason){if(!value)throw new IllegalStateException(reason);}
}
