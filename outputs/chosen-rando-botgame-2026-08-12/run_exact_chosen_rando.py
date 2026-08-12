#!/usr/bin/env python3
"""Run one controlled Chosen One Light versus Rando Dark validation game.

The bot-game POST is sent exactly once. Database access is SELECT-only, replay
identity comes only from game_history recording IDs, and no setting endpoint is
called. Any ambiguous identity or non-natural result fails the run.
"""

from __future__ import annotations

from dataclasses import asdict, dataclass, field
from datetime import datetime, timedelta, timezone
from http import cookiejar
import gzip
import hashlib
import html
import json
import os
from pathlib import Path
import re
import socket
import subprocess
import sys
import time
from typing import Callable, Iterable
from urllib import error, parse, request
import xml.etree.ElementTree as ET
import zlib


LIGHT_PLAYER = "~The_Chosen_One"
DARK_PLAYER = "~Rando_Cal"
LIGHT_SKILL = "CHOSENONE"
DARK_SKILL = "RANDO"
LIGHT_CONTROLLER = (
    "com.gempukku.swccgo.ai.models.chosenone.TheChosenOneAi"
)
DARK_CONTROLLER = "com.gempukku.swccgo.ai.models.rando.RandoCalAi"
NATURAL_WIN_REASON = "Depleted opponent's Life Force"
NATURAL_LOSE_REASON = "Life Force depleted"
START_MARKER = "You're starting a game"
ARM_VALUE = "CHOSENONE_LIGHT_VS_RANDO_DARK_ONCE"
HALL_GAP_SECONDS = 15.0
TIME_TOLERANCE_MS = 30_000

OUTPUT_ROOT = Path(__file__).resolve().parent
DEFAULT_REPORT_DIR = OUTPUT_ROOT / "evidence_reports"

RECORDING_ID_RE = re.compile(r"[A-Za-z0-9]{8,64}")
GAME_ID_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9-]{7,127}")
CONTAINER_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")
LOG_TS_RE = re.compile(r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}),(\d{3})")
CARD_HINT_RE = re.compile(
    r"<div class='cardHint'[^>]*value='([^']+)'[^>]*>(.*?)</div>"
)
PLAYERS_RE = re.compile(r"^Players in the game are: (.*)$")
WINNER_RE = re.compile(r"^(.*?) is the winner due to: (.*)$")
LOSER_RE = re.compile(r"^(.*?) lost due to: (.*)$")


class ValidationError(RuntimeError):
    """A fail-closed validation or infrastructure error."""


def require_text(name: str, value: str | None, max_length: int = 255) -> str:
    if value is None or not value.strip():
        raise ValidationError(f"required environment variable {name} is missing")
    if value != value.strip():
        raise ValidationError(f"{name} must not have leading or trailing whitespace")
    if len(value) > max_length or any(ord(char) < 32 for char in value):
        raise ValidationError(f"{name} contains invalid text")
    return value


def require_secret(name: str, value: str | None) -> str:
    if value is None or value == "":
        raise ValidationError(f"required environment variable {name} is missing")
    if len(value) > 4096 or "\x00" in value:
        raise ValidationError(f"{name} is invalid")
    return value


