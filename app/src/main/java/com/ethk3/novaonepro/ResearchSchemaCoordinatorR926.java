package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/**
 * R926 bounded Research schema scheduler.  Live pressure may defer a step briefly, but cannot starve
 * Research forever: after MAX_DEFER_MS the next call performs exactly one small idempotent step.
 */
public final class ResearchSchemaCoordinatorR926 {
    private static final long MAX_DEFER_MS = 15000L;
    private static final Object LOCK = new Object();
    private static volatile long firstRequestedAt;
    private static volatile String state = "NOT_STARTED";
    private static volatile String cursor = "";
    private static volatile String error = "";
    private static Throwable failureCause; // guarded by LOCK, copied into the invocation result

    private ResearchSchemaCoordinatorR926() {}

    /** Result belongs to this invocation, even if another writer finishes before it returns. */
    public static final class StepResult {
        public final boolean ready, enteredWriter;
        public final String status, detail;
        public final Throwable cause;
        StepResult(boolean ready, boolean enteredWriter, String status, String detail, Throwable cause) {
            this.ready=ready; this.enteredWriter=enteredWriter; this.status=status;
            this.detail=detail; this.cause=cause;
        }
    }

    public static boolean ensureOneStep(final DatabaseCoordinator owner) {
        return ensureOneStepResult(owner).ready;
    }

