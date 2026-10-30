package android.app;

/** File-backed HOST Application identity for real coordinator field injection. Never in APK. */
public class Application extends android.content.Context {
    public Application(){}
    public Application(java.io.File root){super(root);}
    public void onCreate(){}
}
