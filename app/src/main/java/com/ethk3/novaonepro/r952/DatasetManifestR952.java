package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;

public final class DatasetManifestR952 {
    public final String sourceFirst,sourceLast,targetIssue,featureHash,labelContract,codeHash,splitHash,hash;
    public DatasetManifestR952(String first,String last,String target,String features,String label,String code,String split){
        sourceFirst=safe(first);sourceLast=safe(last);targetIssue=safe(target);featureHash=req(features);labelContract=req(label);codeHash=req(code);splitHash=req(split);
        if(sourceLast.length()>0 && sourceLast.compareTo(targetIssue)>=0)throw new IllegalArgumentException("R952_DATASET_PIT_LEAKAGE");
        hash=V51ChecksumEngine.sha256("R952_DATASET_V1|"+sourceFirst+'|'+sourceLast+'|'+targetIssue+'|'+featureHash+'|'+labelContract+'|'+codeHash+'|'+splitHash);
    }
    private static String req(String v){String s=safe(v);if(s.length()==0)throw new IllegalArgumentException("R952_DATASET_FIELD");return s;}
    private static String safe(String v){return v==null?"":v.trim();}
}