    /** SU1's self-contained storage must not wait behind the entire legacy research overlay.
     * The same existing writer commits one statement; this does not stamp the legacy domain READY. */
    public static StepResult ensureStrategyStorageOneStep(final DatabaseCoordinator owner) {
        if(owner==null)return new StepResult(false,false,"WRITER_FAILED","OWNER_REQUIRED",null);
        try{return owner.runAtomicWriteOrThrowF01("R26_STRATEGY_STORAGE_UNIT",db->{
            int before=0;try(android.database.Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(c.moveToFirst())before=c.getInt(0);}
            int unitsBefore=strategyUnits(db);boolean ready=com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.advance(db);
            int after=0;try(android.database.Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(c.moveToFirst())after=c.getInt(0);}
            int unitsAfter=strategyUnits(db);boolean changed=before!=after||unitsBefore!=unitsAfter;
            return new StepResult(ready,true,ready?"STRATEGY_STORAGE_READY":changed?"STRATEGY_UNIT_COMMITTED":"STRATEGY_NO_UNIT_PROGRESS",unitsBefore+"->"+unitsAfter,null);
        });}catch(Exception failure){return new StepResult(false,false,"STRATEGY_STORAGE_FAILED",DatabaseCoordinator.describeCauseChain(failure,1800),failure);}
    }
    private static int strategyUnits(SQLiteDatabase db){
        try(android.database.Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='su1_migration_unit_v2'",null)){if(!c.moveToFirst())return 0;}
        try(android.database.Cursor c=db.rawQuery("SELECT count(*) FROM su1_migration_unit_v2",null)){return c.moveToFirst()?c.getInt(0):0;}
    }

    public static StepResult ensureOneStepResult(final DatabaseCoordinator owner) {
        if (owner == null) return new StepResult(false,false,"WRITER_FAILED","OWNER_REQUIRED",null);
        final boolean[] entered = {false};
        try {
            return owner.runWriteOrThrowF01("R958_RESEARCH_SCHEMA_STEP",new DatabaseCoordinator.WriteCallable<StepResult>() {
                public StepResult call(SQLiteDatabase db) {
                    entered[0]=true;
                    synchronized (LOCK) {
                        boolean ready;
                        failureCause=null;
                        try { ready=advance(db); }
                        catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; }
                        catch (RuntimeResourceAuditR927.BudgetExceeded yielded) { throw yielded; }
                        catch (Exception failure) {
                            state="FAILED"; failureCause=failure; error=DatabaseCoordinator.describeCauseChain(failure,1800);
                            owner.publishResearchSchemaResultR80(false, error);
                            return new StepResult(false,true,state,error,failure);
                        }
                        // Publish while still owning the existing writer, never after its release.
                        // A delayed acquisition failure cannot demote another call's verified READY.
                        owner.publishResearchSchemaResultR80(ready, error);
                        return new StepResult(ready,true,state,error,failureCause);
                    }
                }
            });
        } catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; }
        catch (RuntimeResourceAuditR927.BudgetExceeded yielded) { throw yielded; }
        catch (Exception failure) {
            if (failure instanceof IllegalStateException
                    && "CP3_DB_WRITE_ON_MAIN_THREAD".equals(failure.getMessage()))
                throw (IllegalStateException)failure;
            String detail=DatabaseCoordinator.describeCauseChain(failure,1800);
            boolean busy=detail.contains("CP3_DB_BUSY_DEADLINE") || detail.contains("InterruptedException");
            // Do not publish outside the writer. Entry and the F01 cause belong to this call,
            // including the uncommon case of a failure after its callback began.
            return new StepResult(false,entered[0],busy?"WRITER_BUSY":"WRITER_FAILED",detail,failure);
        }
    }

    private static boolean advance(SQLiteDatabase db) {
        synchronized (LOCK) {
            SchemaDomainCoordinatorR926.Contract contract = SchemaDomainCoordinatorR926.researchContract();
            boolean priorDomainReady = SchemaDomainCoordinatorR926.matches(db, contract);
            if (priorDomainReady && com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.readyOrderR79(db)
                    && LegacySettlementProofR79.ready(db)
                    && com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.readyR80(db)
                    && MultiHorizonProbeR80.readyR80(db)
                    && com.ethk3.novaonepro.r952.LongScaleSummaryR80.ready(db)
                    && com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.ready(db)
                    && com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.ready(db)) {
                state = "READY"; cursor = "COMPLETE"; error = ""; failureCause=null; return true;
            }
            long now = System.currentTimeMillis();
            if (firstRequestedAt <= 0L) firstRequestedAt = now;
            boolean pressure = RealtimePressureGateR811.isUnderPressure();
            if (pressure && now - firstRequestedAt < MAX_DEFER_MS) {
                state = "WAITING_FOR_BOUNDED_SLOT"; error=""; failureCause=null;
                cursor = nextStep(db, contract);
                if (!priorDomainReady) SchemaDomainCoordinatorR926.markState(db, contract, state, cursor, "LIVE_PRESSURE");
                return false;
            }
            try {
                state = "INITIALIZING"; error = ""; failureCause=null;
                if (priorDomainReady) {
                    // Preserve the prior READY baseline and its completed journals. Exactly one
                    // additive helper commits per writer callback; its DDL, journal and updated
                    // baseline fingerprint commit together, so restart never replays old units.
                    cursor = nextAdditiveStep(db);
                    if (com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.STEP.equals(cursor)
                            && !com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.advance(db)) return false;
                    installAdditiveStep(db, contract, cursor, true);
                    if (!"VERIFY".equals(nextAdditiveStep(db))) {
                        state="INITIALIZING"; cursor=nextAdditiveStep(db); error=""; return false;
                    }
                    state="READY";cursor="COMPLETE";error="";return true;
                }
                SchemaDomainCoordinatorR926.prepareRepairIfDrift(db, contract);
            String step = nextStep(db, contract);
                cursor = step;
                SchemaDomainCoordinatorR926.markState(db, contract, state, cursor, "");
                if ("VERIFY".equals(step)) {
                    // Final verification has its own writer callback. Do not append the
                    // complete integrity/readiness scan to the last DDL installation unit.
                    R811SchemaIntegrityVerifier.Report report = R811SchemaIntegrityVerifier.verifyResearch(db);
                    if (!report.passed) throw new IllegalStateException("R926_RESEARCH_INTEGRITY:" + report.detail);
                    SchemaDomainCoordinatorR926.verifyAndStamp(db, contract);
                    state = "READY"; cursor = "COMPLETE"; error = ""; failureCause=null; return true;
                } else if ("R811_RESEARCH".equals(step)) {
                    SchemaDomainCoordinatorR926.runStep(db, contract, step,
                            new SchemaDomainCoordinatorR926.Step() { public void run(SQLiteDatabase x) { DbHelper.ensureR811ResearchSchema(x); }});
                } else if ("R815_RESEARCH".equals(step)) {
                    SchemaDomainCoordinatorR926.runStep(db, contract, step,
                            new SchemaDomainCoordinatorR926.Step() { public void run(SQLiteDatabase x) { DbHelper.ensureR815ResearchSchema(x); }});
                } else if ("R906_RESEARCH_R64_VERIFIED".equals(step)) {
                    SchemaDomainCoordinatorR926.runStep(db, contract, step,
                            new SchemaDomainCoordinatorR926.Step() { public void run(SQLiteDatabase x) { DbHelper.ensureR906ResearchSchema(x); }});
                } else if ("R925_RESEARCH".equals(step)) {
                    SchemaDomainCoordinatorR926.runStep(db, contract, step,
                            new SchemaDomainCoordinatorR926.Step() { public void run(SQLiteDatabase x) { SchemaRevisionR925.ensureResearchTables(x); }});
                } else if ("R932_UNIFIED".equals(step)) {
                    SchemaDomainCoordinatorR926.runStep(db, contract, step,
                            new SchemaDomainCoordinatorR926.Step() { public void run(SQLiteDatabase x) { ensureUnifiedResearchSchema(x); }});
                } else if ("R952_EXTENSION".equals(step)) {
                    // Each child DDL unit commits independently. A completed child survives a
                    // later preemption; one callback never holds the writer for the entire overlay.
                    if (!com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.advanceR64(db))
                        return false;
                    SchemaDomainCoordinatorR926.runStep(db, contract, step, x -> {});
                } else if (isAdditiveStep(step)) {
                    if (com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.STEP.equals(step)
                            && !com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.advance(db)) return false;
                    installAdditiveStep(db, contract, step, false);
                }
                String remaining = nextStep(db, contract);
                if ("VERIFY".equals(remaining)) {
                    state = "INITIALIZING"; cursor = "VERIFY"; return false;
                }
                state = "INITIALIZING"; cursor = remaining; return false;
            } catch (ResearchWorkBudgetR927.Yield yielded) {
                throw yielded;
            } catch (RuntimeResourceAuditR927.BudgetExceeded yielded) {
                throw yielded;
            } catch (Exception failure) {
                state = "FAILED";
                failureCause=failure;
                error = DatabaseCoordinator.describeCauseChain(failure, 1800);
                if (!priorDomainReady) SchemaDomainCoordinatorR926.markState(db, contract, state, cursor, error);
                return false;
            }
        }
    }

    public static boolean ensureToReady(DatabaseCoordinator owner, int maxSteps) {
        int limit = Math.max(1, Math.min(8, maxSteps));
        for (int i = 0; i < limit; i++) {
            StepResult result=ensureOneStepResult(owner);
            if (result.ready) return true;
            if ("WAITING_FOR_BOUNDED_SLOT".equals(result.status) || !result.enteredWriter) break;
        }
        return false;
    }

    public static String summary() { return state + "|cursor=" + cursor + "|error=" + error; }

    private static String nextStep(SQLiteDatabase db, SchemaDomainCoordinatorR926.Contract c) {
        if (!SchemaDomainCoordinatorR926.stepComplete(db, c, "R811_RESEARCH")) return "R811_RESEARCH";
        if (!SchemaDomainCoordinatorR926.stepComplete(db, c, "R815_RESEARCH")) return "R815_RESEARCH";
        if (!SchemaDomainCoordinatorR926.stepComplete(db, c, "R906_RESEARCH_R64_VERIFIED")) return "R906_RESEARCH_R64_VERIFIED";
        if (!SchemaDomainCoordinatorR926.stepComplete(db, c, "R925_RESEARCH")) return "R925_RESEARCH";
        if (!SchemaDomainCoordinatorR926.stepComplete(db, c, "R932_UNIFIED")) return "R932_UNIFIED";
        if (!SchemaDomainCoordinatorR926.stepComplete(db, c, "R952_EXTENSION")) return "R952_EXTENSION";
        return nextAdditiveStep(db);
    }

    private static boolean isAdditiveStep(String step) {
        return "R79_MATRIX_READY_ORDER_V1".equals(step) || "R79_LEGACY_ORIGINAL_PROOF_V1".equals(step)
                || "R80_BOUND_ATOM_ORIGINAL_INPUT_V1".equals(step) || "R80_MULTI_HORIZON_PROBE_V1".equals(step)
                || com.ethk3.novaonepro.r952.LongScaleSummaryR80.SCHEMA_STEP.equals(step)
                || com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.SCHEMA_STEP.equals(step)
                || com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.STEP.equals(step);
    }

    private static void installAdditiveStep(SQLiteDatabase db, SchemaDomainCoordinatorR926.Contract contract,
                                            String step, boolean preserveReady) throws Exception {
        ResearchWorkBudgetR927.checkpoint();
        // Android nested transaction failure also marks an enclosing writer rollback-only.
        // A caller cannot accidentally commit partial additive DDL after a false result.
        db.beginTransaction();
        try {
            // Only the selected additive receipt may be re-armed; previous revisions stay intact.
            db.execSQL("UPDATE " + SchemaDomainCoordinatorR926.TABLE_JOURNAL
                    + " SET status='REPAIR_REQUIRED',error_code='ADDITIVE_OBJECT_NOT_READY'"
                    + " WHERE domain=? AND revision=? AND step_id=? AND status='COMPLETED'",
                    new Object[]{contract.domain, contract.revision, step});
            if ("R79_MATRIX_READY_ORDER_V1".equals(step)) {
                com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.ensureReadyOrderR79(db);
            } else {
                SchemaDomainCoordinatorR926.runStep(db, contract, step, x -> {
                    if ("R79_LEGACY_ORIGINAL_PROOF_V1".equals(step)) {
                        LegacySettlementProofR79.schema(x);
                        if (!LegacySettlementProofR79.ready(x)) throw new IllegalStateException("LEGACY_SCHEMA_NOT_READY");
                    } else if ("R80_BOUND_ATOM_ORIGINAL_INPUT_V1".equals(step)) {
                        com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.schemaR80(x);
                        if (!com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.readyR80(x)) throw new IllegalStateException("BOUND_SCHEMA_NOT_READY");
                    } else if ("R80_MULTI_HORIZON_PROBE_V1".equals(step)) {
                        MultiHorizonProbeR80.schemaR80(x);
                        if (!MultiHorizonProbeR80.readyR80(x)) throw new IllegalStateException("PROBE_SCHEMA_NOT_READY");
                    } else if (com.ethk3.novaonepro.r952.LongScaleSummaryR80.SCHEMA_STEP.equals(step)) {
                        com.ethk3.novaonepro.r952.LongScaleSummaryR80.schema(x);
                        if (!com.ethk3.novaonepro.r952.LongScaleSummaryR80.ready(x)) throw new IllegalStateException("LONG_SCHEMA_NOT_READY");
                    } else if (com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.SCHEMA_STEP.equals(step)) {
                        com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.schema(x);
                        if (!com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.ready(x)) throw new IllegalStateException("LONG_REGISTRATION_SCHEMA_NOT_READY");
                    } else if (com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.STEP.equals(step)) {
                        if (!com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.ready(x)) throw new IllegalStateException("SU1_SCHEMA_NOT_READY");
                    } else throw new IllegalStateException("UNKNOWN_ADDITIVE_STEP:" + step);
                });
            }
            if (preserveReady) SchemaDomainCoordinatorR926.verifyAndStamp(db, contract);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    private static String nextAdditiveStep(SQLiteDatabase db) {
        if (!com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952.readyOrderR79(db)) return "R79_MATRIX_READY_ORDER_V1";
        if (!LegacySettlementProofR79.ready(db)) return "R79_LEGACY_ORIGINAL_PROOF_V1";
        if (!com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.readyR80(db)) return "R80_BOUND_ATOM_ORIGINAL_INPUT_V1";
        if (!MultiHorizonProbeR80.readyR80(db)) return "R80_MULTI_HORIZON_PROBE_V1";
        if (!com.ethk3.novaonepro.r952.LongScaleSummaryR80.ready(db)) return com.ethk3.novaonepro.r952.LongScaleSummaryR80.SCHEMA_STEP;
        if (!com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.ready(db)) return com.ethk3.novaonepro.r952.LongCandidateLifecycleR81.SCHEMA_STEP;
        if (!com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.ready(db)) return com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.STEP;
        return "VERIFY";
    }

    private static void ensureUnifiedResearchSchema(SQLiteDatabase db) {
        TransactionSchemaCacheR941.Scope scope = TransactionSchemaCacheR941.enter(db);
        try {
            ResearchPlatformStoreR929.createSchema(db);
            ResearchLifecycleR932.schema(db);
            FixedResearchWindowR932.schema(db);
            AtomCaptureR932.schema(db);
            AtomicToolWorldR943.schema(db);
            StagedResearchRosterR943.schema(db);
            AlgorithmProofRecoveryR948.schema(db);
            // Legacy proof schema has its own additive slice after all base tables exist.
            // These are read before any lazy per-candidate schema call. Omitting them caused
            // R957 to stamp READY while capture/recovery failed on research_schedule_plan_r932.
            GlobalResearchScheduleR932.schema(db);
            ResearchCaptureLedgerR932.schema(db);
            CandidateExecutionAuditR932.schema(db);
            FixedWindowCertificatePublisherR932.schema(db);
            AtomAdmissionAuditR932.schema(db);
            ModuleAblationR932.schema(db);
            DirectedResearchCreationR932.schema(db);
            MolecularToolWorldR947.schema(db);
            CausalResearchFeedbackR947.schema(db);
            DbHelper.installRetentionTriggersR943(db);
        } finally { TransactionSchemaCacheR941.leave(scope); }
    }
}
