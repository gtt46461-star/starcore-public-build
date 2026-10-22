package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Optional prospective research on the existing capture/settlement owners. No authority path.
 * The protocol is the fixed R78 offline protocol, including its negative efficacy finding.
 * Probabilities are raw, never calibrated. Prior residuals are diagnostics, not training labels.
 */
public final class MultiHorizonProbeR80 {
    public static final String INPUT="r80_probe_input", FORECAST="r80_probe_forecast",
            LABEL="r80_probe_label", DIAGNOSTIC_REF="r80_probe_diagnostic_ref", OPPORTUNITY="r80_probe_opportunity", SOURCE="r80_probe_pending";
    public static final String ROLE="PROBE_RESEARCH_ONLY_NOT_FORMAL";
    public static final String PROTOCOL="R80_PROBE_V1|game=Binance1-PK10|windows=48,240,960|h=1,2,4,8|alpha=1|weights=equal|position=1|K=3|threshold=0.40|consensus=equal_same_target|residual=prior_settled_48_per_h|baseline=fixed123|NO_REAL_NO_CALIBRATION_NO_TRAINING";
    public static final String PROTOCOL_HASH=V51ChecksumEngine.sha256(PROTOCOL);
    private static final int[] WINDOWS={48,240,960}, HORIZONS={1,2,4,8};
    private static final long MAX_SLICE_MS=180L, MIN_START_MS=80L;
    private MultiHorizonProbeR80() {}

