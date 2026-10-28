#!/usr/bin/env python3
"""R27-010 golden set builder (docs/08 8.3). Deterministic: the first 520 valid rows of the repository CSV, with rows
150 and 330 removed so the set contains two real missing-issue gaps, truncated to 512 rows.
Format: b'R27G' + int32 rows + per row int64 ordinal + 10 bytes (numbers 1..10). Usage: make_golden.py <csv> <out.bin>"""
import csv, datetime, hashlib, struct, sys
def ordinal(p):
    d = datetime.date(int(p[:4]), int(p[4:6]), int(p[6:8]))
    return (d - datetime.date(1970, 1, 1)).days * 1440 + int(p[8:])
rows = [r for r in csv.DictReader(open(sys.argv[1])) if r['valid'] == '1']
rows.sort(key=lambda r: r['period'])
rows = rows[:520]; del rows[330]; del rows[150]; rows = rows[:512]
out = bytearray(b'R27G') + struct.pack('>i', len(rows))
for r in rows:
    nums = [int(x) for x in r['open_number'].split(',')]
    assert sorted(nums) == list(range(1, 11))
    out += struct.pack('>q', ordinal(r['period'])) + bytes(nums)
open(sys.argv[2], 'wb').write(out)
print(hashlib.sha256(out).hexdigest(), len(rows), rows[0]['period'], rows[-1]['period'])
