"""Compile exact production numerical owners; compare old and corrected B04 behavior."""
import argparse
import ast
import hashlib
import json
import shutil
import subprocess
import tempfile
from pathlib import Path


def run(plan, owner, mode):
    root = plan / 'B04'
    manifest = json.loads((root/'SOURCE_MANIFEST.json').read_bytes())
    for entry in manifest['entries']:
        body = (root/'baseline'/entry['file']).read_bytes()
        git_blob = hashlib.sha1(b'blob '+str(len(body)).encode()+b'\0'+body).hexdigest()
        if hashlib.sha256(body).hexdigest()!=entry['sha256'] or git_blob!=entry['gitBlob']:
            raise ValueError('ORIGINAL_NUMERICAL_SOURCE_DRIFT:'+entry['file'])
    # Reuse the exact published TYPE-ONLY/THROWING boundary seams, never a numerical substitute.
    module = ast.parse((root / 'tests/run_session_numerics.py').read_text())
    assignments = {n.targets[0].id: n.value for n in module.body
                   if isinstance(n, ast.Assign) and isinstance(n.targets[0], ast.Name)}
    seams = ast.literal_eval(assignments['seams'])
    production = ast.literal_eval(assignments['production'])
    java = shutil.which('java')
    if not java:
        raise ValueError('JDK_REQUIRED')
    outputs = {}
    with tempfile.TemporaryDirectory() as temp:
        working = Path(temp)
        files = []
        for name, body in seams.items():
            path = working / 'src' / name
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(body)
            files.append(path)
        for name in production:
            if name == 'AtomNetSessionR30.java':
                path = working / 'owner' / name
                path.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(owner, path)
            else:
                candidate = root/'payload/app/src/main/java/com/ethk3/novaonepro/strategyv2'/name
                path = candidate if candidate.exists() else root/'baseline'/name
            files.append(path)
        files += [root/'after/AtomNetProgressR33.java',
                  Path(__file__).with_name('AtomNetSessionNumericalProbe.java'),
                  Path(__file__).with_name('AtomNetSessionResumeReviewTest.java')]
        classes = working/'classes'
        classes.mkdir()
        subprocess.run([java,'-m','jdk.compiler/com.sun.tools.javac.Main','-source','17','-target','17',
                        '-d',str(classes),*map(str,files)],check=True)
        for test, args in [('AtomNetSessionResumeReviewTest',[mode]),
                           ('AtomNetSessionNumericalProbe',[])] if mode == 'fixed' else [('AtomNetSessionResumeReviewTest',[mode])]:
            result = subprocess.run([java,'-cp',str(classes),
                       'com.ethk3.novaonepro.strategyv2.'+test,*args],
                       check=True,capture_output=True,text=True)
            outputs[test] = result.stdout.strip()
        version = subprocess.run([java,'-version'],capture_output=True,text=True,check=True)
        return {'mode':mode,'ownerSha256':hashlib.sha256(owner.read_bytes()).hexdigest(),
                'java':version.stderr.strip() or version.stdout.strip(),
                'tests':outputs,'productionOwners':production,
                'inputSourceSha256':{path.name:hashlib.sha256(path.read_bytes()).hexdigest()
                                     for path in files},
                'scope':'ACTUAL_NUMERICAL_OWNERS_WITH_PUBLISHED_THROWING_DB_AND_CALENDAR_BOUNDARIES',
                'notRun':['Android','database_publication','natural_future_quality']}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('plan',type=Path)
    parser.add_argument('owner',type=Path)
    parser.add_argument('--mode',choices=['legacy','fixed'],required=True)
    args = parser.parse_args()
    print(json.dumps(run(args.plan,args.owner,args.mode),ensure_ascii=False,indent=2))
