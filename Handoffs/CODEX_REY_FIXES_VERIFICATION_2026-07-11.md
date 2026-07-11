# CODEX HANDOFF — Rey-game + AMN fixes: verification & follow-ups (2026-07-11)

Owner: Codex/Alfred (verification + data). K-2 shipped the fixes; your job is adversarial verification and
the follow-up corpus work below. NO Java edits without explicit Steve redirect.

## The four commits to verify (in order)
| SHA | What |
|---|---|
| `c20e09e10` | AMN setup-hang: DecisionSafety SELECTABLE-CLAMP (both bots) + V22.7 routing excludes into-hand/prison. Already verified PASS (your m00113) — re-verify only the runtime SAFETY CLAMP fire on the next AMN game. |
| `5bfcd8701` | Wave 1: V177/V166 survivability gate (affordable-buddy wave + weapon-adjusted enemy), V22.3 inclusive tiers, V148 deploy-cancel bar 0 for "where to deploy", V67s+V185 possessive/fuzzy pull guards (isPossessiveTypeTarget), V82.1 search-into-hand parser pattern, V82.2 all-words + partial→UNKNOWN (your m00118), V82.2b persona rescue. |
| `4836a836d` | Wave 2: V29.7/V76 opponent weapon bonus (predictor theirPower+oppWeaponBonus), V76 hit-economics on the V22.4 fallback route (-500 pyrrhic, never-favorable guard), V47+V37.1 weapon-adjusted threat (oppWeaponBonusAt), V67ae V33-mirror retreat exemption (gap>=6), V177 word-rescue type-word skip (mapTypeWordToCategory now public), V185 veto mirrored into ATE V67h (-9999). |
| `83e4ff89a` | V82.1 regex widened per your m00122/m00124: `[,.]?` separator + optional `your` (36/40 corpus). |

## Verification protocol per SHA
- rando↔chosenone parity (normalized diff; only package tokens differ).
- Adjust-in-place discipline: every change tagged with its EXISTING V-tag + "ADJUSTED 2026-07-10" (no new V-tags; V82.2b is the one sanctioned extension inside validatePullFromSourceCard).
- No edits inside `if (false /* SUPERSEDED */)` blocks.
- Compile in-container MVN_EXIT=0 + byte-presence of the new strings in web.jar AFTER K-2's deploy
  ("SAFETY CLAMP", "V76 (fallback) HIT ECONOMICS", "V67ae RETREAT EXEMPT", "V82.2b persona match",
  "V185 (ATE mirror)", "isPossessiveTypeTarget").
- Boundary math spot-checks (the changelog entries carry the per-incident numbers — recompute independently):
  T2 Yoda gate (3 vs 18 → GATED), T2 Luke (best −10 → PASS via V148), T5 battle (pyrrhic or non-favorable),
  T5 Lando (RETREAT tier at −5..−6 armed), incidents 1-6 pull verdicts.
- Replay/log proof AFTER Steve's next games: grep for the new log markers; flag any silent regression
  (esp. V177 false-blocks: your Quite A Mercenary case must now WILL_SUCCEED via persona rescue).

## Your follow-up corpus tasks (non-blocking, standing queue)
1. Remaining 4/40 unmatched search texts (10_13, 5_61 any-character react; 3_51 exterior-site deploy;
   4_44 levitation word order): confirm fail-open is CORRECT for each (V177 skips, engine handles) or
   propose a pattern if any is a real dead-search risk in现 decks.
2. Persona-rescue corpus sweep: list every pull filter using Filters.persona / Persona-based combos and
   confirm V82.2b resolves each (evidence table). Flag personas whose getHumanReadable differs from the
   deck-list nickname.
3. Weapon-heuristic corpus check: cards whose game text contains "permanent weapon" but are NOT
   combat-relevant (if any) — false +3/+5 sources for the new opponent-weapon math.
4. AMN-class sweep: other iterative TakeCardCombination cards (prisons/bounty hunters family) — list them
   so the SELECTABLE-CLAMP path can be regression-watched on their setup decisions.

## Context docs
- Incident evidence: agent reports summarized in AI_CHANGELOG 2026-07-10 entries (waves 1-2) + your
  handoffs (CODEX_REY_REPLAY_AMN_ANALYSIS, OBJECTIVE_* docs unrelated).
- Engine-side flag PARKED for Steve: SwccgGameMediator.maybeLetAiPlay swallows DecisionResultInvalidException
  for AI players (no log, no retry). A LOG.error + one emergency retry there would make the whole hang class
  impossible. NEEDS STEVE'S EXPLICIT OK (engine file). Neither of us touches it until then.
