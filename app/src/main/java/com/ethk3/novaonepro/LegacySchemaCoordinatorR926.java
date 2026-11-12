package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/** R926 lazy Legacy schema owner. Legacy failure is observable and never marks Critical as failed. */
public final class LegacySchemaCoordinatorR926 {
    private LegacySchemaCoordinatorR926() {}
    public static final String MAIN_CHAIN_STEP_R31 = "LEGACY_MAIN_CHAIN_R31_V1";
    public interface MainChainInitializerR31 extends DatabaseCoordinator.SchemaInitializer {
        void initializeMainChainDefaultsR31(SQLiteDatabase db) throws Exception;
    }
    public interface SlicedInitializerR31 extends MainChainInitializerR31 {
        boolean advanceRuntimeR31(SQLiteDatabase db) throws Exception;
    }

    /** One durable idempotent unit per writer callback. A later callback reads
     * the journal again; process death cannot turn an uncommitted unit into DONE. */
    static boolean advanceUnitsR31(SQLiteDatabase db,String prefix,
            java.util.LinkedHashMap<String,Runnable> units,boolean atomic) throws Exception {
        SchemaDomainCoordinatorR926.Contract c=SchemaDomainCoordinatorR926.legacyContract();
        java.util.Set<String> completed=new java.util.HashSet<>();
        try(android.database.Cursor rows=db.rawQuery("SELECT step_id FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL
                +" WHERE domain=? AND revision=? AND status='COMPLETED'",new String[]{c.domain,String.valueOf(c.revision)})){
            while(rows.moveToNext())completed.add(rows.getString(0));
        }
        for(java.util.Map.Entry<String,Runnable> unit:units.entrySet()){
            String step=prefix+unit.getKey();if(completed.contains(step))continue;
            SchemaDomainCoordinatorR926.markState(db,c,"INITIALIZING",step,"");
            if(atomic)SchemaDomainCoordinatorR926.runStep(db,c,step,x->unit.getValue().run());
            else SchemaDomainCoordinatorR926.runStepAutocommitR31(db,c,step,x->unit.getValue().run());
            return false;
        }
        return true;
    }

    private static String[] mainTablesR31() {
        return new String[]{DbHelper.TABLE_DECISIONS,DbHelper.TABLE_TRADES,DbHelper.TABLE_SHADOW,
                DbHelper.TABLE_FULL_CANDIDATE_SCAN,DbHelper.TABLE_CANDIDATE_AUDIT,
                DbHelper.TABLE_DYNAMIC_CAUSE_CHAIN,DbHelper.TABLE_DECISION_AUDIT,
                DbHelper.TABLE_V20_CLOSED_LOOP,DbHelper.TABLE_MAIN_CHAIN_CANDIDATE_SNAPSHOT,
                DbHelper.TABLE_REAL_TRADE_LEDGER,DbHelper.TABLE_ADAPTIVE_JUDGE_SAMPLE,
                DbHelper.TABLE_PROMOTION_DOMAIN_SNAPSHOT};
    }
    private static String[] factorTablesR31() {
        return new String[]{DbHelper.TABLE_CAUSAL_FACTOR_MATRIX,DbHelper.TABLE_POSITION_CAUSE_MATRIX,
                DbHelper.TABLE_TEACHER_NUMBER_MATRIX,DbHelper.TABLE_COUNTERFACTUAL_RANK,
                DbHelper.TABLE_THIRD_GUARD_MATRIX,DbHelper.TABLE_RULE_HEALTH_MATRIX,
                DbHelper.TABLE_SOURCE_HEALTH_MATRIX,DbHelper.TABLE_STATE_HEALTH_MATRIX,
                DbHelper.TABLE_SHADOW_HEALTH_GATE,DbHelper.TABLE_DYNAMIC_WINDOW_CLUSTER,
                DbHelper.TABLE_WINDOW_FEATURE_SAMPLE,DbHelper.TABLE_POSITION_NEED_SAMPLE,
                DbHelper.TABLE_NUMBER_SUPPLY_SAMPLE,DbHelper.TABLE_NEED_SUPPLY_MATCH_SAMPLE,
                DbHelper.TABLE_COUNTERFACTUAL_REASON_SAMPLE,DbHelper.TABLE_PROMOTION_DOMAIN_SNAPSHOT};
    }
    private static String[] indexesR31() {
        return new String[]{"idx_mccs_target_rank","idx_mccs_candidate","idx_mccs_adaptive","idx_mccs_reject",
                "idx_mcrb_code","idx_decisions_main_chain","idx_decisions_adaptive_status",
                "idx_decisions_factor_cluster","idx_trades_main_chain","idx_trades_factor_cluster",
                "idx_shadow_main_chain","idx_shadow_promotion_status","idx_candidate_scan_top40",
                "idx_candidate_scan_main_chain","idx_candidate_scan_factor_cluster",
                "idx_adaptive_candidate_count","idx_adaptive_status_action","idx_promo_main_chain"};
    }
    /** Same objects feed the Legacy contract; fresh arrays cannot mutate the contract. */
    public static String[] requiredMainChainObjectsR31() {
        java.util.LinkedHashSet<String> objects=new java.util.LinkedHashSet<>();
        java.util.Collections.addAll(objects,mainTablesR31());
        java.util.Collections.addAll(objects,factorTablesR31());
        objects.add(DbHelper.TABLE_MAIN_CHAIN_REJECT_BUCKET);
        java.util.Collections.addAll(objects,indexesR31());
        return objects.toArray(new String[0]);
    }

