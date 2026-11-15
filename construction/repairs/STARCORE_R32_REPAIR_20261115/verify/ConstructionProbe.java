package com.ethk3.novaonepro.strategyv2;

import java.nio.file.*;
import java.util.*;

/** Host harness for the patched production classes. It never opens a phone's database. */
public final class ConstructionProbe {
    private static int checked;
    private static void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        System.out.println("PASS " + label);
        checked++;
    }
    private static ModelGenomeSU2 genome(String kind) {
        return new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(
                kind, Collections.emptyMap(), 1)), "STATIC", false, "MASS", 0.15, 0.2, false);
    }
    private static CertificateGateR27.Evidence eligible() {
        CertificateGateR27.Evidence e = new CertificateGateR27.Evidence();
        e.sequentialState=SequentialQualificationR27.QUALIFIED;
        e.acts=250; e.hits=85; e.ece=.04; e.worstBlock=.26; e.maxLosingStreak=6;
        e.abiEquivalent=true; e.pairedLcbVsI00=.01; e.pairedLcbVsNull=.01;
        return e;
    }
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        List<ModelGenomeSU2> donors=Arrays.asList(genome("R26F01"),genome("R26F02"),genome("R26F08"));
        String first = StructuralProposalR26.complementR32(donors.get(0),donors,100,0).kind;
        require(!first.equals("R26F01"),"donor differs from parent's mechanism");
        require(first.equals(StructuralProposalR26.complementR32(donors.get(0),donors,100,0).kind),"same frozen campaign deterministic");
        require(!first.equals(StructuralProposalR26.complementR32(donors.get(0),donors,101,0).kind),"later campaign rotates donor");
        require(CertificateGateR27.evaluate(eligible()).active,"unchanged registered boundary can qualify");
        CertificateGateR27.Evidence e=eligible();e.acts=249;require(!CertificateGateR27.evaluate(e).active,"249 future acts blocked");
        e=eligible();e.hits=251;require(!CertificateGateR27.evaluate(e).active,"impossible numerator blocked");
        e=eligible();e.ece=-.01;require(!CertificateGateR27.evaluate(e).active,"negative ECE blocked");
        e=eligible();e.worstBlock=1.01;require(!CertificateGateR27.evaluate(e).active,"invalid block proportion blocked");
        e=eligible();e.maxLosingStreak=-1;require(!CertificateGateR27.evaluate(e).active,"negative streak blocked");
        Object index=JsonR27.parse(Files.readString(Path.of(args[0])));
        Map<String,Object> report=RepairDiagnosisR32.build(index);
        List<Map<String,Object>> rows=(List<Map<String,Object>>)report.get("rows");
        require(rows.size()==165 && ((List<?>)report.get("coverageGaps")).isEmpty(),"165 distinct category/id entries in actual snapshot projection");
        require(rows.stream().filter(r->"F23".equals(r.get("id"))).anyMatch(r->"BINDING_UNAVAILABLE".equals(r.get("firstKnownGap"))),"F23 teacher binding identified");
        require(rows.stream().filter(r->"matrices".equals(r.get("group"))).allMatch(r->!Boolean.TRUE.equals(r.get("produced"))),"matrix absence retained");
        require(rows.stream().allMatch(r->r.containsKey("verificationEntry")&&r.containsKey("nextAction")),"each entry has an action and inspection point");
        Map<String,Object> bad=new LinkedHashMap<>((Map<String,Object>)index);bad.remove("matrices");
        require(!((List<?>)RepairDiagnosisR32.build(bad).get("coverageGaps")).isEmpty(),"missing category exposed");
        String[] attached=RepairDiagnosisR32.attach(new String[]{"LEGACY","{\"old\":1}"},report);
        Map<?,?> back=(Map<?,?>)JsonR27.parse(attached[1]);
        require(back.containsKey("old") && back.containsKey("capabilityRepairR32") && attached[0].contains("T15") && attached[0].contains("LEGACY"),"both upgrade formats preserve prior content and detail");
        CheckMetricCrosswalkR31.validateCoverage();
        require(CheckMetricCrosswalkR31.checks().size()==96 && CheckMetricCrosswalkR31.definitions().size()==44,"baseline production crosswalk 96 / 44");
        Files.writeString(Path.of(args[1]),JsonR27.write(Arrays.asList(HypothesisStoreR31.ddlR31())));
        System.out.println("passed="+checked);
    }
}
