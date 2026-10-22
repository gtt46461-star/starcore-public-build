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
                        if(mode.equals("ROLLBACK_MODEL")){lastAction=MainlineHeadSU1.rollback(db,System.currentTimeMillis())?"已回退到仍持有效证书的父代；自动晋级已暂停。":"没有仍持有效证书的父代，已保留当前策略和原始证据。";return 0;}
                        if(mode.equals("MAINLINE_ADAPTER")){
                            boolean valid=false;try(Cursor c=db.rawQuery("SELECT certificate_id FROM su1_certificate ORDER BY issued_at DESC LIMIT 32",null)){while(c.moveToNext())if(CertificateSU1.read(db,c.getString(0),System.currentTimeMillis(),false)!=null){valid=true;break;}}
                            if(!valid){lastAction="尚无通过全部独立验证的策略，已继续保留研究状态。";return 0;}
                        }
                        db.execSQL("UPDATE su1_runtime_control SET mode=?,contract_hash=?,updated_at=?,reason='USER_CONTROL' WHERE singleton=1",new Object[]{mode,mode.equals("MAINLINE_ADAPTER")?MainlineAdapterSU1.CONTRACT:StrategyRepositorySU1.ACTION,System.currentTimeMillis()});return 0;
                    });
                    result=(lastAction.isEmpty()?"":lastAction+"\n\n")+ResearchDashboardSU1.render(store.readable());
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
                if(in==null)throw new java.io.IOException("无法读取所选文件");java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();byte[] block=new byte[4096];int n;while((n=in.read(block))!=-1){if(bytes.size()+n>262144)throw new java.io.IOException("工件超过256KB限制");bytes.write(block,0,n);}
                String wire=java.nio.charset.StandardCharsets.UTF_8.newDecoder().onMalformedInput(java.nio.charset.CodingErrorAction.REPORT).decode(java.nio.ByteBuffer.wrap(bytes.toByteArray())).toString();DbStore store=new DbStore(getApplicationContext());
                try{String id=ArtifactImportSU1.install(store,wire);lastAction="已导入研究策略 "+id.substring(0,8)+"，实时证据从零开始。";}finally{store.close();}
            }catch(Exception failure){lastAction="导入未完成："+DiagnosticsSU1.cause(failure,400);}
            if(!destroyed)runOnUiThread(()->refresh(null));
        });
    }
    @Override protected void onDestroy(){destroyed=true;reader.shutdownNow();super.onDestroy();}
}