@dataclass(frozen=True)
class Config:
    base_url: str
    admin_username: str
    admin_password: str = field(repr=False)
    format_code: str
    light_deck: str
    dark_deck: str
    deck_owner: str
    repo_root: Path
    db_container: str
    post_timeout_seconds: int
    report_dir: Path

    @classmethod
    def from_environment(cls, env: dict[str, str] | None = None) -> "Config":
        values = os.environ if env is None else env
        if values.get("GEMP_BOTGAME_ARM") != ARM_VALUE:
            raise ValidationError(
                f"GEMP_BOTGAME_ARM must be exactly {ARM_VALUE}"
            )

        base_url = values.get("GEMP_BASE_URL", "http://localhost:17001").rstrip("/")
        parsed_url = parse.urlsplit(base_url)
        if (
            parsed_url.scheme not in {"http", "https"}
            or not parsed_url.netloc
            or parsed_url.username is not None
            or parsed_url.password is not None
            or parsed_url.path not in {"", "/"}
            or parsed_url.query
            or parsed_url.fragment
        ):
            raise ValidationError("GEMP_BASE_URL must be an origin without credentials")

        timeout_text = values.get("GEMP_BOTGAME_TIMEOUT_SECONDS", "1800")
        try:
            timeout = int(timeout_text)
        except ValueError as exc:
            raise ValidationError("GEMP_BOTGAME_TIMEOUT_SECONDS must be an integer") from exc
        if not 60 <= timeout <= 7200:
            raise ValidationError(
                "GEMP_BOTGAME_TIMEOUT_SECONDS must be between 60 and 7200"
            )

        db_container = values.get("GEMP_DB_CONTAINER", "gemp_swccg_db_1")
        if not CONTAINER_RE.fullmatch(db_container):
            raise ValidationError("GEMP_DB_CONTAINER is invalid")

        root_text = require_text("GEMP_ROOT", values.get("GEMP_ROOT"), 4096)
        repo_root = Path(root_text).expanduser().resolve()
        if not repo_root.is_dir():
            raise ValidationError("GEMP_ROOT is not an existing directory")

        report_text = values.get("GEMP_BOTGAME_REPORT_DIR")
        report_dir = (
            Path(report_text).expanduser().resolve()
            if report_text
            else DEFAULT_REPORT_DIR
        )
        if OUTPUT_ROOT.resolve() not in (report_dir, *report_dir.parents):
            raise ValidationError(
                "GEMP_BOTGAME_REPORT_DIR must stay under the botgame output directory"
            )

        format_code = require_text(
            "GEMP_BOTGAME_FORMAT", values.get("GEMP_BOTGAME_FORMAT"), 80
        )
        if not re.fullmatch(r"[A-Za-z0-9._-]+", format_code):
            raise ValidationError("GEMP_BOTGAME_FORMAT contains invalid characters")

        return cls(
            base_url=base_url,
            admin_username=require_text(
                "GEMP_ADMIN_USERNAME", values.get("GEMP_ADMIN_USERNAME"), 80
            ),
            admin_password=require_secret(
                "GEMP_ADMIN_PASSWORD", values.get("GEMP_ADMIN_PASSWORD")
            ),
            format_code=format_code,
            light_deck=require_text(
                "GEMP_BOTGAME_LIGHT_DECK", values.get("GEMP_BOTGAME_LIGHT_DECK")
            ),
            dark_deck=require_text(
                "GEMP_BOTGAME_DARK_DECK", values.get("GEMP_BOTGAME_DARK_DECK")
            ),
            deck_owner=require_text(
                "GEMP_BOTGAME_DECK_OWNER", values.get("GEMP_BOTGAME_DECK_OWNER"), 80
            ),
            repo_root=repo_root,
            db_container=db_container,
            post_timeout_seconds=timeout,
            report_dir=report_dir,
        )

    def public_request(self) -> dict[str, object]:
        return {
            "base_url": self.base_url,
            "format_code": self.format_code,
            "light_player": LIGHT_PLAYER,
            "light_skill": LIGHT_SKILL,
            "light_deck": self.light_deck,
            "dark_player": DARK_PLAYER,
            "dark_skill": DARK_SKILL,
            "dark_deck": self.dark_deck,
            "deck_owner": self.deck_owner,
            "repo_root": str(self.repo_root),
            "db_container": self.db_container,
            "post_timeout_seconds": self.post_timeout_seconds,
        }


@dataclass(frozen=True)
class HallTable:
    table_id: str
    game_id: str
    status: str
    players: str


@dataclass(frozen=True)
class HallSnapshot:
    ai_tables_enabled: bool
    tables: tuple[HallTable, ...]

    @property
    def active_tables(self) -> tuple[HallTable, ...]:
        return tuple(
            table for table in self.tables if table.status in {"WAITING", "PLAYING"}
        )

    def summary(self) -> dict[str, object]:
        return {
            "ai_tables_enabled": self.ai_tables_enabled,
            "waiting": sum(table.status == "WAITING" for table in self.tables),
            "playing": sum(table.status == "PLAYING" for table in self.tables),
            "finished_visible": sum(
                table.status == "FINISHED" for table in self.tables
            ),
        }


@dataclass(frozen=True)
class GameRow:
    game_history_id: int
    winner: str
    loser: str
    win_reason: str
    lose_reason: str
    win_recording_id: str
    lose_recording_id: str
    start_ms: int
    end_ms: int
    winner_side: str
    format_name: str
    winner_deck_name: str
    loser_deck_name: str

    @property
    def start_utc(self) -> datetime:
        return datetime.fromtimestamp(self.start_ms / 1000, tz=timezone.utc)

    @property
    def end_utc(self) -> datetime:
        return datetime.fromtimestamp(self.end_ms / 1000, tz=timezone.utc)

    def recording_for(self, player_id: str) -> str:
        if player_id == self.winner:
            return self.win_recording_id
        if player_id == self.loser:
            return self.lose_recording_id
        raise ValidationError("recording requested for a non-participant")

    def deck_for(self, player_id: str) -> str:
        if player_id == self.winner:
            return self.winner_deck_name
        if player_id == self.loser:
            return self.loser_deck_name
        raise ValidationError("deck requested for a non-participant")

    def report_dict(self) -> dict[str, object]:
        result = asdict(self)
        result["start_utc"] = self.start_utc.isoformat()
        result["end_utc"] = self.end_utc.isoformat()
        return result


