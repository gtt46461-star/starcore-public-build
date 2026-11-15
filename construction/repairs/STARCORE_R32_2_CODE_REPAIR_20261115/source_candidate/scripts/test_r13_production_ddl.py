"""Execute the exact Java DDL; the reference SQL alone cannot detect generator bugs."""
import json,re,sqlite3
from pathlib import Path
root=Path(__file__).resolve().parents[1]
text=(root/'app/src/main/java/com/ethk3/novaonepro/strategyv2/StrategySchemaSU2.java').read_text()
body=text.split('String[] DDL = {',1)[1].split('    };',1)[0]
ddl=[json.loads(x) for x in re.findall(r'^\s*(".*"),?\s*$',body,re.M)]
with sqlite3.connect(':memory:') as db:
    for sql in ddl:db.execute(sql)
    for sql in ddl:db.execute(sql)
    assert db.execute("SELECT count(*) FROM pragma_table_info('su2_live_state')").fetchone()[0]==5
    insert="INSERT INTO su2_live_decision(genome_key,sequence_ordinal,position,number_mask,act,gscore,phat,frozen_at,target_open_time) VALUES('x',?,1,?,1,0,0.4,1,2)"
    db.execute(insert,(1,7))
    for sql,args in [(insert,(2,15)),("UPDATE su2_live_decision SET number_mask=11",())]:
        try:db.execute(sql,args)
        except sqlite3.IntegrityError:pass
        else:raise AssertionError('Invalid or mutable K3 accepted')
    db.execute('UPDATE su2_live_decision SET hit=1,settled_at=3')
    try:db.execute('UPDATE su2_live_decision SET hit=0')
    except sqlite3.IntegrityError:pass
    else:raise AssertionError('Settled record overwritten')
print('R13_PRODUCTION_DDL_PASS')
