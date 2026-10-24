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
        add(root,R.string.su1_rollback_model,()->refresh("ROLLBACK_MODEL"));
        add(root,R.string.su2_real_enable,()->new android.app.AlertDialog.Builder(this).setTitle(R.string.su2_real_enable).setMessage("仅允许持有有效样本外证书、且本期已在开奖前冻结出手的策略进入主链。没有有效证书时仍不出手；历史成绩不保证后续结果。").setNegativeButton("取消",null).setPositiveButton("确认启用",(d,w)->refresh("SU2_REAL_ON")).show());
        add(root,R.string.su2_real_disable,()->refresh("SU2_REAL_OFF"));
        ScrollView scroll=new ScrollView(this);text=new TextView(this);text.setTextSize(15);text.setTextIsSelectable(true);scroll.addView(text);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh(null);
    }
    private void add(LinearLayout parent,int label,Runnable action){Button button=new Button(this);button.setText(label);button.setOnClickListener(v->action.run());parent.addView(button);}
    private void refresh(String mode){
        text.setText(R.string.su1_initializing);
        reader.execute(()->{
            String result;
            try{
                // Reading this screen must not start cold DDL, runtime epochs or recovery writes.
                // The service owns initialization; a missing owner is a real waiting state.
                DatabaseCoordinator owner=DatabaseCoordinator.peek();
                if(owner==null||!owner.isCriticalSchemaReady())result=waitingForDatabase(owner==null);
                else if(!StrategySchemaSU1.ready(owner.readable()))result=waitingForDatabase(false);
                else{
                    if(mode!=null)owner.runAtomicWriteOrThrowF01("SU1_USER_MODE",db->{
                        if(mode.equals("SU2_REAL_ON")||mode.equals("SU2_REAL_OFF")){
                            boolean enabled=mode.equals("SU2_REAL_ON");
                            if(enabled){
                                boolean valid=false;
                                if(StrategySchemaSU2.ready(db))try(Cursor c=db.rawQuery("SELECT genome_key FROM su2_model_genome WHERE state='LEAGUE' LIMIT 32",null)){while(c.moveToNext())if(CertificateSU2.read(db,c.getString(0),System.currentTimeMillis())!=null){valid=true;break;}}
                                if(!valid){lastAction="尚无有效样本外证书，实盘授权保持关闭。";return 0;}
                            }
                            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{RealAuthoritySU2.USER_SWITCH,enabled?"1":"0",System.currentTimeMillis()});
                            lastAction=enabled?"已启用：仍须逐期通过证书、冻结出手与唯一主链校验。":"已关闭实盘授权；继续影子观察与训练。";return 0;
                        }
                        if(mode.equals("ROLLBACK_MODEL")){lastAction=MainlineHeadSU1.rollback(db,System.currentTimeMillis())?"已回退到仍持有效证书的父代；自动晋级已暂停。":"没有仍持有效证书的父代，已保留当前策略和原始证据。";return 0;}
                        if(mode.equals("MAINLINE_ADAPTER")){
                            boolean valid=false;try(Cursor c=db.rawQuery("SELECT certificate_id FROM su1_certificate ORDER BY issued_at DESC LIMIT 32",null)){while(c.moveToNext())if(CertificateSU1.read(db,c.getString(0),System.currentTimeMillis(),false)!=null){valid=true;break;}}
                            if(!valid){lastAction="尚无通过全部独立验证的策略，已继续保留研究状态。";return 0;}
                        }
                        db.execSQL("UPDATE su1_runtime_control SET mode=?,contract_hash=?,updated_at=?,reason='USER_CONTROL' WHERE singleton=1",new Object[]{mode,mode.equals("MAINLINE_ADAPTER")?MainlineAdapterSU1.CONTRACT:StrategyRepositorySU1.ACTION,System.currentTimeMillis()});return 0;
                    });
                    result=(lastAction.isEmpty()?"":lastAction+"\n\n")+ResearchDashboardSU1.render(owner.readable());
                }
            }catch(RuntimeException failure){result=getString(R.string.su1_error,DiagnosticsSU1.cause(failure,1200));}
            String shown=result;if(!destroyed)runOnUiThread(()->{if(!destroyed)text.setText(shown);});
        });
    }
    private String waitingForDatabase(boolean notStarted){
        return ApiCredentialSettingsCP12.endpointStatus("")+"\n\n"+(notStarted
                ?"数据库尚未启动。请先在首页启动服务，再刷新研究状态。"
                :"数据库尚未准备就绪。请在首页查看启动状态；后台升级完成后刷新此页。");
    }
    @Override protected void onActivityResult(int request,int result,android.content.Intent data){
        super.onActivityResult(request,result,data);if(request!=5101||result!=RESULT_OK||data==null||data.getData()==null)return;android.net.Uri uri=data.getData();
        reader.execute(()->{
            try(java.io.InputStream in=getContentResolver().openInputStream(uri)){
                if(in==null)throw new java.io.IOException("无法读取所选文件");java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1){if(bytes.size()+n>262144)throw new java.io.IOException("工件超过256KB限制");bytes.write(block,0,n);}
                String wire=java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())).toString();DbStore store=new DbStore(getApplicationContext());
                try{String id=ArtifactImportSU1.install(store,wire);lastAction="已导入研究策略 "+id.substring(0,8)+"，实时证据从零开始。";}finally{store.close();}
            }catch(Exception failure){lastAction="导入未完成："+DiagnosticsSU1.cause(failure,400);}
            if(!destroyed)runOnUiThread(()->refresh(null));
        });
    }
    @Override protected void onDestroy(){destroyed=true;reader.shutdownNow();super.onDestroy();}
}
