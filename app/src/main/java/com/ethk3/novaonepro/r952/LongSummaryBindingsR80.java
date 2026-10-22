package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Explicit immutable long-summary source, separate from raw windows and teacher bindings. */
public final class LongSummaryBindingsR80 {
    public static final String FEATURE_REVISION="CP4_FEATURE_LONG_R80_1";
    public static final String CONTEXT_REVISION="SCCONTEXT_LONG_R80_3";
    private LongSummaryBindingsR80(){}
    public static final class Ref {
        public final String nodeId,specHash,epochHash,metric;public final int window,digit;
        public Ref(Map<String,Object> o){
            keys(o,"nodeId","specHash","epochHash","window","metric","digit");nodeId=str(o,"nodeId");specHash=hash(str(o,"specHash"));epochHash=hash(str(o,"epochHash"));metric=str(o,"metric");window=integer(o,"window");digit=integer(o,"digit");
            need(nodeId.matches("n[0-9]{1,3}")&&Integer.parseInt(nodeId.substring(1))<128,"LONG_REF_NODE");need(validWindow(window),"LONG_REF_WINDOW");
            need(metric.equals("FREQUENCY")&&digit>=0&&digit<10||metric.equals("ENTROPY")&&digit==-1,"LONG_REF_METRIC");
        }
        public String node(){return "CP4:"+nodeId;}
        public Map<String,Object> canonicalObject(){return fields("nodeId",nodeId,"specHash",specHash,"epochHash",epochHash,"window",window,"metric",metric,"digit",digit);}
        public String canonical(){return StrictCanonicalJson.write(canonicalObject());}
    }
    public static boolean validWindow(int window){return window==1440||window==10080||window==43200;}
    /** Closed node/spec/reference set; adding a long atom without its manifest is rejected. */
    public static void validate(CandidateAstR952 ast,FeatureManifestR952 manifest,List<AtomSpecCP4> specs){
        Map<String,AtomSpecCP4> byId=new HashMap<>();for(AtomSpecCP4 s:specs)byId.put(s.atomId,s);
        Map<String,Ref> refs=new TreeMap<>();String epoch="";
        for(Ref ref:manifest.summarySpecRefs){need(refs.put(ref.node(),ref)==null,"LONG_REF_DUPLICATE");if(epoch.isEmpty())epoch=ref.epochHash;need(epoch.equals(ref.epochHash),"LONG_REF_MIXED_EPOCHS");}
        Set<String> required=new TreeSet<>();
        for(TypedAtomMoleculeGraphR930.NodeSpec node:ast.graphSpec.nodes){
            if(node.kind!=TypedAtomMoleculeGraphR930.NodeKind.ATOM)continue;
            AtomSpecCP4 atom=byId.get(node.source);need(atom!=null,"LONG_REF_SPEC_MISSING");if(!atom.requiresSummary)continue;required.add(node.qualifiedId());Ref ref=refs.get(node.qualifiedId());
            need(ref!=null&&ref.specHash.equals(atom.specHash)&&ref.epochHash.equals(atom.summaryEpochHash)&&ref.window==atom.summaryWindow&&ref.digit==atom.summaryDigit&&ref.metric.equals(atom.summaryMetric),"LONG_REF_SPEC_IDENTITY");
        }
        need(required.equals(refs.keySet()),"LONG_REF_CLOSURE");need(refs.isEmpty()||FEATURE_REVISION.equals(manifest.revision),"LONG_REF_FEATURE_REVISION");
    }
    private static final class Source {
        final String snapshotHash,epoch,through,canonical,mode,session,receiptHash;final long available,prepared;final long[][][] counts=new long[3][10][10];final int[] support=new int[3];
        private Source(Map<String,Object> wrapper){
            keys(wrapper,"snapshotHash","snapshot","publication");snapshotHash=hash(str(wrapper,"snapshotHash"));Map<String,Object> o=obj(wrapper.get("snapshot"));canonical=StrictCanonicalJson.writeContext(o);need(V51ChecksumEngine.sha256(canonical).equals(snapshotHash),"LONG_SNAPSHOT_HASH");
            keys(o,"revision","game","protocolHash","epochHash","firstIssue","throughIssue","generation","sourceChain","stateHash","availableAt","preparedAt","scales");
            need("SC_LONG_SNAPSHOT_R80_1".equals(str(o,"revision"))&&ReleaseManifestR926.GAME_CODE.equals(str(o,"game"))&&LongScaleSummaryR80.PROTOCOL.equals(str(o,"protocolHash")),"LONG_SNAPSHOT_PROTOCOL");
            epoch=hash(str(o,"epochHash"));through=str(o,"throughIssue");String first=str(o,"firstIssue");hash(str(o,"sourceChain"));hash(str(o,"stateHash"));prepared=number(o,"preparedAt");long sourceAt=number(o,"availableAt");
            need(TargetIssueResolver.isValid(first)&&TargetIssueResolver.isValid(through)&&first.compareTo(through)<=0&&number(o,"generation")>0&&sourceAt>0&&sourceAt<=prepared,"LONG_SNAPSHOT_SOURCE");
            int sequence=TargetIssueResolver.distance(first,through);need(sequence>=0&&sequence<Integer.MAX_VALUE,"LONG_SNAPSHOT_SEQUENCE");
            List<?> scales=list(o.get("scales"));need(scales.size()==3,"LONG_SNAPSHOT_SCALES");int[] windows=LongScaleCountsR80.windows();
            for(int s=0;s<3;s++){
                Map<String,Object> scale=obj(scales.get(s));keys(scale,"window","support","counts");need(integer(scale,"window")==windows[s],"LONG_SNAPSHOT_SCALE_ORDER");support[s]=integer(scale,"support");need(support[s]==Math.min((long)sequence+1,windows[s]),"LONG_SNAPSHOT_SUPPORT");
                List<?> positions=list(scale.get("counts"));need(positions.size()==10,"LONG_SNAPSHOT_POSITION_COUNT");
                for(int p=0;p<10;p++){List<?> bins=list(positions.get(p));need(bins.size()==10,"LONG_SNAPSHOT_BIN_COUNT");long sum=0;for(int d=0;d<10;d++){long n=whole(bins.get(d));need(n>=0&&n<=support[s],"LONG_SNAPSHOT_COUNTER_RANGE");counts[s][p][d]=n;sum+=n;}need(sum==support[s],"LONG_SNAPSHOT_COUNTER_SUM");}
                for(int d=0;d<10;d++){long sum=0;for(int p=0;p<10;p++)sum+=counts[s][p][d];need(sum==support[s],"LONG_SNAPSHOT_PERMUTATION_SUM");}
                if(s>0)for(int p=0;p<10;p++)for(int d=0;d<10;d++)need(counts[s][p][d]>=counts[s-1][p][d],"LONG_SNAPSHOT_NESTED_WINDOWS");
            }
            Map<String,Object> publication=obj(wrapper.get("publication"));keys(publication,"availableAt","mode","clockSession","receiptHash");available=number(publication,"availableAt");mode=str(publication,"mode");session=str(publication,"clockSession");receiptHash=hash(str(publication,"receiptHash"));
            need(available>=prepared&&(mode.equals("ORIGINAL_RETURN")||mode.equals("RECOVERED_AVAILABLE_NOW"))&&!session.isEmpty()&&session.length()<=256,"LONG_PUBLICATION_PROOF");
            need(V51ChecksumEngine.sha256("LONG_AVAILABILITY_R80|"+snapshotHash+'|'+available+'|'+mode+'|'+session).equals(receiptHash),"LONG_PUBLICATION_HASH");
        }
        private Map<String,Object> object(){return fields("snapshotHash",snapshotHash,"snapshot",StrictCanonicalJson.readContextObject(canonical),"publication",fields("availableAt",available,"mode",mode,"clockSession",session,"receiptHash",receiptHash));}
    }
    public static final class Frozen {
        public final Ref ref;private final Source source;
        private Frozen(Ref ref,Source source){this.ref=ref;this.source=source;need(ref.epochHash.equals(source.epoch),"LONG_FROZEN_EPOCH");int s=scale(ref.window);need(source.support[s]==ref.window,"WAIT_LONG_SCALE_HISTORY");}
        public TypedAtomMoleculeGraphR930.Value value(int position){
            need(position>=1&&position<=10,"LONG_POSITION");long[] counts=source.counts[scale(ref.window)][position-1];double value=0;
            if(ref.metric.equals("FREQUENCY"))value=counts[ref.digit]/(double)ref.window;
            else for(long n:counts)if(n>0){double p=n/(double)ref.window;value-=p*Math.log(p);}
            String proof=V51ChecksumEngine.sha256("LONG_VALUE_R80|"+source.snapshotHash+'|'+source.receiptHash+'|'+ref.canonical());
            return TypedAtomMoleculeGraphR930.Value.present(new double[]{value},source.through,source.available,0,proof);
        }
        private Map<String,Object> object(){return fields("ref",ref.canonicalObject(),"source",source.object());}
    }
    private static Source readSource(SQLiteDatabase db,String epoch,String through){
        try(Cursor c=db.rawQuery("SELECT s.snapshot_hash,s.payload_json,s.prepared_at,p.available_at,p.mode,p.clock_session,p.receipt_hash FROM "+LongScaleSummaryR80.SNAPSHOT+" s JOIN "+LongScaleSummaryR80.PUBLICATION+" p ON p.snapshot_hash=s.snapshot_hash WHERE s.epoch_hash=? AND s.through_issue=?",new String[]{epoch,through})){
            if(!c.moveToFirst())throw new DevelopmentProductionCP12.Wait("WAIT_LONG_ORIGINAL_SUMMARY");String json=c.getString(1);need(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=16384,"LONG_SOURCE_BYTES");
            Source source=new Source(fields("snapshotHash",c.getString(0),"snapshot",StrictCanonicalJson.readContextObject(json),"publication",fields("availableAt",c.getLong(3),"mode",c.getString(4),"clockSession",c.getString(5),"receiptHash",c.getString(6))));need(source.epoch.equals(epoch)&&source.through.equals(through)&&source.prepared==c.getLong(2),"LONG_SOURCE_ROW_IDENTITY");return source;
        }
    }
    public static Map<String,Frozen> capture(SQLiteDatabase db,CandidateCompilerR952.Program program,PITSnapshotContractR952 pit,long at){
        Map<String,Frozen> result=new TreeMap<>();Map<String,Source> sources=new HashMap<>();
        for(Ref ref:program.manifest.summarySpecRefs){
            Source source=sources.get(ref.epochHash);if(source==null){need(LongScaleSummaryR80.activeEpoch(db,ref.epochHash),"LONG_SOURCE_EPOCH_INVALID");source=readSource(db,ref.epochHash,pit.lastIssue);sources.put(ref.epochHash,source);}
            Frozen frozen=new Frozen(ref,source);need(source.available<=at&&TargetIssueResolver.exact(source.through,pit.targetIssue).valid,"LONG_SOURCE_PIT");result.put(ref.node(),frozen);
        }return Collections.unmodifiableMap(result);
    }
    public static Map<String,Frozen> historical(SQLiteDatabase db,CandidateCompilerR952.Program program,String target,long drawAt){
        Map<String,Frozen> result=new TreeMap<>();Map<String,Source> sources=new HashMap<>();
        for(Ref ref:program.manifest.summarySpecRefs){
            Source source=sources.get(ref.epochHash);
            if(source==null){
                need(LongScaleSummaryR80.activeEpoch(db,ref.epochHash),"LONG_SOURCE_EPOCH_INVALID");String through="";
                try(Cursor c=db.rawQuery("SELECT through_issue FROM "+LongScaleSummaryR80.SNAPSHOT+" WHERE epoch_hash=? AND through_issue<? ORDER BY through_issue DESC LIMIT 1",new String[]{ref.epochHash,target})){if(c.moveToFirst())through=c.getString(0);}
                if(!TargetIssueResolver.exact(through,target).valid)throw new DevelopmentProductionCP12.Wait("WAIT_LONG_ORIGINAL_SUMMARY");
                source=readSource(db,ref.epochHash,through);if(source.available>=drawAt)throw new DevelopmentProductionCP12.Wait("WAIT_LONG_HISTORICAL_SOURCE_LATE");sources.put(ref.epochHash,source);
            }
            result.put(ref.node(),new Frozen(ref,source));
        }return Collections.unmodifiableMap(result);
    }

