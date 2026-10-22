#!/usr/bin/env python3
"""Read-only report attribution: phone R75 evidence, not synthetic acceptance results."""
import argparse,collections,hashlib,json
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('report',type=Path);p.add_argument('out',type=Path);a=p.parse_args()
d=json.loads(a.report.read_text());t={e['table']:e['rows'] for e in d['recentEvidence']};counts={e['table']:e['rows'] for e in d['tables']}
state=lambda name,col:dict(collections.Counter(str(r[col]) for r in t[name]))
epoch=max((r for r in t['v57_runtime_epoch_r925'] if r['release_code']=='577251034'),key=lambda r:int(r['started_at']))
studies={r['study_hash']:r for r in t['research_candidate_cal_study_r932']}
post=[]
for r in t['research_capture_reservation_v2']:
 if int(r['created_at'])<int(epoch['started_at']):continue
 post.append({k:r[k] for k in ['candidate_key','freeze_hash','study_hash','first_issue','planned_n','state','next_issue','last_reason','created_at']} | {'study':studies.get(r['study_hash'])})
freezes=[]
for r in t['r967_frozen_evaluation_v2']:
 freezes.append({k:r[k] for k in ['candidate_id','learner_id','trained_until_issue','created_issue','update_endpoint','created_at','model_hash']} | {'ast_format':r['ast'].split('(')[0].split(':')[0], 'ast_prefix':r['ast'][:100]})
zero_tables=['research_candidate_cal_model_r932','research_calibration_quality_cp12','research_development_receipt_v2','research_fixed_window_r932','research_fixed_pair_r932','research_window_publication_r932','research_certificate_r929','research_mainline_contribution_r929']
output={'scope':'READ_ONLY_PHONE_REPORT_NOT_HOST_FIXTURE','report_sha256':hashlib.sha256(a.report.read_bytes()).hexdigest(),'release_epoch':epoch,'frozen_models':freezes,'post_install_reservations':post,'reservation_states':state('research_capture_reservation_v2','state'),'calibration_study_states':state('research_candidate_cal_study_r932','state'),'failures':state('r975_calibration_failure','reason'),'calibration_observations_total':counts['research_candidate_cal_observation_r932'],'calibration_settled_total':sum(int(r['settled_n']) for r in studies.values()),'downstream_counts':{k:counts.get(k) for k in zero_tables},'maintenance_lanes':[r for r in t['v57_research_lane_cursor_r967'] if str(r['lane']) in ['6','9']],'conclusion_zh':['R75 安装后两个旧 AST1 冻结模型均在 480 和 482 期缺失开奖前预测，合法新研究分别于 481、483 期开启。','当前每个研究仅抓取 2 期、校准结算 0 期，尚未产生校准拟合与质量结果；不能归因于胜率或稳定性不达标。','累计 7286 条 CAL 观察分属不同研究和冻结身份，不能拼作一个候选的正式 Future 或证书资格。','203 个 FAILED_GAP 不占用 8 个 active/reserved 容量；当前 2 个 active 预约仍在推进。','modern AST3 尚无 development receipt 或 frozen version；由训练 owner 专项修复。']}
a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n');print(a.out)
