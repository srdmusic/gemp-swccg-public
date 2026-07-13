#!/usr/bin/env python3
"""Compare two decision-fixture JSONL files (baseline vs shadow) for parity.

Alignment: records with source=="log" are grouped by game id and aligned by
position within each game. A decisionType/phase mismatch at an aligned slot is
reported as MISALIGNED and further checks for that pair are skipped.

Divergence kinds:
  WINNER      chosen action differs
  TOP5-SET    top-5 candidate id sets differ
  VETO-COUNT  number of veto lines differs
  SCORE-DRIFT |baseline - shadow| > tolerance for an actionId present in both top-5s
  MISALIGNED  decisionType/phase differ at the same slot (sequence skew)
  MISSING     baseline record with no shadow counterpart (and vice versa: EXTRA)

Exit codes: 0 = parity, 1 = divergences found, 2 = usage/load error.

Usage:
  python3 compare_fixtures.py baseline.jsonl shadow.jsonl [--tolerance 0.01] [--max-rows 50]
"""

import argparse
import json
import sys
from collections import OrderedDict


def load(path):
    games = OrderedDict()
    try:
        with open(path, "r", encoding="utf-8") as fh:
            for lineno, line in enumerate(fh, 1):
                line = line.strip()
                if not line:
                    continue
                try:
                    rec = json.loads(line)
                except json.JSONDecodeError as e:
                    print("%s:%d: bad JSON: %s" % (path, lineno, e), file=sys.stderr)
                    return None
                if rec.get("source") != "log":
                    continue
                games.setdefault(rec.get("game", "?"), []).append(rec)
    except OSError as e:
        print("cannot read %s: %s" % (path, e), file=sys.stderr)
        return None
    return games


def top5_ids(rec):
    return [aid for aid, _ in rec.get("top5", [])]


def compare_pair(b, s, tolerance):
    """Return list of (kind, detail) divergences for one aligned pair."""
    if b.get("decisionType") != s.get("decisionType") or b.get("phase") != s.get("phase"):
        return [("MISALIGNED", "%s/%s vs %s/%s" % (
            b.get("decisionType"), b.get("phase"),
            s.get("decisionType"), s.get("phase")))]

    divs = []
    if b.get("chosen") != s.get("chosen"):
        divs.append(("WINNER", "'%s' vs '%s'" % (b.get("chosen"), s.get("chosen"))))

    b_ids, s_ids = set(top5_ids(b)), set(top5_ids(s))
    if b_ids != s_ids:
        only_b = sorted(b_ids - s_ids)
        only_s = sorted(s_ids - b_ids)
        divs.append(("TOP5-SET", "base-only=%s shadow-only=%s" % (only_b, only_s)))

    bv, sv = b.get("vetoCount", 0), s.get("vetoCount", 0)
    if bv != sv:
        divs.append(("VETO-COUNT", "%d vs %d" % (bv, sv)))

    b_scores = dict(b.get("top5", []))
    s_scores = dict(s.get("top5", []))
    worst = None
    for aid in b_ids & s_ids:
        delta = abs(b_scores[aid] - s_scores[aid])
        if delta > tolerance and (worst is None or delta > worst[1]):
            worst = (aid, delta, b_scores[aid], s_scores[aid])
    if worst is not None:
        divs.append(("SCORE-DRIFT", "id='%s' base=%s shadow=%s (delta=%.3f)" %
                     (worst[0], worst[2], worst[3], worst[1])))
    return divs


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("baseline")
    ap.add_argument("shadow")
    ap.add_argument("--tolerance", type=float, default=0.01,
                    help="allowed absolute score drift per actionId (default 0.01)")
    ap.add_argument("--max-rows", type=int, default=50,
                    help="max divergence rows to print (default 50)")
    args = ap.parse_args(argv)

    base = load(args.baseline)
    shad = load(args.shadow)
    if base is None or shad is None:
        return 2

    rows = []        # (seq, game, dtype, phase, kind, detail)
    kind_counts = {}
    compared = 0

    def add(rec, kind, detail):
        kind_counts[kind] = kind_counts.get(kind, 0) + 1
        rows.append((rec.get("seq"), str(rec.get("game", "?"))[:12],
                     rec.get("decisionType", "?"), rec.get("phase", "?"),
                     kind, detail))

    all_games = list(base.keys()) + [g for g in shad.keys() if g not in base]
    # If the two runs used different game ids but the same number of games,
    # fall back to pairing games by order.
    if not (set(base) & set(shad)) and len(base) == len(shad) and base:
        game_pairs = list(zip(base.keys(), shad.keys()))
        print("note: no shared game ids; pairing %d game(s) by order"
              % len(game_pairs), file=sys.stderr)
    else:
        game_pairs = [(g, g) for g in all_games]

    for bg, sg in game_pairs:
        b_recs = base.get(bg, [])
        s_recs = shad.get(sg, [])
        for i in range(max(len(b_recs), len(s_recs))):
            if i >= len(s_recs):
                add(b_recs[i], "MISSING", "no shadow record at slot %d of game %s" % (i, sg))
                continue
            if i >= len(b_recs):
                add(s_recs[i], "EXTRA", "no baseline record at slot %d of game %s" % (i, bg))
                continue
            compared += 1
            for kind, detail in compare_pair(b_recs[i], s_recs[i], args.tolerance):
                add(b_recs[i], kind, detail)

    if rows:
        header = ("seq", "game", "type", "phase", "kind", "detail")
        widths = [5, 12, 20, 16, 11]
        fmt = "%-5s %-12s %-20s %-16s %-11s %s"
        print(fmt % header)
        print(fmt % tuple("-" * w for w in widths + [30]))
        for row in rows[: args.max_rows]:
            print(fmt % tuple(str(c) for c in row))
        if len(rows) > args.max_rows:
            print("... %d more row(s) suppressed (--max-rows)" % (len(rows) - args.max_rows))

    print("", file=sys.stderr)
    print("compared %d aligned pair(s) across %d game pairing(s)"
          % (compared, len(game_pairs)), file=sys.stderr)
    if kind_counts:
        for kind in sorted(kind_counts):
            print("  %-11s %d" % (kind, kind_counts[kind]), file=sys.stderr)
        print("DIVERGENT", file=sys.stderr)
        return 1
    print("PARITY: no divergences", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
