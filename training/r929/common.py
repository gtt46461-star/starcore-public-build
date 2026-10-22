"""Compatibility facade. Dataset validation and SEALED ownership have one production implementation."""
from .pipeline import (SCHEMA,GAME,FEATURE_STAGE,FEATURE_COUNT,GROUP_SIZE,POSITIONS,K,ACTIONS,
    digest,file_digest,canonical,write_json,read_json,read_integrity_row,validate_row,
    chronological_splits,load_dataset,registry_root,experiment_identity,register_trial,
    consume_sealed_once,decode_raw_upstream,stage_dataset as _stage_dataset)
DEFAULT_EMBARGO_ISSUES=1

def stage_dataset(source,out,embargo=DEFAULT_EMBARGO_ISSUES):
    if embargo!=DEFAULT_EMBARGO_ISSUES:raise ValueError("EMBARGO_CHANGE_REQUIRES_NEW_FROZEN_PROTOCOL")
    return _stage_dataset(source,out)

