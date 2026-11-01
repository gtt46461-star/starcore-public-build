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
        boolean any=false;for(Point p:points){any|=Double.isFinite(p.value);if(p.stack!=null)for(double v:p.stack)any|=Double.isFinite(v);}if("GRAPH".equals(s.kind))any=!points.isEmpty();
        String status=f.get("status") instanceof String?(String)f.get("status"):any?"MEASURED_RESEARCH_ONLY":"INSUFFICIENT";note+="\n当前产物："+status+(f.get("reason")==null?"":"；"+f.get("reason"))+"\n分析角色不是质量证书；负值和区间跨零如实保留。";String kind=s.kind;if(kind.equals("HEATMAP")&&(!(f.get("matrixRows") instanceof List)||!(f.get("matrixColumns") instanceof List)))kind="BAR";
        Panel panel=new Panel("research-workbench",s.id+" "+s.title,kind,"r30r1_research_measure/"+s.measure,"输入截至ordinal="+through,note,status,points,edges,Collections.singletonList(0.0),series);if(f.get("matrixRows") instanceof List&&f.get("matrixColumns") instanceof List){List<String> rows=new ArrayList<>(),cols=new ArrayList<>();for(Object x:(List<?>)f.get("matrixRows"))rows.add(String.valueOf(x));for(Object x:(List<?>)f.get("matrixColumns"))cols.add(String.valueOf(x));return new Panel(panel,rows,cols);}return panel;
    }
}
