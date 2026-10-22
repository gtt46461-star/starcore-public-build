package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Read-only bridge from the frozen V51 mainline to the legacy Teacher/推理/反推/因果/反哺 UI
 * vocabulary.  It never runs a second predictor and never modifies legacy assets.
 */
public final class LegacyPredrawContextBridgeR915 {
    public static final String TABLE = "v57_legacy_predraw_context_r915";
    public static final String POST_TABLE = "v57_legacy_postsettlement_context_r919";
    public static final String NAMESPACE = "LEGACY_FIXED_ASSETS";

    public static final class Row {
        public final String targetIssue, teacher, reasoning, reverse, causal, feedback, trend,
                shape, relation, recordHash;

        Row(String target, String teacher, String reasoning, String reverse, String causal,
                String feedback, String trend, String shape, String relation, String hash) {
            targetIssue = safe(target); this.teacher = safe(teacher); this.reasoning = safe(reasoning);
            this.reverse = safe(reverse); this.causal = safe(causal); this.feedback = safe(feedback);
            this.trend = safe(trend); this.shape = safe(shape); this.relation = safe(relation);
            recordHash = safe(hash);
        }
    }

    public static final class PostRow {
        public final String targetIssue,status,attribution,feedback,predrawHash,drawHash,recordHash;
        public final int actualNumber,hitLabel;

        PostRow(String target,String status,String attribution,String feedback,String predrawHash,
                String drawHash,String recordHash,int actual,int hit) {
            targetIssue=safe(target);this.status=safe(status);this.attribution=safe(attribution);
            this.feedback=safe(feedback);this.predrawHash=safe(predrawHash);this.drawHash=safe(drawHash);
            this.recordHash=safe(recordHash);actualNumber=actual;hitLabel=hit;
        }
    }

    private LegacyPredrawContextBridgeR915() {}

