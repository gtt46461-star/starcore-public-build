"""Compare retained originals from two normal phone exports and the separately measured APK transition."""
import argparse
import hashlib
import json
import tempfile
from pathlib import Path
from zipfile import ZipFile

from verify_overlay_032 import check_transition

DRAW_FIELDS = ("issue", "open_time", "draw_hash_v2") + tuple("n" + str(i) for i in range(1, 11))
FROZEN_FIELDS = ("game_code", "target_issue", "base_issue", "position", "n1", "n2", "n3", "decision_action",
                 "record_hash", "contract_hash", "snapshot_hash", "commit_at", "deadline_at",
                 "actual_number", "hit_label", "settlement_hash", "settled_at")
LIMIT = 32


def require(test, message):
    if not test:
        raise ValueError(message)


def read_json(archive, name):
    matches = [x for x in archive.infolist() if x.filename == name]
    require(len(matches) == 1, "missing/duplicate member " + name)
    with archive.open(matches[0]) as src:
        return json.load(src)


def snapshot(path):
    with ZipFile(path) as outer, tempfile.TemporaryFile() as temporary:
        candidates = [x for x in outer.infolist() if x.filename.split("/")[-1] == "STARCORE_FULL_REPORT.zip"]
        require(len(candidates) == 1, "expected one original field export")
        with outer.open(candidates[0]) as member:
            while block := member.read(1024 * 1024):
                temporary.write(block)
        temporary.seek(0)
        with ZipFile(temporary) as archive:
            source = read_json(archive, "source-manifest.json")
            data = read_json(archive, "DIAGNOSTIC_SCOPE.json")
            require(source.get("snapshotSha256") and source["snapshotSha256"] == data.get("snapshotSha256"),
                    "export contains mixed snapshots")
            by_name = {}
            for table in data["recentEvidence"]:
                name = table["table"]
                if name not in ("draws", "v57_shadow_observation_r35"):
                    continue
                require(name not in by_name and table.get("complete") is True and
                        len(table["rows"]) == int(table["exportedRows"]) == int(table["totalRows"]),
                        "incomplete or duplicate projection " + name)
                by_name[name] = table["rows"]
            require(len(by_name) == 2, "missing draw or canonical observation evidence")
    return source, by_name


def canonical_map(rows, key_fields, checked_fields, eligible):
    result = {}
    for row in rows:
        if not eligible(row):
            continue
        require(all(k in row for k in checked_fields), "missing original fields")
        key = tuple(str(row[k]) for k in key_fields)
        require(all(k not in ("None", "") for k in key) and key not in result, "empty or duplicate original key")
        facts = [row[k] for k in checked_fields]
        result[key] = hashlib.sha256(json.dumps(facts, ensure_ascii=False, separators=(",", ":")).encode()).hexdigest()
    return result


def preserved(before, after, sample):
    selected = sorted(before)[-sample:]
    require(len(selected) == sample, "insufficient pre-upgrade fixture; seed the old version via its normal entry")
    for key in selected:
        require(before[key] == after.get(key), "missing or changed retained original " + str(key))
    return {"count": len(selected), "first": selected[0], "last": selected[-1],
            "digest": hashlib.sha256("".join(before[k] for k in selected).encode()).hexdigest()}


def compare_rows(previous, current):
    result = {}
    for name, keys, fields, eligible in [
        ("draws", ("issue",), DRAW_FIELDS, lambda r: str(r["dirty"]) == "0"),
        ("v57_shadow_observation_r35", ("target_issue", "game_code"), FROZEN_FIELDS,
         lambda r: str(r["settled"]) == "1" and str(r["pre_draw_frozen"]) == "1" and bool(r["record_hash"] and r["settlement_hash"])),
    ]:
        old = canonical_map(previous[name], keys, fields, eligible)
        new = canonical_map(current[name], keys, fields, eligible)
        result[name] = preserved(old, new, LIMIT)
    return result


def verify(old, new, overlay):
    prior, after = overlay["before"], overlay["after"]
    check_transition(prior, after)
    require(overlay.get("status") == "PASS" and overlay.get("markerPreserved") is True and
            overlay.get("installedBeforeSha256") == prior["sha256"] and overlay.get("installedAfterSha256") == after["sha256"],
            "exact installed predecessor, candidate and marker evidence required")
    (old_source, old_rows), (new_source, new_rows) = snapshot(old), snapshot(new)
    require(old_source["apkSha256"] == prior["sha256"] and new_source["apkSha256"] == after["sha256"], "export identities differ from device APKs")
    require(old_source["snapshotSha256"] != new_source["snapshotSha256"], "same snapshot reused")
    return {"scope": "SAMPLED_IMMUTABLE_DRAWS_AND_SETTLED_FREEZES_ONLY", "beforeSource": old_source["sourceCommit"],
            "afterSource": new_source["sourceCommit"], "rows": compare_rows(old_rows, new_rows)}


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("before", type=Path)
    p.add_argument("after", type=Path)
    p.add_argument("--overlay", type=Path, required=True)
    args = p.parse_args()
    print(json.dumps(verify(args.before, args.after, json.loads(args.overlay.read_text())), ensure_ascii=False, indent=2))
