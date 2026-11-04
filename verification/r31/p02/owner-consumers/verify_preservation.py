#!/usr/bin/env python3
"""Compare original assertion tokens, numeric literals and test methods to the authorized baseline."""
import hashlib
import json
from pathlib import Path
import re
import subprocess

ROOT = Path(__file__).resolve().parents[4]
BASE = "3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056"
NAMES = ["R27QualityTest", "R27ReplayTest", "R30IncrementalFeatureTest", "R30ModelSurfaceTest", "R30FailureRepairTest"]
LEXER = re.compile(
    r"(?P<comment>//[^\n]*|/\*[\s\S]*?\*/)"
    r"|(?P<string>\"(?:\\.|[^\"\\])*\"|'(?:\\.|[^'\\])*')"
    r"|(?P<id>[A-Za-z_$][\w$]*)"
    r"|(?P<number>(?:0[xX][0-9a-fA-F]+(?:\.[0-9a-fA-F]*)?(?:[pP][+-]?\d+)?|(?:\d+\.\d*|\.\d+|\d+)(?:[eE][+-]?\d+)?)[fFdDlL]?)"
    r"|(?P<other>\S)"
)


def tokens(text):
    return [(m.lastgroup, m.group()) for m in LEXER.finditer(text) if m.lastgroup != "comment"]


def assertions(items):
    calls = []
    for i, (kind, token) in enumerate(items[:-1]):
        if kind != "id" or not (token.startswith("assert") or token == "fail") or items[i + 1][1] != "(":
            continue
        depth = 0
        for j in range(i + 1, len(items)):
            if items[j][1] == "(":
                depth += 1
            elif items[j][1] == ")":
                depth -= 1
                if not depth:
                    calls.append(items[i:j + 1])
                    break
    return calls


def main():
    proof = []
    for name in NAMES:
        relative = "app/src/test/java/com/ethk3/novaonepro/strategyv2/" + name + ".java"
        original = subprocess.check_output(["git", "show", BASE + ":" + relative], cwd=ROOT).decode()
        modified = (ROOT / relative).read_text()
        old, new = tokens(original), tokens(modified)
        before, after = assertions(old), assertions(new)
        numbers_before = [token for kind, token in old if kind == "number"]
        numbers_after = [token for kind, token in new if kind == "number"]
        methods_before = re.findall(r"@Test\s+public\s+void\s+(\w+)", original)
        methods_after = re.findall(r"@Test\s+public\s+void\s+(\w+)", modified)
        proof.append(dict(path=relative, assertionCalls=len(before), assertionTokenSequenceEqual=before == after,
                          numericLiteralCount=len(numbers_before), numericLiteralSequenceEqual=numbers_before == numbers_after,
                          testMethods=methods_before, testMethodSequenceEqual=methods_before == methods_after,
                          originalSha256=hashlib.sha256(original.encode()).hexdigest(),
                          modifiedSha256=hashlib.sha256(modified.encode()).hexdigest()))
    passed = all(x["assertionTokenSequenceEqual"] and x["numericLiteralSequenceEqual"] and x["testMethodSequenceEqual"] for x in proof)
    print(json.dumps(dict(status="PASS_SOURCE_TOKEN_COMPARISON_ONLY" if passed else "FAILED", baseline=BASE, files=proof,
                          scope="ORIGINAL_ASSERTION_CALL_TOKENS_NUMERIC_LITERALS_AND_TEST_METHODS_UNCHANGED_NOT_RUNTIME_ACCEPTANCE"), indent=2))
    return 0 if passed else 1


if __name__ == "__main__":
    raise SystemExit(main())
