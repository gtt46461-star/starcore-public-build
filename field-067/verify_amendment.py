"""Verify both complete repair images, object modes, original parentage and the exact cumulative source trees."""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import zipfile

ORIGINAL='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d'
BASE='bb7b49bbcc9266bad548ec37e71403256fc06a4d'
FIXES=('809a39e11856385326937f27beaf5520df1b332a','43fef2458c33e7c6140d9822ae91ad1bf4cc6529')
def git(repo,*args,stdin=None):
    return subprocess.check_output(['git','-C',str(repo),*args],input=stdin)
def tree(repo,commit):
    entries={}
    for item in git(repo,'ls-tree','-rz',commit).split(b'\0'):
        if item:
            header,name=item.split(b'\t',1)
            mode,typ,obj=header.decode().split()
            entries[name.decode()]=(mode,typ,obj)
    return entries
def check(repo,archive):
    git(repo,'merge-base','--is-ancestor',BASE,ORIGINAL)
    output=[];parent=ORIGINAL
    with zipfile.ZipFile(archive) as z:
        assert z.testzip() is None
        manifest=json.loads(z.read('field-amendment.json'))
        assert [p['commit'] for p in manifest['revisions']]==list(FIXES)
        assert len(z.namelist())==len(set(z.namelist()))
        for revision in manifest['revisions']:
            commit=revision['commit'];assert revision['parent']==parent and git(repo,'rev-parse',commit+'^').decode().strip()==parent
            before=tree(repo,parent);after=tree(repo,commit);rebuilt=before.copy()
            paths=set(before)|set(after)
            changes={k for k in paths if before.get(k)!=after.get(k)}
            assert changes=={p['path'] for p in revision['files']} and len(changes)==len(revision['files'])
            for row in revision['files']:
                k=row['path']
                for which,ref in [('before',before),('after',after)]:
                    v=row[which]
                    if v is None:
                        assert k not in ref
                        if which=='after':rebuilt.pop(k)
                        continue
                    data=z.read(v['image']);expected=ref[k]
                    assert expected==(v['mode'],'blob',v['gitBlob'])
                    assert git(repo,'hash-object','--stdin',stdin=data).decode().strip()==v['gitBlob']
                    assert hashlib.sha256(data).hexdigest()==v['sha256'] and len(data)==v['bytes']
                    if which=='after':rebuilt[k]=expected
            assert rebuilt==after
            tree_id=git(repo,'rev-parse',commit+'^{tree}').decode().strip()
            assert revision['tree']==tree_id and git(repo,'show',commit+':release/release_manifest.json')
            output.append({'commit':commit,'tree':tree_id,'beforeFiles':len(before),'afterFiles':len(after),'amendedPaths':len(changes),'untouchedPaths':len(paths-changes)})
            parent=commit
        assert json.loads(git(repo,'show',FIXES[-1]+':release/release_manifest.json'))['versionCode']==577311067
    return {'status':'VERIFIED_COMPLETE_FIELD_REPAIR_IMAGES','sourceBaseline':BASE,'originalB08':ORIGINAL,'steps':output}
if __name__=='__main__':
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('repo',type=Path);p.add_argument('archive',type=Path);arg=p.parse_args()
    print(json.dumps(check(arg.repo,arg.archive),indent=2))
