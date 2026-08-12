#!/usr/bin/env python3
"""Narrow deterministic tests for the controlled evidence correlator."""

from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock
import xml.etree.ElementTree as ET
import zlib

import correlate_evidence as evidence


def game_row(**overrides) -> evidence.GameRow:
    values = {
        "game_id": 72276,
        "winner": "asdf",
        "loser": "~Rando_Cal",
        "win_reason": "timeout",
        "lose_reason": "timeout",
        "win_recording_id": "pd4emldbzpvhtduh",
        "lose_recording_id": "5vgvkjr4wo3ofq8y",
        "start_ms": 1_786_220_400_000,
        "end_ms": 1_786_220_700_000,
        "winner_side": "Light",
        "format_name": "Open",
        "winner_deck_name": "LS fixture",
        "loser_deck_name": "DS fixture",
    }
    values.update(overrides)
    return evidence.GameRow(**values)


def write_replay(path: Path, segments: list[list[str]]) -> None:
    root = ET.Element("gevents")
    for segment in segments:
        for message in segment:
            ET.SubElement(root, "ge", {"type": "M", "message": message})
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(zlib.compress(ET.tostring(root, encoding="utf-8")))


class DatabaseTests(unittest.TestCase):
    def test_exact_select_uses_read_only_transaction_and_container_env(self) -> None:
        stdout = (
            "72276\tasdf\t~Rando_Cal\ttimeout\ttimeout\t"
            "pd4emldbzpvhtduh\t5vgvkjr4wo3ofq8y\t"
            "1786220400000\t1786220700000\tLight\tOpen\tLS fixture\tDS fixture\n"
        )
        completed = subprocess.CompletedProcess([], 0, stdout=stdout, stderr="")
        with mock.patch.object(evidence.subprocess, "run", return_value=completed) as run:
            row = evidence.query_game_history(72276, "gemp_swccg_db_1")

        self.assertEqual(row.game_id, 72276)
        args, kwargs = run.call_args
        command = args[0]
        sql = kwargs["input"]
        self.assertEqual(command[:4], ["docker", "exec", "-i", "gemp_swccg_db_1"])
        self.assertIn('$MYSQL_USER', command[-1])
        self.assertIn('$MYSQL_PASSWORD', command[-1])
        self.assertIn('$MYSQL_DATABASE', command[-1])
        self.assertIn("START TRANSACTION READ ONLY;", sql)
        self.assertIn("FROM game_history", sql)
        self.assertIn("WHERE id=72276;", sql)
        self.assertIn("ROLLBACK;", sql)
        self.assertNotRegex(sql.upper(), r"\b(?:INSERT|UPDATE|DELETE|REPLACE|ALTER|DROP)\b")

    def test_database_failure_withholds_client_stderr(self) -> None:
        completed = subprocess.CompletedProcess(
            [], 1, stdout="", stderr="password=do-not-print-this"
        )
        with mock.patch.object(evidence.subprocess, "run", return_value=completed):
            with self.assertRaises(evidence.EvidenceError) as raised:
                evidence.query_game_history(72276, "gemp_swccg_db_1")
        self.assertNotIn("do-not-print-this", str(raised.exception))

    def test_orientation_mismatch_fails_closed(self) -> None:
        with self.assertRaisesRegex(evidence.EvidenceError, "orientation mismatch"):
            evidence.validate_game_row(
                game_row(winner="~Rando_Cal", loser="asdf", winner_side="Light"),
                72276,
            )


