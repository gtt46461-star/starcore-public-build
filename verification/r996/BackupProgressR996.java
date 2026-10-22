package com.ethk3.novaonepro;
import java.io.*;import java.lang.reflect.*;import java.util.concurrent.atomic.AtomicBoolean;
import android.content.Context;import android.content.ContextWrapper;import android.app.Activity;import android.widget.Button;
/** Actual native callback → service atomic progress → pending-job UI; no published-job shortcut. */
public final class BackupProgressR996 {
 static final class Ctx extends ContextWrapper {File files;Ctx(){super(null);}@Override public File getFilesDir(){return files;}}
 static final class Ui extends Activity {@Override public boolean isFinishing(){return false;}@Override public boolean isDestroyed(){return false;}}
 static final class Btn extends Button {boolean enabled;Btn(){super(null);}@Override public void setEnabled(boolean value){enabled=value;}}
 public static void main(String[] a)throws Exception{
  File root=new File(a[1]);root.mkdirs();String id=String.format("%064x",996);File reports=new File(root,"r981_reports"),jobDir=new File(reports,"jobs/"+id);jobDir.mkdirs();
  ReportExportServiceCP4 service=(ReportExportServiceCP4)UiContinuityR996.U.allocateInstance(ReportExportServiceCP4.class);
  ReportJobJournalCP12.Job job=new ReportJobJournalCP12.Job(id,id,ReleaseManifestR926.MANIFEST_SHA256,"FULL_REPORT",System.currentTimeMillis());job.stage="BACKUP";
  Class<?> type=Class.forName("com.ethk3.novaonepro.ReportExportServiceCP4$ActiveControl");Constructor<?> c=type.getDeclaredConstructor(ReportExportServiceCP4.class,ReportJobJournalCP12.Job.class,File.class);c.setAccessible(true);Object control=c.newInstance(service,job,reports);Method enter=type.getDeclaredMethod("enter",String.class,long.class);enter.setAccessible(true);enter.invoke(control,"BACKUP",120000L);Method progress=type.getDeclaredMethod("onBackupProgress",long.class,long.class,int.class);progress.setAccessible(true);
  ReportNativeCP4.loadForDiagnosticProcess("fixture:diagnostics","fixture");final long[] counters={0,Long.MAX_VALUE,0};File snapshot=new File(jobDir,"snapshot.db");
  ReportNativeCP4.backup(new File(a[0]),snapshot,32L<<20,new ReportNativeCP4.Control(){public boolean shouldContinue(){return true;}public void onBackupProgress(long pages,long remaining,int nativeCode){if(pages<remaining||remaining>counters[1])throw new AssertionError("nonmonotonic pinned progress");counters[0]++;counters[1]=remaining;counters[2]=pages;try{progress.invoke(control,pages,remaining,nativeCode);}catch(Exception e){throw new IllegalStateException(e);}}});
  if(counters[0]<1||counters[1]!=0||counters[2]<1)throw new AssertionError("native progress missing");if(!new File(jobDir,"backup-progress.json").isFile())throw new AssertionError("actual service progress path");
  Ctx ctx=(Ctx)UiContinuityR996.U.allocateInstance(Ctx.class);ctx.files=root;String text=CompleteReportFileR927.backupProgressText(ctx,id);if(!text.contains(counters[2]+"/"+counters[2]))throw new AssertionError("UI cannot read pending BACKUP progress");
  Ui activity=(Ui)UiContinuityR996.U.allocateInstance(Ui.class);Btn button=(Btn)UiContinuityR996.U.allocateInstance(Btn.class);Method show=CompleteReportFileR927.class.getDeclaredMethod("show",Activity.class,Button.class,android.widget.TextView.class,String.class,boolean.class);show.setAccessible(true);show.invoke(null,activity,button,null,"BACKUP",false);if(!button.enabled)throw new AssertionError("recovery/cancel menu disabled during export");
  System.out.println("BACKUP_PROGRESS_R996_PASS assertions=5 nativeCallbacks="+counters[0]+" snapshotPages="+counters[2]+" scope=ACTUAL_NATIVE_ACTUAL_SERVICE_CALLBACK_PENDING_UI_NO_DEVICE");
 }
}
