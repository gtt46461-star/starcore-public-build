package android.system;
import java.io.*;import java.nio.channels.FileChannel;import java.nio.file.*;import java.util.*;
/** Android filesystem transport only; actual Linux rename and directory fsync. */
public final class Os {
 private static final Map<FileDescriptor,FileChannel> OPEN=new IdentityHashMap<>();
 public static synchronized FileDescriptor open(String path,int flags,int mode)throws ErrnoException {
  try{FileDescriptor descriptor=new FileDescriptor();OPEN.put(descriptor,FileChannel.open(Path.of(path),StandardOpenOption.READ));return descriptor;}
  catch(IOException io){throw new RuntimeException(io);}
 }
 public static synchronized void fsync(FileDescriptor descriptor)throws ErrnoException {try{OPEN.get(descriptor).force(true);}catch(IOException io){throw new RuntimeException(io);}}
 public static synchronized void close(FileDescriptor descriptor)throws ErrnoException {try{OPEN.remove(descriptor).close();}catch(IOException io){throw new RuntimeException(io);}}
 public static void rename(String from,String to)throws ErrnoException {try{Files.move(Path.of(from),Path.of(to),StandardCopyOption.ATOMIC_MOVE);}catch(IOException io){throw new RuntimeException(io);}}
}