    /** Actual read-only readiness. No stamp or DbStore column cache can hide drift. */
    public static void requireMainChainClosedLoopReadyR31(SQLiteDatabase db) {
        java.util.LinkedHashMap<String,java.util.LinkedHashSet<String>> tables=new java.util.LinkedHashMap<>();
        for(String table:mainTablesR31()){
            addColumnsR31(tables,table,DbStore.mainChainClosedLoopColumnDefs());
            addColumnsR31(tables,table,new String[]{"_id INTEGER","base_issue TEXT","target_issue TEXT",
                    "created_at INTEGER","position INTEGER","n1 INTEGER","n2 INTEGER","n3 INTEGER","k3_mask INTEGER"});
        }
        for(String table:factorTablesR31())addColumnsR31(tables,table,DbStore.mainChainFactorColumnDefs());
        addColumnsR31(tables,DbHelper.TABLE_SHADOW,new String[]{"rank_no INTEGER"});
        addColumnsR31(tables,DbHelper.TABLE_CANDIDATE_AUDIT,new String[]{"audit_key TEXT"});
        addColumnsR31(tables,DbHelper.TABLE_MAIN_CHAIN_REJECT_BUCKET,new String[]{
                "_id INTEGER","bucket_key TEXT","reject_code TEXT","sample_count INTEGER",
                "candidate_count INTEGER","bet_count INTEGER","shadow_count INTEGER","hold_count INTEGER",
                "hit_count INTEGER","miss_count INTEGER","avg_final_p REAL","avg_gate REAL","avg_edge REAL","updated_at INTEGER"});
        String[][] required=new String[tables.size()][2];int at=0;
        for(java.util.Map.Entry<String,java.util.LinkedHashSet<String>> table:tables.entrySet()){
            StringBuilder columns=new StringBuilder();
            for(String column:table.getValue()){if(columns.length()>0)columns.append(',');columns.append(column);}
            required[at++]=new String[]{table.getKey(),columns.toString()};
        }
        String[] indexes=indexesR31();
        String[][] objects=new String[indexes.length][2];
        for(int i=0;i<indexes.length;i++)objects[i]=new String[]{"index",indexes[i]};
        DatabaseCoordinator.requireSchemaObjectsR31(db,"MAIN_CHAIN_CLOSED_LOOP",required,objects);
    }

