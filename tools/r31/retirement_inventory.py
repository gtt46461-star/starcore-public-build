#!/usr/bin/env python3
"""R31-B12-C04: builds tools/r31/retirement_manifest.json from the source tree (static scan = candidates only).

Sections:
  approved_drops        objects whose data may be removed for good. Empty in R31: no object has the required
                        inactivity + no-reference + equivalent replacement + recovery evidence.
  rebuild_in_place      every DROP TABLE in production code. Each is a same-transaction table rebuild (copy every row,
                        drop the old copy, recreate the original name) - data is kept, nothing is retired.
  derived_rebuilds      DROP TRIGGER / DROP INDEX sites (derived objects dropped to be recreated by their owner).
  candidates            static-scan retirement candidates with owner/readers/writers/dynamic SQL/reflection/persistent
                        references/replacement/observation/equivalence; decision is never DROP without evidence.
  preserved_capabilities every existing capability that must keep an executable implementation and a running test.
  dbstore               coverage and owner map of DbStore (no method is removed by this card:
                        DEFERRED_TARGET_SELECTION_AFTER_COVERAGE).
Usage: python3 tools/r31/retirement_inventory.py [--write | --check]
"""
import argparse, collections, hashlib, json, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
MAIN = ROOT / 'app/src/main/java'
OUT = ROOT / 'tools/r31/retirement_manifest.json'
DBSTORE = MAIN / 'com/ethk3/novaonepro/DbStore.java'
METHOD = re.compile(r'^    (?:(?:public|private|protected|static|synchronized|final)\s+)*[\w<>\[\],.?]+(?:<[^>]*>)?\s+(\w+)\s*\(')
NOT_METHOD = {'if', 'for', 'while', 'switch', 'catch', 'return', 'new', 'else', 'synchronized'}
DROP = re.compile(r'DROP\s+(TABLE|VIEW|INDEX|TRIGGER)\s+(?:IF\s+EXISTS\s+)?("?[\w$]+"?|"\s*\+\s*[\w.()]+)')
TABLE_REF = re.compile(r'\b(?:FROM|INTO|UPDATE|JOIN|TABLE(?:\s+IF\s+(?:NOT\s+)?EXISTS)?)\s+([a-z][a-z0-9_]{2,})\b')
CONST_REF = re.compile(r'\b(?:DbHelper\.)?(TABLE_[A-Z0-9_]+)\b')

