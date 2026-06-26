# Rando Cal AI — History Highlights

> Distilled from the two full source files, which remain authoritative in
> `originals/02-rando-history/`:
> - `AI_CHANGELOG.md` (~2336 lines) — every rule change, organized by user-facing impact.
> - `AI_VERSION_HISTORY.md` (~5358 lines) — every V-tag in version order, with full rationale.
>
> This file is the **arc**: major milestones, the most impactful V-tags, recurring
> themes, and superseded rules a new K-2 should know. For the exact rationale, replay
> IDs, and code locations behind any V-tag, go back to the originals. `git blame` on
> any `models/rando/` or `models/chosenone/` file jumps straight to the comment.

---

## The arc at a glance

- **Pre-tag era (V0, Jan 15 – Mar 16, 2026)** — Rando Cal was authored by **Snacks**, with V0-era improvements by **eric lanz**. ~16 untagged commits: initial AI scaffold (`AdvancedAi`, `BeginnerAi`, `HeuristicAiBase`, `SwccgAiController`), the `models/rando/` evaluator split, loop-detection groundwork, and the bot-stats / achievements system. Comments reference a "Ported from Python" prototype; if V1–V20 ever existed it was there.
- **V-tag convention starts at V21** (commit V29.13, 2026-03-16). The counter begins at 21 — there is no V1–V20 in the Java code. ("V15/V22/V23" elsewhere in GEMP git logs are SWCCG *card-set* numbers, not AI tags. Unrelated.)
- **179 tags catalogued total**, two parallel bots kept in sync: **rando** (dark) and **chosenone** (light). Most rules are "mirrored in chosenone."
- **Two co-authors of the tagged era**: the original Snacks/eric foundation, then a long Claude-assisted run driven by Steve's gameplay reports and replay forensics (V40 onward, heaviest May–June 2026).

### Rough timeline of the tagged work
- **V21–V38** (Mar–early dev): foundations — starting-effect bans, objective-location priority, buddy system, weapon limits, battle thresholds, force activation. Many were aggressive `-9999` hard blocks, later softened to graduated scoring.
- **V40–V67**: deck-specific strategy scripts (TDIGWATT, Hunt Down, Hidden Path, Skywalker Saga, Senate, Invasion) + the giant **V67 family** (~aa through ~bt, the densest replay-driven tuning era).
- **V70–V129** (late May): consolidation, type-by-API discipline, reserve-pull correctness, 4th-shield logic, detection-path mirrors (Deploy vs CardSelection vs ActionText).
- **V130–V184** (late May–June): the most strategic batch — unified forfeit/force-loss pickers, winnability gates, Deck Oracle dead-search prevention, persona-based detection, offensive force-banking.

---

## Recurring themes (the spine of the whole project)

1. **Type-by-API discipline (standing rule).** Every rule matches on `CardCategory`, `CardSubtype`, `Icon`, `Keyword`, `Species`, `Persona`, or a `Filter` — never a substring match of a generic noun against a card title. "Naboo: Theed Palace Hangar" doesn't contain "site," but `CardCategory.LOCATION + CardSubtype.SITE` matches it. Multiple bugs (V123, V177, V180) trace to a title-substring shortcut that should have used the API.
2. **Detection-path mirrors.** A rule that fires in `DeployEvaluator` often misses because GEMP splits a generic "Deploy" action into action-step + location-pick-step, which routes through `CardSelectionEvaluator` or `evaluateUnknown` instead. Recurring fix pattern: mirror the rule into the other path (V89→V89-CS, V99→V99-CS, V90→V122, V86→V121, V101→V119, V112→V117).
3. **Reserve-deck pulls must fire and must not waste.** A failed search reveals your reserve to the opponent. Whole sub-systems exist to (a) guarantee valid pulls fire (V60, V67ai, V82, V116 floor) and (b) block dead/unaffordable searches (V66, V67ac affordability, V177/V183 Deck Oracle).
4. **Don't suicide characters; commit to winnable fights.** From V38's buddy-path requirement through V137/V172 winnability gates to V181's drain-weighted fair-fight commit: don't deploy solo into danger, but don't be timid when the drain pays.
5. **Force economy: reserve correctly, then spend.** Maintenance budgeting (V22.3→V74, V59, V64), battle-interrupt reservation (V27, V176), and the late offensive **force-banking** idea (V182): the shortage decides the action.
6. **Graduated scoring over hard blocks.** Early rules leaned on `-9999`. The project trend (V39+) converts those to positive/graduated scores so a rule can be outvoted rather than dominating — except for true must-never actions (loops, suicide, illegal pulls).

