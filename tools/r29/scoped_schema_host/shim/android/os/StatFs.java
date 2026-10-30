package android.os;

import java.io.File;

/** HOST filesystem capacity from the actual fixture volume, never fabricated Android storage. */
public final class StatFs {
    private File path;
    public StatFs(String path){restat(path);}
    public void restat(String path){if(path==null)throw new IllegalArgumentException("HOST_STATFS_PATH_REQUIRED");File file=new File(path).getAbsoluteFile();if(!file.exists())throw new IllegalArgumentException("HOST_STATFS_PATH_MISSING:"+path);this.path=file;}
    public long getAvailableBytes(){return path.getUsableSpace();}
    public long getFreeBytes(){return path.getFreeSpace();}
    public long getTotalBytes(){return path.getTotalSpace();}
    public long getBlockSizeLong(){return 4096L;}
    public long getBlockCountLong(){return getTotalBytes()/getBlockSizeLong();}
    public long getAvailableBlocksLong(){return getAvailableBytes()/getBlockSizeLong();}
    public long getFreeBlocksLong(){return getFreeBytes()/getBlockSizeLong();}
    public int getBlockSize(){return (int)getBlockSizeLong();}
    public int getBlockCount(){return (int)Math.min(Integer.MAX_VALUE,getBlockCountLong());}
    public int getAvailableBlocks(){return (int)Math.min(Integer.MAX_VALUE,getAvailableBlocksLong());}
    public int getFreeBlocks(){return (int)Math.min(Integer.MAX_VALUE,getFreeBlocksLong());}
}
