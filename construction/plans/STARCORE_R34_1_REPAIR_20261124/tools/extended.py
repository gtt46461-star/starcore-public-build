"""Rebuild or compare the additional source-direction crosswalk for the 604 report."""
import argparse
import hashlib
import json
from pathlib import Path
import re


SECTIONS = dict(GENERATION="08-05", OPERATORS="08-07", FAMILIES="06-05", ATOMS="08-03",
                REPAIR="08-05", STRUCTURE="08-01", LEDGER="11-05", STORAGE="10-01",
                ABLATION="08-07", MATRICES="08-03", TEACHERS="06-04", REFIT="08-04")
CHAPTERS = ["09-03", "08-05", "04-03", "03-03", "04-02", "07-03", "08-03", "09-04",
            "06-01", "08-07", "10-02", "11-05"]
EMBEDDED = {"pairedImprovementChart":"08-05", "capabilityCoverageChart":"11-03",
            "upgradeDiagnosisSources/ledger":"11-05", "upgradeDiagnosisSources/snapshots":"08-04",
            "ablationEvidence/model":"08-07", "ablationEvidence/policy":"08-07", "retention":"10-02"}
R26_ISSUES = {
    "IDENTITY":"01-02", "R27_01_WIN_RATE_TRUTH":"09-03", "R27_02_EVOLUTION_TRUTH":"08-05",
    "R27_03_CONTINUOUS_RUNTIME":"02-03", "R27_04_UPGRADE_CONTINUITY":"06-01",
    "R29_12_ACCEPTANCE_GAPS":"11-05", "REQUIREMENTS":"12-03", "DATA":"10-04",
    "OPPORTUNITY":"01-05", "FETCH":"04-06", "SETTLEMENT":"04-04", "FREEZE":"04-04",
    "AUTHORITY":"09-04", "ATOMS":"08-03", "FAMILIES":"06-02", "EXPERTS":"06-04",
    "TRAINING":"07-02", "EVOLUTION":"08-05", "FAILURES":"08-05", "ABLATION":"08-07",
    "EVIDENCE":"09-01", "CALIBRATION":"09-03", "RANKING":"07-03", "ACTION_SURFACE":"07-03",
    "OWNER":"04-04", "PERFORMANCE":"02-01", "BACKGROUND":"05-03", "RETENTION":"10-02",
    "RECOVERY":"05-02","UI":"11-04","SOURCE":"01-02","DEVICE":"12-04","CONCLUSION":"11-05",
    "POLICY_IDENTITY":"06-03","AVAILABILITY":"04-02","EXPERIMENT_LEDGER":"09-01",
    "TEST_BUDGET":"09-03","PUBLICATION_RECOVERY":"07-03","BUILD_IDENTITY":"12-01",
    "SUPPLY_CHAIN":"12-06","SLO_BUDGET":"05-03","MERGE":"12-03","CONTINUOUS_RUNTIME":"05-01",
    "FAIR_CAPABILITY_SERVICE":"02-03","LOSSLESS_IDENTITY":"06-01"}
TEXT = {
    "03b17336":("11-02","R967登记与正式实验统计"),"1ac48e37":("09-04","逐期生产fallback与正式授权"),
    "1d74ec58":("11-03","R975运行断点定位"),"4da09317":("11-02","R970历史资产库存及状态"),
    "7506a202":("11-02","统一软件运行状态"),"8357e49e":("11-05","CP12十四阶段业务漏斗"),
    "8411b545":("04-06","逐请求接收与源发布时间"),"8ebe303f":("05-03","冷/热路径实测时延"),
    "905c4941":("11-02","R925静态、实测与历史施工卡"),"9a2c2945":("07-02","原子组网络研究更新"),
    "a449b9b1":("07-03","深度网络、旧FTRL与模型使用分栏"),"a7c8c907":("04-02","数据脏行与可见时点"),
    "be34e792":("08-03","原子、分子、固定受保护资产"),"c0b455af":("04-04","逐目标业务水位"),
    "c4f6b2cb":("02-03","资源预算与研究服务能力"),"cc5a9225":("02-01","B05近120次资源测量"),
    "f3a35297":("06-03","创生/算法/公式与十四维护任务"),"f8d86637":("06-03","240个原子身份及角色"),
    "f9e8ede4":("03-02","数据库模式索引与运行能力分层")}


def load(p):
    return json.loads(p.read_bytes())


def subset(row, names):
    return {k:row[k] for k in names.split() if k in row}


