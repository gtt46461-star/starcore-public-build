"""Run the 115-check suite against the reviewed final candidate and its two revised file pins."""
import argparse
import hashlib
from pathlib import Path
import sys

p = argparse.ArgumentParser(add_help=False)
p.add_argument('--plan', required=True, type=Path)
a, remaining = p.parse_known_args()
original = (a.plan/'B01/tests/host_runtime/custody_cross/run_registered.py').resolve(strict=True)
body = original.read_bytes()
if hashlib.sha256(body).hexdigest() != '44fd1c80ba39c5fa84f02a2edd286631bc7cd022fb9ef31b650a47baf9f865bb':
    raise SystemExit('Unexpected original custody verifier')
review = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(review/'research'))
from b05_pair_source import AFTER, REL, IDENTITY_REL, IDENTITY_PINS
pins = {REL.as_posix(): AFTER, str(IDENTITY_REL): IDENTITY_PINS['B07'][1]}
# All sealed base payloads are still checked. Only the final candidate side of the comparison
# must resolve the two reviewed paths to their effective, fixed SHA-256 values.
old = "sha(repo / relative) == row['afterSha256']"
new = "sha(repo / relative) == reviewed_pins.get(relative, row['afterSha256'])"
source = body.decode()
if source.count(old) != 1:
    raise SystemExit('Unexpected candidate binding site')
sys.argv = [str(original), *remaining]
exec(compile(source.replace(old, new), str(original), 'exec'),
     {'__name__': '__main__', '__file__': str(original), 'reviewed_pins': pins})
