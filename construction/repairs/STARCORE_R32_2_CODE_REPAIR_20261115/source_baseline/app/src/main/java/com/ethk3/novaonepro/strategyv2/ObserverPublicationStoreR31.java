package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;

/** Owner-prepared exploration refresh. Existing canonical rows publish in one transaction; no new schema. */
public final class ObserverPublicationStoreR31 {
    private ObserverPublicationStoreR31() {}
    public static final String ABI="R31_OBSERVER_PUBLICATION_V1";
    public static final int MAX_ROSTER_ROWS=8192,MAX_FROZEN_BYTES=4<<20;
    public static final long MAX_CANONICAL_ROW_BYTES=32L<<20;
    private static final String ELIGIBLE="e.global_index>0 AND e.state NOT IN ('"+SequentialQualificationR27.QUALIFIED+"','"+SequentialQualificationR27.NOT_BETTER+"')";
    private static final String QUEUE="SELECT s.policy_hash,coalesce(nullif(s.base_policy,''),s.policy_hash),e.global_index FROM r27_live_shadow s JOIN r27_eprocess e ON e.policy_hash=s.policy_hash WHERE "+ELIGIBLE+" AND s.status IN ('ACTIVE','ACTIVE_REPAIR1','OBSERVE','OBSERVE_RETIRED','OBSERVE_REPAIR1','OBSERVE_REPAIR1_RETIRED') ORDER BY e.global_index,s.policy_hash LIMIT "+(MAX_ROSTER_ROWS+1);

    public static class Dependency extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        // Compatibility with existing coordinator adapters which classify a wrapped cause by message.
        Dependency(String message){super("DEPENDENCY("+message+")");}
        public String failureCategory(){return "DEPENDENCY";}
        public String operation(){return ABI;}
        public String phase(){return "PREPARE_OR_SOURCE_CAS";}
        public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    public static final class RosterChanged extends Dependency {RosterChanged(){super("R31_OBSERVER_SOURCE_CHANGED_REPREPARE");}}
    public static final class BoundaryChanged extends Dependency {BoundaryChanged(){super("R31_OBSERVER_COMPLETED_BOUNDARY_CHANGED_RELOAD");}}
    private static final class StaleOwner extends Dependency {
        StaleOwner(){super("R31_OBSERVER_STALE_OWNER");}
        @Override public String failureCategory(){return "STALE_OWNER";}
    }
    private static final class Ranked {
        final String id,base,json;final double tau,u;
        Ranked(EvolutionLoopR27.Member member){base=member.hash();id=RuntimeR27.observerId(base);json=member.spec.fullJson();tau=member.tau;u=member.u;}
    }
    /** Owns immutable strings/doubles only. No mutable model/cache is shared with a subsequent generation. */
    public static final class FrozenInput implements AutoCloseable {
        public final int generation,completedGeneration,completedStep;
        public final String dataset,rankHash;public final long rng;
        private final List<Ranked> ranked;private final MemoryBudgetR31.Lease memory;
        private boolean closed;
        private FrozenInput(EvolutionLoopR27 loop,RuntimeCoordinatorR26.Claim claim,BooleanSupplier cancel){
            if(loop.population.size()>MAX_ROSTER_ROWS)throw new Dependency("R31_OBSERVER_POPULATION_CAPACITY_DEBT");
            memory=admit("observer-frozen-input:"+claim.owner,claim.fence,16384L+2048L*loop.population.size(),cancel);
            try{
                generation=loop.generation-1;completedGeneration=loop.generation;completedStep=loop.step;
                dataset=loop.context().data.datasetHash;rng=loop.rng.state;
                List<Ranked> copy=new ArrayList<>();long retained=16384;
                for(EvolutionLoopR27.Member member:loop.population){check(cancel);Ranked row=new Ranked(member);retained+=4*(utf8(row.json)+utf8(row.id)+utf8(row.base)+128);if(retained>MAX_FROZEN_BYTES)throw new Dependency("R31_OBSERVER_FROZEN_INPUT_BYTES_DEBT");grow(memory,retained);copy.add(row);}
                // The original comparator, including its tie behavior, remains unchanged.
                copy.sort((a,b)->a.u!=b.u?Double.compare(b.u,a.u):a.base.compareTo(b.base));
                List<Object> identity=new ArrayList<>();for(Ranked row:copy)identity.add(java.util.Arrays.asList(row.base,Long.toHexString(Double.doubleToRawLongBits(row.tau)),Long.toHexString(Double.doubleToRawLongBits(row.u)),row.json));
                rankHash=StatsR27.sha256(JsonR27.write(identity));ranked=Collections.unmodifiableList(copy);
            }catch(RuntimeException|Error failure){memory.close();throw failure;}
        }
        @Override public void close(){if(!closed){closed=true;memory.close();}}
    }
    public static FrozenInput freeze(EvolutionLoopR27 loop,RuntimeCoordinatorR26.Claim claim,BooleanSupplier cancel){
        Objects.requireNonNull(loop);Objects.requireNonNull(claim);Objects.requireNonNull(cancel);check(cancel);
        return new FrozenInput(loop,claim,cancel);
    }