REBUILDS = [
    {'object': 'b03_selected_event (legacy shape)', 'file': 'com/ethk3/novaonepro/DbHelper.java', 'dropped': 'TABLE_B03_SELECTED_EVENT + "_r30_shadow"',
     'copy': 'INSERT INTO ' + 'TABLE_B03_SELECTED_EVENT(all columns) SELECT ... FROM legacy', 'recreated_as': 'TABLE_B03_SELECTED_EVENT',
     'transaction': 'caller migration transaction (DbHelper upgrade owner)', 'rows': 'every column copied, then indexes and immutable triggers recreated',
     'reason': 'decision_action CHECK widened (BET/SHADOW/HOLD); SQLite cannot ALTER a CHECK'},
    {'object': 'su2_model_genome', 'file': 'com/ethk3/novaonepro/strategyv2/StrategySchemaSU2.java', 'dropped': 'su2_model_genome',
     'copy': 'INSERT INTO su2_model_genome_r16(columns) SELECT columns FROM su2_model_genome', 'recreated_as': 'su2_model_genome (renamed from su2_model_genome_r16)',
     'transaction': 'SU2 schema writer transaction (ensure requires inTransaction)', 'rows': 'all 13 columns copied',
     'reason': 'R13 2048-character program constraint widened for committees'},
    {'object': 'research_development_receipt_v2', 'file': 'com/ethk3/novaonepro/RetentionArtifactIdentityCP12.java', 'dropped': 'research_development_receipt_v2',
     'copy': 'INSERT INTO research_development_receipt_cp12_rebuild SELECT * FROM research_development_receipt_v2', 'recreated_as': 'research_development_receipt_v2 (renamed rebuild)',
     'transaction': 'CP12 retention-identity rebuild statement list, one writer transaction', 'rows': 'SELECT * copy after a foreign_key_check',
     'reason': 'artifact identity foreign key added; triggers recreated after the rebuild'},
]
CANDIDATES = [
    {'object': 'r30_keep_* tables', 'kind': 'TABLE_FAMILY', 'owner': 'KeepMigrationR31 / ResearchRetentionStoreR30',
     'readers': ['ResearchRetentionStoreR30', 'KeepMigrationR31'], 'writers': ['copy-on-delete triggers until SWITCH'],
     'dynamic_sql_or_reflection': 'table names built at run time by the retention owner', 'persistent_references': 'archive index r30_retention_archive_index',
     'replacement': 'evidence lifecycle references (KeepMigrationR31 REGISTER_REFERENCE/MIGRATE/SWITCH)',
     'observation': 'NOT_RUN on device (72h observation is a DEVICE item); upgrade paths not yet triggered cannot be excluded',
     'equivalence': 'R31KeepMigrationTest#aKillAtEveryStageLosesAndDuplicatesNothing (row digests equal through migration)',
     'decision': 'RETAIN_UNTIL_READERS_MIGRATED_AND_SCHEMA_MIGRATION_DECLARES_DROP'},
    {'object': 'r27_chart_cache legacy mainline projection (panel mainline/choice)', 'kind': 'ROW_FAMILY', 'owner': 'MainlineChoiceStoreR31',
     'readers': ['MainlineChoiceStoreR31.legacyChoice', 'MainlineBridgeSU2 (before TYPED_READ)'], 'writers': ['LiveShadowsR27.certify dual-write'],
     'dynamic_sql_or_reflection': 'none', 'persistent_references': 'r31_mainline_authority mode LEGACY_READ protects it by trigger',
     'replacement': 'r31_mainline_choice typed store (TYPED_READ after compareLegacyProjection)',
     'observation': 'dual read compared on host; device switch NOT_RUN', 'equivalence': 'HOST:r30repair_mainline_authority',
     'decision': 'RETAIN_DUAL_WRITE_UNTIL_TYPED_READ_SWITCH'},
    {'object': 'su1/su2 legacy research engines (SU1_*/SU2_* operations)', 'kind': 'ENGINE', 'owner': 'StrategySchemaSU1 / StrategySchemaSU2',
     'readers': ['MainlineBridgeSU2', 'ReportObjectIndexR30'], 'writers': ['SU1/SU2 registered operations in DatabaseCoordinator.OPERATION_REGISTRY_R31'],
     'dynamic_sql_or_reflection': 'SqlStatementsSU1 generated statement list', 'persistent_references': 'su1_migration_unit_v2 journal, certificates, matrix snapshots',
     'replacement': 'none proven equivalent', 'observation': 'NOT_RUN', 'equivalence': 'NOT_PROVEN', 'decision': 'RETAIN'},
    {'object': 'DbStore legacy generation methods', 'kind': 'CLASS_METHODS', 'owner': 'DbStore (sole DatabaseCoordinator writer)',
     'readers': ['see dbstore.methods[].callers'], 'writers': ['see dbstore.methods[].writes'],
     'dynamic_sql_or_reflection': 'see dbstore.methods[].dynamic', 'persistent_references': 'see dbstore.methods[].tables',
     'replacement': 'none selected', 'observation': 'coverage map only', 'equivalence': 'NOT_PROVEN',
     'decision': 'DEFERRED_TARGET_SELECTION_AFTER_COVERAGE'},
]
DERIVED_NOTES = {
    'com/ethk3/novaonepro/strategyv2/R29FoldBackR30.java': 'RETIRED_DERIVED (not recreated): R29 CP07-CP16 root triggers retired by the R30-B00-03 fold-back; replacement = R30 fenced owner protocol; data rows untouched; recovery = the fold-back route stays registered and retries on the next start (r30_foldback_error)',
    'com/ethk3/novaonepro/r952/ResearchMigrationRegistryR952.java': 'archive-only bypass of DELETE guards, generated from the actual sqlite_master; the same script recreates every guard',
}
PRESERVED = [
    ('Replay evaluation with probabilities', 'com.ethk3.novaonepro.strategyv2.ReplayEvaluatorR27#selectTraceWithProbabilities', ['com.ethk3.novaonepro.strategyv2.RepairPairedResearchR30Test#resumableRunnerMatchesActualOriginalProbabilitiesAndNeverReadsFormalLabels']),
    ('Paired component study (MASK_FROZEN / RETRAIN_REMOVED)', 'com.ethk3.novaonepro.strategyv2.PairedReplayRunnerR30R1#advance', ['com.ethk3.novaonepro.strategyv2.R31PairedStudyProtocolTest#bothProtocolsResumeExactlyAndTheRemovedArmNeverTouchesTheParentState']),
    ('Verified paired improvement', 'com.ethk3.novaonepro.strategyv2.VerifiedImprovementR31#record', ['com.ethk3.novaonepro.strategyv2.R31VerifiedImprovementTest#futureLabelsAreDetectedAndCannotForgeAnImprovement']),
    ('Research result consumption contract', 'com.ethk3.novaonepro.strategyv2.ResearchResultContractR31#validateForConsumption', ['com.ethk3.novaonepro.strategyv2.R31ResearchResultContractTest#lateLabelsWrongAbiRevisionParentAndRepeatedConsumptionAreRefused']),
    ('Certificate streak gate', 'com.ethk3.novaonepro.strategyv2.CertificateGateR27#streakGatePassProbabilityR31', ['com.ethk3.novaonepro.strategyv2.R31EvidenceContractTest#streakDpMatchesAnalyticValues']),
    ('Metric evaluation of 44 metrics', 'com.ethk3.novaonepro.strategyv2.MetricEvaluatorR29#evaluateR31', ['com.ethk3.novaonepro.strategyv2.R31MetricReportTest#all44MetricsAreDefinedComputedCollectedAndAcceptedWithoutFillingUnknowns']),
    ('Check/metric crosswalk', 'com.ethk3.novaonepro.strategyv2.CheckMetricCrosswalkR31#coverage', ['com.ethk3.novaonepro.strategyv2.R31MetricReportTest#checkIdsEqualTheReal96AndEveryMetricHasIndependentEvidence']),
    ('Family registry acceptance', 'com.ethk3.novaonepro.strategyv2.FamilyRegistryR26#acceptR31', ['com.ethk3.novaonepro.strategyv2.R31FamilyCapabilityAcceptanceTest#everyFamilyRowIsExecutedOrNamesItsDependency']),
    ('Creation budget', 'com.ethk3.novaonepro.strategyv2.CreationBudgetR31', ['com.ethk3.novaonepro.strategyv2.R31CreationBudgetTest#everyFamilyIsExploredEveryRoundOrNamesItsDependency']),
    ('Matrix research dependency state', 'com.ethk3.novaonepro.strategyv2.MatrixResearchR30R1#produceR31', ['com.ethk3.novaonepro.strategyv2.R31MatrixKindStateTest#missingStudentWaitsWhileSourceOnlyMatricesStillProduce']),
    ('Research measure tables', 'com.ethk3.novaonepro.strategyv2.ResearchMeasureProjectionR31', ['com.ethk3.novaonepro.strategyv2.R31ResearchMeasureTablesTest#unknownKindsAndWrongUnitsAreRefusedNotPlottedAsZero']),
    ('Keep-table migration', 'com.ethk3.novaonepro.strategyv2.KeepMigrationR31#step', ['com.ethk3.novaonepro.strategyv2.R31KeepMigrationTest#aKillAtEveryStageLosesAndDuplicatesNothing']),
    ('Evidence reachability GC', 'com.ethk3.novaonepro.StorageGcR31#claim', ['com.ethk3.novaonepro.R31EvidenceGcTest#rootedClosureIsKeptRootlessCycleCollectedAndPinOrClaimWinsAlone']),
    ('Chart registry', 'com.ethk3.novaonepro.ChartRegistryR31', ['com.ethk3.novaonepro.R31ChartRegistryTest#oneBlockedOrMalformedPanelLeavesTheOthersInBudget']),
    ('Mainline typed choice', 'com.ethk3.novaonepro.strategyv2.MainlineChoiceStoreR31#appendChoice', ['HOST:r30repair_mainline_authority', 'HOST:run_full_chain_fault_host']),
    ('Live settlement', 'com.ethk3.novaonepro.strategyv2.LiveShadowsR27#settle', ['HOST:run_full_chain_fault_host']),
    ('Overlay-install upgrade journal', 'com.ethk3.novaonepro.SchemaDomainCoordinatorR926#runUpgradeR31', ['HOST:run_upgrade_matrix_host']),
]