@dataclass(frozen=True)
class ReplayEvidence:
    owner: str
    recording_id: str
    path: Path
    raw_message_count: int
    segment_count: int
    discarded_message_count: int
    final_message_count: int
    public_sha256: str
    participants: tuple[str, ...]
    winner: str
    loser: str
    win_reason: str
    lose_reason: str

    def report_dict(self) -> dict[str, object]:
        result = asdict(self)
        result["path"] = str(self.path)
        return result


@dataclass(frozen=True)
class LogAnchor:
    path: Path
    line_number: int
    timestamp: str
    line: str
    line_sha256: str

    def report_dict(self) -> dict[str, object]:
        result = asdict(self)
        result["path"] = str(self.path)
        return result


def utc_iso(milliseconds: int) -> str:
    return datetime.fromtimestamp(milliseconds / 1000, tz=timezone.utc).isoformat()


def parse_hall(xml_bytes: bytes) -> HallSnapshot:
    try:
        root = ET.fromstring(xml_bytes)
    except ET.ParseError as exc:
        raise ValidationError("authenticated Hall response was not valid XML") from exc
    if root.tag != "hall":
        raise ValidationError("authenticated Hall response did not contain a hall root")
    ai_value = root.get("aiTablesEnabledBoolean")
    if ai_value not in {"true", "false"}:
        raise ValidationError("Hall omitted the AI-table setting")

    tables: list[HallTable] = []
    for element in root.findall(".//table"):
        status = element.get("status", "").upper()
        if status not in {"WAITING", "PLAYING", "FINISHED"}:
            raise ValidationError("Hall contained a table with an unknown status")
        tables.append(
            HallTable(
                table_id=element.get("id", ""),
                game_id=element.get("gameId", ""),
                status=status,
                players=element.get("players", ""),
            )
        )
    return HallSnapshot(ai_value == "true", tuple(tables))


def require_clear_hall(snapshot: HallSnapshot, label: str) -> None:
    if not snapshot.ai_tables_enabled:
        raise ValidationError(f"{label}: AI tables are disabled")
    if snapshot.active_tables:
        states = ", ".join(
            f"{table.status}:{table.table_id or '?'}"
            for table in snapshot.active_tables
        )
        raise ValidationError(f"{label}: active Hall table detected ({states})")


def botgame_form(config: Config) -> dict[str, str]:
    return {
        "format": config.format_code,
        "lightSkill": LIGHT_SKILL,
        "lightDeck": config.light_deck,
        "darkSkill": DARK_SKILL,
        "darkDeck": config.dark_deck,
        "deckOwner": config.deck_owner,
    }


class AuthenticatedGempClient:
    """Small authenticated client with a one-shot botgame POST fuse."""

    def __init__(self, config: Config):
        self._config = config
        self._cookies = cookiejar.CookieJar()
        self._opener = request.build_opener(request.HTTPCookieProcessor(self._cookies))
        self.botgame_post_count = 0

    def _open(
        self,
        path: str,
        method: str,
        form: dict[str, str] | None,
        timeout: float,
    ) -> bytes:
        data = None if form is None else parse.urlencode(form).encode("utf-8")
        headers = {"Referer": f"{self._config.base_url}/gemp-swccg/hall.html"}
        if data is not None:
            headers["Content-Type"] = "application/x-www-form-urlencoded"
        req = request.Request(
            f"{self._config.base_url}{path}",
            data=data,
            headers=headers,
            method=method,
        )
        try:
            with self._opener.open(req, timeout=timeout) as response:
                if response.status != 200:
                    raise ValidationError(
                        f"{method} {path} failed with HTTP {response.status}"
                    )
                return response.read()
        except error.HTTPError as exc:
            raise ValidationError(
                f"{method} {path} failed with HTTP {exc.code}"
            ) from exc
        except (error.URLError, TimeoutError, socket.timeout, OSError) as exc:
            raise ValidationError(f"{method} {path} did not complete") from exc

    def login(self) -> None:
        self._open(
            "/gemp-swccg-server/login",
            "POST",
            {
                "login": self._config.admin_username,
                "password": self._config.admin_password,
            },
            15,
        )
        if not any(cookie.name == "loggedUser" for cookie in self._cookies):
            raise ValidationError("login returned without an authenticated session cookie")

    def hall(self) -> HallSnapshot:
        participant = parse.urlencode(
            {"participantId": self._config.admin_username}
        )
        body = self._open(
            f"/gemp-swccg-server/hall?{participant}", "GET", None, 15
        )
        return parse_hall(body)

    def post_botgame_once(self) -> str:
        if self.botgame_post_count != 0:
            raise ValidationError("botgame POST fuse refused a second request")
        self.botgame_post_count += 1
        try:
            body = self._open(
                "/gemp-swccg-server/admin/botgame",
                "POST",
                botgame_form(self._config),
                self._config.post_timeout_seconds,
            )
        except ValidationError as exc:
            raise ValidationError(
                "botgame POST failed or timed out after transmission; do not retry"
            ) from exc
        text = body.decode("utf-8", errors="strict").strip()
        match = re.fullmatch(r"OK gameId=([A-Za-z0-9][A-Za-z0-9-]{7,127})", text)
        if match is None:
            raise ValidationError("botgame POST returned an unexpected success body")
        return match.group(1)


