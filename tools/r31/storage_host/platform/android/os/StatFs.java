package android.os;
/** Host filesystem observation only; Android allocated-block path remains unexecuted. */
public final class StatFs {
 private final java.io.File file;
 public StatFs(String path){file=new java.io.File(path);}
 public long getAvailableBytes(){return file.getUsableSpace();}
}
