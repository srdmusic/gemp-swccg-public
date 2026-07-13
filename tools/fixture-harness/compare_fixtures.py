#!/usr/bin/env python3
"""Compare two decision-fixture JSONL files (baseline vs shadow) for parity.

Alignment: records with source=="log" are grouped by game id and aligned by
position within each game. A decisionType/phase mismatch at an aligned slot is
reported as MISALIGNED and further checks for that pair are skipped.

Divergence kinds:
  WINNER         chosen action differs
  DECISION-TEXT  engine decisionText differs
  TOP5-SET       top-5 candidate id sets differ
  TOP5-ORDER     same top-5 id set but different sequence (order is behavioral)
  VETO-COUNT     number of veto lines differs
  VETO-REASON    ordered veto reason lists differ (even when counts match)
  SCORE-DRIFT    |baseline - shadow| > tolerance for a top-5 score
                 (default tolerance 0 = EXACT float equality; per Codex review
                 e5b393955, intentional deltas need an explicit --tolerance)
  MISALIGNED     decisionType/phase differ at the same slot (sequence skew)
  MISSING        baseline record with no shadow counterpart (and vice versa: EXTRA)

Exit codes: 0 = parity, 1 = divergences found, 2 = usage/load error.

Usage:
  python3 compare_fixtures.py baseline.jsonl shadow.jsonl [--tolerance 0.001] [--max-rows 50]
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


def _short(s, n=60):
    s = str(s)
    return s if len(s) <= n else s[: n - 3] + "..."


def compare_pair(b, s, tolerance):
    """Return list of (kind, detail) divergences for one aligned pair."""
    if b.get("decisionType") != s.get("decisionType") or b.get("phase") != s.get("phase"):
        return [("MISALIGNED", "%s/%s vs %s/%s" % (
            b.get("decisionType"), b.get("phase"),
            s.get("decisionType"), s.get("phase")))]

    divs = []
    if b.get("chosen") != s.get("chosen"):
        divs.append(("WINNER", "'%s' vs '%s'" % (b.get("chosen"), s.get("chosen"))))

    if b.get("decisionText") != s.get("decisionText"):
        divs.append(("DECISION-TEXT", "'%s' vs '%s'" %
                     (_short(b.get("decisionText")), _short(s.get("decisionText")))))

    # Top-5 candidates: order is behavioral (Codex review e5b393955), so the
    # id SEQUENCE must match, not just the membership set.
    b_top, s_top = b.get("top5", []), s.get("top5", [])
    b_ids, s_ids = top5_ids(b), top5_ids(s)
    if set(b_ids) != set(s_ids):
        only_b = sorted(set(b_ids) - set(s_ids))
        only_s = sorted(set(s_ids) - set(b_ids))
        divs.append(("TOP5-SET", "base-only=%s shadow-only=%s" % (only_b, only_s)))
    elif b_ids != s_ids:
        divs.append(("TOP5-ORDER", "base=%s shadow=%s" % (b_ids, s_ids)))

    # Veto reasons: full ordered list, not just the count. Counts can match
    # while the vetoed action or reason changed.
    bv, sv = b.get("vetoCount", 0), s.get("vetoCount", 0)
    if bv != sv:
        divs.append(("VETO-COUNT", "%d vs %d" % (bv, sv)))
    bv_list, sv_list = b.get("vetoes", []), s.get("vetoes", [])
    if bv_list != sv_list:
        common = min(len(bv_list), len(sv_list))
        idx = next((i for i in range(common) if bv_list[i] != sv_list[i]), common)
        b_at = bv_list[idx] if idx < len(bv_list) else "<none>"
        s_at = sv_list[idx] if idx < len(sv_list) else "<none>"
        divs.append(("VETO-REASON", "first diff at [%d]: '%s' vs '%s'" %
                     (idx, _short(b_at), _short(s_at))))

    # Scores: exact by default (tolerance 0). Positional when the id
    # sequences match (handles duplicate ids); id-keyed otherwise so a
    # reordered pair still gets its score checked.
    if b_ids == s_ids:
        pairs = [(b_ids[i], b_top[i][1], s_top[i][1]) for i in range(len(b_ids))]
    else:
        b_scores, s_scores = dict(b_top), dict(s_top)
        pairs = [(aid, b_scores[aid], s_scores[aid])
                 for aid in b_scores if aid in s_scores]
    worst = None
    for aid, b_sc, s_sc in pairs:
        delta = abs(b_sc - s_sc)
        if delta > tolerance and (worst is None or delta > worst[1]):
            worst = (aid, delta, b_sc, s_sc)
    if worst is not None:
        divs.append(("SCORE-DRIFT", "id='%s' base=%s shadow=%s (delta=%.6g)" %
                     (worst[0], worst[2], worst[3], worst[1])))
    return divs


def main(argv=None):
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("baseline")
    ap.add_argument("shadow")
    ap.add_argument("--tolerance", type=float, default=0.0,
                    help="allowed absolute score drift per actionId "
                         "(default 0 = exact float equality; pass explicitly "
                         "only for a reviewed intentional delta)")
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
        widths = [5, 12, 20, 16, 13]
        fmt = "%-5s %-12s %-20s %-16s %-13s %s"
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
            print("  %-13s %d" % (kind, kind_counts[kind]), file=sys.stderr)
        print("DIVERGENT", file=sys.stderr)
        return 1
    print("PARITY: no divergences", file=sys.stderr)
    return 0


if __name__ == "__main__":
    sys.exit(main())
