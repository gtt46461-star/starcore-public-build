"""Run real release bytecode consumers with executable Android JSON, without DB work."""
import hashlib, json, os, subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
TOOLS = Path(os.environ['STARCORE_TOOLCHAIN_ROOT'])
CLASSES = ROOT / 'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes'
OUTPUT = ROOT / 'build/b01-consumer-probes'
OUTPUT.mkdir(parents=True, exist_ok=True)
json_jar = TOOLS / 'android-json.jar'
classpath = ':'.join(map(str, [CLASSES, json_jar, TOOLS / 'sdk/platforms/android-36/android.jar']))
commands = [[str(TOOLS / 'jdk17/bin/javac'), '-cp', classpath, '-d', str(OUTPUT),
             'verification/batch01/SemanticConsumerProbe.java', 'verification/batch01/PolicyConsumerProbe.java']]
identity = json.loads((ROOT / 'app/build/generated/r967/semantic_manifest.json').read_text())
for main, args in [('SemanticConsumerProbe', [identity['training_rule_sha256']]),
                   ('com.ethk3.novaonepro.PolicyConsumerProbe', [])]:
    commands.append([str(TOOLS / 'jdk17/bin/java'), '-cp', str(OUTPUT) + ':' + classpath, main, *args])
for command in commands:
    print('COMMAND ' + json.dumps(command), flush=True)
    subprocess.run(command, cwd=ROOT, check=True)
paths = ['ResearchSemanticsGeneratedR967', 'TrainingCodeIdentityR964',
         'TrainingContinuationR973', 'ResearchGatePlanR967', 'ResearchGatePlanR967$Policy']
receipt = {'consumer_classes_sha256': {
    name: hashlib.sha256((CLASSES / ('com/ethk3/novaonepro/' + name + '.class')).read_bytes()).hexdigest()
    for name in paths}, 'executable_json_jar_sha256': hashlib.sha256(json_jar.read_bytes()).hexdigest(),
    'training_rule_sha256': identity['training_rule_sha256'], 'commands': commands,
    'scope': 'REAL_RELEASE_BYTECODE_POLICY_AND_CONTINUATION;NO_DATABASE_OR_REAL_AUTHORITY'}
print(json.dumps(receipt, indent=2))
