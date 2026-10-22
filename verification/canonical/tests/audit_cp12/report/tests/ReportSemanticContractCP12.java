package com.ethk3.novaonepro;
import com.ethk3.novaonepro.*;import java.util.*;
public class ReportSemanticContractCP12 {
 static int assertions;static void need(boolean b){assertions++;if(!b)throw new AssertionError(assertions);}
 public static void main(String[]args)throws Exception{
  List<ReportCatalogCP4.CheckSpec> checks=ReportCheckRegistryCP12.checks(null);need(checks.size()==96);Set<String> ids=new HashSet<>();int assertionsKind=0,references=0,diagnostics=0;
  for(ReportCatalogCP4.CheckSpec s:checks){need(ids.add(s.id));need(s.runner!=null);need(!s.sourceMethod.isEmpty());need(!s.queryContract.isEmpty());if(s.kind==ReportCatalogCP4.CheckKind.ASSERTION)assertionsKind++;else if(s.kind==ReportCatalogCP4.CheckKind.REFERENCE)references++;else diagnostics++;
   ReportCatalogCP4.CheckResult healthy=new ReportCatalogCP4.CheckResult("EXECUTED",s.kind==ReportCatalogCP4.CheckKind.REFERENCE?"N/A":s.kind==ReportCatalogCP4.CheckKind.ASSERTION?"PASS":"OBSERVED",s.kind==ReportCatalogCP4.CheckKind.REFERENCE?"NOT_APPLICABLE":"OBSERVED","fixture","body");need(ReportCatalogCP4.hasCompleteResult(s,healthy));need(ReportCatalogCP4.isHealthy(s,healthy));
   ReportCatalogCP4.CheckResult error=new ReportCatalogCP4.CheckResult("READ_ERROR","READ_ERROR","UNKNOWN","SCHEMA_ABSENT","snapshot.db");need(!ReportCatalogCP4.hasCompleteResult(s,error));need(!ReportCatalogCP4.isHealthy(s,error));
   if(s.kind==ReportCatalogCP4.CheckKind.DIAGNOSTIC){for(String outcome:new String[]{"WARN","FAIL","NOT_PROVEN","UNKNOWN"}){ReportCatalogCP4.CheckResult r=new ReportCatalogCP4.CheckResult("EXECUTED",outcome,outcome,"source predicate","row reference");need(ReportCatalogCP4.hasCompleteResult(s,r));need(!ReportCatalogCP4.isHealthy(s,r));need(r.domainOutcome.equals(outcome));}}
  }
  need(assertionsKind==2);need(references==16);need(diagnostics==78);need(ReportCheckRegistryCP12.health("invented_domain").equals("UNKNOWN"));
  System.out.println("PASS REPORT96_BINDINGS_AND_SEMANTICS assertions="+assertions+" assertionKinds="+assertionsKind+" diagnostics="+diagnostics+" references="+references);
 }
}
