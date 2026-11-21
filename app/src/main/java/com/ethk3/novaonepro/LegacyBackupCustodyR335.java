package com.ethk3.novaonepro;

import android.content.*;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.system.*;
import java.io.*;
import java.util.*;

/** App-owned r926 custody: exact SAF archive -> actual external readback -> the
 * original fresh Android restore process -> separately revocable single-file
 * retirement. External JSON/properties are never accepted as authorization. */
final class LegacyBackupCustodyR335 {
    private static final String ROOT="r335-legacy-custody";
    private static final LegacyRetirementAuthorizationR335 AUTHORITY=new LegacyRetirementAuthorizationR335();
    private static final class Resources implements AutoCloseable {
        final LegacyRecoveryPointLeaseR335 point;StorageBudgetR31.Reservation budget;boolean transferred;
        Resources(LegacyRecoveryPointLeaseR335 point){this.point=point;}
        public void close()throws IOException{if(transferred)return;IOException failure=null;try{if(budget!=null)budget.close();}catch(IOException error){failure=error;}finally{try{point.close();}catch(IOException error){if(failure==null)failure=error;else failure.addSuppressed(error);}}if(failure!=null)throw failure;}
    }
    static com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Lease memory()throws IOException {
        com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Admission admitted=com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.processBudget().tryAcquire("R335_LEGACY_CUSTODY",com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Kind.CHECKPOINT,64L<<20,()->Thread.currentThread().isInterrupted());
        if(!admitted.acquired())throw new IOException("WAIT_R335_CUSTODY_MEMORY:"+admitted.reason);return admitted.lease;
    }
    private static Map<String,File> runtimeFiles(Context context)throws IOException {
        android.content.pm.ApplicationInfo info=context.getApplicationInfo();TreeMap<String,File> files=new TreeMap<>();files.put("runtime/apk/base.apk",new File(info.sourceDir).getCanonicalFile());
        String[] splits=info.splitSourceDirs;if(splits!=null){if(splits.length>32)throw new IOException("R335_CUSTODY_APK_SPLIT_BOUND");for(int i=0;i<splits.length;i++)files.put(String.format(java.util.Locale.ROOT,"runtime/apk/split-%02d.apk",i),new File(splits[i]).getCanonicalFile());}return files;
    }
    static final class Case {
        final File directory;final Properties p;final LegacyCustodyArchiveR335.Index index;
        Case(File d,Properties p,LegacyCustodyArchiveR335.Index i){directory=d;this.p=p;index=i;}
        String id(){return p.getProperty("case_id");}File state(){return new File(directory,"state.properties");}
        String required(String key)throws IOException{String v=p.getProperty(key);if(v==null||v.isEmpty())throw new IOException("R335_CUSTODY_FIELD_MISSING:"+key);return v;}
        void save()throws IOException{StorageMaintenanceFileR82.write(state(),p);}
    }
    static final class Review {
        final String caseId,fingerprint,basename,sourceHash,manifestHash,archiveHash,probeNonce,document;final long bytes,created;
        private Review(Case c,String hash)throws IOException{caseId=c.id();fingerprint=hash;basename=c.index.backup;sourceHash=c.index.sourceSha;manifestHash=c.index.manifestSha;archiveHash=c.required("archive_sha256");probeNonce=c.required("probe_nonce");document=c.required("document_uri");bytes=c.index.sourceBytes;created=AUTHORITY.now();}
        String text(){return "备份："+basename+"\n字节数："+bytes+"\n原件 SHA-256："+sourceHash+"\n原 manifest SHA-256："+manifestHash+"\n外部归档 SHA-256："+archiveHash+"\n本次独立恢复 nonce："+probeNonce+"\n外部位置："+document+"\n\n仅退役上述一份本地 .bak；原 manifest、原恢复证明和外部完整原件仍保留。外部卷与授权须持续保管。本次授权最多60分钟；确认后仍会完整回读外部文件、重新排除占用并核对原件；过期或任何检查失败均保留本地，可再次审阅确认。可在最终提交开始前撤销；完成后需手动启动运行。";}
    }
    static String nonce()throws IOException{return StorageRestoreProofR31.digest(UUID.randomUUID().toString()+"|"+SystemClock.elapsedRealtimeNanos());}
    static File cases(Context context)throws IOException {File root=new File(context.getFilesDir().getCanonicalFile(),ROOT).getAbsoluteFile();if(!root.equals(root.getCanonicalFile()))throw new IOException("R335_CUSTODY_ROOT_SYMLINK");return root;}
    static String[] listCases(Context context)throws IOException {
        File root=cases(context);if(!root.exists())return new String[0];List<String> ids=new ArrayList<>();
        try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(root)){File next;int seen=0;while((next=cursor.next())!=null){if(++seen>128)throw new IOException("WAIT_R335_CUSTODY_CASE_BOUND");if(next.getName().matches("[0-9a-f]{64}")){try{Properties state=properties(new File(next,"state.properties"),131072);if(!next.getName().equals(state.getProperty("case_id"))||!"R335_NATIVE_LEGACY_CUSTODY_V1".equals(state.getProperty("format")))throw new IOException("R335_CUSTODY_CASE_IDENTITY");ids.add(next.getName());}catch(IOException malformed){/* Preserve this case; one bad case cannot hide the other valid cases. */}}}}
        Collections.sort(ids);return ids.toArray(new String[0]);
    }
    static Case load(Context context,String id)throws IOException {
        if(id==null||!id.matches("[0-9a-f]{64}"))throw new IOException("R335_CUSTODY_CASE_ID");File dir=LegacyCustodyArchiveR335.child(cases(context),id);
        Properties p=properties(new File(dir,"state.properties"),131072);if(!id.equals(p.getProperty("case_id"))||!"R335_NATIVE_LEGACY_CUSTODY_V1".equals(p.getProperty("format")))throw new IOException("R335_CUSTODY_CASE_IDENTITY");
        LegacyCustodyArchiveR335.Index index=LegacyCustodyArchiveR335.decode(LegacyBackupRecoveryR335.readBounded(new File(dir,"index.bin"),LegacyCustodyArchiveR335.MAX_INDEX));
        if(!index.sha().equals(p.getProperty("index_sha256"))||!index.backup.equals(p.getProperty("backup"))||!index.sourceSha.equals(p.getProperty("source_sha256")))throw new IOException("R335_CUSTODY_INDEX_IDENTITY");return new Case(dir,p,index);
    }
    private static Properties properties(File file,int maximum)throws IOException {Properties p=new Properties();p.load(new ByteArrayInputStream(LegacyBackupRecoveryR335.readBounded(file,maximum)));return p;}
    private static void exactCopy(File from,File to,int maximum)throws IOException {LegacyCustodyArchiveR335.writeExact(to,LegacyBackupRecoveryR335.readBounded(from,maximum));}
    private static void requireGrant(Context context,Uri tree)throws IOException {
        if(android.os.Build.VERSION.SDK_INT<28)throw new IOException("R335_CUSTODY_PLATFORM_API_28_REQUIRED");
        if(tree==null||!"content".equals(tree.getScheme())||!DocumentsContract.isTreeUri(tree))throw new IOException("R335_CUSTODY_SAF_TREE_REQUIRED");
        for(UriPermission permission:context.getContentResolver().getPersistedUriPermissions())if(tree.equals(permission.getUri())&&permission.isReadPermission()&&permission.isWritePermission())return;
        throw new IOException("WAIT_R335_CUSTODY_PERSISTED_READ_WRITE_GRANT");
    }
    private static void requireReadGrant(Context context,Uri document)throws IOException {
        if(document==null||!"content".equals(document.getScheme()))throw new IOException("R335_CUSTODY_DOCUMENT_REQUIRED");
        for(UriPermission permission:context.getContentResolver().getPersistedUriPermissions())if(document.equals(permission.getUri())&&permission.isReadPermission())return;
        throw new IOException("WAIT_R335_CUSTODY_PERSISTED_DOCUMENT_READ_GRANT");
    }
    private static void requireCaseGrant(Context context,Case c)throws IOException {String tree=c.p.getProperty("tree_uri","");if(tree.isEmpty())requireReadGrant(context,Uri.parse(c.required("document_uri")));else requireGrant(context,Uri.parse(tree));}
    static StorageRestoreProofR31.Session importExternal(Context context,Uri document)throws Exception {
        requireReadGrant(context,document);if(!DatabaseCoordinator.isExplicitMaintenanceActiveR82())throw new IOException("R335_CUSTODY_MAINTENANCE_REQUIRED");
        final LegacyCustodyArchiveR335.Index index;try(InputStream input=context.getContentResolver().openInputStream(document)){if(input==null)throw new IOException("R335_CUSTODY_INPUT_UNAVAILABLE");index=LegacyCustodyArchiveR335.readIndex(input);}
        final LegacyCustodyArchiveR335.Transfer transfer;try(InputStream input=context.getContentResolver().openInputStream(document)){if(input==null)throw new IOException("R335_CUSTODY_INPUT_UNAVAILABLE");transfer=LegacyCustodyArchiveR335.measure(input);}
        if(listCases(context).length>=64)throw new IOException("WAIT_R335_CUSTODY_CASE_CAPACITY");
        String id=nonce();File pending=LegacyCustodyArchiveR335.child(cases(context),".prepare-"+id),published=LegacyCustodyArchiveR335.child(cases(context),id);
        LegacyRecoveryPointLeaseR335 point=LegacyRecoveryPointLeaseR335.shared(context.getDatabasePath(AppConfig.DB_NAME));Resources resources=new Resources(point);
        try(Resources owned=resources){long growth=Math.addExact(Math.addExact(index.total,Math.multiplyExact(2L,index.sourceBytes+Math.max(64L<<20,index.sourceBytes/10))),(128L<<20)+2L*index.encoded.length);
            resources.budget=StorageBudgetR31.forFiles(context.getFilesDir()).reserve("R335_LEGACY_EXTERNAL_IMPORT",0,StorageBudgetR31.Category.BACKUP,"files/"+ROOT+"/"+id,growth,growth,true);resources.budget.beforeChunk(growth);
            if(pending.exists()||!pending.mkdirs())throw new IOException("R335_CUSTODY_EXCLUSIVE_CASE");LegacyCustodyArchiveR335.writeExact(new File(pending,"index.bin"),index.encoded);
            Properties p=new Properties();p.setProperty("format","R335_NATIVE_LEGACY_CUSTODY_V1");p.setProperty("case_id",id);p.setProperty("backup",index.backup);p.setProperty("source_sha256",index.sourceSha);p.setProperty("manifest_sha256",index.manifestSha);p.setProperty("index_sha256",index.sha());p.setProperty("source_identity","EXTERNAL_IMPORT_ONLY_NO_LOCAL_DELETE_AUTHORITY");p.setProperty("origin","EXTERNAL_IMPORT_ONLY");p.setProperty("source_commit",BuildConfig.SOURCE_COMMIT_R26);p.setProperty("document_uri",document.toString());p.setProperty("archive_sha256",transfer.sha);p.setProperty("archive_bytes",Long.toString(transfer.bytes));p.setProperty("authorization_epoch","0");p.setProperty("state","EXTERNAL_IMPORT_SELECTED_UNVERIFIED");p.setProperty("keep_restored_tree","true");p.setProperty("requested_peak_growth",Long.toString(growth));p.setProperty("available_before",Long.toString(filesystem().available(context.getFilesDir())));
            Case c=new Case(pending,p,index);c.save();if(published.exists()||!pending.renameTo(published))throw new IOException("R335_CUSTODY_CASE_PUBLICATION_FAILED");LegacyCustodyArchiveR335.sync(published.getParentFile());c=new Case(published,p,index);
            StorageRestoreProofR31.Session session=readbackAndPrepare(context,c,resources.budget,point);resources.transferred=true;return session;
        }
    }
    private static ReportSafCopyCP12.Destination destination(Context context,Uri uri){return new ReportSafCopyCP12.Destination(){public OutputStream openOutputTruncating()throws IOException{return context.getContentResolver().openOutputStream(uri,"wt");}public InputStream openInput()throws IOException{return context.getContentResolver().openInputStream(uri);}};}
    private static final ReportNativeCP4.Control CONTROL=new ReportNativeCP4.Control(){public boolean shouldContinue(){return !Thread.currentThread().isInterrupted();}public void progress(long done,long total){}};
    private static LegacyRetirementFileR335.Filesystem filesystem(){return new LegacyRetirementFileR335.Filesystem(){
        public String identity(File file)throws IOException{try{StructStat stat=Os.lstat(file.getCanonicalPath());if(!OsConstants.S_ISREG(stat.st_mode)||stat.st_nlink!=1)throw new IOException("R335_CUSTODY_SOURCE_REGULAR_SINGLE_LINK");return stat.st_dev+":"+stat.st_ino+":"+stat.st_size+":"+stat.st_mtime;}catch(ErrnoException error){throw new IOException("R335_CUSTODY_SOURCE_STAT",error);}}
        public long available(File directory)throws IOException{return StorageMaintenanceFileR82.availableRestoreBytesR31(directory);}
    };}
    /** Called only by the existing explicit-maintenance worker. Network/provider
     * I/O runs outside withClosedDatabaseR31; no SQLite actor waits on SAF. */
    static StorageRestoreProofR31.Session exportAndPrepare(Context context,String backup,Uri tree)throws Exception {
        requireGrant(context,tree);LegacyBackupRecoveryR335.requireName(backup);File active=context.getDatabasePath(AppConfig.DB_NAME);
        if(!DatabaseCoordinator.isExplicitMaintenanceActiveR82())throw new IOException("R335_CUSTODY_MAINTENANCE_REQUIRED");
        final Map<String,File> runtime=runtimeFiles(context);LegacyRecoveryPointLeaseR335 point=LegacyRecoveryPointLeaseR335.shared(active);Resources resources=new Resources(point);StorageBudgetR31.Reservation budget;
        try(Resources owned=resources;ReportInputsLeaseCP12 inputs=ReportInputsLeaseCP12.acquire(context.getFilesDir())){
            final String id=nonce();final File published=LegacyCustodyArchiveR335.child(cases(context),id),dir=LegacyCustodyArchiveR335.child(cases(context),".prepare-"+id);
            if(listCases(context).length>=64)throw new IOException("WAIT_R335_CUSTODY_CASE_CAPACITY");
            final LegacyCustodyArchiveR335.Index index=DatabaseCoordinator.withClosedDatabaseR31(context,db->{
                File original=LegacyCustodyArchiveR335.child(db.getParentFile(),backup);DatabaseBackupManifestR926 manifest=DatabaseBackupManifestR926.readManifestR335(new File(original.getPath()+".manifest.txt"));
                LegacyBackupRecoveryR335.verifyOriginal(original,manifest);return LegacyCustodyArchiveR335.capture(db.getParentFile().getParentFile(),backup,runtime);
            });
            long growth=Math.addExact(Math.addExact(index.total,Math.multiplyExact(2L,index.sourceBytes+Math.max(64L<<20,index.sourceBytes/10))),(128L<<20)+2L*index.encoded.length);
            budget=StorageBudgetR31.forFiles(context.getFilesDir()).reserve("R335_LEGACY_EXTERNAL_CUSTODY",0,StorageBudgetR31.Category.BACKUP,"files/"+ROOT+"/"+id,growth,growth,true);resources.budget=budget;budget.beforeChunk(growth);
            if(dir.exists()||!dir.mkdirs())throw new IOException("R335_CUSTODY_EXCLUSIVE_CASE");LegacyCustodyArchiveR335.sync(dir.getParentFile());
            LegacyCustodyArchiveR335.writeExact(new File(dir,"index.bin"),index.encoded);
            Properties p=new Properties();p.setProperty("format","R335_NATIVE_LEGACY_CUSTODY_V1");p.setProperty("case_id",id);p.setProperty("backup",backup);p.setProperty("source_sha256",index.sourceSha);p.setProperty("manifest_sha256",index.manifestSha);p.setProperty("index_sha256",index.sha());p.setProperty("source_identity",filesystem().identity(new File(active.getParentFile(),backup)));
            p.setProperty("source_commit",BuildConfig.SOURCE_COMMIT_R26);p.setProperty("tree_uri",tree.toString());p.setProperty("state","PACKED_IDENTITY_SOURCE_RETAINED");p.setProperty("authorization_epoch","0");p.setProperty("requested_peak_growth",Long.toString(growth));p.setProperty("available_before",Long.toString(filesystem().available(context.getFilesDir())));
            Case c=new Case(dir,p,index);c.save();
            if(published.exists()||!dir.renameTo(published))throw new IOException("R335_CUSTODY_CASE_PUBLICATION_FAILED");LegacyCustodyArchiveR335.sync(published.getParentFile());c=new Case(published,p,index);
            Uri parent=DocumentsContract.buildDocumentUriUsingTree(tree,DocumentsContract.getTreeDocumentId(tree));Uri document=DocumentsContract.createDocument(context.getContentResolver(),parent,"application/zip","STARCORE-r926-"+id+".zip");
            if(document==null||!"content".equals(document.getScheme())||!tree.getAuthority().equals(document.getAuthority()))throw new IOException("R335_CUSTODY_DOCUMENT_CREATE");
            p.setProperty("document_uri",document.toString());c.save();
            OutputStream output=context.getContentResolver().openOutputStream(document,"wt");if(output==null)throw new IOException("R335_CUSTODY_OUTPUT_UNAVAILABLE");
            LegacyCustodyArchiveR335.Transfer sent=LegacyCustodyArchiveR335.write(active.getParentFile().getParentFile(),index,output,runtime);
            p.setProperty("archive_sha256",sent.sha);p.setProperty("archive_bytes",Long.toString(sent.bytes));p.setProperty("state","EXTERNAL_WRITTEN_SOURCE_RETAINED");c.save();
            ReportSafCopyCP12.verifyReadback(sent.sha,sent.bytes,destination(context,document),CONTROL);requireGrant(context,tree);
            StorageRestoreProofR31.Session session=readbackAndPrepare(context,c,budget,point);resources.transferred=true;return session;
        }
    }
    /** Resume an existing exact archive with a fresh nonce and process. A prior
     * external write or old proof never skips a readback/restoration generation. */
    static StorageRestoreProofR31.Session verifyExisting(Context context,String id)throws Exception {
        Case c=load(context,id);requireCaseGrant(context,c);revoke(id);if(new File(c.directory,"retirement.properties").isFile()&&!"true".equals(c.p.getProperty("local_retired")))throw new IOException("WAIT_R335_CUSTODY_RECONCILE_SAME_GENERATION_RETIREMENT_FIRST");cleanupPending(context,c,false);c.p.setProperty("source_commit",BuildConfig.SOURCE_COMMIT_R26);c.p.setProperty("state","REVERIFY_REQUESTED_SOURCE_RETAINED");c.save();
        if("true".equals(c.p.getProperty("local_retired"))||"EXTERNAL_IMPORT_ONLY".equals(c.p.getProperty("origin")))c.p.setProperty("keep_restored_tree","true");
        LegacyRecoveryPointLeaseR335 point=LegacyRecoveryPointLeaseR335.shared(context.getDatabasePath(AppConfig.DB_NAME));Resources resources=new Resources(point);StorageBudgetR31.Reservation budget;
        try(Resources owned=resources){long growth=Math.addExact(Math.addExact(c.index.total,Math.multiplyExact(2L,c.index.sourceBytes+Math.max(64L<<20,c.index.sourceBytes/10))),128L<<20);
            budget=StorageBudgetR31.forFiles(context.getFilesDir()).reserve("R335_LEGACY_CUSTODY_REVERIFY",0,StorageBudgetR31.Category.BACKUP,"files/"+ROOT+"/"+id,growth,growth,true);resources.budget=budget;budget.beforeChunk(growth);
            c.p.setProperty("requested_peak_growth",Long.toString(growth));c.p.setProperty("available_before",Long.toString(filesystem().available(context.getFilesDir())));c.save();
            StorageRestoreProofR31.Session s=readbackAndPrepare(context,c,budget,point);resources.transferred=true;return s;
        }
    }
    private static void cleanupPending(Context context,Case c,boolean knownUnsubmitted)throws IOException {
        String nonce=c.p.getProperty("pending_probe_nonce","");if(nonce.isEmpty())return;if("VERIFIED_ISOLATED_RECOVERY_TREE_RETAINED".equals(c.p.getProperty("private_workspace")))throw new IOException("WAIT_R335_CUSTODY_VERIFIED_RESTORED_TREE_REQUIRES_SEPARATE_RELEASE");if(!nonce.matches("[0-9a-f]{64}"))throw new IOException("R335_CUSTODY_PENDING_NONCE_INVALID");
        File root=StorageRestoreProofR31.child(new File(context.getFilesDir(),"r31-restore-probes"),nonce+"/"+ReleaseManifestR926.APPLICATION_ID);
        if(root.exists()){
            if(!knownUnsubmitted){android.app.ActivityManager manager=(android.app.ActivityManager)context.getSystemService(Context.ACTIVITY_SERVICE);
                java.util.List<android.app.ActivityManager.RunningAppProcessInfo> processes=manager==null?null:manager.getRunningAppProcesses();
                if(processes==null)throw new IOException("WAIT_R335_CUSTODY_PRIOR_PROBE_LIFETIME_UNKNOWN");
                for(android.app.ActivityManager.RunningAppProcessInfo process:processes)if(context.getPackageName().concat(":restore_probe").equals(process.processName))throw new IOException("WAIT_R335_CUSTODY_PRIOR_PROBE_STILL_RUNNING");
            }
            if(!StorageRestoreAssetsR324.removePrivateTree(root,root,0))throw new IOException("WAIT_R335_CUSTODY_PRIVATE_IMPORT_RETAINED_DEBT");
            root.getParentFile().delete();
        }
        c.p.setProperty("pending_probe_nonce","");c.p.setProperty("private_workspace","RELEASED_ONLY_PRIVATE_COPY");c.save();
    }
    /** Separate user action for a verified private recovery copy. Full external
     * custody is re-read first; the historical local .bak is never touched. */
    static void releaseIsolated(Context context,String id)throws Exception {
        Case c=load(context,id);if(!"VERIFIED_ISOLATED_RECOVERY_TREE_RETAINED".equals(c.p.getProperty("private_workspace")))throw new IOException("R335_CUSTODY_NO_RETAINED_VERIFIED_TREE");evidence(c);requireCaseGrant(context,c);
        ReportSafCopyCP12.verifyReadback(c.required("archive_sha256"),Long.parseLong(c.required("archive_bytes")),destination(context,Uri.parse(c.required("document_uri"))),CONTROL);requireCaseGrant(context,c);
        c.p.setProperty("private_workspace","EXPLICIT_VERIFIED_PRIVATE_COPY_RELEASE_REQUESTED");c.save();cleanupPending(context,c,false);c.p.setProperty("restored_workspace","");c.p.setProperty("state","EXTERNAL_CUSTODY_VERIFIED_PRIVATE_COPY_RELEASED");c.save();
    }
    static void markProbeSubmitted(StorageRestoreProofR31.Session session)throws IOException {if(session.custody!=null){session.custody.p.setProperty("state","PROBE_SUBMITTED_POSSIBLY_RUNNING");session.custody.save();}}
    static void workspaceReleased(StorageRestoreProofR31.Session session)throws IOException {if(session.custody!=null){session.custody.p.setProperty("pending_probe_nonce","");session.custody.p.setProperty("private_workspace","RELEASED_ONLY_PRIVATE_COPY");session.custody.save();}}
    private static StorageRestoreProofR31.Session readbackAndPrepare(Context context,Case c,StorageBudgetR31.Reservation budget,LegacyRecoveryPointLeaseR335 point)throws Exception {
        String nonce=nonce();File root=StorageRestoreProofR31.child(new File(context.getFilesDir(),"r31-restore-probes"),nonce+"/"+ReleaseManifestR926.APPLICATION_ID);
        c.p.setProperty("state","EXTERNAL_READBACK_IN_PROGRESS");c.p.setProperty("probe_nonce",nonce);c.p.setProperty("pending_probe_nonce",nonce);c.p.setProperty("private_workspace","RESERVED_IMPORT_NOT_SUBMITTED");c.save();
        try{
            LegacyCustodyArchiveR335.Transfer expected=new LegacyCustodyArchiveR335.Transfer(Long.parseLong(c.required("archive_bytes")),c.required("archive_sha256"));
            try(InputStream input=context.getContentResolver().openInputStream(Uri.parse(c.required("document_uri")))){if(input==null)throw new IOException("R335_CUSTODY_READBACK_UNAVAILABLE");LegacyCustodyArchiveR335.extract(input,c.index,expected,root);}
            requireCaseGrant(context,c);budget.reconcile();
            c.p.setProperty("state","EXTERNAL_READBACK_VERIFIED_SOURCE_RETAINED");c.save();
            StorageRestoreProofR31.Session s=DatabaseCoordinator.withClosedDatabaseR31(context,db->LegacyBackupRecoveryR335.prepareExternalR335(db,root,c,nonce,android.os.Process.myPid(),BuildConfig.SOURCE_COMMIT_R26));
            s.legacyBudget=budget;s.legacyPointLease=point;s.custody=c;return s;
        }catch(Exception failure){
            c.p.setProperty("state","READBACK_OR_PREPARE_FAILED_SOURCE_RETAINED");c.p.setProperty("last_refusal",DatabaseCoordinator.describeCauseChain(failure,1600));
            try{cleanupPending(context,c,true);}catch(IOException debt){failure.addSuppressed(debt);c.p.setProperty("private_workspace","RETAINED_DEBT_NO_NEW_NONCE_UNTIL_CLEANED");try{c.save();}catch(IOException journal){failure.addSuppressed(journal);}}
            throw failure;
        }
    }
    /** Only called by StorageRestoreProof's fresh-session authenticated collector,
     * after synchronous PID observation and actual Binder death. */
    static void recordCollected(StorageRestoreProofR31.Session s,Properties proof)throws IOException {
        Case c=s.custody;if(c==null||!DatabaseCoordinator.isClosedOfflineOwnerR31()||s.observedProbePid<=0||s.recorded)throw new IOException("R335_CUSTODY_AUTHENTIC_COLLECTOR_REQUIRED");
        String refusal=StorageMaintenanceFileR82.restoreProofRefusalR31(proof,c.index.sourceSha);if(refusal!=null)throw new IOException(refusal);
        Properties request=properties(s.request,262144);
        for(String[] pair:new String[][]{{"legacy_custody_case",c.id()},{"legacy_custody_index_sha256",c.index.sha()},{"legacy_custody_archive_sha256",c.required("archive_sha256")},{"legacy_manifest_sha256",c.index.manifestSha},{"nonce",c.required("probe_nonce")}})if(!pair[1].equals(request.getProperty(pair[0])))throw new IOException("R335_CUSTODY_COLLECTOR_CASE_CONFLICT");
        if(!"PASS:ORIGINAL_MANIFEST_EXACT_PREMIGRATION_COLUMNS".equals(proof.getProperty("restore_legacy_historical_rows"))||!"NONE_ORIGINAL_RETAINED".equals(proof.getProperty("restore_legacy_cleanup_authority")))throw new IOException("R335_CUSTODY_RESTORE_NOT_DELETE_AUTHORITY");
        File evidence=LegacyCustodyArchiveR335.child(c.directory,"proof-"+s.nonce);if(evidence.exists()||!evidence.mkdir())throw new IOException("R335_CUSTODY_PROOF_GENERATION_EXISTS");
        exactCopy(s.request,new File(evidence,"request.properties"),262144);exactCopy(s.result,new File(evidence,"result.properties"),262144);
        exactCopy(s.legacyManifest,new File(evidence,"original-manifest.txt"),131072);exactCopy(new File(s.root,StorageRestoreAssetsR324.MEMBER),new File(evidence,StorageRestoreAssetsR324.MEMBER),LegacyCustodyArchiveR335.MAX_INDEX);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();proof.store(bytes,"Authenticated restore only; separate user authorization required");LegacyCustodyArchiveR335.writeExact(new File(evidence,"collected.properties"),bytes.toByteArray());
        c.p.setProperty("proof_directory",evidence.getName());c.p.setProperty("proof_sha256",StorageRestoreAssetsR324.hash(new File(evidence,"collected.properties")));c.p.setProperty("state","true".equals(c.p.getProperty("keep_restored_tree"))?"RESTORED_TO_ISOLATED_TREE":"VERIFIED_RETAINED");if("true".equals(c.p.getProperty("keep_restored_tree"))){c.p.setProperty("private_workspace","VERIFIED_ISOLATED_RECOVERY_TREE_RETAINED");c.p.setProperty("restored_workspace",s.root.getCanonicalPath());}c.p.setProperty("authorization_epoch",Long.toString(Math.addExact(Long.parseLong(c.p.getProperty("authorization_epoch","0")),1)));c.save();
        evidence(c); // Independent on-disk readback before the private workspace is disposable.
    }
    private static String evidence(Case c)throws IOException{return evidence(c,true);}
    private static String evidence(Case c,boolean requireCurrent)throws IOException {
        if(requireCurrent&&!BuildConfig.SOURCE_COMMIT_R26.equals(c.required("source_commit")))throw new IOException("R335_CUSTODY_SOURCE_VERSION_CHANGED_REVERIFY_REQUIRED");
        String directory=c.required("proof_directory");if(!directory.equals("proof-"+c.required("probe_nonce")))throw new IOException("R335_CUSTODY_PROOF_GENERATION");File root=LegacyCustodyArchiveR335.child(c.directory,directory);
        File collected=new File(root,"collected.properties");if(!c.required("proof_sha256").equals(StorageRestoreAssetsR324.hash(collected)))throw new IOException("R335_CUSTODY_PROOF_CHANGED");
        Properties proof=properties(collected,1<<20),request=properties(new File(root,"request.properties"),262144),result=properties(new File(root,"result.properties"),262144);
        String refusal=StorageMaintenanceFileR82.restoreProofRefusalR31(proof,c.index.sourceSha);if(refusal!=null)throw new IOException(refusal);
        if(!c.required("source_commit").equals(proof.getProperty("restore_source_commit"))||!c.required("source_commit").equals(request.getProperty("source"))||!c.required("probe_nonce").equals(proof.getProperty("restore_nonce"))||!c.index.manifestSha.equals(StorageRestoreAssetsR324.hash(new File(root,"original-manifest.txt")))||!c.required("archive_sha256").equals(request.getProperty("legacy_custody_archive_sha256"))||!c.index.sha().equals(request.getProperty("legacy_custody_index_sha256"))||!c.id().equals(request.getProperty("legacy_custody_case"))||!StorageRestoreAssetsR324.hash(new File(root,"request.properties")).equals(proof.getProperty("restore_request_sha256"))||!StorageRestoreAssetsR324.hash(new File(root,"result.properties")).equals(proof.getProperty("restore_probe_receipt_sha256"))||!StorageRestoreAssetsR324.hash(new File(root,StorageRestoreAssetsR324.MEMBER)).equals(request.getProperty("asset_manifest_sha256")))throw new IOException("R335_CUSTODY_PROOF_CLOSURE_CHANGED");
        for(String key:result.stringPropertyNames())if(key.startsWith("restore_")&&!result.getProperty(key).equals(proof.getProperty(key)))throw new IOException("R335_CUSTODY_RESULT_ENVELOPE_CHANGED");
        return StorageRestoreProofR31.digest(c.id()+"|"+c.index.sha()+"|"+c.required("source_identity")+"|"+c.required("document_uri")+"|"+c.required("archive_sha256")+"|"+c.required("archive_bytes")+"|"+c.required("proof_sha256")+"|"+c.required("probe_nonce")+"|"+c.p.getProperty("authorization_epoch","0"));
    }
    static LegacyRetirementFileR335.Result reconcile(Context context,String id)throws Exception {
        Case c=load(context,id);String proof=evidence(c,false);requireCaseGrant(context,c);ReportSafCopyCP12.verifyReadback(c.required("archive_sha256"),Long.parseLong(c.required("archive_bytes")),destination(context,Uri.parse(c.required("document_uri"))),CONTROL);
        LegacyRetirementFileR335.Result result=DatabaseCoordinator.withClosedDatabaseR31(context,db->{try(LegacyRecoveryPointLeaseR335 lease=LegacyRecoveryPointLeaseR335.exclusive(db)){return LegacyRetirementFileR335.reconcile(db,c.index.backup,c.index.sourceSha,c.index.sourceBytes,c.required("source_identity"),c.index.manifestSha,proof,new File(c.directory,"retirement.properties"),filesystem());}});
        c.p.setProperty("state","RETIRED_WITH_EXTERNAL_CUSTODY");c.p.setProperty("local_retired","true");c.p.setProperty("removed_file_length",Long.toString(result.removedLength));c.p.setProperty("observed_available_before",Long.toString(result.availableBefore));c.p.setProperty("observed_available_after",Long.toString(result.availableAfter));c.p.setProperty("observed_free_delta",Long.toString(result.observedFreeDelta()));c.p.setProperty("last_authorization","READ_ONLY_COMMIT_RECONCILIATION_NO_NEW_UNLINK");c.save();return result;
    }
    static Review review(Context context,String id)throws IOException {try(com.ethk3.novaonepro.strategyv2.MemoryBudgetR31.Lease memory=memory()){Case c=load(context,id);if("EXTERNAL_IMPORT_ONLY".equals(c.p.getProperty("origin"))||"true".equals(c.p.getProperty("local_retired")))throw new IOException("R335_CUSTODY_IMPORT_OR_RETIRED_HAS_NO_NEW_DELETE_AUTHORITY");String state=c.required("state");if(!Arrays.asList("VERIFIED_RETAINED","RETIREMENT_REQUESTED","DELETE_RESULT_UNKNOWN").contains(state))throw new IOException("R335_CUSTODY_NOT_VERIFIED_RETAINED:"+state);return new Review(c,evidence(c));}}
    static String confirm(Review review)throws IOException {return AUTHORITY.confirm(review.caseId,review.fingerprint,review.created);}
    static void revoke(String id){AUTHORITY.revoke(id);}
    private static void ticket(String token,String id,String fingerprint)throws IOException {AUTHORITY.check(token,id,fingerprint);}
    static LegacyRetirementFileR335.Result retire(Context context,String id,String token)throws Exception {
        Case c=load(context,id);if("EXTERNAL_IMPORT_ONLY".equals(c.p.getProperty("origin"))||"true".equals(c.p.getProperty("local_retired")))throw new IOException("R335_CUSTODY_IMPORT_OR_RETIRED_HAS_NO_NEW_DELETE_AUTHORITY");String fingerprint=evidence(c);ticket(token,id,fingerprint);
        requireCaseGrant(context,c);ReportSafCopyCP12.verifyReadback(c.required("archive_sha256"),Long.parseLong(c.required("archive_bytes")),destination(context,Uri.parse(c.required("document_uri"))),CONTROL);
        requireCaseGrant(context,c);ticket(token,id,evidence(load(context,id)));
        c.p.setProperty("state","RETIREMENT_REQUESTED");c.p.setProperty("last_authorization","EXPLICIT_UI_CONFIRMATION:"+StorageRestoreProofR31.digest(token));c.save();File active=context.getDatabasePath(AppConfig.DB_NAME);
        try{
            LegacyRetirementFileR335.Result result=DatabaseCoordinator.withClosedDatabaseR31(context,db->{
                try(LegacyRecoveryPointLeaseR335 point=LegacyRecoveryPointLeaseR335.exclusive(db);ReportInputsLeaseCP12 inputs=ReportInputsLeaseCP12.tryAcquireExclusive(context.getFilesDir())){
                    if(inputs==null)throw new IOException("WAIT_R335_CUSTODY_REPORT_INPUTS_IN_USE");
                    for(String suffix:new String[]{".r82-maintenance-journal",".r82-rebuilt",".r926.restore",".r926.restore.copying"})if(new File(db.getPath()+suffix).exists())throw new IOException("WAIT_R335_CUSTODY_PENDING_RECOVERY_FILE:"+suffix);
                    try(SQLiteDatabase read=SQLiteDatabase.openDatabase(db.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)){if(!SchemaDomainCoordinatorR926.matches(read,SchemaDomainCoordinatorR926.criticalContract()))throw new IOException("WAIT_R335_CUSTODY_CURRENT_CRITICAL_NOT_READY");}
                    final String currentEvidence=evidence(load(context,id));ticket(token,id,currentEvidence);
                    return LegacyRetirementFileR335.retire(db,c.index.backup,c.index.sourceSha,c.index.sourceBytes,c.required("source_identity"),c.index.manifestSha,StorageRestoreProofR31.digest(token),fingerprint,new File(c.directory,"retirement.properties"),point,filesystem(),new LegacyRetirementFileR335.Gate(){
                        public void check()throws Exception{ticket(token,id,currentEvidence);}
                        public void beginCommit()throws Exception{AUTHORITY.beginCommit(token,id,currentEvidence);}
                    });
                }
            });
            c.p.setProperty("state","RETIRED_WITH_EXTERNAL_CUSTODY");c.p.setProperty("local_retired","true");c.p.setProperty("removed_file_length",Long.toString(result.removedLength));c.p.setProperty("observed_available_before",Long.toString(result.availableBefore));c.p.setProperty("observed_available_after",Long.toString(result.availableAfter));c.p.setProperty("observed_free_delta",Long.toString(result.observedFreeDelta()));c.save();AUTHORITY.finish(token);return result;
        }catch(Exception unknown){AUTHORITY.finish(token);c.p.remove("local_retired");c.p.setProperty("last_refusal",DatabaseCoordinator.describeCauseChain(unknown,1600));c.p.setProperty("state",new File(c.directory,"retirement.properties").isFile()?"DELETE_RESULT_UNKNOWN":"VERIFIED_RETAINED");try{c.save();}catch(IOException suppressed){unknown.addSuppressed(suppressed);}throw unknown;}
    }
    static String describe(Context context,String id)throws IOException {File dir=LegacyCustodyArchiveR335.child(cases(context),id);Properties p=properties(new File(dir,"state.properties"),131072);return p.getProperty("backup","UNKNOWN")+" | "+p.getProperty("state","UNKNOWN")+" | "+id.substring(0,12);}
    static void recordRevocation(Context context,String id)throws IOException {System.loadLibrary("starcore_report_cp4");File dir=LegacyCustodyArchiveR335.child(cases(context),id);Properties p=new Properties();p.setProperty("case_id",id);p.setProperty("state","USER_REVOKE_REQUEST_NO_REVERSE_AFTER_COMMIT_START");p.setProperty("recorded_wall_ms",Long.toString(System.currentTimeMillis()));StorageMaintenanceFileR82.write(new File(dir,"revocation.properties"),p);}
    static String summary(Context context){try{StringBuilder out=new StringBuilder("R335_LEGACY_CUSTODY|scope=RECORDED_METADATA_NOT_AUTHORIZATION");File root=cases(context);if(!root.isDirectory())return out.toString();try(DirectoryCursorR31 cursor=DirectoryCursorR31.open(root)){File f;int count=0;while((f=cursor.next())!=null){if(++count>128){out.append("\nCASE_ENUMERATION_BOUND");break;}if(f.getName().startsWith(".prepare-")){out.append("\nUNPUBLISHED_CASE_RETAINED:").append(f.getName());continue;}if(!f.getName().matches("[0-9a-f]{64}"))continue;try{Properties p=properties(new File(f,"state.properties"),131072);out.append("\ncase=").append(f.getName());File revocation=new File(f,"revocation.properties");if(revocation.isFile())out.append("|revocation_record=").append(properties(revocation,16384).getProperty("state","UNKNOWN"));for(String key:new String[]{"backup","source_sha256","manifest_sha256","index_sha256","archive_sha256","probe_nonce","proof_sha256","state","authorization_epoch","last_authorization","last_refusal","private_workspace","restored_workspace","requested_peak_growth","available_before","removed_file_length","observed_available_before","observed_available_after","observed_free_delta"})out.append('|').append(key).append('=').append(p.getProperty(key,"UNKNOWN"));}catch(IOException invalid){out.append("\nCASE_UNKNOWN:").append(f.getName()).append(':').append(invalid.getMessage());}}}return out.toString();}catch(Exception unavailable){return "R335_LEGACY_CUSTODY_UNKNOWN:"+unavailable.getClass().getSimpleName();}}
}
