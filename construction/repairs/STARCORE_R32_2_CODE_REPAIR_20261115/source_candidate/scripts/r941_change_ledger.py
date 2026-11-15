#!/usr/bin/env python3
"""Reproducible line/block-character diff against an explicitly supplied baseline."""
from pathlib import Path
import argparse, collections, difflib, hashlib, json, subprocess

def main():
    p=argparse.ArgumentParser();p.add_argument('--base',required=True);p.add_argument('--out',required=True,type=Path)
    a=p.parse_args();root=Path(__file__).resolve().parents[1];a.out.mkdir(parents=True,exist_ok=True)
    before=set(filter(None,subprocess.check_output(['git','ls-tree','-r','--name-only','-z',a.base],cwd=root,text=True).split('\0')))
    after=set(filter(None,subprocess.check_output(['git','ls-files','-z'],cwd=root,text=True).split('\0')))
    after.update(filter(None,subprocess.check_output(['git','ls-files','--others','--exclude-standard','-z'],cwd=root,text=True).split('\0')))
    ledger=[]
    for name in sorted(before|after):
        if not name.startswith(('app/','verification/','scripts/','release/','.github/')):continue
        old=subprocess.check_output(['git','show',a.base+':'+name],cwd=root) if name in before else b''
        path=root/name;new=path.read_bytes() if path.is_file() else b''
        if old==new:continue
        entry={'path':name,'before_bytes':len(old),'after_bytes':len(new),
               'before_sha256':hashlib.sha256(old).hexdigest() if name in before else None,
               'after_sha256':hashlib.sha256(new).hexdigest() if path.is_file() else None}
        try:o,n=old.decode('utf-8'),new.decode('utf-8')
        except UnicodeDecodeError:entry['kind']='binary';ledger.append(entry);continue
        ol,nl=o.splitlines(keepends=True),n.splitlines(keepends=True)
        added=deleted=char_added=char_deleted=0
        for op,i,j,k,l in difflib.SequenceMatcher(None,ol,nl,autojunk=False).get_opcodes():
            if op=='equal':continue
            deleted+=j-i;added+=l-k
            x,y=''.join(ol[i:j]),''.join(nl[k:l])
            for tag,u,v,w,z in difflib.SequenceMatcher(None,x,y,autojunk=False).get_opcodes():
                if tag!='equal':char_deleted+=v-u;char_added+=z-w
        assert len(n)-len(o)==char_added-char_deleted
        entry.update(kind='text',lines_added=added,lines_deleted=deleted,
                     chars_added=char_added,chars_deleted=char_deleted,net_chars=len(n)-len(o))
        ledger.append(entry)
    java=[e for e in ledger if e['path'].startswith('app/src/main/java/')]
    summary={'baseline':a.base,'scope':'app, verification, scripts, release, workflows',
             'method':'Unicode code points including newlines; SequenceMatcher line blocks, then character diff in each changed block',
             'files_changed':len(ledger),'production_java_files_changed':len(java)}
    for field in ['lines_added','lines_deleted','chars_added','chars_deleted']:
        summary['production_java_'+field]=sum(e.get(field,0) for e in java)
    (a.out/'CHANGE_SUMMARY.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2)+'\n')
    (a.out/'FILE_CHANGES.json').write_text(json.dumps(ledger,ensure_ascii=False,indent=2)+'\n')
    columns=['path','kind','lines_added','lines_deleted','chars_added','chars_deleted','net_chars','before_bytes','after_bytes','before_sha256','after_sha256']
    (a.out/'FILE_CHANGES.tsv').write_text('\t'.join(columns)+'\n'+'\n'.join('\t'.join(str(e.get(c,'')) for c in columns) for e in ledger)+'\n')
    print(json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':main()