class ReadOnlyGameHistory:
    """SELECT-only MariaDB client using credentials configured in the container."""

    def __init__(self, container: str):
        self._container = container

    def _query(self, sql: str) -> list[str]:
        if not CONTAINER_RE.fullmatch(self._container):
            raise ValidationError("database container name is invalid")
        credential_command = (
            'test -n "$MYSQL_USER" && test -n "$MYSQL_PASSWORD" '
            '&& test -n "$MYSQL_DATABASE" || exit 91; '
            'exec mariadb -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" '
            '"$MYSQL_DATABASE" --batch --raw --skip-column-names'
        )
        try:
            result = subprocess.run(
                [
                    "docker",
                    "exec",
                    "-i",
                    self._container,
                    "sh",
                    "-c",
                    credential_command,
                ],
                input=sql,
                text=True,
                capture_output=True,
                check=False,
                timeout=20,
            )
        except (OSError, subprocess.TimeoutExpired) as exc:
            raise ValidationError("read-only game_history query could not start") from exc
        if result.returncode != 0:
            raise ValidationError(
                f"read-only game_history query failed with exit {result.returncode}; details withheld"
            )
        return [line for line in result.stdout.splitlines() if line.strip()]

    def high_water_mark(self) -> int:
        sql = """START TRANSACTION READ ONLY;
SELECT COALESCE(MAX(id),0) FROM game_history;
ROLLBACK;
"""
        rows = self._query(sql)
        if len(rows) != 1:
            raise ValidationError("game_history high-water query did not return one row")
        try:
            value = int(rows[0])
        except ValueError as exc:
            raise ValidationError("game_history high-water mark was malformed") from exc
        if value < 0:
            raise ValidationError("game_history high-water mark was negative")
        return value

    def rows_after(self, high_water_mark: int) -> tuple[GameRow, ...]:
        if high_water_mark < 0:
            raise ValidationError("game_history high-water mark is invalid")
        sql = f"""START TRANSACTION READ ONLY;
SELECT id,winner,loser,COALESCE(win_reason,''),COALESCE(lose_reason,''),
       COALESCE(win_recording_id,''),COALESCE(lose_recording_id,''),
       start_date,end_date,COALESCE(winner_side,''),COALESCE(format_name,''),
       COALESCE(winner_deck_name,''),COALESCE(loser_deck_name,'')
FROM game_history
WHERE id>{high_water_mark}
ORDER BY id;
ROLLBACK;
"""
        result: list[GameRow] = []
        for raw in self._query(sql):
            fields = raw.split("\t")
            if len(fields) != 13:
                raise ValidationError("game_history row shape was malformed")
            try:
                result.append(
                    GameRow(
                        game_history_id=int(fields[0]),
                        winner=fields[1],
                        loser=fields[2],
                        win_reason=fields[3],
                        lose_reason=fields[4],
                        win_recording_id=fields[5],
                        lose_recording_id=fields[6],
                        start_ms=int(fields[7]),
                        end_ms=int(fields[8]),
                        winner_side=fields[9],
                        format_name=fields[10],
                        winner_deck_name=fields[11],
                        loser_deck_name=fields[12],
                    )
                )
            except ValueError as exc:
                raise ValidationError("game_history numeric field was malformed") from exc
        return tuple(result)


