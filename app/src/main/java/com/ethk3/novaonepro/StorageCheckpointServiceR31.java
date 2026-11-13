package com.ethk3.novaonepro;

import android.app.Service;
import android.content.*;
import android.os.*;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/** Same-UID bound diagnostic capability: fixed PASSIVE only. No SQL, paths,
 * schema operations, source rows, offline rebuild or implicit service restart. */
public final class StorageCheckpointServiceR31 extends Service {
    static final int PASSIVE=1,CANCEL=2,RESULT=3;
    private final AtomicBoolean active=new AtomicBoolean(),cancelled=new AtomicBoolean();
    private volatile String owner="";
    private final java.util.concurrent.ExecutorService worker=java.util.concurrent.Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"r31-passive-wal-owner");t.setDaemon(true);return t;});
    private final Messenger messenger=new Messenger(new Handler(Looper.getMainLooper()){@Override public void handleMessage(Message message){
        if(message.sendingUid!=android.os.Process.myUid())return;
        String id=message.getData().getString("owner","");
        if(message.what==CANCEL){if(owner.equals(id))cancelled.set(true);return;}
        if(message.what!=PASSIVE||message.replyTo==null||!id.matches("[0-9a-f-]{36}"))return;
        Messenger reply=message.replyTo;boolean device=message.getData().getBoolean("deviceProtected");
        if(!active.compareAndSet(false,true)){respond(reply,id,null,"ANOTHER_PASSIVE_OWNER_STILL_ACTIVE");return;}
        owner=id;cancelled.set(false);worker.execute(()->{
            long[] result=null;String state="FAILED_OR_INCOMPLETE";long end=TimeSourceR811.monoNowMs()+40;
            try{
                Context source=getApplicationContext();
                if(device){
                    if(Build.VERSION.SDK_INT>=24)source=createDeviceProtectedStorageContext();
                    else{state="DEVICE_PROTECTED_STORAGE_UNAVAILABLE_BEFORE_API_24";return;}
                }
                File file=source.getDatabasePath(AppConfig.DB_NAME);
                // Cancel only stale readers registered by their owners in this
                // diagnostics process. An unknown main/other-process reader is
                // never declared closed; SQLite's zero-wait result is authoritative.
                if(!SourceReadLeaseR82.requestStaleReaders(file,60000L))state="KNOWN_SOURCE_READER_NOT_CLOSED";
                else try(SourceReadLeaseR82 lease=SourceReadLeaseR82.tryExclusiveCheckpoint(file,()->!cancelled.get()&&TimeSourceR811.monoNowMs()<end)){
                    if(lease==null)state="SOURCE_OR_OFFLINE_LEASE_BUSY";
                    else{result=ReportNativeCP4.checkpointWalR31(source,file,0,Math.max(1,end-TimeSourceR811.monoNowMs()),lease,new ReportNativeCP4.Control(){public boolean shouldContinue(){return !cancelled.get()&&TimeSourceR811.monoNowMs()<end;}});state=resultStateR31(result);}
                }
            }catch(Exception failure){state=cancelled.get()?"CANCELLED_OWNER_NOT_SUCCESS":"NATIVE_OR_SOURCE_UNAVAILABLE";}
            finally{owner="";active.set(false);respond(reply,id,result,state);}
        });
    }});
    /** A successful NOOP probe, an empty WAL, cancellation and a copy-budget
     * refusal are observations, never receipts for an executed PASSIVE. */
    static String resultStateR31(long[] values){
        if(values==null||values.length!=8)return "NATIVE_RESULT_UNAVAILABLE";
        long code=values[0];
        if(code==-1101)return "CANCELLED_NATIVE_BUDGET";
        if(code==-1102)return "RESOURCE_WAIT_NATIVE_COPY_BUDGET";
        if(values[7]!=1)return code==0?"PASSIVE_NOT_ATTEMPTED_NO_WAL":"PASSIVE_PRECHECK_NOT_EXECUTED";
        return code==0||code==5||code==6?"ACTUAL_PASSIVE_RESULT":"ACTUAL_PASSIVE_FAILED";
    }
    private static void respond(Messenger target,String owner,long[] values,String state){try{Message reply=Message.obtain(null,RESULT);Bundle b=new Bundle();b.putString("owner",owner);b.putString("state",state);if(values!=null)b.putLongArray("result",values);reply.setData(b);target.send(reply);}catch(RemoteException gone){/* A dead client cannot make this executed operation a success receipt. */}}
    @Override public IBinder onBind(Intent intent){return messenger.getBinder();}
    @Override public void onDestroy(){cancelled.set(true);worker.shutdown();super.onDestroy();}
}
