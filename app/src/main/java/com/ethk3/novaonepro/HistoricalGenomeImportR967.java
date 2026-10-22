package com.ethk3.novaonepro;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import org.json.JSONException;
import org.json.JSONObject;

/** R970: immutable historical inventory, not a certificate or a second algorithm creator.
 * Old source, state, lineage, status and evidence are preserved verbatim. A usable record may
 * supply a NEW initialized research seed through DirectedResearchCreationR932, with N=0.
 * Its old VM/evidence is NOT certified by parsing it under the current VM.
 */
public final class HistoricalGenomeImportR967 {
    public static final String SOURCE="research_historical_source_r967";
    public static final String ARCHIVE="research_historical_genome_archive_r967";
    public static final String USE="research_historical_genome_use_r967";
    public static final String KIND="HISTORICAL_RESEARCH_SEED";
    public static final String ASSET="research/r967_history/R908_125_algorithms.tsv";
    public static final String SOURCE_SHA="3697570b8db5ad0b14285685e7c2c12712dd02433d7efa6019e123838100ef80";
    public static final int SOURCE_ROWS=125, BATCH=4, MAX_BYTES=1048576;
    private static final String ACCEPT="ACCEPT_RESEARCH_SEED";
    private static final String HEADER="genome_id\tdisplay_name_zh\tgenome_spec\tstructural_hash\tbehavior_hash\tgeneration\tparent_ids\ttrained_until_issue\tearliest_use_issue\tdev_n\tdev_hit\tdev_hit_rate\tdev_wilson_lower\tdev_block_min\tdev_max_miss\tnovelty\tcomplexity\tstate_blob\tstate_updates\tstatus\tmay_authorize_real\tcreated_at\tupdated_at\tfuture_n\tfuture_hit\tfuture_hit_rate\tfuture_wilson_lower\tfuture_last_issue\tfuture_evidence_hash\tmechanism_spec_zh\tfuture_brier\tfuture_ece\tfuture_coverage\tfuture_block_min\tfuture_regime_min\tfuture_max_miss\tpaired_mainline_n\tpaired_mainline_delta\tlifecycle_reason\tresearch_genome_contract\tfuture_fdr_q\tmutation_intent\tcreation_reason";
    private static volatile Archive cached;
    private HistoricalGenomeImportR967(){}

