"""Compare the old handset's diagnostic scope against its independently written immutable catalog."""
import hashlib
import io
import json
import re
import zipfile
from pathlib import Path

path = Path("construction/plans/STARCORE_R34_3_REPAIR_20261126/report/ORIGINAL_DIAGNOSTIC_173.zip")
with zipfile.ZipFile(path) as outer, zipfile.ZipFile(io.BytesIO(outer.read(outer.namelist()[0]))) as archive:
    original = archive.read("DIAGNOSTIC_SCOPE.json")
    scope = json.loads(original)
    counts = {}
    for name in sorted(n for n in archive.namelist() if re.fullmatch(r"catalog-\d+\.jsonl", n)):
        with archive.open(name) as page:
            for line in page:
                row = json.loads(line)
                if row["kind"] == "table_count" and row["status"] == "OK":
                    counts[row["table"]] = row["rows"]
    tables = scope["tables"]
    assert counts == {row["table"]: row["rows"] for row in tables}
    schema = {}
    for name in sorted(n for n in archive.namelist() if re.fullmatch(r"catalog-\d+\.jsonl", n)):
        with archive.open(name) as page:
            for line in page:
                row = json.loads(line)
                if row["kind"] == "pragma_row" and row["operation"] == "table_info":
                    schema.setdefault(row["object_name"],[]).append(dict(name=row["name"],type=row["type"],notNull=bool(row["notnull"]),primaryKeyOrder=row["pk"]))
    for row in tables:
        assert row["columns"] == schema.get(row["table"],[]), row["table"]
    empty = [row for row in tables if row["rows"] == 0]
    assert all(not row["states"] for row in empty)
    for row in tables:
        totals = {}
        for state in row["states"]:
            column = state["column"]
            totals[column] = totals.get(column, 0) + state["rows"]
        assert all(n == row["rows"] for n in totals.values())
    for section in scope["recentEvidence"]:
        if "totalRows" in section:
            assert section["totalRows"] == counts[section["table"]]
    print(json.dumps(dict(scopeSha256=hashlib.sha256(original).hexdigest(),scopeBytes=len(original),
        tables=len(tables),emptyTables=len(empty),largestTableRows=max(counts.values()),
        tableDefinitionsMatched=len(tables),totalRows=sum(counts.values()),allCountsAndGroupsMatched=True), sort_keys=True))
