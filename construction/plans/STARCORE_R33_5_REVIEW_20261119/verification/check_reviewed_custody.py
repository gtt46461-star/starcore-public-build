"""Run the original 115 checks against all final effective production images.

The original sealed payloads, ten compile receipts, dependency bytes and every
compiled Java input remain checked by the pinned verifier. The final candidate
comparison uses the same complete overlay as stage_checked, including new paths.
"""
import argparse
import hashlib
import json
from pathlib import Path
import sys
from tempfile import TemporaryDirectory

p = argparse.ArgumentParser(add_help=False)
p.add_argument('--plan', required=True, type=Path)
a, remaining = p.parse_known_args()
plan = a.plan.resolve(strict=True)
original = (plan/'B01/tests/host_runtime/custody_cross/run_registered.py').resolve(strict=True)
body = original.read_bytes()
if hashlib.sha256(body).hexdigest() != '44fd1c80ba39c5fa84f02a2edd286631bc7cd022fb9ef31b650a47baf9f865bb':
    raise SystemExit('Unexpected original custody verifier')
review = Path(__file__).resolve().parents[1]
sys.path[:0] = [str(review), str(review/'replay'), str(review/'research')]
import production_overlay
import stage_checked
import b05_pair_source as pair
from verify_review import verify

verify(review)
tool = stage_checked.pinned_module(plan, 'verification/contractor/stage.py', 'custody_stage_original',
    '2457b6f332ea478271dde5582edd36e8f09ae68c697c194339ab4d5981e9620c')
with TemporaryDirectory(prefix='r335-custody-effective-') as temporary:
    effective = Path(temporary)
    composition = production_overlay.prepare(tool, plan, pair, review, effective)
    effective_bytes = (effective/'control/APPLY_MANIFEST_R335.json').read_bytes()
    manifest = json.loads(effective_bytes)
    pins = {row['path']: row['afterSha256'] for batch in manifest['batches'] for row in batch['files']}
    wrapper = Path(__file__).resolve()

    def reviewed_candidate_check(repo, prefix):
        candidate_manifest = prefix/'EFFECTIVE_APPLY_MANIFEST.json'
        if not candidate_manifest.is_file() or candidate_manifest.read_bytes() != effective_bytes:
            raise ValueError('FINAL_EFFECTIVE_MANIFEST_CHANGED_OR_MISSING')
        prefix_binding = json.loads((prefix/'REVIEW_BINDING.json').read_text())
        if prefix_binding.get('productionReview') != composition:
            raise ValueError('FINAL_PRODUCTION_REVIEW_BINDING_CHANGED')
        for relative, digest in pins.items():
            path = production_overlay.safe(repo, relative)
            if not path.is_file() or hashlib.sha256(path.read_bytes()).hexdigest() != digest:
                raise ValueError('FINAL_EFFECTIVE_CANDIDATE_CHANGED:' + relative)
        # The recorded prefix seal is its actual start input. Documentation may
        # subsequently change; complete source and overlay bytes must still match.
        return {'productionReview': composition, 'productionPathCount': len(pins),
                'prefixReviewManifestSha256': prefix_binding['reviewManifestSha256'],
                'currentReviewManifestSha256': verify(review),
                'reviewWrapperSha256': hashlib.sha256(wrapper.read_bytes()).hexdigest(),
                'reviewInputsSha256': hashlib.sha256((review/'production/REVIEW_INPUTS.json').read_bytes()).hexdigest()}

    old = "    for relative, row in latest.items():\n        require(sha(repo / relative) == row['afterSha256'], 'FINAL_CANDIDATE_AFTER_IMAGE_CHANGED:' + relative)"
    new = '    reviewed_binding = reviewed_candidate_check(repo, prefix)'
    marker = "        'schema': 'R335_CUSTODY_FINAL_PREFIX_BINDING_V1',"
    source = body.decode()
    if source.count(old) != 1 or source.count(marker) != 1:
        raise SystemExit('Unexpected candidate binding sites')
    source = source.replace(old, new).replace(marker, marker + "\n        'reviewedProduction': reviewed_binding,")
    sys.argv = [str(original), *remaining]
    exec(compile(source, str(original), 'exec'),
         {'__name__': '__main__', '__file__': str(original),
          'reviewed_candidate_check': reviewed_candidate_check})