def resolve_game_row(
    rows: Iterable[GameRow],
    config: Config,
    post_started_ms: int,
    response_received_ms: int,
) -> GameRow:
    candidates: list[GameRow] = []
    for row in rows:
        if {row.winner, row.loser} != {LIGHT_PLAYER, DARK_PLAYER}:
            continue
        if row.deck_for(LIGHT_PLAYER) != config.light_deck:
            continue
        if row.deck_for(DARK_PLAYER) != config.dark_deck:
            continue
        if row.start_ms < post_started_ms - TIME_TOLERANCE_MS:
            continue
        if row.start_ms > response_received_ms + TIME_TOLERANCE_MS:
            continue
        if row.end_ms < row.start_ms:
            continue
        if row.end_ms > response_received_ms + TIME_TOLERANCE_MS:
            continue
        candidates.append(row)
    if len(candidates) != 1:
        raise ValidationError(
            f"new game_history rows resolved to {len(candidates)} exact matchup candidates; expected one"
        )
    row = candidates[0]
    validate_game_row(row, config)
    return row


def validate_game_row(row: GameRow, config: Config) -> None:
    if {row.winner, row.loser} != {LIGHT_PLAYER, DARK_PLAYER}:
        raise ValidationError("game_history participant identity mismatch")
    if row.deck_for(LIGHT_PLAYER) != config.light_deck:
        raise ValidationError("game_history Light deck identity mismatch")
    if row.deck_for(DARK_PLAYER) != config.dark_deck:
        raise ValidationError("game_history Dark deck identity mismatch")
    winner_side = row.winner_side.upper()
    if winner_side not in {"LIGHT", "DARK"}:
        raise ValidationError("game_history winner side is missing or invalid")
    light_player_side = (
        winner_side
        if row.winner == LIGHT_PLAYER
        else ("DARK" if winner_side == "LIGHT" else "LIGHT")
    )
    if light_player_side != "LIGHT":
        raise ValidationError("game_history bot orientation mismatch")
    if row.start_ms <= 0 or row.end_ms < row.start_ms:
        raise ValidationError("game_history time bounds are invalid")
    if row.win_reason != NATURAL_WIN_REASON or row.lose_reason != NATURAL_LOSE_REASON:
        raise ValidationError(
            "game did not end by natural Life Force depletion; timeout, cancellation, and concession are rejected"
        )
    if row.win_recording_id == row.lose_recording_id:
        raise ValidationError("game_history recording IDs are identical")
    for recording_id in (row.win_recording_id, row.lose_recording_id):
        if not RECORDING_ID_RE.fullmatch(recording_id):
            raise ValidationError("game_history recording ID is missing or malformed")
    try:
        row.start_utc
        row.end_utc
    except (OSError, OverflowError, ValueError) as exc:
        raise ValidationError("game_history time bounds are out of range") from exc


def resolve_replay_paths(repo_root: Path, row: GameRow) -> dict[str, Path]:
    replay_root = repo_root / "replays"
    resolved: dict[str, Path] = {}
    for owner in (LIGHT_PLAYER, DARK_PLAYER):
        recording_id = row.recording_for(owner)
        expected = replay_root / owner / f"{recording_id}.xml.gz"
        matches = sorted(replay_root.glob(f"*/{recording_id}.xml.gz"))
        if len(matches) != 1:
            raise ValidationError(
                f"recording {recording_id} resolved to {len(matches)} local streams; expected one"
            )
        if matches[0].resolve() != expected.resolve() or not expected.is_file():
            raise ValidationError(
                f"recording {recording_id} is missing from its exact participant directory"
            )
        resolved[owner] = expected
    return resolved


def inflate_replay(path: Path) -> bytes:
    try:
        compressed = path.read_bytes()
    except OSError as exc:
        raise ValidationError(f"replay read failed for {path.name}") from exc
    for window_bits in (
        zlib.MAX_WBITS | 32,
        zlib.MAX_WBITS,
        -zlib.MAX_WBITS,
    ):
        try:
            return zlib.decompress(compressed, window_bits)
        except zlib.error:
            continue
    raise ValidationError(f"replay decompression failed for {path.name}")


def clean_message(message: str) -> str:
    message = CARD_HINT_RE.sub(
        lambda match: html.unescape(re.sub(r"<[^>]+>", "", match.group(2))),
        message,
    )
    return html.unescape(re.sub(r"<[^>]+>", "", message)).strip()


