#!/usr/bin/env python3
"""Compatibility command for explicit-manifest DEV identity generation (R967)."""
import argparse
from r967_semantic_identity import build, write_outputs, verify_outputs, self_test

def compute(overrides=None, manifest=None):
    return build(overrides=overrides, release_override=manifest)["training_rule_sha256"]

if __name__ == "__main__":
    p=argparse.ArgumentParser();p.add_argument("--check",action="store_true");p.add_argument("--self-test",action="store_true");a=p.parse_args()
    if a.self_test:self_test()
    elif a.check:verify_outputs()
    else:write_outputs()
