"""Check this revision's complete file inventory against the checked-out Git manifest."""
import argparse
import hashlib
import json
from pathlib import Path


def verify(review):
    root = Path(review).resolve(strict=True)
    seal = root / 'REVIEW_SHA256.json'
    data = seal.read_bytes()
    expected = json.loads(data)
    actual = {}
    for path in root.rglob('*'):
        rel = path.relative_to(root)
        if path.is_symlink():
            raise ValueError('Unexpected link: ' + str(rel))
        if ('__pycache__' in rel.parts or path.suffix in ('.pyc', '.pyo') or
                path == seal or not path.is_file()):
            continue
        with path.open('rb') as handle:
            actual[rel.as_posix()] = hashlib.file_digest(handle, 'sha256').hexdigest()
    if actual != expected:
        mismatch = [n for n in sorted(set(expected) | set(actual)) if expected.get(n) != actual.get(n)]
        raise ValueError('Review files differ: ' + ', '.join(mismatch))
    return hashlib.sha256(data).hexdigest()


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('review', type=Path, nargs='?', default=Path(__file__).parent)
    print(verify(p.parse_args().review))
