#!/usr/bin/env python3
"""Prove that compiled Dex, resources.arsc and launch layouts share one R contract."""

from __future__ import annotations

import argparse
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ANDROID_NS = "{http://schemas.android.com/apk/res/android}"
SMALI_FIELD = re.compile(
    r"^\.field public static final ([A-Za-z_][A-Za-z0-9_]*):I = (0x[0-9a-fA-F]+|-?[0-9]+)$"
)
SOURCE_R_REF = re.compile(
    r"(?<!android\.)\bR\.([A-Za-z_][A-Za-z0-9_]*)\.([A-Za-z_][A-Za-z0-9_]*)"
)


def fail(code: str, details: list[str] | None = None) -> None:
    print(f"R920_APK_RESOURCE_CONTRACT=FAIL|{code}", file=sys.stderr)
    for detail in (details or [])[:80]:
        print(detail, file=sys.stderr)
    raise SystemExit(1)


def public_resources(decoded: Path) -> dict[tuple[str, str], int]:
    path = decoded / "res" / "values" / "public.xml"
    if not path.is_file():
        fail("PUBLIC_XML_MISSING")
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        fail(f"PUBLIC_XML_PARSE:{type(exc).__name__}")
    result: dict[tuple[str, str], int] = {}
    reverse: dict[int, tuple[str, str]] = {}
    for node in root.findall("public"):
        resource_type = (node.get("type") or "").strip()
        name = (node.get("name") or "").strip()
        raw_id = (node.get("id") or "").strip()
        if not resource_type or not name or not re.fullmatch(r"0x[0-9a-fA-F]{8}", raw_id):
            fail(f"PUBLIC_XML_INVALID:{resource_type}/{name}:{raw_id}")
        key = (resource_type, name)
        value = int(raw_id, 16)
        if key in result:
            fail(f"PUBLIC_DUPLICATE_NAME:{resource_type}/{name}")
        if value in reverse and reverse[value] != key:
            fail(
                "PUBLIC_DUPLICATE_ID",
                [f"0x{value:08x}={reverse[value]} and {key}"],
            )
        result[key] = value
        reverse[value] = key
    if not result:
        fail("PUBLIC_EMPTY")
    return result


def dex_resources(smali: Path) -> dict[tuple[str, str], int]:
    package_dir = smali / "com" / "ethk3" / "novaonepro"
    files = sorted(package_dir.glob("R$*.smali"))
    if not files:
        fail("DEX_R_CLASSES_MISSING")
    result: dict[tuple[str, str], int] = {}
    for path in files:
        match = re.fullmatch(r"R\$([A-Za-z_][A-Za-z0-9_]*)\.smali", path.name)
        if match is None:
            continue
        resource_type = match.group(1)
        for line in path.read_text(encoding="utf-8").splitlines():
            field = SMALI_FIELD.fullmatch(line.strip())
            if field is None:
                continue
            name, raw_value = field.groups()
            value = int(raw_value, 0)
            key = (resource_type, name)
            if key in result:
                fail(f"DEX_DUPLICATE_NAME:{resource_type}/{name}")
            result[key] = value
    if not result:
        fail("DEX_R_FIELDS_EMPTY")
    return result


def compare_resource_maps(
    compiled: dict[tuple[str, str], int], dex: dict[tuple[str, str], int]
) -> None:
    details: list[str] = []
    for key in sorted(set(compiled) | set(dex)):
        compiled_value = compiled.get(key)
        dex_value = dex.get(key)
        if compiled_value != dex_value:
            details.append(
                f"{key[0]}/{key[1]}|arsc="
                f"{('MISSING' if compiled_value is None else f'0x{compiled_value:08x}')}"
                f"|dex={('MISSING' if dex_value is None else f'0x{dex_value:08x}')}"
            )
    if details:
        fail(f"DEX_ARSC_ID_MISMATCH:{len(details)}", details)


def verify_source_refs(
    source_root: Path, compiled: dict[tuple[str, str], int]
) -> int:
    references: set[tuple[str, str]] = set()
    for path in sorted(source_root.rglob("*.java")):
        text = path.read_text(encoding="utf-8")
        references.update(SOURCE_R_REF.findall(text))
    missing = [f"{kind}/{name}" for kind, name in sorted(references) if (kind, name) not in compiled]
    if missing:
        fail(f"SOURCE_RESOURCE_MISSING:{len(missing)}", missing)
    return len(references)


def layout_ids(path: Path) -> dict[str, str]:
    if not path.is_file():
        fail(f"LAYOUT_MISSING:{path.name}")
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        fail(f"LAYOUT_PARSE:{path.name}:{type(exc).__name__}")
    result: dict[str, str] = {}
    for node in root.iter():
        raw = node.get(ANDROID_NS + "id", "")
        match = re.fullmatch(r"@\+?id/([A-Za-z_][A-Za-z0-9_]*)", raw)
        if match is None:
            continue
        name = match.group(1)
        tag = node.tag.rsplit("}", 1)[-1].rsplit(".", 1)[-1]
        if name in result:
            fail(f"LAYOUT_DUPLICATE_ID:{path.name}:{name}")
        result[name] = tag
    return result


