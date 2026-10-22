"""Lift the app's real atom registry (cp4_atom_registry.jsonl) into GRAMMAR material.

The registry stores 25,350 atoms, but almost all are one formula repeated over
position x digit x window. A per-(position, digit) atom family with fixed parameters
is therefore lifted into ONE score tensor S[t, p, n] (all positions, all ten numbers),
and per-position scalar families become CONDITION atoms. Formulas follow
r952/AtomEvaluatorCP4.calculate; every value at target t uses rows < t only.

Digit convention: digit k <-> number k+1, as production feeds AtomWindowCP4 with
getNumberAtPosition(p)-1 (FrozenCandidateAdapterR958:439, DevelopmentProductionCP12:277).
Formulas follow AtomWindowCP4 exactly: window statistics need size >= count+lag, omission
is measured from the last appended row (end = t-1), GAP_LAST_GAP is the interval between
the last two occurrences inside the window, MUTUAL_INFORMATION is MI between the lagged
digit (10 categories) and the current "is digit k" indicator, DRIFT_JS uses (c+0.5)/(n+5).
Missing values (production Result.present=false) are filled with the row mean of the
present digits before z-normalisation; an all-missing row becomes all zeros.
"""
from __future__ import annotations

import collections
import json
import math
import warnings

import numpy as np

SCORE_FAMILIES = ("FREQUENCY", "ROLLING_VARIANCE_INDICATOR", "FREQUENCY_SLOPE", "OMISSION_RATE",
                  "OMISSION", "HOT_COLD_Z", "MARKOV_SCORE", "PERIODICITY", "MUTUAL_INFORMATION",
                  "LAG_DIGIT_EQ", "GAP_LAST_GAP")
COND_FAMILIES = ("ENTROPY", "DRIFT_JS")


def _pkey(params):
    return tuple(sorted((k, v) for k, v in params.items() if k != "digit"))


def load_registry(path):
    """Return (score_specs, cond_specs, coverage) built from the real registry."""
    score, cond = set(), set()
    fam_count = collections.Counter()
    lifted = collections.Counter()
    with open(path, encoding="utf-8") as f:
        for line in f:
            a = json.loads(line)
            s = a["spec"]
            fam_count[s["family"]] += 1
            if s.get("context") != "PIT_DRAW":
                continue
            if s["family"] in SCORE_FAMILIES:
                p = dict(s["params"])
                if s["family"] == "MARKOV_SCORE" and p.get("order", 1) != 1:
                    continue
                score.add((s["family"], _pkey(p)))
                lifted[s["family"]] += 1
            elif s["family"] in COND_FAMILIES:
                cond.add((s["family"], s["position"] - 1, _pkey(s["params"])))
                lifted[s["family"]] += 1
    cov = {"registry_atoms": sum(fam_count.values()), "registry_families": len(fam_count),
           "lifted_atoms": sum(lifted.values()), "lifted_families": sorted(lifted),
           "not_lifted_families": sorted(set(fam_count) - set(lifted)),
           "score_tensors": len(score), "condition_series": len(cond)}
    return sorted(score), sorted(cond), cov


