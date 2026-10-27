"""R25 host-reference kernels and contracts; NOT an Android release or win-rate proof.
Uses only Python standard library. Numerical head is exact for UNARY additive
permutation energy, not an arbitrary pairwise/hypergraph energy model.
"""
from __future__ import annotations
from dataclasses import dataclass
import hashlib, json, math, struct
from collections.abc import Mapping
from typing import Any, Sequence


def typed(value: Any) -> Any:
    """Unambiguous, lossless typed canonical form; deliberately rejects non-finite floats."""
    if value is None: return ["null"]
    if type(value) is bool: return ["bool", value]
    if type(value) is int: return ["int", str(value)]
    if type(value) is float:
        if not math.isfinite(value): raise ValueError("NON_FINITE_IDENTITY")
        return ["f64", struct.pack(">d", value).hex()]
    if type(value) is str: return ["str", value]
    if isinstance(value, Mapping):
        if any(type(k) is not str for k in value): raise ValueError("STRING_KEYS_REQUIRED")
        return ["map", [[k, typed(value[k])] for k in sorted(value)]]
    if isinstance(value, (tuple, list)): return ["seq", [typed(v) for v in value]]
    raise TypeError(type(value).__name__)


def canonical(value: Any) -> str:
    return json.dumps(typed(value), ensure_ascii=False, separators=(",", ":"), allow_nan=False)


def digest(value: Any) -> str:
    return hashlib.sha256(canonical(value).encode("utf-8")).hexdigest()


@dataclass(frozen=True)
class Policy:
    """Snapshot the full executable policy specification, never retain a mutable input map."""
    canonical_spec: str
    policy_hash: str

    @staticmethod
    def create(spec: Mapping) -> "Policy":
        required = {"graph", "updater", "feature_contract", "calibration_rule", "gate_rule", "protocol"}
        if not required <= spec.keys(): raise ValueError("INCOMPLETE_POLICY")
        text = canonical({"domain": "STARCORE_POLICY_R25_V1", "spec": spec})
        return Policy(text, hashlib.sha256(text.encode()).hexdigest())


def checkpoint_hash(policy: Policy, state: Any, trained_through: int,
                    optimizer: Any, rng: Any, parent_checkpoint: str) -> str:
    if trained_through < 0: raise ValueError("INVALID_CURSOR")
    return digest({"domain":"CHECKPOINT_R25_V1", "policy":policy.policy_hash,
                   "state":state, "optimizer":optimizer, "rng":rng,
                   "trained_through":trained_through, "parent":parent_checkpoint})


@dataclass(frozen=True)
class FrozenReceipt:
    # sequence numbers must come from TargetIssueResolver, not naive issue-id arithmetic.
    policy_hash: str
    checkpoint_hash: str
    target: int
    trained_through: int
    input_through: int
    freeze_elapsed_ns: int
    deadline_elapsed_ns: int
    status: str
    position: int | None = None
    numbers: tuple[int, ...] = ()
    evidence_scope: str = "PROSPECTIVE_FROZEN"

    def validate(self) -> None:
        if self.evidence_scope != "PROSPECTIVE_FROZEN": raise ValueError("NOT_PROSPECTIVE")
        if not self.policy_hash or not self.checkpoint_hash: raise ValueError("UNBOUND_ARTIFACT")
        if min(self.target, self.trained_through, self.input_through, self.freeze_elapsed_ns) < 0: raise ValueError("NEGATIVE_CURSOR")
        if max(self.trained_through, self.input_through) >= self.target: raise ValueError("FUTURE_INPUT")
        if self.freeze_elapsed_ns >= self.deadline_elapsed_ns: raise ValueError("LATE_FREEZE")
        if self.status not in {"ACT", "ABSTAIN", "MISSING", "INVALID"}: raise ValueError("STATUS")
        if self.status == "ACT":
            if self.position not in range(1,11) or len(self.numbers)!=3 or len(set(self.numbers))!=3 or any(type(n) is not int or n not in range(1,11) for n in self.numbers):
                raise ValueError("SINGLE_POSITION_K3_REQUIRED")
        elif self.position is not None or self.numbers: raise ValueError("NON_ACTION_HAS_ACTION")


def alpha_allocation(root: float, candidate: int, look: int, endpoint_weight: float) -> float:
    """Union-budget conservative reference. Allocate candidate/endpoint BEFORE outcomes.
    Sum of endpoint_weight for all endpoints in a candidate must be <=1.
    Candidate IDs and look IDs must never be recycled after restart/retirement.
    """
    if not (0 < root < 1 and 0 < endpoint_weight <= 1 and candidate>=1 and look>=1): raise ValueError("ALPHA_DOMAIN")
    return root*endpoint_weight/(candidate*(candidate+1)*look*(look+1))


