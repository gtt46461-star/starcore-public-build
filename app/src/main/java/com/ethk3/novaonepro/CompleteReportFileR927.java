package com.ethk3.novaonepro;
import android.app.Activity;import android.app.AlertDialog;import android.app.Fragment;import android.os.Bundle;import android.content.*;import android.net.Uri;import android.widget.*;import java.io.*;import java.lang.ref.WeakReference;import java.util.*;import java.util.concurrent.*;import java.util.concurrent.atomic.AtomicBoolean;import org.json.*;
/** UI adapter only. ReportExportServiceCP4 owns snapshot creation, evaluation, publication and recovery. */
public final class CompleteReportFileR927 {
 private static final AtomicBoolean RUNNING=new AtomicBoolean();private static final ScheduledExecutorService UI_IO=Executors.newSingleThreadScheduledExecutor(r->new Thread(r,"report-ui-state-cp12"));
 private static final java.util.concurrent.atomic.AtomicLong POLL_SESSION=new java.util.concurrent.atomic.AtomicLong();
 private CompleteReportFileR927(){}
 public static void openExportMenu(Activity activity,Button button,TextView status){if(activity==null||activity.isFinishing()||activity.isDestroyed())return;
  new AlertDialog.Builder(activity).setTitle("完整检测报告").setItems(new String[]{"清理旧报告并生成当前完整报告","取回上次报告或继续生成","查看上次导出失败详情","取消未完成的完整报告","另存完整原始备份（体积大）","停止任务并删除旧报告缓存"},(dialog,which)->{
   if(which==0){controlReports(activity,button,status,true,true);return;}if(which==5){controlReports(activity,button,status,true,false);return;}if(which==4){export(activity,button,status,true);return;}if(which==2){showLastFailure(activity);return;}if(which==3){cancelLatest(activity,button,status);return;}recover(activity,button,status);
  }).show();
 }
 private static void cancelLatest(Activity activity,Button button,TextView status){controlReports(activity,button,status,false,false);}
 private static void controlReports(Activity activity,Button button,TextView status,boolean clear,boolean restart){
  final long session=POLL_SESSION.incrementAndGet();RUNNING.set(false);
  Context app=activity.getApplicationContext();WeakReference<Activity> weak=new WeakReference<>(activity);WeakReference<Button>b=new WeakReference<>(button);WeakReference<TextView>s=new WeakReference<>(status);
  show(activity,button,status,clear?"正在停止旧任务并清理报告缓存；开奖数据和学习成果保留。":"正在停止报告生成和保存任务。",true);
  UI_IO.execute(()->{try{String command=ReportExportServiceCP4.requestReportControl(app,clear);pollControl(app,command,weak,b,s,session,restart,0);}
   catch(Exception error){Activity current=weak.get();if(current!=null)current.runOnUiThread(()->{if(session==POLL_SESSION.get())show(current,b.get(),s.get(),"报告操作未完成："+error.getMessage(),true);});}});
 }
 private static void pollControl(Context app,String command,WeakReference<Activity>a,WeakReference<Button>b,WeakReference<TextView>s,long session,boolean restart,int attempts){
  UI_IO.schedule(()->{if(session!=POLL_SESSION.get())return;try{
   JSONObject state=ReportTaskControlR69.status(app.getFilesDir(),command);String stage=state.getString("stage");Activity current=a.get();
   if(stage.equals("CONTROL_FAILED"))throw new IOException(state.optString("reason"));
   if(stage.equals("CONTROL_DONE")){RUNNING.set(false);if(current!=null)current.runOnUiThread(()->{if(session!=POLL_SESSION.get())return;if(restart)export(current,b.get(),s.get(),false);else show(current,b.get(),s.get(),state.optBoolean("clear")?"旧报告任务与缓存已清理，可以重新生成完整报告。":"报告任务已停止，可以重新生成。",true);});return;}
   String message=stage.equals("WAITING_FOR_READERS")?"任务已停止；旧报告仍被文件预览或分享读取，释放后自动完成清理。":"正在等待旧任务关闭文件并清理；未宣称已经完成。";
   if(current!=null)current.runOnUiThread(()->{if(session==POLL_SESSION.get())show(current,b.get(),s.get(),message,true);});
   if(attempts<600)pollControl(app,command,a,b,s,session,restart,attempts+1);
  }catch(Exception error){RUNNING.set(false);Activity current=a.get();if(current!=null)current.runOnUiThread(()->{if(session==POLL_SESSION.get())show(current,b.get(),s.get(),"报告操作未完成："+error.getMessage(),true);});}},500,TimeUnit.MILLISECONDS);
 }
 private static void recover(Activity activity,Button button,TextView status){final Context app=activity.getApplicationContext();WeakReference<Activity> weak=new WeakReference<>(activity);WeakReference<Button>b=new WeakReference<>(button);WeakReference<TextView>s=new WeakReference<>(status);
  // Reattach to the durable owner after Activity recreation; an old UI poll is not an execution lease.
  final long session=POLL_SESSION.incrementAndGet();RUNNING.set(true);show(activity,button,status,"正在取回上次报告。",false);
  UI_IO.execute(()->{try{File root=ReportDirectoriesCP12.root(app.getFilesDir());ReportJobJournalCP12.Job job=ReportJobJournalCP12.latestFullReport(root);if(job==null)throw new IOException("没有可取回的完整报告，请生成当前报告。");String id=job.jobId;
   if(!job.mode.equals("FULL_REPORT"))throw new IOException("上次任务是历史归档，请生成当前完整报告。");
   if(!job.stage.equals("FAILED")&&!job.stage.equals("CANCELLED"))ReportExportServiceCP4.resumeExport(app,id);
   poll(app,job.requestId,weak,b,s,0,session);
  }catch(Exception error){RUNNING.set(false);Activity current=weak.get();if(current!=null)current.runOnUiThread(()->show(current,b.get(),s.get(),"上次报告无法取回："+error.getMessage(),true));}});
 }
 private static void showLastFailure(Activity activity){final Context app=activity.getApplicationContext();WeakReference<Activity> weak=new WeakReference<>(activity);
  UI_IO.execute(()->{String detail;try{File root=ReportDirectoriesCP12.root(app.getFilesDir()),directory=new File(root,"failures");File[] files=directory.listFiles();File latest=null;
   if(files!=null)for(File file:files)if(file.getName().matches("[0-9a-f]{64}\\.txt")&&(latest==null||file.lastModified()>latest.lastModified()))latest=file;
   detail=latest==null?"没有保存的导出失败详情。":ReportJobJournalCP12.readAtomic(latest,1024*1024);
  }catch(Exception error){detail="失败详情无法读取："+error.getMessage();}final String text=detail;Activity current=weak.get();if(current!=null)current.runOnUiThread(()->{if(current.isFinishing()||current.isDestroyed())return;
   TextView content=new TextView(current);content.setText(text);content.setTextIsSelectable(true);content.setPadding(24,24,24,24);ScrollView scroll=new ScrollView(current);scroll.addView(content);
   new AlertDialog.Builder(current).setTitle("导出失败详情").setView(scroll).setPositiveButton("复制详情",(d,w)->{ClipboardManager clipboard=(ClipboardManager)current.getSystemService(Context.CLIPBOARD_SERVICE);if(clipboard!=null)clipboard.setPrimaryClip(ClipData.newPlainText("STARCORE 导出错误",text));}).setNegativeButton("关闭",null).show();});});
 }

