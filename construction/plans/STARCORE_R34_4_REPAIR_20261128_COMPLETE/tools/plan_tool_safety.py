"""Small shared guards for plan tools; no application or network operations."""
from __future__ import annotations

import os
from pathlib import Path, PurePosixPath
import tempfile


def require(condition, message):
    if not condition:
        raise ValueError(message)


def safe_relative(root, value):
    require(isinstance(value, str) and value and "\\" not in value and "\x00" not in value,
            "invalid relative path")
    relative = PurePosixPath(value)
    require(not relative.is_absolute() and ".." not in relative.parts and value != ".",
            "unsafe relative path: " + value)
    root = Path(root).resolve()
    path = root.joinpath(*relative.parts)
    require(path.resolve().is_relative_to(root) and not path.is_symlink(),
            "symlink/escaping path: " + value)
    return path


def fresh_output(path, protected_roots):
    """Check before expensive work; refuse existing files and protected trees."""
    requested = Path(path).absolute()
    require(not requested.is_symlink(), "output must not be a symlink")
    output = requested.resolve()
    for root in protected_roots:
        if root is not None:
            require(not output.is_relative_to(Path(root).resolve()),
                    "output must be outside the frozen plan and application source: " + str(output))
    require(not output.exists(), "output already exists; preserve it and choose a new path: " + str(output))
    return output


def write_fresh_text(output, text, protected_roots):
    """Atomic creation without replacing an existing or concurrently created file."""
    output = fresh_output(output, protected_roots)
    output.parent.mkdir(parents=True, exist_ok=True)
    temporary = None
    try:
        with tempfile.NamedTemporaryFile(prefix=".plan-result-", dir=output.parent,
                                         delete=False, mode="w", encoding="utf-8") as stream:
            temporary = Path(stream.name)
            stream.write(text)
            stream.flush()
            os.fsync(stream.fileno())
        os.link(temporary, output)
    finally:
        if temporary is not None:
            temporary.unlink(missing_ok=True)
