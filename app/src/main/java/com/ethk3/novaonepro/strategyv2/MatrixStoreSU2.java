package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.IOException;
import java.util.*;
import org.json.*;

/** One shared persisted matrix document for report and dashboard; live grades never consume replay outcomes. */
public final class MatrixStoreSU2 {
    private MatrixStoreSU2() {}
    public static void save(SQLiteDatabase db,String key,long through,MatrixSnapshotSU2.Data data,long now){
        if(!db.inTransaction())throw new IllegalStateException("SU2_MATRIX_WRITER");
        db.execSQL("INSERT OR IGNORE INTO su2_matrix_snapshot(genome_key,checkpoint_sequence,snapshot_json,factors_blob,reason_state,graded_through,updated_at) VALUES(?,?,?,?,?,?,?)",new Object[]{key,through,data.json,data.factors,data.stream,through,now});
        db.execSQL("DELETE FROM su2_matrix_snapshot WHERE checkpoint_sequence<(SELECT min(checkpoint_sequence) FROM (SELECT checkpoint_sequence FROM su2_matrix_snapshot ORDER BY checkpoint_sequence DESC LIMIT 8))");
    }
    static final class Row {String key,json,stats;byte[] factors,state;long checkpoint,graded,end;}
    public static String maintain(DbStore store,String target){
        try{
            SQLiteDatabase db=store.readable();if(!StrategySchemaSU2.ready(db))return"SU2_MATRIX_SCHEMA_PENDING";
            EventFrameSU1 frame=EventFrameBuilderSU1.capture(store,target);int[][] history=frame.history();
            long first=TargetIssueResolver.sequenceOrdinalSU1(String.valueOf(frame.issue(0))),last=first+history.length-1;
            List<Row> rows=new ArrayList<>();
            try(Cursor c=db.rawQuery("SELECT genome_key,checkpoint_sequence,snapshot_json,factors_blob,reason_state,stats_json,graded_through FROM su2_matrix_snapshot ORDER BY checkpoint_sequence DESC LIMIT 8",null)){
                while(c.moveToNext()){Row r=new Row();r.key=c.getString(0);r.checkpoint=c.getLong(1);r.json=c.getString(2);r.factors=c.getBlob(3);r.state=c.getBlob(4);r.stats=c.getString(5);r.graded=c.getLong(6);r.end=last;rows.add(r);}
            }
            for(Row r:rows){
                for(Row later:rows)if(later.key.equals(r.key)&&later.checkpoint>r.checkpoint)r.end=Math.min(r.end,later.checkpoint);
                if(r.graded>=r.end)continue;
                SortedMap<Long,int[]> decisions=new TreeMap<>();
                try(Cursor c=db.rawQuery("SELECT sequence_ordinal,position,number_mask,hit FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>? AND sequence_ordinal<=? AND act=1 AND hit IS NOT NULL ORDER BY sequence_ordinal LIMIT 64",new String[]{r.key,String.valueOf(r.graded),String.valueOf(r.end)})){
                    while(c.moveToNext())decisions.put(c.getLong(0),new int[]{c.getInt(1),c.getInt(2),c.getInt(3)});
                }
                long end=decisions.size()==64?decisions.lastKey():r.end;
                Map<String,long[]> stats=readStats(r.stats);List<MatrixSnapshotSU2.Factor> factors=MatrixSnapshotSU2.decode(r.factors);AtomStreamSU2 stream=new AtomStreamSU2();
                long missing=Math.max(0,first-r.graded-1);
                if(missing==0)stream.read(new java.io.DataInputStream(new java.io.ByteArrayInputStream(r.state)));
                int begin=missing>0?0:(int)Math.max(0,r.graded+1-first);
                for(int i=begin;i<history.length&&first+i<=end;i++){
                    int[] decision=decisions.get(first+i);
                    if(decision!=null)for(Map.Entry<String,Double> support:MatrixSnapshotSU2.support(factors,stream,decision[0],decision[1]).entrySet()){
                        long[] count=stats.computeIfAbsent(support.getKey(),k->new long[4]);int sign=support.getValue()>0?0:2;count[sign]++;count[sign+1]+=decision[2];
                    }
                    stream.update(history[i]);
                }
                if(missing>0){long[] loss=stats.computeIfAbsent("__MISSING_SOURCE",k->new long[4]);loss[0]+=missing;}
                final long done=end;final String encoded=statsJson(stats);final Row row=r;final byte[] state=MatrixSnapshotSU2.streamBytes(stream);
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU2_MATRIX_GRADES",w->{w.execSQL("UPDATE su2_matrix_snapshot SET stats_json=?,reason_state=?,graded_through=?,updated_at=? WHERE genome_key=? AND checkpoint_sequence=? AND graded_through=?",new Object[]{encoded,state,done,System.currentTimeMillis(),row.key,row.checkpoint,row.graded});return 0;});
                return"SU2_MATRIX_GRADED:"+decisions.size(); // bounded slice: at most 64 frozen actions, one snapshot
            }
            return"SU2_MATRIX_UP_TO_DATE";
        }catch(IOException|RuntimeException|JSONException failure){return"SU2_MATRIX_FAILED:"+failure.getClass().getSimpleName();}
    }
    static Map<String,long[]> readStats(String text)throws JSONException{Map<String,long[]> out=new TreeMap<>();JSONObject j=new JSONObject(text);Iterator<String> it=j.keys();while(it.hasNext()){String key=it.next();JSONArray a=j.getJSONArray(key);out.put(key,new long[]{a.getLong(0),a.getLong(1),a.getLong(2),a.getLong(3)});}return out;}
    static String statsJson(Map<String,long[]> rows)throws JSONException{JSONObject j=new JSONObject();for(Map.Entry<String,long[]> e:rows.entrySet()){JSONArray a=new JSONArray();for(long n:e.getValue())a.put(n);j.put(e.getKey(),a);}return j.toString();}
    public static String report(SQLiteDatabase db){
        try{
            JSONArray snapshots=new JSONArray();
            try(Cursor c=db.rawQuery("SELECT snapshot_json,stats_json,graded_through FROM su2_matrix_snapshot ORDER BY checkpoint_sequence DESC LIMIT 8",null)){
                while(c.moveToNext()){
                    JSONObject row=new JSONObject(c.getString(0)),grades=new JSONObject();Map<String,long[]> stats=readStats(c.getString(1));
                    for(Map.Entry<String,long[]> e:stats.entrySet()){
                        long[] n=e.getValue();if(e.getKey().startsWith("__")){row.put("missing_source_periods",n[0]);continue;}
                        JSONObject g=new JSONObject();g.put("supported",n[0]);g.put("hit_when_supported",n[0]>0?n[1]/(double)n[0]:JSONObject.NULL);g.put("other",n[2]);g.put("hit_otherwise",n[2]>0?n[3]/(double)n[2]:JSONObject.NULL);
                        g.put("wilson_supported",MatrixSnapshotSU2.wilson(n[1],n[0]));g.put("grade",stats.containsKey("__MISSING_SOURCE")?"C_EXPLAIN_ONLY":MatrixSnapshotSU2.grade(n[0],n[1],n[2],n[3]));grades.put(e.getKey(),g);
                    }
                    row.put("reason_grades",grades);row.put("forward_through",c.getLong(2));snapshots.put(row);
                }
            }
            return new JSONObject().put("schema","SU2_MATRICES_V1").put("snapshots",snapshots).toString()+"\n";
        }catch(RuntimeException|JSONException failure){return"{\"schema\":\"SU2_MATRICES_V1\",\"status\":\"PENDING\",\"snapshots\":[]}\n";}
    }
    public static String render(SQLiteDatabase db){
        StringBuilder out=new StringBuilder("\n因子、原因与教师矩阵\n");
        try{
            JSONArray a=new JSONObject(report(db)).getJSONArray("snapshots");if(a.length()==0)return out.append("委员会完成训练后保存快照；原因等级等待后续真实冻结样本。\n").toString();
            JSONObject s=a.getJSONObject(0);out.append("冻结序号 ").append(s.getLong("checkpoint_sequence")).append("；前推已核对至 ").append(s.getLong("forward_through")).append('\n');
            JSONArray factors=s.getJSONArray("factor_matrix");for(int i=0;i<Math.min(12,factors.length());i++){JSONObject f=factors.getJSONObject(i);out.append(f.getString("factor")).append(" · 权重 ").append(f.getJSONArray("by_position")).append('\n');}
            JSONObject grades=s.getJSONObject("reason_grades");Iterator<String> it=grades.keys();if(!it.hasNext())out.append("原因分级：待积累真实冻结出手。\n");while(it.hasNext()){String family=it.next();JSONObject g=grades.getJSONObject(family);String grade=g.getString("grade");String label=grade.equals("A_VALID")?"前推支持":grade.equals("B_USEFUL")?"有帮助，继续验证":grade.equals("E_FALSE")?"负向关联":"仅作解释";out.append(family).append(" · ").append(label).append(" · 支持/对照样本 ").append(g.getLong("supported")).append('/').append(g.getLong("other")).append('\n');}
            JSONArray teachers=s.getJSONArray("teacher_matrix");for(int i=0;i<teachers.length();i++){JSONObject t=teachers.getJSONObject(i);out.append("教师 ").append(i+1).append(" · ").append(t.getString("formula")).append('\n');JSONObject states=t.getJSONObject("gain_by_state");Iterator<String> names=states.keys();while(names.hasNext()){String name=names.next();JSONObject gain=states.getJSONObject(name);out.append(name).append("：高状态增益 ").append(gain.opt("high_mnat")).append(" / 低状态增益 ").append(gain.opt("low_mnat")).append("；样本 ").append(gain.getLong("high_n")).append('/').append(gain.getLong("low_n")).append('\n');}}
            JSONObject explanation=s.getJSONObject("atomic_explanation");if(explanation.has("r2"))out.append("整体原子解释拟合度 R²：").append(explanation.get("r2")).append("；原子与权重 ").append(explanation.getJSONArray("atoms")).append('\n');
        }catch(JSONException failure){out.append("快照暂不可读。\n");}
        return out.length()>16000?out.substring(0,16000)+"\n完整矩阵见检测报告。\n":out.toString();
    }
}
