package android.net;
public final class Uri {
 private final String value;private Uri(String v){value=v;}public static Uri parse(String v){return new Uri(v);}
 public String getScheme(){return java.net.URI.create(value).getScheme();}
 public java.util.List<String> getPathSegments(){java.util.ArrayList<String>a=new java.util.ArrayList<>();for(String s:java.net.URI.create(value).getPath().split("/"))if(!s.isEmpty())a.add(s);return a;}
 public String toString(){return value;}
}
