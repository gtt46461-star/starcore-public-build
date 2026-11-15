package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.FileOutputStream;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * R929 atomic model-bundle verifier/stager/activator.
 * A bundle is all-or-nothing: Number/Gate/Ranker/Calibration/Predictability/Policy can never be
 * mixed across versions.  APK signing is intentionally unrelated to the model signature.
 */
public final class ModelBundleManagerR929 {
    public static final String REVISION = "R929_MODEL_BUNDLE_V1";
    public static final long MAX_ARTIFACT_BYTES = 16L * 1024L * 1024L;
    public static final long MAX_BUNDLE_BYTES = 48L * 1024L * 1024L;
    public static final String STATE_ACTIVE = "r929_model_bundle_active";
    public static final String STATE_PREVIOUS = "r929_model_bundle_previous";
    public static final String STATE_STAGED = "r929_model_bundle_staged";
    public static final String STATE_ACTIVATED_ISSUE = "r929_model_bundle_activated_issue";
    public static final String STATE_STATUS = "r929_model_bundle_status";
    private static final List<String> REQUIRED = Collections.unmodifiableList(Arrays.asList(
            "NUMBER", "GATE", "RANKER", "CALIBRATION", "PREDICTABILITY", "POLICY"));

    private ModelBundleManagerR929() {}

    public static final class ArtifactEntry {
        public final String type, path, sha256, inputStage, schemaHash, parentSha256;
        public final long size;
        ArtifactEntry(String type,String path,String sha256,long size,String inputStage,
                      String schemaHash,String parentSha256){
            this.type=required(type).toUpperCase(Locale.US);this.path=safePath(path);
            this.sha256=hash(sha256);this.size=size;this.inputStage=required(inputStage);
            this.schemaHash=hash(schemaHash);this.parentSha256=parentSha256==null||parentSha256.trim().isEmpty()?"":hash(parentSha256);
            if(size<0||size>MAX_ARTIFACT_BYTES)throw new IllegalArgumentException("R929_ARTIFACT_SIZE:"+this.type);
        }
        String canonical(){return type+'|'+path+'|'+sha256+'|'+size+'|'+inputStage+'|'+schemaHash+'|'+parentSha256;}
    }

    public static final class BundleManifest {
        public final String bundleHash, selectionPolicyHash, inputSchemaHash, certificateHash;
        public final List<ArtifactEntry> artifacts;
        public final String canonicalHash;
        BundleManifest(String bundleHash,String selectionPolicyHash,String inputSchemaHash,
                       String certificateHash,List<ArtifactEntry> artifacts){
            this.bundleHash=hash(bundleHash);this.selectionPolicyHash=hash(selectionPolicyHash);
            this.inputSchemaHash=hash(inputSchemaHash);this.certificateHash=hash(certificateHash);
            this.artifacts=Collections.unmodifiableList(new ArrayList<ArtifactEntry>(artifacts));
            validateStructure();
            StringBuilder b=new StringBuilder(2048).append(REVISION).append('|')
                    .append(this.selectionPolicyHash).append('|').append(this.inputSchemaHash)
                    .append('|').append(this.certificateHash);
            ArrayList<ArtifactEntry> sorted=new ArrayList<ArtifactEntry>(this.artifacts);
            Collections.sort(sorted,new Comparator<ArtifactEntry>(){public int compare(ArtifactEntry a,ArtifactEntry c){return a.type.compareTo(c.type);}});
            for(ArtifactEntry e:sorted)b.append('\n').append(e.canonical());
            canonicalHash=sha256(b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            if(!this.bundleHash.equals(canonicalHash))throw new IllegalArgumentException("R929_BUNDLE_HASH_MISMATCH");
        }
        private void validateStructure(){
            Map<String,ArtifactEntry> byType=new HashMap<String,ArtifactEntry>();java.util.HashSet<String> paths=new java.util.HashSet<String>();long total=0;
            for(ArtifactEntry e:artifacts){if(!REQUIRED.contains(e.type)||!paths.add(e.path))throw new IllegalArgumentException("R932_TYPE_OR_PATH_DUPLICATE");if(byType.put(e.type,e)!=null)throw new IllegalArgumentException("R929_DUP_ARTIFACT:"+e.type);total+=e.size;}
            if(total>MAX_BUNDLE_BYTES)throw new IllegalArgumentException("R929_BUNDLE_TOO_LARGE");
            for(String type:REQUIRED)if(!byType.containsKey(type))throw new IllegalArgumentException("R929_REQUIRED_ARTIFACT_MISSING:"+type);
            if(!inputSchemaHash.equals(byType.get("RANKER").schemaHash)||!inputSchemaHash.equals(byType.get("CALIBRATION").schemaHash))
                throw new IllegalArgumentException("R929_BUNDLE_SCHEMA_SPLIT");
            if(!byType.get("CALIBRATION").parentSha256.equals(byType.get("RANKER").sha256))
                throw new IllegalArgumentException("R929_CAL_PARENT_NOT_RANKER");
            if(!byType.get("POLICY").parentSha256.equals(byType.get("CALIBRATION").sha256))
                throw new IllegalArgumentException("R929_POLICY_PARENT_NOT_CALIBRATION");
        }
    }

