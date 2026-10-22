package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public final class AtomOntologyR952 {
    private static final Set<String> TYPES=new HashSet<String>(Arrays.asList("COUNT","RATE","RECENCY","TREND","ENTROPY","RESIDUAL"));
    private AtomOntologyR952(){}
    public static void validate(WorldAtomR952 atom){
        if(atom==null||!TYPES.contains(atom.type))throw new IllegalArgumentException("R952_ATOM_TYPE");
        if(!"BINANCE1_PK10".equals(atom.domain)||!("FLOAT64".equals(atom.valueSchema)||"INTEGER".equals(atom.valueSchema)||"BOOLEAN".equals(atom.valueSchema)))throw new IllegalArgumentException("R958_ATOM_DOMAIN_OR_SCHEMA");
        if(!"source_issue<target_issue".equals(atom.pitContract)||!atom.lineageHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R958_ATOM_PIT_OR_LINEAGE");
        cost(atom);
        if(!atom.atomHash.equals(V51ChecksumEngine.sha256(atom.canonical())))throw new IllegalStateException("R952_ATOM_HASH_DRIFT");
    }
    public static int cost(WorldAtomR952 atom){if("LOW".equals(atom.costClass))return 1;if("MEDIUM".equals(atom.costClass))return 4;if("HIGH".equals(atom.costClass))return 10;throw new IllegalArgumentException("R958_ATOM_COST");}
}
