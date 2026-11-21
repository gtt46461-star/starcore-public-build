package com.ethk3.novaonepro;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Exact bounded archive of one r926 original plus the restore owner's complete
 * asset roots. Writes directly to the granted destination; imports only into a
 * newly created private probe tree. No local source deletion exists here. */
final class LegacyCustodyArchiveR335 {
    static final String MEMBER="R335-LEGACY-CUSTODY.bin";
    static final int MAGIC=0x335ca501,MAX_FILES=16384,MAX_INDEX=4<<20;
    static final long MAX_PAYLOAD=8L<<30,MAX_ARCHIVE=MAX_PAYLOAD+(256L<<20);
    static final String[] ASSETS={"files/r26-checkpoints","files/r26-checkpoints-cold-r324","files/r31-keep","files/model-bundles-r929","files/trusted-model-key-r932.der","files/r31-history-inputs","files/research-input-v2","files/r335-fault-originals","files/r335-checkpoint-recovery","databases/su1-archives"};
    static final class Row {final String path,sha;final long bytes;Row(String p,long b,String s){path=p;bytes=b;sha=s;}}
    static final class Index {
        final String backup,sourceSha,manifestSha;final long sourceBytes,total;final List<Row> rows;final byte[] encoded;
        Index(String b,String s,String m,long n,long t,List<Row> r,byte[] e){backup=b;sourceSha=s;manifestSha=m;sourceBytes=n;total=t;rows=Collections.unmodifiableList(r);encoded=e;}
        String sha()throws IOException{return digest(encoded);}
    }
    static final class Transfer {final long bytes;final String sha;Transfer(long b,String s){bytes=b;sha=s;}}
    private static MessageDigest hash()throws IOException{try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new IOException(impossible);}}
    static String hex(byte[] bytes){StringBuilder s=new StringBuilder();for(byte b:bytes)s.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));return s.toString();}
    static String digest(byte[] bytes)throws IOException{return hex(hash().digest(bytes));}
    static void check()throws IOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R335_CUSTODY_CANCELLED");}
    private static void validateRelative(String relative)throws IOException {
        if(relative==null||relative.isEmpty()||relative.length()>4096||relative.startsWith("/")||relative.contains("\\"))throw new IOException("R335_CUSTODY_MEMBER_PATH");
        for(String part:relative.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IOException("R335_CUSTODY_MEMBER_PATH");
        for(int i=0;i<relative.length();i++)if(Character.isISOControl(relative.charAt(i)))throw new IOException("R335_CUSTODY_MEMBER_CONTROL");
    }
    static File child(File root,String relative)throws IOException {
        validateRelative(relative);
        if(relative==null||relative.isEmpty()||relative.length()>4096||relative.startsWith("/")||relative.contains("\\"))throw new IOException("R335_CUSTODY_MEMBER_PATH");
        for(String part:relative.split("/",-1))if(part.isEmpty()||part.equals(".")||part.equals(".."))throw new IOException("R335_CUSTODY_MEMBER_PATH");
        for(int i=0;i<relative.length();i++)if(Character.isISOControl(relative.charAt(i)))throw new IOException("R335_CUSTODY_MEMBER_CONTROL");
        File base=root.getCanonicalFile(),file=new File(base,relative).getAbsoluteFile();
        if(!file.equals(file.getCanonicalFile())||!file.getPath().startsWith(base.getPath()+File.separator))throw new IOException("R335_CUSTODY_MEMBER_ESCAPE");return file;
    }
    static boolean asset(String path){for(String root:ASSETS)if(path.equals(root)||path.startsWith(root+"/"))return !path.startsWith("files/r26-checkpoints-cold-r324/prepare/")&&!path.equals("files/r26-checkpoints-cold-r324/prepare");return false;}
    private static boolean allowed(String path,String backup){return path.equals("databases/"+backup)||path.equals("databases/"+backup+".manifest.txt")||asset(path)||path.matches("runtime/apk/(base|split-[0-9]{2})\\.apk");}
    static Index capture(File dataRoot,String backup)throws IOException{return capture(dataRoot,backup,Collections.emptyMap());}
    static Index capture(File dataRoot,String backup,Map<String,File> runtime)throws IOException {
        LegacyBackupRecoveryR335.requireName(backup);ArrayList<Row> rows=new ArrayList<>();long[] count={0,0,512};
        add(dataRoot,"databases/"+backup,rows,count,0);add(dataRoot,"databases/"+backup+".manifest.txt",rows,count,0);
        for(String root:ASSETS)add(dataRoot,root,rows,count,0);
        if(runtime.size()>33)throw new IOException("R335_CUSTODY_APK_COUNT_BOUND");
        for(Map.Entry<String,File> part:runtime.entrySet()){if(rows.size()>=MAX_FILES)throw new IOException("R335_CUSTODY_REGULAR_OR_FILE_BOUND");if(!part.getKey().matches("runtime/apk/(base|split-[0-9]{2})\\.apk"))throw new IOException("R335_CUSTODY_APK_PATH");long length=part.getValue().length();if(length<1)throw new IOException("R335_CUSTODY_APK_MISSING");count[1]=Math.addExact(count[1],length);if(count[1]>MAX_PAYLOAD)throw new IOException("R335_CUSTODY_PAYLOAD_BOUND");accountIndex(count,part.getKey());rows.add(new Row(part.getKey(),length,StorageRestoreAssetsR324.hash(part.getValue())));}
        rows.sort(Comparator.comparing(row->row.path));
        Row source=null,manifest=null;for(Row row:rows){if(row.path.equals("databases/"+backup))source=row;if(row.path.equals("databases/"+backup+".manifest.txt"))manifest=row;}
        if(source==null||manifest==null||source.bytes<512||manifest.bytes<1||manifest.bytes>131072)throw new IOException("R335_CUSTODY_ORIGINAL_REQUIRED");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){
            out.writeInt(MAGIC);out.writeUTF(backup);out.writeUTF(source.sha);out.writeLong(source.bytes);out.writeUTF(manifest.sha);out.writeLong(count[1]);out.writeInt(rows.size());
            for(Row row:rows){out.writeUTF(row.path);out.writeLong(row.bytes);out.writeUTF(row.sha);if(bytes.size()>MAX_INDEX)throw new IOException("R335_CUSTODY_INDEX_BOUND");}
        }return new Index(backup,source.sha,manifest.sha,source.bytes,count[1],rows,bytes.toByteArray());
    }
    private static void add(File root,String path,List<Row> rows,long[] counts,int depth)throws IOException {
        check();if(path.equals("files/r26-checkpoints-cold-r324/prepare"))return;
        if(depth>16||++counts[0]>MAX_FILES*2L)throw new IOException("R335_CUSTODY_ENUMERATION_BOUND");File file=child(root,path);if(!file.exists())return;
        if(file.isDirectory()){try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(file)){File next;while((next=cursor.next())!=null)add(root,path+"/"+next.getName(),rows,counts,depth+1);}return;}
        if(!file.isFile()||rows.size()>=MAX_FILES)throw new IOException("R335_CUSTODY_REGULAR_OR_FILE_BOUND");
        long bytes=file.length();counts[1]=Math.addExact(counts[1],bytes);if(counts[1]>MAX_PAYLOAD)throw new IOException("R335_CUSTODY_PAYLOAD_BOUND");
        accountIndex(counts,path);rows.add(new Row(path,bytes,StorageRestoreAssetsR324.hash(file)));
    }
    private static void accountIndex(long[] counts,String path)throws IOException {
        long utf=0;for(int i=0;i<path.length();i++){int c=path.charAt(i);utf+=c>=1&&c<=127?1:c<=2047?2:3;}
        counts[2]=Math.addExact(counts[2],2+utf+8+2+64);if(counts[2]>MAX_INDEX)throw new IOException("R335_CUSTODY_INDEX_BOUND_BEFORE_ROW_ALLOCATION");
    }
    static Index decode(byte[] bytes)throws IOException {
        if(bytes.length<1||bytes.length>MAX_INDEX)throw new IOException("R335_CUSTODY_INDEX_BOUND");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=MAGIC)throw new IOException("R335_CUSTODY_INDEX_VERSION");String backup=in.readUTF();LegacyBackupRecoveryR335.requireName(backup);
            String source=in.readUTF();long size=in.readLong();String manifest=in.readUTF();long total=in.readLong();int count=in.readInt();
            if(!source.matches("[0-9a-f]{64}")||!manifest.matches("[0-9a-f]{64}")||size<512||total<size||total>MAX_PAYLOAD||count<2||count>MAX_FILES)throw new IOException("R335_CUSTODY_INDEX_IDENTITY");
            List<Row> rows=new ArrayList<>();String previous="";long sum=0;boolean haveSource=false,haveManifest=false;
            for(int i=0;i<count;i++){String path=in.readUTF();long length=in.readLong();String sha=in.readUTF();
                if(path.compareTo(previous)<=0||!allowed(path,backup)||length<0||!sha.matches("[0-9a-f]{64}"))throw new IOException("R335_CUSTODY_INDEX_MEMBER");
                validateRelative(path);previous=path;sum=Math.addExact(sum,length);if(sum>MAX_PAYLOAD)throw new IOException("R335_CUSTODY_PAYLOAD_BOUND");
                if(path.equals("databases/"+backup)){if(length!=size||!sha.equals(source))throw new IOException("R335_CUSTODY_SOURCE_MEMBER");haveSource=true;}
                if(path.equals("databases/"+backup+".manifest.txt")){if(length<1||length>131072||!sha.equals(manifest))throw new IOException("R335_CUSTODY_MANIFEST_MEMBER");haveManifest=true;}
                rows.add(new Row(path,length,sha));
            }
            if(!haveSource||!haveManifest||sum!=total||in.read()!=-1)throw new IOException("R335_CUSTODY_INDEX_FOOTER");return new Index(backup,source,manifest,size,total,rows,bytes.clone());
        }catch(IllegalArgumentException|ArithmeticException invalid){throw new IOException("R335_CUSTODY_INDEX_INVALID",invalid);}
    }
    static Transfer write(File dataRoot,Index index,OutputStream destination)throws IOException{return write(dataRoot,index,destination,Collections.emptyMap());}
    static Transfer write(File dataRoot,Index index,OutputStream destination,Map<String,File> runtime)throws IOException {
        MessageDigest hash=hash();CountingOutput counter=new CountingOutput(destination,hash);ZipOutputStream zip=new ZipOutputStream(counter);zip.setLevel(0);
        try(ZipOutputStream out=zip){out.putNextEntry(new ZipEntry(MEMBER));out.write(index.encoded);out.closeEntry();
            byte[] buffer=new byte[65536];
            for(Row row:index.rows){check();File source=runtime.containsKey(row.path)?runtime.get(row.path):child(dataRoot,row.path);if(!source.isFile()||source.length()!=row.bytes)throw new IOException("R335_CUSTODY_SOURCE_CHANGED");
                out.putNextEntry(new ZipEntry(row.path));MessageDigest content=hash();long read=0;
                try(InputStream in=new FileInputStream(source)){int n;while((n=in.read(buffer))!=-1){check();read=Math.addExact(read,n);if(read>row.bytes)throw new IOException("R335_CUSTODY_SOURCE_GREW");content.update(buffer,0,n);out.write(buffer,0,n);}}
                if(read!=row.bytes||!row.sha.equals(hex(content.digest())))throw new IOException("R335_CUSTODY_SOURCE_CHANGED");out.closeEntry();
            }
        }
        if(!Arrays.equals(capture(dataRoot,index.backup,runtime).encoded,index.encoded))throw new IOException("R335_CUSTODY_SOURCE_SET_CHANGED");
        return new Transfer(counter.count,hex(hash.digest()));
    }
    static Index readIndex(InputStream source)throws IOException {
        try(ZipInputStream zip=new ZipInputStream(source)){ZipEntry first=zip.getNextEntry();if(first==null||first.isDirectory()||!MEMBER.equals(first.getName()))throw new IOException("R335_CUSTODY_INDEX_FIRST_REQUIRED");ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[65536];int n;
            while((n=zip.read(buffer))!=-1){check();if(n>MAX_INDEX-out.size())throw new IOException("R335_CUSTODY_INDEX_BOUND");out.write(buffer,0,n);}return decode(out.toByteArray());}
    }
    static Transfer measure(InputStream source)throws IOException {
        MessageDigest hash=hash();long bytes=0;try(InputStream in=source){byte[] buffer=new byte[65536];int n;while((n=in.read(buffer))!=-1){check();bytes=Math.addExact(bytes,n);if(bytes>MAX_ARCHIVE)throw new IOException("R335_CUSTODY_ARCHIVE_BOUND");hash.update(buffer,0,n);}}
        if(bytes<1)throw new IOException("R335_CUSTODY_EMPTY_ARCHIVE");return new Transfer(bytes,hex(hash.digest()));
    }
    static void extract(InputStream source,Index expected,Transfer transfer,File root)throws IOException {
        try(InputStream ownership=source){
        if(root.exists()||!root.mkdirs())throw new IOException("R335_CUSTODY_IMPORT_EXCLUSIVE_ROOT");MessageDigest archiveHash=hash();CountingInput counted=new CountingInput(ownership,archiveHash,transfer.bytes);
        try(InputStream owned=counted;ZipInputStream zip=new ZipInputStream(owned)){
            ZipEntry first=zip.getNextEntry();if(first==null||first.isDirectory()||!MEMBER.equals(first.getName()))throw new IOException("R335_CUSTODY_INDEX_FIRST_REQUIRED");
            ByteArrayOutputStream manifest=new ByteArrayOutputStream();byte[] buffer=new byte[65536];int n;while((n=zip.read(buffer))!=-1){check();if(n>MAX_INDEX-manifest.size())throw new IOException("R335_CUSTODY_INDEX_BOUND");manifest.write(buffer,0,n);}
            byte[] encoded=manifest.toByteArray();if(!Arrays.equals(encoded,expected.encoded))throw new IOException("R335_CUSTODY_IMPORTED_INDEX_CHANGED");zip.closeEntry();
            for(Row row:expected.rows){ZipEntry entry=zip.getNextEntry();if(entry==null||entry.isDirectory()||!row.path.equals(entry.getName())||(entry.getSize()>=0&&entry.getSize()!=row.bytes))throw new IOException("R335_CUSTODY_ENTRY_SET_OR_SIZE");
                File target=child(root,row.path);File parent=target.getParentFile();if(!parent.mkdirs()&&!parent.isDirectory())throw new IOException("R335_CUSTODY_IMPORT_PARENT");
                if(!target.createNewFile())throw new IOException("R335_CUSTODY_IMPORT_DUPLICATE");MessageDigest content=hash();long bytes=0;
                try(FileOutputStream out=new FileOutputStream(target)){while((n=zip.read(buffer))!=-1){check();bytes=Math.addExact(bytes,n);if(bytes>row.bytes)throw new IOException("R335_CUSTODY_MEMBER_SIZE_BOUND");content.update(buffer,0,n);out.write(buffer,0,n);}out.getFD().sync();}
                if(bytes!=row.bytes||!row.sha.equals(hex(content.digest())))throw new IOException("R335_CUSTODY_MEMBER_HASH");sync(parent);zip.closeEntry();
            }
            if(zip.getNextEntry()!=null)throw new IOException("R335_CUSTODY_UNDECLARED_OR_DUPLICATE_ENTRY");
            // ZipInputStream may prefetch central-directory bytes; these already
            // passed counted. Drain the remaining raw stream before whole SHA.
            while(counted.read(buffer)!=-1)check();
            if(counted.count!=transfer.bytes||!transfer.sha.equals(hex(archiveHash.digest())))throw new IOException("R335_CUSTODY_WHOLE_ARCHIVE_HASH");
        }sync(root);
        }
    }
    static void verifyImported(File root,Index index)throws IOException {for(Row row:index.rows){File file=child(root,row.path);if(!file.isFile()||file.length()!=row.bytes||!row.sha.equals(StorageRestoreAssetsR324.hash(file)))throw new IOException("R335_CUSTODY_IMPORTED_MEMBER_CHANGED");}}
    static void writeAssetManifest(File root,Index index)throws IOException {
        File target=child(root,StorageRestoreAssetsR324.MEMBER);if(target.exists())throw new IOException("R335_CUSTODY_ASSET_MANIFEST_EXISTS");long files=0,bytes=0;
        try(FileOutputStream file=new FileOutputStream(target);DataOutputStream out=new DataOutputStream(new BufferedOutputStream(file))){out.writeInt(0x32415301);
            for(Row row:index.rows)if(asset(row.path)){out.writeUTF(row.path);out.writeLong(row.bytes);out.writeUTF(row.sha);files++;bytes=Math.addExact(bytes,row.bytes);}
            out.writeUTF("");out.writeLong(files);out.writeLong(bytes);out.flush();file.getFD().sync();
        }sync(root);StorageRestoreAssetsR324.verify(root,target);
    }
    static void writeExact(File file,byte[] bytes)throws IOException {if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.createNewFile())throw new IOException("R335_CUSTODY_EXACT_FILE_EXISTS");try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);out.getFD().sync();}sync(file.getParentFile());}
    static void sync(File directory)throws IOException {StorageMaintenanceFileR82.directorySyncR31.sync(directory.getCanonicalPath());}
    private static final class CountingOutput extends FilterOutputStream {
        final MessageDigest hash;long count;CountingOutput(OutputStream out,MessageDigest h){super(out);hash=h;}
        @Override public void write(int b)throws IOException{byte[] one={(byte)b};write(one,0,1);}
        @Override public void write(byte[] b,int off,int n)throws IOException{check();count=Math.addExact(count,n);if(count>MAX_ARCHIVE)throw new IOException("R335_CUSTODY_ARCHIVE_BOUND");out.write(b,off,n);hash.update(b,off,n);}
    }
    private static final class CountingInput extends FilterInputStream {
        final MessageDigest hash;final long limit;long count;CountingInput(InputStream in,MessageDigest h,long n)throws IOException{super(in);if(n<1||n>MAX_ARCHIVE)throw new IOException("R335_CUSTODY_ARCHIVE_BOUND");hash=h;limit=n;}
        @Override public int read()throws IOException{byte[] one=new byte[1];int n=read(one,0,1);return n<0?-1:one[0]&255;}
        @Override public int read(byte[] b,int off,int len)throws IOException{check();int n=in.read(b,off,len);if(n>0){count=Math.addExact(count,n);if(count>limit)throw new IOException("R335_CUSTODY_ARCHIVE_GREW");hash.update(b,off,n);}return n;}
    }
}
