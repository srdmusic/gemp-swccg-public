#!/usr/bin/env python3
"""Unit tests for compare_fixtures.py (stdlib unittest only).

Encodes the three false-parity cases from the Codex review of e5b393955
(Handoffs/CODEX_FIXTURE_HARNESS_REVIEW_E5B393955_2026-07-13.md), each of
which the old comparator wrongly passed:

  1. Same top-five set and winner, candidate order reversed  -> must diverge
  2. Score 10.0 -> 10.005 under DEFAULT tolerance            -> must diverge
  3. decisionText + veto reason changed, veto count equal    -> must diverge

Plus a self-parity pass case and coverage of the other divergence kinds.

Run:
  python3 -m unittest discover tools/fixture-harness
"""

import contextlib
import io
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import compare_fixtures  # noqa: E402


def make_rec(seq=1, **overrides):
    rec = {
        "seq": seq,
        "source": "log",
        "game": "abc123",
        "bot": "RandoCalAi",
        "ts": "2026-07-12 10:00:00,000",
        "decisionType": "CARD_ACTION_CHOICE",
        "phase": "Deploy",
        "decisionText": "Choose action to perform",
        "top5": [["temp1", 10.0], ["temp2", 5.0], ["temp3", 1.0]],
        "chosen": "temp1",
    }
    rec.update(overrides)
    return rec


class CompareFixturesTest(unittest.TestCase):

    def run_compare(self, base_recs, shadow_recs, extra_args=()):
        """Write two JSONL files, run main(), return (exitcode, stdout, stderr)."""
        with tempfile.TemporaryDirectory() as td:
            bpath = os.path.join(td, "baseline.jsonl")
            spath = os.path.join(td, "shadow.jsonl")
            for path, recs in ((bpath, base_recs), (spath, shadow_recs)):
                with open(path, "w", encoding="utf-8") as fh:
                    for rec in recs:
                        fh.write(json.dumps(rec) + "\n")
            out, err = io.StringIO(), io.StringIO()
            with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
                rc = compare_fixtures.main([bpath, spath] + list(extra_args))
            return rc, out.getvalue(), err.getvalue()

    # ------------------------------------------------------------------
    # Pass case

    def test_self_parity(self):
        recs = [
            make_rec(seq=1),
            make_rec(seq=2, decisionType="MULTIPLE_CHOICE", phase="Battle",
                     vetoCount=2,
                     vetoes=["FORMATION SAFETY L3: veto pair-budget",
                             "HARD VETO: no battle destiny"]),
        ]
        rc, out, err = self.run_compare(recs, recs)
        self.assertEqual(rc, 0)
        self.assertIn("PARITY", err)
        self.assertEqual(out, "")

    # ------------------------------------------------------------------
    # Codex reviewer false-parity case 1: top-5 order reversed, same set

    def test_top5_order_reversed_diverges(self):
        base = [make_rec(top5=[["temp1", 10.0], ["temp2", 5.0], ["temp3", 1.0]])]
        shad = [make_rec(top5=[["temp3", 1.0], ["temp2", 5.0], ["temp1", 10.0]])]
        rc, out, err = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("TOP5-ORDER", out)
        self.assertIn("DIVERGENT", err)

    # ------------------------------------------------------------------
    # Codex reviewer false-parity case 2: 10.0 -> 10.005, default tolerance

    def test_score_drift_exact_by_default(self):
        base = [make_rec(top5=[["temp1", 10.0], ["temp2", 5.0]])]
        shad = [make_rec(top5=[["temp1", 10.005], ["temp2", 5.0]])]
        rc, out, err = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("SCORE-DRIFT", out)
        self.assertIn("DIVERGENT", err)

    def test_score_drift_allowed_only_via_explicit_tolerance(self):
        base = [make_rec(top5=[["temp1", 10.0]])]
        shad = [make_rec(top5=[["temp1", 10.005]])]
        rc, _, err = self.run_compare(base, shad, extra_args=["--tolerance", "0.01"])
        self.assertEqual(rc, 0)
        self.assertIn("PARITY", err)

    # ------------------------------------------------------------------
    # Codex reviewer false-parity case 3: decisionText and veto REASON
    # change while type/phase/winner/veto COUNT all stay equal

    def test_decision_text_and_veto_reason_diverge(self):
        base = [make_rec(decisionText="Choose action to perform",
                         vetoCount=1,
                         vetoes=["FORMATION SAFETY L3: veto Greedo pair-budget"])]
        shad = [make_rec(decisionText="Choose card to deploy",
                         vetoCount=1,
                         vetoes=["FORMATION SAFETY L4: veto Tarkin into armed Rey"])]
        rc, out, err = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("DECISION-TEXT", out)
        self.assertIn("VETO-REASON", out)
        self.assertNotIn("VETO-COUNT", out)  # counts are equal by construction
        self.assertIn("DIVERGENT", err)

    def test_veto_reason_alone_diverges(self):
        base = [make_rec(vetoCount=1, vetoes=["HARD VETO: reason A"])]
        shad = [make_rec(vetoCount=1, vetoes=["HARD VETO: reason B"])]
        rc, out, _ = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("VETO-REASON", out)

    def test_veto_order_diverges(self):
        base = [make_rec(vetoCount=2, vetoes=["HARD VETO: A", "HARD VETO: B"])]
        shad = [make_rec(vetoCount=2, vetoes=["HARD VETO: B", "HARD VETO: A"])]
        rc, out, _ = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("VETO-REASON", out)

    # ------------------------------------------------------------------
    # Existing divergence kinds still work

    def test_winner_diverges(self):
        rc, out, _ = self.run_compare([make_rec(chosen="temp1")],
                                      [make_rec(chosen="temp2")])
        self.assertEqual(rc, 1)
        self.assertIn("WINNER", out)

    def test_top5_set_diverges(self):
        base = [make_rec(top5=[["temp1", 10.0], ["temp2", 5.0]])]
        shad = [make_rec(top5=[["temp1", 10.0], ["temp9", 5.0]])]
        rc, out, _ = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("TOP5-SET", out)

    def test_veto_count_diverges(self):
        base = [make_rec(vetoCount=2, vetoes=["HARD VETO: A", "HARD VETO: B"])]
        shad = [make_rec(vetoCount=1, vetoes=["HARD VETO: A"])]
        rc, out, _ = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("VETO-COUNT", out)

    def test_missing_record_diverges(self):
        base = [make_rec(seq=1), make_rec(seq=2, phase="Battle")]
        shad = [make_rec(seq=1)]
        rc, out, _ = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("MISSING", out)

    def test_misaligned_slot_diverges(self):
        base = [make_rec(phase="Deploy")]
        shad = [make_rec(phase="Battle")]
        rc, out, _ = self.run_compare(base, shad)
        self.assertEqual(rc, 1)
        self.assertIn("MISALIGNED", out)

    def test_unreadable_file_exits_2(self):
        with tempfile.TemporaryDirectory() as td:
            bpath = os.path.join(td, "baseline.jsonl")
            with open(bpath, "w", encoding="utf-8") as fh:
                fh.write(json.dumps(make_rec()) + "\n")
            out, err = io.StringIO(), io.StringIO()
            with contextlib.redirect_stdout(out), contextlib.redirect_stderr(err):
                rc = compare_fixtures.main([bpath, os.path.join(td, "nope.jsonl")])
            self.assertEqual(rc, 2)


if __name__ == "__main__":
    unittest.main()