    private static LinkedHashMap<String,String> schema() {
        LinkedHashMap<String,String> s=new LinkedHashMap<>();
        s.put(INPUT,"CREATE TABLE IF NOT EXISTS "+INPUT+"(anchor_issue TEXT PRIMARY KEY,protocol_hash TEXT NOT NULL,source_hash TEXT NOT NULL,model_hash TEXT NOT NULL,receipt_json TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE,captured_at INTEGER NOT NULL CHECK(captured_at>0))");
        s.put(FORECAST,"CREATE TABLE IF NOT EXISTS "+FORECAST+"(anchor_issue TEXT NOT NULL,horizon INTEGER NOT NULL CHECK(horizon IN(0,1,2,4,8)),target_issue TEXT NOT NULL,role TEXT NOT NULL CHECK(role='"+ROLE+"'),protocol_hash TEXT NOT NULL,input_hash TEXT NOT NULL,model_hash TEXT NOT NULL,forecast_hash TEXT NOT NULL UNIQUE,receipt_json TEXT NOT NULL,captured_at INTEGER NOT NULL CHECK(captured_at>0),PRIMARY KEY(anchor_issue,horizon))");
        s.put(LABEL,"CREATE TABLE IF NOT EXISTS "+LABEL+"(forecast_hash TEXT PRIMARY KEY,target_issue TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN('SETTLED','REJECTED')),reason TEXT NOT NULL,settlement_hash TEXT NOT NULL,completed_at INTEGER NOT NULL,receipt_json TEXT NOT NULL,label_hash TEXT NOT NULL UNIQUE,horizon INTEGER NOT NULL CHECK(horizon IN(0,1,2,4,8)))");
        s.put(OPPORTUNITY,"CREATE TABLE IF NOT EXISTS "+OPPORTUNITY+"(anchor_issue TEXT PRIMARY KEY,target_issue TEXT NOT NULL,protocol_hash TEXT NOT NULL,status TEXT NOT NULL CHECK(status IN('FROZEN','NOT_RUN')),reason TEXT NOT NULL,consensus_status TEXT NOT NULL,observed_at INTEGER NOT NULL,receipt_json TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE)");
        s.put(DIAGNOSTIC_REF,"CREATE TABLE IF NOT EXISTS "+DIAGNOSTIC_REF+"(input_hash TEXT NOT NULL REFERENCES "+INPUT+"(receipt_hash) ON DELETE CASCADE,label_hash TEXT NOT NULL REFERENCES "+LABEL+"(label_hash),PRIMARY KEY(input_hash,label_hash))");
        s.put("idx_r80_probe_diagnostic_label","CREATE INDEX IF NOT EXISTS idx_r80_probe_diagnostic_label ON "+DIAGNOSTIC_REF+"(label_hash)");
        s.put("idx_r80_probe_input_hash","CREATE INDEX IF NOT EXISTS idx_r80_probe_input_hash ON "+FORECAST+"(input_hash)");
        s.put("idx_r80_probe_label_horizon","CREATE INDEX IF NOT EXISTS idx_r80_probe_label_horizon ON "+LABEL+"(horizon,status,target_issue)");
        s.put("r80_probe_ref_no_replace","CREATE TRIGGER IF NOT EXISTS r80_probe_ref_no_replace BEFORE INSERT ON "+DIAGNOSTIC_REF+" WHEN EXISTS(SELECT 1 FROM "+DIAGNOSTIC_REF+" r WHERE r.input_hash=NEW.input_hash AND r.label_hash=NEW.label_hash) BEGIN SELECT RAISE(ABORT,'PROBE_REFERENCE_REPLACE_FORBIDDEN'); END");
        s.put("r80_probe_ref_no_update","CREATE TRIGGER IF NOT EXISTS r80_probe_ref_no_update BEFORE UPDATE ON "+DIAGNOSTIC_REF+" BEGIN SELECT RAISE(ABORT,'PROBE_REFERENCE_IMMUTABLE'); END");
        s.put("r80_probe_ref_delete_guard","CREATE TRIGGER IF NOT EXISTS r80_probe_ref_delete_guard BEFORE DELETE ON "+DIAGNOSTIC_REF+" WHEN EXISTS(SELECT 1 FROM "+INPUT+" i WHERE i.receipt_hash=OLD.input_hash) BEGIN SELECT RAISE(ABORT,'PROBE_REFERENCE_STILL_IN_USE'); END");
        s.put("r80_probe_input_ref_release","CREATE TRIGGER IF NOT EXISTS r80_probe_input_ref_release AFTER DELETE ON "+INPUT+" BEGIN DELETE FROM "+DIAGNOSTIC_REF+" WHERE input_hash=OLD.receipt_hash; END");
        s.put("idx_r80_probe_target","CREATE INDEX IF NOT EXISTS idx_r80_probe_target ON "+FORECAST+"(target_issue,horizon)");
        s.put("idx_r80_probe_label_target","CREATE INDEX IF NOT EXISTS idx_r80_probe_label_target ON "+LABEL+"(target_issue,status,completed_at)");
        s.put(SOURCE,"CREATE VIEW IF NOT EXISTS "+SOURCE+" AS SELECT f.target_issue,CASE WHEN l.forecast_hash IS NULL THEN 0 ELSE 1 END AS settled,CASE WHEN l.status='REJECTED' THEN l.reason ELSE '' END AS proof_error FROM "+FORECAST+" f LEFT JOIN "+LABEL+" l ON l.forecast_hash=f.forecast_hash");
        for(String table:new String[]{INPUT,FORECAST,LABEL,OPPORTUNITY}) {
            for(String action:new String[]{"UPDATE"}) {
                String name=table+"_no_"+action.toLowerCase(Locale.US);
                s.put(name,"CREATE TRIGGER IF NOT EXISTS "+name+" BEFORE "+action+" ON "+table+" BEGIN SELECT RAISE(ABORT,'R80_PROBE_IMMUTABLE'); END");
            }
            String name=table+"_no_replace";
            String identity=table.equals(LABEL)?"forecast_hash=NEW.forecast_hash":table.equals(FORECAST)?"anchor_issue=NEW.anchor_issue AND horizon=NEW.horizon":"anchor_issue=NEW.anchor_issue";
            s.put(name,"CREATE TRIGGER IF NOT EXISTS "+name+" BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+identity+") BEGIN SELECT RAISE(ABORT,'R80_PROBE_NO_REPLACE'); END");
            archiveOnly(s,table);
        }
        return s;
    }
    private static void archiveOnly(Map<String,String> s,String table){
        String proof="SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='"+table+"' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='"+table+"' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')";
        String eligible=archiveEligibleR80(table,"OLD","(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999)");
        s.put(table+"_archive_guard","CREATE TRIGGER "+table+"_archive_guard BEFORE DELETE ON "+table+" WHEN NOT COALESCE(("+eligible+"),0) OR NOT EXISTS("+proof+") BEGIN SELECT RAISE(ABORT,'PROBE_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END");
        s.put(table+"_archive_locator","CREATE TRIGGER "+table+"_archive_locator BEFORE DELETE ON "+table+" BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='"+table+"' AND p.source_rowid=OLD.rowid; END");
        s.put(table+"_revision_insert","CREATE TRIGGER "+table+"_revision_insert AFTER INSERT ON "+table+" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('"+table+"',NEW.rowid); END");
        s.put(table+"_revision_delete","CREATE TRIGGER "+table+"_revision_delete AFTER DELETE ON "+table+" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='"+table+"' AND source_rowid=OLD.rowid; END");
    }
    public static String archiveIssueColumnR80(String table) {
        if(table.equals(INPUT)||table.equals(OPPORTUNITY))return "anchor_issue";
        if(table.equals(FORECAST)||table.equals(LABEL))return "target_issue";
        throw new IllegalArgumentException("PROBE_ARCHIVE_TABLE");
    }
    /** Same predicate at selection and atomic deletion; no arbitrary completion/sample cap.
     * Live receipts keep every input needed for pending settlement and the fixed diagnostic window.
     * Old originals leave SQLite only through the existing exact-byte verified CAS archive owner.
     */
    public static String archiveEligibleR80(String table,String alias,String cutoff) {
        String old=alias+"."+archiveIssueColumnR80(table)+"<"+cutoff;
        if(table.equals(FORECAST))return old
            +" AND EXISTS(SELECT 1 FROM "+LABEL+" l WHERE l.forecast_hash="+alias+".forecast_hash)"
            +" AND "+alias+".forecast_hash NOT IN(SELECT l.forecast_hash FROM "+LABEL+" l WHERE l.horizon="+alias+".horizon AND l.status='SETTLED' ORDER BY l.target_issue DESC LIMIT 48)"
            +" AND NOT EXISTS(SELECT 1 FROM "+FORECAST+" c WHERE c.target_issue="+alias+".target_issue AND c.horizon=0 AND instr(c.receipt_json,"+alias+".forecast_hash)>0 AND NOT EXISTS(SELECT 1 FROM "+LABEL+" l WHERE l.forecast_hash=c.forecast_hash))";
        if(table.equals(INPUT))return old+" AND NOT EXISTS(SELECT 1 FROM "+FORECAST+" f WHERE f.input_hash="+alias+".receipt_hash)";
        if(table.equals(LABEL))return old+" AND NOT EXISTS(SELECT 1 FROM "+FORECAST+" f WHERE f.forecast_hash="+alias+".forecast_hash)"
            +" AND NOT EXISTS(SELECT 1 FROM "+DIAGNOSTIC_REF+" r WHERE r.label_hash="+alias+".label_hash)";
        return old+" AND NOT EXISTS(SELECT 1 FROM "+FORECAST+" f WHERE f.anchor_issue="+alias+".anchor_issue)";
    }
    /** Only the established migration owner calls this, in its transaction. */
    public static void schemaR80(SQLiteDatabase db) {
        requireWriter(db);
        for(Map.Entry<String,String> e:schema().entrySet()) {
            try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{e.getKey()})) {
                if(c.moveToFirst()) {
                    if(!normalizeSql(e.getValue()).equals(normalizeSql(c.getString(0))))throw new IllegalStateException("PROBE_SCHEMA_DRIFT:"+e.getKey());
                    continue;
                }
            }
            db.execSQL(e.getValue());
        }
    }
    public static boolean readyR80(SQLiteDatabase db) {
        if(db==null)return false;
        for(Map.Entry<String,String> e:schema().entrySet())try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{e.getKey()})) {
            if(!c.moveToFirst()||!normalizeSql(e.getValue()).equals(normalizeSql(c.getString(0))))return false;
        }
        return true;
    }
    private static String normalizeSql(String sql){return sql==null?"":sql.replace("IF NOT EXISTS ","").replaceAll("\\s+"," ").trim();}

    public static final class Model {
        private final int[][][] counts;
        public final int lastDigit;
        public final String sourceHash, modelJson, modelHash;
        Model(int[][][] counts,int lastDigit,String sourceHash) {
            this.counts=counts;this.lastDigit=lastDigit;this.sourceHash=sourceHash;
            Map<String,Object> m=new TreeMap<>();m.put("protocolHash",PROTOCOL_HASH);m.put("lastDigit",lastDigit+1);
            List<Object> cubes=new ArrayList<>();
            for(int[][] matrix:counts){List<Object> rows=new ArrayList<>();for(int[] row:matrix){List<Integer> values=new ArrayList<>();for(int x:row)values.add(x);rows.add(values);}cubes.add(rows);}
            m.put("transitionCounts",cubes);modelJson=StrictCanonicalJson.write(m);modelHash=hash(modelJson);
        }
        public double[] distribution(int horizon) {
            if(!validHorizon(horizon))throw new IllegalArgumentException("PROBE_HORIZON");
            double[] q=new double[10];
            for(int[][] matrix:counts) {
                double[][] t=new double[10][10];
                for(int r=0;r<10;r++){int sum=10;for(int x:matrix[r])sum+=x;for(int c=0;c<10;c++)t[r][c]=(matrix[r][c]+1d)/sum;}
                // Row-vector multiplication h times is exactly the row of T^h; no chosen horizon.
                double[] v=new double[10];v[lastDigit]=1d;
                for(int h=0;h<horizon;h++){double[] next=new double[10];for(int c=0;c<10;c++)for(int r=0;r<10;r++)next[c]+=v[r]*t[r][c];v=next;}
                for(int i=0;i<10;i++)q[i]+=v[i]/3d;
            }
            requireDistribution(q);return q;
        }
    }
    /** Oldest to newest, exactly the trailing 960 available rows; never reads a target label. */
    public static Model fit(List<Models.Draw> history,String anchor,long frozenAt) {
        if(history==null||history.size()<960||history.size()>5000)throw new IllegalArgumentException("PROBE_HISTORY_INSUFFICIENT_OR_BOUND");
        int first=history.size()-960;String previous="";StringBuilder source=new StringBuilder("R80_PROBE_SOURCE_V1");
        int[] digits=new int[960];
        for(int i=first;i<history.size();i++) {
            if((i&63)==0)ResearchWorkBudgetR927.checkpoint();
            Models.Draw d=history.get(i);
            if(d==null||!d.isValid()||d.dirty!=0||!TargetIssueResolver.isValid(d.issue)||d.issue.compareTo(anchor)>0
                    ||d.openTime<=0||d.openTime>=frozenAt||d.fetchTime<=0||d.fetchTime>frozenAt)
                throw new IllegalArgumentException("PROBE_HISTORY_TIME_OR_DRAW");
            if(!previous.isEmpty()&&!TargetIssueResolver.exact(previous,d.issue).valid)throw new IllegalArgumentException("PROBE_HISTORY_GAP");
            source.append('|').append(d.issue).append(':').append(d.numbersText()).append(':').append(d.openTime).append(':').append(d.fetchTime);
            previous=d.issue;digits[i-first]=d.numbers[0]-1;
        }
        if(!anchor.equals(previous))throw new IllegalArgumentException("PROBE_ANCHOR_MISSING");
        int[][][] counts=new int[3][10][10];
        for(int w=0;w<3;w++)for(int i=960-WINDOWS[w];i<959;i++)counts[w][digits[i]][digits[i+1]]++;
        return new Model(counts,digits[959],hash(source.toString()));
    }
    public static String target(String anchor,int h) {
        if(!validHorizon(h))throw new IllegalArgumentException("PROBE_HORIZON");
        String next=anchor;for(int i=0;i<h;i++){TargetIssueResolver.Resolution r=TargetIssueResolver.next(next);if(!r.valid)throw new IllegalArgumentException("PROBE_ISSUE");next=r.targetIssue;}return next;
    }
    public static int mask(double[] q) {
        requireDistribution(q);Integer[] digits={0,1,2,3,4,5,6,7,8,9};
        Arrays.sort(digits,(a,b)->{int c=Double.compare(q[b],q[a]);return c!=0?c:Integer.compare(a,b);});
        return (1<<digits[0])|(1<<digits[1])|(1<<digits[2]);
    }
    public static double mass(double[] q,int mask){double out=0;for(int i=0;i<10;i++)if((mask&(1<<i))!=0)out+=q[i];return out;}

    /** Called only AFTER ResearchCaptureQueue.complete, never consumes an admitted capture slot.
     * Existing slice/deadline/P0 fences govern computation and publication. A miss is not zero.
     */
    public static String captureOptional(DbStore store,DeferredEvidenceSnapshotR36.Snapshot snapshot,long ownerDeadlineMono) {
        if(store==null||snapshot==null)return "NOT_RUN_INPUT";
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "NOT_RUN_P0_PREEMPTED";
        if(TimeSourceR811.monoNowMs()>=ownerDeadlineMono||ResearchWorkBudgetR927.remainingMs()<10L)return "NOT_RUN_OWNER_BUDGET_GAP";
        if(!readyR80(store.readable()))return "NOT_RUN_SCHEMA";
        if(!TargetIssueResolver.exact(snapshot.baseIssue,snapshot.targetIssue).valid)return "NOT_RUN_ANCHOR";
        if(exists(store.readable(),OPPORTUNITY,"anchor_issue",snapshot.baseIssue))return "ALREADY_RECORDED";
        if(!ResearchCaptureQueueR946.complete(store,snapshot.targetIssue))return "NOT_RUN_MANDATORY_CAPTURE_PENDING";
        long remaining=Math.min(ResearchWorkBudgetR927.remainingMs(),ownerDeadlineMono-TimeSourceR811.monoNowMs());
        if(remaining<MIN_START_MS)return recordNotRun(store,snapshot,"TIME_BUDGET",ownerDeadlineMono);
        ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(Math.min(MAX_SLICE_MS,remaining));
        try(com.ethk3.novaonepro.r952.MemoryBudgetCP4.Lease memory=
                RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(2L<<20)) {
            ResearchWorkBudgetR927.checkpoint();
            long now=System.currentTimeMillis();Models.Draw base=store.getDrawByIssue(snapshot.baseIssue);
            long boundary=ResearchPredrawCaptureR923.captureDeadlineR77(base,snapshot.targetIssue);
            if(now>=boundary)return recordNotRun(store,snapshot,"MISSED_ORIGINAL_CLOCK",ownerDeadlineMono);
            verifyHistoryObservation(store.readable(),snapshot.baseIssue,now);
            String historyRevision=historyRevision(store.readable(),snapshot.baseIssue);
            List<Models.Draw> rows=store.getDrawsBeforeIssue(snapshot.targetIssue,960);
            if(rows==null||rows.size()<960)return recordNotRun(store,snapshot,"HISTORY_INSUFFICIENT",ownerDeadlineMono);
            rows=new ArrayList<>(rows);Collections.sort(rows,(a,b)->a.issue.compareTo(b.issue));
            Model model=fit(rows,snapshot.baseIssue,now);
            Map<String,Object> clock=readClock(store.readable(),snapshot.baseIssue,now);
            clock.put("historyRevision",historyRevision);
            Map<String,Object> diagnostics=priorDiagnostics(store.readable(),snapshot.baseIssue,now);
            Prepared prepared=prepare(snapshot.baseIssue,snapshot.recordHash,model,clock,diagnostics,now);
            addConsensus(store.readable(),prepared);
            ResearchWorkBudgetR927.checkpoint();
            Boolean committed=store.coordinatorR36().runAtomicWrite("R80_OPTIONAL_PROBE_FREEZE",new DatabaseCoordinator.WriteCallable<Boolean>(){
                public Boolean call(SQLiteDatabase db){return commit(db,prepared,ownerDeadlineMono);}
            },Boolean.FALSE);
            return Boolean.TRUE.equals(committed)?"FROZEN_RESEARCH_ONLY":"NOT_RUN_WRITE_NOT_COMMITTED";
        } catch(ResearchWorkBudgetR927.Yield yield) { return "NOT_RUN_YIELD_GAP"; }
        catch(IllegalArgumentException invalid) { return recordNotRun(store,snapshot,invalid.getMessage(),ownerDeadlineMono); }
        finally {ResearchWorkBudgetR927.leave(scope);}
    }
    private static String recordNotRun(DbStore store,DeferredEvidenceSnapshotR36.Snapshot s,String reason,long deadline) {
        if(TimeSourceR811.monoNowMs()>=deadline||ResearchWorkBudgetR927.remainingMs()<10
                ||RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "NOT_RUN_UNRECORDED_GAP_"+reason;
        final Map<String,Object> row=opportunity(s.baseIssue,"NOT_RUN",reason,"NOT_RUN",System.currentTimeMillis());
        Boolean recorded=store.coordinatorR36().runAtomicWrite("R80_PROBE_NOT_RUN",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(SQLiteDatabase db){ResearchWorkBudgetR927.checkpoint();if(TimeSourceR811.monoNowMs()>=deadline)return false;writeOpportunity(db,row);return true;}
        },Boolean.FALSE);
        return "NOT_RUN_"+reason+(Boolean.TRUE.equals(recorded)?"":"_UNRECORDED_GAP");
    }
    static final class Prepared {
        final String anchor,inputJson,inputHash,modelHash;
        final long frozenAt,deadline;
        final List<Map<String,Object>> predictions=new ArrayList<>();
        String consensusStatus="NOT_RUN_MISSING_PRIOR_HORIZONS";
        Prepared(String anchor,String inputJson,String modelHash,long frozenAt,long deadline){this.anchor=anchor;this.inputJson=inputJson;inputHash=hash(inputJson);this.modelHash=modelHash;this.frozenAt=frozenAt;this.deadline=deadline;}
    }
    static Prepared prepare(String anchor,String upstream,Model model,Map<String,Object> clock,Map<String,Object> diagnostics,long now) {
        requireHash(upstream);long open=number(clock,"openTime"),first=number(clock,"firstSeenAt");
        if(!anchor.equals(string(clock,"issue"))||open<=0||first<=0||open>=now||first>now)throw new IllegalArgumentException("PROBE_ANCHOR_CLOCK");
        long deadline=Math.addExact(open,60000L);
        if(now>=deadline)throw new IllegalArgumentException("PROBE_LATE_CAPTURE");
        Map<String,Object> input=base();input.put("anchorIssue",anchor);input.put("clock",clock);input.put("frozenAt",now);
        input.put("sourceRetention","COMPACT_COUNTS_AND_ORIGINAL_SOURCE_SHA256;RAW_960_PREFIX_NOT_RETAINED_AFTER_5000_ROLLING_CACHE");
        input.put("nextBoundary",deadline);input.put("sourceHash",model.sourceHash);input.put("upstreamSnapshotHash",upstream);
        input.put("model",StrictCanonicalJson.readObject(model.modelJson));input.put("modelHash",model.modelHash);
        input.put("priorSettledDiagnostics",diagnostics);input.put("diagnosticHash",hash(StrictCanonicalJson.write(diagnostics)));
        Prepared p=new Prepared(anchor,StrictCanonicalJson.write(input),model.modelHash,now,deadline);
        for(int h:HORIZONS)p.predictions.add(prediction(p,h,model.distribution(h),Collections.singletonList(p.inputHash)));
        return p;
    }
    private static Map<String,Object> prediction(Prepared p,int h,double[] q,List<String> components) {
        Map<String,Object> f=base();f.put("anchorIssue",p.anchor);f.put("targetIssue",target(p.anchor,h==0?1:h));f.put("horizon",h);
        f.put("inputHash",p.inputHash);f.put("modelHash",p.modelHash);f.put("frozenAt",p.frozenAt);f.put("nextBoundary",p.deadline);
        f.put("targetBoundary",Math.addExact(p.deadline,(long)Math.max(0,h-1)*60000L));
        f.put("distribution",hex(q));int mask=mask(q);f.put("mask",mask);f.put("rawMass",Double.toHexString(mass(q,mask)));
        f.put("threshold040",mass(q,mask)>=.40d);f.put("componentHashes",components);return f;
    }
    private static void addConsensus(SQLiteDatabase db,Prepared p) {
        double[] q=distribution(p.predictions.get(0));for(int i=0;i<10;i++)q[i]/=4d;
        List<String> components=new ArrayList<>();components.add(hash(StrictCanonicalJson.write(p.predictions.get(0))));
        String t=target(p.anchor,1);
        for(int h:new int[]{2,4,8})try(Cursor c=db.rawQuery("SELECT receipt_json,forecast_hash FROM "+FORECAST+" WHERE target_issue=? AND horizon=?",new String[]{t,Integer.toString(h)})) {
            if(!c.moveToFirst())return;
            Map<String,Object> f=verified(c.getString(0),c.getString(1));verifyForecast(f);
            if(number(f,"frozenAt")>=p.frozenAt||string(f,"anchorIssue").compareTo(p.anchor)>=0)throw new IllegalArgumentException("PROBE_CONSENSUS_FUTURE");
            if(number(f,"targetBoundary")!=p.deadline){p.consensusStatus="NOT_RUN_ORIGINAL_CLOCKS_DISAGREE";return;}
            double[] old=distribution(f);for(int i=0;i<10;i++)q[i]+=old[i]/4d;components.add(c.getString(1));
            if(c.moveToNext())throw new IllegalArgumentException("PROBE_CONSENSUS_AMBIGUOUS");
        }
        p.predictions.add(prediction(p,0,q,components));p.consensusStatus="FROZEN";
    }
    /** Package visibility permits rollback/restart tests of the exact publication transaction. */
    static boolean commit(SQLiteDatabase db,Prepared p,long ownerDeadline) {
        requireWriter(db);ResearchWorkBudgetR927.checkpoint();
        if(TimeSourceR811.monoNowMs()>=ownerDeadline||System.currentTimeMillis()>=p.deadline)throw new IllegalArgumentException("PROBE_PUBLICATION_LATE");
        if(exists(db,OPPORTUNITY,"anchor_issue",p.anchor))return false;
        try(Cursor latest=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)) {
            if(!latest.moveToFirst()||!p.anchor.equals(latest.getString(0)))throw new IllegalArgumentException("PROBE_ANCHOR_SUPERSEDED");
        }
        Map<String,Object> input=verified(p.inputJson,p.inputHash);
        verifyHistoryObservation(db,p.anchor,p.frozenAt);
        Map<String,Object> currentClock=readClock(db,p.anchor,p.frozenAt);
        currentClock.put("historyRevision",historyRevision(db,p.anchor));
        if(!StrictCanonicalJson.write(currentClock).equals(StrictCanonicalJson.write(input.get("clock"))))throw new IllegalArgumentException("PROBE_CLOCK_OR_PREFIX_CHANGED");
        // Recheck every diagnostic receipt; a correction cannot silently become a training fact.
        verifyDiagnostics(db,object(input,"priorSettledDiagnostics"),p.anchor,p.frozenAt);
        db.execSQL("INSERT INTO "+INPUT+" VALUES(?,?,?,?,?,?,?)",new Object[]{p.anchor,PROTOCOL_HASH,string(input,"sourceHash"),p.modelHash,p.inputJson,p.inputHash,p.frozenAt});
        Map<String,Object> diagnostics=object(input,"priorSettledDiagnostics");
        for(int h:new int[]{0,1,2,4,8})for(Object ref:list(object(diagnostics,"h"+h),"labelHashes"))
            db.execSQL("INSERT INTO "+DIAGNOSTIC_REF+" VALUES(?,?)",new Object[]{p.inputHash,String.valueOf(ref)});
        for(Map<String,Object> f:p.predictions) {
            verifyForecast(f);String json=StrictCanonicalJson.write(f),fh=hash(json);int h=(int)number(f,"horizon");
            if(exists(db,DbHelper.TABLE_DRAWS,"issue",string(f,"targetIssue")))throw new IllegalArgumentException("PROBE_TARGET_ALREADY_DRAWN");
            db.execSQL("INSERT INTO "+FORECAST+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{p.anchor,h,string(f,"targetIssue"),ROLE,PROTOCOL_HASH,p.inputHash,p.modelHash,fh,json,p.frozenAt});
        }
        writeOpportunity(db,opportunity(p.anchor,"FROZEN","PROSPECTIVE_ONLY",p.consensusStatus,p.frozenAt));
        ResearchWorkBudgetR927.checkpoint();if(System.currentTimeMillis()>=p.deadline||TimeSourceR811.monoNowMs()>=ownerDeadline)throw new IllegalArgumentException("PROBE_COMMIT_LATE");return true;
    }

    /** Called by the existing authoritative settlement transaction; no formal-state writes. */
    public static int settle(SQLiteDatabase db,Models.Draw draw,String settlementHash,long completedAt) {
        requireWriter(db);if(!readyR80(db))return 0;
        // Optional research must not impose extra clock requirements on unrelated settlements.
        if(draw!=null)try(Cursor pending=db.rawQuery("SELECT 1 FROM "+FORECAST+" f WHERE f.target_issue=? AND NOT EXISTS(SELECT 1 FROM "+LABEL+" l WHERE l.forecast_hash=f.forecast_hash) LIMIT 1",new String[]{draw.issue})) {
            if(!pending.moveToFirst())return 0;
        }
        requireHash(settlementHash);
        if(draw==null||!draw.isValid()||draw.dirty!=0||draw.openTime<=0)throw new IllegalArgumentException("PROBE_ACTUAL_INVALID");
        try(Cursor c=db.rawQuery("SELECT status,settlement_hash,completed_at FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=?",new String[]{draw.issue})) {
            if(!c.moveToFirst()||!"COMPLETED".equals(c.getString(0))||!settlementHash.equals(c.getString(1))||completedAt!=c.getLong(2))throw new IllegalArgumentException("PROBE_AUTHORITY_MISSING");
        }
        Map<String,Object> actual=readActual(db,draw);
        if(completedAt<draw.openTime||number(actual,"firstSeenAt")>completedAt||number(actual,"fetchTime")>completedAt)throw new IllegalArgumentException("PROBE_TERMINAL_CLOCK");
        List<String[]> pending=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT f.forecast_hash,f.receipt_json,f.input_hash,i.receipt_json,f.anchor_issue,f.horizon,f.model_hash,f.captured_at FROM "+FORECAST+" f LEFT JOIN "+INPUT+" i ON i.receipt_hash=f.input_hash WHERE f.target_issue=? AND NOT EXISTS(SELECT 1 FROM "+LABEL+" l WHERE l.forecast_hash=f.forecast_hash) ORDER BY f.horizon LIMIT 5",new String[]{draw.issue})) {
            while(c.moveToNext()){String[] r=new String[8];for(int i=0;i<8;i++)r[i]=c.getString(i);pending.add(r);}
        }
        int settled=0;
        for(String[] row:pending) {
            String status="SETTLED",reason="EXACT_ORIGINAL_FREEZE_AND_AUTHORITY";Map<String,Object> label=base();
            try {
                Map<String,Object> f=verified(row[1],row[0]),input=verified(row[3],row[2]);verifyForecast(f);
                if(!draw.issue.equals(string(f,"targetIssue"))||!row[4].equals(string(f,"anchorIssue"))
                        ||Long.parseLong(row[5])!=number(f,"horizon")||!row[6].equals(string(f,"modelHash"))
                        ||Long.parseLong(row[7])!=number(f,"frozenAt")||!row[2].equals(string(f,"inputHash")))throw new IllegalArgumentException("PROBE_INDEX_DRIFT");
                long frozen=number(f,"frozenAt"),boundary=number(f,"targetBoundary");
                if(frozen>=draw.openTime||frozen>=number(actual,"firstSeenAt")||frozen>=completedAt||draw.openTime!=boundary
                        ||!string(input,"modelHash").equals(string(f,"modelHash"))
                        ||!hash(StrictCanonicalJson.write(input.get("model"))).equals(string(f,"modelHash"))
                        ||number(input,"nextBoundary")!=number(f,"nextBoundary")||number(input,"frozenAt")!=frozen
                        ||!string(input,"sourceCodeHash").equals(string(f,"sourceCodeHash")))
                    throw new IllegalArgumentException("PROBE_ORIGINAL_CLOCK_OR_MODEL_MISMATCH");
                Map<String,Object> originalClock=object(input,"clock");
                if(!string(f,"anchorIssue").equals(string(originalClock,"issue"))
                        ||Math.addExact(number(originalClock,"openTime"),60000L)!=number(f,"nextBoundary")
                        ||number(originalClock,"firstSeenAt")>frozen)throw new IllegalArgumentException("PROBE_ORIGINAL_ANCHOR_CLOCK");
                Model original=readModel(object(input,"model"),string(input,"sourceHash"));
                if(number(f,"horizon")!=0&&!hex(original.distribution((int)number(f,"horizon"))).equals(f.get("distribution")))throw new IllegalArgumentException("PROBE_MODEL_MATH");
                verifyConsensus(db,f);int number=draw.numbers[0],hit=(((int)number(f,"mask"))&(1<<(number-1)))!=0?1:0;
                double raw=decimal(f,"rawMass"),residual=hit-raw;
                label.put("hit",hit);label.put("rawMass",Double.toHexString(raw));label.put("residual",Double.toHexString(residual));label.put("brierRawNotCalibrated",Double.toHexString(residual*residual));label.put("horizon",number(f,"horizon"));settled++;
            } catch(IllegalArgumentException bad) {status="REJECTED";reason=bad.getMessage()==null?"PROBE_INVALID_RECEIPT":bad.getMessage();}
            label.put("horizon",Long.parseLong(row[5]));label.put("forecastHash",row[0]);label.put("targetIssue",draw.issue);label.put("status",status);label.put("reason",reason);
            label.put("actual",actual);label.put("settlementHash",settlementHash);label.put("completedAt",completedAt);
            String json=StrictCanonicalJson.write(label);
            db.execSQL("INSERT INTO "+LABEL+" VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{row[0],draw.issue,status,reason,settlementHash,completedAt,json,hash(json),Integer.parseInt(row[5])});
        }
        return settled;
    }
    private static Model readModel(Map<String,Object> m,String sourceHash) {
        requireHash(sourceHash);if(!PROTOCOL_HASH.equals(string(m,"protocolHash")))throw new IllegalArgumentException("PROBE_MODEL_PROTOCOL");
        int digit=(int)number(m,"lastDigit")-1;if(digit<0||digit>9)throw new IllegalArgumentException("PROBE_MODEL_LAST_DIGIT");
        List<?> cubes=list(m,"transitionCounts");if(cubes.size()!=3)throw new IllegalArgumentException("PROBE_MODEL_WINDOWS");
        int[][][] counts=new int[3][10][10];
        for(int w=0;w<3;w++){Object matrix=cubes.get(w);if(!(matrix instanceof List)||((List<?>)matrix).size()!=10)throw new IllegalArgumentException("PROBE_MODEL_MATRIX");int total=0;
            for(int r=0;r<10;r++){Object row=((List<?>)matrix).get(r);if(!(row instanceof List)||((List<?>)row).size()!=10)throw new IllegalArgumentException("PROBE_MODEL_ROW");
                for(int c=0;c<10;c++){Object value=((List<?>)row).get(c);if(!(value instanceof Number))throw new IllegalArgumentException("PROBE_MODEL_COUNT");long x=((Number)value).longValue();if(x<0||x>=WINDOWS[w])throw new IllegalArgumentException("PROBE_MODEL_COUNT");counts[w][r][c]=(int)x;total+=x;}}
            if(total!=WINDOWS[w]-1)throw new IllegalArgumentException("PROBE_MODEL_WINDOW_COUNT");
        }
        return new Model(counts,digit,sourceHash);
    }

    /** Snapshot-only report. Each horizon and consensus keeps its own target denominator.
     * Missing schedule opportunities are inferred gaps, never fabricated frozen predictions.
     */
    public static String reportJson(SQLiteDatabase db) {return reportJson(db,()->true);}
    public static String reportJson(SQLiteDatabase db,java.util.function.BooleanSupplier proceed) {
        Map<String,Object> out=base();out.put("protocol",PROTOCOL);out.put("promotion","NOT_GRANTED");
        out.put("efficacy","NOT_PROVEN");out.put("correlatedHorizonsAreIndependentSamples",false);
        out.put("probabilityEncoding","JAVA_HEX_DOUBLE_EXACT_RAW_NOT_CALIBRATED");
        out.put("comparisonPolicy","FIXED_HORIZONS_THRESHOLD_AND_RESIDUAL_STRATA;NO_RETROSPECTIVE_SELECTION");
        out.put("historyRetentionLimit",5000);out.put("diagnosticWindowPerHorizon",48);
        if(!readyR80(db)){out.put("status","NOT_RUN_SCHEMA_UNAVAILABLE");return StrictCanonicalJson.write(out);}
        out.put("status","READ_OK");long archivedPages=0;
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM research_evidence_archive_v2 WHERE source_table IN('r80_probe_input','r80_probe_forecast','r80_probe_label','r80_probe_opportunity') AND state IN('VERIFIED','REFERENCED','PRUNED')",null)){if(c.moveToFirst())archivedPages=c.getLong(0);}
        out.put("archivedOriginalPages",archivedPages);out.put("metricScope","RETAINED_HOT_ORIGINALS_AND_PENDING;NOT_LIFETIME_METRICS");
        out.put("archivePolicy","VERIFIED_CAS_BEYOND_5000;KEEP_PENDING_REFERENCES_AND_LAST48_SETTLED_PER_HORIZON;AUDIT_LOCATORS_RETAIN_ORIGINAL_BYTES");
        long observed=0;String first="",latest="";
        try(Cursor c=db.rawQuery("SELECT COUNT(*),MIN(anchor_issue) FROM "+OPPORTUNITY,null)){if(c.moveToFirst()){observed=c.getLong(0);first=c.isNull(1)?"":c.getString(1);}}
        try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        out.put("observedOpportunityTargets",observed);out.put("firstObservedAnchor",first);out.put("latestConfirmedAnchor",latest);
        int distance=TargetIssueResolver.distance(first,latest);
        Long expected=archivedPages==0&&distance>=0?(long)distance+1:null;out.put("scheduledTargetsSinceFirstObservedAnchor",expected);
        out.put("unrecordedGapTargets",expected==null?null:Math.max(0,expected-observed));out.put("gapMeaning",archivedPages==0?"EXPECTED_SCHEDULE_NO_CAPTURE_RECEIPT_NOT_BACKFILLED":"UNAVAILABLE_AFTER_ARCHIVAL;ARCHIVED_OPPORTUNITIES_ARE_NOT_CAPTURE_GAPS");
        Map<String,Object> groups=new TreeMap<>();
        for(int h:new int[]{0,1,2,4,8}) {
            long frozen=0,settled=0,rejected=0,gated=0,gatedHits=0,hits=0,baselineHits=0;double brier=0,baselineBrier=0;
            Map<String,long[]> strata=new TreeMap<>();for(String name:new String[]{"UNAVAILABLE","PRIOR_MEAN_RESIDUAL_POSITIVE","PRIOR_MEAN_RESIDUAL_NONPOSITIVE"})strata.put(name,new long[2]);
            try(Cursor c=db.rawQuery("SELECT f.receipt_json,f.forecast_hash,l.receipt_json,l.label_hash,i.receipt_json,i.receipt_hash FROM "+FORECAST+" f LEFT JOIN "+INPUT+" i ON i.receipt_hash=f.input_hash LEFT JOIN "+LABEL+" l ON l.forecast_hash=f.forecast_hash WHERE f.horizon=? ORDER BY f.target_issue",new String[]{Integer.toString(h)})) {
                while(c.moveToNext()){
                    if(Thread.currentThread().isInterrupted()||!proceed.getAsBoolean())throw new IllegalStateException("PROBE_REPORT_CANCELLED");
                    Map<String,Object> f=verified(c.getString(0),c.getString(1));verifyForecast(f);frozen++;
                    if(c.isNull(2))continue;Map<String,Object> l=verified(c.getString(2),c.getString(3));
                    if(!"SETTLED".equals(string(l,"status"))){rejected++;continue;}
                    settled++;long hit=number(l,"hit");hits+=hit;brier+=decimal(l,"brierRawNotCalibrated");if(Boolean.TRUE.equals(f.get("threshold040"))){gated++;gatedHits+=hit;}
                    int actual=((Number)list(object(l,"actual"),"numbers").get(0)).intValue();int baselineHit=actual<=3?1:0;baselineHits+=baselineHit;baselineBrier+=(baselineHit-.3d)*(baselineHit-.3d);
                    Map<String,Object> input=verified(c.getString(4),c.getString(5));String condition=string(object(object(input,"priorSettledDiagnostics"),"h"+h),"condition");
                    if(!strata.containsKey(condition))throw new IllegalArgumentException("PROBE_DIAGNOSTIC_CONDITION");strata.get(condition)[0]++;strata.get(condition)[1]+=hit;
                }
            }
            Map<String,Object> one=new TreeMap<>();one.put("frozenTargets",frozen);one.put("settledTargets",settled);one.put("rejectedTargets",rejected);one.put("pendingTargets",frozen-settled-rejected);one.put("hits",hits);
            one.put("hitRate",settled==0?null:Double.toHexString((double)hits/settled));one.put("brierRawNotCalibrated",settled==0?null:Double.toHexString(brier/settled));
            one.put("threshold040SettledTargets",gated);one.put("threshold040Hits",gatedHits);one.put("threshold040CoverageOfSettledTargets",settled==0?null:Double.toHexString((double)gated/settled));
            one.put("threshold040HitRate",gated==0?null:Double.toHexString((double)gatedHits/gated));
            one.put("fixed123BaselineHitsSameTargets",baselineHits);one.put("fixed123BaselineBrierSameTargets",settled==0?null:Double.toHexString(baselineBrier/settled));
            Map<String,Object> bins=new TreeMap<>();for(Map.Entry<String,long[]> bin:strata.entrySet()){Map<String,Object> row=new TreeMap<>();row.put("settledTargets",bin.getValue()[0]);row.put("hits",bin.getValue()[1]);row.put("hitRate",bin.getValue()[0]==0?null:Double.toHexString((double)bin.getValue()[1]/bin.getValue()[0]));bins.put(bin.getKey(),row);}one.put("predeclaredPriorResidualStrata",bins);
            groups.put(h==0?"fixedSameTargetConsensus":"h"+h,one);
        }
        out.put("separateTargetDenominators",groups);return StrictCanonicalJson.write(out);
    }

    private static void verifyConsensus(SQLiteDatabase db,Map<String,Object> f) {
        if(number(f,"horizon")!=0)return;
        List<?> refs=list(f,"componentHashes");if(refs.size()!=4)throw new IllegalArgumentException("PROBE_CONSENSUS_COMPONENTS");
        double[] q=new double[10];
        for(int i=0;i<4;i++)try(Cursor c=db.rawQuery("SELECT receipt_json,forecast_hash FROM "+FORECAST+" WHERE forecast_hash=?",new String[]{String.valueOf(refs.get(i))})) {
            if(!c.moveToFirst())throw new IllegalArgumentException("PROBE_CONSENSUS_SOURCE_MISSING");Map<String,Object> part=verified(c.getString(0),c.getString(1));verifyForecast(part);
            if(number(part,"horizon")!=HORIZONS[i]||!string(part,"targetIssue").equals(string(f,"targetIssue"))||number(part,"frozenAt")>number(f,"frozenAt")||number(part,"targetBoundary")!=number(f,"targetBoundary"))throw new IllegalArgumentException("PROBE_CONSENSUS_SOURCE_CHANGED");
            double[] one=distribution(part);for(int k=0;k<10;k++)q[k]+=one[k]/4d;
        }
        if(!hex(q).equals(f.get("distribution")))throw new IllegalArgumentException("PROBE_CONSENSUS_MATH");
    }
    private static Map<String,Object> priorDiagnostics(SQLiteDatabase db,String anchor,long frozenAt) {
        Map<String,Object> out=new TreeMap<>();out.put("role","PRIOR_SETTLED_DIAGNOSTICS_ONLY_NO_TRAINING");out.put("boundaryIssue",anchor);out.put("frozenAt",frozenAt);
        for(int h:new int[]{0,1,2,4,8}) {
            List<String> refs=new ArrayList<>();double residual=0,brier=0;int hits=0;
            try(Cursor c=db.rawQuery("SELECT l.receipt_json,l.label_hash FROM "+LABEL+" l WHERE l.status='SETTLED' AND l.horizon=? AND l.target_issue<=? AND l.completed_at<? ORDER BY l.target_issue DESC LIMIT 48",new String[]{Integer.toString(h),anchor,Long.toString(frozenAt)})) {
                while(c.moveToNext()){Map<String,Object> l=verified(c.getString(0),c.getString(1));requirePrior(l,anchor,frozenAt);refs.add(c.getString(1));residual+=decimal(l,"residual");brier+=decimal(l,"brierRawNotCalibrated");hits+=(int)number(l,"hit");}
            }
            Map<String,Object> one=new TreeMap<>();one.put("n",refs.size());one.put("labelHashes",refs);one.put("hits",hits);
            one.put("meanResidual",refs.isEmpty()?null:Double.toHexString(residual/refs.size()));one.put("brierRawNotCalibrated",refs.isEmpty()?null:Double.toHexString(brier/refs.size()));
            one.put("condition",refs.isEmpty()?"UNAVAILABLE":residual>0?"PRIOR_MEAN_RESIDUAL_POSITIVE":"PRIOR_MEAN_RESIDUAL_NONPOSITIVE");out.put("h"+h,one);
        }
        return out;
    }
    private static void verifyDiagnostics(SQLiteDatabase db,Map<String,Object> d,String anchor,long frozenAt) {
        if(!anchor.equals(string(d,"boundaryIssue"))||frozenAt!=number(d,"frozenAt"))throw new IllegalArgumentException("PROBE_DIAGNOSTIC_BOUNDARY");
        for(int h:new int[]{0,1,2,4,8})for(Object ref:list(object(d,"h"+h),"labelHashes"))try(Cursor c=db.rawQuery("SELECT receipt_json,label_hash FROM "+LABEL+" WHERE label_hash=?",new String[]{String.valueOf(ref)})) {
            if(!c.moveToFirst())throw new IllegalArgumentException("PROBE_DIAGNOSTIC_MISSING");Map<String,Object> l=verified(c.getString(0),c.getString(1));requirePrior(l,anchor,frozenAt);
            if(number(l,"horizon")!=h)throw new IllegalArgumentException("PROBE_DIAGNOSTIC_HORIZON");
        }
    }
    private static void requirePrior(Map<String,Object> label,String anchor,long now) {
        if(!"SETTLED".equals(string(label,"status"))||!ROLE.equals(string(label,"role"))||string(label,"targetIssue").compareTo(anchor)>0||number(label,"completedAt")>=now)
            throw new IllegalArgumentException("PROBE_UNSETTLED_OR_FUTURE_RESIDUAL");
    }
    private static void verifyHistoryObservation(SQLiteDatabase db,String anchor,long frozenAt) {
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM (SELECT first_seen_at FROM "+DbHelper.TABLE_DRAWS+" WHERE issue<=? ORDER BY issue DESC LIMIT 960) WHERE first_seen_at>0 AND first_seen_at<=?",new String[]{anchor,Long.toString(frozenAt)})) {
            if(!c.moveToFirst()||c.getLong(0)!=960)throw new IllegalArgumentException("PROBE_PREFIX_OBSERVATION_CLOCK");
        }
    }
    private static String historyRevision(SQLiteDatabase db,String anchor) {
        try(Cursor c=db.rawQuery("SELECT COUNT(*),COALESCE(MAX(r.generation),0) FROM (SELECT rowid AS draw_rowid FROM "+DbHelper.TABLE_DRAWS+" WHERE issue<=? ORDER BY issue DESC LIMIT 960) d JOIN research_row_revision_v2 r ON r.source_table='draws' AND r.source_rowid=d.draw_rowid",new String[]{anchor})) {
            if(!c.moveToFirst()||c.getLong(0)!=960||c.getLong(1)<=0)throw new IllegalArgumentException("PROBE_HISTORY_REVISION_UNAVAILABLE");
            return c.getLong(0)+":"+c.getLong(1);
        }
    }
    private static Map<String,Object> readClock(SQLiteDatabase db,String issue,long now) {
        try(Cursor c=db.rawQuery("SELECT issue,open_time,fetch_time,first_seen_at,draw_hash_v2,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue=?",new String[]{issue})) {
            if(!c.moveToFirst()||c.getInt(15)!=0||c.getLong(1)<=0||c.getLong(2)<=0||c.getLong(3)<=0||c.getLong(1)>now||c.getLong(2)>now||c.getLong(3)>now)throw new IllegalArgumentException("PROBE_SOURCE_CLOCK_UNAVAILABLE");
            Map<String,Object> m=new TreeMap<>();m.put("issue",c.getString(0));m.put("openTime",c.getLong(1));m.put("fetchTime",c.getLong(2));m.put("firstSeenAt",c.getLong(3));m.put("drawHash",c.getString(4));
            List<Integer> numbers=new ArrayList<>();for(int i=5;i<15;i++)numbers.add(c.getInt(i));m.put("numbers",numbers);return m;
        }
    }
    private static Map<String,Object> readActual(SQLiteDatabase db,Models.Draw draw) {
        Map<String,Object> m=readClock(db,draw.issue,Long.MAX_VALUE);List<?> values=list(m,"numbers");
        if(number(m,"openTime")!=draw.openTime)throw new IllegalArgumentException("PROBE_ACTUAL_CLOCK_CHANGED");
        for(int i=0;i<10;i++)if(((Number)values.get(i)).intValue()!=draw.numbers[i])throw new IllegalArgumentException("PROBE_ACTUAL_DRAW_CHANGED");return m;
    }
    private static Map<String,Object> opportunity(String anchor,String status,String reason,String consensus,long now) {
        Map<String,Object> o=base();o.put("anchorIssue",anchor);o.put("targetIssue",target(anchor,1));o.put("status",status);o.put("reason",reason);o.put("consensusStatus",consensus);o.put("observedAt",now);return o;
    }
    private static void writeOpportunity(SQLiteDatabase db,Map<String,Object> o) {
        if(exists(db,OPPORTUNITY,"anchor_issue",string(o,"anchorIssue")))return;
        String json=StrictCanonicalJson.write(o);db.execSQL("INSERT INTO "+OPPORTUNITY+" VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{string(o,"anchorIssue"),string(o,"targetIssue"),PROTOCOL_HASH,string(o,"status"),string(o,"reason"),string(o,"consensusStatus"),number(o,"observedAt"),json,hash(json)});
    }
    static void verifyForecast(Map<String,Object> f) {
        if(!ROLE.equals(string(f,"role"))||!PROTOCOL_HASH.equals(string(f,"protocolHash"))||!Boolean.FALSE.equals(f.get("mayCreateReal")))throw new IllegalArgumentException("PROBE_ROLE_PROTOCOL");
        int h=(int)number(f,"horizon");if(h!=0&&!validHorizon(h))throw new IllegalArgumentException("PROBE_HORIZON");
        if(!target(string(f,"anchorIssue"),h==0?1:h).equals(string(f,"targetIssue")))throw new IllegalArgumentException("PROBE_EXACT_TARGET");
        long frozen=number(f,"frozenAt"),next=number(f,"nextBoundary");
        if(frozen<=0||frozen>=next||number(f,"targetBoundary")!=Math.addExact(next,Math.max(0,h-1)*60000L))throw new IllegalArgumentException("PROBE_FROZEN_CLOCK");
        requireHash(string(f,"inputHash"));requireHash(string(f,"modelHash"));requireHash(string(f,"sourceCodeHash"));double[] q=distribution(f);int m=mask(q);
        if(number(f,"mask")!=m||!Double.toHexString(mass(q,m)).equals(string(f,"rawMass"))||!Boolean.valueOf(mass(q,m)>=.40d).equals(f.get("threshold040")))throw new IllegalArgumentException("PROBE_DISTRIBUTION_DRIFT");
    }
    static Map<String,Object> verified(String json,String expected) {
        requireHash(expected);if(json==null||!hash(json).equals(expected))throw new IllegalArgumentException("PROBE_RECEIPT_HASH");Map<String,Object> m=StrictCanonicalJson.readObject(json);
        if(!StrictCanonicalJson.write(m).equals(json))throw new IllegalArgumentException("PROBE_RECEIPT_CANONICAL");return m;
    }
    private static Map<String,Object> base(){Map<String,Object> m=new TreeMap<>();m.put("role",ROLE);m.put("protocolHash",PROTOCOL_HASH);m.put("mayCreateReal",false);m.put("probabilityKind","RAW_NOT_CALIBRATED");m.put("sourceCodeHash",TrainingCodeIdentityR964.SHA256);return m;}
    private static boolean validHorizon(int h){return h==1||h==2||h==4||h==8;}
    private static boolean exists(SQLiteDatabase db,String table,String column,String value){try(Cursor c=db.rawQuery("SELECT 1 FROM "+table+" WHERE "+column+"=? LIMIT 1",new String[]{value})){return c.moveToFirst();}}
    private static String hash(String s){return V51ChecksumEngine.sha256(s);}
    private static void requireHash(String s){if(s==null||!s.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("PROBE_HASH");}
    private static void requireWriter(SQLiteDatabase db){if(db==null||!db.inTransaction())throw new IllegalStateException("PROBE_EXISTING_WRITER_REQUIRED");}
    private static void requireDistribution(double[] q){if(q==null||q.length!=10)throw new IllegalArgumentException("PROBE_Q_SHAPE");double sum=0;for(double p:q){if(!Double.isFinite(p)||p<0||p>1)throw new IllegalArgumentException("PROBE_Q_FINITE");sum+=p;}if(Math.abs(sum-1)>1e-12)throw new IllegalArgumentException("PROBE_Q_SUM");}
    private static List<String> hex(double[] q){List<String> out=new ArrayList<>();for(double p:q)out.add(Double.toHexString(p));return out;}
    private static double[] distribution(Map<String,Object> m){List<?> a=list(m,"distribution");if(a.size()!=10)throw new IllegalArgumentException("PROBE_Q_SIZE");double[] q=new double[10];for(int i=0;i<10;i++){Object v=a.get(i);if(!(v instanceof String))throw new IllegalArgumentException("PROBE_Q_TYPE");q[i]=Double.parseDouble((String)v);if(!Double.toHexString(q[i]).equals(v))throw new IllegalArgumentException("PROBE_Q_CANONICAL");}requireDistribution(q);return q;}
    private static String string(Map<String,Object> m,String key){Object v=m.get(key);if(!(v instanceof String))throw new IllegalArgumentException("PROBE_STRING_"+key);return (String)v;}
    private static long number(Map<String,Object> m,String key){Object v=m.get(key);if(!(v instanceof Number))throw new IllegalArgumentException("PROBE_NUMBER_"+key);return ((Number)v).longValue();}
    private static double decimal(Map<String,Object> m,String key){String s=string(m,key);double d=Double.parseDouble(s);if(!Double.isFinite(d)||!Double.toHexString(d).equals(s))throw new IllegalArgumentException("PROBE_DECIMAL");return d;}
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Map<String,Object> m,String key){Object v=m.get(key);if(!(v instanceof Map))throw new IllegalArgumentException("PROBE_OBJECT_"+key);return (Map<String,Object>)v;}
    private static List<?> list(Map<String,Object> m,String key){Object v=m.get(key);if(!(v instanceof List))throw new IllegalArgumentException("PROBE_LIST_"+key);return (List<?>)v;}
}
