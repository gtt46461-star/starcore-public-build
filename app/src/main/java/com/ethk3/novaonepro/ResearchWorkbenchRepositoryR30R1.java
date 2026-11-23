package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import java.util.*;
import com.ethk3.novaonepro.strategyv2.*;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/** One small read pins the six frame identities; immutable bodies are verified independently. */
final class ResearchWorkbenchRepositoryR30R1 {
    private ResearchWorkbenchRepositoryR30R1(){}

    static List<Panel> load(SQLiteDatabase db, int group, CancellationSignal cancel) {
        Page page = new Page(db, group);
        List<Panel> panels = new ArrayList<>();
        for (ResearchGraphSpecsR30R1.Spec spec : page.specs) panels.add(page.read(spec, cancel));
        return panels;
    }

    static final class Page {
        private final SQLiteDatabase db;
        final List<ResearchGraphSpecsR30R1.Spec> specs;
        private Map<String,String> frameIds;
        private String waiting;

        Page(SQLiteDatabase db, int group) {
            this.db = db;
            specs = ResearchGraphSpecsR30R1.group(group);
        }

        private Map<String,String> pin(CancellationSignal cancel) {
            String[] args = new String[specs.size() + 1];
            for (int i = 0; i < specs.size(); i++) args[i] = specs.get(i).id;
            args[specs.size()] = BuildConfig.SOURCE_COMMIT_R26;
            String values = String.join(",", Collections.nCopies(specs.size(), "(?)"));
            String sql = "WITH wanted(view_id) AS (VALUES " + values + ") SELECT w.view_id,("
                    + "SELECT c.frame_id FROM r30r1_chart_cache c JOIN r30r1_analysis_run r ON r.run_id=c.run_id "
                    + "WHERE c.view_id=w.view_id AND r.status='COMMITTED' ORDER BY (r.source_commit=?) DESC,"
                    + "c.updated_at DESC,c.frame_id LIMIT 1) FROM wanted w";
            Map<String,String> selected = new HashMap<>();
            try (Cursor c = db.rawQuery(sql, args, cancel)) {
                while (c.moveToNext()) selected.put(c.getString(0), c.getString(1));
            }
            return selected;
        }

        Panel read(ResearchGraphSpecsR30R1.Spec spec, CancellationSignal cancel) {
            try {
                if (frameIds == null) frameIds = pin(cancel);
                String frame = frameIds.get(spec.id);
                if (frame == null) {
                    String reason = viewSourceError(db, spec.id, cancel);
                    if (reason != null) return missing(spec, "BLOCKED_SOURCE", reason);
                    if (waiting == null) waiting = producerStatus(db, cancel);
                    return missing(spec, "WAITING_ANALYSIS", waiting);
                }
                return readFrame(db, spec, frame, cancel);
            } catch (OperationCanceledException stopped) {
                throw stopped;
            } catch (RuntimeException failure) {
                return missing(spec, "READ_ERROR", failure.getClass().getSimpleName() + ":" + failure.getMessage());
            }
        }
    }

