package com.ethk3.novaonepro;

import android.app.Application;
import android.content.*;
import android.content.pm.ApplicationInfo;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import androidx.test.runner.AndroidJUnitRunner;
import java.io.File;
import java.util.UUID;

/** Test APK only. Route the original Application before its sole DB owner exists; never reset that owner. */
public final class R30FaultAndroidRunner extends AndroidJUnitRunner {
    private RoutedContext routed;
    @Override public Application newApplication(ClassLoader loader,String name,Context base)throws InstantiationException,IllegalAccessException,ClassNotFoundException{
        routed=new RoutedContext(base);Application app=super.newApplication(loader,name,routed);routed.application=app;return app;
    }
    @Override public void onCreate(Bundle args){
        String scope=args==null?null:args.getString("r30Sandbox");if(scope==null||!scope.matches("[A-Za-z0-9_-]{1,80}"))throw new IllegalArgumentException("explicit r30Sandbox required");
        if(routed==null)throw new IllegalStateException("application context not installed");routed.scope=scope;
        new ServiceHeartbeatStore(routed.application).setUserEnabled(false);
        super.onCreate(args);
    }
    static final class RoutedContext extends ContextWrapper {
        Application application;volatile String scope="unbound-"+UUID.randomUUID();
        RoutedContext(Context base){super(base);}
        File root(){return new File(super.getNoBackupFilesDir(),"r30-instrumentation/"+scope);}
        File dir(String name){File f=new File(root(),name);if(!f.isDirectory()&&!f.mkdirs())throw new IllegalStateException("test sandbox directory");return f;}
        @Override public Context getApplicationContext(){return application==null?this:application;}
        @Override public Context createDeviceProtectedStorageContext(){return this;}
        @Override public File getFilesDir(){return dir("files");}
        @Override public File getCacheDir(){return dir("cache");}
        @Override public File getCodeCacheDir(){return dir("code_cache");}
        @Override public File getNoBackupFilesDir(){return dir("no_backup");}
        @Override public File getDatabasePath(String name){return new File(dir("databases"),name);}
        @Override public String[] databaseList(){String[] names=dir("databases").list();return names==null?new String[0]:names;}
        @Override public SharedPreferences getSharedPreferences(String name,int mode){return super.getSharedPreferences("r30-sandbox-"+scope+"-"+name,mode);}
        @Override public ApplicationInfo getApplicationInfo(){ApplicationInfo info=new ApplicationInfo(super.getApplicationInfo());info.dataDir=root().getPath();return info;}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name),factory);}
        @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,DatabaseErrorHandler handler){return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(),factory,handler);}
        @Override public boolean deleteDatabase(String name){return SQLiteDatabase.deleteDatabase(getDatabasePath(name));}
    }
}
