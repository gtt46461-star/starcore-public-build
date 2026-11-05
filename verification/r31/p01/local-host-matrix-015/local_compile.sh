#!/usr/bin/env bash
# NON-AUTHORITATIVE local compile (no Android SDK reachable): javac against Robolectric android-all 16 + repo stubs.
set -euo pipefail
ROOT=${1:-/home/user/starcore-public-build}; OUT=${2:-/tmp/claude-0/build}
SDKJAR=/tmp/claude-0/sdk/android-compile.jar
rm -rf "$OUT"; mkdir -p "$OUT/classes" "$OUT/gen/com/ethk3/novaonepro"
cd "$ROOT"
python3 /tmp/claude-0/tools/gen_r.py app/src/main/res "$OUT/gen/com/ethk3/novaonepro/R.java" >/dev/null
python3 - "$ROOT" "$OUT" <<'PY'
import sys,re,subprocess,json
root,out=sys.argv[1],sys.argv[2]
stub=open(root+'/verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java').read()
head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
ident=json.loads(subprocess.check_output(['python3','tools/r31/verify_baseline.py','--identity-only'],cwd=root,text=True))
extra=f'''    public static final String SOURCE_COMMIT_R26 = "{head}";
    public static final String BASELINE_SOURCE_COMMIT_R31 = "{ident.get('baseline_commit','')}";
    public static final String SOURCE_TREE_R31 = "{ident.get('source_tree','')}";
    public static final boolean SOURCE_DIRTY_R31 = {str(bool(ident.get('source_dirty'))).lower()};
    public static final String SOURCE_WORKTREE_SHA256_R31 = "{ident.get('product_worktree_sha256','')}";
    public static final String PLAN_MANIFEST_SHA256_R31 = "{ident.get('plan_manifest_sha256','')}";
    public static final String MIGRATION_CONTRACT_SHA256_R31 = "{ident.get('migration_contract_sha256','')}";
'''
stub=stub.rstrip().rstrip('}')+extra+'}\n'
open(out+'/gen/com/ethk3/novaonepro/BuildConfig.java','w').write(stub)
PY
# r815 generated facade: run the real generator into the build dir with locally available toolchain files.
D=/tmp/claude-0/sdk/placeholder.txt; echo "LOCAL_NO_SDK" > $D
python3 scripts/r815_generate_provenance.py --toolchain-file jdk-modules=$(dirname $(dirname $(readlink -f $(which javac))))/lib/modules \
  --toolchain-file android-platform=$SDKJAR --toolchain-file d8=$D --toolchain-file aapt2=$D --toolchain-file ndk-version=$D \
  --toolchain-file clang=$D --toolchain-file cmake=$D --toolchain-file gradle-wrapper=gradle/wrapper/gradle-wrapper.jar > "$OUT/r815.log" 2>&1 || { cat "$OUT/r815.log"; exit 3; }
find app/src/main/java app/build/generated/source -name '*.java' > "$OUT/sources.txt"
find app/build/generated/source/r815 -name '*.java' >> "$OUT/sources.txt" 2>/dev/null || true
echo "$OUT/gen/com/ethk3/novaonepro/R.java" >> "$OUT/sources.txt"; echo "$OUT/gen/com/ethk3/novaonepro/BuildConfig.java" >> "$OUT/sources.txt"
find /tmp/claude-0/stubs/androidx -name "*.java" >> "$OUT/sources.txt"
sort -u "$OUT/sources.txt" -o "$OUT/sources.txt"
javac -J-Xmx6g -nowarn -encoding UTF-8 --release 17 -proc:none -Xmaxerrs 400 -cp "$SDKJAR" -d "$OUT/classes" @"$OUT/sources.txt" > "$OUT/javac.log" 2>&1 && echo JAVAC_PASS || { grep -E "error:" "$OUT/javac.log" | head -40; grep -cE "error:" "$OUT/javac.log"; exit 1; }
find "$OUT/classes" -name '*.class' | wc -l
