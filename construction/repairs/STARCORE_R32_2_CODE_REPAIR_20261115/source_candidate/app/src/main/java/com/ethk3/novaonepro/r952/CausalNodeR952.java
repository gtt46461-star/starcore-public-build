package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.V51ChecksumEngine;

public final class CausalNodeR952 {public final String id,type,hash;public CausalNodeR952(String i,String t){id=req(i);type=req(t);hash=V51ChecksumEngine.sha256("CAUSAL_NODE1|"+id+'|'+type);}private static String req(String v){if(v==null||v.trim().length()==0)throw new IllegalArgumentException("R952_CAUSAL_NODE");return v.trim();}}
