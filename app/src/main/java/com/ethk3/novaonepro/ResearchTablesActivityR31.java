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
    private Spinner view;private EditText role;private TextView contents;private Button next;private long offset,epoch,nextOffset;private Runnable timeout;private boolean hasMore,started;private String filterIdentity="";
    private final List<String> ids=new ArrayList<>(ResearchTableAdaptersR30R1.ids());
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);if(state!=null){offset=state.getLong("offset",0);filterIdentity=state.getString("filterIdentity","");}LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);
        view=new Spinner(this);view.setAdapter(new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,ids));root.addView(view);if(state!=null)view.setSelection(Math.max(0,Math.min(ids.size()-1,state.getInt("view",0))));
        role=new EditText(this);role.setSingleLine();role.setHint("T01/T09 角色筛选，如 SELECT；留空读取全部角色");root.addView(role);if(state!=null)role.setText(state.getString("role",""));
        LinearLayout controls=new LinearLayout(this);
        for(String title:new String[]{"读取","上一页","下一页"}){Button button=new Button(this);button.setText(title);if(title.equals("下一页")){next=button;button.setEnabled(false);}button.setOnClickListener(v->{if(title.equals("读取"))offset=0;else if(title.equals("上一页"))offset=Math.max(0,offset-20);else {if(!hasMore)return;offset=nextOffset;}load();});controls.addView(button,new LinearLayout.LayoutParams(0,-2,1));}
        root.addView(controls);ScrollView scroll=new ScrollView(this);contents=new TextView(this);contents.setTextIsSelectable(true);contents.setPadding(16,16,16,16);scroll.addView(contents);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);view.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int at,long id){if(started)load();}public void onNothingSelected(android.widget.AdapterView<?> p){}});
    }
    private void load() {
        cancel();long request=epoch;String id=ids.get(view.getSelectedItemPosition());String identity=id+"|"+role.getText().toString().trim();if(!identity.equals(filterIdentity)){offset=0;filterIdentity=identity;}long pageOffset=offset;hasMore=false;next.setEnabled(false);
        Map<String,String> filters=new LinkedHashMap<>();String selectedRole=role.getText().toString().trim();if(!selectedRole.isEmpty()&&(id.equals("T01")||id.equals("T09")))filters.put("role",selectedRole);
        CancellationSignal signal=new CancellationSignal();cancellation=signal;timeout=signal::cancel;ui.postDelayed(timeout,5000);
        contents.setText(id+" · 读取第 "+(pageOffset+1)+" 行起的20行……");
        pending=worker.submit(()->{String result;Map<String,Object> page=null;
            try{File file=getDatabasePath(AppConfig.DB_NAME);
                try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(getApplicationContext(),file,signal);SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getPath(),null,SQLiteDatabase.OPEN_READONLY)) {
                    page=ResearchTableAdaptersR30R1.readPageR31(db,id,filters,pageOffset,20,signal);result=JsonR27.write(page);
                }
            }catch(OperationCanceledException cancelled){result="读取已取消或超过5秒。";}
             catch(Exception error){result="READ_ERROR: "+error.getClass().getSimpleName()+": "+error.getMessage();}
            final String rendered=result;final Map<String,Object> completed=page;ui.post(()->{if(request==epoch&&started&&!isFinishing()){if(timeout!=null)ui.removeCallbacks(timeout);timeout=null;cancellation=null;pending=null;contents.setText(rendered);hasMore=completed!=null&&Boolean.TRUE.equals(completed.get("hasMore"));nextOffset=completed!=null&&completed.get("nextOffset") instanceof Number?((Number)completed.get("nextOffset")).longValue():pageOffset;next.setEnabled(hasMore);}});
        });
    }
    private void cancel(){epoch++;if(timeout!=null)ui.removeCallbacks(timeout);timeout=null;if(cancellation!=null)cancellation.cancel();cancellation=null;if(pending!=null)pending.cancel(true);pending=null;}
    @Override protected void onStart(){super.onStart();started=true;load();}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("view",view.getSelectedItemPosition());state.putString("role",role.getText().toString());state.putLong("offset",offset);state.putString("filterIdentity",filterIdentity);super.onSaveInstanceState(state);}
    @Override protected void onStop(){started=false;cancel();super.onStop();}
    @Override protected void onDestroy(){cancel();worker.shutdownNow();ui.removeCallbacksAndMessages(null);super.onDestroy();}
}
