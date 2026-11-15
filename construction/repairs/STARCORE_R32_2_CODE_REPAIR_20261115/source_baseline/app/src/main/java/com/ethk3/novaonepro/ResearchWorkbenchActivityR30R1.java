package com.ethk3.novaonepro;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.*;
import android.widget.*;
import com.ethk3.novaonepro.r952.ResearchChartViewR82;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import static com.ethk3.novaonepro.ResearchVisualDataR82.*;

/** Three groups of six research views. Opening a chart never starts training or grants a certificate. */
@android.annotation.SuppressLint("SetTextI18n")
public final class ResearchWorkbenchActivityR30R1 extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();private final Handler ui=new Handler(Looper.getMainLooper());
    private CancellationSignal cancellation;private Future<?> future;private Runnable timeout;
    private LinearLayout content;private TextView status;private long epoch;private int group;private android.app.AlertDialog details;
    TextView text(String s,int size){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(Color.rgb(225,237,246));t.setPadding(16,12,16,12);return t;}
    @Override protected void onCreate(Bundle state){super.onCreate(state);if(state!=null)group=state.getInt("group",0);group=Math.max(0,Math.min(2,group));
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(11,23,39));root.addView(text("研究工作台 · 18个研究图",21));
        root.addView(text("分析产物→算法动作→独立确认→主链证据。未产出、未消费和未证明有效分别显示。",13));
        LinearLayout tabs=new LinearLayout(this);String[] names={"结构与归因","创造与融合","质量与应用"};for(int i=0;i<3;i++){final int id=i;Button b=new Button(this);b.setText(names[i]);b.setOnClickListener(v->{group=id;load();});tabs.addView(b,new LinearLayout.LayoutParams(0,-2,1));}root.addView(tabs);
        Button tables=new Button(this);tables.setText("查看16张研究逻辑表");tables.setOnClickListener(v->startActivity(new android.content.Intent(this,ResearchTablesActivityR31.class)));root.addView(tables);
        status=text("等待读取已提交分析……",13);root.addView(status);ScrollView scroll=new ScrollView(this);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);scroll.addView(content);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
    }
    @Override protected void onStart(){super.onStart();load();}
    @Override protected void onStop(){cancel();if(details!=null){details.dismiss();details=null;}super.onStop();}
    @Override protected void onDestroy(){cancel();worker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle out){out.putInt("group",group);super.onSaveInstanceState(out);}
    static String stateText(String state){if(state==null)return "状态未取得";if(state.startsWith("READ_ERROR"))return "读取失败，原数据保留";if(state.startsWith("WAITING"))return "等待已提交的分析产物";if(state.startsWith("BLOCKED"))return "所需源数据或依赖尚未满足";if(state.startsWith("INSUFFICIENT"))return "样本或分析证据不足";if(state.startsWith("MEASURED")||state.equals("COMMITTED"))return "已有分析记录，效果仍需独立验证";return "分析状态："+state+"；有效性另行验证";}
    private void showDetails(Panel panel){if(details!=null)details.dismiss();ScrollView scroll=new ScrollView(this);scroll.setBackgroundColor(Color.rgb(11,23,39));TextView note=text(panel.status+"\n"+panel.note,13);note.setTextIsSelectable(true);scroll.addView(note);details=new android.app.AlertDialog.Builder(this).setTitle(panel.title).setView(scroll).setPositiveButton("关闭",null).create();details.show();}
    private void cancel(){epoch++;if(cancellation!=null)cancellation.cancel();if(future!=null)future.cancel(true);if(timeout!=null)ui.removeCallbacks(timeout);}
    private void load(){cancel();final long request=epoch;final int selectedGroup=group;final CancellationSignal signal=new CancellationSignal();cancellation=signal;timeout=signal::cancel;ui.postDelayed(timeout,5000);status.setText("读取已提交研究快照……");
        future=worker.submit(()->{List<Panel> panels=null;String failure=null;
            try{File file=getApplicationContext().getDatabasePath(AppConfig.DB_NAME);if(!file.isFile())throw new IllegalStateException("数据库尚未建立");
                try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(getApplicationContext(),file,signal);SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)){panels=ResearchWorkbenchRepositoryR30R1.load(db,selectedGroup,signal);}
            }catch(OperationCanceledException cancelled){failure="读取已取消或超过5秒；研究任务继续按原调度运行。";}catch(RuntimeException|java.io.IOException error){failure="研究视图读取失败："+error.getClass().getSimpleName();}
            final List<Panel> ready=panels;final String message=failure;ui.post(()->{if(request!=epoch||isFinishing())return;if(timeout!=null)ui.removeCallbacks(timeout);content.removeAllViews();if(ready==null){status.setText(message);return;}status.setText("本组"+ready.size()+"图；页面只读，数据状态逐图显示。");for(Panel p:ready){LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setTag(p.title.substring(0,3));card.addView(text(p.title,17));card.addView(text(stateText(p.status)+"\n"+p.window,12));if(!"STATUS".equals(p.kind))card.addView(new ResearchChartViewR82(this,p));Button source=new Button(this);source.setText("查看来源与分析口径");source.setOnClickListener(v->showDetails(p));card.addView(source);content.addView(card);}});
        });
    }
}
