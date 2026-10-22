package com.ethk3.novaonepro.r952;
import android.database.sqlite.SQLiteDatabase;
/** R967 two-phase adapter for the currently installed single numeric consumer. */
public final class ResearchTrainingHookRegistryR952 {
    private ResearchTrainingHookRegistryR952(){}
    public static final class PreparedBatch {
        private final String delivery;
        private final ResearchDirectorR952.PreparedTraining update;
        private PreparedBatch(String delivery,ResearchDirectorR952.PreparedTraining update){this.delivery=delivery;this.update=update;}
    }
    public static PreparedBatch prepare(SQLiteDatabase db,ResearchOutboxR952.Event event){
        if(db==null||event==null||db.inTransaction())throw new IllegalArgumentException("R967_HOOK_PREPARE_CONTRACT");
        return new PreparedBatch(event.deliveryHash,ResearchDirectorR952.prepareTrainingLabel(db,event));
    }
    public static void commit(SQLiteDatabase db,ResearchOutboxR952.Event event,PreparedBatch batch){
        if(db==null||event==null||batch==null||!db.inTransaction()||!batch.delivery.equals(event.deliveryHash))
            throw new IllegalArgumentException("R967_HOOK_COMMIT_CONTRACT");
        ResearchDirectorR952.commitTrainingLabel(db,event,batch.update);
    }

    /** CP9 pure numerical preparation; original online label prepare/commit above is unchanged.
     * Production prepareCandidateBatchCP11 prepares protocol/PIT/CAS inputs before this call;
     * ResearchDirectorR952 owns the atomic development commit after preparation.
     * This method never marks a task complete. */
    public static final class PreparedNumericsCP9 implements AutoCloseable {
        public final DevelopmentTrainerCP9.Dataset dataset;
        private final DevelopmentTrainerCP9.State state;
        public final String checkpointHash;
        private final byte[] bytes;
        private final MemoryBudgetCP4.Lease reservation;
        private boolean closed;
        private PreparedNumericsCP9(DevelopmentTrainerCP9.Dataset d,DevelopmentTrainerCP9.State s,
                byte[] checkpoint,MemoryBudgetCP4.Lease reservation) {
            dataset=d;state=s;bytes=checkpoint;this.reservation=reservation;
            try {byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(bytes);
                StringBuilder out=new StringBuilder(64);for(byte b:digest)out.append(Character.forDigit((b&255)>>>4,16)).append(Character.forDigit(b&15,16));checkpointHash=out.toString();
            }catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
        }
        public synchronized boolean complete(){requireOpen();return state.complete();}
        public synchronized int epochsExecuted(){requireOpen();return state.executedEpochs();}
        public synchronized int selectedEpoch(){requireOpen();return state.selectedEpoch();}
        public synchronized long optimizerSteps(){requireOpen();return state.optimizerSteps();}
        public synchronized double validationCE(){requireOpen();return state.validationCE();}
        public synchronized double validationBrier(){requireOpen();return state.validationBrier();}
        public synchronized int validationHits(){requireOpen();return state.validationHits();}
        public synchronized String orderedPredictionHash(){requireOpen();return state.orderedPredictionHash();}
        public synchronized byte[] checkpoint(){requireOpen();return bytes.clone();}
        public synchronized String queueCursor(){requireOpen();return state.phase().name()+"|"+state.epoch()+"|"+state.rowCursor()+"|"+checkpointHash;}
        public synchronized CandidateCompilerR952.Model model(String receiptHash){requireOpen();return DevelopmentTrainerCP9.finish(dataset,state,receiptHash);}
        private void requireOpen(){if(closed)throw new IllegalStateException("TRAIN_SLICE_CLOSED");}
        public synchronized void close(){if(!closed){closed=true;reservation.close();}}
    }
    public static PreparedNumericsCP9 prepareNumericsCP9(DevelopmentTrainerCP9.Dataset dataset,
            byte[] previousCheckpoint,long deadlineNs,MemoryBudgetCP4 modelBudget) {
        return prepareNumericsCP9(dataset,previousCheckpoint,deadlineNs,modelBudget,null,null);
    }
    static PreparedNumericsCP9 prepareNumericsCP9(DevelopmentTrainerCP9.Dataset dataset,
            byte[] previousCheckpoint,long deadlineNs,MemoryBudgetCP4 modelBudget,String expectedCursor,String expectedHash) {
        if(dataset==null||modelBudget==null)throw new IllegalArgumentException("TRAIN_NUMERICAL_INPUT");
        // State/optimizer, restore scratch, bounded encoder/result copies and model codec.
        // The production caller has closed SCDEV before retaining Dataset rows.
        MemoryBudgetCP4.Lease reservation=modelBudget.acquire(DevelopmentTrainerCP9.estimatedNumericsBytesCP12(dataset));
        try {
            DevelopmentTrainerCP9.State state=previousCheckpoint==null?DevelopmentTrainerCP9.start(dataset):DevelopmentTrainerCP9.restore(dataset,previousCheckpoint);
            if(previousCheckpoint!=null&&expectedCursor!=null&&!expectedCursor.equals(state.phase().name()+"|"+state.epoch()+"|"+state.rowCursor()+"|"+expectedHash))throw new IllegalArgumentException("TRAIN_CHECKPOINT_QUEUE_CURSOR");
            while(!state.complete()&&System.nanoTime()<deadlineNs) {
                try {com.ethk3.novaonepro.ResearchWorkBudgetR927.checkpoint();}
                catch(com.ethk3.novaonepro.ResearchWorkBudgetR927.Yield yielded){break;}
                DevelopmentTrainerCP9.step(dataset,state);
            }
            byte[] bytes=DevelopmentTrainerCP9.checkpoint(dataset,state);
            return new PreparedNumericsCP9(dataset,state,bytes,reservation);
        }catch(RuntimeException|Error e){reservation.close();throw e;}
    }

