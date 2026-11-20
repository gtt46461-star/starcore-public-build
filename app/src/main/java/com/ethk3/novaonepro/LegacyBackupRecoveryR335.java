package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;

/** Explicit single-source bridge to the existing independent Android restore
 * collector. This owner has no live replacement or original-deletion API. */
public final class LegacyBackupRecoveryR335 {
    private LegacyBackupRecoveryR335(){}
    private static final long MAX_SOURCE=8L<<30,RESERVE=128L<<20;
    private static final String[] ASSETS={"files/r26-checkpoints","files/r26-checkpoints-cold-r324","files/r31-keep",
        "files/model-bundles-r929","files/trusted-model-key-r932.der","files/r31-history-inputs","files/research-input-v2",
        "files/r335-fault-originals","files/r335-checkpoint-recovery","databases/su1-archives"};
    public static void requireName(String name){
        String pattern=java.util.regex.Pattern.quote(AppConfig.DB_NAME)+"\\.r926\\.[0-9]{1,12}\\.[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.bak";
        if(name==null||!name.matches(pattern))throw new IllegalArgumentException("R335_LEGACY_SOURCE_NAME");
    }
    public static String[] listBackupNames(Context context)throws IOException {
        ArrayList<String> out=new ArrayList<>();int visited=0;
        try(DirectoryCursorR31 entries=DirectoryCursorR31.open(context.getDatabasePath(AppConfig.DB_NAME).getParentFile().getCanonicalFile())){
            File file;while((file=entries.next())!=null){check();if(++visited>4096)throw new IOException("WAIT_LEGACY_DIRECTORY_BOUND");
                try{requireName(file.getName());}catch(IllegalArgumentException other){continue;}
                if(!file.getCanonicalFile().equals(file.getAbsoluteFile())||!file.isFile())throw new IOException("R335_LEGACY_SOURCE_PATH");
                if(out.size()>=64)throw new IOException("WAIT_LEGACY_BACKUP_SELECTION_BOUND");out.add(file.getName());
            }
        }Collections.sort(out);return out.toArray(new String[0]);
    }
    static byte[] readBounded(File file,int max)throws IOException {
        if(file==null||!file.isFile()||!file.getAbsoluteFile().equals(file.getCanonicalFile())||file.length()>max)throw new IOException("R335_LEGACY_EVIDENCE_MISSING_OR_BOUND");
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){
            byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){check();if(n>max-out.size())throw new IOException("R335_LEGACY_EVIDENCE_GREW");out.write(buffer,0,n);}return out.toByteArray();
        }
    }
    private static void check()throws InterruptedIOException{if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("R335_LEGACY_CANCELLED");}
    private static File exact(File root,String relative)throws IOException{return StorageRestoreProofR31.child(root,relative);}
    static void verifyOriginal(File source,DatabaseBackupManifestR926 original)throws Exception {
        if(!source.isFile()||source.length()<512||source.length()>MAX_SOURCE||!source.getAbsoluteFile().equals(source.getCanonicalFile()))throw new IOException("R335_LEGACY_SOURCE_SIZE_OR_PATH");
        StorageMaintenanceFileR82.requireNoSidecars(source);
        if(!original.databaseSha256.equals(StorageRestoreAssetsR324.hash(source)))throw new IOException("R335_LEGACY_ORIGINAL_HASH_MISMATCH");
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(source.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){
            try(Cursor c=db.rawQuery("PRAGMA user_version",null)){if(!c.moveToFirst()||c.getInt(0)!=original.userVersion)throw new IOException("R335_LEGACY_USER_VERSION");}
            try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){if(!c.moveToFirst()||!"ok".equals(c.getString(0))||c.moveToNext())throw new IOException("R335_LEGACY_INTEGRITY");}
            DatabaseBackupManifestR926.Verification rows=original.verifyHistoricalRows(db);if(!rows.passed)throw new IOException("R335_LEGACY_HISTORY:"+rows.summary());
        }
        StorageMaintenanceFileR82.requireNoSidecars(source);
        if(!original.databaseSha256.equals(StorageRestoreAssetsR324.hash(source)))throw new IOException("R335_LEGACY_SOURCE_CHANGED_AFTER_READ");
    }
    private static long assetBytes(File dataRoot,String relative,int depth,long[] entries)throws IOException {
        check();if(relative.equals("files/r26-checkpoints-cold-r324/prepare"))return 0;
        if(depth>16||++entries[0]>131072)throw new IOException("WAIT_LEGACY_ASSET_INVENTORY_BOUND");
        File file=exact(dataRoot,relative);if(!file.exists())return 0;if(file.isFile())return file.length();
        if(!file.isDirectory())throw new IOException("R335_LEGACY_ASSET_TYPE");long sum=0;
        try(DirectoryCursorR31 children=DirectoryCursorR31.open(file)){File child;while((child=children.next())!=null)sum=Math.addExact(sum,assetBytes(dataRoot,relative+"/"+child.getName(),depth+1,entries));}return sum;
    }
    static StorageRestoreProofR31.Session prepare(File active,File directory,String backupName,String nonce,int parentPid,String sourceCommit)throws Exception {
        LegacyRecoveryPointLeaseR335 point=LegacyRecoveryPointLeaseR335.shared(active);boolean transferred=false;
        try{StorageRestoreProofR31.Session session=prepareOwnedR335(active,directory,backupName,nonce,parentPid,sourceCommit);session.legacyPointLease=point;transferred=true;return session;}
        finally{if(!transferred)point.close();}
    }
    private static StorageRestoreProofR31.Session prepareOwnedR335(File active,File directory,String backupName,String nonce,int parentPid,String sourceCommit)throws Exception {
        requireName(backupName);
        if(!DatabaseCoordinator.isClosedOfflineOwnerR31()||!nonce.matches("[0-9a-f]{64}")||parentPid<=0||!sourceCommit.matches("[0-9a-f]{40}"))throw new IOException("R335_LEGACY_OFFLINE_IDENTITY");
        active=active.getCanonicalFile();File dataRoot=active.getParentFile().getParentFile(),files=new File(dataRoot,"files");
        File source=exact(active.getParentFile(),backupName),manifest=exact(active.getParentFile(),backupName+".manifest.txt");
        DatabaseBackupManifestR926 original=DatabaseBackupManifestR926.readManifestR335(manifest);
        String manifestHash=StorageRestoreAssetsR324.hash(manifest);verifyOriginal(source,original);
        try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
            if(pin==null)throw new IOException("WAIT_LEGACY_ASSET_PIN_BUSY");
            long assets=0;long[] entries={0};for(String relative:ASSETS)assets=Math.addExact(assets,assetBytes(dataRoot,relative,0,entries));
            long databaseEnvelope=Math.addExact(source.length(),Math.max(64L<<20,source.length()/10));
            long growth=Math.addExact(Math.addExact(databaseEnvelope,assets),RESERVE);
            StorageBudgetR31.Reservation budget=StorageBudgetR31.forFiles(files).reserve("R335_LEGACY_BACKUP_RESTORE",0L,StorageBudgetR31.Category.BACKUP,"files/r31-restore-probes/"+nonce,growth,growth,true);
            boolean transferred=false;
            try{
                budget.beforeChunk(growth);
                File root=exact(directory,nonce+"/"+ReleaseManifestR926.APPLICATION_ID);
                if(root.exists()||!new File(root,"databases").mkdirs()||!new File(root,"files").mkdirs())throw new IOException("R335_LEGACY_EXCLUSIVE_WORKSPACE");
                File clone=new File(root,"databases/"+AppConfig.DB_NAME);
                String full=StorageMaintenanceFileR82.prepareLegacyRestoreCopyR335(source,clone,original.databaseSha256);
                if(clone.length()>databaseEnvelope)throw new IOException("WAIT_LEGACY_CLONE_GROWTH_BOUND");
                StorageRestoreAssetsR324.Totals totals=StorageRestoreAssetsR324.copy(dataRoot,root,ASSETS);
                if(totals.bytes!=assets)throw new IOException("R335_LEGACY_ASSET_SET_CHANGED");
                File copiedManifest=new File(root,"legacy-manifest.txt");writeExact(copiedManifest,readBounded(manifest,131072));
                String cloneHash=StorageRestoreAssetsR324.hash(clone);
                Properties request=new Properties();request.setProperty("nonce",nonce);request.setProperty("source",sourceCommit);request.setProperty("parent_pid",Integer.toString(parentPid));
                request.setProperty("active_sha256",original.databaseSha256);request.setProperty("clone_sha256",cloneHash);request.setProperty("native_full_compare",full);
                request.setProperty("asset_manifest_sha256",StorageRestoreAssetsR324.hash(new File(root,StorageRestoreAssetsR324.MEMBER)));
                request.setProperty("asset_manifest_format",StorageRestoreAssetsR324.FORMAT);request.setProperty("asset_files",Long.toString(totals.files));request.setProperty("asset_bytes",Long.toString(totals.bytes));
                request.setProperty("legacy_mode","R926_READ_ONLY_ORIGINAL_V1");request.setProperty("legacy_backup_name",backupName);request.setProperty("legacy_manifest_sha256",manifestHash);request.setProperty("legacy_snapshot_hash",original.snapshotHash);
                File req=new File(root,"request.properties");StorageMaintenanceFileR82.write(req,request);
                if(!manifestHash.equals(StorageRestoreAssetsR324.hash(manifest))||!original.databaseSha256.equals(StorageRestoreAssetsR324.hash(source)))throw new IOException("R335_LEGACY_SOURCE_OR_MANIFEST_CHANGED");
                budget.reconcile();
                StorageRestoreProofR31.Session session=new StorageRestoreProofR31.Session(source,root,clone,nonce,original.databaseSha256,cloneHash,parentPid,req,new File(root,"result.properties"),StorageRestoreAssetsR324.hash(req));
                session.legacySource=source;session.legacyManifest=manifest;session.legacyManifestHash=manifestHash;session.legacyBudget=budget;transferred=true;return session;
            }finally{if(!transferred)budget.close();}
        }
    }
    /** The sole input tree here was completely re-imported from the granted SAF
     * archive. This method never looks up an asset in the live application tree. */
    static StorageRestoreProofR31.Session prepareExternalR335(File active,File root,LegacyBackupCustodyR335.Case custody,String nonce,int parentPid,String sourceCommit)throws Exception {
        if(!DatabaseCoordinator.isClosedOfflineOwnerR31()||custody==null||!nonce.matches("[0-9a-f]{64}")||parentPid<=0||!sourceCommit.matches("[0-9a-f]{40}"))throw new IOException("R335_CUSTODY_OFFLINE_IDENTITY");
        File expected=StorageRestoreProofR31.child(new File(active.getParentFile().getParentFile(),"files/r31-restore-probes"),nonce+"/"+ReleaseManifestR926.APPLICATION_ID);
        if(!expected.equals(root.getAbsoluteFile())||!root.equals(root.getCanonicalFile()))throw new IOException("R335_CUSTODY_PROBE_ROOT");
        LegacyCustodyArchiveR335.verifyImported(root,custody.index);
        File source=LegacyCustodyArchiveR335.child(root,"databases/"+custody.index.backup);
        File manifest=LegacyCustodyArchiveR335.child(root,"databases/"+custody.index.backup+".manifest.txt");
        DatabaseBackupManifestR926 original=DatabaseBackupManifestR926.readManifestR335(manifest);verifyOriginal(source,original);
        if(!original.databaseSha256.equals(custody.index.sourceSha)||!custody.index.manifestSha.equals(StorageRestoreAssetsR324.hash(manifest)))throw new IOException("R335_CUSTODY_IMPORTED_ORIGINAL_IDENTITY");
        File clone=new File(root,"databases/"+AppConfig.DB_NAME);String full=StorageMaintenanceFileR82.prepareLegacyRestoreCopyR335(source,clone,original.databaseSha256);
        long cloneBound=Math.addExact(source.length(),Math.max(64L<<20,source.length()/10));if(clone.length()>cloneBound)throw new IOException("WAIT_R335_CUSTODY_CLONE_GROWTH_BOUND");
        LegacyCustodyArchiveR335.writeAssetManifest(root,custody.index);
        StorageRestoreAssetsR324.Totals assets=StorageRestoreAssetsR324.verify(root,new File(root,StorageRestoreAssetsR324.MEMBER));
        writeExact(new File(root,"legacy-manifest.txt"),readBounded(manifest,131072));
        String cloneHash=StorageRestoreAssetsR324.hash(clone);Properties request=new Properties();
        request.setProperty("nonce",nonce);request.setProperty("source",sourceCommit);request.setProperty("parent_pid",Integer.toString(parentPid));request.setProperty("active_sha256",original.databaseSha256);request.setProperty("clone_sha256",cloneHash);request.setProperty("native_full_compare",full);
        request.setProperty("asset_manifest_sha256",StorageRestoreAssetsR324.hash(new File(root,StorageRestoreAssetsR324.MEMBER)));request.setProperty("asset_manifest_format",StorageRestoreAssetsR324.FORMAT);request.setProperty("asset_files",Long.toString(assets.files));request.setProperty("asset_bytes",Long.toString(assets.bytes));
        request.setProperty("legacy_mode","R926_READ_ONLY_ORIGINAL_V1");request.setProperty("legacy_backup_name",custody.index.backup);request.setProperty("legacy_manifest_sha256",custody.index.manifestSha);request.setProperty("legacy_snapshot_hash",original.snapshotHash);
        request.setProperty("legacy_custody_case",custody.id());request.setProperty("legacy_custody_index_sha256",custody.index.sha());request.setProperty("legacy_custody_archive_sha256",custody.required("archive_sha256"));
        File req=new File(root,"request.properties");StorageMaintenanceFileR82.write(req,request);
        StorageRestoreProofR31.Session session=new StorageRestoreProofR31.Session(source,root,clone,nonce,original.databaseSha256,cloneHash,parentPid,req,new File(root,"result.properties"),StorageRestoreAssetsR324.hash(req));
        session.legacySource=source;session.legacyManifest=manifest;session.legacyManifestHash=custody.index.manifestSha;return session;
    }
    private static void writeExact(File file,byte[] body)throws IOException {
        if(file.exists()||!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("R335_LEGACY_EVIDENCE_ALREADY_EXISTS");
        try(FileOutputStream out=new FileOutputStream(file)){out.write(body);out.getFD().sync();}StorageMaintenanceFileR82.directorySyncR31.sync(file.getParentFile().getCanonicalPath());
    }
    /** A durable verification receipt preserves the complete request/result and
     * original manifest. It deliberately cannot authorize r82 or r926 cleanup. */
    static void recordCollected(StorageRestoreProofR31.Session session,Properties proof)throws IOException {
        if(!DatabaseCoordinator.isClosedOfflineOwnerR31()||session.observedProbePid<=0||session.legacySource==null)throw new IOException("R335_LEGACY_AUTHENTIC_COLLECTOR_REQUIRED");
        if(!session.activeHash.equals(StorageRestoreAssetsR324.hash(session.legacySource))||!session.legacyManifestHash.equals(StorageRestoreAssetsR324.hash(session.legacyManifest)))throw new IOException("R335_LEGACY_ORIGINAL_CHANGED_AT_RECEIPT");
        String refusal=StorageMaintenanceFileR82.restoreProofRefusalR31(proof,session.activeHash);if(refusal!=null)throw new IOException(refusal);
        if(!"PASS:ORIGINAL_MANIFEST_EXACT_PREMIGRATION_COLUMNS".equals(proof.getProperty("restore_legacy_historical_rows"))||!session.legacyManifestHash.equals(proof.getProperty("restore_legacy_manifest_sha256"))||!"NONE_ORIGINAL_RETAINED".equals(proof.getProperty("restore_legacy_cleanup_authority")))throw new IOException("R335_LEGACY_PROOF_SCOPE");
        Properties receipt=new Properties();receipt.putAll(proof);receipt.setProperty("legacy_source_name",session.legacySource.getName());receipt.setProperty("legacy_source_bytes",Long.toString(session.legacySource.length()));
        receipt.setProperty("legacy_receipt_kind","R335_READ_ONLY_R926_ORIGINAL_RESTORE_PROOF_V1");receipt.setProperty("cleanup_authority","NONE");receipt.setProperty("active_database_replaced","false");
        receipt.setProperty("request_base64",Base64.getEncoder().encodeToString(readBounded(session.request,262144)));
        receipt.setProperty("probe_result_base64",Base64.getEncoder().encodeToString(readBounded(session.result,262144)));
        receipt.setProperty("original_manifest_base64",Base64.getEncoder().encodeToString(readBounded(session.legacyManifest,131072)));
        File output=exact(session.legacySource.getParentFile(),session.legacySource.getName()+".r335-restore-"+session.nonce+".properties");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();receipt.store(bytes,"R335 original retained; no cleanup authority");if(bytes.size()>(1<<20))throw new IOException("R335_LEGACY_RECEIPT_BOUND");writeExact(output,bytes.toByteArray());
        Properties read=StorageMaintenanceFileR82.read(output);
        if(!session.activeHash.equals(read.getProperty("restore_active_sha256"))||!"NONE".equals(read.getProperty("cleanup_authority"))||StorageMaintenanceFileR82.restoreProofRefusalR31(read,session.activeHash)!=null)throw new IOException("R335_LEGACY_RECEIPT_READBACK");
    }
}
