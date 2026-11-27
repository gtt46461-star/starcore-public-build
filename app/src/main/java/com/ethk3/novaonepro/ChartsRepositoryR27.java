package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import com.ethk3.novaonepro.strategyv2.ChartCacheR27;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/** R27-050 chart page (docs/10 10.1): a top status line, the 14 R27 panels read from the pre-aggregated
 * r27_chart_cache (one indexed read each, cancellable), then the still-valid R83 panels. The R83 lineage and
 * evolution-event panels (stale R9xx tables) are removed; the R83 rolling-20 Brier is kept only as
 * "短期波动（仅参考）" at the end. */
final class ChartsRepositoryR27 {
    private ChartsRepositoryR27() {}
    static final String PAGE = "charts";
    /** R31-B09-C03: per-chart frame budget (characters of the stored JSON). */
    static final int MAX_PANEL_CHARS_R31 = 512 * 1024;

    /** R31-B09-C01: page order and membership come from ChartRegistryR31 stable ids; titles never filter or route. */
    static List<Panel> load(SQLiteDatabase db, CancellationSignal cancel) {
        Page page = new Page(db);
        List<Panel> panels = new ArrayList<>();
        for (String id : panelIdsR31()) panels.add(page.read(id, cancel));
        return panels;
    }

    static final class Page {
        private final SQLiteDatabase db;
        private boolean snapshotLoaded;
        private Map<String,Object> frame;
        private ChartsRepositoryR83.ReadContext history;

        Page(SQLiteDatabase db) { this.db = db; }

        private Map<String,Object> snapshot(CancellationSignal cancel) {
            if (!snapshotLoaded) {
                frame = snapshotR30(db, cancel);
                snapshotLoaded = true;
            }
            return frame;
        }

        Panel read(String id, CancellationSignal cancel) {
            ChartRegistryR31.Entry entry = ChartRegistryR31.find(id);
            switch (entry.group) {
                case LEGACY_STATUS:
                    Map<String,Object> current = snapshot(cancel);
                    return current == null ? headline(db, cancel) : headlineSnapshotR30(current);
                case LEGACY_CACHED:
                    return cached(db, cancel, id);
                case LEGACY_SNAPSHOT:
                    int index = java.util.Arrays.asList(com.ethk3.novaonepro.strategyv2.ReportChartsR30.IDS).indexOf(id);
                    return snapshotPanelR30(snapshot(cancel), index);
                case LEGACY_R83:
                    if (history == null) history = new ChartsRepositoryR83.ReadContext(db, cancel);
                    Panel chart = ChartsRepositoryR83.loadPanel(id, db, cancel, history);
                    if (!id.equals("r83_brier20")) return chart;
                    return new Panel(PAGE, "短期波动（仅参考）：滚动20期Brier", chart.kind, chart.source, chart.window,
                            "纯噪声下滚动20期Brier即可在0.11—0.35摆动（docs/01 E）；不能用来判断“越学越准”。请看上方图A/图B。",
                            chart.status, chart.points, chart.edges);
                default:
                    throw new IllegalStateException("R31_CHART_GROUP_NOT_ON_LEGACY_PAGE:" + id);
            }
        }
    }

    /** The ids this page shows, in order (the same set the report index and machine tables use). */
    static List<String> panelIdsR31() { return ChartRegistryR31.visibleLegacyIds(); }

    static Panel headline(SQLiteDatabase db, CancellationSignal cancel) {
        String status="INSUFFICIENT";String text = "当前合格策略：UNKNOWN；尚未取得资格计数，请等待真实快照。";
        try (Cursor c = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'", null, cancel)) {
            if (c.moveToFirst()) {
                Map<?, ?> m = (Map<?, ?>) JsonR27.parse(c.getString(0));
                long q = ((Number) m.get("qualified")).longValue();status="READY";
                text = "当前合格策略：" + q + "个；主链：" + m.get("choice") + (q == 0 ? "；最接近合格：" + m.get("nearest") : "");
            }
        } catch (android.os.OperationCanceledException cancelled) { throw cancelled; } catch (RuntimeException schemaPending) { status="READ_ERROR";text = "资格原件读取失败："+schemaPending.getClass().getSimpleName()+"；不能按0或健康处理。"; }
        return new Panel(PAGE, "研究状态", "STATUS", "r27_chart_cache", "", text, status, null, null);
    }

