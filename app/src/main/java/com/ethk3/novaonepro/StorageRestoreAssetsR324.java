package com.ethk3.novaonepro;

import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import java.io.*;
import java.security.*;
import java.util.*;

/** Streaming private-restore assets. Per-path/depth/physical-space limits remain;
 * accumulated evidence does not have a file-count or total-byte lifetime cap.
 * The manifest is bound by the existing authenticated restore request hash.
 */
final class StorageRestoreAssetsR324 {
    private StorageRestoreAssetsR324(){}
    static final String FORMAT="R324_STREAM_ASSET_MANIFEST_V1",MEMBER="asset-manifest-r324.bin";
    private static final int MAGIC=0x32415301,MAX_PATH=4096;
    private static final long FREE_RESERVE=64L<<20;
    static final class Totals {long bytes,files;}
    private static void check()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("RESTORE_ASSET_CANCELLED");}
    private static File child(File root,String relative)throws IOException {
        if(relative==null||relative.isEmpty()||relative.length()>MAX_PATH||relative.startsWith("/")||relative.contains("\\")||Arrays.asList(relative.split("/",-1)).contains("..")||Arrays.asList(relative.split("/",-1)).contains("."))throw new IOException("RESTORE_ASSET_PATH");
        return StorageRestoreProofR31.child(root,relative);
    }
    private static void sync(File directory)throws IOException {
        java.io.FileDescriptor fd=null;try{fd=Os.open(directory.getCanonicalPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}catch(ErrnoException failed){throw new IOException("RESTORE_ASSET_DIRECTORY_SYNC",failed);}finally{if(fd!=null)try{Os.close(fd);}catch(ErrnoException ignored){}}
    }
    static Totals copy(File original,File destination,String[] relativeRoots)throws IOException {
        Totals totals=new Totals();File manifest=child(destination,MEMBER);
        if(manifest.exists())throw new IOException("RESTORE_ASSET_MANIFEST_ALREADY_EXISTS");
        try(FileOutputStream file=new FileOutputStream(manifest);DataOutputStream out=new DataOutputStream(new BufferedOutputStream(file,32768))){
            out.writeInt(MAGIC);for(String relative:relativeRoots)copyTree(original,destination,relative,out,totals,0);
            out.writeUTF("");out.writeLong(totals.files);out.writeLong(totals.bytes);out.flush();file.getFD().sync();
        }
        sync(destination);return totals;
    }
    private static void copyTree(File original,File destination,String relative,DataOutputStream manifest,Totals totals,int depth)throws IOException {
        check();if(relative.equals("files/r26-checkpoints-cold-r324/prepare"))return;
        File source=child(original,relative);if(!source.exists())return;if(depth>16)throw new IOException("WAIT_RESTORE_ASSET_DEPTH");File target=child(destination,relative);
        if(source.isDirectory()){
            if(!target.mkdirs()&&!target.isDirectory())throw new IOException("RESTORE_ASSET_DIRECTORY");sync(target.getParentFile());
            try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(source)){File next;while((next=cursor.next())!=null)copyTree(original,destination,relative+"/"+next.getName(),manifest,totals,depth+1);}sync(target);return;
        }
        if(!source.isFile()||target.exists())throw new IOException("RESTORE_ASSET_REGULAR_OR_DUPLICATE");
        long bytes=source.length(),modified=source.lastModified();if(target.getParentFile().getUsableSpace()<Math.addExact(bytes,FREE_RESERVE))throw new IOException("WAIT_RESTORE_ASSET_DISK_CAPACITY");
        String hash=hash(source);long written=0;
        try(InputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(target)){
            byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){check();written=Math.addExact(written,n);if(written>bytes)throw new IOException("RESTORE_ASSET_GREW");out.write(buffer,0,n);}out.getFD().sync();
        }
        if(written!=bytes||source.length()!=bytes||source.lastModified()!=modified||!hash.equals(hash(source))||!hash.equals(hash(target)))throw new IOException("RESTORE_ASSET_CHANGED");
        sync(target.getParentFile());manifest.writeUTF(relative);manifest.writeLong(bytes);manifest.writeUTF(hash);
        totals.files=Math.addExact(totals.files,1);totals.bytes=Math.addExact(totals.bytes,bytes);
    }
    static Totals verify(File root,File manifest)throws IOException {
        if(!manifest.getAbsoluteFile().equals(manifest.getCanonicalFile())||!manifest.isFile())throw new IOException("RESTORE_ASSET_MANIFEST_PATH");Totals totals=new Totals();
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(manifest),32768))){
            if(in.readInt()!=MAGIC)throw new IOException("RESTORE_ASSET_MANIFEST_VERSION");
            for(;;){check();String relative=in.readUTF();if(relative.isEmpty()){if(in.readLong()!=totals.files||in.readLong()!=totals.bytes||in.read()!=-1)throw new IOException("RESTORE_ASSET_MANIFEST_FOOTER");break;}
                long bytes=in.readLong();String expected=in.readUTF();if(bytes<0||!expected.matches("[0-9a-f]{64}"))throw new IOException("RESTORE_ASSET_MANIFEST_RECORD");
                File file=child(root,relative);if(!file.isFile()||file.length()!=bytes||!expected.equals(hash(file)))throw new IOException("RESTORE_ASSET_BYTES_CHANGED");
                totals.files=Math.addExact(totals.files,1);totals.bytes=Math.addExact(totals.bytes,bytes);
            }
        }return totals;
    }
    static String hash(File file)throws IOException {
        check();if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.isFile())throw new IOException("RESTORE_ASSET_HASH_PATH");
        MessageDigest sha;try{sha=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){check();sha.update(buffer,0,n);}}
        StringBuilder out=new StringBuilder(64);for(byte b:sha.digest())out.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));return out.toString();
    }
    static boolean removePrivateTree(File root,File file,int depth)throws IOException {
        check();if(depth>20||!file.getAbsoluteFile().equals(file.getCanonicalFile())||(!file.equals(root)&&!file.getPath().startsWith(root.getPath()+File.separator)))throw new IOException("RESTORE_WORKSPACE_RELEASE_PATH");
        if(file.isDirectory())try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(file)){File child;while((child=cursor.next())!=null)if(!removePrivateTree(root,child,depth+1))return false;}
        return !file.exists()||file.delete();
    }
}
