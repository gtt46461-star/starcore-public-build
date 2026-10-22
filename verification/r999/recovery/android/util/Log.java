package android.util;
public final class Log {
    public static int e(String tag,String message,Throwable failure){System.out.println(tag+":"+message+":"+failure.getClass().getSimpleName());return 0;}
    public static int w(String tag,String message){return 0;}
}