    private static Panel readFrame(SQLiteDatabase db, ResearchGraphSpecsR30R1.Spec spec,
            String frameId, CancellationSignal cancel) {
        String sql = "SELECT c.view_id,c.payload_json,c.payload_hash,r.dataset_hash,r.role,r.through_ordinal,"
                + "c.run_id,r.contract_hash,r.source_commit FROM r30r1_chart_cache c "
                + "JOIN r30r1_analysis_run r ON r.run_id=c.run_id WHERE c.view_id=? AND c.frame_id=? AND r.status='COMMITTED'";
        try (Cursor c = db.rawQuery(sql, new String[]{spec.id, frameId}, cancel)) {
            if (!c.moveToFirst()) return missing(spec, "READ_ERROR", "PINNED_FRAME_UNAVAILABLE");
            if (cancel != null) cancel.throwIfCanceled();
            String payload = c.getString(1);
            if (payload == null || payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 262144)
                throw new IllegalStateException("FRAME_BYTE_BUDGET");
            if (!ResearchContractR30R1.hash(payload).equals(c.getString(2)))
                throw new IllegalStateException("FRAME_HASH_MISMATCH");
            @SuppressWarnings("unchecked")
            Map<String,Object> frame = (Map<String,Object>) JsonR27.parse(payload);
            String producer = c.getString(8);
            ResearchOriginalR335 original = ResearchOriginalR335.readForDisplay(db, c.getString(6), producer, cancel);
            Map<?,?> source = original.fields;
            if ("R06".equals(spec.id)) {
                for (String key : Arrays.asList("sourceContract", "mean", "lower95", "upper95", "componentPath",
                        "counterfactualHash", "status", "evaluatedIssues")) {
                    if (!JsonR27.write(source.get(key)).equals(JsonR27.write(frame.get(key))))
                        throw new IllegalStateException("R06_ORIGINAL_PROJECTION_CHANGED:" + key);
                }
            } else if (!c.getString(7).equals(original.frameHash) || !spec.id.equals(source.get("viewId"))
                    || !matchesOriginalFrame(c.getString(7), frame)) {
                throw new IllegalStateException("RESEARCH_CACHE_NOT_BOUND_TO_ORIGINAL");
            }
            Panel panel = panel(spec, frame, c.getString(3), c.getString(4), c.getLong(5), c.getString(6));
            if (!BuildConfig.SOURCE_COMMIT_R26.equals(producer)) {
                Panel previous = new Panel(panel.page, panel.title, panel.kind, panel.source, panel.window,
                        "历史帧 " + producer + "。本版本暂无已提交帧。\n" + panel.note, "HISTORICAL_ABI",
                        panel.points, panel.edges, panel.refs, panel.series);
                panel = new Panel(previous, panel.matrixRows, panel.matrixColumns);
            }
            return panel;
        }
    }

