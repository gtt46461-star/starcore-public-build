package com.ethk3.novaonepro;

import java.io.*;
import java.util.*;

/** Final single-file effect under the original maintenance owner and exclusive
 * recovery-point lease. Evidence/authorization is checked by the concrete app
 * owner immediately before this method and again at the irreversible boundary.
 * An intent survives unknown unlink/fsync outcomes; retries never select a new file. */
final class LegacyRetirementFileR335 {
    interface Gate {void check()throws Exception;default void beginCommit()throws Exception{check();}}
    interface Filesystem {String identity(File file)throws IOException;long available(File directory)throws IOException;}
    static final class Result {final String state;final long removedLength,availableBefore,availableAfter;
        Result(String s,long b,long before,long after){state=s;removedLength=b;availableBefore=before;availableAfter=after;}
        long observedFreeDelta(){return availableAfter-availableBefore;}}
    /** Reconcile an already absent exact source after a lost final acknowledgement.
     * This entry cannot unlink anything and does not create deletion authority. */
    static Result reconcile(File database,String basename,String sourceHash,long bytes,String sourceIdentity,String manifestHash,
                            String evidenceHash,File intent,Filesystem filesystem)throws Exception {
        if(!DatabaseCoordinator.isClosedOfflineOwnerR31())throw new IOException("R335_RETIRE_CLOSED_OWNER_REQUIRED");
        LegacyBackupRecoveryR335.requireName(basename);if(!intent.isFile()||!intent.getAbsoluteFile().equals(intent.getCanonicalFile())||intent.length()>131072)throw new IOException("R335_RETIRE_RECONCILE_INTENT_REQUIRED");
        Properties prior=StorageMaintenanceFileR82.read(intent);
        for(String[] pair:new String[][]{{"source",basename},{"source_sha256",sourceHash},{"source_bytes",Long.toString(bytes)},{"source_identity",sourceIdentity},{"manifest_sha256",manifestHash},{"evidence_sha256",evidenceHash}})if(!pair[1].equals(prior.getProperty(pair[0])))throw new IOException("R335_RETIRE_RECONCILE_IDENTITY");
        if(!Arrays.asList("DELETE_INTENT","RETIRED_WITH_EXTERNAL_CUSTODY","UNLINK_OBSERVED_AFTER_UNKNOWN_COMMIT").contains(prior.getProperty("state")))throw new IOException("R335_RETIRE_RECONCILE_PHASE");
        File source=LegacyCustodyArchiveR335.child(database.getParentFile(),basename),manifest=LegacyCustodyArchiveR335.child(database.getParentFile(),basename+".manifest.txt");
        if(source.exists())throw new IOException("R335_RETIRE_RECONCILE_SOURCE_PRESENT_NO_DELETE_PERFORMED");
        if(!manifest.isFile()||!manifestHash.equals(StorageRestoreAssetsR324.hash(manifest)))throw new IOException("R335_RETIRE_RECONCILE_MANIFEST_CHANGED");
        LegacyCustodyArchiveR335.sync(database.getParentFile());long before=Long.parseLong(prior.getProperty("available_before")),after=filesystem.available(database.getParentFile());
        String state="RETIRED_WITH_EXTERNAL_CUSTODY".equals(prior.getProperty("state"))?"ALREADY_RETIRED":"UNLINK_OBSERVED_AFTER_UNKNOWN_COMMIT";
        prior.setProperty("state",state.equals("ALREADY_RETIRED")?"RETIRED_WITH_EXTERNAL_CUSTODY":state);prior.setProperty("available_after",Long.toString(after));StorageMaintenanceFileR82.write(intent,prior);return new Result(state,bytes,before,after);
    }
    static Result retire(File database,String basename,String sourceHash,long bytes,String sourceIdentity,String manifestHash,
                         String authorizationHash,String evidenceHash,File intent,
                         LegacyRecoveryPointLeaseR335 exclusive,Filesystem filesystem,Gate gate)throws Exception {
        LegacyBackupRecoveryR335.requireName(basename);exclusive.requireExclusive(database);
        if(!DatabaseCoordinator.isClosedOfflineOwnerR31())throw new IOException("R335_RETIRE_CLOSED_OWNER_REQUIRED");
        if(!sourceHash.matches("[0-9a-f]{64}")||!manifestHash.matches("[0-9a-f]{64}")||!authorizationHash.matches("[0-9a-f]{64}")||!evidenceHash.matches("[0-9a-f]{64}")||bytes<512||sourceIdentity==null||sourceIdentity.isEmpty())throw new IOException("R335_RETIRE_EXACT_IDENTITY_REQUIRED");
        File source=LegacyCustodyArchiveR335.child(database.getParentFile(),basename),manifest=LegacyCustodyArchiveR335.child(database.getParentFile(),basename+".manifest.txt");
        if(!intent.getAbsoluteFile().equals(intent.getCanonicalFile()))throw new IOException("R335_RETIRE_INTENT_PATH");
        Properties prior=intent.isFile()?StorageMaintenanceFileR82.read(intent):null;
        if(prior!=null){for(String[] pair:new String[][]{{"source",basename},{"source_sha256",sourceHash},{"source_bytes",Long.toString(bytes)},{"source_identity",sourceIdentity},{"manifest_sha256",manifestHash},{"evidence_sha256",evidenceHash}})
            if(!pair[1].equals(prior.getProperty(pair[0])))throw new IOException("R335_RETIRE_INTENT_CONFLICT:"+pair[0]);}
        gate.check();exclusive.requireExclusive(database);
        if(!manifest.isFile()||!manifestHash.equals(StorageRestoreAssetsR324.hash(manifest)))throw new IOException("R335_RETIRE_MANIFEST_CHANGED");
        if(!source.exists()){
            if(prior==null||!Arrays.asList("DELETE_INTENT","RETIRED_WITH_EXTERNAL_CUSTODY","UNLINK_OBSERVED_AFTER_UNKNOWN_COMMIT").contains(prior.getProperty("state")))throw new IOException("R335_RETIRE_SOURCE_MISSING_WITHOUT_INTENT");
            long before=Long.parseLong(prior.getProperty("available_before")),after=filesystem.available(database.getParentFile());
            String state="RETIRED_WITH_EXTERNAL_CUSTODY".equals(prior.getProperty("state"))?"ALREADY_RETIRED":"UNLINK_OBSERVED_AFTER_UNKNOWN_COMMIT";
            prior.setProperty("state",state.equals("ALREADY_RETIRED")?"RETIRED_WITH_EXTERNAL_CUSTODY":state);prior.setProperty("available_after",Long.toString(after));
            StorageMaintenanceFileR82.write(intent,prior);return new Result(state,bytes,before,after);
        }
        if(prior!=null&&Arrays.asList("RETIRED_WITH_EXTERNAL_CUSTODY","UNLINK_OBSERVED_AFTER_UNKNOWN_COMMIT").contains(prior.getProperty("state")))throw new IOException("R335_RETIRE_SOURCE_REAPPEARED");
        StorageMaintenanceFileR82.requireNoSidecars(source);
        if(!source.isFile()||source.length()!=bytes||!sourceIdentity.equals(filesystem.identity(source))||!sourceHash.equals(StorageRestoreAssetsR324.hash(source)))throw new IOException("R335_RETIRE_SOURCE_REPLACED");
        long before=filesystem.available(database.getParentFile());Properties next=new Properties();
        next.setProperty("source",basename);next.setProperty("source_sha256",sourceHash);next.setProperty("source_bytes",Long.toString(bytes));next.setProperty("source_identity",sourceIdentity);
        next.setProperty("manifest_sha256",manifestHash);next.setProperty("authorization_sha256",authorizationHash);next.setProperty("evidence_sha256",evidenceHash);
        next.setProperty("available_before",Long.toString(before));next.setProperty("state","DELETE_INTENT");StorageMaintenanceFileR82.write(intent,next);
        // The concrete caller serializes this check + unlink against revocation.
        gate.check();exclusive.requireExclusive(database);StorageMaintenanceFileR82.requireNoSidecars(source);
        if(!sourceIdentity.equals(filesystem.identity(source))||source.length()!=bytes||!sourceHash.equals(StorageRestoreAssetsR324.hash(source)))throw new IOException("R335_RETIRE_SOURCE_CHANGED_AT_COMMIT");
        gate.beginCommit();
        if(!source.delete())throw new IOException("R335_RETIRE_UNLINK_NOT_ACKNOWLEDGED");
        LegacyCustodyArchiveR335.sync(database.getParentFile());
        if(source.exists())throw new IOException("R335_RETIRE_UNLINK_UNKNOWN");
        long after=filesystem.available(database.getParentFile());next.setProperty("state","RETIRED_WITH_EXTERNAL_CUSTODY");next.setProperty("available_after",Long.toString(after));
        StorageMaintenanceFileR82.write(intent,next);return new Result("RETIRED_WITH_EXTERNAL_CUSTODY",bytes,before,after);
    }
}
