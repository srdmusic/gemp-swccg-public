#!/usr/bin/env python3
"""Rulebook extractor: parse every scoring arm out of the AI policy layer into JSON.

Read-only. Emits resources/rulebook/rules.json. The CODE is the source of truth;
this registry is generated, never hand-edited. See Handoffs/RULEBOOK_PLAN_2026-07-20.md.

Families (from the 2026-07-22 survey):
  A: per-file add(operations, ...) helpers wrapping PolicyOperation.add(...)
  B: per-file one(...) helpers returning single-op PolicyResult/Evaluation
  C: new Contribution(...) record arms (reason+delta, id only as V-tag prose)
  D: ShieldStrategy.java raw `score +=/-=` accumulator (bespoke pass, low confidence)
Plus inline PolicyOperation.add/hardVeto/defer call sites.
"""
import json
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
COMMON = os.path.join(REPO, "src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common")
PHASE_DIR = os.path.join(COMMON, "phase")
SHIELD_STRATEGY = os.path.join(COMMON, "strategy", "ShieldStrategy.java")
OUT_DIR = os.path.join(REPO, "resources", "rulebook")

VTAG_RE = re.compile(r"\bV\d+(?:\.\d+)?[a-z]*\b")
FLOAT_LIT_RE = re.compile(r"^[+-]?\d+(?:\.\d+)?[fF]?$")

CANON_SLOTS = ["actionId", "ruleId", "domainId", "outputKind", "delta", "reason"]


def phase_of(fname):
    n = os.path.basename(fname)
    for prefix, phase in [
        ("Setup", "SETUP"), ("Activate", "ACTIVATE"), ("Draw", "DRAW"),
        ("Control", "CONTROL"), ("Deploy", "DEPLOY"), ("Battle", "BATTLE"),
        ("Move", "MOVE"), ("Pull", "PULL"), ("Shield", "SHIELDS"),
        ("ForceLoss", "FORCE_LOSS"), ("Pass", "PASS"), ("Response", "RESPONSE"),
        ("TargetSelection", "TARGETING"), ("CoordinatorPosture", "LEGACY_COORDINATOR"),
        ("ObjectiveSideBlueprints", "OBJECTIVE"),
    ]:
        if n.startswith(prefix):
            return phase
    return "OTHER"


def strip_comments(src):
    """Blank out comments but keep line structure so line numbers survive."""
    out = []
    i, n = 0, len(src)
    in_line = in_block = in_str = False
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        if in_line:
            if c == "\n":
                in_line = False
                out.append(c)
            else:
                out.append(" ")
        elif in_block:
            if c == "*" and nxt == "/":
                in_block = False
                out.append("  ")
                i += 1
            else:
                out.append(c if c == "\n" else " ")
        elif in_str:
            out.append(c)
            if c == "\\":
                out.append(nxt)
                i += 1
            elif c == '"':
                in_str = False
        else:
            if c == "/" and nxt == "/":
                in_line = True
                out.append("  ")
                i += 1
            elif c == "/" and nxt == "*":
                in_block = True
                out.append("  ")
                i += 1
            elif c == '"':
                in_str = True
                out.append(c)
            else:
                out.append(c)
        i += 1
    return "".join(out)


def balanced_call(src, open_paren_idx):
    """Return (arg_string, end_idx) for the call whose '(' is at open_paren_idx."""
    depth = 0
    i = open_paren_idx
    in_str = False
    while i < len(src):
        c = src[i]
        if in_str:
            if c == "\\":
                i += 1
            elif c == '"':
                in_str = False
        elif c == '"':
            in_str = True
        elif c == "(":
            depth += 1
        elif c == ")":
            depth -= 1
            if depth == 0:
                return src[open_paren_idx + 1:i], i
        i += 1
    return None, i


def split_args(argstr):
    """Top-level comma split, respecting parens/strings/generics-lite."""
    args, depth, cur, in_str = [], 0, [], False
    i = 0
    while i < len(argstr):
        c = argstr[i]
        if in_str:
            cur.append(c)
            if c == "\\":
                i += 1
                cur.append(argstr[i] if i < len(argstr) else "")
            elif c == '"':
                in_str = False
        elif c == '"':
            in_str = True
            cur.append(c)
        elif c in "([{":
            depth += 1
            cur.append(c)
        elif c in ")]}":
            depth -= 1
            cur.append(c)
        elif c == "," and depth == 0:
            args.append("".join(cur).strip())
            cur = []
        else:
            cur.append(c)
        i += 1
    if cur:
        args.append("".join(cur).strip())
    return [a for a in args if a != ""]


