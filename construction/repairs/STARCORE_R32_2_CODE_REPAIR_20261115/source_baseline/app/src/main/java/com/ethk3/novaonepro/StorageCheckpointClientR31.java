package com.ethk3.novaonepro;

import android.content.*;
import android.os.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** One real cross-process operation in flight. A caller deadline requests owner
 * cancellation but never assumes Binder/native work has stopped or starts a
 * replacement thread. Only an actual result or never-sent bind clears ownership. */
public final class StorageCheckpointClientR31 {
    private StorageCheckpointClientR31() {}
    private static final Object LOCK=new Object();private static Pending pending;private static Map<String,Object> last=new LinkedHashMap<>();
    private static final class Pending implements ServiceConnection {
        final android.app.Application application;final String id=UUID.randomUUID().toString();final CountDownLatch finished=new CountDownLatch(1);final boolean device;
        volatile boolean timedOut,sent;volatile Messenger remote;volatile long[] result;
        final Messenger reply=new Messenger(new Handler(Looper.getMainLooper()){@Override public void handleMessage(Message message){if(message.what!=StorageCheckpointServiceR31.RESULT||!id.equals(message.getData().getString("owner")))return;
            result=message.getData().getLongArray("result");Map<String,Object> observed=new LinkedHashMap<>();observed.put("state",message.getData().getString("state"));observed.put("failureCode",message.getData().getString("failureCode",""));observed.put("ownerCompleted",true);observed.put("callerTimedOut",timedOut);observed.put("completedAt",System.currentTimeMillis());if(result!=null&&result.length==8){observed.put("nativeCode",result[0]);observed.put("busy",result[1]);observed.put("logFrames",result[2]);observed.put("checkpointedFrames",result[3]);observed.put("nativeElapsedMs",result[4]);observed.put("nativeSqliteVersion",result[6]);observed.put("operationAttempted",result[7]==1);}
            synchronized(LOCK){last=observed;if(pending==Pending.this)pending=null;}finished.countDown();try{application.unbindService(Pending.this);}catch(IllegalArgumentException ignored){}
        }});
        Pending(android.app.Application application,boolean device){this.application=application;this.device=device;}
        void neverSent(String state){synchronized(LOCK){if(pending==this)pending=null;last=new LinkedHashMap<>();last.put("state",state);last.put("ownerCompleted",false);}finished.countDown();try{application.unbindService(this);}catch(IllegalArgumentException ignored){}}
        public void onServiceConnected(ComponentName name,IBinder binder){remote=new Messenger(binder);if(timedOut){neverSent("DEADLINE_BEFORE_OWNER_SENT");return;}try{binder.linkToDeath(()->{synchronized(LOCK){last=new LinkedHashMap<>();last.put("state","REMOTE_PROCESS_DIED_OPERATION_NOT_SUCCESS");last.put("ownerCompleted",false);last.put("ownerExecutionEnded",true);if(pending==Pending.this)pending=null;}finished.countDown();try{application.unbindService(Pending.this);}catch(IllegalArgumentException ignored){}},0);Message request=Message.obtain(null,StorageCheckpointServiceR31.PASSIVE);Bundle data=new Bundle();data.putString("owner",id);data.putBoolean("deviceProtected",device);request.setData(data);request.replyTo=reply;sent=true;remote.send(request);if(timedOut)cancel();}catch(RemoteException failed){/* Delivery is ambiguous: retain the owner until death proof/result. */}}
        public void onServiceDisconnected(ComponentName name){synchronized(LOCK){last=new LinkedHashMap<>();last.put("state","REMOTE_DISCONNECTED_OWNER_END_UNPROVEN");last.put("ownerCompleted",false);}finished.countDown();}
        public void onNullBinding(ComponentName name){if(!sent)neverSent("NO_BOUND_CHECKPOINT_CAPABILITY");}
        void cancel(){timedOut=true;Messenger target=remote;if(target!=null&&sent)try{Message cancel=Message.obtain(null,StorageCheckpointServiceR31.CANCEL);Bundle data=new Bundle();data.putString("owner",id);cancel.setData(data);target.send(cancel);}catch(RemoteException ignored){}}
    }
    static boolean sourceDeviceProtectedR31(Context context,File database)throws IOException {
        boolean device;
        if(database.getCanonicalFile().equals(context.getApplicationContext().getDatabasePath(AppConfig.DB_NAME).getCanonicalFile()))device=Build.VERSION.SDK_INT>=24&&context.getApplicationContext().isDeviceProtectedStorage();
        else if(Build.VERSION.SDK_INT<24)throw new IOException("WAL_DEVICE_PROTECTED_STORAGE_UNAVAILABLE_BEFORE_API_24");
        else if(database.getCanonicalFile().equals(context.createDeviceProtectedStorageContext().getDatabasePath(AppConfig.DB_NAME).getCanonicalFile()))device=true;
        else throw new IOException("WAL_SOURCE_PATH_NOT_APPLICATION_DATABASE");
        return device;
    }
    public static long[] passive(Context context,File database,long deadline)throws IOException {
        boolean device=sourceDeviceProtectedR31(context,database);
        Context app=context.getApplicationContext();
        if(!(app instanceof android.app.Application))throw new IOException("WAL_APPLICATION_CONTEXT_REQUIRED");
        Pending work;synchronized(LOCK){if(pending!=null)return null;work=new Pending((android.app.Application)app,device);pending=work;}
        try{if(!work.application.bindService(new Intent(work.application,StorageCheckpointServiceR31.class),work,Context.BIND_AUTO_CREATE)){work.neverSent("CHECKPOINT_BIND_UNAVAILABLE");return null;}
            long left=Math.max(0,deadline-TimeSourceR811.monoNowMs());if(left==0||!work.finished.await(left,TimeUnit.MILLISECONDS)){work.cancel();return null;}return work.result;
        }catch(InterruptedException cancelled){Thread.currentThread().interrupt();work.cancel();return null;}catch(RuntimeException unavailable){if(!work.sent)work.neverSent("CHECKPOINT_BIND_REJECTED");else work.cancel();return null;}
    }
    public static Map<String,Object> snapshot(){synchronized(LOCK){Map<String,Object> value=new LinkedHashMap<>(last);value.put("inFlight",pending!=null);value.put("timeoutMeansStopped",false);value.put("sourceProcess",":diagnostics");return value;}}
}
