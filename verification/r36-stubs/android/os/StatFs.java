package android.os;

public class StatFs {
    public StatFs(String path) {}
    public long getAvailableBytes() { return 1024L * 1024L * 1024L; }
    public long getTotalBytes() { return 2L * 1024L * 1024L * 1024L; }
}
