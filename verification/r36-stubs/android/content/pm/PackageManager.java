package android.content.pm;

import android.content.Intent;

public class PackageManager {
    public static final int PERMISSION_GRANTED = 0;
    public static final int GET_PERMISSIONS = 4096;
    public static final int GET_SIGNATURES = 64;
    public static final int GET_SIGNING_CERTIFICATES = 134217728;
    public static class NameNotFoundException extends Exception { public NameNotFoundException(){} public NameNotFoundException(String message){super(message);} }
    public ResolveInfo resolveActivity(Intent intent, int flags) { return null; }
    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException { return new PackageInfo(); }
}
