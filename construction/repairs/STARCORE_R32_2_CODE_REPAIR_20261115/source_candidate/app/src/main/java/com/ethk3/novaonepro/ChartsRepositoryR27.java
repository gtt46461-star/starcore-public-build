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
        List<Panel> out = new ArrayList<>();
        Map<String,Object> frame=snapshotR30(db,cancel);
        ChartsRepositoryR83.ReadContext r83=null;
        for (String id : ChartRegistryR31.visibleLegacyIds()) {
            ChartRegistryR31.Entry e = ChartRegistryR31.find(id);
            switch (e.group) {
                case LEGACY_STATUS: out.add(frame==null?headline(db,cancel):headlineSnapshotR30(frame)); break;
                case LEGACY_CACHED: out.add(cached(db, cancel, id)); break;
                case LEGACY_SNAPSHOT: out.add(snapshotPanelR30(frame, java.util.Arrays.asList(com.ethk3.novaonepro.strategyv2.ReportChartsR30.IDS).indexOf(id))); break;
                case LEGACY_R83: {
                    if (r83 == null) r83 = new ChartsRepositoryR83.ReadContext(db, cancel);
                    Panel p = ChartsRepositoryR83.loadPanel(id, db, cancel, r83);
                    if (id.equals("r83_brier20")) p = new Panel(PAGE, "短期波动（仅参考）：滚动20期Brier", p.kind, p.source, p.window,
                            "纯噪声下滚动20期Brier即可在0.11—0.35摆动（docs/01 E）；不能用来判断“越学越准”。请看上方图A/图B。", p.status, p.points, p.edges);
                    out.add(p); break;
                }
                default: throw new IllegalStateException("R31_CHART_GROUP_NOT_ON_LEGACY_PAGE:" + id);
            }
        }
        return out;
    }
    /** The ids this page shows, in order (the same set the report index and machine tables use). */
    static List<String> panelIdsR31() { return ChartRegistryR31.visibleLegacyIds(); }

    static Panel headline(SQLiteDatabase db, CancellationSignal cancel) {
        String text = "当前合格策略：UNKNOWN；尚未取得资格计数，请等待真实快照。";
        try (Cursor c = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'", null, cancel)) {
            if (c.moveToFirst()) {
                Map<?, ?> m = (Map<?, ?>) JsonR27.parse(c.getString(0));
                long q = ((Number) m.get("qualified")).longValue();
                text = "当前合格策略：" + q + "个；主链：" + m.get("choice") + (q == 0 ? "；最接近合格：" + m.get("nearest") : "");
            }
        } catch (android.os.OperationCanceledException cancelled) { throw cancelled; } catch (RuntimeException schemaPending) { text = "R27 数据尚未生成（升级后首次运行会自动建立）。"; }
        return new Panel(PAGE, "研究状态", "STATUS", "r27_chart_cache", "", text, "READY", null, null);
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
            String invalid=com.ethk3.novaonepro.strategyv2.ChartCacheR27.validateR31(m,latest,c.getLong(1),System.currentTimeMillis());
            if(invalid!=null)return new Panel(PAGE,id,"STATUS","r27_chart_cache","",invalid+"；等待真实源帧重新生成。",invalid,null,null);
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
    @SuppressWarnings("unchecked") static Map<String,Object> snapshotR30(SQLiteDatabase db,CancellationSignal cancel){
        try(Cursor c=db.rawQuery("SELECT payload_json,updated_at FROM r27_chart_cache WHERE panel_id='r30-charts' AND key='snapshot'",null,cancel)){
            if(!c.moveToFirst())return null;String raw=c.getString(0);if(raw==null||raw.length()>2*1024*1024||raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>2*1024*1024)throw new IllegalStateException("R31_CHART_PACKET_BYTE_BOUND");Object parsed=JsonR27.parse(raw);if(!(parsed instanceof Map))throw new IllegalStateException("R30_CHART_PACKET_FORMAT");
            Map<String,Object> packet=(Map<String,Object>)parsed;String latest=null;try(Cursor issue=db.rawQuery("SELECT max(issue) FROM draws WHERE dirty=0",null,cancel)){if(issue.moveToFirst())latest=issue.getString(0);}String invalid=com.ethk3.novaonepro.strategyv2.ChartCacheR27.validateR31(packet,latest,c.getLong(1),System.currentTimeMillis());if(invalid!=null)throw new IllegalStateException(invalid);if(!"R30_CHART_SNAPSHOT_1".equals(packet.get("version")))throw new IllegalStateException("R30_CHART_PACKET_VERSION");return packet;
        }catch(android.os.OperationCanceledException cancelled){throw cancelled;}catch(RuntimeException malformed){Map<String,Object> failed=new java.util.LinkedHashMap<>();failed.put("readError",malformed.getClass().getSimpleName());return failed;}
    }
    static Panel headlineSnapshotR30(Map<String,Object> frame){
        Object raw=frame.get("commonMetrics"),mark=frame.get("snapshotWatermark");Object q=raw instanceof Map?((Map<?,?>)raw).get("qualifiedExperiments"):null;
        String text="当前合格策略："+(q instanceof Number?q:"UNKNOWN")+"；来源为本图报告快照；有记录不代表自然质量达标。";
        if(mark instanceof Map)text+=" 期号 "+((Map<?,?>)mark).get("latestIssue");
        return new Panel(PAGE,"研究状态（同报告快照）","STATUS","r30-charts/snapshot","",text,q instanceof Number?"RECORDED_UNVALIDATED":"UNKNOWN",null,null);
    }
    @SuppressWarnings("unchecked") static Panel snapshotPanelR30(Map<String,Object> frame,int index){
        String id=com.ethk3.novaonepro.strategyv2.ReportChartsR30.IDS[index];
        if(frame==null)return new Panel(PAGE,id,"STATUS","r30-charts/snapshot","","等待原图表任务生成与报告同源的快照；缺数据不补零。","INSUFFICIENT",null,null);
        try{
            Object raw=frame.get("panels");if(!(raw instanceof List)||((List<?>)raw).size()!=2)throw new IllegalStateException("R30_CHART_PANEL_COUNT");
            Map<String,Object> panel=(Map<String,Object>)((List<?>)raw).get(index);if(!id.equals(panel.get("id"))||!Objects.equals(frame.get("snapshotWatermark"),panel.get("snapshotWatermark"))||!Objects.equals(frame.get("commonMetrics"),panel.get("commonMetrics")))throw new IllegalStateException("R30_CHART_SNAPSHOT_MISMATCH");
            List<Point> points=new ArrayList<>();Object rows=panel.get("points");if(!(rows instanceof List))throw new IllegalStateException("R30_CHART_POINTS");
            for(Object item:(List<?>)rows){Map<?,?> p=(Map<?,?>)item;double[] stack=null;Object components=p.get("stack");if(components instanceof List){stack=new double[((List<?>)components).size()];for(int i=0;i<stack.length;i++)stack[i]=d(((List<?>)components).get(i));}
                points.add(new Point(String.valueOf(p.get("label")),d(p.get("value")),d(p.get("lower")),d(p.get("upper")),p.get("n") instanceof Number?((Number)p.get("n")).longValue():-1,String.valueOf(p.get("details")),d(p.get("open")),d(p.get("expected")),stack));}
            List<Double> refs=new ArrayList<>();if(panel.get("refs") instanceof List)for(Object v:(List<?>)panel.get("refs"))refs.add(d(v));List<String> series=new ArrayList<>();if(panel.get("series") instanceof List)for(Object v:(List<?>)panel.get("series"))series.add(String.valueOf(v));
            String window=String.valueOf(panel.get("window"));Object mark=frame.get("snapshotWatermark");if(mark instanceof Map)window+="｜快照 "+((Map<?,?>)mark).get("factsSha256");
            String note=panel.get("note")+"\n"+panel.get("collectionAction");
            return new Panel(PAGE,String.valueOf(panel.get("title")),String.valueOf(panel.get("kind")),String.valueOf(panel.get("source")),window,note,String.valueOf(panel.get("status")),points,null,refs,series);
        }catch(android.os.OperationCanceledException cancelled){throw cancelled;}catch(RuntimeException malformed){return new Panel(PAGE,id,"STATUS","r30-charts/snapshot","","快照图读取失败："+malformed.getClass().getSimpleName()+"；不按0处理。","READ_ERROR",null,null);}
    }
    /** R30-B02-05: one panel page holds at most this many points (the most recent ones); older points stay in the cache. */
    static final int POINTS_PER_PAGE_R30 = 500;
    static double d(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : Double.NaN; }
}
