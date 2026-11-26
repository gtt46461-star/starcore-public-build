#!/usr/bin/env bash
set -euo pipefail

cd "${GITHUB_WORKSPACE:?}"
repo="$PWD/app-source"
url="https://github.com/${GITHUB_REPOSITORY}.git"

sealed_checkout() {
    local directory=$1 commit=$2
    shift 2
    git init -q "$directory"
    git -C "$directory" remote add origin "$url"
    git -C "$directory" sparse-checkout init --cone
    git -C "$directory" sparse-checkout set "$@"
    git -C "$directory" fetch -q --depth=1 --filter=blob:none origin "$commit"
    git -C "$directory" checkout -q --detach FETCH_HEAD
    test "$(git -C "$directory" rev-parse HEAD)" = "$commit"
}

public_file() {
    local location=$1 file=$2 digest=$3
    curl --fail --silent --show-error --location --retry 3 "$location" -o "$file"
    printf '%s  %s\n' "$digest" "$file" | sha256sum --check
}

r335=construction/plans/STARCORE_R33_5_REVIEW_20261119
r34=construction/plans/STARCORE_R34_20261123
r341=construction/plans/STARCORE_R34_1_REPAIR_20261124
r342=construction/plans/STARCORE_R34_2_REPAIR_20261125
sealed_checkout field-plans 4e1606e9c20c5f01a82cf495b8d39b2630c766f0 "$r335" "$r34"
sealed_checkout previous-plan 2b0177b77fa5f6079da27c915201b95b8bdcef20 "$r341"
sealed_checkout current-plan e95b72c6a6a963d7f1a1db3596bb2f05b501240f "$r342"

release="https://github.com/${GITHUB_REPOSITORY}/releases/download"
public_file "$release/r33.5-plan/33.5.zip" "$RUNNER_TEMP/33.5.zip" 01af8e98ae4d0917b7dd3abe4bff2c44ef8e47cd80136df635cbb0441d6b3a79
public_file "https://raw.githubusercontent.com/${GITHUB_REPOSITORY}/0fcf8b9e6286e24dc185328af0d02a0458aaee03/STARCORE-R32.5.1-C1-033-release.apk" "$RUNNER_TEMP/formal-033.apk" 8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763
public_file "$release/r34.1-B12-eb8f82a8bda8/STARCORE-R341-577311152-eb8f82a8bda8-release.apk" "$RUNNER_TEMP/baseline152.apk" 2afc220e8942f7eda678b7ddd09ca16c48225c2f4f99167a9233a5e9bf1a8c38
python3 -B "field-plans/$r335/prepare.py" "$RUNNER_TEMP/33.5.zip" "$RUNNER_TEMP/material"
mkdir -p "$RUNNER_TEMP/engineering"
python3 -B "field-plans/$r34/tools/r34/verify_plan.py" --plan "$PWD/field-plans/$r34" > "$RUNNER_TEMP/engineering/r34-plan.json"
python3 -B "$repo/tools/r34/test_legacy_r335.py" --plan "$PWD/field-plans/$r34" --legacy "$RUNNER_TEMP/material/current-review"
python3 -B "previous-plan/$r341/tools/verify_plan.py" --plan "$PWD/previous-plan/$r341" --source-root "$repo" > "$RUNNER_TEMP/engineering/r341-plan.json"
python3 -B "current-plan/$r342/tools/verify_plan.py" --plan "$PWD/current-plan/$r342" --source-repo "$repo" --out "$RUNNER_TEMP/engineering/r342-plan.json"
{
    printf 'STARCORE_R33_PLAN_ROOT=%s\n' "$RUNNER_TEMP/material/STARCORE_R33_5_20261118"
    printf 'STARCORE_R34_PLAN_ROOT=%s\n' "$PWD/field-plans/$r34"
    printf 'STARCORE_R341_PLAN_ROOT=%s\n' "$PWD/previous-plan/$r341"
    printf 'STARCORE_R342_PLAN_ROOT=%s\n' "$PWD/current-plan/$r342"
} >> "$GITHUB_ENV"
