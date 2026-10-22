package android.content;
import java.io.File; import android.content.res.AssetManager; import android.content.pm.ApplicationInfo; import android.content.pm.PackageManager;
public class Context {
 public static final String ALARM_SERVICE="alarm", JOB_SCHEDULER_SERVICE="jobscheduler", POWER_SERVICE="power", NOTIFICATION_SERVICE="notification", CONNECTIVITY_SERVICE="connectivity", CLIPBOARD_SERVICE="clipboard", USER_SERVICE="user";
 public static final int MODE_PRIVATE=0, MODE_MULTI_PROCESS=4, RECEIVER_NOT_EXPORTED=4;
 public static File HOST_FILES; public static ContentResolver HOST_RESOLVER; public static android.app.AlarmManager HOST_ALARMS; public Object getSystemService(String n){return n.equals(ALARM_SERVICE)?HOST_ALARMS:new android.app.NotificationManager();} public Context getApplicationContext(){return this;}
 public SharedPreferences getSharedPreferences(String n,int m){return null;} public File getFilesDir(){return HOST_FILES;}
 public File getDatabasePath(String n){return new File(n);} public AssetManager getAssets(){return null;}
 public PackageManager getPackageManager(){return null;} public String getPackageName(){return "com.ethk3.novaonepro";}
 public ApplicationInfo getApplicationInfo(){return new ApplicationInfo();}
 public Intent registerReceiver(BroadcastReceiver r,IntentFilter f){return null;} public Intent registerReceiver(BroadcastReceiver r,IntentFilter f,int flags){return null;} public void unregisterReceiver(BroadcastReceiver r){}
 public ComponentName startService(Intent i){return null;} public ComponentName startForegroundService(Intent i){return null;} public boolean stopService(Intent i){return false;}
 public boolean isDeviceProtectedStorage(){return false;} public Context createDeviceProtectedStorageContext(){return this;} public boolean moveSharedPreferencesFrom(Context c,String n){return true;} public boolean deleteDatabase(String n){return true;} public File getExternalFilesDir(String t){return new File(".");} public File getCacheDir(){return new File(".");} public int checkSelfPermission(String p){return 0;} public android.content.res.Resources getResources(){return null;} public android.content.res.Resources.Theme getTheme(){return null;} public android.os.Looper getMainLooper(){return android.os.Looper.getMainLooper();} public void startActivity(Intent i){} public void sendBroadcast(Intent i){} public ContentResolver getContentResolver(){return HOST_RESOLVER;}
}
