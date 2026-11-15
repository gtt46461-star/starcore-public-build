#!/usr/bin/env python3
from pathlib import Path

P = Path('app/src/main/java/com/ethk3/novaonepro/AlgorithmInventionLabR908.java')
s = P.read_text(encoding='utf-8')
original = s

replacements = []

def replace_once(old, new, label):
    global s
    count = s.count(old)
    if count == 0 and new in s:
        print('already_applied=' + label)
        return
    if count != 1:
        raise SystemExit('R946_PATCH_CARDINALITY_' + label + '=' + str(count))
    s = s.replace(old, new, 1)
    replacements.append(label)

replace_once(
'''            c = store.readable().rawQuery(\n                    "SELECT COUNT(1) FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW\n                            + " WHERE target_issue=?", new String[]{safe(target)});''',
'''            c = store.readable().rawQuery(\n                    "SELECT COUNT(1) FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN "\n                            + CandidateExecutionAuditR932.TABLE + " e ON e.candidate_key=('algorithm:'||p.genome_id)"\n                            + " AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash"\n                            + " WHERE p.target_issue=?", new String[]{safe(target)});''',
'complete_predraw_count')

replace_once(
'''                            + " AND NOT EXISTS(SELECT 1 FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW\n                            + " p WHERE p.genome_id=g.genome_id AND p.target_issue=?) ORDER BY COALESCE((SELECT MAX(cp.target_issue) FROM v57_algorithm_predraw_r908 cp WHERE cp.genome_id=g.genome_id),'') ASC,CASE status WHEN 'CHALLENGER_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 ELSE 2 END,dev_wilson_lower DESC,generation DESC,updated_at ASC LIMIT ?",''',
'''                            + " AND NOT EXISTS(SELECT 1 FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN "\n                            + CandidateExecutionAuditR932.TABLE + " e ON e.candidate_key=('algorithm:'||p.genome_id)"\n                            + " AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash"\n                            + " WHERE p.genome_id=g.genome_id AND p.target_issue=?) ORDER BY COALESCE((SELECT MAX(cp.target_issue) FROM v57_algorithm_predraw_r908 cp WHERE cp.genome_id=g.genome_id),'') ASC,CASE status WHEN 'CHALLENGER_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 ELSE 2 END,dev_wilson_lower DESC,generation DESC,updated_at ASC LIMIT ?",''',
'heal_orphan_runnable')

replace_once(
'''                                            + "FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN "\n                                            + DbHelper.TABLE_R908_ALGORITHM_GENOME + " g ON g.genome_id=p.genome_id "\n                                            + "WHERE p.target_issue=? AND p.hit_label=-1 ORDER BY p.genome_id",''',
'''                                            + "FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN "\n                                            + DbHelper.TABLE_R908_ALGORITHM_GENOME + " g ON g.genome_id=p.genome_id JOIN "\n                                            + CandidateExecutionAuditR932.TABLE + " e ON e.candidate_key=('algorithm:'||p.genome_id)"\n                                            + " AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash "\n                                            + "WHERE p.target_issue=? AND p.hit_label=-1 ORDER BY p.genome_id",''',
'settle_only_complete_proof')

replace_once(
'''        Boolean ok = store.coordinatorR36().runWrite(\n                "R908_ALGORITHM_PREDRAW_FREEZE", new DatabaseCoordinator.WriteCallable<Boolean>() {''',
'''        Boolean ok = store.coordinatorR36().runAtomicWrite(\n                "R908_ALGORITHM_PREDRAW_FREEZE", new DatabaseCoordinator.WriteCallable<Boolean>() {''',
'atomic_freeze')

replace_once(
'''                    "ALREADY_FROZEN_FIRST_WRITE=" + Math.max(0, existing));''',
'''                    "ALREADY_COMPLETE_FROZEN=" + Math.max(0, existing));''',
'complete_existing_status')

if s == original:
    print('R946_ATOMIC_FREEZE_REPAIR_ALREADY_APPLIED')
else:
    P.write_text(s, encoding='utf-8')
    print('R946_ATOMIC_FREEZE_REPAIR_APPLIED=' + ','.join(replacements))

# Source-level invariants: fail hard rather than producing a cosmetic patch.
check = P.read_text(encoding='utf-8')
required = [
    'runAtomicWrite(\n                "R908_ALGORITHM_PREDRAW_FREEZE"',
    "CandidateExecutionAuditR932.TABLE + \" e ON e.candidate_key=('algorithm:'||p.genome_id)\"",
    'ALREADY_COMPLETE_FROZEN=',
]
for token in required:
    if token not in check:
        raise SystemExit('R946_REQUIRED_TOKEN_MISSING:' + token)
if 'runWrite(\n                "R908_ALGORITHM_PREDRAW_FREEZE"' in check:
    raise SystemExit('R946_NON_ATOMIC_FREEZE_STILL_PRESENT')
print('R946_ATOMIC_FREEZE_SOURCE_GATE=PASS')