    /** Called by the existing research migration owner, never by the UI or hot prediction path. */
    public static void schema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+SOURCE+"(source_hash TEXT PRIMARY KEY,asset_name TEXT NOT NULL,header TEXT NOT NULL,row_count INTEGER NOT NULL CHECK(row_count BETWEEN 1 AND 125),imported_issue TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ARCHIVE+"(record_id TEXT PRIMARY KEY,source_hash TEXT NOT NULL,row_number INTEGER NOT NULL,row_hash TEXT NOT NULL,raw_tsv TEXT NOT NULL,original_id TEXT NOT NULL,original_status TEXT NOT NULL,original_reason TEXT NOT NULL,program_hash TEXT NOT NULL,state_hash TEXT NOT NULL,verdict TEXT NOT NULL,reason TEXT NOT NULL,seed_spec TEXT NOT NULL,seed_hash TEXT NOT NULL,imported_issue TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(source_hash,row_number))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r970_history_queue ON "+ARCHIVE+"(source_hash,verdict,row_number)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r970_history_original ON "+ARCHIVE+"(original_id,program_hash)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+USE+"(record_id TEXT PRIMARY KEY,source_hash TEXT NOT NULL,record_hash TEXT NOT NULL,seed_key TEXT NOT NULL,attempt_id TEXT NOT NULL,boundary_issue TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,created_at INTEGER NOT NULL)");
        immutable(db,SOURCE,"source_hash", "OLD.source_hash=NEW.source_hash");
        immutable(db,ARCHIVE,"record_id", "OLD.record_id=NEW.record_id OR (OLD.source_hash=NEW.source_hash AND OLD.row_number=NEW.row_number)");
        immutable(db,USE,"record_id", "OLD.record_id=NEW.record_id");
    }
    private static void immutable(SQLiteDatabase db,String table,String key,String collision) {
        db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_update BEFORE UPDATE ON "+table+" BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_delete BEFORE DELETE ON "+table+" BEGIN SELECT RAISE(ABORT,'R970_HISTORY_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_replace BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" AS OLD WHERE "+collision+") BEGIN SELECT RAISE(ABORT,'R970_HISTORY_NO_REPLACE'); END");
    }

    static final class Archive {
        final String hash,header;
        final List<String> rows;
        final Set<String> conflictingIds;
        Archive(String h,String head,List<String> r,Set<String> conflicts) {
            hash=h;header=head;rows=Collections.unmodifiableList(new ArrayList<String>(r));
            conflictingIds=Collections.unmodifiableSet(new HashSet<String>(conflicts));
        }
    }
    static Archive decode(byte[] bytes,String expected,int expectedRows) {
        if(bytes==null||bytes.length>MAX_BYTES||expectedRows<1||expectedRows>SOURCE_ROWS)
            throw new IllegalArgumentException("R970_ARCHIVE_SIZE_OR_ROW_LIMIT");
        if(!sha(bytes).equals(expected))throw new IllegalArgumentException("R970_ARCHIVE_SHA256_MISMATCH");
        final String text;
        try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();}
        catch(CharacterCodingException bad){throw new IllegalArgumentException("R970_ARCHIVE_UTF8",bad);}
        if(text.indexOf('\r')>=0||text.indexOf('\0')>=0)throw new IllegalArgumentException("R970_ARCHIVE_NONCANONICAL_TEXT");
        String[] lines=text.split("\n",-1);
        int length=lines.length-(lines[lines.length-1].isEmpty()?1:0);
        if(length!=expectedRows+1||!HEADER.equals(lines[0]))throw new IllegalArgumentException("R970_ARCHIVE_HEADER_OR_ROW_COUNT");
        List<String> rows=new ArrayList<String>();Map<String,String> programs=new HashMap<String,String>();Set<String> conflicts=new HashSet<String>();
        for(int i=1;i<length;i++) {
            String[] values=lines[i].split("\t",-1);
            if(values.length!=43)throw new IllegalArgumentException("R970_ARCHIVE_COLUMN_COUNT:"+i);
            String old=programs.put(values[0],values[2]);if(old!=null&&!old.equals(values[2]))conflicts.add(values[0]);
            rows.add(lines[i]);
        }
        return new Archive(expected,lines[0],rows,conflicts);
    }
    private static Archive asset(Context context)throws IOException {
        Archive a=cached;if(a!=null)return a;
        if(context==null)throw new IOException("R970_ANDROID_ASSET_CONTEXT_UNAVAILABLE");
        synchronized(HistoricalGenomeImportR967.class){
            if(cached!=null)return cached;
            ByteArrayOutputStream out=new ByteArrayOutputStream();
            try(InputStream in=context.getAssets().open(ASSET)) {
                byte[] buffer=new byte[8192];int read;
                while((read=in.read(buffer))!=-1){if(out.size()+read>MAX_BYTES)throw new IOException("R970_ARCHIVE_TOO_LARGE");out.write(buffer,0,read);}
            }
            cached=decode(out.toByteArray(),SOURCE_SHA,SOURCE_ROWS);return cached;
        }
    }
    /** Legacy callers only enqueue work through the Director's existing pulse. */
    static String maintain(final DbStore store) { return "DEFER_IMPORT_TO_CANONICAL_DIRECTOR"; }

    /** Fixed sixth slot; persistent two-subslot fairness, no added scheduler or thread. */
    public static boolean maintainCP12(final DbStore store,final long deadlineNs) {
        if(store==null||System.nanoTime()>=deadlineNs)return false;
        final int slot=Integer.parseInt(store.getState("cp12_import_subslot","0"));
        if(slot<0||slot>1)throw new IllegalStateException("IMPORT_SUBSLOT_CORRUPT");
        store.setState("cp12_import_subslot",String.valueOf(1-slot));
        if(slot==1){String result=MolecularToolWorldR947.importSeedBatchCP4(store,deadlineNs);
            store.setState("cp12_import_last_result",result);
            return result.matches("[0-9a-f]{64}")||result.startsWith("SEEDS_IMPORTED:")
                    ||result.startsWith("ATOMS_IMPORTED:")||result.startsWith("ATOMS_COMPLETE:")
                    ||result.equals("SEEDS_CHECKPOINT_INITIALIZED")
                    ||result.startsWith("SEEDS_RAW_SCAN:")||result.startsWith("SEEDS_SCAN_REPLAY:")
                    ||result.startsWith("SEEDS_TYPED_MATERIALIZED:")||result.startsWith("SEEDS_MATERIAL_REPLAY:")
                    ||result.startsWith("NO_OP:")||result.startsWith("HELD:");}
        return importArchivePageCP12(store,deadlineNs);
    }
    private static final class PreparedRowCP12 {
        final int number;final String raw,rowHash;final String[] fields;final Verdict verdict;
        final String liveProgram,liveStatus;
        PreparedRowCP12(int n,String raw,Verdict v,String program,String status){
            number=n;this.raw=raw;rowHash=hash(raw);fields=raw.split("\t",-1);
            verdict=v;liveProgram=program;liveStatus=status;
        }
    }
    private static String boundedLineCP12(java.io.Reader in)throws IOException {
        StringBuilder b=new StringBuilder(1024);int ch;
        while((ch=in.read())!=-1){if(ch=='\n')return b.toString();
            if(ch=='\r'||ch==0||b.length()>=8192)throw new IOException("HISTORY_LINE_BOUND_OR_TEXT");b.append((char)ch);}
        return b.length()==0?null:b.toString();
    }
    /** Hash and parse outside DB writer; one 1MiB scope, at most four prepared rows. */
    public static boolean importArchivePageCP12(final DbStore store,long deadlineNs) {
        if(store==null||System.nanoTime()>=deadlineNs||RealtimePressureGateR811.hasActiveCriticalWork()
                ||SettlementExecutionGateR819.isActive())return false;
        if(count(store.readable(),"SELECT COUNT(*) FROM "+ARCHIVE+" WHERE source_hash=?",SOURCE_SHA)==SOURCE_ROWS)return false;
        final String latest=ResearchLearningContinuityR912.latest(store);
        if(!TargetIssueResolver.isValid(latest))return false;
        android.content.Context ctx=store.coordinatorR36().appContextR925();
        if(ctx==null)throw new IllegalStateException("HISTORY_ASSET_CONTEXT");
        try(com.ethk3.novaonepro.r952.MemoryBudgetCP4.Lease lease=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(1L<<20)) {
            MessageDigest digest=MessageDigest.getInstance("SHA-256");long bytes=0;
            try(InputStream in=ctx.getAssets().open(ASSET)){byte[] chunk=new byte[8192];int n;
                while((n=in.read(chunk))!=-1){bytes+=n;if(bytes>MAX_BYTES)throw new IOException("HISTORY_ASSET_BOUND");digest.update(chunk,0,n);}}
            StringBuilder hex=new StringBuilder(64);for(byte v:digest.digest()){int x=v&255;hex.append("0123456789abcdef".charAt(x>>>4)).append("0123456789abcdef".charAt(x&15));}
            if(!SOURCE_SHA.equals(hex.toString()))throw new IllegalStateException("HISTORY_ASSET_HASH");
            final Map<Integer,String> prior=new HashMap<Integer,String>();
            try(Cursor c=store.readable().rawQuery("SELECT row_number,row_hash FROM "+ARCHIVE+" WHERE source_hash=? ORDER BY row_number LIMIT 125",new String[]{SOURCE_SHA})){
                while(c.moveToNext())prior.put(c.getInt(0),c.getString(1));}
            Map<String,String> programs=new HashMap<String,String>();Set<String> conflicts=new HashSet<String>();
            List<String> pending=new ArrayList<String>(BATCH);List<Integer> numbers=new ArrayList<Integer>(BATCH);
            try(java.io.Reader in=new java.io.BufferedReader(new java.io.InputStreamReader(ctx.getAssets().open(ASSET),StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)),8192)){
                if(!HEADER.equals(boundedLineCP12(in)))throw new IllegalStateException("HISTORY_HEADER");
                for(int i=1;i<=SOURCE_ROWS;i++){
                    String raw=boundedLineCP12(in);if(raw==null)throw new IllegalStateException("HISTORY_ROW_COUNT");
                    String[] f=raw.split("\t",-1);if(f.length!=43)throw new IllegalStateException("HISTORY_COLUMN_COUNT");
                    String old=programs.put(f[0],hash(f[2]));if(old!=null&&!old.equals(hash(f[2])))conflicts.add(f[0]);
                    String previous=prior.get(i);if(previous!=null&&!previous.equals(hash(raw)))throw new IllegalStateException("HISTORY_ROW_IDENTITY");
                    if(previous==null&&pending.size()<BATCH){pending.add(raw);numbers.add(i);}
                }
                if(boundedLineCP12(in)!=null)throw new IllegalStateException("HISTORY_EXTRA_ROW");
            }
            final List<PreparedRowCP12> rows=new ArrayList<PreparedRowCP12>(BATCH);
            for(int i=0;i<pending.size();i++){
                if(System.nanoTime()>=deadlineNs)return false;ResearchWorkBudgetR927.checkpoint();
                String raw=pending.get(i),program=null,status=null;String[] f=raw.split("\t",-1);
                Verdict v=validate(raw,latest);if(conflicts.contains(f[0]))v=Verdict.reject("SOURCE_ID_PROGRAM_COLLISION");
                try(Cursor c=store.readable().rawQuery("SELECT genome_spec,status FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{f[0]})){
                    if(c.moveToFirst()){program=c.getString(0);status=c.getString(1);}}
                if(v.accepted()&&program!=null){if(!f[2].equals(program))v=Verdict.reject("CURRENT_ID_PROGRAM_COLLISION");
                    else if(status!=null&&status.startsWith("QUARANTINED"))v=Verdict.reject("CURRENT_QUARANTINE_PRESERVED");}
                rows.add(new PreparedRowCP12(numbers.get(i),raw,v,program,status));
            }
            if(rows.isEmpty()||System.nanoTime()>=deadlineNs)return false;
            Integer added=store.coordinatorR36().runAtomicWrite("CP12_HISTORY_ARCHIVE_PAGE",new DatabaseCoordinator.WriteCallable<Integer>(){
                public Integer call(SQLiteDatabase db){return commitArchivePageCP12(db,rows,latest);}},null);
            if(added==null)throw new IllegalStateException("HISTORY_ARCHIVE_COMMIT");
            return added.intValue()>0;
        }catch(IOException|NoSuchAlgorithmException e){throw new IllegalStateException("HISTORY_ASSET_PREPARE",e);}
    }
    /** Small identity CAS and append only; VM parsing/validation never occurs under writer. */
    private static int commitArchivePageCP12(SQLiteDatabase db,List<PreparedRowCP12> rows,String boundary) {
        transaction(db);boolean known=false;
        try(Cursor c=db.rawQuery("SELECT header,row_count,asset_name FROM "+SOURCE+" WHERE source_hash=?",new String[]{SOURCE_SHA})){
            if(c.moveToFirst()){known=true;if(!HEADER.equals(c.getString(0))||c.getInt(1)!=SOURCE_ROWS||!ASSET.equals(c.getString(2)))throw new IllegalStateException("HISTORY_SOURCE_CAS");}}
        if(!known)db.execSQL("INSERT INTO "+SOURCE+" VALUES(?,?,?,?,?,?)",new Object[]{SOURCE_SHA,ASSET,HEADER,SOURCE_ROWS,boundary,System.currentTimeMillis()});
        int added=0;
        for(PreparedRowCP12 r:rows){
            try(Cursor c=db.rawQuery("SELECT row_hash FROM "+ARCHIVE+" WHERE source_hash=? AND row_number=?",new String[]{SOURCE_SHA,String.valueOf(r.number)})){
                if(c.moveToFirst()){if(!r.rowHash.equals(c.getString(0)))throw new IllegalStateException("HISTORY_ROW_CAS");continue;}}
            String program=null,status=null;
            try(Cursor c=db.rawQuery("SELECT genome_spec,status FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{r.fields[0]})){
                if(c.moveToFirst()){program=c.getString(0);status=c.getString(1);}}
            if(!Objects.equals(program,r.liveProgram)||!Objects.equals(status,r.liveStatus))throw new IllegalStateException("HISTORY_LIVE_PROGRAM_CAS");
            Verdict v=r.verdict;String[] f=r.fields;
            db.execSQL("INSERT INTO "+ARCHIVE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{recordId(SOURCE_SHA,r.number),SOURCE_SHA,r.number,r.rowHash,r.raw,f[0],f[19],f[38],hash(f[2]),hash(f[17]),v.accepted()?ACCEPT:"REJECT",v.reason,v.seed==null?"":v.seed.canonical(),v.seed==null?"":v.seed.structuralHash,boundary,System.currentTimeMillis()});added++;
        }
        return added;
    }

    /** Only appends inventory. Creation, live model updates and formal sample tables are untouched. */
    static int importBatch(SQLiteDatabase db,Archive a,String boundary,int maximum) {
        transaction(db);if(!TargetIssueResolver.isValid(boundary))throw new IllegalArgumentException("R970_IMPORT_BOUNDARY");
        boolean known=false;
        try(Cursor c=db.rawQuery("SELECT header,row_count,asset_name FROM "+SOURCE+" WHERE source_hash=?",new String[]{a.hash})){
            if(c.moveToFirst()){known=true;if(!a.header.equals(c.getString(0))||a.rows.size()!=c.getInt(1)||!ASSET.equals(c.getString(2)))throw new IllegalStateException("R970_SOURCE_IDENTITY_CONFLICT");}
        }
        if(!known)db.execSQL("INSERT INTO "+SOURCE+" VALUES(?,?,?,?,?,?)",new Object[]{a.hash,ASSET,a.header,a.rows.size(),boundary,System.currentTimeMillis()});
        Map<Integer,String> prior=new HashMap<Integer,String>();
        try(Cursor c=db.rawQuery("SELECT row_number,row_hash FROM "+ARCHIVE+" WHERE source_hash=?",new String[]{a.hash})){while(c.moveToNext())prior.put(c.getInt(0),c.getString(1));}
        int added=0,limit=Math.max(1,Math.min(BATCH,maximum));
        for(int i=0;i<a.rows.size();i++) {
            String raw=a.rows.get(i),rowHash=hash(raw),old=prior.get(i+1);
            if(old!=null){if(!old.equals(rowHash))throw new IllegalStateException("R970_ARCHIVED_ROW_CHANGED");continue;}
            if(added>=limit)break;
            ResearchWorkBudgetR927.checkpoint();
            Verdict v=validate(raw,boundary);
            String[] f=raw.split("\t",-1);
            if(a.conflictingIds.contains(f[0]))v=Verdict.reject("SOURCE_ID_PROGRAM_COLLISION");
            if(v.accepted()) {
                try(Cursor c=db.rawQuery("SELECT genome_spec,status FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{f[0]})){
                    if(c.moveToFirst()) {
                        if(!f[2].equals(c.getString(0)))v=Verdict.reject("CURRENT_ID_PROGRAM_COLLISION");
                        else if(c.getString(1).startsWith("QUARANTINED"))v=Verdict.reject("CURRENT_QUARANTINE_PRESERVED");
                    }
                }
            }
            String id=recordId(a.hash,i+1);
            db.execSQL("INSERT INTO "+ARCHIVE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,a.hash,i+1,rowHash,raw,f[0],f[19],f[38],hash(f[2]),hash(f[17]),v.accepted()?ACCEPT:"REJECT",v.reason,v.seed==null?"":v.seed.canonical(),v.seed==null?"":v.seed.structuralHash,boundary,System.currentTimeMillis()});
            added++;
        }
        return added;
    }
    static final class Verdict {
        final String reason;final AlgorithmGenomeR908 seed;
        Verdict(String r,AlgorithmGenomeR908 g){reason=r;seed=g;}
        static Verdict reject(String r){return new Verdict(r,null);}
        boolean accepted(){return seed!=null;}
    }
    static Verdict validate(String raw,String boundary) {
        try {
            String[] f=raw.split("\t",-1);if(f.length!=43)return Verdict.reject("COLUMN_COUNT");
            if(!TargetIssueResolver.isValid(boundary))return Verdict.reject("IMPORT_BOUNDARY");
            AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(f[2]);
            if(!g.canonical().equals(f[2])||!g.genomeId.equals(f[0])||!g.structuralHash.equals(f[3])
                    ||!String.valueOf(g.generation).equals(f[5])||!g.parentIds.equals(f[6]))return Verdict.reject("PROGRAM_ID_HASH_OR_LINEAGE_MISMATCH");
            if(!g.algorithmGrade())return Verdict.reject("PROGRAM_NOT_ALGORITHM_GRADE");
            AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.State.decodeStrict(f[17]);
            if(!String.valueOf(state.updates).equals(f[18]))return Verdict.reject("STATE_COUNTER_MISMATCH");
            if(!TargetIssueResolver.isValid(f[7])||!TargetIssueResolver.isValid(f[8])||f[8].compareTo(f[7])<=0||boundary.compareTo(f[7])<0)
                return Verdict.reject("INVALID_PIT_TRAIN_OR_EARLIEST_BOUNDARY");
            if(!f[27].isEmpty()&&(!TargetIssueResolver.isValid(f[27])||f[27].compareTo(boundary)>0))return Verdict.reject("FUTURE_LABEL_AFTER_IMPORT_BOUNDARY");
            int n=nonnegative(f[23]),hit=nonnegative(f[24]);if(hit>n)return Verdict.reject("OLD_EVIDENCE_COUNT_INCONSISTENT");
            if(n>0&&f[27].isEmpty())return Verdict.reject("OLD_EVIDENCE_MISSING_LAST_ISSUE");
            if(!f[28].isEmpty()&&!f[28].matches("[0-9a-f]{64}"))return Verdict.reject("OLD_EVIDENCE_HASH_FORMAT");
            if(f[19].startsWith("QUARANTINED"))return Verdict.reject("ORIGINAL_QUARANTINE_PRESERVED");
            if(f[19].startsWith("PRUNED")||f[19].equals("RETIRED")||f[19].equals("ARCHIVED"))return Verdict.reject("ORIGINAL_RETIREMENT_PRESERVED");
            if(!Arrays.asList("OBSERVING_FUTURE_OOS","PAUSED_CAPACITY_R912","IDEA_WAIT_FUTURE_OOS","CHALLENGER_FUTURE_OOS").contains(f[19]))return Verdict.reject("UNKNOWN_ORIGINAL_STATUS");
            if(!f[20].equals("0"))return Verdict.reject("OLD_AUTHORITY_REQUIRES_INDEPENDENT_AUDIT");
            if(g.generation==Integer.MAX_VALUE)return Verdict.reject("GENERATION_OVERFLOW");
            state.updates=0; // Parameters are initialization only, never proof or credited updates.
            AlgorithmGenomeR908 seed=new AlgorithmGenomeR908(g.generation+1,g.genomeId,g.scoreProgram,g.updateProgram,g.scoreOutputReg,state.encode());
            return new Verdict("CURRENT_VM_RESEARCH_INITIALIZATION_ONLY_OLD_FUTURE_NOT_TRANSFERRED",seed);
        }catch(IllegalArgumentException bad){return Verdict.reject("INVALID_RECORD:"+bad.getMessage());}
    }
    private static int nonnegative(String x){if(!x.matches("0|[1-9][0-9]{0,9}"))throw new IllegalArgumentException("NONNEGATIVE_INTEGER");return Integer.parseInt(x);}

    static final class Seed {
        final String recordId,sourceHash,rowHash,originalId,originalProgramHash;
        final AlgorithmGenomeR908 genome;
        Seed(String id,String source,String row,String original,String oldHash,AlgorithmGenomeR908 g){recordId=id;sourceHash=source;rowHash=row;originalId=original;originalProgramHash=oldHash;genome=g;}
        void describe(JSONObject p)throws JSONException {
            p.put("historical_record_id",recordId);p.put("historical_source_sha256",sourceHash);p.put("historical_row_sha256",rowHash);
            p.put("historical_original_id",originalId);p.put("historical_original_program_hash",originalProgramHash);
            p.put("old_future_n_transferred",0);p.put("old_state_update_count_transferred",0);
            p.put("old_semantics_certified",false);p.put("current_vm_reinterpretation",true);
        }
    }
    /** The existing creator calls this inside its own transaction; at most one of four attempts. */
    static Seed choose(SQLiteDatabase db,String latest) {
        transaction(db);
        if(!TargetIssueResolver.isValid(latest))return null;
        try(Cursor c=db.rawQuery("SELECT kind FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE lane='algorithm' ORDER BY created_issue DESC,attempt_id DESC LIMIT 3",null)){
            while(c.moveToNext())if(KIND.equals(c.getString(0)))return null;
        }
        List<String[]> candidates=new ArrayList<String[]>();
        try(Cursor c=db.rawQuery("SELECT a.record_id,a.source_hash,a.row_hash,a.raw_tsv,a.seed_spec,a.seed_hash,a.imported_issue FROM "+ARCHIVE+" a LEFT JOIN "+USE+" u ON a.record_id=u.record_id WHERE a.source_hash=? AND a.verdict=? AND u.record_id IS NULL ORDER BY a.row_number LIMIT 8",new String[]{SOURCE_SHA,ACCEPT})){
            while(c.moveToNext()){String[] row=new String[7];for(int i=0;i<row.length;i++)row[i]=c.getString(i);candidates.add(row);}
        }
        for(String[] row:candidates){
            ResearchWorkBudgetR927.checkpoint();
            if(!hash(row[3]).equals(row[2]))throw new IllegalStateException("R970_ARCHIVE_ROW_HASH_CHANGED");
            Verdict v=validate(row[3],latest);String[] f=row[3].split("\t",-1);
            if(!v.accepted()||!row[4].equals(v.seed.canonical())||!row[5].equals(v.seed.structuralHash)){
                terminal(db,row[0],row[1],row[2],"","",latest,"REJECTED","CURRENT_INTERPRETER_INCOMPATIBLE:"+v.reason);continue;
            }
            Seed seed=new Seed(row[0],row[1],row[2],f[0],f[3],v.seed);
            if(row[6].compareTo(latest)>0)throw new IllegalStateException("R970_ARCHIVE_FROM_FUTURE_BOUNDARY");
            boolean collision=false;
            try(Cursor c=db.rawQuery("SELECT genome_spec,status FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{f[0]})){
                if(c.moveToFirst()&&(!f[2].equals(c.getString(0))||c.getString(1).startsWith("QUARANTINED")))collision=true;
            }
            if(collision){rejected(db,seed,"",latest,"CURRENT_ID_PROGRAM_COLLISION_OR_QUARANTINE");continue;}
            String existing=null;
            try(Cursor c=db.rawQuery("SELECT genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{seed.genome.genomeId})){if(c.moveToFirst())existing=c.getString(0);}
            if(existing!=null){rejected(db,seed,"",latest,existing.equals(seed.genome.canonical())?"DUPLICATE_SEED_NO_EVIDENCE_MERGE":"NEW_SEED_ID_COLLISION");continue;}
            return seed;
        }
        return null;
    }
    static void committed(SQLiteDatabase db,Seed seed,String attempt,String child,String latest) {
        transaction(db);
        try(Cursor c=db.rawQuery("SELECT genome_spec,state_updates,future_n,future_hit,may_authorize_real,dev_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{seed.genome.genomeId})){
            if(!c.moveToFirst()||!seed.genome.canonical().equals(c.getString(0))||c.getLong(1)!=0||c.getLong(2)!=0||c.getLong(3)!=0||c.getInt(4)!=0||c.getInt(5)!=0)
                throw new IllegalStateException("R970_FRESH_SEED_INSERT_OR_ZERO_EVIDENCE_CONTRACT");
        }
        terminal(db,seed.recordId,seed.sourceHash,seed.rowHash,child,attempt,latest,"CREATED_N0","CURRENT_VM_NEW_RESEARCH_SEED_NOT_OLD_PROOF");
    }
    static void rejected(SQLiteDatabase db,Seed seed,String attempt,String latest,String reason){terminal(db,seed.recordId,seed.sourceHash,seed.rowHash,"",attempt,latest,"REJECTED",reason);}
    private static void terminal(SQLiteDatabase db,String id,String source,String row,String child,String attempt,String latest,String state,String reason){
        transaction(db);db.execSQL("INSERT INTO "+USE+" VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{id,source,row,child,attempt,latest,state,reason,System.currentTimeMillis()});
    }
    /** Bounded read-only provenance listing. Raw programs/states remain in the archive and asset. */
    public static void writeReport(DbStore store,java.io.Writer out)throws IOException {
        out.write("R970_HISTORICAL_INVENTORY|source="+SOURCE_SHA+"|expected=125|OLD_FUTURE_NOT_TRANSFERRED\n");
        if(store==null){out.write("STATUS=STORE_NULL\n");return;}
        out.write("worker_status="+store.getState("r970_historical_import_status","NOT_YET_RUN")+"\n");
        try(Cursor c=store.readable().rawQuery("SELECT a.row_number,a.record_id,a.original_id,a.original_status,a.original_reason,a.verdict,a.reason,a.row_hash,a.program_hash,a.state_hash,COALESCE(u.state,''),COALESCE(u.seed_key,''),COALESCE(u.attempt_id,''),COALESCE(u.reason,'') FROM "+ARCHIVE+" a LEFT JOIN "+USE+" u ON a.record_id=u.record_id WHERE a.source_hash=? ORDER BY a.row_number LIMIT 125",new String[]{SOURCE_SHA})) {
            while(c.moveToNext()){
                out.write("row="+c.getInt(0)+"|record="+c.getString(1)+"|original="+c.getString(2)+"|old_status="+c.getString(3)
                    +"|old_reason="+c.getString(4)+"|verdict="+c.getString(5)+"|reason="+c.getString(6)+"|row_sha="+c.getString(7)
                    +"|program_sha="+c.getString(8)+"|state_sha="+c.getString(9)+"|use_state="+c.getString(10)
                    +"|new_seed="+c.getString(11)+"|creation_attempt="+c.getString(12)+"|use_reason="+c.getString(13)+"\n");
            }
        }catch(RuntimeException error){out.write("STATUS=READ_ERROR|"+error.getClass().getSimpleName()+":"+error.getMessage()+"\n");}
        out.write("Interpretation=CURRENT_VM_INITIALIZATION_ONLY; OLD_VM_AND_OLD_FUTURE_NOT_RECERTIFIED; SEED_CREATION_IS_NOT_TRAINING_OR_CERTIFICATION\n");
    }

    static String recordId(String source,int row){return new ResearchSemanticsR967.Builder("HISTORICAL_RECORD_V1").hash("source",source).integer("row",row).digest();}
    private static void transaction(SQLiteDatabase db){if(db==null||!db.inTransaction())throw new IllegalStateException("R970_HISTORY_TRANSACTION_REQUIRED");}
    private static long count(SQLiteDatabase db,String sql,String parameter){try(Cursor c=db.rawQuery(sql,new String[]{parameter})){return c.moveToFirst()?c.getLong(0):0;}}
    static String hash(String s){return V51ChecksumEngine.sha256(s);}
    static String sha(byte[] b){try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(b);StringBuilder s=new StringBuilder(64);for(byte value:digest){int x=value&255;s.append("0123456789abcdef".charAt(x>>>4)).append("0123456789abcdef".charAt(x&15));}return s.toString();}catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}

public static void writeReportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store,java.io.Writer out)throws IOException {
        out.write("R970_HISTORICAL_INVENTORY|source="+SOURCE_SHA+"|expected=125|OLD_FUTURE_NOT_TRANSFERRED\n");
        if(store==null){out.write("STATUS=STORE_NULL\n");return;}
        out.write("worker_status="+store.getState("r970_historical_import_status","NOT_YET_RUN")+"\n");
        try(Cursor c=report.select("SELECT a.row_number,a.record_id,a.original_id,a.original_status,a.original_reason,a.verdict,a.reason,a.row_hash,a.program_hash,a.state_hash,COALESCE(u.state,''),COALESCE(u.seed_key,''),COALESCE(u.attempt_id,''),COALESCE(u.reason,'') FROM "+ARCHIVE+" a LEFT JOIN "+USE+" u ON a.record_id=u.record_id WHERE a.source_hash=? ORDER BY a.row_number LIMIT 125",new String[]{SOURCE_SHA})) {
            while(c.moveToNext()){
                out.write("row="+c.getInt(0)+"|record="+c.getString(1)+"|original="+c.getString(2)+"|old_status="+c.getString(3)
                    +"|old_reason="+c.getString(4)+"|verdict="+c.getString(5)+"|reason="+c.getString(6)+"|row_sha="+c.getString(7)
                    +"|program_sha="+c.getString(8)+"|state_sha="+c.getString(9)+"|use_state="+c.getString(10)
                    +"|new_seed="+c.getString(11)+"|creation_attempt="+c.getString(12)+"|use_reason="+c.getString(13)+"\n");
            }
        }catch(RuntimeException error){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
        out.write("Interpretation=CURRENT_VM_INITIALIZATION_ONLY; OLD_VM_AND_OLD_FUTURE_NOT_RECERTIFIED; SEED_CREATION_IS_NOT_TRAINING_OR_CERTIFICATION\n");
    }
}
