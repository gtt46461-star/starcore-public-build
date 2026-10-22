package com.ethk3.novaonepro;

import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import com.ethk3.novaonepro.r952.*;
import java.util.ArrayList;

/** Read-only evidence view; subclasses choose a page, never a training or business writer. */
public class DeepLearningActivity extends Activity implements DeepLearningPresenterR952.View {
    private DeepLearningPresenterR952 presenter;private TextView output;private EditText filter;
    private Button next,back;private String snapshot="",after="",nextKey="",copy="",activeFilter="";
    private final ArrayList<String> history=new ArrayList<String>();private boolean valid;
    protected String pageTitle(){return "深度学习与逐算法训练";}
    protected DeepLearningPresenterR952 createPresenter(){return new DeepLearningPresenterR952(this);}
    public static Intent createIntent(Context context,String game,String candidate){
        return new Intent(context,DeepLearningActivity.class).putExtra("game",game).putExtra("candidate",candidate);
    }
    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(16,12,16,12);
        TextView title=new TextView(this);title.setText(pageTitle());title.setTextSize(21);root.addView(title);
        TextView scope=new TextView(this);scope.setText("历史训练、Future评价、证书与主链采用分别显示。进化页保留最近30期应用，创造页保留最近200次尝试。缺失指标不表示0或通过。");root.addView(scope);
        Button routes=new Button(this);routes.setText("切换能力页面（沿用快照）");root.addView(routes);
        routes.setOnClickListener(new View.OnClickListener(){public void onClick(View v){
            new android.app.AlertDialog.Builder(DeepLearningActivity.this).setItems(new String[]{"深度学习","动态迭代","创造能力","原子与分子质量"},new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int which){
                String[] pages={"deep","evolution","creation","atoms"};
                startActivity(FullbloodPageProviderR952.intent(DeepLearningActivity.this,pages[which],snapshot,""));
            }}).show();
        }});
        filter=new EditText(this);filter.setSingleLine(true);filter.setHint("按名称或标识筛选");root.addView(filter);
        LinearLayout actions=new LinearLayout(this);root.addView(actions);
        Button search=new Button(this);search.setText("筛选");actions.addView(search);
        Button refresh=new Button(this);refresh.setText("读取新快照");actions.addView(refresh);
        Button copyButton=new Button(this);copyButton.setText("复制本页");actions.addView(copyButton);
        ScrollView scroll=new ScrollView(this);output=new TextView(this);output.setTextSize(15);output.setTextIsSelectable(true);scroll.addView(output);
        root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout pagination=new LinearLayout(this);back=new Button(this);back.setText("上一页");next=new Button(this);next.setText("下一页");pagination.addView(back);pagination.addView(next);root.addView(pagination);
        setContentView(root);
        String game=getIntent().getStringExtra("game");valid=game==null||V51Repository.GAME_CODE.equals(game);
        if(!valid){output.setText("INVALID_GAME：只支持当前 Binance1-PK10 数据。");return;}
        if(state!=null){snapshot=state.getString("snapshot","");after=state.getString("after","");activeFilter=state.getString("filter","");ArrayList<String> saved=state.getStringArrayList("history");if(saved!=null)history.addAll(saved);}
        else{snapshot=value(getIntent().getStringExtra("snapshot"));activeFilter=value(getIntent().getStringExtra("candidate"));}
        filter.setText(activeFilter);presenter=createPresenter();
        search.setOnClickListener(new View.OnClickListener(){public void onClick(View v){activeFilter=filter.getText().toString().trim();after="";history.clear();request();}});
        refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){snapshot="";after="";history.clear();request();}});
        copyButton.setOnClickListener(new View.OnClickListener(){public void onClick(View v){ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm!=null&&!copy.isEmpty()){cm.setPrimaryClip(ClipData.newPlainText(pageTitle(),copy));Toast.makeText(DeepLearningActivity.this,"已复制当前快照的本页内容",Toast.LENGTH_SHORT).show();}}});
        next.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(!nextKey.isEmpty()){history.add(after);after=nextKey;request();}}});
        back.setOnClickListener(new View.OnClickListener(){public void onClick(View v){if(!history.isEmpty()){after=history.remove(history.size()-1);request();}}});
    }
    private static String value(String s){return s==null?"":s;}
    private void request(){if(presenter==null)return;next.setEnabled(false);back.setEnabled(false);output.setText("正在读取同一快照……");presenter.load(snapshot,after,activeFilter);}
    @Override protected void onStart(){super.onStart();if(valid&&presenter!=null){presenter.attach(this);request();}}
    @Override protected void onStop(){if(presenter!=null)presenter.detach();super.onStop();}
    @Override protected void onDestroy(){if(presenter!=null)presenter.close();super.onDestroy();}
    @Override protected void onSaveInstanceState(Bundle state){state.putString("snapshot",snapshot);state.putString("after",after);state.putString("filter",activeFilter);state.putStringArrayList("history",history);super.onSaveInstanceState(state);}
    public void render(ResearchPageRepositoryR952.PageResult result){
        if(!result.snapshotId.isEmpty())snapshot=result.snapshotId;nextKey=result.nextKey;copy=result.text();
        output.setText(copy+(result.error.equals("SNAPSHOT_EXPIRED")?"\n快照已失效，请点击“读取新快照”。":""));
        next.setEnabled(!nextKey.isEmpty());back.setEnabled(!history.isEmpty());
    }
}
