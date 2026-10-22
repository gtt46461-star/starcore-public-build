package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import com.ethk3.novaonepro.r952.FrozenGraphContextCP4.ExpertPrediction;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Production adapter for one durable TRAIN_CANDIDATE task, not a second Owner.
 * Numerical and feature work occur outside coordinator writes; Director owns publication. */
public final class DevelopmentProductionCP12 {
    private DevelopmentProductionCP12() {}
    private static final String ZERO=String.join("",Collections.nCopies(64,"0"));
    // Recomputable, process-local prepared pages. They confer no lease, receipt or
    // training authority. Every publication rechecks the original protocol/prefix
    // inside the existing owner's transaction. A restart uses the durable cursor.
    private static final int FEATURE_PAGE_ROWS_R77=64, FEATURE_PAGE_BYTES_R77=128*1024;
    private static final int PREPARED_PAGES_R77=4;
    private static final LinkedHashMap<String,PreparedFeaturePageR77> PREPARED_FEATURES_R77=new LinkedHashMap<>();
    private static final class PreparedFeaturePageR77 implements AutoCloseable {
        final String task,protocol,input,cursor,path;final FeaturePrefix before;
        final List<FeatureRow> rows=new ArrayList<>();final MemoryBudgetCP4.Lease memory;
        String nextHash;long bytes=512;
        PreparedFeaturePageR77(ResearchTaskQueueR967.Lease lease,String path,FeaturePrefix prefix){
            task=lease.id;protocol=lease.sequence;input=lease.inputHash;cursor=lease.cursor;this.path=path;before=prefix;nextHash=prefix.hash;
            memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(FEATURE_PAGE_BYTES_R77);
        }
        boolean add(FeatureRow row){
            long retained=512L+row.payload.length+2L*(row.issue.length()+row.role.length()+row.hash.length()+row.sourceHash.length()+row.rawHash.length());
            if(rows.size()>=FEATURE_PAGE_ROWS_R77||bytes+retained>memory.bytes)return false;
            rows.add(row);bytes+=retained;nextHash=V51ChecksumEngine.sha256(nextHash+'|'+row.hash);return true;
        }
        boolean matches(ResearchTaskQueueR967.Lease lease,String dbPath){return task.equals(lease.id)&&protocol.equals(lease.sequence)&&input.equals(lease.inputHash)&&cursor.equals(lease.cursor)&&path.equals(dbPath);}
        public void close(){memory.close();}
    }
    private static synchronized PreparedFeaturePageR77 takePreparedFeaturesR77(ResearchTaskQueueR967.Lease lease,String path){
        PreparedFeaturePageR77 page=PREPARED_FEATURES_R77.remove(lease.id);
        if(page!=null&&!page.matches(lease,path)){page.close();return null;}return page;
    }
    private static synchronized void retainPreparedFeaturesR77(PreparedFeaturePageR77 page){
        if(page.rows.isEmpty()){page.close();return;}
        PreparedFeaturePageR77 old=PREPARED_FEATURES_R77.remove(page.task);if(old!=null)old.close();
        while(PREPARED_FEATURES_R77.size()>=PREPARED_PAGES_R77){Iterator<PreparedFeaturePageR77> it=PREPARED_FEATURES_R77.values().iterator();PreparedFeaturePageR77 first=it.next();it.remove();first.close();}
        PREPARED_FEATURES_R77.put(page.task,page);
    }
    private static void publishPreparedFeaturesR77(DbStore store,ResearchTaskQueueR967.Lease lease,Protocol p,PreparedFeaturePageR77 page){
        boolean published=false;
        try{
            final RuntimeException[] failure=new RuntimeException[1];
            Boolean committed=store.coordinatorForResearchR952().runAtomicWrite("CP12_TRAIN_FEATURE_PAGE",tx->{try{return commitFeaturePage(tx,lease,p,page.before,page.rows,page.nextHash);}catch(RuntimeException e){failure[0]=e;throw e;}},false);
            if(!Boolean.TRUE.equals(committed))throw new Wait(ResearchTrainingHookRegistryR952.commitFailureReasonCP12(failure[0],"TRAIN_FEATURE_COMMIT_FAILED"));
            published=true;
        }finally{if(published)page.close();else retainPreparedFeaturesR77(page);}
    }
    public static final class Wait extends RuntimeException {
        public final String reason;public Wait(String reason){super(reason);this.reason=reason;}
        public Wait(String reason,Throwable cause){super(reason,cause);this.reason=reason;}
    }
    public static final class PreparedProtocol implements AutoCloseable {
        public final CandidateAstR952 ast;public final String candidateId,actionId,registeredIssue,inputHash,protocolHash,protocolJson;
        public final String firstIssue,lastIssue,selectionSourceIssue,headerJson,sourceManifestHash;
        public final long bytes,seed,historyGeneration;public final int rows,trainFirst,trainEnd,validationFirst,validationEnd,trainN;
        public final List<TrainingCasCP12.LockedFile> boundFiles;
        private final TrainingCasCP12.LockedFile inputFile;private boolean closed;
        private PreparedProtocol(CandidateAstR952 ast,String candidateId,String actionId,String registeredIssue,
                TrainingCasCP12.LockedFile input,DevelopmentInputCP9.Header header,DevelopmentInputCP9.Input decoded,
                List<TrainingCasCP12.LockedFile> boundFiles,long historyGeneration,String selectionSourceIssue,List<String> summaryEpochs) {
            this.selectionSourceIssue=selectionSourceIssue;this.historyGeneration=historyGeneration;this.ast=ast;this.candidateId=candidateId;this.actionId=actionId;this.registeredIssue=registeredIssue;
            inputFile=input;inputHash=input.hash;bytes=input.bytes;rows=decoded.rowCount();firstIssue=decoded.issue(0);lastIssue=decoded.issue(rows-1);
            headerJson=header.canonical;sourceManifestHash=header.sourceManifestHash;this.boundFiles=Collections.unmodifiableList(new ArrayList<>(boundFiles));
            DevelopmentInputCP9.Split s=decoded.split(ast.graphSpec.manifest.requiredHistory);
            trainFirst=s.trainFirst;trainEnd=s.trainEnd;validationFirst=s.validationFirst;validationEnd=s.validationEnd;trainN=s.trainN;
            String framed=actionId+'|'+inputHash.length()+':'+inputHash+'|'+ast.structuralHash.length()+':'+ast.structuralHash;
            seed=ByteBuffer.wrap(hexBytes(V51ChecksumEngine.sha256(framed))).getLong();
            Map<String,Object> p=new TreeMap<>();p.put("revision","CP12_TRAIN_PROTOCOL_1");p.put("header",header.canonical);
            p.put("inputHash",inputHash);p.put("astHash",ast.structuralHash);p.put("manifestHash",ast.graphSpec.manifest.hash);
            p.put("modelKind",ast.graphSpec.modelKind.name());p.put("inputSelectionPolicy",ast.graphSpec.experts.isEmpty()&&ast.graphSpec.manifest.bindingHashes.isEmpty()&&!MatrixHistoricalDrawCP12.requiresStoredReceiptsCP12(ast,ast.graphSpec.manifest)&&ast.graphSpec.manifest.summarySpecRefs.isEmpty()?"DATA_UP_TO1200":"BOUND_TAIL300");p.put("sourceIssue",selectionSourceIssue);p.put("selectionBoundary",TargetIssueResolver.next(selectionSourceIssue).targetIssue);if(!lastIssue.equals(selectionSourceIssue))p.put("developmentCutoff",lastIssue);p.put("trainFirstIndex",trainFirst);p.put("trainEndIndex",trainEnd);
            p.put("embargoIndex",s.embargo);p.put("validationFirstIndex",validationFirst);p.put("validationEndIndex",validationEnd);
            p.put("trainFirst",decoded.issue(trainFirst));p.put("trainLast",decoded.issue(trainEnd-1));p.put("embargoIssue",decoded.issue(s.embargo));
            p.put("validationFirst",decoded.issue(validationFirst));p.put("validationLast",decoded.issue(validationEnd-1));
            p.put("featureIds",ast.graphSpec.manifest.orderedFeatureIds);p.put("missingPolicies",ast.graphSpec.manifest.missingPolicyPerFeature);
            p.put("seed",seed);p.put("batch",32);p.put("optimizer",ast.graphSpec.modelKind==ModelKind.MLP_D32_16_10?"ADAM_0.001_0.9_0.999_1E-8":ast.graphSpec.modelKind==ModelKind.SOFTMAX_LINEAR||ast.graphSpec.modelKind==ModelKind.OVR_LOGISTIC?"SGD_0.01":"KIND_SPECIFIC_CP9");p.put("epochs",20);p.put("patience",3);
            p.put("treeDepth",4);p.put("baseline","UNIFORM_K3_OF_10");p.put("codeRevision","CP12_WITH_SCCK2");
            List<Object> refs=new ArrayList<>();int boundOrdinal=0;for(TrainingCasCP12.LockedFile f:boundFiles){
                int first=boundOrdinal*HistoricalTrainingReceiptCP12.ROWS_PER_FILE,last=Math.min(trainN+120,first+HistoricalTrainingReceiptCP12.ROWS_PER_FILE)-1;
                int a=first<trainN?trainFirst+first:validationFirst+first-trainN,b=last<trainN?trainFirst+last:validationFirst+last-trainN;
                Map<String,Object> ref=new TreeMap<>();ref.put("hash",f.hash);ref.put("byteCount",f.bytes);ref.put("firstIssue",decoded.issue(a));ref.put("lastIssue",decoded.issue(b));refs.add(ref);boundOrdinal++;
            }p.put("boundFiles",refs);if(!summaryEpochs.isEmpty())p.put("summaryEpochHashes",summaryEpochs);
            protocolJson=StrictCanonicalJson.writeContext(p);need(protocolJson.getBytes(StandardCharsets.UTF_8).length<=65536,"TRAIN_PROTOCOL_SIZE");
            protocolHash=V51ChecksumEngine.sha256(protocolJson);
        }
        public synchronized void requireOpen(){need(!closed,"TRAIN_PROTOCOL_CLOSED");inputFile.requireOpen();for(TrainingCasCP12.LockedFile file:boundFiles)file.requireOpen();}
        public synchronized void close()throws IOException {if(closed)return;closed=true;IOException first=null;
            for(TrainingCasCP12.LockedFile f:boundFiles)try{f.close();}catch(IOException e){if(first==null)first=e;}
            try{inputFile.close();}catch(IOException e){if(first==null)first=e;}if(first!=null)throw first;}
    }
    public static PreparedProtocol prepare(DbStore store,CandidateAstR952 ast,String candidateId,String actionId,String registeredIssue) {
        need(store!=null&&ast!=null&&ast.formatVersion==3&&candidateId!=null&&!candidateId.isEmpty()
                &&actionId!=null&&!actionId.isEmpty()&&TargetIssueResolver.isValid(registeredIssue),"TRAIN_REGISTER_INPUT");
        need(!store.readable().inTransaction(),"TRAIN_PREPARE_OUTSIDE_TRANSACTION");
        MemoryBudgetCP4 budget=RuntimeMemoryGuardR811.algorithmModelBudget();
        List<TrainingCasCP12.LockedFile> attachments=new ArrayList<>();TrainingCasCP12.LockedFile raw=null;
        try(MemoryBudgetCP4.Lease liveGraph=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1L<<20);
            MemoryBudgetCP4.Lease temporary=budget.acquire(1L<<20)) {
            List<AtomSpecCP4> specs=readSpecs(store.readable(),ast);Program program=CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,specs);
            String selectionBoundary,sourceIssue;
            try(Cursor action=store.readable().rawQuery("SELECT b.source_issue,b.selection_boundary,b.evidence_hash,b.boundary_hash,a.evidence_hash,a.state FROM research_action_v2 a JOIN research_action_input_boundary_cp12 b ON b.action_id=a.action_id WHERE a.action_id=?",new String[]{actionId})){
                need(action.moveToFirst()&&"PROPOSED".equals(action.getString(5))&&TargetIssueResolver.isValid(action.getString(0)),"TRAIN_SELECTION_ACTION_CAS");
                sourceIssue=action.getString(0);selectionBoundary=action.getString(1);need(TargetIssueResolver.next(action.getString(0)).targetIssue.equals(selectionBoundary)&&action.getString(2).equals(action.getString(4)),"TRAIN_SELECTION_BOUNDARY_IDENTITY");
                need(V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|"+actionId+'|'+action.getString(0)+'|'+selectionBoundary+'|'+action.getString(2)).equals(action.getString(3)),"TRAIN_SELECTION_BOUNDARY_HASH");
                need(selectionBoundary.compareTo(registeredIssue)<=0,"TRAIN_SELECTION_FUTURE");
            }
            String developmentCutoff=CalibrationRepairR75.developmentCutoff(store.readable(),actionId,sourceIssue);
            String developmentBoundary=TargetIssueResolver.next(developmentCutoff).targetIssue;
            long historyGeneration=historyGeneration(store.readable());
            boolean bound=!program.manifest.bindingHashes.isEmpty()||!ast.graphSpec.experts.isEmpty()||MatrixHistoricalDrawCP12.requiresStoredReceiptsCP12(program)||!program.manifest.summarySpecRefs.isEmpty();
            BoundAtomLifecycleR80.Cohort originalBound=BoundAtomLifecycleR80.cohortForProgramR80(store.readable(),program);
            int historyLimit=historyReadLimitR72(program.manifest.requiredHistory,bound);
            final List<Models.Draw> history=selectHistoryCP12(originalBound==null?store.getDrawsBeforeIssue(developmentBoundary,historyLimit):BoundAtomLifecycleR80.trainingHistoryR80(store.readable(),originalBound,developmentBoundary,historyLimit),program.manifest.requiredHistory,bound);
            if(!history.get(history.size()-1).issue.equals(developmentCutoff))throw new Wait("STALE_HISTORY");
            if(originalBound==null)verifyDevelopmentHistoryCP12(store.readable(),history,bound); // R80 loader verifies its exact immutable original authority.
            if(historyGeneration(store.readable())!=historyGeneration)throw new Wait("TRAIN_HISTORY_CHANGED");
            if(bound)attachments.addAll(HistoricalTrainingReceiptCP12.publish(store,program,history,registeredIssue,actionId));
            List<Object> bindings=new ArrayList<>();int ordinal=0;long boundBytes=0;
            for(TrainingCasCP12.LockedFile f:attachments){boundBytes+=f.bytes;if(boundBytes>64L<<20)throw new Wait("TRAIN_INPUT_CAPACITY");
                Map<String,Object> b=new TreeMap<>();b.put("kind",String.format(java.util.Locale.ROOT,"BIND_%04d",ordinal++));b.put("hash",f.hash);b.put("byteCount",f.bytes);bindings.add(b);}
            Map<String,Object> header=new TreeMap<>();header.put("revision","SCDEV1");header.put("game","PK10");header.put("build",AppConfig.VERSION_NAME);
            header.put("sourceManifestHash",SourceManifestR815.SOURCE_TREE_SHA256);header.put("scheduleRevision","TARGET_ISSUE_RESOLVER_R980");header.put("bindings",bindings);
            DevelopmentInputCP9.Header h=new DevelopmentInputCP9.Header(StrictCanonicalJson.writeContext(header));
            DevelopmentInputCP9.Receipt identity=DevelopmentInputCP9.write(new OutputStream(){public void write(int b){}public void write(byte[] bytes,int offset,int count){}},h,history);
            raw=TrainingCasCP12.publish(store.researchInputRootCP11(),TrainingCasCP12.INPUT_SUFFIX,identity.inputHash,identity.byteCount,out->DevelopmentInputCP9.write(out,h,history).inputHash);
            try(InputStream in=raw.open();DevelopmentInputCP9.Input decoded=DevelopmentInputCP9.read(in,raw.bytes,raw.hash,h,budget)) {
                PreparedProtocol result=new PreparedProtocol(ast,candidateId,actionId,registeredIssue,raw,h,decoded,attachments,historyGeneration,sourceIssue,LongSummaryBindingsR80.epochsForAst(store.readable(),ast));raw=null;attachments=new ArrayList<>();return result;
            }
        }catch(IOException io){throw new Wait("WAIT_INPUT_IO",io);}finally{
            if(raw!=null)try{raw.close();}catch(IOException ignored){}
            for(TrainingCasCP12.LockedFile f:attachments)try{f.close();}catch(IOException ignored){}
        }
    }
    /** Development labels are authenticated completed draws. A historical label does not
     * require a prediction to have existed before that draw. Bound/teacher features still
     * require original predraw receipts plus their authoritative completed settlement.
     * This method never writes settlement, CAL, Future, certificates or REAL state. */
    static void verifyDevelopmentHistoryCP12(SQLiteDatabase db,List<Models.Draw> history,boolean bound) {
        if(bound) {
            try(Cursor c=db.rawQuery("SELECT d.issue FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue>=? AND d.issue<=? AND d.dirty=0 AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t WHERE t.target_issue=d.issue AND t.status='COMPLETED') LIMIT 1",new String[]{history.get(0).issue,history.get(history.size()-1).issue})) {
                if(c.moveToFirst())throw new Wait("WAIT_AUTHORITATIVE_SETTLEMENT");
            }
        }
        int ordinal=0;
        try(Cursor c=db.rawQuery("SELECT d.issue,c.status,c.canonical_draw_hash FROM "+DbHelper.TABLE_DRAWS+" d LEFT JOIN "+DbHelper.TABLE_R811_DRAW_CANONICAL+" c ON c.target_issue=d.issue WHERE d.issue>=? AND d.issue<=? AND d.dirty=0 ORDER BY d.issue",new String[]{history.get(0).issue,history.get(history.size()-1).issue})) {
            while(c.moveToNext()) {
                if(ordinal>=history.size())throw new Wait("STALE_HISTORY");
                Models.Draw d=history.get(ordinal++);
                if(!d.issue.equals(c.getString(0)) || !"CONFIRMED_CANONICAL_DRAW".equals(c.getString(1))
                        || !(DrawHashR36.sha256V2(d).equals(c.getString(2)) || DrawHashR36.sha256(d).equals(c.getString(2))))
                    throw new Wait("WAIT_AUTHORITATIVE_DRAW");
            }
        }
        if(ordinal!=history.size())throw new Wait("STALE_HISTORY");
    }
    /** Bound families use the minimum300/embargo1/validation120 protocol so a
     * valid recent original-witness window is not rejected for irrelevant old draws. */
    static int historyReadLimitR72(int requiredHistory,boolean bound){
        int history=Math.max(20,requiredHistory);
        return Math.min(5000,history+(bound?421:1621));
    }
    static List<Models.Draw> selectHistoryCP12(List<Models.Draw> loaded,int requiredHistory,boolean bound){
        int required=Math.max(20,requiredHistory)+421;
        if(loaded==null||loaded.size()<required)throw new Wait("WAIT_HISTORY");
        int suffix=loaded.size()-1;
        while(suffix>0 && TargetIssueResolver.exact(loaded.get(suffix-1).issue,loaded.get(suffix).issue).valid) suffix--;
        if(loaded.size()-suffix<required)throw new Wait("WAIT_HISTORY");
        int first=bound?loaded.size()-required:Math.max(suffix,loaded.size()-historyReadLimitR72(requiredHistory,false));
        return Collections.unmodifiableList(new ArrayList<>(loaded.subList(first,loaded.size())));
    }
    private static List<AtomSpecCP4> readSpecs(SQLiteDatabase db,CandidateAstR952 ast) {
        List<AtomSpecCP4> specs=new ArrayList<>();Set<String> seen=new HashSet<>();
        for(NodeSpec node:ast.graphSpec.nodes)if(node.kind==NodeKind.ATOM&&seen.add(node.source)){
            try(Cursor c=db.rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{node.source})){
                if(!c.moveToFirst())throw new Wait("WAIT_ATOM_REGISTRATION");specs.add(AtomSpecCP4.fromRegistry(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getString(5),c.getString(6)));}}
        return specs;
    }
    /** Returns null after an atomically persisted feature page; the dispatcher must not perform a second yield. */
    public static ResearchTrainingHookRegistryR952.DevelopmentBatchCP11 prepareBatch(DbStore store,ResearchTaskQueueR967.Lease lease,long deadlineNs) {
        need(store!=null&&lease!=null&&"TRAIN_CANDIDATE".equals(lease.kind),"TRAIN_PREPARE_TASK");
        SQLiteDatabase db=store.readable();need(!db.inTransaction(),"TRAIN_PREPARE_OUTSIDE_TRANSACTION");
        try(MemoryBudgetCP4.Lease liveGraph=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1L<<20)) {
        Protocol p=readProtocol(db,lease);
        PreparedFeaturePageR77 prepared=takePreparedFeaturesR77(lease,db.getPath());
        if(prepared!=null){publishPreparedFeaturesR77(store,lease,p,prepared);return null;}
        CandidateAstR952 ast=CandidateAstR952.parse(p.ast);
        Program program=CandidateCompilerR952.compile(ast,ast.graphSpec.manifest,readSpecs(db,ast));
        long margin=lease.monotonicUntil-TimeSourceR811.monoNowMs()-50L;if(margin<=0)throw new Wait("TASK_LEASE_EXPIRED");
        long end=Math.min(deadlineNs,System.nanoTime()+margin*1000000L);MemoryBudgetCP4 modelBudget=RuntimeMemoryGuardR811.algorithmModelBudget();
        try(TrainingCasCP12.LockedFile raw=TrainingCasCP12.open(store.researchInputRootCP11(),lease.inputHash,TrainingCasCP12.INPUT_SUFFIX);
            InputStream stream=raw.open();DevelopmentInputCP9.Input input=DevelopmentInputCP9.read(stream,p.bytes,lease.inputHash,new DevelopmentInputCP9.Header(str(p.json,"header")),modelBudget)) {
            DevelopmentInputCP9.Split split=input.split(program.manifest.requiredHistory);need(split.trainN==p.trainN&&input.issue(split.validationEnd-1).equals(p.validationLast),"TRAIN_SPLIT_CAS");
            FeaturePrefix prefix=readPrefix(db,lease,p,split.trainN+120);
            if(prefix.next<split.trainN+120){
                need(prefix.next==0&&lease.cursor.isEmpty()||lease.cursor.equals("FEATURES|"+prefix.next+'|'+prefix.hash),"TRAIN_FEATURE_QUEUE_CURSOR");
                PreparedFeaturePageR77 page=new PreparedFeaturePageR77(lease,db.getPath(),prefix);
                boolean handedOff=false;
                try {
                    // Byte/row bounds protect memory; the existing monotonic end
                    // reserves 75ms for publication. The numerical order is unchanged.
                    try(MemoryBudgetCP4.Lease scratch=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(1L<<20);
                        FeatureAtomHistoryR79 atomHistory=new FeatureAtomHistoryR79(input,program.ast.position,RuntimeMemoryGuardR811.algorithmAtomMatrixBudget())) {
                    for(int offset=0;offset<FEATURE_PAGE_ROWS_R77&&prefix.next+offset<split.trainN+120&&System.nanoTime()+75_000_000L<end;offset++) {
                        ResearchWorkBudgetR927.checkpoint();int index=prefix.next+offset;
                        int targetIndex=index<split.trainN?split.trainFirst+index:split.validationFirst+index-split.trainN;
                        atomHistory.beforeExternalInputs(program);
                        HistoricalTrainingReceiptCP12.Row bound=HistoricalTrainingReceiptCP12.readFor(store,program,p.json,input.issue(targetIndex),input.openTime(targetIndex));
                        Map<String,Value> atoms=dataAtoms(program,input,targetIndex,bound,atomHistory);
                        DevelopmentTrainerCP9.Row row=WorldFeatureComputationR974.rawTrainingRowCP11(program,input,targetIndex,lease.sequence,atoms,bound.experts,bound.hash,RuntimeMemoryGuardR811.algorithmAtomMatrixBudget());
                        if(!page.add(FeatureRow.of(lease,index,index<split.trainN?"TRAIN":"VALIDATION",row,bound.hash)))break;
                    }
                    }
                    if(page.rows.isEmpty())throw new Wait("TIME_BUDGET_EXCEEDED");
                    handedOff=true;publishPreparedFeaturesR77(store,lease,p,page);return null;
                } catch(ResearchWorkBudgetR927.Yield|RuntimeResourceAuditR927.BudgetExceeded preempted) {
                    if(!handedOff){handedOff=true;retainPreparedFeaturesR77(page);}throw preempted;
                } finally {if(!handedOff)page.close();}
            }
        }catch(IOException io){throw new Wait("WAIT_INPUT_IO",io);}
        // Input.close clears its compact arrays, and the raw publication lock has left
        // scope before the first Dataset/optimizer allocation. They are never live together.
        FeaturePrefix prefix=readPrefix(db,lease,p,p.trainN+120);
        int width=program.manifest.orderedFeatureIds.isEmpty()?program.manifest.inputDimension:program.manifest.orderedFeatureIds.size();
        try(MemoryBudgetCP4.Lease dataReservation=modelBudget.acquire(datasetBytesCP12(p.trainN,width))) {
            List<DevelopmentTrainerCP9.Row> rows=readRows(db,lease,p,p.trainN+120);Modern shape=shape(ast,p);
            DevelopmentTrainerCP9.Dataset dataset=new DevelopmentTrainerCP9.Dataset(shape,lease.sequence,lease.inputHash,program.manifest.orderedFeatureIds,program.manifest.missingPolicyPerFeature,rows,p.trainN);
            byte[] checkpoint=null;String oldHash="";long oldGeneration=0;
            try(Cursor c=db.rawQuery("SELECT length(partial_payload),payload_hash,lease_generation,phase,cursor FROM research_task_checkpoint_v2 WHERE task_id=?",new String[]{lease.id})){
                if(c.moveToFirst()){need(c.getLong(0)>0&&c.getLong(0)<=DevelopmentTrainerCP9.MAX_CHECKPOINT,"TRAIN_CHECKPOINT_SIZE");oldHash=c.getString(1);oldGeneration=c.getLong(2);
                    try(Cursor bytes=db.rawQuery("SELECT partial_payload FROM research_task_checkpoint_v2 WHERE task_id=? AND payload_hash=?",new String[]{lease.id,oldHash})){
                        need(bytes.moveToFirst(),"TRAIN_CHECKPOINT_CHANGED");checkpoint=bytes.getBlob(0);}
                    need(TrainingCasCP12.hex(TrainingCasCP12.sha().digest(checkpoint)).equals(oldHash),"TRAIN_CHECKPOINT_HASH");
                }else need(lease.cursor.equals("FEATURES|"+prefix.next+'|'+prefix.hash),"TRAIN_FEATURE_CURSOR_MISSING");
            }
            try(ResearchTrainingHookRegistryR952.PreparedNumericsCP9 n=ResearchTrainingHookRegistryR952.prepareNumericsCP9(dataset,checkpoint,end,modelBudget,lease.cursor,oldHash)) {
                return ResearchTrainingHookRegistryR952.candidateBatchCP11(lease,ast,n,oldHash,oldGeneration,p.validationFirst,p.validationLast);
            }
        }
        }
    }
    /** Before row decoding: conservative object/array/string overhead plus one SQL row
     * and model-shape scratch. Dataset keeps row references; it never duplicates vectors. */
    static long datasetBytesCP12(int trainN,int width){
        need(trainN>=300&&trainN<=1200&&width>=1&&width<=80,"TRAIN_DATA_MEMORY_SHAPE");
        return DevelopmentTrainerCP9.estimatedDatasetBytesCP12(trainN,width);
    }
    private static Map<String,Value> dataAtoms(Program program,DevelopmentInputCP9.Input input,int index,HistoricalTrainingReceiptCP12.Row bound,MemoryBudgetCP4 budget) {
        try(FeatureAtomHistoryR79 history=new FeatureAtomHistoryR79(input,program.ast.position,budget)) {
            return dataAtoms(program,input,index,bound,history);
        }
    }
    /** One page owns one history window. Rows remain strict PIT: append only indices < target.
     * No window is allocated for graphs whose atoms are all supplied by bound/matrix receipts.
     * The window and its sufficient statistics never survive the page or a cooperative yield. */
    static final class FeatureAtomHistoryR79 implements AutoCloseable {
        private final DevelopmentInputCP9.Input input;private final int position;private final MemoryBudgetCP4 budget;
        private AtomWindowCP4 window;private int next=-1,lastIndex=-1;private boolean closed;
        FeatureAtomHistoryR79(DevelopmentInputCP9.Input input,int position,MemoryBudgetCP4 budget){
            this.input=Objects.requireNonNull(input);this.position=position;this.budget=Objects.requireNonNull(budget);
        }
        AtomWindowCP4 before(int index){
            need(!closed&&index>=1&&index<input.rowCount(),"TRAIN_ATOM_HISTORY_RANGE");
            need(index>=lastIndex,"TRAIN_ATOM_HISTORY_ORDER");lastIndex=index;
            if(window==null){window=new AtomWindowCP4(budget);next=Math.max(0,index-AtomWindowCP4.CAPACITY);}
            need(index>=next,"TRAIN_ATOM_HISTORY_ORDER");
            while(next<index){window.append(next,input.actual(next,position)-1);next++;}
            return window;
        }
        // Stored receipts and matrix projections may need their own memory. Keep the
        // original row-local lifetime for mixed graphs rather than guessing that peak.
        void beforeExternalInputs(Program program){if(!program.manifest.bindingHashes.isEmpty()
                ||!program.manifest.matrixSpecRefs.isEmpty()||!program.ast.graphSpec.experts.isEmpty())evict();}
        void afterAtoms(Program program){
            // This is rawTrainingRowCP11's graph reservation, not a new reservation.
            // Evict only when retaining the optional cache would block the next stage.
            long rowBytes=4096;for(NodeSpec node:program.ast.graphSpec.typedGraph.orderedNodes)
                rowBytes+=512L+32L*node.shape.elements();
            if(rowBytes>budget.limit-budget.used())evict();
            beforeExternalInputs(program);
        }
        private void evict(){if(window!=null){window.close();window=null;next=-1;}}
        public void close(){if(!closed){closed=true;evict();}}
    }
    private static Map<String,Value> dataAtoms(Program program,DevelopmentInputCP9.Input input,int index,HistoricalTrainingReceiptCP12.Row bound,FeatureAtomHistoryR79 history) {
        history.beforeExternalInputs(program);
        Map<String,Value> out=new TreeMap<>(bound.atoms);out.putAll(MatrixHistoricalDrawCP12.evaluateCP12(program,input,index));long at=input.openTime(index)-1;String prior=input.issue(index-1);Set<String> matrixNodes=MatrixBindingsCP12.nodeIds(program.ast,program.manifest);
        for(NodeSpec node:program.ast.graphSpec.nodes)if(node.kind==NodeKind.ATOM){if(matrixNodes.contains(node.qualifiedId())){if(!out.containsKey(node.qualifiedId()))throw new Wait("WAIT_BOUND_FEATURE_HISTORY");continue;}AtomSpecCP4 spec=program.specById(node.source);
            if(spec.requiresBinding||spec.requiresSummary){if(!out.containsKey(node.qualifiedId()))throw new Wait("WAIT_BOUND_FEATURE_HISTORY");continue;}
            AtomEvaluatorCP4.Result r=AtomEvaluatorCP4.evaluate(spec.mathSpec,history.before(index),null,index);
            out.put(node.qualifiedId(),new Value(new double[]{r.present?r.value:0},new boolean[]{r.present},prior,at,0,V51ChecksumEngine.sha256("SCDEV_ATOM|"+input.inputHash+'|'+input.issue(index)+'|'+spec.specHash)));
        }history.afterAtoms(program);return out;
    }
    static final class Protocol {
        final String ast,validationFirst,validationLast;final Map<String,Object> json;final int trainN;final long bytes,seed;
        Protocol(Cursor c){ast=c.getString(0);String encoded=c.getString(1);need(encoded.getBytes(StandardCharsets.UTF_8).length<=65536,"TRAIN_PROTOCOL_SIZE");json=StrictCanonicalJson.readContextObject(encoded);
            validationFirst=c.getString(2);validationLast=c.getString(3);trainN=c.getInt(4);bytes=c.getLong(5);seed=c.getLong(6);}
    }
    private static Protocol readProtocol(SQLiteDatabase db,ResearchTaskQueueR967.Lease lease) {
        try(Cursor c=db.rawQuery("SELECT c.ast,p.protocol_json,p.validation_first,p.validation_last,p.train_n,i.byte_count,p.seed,p.ast_hash,p.feature_manifest_hash,c.structural_hash,i.relative_path FROM research_training_protocol_v2 p JOIN r952_candidate c ON c.candidate_id=p.candidate_id JOIN research_training_input_v2 i ON i.input_hash=p.input_hash WHERE p.protocol_hash=? AND p.candidate_id=? AND p.input_hash=?",new String[]{lease.sequence,lease.candidate,lease.inputHash})){
            need(c.moveToFirst()&&c.getString(7).equals(c.getString(9))&&c.getString(10).equals(lease.inputHash+TrainingCasCP12.INPUT_SUFFIX),"TRAIN_PROTOCOL_CAS");Protocol p=new Protocol(c);
            need(V51ChecksumEngine.sha256(StrictCanonicalJson.writeContext(p.json)).equals(lease.sequence)&&str(p.json,"inputHash").equals(lease.inputHash),"TRAIN_PROTOCOL_HASH");LongSummaryBindingsR80.validateProtocolSources(db,p.json);return p;}
    }
    private static Modern shape(CandidateAstR952 ast,Protocol p) {
        FeatureManifestR952 manifest=ast.graphSpec.manifest;int f=manifest.orderedFeatureIds.size(),e=ast.graphSpec.experts.size(),d=manifest.inputDimension;
        double[] std=new double[f];Arrays.fill(std,1);int[] tree=ast.graphSpec.modelKind==ModelKind.HISTOGRAM_TREE?new int[]{-1}:new int[0];int[] child=tree.length==0?new int[0]:new int[]{-1};double[] probabilities=new double[tree.length*10];if(tree.length>0)Arrays.fill(probabilities,.1);
        return new Modern(ast.graphSpec.modelKind,ast.family,d,f,e,p.trainN,p.seed,p.validationLast,manifest.hash,Modern.normalizerHash(manifest.orderedFeatureIds,new double[f],std),manifest.graphHash,ZERO,new double[f],std,new double[Modern.expectedWeights(ast.graphSpec.modelKind,d,f,e)],tree,new double[tree.length],child,child,probabilities,ast.graphSpec.experts);
    }
    static final class FeaturePrefix {final int next;final String hash;FeaturePrefix(int n,String h){next=n;hash=h;}}
    private static FeaturePrefix readPrefix(SQLiteDatabase db,ResearchTaskQueueR967.Lease lease,Protocol p,int expectedN) {
        int next=0;String hash=V51ChecksumEngine.sha256("CP12_FEATURE_PREFIX|"+lease.id+'|'+lease.sequence+'|'+lease.inputHash);
        try(Cursor c=db.rawQuery("SELECT row_index,feature_hash FROM research_training_feature_row_cp11 WHERE task_id=? ORDER BY row_index",new String[]{lease.id})){
            while(c.moveToNext()){need(c.getInt(0)==next&&next<expectedN,"TRAIN_FEATURE_NONCONTIGUOUS");hash=V51ChecksumEngine.sha256(hash+'|'+c.getString(1));next++;}}
        try(Cursor c=db.rawQuery("SELECT next_index,prefix_hash,protocol_hash,input_hash FROM research_training_feature_cursor_cp12 WHERE task_id=?",new String[]{lease.id})){
            if(c.moveToFirst())need(c.getInt(0)==next&&hash.equals(c.getString(1))&&lease.sequence.equals(c.getString(2))&&lease.inputHash.equals(c.getString(3)),"TRAIN_FEATURE_PREFIX_CAS");else need(next==0,"TRAIN_FEATURE_CURSOR_MISSING");}
        return new FeaturePrefix(next,hash);
    }
    /** The preceding full prefix was verified outside the writer. While the task is
     * active, the three exact guards below prevent UPDATE, REPLACE and DELETE.
     * Therefore an intervening insert can only extend MAX(row_index), or collide
     * with the existing primary key. Check that tail and the durable cursor under
     * the same writer transaction and lease. Unknown schema uses the full scan. */
    private static FeaturePrefix readCommitPrefixR80(SQLiteDatabase db,ResearchTaskQueueR967.Lease lease,Protocol p) {
        if(!featureGuardsR80(db))return readPrefix(db,lease,p,p.trainN+120);
        int next=0;String hash=V51ChecksumEngine.sha256("CP12_FEATURE_PREFIX|"+lease.id+'|'+lease.sequence+'|'+lease.inputHash);
        try(Cursor c=db.rawQuery("SELECT next_index,prefix_hash,protocol_hash,input_hash FROM research_training_feature_cursor_cp12 WHERE task_id=?",new String[]{lease.id})) {
            if(c.moveToFirst()) {
                next=c.getInt(0);hash=c.getString(1);
                need(next>=0&&next<=p.trainN+120&&hash!=null&&hash.matches("[0-9a-f]{64}")
                    &&lease.sequence.equals(c.getString(2))&&lease.inputHash.equals(c.getString(3)),"TRAIN_FEATURE_PREFIX_CAS");
            }
        }
        try(Cursor c=db.rawQuery("SELECT MAX(row_index) FROM research_training_feature_row_cp11 WHERE task_id=?",new String[]{lease.id})) {
            need(c.moveToFirst()&&(c.isNull(0)?-1:c.getInt(0))==next-1,"TRAIN_FEATURE_PREFIX_CAS");
        }
        return new FeaturePrefix(next,hash);
    }
    private static String guardSqlR80(String sql) {
        return sql==null?"":sql.replace("IF NOT EXISTS ","").replaceAll("\\s+"," ").trim().replaceAll(";+$","");
    }
    private static boolean featureGuardsR80(SQLiteDatabase db) {
        Map<String,String> expected=new HashMap<>();
        expected.put("cp12_feature_no_update","CREATE TRIGGER cp12_feature_no_update BEFORE UPDATE ON research_training_feature_row_cp11 BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_IMMUTABLE'); END");
        expected.put("cp12_feature_no_replace","CREATE TRIGGER cp12_feature_no_replace BEFORE INSERT ON research_training_feature_row_cp11 WHEN EXISTS(SELECT 1 FROM research_training_feature_row_cp11 WHERE task_id=NEW.task_id AND (row_index=NEW.row_index OR target_issue=NEW.target_issue)) BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_REPLACE_FORBIDDEN'); END");
        expected.put("cp12_feature_active_delete","CREATE TRIGGER cp12_feature_active_delete BEFORE DELETE ON research_training_feature_row_cp11 WHEN EXISTS(SELECT 1 FROM v57_research_task_r967 WHERE task_id=OLD.task_id AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL')) BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_FEATURE_PROTECTED'); END");
        int found=0;
        try(Cursor c=db.rawQuery("SELECT name,sql FROM sqlite_master WHERE type='trigger' AND name IN ('cp12_feature_no_update','cp12_feature_no_replace','cp12_feature_active_delete')",null)) {
            while(c.moveToNext()) {
                String contract=expected.get(c.getString(0));
                if(contract==null||!guardSqlR80(contract).equals(guardSqlR80(c.getString(1))))return false;
                found++;
            }
        }
        return found==3;
    }
    private static boolean commitFeaturePage(SQLiteDatabase db,ResearchTaskQueueR967.Lease lease,Protocol p,FeaturePrefix before,List<FeatureRow> rows,String nextHash) {
        need(db.inTransaction()&&ResearchTaskQueueR967.owns(db,lease),"TRAIN_FEATURE_LEASE");readProtocol(db,lease);
        FeaturePrefix current=readCommitPrefixR80(db,lease,p);need(current.next==before.next&&current.hash.equals(before.hash),"TRAIN_FEATURE_PREDECESSOR_CAS");
        int next=before.next;for(FeatureRow f:rows){need(f.index==next++,"TRAIN_FEATURE_PAGE_ORDER");
            db.execSQL("INSERT INTO research_training_feature_row_cp11(task_id,row_index,protocol_hash,input_hash,target_issue,role,actual_number,feature_payload,feature_hash,source_receipt_hash,raw_row_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{lease.id,f.index,lease.sequence,lease.inputHash,f.issue,f.role,f.actual,f.payload,f.hash,f.sourceHash,f.rawHash,System.currentTimeMillis()});}
        if(before.next==0)db.execSQL("INSERT INTO research_training_feature_cursor_cp12(task_id,protocol_hash,input_hash,next_index,prefix_hash,updated_at) VALUES(?,?,?,?,?,?)",new Object[]{lease.id,lease.sequence,lease.inputHash,next,nextHash,System.currentTimeMillis()});
        else{db.execSQL("UPDATE research_training_feature_cursor_cp12 SET next_index=?,prefix_hash=?,updated_at=? WHERE task_id=? AND next_index=? AND prefix_hash=?",new Object[]{next,nextHash,System.currentTimeMillis(),lease.id,before.next,before.hash});need(changed(db)==1,"TRAIN_FEATURE_CURSOR_CAS");}
        need(ResearchTaskQueueR967.yield(db,lease,"FEATURES|"+next+'|'+nextHash,"TRAIN_FEATURE_PAGE",250),"TRAIN_FEATURE_QUEUE_CAS");return true;
    }
    static final class FeatureRow {
        final int index,actual;final String issue,role,hash,sourceHash,rawHash;final byte[] payload;
        private FeatureRow(int i,int a,String issue,String role,String hash,String source,String raw,byte[] p){index=i;actual=a;this.issue=issue;this.role=role;this.hash=hash;sourceHash=source;rawHash=raw;payload=p;}
        static FeatureRow of(ResearchTaskQueueR967.Lease task,int index,String role,DevelopmentTrainerCP9.Row row,String receipt) {
            byte[] payload=encode(row.valuesCopy(),row.presentCopy());String hash=featureHash(task,index,role,row.issue,row.actual,receipt,row.inputHash,payload);
            return new FeatureRow(index,row.actual,row.issue,role,hash,receipt,row.inputHash,payload);
        }
    }
    private static List<DevelopmentTrainerCP9.Row> readRows(SQLiteDatabase db,ResearchTaskQueueR967.Lease lease,Protocol p,int n) {
        List<DevelopmentTrainerCP9.Row> rows=new ArrayList<>(n);try(Cursor c=db.rawQuery("SELECT row_index,target_issue,role,actual_number,feature_payload,feature_hash,source_receipt_hash,raw_row_hash,length(feature_payload) FROM research_training_feature_row_cp11 WHERE task_id=? ORDER BY row_index",new String[]{lease.id})){
            while(c.moveToNext()){int i=rows.size();need(c.getInt(0)==i&&i<n&&c.getLong(8)>0&&c.getLong(8)<=8192,"TRAIN_FEATURE_ROW_RANGE");
                byte[] bytes=c.getBlob(4);need(featureHash(lease,i,c.getString(2),c.getString(1),c.getInt(3),c.getString(6),c.getString(7),bytes).equals(c.getString(5)),"TRAIN_FEATURE_HASH");
                need(c.getString(2).equals(i<p.trainN?"TRAIN":"VALIDATION"),"TRAIN_FEATURE_ROLE");rows.add(decode(c.getString(1),c.getString(7),c.getInt(3),bytes));}}
        need(rows.size()==n,"TRAIN_FEATURE_MISSING");return rows;
    }
    public static byte[] encode(double[] values,boolean[] present) {
        need(values!=null&&present!=null&&values.length==present.length&&values.length>=1&&values.length<=80,"SCFEAT_SHAPE");
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream(10+values.length*9);DataOutputStream out=new DataOutputStream(bytes);out.writeBytes("SCFEAT1");out.writeShort(values.length);
            for(int i=0;i<values.length;i++){need(Double.isFinite(values[i])&&(present[i]||values[i]==0),"SCFEAT_VALUE");out.writeDouble(values[i]);out.writeByte(present[i]?1:0);}out.flush();return bytes.toByteArray();}catch(IOException e){throw new AssertionError(e);}
    }
    public static DevelopmentTrainerCP9.Row decode(String issue,String rawHash,int actual,byte[] bytes) {
        need(bytes!=null&&bytes.length>=18&&bytes.length<=8192,"SCFEAT_SIZE");try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));byte[] magic=new byte[7];in.readFully(magic);need(Arrays.equals(magic,"SCFEAT1".getBytes(StandardCharsets.US_ASCII)),"SCFEAT_MAGIC");int n=in.readUnsignedShort();need(n>=1&&n<=80&&bytes.length==9+9*n,"SCFEAT_WIDTH");double[] v=new double[n];boolean[] m=new boolean[n];for(int i=0;i<n;i++){v[i]=in.readDouble();int b=in.readUnsignedByte();need(b<=1,"SCFEAT_MASK");m[i]=b==1;}return new DevelopmentTrainerCP9.Row(issue,rawHash,v,m,actual);}catch(IOException bad){throw new IllegalArgumentException("SCFEAT_TRUNCATED",bad);}
    }
    private static String featureHash(ResearchTaskQueueR967.Lease task,int index,String role,String issue,int actual,String receipt,String raw,byte[] payload) {
        return featureHashCP12(task.id,task.sequence,task.inputHash,index,role,issue,actual,receipt,raw,payload);
    }
    /** Same original row wire hash for a read-only diagnostic of a terminal task. */
    static String featureHashCP12(String task,String protocol,String input,int index,String role,String issue,int actual,String receipt,String raw,byte[] payload) {
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);out.writeUTF("CP12_FEATURE_ROW");out.writeUTF(task);out.writeInt(index);out.writeUTF(protocol);out.writeUTF(input);out.writeUTF(issue);out.writeUTF(role);out.writeInt(actual);out.writeUTF(receipt);out.writeUTF(raw);out.writeInt(payload.length);out.write(payload);return TrainingCasCP12.hex(TrainingCasCP12.sha().digest(bytes.toByteArray()));}catch(IOException impossible){throw new AssertionError(impossible);}
    }
    public static long historyGeneration(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='cp12_training_history_generation'",null)){need(c.moveToFirst(),"TRAIN_HISTORY_GENERATION_MISSING");try{return Long.parseLong(c.getString(0));}catch(NumberFormatException bad){throw new IllegalArgumentException("TRAIN_HISTORY_GENERATION",bad);}}}
    static int changed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()?c.getInt(0):0;}}
    static String str(Map<String,Object> object,String key){Object v=object.get(key);need(v instanceof String,"TRAIN_PROTOCOL_STRING:"+key);return (String)v;}
    static byte[] hexBytes(String text){byte[] bytes=new byte[text.length()/2];for(int i=0;i<bytes.length;i++)bytes[i]=(byte)Integer.parseInt(text.substring(2*i,2*i+2),16);return bytes;}
    static void need(boolean ok,String reason){if(!ok)throw new IllegalArgumentException(reason);}

    public static void schema(SQLiteDatabase db) {
        if(db==null||!db.inTransaction())throw new IllegalStateException("TRAIN_SCHEMA_TX_REQUIRED");
        db.execSQL("-- Additive CP12 training preparation; uses the sole existing queue, never a second queue.\nCREATE TABLE IF NOT EXISTS research_training_feature_row_cp11(\n task_id TEXT NOT NULL REFERENCES v57_research_task_r967(task_id),\n row_index INTEGER NOT NULL CHECK(row_index BETWEEN 0 AND 1319),\n protocol_hash TEXT NOT NULL REFERENCES research_training_protocol_v2(protocol_hash),\n input_hash TEXT NOT NULL, target_issue TEXT NOT NULL,\n role TEXT NOT NULL CHECK(role IN('TRAIN','VALIDATION')),\n actual_number INTEGER NOT NULL CHECK(actual_number BETWEEN 1 AND 10),\n feature_payload BLOB NOT NULL CHECK(length(feature_payload) BETWEEN 18 AND 8192),\n feature_hash TEXT NOT NULL CHECK(length(feature_hash)=64),\n source_receipt_hash TEXT NOT NULL CHECK(length(source_receipt_hash)=64),\n raw_row_hash TEXT NOT NULL CHECK(length(raw_row_hash)=64),\n created_at INTEGER NOT NULL,\n PRIMARY KEY(task_id,row_index), UNIQUE(task_id,target_issue));\n");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cp11_feature_protocol ON research_training_feature_row_cp11(protocol_hash,row_index);\n");
        db.execSQL("CREATE TABLE IF NOT EXISTS research_training_feature_cursor_cp12(\n task_id TEXT PRIMARY KEY REFERENCES v57_research_task_r967(task_id),\n protocol_hash TEXT NOT NULL REFERENCES research_training_protocol_v2(protocol_hash),\n input_hash TEXT NOT NULL,next_index INTEGER NOT NULL CHECK(next_index BETWEEN 0 AND 1320),\n prefix_hash TEXT NOT NULL CHECK(length(prefix_hash)=64),updated_at INTEGER NOT NULL);\n");
        db.execSQL("CREATE TABLE IF NOT EXISTS research_training_attachment_cp12(\n attachment_hash TEXT PRIMARY KEY CHECK(length(attachment_hash)=64),\n relative_path TEXT NOT NULL UNIQUE,\n byte_count INTEGER NOT NULL CHECK(byte_count BETWEEN 1 AND 8388608),\n created_at INTEGER NOT NULL);\n");
        db.execSQL("CREATE TABLE IF NOT EXISTS research_training_protocol_attachment_cp12(\n protocol_hash TEXT NOT NULL REFERENCES research_training_protocol_v2(protocol_hash),\n attachment_hash TEXT NOT NULL REFERENCES research_training_attachment_cp12(attachment_hash),\n PRIMARY KEY(protocol_hash,attachment_hash));\n");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_cp12_attachment_protocol ON research_training_protocol_attachment_cp12(attachment_hash,protocol_hash);\n");
        db.execSQL("CREATE TABLE IF NOT EXISTS research_training_registration_row_cp12(\n action_id TEXT NOT NULL REFERENCES research_action_v2(action_id),\n selection_hash TEXT NOT NULL CHECK(length(selection_hash)=64),\n row_index INTEGER NOT NULL CHECK(row_index BETWEEN 0 AND 1319),\n payload TEXT NOT NULL CHECK(length(CAST(payload AS BLOB)) BETWEEN 1 AND 65535),\n payload_hash TEXT NOT NULL CHECK(length(payload_hash)=64),created_at INTEGER NOT NULL,\n PRIMARY KEY(action_id,selection_hash,row_index));\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_feature_no_update BEFORE UPDATE ON research_training_feature_row_cp11\n BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_IMMUTABLE'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_feature_no_replace BEFORE INSERT ON research_training_feature_row_cp11\n WHEN EXISTS(SELECT 1 FROM research_training_feature_row_cp11 WHERE task_id=NEW.task_id AND (row_index=NEW.row_index OR target_issue=NEW.target_issue))\n BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_REPLACE_FORBIDDEN'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_feature_active_delete BEFORE DELETE ON research_training_feature_row_cp11\n WHEN EXISTS(SELECT 1 FROM v57_research_task_r967 WHERE task_id=OLD.task_id AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))\n BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_FEATURE_PROTECTED'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_feature_cursor_identity BEFORE UPDATE OF task_id,protocol_hash,input_hash ON research_training_feature_cursor_cp12\n BEGIN SELECT RAISE(ABORT,'CP12_FEATURE_CURSOR_IDENTITY'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_feature_cursor_active_delete BEFORE DELETE ON research_training_feature_cursor_cp12\n WHEN EXISTS(SELECT 1 FROM v57_research_task_r967 WHERE task_id=OLD.task_id AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))\n BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_FEATURE_CURSOR_PROTECTED'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_registration_no_update BEFORE UPDATE ON research_training_registration_row_cp12\n BEGIN SELECT RAISE(ABORT,'CP12_REGISTRATION_RECEIPT_IMMUTABLE'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_registration_no_replace BEFORE INSERT ON research_training_registration_row_cp12\n WHEN EXISTS(SELECT 1 FROM research_training_registration_row_cp12 WHERE action_id=NEW.action_id AND selection_hash=NEW.selection_hash AND row_index=NEW.row_index)\n BEGIN SELECT RAISE(ABORT,'CP12_REGISTRATION_REPLACE_FORBIDDEN'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_registration_active_delete BEFORE DELETE ON research_training_registration_row_cp12\n WHEN EXISTS(SELECT 1 FROM research_action_v2 WHERE action_id=OLD.action_id AND state='PROPOSED')\n BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_REGISTRATION_PROTECTED'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_attachment_no_update BEFORE UPDATE ON research_training_attachment_cp12\n BEGIN SELECT RAISE(ABORT,'CP12_ATTACHMENT_IMMUTABLE'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_attachment_no_replace BEFORE INSERT ON research_training_attachment_cp12\n WHEN EXISTS(SELECT 1 FROM research_training_attachment_cp12 WHERE attachment_hash=NEW.attachment_hash OR relative_path=NEW.relative_path)\n BEGIN SELECT RAISE(ABORT,'CP12_ATTACHMENT_REPLACE_FORBIDDEN'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_protocol_attachment_no_update BEFORE UPDATE ON research_training_protocol_attachment_cp12\n BEGIN SELECT RAISE(ABORT,'CP12_PROTOCOL_ATTACHMENT_IMMUTABLE'); END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_attachment_active_delete BEFORE DELETE ON research_training_attachment_cp12\n WHEN EXISTS(SELECT 1 FROM research_training_protocol_attachment_cp12 p JOIN research_training_protocol_v2 t ON t.protocol_hash=p.protocol_hash JOIN v57_research_task_r967 q ON q.candidate_id=t.candidate_id WHERE p.attachment_hash=OLD.attachment_hash AND q.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))\n BEGIN SELECT RAISE(ABORT,'CP12_ACTIVE_ATTACHMENT_PROTECTED'); END;\n");
        db.execSQL("\n-- This token detects source changes between bounded raw-history capture and candidate commit.\nINSERT OR IGNORE INTO app_state(state_key,state_value,updated_at) VALUES('cp12_training_history_generation','0',0);\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_training_draw_generation_insert AFTER INSERT ON draws\n BEGIN UPDATE app_state SET state_value=CAST(state_value AS INTEGER)+1,updated_at=0 WHERE state_key='cp12_training_history_generation'; END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_training_draw_generation_update AFTER UPDATE ON draws\n BEGIN UPDATE app_state SET state_value=CAST(state_value AS INTEGER)+1,updated_at=0 WHERE state_key='cp12_training_history_generation'; END;\n");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS cp12_training_draw_generation_delete AFTER DELETE ON draws\n BEGIN UPDATE app_state SET state_value=CAST(state_value AS INTEGER)+1,updated_at=0 WHERE state_key='cp12_training_history_generation'; END;\n");
    }
}
