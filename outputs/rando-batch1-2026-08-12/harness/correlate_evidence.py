#!/usr/bin/env python3
"""Create one fail-closed evidence packet for an exact game_history id.

The database query is SELECT-only and runs inside a READ ONLY transaction.
Configured container environment variables supply credentials. Their values are
never copied into this process' argument list, report, or error text.
"""

from __future__ import annotations

import argparse
import gzip
import hashlib
import html
import os
from dataclasses import dataclass
from datetime import datetime, timedelta, timezone
from pathlib import Path
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
import zlib


REPO_ROOT = Path(__file__).resolve().parents[3]
REPORT_DIR = Path(__file__).resolve().parents[1] / "evidence_reports"
OWNERS = ("asdf", "~Rando_Cal")
START_MARKER = "You're starting a game"
LOG_BEFORE = timedelta(minutes=2)
LOG_AFTER = timedelta(minutes=2)
START_TOLERANCE = timedelta(seconds=30)

RECORDING_ID_RE = re.compile(r"[A-Za-z0-9]{8,64}")
LABEL_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9._-]{0,79}")
CONTAINER_RE = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")
LOG_TS_RE = re.compile(
    r"^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}),(\d{3})"
)
RANDO_START_RE = re.compile(r"New game started vs asdf as DARK")
ANY_AI_START_RE = re.compile(r"New game started vs .+ as (?:DARK|LIGHT)\b")
CARD_HINT_RE = re.compile(r"<div class='cardHint'[^>]*value='([^']+)'[^>]*>(.*?)</div>")
PLAYERS_RE = re.compile(r"^Players in the game are: (.*)$")
WINNER_RE = re.compile(r"^(.*?) is the winner due to: (.*)$")
LOSER_RE = re.compile(r"^(.*?) lost due to: (.*)$")

TAGS = (
    ("B1-PERSIST", re.compile(
        r"deploy-persistent-response-selected|"
        r"Selected executable response to a two-turn drain lane; target=")),
    ("B1-CRITICAL", re.compile(
        r"deploy-objective-critical-eviction-selected|"
        r"Selected executable response clears a typed objective-critical location; target=")),
    ("B1-FAILCLOSED", re.compile(r"Persistent response ledger reset fail-closed")),
    ("V166-DEPLOY", re.compile(r"V166 CONTEST DRAIN \(deploy\)")),
    ("V171-CONTACT", re.compile(r"V171 DEPLOY TO CONTACT")),
    ("FS-SAFETY", re.compile(r"FORMATION SAFETY")),
    ("WMAOP.DEPLOY_ONLY", re.compile(r"WMAOP\.DEPLOY_ONLY")),
    ("WMAOP.BLOCKADE_NEGATIVE", re.compile(
        r"WMAOP\.BLOCKADE_ONLY: only the Blockade Flagship site pull is sanctioned|"
        r"WMAOP\.BLOCKADE_ONLY: .* is not the Blockade Flagship site|"
        r"WMAOP\.BLOCKADE_ONLY: non-Blockade candidate offered by a WMAOP search")),
    ("WMAOP.BLOCKADE_POSITIVE", re.compile(
        r"WMAOP\.BLOCKADE_ONLY: prefer the Blockade Flagship site")),
    ("WMAOP.LIVE_HOLD", re.compile(r"WMAOP\.LIVE_HOLD")),
    ("WMAOP.FODDER_HOLD", re.compile(r"WMAOP\.FODDER_HOLD")),
    ("V76 BATTLE PREDICT", re.compile(r"V76 BATTLE PREDICT")),
    ("V76 PREDICTOR CONFIDENT", re.compile(r"V76 PREDICTOR CONFIDENT")),
    ("L2 WAIVED", re.compile(r"L2 WAIVED")),
    ("ATTACK CANDIDATE", re.compile(r"ATTACK CANDIDATE")),
    ("V96 CONCENTRATE", re.compile(r"V96 CONCENTRATE")),
    ("V136 DOMINANCE", re.compile(
        r"V136 .*DOMINANCE PASS|V136 unified deploy-site score")),
    ("V29 PASSENGER ABOARD", re.compile(r"V29 PASSENGER ABOARD")),
)