    static void freezeWithinTransaction(
            SQLiteDatabase db, V51RuntimeResult result, long frozenAt) {
        if (db == null || !db.inTransaction() || result == null || result.selected == null
                || result.selected.action == null || result.context == null)
            throw new IllegalStateException("R915_LEGACY_CONTEXT_TX_INPUT_INVALID");
        ensureSchema(db);
        V51Models.PositionAction action = result.selected.action;
        int p = action.position - 1;
        V51Models.StateTrajectorySnapshot state = result.states != null
                        && p >= 0 && p < result.states.length ? result.states[p] : null;
        V51Models.ExpertEvidence teacherEvidence = teacher(result.numberModel);
        String teacher = teacherEvidence == null
                ? "教师后验=不可用｜原因=TEACHER_EXPERT_MISSING"
                : "教师后验=" + at(teacherEvidence.evidence, p, "N/A")
                        + "｜信任=" + number(at(teacherEvidence.trust, p, -1.0d))
                        + "｜不确定性=" + number(at(teacherEvidence.uncertainty, p, -1.0d))
                        + "｜反证=" + at(teacherEvidence.counterEvidence, p, "N/A");
        String trend = state == null ? "趋势状态=N/A"
                : "趋势生命周期=" + state.lifecycle.name() + "｜连续=" + state.runLength
                        + "｜剩余寿命=" + number(state.remainingLife)
                        + "｜变局风险=" + number(state.changePointRisk);
        String shape = state == null ? "形态原型=N/A"
                : "形态原型=" + state.prototypeId + "｜状态域=" + state.domainId
                        + "｜后验=" + number(state.posterior)
                        + "｜置信=" + number(state.confidence);
        double assignment = action.metaFeatures != null && action.metaFeatures.length > 15
                ? action.metaFeatures[15] : Double.NaN;
        double window = action.metaFeatures != null && action.metaFeatures.length > 20
                ? action.metaFeatures[20] : Double.NaN;
        String algorithmStatus = state(db, "r915_algorithm_production_status");
        String algorithmName = state(db, "r915_algorithm_production_name_zh");
        String algorithmEvidence = state(db, "r915_algorithm_production_evidence");
        String relation = "跨位分配支持=" + number(assignment)
                + "｜多窗共识=" + number(window)
                + "｜单位置K3=" + action.position + ':' + action.n1 + '/' + action.n2 + '/' + action.n3
                + ("CERTIFIED_ALGORITHM_INFLUENCE".equals(algorithmStatus)
                        ? "｜发明算法生产影响=" + algorithmName
                                + "｜FutureOOS证据=" + shortHash(algorithmEvidence)
                        : "｜发明算法生产影响=未启用(" + explicit(algorithmStatus, "NO_CERTIFIED_ALGORITHM") + ')');
        String reasoning = "正向推理=100号码面→Sinkhorn置换约束→1200固定K3→Rank1"
                + "｜raw=" + number(action.rawActionP)
                + "｜rankScore=" + number(action.rankScore)
                + "｜第三边界=" + number(action.thirdMargin)
                + "｜稳定性=" + number(action.stability);
        String reverse = "反推矩阵=从最佳排除动作与第三边界寻找失败路径"
                + "｜熵风险=" + number(action.entropy)
                + "｜反证强度=" + number(result.selected.falsifierScore)
                + "｜不确定性=" + number(result.selected.uncertainty);
        String causal = "因果口径=机制支持/反证/消融候选，不宣称观察相关即因果"
                + "｜教师、趋势、形态、关系均为赛前冻结证据｜唯一出号仍属DecisionGate";
        String feedback = "反哺契约=开奖结果后仅由SettlementEngine按同一期冻结hash写命中/未中、"
                + "教师误导/结构命中/第三边界失败与下一代Research状态；禁止开奖后改写赛前理由";
        String canonical = result.context.targetIssue + '|' + teacher + '|' + reasoning + '|'
                + reverse + '|' + causal + '|' + feedback + '|' + trend + '|' + shape + '|'
                + relation + '|' + result.selected.contractHash;
        String hash = V51ChecksumEngine.sha256(canonical);
        Cursor old = null;
        try {
            old = db.rawQuery("SELECT record_hash FROM " + TABLE
                    + " WHERE target_issue=? LIMIT 1", new String[] {result.context.targetIssue});
            if (old.moveToFirst()) {
                if (!hash.equals(safe(old.getString(0))))
                    throw new IllegalStateException("R915_LEGACY_CONTEXT_FIRST_WRITE_CONFLICT");
                return;
            }
        } finally { if (old != null) old.close(); }
        ContentValues cv = new ContentValues();
        cv.put("target_issue", result.context.targetIssue);
        cv.put("position", action.position);
        cv.put("teacher_summary", teacher);
        cv.put("reasoning_summary", reasoning);
        cv.put("reverse_summary", reverse);
        cv.put("causal_summary", causal);
        cv.put("feedback_contract", feedback);
        cv.put("trend_summary", trend);
        cv.put("shape_summary", shape);
        cv.put("relation_summary", relation);
        cv.put("namespace", NAMESPACE);
        cv.put("frozen_at", frozenAt);
        cv.put("record_hash", hash);
        long id = db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_ABORT);
        if (id <= 0L) throw new IllegalStateException("R915_LEGACY_CONTEXT_INSERT_FAILED");
    }

    public static Row read(SQLiteDatabase db, String target) {
        if (db == null || safe(target).length() == 0) return null;
        // Read paths are deliberately DDL-free.  The table is created by the canonical writer;
        // attempting CREATE TABLE from a UI/read transaction can upgrade its lock and briefly
        // block the per-minute settlement writer on older SQLite builds.
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT teacher_summary,reasoning_summary,reverse_summary,"
                            + "causal_summary,feedback_contract,trend_summary,shape_summary,"
                            + "relation_summary,record_hash FROM " + TABLE
                            + " WHERE target_issue=? LIMIT 1", new String[] {target});
            if (!c.moveToFirst()) return null;
            return new Row(target, c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getString(5), c.getString(6),
                    c.getString(7), c.getString(8));
        } catch (Throwable ignored) { return null; }
        finally { if (c != null) c.close(); }
    }

    /** Returns the immutable post-settlement explanation written from exact draw plus predraw hash. */
    public static PostRow readPost(SQLiteDatabase db,String target) {
        if(db==null||safe(target).length()==0)return null;Cursor c=null;try{
            c=db.rawQuery("SELECT status,attribution_summary,feedback_summary,predraw_record_hash,"
                            +"draw_hash,record_hash,actual_number,hit_label FROM "+POST_TABLE
                            +" WHERE target_issue=? LIMIT 1",new String[]{target});
            if(!c.moveToFirst())return null;
            return new PostRow(target,c.getString(0),c.getString(1),c.getString(2),c.getString(3),
                    c.getString(4),c.getString(5),c.getInt(6),c.getInt(7));
        }catch(Throwable ignored){return null;}finally{if(c!=null)c.close();}
    }

    /** Precise reason used by UI/report instead of a content-free N/A. */
    public static String missingReason(SQLiteDatabase db,String target,boolean post) {
        if(db==null||!db.isOpen())return "READ_ERROR:DB_NOT_OPEN";
        if(safe(target).length()==0)return "NO_SAMPLE:TARGET_ISSUE_EMPTY";
        String table=post?POST_TABLE:TABLE;Cursor c=null;try{
            c=db.rawQuery("SELECT COUNT(1) FROM sqlite_master WHERE type='table' AND name=?",new String[]{table});
            if(!c.moveToFirst()||c.getInt(0)!=1)return "NO_SAMPLE:TABLE_MISSING:"+table;
            c.close();c=db.rawQuery("SELECT COUNT(1) FROM "+table+" WHERE target_issue=?",new String[]{target});
            if(!c.moveToFirst()||c.getInt(0)!=1)return "NO_SAMPLE:ROW_MISSING:"+table+":issue="+target;
            return "AVAILABLE";
        }catch(Throwable e){return "READ_ERROR:"+e.getClass().getSimpleName()+":table="+table;}
        finally{if(c!=null)c.close();}
    }

    /**
     * Idempotently materializes actual Teacher/matrix attribution after Production settlement.
     * This never edits the immutable pre-draw row and never invokes a predictor.
     */
    public static int settleDraw(final DbStore store,final Models.Draw draw) {
        if(store==null||draw==null||!draw.isValid())return 0;
        Integer changed=store.coordinatorR36().runWrite("R919_LEGACY_CONTEXT_SETTLEMENT",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    @Override public Integer call(SQLiteDatabase db) {
                        ensureSchema(db);SettlementInput input=settlementInput(db,draw);
                        if(input==null)return Integer.valueOf(0);
                        Row predraw=read(db,draw.issue);
                        String status=predraw==null?"NO_PREDRAW_CONTEXT":"ATTRIBUTED_EXACT_DRAW";
                        String predrawHash=predraw==null?"":predraw.recordHash;
                        String attribution;
                        String feedback;
                        if(predraw==null){
                            attribution="机械结算="+(input.hit==1?"HIT":"MISS")+"｜实际="+two(input.actual)
                                    +"｜赛前K3="+input.k3+"｜Teacher/矩阵缺失原因=PREDRAW_CONTEXT_ROW_MISSING";
                            feedback="反哺未执行｜原因=没有同一期赛前Teacher/矩阵冻结行；核心结算不受影响";
                        }else if(input.hit==1){
                            attribution="结构命中｜实际"+two(input.actual)+"∈赛前K3("+input.k3+")"
                                    +"｜Teacher="+clip(predraw.teacher,260)+"｜趋势="+clip(predraw.trend,180)
                                    +"｜形态="+clip(predraw.shape,180)+"｜关系="+clip(predraw.relation,220)
                                    +"｜量化="+input.quantitative;
                            feedback="教师反哺=命中样本进入下一期可靠度与结构贡献审计；只更新t+1以后状态，"
                                    +"不把本期开奖倒灌成本期赛前理由；命中也不自动证明Teacher因果有效";
                        }else{
                            attribution="结构未中｜实际"+two(input.actual)+"∉赛前K3("+input.k3+")"
                                    +"｜反推="+clip(predraw.reverse,300)+"｜Teacher="+clip(predraw.teacher,220)
                                    +"｜关系="+clip(predraw.relation,220)+"｜量化="+input.quantitative;
                            feedback="教师反哺=未中样本进入下一期失败向量；审计Teacher反证、第三边界、"
                                    +"熵风险、动作稳定性、窗口共识和跨位支持，只允许影响t+1以后学习状态";
                        }
                        String drawHash=DrawHashR36.sha256V2(draw);
                        String record=V51ChecksumEngine.sha256("R919_LEGACY_POST|"+draw.issue+'|'
                                +predrawHash+'|'+drawHash+'|'+input.actual+'|'+input.hit+'|'
                                +status+'|'+attribution+'|'+feedback);
                        Cursor old=null;try{old=db.rawQuery("SELECT record_hash FROM "+POST_TABLE
                                +" WHERE target_issue=? LIMIT 1",new String[]{draw.issue});
                            if(old.moveToFirst()){
                                if(!record.equals(safe(old.getString(0))))
                                    throw new IllegalStateException("R919_POSTSETTLEMENT_FIRST_WRITE_CONFLICT");
                                return Integer.valueOf(0);
                            }
                        }finally{if(old!=null)old.close();}
                        ContentValues cv=new ContentValues();cv.put("target_issue",draw.issue);
                        cv.put("status",status);cv.put("actual_number",input.actual);cv.put("hit_label",input.hit);
                        cv.put("attribution_summary",attribution);cv.put("feedback_summary",feedback);
                        cv.put("predraw_record_hash",predrawHash);cv.put("draw_hash",drawHash);
                        cv.put("settled_at",System.currentTimeMillis());cv.put("record_hash",record);
                        long id=db.insertWithOnConflict(POST_TABLE,null,cv,SQLiteDatabase.CONFLICT_ABORT);
                        return Integer.valueOf(id>0?1:0);
                    }
                },Integer.valueOf(0));
        int rows=changed==null?0:changed.intValue();try{
            store.setState("r919_legacy_postsettlement_issue",draw.issue);
            store.setState("r919_legacy_postsettlement_rows",String.valueOf(rows));
            store.setState("r919_legacy_postsettlement_at",String.valueOf(System.currentTimeMillis()));
        }catch(Throwable ignored){}return rows;
    }

    /** Read-only scan predicate for a settlement task aliased as t.
     * The optional legacy writer creates its table lazily. Before that first write,
     * every otherwise eligible task lacks a post row; a missing table must not
     * prevent the writer from ever being reached. Schema/read errors still escape.
     */
    static String missingPostRowPredicate(SQLiteDatabase db) {
        try (Cursor c = db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",
                new String[]{POST_TABLE})) {
            if (!c.moveToFirst()) return "1=1";
        }
        return "NOT EXISTS(SELECT 1 FROM " + POST_TABLE
                + " p WHERE p.target_issue=t.target_issue)";
    }

    public static void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + " ("
                + "target_issue TEXT PRIMARY KEY,position INTEGER NOT NULL,"
                + "teacher_summary TEXT NOT NULL,reasoning_summary TEXT NOT NULL,"
                + "reverse_summary TEXT NOT NULL,causal_summary TEXT NOT NULL,"
                + "feedback_contract TEXT NOT NULL,trend_summary TEXT NOT NULL,"
                + "shape_summary TEXT NOT NULL,relation_summary TEXT NOT NULL,"
                + "namespace TEXT NOT NULL,frozen_at INTEGER NOT NULL,record_hash TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+POST_TABLE+" ("
                +"target_issue TEXT PRIMARY KEY,status TEXT NOT NULL,actual_number INTEGER NOT NULL,"
                +"hit_label INTEGER NOT NULL,attribution_summary TEXT NOT NULL,feedback_summary TEXT NOT NULL,"
                +"predraw_record_hash TEXT NOT NULL,draw_hash TEXT NOT NULL,settled_at INTEGER NOT NULL,"
                +"record_hash TEXT NOT NULL UNIQUE)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r919_legacy_post_status ON "+POST_TABLE
                +"(status,settled_at DESC,target_issue DESC)");
    }

    private static SettlementInput settlementInput(SQLiteDatabase db,Models.Draw draw) {
        Cursor c=null;try{c=db.rawQuery("SELECT o.position,o.n1,o.n2,o.n3,o.actual_number,o.hit_label,"
                        +"e.raw_action_p_value,e.third_margin_value,e.entropy_risk_value,e.stability_value,"
                        +"e.window_consensus_value,e.assignment_support_value FROM "
                        +DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o LEFT JOIN "
                        +DbHelper.TABLE_R906_SELECTED_EVIDENCE+" e ON e.target_issue=o.target_issue "
                        +"WHERE o.target_issue=? AND o.pre_draw_frozen=1 AND o.settled=1 AND o.hit_label IN(0,1) LIMIT 1",
                new String[]{draw.issue});if(!c.moveToFirst())return null;
            int position=c.getInt(0),actual=c.getInt(4),hit=c.getInt(5);
            if(actual!=draw.getNumberAtPosition(position))
                throw new IllegalStateException("R919_EXACT_DRAW_POSITION_MISMATCH");
            String k3=two(c.getInt(1))+"/"+two(c.getInt(2))+"/"+two(c.getInt(3));
            String quantitative="raw="+metric(c,6)+"｜third="+metric(c,7)+"｜risk="+metric(c,8)
                    +"｜stability="+metric(c,9)+"｜window="+metric(c,10)+"｜cross="+metric(c,11);
            return new SettlementInput(position,actual,hit,k3,quantitative);
        }finally{if(c!=null)c.close();}
    }

    private static String metric(Cursor c,int index){return c.isNull(index)?"MISSING":number(c.getDouble(index));}
    private static String two(int value){return value<10?"0"+value:String.valueOf(value);}
    private static String clip(String value,int max){String x=safe(value);return x.length()<=max?x:x.substring(0,max)+"…";}
    private static final class SettlementInput {
        final int position,actual,hit;final String k3,quantitative;
        SettlementInput(int p,int a,int h,String k,String q){position=p;actual=a;hit=h;k3=k;quantitative=q;}
    }

    private static V51Models.ExpertEvidence teacher(V51PermutationNumberModelEngine.Result model) {
        if (model == null || model.experts == null) return null;
        for (V51Models.ExpertEvidence e : model.experts)
            if (e != null && "TEACHER_POSTERIOR".equals(e.expertId)) return e;
        return null;
    }

    private static String state(SQLiteDatabase db, String key) {
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT state_value FROM " + DbHelper.TABLE_APP_STATE
                            + " WHERE state_key=? LIMIT 1",
                    new String[] {key});
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        } catch (Throwable ignored) {
            return "";
        } finally {
            if (c != null) c.close();
        }
    }

    private static String explicit(String value, String fallback) {
        return safe(value).length() == 0 ? fallback : safe(value);
    }

    private static String shortHash(String value) {
        String hash = safe(value);
        return hash.length() <= 12 ? explicit(hash, "N/A") : hash.substring(0, 12);
    }

    private static String at(String[] values, int index, String fallback) {
        return values != null && index >= 0 && index < values.length
                && safe(values[index]).length() > 0 ? safe(values[index]) : fallback;
    }
    private static double at(double[] values, int index, double fallback) {
        return values != null && index >= 0 && index < values.length ? values[index] : fallback;
    }
    private static String number(double value) {
        return Double.isNaN(value) || Double.isInfinite(value)
                ? "N/A" : String.format(java.util.Locale.US, "%.5f", value);
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }

