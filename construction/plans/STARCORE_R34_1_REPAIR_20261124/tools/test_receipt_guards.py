#!/usr/bin/env python3
"""Host tests of receipt rejection rules; these are not Android or quality tests."""
import unittest
from verify_receipt import assert_measured, product_paths, background_window_seconds


class MeasuredAssertionGuards(unittest.TestCase):
    def test_missing_original_is_rejected(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "commits", "actual": 1, "expected": 1})

    def test_false_success_is_rejected(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "commits", "actual": 0, "expected": 1,
                             "evidence_artifact": "commits.json"})

    def test_unknown_cannot_become_zero(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "training", "actual": None, "expected": 0,
                             "evidence_artifact": "training.json"})

    def test_boolean_is_not_counter(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "trained", "actual": True, "expected": 1,
                             "operator": "ge", "evidence_artifact": "training.json"})

    def test_nan_is_not_passing_measurement(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "loss", "actual": float("nan"), "expected": 1,
                             "operator": "le", "evidence_artifact": "training.json"})

    def test_measured_bound_passes(self):
        assert_measured({"metric": "commits", "actual": 2, "expected": 1,
                         "operator": "ge", "evidence_artifact": "commits.json"})

    def test_unknown_operator_is_rejected(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "commits", "actual": 1, "expected": 1,
                             "operator": "ASSUME_PASS", "evidence_artifact": "commits.json"})

    def test_equal_infinities_do_not_pass(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "loss", "actual": float("inf"), "expected": float("inf"),
                             "evidence_artifact": "loss.json"})

    def test_membership_cannot_turn_boolean_into_count(self):
        with self.assertRaises(ValueError):
            assert_measured({"metric": "count", "actual": True, "expected": [1],
                             "operator": "in", "evidence_artifact": "count.json"})

    def test_version_and_tests_alone_are_not_product_implementation(self):
        self.assertFalse(product_paths({"release/release_manifest.json",
            "app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java",
            "app/src/androidTest/java/com/ethk3/novaonepro/Test.java", "tools/r341/test_identity.py"}))

    def test_production_owner_change_is_visible_for_review(self):
        path="app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java"
        self.assertEqual(product_paths({path,"release/release_manifest.json"}),{path})

    def test_five_minute_window_cannot_sign_72_hours(self):
        with self.assertRaises(ValueError):
            background_window_seconds("2026-11-24T10:00:00+08:00", "2026-11-24T10:05:00+08:00")

    def test_24_hour_checkpoint_is_not_final_72_hour_pass(self):
        with self.assertRaises(ValueError):
            background_window_seconds("2026-11-24T10:00:00+08:00", "2026-11-25T10:00:00+08:00")

    def test_real_72_hour_span_can_be_structurally_checked(self):
        self.assertEqual(background_window_seconds("2026-11-24T10:00:00+08:00", "2026-11-27T10:00:00+08:00"),259200)


if __name__ == "__main__":
    unittest.main()
