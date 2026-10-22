package com.ethk3.novaonepro;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.*;
import android.widget.*;
import java.io.File;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/** Read-only health entry: never constructs DbStore or runs schema/epoch writers. */
public final class DatabaseHealthActivity extends Activity {
    private TextView text;private final AtomicLong generation=new AtomicLong();
    private final ExecutorService reader=Executors.newSingleThreadExecutor(r->new Thread(r,"r82-health-read"));
    private final Handler main=new Handler(Looper.getMainLooper());private CancellationSignal cancel;private Future<?> future;
    @Override protected void onCreate(Bundle state){super.onCreate(state);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,16,20,16);
        TextView title=new TextView(this);title.setText("数据库健康与空间维护");title.setTextSize(20);root.addView(title);
        Button refresh=new Button(this);refresh.setText("刷新只读检查");root.addView(refresh);refresh.setOnClickListener(v->refresh());
        Button rebuild=new Button(this);rebuild.setText("停止运行并重建空间");root.addView(rebuild);rebuild.setOnClickListener(v->confirm(false));
        Button cleanup=new Button(this);cleanup.setText("单独清理已验证旧库备份");root.addView(cleanup);cleanup.setOnClickListener(v->confirm(true));
        Button copy=new Button(this);copy.setText("复制检查内容");root.addView(copy);copy.setOnClickListener(v->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("数据库健康",text.getText()));});
        ScrollView scroll=new ScrollView(this);text=new TextView(this);text.setTextSize(14);text.setTextIsSelectable(true);scroll.addView(text);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh();
    }
    private void confirm(boolean cleanup){new AlertDialog.Builder(this).setTitle(cleanup?"单独清理已验证备份":"完整副本重建数据库")
        .setMessage(cleanup?"将停止运行，校验并删除保留的旧数据库备份。删除后无法通过该备份回退；不会自动恢复运行。":"将停止运行，离线逐表复制并验证行号、类型、值、结构与外键后安全换库。需要至少一份完整副本空间。旧库备份保留，实际磁盘空间需之后单独清理备份才释放；完成后请手动启动运行。安全换库阶段请勿强制停止。")
        .setNegativeButton("取消",null).setPositiveButton(cleanup?"确认清理备份":"确认停止并重建",(d,w)->{try{StorageMaintenanceServiceR82.request(getApplicationContext(),cleanup);refresh();}catch(RuntimeException failure){text.setText(getString(R.string.maintenance_not_started,DatabaseCoordinator.describeCauseChain(failure,1200)));}}).show();}
    private void refresh(){long token=generation.incrementAndGet();if(cancel!=null)cancel.cancel();if(future!=null)future.cancel(true);cancel=new CancellationSignal();final CancellationSignal signal=cancel;text.setText("正在后台读取；返回将取消读取。不会修改数据库。");
        future=reader.submit(()->{String result;try{result=readHealthR82(getApplicationContext(),signal);}catch(OperationCanceledException canceled){result="读取已取消或超时；数据库未修改。";}catch(Throwable failure){result="读取失败，数据未删除。\n"+DatabaseCoordinator.describeCauseChain(failure,1600);}final String shown=result;main.post(()->{if(generation.get()==token)text.setText(shown);});});
        main.postDelayed(()->{if(generation.get()==token&&!signal.isCanceled())signal.cancel();},5000);
    }
    /** Source entry used by the UI and host integration test. Every SQLite SQL carries cancel. */
    public static String readHealthR82(Context context,CancellationSignal cancel)throws Exception {
        Context app=context.getApplicationContext();File path=app.getDatabasePath(AppConfig.DB_NAME);
        if(!path.isFile())return "DATABASE_MISSING\n"+StorageMaintenanceServiceR82.summary(app);
        if(DatabaseCoordinator.isExplicitMaintenanceActiveR82())return "维护进行中；暂不打开数据库。\n"+StorageMaintenanceServiceR82.summary(app);
        try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(app,path,cancel);
            SQLiteDatabase db=SQLiteDatabase.openDatabase(path.getCanonicalPath(),null,SQLiteDatabase.OPEN_READONLY)) {
            cancel.throwIfCanceled();StringBuilder out=new StringBuilder("【只读完整性】\n");
            try{DatabaseIntegrityVerifier.IntegrityReport r=DatabaseIntegrityVerifier.verify(db,cancel);out.append("valid=").append(r.valid).append("\nexpected=").append(r.expectedChecksum).append("\nstored=").append(r.storedChecksum).append("\nmissing table/column/index=").append(r.missingTableCount).append('/').append(r.missingColumnCount).append('/').append(r.missingIndexCount).append("\ndetail=").append(r.detail);}
            catch(OperationCanceledException canceled){throw canceled;}catch(RuntimeException error){out.append("INTEGRITY_UNKNOWN ").append(error.getClass().getSimpleName());}
            out.append("\n\n【账本计数：失败为UNKNOWN，不冒充0】\n");
            for(String table:new String[]{SchemaContractRegistry.TABLE_ISSUE,SchemaContractRegistry.TABLE_ACTION,SchemaContractRegistry.TABLE_NUMBER,SchemaContractRegistry.TABLE_SELECTED,SchemaContractRegistry.TABLE_AUTH,SchemaContractRegistry.TABLE_STATE_HISTORY})out.append(table).append('=').append(count(db,table,cancel)).append('\n');
            out.append("\n【最近未解决错误】\n");appendRecent(out,db,"SELECT target_issue,failure_code,owner,detail,created_at FROM "+SchemaContractRegistry.TABLE_LEDGER_ERROR+" WHERE resolved=0 ORDER BY created_at DESC LIMIT 30",cancel);
            out.append("\n【最近迁移】\n");appendRecent(out,db,"SELECT from_version,to_version,status,error_detail,created_at FROM "+SchemaContractRegistry.TABLE_MIGRATION_LOG+" ORDER BY created_at DESC LIMIT 10",cancel);
            out.append("\n【离线空间维护】\n").append(StorageMaintenanceServiceR82.summary(app));return out.toString();
        }
    }
    private static String count(SQLiteDatabase db,String table,CancellationSignal cancel){cancel.throwIfCanceled();try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null,cancel)){return c.moveToFirst()?Long.toString(c.getLong(0)):"UNKNOWN_EMPTY_RESULT";}catch(OperationCanceledException canceled){throw canceled;}catch(RuntimeException failure){return "UNKNOWN:"+failure.getClass().getSimpleName();}}
    private static void appendRecent(StringBuilder out,SQLiteDatabase db,String sql,CancellationSignal cancel){cancel.throwIfCanceled();try(Cursor c=db.rawQuery(sql,null,cancel)){boolean any=false;while(c.moveToNext()){cancel.throwIfCanceled();any=true;for(int i=0;i<c.getColumnCount();i++){if(i>0)out.append('|');out.append(c.isNull(i)?"NULL":c.getString(i));}out.append('\n');}if(!any)out.append("NONE\n");}catch(OperationCanceledException canceled){throw canceled;}catch(RuntimeException failure){out.append("UNKNOWN:").append(failure.getClass().getSimpleName()).append('\n');}}
    @Override protected void onDestroy(){generation.incrementAndGet();if(cancel!=null)cancel.cancel();if(future!=null)future.cancel(true);main.removeCallbacksAndMessages(null);reader.shutdownNow();super.onDestroy();}
}
