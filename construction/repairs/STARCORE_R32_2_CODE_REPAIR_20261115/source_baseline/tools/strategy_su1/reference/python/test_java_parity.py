"""Executable Python-to-Java numerical and exact-action contract tests."""
from pathlib import Path
import hashlib
import json
import subprocess
import sys
import unittest
import numpy as np

ROOT = Path(__file__).resolve().parents[2]
JAVA_DIR = ROOT / 'reference/java'
CLASSES = ROOT / 'verification/java_classes'
CLASS = 'com.ethk3.novaonepro.strategyv2.RelationalStateInference'

class JavaParity(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        CLASSES.mkdir(parents=True, exist_ok=True)
        source = JAVA_DIR / 'com/ethk3/novaonepro/strategyv2/RelationalStateInference.java'
        subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-d',str(CLASSES),str(source)],check=True,capture_output=True,text=True)
        cls.model_path = ROOT / 'reference/fixtures/relstate10_model.json'
        cls.model = json.loads(cls.model_path.read_text())
        cls.golden = json.loads((ROOT / 'reference/fixtures/relstate10_golden.json').read_text())

    def invoke(self, model, history):
        tokens=[str(model['width']),str(len(history))]
        for key in ('E','U','R','Wx','Wm','Wh','b','Wo','c'):
            tokens.extend(format(float(v),'.17g') for v in np.asarray(model['weights'][key]).ravel())
        tokens.extend(str(int(v)) for v in np.asarray(history).ravel())
        return subprocess.run(['java','-cp',str(CLASSES),CLASS],input=' '.join(tokens),capture_output=True,text=True)

    def test_golden_numerics_and_actions(self):
        self.assertEqual(hashlib.sha256(self.model_path.read_bytes()).hexdigest(),self.golden['modelSha256'])
        details=[]
        for index,case in enumerate(self.golden['cases']):
            with self.subTest(index=index):
                process=self.invoke(self.model,case['history'])
                self.assertEqual(process.returncode,0,process.stderr)
                lines=process.stdout.splitlines()
                actual=np.asarray([[float(v) for v in line.split()] for line in lines[:10]])
                error=float(np.max(np.abs(actual-np.asarray(case['probabilities']))))
                self.assertLessEqual(error,float(self.golden['absoluteTolerance']))
                selected=[int(v) for v in lines[10].split()]
                self.assertEqual(selected,[case['position']]+case['numbers'])
                details.append({'case':index,'max_abs_error':error,'action':selected})
        output=ROOT/'verification/java_parity_results.json'
        output.write_text(json.dumps({'status':'PASS','scope':'Host JVM numerical reference only; no Android integration', 'model_sha256':self.golden['modelSha256'],'cases':details},indent=2))

    def test_invalid_permutation_rejected(self):
        process=self.invoke(self.model,[[1]*10])
        self.assertNotEqual(process.returncode,0)
        self.assertIn('DRAW_PERMUTATION',process.stderr)

    def test_uniform_tie_is_deterministic(self):
        model=json.loads(json.dumps(self.model))
        for key,weight in model['weights'].items():
            model['weights'][key]=np.zeros_like(np.asarray(weight)).tolist()
        process=self.invoke(model,[list(range(1,11))])
        self.assertEqual(process.returncode,0,process.stderr)
        self.assertEqual([int(v) for v in process.stdout.splitlines()[10].split()],[1,1,2,3])

if __name__=='__main__':
    unittest.main(verbosity=2)