    private static void addColumnsR31(java.util.Map<String,java.util.LinkedHashSet<String>> tables,
            String table,String[] definitions) {
        java.util.LinkedHashSet<String> columns=tables.get(table);
        if(columns==null){columns=new java.util.LinkedHashSet<>();tables.put(table,columns);}
        for(String definition:definitions)columns.add(definition.substring(0,definition.indexOf(' ')));
    }

    public static boolean ensure(DatabaseCoordinator owner, final DatabaseCoordinator.SchemaInitializer initializer) {
        if (owner == null || initializer == null) return false;
        return ensure(owner,owner.writable(),initializer);
    }

    /** The migration's DDL, journal, validation and final main-chain step must
     * use the same handle. Reopening owner.writable() here silently selected
     * the business pool even when the caller had acquired a dedicated handle. */
    public static boolean ensure(DatabaseCoordinator owner, SQLiteDatabase db,
            final DatabaseCoordinator.SchemaInitializer initializer) {
        if (owner == null || db == null || initializer == null) return false;
        final SchemaDomainCoordinatorR926.Contract c = SchemaDomainCoordinatorR926.legacyContract();
        if (SchemaDomainCoordinatorR926.matches(db, c)
                && SchemaDomainCoordinatorR926.stepComplete(db,c,MAIN_CHAIN_STEP_R31)) {
            try { requireMainChainClosedLoopReadyR31(db);return true; }
            catch(DatabaseCoordinator.SchemaPendingR31 drift) { /* the owner repairs below */ }
        }
        try {
            SchemaDomainCoordinatorR926.prepareRepairIfDrift(db, c);
            SchemaDomainCoordinatorR926.markState(db, c, "INITIALIZING", "LEGACY_RUNTIME", "");
            if(!SchemaDomainCoordinatorR926.stepComplete(db,c,"LEGACY_RUNTIME")){
                if(initializer instanceof SlicedInitializerR31){
                    if(!((SlicedInitializerR31)initializer).advanceRuntimeR31(db)){
                        owner.noteLegacySlicePendingR31("LEGACY_RUNTIME_UNIT_COMMITTED");return false;
                    }
                    SchemaDomainCoordinatorR926.runStepAutocommitR31(db,c,"LEGACY_RUNTIME",x->{});
                }else SchemaDomainCoordinatorR926.runStepAutocommitR31(db,c,"LEGACY_RUNTIME",x->initializer.run(x));
            }
            // A matching old LEGACY_RUNTIME stamp cannot skip this additive owner step.
            if(initializer instanceof SlicedInitializerR31&&!SchemaDomainCoordinatorR926.stepComplete(db,c,MAIN_CHAIN_STEP_R31)
                    &&!owner.getHelper().advanceMainChainClosedLoopSchemaR31(db)){
                owner.noteLegacySlicePendingR31("LEGACY_MAIN_CHAIN_UNIT_COMMITTED");return false;
            }
            SchemaDomainCoordinatorR926.runStep(db,c,MAIN_CHAIN_STEP_R31,x->{
                if(!(initializer instanceof SlicedInitializerR31))owner.getHelper().ensureMainChainClosedLoopSchemaR31(x);
                if(initializer instanceof MainChainInitializerR31)
                    ((MainChainInitializerR31)initializer).initializeMainChainDefaultsR31(x);
                requireMainChainClosedLoopReadyR31(x);
            });
            requireMainChainClosedLoopReadyR31(db);
            SchemaDomainCoordinatorR926.verifyAndStamp(db, c);
            return true;
        } catch (Throwable failure) {
            Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(failure);
            if(fatal!=null)throw fatal;
            SchemaDomainCoordinatorR926.markState(db, c, "FAILED", "LEGACY_RUNTIME",
                    DatabaseCoordinator.describeCauseChain(failure,1800));
            return false;
        }
    }
}
