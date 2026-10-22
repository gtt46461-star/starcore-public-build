package android.os;
public class StatFs {
 final java.io.File path; public StatFs(String p){path=new java.io.File(p);}
 public long getAvailableBytes(){return path.getUsableSpace();}
}
