"""Measured logical-retention demonstrations; not Android disk measurements."""
import json
from pathlib import Path
import tempfile

from contracts import SCOPE, ObjectBudget, connect, install


def run():
    with tempfile.TemporaryDirectory() as tmp:
        db = connect(Path(tmp) / 'demo.db'); install(db)
        db.executescript('''CREATE TABLE hot(id INTEGER PRIMARY KEY,payload BLOB);
            CREATE TABLE keep(id INTEGER PRIMARY KEY,payload BLOB);
            CREATE TRIGGER move_before_delete BEFORE DELETE ON hot BEGIN
              INSERT OR IGNORE INTO keep VALUES(OLD.id,OLD.payload); END;
            CREATE TRIGGER forbid_delete_keep BEFORE DELETE ON keep BEGIN
              SELECT RAISE(ABORT,'protected'); END;''')
        for i in range(200):
            db.execute('INSERT INTO hot VALUES(?,?)', (i, b'x' * 40))
            db.execute('DELETE FROM hot WHERE id<=?', (i - 10,))
        legacy = {name: db.execute('SELECT count(*) FROM ' + name).fetchone()[0] for name in ['hot', 'keep']}
        legacy['total_rows'] = sum(legacy.values())
        legacy['incoming_rows'] = 200
        legacy['hot_retention_limit'] = 10
        legacy['finding'] = 'Deleting the hot tail moved originals; total retained rows kept growing.'
        budget = ObjectBudget(db, global_cap_bytes=128, emergency_disk_bytes=100)
        budget.configure_pool('models', 128)
        peak, successful_replacements = 0, 0
        for i in range(200):
            token, object_id = f'token-{i}', f'checkpoint-{i}'
            budget.reserve(token, 'models', 40, 10000, 'verified demo checkpoint')
            peak = max(peak, budget.used())
            budget.publish(token, object_id, 40, root_id='model:current')
            budget.mark_unreachable()
            budget.sweep(lambda _: None)
            successful_replacements += 1
        controlled = {'replacements': successful_replacements,
                      'retained_objects': db.execute('SELECT count(*) FROM object_catalog').fetchone()[0],
                      'peak_catalog_accounted_bytes': peak,
                      'final_catalog_accounted_bytes': budget.used(),
                      'file_unlink_in_this_example': 'SIMULATED_CALLBACK',
                      'byte_scope': 'CATALOG_LOGICAL_BYTES_NOT_PHYSICAL_DB_FILE'}
        assert legacy['total_rows'] == 200 and legacy['hot'] == 10
        assert controlled['retained_objects'] == 1 and peak <= 80 and budget.used() == 40
        db.close()
        return {'scope': SCOPE, 'status': 'PASS', 'before': legacy, 'after': controlled,
                'does_not_prove': ['Android disk usage', 'SQLite file shrinks after DELETE',
                                   'APK repair completed', 'Real-data predictive quality']}


if __name__ == '__main__':
    result = run()
    Path(__file__).with_name('demonstration_results.json').write_text(json.dumps(result, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps(result, ensure_ascii=False))
