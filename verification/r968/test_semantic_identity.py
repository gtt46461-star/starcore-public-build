#!/usr/bin/env python3
"""Mutation tests of the actual product-build generator; no copied hash algorithm."""
from pathlib import Path
import copy, importlib.util, json, tempfile, unittest
R=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('actual_generator',R/'scripts/r967_semantic_identity.py')
gen=importlib.util.module_from_spec(spec);spec.loader.exec_module(gen)
class IdentityTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.baseline=gen.build();cls.java=R/'app/src/main/java/com/ethk3/novaonepro'
    def changed(self,name):
        return gen.build(overrides={name:(self.java/name).read_bytes()+b'\n// identity regression mutation\n'})
    def test_report_change_preserves_dev_and_policy_but_changes_provenance(self):
        for name in ['MainActivity.java','CompleteReportFileR927.java','ResearchFunnelR967.java','r952/CreationPresenterR952.java','r952/DeepLearningPresenterR952.java']:
            with self.subTest(name=name):
                b=self.changed(name)
                self.assertEqual(b['training_rule_sha256'],self.baseline['training_rule_sha256'])
                self.assertEqual(b['policy_hash'],self.baseline['policy_hash'])
                self.assertNotEqual(b['source_provenance_sha256'],self.baseline['source_provenance_sha256'])
    def test_vm_features_calibrator_and_state_model_are_bound(self):
        for name in ['AlgorithmGenomeVmR908.java','V51TrajectoryFeatureEngine.java','ResearchLogisticCalibrationR932.java','r952/CandidateCompilerR952.java','SchemaDomainCoordinatorR926.java']:
            with self.subTest(name=name):self.assertNotEqual(self.changed(name)['training_rule_sha256'],self.baseline['training_rule_sha256'])
    def test_release_version_and_description_not_training_math(self):
        m=json.loads((R/'release/release_manifest.json').read_text());m['versionCode']+=1;m['versionName']='next UI release';m['proofBoundary']['modelProof']='still unproven'
        self.assertEqual(gen.build(release_override=m)['training_rule_sha256'],self.baseline['training_rule_sha256'])
    def test_database_schema_and_game_are_training_inputs(self):
        for target in ('schema','game'):
            m=json.loads((R/'release/release_manifest.json').read_text())
            if target=='schema':m['schema']['userVersion']+=1
            else:m['gameCode']='DIFFERENT_GAME'
            self.assertNotEqual(gen.build(release_override=m)['training_rule_sha256'],self.baseline['training_rule_sha256'])
    def test_threshold_policy_changes_without_changing_training_math(self):
        p=json.loads((R/gen.POLICY).read_text());p['min_hit_rate']=.35;p['revision']='R967_STATIC_FIXED_POLICY_V2'
        b=gen.build(overrides={gen.POLICY:json.dumps(p).encode()})
        self.assertNotEqual(b['policy_hash'],self.baseline['policy_hash']);self.assertEqual(b['training_rule_sha256'],self.baseline['training_rule_sha256'])
    def test_unsupported_modes_and_gate_weakening_rejected(self):
        for key,value in [('mode','ADAPTIVE_UNIMPLEMENTED'),('min_hit_rate',.30),('consumer_cap',.10),('total_alpha',.10),('real_authority',True),('window_sizes',[10]),('statistics_revision','UNKNOWN'),('null_assumption','anything')]:
            p=json.loads((R/gen.POLICY).read_text());p[key]=value
            with self.subTest(key=key),self.assertRaises(ValueError):gen.build(overrides={gen.POLICY:json.dumps(p).encode()})
    def test_nonfinite_and_extra_policy_keys_rejected(self):
        for action in ('nan','extra'):
            p=json.loads((R/gen.POLICY).read_text())
            if action=='nan':p['max_ece']=float('nan')
            else:p['silently_ignored']=True
            with self.assertRaises(ValueError):gen.build(overrides={gen.POLICY:json.dumps(p).encode()})
    def test_unclassified_removed_manifest_entry_is_hard_failure(self):
        m=json.loads((R/gen.MANIFEST).read_text());m['files'].pop()
        with self.assertRaisesRegex(ValueError,'UNCLASSIFIED_OR_MISSING'):gen.build(manifest_override=m)
    def test_duplicate_or_unknown_roles_rejected(self):
        m=json.loads((R/gen.MANIFEST).read_text());m['files'].append(copy.deepcopy(m['files'][0]))
        with self.assertRaisesRegex(ValueError,'INVALID_CLASSIFICATION'):gen.build(manifest_override=m)
        m=json.loads((R/gen.MANIFEST).read_text());m['files'][0]['role']='AUTO_TRUST'
        with self.assertRaisesRegex(ValueError,'INVALID_CLASSIFICATION'):gen.build(manifest_override=m)
    def test_new_reflection_is_not_auto_compatible(self):
        name='AlgorithmGenomeVmR908.java';raw=(self.java/name).read_bytes()+b'\n// Class.forName("unreviewed.Evaluator")\n'
        with self.assertRaisesRegex(ValueError,'UNKNOWN_DYNAMIC_DEPENDENCY'):gen.build(overrides={name:raw})
    def test_existing_reflection_review_is_byte_pinned(self):
        m=json.loads((R/gen.MANIFEST).read_text());reviewed=[r for r in m['files'] if 'dynamic_review_sha256' in r];self.assertTrue(reviewed)
        r=reviewed[0]
        with self.assertRaisesRegex(ValueError,'UNKNOWN_DYNAMIC_DEPENDENCY'):gen.build(overrides={r['path']:(R/r['path']).read_bytes()+b'\n'})
    def test_input_order_and_json_whitespace_not_semantic(self):
        m=json.loads((R/gen.MANIFEST).read_text())
        # Explicit order is part of classification review, so it is never silently sorted away.
        p=json.loads((R/gen.POLICY).read_text())
        b=gen.build(overrides={gen.POLICY:json.dumps(p,indent=8,sort_keys=False).encode()})
        self.assertEqual(b['policy_hash'],self.baseline['policy_hash']);self.assertEqual(b['training_rule_sha256'],self.baseline['training_rule_sha256'])
    def test_generated_outputs_exact_and_not_sources(self):
        gen.verify_outputs()
        self.assertNotIn(gen.GENERATED,gen.controlled_paths(R));self.assertNotIn(gen.REPORT,gen.controlled_paths(R))
    def test_new_source_and_symlink_fail_without_silently_ignoring(self):
        # Only this test uses a throwaway source mirror; the product checkout is not altered.
        with tempfile.TemporaryDirectory(prefix='r968-identity-') as d:
            t=Path(d)
            for path in gen.controlled_paths(R)|{gen.MANIFEST,'release/release_manifest.json'}:
                out=t/path;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes((R/path).read_bytes())
            new=t/'app/src/main/java/com/ethk3/novaonepro/UnknownNumeric.java';new.write_text('class UnknownNumeric {}')
            with self.assertRaisesRegex(ValueError,'UNCLASSIFIED_OR_MISSING'):gen.build(t)
            new.unlink();new.symlink_to(t/'release/release_manifest.json')
            with self.assertRaisesRegex(ValueError,'UNREVIEWED_SYMLINK'):gen.build(t)
if __name__=='__main__':unittest.main(verbosity=2)
