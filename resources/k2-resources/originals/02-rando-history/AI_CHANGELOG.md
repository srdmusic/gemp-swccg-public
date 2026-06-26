Rando Cal / Chosen One AI — Change Log (by category)
=====================================================

User-facing summary of every AI rule change, organized by impact.
Reads cleanly in any plain-text editor.

Each entry shows the V-tag that exists in code comments. Use
`git blame` on any `models/rando/` or `models/chosenone/` file to
jump straight to the rationale.

For the complete version-by-version list of all 179 V-tags, see:
  AI_VERSION_HISTORY.md

The V-tag counter starts at V21. Rando Cal was first committed on
2026-01-15, then existed for two months of untagged development
before the V-tag convention started with the V29.13 commit on
2026-03-16. V21 through V29.12 were added to code at or after that
point. The pre-V21 development is documented from git history in
Section 12 below ("PRE-TAG ERA").

(Source-code comments reference "Ported from Python" for files
like DecisionTracker.java and DecisionSafety.java, suggesting an
earlier Python prototype existed before the Java code. If V1-V20
ever existed, it would have been in that prototype, not here.)


1. OBJECTIVES THE BOT CAN NOW PLAY PROPERLY
-------------------------------------------

Each of these decks previously had the bot deploy cards without
actually pursuing the win condition. They now have dedicated logic.

  The Hidden Path / Fallen Order (1 Rey)
    Tags: V53b, V67n, V67z (V67z updated 2026-06-18)
    The three Jedi survivors deploy crippled at Mapuzo (power 3,
    forfeit 3, game text canceled) and MUST transit out — Safehouse
    -> Underground Corridor -> off-Mapuzo — to flip the objective and
    restore themselves. 2026-06-18: fixed the bot passing instead of
    moving them. The "wrong direction" penalty (go defend the drain
    elsewhere) was burying the mandatory Corridor move, so the Jedi
    rotted at Mapuzo and got slaughtered. Now the Corridor transit hub
    is exempt from that penalty, so they move out as intended. Don't
    battle with them while they're crippled at Mapuzo. Also (2026-06-18
    step 2): the off-Mapuzo move costs 1 Force per Jedi, so the bot now
    reserves 1 Force per Jedi sitting at the Corridor — otherwise it
    spent everything and stranded them there.

  TDIGWATT (This Deal Is Getting Worse All The Time)
    Tags: V23, V24.10, V24.15, V40, V46
    Forces Bespin-system establishment. Executor + Piett priority.
    Never holds back during turns 1-2 build phase.

  Hunt Down V
    Tags: V29.13, V35, V36
    Vader leaves the Castle to hunt instead of camping there.
    Inquisitor battle-destiny bonus. Vader-group movement + Hatred
    loop awareness.

  Skywalker Saga
    Tag: V54
    Turn-1 to turn-3 deploy script for the Skywalker family.

  The Force Is Strong In My Family
    Tag: V61
    Picks the right branch (My Father Has It / I Have It / You
    Have That Power, Too) based on the deck variant in play.

  Skywalker required Effects
    Tag: V80
    Deploys A Good Friend + A Cunning Warrior when offered.

  Verge of Greatness (Death Star → Scarif)
    Tag: V79
    Moves Death Star toward parsec 7. Picks the Scarif orbit
    option when offered.

  My Lord, Is That Legal? (Senate deck)
    Tags: V83, V88
    Senators only at Galactic Senate (weapon destiny -6 protects
    them). +1500 bonus overrides solo-deploy penalty so senators
    actually leave hand and contribute to the flip condition.

  Invasion
    Tags: V82, V86
    Blockade Flagship Neimoidian-pilot pull fires every game.
    Neimoidian pilots route aboard the capital ship instead of
    vulnerable ground sites. Begin Landing Your Troops' docking
    bay pull fires every turn.


