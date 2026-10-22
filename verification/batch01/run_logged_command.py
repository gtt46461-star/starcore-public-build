#!/usr/bin/env python3
"""Keep an atomic recovery receipt while a real construction command runs."""
import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import signal
import subprocess
import time

ROOT = Path(__file__).resolve().parents[2]


def now():
    return datetime.datetime.now(datetime.timezone.utc).isoformat()


def git(*arguments):
    return subprocess.check_output(['git', *arguments], cwd=ROOT, text=True).strip()


def inputs():
    result = {}
    names = subprocess.check_output(['git', 'ls-files', '-z'], cwd=ROOT).split(b'\0')
    for raw in names:
        if not raw:
            continue
        name = os.fsdecode(raw)
        path = ROOT / name
        if path.is_file() and not name.startswith('construction/eight-batches-20261007/'):
            result[name] = hashlib.sha256(path.read_bytes()).hexdigest()
    return result


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--name', required=True)
    parser.add_argument('--evidence-dir', type=Path, required=True)
    parser.add_argument('--timeout', type=float, default=1800)
    parser.add_argument('command', nargs=argparse.REMAINDER)
    args = parser.parse_args()
    command = args.command[1:] if args.command[:1] == ['--'] else args.command
    if not command or args.timeout <= 0 or '/' in args.name or '\\' in args.name:
        parser.error('Provide a command, positive timeout and one receipt filename')
    output = args.evidence_dir.resolve()
    output.mkdir(parents=True, exist_ok=True)
    receipt = output / (args.name + '.json')
    log_path = output / (args.name + '.log')
    if receipt.exists() or log_path.exists():
        parser.error('Evidence already exists; use a new attempt name')
    before = inputs()
    state = {'command': command, 'cwd': str(ROOT), 'started': now(),
             'status': 'STARTING', 'source_commit': git('rev-parse', 'HEAD'),
             'source_tree': git('rev-parse', 'HEAD^{tree}'),
             'branch': git('branch', '--show-current'), 'inputs_sha256': before,
             'raw_log': str(log_path), 'exit_code': None}

    def save():
        state['heartbeat'] = now()
        state['log_bytes'] = log_path.stat().st_size if log_path.exists() else 0
        temporary = receipt.with_suffix('.json.tmp')
        temporary.write_text(json.dumps(state, ensure_ascii=False, indent=2) + '\n')
        temporary.replace(receipt)

    save()
    child = None
    interrupted = None

    def stop(signum, frame):
        nonlocal interrupted
        interrupted = signum
        if child is not None and child.poll() is None:
            os.killpg(child.pid, signal.SIGTERM)

    signal.signal(signal.SIGINT, stop)
    signal.signal(signal.SIGTERM, stop)
    started = time.monotonic()
    try:
        with log_path.open('wb') as log:
            child = subprocess.Popen(command, cwd=ROOT, stdout=log,
                                     stderr=subprocess.STDOUT, start_new_session=True)
            state.update(status='RUNNING', runner_pid=os.getpid(), command_pid=child.pid)
            save()
            while child.poll() is None:
                remaining = args.timeout - (time.monotonic() - started)
                if remaining <= 0:
                    state['timeout_seconds'] = args.timeout
                    os.killpg(child.pid, signal.SIGTERM)
                    try:
                        child.wait(timeout=10)
                    except subprocess.TimeoutExpired:
                        os.killpg(child.pid, signal.SIGKILL)
                        child.wait()
                    break
                try:
                    child.wait(timeout=min(20, remaining))
                except subprocess.TimeoutExpired:
                    save()
                    print(json.dumps({'status': 'RUNNING', 'name': args.name,
                                      'elapsed_seconds': round(time.monotonic() - started),
                                      'log_bytes': state['log_bytes']}), flush=True)
            state['exit_code'] = (124 if 'timeout_seconds' in state else
                                  128 + interrupted if interrupted else child.returncode)
    except Exception as error:
        state.update(status='ERROR', error=repr(error), exit_code=125)
    after = inputs()
    state['changed_inputs'] = sorted(name for name in set(before) | set(after)
                                    if before.get(name) != after.get(name))
    state['final_source_commit'] = git('rev-parse', 'HEAD')
    state['final_source_tree'] = git('rev-parse', 'HEAD^{tree}')
    if state['status'] != 'ERROR':
        state['status'] = ('INTERRUPTED' if interrupted else
                           'FAIL' if state['exit_code'] else
                           'INVALIDATED' if state['changed_inputs'] or
                           state['source_commit'] != state['final_source_commit'] else 'PASS')
    state['finished'] = now()
    if log_path.exists():
        state['log_sha256'] = hashlib.sha256(log_path.read_bytes()).hexdigest()
    save()
    print(json.dumps({key: state[key] for key in
                      ('status', 'exit_code', 'source_commit', 'raw_log', 'changed_inputs')}), flush=True)
    return state['exit_code'] or (126 if state['status'] != 'PASS' else 0)


if __name__ == '__main__':
    raise SystemExit(main())
