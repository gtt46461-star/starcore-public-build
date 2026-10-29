#!/usr/bin/env python3
"""Generate executable SU2 DDL without collapsing SQL comments into statements."""
import argparse
import json
from pathlib import Path
import re
import sqlite3

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'tools/strategy_su2/schema/strategy_su2.sql'
TARGET = ROOT / 'app/src/main/java/com/ethk3/novaonepro/strategyv2/StrategySchemaSU2.java'


def statements():
    sql = SOURCE.read_text(encoding='utf-8')
    tokens = re.compile(r"'(?:''|[^'])*'|\"(?:\"\"|[^\"])*\"|--[^\n]*|/\*.*?\*/", re.S)
    sql = tokens.sub(lambda m: ' ' if m[0].startswith(('--', '/*')) else m[0], sql)
    result, pending = [], ''
    for char in sql:
        pending += char
        if char == ';' and sqlite3.complete_statement(pending):
            result.append(' '.join(pending.strip().rstrip(';').split()))
            pending = ''
    if pending.strip():
        raise ValueError('SU2_INCOMPLETE_DDL')
    # Validate the exact statements that the generated Android class will execute.
    with sqlite3.connect(':memory:') as db:
        for statement in result:
            db.execute(statement)
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--check', action='store_true')
    args = parser.parse_args()
    original = TARGET.read_text(encoding='utf-8')
    entries = '\n'.join('        ' + json.dumps(s, ensure_ascii=False) + ',' for s in statements())
    updated, count = re.subn(r'(?<=public static final String\[\] DDL = \{)\n.*?\n    (?=\};)', lambda _: '\n' + entries + '\n    ', original, flags=re.S)
    if count != 1:
        raise ValueError('SU2_DDL_GENERATION_ANCHOR')
    if args.check:
        if updated != original:
            raise SystemExit('SU2_DDL_GENERATED_SOURCE_DRIFT')
    else:
        TARGET.write_text(updated, encoding='utf-8')
    print('SU2 production DDL: reproducible and executable')


if __name__ == '__main__':
    main()
