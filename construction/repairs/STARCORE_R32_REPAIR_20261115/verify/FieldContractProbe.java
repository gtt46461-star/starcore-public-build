package com.ethk3.novaonepro.strategyv2;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Exercises the report patch through the real registries and query definitions. No SQLite or live ledger is modified. */
public final class FieldContractProbe {
    private static int assertions;

    private static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        System.out.println("PASS " + name);
        assertions++;
    }

    private static Map<?,?> object(Object value) { return (Map<?,?>) value; }

    private static List<?> entries(Map<?,?> report) { return (List<?>)report.get("rows"); }

    private static Map<?,?> find(Map<?,?> report, String group, String id) {
        for (Object item : entries(report)) {
            Map<?,?> row = object(item);
            if (group.equals(row.get("group")) && id.equals(row.get("id"))) return row;
        }
        throw new AssertionError(group + "/" + id);
    }

    private static Map<String,Object> copy(Map<?,?> obj) {
        Map<String,Object> result = new LinkedHashMap<>();
        for (Map.Entry<?,?> e : obj.entrySet()) result.put((String)e.getKey(), e.getValue());
        return result;
    }

    private static Map<String,Object> changeTable(Map<?,?> fixture, String id, Map<String,Object> change) {
        Map<String,Object> root = copy(fixture);
        Map<String,Object> section = copy(object(root.get("logicalTables")));
        List<Object> list = new ArrayList<>((List<?>)section.get("objects"));
        for (int i = 0; i < list.size(); i++) {
            Map<String,Object> row = copy(object(list.get(i)));
            if (id.equals(row.get("id"))) {
                row.putAll(change);
                list.set(i, row);
                section.put("objects", list);
                root.put("logicalTables", section);
                return root;
            }
        }
        throw new AssertionError("unknown fixture id");
    }

    public static void main(String[] args) throws Exception {
        Map<?,?> fixture = object(JsonR27.parse(Files.readString(Path.of(args[0]))));
        Map<String,Object> proto = RepairDiagnosisR32.build(fixture);
        check(((List<?>)proto.get("coverageGaps")).isEmpty(), "current five registries match snapshot IDs");
        check(entries(proto).size() == 165, "every 032 registry item gets its own row");
        Map<?,?> t06 = find(proto,"logicalTables","T06"), t10 = find(proto,"logicalTables","T10");
        check("PAGE_DEPENDENCY_BLOCKED".equals(t06.get("firstKnownGap")) &&
              ((List<?>)t06.get("frameReasons")).contains("LEGAL_TWO_COMPONENT_ADD_PARENT_REQUIRED"), "T06 reads blocked two-component frame");
        check("PAGE_DEPENDENCY_BLOCKED".equals(t10.get("firstKnownGap")) &&
              ((List<?>)t10.get("frameReasons")).contains("TWO_SAME_TARGET_PARENTS_REQUIRED"), "T10 reads blocked same-target parents");
        check("EMPTY_READ_PAGE".equals(find(proto,"logicalTables","T15").get("firstKnownGap")), "T15 empty receipt page stays empty");
        check("READ_PROJECTION_ONLY".equals(find(proto,"logicalTables","T16").get("firstKnownGap")), "T16 decision explanation never infers a real trade");
        Map<?,?> changed = RepairDiagnosisR32.build(changeTable(fixture,"T02",Map.of("id","T99")));
        List<?> gaps = (List<?>)changed.get("coverageGaps");
        check(gaps.contains("logicalTables unexpected=T99") && gaps.contains("logicalTables missing=T02"), "same-size substitution exposes missing and unexpected IDs");
        gaps = (List<?>)RepairDiagnosisR32.build(changeTable(fixture,"T02",Map.of("id","T03"))).get("coverageGaps");
        check(gaps.contains("logicalTables duplicate=T03") && gaps.contains("logicalTables missing=T02"), "duplicate cannot hide skipped table");
        changed=RepairDiagnosisR32.build(changeTable(fixture,"T06",Map.of("rows",List.of(Map.of("payload_json","{")))));
        check("FRAME_DETAIL_INSUFFICIENT".equals(find(changed,"logicalTables","T06").get("firstKnownGap")), "malformed frame is not a positive result");
        changed=RepairDiagnosisR32.build(changeTable(fixture,"T06",Map.of("status","READ_ERROR","reason","TABLE_FIRST_ROW_BYTE_BUDGET")));
        check("QUERY_FAILED".equals(find(changed,"logicalTables","T06").get("firstKnownGap")), "read failure remains distinct from zero rows");
        String[] attached=RepairDiagnosisR32.attach(new String[]{"原诊断","{\"existing\":true}"},proto);
        Map<?,?> json=object(JsonR27.parse(attached[1]));
        check(Boolean.TRUE.equals(json.get("existing")) && json.containsKey("capabilityRepairR32") && attached[0].contains("原诊断"), "both upgrade formats preserve the prior diagnosis");
        check(attached[0].contains("TWO_SAME_TARGET_PARENTS_REQUIRED") && attached[0].contains("LEGAL_TWO_COMPONENT_ADD_PARENT_REQUIRED"), "Markdown exposes the same concrete dependency reasons as JSON");
        for (String id : ResearchTableAdaptersR30R1.ids()) {
            Map<?,?> row=find(proto,"logicalTables",id);
            if (!StatsR27.sha256(ResearchTableAdaptersR30R1.query(id)).equals(row.get("registeredSqlHash")))
                throw new AssertionError("query hash mismatch " + id);
        }
        check(ResearchTableAdaptersR30R1.ids().size()==16, "sixteen registered SQL digests agree with production query()");
        Set<Object> actions = new HashSet<>();
        int families = 0;
        for (Object item : entries(proto)) {
            Map<?,?> row = object(item);
            if ("capabilities".equals(row.get("group")) && String.valueOf(row.get("id")).matches("[FIE][0-9]{2}")) {
                if (!actions.add(row.get("nextAction"))) throw new AssertionError("duplicate family action: " + row.get("id"));
                families++;
            }
        }
        check(families == 61, "61 individual experiments for families, ideas and experts");
        check(String.valueOf(find(proto,"capabilities","E15").get("verificationEntry")).contains("审计")
                 && String.valueOf(find(proto,"capabilities","I00").get("verificationEntry")).contains("同期对照"),
              "audit expert and control idea receive role-specific checks");
        List<Map<String,Object>> contracts=ResearchTableAdaptersR30R1.contracts();
        Files.writeString(Path.of(args[1]),JsonR27.write(contracts));
        Map<String,Object> sensitivity=new LinkedHashMap<>();
        for (double p : new double[]{.30,.34,.37,.40,.45,.50}) {
            sensitivity.put(String.valueOf(p),CertificateGateR27.streakGatePassProbabilityR31(250,p,6));
        }
        Files.writeString(Path.of(args[2]),JsonR27.write(sensitivity));
        System.out.println("assertions=" + assertions);
    }
}
