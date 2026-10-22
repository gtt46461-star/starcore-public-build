package android.system;
import java.io.*;import java.nio.channels.FileChannel;import java.nio.file.*;import java.util.*;
/** Host adapter for the two directory-fsync Android API calls. Real Linux FileChannel I/O. */
public final class Os {
 public static void rename(String source,String destination)throws ErrnoException {try{Files.move(Path.of(source),Path.of(destination),StandardCopyOption.ATOMIC_MOVE);}catch(IOException failure){throw new RuntimeException(failure);}}
 private static final Map<FileDescriptor,FileChannel> OPEN=new IdentityHashMap<>();
 public static synchronized FileDescriptor open(String path,int flags,int mode)throws ErrnoException {
  try{FileDescriptor descriptor=new FileDescriptor();OPEN.put(descriptor,FileChannel.open(Path.of(path),StandardOpenOption.READ));return descriptor;}
  catch(IOException io){throw new RuntimeException(io);}
 }
 public static synchronized void fsync(FileDescriptor descriptor)throws ErrnoException {try{OPEN.get(descriptor).force(true);}catch(IOException io){throw new RuntimeException(io);}}
 public static synchronized void close(FileDescriptor descriptor)throws ErrnoException {try{OPEN.remove(descriptor).close();}catch(IOException io){throw new RuntimeException(io);}}
}
