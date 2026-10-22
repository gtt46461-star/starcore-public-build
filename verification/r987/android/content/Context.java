package android.content;
import java.io.File;import java.util.*;
/** Host transport for actual static requestCancel; no Android scheduling claim. */
public class Context {
 private final File files;public final List<Intent> starts=new ArrayList<>();
 public Context(File files){this.files=files;}public Context getApplicationContext(){return this;}
 public File getFilesDir(){return files;}
 public ComponentName startService(Intent intent){starts.add(intent);return null;}
 public ComponentName startForegroundService(Intent intent){starts.add(intent);return null;}
}