    private static String viewSourceError(SQLiteDatabase db,String view,CancellationSignal cancel) {
        try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='r335-research-scheduler' AND key=?",
                new String[]{"SOURCE_ERROR_"+view},cancel)){
            if(c.moveToFirst()){
                Map<?,?> row=(Map<?,?>)JsonR27.parse(c.getString(0));
                if(BuildConfig.SOURCE_COMMIT_R26.equals(row.get("source")) && view.equals(row.get("viewId")))
                    return String.valueOf(row.get("reason"));
            }
        }catch(OperationCanceledException e){throw e;}catch(RuntimeException e){return "图表调度台账读取失败 "+e.getMessage();}
        return null;
    }
    private static String producerStatus(SQLiteDatabase db,CancellationSignal cancel) {
        StringBuilder reason=new StringBuilder("本版本尚无这项分析的已提交产物。");
        try(Cursor tasks=db.rawQuery("SELECT task_id,status,blocked_reason,cursor,last_progress FROM r26_task "
                +"WHERE task_id IN ('r27-replay-dataset','r27-observatory-r1','r27-evolution') ORDER BY task_id",null,cancel)){
            while(tasks.moveToNext()) reason.append("\n").append(tasks.getString(0)).append(" ").append(tasks.getString(1))
                    .append(" ").append(tasks.isNull(2)?"":tasks.getString(2)).append(" 游标=").append(tasks.getLong(3))
                    .append(" 最近进展=").append(tasks.getLong(4));
        }catch(OperationCanceledException e){throw e;}catch(android.database.sqlite.SQLiteException e){reason.append(" 任务台账读取失败 ").append(e.getMessage());}
        return reason.toString();
    }
    static boolean matchesOriginalFrame(String originalHash, Map<?, ?> cache) {
        if (originalHash == null || !originalHash.matches("[0-9a-f]{64}")) return false;
        // The bounded chart includes the digest of the complete, separately stored machine frame.
        Object displayHash = cache.get("fullMachineFrameSha256");
        Object historicalHash = cache.get("originalMachineFrameSha256");
        if (cache.containsKey("fullMachineFrameSha256") && !originalHash.equals(displayHash)) return false;
        if (cache.containsKey("originalMachineFrameSha256") && !originalHash.equals(historicalHash)) return false;
        return originalHash.equals(displayHash) || originalHash.equals(historicalHash);
    }
    static Panel missing(ResearchGraphSpecsR30R1.Spec s,String status,String reason){return new Panel("research-workbench",s.id+" "+s.title,"STATUS","r30r1_chart_cache/"+s.measure,"",reason+"\n消费者："+s.consumer+"\n验收："+s.verification,status,null,null);}
    static double number(Object x){return x instanceof Number&&Double.isFinite(((Number)x).doubleValue())?((Number)x).doubleValue():Double.NaN;}
    @SuppressWarnings("unchecked") static Panel panel(ResearchGraphSpecsR30R1.Spec s,Map<String,Object> f,String dataset,String role,long through,String run){
        List<Point> points=new ArrayList<>();List<Edge> edges=new ArrayList<>();
        if("R06".equals(s.id)&&f.containsKey("mean")){
            long n=f.get("evaluatedIssues") instanceof Number?((Number)f.get("evaluatedIssues")).longValue():-1;
            points.add(new Point(String.valueOf(f.get("componentPath")),number(f.get("mean")),number(f.get("lower95")),number(f.get("upper95")),n,"研究配对贡献；"+f.get("reason")+"；proposal="+f.get("proposalId")));
        }else{
            Object raw=f.get("points");if(!(raw instanceof List))return missing(s,"INSUFFICIENT_ANALYSIS_FRAME","缺少points；生产者须输出对象级分析结果");
            if(((List<?>)raw).size()>MAX_POINTS)throw new IllegalArgumentException("RESEARCH_FRAME_REQUIRES_PAGINATION");
            for(Object row:(List<?>)raw){Map<String,Object> p=(Map<String,Object>)row;long n=p.get("n") instanceof Number?((Number)p.get("n")).longValue():-1;
                double[] stack=null;if(p.get("stack") instanceof List){List<?> v=(List<?>)p.get("stack");if(v.size()>12)throw new IllegalArgumentException("RESEARCH_SERIES_LIMIT");stack=new double[v.size()];for(int i=0;i<stack.length;i++)stack[i]=number(v.get(i));}
                points.add(new Point(String.valueOf(p.get("label")),number(p.get("value")),number(p.get("lower")),number(p.get("upper")),n,String.valueOf(p.get("details")),number(p.get("open")),number(p.get("expected")),stack));
            }
            if(f.get("edges") instanceof List){List<?> rows=(List<?>)f.get("edges");if(rows.size()>MAX_EDGES)throw new IllegalArgumentException("RESEARCH_EDGE_PAGINATION");for(Object row:rows){Map<?,?> e=(Map<?,?>)row;edges.add(new Edge(String.valueOf(e.get("from")),String.valueOf(e.get("to")),String.valueOf(e.get("label"))));}}
        }
        List<String> series=new ArrayList<>();if(f.get("series") instanceof List)for(Object x:(List<?>)f.get("series"))series.add(String.valueOf(x));
        String note="用途："+s.measure+"\n算法消费者："+s.consumer+"\n实效验收："+s.verification+"\n角色="+role+"；run="+run+"；dataset="+dataset+"。研究图不授予REAL资格。";
        boolean any=false;for(Point p:points){any|=Double.isFinite(p.value);if(p.stack!=null)for(double v:p.stack)any|=Double.isFinite(v);}if("GRAPH".equals(s.kind)&&!points.isEmpty())note+="\n图节点存在仅代表来源关系；不代表数值有效或算法改善。";
        if(f.get("modelReplayPerformance")!=null)note+="\n模型前后版本配对："+JsonR27.write(f.get("modelReplayPerformance"));String status=f.get("status") instanceof String?(String)f.get("status"):any?"MEASURED_RESEARCH_ONLY":"INSUFFICIENT";if(!any&&!Boolean.TRUE.equals(f.get("formalReadOnly"))&&!"BLOCKED_DEPENDENCY".equals(status))status="INSUFFICIENT";note+="\n当前产物："+status+(f.get("reason")==null?"":"；"+f.get("reason"))+"\n分析角色不是质量证书；负值和区间跨零如实保留。";String kind=s.kind;if(kind.equals("HEATMAP")&&(!(f.get("matrixRows") instanceof List)||!(f.get("matrixColumns") instanceof List)))kind="BAR";
        Panel panel=new Panel("research-workbench",s.id+" "+s.title,kind,"r30r1_research_measure/"+s.measure,"输入截至ordinal="+through,note,status,points,edges,Collections.singletonList(0.0),series);if(f.get("matrixRows") instanceof List&&f.get("matrixColumns") instanceof List){List<String> rows=new ArrayList<>(),cols=new ArrayList<>();for(Object x:(List<?>)f.get("matrixRows"))rows.add(String.valueOf(x));for(Object x:(List<?>)f.get("matrixColumns"))cols.add(String.valueOf(x));return new Panel(panel,rows,cols);}return panel;
    }
}
