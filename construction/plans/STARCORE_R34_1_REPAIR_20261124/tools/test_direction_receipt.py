import copy
import hashlib
import json
import unittest
from direction_receipt import verify_direction_measurements


class Evidence:
    def __init__(self, rows):
        self.body = {"counts/by~direction": rows}
        self.hashes = {"all.json": hashlib.sha256(json.dumps(self.body).encode()).hexdigest()}

    def json(self, name):
        if name not in self.hashes:
            raise ValueError(name)
        return self.body


class DirectionTest(unittest.TestCase):
    def setUp(self):
        case = "B08-case"
        commit = "a"*40
        self.case = {"id": case, "field3_directions": []}
        self.run = {"source_after": commit, "direction_results": []}
        self.obs, self.rows = [], []
        for key in ("capabilities:F01", "needs:DEVELOPMENT"):
            digest = hashlib.sha256(key.encode()).hexdigest()
            contract = {"key": key, "contract_sha256": digest}
            self.case["field3_directions"].append(contract)
            result = dict(contract)
            for polarity in ("positive", "negative"):
                metric = key + "-" + polarity
                result[polarity + "_metric"] = metric
                self.obs.append({"metric": metric, "evidence_artifact": "all.json",
                                 "json_pointer": f"/counts~1by~0direction/{len(self.rows)}/effective_count"})
                self.rows.append({"direction_key": key, "contract_sha256": digest, "polarity": polarity,
                                  "case_id": case, "source_commit": commit, "effective_count": 0})
            self.run["direction_results"].append(result)

    def check(self):
        verify_direction_measurements(self.case, self.run, self.obs, Evidence(self.rows))

    def test_shared_file_with_distinct_observations_and_real_zero(self):
        self.check()

    def test_historical_case_without_directions(self):
        verify_direction_measurements({"id": "old"}, {}, [], Evidence([]))
        with self.assertRaises(ValueError):
            verify_direction_measurements({"id": "old"}, self.run, self.obs, Evidence(self.rows))

    def test_missing_or_surplus_direction(self):
        self.run["direction_results"].pop()
        with self.assertRaises(ValueError):
            self.check()

    def test_duplicate_key_and_stale_contract(self):
        self.run["direction_results"][0]["contract_sha256"] = "7"*64
        with self.assertRaises(ValueError):
            self.check()
        self.run["direction_results"][0] = copy.copy(self.run["direction_results"][1])
        with self.assertRaises(ValueError):
            self.check()

    def test_reused_positive_or_negative_metric(self):
        result = self.run["direction_results"][0]
        result["negative_metric"] = result["positive_metric"]
        with self.assertRaises(ValueError):
            self.check()

    def test_wrong_raw_identity(self):
        for key in ("case_id", "source_commit", "contract_sha256", "direction_key", "polarity"):
            with self.subTest(key=key):
                before = self.rows[2][key]
                self.rows[2][key] = "unrelated"
                with self.assertRaises(ValueError):
                    self.check()
                self.rows[2][key] = before

    def test_absent_measurement_and_shared_source_row(self):
        self.obs[-1]["metric"] = "other"
        with self.assertRaises(ValueError):
            self.check()
        self.obs[-1]["metric"] = self.run["direction_results"][-1]["negative_metric"]
        self.rows[0]["another_field"] = 0
        self.obs[-1]["json_pointer"] = self.obs[0]["json_pointer"].replace("effective_count", "another_field")
        with self.assertRaises(ValueError):
            self.check()

    def test_metadata_field_is_not_a_measurement(self):
        self.obs[0]["json_pointer"] = self.obs[0]["json_pointer"].replace("effective_count", "source_commit")
        with self.assertRaises(ValueError):
            self.check()


if __name__ == "__main__":
    unittest.main()
