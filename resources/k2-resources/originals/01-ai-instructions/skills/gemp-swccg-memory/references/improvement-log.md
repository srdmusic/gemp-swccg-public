---
title: Rando Cal AI - Improvement Log
updated: 2026-03-03
purpose: Accumulated observations from game analysis, testing, and code review. Append new findings at the bottom.
---

# Rando Cal AI - Improvement Log

This file accumulates observations across sessions. Each entry should include a date, source (code review, game observation, testing), and actionable insight.

## Session 1 - 2026-03-03 (Initial Codebase Analysis)

### Architecture Observations

1. **Evaluator ordering matters**: CombinedEvaluator runs evaluators in fixed order (ForceActivation → Deploy → Battle → Move → Draw → CardSelection → ActionText → Pass). Scores are additive. If DeployEvaluator scores a card +80 and ActionTextEvaluator also matches it at +30, the total is +110. This means evaluator interactions can create unexpected score inflation.

2. **CHAOS_PERCENT is 0**: RandoConfig.CHAOS_PERCENT was reduced from 25% to 0%. This eliminated random exploration. While this makes Rando more predictable and "optimal," it may reduce its ability to discover non-obvious plays. Consider a small exploration rate (5-10%) for non-critical decisions.

3. **BattlePredictor uses 50 simulations**: Monte Carlo with 50 runs is fast but noisy. For critical battles (high stakes), consider increasing simulations or using the deterministic `predictBattleFullIntel()` when DeckOracle data is available.

4. **Loop detection is sophisticated but may over-block**: DecisionTracker blocks responses after just 2 repeats. In some game states, the same action genuinely IS the best play multiple turns in a row (e.g., force draining at a controlled location). The tracker may incorrectly penalize correct repeated play.

5. **No learning between games**: Rando starts fresh every game. There's no mechanism to track win/loss patterns, deck matchup statistics, or strategy effectiveness across games. A BotStatsDAO exists but is only used for recording, not informing decisions.

### Potential Improvement Areas

- **Force activation is always max early**: ForceActivationEvaluator always activates maximum in turns 1-3. Some decks benefit from slower activation (preserving reserve for destiny draws). Could be deck-strategy-aware.

- **Deploy location scoring is position-blind**: DeployEvaluator scores locations without considering the overall board topology. Deploying at a location that connects two friendly positions is strategically different from deploying at an isolated location.

- **No interrupt timing strategy**: The AI doesn't have sophisticated timing for when to play interrupts. It plays them as soon as it can, rather than waiting for optimal moments (e.g., saving Sense for high-impact opponent actions).

- **Destiny draw manipulation is basic**: When choosing destiny draws, the AI doesn't factor in what cards remain in the reserve deck (even though DeckOracle knows). It could calculate expected destiny values and choose whether to substitute.

### Known Issues in Current Code

- **V22.4 suicide block threshold**: 2x power AND > 6 total may be too aggressive. Blocks legitimate attacks at 4 power vs 8 power where we might have good destiny.
- **LOCATIONS FIRST exception list**: Only covers TDIGWATT, "I'm Sorry", and AMSD. Other location-search cards may exist.
- **ObjectiveHandler coverage**: Unknown how many objectives are fully mapped. Missing objectives means Rando may fail to play starting cards correctly.

---

## Template for New Entries

### Session N - YYYY-MM-DD (Source: game observation / code review / testing)

**What was observed:**
[Description]

