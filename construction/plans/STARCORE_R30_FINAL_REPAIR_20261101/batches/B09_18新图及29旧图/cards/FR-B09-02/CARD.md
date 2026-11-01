# FR-B09-02 有界图帧、张量切片与单快照读取

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-18。

前驱：B08。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchRepositoryR30R1.java`

动作：**ADD**。施工范围：完整新增类。

问题：同页多SELECT可能读不同提交，复杂图超120点会抛异常。

修改方式：单个SELECT读取本组六最新已提交frame；payload≤256KiB，点/边≤120；大矩阵按事前固定切片分页。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`NEW_FILE`。共享合并顺序：FR-B09-02。

### `app/src/main/java/com/ethk3/novaonepro/ResearchVisualDataR82.java`

动作：**MODIFY**。施工范围：Panel/Dashboard传输合同。

问题：已有MAX_POINTS=120、MAX_EDGES=120、Dashboard40不能随意超过。

修改方式：保持边界或新增明确分页结构，不暴力加大上限；完整机读分析与展示截取分开。

原方法锚点：ResearchVisualDataR82$Panel.<init>(String page,String title,String kind,String source,String window,String note,String status,List<Point> points,List<Edge> edges,List<Double> refs,List<String> series) L43–47；ResearchVisualDataR82$Panel.<init>(String page,String title,String kind,String source,String window,long ignoredN,String note,String status,List<Point> points,List<Edge> edges) L48–50；ResearchVisualDataR82$Panel.<init>(String page,String title,String kind,String source,String window,String note,String status,List<Point> points,List<Edge> edges) L51–55；ResearchVisualDataR82$Dashboard.<init>(String page,String status,String asOfIssue,String exactCandidate,String candidateKey,String dataVersion,String sourceVersion,String note,List<Panel> panels) L61–63；ResearchVisualDataR82$Dashboard.<init>(String page,String status,String asOfIssue,String exactCandidate,String candidateKey,String dataVersion,String sourceVersion,String note,String snapshotId,long generatedAt,List<Panel> panels) L64–69

基线SHA256：`61ca338a1b28f98aedc290e518958feff0b401c4526ed44aaa08b81350bec280`。共享合并顺序：FR-B09-02。

### `app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java`

动作：**MODIFY**。施工范围：POINTS_PER_PAGE_R30。

问题：静态分页常量500与传输120上限不一致，存在潜在超限风险。

修改方式：改用MAX_POINTS；是否实际触发另用运行证据判断。

原方法锚点：类/字段/新增接口范围；原完整方法索引见source_audit/methods.jsonl。

基线SHA256：`e96455d47638b094bb12bc2c833a198709dbd303ab5821d749273608936c72bb`。共享合并顺序：FR-B09-02。

## 实施步骤

1. R06预置帧读取实际配对贡献；其他帧必须由对应B08算法提交，不生成演示数据。
2. 图帧hash、run、dataset、role和输入水位明确；跨图窗口不同时禁止做同窗聚合结论。

## 测试与反证

- 错hash/超字节/超点数/超边数/坏JSON/缺表/取消/并发更新；一图坏不关闭全页。

## 验收

有界、可取消、来源一致且不误称已有效。

## 恢复与回滚

回退新reader，分析原件与旧图继续。

## 关键代码

顺序补丁：`patches/B09_research_graph_contracts.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

顺序补丁：`patches/B09_research_workbench_entry.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchRepositoryR30R1.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/ChartsRepositoryR27.java`（关键组合目标；集成限制见上）：

