"""Verify and optionally apply the narrow patch to an explicitly supplied checkout.
The replacement path has no network side effect and performs no Android build.
"""
import argparse,hashlib,json
from pathlib import Path
a=argparse.ArgumentParser();a.add_argument('checkout',type=Path);a.add_argument('--apply',action='store_true');args=a.parse_args()
base=Path(__file__).parents[1];spec=json.loads((base/'patches/preconditions_r25.json').read_text());p=args.checkout/spec['file'];b=p.read_bytes()
actual=hashlib.sha256(b).hexdigest()
if actual==spec['after_sha256']:print('ALREADY_APPLIED');raise SystemExit(0)
if actual!=spec['before_sha256']:raise SystemExit('BASE_HASH_MISMATCH stop and review diff')
after=(base/'patches/after'/spec['file']).read_bytes();assert hashlib.sha256(after).hexdigest()==spec['after_sha256']
if args.apply:p.write_bytes(after);print('APPLIED_SOURCE_ONLY_ANDROID_BUILD_REQUIRED')
else:print('PRECONDITION_PASS_NO_MUTATION')