    public static void validateFrozen(CandidateCompilerR952.Program program,String target,long at,Map<String,Frozen> frozen,Map<String,TypedAtomMoleculeGraphR930.Value> atoms){
        Set<String> expected=new TreeSet<>();for(Ref ref:program.manifest.summarySpecRefs){expected.add(ref.node());Frozen f=frozen.get(ref.node());need(f!=null&&ref.canonical().equals(f.ref.canonical())&&f.source.available<=at&&TargetIssueResolver.exact(f.source.through,target).valid,"LONG_FROZEN_CLOSURE_OR_PIT");TypedAtomMoleculeGraphR930.Value actual=atoms.get(ref.node()),proved=f.value(program.ast.position);need(actual!=null&&actual.valueHash.equals(proved.valueHash),"LONG_FROZEN_VALUE");}need(frozen.keySet().equals(expected),"LONG_FROZEN_CLOSURE");
    }
    public static Map<String,Object> canonicalObject(Map<String,Frozen> frozen){Map<String,Object> result=new TreeMap<>();for(Map.Entry<String,Frozen> e:frozen.entrySet())result.put(e.getKey(),e.getValue().object());return result;}
    public static Map<String,Frozen> fromCanonicalObject(Object raw){Map<String,Frozen> result=new TreeMap<>();for(Map.Entry<String,Object> e:obj(raw).entrySet()){Map<String,Object> o=obj(e.getValue());keys(o,"ref","source");Frozen f=new Frozen(new Ref(obj(o.get("ref"))),new Source(obj(o.get("source"))));need(e.getKey().equals(f.ref.node()),"LONG_SOURCE_NODE_KEY");result.put(e.getKey(),f);}return Collections.unmodifiableMap(result);}
    public static String currentReason(SQLiteDatabase db,FeatureManifestR952 manifest){for(Ref ref:manifest.summarySpecRefs)if(!LongScaleSummaryR80.activeEpoch(db,ref.epochHash))return "LONG_SOURCE_EPOCH_INVALID";return "";}
    public static String currentReason(SQLiteDatabase db,CandidateAstR952 ast){return currentReason(db,ast,liveTarget(db));}
    public static String currentReason(SQLiteDatabase db,CandidateAstR952 ast,String target){
        try{new SourceWalkR81(db,target,true).walk(ast,0);return "";}catch(IllegalArgumentException|IllegalStateException error){return error.getMessage()==null?"LONG_SOURCE_INTEGRITY":error.getMessage();}
    }
    private static String liveTarget(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){return c.moveToFirst()?TargetIssueResolver.next(c.getString(0)).targetIssue:"";}}
    /** Bound program descendants retain immutable original bindings. Resolve the
     * exact original cohort without granting that cohort authority over a changed
     * AST: derived historical values are authenticated separately by the original
     * receipt replay, never by weakening its AST/manifest hash guard. */
    private static BoundAtomLifecycleR80.Cohort boundProgramR81(SQLiteDatabase db,CandidateAstR952 ast){
        if(ast.graphSpec.manifest.bindingHashes.isEmpty())return null;
        try(Cursor table=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{BoundAtomLifecycleR80.COHORT})){if(!table.moveToFirst())return null;}
        String selected="";int found=0;
        for(String binding:ast.graphSpec.manifest.bindingHashes)try(Cursor row=db.rawQuery("SELECT b.cohort_hash FROM research_atom_binding_v2 b JOIN "+BoundAtomLifecycleR80.COHORT+" c ON c.cohort_hash=b.cohort_hash WHERE b.binding_hash=?",new String[]{binding})){
            if(!row.moveToFirst())continue;String hash=row.getString(0);need(!row.moveToNext(),"LONG_BOUND_BINDING_AMBIGUOUS");if(selected.isEmpty())selected=hash;need(selected.equals(hash),"LONG_BOUND_MIXED_COHORT");found++;
        }
        if(found==0)return null;need(found==ast.graphSpec.manifest.bindingHashes.size(),"LONG_BOUND_BINDING_CLOSURE");
        BoundAtomLifecycleR80.Cohort cohort=BoundAtomLifecycleR80.readR80(db,selected);need(cohort!=null&&cohort.ast.position==ast.position&&cohort.ast.graphSpec.manifest.bindingHashes.containsAll(ast.graphSpec.manifest.bindingHashes),"LONG_BOUND_PROGRAM_IDENTITY");return cohort;
    }
    /** One traversal is shared by live checks, prepared writer fences and frozen
     * source closure. Both expert edges and immutable bound-parent edges count
     * toward the same finite depth/identity limits. A cycle never becomes PASS. */
    private static final class SourceWalkR81 {
        final SQLiteDatabase db;final String target;final boolean requireCurrent;
        final TreeSet<String> epochs=new TreeSet<>();final TreeMap<String,PreparedTeacherR81> teachers=new TreeMap<>();
        final TreeMap<String,BoundAtomLifecycleR80.Cohort> boundPrograms=new TreeMap<>();
        final Set<String> visited=new HashSet<>(),active=new HashSet<>();
        SourceWalkR81(SQLiteDatabase db,String target,boolean requireCurrent){this.db=db;this.target=target;this.requireCurrent=requireCurrent;}
        void walk(CandidateAstR952 ast,int depth){
            if(ast==null||ast.formatVersion!=3)return;need(depth<=3,"LONG_DEPENDENCY_DEPTH");need(!active.contains(ast.structuralHash),"LONG_DEPENDENCY_CYCLE");if(visited.contains(ast.structuralHash))return;need(visited.size()<16,"LONG_DEPENDENCY_DEPTH");visited.add(ast.structuralHash);active.add(ast.structuralHash);
            try{
                for(Ref ref:ast.graphSpec.manifest.summarySpecRefs){if(requireCurrent)need(LongScaleSummaryR80.activeEpoch(db,ref.epochHash),"LONG_SOURCE_EPOCH_INVALID");epochs.add(ref.epochHash);}
                List<CandidateCompilerR952.ExpertRef> refs=new ArrayList<>(ast.graphSpec.experts);
                BoundAtomLifecycleR80.Cohort program=boundProgramR81(db,ast);
                if(program!=null){
                    if(requireCurrent){String reason=BoundAtomLifecycleR80.currentReasonR80(db,program,target);need(reason.isEmpty(),"BOUND_EXPERT_SOURCE_RETIRED:"+reason);}
                    boundPrograms.put(program.hash,program);int parents=0;
                    try(Cursor row=db.rawQuery("SELECT p.freeze_hash,p.model_hash,p.position,f.ast,p.learner_id,p.learner_ast_hash,f.learner_id FROM "+BoundAtomLifecycleR80.PARENT+" p JOIN "+ResearchEvaluationRunR967.ALL+" f ON f.candidate_id=p.candidate_id AND f.structural_hash=p.freeze_hash AND f.model_hash=p.model_hash WHERE p.cohort_hash=? AND length(CAST(f.ast AS BLOB)) BETWEEN 1 AND 32768 ORDER BY p.ordinal LIMIT 3",new String[]{program.hash})){
                        while(row.moveToNext()){need(++parents<=2,"LONG_BOUND_PARENT_COUNT");CandidateAstR952 parent=CandidateAstR952.parse(row.getString(3));need(parent.position==row.getInt(2)&&parent.position==ast.position&&parent.structuralHash.equals(row.getString(5))&&row.getString(4).equals(row.getString(6)),"LONG_BOUND_PARENT_IDENTITY");refs.add(new CandidateCompilerR952.ExpertRef(row.getString(0),row.getString(1),FeatureManifestR952.forAst(parent).hash,parent.position));}
                    }need(parents==2,"LONG_BOUND_PARENT_COUNT");
                }
                for(CandidateCompilerR952.ExpertRef ref:refs){
                    String encoded=null;try(Cursor row=db.rawQuery("SELECT ast FROM "+ResearchEvaluationRunR967.ALL+" WHERE structural_hash=? AND model_hash=? AND length(CAST(ast AS BLOB)) BETWEEN 1 AND 32768",new String[]{ref.freezeHash,ref.modelHash})){if(row.moveToFirst()){encoded=row.getString(0);need(!row.moveToNext(),"LONG_EXPERT_DEPENDENCY_AMBIGUOUS");}}
                    need(encoded!=null,"LONG_EXPERT_DEPENDENCY_MISSING");CandidateAstR952 parent=CandidateAstR952.parse(encoded);need(parent.position==ref.position&&FeatureManifestR952.forAst(parent).hash.equals(ref.featureManifestHash),"LONG_EXPERT_DEPENDENCY_IDENTITY");
                    PreparedTeacherR81 known=teachers.get(ref.freezeHash);if(known!=null)need(known.ref.modelHash.equals(ref.modelHash)&&known.ref.featureManifestHash.equals(ref.featureManifestHash)&&known.ref.position==ref.position,"LONG_EXPERT_DEPENDENCY_IDENTITY");
                    String cohortHash=boundCohortHashR81(db,ref.freezeHash);BoundAtomLifecycleR80.Cohort cohort=cohortHash.isEmpty()?null:BoundAtomLifecycleR80.readR80(db,cohortHash);
                    if(requireCurrent&&!cohortHash.isEmpty()){String reason=BoundAtomLifecycleR80.currentReasonR80(db,cohort,target);need(reason.isEmpty(),"BOUND_EXPERT_SOURCE_RETIRED:"+reason);}
                    need(known!=null||teachers.size()<16,"LONG_DEPENDENCY_DEPTH");teachers.put(ref.freezeHash,new PreparedTeacherR81(ref,parent,cohort));walk(parent,depth+1);
                }
            }finally{active.remove(ast.structuralHash);}
        }
    }
    public static final class PreparedSourcesR81 {
        public final List<String> epochs;private final String target;
        private final List<PreparedTeacherR81> teachers;private final List<BoundAtomLifecycleR80.Cohort> boundPrograms;
        private PreparedSourcesR81(String target,Set<String> epochs,Collection<PreparedTeacherR81> teachers,Collection<BoundAtomLifecycleR80.Cohort> boundPrograms){
            this.target=target;this.epochs=Collections.unmodifiableList(new ArrayList<>(epochs));
            this.teachers=Collections.unmodifiableList(new ArrayList<>(teachers));this.boundPrograms=Collections.unmodifiableList(new ArrayList<>(boundPrograms));
        }
        public void validateAtCommit(SQLiteDatabase db){
            need(db.inTransaction(),"LONG_PREPARED_SOURCE_WRITER");
            for(String epoch:epochs)need(LongScaleSummaryR80.activeEpoch(db,epoch),"LONG_SOURCE_EPOCH_INVALID");
            for(BoundAtomLifecycleR80.Cohort expected:boundPrograms){BoundAtomLifecycleR80.Cohort current=BoundAtomLifecycleR80.readR80(db,expected.hash);need(current!=null&&!"RETIRED".equals(current.state),"BOUND_EXPERT_SOURCE_RETIRED:BOUND_COHORT_RETIRED");need(current.generation==expected.generation&&current.state.equals(expected.state),"LONG_PREPARED_BOUND_GENERATION_CHANGED");String reason=BoundAtomLifecycleR80.currentReasonR80(db,current,target);need(reason.isEmpty(),"BOUND_EXPERT_SOURCE_RETIRED:"+reason);}
            for(PreparedTeacherR81 teacher:teachers){
                try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchEvaluationRunR967.ALL+" WHERE structural_hash=? AND model_hash=? AND ast=?",new String[]{teacher.ref.freezeHash,teacher.ref.modelHash,teacher.ast.canonical})){need(c.moveToFirst(),"LONG_EXPERT_DEPENDENCY_IDENTITY");}
                String current=boundCohortHashR81(db,teacher.ref.freezeHash);
                need(current.equals(teacher.cohort==null?"":teacher.cohort.hash),"LONG_PREPARED_BOUND_RELATION_CHANGED");
                if(teacher.cohort!=null){
                    BoundAtomLifecycleR80.Cohort prepared=teacher.cohort;
                    try(Cursor c=db.rawQuery("SELECT state,generation FROM "+BoundAtomLifecycleR80.COHORT+" WHERE cohort_hash=?",new String[]{prepared.hash})){
                        need(c.moveToFirst(),"BOUND_EXPERT_SOURCE_RETIRED:BOUND_COHORT_RETIRED");
                        need(!"RETIRED".equals(c.getString(0)),"BOUND_EXPERT_SOURCE_RETIRED:BOUND_COHORT_RETIRED");
                        need(prepared.state.equals(c.getString(0))&&prepared.generation==c.getLong(1),"LONG_PREPARED_BOUND_GENERATION_CHANGED");
                    }
                    String reason=BoundAtomLifecycleR80.currentReasonR80(db,prepared,target);
                    need(reason.isEmpty(),"BOUND_EXPERT_SOURCE_RETIRED:"+reason);
                }
            }
        }
    }
    private static final class PreparedTeacherR81 {
        final CandidateCompilerR952.ExpertRef ref;final CandidateAstR952 ast;final BoundAtomLifecycleR80.Cohort cohort;
        PreparedTeacherR81(CandidateCompilerR952.ExpertRef ref,CandidateAstR952 ast,BoundAtomLifecycleR80.Cohort cohort){this.ref=ref;this.ast=ast;this.cohort=cohort;}
    }
    private static String boundCohortHashR81(SQLiteDatabase db,String freeze){
        try(Cursor table=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{BoundAtomLifecycleR80.COHORT})){if(!table.moveToFirst())return "";}
        try(Cursor c=db.rawQuery("SELECT b.cohort_hash FROM "+BoundAtomLifecycleR80.COHORT+" b JOIN "+ResearchEvaluationRunR967.ALL+" f ON f.learner_id=b.child_id WHERE f.structural_hash=? LIMIT 2",new String[]{freeze})){
            if(!c.moveToFirst())return "";String hash=c.getString(0);need(!c.moveToNext(),"BOUND_CHILD_COHORT_AMBIGUOUS");return hash;
        }
    }
    public static PreparedSourcesR81 prepareCurrentSourcesR81(SQLiteDatabase db,CandidateAstR952 ast,String target){
        need(!db.inTransaction()&&TargetIssueResolver.isValid(target),"LONG_PREPARED_SOURCE_INPUT");SourceWalkR81 graph=new SourceWalkR81(db,target,true);graph.walk(ast,0);return new PreparedSourcesR81(target,graph.epochs,graph.teachers.values(),graph.boundPrograms.values());
    }
    /** Prepared closure is installed with the original immutable frozen version.
     * Consumer hot paths inspect this small relation, not repeated AST/model IO. */
    public static void registerFrozenSourcesR81(SQLiteDatabase db,String freeze,List<String> epochs){
        need(db.inTransaction()&&freeze!=null&&freeze.matches("[0-9a-f]{64}")&&epochs!=null&&epochs.size()<=16,"LONG_FROZEN_SOURCE_WRITER");
        String previous=null;for(String epoch:epochs){hash(epoch);need(previous==null||previous.compareTo(epoch)<0,"LONG_PROTOCOL_EPOCH_ORDER");previous=epoch;need(LongScaleSummaryR80.activeEpoch(db,epoch),"LONG_SOURCE_EPOCH_INVALID");
            try(Cursor c=db.rawQuery("SELECT 1 FROM "+LongCandidateLifecycleR81.FROZEN_SOURCE+" WHERE freeze_hash=? AND epoch_hash=?",new String[]{freeze,epoch})){if(!c.moveToFirst())db.execSQL("INSERT INTO "+LongCandidateLifecycleR81.FROZEN_SOURCE+" VALUES(?,?)",new Object[]{freeze,epoch});}
        }
    }
    public static String frozenDependencyReasonR81(SQLiteDatabase db,String freeze){return frozenDependencyReasonR81(db,freeze,liveTarget(db));}
    public static String frozenDependencyReasonR81(SQLiteDatabase db,String freeze,String target){
        boolean installed;try(Cursor table=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{LongCandidateLifecycleR81.FROZEN_SOURCE})){installed=table.moveToFirst();}
        TreeSet<String> cached=new TreeSet<>();int count=0;if(installed)try(Cursor c=db.rawQuery("SELECT s.epoch_hash,h.epoch_hash FROM "+LongCandidateLifecycleR81.FROZEN_SOURCE+" s LEFT JOIN "+LongScaleSummaryR80.HEAD+" h ON h.singleton=1 AND h.epoch_hash=s.epoch_hash AND h.status='ACTIVE' WHERE s.freeze_hash=? LIMIT 17",new String[]{freeze})){
            while(c.moveToNext()){if(++count>16)return "LONG_DEPENDENCY_DEPTH";cached.add(c.getString(0));if(c.isNull(1))return "LONG_SOURCE_EPOCH_INVALID";}
        }
        // A cached epoch is a bounded observation, not a substitute for the
        // original immutable expert and bound-parent dependency identities.
        try(Cursor c=db.rawQuery("SELECT ast FROM "+ResearchEvaluationRunR967.ALL+" WHERE structural_hash=? AND length(CAST(ast AS BLOB)) BETWEEN 1 AND 32768",new String[]{freeze})){
            if(!c.moveToFirst())return "";String encoded=c.getString(0);if(c.moveToNext())return "LONG_EXPERT_DEPENDENCY_AMBIGUOUS";
            try{SourceWalkR81 graph=new SourceWalkR81(db,target,true);graph.walk(CandidateAstR952.parse(encoded),0);if(!cached.equals(graph.epochs))return count==0?"LONG_FROZEN_SOURCE_BINDING_MISSING":"LONG_FROZEN_SOURCE_BINDING_MISMATCH";}
            catch(IllegalArgumentException|IllegalStateException failure){return failure.getMessage()==null?"LONG_SOURCE_INTEGRITY":failure.getMessage();}
        }return "";
    }
    public static void decorateManifest(Map<String,Object> root,List<AtomSpecCP4> specs){
        Map<String,Object> manifest=obj(root.get("featureManifest"));List<Object> refs=refsForGraph(root,specs);
        if(refs.isEmpty()){manifest.put("revision","CP4_FEATURE_1");manifest.remove("summarySpecRefs");}
        else{manifest.put("revision",FEATURE_REVISION);manifest.put("summarySpecRefs",refs);}
    }
    public static List<String> epochsForAst(SQLiteDatabase db,CandidateAstR952 ast){SourceWalkR81 graph=new SourceWalkR81(db,liveTarget(db),false);graph.walk(ast,0);return Collections.unmodifiableList(new ArrayList<>(graph.epochs));}
    /** Used at prepare and again inside the original writer transactions. */
    public static void validateProtocolSources(SQLiteDatabase db,Map<String,Object> protocol){
        if(!protocol.containsKey("summaryEpochHashes"))return;List<?> epochs=list(protocol.get("summaryEpochHashes"));need(!epochs.isEmpty()&&epochs.size()<=16,"LONG_PROTOCOL_EPOCH_COUNT");String previous=null;
        for(Object value:epochs){need(value instanceof String,"LONG_PROTOCOL_EPOCH_TYPE");String epoch=hash((String)value);need(previous==null||previous.compareTo(epoch)<0,"LONG_PROTOCOL_EPOCH_ORDER");previous=epoch;need(LongScaleSummaryR80.activeEpoch(db,epoch),"LONG_SOURCE_EPOCH_INVALID");}
    }
    public static void validateTaskSources(SQLiteDatabase db,ResearchTaskQueueR967.Lease task){
        if(task==null||!("TRAIN_CANDIDATE".equals(task.kind)||"FREEZE_CANDIDATE".equals(task.kind)))return;
        try(Cursor c=db.rawQuery("SELECT protocol_json FROM research_training_protocol_v2 WHERE protocol_hash=? AND candidate_id=? AND input_hash=?",new String[]{task.sequence,task.candidate,task.inputHash})){
            need(c.moveToFirst(),"LONG_TASK_PROTOCOL_MISSING");String json=c.getString(0);need(V51ChecksumEngine.sha256(json).equals(task.sequence),"LONG_TASK_PROTOCOL_HASH");validateProtocolSources(db,StrictCanonicalJson.readContextObject(json));
        }
    }
    public static List<Object> refsForGraph(Map<String,Object> root,List<AtomSpecCP4> specs){
        Map<String,AtomSpecCP4> byId=new HashMap<>();for(AtomSpecCP4 atom:specs)byId.put(atom.atomId,atom);TreeMap<String,Object> refs=new TreeMap<>();
        for(Object raw:list(root.get("nodes"))){Map<String,Object> node=obj(raw);if(!"ATOM".equals(node.get("operator")))continue;AtomSpecCP4 atom=byId.get(str(node,"atomId"));need(atom!=null,"LONG_GRAPH_UNKNOWN_SPEC");if(atom.requiresSummary){Ref ref=new Ref(fields("nodeId",str(node,"id"),"specHash",atom.specHash,"epochHash",atom.summaryEpochHash,"window",atom.summaryWindow,"metric",atom.summaryMetric,"digit",atom.summaryDigit));refs.put(ref.nodeId,ref.canonicalObject());}}
        need(refs.size()<=16,"LONG_REF_COUNT");return new ArrayList<>(refs.values());
    }
    private static int scale(int window){int[] windows=LongScaleCountsR80.windows();for(int i=0;i<windows.length;i++)if(windows[i]==window)return i;throw new IllegalArgumentException("LONG_WINDOW");}
    @SuppressWarnings("unchecked") private static Map<String,Object> obj(Object o){need(o instanceof Map,"LONG_OBJECT");return(Map<String,Object>)o;}
    private static List<?> list(Object o){need(o instanceof List,"LONG_ARRAY");return(List<?>)o;}
    private static Map<String,Object> fields(Object... entries){Map<String,Object> r=new TreeMap<>();for(int i=0;i<entries.length;i+=2)r.put((String)entries[i],entries[i+1]);return r;}
    private static String str(Map<String,Object> o,String key){need(o.get(key) instanceof String,"LONG_STRING:"+key);return(String)o.get(key);}
    private static long whole(Object o){need(o instanceof Integer||o instanceof Long,"LONG_INTEGER");return((Number)o).longValue();}
    private static long number(Map<String,Object> o,String key){return whole(o.get(key));}
    private static int integer(Map<String,Object> o,String key){long n=number(o,key);need(n>=Integer.MIN_VALUE&&n<=Integer.MAX_VALUE,"LONG_INTEGER_RANGE");return(int)n;}
    private static String hash(String value){need(value!=null&&value.matches("[0-9a-f]{64}"),"LONG_HASH");return value;}
    private static void keys(Map<String,Object> o,String... keys){need(o.keySet().equals(new HashSet<>(Arrays.asList(keys))),"LONG_KEYS");}
    private static void need(boolean good,String reason){if(!good)throw new IllegalArgumentException(reason);}
}
