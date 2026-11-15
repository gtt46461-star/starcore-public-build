"""Run: python3 -m unittest discover -s reference/python -p 'test_relational_state_model.py'."""
import hashlib
import json
import unittest
import numpy as np
from relational_state_model import AdamOptimizer, Draw, RelationalStateModel, causal_window, synthetic_examples


class RelationalStateModelTest(unittest.TestCase):
    def setUp(self):
        self.model = RelationalStateModel(width=4, seed=14)
        self.history, self.target = synthetic_examples(1, 93)[0]

    def test_all_parameter_gradients_by_finite_difference(self):
        # All entries, including E, U, relation R and recurrent Wh, are checked.
        _, gradients = self.model.loss_and_grads(self.history, self.target)
        epsilon = 1e-6
        for key, value in self.model.params.items():
            self.assertGreater(np.linalg.norm(gradients[key]), 1e-10, key)
            for index in np.ndindex(value.shape):
                original = value[index]
                value[index] = original + epsilon
                plus = self.model.loss_and_grads(self.history, self.target)[0]
                value[index] = original - epsilon
                minus = self.model.loss_and_grads(self.history, self.target)[0]
                value[index] = original
                numeric = (plus - minus) / (2 * epsilon)
                self.assertAlmostEqual(numeric, gradients[key][index], delta=2e-7,
                                       msg=f"{key}{index}")

    def test_training_updates_every_parameter_group_and_decreases_loss(self):
        before = {k: v.copy() for k, v in self.model.params.items()}
        old = self.model.loss_and_grads(self.history, self.target)[0]
        for _ in range(40):
            self.model.train_batch([(self.history, self.target)], learning_rate=0.08)
        new = self.model.loss_and_grads(self.history, self.target)[0]
        self.assertLess(new, old * 0.9)
        for key in before:
            self.assertGreater(np.linalg.norm(self.model.params[key] - before[key]), 1e-9, key)

    def test_future_mutation_cannot_affect_input_or_prediction(self):
        records = [Draw(i, i * 100, tuple(np.roll(np.arange(1, 11), i))) for i in range(1, 8)]
        a, h1 = causal_window(records, target_ordinal=5, cutoff_ms=499, lookback=3)
        changed = records[:4] + [Draw(i, i * 100, tuple(np.roll(np.arange(1, 11), i + 3)))
                                  for i in range(5, 8)]
        b, h2 = causal_window(changed, target_ordinal=5, cutoff_ms=499, lookback=3)
        np.testing.assert_array_equal(a, b)
        self.assertEqual(h1, h2)
        np.testing.assert_array_equal(self.model.predict(a), self.model.predict(b))

    def test_late_source_duplicate_and_gap_rejected(self):
        rows = [Draw(i, i * 100, tuple(range(1, 11))) for i in range(1, 5)]
        with self.assertRaisesRegex(ValueError, "INCOMPLETE"):
            causal_window(rows, 5, 399, 3)
        with self.assertRaisesRegex(ValueError, "DUPLICATE"):
            causal_window(rows + [rows[-1]], 5, 499, 3)
        with self.assertRaisesRegex(ValueError, "INCOMPLETE"):
            causal_window([rows[0], rows[1], rows[3]], 5, 499, 3)

    def test_frozen_replay_hash_and_live_mutation_isolation(self):
        wire = self.model.export_bytes()
        sha = hashlib.sha256(wire).hexdigest()
        frozen = RelationalStateModel.from_bytes(wire, sha)
        old = frozen.predict(self.history)
        self.model.train_batch([(self.history, self.target)])
        np.testing.assert_array_equal(old, frozen.predict(self.history))
        self.assertEqual(wire, frozen.export_bytes())
        with self.assertRaisesRegex(ValueError, "HASH"):
            RelationalStateModel.from_bytes(wire + b" ", sha)
        data = json.loads(wire)
        data["weights"]["E"] = [[0]]
        with self.assertRaisesRegex(ValueError, "SHAPE"):
            RelationalStateModel.from_bytes(json.dumps(data).encode())

    def test_probability_and_action_contract(self):
        p = self.model.predict(self.history)
        self.assertTrue(np.isfinite(p).all())
        self.assertTrue((p > 0).all())
        np.testing.assert_allclose(p.sum(axis=1), np.ones(10), atol=1e-14)
        pos, numbers, mass = self.model.actions(p)
        self.assertIn(pos, range(1, 11))
        self.assertEqual(len(set(numbers)), 3)
        self.assertTrue(0 < mass <= 1)
        self.assertEqual(self.model.actions(np.full((10, 10), 0.1))[:2], (1, (1, 2, 3)))
        with self.assertRaisesRegex(ValueError, "SUM"):
            self.model.actions(np.full((10, 10), 0.2))

    def test_prediction_is_read_only_and_history_order_matters(self):
        old = self.model.export_bytes()
        p = self.model.predict(self.history)
        self.assertEqual(old, self.model.export_bytes())
        self.assertGreater(np.max(np.abs(p - self.model.predict(self.history[::-1]))), 1e-8)

    def test_adam_checkpoint_resumes_identical_update(self):
        opt = AdamOptimizer(self.model.params)
        for _ in range(3):
            self.model.train_batch([(self.history, self.target)], optimizer=opt)
        restored = RelationalStateModel.from_bytes(self.model.export_bytes())
        restored_opt = AdamOptimizer.from_bytes(opt.export_bytes(), restored.params)
        self.model.train_batch([(self.history, self.target)], optimizer=opt)
        restored.train_batch([(self.history, self.target)], optimizer=restored_opt)
        self.assertEqual(self.model.export_bytes(), restored.export_bytes())
        self.assertEqual(opt.export_bytes(), restored_opt.export_bytes())

    def test_invalid_draw_and_nonfinite_weights_rejected(self):
        with self.assertRaisesRegex(ValueError, "PERMUTATION"):
            Draw(1, 100, (1,) * 10)
        self.model.params["R"][0, 0] = np.nan
        with self.assertRaisesRegex(ValueError, "NONFINITE"):
            self.model.predict(self.history)


if __name__ == "__main__":
    unittest.main(verbosity=2)
