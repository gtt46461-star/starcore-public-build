package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.*;
import java.io.*;
import java.util.*;

/** Actual restore consumers. Runs solely in the private clone process. Missing
 * schema/codecs/locators throw WAIT; no invented model or settlement is added. */
final class StorageRestoreBusinessProbeR31 {
    private static String q(String value){return "\""+value.replace("\"","\"\"")+"\"";}
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new IllegalStateException("RESTORE_COUNT_UNKNOWN");return c.getLong(0);}}
    private static boolean table(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){return c.moveToFirst();}}
    private static String rowsHash(SQLiteDatabase db,String sql){StringBuilder b=new StringBuilder();try(Cursor c=db.rawQuery(sql,null)){while(c.moveToNext()){for(int i=0;i<c.getColumnCount();i++){String value=c.isNull(i)?"<NULL>":c.getString(i);b.append(value.length()).append(':').append(value);}b.append('\n');if(b.length()>65536){b=new StringBuilder(StatsR27.sha256(b.toString()));}}}return StatsR27.sha256(b.toString());}
    private static String pendingIdentity(SQLiteDatabase db){return rowsHash(db,"SELECT target_issue,draw_issue,draw_hash,settlement_hash,attempts,completed_at,terminal_code FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" ORDER BY target_issue");}
    static Properties inventory(SQLiteDatabase db,String nativeFull,String assetsHash)throws Exception {
        if(nativeFull==null||!nativeFull.matches("OK rows=[0-9]+ tables=[0-9]+ auto_vacuum=2 user_version=[0-9]+ application_id=[0-9]+"))throw new IOException("RESTORE_NATIVE_FULL_COMPARE_REQUIRED");
        Properties out=new Properties();String schema=rowsHash(db,"SELECT type,name,tbl_name,sql FROM sqlite_master WHERE name NOT LIKE 'sqlite_autoindex_%' ORDER BY type,name");out.setProperty("restore_schema_sha256",schema);
        StringBuilder counts=new StringBuilder();int tables=0;try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null)){while(c.moveToNext()){if(++tables>4096)throw new IOException("WAIT_RESTORE_TABLE_BOUND");String name=c.getString(0);counts.append(name.length()).append(':').append(name).append(':').append(count(db,"SELECT count(*) FROM "+q(name))).append('\n');}}
        out.setProperty("restore_business_row_counts","PASS:FULL_NATIVE_TYPED_SOURCE_COMPARE:tables="+tables+":counts_sha256="+StatsR27.sha256(counts.toString()));
        out.setProperty("restore_rowid_dependencies","PASS:FULL_NATIVE_INTRINSIC_ROWID_AND_TYPED_VALUES:"+StatsR27.sha256(nativeFull));
        out.setProperty("restore_native_full_compare_sha256",StatsR27.sha256(nativeFull));out.setProperty("restore_asset_manifest_sha256",assetsHash);
        out.setProperty("restore_pending_before_sha256",pendingIdentity(db));out.setProperty("restore_pending_before_count",Long.toString(count(db,"SELECT count(*) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')")));out.setProperty("restore_running_before_count",Long.toString(count(db,"SELECT count(*) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE status='RUNNING'")));return out;
    }
    static void verify(DbStore store,Properties out)throws Exception {
        SQLiteDatabase db=store.readable();String pendingBefore=pendingIdentity(db);if(!pendingBefore.equals(out.getProperty("restore_pending_before_sha256")))throw new IOException("RESTORE_STARTUP_CHANGED_SETTLEMENT_IDENTITY");
        int recovered=SettlementTaskStoreR36.recoverOrphanedRunningLeasesOnProcessStart(store);
        if(recovered!=Long.parseLong(out.getProperty("restore_running_before_count"))||count(db,"SELECT count(*) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE status='RUNNING'")!=0||!pendingBefore.equals(pendingIdentity(db))||count(db,"SELECT count(*) FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')")!=Long.parseLong(out.getProperty("restore_pending_before_count")))throw new IOException("RESTORE_ACTUAL_PENDING_RECOVERY_POSTCONDITION");
        out.setProperty("restore_pending_settlement_evidence","PASS:ORIGINAL_PROCESS_START_RECOVERY:recovered="+recovered+":immutable_sha256="+pendingBefore);
        try(Cursor pending=db.rawQuery("SELECT target_issue,draw_hash FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE status IN ('PENDING','RUNNING','RETRY_WAIT')",null)){
            int inspected=0;while(pending.moveToNext()){bound(++inspected);Models.Draw draw=store.getDrawByIssue(pending.getString(0));if(draw==null||!draw.isValid()||(!pending.getString(1).equals(DrawHashR36.sha256V2(draw))&&!pending.getString(1).equals(DrawHashR36.sha256(draw))))throw new IOException("WAIT_RESTORE_PENDING_CANONICAL_DRAW_EVIDENCE");}
        }
        int models=verifyModels(db,store.contextForR26().getFilesDir())+verifyReplayWorkspaces(store),closure=verifyReferences(db,store.contextForR26().getFilesDir());out.setProperty("restore_model_load","PASS:ACTUAL_PERSISTED_MODEL_DECODERS:models="+models);out.setProperty("restore_reference_closure","PASS:ACTUAL_HASHED_CHECKPOINT_GRAPH_AND_LOCATOR_READERS:objects="+closure);
        out.setProperty("restore_blob_and_locators","PASS:FULL_NATIVE_TYPED_BLOB_COMPARE_AND_REAL_LOCATOR_READERS:"+out.getProperty("restore_asset_manifest_sha256"));
        out.setProperty("restore_keep_scope","ALL_PERSISTED_KEEP_STATES_KEYSET64_HASH_BYTES_AND_ORIGINAL_CODEC; NO_REGISTRY_IS_LEGACY_NOT_APPLICABLE");
        try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){if(!c.moveToFirst()||!"ok".equals(c.getString(0))||c.moveToNext())throw new IOException("RESTORE_POST_START_INTEGRITY");}
        try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){if(c.moveToFirst())throw new IOException("RESTORE_POST_START_REFERENCE_FOREIGN_KEY");}
        out.setProperty("restore_startup_recovery","PASS:ORIGINAL_DBSTORE_CRITICAL_STARTUP_AND_EVENT_RECONCILE_AND_SETTLEMENT_RECOVERY");
        out.setProperty("restore_scope","ACTUAL_SEPARATE_ANDROID_PROCESS_CLONE_RESTORE; NO_NETWORK_NO_NEW_PREDICTIONS_NO_TRAINING_NO_SETTLEMENT_EXECUTION; ZERO_MODEL_COUNT_MEANS_EMPTY_PERSISTED_INVENTORY");
    }
    static int verifyModels(SQLiteDatabase db,File filesRoot)throws Exception {
        int models=0;
        if(table(db,"r26_live_model"))try(Cursor c=db.rawQuery("SELECT canonical_text,state,state_hash FROM r26_live_model",null)){while(c.moveToNext()){bound(++models);LiveStateR26.decode(c.getString(0),new CheckpointArtifactsR26(new File(filesRoot,"r26-checkpoints")).resolve(c.getBlob(1)),c.getString(2));}}
        if(table(db,"r26_legacy_runtime"))try(Cursor c=db.rawQuery("SELECT state,state_hash FROM r26_legacy_runtime",null)){while(c.moveToNext()){bound(++models);new CheckpointArtifactsR26(new File(filesRoot,"r26-checkpoints")).restoreLegacy(c.getBlob(0),c.getString(1));}}
        if(table(db,"r26_prepared_policy"))try(Cursor c=db.rawQuery("SELECT state,state_hash FROM r26_prepared_policy",null)){while(c.moveToNext()){bound(++models);new CheckpointArtifactsR26(new File(filesRoot,"r26-checkpoints")).restoreLegacy(c.getBlob(0),c.getString(1));}}
        if(table(db,"r26_expert_experiment"))try(Cursor c=db.rawQuery("SELECT state,state_hash,expert_id,cursor FROM r26_expert_experiment",null)){while(c.moveToNext()){bound(++models);byte[] bytes=new CheckpointArtifactsR26(new File(filesRoot,"r26-checkpoints")).resolve(c.getBlob(0));if(!LiveStateR26.hash(bytes).equals(c.getString(1)))throw new IOException("RESTORE_EXPERT_STATE_HASH");ExpertExperimentR26.Pair pair=ExpertExperimentR26.Pair.decode(bytes);if(!pair.spec.id.equals(c.getString(2))||pair.cursor!=c.getInt(3))throw new IOException("RESTORE_EXPERT_OWNER_CURSOR");}}
        if(table(db,"r29_model_checkpoint"))try(Cursor c=db.rawQuery("SELECT owner,artifact,state_hash,dataset_hash FROM r29_model_checkpoint",null)){while(c.moveToNext()){bound(++models);String owner=c.getString(0);if(AtomNetSessionR30.OWNER.equals(owner)||AtomNetSessionR30.LEGACY_OWNER.equals(owner)){AtomNetSessionR30 state=AtomNetSessionR30.decode(db,c.getBlob(1),c.getString(2));if(!state.datasetHash.equals(c.getString(3)))throw new IOException("RESTORE_ATOM_DATASET_IDENTITY");}else if("ATOMNET_FTRL".equals(owner))ModelArtifactR29.decode(db,c.getBlob(1),c.getString(2));else throw new IOException("WAIT_RESTORE_UNSUPPORTED_CHECKPOINT_OWNER");}}
        for(String[] legacy:new String[][]{{"r29_legacy_model_checkpoint","artifact_hash"},{"r27_atomnet_state","artifact_hash"}})if(table(db,legacy[0]))try(Cursor c=db.rawQuery("SELECT artifact,"+legacy[1]+" FROM "+legacy[0],null)){while(c.moveToNext()){bound(++models);verifyLegacyModel(db,c.getBlob(0),c.getString(1));}}
        if(table(db,"r27_policy"))try(Cursor c=db.rawQuery("SELECT policy_hash,spec_json FROM r27_policy",null)){while(c.moveToNext()){bound(++models);if(!c.getString(0).equals(PolicySpecR27.parse(c.getString(1)).policyHash()))throw new IOException("RESTORE_POLICY_HASH");}}
        if(table(db,"r27_evolution_cursor"))try(Cursor c=db.rawQuery("SELECT snapshot,dataset_hash FROM r27_evolution_cursor",null)){while(c.moveToNext()){bound(++models);String dataset=c.getString(1);ResearchInputPinR30R1.Pin pin=ResearchInputPinR30R1.load(db,dataset);ReplayDatasetR27 data=pin==null?StoreR27.loadDataset(db,10000):pin.data;if(!dataset.equals(data.datasetHash))throw new IOException("WAIT_RESTORE_ORIGINAL_EVOLUTION_DATASET");try(FeatureCacheR27 cache=new FeatureCacheR27(data);EvolutionLoopR27 restored=EvolutionLoopR27.restore(EvolutionPublicationStoreR31.resolveSnapshot(db,c.getString(0)),new EvolutionLoopR27.Config(),cache,null,System::currentTimeMillis)){restored.snapshot();}}}
        File bundles=new File(filesRoot,"model-bundles-r929");if(bundles.exists()){File[] dirs=bundles.listFiles();if(dirs==null||dirs.length>4096)throw new IOException("WAIT_RESTORE_BUNDLE_BOUND");for(File dir:dirs){if(!dir.isDirectory()||!dir.getName().matches("[0-9a-f]{64}"))throw new IOException("WAIT_RESTORE_UNKNOWN_BUNDLE_ENTRY");bound(++models);ModelBundleManagerR929.BundleManifest manifest=ModelBundleManagerR929.parse(readText(new File(dir,"manifest.json"),65536));ModelBundleManagerR929.verifyDisk(dir,manifest,readBytes(new File(dir,"signature.bin"),8192),ModelBundleRuntimeR932.trustedKey());Map<String,byte[]> bytes=new HashMap<>();for(ModelBundleManagerR929.ArtifactEntry e:manifest.artifacts)bytes.put(e.path,readBytes(new File(dir,e.path),e.size));new ModelBundleRuntimeR932.Snapshot(manifest,bytes);}}
        return models;
    }
    private static void verifyLegacyModel(SQLiteDatabase db,byte[] stored,String hash)throws Exception {
        byte[] bytes=CheckpointArtifactsR26.resolveSerializedR31(db,stored);if(bytes==null||bytes.length<4||!StatsR27.sha256(bytes).equals(hash))throw new IOException("RESTORE_LEGACY_MODEL_HASH");
        int magic;try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){magic=in.readInt();}
        if(magic==0x30A70001||magic==0x30A70002||magic==0x30A70003){AtomNetSessionR30.decode(db,stored,hash);return;}
        if(magic==0x29A70001||magic==0x29A70002){ModelArtifactR29.decode(db,stored,hash);return;}
        if(magic==0x27A70001){try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){in.readInt();int input=in.readInt();if(input<1||input>4096||in.readLong()<0||(bytes.length-16)%8!=0)throw new IOException("RESTORE_LEGACY_NETWORK_HEADER");while(in.available()>0)if(!Double.isFinite(in.readDouble()))throw new IOException("RESTORE_LEGACY_NETWORK_NONFINITE");}if(!Arrays.equals(bytes,AtomNetR27.decode(bytes).encode()))throw new IOException("RESTORE_LEGACY_NETWORK_CANONICAL_BYTES");return;}
        throw new IOException("WAIT_RESTORE_UNSUPPORTED_LEGACY_MODEL_CODEC");
    }
    private static int verifyReplayWorkspaces(DbStore store)throws Exception {
        SQLiteDatabase db=store.readable();int models=0;if(!table(db,"r26_member_replay"))return 0;
        try(Cursor c=db.rawQuery("SELECT m.member_text,m.calibrated,m.cursor,m.campaign_id,c.snapshot_hash,c.source_through FROM r26_member_replay m LEFT JOIN r26_legacy_campaign c ON c.campaign_id=m.campaign_id WHERE m.state IS NOT NULL",null)){
            while(c.moveToNext()){bound(++models);if(c.isNull(4))throw new IOException("RESTORE_MEMBER_SOURCE_CLOSURE");ModelGenomeSU2 genome=ModelGenomeSU2.parse(c.getString(0));if(genome.members.size()!=1)throw new IOException("RESTORE_MEMBER_MODEL_IDENTITY");ReplayWorkspaceR26 workspace=new ReplayWorkspaceR26(store,c.getString(3),c.getString(4),c.getLong(5));if(c.getInt(1)==0)workspace.trained(genome.members.get(0),c.getInt(2));else workspace.calibrator(genome.members.get(0),c.getInt(2));}
        }return models;
    }
    static int verifyReferences(SQLiteDatabase db,File filesRoot)throws Exception {
        Set<String> roots=new TreeSet<>();
        try(Cursor tables=db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' ORDER BY name",null)){while(tables.moveToNext()){String table=tables.getString(0);try(Cursor columns=db.rawQuery("PRAGMA table_info("+q(table)+")",null)){while(columns.moveToNext())if(columns.getString(2)==null||columns.getString(2).trim().isEmpty()||columns.getString(2).toUpperCase(Locale.ROOT).contains("BLOB")){String column=q(columns.getString(1));try(Cursor values=db.rawQuery("SELECT "+column+" FROM "+q(table)+" WHERE typeof("+column+")='blob' AND length("+column+")=72 AND hex(substr("+column+",1,8))='523236434153313A'",null)){while(values.moveToNext()){String hash=CheckpointArtifactsR26.referenceHash(values.getBlob(0));if(!hash.matches("[0-9a-f]{64}")||roots.size()>=8192)throw new IOException("WAIT_RESTORE_REFERENCE_BOUND");roots.add(hash);}}}}}}
        int objects=0;if(!roots.isEmpty()){CheckpointArtifactsR26 files=new CheckpointArtifactsR26(new File(filesRoot,"r26-checkpoints"));Set<String> closure=files.reachableHashes(roots);for(String hash:closure){files.readHash(hash);objects++;}}
        if(table(db,"su1_blob"))try(Cursor c=db.rawQuery("SELECT blob_hash FROM su1_blob",null)){while(c.moveToNext()){bound(++objects);CampaignStateSU1.localBlob(db,c.getString(0));}}
        if(table(db,"r31_history_import_run"))try(Cursor c=db.rawQuery("SELECT source_path,source_hash,source_bytes FROM r31_history_import_run",null)){while(c.moveToNext()){bound(++objects);File file=StorageRestoreProofR31.child(filesRoot.getParentFile(),c.getString(0));if(file.length()!=c.getLong(2)||!c.getString(1).equals(StorageMaintenanceFileR82.sha256(file)))throw new IOException("RESTORE_HISTORY_LOCATOR_CHANGED");}}
        return Math.addExact(objects,verifyKeepReferences(db,filesRoot));
    }
    static int verifyKeepReferences(SQLiteDatabase db,File filesRoot)throws Exception {
        boolean originals=table(db,"r31_keep_original"),checkpoints=table(db,"r31_keep_checkpoint");
        if(!originals&&!checkpoints)return 0; // Actual old schema has no Keep CAS registry.
        if(!originals||!checkpoints||!KeepMigrationStoreR31.ready(db))throw new IOException("WAIT_RESTORE_KEEP_SCHEMA_PARTIAL");
        // ready() only checks table presence. Unknown/partial definitions cannot
        // turn snapshotArtifactPage's empty list into a successful restore.
        for(String ddl:KeepMigrationStoreR31.DDL){java.util.regex.Matcher name=java.util.regex.Pattern.compile("^CREATE (?:TABLE|INDEX) IF NOT EXISTS ([A-Za-z0-9_]+)").matcher(ddl);if(!name.find())throw new IOException("WAIT_RESTORE_KEEP_SCHEMA_UNRECOGNIZED");try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{name.group(1)})){if(!c.moveToFirst()||!canonicalDdl(ddl).equals(canonicalDdl(c.getString(0))))throw new IOException("WAIT_RESTORE_KEEP_SCHEMA_UNKNOWN");}}
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_keep_original WHERE row_hash IS NULL OR length(row_hash)<>64 OR row_hash GLOB '*[^0-9a-f]*' OR bytes IS NULL OR bytes<1 OR bytes>? OR row_id IS NULL OR archive_table IS NULL LIMIT 1",new String[]{Integer.toString(KeepMigrationStoreR31.MAX_ROW_BYTES)})){if(c.moveToFirst())throw new IOException("WAIT_RESTORE_KEEP_REGISTRY_IDENTITY");}
        File root=StorageRestoreProofR31.child(filesRoot,"r31-keep");String after="";int artifacts=0,rows=0;long bytes=0;
        for(;;){List<KeepMigrationStoreR31.ArtifactRef> page=KeepMigrationStoreR31.snapshotArtifactPage(db,after,64);if(page.isEmpty())break;if(page.size()>64)throw new IOException("WAIT_RESTORE_KEEP_PAGE_BOUND");
            for(KeepMigrationStoreR31.ArtifactRef ref:page){if(ref.hash.compareTo(after)<=0||++artifacts>16384||(bytes=Math.addExact(bytes,ref.bytes))>(1L<<30))throw new IOException("WAIT_RESTORE_KEEP_ARTIFACT_BOUND");File original=StorageRestoreProofR31.child(root,ref.hash+".row");if(!original.isFile()||original.length()!=ref.bytes)throw new IOException("WAIT_RESTORE_KEEP_ARTIFACT_MISSING_OR_BYTES");if(!ref.hash.equals(StorageMaintenanceFileR82.sha256(original)))throw new IOException("RESTORE_KEEP_ARTIFACT_HASH");
                try(Cursor c=db.rawQuery("SELECT archive_table,row_id FROM r31_keep_original WHERE row_hash=? ORDER BY archive_table,row_id LIMIT 16385",new String[]{ref.hash})){while(c.moveToNext()){if(++rows>16384)throw new IOException("WAIT_RESTORE_KEEP_REFERENCE_BOUND");KeepMigrationStoreR31.readArtifact(root,c.getString(0),c.getLong(1),ref.hash);}}
                after=ref.hash;
            }
        }
        if(rows!=count(db,"SELECT count(*) FROM r31_keep_original"))throw new IOException("WAIT_RESTORE_KEEP_REGISTRY_NOT_FULLY_READ");return artifacts;
    }
    private static String canonicalDdl(String sql){return sql==null?"":sql.replace("IF NOT EXISTS ","").replaceAll("\\s+","").replaceAll(";+$","").toLowerCase(Locale.ROOT);}
    private static void bound(int count)throws IOException{if(count>4096)throw new IOException("WAIT_RESTORE_CONSUMER_BOUND");}
    private static byte[] readBytes(File file,long maximum)throws Exception{try(InputStream in=new FileInputStream(file)){return ModelBundleManagerR929.readBounded(in,maximum);}}
    private static String readText(File file,long maximum)throws Exception{return new String(readBytes(file,maximum),"UTF-8");}
}