def parse_replay(path: Path, owner: str) -> ReplayEvidence:
    try:
        root = ET.fromstring(inflate_replay(path))
    except ET.ParseError as exc:
        raise ValidationError(f"replay XML parse failed for {path.name}") from exc
    raw_messages = [
        element.get("message", "")
        for element in root.iter("ge")
        if element.get("type") == "M"
    ]
    starts = [
        index
        for index, message in enumerate(raw_messages)
        if html.unescape(message).startswith(START_MARKER)
    ]
    if not starts:
        raise ValidationError(f"replay {path.name} has no game-start segment")
    final_messages = raw_messages[starts[-1] :]
    cleaned = tuple(clean_message(message) for message in final_messages)

    participants: tuple[str, ...] = ()
    winner = loser = win_reason = lose_reason = ""
    for message in cleaned:
        players = PLAYERS_RE.match(message)
        if players:
            participants = tuple(
                sorted(part.strip() for part in players.group(1).split(","))
            )
        winner_match = WINNER_RE.match(message)
        if winner_match:
            winner, win_reason = winner_match.groups()
        loser_match = LOSER_RE.match(message)
        if loser_match:
            loser, lose_reason = loser_match.groups()
        if message.startswith("Game was cancelled"):
            raise ValidationError(f"replay {path.name} records a cancelled game")

    digest = hashlib.sha256("\n".join(cleaned).encode("utf-8")).hexdigest()
    return ReplayEvidence(
        owner=owner,
        recording_id=path.name.removesuffix(".xml.gz"),
        path=path,
        raw_message_count=len(raw_messages),
        segment_count=len(starts),
        discarded_message_count=starts[-1],
        final_message_count=len(final_messages),
        public_sha256=digest,
        participants=participants,
        winner=winner,
        loser=loser,
        win_reason=win_reason,
        lose_reason=lose_reason,
    )


def validate_replay_pair(
    row: GameRow, replays: dict[str, ReplayEvidence]
) -> None:
    if set(replays) != {LIGHT_PLAYER, DARK_PLAYER}:
        raise ValidationError("both exact participant replay streams are required")
    if len({replay.public_sha256 for replay in replays.values()}) != 1:
        raise ValidationError("final replay segments have different public fingerprints")
    for replay in replays.values():
        if set(replay.participants) != {LIGHT_PLAYER, DARK_PLAYER}:
            raise ValidationError(
                f"replay {replay.path.name} participant identity mismatch"
            )
        if replay.winner != row.winner or replay.loser != row.loser:
            raise ValidationError(f"replay {replay.path.name} terminal identity mismatch")
        if replay.win_reason != row.win_reason or replay.lose_reason != row.lose_reason:
            raise ValidationError(f"replay {replay.path.name} terminal reason mismatch")
        if (
            replay.win_reason != NATURAL_WIN_REASON
            or replay.lose_reason != NATURAL_LOSE_REASON
        ):
            raise ValidationError(f"replay {replay.path.name} is not a natural terminal")


def month_keys(start: datetime, end: datetime) -> tuple[str, ...]:
    cursor = datetime(start.year, start.month, 1, tzinfo=timezone.utc)
    last = datetime(end.year, end.month, 1, tzinfo=timezone.utc)
    keys: list[str] = []
    while cursor <= last:
        keys.append(cursor.strftime("%Y-%m"))
        if cursor.month == 12:
            cursor = datetime(cursor.year + 1, 1, 1, tzinfo=timezone.utc)
        else:
            cursor = datetime(cursor.year, cursor.month + 1, 1, tzinfo=timezone.utc)
    return tuple(keys)


def candidate_log_files(repo_root: Path, row: GameRow) -> tuple[Path, ...]:
    log_root = repo_root / "logs"
    files: list[Path] = []
    current = log_root / "gemp-swccg.log"
    if current.is_file():
        files.append(current)
    for key in month_keys(
        row.start_utc - timedelta(days=1), row.end_utc + timedelta(days=1)
    ):
        month_dir = log_root / key
        if month_dir.is_dir():
            files.extend(sorted(month_dir.glob("app-*.log.gz")))
    unique: list[Path] = []
    seen: set[Path] = set()
    for path in files:
        resolved = path.resolve()
        if resolved not in seen:
            seen.add(resolved)
            unique.append(path)
    if not unique:
        raise ValidationError("no current or rotated logs were available")
    return tuple(unique)


def iter_log_lines(path: Path):
    opener = gzip.open if path.suffix == ".gz" else open
    timestamp = ""
    try:
        with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
            for line_number, raw in enumerate(handle, start=1):
                line = raw.rstrip("\n")
                match = LOG_TS_RE.match(line)
                if match:
                    timestamp = f"{match.group(1)}.{match.group(2)}"
                yield path, line_number, timestamp, line
    except OSError as exc:
        raise ValidationError(f"log read failed for {path}") from exc


