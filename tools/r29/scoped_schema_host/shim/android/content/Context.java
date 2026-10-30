package android.content;

import java.io.File;

/** File-backed HOST framework context. Never packaged into the APK. */
public class Context {
    public static final int MODE_PRIVATE=0;
    private File hostRoot;
    public Context(){}
    public Context(File root){hostSetRoot(root);}
    public void hostSetRoot(File root){if(root==null)throw new IllegalArgumentException("HOST_CONTEXT_ROOT_REQUIRED");hostRoot=root.getAbsoluteFile();directory(hostRoot);}
    private static File directory(File dir){if(!dir.isDirectory()&&!dir.mkdirs())throw new IllegalStateException("HOST_DIRECTORY_FAILED:"+dir);return dir;}
    private File root(){if(hostRoot==null)throw new IllegalStateException("HOST_CONTEXT_NOT_BOUND");return hostRoot;}
    protected void attachBaseContext(Context context){if(context==null)throw new IllegalArgumentException("HOST_CONTEXT_REQUIRED");hostRoot=context.root();}
    public Context getApplicationContext(){return this;}
    public File getDatabasePath(String name){return new File(directory(new File(root(),"databases")),name);}
    public File getFilesDir(){return directory(new File(root(),"files"));}
    public File getCacheDir(){return directory(new File(root(),"cache"));}
    public Context createDeviceProtectedStorageContext(){return this;}
    public String getPackageName(){return "com.ethk3.novaonepro.host.fixture";}
    public ClassLoader getClassLoader(){return getClass().getClassLoader();}
}