def compatible(expected: str, actual: str) -> bool:
    if expected == "View":
        return True
    if expected == "TextView":
        return actual in {"TextView", "Button"}
    return expected == actual


def verify_launch_bindings(source_root: Path, decoded: Path) -> tuple[int, int]:
    main_source = (source_root / "MainActivity.java").read_text(encoding="utf-8")
    begin = main_source.find("private void bindViews()")
    end = main_source.find("private void fixRuntimeTopTextViews()", begin)
    if begin < 0 or end < 0:
        fail("MAIN_BIND_VIEWS_BLOCK_MISSING")
    block = main_source[begin:end]
    expected: dict[str, str] = {}
    for name in re.findall(r"findText\(R\.id\.([A-Za-z_][A-Za-z0-9_]*)\)", block):
        expected[name] = "TextView"
    for view_type, name in re.findall(
        r"\((TextView|LinearLayout|Button)\)\s*findViewById\(R\.id\.([A-Za-z_][A-Za-z0-9_]*)\)",
        block,
    ):
        expected[name] = view_type
    for name in re.findall(r"(?<!\))\bfindViewById\(R\.id\.([A-Za-z_][A-Za-z0-9_]*)\)", block):
        expected.setdefault(name, "View")

    actual = layout_ids(decoded / "res" / "layout" / "activity_main.xml")
    errors = []
    for name, view_type in sorted(expected.items()):
        actual_type = actual.get(name)
        if actual_type is None:
            errors.append(f"activity_main:{name}|expected={view_type}|actual=MISSING")
        elif not compatible(view_type, actual_type):
            errors.append(f"activity_main:{name}|expected={view_type}|actual={actual_type}")
    if errors:
        fail(f"MAIN_BINDING_MISMATCH:{len(errors)}", errors)

    row_contracts = {
        "row_diagnostic_state_r906.xml": {
            "txt_diag_state_title": "TextView",
            "txt_diag_state_value": "TextView",
            "txt_diag_state_source": "TextView",
        },
        "row_why_truth_r906.xml": {
            "txt_why_truth_title": "TextView",
            "txt_why_truth_detail": "TextView",
            "txt_why_truth_source": "TextView",
        },
    }
    row_count = 0
    for filename, contract in row_contracts.items():
        row_ids = layout_ids(decoded / "res" / "layout" / filename)
        for name, view_type in contract.items():
            row_count += 1
            actual_type = row_ids.get(name)
            if actual_type is None or not compatible(view_type, actual_type):
                fail(
                    "ROW_BINDING_MISMATCH",
                    [f"{filename}:{name}|expected={view_type}|actual={actual_type or 'MISSING'}"],
                )
    return len(expected), row_count


def verify_launcher_classes(decoded: Path, smali: Path) -> None:
    manifest = decoded / "AndroidManifest.xml"
    if not manifest.is_file():
        fail("MANIFEST_MISSING")
    text = manifest.read_text(encoding="utf-8")
    required_manifest = [
        'android:name=".StarcoreApplication"',
        'android:name=".MainActivity"',
        'android.intent.action.MAIN',
        'android.intent.category.LAUNCHER',
    ]
    missing = [value for value in required_manifest if value not in text]
    if missing:
        fail("LAUNCHER_MANIFEST_CONTRACT", missing)
    package_dir = smali / "com" / "ethk3" / "novaonepro"
    for class_name in ("StarcoreApplication", "MainActivity", "CrashRecorder"):
        if not (package_dir / f"{class_name}.smali").is_file():
            fail(f"LAUNCH_CLASS_MISSING:{class_name}")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("decoded_apk", type=Path)
    parser.add_argument("smali_root", type=Path)
    parser.add_argument("source_package", type=Path)
    arguments = parser.parse_args()

    compiled = public_resources(arguments.decoded_apk)
    dex = dex_resources(arguments.smali_root)
    compare_resource_maps(compiled, dex)
    source_refs = verify_source_refs(arguments.source_package, compiled)
    main_bindings, row_bindings = verify_launch_bindings(
        arguments.source_package, arguments.decoded_apk
    )
    verify_launcher_classes(arguments.decoded_apk, arguments.smali_root)
    print(
        "R920_APK_RESOURCE_CONTRACT=PASS"
        f"|PUBLIC={len(compiled)}|DEX_R={len(dex)}|SOURCE_REFS={source_refs}"
        f"|MAIN_BINDINGS={main_bindings}|ROW_BINDINGS={row_bindings}"
        "|RESOURCE_ID_MISMATCH=0|LAUNCHER_CLASSES=3/3"
    )


if __name__ == "__main__":
    main()
