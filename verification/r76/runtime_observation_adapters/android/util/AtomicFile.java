package android.util;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Host transport only: real files + fsync, with observable injected durability delay.
 * Models both modern .new atomic replacement and legacy .bak recovery. */
public final class AtomicFile {
    public static int starts,finishes,failures,reads,recoveries;
    public static long delayMs=20L;
    public static final List<String> durableNames=new ArrayList<>();
    private final File base,pending,backup;
    public AtomicFile(File base){this.base=base;pending=new File(base+".new");backup=new File(base+".bak");}
    public File getBaseFile(){return base;}
    public FileOutputStream startWrite()throws IOException{
        starts++;File parent=base.getParentFile();if(parent!=null)parent.mkdirs();return new FileOutputStream(pending);
    }
    public void finishWrite(FileOutputStream out){
        try{if(delayMs>0)Thread.sleep(delayMs);out.getFD().sync();out.close();
            Files.move(pending.toPath(),base.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
            finishes++;durableNames.add(base.getName());
        }catch(Exception e){throw new IllegalStateException(e);}
    }
    public void failWrite(FileOutputStream out){failures++;try{out.close();Files.deleteIfExists(pending.toPath());}catch(IOException e){throw new IllegalStateException(e);}}
    public FileInputStream openRead()throws FileNotFoundException{
        reads++;try{
            if(backup.exists()){Files.move(backup.toPath(),base.toPath(),StandardCopyOption.REPLACE_EXISTING);recoveries++;}
            Files.deleteIfExists(pending.toPath());
        }catch(IOException e){throw new IllegalStateException(e);}
        return new FileInputStream(base);
    }
    public byte[] readFully()throws IOException{try(FileInputStream in=openRead()){return in.readAllBytes();}}
    public void delete(){base.delete();pending.delete();backup.delete();}
    public static void reset(){starts=finishes=failures=reads=recoveries=0;durableNames.clear();}
}