```java
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

    static List<Panel> load(SQLiteDatabase db, CancellationSignal cancel) {
        List<Panel> out = new ArrayList<>();
        Map<String,Object> frame=snapshotR30(db,cancel);
        out.add(frame==null?headline(db,cancel):headlineSnapshotR30(frame));
        for (String id : ChartCacheR27.PANELS) out.add(cached(db, cancel, id));
        for(int i=0;i<2;i++)out.add(snapshotPanelR30(frame,i));
        for (Panel p : ChartsRepositoryR83.load(db, cancel)) {
            if (p.title.startsWith("算法迭代进化事件") || p.title.startsWith("算法父子谱系") || p.title.startsWith("主链算法参与权重")) continue;
            if (p.title.startsWith("深度学习训练误差")) {
                out.add(new Panel(PAGE, "短期波动（仅参考）：滚动20期Brier", p.kind, p.source, p.window,
                        "纯噪声下滚动20期Brier即可在0.11—0.35摆动（docs/01 E）；不能用来判断“越学越准”。请看上方图A/图B。", p.status, p.points, p.edges));
                continue;
            }
            out.add(p);
        }
        return out;
    }

    static Panel headline(SQLiteDatabase db, CancellationSignal cancel) {
        String text = "当前合格策略：UNKNOWN；尚未取得资格计数，请等待真实快照。";
        try (Cursor c = db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='mainline' AND key='choice'", null, cancel)) {
            if (c.moveToFirst()) {
                Map<?, ?> m = (Map<?, ?>) JsonR27.parse(c.getString(0));
                long q = ((Number) m.get("qualified")).longValue();
                text = "当前合格策略：" + q + "个；主链：" + m.get("choice") + (q == 0 ? "；最接近合格：" + m.get("nearest") : "");
            }
        } catch (RuntimeException schemaPending) { text = "R27 数据尚未生成（升级后首次运行会自动建立）。"; }
        return new Panel(PAGE, "研究状态", "STATUS", "r27_chart_cache", "", text, "READY", null, null);
    }

    @SuppressWarnings("unchecked")
    static Panel cached(SQLiteDatabase db, CancellationSignal cancel, String id) {
        try (Cursor c = db.rawQuery("SELECT payload_json,updated_at FROM r27_chart_cache WHERE panel_id=? AND key='panel'", new String[] {id}, cancel)) {
            if (!c.moveToFirst()) return new Panel(PAGE, id, "STATUS", "r27_chart_cache", "", "等待研究线程生成本图（每30秒刷新）。", "INSUFFICIENT", null, null);
            Map<String, Object> m = (Map<String, Object>) JsonR27.parse(c.getString(0));
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
        try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id='r30-charts' AND key='snapshot'",null,cancel)){
            if(!c.moveToFirst())return null;Object parsed=JsonR27.parse(c.getString(0));if(!(parsed instanceof Map))throw new IllegalStateException("R30_CHART_PACKET_FORMAT");
            Map<String,Object> packet=(Map<String,Object>)parsed;if(!"R30_CHART_SNAPSHOT_1".equals(packet.get("version")))throw new IllegalStateException("R30_CHART_PACKET_VERSION");return packet;
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
        }catch(RuntimeException malformed){return new Panel(PAGE,id,"STATUS","r30-charts/snapshot","","快照图读取失败："+malformed.getClass().getSimpleName()+"；不按0处理。","READ_ERROR",null,null);}
    }
    /** R30-B02-05: one panel page holds at most this many points (the most recent ones); older points stay in the cache. */
    static final int POINTS_PER_PAGE_R30 = MAX_POINTS;
    static double d(Object o) { return o instanceof Number ? ((Number) o).doubleValue() : Double.NaN; }
}

```

`app/src/main/java/com/ethk3/novaonepro/ResearchWorkbenchRepositoryR30R1.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import java.util.*;
import com.ethk3.novaonepro.strategyv2.*;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/** A single SELECT captures the latest frame for all six views. This class is strictly read-only. */
final class ResearchWorkbenchRepositoryR30R1 {
    private ResearchWorkbenchRepositoryR30R1(){}
    static List<Panel> load(SQLiteDatabase db,int group,CancellationSignal cancel){
        List<ResearchGraphSpecsR30R1.Spec> specs=ResearchGraphSpecsR30R1.group(group);Map<String,Panel> found=new HashMap<>();
        String[] args=new String[specs.size()];StringBuilder marks=new StringBuilder();for(int i=0;i<args.length;i++){args[i]=specs.get(i).id;if(i>0)marks.append(',');marks.append('?');}
        String sql="SELECT c.view_id,c.payload_json,c.payload_hash,r.dataset_hash,r.role,r.through_ordinal,c.run_id FROM r30r1_chart_cache c JOIN r30r1_analysis_run r ON r.run_id=c.run_id WHERE c.view_id IN ("+marks+") AND c.frame_id=(SELECT n.frame_id FROM r30r1_chart_cache n WHERE n.view_id=c.view_id ORDER BY n.updated_at DESC,n.frame_id LIMIT 1) ORDER BY c.view_id LIMIT 6";
        String error=null;
        try(Cursor c=db.rawQuery(sql,args,cancel)){
            while(c.moveToNext())for(ResearchGraphSpecsR30R1.Spec spec:specs)if(spec.id.equals(c.getString(0))){
                try{String payload=c.getString(1);if(payload==null||payload.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>262144)throw new IllegalStateException("FRAME_BYTE_BUDGET");if(!ResearchContractR30R1.hash(payload).equals(c.getString(2)))throw new IllegalStateException("FRAME_HASH_MISMATCH");
                    @SuppressWarnings("unchecked") Map<String,Object> frame=(Map<String,Object>)JsonR27.parse(payload);
                    found.put(spec.id,panel(spec,frame,c.getString(3),c.getString(4),c.getLong(5),c.getString(6)));
                }catch(RuntimeException malformed){found.put(spec.id,missing(spec,"READ_ERROR",malformed.getClass().getSimpleName()+":"+String.valueOf(malformed.getMessage())));}
            }
        }catch(OperationCanceledException cancelled){throw cancelled;}catch(RuntimeException missing){error=missing.getClass().getSimpleName();}
        List<Panel> out=new ArrayList<>();for(ResearchGraphSpecsR30R1.Spec spec:specs)out.add(found.containsKey(spec.id)?found.get(spec.id):missing(spec,error==null?"WAITING_ANALYSIS":"BLOCKED_SOURCE",error==null?"尚无已提交分析产物；不能以统计行数代替分析":error));return out;
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
        boolean any=false;for(Point p:points)any|=Double.isFinite(p.value);if("GRAPH".equals(s.kind))any=!points.isEmpty();
        return new Panel("research-workbench",s.id+" "+s.title,s.kind,"r30r1_research_measure/"+s.measure,"输入截至ordinal="+through,note,any?"MEASURED_RESEARCH_ONLY":"INSUFFICIENT",points,edges,Collections.singletonList(0.0),series);
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
