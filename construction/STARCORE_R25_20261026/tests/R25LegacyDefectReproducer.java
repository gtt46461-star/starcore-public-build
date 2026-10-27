package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Exercises the supplied unchanged production Java, not mock implementations. */
public final class R25LegacyDefectReproducer {
  static void must(boolean ok,String message){if(!ok)throw new AssertionError(message);}
  static ModelGenomeSU2 genome(double w,double c) {
    Map<String,String> p = new TreeMap<>(); p.put("alpha","1");
    return new ModelGenomeSU2(Arrays.asList(new ModelGenomeSU2.Member("MSDIR",p,w),new ModelGenomeSU2.Member("HMM",Collections.singletonMap("K","2"),1)),"STATIC",false,"META",.15,c,true);
  }
  public static void main(String[] args){
    ModelGenomeSU2 a=genome(1.23441,.234541),b=genome(1.23449,.234549);
    must(a.members.get(0).weight!=b.members.get(0).weight,"different weights required");
    must(a.key().equals(b.key()),"legacy lossy identity not reproduced");
    ModelGenomeSU2 parsed=ModelGenomeSU2.parse(a.text());
    must(parsed.members.get(0).weight!=a.members.get(0).weight,"lossy roundtrip not reproduced");
    String before=a.key(); a.members.get(0).params.put("alpha","3");
    must(!a.key().equals(before),"mutable identity not reproduced");
    int n=1000;boolean[] act=new boolean[n],child=new boolean[n],parent=new boolean[n],missing=new boolean[n];
    Arrays.fill(act,true);for(int i=0;i<n;i++){child[i]=i%20<8;parent[i]=i%20<9;}
    SelectiveEvidenceSU2.Verdict v=SelectiveEvidenceSU2.evaluate(act,child,missing,act,parent,0,n,1,.05,250,.28,.15);
    must(v.pass,"UCB noninferiority defect not reproduced");
    System.out.println("{\"identity_collision\":true,\"original_weight\":"+b.members.get(0).weight+",\"roundtrip_weight\":"+parsed.members.get(0).weight+",\"mutable_identity\":true,\"legacy_noninferiority_pass\":"+v.pass+",\"child_rate\":"+v.rate+",\"parent_rate\":0.45,\"paired_mean\":-0.05,\"paired_upper\":"+v.pairedUpper+",\"scope\":\"SYNTHETIC_CONTRACT_REPRODUCTION_NOT_WINRATE_TEST\"}");
  }
}