class EvidenceError(RuntimeError):
    """A fail-closed evidence identity or input error."""


@dataclass(frozen=True)
class GameRow:
    game_id: int
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

    @property
    def asdf_side(self) -> str:
        side = self.winner_side.upper()
        if self.winner == "asdf":
            return side
        if self.loser == "asdf" and side in {"LIGHT", "DARK"}:
            return "DARK" if side == "LIGHT" else "LIGHT"
        return ""


@dataclass(frozen=True)
class ReplayEvidence:
    owner: str
    recording_id: str
    path: Path
    messages: tuple[str, ...]
    raw_message_count: int
    segment_count: int
    repeated_message_count: int
    public_fingerprint: str
    participants: tuple[str, ...]
    terminal_winner: str
    terminal_loser: str
    terminal_win_reason: str
    terminal_lose_reason: str


@dataclass(frozen=True)
class LogRecord:
    timestamp: datetime
    path: Path
    line_number: int
    line: str


@dataclass(frozen=True)
class LogSlice:
    records: tuple[LogRecord, ...]
    files: tuple[Path, ...]
    lower_bound: datetime
    upper_bound: datetime
    game_start: LogRecord


def query_game_history(game_id: int, container: str) -> GameRow:
    """Read one exact game row without exposing configured credentials."""
    if game_id <= 0:
        raise EvidenceError("game id must be a positive integer")
    if not CONTAINER_RE.fullmatch(container):
        raise EvidenceError("database container name is invalid")

    sql = f"""START TRANSACTION READ ONLY;
SELECT id,winner,loser,COALESCE(win_reason,''),COALESCE(lose_reason,''),
       COALESCE(win_recording_id,''),COALESCE(lose_recording_id,''),
       start_date,COALESCE(end_date,0),COALESCE(winner_side,''),
       COALESCE(format_name,''),COALESCE(winner_deck_name,''),
       COALESCE(loser_deck_name,'')
FROM game_history
WHERE id={game_id};
ROLLBACK;
"""
    credential_command = (
        'test -n "$MYSQL_USER" && test -n "$MYSQL_PASSWORD" '
        '&& test -n "$MYSQL_DATABASE" || exit 91; '
        'exec mariadb -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" '
        '"$MYSQL_DATABASE" --batch --raw --skip-column-names'
    )
    try:
        proc = subprocess.run(
            ["docker", "exec", "-i", container, "sh", "-c", credential_command],
            input=sql,
            text=True,
            capture_output=True,
            check=False,
            timeout=15,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise EvidenceError("read-only game_history query could not start") from exc
    if proc.returncode != 0:
        # Do not relay stderr. A client or shell error could include environment data.
        raise EvidenceError(
            f"read-only game_history query failed with exit {proc.returncode}; details withheld"
        )

    rows = [line for line in proc.stdout.splitlines() if line.strip()]
    if len(rows) != 1:
        raise EvidenceError(
            f"game_history id {game_id} resolved to {len(rows)} rows; expected exactly one"
        )
    fields = rows[0].split("\t")
    if len(fields) != 13:
        raise EvidenceError("game_history row shape did not match the evidence contract")
    try:
        row = GameRow(
            game_id=int(fields[0]),
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
    except ValueError as exc:
        raise EvidenceError("game_history numeric fields were malformed") from exc

    validate_game_row(row, game_id)
    return row


def validate_game_row(row: GameRow, expected_id: int) -> None:
    if row.game_id != expected_id:
        raise EvidenceError("database returned a different game id")
    if {row.winner, row.loser} != set(OWNERS):
        raise EvidenceError("game participants are not exactly asdf and ~Rando_Cal")
    if row.asdf_side != "LIGHT":
        raise EvidenceError("orientation mismatch: controlled evidence requires Steve Light")
    if row.start_ms <= 0 or row.end_ms < row.start_ms:
        raise EvidenceError("game_history start/end bounds are missing or invalid")
    if row.end_ms == 0:
        raise EvidenceError("game_history row has no terminal end bound")
    if not row.win_reason.strip() or not row.lose_reason.strip():
        raise EvidenceError("game_history terminal reasons are missing")
    try:
        row.start_utc
        row.end_utc
    except (OSError, OverflowError, ValueError) as exc:
        raise EvidenceError("game_history start/end bounds are out of range") from exc
    ids = (row.win_recording_id, row.lose_recording_id)
    if ids[0] == ids[1]:
        raise EvidenceError("win and lose recording ids are identical")
    for recording_id in ids:
        if not RECORDING_ID_RE.fullmatch(recording_id):
            raise EvidenceError("recording id is missing or malformed")


def resolve_replay_paths(root: Path, row: GameRow) -> dict[str, Path]:
    """Resolve each DB recording id to one exact participant-owned local file."""
    replay_root = root / "replays"
    by_owner = {
        row.winner: row.win_recording_id,
        row.loser: row.lose_recording_id,
    }
    resolved: dict[str, Path] = {}
    for owner, recording_id in by_owner.items():
        expected = replay_root / owner / f"{recording_id}.xml.gz"
        matches = sorted(replay_root.glob(f"*/{recording_id}.xml.gz"))
        if len(matches) != 1:
            raise EvidenceError(
                f"recording {recording_id} resolved to {len(matches)} local files; expected one"
            )
        if matches[0].resolve() != expected.resolve():
            raise EvidenceError(
                f"recording {recording_id} is not in its DB participant directory"
            )
        if not expected.is_file():
            raise EvidenceError(f"recording {recording_id} is missing locally")
        resolved[owner] = expected
    if set(resolved) != set(OWNERS):
        raise EvidenceError("both participant replay paths were not resolved")
    return resolved


def inflate_replay(path: Path) -> bytes:
    try:
        data = path.read_bytes()
    except OSError as exc:
        raise EvidenceError(f"replay read failed for {path.name}") from exc
    for wbits in (zlib.MAX_WBITS | 32, zlib.MAX_WBITS, -zlib.MAX_WBITS):
        try:
            return zlib.decompress(data, wbits)
        except zlib.error:
            continue
    raise EvidenceError(f"replay decompression failed for {path.name}")


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
        raise EvidenceError(f"replay XML parse failed for {path.name}") from exc

    raw_messages = [
        node.get("message", "")
        for node in root.iter("ge")
        if node.get("type") == "M"
    ]
    starts = [
        index for index, message in enumerate(raw_messages)
        if html.unescape(message).startswith(START_MARKER)
    ]
    if not starts:
        raise EvidenceError(f"replay {path.name} has no game-start segment marker")
    final_messages = raw_messages[starts[-1]:]
    cleaned = tuple(clean_message(message) for message in final_messages)
    fingerprint = hashlib.sha256("\n".join(cleaned).encode("utf-8")).hexdigest()

    participants: tuple[str, ...] = ()
    terminal_winner = ""
    terminal_loser = ""
    terminal_win_reason = ""
    terminal_lose_reason = ""
    for message in cleaned:
        players = PLAYERS_RE.match(message)
        if players:
            participants = tuple(sorted(
                part.strip() for part in players.group(1).split(",")
            ))
        winner = WINNER_RE.match(message)
        if winner:
            terminal_winner = winner.group(1)
            terminal_win_reason = winner.group(2)
        loser = LOSER_RE.match(message)
        if loser:
            terminal_loser = loser.group(1)
            terminal_lose_reason = loser.group(2)
        if message.startswith("Game was cancelled"):
            raise EvidenceError(f"replay {path.name} is a cancelled game")

    return ReplayEvidence(
        owner=owner,
        recording_id=path.name.removesuffix(".xml.gz"),
        path=path,
        messages=tuple(final_messages),
        raw_message_count=len(raw_messages),
        segment_count=len(starts),
        repeated_message_count=starts[-1],
        public_fingerprint=fingerprint,
        participants=participants,
        terminal_winner=terminal_winner,
        terminal_loser=terminal_loser,
        terminal_win_reason=terminal_win_reason,
        terminal_lose_reason=terminal_lose_reason,
    )


def validate_replay_pair(
    row: GameRow, replays: dict[str, ReplayEvidence]
) -> None:
    if set(replays) != set(OWNERS):
        raise EvidenceError("replay evidence does not contain both participants")
    fingerprints = {evidence.public_fingerprint for evidence in replays.values()}
    if len(fingerprints) != 1:
        raise EvidenceError("final replay segments have different public fingerprints")
    for evidence in replays.values():
        if set(evidence.participants) != set(OWNERS):
            raise EvidenceError(
                f"replay {evidence.path.name} participant message does not match the DB row"
            )
        if evidence.terminal_winner != row.winner:
            raise EvidenceError(
                f"replay {evidence.path.name} terminal winner does not match the DB row"
            )
        if evidence.terminal_loser != row.loser:
            raise EvidenceError(
                f"replay {evidence.path.name} terminal loser does not match the DB row"
            )
        if evidence.terminal_win_reason != row.win_reason:
            raise EvidenceError(
                f"replay {evidence.path.name} winner reason does not match the DB row"
            )
        if evidence.terminal_lose_reason != row.lose_reason:
            raise EvidenceError(
                f"replay {evidence.path.name} loser reason does not match the DB row"
            )


def parse_log_timestamp(line: str) -> datetime | None:
    match = LOG_TS_RE.match(line)
    if not match:
        return None
    return datetime.strptime(
        f"{match.group(1)}.{match.group(2)}", "%Y-%m-%d %H:%M:%S.%f"
    ).replace(tzinfo=timezone.utc)


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


def candidate_log_files(
    root: Path, start: datetime, end: datetime
) -> tuple[Path, ...]:
    log_root = root / "logs"
    files: list[Path] = []
    current = log_root / "gemp-swccg.log"
    if current.is_file():
        files.append(current)
    for key in month_keys(start, end):
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
    return tuple(unique)


def iter_log_records(path: Path):
    opener = gzip.open if path.suffix == ".gz" else open
    current_timestamp: datetime | None = None
    with opener(path, "rt", encoding="utf-8", errors="replace") as handle:
        for line_number, raw_line in enumerate(handle, start=1):
            line = raw_line.rstrip("\n")
            timestamp = parse_log_timestamp(line)
            if timestamp is not None:
                current_timestamp = timestamp
            if current_timestamp is not None:
                yield LogRecord(current_timestamp, path, line_number, line)


def collect_game_log_slice(root: Path, row: GameRow) -> LogSlice:
    lower = row.start_utc - LOG_BEFORE
    upper = row.end_utc + LOG_AFTER
    files = candidate_log_files(root, lower, upper)
    if not files:
        raise EvidenceError("no current or rotated log files cover the DB month")

    records: list[LogRecord] = []
    first_source: dict[tuple[datetime, str], Path] = {}
    for path in files:
        try:
            for record in iter_log_records(path):
                if record.timestamp < lower or record.timestamp > upper:
                    continue
                duplicate_key = (record.timestamp, record.line)
                source = record.path.resolve()
                if (duplicate_key in first_source
                        and first_source[duplicate_key] != source):
                    continue
                first_source.setdefault(duplicate_key, source)
                records.append(record)
        except OSError as exc:
            raise EvidenceError(f"log read failed for {path}") from exc
    records.sort(key=lambda record: (
        record.timestamp, str(record.path), record.line_number
    ))
    if not records:
        raise EvidenceError("no log lines fall inside the DB-bounded time window")

    markers = [record for record in records if RANDO_START_RE.search(record.line)]
    if len(markers) != 1:
        raise EvidenceError(
            f"DB-bounded logs contain {len(markers)} Dark-Rando starts; expected exactly one"
        )
    marker = markers[0]
    if abs(marker.timestamp - row.start_utc) > START_TOLERANCE:
        raise EvidenceError(
            "exact Dark-Rando start is not within 30 seconds of the DB start"
        )
    all_starts = [
        record for record in records if ANY_AI_START_RE.search(record.line)
    ]
    if len(all_starts) != 1:
        raise EvidenceError(
            f"DB-bounded logs contain {len(all_starts)} AI game starts; "
            "one-table evidence requires exactly one"
        )
    marker_index = records.index(marker)
    narrowed = tuple(records[marker_index:])
    if not narrowed:
        raise EvidenceError("log slice is empty after the exact Dark-Rando start marker")
    used_files = tuple(dict.fromkeys(record.path for record in narrowed))
    return LogSlice(narrowed, used_files, lower, upper, marker)


def tag_hits(log_slice: LogSlice) -> dict[str, tuple[LogRecord, ...]]:
    return {
        name: tuple(
            record for record in log_slice.records if pattern.search(record.line)
        )
        for name, pattern in TAGS
    }


def extract_entities(line: str) -> set[str]:
    entities = set(re.findall(r"target=([^#;|]+)#\d+", line))
    entities.update(re.findall(r"'([^']{4,80})'", line))
    entities.update(re.findall(
        r"(?: at |target=)([A-Z][^|;#]{3,80})", line
    ))
    return {entity.strip() for entity in entities if entity.strip()}


def correlate_messages(
    entities: set[str], messages: tuple[str, ...], cap: int = 5
) -> list[tuple[int, str]]:
    matches: list[tuple[int, str]] = []
    for index, message in enumerate(messages):
        cleaned = clean_message(message)
        if any(entity in cleaned for entity in entities):
            matches.append((index, cleaned))
            if len(matches) >= cap:
                break
    return matches


def outcome_class(reason: str) -> str:
    lower = reason.lower()
    if "timeout" in lower:
        return "decision_timeout_noncompetitive"
    if "conced" in lower:
        return "concession_noncompetitive"
    if "life force" in lower:
        return "life_force_depleted"
    return "completed_other"


def markdown_safe(value: object) -> str:
    return str(value).replace("|", "\\|").replace("\n", " ")


def render_report(
    label: str,
    row: GameRow,
    replays: dict[str, ReplayEvidence],
    log_slice: LogSlice,
) -> str:
    now = datetime.now(timezone.utc)
    reference = replays["~Rando_Cal"]
    hits = tag_hits(log_slice)
    terminal_class = outcome_class(reference.terminal_win_reason)

    lines = [
        f"# Controlled evidence packet: {label}",
        "",
        f"Generated: `{now.isoformat()}`",
        "",
        "Identity status: **EVIDENCE_IDENTITY_VALIDATED**",
        "",
        "This packet validates DB, replay, and log identity. It does not by itself prove a scenario opportunity gate or PASS.",
        "",
        "## Database row",
        "",
        "| field | value |",
        "|---|---|",
        f"| game_history.id | `{row.game_id}` |",
        f"| participants | `{row.winner}` / `{row.loser}` |",
        f"| orientation | Steve `{row.asdf_side}`, Rando `DARK` |",
        f"| winner | `{row.winner}` |",
        f"| terminal class | `{terminal_class}` |",
        f"| winner reason | {markdown_safe(reference.terminal_win_reason)} |",
        f"| loser reason | {markdown_safe(reference.terminal_lose_reason)} |",
        f"| start UTC | `{row.start_utc.isoformat()}` |",
        f"| end UTC | `{row.end_utc.isoformat()}` |",
        f"| format | {markdown_safe(row.format_name)} |",
        f"| winner deck | {markdown_safe(row.winner_deck_name)} |",
        f"| loser deck | {markdown_safe(row.loser_deck_name)} |",
        "",
        "A timeout or concession may preserve earlier branch evidence, but it is never a natural Rando result.",
        "",
        "## Exact replay streams",
        "",
        "| owner | recording id | path | raw M | segments | discarded resend M | final public SHA-256 |",
        "|---|---|---|---:|---:|---:|---|",
    ]
    for owner in OWNERS:
        evidence = replays[owner]
        lines.append(
            f"| `{owner}` | `{evidence.recording_id}` | `{evidence.path}` | "
            f"{evidence.raw_message_count} | {evidence.segment_count} | "
            f"{evidence.repeated_message_count} | `{evidence.public_fingerprint}` |"
        )

    lines.extend([
        "",
        "Both final segments have the same public fingerprint and terminal result.",
        "",
        "## DB-bounded log slice",
        "",
        "| field | value |",
        "|---|---|",
        f"| lower bound | `{log_slice.lower_bound.isoformat()}` |",
        f"| upper bound | `{log_slice.upper_bound.isoformat()}` |",
        f"| exact Dark-Rando start | `{log_slice.game_start.path}:{log_slice.game_start.line_number}` |",
        f"| retained lines | {len(log_slice.records)} |",
        f"| source files | {markdown_safe(', '.join(str(path) for path in log_slice.files))} |",
        "",
        "## Tag summary",
        "",
        "| tag | count |",
        "|---|---:|",
    ])
    for name, _ in TAGS:
        lines.append(f"| {name} | {len(hits[name])} |")

    for name, _ in TAGS:
        records = hits[name]
        if not records:
            continue
        lines.extend(["", f"### {name}: {len(records)} hit(s)", "", "```text"])
        for record in records[:30]:
            lines.append(f"{record.path}:{record.line_number}: {record.line}")
        if len(records) > 30:
            lines.append(f"... {len(records) - 30} additional hits")
        lines.append("```")

        entities: set[str] = set()
        for record in records:
            entities.update(extract_entities(record.line))
        correlations = correlate_messages(entities, reference.messages)
        if correlations:
            lines.extend(["", "Correlated final-segment replay messages:", ""])
            for index, message in correlations:
                lines.append(f"- `msg[{index}]`: {message[:240]}")

    lines.extend([
        "",
        "## Required manual attachment",
        "",
        "Attach the scenario opportunity packet, exact decision window, selected Best action and Reasoning pair, and replay consequence before assigning PASS or FAIL.",
        "",
    ])
    return "\n".join(lines)


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Correlate one exact game_history id with its DB recordings and logs."
    )
    parser.add_argument("--game-id", required=True, type=int)
    parser.add_argument("--label", required=True)
    parser.add_argument(
        "--db-container",
        default=os.environ.get("GEMP_DB_CONTAINER", "gemp_swccg_db_1"),
    )
    parser.add_argument(
        "--no-write",
        action="store_true",
        help="print the validated packet without writing evidence_reports",
    )
    args = parser.parse_args(argv)
    if not LABEL_RE.fullmatch(args.label):
        parser.error("--label must use only letters, digits, dot, underscore, or hyphen")
    return args


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    root = Path(os.environ.get("GEMP_ROOT", str(REPO_ROOT))).resolve()
    try:
        row = query_game_history(args.game_id, args.db_container)
        replay_paths = resolve_replay_paths(root, row)
        replays = {
            owner: parse_replay(path, owner)
            for owner, path in replay_paths.items()
        }
        validate_replay_pair(row, replays)
        log_slice = collect_game_log_slice(root, row)
        report = render_report(args.label, row, replays, log_slice)
    except EvidenceError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    destination: Path | None = None
    if not args.no_write:
        try:
            REPORT_DIR.mkdir(parents=True, exist_ok=True)
            timestamp = datetime.now(timezone.utc).strftime(
                "%Y%m%d-%H%M%S-%fZ"
            )
            destination = REPORT_DIR / f"{row.game_id}-{args.label}-{timestamp}.md"
            with destination.open("x", encoding="utf-8") as handle:
                handle.write(report + "\n")
        except OSError:
            print("ERROR: evidence report could not be written safely", file=sys.stderr)
            return 2

    print(report)
    if destination is not None:
        print(f"\n[report written to {destination}]")
    return 0


if __name__ == "__main__":
    sys.exit(main())
