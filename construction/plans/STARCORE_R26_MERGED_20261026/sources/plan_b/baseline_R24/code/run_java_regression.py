"""Pure-Java regression; no Android release claim."""
from pathlib import Path
import subprocess,tempfile,shutil
p=Path(__file__).resolve().parents[1]
if not shutil.which('javac') or not shutil.which('java'):raise SystemExit('JDK required')
rel=Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/MainlinePolicySU2.java')
for original,src in [(True,p/'evidence/source_verified'/rel),(False,p/'patches/after'/rel)]:
 with tempfile.TemporaryDirectory(prefix='r24java_') as td:
  d=Path(td);shutil.copy2(src,d/'MainlinePolicySU2.java');shutil.copy2(p/'tests/java/R24MainlineRegression.java',d/'R24MainlineRegression.java')
  subprocess.run(['javac','-encoding','UTF-8','-d',str(d),str(d/'MainlinePolicySU2.java'),str(d/'R24MainlineRegression.java')],check=True)
  subprocess.run(['java','-cp',str(d),'com.ethk3.novaonepro.strategyv2.R24MainlineRegression']+(['original'] if original else []),check=True)
