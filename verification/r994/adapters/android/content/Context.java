package android.content;
public class Context {
 private final java.io.File files; private final android.content.res.AssetManager assets;private final android.content.pm.ApplicationInfo info;
 public Context(java.io.File files,java.io.File apk){this.files=files;assets=new android.content.res.AssetManager(apk);info=new android.content.pm.ApplicationInfo();info.sourceDir=apk.getPath();}
 public Context getApplicationContext(){return this;}public java.io.File getFilesDir(){return files;}public android.content.res.AssetManager getAssets(){return assets;}public android.content.pm.ApplicationInfo getApplicationInfo(){return info;}
}
