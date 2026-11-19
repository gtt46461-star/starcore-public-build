#!/usr/bin/env python3
"""Recheck R33.5's two diagnostic indexes against the sealed reports inside PLAN."""

import argparse
import csv
import hashlib
import io
import json
import sys
import zipfile
from collections import Counter
from pathlib import Path


REPORTS = {
    "早报": (
        "STARCORE-e476ca6126c21e0368a62fe93c6a8db9e4a82b4eb4a.zip",
        "e164852debfeab84eaffa4f98e215ce855bbeec5ff20334da5b0e0aab909f19a",
    ),
    "晚报": (
        "STARCORE-d8ca0632b9fc9ac425b04aa32600750bf16bb513e4c.zip",
        "da57ec5917c8922132c331c600f607d0833054879955895a8550897174c2b18f",
    ),
}
DOCUMENTS = ("UPGRADE_DIAGNOSIS.json", "R30_REPORT.json")
FIELDS = {"nextAction", "prescription", "collectionAction", "instruction", "verify", "verificationEntry"}
CATEGORIES = {"对象": 165, "规则": 16, "概览": 12, "章节": 9, "旧finding": 1}


def require(ok, message):
    if not ok:
        raise ValueError(message)


def sha(data):
    return hashlib.sha256(data).hexdigest()


def load_report(path, digest):
    data = path.read_bytes()
    require(sha(data) == digest, f"原始报告哈希不符 {path.name}")
    with zipfile.ZipFile(io.BytesIO(data)) as outer:
        exports = [name for name in outer.namelist() if name.endswith("/STARCORE_FULL_REPORT.zip")]
        require(len(exports) == 1, f"内包数量异常 {path.name}")
        name = exports[0]
        inner = outer.read(name)
        index = json.loads(outer.read(name.rsplit("/", 1)[0] + "/index.json"))
        require(len(index["files"]) == 1, f"index.json 分卷数量异常 {path.name}")
        expected = index["files"][0]
        require(len(inner) == expected["bytes"] and sha(inner) == expected["sha256"],
                f"内包长度或哈希不符 {path.name}")
    with zipfile.ZipFile(io.BytesIO(inner)) as archive:
        return name, {f: json.loads(archive.read(f)) for f in DOCUMENTS}


def rows(path):
    with path.open(encoding="utf-8-sig", newline="") as handle:
        yield from csv.DictReader(handle)


def read_pointer(document, pointer):
    require(pointer.startswith("/"), f"缺少绝对 JSON Pointer {pointer}")
    value = document
    for token in pointer[1:].split("/"):
        token = token.replace("~1", "/").replace("~0", "~")
        if isinstance(value, list):
            require(token.isascii() and token.isdigit() and str(int(token)) == token,
                    f"数组索引无效 {pointer}")
            value = value[int(token)]
        else:
            value = value[token]
    return value


def field_values(value, path=""):
    if isinstance(value, list):
        for i, item in enumerate(value):
            yield from field_values(item, f"{path}/{i}")
    elif isinstance(value, dict):
        for key, item in value.items():
            pointer = path + "/" + key.replace("~", "~0").replace("/", "~1")
            if key in FIELDS and isinstance(item, str):
                yield pointer, item
            else:
                yield from field_values(item, pointer)


def verify(plan):
    reports = {name: load_report(plan / "evidence" / "original" / name, digest)
               for name, digest in REPORTS.values()}
    counts = Counter()
    seen_keys = set()
    seen_refs = set()
    for row in rows(plan / "control" / "两份报告原处方与施工索引.csv"):
        key = row["key"]
        require(key not in seen_keys and key == row["类别"] + "/" + row["原ID"], f"重复或错误对象键 {key}")
        seen_keys.add(key)
        counts[row["类别"]] += 1
        for prefix, (filename, _) in REPORTS.items():
            require(row[prefix + "ZIP"] == filename, f"早晚报告身份不符 {key}")
            outer_entry, documents = reports[filename]
            location = row[prefix + "原文位置"]
            require((filename, location) not in seen_refs, f"重复原始位置 {key} {prefix}")
            seen_refs.add((filename, location))
            values = []
            for part in location.split(" + "):
                inner_entry, source = part.split("!/")
                require(inner_entry == outer_entry, f"报告内包路径不符 {key}")
                doc, pointer = source.split("#", 1)
                values.append(read_pointer(documents[doc], pointer))
            expected_parts = 2 if row["类别"] == "旧finding" else 1
            require(len(values) == expected_parts, f"原始记录复合项数量异常 {key}")
            record = values if len(values) == 2 else values[0]
            canonical = json.dumps(record, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode()
            require(sha(canonical) == row[prefix + "整条规范JSON_SHA256"], f"原始记录摘要不符 {prefix} {key}")
    require(counts == CATEGORIES and len(seen_refs) == 406, f"双报告记录覆盖不足 {counts}")
    truth = {(filename, doc, pointer): text for filename, (_, documents) in reports.items()
             for doc, data in documents.items() for pointer, text in field_values(data)}
    require(len(truth) == 2474, f"原始字段集合数量变化 {len(truth)}")
    seen_fields = set()
    for row in rows(plan / "control" / "两报告诊断字段原文索引.csv"):
        address = tuple(row[k] for k in ("033原包", "报告内文件", "JSON Pointer"))
        require(address not in seen_fields and address in truth, f"重复或未知字段 {address}")
        require(row["原文或实际空字符串"] == truth[address], f"字段值不符 {address}")
        seen_fields.add(address)
    require(seen_fields == truth.keys(), f"索引遗漏 {len(truth) - len(seen_fields)} 个原始字段")
    return {"status": "PASS", "reports": len(reports), "comparisonRows": len(seen_keys),
            "recordDigests": len(seen_refs), "fieldPointers": len(seen_fields),
            "scope": "ORIGINAL_REPORT_INDEX_TRACEABILITY"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("plan", metavar="PLAN", type=Path, help="解压后同时含 control/ 和 evidence/ 的目录")
    args = parser.parse_args()
    try:
        result = verify(args.plan)
    except (ValueError, KeyError, IndexError, TypeError, OSError, zipfile.BadZipFile, csv.Error) as error:
        print(json.dumps({"status": "FAIL", "error": str(error)}, ensure_ascii=False), file=sys.stderr)
        return 1
    print(json.dumps(result, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    sys.exit(main())
