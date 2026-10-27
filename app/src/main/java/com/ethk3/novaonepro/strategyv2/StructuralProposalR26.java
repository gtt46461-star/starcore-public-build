package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Executable structural edits with distinct accounting from parameter variants.
 * A changed structure is only a candidate, never an assertion of improvement. */
public final class StructuralProposalR26 {
    private StructuralProposalR26() {}
    public static final class Proposal {
        public final ModelGenomeSU2 genome;public final String parent,operation,category;
        Proposal(ModelGenomeSU2 g,String p,String op,String category){genome=g;parent=p;operation=op;this.category=category;}
    }
    public static String structure(ModelGenomeSU2 g){
        List<Object> members=new ArrayList<>();for(ModelGenomeSU2.Member m:g.members){Map<String,Object> graph=new TreeMap<>();graph.put("kind",m.kind);for(String k:Arrays.asList("sources","lags","atoms","mols","K","text_hex"))if(m.params.containsKey(k))graph.put(k,m.params.get(k));members.add(graph);}
        return PolicyIdentityR26.sha256(PolicyIdentityR26.canonical(Arrays.asList(members,g.pool,g.sink)));
    }
    private static ModelGenomeSU2 copy(ModelGenomeSU2 p,List<ModelGenomeSU2.Member> members,String pool){return new ModelGenomeSU2(members,pool,p.sink,p.gate,p.coverage,p.metaC,p.calibrated);}
    public static List<Proposal> from(ModelGenomeSU2 parent,ModelGenomeSU2.Member complementary){
        List<Proposal> out=new ArrayList<>();ModelGenomeSU2 base=copy(parent,parent.members,parent.pool);
        out.add(new Proposal(base,parent.key(),"PRESERVE_PARENT_STRUCTURE",base.key().equals(parent.key())?"NO_OP":"IDENTITY_MIGRATION"));
        List<ModelGenomeSU2.Member> mem=new ArrayList<>(parent.members);boolean present=false;for(ModelGenomeSU2.Member m:mem)present|=m.modelKey().equals(complementary.modelKey());
        if(!present&&mem.size()<4){mem.add(complementary);add(out,parent,copy(parent,mem,"BMA"),"ADD_COMPLEMENTARY_MEMBER");}
        if(parent.members.size()>1){mem=new ArrayList<>(parent.members);mem.remove(mem.size()-1);add(out,parent,copy(parent,mem,parent.pool),"REMOVE_MEMBER");}
        if(parent.members.size()>1&&!parent.pool.equals("GATED"))add(out,parent,copy(parent,parent.members,"GATED"),"CONDITIONAL_ROUTER");
        return out;
    }
    private static void add(List<Proposal> out,ModelGenomeSU2 p,ModelGenomeSU2 g,String op){out.add(new Proposal(g,p.key(),op,g.key().equals(p.key())?"NO_OP":structure(g).equals(structure(p))?"PARAMETER_VARIANT":"STRUCTURAL_CANDIDATE"));}
}