    public static final class Prepared implements AutoCloseable {
        public final ResearchWriteQueueR27.Batch batch;
        public final String inputSnapshot;
        public final Map<String,Object> envelope;
        private final MemoryBudgetR31.Lease memory;
        private boolean closed;
        private Prepared(SQLiteDatabase read,FrozenInput input,RuntimeCoordinatorR26.Claim claim,BooleanSupplier cancel){
            memory=admit("observer-prepared:"+claim.owner,claim.fence,32768,cancel);
            try{
                if(read.inTransaction())throw new IllegalStateException("R31_OBSERVER_PREPARE_OUTSIDE_WRITER");
                if(input.closed)throw new CancellationException("R31_OBSERVER_FROZEN_INPUT_CLOSED");
                check(cancel);final long schema=schemaVersion(read);
                Map<String,Object> protection=requireSchema(read);
                if(schemaVersion(read)!=schema)throw new RosterChanged();
                final long sourceRevision=SchemaR27.liveSourceRevision(read);
                if(sourceRevision<0)throw new Dependency("SCHEMA_PENDING:OBSERVER_SOURCE_REVISION");
                if(ResearchInputPinR30R1.invalid(read,input.dataset))throw new Dependency("R31_OBSERVER_CORRECTED_DATASET_REQUIRED");
                final List<String[]> cursor=rows(read,"SELECT generation,step,dataset_hash,rng_state,snapshot FROM r27_evolution_cursor WHERE loop=?",new String[]{RuntimeR27.LOOP},memory,cancel);
                if(cursor.size()!=1)throw new Dependency("R31_OBSERVER_COMPLETED_CURSOR_REQUIRED");
                String[] source=cursor.get(0);
                if(!String.valueOf(input.completedGeneration).equals(source[0])||!String.valueOf(input.completedStep).equals(source[1])||!input.dataset.equals(source[2])||!String.valueOf(input.rng).equals(source[3]))throw new BoundaryChanged();
                if(source[4]==null||!source[4].startsWith(EvolutionPublicationStoreR31.SNAPSHOT_PREFIX)||utf8(source[4])>4096)throw new Dependency("R31_OBSERVER_COMPLETE_CHECKPOINT_REFERENCE_REQUIRED");
                final List<String[]> queue=rows(read,QUEUE,null,memory,cancel);
                Map<String,String> confirmation=new LinkedHashMap<>();for(String[] row:queue)confirmation.put(row[0],row[1]);
                final int reservedPairSlots=ImprovementStoreR31.reservedObserverSlots(read);
                int remaining=Math.max(0,RuntimeR27.OBSERVE_CAP-confirmation.size()-reservedPairSlots);
                java.util.Set<String> protectedBases=new java.util.HashSet<>(confirmation.values());
                Map<String,Ranked> uniqueKeep=new LinkedHashMap<>();for(Ranked ranked:input.ranked){if(uniqueKeep.size()>=remaining)break;if(Double.isFinite(ranked.tau)&&!confirmation.containsKey(ranked.id)&&!protectedBases.contains(ranked.base))uniqueKeep.put(ranked.id,ranked);}
                List<Ranked> keep=new ArrayList<>(uniqueKeep.values());
                List<String> keepIds=new ArrayList<>();for(Ranked ranked:keep)keepIds.add(ranked.id);
                String in=keepIds.isEmpty()?"'__R31_NO_OBSERVER_IDS__'":slots(keepIds.size());
                final String shadowSql="SELECT policy_hash,status,512+coalesce(length(CAST(spec_json AS BLOB)),0)+coalesce(length(bias),0)+coalesce(length(reason),0)+coalesce(length(base_policy),0)+coalesce(length(semantic_abi),0) FROM r27_live_shadow s WHERE status IN ('OBSERVE','OBSERVE_REPAIR1') OR (status IN ('OBSERVE_RETIRED','OBSERVE_REPAIR1_RETIRED') AND EXISTS(SELECT 1 FROM r27_eprocess e WHERE e.policy_hash=s.policy_hash AND "+ELIGIBLE+")) OR policy_hash IN ("+in+") OR status='OBSERVE_PAIR_R31' ORDER BY policy_hash LIMIT "+(MAX_ROSTER_ROWS+1);
                final String[] shadowArgs=keepIds.isEmpty()?null:keepIds.toArray(new String[0]);
                final List<String[]> shadows=rows(read,shadowSql,shadowArgs,memory,cancel);
                Map<String,String> status=new LinkedHashMap<>();int retire=0,protect=0;long canonicalBytes=0;
                for(String[] row:shadows){status.put(row[0],row[1]);boolean active="OBSERVE".equals(row[1])||RuntimeR27.OBSERVE.equals(row[1]);boolean retired="OBSERVE_RETIRED".equals(row[1])||RuntimeR27.OBSERVE_RETIRED.equals(row[1]);if(active&&!keepIds.contains(row[0])&&!confirmation.containsKey(row[0]))retire++;if(retired&&confirmation.containsKey(row[0]))protect++;canonicalBytes=Math.addExact(canonicalBytes,Long.parseLong(row[2]));}
                // Original fullJson/pack and rank work occur here, before entering the sole writer.
                final long preparedAt=System.currentTimeMillis();final String reason="EXPLORATION_H_SELECT_G"+input.generation;
                ResearchWriteQueueR27.Batch prepared=new ResearchWriteQueueR27.Batch();
                if(retire>0){List<Object> args=new ArrayList<>();args.add("LEFT_EXPLORATION_G"+input.generation);args.add(preparedAt);args.addAll(keepIds);
                    prepared.add("UPDATE r27_live_shadow SET status=CASE status WHEN 'OBSERVE' THEN 'OBSERVE_RETIRED' ELSE 'OBSERVE_REPAIR1_RETIRED' END,reason=?,updated_at=? WHERE status IN ('OBSERVE','OBSERVE_REPAIR1') AND policy_hash NOT IN ("+in+") AND NOT EXISTS(SELECT 1 FROM r27_eprocess e WHERE e.policy_hash=r27_live_shadow.policy_hash AND "+ELIGIBLE+")",args.toArray());}
                if(protect>0)prepared.add("UPDATE r27_live_shadow SET status=CASE status WHEN 'OBSERVE_RETIRED' THEN 'OBSERVE' ELSE 'OBSERVE_REPAIR1' END,reason=?,updated_at=? WHERE status IN ('OBSERVE_RETIRED','OBSERVE_REPAIR1_RETIRED') AND EXISTS(SELECT 1 FROM r27_eprocess e WHERE e.policy_hash=r27_live_shadow.policy_hash AND "+ELIGIBLE+")","CONFIRMATION_QUEUE_PROTECTED",preparedAt);
                int insert=0,resume=0;byte[] bias=StoreR27.pack(new double[10]);
                for(Ranked candidate:keep){String prior=status.get(candidate.id);
                    if(prior==null){prepared.add("INSERT OR IGNORE INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,?,?,?,0,?,?,?,?,-1,?,'')",candidate.id,candidate.json,candidate.tau,bias,RuntimeR27.OBSERVE,reason,preparedAt,preparedAt,candidate.base);insert++;canonicalBytes=Math.addExact(canonicalBytes,1024+utf8(candidate.json));}
                    else if(RuntimeR27.OBSERVE_RETIRED.equals(prior)){prepared.add("UPDATE r27_live_shadow SET status=?,temperature=?,reason=?,updated_at=? WHERE policy_hash=? AND status=?",RuntimeR27.OBSERVE,candidate.tau,reason,preparedAt,candidate.id,RuntimeR27.OBSERVE_RETIRED);resume++;}
                }
                if(canonicalBytes>MAX_CANONICAL_ROW_BYTES)throw new Dependency("R31_OBSERVER_CANONICAL_ROW_BYTES_DEBT:"+canonicalBytes);
                List<Object> sourceIdentity=new ArrayList<>();sourceIdentity.add(ABI);sourceIdentity.add(input.rankHash);sourceIdentity.add(sourceRevision);appendRows(sourceIdentity,cursor);appendRows(sourceIdentity,queue);appendRows(sourceIdentity,shadows);
                inputSnapshot=ABI+"|"+StatsR27.sha256(JsonR27.write(sourceIdentity));
                // A satisfied target still validates under the sole writer before acknowledging it.
                // Its only possible write is the existing protocol receipt, never an experiment row.
                {
                    final String owner=claim.owner,task=claim.task;final long fence=claim.fence;
                    prepared.guardBefore(db->{
                        if(closed||input.closed)throw new CancellationException("R31_OBSERVER_PREPARED_CLOSED");
                        RuntimeCoordinatorR26.Claim current=RuntimeCoordinatorR26.currentClaimR30();
                        if(current==null||!owner.equals(current.owner)||!task.equals(current.task)||fence!=current.fence)throw new StaleOwner();
                        RuntimeCoordinatorR26.requireFence(db,current);
                        if(!sameRows(db,"SELECT generation,step,dataset_hash,rng_state,snapshot FROM r27_evolution_cursor WHERE loop=?",new String[]{RuntimeR27.LOOP},cursor))throw new BoundaryChanged();
                        if(schemaVersion(db)!=schema||SchemaR27.liveSourceRevision(db)!=sourceRevision||!sameRows(db,QUEUE,null,queue)||!sameRows(db,shadowSql,shadowArgs,shadows))throw new RosterChanged();
                        return 0;
                    });
                }
                long guardBytes=128+utf8(source[4]);for(String id:keepIds)guardBytes+=utf8(id);
                QualificationInboxR29.requireBudgetR31(prepared,5,guardBytes);
                long bind=guardBytes+8192;for(ResearchWriteQueueR27.Op op:prepared.ops)for(Object a:op.args)bind+=a instanceof String?utf8((String)a):a instanceof byte[]?((byte[])a).length:8;
                grow(memory,32768L+4*bind+256L*(queue.size()+shadows.size()));
                Map<String,Object> report=new LinkedHashMap<>();report.put("abi",ABI);report.put("rankedMembers",input.ranked.size());report.put("confirmationRows",queue.size());report.put("frozenRelevantShadowRows",shadows.size());report.put("selectedExploration",keep.size());report.put("insertRows",insert);report.put("resumeRows",resume);report.put("retireRows",retire);report.put("restoreProtectedRows",protect);report.put("canonicalRowsUpperBound",insert+resume+retire+protect);report.put("canonicalEncodedRowBytesUpperBound",canonicalBytes);report.put("rowBytesMeaning","ENCODED_ROWS_PLUS_CONSERVATIVE_FIXED_HEADER_ESTIMATE_NOT_PHYSICAL_WAL_GUARANTEE");report.put("sqlOperations",prepared.ops.size());report.put("guardSqlUpperBound",5);report.put("pairedObserverReservedWithinOriginal24",reservedPairSlots);report.put("protocolSqlReserve",32);report.put("bindBytesIncludingGuardAndProtocolReserve",bind);report.put("canonicalTriggerAmplification",0);report.put("implicitReplaceDeletes",0);report.put("ddl","NO_NEW_SCHEMA");report.put("androidHoldP99","NOT_MEASURED");report.put("protectedAlphaExposureWrites",0);report.put("sourceContract","COMPLETED_EVOLUTION_CURSOR_EXACT_QUEUE_STATUS_AND_LIVE_SOURCE_REVISION_CAS");
                report.put("retentionSourceProtection",protection);envelope=Collections.unmodifiableMap(report);batch=prepared;
            }catch(RuntimeException|Error failure){memory.close();throw failure;}
        }
        @Override public void close(){if(!closed){closed=true;memory.close();}}
    }
    public static Prepared prepare(SQLiteDatabase read,FrozenInput input,RuntimeCoordinatorR26.Claim claim,BooleanSupplier cancel){Objects.requireNonNull(read);Objects.requireNonNull(input);Objects.requireNonNull(claim);Objects.requireNonNull(cancel);check(cancel);return new Prepared(read,input,claim,cancel);}
    private static List<String[]> rows(SQLiteDatabase db,String sql,String[] args,MemoryBudgetR31.Lease lease,BooleanSupplier cancel){
        List<String[]> out=new ArrayList<>();long bytes=memoryBytes(lease)+8192;
        try(Cursor cursor=db.rawQuery(sql,args)){int columns=cursor.getColumnCount();while(cursor.moveToNext()){check(cancel);if(out.size()>=MAX_ROSTER_ROWS)throw new Dependency("R31_OBSERVER_ROSTER_CAPACITY_DEBT");String[] row=new String[columns];for(int c=0;c<columns;c++){row[c]=cursor.getString(c);bytes+=4*utf8(row[c])+64;}if(bytes>MAX_FROZEN_BYTES)throw new Dependency("R31_OBSERVER_ROSTER_BYTES_DEBT");grow(lease,bytes);out.add(row);}}
        return out;
    }
    /** Validation only: no maps, sorting, hashes, formula serialization or numerical work in the writer. */
    private static boolean sameRows(SQLiteDatabase db,String sql,String[] args,List<String[]> expected){
        try(Cursor cursor=db.rawQuery(sql,args)){int index=0;while(cursor.moveToNext()){if(index>=expected.size())return false;String[] row=expected.get(index++);for(int c=0;c<row.length;c++)if(!Objects.equals(row[c],cursor.getString(c)))return false;}return index==expected.size();}
    }
    private static Map<String,Object> requireSchema(SQLiteDatabase read){
        DatabaseCoordinator.requireSchemaObjectsR31(read,ABI,new String[][]{{"r27_live_shadow","policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi"},{"r27_eprocess","policy_hash,global_index,state"},{"r27_evolution_cursor","loop,generation,step,dataset_hash,rng_state,snapshot"}},new String[0][]);
        // Reuse the archive owner's exact builder. Prefix/name matches are never an authentication.
        String[][] definitions=ResearchRetentionStoreR30.sourceProtectionTriggersR31(read,"r27_live_shadow");
        boolean installed=ResearchRetentionStoreR30.requireGuardDefinitionsR31(read,definitions,false);
        java.util.Set<String> known=new java.util.HashSet<>();List<String> payloadColumns=new ArrayList<>();
        for(String[] definition:definitions){known.add(definition[0]);
            String sql=definition[1];
            if(sql.contains(" BEFORE DELETE ON \"r27_live_shadow\" WHEN "))continue; // no DELETE or REPLACE here
            String event=" BEFORE UPDATE OF ";int start=sql.indexOf(event),end=sql.indexOf(" ON \"r27_live_shadow\" WHEN ",start+event.length());
            if(start<0||end<0)throw new Dependency("R31_OBSERVER_UNREVIEWED_RETENTION_EVENT:"+definition[0]);
            for(String column:quotedColumns(sql.substring(start+event.length(),end))){payloadColumns.add(column);if(java.util.Arrays.asList("status","temperature","reason","updated_at").contains(column))throw new Dependency("R31_OBSERVER_RETENTION_UPDATE_COLUMN_INTERSECTION:"+column);}
        }
        try(Cursor c=read.rawQuery("SELECT name FROM sqlite_master WHERE type='trigger' AND tbl_name='r27_live_shadow' LIMIT 3",null)){while(c.moveToNext())if(!known.contains(c.getString(0)))throw new Dependency("R31_OBSERVER_UNREVIEWED_TRIGGER_ENVELOPE:"+c.getString(0));}
        try(Cursor c=read.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='index' AND tbl_name='r27_live_shadow' LIMIT 2",null)){while(c.moveToNext())if(!"sqlite_autoindex_r27_live_shadow_1".equals(c.getString(0))||!c.isNull(1))throw new Dependency("R31_OBSERVER_UNREVIEWED_INDEX_ENVELOPE:"+c.getString(0));}
        Map<String,Object> contract=new LinkedHashMap<>();contract.put("expectedSourceGuards",definitions.length);contract.put("allOwnerGuardsInstalled",installed);contract.put("payloadUpdateColumns",payloadColumns);contract.put("observerChangedColumns",java.util.Arrays.asList("status","temperature","reason","updated_at"));contract.put("updateColumnIntersection",0);contract.put("deleteOrReplaceOperations",0);contract.put("exactOwnerDefinitionsVerified",true);return contract;
    }
    private static List<String> quotedColumns(String text){List<String> columns=new ArrayList<>();int i=0;while(i<text.length()){if(text.charAt(i++)!='\"')throw new Dependency("R31_OBSERVER_RETENTION_COLUMN_CONTRACT");StringBuilder name=new StringBuilder();boolean closed=false;while(i<text.length()){char c=text.charAt(i++);if(c=='\"'){if(i<text.length()&&text.charAt(i)=='\"'){name.append(c);i++;}else{closed=true;break;}}else name.append(c);}if(!closed)throw new Dependency("R31_OBSERVER_RETENTION_COLUMN_CONTRACT");columns.add(name.toString());if(i<text.length()&&text.charAt(i++)!=',')throw new Dependency("R31_OBSERVER_RETENTION_COLUMN_CONTRACT");}return columns;}
    private static long schemaVersion(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(!c.moveToFirst())throw new Dependency("R31_OBSERVER_SCHEMA_VERSION_UNKNOWN");return c.getLong(0);}}
    private static MemoryBudgetR31.Lease admit(String owner,long fence,long bytes,BooleanSupplier cancel){MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire(owner,fence,MemoryBudgetR31.Kind.CHECKPOINT,bytes,cancel);if(!admission.acquired())throw new Dependency("R31_OBSERVER_MEMORY_WAIT:"+admission.reason);return admission.lease;}
    private static void grow(MemoryBudgetR31.Lease memory,long bytes){if(bytes>memory.bytes()){MemoryBudgetR31.Admission admission=memory.grow(bytes-memory.bytes());if(!admission.acquired())throw new Dependency("R31_OBSERVER_MEMORY_WAIT:"+admission.reason);}}
    private static long memoryBytes(MemoryBudgetR31.Lease memory){return memory.bytes();}
    private static void appendRows(List<Object> target,List<String[]> rows){for(String[] row:rows)target.add(java.util.Arrays.asList(row));}
    private static void check(BooleanSupplier cancel){if(cancel.getAsBoolean()||Thread.currentThread().isInterrupted())throw new CancellationException("R31_OBSERVER_CANCELLED");}
    private static String slots(int count){return String.join(",",Collections.nCopies(count,"?"));}
    private static long utf8(String s){if(s==null)return 0;long count=0;for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c<128)count++;else if(c<2048)count+=2;else if(Character.isHighSurrogate(c)&&i+1<s.length()&&Character.isLowSurrogate(s.charAt(i+1))){count+=4;i++;}else count+=Character.isSurrogate(c)?1:3;}return count;}
}
