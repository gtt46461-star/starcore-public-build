package android.app;
/** Host scheduling port records the real Service resume command; no Android alarm claim. */
public class AlarmManager {
 public static final int ELAPSED_REALTIME_WAKEUP=2; public int schedules;
 public void set(int type,long at,PendingIntent callback){schedules++;}
 public void setAndAllowWhileIdle(int type,long at,PendingIntent callback){schedules++;}
}