def const_table(clean):
    """Resolve simple numeric constants: private static final float/int NAME = <num>;"""
    table = {}
    for m in re.finditer(
        r"static\s+final\s+(?:float|int)\s+(\w+)\s*=\s*(-?\d+(?:\.\d+)?)[fF]?\s*;", clean
    ):
        table[m.group(1)] = float(m.group(2))
    return table


def parse_delta(expr, consts):
    expr = expr.strip()
    if FLOAT_LIT_RE.match(expr):
        return float(expr.rstrip("fF")), "literal"
    if expr in consts:
        return consts[expr], "constant"
    neg = expr.lstrip("-").strip()
    if expr.startswith("-") and neg in consts:
        return -consts[neg], "constant"
    return None, "expression"


def parse_reason(expr):
    expr = expr.strip()
    if re.fullmatch(r'"(?:[^"\\]|\\.)*"', expr):
        return json.loads(expr), "literal"
    fm = re.match(r'String\.format\(\s*("(?:[^"\\]|\\.)*")', expr)
    if fm:
        return json.loads(fm.group(1)), "format"
    lit = re.search(r'"(?:[^"\\]|\\.)*"', expr)
    if lit:
        return json.loads(lit.group(0)) + " …", "concat"
    return expr[:80], "expression"


def line_of(src, idx):
    return src.count("\n", 0, idx) + 1


def canonical_positions(body_args, helper_params, file_consts_domains):
    """Map canonical PolicyOperation.add arg positions -> helper param name or fixed value.

    PolicyOperation.add(actionId, TraceRuleId.of(x)|x, domain, kind, delta, reason)
    """
    slots = {}
    if len(body_args) < 6:
        return None
    for slot, arg in zip(CANON_SLOTS, body_args[:6]):
        arg = arg.strip()
        m = re.match(r"TraceRuleId\.of\(\s*(\w+)\s*\)", arg)
        if m:
            arg = m.group(1)
        if arg in helper_params:
            slots[slot] = ("param", helper_params.index(arg))
        else:
            slots[slot] = ("fixed", arg)
    return slots


def helper_maps(clean, fname):
    """Find local helper defs (add/one/…), map params to canonical slots.

    Returns (helpers, body_spans). Handles two-level indirection: a helper whose
    body calls another local mapped helper composes that helper's slot map.
    """
    defs = []
    for m in re.finditer(
        r"(?:(?:public|private|protected|static)\s+)+[\w.<>\[\]]+\s+(\w+)\s*\(",
        clean,
    ):
        name = m.group(1)
        if name not in ("add", "one", "operation", "op"):
            continue
        sig, sig_end = balanced_call(clean, clean.index("(", m.end() - 1))
        if sig is None:
            continue
        params = []
        for p in split_args(sig):
            toks = p.split()
            if toks:
                params.append(toks[-1])
        brace = clean.find("{", sig_end)
        if brace < 0:
            continue
        depth, j = 0, brace
        while j < len(clean):
            if clean[j] == "{":
                depth += 1
            elif clean[j] == "}":
                depth -= 1
                if depth == 0:
                    break
            j += 1
        defs.append({"name": name, "params": params, "body": clean[brace:j],
                     "span": (brace, j), "nparams": len(params)})

    helpers = {}
    body_spans = []  # only spans of defs that resolve to slot maps (real builders)

    def direct_map(d):
        po = re.search(r"PolicyOperation\.(add|hardVeto|defer)\s*\(", d["body"])
        if not po:
            return None
        body_args, _ = balanced_call(d["body"], d["body"].index("(", po.end() - 1))
        return canonical_positions(split_args(body_args), d["params"], None)

    # pass 1: direct
    unresolved = []
    for d in defs:
        slots = direct_map(d)
        if slots:
            helpers.setdefault(d["name"], []).append(
                {"params": d["params"], "slots": slots, "nparams": d["nparams"]})
            body_spans.append(d["span"])
        else:
            unresolved.append(d)
    # pass 2: compose through a call to an already-mapped local helper
    for d in unresolved:
        composed = None
        for hname, variants in list(helpers.items()):
            cm = re.search(r"(?<![\w.])%s\s*\(" % hname, d["body"])
            if not cm:
                continue
            argstr, _ = balanced_call(d["body"], d["body"].index("(", cm.end() - 1))
            inner_args = split_args(argstr or "")
            variant = next((v for v in variants if v["nparams"] == len(inner_args)), None)
            if not variant:
                continue
            slots = {}
            ok = True
            for s in CANON_SLOTS:
                origin, val = variant["slots"][s]
                if origin == "fixed":
                    slots[s] = ("fixed", val)
                else:
                    arg = inner_args[val].strip() if val < len(inner_args) else ""
                    am = re.match(r"TraceRuleId\.of\(\s*(\w+)\s*\)", arg)
                    if am:
                        arg = am.group(1)
                    if arg in d["params"]:
                        slots[s] = ("param", d["params"].index(arg))
                    elif arg:
                        slots[s] = ("fixed", arg)
                    else:
                        ok = False
                        break
            if ok:
                composed = slots
                break
        if composed:
            helpers.setdefault(d["name"], []).append(
                {"params": d["params"], "slots": composed, "nparams": d["nparams"]})
            body_spans.append(d["span"])
    return helpers, body_spans


