package android.content;
import java.io.File;
/** Host boundary only: real files/assets and a recorded service start, no Android lifecycle. */
public class Context {
 private final File files;private final android.content.res.AssetManager assets;
 public static int serviceStarts;
 public Context(File files,File root){this.files=files;File atom=new File(root,"cp4_atom_registry.jsonl");assets=new android.content.res.AssetManager(atom,new File(root,"research/cp4_molecule_seeds.jsonl"),0,atom.length());}
 public Context getApplicationContext(){return this;}
 public File getFilesDir(){return files;}
 public android.content.res.AssetManager getAssets(){return assets;}
 public ComponentName startService(Intent intent){serviceStarts++;return null;}
 public ComponentName startForegroundService(Intent intent){serviceStarts++;return null;}
}
