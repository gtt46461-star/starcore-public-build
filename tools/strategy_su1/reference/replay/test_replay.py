"""R11 Historical Replay Engine + Strategy Foundry tests (host reference only)."""
import random
import unittest

import numpy as np

import json
from pathlib import Path

import atoms
import forge
import grammar
import hre

CATALOG = Path(__file__).resolve().parents[1] / "fixtures" / "atom_catalog_lifted.json"


def uniform(T, seed):
    rng = np.random.default_rng(seed)
    return np.array([rng.permutation(10) + 1 for _ in range(T)], dtype=np.int8)


def sticky(T, seed=3, rate=0.25, position=3):
    rng = np.random.default_rng(seed)
    D = uniform(T, seed)
    for t in range(1, T):
        if rng.random() < rate:
            j = int(np.where(D[t] == D[t - 1, position])[0][0])
            D[t, position], D[t, j] = D[t, j], D[t, position]
    return D


class CausalityTests(unittest.TestCase):
    """The defining property: decisions for target t never depend on draws >= t."""

    def test_future_rewrite_does_not_change_past_decisions(self):
        D = uniform(2600, 1)
        cut = 2200
        D2 = D.copy()
        D2[cut:] = uniform(len(D) - cut, 99)
        rng = random.Random(4)
        for fam in forge.FAMILIES:
            s = forge.random_strategy(fam, rng)
            if fam == "XI_SELECTOR":
                s = forge.hre_seed()
            a = forge.evaluate(forge.Library(D), s, 1500, cut)
            b = forge.evaluate(forge.Library(D2), s, 1500, cut)
            with self.subTest(family=fam):
                self.assertTrue(np.array_equal(a.act[:cut], b.act[:cut]))
                self.assertTrue(np.array_equal(a.position[:cut], b.position[:cut]))
                self.assertTrue(np.array_equal(a.numbers[:cut], b.numbers[:cut]))

    def test_expert_bank_rows_use_only_the_past(self):
        D = uniform(800, 2)
        D2 = D.copy()
        D2[500:] = uniform(300, 5)
        a, b = hre.ExpertBank(D), hre.ExpertBank(D2)
        self.assertTrue(np.array_equal(a.Z[:, :501], b.Z[:, :501]))
        self.assertFalse(np.array_equal(a.Z[:, 502:], b.Z[:, 502:]))

    def test_relation_primitive_reads_lagged_rows_only(self):
        D = uniform(300, 3)
        lib = forge.Library(D)
        R = lib.relation("SAME", 1, 0, 1)
        t, p = 120, 4
        n = int(np.argmax(R[t, p]))
        self.assertEqual(n + 1, int(D[t - 1, p]))


class AtomCausalityTests(unittest.TestCase):
    def test_every_lifted_atom_family_is_causal(self):
        cat = json.loads(CATALOG.read_text(encoding="utf-8"))
        self.assertEqual(25350, cat["coverage"]["registry_atoms"])
        D = uniform(1500, 11)
        cut = 1200
        D2 = D.copy()
        D2[cut:] = uniform(len(D) - cut, 12)
        A = atoms.AtomLibrary(D, hre.ExpertBank(D))
        B = atoms.AtomLibrary(D2, hre.ExpertBank(D2))
        seen = set()
        for f, pk in cat["score"]:
            if f in seen:
                continue
            seen.add(f)
            pk = tuple(map(tuple, pk))
            with self.subTest(family=f):
                self.assertTrue(np.allclose(A.score(f, pk)[:cut + 1], B.score(f, pk)[:cut + 1], atol=1e-5))
        for f, q, pk in cat["cond"][:4]:
            pk = tuple(map(tuple, pk))
            self.assertTrue(np.array_equal(A.cond(f, q, pk)[:cut], B.cond(f, q, pk)[:cut]))

    def test_grammar_trees_render_and_respect_depth(self):
        rng = random.Random(9)
        for _ in range(50):
            t = grammar.random_tree(rng, 4)
            for _ in range(5):
                t = grammar.mutate(t, rng)
            self.assertLessEqual(grammar.depth(t), 6)
            self.assertTrue(grammar.text(t))


class LearningTests(unittest.TestCase):
    def test_foundry_finds_a_planted_pattern_and_improves(self):
        D = sticky(6000)
        rep = forge.campaign(D, generations=5, population=16, seed=1)
        sealed = forge.open_sealed(rep)["champion"]
        self.assertGreater(sealed["hit_rate"], 0.36, rep["champion_description"])
        self.assertLess(sealed["binomial_p_vs_30"], 0.01)
        h = rep["history"]
        self.assertGreaterEqual(h[-1]["champion_val_fitness"], h[0]["champion_val_fitness"])

    def test_noise_does_not_survive_sealed(self):
        D = uniform(6000, 8)
        rep = forge.campaign(D, generations=4, population=16, seed=2)
        sealed = forge.open_sealed(rep)["champion"]
        self.assertGreater(sealed["binomial_p_vs_30"], 0.001)
        self.assertLess(sealed["hit_rate"] if sealed["hit_rate"] is not None else 0.3, 0.37)

    def test_population_is_multi_family_and_ledger_keeps_losers(self):
        D = uniform(4000, 9)
        rep = forge.campaign(D, generations=2, population=16, seed=3)
        fams = {x["family"] for x in rep["ledger"]}
        self.assertGreaterEqual(len(fams), 3)
        self.assertEqual(rep["evaluated_programs"], len(rep["ledger"]))
        self.assertTrue(any(x["dev_fitness"] < 0.2 for x in rep["ledger"]))


if __name__ == "__main__":
    unittest.main(verbosity=2)
