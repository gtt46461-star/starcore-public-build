#!/usr/bin/env python3
"""Output/platform-path adapter for unchanged HOST assertions. Never rebind expected hashes.

Some historical runners hard-code verification output directories. Only their output
path expressions and known JDK/SDK/JDBC locations are relocated for the gate; fixture
read paths, business sources, assertions, heaps and numerical constants are retained.
"""
import argparse
import ast
import copy
import hashlib
import json
import os
from pathlib import Path
import sys
import types


class OutputPaths(ast.NodeTransformer):
    def __init__(self, output, temporary):
        self.output, self.temporary, self.changes = output, temporary, []
        self.in_output = False
        home = os.environ.get("JAVA_HOME", "")
        sdk = os.environ.get("R312_ANDROID_JAR") or os.environ.get("R31_HOST_ANDROID_JAR")
        jdbc = os.environ.get("R312_SQLITE_JDBC") or os.environ.get("SQLITE_JDBC")
        self.platform = {}
        if home:
            self.platform["/workspace/starcore-environment/tools/jdk-17.0.17+10"] = home
            self.platform["/workspace/starcore-environment/tools/jdk-17.0.17+10/bin"] = str(Path(home) / "bin")
        if sdk:
            self.platform["/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar"] = sdk
        if jdbc:
            self.platform["/tmp/r31-baseline-deps/sqlite-jdbc.jar"] = jdbc
        if os.environ.get("GRADLE_USER_HOME"):
            self.platform["/workspace/starcore-environment/gradle-home"] = os.environ["GRADLE_USER_HOME"]

    def visit_Constant(self, node):
        value = node.value
        new = self.platform.get(value) if isinstance(value, str) else None
        if self.in_output and isinstance(value, str) and value.startswith("verification/r31/"):
            new = str(self.output / "legacy-output" / value.removeprefix("verification/r31/"))
        if new is not None and new != value:
            self.changes.append({"line": node.lineno, "old": value, "new": new,
                                 "kind": "output_path" if self.in_output else "platform_path"})
            return ast.copy_location(ast.Constant(new), node)
        return node

    def visit_Assign(self, node):
        prior = self.in_output
        self.in_output = any(isinstance(target, ast.Name) and target.id in {"OUT", "OUTPUT", "DEST", "OUTDIR", "out", "output"} for target in node.targets)
        node.value = self.visit(node.value)
        self.in_output = prior
        return node

    def visit_Call(self, node):
        if isinstance(node.func, ast.Attribute) and node.func.attr in {"write_text", "write_bytes", "mkdir"}:
            is_output = any((isinstance(value, ast.Constant) and isinstance(value.value, str) and value.value.startswith("verification/r31/"))
                            or (isinstance(value, ast.Name) and value.id in {"OUT", "OUTPUT", "DEST", "OUTDIR", "out", "output"})
                            for value in ast.walk(node.func.value))
            prior = self.in_output
            self.in_output = is_output
            node.func.value = self.visit(node.func.value)
            self.in_output = prior
            if is_output and node.func.attr in {"write_text", "write_bytes"}:
                node.func.value = ast.copy_location(ast.Call(func=ast.Name(id="__r312_output_target__", ctx=ast.Load()), args=[node.func.value], keywords=[]), node.func.value)
        if isinstance(node.func, ast.Attribute) and node.func.attr in {"mkdtemp", "TemporaryDirectory", "mkstemp"}:
            for keyword in node.keywords:
                if keyword.arg == "dir" and isinstance(keyword.value, ast.Constant) and keyword.value.value == "/workspace":
                    self.changes.append({"line": keyword.value.lineno, "old": "/workspace", "new": str(self.temporary), "kind": "temporary_directory"})
                    keyword.value = ast.copy_location(ast.Constant(str(self.temporary)), keyword.value)
        node.args = [self.visit(value) for value in node.args]
        node.keywords = [self.visit(value) for value in node.keywords]
        return node


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--script", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("arguments", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    script = args.script.resolve()
    original = script.read_bytes()
    source = ast.parse(original, filename=str(script))
    before_assertions = [ast.dump(node, include_attributes=False) for node in ast.walk(source) if isinstance(node, ast.Assert)]
    before_numbers = [repr(node.value) for node in ast.walk(source) if isinstance(node, ast.Constant) and isinstance(node.value, (int, float))]
    temporary = Path(os.environ["TMPDIR"])
    mapper = OutputPaths(args.output, temporary)
    adapted = ast.fix_missing_locations(mapper.visit(copy.deepcopy(source)))
    after_assertions = [ast.dump(node, include_attributes=False) for node in ast.walk(adapted) if isinstance(node, ast.Assert)]
    after_numbers = [repr(node.value) for node in ast.walk(adapted) if isinstance(node, ast.Constant) and isinstance(node.value, (int, float))]
    if before_assertions != after_assertions or before_numbers != after_numbers:
        raise ValueError("OUTPUT_ADAPTER_CHANGED_ASSERTION_OR_NUMERICAL_CONSTANT")
    meta = args.output.parent / (args.output.name + "-io-adapter.json")
    meta.write_text(json.dumps({"scope": "OUTPUT_AND_PLATFORM_PATH_ADAPTER_NOT_BUSINESS_OR_ASSERTION_CHANGE",
                               "originalScript": str(script), "originalScriptSha256": hashlib.sha256(original).hexdigest(),
                               "adaptedAstSha256": hashlib.sha256(ast.dump(adapted, include_attributes=False).encode()).hexdigest(),
                               "changes": mapper.changes, "assertionsUnchanged": True, "numericConstantsUnchanged": True}, indent=2) + "\n")

    def output_target(path):
        path = Path(path)
        path.parent.mkdir(parents=True, exist_ok=True)
        return path

    arguments = args.arguments[1:] if args.arguments[:1] == ["--"] else args.arguments
    sys.path.insert(0, str(script.parent))
    sys.argv = [str(script), *arguments]
    module = types.ModuleType("__main__")
    module.__dict__.update({"__file__": str(script), "__package__": None,
                            "__r312_output_target__": output_target})
    sys.modules["__main__"] = module
    exec(compile(adapted, str(script), "exec"), module.__dict__)


if __name__ == "__main__":
    main()
