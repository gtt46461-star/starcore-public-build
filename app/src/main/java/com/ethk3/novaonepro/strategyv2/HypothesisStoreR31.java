package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.*;

/** Durable development-only failure hypotheses. The existing fenced foundry
 * owner registers births, commits prequential SELECT traces with checkpoints,
 * and publishes paired outcomes with the campaign finish. Never reads formal
 * confirmation/future rows and never issues a certificate. */
public final class HypothesisStoreR31 {
    private HypothesisStoreR31() {}
    public static final String VERSION="R31_HYPOTHESIS_STORE_1";
    public static final int MAX_SAMPLES=5000,MAX_BINDINGS=16;
    private static final String REG="r31_hypothesis_registry",BIND="r31_hypothesis_binding",TRACE="r31_hypothesis_select",OUT="r31_hypothesis_outcome",QUOTA="r31_hypothesis_quota";
    public static String[] ddlR31(){
        List<String> sql=new ArrayList<>();
        sql.add("CREATE TABLE IF NOT EXISTS "+REG+"(hypothesis_id TEXT PRIMARY KEY,parent_policy TEXT NOT NULL,source_run TEXT NOT NULL,family TEXT NOT NULL,status TEXT NOT NULL,payload TEXT NOT NULL CHECK(length(CAST(payload AS BLOB))<=16384),payload_hash TEXT NOT NULL,registered_at INTEGER NOT NULL)");
        sql.add("CREATE TABLE IF NOT EXISTS "+BIND+"(campaign_id TEXT NOT NULL,child_policy TEXT NOT NULL,hypothesis_id TEXT NOT NULL,control_policy TEXT NOT NULL,snapshot_hash TEXT NOT NULL,select_from INTEGER NOT NULL,select_rows INTEGER NOT NULL CHECK(select_rows BETWEEN 1 AND 5000),ordinal_digest TEXT NOT NULL,source_through INTEGER NOT NULL,PRIMARY KEY(campaign_id,child_policy)) WITHOUT ROWID");
        sql.add("CREATE TABLE IF NOT EXISTS "+TRACE+"(campaign_id TEXT NOT NULL,policy_hash TEXT NOT NULL,sample_index INTEGER NOT NULL,ordinal INTEGER NOT NULL,snapshot_hash TEXT NOT NULL,position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 10),mask INTEGER NOT NULL CHECK(mask BETWEEN 1 AND 1023),act INTEGER NOT NULL CHECK(act IN (0,1)),hit INTEGER NOT NULL CHECK(hit IN (0,1)),phat REAL,trace_hash TEXT NOT NULL,PRIMARY KEY(campaign_id,policy_hash,sample_index),UNIQUE(campaign_id,policy_hash,ordinal)) WITHOUT ROWID");
        sql.add("CREATE TABLE IF NOT EXISTS "+OUT+"(campaign_id TEXT NOT NULL,child_policy TEXT NOT NULL,hypothesis_id TEXT NOT NULL,family TEXT NOT NULL,role TEXT NOT NULL CHECK(role='SELECT'),state TEXT NOT NULL,paired_rows INTEGER NOT NULL,gain REAL,cost_penalty REAL,net_credit REAL,payload TEXT NOT NULL CHECK(length(CAST(payload AS BLOB))<=16384),payload_hash TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(campaign_id,child_policy)) WITHOUT ROWID");
        sql.add("CREATE TABLE IF NOT EXISTS "+QUOTA+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),logical_bytes INTEGER NOT NULL CHECK(logical_bytes BETWEEN 0 AND 268435456))");
        for(String table:new String[]{REG,BIND,TRACE,OUT}){
            String match=table.equals(REG)?"hypothesis_id=NEW.hypothesis_id":table.equals(TRACE)?"campaign_id=NEW.campaign_id AND policy_hash=NEW.policy_hash AND sample_index=NEW.sample_index":"campaign_id=NEW.campaign_id AND child_policy=NEW.child_policy";
            String[] equalColumns=table.equals(REG)?new String[]{"parent_policy","source_run","family","status","payload","payload_hash"}:table.equals(BIND)?new String[]{"hypothesis_id","control_policy","snapshot_hash","select_from","select_rows","ordinal_digest","source_through"}:table.equals(TRACE)?new String[]{"ordinal","snapshot_hash","position","mask","act","hit","phat","trace_hash"}:new String[]{"hypothesis_id","family","role","state","paired_rows","gain","cost_penalty","net_credit","payload","payload_hash"};
            StringJoiner equal=new StringJoiner(" AND ");for(String column:equalColumns)equal.add(column+" IS NEW."+column);
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_insert BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+match+") BEGIN SELECT CASE WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+match+" AND "+equal+") THEN RAISE(IGNORE) ELSE RAISE(ABORT,'R31_HYPOTHESIS_ORIGINAL_CONFLICT') END; END");
            for(String action:new String[]{"UPDATE","DELETE"})sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_"+action.toLowerCase(Locale.ROOT)+" BEFORE "+action+" ON "+table+" BEGIN SELECT RAISE(ABORT,'R31_HYPOTHESIS_ORIGINAL_IMMUTABLE'); END");
            String key=table.equals(REG)?"hypothesis_id=NEW.hypothesis_id":table.equals(TRACE)?"campaign_id=NEW.campaign_id AND policy_hash=NEW.policy_hash AND sample_index=NEW.sample_index":"campaign_id=NEW.campaign_id AND child_policy=NEW.child_policy";
            String cost=table.equals(REG)||table.equals(OUT)?"(1024+length(CAST(NEW.payload AS BLOB)))":"1024";
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_admit BEFORE INSERT ON "+table+" WHEN NOT EXISTS(SELECT 1 FROM "+table+" WHERE "+key+") BEGIN SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM "+QUOTA+" WHERE singleton=1 AND logical_bytes+"+cost+"<=268435456) THEN RAISE(ABORT,'R31_HYPOTHESIS_PROTECTED_STORAGE_WAIT') END; END");
            sql.add("CREATE TRIGGER IF NOT EXISTS "+table+"_account AFTER INSERT ON "+table+" BEGIN UPDATE "+QUOTA+" SET logical_bytes=logical_bytes+"+cost+" WHERE singleton=1; END");
        }
        return sql.toArray(new String[0]);
    }
    /** Called exclusively by the journalled schema owner, never by business code. */
    public static void installWithinMigrationR31(SQLiteDatabase db){
        writer(db);for(String sql:ddlR31())db.execSQL(sql);
        initializeWithinR31(db);requireSchemaR31(db);
    }
    public static void initializeWithinR31(SQLiteDatabase db){seedQuotaWithinMigrationR31(db);}
    public static void seedQuotaWithinMigrationR31(SQLiteDatabase db){
        writer(db);try(Cursor c=db.rawQuery("SELECT 1 FROM "+QUOTA+" WHERE singleton=1",null)){if(c.moveToFirst())return;}
        for(String table:new String[]{REG,BIND,TRACE,OUT})try(Cursor c=db.rawQuery("SELECT 1 FROM "+table+" LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("R31_HYPOTHESIS_DATA_WITHOUT_ACCOUNTING");}
        db.execSQL("INSERT INTO "+QUOTA+" VALUES(1,0)");
    }
    public static void requireSchemaR31(SQLiteDatabase db){
        for(String sql:ddlR31()){
            String[] fields=sql.split(" ");String name=fields[5].split("\\(",2)[0];
            try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type=? AND name=?",new String[]{fields[1].toLowerCase(Locale.ROOT),name})){
                if(!c.moveToFirst()||!normalize(sql).equals(normalize(c.getString(0))))throw new DatabaseCoordinator.SchemaPendingR31(VERSION,"EXACT_OBJECT_CONTRACT:"+name);
            }
        }
        try(Cursor c=db.rawQuery("SELECT logical_bytes FROM "+QUOTA+" WHERE singleton=1",null)){if(!c.moveToFirst())throw new DatabaseCoordinator.SchemaPendingR31(VERSION,"QUOTA_MISSING");}
    }
    private static String normalize(String sql){return sql.replaceFirst("^(CREATE (?:UNIQUE )?(?:TABLE|INDEX|TRIGGER) )IF NOT EXISTS ","$1").trim();}
    private static void writer(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R31_HYPOTHESIS_EXISTING_WRITER_REQUIRED");}
    @SuppressWarnings("unchecked") private static Map<String,Object> object(Object raw){if(!(raw instanceof Map))throw new IllegalArgumentException("R31_HYPOTHESIS_OBJECT");return (Map<String,Object>)raw;}
    private static long n(Map<String,Object> row,String key){return ((Number)row.get(key)).longValue();}
    private static String s(Map<String,Object> row,String key){return (String)row.get(key);}
    private static ResearchHypothesisR31.Hypothesis hypothesis(Map<String,Object> metadata){
        return ResearchHypothesisR31.fromFailure(new ResearchHypothesisR31.Failure(s(metadata,"parentPolicy"),s(metadata,"failureEvidenceDigest"),ResearchHypothesisR31.mechanismOf(s(metadata,"failureReason")),s(metadata,"failureReason"),"","REVEALED_FAILED_LOOK",n(metadata,"failureWindowToExclusive"),n(metadata,"failureWindowToExclusive")-1,n(metadata,"failureActs"),((Number)metadata.get("failureRate")).doubleValue()));
    }
    /** Birth, lineage, cursor and this registration share the original final publication transaction. */
    public static void registerWithinR31(SQLiteDatabase db,String campaign,String snapshot,int samples,String ordinalDigest,long through,String metadataText,long at){
        writer(db);Map<String,Object> metadata=object(JsonR27.parse(metadataText));ResearchHypothesisR31.Hypothesis h=hypothesis(metadata);
        if(!Objects.equals(snapshot,metadata.get("researchSnapshot"))||n(metadata,"sourceThrough")!=through||samples<64||samples>MAX_SAMPLES||ordinalDigest==null||!ordinalDigest.matches("[0-9a-f]{64}"))throw new IllegalStateException("R31_HYPOTHESIS_BIRTH_SOURCE");
        ResearchHypothesisR31.Registry registry=new ResearchHypothesisR31.Registry();registry.register(h);String dispatch=registry.dispatch(h);
        if(!dispatch.equals(metadata.get("dispatchR31"))||!h.json().equals(object(metadata.get("hypothesisR31"))))throw new IllegalStateException("R31_HYPOTHESIS_BIRTH_CONTRACT");
        List<?> children=(List<?>)metadata.get("children");String actualStatus=h.supported()&&children.isEmpty()?"UNSUPPORTED_ADAPTER":h.status;
        String payload=JsonR27.write(h.json()),hash=StatsR27.sha256(payload);
        exact(db,"SELECT payload_hash FROM "+REG+" WHERE hypothesis_id=?",new String[]{h.id},hash,"R31_HYPOTHESIS_REGISTRY_CONFLICT");
        db.execSQL("INSERT OR IGNORE INTO "+REG+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{h.id,h.parentHash,h.sourceRun,h.family,actualStatus,payload,hash,at});
        requireExact(db,"SELECT payload_hash FROM "+REG+" WHERE hypothesis_id=?",new String[]{h.id},hash);
        String control=null;
        for(Object value:children){Map<String,Object> row=object(value);if("PRESERVE_PARENT_STRUCTURE".equals(row.get("operator")))control=s(row,"policy");}
        if(children.isEmpty())return;
        if(!h.supported()||control==null)throw new IllegalStateException("R31_HYPOTHESIS_CONTROL_MISSING");
        if(children.size()>3)throw new IllegalStateException("R31_HYPOTHESIS_CHILD_BOUND");
        for(Object value:children){Map<String,Object> row=object(value);String child=s(row,"policy");
            try(Cursor c=db.rawQuery("SELECT canonical_text FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{campaign,child})){
                if(!c.moveToFirst()||!CurrentPolicyR26.matches(c.getString(0),child)||!ModelGenomeSU2.parse(c.getString(0)).bindBirth(snapshot,through).text().equals(c.getString(0)))throw new IllegalStateException("R31_HYPOTHESIS_UNREGISTERED_CHILD");
            }
            // Same canonical seed/control may have won the candidate's first
            // insert. Its real failure-guided attempt lineage still has to bind
            // this exact hypothesis, intervention, child and frozen snapshot.
            boolean lineage=false;
            try(Cursor c=db.rawQuery("SELECT operation FROM r26_evolution WHERE campaign_id=? AND policy_hash=? AND parent=? AND instr(operation,'FAILURE_GUIDED_V1:')=1 LIMIT 16",new String[]{campaign,child,h.parentHash})){
                while(c.moveToNext()){
                    Map<String,Object> event=object(JsonR27.parse(c.getString(0).substring("FAILURE_GUIDED_V1:".length())));
                    if(child.equals(event.get("childPolicy"))&&Objects.equals(row.get("operator"),event.get("operator"))&&snapshot.equals(event.get("researchSnapshot"))&&h.json().equals(object(event.get("hypothesisR31"))))lineage=true;
                }
            }
            if(!lineage)throw new IllegalStateException("R31_HYPOTHESIS_CHILD_LINEAGE_MISSING");
            if(child.equals(control))continue;
            db.execSQL("INSERT OR IGNORE INTO "+BIND+" VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{campaign,child,h.id,control,snapshot,samples*3/5,samples-samples*3/5,ordinalDigest,through});
            try(Cursor c=db.rawQuery("SELECT hypothesis_id,control_policy,snapshot_hash,select_from,select_rows,ordinal_digest,source_through FROM "+BIND+" WHERE campaign_id=? AND child_policy=?",new String[]{campaign,child})){
                if(!c.moveToFirst()||!h.id.equals(c.getString(0))||!control.equals(c.getString(1))||!snapshot.equals(c.getString(2))||c.getInt(3)!=samples*3/5||c.getInt(4)!=samples-samples*3/5||!ordinalDigest.equals(c.getString(5))||c.getLong(6)!=through)throw new IllegalStateException("R31_HYPOTHESIS_BINDING_CONFLICT");
            }
        }
    }
    private static void exact(SQLiteDatabase db,String sql,String[] args,String expected,String code){try(Cursor c=db.rawQuery(sql,args)){if(c.moveToFirst()&&!expected.equals(c.getString(0)))throw new IllegalStateException(code);}}
    private static void requireExact(SQLiteDatabase db,String sql,String[] args,String expected){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst()||!expected.equals(c.getString(0)))throw new IllegalStateException("R31_HYPOTHESIS_DURABLE_ROW_NOT_COMMITTED");}}
    public static boolean tracksPolicyR31(SQLiteDatabase db,String campaign,String policy){
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+BIND+" WHERE campaign_id=? AND (child_policy=? OR control_policy=?) LIMIT 1",new String[]{campaign,policy,policy})){return c.moveToFirst();}
    }
    public static final class Sample {
        final int index,position,mask;final long ordinal;final boolean act,hit;final double phat;
        public Sample(int index,long ordinal,ModelLiveSU2.Decision decision,boolean hit){this.index=index;this.ordinal=ordinal;position=decision.position;mask=decision.mask;act=decision.act;this.hit=hit;phat=decision.phat;}
    }
    /** Trace rows are exposed-history SELECT opportunities, never formal rows.
     * Their checkpoint/cursor transaction either commits all samples or none. */
    public static void recordSelectWithinR31(SQLiteDatabase db,String campaign,String policy,String snapshot,List<Sample> samples){
        writer(db);if(samples.size()>16)throw new IllegalArgumentException("R31_HYPOTHESIS_TRACE_SLICE");
        for(Sample p:samples){
            if(p.position<1||p.position>10||Integer.bitCount(p.mask)!=3||p.mask>=1024||p.mask<=0)throw new IllegalArgumentException("R31_HYPOTHESIS_SINGLE_POSITION_K3");
            try(Cursor c=db.rawQuery("SELECT 1 FROM "+BIND+" WHERE campaign_id=? AND (child_policy=? OR control_policy=?) AND snapshot_hash=? AND CAST(? AS INTEGER)>=select_from AND CAST(? AS INTEGER)<select_from+select_rows AND CAST(? AS INTEGER)<=source_through LIMIT 1",new String[]{campaign,policy,policy,snapshot,""+p.index,""+p.index,""+p.ordinal})){
                if(!c.moveToFirst())throw new IllegalStateException("R31_HYPOTHESIS_TRACE_OUTSIDE_SELECT");
            }
            Double phat=Double.isFinite(p.phat)&&p.phat>=0&&p.phat<=1?p.phat:null;
            String hash=StatsR27.sha256(JsonR27.write(Arrays.asList(campaign,policy,p.index,p.ordinal,snapshot,p.position,p.mask,p.act,p.hit,phat)));
            exact(db,"SELECT trace_hash FROM "+TRACE+" WHERE campaign_id=? AND policy_hash=? AND sample_index=?",new String[]{campaign,policy,""+p.index},hash,"R31_HYPOTHESIS_SELECT_CONFLICT");
            db.execSQL("INSERT OR IGNORE INTO "+TRACE+" VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{campaign,policy,p.index,p.ordinal,snapshot,p.position,p.mask,p.act,p.hit,phat,hash});
            requireExact(db,"SELECT trace_hash FROM "+TRACE+" WHERE campaign_id=? AND policy_hash=? AND sample_index=?",new String[]{campaign,policy,""+p.index},hash);
        }
    }
    private static final class Trace {final List<Long> ordinals=new ArrayList<>();final List<String> hashes=new ArrayList<>();double utility,brier;long acts,hits,calibrated;boolean complete=true;}
    private static Trace trace(SQLiteDatabase db,String campaign,String policy,String source,int from,int rows){
        Trace t=new Trace();try(Cursor c=db.rawQuery("SELECT sample_index,ordinal,snapshot_hash,act,hit,phat,trace_hash FROM "+TRACE+" WHERE campaign_id=? AND policy_hash=? ORDER BY sample_index LIMIT 5001",new String[]{campaign,policy})){
            while(c.moveToNext()){if(t.ordinals.size()>=MAX_SAMPLES)throw new IllegalStateException("R31_HYPOTHESIS_TRACE_BOUND");
                if(c.getInt(0)!=from+t.ordinals.size()||!source.equals(c.getString(2)))t.complete=false;
                t.ordinals.add(c.getLong(1));t.hashes.add(c.getString(6));boolean act=c.getInt(3)==1,hit=c.getInt(4)==1;
                if(act){t.acts++;if(hit)t.hits++;t.utility+=(hit?1:0)-StatsR27.BASE;}
                if(!c.isNull(5)){double error=c.getDouble(5)-(hit?1:0);t.brier+=error*error;t.calibrated++;}
            }
        }t.complete&=t.ordinals.size()==rows;return t;
    }
    /** Prepared outside the writer. Immutable trace hashes, exact cardinality,
     * snapshot and ordinal digest are guarded again at final publication. */
    public static String prepareOutcomesR31(SQLiteDatabase db,String campaign){
        List<Object> outcomes=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT b.child_policy,b.hypothesis_id,b.control_policy,b.snapshot_hash,b.select_from,b.select_rows,b.ordinal_digest,r.payload FROM "+BIND+" b JOIN "+REG+" r ON r.hypothesis_id=b.hypothesis_id WHERE b.campaign_id=? ORDER BY b.child_policy LIMIT 17",new String[]{campaign})){
            while(c.moveToNext()){
                if(outcomes.size()>=MAX_BINDINGS)throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_BOUND");
                String child=c.getString(0),control=c.getString(2),source=c.getString(3);int from=c.getInt(4),rows=c.getInt(5);Map<String,Object> h=object(JsonR27.parse(c.getString(7)));
                Trace a=trace(db,campaign,child,source,from,rows),b=trace(db,campaign,control,source,from,rows);
                Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("campaign",campaign);out.put("child",child);out.put("control",control);out.put("hypothesis",c.getString(1));out.put("family",s(h,"family"));out.put("role","SELECT");out.put("snapshot",source);out.put("ordinalDigest",c.getString(6));out.put("expectedRows",rows);out.put("childTraceDigest",StatsR27.sha256(JsonR27.write(a.hashes)));out.put("controlTraceDigest",StatsR27.sha256(JsonR27.write(b.hashes)));out.put("childRows",a.ordinals.size());out.put("controlRows",b.ordinals.size());
                boolean paired=a.complete&&b.complete&&a.ordinals.equals(b.ordinals)&&c.getString(6).equals(StatsR27.sha256(JsonR27.write(a.ordinals)));
                String state=!paired?"MISSING_ORIGINAL_SELECT_TRACE":rows<((Number)h.get("minimumSupport")).longValue()?"INSUFFICIENT_SELECT_SUPPORT":"PAIRED_SELECT_COMPLETE";
                double cpu=0,controlCpu=0;boolean candidatesComplete=true;List<String> candidates=new ArrayList<>(),candidateStates=new ArrayList<>();List<Integer> candidateCursors=new ArrayList<>();
                for(String policy:new String[]{child,control})try(Cursor p=db.rawQuery("SELECT status,cursor,state_hash,checkpoint_hash,service_cpu_ms FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{campaign,policy})){
                    if(!p.moveToFirst())throw new IllegalStateException("R31_HYPOTHESIS_BOUND_CANDIDATE_MISSING");
                    candidateStates.add(p.getString(0));candidateCursors.add(p.getInt(1));candidatesComplete&="DONE".equals(p.getString(0))&&p.getInt(1)==from+rows;
                    candidates.add(p.getString(2));candidates.add(p.getString(3));if(policy.equals(child))cpu=p.getDouble(4);else controlCpu=p.getDouble(4);
                }
                if(!candidatesComplete)state="CANDIDATE_NOT_SCORED";
                out.put("candidateDigests",candidates);out.put("candidateStates",candidateStates);out.put("candidateCursors",candidateCursors);out.put("state",state);out.put("pairedRows",paired?rows:0);out.put("pairedUtilityGain",paired?(a.utility-b.utility)/rows:null);out.put("childActs",a.acts);out.put("controlActs",b.acts);out.put("childHits",a.hits);out.put("controlHits",b.hits);out.put("childBrier",a.calibrated==rows?a.brier/rows:null);out.put("controlBrier",b.calibrated==rows?b.brier/rows:null);out.put("childCpuMs",cpu);out.put("controlCpuMs",controlCpu);out.put("costRule","CHILD_CPU_MINUTES_PER_SELECT_OPPORTUNITY");out.put("costPenalty",cpu/(60000.0*rows));out.put("formalEvidenceInherited",false);out.put("naturalQuality","NOT_PROVEN_EXPOSED_HISTORY_ONLY");
                Double net=null;
                if("PAIRED_SELECT_COMPLETE".equals(state)){
                    ResearchHypothesisR31.Hypothesis typed=ResearchHypothesisR31.restoreRegisteredR31(h);
                    ResearchHypothesisR31.Registry registry=new ResearchHypothesisR31.Registry();registry.register(typed);
                    registry.recordOutcome(typed,child,"SELECT",((Number)out.get("pairedUtilityGain")).doubleValue(),((Number)out.get("costPenalty")).doubleValue());net=registry.developmentCredit(typed.family);
                }
                out.put("netCredit",net);outcomes.add(out);
            }
        }return JsonR27.write(outcomes);
    }
    public static void publishOutcomesWithinR31(SQLiteDatabase db,String campaign,String prepared,long at){
        writer(db);List<?> outcomes=(List<?>)JsonR27.parse(prepared);if(outcomes.size()>MAX_BINDINGS)throw new IllegalArgumentException("R31_HYPOTHESIS_OUTCOME_BOUND");
        for(Object item:outcomes){Map<String,Object> row=object(item);if(!campaign.equals(row.get("campaign"))||!"SELECT".equals(row.get("role")))throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_ROLE");
            try(Cursor binding=db.rawQuery("SELECT b.control_policy,b.snapshot_hash,b.ordinal_digest,b.select_rows,b.hypothesis_id,r.family,r.payload FROM "+BIND+" b JOIN "+REG+" r ON r.hypothesis_id=b.hypothesis_id WHERE b.campaign_id=? AND b.child_policy=?",new String[]{campaign,s(row,"child")})){
                if(!binding.moveToFirst()||!Objects.equals(row.get("control"),binding.getString(0))||!Objects.equals(row.get("snapshot"),binding.getString(1))||!Objects.equals(row.get("ordinalDigest"),binding.getString(2))||n(row,"expectedRows")!=binding.getInt(3)||!Objects.equals(row.get("hypothesis"),binding.getString(4))||!Objects.equals(row.get("family"),binding.getString(5)))throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_BINDING");
                if("PAIRED_SELECT_COMPLETE".equals(row.get("state"))){
                    long minimum=((Number)object(JsonR27.parse(binding.getString(6))).get("minimumSupport")).longValue();
                    if(n(row,"pairedRows")!=binding.getInt(3)||n(row,"pairedRows")<minimum)throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_SUPPORT");
                    try(Cursor measured=db.rawQuery("SELECT count(*),sum(CASE WHEN a.act=1 THEN a.hit-0.30 ELSE 0 END)-sum(CASE WHEN b.act=1 THEN b.hit-0.30 ELSE 0 END) FROM "+TRACE+" a JOIN "+TRACE+" b ON b.campaign_id=a.campaign_id AND b.sample_index=a.sample_index AND b.ordinal=a.ordinal AND b.snapshot_hash=a.snapshot_hash WHERE a.campaign_id=? AND a.policy_hash=? AND b.policy_hash=? AND a.snapshot_hash=?",new String[]{campaign,s(row,"child"),s(row,"control"),s(row,"snapshot")})){
                        if(!measured.moveToFirst()||measured.getLong(0)!=n(row,"pairedRows")||Math.abs(measured.getDouble(1)/measured.getLong(0)-((Number)row.get("pairedUtilityGain")).doubleValue())>1e-10)throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_NOT_PAIRED");
                    }
                    double cpu;try(Cursor c=db.rawQuery("SELECT service_cpu_ms FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{campaign,s(row,"child")})){if(!c.moveToFirst())throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_COST_SOURCE");cpu=c.getDouble(0);}
                    double penalty=cpu/(60000.0*n(row,"pairedRows")),gain=((Number)row.get("pairedUtilityGain")).doubleValue();
                    if(!Double.isFinite(gain)||Math.abs(((Number)row.get("costPenalty")).doubleValue()-penalty)>1e-12||Math.abs(((Number)row.get("netCredit")).doubleValue()-(gain-penalty))>1e-12)throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_CREDIT_TAMPERED");
                }else if(!Arrays.asList("MISSING_ORIGINAL_SELECT_TRACE","INSUFFICIENT_SELECT_SUPPORT","CANDIDATE_NOT_SCORED").contains(row.get("state"))||row.get("netCredit")!=null)throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_UNSCORED_CREDIT");
            }
            // All traces are immutable; cardinality is checked in the same writer
            // as campaign finish, so a partly committed child cannot gain credit.
            int rows=((Number)row.get("expectedRows")).intValue();List<?> guards=(List<?>)row.get("candidateDigests"),states=(List<?>)row.get("candidateStates"),cursors=(List<?>)row.get("candidateCursors");int i=0,which=0;
            for(String policy:new String[]{s(row,"child"),s(row,"control")}){
                try(Cursor p=db.rawQuery("SELECT status,state_hash,checkpoint_hash,cursor FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{campaign,policy})){
                    if(!p.moveToFirst()||!Objects.equals(states.get(which),p.getString(0))||((Number)cursors.get(which++)).intValue()!=p.getInt(3)||!Objects.equals(guards.get(i++),p.getString(1))||!Objects.equals(guards.get(i++),p.getString(2)))throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_STALE_CANDIDATE");
                    if("PAIRED_SELECT_COMPLETE".equals(row.get("state"))&&!"DONE".equals(p.getString(0)))throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_CANDIDATE_NOT_DONE");
                }
                try(Cursor t=db.rawQuery("SELECT count(*) FROM "+TRACE+" WHERE campaign_id=? AND policy_hash=? AND snapshot_hash=?",new String[]{campaign,policy,s(row,"snapshot")})){
                    t.moveToFirst();long expected=n(row,policy.equals(s(row,"child"))?"childRows":"controlRows");if(t.getLong(0)!=expected)throw new IllegalStateException("R31_HYPOTHESIS_OUTCOME_STALE_TRACE");
                }
            }
            String payload=JsonR27.write(row),hash=StatsR27.sha256(payload);exact(db,"SELECT payload_hash FROM "+OUT+" WHERE campaign_id=? AND child_policy=?",new String[]{campaign,s(row,"child")},hash,"R31_HYPOTHESIS_OUTCOME_CONFLICT");
            db.execSQL("INSERT OR IGNORE INTO "+OUT+" VALUES(?,?,?,?,'SELECT',?,?,?,?,?,?,?,?)",new Object[]{campaign,row.get("child"),row.get("hypothesis"),row.get("family"),row.get("state"),row.get("pairedRows"),row.get("pairedUtilityGain"),row.get("costPenalty"),row.get("netCredit"),payload,hash,at});
            requireExact(db,"SELECT payload_hash FROM "+OUT+" WHERE campaign_id=? AND child_policy=?",new String[]{campaign,s(row,"child")},hash);
        }
    }
    /** Conservative whole-branch reservation before birth. Existing unfinished
     * bindings remain protected debt. Resource wait removes only new failure
     * proposals; the foundry's original seeds/families continue unchanged. */
    public static boolean canAdmitResearchR31(SQLiteDatabase db,int sourceSamples){
        if(sourceSamples<64||sourceSamples>MAX_SAMPLES)return false;
        long used;try(Cursor c=db.rawQuery("SELECT logical_bytes FROM "+QUOTA+" WHERE singleton=1",null)){if(!c.moveToFirst())return false;used=c.getLong(0);}
        long debt;try(Cursor c=db.rawQuery("SELECT coalesce(sum(2*select_rows*1024+32768),0) FROM "+BIND+" b WHERE NOT EXISTS(SELECT 1 FROM "+OUT+" o WHERE o.campaign_id=b.campaign_id AND o.child_policy=b.child_policy)",null)){if(!c.moveToFirst())return false;debt=c.getLong(0);}
        long next=3L*(3L*(sourceSamples-sourceSamples*3/5)*1024+131072);
        return used<=268435456L-debt-next;
    }
    /** Next research allocation uses development credit only. Sparse families
     * keep zero prior; negative outcomes remain negative, never fake reward. */
    public static double creditR31(SQLiteDatabase db,String family){
        try(Cursor c=db.rawQuery("SELECT count(*),avg(net_credit) FROM "+OUT+" WHERE family=? AND role='SELECT' AND state='PAIRED_SELECT_COMPLETE'",new String[]{family})){
            if(!c.moveToFirst()||c.getLong(0)<CreationBudgetR31.MIN_CREDIT_SUPPORT||c.isNull(1))return 0;double value=c.getDouble(1);if(!Double.isFinite(value))throw new IllegalStateException("R31_HYPOTHESIS_CREDIT_NONFINITE");return Math.max(-1,Math.min(1,value));
        }
    }
    /** Bounded report projection. Missing schema or old untraced experiments
     * are explicit unknowns, never fabricated zero outcomes or a PASS. */
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("role","SELECT_ONLY");out.put("naturalQuality","NOT_PROVEN");out.put("formalEvidenceInherited",false);
        try{
            try(Cursor c=db.rawQuery("SELECT logical_bytes FROM "+QUOTA+" WHERE singleton=1",null)){if(!c.moveToFirst())throw new IllegalStateException("R31_HYPOTHESIS_QUOTA_MISSING");out.put("logicalBytes",c.getLong(0));out.put("storageLimitBytes",268435456L);out.put("newFailureResearchAdmission",canAdmitResearchR31(db,4096)?"AVAILABLE":"RESOURCE_WAIT_PROTECTED_DEBT");}
            try(Cursor c=db.rawQuery("SELECT count(*),sum(CASE WHEN status='REGISTERED' THEN 1 ELSE 0 END) FROM "+REG,null)){c.moveToFirst();out.put("registeredHypotheses",c.getLong(0));out.put("supportedHypotheses",c.isNull(1)?0:c.getLong(1));}
            List<Object> states=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT state,count(*),sum(paired_rows) FROM "+OUT+" GROUP BY state ORDER BY state LIMIT 16",null)){while(c.moveToNext())states.add(Arrays.asList(c.getString(0),c.getLong(1),c.getLong(2)));}out.put("outcomeStates",states);
            try(Cursor c=db.rawQuery("SELECT count(*) FROM "+BIND+" b WHERE NOT EXISTS(SELECT 1 FROM "+OUT+" o WHERE o.campaign_id=b.campaign_id AND o.child_policy=b.child_policy)",null)){c.moveToFirst();out.put("pendingBindings",c.getLong(0));}
            try(Cursor c=db.rawQuery("SELECT count(*) FROM r26_candidate c WHERE instr(operation,'FAILURE_GUIDED_V1:')=1 AND NOT EXISTS(SELECT 1 FROM "+BIND+" b WHERE b.campaign_id=c.campaign_id AND (b.child_policy=c.policy_hash OR b.control_policy=c.policy_hash))",null)){c.moveToFirst();out.put("legacyCandidatesWithoutTraceContract",c.getLong(0));}
            List<Object> credits=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT family,count(*),avg(net_credit) FROM "+OUT+" WHERE state='PAIRED_SELECT_COMPLETE' GROUP BY family ORDER BY family LIMIT 16",null)){while(c.moveToNext())credits.add(Arrays.asList(c.getString(0),c.getLong(1),c.isNull(2)?null:c.getDouble(2)));}out.put("familyDevelopmentCredit",credits);out.put("status","OBSERVED_DURABLE_DEVELOPMENT_REGISTRY_NOT_QUALITY_PROOF");
        }catch(RuntimeException missing){if(missing instanceof android.os.OperationCanceledException||missing instanceof java.util.concurrent.CancellationException||Thread.currentThread().isInterrupted())throw missing;out.put("status","UNAVAILABLE_REGISTRY_OR_SOURCE");out.put("registeredHypotheses",null);out.put("outcomeStates",null);out.put("familyDevelopmentCredit",null);}
        return out;
    }
}
