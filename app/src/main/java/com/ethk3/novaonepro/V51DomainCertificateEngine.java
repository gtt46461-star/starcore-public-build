package com.ethk3.novaonepro;

/** B11 domain certificate backed only by settled time-ordered selected-action OOS evidence. */
public final class V51DomainCertificateEngine {
    public static final String V57_B11_OWNER = "DOMAIN_CERTIFICATE";

    public V51Models.DomainCertificate certify(V51Models.StateTrajectorySnapshot state) {
        return descriptive(state);
    }

    public V51Models.DomainCertificate certify(
            DbStore store, V51Models.StateTrajectorySnapshot state) {
        return certify(store,state,null,"");
    }
    public V51Models.DomainCertificate certify(DbStore store,V51Models.StateTrajectorySnapshot state,
            java.util.List<Models.Draw> frozenHistory,String baseIssue) {
        if (state == null)
            return new V51Models.DomainCertificate(
                    "",
                    V51Enums.DomainStatus.DISCOVERED,
                    0,
                    0,
                    0,
                    1,
                    0,
                    1,
                    V51ChecksumEngine.sha256("DOMAIN_NULL"));
        ModelBundleRuntimeR932.Domain installed=ModelBundleRuntimeR932.current().domains.get(state.domainId);
        String runtimeReason=AutoFetchService.runtimeHealthBlockerR82(baseIssue,state.targetIssue);
        FrozenDriftPolicyR82.Policy driftPolicy=ModelBundleRuntimeR932.current().policy.DRIFT_POLICY_R82;
        FrozenDriftPolicyR82.Observation drift=FrozenDriftPolicyR82.observe(driftPolicy,state.position,frozenHistory,baseIssue,state.targetIssue);
        DomainRouterR929.Decision route=DomainRouterR929.routeWithEvidenceR82(store,state.targetIssue,state,
                installed==null?null:installed.evidence,runtimeReason.isEmpty(),runtimeReason,driftPolicy,drift);
        if(route.route==DomainRouterR929.Route.PAUSE)return new V51Models.DomainCertificate(state.domainId,V51Enums.DomainStatus.HARD_FREEZE,0,0,0,1,0,1,V51ChecksumEngine.sha256(route.routeHash+'|'+drift.receiptJson));
        if(installed!=null&&route.route==DomainRouterR929.Route.DOMAIN){
            boolean quality=installed.hitRate>=0.40d&&installed.wilsonLower>=0.30d&&installed.blockMinimum>=0.37d;
            return new V51Models.DomainCertificate(state.domainId,quality?V51Enums.DomainStatus.PROVEN:V51Enums.DomainStatus.WATCH,
                installed.evidence.futureN,installed.hitRate,installed.wilsonLower,installed.evidence.ece,installed.blockMinimum,
                state.changePointRisk,V51ChecksumEngine.sha256(route.routeHash+'|'+installed.evidence.certificateHash));
        }
        SelectedHitCertificationEngine.Certificate c =
                new SelectedHitCertificationEngine().certify(store, state.domainId);
        V51Enums.DomainStatus status =
                (c.sample >= 80
                                ? V51Enums.DomainStatus.READY
                                : (c.sample >= 20
                                        ? V51Enums.DomainStatus.WATCH
                                        : V51Enums.DomainStatus.DISCOVERED));
        if (c.driftRisk > 0.12d) status = V51Enums.DomainStatus.SOFT_RISK;
        if (c.driftRisk > 0.20d) status = V51Enums.DomainStatus.HARD_FREEZE;
        return new V51Models.DomainCertificate(
                state.domainId,
                status,
                c.sample,
                c.hitRate,
                Math.min(c.wilsonLower, c.betaLower),
                c.ece,
                c.blockMinimum,
                Math.max(c.driftRisk, state.changePointRisk),
                V51ChecksumEngine.sha256(c.checksum + '|' + status + '|' + route.routeHash));
    }

    private static V51Models.DomainCertificate descriptive(
            V51Models.StateTrajectorySnapshot state) {
        if (state == null)
            return new V51Models.DomainCertificate(
                    "",
                    V51Enums.DomainStatus.DISCOVERED,
                    0,
                    0,
                    0,
                    1,
                    0,
                    1,
                    V51ChecksumEngine.sha256("DOMAIN_NULL"));
        return new V51Models.DomainCertificate(
                state.domainId,
                V51Enums.DomainStatus.DISCOVERED,
                0,
                0,
                0,
                1,
                0,
                state.changePointRisk,
                V51ChecksumEngine.sha256(state.domainId + "|NO_OOS"));
    }
}