2. DEPLOY DECISIONS
-------------------

  V22, V24.4 — Locations deploy before characters
    Opens drain pressure before committing characters.

  V22.3 — Maintenance reserve uses actual upkeep cost
    Reserves each card's full maintenance cost, not just 1 force.

  V38 — Buddy-path requirement for low-power characters
    Vader and Emperor (ability 6+) deploy solo anywhere. Other
    characters need a buddy at the site, or a paired deploy that
    hits 7+ ability that turn.

  V51 — Reinforce bonuses for joining existing friendlies
    +200 to +500 for stacking at a battleground with friendlies
    already there.

  V55 — High-ability characters deploy turn 1
    Rey, Vader, Yoda, etc. get a strong push to come down
    immediately.

  V59 — Holistic maintenance budget across all planned deploys
    Calculation accounts for everything you plan to deploy this
    turn, not just what's already on the table.

  V64 — Tighter maintenance budget
    Accounts for opponent drains and Visage losses on top of the
    base maintenance cost.

  V89 — Dr. Evazan needs an armed friend
    No more solo-Evazan deploys to a site he can't survive at.

  V90 — No solo deploy near an enemy weapon
    Catches the "Phasma deployed next to Leia's Lightsaber"
    failure. Requires a friendly armed character at the site, or
    picks a different deploy.

  V113 — Solo ability-3+ character deploy penalty (-300)
    Any character with ability 3 or higher deployed alone to a
    site (no other friendly characters already there) takes a
    -300 penalty. Prevents the "Dengar alone at Xizor's Palace →
    Anakin + Chewie overwhelm him" pattern. Applies to both Rando
    and Chosen One. V29.5 already blocked solo deploys to opponent
    locations; V113 extends this to own and neutral locations.

  V115 — Weapon-deploy criteria awareness (hard block second weapons)
    Previous V67aq counted ALL armed/unarmed characters when
    scoring a weapon deploy. It allowed the deploy if ANY friendly
    was unarmed — even when that friendly couldn't legally take the
    weapon. Result: 2nd Dark Jedi Lightsaber V landed on Vader
    (the only "warrior of ability > 4" target, already armed)
    because Tarkin was unarmed (but Tarkin isn't a warrior of
    ability > 4 so the weapon couldn't deploy on him anyway).
    Vader ended up with two lightsabers in the replay
    ig4n5m5nzc4gronn (2026-05-21).
    V115 parses the weapon's "Deploys on X" criteria from its game
    text (using V70's existing helpers, now package-visible) and
    counts ONLY characters that match that criteria. Hard-blocks
    -9999 when criteria is parsed AND no criteria-matching UNARMED
    friendly exists on table. Two scenarios covered:
      (a) every eligible target already armed → 2nd weapon attempt
      (b) no eligible target on table at all → weapon would orphan
    Applies to both Rando and Chosen One DeployEvaluator. Steve's
    standing rule, fourth time asked: "No character should ever
    have two weapons."


3. FORCE-DRAIN ECONOMY (offense and defense)
--------------------------------------------

  V37 — No retreat from battleground to non-battleground
    -800 penalty for moves that surrender drain ground.

  V111 — Advance from non-battleground to adjacent battleground (+400)
    Exception to the V38.3 "wrong direction" hard block. When a
    character is at a non-battleground site and the destination is
    a battleground, the move is ENCOURAGED (+400) rather than
    blocked. Covers the "deploy to Imperial City via Reserve Deck
    pull → move to adjacent Xizor's Palace (battleground) to
    force drain" pattern in Agents of Black Sun.

  V53 — Spy follows opponent who moves
    Keeps reducing opponent's drain wherever they go.

  V73 — Drain comparison on moves
    Penalizes moving from a higher-drain site to a lower-drain
    site. Recognizes the Cantina ↔ Mos Eisley shuttle as +400
    (preserves both drain values).

  V85 — Hard block for uncontested-source low-drain moves
    If we're uncontested at a 3-drain site and the best adjacent
    site has lower drain, the move is blocked outright.

  V91 — Escape landed-ship trap
    When a friendly starship is landed at a site (power 0), the
    bot prefers "take off to system" (+800) or "disembark pilot"
    (+600) on its next move.


4. RESERVE DECK SEARCHES (won't fail or waste)
----------------------------------------------

  V60, V67h — WILL_FAIL prediction
    The bot refuses searches that won't find a target. Failed
    searches reveal the Reserve to opponent.

  V24.10 (AMSD = Alert My Star Destroyer!) — Failed pull tracking
    Stops re-trying a failed search on the same turn.

  V82 — Site / docking-bay / system pulls get priority
    "Deploy a site / location / battleground / docking bay from
    Reserve Deck" scores +2500.

  V116 — Guaranteed +100 floor for any deploy-from-reserve option
    Per Steve: any action whose text indicates a Reserve Deck deploy
    or [download] gets an unconditional +100 baseline at the TOP of
    evaluateActionText, before any other rule runs. Safety net —
    even if downstream V60 / V67ai / V82 handlers fail to score this
    action for any reason, the floor guarantees the AI sees it as a
    positive option. The user-facing rationale: "The game gives
    players an option to deploy anything from reserve deck should be
    +100 at least... Not sure why they aren't firing when they are
    lit up green as options to deploy." Mirrored in chosenone.

  V114 — Removed broken "Deploy from reserve - risky" catch-all
    Pre-V114 the V21 catch-all `else if (actionText.contains("Deploy")
    && actionText.contains("from"))` matched ALL "Deploy X from Y"
    action texts (Reserve Deck, Lost Pile, Used Pile, etc.) and gave
    them -10. Because it appeared BEFORE the V60/V67ai block in the
    else-if chain, it short-circuited the chain and prevented the
    +2000 OBJECTIVE-tier location-pull bonus from ever firing for
    generic action texts like "Deploy a location from Reserve Deck".
    Replay dc8n6dl9s88rqycz (Hunt Down V, 2026-05-12): Rando ignored
    the objective's once-per-turn site pull for the entire game.
    The original V21 author already knew the penalty was too aggressive
    (V29.13 dialed it from -30 → -10) but left the block in place. By
    now the real safety net lives downstream: V60 guards (DeckOracle
    reveal-risk, fail-count tracking), V66 memory audit, V67ai tiered
    positive scoring, and V82 source-card site-pull matching. All four
    use real data instead of a blanket guess. V114 deletes the
    obsolete catch-all in both rando and chosenone.

  V82.2 — Multi-word specialty pulls
    "Neimoidian pilot" (Blockade Flagship), "Imperial admiral"
    (Endor Shield) and similar combination filters now resolve
    correctly. Was previously hard-blocking valid pulls.

  V82.3 — Strip parens / brackets from parsed targets
    So docking-bay pulls work even when the source text has
    "[Episode I] (or Coruscant) docking bay" formatting.

  V83.1, V86.1 — Null-target guard
    Penalty only fires when the target location IS identifiable in
    action text. Avoids blocking generic "Deploy" actions before
    the location-pick step runs.


5. BATTLE DECISIONS
-------------------

  V22 (must-fight) — 4-ability threshold strictly enforced
    Bot won't initiate battle without enough ability to draw
    destiny (SWCCG rule).

  V24.7 — Battle predictor with opponent-deck intel
    Uses OpponentDeckTracker to simulate destiny draws and
    attrition. Picks battles it can actually win.

  V70 — Universal one-weapon-per-character rule
    Applies to ALL effect-pull deploys, not just specific
    scenarios.

  V81.1 — Defender weapons-segment manual control
    The auto-pass UI no longer skips your fire-weapon decision
    when you defend during opponent's drain.

  V112 — Battle Order / Battle Plan defensive shield gate
    (evaluateUnknown path)
    V51 already blocked Battle Order / Battle Plan from being chosen
    as a defensive shield when Rando doesn't occupy both a battleground
    SYSTEM and a battleground SITE. But V51 only fires in the explicit
    `evaluateDefensiveShieldSelection` path. K&D "play a card" with a
    mixed stack (shields + non-shields) routes through
    `evaluateUnknown` instead — V51 was bypassed and Rando played
    Battle Order without meeting the occupation prerequisite (Agents
    of Black Sun replay, 2026-05-20). V112 mirrors the V51 gate in
    `evaluateUnknown` with -9999 when site+system battleground
    occupation isn't satisfied. Applies to both rando and chosenone.

  V121 — V86 Neimoidian-pilot CardSelection mirror
    V86 (Invasion: Neimoidian pilots must deploy aboard capital ship)
    lives in DeployEvaluator with V86.1's "identifiable target" guard.
    Generic "Deploy" actions split into action + location-pick; V86
    silently skips when actionText doesn't contain "aboard"/" to "/
    " on ". V121 mirrors the rule into CardSelectionEvaluator at the
    location-pick step: under Invasion objective, if the deploying
    card is a Neimoidian + pilot AND a friendly capital ship is on
    the table, candidates other than the capital ship score -1500;
    capital-ship match scores +300. Same pattern as V99-CS / V89-CS.

  V126 — Expanded starting-effect bonuses (V126a/b/c)
    Per Steve: "Evil Is Everywhere should deploy if Revenge of the
    Sith on table. First Strike is a good choice. Any effect that
    adds to force generation should get a bump."
    Three new positive-scoring rules at the V22 starting-effect
    block, all type-by-API where possible:
      V126a: +500 when game text contains "initiate battles for
        free" (catches First Strike V), or title contains
        "first strike" (catches plain First Strike).
      V126b: +400 when game text matches "force generation +N"
        regex or contains "force generation" + "+". Bumps the
        previous V22 +25 partial bonus on the same condition.
      V126c: +600 when game text references "[Episode I]" + "Dark
        Jedi" restrictions AND Revenge of the Sith is on table.
        Captures Evil Is Everywhere ↔ ROTS pairing.
    Mirrored in chosenone.

  V22 addendum (2026-06-19) — Shadow Collective payoff starting-effects
    Per Steve: "You'll Be Dead and Inconsequential Losses effects
    should be added to the list of effects to play when given the
    option as effects pulled from a starting interrupt."
    You'll Be Dead! (per-battleground blaster drain) and
    Inconsequential Losses (weapon recycling) are now title-matched
    and given +500 in the V22 PREFERRED STARTING EFFECT block, so
    Rando picks them when the starting interrupt offers Effects from
    Reserve Deck at turn 0. Previously You'll Be Dead! scored only
    +130 (V22 misread it as a location-puller) and lost the slot.
    Extends the existing V22 list (not a new version). Gated to
    turn <= 0, mirrored in both routing paths and in chosenone.

  V125 — V120 exact-match fix (.equals → .contains)
    V120's title-equality check looked for "vader's lightsaber"
    but the real card title is "•Darth Vader's Lightsaber (V)"
    (bullet + Darth prefix + (V) suffix). Silent miss. V125
    switches to bidirectional contains() match. Mirrored in
    chosenone.

  V124 — K&D parent-action hard-block at 4th slot
    V105/V107 correctly hard-block 4th-slot candidates at -5000
    in the sub-decision, but the K&D parent "Play a card"
    action was +50, so the AI committed to playing K&D and was
    forced to pick the least-bad shield from the stack. V124
    counts friendly shields on table at the parent-action step;
    if 3+ AND ShieldStrategy.prefers4thSlot() returns null, the
    parent action gets -3000. AI never starts the sub-decision.
    Mirrored in chosenone.

  V123-DEPLOY — V66 stopword guard (DeployEvaluator copy)
    The original V123 only patched ActionTextEvaluator's V66 block.
    A SECOND V66 block lives in DeployEvaluator (it scores reserve-
    pull actions for the engine's standard "Deploy" action path)
    and was still firing the old dumb keyword lookup. Confirmed via
    log evidence in the next replay:
      V67bg RESERVE OK: typed filter for 'location' has matches —
        pull valid
      V66 MEMORY WILL_FAIL: No match for 'location' in RESERVE_DECK
        — search will FAIL (-9999)
    V67bg said locations exist, V66 immediately after said no card
    titled "location" exists. Both run against the same action.
    V123-DEPLOY adds the same stopword list to DeployEvaluator's
    V66 block so it defers to V67bg's typed Filter semantics
    (CardCategory.LOCATION / CardSubtype.SITE etc) — the actual
    SWCCG type system, not a literal title lookup.
    Mirrored in chosenone DeployEvaluator.

  V123 — V66 stopword guard (generic category words)
    V66 MEMORY AUDIT was hard-blocking "Deploy a location from
    Reserve Deck" at -9999 because it extracted "location" as
    a literal card title to look up — no card is titled
    "location", so it assumed search would fail. Same problem
    for "site"/"weapon"/"lightsaber"/etc. V123 adds a stopword
    list of generic category nouns so V66 defers to V67ai/V82's
    criteria-aware validation. Mirrored in chosenone.

  V122 — V90 no-suicide-deploy CardSelection mirror
    V90 (no solo deploy to site with enemy armed char + no friendly
    weapon — the Phasma-at-Shield-Control suicide pattern) lives in
    DeployEvaluator with the same actionText-contains-location-title
    requirement. Generic "Deploy" splits the decision; V90 misses the
    location-pick step. V122 mirrors V90's logic to the location-pick
    candidates: for any CHARACTER deploy candidate, scan all cards at
    that candidate location; if enemy-armed > 0 AND friendly-armed ==
    0, penalize -1500. Closes the architectural gap noted in the K2
    rule audit's "Detection-path mismatches" category.

  V119 — V101 mirror into combined battle handler
    V101 (Used > Reserve > Hand zone priority) lived in the standalone
    `evaluateForceLoss` handler only. The COMBINED "Choose Force to
    lose or a card from battle to forfeit" decision routed through
    `evaluateForceLossOrForfeit`, which had no zone-priority logic —
    so battle-end losses came out in mixed order. Hunt Down replay
    ig4n5m5nzc4gronn last turn: 4 hand losses interleaved with
    reserve + used. V119 mirrors V101's +500 used / +300 reserve /
    -500 hand bonuses into the combined handler's force-loss branch.
    Same audit's "detection-path mismatch" category as the V99→V99-CS
    pattern. Mirrored in chosenone.

  V120 — Universal weapon-pull criteria block (ActionTextEvaluator)
    Fifth gap that V67aq / V70 / V67ar / V67ay / V115 all missed:
    Effect / Interrupt / Objective top-level actions that deploy a
    weapon FROM RESERVE (e.g. "Deploy Vader's Lightsaber from Reserve
    Deck using •I Am Your Father (V)"). These score in
    `ActionTextEvaluator.evaluate()` and never reach DeployEvaluator
    or CardSelectionEvaluator's weapon paths.
    V120 parses the weapon title from the action text via regex
    "Deploy <NAME> from reserve", finds that card's blueprint
    anywhere in Rando's known cards (gameState.getAllPermanentCards
    covers all zones), extracts the weapon's "Deploys on X" criteria
    using CardSelectionEvaluator.v70ExtractDeployCriteria, and counts
    criteria-matching armed/unarmed friendlies. Hard-blocks -9999
    when matchingUnarmed == 0 (every eligible target armed OR no
    eligible target on table at all). Stops "Vader's Lightsaber via
    IAYF" attempts when Vader is already armed or not in play.
    Same V70 helpers reused as V115. Mirrored in chosenone.

  V118 — Save characters from small battle damage (≤2)
    Per Steve: "Don't forfeit guys from site if battle damage is 2
    or less. Unless they are hit of course. Characters are typically
    worth more than 2 force to save from dying."
    In evaluateForceLossOrForfeit (the combined Force-loss OR forfeit
    decision in battle), when battle damage remaining is 1 or 2 AND
    no attrition is remaining:
      - force-loss options get +200 (prefer reserve/hand/used-pile
        loss over forfeiting a character)
      - non-hit CHARACTER forfeit options get -500 (save the character)
    Attrition damage still MUST be satisfied by forfeit, so this only
    fires for pure-damage situations. Hit characters skip the penalty
    (their forfeit value is 0 anyway from the weapon hit). Strengthens
    the earlier V67bh / V67t partial protection — V118 covers ALL
    characters regardless of forfeit value, where V67bh only fired for
    fv ≥ 4 and V67t for fv ≥ 2. Mirrored in chosenone.

  V117 — Universal 4th-shield hard block (evaluateUnknown path)
    V112 covered Battle Order / Battle Plan in the evaluateUnknown
    path. V117 generalizes the same fix to ALL defensive shields.
    When 3+ friendly defensive shields are already in play AND K&D
    (or similar) offers to deploy another from a mixed stacked pile,
    the candidate is hard-blocked -9999 unless
    ShieldStrategy.prefers4thSlot() returns this exact shield title.
    Mirrors V105/V107's 4th-slot logic from the defensive-shield-
    selection path into evaluateUnknown so the same conditions apply
    no matter which code path the AI takes. Per Steve: "We need to
    hard block deploy from K&D effect when 3 shields already on
    table. The conditions we set for that fourth shield must be met
    before deploying." Applies to both rando and chosenone.


6. CARD-SPECIFIC FIXES
----------------------

  Tentacle (V)
    Banned from turn-0 starting-interrupt pick when better Effects
    are available.

  No Escape / Coarse and Rough
    Banned from turn-0 starting-interrupt pick.

  Knowledge And Defense (V)
    Stacked cards properly recognized as available for "If a
    Skywalker Effect on table" triggers.

  Cloud City Occupation / Dark Deal (V)
    Only deploys when Bespin is actually occupied (otherwise
    self-cancels).

  Sidious + Vader pair
    Deploy + Force Lightning combo coordinated properly.

  Squabbling Delegates
    Recognized as the senator uploader for the Senate deck.

  My Sister Has It (V95, queued)
    Saved as force-loss fodder when its upload targets (Chief
    Chirpa's Hut or Guest Quarters) are already on the table.


7. UX / FRAMEWORK FIXES
-----------------------

  V81 — Auto-pass disabled during your turn (old GUI)
    Shows a manual Continue/Pass button instead of a silent
    timeout. You don't miss your own deploy/battle decisions.

  V81.1 — Defender weapons-segment respects engine signal
    Manual Pass shown when `autoPassEligible=false`, so the bot
    doesn't auto-skip a meaningful decision.

  V87 — Pilot ↔ passenger capacity-slot swap loop blocked
    Hard-blocks both swap action variants. Sil Unch was swapping
    40+ times in a single deploy phase before this fix.


8. LOOP AND SAFETY NET
----------------------

  DecisionTracker
    Detects 2 / 3 / 4-decision loops. Adds randomness, then forces
    a different choice as the loop count rises.

  DecisionSafety
    Generates a valid response even if all evaluators return
    nothing. Prevents the bot from hanging on unexpected decision
    types.


9. TYPE-BY-API DISCIPLINE (standing rule)
-----------------------------------------

Every rule uses CardCategory, CardSubtype, Icon, Keyword, Species,
or a Filter. None of the rules substring-match generic type words
like "weapon" or "location" against card titles.

This matters because "Naboo: Theed Palace Hangar" doesn't contain
the word "site", but CardCategory.LOCATION + CardSubtype.SITE
matches it correctly.


10. QUEUED (not yet in this PR)
-------------------------------

  (None currently queued. V95-V97 are now in code.)


V95-V97 ACTIVE RULES (added 2026-05-20)
---------------------------------------

  V95 — Save dead interrupts in hand
    Source: ActionTextEvaluator.java
    If an INTERRUPT's pull/upload target(s) are ALL already on
    the table AND reserve force (force pile + used pile + reserve
    deck) >= 15, penalize playing the interrupt (-2000). Keep it
    in hand for force-loss fodder. Example: My Sister Has It
    uploads Chief Chirpa's Hut or Guest Quarters — when both are
    on the table, the upload effect is dead.

  V96 — Concentrate at contested sites
    Source: DeployEvaluator.java
    For character deploys: if the target location has opponent
    power > 0, score the deploy. If our power - their power is
    within +/-10 (close battle) → +500 bonus for piling on
    (overflow battle damage). If we already lead by > 10 → +100
    (finish them). Uncontested sites left to V67al's spread
    penalty.

  V97 — Pull from Reserve before activating Force
    Source: ActionTextEvaluator.java
    During Activate Phase, if the action is a Reserve Deck pull
    from an Effect / Epic Event / Interrupt / Objective source,
    score it +1500. Beats the default "activate force" action.
    Force activation moves cards from Reserve into Force Pile
    where pulls can't reach them. Excludes Knowledge And Defense
    (pulls from stacked cards, not Reserve).


V98-V107 ACTIVE RULES (added 2026-05-20)
----------------------------------------

  V98 — Log silent /hall 500 catches + restore ImageProxyRequestHandler
    Source: HallRequestHandler.java, ImageProxyRequestHandler.java,
            RootUriRequestHandler.java, newgui.html
    Infrastructure repair, not an AI rule. The /hall catch block was
    swallowing exceptions silently; now logs the full stack to
    nohup.out. This is how we found the lockedDeckType DB schema
    bug. ImageProxyRequestHandler.java + RootUriRequestHandler route
    + newgui.html interceptor were restored after being lost in the
    upstream master rebase. Without these, Unity cannot load card
    images (CDN blocks Origin-bearing fetch).

  V99 — Non-senator at Galactic Senate block
    Source: DeployEvaluator.java
    Inverse of V83/V88. Non-senator characters deploying to
    Coruscant: Galactic Senate get -1500 unless opponent power at
    Senate already exceeds friendly senator power there (genuine
    defensive reinforcement). Stops Rando from wasting Maarek Stele
    / Admiral Ozzel deploys at Senate on turn 1 with zero threat.

  V100 — Pull or deploy LOCATIONS before character deploys
    Source: ActionTextEvaluator.java
    During DEPLOY phase, when an Effect / Interrupt / Objective /
    Epic Event source has GameText matching a location pull or
    deploy from Reserve (docking bay, location, system, site,
    sector), AND we still have a CHARACTER or VEHICLE in hand →
    +1500 to fire that pull BEFORE deploying characters. Locations
    expand deployment footprint; firing this first means the new
    location is available as a deploy target for the same turn's
    character drops. Excludes Knowledge And Defense.

  V101 — Force loss source priority Used > Reserve > Hand
    Source: CardSelectionEvaluator.java
    When the AI picks which card to lose for Force loss, prefer
    cards in Used Pile (+500) over Reserve Deck (+300) over Hand
    (-500). Replays showed Rando losing 4 critical hand cards
    (Ap'lek, Rise Of The Sith, We Must Accelerate Our Plans, Force
    Push V) while Used Pile had 2 cards available and Reserve had
    40 — leaving the next turn with a too-small hand to play.

  V102 — K&D activation cap by ACTIVATION count
    Source: ShieldStrategy.java, ActionTextEvaluator.java, RandoCalAi.java
    Pre-V102 the K&D pacing only counted CardCategory.DEFENSIVE_SHIELD
    deploys. K&D's "play a card" pulled Effects / Interrupts /
    Objectives that didn't trigger the counter, so the cap never
    bit. V102 adds a per-turn K&D activation counter
    (knDActivationsThisTurn) that increments every time the AI
    fires K&D's top-level "Play a card". Hard block -2000 once the
    cap (2 turn 1, 3 turn 2, 4 turn 3+) is reached.

  V103 — Parsec MULTIPLE_CHOICE Verge detection fix
    Source: ActionTextEvaluator.java
    V79 detected Verge of Greatness correctly in DeployEvaluator
    but returned false in the parsec MULTIPLE_CHOICE evaluator. The
    AI then "produced no actions" for the choice and the engine
    defaulted to option 0 (parsec 2). Fixed by adding isInPlay zone
    filter, loosening owner check for the ~prefix, instrumentation
    logs for debugging, and a parsec-distance fallback so the AI
    always scores at least something.

  V104 — Block drains of 1 or less under Battle Order rules
    Source: ActionTextEvaluator.java
    Pre-V104 the V52 rule said "turn 3+ always drain even under
    Battle Order." Steve's correction: paying 3 force for a drain
    of 1 or less is a net -2 force loss — never worth it. V104
    hard-blocks (-2000) drains where drain value ≤ 1 AND we're
    under Battle Order / Plan rules AND we don't occupy system+site
    (the +3 cost trigger). V52 still applies for drains ≥ 2.

  V105 — 4th defensive shield: Battle Order / Battle Plan
    Source: ShieldStrategy.prefers4thSlot()
    When Rando friendly-occupies a SYSTEM battleground AND a SITE
    battleground, the 4th shield slot prefers Battle Order (Dark)
    or Battle Plan (Light) +2000. Rationale: Battle Order/Plan
    forces +3 force per drain unless drainer occupies system+site.
    When Rando is on both, his drains are unaffected; opponent
    pays the +3 penalty. One-sided hit.

  V106 — 4th defensive shield: Come Here You Big Coward / Simple Tricks
    Source: ShieldStrategy.prefers4thSlot()
    When opponent has Force-drain-bonus sources on table
    (lightsabers, drain-bonus objectives) AND opponent occupies
    fewer than 2 battlegrounds AND Rando occupies at least one
    battleground, prefer Come Here You Big Coward (Dark) or Simple
    Tricks And Nonsense (Light) +2000. Verified card text: "cancel
    opponent's Force drains at non-battleground locations and
    Force retrieval" — exactly counters the drain-bonus stack.

  V107 — 4th defensive shield: Resistance / Ultimatum
    Source: ShieldStrategy.prefers4thSlot()
    When opponent can drain for 3+ at any site AND prerequisite
    holds (Rando occupies ≥3 battlegrounds OR opponent occupies 0),
    prefer Resistance (Dark) or Ultimatum (Light) +2000. Verified
    card text: "you lose no more than 2 Force from each Force drain
    or 'insert' card." Caps incoming drain damage regardless of how
    many bonuses opponent stacks. Priority order at 4th slot:
    A (Battle Order/Plan) > C (Resistance/Ultimatum) > B (CHYBC /
    Simple Tricks).


V108-V110 ACTIVE RULES (added 2026-05-20)
-----------------------------------------

  V108 — Prioritize deploying senators in MLITL deck
    Source: DeployEvaluator.java
    When My Lord, Is That Legal? / Make It Legal objective is active
    AND the deploying card is a senator (Keyword.SENATOR OR lore
    contains "senator"), score the deploy action +500. Without this,
    Rando left Edcel Bar Gane / Lott Dod / Toonbuck Toora / Tikkes
    sitting in hand for 5+ turns while deploying non-senators. With
    V108, senators are first into Senate.

  V109 — Protect senators from force loss / cost / forfeit
    Source: CardSelectionEvaluator.java
    When MLITL active AND the card being chosen for force-loss /
    cost / discard / forfeit is a senator (lore or keyword) → -300.
    Stops Rando from burning Aks Moe as With Thunderous Applause
    cost, or losing senators from hand during battle damage.

  V110 — Hold non-senator deploys until a non-Senate site exists
    Source: DeployEvaluator.java
    When MLITL active AND deploying a non-senator character AND the
    only sites on table are Galactic Senate (no other site available)
    → -2000 on the deploy action. Prevents the engine forcing
    non-senators into Senate by elimination. Holds them in hand until
    a docking bay / interior / other site is on table.

  V105/V107 — 4th-shield slot closed indefinitely (revision)
    Source: ShieldStrategy.prefers4thSlot(), CardSelectionEvaluator.java
    V106 (Come Here You Big Coward / Simple Tricks) was REMOVED as a
    trigger — too easy to fire spuriously. 4th defensive shield slot
    now stays CLOSED INDEFINITELY until ONE of:
      A. V105: Rando occupies both a system AND a site battleground
         → Battle Order (Dark) or Battle Plan (Light)
      C. V107: opponent can drain 3+ at a site AND Rando occupies
         ≥3 battlegrounds (or opp occupies 0) → Resistance (Dark) or
         Ultimatum (Light)
    When neither fires, all 4th-slot shield candidates score -5000
    (hard block) so K&D passes over them rather than picking the
    least-bad one. Earlier -500 penalty was insufficient.

    Updated 2026-06-17: V106 RE-ENABLED, tightened. It was dropped for firing on
    "any opponent lightsaber"; now it fires only when the opponent is actually
    force-draining a NON-battleground site (exactly what Simple Tricks And
    Nonsense / Come Here You Big Coward cancel) AND Rando occupies a battleground,
    opp occupies < 2, opp has a drain bonus. From Steve's Dooku game: Rando held
    Simple Tricks all game while Dooku drained him at a non-battleground every
    turn. Now he plays it. Verified: fired and Rando won. (Same V106 rule,
    re-enabled — not a new version.)

  V99 — Senate non-senator block (revision)
    Source: CardSelectionEvaluator.java
    Originally placed in DeployEvaluator only — never fired because
    the deploy action text is generic ("Deploy"), not "Deploy to
    Galactic Senate". Moved into CardSelectionEvaluator's
    location-choice step (mirror of V88's pattern) so it actually
    scores the Senate candidate against alternatives.

  V99/V88-LORE — senator detection uses lore + keyword
    Source: CardSelectionEvaluator.java, DeployEvaluator.java
    Only 29 of 35 senator cards in the codebase use
    addKeyword(Keyword.SENATOR). The remaining 6+ identify senator
    status only in lore text. V88 and V99 senator checks now match
    EITHER keyword OR "senator" substring in blueprint.getLore().


CORRECTIONS (2026-05-20)
------------------------

  V24.14 description — corrected per BOTVHD PR #3260 review
    Earlier wording said "deploy spy where only WE have presence →
    blocks OUR drain." That's wrong — an own undercover spy does
    NOT block your own drains. The rule's outcome is right: avoid
    deploying spies at locations we already control, because the
    spy's undercover drain-blocking is WASTED there (its value is
    only realized at opponent-occupied sites).

  V24.14B UPDATE (2026-06) — don't waste spies
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    From Steve's game: Rando deployed a spy onto a site he already held,
    and a 2nd spy onto an existing spy, while Dooku drained 3 unblocked
    elsewhere — wasting spies whose whole job is blocking enemy drains.
    Two fixes to the spy-deploy scorer: (1) detect a friendly spy already
    at the site (undercover spies have no power, so the old check missed
    them) and block a 2nd one (-1200) so it routes to the open drain; (2) a
    spy onto a site we already hold is now penalized hard UNLESS we can
    flip it and the combined force wins the fight there (Steve's buddy
    caveat). Updates V24.14B in place, no new tag.

  V62 description — corrected per BOTVHD PR #3260 review
    Earlier wording confused "power" with "presence/ability." A
    spy's drain-blocking only works while it's the only character
    at the location with visible presence; moving non-spy
    characters there reveals presence and breaks the block.


HOW TO READ A V-VERSION TAG IN CODE
-----------------------------------

Every rule has a comment block like:

    // === V83 (Steve, 2026-05-16): MY LORD — SENATORS ONLY AT GALACTIC SENATE ===
    // Per Steve: "For the my lord objective, Rando should deploy
    // senators only to the senate location..."

`git blame` on any AI file shows the V-tag history with dates.


CODE PATHS
----------

  src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/
    Rando Cal bot

  src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/
    Chosen One bot (functionally mirrors Rando, slightly different
    score weights for experimentation)

  Evaluators (in each bot):
    Deploy, Move, Battle, Draw, ForceActivation, CardSelection,
    ActionText, Pass

  Aggregator:
    CombinedEvaluator sums scores from all evaluators; highest
    action wins.

  Strategy layer:
    DeckOracle (full deck knowledge with zone tracking)
    ObjectiveAnalyzer (parses objective text for flip conditions)
    DeployPhasePlanner (multi-card deploy plans)
    OpponentDeckTracker (predicts opponent's reserve composition)

  Safety net:
    DecisionTracker (loop detection)
    DecisionSafety (fallback responses)


V127-V129 ACTIVE RULES (added 2026-05-22 to 2026-05-24)
-------------------------------------------------------

  V127 — Force-loss consolidation (V13 priority restored)
    Source: CardSelectionEvaluator.java (rando + chosenone partial)
    V101's blanket -500 hand penalty silently dominated V29.8's
    conditional duplicate detection and life-force-low logic. Net
    effect: duplicate hand cards scored -750 while any pile card
    scored +1000 — hand always lost by 1750, even when reserves <= 10
    and Steve's standing rule said "switch to hand losses." Classic
    §2A regression.
    V127 deletes V101 + V119, updates V29.8 magnitudes (Used +400 >
    Reserve +300 > ForcePile +100 > Hand healthy non-dup -300; Hand
    low +400; Duplicate +800), and mirrors V29.8 into the combined
    battle handler. Restores pre-V21 V13-era priority spec:
    Duplicate Hand > Used > Reserve > Hand > Force Pile.
    Chosenone V25 zone scoring magnitude alignment is a PENDING
    follow-up — V127 only deletes V101 from chosenone; full V25
    parity is its own fix.

  V128 — Remove DEPLOY/BATTLE from server auto-pass default
    Source: GameRequestHandler.java
    Upstream bug present in both origin/master and pc-private/master:
    the _autoPassDefault set included DEPLOY and BATTLE. Server
    auto-passed those phases when no client cookie was set. Old GUI
    masked it by actively clicking Pass; Unity epic-duel users saw it
    because Unity doesn't set the autoPassPhases cookie. V128 deletes
    Deploy + Battle from the default. MOVE intentionally kept (Steve's
    rule: auto-pass MOVE with engine autoPassEligible=false override
    for spy moves). Could be PR'd upstream.

  V129 — AFA detection mirror in stacked-pile discipline
    Source: ActionTextEvaluator.java (rando + chosenone)
    V97 (pull-before-activate exclusion), V100 (location-pull-before-
    character exclusion), V102 (activation cap), V124 (4th-slot
    hard-block) all gated detection on "Knowledge And Defense" only.
    Light-side equivalent — Anger, Fear, Aggression (V) — same
    stacked-pile mechanic but never triggered the gates. Net effect:
    light bot had zero 4th-shield discipline when running the AFA-
    based Rey/Skywalker Saga deck. V129 adds AFA to all four
    detection points in BOTH bots (symmetric), and renames local
    variable isKnDShieldPlay -> isStackedPileShieldPlay for clarity.
    Triple-reviewed by Claude subagent + council vote (engineer +
    voice_of_reason both AGREE).


DECISION TRACKER FIX (2026-05-29; no V-tag)
-------------------------------------------

  Cancel-loop detection — break Rando's "Done loop"
    Source: ai/models/rando/DecisionTracker.java
    Rando was getting stuck on turn 1 some games — picking a card to play,
    hitting Done on the where-to-deploy sub-decision, then re-picking the same
    card next phase, hitting Done again. The existing loop detector couldn't see
    it because empty (pass/done) responses are explicitly excluded. Now: if
    Rando hits Done on the same target-pick decision 3 times in a row, the
    tracker invokes the existing blockLastActionOnCancel method (which had no
    caller before) to block the outer action that keeps leading into the dead
    end. Rando picks a different card next phase.


V88 + V33 DEFICIT-6 SAFETY GATES (2026-06-04; existing-rule edits, no new V-tags)
---------------------------------------------------------------------------------

  Steve, last replay: Jabba The Hutt deployed to Jabba's Palace: Audience
  Chamber where Steve had 23 power with Luke + Anakin's Lightsaber, because
  the V88 TEXT-NAMED SITE +500 home-site bonus (shipped yesterday) had no
  contested-fight gate. Same replay: V33 BUDDY BREAK -150 trapped Jabba at
  the same doomed site by penalizing the retreat move that would have
  dropped site ability from 9 to 5.
  Two edits, same deficit threshold (6, matching V67bn cap of 5 with a
  one-point hysteresis buffer):

  V88 TEXT-NAMED SITE (CardSelectionEvaluator.java ~line 1497):
    Was: positive substring match → +500 unconditional.
    Now: compute (oppPower - ourPower) at the candidate site via
    game.getModifiersQuerying().getTotalPowerAtLocation. If gap ≥ 6
    (site is hopelessly outgunned), skip the +500 and log "V88 TEXT-NAMED
    SITE SKIP". The negative -500 branch ("may not deploy at X") is
    untouched — that applies regardless of contestation. The senator/
    Galactic Senate hardcoded block above is also untouched.

  V33 BUDDY BREAK (MoveEvaluator.java ~line 638):
    Was: 4-condition gate (2+ friendly chars, total ability ≥7, post-move
    ability <7 AND ≥4) → -150 unconditional.
    Now: 5th condition appended — site NOT hopelessly outgunned (gap ≥ 6
    means we'd rather retreat than defend). Same getTotalPowerAtLocation
    call. -150 still fires when defending a winnable site; releases the
    move when the site is doomed.

  Net effect on Steve's replay:
    Audience Chamber: V88 +500 doesn't fire; Jabba's score drops from
    +1085 to +585 — Audience Chamber loses to other deploy sites.
    Audience Chamber retreat: V33 -150 doesn't fire; Jabba's retreat
    move scores +100 instead of -50 — Rando actually moves out.

  Council split (engineer EDIT, rules_lawyer + voice_of_reason call it
  "new conditional logic"). Engineer's framing matches the precedent
  set by my V67bn deficit-cap edit (unanimous EDIT verdict 2026-06-03).
  Steve approved "Yes ship" directly; documenting the disagreement
  here.


V88 TEXT-NAMED SITE BONUS (2026-06-03 generalization; no new V-tag)
-------------------------------------------------------------------

  Steve, Jabba's Haven replay: Jabba The Hutt deployed to Tatooine: Desert
  Heart instead of Jabba's Palace: Audience Chamber because V136 §A returned
  the same +500 for every objective-relevant battleground and the four
  candidates tied at +1225. Jabba's blueprint text says "While at Audience
  Chamber, may [download] Scum And Villainy and immune to attrition < 4"
  but no existing rule reads it.
  Steve: "I want to think of a way to make a general rule to give bonues
  when a charactor metnions a specific site. So Jabba's game text says
  Jabba's Palace. If Jabba's palice is on table he gets a bonus to deploy
  there."
  Council vote (engineer + rules_lawyer + voice_of_reason): unanimous
  "this is an EDIT of V88, not a new rule".
  Extension: the existing V88 senator + Galactic Senate hardcoded block
  (line ~1402) is untouched. Immediately below it (line ~1446), a
  universal text-scan clause now runs for every character + every
  candidate site. Detection: lowercase the character's getGameText() +
  " " + getLore(), strip the candidate site title prefix-before-colon
  to get the bare site name (e.g. "audience chamber"), match as
  substring. Length ≥ 5 chars to avoid generic words. Positive match
  → +500 reasoning "V88 TEXT-NAMED SITE: character text mentions 'X' —
  home-site bonus". Negative phrases ("not at X", "may not deploy at
  X", "cannot deploy at X") flip the sign to -500.
  Generalizes to ANY character/site pairing: Vader/Death Star,
  Boushh/Jabba's Palace site, Han/Mos Eisley, etc. — no card-name
  hardcoding, no new V-tag.
  Magnitude rationale: +500 / -500 sits above the V22 objective tie
  spread (+150) but below V59 SPY UNIVERSAL (-2000) and V136 §A
  SPY-BLOCKED (-1000), so safety rules still win when the matched
  site is dangerous.


MUSTAFAR/JABBA REPLAY FIXES (2026-06-03; three existing-rule edits, no new V-tags)
----------------------------------------------------------------------------------

  Steve, last game: "Rando deployed guys to fight me but very underpowered.
  Then wasted his ships on a docking bay where his power was 0 easy to beat.
  I thought we fixed both of these many times. I don't want new rules, I
  want to look at old logic and edit."
  Three changes, council-verified (engineer, rules_lawyer, voice_of_reason
  unanimous: all three edit existing logic, no new rules):

  V67ai isActualLocationDeploy gate (DeployEvaluator.java ~line 3105):
    Was: equals("deploy") OR startsWith("deploy ") with a denylist of
    suffixes (padawan/jedi-survivor/tala-durith/from-reserve). Denylist
    missed "starfighter", "alien", and every other game-text-pull keyword,
    so the docking bay's "Deploy starfighter with 'Vader' in title here"
    action got +1400 Tier-4-HAND bonus and outer scored +1530.
    Now: equals("deploy") only. Bare "Deploy" (cardId-resolved) is the only
    legitimate location-deploy action text; all "deploy <X>" variants fall
    through to the existing V60 V24 SKIP else branch.

  STARSHIP TO DOCKING BAY magnitude bump (CardSelectionEvaluator.java ~line 1144):
    Was: VERY_BAD_DELTA (-150). Sub-decision totals around -100 per
    docking bay site, which still beat several pass alternatives and
    Rando let the ship land at 0 power.
    Now: -1500 hard-coded (was VERY_BAD_DELTA -150). Same rule, same gate,
    bigger magnitude. Sub-decision totals around -1450, every option
    decisively below pass; if Rando picks the outer he'll cancel the sub,
    and after 3 cancels the existing cancel-loop detector blocks the outer.

  V67bn REINFORCE OUTGUNNED deficit cap (CardSelectionEvaluator.java ~line 2345):
    Was: v67bnOutgunned = (theirPower - ourPower) >= 4f. No upper bound —
    fired at deficit 9 (our 3 vs opp 12), +800 reinforcement, Rando piled
    underpowered chars into a losing fight.
    Now: deficit ≥ 4 AND deficit ≤ 5. Braveheart reinforcement only fires
    when the gap can plausibly close (1 mid-power char absorbs 4-5
    deficit). Beyond 5 it's unwinnable and reinforcing just feeds forfeits
    to the opponent.

  None of these adds a new V-tag, new method, or new evaluator path. Each
  is a single-line predicate or magnitude edit on the existing rule.


VEHICLE-PILOT GENERIC RULE (added 2026-05-31; STARSHIP + affordability + embark + power-3 gate 2026-06-01; no V-tag)
-------------------------------------------------------------------------------------------------------------------

  Every vehicle needs a pilot, named or not
    Source: DeployEvaluator.java (appended into V30 block, after the reverse
    V30 ship+pilot rule)
    Steve, multiple replays: "Rando deployed speeder bike but no pilot for the
    bike. Bike is useless. Rando deployed a walker without a pilot making the
    walker useless. Rando should try to deploy troopers to speeder bikes. And
    imperial pilots to Walkers. Of all other vehicles Rando should deploy a
    pilot on the vehicle." A solo vehicle has no power, no move bonus, no
    protection — wasted Force.
    V30 above only fires for NAMED pilot/ship pairs (Wedge+X-Wing,
    Piett+Executor) via getMatchingStarshipFilter(). This block adds the
    GENERIC case so every vehicle deploy gets pilot-aware.
    Two paths:
      (A) VEHICLE deploy: soft-block (-1500) if no pilot-capable character
          is in hand AND no candidate pilot is on the table to crew it.
          Soft so engine fallback can still ship the vehicle when everything
          else is worse, but Rando strongly prefers pilot-first.
      (B) PILOT-CAPABLE character deploy: +400 if Rando has an unmanned
          vehicle on table — pilot it.
    "Pilot-capable" detection (UNIVERSAL — no card-name hardcoding):
    Icon.PILOT OR Keyword.TROOPER. Covers Imperial Pilots (Icon.PILOT for
    Walkers), generic pilots (Icon.PILOT for any vehicle), and
    Stormtroopers / Snowtroopers / etc. (Keyword.TROOPER for Speeder Bikes
    + miscellaneous). Engine-side getValidPilotFilter handles game-text
    exceptions; this AI heuristic gets 95%+ of cases right.
    Magnitudes: -1500 soft block dominates V67ai +1400 location-hand boost
    but leaves room for stronger overrides; +400 boost on par with V30 +300
    MATCHING SHIP IN PLAY (slightly higher because this is the generic BASE
    case Steve called out as missing).
    No new V-tag (Steve's standing "avoid splintering" directive).


  2026-06-01 STARSHIP EXTENSION (First Light replay): the original rule only
  fired on CardCategory.VEHICLE. First Light is CardCategory.STARSHIP — Rando
  shipped it solo into a contested 6-power site, lost the battle (attrition
  13 dmg 12) and the game. Extended Path A's category gate to VEHICLE OR
  STARSHIP, and Path B's unmanned-vehicle scan to also include STARSHIPs.
  Log messages renamed VEHICLE → VEHICLE/SHIP for clarity. Covers Falcon,
  Slave I, First Light, X-Wing, TIE Fighter, Star Destroyer, Y-Wing — any
  starship without a named pilot match now requires a generic pilot in hand
  or on table. Single-line scope fix; no new V-tag.


  2026-06-01 AFFORDABILITY EXTENSION (Steve, both losing games — Walker deck +
  Hoth Walker deck): "Rando had Walkers and did not put pilots on them. Easy
  targets and some of the walkers were powerless with no pilot." Replay
  showed Blizzard 2 deploying with a pilot in hand BUT not enough Force to
  cover ship + pilot. The original Path A only blocked when "no pilot at
  all" — pilot-in-hand-but-unaffordable slipped through and the walker hit
  the table at 0 power, free kill for the opponent. V40 SHIP ABILITY
  already detected this case but only -50 ("was -400"; Steve had intentionally
  softened it earlier, leave it alone).
  Fix: Path A now tracks BOTH "pilot in hand" AND "pilot in hand AND
  affordable to deploy together this turn" (Force >= vehicle_cost +
  pilot_cost). The -1500 block fires when no affordable pilot is in hand
  AND no pilot is on table. Two log sub-cases for triage: "no pilot" vs
  "pilot in hand but unaffordable". Rando now holds the vehicle until he
  has the Force for both, or deploys the pilot first to set up.


  2026-06-01 EMBARK BOOST (Steve, follow-up): "But Rando already had pilots
  on the same site. He's not embarking them onto the walkers or vehicles."
  When a pilot was already at the same site as an unmanned walker, Rando
  could fire an 'Embark' action to crew the walker for free — but
  ActionTextEvaluator.evaluateEmbark scored every Embark at 0 (placeholder
  with a TODO comment), and other action scores or pass won.
  Fix in ActionTextEvaluator: passed cardId into evaluateEmbark, resolved
  the embarker, checked Icon.PILOT/Keyword.TROOPER, walked permanents at
  the same site for an unmanned VEHICLE/STARSHIP via Filters.piloted.
  Match → +500 with reason "EMBARK PILOT: 'X' boarding unmanned 'Y' —
  vehicle gets power & protection". Non-pilot embarkers / no-unmanned-target
  paths return 0 (neutral) like before. Same Icon.PILOT or Keyword.TROOPER
  detection as Path A/B for consistency.


  2026-06-01 POWER-3 GATE (Steve, follow-up #2): "If pilot is power 4 or
  more let's leave them disembarked from vehicles. Likely better as ground
  troops. Regular pilots are usually power 3 or less." High-power characters
  (Vader, Tarkin, Veers, etc.) often have Icon.PILOT but they're worth more
  hitting people on the ground than crewing a walker. Gate applied to TWO
  spots:
    • ActionTextEvaluator.evaluateEmbark — if embarker's power >= 4, skip
      the +500 boost (return 0 with reason "power N — better as ground
      troop").
    • DeployEvaluator Path B — if deploying character's power >= 4, skip
      the +400 "PILOT FOR UNMANNED VEHICLE/SHIP" boost.
  Path A (vehicle-needs-pilot block) stays untouched because the WALKER
  still needs *some* pilot — we just don't want our power-4+ characters
  to be that pilot. Path B and the embark boost effectively steer the
  power-3-or-less troopers into the vehicle and leave the heavyweights
  on the ground.


MOVEEVALUATOR BLOCKED-RESPONSE GATE (added 2026-06-02; no V-tag)
----------------------------------------------------------------

  MoveEvaluator now honors blockedResponses (cancel-loop enforcement)
    Source: ai/models/rando/evaluators/MoveEvaluator.java (action loop)
    Steve replay (lost-pile lockup that surfaced as a MOVE loop): Rando
    repeatedly picked 'Move using landspeed' as the outer action, the
    sub-decision "Choose where to move Ponda Baba (V) using landspeed, or
    click 'Done' to cancel" scored every destination negative (e.g.,
    Jabba's Palace: Dungeon at -432 due to V67g zero-drain + drain-loss
    penalties), pile-selection returned empty, cancel-loop counter tripped
    8 times and called blockLastActionOnCancel each time. The block
    REGISTERED ('Blocking action 2 for "Choose Move action or Pass"') but
    MoveEvaluator re-scored the same move positively next phase and Rando
    picked it again — same hole DeployEvaluator had until commit
    5df527801 (2026-05-31).
    Fix mirrors that DeployEvaluator patch: read context.getBlockedResponses()
    at the top of MoveEvaluator's action loop; if a candidate's actionId or
    actionText is in the block set, hard-block -9999 with reason
    "CANCEL-LOOP BLOCK". ActionTextEvaluator already honored the block list
    (line 86-99); DeployEvaluator was patched 2026-05-31; MoveEvaluator was
    the last hole. Pattern is identical in all three files — same Set,
    same magnitude, same reason string.
    Why this happened: V67g and other move-scoring rules don't know about
    cancel-loop state — they just evaluate "is this move good per Rando's
    objective?" When every site is bad, Rando bottoms out and Done-cancels.
    The cancel-loop machinery exists to detect this and block the outer
    action; it just needs each evaluator to enforce the block on its own
    actions. Three evaluators, three identical gates.
    No new V-tag; same enforcement layer as the cancel-loop work from May 29.


MULTI-SELECT RESPONSE FIX (added 2026-05-31; no V-tag)
------------------------------------------------------

  ARBITRARY_CARDS / multi-select min>1 needs comma-joined response
    Source: ai/models/rando/RandoCalAi.java (tryEvaluators return path)
    Steve, Hoth deck replay: "Rando keeps getting stuck. He won't pass or move
    onto his next move." Decision: "Choose Walker Garrison and 3rd Marker to
    take into hand" (You May Start Your Landing turn-1 effect) with min=2 max=2
    and 2 selectable cards. Rando sent a single ID ('temp7'); engine's
    ArbitraryCardsSelectionDecision splits the response on commas and validates
    cardIds.length in [min, max] — a single ID with min=2 throws
    DecisionResultInvalidException and the engine re-prompts the same decision
    forever. NOT a Done/cancel loop (responses are non-empty), NOT caught by the
    cancel-loop detector. Pre-existing output-format gap.
    Fix: when min>1 in the tryEvaluators return path, build a comma-joined
    response. Seed with the evaluator's best ID (if it's a selectable card
    from the offered list — guards against CARD_ACTION_CHOICE index leakage),
    then fill from selectable cards in list order until exactly `min` IDs are
    collected. Returning exactly `min` is the safest count: always satisfies
    the lower bound, never exceeds max. min==1 paths are unchanged.
    Lives at the response boundary so every evaluator benefits — no
    per-evaluator multi-select wiring needed. No new V-tag (Steve's "avoid
    splintering" directive).


PULL-TARGET: DOWNLOAD-ENABLER PRIORITY (added 2026-05-31; no V-tag)
-------------------------------------------------------------------

  Universal "deploy-chain" rule for Take-from-Reserve picker
    Source: CardSelectionEvaluator.java (evaluateTakeIntoHand)
    Steve, Xizor / Black Sun replay: "vigo can deploy to imperial city. Then
    pull The palace then the palace sewer turn 1." Rando let Coruscant: Xizor's
    Palace sit in Reserve all game — never pulled it. Once on table, the Palace
    [downloads] Sewer / Uplink Station from Reserve. Same pattern for Vigo
    (200_91): [downloads] a battleground planet site = a free Xizor's Palace.
    Three locations on table turn 1 for 1 Force.
    Detection (UNIVERSAL — no card-name hardcoding): scan candidate's blueprint
    game text (base + light-side + dark-side per V71 pattern) for "[download]"
    + a location target word (site / location / system / battleground). If
    matched, +500 boost on Take-from-Reserve picks. Universally covers Vigo,
    Coruscant: Xizor's Palace, Shadows Of The Empire, Cloud City [download] sites,
    Death Star II [download] sites, any future card with the same chain. The
    "[download]" token is the canonical Reserve-pull marker; character-target
    downloads (e.g. Imperial City's "[download] a Black Sun character") don't
    fire the boost because they lack the location target words.
    Magnitude +500: on par with the strongest TDIGWATT-specific pull boost
    (V24.1 Gherant +400), because each enabler pull compounds — one good pull
    sets up 2-3 future location deploys. Appended into the existing pull-picker
    block, no new V-tag per Steve's "avoid splintering" directive.


V67z UPDATE (deploy-phase transit reserve, 2026-06)
-----------------------------------

  V67z (step 3) — Save Force to transit off Mapuzo (Hidden Path)
    Source: DeployEvaluator.java (Rando + Chosen One)
    From Steve's HIDDEN PATH CHARGE game: Rando got his Jedi to the Underground
    Corridor but never moved them off Mapuzo, so his objective never flipped and the
    Jedi stayed crippled. Cause: the off-Mapuzo transit costs 1 Force per Jedi in the
    MOVE phase, but Rando spent all his Force on deploys first (the draw-phase reserve
    didn't survive the deploy phase). Now the deploy phase holds back up to 3 Force on
    Hidden Path so the transit can fire next move phase. Verified: the objective now
    flips in self-play (it never did before).


V184 ACTIVE RULE (added 2026-06)
-----------------------------------

  V184 — Use "when deployed" free abilities (don't pass them up)
    Source: ActionTextEvaluator.java (Rando + Chosen One)
    From Steve's game: Rando deployed Han (Optimistic General), whose "when deployed"
    ability reveals the top two of his Reserve Deck and takes one into hand — free card
    advantage. Rando passed it (the action scored nothing, Pass won). V184 makes Rando
    fire these free when-deployed triggers — reveal/look at the reserve and take a card,
    or retrieve a Force — as long as there's actually something to get (non-empty reserve
    / lost pile). Verified: Han's reveal now scores well above Pass and gets taken.


V183 ACTIVE RULE (added 2026-06)
-----------------------------------

  V183 — Don't search the Reserve for a card that's in your hand (deck-title retool)
    Source: DeckOracle.java + ActionTextEvaluator.java (Rando + Chosen One)
    From Steve's game: Rando played Fall Of The Legend to fetch Weather Vane, which
    was already in his hand — the search failed and revealed his Reserve. The old
    guard couldn't read that card's wording. V183 resolves the target a sturdier way:
    the oracle already knows every card title in the deck, so it scans the source
    card's text for those titles and checks the real card's zone. If the named card
    isn't in the Reserve, the search is dead and gets blocked. Verified: blocked the
    Weather Vane search ~98% of the time, plus a Bespin dead-search, with no
    false-blocks. (A few leak through on timing — a follow-up.)


V182 ACTIVE RULE (added 2026-06)
-----------------------------------

  V182 — Save force for a bigger army next turn (offensive force-banking)
    Source: DrawEvaluator.java (Rando + Chosen One)
    Steve's report: Rando never leaves force in the pile — his draw phase converts it
    all into hand cards, so he could never bank up for a winning deploy next turn.
    V182 adds the missing rule, by Steve's bottleneck logic: if he already has enough
    characters in hand to win a fight he's losing but lacks the force, he STOPS drawing
    and holds the force; next turn he deploys the army and takes the fight. If he does
    NOT have enough characters, he draws normally to find more. Verified: banks, then
    the army deploys and battles. (Pairs with V181: V181 takes the fight you can win
    now, V182 saves up for the one you can win next turn.)


V181 ACTIVE RULE (added 2026-06)
-----------------------------------

  V181 — Take the close fight when the drain is worth it (forfeit-weighted)
    Source: CharacterDeploySiteEvaluator.java (shared — both bots)
    Steve's concept: a small power gap is a coin-flip, not a loss — battle destiny
    decides it, the extra body is forfeit/weapon fodder, and contesting denies the
    opponent's drain. So when Rando is out-powered by <=3 at a contested site, the
    site drains >=2, and the forfeit trade is favorable-or-even (we don't lose more
    value than them), Rando now commits instead of passing. The bonus scales with the
    drain (drain 2 -> +200, drain 3 -> +300) and is capped below a clean win, so it
    never overrides a guaranteed-better deploy. Drain 1 sites are skipped (not worth
    a coin-flip). Verified: armed Maul commits into a drain-2 site at gap 3 with a
    favorable forfeit trade. Follow-up (not yet built): draw-phase force-banking so
    "save force for a bigger army next turn" actually holds the fuel.


V180 ACTIVE RULE (added 2026-06)
-----------------------------------

  V180 — Recognize Luke as Luke (arm characters by persona, not name)
    Source: ActionTextEvaluator.java (Rando + Chosen One)
    From Steve's game: Rando never armed Luke. The guard that decides whether a
    lightsaber has a wielder on table was checking the character's printed NAME for
    "luke" — but "Young Skywalker" is Luke by persona, not by name, so the guard
    blocked his own saber 12 times and Luke fought bare-handed all game. V180 also
    checks the persona, the same way the rest of the code identifies characters.
    Verified: Rando now deploys Luke's Lightsaber (and arms Rey) in self-play.


V179 ACTIVE RULE (added 2026-06)
-----------------------------------

  V179 — Deploy the location-from-Reserve (farm) before characters
    Source: DeployPhaseScript.java (Rando + Chosen One)
    From Steve's game: Rando had "I Must Be Allowed To Speak" out, which deploys a
    free Tatooine farm (a drain site) from Reserve Deck — but he deployed Luke
    instead, every turn, and the farm rotted in Reserve. The deploy-priority walk
    classifies actions into Locations -> Key Characters -> ... and takes the first.
    It didn't recognize "farm" as a location keyword (the card scorer did), so the
    farm never entered the Locations bucket and a character always won. V179 gives
    the walk the same location-keyword list the scorer uses (farm, planet names,
    docking bay, cantina, etc.), so location pulls deploy first as intended.
    Verified: farm now deploys in self-play.


V178 ACTIVE RULE (added 2026-06)
-----------------------------------

  V178 — Keep the lightsaber carriers alive (small forfeit tiebreaker)
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    Sabers kept dying with their forfeited carriers, costing the drain bonus
    and hit potential until re-pulled. Armed characters now get a -10 forfeit
    tiebreaker: when two forfeit choices are otherwise equal, the unarmed body
    goes first. Per Steve: weapons are worth something, not everything — real
    factors (forfeit value, hit status, immunity) still dominate.


V178 ACTIVE RULE (added 2026-06)
-----------------------------------

  V178 — Don't throw away the lightsaber (protect weapons that have a wielder)
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    From Steve's game: Luke's Lightsaber got lost as force fodder while Luke was
    in play, so Luke fought bare-handed. V175 protects battle interrupts from being
    used as fodder; V178 extends that to weapons whenever there is a character to
    wield them (on table or in hand). The weapon now ranks near-last, like a
    character. Turn-gated to turn 4+ (early game, a blind reserve loss is riskier
    than losing a known weapon). Verified firing in self-play.


V177 ACTIVE RULE (added 2026-06)
-----------------------------------

  V177 — Never search the Reserve Deck for cards that aren't there
    Source: ActionTextEvaluator.java (Rando + Chosen One) + DeckOracle
    From Steve's game: Rando used Luke Skywalker's game text to search his
    Reserve Deck for "Force Projection" over and over — a card that was never
    in the deck. Each attempt wasted the action and showed Steve the whole
    reserve deck. Steve: "A real player knows all the cards in his deck and
    would never search for a card he knows is not in it."
    The Deck Oracle already knows the full deck and where every card currently
    is — it just was never asked. Now, before ANY game-text search of the
    Reserve Deck (characters, effects, interrupts, objectives — universal, no
    hardcoded card names), the bot parses what the card searches FOR and asks
    the Oracle if any of it is still in the reserve deck. Nothing there -> the
    search is blocked. Safety valve: when the card text can't be parsed
    cleanly, the bot does NOT block (an untrusted parse never kills a live
    pull), and multi-target searches stay alive if any one target remains.
    Verified: Force Projection searches went from 4+ per game to zero; the
    long-standing Petranaki Arena dead pull is finally caught by the same net;
    live pulls (lightsabers, effects) keep firing.


V177 ACTIVE RULE (added 2026-06)
-----------------------------------

  V177 — Take the winnable fight (Luke should have crushed Kylo) + don't lure
         a deploy into a site it can't hold
    Source: CardSelectionEvaluator.java + CharacterDeploySiteEvaluator.java (both bots)
    From Steve's game: Rando left Luke + Luke's Bionic Hand + Threepio + a
    lightsaber in hand instead of overpowering Kylo, because the winnability math
    talked him out of it two ways — it reserved more force than he had (so it
    thought he could only send Luke alone), and it counted only character power,
    never the Bionic Hand or the saber that arm Luke. Now the reserve can't starve
    the attack to zero, and the strike-group projection counts the gear (devices +
    weapons), so an armed group is correctly seen as a winning attack. Also: a ship
    (Wild Karrde) had been lured to a site to "contest a drain" then forced to move
    away, wasting force — now the contest bonus only fires when the deploy can
    actually hold the site. Verified in self-play; the full Luke-gear case needs a
    real (uneven) matchup to show. Still open from that game: protecting Luke's
    Lightsaber and the farm from being lost as fodder (next).


V176 ACTIVE RULE (added 2026-06)
-----------------------------------

  V176 — Always keep the force to start the battle
    Source: CardSelectionEvaluator.java + DeployEvaluator.java (Rando + Chosen One)
    From Steve's game: solo Yoda sat at Hoth, badly outgunned. Rando deployed an
    army right onto him — and then couldn't battle, because the deploys spent
    the last force and starting a battle costs 1. The engine never even offered
    the battle option. Steve reinforced next turn and the free kill turned into
    a real fight. Now (1) the attack-wave budget always reserves 1 force as the
    battle-initiation fee, and (2) when a winnable battle is sitting on the
    table and the force pile is down to 2 or less, Rando stops deploying and
    keeps the battle money — the battle phase comes right after deploy, so the
    saved force becomes the kill. Verified: fired at the exact Hoth site from
    Steve's game; battles stayed balanced in self-play.


V175 ACTIVE RULE (added 2026-06)
-----------------------------------

  V175 — Use battle interrupts offensively (kill shots, smart substitutes,
         and stop burning the tricks as fodder)
    Source: ActionTextEvaluator.java + CardSelectionEvaluator.java (both bots)
    Steve noticed Rando almost never used interrupts offensively in battle. The
    logs showed why, three ways: (1) "Make <character> lost" kill-shot actions
    (the Sniper / Dark Strike class) were unrecognized — the engine offered five
    kill-shots on Steve's characters (Yoda, Rey, Ben Solo, Anakin, Han) and Rando
    passed every one; (2) the force-loss picker burned battle interrupts like
    Welcome Home, Lord Tyranus as fodder before any battle could use them; (3)
    when the one recognized pattern (substitute destiny) WAS offered, Rando took
    it — the pipeline starved it, not the choice.
    Now: kill-shots on opponent characters score big (scaled by power+forfeit)
    and never target Rando's own; "substitute destiny" weighs the actual swing
    (drawn value vs best ability in battle) — fires on a bad draw, saves the card
    on a good one; and battle-relevant interrupts in hand are lost near-last,
    like characters, instead of first. Verified: the fodder protection fired 86x
    in self-play (Welcome Home survives); kill-shot/substitute fire in live play
    where weapons actually hit.
    V175a refinement (Steve): the interrupt protection only starts on TURN 4.
    In the first 3 turns, losing a known interrupt from hand is BETTER than a
    blind reserve loss — the deck is still full of undeployed key cards, so an
    early reserve hit risks something crucial. After turn 3, protect the tricks.


V174 ACTIVE RULE (added 2026-06)
-----------------------------------

  V174 — The attack budget saves force for upkeep and battle interrupts
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    Steve: the flat force check wasn't good enough — the bot must save force for
    maintenance cards (on table and ones deploying with the army) and for
    interrupts it'll want during the battle. Now, before the bot counts what army
    it can afford, it sets aside: upkeep for every maintenance card it already
    has in play, upkeep for the card it's deploying if that card needs it, and
    1-2 force when it's holding battle interrupts. Maintenance characters joining
    the wave cost double (deploy + upkeep). The old flat "force >= 4" check is
    gone — replaced by "can I actually afford at least one buddy after reserves."
    Verified: real games held 7-12 force in reserves, more unwinnable attacks
    got rejected, and affordable aggression continued normally.


V173 ACTIVE RULE (added 2026-06)
-----------------------------------

  V173 — The attack math now counts the whole hand, weapons, and force cost
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    Steve asked whether the V172 winnability check accounts for the whole hand
    with weapon weights vs force cost. It didn't (one buddy, no weapons, crude
    force check) — now it does. Before committing to a contested deploy or a
    reinforcement, the bot projects the full wave it can actually AFFORD this
    phase: every other character in hand (strongest first) whose deploy cost fits
    the remaining force budget, plus lightsaber (+5) / other weapon (+3) weights
    when affordable. That projected wave feeds both gates: deploy-to-contact
    needs near-parity with the defenders; protect-reinforcement needs to close
    the deficit to within 4. Printed costs are used (no per-location modifiers) —
    a known approximation. Verified in self-play: projections ranged 0-11 and
    unwinnable walk-ins were gated while real contests still fired.


V172 ACTIVE RULE (added 2026-06)
-----------------------------------

  V172 — Only attack/reinforce when the math works (stop feeding the kill zone)
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    After V171, two of Steve's games collapsed: Rando deployed characters
    piecemeal into Steve's bigger stacks, Steve initiated every battle and wiped
    each wave, the protect rule then fed the NEXT wave into the same site, and
    Rando conceded with a lost-pile score of 30-to-0. "No challenge at all."
    Now both aggressive deploys share one sanity test — projected power (what's
    at the site + this card + the best character still in hand) vs the opponent's
    power there:
    - Deploy-to-contact (V171) only fires at near-parity; otherwise Rando
      assembles its force on an adjacent site first and moves in as a group (the
      old "march" — which it turns out was the right play when outmatched).
    - Deploy-buddies (V169) only fires when reinforcement can close the gap to
      within 4; a hopeless site triggers RETREAT instead of another wave of
      casualties.
    Verified in self-play (gates rejecting unwinnable walk-ins, battles still
    happening on winnable ones). Real test: Steve's next game.


V171 ACTIVE RULE (added 2026-06)
-----------------------------------

  V171 — Deploy to contact (stop deploying next door and marching in)
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    From Steve's game: Rando kept deploying characters to an EMPTY site next to
    Steve's characters and then moving them in — wasting force and, worse, always
    arriving AFTER its own battle phase (turn order is Deploy -> Battle -> Move),
    so Steve got to initiate every battle on his terms (7-2). Cause: the first
    character deployed to a contested site is momentarily "alone," so solo/danger
    penalties (~-700) buried the contest bonus, and the empty site next door won
    every time. Now, when the bot has a deploy WAVE coming (another character in
    hand + force to land it), deploying directly to the opponent-occupied site
    gets +600 — the wave starts AT the contested site and the battle happens the
    same turn. Truly suicidal sites still lose (their danger penalties stack past
    the bonus). Verified: contested-site deploys now win the location choice and
    battle initiation went from 7-2 against to even.


V170 ACTIVE RULE (added 2026-06)
-----------------------------------

  V170 — Undercover spies block drains (the cheap denial)
    Source: RandoCalAi/TheChosenOneAi (undercover yes/no) + CardSelectionEvaluator
            (spy deploy steering), both bots
    Steve: "Spies cost much less to block a drain than deploying a bunch of
    characters to overpower opponent." The engine asks "Do you want to deploy X as
    an Undercover spy?" at deploy time — the bot had no handler and answered by
    accident. Now it answers YES whenever the opponent has an active drain to block
    (an undercover spy on their site breaks their control, stopping the drain for
    the cost of one cheap character) and NO early-game when there's nothing to
    block. Spy deploys are also steered toward the opponent's BIGGEST drain site.
    Priority order is deliberate: protecting endangered allies (V169) > spy block
    (V170) > battle contest (V166). Verified: spies actually went undercover in
    self-play replays, answering YES against a 4-drain.


V169 ACTIVE RULE (added 2026-06)
-----------------------------------

  V169 — Protect endangered characters (reinforce or retreat)
    Source: CardSelectionEvaluator + DeployEvaluator + ActionTextEvaluator +
            MoveEvaluator (Rando + Chosen One)
    From Steve's live game: Rando left Asajj Ventress alone at Guest Quarters with
    Luke standing AT her site (beaten 6v27 next turn), and later deployed two
    characters to an empty Cloud City site while Tyranus faced a 5-character army
    on Hoth (16v37). No rule knew a friendly character was in danger. Now:
    - A site where our characters are outpowered gets a dominating "deploy buddies
      here" bonus — protection beats draining, spreading, and maintenance worries,
      even into a losing battle (Steve's explicit call).
    - If reinforcement isn't the pick, the endangered character can RETREAT: safe
      empty destinations get a strong bonus and the "wrong direction" block is
      turned off for endangered movers (a retreat IS a move to an empty site —
      that block is what trapped Asajj).
    - The deploy phase can never again be fully passed over a maintenance penalty
      while allies are endangered, and loop-guards soften (not hard-veto) an
      endangered character's move so the escape stays possible.
    Verified in self-play right after deploy: the bot reinforced Guest Quarters
    (+1100, won the choice) and flagged Hoth 3rd Marker — the exact two sites from
    Steve's game.


V168 ACTIVE RULE (added 2026-06)
-----------------------------------

  V168 — Always activate Force (never pass activation)
    Source: ActionTextEvaluator.java (Rando + Chosen One)
    Steve, playing against Rando: "Rando should always activate force and not pass
    activating." Activating Force is how the bot pays for everything (deploys,
    drains, battles). Now "Activate Force" always wins over Pass whenever the game
    offers it; once max Force is activated it's no longer offered, so the bot still
    ends the phase normally.

  V167 — Don't permanently block essential actions (fix Rando stalling out)
    Source: ActionTextEvaluator.java + DrawEvaluator.java (Rando + Chosen One)
    Steve played Rando, got beaten in 5 turns (great!), but then Rando stopped
    playing — no force activation, no drains, full Reserve Deck. Cause: the V163
    loop-breaker hard-vetoed "Activate Force" (it had gotten flagged by a transient
    loop), permanently — so Rando could never put Force in its pile and froze. Fix:
    mandatory actions (Activate Force, draws) can be nudged by the loop-breaker but
    never hard-blocked, so the bot can always activate and draw. Tactical moves keep
    the hard block. Paired with V168.


V166 ACTIVE RULE (added 2026-06)
-----------------------------------

  V166 — Contest the opponent's drain (deploy to make battles happen)
    Source: CardSelectionEvaluator.java (Rando + Chosen One)
    The bots were stuck in drain stalemates — barely battling (8 battles vs 417
    drains over 5 games) and grinding 20-38 turns, even with battle-heavy decks.
    Root cause: both sides deploy to their own safe sites and drain; nobody forces
    a shared site to fight at. V166 measures the force-drain balance — now properly
    bonus-aware (counts lightsaber/objective/Effect drain bonuses, not just raw
    icons) — and when the opponent is out-draining us by 2+, strongly steers Rando
    to DEPLOY into the opponent's softest drain site (fewest defenders). That
    creates contested sites, and V164a then fights them. Result: battles more than
    doubled (8 -> 19), with games staying balanced. Paired with V164a.

  V164 — Battle when ability is equal or greater (V164a)
    Source: BattleEvaluator.java (Rando + Chosen One)
    Old rule only initiated battle with a power advantage. Now Rando also initiates
    when its ability is >= the opponent's and it isn't badly outpowered — a fair-or-
    better attrition trade. On its own it barely moved the needle; it needs V166 to
    create the contested sites worth fighting at.


V165 ACTIVE RULE (added 2026-06)
-----------------------------------

  V165 — Bot-vs-bot stalemate breaker (decide at turn 20 by life force)
    Source: SwccgGameMediator.java (shared infrastructure — applies to both bots;
    NOT an evaluator, so there's no per-bot copy to keep in sync).
    Self-play games could fall into a do-nothing stalemate (no drains, no battles
    resolving, no force loss) and run hundreds of turns, pinning the CPU. Now any
    BOT game that reaches turn 20 (per side) ends immediately and the player with
    more Life Force is declared the winner — "most pro games end within 10 turns,"
    so 20 is a generous cap. Real human games are never affected. Verified: a
    matchup that previously ran 237+ turns now caps cleanly at 20.
    (V164, an ability-based battle trigger, was attempted but was a near-no-op and
    is left uncommitted pending rework.)


V163 ACTIVE RULE (added 2026-06)
-----------------------------------

  V163 — Loop-breaker is now a hard veto (stops a game-freezing move loop)
    Source: ActionTextEvaluator.java + DrawEvaluator.java (Rando + Chosen One)
    A bot-vs-bot game froze for 1000+ iterations on turn 10: a character ("Keder
    The Black") kept choosing "Move using landspeed," but its only legal
    destination was blocked as wrong-direction, so the move cancelled and was
    re-offered forever. The anti-loop guard WAS firing, but its "blocked" penalty
    was a soft -200 that got outweighed by a +250 "flee the undercover spy"
    movement bonus on the same action — so the blocked move kept winning. Now,
    once the guard flags an action as loop-causing, that action is hard-vetoed
    (huge negative + skip all other scoring) so nothing can push it back above
    Pass. The bot passes and the turn advances instead of hanging. This is the
    same "loop-breakers must dominate" discipline that bit us before.


V162 ACTIVE RULE (added 2026-06)
-----------------------------------

  V162 — Deploy locations first; only hold them when nearly out of force
    Source: DeployEvaluator.java (Rando + Chosen One)
    A bot-vs-bot game locked up at 100% CPU because the bot held the Bespin
    system in its hand (free to deploy) and instead kept choosing "Alert My Star
    Destroyer" forever — that action needs the Bespin system already on the table
    to work, so it never completed and just re-offered itself in a loop. Root
    cause: the bot wasn't deploying its locations. Now a location (site or system)
    in hand gets a strong "deploy first" priority before anything else in the
    deploy phase, UNLESS the bot is nearly out of force (reserve deck + force pile
    + used pile combined <= 10) — in which case it holds the location in hand as
    force-loss fodder, as a real player would. Deploying the system early means
    the Star Destroyer combo actually works and the loop can't happen.


V161 ACTIVE RULE (added 2026-05-29)
-----------------------------------

  V161 — Forfeit an immune ship/character to cover big damage
    Source: CardSelectionEvaluator.java
    When Rando had a card immune to attrition in a battle (typically a ship,
    e.g. Capital), the old logic refused to forfeit it even when damage was
    large — Rando burned pile cards instead and lost a lot of force. Now, if
    damage is 4 or more and the immune card's forfeit value covers at least
    3, Rando forfeits the immune ship/character to absorb the damage instead
    of bleeding pile cards. Small damage / thin coverage still keeps the
    cautious behavior (don't waste a board piece).

    Updated 2026-06-17: extended to SOLO characters at SMALL damage. A solo
    immune character (e.g. Yoda) stuck in a losing battle was scored negative on
    small damage, so Rando bled force one point at a time instead of forfeiting
    once. Now a solo immune character is forfeited scaled by how out-powered he
    is at the site: solo-vs-army forfeits him to end the bleed, solo-vs-solo
    barely leans (keeps him), not-out-powered keeps him. Grouped characters
    unchanged. (Same V161 rule, updated — not a new version.)


V160 ACTIVE RULE (added 2026-05-29)
-----------------------------------

  V160 — Recognize the "Shield Will Be Down In Moments" deck
    Source: ObjectiveAnalyzer.java + ActionTextEvaluator.java
    Rando played the dark Hoth invasion deck but ignored its win condition — he
    never deployed Target The Main Generator (the epic event that lets the AT-AT
    Cannon fire at Main Power Generators), so the objective never flipped. Now
    ObjectiveAnalyzer recognizes the deck by title, marks Target The Main
    Generator + AT-AT Cannon + the Hoth marker sites as priority, and any action
    involving Target The Main Generator gets a strong push (+800). The deck can
    actually play its win path now: deploy Target The Main Generator on Ice
    Plains, get the AT-AT Cannon in range, fire each deploy phase, blow away
    Main Power Generators, flip to Imperial Troops Have Entered The Base.


V159 ACTIVE RULE (added 2026-05-31; immune-threshold + capital-release + immune-subject + engine-immune fixes 2026-06-02)
------------------------------------------------------------------------------------------------------------------------

  V159 — Forfeit the right card before burning piles
    Source: CardSelectionEvaluator.java
    Rando used to burn ten Reserve cards paying battle damage before finally
    forfeiting a character for the attrition, and would refuse to forfeit a
    high-fv character even when the damage justified it (Blizzard 1 fv=7 sitting
    on the table while 11 damage came out of the Reserve Deck). One shared
    forfeit picker now decides both decisions the same way: hit/dead characters
    forfeit first, attrition owed makes forfeit mandatory (with a release valve
    so a game-winner isn't sacrificed to a 1-point attrition), and for pure
    damage Rando forfeits when damage is 3+ and the character's forfeit value
    soaks a real chunk. Below 3 damage he still protects the character and
    loses Force instead. Same magnitudes in both paths — kills the long-standing
    drift where one decision scored "hit forfeits first" +150 and the other +1500.
    The old V143 / V67bh / V67t / V139 / V146 / V67bd / V145 blocks are wrapped
    in `if (false /* V159 SUPERSEDED */)` and stripped from the compiled bytecode,
    so the logs only show "V159 FORFEIT" — one rule, one version.


  2026-06-02 IMMUNE-THRESHOLD + CAPITAL-RELEASE FIXES (Steve, Bossk replay):
  Steve replay: Rando owed attrition=6 / damage=9, the ONLY in-battle character
  was Bossk In Hound's Tooth (CAPITAL ship, fv=6, gameText "Immune to attrition
  < 4."). V159 scored Bossk's forfeit at -140; Rando picked "Lose Force from
  pile" at +150 every step, bled 9 Reserve cards, and STILL owed 6 attrition
  he'd have to forfeit for next.
  Two bugs found and patched (no new V-tag, edits to existing V159 only):
    1. Immune-detection was a plain substring match — "Immune to attrition < 4"
       hit "immune to attrition", set isImmune=true regardless of attrition
       value. V159's STEP 4 immune-short-circuit returned 6*60-500=-140. Fix:
       parse the optional "< N" qualifier with a regex; only set isImmune when
       the actual attrition owed is below the threshold. Unqualified
       "Immune to attrition" still maps to true (genuine total immunity).
    2. CAPITAL / AiPriorityCards release valve (`return -1000`) fired
       unconditionally for any attrition-owed scenario, even when the ship's
       forfeit fully covered the attrition. If fv >= attrition, the ship is
       going to be sacrificed eventually anyway — forfeiting NOW also absorbs
       damage in the same move. Narrowed the gate to `(isCapitalShip ||
       isPriority) && fv < attrition` — only protect when the partial forfeit
       wouldn't even cover the attrition demand. When fv >= attrition, fall
       through to the standard +1500 + coverage*100 scoring so the forfeit wins.
  Verified end-to-end with Bossk's exact numbers: attr=6, dmg=9, fv=6 → V159
  now returns +2100 (vs old -140), forfeit beats pile loss decisively.


  2026-06-02 IMMUNE-SUBJECT FIX (Steve, Bib Fortuna replay, same day): the
  yesterday-shipped threshold-parse fix still substring-matched "immune to
  attrition" anywhere in the text. Bib Fortuna's text says
  "While with Jabba, Bib is power +2 and Jabba is immune to attrition." —
  the immunity refers to JABBA, not Bib, but the match treated Bib as immune
  and V159 returned -320 for him at attr=6/dmg=9/fv=3. Rando ran another
  9-cycle pile-loss loop and burned a third of his Reserve before the
  attrition forced a forfeit anyway. Tightened the regex to require the
  phrase to start its own clause: `(?:^|[.,;:!?]\s+|^\s*)immune to attrition`.
  Matches "Immune to attrition < 4." (Bossk, starts after period) and
  "Immune to attrition." (unqualified self-immunity) but does NOT match
  "Jabba is immune to attrition." (preceded by "is", no sentence boundary).
  No new V-tag — surgical replacement of the regex pattern inside V159.


  2026-06-02 ENGINE-BACKED IMMUNITY (Steve, "lets try the edit"): replaced
  the regex entirely with the engine's live modifier-state query — the SAME
  call GuiUtils.isImmuneToRemainingAttrition uses to change the attrition
  icon in the UI. The engine already normalizes "Immune to attrition < N",
  "Immune to attrition of exactly N", and "Immunity to attrition capped at N"
  into numeric modifier values, and tracks dynamic immunity granted by other
  cards on the table. V159 now reads:
    float exactImmunity = mq.getImmunityToAttritionOfExactly(gs, card);
    if (exactImmunity > 0) isImmune = (exactImmunity == attrition);
    else                   isImmune = (mq.getImmunityToAttritionLessThan(gs, card) > attrition);
  Mirrors the engine's check at logic/timing/GuiUtils.java:158-171. No
  card-text parsing, no regex maintenance, no false positives from other
  characters' immunity mentions. Game-null fallback: assume not immune
  (forfeit branch is the correct default when state is unavailable).
  Bossk and Bib still resolve correctly: Bossk threshold 4, attrition 6 →
  6 < 4 is false → not immune → STEP 2 forfeit. Bib's "Jabba is immune"
  modifies Jabba (a separate card), so the engine returns 0 for Bib's own
  immunity → not immune → STEP 2 forfeit. Plus future-proof: any new card
  with dynamic or modifier-granted immunity is now correctly classified
  without code changes.


V158 ACTIVE RULE (added 2026-05-28; reserve-deploy guard + no-wielder branch 2026-05-29; criteria-absent fix 2026-05-29)
--------------------------------------------------------------------

  V158 — One weapon-deploy rule (replaces V33 + V67aq + V115)
    Source: DeployEvaluator.java
    Three overlapping rules decided whether to deploy a weapon, and they fought:
    two double-blocked the same "all characters armed" case, and the
    criteria-aware one would block EVERY weapon if a card's "deploys on X" text
    parsed wrong. Now one rule: deploy a weapon only if an unarmed legal wielder
    exists, else hold it (a lightsaber needs an unarmed warrior of ability 4+).
    Crucially, a mis-read deploy restriction can no longer false-block all
    weapons — it only blocks when there's proof the restriction matched a real
    character who's already armed.


  2026-05-29 follow-up: replay ss2jc7 showed Lord Sidious arming TWO lightsabers
  (Asajj Ventress' Lightsabers + Sidious' Lightsaber) — both came FROM RESERVE via
  an effect, which slips past V158's deploy-action gate. Appended a defensive
  guard in ActionTextEvaluator (paired next to V155 since both target the
  reserve-via-effect path): an action text matching "<weapon-word> from Reserve
  Deck on <character>" now blocks if the named character is already armed. No new
  tag.


  2026-05-29 follow-up #3 (Dooku-deck stuck loop, "every deck" complaint): the
  in-evaluator V158 gate had a hole — the first block required matchArmed > 0
  AND matchUnarmed == 0, so when the criteria parsed but the named persona
  wasn't on the table AT ALL (matchArmed == 0 AND matchUnarmed == 0), Rando fell
  through to "+300 unarmed wielder available" because some OTHER non-matching
  character was unarmed. Rando committed to "Play Dooku's Lightsaber" as the
  outer pick, sub-decision asked "where to attach?" with no legal target, Rando
  hit Done, engine re-asked → stuck. Fix: drop the matchArmed>0 condition; block
  whenever matchUnarmed==0 regardless of matchArmed. Two log sub-cases for
  triage: "criteria all armed" vs "criteria absent". (The 3-strike cancel-loop
  fallback would have caught it eventually, but the +300 was the root cause.)
  No new V-tag.


  2026-05-29 follow-up #4 (cancel-loop counter never tripped): Steve hit the
  loop again with a U-3PO sub-decision. Diagnosis: the loop pattern is
  OUTER_PICK (non-empty, e.g. 'Deploy U-3PO' → key A) → SUB_CANCEL (empty 'Done',
  key B) → OUTER_PICK → SUB_CANCEL → … and so on. The DecisionTracker reset the
  cancel counter on ANY non-empty response, so each OUTER_PICK wiped the streak
  the SUB_CANCEL had just started, and the threshold of 3 was never reached.
  Fix: only reset the cancel counter when the non-empty response is to the SAME
  key being tracked (meaning Rando actually picked a sub-target instead of
  cancelling). Different-key non-empties preserve the cancel streak so the
  3-strikes rule can fire and block the outer action. No new V-tag (appended
  into the same DecisionTracker cancel-loop entry).


  2026-05-29 follow-up #5 (cancel-loop fires but Rando still picks the blocked
  action): with the counter fix in place, the cancel-loop detector trips on the
  3rd Done, blockLastActionOnCancel adds the offending actionId to the per-key
  blockedResponses set, but Rando KEEPS picking the same action and re-enters
  the loop. Root cause: ActionTextEvaluator honors context.getBlockedResponses()
  (penalizes blocked actionIds/texts) but DeployEvaluator does NOT — so when
  the outer decision is "Choose Deploy action or Pass" with action '1' blocked,
  DeployEvaluator still scores Deploy at -50 (best of the lot), Rando picks '1'
  anyway, sub-decision re-cancels, infinite loop. Fix: copy the
  ActionTextEvaluator block check into DeployEvaluator's action loop. Any
  actionId or actionText in blockedResponses → hard-block -9999. Rando now picks
  Play a card or Pass when its only Deploy option leads to a dead-end sub-pick.
  No new V-tag (appended into the same DecisionTracker cancel-loop entry).


  2026-05-29 follow-up #2 (replay filx81 turn 2): the first reserve-deploy branch
  catches "X from Reserve Deck on Y" patterns. But filx81 surfaced a second
  variant: Rando pulled Vader's Lightsaber via I Am Your Father turn 2 — auto-
  targeted by the weapon's name (no "on X" in the action text). Lord Vader didn't
  deploy until turn 3; the saber sat in hand and was lost as force-loss fodder.
  Added a second branch: action text matching "X's Lightsaber" + "from Reserve"
  extracts X (the persona word before "'s lightsaber") and blocks the pull if X
  isn't on the table. No wielder = no point pulling the saber.


V157 ACTIVE RULE (added 2026-05-28)
-----------------------------------

  V157 — Cap only empty sites; pile in to overwhelm a weak defender
    Source: CharacterDeploySiteEvaluator.java
    Rando's per-turn limit on how much it stacks at a site used to apply
    everywhere, so it could hold Rando back from massing to win a fight. Now the
    cap (and the over-stack penalty) apply only to UNCONTESTED sites — don't pile
    an empty location. At a contested site there's no cap; and if the opponent's
    defenders there are weak (4 or less total ability), Rando gets an extra nudge
    to mass up and overthrow them. Whether a fight is actually winnable is still
    judged by the existing team-power logic, so Rando won't pile into a wall it
    can't beat.


V156 ACTIVE RULE (added 2026-05-28; SOLO-NO-BUDDY revision 2026-05-29)
----------------------------------------------------------------------

  V156 — Don't deploy a character solo without a buddy plan
    Source: CharacterDeploySiteEvaluator.java
    On turn 1-2, Rando used to spread a low-power character (e.g. 3 power) out to
    its own empty site, where the opponent could mass up and overwhelm it the next
    turn. Now a weak character (power 3 or less) won't deploy alone to a fresh,
    uncontested site that early — Rando reinforces a location where he already has
    characters, or saves the force for a bigger combined deploy. Strong characters
    can still spread out on their own.


  2026-05-29 follow-up (Steve, after filx81): the weak-defender gate is dropped.
  Steve: "They should not deploy solo. They should at minimum have a buddy move
  to them or deploy a buddy." The penalty now fires on ANY solo deploy on turn
  1-2 unless Rando has either (a) an affordable buddy in hand to co-deploy, or
  (b) any friendly character already on the table at another site (who could
  move to join next phase). Strong or weak doesn't matter — even Vader gets held
  until a buddy is in place. Power/ability are kept for the log line only.


V155 ACTIVE RULE (added 2026-05-28; gate fix 2026-05-29)
--------------------------------------------------------

  V155 — Save Welcome Home, Lord Tyranus for battle
    Source: ActionTextEvaluator.java
    Welcome Home, Lord Tyranus is a premium battle interrupt — once per game,
    if Dooku is in a battle and about to draw battle destiny, he uses his high
    ability number instead (a guaranteed strong destiny). Its other mode just
    fetches The Works or Petranaki Arena into hand. Rando kept burning the card
    on that fetch even when there was nothing useful to fetch. Now it asks the
    Deck Oracle: if neither The Works nor Petranaki Arena is actually in the
    Reserve Deck — e.g. this deck runs no Petranaki Arena and The Works is
    already on the table — the pull is dead and Rando holds the card for the
    battle mode. It also holds once The Works is secured (on table or in hand).


  2026-05-29 follow-up: a replay (ss2jc7) showed V155 firing zero times even with
  The Works on the table. Cause: the old gate also required "the works" or
  "petranaki" to appear in the action text, but the actual play-action text is just
  "Take location into hand from Reserve Deck" (the names live in the card's game
  text, not the action text). Gate now only requires "into hand from reserve"
  and that the source card is Welcome Home — appended into V155, no new tag.


V153 ACTIVE RULE (added 2026-05-28)
-----------------------------------

  V153 — Stop bleeding your hand and reserve; lose the right cards
    Source: CardSelectionEvaluator.java (regular force loss AND battle damage)
    When Rando had to lose Force — to a drain, First Strike, or as battle
    damage — he was throwing away the wrong cards: losing General Grievous off
    the top of his Reserve while junk interrupts sat in hand, and dumping Lord
    Sidious + Ap'lek from hand before his Force Pile. The old scoring was
    backwards. Key fact (confirmed in the engine): your life force = Reserve +
    Force Pile + Used Pile; your HAND doesn't count, so losing from hand is
    "free" to the lose-condition, while losing from reserve/used/force pushes
    you toward defeat. The new order uses that:
      Life force >= 4 (protect characters):
        duplicates > Used > hand junk > Reserve > HAND CHARACTERS > Force pile
      Life force < 4 (survival — keep yourself off the deck-out line):
        duplicates > hand junk > HAND CHARACTERS > Used > Reserve > Force pile
    Within the hand, every other card goes before a character. While healthy
    Rando keeps his characters (spends Reserve instead); only when critically
    low (< 4) does he dump the hand to preserve the life-force piles. Force
    pile is always last. He keeps at least 4 cards in hand while life force is
    10+, and won't lose a known key card from hand or Used pile. Applies to
    both the regular and battle-damage handlers now.

  V154 — Strip a weapon before forfeiting a character
    Source: CardSelectionEvaluator.java (battle damage / attrition)
    Some decks (Shadow Collective) run an effect that lets Rando lose a
    deployed weapon to pay battle damage or attrition. When that's available,
    Rando now loses the WEAPON first — ahead of everything, including hit
    characters. A hit character is going to be forfeited anyway and its weapon
    would be lost for free along with it, so stripping the weapon first banks
    the extra coverage, then the doomed character forfeits next.


V151 ACTIVE RULE (added 2026-05-28)
-----------------------------------

  V151 — Deploy the strike group into the fight, skip the move
    Source: CharacterDeploySiteEvaluator.java (V136 deploy logic)
    Rando used to deploy a character to a safe site then waste move force
    walking it into the enemy. Now, if the enemy site is contested and
    Rando's hand holds enough reinforcements to win the battle TOGETHER,
    he deploys the whole strike group straight onto the enemy site and
    attacks — no move force wasted. Deploying-into-the-fight is now the
    preferred play; moving is the fallback (V137).


V137 ACTIVE RULE (added 2026-05-28; ANTI-SOLO-BG extension 2026-05-29)
----------------------------------------------------------------------

  V137 — Don't waste move force charging into a battle you'll lose
    Source: MoveEvaluator.java
    Rando deployed Vader to a safe site, then moved him solo into Rey +
    Yoda next door — wasting move force to walk into a losing battle. The
    old hunt rule was blind (flat +350 "charge the Jedi," no battle math).
    Now, when a move targets a site with enemies, Rando sums his whole
    strike group (the mover + buddies that can move together) and only
    commits if they can actually win (power >= opponent AND ability >= 4
    for battle destiny). A solo charge into a loss is blocked; a buddy
    pair that CAN win attacks together.

  V137b — Vader AND Dooku both hunt aggressively
    Source: MoveEvaluator.java
    The hunt + grouping logic was Vader-only. Extended to all Dark Jedi
    (Vader, Dooku/Tyranus, ability >= 6) so Dooku hunts Jedi too and
    buddies group toward whichever Sith leads the attack. Inquisitors
    (Third Sister) stay as buddies, not lone hunters.


  2026-05-29 follow-up: replay ss2jc7 showed Asajj deployed to Guest Quarters
  (drain), then moved SOLO to Beldon's Corridor (uncontested at move time); asdf
  reinforced Beldon's the next turn and overran her — -7 force. The old V137 only
  fires when the destination is already contested. Appended a second branch:
  even when the destination is empty at move time, if it's a BATTLEGROUND and the
  projected team there would be just one character (solo), Rando penalizes the
  move. Don't park a lone body in opponent-reachable BG territory. No new tag.


V150 ACTIVE RULE (added 2026-05-28)
-----------------------------------

  V150 — Forfeit covers attrition + damage together (no wasted pile loss)
    Source: CardSelectionEvaluator.java
    When Rando owes both attrition and battle damage, he was paying the
    damage from his piles card-by-card AND THEN forfeiting characters
    for the attrition — bleeding cards. Since attrition forces a forfeit
    anyway, and that forfeit also soaks up battle damage, Rando now
    forfeits first (cheapest characters, via V139) to cover both, and
    only loses from pile for any small leftover damage after attrition
    is satisfied. Fixes a regression from this session's forfeit-
    protection tuning.


V149 ACTIVE RULE (added 2026-05-28)
-----------------------------------

  V149 — Lightsaber pull needs a capable wielder
    Source: DeployEvaluator.java + ActionTextEvaluator.java
    Rando used to pull a lightsaber from reserve (via Evil Is Everywhere)
    just because an unarmed character was on table — even when that
    character (Dr. Evazan, a cantina alien) couldn't wield it. Now Rando
    only pulls a lightsaber if he has an unarmed character with the
    [Warrior] icon AND ability 4+ to wield it (Jedi/Sith carry [Warrior];
    cantina aliens don't). No capable wielder → no pull. Stops wasting
    the once-per-turn download on a saber nobody can use.


V148 ACTIVE RULE (added 2026-05-28)
-----------------------------------

  V148 — Always allow Done/Cancel when all options are unfavorable
    Source: CombinedEvaluator.java + DecisionSafety.java
    Rando used to commit to terrible deploys/selections (e.g. Dr.
    Evazan to a -1330 site) because the decision was flagged
    noPass=true even though it had a "click Done to cancel" button.
    Now, when every option scores below -100 AND the decision offers a
    Done/Cancel (min=0 + prompt text), Rando cancels — like a real
    player who scopes an option, finds it bad, and backs out. Fixes a
    whole class of "why did he pick that awful option" bugs. Forced
    decisions (no cancel button) still force a pick.


V139-V147 ACTIVE RULES (added 2026-05-26 to 2026-05-28)
-------------------------------------------------------

  Battle-damage forfeit discipline + interrupt-timing gates + a
  critical activation-suppression bugfix. Rando-side; chosenone
  mirror pending.

  V139 — High-value character forfeit protection (3 revisions)
    Always forfeit LEAST-value characters first. Bumped protections
    so valuable uniques (Tyranus, Sidious) aren't forfeited while
    cheap fodder exists. v3 added damage-aware scaling: full
    protection at damage ≤3, 25% at higher damage so a single
    high-forfeit character can still efficiently absorb big hits
    instead of burning 5+ reserve cards.

  V140 — Battle Order cost-waiver
    Drains are FREE under Battle Order when you occupy a non-holosite
    battleground site + a battleground system (or have Battle Plan).
    Previously the bot paid the 3-Force penalty or skipped drains.

  V141 — Transport interrupt floor (Elis Helrot, Nabrun Leids)
    Don't play transport interrupts with < 4 Force or an empty reserve
    deck (can't cover/draw the destiny). Wasted-card prevention.

  V142 — WMAOP mode gating + CRITICAL activation bugfix
    We Must Accelerate Our Plans now only fires in deploy phase for
    modes that deliver value (deck-aware). CRITICAL: the phase-gate was
    accidentally blocking the generic "Activate Force" action when
    WMAOP was in hand (shared cardId), making Rando pass his entire
    activate phase. Fixed to only gate genuine WMAOP plays.

  V143 — Hard-block small-damage forfeit
    When attrition is 0 and battle damage ≤ 2, never forfeit a
    character — lose from pile. No killing a character to pay 2 force.

  V144 — You Are Beaten mode gating
    Reserved for battle-freeze (encouraged in battle phase) or Cancel
    Uncontrollable Fury. The "search Reserve for I Am Your Father"
    mode is never used.

  V145 — Immune-to-attrition forfeit correction
    Characters immune to attrition (e.g. Sidious) can't satisfy
    attrition by forfeit. The bot no longer credits them with covering
    attrition — only battle damage.

  V146 — Hit characters forfeit first
    Hit characters (forfeit reset to 0 by a weapon) are dead weight and
    forfeit first (+1500). All value-protection penalties skipped for
    hit characters.

  V147 — I Am Your Father: don't search an empty Lost Pile
    Don't lose 1 Force to deploy Vader's Lightsaber from the Lost Pile
    when it isn't there. The free Reserve download is preferred.

  V134 guard / V29.7 removal / deck-list light-first
    V134 got the same action-match guard as V142 (latent misfire).
    V29.7's hardcoded "Blockade Flagship only" WMAOP penalties removed
    (superseded by V142). Local-only: deck dropdown now lists light
    decks first.


V136 ACTIVE RULE (added 2026-05-26; opp-undercover detection 2026-06-01)
------------------------------------------------------------------------

  2026-06-01 OPP UNDERCOVER DETECTION (Steve, Jabba Palace replay):
  "Rando deployed a heap of guys on Jabba's Palace while I had a spy blocking
  him." V136 §A computeTeamViability called the engine's
  getTotalPowerAtLocation, which EXCLUDES undercover characters from the
  power tally. Jyn Erso at Audience Chamber → oppPower reported as 0 →
  powerPass && bodyPass → +500 "winnable contested deploy" — same score as
  a truly empty site. Stacked with §B objective-relevant (+200) +
  battleground (+100) + V23 + V29.5/.6/.7 etc., Audience Chamber scored
  +1155 per deploy, Rando piled three characters in, none drained, Force
  wasted, lost the game.
  Pre-existing V67f SPY-ONLY (-100) only fires in evaluateLocationSelection's
  move-destination branch — never reaches V136 §A.
  Fix: after computing oppPower in §A, scan permanents at the candidate
  site for opponent characters with isUndercover()==true. If found AND
  oppPower == 0 (only spies, no real opponents), short-circuit §A with
  -1000 and a "V136 §A SPY-BLOCKED" log line. Rando's Audience Chamber
  score drops from +1155 to about -345, pass wins, Rando picks elsewhere.
  When oppPower > 0 (spy alongside real opponents) the existing
  contested-fight logic runs normally — we're going to battle anyway and
  drain decisions don't matter mid-fight.




  V136 — Unified Character Deploy Site Evaluator
    Source: NEW file ai/models/common/strategy/CharacterDeploySiteEvaluator.java
            + wiring in rando/evaluators/{DeployEvaluator, CardSelectionEvaluator}.java
    Consolidates five separate scoring rules (V90, V67aj, V67al,
    V122, V67as) that all scored the "deploy character X to site
    Y" decision through overlapping checks in two evaluators. §2A
    regressions kept recurring because rules silently over-dominated
    each other.
    Single static method `evaluateSite()` returns one score per
    (deployingCard, candidateSite) pair via four components:
      §A team viability (±2000): ability ≥4, power ≥ opp, body count
         vs opp weapon. Buddy-in-hand lookahead with asymmetric
         resolver (lower-ability character deploys first).
      §B strategic position (±700): BG bonus, NBG penalty two-tier
         (-500 turn 1-2, -300 turn 3+), uncontested over-stack ceiling,
         per-site ability saturation cap (turn 1>10, turn 2>13,
         turn 3>17, nullified turn 4+).
      §C modifiers (±10): small weapon ±.
      §D site-count gate (-700): max 2 ground BGs + max 2 systems
         turn 1-2; overrides for objective-relevant sites + ship-heavy
         deck (5+ ships).
    Side-symmetric: lives in ai/models/common/, parameterized by
    playerId. Both rando and chosenone callers extract primitives
    side-locally (ObjectiveAnalyzer, DeckOracle, hand, turn, force
    available, per-site effect text scan) and pass into the same
    static method. No mirror class.
    Phase 1 (this push): rando side wired. V90, V67aj, V67al, V122,
    V67as commented out (`if (false /* SUPERSEDED V136 */ && ...)`)
    for easy revert via git revert. Chosenone mirror pending.
    Stubs flagged for follow-up (deckShipCount=0, perSiteEffectActive
    =false, isObjectiveRelevantSite=false). See V136_DEPLOY_LOG.md
    for full revert plan + TODO list.
    Spec drafts: /tmp/V136_SPEC_V3.md + /tmp/V136_RULE_CATALOG.md.
    Review process: council engineer (qwen3-coder:30b) APPROVE-WITH-
    CHANGES; subagent NEEDS-REWORK (7 fixes applied to v3 spec);
    v3 includes dominance table with 14 boundary cases including
    replay validation (5bognj14thaf44kn 2026-05-26).


V130-V135 ACTIVE RULES (added 2026-05-26)
-----------------------------------------

  V130 — DeckOracle helpers for deck-aware pull decisions
    Source: DeckOracle.java (rando + chosenone)
    Added countMatchingInDeck() and countMatchingInHandOrTable()
    methods backing V131's three-tier deck-aware logic. Pure helpers,
    no scoring side effects. Mirrored both sides.

  V131 — Deck-aware pull detection (three-tier gate on V67ai LOCATION)
    Source: ActionTextEvaluator.java (rando + chosenone)
    V67ai LOCATION-tier pull bonuses previously fired whenever the
    source card's game text mentioned ANY substring like "site",
    "city", "corridor". This caused two bug classes:
    1. False-positive LOCATION classification when target is actually
       a weapon (Cunning Warrior bug — game text mentions Cloud City
       Corridor but actual pull target is a lightsaber).
    2. Pull fires even when target proven not in deck (effect fails
       and reveals reserve) or already satisfied (wasted action).
    V131 gates V67ai with three tiers:
      Tier 1 HARD BLOCK (-9999): target not in deck at all
      Tier 2 SOFT DOWNGRADE (-2000): target already in hand/table
      Tier 3 EXISTING: target genuinely needed
    Also closes V67l gate entirely when parsed targets are weapon-
    only. FAIL-OPEN throughout — never hard-block on ambiguity.

  V132 — DROPPED
    Was: lower allow-opponent-to-activate baseline 50 → 10. Per Steve:
    allowing opponent to activate force is normal SWCCG play, not a
    last-resort. Reverted to original 50.0f peer score with self-
    activate. Mirror revert in chosenone.

  V133 — DROPPED
    Was: same-persona buddy bonus +1000 when "deploys to same site as
    X" text matches. Per Steve: narrow regex caught only ~5% of cards
    Steve's broader buddy concept actually needs. Folded into upcoming
    consolidated V136 master deploy rule (battle-math team viability +
    universal solo-low-ability gate). V90 and V122 also slated for
    V136 consolidation. Placeholder comments in both rando and
    chosenone CardSelectionEvaluator point at V136 plan.

  V134 — Odin Nesloor 5-force floor (MOVE phase only)
    Source: ActionTextEvaluator.java (rando + chosenone mirror)
    Steve's standing rule: must have at least 5 force in force pile to
    play Odin Nesloor during MOVE phase. The card's repositioning
    ability is wasted when reserves are too low to drain at the
    destination next turn. Hard-block -9999 when forcePile < 5.
    Odin Nesloor is LIGHT-side only; rando mirror is dead code by
    design, kept for V-tag symmetry across both evaluators.

  V135 — Self-move-to-friend requires companion at destination
    Source: MoveEvaluator.java (rando + chosenone mirror)
    Some characters have game text "may move to same site as <X>" —
    self-move intended to put them next to allies. Bug 7a: character
    moved alone via this text to a destination with zero friendlies,
    landing in isolation. V135 detects the self-move-to-friend pattern
    and penalizes -2000 when destination has zero friendly characters
    (excluding cardToMove). Generalized — no hardcoded character names.
    FAIL-OPEN if blueprint or game text missing.

  Review process (V130-V135 bundle):
    Claude subagent code review of chosenone mirrors: SHIP. Council
    engineer (qwen3-coder:30b) via deliberate endpoint with bridge
    tools: APPROVE. V132 reverted after Steve's gameplay-semantics
    correction. V133 dropped after subagent flagged §2A regression
    risk in V136 spec — deferred to dedicated session with proper
    dominance-table discipline. Final shipped bundle: V130, V131,
    V134, V135 (four V-tags + supporting helpers).


11. EARLIER-VERSION DETAILS (V33-V38)
-------------------------------------

Notes from the original V33-V38 development session, preserved for
traceability. Most rules below have evolved (e.g., -9999 hard
blocks were later converted to graduated positive scoring in V39+),
but the underlying intent remained.

  V33 — Weapon limits + buddy system
    One weapon per character (hard block, later graduated in V70).
    Named weapons deploy before generic (Vader's Lightsaber before
    Dark Jedi Lightsaber). Ability 7 buddy threshold for sites.

  V34 — Contest & fight
    Battle base score 50 → 100. Favorable bonus +40 → +150.
    Destination-aware movement: +250 toward opponents, -150 wrong
    direction. Opponent-presence deploy scoring +250/+350.

  V35 — Hunt Down deck strategy
    Inquisitor battle destiny bonus +120/+250 from objective.
    Vader seeks Jedi +350-600 deploy/move. Hatred requires
    Inquisitor at same site. FMFTD (Far More Frightening Than
    Death) interrupt scoring: USED mode stacks hatred on
    opponent's leader at a battleground, LOST mode adds battle
    destiny when Inquisitor fights a Jedi/Padawan. Vader
    expendability (barrier risk × 0.3). Vader self-recall awareness.

  V35.1-V35.8 — Replay-tested fixes
    Stay-and-fight: -400/-500 → -9999 for leaving favorable
    positions. Blaster Rack checks weapon's character is at battle
    location. Hunt Block for empty sites (raised to -1500, later
    converted to positive-only). Ship ability >= 4 check with
    named pilot matching. Castle retreat block: -9999 prevents
    Mustafar retreat. IAYF (I Am Your Father) lightsaber retrieval
    from correct zone.

  V36 — Defend Malachor + weapon targeting
    Early game (turns 1-3) Inquisitor deploy to objective sites
    +800/+1000. Destiny-based weapon targeting: avgDestiny ×
    numDraws vs defenseValue. Padme priority +300 (cancels Vader
    game text). Force-drain contest bonus scales with drain amount.

  V37 — DeckOracle integration + bug fixes
    IAYF (I Am Your Father) zone awareness: checks Reserve vs Lost
    Pile. Sense self-cancel: -9999 block on canceling own interrupts.
    Stunning Leader: -9999 when WE initiated battle, +300 defensive
    only. Reserve search validation: blocks searches when target
    not in zone.

  V38 — Activation + deploy urgency
    Force activation: +500 score, "not activated" confirmation
    always "No". Removed Force Pile cap of 20 that suppressed
    activation. Force reserve when life < 4 after activation
    (save 2-4 for destiny). Deploy urgency scaling with hand size
    (+100 to +300). Persona replace blocked. Force Lightning
    self-target blocked. Castle retreat hard block. Wrong
    direction hard block.

  Issues flagged at V38 (most since resolved):
    "Deploy penalties too aggressive" → V39+ graduated scoring
    "HOLD_BACK applies to Hunt Down" → V40 TDIGWATT-only
    "Undercover spy awareness incomplete" → V53 spy follow + V24.15
    "Emperor Palpatine Force Lightning re-pull each turn" → V95
      (queued) dead-interrupt save rule covers this pattern


12. PRE-TAG ERA (V0 — January 15 to March 16, 2026)
---------------------------------------------------

Before the V-tag convention started, Rando Cal AI existed
for about 2 months of untagged development. Below are the
16 commits to the AI directory during that window. Commit
messages are verbatim from git history.

Authors:
  Snacks      Original Rando Cal author
  eric lanz   Contributor (V0-era improvements)
  bot         Automated upstream merges

  2026-01-15  (Snacks)
  commit 853ef8d48
    Initial Check In
    File: AdvancedAi.java
    File: AiRegistry.java
    File: BeginnerAi.java
    File: HeuristicAiBase.java
    File: SwccgAiController.java

  2026-01-15  (Snacks)
  commit 44e9df629
    Moving AI to new model folder (seperate models/controllers)
    File: models/AdvancedAi.java
    File: models/BeginnerAi.java
    File: models/HeuristicAiBase.java

  2026-01-15  (Snacks)
  commit 508fb08b6
    Added Rando as AI client (init)
    Files touched: 34 files in AI directory
    Touched: common/, models/

  2026-01-15  (Snacks)
  commit e2f706509
    Fixed issue with undefined "holdBackCharacters" var
    File: models/rando/strategy/DeployPhasePlanner.java

  2026-01-15  (Snacks)
  commit 516c15aa5
    Removed unneeded readme file
    File: models/rando/README.md

  2026-01-15  (Snacks)
  commit ca7d19695
    Updates for Rando Cal.
    Files touched: 17 files in AI directory
    Touched: common/, models/

  2026-01-15  (Snacks)
  commit b1b894b75
    Fixed compile error around getting libary for blueprint look up
    File: models/rando/evaluators/CardSelectionEvaluator.java

  2026-01-15  (Snacks)
  commit d54c8eec7
    Changed to use game id instead of player id for stats and viewing pleasure!
    File: AiRegistry.java

  2026-01-16  (Snacks)
  commit 6fbc8fb2f
    Updated with Rando Cal changes requested
    Files touched: 9 files in AI directory
    Touched: (root), common/, models/

  2026-01-21  (github-actions[bot])  [upstream merge]
  commit 132f50142
    Merging changes from upstream

  2026-01-22  (Snacks)
  commit 7b848880c
    Updated AI Base layer to be "smarter" and hopefully not loop
    File: models/HeuristicAiBase.java

  2026-01-23  (Snacks)
  commit 1a991a5b8
    Enhance decision tracking in Heuristic AI by introducing tracking response handling and improving single decision loop logic
    File: models/HeuristicAiBase.java

  2026-01-24  (Snacks)
  commit 0aeb06918
    Implement recent decision tracking and reassignment handling in Heuristic AI
    File: models/HeuristicAiBase.java

  2026-01-25  (github-actions[bot])  [upstream merge]
  commit 263fc88a4
    Merging changes from upstream

  2026-01-29  (eric lanz)
  commit 34326a5b9
    Rando Cal AI improvements and bot stats system
      AI Decision Improvements:
      - BattleEvaluator: Lower thresholds, add ability checking for battle initiation
      - CardSelectionEvaluator: Check if character already has weapon before deploying another
      - CardSelectionEvaluator: Check power differential when deploying starships to space
      - DeployPhasePlanner: Consider ability when deploying to contested locations
      - MoveEvaluator: More conservative movement (require strategic justification)

      Bot Stats & Achievements System:
      - Add BotStatsDAO for tracking player stats against bot
      - Add achievement system with 72+ achievements
      - Track damage records, route scores, wins/losses
      - Display current records in welcome message

      Chat & Message Fixes:
      - Send AI chat messages via ChatRoomMediator (blue player messages)
      - Fix stale battle damage record messages (reset on new battle)
      - Shield pacing: properly track deployed shields

      Database:
      - Add bot_player_stats table schema
      - Add update script for existing databases

      Co-Authored-By: Claude Opus 4.5 <noreply@anthropic.com>
    Files touched: 6 files in AI directory
    Touched: models/

  2026-02-02  (github-actions[bot])  [upstream merge]
  commit ade2f06a9
    Merging changes from upstream
