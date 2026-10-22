package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import java.lang.reflect.Field;
import java.util.*;
import org.json.*;

/** Read-only diagnostic DTO assembly. Legacy read math is preserved; no DbStore/DbHelper instance is created. */
public final class ReportReadContextCP4 implements java.io.Closeable,ReportTextCP12.Context {
 private final java.util.Set<ReportTextCP12> renderers=new java.util.HashSet<>();
 private final java.util.Map<String,Object> memo=new java.util.HashMap<>();
 private java.io.Writer artifactManifest;private java.io.FileOutputStream artifactFd;
 private final SQLiteDatabase snapshot;private final String snapshotId,snapshotSha;private final long snapshotAt;private final CancellationSignal cancellation;
 public final RuntimeObservationCP4 runtime;public final SourceManifestCP4 source;private boolean closed;
 public static final class RuntimeObservationCP4 {public final String json;public RuntimeObservationCP4(String json){if(json==null||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("REPORT_OBSERVATION_BOUND");this.json=json;}}
 public static final class SourceManifestCP4 {public final String json;public SourceManifestCP4(String json){if(json==null||json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>65536)throw new IllegalArgumentException("REPORT_SOURCE_BOUND");this.json=json;}}
 public static final class ReportReadFailureCP4 extends RuntimeException {public final String code;public ReportReadFailureCP4(String code){super(code);this.code=code;}public ReportReadFailureCP4(String code,Throwable cause){super(code,cause);this.code=code;}}
 public ReportReadContextCP4(SQLiteDatabase snapshot,String id,String hash,long at,RuntimeObservationCP4 observation,SourceManifestCP4 source,CancellationSignal cancellation,long javaBudgetBytes){
  if(snapshot==null||!snapshot.isReadOnly()||!snapshot.isOpen()||id==null||id.isEmpty()||hash==null||!hash.matches("[0-9a-f]{64}")||at<=0||observation==null||source==null||cancellation==null||javaBudgetBytes<3L*1024*1024)throw new IllegalArgumentException("REPORT_CONTEXT_IDENTITY");
  this.snapshot=snapshot;snapshotId=id;snapshotSha=hash;snapshotAt=at;runtime=observation;this.source=source;this.cancellation=cancellation;
 }
 public String observationString(String key){try{Object value=new JSONObject(runtime.json).get(key);if(!(value instanceof String))throw new ReportReadFailureCP4("OBSERVATION_TYPE");return (String)value;}catch(JSONException missing){throw new ReportReadFailureCP4("OBSERVATION_UNAVAILABLE",missing);}}
 public long observationLong(String key){try{Object value=new JSONObject(runtime.json).get(key);if(!(value instanceof Number))throw new ReportReadFailureCP4("OBSERVATION_TYPE");return ((Number)value).longValue();}catch(JSONException missing){throw new ReportReadFailureCP4("OBSERVATION_UNAVAILABLE",missing);}}
 public boolean observationBoolean(String key){try{Object value=new JSONObject(runtime.json).get(key);if(!(value instanceof Boolean))throw new ReportReadFailureCP4("OBSERVATION_TYPE");return (Boolean)value;}catch(JSONException missing){throw new ReportReadFailureCP4("OBSERVATION_UNAVAILABLE",missing);}}
 public static final String COMPLETE_DIAGNOSTIC_PROFILE_R68="COMPLETE_DIAGNOSTIC_R68";
 public static final String LEGACY_DIAGNOSTIC_PROFILE_R64="COMPACT_DIAGNOSTIC_R64";
 public static final String RAW_EVIDENCE_PROFILE="FULL_RAW_EVIDENCE";
 public boolean compactDiagnosticR64(){
  try{
   JSONObject manifest=new JSONObject(source.json);
   // Reports before the profile field always exported their raw SQLite snapshot.
   // Preserve that historical format only when no contradictory flag is present.
   if(!manifest.has("reportProfile")){
    if(manifest.has("rawEvidenceIncluded")){
     Object included=manifest.get("rawEvidenceIncluded");
     if(!(included instanceof Boolean))throw new ReportReadFailureCP4("REPORT_PROFILE_EVIDENCE_CONFLICT");
     if(!((Boolean)included).booleanValue())throw new ReportReadFailureCP4("REPORT_PROFILE_REQUIRED_FOR_COMPACT_EVIDENCE");
    }
    return false;
   }
   Object value=manifest.get("reportProfile");
   if(!(value instanceof String))throw new ReportReadFailureCP4("REPORT_PROFILE_INVALID_TYPE");
   String profile=(String)value;
   boolean compact=COMPLETE_DIAGNOSTIC_PROFILE_R68.equals(profile)||LEGACY_DIAGNOSTIC_PROFILE_R64.equals(profile);
   if(!compact&&!RAW_EVIDENCE_PROFILE.equals(profile))throw new ReportReadFailureCP4("REPORT_PROFILE_UNKNOWN");
   if(manifest.has("rawEvidenceIncluded")){
    Object included=manifest.get("rawEvidenceIncluded");
    if(!(included instanceof Boolean)||((Boolean)included).booleanValue()==compact)
     throw new ReportReadFailureCP4("REPORT_PROFILE_EVIDENCE_CONFLICT");
   }
   return compact;
  }catch(JSONException e){throw new ReportReadFailureCP4("REPORT_PROFILE_INVALID",e);}
 }
 public String evidenceMemberR64(){return compactDiagnosticR64()?"DIAGNOSTIC_SCOPE.json":"snapshot.db";}
 public String assetReference(String asset){try{JSONObject reference=new JSONObject(source.json).getJSONObject("assets").getJSONObject(asset);return "RAW_ASSET_REFERENCE|asset="+asset+"|member="+reference.getString("member")+"|sha256="+reference.getString("sha256")+"|bytes="+reference.getLong("bytes");}catch(JSONException missing){throw new ReportReadFailureCP4("REPORT_ASSET_REFERENCE_MISSING",missing);}}
 public void checkpoint(){if(closed||cancellation.isCanceled()||Thread.currentThread().isInterrupted())throw new ReportReadFailureCP4("REPORT_CONTEXT_CANCELLED");}
 public SQLiteDatabase readable(){checkpoint();return snapshot;}public SQLiteDatabase snapshotReadOnly(){return readable();}
 public CancellationSignal cancellationSignal(){return cancellation;}
 public String snapshotSha256(){return snapshotSha;}
 public long nowMs(){return snapshotAt;}public RuntimeObservationCP4 observation(){return runtime;}public SourceManifestCP4 sourceManifest(){return source;}
 public String getState(String key,String fallback){return state(key,fallback);}
 public String state(String key,String fallback){checkpoint();if(key==null||key.isEmpty())return fallback;
  try(Cursor c=snapshot.rawQuery("SELECT length(CAST(state_value AS BLOB)) FROM app_state WHERE state_key=?",new String[]{key},cancellation)){if(!c.moveToFirst())return fallback;if(c.isNull(0)||c.getLong(0)>16384)throw new ReportReadFailureCP4("REPORT_VALUE_REQUIRES_RAW_REFERENCE");}
  try(Cursor c=snapshot.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{key},cancellation)){if(!c.moveToFirst())throw new ReportReadFailureCP4("REPORT_STATE_DISAPPEARED");return c.getString(0);}
 }
 public String startupSchemaSummaryR363(){try{return new JSONObject(runtime.json).getString("schemaStartupSummary");}catch(Exception error){throw new ReportReadFailureCP4("OBSERVATION_UNAVAILABLE",error);}}
 public Cursor select(String sql,String[] args){checkpoint();if(sql==null||!sql.trim().toUpperCase(java.util.Locale.ROOT).matches("^(SELECT|WITH|PRAGMA)\\b.*")||sql.indexOf(';')>=0)throw new ReportReadFailureCP4("REPORT_SELECT_REQUIRED");return snapshot.rawQuery(sql,args,cancellation);}
 public long scalarLong(String sql,String[] args){try(Cursor c=select(sql,args)){if(!c.moveToFirst()||c.getColumnCount()!=1||c.isNull(0)||c.getType(0)!=Cursor.FIELD_TYPE_INTEGER)throw new ReportReadFailureCP4("REPORT_SCALAR_UNKNOWN");long v=c.getLong(0);if(c.moveToNext())throw new ReportReadFailureCP4("REPORT_SCALAR_MULTIPLE_ROWS");return v;}}
 public Double scalarDouble(String sql,String[] args){try(Cursor c=select(sql,args)){if(!c.moveToFirst())return null;if(c.getColumnCount()!=1)throw new ReportReadFailureCP4("REPORT_SCALAR_SHAPE");Double v=c.isNull(0)?null:c.getDouble(0);if(v!=null&&!Double.isFinite(v))throw new ReportReadFailureCP4("REPORT_NON_FINITE");if(c.moveToNext())throw new ReportReadFailureCP4("REPORT_SCALAR_MULTIPLE_ROWS");return v;}}
 public interface Factory<T>{T create();}
 public <T> T memo(String key,Factory<T> factory){checkpoint();Object value=memo.get(key);if(value==null){if(memo.size()>=32)throw new ReportReadFailureCP4("REPORT_MEMO_LIMIT");value=java.util.Objects.requireNonNull(factory.create());memo.put(key,value);}return (T)value;}
 public java.io.File jobDirectory(){return new java.io.File(snapshot.getPath()).getParentFile();}
 public void recordTextArtifact(java.io.File file,String hash,long bytes)throws java.io.IOException{
  checkpoint();if(!file.getParentFile().equals(jobDirectory())||!file.getName().matches("report-text-[0-9a-f]{64}\\.txt"))throw new java.io.IOException("REPORT_RENDER_PATH");
  if(artifactManifest==null){artifactFd=new java.io.FileOutputStream(new java.io.File(jobDirectory(),"render-artifacts.jsonl"));artifactManifest=new java.io.OutputStreamWriter(artifactFd,java.nio.charset.StandardCharsets.UTF_8);}
  try{ReportMemberPlanCP12.append(artifactManifest,file.getName(),file.getName(),hash,bytes);}catch(Exception error){throw new java.io.IOException("REPORT_RENDER_MANIFEST",error);}
 }
 public void finishArtifacts()throws java.io.IOException{if(artifactManifest!=null){artifactManifest.flush();artifactFd.getFD().sync();artifactManifest.close();artifactManifest=null;artifactFd=null;}}
 public void trackRenderer(ReportTextCP12 renderer){if(renderers.size()>=32)throw new ReportReadFailureCP4("REPORT_OPEN_RENDER_LIMIT");renderers.add(renderer);}
 public void releaseRenderer(ReportTextCP12 renderer){renderers.remove(renderer);}
 public void close(){try{for(ReportTextCP12 renderer:new java.util.ArrayList<ReportTextCP12>(renderers))renderer.abort();renderers.clear();finishArtifacts();}catch(java.io.IOException error){throw new ReportReadFailureCP4("REPORT_RENDER_MANIFEST_CLOSE",error);}finally{closed=true;}} // Service owns and closes the supplied SQLite connection exactly once.
    private void closeCursor(Cursor c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable e) {
            }
        }
    }

    private String getString(Cursor c, String name) {
        if (c == null || name == null) {
            return "";
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return "";
        }

        return c.getString(index);
    }

    private String getString(Object obj, String field, String def) {
        if (obj == null || field == null || field.length() == 0) {
            return def == null ? "" : def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v != null) {
                return String.valueOf(v);
            }
        } catch (Throwable e) {
        }
        return def == null ? "" : def;
    }

    public Models.RuntimeStats getRuntimeStats() {
        Models.RuntimeStats stats = new Models.RuntimeStats();

        Cursor c = null;

        try {
            c =
                    readable()
                            .rawQuery(
                                    "SELECT COUNT(*) AS bet_count, SUM(hit) AS hit_count, SUM(pnl)"
                                        + " AS pnl FROM "
                                            + DbHelper.TABLE_TRADES
                                            + " WHERE settled=1 AND action=? AND stat_scope=? AND"
                                            + " result IN (?,?)",
                                    new String[] {
                                        AppConfig.DECISION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE,
                                        AppConfig.RESULT_WIN,
                                        AppConfig.RESULT_MISS
                                    });

            if (c.moveToFirst()) {
                stats.betCount = getInt(c, "bet_count");
                stats.hitCount = getInt(c, "hit_count");
                stats.pnl = getDouble(c, "pnl");
            }
        } finally {
            closeCursor(c);
        }

        try {
            c =
                    readable()
                            .rawQuery(
                                    "SELECT COUNT(1),"
                                            + "COALESCE(SUM(CASE WHEN settled=1 THEN 1 ELSE 0 END),0),"
                                            + "COALESCE(SUM(CASE WHEN settled=1 AND hit_label=1 THEN 1 ELSE 0 END),0)"
                                            + " FROM "
                                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION,
                                    null);
            if (c.moveToFirst()) {
                stats.shadowObservationCount = c.getInt(0);
                stats.shadowSettledCount = c.getInt(1);
                stats.shadowHitCount = c.getInt(2);
            }
        } finally {
            closeCursor(c);
            c = null;
        }

        stats.holdCount = countDecisionByType(AppConfig.DECISION_HOLD);
        stats.shadowCount = countDecisionByType(AppConfig.DECISION_SHADOW);

        stats.freq100 = calcFrequency(AppConfig.FREQ_WINDOW_SHORT);
        stats.freq500 = calcFrequency(AppConfig.FREQ_WINDOW_MID);
        stats.freq1000 = calcFrequency(AppConfig.FREQ_WINDOW_LONG);
        stats.frequency = stats.freq100;
        stats.maxDrawdown = calcMaxDrawdown();
        stats.loseStreak = calcLoseStreak();

        stats.openTrades =
                countTable(
                        DbHelper.TABLE_TRADES,
                        "settled=0 AND action=? AND stat_scope=?",
                        new String[] {AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE});

        stats.lastBacktestSummary =
                firstNonEmpty(
                        getState("backtest_last_summary", ""), getState("backtest_reason", ""));
        stats.lastBacktestMode = getState("backtest_mode", "");
        stats.learningSummary =
                firstNonEmpty(getState("auto_learn_summary", ""), getState("learning_summary", ""));
        stats.autoLearnSummary = getState("auto_learn_summary", "");
        stats.runtimeProfileSummary = buildProfileCacheSummary();
        stats.biasSummary = getState("bias_summary", "");
        stats.auditSummary = getState("audit_summary", "");
        stats.promotionSummary = getState("promotion_summary", "");
        stats.aPlusSummary = getState("a_plus_summary", "");
        stats.aiJudgeSummary = getState("ai_judge_summary", "");
        stats.selfDiagnosisSummary = getState("self_diagnosis_summary", "");
        stats.lastSnapshotId = getState("last_snapshot_id", "");

        setOptionalStringField(
                stats, "teacherMatrixSummary", getState("teacher_matrix_summary", ""));
        setOptionalIntField(stats, "teacherMatrixSampleCount", countTeacherMatrixSamples());
        setOptionalIntField(stats, "teacherMatrixProfileCount", countTeacherMatrixProfiles());
        setOptionalIntField(stats, "teacherMatrixStrongCount", countTeacherMatrixStrongProfiles());
        setOptionalIntField(stats, "teacherMatrixRiskCount", countTeacherMatrixRiskProfiles());

        Models.BacktestReport latest = getLatestBacktestReport();

        if (latest != null) {
            stats.lastBacktestMode = latest.mode;
            stats.lastBacktestSummary = latest.summary;
            stats.lastBacktestWinRate = latest.winRate;
            stats.lastBacktestFrequency = latest.frequency;
            stats.lastBacktestPnl = latest.pnl;
            stats.lastBacktestMaxDrawdown = latest.maxDrawdown;
        }

        stats.updatedAt = nowMs();
        stats.recalc();

        return stats;
    }

    public Models.BacktestReport getLatestBacktestReport() {
        Cursor c = null;

        try {
            c =
                    query(
                                    DbHelper.TABLE_BACKTEST,
                                    null,
                                    null,
                                    null,
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readBacktestReport(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }

    private Models.BacktestReport readBacktestReport(Cursor c) {
        Models.BacktestReport r = new Models.BacktestReport();

        r.id = getLong(c, "_id");

        r.ok = getInt(c, "ok");
        r.mode = getString(c, "mode");

        if (r.mode == null || r.mode.length() == 0) {
            r.mode = AppConfig.BACKTEST_MODE_FAST;
        }

        r.type = getString(c, "type");
        r.window = getIntAny(c, "window_no", "window");

        r.rangeStart = getString(c, "range_start");
        r.rangeEnd = getString(c, "range_end");
        r.startIssue = getString(c, "start_issue");
        r.endIssue = getString(c, "end_issue");

        if (r.startIssue == null || r.startIssue.length() == 0) {
            r.startIssue = r.rangeStart;
        }

        if (r.endIssue == null || r.endIssue.length() == 0) {
            r.endIssue = r.rangeEnd;
        }

        r.trainCount = getInt(c, "train_count");
        r.testCount = getInt(c, "test_count");
        r.totalCount = getInt(c, "total_count");

        if (r.totalCount <= 0) {
            r.totalCount = r.testCount;
        }

        r.betCount = getInt(c, "bet_count");
        r.hitCount = getInt(c, "hit_count");
        r.missCount = getInt(c, "miss_count");

        if (r.missCount <= 0) {
            r.missCount = r.betCount - r.hitCount;

            if (r.missCount < 0) {
                r.missCount = 0;
            }
        }

        r.holdCount = getInt(c, "hold_count");
        r.shadowCount = getInt(c, "shadow_count");

        r.candidateCount = getInt(c, "candidate_count");
        r.enrichCount = getInt(c, "enrich_count");
        r.deepCount = getInt(c, "deep_count");

        r.hitRate = getDouble(c, "hit_rate");
        r.winRate = getDouble(c, "win_rate");

        if (r.winRate <= 0.0d) {
            r.winRate = r.hitRate;
        }

        r.frequency = getDouble(c, "frequency");
        r.pnl = getDouble(c, "pnl");
        r.maxDrawdown = getDouble(c, "max_drawdown");
        r.longestLose = getInt(c, "longest_lose");

        r.avgRawP = getDouble(c, "avg_raw_p");
        r.avgFinalP = getDouble(c, "avg_final_p");
        r.avgGate = getDouble(c, "avg_gate");
        r.avgEdge = getDouble(c, "avg_edge");
        r.calibrationError = getDouble(c, "calibration_error");

        r.status = getString(c, "status");
        r.summary = getString(c, "summary");

        if (r.summary == null || r.summary.length() == 0) {
            r.summary = r.status;
        }

        r.startedAt = getLong(c, "started_at");
        r.finishedAt = getLong(c, "finished_at");
        r.costMs = getLong(c, "cost_ms");
        r.createdAt = getLong(c, "created_at");

        r.manualTriggered = getInt(c, "manual_triggered");
        r.profileCount = getInt(c, "profile_count");
        r.attributionCount = getInt(c, "attribution_count");
        r.promotedCount = getInt(c, "promoted_count");
        r.rejectedCount = getInt(c, "rejected_count");
        r.aPlusCount = getInt(c, "a_plus_count");
        r.progress = getInt(c, "progress");
        r.aPlusHitRate = getDouble(c, "a_plus_hit_rate");
        r.biasScoreAvg = getDouble(c, "bias_score_avg");
        r.brierScore = getDouble(c, "brier_score");
        r.aiJudgeAvgScore = getDouble(c, "ai_judge_avg_score");
        r.deepScoreAvg = getDouble(c, "deep_score_avg");
        r.biasSummary = getString(c, "bias_summary");
        r.auditSummary = getString(c, "audit_summary");
        r.promotionSummary = getString(c, "promotion_summary");
        r.aPlusSummary = getString(c, "a_plus_summary");
        r.learningImpactSummary = getString(c, "learning_impact_summary");
        r.topProfiles = getString(c, "top_profiles");
        r.topMissReasons = getString(c, "top_miss_reasons");
        r.topHitReasons = getString(c, "top_hit_reasons");

        setOptionalIntField(
                r, "teacherMatrixSampleCount", getInt(c, "teacher_matrix_sample_count"));
        setOptionalIntField(
                r, "teacherMatrixProfileCount", getInt(c, "teacher_matrix_profile_count"));
        setOptionalDoubleField(
                r, "teacherMatrixAvgHitRate", getDouble(c, "teacher_matrix_avg_hit_rate"));
        setOptionalDoubleField(
                r, "teacherMatrixAvgConfidence", getDouble(c, "teacher_matrix_avg_confidence"));
        setOptionalStringField(r, "teacherMatrixSummary", getString(c, "teacher_matrix_summary"));

        if (r.window <= 0) {
            r.window = r.testCount;
        }

        return r;
    }

    private void setOptionalStringField(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private double getDouble(Cursor c, String name) {
        if (c == null || name == null) {
            return 0.0d;
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return 0.0d;
        }

        return c.getDouble(index);
    }

    private double getDouble(Object obj, String field, double def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).doubleValue();
            }
            if (v != null) {
                return Double.parseDouble(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private void setOptionalDoubleField(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.setDouble(obj, value);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(fieldName);
                f.set(obj, Double.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private int getInt(Cursor c, String name) {
        if (c == null || name == null) {
            return 0;
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return 0;
        }

        return c.getInt(index);
    }

    private int getInt(Object obj, String field, int def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).intValue();
            }
            if (v != null) {
                return Integer.parseInt(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private void setOptionalIntField(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.setInt(obj, value);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(fieldName);
                f.set(obj, Integer.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private long getLong(Cursor c, String name) {
        if (c == null || name == null) {
            return 0L;
        }

        int index = c.getColumnIndex(name);

        if (index < 0 || c.isNull(index)) {
            return 0L;
        }

        return c.getLong(index);
    }

    private long getLong(Object obj, String field, long def) {
        if (obj == null || field == null || field.length() == 0) {
            return def;
        }
        try {
            Field f = obj.getClass().getField(field);
            Object v = f.get(obj);
            if (v instanceof Number) {
                return ((Number) v).longValue();
            }
            if (v != null) {
                return Long.parseLong(String.valueOf(v));
            }
        } catch (Throwable e) {
        }
        return def;
    }

    private int getIntAny(Cursor c, String a, String b) {
        int v = getInt(c, a);

        if (v != 0) {
            return v;
        }

        return getInt(c, b);
    }

    public int countTeacherMatrixRiskProfiles() {
        return countTable(DbHelper.TABLE_TEACHER_MATRIX_PROFILE, "status=?", new String[] {"RISK"});
    }

    private int countTable(String table, String where, String[] args) {
        return countTable(readable(), table, where, args);
    }

    private int countTable(SQLiteDatabase db, String table, String where, String[] args) {
        if (db == null || table == null || table.length() == 0) {
            return 0;
        }

        Cursor c = null;

        try {
            c = query(table, new String[] {"COUNT(*) AS c"}, where, args, null, null, null);

            if (c.moveToFirst()) {
                return getInt(c, "c");
            }
        } catch(Throwable e){throw new ReportReadFailureCP4("REPORT_QUERY_FAILED",e);} finally {
            closeCursor(c);
        }

        return 0;
    }

    public int countTeacherMatrixStrongProfiles() {
        return countTable(DbHelper.TABLE_TEACHER_MATRIX_PROFILE, "status=?", new String[] {"STRONG"});
    }

    public int countTeacherMatrixProfiles() {
        return countTable(DbHelper.TABLE_TEACHER_MATRIX_PROFILE, null, null);
    }

    public int countTeacherMatrixSamples() {
        return countTable(DbHelper.TABLE_TEACHER_MATRIX_SAMPLE, null, null);
    }

    private String buildProfileCacheSummary() {
        int a = countTable(DbHelper.TABLE_RUNTIME_PROFILE_CACHE, null, null);
        int b = countTable(DbHelper.TABLE_STATE_NUMBER_PROFILE, null, null);
        int c = countTable(DbHelper.TABLE_LEARNING_CASE_LOG, null, null);
        int d = countTable(DbHelper.TABLE_TEACHER_MATRIX_SAMPLE, null, null);
        int e = countTable(DbHelper.TABLE_TEACHER_MATRIX_PROFILE, null, null);

        return "画像缓存：组合="
                + a
                + "｜号码="
                + b
                + "｜学习样本="
                + c
                + "｜教师样本="
                + d
                + "｜教师画像="
                + e
                + "｜说明=缓存只辅助解释、反推和主链读取字段，不在DbStore直接救援BET";
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && a.length() > 0) {
            return a;
        }

        if (b != null) {
            return b;
        }

        return "";
    }

    private String firstNonEmpty(String a, String b, String c) {
        if (a != null && a.length() > 0) {
            return a;
        }
        if (b != null && b.length() > 0) {
            return b;
        }
        return c == null ? "" : c;
    }

    private String firstNonEmpty(String a, String b, String c, String d) {
        if (a != null && a.length() > 0) {
            return a;
        }
        if (b != null && b.length() > 0) {
            return b;
        }
        if (c != null && c.length() > 0) {
            return c;
        }
        return d == null ? "" : d;
    }

    private String firstNonEmpty(String a, String b, String c, String d, String e) {
        if (a != null && a.length() > 0) {
            return a;
        }
        if (b != null && b.length() > 0) {
            return b;
        }
        if (c != null && c.length() > 0) {
            return c;
        }
        if (d != null && d.length() > 0) {
            return d;
        }
        return e == null ? "" : e;
    }

    private int calcLoseStreak() {
        Cursor c = null;
        int streak = 0;

        try {
            c =
                    query(
                                    DbHelper.TABLE_TRADES,
                                    new String[] {"result"},
                                    "settled=1 AND action=? AND stat_scope=? AND result IN (?,?)",
                                    new String[] {
                                        AppConfig.DECISION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE,
                                        AppConfig.RESULT_WIN,
                                        AppConfig.RESULT_MISS
                                    },
                                    null,
                                    null,
                                    "settle_time DESC, settled_at DESC, _id DESC");

            while (c.moveToNext()) {
                String r = AppConfig.normalizeResult(getString(c, "result"));

                if (AppConfig.RESULT_MISS.equals(r)) {
                    streak++;
                } else {
                    break;
                }
            }
        } finally {
            closeCursor(c);
        }

        return streak;
    }

    private double calcMaxDrawdown() {
        Cursor c = null;
        double equity = 0.0d;
        double peak = 0.0d;
        double maxDd = 0.0d;

        try {
            c =
                    query(
                                    DbHelper.TABLE_TRADES,
                                    new String[] {"pnl"},
                                    "settled=1 AND action=? AND stat_scope=? AND result IN (?,?)",
                                    new String[] {
                                        AppConfig.DECISION_BET,
                                        AppConfig.STAT_SCOPE_REAL_TRADE,
                                        AppConfig.RESULT_WIN,
                                        AppConfig.RESULT_MISS
                                    },
                                    null,
                                    null,
                                    "settle_time ASC, settled_at ASC, _id ASC");

            while (c.moveToNext()) {
                equity += getDouble(c, "pnl");

                if (equity > peak) {
                    peak = equity;
                }

                double dd = peak - equity;

                if (dd > maxDd) {
                    maxDd = dd;
                }
            }
        } finally {
            closeCursor(c);
        }

        return maxDd;
    }

    private double calcFrequency(int window) {
        if (window <= 0) {
            window = AppConfig.FREQ_WINDOW_SHORT;
        }

        List<Models.Draw> draws = null;

        try {
            draws = getRecentDraws(window);
        } catch(Throwable e){throw new ReportReadFailureCP4("REPORT_QUERY_FAILED",e);}

        if (draws == null || draws.size() == 0) {
            return 0.0d;
        }

        HashSet<String> issueSet = new HashSet<String>();

        for (int i = 0; i < draws.size(); i++) {
            Models.Draw d = draws.get(i);

            if (d == null || d.issue == null || d.issue.length() == 0) {
                continue;
            }

            issueSet.add(d.issue);
        }

        if (issueSet.size() == 0) {
            return 0.0d;
        }

        int betCount = countRealBetIssueInRecentDrawIssues(issueSet, window);
        int total = issueSet.size();

        if (betCount < 0) {
            betCount = 0;
        }

        if (betCount > total) {
            betCount = total;
        }

        return total > 0
                ? AppConfig.clampProbability(((double) betCount) / ((double) total))
                : 0.0d;
    }

    private int countRealBetIssueInRecentDrawIssues(HashSet<String> issueSet, int window) {
        if (issueSet == null || issueSet.size() == 0) {
            return 0;
        }

        HashSet<String> betIssues = new HashSet<String>();

        countRealBetIssuesFromTrades(issueSet, betIssues, window);
        countRealBetIssuesFromDecisions(issueSet, betIssues, window);

        return betIssues.size();
    }

    private void countRealBetIssuesFromDecisions(
            HashSet<String> issueSet, HashSet<String> betIssues, int window) {
        if (issueSet == null || betIssues == null || issueSet.size() == 0) {
            return;
        }

        Cursor c = null;

        try {
            int queryLimit = window * 8;

            if (queryLimit < 120) {
                queryLimit = 120;
            }

            if (queryLimit > 6000) {
                queryLimit = 6000;
            }

            c =
                    query(
                                    DbHelper.TABLE_DECISIONS,
                                    null,
                                    "decision=? AND stat_scope=?",
                                    new String[] {
                                        AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "created_at DESC, _id DESC",
                                    String.valueOf(queryLimit));

            while (c.moveToNext()) {
                Models.Decision d = readDecision(c);

                if (d == null) {
                    continue;
                }

                if (d.targetIssue == null || d.targetIssue.length() == 0) {
                    continue;
                }

                if (!issueSet.contains(d.targetIssue)) {
                    continue;
                }

                if (!AppConfig.DECISION_BET.equals(d.decision)) {
                    continue;
                }

                if (!isValidBetDecision(d)) {
                    continue;
                }

                if (isWeakPlaceholderDecision(d) || isQuickObservationDecision(d)) {
                    continue;
                }

                if (!AppConfig.STAT_SCOPE_REAL_TRADE.equals(d.statScope)) {
                    continue;
                }

                betIssues.add(d.targetIssue);
            }
        } catch(Throwable e){throw new ReportReadFailureCP4("REPORT_QUERY_FAILED",e);} finally {
            closeCursor(c);
        }
    }

    private boolean isQuickObservationDecision(Models.Decision d) {
        if (d == null) {
            return false;
        }

        String text = "";

        if (d.shapeKey != null) text += d.shapeKey;
        if (d.probabilityMode != null) text += " " + d.probabilityMode;
        if (d.backtestMode != null) text += " " + d.backtestMode;
        if (d.reasonSummary != null) text += " " + d.reasonSummary;
        if (d.reasonText != null) text += " " + d.reasonText;
        if (d.deepReason != null) text += " " + d.deepReason;
        if (d.learningSummary != null) text += " " + d.learningSummary;
        if (d.selfDiagnosisText != null) text += " " + d.selfDiagnosisText;

        if (text.indexOf("QUICK_OBSERVE") >= 0
                || text.indexOf("QUICK_") >= 0
                || text.indexOf("快速观察") >= 0
                || text.indexOf("只做观察") >= 0
                || text.indexOf("不计真实BET") >= 0
                || text.indexOf("不作为真实BET") >= 0
                || text.indexOf("兜底") >= 0) {
            return true;
        }

        return false;
    }

    private boolean isWeakPlaceholderDecision(Models.Decision d) {
        if (d == null) {
            return true;
        }

        if (isQuickObservationDecision(d)) {
            return true;
        }

        String decision = AppConfig.normalizeDecision(d.decision);
        if (AppConfig.DECISION_BET.equals(decision)
                && AppConfig.isValidPosition(d.position)
                && Models.isValidK3Mask(d.k3Mask)) {
            return false;
        }
        String text = "";

        if (d.reasonSummary != null) text += d.reasonSummary;
        if (d.reasonText != null) text += " " + d.reasonText;
        if (d.deepReason != null) text += " " + d.deepReason;
        if (d.learningSummary != null) text += " " + d.learningSummary;
        if (d.selfDiagnosisText != null) text += " " + d.selfDiagnosisText;
        if (d.blockCode != null) text += " " + d.blockCode;

        if (text.indexOf("临时") >= 0
                || text.indexOf("占位") >= 0
                || text.indexOf("正在生成") >= 0
                || text.indexOf("保护性HOLD") >= 0
                || text.indexOf("未生成有效决策") >= 0
                || text.indexOf("分析线程未生成") >= 0
                || text.indexOf("等待实时分析") >= 0
                || text.indexOf("看门狗") >= 0
                || text.indexOf("快速观察") >= 0
                || text.indexOf("只做观察") >= 0
                || text.indexOf("兜底") >= 0) {
            return true;
        }

        if (AppConfig.DECISION_HOLD.equals(decision)
                && d.finalP <= 0.0001d
                && d.position <= 0
                && !Models.isValidK3Mask(d.k3Mask)) {
            return true;
        }

        return false;
    }

    private boolean isValidBetDecision(Models.Decision d) {
        if (d == null) {
            return false;
        }

        d.decision = AppConfig.normalizeDecision(d.decision);

        if (!AppConfig.DECISION_BET.equals(d.decision)) {
            return false;
        }

        if (!AppConfig.isValidPosition(d.position)) {
            return false;
        }

        if (d.k3Mask <= 0 && AppConfig.isValidK3Numbers(d.n1, d.n2, d.n3)) {
            d.k3Mask = Models.buildK3Mask(d.n1, d.n2, d.n3);
        }

        if (!AppConfig.isValidNumber(d.n1)
                || !AppConfig.isValidNumber(d.n2)
                || !AppConfig.isValidNumber(d.n3)) {
            return false;
        }

        if (!Models.isValidK3Mask(d.k3Mask)) {
            return false;
        }

        try {
            if (!d.hasLegalPlayRule()) {
                return false;
            }
        } catch (Throwable e) {
        }

        /* DbStore不创造BET，只验证DecisionGate签发的不可变授权。 */
        return DecisionAuthorizationEngine.isDecisionAuthorized(d);
    }

    private Models.Decision readDecision(Cursor c) {
        Models.Decision d = new Models.Decision();

        d.id = getLong(c, "_id");
        d.tradeId = getLong(c, "trade_id");
        d.baseIssue = getString(c, "base_issue");
        d.targetIssue = getString(c, "target_issue");
        d.createdAt = getLong(c, "created_at");
        d.decision = AppConfig.normalizeDecision(getString(c, "decision"));
        d.action = firstNonEmpty(AppConfig.normalizeAction(getString(c, "action")), d.decision);

        d.position = getInt(c, "position");
        d.n1 = getInt(c, "n1");
        d.n2 = getInt(c, "n2");
        d.n3 = getInt(c, "n3");
        d.k3Mask = getInt(c, "k3_mask");

        d.rawP = getDouble(c, "raw_p");
        d.calibratedP = getDouble(c, "calibrated_p");
        d.finalP = getDouble(c, "final_p");
        d.dynamicGate = getDouble(c, "dynamic_gate");
        d.edge = getDouble(c, "edge");

        d.modelP = getDouble(c, "model_p");
        d.shapeP = getDouble(c, "shape_p");
        d.comboP = getDouble(c, "combo_p");
        d.recoveryP = getDouble(c, "recovery_p");
        d.similarP = getDouble(c, "similar_p");
        d.memoryP = getDouble(c, "memory_p");

        d.similarHitRate = getDouble(c, "similar_hit_rate");
        d.comboHitRate = getDouble(c, "combo_hit_rate");
        d.effectiveSample = getInt(c, "effective_sample");
        d.comboSample = getInt(c, "combo_sample");

        d.trendLife = getDouble(c, "trend_life");
        d.driftRisk = getDouble(c, "drift_risk");
        d.breakDepth = getDouble(c, "break_depth");
        d.recoveryScore = getDouble(c, "recovery_score");
        d.championGap = getDouble(c, "champion_gap");
        d.comboGap = getDouble(c, "combo_gap");
        d.hotReverseRisk = getDouble(c, "hot_reverse_risk");
        d.falseStrongRisk = getDouble(c, "false_strong_risk");

        d.freq100 = getDouble(c, "freq100");
        d.freq500 = getDouble(c, "freq500");
        d.freq1000 = getDouble(c, "freq1000");

        d.calibrationError = getDouble(c, "calibration_error");
        d.probabilityMode = getString(c, "probability_mode");
        d.backtestMode = getString(c, "backtest_mode");

        d.stateMode = getString(c, "state_mode");
        d.shapeKey = getString(c, "shape_key");
        d.blockCode = getString(c, "block_code");
        d.reasonSummary = getString(c, "reason_summary");
        d.reasonText = firstNonEmpty(getString(c, "reason_text"), d.reasonSummary);

        d.dataOk = getInt(c, "data_ok");
        d.settled = getInt(c, "settled");
        d.result = AppConfig.normalizeResult(getString(c, "result"));
        d.settleTime = getLong(c, "settle_time");
        d.settledAt = getLong(c, "settled_at");
        d.actualNumber = getInt(c, "actual_number");
        d.hit = getInt(c, "hit");
        d.pnl = getDouble(c, "pnl");
        d.missReason = getString(c, "miss_reason");
        d.settlementReason = getString(c, "settlement_reason");

        d.deepScore = getDouble(c, "deep_score");
        d.deepConfidence = getDouble(c, "deep_confidence");
        d.deepFeatureP = getDouble(c, "deep_feature_p");
        d.deepPositionScore = getDouble(c, "deep_position_score");
        d.deepNumberScore = getDouble(c, "deep_number_score");
        d.deepCycleScore = getDouble(c, "deep_cycle_score");
        d.deepTransitionScore = getDouble(c, "deep_transition_score");
        d.deepStateScore = getDouble(c, "deep_state_score");

        d.biasScore = getDouble(c, "bias_score");
        d.biasConfidence = getDouble(c, "bias_confidence");
        d.profileScore = getDouble(c, "profile_score");
        d.fallbackProfileScore = getDouble(c, "fallback_profile_score");
        d.auditPenalty = getDouble(c, "audit_penalty");
        d.auditBoost = getDouble(c, "audit_boost");

        d.aiJudgeScore = getDouble(c, "ai_judge_score");
        d.aiJudgeConfidence = getDouble(c, "ai_judge_confidence");
        d.evidenceScore = getDouble(c, "evidence_score");
        d.evidenceRisk = getDouble(c, "evidence_risk");
        d.conflictLevel = getDouble(c, "conflict_level");

        d.aPlusScore = getDouble(c, "a_plus_score");
        d.promotionScore = getDouble(c, "promotion_score");
        d.weakestNumberScore = getDouble(c, "weakest_number_score");
        d.balanceScore = getDouble(c, "balance_score");

        d.deepProfileKey = getString(c, "deep_profile_key");
        d.deepProfileHitRate = getDouble(c, "deep_profile_hit_rate");
        d.deepProfileSample = getInt(c, "deep_profile_sample");
        d.deepFallbackLevel = getInt(c, "deep_fallback_level");
        d.fallbackProfileKey = getString(c, "fallback_profile_key");
        d.promotionStatus = getString(c, "promotion_status");
        d.aPlusStatus = getString(c, "a_plus_status");
        d.candidateEvidenceKey = getString(c, "candidate_evidence_key");
        d.snapshotId = getString(c, "snapshot_id");

        d.deepAttribution = getString(c, "deep_attribution");
        d.deepReason = getString(c, "deep_reason");
        d.aiJudgeSummary = getString(c, "ai_judge_summary");
        d.learningSummary = getString(c, "learning_summary");
        d.selfDiagnosisText = getString(c, "self_diagnosis_text");

        d.manualBacktestSupport = getInt(c, "manual_backtest_support");
        d.statScope = getString(c, "stat_scope");

        d.stateKey = getString(c, "state_key");
        d.trendKey = getString(c, "trend_key");
        d.driftKey = getString(c, "drift_key");
        d.breakKey = getString(c, "break_key");
        d.recoveryKey = getString(c, "recovery_key");
        d.runtimeProfileScore = getDouble(c, "runtime_profile_score");
        d.stateNumberProfileScore = getDouble(c, "state_number_profile_score");
        d.teacherWeight = getDouble(c, "teacher_weight");
        d.stateConfidence = getDouble(c, "state_confidence");
        d.stateSample = getInt(c, "state_sample");
        d.cacheSample = getInt(c, "cache_sample");
        d.recentSample = getInt(c, "recent_sample");
        d.recentHitRate = getDouble(c, "recent_hit_rate");
        d.runtimeHitRate = getDouble(c, "runtime_hit_rate");
        d.numberProfileHitRate = getDouble(c, "number_profile_hit_rate");
        d.realtimeCostMs = getLong(c, "realtime_cost_ms");
        d.realtimeTimeout = getInt(c, "realtime_timeout");

        readTeacherMatrixFieldsToObject(c, d);

        if (d.edge == 0.0d && d.finalP > 0.0d && d.dynamicGate > 0.0d) {
            d.edge = d.finalP - d.dynamicGate;
        }

        d.statScope = AppConfig.normalizeStatScopeForDecision(d.decision, d.statScope);

        readFieldChainValues(c, d);
        readDynamicCausalFields(c, d);
        readMainChainClosedLoopFields(c, d);
        readV24ContractValues(c, d);
        d.v46EventProbability = getDouble(c, "v46_event_probability");
        d.v46ActionLcb = getDouble(c, "v46_action_lcb");
        d.v46ProbabilityUncertainty = getDouble(c, "v46_probability_uncertainty");
        d.v46ExpectedValue = getDouble(c, "v46_expected_value");
        d.v46FinalRank = getInt(c, "v46_final_rank");
        d.v46FormalCandidateCount = getInt(c, "v46_formal_candidate_count");
        d.v46FinalContractHash = getString(c, "v46_final_contract_hash");
        d.v46ContractHash = d.v46FinalContractHash;
        d.v46Lane = getString(c, "v46_lane");

        return d;
    }

    private void readV24ContractValues(Cursor c, Object obj) {
        if (c == null || obj == null) return;
        String[][] m = v24StringMap();
        for (int i = 0; i < m.length; i++) readFieldChainString(c, obj, m[i][0], m[i][1]);
        m = v24IntMap();
        for (int i = 0; i < m.length; i++) readFieldChainInt(c, obj, m[i][0], m[i][1]);
        m = v24LongMap();
        for (int i = 0; i < m.length; i++) setOptionalLongField(obj, m[i][1], getLong(c, m[i][0]));
        m = v24DoubleMap();
        for (int i = 0; i < m.length; i++)
            setOptionalDoubleField(obj, m[i][1], getDouble(c, m[i][0]));
    }

    private String[][] v24DoubleMap() {
        return new String[][] {
            {"raw_rank_score", "rawRankScore"},
            {"calibrated_hit_probability", "calibratedHitProbability"},
            {"posterior_mean", "posteriorMean"},
            {"posterior_wilson_lower", "posteriorWilsonLower"},
            {"posterior_recent_rate", "posteriorRecentRate"},
            {"posterior_drift", "posteriorDrift"},
            {"family_wilson_lower", "familyWilsonLower"},
            {"calibration_brier", "calibrationBrier"},
            {"authorization_probability", "authorizationProbability"},
            {"authorization_gate", "authorizationGate"},
            {"authorization_edge", "authorizationEdge"},
            {"honest_probability", "honestProbability"},
            {"action_utility", "actionUtility"},
            {"action_structure_score", "actionStructureScore"},
            {"action_discrimination_score", "actionDiscriminationScore"},
            {"action_maturity_score", "actionMaturityScore"},
            {"action_uncertainty_penalty", "actionUncertaintyPenalty"},
            {"action_frequency_support", "actionFrequencySupport"},
            {"v42_accept_score", "v42AcceptScore"},
            {"v42_family_lcb", "v42FamilyLcb"},
            {"v42_regime_quality", "v42RegimeQuality"},
            {"v42_association_score", "v42AssociationScore"},
            {"v42_distillation_risk", "v42DistillationRisk"},
            {"v43_strategy_support", "v43StrategySupport"},
            {"v43_strategy_risk", "v43StrategyRisk"},
            {"v43_strategy_confidence", "v43StrategyConfidence"},
            {"v43_strategy_diversity", "v43StrategyDiversity"},
            {"v43_strategy_maturity", "v43StrategyMaturity"},
            {"v43_safe_probability", "v43SafeProbability"},
            {"v43_probability_residual", "v43ProbabilityResidual"},
            {"v43_ablation_fragility", "v43AblationFragility"},
            {"v43_probability_lock", "v43ProbabilityLock"}
        };
    }

    private void setOptionalLongField(Object obj, String fieldName, long value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            f.setLong(obj, value);
        } catch (Throwable e) {
            try {
                java.lang.reflect.Field f = obj.getClass().getField(fieldName);
                f.set(obj, Long.valueOf(value));
            } catch (Throwable ignored) {
            }
        }
    }

    private String[][] v24LongMap() {
        return new String[][] {{"authorized_at", "authorizedAt"}};
    }

    private void readFieldChainInt(Cursor c, Object obj, String column, String field) {
        setOptionalIntField(obj, field, getInt(c, column));
    }

    private String[][] v24IntMap() {
        return new String[][] {
            {"is_realtime_real", "isRealtimeReal"},
            {"contract_valid", "contractValid"},
            {"posterior_sample", "posteriorSample"},
            {"posterior_hit", "posteriorHit"},
            {"calibration_bucket", "calibrationBucket"},
            {"calibration_sample", "calibrationSample"},
            {"calibration_total_sample", "calibrationTotalSample"},
            {"calibration_ready", "calibrationReady"},
            {"authorization_valid", "authorizationValid"},
            {"micro_walk_forward_sample", "microWalkForwardSample"},
            {"oos_rank_slot", "oosRankSlot"},
            {"oos_candidate_count", "oosCandidateCount"},
            {"v42_support_votes", "v42SupportVotes"},
            {"v43_evidence_group_count", "v43EvidenceGroupCount"},
            {"v43_hard_risk", "v43HardRisk"}
        };
    }

    private void readFieldChainString(Cursor c, Object obj, String column, String field) {
        setOptionalStringField(obj, field, getString(c, column));
    }

    private String[][] v24StringMap() {
        return new String[][] {
            // These two provenance fields are part of the same persisted decision/trade
            // contract.  Omitting them here meant that every settlement read/write round-trip
            // silently reset a legitimate REALTIME trade to UNKNOWN/0.
            {"source_type", "sourceType"},
            {"contract_decision_id", "contractDecisionId"},
            {"candidate_hash", "candidateHash"},
            {"snapshot_max_issue", "snapshotMaxIssue"},
            {"trained_until_issue", "trainedUntilIssue"},
            {"build_id", "buildId"},
            {"cohort_id", "cohortId"},
            {"schema_version", "schemaVersion"},
            {"feature_schema_version", "featureSchemaVersion"},
            {"ranking_version", "rankingVersion"},
            {"calibration_version", "calibrationVersion"},
            {"gate_policy_version", "gatePolicyVersion"},
            {"pipeline_version", "pipelineVersion"},
            {"contract_status", "contractStatus"},
            {"coarse_family_key", "coarseFamilyKey"},
            {"mid_family_key", "midFamilyKey"},
            {"fine_family_key", "fineFamilyKey"},
            {"position_family_key", "positionFamilyKey"},
            {"posterior_matched_level", "posteriorMatchedLevel"},
            {"posterior_family_key", "posteriorFamilyKey"},
            {"posterior_policy", "posteriorPolicy"},
            {"posterior_version", "posteriorVersion"},
            {"calibration_status", "calibrationStatus"},
            {"authorization_id", "authorizationId"},
            {"authorized_mode", "authorizedMode"},
            {"authorization_reject_code", "authorizationRejectCode"},
            {"teacher_policy", "teacherPolicy"},
            {"dynamic_policy", "dynamicPolicy"},
            {"adaptive_policy", "adaptivePolicy"},
            {"micro_walk_forward_status", "microWalkForwardStatus"},
            {"micro_walk_forward_version", "microWalkForwardVersion"},
            {"micro_walk_forward_as_of_issue", "microWalkForwardAsOfIssue"},
            {"action_utility_version", "actionUtilityVersion"},
            {"lane_candidate", "laneCandidate"},
            {"v42_regime_family", "v42RegimeFamily"},
            {"v42_risk_coverage_mode", "v42RiskCoverageMode"},
            {"v42_primary_reject", "v42PrimaryReject"},
            {"v42_secondary_reject", "v42SecondaryReject"},
            {"v42_tertiary_reject", "v42TertiaryReject"},
            {"v42_lane_policy_version", "v42LanePolicyVersion"},
            {"v43_dominant_evidence", "v43DominantEvidence"},
            {"v43_missing_evidence", "v43MissingEvidence"},
            {"v43_evidence_fingerprint", "v43EvidenceFingerprint"},
            {"v43_probability_lock_hash", "v43ProbabilityLockHash"},
            {"v43_pipeline_stage", "v43PipelineStage"},
            {"v43_invariant_status", "v43InvariantStatus"},
            {"v43_strategy_version", "v43StrategyVersion"}
        };
    }

    private void readMainChainClosedLoopFields(Cursor c, Object obj) {
        if (c == null || obj == null) return;
        String[][] textMap = mainChainTextMap();
        for (int i = 0; i < textMap.length; i++)
            setStringField(obj, textMap[i][1], getString(c, textMap[i][0]));
        String[][] intMap = mainChainIntMap();
        for (int i = 0; i < intMap.length; i++)
            setIntField(obj, intMap[i][1], getInt(c, intMap[i][0]));
        String[][] doubleMap = mainChainDoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setDoubleRawField(obj, doubleMap[i][1], getDouble(c, doubleMap[i][0]));
        setOptionalLongField(
                obj, "teacherSnapshotCreatedAt", getLong(c, "teacher_snapshot_created_at"));
        readLinkModificationV2Values(c, obj);
        try {
            Models.normalizeMainChainBridgeFields(obj);
        } catch (Throwable e) {
        }
    }

    private void readLinkModificationV2Values(Cursor c, Object obj) {
        if (c == null || obj == null) return;
        String[][] textMap = linkModificationV2TextMap();
        for (int i = 0; i < textMap.length; i++)
            setOptionalStringField(obj, textMap[i][1], getString(c, textMap[i][0]));
        String[][] intMap = linkModificationV2IntMap();
        for (int i = 0; i < intMap.length; i++)
            setOptionalIntField(obj, intMap[i][1], getInt(c, intMap[i][0]));
        String[][] doubleMap = linkModificationV2DoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setOptionalDoubleField(obj, doubleMap[i][1], getDouble(c, doubleMap[i][0]));
        setOptionalLongField(obj, "snapshotCreatedAt", getLong(c, "snapshot_created_at"));
        setOptionalLongField(
                obj, "frequencyStateUpdatedAt", getLong(c, "frequency_state_updated_at"));
        setOptionalIntField(
                obj, "frequencyFloorRequiredCount", getInt(c, "frequency_floor_required_count"));
        setOptionalIntField(
                obj, "frequencyFloorTargetCount", getInt(c, "frequency_floor_target_count"));
        setOptionalIntField(
                obj, "frequencyFloorActualCount", getInt(c, "frequency_floor_actual_count"));
        setOptionalIntField(
                obj, "frequencyFloorDebtCount", getInt(c, "frequency_floor_debt_count"));
        setOptionalIntField(
                obj, "frequencyTargetDebtCount", getInt(c, "frequency_target_debt_count"));
        setOptionalIntField(obj, "frequencyFloorMandatory", getInt(c, "frequency_floor_mandatory"));
        setOptionalIntField(
                obj, "frequencyFloorNoUpperCap", getInt(c, "frequency_floor_no_upper_cap"));
        setOptionalDoubleField(
                obj, "frequencyFloorRankScore", getDouble(c, "frequency_floor_rank_score"));
        setOptionalStringField(
                obj, "frequencyFloorVersion", getString(c, "frequency_floor_version"));
        setOptionalDoubleField(obj, "realBetFrequency20", getDouble(c, "real_bet_frequency_20"));
        setOptionalIntField(obj, "frequencyObserved20", getInt(c, "frequency_observed_20"));
        setOptionalIntField(obj, "frequencyObserved50", getInt(c, "frequency_observed_50"));
        setOptionalIntField(obj, "frequencyObserved100", getInt(c, "frequency_observed_100"));
        setOptionalIntField(obj, "frequencyObserved300", getInt(c, "frequency_observed_300"));
        setOptionalIntField(obj, "frequencyBetCount20", getInt(c, "frequency_bet_count_20"));
        setOptionalIntField(obj, "frequencyBetCount50", getInt(c, "frequency_bet_count_50"));
        setOptionalIntField(obj, "frequencyBetCount100", getInt(c, "frequency_bet_count_100"));
        setOptionalIntField(obj, "frequencyBetCount300", getInt(c, "frequency_bet_count_300"));
        setOptionalIntField(obj, "frequencyDebt20", getInt(c, "frequency_debt_20"));
        setOptionalIntField(obj, "frequencyDebt50", getInt(c, "frequency_debt_50"));
        setOptionalIntField(obj, "frequencyDebt100", getInt(c, "frequency_debt_100"));
        setOptionalIntField(obj, "frequencyDebt300", getInt(c, "frequency_debt_300"));
        setOptionalIntField(obj, "frequencyTargetDebt20", getInt(c, "frequency_target_debt_20"));
        setOptionalIntField(obj, "frequencyTargetDebt50", getInt(c, "frequency_target_debt_50"));
        setOptionalIntField(obj, "frequencyTargetDebt100", getInt(c, "frequency_target_debt_100"));
        setOptionalIntField(obj, "frequencyTargetDebt300", getInt(c, "frequency_target_debt_300"));
        setOptionalStringField(
                obj, "frequencyUrgentWindow", getString(c, "frequency_urgent_window"));
        setOptionalIntField(
                obj,
                "frequencyIssuesSinceLastRealBet",
                getInt(c, "frequency_issues_since_last_real_bet"));
        setOptionalIntField(
                obj,
                "frequencyIssuesSinceLastFloorBet",
                getInt(c, "frequency_issues_since_last_floor_bet"));
        setOptionalIntField(
                obj,
                "frequencyConsecutiveFloorBets",
                getInt(c, "frequency_consecutive_floor_bets"));
        setOptionalIntField(obj, "frequencyFloorMinGap", getInt(c, "frequency_floor_min_gap"));
        setOptionalIntField(
                obj, "frequencyFloorPacingReady", getInt(c, "frequency_floor_pacing_ready"));
        setOptionalStringField(
                obj, "frequencyFloorPacingReason", getString(c, "frequency_floor_pacing_reason"));
        setOptionalStringField(obj, "decisionQualityPath", getString(c, "decision_quality_path"));
        setOptionalIntField(obj, "evidenceDomainFrozen", getInt(c, "evidence_domain_frozen"));
        setOptionalStringField(
                obj, "evidenceDomainFrozenIssue", getString(c, "evidence_domain_frozen_issue"));
        setOptionalLongField(
                obj, "evidenceDomainFrozenAt", getLong(c, "evidence_domain_frozen_at"));
        setOptionalStringField(obj, "dynamicMatchedLevel", getString(c, "dynamic_matched_level"));
        setOptionalIntField(obj, "dynamicRealSample", getInt(c, "dynamic_real_sample"));
        setOptionalIntField(obj, "dynamicShadowSample", getInt(c, "dynamic_shadow_sample"));
        setOptionalStringField(obj, "moduleBuildVersion", getString(c, "module_build_version"));
    }

    private String[][] linkModificationV2DoubleMap() {
        return new String[][] {
            {"base_gate", "baseGate"}, {"risk_adjusted_gate", "riskAdjustedGate"},
            {"frequency_gate_adjust", "frequencyGateAdjust"},
            {"effective_probe_gate", "effectiveProbeGate"}, {"raw_edge", "rawEdge"},
            {"effective_probe_edge", "effectiveProbeEdge"},
            {"frequency_50", "realBetFrequency50"}, {"frequency_100", "realBetFrequency100"},
            {"frequency_300", "realBetFrequency300"},
            {"frequency_debt_score", "frequencyDebtScore"},
            {"frequency_support_score", "frequencySupportScore"},
            {"qualified_probe_score", "qualifiedProbeScore"},
            {"shadow_promotion_score", "shadowPromotionScore"},
            {"need_supply_score", "needSupplyScore"}, {"counter_risk", "counterRisk"}
        };
    }

    private String[][] linkModificationV2IntMap() {
        return new String[][] {
            {"qualified_probe", "qualifiedProbe"},
            {"real_trade", "realTrade"},
            {"coverage_available", "coverageAvailable"},
            {"third_quality_available", "thirdQualityAvailable"},
            {"promotion_domain_available", "promotionDomainAvailable"}
        };
    }

    private String[][] linkModificationV2TextMap() {
        return new String[][] {
            {"frequency_state", "frequencyState"},
            {"frequency_state_version", "frequencyStateVersion"},
            {"shadow_promotion_domain", "shadowPromotionDomain"},
            {"decision_path", "decisionPath"},
            {"bet_domain", "betDomain"},
            {"probe_reject_reason", "probeRejectReason"},
            {"probe_eligibility_version", "probeEligibilityVersion"},
            {"rule_health_status", "ruleHealthStatus"},
            {"snapshot_freshness", "snapshotFreshness"},
            {"warm_profile_freshness", "warmProfileFreshness"},
            {"cold_profile_freshness", "coldProfileFreshness"},
            {"deep_profile_freshness", "deepProfileFreshness"},
            {"decision_version_v2", "decisionVersionV2"},
            {"evidence_as_of_issue", "evidenceAsOfIssue"},
            {"snapshot_as_of_issue", "snapshotAsOfIssue"}
        };
    }

    private void setDoubleRawField(Object o, String field, double v) {
        try {
            Field f = o.getClass().getField(field);
            f.setDouble(o, v);
        } catch (Throwable e) {
        }
    }

    private String[][] mainChainDoubleMap() {
        return new String[][] {
            {"position_need_score", "positionNeedScore"},
            {"number_supply_score", "numberSupplyScore"},
            {"coverage_quality", "coverageQuality"},
            {"third_quality", "thirdQuality"},
            {"counter_evidence_score", "counterEvidenceScore"},
            {"similar_hit_rate", "similarHitRate"},
            {"teacher_sample_reliability", "teacherSampleReliability"},
            {"teacher_recent_validity", "teacherRecentValidity"},
            {"teacher_evidence_contribution", "teacherEvidenceContribution"},
            {"teacher_counter_contribution", "teacherCounterContribution"},
            {"v20_sample_reliability", "v20SampleReliability"},
            {"v20_recent_validity", "v20RecentValidity"},
            {"v20_evidence_contribution", "v20EvidenceContribution"},
            {"v20_counter_contribution", "v20CounterContribution"},
            {"final_p_before_diagnosis", "finalPBeforeDiagnosis"},
            {"final_p_after_diagnosis", "finalPAfterDiagnosis"},
            {"gate", "gate"},
            {"edge", "edge"},
            {"promotion_wilson_lower_bound", "promotionWilsonLowerBound"}
        };
    }

    private void setIntField(Object o, String field, int v) {
        try {
            Field f = o.getClass().getField(field);
            f.setInt(o, v);
        } catch (Throwable e) {
        }
    }

    private String[][] mainChainIntMap() {
        return new String[][] {
            {"candidate_rank", "rank"},
            {"realtime_top40_rank", "realtimeTop40Rank"},
            {"adaptive_input_flag", "adaptiveInputFlag"},
            {"similar_sample_count", "similarSampleCount"},
            {"real_probe_allowed", "realProbeAllowed"},
            {"teacher_issue_lag", "teacherIssueLag"},
            {"teacher_fresh_by_issue", "teacherFreshByIssue"},
            {"probe_eligible", "probeEligible"},
            {"shadow_probe_eligible", "shadowProbeEligible"},
            {"shadow_settled_count", "shadowSettledCount"},
            {"shadow_domain_sample", "shadowDomainSample"},
            {"predecision_opportunity", "preDecisionOpportunity"},
            {"hard_block_flag", "hardBlockFlag"},
            {"evidence_settled_only", "evidenceSettledOnly"},
            {"evidence_sample_count", "evidenceSampleCount"},
            {"evidence_hit_count", "evidenceHitCount"},
            {"evidence_miss_count", "evidenceMissCount"}
        };
    }

    private void setStringField(Object o, String field, String v) {
        try {
            Field f = o.getClass().getField(field);
            f.set(o, v == null ? "" : v);
        } catch (Throwable e) {
        }
    }

    private String[][] mainChainTextMap() {
        return new String[][] {
            {"snapshot_key", "snapshotId"},
            {"candidate_key", "candidateEvidenceKey"},
            {"window_state_key", "windowStateKey"},
            {"dynamic_window_state_key", "dynamicWindowStateKey"},
            {"final_decision_window_state_key", "finalDecisionWindowStateKey"},
            {"probability_window_state_key", "probabilityWindowStateKey"},
            {"cluster_key", "clusterKey"},
            {"factor_key", "factorKey"},
            {"teacher_status", "teacherStatus"},
            {"teacher_status_class", "teacherStatusClass"},
            {"teacher_number_status", "teacherNumberStatus"},
            {"v20_status", "v20Status"},
            {"v20_action_advice", "v20ActionAdvice"},
            {"adaptive_status", "adaptiveStatus"},
            {"shadow_promotion_status", "shadowPromotionStatus"},
            {"reject_code", "rejectCode"},
            {"real_probe_reason", "realProbeReason"},
            {"teacher_number_history_sample", "teacherNumberHistorySample"},
            {"teacher_domain_shadow_sample", "teacherDomainShadowSample"},
            {"teacher_domain_real_sample", "teacherDomainRealSample"},
            {"teacher_apply_error", "teacherApplyError"},
            {"predecision_domain_version", "preDecisionDomainVersion"},
            {"teacher_runtime_apply_version", "teacherRuntimeApplyVersion"},
            {"teacher_as_of_issue", "teacherAsOfIssue"},
            {"block_tier", "blockTier"},
            {"hard_risk_code", "hardRiskCode"},
            {"observe_reason", "observeReason"},
            {"risk_severity", "riskSeverity"},
            {"hard_block_source", "hardBlockSource"},
            {"evidence_source", "evidenceSource"},
            {"promotion_coarse_domain", "promotionCoarseDomain"},
            {"promotion_mid_domain", "promotionMidDomain"},
            {"promotion_fine_domain", "promotionFineDomain"},
            {"probe_level", "probeLevel"},
            {"promotion_eligibility_reason", "promotionEligibilityReason"},
            {"bet_source_engine", "betSourceEngine"},
            {"decision_gate_version", "finalDecisionVersion"},
            {"field_chain_version", "fieldChainVersion"},
            {"schema_fix_version", "schemaFixVersion"}
        };
    }

    private void readDynamicCausalFields(Cursor c, Object o) {
        if (c == null || o == null) return;
        String[][] textMap = dynamicCausalTextMap();
        for (int i = 0; i < textMap.length; i++)
            setStringField(o, textMap[i][1], getString(c, textMap[i][0]));
        String[][] intMap = dynamicCausalIntMap();
        for (int i = 0; i < intMap.length; i++)
            setIntField(o, intMap[i][1], getInt(c, intMap[i][0]));
        String[][] doubleMap = dynamicCausalDoubleMap();
        for (int i = 0; i < doubleMap.length; i++)
            setDoubleField(o, doubleMap[i][1], getDouble(c, doubleMap[i][0]));
        try {
            Models.normalizeDynamicCausalBridgeFields(o);
        } catch (Throwable e) {
        }
    }

    private void setDoubleField(Object o, String field, double v) {
        try {
            Field f = o.getClass().getField(field);
            f.setDouble(o, AppConfig.clampDynamicCausalScore(v));
        } catch (Throwable e) {
        }
    }

    private String[][] dynamicCausalDoubleMap() {
        return new String[][] {
            {"dynamic_cluster_hit_rate", "dynamicClusterHitRate"},
            {"dynamic_cluster_recent_hit_rate", "dynamicClusterRecentHitRate"},
            {"dynamic_cluster_confidence", "dynamicClusterConfidence"},
            {"dynamic_cluster_distance", "dynamicClusterDistance"},
            {"dynamic_cluster_volatility", "dynamicClusterVolatility"},
            {"dynamic_cluster_counter_score", "dynamicClusterCounterScore"},
            {"position_need_strength", "positionNeedStrength"},
            {"position_need_chaos_risk", "positionNeedChaosRisk"},
            {"position_need_repair_score", "positionNeedRepairScore"},
            {"position_need_relay_score", "positionNeedRelayScore"},
            {"number_supply_strength", "numberSupplyStrength"},
            {"number_supply_risk", "numberSupplyRisk"},
            {"need_supply_match_score", "needSupplyMatchScore"},
            {"need_supply_third_quality", "needSupplyThirdQuality"},
            {"need_supply_role_diversity", "needSupplyRoleDiversity"},
            {"need_supply_risk", "needSupplyRisk"},
            {"counterfactual_rank_score", "counterfactualRankScore"},
            {"counterfactual_why_not_risk", "counterfactualWhyNotRisk"},
            {"cause_ablation_score", "causeAblationScore"},
            {"adaptive_cluster_hit_rate", "adaptiveClusterHitRate"},
            {"adaptive_cluster_recent_hit_rate", "adaptiveClusterRecentHitRate"},
            {"adaptive_cluster_confidence", "adaptiveClusterConfidence"},
            {"adaptive_cause_score", "adaptiveCauseScore"},
            {"adaptive_counter_score", "adaptiveCounterScore"},
            {"adaptive_promote_score", "adaptivePromoteScore"},
            {"adaptive_freeze_score", "adaptiveFreezeScore"},
            {"promotion_cluster_hit_rate", "promotionClusterHitRate"},
            {"promotion_cluster_recent_hit_rate", "promotionClusterRecentHitRate"}
        };
    }

    private String[][] dynamicCausalIntMap() {
        return new String[][] {
            {"dynamic_cluster_sample", "dynamicClusterSample"},
            {"dynamic_cluster_hit_count", "dynamicClusterHitCount"},
            {"dynamic_cluster_miss_count", "dynamicClusterMissCount"},
            {"dynamic_cluster_recent_sample", "dynamicClusterRecentSample"},
            {"dynamic_cluster_recent_hit_count", "dynamicClusterRecentHitCount"},
            {"dynamic_cluster_max_lose", "dynamicClusterMaxLose"},
            {"k3_role_main_number", "k3RoleMainNumber"},
            {"k3_role_assist_number", "k3RoleAssistNumber"},
            {"k3_role_defense_number", "k3RoleDefenseNumber"},
            {"counterfactual_rank", "counterfactualRank"},
            {"cause_ablation_false_cause_count", "causeAblationFalseCauseCount"},
            {"adaptive_judge_candidate_count", "adaptiveJudgeCandidateCount"},
            {"adaptive_cluster_sample", "adaptiveClusterSample"},
            {"promotion_cluster_sample", "promotionClusterSample"}
        };
    }

    private String[][] dynamicCausalTextMap() {
        return new String[][] {
            {"dynamic_cluster_id", "dynamicClusterId"},
            {"dynamic_cluster_status", "dynamicClusterStatus"},
            {"dynamic_cluster_prototype_key", "dynamicClusterPrototypeKey"},
            {"dynamic_cluster_feature_key", "dynamicClusterFeatureKey"},
            {"dynamic_cluster_version", "dynamicClusterVersion"},
            {"dynamic_cluster_reason", "dynamicClusterReason"},
            {"position_need_key", "positionNeedKey"},
            {"position_need_summary", "positionNeedSummary"},
            {"number_supply_key", "numberSupplyKey"},
            {"number_supply_summary", "numberSupplySummary"},
            {"need_supply_match_key", "needSupplyMatchKey"},
            {"need_supply_summary", "needSupplySummary"},
            {"k3_role_main", "k3RoleMain"},
            {"k3_role_assist", "k3RoleAssist"},
            {"k3_role_defense", "k3RoleDefense"},
            {"k3_role_structure", "k3RoleStructure"},
            {"counterfactual_reason_key", "counterfactualReasonKey"},
            {"counterfactual_why_not", "counterfactualWhyNot"},
            {"counterfactual_summary", "counterfactualSummary"},
            {"cause_ablation_key", "causeAblationKey"},
            {"cause_ablation_summary", "causeAblationSummary"},
            {"miss_reason_type", "missReasonType"},
            {"miss_reason_matrix_key", "missReasonMatrixKey"},
            {"miss_reason_detail", "missReasonDetail"},
            {"adaptive_judge_status", "adaptiveJudgeStatus"},
            {"adaptive_judge_action", "adaptiveJudgeAction"},
            {"adaptive_judge_domain", "adaptiveJudgeDomain"},
            {"adaptive_judge_reason", "adaptiveJudgeReason"},
            {"adaptive_judge_version", "adaptiveJudgeVersion"},
            {"promotion_cluster_status", "promotionClusterStatus"},
            {"promotion_cluster_reason", "promotionClusterReason"},
            {"why_bet", "whyBet"},
            {"why_no_bet", "whyNoBet"},
            {"why_hit", "whyHit"},
            {"why_miss", "whyMiss"},
            {"dynamic_causal_summary", "dynamicCausalSummary"}
        };
    }

    private void readFieldChainValues(Cursor c, Object obj) {
        if (c == null || obj == null) {
            return;
        }
        readFieldChainInt(c, obj, "teacher_weak_no_bet_flag", "teacherWeakNoBetFlag");
        readFieldChainInt(c, obj, "teacher_matrix_weak_no_bet_flag", "teacherMatrixWeakNoBetFlag");
        readFieldChainInt(
                c,
                obj,
                "teacher_matrix_ordinary_bet_block_flag",
                "teacherMatrixOrdinaryBetBlockFlag");
        readFieldChainInt(
                c, obj, "teacher_matrix_supports_final_gate", "teacherMatrixSupportsFinalGate");
        readFieldChainInt(c, obj, "teacher_matrix_supports_bet", "teacherMatrixSupportsBet");
        readFieldChainString(c, obj, "teacher_ordinary_bet_policy", "teacherOrdinaryBetPolicy");
        readFieldChainString(
                c, obj, "teacher_matrix_ordinary_bet_policy", "teacherMatrixOrdinaryBetPolicy");
        readFieldChainString(c, obj, "teacher_field_source", "teacherFieldSource");
        readFieldChainString(c, obj, "teacher_field_version", "teacherFieldVersion");
        readFieldChainString(c, obj, "teacher_number_status", "teacherNumberStatus");
        readFieldChainString(c, obj, "teacher_status", "teacherStatus");
        readFieldChainDouble(
                c, obj, "teacher_evidence_contribution", "teacherEvidenceContribution");
        readFieldChainDouble(
                c,
                obj,
                "teacher_matrix_evidence_contribution",
                "teacherMatrixEvidenceContribution");
        readFieldChainDouble(c, obj, "teacher_counter_contribution", "teacherCounterContribution");
        readFieldChainDouble(
                c, obj, "teacher_matrix_counter_contribution", "teacherMatrixCounterContribution");
        readFieldChainDouble(c, obj, "teacher_sample_reliability", "teacherSampleReliability");
        readFieldChainDouble(c, obj, "teacher_recent_validity", "teacherRecentValidity");

        readFieldChainString(c, obj, "v20_status", "v20Status");
        readFieldChainString(c, obj, "dynamic_status", "dynamicStatus");
        readFieldChainString(c, obj, "dynamic_cause_status", "dynamicCauseStatus");
        readFieldChainDouble(c, obj, "v20_support_score", "v20SupportScore");
        readFieldChainDouble(c, obj, "v20_risk_score", "v20RiskScore");
        readFieldChainDouble(c, obj, "v20_evidence_contribution", "v20EvidenceContribution");
        readFieldChainDouble(c, obj, "v20_counter_contribution", "v20CounterContribution");
        readFieldChainDouble(c, obj, "v20_sample_reliability", "v20SampleReliability");
        readFieldChainDouble(c, obj, "v20_recent_validity", "v20RecentValidity");
        readFieldChainString(c, obj, "v20_action_advice", "v20ActionAdvice");
        readFieldChainString(c, obj, "v20_window_state_key", "v20WindowStateKey");
        readFieldChainString(c, obj, "window_state_key", "windowStateKey");
        readFieldChainString(
                c, obj, "final_decision_window_state_key", "finalDecisionWindowStateKey");
        readFieldChainString(c, obj, "dynamic_window_state_key", "dynamicWindowStateKey");
        readFieldChainString(c, obj, "probability_window_state_key", "probabilityWindowStateKey");

        readFieldChainDouble(c, obj, "coverage_quality_score", "coverageQualityScore");
        readFieldChainDouble(c, obj, "third_number_quality_score", "thirdNumberQualityScore");
        readFieldChainDouble(c, obj, "k3_coverage_risk", "k3CoverageRisk");
        readFieldChainInt(c, obj, "false_high_p_flag", "falseHighPFlag");
        readFieldChainInt(c, obj, "probability_false_high_p_flag", "probabilityFalseHighPFlag");
        readFieldChainDouble(c, obj, "probability_false_high_risk", "probabilityFalseHighRisk");
        readFieldChainInt(
                c, obj, "probability_preferred_edge_band", "probabilityPreferredEdgeBand");
        readFieldChainInt(
                c, obj, "probability_teacher_weak_or_empty", "probabilityTeacherWeakOrEmpty");
        readFieldChainInt(
                c, obj, "probability_v20_unknown_or_empty", "probabilityV20UnknownOrEmpty");
        readFieldChainInt(c, obj, "probability_strong_cause_flag", "probabilityStrongCauseFlag");
        readFieldChainString(c, obj, "probability_coverage_summary", "probabilityCoverageSummary");
        readFieldChainString(c, obj, "probability_action_advice", "probabilityActionAdvice");

        readFieldChainInt(c, obj, "dynamic_gate_false_high_p_flag", "dynamicGateFalseHighPFlag");
        readFieldChainInt(
                c, obj, "dynamic_gate_preferred_edge_band", "dynamicGatePreferredEdgeBand");
        readFieldChainString(c, obj, "dynamic_gate_edge_policy", "dynamicGateEdgePolicy");
        readFieldChainDouble(c, obj, "dynamic_gate_coverage_risk", "dynamicGateCoverageRisk");
        readFieldChainDouble(
                c, obj, "dynamic_gate_coverage_quality_score", "dynamicGateCoverageQualityScore");
        readFieldChainDouble(
                c, obj, "dynamic_gate_third_number_quality", "dynamicGateThirdNumberQuality");
        readFieldChainDouble(
                c, obj, "dynamic_gate_virtual_edge_risk", "dynamicGateVirtualEdgeRisk");
        readFieldChainString(c, obj, "dynamic_gate_version", "dynamicGateVersion");

        readFieldChainString(c, obj, "final_decision_version", "finalDecisionVersion");
        readFieldChainString(c, obj, "final_decision_edge_policy", "finalDecisionEdgePolicy");
        readFieldChainString(c, obj, "final_decision_teacher_policy", "finalDecisionTeacherPolicy");
        readFieldChainString(c, obj, "final_decision_v20_policy", "finalDecisionV20Policy");
        readFieldChainString(c, obj, "final_decision_bet_domain", "finalDecisionBetDomain");
        readFieldChainString(c, obj, "final_decision_reject_code", "finalDecisionRejectCode");
        readFieldChainString(c, obj, "final_decision_shadow_reason", "finalDecisionShadowReason");
        readFieldChainDouble(c, obj, "final_decision_coverage_risk", "finalDecisionCoverageRisk");
        readFieldChainDouble(
                c, obj, "final_decision_third_number_quality", "finalDecisionThirdNumberQuality");
        readFieldChainInt(
                c, obj, "final_decision_false_high_p_flag", "finalDecisionFalseHighPFlag");

        syncFieldChainAliases(obj);
    }

    private void syncFieldChainAliases(Object obj) {
        if (obj == null) {
            return;
        }
        if (getOptionalStringField(obj, "teacherMatrixStatus", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "teacherMatrixStatus",
                    firstNonEmpty(
                            getOptionalStringField(obj, "teacherNumberStatus", ""),
                            getOptionalStringField(obj, "teacherStatus", "")));
        }
        if (getOptionalStringField(obj, "teacherStatus", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "teacherStatus",
                    firstNonEmpty(
                            getOptionalStringField(obj, "teacherMatrixStatus", ""),
                            getOptionalStringField(obj, "teacherNumberStatus", "")));
        }
        if (getOptionalStringField(obj, "v20Status", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "v20Status",
                    firstNonEmpty(
                            getOptionalStringField(obj, "dynamicStatus", ""),
                            getOptionalStringField(obj, "dynamicCauseStatus", "")));
        }
        if (getOptionalStringField(obj, "dynamicStatus", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "dynamicStatus",
                    firstNonEmpty(
                            getOptionalStringField(obj, "v20Status", ""),
                            getOptionalStringField(obj, "dynamicCauseStatus", "")));
        }
        if (getOptionalStringField(obj, "windowStateKey", "").length() == 0) {
            setOptionalStringField(
                    obj,
                    "windowStateKey",
                    firstNonEmpty(
                            getOptionalStringField(obj, "finalDecisionWindowStateKey", ""),
                            firstNonEmpty(
                                    getOptionalStringField(obj, "dynamicWindowStateKey", ""),
                                    getOptionalStringField(obj, "probabilityWindowStateKey", ""))));
        }
        double cr =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "k3CoverageRisk", "dynamicGateCoverageRisk", "finalDecisionCoverageRisk"
                        });
        if (cr > 0.0d) {
            setOptionalDoubleField(obj, "k3CoverageRisk", cr);
            setOptionalDoubleField(obj, "dynamicGateCoverageRisk", cr);
        }
        double third =
                maxOptionalDouble(
                        obj,
                        new String[] {
                            "thirdNumberQualityScore",
                            "dynamicGateThirdNumberQuality",
                            "finalDecisionThirdNumberQuality"
                        });
        if (third > 0.0d) {
            setOptionalDoubleField(obj, "thirdNumberQualityScore", third);
            setOptionalDoubleField(obj, "dynamicGateThirdNumberQuality", third);
        }
        int fh =
                maxOptionalInt(
                        obj,
                        new String[] {
                            "falseHighPFlag",
                            "probabilityFalseHighPFlag",
                            "dynamicGateFalseHighPFlag",
                            "finalDecisionFalseHighPFlag"
                        });
        if (fh > 0) {
            setOptionalIntField(obj, "falseHighPFlag", fh);
            setOptionalIntField(obj, "dynamicGateFalseHighPFlag", fh);
        }
    }

    private int maxOptionalInt(Object obj, String[] fields) {
        int best = 0;
        if (fields == null) {
            return best;
        }
        for (int i = 0; i < fields.length; i++) {
            int v = getOptionalIntField(obj, fields[i], 0);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private int getOptionalIntField(Object obj, String fieldName, int fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object value = f.get(obj);

            if (value instanceof Integer) {
                return ((Integer) value).intValue();
            }

            if (value instanceof Long) {
                return (int) ((Long) value).longValue();
            }

            if (value instanceof Number) {
                return ((Number) value).intValue();
            }

            if (value != null) {
                return Integer.parseInt(String.valueOf(value));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private double maxOptionalDouble(Object obj, String[] fields) {
        double best = 0.0d;
        if (fields == null) {
            return best;
        }
        for (int i = 0; i < fields.length; i++) {
            double v = getOptionalDoubleField(obj, fields[i], 0.0d);
            if (v > best) {
                best = v;
            }
        }
        return best;
    }

    private double getOptionalDoubleField(Object obj, String fieldName, double fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object value = f.get(obj);

            if (value instanceof Double) {
                return ((Double) value).doubleValue();
            }

            if (value instanceof Float) {
                return ((Float) value).doubleValue();
            }

            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }

            if (value != null) {
                return Double.parseDouble(String.valueOf(value));
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private String getOptionalStringField(Object obj, String fieldName, String fallback) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return fallback;
        }

        try {
            java.lang.reflect.Field f = obj.getClass().getField(fieldName);
            Object value = f.get(obj);

            if (value != null) {
                return String.valueOf(value);
            }
        } catch (Throwable e) {
        }

        return fallback;
    }

    private void readFieldChainDouble(Cursor c, Object obj, String column, String field) {
        setOptionalDoubleField(obj, field, getDouble(c, column));
    }

    private void readTeacherMatrixFieldsToObject(Cursor c, Object obj) {
        if (c == null || obj == null) {
            return;
        }

        setOptionalIntField(obj, "teacherMatrixSample", getInt(c, "teacher_matrix_sample"));
        setOptionalDoubleField(
                obj, "teacherMatrixHitRate", getDouble(c, "teacher_matrix_hit_rate"));
        setOptionalDoubleField(
                obj, "teacherMatrixConfidence", getDouble(c, "teacher_matrix_confidence"));
        setOptionalDoubleField(
                obj, "teacherMatrixProbAdjust", getDouble(c, "teacher_matrix_prob_adjust"));
        setOptionalDoubleField(
                obj, "teacherMatrixGateAdjust", getDouble(c, "teacher_matrix_gate_adjust"));
        setOptionalDoubleField(
                obj, "teacherMatrixRiskPenalty", getDouble(c, "teacher_matrix_risk_penalty"));
        setOptionalIntField(obj, "teacherMatrixApplied", getInt(c, "teacher_matrix_applied"));
        setOptionalStringField(obj, "teacherMatrixStatus", getString(c, "teacher_matrix_status"));
        setOptionalStringField(
                obj, "teacherMatrixProfileKey", getString(c, "teacher_matrix_profile_key"));
        setOptionalStringField(
                obj, "teacherMatrixComboKey", getString(c, "teacher_matrix_combo_key"));
        setOptionalStringField(obj, "teacherMatrixSummary", getString(c, "teacher_matrix_summary"));
    }

    private void countRealBetIssuesFromTrades(
            HashSet<String> issueSet, HashSet<String> betIssues, int window) {
        if (issueSet == null || betIssues == null || issueSet.size() == 0) {
            return;
        }

        Cursor c = null;

        try {
            int queryLimit = window * 5;

            if (queryLimit < 80) {
                queryLimit = 80;
            }

            if (queryLimit > 5000) {
                queryLimit = 5000;
            }

            c =
                    query(
                                    DbHelper.TABLE_TRADES,
                                    new String[]{"target_issue","settled","result"},
                                    "action=? AND stat_scope=?",
                                    new String[] {
                                        AppConfig.DECISION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    },
                                    null,
                                    null,
                                    "bet_time DESC, created_at DESC, _id DESC",
                                    String.valueOf(queryLimit));

            while (c.moveToNext()) {
                String targetIssue = getString(c, "target_issue");

                if (targetIssue == null || targetIssue.length() == 0) {
                    continue;
                }

                if (!issueSet.contains(targetIssue)) {
                    continue;
                }

                int settled = getInt(c, "settled");
                String result = AppConfig.normalizeResult(getString(c, "result"));

                if (settled == 1 && !AppConfig.isTerminalResult(result)) {
                    continue;
                }

                betIssues.add(targetIssue);
            }
        } catch(Throwable e){throw new ReportReadFailureCP4("REPORT_QUERY_FAILED",e);} finally {
            closeCursor(c);
        }
    }

    public List<Models.Draw> getRecentDraws(int limit) {
        if(limit>5000)throw new ReportReadFailureCP4("REPORT_HISTORY_LIMIT");
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();

        if (limit <= 0) {
            return list;
        }

        Cursor c = null;

        try {
            StringBuilder columnsR927 = new StringBuilder();
            for (String columnR927 : DrawHistoryProjectionR817.COLUMNS) {
                if (columnsR927.length() > 0) columnsR927.append(',');
                columnsR927.append(columnR927);
            }
            c = select( "SELECT " + columnsR927 + " FROM "
                    + DbHelper.TABLE_DRAWS + " WHERE dirty=0 ORDER BY issue DESC, open_time DESC, _id DESC LIMIT " + limit, null);

            while (c.moveToNext()) {
                list.add(readDraw(c));
            }
        } finally {
            closeCursor(c);
        }

        ArrayList<Models.Draw> ordered = new ArrayList<Models.Draw>();

        for (int i = list.size() - 1; i >= 0; i--) {
            ordered.add(list.get(i));
        }

        return ordered;
    }

    private Models.Draw readDraw(Cursor c) {
        Models.Draw d = new Models.Draw();

        d.id = getLong(c, "_id");
        d.issue = getString(c, "issue");
        d.openTime = getLong(c, "open_time");
        d.fetchTime = getLong(c, "fetch_time");
        d.sourceLine = getString(c, "source_line");

        d.numbers[0] = getInt(c, "n1");
        d.numbers[1] = getInt(c, "n2");
        d.numbers[2] = getInt(c, "n3");
        d.numbers[3] = getInt(c, "n4");
        d.numbers[4] = getInt(c, "n5");
        d.numbers[5] = getInt(c, "n6");
        d.numbers[6] = getInt(c, "n7");
        d.numbers[7] = getInt(c, "n8");
        d.numbers[8] = getInt(c, "n9");
        d.numbers[9] = getInt(c, "n10");

        d.rawJson = getString(c, "raw_json");
        d.dirty = getInt(c, "dirty");
        d.dirtyReason = getString(c, "dirty_reason");

        try {
            d.normalizeDirty();
        } catch (Throwable e) {
        }

        return d;
    }

    private int countDecisionByType(String decision) {
        if (decision == null || decision.length() == 0) {
            return 0;
        }

        return countTable(DbHelper.TABLE_DECISIONS, "decision=?", new String[] {decision});
    }

    public String getResearchStateR811(String key, String fallback) {
        String safeKey = key == null ? "" : key.trim();
        if (!safeKey.startsWith("r811_research_")) return fallback;
        return getState(safeKey, fallback);
    }

    public String getLatestDrawIssue() {
        Models.Draw d = getLatestDraw();
        return d == null ? "" : d.issue;
    }

    public Models.Draw getLatestDraw() {
        Cursor c = null;

        try {
            c =
                    query(
                                    DbHelper.TABLE_DRAWS,
                                    DrawHistoryProjectionR817.COLUMNS,
                                    "dirty=0",
                                    null,
                                    null,
                                    null,
                                    "issue DESC, open_time DESC, _id DESC",
                                    "1");

            if (c.moveToFirst()) {
                return readDraw(c);
            }
        } finally {
            closeCursor(c);
        }

        return null;
    }


 private Cursor query(String table,String[] columns,String selection,String[] args,String group,String having,String order){return query(table,columns,selection,args,group,having,order,null);}
 private Cursor query(String table,String[] columns,String selection,String[] args,String group,String having,String order,String limit){return select(android.database.sqlite.SQLiteQueryBuilder.buildQueryString(false,table,columns,selection,group,having,order,limit),args);}
}
