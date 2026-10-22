package com.ethk3.novaonepro;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.DigestOutputStream;
import java.security.MessageDigest;

/** Exact UTF-8 files and bounded previews: never send the entire report through Binder. */
public final class DiagnosticFileStoreR941 {
    public static final int PREVIEW_CHARS = 16000;
    private DiagnosticFileStoreR941() {}
    public static File write(File directory, String body) throws Exception {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("REPORT_DIRECTORY");
        File partial = File.createTempFile("detail_", ".partial", directory);
        boolean published = false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (FileOutputStream bytes = new FileOutputStream(partial);
                 DigestOutputStream hashed = new DigestOutputStream(bytes, digest);
                 Writer writer = new BufferedWriter(new OutputStreamWriter(hashed, StandardCharsets.UTF_8), 8192)) {
                String exact = body == null ? "" : body;
                for (int offset = 0; offset < exact.length(); offset += 8192)
                    writer.write(exact, offset, Math.min(8192, exact.length() - offset));
                writer.flush(); bytes.getFD().sync();
            }
            byte[] expectedHash = digest.digest();
            StringBuilder hex = new StringBuilder(64);
            for (byte value : expectedHash) hex.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            File ready = new File(directory, "R923_" + hex + ".txt");
            if (ready.isFile()) {
                if (ready.length() != partial.length()) throw new IOException("REPORT_HASH_LENGTH_CONFLICT");
                // A content-addressed name and equal length do not prove existing bytes are intact.
                // Reject corruption without publishing success or discarding the conflicting artifact.
                MessageDigest existingHash = MessageDigest.getInstance("SHA-256");
                try (InputStream existing = new FileInputStream(ready)) {
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = existing.read(buffer)) != -1) existingHash.update(buffer, 0, count);
                }
                if (!MessageDigest.isEqual(expectedHash, existingHash.digest()))
                    throw new IOException("REPORT_HASH_CONTENT_CONFLICT");
                if (!partial.delete()) throw new IOException("REPORT_PARTIAL_DELETE");
            } else if (!partial.renameTo(ready)) throw new IOException("REPORT_PUBLISH");
            published = true; prune(directory,ready); return ready;
        } finally { if (!published) partial.delete(); }
    }
    public static File resolve(File directory, String name) throws IOException {
        if (name == null || !name.matches("R923_[0-9a-f]{64}\\.(txt|zip)")) throw new IOException("REPORT_NAME");
        File parent = directory.getCanonicalFile(), file = new File(parent, name).getCanonicalFile();
        if (!parent.equals(file.getParentFile()) || !file.isFile()) throw new IOException("REPORT_MISSING");
        return file;
    }
    public static String preview(File file) throws IOException {
        char[] buffer = new char[PREVIEW_CHARS + 1]; int count = 0;
        try (Reader reader = new BufferedReader(new InputStreamReader(openPreview(file), StandardCharsets.UTF_8))) {
            while (count < buffer.length) { int n = reader.read(buffer, count, buffer.length - count); if (n < 0) break; count += n; }
        }
        if (count <= PREVIEW_CHARS) return new String(buffer, 0, count);
        int end = PREVIEW_CHARS; if (Character.isHighSurrogate(buffer[end - 1])) end--;
        return "全文已保存（" + file.length() + "字节）。以下为预览，点击导出可取得完整原文（ZIP为无损压缩）。\n\n" + new String(buffer, 0, end);
    }
    private static InputStream openPreview(File file)throws IOException {
        if(!file.getName().endsWith(".zip"))return new FileInputStream(file);
        java.util.zip.ZipInputStream in=new java.util.zip.ZipInputStream(new FileInputStream(file));
        try {java.util.zip.ZipEntry entry=in.getNextEntry();if(entry==null||!"report.txt".equals(entry.getName()))throw new IOException("REPORT_ZIP_ENTRY");return in;}
        catch(IOException error){in.close();throw error;}
    }
    /** Only generated cache files; the file currently shared remains valid. */
    public static void prune(File directory,File keep) {
        File[] files=directory.listFiles();if(files==null)return;
        java.util.Arrays.sort(files,new java.util.Comparator<File>(){public int compare(File a,File b){return Long.compare(b.lastModified(),a.lastModified());}});
        long bytes=keep!=null&&keep.isFile()?keep.length():0;int count=keep==null?0:1;
        long now=System.currentTimeMillis();
        for(File f:files){
            if(f.equals(keep)||!f.isFile())continue;
            if(f.getName().matches("R923_[0-9a-f]{64}\\.(txt|zip|csv|jsonl)")){
                if(count>=3||bytes+f.length()>48L*1024*1024)f.delete();else{count++;bytes+=f.length();}
            }else if(f.getName().endsWith(".partial")&&now-f.lastModified()>3600000L)f.delete();
        }
    }
}
