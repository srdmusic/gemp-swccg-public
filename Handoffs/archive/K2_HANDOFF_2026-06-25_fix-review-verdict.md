# VERDICT — cancel-loop deploy Fix #1 review

**Reply to `K2_HANDOFF_2026-06-25_fix-review-request.md`.** Adversarially verified read-only against live source + the 196k-line game log, then independently re-checked. Nothing applied (Steve's hold).

---

## Headline: GO-WITH-CHANGES

Your Fix #1 (comment out the permanent `blockedResponses` write at 443, keep the turn-scoped one) is **mechanically correct and fixes the observed lockout**. As scoped it is **incomplete, not wrong**: line 302 has the identical defect, so ship them together.

## Your 5 claims, resolved

- **(1) Line 302 — you were right to flag it.** It writes the same `(decisionKey, positional-action-ID)` shape into the same permanent-until-game-start map, consumed by the same `DeployEvaluator -9999` / `ActionTextEvaluator -100000` vetoes. Structurally it can poison deploys exactly like 443. **Empirically it never has:** across the whole log it fired 14 times, all on Activate/Battle/Control/Draw/Move/transport, **zero on a `Choose Deploy action`**. The turn-5 lockout was 100% the 443 cancel path (35 `CANCEL LOOP BROKEN` events). So 302 did not cause this, but it is a live landmine of the same class. CONFIRMED empirically; deploy-poison is INFERRED-but-structural.
- **(2) Within-turn protection is sufficient.** The 3-strike cancel detector re-trips fresh every turn (log: it repopulates `turnBlockedActions` in turn 6 after the 5→6 clear), and the cross-turn severity ladder (`getLoopSeverity` / `shouldForceDifferentChoice` / `shouldConsiderConcede`) keys off `sequenceRepeatCount`, not this map. The permanent map adds zero protection, only stale poison. CONFIRMED.
- **(3) No other consumer needs cross-turn persistence.** Four evaluators read the flattened union (`DeployEvaluator`, `MoveEvaluator`, `ActionTextEvaluator`, `DrawEvaluator`); none distinguishes the maps or relies on persistence. `BattleEvaluator` / `CardSelectionEvaluator` do not read it. No MOVE/BATTLE regression. CONFIRMED.
- **(4) `updateState` actually clears, on the right tracker.** RandoCalAi's own `decide()` calls `updateState` on its OWN tracker before the deploy gate reads `getBlockedResponses`, and `turnBlockedActions.clear()` runs unconditionally in the turn-change branch. The wrong-instance problem is isolated to `onPhaseChange` only. The fix is NOT a no-op. CONFIRMED.
- **(5) chosenone** is byte-identical to rando except the package line. Its 302 write has NO turn-scoped sibling at all, so it is the MOST exposed copy. Mirror the fix. CONFIRMED.

## To earn the GO, apply three changes together (not just 443)

```
CHANGE 1  rando/DecisionTracker.java:443-444   comment out the blockedResponses write
          rando/DecisionTracker.java:446-447   KEEP the turnBlockedActions write
CHANGE 2  rando/DecisionTracker.java:302       write turnBlockedActions, not blockedResponses
          (same defect class as 443; closes the whole permanent-write class, not half)
CHANGE 3  chosenone/DecisionTracker.java:302, 443-444   mirror CHANGE 1 + CHANGE 2
```
No compile risk: the cancel-path locals (`lastActionChoiceKey` / `lastActionChoiceResponse`) are still referenced by the kept 446-447 write and the truncation log; the 302 locals (`k` / `r`) are still referenced by the rewritten line.

## What Fix #1 does NOT close (know this before calling it done)

Reducing scope from whole-game to one-turn kills the cross-turn lockout, but **within a single turn** the positional-ID collision survives: one deploy phase can still accumulate slot-IDs that wall off a good card later that SAME turn, because the engine reuses slot indices for different cards (the log showed 13 distinct deploy actionIds piling up in one turn). The real cure is to key the block on the **cancelled card's blueprint/title** instead of the reused positional slot. That is a follow-up V-tag, not a blocker for this ship, and it pairs with Fix #2.

## Fix #2 read: right lever, not yet cleared to ship

Contested-only (`oppPower > 0`) gating of the §A -1500 is the correct fix to stop droids entering the loop at the source, and Fix #1 does not depend on it. But the agents did NOT re-read V136 §A / V151 / V156 / V181 in this pass, so "it does not regress the contested-commit rules" is **UNVERIFIED**. Before shipping Fix #2: grep those four and do the boundary math at `oppPower == 0` (the one discipline: confirm no old positive rule silently flips sign when the -1500 is removed). Ship Fix #1 first to de-risk.

## After you apply (whichever fixes)
Compiled is not deployed is not fired. Rebuild via `reload-ai` (see `resources/BUILD_AND_DEPLOY.md`), play a real droid-deck game, and grep the container `nohup.out`: confirm the turn-5+ deploy is NOT a wall of `-9999`, and a droid actually lands. Both files are live code (no `if(false)`); still grep the enclosing `if (...)` before editing, per `feedback_check_rule_is_live_before_editing`.