def contribution_shape(clean):
    """Find record Contribution(...) component order; return (reason_idx, delta_idx, n) or None."""
    m = re.search(r"record\s+Contribution\s*\(", clean)
    if not m:
        return None
    sig, _ = balanced_call(clean, clean.index("(", m.end() - 1))
    comps = split_args(sig)
    reason_idx = delta_idx = None
    for i, c in enumerate(comps):
        toks = c.split()
        if len(toks) >= 2:
            if toks[-2] == "String" and reason_idx is None and "reason" in toks[-1].lower():
                reason_idx = i
            if toks[-2] == "float" and delta_idx is None:
                delta_idx = i
    if reason_idx is None:
        for i, c in enumerate(comps):
            if c.split()[-2:] and c.split()[-2] == "String":
                reason_idx = i
                break
    if reason_idx is None or delta_idx is None:
        return None
    return reason_idx, delta_idx, len(comps)


def enclosing_method(clean, idx):
    """Best-effort: nearest preceding method definition name."""
    best = None
    for m in re.finditer(r"(?:public|private|protected)\s+(?:static\s+)?[\w.<>\[\]]+\s+(\w+)\s*\(", clean[:idx]):
        best = m.group(1)
    return best or "?"


def extract_file(path, rel):
    src = open(path).read()
    clean = strip_comments(src)
    consts = const_table(clean)
    phase = phase_of(path)
    dom_m = re.search(r"DOMAIN\s*=\s*TraceDomainId\.(\w+)", clean)
    file_domain = dom_m.group(1) if dom_m else None
    kind_m = re.search(r"OUTPUT_KIND\s*=\s*TraceOutputKind\.(\w+)", clean)
    file_kind = kind_m.group(1) if kind_m else None
    helpers, helper_body_spans = helper_maps(clean, path)
    rules = []
    seen_spans = []

    def in_helper_body(idx):
        return any(a <= idx <= b for a, b in helper_body_spans)

    def emit(idx, family, id_val, id_source, kind, domain, delta_raw, reason_raw):
        delta, delta_kind = parse_delta(delta_raw, consts) if delta_raw is not None else (None, "expression")
        reason, reason_kind = parse_reason(reason_raw) if reason_raw is not None else ("", "expression")
        prov = sorted(set(VTAG_RE.findall((id_val or "") + " " + (reason or ""))))
        method = enclosing_method(clean, idx)
        if id_source == "none":
            id_val = "%s.%s#%d" % (os.path.basename(path).replace(".java", ""), method, line_of(src, idx))
        rules.append({
            "id": id_val,
            "id_source": id_source,
            "phase": phase,
            "domain": domain or file_domain,
            "policy": os.path.basename(path).replace(".java", ""),
            "method": method,
            "kind": kind or file_kind,
            "delta": delta,
            "delta_expr": None if delta_kind in ("literal", "constant") else (delta_raw or "").strip()[:100],
            "delta_kind": delta_kind,
            "reason": reason,
            "reason_kind": reason_kind,
            "provenance": prov,
            "file": rel,
            "line": line_of(src, idx),
            "family": family,
        })

    # Families A + B: local helper call sites
    for hname, variants in helpers.items():
        if hname not in ("add", "one"):
            continue
        for call in re.finditer(r"(?<![\w.])%s\s*\(" % hname, clean):
            cidx = call.start()
            # skip the helper definitions themselves and forwarding calls inside helper bodies
            pre = clean[max(0, cidx - 80):cidx]
            if re.search(r"(void|PolicyResult|Evaluation|static)\s+$", pre):
                continue
            if in_helper_body(cidx):
                continue
            argstr, endi = balanced_call(clean, clean.index("(", call.end() - 1))
            if argstr is None:
                continue
            args = split_args(argstr)
            variant = next((v for v in variants if v["nparams"] == len(args)), None)
            if variant is None:
                if len(variants) == 1 and abs(variants[0]["nparams"] - len(args)) <= 0:
                    variant = variants[0]
                else:
                    continue
            slot = {}
            for s in CANON_SLOTS:
                origin, val = variant["slots"][s]
                slot[s] = args[val] if origin == "param" and val < len(args) else (val if origin == "fixed" else None)
            rid = slot.get("ruleId") or ""
            rid_str = None
            if rid.startswith('"'):
                try:
                    rid_str = json.loads(rid)
                except ValueError:
                    lm = re.match(r'"((?:[^"\\]|\\.)*)"', rid)
                    rid_str = lm.group(1) if lm else None
            dom = None
            dm = re.match(r"TraceDomainId\.(\w+)", slot.get("domainId") or "")
            if dm:
                dom = dm.group(1)
            kn = None
            km = re.match(r"TraceOutputKind\.(\w+)", slot.get("outputKind") or "")
            if km:
                kn = km.group(1)
            family = "A" if hname == "add" else "B"
            emit(cidx, family,
                 rid_str if rid_str else (rid or None),
                 "typed" if rid_str else ("typed-dynamic" if rid else "none"),
                 kn, dom, slot.get("delta"), slot.get("reason"))
            seen_spans.append((cidx, endi))

    # Inline PolicyOperation.add / hardVeto / defer outside helper bodies
    for m in re.finditer(r"PolicyOperation\.(add|hardVeto|defer)\s*\(", clean):
        cidx = m.start()
        if any(a <= cidx <= b for a, b in seen_spans) or in_helper_body(cidx):
            continue
        # skip if inside a helper body we already mapped (helper bodies re-found here)
        argstr, endi = balanced_call(clean, clean.index("(", m.end() - 1))
        args = split_args(argstr or "")
        if len(args) < 4:
            continue
        # canonical order
        rid = args[1]
        rm = re.match(r"TraceRuleId\.of\(\s*(\"(?:[^\"\\]|\\.)*\")\s*\)", rid)
        rid_str = json.loads(rm.group(1)) if rm else None
        if rid_str is None and re.match(r"TraceRuleId\.of\(", rid):
            rid_str = None  # dynamic
        elif rid_str is None:
            # helper param name → this IS a helper body; skip (already represented)
            continue
        dom = (re.match(r"TraceDomainId\.(\w+)", args[2]) or [None]) and (
            re.match(r"TraceDomainId\.(\w+)", args[2]).group(1) if re.match(r"TraceDomainId\.(\w+)", args[2]) else None)
        kn = re.match(r"TraceOutputKind\.(\w+)", args[3]).group(1) if len(args) > 3 and re.match(r"TraceOutputKind\.(\w+)", args[3]) else None
        delta_raw = args[4] if m.group(1) == "add" and len(args) > 4 else "0"
        reason_raw = args[5] if m.group(1) == "add" and len(args) > 5 else (args[4] if len(args) > 4 else '""')
        emit(cidx, "A-inline", rid_str, "typed" if rid_str else "typed-dynamic", kn, dom, delta_raw, reason_raw)

    # Family C: new Contribution(...)
    shape = contribution_shape(clean)
    if shape:
        reason_idx, delta_idx, ncomp = shape
        for m in re.finditer(r"new\s+Contribution\s*\(", clean):
            cidx = m.start()
            argstr, endi = balanced_call(clean, clean.index("(", m.end() - 1))
            args = split_args(argstr or "")
            if len(args) != ncomp:
                continue
            reason_raw = args[reason_idx]
            delta_raw = args[delta_idx]
            if reason_raw.strip() in ("null",):
                continue
            reason, _rk = parse_reason(reason_raw)
            tags = VTAG_RE.findall(reason or "")
            # BattleDecision variant carries TraceRuleId in components
            tri = next((a for a in args if "TraceRuleId.of(" in a), None)
            if tri:
                rm = re.search(r'TraceRuleId\.of\(\s*("(?:[^"\\]|\\.)*")', tri)
                emit(cidx, "C", json.loads(rm.group(1)) if rm else None,
                     "typed" if rm else "typed-dynamic", None, None, delta_raw, reason_raw)
            elif tags:
                emit(cidx, "C", tags[0], "prose-vtag", None, None, delta_raw, reason_raw)
            else:
                emit(cidx, "C", None, "none", None, None, delta_raw, reason_raw)
    return rules


