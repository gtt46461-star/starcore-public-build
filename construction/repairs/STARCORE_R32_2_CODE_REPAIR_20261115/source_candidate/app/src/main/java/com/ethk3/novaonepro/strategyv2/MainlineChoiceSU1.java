package com.ethk3.novaonepro.strategyv2;

import java.util.*;
import java.util.function.Function;

/** Pure ordering policy; callers supply only independently verified, unexpired certificates. */
public final class MainlineChoiceSU1 {
    private MainlineChoiceSU1() {}
    public static final class Candidate {
        public final String strategy,certificate,parent;
        public final long issued,expires;
        public Candidate(String strategy,String certificate,String parent,long issued,long expires){this.strategy=strategy;this.certificate=certificate;this.parent=parent;this.issued=issued;this.expires=expires;}
    }
    public static final class Choice {
        public final Candidate candidate;public final String operation;
        Choice(Candidate candidate,String operation){this.candidate=candidate;this.operation=operation;}
    }
    public static Choice choose(String current,String currentCertificate,List<Candidate> valid,Function<String,String> parentOf,boolean holdPromotions){
        List<Candidate> ordered=new ArrayList<>(valid);ordered.sort(Comparator.comparingLong((Candidate c)->c.issued).thenComparing(c->c.strategy).thenComparing(c->c.certificate));
        if(current==null)return ordered.isEmpty()?null:new Choice(ordered.get(0),"BOOTSTRAP");
        Candidate same=null;
        for(Candidate c:ordered)if(c.strategy.equals(current)&&(same==null||c.expires>same.expires))same=c;
        if(!holdPromotions)for(Candidate c:ordered)if(current.equals(c.parent))return new Choice(c,"PROMOTE");
        if(same!=null)return same.certificate.equals(currentCertificate)?null:new Choice(same,"RENEW");
        Set<String> seen=new HashSet<>();String parent=parentOf.apply(current);
        for(int depth=0;parent!=null&&seen.add(parent)&&depth<64;depth++,parent=parentOf.apply(parent))
            for(Candidate c:ordered)if(parent.equals(c.strategy))return new Choice(c,"FALLBACK");
        return new Choice(null,"REVOKE");
    }
}
