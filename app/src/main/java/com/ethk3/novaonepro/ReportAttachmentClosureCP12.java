package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.StatFs;
import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import org.json.JSONObject;

/** Explicit external evidence registry, bound to the copied SQLite file; no live DbStore/model runtime is opened. */
public final class ReportAttachmentClosureCP12 {
    private static final long MiB=1024L*1024L;
    /** Shared with the read-only report context: producer and consumer name one format. */
    public static String reportProfileR80(boolean compact){
        return compact?ReportReadContextCP4.COMPLETE_DIAGNOSTIC_PROFILE_R68:ReportReadContextCP4.RAW_EVIDENCE_PROFILE;
    }

    private ReportAttachmentClosureCP12() {}
    public static void capture(Context context,SQLiteDatabase copy,File job,JSONObject request,
                               ReportNativeCP4.Control control,Writer members)throws Exception {
        capture(context,copy,job,request,control,members,ReportArchiveCP4.hash(new File(copy.getPath()),control),new File(copy.getPath()).length());
    }
    /** Caller supplies the hash of its closed immutable backup, already verified on resume. */
    public static void capture(Context context,SQLiteDatabase copy,File job,JSONObject request,
                               ReportNativeCP4.Control control,Writer members,String snapshotHash,long snapshotBytes)throws Exception {
        requireHash(snapshotHash);
        if(new File(copy.getPath()).length()!=snapshotBytes)throw new IOException("REPORT_SNAPSHOT_BYTES_CHANGED");
        if(!copy.isReadOnly())throw new IOException("REPORT_ATTACHMENT_READONLY_REQUIRED");Context app=context.getApplicationContext();
        File attachments=new File(job,"attachments");if(!attachments.isDirectory()&&!attachments.mkdir())throw new IOException("REPORT_ATTACHMENT_DIRECTORY");
        File plans=new File(job,"attachments-plan-"+UUID.randomUUID().toString().replace("-","")+".jsonl");
        File observation=new File(job,"runtime-observation.json");ReportJobJournalCP12.writeAtomic(observation,request.getJSONObject("observation").toString());
        ReportMemberPlanCP12.append(members,"runtime-observation.json",observation.getName(),ReportArchiveCP4.hash(observation,control),observation.length());
        String apkBefore=ReportArchiveCP4.hash(new File(app.getApplicationInfo().sourceDir),control);JSONObject source=new JSONObject();source.put("releaseSha256",request.getString("releaseHash"));source.put("apkSha256",apkBefore);source.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);source.put("sourceTreeSha256",SourceManifestR815.SOURCE_TREE_SHA256);source.put("generatedRuleSha256",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);source.put("sourceIndexAsset",RuntimeChainDiagnosisR975.INDEX);
        BuildProvenanceR815.Snapshot runtimeIdentity=BuildProvenanceR815.lastSnapshot();if(runtimeIdentity!=null&&runtimeIdentity.valid&&apkBefore.equals(runtimeIdentity.apkSha256))source.put("signingCertificateSha256",runtimeIdentity.certSha256);else source.put("signingCertificateState","MATCHED_RUNTIME_FINGERPRINT_UNAVAILABLE");
        JSONObject registries=new JSONObject();
        for(String table:new String[]{ResearchPlatformStoreR929.TABLE_BUNDLE,"research_evidence_archive_v2","research_training_input_v2","research_training_attachment_cp12"})
            registries.put(table,hasTable(copy,table)?"PRESENT":"MISSING_SCHEMA");
        source.put("attachmentRegistries",registries);
        source.put("registryEvidenceBoundary","MISSING_SCHEMA is unavailable evidence, never an empty initialized registry or a passed business check");
        final boolean compact = !request.optBoolean("includeRawEvidence", false);
        source.put("reportProfile", reportProfileR80(compact));
        source.put("rawEvidenceIncluded", !compact);
        source.put("snapshotSha256", snapshotHash);
        source.put("snapshotBytes", new File(copy.getPath()).length());
        source.put("omittedPayloads", compact ? "installed APK; raw SQLite; model/training/retention binaries; historical duplicate ledgers" : "NONE");
        source.put("coverage", "All 96 checks evaluated on one immutable full snapshot; all schema objects and table counts; exact source call index; current construction ledgers; runtime observations. No omitted payload is claimed to be exported.");
        long total,seedRows=0,seedMismatches=0;
        File seedEvidence=new File(job,"seed-bundle-observations.jsonl");
        source.put("installedSeedBundleSha256",V51ArtifactBundleManifest.BUNDLE_HASH);
        try(FileOutputStream fd=new FileOutputStream(plans);Writer out=new BufferedWriter(new OutputStreamWriter(fd,StandardCharsets.UTF_8),8192)) {
            org.json.JSONArray observations=request.getJSONObject("observation").optJSONArray("fullObservations");
            if(observations!=null){if(observations.length()>8)throw new IOException("REPORT_OBSERVATION_REFERENCE_BOUND");
                for(int i=0;i<observations.length();i++){JSONObject ref=observations.getJSONObject(i);String key=ref.getString("key"),requestId=request.getString("requestId"),hash=ref.getString("sha256");
                    if(!key.matches("[A-Za-z][A-Za-z0-9]{0,63}")||!requestId.matches("[0-9a-f]{64}")||!ref.getString("path").equals("observations/"+requestId+"/"+key+".txt"))throw new IOException("REPORT_OBSERVATION_REFERENCE_PATH");requireHash(hash);
                    File original=ReportArchiveVerifierCP12.inside(new File(app.getFilesDir(),"r981_reports"),ref.getString("path"));
                    if(!original.isFile()||original.length()!=ref.getLong("bytes")||!hash.equals(ReportArchiveCP4.hash(original,control)))throw new IOException("REPORT_OBSERVATION_CHANGED");
                    descriptor(out,"runtime-observation-"+key+".txt",hash,original.length(),"OBSERVATION",original.getPath(),"");
                }
            }
            File installed=new File(app.getApplicationInfo().sourceDir).getCanonicalFile();if(!installed.isFile())throw new IOException("REPORT_INSTALLED_APK_MISSING");source.put("installedApkBytes",installed.length());source.put("installedApkIncluded",!compact);if(!compact){descriptor(out,"installed-app.apk",apkBefore,installed.length(),"INSTALLED_APK",installed.getPath(),"");source.put("installedApkMember","installed-app.apk");}
            File r26Root=new File(app.getFilesDir(),"r26-checkpoints").getCanonicalFile();
            long r26Bytes=0;java.util.Set<String> r26Checkpoints=new com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26(r26Root).reachableHashes(com.ethk3.novaonepro.strategyv2.CheckpointReferencesR26.hashes(copy));
            for(String hash:r26Checkpoints){checkpoint(control);File input=ReportArchiveVerifierCP12.inside(r26Root,hash+".bin");long size=input.length();if(!input.isFile()||size<1||size>com.ethk3.novaonepro.strategyv2.CheckpointArtifactsR26.MAX_BYTES)throw new IOException("REPORT_R26_CHECKPOINT_MISSING_OR_INVALID:"+hash);r26Bytes=Math.addExact(r26Bytes,size);
                if(!compact)descriptor(out,"r26-checkpoint-"+hash+".bin",hash,size,"R26_CHECKPOINT",input.getPath(),"");}
            source.put("r26CheckpointObjects",r26Checkpoints.size()).put("r26CheckpointBytes",r26Bytes).put("r26CheckpointBodiesIncluded",!compact).put("r26CheckpointByteValidation",compact?"NOT_RUN_COMPACT_BYTE_BUDGET":"EACH_INCLUDED_BODY_SHA256_VERIFIED").put("r26CheckpointScope","Referenced complete states, durable replay outputs and retained parents of this exact SQLite snapshot; older historical states are not claimed");
            String last="";
            try(FileOutputStream seedFd=new FileOutputStream(seedEvidence);Writer seeds=new BufferedWriter(new OutputStreamWriter(seedFd,StandardCharsets.UTF_8),8192)) {
            while(hasTable(copy,ResearchPlatformStoreR929.TABLE_BUNDLE)) {
                checkpoint(control);String hash,key;long bytes;
                try(Cursor c=copy.rawQuery("SELECT bundle_hash,key_id,total_bytes FROM "+ResearchPlatformStoreR929.TABLE_BUNDLE+" WHERE bundle_hash>? ORDER BY bundle_hash LIMIT 1",new String[]{last})) {
                    if(!c.moveToFirst())break;hash=c.getString(0);key=c.getString(1);bytes=c.getLong(2);
                }
                last=hash;requireHash(hash);
                if("INSTALLED_APK_SEED".equals(key)) {
                    // Old APK seeds and credential-dependent hashes remain database evidence.
                    // A diagnostic must describe that mismatch instead of rejecting its snapshot.
                    boolean matches=bytes==0&&hash.equals(V51ArtifactBundleManifest.BUNDLE_HASH);
                    JSONObject row=new JSONObject();row.put("databaseBundleSha256",hash);row.put("totalBytes",bytes);
                    row.put("installedBundleSha256",V51ArtifactBundleManifest.BUNDLE_HASH);
                    row.put("outcome",matches?"CURRENT_SEED":"SEED_IDENTITY_MISMATCH");row.put("productionAuthorized",false);
                    seeds.write(row.toString());seeds.write('\n');seedRows++;if(!matches)seedMismatches++;
                } else if("PINNED_MODEL_OWNER".equals(key)&&bytes>0&&bytes<=ModelBundleManagerR929.MAX_BUNDLE_BYTES){if(!compact)bundle(app,hash,out,control);}
                else throw new IOException("REPORT_BUNDLE_ORIGIN_UNKNOWN");
            }
            seeds.flush();seedFd.getFD().sync();}
            String seedSha=ReportArchiveCP4.hash(seedEvidence,control);
            descriptor(out,"seed-bundle-observations.jsonl",seedSha,seedEvidence.length(),"FILE",seedEvidence.getCanonicalPath(),new File(app.getFilesDir(),"r994-seed-observations.lock").getCanonicalPath());
            source.put("seedRegistryRows",seedRows);source.put("seedIdentityMismatches",seedMismatches);
            source.put("seedRegistryMember","seed-bundle-observations.jsonl");
            if(!compact) for(String stateKey:new String[]{ModelBundleManagerR929.STATE_ACTIVE,ModelBundleManagerR929.STATE_PREVIOUS,ModelBundleManagerR929.STATE_STAGED,"r932_bundle_capture_hash"}) {
                String hash=state(copy,stateKey);if(hash.isEmpty())continue;requireHash(hash);
                if(hash.equals(V51ArtifactBundleManifest.BUNDLE_HASH))continue;
                if(hasTable(copy,ResearchPlatformStoreR929.TABLE_BUNDLE))
                    try(Cursor c=copy.rawQuery("SELECT key_id,total_bytes FROM "+ResearchPlatformStoreR929.TABLE_BUNDLE+" WHERE bundle_hash=?",new String[]{hash})) {
                        if(c.moveToFirst()&&"INSTALLED_APK_SEED".equals(c.getString(0)))continue;
                    }
                bundle(app,hash,out,control);
            }
            File archiveObservations=new File(job,"r80-archive-observations.jsonl");
            source.put("r80ArchivedOriginals",observeR80Archives(copy,app.getFilesDir(),archiveObservations,control,compact));
            source.put("r80ArchivedBodiesIncluded",!compact);
            source.put("r80ArchiveRecovery","FULL_RAW_EVIDENCE exports exact verified bodies; compact exports availability/hash/locator metadata and hot originals only");
            descriptor(out,archiveObservations.getName(),ReportArchiveCP4.hash(archiveObservations,control),archiveObservations.length(),"FILE",archiveObservations.getCanonicalPath(),new File(app.getFilesDir(),"r80-archive-observations.lock").getCanonicalPath());
            last="";
            while(!compact && hasTable(copy,"research_evidence_archive_v2")) {
                checkpoint(control);String id,hash,path;
                try(Cursor c=copy.rawQuery("SELECT archive_id,archive_hash,relative_path FROM research_evidence_archive_v2 WHERE archive_id>? AND state IN ('VERIFIED','REFERENCED','PRUNED') ORDER BY archive_id LIMIT 1",new String[]{last})) {
                    if(!c.moveToFirst())break;id=c.getString(0);hash=c.getString(1);path=c.getString(2);
                }
                last=id;requireHash(hash);String expected="retention-cas-cp12/"+hash+".scrow";if(!expected.equals(path))throw new IOException("REPORT_ARCHIVE_REFERENCE_PATH");
                File original=ReportArchiveVerifierCP12.inside(app.getFilesDir().getCanonicalFile(),path);
                try(SharedLease lease=shared(new File(original.getParentFile(),hash+".lock"))) {
                    if(!original.isFile()||!hash.equals(ReportArchiveCP4.hash(original,control)))throw new IOException("REPORT_ARCHIVE_MISSING_OR_CHANGED");
                    descriptor(out,"artifact-"+hash+".bin",hash,original.length(),"FILE",original.getPath(),lease.path.getPath());
                }
            }
            // Training input and bound protocol artifacts are external CAS. Metadata comes only from this immutable SQLite snapshot.
            if(!compact) trainingFiles(app,copy,out,control,"research_training_input_v2","input_hash",".scdev","training-input-");
            if(!compact) trainingFiles(app,copy,out,control,"research_training_attachment_cp12","attachment_hash",".scbind","training-bound-");
            JSONObject assets=new JSONObject();
            captureAsset(app,out,control,assets,"r925_software_audit.txt","software-audit-",".txt");
            if(!compact) for(String name:new String[]{"REQUIREMENTS_1832.csv","DETECTIONS_166.csv","TEST_CASES_328.csv","CARDS_299.json"})captureAsset(app,out,control,assets,"diagnostics/construction-"+name,"construction-",".txt");
            for(String name:new String[]{"REQUIREMENTS_1832.csv","CARDS_299.csv","SUBITEMS.csv","IDENTITY.json"})
                captureAsset(app,out,control,assets,"diagnostics/construction-current-"+name,
                        "construction-current-",".txt");
            source.put("assets",assets);
            // One bundled source index, streamed twice with exact hash; no repeated source TSV expansion in each section.
            MessageDigest digest=MessageDigest.getInstance("SHA-256");long indexBytes=0;try(InputStream index=app.getAssets().open(RuntimeChainDiagnosisR975.INDEX)){byte[] buffer=new byte[32768];int n;while((n=index.read(buffer))!=-1){checkpoint(control);digest.update(buffer,0,n);indexBytes=Math.addExact(indexBytes,n);}}
            String indexSha=ReportArchiveCP4.hex(digest.digest());source.put("sourceIndexSha256",indexSha);source.put("sourceIndexBytes",indexBytes);JSONObject indexRef=new JSONObject();indexRef.put("member","source-index-"+indexSha+".tsv");indexRef.put("sha256",indexSha);indexRef.put("bytes",indexBytes);source.getJSONObject("assets").put(RuntimeChainDiagnosisR975.INDEX,indexRef);
            descriptor(out,"source-index-"+indexSha+".tsv",indexSha,indexBytes,"ASSET",RuntimeChainDiagnosisR975.INDEX,"");
            out.flush();fd.getFD().sync();
        }
        File sorted=ReportMemberPlanCP12.sort(job,plans,control);total=0;
        try(ReportArchiveVerifierCP12.LineReader rows=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(sorted),8192)) {byte[] line;while((line=rows.next())!=null){checkpoint(control);JSONObject row=new JSONObject(new String(line,StandardCharsets.UTF_8));total=Math.addExact(total,row.getLong("bytes"));}}
        long available=new StatFs(app.getFilesDir().getPath()).getAvailableBytes();if(available<Math.addExact(Math.multiplyExact(2,total),64*MiB))throw new IOException("REPORT_ATTACHMENT_DISK_BUDGET");
        File manifest=new File(job,"attachments.jsonl");try(ReportArchiveVerifierCP12.LineReader rows=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(sorted),8192);FileOutputStream fd=new FileOutputStream(manifest);Writer refs=new OutputStreamWriter(fd,StandardCharsets.UTF_8)) {
            byte[] line;while((line=rows.next())!=null){checkpoint(control);JSONObject row=new JSONObject(new String(line,StandardCharsets.UTF_8));String hash=row.getString("sha256");File fixed=ReportArchiveVerifierCP12.inside(job,row.getString("path"));long bytes=row.getLong("bytes");
                if(fixed.exists()){if(fixed.length()!=bytes||!hash.equals(ReportArchiveCP4.hash(fixed,control)))throw new IOException("REPORT_FROZEN_ATTACHMENT_CHANGED");}
                else {
                    String kind=row.getString("sourceKind"),input=row.getString("source"),lock=row.getString("lock");
                    if(kind.equals("INSTALLED_APK")){File installed=new File(app.getApplicationInfo().sourceDir).getCanonicalFile();if(!installed.getPath().equals(input))throw new IOException("REPORT_INSTALLED_APK_PATH");try(InputStream in=new FileInputStream(installed)){copy(in,fixed,bytes,hash,control);}}
                    else if(kind.equals("ASSET")){try(InputStream in=app.getAssets().open(input)){copy(in,fixed,bytes,hash,control);}}
                    else if(kind.equals("TRAINING")){File root=new File(app.getFilesDir(),"research-input-v2").getCanonicalFile();String suffix=input.endsWith(".scdev")?".scdev":input.endsWith(".scbind")?".scbind":"";
                        try(com.ethk3.novaonepro.r952.TrainingCasCP12.LockedFile lease=com.ethk3.novaonepro.r952.TrainingCasCP12.open(root,hash,suffix);InputStream in=lease.open()){if(lease.bytes!=bytes)throw new IOException("REPORT_TRAINING_CAS_BYTES");copy(in,fixed,bytes,hash,control);}}
                    else if(kind.equals("OBSERVATION")){File file=new File(input),root=new File(app.getFilesDir(),"r981_reports/observations").getCanonicalFile();
                        if(!file.getCanonicalFile().equals(file.getAbsoluteFile())||!file.getCanonicalPath().startsWith(root.getPath()+File.separator))throw new IOException("REPORT_OBSERVATION_SOURCE_PATH");
                        // Unique request files are immutable after queue publication. Copy verifies every original byte.
                        try(InputStream in=new FileInputStream(file)){copy(in,fixed,bytes,hash,control);}}
                    else if(kind.equals("R26_CHECKPOINT")){File expected=ReportArchiveVerifierCP12.inside(new File(app.getFilesDir(),"r26-checkpoints").getCanonicalFile(),hash+".bin");if(!expected.getPath().equals(input))throw new IOException("REPORT_R26_CHECKPOINT_SOURCE_PATH");try(InputStream in=new FileInputStream(expected)){copy(in,fixed,bytes,hash,control);}}
                    else if(kind.equals("FILE")){File file=new File(input);if(!file.getCanonicalFile().equals(file.getAbsoluteFile())||!file.getCanonicalPath().startsWith(app.getFilesDir().getCanonicalPath()+File.separator))throw new IOException("REPORT_ATTACHMENT_SOURCE_PATH");
                        try(SharedLease lease=shared(new File(lock));InputStream in=new FileInputStream(file)){copy(in,fixed,bytes,hash,control);}}
                    else throw new IOException("REPORT_ATTACHMENT_KIND");
                }
                JSONObject ref=new JSONObject();ref.put("logicalName",row.getString("name"));ref.put("sourceKind",row.getString("sourceKind"));ref.put("sourceKey",row.getString("source"));ref.put("sha256",hash);ref.put("bytes",bytes);ref.put("state","FIXED_VERIFIED");String text=ref.toString()+"\n";
                if(text.getBytes(StandardCharsets.UTF_8).length>8192)throw new IOException("REPORT_ATTACHMENT_REF_BOUND");refs.write(text);
                ReportMemberPlanCP12.append(members,row.getString("name"),row.getString("path"),hash,bytes);
            }refs.flush();fd.getFD().sync();
        }
        if(!apkBefore.equals(ReportArchiveCP4.hash(new File(app.getApplicationInfo().sourceDir),control)))throw new IOException("REPORT_APK_CHANGED_DURING_CAPTURE");
        File sourceManifest=new File(job,"source-manifest.json");ReportJobJournalCP12.writeAtomic(sourceManifest,source.toString());
        ReportMemberPlanCP12.append(members,"source-manifest.json",sourceManifest.getName(),ReportArchiveCP4.hash(sourceManifest,control),sourceManifest.length());
        ReportMemberPlanCP12.append(members,"attachments.jsonl",manifest.getName(),ReportArchiveCP4.hash(manifest,control),manifest.length());
        JSONObject complete=new JSONObject();complete.put("format","CP12_FIXED_INPUT_CLOSURE_V1");complete.put("snapshotSha256",snapshotHash);complete.put("attachmentsSha256",ReportArchiveCP4.hash(manifest,control));complete.put("sourceSha256",ReportArchiveCP4.hash(sourceManifest,control));complete.put("observationSha256",ReportArchiveCP4.hash(observation,control));
        ReportJobJournalCP12.writeAtomic(new File(job,"inputs-complete.json"),complete.toString());
    }
    /** Stream availability evidence, never archived bodies, into compact diagnostics.
     * Exact body export remains the existing full-raw closure above; no duplicated payloads.
     */
    static JSONObject observeR80Archives(SQLiteDatabase snapshot,File files,File report,ReportNativeCP4.Control control)throws Exception {
        return observeR80Archives(snapshot,files,report,control,false);
    }
    static JSONObject observeR80Archives(SQLiteDatabase snapshot,File files,File report,ReportNativeCP4.Control control,boolean compact)throws Exception {
        final long byteBudget=8L*MiB;long[] remaining={byteBudget};
        long pages=0,rows=0,bytes=0,verified=0,unavailable=0,notRun=0;String last="";
        boolean present=hasTable(snapshot,"research_evidence_archive_v2");
        try(FileOutputStream fd=new FileOutputStream(report);Writer out=new BufferedWriter(new OutputStreamWriter(fd,StandardCharsets.UTF_8),8192)) {
            while(present) {
                checkpoint(control);String id,table,hash,path;long rowCount,locators;
                try(Cursor c=snapshot.rawQuery("SELECT a.archive_id,a.source_table,a.archive_hash,a.relative_path,a.row_count,(SELECT COUNT(*) FROM research_evidence_locator_v2 l WHERE l.archive_id=a.archive_id) FROM research_evidence_archive_v2 a WHERE a.archive_id>? AND a.source_table IN('r80_probe_input','r80_probe_forecast','r80_probe_label','r80_probe_opportunity','research_bound_source_r80','research_bound_input_r80') AND a.state IN('VERIFIED','REFERENCED','PRUNED') ORDER BY a.archive_id LIMIT 1",new String[]{last})) {
                    if(!c.moveToFirst())break;id=c.getString(0);table=c.getString(1);hash=c.getString(2);path=c.getString(3);rowCount=c.getLong(4);locators=c.getLong(5);
                }
                last=id;pages++;rows=Math.addExact(rows,rowCount);String status;long size=0;
                if(hash==null||!hash.matches("[0-9a-f]{64}")||!("retention-cas-cp12/"+hash+".scrow").equals(path))status="INVALID_REGISTRY_IDENTITY";
                else {
                    File original=ReportArchiveVerifierCP12.inside(files.getCanonicalFile(),path);
                    if(!original.isFile())status="MISSING_ARCHIVE";
                    else try(SharedLease lease=shared(new File(original.getParentFile(),hash+".lock"))) {
                        size=original.length();
                        String observedHash=compact?boundedArchiveHashR80(original,remaining,control):ReportArchiveCP4.hash(original,control);
                        status=observedHash==null?"NOT_RUN_COMPACT_BYTE_BUDGET":hash.equals(observedHash)?"VERIFIED_BYTES":"CORRUPT_HASH_MISMATCH";
                    }
                }
                bytes=Math.addExact(bytes,size);if(status.equals("VERIFIED_BYTES"))verified++;else if(status.equals("NOT_RUN_COMPACT_BYTE_BUDGET"))notRun++;else unavailable++;
                JSONObject row=new JSONObject().put("archiveId",id).put("sourceTable",table).put("expectedSha256",hash).put("relativePath",path).put("registeredRows",rowCount).put("auditLocators",locators).put("observedBytes",size).put("availability",status).put("bodyIncludedInCompact",false);
                out.write(row.toString());out.write('\n');
            }
            out.flush();fd.getFD().sync();
        }
        return new JSONObject().put("status",present?(unavailable>0?"ARCHIVE_EVIDENCE_UNAVAILABLE":notRun>0?"VERIFICATION_PARTIAL":"VERIFIED_ARCHIVE_AVAILABILITY"):"MISSING_SCHEMA").put("pages",pages).put("registeredRows",rows).put("observedTotalDiskBytes",bytes).put("verifiedPages",verified).put("missingOrCorruptPages",unavailable).put("byteVerificationNotRunPages",notRun).put("byteVerificationMode",compact?"COMPACT_CUMULATIVE_BYTE_BUDGET":"FULL_RAW_ALL_BYTES").put("compactByteVerificationBudget",compact?byteBudget:JSONObject.NULL).put("compactArchiveBytesRead",compact?byteBudget-remaining[0]:JSONObject.NULL).put("metadataMember",report.getName()).put("hotDatabaseBoundIsNotTotalDiskBound",true);
    }
    /** Budget accounts actual reads, including file growth. A skipped/partial hash is never verified. */
    static String boundedArchiveHashR80(File original,long[] remaining,ReportNativeCP4.Control control)throws Exception {
        checkpoint(control);
        // Reserve room to establish EOF. Equality conservatively skips rather than claiming a hash.
        if(remaining[0]<=0||original.length()>=remaining[0])return null;
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[32768];
        try(InputStream in=new FileInputStream(original)) {
            while(remaining[0]>0) {
                checkpoint(control);int n=in.read(buffer,0,(int)Math.min(buffer.length,remaining[0]));
                if(n<0)return ReportArchiveCP4.hex(digest.digest());
                if(n>0){remaining[0]-=n;digest.update(buffer,0,n);}
            }
        }
        return null;
    }
    private static void bundle(Context app,String hash,Writer out,ReportNativeCP4.Control control)throws Exception {
        File root=new File(app.getFilesDir(),"model-bundles-r929").getCanonicalFile(),directory=ReportArchiveVerifierCP12.inside(root,hash),lock=new File(root,hash+".lock");
        try(SharedLease lease=shared(lock)) {
            File manifest=ReportArchiveVerifierCP12.inside(directory,"manifest.json");String body=readBounded(manifest,65536);
            ModelBundleManagerR929.BundleManifest parsed=ModelBundleManagerR929.parse(body);if(!hash.equals(parsed.bundleHash))throw new IOException("REPORT_BUNDLE_IDENTITY");
            descriptorFile(out,manifest,lock,control);
            File signature=ReportArchiveVerifierCP12.inside(directory,"signature.bin");if(signature.length()<1||signature.length()>8192)throw new IOException("REPORT_SIGNATURE_BOUND");descriptorFile(out,signature,lock,control);
            File key=ReportArchiveVerifierCP12.inside(app.getFilesDir().getCanonicalFile(),"trusted-model-key-r932.der");if(key.length()<1||key.length()>8192)throw new IOException("REPORT_PUBLIC_KEY_BOUND");descriptorFile(out,key,lock,control);
            byte[] signatureBytes=readBytes(signature,8192),keyBytes=readBytes(key,8192);ModelBundleManagerR929.verifySignature(parsed,signatureBytes,ModelBundleRuntimeR932.decodeKey(keyBytes));
            for(ModelBundleManagerR929.ArtifactEntry artifact:parsed.artifacts){checkpoint(control);File file=ReportArchiveVerifierCP12.inside(directory,artifact.path);
                if(!file.isFile()||file.length()!=artifact.size||!artifact.sha256.equals(ReportArchiveCP4.hash(file,control)))throw new IOException("REPORT_MODEL_ARTIFACT_CHANGED");
                descriptor(out,"artifact-"+artifact.sha256+".bin",artifact.sha256,artifact.size,"FILE",file.getPath(),lock.getPath());}
        }
    }
    private static void descriptorFile(Writer out,File file,File lock,ReportNativeCP4.Control control)throws Exception {String hash=ReportArchiveCP4.hash(file,control);descriptor(out,"artifact-"+hash+".bin",hash,file.length(),"FILE",file.getCanonicalPath(),lock.getCanonicalPath());}
    private static void descriptor(Writer out,String name,String hash,long bytes,String kind,String source,String lock)throws Exception {requireHash(hash);if(bytes<0)throw new IOException("REPORT_ATTACHMENT_BYTES");JSONObject o=new JSONObject();o.put("name",name);o.put("path",fixedPath(hash));o.put("sha256",hash);o.put("bytes",bytes);o.put("sourceKind",kind);o.put("source",source);o.put("lock",lock);String line=o.toString()+"\n";if(line.getBytes(StandardCharsets.UTF_8).length>8192)throw new IOException("REPORT_ATTACHMENT_BOUND");out.write(line);}
    private static String state(SQLiteDatabase copy,String key)throws IOException {try(Cursor c=copy.rawQuery("SELECT length(CAST(state_value AS BLOB)) FROM app_state WHERE state_key=?",new String[]{key})){if(!c.moveToFirst())return "";if(c.isNull(0)||c.getLong(0)>128)throw new IOException("REPORT_BUNDLE_STATE_BOUND");}try(Cursor c=copy.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{key})){if(!c.moveToFirst())throw new IOException("REPORT_SNAPSHOT_STATE_DISAPPEARED");return c.isNull(0)?"":c.getString(0);}}
    private static void copy(InputStream input,File fixed,long expectedBytes,String expectedSha,ReportNativeCP4.Control control)throws Exception {
        File parent=fixed.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("REPORT_ATTACHMENT_BUCKET");File partial=new File(fixed.getPath()+".partial");if(partial.exists()&&!partial.delete())throw new IOException("REPORT_ATTACHMENT_PARTIAL_REMOVE");if(!partial.createNewFile())throw new IOException("REPORT_ATTACHMENT_PARTIAL_EXISTS");
        MessageDigest digest=MessageDigest.getInstance("SHA-256");long size=0;try(FileOutputStream out=new FileOutputStream(partial)){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1){checkpoint(control);if(n>expectedBytes-size)throw new IOException("REPORT_ATTACHMENT_OVERSIZE");out.write(buffer,0,n);digest.update(buffer,0,n);size+=n;}out.flush();out.getFD().sync();}
        if(size!=expectedBytes||!expectedSha.equals(ReportArchiveCP4.hex(digest.digest())))throw new IOException("REPORT_ATTACHMENT_COPY_HASH");if(!partial.renameTo(fixed))throw new IOException("REPORT_ATTACHMENT_RENAME");
    }
    private static final class SharedLease implements Closeable {final File path;final RandomAccessFile file;final FileLock lease;SharedLease(File path,RandomAccessFile file,FileLock lease){this.path=path;this.file=file;this.lease=lease;}public void close()throws IOException{try{lease.release();}finally{file.close();}}}
    private static SharedLease shared(File path)throws IOException {if(!path.getCanonicalFile().equals(path.getAbsoluteFile()))throw new IOException("REPORT_ATTACHMENT_LOCK_PATH");RandomAccessFile file=new RandomAccessFile(path,"rw");FileLock lease=null;try{lease=file.getChannel().tryLock(0,Long.MAX_VALUE,true);if(lease==null)throw new IOException("REPORT_ATTACHMENT_LEASE_BUSY");return new SharedLease(path,file,lease);}catch(IOException|RuntimeException|Error error){if(lease!=null)lease.release();file.close();throw error;}}
    private static byte[] readBytes(File file,int maximum)throws IOException {if(!file.isFile()||file.length()>maximum)throw new IOException("REPORT_SMALL_FILE_BOUND");try(InputStream input=new FileInputStream(file)){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int n;while((n=input.read(buffer))!=-1){if(n>maximum-out.size())throw new IOException("REPORT_SMALL_FILE_BOUND");out.write(buffer,0,n);}return out.toByteArray();}}
    private static String readBounded(File file,int maximum)throws IOException{return new String(readBytes(file,maximum),StandardCharsets.UTF_8);}
    private static void requireHash(String hash)throws IOException{if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IOException("REPORT_ATTACHMENT_HASH");}
    private static void checkpoint(ReportNativeCP4.Control control)throws IOException{if(Thread.currentThread().isInterrupted()||!control.shouldContinue())throw new InterruptedIOException("REPORT_CANCELLED");}
    public static long failedCheckCount(List<File> catalog)throws Exception {long failed=0;for(File page:catalog){try(ReportArchiveVerifierCP12.LineReader input=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(page),131072)){byte[] line;while((line=input.next())!=null){JSONObject row=new JSONObject(new String(line,StandardCharsets.UTF_8));if("check_result".equals(row.optString("kind"))&&"FAIL".equals(row.optString("state")))failed++;}}}return failed;}

    private static void trainingFiles(Context app,SQLiteDatabase snapshot,Writer out,ReportNativeCP4.Control control,String table,String hashColumn,String suffix,String prefix)throws Exception {
        if(!hasTable(snapshot,table))return; // absence is recorded in the frozen source manifest
        File root=new File(app.getFilesDir(),"research-input-v2").getCanonicalFile();String last="";
        while(true){checkpoint(control);String hash,relative;long bytes;
            try(Cursor row=snapshot.rawQuery("SELECT "+hashColumn+",relative_path,byte_count FROM "+table+" WHERE "+hashColumn+">? ORDER BY "+hashColumn+" LIMIT 1",new String[]{last})){
                if(!row.moveToFirst())break;hash=row.getString(0);relative=row.getString(1);bytes=row.getLong(2);
            }last=hash;requireHash(hash);if(!relative.equals(hash+suffix)||bytes<1||bytes>8*MiB)throw new IOException("REPORT_TRAINING_CAS_IDENTITY");
            File file=ReportArchiveVerifierCP12.inside(root,relative),lock=ReportArchiveVerifierCP12.inside(root,"publication.lock");
            try(com.ethk3.novaonepro.r952.TrainingCasCP12.LockedFile lease=com.ethk3.novaonepro.r952.TrainingCasCP12.open(root,hash,suffix)){checkpoint(control);if(lease.bytes!=bytes||!lease.file.equals(file))throw new IOException("REPORT_TRAINING_CAS_MISSING_OR_CHANGED");descriptor(out,prefix+hash+suffix,hash,bytes,"TRAINING",file.getPath(),lock.getPath());}
        }
    }
    private static void captureAsset(Context app,Writer out,ReportNativeCP4.Control control,JSONObject assets,String asset,String prefix,String suffix)throws Exception {
        MessageDigest digest=MessageDigest.getInstance("SHA-256");long bytes=0;try(InputStream in=app.getAssets().open(asset)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){checkpoint(control);digest.update(buffer,0,n);bytes=Math.addExact(bytes,n);}}
        String hash=ReportArchiveCP4.hex(digest.digest()),member=prefix+hash+suffix;descriptor(out,member,hash,bytes,"ASSET",asset,"");JSONObject ref=new JSONObject();ref.put("member",member);ref.put("sha256",hash);ref.put("bytes",bytes);assets.put(asset,ref);
    }
    /** Recovery can reuse an immutable snapshot only if every fixed external member is durably bound and present. */
    public static boolean isComplete(File job,String snapshotSha,ReportNativeCP4.Control control)throws Exception {
        File marker=new File(job,"inputs-complete.json");if(!marker.isFile())return false;JSONObject complete=new JSONObject(ReportJobJournalCP12.readAtomic(marker,4096));
        if(!complete.getString("format").equals("CP12_FIXED_INPUT_CLOSURE_V1")||!complete.getString("snapshotSha256").equals(snapshotSha))return false;
        for(String[] pair:new String[][]{{"attachments.jsonl","attachmentsSha256"},{"source-manifest.json","sourceSha256"},{"runtime-observation.json","observationSha256"}}){File file=ReportArchiveVerifierCP12.inside(job,pair[0]);if(!file.isFile()||!complete.getString(pair[1]).equals(ReportArchiveCP4.hash(file,control)))return false;}
        try(ReportArchiveVerifierCP12.LineReader rows=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(new File(job,"attachments.jsonl")),8192)){byte[] line;while((line=rows.next())!=null){checkpoint(control);JSONObject ref=new JSONObject(new String(line,StandardCharsets.UTF_8));String hash=ref.getString("sha256");requireHash(hash);File file=ReportArchiveVerifierCP12.inside(job,fixedPath(hash));if(!file.isFile()||file.length()!=ref.getLong("bytes")||!hash.equals(ReportArchiveCP4.hash(file,control)))return false;}}
        return true;
    }
    /** Reuses verified fixed members, without reopening a live CAS file after a crash. */
    public static void appendComplete(File job,Writer members,ReportNativeCP4.Control control)throws Exception {
        try(ReportArchiveVerifierCP12.LineReader rows=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(new File(job,"attachments.jsonl")),8192)){byte[] line;while((line=rows.next())!=null){JSONObject ref=new JSONObject(new String(line,StandardCharsets.UTF_8));String hash=ref.getString("sha256");ReportMemberPlanCP12.append(members,ref.getString("logicalName"),fixedPath(hash),hash,ref.getLong("bytes"));}}
        for(String file:new String[]{"attachments.jsonl","source-manifest.json","runtime-observation.json"}){File fixed=ReportArchiveVerifierCP12.inside(job,file);ReportMemberPlanCP12.append(members,file,file,ReportArchiveCP4.hash(fixed,control),fixed.length());}
    }

    private static boolean hasTable(SQLiteDatabase snapshot,String table) {
        try(Cursor c=snapshot.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){return c.moveToFirst();}
    }

    private static String fixedPath(String hash){return "attachments/"+hash.substring(0,2)+"/"+hash.substring(2,4)+"/"+hash+".fixed";}
}
