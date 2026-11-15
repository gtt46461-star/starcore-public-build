package com.ethk3.novaonepro;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.widget.*;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

/** Actual bounded, cancellable, read-only consumer of all sixteen logical views. */
@android.annotation.SuppressLint("SetTextI18n")
public final class ResearchTablesActivityR31 extends Activity {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private CancellationSignal cancellation;private Future<?> pending;
    private Spinner view;private EditText role;private TextView contents;private long offset,epoch;
    private final List<String> ids=new ArrayList<>(ResearchTableAdaptersR30R1.ids());
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        view=new Spinner(this);view.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ids));root.addView(view);
        role=new EditText(this);role.setSingleLine();role.setHint("T01/T09 角色筛选，如 SELECT；留空读取全部角色");root.addView(role);
        LinearLayout controls=new LinearLayout(this);
        for(String title:new String[]{"读取","上一页","下一页"}){Button button=new Button(this);button.setText(title);button.setOnClickListener(v->{if(title.equals("读取"))offset=0;else offset=title.equals("上一页")?Math.max(0,offset-20):offset+20;load();});controls.addView(button,new LinearLayout.LayoutParams(0,-2,1));}
        root.addView(controls);ScrollView scroll=new ScrollView(this);contents=new TextView(this);contents.setTextIsSelectable(true);contents.setPadding(16,16,16,16);scroll.addView(contents);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);load();
    }
    private void load() {
        cancel();long request=epoch;String id=ids.get(view.getSelectedItemPosition());long pageOffset=offset;
        Map<String,String> filters=new LinkedHashMap<>();String selectedRole=role.getText().toString().trim();if(!selectedRole.isEmpty()&&(id.equals("T01")||id.equals("T09")))filters.put("role",selectedRole);
        CancellationSignal signal=new CancellationSignal();cancellation=signal;ui.postDelayed(signal::cancel,5000);
        contents.setText(id+" · 读取第 "+(pageOffset+1)+" 行起的20行……");
        pending=worker.submit(()->{String result;
            try{File file=getDatabasePath(AppConfig.DB_NAME);
                try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(getApplicationContext(),file,signal);SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY)) {
                    result=JsonR27.write(ResearchTableAdaptersR30R1.readPageR31(db,id,filters,pageOffset,20,signal));
                }
            }catch(OperationCanceledException cancelled){result="读取已取消或超过5秒。";}
             catch(Exception error){result="READ_ERROR: "+error.getClass().getSimpleName()+": "+error.getMessage();}
            final String rendered=result;ui.post(()->{if(request==epoch&&!isFinishing())contents.setText(rendered);});
        });
    }
    private void cancel(){epoch++;if(cancellation!=null)cancellation.cancel();if(pending!=null)pending.cancel(true);}
    @Override protected void onStop(){cancel();super.onStop();}
    @Override protected void onDestroy(){cancel();worker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
