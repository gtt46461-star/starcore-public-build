package com.ethk3.novaonepro;
import android.database.Cursor;
import java.util.Map;

/** Canonical per-issue attribution loaded on the existing UI snapshot worker. */
public final class AlgorithmAttributionR943 {
    private AlgorithmAttributionR943(){}
    public static void load(DbStore store,Map<String,String> states){
        try(Cursor c=store.readable().rawQuery("SELECT target_issue,candidate_name,candidate_id,applied,influence_weight,status FROM "+ResearchMainlineContributionAuditR922.TABLE+" ORDER BY target_issue DESC LIMIT 64",null)){
            while(c.moveToNext()){
                String name=c.getString(1),id=c.getString(2);
                String text=c.getInt(3)==1?"本期算法：V51 号码概率／1200动作排序 + "+(name==null||name.isEmpty()?id:name+" ["+id+"]")+"｜参与权重 "+String.format(java.util.Locale.ROOT,"%.1f%%",100*c.getDouble(4))
                        :"本期算法：V51 号码概率／1200动作排序｜创造算法未参与（"+c.getString(5)+"）";
                states.put("r943_algorithm_"+c.getString(0),text);
            }
        }catch(Exception error){states.put("r943_algorithm_read_error",error.getClass().getSimpleName());}
    }
    public static String line(Map<String,String> states,String issue){
        if(states==null)return "本期算法：等待同一期应用记录";
        String text=states.get("r943_algorithm_"+issue);
        return text==null?"本期算法：等待同一期应用记录":text;
    }
}
