package com.ethk3.novaonepro;

import android.util.AtomicFile;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** Bounded optional audit progress. Never opens the business database or grants model/GC authority. */
final class CheckpointAuditContinuationR343 {
    static final String FILE = "r343-checkpoint-audit-cursor.bin";
    static final int MAX_NODES = 16384;
    private static final int MAGIC = 0x52333433;
    private static final int HASH = 32, SOURCE = 40, DATABASE = 36;
    private static final int HEADER = Integer.BYTES + SOURCE + DATABASE + 2 * HASH + 1 + Integer.BYTES;
    private static final int ROW = 2 * HASH + 1;
    private static final int MAX_BYTES = HEADER + MAX_NODES * ROW + HASH;
    private static final byte[] EMPTY_HASH = new byte[HASH];

    private final File file;
    private final String snapshot, database, source, rootsSha256;
    private final Map<String,String> previous = new TreeMap<>();
    private final Set<String> deferredPrevious = new TreeSet<>();
    private String opened = "FIRST_PAGE", previousSnapshot;
    private boolean previousTruncated;

    private CheckpointAuditContinuationR343(File file, String snapshot, String database, String source,
            String rootsSha256) {
        this.file=file; this.snapshot=snapshot; this.database=database; this.source=source;
        this.rootsSha256=rootsSha256;
    }

    static CheckpointAuditContinuationR343 open(File file, String snapshot, String database,
            String source, Set<String> sortedRoots, boolean omittedRoots) {
        MessageDigest roots=sha();
        for(String hash:sortedRoots) roots.update(hash.getBytes(StandardCharsets.US_ASCII));
        CheckpointAuditContinuationR343 page=new CheckpointAuditContinuationR343(
                file,snapshot,database,source,hex(roots.digest()));
        if(omittedRoots || !validHash(snapshot) || database==null || !database.matches("[a-f0-9-]{36}")
                || source==null || !source.matches("[a-f0-9]{40}")) {
            page.opened="DISABLED_UNBOUND_SOURCE";
            return page;
        }
        try {
            page.read();
            if("RESUMED_EXACT_ROOTS".equals(page.opened) && !page.previous.keySet().containsAll(sortedRoots))
                throw new IOException("R343_CHECKPOINT_CURSOR_ROOTS_INCOMPLETE");
        } catch(FileNotFoundException first) {
            // No earlier cursor exists.
        } catch(IOException|SecurityException unavailable) {
            page.opened="CURSOR_UNAVAILABLE_RESTARTED";page.previousSnapshot=null;
            page.previous.clear();page.deferredPrevious.clear();page.previousTruncated=false;
        }
        return page;
    }

    Set<String> previousHashes() { return Collections.unmodifiableSet(previous.keySet()); }
    Set<String> deferredPrevious() { return Collections.unmodifiableSet(deferredPrevious); }
    String verifiedIn(String hash) { return previous.get(hash); }
    boolean wasTruncated() { return previousTruncated; }

    JSONObject finish(Set<String> reached, Set<String> verifiedThisPage, Set<String> deferred,
            long oversized, long historical, boolean truncated, BooleanSupplier continuing) throws Exception {
        boolean complete=deferred.isEmpty() && !truncated;
        String saved="DISABLED_UNBOUND_SOURCE";
        if(!"DISABLED_UNBOUND_SOURCE".equals(opened)) {
            try {
                if(reached.size()>MAX_NODES)throw new IOException("R343_CHECKPOINT_CURSOR_NODE_BOUND");
                write(reached,verifiedThisPage,deferred,(complete?1:0)|(truncated?2:0),continuing);
                saved="DURABLE";
            } catch(java.io.InterruptedIOException cancelled) {
                throw cancelled;
            } catch(IOException|SecurityException unavailable) {
                // Cursor placement is optional. Report the current observations even
                // when metadata admission fails and the former cursor must remain.
                saved="CURSOR_WRITE_UNAVAILABLE";
            }
        }
        return new JSONObject().put("format","R343_CHECKPOINT_AUDIT_PAGES_1")
                .put("rootSetSha256",rootsSha256).put("snapshotSha256",snapshot)
                .put("previousPageSnapshotSha256",previousSnapshot==null?JSONObject.NULL:previousSnapshot)
                .put("progressOpenState",opened).put("progressWriteState",saved)
                .put("newlyByteVerifiedObjects",verifiedThisPage.size())
                .put("historicalPreviouslyVerifiedObjects",historical)
                .put("deferredObjectsThisPage",deferred.size()).put("objectsOverOnePageBudget",oversized)
                .put("pageSweepComplete",complete)
                .put("evidenceScope","PREVIOUS_PAGES_SUPPLY_DIAGNOSTIC_PROGRESS_ONLY; CURRENT_BYTES_UNVERIFIED_WHERE_REUSED");
    }