    /** Immutable writer input. Created from the validated numerical kernel, never by UI. */
    public static final class DevelopmentBatchCP11 {
        public final String taskId,inputHash,protocolHash,astHash,manifestHash;
        public final String oldCheckpointHash,newCheckpointHash,phase,queueCursor;
        public final long oldCheckpointGeneration,trainN;
        public final int rowCursor;
        public final boolean complete;
        public final String modelPayload,modelHash,receiptJson,receiptHash,validationJson,validationHash;
        public final String validationFirst,validationLast;
        private final byte[] bytes;
        private DevelopmentBatchCP11(com.ethk3.novaonepro.ResearchTaskQueueR967.Lease task,
                CandidateAstR952 ast,PreparedNumericsCP9 n,String previousHash,long previousGeneration,
                String firstValidation,String lastValidation) {
            if(task==null||ast==null||n==null||previousHash==null
                    ||(!previousHash.isEmpty()&&!previousHash.matches("[0-9a-f]{64}"))
                    ||previousGeneration<0||previousGeneration>task.generation||ast.formatVersion!=3
                    ||!"TRAIN_CANDIDATE".equals(task.kind)||!n.dataset.protocolHash.equals(task.sequence)
                    ||!n.dataset.inputHash.equals(task.inputHash)
                    ||!n.dataset.shape.trainedUntil.equals(lastValidation)
                    ||!n.dataset.shape.featureManifestHash.equals(ast.graphSpec.manifest.hash)
                    ||!n.dataset.shape.graphHash.equals(ast.graphSpec.manifest.graphHash))
                throw new IllegalArgumentException("TRAIN_BATCH_IDENTITY");
            taskId=task.id;inputHash=task.inputHash;protocolHash=task.sequence;astHash=ast.structuralHash;
            manifestHash=ast.graphSpec.manifest.hash;oldCheckpointHash=previousHash;
            oldCheckpointGeneration=previousGeneration;bytes=n.checkpoint();newCheckpointHash=n.checkpointHash;
            phase=n.state.phase().name();rowCursor=n.state.rowCursor();queueCursor=n.queueCursor();
            complete=n.complete();trainN=n.dataset.trainN;validationFirst=firstValidation;validationLast=lastValidation;
            if(!complete){modelPayload=receiptJson=receiptHash=validationJson=validationHash="";modelHash=newCheckpointHash;return;}
            CandidateCompilerR952.Model provisional=n.model(String.join("",java.util.Collections.nCopies(64,"0")));
            if(provisional.modern().kind!=ast.graphSpec.modelKind||provisional.modern().family!=ast.family)
                throw new IllegalArgumentException("TRAIN_BATCH_MODEL_AST");
            String weights=com.ethk3.novaonepro.FrozenCandidateAdapterR958.developmentWeightsHashCP8(provisional);
            java.util.Map<String,Object> v=new java.util.TreeMap<>();
            v.put("revision","CP8_DEV_VALIDATION_1");v.put("rowCount",120);v.put("firstIssue",firstValidation);
            v.put("lastIssue",lastValidation);v.put("orderedPredictionHash",n.orderedPredictionHash());
            v.put("ceMean",Double.toHexString(n.validationCE()));v.put("brierMean",Double.toHexString(n.validationBrier()));
            v.put("hitCount",n.validationHits());v.put("allFinite",true);
            v.put("normalizerHash",provisional.modern().normalizerHash);v.put("epochSelected",n.selectedEpoch()+1); /* numerical epoch is zero-based; frozen receipt is one-based */v.put("weightsHash",weights);
            validationJson=StrictCanonicalJson.writeContext(v);validationHash=com.ethk3.novaonepro.V51ChecksumEngine.sha256(validationJson);
            java.util.Map<String,Object> r=new java.util.TreeMap<>();
            r.put("revision","CP8_DEV_RECEIPT_1");r.put("protocolHash",protocolHash);r.put("inputHash",inputHash);
            r.put("astHash",astHash);r.put("manifestHash",manifestHash);r.put("kind",provisional.modern().kind.name());
            r.put("normalizerHash",provisional.modern().normalizerHash);r.put("trainN",trainN);r.put("validationN",120);
            r.put("validationLast",lastValidation);r.put("epochsExecuted",n.epochsExecuted());r.put("optimizerSteps",n.optimizerSteps());
            r.put("weightsHash",weights);r.put("validationHash",validationHash);
            receiptJson=StrictCanonicalJson.writeContext(r);receiptHash=com.ethk3.novaonepro.V51ChecksumEngine.sha256(receiptJson);
            CandidateCompilerR952.Model model=n.model(receiptHash);modelPayload=model.payload();modelHash=model.hash();
            if(!Double.isFinite(n.validationCE())||!Double.isFinite(n.validationBrier())
                    ||receiptJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>8192
                    ||validationJson.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>16384)
                throw new IllegalStateException("TRAIN_BATCH_VALIDATION");
        }
        public byte[] checkpoint(){return bytes.clone();}
    }
    public static DevelopmentBatchCP11 candidateBatchCP11(
            com.ethk3.novaonepro.ResearchTaskQueueR967.Lease task,CandidateAstR952 ast,
            PreparedNumericsCP9 numerics,String oldHash,long oldGeneration,
            String firstValidation,String lastValidation) {
        return new DevelopmentBatchCP11(task,ast,numerics,oldHash,oldGeneration,firstValidation,lastValidation);
    }
    /** The same research executor serves all kinds; this persistent wheel prevents starvation. */
    public static boolean drainTrainingSlotCP11(com.ethk3.novaonepro.DbStore store,long endNs) {
        if(store==null)return false;
        boolean advanced=false;int empty=0;
        // Spend the bounded TRAIN quantum on independent durable units. Keep the
        // persisted three-owner rotation so a legacy backlog cannot starve AST3/freezing.
        for(int turn=0;turn<12&&System.nanoTime()+50_000_000L<endNs;turn++){
            com.ethk3.novaonepro.ResearchWorkBudgetR927.checkpoint();
            String kind=advanceTrainingSlotCP11(store,endNs);if(kind==null)return advanced;
            if(drainTrainingKindR65(store,kind,endNs)){advanced=true;empty=0;}
            else if(++empty>=3)break;
        }
        return advanced;
    }
    private static boolean drainTrainingKindR65(com.ethk3.novaonepro.DbStore store,String kind,long endNs){
        if(System.nanoTime()>=endNs)return false;
        if("LEGACY_OUTBOX".equals(kind))return ResearchOutboxR952.drainOne(store);
        com.ethk3.novaonepro.ResearchTaskQueueR967.Lease lease=store.coordinatorForResearchR952().runAtomicWrite(
                "CP12_CLAIM_"+kind,db->com.ethk3.novaonepro.ResearchTaskQueueR967.claim(db,kind,System.currentTimeMillis()),null);
        if(lease==null)return false;
        try {
            if("TRAIN_CANDIDATE".equals(kind)) {
                DevelopmentBatchCP11 batch=prepareCandidateBatchCP11(store,lease,endNs);
                if(batch==null)return true; // feature page and queue yield already committed together.
                final RuntimeException[] writeFailure=new RuntimeException[1];
                String result=store.coordinatorForResearchR952().runAtomicWrite("CP12_TRAIN_COMMIT",db->{try{return ResearchDirectorR952.commitCandidateBatchCP11(db,lease,batch);}catch(RuntimeException e){writeFailure[0]=e;throw e;}},null);
                if(result==null)throw new DevelopmentProductionCP12.Wait(commitFailureReasonCP12(writeFailure[0],"TRAIN_COMMIT_FAILED"));return true;
            }
            String latest=store.getLatestDrawIssue();if(!com.ethk3.novaonepro.TargetIssueResolver.isValid(latest))throw new DevelopmentProductionCP12.Wait("WAIT_HISTORY");
            String target=com.ethk3.novaonepro.TargetIssueResolver.next(latest).targetIssue;
            try(com.ethk3.novaonepro.FrozenCandidateAdapterR958.PreparedFreezeCP8 prepared=
                    com.ethk3.novaonepro.FrozenCandidateAdapterR958.prepareDevelopmentFreezeCP8(store,lease,target)) {
                final RuntimeException[] writeFailure=new RuntimeException[1];
                String frozen=store.coordinatorForResearchR952().runAtomicWrite("CP12_FREEZE_COMMIT",db->{try{return com.ethk3.novaonepro.FrozenCandidateAdapterR958.commitDevelopmentFreezeCP8(db,lease,prepared);}catch(RuntimeException e){writeFailure[0]=e;throw e;}},null);
                if(frozen==null)throw new DevelopmentProductionCP12.Wait(commitFailureReasonCP12(writeFailure[0],"FREEZE_COMMIT_FAILED"));return true;
            }
        }catch(com.ethk3.novaonepro.ResearchWorkBudgetR927.Yield|com.ethk3.novaonepro.RuntimeResourceAuditR927.BudgetExceeded yielded) {
            yieldUnchangedCP12(store,lease,"REALTIME_PRIORITY",250);return true;
        }catch(com.ethk3.novaonepro.FrozenCandidateAdapterR958.FreezeTargetChangedCP8 superseded) {
            yieldUnchangedCP12(store,lease,"TARGET_SUPERSEDED",250);return true;
        }catch(DevelopmentProductionCP12.Wait wait) {
            if(terminalFailureCP12(wait.reason)){quarantineOwnedTaskCP12(store,lease,wait);return true;}
            if(storageHoldCP12(wait.reason)){waitStorageCP12(store,lease,wait.reason);return true;}
            yieldUnchangedCP12(store,lease,wait.reason,1000);return true;
        }catch(RuntimeException bad) {
            if("ALGORITHM_MEMORY_BUDGET".equals(bad.getMessage())){yieldUnchangedCP12(store,lease,"WAIT_RESOURCE",1000);return true;}
            // Infrastructure availability says nothing about candidate quality or input integrity.
            // Preserve the exact feature/numerical cursor; only deterministic integrity errors
            // below are allowed to quarantine the candidate and reject its originating action.
            if(retryInfrastructureCP12(store,lease,bad))return true;
            quarantineOwnedTaskCP12(store,lease,bad);return true;
        }
    }
    public static DevelopmentBatchCP11 prepareCandidateBatchCP11(com.ethk3.novaonepro.DbStore store,
            com.ethk3.novaonepro.ResearchTaskQueueR967.Lease lease,long deadlineNs) {
        return DevelopmentProductionCP12.prepareBatch(store,lease,deadlineNs);
    }
    private static String advanceTrainingSlotCP11(com.ethk3.novaonepro.DbStore store,long endNs) {
        final String[] kinds={"LEGACY_OUTBOX","TRAIN_CANDIDATE","FREEZE_CANDIDATE"};
        return store.coordinatorForResearchR952().runAtomicWrite("CP12_TRAIN_SUBSLOT",db->{
            int selected=0;try(android.database.Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='cp11_training_subslot'",null)) {
                if(c.moveToFirst())try{selected=Integer.parseInt(c.getString(0));}catch(NumberFormatException ignored){}
            }
            if(selected<0||selected>=kinds.length)selected=0;
            // Preserve the next owner when prior work leaves only an unusable tail.
            // Otherwise LEGACY_OUTBOX can repeatedly consume the full slice, while
            // TRAIN_CANDIDATE is advanced past before decoding its pinned input.
            // This is admission only: no numerical/evidence or lease state changes.
            long minimumNs=selected==0?125_000_000L:250_000_000L;
            if(endNs-System.nanoTime()<minimumNs
                    ||com.ethk3.novaonepro.ResearchWorkBudgetR927.remainingMs()<minimumNs/1_000_000L)return null;
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('cp11_training_subslot',?,?)",new Object[]{Integer.toString((selected+1)%kinds.length),System.currentTimeMillis()});
            return kinds[selected];
        },null);
    }
    /** Pure failure classification. Never matches messages, generic SQLiteException,
     * corruption/constraints, or hash/protocol validation failures. Retry is allowed
     * only after the complete, acyclic cause chain terminates within 32 objects. */
    static String infrastructureWaitReasonCP12(Throwable failure){
        java.util.Set<Throwable> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        java.util.List<Throwable> causes=new java.util.ArrayList<>();
        Throwable current=failure;
        while(current!=null){
            // An incomplete or cyclic chain cannot establish absence of corruption.
            if(causes.size()>=32||!seen.add(current))return "";
            if(current instanceof android.database.sqlite.SQLiteDatabaseCorruptException
                    ||current instanceof android.database.sqlite.SQLiteConstraintException)return "";
            causes.add(current);
            try{current=current.getCause();}catch(RuntimeException unreadableCause){return "";}
        }
        for(Throwable cause:causes){
            if(cause instanceof android.database.sqlite.SQLiteDatabaseLockedException
                    ||cause instanceof android.database.sqlite.SQLiteTableLockedException)return "INFRA_DB_BUSY";
            if(cause instanceof android.database.sqlite.SQLiteFullException)return "INFRA_STORAGE_FULL";
            if(cause instanceof android.database.sqlite.SQLiteDiskIOException)return "INFRA_DB_IO";
            if(cause instanceof android.database.sqlite.SQLiteCantOpenDatabaseException)return "INFRA_DB_OPEN";
            if(cause instanceof android.database.sqlite.SQLiteReadOnlyDatabaseException)return "INFRA_DB_READ_ONLY";
            if(cause instanceof android.os.OperationCanceledException
                    ||cause instanceof java.util.concurrent.CancellationException)return "INFRA_CANCELLED";
        }
        return "";
    }
    /** Existing task owner remains authoritative. A failed release leaves its fenced
     * RUNNING lease recoverable; it cannot authorize completion, training or quality. */
    static boolean retryInfrastructureCP12(com.ethk3.novaonepro.DbStore store,
            com.ethk3.novaonepro.ResearchTaskQueueR967.Lease lease,RuntimeException failure){
        final String reason=infrastructureWaitReasonCP12(failure);if(reason.isEmpty())return false;
        final boolean wait=!("INFRA_DB_BUSY".equals(reason)||"INFRA_CANCELLED".equals(reason));
        final long delay="INFRA_CANCELLED".equals(reason)?250L:wait?60000L:1000L;
        com.ethk3.novaonepro.ResearchWorkBudgetR927.finishLeaseReceiptR66(() ->
            store.coordinatorForResearchR952().runAtomicWrite("CP12_INFRASTRUCTURE_WAIT",db->
                wait?com.ethk3.novaonepro.ResearchTaskQueueR967.waitInputCP12(db,lease,reason,delay)
                    :com.ethk3.novaonepro.ResearchTaskQueueR967.yield(db,lease,lease.cursor,reason,delay),false));
        return true;
    }
    static String commitFailureReasonCP12(Throwable failure,String fallback){
        String quota=com.ethk3.novaonepro.RetentionDurableModelQuotaCP12.reason(failure);return quota.isEmpty()?fallback:quota;
    }
    static boolean storageHoldCP12(String reason){return "MODEL_STORAGE_CAPACITY".equals(reason)||"MODEL_STORAGE_LEGACY_OVER_BUDGET".equals(reason)||"TRAINING_WORKING_STORAGE_CAPACITY".equals(reason)||"TRAINING_WORKING_STORAGE_LEGACY_OVER_BUDGET".equals(reason)||"TRAINING_INPUT_STORAGE_CAPACITY".equals(reason);}
    static boolean terminalFailureCP12(String reason){return "MODEL_STORAGE_HASH_SIZE_CONFLICT".equals(reason)||"MODEL_STORAGE_SINGLE_PAYLOAD_BOUND".equals(reason)||"TRAINING_WORKING_SINGLE_PAYLOAD_BOUND".equals(reason);}
    private static void waitStorageCP12(com.ethk3.novaonepro.DbStore store,com.ethk3.novaonepro.ResearchTaskQueueR967.Lease lease,String reason){
        Boolean saved=store.coordinatorForResearchR952().runAtomicWrite("CP12_STORAGE_WAIT",db->com.ethk3.novaonepro.ResearchTaskQueueR967.waitInputCP12(db,lease,reason,60000),false);
        if(!Boolean.TRUE.equals(saved))throw new IllegalStateException("STORAGE_WAIT_LEASE_CHANGED");
    }
    private static void yieldUnchangedCP12(com.ethk3.novaonepro.DbStore store,com.ethk3.novaonepro.ResearchTaskQueueR967.Lease lease,String reason,long delay) {
        Boolean released=com.ethk3.novaonepro.ResearchWorkBudgetR927.finishLeaseReceiptR66(() -> store.coordinatorForResearchR952().runAtomicWrite("CP12_TRAIN_WAIT",db->com.ethk3.novaonepro.ResearchTaskQueueR967.yield(db,lease,lease.cursor,reason,delay),false));
        if(!Boolean.TRUE.equals(released))throw new IllegalStateException("TRAIN_WAIT_LEASE_CHANGED");
    }
    private static void quarantineOwnedTaskCP12(com.ethk3.novaonepro.DbStore store,com.ethk3.novaonepro.ResearchTaskQueueR967.Lease lease,RuntimeException bad) {
        Throwable root=bad;java.util.Set<Throwable> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
        // Diagnostic traversal must not rethrow an unavailable custom cause accessor
        // after conservative classification, or spend unbounded time on a cause chain.
        for(int depth=0;depth<32&&seen.add(root);depth++){
            final Throwable next;try{next=root.getCause();}catch(RuntimeException unreadableCause){break;}
            if(next==null||seen.contains(next))break;root=next;
        }
        final String detail=(bad.getClass().getSimpleName()+":"+String.valueOf(bad.getMessage())+"|cause="+root.getClass().getSimpleName()+":"+String.valueOf(root.getMessage()));final String reason=detail.length()>512?detail.substring(0,512):detail;
        Boolean committed=store.coordinatorForResearchR952().runAtomicWrite("CP12_TRAIN_TERMINAL",db->ResearchDirectorR952.failCandidateTaskCP12(db,lease,reason),false);
        if(!Boolean.TRUE.equals(committed))throw new IllegalStateException("TRAIN_TERMINAL_LEASE_CHANGED",bad);
    }

}
