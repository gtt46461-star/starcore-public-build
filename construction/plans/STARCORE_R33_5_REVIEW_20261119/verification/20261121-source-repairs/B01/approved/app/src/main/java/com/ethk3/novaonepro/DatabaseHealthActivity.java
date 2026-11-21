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
    private static final int CUSTODY_TREE_R335=3351,CUSTODY_IMPORT_R335=3352;private String custodyBackupR335;
    private TextView text;private final AtomicLong generation=new AtomicLong();
    private final ExecutorService reader=Executors.newSingleThreadExecutor(r->new Thread(r,"r82-health-read"));
    private final Handler main=new Handler(Looper.getMainLooper());private CancellationSignal cancel;private Future<?> future;
    @Override protected void onCreate(Bundle state){super.onCreate(state);if(state!=null)custodyBackupR335=state.getString("custody_backup_r335");
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(20,16,20,16);
        TextView title=new TextView(this);title.setText("数据库健康与空间维护");title.setTextSize(20);root.addView(title);
        Button refresh=new Button(this);refresh.setText("刷新只读检查");root.addView(refresh);refresh.setOnClickListener(v->refresh());
        Button rebuild=new Button(this);rebuild.setText("停止运行并重建空间");root.addView(rebuild);rebuild.setOnClickListener(v->confirm(false));
        Button cleanup=new Button(this);cleanup.setText("单独清理已验证旧库备份");root.addView(cleanup);cleanup.setOnClickListener(v->confirm(true));
        Button legacy=new Button(this);legacy.setText("只读验证历史迁移备份可恢复性");root.addView(legacy);legacy.setOnClickListener(v->chooseLegacyR335());
        Button migrate=new Button(this);migrate.setText("迁出单份历史备份并独立验证（保留本地）");root.addView(migrate);migrate.setOnClickListener(v->chooseCustodyExportR335());
        Button manage=new Button(this);manage.setText("外部保全／重验／单份明确退役");root.addView(manage);manage.setOnClickListener(v->chooseCustodyCaseR335());
        Button restore=new Button(this);restore.setText("导入外部保全包到隔离恢复环境");root.addView(restore);restore.setOnClickListener(v->{Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/zip").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,CUSTODY_IMPORT_R335);});
        Button revoke=new Button(this);revoke.setText("撤销尚未提交的历史备份退役授权");root.addView(revoke);revoke.setOnClickListener(v->{LegacyBackupCustodyR335.revoke(null);reader.submit(()->{try{for(String id:LegacyBackupCustodyR335.listCases(getApplicationContext()))LegacyBackupCustodyR335.recordRevocation(getApplicationContext(),id);}catch(Exception failure){main.post(()->text.append("\n撤销已生效；耐久记录未完整："+DatabaseCoordinator.describeCauseChain(failure,800)));}});text.setText("已撤销当前进程中尚未进入最终提交的退役授权；已经完成的unlink不会回滚。原独立恢复证明不受影响。");});
        Button copy=new Button(this);copy.setText("复制检查内容");root.addView(copy);copy.setOnClickListener(v->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("数据库健康",text.getText()));});
        ScrollView scroll=new ScrollView(this);text=new TextView(this);text.setTextSize(14);text.setTextIsSelectable(true);scroll.addView(text);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);refresh();
    }
    private void chooseCustodyExportR335(){
        long token=generation.incrementAndGet();future=reader.submit(()->{try{String[] names=LegacyBackupRecoveryR335.listBackupNames(getApplicationContext());main.post(()->{if(generation.get()!=token)return;if(names.length==0){text.setText("没有可迁出的原r926备份。");return;}new AlertDialog.Builder(this).setTitle("选择唯一原备份").setItems(names,(dialog,index)->{custodyBackupR335=names[index];Intent pick=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(pick,CUSTODY_TREE_R335);}).show();});}catch(Exception e){showCustodyErrorR335(token,e);}});
    }
    private void chooseCustodyCaseR335(){
        long token=generation.incrementAndGet();future=reader.submit(()->{try{String[] ids=LegacyBackupCustodyR335.listCases(getApplicationContext());String[] labels=new String[ids.length];for(int i=0;i<ids.length;i++)labels[i]=LegacyBackupCustodyR335.describe(getApplicationContext(),ids[i]);main.post(()->{if(generation.get()!=token)return;if(ids.length==0){text.setText("没有已发布的保全case；半成品或异常case保留在维护摘要中。");return;}new AlertDialog.Builder(this).setTitle("选择精确保全case").setItems(labels,(dialog,index)->{String id=ids[index];new AlertDialog.Builder(this).setTitle("该case的独立操作").setItems(new String[]{"从外部再次完整回读并隔离恢复","审阅并明确退役这一份本地备份","撤销尚未提交的该case授权","单独释放已取回的私有隔离副本","只读核对中断的退役结果（不删除文件）"},(choice,which)->{try{if(which==0){StorageMaintenanceServiceR82.requestCustodyVerifyR335(this,id);refresh();}else if(which==1)reviewCustodyR335(id);else if(which==4){StorageMaintenanceServiceR82.requestCustodyReconcileR335(this,id);refresh();}else if(which==3){new AlertDialog.Builder(this).setTitle("只释放该case的已验证私有恢复副本").setMessage("会先完整回读外部保全包。仅移除明确登记的私有恢复树，不改当前生产库、原r926备份、manifest或外部归档；外部不可回读或进程仍活跃则保留副本。").setNegativeButton("保留",null).setPositiveButton("确认停止并释放私有副本",(x,y)->{try{StorageMaintenanceServiceR82.requestCustodyReleaseR335(this,id);refresh();}catch(RuntimeException e){text.setText(DatabaseCoordinator.describeCauseChain(e,1600));}}).show();}else{LegacyBackupCustodyR335.revoke(id);reader.submit(()->{try{LegacyBackupCustodyR335.recordRevocation(getApplicationContext(),id);}catch(Exception logging){main.post(()->text.append("\n撤销已在内存生效，但耐久记录失败："+DatabaseCoordinator.describeCauseChain(logging,800)));}});text.setText("已撤销尚未提交的该case授权；已进入最终提交的单文件操作以耐久结果为准。");}}catch(RuntimeException failure){text.setText(DatabaseCoordinator.describeCauseChain(failure,1600));}}).show();}).show();});}catch(Exception failure){showCustodyErrorR335(token,failure);}});
    }
    private void reviewCustodyR335(String id){long token=generation.incrementAndGet();text.setText("正在核对本次独立恢复的完整耐久证据；尚未授予删除权。");future=reader.submit(()->{try{LegacyBackupCustodyR335.Review review=LegacyBackupCustodyR335.review(getApplicationContext(),id);main.post(()->{if(generation.get()!=token)return;new AlertDialog.Builder(this).setTitle("单独确认退役这一份本地备份").setMessage(review.text()).setNegativeButton("保留本地",(d,w)->LegacyBackupCustodyR335.revoke(id)).setPositiveButton("确认停止并退役此一份",(d,w)->{try{String authorization=LegacyBackupCustodyR335.confirm(review);StorageMaintenanceServiceR82.requestCustodyRetireR335(getApplicationContext(),id,authorization);refresh();}catch(Exception failure){LegacyBackupCustodyR335.revoke(id);text.setText(DatabaseCoordinator.describeCauseChain(failure,1600));}}).show();});}catch(Exception failure){showCustodyErrorR335(token,failure);}});}
    private void showCustodyErrorR335(long token,Exception failure){main.post(()->{if(generation.get()==token)text.setText("保全操作未完成，未授予新的删除权："+DatabaseCoordinator.describeCauseChain(failure,1600));});}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=CUSTODY_TREE_R335&&request!=CUSTODY_IMPORT_R335)return;if(result!=RESULT_OK||data==null||data.getData()==null)return;
        try{android.net.Uri uri=data.getData();int grant=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);int needed=request==CUSTODY_TREE_R335?Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION:Intent.FLAG_GRANT_READ_URI_PERMISSION;if((grant&needed)!=needed)throw new IllegalStateException("保全目标未授予所需持久读写权限");getContentResolver().takePersistableUriPermission(uri,needed);
            String selected=custodyBackupR335;if(request==CUSTODY_TREE_R335)LegacyBackupRecoveryR335.requireName(selected);
            new AlertDialog.Builder(this).setTitle(request==CUSTODY_TREE_R335?"停止运行并迁出验证":"停止运行并隔离恢复外部包").setMessage("需要原预算准入和真实副本空间；外部写出不会提前删除本地备份。将完整回读并在独立进程运行原模型和引用消费者。导入仅恢复到新的私有隔离树，不覆盖当前生产库；不足空间、缺件或未知结果均保留来源。完成后需手动启动运行。").setNegativeButton("取消",null).setPositiveButton("确认停止并执行",(d,w)->{try{if(request==CUSTODY_TREE_R335)StorageMaintenanceServiceR82.requestCustodyExportR335(this,selected,uri);else StorageMaintenanceServiceR82.requestCustodyImportR335(this,uri);refresh();}catch(RuntimeException failure){text.setText(DatabaseCoordinator.describeCauseChain(failure,1600));}}).show();
        }catch(Exception failure){text.setText(DatabaseCoordinator.describeCauseChain(failure,1600));}
    }
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putString("custody_backup_r335",custodyBackupR335);}
    private void chooseLegacyR335(){
        long token=generation.incrementAndGet();if(cancel!=null)cancel.cancel();if(future!=null)future.cancel(true);
        text.setText("正在读取历史备份文件名；尚未停止运行。");
        future=reader.submit(()->{try{final String[] names=LegacyBackupRecoveryR335.listBackupNames(getApplicationContext());
            main.post(()->{if(generation.get()!=token)return;if(names.length==0){text.setText("未找到符合原命名规则的 r926 迁移备份；未删除或生成任何备份。");return;}
                new AlertDialog.Builder(this).setTitle("选择要验证的原备份").setItems(names,(d,index)->
                    new AlertDialog.Builder(this).setTitle("停止运行并验证备份").setMessage("将保留原备份和当前数据库，在独立副本中执行真实启动、模型解码和引用闭包检查。需要副本空间；缺少原模型文件时保留为待恢复，不会伪造成功。完成后需手动启动运行。此操作不会授予删除历史备份的权限。")
                    .setNegativeButton("取消",null).setPositiveButton("确认停止并验证",(confirm,which)->{try{StorageMaintenanceServiceR82.requestLegacyBackupProofR335(getApplicationContext(),names[index]);refresh();}catch(RuntimeException failure){text.setText(DatabaseCoordinator.describeCauseChain(failure,1600));}}).show()).show();});
        }catch(Exception failure){main.post(()->{if(generation.get()==token)text.setText("历史备份目录读取未完成："+DatabaseCoordinator.describeCauseChain(failure,1200));});}});
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
