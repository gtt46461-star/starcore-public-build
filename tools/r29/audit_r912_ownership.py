#!/usr/bin/env python3
"""Recompute the 22 direct R912 lane entrypoints and identify authority-sensitive source references.

This is a static scope review, not a transitive callgraph proof or Android execution receipt.
"""
import argparse
import hashlib
import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ENTRY = ROOT / 'app/src/main/java/com/ethk3/novaonepro/ResearchLearningContinuityR912.java'
SENSITIVE = ['TABLE_R35_SHADOW_OBSERVATION', 'TABLE_R35_OOS_LABEL', 'TABLE_R36_SETTLEMENT_TASK',
             'V51Repository.SELECTED', 'CanonicalObservationWriterR36', 'SettlementEngine']


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--out', type=Path, default=ROOT / 'evidence/r29/batch01/readers-events/r912-ownership-static.json')
    args = parser.parse_args()
    source = ENTRY.read_text()
    switch = source.split('private static String runLane(', 1)[1].split('default:', 1)[0]
    blocks = re.findall(r'case\s+(\d+)\s*:(.*?)(?=case\s+\d+\s*:|$)', switch, re.S)
    if sorted(int(n) for n, _ in blocks) != list(range(22)):
        raise SystemExit('R29_R912_LANE_INVENTORY_INCOMPLETE')
    sources = {}
    for name in subprocess.check_output(['rg', '--files', 'app/src/main/java'], cwd=ROOT, text=True).splitlines():
        path = ROOT / name
        if path.suffix == '.java':
            sources.setdefault(path.stem, []).append(path)
    lanes = []
    for number, block in sorted(blocks, key=lambda pair: int(pair[0])):
        refs = []
        for cls, method in sorted(set(re.findall(r'\b([A-Z][A-Za-z0-9]*)\.([a-z][A-Za-z0-9]*)\(', block))):
            for path in sources.get(cls, []):
                text = path.read_text()
                refs.append({'entry': cls + '.' + method, 'source': str(path.relative_to(ROOT)),
                             'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
                             'authorityTokensReferencedInFile': [token for token in SENSITIVE if token in text]})
        lane = int(number)
        lanes.append({'lane': lane, 'directEntrypoints': refs,
                      'retiredEntry': lane in (4, 5),
                      'confirmedMixedWriter': 'SettlementPostCommitSidecarR909' if lane == 2 else None,
                      'scopeReview': 'STATIC_DIRECT_ENTRYPOINTS_ONLY', 'androidExecution': 'NOT_RUN'})
    data = {'schema': 'R29_R912_OWNERSHIP_REVIEW_1', 'scope': 'STATIC_REVIEW_NOT_RUNTIME_PROOF',
            'entrySource': str(ENTRY.relative_to(ROOT)), 'entrySha256': hashlib.sha256(ENTRY.read_bytes()).hexdigest(),
            'laneCount': 22, 'lanes': lanes,
            'implementedMixedBoundary': {'lane': 2, 'source': 'SettlementPostCommitSidecarR909.reconcileIssue',
                'capture': 'TerminalProjectionR29.capture reads original Root terminal, formal predraw and strict OOS',
                'rootWrite': 'LegacyResearchScopeR29.realtimeWrite -> TerminalProjectionR29.projectSelected, only the existing selected row',
                'researchWrite': 'After Root commit returns, RP transaction writes derivative R906 closure/outbox',
                'forbidden': ['No RP-created formal shadow/OOS/terminal', 'No Root writer while RP writer held', 'No fallback completion timestamp']},
            'runtimeLimit': 'Direct targets and authority token references do not establish every nested consumer passed. Full production-lifecycle Android run remains required.'}
    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n')
    print('R29_R912_STATIC_LANE_INVENTORY|lanes=22|ANDROID_NOT_RUN')


if __name__ == '__main__':
    main()
