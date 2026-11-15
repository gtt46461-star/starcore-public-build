#!/usr/bin/env python3
"""Convert the uploaded ETHPK10 CSV to issue,n1,...,n10 for Java harnesses."""
import csv
import sys

if len(sys.argv) != 3:
    raise SystemExit("usage: convert_training_csv.py input.csv output.csv")
with open(sys.argv[1], encoding="utf-8-sig", newline="") as src, open(sys.argv[2], "w", encoding="utf-8", newline="") as dst:
    reader = csv.DictReader(src)
    writer = csv.writer(dst)
    for row in reader:
        if row.get("valid") != "1":
            continue
        nums = [x.strip() for x in (row.get("open_number") or "").split(",")]
        if len(nums) != 10:
            continue
        writer.writerow([row.get("period", "")] + [int(x) for x in nums])
