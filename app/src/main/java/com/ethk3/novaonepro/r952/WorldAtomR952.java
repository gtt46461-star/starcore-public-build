package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;

public final class WorldAtomR952 {
    public final String atomId,type,domain,producer,valueSchema,pitContract,lineageHash,costClass,revision,atomHash;
    public WorldAtomR952(String id,String t,String d,String p,String schema,String pit,String lineage,String cost,String rev){
        atomId=req(id);type=req(t);domain=req(d);producer=req(p);valueSchema=req(schema);pitContract=req(pit);lineageHash=req(lineage);costClass=req(cost);revision=req(rev);
        if(pitContract.indexOf("source_issue<target_issue")<0)throw new IllegalArgumentException("R952_ATOM_PIT_CONTRACT");
        atomHash=V51ChecksumEngine.sha256(canonical());
    }
    public String canonical(){return "ATOM1|"+atomId+'|'+type+'|'+domain+'|'+producer+'|'+valueSchema+'|'+pitContract+'|'+lineageHash+'|'+costClass+'|'+revision;}
    private static String req(String v){String s=v==null?"":v.trim();if(s.length()==0||s.indexOf('|')>=0||s.indexOf(',')>=0||s.indexOf('\n')>=0||s.indexOf('\r')>=0)throw new IllegalArgumentException("R952_ATOM_FIELD");return s;}
}
