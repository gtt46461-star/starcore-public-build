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
    private volatile String lastAction="";
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);setTitle(R.string.su1_research_title);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(24,24,24,24);
        add(root,R.string.su1_refresh,()->refresh(null));
        add(root,R.string.su1_schedule,()->{ReplayBurstSchedulerSU1.schedule(getApplicationContext());refresh(null);});
        add(root,R.string.su1_pause,()->refresh("DISABLED"));add(root,R.string.su1_resume,()->refresh("SHADOW"));
        add(root,R.string.su1_import,()->{android.content.Intent intent=new android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT);intent.setType("*/*");intent.addCategory(android.content.Intent.CATEGORY_OPENABLE);intent.addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(intent,5101);});
        add(root,R.string.su1_use_verified,()->refresh("MAINLINE_ADAPTER"));
        ScrollView scroll=new ScrollView(this);text=new TextView(this);text.setTextSize(15);text.setTextIsSelectable(true);scroll.addView(text);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh(null);
    }
    private void add(LinearLayout parent,int label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());parent.addView(button);}
    private void refresh(String mode){
        text.setText(R.string.su1_initializing);
        reader.execute(()->{
            String result;
            try{DbStore store=new DbStore(getApplicationContext());
                try{
                if(!StrategySchemaSU1.ready(store.readable()))result="新引擎正在等待数据库升级。启动服务后，将在后台逐步完成。";
                else{
                    if(mode!=null)store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_USER_MODE",db->{
                        if(mode.equals("MAINLINE_ADAPTER")){
                            boolean valid=false;try(Cursor c=db.rawQuery("SELECT certificate_id FROM su1_certificate ORDER BY issued_at DESC LIMIT 32",null)){while(c.moveToNext())if(CertificateSU1.read(db,c.getString(0),System.currentTimeMillis(),false)!=null){valid=true;break;}}
                            if(!valid){lastAction="尚无通过全部独立验证的策略，已继续保留研究状态。";return 0;}
                        }
                        db.execSQL("UPDATE su1_runtime_control SET mode=?,contract_hash=?,updated_at=?,reason='USER_CONTROL' WHERE singleton=1",new Object[]{mode,mode.equals("MAINLINE_ADAPTER")?MainlineAdapterSU1.CONTRACT:StrategyRepositorySU1.ACTION,System.currentTimeMillis()});return 0;
                    });
                    StringBuilder out=new StringBuilder(lastAction.isEmpty()?"":lastAction+"\n\n").append("每个策略独立研究、提前冻结并等待实际结算。历史成绩与实时成绩分别展示。\n\n");
                    try(Cursor c=store.readable().rawQuery("SELECT mode FROM su1_runtime_control WHERE singleton=1",null)){if(c.moveToFirst())out.append("研究状态：").append(c.getString(0).equals("DISABLED")?"已暂停":"运行中").append('\n');}
                    out.append("活动：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_campaign")).append("；已检验程序：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_search_attempt WHERE result='EVALUATED'")).append('\n');
                    out.append("实时研究池：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_research_pool WHERE state='ACTIVE'")).append(" / 32\n");
                    out.append("已提炼结构：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_macro WHERE state='ACTIVE'")).append("；新家族：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_emergent_family")).append("\n\n实时研究记录\n");
                    String sql="SELECT r.strategy_id,r.canonical_text,coalesce(d.n,0)+coalesce(s.n,0),coalesce(d.k,0)+coalesce(s.k,0) FROM su1_runtime_program r LEFT JOIN (SELECT strategy_id,sum(actions) n,sum(hits) k FROM su1_live_daily GROUP BY strategy_id) d ON d.strategy_id=r.strategy_id LEFT JOIN (SELECT strategy_id,count(*) n,sum(hit) k FROM su1_live_shadow WHERE hit IS NOT NULL AND NOT EXISTS(SELECT 1 FROM su1_live_daily q WHERE q.strategy_id=su1_live_shadow.strategy_id AND q.day_key=su1_live_shadow.day_key) GROUP BY strategy_id) s ON s.strategy_id=r.strategy_id ORDER BY 3 DESC,r.strategy_id LIMIT 32";
                    try(Cursor c=store.readable().rawQuery(sql,null)){while(c.moveToNext()){int n=c.getInt(2),k=c.getInt(3);out.append(c.getString(0).substring(0,8)).append(" · ").append(n==0?"等待结算":String.format(Locale.CHINA,"%d/%d = %.1f%%",k,n,100.0*k/n)).append('\n').append(c.getString(1)).append("\n\n");}}
                    out.append("已锁定未来测试：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_sealed_plan")).append("；已完成：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_sealed_result")).append('\n');
                    out.append("独立确认声明：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_eval_cohort WHERE evidence_role='CONFIRM'")).append("；证书记录：").append(StrategyRepositorySU1.count(store.readable(),"SELECT count(*) FROM su1_certificate")).append('\n');
                    try(Cursor budget=store.readable().rawQuery("SELECT coalesce(sum(amount),0) FROM su1_alpha_reservation",null)){if(budget.moveToFirst())out.append(String.format(Locale.CHINA,"本数据库累计错误预算占用：%.8f / 0.05\n",budget.getDouble(0)));}
                    try(Cursor alpha=store.readable().rawQuery("SELECT family_alpha,global_claim_ordinal,planned_opportunities FROM su1_eval_cohort WHERE evidence_role='CONFIRM' ORDER BY created_at DESC,global_claim_ordinal DESC LIMIT 1",null)){
                        if(alpha.moveToFirst()){double j=alpha.getDouble(1),lookAlpha=alpha.getDouble(0)/(j*(j+1)*2);long needed=(long)Math.ceil(2*Math.log(1/lookAlpha)/(.02*.02));out.append(String.format(Locale.CHINA,"最近声明 α：%.10f；固定计划：%d机会\n下界半径降至2%%需约%d个完整配对机会。\n",lookAlpha,alpha.getInt(2),needed));}
                    }
                    out.append("独立资格须以有效证书为准；历史成绩不会自动取得主链授权。\n");
                    try(Cursor c=store.readable().rawQuery("SELECT phase,detail FROM su1_health_event ORDER BY id DESC LIMIT 3",null)){while(c.moveToNext())out.append("最近状态：").append(c.getString(0)).append(' ').append(c.getString(1)).append('\n');}
                    result=out.toString();
                }
                }finally{store.close();}
            }catch(RuntimeException failure){result=getString(R.string.su1_error,DiagnosticsSU1.cause(failure,1200));}
            String shown=result;if(!destroyed)runOnUiThread(()->{if(!destroyed)text.setText(shown);});
        });
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);if(request!=5101||result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();
        reader.execute(()->{
            try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new java.io.IOException("无法读取所选文件");java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1){if(bytes.size()+n>32768)throw new java.io.IOException("工件超过32KB限制");bytes.write(block,0,n);}
                String wire=java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())).toString();DbStore store=new DbStore(getApplicationContext());
                try{String id=ArtifactImportSU1.install(store,wire);lastAction="已导入研究策略 "+id.substring(0,8)+"，实时证据从零开始。";}finally{store.close();}
            }catch(Exception failure){lastAction="导入未完成："+DiagnosticsSU1.cause(failure,400);}
            if(!destroyed)runOnUiThread(()->refresh(null));
        });
    }
    @Override protected void onDestroy(){destroyed=true;reader.shutdownNow();super.onDestroy();}
}
