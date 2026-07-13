#!/usr/bin/env python3
"""Extract AI decision fixtures from GEMP-SWCCG server logs (and optionally replays).

One JSONL record per decide() call: what the engine asked, what the evaluators
scored (V191 top-5), what safety vetoes/clamps fired, and what the bot answered.
These fixtures are the parity baseline for a shadow implementation.

Usage:
  python3 extract_fixtures.py LOG [LOG...] [--replay REPLAY.xml.gz ...] [-o OUT.jsonl]

LOG may be a plain log file or a rotated .gz (logs/2026-07/app-*.log.gz).
Stdlib only. Summary goes to stderr; records go to stdout unless -o is given.
"""

import argparse
import gzip
import json
import re
import sys
import zlib
from collections import Counter, OrderedDict

# ---------------------------------------------------------------------------
# Log line patterns (verified against logs/gemp-swccg.stuck-173119.log 2026-07-12
# and src/.../ai/models/{rando,chosenone}/ log statements)
# ---------------------------------------------------------------------------

LINE_RE = re.compile(
    r'^(?P<ts>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2},\d{3})\s+'
    r'(?P<level>TRACE|DEBUG|INFO|WARN|ERROR|FATAL)\s+'
    r'(?P<logger>.+?):(?P<lineno>\d+) - (?P<msg>.*)$'
)

# "[RandoCalAi] decide() called: type=MULTIPLE_CHOICE, phase=PLAY_STARTING_CARDS, text='...'"
# (chosenone bot logs the same shape with [TheChosenOneAi])
DECIDE_CALLED_RE = re.compile(
    r"\[(?P<bot>\w+)\] decide\(\) called: type=(?P<dtype>[A-Z_]+), "
    r"phase=(?P<phase>[A-Za-z_]+|null), text='(?P<text>.*)'\s*$"
)

# "[RandoCalAi] decide() result: 'temp1' <emoji>"
DECIDE_RESULT_RE = re.compile(
    r"\[(?P<bot>\w+)\] decide\(\) result: '(?P<result>.*)'"
)

# "V191 TOPN: ARBITRARY_CARDS phase=PLAY_STARTING_CARDS :: temp1=960.0, temp2=-40.0"
# or  ":: fallback-heuristic picked='0' (top-5 n/a: pick loop in HeuristicAiBase)"
# Action ids may be EMPTY strings (the pass action logs as "=2.0").
TOPN_RE = re.compile(
    r"V191 TOPN: (?P<dtype>[A-Z_]+) phase=(?P<phase>[A-Za-z_]+|null) :: (?P<payload>.*)$"
)
TOPN_FALLBACK_RE = re.compile(r"fallback-heuristic picked='(?P<picked>.*?)'")

# "Best action: Starting location temp1 (score: 960.0)"
BEST_ACTION_RE = re.compile(r"Best action: (?P<text>.*) \(score: (?P<score>-?[\d.]+)\)\s*$")
REASONING_RE = re.compile(r"^\s*Reasoning: (?P<text>.*)$")

# "Evaluator decision: Starting location temp1 (score: 960.0)"
EVAL_DECISION_RE = re.compile(r"Evaluator decision: (?P<text>.*) \(score: (?P<score>-?[\d.]+)\)\s*$")

# Game boundaries: "System: Welcome to room: Game<hex>" (excludes "Game Hall" -
# the \S+ requires a non-space right after "Game"). Server restart clears state.
ROOM_RE = re.compile(r"System: Welcome to room: (?P<room>Game\S+)")
SERVER_START_MARKER = "GempukkuServer loading prerequisites"
OBJECTIVE_MARKER = "Analyzing objective"  # secondary boundary heuristic

VETO_MARKERS = ("FORMATION SAFETY", "HARD VETO")
CLAMP_MARKERS = ("SAFETY CLAMP",)

TEXT_TRUNC = 200
REASONING_TRUNC = 500
VETO_TRUNC = 300
VETO_LIST_CAP = 40


def _trunc(s, n):
    if s is None:
        return None
    return s if len(s) <= n else s[: n - 3] + "..."


def _open_maybe_gz(path):
    if path.endswith(".gz"):
        return gzip.open(path, "rt", encoding="utf-8", errors="replace")
    return open(path, "rt", encoding="utf-8", errors="replace")


def _parse_topn_payload(payload):
    """Return (top_list, fallback_pick). top_list = [[actionId, score], ...]."""
    m = TOPN_FALLBACK_RE.search(payload)
    if m:
        return [], m.group("picked")
    top = []
    for token in payload.split(", "):
        token = token.strip()
        if not token:
            continue
        # split on the LAST '=' so ids containing '=' (unlikely) survive;
        # empty id before '=' is legal (pass action).
        if "=" not in token:
            continue
        aid, _, score_s = token.rpartition("=")
        try:
            score = float(score_s)
        except ValueError:
            continue
        top.append([aid, score])
    return top, None