def paired_lower(deltas: Sequence[float], alpha: float) -> float:
    """Hoeffding-Azuma lower bound for mean predictable expected delta in [-1,1].
    Requires valid pre-outcome protocol and its stated bounded martingale model.
    This is NOT a conditional future-performance guarantee under arbitrary drift.
    """
    if not deltas or not 0 < alpha < 1: raise ValueError("PAIRED_DOMAIN")
    if any(not math.isfinite(x) or x < -1 or x > 1 for x in deltas): raise ValueError("DELTA_RANGE")
    return sum(deltas)/len(deltas)-math.sqrt(2*math.log(1/alpha)/len(deltas))


def noninferior(deltas: Sequence[float], alpha: float, margin: float) -> bool:
    if not 0 <= margin < 1: raise ValueError("MARGIN")
    return paired_lower(deltas, alpha) >= -margin


def excess_hit(act: bool, hit: bool, null: float=.3) -> float:
    if hit and not act: raise ValueError("ABSTENTION_CANNOT_HIT")
    return (float(hit)-null) if act else 0.0


def bernoulli_log_e(hits: Sequence[int], alternatives: Sequence[float], null: float=.3) -> float:
    """Likelihood-ratio test martingale for conditional success <=null.
    alternatives must be predictable and >=null, never chosen using current/future hits.
    The caller is responsible for prospective, predictable choice and alpha ownership.
    """
    if len(hits)!=len(alternatives) or not 0<null<1: raise ValueError("EVALUE_DOMAIN")
    z=0.0
    for hit,q in zip(hits, alternatives):
        if hit not in (0,1) or not null<=q<1: raise ValueError("EVALUE_INPUT")
        z += math.log(q/null) if hit else math.log((1-q)/(1-null))
    return z


def probability_of_losing_run(n: int, hit_probability: float, run: int=7) -> float:
    """Exact finite-n DP under IID Bernoulli illustration, NOT a model of real draws."""
    if n<0 or not 0<=hit_probability<=1 or run<1: raise ValueError("RUN_DOMAIN")
    s=[1.0]+[0.0]*(run-1)
    for _ in range(n): s=[sum(s)*hit_probability]+[v*(1-hit_probability) for v in s[:-1]]
    return max(0.,min(1.,1-sum(s)))


def _logadd(a: float,b: float) -> float:
    if a == -math.inf: return b
    if b == -math.inf: return a
    m=max(a,b)
    return m+math.log1p(math.exp(min(a,b)-m))


def exact_unary_matching(scores: Sequence[Sequence[float]]) -> tuple[float,list[list[float]]]:
    """P(pi)=exp(sum scores[p][pi[p]])/Z; O(n*2**n), max n=10.
    Returns logZ and EXACT (up to float precision) row/column marginals.
    Does not enforce or fabricate predictive advantage.
    """
    n=len(scores)
    if not 1<=n<=10 or any(len(r)!=n for r in scores) or any(not math.isfinite(x) for r in scores for x in r): raise ValueError("SCORE_MATRIX")
    total=1<<n;full=total-1
    f=[-math.inf]*total;f[0]=0.
    for mask in range(full):
        p=mask.bit_count()
        for j in range(n):
            bit=1<<j
            if not mask&bit: f[mask|bit]=_logadd(f[mask|bit],f[mask]+scores[p][j])
    b=[-math.inf]*total;b[full]=0.
    for mask in range(full-1,-1,-1):
        p=mask.bit_count()
        for j in range(n):
            bit=1<<j
            if not mask&bit: b[mask]=_logadd(b[mask],scores[p][j]+b[mask|bit])
    lm=[[-math.inf]*n for _ in range(n)]
    for mask in range(full):
        p=mask.bit_count()
        for j in range(n):
            bit=1<<j
            if not mask&bit: lm[p][j]=_logadd(lm[p][j],f[mask]+scores[p][j]+b[mask|bit]-f[full])
    return f[full],[[math.exp(x) for x in row] for row in lm]


def matching_nll_gradient(scores: Sequence[Sequence[float]], draw: Sequence[int]) -> tuple[float,list[list[float]]]:
    """draw is zero-based permutation, unlike user-facing 1..10 numbers."""
    n=len(scores)
    if sorted(draw)!=list(range(n)): raise ValueError("DRAW_PERMUTATION")
    z,p=exact_unary_matching(scores)
    return z-sum(scores[i][draw[i]] for i in range(n)),[[p[i][j]-float(j==draw[i]) for j in range(n)] for i in range(n)]


def k3_actions(prob: Sequence[Sequence[float]]) -> list[tuple[int,tuple[int,int,int],float]]:
    from itertools import combinations
    if len(prob)!=10 or any(len(row)!=10 for row in prob): raise ValueError("MARGINAL_SHAPE")
    if any(not math.isfinite(x) or not 0<=x<=1 for row in prob for x in row): raise ValueError("MARGINAL_RANGE")
    if any(abs(sum(row)-1)>1e-6 for row in prob): raise ValueError("MARGINAL_ROW_SUM")
    if any(abs(sum(prob[p][j] for p in range(10))-1)>1e-6 for j in range(10)): raise ValueError("MARGINAL_COL_SUM")
    return [(p+1,tuple(j+1 for j in group),sum(prob[p][j] for j in group)) for p in range(10) for group in combinations(range(10),3)]
