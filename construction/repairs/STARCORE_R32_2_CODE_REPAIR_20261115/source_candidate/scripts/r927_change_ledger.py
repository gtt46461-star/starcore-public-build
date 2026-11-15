#!/usr/bin/env python3
"""Exact changed lines, Unicode codepoints, bytes and hashes against a selected baseline."""
import argparse,csv,difflib,hashlib,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
def git(*args):return subprocess.check_output(['git',*args],cwd=ROOT)
def digest(b):return hashlib.sha256(b).hexdigest()
def characters(a,b):
    # Line-aligned exact character edits avoid quadratic matching over huge unchanged files.
    plus=minus=0
    aa=a.splitlines(keepends=True);bb=b.splitlines(keepends=True)
    for tag,i,j,k,l in difflib.SequenceMatcher(None,aa,bb,autojunk=False).get_opcodes():
        if tag=='equal':continue
        old=''.join(aa[i:j]);new=''.join(bb[k:l])
        for op,x,y,u,v in difflib.SequenceMatcher(None,old,new,autojunk=False).get_opcodes():
            if op!='equal':minus+=y-x;plus+=v-u
    return plus,minus
def main():
    ap=argparse.ArgumentParser();ap.add_argument('--base',default='HEAD');args=ap.parse_args()
    names=git('diff','--name-only',args.base).decode().splitlines()
    names+=git('ls-files','--others','--exclude-standard').decode().splitlines()
    rows=[]
    for name in sorted(set(names)):
        if name.startswith('reports/r927/CHANGE_LEDGER'):continue
        old=subprocess.run(['git','show',args.base+':'+name],cwd=ROOT,capture_output=True).stdout
        p=ROOT/name;new=p.read_bytes() if p.is_file() else b''
        if old==new:continue
        row={'file':name,'old_bytes':len(old),'new_bytes':len(new),'byte_delta':len(new)-len(old),
             'old_sha256':digest(old),'new_sha256':digest(new)}
        try:
            if b'\0' in old+new:raise UnicodeError()
            a=old.decode('utf-8');b=new.decode('utf-8')
            plus,minus=characters(a,b);added=removed=0
            for tag,i,j,k,l in difflib.SequenceMatcher(None,a.splitlines(),b.splitlines(),autojunk=False).get_opcodes():
                if tag!='equal':removed+=j-i;added+=l-k
            row.update(lines_added=added,lines_deleted=removed,old_characters=len(a),new_characters=len(b),characters_added=plus,characters_deleted=minus)
        except UnicodeError:row.update(lines_added=None,lines_deleted=None,old_characters=None,new_characters=None,characters_added=None,characters_deleted=None)
        rows.append(row)
    result={'baseline_argument':args.base,'upstream_baseline':'b2439b2078434690c355890cf04083997e254fbf',
            'character_definition':'Unicode codepoints; line-aligned changed regions then exact SequenceMatcher autojunk=false; ledger excludes itself',
            'files':rows,'totals':{k:sum(r[k] or 0 for r in rows) for k in ['lines_added','lines_deleted','characters_added','characters_deleted','byte_delta']}}
    out=ROOT/'reports/r927';out.mkdir(exist_ok=True)
    (out/'CHANGE_LEDGER.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    with (out/'CHANGE_LEDGER.csv').open('w',newline='',encoding='utf-8-sig') as f:
        w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
    print(json.dumps({'files':len(rows),**result['totals']}))
if __name__=='__main__':main()