def build(report, plan):
    report, plan = Path(report), Path(plan)
    read = lambda name: load(report / name)
    up, r30, r26 = (read(s) for s in ("UPGRADE_DIAGNOSIS.json", "R30_REPORT.json", "R26_REPORT.json"))
    scope = read("DIAGNOSTIC_SCOPE.json")
    refs = load(plan/"evidence/REPORT_COVERAGE.json")
    owners = {row["id"]:row["work_orders"] for group in ("metrics","diagnostic_rules") for row in refs[group]}
    issues = {i["id"]:i for b in load(plan/"WORK_ORDERS.json")["batches"] for i in b["issues"]}
    def route(id):
        keys=owners[id] if id in owners else ["R341-B"+id]
        return [{"issue":k,"cases":[a["id"] for a in issues[k]["acceptance"] if a["level"]=="ENGINEERING"]} for k in keys]
    def directed(payload, id, src):
        return {"source_locator":src, **payload,"routes":route(id)}
    sections = [directed(s, SECTIONS[s["section"]], "UPGRADE_DIAGNOSIS.json/r30Sections/"+str(n))
                for n,s in enumerate(up["r30Sections"])]
    projections=[]
    for k,s in up["sourceEvidenceR335"].items():
        if isinstance(s,dict):
            projections.append(directed({"id":k,**s},SECTIONS[k],"UPGRADE_DIAGNOSIS.json/sourceEvidenceR335/"+k))
    rules=[]
    for n,s in enumerate(r30["diagnosis"]["findings"]):
        facts=subset(s,"id metric scope strength status condition conclusion collectionAction refutedBy action")
        facts["observation"]=subset(s.get("evidence",{}),"source field value")
        rules.append(directed(facts,s["id"],f"R30_REPORT.json/diagnosis/findings/{n}"))
    metrics=[]
    for n,m in enumerate(r30["metricResults"]):
        facts=subset(m,"metricId name status missingReason value sampleCount nextTrigger repairCardIds target")
        facts["definition"]=subset(m["r31Definition"],"role requiredEvidence minimumSample minimumSampleScope collectionSource window target supportingChecks")
        metrics.append(directed(facts,m["metricId"],f"R30_REPORT.json/metricResults/{n}"))
    chapters=[]; nested=[]
    for n,s in enumerate(r30["chapters"]):
        src=f"R30_REPORT.json/chapters/{n}"
        chapters.append(directed(subset(s,"sectionId title status collectionAction reason evidenceLevel"),CHAPTERS[n],src))
        for key,issue in EMBEDDED.items():
            cur=s
            for field in key.split("/"):
                cur=cur.get(field,{})
            if "collectionAction" in cur:
                fields=subset(cur,"status reason source collectionAction records pairedImprovementLower95 qualityEffective")
                nested.append(directed(fields,issue,src+"/"+key))
    inventory=[]
    extra=[]
    for n,s in enumerate(r26["sections"]):
        lane=R26_ISSUES[s["sectionId"]];src=f"R26_REPORT.json/sections/{n}"
        inventory.append(directed(subset(s,"sectionId title status reason evidenceLevel requiredMetrics conclusion reading"),lane,src))
        for j,f in enumerate(s.get("findings",[])):
            extra.append(directed(f,"09-04",src+"/findings/"+str(j)))
    check_refs={}
    for f in sorted(report.glob("catalog-*.jsonl")):
        for line in f.open():
            row=json.loads(line)
            if row.get("kind")=="check_result":
                for name in set(re.findall(r'report-text-[0-9a-f]{64}\.txt',str(row["evidence"]))):
                    check_refs.setdefault(name,[]).append(row["id"])
    texts=[]
    for f in sorted(report.glob("report-text-*.txt")):
        sha=hashlib.sha256(f.read_bytes()).hexdigest()
        if f.name != "report-text-"+sha+".txt":
            raise ValueError("Report text digest mismatch "+f.name)
        issue,subject=TEXT[sha[:8]]
        texts.append(directed({"subject":subject,"sha256":sha,"catalog_direct_references":sorted(check_refs.get(f.name,[])),
            "scope":"辅助原件；来源与对象角色决定结论；重复页面引用沿用同一事实。"},issue,f.name))
    m=scope["multiHorizonProbeResearch"]
    outcome={"schema":"R341_FIELD_604_EXTENDED_DIRECTIONS_1","job":read("R31_SEMANTIC_RECEIPT.json")["jobId"],
             "source_commit":r26["sourceCommit"],"snapshot_sha256":scope["snapshotSha256"],
             "r30_sections":sections,"source_projections":projections,"rules":rules,"metric_triggers":metrics,
             "chapter_actions":chapters,"embedded_actions":nested,"r26_sections":inventory,
             "r26_finding_actions":extra,"report_text_sources":texts,
             "probe_protocol":directed(m,"04-05","DIAGNOSTIC_SCOPE.json/multiHorizonProbeResearch"),
             "strategy_assessment":directed(subset(r26["strategyAssessment"],"scope requiredCalendarOpportunities independentOpportunityUnit failureDirectedResearch evolution authorityGranted totalPolicies"),
                "08-05","R26_REPORT.json/strategyAssessment")}
    return outcome


def main():
    cli=argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--report-dir",type=Path,required=True);cli.add_argument("--plan",type=Path,required=True)
    group=cli.add_mutually_exclusive_group(required=True)
    group.add_argument("--out",type=Path);group.add_argument("--check","--compare",dest="check",type=Path)
    args=cli.parse_args()
    data=build(args.report_dir,args.plan)
    enc=lambda v: json.dumps(v,ensure_ascii=False,sort_keys=True,allow_nan=False)
    if args.check and enc(load(args.check))!=enc(data):
        cli.exit(1,"FAIL_EXTENDED_REPORT_DIRECTIONS\n")
    if args.out:
        args.out.write_text(json.dumps(data,ensure_ascii=False,indent=2,allow_nan=False)+"\n")
    print(json.dumps({k:len(v) for k,v in data.items() if isinstance(v,list)}))


if __name__=="__main__":
    main()
