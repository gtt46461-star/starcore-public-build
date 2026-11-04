package com.ethk3.novaonepro;

import java.io.*;
import java.util.Iterator;

/** Streaming directory enumeration. Unsupported API21-25 stays explicit; no unbounded listFiles fallback. */
public interface DirectoryCursorR31 extends Closeable {
    File next()throws IOException;
    static DirectoryCursorR31 open(File directory)throws IOException{
        String vm=System.getProperty("java.vm.name",""),runtime=System.getProperty("java.runtime.name","");boolean android=vm.toLowerCase(java.util.Locale.ROOT).contains("dalvik")||runtime.toLowerCase(java.util.Locale.ROOT).contains("android");
        if(android){try{int api=Class.forName("android.os.Build$VERSION").getField("SDK_INT").getInt(null);if(api<26)throw new IOException("DIRECTORY_STREAM_UNSUPPORTED_API_"+api);}catch(ReflectiveOperationException unavailable){throw new IOException("DIRECTORY_STREAM_API_UNKNOWN",unavailable);}}
        try{return Api26.open(directory);}catch(LinkageError unsupported){throw new IOException("DIRECTORY_STREAM_UNSUPPORTED",unsupported);}
    }
    final class Api26 {
        private Api26(){}
        static DirectoryCursorR31 open(File directory)throws IOException{
            final java.nio.file.DirectoryStream<java.nio.file.Path> stream=java.nio.file.Files.newDirectoryStream(directory.toPath());final Iterator<java.nio.file.Path> rows=stream.iterator();
            return new DirectoryCursorR31(){public File next()throws IOException{try{return rows.hasNext()?rows.next().toFile():null;}catch(java.nio.file.DirectoryIteratorException error){throw error.getCause();}}public void close()throws IOException{stream.close();}};
        }
    }
}
