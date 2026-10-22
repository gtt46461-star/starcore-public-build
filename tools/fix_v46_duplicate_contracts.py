#!/usr/bin/env python3
"""Remove duplicate V46 contract members from Models.Decision and ComboCandidate.

Idempotent compile fix. It preserves the canonical V46 owner block and the
conditional-calibration-only fields from the later compatibility block.
"""
from pathlib import Path
import collections
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
PATH = ROOT / "app/src/main/java/com/ethk3/novaonepro/Models.java"
DIAG_PATH = ROOT / "app/src/main/java/com/ethk3/novaonepro/DecisionFunnelDiagnosisEngine.java"
MARKER = "/* V46 separated event probability, conservative action bound and final funnel. */"
DUPLICATE_NAMES = {
    "v46EventProbability",
    "v46ActionLcb",
    "v46ProbabilityUncertainty",
    "v46ExpectedValue",
    "v46FinalRank",
    "v46FormalCandidateCount",
    "v46ProbabilityTrace",
    "v46NumberDistributionSummary",
    "v46RootCauseCode",
    "v46RootCauseOwner",
    "v46Version",
}


def class_range(source: str, class_name: str, next_class_name: str) -> tuple[int, int]:
    start = source.index("class " + class_name + " {")
    end = source.index("class " + next_class_name + " {", start)
    return start, end


def normalize_class(source: str, class_name: str, next_class_name: str) -> tuple[str, list[str]]:
    start, end = class_range(source, class_name, next_class_name)
    section = source[start:end]
    if MARKER not in section:
        raise RuntimeError("missing V46 compatibility marker in " + class_name)
    marker_at = section.index(MARKER)
    prefix = section[:marker_at]
    lines = section[marker_at:].splitlines(True)
    output: list[str] = []
    removed: list[str] = []
    active = True
    for line in lines:
        if active:
            match = re.match(r"\s*public\s+(?:double|int|String)\s+(v46\w+)\s*;", line)
            if match and match.group(1) in DUPLICATE_NAMES:
                removed.append(match.group(1))
                continue
            if "public String actionUtilityVersion;" in line:
                active = False
        output.append(line)
    if removed and set(removed) != DUPLICATE_NAMES:
        raise RuntimeError(class_name + " partial duplicate set: " + repr(removed))
    return source[:start] + prefix + "".join(output) + source[end:], removed


def verify_no_duplicate_fields() -> None:
    try:
        import javalang
    except Exception as exc:
        raise RuntimeError("javalang is required for duplicate-field verification") from exc
    tree = javalang.parse.parse(PATH.read_text("utf-8"))
    problems: list[tuple[str, list[str]]] = []
    for _, declaration in tree.filter(javalang.tree.ClassDeclaration):
        names: list[str] = []
        for field in declaration.fields:
            names.extend(item.name for item in field.declarators)
        duplicates = sorted(name for name, count in collections.Counter(names).items() if count > 1)
        if duplicates:
            problems.append((declaration.name, duplicates))
    if problems:
        raise RuntimeError("duplicate Java members remain: " + repr(problems))


def normalize_java7_anonymous_capture() -> bool:
    source = DIAG_PATH.read_text("utf-8")
    old = "private static String sorted(HashMap<String,Integer> m, int max)"
    new = "private static String sorted(final HashMap<String,Integer> m, int max)"
    if old in source:
        source = source.replace(old, new, 1)
        DIAG_PATH.write_text(source, "utf-8")
        print("fixed Java7 anonymous Comparator capture: m declared final")
        changed = True
    else:
        changed = False
    verified = DIAG_PATH.read_text("utf-8")
    if verified.count(new) != 1 or old in verified:
        raise RuntimeError("Java7 final-capture contract is not satisfied")
    print("PASS DecisionFunnelDiagnosisEngine Java7 capture contract")
    return changed


def main() -> int:
    source = PATH.read_text("utf-8")
    source, decision_removed = normalize_class(source, "Decision", "Trade")
    source, combo_removed = normalize_class(source, "ComboCandidate", "ProbabilityResult")
    if decision_removed or combo_removed:
        if len(decision_removed) != 11 or len(combo_removed) != 11:
            raise RuntimeError("expected exactly 22 duplicate declarations")
        PATH.write_text(source, "utf-8")
        print("removed duplicate fields: Decision=11 ComboCandidate=11 total=22")
    else:
        print("duplicate V46 declarations already normalized")
    verify_no_duplicate_fields()
    print("PASS Models.java class-field uniqueness")
    normalize_java7_anonymous_capture()
    return 0


if __name__ == "__main__":
    sys.exit(main())
