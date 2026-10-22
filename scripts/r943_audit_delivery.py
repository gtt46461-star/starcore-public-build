#!/usr/bin/env python3
"""Record current evidence separately from the inherited 64-item acceptance ledger."""
from pathlib import Path
import collections, hashlib, json, subprocess

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'reports/r943'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def write_json(name, value):
    (OUT / name).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n')

def main():
    old = json.loads((ROOT / 'reports/r939/ACCEPTANCE_LEDGER.json').read_text())
    suite = (OUT / 'MODEL_SUITE_CURRENT.txt').read_text()
    tests = {
        'raw': 'R932_RAW_UPSTREAM_PASS', 'model': 'R932_MODEL_BUNDLE_PRODUCTION_PASS',
        'domain': 'R932_DOMAIN_ROUTE_PASS', 'cert': 'R932_UNIFIED_CERTIFICATE_PASS',
        'fixed': 'R932_FIXED_WINDOW_PASS', 'capture': 'R932_CAPTURE_TRANSACTION_PASS',
        'multi': 'R932_MULTIPLICITY_PASS', 'memory': 'R932_DUAL_MEMORY_PASS',
        'paired': 'R932_PAIRED_CHALLENGE_PASS', 'cohort': 'R932_COHORT_LIFECYCLE_PASS',
        'lifecycle': 'R932_LIFECYCLE_INTEGRITY_PASS', 'ablation': 'R932_MODULE_ABLATION_PASS',
        'atom': 'R932_ATOM_CAPTURE_PASS', 'million': 'R932_MILLION_ATOM_INDEX_PASS',
        'creation': 'R932_DIRECTED_CREATION_PASS', 'cal': 'R932_CALIBRATED_WINDOW_PASS',
        'modulecal': 'R932_MODULE_CALIBRATION_PASS', 'schedule': 'R932_GLOBAL_SCHEDULE_PASS',
        'publish': 'R932_WINDOW_PUBLICATION_PASS', 'slow': 'R932_SLOW_STATE_PASS',
    }
    evidence = {}
    for key, marker in tests.items():
        if marker not in suite:
            raise ValueError('CURRENT_SUITE_MISSING: ' + marker)
        evidence[key] = {'path': 'reports/r943/MODEL_SUITE_CURRENT.txt', 'marker': marker}
    for key, filename, marker in [
        ('pit', 'E03A_PIT_DAG_CURRENT.txt', 'R932_E03A_PIT_DAG_ACCEPTANCE_PASS'),
        ('sealed', 'E03AB_CURRENT.txt', 'R932_E03AB_ACCEPTANCE_PASS'),
        ('enh', 'ENHANCEMENT_GAP_CURRENT.txt', 'R932_ENHANCEMENT_GAP_PASS'),
        ('family', 'FAMILY_CHAMPION_CURRENT.txt', 'R932_FAMILY_CHAMPION_PASS'),
        ('legacy', 'C11_QUARANTINE_CURRENT.txt', 'R932_C11_QUARANTINE_LEGACY_PASS'),
        ('c12', 'C12_CURRENT.txt', 'R932_C12_ACCEPTANCE_PASS'),
        ('r943', 'TARGETED_RUNTIME_CURRENT.txt', 'R943_TARGETED_RUNTIME_PASS'),
        ('ranker', 'RANKER_COMPARISON_CURRENT.txt', 'R932_RANKER_COMPARISON_PASS|assertions=20'),
        ('trained', 'TRAINED_MODEL_PARITY_CURRENT.txt', 'R932_TRAINED_MODEL_PARITY_PASS'),
        ('bundle', 'TRAINING_BUNDLE_CURRENT.txt', 'R932_TRAINING_BUNDLE_ROUNDTRIP_PASS'),
        ('frozen', 'FROZEN_SELECTOR_CURRENT.json', 'SIGNED_FALSE_PARENT_BINDING_REJECTED_BY_PRODUCTION_LOADER'),
    ]:
        if marker not in (OUT / filename).read_text():
            raise ValueError('CURRENT_EVIDENCE_MISSING: ' + marker)
        evidence[key] = {'path': 'reports/r943/' + filename, 'marker': marker}
    mapping = {
        'D01': ['raw'], 'D02': ['pit', 'slow'], 'D03': ['sealed', 'model'],
        'D04': ['ranker', 'bundle'], 'D05': ['trained', 'ranker'], 'D06': ['cal', 'modulecal'], 'D07': ['domain'],
        'D08': ['domain', 'cert'], 'D10': ['model', 'memory'], 'C01': ['cert'],
        'C03': ['fixed', 'modulecal'], 'C06': ['multi', 'cal'], 'D09': ['frozen'],
        'C02': ['fixed', 'cal'], 'C04': ['capture'], 'C05': ['family', 'cohort'],
        'C07': ['schedule', 'r943'], 'C08': ['paired', 'cohort', 'lifecycle'],
        'C09': ['ablation'], 'C10': ['atom', 'million', 'r943'],
        'C11': ['legacy', 'atom'], 'C12': ['c12', 'creation', 'r943'],
        'E03A.1': ['raw'], 'E03A.2': ['pit'], 'E03A.3': ['pit'],
        'E03B.1': ['sealed'], 'E03B.2': ['sealed'], 'E03B.3': ['sealed'],
        'E03C.1': ['ranker', 'trained'], 'E03C.2': ['trained'], 'E03C.3': ['ranker'],
        'E03D.1': ['fixed'], 'E03D.2': ['modulecal'], 'E03D.3': ['frozen'],
        'E03E.1': ['enh'], 'E03E.2': ['domain'], 'E03E.3': ['enh'],
        'E03F.1': ['memory'], 'E03F.2': ['memory'], 'E03F.3': ['cert'],
        'E03G.1': ['cert'], 'E03G.2': ['model'], 'E03G.3': ['model'],
        'E04A.1': ['atom'], 'E04A.2': ['atom', 'creation'], 'E04A.3': ['enh'],
        'E04B.1': ['legacy'], 'E04B.2': ['ablation'], 'E04B.3': ['ablation'],
        'E04C.1': ['ablation'], 'E04C.2': ['cohort'], 'E04C.3': ['paired'],
        'E04D.1': ['cohort', 'schedule'], 'E04D.2': ['creation'],
        'E04D.3': ['schedule'], 'E04E.1': ['capture'],
        'E04E.2': ['lifecycle', 'cal'], 'E04E.3': ['fixed', 'multi'],
        'E04F.1': ['enh', 'r943'], 'E04F.2': ['frozen'], 'E04F.3': ['enh'],
        'E04G.1': ['enh'], 'E04G.2': ['cert'], 'E04G.3': ['enh'],
    }
    device = {'D02', 'D03', 'D04', 'D05', 'C07', 'E03A.1', 'E03C.2'}
    changed = {
        'C05': '按用户逐期训练要求，所有有效可执行候选取得评估机会；结构与行为家族仍共用统计身份，不能靠改名多得假设额度。',
        'C07': '原 8/24/64 数量上限改为持久分页名单；80 项中断恢复与 480 项名单验证通过，新版手机 5 秒延迟门仍待验证。',
        'E04D.1': '训练机会不再按结构别名直接剔除；统计假设去重、分母与证书授权仍独立执行。',
        'E04F.1': '主链候选准入命中率按用户要求为 35%；原联合效果目标的 40%/15%/ECE4% 保留为后续效果目标，两者不是同一指标。',
    }
    rows = []
    for original in old['rows']:
        key = original['id']
        current = [dict(evidence[k]) for k in mapping[key]]
        for item in current:
            item['sha256'] = digest(ROOT / item['path'])
        historical = []
        for item in original['evidence']:
            path = ROOT / item['path']
            historical.append({**item, 'scope': 'INHERITED_NOT_A_NEW_R943_RUN',
                               'file_present': path.is_file(),
                               'current_file_sha256': digest(path) if path.is_file() else None})
        status = 'CURRENT_HOST_EVIDENCE' if current else 'HISTORICAL_EVIDENCE_ONLY'
        if key in device:
            status += '_DEVICE_PENDING'
        if key in changed:
            status += '_USER_REQUIREMENT_UPDATED'
        rows.append({'id': key, 'batch': original['batch'], 'kind': original['kind'],
                     'requirements': original['requirements'], 'status': status,
                     'current_evidence': current, 'historical_evidence': historical,
                     'requirement_update': changed.get(key), 'new_device_verified': False,
                     'real_future_effect_proven': False})
    if len(rows) != 64 or len({r['id'] for r in rows}) != 64:
        raise ValueError('ORIGINAL_64_ITEM_COVERAGE_CHANGED')
    counts = dict(collections.Counter(r['status'] for r in rows))
    result = {'release': 'R943', 'scope': 'CURRENT_HOST_TESTS_AND_EXPLICIT_HISTORICAL_EVIDENCE',
              'reviewed': 64, 'main_tasks': 22, 'enhancements': 42,
              'rows_with_current_host_evidence': sum(bool(r['current_evidence']) for r in rows),
              'status_counts': counts, 'all_requirements_100_percent_verified': False,
              'physical_device_verified': False, 'future_effect_proven': False, 'rows': rows}
    write_json('BATCH34_CURRENT_AUDIT.json', result)
    lines = ['# 第三、第四批及加强项：R943 当前审计', '',
             '结论：不能声称此前或本次“百分百完美效果验收”。原 22 个主任务和 42 个加强条目已逐项核对。下表的主机证据仅证明相应测试场景，不能自动代表原条目的全部设备、时延及未来效果要求。', '',
             '旧 R939 的 64/64 是历史工程台账；原件保留，不能据此覆盖用户 R942 报告里的实际故障。当前证据保存独立日志和哈希；JSON 中同时保留原验收文字和历史证据，便于逐项复核。', '',
             f'64 项中 {result["rows_with_current_host_evidence"]} 项有本次相关主机重跑证据，其余项保留历史证据并明确未重跑。当前版本没有手机或模拟器运行证据。', '',
             '| 项目 | 当前状态 | 本次证据 | 原要求及变化 |', '|---|---|---|---|']
    for row in rows:
        sources = ', '.join(Path(e['path']).name + ':' + e['marker'] for e in row['current_evidence']) or '仅原台账中的历史文件'
        requirements = '；'.join(row['requirements'])
        if row['requirement_update']:
            requirements += '。本次变化：' + row['requirement_update']
        lines.append('| ' + ' | '.join([row['id'], row['status'], sources, requirements]).replace('\n', ' ') + ' |')
    lines += ['', '## 当前仍未闭合的验收', '',
              '- 新 APK 在用户手机上的新装、覆盖、持续后台、断网恢复、冷热启动及闪退复现。',
              '- 10 GB 老库在用户手机上的清理耗时、实际磁盘收缩和请求 P95；主机 6000→5000 记录及 B03 明细删除已经执行，不能代替旧大库手机实测。',
              '- 全量候选在手机每个实际周期内完成的比例及 5 秒主链预算；有界时间片和持久恢复已落地，但不是任意设备、任意数量候选的速度保证。',
              '- 独立真实未来窗口中的高胜率、高稳定、校准和参与频率；合成测试通过不会生成用户真实证书。',
              '', '本次没有用空函数、虚构计数或预置证书填补这些验收。当前源码、实际测试和未验证范围分别有可查文件。', '']
    (OUT / 'BATCH34_CURRENT_AUDIT.md').write_text('\n'.join(lines))
    inputs = []
    for folder in ['app/src/main', 'release']:
        for path in sorted((ROOT / folder).rglob('*')):
            if path.is_file():
                inputs.append({'path': path.relative_to(ROOT).as_posix(), 'bytes': path.stat().st_size,
                               'sha256': digest(path)})
    write_json('APK_INPUT_MANIFEST.json', inputs)
    dex = json.loads((OUT / 'FINAL_DEX_AUDIT.json').read_text())
    release = {'version_name': 'V57.21.1.0.12-R5.4.1.0.12-943', 'version_code': 577250943,
               'application_id': 'com.ethk3.novaonepro', 'branch': 'agent/r943-training-storage-atomic-world-20260913',
               'base_commit': '54e8b7c934f8f5558d572790e6b9d95607701e68',
               'apk_sha256': dex['apk_sha256'], 'apk_bytes': dex['apk_bytes'],
               'source_tree_sha256': dex['source_tree_sha256'], 'apk_input_files': len(inputs),
               'certificate_sha256': 'ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f',
               'signature_continuity': 'PASS', 'production_java_files': 659,
               'targeted_runtime_suites': 5, 'model_platform_suites': 22,
               'additional_host_suites': 9, 'runtime_regression_checks': 21,
               'batch34_manifest_contracts': 22, 'original_requirements_reviewed': 64,
               'new_physical_or_emulator_runtime': 'NOT_EXECUTED',
               'old_10gb_device_storage_reclamation': 'NOT_MEASURED',
               'future_effect_proven': False, 'all_requirements_100_percent_verified': False}
    write_json('RELEASE_RESULT.json', release)
    print(json.dumps({'reviewed': len(rows), 'with_current_host_evidence': result['rows_with_current_host_evidence'],
                      'apk_input_files': len(inputs), 'status_counts': counts}, ensure_ascii=False))

if __name__ == '__main__':
    main()
