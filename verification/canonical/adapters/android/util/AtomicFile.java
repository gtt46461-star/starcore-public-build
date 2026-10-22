package android.util;
import java.io.*;
import java.nio.file.*;
/** Host transport adapter only. Android OS durability is a separate device gate. */
public class AtomicFile {
 private final File base, pending;
 public AtomicFile(File file){base=file;pending=new File(file.getPath()+".new");}
 public File getBaseFile(){return base;}
 public FileOutputStream startWrite() throws IOException {File parent=base.getParentFile();if(parent!=null)parent.mkdirs();return new FileOutputStream(pending);}
 public void finishWrite(FileOutputStream out){try{out.getFD().sync();out.close();Files.move(pending.toPath(),base.toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(IOException e){throw new IllegalStateException(e);}}
 public void failWrite(FileOutputStream out){try{out.close();Files.deleteIfExists(pending.toPath());}catch(IOException e){throw new IllegalStateException(e);}}
 public FileInputStream openRead() throws FileNotFoundException {if(pending.exists())pending.delete();return new FileInputStream(base);}
 public byte[] readFully() throws IOException {try(FileInputStream in=openRead()){return in.readAllBytes();}}
 public void delete(){base.delete();pending.delete();}
}
