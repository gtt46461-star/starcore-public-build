package com.ethk3.novaonepro.strategyv2;
import java.nio.file.*;
import java.util.*;
public final class DiagnosisRenderRegression {
 public static void main(String[] args)throws Exception{
  String empty="SU2DIAG\t1\ndataset\tempty-probe\nseq\t2000\nprobe\tCONTMAT\t-\t\n";
  UpgradeDiagnosisSU2.Diagnosis d=UpgradeDiagnosisSU2.diagnose(UpgradeDiagnosisSU2.parse(empty));UpgradeDiagnosisSU2.Ledger l=new UpgradeDiagnosisSU2.Ledger();
  String md=UpgradeDiagnosisSU2.renderR30(d,l),json=UpgradeDiagnosisSU2.toJsonR30(d,l);if(md.isEmpty()||!json.contains("r30Sections"))throw new AssertionError("EMPTY_RENDER");
  int count=0;Path fixtures=Paths.get(args[0]);TreeSet<String> tags=new TreeSet<>();try(java.util.stream.Stream<Path>s=Files.list(fixtures)){s.filter(p->p.getFileName().toString().endsWith("_0.snapshot.txt")).forEach(p->tags.add(p.getFileName().toString().replace("_0.snapshot.txt","")));}
  for(String tag:tags){UpgradeDiagnosisSU2.Ledger ledger=new UpgradeDiagnosisSU2.Ledger();
   for(int i=0;;i++){Path p=fixtures.resolve(tag+"_"+i+".snapshot.txt");if(!Files.exists(p))break;String text=Files.readString(p);UpgradeDiagnosisSU2.Snapshot snap=UpgradeDiagnosisSU2.parse(text);UpgradeDiagnosisSU2.Diagnosis result=UpgradeDiagnosisSU2.diagnose(snap);
    ledger=UpgradeDiagnosisSU2.Ledger.fromText(ledger.text(),ledger.snaps);ledger.update(result.findings,snap.seq);ledger.addSnapshot(snap.seq,text);
    if(!UpgradeDiagnosisSU2.render(result,ledger).equals(Files.readString(fixtures.resolve(tag+"_"+i+".md"))))throw new AssertionError("LEGACY_MD_PARITY:"+p);
    if(!UpgradeDiagnosisSU2.toJson(result,ledger).equals(Files.readString(fixtures.resolve(tag+"_"+i+".json"))))throw new AssertionError("LEGACY_JSON_PARITY:"+p);count++;
   }
  }
  if(count<12)throw new AssertionError("FIXTURE_COVERAGE:"+count);System.out.println("DIAGNOSIS_HOST_PASS empty_probe_NPE_fixed=1 legacy_reports_byte_identical="+count+" unrelated_committee_paths_not_tested=1");
 }
}