def collect_controller_anchors(
    repo_root: Path,
    row: GameRow,
    game_id: str,
    config: Config,
) -> tuple[tuple[LogAnchor, ...], tuple[Path, ...]]:
    if not GAME_ID_RE.fullmatch(game_id):
        raise ValidationError("runtime gameId is malformed")
    files = candidate_log_files(repo_root, row)
    logical_lines: list[tuple[Path, int, str, str]] = []
    first_source: dict[tuple[str, str], Path] = {}
    for path in files:
        for source, line_number, timestamp, line in iter_log_lines(path):
            if game_id not in line:
                continue
            key = (timestamp, line)
            resolved = source.resolve()
            if key in first_source and first_source[key] != resolved:
                continue
            first_source.setdefault(key, resolved)
            logical_lines.append((source, line_number, timestamp, line))

    marker = f"BOTGAME AI REGISTERED gameId={game_id} "
    controller_lines = [entry for entry in logical_lines if marker in entry[3]]
    if len(controller_lines) != 2:
        raise ValidationError(
            f"logs contain {len(controller_lines)} controller registrations for the exact gameId; expected two"
        )

    expected = (
        (
            (
                "side=LIGHT",
                f"playerId={LIGHT_PLAYER}",
                f"controllerClass={LIGHT_CONTROLLER}",
                f"deckOwner={config.deck_owner}",
            ),
            config.light_deck,
        ),
        (
            (
                "side=DARK",
                f"playerId={DARK_PLAYER}",
                f"controllerClass={DARK_CONTROLLER}",
                f"deckOwner={config.deck_owner}",
            ),
            config.dark_deck,
        ),
    )
    chosen: list[tuple[Path, int, str, str]] = []
    for tokens, deck_name in expected:
        matches = [
            entry
            for entry in controller_lines
            if all(token in entry[3] for token in tokens)
            and entry[3].endswith(f"deck={deck_name}")
        ]
        if len(matches) != 1:
            raise ValidationError("exact controller identity log anchor is missing")
        chosen.append(matches[0])

    abort_tokens = (
        f"All-AI game {game_id} aborted:",
        f"Game {game_id} aborted:",
    )
    failures = [
        entry
        for entry in logical_lines
        if any(token in entry[3] for token in abort_tokens)
        or (game_id in entry[3] and " ERROR " in entry[3])
    ]
    if failures:
        raise ValidationError("logs contain an abort or error for the exact gameId")

    anchors = tuple(
        LogAnchor(
            path=path,
            line_number=line_number,
            timestamp=timestamp,
            line=line,
            line_sha256=hashlib.sha256(line.encode("utf-8")).hexdigest(),
        )
        for path, line_number, timestamp, line in chosen
    )
    return anchors, files


