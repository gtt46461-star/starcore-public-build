package android.app;
public class Service extends android.content.Context {
 public static final int START_REDELIVER_INTENT=3;
 public void onCreate(){} public void onDestroy(){}
 public int onStartCommand(android.content.Intent i,int f,int id){return 3;}
 public android.os.IBinder onBind(android.content.Intent i){return null;}
 public void startForeground(int id,Notification n){} public void stopForeground(boolean r){}
 public boolean stopSelfResult(int id){return true;}
}
