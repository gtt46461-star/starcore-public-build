#!/usr/bin/env python3
"""Shared helpers for executable V50 engineering and model-evidence gates."""
from __future__ import annotations

import json
import math
import re
from pathlib import Path
from typing import Any


ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / "app" / "src" / "main" / "java" / "com" / "ethk3" / "novaonepro"
REPORT = ROOT / "reports" / "V50_MODEL_LAB.json"


def require(condition: bool, code: str, detail: str = "") -> None:
    if not condition:
        raise AssertionError("%s%s" % (code, ": " + detail if detail else ""))


def close(left: float, right: float, tolerance: float = 1.0e-12) -> bool:
    return math.isclose(float(left), float(right), rel_tol=tolerance, abs_tol=tolerance)


def read_java(name: str) -> str:
    path = JAVA / name
    require(path.is_file(), "MISSING_JAVA", str(path))
    return path.read_text("utf-8")


def load_report() -> dict[str, Any]:
    require(REPORT.is_file(), "MISSING_MODEL_REPORT", str(REPORT))
    value = json.loads(REPORT.read_text("utf-8"))
    require(isinstance(value, dict), "MODEL_REPORT_NOT_OBJECT")
    return value


def java_string(source: str, name: str) -> str:
    match = re.search(
        r"public\s+static\s+final\s+String\s+%s\s*=\s*"
        r"(\"(?:\\.|[^\"\\])*\")\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_STRING_MISSING", name)
    return str(json.loads(match.group(1)))


def java_json(source: str, name: str) -> Any:
    return json.loads(java_string(source, name))


def java_double(source: str, name: str) -> float:
    match = re.search(
        r"public\s+static\s+final\s+double\s+%s\s*=\s*"
        r"([-+0-9.eE]+)d?\s*;" % re.escape(name),
        source,
    )
    require(match is not None, "JAVA_DOUBLE_MISSING", name)
    return float(match.group(1))


def java_int(source: str, name: str) -> int:
    match = re.search(
        r"public\s+static\s+final\s+int\s+%s\s*=\s*([0-9]+)\s*;"
        % re.escape(name),
        source,
    )
    require(match is not None, "JAVA_INT_MISSING", name)
    return int(match.group(1))


def java_bool(source: str, name: str) -> bool:
    match = re.search(
        r"public\s+static\s+final\s+boolean\s+%s\s*=\s*(true|false)\s*;"
        % re.escape(name),
        source,
    )
    require(match is not None, "JAVA_BOOLEAN_MISSING", name)
    return match.group(1) == "true"


def java_double_array(source: str, name: str) -> list[float]:
    match = re.search(
        r"public\s+static\s+final\s+double\[\]\s+%s\s*=\s*"
        r"new\s+double\[\]\s*\{(.*?)\}\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_DOUBLE_ARRAY_MISSING", name)
    text = match.group(1).strip()
    if not text:
        return []
    return [float(item.strip().rstrip("dD")) for item in text.split(",")]


def java_double_matrix(source: str, name: str) -> list[list[float]]:
    match = re.search(
        r"public\s+static\s+final\s+double\[\]\[\]\s+%s\s*=\s*"
        r"new\s+double\[\]\[\]\s*\{(.*?)\}\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_DOUBLE_MATRIX_MISSING", name)
    rows = re.findall(r"\{([^{}]*)\}", match.group(1))
    return [
        [float(item.strip().rstrip("dD")) for item in row.split(",") if item.strip()]
        for row in rows
    ]


def java_string_array(source: str, name: str) -> list[str]:
    match = re.search(
        r"public\s+static\s+final\s+String\[\]\s+%s\s*=\s*"
        r"new\s+String\[\]\s*\{(.*?)\}\s*;" % re.escape(name),
        source,
        re.DOTALL,
    )
    require(match is not None, "JAVA_STRING_ARRAY_MISSING", name)
    return [
        str(json.loads(item))
        for item in re.findall(r"\"(?:\\.|[^\"\\])*\"", match.group(1))
    ]


def emit(name: str, details: dict[str, Any]) -> int:
    print(json.dumps({
        "harness": name,
        "status": "PASS",
        "details": details,
    }, ensure_ascii=False, indent=2, sort_keys=True))
    return 0

