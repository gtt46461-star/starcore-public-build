package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** Private offline collector protocol. A properties map never grants cleanup.
 * The parent admits a fresh nonce, synchronously queries the bound process PID,
 * then records only under the original closed-pool maintenance owner. */
final class StorageRestoreProofR31 {
    static final String METHOD="ANDROID_SEPARATE_PROCESS_FULL_RESTORE_V1";
    private static final ThreadLocal<Session> RECORDING=new ThreadLocal<>();
    static final class Session {
        final File active,root,clone,request,result;final String nonce,activeHash,cloneHash,requestHash;final int parentPid;
        int observedProbePid;String observedResultHash;boolean recorded;
        private Session(File active,File root,File clone,String nonce,String activeHash,String cloneHash,int parentPid,File request,File result,String requestHash){this.active=active;this.root=root;this.clone=clone;this.nonce=nonce;this.activeHash=activeHash;this.cloneHash=cloneHash;this.parentPid=parentPid;this.request=request;this.result=result;this.requestHash=requestHash;}
    }
    static String digest(String value)throws IOException {try{return hex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IOException(e);}}
    private static String hex(byte[] bytes){StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}
    static String canonical(Properties p){StringBuilder b=new StringBuilder();for(String key:new TreeSet<>(p.stringPropertyNames()))if(key.startsWith("restore_")&&!key.equals("restore_collector_envelope_sha256")){String value=p.getProperty(key);b.append(key.length()).append(':').append(key).append(value.length()).append(':').append(value);}return b.toString();}
    static File child(File root,String relative)throws IOException {File base=root.getCanonicalFile(),file=new File(base,relative).getAbsoluteFile();if(!file.equals(file.getCanonicalFile())||!file.getPath().startsWith(base.getPath()+File.separator))throw new IOException("RESTORE_PRIVATE_PATH");return file;}
    static Session prepare(File active,File directory,String nonce,int parentPid,String source)throws IOException {
        if(!DatabaseCoordinator.isClosedOfflineOwnerR31())throw new IOException("RESTORE_CLOSED_OFFLINE_OWNER_REQUIRED");
        if(!nonce.matches("[0-9a-f]{64}")||parentPid<=0||!source.matches("[0-9a-f]{40}"))throw new IOException("RESTORE_REQUEST_IDENTITY");
        active=active.getCanonicalFile();File root=child(directory,nonce+"/"+ReleaseManifestR926.APPLICATION_ID);
        if(root.exists()||!new File(root,"databases").mkdirs()||!new File(root,"files").mkdirs())throw new IOException("RESTORE_EXCLUSIVE_WORKSPACE_REQUIRED");
        File clone=new File(root,"databases/"+AppConfig.DB_NAME);String activeHash=StorageMaintenanceFileR82.sha256(active);
        String fullCopy=StorageMaintenanceFileR82.prepareRestoreCopyR31(active,clone),cloneHash=StorageMaintenanceFileR82.sha256(clone);
        Properties assets=new Properties();long[] used={0,0};File originalRoot=active.getParentFile().getParentFile();
        for(String path:new String[]{"files/r26-checkpoints","files/r31-keep","files/model-bundles-r929","files/trusted-model-key-r932.der","files/r31-history-inputs","files/research-input-v2","databases/su1-archives"})copyTree(originalRoot,root,path,assets,used,0);
        File assetFile=new File(root,"asset-manifest.properties");StorageMaintenanceFileR82.write(assetFile,assets);
        Properties request=new Properties();request.setProperty("nonce",nonce);request.setProperty("source",source);request.setProperty("parent_pid",Integer.toString(parentPid));request.setProperty("active_sha256",activeHash);request.setProperty("clone_sha256",cloneHash);request.setProperty("native_full_compare",fullCopy);request.setProperty("asset_manifest_sha256",StorageMaintenanceFileR82.sha256(assetFile));request.setProperty("asset_files",Long.toString(used[1]));request.setProperty("asset_bytes",Long.toString(used[0]));
        File requestFile=new File(root,"request.properties");StorageMaintenanceFileR82.write(requestFile,request);
        if(!activeHash.equals(StorageMaintenanceFileR82.sha256(active)))throw new IOException("RESTORE_ACTIVE_CHANGED_AFTER_PREPARATION");
        return new Session(active,root,clone,nonce,activeHash,cloneHash,parentPid,requestFile,new File(root,"result.properties"),StorageMaintenanceFileR82.sha256(requestFile));
    }
    private static void copyTree(File original,File copy,String relative,Properties manifest,long[] used,int depth)throws IOException {
        File source=child(original,relative);if(!source.exists())return;if(depth>16)throw new IOException("WAIT_RESTORE_ASSET_DEPTH");
        File target=child(copy,relative);
        if(source.isDirectory()){if(!target.mkdirs()&&!target.isDirectory())throw new IOException("RESTORE_ASSET_DIRECTORY");String[] names=source.list();if(names==null||names.length>16384)throw new IOException("WAIT_RESTORE_ASSET_ENUMERATION_BOUND");Arrays.sort(names);for(String name:names)copyTree(original,copy,relative+"/"+name,manifest,used,depth+1);return;}
        if(!source.isFile()||++used[1]>16384||(used[0]=Math.addExact(used[0],source.length()))>(1L<<30))throw new IOException("WAIT_RESTORE_ASSET_BOUND");
        if(target.getParentFile().getUsableSpace()<source.length()+(64L<<20))throw new IOException("WAIT_RESTORE_ASSET_DISK_CAPACITY");
        String hash=StorageMaintenanceFileR82.sha256(source);try(InputStream in=new FileInputStream(source);FileOutputStream out=new FileOutputStream(target)){byte[] b=new byte[65536];for(int n;(n=in.read(b))!=-1;)out.write(b,0,n);out.getFD().sync();}
        if(!hash.equals(StorageMaintenanceFileR82.sha256(source))||!hash.equals(StorageMaintenanceFileR82.sha256(target)))throw new IOException("RESTORE_ASSET_CHANGED");manifest.setProperty(relative,hash+":"+source.length());
    }
    static void authenticateCallback(Session s,String nonce,int observedBoundPid,String resultHash)throws IOException {
        if(!s.nonce.equals(nonce)||observedBoundPid<=0||observedBoundPid==s.parentPid||resultHash==null||!resultHash.matches("[0-9a-f]{64}"))throw new IOException("RESTORE_PROBE_CALLER_IDENTITY");
        if(!resultHash.equals(StorageMaintenanceFileR82.sha256(s.result)))throw new IOException("RESTORE_PROBE_RESULT_CHANGED");
        s.observedProbePid=observedBoundPid;s.observedResultHash=resultHash;
    }
    static void recordCollected(Session s)throws IOException {
        Properties proof=StorageMaintenanceFileR82.read(s.result);
        if(s.recorded||s.observedProbePid<=0||s.observedResultHash==null||!s.observedResultHash.equals(StorageMaintenanceFileR82.sha256(s.result))||!s.requestHash.equals(StorageMaintenanceFileR82.sha256(s.request)))throw new IOException("RESTORE_AUTHENTICATED_COLLECTOR_REQUIRED");
        for(String[] pair:new String[][]{{"restore_nonce",s.nonce},{"restore_active_sha256",s.activeHash},{"restore_clone_sha256",s.cloneHash},{"restore_parent_pid",Integer.toString(s.parentPid)},{"restore_probe_pid",Integer.toString(s.observedProbePid)},{"restore_request_sha256",s.requestHash}})if(!pair[1].equals(proof.getProperty(pair[0])))throw new IOException("RESTORE_COLLECTOR_ENVELOPE_IDENTITY:"+pair[0]);
        proof.setProperty("restore_probe_receipt_sha256",s.observedResultHash);proof.setProperty("restore_collector_authenticated","BINDER_PID_AND_FRESH_OFFLINE_NONCE");proof.setProperty("restore_collector_envelope_sha256",digest(canonical(proof)));
        RECORDING.set(s);try{StorageMaintenanceFileR82.recordRestoreProofR31(s.active,proof);s.recorded=true;}finally{RECORDING.remove();}
    }
    static void requireRecordingAuthority(File active,Properties proof)throws IOException {
        if(!DatabaseCoordinator.isClosedOfflineOwnerR31())throw new IOException("REFUSED_RESTORE_CLOSED_OFFLINE_OWNER_REQUIRED");
        Session s=RECORDING.get();if(s==null||s.recorded||!s.active.equals(active.getCanonicalFile())||!s.nonce.equals(proof.getProperty("restore_nonce")))throw new IOException("REFUSED_NO_AUTHENTIC_COLLECTOR");
    }
    /** Only the private verification copy is disposable after its complete
     * receipt is durable and the service observed the probe's actual death.
     * The original backup is handled solely by the separate user action. */
    static boolean discardVerifiedWorkspace(Session session)throws IOException {
        if(!session.recorded)throw new IOException("RESTORE_DURABLE_RECEIPT_REQUIRED_BEFORE_WORKSPACE_RELEASE");
        boolean removed=removePrivateTree(session.root,session.root,new int[]{0},0);
        if(removed)session.root.getParentFile().delete();return removed;
    }
    private static boolean removePrivateTree(File root,File file,int[] count,int depth)throws IOException {
        if(++count[0]>20000||depth>20||!file.getAbsoluteFile().equals(file.getCanonicalFile())||(!file.equals(root)&&!file.getPath().startsWith(root.getPath()+File.separator)))throw new IOException("RESTORE_WORKSPACE_RELEASE_BOUND");
        if(file.isDirectory()){File[] children=file.listFiles();if(children==null||children.length>16384)throw new IOException("RESTORE_WORKSPACE_RELEASE_ENUMERATION");for(File child:children)if(!removePrivateTree(root,child,count,depth+1))return false;}
        return !file.exists()||file.delete();
    }
    static String receiptRefusal(Properties p){
        if(!METHOD.equals(p.getProperty("restore_method")))return "REFUSED_UNAUTHENTICATED_RESTORE_COLLECTOR";
        if(!"BINDER_PID_AND_FRESH_OFFLINE_NONCE".equals(p.getProperty("restore_collector_authenticated")))return "REFUSED_UNAUTHENTICATED_RESTORE_COLLECTOR";
        if(!p.getProperty("restore_source_commit","").matches("[0-9a-f]{40}")||!"PRIVATE_PROCESS_NO_CREDENTIAL_OWNER_NO_LIVE_DB".equals(p.getProperty("restore_application_boot")))return "REFUSED_RESTORE_COLLECTOR_SOURCE_OR_PROCESS_BOOT";
        for(String key:new String[]{"restore_nonce","restore_clone_sha256","restore_request_sha256","restore_probe_receipt_sha256","restore_schema_sha256","restore_collector_envelope_sha256"})if(!p.getProperty(key,"").matches("[0-9a-f]{64}"))return "REFUSED_RESTORE_ENVELOPE_FIELD:"+key;
        try{int parent=Integer.parseInt(p.getProperty("restore_parent_pid","0")),probe=Integer.parseInt(p.getProperty("restore_probe_pid","0"));if(parent<=0||probe<=0||parent==probe)return "REFUSED_RESTORE_PROCESS_IDENTITY";if(!digest(canonicalRestoreOnly(p)).equals(p.getProperty("restore_collector_envelope_sha256")))return "REFUSED_RESTORE_ENVELOPE_CHANGED";}
        catch(Exception unavailable){return "REFUSED_RESTORE_ENVELOPE_INVALID";}return null;
    }
    private static String canonicalRestoreOnly(Properties original){Properties p=new Properties();for(String key:original.stringPropertyNames())if(key.startsWith("restore_"))p.setProperty(key,original.getProperty(key));return canonical(p);}
}