def extract_shield_strategy(path, rel):
    src = open(path).read()
    clean = strip_comments(src)
    consts = const_table(clean)
    rules = []
    lines = src.split("\n")
    for m in re.finditer(r"(?:score|\w*[Ss]core)\s*([+\-])=\s*([^;]+);", clean):
        ln = line_of(src, m.start())
        expr = ("-" if m.group(1) == "-" else "") + m.group(2).strip()
        delta, delta_kind = parse_delta(expr, consts)
        vtag = None
        reason = None
        for back in range(ln - 1, max(0, ln - 8), -1):
            t = lines[back - 1]
            tag = VTAG_RE.search(t)
            if tag:
                vtag = tag.group(0)
                lit = re.search(r'"((?:[^"\\]|\\.)*)"', t)
                reason = lit.group(1) if lit else t.strip().lstrip("/ ").strip()[:90]
                break
        rules.append({
            "id": vtag or "ShieldStrategy#%d" % ln,
            "id_source": "prose-vtag" if vtag else "none",
            "phase": "SHIELDS",
            "domain": "SHIELDS",
            "policy": "ShieldStrategy",
            "method": enclosing_method(clean, m.start()),
            "kind": None,
            "delta": delta,
            "delta_expr": None if delta_kind in ("literal", "constant") else expr[:100],
            "delta_kind": delta_kind,
            "reason": reason or "(legacy accumulator arm)",
            "reason_kind": "adjacent-log" if reason else "none",
            "provenance": [vtag] if vtag else [],
            "file": rel,
            "line": ln,
            "family": "D",
            "confidence": "low",
        })
    return rules


