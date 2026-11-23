"""Execute original Java probes using the compiled classes of the candidate under test."""
import hashlib
import os
from pathlib import Path
import subprocess

# Immutable test inputs from the R33.5 original volume. Only the test class is
# compiled. No historical production Java source or compiled owner is loaded.
ORIGINAL_PROBES = {
    'consumers': ('B05/tests/ResearchConsumerR335Host.java', '47b509d6d5893c65b905589a571ceb12d6553d1e106e2e153dfd71f6214620b6', ()),
    'research-actions': ('B05/tests/ResearchActionPolicyR33HostTest.java', 'ff4de3f082ca5c7fb2a26758a495d1ce9908c6d23a0ca378bba83f025568b840', ()),
    's5-retry': ('B05/s4_code/S5LedgerHostR33.java', '9172fd21b69137184665c44b0e524631e005a4afad23fe49dc345973e3585291', ('after',)),
    'contmat-refit': ('B05/verification_r335/c10_cross_review/ContMatLogCompatibilityR335.java', 'd6dd9bf739a1715851409b5bbc2a07d2d3048b0d9e80f227029fa96a5b65063f', ()),
    'contmat-cursor': ('B05/verification_r335/c10_cross_review/CrossContMatCursorR335.java', 'f4a712e3c7cf099a0020f1da78d0a02446292670f5cc26c880839db2a301869d', ()),
}
PROBES_BY_CASE = {
    'RE02': ('research-actions',), 'RE03': ('research-actions',),
    'RE04': ('consumers', 'research-actions'), 'RE05': ('consumers', 'research-actions'),
    'RE06': ('consumers', 'research-actions'), 'RE07': ('consumers',), 'RE08': ('consumers',),
    'RE11': ('consumers',), 'RE12': ('research-actions',), 'RE13': ('consumers',),
    'RE17': ('consumers',), 'RE22': ('contmat-refit',), 'RE23': ('s5-retry',),
    'RE25': ('contmat-cursor', 'contmat-refit'), 'C05': ('s5-retry',),
}


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def run_logged(argv, directory, logfile, timeout):
    with logfile.open('xb') as log:
        try:
            return subprocess.run(list(map(str, argv)), cwd=directory, stdout=log,
                                  stderr=subprocess.STDOUT, timeout=timeout).returncode
        except subprocess.TimeoutExpired:
            return 124


def run_original(name, old_plan, source, output):
    relative, expected, args = ORIGINAL_PROBES[name]
    if old_plan is None:
        return {'status': 'OPEN', 'reason': 'R335_ORIGINAL_VOLUME_REQUIRED'}
    fixture = old_plan / relative
    if not fixture.is_file() or digest(fixture) != expected:
        raise ValueError('ORIGINAL_PROBE_INPUT_CHANGED:' + relative)
    classes = source / 'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
    android_home = os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not android_home or not classes.is_dir():
        return {'status': 'OPEN', 'reason': 'COMPILED_ANDROID_CANDIDATE_REQUIRED'}
    android = Path(android_home) / 'platforms/android-36/android.jar'
    if not android.is_file():
        return {'status': 'OPEN', 'reason': 'PLATFORM_ANDROID_36_REQUIRED'}
    dest = output / name
    dest.mkdir()
    compiled = dest / 'test-classes'
    compiled.mkdir()
    # An empty sourcepath prevents implicit recompilation of older owners.
    classpath = os.pathsep.join(map(str, (classes, android)))
    sources = [fixture]
    if name == 'contmat-refit':
        helper, checksum, _ = ORIGINAL_PROBES['contmat-cursor']
        if digest(old_plan / helper) != checksum:
            raise ValueError('ORIGINAL_CURSOR_HELPER_INPUT_CHANGED')
        sources.append(old_plan / helper)
    compile_log = dest / 'compile.log'
    compile_exit = run_logged(['javac', '--release', '17', '-sourcepath', '', '-classpath', classpath,
                               '-d', compiled, *sources], source, compile_log, 90)
    record = {'fixture': relative, 'fixtureSha256': expected, 'compileExit': compile_exit,
              'compileLog': str(compile_log.relative_to(output)), 'compileLogSha256': digest(compile_log)}
    if compile_exit:
        return dict(record, status='FAIL', reason='ORIGINAL_FIXTURE_DID_NOT_COMPILE')
    log = dest / 'run.log'
    main = fixture.stem if name == 'research-actions' else 'com.ethk3.novaonepro.strategyv2.' + fixture.stem
    rc = run_logged(['java', '-ea', '-Xmx448m', '-XX:ActiveProcessorCount=2', '-cp',
                     os.pathsep.join((str(compiled), classpath)), main, *args], dest, log, 240)
    return dict(record, status='PASS' if rc == 0 else 'FAIL', exit=rc,
                log=str(log.relative_to(output)), logSha256=digest(log))