def dbstore():
    lines = DBSTORE.read_text(encoding='utf-8').split('\n')
    starts = [(i, m.group(1)) for i, l in enumerate(lines) for m in [METHOD.match(l)] if m and m.group(1) not in NOT_METHOD]
    others = {}
    for f in MAIN.rglob('*.java'):
        if f != DBSTORE:
            others[f] = f.read_text(encoding='utf-8', errors='replace')
    methods = []
    for k, (start, name) in enumerate(starts):
        end = starts[k + 1][0] if k + 1 < len(starts) else len(lines)
        body = '\n'.join(lines[start:end])
        call = re.compile(r'\.' + re.escape(name) + r'\s*\(')
        callers = sorted(str(f.relative_to(MAIN)) for f, t in others.items() if call.search(t))
        tables = sorted(set(TABLE_REF.findall(body)) | set(CONST_REF.findall(body)))
        methods.append({'name': name, 'callers': len(callers), 'caller_files': callers[:6], 'tables': tables[:12],
                        'writes': bool(re.search(r'execSQL|\binsert(?:OrThrow|WithOnConflict)?\(|\bupdate\(|\bdelete\(|runAtomicWrite', body)),
                        'dynamic': bool(re.search(r'rawQuery\([^;]*"\s*\+|execSQL\([^;]*"\s*\+|getDeclaredMethod|Class\.forName', body))})
    names = collections.Counter(m['name'] for m in methods)
    return {'source_sha256': hashlib.sha256(DBSTORE.read_bytes()).hexdigest(), 'method_declarations': len(methods), 'distinct_names': len(names),
            'no_external_caller': sorted(m['name'] for m in methods if m['callers'] == 0),
            'status': 'DEFERRED_TARGET_SELECTION_AFTER_COVERAGE', 'removed_by_this_card': [], 'methods': methods}


