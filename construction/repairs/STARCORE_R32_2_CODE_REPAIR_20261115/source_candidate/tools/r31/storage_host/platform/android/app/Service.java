package android.app;
import android.content.*;
import android.os.IBinder;
import java.io.File;
/** HOST context transport for private publish only. It does not run an Android Service. */
public class Service extends ContextWrapper {
 public static File filesR31;
 public Service(){super(null);throw new UnsupportedOperationException("USE_EXPLICIT_HOST_ALLOCATION");}
 @Override public File getFilesDir(){return filesR31;}
 public void onCreate(){throw new UnsupportedOperationException("ANDROID_LIFECYCLE_NOT_TESTED");}
 public void onDestroy(){throw new UnsupportedOperationException("ANDROID_LIFECYCLE_NOT_TESTED");}
 public int onStartCommand(Intent intent,int flags,int startId){throw new UnsupportedOperationException("ANDROID_LIFECYCLE_NOT_TESTED");}
 public IBinder onBind(Intent intent){throw new UnsupportedOperationException("ANDROID_LIFECYCLE_NOT_TESTED");}
 public void startForeground(int id,Notification notification){throw new UnsupportedOperationException("ANDROID_LIFECYCLE_NOT_TESTED");}
 public void stopForeground(boolean remove){throw new UnsupportedOperationException("ANDROID_LIFECYCLE_NOT_TESTED");}
 public boolean stopSelfResult(int id){throw new UnsupportedOperationException("ANDROID_LIFECYCLE_NOT_TESTED");}
}
