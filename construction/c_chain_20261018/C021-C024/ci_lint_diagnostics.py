from pathlib import Path
import subprocess,os,json,xml.etree.ElementTree as ET
R=Path(__file__).resolve().parents[3]
subprocess.run(['git','config','user.name','github-actions[bot]'],check=True)
subprocess.run(['git','config','user.email','41898282+github-actions[bot]@users.noreply.github.com'],check=True)
code=subprocess.run(['bash','./gradlew','--no-daemon','--no-build-cache',':app:lintRelease']).returncode
dest=R/'construction/c_chain_20261018/C021-C024/lint-diagnostics'/os.environ['GITHUB_RUN_ID'];dest.mkdir(parents=True)
for name in ['lint-results-release.xml','lint-results-release.html']:
 p=R/'app/build/reports'/name
 if p.exists():
  (dest/name).write_bytes(p.read_bytes())
  if name.endswith('.xml'):
   x=ET.parse(p)
   for issue in x.findall('issue'):
    if issue.get('severity') in ['Error','Fatal']:
     print('EXACT_LINT_ERROR|'+json.dumps(dict(issue.attrib,locations=[v.attrib for v in issue.findall('location')])),flush=True)
(dest/'status.json').write_text(json.dumps(dict(exit_code=code,scope='Complete current lint diagnostics; no product edits or baselines changed'))+'\n')
subprocess.run(['git','add','-f','--',str(dest.relative_to(R))],check=True)
subprocess.run(['git','commit','-m','test(C024): retain complete unchanged-source lint diagnostics'],check=True)
subprocess.run(['git','push','origin','HEAD:refs/heads/construction/c021-c024-20261021'],check=True)
