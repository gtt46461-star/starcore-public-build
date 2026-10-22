#!/bin/sh
set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$ROOT"

fail() { echo "R366_SECRET_GATE=FAIL|$1" >&2; exit 1; }

# A missing scanner must fail explicitly instead of making each `if rg` look clean.
command -v rg >/dev/null 2>&1 || fail RIPGREP_MISSING

if rg -q --hidden \
    --glob '!.git/**' \
    --glob '!build/**' \
    --glob '!*.apk' \
    --glob '!*.zip' \
    --glob '!*.bundle' \
    'DUOQU_API_KEY_[AB][[:space:]]*=[[:space:]]*"[A-Za-z0-9]{4,}"' .; then
    fail PROVISIONED_API_KEY_IN_SOURCE
fi

if rg -q --hidden \
    --glob '!.git/**' \
    --glob '!build/**' \
    --glob '!*.apk' \
    --glob '!*.zip' \
    --glob '!*.bundle' \
    '(^|[[:space:]])Password:[[:space:]]*[^[:space:]]+' .; then
    fail SIGNING_PASSWORD_IN_SOURCE
fi

if find . -path './.git' -prune -o -type f \
    \( -name '*.p12' -o -name '*.jks' -o -name '*.keystore' \) -print -quit \
    | rg -q '.'; then
    fail PRIVATE_KEY_FILE_IN_SOURCE_TREE
fi

echo "R366_SECRET_GATE=PASS|PROVISIONED_API_KEY_IN_SOURCE=0|SIGNING_PASSWORD_IN_SOURCE=0|PRIVATE_KEY_FILE_IN_SOURCE_TREE=0"
