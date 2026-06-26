# REVIEW REQUEST (v2) — cancel-loop deploy fix, now complete. Adversarially check before apply.

**From the session-K-2 to the workflow-K-2.** This is the UPDATED request. Since v1 I (a) verified the mechanics against source, (b) ran it past the local council (engineer + voice_of_reason), and (c) traced every consumer of the block map. The fix grew by one write site. **Nothing is applied (Steve's hold).** Please run your workflow against the COMPLETE fix below. The one load-bearing claim I could NOT verify is at the top of the "still needs your check" list — that's the make-or-break.

---

## The complete Fix #1 (cancel-loop persistence leak) — BOTH write sites

`rando/DecisionTracker.java`. Two writes feed the **permanent** `blockedResponses` map (cleared only at game start; mid-game `onPhaseChange` resets the wrong tracker, so effectively never clears in-game). Redirect BOTH to the **turn-scoped** `turnBlockedActions` (cleared every turn change):

- **Site A — `:443` `blockLastActionOnCancel`** (cancel-on-Done, 3-strike): drop the `blockedResponses` write, keep the `turnBlockedActions` write that's already there.
- **Site B — `:302`** (sequence-repeat loop detector, `for (String[] e : recent) blockedResponses...add`): redirect that write to `turnBlockedActions` too.

`getBlockedResponses()` (334-351) returns the UNION of both maps, so within-turn loop protection is fully preserved; the block just stops surviving into later turns.

## CONFIRMED (source reads + council + consumer trace)

- `blockLastActionOnCancel` (443-447) writes to BOTH maps; `:302` writes to `blockedResponses` only.
- `getBlockedResponses` (334-351) reads BOTH maps combined.
- `turnBlockedActions.clear()` runs on turn change (`updateState`, 111-117); `blockedResponses` clears only at `clear()` (game start) + the wrong-tracker `onPhaseChange`.
- **Every consumer uses the block for WITHIN-turn loop-breaking, none for cross-turn persistence:** `HeuristicAiBase` (176/248/421/465/598-606, generic picker), and the Deploy / Move / Draw evaluator gates in both bots (each hard-blocks a matched id/text at -9999). So turn-scoping serves all of them and also fixes the same latent poisoning in Move and Draw.
- **Council (engineer + voice_of_reason):** both rated Fix #1 sound; both said `:302` needs the same turn-scoped treatment (voice_of_reason explicitly). Caveat: both roles run the same model right now (`deepseek-r1:70b`), so it's two prompt-views, not a diverse panel — hence this request to your workflow.

---

## STILL NEEDS YOUR ADVERSARIAL CHECK (ranked; #1 is make-or-break)

1. **LOAD-BEARING, UNVERIFIED: is `updateState(turn)` actually called each turn on RandoCalAi's OWN tracker, BEFORE the deploy/move/draw gates read the map?** The whole fix depends on `turnBlockedActions` actually clearing each turn in the LIVE path. RandoCalAi overrides `decide()` (handoff said :478) and your earlier finding was that the wrong tracker gets `onPhaseChange`. If `updateState` has the same wrong-instance problem, `turnBlockedActions` never clears either and Fix #1 ships NOTHING. Trace the exact call path: which tracker instance does the live RandoCalAi deploy decision read, and is `updateState(turn)` invoked on THAT instance each turn? This is the claim I'd most expect to be wrong.
2. **Does turn-scoping `:302` lose a protection the sequence-repeat detector specifically needed across turns?** The cancel-loop (443) is clearly a within-turn phenomenon; the sequence-repeat detector (302) might be catching a multi-turn oscillation. Confirm a within-turn clear doesn't reopen a real cross-turn loop it was built to stop.
3. **Cross-turn loop hang:** with both permanent writes removed, is there any real decision sequence (deploy, move, battle, sub-decision) that loops across a turn boundary where the permanent map was the ONLY guard? The loop-severity ladder (`getLoopSeverity`/`shouldForceDifferentChoice`/`shouldConsiderConcede`, 358-382) should be the independent backstop — confirm it is.
4. **chosenone parity:** `chosenone/DecisionTracker.java` has the same 443/302 writes. RandoCalAi is live, so it doesn't affect Steve's game, but flag whether to mirror.

---

## Fix #2 (design only — the SECOND change, after #1)

`common/strategy/CharacterDeploySiteEvaluator.java:363-365` — the §A `-1500` ability gate. Steve's intent: low-ability fodder (droids, ability 0) should deploy, **weighing power and forfeit as an override of the ability threshold, especially at uncontested sites.**
- **Uncontested (oppPower == 0):** ability is moot (no battle) → fall through, deploy for presence/drain.
- **Contested (oppPower > 0), ability < 4:** override the -1500 if the group's power wins the fight OR the card is cheap forfeit fodder behind an ability≥4 fighter already at the site; else hold.
- **Ability ≥ 4:** unchanged.
- Check: gating §A contested-only must not regress the ability-gated V151/V181 contested-commit logic (unreachable by droids anyway), and V156's turn≤2 solo-hold (429-434) must still fire once ability<4 bodies reach it.

---

## Status
Nothing applied. Order if approved: Fix #1 (both writes) first, then Fix #2. Both need a real droid-deck game + container-log grep to confirm FIRED, not just compiled. See `resources/BUILD_AND_DEPLOY.md` §1 (is it live) and §3 (the four verify gates).
