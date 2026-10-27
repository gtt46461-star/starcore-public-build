package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Required metric names preserved verbatim from the sole R26 report registry. */
public final class ReportMetricContractR26 {
private ReportMetricContractR26() {}
public static final String REGISTRY_SHA256="9764e8cc8ec9404ad4d526088ef8fbfa71c73af22604d7ae8f700f6dce8cdb6c";
private static final Map<String,List<String>> SECTIONS;
static {Map<String,List<String>> sections=new LinkedHashMap<>();
sections.put("IDENTITY",Collections.unmodifiableList(Arrays.asList("repoCommit","generatedHash","apkHash","snapshotHash","exportedAt")));
sections.put("REQUIREMENTS",Collections.unmodifiableList(Arrays.asList("1832Legacy","299Cards","P001_P124","newCards","statusCounts")));
sections.put("DATA",Collections.unmodifiableList(Arrays.asList("rowCount","permutationValidity","duplicates","gaps","dirty","firstSeen")));
sections.put("OPPORTUNITY",Collections.unmodifiableList(Arrays.asList("scheduled","eligible","frozen","late","abstain","real","missingReason")));
sections.put("FETCH",Collections.unmodifiableList(Arrays.asList("request","response","upstreamPublished","validation","commit","rejectReason")));
sections.put("SETTLEMENT",Collections.unmodifiableList(Arrays.asList("pending","completed","noPredraw","duplicate","conflictingDraw","owner")));
sections.put("FREEZE",Collections.unmodifiableList(Arrays.asList("inputThrough","freezeTime","deadline","hash","deferredBlocks")));
sections.put("AUTHORITY",Collections.unmodifiableList(Arrays.asList("assetState","certificate","expiry","authorization","blockReason","nextTrigger")));
sections.put("ATOMS",Collections.unmodifiableList(Arrays.asList("registered","executed","trained","support","ablation","100Surfaces")));
sections.put("FAMILIES",Collections.unmodifiableList(Arrays.asList("mechanism","codeHash","updateState","performance","cost","status")));
sections.put("EXPERTS",Collections.unmodifiableList(Arrays.asList("task","proposal","ASTdiff","falsifier","experiment","acceptedReason")));
sections.put("TRAINING",Collections.unmodifiableList(Arrays.asList("labelsConsumed","weightDelta","optimizer","checkpoint","CAS","lag")));
sections.put("EVOLUTION",Collections.unmodifiableList(Arrays.asList("parent","child","NO_OP","attempted","structural","research","verified")));
sections.put("FAILURES",Collections.unmodifiableList(Arrays.asList("rootCause","counterEvidence","hit0","delayHit","owner","repairCard")));
sections.put("ABLATION",Collections.unmodifiableList(Arrays.asList("variant","commonIssues","delta","interval","budget","allAttempts")));
sections.put("EVIDENCE",Collections.unmodifiableList(Arrays.asList("history","holdout","prospective","birth","exposure","alphaBudget")));
sections.put("CALIBRATION",Collections.unmodifiableList(Arrays.asList("selectedP","rawP","bins","ECE","Brier","matchedBaseline","uncertainty")));
sections.put("RANKING",Collections.unmodifiableList(Arrays.asList("rankIdentity","hitRate","sample","MRR","NDCG","denominator")));
sections.put("ACTION_SURFACE",Collections.unmodifiableList(Arrays.asList("identity","probability","completeCount","checksum","nested7Stages")));
sections.put("OWNER",Collections.unmodifiableList(Arrays.asList("runtimeOwner","betOwner","settlementOwner","conflicts","activeReceipts")));
sections.put("PERFORMANCE",Collections.unmodifiableList(Arrays.asList("P50","P95","P99","max","OOM","CPU","heap","writerWait","deadlineMiss")));
sections.put("BACKGROUND",Collections.unmodifiableList(Arrays.asList("serviceBeat","businessBeat","screenState","fgsType","OSRestriction")));
sections.put("RETENTION",Collections.unmodifiableList(Arrays.asList("rawDraws<=5000","pins","DB","WAL","models","reports","temp","GC")));
sections.put("RECOVERY",Collections.unmodifiableList(Arrays.asList("crash","stall","IOError","lease","checkpoint","duplicates","recoveryResult")));
sections.put("UI",Collections.unmodifiableList(Arrays.asList("sourceMapping","unknownLabels","chartConsistency","mainThreadCost")));
sections.put("SOURCE",Collections.unmodifiableList(Arrays.asList("filesIndexed","bodiesAvailable","reviewed","staticCalls","runtimeReceipts")));
sections.put("DEVICE",Collections.unmodifiableList(Arrays.asList("device","API","APKhash","scenario","logHash","result","openLimit")));
sections.put("CONCLUSION",Collections.unmodifiableList(Arrays.asList("engineeringPass","qualityPass","coveragePass","mainlinePass","missingEvidence")));
sections.put("POLICY_IDENTITY",Collections.unmodifiableList(Arrays.asList("policyHash","checkpointHash","parentCheckpoint","ruleHash","consumedThrough","newBirthReason")));
sections.put("AVAILABILITY",Collections.unmodifiableList(Arrays.asList("firstSeen","upstreamKnown","freezeCutoff","clockState","lateLabelCount")));
sections.put("EXPERIMENT_LEDGER",Collections.unmodifiableList(Arrays.asList("attempted","failed","noOp","cpuSeconds","hypothesis","falsifier","allSeeds")));
sections.put("TEST_BUDGET",Collections.unmodifiableList(Arrays.asList("cohort","evidenceDigest","look","alphaReserved","deduplicated","precheckReason")));
sections.put("PUBLICATION_RECOVERY",Collections.unmodifiableList(Arrays.asList("prepared","committed","active","casVersion","crashPoint","recovery")));
sections.put("BUILD_IDENTITY",Collections.unmodifiableList(Arrays.asList("checkout","tree","generated","apk","signature","applicationId","versionCode","machineResults")));
sections.put("SUPPLY_CHAIN",Collections.unmodifiableList(Arrays.asList("dependencyLock","license","origin","hash","auditStatus")));
sections.put("SLO_BUDGET",Collections.unmodifiableList(Arrays.asList("metric","window","n","p95","p99","deadlineMisses","device","os","targetSdk")));
sections.put("MERGE",Collections.unmodifiableList(Arrays.asList("sourceAHash","sourceBHash","A32","B32","R2448","R2616","unmapped","conflicts")));
sections.put("CONTINUOUS_RUNTIME",Collections.unmodifiableList(Arrays.asList("session","blockedReason","resumeLatency","criticalDuration","lastActualProgress","chargingTelemetryOnly")));
sections.put("FAIR_CAPABILITY_SERVICE",Collections.unmodifiableList(Arrays.asList("familyService40","cpuMs","cursorDelta","oldestReadyAge","lag","missedServiceWindow","enabledButNeverRun")));
sections.put("LOSSLESS_IDENTITY",Collections.unmodifiableList(Arrays.asList("protocolVersion","goldenVectors","legacyMapping","floatCollision","pairedLowerRegression","NO_OPRegression")));
SECTIONS=Collections.unmodifiableMap(sections);}
public static List<String> metrics(String id){List<String> result=SECTIONS.get(id);if(result==null)throw new IllegalArgumentException("R26_UNKNOWN_REPORT_SECTION:"+id);return result;}
public static int sections(){return SECTIONS.size();}
}
