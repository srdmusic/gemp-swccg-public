# K-2 HANDOFF — 2026-06-25: why Rando won't deploy a droid deck

**For the new K-2.** Steve played a DARK Battle-Droid swarm deck. Rando stopped deploying characters after turn 2, drew into a 16-20 card hand with plenty of force, and never built ground presence. The opponent walked onto Naboo. This is the verified root cause, ranked, with exact fix points. **Nothing is applied** (Steve's hold). Verified by a 6-agent read-only workflow plus an adversarial pass against the real source and the game log; confirmed vs inferred is marked throughout.

---

## The one-paragraph version

Battle Droids are ability 0. The V136 §A site-evaluator returns **-1500 to any group under ability 4 at every site, contested or empty**, so a pure-droid deck cannot score a single deployable site. The lone droid then fails the "Choose where to deploy" pick, clicks Done, and after **3 strikes** a cancel-loop block fires and writes the blocked deploy slot into a **permanent** map that is never cleared mid-game. Those slots are **positional action IDs that the engine reuses for different cards each decision**, so by turn 5 the accumulated block set poisoned **every** Deploy action, including the legitimate Lord Sidious (A:7) and Dofine+Tey How (A:5) groups the planner actually wanted. All three mechanisms chain. If you change one line, change the cancel-loop persistence.

---

## The causal chain (CONFIRMED from log + code)

1. **Trigger — V136 §A ability gate.** `CharacterDeploySiteEvaluator.java:212` (`abilityPass = teamAbility >= 4`) and `:363-365`:
   ```java
   if (!abilityPass) {
       if (buddyInHand) return -200f;   // needs a hand CHARACTER with ability
       return -1500f;                    // "Steve's almost never"
   }
   ```
   Droids hard-code ability 0 (`AbstractDroid`), so `teamAbility` stays 0 and `buddyInHand` can never be set by an all-droid hand. The -1500 fires at **uncontested/empty sites too** (the gate at 363 is reached before any oppPower-conditioned branch; all the +400/+500 win logic at 259-360 and 436 is ability-gated and unreachable for a droid). §B/§C/§D max out around +300, so -1500 is never offset. LIVE: the call-site guard at `DeployEvaluator.java:1738` is real conditions, not `if(false)` (the dead `if(false)` nearby is the unrelated V90 block at :1784).

2. **Amplifier and dominant killer — the cancel-loop persistence leak.**
   - `DecisionTracker.java:63-65`: `CANCEL_LOOP_THRESHOLD = 3`. A "strike" is a Done/Pass on a cancelable `CARD_SELECTION`/`ARBITRARY_CARDS` sub-decision on the same key (`:206-226`, checked `:218`).
   - After 3 strikes, `blockLastActionOnCancel` (`:425-461`) writes the last outer action ID into **both** `blockedResponses` (`:443-444`, **permanent**) and `turnBlockedActions` (`:446-447`, turn-scoped).
   - `DeployEvaluator.java:525-545` hard-blocks any deploy whose `actionId` **OR** `actionText` is in `getBlockedResponses()` at **-9999**; `ActionTextEvaluator.java:~96-116` escalates to **-100000** (hard veto).
   - The block IDs are **positional indices rebuilt each decision**, and every from-hand deploy shares the text `Deploy`, so a slot blocked by a useless droid on turn 2 vetoes a different, good card on turn 5. The accumulated set `{3,4,5,7,8,9,10,11}` blocked every Deploy by turn 5.
   - RandoCalAi's `blockedResponses` is cleared by **nothing** mid-game, only `clear()` at game start (`RandoCalAi.java:1524`).

3. **Minor/redundant — the early-game hold.** `DeployPhasePlanner.java:258-266`, `RandoConfig.java:49` (`DEPLOY_EARLY_GAME_THRESHOLD = 110`), `:52` (`DEPLOY_EARLY_GAME_TURNS = 3`). It only bit turn 3 (plan score 6 < 110), and turn 3 was already cancel-loop-blocked, so it changed nothing here. Real but lowest leverage.

**Confirmed in the log:** turn-5 `FINAL PLAN` wanted Dofine + Tey How + Security Droid (group power 6) and Sidious (A:7) + Infantry Droid (power 7), all ability-sufficient groups that PASS §A, but every Deploy was -9999'd then -100000 vetoed. Even `V37.4 HAND BLOAT: hand=13, force=27 — DEPLOY SOMETHING!` could not break through. Rando passed and drew into bloat. Steve's "20 cards, surely ability 4 was there" was exactly right: it was there, and it was blocked.

---

## Two corrections (do NOT repeat these)

- **"`onPhaseChange()` has zero callers" is WRONG.** It has a caller (`HeuristicAiBase.java:75`), but there are **two** DecisionTracker instances. `RandoCalAi` overrides `decide()` (`:478`) and never calls `onPhaseChange` on its own tracker (`:61/216`); the caller resets HeuristicAiBase's instance instead. Every `Loop tracker reset on phase change to: DEPLOY` log line resets the WRONG tracker (it is preceded by `Evaluators returned null, falling back to heuristics`). So do not "wire up onPhaseChange" as the fix; it would reset the tracker the deploy gate does not read. The deploy-gating map clears at game start only.
- **"Blocks the generic Deploy text" is imprecise.** It blocks positional numeric IDs; the generic effect comes from ID reuse plus the gate matching on `actionId` OR `actionText`. True in effect, wrong in literal mechanism.

---

## The fix (proposed, NOT applied — Steve's call)

- **#1, highest leverage, one spot: `DecisionTracker.java:443-447`.** Stop writing the cancel-block to the permanent `blockedResponses`. Either write it to `turnBlockedActions` ONLY (it already clears each turn), or key the block on the cancelled card's blueprint/title instead of the positional outer action ID. This single change unsticks the turn-5 commander deploys and removes the landmine for EVERY archetype (a spy-blocked site or a weapon with no wielder would poison deploys the same way). Independently necessary.
- **#2, the droid root trigger: `CharacterDeploySiteEvaluator.java:363-365`.** Gate the -1500 on `oppPower > 0` so ability-0 fodder can spread to uncontested drain sites. Droids are forfeit fodder and drain bodies, not battlers, so a low-ability body at an empty site is correct play. **Caveat (do the boundary math first):** this is NOT a clean one-liner. It collides with V156 (`:429-434`), which deliberately -300-holds solo bodies at uncontested sites on turns 1-2, and with the +500 ability>=4 reward at :436. Thread between them. Fix-safety confirmed: everything below the -1500 gate (V156, the power branches, the contested V151/V181 commit logic at +400/+500) is reachable only by ability>=4 bodies, so gating §A on contested-only does NOT regress contested-fight commitment.
- **#3, optional cleanup:** `DEPLOY_EARLY_GAME_TURNS 3 → 2` (`RandoConfig.java:52`) closes the minor turn-3 hold seam. Do it only as housekeeping; it is moot for this symptom.

**Order:** ship #1 first (restores the most deploying, lowest risk, helps all decks), then #2 for droid decks, then #3 if you feel like it.

---

## Still UNCONFIRMED (be honest about this)
- Whether fix #2 ALONE would let the turn-5 commander groups deploy cannot be proven without re-running, because the turn-5 lockout was 100% cancel-loop residue. INFERRED: if #2 keeps droids out of the cancel-loop, the residue never forms and turn 5 is clean. Fix #1 makes this moot.
- The `-200` buddy branch was never exercised this game (pure-droid hand). Its mixed-hand behavior is read from code, not observed.

## Before you touch any of this
- It is all LIVE code (verified, none in `if(false)`). Still grep the enclosing `if (...)` before editing, per `feedback_check_rule_is_live_before_editing`.
- Both fixes are in the `common/` and `rando/` packages. Mirror into `chosenone/` if you want parity (RandoCalAi is the live bot).
- Verify after deploying with a REAL droid-deck game: grep the container `nohup.out` for `cancel-loop`, confirm the turn-5 deploy is NOT a wall of `-9999`, and confirm a droid actually lands on an uncontested site. Compiled is not deployed is not fired. See `resources/BUILD_AND_DEPLOY.md`.
