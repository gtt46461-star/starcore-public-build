#!/usr/bin/env python3
"""Generate the exact Java R contract from apktool's compiled public.xml.

R919 compiled product Java against a hand-maintained R.java before apktool allocated IDs for
new resources.  The resulting Dex and resources.arsc disagreed and MainActivity could crash on
its first findViewById.  R922 makes compiled resources the source of truth: stage one builds and
decodes the resource table, this program generates R.java from that table, and only then is
product Java compiled.
"""

from __future__ import annotations

import argparse
import os
import re
import tempfile
import xml.etree.ElementTree as ET
from collections import defaultdict
from pathlib import Path


JAVA_IDENTIFIER = re.compile(r"^[A-Za-z_$][A-Za-z0-9_$]*$")
ANDROID_RESOURCE_NAME = re.compile(r"^[A-Za-z_][A-Za-z0-9_]*$")


def fail(code: str) -> None:
    raise SystemExit(f"R922_R_GENERATOR=FAIL|{code}")


def parse_public_xml(path: Path) -> dict[str, list[tuple[str, int]]]:
    if not path.is_file():
        fail(f"PUBLIC_XML_MISSING:{path}")
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as exc:
        fail(f"PUBLIC_XML_PARSE:{type(exc).__name__}")
    if root.tag != "resources":
        fail(f"PUBLIC_XML_ROOT:{root.tag}")

    by_type: dict[str, list[tuple[str, int]]] = defaultdict(list)
    names: set[tuple[str, str]] = set()
    ids: dict[int, tuple[str, str]] = {}
    for node in root.findall("public"):
        resource_type = (node.get("type") or "").strip()
        name = (node.get("name") or "").strip()
        raw_id = (node.get("id") or "").strip().lower()
        if not JAVA_IDENTIFIER.fullmatch(resource_type):
            fail(f"INVALID_TYPE:{resource_type}")
        if not ANDROID_RESOURCE_NAME.fullmatch(name):
            fail(f"INVALID_NAME:{resource_type}/{name}")
        if not re.fullmatch(r"0x[0-9a-f]{8}", raw_id):
            fail(f"INVALID_ID:{resource_type}/{name}:{raw_id}")
        value = int(raw_id, 16)
        if value < 0x7F000000 or value > 0x7FFFFFFF:
            fail(f"NON_APP_ID:{resource_type}/{name}:{raw_id}")
        key = (resource_type, name)
        if key in names:
            fail(f"DUPLICATE_NAME:{resource_type}/{name}")
        previous = ids.get(value)
        if previous is not None and previous != key:
            fail(
                "DUPLICATE_ID:"
                f"{raw_id}:{previous[0]}/{previous[1]}:{resource_type}/{name}"
            )
        names.add(key)
        ids[value] = key
        by_type[resource_type].append((name, value))

    if not by_type or "id" not in by_type or "layout" not in by_type:
        fail("INCOMPLETE_RESOURCE_TABLE")
    return {
        resource_type: sorted(entries, key=lambda item: (item[1], item[0]))
        for resource_type, entries in sorted(by_type.items())
    }


def render_java(package_name: str, resources: dict[str, list[tuple[str, int]]]) -> str:
    if not re.fullmatch(r"[A-Za-z_$][A-Za-z0-9_$.]*", package_name):
        fail(f"INVALID_PACKAGE:{package_name}")
    lines = [
        f"package {package_name};",
        "",
        "/** AUTO-GENERATED from the compiled resources.arsc public contract. */",
        "public final class R {",
        "    private R() {}",
    ]
    for resource_type, entries in resources.items():
        lines.append(f"    public static final class {resource_type} {{")
        lines.append(f"        private {resource_type}() {{}}")
        for name, value in entries:
            lines.append(f"        public static final int {name} = 0x{value:08x};")
        lines.append("    }")
    lines.append("}")
    lines.append("")
    return "\n".join(lines)


def write_atomic(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    file_descriptor, temporary_name = tempfile.mkstemp(
        prefix=path.name + ".", suffix=".tmp", dir=str(path.parent)
    )
    try:
        with os.fdopen(file_descriptor, "w", encoding="utf-8", newline="\n") as stream:
            stream.write(content)
            stream.flush()
            os.fsync(stream.fileno())
        os.replace(temporary_name, path)
    except BaseException:
        try:
            os.unlink(temporary_name)
        except OSError:
            pass
        raise


def write_inventory(path: Path, resources: dict[str, list[tuple[str, int]]]) -> None:
    rows = ["type\tname\tid"]
    for resource_type, entries in resources.items():
        for name, value in entries:
            rows.append(f"{resource_type}\t{name}\t0x{value:08x}")
    write_atomic(path, "\n".join(rows) + "\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("public_xml", type=Path)
    parser.add_argument("output_java", type=Path)
    parser.add_argument("--package", default="com.ethk3.novaonepro")
    parser.add_argument("--inventory", type=Path)
    arguments = parser.parse_args()

    resources = parse_public_xml(arguments.public_xml)
    write_atomic(arguments.output_java, render_java(arguments.package, resources))
    if arguments.inventory is not None:
        write_inventory(arguments.inventory, resources)
    count = sum(len(entries) for entries in resources.values())
    print(
        "R922_R_GENERATOR=PASS"
        f"|TYPES={len(resources)}|RESOURCES={count}"
        f"|IDS={len(resources.get('id', []))}|LAYOUTS={len(resources.get('layout', []))}"
    )


if __name__ == "__main__":
    main()
