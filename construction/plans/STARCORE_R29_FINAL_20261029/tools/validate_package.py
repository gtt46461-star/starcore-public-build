#!/usr/bin/env python3
"""Validate plan references and standalone SQL contract, not the Android app."""
from pathlib import Path
import json,sqlite3,hashlib

def run(root):
    read=lambda n:json.loads((root/n).read_text())
    cards=read('registries/construction_cards.json');metrics=read('registries/acceptance_metrics.json')
    ids={c['id'] for c in cards};mids={m['id'] for m in metrics}
    assert len(cards)==len(ids)==52
    assert len(metrics)==len(mids)==44
    for c in cards:
        assert c['implementation_status']=='PLANNED_NOT_IMPLEMENTED'
        assert set(c['metrics'])<=mids and set(c['depends'])<=ids
        assert c['id'] not in c['depends']
        assert all(int(d[-3:])<int(c['id'][-3:]) for d in c['depends'])
    old=read('registries/legacy_requirements_preserved.json');assert len(old)==len({x['requirement_id'] for x in old})==1832
    assert len(read('registries/legacy_file_cards_preserved.json'))==299
    cross=read('registries/r28_to_r29_crosswalk.json');assert len(cross)==42
    assert all(set(x['r29'])<=ids for x in cross)
    paths={x['path'] for x in read('registries/repository_path_inventory.json')}
    for c in cards:
        for f in c['files']:
            if f['action']=='MODIFY' and f['path'].endswith('.java'):assert f['path'] in paths,f
    sql=(root/'contracts/r29_contract.sql').read_text();db=sqlite3.connect(':memory:');db.executescript(sql)
    db.execute("INSERT INTO experiment VALUES('e','p',1,'v',1,'2',.01,'hash')")
    db.execute("INSERT INTO frozen_prediction VALUES('f','e','2',1,1,2,3,1,'features','model',1,2,6000)")
    rejected=0
    for stmt in ["INSERT INTO experiment VALUES('e2','p2',1,'v',1,'2',.01,'hash')",
                 "INSERT INTO frozen_prediction VALUES('bad','e','3',1,1,1,3,1,'features','model',1,2,6000)",
                 "INSERT INTO frozen_prediction VALUES('late','e','3',1,1,2,3,1,'features','model',1,5999,6000)",
                 "INSERT INTO frozen_prediction VALUES('dup','e','2',1,1,2,3,1,'features','model',1,2,6000)"]:
        try:db.execute(stmt)
        except sqlite3.IntegrityError:rejected+=1
    assert rejected==4
    if (root/'MANIFEST.json').exists():
        for x in read('MANIFEST.json')['files']:assert hashlib.sha256((root/x['path']).read_bytes()).hexdigest()==x['sha256'],x['path']
    return {'scope':'PLAN_PACKAGE_AND_STANDALONE_SQL_ONLY','result':'PASS','cards':52,'metrics':44,'legacy_requirements':1832,'legacy_cards':299,'r28_crosswalk':42,'sql_invalid_cases_rejected':rejected,'android_build':'NOT_RUN','device_validation':'NOT_RUN','natural_future_quality':'NOT_RUN'}

if __name__=='__main__':print(json.dumps(run(Path(__file__).resolve().parents[1]),ensure_ascii=False,indent=2))
