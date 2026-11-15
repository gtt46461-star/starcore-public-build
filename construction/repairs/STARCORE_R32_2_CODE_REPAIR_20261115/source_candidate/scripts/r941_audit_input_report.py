#!/usr/bin/env python3
"""Verify the supplied report and reconstruct only its exported tables as evidence.

The output is NOT the phone database and must never be loaded into production.
"""
import argparse, base64, collections, hashlib, json, re, sqlite3
from pathlib import Path

def decode(value):
    if value == r'\N':
        return None
    if value.startswith('base64:'):
        return base64.b64decode(value[7:], validate=True)
    return re.sub(r'\\([\\trn])', lambda m: {'\\':'\\','t':'\t','r':'\r','n':'\n'}[m[1]], value)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('report', type=Path)
    parser.add_argument('--out', required=True, type=Path)
    args=parser.parse_args(); args.out.mkdir(parents=True,exist_ok=True)
    raw=args.report.read_bytes()
    footer=re.search(rb'\nFILE_BODY_BYTES=(\d+)\nFILE_BODY_SHA256=([0-9a-f]{64})\n',raw)
    if not footer or not raw.endswith(b'END_OF_COMPLETE_FILE\n'):
        raise ValueError('REPORT_FOOTER_MISSING')
    length=int(footer[1]); expected=footer[2].decode()
    if length!=footer.start() or hashlib.sha256(raw[:length]).hexdigest()!=expected:
        raise ValueError('REPORT_BODY_HASH_MISMATCH')
    database=args.out/'report_evidence.sqlite'
    if database.exists():
        raise FileExistsError('Choose a new evidence output directory; existing evidence is preserved')
    db=sqlite3.connect(database); tables={}; table=None; header=None; rows=0
    try:
        db.execute('CREATE TABLE source_report_metadata(key TEXT PRIMARY KEY,value TEXT)')
        db.executemany('INSERT INTO source_report_metadata VALUES(?,?)',[
            ('scope','EXPORTED_REPORT_TABLES_ONLY_NOT_PHONE_BACKUP_NOT_PRODUCTION_INPUT'),
            ('input_sha256',hashlib.sha256(raw).hexdigest()),('body_sha256',expected)])
        for number,line in enumerate(raw.decode('utf-8-sig').splitlines(),1):
            start=re.fullmatch(r'\[TABLE ([A-Za-z0-9_]+)\]',line)
            if start:
                if table is not None: raise ValueError('NESTED_TABLE')
                table=start[1]; header=None; rows=0; continue
            if table is None: continue
            end=re.fullmatch(r'\[END_TABLE rows=(\d+) status=(.+)\]',line)
            if end:
                if int(end[1])!=rows or end[2]!='OK': raise ValueError('TABLE_NOT_COMPLETE:'+table)
                tables[table]=rows; table=None; continue
            if header is None:
                header=line.split('\t')
                if not all(re.fullmatch(r'[A-Za-z0-9_]+',n) for n in header): raise ValueError('COLUMN_IDENTIFIER')
                db.execute('CREATE TABLE "'+table+'"('+','.join('"'+n+'" TEXT' for n in header)+')')
                sql='INSERT INTO "'+table+'" VALUES('+','.join('?' for _ in header)+')'
            else:
                fields=line.split('\t')
                if len(fields)!=len(header): raise ValueError('TSV_COLUMN_COUNT:'+str(number))
                db.execute(sql,[decode(value) for value in fields]); rows+=1
        if table is not None: raise ValueError('UNCLOSED_TABLE')
        db.commit()
        if db.execute('PRAGMA quick_check').fetchone()[0]!='ok': raise ValueError('EVIDENCE_SQLITE_INVALID')
        db.row_factory=sqlite3.Row
        attempts=[dict(r) for r in db.execute("SELECT * FROM v57_attempt_resource_r927 WHERE release_code='577250939'")]
        candidates=[dict(r) for r in db.execute("SELECT genome_id,display_name_zh,future_n,future_hit_rate FROM v57_algorithm_genome_r908 WHERE CAST(future_n AS INTEGER)>=1000 AND CAST(future_hit_rate AS REAL)>=0.35 AND CAST(future_wilson_lower AS REAL)>=0.30 AND CAST(future_block_min AS REAL)>=0.22 AND CAST(future_regime_min AS REAL)>=0.18 AND CAST(future_coverage AS REAL)>=0.50 AND may_authorize_real='0' AND status NOT LIKE 'PRUNED%' AND status NOT LIKE '%QUARANTIN%' AND length(structural_hash)=64 AND trained_until_issue<>''")]
        result={'input_sha256':hashlib.sha256(raw).hexdigest(),'body_hash_verified':True,
                'body_bytes':length,'tables':tables,'r939_attempts':len(attempts),
                'r939_statuses':dict(collections.Counter(r['status'] for r in attempts)),
                'r939_peak_heap_bytes':max(int(r['heap_peak_bytes']) for r in attempts),
                'r939_max_lock_wait_ms':max(int(r['lock_wait_ms']) for r in attempts),
                'l4_admission_eligible_from_report':candidates,
                'limitations':'仅重建报告导出的原始表；不是手机备份，不能倒补赛前证据或授予REAL。滚动窗口包含升级前时段，不能将全部缺口归因于R939。'}
        (args.out/'INPUT_REPORT_AUDIT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
        (args.out/'R939_ATTEMPTS.json').write_text(json.dumps(attempts,ensure_ascii=False,indent=2)+'\n')
        print(json.dumps({'status':'PASS','tables':len(tables),'rows':sum(tables.values()),'r939_attempts':len(attempts),'scope':'REPORT_EVIDENCE_ONLY'}))
    finally: db.close()

if __name__=='__main__': main()