class ReplayTests(unittest.TestCase):
    def test_recording_ids_resolve_only_to_db_participant_directories(self) -> None:
        row = game_row()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            expected = {
                "asdf": root / "replays/asdf/pd4emldbzpvhtduh.xml.gz",
                "~Rando_Cal": root / "replays/~Rando_Cal/5vgvkjr4wo3ofq8y.xml.gz",
            }
            for path in expected.values():
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(b"fixture")
            self.assertEqual(evidence.resolve_replay_paths(root, row), expected)

            duplicate = root / "replays/~Rando_Cal/pd4emldbzpvhtduh.xml.gz"
            duplicate.write_bytes(b"duplicate")
            with self.assertRaisesRegex(evidence.EvidenceError, "resolved to 2 local files"):
                evidence.resolve_replay_paths(root, row)

    def test_only_matching_final_segments_validate(self) -> None:
        final = [
            "You're starting a game",
            "Players in the game are: asdf, ~Rando_Cal",
            "asdf deploys Luke to Naboo: Theed Palace Throne Room",
            "~Rando_Cal lost due to: timeout",
            "asdf is the winner due to: timeout",
        ]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            asdf_path = root / "asdf.xml.gz"
            rando_path = root / "rando.xml.gz"
            write_replay(asdf_path, [["You're starting a game", "discarded resend"], final])
            write_replay(rando_path, [["You're starting a game", "older history"], final])

            replays = {
                "asdf": evidence.parse_replay(asdf_path, "asdf"),
                "~Rando_Cal": evidence.parse_replay(rando_path, "~Rando_Cal"),
            }
            evidence.validate_replay_pair(game_row(), replays)
            self.assertEqual(replays["asdf"].segment_count, 2)
            self.assertEqual(replays["asdf"].messages, tuple(final))

            bad_path = root / "bad.xml.gz"
            bad_final = final.copy()
            bad_final[2] = "asdf deploys Leia elsewhere"
            write_replay(bad_path, [bad_final])
            replays["~Rando_Cal"] = evidence.parse_replay(
                bad_path, "~Rando_Cal"
            )
            with self.assertRaisesRegex(evidence.EvidenceError, "different public fingerprints"):
                evidence.validate_replay_pair(game_row(), replays)

    def test_terminal_reason_mismatch_fails_closed(self) -> None:
        final = [
            "You're starting a game",
            "Players in the game are: asdf, ~Rando_Cal",
            "~Rando_Cal lost due to: timeout",
            "asdf is the winner due to: timeout",
        ]
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            paths = {
                "asdf": root / "asdf.xml.gz",
                "~Rando_Cal": root / "rando.xml.gz",
            }
            for path in paths.values():
                write_replay(path, [final])
            replays = {
                owner: evidence.parse_replay(path, owner)
                for owner, path in paths.items()
            }
            with self.assertRaisesRegex(evidence.EvidenceError, "winner reason"):
                evidence.validate_replay_pair(
                    game_row(win_reason="conceded"), replays
                )


class LogTests(unittest.TestCase):
    def test_current_wmaop_child_veto_tag_is_recognized(self) -> None:
        timestamp = datetime(2026, 8, 12, 12, 0, tzinfo=timezone.utc)
        record = evidence.LogRecord(
            timestamp,
            Path("fixture.log"),
            1,
            "Reasoning: WMAOP.BLOCKADE_ONLY: non-Blockade candidate offered by a WMAOP search - veto",
        )
        log_slice = evidence.LogSlice(
            (record,), (record.path,), timestamp, timestamp, record
        )
        self.assertEqual(
            len(evidence.tag_hits(log_slice)["WMAOP.BLOCKADE_NEGATIVE"]),
            1,
        )

    def test_log_slice_is_db_bounded_and_requires_one_rando_start(self) -> None:
        row = game_row(
            start_ms=int(datetime(2026, 8, 12, 12, 0, tzinfo=timezone.utc).timestamp() * 1000),
            end_ms=int(datetime(2026, 8, 12, 12, 5, tzinfo=timezone.utc).timestamp() * 1000),
        )
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            log = root / "logs/gemp-swccg.log"
            log.parent.mkdir(parents=True)
            log.write_text(
                "2026-08-12 11:57:59,999 ignored before bound\n"
                "2026-08-12 12:00:01,000 New game started vs asdf as DARK\n"
                "2026-08-12 12:02:00,000 Best action: Deploy card\n"
                "2026-08-12 12:02:00,001 Reasoning: Selected executable response to a two-turn drain lane; target=Beldon's Corridor#42\n"
                "2026-08-12 12:02:00,001 repeated evaluator row\n"
                "2026-08-12 12:02:00,001 repeated evaluator row\n"
                "2026-08-12 12:08:01,000 ignored after bound\n",
                encoding="utf-8",
            )
            log_slice = evidence.collect_game_log_slice(root, row)
            self.assertEqual(len(log_slice.records), 5)
            self.assertEqual(len(evidence.tag_hits(log_slice)["B1-PERSIST"]), 1)
            self.assertEqual(
                sum(record.line.endswith("repeated evaluator row")
                    for record in log_slice.records),
                2,
            )

            with log.open("a", encoding="utf-8") as handle:
                handle.write(
                    "2026-08-12 12:04:00,000 New game started vs another-player as LIGHT\n"
                )
            with self.assertRaisesRegex(evidence.EvidenceError, "contain 2 AI game starts"):
                evidence.collect_game_log_slice(root, row)

    def test_log_start_must_match_db_start(self) -> None:
        row = game_row(
            start_ms=int(datetime(2026, 8, 12, 12, 0, tzinfo=timezone.utc).timestamp() * 1000),
            end_ms=int(datetime(2026, 8, 12, 12, 5, tzinfo=timezone.utc).timestamp() * 1000),
        )
        with tempfile.TemporaryDirectory() as temp:
            log = Path(temp) / "logs/gemp-swccg.log"
            log.parent.mkdir(parents=True)
            log.write_text(
                "2026-08-12 12:01:00,000 New game started vs asdf as DARK\n",
                encoding="utf-8",
            )
            with self.assertRaisesRegex(evidence.EvidenceError, "within 30 seconds"):
                evidence.collect_game_log_slice(Path(temp), row)


if __name__ == "__main__":
    unittest.main()
