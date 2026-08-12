#!/usr/bin/env python3
"""Deterministic offline tests for the one-shot botgame evidence runner."""

from __future__ import annotations

import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock
import xml.etree.ElementTree as ET
import zlib

import run_exact_chosen_rando as runner


def make_config(root: Path, **overrides) -> runner.Config:
    values = {
        "base_url": "http://localhost:17001",
        "admin_username": "admin-user",
        "admin_password": "not-written-to-reports",
        "format_code": "open",
        "light_deck": "Chosen fixture",
        "dark_deck": "Rando fixture",
        "deck_owner": "deck-owner",
        "repo_root": root,
        "db_container": "gemp_swccg_db_1",
        "post_timeout_seconds": 1800,
        "report_dir": runner.OUTPUT_ROOT / "evidence_reports",
    }
    values.update(overrides)
    return runner.Config(**values)


def make_row(**overrides) -> runner.GameRow:
    values = {
        "game_history_id": 80001,
        "winner": runner.LIGHT_PLAYER,
        "loser": runner.DARK_PLAYER,
        "win_reason": runner.NATURAL_WIN_REASON,
        "lose_reason": runner.NATURAL_LOSE_REASON,
        "win_recording_id": "lightrecording01",
        "lose_recording_id": "darkrecording001",
        "start_ms": 100_000,
        "end_ms": 100_000,
        "winner_side": "Light",
        "format_name": "Open",
        "winner_deck_name": "Chosen fixture",
        "loser_deck_name": "Rando fixture",
    }
    values.update(overrides)
    return runner.GameRow(**values)


def final_messages(row: runner.GameRow) -> list[str]:
    return [
        "You're starting a game of Open",
        f"Players in the game are: {runner.LIGHT_PLAYER}, {runner.DARK_PLAYER}",
        f"{row.loser} lost due to: {row.lose_reason}",
        f"{row.winner} is the winner due to: {row.win_reason}",
    ]


def write_replay(path: Path, segments: list[list[str]]) -> None:
    root = ET.Element("gameReplay")
    for segment in segments:
        for message in segment:
            ET.SubElement(root, "ge", {"type": "M", "message": message})
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(zlib.compress(ET.tostring(root, encoding="utf-8")))


def write_success_evidence(root: Path, row: runner.GameRow, game_id: str) -> None:
    messages = final_messages(row)
    for owner in (runner.LIGHT_PLAYER, runner.DARK_PLAYER):
        recording_id = row.recording_for(owner)
        write_replay(
            root / "replays" / owner / f"{recording_id}.xml.gz",
            [["You're starting a game of Old", "discarded"], messages],
        )
    log = root / "logs" / "gemp-swccg.log"
    log.parent.mkdir(parents=True, exist_ok=True)
    log.write_text(
        "2026-08-12 12:00:00,001 INFO BOTGAME AI REGISTERED "
        f"gameId={game_id} side=LIGHT playerId={runner.LIGHT_PLAYER} "
        f"controllerClass={runner.LIGHT_CONTROLLER} deckOwner=deck-owner "
        "deck=Chosen fixture\n"
        "2026-08-12 12:00:00,002 INFO BOTGAME AI REGISTERED "
        f"gameId={game_id} side=DARK playerId={runner.DARK_PLAYER} "
        f"controllerClass={runner.DARK_CONTROLLER} deckOwner=deck-owner "
        "deck=Rando fixture\n",
        encoding="utf-8",
    )


