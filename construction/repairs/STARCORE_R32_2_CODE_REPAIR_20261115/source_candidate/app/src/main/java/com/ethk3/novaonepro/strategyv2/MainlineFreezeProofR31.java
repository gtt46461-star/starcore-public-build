package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** R27's own typed, immutable freeze contract. This never issues a certificate
 * or writes a REAL action. The existing freeze writer publishes it; the existing
 * R974/DecisionGate/canonical writer independently read it before authorisation. */
public final class MainlineFreezeProofR31 {
    private MainlineFreezeProofR31() {}
    public static final String PREFIX="R31-R27-", FORMAT="R31_R27_TYPED_FREEZE_V1";
    public static final String SELECTOR=StatsR27.sha256("R31_R27_ORIGINAL_FROZEN_ACT_K3|TARGET_POSITION_ORIGINAL_P_OTHERS_UNIFORM|RAW_SCORE_PLATT_V1");
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_mainline_freeze(policy_hash TEXT NOT NULL,target_ordinal INTEGER NOT NULL,target_issue TEXT NOT NULL,choice_id TEXT NOT NULL,certificate_digest TEXT NOT NULL,model_state_hash TEXT NOT NULL,source_revision INTEGER NOT NULL,source_schema INTEGER NOT NULL,selection_fence INTEGER NOT NULL,proof_json TEXT NOT NULL CHECK(length(CAST(proof_json AS BLOB))<=16384),proof_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(policy_hash,target_ordinal))",
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_freeze_target ON r31_mainline_freeze(target_ordinal,choice_id)",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_freeze_no_update BEFORE UPDATE ON r31_mainline_freeze BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_FREEZE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_mainline_freeze_no_delete BEFORE DELETE ON r31_mainline_freeze BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_FREEZE_IMMUTABLE'); END",
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_qualification_open ON r29_qual_conflict(experiment_id) WHERE resolved_at=0",
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_choice_current ON r31_mainline_choice(selection_fence DESC,created_at DESC) WHERE status IN ('ACTIVE','FALLBACK')",
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_choice_fence ON r31_mainline_choice(selection_fence)",
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_model_call ON r30_model_call(policy_hash,target_ordinal,position) WHERE status='MATCHED'",
        "CREATE INDEX IF NOT EXISTS idx_r31_mainline_model_publication ON r31_atomnet_publication(model_hash,weights_hash,published_at DESC,publication_id) WHERE status='PUBLISHED'"
    };
    public static boolean candidate(String value){return value!=null&&value.matches(PREFIX+"[0-9a-f]{64}");}
    public static boolean ready(SQLiteDatabase db){
        for(String expected:DDL){String declaration=expected.substring("CREATE ".length());String type=declaration.substring(0,declaration.indexOf(' ')).toLowerCase(Locale.ROOT);String tail=declaration.substring(declaration.indexOf("IF NOT EXISTS ")+"IF NOT EXISTS ".length());String name=tail.split("[ (]",2)[0];
            try(Cursor c=db.rawQuery("SELECT type,sql FROM sqlite_master WHERE name=?",new String[]{name})){if(!c.moveToFirst()||!type.equals(c.getString(0))||!expected.replaceFirst(" IF NOT EXISTS "," ").equals(c.getString(1)))return false;}
        }return SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31MainlineChoiceContract())
                &&SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31QualificationReceiptContract());
    }
    public static Map<String,Object> gateEvidence(CertificateGateR27.Evidence e){
        Map<String,Object> m=new LinkedHashMap<>();m.put("policyHash",e.policyHash);m.put("gateVersion",e.gateVersion);m.put("sequentialState",e.sequentialState);m.put("acts",e.acts);m.put("hits",e.hits);m.put("ece",finite(e.ece));m.put("worstBlock",finite(e.worstBlock));m.put("maxLosingStreak",(long)e.maxLosingStreak);m.put("abiEquivalent",e.abiEquivalent);m.put("pairedLcbVsI00",finite(e.pairedLcbVsI00));m.put("pairedLcbVsNull",finite(e.pairedLcbVsNull));return m;
    }
    private static Object finite(double n){return Double.isFinite(n)?n:null;}
    /** Snapshot the actual R27 raw-score calibrator; never the SU2 logit model. */
    public static Map<String,Object> calibrationBinding(SQLiteDatabase db,String policy){
        Map<String,Object> m=new LinkedHashMap<>();m.put("format","R27_RAW_SCORE_PLATT_V1");m.put("policyHash",policy);
        try(Cursor c=db.rawQuery("SELECT method,fit_source,fit_rows,validation_rows,a,b,ece,state,updated_at,reason,excluded_late,excluded_gap FROM r27_calibration WHERE policy_hash=?",new String[]{policy})){
            if(!c.moveToFirst()||!"PLATT".equals(c.getString(0))||!"REPLAY".equals(c.getString(1))||!"ROLLING_PLATT_PRIOR_ORDINAL_EMBARGO1_VALIDATED_FUTURE".equals(c.getString(9))||!"FITTED".equals(c.getString(7))||c.getInt(2)<CalibrationRowsR27.FIT||c.getInt(3)<CalibrationRowsR27.VALIDATION||c.isNull(4)||c.isNull(5)||c.isNull(6)||!Double.isFinite(c.getDouble(4))||!Double.isFinite(c.getDouble(5))||!Double.isFinite(c.getDouble(6))||c.getDouble(6)>CertificateGateR27.MAX_ECE||c.getLong(8)<=0){m.put("state","WAIT_CALIBRATION_BINDING");return m;}
            m.put("method",c.getString(0));m.put("fitSource",c.getString(1));m.put("fitRows",c.getLong(2));m.put("validationRows",c.getLong(3));m.put("a",c.getDouble(4));m.put("b",c.getDouble(5));m.put("ece",c.getDouble(6));m.put("updatedAt",c.getLong(8));m.put("reason",c.getString(9));m.put("excludedLate",c.getLong(10));m.put("excludedGap",c.getLong(11));m.put("fitRole","H_SELECT");m.put("validationRole","FUTURE_READ_ONLY");m.put("state","BOUND");m.put("calibrationSha256",StatsR27.sha256(JsonR27.write(m)));return Collections.unmodifiableMap(m);
        }
    }
    public static boolean calibrationStillCommitted(SQLiteDatabase db,Map<String,Object> binding){return "BOUND".equals(binding.get("state"))&&JsonR27.write(binding).equals(JsonR27.write(calibrationBinding(db,String.valueOf(binding.get("policyHash")))));}
    private static Map<String,Object> originalRow(SQLiteDatabase db,String sql,String policy){
        Map<String,Object> row=new LinkedHashMap<>();try(Cursor q=db.rawQuery(sql,new String[]{policy})){if(!q.moveToFirst())throw new IllegalStateException("R31_R27_QUALIFICATION_ROW_MISSING");for(int i=0;i<q.getColumnCount();i++){Object value;if(q.isNull(i))value=null;else if(q.getType(i)==Cursor.FIELD_TYPE_INTEGER)value=Long.valueOf(q.getLong(i));else if(q.getType(i)==Cursor.FIELD_TYPE_FLOAT)value=Double.valueOf(q.getDouble(i));else value=q.getString(i);row.put(q.getColumnName(i),value);}}return row;
    }
    /** Capture BEFORE the certificate owner's existing full integrity scan. The
     * immutable receipt guards plus global append watermark/schema detect a
     * later orphan append, mutation or guard ABA without a history scan in the
     * freeze/canonical writer. These are original bytes, not a freshness guess. */
    public static Map<String,Object> qualificationBinding(SQLiteDatabase db,String policy){
        Map<String,Object> out=new LinkedHashMap<>();out.put("policyHash",policy);out.put("format","R31_R27_QUALIFICATION_ORIGINALS_V1");
        try{
            QualificationInboxR29.requireLegacyReceiptGuardsR31(db);
            Map<String,Object> state=originalRow(db,"SELECT global_index,alpha,target_rate,log_e_up,log_e_down,n,acts,hits,sum_u,sum_u2,last_ordinal,state,updated_at FROM r27_eprocess WHERE policy_hash=?",policy);
            Map<String,Object> calendar=originalRow(db,"SELECT first_ordinal,registered_at,origin FROM r29_qual_calendar WHERE experiment_id=?",policy);
            Map<String,Object> cursor=originalRow(db,"SELECT blocked_ordinal,blocked_reason,consumed_total,cas_retries,updated_at FROM r29_qual_cursor WHERE experiment_id=?",policy);
            if(!SequentialQualificationR27.QUALIFIED.equals(state.get("state"))||number(state,"n")!=number(cursor,"consumed_total")||!(QualificationInboxR29.BLOCK_NONE.equals(cursor.get("blocked_reason"))||QualificationInboxR29.BLOCK_NOT_DUE.equals(cursor.get("blocked_reason")))||QualificationInboxR29.hasOpenConflict(db,policy)){out.put("state","WAIT_QUALIFICATION_ORIGINALS");return out;}
            long last=0;try(Cursor q=db.rawQuery("SELECT rowid FROM r29_qual_receipt ORDER BY rowid DESC LIMIT 1",null)){if(q.moveToFirst())last=q.getLong(0);}
            // NONE -> NOT_DUE merely records the already frozen next period;
            // its wall timestamp/CAS retries are not new FUTURE evidence. The
            // complete exposure state/calendar/watermark stay exact, and both
            // allowed block states plus open conflicts are checked every read.
            Map<String,Object> exposure=new LinkedHashMap<>();exposure.put("consumed_total",cursor.get("consumed_total"));exposure.put("readCondition","NONE_OR_NOT_DUE_ONLY");
            out.put("eprocess",state);out.put("calendar",calendar);out.put("cursor",exposure);out.put("receiptAppendWatermark",last);out.put("schemaVersion",SchemaR27.liveSourceSchemaVersion(db));out.put("state","BOUND");out.put("qualificationSha256",StatsR27.sha256(JsonR27.write(out)));return Collections.unmodifiableMap(out);
        }catch(RuntimeException missing){if(missing instanceof ResearchWorkBudgetR927.Yield)throw missing;out.put("state","WAIT_QUALIFICATION_ORIGINALS");return out;}
    }
    public static boolean qualificationStillCommitted(SQLiteDatabase db,Map<String,Object> binding){return "BOUND".equals(binding.get("state"))&&JsonR27.write(binding).equals(JsonR27.write(qualificationBinding(db,String.valueOf(binding.get("policyHash")))));}
    static long number(Map<?,?> m,String key){Object value=m.get(key);if(!(value instanceof Number))throw new IllegalStateException("R31_R27_PROOF_NUMBER");return ((Number)value).longValue();}
    static double decimal(Map<?,?> m,String key){Object value=m.get(key);if(!(value instanceof Number)||!Double.isFinite(((Number)value).doubleValue()))throw new IllegalStateException("R31_R27_PROOF_DECIMAL");return ((Number)value).doubleValue();}
    @SuppressWarnings("unchecked") static Map<String,Object> map(Object value){if(!(value instanceof Map))throw new IllegalStateException("R31_R27_PROOF_OBJECT");return (Map<String,Object>)value;}
    static String text(Map<?,?> m,String key){Object value=m.get(key);if(!(value instanceof String))throw new IllegalStateException("R31_R27_PROOF_TEXT");return (String)value;}
    static String gateContract(){return CertificateGateR27.GATE_VERSION+"|"+CertificateGateR27.contract().get("contractHash");}
    static final class Certificate {
        final MainlineChoiceStoreR31.Choice choice;final Map<String,Object> model,calibration;final String semanticAbi;final PolicySpecR27 spec;final long issuedAt,choiceCreatedAt;
        Certificate(MainlineChoiceStoreR31.Choice c,Map<String,Object> m,Map<String,Object> cal,String abi,PolicySpecR27 spec,long issued,long created){choice=c;model=m;calibration=cal;semanticAbi=abi;this.spec=spec;issuedAt=issued;choiceCreatedAt=created;}
    }
    /** Authenticates the current original V1 verdict and all executable state. Old
     * certificates without these bindings remain history and grant no new lane. */
    static Certificate certificate(SQLiteDatabase db,long target,long now){
        if(!"TYPED_READ".equals(MainlineChoiceStoreR31.authorityMode(db)))return null;
        MainlineChoiceStoreR31.Authorization auth=MainlineChoiceStoreR31.authorizedChoice(db,now,target);if(!auth.authorized())return null;
        MainlineChoiceStoreR31.Choice choice=MainlineChoiceStoreR31.current(db);
        if(choice==null||!choice.policyHash.matches("[0-9a-f]{64}")||!choice.policyHash.equals(choice.experimentId)||!gateContract().equals(choice.gateContract)||choice.selectionFence<0||!SchemaR27.liveSourceTriggersValid(db)||choice.sourceRevision!=SchemaR27.liveSourceRevision(db))return null;
        Map<String,Object> g;long issued;
        try(Cursor c=db.rawQuery("SELECT gates_json,evidence_digest,issued_at FROM r27_certificate WHERE policy_hash=? AND status='ACTIVE' AND gate_version=? AND issued_at<=? AND expires_at>? AND length(CAST(gates_json AS BLOB))<=65536",new String[]{choice.policyHash,CertificateGateR27.GATE_VERSION,String.valueOf(now),String.valueOf(now)})){
            if(!c.moveToFirst()||!choice.certificateDigest.equals(c.getString(1)))return null;g=map(JsonR27.parse(c.getString(0)));issued=c.getLong(2);
        }
        Map<String,Object> digestInput=new LinkedHashMap<>(g);digestInput.remove("inputHash");digestInput.remove("evaluatedAt");
        if(!choice.certificateDigest.equals(StatsR27.sha256(JsonR27.write(digestInput)))||!choice.certificateDigest.equals(g.get("inputHash"))||!CertificateGateR27.GATE_VERSION.equals(g.get("gateVersion"))||!CertificateGateR27.GATE_VERSION.equals(g.get("authority"))||!CertificateGateR27.contract().get("contractHash").equals(g.get("thresholdContractHash"))||!(g.get("failing") instanceof List)||!((List<?>)g.get("failing")).isEmpty()||number(g,"sourceRevision")!=choice.sourceRevision||number(g,"evaluatedAt")!=issued)return null;
        Map<String,Object> ev=map(g.get("gateEvidenceR31"));CertificateGateR27.Evidence evidence=new CertificateGateR27.Evidence();
        evidence.policyHash=text(ev,"policyHash");evidence.gateVersion=text(ev,"gateVersion");evidence.sequentialState=text(ev,"sequentialState");evidence.acts=number(ev,"acts");evidence.hits=number(ev,"hits");evidence.ece=decimal(ev,"ece");evidence.worstBlock=decimal(ev,"worstBlock");evidence.maxLosingStreak=Math.toIntExact(number(ev,"maxLosingStreak"));evidence.abiEquivalent=Boolean.TRUE.equals(ev.get("abiEquivalent"));evidence.pairedLcbVsI00=decimal(ev,"pairedLcbVsI00");evidence.pairedLcbVsNull=decimal(ev,"pairedLcbVsNull");
        CertificateGateR27.Verdict verdict=CertificateGateR27.evaluate(evidence);if(!choice.policyHash.equals(evidence.policyHash)||!verdict.active||evidence.hits<0||evidence.hits>evidence.acts||evidence.maxLosingStreak<0)return null;
        for(Map.Entry<String,Object> gate:verdict.gates.entrySet())if(!"gateVersion".equals(gate.getKey())&&!JsonR27.write(gate.getValue()).equals(JsonR27.write(g.get(gate.getKey()))))return null;
        Map<String,Object> model=map(g.get("modelStateBindingR31")),cal=map(g.get("calibrationBindingR31"));
        if(!choice.policyHash.equals(model.get("policyHash"))||!choice.modelStateHash.equals(model.get("bindingSha256"))||!MainlineModelBindingR31.stillCommitted(db,model)||!calibrationStillCommitted(db,cal)||Double.compare(evidence.ece,decimal(cal,"ece"))!=0||number(cal,"updatedAt")>issued||!choice.policyHash.equals(cal.get("policyHash")))return null;
        String abi=text(g,"semanticAbi");PolicySpecR27 spec;
        try(Cursor c=db.rawQuery("SELECT spec_json,semantic_abi FROM r27_live_shadow WHERE policy_hash=? AND status IN ('ACTIVE','ACTIVE_REPAIR1')",new String[]{choice.policyHash})){
            if(!c.moveToFirst()||abi.isEmpty()||!abi.equals(c.getString(1)))return null;spec=PolicySpecR27.parse(c.getString(0));if(!spec.policyHash().equals(model.get("executablePolicyHash"))||!spec.canonical().equals(model.get("policyText")))return null;
        }
        if(spec.ast.modelPositions!=0&&!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.r31AtomNetPublicationContract()))return null;
        try(Cursor c=db.rawQuery("SELECT 1 FROM r27_semantic_fingerprint WHERE policy_hash=? AND build=? AND length(fingerprint)>0 AND length(operator_abi)>0",new String[]{choice.policyHash,BuildConfig.SOURCE_COMMIT_R26})){if(!c.moveToFirst())return null;}
        try(Cursor c=db.rawQuery("SELECT state,last_ordinal,acts,global_index,alpha FROM r27_eprocess WHERE policy_hash=?",new String[]{choice.policyHash})){
            if(!c.moveToFirst()||!SequentialQualificationR27.QUALIFIED.equals(c.getString(0))||c.getLong(1)<number(g,"qualificationPrefix")||c.getLong(2)<evidence.acts||Double.compare(c.getDouble(4),SequentialQualificationR27.alpha(c.getLong(3)))!=0)return null;
        }
        Map<String,Object> qualification=map(g.get("qualificationBindingR31"));
        if(!choice.policyHash.equals(qualification.get("policyHash"))||!qualificationStillCommitted(db,qualification)||CertificateRevocationR30.blockedPolicy(db,"R27",choice.policyHash))return null;
        long created;try(Cursor q=db.rawQuery("SELECT created_at FROM r31_mainline_choice WHERE choice_id=?",new String[]{choice.choiceId})){if(!q.moveToFirst())return null;created=q.getLong(0);}
        return new Certificate(choice,model,cal,abi,spec,issued,created);
    }
    public static final class Authority {
        public final String candidate,policy,choiceId,certificate,modelStateHash,modelHash,calibrationHash,semanticAbi,target,freezeHash,recipe,proofHash;
        public final long targetOrdinal,deadlineAt,frozenAt,sourceRevision,sourceSchema,selectionFence;public final int position,n1,n2,n3;public final double selectedHitP;private final double[] probabilities;
        Authority(Certificate c,Map<String,Object> p,String json,String hash,double[] probabilities){
            policy=c.choice.policyHash;candidate=PREFIX+policy;choiceId=c.choice.choiceId;certificate=c.choice.certificateDigest;modelStateHash=c.choice.modelStateHash;modelHash=String.valueOf(c.model.getOrDefault("modelHash",modelStateHash));calibrationHash=text(c.calibration,"calibrationSha256");semanticAbi=c.semanticAbi;target=text(p,"targetIssue");targetOrdinal=number(p,"targetOrdinal");deadlineAt=number(p,"deadlineAt");frozenAt=number(p,"frozenAt");sourceRevision=c.choice.sourceRevision;sourceSchema=number(p,"sourceSchema");selectionFence=c.choice.selectionFence;position=Math.toIntExact(number(p,"position"));n1=Math.toIntExact(number(p,"n1"));n2=Math.toIntExact(number(p,"n2"));n3=Math.toIntExact(number(p,"n3"));selectedHitP=decimal(p,"selectedHitP");freezeHash=text(p,"freezeHash");recipe=json;proofHash=hash;this.probabilities=probabilities.clone();
        }
        public double[] probabilities(){return probabilities.clone();}
    }
    /** Called after the original INSERT/model-call operations, in that same
     * transaction. A shadow may freeze without REAL authority, never vice versa. */
    public static void publishWithinFreeze(SQLiteDatabase db,long target,String expectedChoice,long sourceRevision,long sourceSchema){
        if(!db.inTransaction())throw new IllegalStateException("R31_R27_FREEZE_WRITER_REQUIRED");if(!ready(db)||expectedChoice==null)return;
        long now=System.currentTimeMillis();Certificate c=certificate(db,target,now);if(c==null||!expectedChoice.equals(c.choice.choiceId)||c.choice.sourceRevision!=sourceRevision||sourceSchema!=SchemaR27.liveSourceSchemaVersion(db))return;
        Map<String,Object> p=freeze(db,c,target,now,sourceSchema);if(p==null)return;String json=JsonR27.write(p),hash=StatsR27.sha256(json);
        db.execSQL("INSERT OR IGNORE INTO r31_mainline_freeze VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{c.choice.policyHash,target,p.get("targetIssue"),c.choice.choiceId,c.choice.certificateDigest,c.choice.modelStateHash,sourceRevision,sourceSchema,c.choice.selectionFence,json,hash,now});
    }
    static Map<String,Object> freeze(SQLiteDatabase db,Certificate c,long target,long now,long schema){
        String policy=c.choice.policyHash;Map<String,Object> p=new LinkedHashMap<>();double[] probabilities;long frozen,deadline;int position,n1,n2,n3;String issue;
        try(Cursor q=db.rawQuery("SELECT target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{policy,String.valueOf(target)})){
            if(!q.moveToFirst()||q.getInt(6)!=1||!"FROZEN".equals(q.getString(7))||q.getInt(10)!=-1)return null;issue=q.getString(0);position=q.getInt(1);probabilities=StoreR27.unpack(q.getBlob(2));n1=q.getInt(3);n2=q.getInt(4);n3=q.getInt(5);frozen=q.getLong(8);deadline=q.getLong(9);
        }
        if(!TargetIssueResolver.isValid(issue)||TargetIssueResolver.sequenceOrdinalSU1(issue)!=target||position!=c.spec.position||probabilities.length!=10||!DecisionGateEngine.b12K3(n1,n2,n3)||frozen<=0||frozen>deadline-LiveShadowsR27.FREEZE_MARGIN_MS||now>=deadline||c.issuedAt>frozen||c.choiceCreatedAt>frozen||number(c.calibration,"updatedAt")>frozen)return null;
        if(c.model.containsKey("publicationId")&&(number(c.model,"publishedAt")>frozen||number(c.model,"modelAvailableAt")>frozen||number(c.model,"trainedThroughOrdinal")>=target))return null;
        double sum=0;for(double value:probabilities){if(!Double.isFinite(value)||value<0||value>1)return null;sum+=value;}if(Math.abs(sum-1)>PitGuardR30.PROB_SUM_TOLERANCE)return null;
        String base=TargetIssueResolver.fromSequenceOrdinalSU1(target-1);if(!TargetIssueResolver.exact(base,issue).valid||!LiveShadowsR27.sameDrawHead(db,base,deadline))return null;
        try(Cursor draw=db.rawQuery("SELECT 1 FROM draws WHERE issue=?",new String[]{issue})){if(draw.moveToFirst())return null;}
        if(c.spec.ast.modelPositions!=0){
            String modelHash=text(c.model,"modelHash");int count=0;
            try(Cursor q=db.rawQuery("SELECT position,model_hash,status FROM r30_model_call WHERE policy_hash=? AND target_ordinal=? AND status='MATCHED' LIMIT 11",new String[]{policy,String.valueOf(target)})){
                while(q.moveToNext()){int pos=q.getInt(0);if(pos<1||pos>10||(c.spec.ast.modelPositions&(1<<(pos-1)))==0||!modelHash.equals(q.getString(1)))return null;count|=1<<(pos-1);}
            }
            if(count!=c.spec.ast.modelPositions)return null;
        }
        double raw=probabilities[n1-1]+probabilities[n2-1]+probabilities[n3-1];double selected=CalibrationRowsR27.sigmoid(decimal(c.calibration,"a")*raw+decimal(c.calibration,"b"));if(!Double.isFinite(selected)||selected<=0||selected>=1)return null;
        p.put("format",FORMAT);p.put("policyHash",policy);p.put("choiceId",c.choice.choiceId);p.put("certificateDigest",c.choice.certificateDigest);p.put("certificateIssuedAt",c.issuedAt);p.put("choiceCreatedAt",c.choiceCreatedAt);p.put("modelStateHash",c.choice.modelStateHash);p.put("sourceRevision",c.choice.sourceRevision);p.put("sourceSchema",schema);p.put("selectionFence",c.choice.selectionFence);p.put("gateContract",c.choice.gateContract);p.put("semanticAbi",c.semanticAbi);p.put("selectorHash",SELECTOR);p.put("calibrationHash",c.calibration.get("calibrationSha256"));p.put("targetOrdinal",target);p.put("targetIssue",issue);p.put("baseIssue",base);p.put("position",(long)position);p.put("n1",(long)n1);p.put("n2",(long)n2);p.put("n3",(long)n3);p.put("probabilityHash",StatsR27.sha256(StoreR27.pack(probabilities)));p.put("frozenAt",frozen);p.put("deadlineAt",deadline);p.put("selectedHitP",selected);p.put("readout","ORIGINAL_FROZEN_TARGET_POSITION_PROBABILITIES_OTHERS_UNIFORM");p.put("calibrationFormula","R27_SIGMOID_CLAMP30_A_TIMES_RAW_K3_SUM_PLUS_B");p.put("freezeHash",StatsR27.sha256(JsonR27.write(p)));return p;
    }
    /** Pure read, bounded to one typed choice/certificate/freeze and <=10 calls. */
    public static Authority read(SQLiteDatabase db,long target,long now){
        try{
            if(!ready(db))return null;Certificate c=certificate(db,target,now);if(c==null)return null;Map<String,Object> p;String json,hash;
            try(Cursor q=db.rawQuery("SELECT proof_json,proof_hash FROM r31_mainline_freeze WHERE policy_hash=? AND target_ordinal=? AND choice_id=? AND certificate_digest=? AND model_state_hash=? AND source_revision=? AND selection_fence=? AND length(CAST(proof_json AS BLOB))<=16384",new String[]{c.choice.policyHash,String.valueOf(target),c.choice.choiceId,c.choice.certificateDigest,c.choice.modelStateHash,String.valueOf(c.choice.sourceRevision),String.valueOf(c.choice.selectionFence)})){
                if(!q.moveToFirst())return null;json=q.getString(0);hash=q.getString(1);if(!hash.equals(StatsR27.sha256(json)))return null;p=map(JsonR27.parse(json));
            }
            long schema=SchemaR27.liveSourceSchemaVersion(db);if(schema!=number(p,"sourceSchema"))return null;Map<String,Object> actual=freeze(db,c,target,now,schema);if(actual==null||!json.equals(JsonR27.write(actual)))return null;
            double[] probabilities;try(Cursor q=db.rawQuery("SELECT probabilities FROM r27_live_frozen WHERE policy_hash=? AND target_ordinal=?",new String[]{c.choice.policyHash,String.valueOf(target)})){if(!q.moveToFirst())return null;probabilities=StoreR27.unpack(q.getBlob(0));}
            MainlineChoiceStoreR31.Choice finalChoice=MainlineChoiceStoreR31.effectiveChoice(db,System.currentTimeMillis());
            if(finalChoice==null||!c.choice.choiceId.equals(finalChoice.choiceId)||c.choice.sourceRevision!=SchemaR27.liveSourceRevision(db)||schema!=SchemaR27.liveSourceSchemaVersion(db))return null;
            return new Authority(c,p,json,hash,probabilities);
        }catch(RuntimeException unavailable){if(unavailable instanceof ResearchWorkBudgetR927.Yield)throw unavailable;return null;}
    }
    public static boolean allows(DbStore store,Authority proof,String target,long deadline){
        if(store==null||proof==null||!target.equals(proof.target)||proof.deadlineAt!=deadline||!"1".equals(store.getState(RealAuthoritySU2.USER_SWITCH,"0"))||store.hasDrawIssue(target)||store.getTradeByTargetIssue(target)!=null)return false;
        try(Cursor q=store.readable().rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE target_issue=? AND decision_action IN ('BET','REAL','REAL_TRADE') LIMIT 1",new String[]{target})){if(q.moveToFirst())return false;}
        Authority actual=read(store.readable(),proof.targetOrdinal,System.currentTimeMillis());return actual!=null&&proof.proofHash.equals(actual.proofHash)&&proof.choiceId.equals(actual.choiceId);
    }
}
