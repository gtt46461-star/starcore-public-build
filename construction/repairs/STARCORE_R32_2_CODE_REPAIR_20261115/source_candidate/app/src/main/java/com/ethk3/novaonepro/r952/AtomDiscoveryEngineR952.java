package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930;

/** Discovery proposes immutable atoms; registration remains owned by WorldModelRepository. */
public final class AtomDiscoveryEngineR952 {
    private AtomDiscoveryEngineR952(){}
    public static WorldAtomR952 propose(String id,String type,String producer,String intentHash){
        if(intentHash==null||!intentHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R958_DISCOVERY_INTENT_HASH");
        return new WorldAtomR952(id,type,"BINANCE1_PK10",producer,"FLOAT64","source_issue<target_issue",V51ChecksumEngine.sha256("DISCOVERY|"+intentHash+'|'+id),"LOW","1");
    }
    /** Metadata for a normalized production atom admitted by the existing observed-quality owner. */
    public static WorldAtomR952 propose(String id,TypedAtomMoleculeGraphR930.Admission admission){
        if(admission==null||!admission.productionEligible||admission.state!=TypedAtomMoleculeGraphR930.AdmissionState.ACTIVE
                ||admission.node.kind!=TypedAtomMoleculeGraphR930.NodeKind.ATOM||!admission.node.stage.productionPredraw()
                ||admission.quality.support<48||admission.quality.pitFailures!=0)
            throw new IllegalArgumentException("R958_DISCOVERY_OBSERVED_ADMISSION_REQUIRED");
        return new WorldAtomR952(id,"RATE","BINANCE1_PK10","GenesisAtomicWorldR920","FLOAT64",
                "source_issue<target_issue",admission.node.structuralHash,"LOW",admission.node.version);
    }
}