def main():
    head = subprocess.run(["git", "-C", REPO, "rev-parse", "--short", "HEAD"],
                          capture_output=True, text=True).stdout.strip()
    all_rules = []
    files = sorted(f for f in os.listdir(PHASE_DIR) if f.endswith(".java"))
    for f in files:
        p = os.path.join(PHASE_DIR, f)
        rel = os.path.relpath(p, REPO)
        try:
            all_rules.extend(extract_file(p, rel))
        except Exception as e:
            print("EXTRACT ERROR %s: %s" % (f, e), file=sys.stderr)
    all_rules.extend(extract_shield_strategy(SHIELD_STRATEGY, os.path.relpath(SHIELD_STRATEGY, REPO)))

    fam = {}
    for r in all_rules:
        fam[r["family"]] = fam.get(r["family"], 0) + 1
    idsrc = {}
    for r in all_rules:
        idsrc[r["id_source"]] = idsrc.get(r["id_source"], 0) + 1

    os.makedirs(OUT_DIR, exist_ok=True)
    out = {
        "head": head,
        "counts": {"total": len(all_rules), "by_family": fam, "by_id_source": idsrc},
        "rules": all_rules,
    }
    with open(os.path.join(OUT_DIR, "rules.json"), "w") as fh:
        json.dump(out, fh, indent=1)
    print("HEAD %s  total=%d  by_family=%s  by_id_source=%s" %
          (head, len(all_rules), fam, idsrc))
    # Coverage tolerance vs survey (A~223 B~97 C~170); warn loudly on collapse
    expect = {"A": 223, "B": 97, "C": 170}
    for k, v in expect.items():
        got = fam.get(k, 0) + (fam.get(k + "-inline", 0) if k == "A" else 0)
        if got < v * 0.75:
            print("WARNING: family %s coverage %d < 75%% of survey %d — extractor gap" % (k, got, v))


if __name__ == "__main__":
    main()
