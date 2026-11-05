package android.os;

/** HOST transport only: actual Linux FileStore available bytes, no fixed quota values. */
public final class StatFs {
    private final java.nio.file.Path path;
    public StatFs(String value){if(value==null)throw new IllegalArgumentException("HOST_NULL_PATH");path=java.nio.file.Paths.get(value);if(!java.nio.file.Files.isDirectory(path))throw new IllegalArgumentException("HOST_DIRECTORY_REQUIRED:"+value);}
    public long getAvailableBytes(){try{return java.nio.file.Files.getFileStore(path).getUsableSpace();}catch(java.io.IOException unknown){throw new IllegalArgumentException("HOST_FILESYSTEM_UNAVAILABLE",unknown);}}
}