def drop_sites():
    rows = []
    for f in sorted(MAIN.rglob('*.java')):
        for n, line in enumerate(f.read_text(encoding='utf-8', errors='replace').split('\n'), 1):
            for kind, target in DROP.findall(line):
                t = target.strip('"'); t = 'expr:' + re.sub(r'[^\w.]', '', t) if '+' in t else t
                rows.append({'file': str(f.relative_to(MAIN)), 'line': n, 'kind': kind, 'target': t})
    return rows


def build():
    drops = drop_sites()
    tables = [d for d in drops if d['kind'] in ('TABLE', 'VIEW')]
    derived = collections.OrderedDict()
    for d in drops:
        if d['kind'] in ('TRIGGER', 'INDEX'):
            derived.setdefault(d['file'], []).append(d['target'])
    return {
        'format': 'R31_RETIREMENT_MANIFEST_V1', 'card': 'R31-B12-C04',
        'rule': 'Static scan lists candidates only. DROP of data is approved only with inactivity, no reference (static, dynamic SQL, reflection, persistent), an equivalent replacement and a recovery path; none qualifies in R31. Empty tables or few tasks are not a reason.',
        'approved_drops': [],
        'rebuild_in_place': REBUILDS,
        'drop_table_sites': tables,
        'derived_rebuilds': [{'file': f, 'objects': sorted(set(v)), 'recreated_by': DERIVED_NOTES.get(f, 'the same owner re-executes its CREATE after the drop')} for f, v in derived.items()],
        'candidates': CANDIDATES,
        'preserved_capabilities': [{'capability': c, 'implementation': i, 'tests': t} for c, i, t in PRESERVED],
        'dbstore': dbstore(),
    }


def main():
    ap = argparse.ArgumentParser(); ap.add_argument('--write', action='store_true'); ap.add_argument('--check', action='store_true'); a = ap.parse_args()
    text = json.dumps(build(), indent=1, ensure_ascii=False, sort_keys=False) + '\n'
    if a.write:
        OUT.write_text(text); print('R31_RETIREMENT_MANIFEST_WRITTEN', OUT.relative_to(ROOT))
    elif a.check:
        ok = OUT.is_file() and OUT.read_text() == text
        print('R31_RETIREMENT_MANIFEST_' + ('CURRENT' if ok else 'STALE')); sys.exit(0 if ok else 1)
    else:
        print(text[:2000])


if __name__ == '__main__':
    main()