    @SuppressWarnings("unchecked")
    static Panel cached(SQLiteDatabase db, CancellationSignal cancel, String id) {
        try (Cursor c = db.rawQuery("SELECT payload_json,updated_at FROM r27_chart_cache WHERE panel_id=? AND key='panel'", new String[] {id}, cancel)) {
            if (!c.moveToFirst()) return new Panel(PAGE, id, "STATUS", "r27_chart_cache", "", "等待研究线程生成本图（每30秒刷新）。", "INSUFFICIENT", null, null);
            // R31-B09-C03: validate the frame before expanding it; a too large, unparsable or non-object frame fails only this chart.
            String raw = c.getString(0);
            if (raw == null || raw.length() > MAX_PANEL_CHARS_R31 || raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MAX_PANEL_CHARS_R31) return new Panel(PAGE, id, "STATUS", "r27_chart_cache", "", "本图缓存帧超过显示预算，未展开（不影响其他图）。", "STALE_FRAME_TOO_LARGE", null, null);
            Object parsed; try { parsed = JsonR27.parse(raw); } catch (RuntimeException bad) { return new Panel(PAGE, id, "STATUS", "r27_chart_cache", "", "本图缓存帧损坏：" + bad.getClass().getSimpleName() + "（不影响其他图）。", "STALE_FRAME_INVALID", null, null); }
            if (!(parsed instanceof Map) || !(((Map<?, ?>) parsed).get("points") instanceof List)) return new Panel(PAGE, id, "STATUS", "r27_chart_cache", "", "本图缓存帧版本不符，等待重建。", "STALE_FRAME_ABI", null, null);
            Map<String, Object> m = (Map<String, Object>) parsed;
            String latest=null;try(Cursor issue=db.rawQuery("SELECT max(issue) FROM draws WHERE dirty=0",null,cancel)){if(issue.moveToFirst())latest=issue.getString(0);}
            String invalid=ChartCacheR27.validateForDisplayR31(m,latest,c.getLong(1),System.currentTimeMillis());
            boolean historical=verifiedHistory(invalid,m,latest);
            if(invalid!=null&&!historical)return new Panel(PAGE,id,"STATUS","r27_chart_cache","",invalid+"；等待真实源帧重新生成。",invalid,null,null);
            List<Point> pts = new ArrayList<>();
            for (Object o : (List<Object>) m.get("points")) {
                Map<String, Object> p = (Map<String, Object>) o; double[] stack = null;
                if (p.get("stack") instanceof List) { List<Object> l = (List<Object>) p.get("stack"); stack = new double[l.size()]; for (int i = 0; i < stack.length; i++) stack[i] = d(l.get(i)); }
                pts.add(new Point((String) p.get("label"), d(p.get("value")), d(p.get("lower")), d(p.get("upper")), ((Number) p.get("n")).longValue(),
                        (String) p.get("details") + ("RED".equals(p.get("flag")) ? "\n⚠ 过拟合" : ""), d(p.get("open")), d(p.get("expected")), stack));
            }
            List<Edge> edges = new ArrayList<>();
            if (m.get("edges") instanceof List) for (Object o : (List<Object>) m.get("edges")) { Map<String, Object> e = (Map<String, Object>) o; edges.add(new Edge((String) e.get("from"), (String) e.get("to"), (String) e.get("label"))); }
            List<Double> refs = new ArrayList<>(); if (m.get("refs") instanceof List) for (Object o : (List<Object>) m.get("refs")) refs.add(d(o));
            List<String> series = new ArrayList<>(); if (m.get("series") instanceof List) for (Object o : (List<Object>) m.get("series")) series.add(String.valueOf(o));
            String status = String.valueOf(m.get("status"));
            String window = m.get("window") + "｜更新于" + new java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.ROOT).format(new java.util.Date(c.getLong(1)));
            String note = m.get("note") + "\n数据来源：" + m.get("source");
            if(historical) {
                status="HISTORICAL_FRAME";
                note+="\n历史实测，数据截至 "+m.get("sourceIssue")+" 期，来源版本 "+m.get("buildVersionCode")+"。";
            }
            if (pts.size() > POINTS_PER_PAGE_R30) { // R30-B02-05 paging
                int total = pts.size();
                pts = new ArrayList<>(pts.subList(total - POINTS_PER_PAGE_R30, total));
                note = note + "\n本页显示最近 " + POINTS_PER_PAGE_R30 + " 点（共 " + total + " 点）。";
            }
            return new Panel(PAGE, String.valueOf(m.get("title")), String.valueOf(m.get("kind")), String.valueOf(m.get("source")), window, note, status, pts, edges, refs, series);
        } catch (RuntimeException schemaPending) {
            if (schemaPending instanceof android.os.OperationCanceledException) throw schemaPending;
            return new Panel(PAGE, id, "STATUS", "r27_chart_cache", "", "读取失败：" + schemaPending.getClass().getSimpleName() + "；不按0处理。", "READ_ERROR", null, null);
        }
    }
    private static boolean verifiedHistory(String status, Map<String,Object> frame, String latest) {
        if (!"HISTORICAL_BUILD_FRAME".equals(status) && !"STALE_SOURCE_WATERMARK".equals(status)) {
            return false;
        }
        String issue = String.valueOf(frame.get("sourceIssue"));
        return TargetIssueResolver.isValid(issue) && (latest == null || issue.compareTo(latest) <= 0);
    }

    private static final class HistoricalSnapshot extends java.util.LinkedHashMap<String,Object> {
        HistoricalSnapshot(Map<String,Object> verified) { super(verified); }
    }

    private static Map<String,Object> snapshotError(String cause) {
        return java.util.Collections.<String,Object>singletonMap("readError", cause);
    }

    @SuppressWarnings("unchecked") static Map<String,Object> snapshotR30(SQLiteDatabase db,CancellationSignal cancel){
        try(Cursor c=db.rawQuery("SELECT payload_json,updated_at FROM r27_chart_cache WHERE panel_id='r30-charts' AND key='snapshot'",null,cancel)){
            if (!c.moveToFirst()) return null;
            String raw=c.getString(0);
            if (raw==null || raw.length()>2*1024*1024 || raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>2*1024*1024)
                return snapshotError("R31_CHART_PACKET_BYTE_BOUND");
            Object parsed=JsonR27.parse(raw);
            if (!(parsed instanceof Map)) return snapshotError("R30_CHART_PACKET_FORMAT");
            Map<String,Object> packet=(Map<String,Object>)parsed;
            String latest=null;
            try(Cursor issue=db.rawQuery("SELECT max(issue) FROM draws WHERE dirty=0",null,cancel)){
                if (issue.moveToFirst()) latest=issue.getString(0);
            }
            String invalid=ChartCacheR27.validateForDisplayR31(packet,latest,c.getLong(1),System.currentTimeMillis());
            if (!"R30_CHART_SNAPSHOT_1".equals(packet.get("version"))) return snapshotError("R30_CHART_PACKET_VERSION");
            if (invalid==null) return packet;
            if (verifiedHistory(invalid,packet,latest)) return new HistoricalSnapshot(packet);
            return snapshotError(invalid);
        }catch(android.os.OperationCanceledException cancelled){throw cancelled;}
        catch(RuntimeException malformed){return snapshotError("READ_ERROR");}
    }

    static Panel headlineSnapshotR30(Map<String,Object> frame){
        if (frame.containsKey("readError")) return unreadableSnapshot("研究状态（同报告快照）",frame);
        Object raw=frame.get("commonMetrics"),mark=frame.get("snapshotWatermark");
        Object q=raw instanceof Map?((Map<?,?>)raw).get("qualifiedExperiments"):null;
        boolean historical=frame instanceof HistoricalSnapshot;
        String text=historical?"截至 "+frame.get("sourceIssue")+" 期登记的合格策略数："+(q instanceof Number?q:"未知")+"。当前资格待刷新。"
                :"当前合格策略："+(q instanceof Number?q:"UNKNOWN")+"；来源为本图报告快照；有记录不代表自然质量达标。";
        if(!historical && mark instanceof Map)text+=" 期号 "+((Map<?,?>)mark).get("latestIssue");
        String status=historical?"HISTORICAL_FRAME":q instanceof Number?"RECORDED_UNVALIDATED":"UNKNOWN";
        return new Panel(PAGE,"研究状态（同报告快照）","STATUS","r30-charts/snapshot","",text,status,null,null);
    }

    private static Panel unreadableSnapshot(String title,Map<String,Object> frame){
        String cause=String.valueOf(frame.get("readError"));
        return new Panel(PAGE,title,"STATUS","r30-charts/snapshot","","快照读取失败："+cause,cause,null,null);
    }
    @SuppressWarnings("unchecked") static Panel snapshotPanelR30(Map<String,Object> frame,int index){
        String id=com.ethk3.novaonepro.strategyv2.ReportChartsR30.IDS[index];
        if(frame==null)return new Panel(PAGE,id,"STATUS","r30-charts/snapshot","","等待原图表任务生成与报告同源的快照；缺数据不补零。","INSUFFICIENT",null,null);
        if(frame.containsKey("readError"))return unreadableSnapshot(id,frame);
        try{
            Object raw=frame.get("panels");if(!(raw instanceof List)||((List<?>)raw).size()!=2)throw new IllegalStateException("R30_CHART_PANEL_COUNT");
            Map<String,Object> panel=(Map<String,Object>)((List<?>)raw).get(index);if(!id.equals(panel.get("id"))||!Objects.equals(frame.get("snapshotWatermark"),panel.get("snapshotWatermark"))||!Objects.equals(frame.get("commonMetrics"),panel.get("commonMetrics")))throw new IllegalStateException("R30_CHART_SNAPSHOT_MISMATCH");
            List<Point> points=new ArrayList<>();Object rows=panel.get("points");if(!(rows instanceof List))throw new IllegalStateException("R30_CHART_POINTS");
            for(Object item:(List<?>)rows){Map<?,?> p=(Map<?,?>)item;double[] stack=null;Object components=p.get("stack");if(components instanceof List){stack=new double[((List<?>)components).size()];for(int i=0;i<stack.length;i++)stack[i]=d(((List<?>)components).get(i));}
                points.add(new Point(String.valueOf(p.get("label")),d(p.get("value")),d(p.get("lower")),d(p.get("upper")),p.get("n") instanceof Number?((Number)p.get("n")).longValue():-1,String.valueOf(p.get("details")),d(p.get("open")),d(p.get("expected")),stack));}
            List<Double> refs=new ArrayList<>();if(panel.get("refs") instanceof List)for(Object v:(List<?>)panel.get("refs"))refs.add(d(v));List<String> series=new ArrayList<>();if(panel.get("series") instanceof List)for(Object v:(List<?>)panel.get("series"))series.add(String.valueOf(v));
            String window=String.valueOf(panel.get("window"));Object mark=frame.get("snapshotWatermark");if(mark instanceof Map)window+="｜快照 "+((Map<?,?>)mark).get("factsSha256");
            String note=panel.get("note")+"\n"+panel.get("collectionAction");
            String status=String.valueOf(panel.get("status"));
            if(frame instanceof HistoricalSnapshot){
                window+="｜截至 "+frame.get("sourceIssue")+" 期";
                note+="\n历史实测。该期采集状态："+status+"。";
                status="HISTORICAL_FRAME";
            }
            return new Panel(PAGE,String.valueOf(panel.get("title")),String.valueOf(panel.get("kind")),String.valueOf(panel.get("source")),window,note,status,points,null,refs,series);
        }catch(android.os.OperationCanceledException cancelled){throw cancelled;}catch(RuntimeException malformed){return new Panel(PAGE,id,"STATUS","r30-charts/snapshot","","快照图读取失败："+malformed.getClass().getSimpleName()+"；不按0处理。","READ_ERROR",null,null);}
    }
    /** R30-B02-05: one panel page holds at most this many points (the most recent ones); older points stay in the cache. */
    static final int POINTS_PER_PAGE_R30 = 500;
    static double d(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : Double.NaN; }
}