def perform_validation(
    config: Config,
    http_client: AuthenticatedGempClient | None = None,
    database: ReadOnlyGameHistory | None = None,
    sleeper: Callable[[float], None] = time.sleep,
    now_ms: Callable[[], int] | None = None,
    monotonic: Callable[[], float] = time.monotonic,
    journal: dict[str, object] | None = None,
) -> dict[str, object]:
    """Execute the one-shot contract and return a complete PASS packet."""
    clock = now_ms or (lambda: time.time_ns() // 1_000_000)
    http = http_client or AuthenticatedGempClient(config)
    db = database or ReadOnlyGameHistory(config.db_container)
    packet = {} if journal is None else journal
    packet.update(
        {
            "contract": "chosen-rando-botgame-evidence-v1",
            "status": "RUNNING",
            "request": config.public_request(),
            "botgame_post_count": 0,
        }
    )

    http.login()
    packet["authenticated"] = True

    first_time = clock()
    first_monotonic = monotonic()
    first_hall = http.hall()
    require_clear_hall(first_hall, "Hall check 1")
    packet["hall_check_1"] = {
        "checked_utc": utc_iso(first_time),
        **first_hall.summary(),
    }

    sleeper(HALL_GAP_SECONDS)

    second_monotonic = monotonic()
    second_time = clock()
    if second_monotonic - first_monotonic < HALL_GAP_SECONDS:
        raise ValidationError("Hall checks were less than 15 seconds apart")
    second_hall = http.hall()
    require_clear_hall(second_hall, "Hall check 2")
    packet["hall_check_2"] = {
        "checked_utc": utc_iso(second_time),
        "gap_seconds": second_monotonic - first_monotonic,
        **second_hall.summary(),
    }

    high_water = db.high_water_mark()
    packet["game_history_high_water_mark"] = high_water
    packet["game_history_high_water_checked_utc"] = utc_iso(clock())

    post_started_ms = clock()
    packet["post_started_utc"] = utc_iso(post_started_ms)
    packet["botgame_post_count"] = 1
    game_id = http.post_botgame_once()
    response_received_ms = clock()
    packet["post_response_received_utc"] = utc_iso(response_received_ms)
    packet["runtime_game_id"] = game_id
    actual_count = getattr(http, "botgame_post_count", 1)
    if actual_count != 1:
        raise ValidationError("botgame POST count was not exactly one")

    post_hall = http.hall()
    require_clear_hall(post_hall, "post-completion Hall check")
    packet["post_completion_hall"] = {
        "checked_utc": utc_iso(clock()),
        **post_hall.summary(),
    }

    new_rows = db.rows_after(high_water)
    packet["new_game_history_row_count"] = len(new_rows)
    row = resolve_game_row(
        new_rows, config, post_started_ms, response_received_ms
    )
    packet["game_history"] = row.report_dict()

    replay_paths = resolve_replay_paths(config.repo_root, row)
    replays = {
        owner: parse_replay(path, owner)
        for owner, path in replay_paths.items()
    }
    validate_replay_pair(row, replays)
    packet["replays"] = {
        owner: replays[owner].report_dict()
        for owner in (LIGHT_PLAYER, DARK_PLAYER)
    }

    anchors, log_files = collect_controller_anchors(
        config.repo_root, row, game_id, config
    )
    packet["logs"] = {
        "files_scanned": [str(path) for path in log_files],
        "controller_anchors": [anchor.report_dict() for anchor in anchors],
    }
    packet["verified_invariants"] = [
        "authenticated admin session",
        "zero WAITING and zero PLAYING tables twice at least 15 seconds apart",
        "read-only game_history high-water mark before POST",
        "exactly one POST to /admin/botgame",
        "CHOSENONE Light and RANDO Dark exact request",
        "zero active Hall tables after synchronous response",
        "exactly one new participant, deck, and time-matched game_history row",
        "natural Life Force depletion terminal",
        "recording-ID replay resolution without mtime",
        "matching final-segment public replay fingerprints",
        "exact gameId controller registration anchors",
        "no exact-game abort or error log",
    ]
    packet["status"] = "PASS"
    return packet


def write_packet(report_dir: Path, packet: dict[str, object]) -> Path:
    report_dir.mkdir(parents=True, exist_ok=True)
    timestamp = datetime.now(timezone.utc).strftime("%Y%m%d-%H%M%S-%fZ")
    status = str(packet.get("status", "FAIL"))
    game_id = str(packet.get("runtime_game_id", "no-game-id"))
    safe_game_id = re.sub(r"[^A-Za-z0-9-]", "_", game_id)
    destination = report_dir / f"{timestamp}-{status}-{safe_game_id}.json"
    with destination.open("x", encoding="utf-8") as handle:
        json.dump(packet, handle, indent=2, sort_keys=True)
        handle.write("\n")
    return destination


def main() -> int:
    packet: dict[str, object] = {
        "contract": "chosen-rando-botgame-evidence-v1",
        "status": "FAIL",
        "started_utc": datetime.now(timezone.utc).isoformat(),
    }
    report_dir = DEFAULT_REPORT_DIR
    try:
        config = Config.from_environment()
        report_dir = config.report_dir
        perform_validation(config, journal=packet)
    except ValidationError as exc:
        packet["status"] = "FAIL"
        packet["error"] = str(exc)
        packet["retry_instruction"] = "Do not retry this run automatically."
    except KeyboardInterrupt:
        packet["status"] = "FAIL"
        packet["error"] = "operator interrupted the run; server completion is unknown"
        packet["retry_instruction"] = "Do not retry this run automatically."
    except Exception as exc:  # Defensive fail-closed boundary for the operator tool.
        packet["status"] = "FAIL"
        packet["error"] = f"unexpected {type(exc).__name__}; details withheld"
        packet["retry_instruction"] = "Do not retry this run automatically."

    packet["finished_utc"] = datetime.now(timezone.utc).isoformat()
    try:
        destination = write_packet(report_dir, packet)
    except OSError:
        print("ERROR: evidence packet could not be written safely", file=sys.stderr)
        return 2

    print(json.dumps(packet, indent=2, sort_keys=True))
    print(f"evidence_packet={destination}")
    return 0 if packet.get("status") == "PASS" else 2


if __name__ == "__main__":
    sys.exit(main())