    public static BundleManifest parse(String json){
        try{
            JSONObject root=new JSONObject(required(json));
            if(!REVISION.equals(root.getString("revision")))throw new IllegalArgumentException("R929_BUNDLE_REVISION");
            JSONArray a=root.getJSONArray("artifacts");ArrayList<ArtifactEntry> entries=new ArrayList<ArtifactEntry>(a.length());
            for(int i=0;i<a.length();i++){
                JSONObject x=a.getJSONObject(i);entries.add(new ArtifactEntry(x.getString("type"),x.getString("path"),x.getString("sha256"),x.getLong("size"),x.getString("input_stage"),x.getString("schema_sha256"),x.optString("parent_sha256","")));
            }
            return new BundleManifest(root.getString("bundle_sha256"),root.getString("selection_policy_sha256"),root.getString("input_schema_sha256"),root.getString("certificate_sha256"),entries);
        }catch(RuntimeException e){throw e;}catch(Throwable e){throw new IllegalArgumentException("R929_BUNDLE_PARSE",e);}
    }

    public static void verifyBytes(BundleManifest manifest,Map<String,byte[]> files){
        if(manifest==null||files==null)throw new IllegalArgumentException("R929_BUNDLE_BYTES_NULL");
        long total=0;
        for(ArtifactEntry e:manifest.artifacts){
            byte[] bytes=files.get(e.path);if(bytes==null)throw new IllegalArgumentException("R929_BUNDLE_FILE_MISSING:"+e.path);
            if(bytes.length!=e.size)throw new IllegalArgumentException("R929_BUNDLE_SIZE_MISMATCH:"+e.path);
            total+=bytes.length;if(total>MAX_BUNDLE_BYTES)throw new IllegalArgumentException("R929_BUNDLE_TOO_LARGE");
            if(!e.sha256.equals(sha256(bytes)))throw new IllegalArgumentException("R929_BUNDLE_FILE_HASH:"+e.path);
        }
        if(files.size()!=manifest.artifacts.size())throw new IllegalArgumentException("R929_BUNDLE_EXTRA_FILE");
    }

    public static void verifySignature(BundleManifest manifest,byte[] signature,PublicKey trustedKey){
        if(manifest==null||signature==null||signature.length==0||trustedKey==null)throw new IllegalArgumentException("R929_MODEL_SIGNATURE_REQUIRED");
        try{
            Signature verifier=Signature.getInstance("SHA256withRSA");verifier.initVerify(trustedKey);
            verifier.update(manifest.bundleHash.getBytes("UTF-8"));
            if(!verifier.verify(signature))throw new IllegalArgumentException("R929_MODEL_SIGNATURE_INVALID");
        }catch(RuntimeException e){throw e;}catch(Throwable e){throw new IllegalStateException("R929_MODEL_SIGNATURE_VERIFY_FAILED",e);}
    }