class ConfigTests(unittest.TestCase):
    def test_environment_requires_explicit_arm_and_credentials(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            values = {
                "GEMP_ROOT": temp,
                "GEMP_ADMIN_USERNAME": "admin-user",
                "GEMP_ADMIN_PASSWORD": "secret",
                "GEMP_BOTGAME_FORMAT": "open",
                "GEMP_BOTGAME_LIGHT_DECK": "Chosen fixture",
                "GEMP_BOTGAME_DARK_DECK": "Rando fixture",
                "GEMP_BOTGAME_DECK_OWNER": "deck-owner",
            }
            with self.assertRaisesRegex(runner.ValidationError, "ARM"):
                runner.Config.from_environment(values)
            values["GEMP_BOTGAME_ARM"] = runner.ARM_VALUE
            config = runner.Config.from_environment(values)
            self.assertEqual(config.admin_password, "secret")
            self.assertNotIn("secret", json.dumps(config.public_request()))

    def test_base_url_cannot_embed_credentials(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            values = {
                "GEMP_BOTGAME_ARM": runner.ARM_VALUE,
                "GEMP_ROOT": temp,
                "GEMP_ADMIN_USERNAME": "admin-user",
                "GEMP_ADMIN_PASSWORD": "secret",
                "GEMP_BOTGAME_FORMAT": "open",
                "GEMP_BOTGAME_LIGHT_DECK": "Chosen fixture",
                "GEMP_BOTGAME_DARK_DECK": "Rando fixture",
                "GEMP_BOTGAME_DECK_OWNER": "deck-owner",
                "GEMP_BASE_URL": "http://user:secret@localhost:17001",
            }
            with self.assertRaisesRegex(runner.ValidationError, "without credentials"):
                runner.Config.from_environment(values)


class HallTests(unittest.TestCase):
    def test_empty_authenticated_hall_passes(self) -> None:
        snapshot = runner.parse_hall(
            b'<hall aiTablesEnabledBoolean="true" channelNumber="1" />'
        )
        runner.require_clear_hall(snapshot, "fixture")
        self.assertEqual(snapshot.summary()["playing"], 0)

    def test_waiting_or_playing_table_fails_closed(self) -> None:
        for status in ("WAITING", "PLAYING"):
            with self.subTest(status=status):
                snapshot = runner.parse_hall(
                    (
                        '<hall aiTablesEnabledBoolean="true">'
                        f'<table id="table-1" status="{status}" />'
                        "</hall>"
                    ).encode()
                )
                with self.assertRaisesRegex(runner.ValidationError, "active Hall table"):
                    runner.require_clear_hall(snapshot, "fixture")

    def test_disabled_ai_tables_or_unknown_state_fails_closed(self) -> None:
        disabled = runner.parse_hall(b'<hall aiTablesEnabledBoolean="false" />')
        with self.assertRaisesRegex(runner.ValidationError, "disabled"):
            runner.require_clear_hall(disabled, "fixture")
        with self.assertRaisesRegex(runner.ValidationError, "unknown status"):
            runner.parse_hall(
                b'<hall aiTablesEnabledBoolean="true"><table status="MAYBE" /></hall>'
            )


class RequestTests(unittest.TestCase):
    def test_runner_has_no_setting_routes_or_mtime_selection(self) -> None:
        source = Path(runner.__file__).read_text(encoding="utf-8")
        self.assertNotIn("/admin/settings", source)
        self.assertNotIn("/admin/shutdown", source)
        self.assertNotIn("st_mtime", source)

    def test_form_is_exactly_chosen_light_and_rando_dark(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            form = runner.botgame_form(make_config(Path(temp)))
        self.assertEqual(
            form,
            {
                "format": "open",
                "lightSkill": "CHOSENONE",
                "lightDeck": "Chosen fixture",
                "darkSkill": "RANDO",
                "darkDeck": "Rando fixture",
                "deckOwner": "deck-owner",
            },
        )

    def test_client_fuse_refuses_second_botgame_post(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            client = runner.AuthenticatedGempClient(make_config(Path(temp)))
            with mock.patch.object(
                client,
                "_open",
                return_value=b"OK gameId=abcdefgh-1234",
            ) as opened:
                self.assertEqual(client.post_botgame_once(), "abcdefgh-1234")
                with self.assertRaisesRegex(runner.ValidationError, "second request"):
                    client.post_botgame_once()
        self.assertEqual(opened.call_count, 1)
        self.assertEqual(client.botgame_post_count, 1)

    def test_transmission_timeout_has_no_retry_language(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            client = runner.AuthenticatedGempClient(make_config(Path(temp)))
            with mock.patch.object(
                client,
                "_open",
                side_effect=runner.ValidationError("timeout"),
            ) as opened:
                with self.assertRaisesRegex(runner.ValidationError, "do not retry"):
                    client.post_botgame_once()
        self.assertEqual(opened.call_count, 1)
        self.assertEqual(client.botgame_post_count, 1)


class DatabaseTests(unittest.TestCase):
    def test_queries_are_read_only_and_use_container_credentials(self) -> None:
        completed = subprocess.CompletedProcess([], 0, stdout="900\n", stderr="")
        database = runner.ReadOnlyGameHistory("gemp_swccg_db_1")
        with mock.patch.object(runner.subprocess, "run", return_value=completed) as run:
            self.assertEqual(database.high_water_mark(), 900)
        args, kwargs = run.call_args
        command = args[0]
        sql = kwargs["input"]
        self.assertEqual(command[:4], ["docker", "exec", "-i", "gemp_swccg_db_1"])
        self.assertIn('$MYSQL_USER', command[-1])
        self.assertIn('$MYSQL_PASSWORD', command[-1])
        self.assertIn("START TRANSACTION READ ONLY", sql)
        self.assertIn("SELECT COALESCE(MAX(id),0)", sql)
        self.assertIn("ROLLBACK", sql)
        self.assertNotRegex(
            sql.upper(), r"\b(?:INSERT|UPDATE|DELETE|REPLACE|ALTER|DROP|CREATE)\b"
        )

    def test_rows_after_parses_all_identity_fields(self) -> None:
        stdout = (
            "80001\t~The_Chosen_One\t~Rando_Cal\t"
            "Depleted opponent's Life Force\tLife Force depleted\t"
            "lightrecording01\tdarkrecording001\t100000\t100000\tLight\t"
            "Open\tChosen fixture\tRando fixture\n"
        )
        completed = subprocess.CompletedProcess([], 0, stdout=stdout, stderr="")
        database = runner.ReadOnlyGameHistory("gemp_swccg_db_1")
        with mock.patch.object(runner.subprocess, "run", return_value=completed) as run:
            rows = database.rows_after(79999)
        self.assertEqual(rows, (make_row(),))
        sql = run.call_args.kwargs["input"]
        self.assertIn("WHERE id>79999", sql)
        self.assertNotRegex(
            sql.upper(), r"\b(?:INSERT|UPDATE|DELETE|REPLACE|ALTER|DROP|CREATE)\b"
        )

    def test_database_error_withholds_stderr(self) -> None:
        completed = subprocess.CompletedProcess(
            [], 1, stdout="", stderr="MYSQL_PASSWORD=do-not-print"
        )
        database = runner.ReadOnlyGameHistory("gemp_swccg_db_1")
        with mock.patch.object(runner.subprocess, "run", return_value=completed):
            with self.assertRaises(runner.ValidationError) as raised:
                database.high_water_mark()
        self.assertNotIn("do-not-print", str(raised.exception))


class RowResolutionTests(unittest.TestCase):
    def test_one_exact_row_resolves(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            config = make_config(Path(temp))
            row = make_row()
            self.assertEqual(
                runner.resolve_game_row([row], config, 100_000, 100_000), row
            )

    def test_multiple_exact_rows_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            config = make_config(Path(temp))
            rows = [make_row(), make_row(game_history_id=80002)]
            with self.assertRaisesRegex(runner.ValidationError, "2 exact"):
                runner.resolve_game_row(rows, config, 100_000, 100_000)

    def test_concession_and_orientation_mismatch_fail_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            config = make_config(Path(temp))
            concession = make_row(
                win_reason="Opponent conceded", lose_reason="Conceded"
            )
            with self.assertRaisesRegex(runner.ValidationError, "natural"):
                runner.resolve_game_row([concession], config, 100_000, 100_000)
            wrong_side = make_row(winner_side="Dark")
            with self.assertRaisesRegex(runner.ValidationError, "orientation"):
                runner.resolve_game_row([wrong_side], config, 100_000, 100_000)


class ReplayTests(unittest.TestCase):
    def test_recording_ids_resolve_exact_paths_without_mtime(self) -> None:
        row = make_row()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            for owner in (runner.LIGHT_PLAYER, runner.DARK_PLAYER):
                path = root / "replays" / owner / f"{row.recording_for(owner)}.xml.gz"
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_bytes(b"fixture")
            resolved = runner.resolve_replay_paths(root, row)
            self.assertEqual(
                resolved[runner.LIGHT_PLAYER].name,
                f"{row.win_recording_id}.xml.gz",
            )
            duplicate = (
                root
                / "replays"
                / runner.DARK_PLAYER
                / f"{row.win_recording_id}.xml.gz"
            )
            duplicate.write_bytes(b"duplicate")
            with self.assertRaisesRegex(runner.ValidationError, "2 local streams"):
                runner.resolve_replay_paths(root, row)

    def test_matching_final_segments_validate(self) -> None:
        row = make_row()
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            replays = {}
            for owner in (runner.LIGHT_PLAYER, runner.DARK_PLAYER):
                path = root / f"{owner}.xml.gz"
                write_replay(
                    path,
                    [["You're starting a game of Old", "discarded"], final_messages(row)],
                )
                replays[owner] = runner.parse_replay(path, owner)
            runner.validate_replay_pair(row, replays)
            self.assertEqual(replays[runner.LIGHT_PLAYER].segment_count, 2)
            self.assertEqual(replays[runner.LIGHT_PLAYER].discarded_message_count, 2)

            bad = root / "bad.xml.gz"
            messages = final_messages(row)
            messages.insert(2, "perspective mismatch")
            write_replay(bad, [messages])
            replays[runner.DARK_PLAYER] = runner.parse_replay(
                bad, runner.DARK_PLAYER
            )
            with self.assertRaisesRegex(runner.ValidationError, "fingerprints"):
                runner.validate_replay_pair(row, replays)

    def test_cancelled_replay_fails_closed(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            path = Path(temp) / "cancelled.xml.gz"
            write_replay(
                path,
                [["You're starting a game of Open", "Game was cancelled due to error"]],
            )
            with self.assertRaisesRegex(runner.ValidationError, "cancelled"):
                runner.parse_replay(path, runner.LIGHT_PLAYER)


class LogTests(unittest.TestCase):
    def test_exact_controller_anchors_are_collected(self) -> None:
        row = make_row()
        game_id = "abcdefgh-1234"
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_success_evidence(root, row, game_id)
            anchors, files = runner.collect_controller_anchors(
                root, row, game_id, make_config(root)
            )
            self.assertEqual(len(anchors), 2)
            self.assertEqual(len(files), 1)
            self.assertIn(runner.LIGHT_CONTROLLER, anchors[0].line)
            self.assertIn(runner.DARK_CONTROLLER, anchors[1].line)

    def test_wrong_controller_or_abort_fails_closed(self) -> None:
        row = make_row()
        game_id = "abcdefgh-1234"
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_success_evidence(root, row, game_id)
            log = root / "logs" / "gemp-swccg.log"
            text = log.read_text(encoding="utf-8").replace(
                runner.LIGHT_CONTROLLER, runner.DARK_CONTROLLER, 1
            )
            log.write_text(text, encoding="utf-8")
            with self.assertRaisesRegex(runner.ValidationError, "identity"):
                runner.collect_controller_anchors(root, row, game_id, make_config(root))

            write_success_evidence(root, row, game_id)
            with log.open("a", encoding="utf-8") as handle:
                handle.write(
                    f"2026-08-12 12:01:00,000 ERROR All-AI game {game_id} aborted: fixture\n"
                )
            with self.assertRaisesRegex(runner.ValidationError, "abort or error"):
                runner.collect_controller_anchors(root, row, game_id, make_config(root))


class FakeHttp:
    def __init__(self, hall: runner.HallSnapshot, game_id: str):
        self.snapshot = hall
        self.game_id = game_id
        self.botgame_post_count = 0
        self.calls: list[str] = []

    def login(self) -> None:
        self.calls.append("login")

    def hall(self) -> runner.HallSnapshot:
        self.calls.append("hall")
        return self.snapshot

    def post_botgame_once(self) -> str:
        self.calls.append("post")
        self.botgame_post_count += 1
        if self.botgame_post_count != 1:
            raise AssertionError("test double received a retry")
        return self.game_id


class FakeDatabase:
    def __init__(self, row: runner.GameRow):
        self.row = row
        self.calls: list[object] = []

    def high_water_mark(self) -> int:
        self.calls.append("high_water")
        return self.row.game_history_id - 1

    def rows_after(self, mark: int) -> tuple[runner.GameRow, ...]:
        self.calls.append(("rows_after", mark))
        return (self.row,)


class SequenceMonotonic:
    def __init__(self, values: list[float]):
        self.values = iter(values)

    def __call__(self) -> float:
        return next(self.values)


class OrchestrationTests(unittest.TestCase):
    def test_complete_offline_contract_posts_once_in_fixed_order(self) -> None:
        row = make_row()
        game_id = "abcdefgh-1234"
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            write_success_evidence(root, row, game_id)
            config = make_config(root)
            hall = runner.HallSnapshot(True, ())
            http = FakeHttp(hall, game_id)
            database = FakeDatabase(row)
            sleeps: list[float] = []
            journal: dict[str, object] = {}
            packet = runner.perform_validation(
                config,
                http_client=http,
                database=database,
                sleeper=sleeps.append,
                now_ms=lambda: 100_000,
                monotonic=SequenceMonotonic([0.0, 15.0]),
                journal=journal,
            )

        self.assertEqual(packet["status"], "PASS")
        self.assertEqual(http.calls, ["login", "hall", "hall", "post", "hall"])
        self.assertEqual(http.botgame_post_count, 1)
        self.assertEqual(sleeps, [15.0])
        self.assertEqual(
            database.calls, ["high_water", ("rows_after", row.game_history_id - 1)]
        )
        self.assertNotIn(config.admin_password, json.dumps(packet))
        self.assertEqual(packet["runtime_game_id"], game_id)

    def test_short_hall_gap_fails_before_post(self) -> None:
        with tempfile.TemporaryDirectory() as temp:
            config = make_config(Path(temp))
            http = FakeHttp(runner.HallSnapshot(True, ()), "abcdefgh-1234")
            database = FakeDatabase(make_row())
            with self.assertRaisesRegex(runner.ValidationError, "less than 15"):
                runner.perform_validation(
                    config,
                    http_client=http,
                    database=database,
                    sleeper=lambda seconds: None,
                    now_ms=lambda: 100_000,
                    monotonic=SequenceMonotonic([0.0, 14.999]),
                )
        self.assertEqual(http.botgame_post_count, 0)
        self.assertEqual(database.calls, [])


if __name__ == "__main__":
    unittest.main()