class AtomLibrary:
    """Causal tensors for lifted atoms; cached per (family, params)."""

    def __init__(self, D, bank):
        self.D = D.astype(np.int64)
        self.T = len(D)
        self.X = bank.X.astype(np.float64)            # one-hot [T,10,10]
        self.C = np.zeros((self.T + 1, 10, 10))
        np.cumsum(self.X, axis=0, out=self.C[1:])
        u = np.arange(self.T, dtype=np.float64)[:, None, None]
        self.CW = np.zeros_like(self.C)
        np.cumsum(self.X * u, axis=0, out=self.CW[1:])
        self._omit = None
        self.cache = {}

    # -- helpers
    def _win(self, w):
        t = np.arange(self.T)
        lo = np.maximum(t - w, 0)
        return t, lo, (t - lo).astype(np.float64)

    def _freq(self, w):
        t, lo, n = self._win(w)
        f = (self.C[t] - self.C[lo]) / np.maximum(n, 1)[:, None, None]
        f[n < w] = np.nan
        return f

    def _omission(self):
        if self._omit is None:
            g = np.full((self.T, 10, 10), np.nan)
            last = np.full((10, 10), -1, dtype=np.int64)
            for t in range(self.T):
                g[t] = np.where(last >= 0, t - last, np.nan)
                last[np.arange(10), self.D[t] - 1] = t
            self._omit = g
        return self._omit

    def _lagged_pair_sums(self, w, lag):
        """Window sums over u in [t-w, t): a=sum x_{u-lag}, b=sum x_u, ab=sum x_u*x_{u-lag}."""
        X = self.X
        Xl = np.zeros_like(X)
        Xl[lag:] = X[:-lag]
        P = X * Xl
        ca = np.vstack([np.zeros((1, 10, 10)), np.cumsum(Xl, axis=0)])
        cp = np.vstack([np.zeros((1, 10, 10)), np.cumsum(P, axis=0)])
        t, lo, n = self._win(w)
        a = ca[t] - ca[lo]
        b = self.C[t] - self.C[lo]
        ab = cp[t] - cp[lo]
        n = np.where(t >= w + lag, n, 0)                         # production: size >= count+lag
        return a, b, ab, n

    # -- score atoms
    def score(self, family, pkey):
        k = (family, pkey)
        if k in self.cache:
            return self.cache[k]
        p = dict(pkey)
        if family == "FREQUENCY":
            v = self._freq(p["window"])
        elif family == "ROLLING_VARIANCE_INDICATOR":
            f = self._freq(p["window"])
            v = f * (1 - f)
        elif family == "FREQUENCY_SLOPE":
            w = p["window"]
            t, lo, n = self._win(w)
            bins = self.C[t] - self.C[lo]
            weighted = (self.CW[t] - self.CW[lo]) - lo[:, None, None] * bins
            v = (weighted - (w - 1) * bins / 2) / (w * (w * w - 1.0) / 12)
            v[n < w] = np.nan
        elif family == "OMISSION_RATE":
            w = p["window"]
            v = np.minimum(self._omission() - 1, w) / w          # production: end - lastSeen, end = t-1
            v[np.arange(self.T) < w] = np.nan                    # needs size >= w
        elif family == "OMISSION":
            v = self._omission() - 1
        elif family == "GAP_LAST_GAP":
            v = self._last_gap(p["window"])
        elif family == "HOT_COLD_Z":
            r, R = p["recent"], p["reference"]
            a, z = self._freq(r), self._freq(R)
            var = z * (1 - z) * (1.0 / r - 1.0 / R)
            v = np.where(var > 1e-12, (a - z) / np.sqrt(np.maximum(var, 1e-12)), np.nan)
        elif family == "MARKOV_SCORE":
            v = self._markov(p["window"], p.get("alpha", 0.5))
        elif family == "PERIODICITY":
            a, b, ab, n = self._lagged_pair_sums(p["window"], p["period"])
            nn = np.maximum(n, 1)[:, None, None]
            den = np.sqrt(np.maximum((a - a * a / nn) * (b - b * b / nn), 0))
            v = np.where(den > 1e-12, (ab - a * b / nn) / np.maximum(den, 1e-12), np.nan)
            v[n == 0] = np.nan
        elif family == "MUTUAL_INFORMATION":
            v = self._indicator_mi(p["window"], p["lag"])
        elif family == "LAG_DIGIT_EQ":
            l = p["lag"]
            v = np.zeros_like(self.X)
            v[l:] = self.X[:-l]
        else:
            raise ValueError("NOT_LIFTED " + family)
        # Missing digits -> row mean of present digits; all-missing row -> zeros.
        with np.errstate(all="ignore"), warnings.catch_warnings():
            warnings.simplefilter("ignore", RuntimeWarning)               # all-missing rows are expected early
            rowmean = np.nanmean(np.where(np.isfinite(v), v, np.nan), axis=2, keepdims=True)
        v = np.where(np.isfinite(v), v, rowmean)
        v = np.nan_to_num(v, nan=0.0)
        mu = v.mean(axis=2, keepdims=True)
        sd = v.std(axis=2, keepdims=True) + 1e-6
        out = ((v - mu) / sd).astype(np.float32)
        if len(self.cache) > 48:
            self.cache.clear()
        self.cache[k] = out
        return out

    def _last_gap(self, w):
        """Interval between the last two occurrences, both inside rows [t-w, t); else missing."""
        out = np.full((self.T, 10, 10), np.nan)
        last = np.full((10, 10), -10 ** 9, dtype=np.int64)
        prev = np.full((10, 10), -10 ** 9, dtype=np.int64)
        P = np.arange(10)
        for t in range(self.T):
            if t >= w:
                ok = prev >= t - w
                out[t] = np.where(ok, last - prev, np.nan)
            d = self.D[t] - 1
            prev[P, d] = last[P, d]
            last[P, d] = t
        return out

    def _indicator_mi(self, w, lag):
        """MI(lagged digit a in 0..9 ; current == k) over the window, natural log, >= 0."""
        key = ("JOINT", w, lag)
        if not hasattr(self, "_joint"):
            self._joint = {}
        if key not in self._joint:
            if len(self._joint) >= 3:                                # ~80 MB each at T=10k: keep few
                self._joint.clear()
            T = self.T
            pair = np.zeros((T, 10, 100), np.float32)
            Dm = self.D - 1
            tt = np.arange(lag, T)
            for p in range(10):
                pair[tt, p, Dm[tt - lag, p] * 10 + Dm[tt, p]] = 1.0
            cj = np.zeros((T + 1, 10, 100), np.float64)
            np.cumsum(pair, axis=0, out=cj[1:])
            t = np.arange(T)
            lo = np.maximum(t - w, 0)
            J = (cj[t] - cj[lo]).astype(np.float32).reshape(T, 10, 10, 10)   # [t, p, a, b]
            self._joint[key] = J
        J = self._joint[key].astype(np.float64)
        count = float(w)
        bins = J.sum(axis=2)                                         # [t,p,b] current digit counts
        na = J.sum(axis=3)                                           # [t,p,a] lagged digit counts
        out = np.zeros((self.T, 10, 10))
        for k in range(10):
            yes = bins[:, :, k][:, :, None]
            no = count - yes
            c = J[:, :, :, k]
            with np.errstate(all="ignore"):
                t1 = np.where(c > 0, c / count * np.log(c * count / (na * yes)), 0.0)
                t2 = np.where(na - c > 0, (na - c) / count * np.log((na - c) * count / (na * no)), 0.0)
            out[:, :, k] = np.maximum(0.0, np.nan_to_num(t1 + t2).sum(axis=2))
        out[np.arange(self.T) < w + lag] = np.nan
        return out

    def _markov(self, w, alpha):
        D = self.D - 1
        T = self.T
        M = np.zeros((10, 10, 10))
        out = np.full((T, 10, 10), np.nan)
        p = np.arange(10)
        for t in range(T):
            if t >= 2:
                M[p, D[t - 2], D[t - 1]] += 1
                if t - 2 - w >= 0:
                    M[p, D[t - 2 - w], D[t - 1 - w]] -= 1
            if t >= w + 1:
                rows = M[p, D[t - 1]]
                out[t] = (rows + alpha) / (rows.sum(axis=1, keepdims=True) + 10 * alpha)
        return out

    # -- condition atoms (per-position scalar -> above its own causal median)
    def cond(self, family, q, pkey):
        k = ("C", family, q, pkey)
        if k in self.cache:
            return self.cache[k]
        p = dict(pkey)
        if family == "ENTROPY":
            f = self._freq(p["window"])[:, q, :]
            s = -(np.where(f > 0, f * np.log(np.maximum(f, 1e-12)), 0)).sum(axis=1)
        else:  # DRIFT_JS between recent and older window
            r, R = p["recent"], p["reference"]
            cr = np.nan_to_num(self._freq(r)[:, q, :]) * r                     # recent counts
            co = np.nan_to_num(self._freq(r + R)[:, q, :]) * (r + R) - cr       # older counts
            fr = (cr + 0.5) / (cr.sum(axis=1, keepdims=True) + 5.0)
            fo = (co + 0.5) / (co.sum(axis=1, keepdims=True) + 5.0)
            ready = np.arange(self.T) >= r + R                                 # production: window not ready -> Missing
            fr[~ready] = 0.1
            fo[~ready] = 0.1
            m = (fr + fo) / 2
            s = 0.5 * (fr * np.log(fr / m)).sum(axis=1) + 0.5 * (fo * np.log(fo / m)).sum(axis=1)
            s[~ready] = np.nan
        # SU1_ATOM_CONDITION_V2: exact mathematical ties must not become branches
        # because libm/vector reduction changes the last bit across Python/ART.
        # The comparison domain is fixed at 1e-12 before both median and test.
        s = np.round(np.nan_to_num(s), 12)
        out = np.zeros(self.T, bool)
        for b in range(300, self.T, 50):             # block-causal median of the past 300 values
            med = np.median(s[b - 300:b])
            out[b:b + 50] = s[b:b + 50] > med
        out = out[:, None, None]
        self.cache[k] = out
        return out


ATOM_NAMES = {"FREQUENCY": "频率", "ROLLING_VARIANCE_INDICATOR": "出现方差", "FREQUENCY_SLOPE": "频率斜率",
              "OMISSION_RATE": "遗漏率", "OMISSION": "遗漏", "HOT_COLD_Z": "冷热Z", "MARKOV_SCORE": "马尔可夫",
              "PERIODICITY": "周期相关", "MUTUAL_INFORMATION": "互信息", "LAG_DIGIT_EQ": "滞后重现",
              "GAP_LAST_GAP": "最近间隔", "ENTROPY": "熵", "DRIFT_JS": "分布漂移"}


def atom_text(family, pkey):
    p = ",".join("%s=%s" % (k, v) for k, v in pkey)
    return "原子·%s(%s)" % (ATOM_NAMES.get(family, family), p)


def cond_text(family, q, pkey):
    p = ",".join("%s=%s" % (k, v) for k, v in pkey)
    return "位置%d的%s(%s)高于近300期中位数" % (q + 1, ATOM_NAMES.get(family, family), p)
