# Rando Cal & Chosen One AI Changelog

A version-by-version history of AI improvements. Every version is applied to
both Rando (`rando/evaluators/`) and Chosen One (`chosenone/evaluators/`)
unless explicitly noted.

The goal of this log is to be readable at a glance: *what got better,
where is the code, and why did it change*.

---

## V57 — Remove force activation throttling (2026-04-16)

**File:** `ForceActivationEvaluator.java` — both bots

`calculateActivationAmount()` used to apply three throttling rules *before*
logging `"V52 ACTIVATE 100% (no throttling)"` — a misleading message,
because Rules 1 and 3 had already clamped `amount`:

- **Rule 1** reserved 4 cards for destiny draws → once Reserve Deck got
  low, `amount` collapsed to 0.
- **Rule 3** (life force < 6) clamped to `max(1, 6 - currentForce)`.

In the 2026-04-16 replay, this caused Rando to activate **1 of 14** Force
generation on Turn 9, effectively conceding the turn. V43 forced the
minimum of 1 but that didn't help — Rando needs *all* the force to deploy,
drain, and fight.

V57 removes all three rules. `calculateActivationAmount()` now returns
`maxAvailable` directly. Log message is now accurate:
`"V57 ACTIVATE FULL: activating N (reserve=.., forcePile=.., ..)"`.

---

## V56 — Close mid/late-game deploy-urgency gap (2026-04-16)

**File:** `DeployEvaluator.java` — both bots, inside the V38.4 DEPLOY URGENCY
block.

Previously the urgency bonus was 0 whenever `handSize < 9`. Once Rando
drained its hand to ~8 cards mid-game, default scores crashed below the
positive threshold and Rando stopped deploying entirely.

V56 adds baseline urgency at small/mid hand sizes:

| handSize | Bonus |
|---|---|
| ≥ 12 | +200 + (handSize - 12) * 50 |
| ≥ 9 | +100 + (handSize - 9) * 30 |
| ≥ 5 | **+80 (new)** |
| ≥ 1 | **+50 (new)** |

Plus: `availableForce ≥ 6 && 1 ≤ handSize < 8` adds another **+80** to
burn off unused force when hand is small.

---

## V55 — High-ability character deploy urgency (2026-04-16)

**File:** `DeployEvaluator.java` — both bots, new block before V52b.

Side-agnostic, deck-agnostic. Any character in hand with `ability >= 6`
(Jedi / Sith / Lord tier — Vader, Emperor, Obi-Wan, Yoda, Luke Jedi
Knight, Mace, Palpatine, Dooku, etc.) gets a steady deploy-urgency bonus:

- T1–3: +500
- T4–6: +350
- T7+:  +200

Generalizes the earlier "Obi-Wan in hand" idea. In the 2026-04-16 replay
Rando pulled Obi-Wan Kenobi (V) to hand on Turn 3 via Rebel Leadership
and then **never deployed him** because default scoring saw "we already
occupy these locations → spreading-too-thin penalty". V55 overrides that
for impact characters.

---

## V54 — Skywalker Saga Epic Event T1-3 script (2026-04-16)

**File:** `DeployEvaluator.java` — both bots, new block before V52b.

Mirror of the V52 TDIGWATT T1 block, but for the Skywalker Saga Epic
Event deck (also known by its key effect "Like My Father Before Me").

### Priorities (turn-scaled: T1=100%, T2=85%, T3=70%)

| Tier | Card | Base bonus |
|---|---|---|
| 1 | Tatooine: Cantina / Mos Eisley / Lars' Moisture Farm | **+1500** |
| 1b | Any other Tatooine battleground site (non-Jabba) | +1300 |
| 1c | Tatooine system | +900 |
| 2 | Young Skywalker | **+1200** |
| 2b | Any other Luke persona (character) | +1100 |
| 3 | Luke's Lightsaber (from hand) | **+1100** |
| 4 | Obi-Wan / Yoda (character) as buddy | +800 |

Lightsaber-from-Reserve pullers (Gift Of The Mentor) are **not** boosted
here — that effect is a BATTLE combo (Obi-Wan/Yoda buddy → +2 destiny),
not deploy tempo.

### V54.1 — Fix detection gate

Skywalker Saga is an *Epic Event* deck. Its objective-slot card is
Anger/Fear/Aggression (V), which has `cardType=EFFECT` not `OBJECTIVE`.
`ObjectiveAnalyzer.findOurObjective()` filters by `CardCategory.OBJECTIVE`,
so `getObjectiveTitle()` returns null — V54's original title-based gate
never matched.

Detection is now by starting-location signature: iterate
`gameState.getLocationsInOrder()` and look for
`"anakin's funeral pyre"` (217_34). Unique to this deck.

