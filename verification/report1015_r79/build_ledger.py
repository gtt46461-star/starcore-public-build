#!/usr/bin/env python3
"""Keep original facts/requirements/cards; add scoped evidence without bulk closure."""
import argparse,ast,csv,gzip,hashlib,io,json,re
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
H=lambda b:hashlib.sha256(b).hexdigest()
def csv_rows(path):
    raw=gzip.decompress(path.read_bytes()) if path.suffix=='.gz' else path.read_bytes()
    return list(csv.DictReader(io.StringIO(raw.decode('utf-8-sig'))))
def main():
    p=argparse.ArgumentParser();p.add_argument('--report',type=Path,required=True);p.add_argument('--engineer',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
    a.out.mkdir(parents=True,exist_ok=True)
    checks=json.loads((a.report/'checks_expanded.json').read_text());assert len(checks)==96
    old=[]
    for line in (ROOT/'verification/report1015/AUDIT.md').read_text().splitlines():
        if not re.match(r'^\|\d+\|',line):continue
        cols=line.strip('|').split('|');old.append({'id':'AUDIT-'+cols[0].zfill(2),'title':cols[1],'original_fact_and_reasoning':cols[2],'responsibility':cols[3],'inherited_validation':cols[4],'status':'REQUIRES_ITEM_ACCEPTANCE','closed':False})
    assert len(old)==22
    # The engineer's numbering is deliberately a separate namespace. Only the
    # explicit RSEC links from that document are represented as direct mappings.
    engineering=[
      ('P-01','请求到提交过慢',['033','052','074']),('P-02','单库写入争用推断待分阶段核实',['052','074']),
      ('P-03','数据库体积与原始动作/标签/修订数据',['006']),('P-04','请求终态与实时漏期',['010','305']),
      ('P-05','延后证据收敛',['031','305']),('P-06','后台与设备验收',['009','305']),
      ('P-07','watchdog历史锁超时记录',[]),('P-08','训练积压',['001','012']),('P-09','预算与服务率',['001']),
      ('P-10','盘前捕获与旧研究结算',['012','307']),('P-11','现代深度学习训练覆盖',['025','079','305']),
      ('P-12','创造变异融合逐期调度',['012','079']),('P-13','独立子代与资源淘汰',['307','321']),
      ('P-14','晋升漏斗',['003','012','024']),('P-15','多重比较与选择偏差',['307','007']),
      ('P-16','Number/Rank/Calibration/Policy冻结训练',['019','020','021','022','061','066']),
      ('P-17','生产链优势与固定基线',['059','079']),('P-18','度量/消融/对照',['054','055','056','057','058','068','069']),
      ('P-19','资格后的统一主链',['310','316']),('P-20','Owner与冲突审计',['067','070','071']),
      ('P-21','证据版本与大文件',['028','070']),('P-22','双源佐证与抓取写入',['010','052'])]
    valid={x['id'] for x in checks}
    eng=[{'id':'ENG-'+i,'title':title,'check_links':['RSEC-'+v for v in ids],'status':'REVALIDATE_AGAINST_CURRENT_SOURCE','closed':False}for i,title,ids in engineering]
    assert all(set(x['check_links'])<=valid for x in eng)
    facts=[dict(x,ledger_id='CHECK-'+x['id'],current_status='ORIGINAL_SNAPSHOT_REQUIRES_CURRENT_ACCEPTANCE',closed=False)for x in checks]
    req=csv_rows(ROOT/'construction/r997/REQUIREMENTS_LEDGER.csv.gz');cards=csv_rows(ROOT/'construction/r997/CARD_LEDGER.csv.gz');subs=csv_rows(ROOT/'construction/r997/SUBITEMS.csv')
    assert len(req)==1832 and len(cards)==299
    assert all(H(x['exact_requirement'].encode())==x['exact_requirement_sha256']for x in req)
    assert all(H(x['original_card'].encode())==x['original_card_sha256']for x in cards)
    new=[
      {'id':'AUDIT-23','title':'重置前合法预测被新epoch挡在结算队列外','links':['AUDIT-02','AUDIT-14','ENG-P-10'],
       'files':['AlgorithmProofRecoveryR948.java','CandidateExecutionAuditR932.java','AlgorithmInventionLabR908.java','ResearchPredrawSettlementPumpR908.java','ResearchShadowChallengerRuntimeR817.java'],
       'root_cause':'Original queue and settlement both required current head epoch even for original valid historical freezes. New path revalidates original proof/clocks/exact confirmed draw; labels only; no current training/Future/pruning/qualification.',
       'test':'HistoricalOutcomeR79.java','status':'GRADLE_PRODUCT_HOST_PASS_FINAL_RELEASE_AND_ANDROID_PENDING','closed':False},
      {'id':'AUDIT-24','title':'修订索引清理丢弃剩余静默预算','links':['AUDIT-03','ENG-P-03'],
       'files':['DataRetentionGovernorR900.java'],'root_cause':'One128-row scan per wake regardless of spare slice. Iterate64-row keyset pages, maximum2048, original deadline/P0/transaction/generation checks retained. Delete only generations without a live source row.',
       'test':'RevisionSweepR79.java','status':'GRADLE_PRODUCT_HOST_PASS_FINAL_RELEASE_AND_ANDROID_PENDING','closed':False},
      {'id':'AUDIT-25','title':'协调器状态投影每次占用5次写队列','links':['AUDIT-01','ENG-P-01','ENG-P-02'],
       'files':['BusinessReconcilerR36.java'],'root_cause':'Five independent optional state writes after authoritative work. Replace with existing atomic optional batch API. No owner/decision/settlement authority changes.',
       'test':'RequestHandoffR79.java: public reconciler, one transaction, second-write fault rollback','status':'GRADLE_PRODUCT_HOST_PASS_FINAL_RELEASE_AND_ANDROID_PENDING','closed':False},
      {'id':'AUDIT-26','title':'当前施工分支未进入既有Android CI推送过滤器','links':['AUDIT-18','ENG-P-06'],
       'files':['.github/workflows/o09-current-source-validation.yml'],'root_cause':'Push filter contains two older branches and omits current R78 construction branch. Add exact current branch, preserve existing gates and emulator assertions.',
       'test':'R79RecoveryAndroidTest and all existing Android runtime contracts on current branch','status':'CODE_CHANGED_REMOTE_RUN_PENDING','closed':False}]
    new.append({'id':'AUDIT-27','title':'结算交接入口的相对截止时间与固定截止合同冲突','links':['AUDIT-01','AUDIT-02','ENG-P-01','ENG-P-04','ENG-P-10'],
      'files':['SettlementCoordinatorR815.java'],'root_cause':'primeNextAnalysisRequestR947 supplied now+55000 while ensurePending requires exact draw-derived deadline. Public handoff fails with F01_REQUEST_DEADLINE_MISMATCH. Use unchanged TargetIssueResolver fixed deadline, expire without renewal.',
      'test':'RequestHandoffR79.java: baseline reproduction, immediate durable request, exact deadline/replay identity, expired rejection','status':'GRADLE_PRODUCT_HOST_PASS_FINAL_RELEASE_AND_ANDROID_PENDING','closed':False})
    for x in new:
        x['source_hashes']={f:H((ROOT/'app/src/main/java/com/ethk3/novaonepro'/f).read_bytes())for f in x['files']if not f.startswith('.')}
    obj={'revision':'R79_UNIFIED_LEDGER_V1','upstream_parent':'721904eff7be2cb597f0e13b858fc520b9b78b44',
      'original_report_sha256':'80efba6cf06cface46009621dae9aa4fed5b04edb9c98ea119e04204135c5275',
      'checks_expanded_sha256':H((a.report/'checks_expanded.json').read_bytes()),'engineer_text_sha256':H(a.engineer.read_bytes()),
      'all_work_closed':False,'inherited_verified_code_groups':9,'audit_work_items':old+new,'engineer_items':eng,'report_checks':facts,
      'requirements':req,'construction_cards':cards,'prior_subitems':subs,
      'status_boundary':'Original statuses preserved. A link or hash is traceability, not proof of execution or closure. Host synthetic fixtures never establish device timing, natural Future or a qualified user REAL.'}
    wire=json.dumps(obj,ensure_ascii=False,sort_keys=True,separators=(',',':')).encode()
    (a.out/'unified-ledger.json.gz').write_bytes(gzip.compress(wire,mtime=0))
    summary={'audit_items':len(old+new),'engineer_items':len(eng),'report_checks':len(facts),'requirements':len(req),'cards':len(cards),
       'requirements_original_statuses':{s:sum(x['current_status']==s for x in req)for s in sorted({x['current_status']for x in req})},
       'all_work_closed':False,'ledger_uncompressed_sha256':H(wire),'ledger_gzip_sha256':H((a.out/'unified-ledger.json.gz').read_bytes())}
    (a.out/'ledger-summary.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n');print(json.dumps(summary,ensure_ascii=False))
if __name__=='__main__':main()
