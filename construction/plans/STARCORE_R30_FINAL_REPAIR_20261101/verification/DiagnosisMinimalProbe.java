package com.ethk3.novaonepro.strategyv2;
public final class DiagnosisMinimalProbe {
 public static void main(String[] args) {
  String s="SU2DIAG\t1\ndataset\tprobe\nseq\t2000\nprobe\tCONTMAT\t-\t\n";
  UpgradeDiagnosisSU2.Diagnosis d=UpgradeDiagnosisSU2.diagnose(UpgradeDiagnosisSU2.parse(s));
  UpgradeDiagnosisSU2.Ledger l=new UpgradeDiagnosisSU2.Ledger();
  System.out.println(UpgradeDiagnosisSU2.renderR30(d,l));
  System.out.println(UpgradeDiagnosisSU2.toJsonR30(d,l));
 }
}
