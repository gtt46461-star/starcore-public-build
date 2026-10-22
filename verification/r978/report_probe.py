#!/usr/bin/env python3
"""Reproduce report excerpts from a report.txt and its byte-offset table index.
This reads exports, not a device database, and never rewrites evidence or repairs data.
"""
import argparse
import itertools
import json
from pathlib import Path


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--report', type=Path, required=True)
    p.add_argument('--index', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    a = p.parse_args()
    index = json.loads(a.index.read_text())
    a.out.mkdir(parents=True, exist_ok=True)
    with a.report.open('rb') as f:
        def table(name):
            matches = [x for x in index if x['name'] == name]
            if len(matches) != 1:
                raise ValueError('Expected one table: ' + name)
            t = matches[0]
            f.seek(t['data_start'])
            rows = []
            for line in f:
                if line.startswith(b'[END_TABLE'):
                    break
                fields = line.decode('utf-8').rstrip('\r\n').split('\t')
                if len(fields) != len(t['columns']):
                    raise ValueError('Column count mismatch in ' + name)
                rows.append(dict(zip(t['columns'], fields)))
            if len(rows) != t['rows']:
                raise ValueError('Exported row count mismatch in ' + name)
            return rows
        obs = table('research_candidate_cal_observation_r932')
        summary = {}
        for key, rows in itertools.groupby(sorted(obs, key=lambda x: x['candidate_key']), key=lambda x: x['candidate_key']):
            rows = list(rows)
            summary[key] = {field: len({x.get(field, '') for x in rows}) for field in ('study_hash', 'structure_hash', 'bundle_hash', 'selection_hash')}
            summary[key]['n'] = len(rows)
        (a.out/'calibration_fragmentation_current_report.json').write_text(json.dumps(summary, indent=2)+'\n')
        f.seek(0)
        head = f.read(index[0]['start']).decode('utf-8')
        jobs = [line for line in head.splitlines() if line.startswith('DEV_JOB|')]
        (a.out/'historical_development_current_report.txt').write_text('\n'.join(jobs)+'\n')
        print(json.dumps({'observations': len(obs), 'candidate_groups': len(summary), 'development_jobs': len(jobs), 'scope':'EXPORTED_REPORT_READ_ONLY'}))

if __name__ == '__main__':
    main()
