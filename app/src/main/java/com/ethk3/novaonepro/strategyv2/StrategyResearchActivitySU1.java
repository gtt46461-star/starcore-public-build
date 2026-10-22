package com.ethk3.novaonepro.strategyv2;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.widget.*;
import com.ethk3.novaonepro.*;
import java.util.*;
import java.util.concurrent.*;

/** Read-only progress and evidence views; control changes are dispatched to the existing writer. */
public final class StrategyResearchActivitySU1 extends Activity {
    private final ExecutorService reader=Executors.newSingleThreadExecutor(r->new Thread(r,"su1-research-view"));
    private TextView text;
    private volatile boolean destroyed;
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setTitle(R.string.su1_research_title);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);
        add(root,R.string.su1_refresh,()->refresh(null));
        add(root,R.string.su1_schedule,()->{ReplayBurstSchedulerSU1.schedule(getApplicationContext());refresh(null);});
        add(root,R.string.su1_pause,()->refresh("DISABLED"));add(root,R.string.su1_resume,()->refresh("SHADOW"));
        ScrollView scroll=new ScrollView(this);text=new TextView(this);text.setTextSize(15);text.setTextIsSelectable(true);scroll.addView(text);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh(null);
    }
    private void add(LinearLayout parent,int label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());parent.addView(button);}
    private void refresh(String mode){
        text.setText(R.string.su1_initializing);
        reader.execute(()->{
            String result;
            try{DbStore store=new DbStore(getApplicationContext());
                if(!StrategySchemaSU1.ready(store.readable()))result="新引擎正在等待数据库升级。启动服务后，将在后台逐步完成。";
                else{
                    if(mode!=null)store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_USER_MODE",db->{db.execSQL("UPDATE su1_runtime_control SET mode=?,updated_at=?,reason='USER_CONTROL' WHERE singleton=1",new Object[]{mode,System.currentTimeMillis()});return 0;});
                    StringBuilder out=new StringBuilder("每个策略独立研究、提前冻结并等待实际结算。历史成绩与实时成绩分别展示。\n\n");
                    try(Cursor c=store.readable().rawQuery("SELECT mode FROM su1_runtime_control WHERE singleton=1",null)){if(c.moveToFirst())out.append("研究状态：").append(c.getString(0).equals("DISABLED")?"已暂停":"运行中").append('\n');}
                    out.append("活动：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_campaign")).append("；已检验程序：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_search_attempt WHERE result='EVALUATED'")).append('\n');
                    out.append("实时研究池：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_research_pool WHERE state='ACTIVE'")).append(" / 32\n");
                    out.append("已提炼结构：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_macro WHERE state='ACTIVE'")).append("；新家族：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_emergent_family")).append("\n\n实时研究记录\n");
                    String sql="SELECT r.strategy_id,r.canonical_text,coalesce(d.n,0)+coalesce(s.n,0),coalesce(d.k,0)+coalesce(s.k,0) FROM su1_runtime_program r LEFT JOIN (SELECT strategy_id,sum(actions) n,sum(hits) k FROM su1_live_daily GROUP BY strategy_id) d ON d.strategy_id=r.strategy_id LEFT JOIN (SELECT strategy_id,count(*) n,sum(hit) k FROM su1_live_shadow WHERE hit IS NOT NULL AND NOT EXISTS(SELECT 1 FROM su1_live_daily q WHERE q.strategy_id=su1_live_shadow.strategy_id AND q.day_key=su1_live_shadow.day_key) GROUP BY strategy_id) s ON s.strategy_id=r.strategy_id ORDER BY 3 DESC,r.strategy_id LIMIT 32";
                    try(Cursor c=store.readable().rawQuery(sql,null)){while(c.moveToNext()){int n=c.getInt(2),k=c.getInt(3);out.append(c.getString(0).substring(0,8)).append(" · ").append(n==0?"等待结算":String.format(Locale.CHINA,"%d/%d = %.1f%%",k,n,100.0*k/n)).append('\n').append(c.getString(1)).append("\n\n");}}
                    out.append("独立确认与主链资格：待积累前瞻证据。历史胜出及实时研究记录不会自动取得主链授权。\n");
                    try(Cursor c=store.readable().rawQuery("SELECT phase,detail FROM su1_health_event ORDER BY id DESC LIMIT 3",null)){while(c.moveToNext())out.append("最近状态：").append(c.getString(0)).append(' ').append(c.getString(1)).append('\n');}
                    result=out.toString();store.close();
                }
            }catch(RuntimeException failure){result=getString(R.string.su1_error,DatabaseCoordinator.describeCauseChain(failure,1200));}
            String shown=result;if(!destroyed)runOnUiThread(()->{if(!destroyed)text.setText(shown);});
        });
    }
    @Override protected void onDestroy(){destroyed=true;reader.shutdownNow();super.onDestroy();}
}
