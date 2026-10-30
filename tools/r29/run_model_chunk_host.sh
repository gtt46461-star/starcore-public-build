#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/batch01/model-chunk-host}
mkdir -p "$OUT"
CHUNK_TMP=$(mktemp -d)
trap 'rm -rf "$CHUNK_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$CHUNK_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$CHUNK_TMP/shim:$CLASSES" -d "$CHUNK_TMP/test" tools/r29/qual_host/ModelArtifactChunkR29HostTest.java
java -Xmx512m -cp "$CHUNK_TMP/shim:$CHUNK_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.ModelArtifactChunkR29HostTest | tee "$OUT/r29_model_chunk_host.log"
