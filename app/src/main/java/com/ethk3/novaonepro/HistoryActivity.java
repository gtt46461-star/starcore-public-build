package com.ethk3.novaonepro;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

/** R900 draw-first history: raw开奖 is always visible; prediction/settlement/OOS are joined when present. */
public final class HistoryActivity extends Activity {
    private DbStore store; // Created and used only on the history worker.
    private final java.util.concurrent.ExecutorService reads = java.util.concurrent.Executors.newSingleThreadExecutor();
    private volatile boolean destroyed;
    private long requestGeneration;

    private HistoryListAdapter adapter;
    private int offset;
    private TextView pageText;
    private Button previous, next;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_history);
        adapter = new HistoryListAdapter(this);
        ListView list = (ListView) findViewById(R.id.list_v49_history);
        list.setAdapter(adapter);
        pageText = (TextView) findViewById(R.id.txt_history_page);
        previous = (Button) findViewById(R.id.btn_history_previous);
        next = (Button) findViewById(R.id.btn_history_next);
        previous.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ offset=Math.max(0,offset-AppConfig.V50.UI_PAGE_SIZE); load(); }});
        next.setOnClickListener(new View.OnClickListener(){ public void onClick(View v){ offset+=AppConfig.V50.UI_PAGE_SIZE; load(); }});
        list.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            public void onItemClick(AdapterView<?> p,View v,int position,long id){
                Object item=adapter.getItem(position);
                if(item instanceof HistoryListAdapter.Row){ HistoryListAdapter.Row r=(HistoryListAdapter.Row)item; DiagnosisActivity.openText(HistoryActivity.this,r.issue,r.body); }
            }});
        load();
    }

    private void load() {
        if(destroyed)return;
        final int requestedOffset=offset;
        final long generation=++requestGeneration;
        previous.setEnabled(false);next.setEnabled(false);
        pageText.setText("正在读取第"+(requestedOffset/AppConfig.V50.UI_PAGE_SIZE+1)+"页……");
        reads.execute(new Runnable(){public void run(){
            if(destroyed)return;
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
            final ArrayList<HistoryListAdapter.Row> rows=readPage(requestedOffset);
            runOnUiThread(new Runnable(){public void run(){
                if(destroyed||isFinishing()||generation!=requestGeneration)return;
                adapter.replace(rows);
                int page=requestedOffset/AppConfig.V50.UI_PAGE_SIZE+1;
                pageText.setText("第"+page+"页｜每页"+AppConfig.V50.UI_PAGE_SIZE+"期｜开奖为主，预测/OOS附加");
                previous.setEnabled(requestedOffset>0);next.setEnabled(rows.size()>=AppConfig.V50.UI_PAGE_SIZE);
            }});
        }});
    }

    private ArrayList<HistoryListAdapter.Row> readPage(int requestedOffset) {
        ArrayList<HistoryListAdapter.Row> rows=new ArrayList<HistoryListAdapter.Row>();
        Cursor c=null;
        try {
            if(store==null)store=new DbStore(getApplicationContext());
            String sql="SELECT d.issue,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10,d.open_time,d.source_line,"
                    + "COALESCE(o.position,0),COALESCE(o.n1,0),COALESCE(o.n2,0),COALESCE(o.n3,0),"
                    + "COALESCE(o.selected_hit_p,-1),COALESCE(o.lane,''),COALESCE(o.decision_action,''),"
                    + "COALESCE(o.settled,0),COALESCE(o.hit_label,-1),COALESCE(o.reject_code,''),COALESCE(o.contract_hash,''),"
                    + "COALESCE(t.task_class,''),COALESCE(t.status,''),COALESCE(t.terminal_code,''),COALESCE(t.attempts,0),COALESCE(t.last_error,''),"
                    + "COALESCE(l.strict_oos,0),COALESCE(l.error_code,'')"
                    + " FROM "+DbHelper.TABLE_DRAWS+" d LEFT JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o ON o.target_issue=d.issue"
                    + " LEFT JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=d.issue"
                    + " LEFT JOIN "+DbHelper.TABLE_R35_OOS_LABEL+" l ON l.target_issue=d.issue"
                    + " WHERE d.dirty=0 ORDER BY d.issue DESC LIMIT "+AppConfig.V50.UI_PAGE_SIZE+" OFFSET "+requestedOffset;
            c=store.readable().rawQuery(sql,null);
            while(c.moveToNext()) rows.add(toRow(c));
        } catch(Throwable e){
            HistoryListAdapter.Row r=new HistoryListAdapter.Row(); r.id=-1; r.issue="读取失败"; r.result="READ_ERROR";
            r.action=e.getClass().getSimpleName(); r.detail=safe(e.getMessage()); r.body=r.action+':'+r.detail; rows.add(r);
        } finally { if(c!=null)c.close(); }
        return rows;
    }

    private static HistoryListAdapter.Row toRow(Cursor c){
        String issue=safe(c.getString(0));
        String actual=ten(c,1);
        long openTime=c.getLong(11); String source=safe(c.getString(12));
        int pos=c.getInt(13), p1=c.getInt(14),p2=c.getInt(15),p3=c.getInt(16);
        double selectedP=c.getDouble(17); String lane=safe(c.getString(18)), action=safe(c.getString(19));
        boolean settled=c.getInt(20)==1; int hit=c.getInt(21); String reject=safe(c.getString(22)), contract=safe(c.getString(23));
        String taskClass=safe(c.getString(24)), taskStatus=safe(c.getString(25)), terminal=safe(c.getString(26)); int attempts=c.getInt(27); String taskError=safe(c.getString(28));
        int strict=c.getInt(29); String oosError=safe(c.getString(30));
        boolean hasPred=pos>=1 && pos<=10 && p1>0 && p2>0 && p3>0;
        String result=!hasPred?"开奖已入库｜本期无合法赛前预测":(!settled?"SHADOW待结算":(hit==1?"SHADOW命中":"SHADOW未中"));
        if(taskStatus.indexOf("FAILED")>=0||terminal.indexOf("FAIL")>=0) result="结算异常｜"+result;
        HistoryListAdapter.Row r=new HistoryListAdapter.Row(); r.id=stableId(issue); r.issue="期="+issue; r.result=result;
        r.action="开奖号码："+actual+(hasPred?"\n赛前冻结：P"+pos+" "+UiFormatter.numberGroup(p1,p2,p3):"\n赛前冻结：无");
        r.probability=hasPred?"SelectedHitP="+UiFormatter.formatOptionalProbability(selectedP>=0&&selectedP<=1,selectedP,"未证明/不可用"):"SelectedHitP=N/A";
        r.detail="结算="+fallback(taskClass,"NO_TASK")+'/'+fallback(taskStatus,"NO_TASK")+"｜attempts="+attempts+"｜strictOOS="+strict;
        r.reason="Reject="+fallback(reject,"--")+"｜"+fallback(first(taskError,oosError),"无错误");
        r.body="【R900开奖历史】\nissue="+issue+"\nactual="+actual+"\nopenTime="+openTime+"\nsource="+source
                +"\n\n【赛前冻结】\nprediction="+(hasPred?("P"+pos+' '+UiFormatter.numberGroup(p1,p2,p3)):"NONE")
                +"\nlane/action="+lane+'/'+action+"\nSelectedHitP="+selectedP+"\nsettled="+settled+"\nhit="+hit
                +"\n\n【Settlement/OOS】\ntask="+taskClass+'/'+taskStatus+"\nterminal="+terminal+"\nattempts="+attempts+"\nerror="+taskError
                +"\nstrictOos="+strict+"\noosError="+oosError+"\ncontract="+contract;
        return r;
    }

    private static String ten(Cursor c,int start){ StringBuilder b=new StringBuilder(); for(int i=0;i<10;i++){ if(i>0)b.append(' '); int n=c.getInt(start+i); if(n<10)b.append('0'); b.append(n);} return b.toString(); }
    @Override protected void onDestroy(){
        destroyed=true;++requestGeneration;reads.shutdownNow();super.onDestroy();
        // DbStore does not own the shared connection; never close it from a racing UI callback.
    }
    private static long stableId(String s){ try{return Long.parseLong(s);}catch(Throwable e){return s.hashCode();} }
    private static String first(String a,String b){ return safe(a).length()>0?a:safe(b); }
    private static String fallback(String v,String f){ return safe(v).length()==0?f:v; }
    private static String safe(String s){ return s==null?"":s; }
}
