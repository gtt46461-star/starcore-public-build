package com.ethk3.novaonepro;

import android.app.*;
import android.content.*;
import android.os.*;
import android.os.Process;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;

/** Private fresh Android process. All DB/file APIs are routed to a restored
 * application tree; no network, fetch, forecast, training or settlement runs. */
public final class StorageRestoreProbeServiceR31 extends Service {
    private boolean started,rejectBeforeStart;
    static final int QUERY_PROCESS_R31=IBinder.FIRST_CALL_TRANSACTION;
    static final int ABANDON_UNSTARTED_R31=IBinder.FIRST_CALL_TRANSACTION+1;
    private boolean abandonUnstartedR31(){synchronized(this){if(started)return false;rejectBeforeStart=true;return true;}}
    private final Binder ownerBinder=new Binder(){@Override protected boolean onTransact(int code,Parcel input,Parcel reply,int flags)throws RemoteException{
        if(code==ABANDON_UNSTARTED_R31){input.enforceInterface(StorageRestoreProofR31.METHOD);boolean empty=abandonUnstartedR31();reply.writeNoException();if(empty)new Thread(()->Process.killProcess(Process.myPid()),"r31-unstarted-probe-close").start();return true;}
        if(code==QUERY_PROCESS_R31){input.enforceInterface(StorageRestoreProofR31.METHOD);reply.writeNoException();reply.writeInt(Process.myPid());return true;}
        return super.onTransact(code,input,reply,flags);
    }};
    @Override public IBinder onBind(Intent ignored){return ownerBinder;}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        if(intent==null)return START_NOT_STICKY;
        synchronized(this){if(started||rejectBeforeStart){ResultReceiver busy=intent.getParcelableExtra("receiver");if(busy!=null){Bundle state=new Bundle();state.putString("nonce",intent.getStringExtra("nonce"));state.putString("state","WAIT_PREVIOUS_RESTORE_WORKER_STILL_RUNNING");busy.send(1,state);}return START_NOT_STICKY;}started=true;}
        ResultReceiver receiver=intent.getParcelableExtra("receiver");String nonce=intent.getStringExtra("nonce");
        new Thread(()->{
            Bundle reply=new Bundle();reply.putString("nonce",nonce);int code=1;
            try{
                if(Build.VERSION.SDK_INT<28||!getPackageName().concat(":restore_probe").equals(Application.getProcessName())||StarcoreApplication.isMainRuntimeProcess(this)||DatabaseCoordinator.peek()!=null)throw new IOException("RESTORE_FRESH_PRIVATE_PROCESS_REQUIRED");
                if(!KeyProvider.unavailableReasonCP12().startsWith("KEY_OWNER_NOT_INITIALIZED"))throw new IOException("RESTORE_CREDENTIAL_OWNER_MUST_NOT_START");
                System.loadLibrary("starcore_report_cp4");
                File directory=new File(getFilesDir(),"r31-restore-probes"),root=StorageRestoreProofR31.child(directory,nonce+"/"+ReleaseManifestR926.APPLICATION_ID);
                File request=new File(root,"request.properties");Properties original=StorageMaintenanceFileR82.read(request);
                if(!nonce.equals(original.getProperty("nonce"))||!BuildConfig.SOURCE_COMMIT_R26.equals(original.getProperty("source"))||Integer.parseInt(original.getProperty("parent_pid"))==Process.myPid())throw new IOException("RESTORE_PROCESS_REQUEST_IDENTITY");
                File clone=new File(root,"databases/"+AppConfig.DB_NAME);
                if(!original.getProperty("clone_sha256").equals(StorageMaintenanceFileR82.sha256(clone)))throw new IOException("RESTORE_CLONE_INITIAL_BYTES");
                File assetFile=new File(root,"asset-manifest.properties");if(!original.getProperty("asset_manifest_sha256").equals(StorageMaintenanceFileR82.sha256(assetFile)))throw new IOException("RESTORE_ASSET_MANIFEST_CHANGED");
                Properties assets=StorageMaintenanceFileR82.read(assetFile);for(String path:assets.stringPropertyNames()){File file=StorageRestoreProofR31.child(root,path);if(!(StorageMaintenanceFileR82.sha256(file)+":"+file.length()).equals(assets.getProperty(path)))throw new IOException("RESTORE_ASSET_BYTES_CHANGED");}
                Properties proof;
                try(SQLiteDatabase read=SQLiteDatabase.openDatabase(clone.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
                    proof=StorageRestoreBusinessProbeR31.inventory(read,original.getProperty("native_full_compare"),original.getProperty("asset_manifest_sha256"));
                }
                ProbeApplication app=new ProbeApplication(getApplicationContext(),root);
                DbStore store=new DbStore(app); // Actual original critical startup/reconciliation on the clone only.
                if(!new File(store.readable().getPath()).getCanonicalFile().equals(clone.getCanonicalFile()))throw new IOException("RESTORE_COORDINATOR_PATH_ESCAPE");
                StorageRestoreBusinessProbeR31.verify(store,proof);
                proof.setProperty("restore_application_boot","PRIVATE_PROCESS_NO_CREDENTIAL_OWNER_NO_LIVE_DB");
                proof.setProperty("restore_method",StorageRestoreProofR31.METHOD);proof.setProperty("restore_nonce",nonce);proof.setProperty("restore_source_commit",BuildConfig.SOURCE_COMMIT_R26);proof.setProperty("restore_parent_pid",original.getProperty("parent_pid"));proof.setProperty("restore_probe_pid",Integer.toString(Process.myPid()));proof.setProperty("restore_active_sha256",original.getProperty("active_sha256"));proof.setProperty("restore_clone_sha256",original.getProperty("clone_sha256"));proof.setProperty("restore_request_sha256",StorageMaintenanceFileR82.sha256(request));
                File result=new File(root,"result.properties");StorageMaintenanceFileR82.write(result,proof);reply.putString("result_sha256",StorageMaintenanceFileR82.sha256(result));code=0;
            }catch(Exception failure){reply.putString("state","WAIT_RESTORE_PROOF:"+failure.getClass().getSimpleName());}
            finally{
                if(receiver!=null)receiver.send(code,reply);
                stopSelf(startId);
                // This process never survives to reuse its clone-bound singleton.
                Process.killProcess(Process.myPid());
            }
        },"r31-restore-probe").start();return START_NOT_STICKY;
    }
    /** The original coordinator requires an Application, not a wrapper retained
     * from another component. This object exists only in :restore_probe. */
    static final class ProbeApplication extends Application {
        final File root;
        ProbeApplication(Context base,File root){this.root=root;attachBaseContext(base);}
        @Override public Context getApplicationContext(){return this;}
        @Override public File getDatabasePath(String name){if(!AppConfig.DB_NAME.equals(name))throw new IllegalArgumentException("RESTORE_DATABASE_NAME");return new File(root,"databases/"+name);}
        @Override public File getFilesDir(){return new File(root,"files");}
        @Override public File getCacheDir(){File file=new File(root,"cache");if(!file.mkdirs()&&!file.isDirectory())throw new IllegalStateException("RESTORE_CACHE_DIRECTORY");return file;}
        @Override public File getCodeCacheDir(){return privateDirectory("code_cache");}
        @Override public File getNoBackupFilesDir(){return privateDirectory("no_backup");}
        @Override public Context createDeviceProtectedStorageContext(){return this;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){throw new UnsupportedOperationException("RESTORE_SHARED_PREFERENCES_UNSUPPORTED");}
        @Override public android.content.pm.ApplicationInfo getApplicationInfo(){android.content.pm.ApplicationInfo info=new android.content.pm.ApplicationInfo(super.getApplicationInfo());info.dataDir=root.getPath();return info;}
        private File privateDirectory(String name){File directory=new File(root,name);if(!directory.mkdirs()&&!directory.isDirectory())throw new IllegalStateException("RESTORE_PRIVATE_DIRECTORY");return directory;}
        @Override public File getDataDir(){return root;}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,android.database.DatabaseErrorHandler handler){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,handler);}
    }
}
