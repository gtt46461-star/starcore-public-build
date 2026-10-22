package android.util;
/** Host API adapter: product is always compiled against android.jar. */
public final class Base64 {
 public static final int DEFAULT=0,NO_PADDING=1,NO_WRAP=2,CRLF=4,URL_SAFE=8;
 public static String encodeToString(byte[] bytes,int flags){java.util.Base64.Encoder e=(flags&URL_SAFE)!=0?java.util.Base64.getUrlEncoder():java.util.Base64.getEncoder();if((flags&NO_PADDING)!=0)e=e.withoutPadding();return e.encodeToString(bytes);}
 public static byte[] decode(String s,int flags){return ((flags&URL_SAFE)!=0?java.util.Base64.getUrlDecoder():java.util.Base64.getDecoder()).decode(s);}
}
