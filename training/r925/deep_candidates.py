#!/usr/bin/env python3
"""Deterministic research candidate models for the R952 chronological pipeline."""
from __future__ import annotations
from dataclasses import dataclass
import hashlib, json, math

@dataclass(frozen=True)
class LinearRanker:
    weights: tuple[float, ...]
    bias: float = 0.0
    def score(self, row: list[float]) -> float:
        if len(row) != len(self.weights): raise ValueError("R952_RANKER_DIMENSION")
        value = self.bias + sum(w*x for w,x in zip(self.weights,row))
        if not math.isfinite(value): raise ValueError("R952_RANKER_NONFINITE")
        return value
    def artifact(self) -> dict:
        core={"schema":"R952_LINEAR_RANKER_V1","weights":self.weights,"bias":self.bias}
        core["artifact_hash"]=hashlib.sha256(json.dumps(core,sort_keys=True,separators=(",",":")).encode()).hexdigest()
        return core

def rank_group(rows: list[list[float]], model: LinearRanker) -> list[int]:
    if len(rows) != 1200: raise ValueError("R952_ACTION_UNIVERSE_NOT_1200")
    return sorted(range(1200), key=lambda i: (-model.score(rows[i]), i))

def fit_pairwise(groups: list[tuple[list[list[float]], int]], epochs: int=5, lr: float=.01) -> LinearRanker:
    if not groups or not groups[0][0]: raise ValueError("R952_EMPTY_TRAINING")
    dim=len(groups[0][0][0]); w=[0.0]*dim
    for _ in range(max(1,epochs)):
        for rows,positive in groups:
            if len(rows)!=1200 or positive<0 or positive>=1200: raise ValueError("R952_GROUP_CONTRACT")
            negative=max((i for i in range(1200) if i!=positive),key=lambda i:sum(w[j]*rows[i][j] for j in range(dim)))
            margin=sum(w[j]*(rows[positive][j]-rows[negative][j]) for j in range(dim))
            if margin<1.0:
                for j in range(dim):w[j]+=lr*(rows[positive][j]-rows[negative][j])
    return LinearRanker(tuple(w))