---

## V53 — Spy asset tracking, Vader flip, Executor, objective-first

**Files:** `DeployEvaluator.java`, `MoveEvaluator.java`,
`ActionTextEvaluator.java`, `CardSelectionEvaluator.java`, `ShieldStrategy.java`

- Spy reserve: reserve 1 Force per undercover spy for next turn's movement
- Vader flip: +900 deploy to opponent battleground (Hunt Down objective flip)
- Cloud City army pre-flip: +500
- Objective-first bonus: +300
- Executor: +800 all turns
- Spy follow opponent: +500 move; -300 don't leave; +400 reposition
- Break cover — flip own spy +500 when friendly character present, -500 when not
- Grabber self-grab: hard blocked (-9999)
- "Stack Jedi here" on Fallen Order: +500 (save Jedi Survivors)
- Shield strategy: A Tragedy / Allegations always first; Battle Order/Plan
  downgraded to SITUATIONAL_HIGH, min Turn 2

### V53b — Hidden Path transit

`MoveEvaluator.java`: MANDATORY Hidden Path Jedi transit:
- Safehouse → Corridor: `setScore(9999)`
- Corridor → outward: `setScore(9999)`
- Corridor → Safehouse: `setScore(-9999)`

### V53c — Wokling search block early

`ActionTextEvaluator.java`: Wokling Effect search blocked Turns 1-3
(early check before V29.7).

---

## V52 — TDIGWATT T1 script, spend-force, momentum, activation

- `DeployEvaluator.java`: TDIGWATT Turn 1 script (Bespin +1500, CC site
  via I'm Sorry +1200, Lando Broker +1000, Executor +900, Chiraneau +850)
- `DeployEvaluator.java`: Spend all force (+300 when force pile > 3)
- `DeployEvaluator.java`: Deploy momentum (+100/150/200 for multiple
  deploys same turn)
- `ActionTextEvaluator.java`: Drain under Battle Order after Turn 3
  (was blocked entirely)
- `ActionTextEvaluator.java`: Surprise Assault self-cancel hard block
  (-9999)
- `ForceActivationEvaluator.java`: removed *some* throttling (message
  said "100% no throttling" but Rules 1 and 3 remained — see V57 for
  the complete removal).

### V52b — Hidden Path Jedi flood

`DeployEvaluator.java`: Jedi deploy priority turns 1-2 for Hidden Path
objective. Ability ≥ 6 for true Jedi; action-text detection for "jedi
survivor". +800 Jedi character, +700 lightsaber, +600 holocron.

---

## V51 — Contest drain, buddy system, spy deployment, Vader flip hunt

- `DeployEvaluator.java`: Drain 2+ sites are THE battleground
  (+500 stack, +600 contest drain 3+, +500 contest drain 2+)
- `DeployEvaluator.java`: Spy deploy +1000 at opponent drain 2+ sites
- `DeployEvaluator.java`: Buddy system bonuses (+400 ability ≥ 4,
  +500 ability ≥ 7)
- `DeployEvaluator.java`: Spy power counted as potential power at
  locations
- `DeployEvaluator.java`: Vader flip aggressive — deploy Vader from hand
  to opponent battleground for instant Hunt Down flip
- `CardSelectionEvaluator.java`: Don't target already-hit characters
  with weapons (-500)
- `CardSelectionEvaluator.java`: Kill opponent spy with Force Lightning/
  Trample (+500)
- `CardSelectionEvaluator.java`: Battle Order shield — don't play
  without system+site occupation
- `DeployEvaluator.java`: Battle Order / Battle Plan shield gate

---

## V50 — Early deploy danger penalty (turns 1-3 only)

`DeployEvaluator.java`: if deploying a character would leave us at lower
power than the opponent at that location on turns 1-3, apply a penalty.
Prevents Rando from committing to hopeless battles before it has built
board.

---

## V49 — Wild Karrde protection + deploy power-disadvantage

`MoveEvaluator.java`: starship landing at a site without passengers =
-9999 hard block (Wild Karrde landing at Mos Eisley with power 0 = free
kill for opponent).

`DeployEvaluator.java`: deploying to an opponent-occupied location where
our total power after deploy would be < opponent power - 3 = -200
penalty + skip.

---

## V47-V48 and earlier

See `git log --oneline` prior to 2026-04-16 for commit-level history of
V33-V48. Highlights:

- V47-V48: Bot fixes + MCP game client for live Claude play
- V43: Spy deployment target logic, shield check, location-first pull,
  starting interrupt preference, stalemate concede
- V38: Activation + deploy urgency
- V37: DeckOracle integration + zone-aware search validation
- V35: Hunt Down deck strategy
- V33: Weapon limits + buddy system
