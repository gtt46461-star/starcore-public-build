#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/domain-extension-host}
mkdir -p "$OUT"
EXTENSION_TMP=$(mktemp -d)
trap 'rm -rf "$EXTENSION_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$EXTENSION_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$EXTENSION_TMP/shim:$CLASSES" -d "$EXTENSION_TMP/test" tools/r29/qual_host/LegacyDomainExtensionR29HostTest.java
java -cp "$EXTENSION_TMP/shim:$EXTENSION_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.LegacyDomainExtensionR29HostTest | tee "$OUT/r29_domain_extension_host.log"
