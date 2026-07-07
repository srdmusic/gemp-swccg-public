# REORG HEALTH AUDIT — 2026-07-07

Independent diagnosis of the 2-day reorg/consolidation (~7,175 lines across 41 files),
run by K-2 (Opus). Reference: Steve's own pre-reorg backup at `/Users/steve/gemp-swccg-public copy/`
(git `8f841bd25`), filesystem-verified byte-identical to git — so both agree.
Method: 6 subsystem diff agents + adversarial verification + 8 self-play soak games.

## Bottom line

**The reorg is fundamentally SOUND.** The specific failure Steve feared — old rules
silently dominated or lost — did NOT happen. Across all 6 subsystems the audit found
zero dominated rules, zero silently-lost rules, and zero undocumented magnitude drift
in live code. The biggest and riskiest change (the move clobber ladder) is the cleanest.
One real defect surfaced: a git-hygiene problem (a referenced file was never committed
in the reorg range), which does NOT affect the running Rando and is already fixed at HEAD.
It validates exactly the caution Steve raised about not trusting git.

## Per-subsystem verdict

| Subsystem | Verdict | What the reorg did |
|---|---|---|
| MOVE + ladder (T4.1) | ✅ SOUND (cleanest) | Converted ~20 raw-score move rules (with ±9999 blocks + early returns that buried everything below them) into rank bands R1-R4 + a veto class. Every old rule verified surviving as a ranked claim or veto. Pre-committed boundary math holds. Fixed 2 real pre-existing bugs along the way (V73 shuttle was dead code by dominance; V47 Lando lock false-fired on any "platform" site) |
| PULL + V192 (T4.2) | ✅ SOUND | Collapsed the ~+6250 additive pull pile into one tiered scorer. All dead-search vetoes (V177/V183/V66/V67h) + nested guards preserved; activate-window pull stays above V168 +5000; V131 downgrade re-sized |
| DEPLOY siting | ⚠ 1 critical (git-only) | V136/V158 hubs + V67al lying-comment fix + V29/V47/V51/V190 in-place updates verified. The critical finding (MaintenanceFacts, below) is a build-provenance issue, not a siting regression |
| BATTLE + forfeit | ✅ SOUND | V159 forfeit picker + V153 force-loss verified. Note: the V153 "byte-identical parity pair" banner slightly overstates it (the two copies differ; see low finding) |
| ECONOMY (T2 helpers) | ✅ SOUND | Maintenance/DTF cache + shield predicate unification. Soak proof: `MAINT CACHE MISMATCH` fired **0 times** — the cache produces identical values to the old per-caller computation. Score-neutral confirmed |
| CROSS-CUTTING | ✅ SOUND | CombinedEvaluator epilogue + V191 logging + banners. V191 verified instrumentation-only (logs from a copy, never reorders the live list). Additive merge loop unchanged |

## The one critical finding — and why it doesn't threaten Rando

**`MaintenanceFacts.java` was referenced but never committed in the reorg range.** The
reorg commits (`34b47ba50` onward) call `MaintenanceFacts.maintainCost(...)` in
DeployEvaluator (both bots, 6 sites), but the file itself was never `git add`-ed until
`c1d5ced8c` (this session's WIP checkpoint, which swept it in). The changelog even claims
it was added in the T2 commit — it wasn't.

| Impact | Reality |
|---|---|
| Running Rando | **Fine.** The file was always physically on disk, so the jar compiled and ran with it. Verified: 82-line real implementation, in the deployed jar |
| Current HEAD | **Fine.** Now tracked; HEAD compiles; no other untracked .java remain |
| Historical commits `34b47ba50..4166a03b5` | **Broken build** — a clean checkout of any of those references a class not in the tree. Local-only, never pushed, so no external harm |
| Steve's instinct | **Vindicated.** This is exactly the "git doesn't tell the full story" gap. Your backup + filesystem check is why we caught it |

Fix: already de-facto resolved (tracked at HEAD). Optional cleanup: correct the changelog
provenance line. No history rewrite needed (nothing pushed).

## Low findings

- **V153 "byte-identical parity pair" comment overstates it.** The two force-loss zone
  copies (drain-loss vs battle-loss) differ; the live logic is close but the battle-loss
  path is missing some protections. This is the pre-existing `shields-response-5` backlog
  item, NOT a reorg regression. The banner comment should be corrected so a future editor
  isn't misled into thinking one edit covers both.
- **V60 Hidden Path transit 9999 → 20000.** Intentional, documented (aligns with the
  ladder R4 band), verified no collision with V192's clamp. Fine.

## Behavioral soak (8 self-play games on the pure-reorg jar)

| Signal | Count | Read |
|---|---|---|
| Exceptions / ERRORs | 0 | No crashes |
| MAINT CACHE MISMATCH | 0 | Cache is score-neutral ✓ |
| LADDER BAND INVERSION | 0 | Rank bands never overlap ✓ |
| LADDER BANDS OK (startup assert) | 2 | Band integrity self-check passes ✓ |
| Ladder claims firing | R2 DOCTRINE (V31), R3 SURVIVAL, SPREAD/ATTACK rejected by L2 gate | The ladder works live ✓ (first real workout) |
| CANCEL LOOP BROKEN | 52 (~6.5/game) | ⚠ friction — see below |
| "All actions bad → PASS" | 203 (~25/game) | ⚠ friction — see below |

The reorg-specific instrumentation is all healthy. The two friction signals (cancel-loops
+ all-bad-pass) align with the **known solo-stranding class**: Rando picks its best move,
a veto (V41 wrong-direction and kin) blocks it, it re-picks and loops. This is the exact
mechanism behind the Fel-at-Beach loss — and it's what the parked solo/Verge fix targets.

CAVEAT (honest): I did NOT run the same soak on a pre-reorg jar, so these counts are
informational, not a reorg-vs-baseline delta. Rando won most soak games regardless.

## Recommendations, in order

1. **Proceed with the solo/Verge fix** (parked at `c1d5ced8c`), refit to your ability-total
   doctrine. The behavioral friction confirms it's the highest-value next fix — it directly
   attacks the V41-veto stranding driving the cancel-loops.
2. **Correct two doc lines** (cheap): the MaintenanceFacts changelog provenance, and the
   V153 "byte-identical" banner. No code change.
3. **Optional A/B** (if you want certainty on the friction): build a jar from your backup
   and run the same 8-game soak, compare cancel-loop / all-bad-pass rates. Confirms whether
   the reorg changed the rate or it's baseline Rando behavior.
4. **Backlog** (`shields-response-5`): the battle-loss force-loss path missing protections —
   a real pre-existing behavioral gap, not urgent.

## What this audit did NOT find (the reassurance)

No dominated rules. No silently-lost rules. No undocumented magnitude drift in live code.
No crashes. No score-neutrality violations in the maintenance cache. The compressed 2-day
reorg preserved Rando's behavior where it claimed to, and the two deliberate consolidations
(ladder, V192) are correct and boundary-checked. Your foundation is load-bearing.
