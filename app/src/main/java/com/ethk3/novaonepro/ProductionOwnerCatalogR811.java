package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Version-scoped current owner certificate; Research is explicitly RESEARCH_ONLY. */
public final class ProductionOwnerCatalogR811 {
    private ProductionOwnerCatalogR811() {}
    public static List<Owner> all() {
        ArrayList<Owner> out = new ArrayList<Owner>();
        out.add(new Owner("Runtime", "AutoFetchService->BusinessReconcilerR36->V51RuntimeOrchestrator", "PRODUCTION"));
        out.add(new Owner("Analysis", "AnalysisSingleFlightR36", "PRODUCTION"));
        out.add(new Owner("RealtimePressure", "RealtimePressureGateR811", "PRODUCTION_GUARD"));
        out.add(new Owner("BusinessTruth", "RuntimeHealthLedgerR36", "READ_ONLY_TRUTH"));
        out.add(new Owner("Issue", "TargetIssueResolver", "PRODUCTION"));
        out.add(new Owner("NumberQ", "V51PermutationNumberModelEngine", "PRODUCTION"));
        out.add(new Owner("FormalAction", "K3ActionSpaceEngine/V51PositionActionBuilder", "PRODUCTION"));
        out.add(new Owner("Rank", "V51ActionRankerEngine", "PRODUCTION"));
        out.add(new Owner("SelectedHitP", "V51SelectedActionCalibrationEngine", "PRODUCTION"));
        out.add(new Owner("REAL", "DecisionGateEngine", "PRODUCTION"));
        out.add(new Owner("Settlement", "SettlementEngine", "PRODUCTION"));
        out.add(new Owner("OOS", "OosAfterDrawWriterR36", "PRODUCTION"));
        out.add(new Owner("Deferred", "DeferredEvidenceWriterR36", "PRODUCTION"));
        out.add(new Owner("Research", "AutonomousResearchEvolutionEngine", "RESEARCH_ONLY"));
        return Collections.unmodifiableList(out);
    }
    public static Validation validate() {
        Set<String> roles = new HashSet<String>(); ArrayList<String> errors = new ArrayList<String>();
        for (Owner owner : all()) {
            if (!roles.add(owner.role)) errors.add("DUPLICATE_ROLE:" + owner.role);
            if ("REAL".equals(owner.role) && !"DecisionGateEngine".equals(owner.path))
                errors.add("REAL_OWNER");
            if ("Settlement".equals(owner.role) && !"SettlementEngine".equals(owner.path))
                errors.add("SETTLEMENT_OWNER");
            if ("OOS".equals(owner.role) && !"OosAfterDrawWriterR36".equals(owner.path))
                errors.add("OOS_OWNER");
            if ("Runtime".equals(owner.role)
                    && owner.path.indexOf("AutoFetchService") < 0)
                errors.add("RUNTIME_OWNER");
            if (owner.path.indexOf("Research") >= 0
                    && !"RESEARCH_ONLY".equals(owner.authority))
                errors.add("RESEARCH_AUTHORITY");
        }
        return new Validation(errors.isEmpty(), errors.toString(), V51ChecksumEngine.sha256(canonical()));
    }
    public static void persist(DbStore store) { if (store == null) return; Validation v = validate(); store.setState("r811_owner_status", v.passed ? "PASS" : "FAIL"); store.setState("r811_owner_detail", v.detail); store.setState("r811_owner_hash", v.hash); }
    private static String canonical() { StringBuilder out = new StringBuilder(); for (Owner o : all()) out.append(o.role).append('=').append(o.path).append('@').append(o.authority).append('|'); return out.toString(); }
    public static final class Owner { public final String role,path,authority; Owner(String r,String p,String a){role=r;path=p;authority=a;} }
    public static final class Validation { public final boolean passed; public final String detail,hash; Validation(boolean p,String d,String h){passed=p;detail=d;hash=h;} }
}