public static String missingReasonSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String target,boolean post) {
        if(db==null||!db.isOpen())return "READ_ERROR:DB_NOT_OPEN";
        if(safeSnapshotCP4(report, target).length()==0)return "NO_SAMPLE:TARGET_ISSUE_EMPTY";
        String table=post?POST_TABLE:TABLE;Cursor c=null;try{
            c=report.select("SELECT COUNT(1) FROM sqlite_master WHERE type='table' AND name=?",new String[]{table});
            if(!c.moveToFirst()||c.getInt(0)!=1)return "NO_SAMPLE:TABLE_MISSING:"+table;
            c.close();c=report.select("SELECT COUNT(1) FROM "+table+" WHERE target_issue=?",new String[]{target});
            if(!c.moveToFirst()||c.getInt(0)!=1)return "NO_SAMPLE:ROW_MISSING:"+table+":issue="+target;
            return "AVAILABLE";
        }catch(Throwable e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
        finally{if(c!=null)c.close();}
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) { return value == null ? "" : value.trim(); }

public static PostRow readPostSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db,String target) {
        if(db==null||safeSnapshotCP4(report, target).length()==0)return null;Cursor c=null;try{
            c=report.select("SELECT status,attribution_summary,feedback_summary,predraw_record_hash,"
                            +"draw_hash,record_hash,actual_number,hit_label FROM "+POST_TABLE
                            +" WHERE target_issue=? LIMIT 1",new String[]{target});
            if(!c.moveToFirst())return null;
            return new PostRow(target,c.getString(0),c.getString(1),c.getString(2),c.getString(3),
                    c.getString(4),c.getString(5),c.getInt(6),c.getInt(7));
        }catch(Throwable ignored){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);}finally{if(c!=null)c.close();}
    }

public static Row readSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, SQLiteDatabase db, String target) {
        if (db == null || safeSnapshotCP4(report, target).length() == 0) return null;
        // Read paths are deliberately DDL-free.  The table is created by the canonical writer;
        // attempting CREATE TABLE from a UI/read transaction can upgrade its lock and briefly
        // block the per-minute settlement writer on older SQLite builds.
        Cursor c = null;
        try {
            c = report.select("SELECT teacher_summary,reasoning_summary,reverse_summary,"
                            + "causal_summary,feedback_contract,trend_summary,shape_summary,"
                            + "relation_summary,record_hash FROM " + TABLE
                            + " WHERE target_issue=? LIMIT 1", new String[] {target});
            if (!c.moveToFirst()) return null;
            return new Row(target, c.getString(0), c.getString(1), c.getString(2),
                    c.getString(3), c.getString(4), c.getString(5), c.getString(6),
                    c.getString(7), c.getString(8));
        } catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);}
        finally { if (c != null) c.close(); }
    }
}
