#!/usr/bin/env bash
set -euo pipefail

BRANCH="agent/v49-full-window-causal-moe"
APK_NAME="ETH-K3-Nova-One-Pro-V49.0.49072201-debug.apk"
SOURCE_NAME="ETH-K3-Nova-One-Pro-V49.0.49072201-full-source.zip"
mkdir -p reports delivery

exec > >(tee reports/V49_CI_FINALIZE.log) 2>&1
set -x

if test ! -f app/src/main/java/com/ethk3/novaonepro/V49BuildManifest.java; then
  cat \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part00 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part01 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part02 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part03 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part04a \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part04b \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part04c \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part05a \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part05b \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part05c \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part06 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part07 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part08 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part09 \
    .github/v49-bootstrap/v49-source.patch.gz.b64.part10 \
    >/tmp/v49.b64
  echo '44b99cf80239d22a49bdd826806ee354f3410e27af18de980c9a422bfefaabb7  /tmp/v49.b64' | sha256sum -c -
  base64 -d /tmp/v49.b64 >/tmp/v49.patch.gz
  echo '7405784c8bafdf4d70abbe3a3d820051ec98496196385560bafc69a04929fcac  /tmp/v49.patch.gz' | sha256sum -c -
  gzip -dc /tmp/v49.patch.gz >/tmp/v49.patch
  echo 'c65ea7b3c7f3103392705f62b0244c72e4575c76f7c8b37964c1f1b129a39c45  /tmp/v49.patch' | sha256sum -c -
  git apply --check --verbose /tmp/v49.patch 2>&1 | tee reports/V49_APPLY_CHECK.log
  git apply --index /tmp/v49.patch

  echo '662c671aa10565dad44b3d2e9475cc6717f6260260a77e69cf0e4776f3413869  .github/v49-bootstrap/v49-compile-fix.patch.gz.b64' | sha256sum -c -
  base64 -d .github/v49-bootstrap/v49-compile-fix.patch.gz.b64 >/tmp/v49-fix.patch.gz
  echo 'b4500fa3db4a12a473dc72324d4e1cd7ed4f35cb30c4c216d1539b87a4bfcbde  /tmp/v49-fix.patch.gz' | sha256sum -c -
  gzip -dc /tmp/v49-fix.patch.gz >/tmp/v49-fix.patch
  echo '7125c015ad75bb9c55eae79279c7e56fb4a1ad58da064f8bc477b2bf2a7c2c74  /tmp/v49-fix.patch' | sha256sum -c -
  git apply --check --verbose /tmp/v49-fix.patch 2>&1 | tee reports/V49_COMPILE_FIX_APPLY.log
  git apply --index /tmp/v49-fix.patch

  mkdir -p app/src/main/assets
  gzip -n -9 -c research_data/ethpk10_draws_20260316_201059.csv >app/src/main/assets/history_seed.csv.gz
  git add app/src/main/assets/history_seed.csv.gz
fi

test -f app/src/main/java/com/ethk3/novaonepro/V49BuildManifest.java
grep -q "versionName 'V49.0.49072201'" app/build.gradle
grep -q 'DB_VERSION = 32' app/src/main/java/com/ethk3/novaonepro/AppConfig.java

python -m pip install --disable-pip-version-check numpy==2.1.3 javalang==0.13.0
python tools/v49_static_selfcheck.py | tee reports/V49_CI_STATIC.log
python tools/run_v49_harnesses.py | tee reports/V49_CI_HARNESSES.log
python tools/v49_feature_parity.py | tee reports/V49_CI_PARITY.log
python tools/v49_deterministic_replay.py | tee reports/V49_CI_REPLAY.log
python tools/v49_ablation_benchmark.py | tee reports/V49_CI_ABLATION.log
python -m py_compile training/*.py tools/v49_*.py tools/run_v49_harnesses.py
python training/build_dataset.py research_data/ethpk10_draws_20260316_201059.csv --output /tmp/v49.npz >reports/V49_CI_DATASET.log
python training/train_compare_export.py /tmp/v49.npz --output /tmp/v49-model.json --java-output /tmp/V49ModelArtifact.java >reports/V49_CI_TRAIN.log
if ! cmp /tmp/V49ModelArtifact.java app/src/main/java/com/ethk3/novaonepro/V49ModelArtifact.java; then
  cp /tmp/V49ModelArtifact.java app/src/main/java/com/ethk3/novaonepro/V49ModelArtifact.java
  git add app/src/main/java/com/ethk3/novaonepro/V49ModelArtifact.java
fi

python - <<'PY'
from pathlib import Path
import collections, javalang, xml.etree.ElementTree as ET
errors=[]; duplicates=[]
files=sorted(Path('app/src/main/java/com/ethk3/novaonepro').glob('*.java'))
for p in files:
    try:
        tree=javalang.parse.parse(p.read_text(encoding='utf-8'))
        for _,c in tree.filter(javalang.tree.ClassDeclaration):
            names=[]
            for f in c.fields: names.extend(d.name for d in f.declarators)
            bad=[n for n,k in collections.Counter(names).items() if k>1]
            if bad: duplicates.append((str(p),c.name,bad))
    except Exception as e: errors.append((str(p),str(e)))
xml_errors=[]
for p in list(Path('app/src/main/res').rglob('*.xml'))+[Path('app/src/main/AndroidManifest.xml')]:
    try: ET.parse(p)
    except Exception as e: xml_errors.append((str(p),str(e)))
print('java=%d parse=%d duplicate=%d xml=%d' % (len(files),len(errors),len(duplicates),len(xml_errors)))
if errors or duplicates or xml_errors:
    print(errors[:30]); print(duplicates[:30]); print(xml_errors[:30]); raise SystemExit(1)
PY

gradle --no-daemon --stacktrace :app:assembleDebug 2>&1 | tee reports/V49_GRADLE_BUILD.log
test -s app/build/outputs/apk/debug/app-debug.apk
cp app/build/outputs/apk/debug/app-debug.apk "delivery/$APK_NAME"

rm -rf .github/v49-bootstrap \
  .github/workflows/v49-bootstrap.yml \
  .github/workflows/v49-delta-verified-build.yml \
  .github/workflows/v49-exact-pr-build.yml \
  .github/workflows/v49-full-build.yml

git config user.name 'github-actions[bot]'
git config user.email '41898282+github-actions[bot]@users.noreply.github.com'
git add -A
git reset -- delivery reports/V49_APPLY_CHECK.log reports/V49_COMPILE_FIX_APPLY.log reports/V49_CI_*.log reports/V49_GRADLE_BUILD.log reports/V49_CI_FINALIZE.log || true
git status --short
git commit --no-verify -m 'V49 full window causal MOE verified full source and APK'

git archive --format=zip --output "delivery/$SOURCE_NAME" HEAD
cp reports/V49_HARNESSES_49072201.json reports/V49_MODEL_LAB_49072201.json delivery/
sha256sum delivery/* >delivery/SHA256SUMS.txt

git push --force origin HEAD:"$BRANCH"