 public static void export(Activity activity){export(activity,null,null);}
 public static void export(Activity activity,Button button,TextView status){export(activity,button,status,false);}
 private static void export(Activity activity,Button button,TextView status,boolean raw){if(activity==null||activity.isFinishing()||activity.isDestroyed())return;if(!RUNNING.compareAndSet(false,true)){controlReports(activity,button,status,false,true);return;}
  final long session=POLL_SESSION.incrementAndGet();final Context app=activity.getApplicationContext();final String request;try{request=ReportExportServiceCP4.requestExport(app,ReportExportServiceCP4.ExportMode.FULL_REPORT,raw);}catch(RuntimeException error){RUNNING.set(false);show(activity,button,status,"完整报告请求失败："+error.getMessage(),true);return;}WeakReference<Activity> weak=new WeakReference<>(activity);WeakReference<Button> b=new WeakReference<>(button);WeakReference<TextView> s=new WeakReference<>(status);show(activity,button,status,raw?"原始备份已排队；包含数据库和二进制，体积可能很大。":"完整检测已排队；一个ZIP保留全部检测结果、运行证据、算法训练状态、文件状态和源码调用索引。",false);poll(app,request,weak,b,s,0,session);
 }
 private static void poll(Context app,String request,WeakReference<Activity> a,WeakReference<Button>b,WeakReference<TextView>s,int attempt,long session){UI_IO.schedule(()->{if(session!=POLL_SESSION.get())return;Activity activity=a.get();try{
   File root=ReportDirectoriesCP12.root(app.getFilesDir());File ack=new File(root,"requests/"+request+".ack.json");JSONObject authority=ReportJobJournalCP12.authoritativeStateR31(root,request);if(!ack.exists()&&authority==null){
    if(attempt%8==0&&new File(root,"requests/"+request+".json").isFile())try{ReportExportServiceCP4.resumeExport(app,request);}catch(RuntimeException pending){android.util.Log.w("CompleteReport","Persisted request waiting for foreground service",pending);}
String failure=ReportRequestStatusR31.failure(request);if(failure!=null)throw new IOException("REPORT_REQUEST_FAILED_BEFORE_DURABLE_ACK: "+failure);if(attempt>0&&attempt%8==0&&activity!=null){final long waited=attempt*750L/1000L;activity.runOnUiThread(()->{if(session==POLL_SESSION.get())show(activity,b.get(),s.get(),"正在采集报告输入或等待导出服务确认（已等待 "+waited+" 秒，尚未取得持久回执）。若超过1分钟仍无进展，请点“快速诊断”复制结果发送。",true);});}if(attempt<1200){poll(app,request,a,b,s,attempt+1,session);return;}throw new IOException("REPORT_REQUEST_ACK_TIMEOUT");}
   JSONObject value=authority!=null?authority:new JSONObject(ReportJobJournalCP12.readAtomic(ack,4096));String stage=value.getString("stage"),reason=value.optString("reason","");
   if(stage.equals("READY_TO_SHARE")){String id=value.getString("jobId");File job=ReportPublicationCP12.job(app.getFilesDir(),id);JSONObject index=ReportPublicationCP12.delivery(job);ReportPublicationCP12.grant(job,System.currentTimeMillis());ArrayList<Uri> volumes=new ArrayList<>();JSONArray files=index.getJSONArray("files");boolean single="SCREPORT_SINGLE_ZIP_R992".equals(index.optString("format"));boolean r30Parts=ReportPublicationCP12.ZIP_PART_FORMAT_R30.equals(index.optString("format"));if(!single)volumes.add(uri(app,id,"index.json"));for(int i=0;i<files.length();i++)volumes.add(uri(app,id,files.getJSONObject(i).getString("name")));boolean healthy=index.getBoolean("allChecksPassed");RUNNING.set(false);if(activity!=null)activity.runOnUiThread(()->{show(activity,b.get(),s.get(),"完整报告已生成｜"+files.length()+(r30Parts?"个ZIP分卷（按索引合并）｜":"个完整文件｜")+String.format(Locale.ROOT,"%.2f MB",index.optLong("totalVolumeBytes")/(1024.0*1024.0))+"｜证据完整="+index.optBoolean("evidenceComplete")+"｜检查执行完整="+index.optBoolean("checksComplete")+"｜全部健康检查通过="+healthy,true);Intent share=new Intent(Intent.ACTION_SEND_MULTIPLE).setType(single?"application/zip":"application/octet-stream").putParcelableArrayListExtra(Intent.EXTRA_STREAM,volumes).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);ClipData clip=ClipData.newRawUri("STARCORE report",volumes.get(0));for(int i=1;i<volumes.size();i++)clip.addItem(new ClipData.Item(volumes.get(i)));share.setClipData(clip);if(activity.isFinishing()||activity.isDestroyed())return;new AlertDialog.Builder(activity).setTitle("完整检测报告交付").setItems(single?new String[]{"一次保存完整ZIP文件","分享完整ZIP","保存到文件夹"}:new String[]{"一次保存完整报告到文件夹","分享全部分卷","保存一个分卷到文件"},(dialog,which)->{try{if(which==0)chooseSafDestination(activity,id,single?"STARCORE_FULL_REPORT.zip":"*");else if(which==1)activity.startActivity(Intent.createChooser(share,"分享完整检测报告"));else if(single)chooseSafDestination(activity,id,"*");else chooseSafVolume(activity,id,volumes);}catch(Exception unavailable){show(activity,b.get(),s.get(),"报告已生成；交付入口不可用："+unavailable.getMessage(),true);}}).show();});return;}
   if(stage.equals("FAILED")&&value.has("recovery")){JSONObject recovery=value.getJSONObject("recovery");String sha=recovery.getString("sha256");if(!sha.matches("[0-9a-f]{64}"))throw new IOException("REPORT_RECOVERY_HASH");Uri uri=new Uri.Builder().scheme("content").authority(app.getPackageName()+".diagnostic.reports").appendPath("recovery").appendPath(request).appendPath(sha+".zip").build();RUNNING.set(false);if(activity!=null)activity.runOnUiThread(()->{show(activity,b.get(),s.get(),"完整报告失败："+reason+"。恢复诊断可导出；它不包含完整数据库或全部检测结果。",true);if(activity.isFinishing()||activity.isDestroyed())return;new AlertDialog.Builder(activity).setTitle("导出失败的恢复诊断").setMessage("证据范围：原始异常和可取得的请求诊断。完整检测尚未完成。\n原因："+reason).setPositiveButton("分享恢复诊断",(dialog,which)->{Intent share=new Intent(Intent.ACTION_SEND).setType("application/zip").putExtra(Intent.EXTRA_STREAM,uri).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);share.setClipData(ClipData.newRawUri("STARCORE recovery",uri));try{activity.startActivity(Intent.createChooser(share,"分享导出失败恢复诊断"));}catch(Exception unavailable){show(activity,b.get(),s.get(),"恢复诊断分享失败："+unavailable.getMessage(),true);}}).setNegativeButton("关闭",null).show();});return;}
   if(stage.equals("FAILED")||stage.equals("CANCELLED"))throw new IOException(stage+":"+reason);
   String progressText=value.has("jobId")?stageProgressText(app,value.getString("jobId")):"";
   final String pendingText="检测报告状态="+stage+progressText+(reason.isEmpty()?"":"｜原因="+reason)+"｜可点击取回或取消";
   if(activity!=null)activity.runOnUiThread(()->show(activity,b.get(),s.get(),pendingText,true));if(attempt<1200)poll(app,request,a,b,s,attempt+1,session);else{RUNNING.set(false);if(activity!=null)activity.runOnUiThread(()->show(activity,b.get(),s.get(),"后台任务仍保留，请稍后查看；任务不会因界面停止而取消。",true));}
  }catch(Exception error){RUNNING.set(false);if(activity!=null)activity.runOnUiThread(()->show(activity,b.get(),s.get(),"完整报告未交付："+error.getMessage(),true));}},750,TimeUnit.MILLISECONDS);}
 static String stageProgressText(Context app,String id)throws Exception{
  if(!id.matches("[0-9a-f]{64}"))throw new IOException("REPORT_PROGRESS_JOB_ID");
  File file=ReportArchiveVerifierCP12.inside(ReportDirectoriesCP12.root(app.getFilesDir()),"jobs/"+id+"/stage-progress.json");
  if(!file.isFile())return backupProgressText(app,id);
  JSONObject p=new JSONObject(ReportJobJournalCP12.readAtomic(file,4096));String stage=p.getString("stage");
  return "｜当前步骤="+stage+"｜本步骤耗时 "+Math.max(0,(System.currentTimeMillis()-p.getLong("startedWallMs"))/1000)+" 秒"+(stage.equals("BACKUP")?backupProgressText(app,id):"");
 }
 private static Uri uri(Context app,String id,String name){return new Uri.Builder().scheme("content").authority(app.getPackageName()+".diagnostic.reports").appendPath("r981").appendPath(id).appendPath(name).build();}
 static String backupProgressText(Context app,String id)throws Exception{if(!id.matches("[0-9a-f]{64}"))throw new IOException("REPORT_PROGRESS_JOB_ID");File file=ReportArchiveVerifierCP12.inside(ReportDirectoriesCP12.root(app.getFilesDir()),"jobs/"+id+"/backup-progress.json");if(!file.isFile())return "";JSONObject progress=new JSONObject(ReportJobJournalCP12.readAtomic(file,4096));return "｜数据库快照块（不是报告页数） "+progress.optLong("copiedPages")+"/"+progress.optLong("totalPages")+"｜耗时 "+(progress.optLong("elapsedMs")/1000)+" 秒";}
 private static void show(Activity activity,Button button,TextView status,String message,boolean enabled){if(activity.isFinishing()||activity.isDestroyed())return;if(button!=null)button.setEnabled(true);if(status!=null)status.setText(message);else Toast.makeText(activity,message,Toast.LENGTH_LONG).show();}
 static String fileHash(File file)throws Exception{return ReportArchiveCP4.hash(file,()->true);}
 static void writePayload(DbStore store,Writer out)throws Exception{throw new IOException("REPORT_EXPORT_OWNER_SERVICE_ONLY");}
 static void writePayload(DbStore store,Writer out,android.content.res.AssetManager assets)throws Exception{throw new IOException("REPORT_EXPORT_OWNER_SERVICE_ONLY");}

 /** Actual SAF picker caller. Android Fragment dispatches its result independently of MainActivity overrides. */
 private static void chooseSafVolume(Activity activity,String jobId,ArrayList<Uri> volumes){String[] names=new String[volumes.size()];for(int i=0;i<names.length;i++)names[i]=volumes.get(i).getLastPathSegment();new AlertDialog.Builder(activity).setTitle("选择保存分卷（全部卷共同组成完整报告）").setItems(names,(d,index)->chooseSafDestination(activity,jobId,names[index])).show();}
 private static void chooseSafDestination(Activity activity,String jobId,String volumeName){String tag="report-saf-picker-cp12";if(activity.getFragmentManager().findFragmentByTag(tag)!=null){Toast.makeText(activity,"文件选择仍在进行。",Toast.LENGTH_SHORT).show();return;}SafVolumePicker picker=new SafVolumePicker();Bundle args=new Bundle();args.putString("jobId",jobId);args.putString("volumeName",volumeName);picker.setArguments(args);activity.getFragmentManager().beginTransaction().add(picker,tag).commit();}
 public static final class SafVolumePicker extends Fragment{
  private boolean launched;public SafVolumePicker(){}
  @Override public void onCreate(Bundle state){super.onCreate(state);launched=state!=null&&state.getBoolean("launched",false);}
  @Override public void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("launched",launched);}
  @Override public void onResume(){super.onResume();if(launched)return;launched=true;boolean all="*".equals(getArguments().getString("volumeName"));Intent picker=all?new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE):new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType(getArguments().getString("volumeName").endsWith(".zip")?"application/zip":"application/octet-stream").putExtra(Intent.EXTRA_TITLE,getArguments().getString("volumeName"));picker.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);try{startActivityForResult(picker,981);}catch(Exception unavailable){Toast.makeText(getActivity(),"文件选择不可用："+unavailable.getMessage(),Toast.LENGTH_LONG).show();remove();}}
  @Override public void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request!=981)return;Activity activity=getActivity();try{if(result==Activity.RESULT_OK&&data!=null&&data.getData()!=null){Uri uri=data.getData();int flags=data.getFlags()&(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);if(flags!=(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))throw new IOException("SAF_URI_READ_WRITE_GRANT_REQUIRED");if((data.getFlags()&Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)!=0){if(flags==(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION))activity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);else if(flags==Intent.FLAG_GRANT_WRITE_URI_PERMISSION)activity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_WRITE_URI_PERMISSION);else if(flags==Intent.FLAG_GRANT_READ_URI_PERMISSION)activity.getContentResolver().takePersistableUriPermission(uri,Intent.FLAG_GRANT_READ_URI_PERMISSION);else throw new IOException("SAF_URI_GRANT_EMPTY");}String id=ReportExportServiceCP4.requestSafTransfer(activity,getArguments().getString("jobId"),getArguments().getString("volumeName"),uri);Toast.makeText(activity,"报告保存已排队；每个文件保存后都会回读核验。",Toast.LENGTH_LONG).show();pollSaf(activity.getApplicationContext(),id,new WeakReference<>(activity),0);}else if(activity!=null)Toast.makeText(activity,"已取消文件选择；报告尚未保存。",Toast.LENGTH_LONG).show();}catch(Exception failure){if(activity!=null)Toast.makeText(activity,"保存请求失败："+failure.getMessage(),Toast.LENGTH_LONG).show();}finally{remove();}}
  private void remove(){if(isAdded())getFragmentManager().beginTransaction().remove(this).commitAllowingStateLoss();}
 }
 private static void pollSaf(Context app,String id,WeakReference<Activity> weak,int attempts){UI_IO.schedule(()->{try{File ack=new File(app.getFilesDir(),"r981_reports/requests/"+id+".ack.json");if(!ack.exists()){if(attempts<1200){pollSaf(app,id,weak,attempts+1);return;}throw new IOException("REPORT_SAF_ACK_TIMEOUT|后台任务可能仍在运行，尚未取得保存回读凭据");}JSONObject state=new JSONObject(ReportJobJournalCP12.readAtomic(ack,8192));String stage=state.optString("state",state.optString("stage",""));if(stage.equals("VERIFIED_READBACK")||stage.equals("FAILED")||stage.equals("CANCELLED")){Activity activity=weak.get();if(activity!=null)activity.runOnUiThread(()->show(activity,null,null,stage.equals("VERIFIED_READBACK")?state.optBoolean("allVolumesSaved")?"完整报告已全部保存并核验。":"分卷已保存并核验。":"分卷保存失败："+state.optString("reason",stage),true));}else if(attempts<1200)pollSaf(app,id,weak,attempts+1);else throw new IOException("REPORT_SAF_PENDING_TIMEOUT|state="+stage+"|后台任务仍保留，尚未完成保存回读核验");}catch(Exception error){Activity activity=weak.get();if(activity!=null)activity.runOnUiThread(()->show(activity,null,null,"保存状态读取失败："+error.getMessage(),true));}},750,TimeUnit.MILLISECONDS);}

 /**
  * R31Q quick diagnostic. It does not use the report export service, the diagnostics process, the
  * report job lock or any SQLite query, so it still works when the business loop or the export is
  * stalled. Every section has its own time limit; a section that does not return records the
  * frame its reader thread is blocked in, which is itself the evidence of the stall.
  */
 public static final class QuickDiagnosticR31Q {
     private static final long SECTION_TIMEOUT_MS = 3000L;
     private static final int SHARE_LIMIT_CHARS = 300_000;
     private static final AtomicBoolean RUNNING = new AtomicBoolean();
     private static final ExecutorService READERS =
             new ThreadPoolExecutor(0,3,30L,TimeUnit.SECONDS,new SynchronousQueue<Runnable>(),
                     r -> {
                         Thread t = new Thread(r, "quick-diagnostic-r31q");
                         t.setDaemon(true);
                         return t;
                     },new ThreadPoolExecutor.AbortPolicy());

     private QuickDiagnosticR31Q() {}

     public static void show(final Activity activity) {
         if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
         if (!RUNNING.compareAndSet(false, true)) {
             Toast.makeText(activity, "快速诊断正在采集，请稍候", Toast.LENGTH_SHORT).show();
             return;
         }
         Toast.makeText(activity, "正在采集快速诊断（约10秒）", Toast.LENGTH_SHORT).show();
         final Context app = activity.getApplicationContext();
         final java.lang.ref.WeakReference<Activity> weak = new java.lang.ref.WeakReference<>(activity);
         Thread worker =
                 new Thread(
                         () -> {
                             String text;
                             try {
                                 text = collect(app);
                             } catch (Throwable error) {
                                 text = "QUICK_DIAGNOSTIC_FAILED|" + error;
                             } finally {
                                 RUNNING.set(false);
                             }
                             final String result = text;
                             Activity current = weak.get();
                             if (current == null) return;
                             current.runOnUiThread(() -> present(current, result));
                         },
                         "quick-diagnostic-r31q-owner");
         worker.setDaemon(true);
         worker.start();
     }

     static String collect(final Context app) {
         StringBuilder b = new StringBuilder(64 * 1024);
         b.append("STARCORE 快速诊断 R31Q\n");
         b.append("time=")
                 .append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.ROOT).format(new Date()))
                 .append("|uptimeMs=")
                 .append(android.os.SystemClock.elapsedRealtime())
                 .append("|pid=")
                 .append(android.os.Process.myPid())
                 .append('\n');
         section(b, "build", () -> {
             android.content.pm.PackageInfo info = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
             return app.getPackageName() + "|versionName=" + info.versionName + "|manifestVersionCode=" + ReleaseManifestR926.VERSION_CODE
                     + "|device=" + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL + "|android=" + android.os.Build.VERSION.RELEASE
                     + "|sdk=" + android.os.Build.VERSION.SDK_INT;
         });
         section(b, "heartbeatTruth", () -> new ServiceHeartbeatStore(app).runtimeTruthSummary());
         section(b, "heartbeatDetail", () -> new ServiceHeartbeatStore(app).detailedSummary());
         section(b, "fetchCadence", () -> new ServiceHeartbeatStore(app).fetchCadenceJsonR31());
         section(b, "schemaStartup", () -> {
             DatabaseCoordinator c = DatabaseCoordinator.peek();
             if (c == null) return "NO_COORDINATOR_IN_THIS_PROCESS";
             return c.startupSchemaSummary() + "|legacyRunningMs=" + c.legacyMigrationRunningMsR31Q() + "|legacyDdlHandle=" + DatabaseCoordinator.legacyDdlHandleStateR31Q() + "|dbLockWaitMs=" + c.lastDbLockWaitMs()
                     + "|dbLockWaitMaxMs=" + c.longestDbLockWaitMs()
                     + "|explicitMaintenance=" + DatabaseCoordinator.isExplicitMaintenanceActiveR82();
         });
         section(b, "storageMaintenance", () -> StorageMaintenanceServiceR82.summary(app));
         section(b, "storage", () -> {
             File db = app.getDatabasePath(AppConfig.DB_NAME);
             File wal = new File(db.getPath() + "-wal");
             android.os.StatFs fs = new android.os.StatFs(app.getFilesDir().getPath());
             return "dbBytes=" + (db.isFile() ? db.length() : -1) + "|walBytes=" + (wal.isFile() ? wal.length() : -1)
                     + "|freeBytes=" + fs.getAvailableBytes() + "|totalBytes=" + fs.getTotalBytes();
         });
         section(b, "processes", () -> {
             android.app.ActivityManager am = (android.app.ActivityManager) app.getSystemService(Context.ACTIVITY_SERVICE);
             List<android.app.ActivityManager.RunningAppProcessInfo> all = am == null ? null : am.getRunningAppProcesses();
             if (all == null) return "UNAVAILABLE";
             StringBuilder p = new StringBuilder();
             for (android.app.ActivityManager.RunningAppProcessInfo i : all) {
                 if (i.processName == null || !i.processName.startsWith(app.getPackageName())) continue;
                 p.append(i.processName).append(" pid=").append(i.pid).append(" importance=").append(i.importance).append('\n');
             }
             return p.length() == 0 ? "NONE_VISIBLE" : p.toString();
         });
         section(b, "reportRequestQueue", () -> ReportExportServiceCP4.requestQueueStateR31Q() + "\nlastRequestOutcome=" + ReportExportServiceCP4.lastRequestOutcomeR31Q());
         section(b, "reportFiles", () -> reportFiles(app));
         section(b, "crash", () -> CrashRecorder.detailedSummary(app));
         section(b, "threadStacks", ReportExportServiceCP4::threadStacksR27);
         return b.toString();
     }

     private static String reportFiles(Context app) throws Exception {
         File root = ReportDirectoriesCP12.root(app.getFilesDir());
         StringBuilder r = new StringBuilder();
         r.append("root=").append(root.getName()).append('\n');
         File current = new File(root, "current-job.json");
         r.append("current-job.json=");
         if (current.isFile()) r.append(ReportJobJournalCP12.readAtomic(current, 4096));
         else r.append("ABSENT");
         r.append('\n');
         File lock = new File(root, "job.lock");
         r.append("job.lock exists=").append(lock.isFile()).append(" mtime=").append(lock.lastModified()).append('\n');
         for (String name : new String[] {"request-queue.json", "interactive-pending-r69.json"}) {
             File f = new File(root, name);
             r.append(name).append('=').append(f.isFile() ? ReportJobJournalCP12.readAtomic(f, 16384) : "ABSENT").append('\n');
         }
         File[] top = root.listFiles();
         r.append("root/ count=").append(top == null ? -1 : top.length).append('\n');
         if (top != null) {
             Arrays.sort(top, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
             for (int n = 0; n < top.length && n < 40; n++) {
                 r.append("  ").append(top[n].getName()).append(top[n].isDirectory() ? "/" : "").append(" bytes=").append(top[n].length()).append(" mtime=").append(top[n].lastModified()).append('\n');
                 String nm = top[n].getName();
                 if (top[n].isFile() && top[n].length() <= 4096 && (nm.startsWith("status-") || nm.startsWith("command-")) && n < 8)
                     r.append("    ").append(ReportJobJournalCP12.readAtomic(top[n], 4096)).append('\n');
             }
         }
         File[] acks = new File(root, "requests").listFiles();
         if (acks != null) {
             Arrays.sort(acks, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
             for (int n = 0, shown = 0; n < acks.length && shown < 3; n++) if (acks[n].getName().endsWith(".ack.json")) { shown++; r.append("ack ").append(acks[n].getName(), 0, 12).append('=').append(ReportJobJournalCP12.readAtomic(acks[n], 4096)).append('\n'); }
         }
         appendNewest(r, new File(root, "requests"), 20);
         appendNewest(r, new File(root, "failures"), 5);
         appendNewest(r, new File(root, "jobs"), 5);
         return r.toString();
     }

     private static void appendNewest(StringBuilder r, File dir, int limit) {
         File[] files = dir.listFiles();
         r.append(dir.getName()).append("/ count=").append(files == null ? -1 : files.length).append('\n');
         if (files == null) return;
         Arrays.sort(files, (x, y) -> Long.compare(y.lastModified(), x.lastModified()));
         for (int i = 0; i < files.length && i < limit; i++) {
             r.append("  ").append(files[i].getName()).append(" bytes=").append(files[i].length())
                     .append(" mtime=").append(files[i].lastModified()).append('\n');
         }
     }

     /** Runs one reader with a time limit. A reader that does not return leaves its blocked frames. */
     private static void section(StringBuilder b, String name, Callable<String> reader) {
         b.append("\n===== ").append(name).append(" =====\n");
         final java.util.concurrent.atomic.AtomicReference<Thread> owner = new java.util.concurrent.atomic.AtomicReference<>();
         long started = android.os.SystemClock.elapsedRealtime();
         Future<String> future;
         try {
             future = READERS.submit(() -> {
                 owner.set(Thread.currentThread());
                 return reader.call();
             });
         } catch (Throwable rejected) {
             b.append("SECTION_NOT_STARTED|").append(rejected).append('\n');
             return;
         }
         try {
             String value = future.get(SECTION_TIMEOUT_MS, TimeUnit.MILLISECONDS);
             b.append(value == null ? "null" : value).append('\n');
         } catch (TimeoutException blocked) {
             b.append("SECTION_TIMEOUT_AFTER_MS=").append(android.os.SystemClock.elapsedRealtime() - started).append('\n');
             Thread t = owner.get();
             if (t != null) {
                 b.append("blocked reader ").append(t.getName()).append(' ').append(t.getState()).append('\n');
                 StackTraceElement[] stack = t.getStackTrace();
                 for (int i = 0; i < stack.length && i < 40; i++) b.append("  at ").append(stack[i]).append('\n');
             }
             future.cancel(true);
         } catch (java.util.concurrent.ExecutionException failed) {
             Throwable cause = failed.getCause() == null ? failed : failed.getCause();
             b.append("SECTION_ERROR|").append(cause).append('\n');
             StackTraceElement[] stack = cause.getStackTrace();
             for (int i = 0; i < stack.length && i < 12; i++) b.append("  at ").append(stack[i]).append('\n');
         } catch (InterruptedException interrupted) {
             Thread.currentThread().interrupt();
             b.append("SECTION_INTERRUPTED\n");
         }
     }

     private static void present(final Activity activity, final String text) {
         if (activity.isFinishing() || activity.isDestroyed()) return;
         final String bounded =
                 text.length() <= SHARE_LIMIT_CHARS
                         ? text
                         : text.substring(0, SHARE_LIMIT_CHARS) + "\nTRUNCATED_FOR_SHARE|total=" + text.length();
         TextView content = new TextView(activity);
         content.setTextIsSelectable(true);
         content.setTextSize(11f);
         content.setPadding(24, 24, 24, 24);
         content.setText(bounded.length() > 60_000 ? bounded.substring(0, 60_000) + "\n…（完整内容请用复制或分享）" : bounded);
         ScrollView scroll = new ScrollView(activity);
         scroll.addView(content);
         new AlertDialog.Builder(activity)
                 .setTitle("快速诊断（不经过导出服务）")
                 .setView(scroll)
                 .setPositiveButton(
                         "复制全部",
                         (d, w) -> {
                             ClipboardManager clipboard =
                                     (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                             if (clipboard != null) {
                                 clipboard.setPrimaryClip(ClipData.newPlainText("STARCORE 快速诊断", bounded));
                                 Toast.makeText(activity, "已复制，可直接粘贴发送", Toast.LENGTH_LONG).show();
                             }
                         })
                 .setNeutralButton(
                         "分享",
                         (d, w) -> {
                             try {
                                 Intent share = new Intent(Intent.ACTION_SEND).setType("text/plain")
                                         .putExtra(Intent.EXTRA_SUBJECT, "STARCORE 快速诊断")
                                         .putExtra(Intent.EXTRA_TEXT, bounded);
                                 activity.startActivity(Intent.createChooser(share, "分享快速诊断"));
                             } catch (Exception unavailable) {
                                 Toast.makeText(activity, "分享失败：" + unavailable.getMessage(), Toast.LENGTH_LONG).show();
                             }
                         })
                 .setNegativeButton("关闭", null)
                 .show();
     }
 }
}
