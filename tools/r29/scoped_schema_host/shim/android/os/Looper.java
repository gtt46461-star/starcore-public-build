package android.os;

/** HOST runs on ordinary JVM worker threads, never an Android UI Looper. Never packaged into APK. */
public final class Looper {
    private static final Looper MAIN=new Looper();
    private Looper(){}
    public static Looper myLooper(){return null;}
    public static Looper getMainLooper(){return MAIN;}
}