class GameTracker:
    """Tracks the 'current game' id from room-welcome lines.

    Limitation: rando/evaluator/strategy log lines carry no game id, so with
    truly concurrent games the latest room-welcome wins (heuristic).
    """

    def __init__(self):
        self.current = None
        self._untracked_n = 0
        self._untracked_active = None

    def on_room(self, room):
        self.current = room[len("Game"):]  # strip "Game" prefix
        self._untracked_active = None

    def on_server_start(self):
        self.current = None
        self._untracked_active = None

    def on_objective_analysis(self):
        # A new objective analysis with no room seen since the last boundary
        # means a game started that we failed to catch (e.g. TRACE disabled).
        if self.current is None:
            self._new_untracked()

    def game_id(self):
        if self.current is not None:
            return self.current
        if self._untracked_active is None:
            self._new_untracked()
        return self._untracked_active

    def _new_untracked(self):
        self._untracked_n += 1
        self._untracked_active = "untracked-%d" % self._untracked_n


def extract_from_log(path, seq_start, emit):
    """Parse one log file; call emit(record) per fixture. Returns next seq."""
    seq = seq_start
    tracker = GameTracker()
    pending = OrderedDict()  # bot name -> open record

    def flush(bot, unterminated):
        rec = pending.pop(bot, None)
        if rec is None:
            return
        if unterminated:
            rec["unterminated"] = True
        _finalize(rec)
        emit(rec)

    def attach_target():
        # Evaluator/strategy lines carry no bot name; attach to the most
        # recently opened pending record (heuristic, fine for 1 bot per game).
        if not pending:
            return None
        return next(reversed(pending.values()))

    with _open_maybe_gz(path) as fh:
        for raw in fh:
            m = LINE_RE.match(raw.rstrip("\n"))
            if not m:
                continue
            msg = m.group("msg")

            if SERVER_START_MARKER in msg:
                for bot in list(pending):
                    flush(bot, unterminated=True)
                tracker.on_server_start()
                continue

            rm = ROOM_RE.search(msg)
            if rm:
                tracker.on_room(rm.group("room"))
                continue

            if OBJECTIVE_MARKER in msg:
                tracker.on_objective_analysis()
                # falls through: objective lines never match the below

            dm = DECIDE_CALLED_RE.search(msg)
            if dm:
                bot = dm.group("bot")
                flush(bot, unterminated=True)  # previous decide never resolved
                seq += 1
                pending[bot] = {
                    "seq": seq,
                    "source": "log",
                    "game": tracker.game_id(),
                    "bot": bot,
                    "ts": m.group("ts"),
                    "decisionType": dm.group("dtype"),
                    "phase": dm.group("phase"),
                    "decisionText": _trunc(dm.group("text"), TEXT_TRUNC),
                    "top5": [],
                    "fallback": None,
                    "topnLines": 0,
                    "bestAction": None,
                    "bestScore": None,
                    "reasoning": None,
                    "evaluatorDecision": None,
                    "evaluatorScore": None,
                    "vetoes": [],
                    "vetoCount": 0,
                    "clamps": [],
                    "chosen": None,
                    "chosenScore": None,
                    "_last_was_best": False,
                }
                continue

            rm2 = DECIDE_RESULT_RE.search(msg)
            if rm2:
                bot = rm2.group("bot")
                rec = pending.get(bot)
                if rec is not None:
                    rec["chosen"] = rm2.group("result")
                    flush(bot, unterminated=False)
                continue

            rec = attach_target()
            if rec is None:
                continue

            tm = TOPN_RE.search(msg)
            if tm:
                top, fb = _parse_topn_payload(tm.group("payload"))
                rec["topnLines"] += 1
                if fb is not None:
                    rec["fallback"] = fb
                else:
                    rec["top5"] = top[:5]
                rec["_last_was_best"] = False
                continue

            bm = BEST_ACTION_RE.search(msg)
            if bm:
                rec["bestAction"] = _trunc(bm.group("text"), TEXT_TRUNC)
                rec["bestScore"] = float(bm.group("score"))
                rec["_last_was_best"] = True
                continue

            if rec["_last_was_best"]:
                rmm = REASONING_RE.match(msg)
                if rmm:
                    rec["reasoning"] = _trunc(rmm.group("text"), REASONING_TRUNC)
                rec["_last_was_best"] = False
                # do not continue: a veto line could theoretically follow Best action

            em = EVAL_DECISION_RE.search(msg)
            if em:
                rec["evaluatorDecision"] = _trunc(em.group("text"), TEXT_TRUNC)
                rec["evaluatorScore"] = float(em.group("score"))
                continue

            if any(k in msg for k in VETO_MARKERS):
                rec["vetoCount"] += 1
                if len(rec["vetoes"]) < VETO_LIST_CAP:
                    rec["vetoes"].append(_trunc(msg, VETO_TRUNC))
                continue

            if any(k in msg for k in CLAMP_MARKERS):
                rec["clamps"].append(_trunc(msg, VETO_TRUNC))
                continue

    for bot in list(pending):
        flush(bot, unterminated=True)
    return seq


