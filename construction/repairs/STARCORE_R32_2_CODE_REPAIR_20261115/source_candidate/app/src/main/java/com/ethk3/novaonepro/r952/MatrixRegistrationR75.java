package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Metadata discovery owned by the existing MATRIX slice. Sources still own every numeric input. */
public final class MatrixRegistrationR75 {
    private MatrixRegistrationR75() {}
    private static final String KEY="r75_matrix_registration_";
    private static final int BATCH=8, PHASES=6;
    private static final MatrixProjectionCP12.Type[] HISTORY={MatrixProjectionCP12.Type.FrequencyMatrix,
        MatrixProjectionCP12.Type.OmissionMatrix,MatrixProjectionCP12.Type.TransitionMatrix,
        MatrixProjectionCP12.Type.DriftMatrix,MatrixProjectionCP12.Type.ConditionalTransitionMatrix};
    private static final class Fence {
        final String sql;final String[] args;
        Fence(String sql,String... args){this.sql=sql;this.args=args;}
        boolean matches(SQLiteDatabase db){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst();}}
    }
    private static final class Page {
        final int phase;final String cursor;String next,status="WAIT_SOURCE";
        final List<MatrixProjectionCP12.Spec> specs=new ArrayList<>();
        final List<Fence> fences=new ArrayList<>();
        final List<String[]> systemFailures=new ArrayList<>();
        final Map<String,String> bootstrap=new LinkedHashMap<>();
        Page(int phase,String cursor){this.phase=phase;this.cursor=cursor;next=cursor;}
    }
    static MatrixProjectionCP12.Spec spec(MatrixProjectionCP12.Type type,String partition){
        return new MatrixProjectionCP12.Spec(type,partition,300,
            MatrixProjectionCP12.hash("R75_MATRIX_DISCOVERY_V1|"+type+'|'+partition+"|300"));
    }
    /** Runs after consumption. Bounded preparation, no SQL/numeric work under an unbounded writer. */
    public static boolean drain(DbStore store,long deadlineNanos){
        if(System.nanoTime()+10000000L>=deadlineNanos)return false;
        final SQLiteDatabase db=store.readable();if(db.inTransaction())throw new IllegalStateException("R75_MATRIX_DISCOVERY_IN_WRITER");
        final int phase=Integer.parseInt(store.getState(KEY+"phase","0"));
        if(phase<0||phase>=PHASES)throw new IllegalStateException("R75_MATRIX_DISCOVERY_PHASE");
        final Page page=new Page(phase,store.getState(KEY+phase,""));
        try {
          try(MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1572864);
              MemoryBudgetCP4.Lease models=(phase==2||phase==4)?RuntimeMemoryGuardR811.algorithmModelBudget().acquire(4L<<20):null){
            ResearchWorkBudgetR927.checkpoint();
            try {
                switch(phase){case 0:history(db,page);break;case 1:latency(db,page);break;
                    case 2:frozen(db,page);break;case 3:peers(db,page);break;case 4:lineage(db,page);break;case 5:systemFailuresR81(db,page);break;default:throw new AssertionError();}
            }catch(IllegalArgumentException|IllegalStateException badSource){
                // An invalid optional source must neither gain evidence nor pin the existing consumer.
                if(transientFailure(badSource))return false;
                page.specs.clear();page.fences.clear();page.bootstrap.clear();page.systemFailures.clear();String reason=badSource.getMessage();
                page.status="WAIT_INVALID_SOURCE:"+(reason!=null&&reason.matches("[A-Z0-9_:.]{1,96}")?reason:badSource.getClass().getSimpleName());
            }
            if(System.nanoTime()+2000000L>=deadlineNanos)return false; // no cursor movement on budget yield
            Boolean committed=store.coordinatorForResearchR952().runAtomicWrite("R75_MATRIX_REGISTER",new DatabaseCoordinator.WriteCallable<Boolean>(){
                public Boolean call(SQLiteDatabase writer){
                    if(!Integer.toString(page.phase).equals(state(writer,KEY+"phase","0"))||!page.cursor.equals(state(writer,KEY+page.phase,"")))return false;
                    for(Fence fence:page.fences)if(!fence.matches(writer))return false;
                    if(page.specs.size()>BATCH)throw new IllegalStateException("R75_MATRIX_DISCOVERY_BATCH");
                    for(MatrixProjectionCP12.Spec spec:page.specs)MatrixSnapshotBusR952.registerSpecCP12(writer,spec);
                    for(String[] failure:page.systemFailures)MatrixSourceRouterCP12.enqueueFailureCP12(writer,failure[0],"TRAIN_INFRASTRUCTURE_DELIVERY","SYSTEM",MatrixProjectionCP12.hash("CP12_REGIME_UNAVAILABLE_V1"),"UNKNOWN",2,failure[1],MatrixSourceRouterCP12.issueOrdinal(failure[0]),null);
                    for(Map.Entry<String,String> bound:page.bootstrap.entrySet())writer.execSQL("INSERT OR IGNORE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{bound.getKey(),bound.getValue(),System.currentTimeMillis()});
                    put(writer,KEY+page.phase,page.next);put(writer,KEY+"phase",Integer.toString((page.phase+1)%PHASES));
                    put(writer,KEY+"status_"+page.phase,!page.systemFailures.isEmpty()?"SYSTEM_FAILURE_EPISODES_PROJECTED:"+page.systemFailures.size():page.specs.isEmpty()?page.status:"REGISTERED_OR_PRESENT:"+page.specs.size());
                    return !page.specs.isEmpty()||!page.systemFailures.isEmpty();
                }},null);
            return Boolean.TRUE.equals(committed);
          }
        }catch(RuntimeException transientOrFatal){if(transientFailure(transientOrFatal))return false;throw transientOrFatal;}
    }
    static boolean registrationFirst(SQLiteDatabase db){String turn=state(db,KEY+"turn","0");put(db,KEY+"turn",turn.equals("0")?"1":"0");return turn.equals("0");}
    private static boolean transientFailure(Throwable failed){for(Throwable c=failed;c!=null;c=c.getCause()){String text=String.valueOf(c.getMessage()).toUpperCase(java.util.Locale.US);if(c instanceof ResearchWorkBudgetR927.Yield||text.contains("BUDGET")||text.contains("PREEMPT")||text.contains("DATABASE IS LOCKED")||text.contains("DATABASE IS BUSY")||text.contains("SQLITE_BUSY")||text.contains("SQLITE_LOCKED")||text.contains("DEADLINE")||text.contains("INTERRUPT"))return true;}return false;}

    private static void history(SQLiteDatabase db,Page p){
        int cursor=p.cursor.isEmpty()?0:Integer.parseInt(p.cursor);
        // 10 positions, four unconditional views and five genuine predraw regime-conditioned views.
        for(int i=cursor;i<90&&p.specs.size()<BATCH;i++){
            int position=i/9+1,kind=i%9;MatrixProjectionCP12.Type type=HISTORY[Math.min(kind,4)];
            String part="position:"+position;
            if(type==MatrixProjectionCP12.Type.TransitionMatrix)part+="|lag:1";
            if(type==MatrixProjectionCP12.Type.ConditionalTransitionMatrix)part+="|lag:1|regime:"+(kind-4)+"|regimePolicy:"+AtomRegimePolicyCP12.POLICY_HASH;
            MatrixProjectionCP12.Spec discovered=spec(type,part);p.specs.add(discovered);p.next=Integer.toString(i+1);
            int retain=type==MatrixProjectionCP12.Type.DriftMatrix?600:300;
            String filter=type==MatrixProjectionCP12.Type.ConditionalTransitionMatrix?" AND EXISTS(SELECT 1 FROM research_matrix_regime5_cp12 r WHERE r.target_issue=d.issue AND r.position="+position+" AND r.policy_hash='"+AtomRegimePolicyCP12.POLICY_HASH+"' AND r.regime_id="+(kind-4)+" AND r.captured_at<d.open_time)":"";
            String lower="";try(Cursor c=db.rawQuery("SELECT d.issue FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.dirty=0"+filter+" ORDER BY d.issue DESC LIMIT 1 OFFSET "+(retain-1),null)){if(c.moveToFirst())lower=c.getString(0);}
            p.bootstrap.put(bootstrapKey(discovered),lower);
        }
        p.status="DEFAULTS_REGISTERED_WAIT_AUTHORITATIVE_SOURCE";
    }
    static String bootstrapKey(MatrixProjectionCP12.Spec spec){return "r75_matrix_bootstrap_"+spec.matrixId;}
    private static void latency(SQLiteDatabase db,Page p){
        String[] cursor=split(p.cursor);long row=cursor[0].isEmpty()?0:Long.parseLong(cursor[0]);int offset=Integer.parseInt(cursor[1]);
        String spans;long found;int release;
        try(Cursor c=db.rawQuery("SELECT rowid,stage_samples,release_code FROM "+RuntimeResourceAuditR927.TABLE+" WHERE rowid"+(offset>0?">=":">")+"? AND finished_at>0 ORDER BY rowid LIMIT 1",new String[]{Long.toString(row)})){
            if(!c.moveToFirst()){p.status="WAIT_EXCLUSIVE_AUDIT";return;}found=c.getLong(0);spans=c.getString(1);release=c.getInt(2);
        }
        p.next=found+"~0";
        if(spans==null||spans.length()>65536||release<1)throw new IllegalStateException("AUDIT_BOUND");
        String[] entries=spans.split(";",-1);if(entries.length>512)throw new IllegalStateException("AUDIT_BOUND");
        if(found!=row)offset=0;
        LinkedHashMap<String,MatrixProjectionCP12.Spec> list=new LinkedHashMap<>();
        for(String entry:entries){String[] e=entry.split("\\|",-1);
            if(e.length!=6||!e[0].equals("CP12EX1")||!e[1].matches("[A-Z][A-Z0-9_]{0,63}"))continue;
            long wall=Long.parseLong(e[2]),cpu=Long.parseLong(e[3]),thread=Long.parseLong(e[4]);
            if(wall<0||cpu< -1||thread<0||!(e[5].equals("SYNC")||e[5].equals("QUEUE")))throw new IllegalStateException("AUDIT_SPAN");
            for(String unit:cpu<0?new String[]{"WALL_MICROS"}:new String[]{"WALL_MICROS","CPU_MICROS"}){
                MatrixProjectionCP12.Spec s=spec(MatrixProjectionCP12.Type.LatencyMatrix,"stage:"+e[1]+"|unit:"+unit+"|device:ANDROID|release:"+release);list.put(s.matrixId,s);
            }
        }
        List<MatrixProjectionCP12.Spec> all=new ArrayList<>(list.values());
        addSlice(p,all,offset);if(offset+p.specs.size()<all.size())p.next=found+"~"+(offset+p.specs.size());
        p.fences.add(new Fence("SELECT 1 FROM "+RuntimeResourceAuditR927.TABLE+" WHERE rowid=? AND stage_samples=? AND release_code=? AND finished_at>0",Long.toString(found),spans,Integer.toString(release)));
    }
    private static void frozen(SQLiteDatabase db,Page p){
        String[] cursor=split(p.cursor);String id;int offset=Integer.parseInt(cursor[1]);
        try(Cursor c=db.rawQuery("SELECT candidate_id FROM r967_frozen_candidate_all WHERE candidate_id"+(offset>0?">=":">")+"? ORDER BY candidate_id LIMIT 1",new String[]{cursor[0]})){
            if(!c.moveToFirst()){p.next="";p.status="WAIT_FROZEN_MODEL_BINDING_OR_TEACHER";return;}id=c.getString(0);
        }
        p.next=id+"~0";if(!id.equals(cursor[0]))offset=0;
        FrozenCandidateAdapterR958.Frozen f=loadBounded(db,id);
        if(f==null)return;
        p.fences.add(fence(f));
        List<MatrixProjectionCP12.Spec> base=new ArrayList<>();
        base.add(spec(MatrixProjectionCP12.Type.RuleHealthMatrix,f.structure));
        for(MatrixProjectionCP12.Type t:new MatrixProjectionCP12.Type[]{MatrixProjectionCP12.Type.CandidateWindowMatrix,MatrixProjectionCP12.Type.CandidateErrorMatrix,MatrixProjectionCP12.Type.ConfidenceMatrix,MatrixProjectionCP12.Type.ResidualMatrix})
            base.add(MatrixSourceRouterCP12.frozenPolicySpecCP12(t,f.structure,300));
        int index=0;for(MatrixProjectionCP12.Spec s:base){if(inPage(index++,offset))p.specs.add(s);}
        if(f.ast.formatVersion==3&&!f.model.isLegacy()){
            FeatureManifestR952 manifest=FeatureManifestR952.forAst(f.ast);f.model.modern().requireManifest(manifest);
            for(TypedAtomMoleculeGraphR930.NodeSpec node:MatrixSourceRouterCP12.consumedAtomsCP12(f.ast)){
                int feature=f.ast.graphSpec.featureOutputIds.indexOf(node.id);
                boolean scalar=node.shape.elements()==1&&feature>=0&&feature<f.model.modern().featureN;
                int count=scalar?11:1,begin=index;index+=count;
                ResearchWorkBudgetR927.checkpoint();
                if(begin>=offset+BATCH||index<=offset)continue; // do not prepare nodes from another page
                AtomSpecCP4 atom;
                try(Cursor c=db.rawQuery("SELECT spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{node.source})){
                    if(!c.moveToFirst()){p.status="WAIT_ATOM_REGISTRY";continue;}
                    atom=AtomSpecCP4.fromRegistry(node.source,c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getString(4),c.getString(5));
                }
                if(!manifest.atomSpecHashes.contains(atom.specHash))throw new IllegalStateException("FROZEN_ATOM_MANIFEST");
                String binding=MatrixSourceRouterCP12.atomBindingIdentityCP12(db,f.ast,manifest,node,atom);
                if(inPage(begin,offset))p.specs.add(MatrixSourceRouterCP12.atomStudySpecCP12(f.structure,f.model,manifest,atom,binding,node));
                if(scalar)for(int digit=0;digit<10;digit++)if(inPage(begin+digit+1,offset))
                    p.specs.add(atomTargetSpec(f.structure,f.model,manifest,atom,binding,feature,digit));
            }
            if(f.ast.graphSpec.modelKind==CandidateCompilerR952.ModelKind.EXPERT_HEDGE){
                requireExpertRefs(f);
                for(CandidateCompilerR952.ExpertRef teacher:f.ast.graphSpec.experts){
                    ResearchWorkBudgetR927.checkpoint();if(!inPage(index++,offset))continue;
                    FrozenCandidateAdapterR958.Frozen t=byHash(db,teacher.freezeHash);
                    if(!matchesTeacher(f,t,teacher,f.createdIssue)){p.status="WAIT_FROZEN_TEACHER";continue;}
                    p.fences.add(fence(t));p.specs.add(spec(MatrixProjectionCP12.Type.TeacherMatrix,t.structure+'|'+f.structure));
                }
            }
        }
        if(offset+BATCH<index)p.next=id+"~"+(offset+BATCH);
    }
    private static boolean inPage(int index,int offset){return index>=offset&&index<offset+BATCH;}
    static MatrixProjectionCP12.Spec atomTargetSpec(String freeze,CandidateCompilerR952.Model model,FeatureManifestR952 manifest,AtomSpecCP4 atom,String binding,int feature,int digit){
        String part=atom.specHash+"|freeze:"+freeze+"|digit:"+digit+"|bins:"+MatrixSourceRouterCP12.atomBinPolicyHash(model.modern().normalizerHash,feature);
        String scope=MatrixSourceRouterCP12.atomSourceScopeHashCP12(freeze,model,manifest,binding);
        return new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.AtomTargetMatrix,part,300,MatrixProjectionCP12.hash("R75_FROZEN_ATOM_TARGET|"+scope+'|'+part+"|300"));
    }
    private static void requireExpertRefs(FrozenCandidateAdapterR958.Frozen student){
        List<CandidateCompilerR952.ExpertRef> a=student.ast.graphSpec.experts,b=student.model.modern().experts;
        if(a.size()!=b.size())throw new IllegalStateException("TEACHER_MODEL_REF_CLOSURE");
        for(int i=0;i<a.size();i++)if(!a.get(i).freezeHash.equals(b.get(i).freezeHash)||!a.get(i).modelHash.equals(b.get(i).modelHash)||!a.get(i).featureManifestHash.equals(b.get(i).featureManifestHash)||a.get(i).position!=b.get(i).position)throw new IllegalStateException("TEACHER_MODEL_REF_CLOSURE");
    }
    private static FrozenCandidateAdapterR958.Frozen byHash(SQLiteDatabase db,String hash){
        try(Cursor c=db.rawQuery("SELECT candidate_id FROM r967_frozen_candidate_all WHERE structural_hash=? ORDER BY candidate_id LIMIT 1",new String[]{hash})){return c.moveToFirst()?loadBounded(db,c.getString(0)):null;}
    }
    private static boolean matchesTeacher(FrozenCandidateAdapterR958.Frozen student,FrozenCandidateAdapterR958.Frozen teacher,CandidateCompilerR952.ExpertRef ref,String issue){
        if(teacher==null||teacher.ast.formatVersion!=3||teacher.model.isLegacy())return false;
        FeatureManifestR952 manifest=FeatureManifestR952.forAst(teacher.ast);teacher.model.modern().requireManifest(manifest);
        return teacher.structure.equals(ref.freezeHash)&&teacher.modelHash.equals(ref.modelHash)
            &&manifest.hash.equals(ref.featureManifestHash)&&teacher.ast.position==ref.position&&ref.position==student.ast.position
            &&teacher.createdIssue.compareTo(student.createdIssue)<=0&&student.createdIssue.compareTo(issue)<=0&&teacher.trainedUntil.compareTo(issue)<0&&!teacher.structure.equals(student.structure);
    }
    static boolean teacherPair(SQLiteDatabase db,String teacherHash,String studentHash,String issue){
      try(MemoryBudgetCP4.Lease models=RuntimeMemoryGuardR811.algorithmModelBudget().acquire(4L<<20)){
        FrozenCandidateAdapterR958.Frozen student=byHash(db,studentHash),teacher=byHash(db,teacherHash);
        if(student==null||student.ast.formatVersion!=3||student.model.isLegacy()||student.ast.graphSpec.modelKind!=CandidateCompilerR952.ModelKind.EXPERT_HEDGE)return false;
        student.model.modern().requireManifest(FeatureManifestR952.forAst(student.ast));requireExpertRefs(student);
        for(CandidateCompilerR952.ExpertRef ref:student.ast.graphSpec.experts)if(ref.freezeHash.equals(teacherHash))return matchesTeacher(student,teacher,ref,issue);
        return false;
      }
    }
    private static FrozenCandidateAdapterR958.Frozen loadBounded(SQLiteDatabase db,String id){
        try(Cursor c=db.rawQuery("SELECT length(CAST(ast AS BLOB)),length(CAST(model_payload AS BLOB)) FROM r967_frozen_candidate_all WHERE candidate_id=?",new String[]{id})){
            if(!c.moveToFirst())return null;if(c.getLong(0)<1||c.getLong(0)>32768||c.getLong(1)<1||c.getLong(1)>262144)throw new IllegalStateException("FROZEN_SIZE");
        }
        return FrozenCandidateAdapterR958.load(db,id);
    }
    private static Fence fence(FrozenCandidateAdapterR958.Frozen f){return new Fence("SELECT 1 FROM r967_frozen_candidate_all WHERE candidate_id=? AND structural_hash=? AND model_hash=? AND ast=? AND model_payload=?",f.id,f.structure,f.modelHash,f.ast.canonical,f.model.payload());}
    private static void peers(SQLiteDatabase db,Page p){
        // The formal source owner already discovers these peers on common Future receipts.
        String id,part,manifest;
        try(Cursor c=db.rawQuery("SELECT matrix_id,partition_key,spec_manifest_hash FROM research_matrix_spec_cp12 WHERE matrix_type='CandidateSimilarityMatrix' AND window_size=300 AND matrix_id>? ORDER BY matrix_id LIMIT 1",new String[]{p.cursor})){
            if(!c.moveToFirst()){p.next="";p.status="WAIT_COMMON_FORMAL_FUTURE";return;}id=c.getString(0);part=c.getString(1);manifest=c.getString(2);
        }
        p.next=id;String[] pair=part.split("\\|",-1);if(pair.length!=2)return;
        MatrixProjectionCP12.Spec source=ProspectiveDiversityCP12.specCP12(MatrixProjectionCP12.Type.CandidateSimilarityMatrix,pair[0],pair[1]);
        if(!source.matrixId.equals(id)||!source.manifest.equals(manifest))return;
        MatrixProjectionCP12.Spec co=spec(MatrixProjectionCP12.Type.CoOccurrenceMatrix,part);
        if(MatrixSourceRouterCP12.preparePairedFormalCP12(db,co)==null)return;
        p.specs.add(co);p.fences.add(new Fence("SELECT 1 FROM research_matrix_spec_cp12 WHERE matrix_id=? AND spec_manifest_hash=?",id,manifest));
    }
    private static void lineage(SQLiteDatabase db,Page p){
        String parent,child,key;String[] cursor=p.cursor.isEmpty()?new String[]{"0","",""}:p.cursor.split("~",-1);
        if(cursor.length!=3)throw new IllegalStateException("LINEAGE_CURSOR");
        try(Cursor c=db.rawQuery("SELECT l.rowid,p.candidate_id,c.candidate_id FROM r974_model_lineage l JOIN r967_frozen_evaluation_v2 p ON p.learner_id=l.parent_id JOIN r967_frozen_evaluation_v2 c ON c.learner_id=l.child_id WHERE l.rowid>=? AND (l.rowid>? OR p.candidate_id>? OR (p.candidate_id=? AND c.candidate_id>?)) ORDER BY l.rowid,p.candidate_id,c.candidate_id LIMIT 1",new String[]{cursor[0],cursor[0],cursor[1],cursor[1],cursor[2]})){
            if(!c.moveToFirst()){p.next="";p.status="WAIT_FROZEN_PARENT_CHILD";return;}parent=c.getString(1);child=c.getString(2);key=c.getLong(0)+"~"+parent+"~"+child;
        }
        p.next=key;FrozenCandidateAdapterR958.Frozen a=loadBounded(db,parent),b=loadBounded(db,child);
        if(a==null||b==null||a.structure.equals(b.structure)||a.ast.position!=b.ast.position)return;
        p.specs.add(spec(MatrixProjectionCP12.Type.ParentChildMatrix,a.structure+'|'+b.structure));p.fences.add(fence(a));p.fences.add(fence(b));
        p.fences.add(new Fence("SELECT 1 FROM r974_model_lineage WHERE parent_id=? AND child_id=?",a.learner,b.learner));
    }
    /** The outbox lease owner's actual failed attempt is the source, never a diagnostic string.
     * Resource yields, dependency retirements and ordinary RUNNING/COMPLETED receipts
     * do not become SYSTEM failures. A delivery has one failure episode; retries retain their original journal audit. No downtime estimate is made.
     * Original journal lacks monotonic failure/recovery endpoints: no recovery lag is invented. */
    private static void systemFailuresR81(SQLiteDatabase db,Page p){
        long cursor=p.cursor.isEmpty()?0:Long.parseLong(p.cursor);if(cursor<0)throw new IllegalArgumentException("SYSTEM_FAILURE_CURSOR");
        String sql="SELECT j.rowid,j.delivery_hash,j.attempt,j.state,j.error_code,j.event_hash,j.created_at,o.target_issue,o.candidate_id,o.settlement_hash,o.label_hash,o.attempt_count,revision.generation FROM r952_recovery_journal j JOIN research_row_revision_v2 revision ON revision.source_table='r952_recovery_journal' AND revision.source_rowid=j.rowid LEFT JOIN r952_research_outbox o ON o.delivery_hash=j.delivery_hash WHERE revision.generation>? ORDER BY revision.generation LIMIT 8";
        int scanned=0;
        try(Cursor c=db.rawQuery(sql,new String[]{Long.toString(cursor)})){while(c.moveToNext()){
            ResearchWorkBudgetR927.checkpoint();scanned++;long row=c.getLong(0),at=c.getLong(6);String delivery=c.getString(1),state=c.getString(3),error=c.getString(4),event=c.getString(5);int attempt=c.getInt(2);
            p.next=Long.toString(c.getLong(12)); // AUTOINCREMENT revision survives source rowid reuse after archival.
            p.fences.add(new Fence("SELECT 1 FROM research_row_revision_v2 WHERE source_table='r952_recovery_journal' AND source_rowid=? AND generation=?",Long.toString(row),Long.toString(c.getLong(12))));
            p.fences.add(new Fence("SELECT 1 FROM r952_recovery_journal WHERE rowid=? AND delivery_hash=? AND attempt=? AND state=? AND error_code=? AND event_hash=? AND created_at=?",Long.toString(row),delivery,Integer.toString(attempt),state,error,event,Long.toString(at)));
            if(!"RETRYABLE_FAILED".equals(state)||error==null||!error.startsWith("RETRYABLE_INFRASTRUCTURE|"))continue;
            if(at<=0||attempt<1||error.length()>500||!MatrixProjectionCP12.hash("RECOVERY|"+delivery+'|'+attempt+'|'+state+'|'+error).equals(event)){p.bootstrap.put("r81_matrix_system_rejected_revision_"+c.getLong(12),"SYSTEM_FAILURE_OWNER_HASH");continue;}
            if(c.isNull(7)){p.status="WAIT_SYSTEM_FAILURE_ORIGINAL_OUTBOX";continue;}
            String target=c.getString(7),candidate=c.getString(8),settlement=c.getString(9),label=c.getString(10);
            // One logical delivery can fail multiple owner attempts. Only its first
            // authoritative infrastructure failure opens the SYSTEM episode.
            try(Cursor first=db.rawQuery("SELECT MIN(rowid) FROM r952_recovery_journal WHERE delivery_hash=? AND state='RETRYABLE_FAILED' AND error_code LIKE 'RETRYABLE_INFRASTRUCTURE|%'",new String[]{delivery})){if(!first.moveToFirst()||first.getLong(0)!=row)continue;}
            if(!TargetIssueResolver.isValid(target)||c.getInt(11)<attempt||!RuntimeEventContractR952.deliveryHash(target,candidate,settlement,label).equals(delivery)){p.bootstrap.put("r81_matrix_system_rejected_revision_"+c.getLong(12),"SYSTEM_FAILURE_OWNER_IDENTITY");continue;}
            p.fences.add(new Fence("SELECT 1 FROM r952_research_outbox WHERE delivery_hash=? AND target_issue=? AND candidate_id=? AND settlement_hash=? AND label_hash=? AND attempt_count>=?",delivery,target,candidate,settlement,label,Integer.toString(attempt)));
            String receipt=MatrixProjectionCP12.hash("R81_SYSTEM_TRAIN_EPISODE|"+delivery+'|'+target);p.systemFailures.add(new String[]{target,receipt});
        }}
        if(scanned==0)p.status="WAIT_SYSTEM_FAILURE_OWNER_EVENT";else if(p.systemFailures.isEmpty()&&p.status.equals("WAIT_SOURCE"))p.status="SYSTEM_JOURNAL_SCANNED_NO_NEW_SYSTEM_EPISODE";
    }
    private static void addSlice(Page p,List<MatrixProjectionCP12.Spec> all,int offset){for(int i=offset;i<all.size()&&p.specs.size()<BATCH;i++)p.specs.add(all.get(i));}
    private static String[] split(String value){if(value.isEmpty())return new String[]{"","0"};String[] s=value.split("~",-1);if(s.length!=2)throw new IllegalStateException("R75_MATRIX_DISCOVERY_CURSOR");return s;}
    private static String state(SQLiteDatabase db,String key,String fallback){try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}}
    private static void put(SQLiteDatabase db,String key,String value){db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{key,value,System.currentTimeMillis()});}
}
