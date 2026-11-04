package android.content;
/** HOST API parity only: actual isolated files; no installed package or certificate. */
public class Context {
 public java.io.File getDatabasePath(String name){throw new IllegalStateException("HOST_PATH_REQUIRED");}
 public Context getApplicationContext(){return this;}
 public java.io.File getFilesDir(){java.io.File files=new java.io.File(getDatabasePath("HOST_FIXTURE").getPath()+".app/files");if(!files.isDirectory()&&!files.mkdirs())throw new IllegalStateException("HOST_FILES_DIRECTORY");return files;}
 public android.content.res.AssetManager getAssets(){return new android.content.res.AssetManager();}
 public android.content.pm.ApplicationInfo getApplicationInfo(){throw new UnsupportedOperationException("HOST_NOT_AN_INSTALLED_APK");}
 public android.content.pm.PackageManager getPackageManager(){throw new UnsupportedOperationException("HOST_NO_ANDROID_PACKAGE_MANAGER");}
 public String getPackageName(){return "HOST_NATIVE_FIXTURE_NOT_AN_INSTALLED_APK";}
}