---

## Most impactful V-tags (what each did + why it mattered)

### Foundations
- **V21** — Ban certain Effects as starting interrupts (turn 0). They can still deploy from hand turn 1+. (Tentacle, No Escape, Coarse and Rough later added.)
- **V22 / V24.4** — Locations deploy before characters; prefer your own objective locations. Opens drain pressure before committing bodies. The location-first principle recurs all the way to V162/V179.
- **V33 / V70 / V158** — One weapon per character, named weapon before generic. V70 made it universal across all effect-pull deploys; V158 unified V33+V67aq+V115 into a single weapon-deploy gate. ("No character should ever have two weapons" — Steve's standing rule.)
- **V38** — Reworked solo deploy: Vader/Emperor (ability 6+) deploy solo anywhere; everyone else needs a buddy at the site or a paired deploy hitting 7+ ability that turn.
- **V38.3 / V168 / V42 / V43** — Always activate Force, always reserve cards for destiny draws (removed an old Force-Pile cap of 20 that suppressed activation).

### Deck-specific strategy scripts
- **V23 / V40 / V46** — TDIGWATT: force Bespin-system establishment, Executor+Piett priority; **HOLD_BACK applies ONLY to TDIGWATT** (V40 scoped it after it wrongly starved other decks).
- **V29.13 / V35 / V36 / V41** — Hunt Down: Vader leaves the Castle to hunt Jedi, Inquisitor battle-destiny bonus, Hatred-loop awareness (Hatred requires an *Inquisitor*, not Vader alone — V35.7).
- **V52b / V53b / V67n** — Hidden Path / Fallen Order: flood Jedi turns 1–2, then the **mandatory crippled-Jedi transit** Mapuzo → Safehouse → Underground Corridor → off-Mapuzo to flip and restore. (Repeatedly re-fixed; the Corridor must outscore other moves or the Jedi rot and die.)
- **V54 / V61** — Skywalker Saga epic-event turn 1–3 script; "The Force Is Strong In My Family" picks the right branch per deck variant.
- **V83 / V88 / V99** — Senate deck: senators only at Galactic Senate (+1500 override so they leave hand); non-senators blocked from the Senate unless defensively needed.
- **V82 / V86 / V121** — Invasion: Blockade Flagship Neimoidian-pilot pull fires every game; Neimoidian pilots route aboard the capital ship, not vulnerable ground.

### Movement & drain economy
- **V37 / V73 / V85** — Don't surrender drain ground: penalize battleground→non-battleground retreats and higher-drain→lower-drain moves; hard-block abandoning an uncontested 3-drain site.
- **V53** — Undercover spy follows the opponent when they move, to keep blocking drain.
- **V111** — Exception to the "wrong direction" block: advancing from a non-battleground to an *adjacent* battleground is encouraged (+400). Fixed the "deploy to Imperial City but never move to Xizor's Palace" pattern.
- **V91** — Escape the landed-ship trap (power-0 ship at a site): prefer take-off or disembark-pilot.

### Reserve-pull correctness
- **V60 / V67h** — WILL_FAIL prediction: refuse searches that can't find a target.
- **V67ac** — Affordability guard: don't fire a pull you can't pay for (it fails AND reveals your reserve).
- **V114** — Deleted the obsolete V21 "Deploy ... from ... = -10" catch-all that was short-circuiting the +2000 location-pull bonus for an entire game.
- **V116** — Unconditional +100 floor for any reserve/`[download]` deploy option, as a safety net beneath all other reserve handlers.
- **V177 / V183** — Deck Oracle dead-search gate: a real player never searches for a card he knows isn't in his reserve. V177 parses pull targets and blocks when all are DEAD (none in reserve); V183 retools it to resolve targets by scanning the source card's text for the deck's own catalogued titles + live zone (caught "Fall Of The Legend searching for a Weather Vane already in hand"). **Supersedes the old "fire every turn, stop after 2 failures" heuristic for parseable pulls.**

### Battle, forfeit & force-loss
- **V22 (must-fight) / V24.7** — Enforce the 4-ability destiny threshold; use OpponentDeckTracker to simulate destiny + attrition and pick winnable battles.
- **V153 / V159 / V101 / V119** — Unified force-loss / forfeit picker: lose from Used > Reserve > Hand; characters worth more than small force losses. V159 implemented one unified forfeit picker (superseding scattered branches); V119 mirrored the zone-priority into the combined battle handler.
- **V118** — Save non-hit characters from small (≤2) battle damage; prefer pile-loss to forfeiting a body.
- **V137 / V172 / V173 / V176 / V181** — The winnability stack: gate aggression on a real affordable-wave projection of your whole hand; save the force needed to actually initiate the fight; and **commit to a close fight when the drain pays** (gap 1–3 power + drain ≥ 2 + favorable forfeit). Raw power decides who wins, but a small gap is a low-stakes coin flip you should take when the drain is worth denying.

### Detection & identity by API (lessons learned the hard way)
- **V123** — V66 was hard-blocking "Deploy a location from Reserve Deck" because no card is *titled* "location." Added a stopword list so it defers to typed Filter semantics.
- **V136** — Master deploy team-viability evaluator; later patched (2026-06-01) because the engine's power tally **excludes undercover characters**, so Rando piled into a site an opponent spy was secretly blocking.
- **V180** — Wielder detection by `Persona`, not title: "Young Skywalker" IS Luke (`Persona.LUKE`) but has no "luke" substring, so a weapon-pull guard blocked Luke's Lightsaber 12× in one game. Same lesson as the senator-in-lore rule: identity lives in the persona/lore, not always the printed name.

### Loop & safety net
- **DecisionTracker** — Detects 2/3/4-decision loops, adds randomness, then forces a different choice as the loop count rises. **V163** makes the cancel-loop veto dominate everything else. (V87 specifically killed a pilot↔passenger swap loop that ran 40+ times in one deploy phase.)
- **DecisionSafety** — Generates a valid response even when every evaluator returns nothing, so the bot never hangs on an unexpected decision type.
- **V165** — Bot-vs-bot stalemate breaker: turn-20 cap decided by life force (so self-play games terminate).

### Latest strategic ideas (V160–V186)
- **V162** — Locations deploy first (life-force gated); fixed an AMSD/Bespin deploy loop.
- **V166** — Contest the opponent's drain (deploy to break drain stalemates).
- **V170 / V171** — Undercover spy as a cheap drain-blocker; "deploy to contact" (don't deploy adjacent and march in next turn).
- **V175 / V178** — Protect battle interrupts and wielded weapons from the force-loss fodder pile (signature weapons kept dying with their carriers).
- **V182** — Offensive force-banking: if you have enough characters but not enough force to win a contested fight, *bank* the force instead of drawing it all into hand. The companion to V181 (V181 takes the fight winnable now; V182 banks for the fight winnable next turn).
- **V184** — Fire optional "when deployed, may reveal/retrieve" free-value triggers instead of passing (gated on the value actually existing — never fire a dead trigger on an empty reserve/lost pile).
- **V185** (2026-06-23) — Weapon-deployability gate: don't pull a weapon from Reserve unless an in-play character its OWN `getMatchingCharacterFilter()` accepts exists (Leia's Lightsaber → Leia/Ben/Rey ability > 4; Anakin's → Skywalker > 3; game-text weapons deferred). DeckOracle + DeployEvaluator. Stops the dead pull that reveals/reshuffles the reserve. rando only so far; chosenone mirror pending.
- **V186** (2026-06-23) — I Want That Map starting setup: name Starkiller Base SYSTEM (208_51) as the "other [Episode VII] location" (temp-safe +400 in `evaluateDeployLocation`, because the turn-0 pick arrives as temp IDs that throw before scoring) and The First Order Was Just The Beginning (214_12) as the starting Effect (+1000 in `evaluateUnknown`); supporting fragment in `ObjectiveAnalyzer`. The system has no battleground icon, so it MUST be named (a battleground heuristic misses it). rando only; chosenone mirror pending. NOTE: `ObjectiveHandler.OBJECTIVE_REQUIREMENTS` is dead code; the live objective brain is `ObjectiveAnalyzer`.

---

## Reverted / superseded / parked rules a new K-2 should know

- **V106 — dropped, then RE-ENABLED (2026-06-17).** The 4th defensive-shield slot stays CLOSED indefinitely; V106's Come Here You Big Coward / Simple Tricks trigger was originally dropped for firing on *any* opponent lightsaber. It was re-enabled tightly gated: fires only when the opponent is actually force-draining a *non-battleground* (the exact threat those shields cancel) and we occupy a BG while they occupy < 2. Priority A (Battle Order/Plan, V105) > C (Resistance/Ultimatum, V107) > B (Simple Tricks, V106). No new V-tag — Steve's standing rule is to adjust the old rule in place, not mint a new version.
- **V132 — DROPPED.** Was lowering the allow-opponent-activate baseline 50→10. Reverted: letting the opponent activate Force is normal SWCCG play, not a last resort. Original 50.0 baseline kept.
- **V133 — DROPPED.** Same-persona buddy bonus (+1000) caught only ~5% of cards via narrow game-text regex. The real buddy concept lives in V136's consolidated battle-math evaluator.
- **V29.7 — REMOVED (2026-05-26).** WMAOP Blockade-Flagship stipulations were misfiring on non-Blockade decks. Superseded by V142 deck-aware gating.
- **V152 — was parked, then shipped as V155** ("Welcome Home, Lord Tyranus: save for battle").
- **V158 supersedes V33 + V67aq + V115** (unified weapon-deploy gate). **V159 supersedes** scattered forfeit branches (wrapped in `if (false /* V159 SUPERSEDED */ ...)`).
- **V74 replaces V22.3** (maintenance cost satisfaction).
- **Several V67 sub-tags removed mid-family** (e.g. V67be removed V67y from a combined prompt; V67bk removed V52's +300 spend-force; V67bl removed V29's paired "solo OK" exception; V67bt removed a whole detection "Method 2"). The V67 family is the most heavily self-revised — trust the latest sub-tag's comment over earlier ones.
- **Many early `-9999` hard blocks were later softened to graduated scoring** (V39+). If you see an old hard block referenced, check whether a later tag converted it to positive-only.

---

## Practical notes for a new K-2

- Two bots, always kept in parity: **rando** (dark) and **chosenone** (light). When you add or read a rule, expect a mirror.
- Most fixes are **replay-driven**: a real game exposed a failure, the V-tag names the replay ID and the symptom. The originals preserve these — invaluable for understanding *why* a rule is shaped the way it is.
- When a rule "doesn't fire," suspect a **detection-path mismatch** (Deploy vs CardSelection vs ActionText vs evaluateUnknown) before assuming the logic is wrong.
- When matching cards, reach for the **type API or persona/lore**, never a generic-noun title substring. That single mistake caused V123, V177, V180, and the senator-keyword bugs.
