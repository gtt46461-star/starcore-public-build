#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/legacy-learning-host}
mkdir -p "$OUT"
LEARNING_TMP=$(mktemp -d)
trap 'rm -rf "$LEARNING_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$LEARNING_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$LEARNING_TMP/shim:$CLASSES" -d "$LEARNING_TMP/test" tools/r29/qual_host/LegacyLearningCommitR29HostTest.java
java -cp "$LEARNING_TMP/shim:$LEARNING_TMP/test:$CLASSES:$SQLITE_JDBC:${ANDROID_JAR:-$ANDROID_HOME/platforms/android-36/android.jar}" com.ethk3.novaonepro.LegacyLearningCommitR29HostTest | tee "$OUT/r29_legacy_learning_host.log"