    private void read() throws IOException {
        checkPath();
        AtomicFile atomic=new AtomicFile(file);
        MessageDigest seal=sha();
        try(DataInputStream raw=new DataInputStream(new BufferedInputStream(atomic.openRead()))) {
            if(file.length()<HEADER+HASH || file.length()>MAX_BYTES)
                throw new IOException("R343_CHECKPOINT_CURSOR_LENGTH");
            DataInputStream data=new DataInputStream(new DigestInputStream(raw,seal));
            if(data.readInt()!=MAGIC)throw new IOException("R343_CHECKPOINT_CURSOR_FORMAT");
            String storedSource=ascii(data,SOURCE),storedDatabase=ascii(data,DATABASE);
            String storedRoots=hex(read(data,HASH)),storedSnapshot=hex(read(data,HASH));
            if(!source.equals(storedSource)||!database.equals(storedDatabase)||!rootsSha256.equals(storedRoots)) {
                opened="NEW_SOURCE_OR_ROOT_SET";return;
            }
            int flags=data.readUnsignedByte(),count=data.readInt();
            if((flags&~3)!=0 || flags==3 || count<0 || count>MAX_NODES || file.length()!=size(count))
                throw new IOException("R343_CHECKPOINT_CURSOR_SCOPE");
            String last=null;
            for(int index=0;index<count;index++) {
                String hash=hex(read(data,HASH));
                int disposition=data.readUnsignedByte();byte[] original=read(data,HASH);
                if((last!=null&&last.compareTo(hash)>=0) || disposition>2
                        || (disposition!=1&&!MessageDigest.isEqual(original,EMPTY_HASH)))
                    throw new IOException("R343_CHECKPOINT_CURSOR_ORDER");
                previous.put(hash,disposition==1?hex(original):null);last=hash;
                if(disposition==2)deferredPrevious.add(hash);
            }
            if(!MessageDigest.isEqual(seal.digest(),read(raw,HASH)) || raw.read()!=-1)
                throw new IOException("R343_CHECKPOINT_CURSOR_DIGEST");
            previousSnapshot=storedSnapshot;
            if((flags&1)!=0){previous.clear();deferredPrevious.clear();opened="NEW_SWEEP_AFTER_COMPLETE_PAGE";}
            else{opened="RESUMED_EXACT_ROOTS";previousTruncated=(flags&2)!=0;}
        }
    }

    private void write(Set<String> reached, Set<String> verifiedThisPage, Set<String> deferred, int flags,
            BooleanSupplier continuing) throws IOException {
        checkPath();check(continuing);
        java.util.TreeSet<String> sorted=new java.util.TreeSet<>(reached);
        try(ReportIoR31.AtomicPermit permit=ReportIoR31.beforeAtomic(file,size(sorted.size()))) {
            AtomicFile atomic=new AtomicFile(file);FileOutputStream descriptor=null;
            try {
                descriptor=atomic.startWrite();
                BufferedOutputStream raw=new BufferedOutputStream(permit.stream(descriptor));
                MessageDigest seal=sha();
                DataOutputStream data=new DataOutputStream(new DigestOutputStream(raw,seal));
                data.writeInt(MAGIC);data.write(source.getBytes(StandardCharsets.US_ASCII));
                data.write(database.getBytes(StandardCharsets.US_ASCII));data.write(bytes(rootsSha256));
                data.write(bytes(snapshot));data.writeByte(flags);data.writeInt(sorted.size());
                for(String hash:sorted) {
                    check(continuing);
                    String proof=verifiedThisPage.contains(hash)?snapshot:verifiedIn(hash);
                    data.write(bytes(hash));data.writeByte(proof!=null?1:deferred.contains(hash)?2:0);
                    data.write(proof==null?EMPTY_HASH:bytes(proof));
                }
                data.flush();byte[] expectedSeal=seal.digest();
                raw.write(expectedSeal);raw.flush();descriptor.getFD().sync();
                check(continuing);atomic.finishWrite(descriptor);descriptor=null;
                // Android AtomicFile may log a failed rename and return normally.
                // Check the durable generation before publishing a progress receipt.
                try(RandomAccessFile stored=new RandomAccessFile(file,"r")){
                    if(stored.length()!=size(sorted.size()))
                        throw new IOException("R343_CHECKPOINT_CURSOR_WRITE_LENGTH");
                    stored.seek(stored.length()-HASH);
                    byte[] storedSeal=new byte[HASH];stored.readFully(storedSeal);
                    if(!MessageDigest.isEqual(expectedSeal,storedSeal))
                        throw new IOException("R343_CHECKPOINT_CURSOR_WRITE_DIGEST");
                }
            } finally {if(descriptor!=null)atomic.failWrite(descriptor);}
        }
    }

    private void checkPath() throws IOException {
        for(File candidate:new File[]{file,new File(file.getPath()+".bak"),new File(file.getPath()+".new")})
            if(!candidate.getAbsoluteFile().equals(candidate.getCanonicalFile()))
                throw new IOException("R343_CHECKPOINT_CURSOR_SYMLINK");
    }
    private static void check(BooleanSupplier continuing) throws IOException {
        if(Thread.currentThread().isInterrupted()||!continuing.getAsBoolean())
            throw new java.io.InterruptedIOException("REPORT_CHECKPOINT_AUDIT_CANCELLED");
    }
    private static int size(int count) { return HEADER+count*ROW+HASH; }
    private static boolean validHash(String hash) { return hash!=null && hash.matches("[a-f0-9]{64}"); }
    private static byte[] read(DataInputStream in,int size) throws IOException {
        byte[] data=new byte[size];in.readFully(data);return data;
    }
    private static String ascii(DataInputStream in,int size) throws IOException {
        return new String(read(in,size),StandardCharsets.US_ASCII);
    }
    private static MessageDigest sha() {
        try{return MessageDigest.getInstance("SHA-256");}
        catch(NoSuchAlgorithmException unavailable){throw new AssertionError(unavailable);}
    }
    private static byte[] bytes(String hex) throws IOException {
        if(!validHash(hex))throw new IOException("R343_CHECKPOINT_CURSOR_HASH");
        byte[] out=new byte[HASH];
        for(int i=0;i<HASH;i++)out[i]=(byte)((Character.digit(hex.charAt(2*i),16)<<4)
                |Character.digit(hex.charAt(2*i+1),16));
        return out;
    }
    private static String hex(byte[] bytes) {
        StringBuilder hex=new StringBuilder(2*bytes.length);
        for(byte value:bytes)hex.append(Character.forDigit((value>>>4)&15,16)).append(Character.forDigit(value&15,16));
        return hex.toString();
    }
}