    /** Writes a verified bundle to an isolated directory; partial staging is deleted and never pointed to. */
    public static synchronized File stageToDisk(File filesDir,BundleManifest manifest,Map<String,byte[]> files,byte[] signature,PublicKey trustedKey){
        verifyBytes(manifest,files);verifySignature(manifest,signature,trustedKey);
        if(filesDir==null)throw new IllegalArgumentException("R929_FILES_DIR_NULL");
        File root=new File(filesDir,"model-bundles-r929"),tmp=new File(root,"tmp-"+manifest.bundleHash),dst=new File(root,manifest.bundleHash);
        if(dst.exists()){ verifyDisk(dst,manifest,signature,trustedKey); return dst; }
        deleteTree(tmp);if(!tmp.mkdirs())throw new IllegalStateException("R929_STAGE_MKDIR");
        try{
            for(ArtifactEntry e:manifest.artifacts){File f=new File(tmp,e.path);File parent=f.getParentFile();if(parent!=null&&!parent.isDirectory()&&!parent.mkdirs())throw new IllegalStateException("R929_STAGE_PARENT");
                try(FileOutputStream out=new FileOutputStream(f)){out.write(files.get(e.path));out.getFD().sync();}}
            File marker=new File(tmp,"VERIFIED.bundle");try(FileOutputStream out=new FileOutputStream(marker)){out.write(manifest.bundleHash.getBytes("UTF-8"));out.getFD().sync();}
            writeSynced(new File(tmp,"manifest.json"),toJson(manifest).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            writeSynced(new File(tmp,"signature.bin"),signature);
            if(!tmp.renameTo(dst))throw new IllegalStateException("R929_STAGE_RENAME");return dst;
        }catch(RuntimeException e){deleteTree(tmp);throw e;}catch(Throwable e){deleteTree(tmp);throw new IllegalStateException("R929_STAGE_IO",e);}
    }

    public static String captureActiveBundle(DbStore store,String fallback){String x=store==null?"":store.getState(STATE_ACTIVE,"");return x.length()==64?x:safe(fallback);}

    public static String toJson(BundleManifest m) {
        try {
            JSONObject r=new JSONObject();r.put("revision",REVISION);r.put("bundle_sha256",m.bundleHash);
            r.put("selection_policy_sha256",m.selectionPolicyHash);r.put("input_schema_sha256",m.inputSchemaHash);
            r.put("certificate_sha256",m.certificateHash);JSONArray a=new JSONArray();
            for(ArtifactEntry e:m.artifacts){JSONObject x=new JSONObject();x.put("type",e.type);x.put("path",e.path);
                x.put("sha256",e.sha256);x.put("size",e.size);x.put("input_stage",e.inputStage);
                x.put("schema_sha256",e.schemaHash);x.put("parent_sha256",e.parentSha256);a.put(x);}
            r.put("artifacts",a);return r.toString();
        }catch(Exception e){throw new IllegalStateException("R932_MANIFEST_SERIALIZE",e);}
    }

    public static void verifyDisk(File directory,BundleManifest manifest,byte[] signature,PublicKey key) {
        verifySignature(manifest,signature,key);
        Map<String,byte[]> files=new LinkedHashMap<String,byte[]>();
        try {
            String root=directory.getCanonicalPath()+File.separator;
            for(ArtifactEntry e:manifest.artifacts){File file=new File(directory,e.path);
                if(!file.getCanonicalPath().startsWith(root)||file.length()!=e.size)
                    throw new IllegalArgumentException("R932_DISK_PATH_OR_SIZE");
                try(java.io.InputStream in=new java.io.FileInputStream(file)){
                    files.put(e.path,readBounded(in,MAX_ARTIFACT_BYTES));}}
            verifyBytes(manifest,files);
        }catch(java.io.IOException e){throw new IllegalStateException("R932_DISK_VERIFY",e);}
    }

    static byte[] readBounded(java.io.InputStream in,long limit) throws java.io.IOException {
        java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();byte[] buffer=new byte[8192];int n;
        while((n=in.read(buffer))!=-1){if((long)out.size()+n>limit)throw new java.io.IOException("R932_INPUT_LIMIT");out.write(buffer,0,n);}
        return out.toByteArray();
    }
    private static void writeSynced(File file,byte[] bytes) throws java.io.IOException {
        try(FileOutputStream out=new FileOutputStream(file)){out.write(bytes);out.getFD().sync();}
    }

    private static void deleteTree(File f){if(f==null||!f.exists())return;if(f.isDirectory()){File[] c=f.listFiles();if(c!=null)for(File x:c)deleteTree(x);}if(!f.delete()&&f.exists())throw new IllegalStateException("R929_DELETE_FAILED:"+f.getName());}
    private static String safePath(String p){String x=required(p).replace('\\','/');if(x.startsWith("/")||x.contains(":")||x.contains("//")||java.util.Arrays.asList(x.split("/",-1)).contains("..")||java.util.Arrays.asList(x.split("/",-1)).contains(".")||x.endsWith("/")||x.equals("manifest.json")||x.equals("signature.bin")||x.equals("VERIFIED.bundle"))throw new IllegalArgumentException("R929_UNSAFE_PATH");return x;}
    private static String required(String x){String v=x==null?"":x.trim();if(v.length()==0)throw new IllegalArgumentException("R929_REQUIRED");return v;}
    private static String safe(String x){return x==null?"":x.trim();}
    private static String hash(String x){String v=required(x).toLowerCase(Locale.US);if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R929_HASH");return v;}
    private static String sha256(byte[] bytes){try{byte[] h=MessageDigest.getInstance("SHA-256").digest(bytes);StringBuilder b=new StringBuilder(64);for(byte v:h)b.append(String.format(Locale.US,"%02x",v&255));return b.toString();}catch(Throwable e){throw new IllegalStateException("R929_SHA256",e);}}
}
