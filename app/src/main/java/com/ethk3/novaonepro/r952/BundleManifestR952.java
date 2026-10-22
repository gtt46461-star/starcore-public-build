package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;

public final class BundleManifestR952 {
    public final String candidateId,artifactHash,parentHash,inputSchemaHash,codeHash,bundleHash;
    public BundleManifestR952(String c,String a,String p,String schema,String code){candidateId=req(c);artifactHash=hash(a);parentHash=p==null?"":p;inputSchemaHash=hash(schema);codeHash=hash(code);if(parentHash.length()>0)hash(parentHash);bundleHash=V51ChecksumEngine.sha256("R952_BUNDLE1|"+candidateId+'|'+artifactHash+'|'+parentHash+'|'+inputSchemaHash+'|'+codeHash);}
    private static String hash(String v){String s=req(v);if(s.length()!=64)throw new IllegalArgumentException("R952_BUNDLE_HASH");return s;}private static String req(String v){if(v==null||v.trim().length()==0)throw new IllegalArgumentException("R952_BUNDLE_FIELD");return v.trim();}
}
