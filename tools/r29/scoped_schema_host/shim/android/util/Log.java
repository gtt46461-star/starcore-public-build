package android.util;

/** HOST logging to stderr; no swallowed failures. Never packaged into APK. */
public final class Log {
    private Log(){}
    private static int log(String tag,String message,Throwable error){System.err.println(tag+":"+message);if(error!=null)error.printStackTrace(System.err);return 0;}
    public static int e(String tag,String message){return log(tag,message,null);}
    public static int e(String tag,String message,Throwable error){return log(tag,message,error);}
    public static int w(String tag,String message){return log(tag,message,null);}
    public static int w(String tag,String message,Throwable error){return log(tag,message,error);}
    public static int d(String tag,String message){return log(tag,message,null);}
    public static int i(String tag,String message){return log(tag,message,null);}
    public static int v(String tag,String message){return log(tag,message,null);}
}