**Why it matters:**
[Impact on Rando's play quality]

**Suggested fix or improvement:**
[Actionable recommendation]

**Files affected:**
[List of files that would need changes]

**Priority:** High / Medium / Low

## Session 2 - 2026-03-05 (V27 Move/Battle/Force Fixes)

### Changes Made

1. **V27 BUDDY PROTECT (MoveEvaluator)**: Prevents characters from moving away and leaving a vulnerable ally solo. Penalty -150 to -400 based on enemy threat.

2. **V27 MAINTENANCE RESERVE (PassEvaluator, MoveEvaluator)**: Conserves Force for Blizzard maintenance. +25 to +50 pass bonus when Force ≤ maintenance cost + 1. Also -80 move penalty.

3. **V27.1 DRAW THEIR FIRE awareness (BattleEvaluator, PassEvaluator, DeployEvaluator)**: Detects opponent's Draw Their Fire effect on table. Reserves Force for interrupt tax (1 Force per interrupt during battles they initiate). Ghhhk becomes unusable without Force reserve.

4. **V27.2 More permissive buddy protection (MoveEvaluator)**: Changed from requiring both power<6 AND ability<2 to just power<6 for moves. Thrawn (power 4, ability 4) was escaping the check.

## Session 3 - 2026-03-06 (V28 Critical Bug Fixes)

### Bug 1: Upper Walkway NEVER selected as starting site

**Root cause:** TDIGWATT objective creates an ARBITRARY_CARDS decision (not CARD_SELECTION) for site choice. ARBITRARY_CARDS uses temp IDs ("temp0", "temp1") instead of integer card IDs. The evaluateStartingLocation method called `Integer.parseInt(cardId)` which threw NumberFormatException for "temp0", silently caught, causing all card lookups to fail. The V24.10 exterior/interior scoring (+500/-500) never fired.

**Fix:** V28 adds blueprint-based lookup path. For ARBITRARY_CARDS decisions, uses parallel `blueprintIds` array and `lookupBlueprint()` to resolve card properties instead of `findCardById()`.

**Files:** CardSelectionEvaluator.java (evaluateStartingLocation method)

### Bug 2: Lando/Lobot deploying before Executor

**Root cause:** BESPIN-FIRST GUARD checked CC site keywords (e.g., "cloud city", "carbonite") in deploy action text. But character deploy actions don't include the target location name — action text is like "Deploy Lando from hand" not "Deploy Lando to Carbonite Chamber". So the CC keyword check never matched.

**Fix:** V28 rewrites the guard to block ALL non-exempt deploys on turns 1-2 when Bespin isn't occupied. Exemptions: location/site/system deploys, AMSD, Executor, starships. This ensures the deploy order is: locations → AMSD/Executor → characters.

**Files:** DeployEvaluator.java (BESPIN-FIRST GUARD block)

### Key Insight: ARBITRARY_CARDS vs CARD_SELECTION

ARBITRARY_CARDS decisions (from pile effects) use "temp0", "temp1" IDs with parallel blueprintId arrays. CARD_SELECTION decisions use integer card IDs. Any evaluator code that does `findCardById(Integer.parseInt(cardId))` will silently fail for ARBITRARY_CARDS. Always check decision type and use blueprint lookup as fallback.

### Bug 3: Reserve deploy bypasses buddy protection (V28.1)

**Root cause:** When card effects (like Dining Room's game text) deploy characters from reserve, the decision goes through CardSelectionEvaluator → evaluateUnknown, NOT through DeployEvaluator. So all the solo vulnerability checks in DeployEvaluator are bypassed. Lando was deployed from reserve to Dining Room alone, with no buddy protection.

**Fix:** Added V28 RESERVE SOLO PROTECT in evaluateUnknown. When choosing characters to deploy from reserve (detected by "deploy" + "reserve" in decision text), checks if any CC location has friendly characters. If the character would be alone, applies penalty (-100 to -350). Extra penalty if enemies are present or character is high-power.

**Files:** CardSelectionEvaluator.java (evaluateUnknown method)

### Bug 4: Force pile loss with Draw Their Fire active (V28.1)

**Root cause:** V25 Force loss logic treated Force pile loss as "OK" when life force was healthy (+10 score). But when Draw Their Fire is active, Force pile = available Force for playing interrupts (Ghhhk etc.). Losing from Force pile directly reduces interrupt-playing ability. After losing Force pile cards, Rando couldn't play Ghhhk despite having it in hand.

**Fix:** Added V28 DTF FORCE PILE PROTECT in evaluateForceLoss. Scans opponent's table for "Draw Their Fire". If found, applies heavy penalty for Force pile losses (-200 to -400, scaling with pile size). This redirects losses to reserve/hand, preserving Force pile for interrupt plays.

**Files:** CardSelectionEvaluator.java (evaluateForceLoss method)

### Key Insight: Reserve deploys bypass DeployEvaluator

Card effects that deploy from reserve (Dining Room, I'm Sorry, etc.) create ARBITRARY_CARDS decisions routed through CardSelectionEvaluator, NOT CARD_ACTION_CHOICE through DeployEvaluator. Any deploy-phase logic in DeployEvaluator (buddy protection, solo vulnerability, BESPIN-FIRST GUARD) is completely bypassed for reserve deploys. Must duplicate critical logic in CardSelectionEvaluator's evaluateUnknown.

## Session 4 - 2026-03-10 (V29.9 Hunt Down V Improvements)

### Source: Game replay analysis (ssogrx8pwtr303fh, b8canjubgmncrgwh)

### Changes Made

1. **V29.9 REBEL BARRIER RISK (BattleEvaluator)**: When Vader is at a battle location, calculates what happens if opponent Barriers Vader out. If remaining characters would be crushed (power deficit > 5), applies -150 to -350 penalty. Prevents suicidal battles where our strength depends on one key character.

2. **V29.9 LIGHTSABER DEPLOY PRIORITY (DeployEvaluator)**: When Vader is on table without a weapon and lightsaber is in hand, boosts lightsaber deploy to +400/+500. If character already armed, -100 penalty. If no matching character on table, -200 penalty. Ensures Vader gets equipped before being sent into danger.

3. **V29.9 I HAVE YOU NOW BATTLE PLAY (ActionTextEvaluator)**: IHYN detected by action text or source card during battle phase. When Vader is in battle: +300 mega boost. Without Vader: +100. Outside battle: -200 (save for later). Ensures IHYN is actually PLAYED during battles instead of sitting unused in hand.

4. **V29.9 CRUSH DUPLICATE PREVENTION (ActionTextEvaluator)**: When Crush The Rebellion would pull IHYN or Evader, checks if target is already in hand. If both in hand: -300. If one in hand and other not in reserve: -250. Prevents wasting Crush pulls on duplicates.

5. **V29.9 UNARMED VADER MOVE BLOCK (MoveEvaluator)**: Before weapon hunter logic, checks if Vader has no weapon. If lightsaber is in hand: returns -250 and blocks attack move (equip first!). If no saber available: -100 penalty (still risky without weapon).

6. **V29.9 HUNT DOWN BATTLE AGGRESSIVENESS (BattleEvaluator)**: When playing Hunt Down V with armed Vader in battle, adds aggressiveness bonus. +80 base, +200 if Luke is present. The whole point of Hunt Down is Vader fighting — he should seek combat.

7. **V29.9 HUNT DOWN FORCE DRAIN PRIORITY (ActionTextEvaluator)**: For Hunt Down V, boosts force drains by +30 (Visage adds +1 to each drain). Extra +40 at locations with 2+ opponent icons. Keeps pressure on opponent's life force.

### Key Insights

- **Rebel Barrier is devastating against concentrated power**: If Rando's battle strength depends mostly on one character (Vader), opponent can Barrier that character and crush the rest. Need either paired deployment (2+ strong characters) or recognition that battle is risky.
- **Weapon equip order matters**: Vader must get lightsaber BEFORE moving to engage. Without it, he's just power 6 with no weapon hit. With it, he's effectively power 11+ (base + hit + destiny throw + IHYN).
- **IHYN is the key battle multiplier**: 2-3 extra destiny draws during battle can swing ANY fight. But Rando was pulling it repeatedly (Crush) and never playing it. Source card detection needed for generic action texts.

### Files Modified
- BattleEvaluator.java (barrier risk, hunt down aggressiveness)
- DeployEvaluator.java (lightsaber deploy priority)
- ActionTextEvaluator.java (IHYN battle play, crush duplicate, hunt down drain)
- MoveEvaluator.java (unarmed Vader move block)

**Priority:** All High — these address the specific replay failures observed in Hunt Down V games.
