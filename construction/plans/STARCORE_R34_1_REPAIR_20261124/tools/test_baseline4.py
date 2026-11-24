import argparse
import json
import tempfile
from pathlib import Path
from baseline4 import MEMBERS, extract


if __name__ == '__main__':
    p=argparse.ArgumentParser()
    p.add_argument('--report-dir',type=Path,required=True)
    root=p.parse_args().report_dir
    parsed=extract(root)
    a,=parsed['r952_candidates']
    assert (a['train_hits'],a['train_settled'],a['evidence_role'])==(2,7,'TRAIN_STREAM_V2')
    assert len({r['target'] for r in a['settled_targets']})==7
    assert [r['value'] for r in parsed['metrics'] if r['key']=='independentFutureIssues']==[0]
    with tempfile.TemporaryDirectory() as work:
        for name in MEMBERS:
            (Path(work)/name).symlink_to((root/name).resolve())
        bad=Path(work)/'R31_SEMANTIC_RECEIPT.json'
        old=json.loads(bad.read_bytes())
        bad.unlink()
        bad.write_text(json.dumps({**old,'jobId':'other'}))
        try:
            extract(Path(work))
        except ValueError as err:
            assert 'foreign or altered' in str(err)
        else:
            raise AssertionError('accepted other report identity')
    print('PASS_SOURCE_ROLE_AND_WRONG_SNAPSHOT_PROBES')