def _finalize(rec):
    rec.pop("_last_was_best", None)
    # chosen score: exact top5 match first, evaluator score as fallback
    chosen = rec.get("chosen")
    if chosen is not None:
        for aid, score in rec.get("top5", []):
            if aid == chosen:
                rec["chosenScore"] = score
                break
        else:
            rec["chosenScore"] = rec.get("evaluatorScore")
    # drop always-empty optionals to keep fixtures compact
    for key in ("fallback", "reasoning", "bestAction", "bestScore",
                "evaluatorDecision", "evaluatorScore", "chosenScore"):
        if rec.get(key) is None:
            del rec[key]
    if not rec["vetoes"]:
        del rec["vetoes"]
    if not rec["clamps"]:
        del rec["clamps"]
    if rec["vetoCount"] == 0:
        del rec["vetoCount"]
    if rec["topnLines"] <= 1:
        del rec["topnLines"]


# ---------------------------------------------------------------------------
# Replay parsing (replays/asdf/*.xml.gz = raw zlib-deflated XML, NOT gzip)
# ---------------------------------------------------------------------------

def _read_replay_xml(path):
    data = open(path, "rb").read()
    for wbits in (zlib.MAX_WBITS, -zlib.MAX_WBITS, zlib.MAX_WBITS | 16):
        try:
            return zlib.decompress(data, wbits).decode("utf-8", "replace")
        except zlib.error:
            continue
    raise ValueError("cannot decompress replay: %s" % path)


def extract_from_replay(path, seq_start, emit):
    """Emit engine-side decision records (ge type='D') from a replay.

    NOTE: replays only record decisions presented to the HUMAN client; the
    bot's server-side decide() calls do NOT appear as D elements. These
    records are context / cross-check material, not Rando fixtures.
    """
    import xml.etree.ElementTree as ET
    import os

    seq = seq_start
    game = os.path.basename(path).split(".")[0]
    root = ET.fromstring(_read_replay_xml(path))
    for ge in root.iter("ge"):
        if ge.get("type") != "D":
            continue
        seq += 1
        params = {}
        for p in ge.iter("parameter"):
            params.setdefault(p.get("name"), []).append(p.get("value"))
        rec = {
            "seq": seq,
            "source": "replay",
            "game": game,
            "participant": ge.get("participantId"),
            "decisionType": ge.get("decisionType"),
            "decisionText": _trunc(ge.get("text") or "", TEXT_TRUNC),
            "actionIds": params.get("actionId", [])[:20],
            "actionTexts": [_trunc(t, 120) for t in params.get("actionText", [])[:20]],
            "cardIds": params.get("cardId", [])[:20],
            "noPass": (params.get("noPass") or [None])[0],
        }
        emit(rec)
    return seq


# ---------------------------------------------------------------------------

def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("logs", nargs="+", help="log file(s), plain or .gz")
    ap.add_argument("--replay", action="append", default=[],
                    help="replay .xml.gz (zlib XML); may repeat")
    ap.add_argument("-o", "--out", default=None, help="output JSONL (default stdout)")
    args = ap.parse_args(argv)

    out = open(args.out, "w", encoding="utf-8") if args.out else sys.stdout
    counts = Counter()
    per_source = Counter()

    def emit(rec):
        counts[rec["decisionType"]] += 1
        per_source[rec["source"]] += 1
        out.write(json.dumps(rec, ensure_ascii=False) + "\n")

    seq = 0
    for path in args.logs:
        seq = extract_from_log(path, seq, emit)
    rseq = 0
    for path in args.replay:
        rseq = extract_from_replay(path, rseq, emit)

    total = sum(per_source.values())
    print("extracted %d fixture record(s)" % total, file=sys.stderr)
    for src, n in sorted(per_source.items()):
        print("  source=%s: %d" % (src, n), file=sys.stderr)
    for dtype, n in counts.most_common():
        print("  %-22s %d" % (dtype, n), file=sys.stderr)

    if args.out:
        out.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
