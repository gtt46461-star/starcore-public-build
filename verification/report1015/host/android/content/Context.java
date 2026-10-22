package android.content;
import java.io.File;
/** Test transport only: real files/assets, no Android lifecycle emulation. */
public class Context {
 private final File files;private final android.content.res.AssetManager assets;
 public Context(File files,File root){this.files=files;File atom=new File(root,"cp4_atom_registry.jsonl");assets=new android.content.res.AssetManager(atom,new File(root,"research/cp4_molecule_seeds.jsonl"),0,atom.length());}
 public Context getApplicationContext(){return this;}
 public File getFilesDir(){return files;}
 public android.content.res.AssetManager getAssets(){return assets;}
}
