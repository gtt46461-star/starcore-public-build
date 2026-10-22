package android.util;
public final class Log { public static int e(String t,String m,Throwable e){System.out.println("EXPECTED_LOG "+t+":"+m);return 0;} public static int d(String t,String m){return 0;} }
