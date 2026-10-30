#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/../.."
OUT=${OUT:-evidence/r29/domain-upgrade-host}
mkdir -p "$OUT"
DOMAIN_TMP=$(mktemp -d)
trap 'rm -rf "$DOMAIN_TMP"' EXIT
javac -nowarn -encoding UTF-8 -proc:none -d "$DOMAIN_TMP/shim" $(find tools/r29/qual_host/shim -name '*.java')
javac -nowarn -encoding UTF-8 -proc:none -cp "$DOMAIN_TMP/shim:$CLASSES" -d "$DOMAIN_TMP/test" tools/r29/qual_host/LegacyDomainUpgradeR29HostTest.java
java -cp "$DOMAIN_TMP/shim:$DOMAIN_TMP/test:$CLASSES:$SQLITE_JDBC" com.ethk3.novaonepro.strategyv2.LegacyDomainUpgradeR29HostTest | tee "$OUT/r29_domain_upgrade_host.log"
