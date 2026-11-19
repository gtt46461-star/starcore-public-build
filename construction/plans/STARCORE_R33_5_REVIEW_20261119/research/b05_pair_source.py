"""Generate the corrected complete DiagnosisEvidenceR335.java in an external staging directory."""
import argparse
import hashlib
import json
from pathlib import Path

BEFORE = '31fdbec19d5ae3aba21a727bda2b6cc06f14931b73c3683bfb545cfa68c3563a'
AFTER = '640bd8c8c0d45d8ff4a2b9f6ed1dcb0117c7bec619470dc025c4e43a8f293249'
REL = Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/DiagnosisEvidenceR335.java')

WINDOW = '''
    /** Read from the same frozen snapshot as the joined numerical summary. */
    private static long[] pairWindow(SQLiteDatabase db, String child, String parent, long start, long end) {
        if (child.equals(parent) || start < 0 || end <= start || end - start > 5000) {
            throw new IllegalArgumentException("R335_INVALID_LINEAGE_WINDOW");
        }
        String validRow = "typeof(sequence_ordinal)='integer' AND typeof(frozen_at)='integer' "
                + "AND typeof(target_open_time)='integer' AND frozen_at>0 AND frozen_at<target_open_time "
                + "AND typeof(act)='integer' AND act IN (0,1) AND ((hit IS NULL "
                + "AND (settled_at IS NULL OR (typeof(settled_at)='integer' AND settled_at=0))) "
                + "OR (typeof(hit)='integer' AND hit IN (0,1) AND typeof(settled_at)='integer' "
                + "AND settled_at>=target_open_time))";
        String sql = "SELECT sequence_ordinal,count(*),count(DISTINCT genome_key),"
                + "min(target_open_time),max(target_open_time),count(hit),"
                + "sum(CASE WHEN " + validRow + " THEN 0 ELSE 1 END) "
                + "FROM su2_live_decision WHERE genome_key IN (?,?) AND sequence_ordinal>=? "
                + "AND sequence_ordinal<? GROUP BY sequence_ordinal ORDER BY sequence_ordinal LIMIT 5001";
        long settledPairs = 0, pendingPairs = 0;
        String[] args = {child, parent, String.valueOf(start), String.valueOf(end)};
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                long arms = cursor.getLong(1);
                if (cursor.getLong(6) != 0 || arms != cursor.getLong(2) || arms > 2
                        || cursor.getLong(3) != cursor.getLong(4)) {
                    throw new IllegalArgumentException("R335_INVALID_PAIR_AT:" + cursor.getLong(0));
                }
                if (arms == 2) {
                    if (cursor.getLong(5) == 2) settledPairs++;
                    else pendingPairs++;
                }
            }
        }
        return new long[]{settledPairs, pendingPairs, end - start - settledPairs - pendingPairs};
    }
'''
PREPARE = '''                long[] window;
                try {
                    window = pairWindow(db, child, parent, start, sequence);
                } catch (IllegalArgumentException invalid) {
                    r.put("status", "UNKNOWN_INVALID_PAIR_ORIGINAL");
                    r.put("reason", invalid.getMessage());
                    r.put("pairedOpportunities", null);
                    r.put("pairedMeanOpportunityHitDifference", null);
                    r.put("qualityEffective", null);
                    out.add(r);
                    continue;
                }
                r.put("lineageWindowOrdinals", sequence - start);
                r.put("ordinalsMissingOneOrBothFreezes", window[2]);
                r.put("ordinalsPendingSettlement", window[1]);
'''
def digest(body):
    return hashlib.sha256(body).hexdigest()


def generate(plan, output):
    original = (plan/'B05/after_full'/REL).read_bytes()
    if digest(original) != BEFORE:
        raise ValueError('R335_B05_SOURCE_DRIFT')
    source = original.decode()
    edits = [
        ('    static boolean finite(', WINDOW+'    static boolean finite('),
        ('                long n=0,childActs=0,parentActs=0;', PREPARE+'                long n=0,childActs=0,parentActs=0;'),
        ('                r.put("pairedOpportunities",n);',
         '                if (n != window[0]) throw new IllegalStateException("R335_PAIRS_SNAPSHOT_COUNT_MISMATCH");\n'
         '                r.put("pairedOpportunities",n);')]
    for marker, replacement in edits:
        if source.count(marker) != 1:
            raise ValueError('R335_B05_METHOD_ANCHOR')
        source = source.replace(marker, replacement)
    body = source.encode()
    if digest(body) != AFTER:
        raise ValueError('R335_B05_OUTPUT_DRIFT:'+digest(body))
    path = output/REL
    if path.exists() and path.read_bytes() == body:
        return path
    path.parent.mkdir(parents=True,exist_ok=True)
    with path.open('xb') as stream:
        stream.write(body)
    return path



# Reproduced with the original full controlled-source identity() and R967 build().
IDENTITY_PINS = {
    "B05": [
        "edefebbdb57bac1a2a20e7c00395ac9ffe34669ba99a1061ce37cc59be900e11",
        "4cd79138d64f8a48499a8a08ad7294bae300c44dde8c5bc351add01e6dee4563",
        "26fe7c6048ef7bf8d6d715fea1f1a78ec8592b6d71489c95ca5ac1150c60976a"
    ],
    "B07": [
        "654bd89bfe91cfca255ec827faa4ddb68004e58d88104289f9bc17048d078570",
        "b58acd80066dea55828d5f86852e21cc2bac0144c7d527a6b5dbd0e3526be7ad",
        "5ec5f060920b815cdcdbc9f0b363f7b6d32f1dcd3fc1436206d25492812899af"
    ]
}
IDENTITY_REL = Path('release/r82_training_invalidation.json')

def generate_identity(plan, stage, output_dir):
    if stage not in ('B05', 'B06', 'B07', 'B08'):
        raise ValueError('STAGE_WITHOUT_PAIR_SOURCE')
    pins = IDENTITY_PINS['B05' if stage in ('B05', 'B06') else 'B07']
    original = (Path(plan)/stage/'semantic_inputs_r335'/IDENTITY_REL).read_bytes()
    if digest(original) != pins[0]:
        raise ValueError('R335_STAGE_R82_DRIFT')
    doc = json.loads(original)
    rows = [row for row in doc['controlled_changes'] if row['path'] == REL.as_posix()]
    if len(rows) != 1 or rows[0]['after_role'] != 'NUMERIC' or rows[0]['after_sha256'] != BEFORE:
        raise ValueError('R335_PAIR_NUMERIC_RECORD_DRIFT')
    if doc['decision'] != 'INVALIDATE_OLD_RULE' or doc['predecessors'] != []:
        raise ValueError('R335_PAIR_INVALIDATION_SCOPE')
    rows[0].update(after_sha256=AFTER, reviewer='R33.5 2026-11-19 source review',
                   rationale='B05 RE24 exact paired-window timing, missing and pending ordinal accounting; preserve NUMERIC role and invalidate old qualification.')
    doc['reviewed_input_provenance_sha256'] = pins[2]
    body = (json.dumps(doc, ensure_ascii=False, indent=2) + chr(10)).encode()
    if digest(body) != pins[1]:
        raise ValueError('R335_PAIR_R82_OUTPUT_DRIFT')
    path = Path(output_dir)/IDENTITY_REL
    if path.exists() and path.read_bytes() == body:
        return path
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open('xb') as stream:
        stream.write(body)
    return path

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan', type=Path)
    parser.add_argument('external_stage',type=Path)
    args=parser.parse_args()
    print(generate(args.plan,args.external_stage))
