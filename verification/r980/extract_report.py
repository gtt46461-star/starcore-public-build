#!/usr/bin/env python3
"""Extract exact selected table snapshots from an R923/R979 diagnostic text or ZIP.
Never combines export timestamps into an atomic database backup. Fixture draw labels
are not synthesized by this extractor. The test harness explicitly does that later.
"""
from pathlib import Path
import argparse, hashlib, json, mmap, re, tempfile, zipfile, shutil
TABLES = ('v57_algorithm_genome_r908','research_lifecycle_r932',
          'research_candidate_cal_study_r932','research_candidate_cal_observation_r932',
          'research_candidate_cal_model_r932')

def extract(report: Path, output: Path) -> None:
    output.mkdir(parents=True, exist_ok=True)
    with report.open('rb') as f, mmap.mmap(f.fileno(), 0, access=mmap.ACCESS_READ) as mm:
        heads = {}
        for match in re.finditer(rb'(?m)^\[TABLE ([A-Za-z0-9_]+) readStartedAt=\d+\]\r?\n', mm):
            table = match.group(1).decode()
            if table in TABLES:
                if table in heads: raise ValueError('AMBIGUOUS_TABLE: ' + table)
                heads[table] = match
        records = []
        for table in TABLES:
            if table not in heads: raise ValueError('MISSING_TABLE: ' + table)
            match=heads[table]; end=mm.find(b'[END_TABLE ', match.end())
            if end < 0: raise ValueError('MISSING_END_TABLE: ' + table)
            body=mm[match.end():end]; endline=mm[end:mm.find(b'\n',end)].decode()
            raw=body.splitlines(); columns=raw[0].decode().split('\t'); rows=[]
            if not all(re.fullmatch(r'[A-Za-z0-9_]+', col) for col in columns):
                raise ValueError('INVALID_COLUMN_NAME: ' + table)
            def unescape(value):
                if value == r'\N': return None
                return re.sub(r'\\([\\trn])', lambda m:{'\\':'\\','t':'\t','r':'\r','n':'\n'}[m[1]], value)
            for line in raw[1:]:
                if not line: continue
                values=[unescape(v) for v in line.decode().split('\t')]
                if len(values)!=len(columns): raise ValueError('COLUMN_COUNT: ' + table)
                rows.append(dict(zip(columns,values)))
            expected=re.search(r'rows=(\d+)',endline)
            if expected is None or int(expected[1])!=len(rows):
                raise ValueError('ROW_COUNT_OR_TRUNCATION: ' + table)
            obj={'table':table,'columns':columns,'rows':rows,'source_offset':match.start(),
                 'raw_sha256':hashlib.sha256(body).hexdigest(),'end':endline,
                 'scope':'PER_TABLE_READ_TIME_NOT_GLOBAL_ATOMIC_BACKUP'}
            target=output/(table+'.json')
            target.write_text(json.dumps(obj,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
            records.append({'table':table,'rows':len(rows),'raw_sha256':obj['raw_sha256'],
                            'fixture_sha256':hashlib.sha256(target.read_bytes()).hexdigest(),
                            'source_offset':match.start()})
            print(table, 'rows='+str(len(rows)), 'raw_sha256='+obj['raw_sha256'])
        (output/'manifest.json').write_text(json.dumps(records,indent=2)+'\n')

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--report',required=True,type=Path)
    p.add_argument('--out',required=True,type=Path)
    a=p.parse_args()
    if not zipfile.is_zipfile(a.report): extract(a.report,a.out); return
    with zipfile.ZipFile(a.report) as z:
        texts=[i for i in z.infolist() if i.filename.lower().endswith('.txt') and i.file_size>1000000]
        if len(texts)!=1: raise ValueError('ZIP_REPORT_SELECTION_AMBIGUOUS: supply extracted report text')
        with tempfile.TemporaryDirectory(prefix='starcore-report-readonly-') as td:
            target=Path(td)/'diagnostic.txt'
            with z.open(texts[0]) as src,target.open('wb') as dst: shutil.copyfileobj(src,dst,1024*1024)
            extract(target,a.out)
if __name__=='__main__': main()
