Rando Cal / Chosen One AI — Version History
=================================================================

Every rule change tagged in code, in version order. Each entry
has the title (from the V-tag comment header), date, source
file, and the full explanation from the comment block.

For the user-impact summary organized by theme, see:
  AI_CHANGELOG.md

Why does this start at V21?
  Rando Cal was first committed to this repo on 2026-01-15 and
  developed for about two months untagged. The V-tag convention
  started with the V29.13 commit on 2026-03-16. V21 through V29.12
  were added to code at or after that point. See section 12 of
  AI_CHANGELOG.md for the pre-V21 git-commit log.

  Source-code comments reference "Ported from Python" for some
  files (DecisionTracker.java, DecisionSafety.java), suggesting an
  earlier Python prototype. If V1-V20 ever existed, it would have
  been there, not in this Java codebase.

  Note: "V15", "V22", "V23" mentions in the broader GEMP git log
  are SWCCG Virtual Set release numbers (cards), not Rando AI
  version tags. Unrelated.

Total tags catalogued: 179

-----------------------------------------------------------------


  ════ V21 family ════

  V21 — BAN CERTAIN EFFECTS AS STARTING EFFECTS
    Source: CardSelectionEvaluator.java
    These should never be deployed via starting interrupt (turn 0)
    They CAN still be deployed from hand during turn 1+ deploy
    phase


  ════ V22 family ════

  V22 — Prefer own objective locations over opponent locations
    Source: CardSelectionEvaluator.java
    V24.15: EXEMPT SPIES from contest penalties! Spies deploy
    undercover — they don't fight battles. Contest penalty is
    meaningless for them. Their scoring is handled by the V24.14B
    spy scoring block below.

  V22.2 — POST-FLIP — protecting locations is MORE important, not less!
    Source: ObjectiveAnalyzer.java
    Returning 0 here was a critical bug: after flipping, Rando
    stopped caring about objective locations and deployed
    elsewhere. Now we return a HIGHER bonus because losing these
    locations means the objective flips BACK.

  V22.3 — ALWAYS prefer forfeiting characters over losing from hand/reserve!
    Source: CardSelectionEvaluator.java
    Forfeiting a character with forfeit=5 satisfies 5 damage with
    1 card. Losing from hand/reserve satisfies only 1 damage per
    card. Example: 15 damage, forfeit 2 chars (forfeit 5 each) =
    10 satisfied + 5 from hand = 7 cards total vs losing 15 from
    hand = 15 cards total. Forfeiting saves 8 cards!

  V22.4 — Optional forfeit handling — COMPLETELY REWORKED
    Source: CardSelectionEvaluator.java
    Old bug: ALL optional forfeits were avoided (-150). This meant
    Rando would NEVER voluntarily forfeit characters to satisfy
    battle damage, leading to massive hand/reserve losses (Emperor
    Palpatine not forfeited, losing 16 cards instead) NEW LOGIC:
    If there's battle damage remaining, optional forfeits are
    GOOD! A character with forfeit=6 satisfies 6 damage in 1
    action vs 6 cards from reserve. Only avoid optional forfeits
    when there's NO damage to satisfy.

  V22.5 — Alert My Star Destroyer / Ship Deployment Priority
    Source: ActionTextEvaluator.java
    "Alert My Star Destroyer" deploys Executor + pilot for cheap.
    This is CRITICAL for TDIGWATT — Bespin system occupation
    enables Dark Deal and Cloud City Occupation, which are the
    deck's primary damage engines.

  V22.6 — UNIVERSAL LOCATION PRIORITY FOR OBJECTIVE PULLS
    Source: CardSelectionEvaluator.java
    When an objective offers multiple cards to pull from the
    reserve deck, locations (systems, sites, sectors) should
    ALWAYS be pulled first. Locations are prerequisites — effects
    and characters deploy ON locations, so without the location on
    table first, those other pulls are wasted. Example: Bespin
    system must be pulled before Alert My Star Destroyer, because
    AMSD deploys on Bespin system. This is universal for ALL
    objectives, not just Bespin-related ones. If the location is
    NOT among the reserve deck options, the game engine already
    filtered it out (it's in hand, on table, or not in deck), so
    pulling other cards is fine — no extra hand/table checks
    needed here.

  V22.7 — Broadened AMSD detection. GEMP may present pilot selection with text
    Source: CardSelectionEvaluator.java
    like "Choose a unique pilot character" without mentioning
    "star destroyer". If we're in Deploy phase choosing a unique
    pilot, it's likely AMSD. V24.12: Also detect AMSD by checking
    if the card is actually on the table, because the decision
    text for "Choose card from hand" doesn't mention AMSD at all.


  ════ V23 family ════

  V23 — BESPIN SYSTEM EARLY DEPLOY PRIORITY
    Source: DeployEvaluator.java
    For TDIGWATT, Bespin system is the FOUNDATION of the entire
    objective. Without Bespin on table, nothing works: no Dark
    Deal, no CC Occupation, no AMSD deploy target. Deploy it
    IMMEDIATELY on turns 1-3.


  ════ V24 family ════

  V24 — MEGA LOCATION PRIORITY
    Source: DeployEvaluator.java
    Locations are the foundation of EVERYTHING — force generation,
    deploy targets, drain sites. In the first 3 turns, deploying
    locations should dominate all other actions. V60 FIX: Only
    apply when the ACTION is actually deploying the location
    (source is a LOCATION card and actionText is a bare "Deploy" /
    "Deploy [location]" — NOT when the action invokes a location's
    game-text to pull a character like "Deploy a Padawan" or
    "Deploy Tala Durith from Reserve Deck". FIXES Issue #A from
    peaceful-pike replay: Rando invoked Malachor STE's "Deploy a
    Padawan" at force=0 because V24 thought it was a location
    deploy.

  V24.1 — A: Endor Shield admiral pull — Piett first, Chiraneau backup
    Source: CardSelectionEvaluator.java
    V24.12: GEMP decision text is just "Choose card to take into
    hand" — no "admiral" in it. So also detect admiral pulls by
    checking if this card IS an admiral. The Endor Shield action
    restricts choices to admirals, so if Piett/Chiraneau are among
    the options, we know it's an admiral pull.

  V24.2 — B: LANDO/LOBOT PULL PRIORITY (TDIGWATT)
    Source: CardSelectionEvaluator.java
    Lando and Lobot are key to flipping the TDIGWATT objective.
    Lando can move to unoccupied CC sites at start of control
    phase = 3-site drains. Both deploy cheap. Prioritize pulling
    them from reserve when available. V47: BUT don't pull Lando if
    he'd be alone at CC — he gets clobbered!

  V24.3 — B: DR. EVAZAN WEAPON COMBO — DEPLOY LOCATION PREFERENCE
    Source: CardSelectionEvaluator.java
    Deploy Evazan to sites with weapon chars, and weapon chars to
    sites with Evazan. Evazan converts weapon hits into immediate
    character loss — devastating combo.

  V24.4 — LOCATIONS FIRST — DEPLOY LOCATIONS BEFORE ANYTHING ELSE
    Source: ActionTextEvaluator.java
    Locations MUST be deployed before activating effects (AMSD,
    K&D, etc.). If the bot has ANY location in hand, penalize all
    non-deploy actions heavily so that deploy actions (handled by
    DeployEvaluator) always win priority.

  V24.5 — RESERVE FORCE FOR EXISTING MAINTENANCE CARDS
    Source: DeployEvaluator.java
    If cards with maintenance costs are already in play, deploying
    this card must leave enough Force to pay their upkeep.
    Otherwise they get sacrificed.

  V24.6 — A+V24.9: EXECUTOR DEPLOY PRIORITY
    Source: DeployEvaluator.java
    Executor is THE key ship for TDIGWATT — it force drains at
    Bespin, enables Dark Deal + CC Occupation. If it's in hand,
    deploy it NOW. V24.9: MUST come out turn 1 or 2 at the latest.
    If AMSD didn't pull it from reserve, deploy it manually from
    hand — no excuses.

  V24.7 — OPPONENT DECK INTEL — SCAN DESTINY VALUES
    Source: CardSelectionEvaluator.java
    When verifying opponent's deck, scan all visible cards for
    destiny values. This gives us real data for BattlePredictor
    instead of random 0-6 guesses.

  V24.9 — MASTERFUL MOVE EARLY-GAME GUARD
    Source: ActionTextEvaluator.java
    Masterful Move searches reserve for Ghhhk (damage cancel combo
    card). On turns 1-3, force should go to deploying Executor +
    characters, NOT searching for Ghhhk. Only play Masterful Move
    when characters are on the table and need protecting.

  V24.10 — AMSD pilot check — two scenarios:
    Source: ActionTextEvaluator.java
    1. Action text names a specific pilot (e.g., "deploy Piett's
    matching Star Destroyer") → Check if it's Piett. Block if not.
    2. Action text is generic (e.g., "Reveal pilot or Star
    Destroyer from hand") → Check DeckOracle: is Piett in hand AND
    Executor in reserve? If so, ALLOW. The actual pilot selection
    happens in CardSelectionEvaluator's AMSD guard.

  V24.11 — AMSD ROUTING — CHECK BEFORE evaluateTargetSelection
    Source: CardSelectionEvaluator.java
    "Choose card from hand, or click 'Done' to cancel" matches
    this branch, but when AMSD is active and we're picking
    characters in deploy phase, this is actually an AMSD pilot
    selection. Route to evaluatePilotSelection so Piett-only
    enforcement fires. Without this, Vader gets picked and the
    AMSD action fails because Executor isn't his matching ship.

  V24.12 — AMSD-on-table detection — if AMSD is deployed and we're choosing
    Source: CardSelectionEvaluator.java
    characters during deploy phase, this IS an AMSD pilot pick
    even if the decision text is generic ("Choose card from hand,
    or click 'Done' to cancel").

  V24.13 — LANDO ALONE DETECTION — MOVE TO SUPPORT
    Source: CardSelectionEvaluator.java
    If Lando is the only friendly character at this CC site, big
    bonus to move here and protect him. Lando alone = easy kill
    for opponent.

  V24.14 — B: SPY LOCATION SCORING — check WHO has presence.
    Source: CardSelectionEvaluator.java
    Undercover spies are most valuable at locations where the
    opponent has presence — they block opponent drains there.
    GOOD: deploy spy where opponent has presence and we don't.
    BAD: deploy spy at a location we already control — the spy's
    undercover drain-blocking ability is wasted there (your own
    spy does NOT block your own drains). Either trigger applies
    to CC/objective sites too — opponent can still deploy onto
    our sites and a well-placed spy stops it. V59: Skipped when
    spyScoringApplied=true (universal scoring already ran).
    (Correction 2026-05-20: prior wording said an own spy blocks
    own drain — that's wrong; the rule's outcome is correct, the
    explanation now matches the actual mechanic. Credit BOTVHD
    PR #3260 review.)

    UPDATED 2026-06 (Steve, HIDDEN PATH CHARGE / Dooku game — two spy
    mistakes): two gaps in the WHO-has-presence scoring.
      (1) SPY DOUBLED: ourPower EXCLUDES undercover spies, so a 2nd spy
          deployed onto a site that already had a friendly spy read as
          "IDEAL" (+300). Steve: Rando stacked a 2nd spy while Dooku
          drained 3 unblocked elsewhere. Now scan the site for a friendly
          undercover spy first; if one is already there, -1200 so the spy
          routes to the OPEN enemy drain instead.
      (2) BOTH-SIDES was a flat -50 — too weak to stop a wasted spy on a
          site we already hold (Steve: deployed a spy onto a site he
          occupied). Now conditioned on Steve's buddy-system caveat: if we
          could FLIP this spy and the combined force contests
          (ourPower + this spy's flipped power >= oppPower), allow it (+50,
          break cover and fight). Otherwise it's wasted — -800 (non-CC) /
          -500 (CC) so it routes elsewhere.
    NOTE: the spy-deploy DECISION runs through this CardSelectionEvaluator
    path (V24.14B/V170), NOT the DeployEvaluator V51 action-text path
    (which checks actionLower "undercover" and never matched the real
    CARD_SELECTION "Deploy to X" spy decisions — confirmed 0 fires in
    self-play). So the fix lives here, updating V24.14B in place per
    Steve's "adjust the old rule, don't mint a new version." Compile-
    verified + deployed; bot-tested only for no-regression (per Steve).

  V24.15 — AVOID DEPLOYING CHARACTERS TO 0-DRAIN LOCATIONS
    Source: CardSelectionEvaluator.java
    Characters at 0-drain locations contribute nothing to force
    drain pressure. They're wasted resources and vulnerable to
    Surprise Assault traps. Penalty scales with character power —
    don't waste your best characters!


  ════ V25 family ════

  V25 — CLOUD CITY ABILITY-BASED SPREAD STRATEGY (TDIGWATT)
    Source: CardSelectionEvaluator.java
    When TDIGWATT is active, spreading across Cloud City locations
    maximizes: - Cloud City Occupation: +1 damage per CC location
    occupied - Dark Deal: +1 to each force drain at CC locations -
    Force drains at each occupied location V25: Use ABILITY (not
    character count) to decide when a site is secure. ~6 ability =
    can draw battle destiny and hold the site. Vader alone
    (ability 6-7) can hold a site. Lando alone (ability 2) cannot.
    V24.15: Skip CC spread scoring for spies — they don't
    contribute while undercover


  ════ V26 family ════

  V26 — /V29.6: Dining Room — Deploy Lando (TDIGWATT)
    Source: ActionTextEvaluator.java
    Dining Room's game text deploys Lando from Reserve Deck — a
    key TDIGWATT piece. DeployEvaluator can't find the card (it's
    in reserve, not hand), so we boost here in
    ActionTextEvaluator. V29.6 FIX: Check if Lando would be ALONE
    at Dining Room. If no friendly characters are already there,
    deploying Lando alone is suicide — opponent will drop a
    character + weapon and kill him immediately. Defer until we
    have a buddy at Dining Room first.


  ════ V27 family ════

  V27 — BATTLE INTERRUPT FORCE RESERVATION
    Source: BattleEvaluator.java
    If opponent has "Draw Their Fire" on table, playing ANY
    interrupt during battles THEY initiate costs 1 extra Force.
    This means Ghhhk (Used Interrupt, normally free to play) needs
    1 Force just from the tax. Without Force in pile, ALL battle
    interrupts are unusable and we take full attrition from heavy
    losses. Also applies when WE initiate: defender (us) still
    loses 1 Force when battle is initiated, and if opponent
    initiates, we need extra Force per interrupt.

  V27.1 — DRAW THEIR FIRE — FORCE RESERVATION FOR BATTLE INTERRUPTS
    Source: PassEvaluator.java
    If opponent has "Draw Their Fire" on table, each interrupt we
    play during battles they initiate costs 1 extra Force. We MUST
    keep Force in reserve so Ghhhk and other battle interrupts
    remain playable. Without this, we take full attrition and lose
    cards from hand/reserve in every battle.

  V27.2 — More permissive buddy protection for MOVES.
    Source: MoveEvaluator.java
    During deploy phase, we use strict thresholds (power<6 AND
    ability<2) because deploying solo is sometimes necessary for
    tempo. But during MOVE phase, abandoning ANY character is bad
    because: 1. They're already deployed and in danger 2. The
    opponent can move to their location and attack 3. Solo
    characters draw unfavorable battles Protect if: ally power < 6
    (even if ability is high like Thrawn's 4) OR if enemy is
    already present


  ════ V28 family ════

  V28 — RESERVE DEPLOY SOLO PROTECTION
    Source: CardSelectionEvaluator.java
    When choosing characters to deploy from reserve (e.g., Dining
    Room effect), apply the same buddy protection as hand deploys.
    Characters deployed from reserve to a location where they'd be
    ALONE are vulnerable. This catches "Choose card to deploy from
    Reserve Deck" decisions.


  ════ V29 family ════

  V29 — / V67u: FORCE PUSH — BATTLE USE ONLY
    Source: ActionTextEvaluator.java
    Force Push has two modes: 1. BATTLE: "use 2 Force to target
    your Dark Jedi and opponent's character... Both targets are
    excluded from battle" — GOOD, removes threat 2. FORCE PILE
    EXCHANGE: "Exchange two cards from hand with any one card from
    Force Pile" — BAD, especially in DRAW PHASE: you'd draw those
    cards anyway, and you're trading 2 hand cards for 1. V67u FIX
    (Steve, 2026-05-03): The OLD V29 check was
    `textLower.contains("force push")` — but the action text for
    the exchange is just "Exchange cards with card in Force Pile"
    which does NOT contain "force push". So V29 never fired and
    Rando happily played the exchange during draw phase, wasting
    Force. New V67u: detect by SOURCE CARD title (when cardId
    resolvable) OR by action text mentioning "force pile" +
    "exchange" (which uniquely identifies this wasteful action
    regardless of source).

  V29b — Increased from -80 to -200 — Rando was still spreading thin.
    Source: CardSelectionEvaluator.java
    (No explanatory comment in source.)

  V29.1 — Shield pacing — don't burn all 4 shield slots immediately.
    Source: ShieldStrategy.java
    Play 2 shields on turn 1 for basic protection, then WAIT to
    see what the opponent is running before committing remaining
    slots. This lets us pick targeted counters (e.g. anti-drain,
    anti-retrieval) instead of generic shields. The pacing cap is
    checked by ActionTextEvaluator to gate K&D "Play a Defensive
    Shield" actions, AND by scoreShield() to rank individual
    shield picks. Turn 0 = PLAY_STARTING_CARDS (setup) — shields
    from K&D aren't played here, but allow 4 in case other
    starting effects deploy shields directly.

  V29.2 — LANDO/LOBOT DEPLOY PRIORITY (TDIGWATT)
    Source: DeployEvaluator.java
    Lando and Lobot are critical for flipping TDIGWATT, BUT they
    should NOT deploy alone to a Cloud City site with no backup —
    they'll get killed. V29.2 FIX: Check BOTH the card title AND
    the action text for "lando"/"lobot". The action text is
    crucial because "Deploy Lando from Reserve Deck" comes from
    Dining Room (a LOCATION card), so the resolved card is Dining
    Room, not Lando. We can't rely on category == CHARACTER or
    cardTitleLower containing "lando".

  V29.3 — BLUEPRINT-BASED CARD TYPE DETECTION
    Source: CardSelectionEvaluator.java
    The decision text "Choose where to deploy •Lobot, Lando's
    Broker" does NOT contain type keywords like "character",
    "alien", "droid". We need the card's actual blueprint.
    PRIMARY: Use gameState to find the card — the game engine
    already has all cards loaded with correct blueprints. Search
    hand, reserve deck, and stacked cards. FALLBACK: Use the
    standalone FALLBACK_LIBRARY (which loads classes via
    reflection and may silently fail for some card sets). LAST
    RESORT: If we're in a "Choose where to deploy" decision and
    nothing else matched, assume CHARACTER — the only other ground
    deploys are vehicles/weapons which always have distinctive
    keywords.

  V29.4 — AMSD deploys Star Destroyer from HAND or RESERVE DECK!
    Source: ActionTextEvaluator.java
    Previous code blocked when Executor was in hand — that was
    WRONG. AMSD is actually the BEST way to deploy Executor from
    hand because it deploys Piett+Executor simultaneously to the
    same system.

  V29.5 — GENERAL BUDDY SYSTEM — PREFER OWN LOCATIONS
    Source: CardSelectionEvaluator.java
    Characters should prefer deploying to locations they OWN or
    have friendly presence at. Deploying alone to opponent-
    controlled empty locations is bad — the opponent will likely
    reinforce and kill you. This applies to ALL decks, not just
    TDIGWATT. V29.6: EMPTY TABLE AWARENESS — If we have NO
    friendly characters anywhere on the table, someone has to go
    first! Reduce penalties so Rando doesn't stall. Still prefer
    own locations, but don't refuse to deploy just because only
    opponent locations exist.

  V29.6 — /V29.11: BLASTER RACK — ONLY RACK TO SAVE WEAPONS FROM DYING CHARACTERS
    Source: ActionTextEvaluator.java
    Blaster Rack stacks a weapon on it. This is ONLY useful at the
    END of a battle when a character carrying the weapon has been
    HIT or is about to be forfeited to satisfy attrition/battle
    damage. Proactively racking weapons outside of battle damage
    resolution is terrible — it strips characters of weapons
    before they can fire. Example: Vader had lightsaber, Rando
    racked it, Vader went to battle unarmed. Action text can be
    "Stack character weapon" OR contain "rack" + "stack"

  V29.7 — WE MUST ACCELERATE OUR PLANS
    Source: ActionTextEvaluator.java
    Card text: "Use 3 Force to take one Effect... OR Deploy a
    Blockade Flagship site... OR Take one Interrupt with
    'Podracer(s)'..." RULES: 1. Deploy Blockade Flagship site =
    the ONLY good use 2. Once that site is already on table, ALL
    uses of Accelerate are wasteful 3. Effect/interrupt pulls cost
    3 Force for minimal value — NEVER use 4. If grabber has
    grabbed this card, each copy costs +1 more — even worse V29.7
    FIX: The action texts from this card are: "Take Effect into
    hand from Reserve Deck" "Deploy a Blockade Flagship site from
    Reserve Deck" "Take Interrupt into hand from Reserve Deck"
    These do NOT contain "accelerate"! Must also identify by
    source card title.

  V29.8 — ZONE-AWARE FORCE LOSS — RESERVE/USED FIRST, HAND LAST
    Source: CardSelectionEvaluator.java
    When life force is healthy (reserve+used+force > 10): STRONGLY
    prefer losing from Reserve/Used/Force Pile. Cards in those
    piles can't be played — they're just life force. Cards in hand
    = deploy options = your entire next turn. Losing your whole
    hand = nothing to deploy = death spiral. When life force is
    critical (<= 10): Reluctantly lose from hand to preserve life
    force. V29.8 FIX: Previous scoring was too weak (+30 reserve
    vs -100 hand). Card-specific penalties (destiny, unique,
    priority) applied to ALL zones equally, which swamped the zone
    preference. Now: - Zone scoring is MASSIVE (+500 for reserve
    when healthy) - Card-specific penalties only apply to hand
    cards (not pile cards)

  V29.9 — REBEL BARRIER RISK ASSESSMENT
    Source: BattleEvaluator.java
    If opponent might have Rebel Barrier, they can EXCLUDE our
    strongest character from battle. If we initiate with Vader +
    Tarkin vs opponents, and they Barrier Vader, suddenly Tarkin
    fights ALONE vs everyone. When our strength is concentrated in
    one key character (Vader), initiating battle is very risky
    because Barrier negates that character.

  V29.10 — /V29.12: LIGHTSABER THROW — ADD DESTINY TO ATTRITION
    Source: ActionTextEvaluator.java
    After firing a lightsaber, Vader can also 'throw' it to add
    destiny to attrition. This is a SEPARATE action from firing —
    both can be done in the same battle. The throw adds extra
    attrition damage which can be decisive. Action text: "'Throw'
    to add destiny to attrition" V29.12 CRITICAL: Throw MUST score
    LOWER than Fire (300). Throwing places the lightsaber in Lost
    Pile — if Rando throws first, he can NEVER fire it. The
    correct sequence is: 1. FIRE lightsaber at target (hit them,
    reduce forfeit) — score 300 2. THROW lightsaber (sacrifice it
    for attrition destiny) — score 200 This gives "double trouble"
    — hit + extra attrition in the same battle.

  V29.12 — HUNT DOWN — VADER MUST LEAVE CASTLE AND HUNT
    Source: MoveEvaluator.java
    When playing Hunt Down V, armed Vader sitting at an
    uncontested location (like Vader's Castle) is WASTING turns.
    The whole point of Hunt Down is that Vader goes out to fight.
    If Vader is armed and there are no opponents at his location,
    give a massive bonus to move him toward the action. This
    overrides the natural tendency to "stay safe" at Castle.

  V29.13 — already does this when the action text includes the
    Source: MoveEvaluator.java
    destination — but for generic actions like "Move using
    landspeed" the destination is selected in a SEPARATE
    CARD_SELECTION decision (e.g., Rey at Cloud City: Lower
    Corridor moving to Upper Plaza Corridor, drain 3 → 0).
    V29.13's destination-from-text loop returns null and silently
    does nothing. V29.7 WEAPON HUNTER then scores the move +130
    because it sees ANY remote attack target — without checking
    reachability or the drain we'd lose. V85 sidesteps the
    destination ambiguity by checking the BEST (highest-drain)
    adjacent site. If even the best adjacent drain is lower than
    current, ANY move from here is wrong → HARD BLOCK. Fires
    BEFORE FLEE/ATTACK/V29.7 so the -2000 dominates their bonuses.

  V29.14 — EPIC EVENT STARTING LOCATION
    Source: CardSelectionEvaluator.java
    Locations whose game text mentions "epic" (e.g. Epic Event
    pull) are critical starting locations — without starting here
    the deck cannot pull its key starting effects. V71: now scans
    Light/Dark side texts (Ajan Kloss text was missed).

  V29.15 — Epic Event Saga Choice
    Source: ActionTextEvaluator.java
    "The Force Is Strong In My Family" presents choices: "My
    Father Has It", "I Have It", "You Have That Power, Too" The
    correct choice depends on the deck name: Luke deck → "I Have
    It" Anakin deck → "My Father Has It" Rey deck → "You Have That
    Power, Too"


  ════ V30 family ════

  V30 — UNIVERSAL MATCHING PILOT + STARSHIP DEPLOY RULE
    Source: DeployEvaluator.java
    If a pilot character and its matching starship are BOTH in
    hand, deploy them together NOW with maximum priority (+1000).
    This applies to ALL matching pilot/ship combos universally:
    Piett + Executor, Han + Falcon, Wedge + Red Squadron, etc.
    Also: deploy them to the system mentioned in the objective
    (+1000). If only the pilot is in hand and matching ship is in
    reserve with AMSD on table, soft-prefer AMSD (-500) but allow
    manual fallback. If matching ship is already in play, boost
    deploying pilot to it (+300).


  ════ V31 family ════

  V31 — PRE-FLIP vs POST-FLIP OBJECTIVE DEPLOYMENT STRATEGY
    Source: DeployEvaluator.java
    PRE-FLIP: Spread characters across objective locations to meet
    flip condition. - TDIGWATT needs to occupy 3 Bespin locations
    (system + 2 CC sites). - Solo deploys to objective locations
    are OK pre-flip — we need presence fast. - Bonus for deploying
    to unoccupied objective locations. POST-FLIP: Consolidate to
    fewer locations to hold. - TDIGWATT only needs 2 locations to
    prevent flip-back (1 CC site + Bespin system). - Penalize
    deploying to a 3rd objective location — consolidate to 2. -
    Bonus for reinforcing the 2 strongest held objective
    locations.


  ════ V32 family ════

  V32 — Use actual ability contribution instead of estimating from power.
    Source: DeployPhasePlanner.java
    Previous code used MIN(power, 4) which is wrong — a character
    with power 7 and ability 1 would be estimated as ability 4,
    causing the planner to think it reached the battle destiny
    threshold when it didn't.


  ════ V33 family ════

  V33 — NAMED WEAPON PRIORITY
    Source: DeployEvaluator.java
    Unique character-specific weapons (Vader's Lightsaber, Mara's
    Lightsaber, etc.) should deploy BEFORE generic weapons (Dark
    Jedi Lightsaber). If deploying a generic weapon on a character
    who has a named weapon available in hand, penalize the generic
    weapon to save the slot.


  ════ V34 family ════

  V34 — DESTINATION-AWARE CONTEST BONUS
    Source: MoveEvaluator.java
    Check if the specific destination of this move has opponents.
    Moving TOWARD opponents = good (can battle next turn, block
    their drains). Moving to empty location while opponents drain
    uncontested elsewhere = bad. This fixes the bug where Hunt
    Down and weapon hunter bonuses applied equally to ALL move
    actions regardless of where they actually go.


  ════ V35 family ════

  V35 — FAR MORE FRIGHTENING THAN DEATH
    Source: ActionTextEvaluator.java
    FMFTD has two modes: USED: Stack hatred on opponent's
    leader/ability>3 at battleground LOST: Add 1-2 battle destiny
    if Inquisitor with Jedi/Padawan/Hatred Detect via testingTexts
    or action text containing "far more frightening"

  V35.1 — Inquisitor recall — DON'T recall if opponents are nearby!
    Source: ActionTextEvaluator.java
    Eighth Brother's ability returns an Inquisitor to hand. Only
    do this if there are NO opponents at adjacent sites. If
    opponents are nearby, keep the Inquisitor to fight!

  V35.2 — During battle — but ONLY rack weapons from characters AT the battle!
    Source: ActionTextEvaluator.java
    Bug: Rando racked Vader's Lightsaber from Mustafar while
    battle was at Mos Eisley.

  V35.3 — STRICT hatred scoring — ONLY place hatred when Vader or Inquisitor
    Source: ActionTextEvaluator.java
    is at the SAME SITE as an opponent character. No
    proactive/remote hatred.

  V35.4 — YOU ARE BEATEN — DON'T WASTE ON UNDERCOVER SPIES
    Source: ActionTextEvaluator.java
    You Are Beaten targets opponent characters. But undercover
    spies appear on OUR side and aren't valid targets for combat
    effects. Don't waste this interrupt. Also: only use during
    battle or when it will lead to meaningful attrition.

  V35.5 — DON'T DEPLOY WEAK STARSHIPS AGAINST STRONG OPPONENTS
    Source: DeployEvaluator.java
    Emperor's Personal Shuttle (power 2) should NOT deploy to a
    system where Han, Chewie, And The Falcon (power 8+) is
    waiting. That's suicide. Check opponent ship power at the
    target system before deploying.

  V35.7 — Hatred requires INQUISITOR only (NOT Vader alone).
    Source: ActionTextEvaluator.java
    The card "There Are Many Hunting You Now" requires "your
    Inquisitor" at the same location. Vader alone cannot use
    hatred.

  V35.8 — IAYF can pull from Reserve Deck (free) OR Lost Pile (lose 1 Force).
    Source: ActionTextEvaluator.java
    Both should score EXTREMELY high when Vader is on table
    unarmed. The Lost Pile retrieval is a KEY mechanic of Hunt
    Down — Vader throws his lightsaber every battle, then
    retrieves it for the next battle.


  ════ V36 family ════

  V36 — SMART EMPTY DEPLOY — penalty depends on context.
    Source: DeployEvaluator.java
    If we have enough Force AND characters to challenge opponents,
    heavy penalty for empty site. But if we CAN'T challenge (low
    Force, no characters in hand to pair up), deploying to an
    empty drain site is acceptable for force economy.


  ════ V37 family ════

  V37 — HIGH-VALUE CHARACTER PROTECTION
    Source: CardSelectionEvaluator.java
    Characters with high power/ability should be protected from
    unnecessary forfeiting. Vader, Emperor, etc. are expendable in
    Hunt Down but only when there's actual damage to absorb. If a
    character "may not be used to satisfy attrition" (hit by
    weapon), the game forces them to be forfeited to battle damage
    instead. High-power unique characters that AREN'T hit should
    be kept alive.

  V37.1 — Only place hatred on OUR turn — placing during opponent's turn
    Source: ActionTextEvaluator.java
    wastes it because we can't follow up with a battle this turn.

  V37.2 — STUNNING LEADER — DEFENSIVE ONLY
    Source: ActionTextEvaluator.java
    Stunning Leader excludes characters from battle. Good when
    DEFENDING against a stronger opponent (saves Vader from
    certain death). BAD when WE initiated (we started the fight to
    WIN).

  V37.3 — NEVER cancel your OWN interrupts!
    Source: ActionTextEvaluator.java
    Rando played FMFTD then Sensed his own FMFTD — self-sabotage.
    Check if the interrupt being canceled was played by US. Clue:
    if the action text mentions a card that we just played this
    turn, or if we're the active player and the interrupt belongs
    to us.

  V37.4 — Check if we CAN actually deploy to any opponent location.
    Source: DeployEvaluator.java
    If not, empty site deploy is our ONLY option — reduce penalty.


  ════ V38 family ════

  V38 — REWORKED SOLO DEPLOY — VADER/EMPEROR SOLO OK, OTHERS NEED BUDDY PATH
    Source: DeployEvaluator.java
    Vader and Emperor (ability >= 6) can deploy solo anywhere.
    Other characters need a buddy PATH to 7 ability — either: 1.
    Deploy to a location with a friendly character (reinforce) 2.
    A paired deploy is affordable (deploy 2 chars this turn) 3.
    Deploy to non-battleground adjacent to battleground (staging)
    4. Objective-flip deploy This replaces the old V29 power < 6
    hard block.

  V38.3 — ALWAYS activate Force. ALWAYS. No exceptions.
    Source: ActionTextEvaluator.java
    Force is the currency for deploying characters. Without Force,
    Rando can't deploy, can't fight, and slowly loses by
    attrition. The old code had a Force pile cap of 20 and
    reserve-low checks that caused Rando to skip activation
    entirely, leading to death spirals. The
    ForceActivationEvaluator (INTEGER handler) now manages how
    MUCH to activate. This function just needs to score the ACTION
    highly.

  V38.4 — + V56 FIX 18: AGGRESSIVE DEPLOY — HAND SIZE + FORCE PILE URGENCY
    Source: DeployEvaluator.java
    Cards in hand do NOTHING. Cards on table drain/battle/occupy.
    The more cards in hand and Force available, the more urgently
    we must deploy. This counteracts the many -200 to -600
    penalties that stack up and cause Rando to pass with Force
    available and cards in hand. V56: Closed the mid/late-game
    urgency gap. Previously handSize < 9 gave ZERO urgency bonus,
    so once we emptied our hand to ~8 cards, scores crashed and
    Rando stopped deploying (see the "activated 8 force, deployed
    nothing" pattern on Turn 8). Now there is a baseline floor any
    time we have force to spend.


  ════ V40 family ════

  V40 — HOLD_BACK only applies to TDIGWATT (non-Hunt Down) decks.
    Source: DeployEvaluator.java
    Hunt Down and all other decks deploy freely — no hold back
    ever.


  ════ V41 family ════

  V41 — HUNT DOWN — MOVE DESTINATION AWARENESS
    Source: CardSelectionEvaluator.java
    When choosing a move destination (e.g., Vader's Castle
    ability), STRONGLY prefer locations with opponents, especially
    Jedi. This fixes Vader going to empty Mapuzo Safehouse instead
    of Malachor Entrance where Obi-Wan was draining 4 per turn.
    V67f2: Exclude UNDERCOVER SPIES from "go fight" bonus — a spy
    doesn't actively threaten us; moving Jedi to an opp-spy site
    wastes drain potential. FIXES uarc0hmiai1i594y replay: Ezra
    and Young Skywalker piled into Tatooine: Mos Eisley because
    V41 saw "+300 go fight" on Steve's U-3PO spy (power 1).

  V41.2 — PIETT DEPLOY — HOLD FOR AMSD
    Source: DeployEvaluator.java
    Piett is the matching pilot for Executor. He should NEVER
    deploy to ground when AMSD is on the table and Executor is
    still available — AMSD needs Piett IN HAND to fire. Deploying
    Piett to ground wastes the AMSD + Executor combo.


  ════ V42 family ════

  V42 — Use calculateActivationAmount which ALWAYS reserves cards for destiny draws.
    Source: ForceActivationEvaluator.java
    Old V38.2 logic only saved reserve when reserveDeck-maxVal <
    4, which meant early game activated everything and depleted
    reserve before the threshold kicked in.


  ════ V43 family ════

  V43 — ALWAYS activate at least 1 Force when asked. Activating 0
    Source: ForceActivationEvaluator.java
    causes the engine to re-ask the same question, creating an
    infinite loop. The game engine only asks this question when
    activation is possible.


  ════ V44 family ════

  V44 — /V67j: ALWAYS accept revert requests — never block the opponent
    Source: RandoCalAi.java
    from reverting. Steve's rule: "Rando must always allow a
    revert. If the gemp game has an error, I need to be able to
    always revert." V67j: Don't assume index 0 = Yes. Inspect the
    `results` param and find the actual "Yes/Allow/Accept"
    choice's index. Fallback to 0 if the array isn't available or
    no clear positive option found.


  ════ V45 family ════

  V45 — NEVER forfeit when all cards are immune to attrition
    Source: RandoCalAi.java
    (No explanatory comment in source.)


  ════ V46 family ════

  V46 — Turn 3+: HOLD_BACK only at start, not end of game!
    Source: DeployEvaluator.java
    Once past setup turns, deploy aggressively like any other
    deck.


  ════ V47 family ════

  V47 — LANDO MOVEMENT — STAY AT DINING ROOM
    Source: CardSelectionEvaluator.java
    Lando should NOT move from Dining Room. He establishes
    occupation there and moving wastes force / loses presence.
    Only move if we have 3+ friendlies at his current location
    (he's redundant) and destination is unoccupied CC site.


  ════ V48 family ════

  V48 — Check if Vader needs force reserved for movement
    Source: DeployEvaluator.java
    In Hunt Down, Vader starts at Vader's Castle and MUST move to
    fight Jedi. If bot spends all force on deploys, Vader is stuck
    at Castle doing nothing.


  ════ V49 family ════

  V49 — NEVER land a starship at a site without characters to protect it.
    Source: MoveEvaluator.java
    A starship at a site has power 0 — anyone can attack for
    catastrophic overflow damage. Only allow landing if the ship
    has passengers who can disembark and provide power.


  ════ V50 family ════

  V50 — Deploy power-disadvantage penalty — turns 1-3 only, even-power threshold.
    Source: DeployEvaluator.java
    After turn 3, deploy everywhere no matter what — can't afford
    to sit idle. Threshold: only penalize if we'd be at LESS than
    even power (was oppPowerHere - 3).


  ════ V51 family ════

  V51 — CONTEST OPPONENT DRAIN LOCATIONS — DRAIN 2+ IS AN EMERGENCY
    Source: DeployEvaluator.java
    Opponent drains are the #1 damage source. Drain 2+ sites are
    THE decisive battleground — both players will stack there,
    whoever wins that fight wins the game. Deploy aggressively to
    contest: flood the location with multiple characters. V51:
    Massively increased bonuses for drain 2+ sites. Every
    character sent to contest a high-drain site gets a large
    bonus, not just the first one.


  ════ V52 family ════

  V52 — FIX 11: DEPLOY MOMENTUM — Bonus for deploying multiple cards same turn
    Source: DeployEvaluator.java
    Check how much force has been used this deploy phase. If we've
    already spent force (meaning cards already deployed), give
    bonus to keep the momentum going. Initial force = force pile +
    force already spent. Current = force pile now. We approximate
    "force spent" by comparing current force pile to hand-implied
    max.

  V52b — FIX 13: HIDDEN PATH JEDI FLOOD (turns 1-2)
    Source: DeployEvaluator.java
    Deploy Jedi FIRST and FAST. Check both card title AND action
    text, because Fallen Order deploys Jedi via "Deploy a Jedi
    Survivor stacked here" where the card is Fallen Order, not the
    Jedi itself.


  ════ V53 family ════

  V53 — SPY FOLLOW — Undercover spy follows opponent when they move away
    Source: MoveEvaluator.java
    If our undercover spy is at a location where the opponent just
    left (no opponent presence remaining), move the spy to follow
    them. The spy is a leech — it sticks to the opponent's army to
    keep reducing their drain wherever they go. +500 to move spy
    toward opponent characters. -300 to move spy AWAY from
    opponent characters (defeats the purpose).

  V53b — HIDDEN PATH MANDATORY JEDI TRANSIT
    Source: MoveEvaluator.java
    HARD RULE: If playing Hidden Path, characters at Safehouse
    MUST move to Underground Corridor. Characters at Corridor MUST
    move OFF Mapuzo. Jedi Survivors move FREE on Mapuzo — there is
    ZERO cost. No force reserve excuses. The objective REQUIRES
    Jedi outside Mapuzo to flip. This overrides ALL other move
    scoring with +9999.

  V53c — BLOCK WOKLING EFFECT SEARCH (EARLY CHECK)
    Source: ActionTextEvaluator.java
    Wokling (V) costs 3 Force to search for an Effect from Reserve
    Deck. Action text: "Take an Effect into hand from Reserve
    Deck" MUST check EARLY before V29.7 PULL FIRST gives it +250.
    Check source card ID — if it's Wokling (bp 200_47), hard
    block.


  ════ V54 family ════

  V54 — FIX 16: SKYWALKER SAGA EPIC EVENT T1-3 SCRIPT
    Source: DeployEvaluator.java
    Mirror of V52 TDIGWATT T1 block, but for the Skywalker Saga
    Epic Event deck (also known by its key effect "Like My Father
    Before Me"). Rando has been losing badly with this deck
    because no script drives the turn-1 ramp. Priorities: PRIORITY
    1 = Tatooine sites (Cantina/Mos Eisley/Lars' Moisture Farm)
    PRIORITY 2 = Young Skywalker (or any Luke persona) PRIORITY 3
    = Luke's Lightsaber from hand DETECTION (V54.1): Skywalker
    Saga is an Epic Event deck — its objective-slot card is
    Anger/Fear/Aggression (V), which has cardType=EFFECT not
    OBJECTIVE. ObjectiveAnalyzer only detects true OBJECTIVE
    cards, so we can't rely on getObjectiveTitle(). Detect the
    deck by its unique starting-location signature instead: Endor:
    Anakin's Funeral Pyre (217_34) on our side of the table.


  ════ V55 family ════

  V55 — FIX 17: HIGH-ABILITY CHARACTER DEPLOY URGENCY
    Source: DeployEvaluator.java
    Generalized replacement for the earlier "Obi-Wan in hand"
    idea. Any character with ability >= 6 (Jedi/Sith/Lord tier —
    Vader, Emperor, Obi-Wan, Yoda, Luke, Mace, etc.) rotting in
    hand is wasted life force. Give it a steady deploy urgency
    bonus, scaled up in the early game. Side-agnostic, deck-
    agnostic.


  ════ V56 family ════

  V56 — mid-hand baseline — still incentivize deploying
    Source: DeployEvaluator.java
    (No explanatory comment in source.)


  ════ V58 family ════

  V58 — Reserve is now accurate (DTF, First Strike, maintenance,
    Source: DrawEvaluator.java
    contested count). If force pile is ABOVE reserve, we should
    aggressively DRAW the surplus into hand — hoarding does
    nothing.


  ════ V59 family ════

  V59 — UNIVERSAL SPY SCORING — runs regardless of ObjectiveAnalyzer state.
    Source: CardSelectionEvaluator.java
    FIXES Issue #1 from peaceful-pike replay: Jyn Erso deployed to
    empty Upper Chamber (+165) instead of Entrance where opponent
    drains 2/turn, because the spy-aware scoring at line ~2201 was
    trapped inside `if (deployObjAnalyzer.isAnalyzed())`. When
    Rando's deck doesn't have an analyzed objective (e.g., "Like
    My Father Before Me" variants), spy placement fell back to
    generic icon-count scoring which ties every BG. This block
    scores spies BEFORE the objective-gated block and sets a flag
    to prevent double-counting downstream.


  ════ V60 family ════

  V60 — RESERVE DECK PULLS — always positive, always fire
    Source: ActionTextEvaluator.java
    Steve's rule (feedback_reserve_deck_pulls.md): Reserve Deck
    pull effects are FREE VALUE — thin the deck, bring key cards
    into play. Always try them. Covers [Download] actions
    (Sai'torr Kal Fas → matching weapon, Visage of Emperor →
    lightsaber) and generic "X from Reserve Deck" / "Take X into
    hand" actions (Mining Village → Tala Durith, Malachor STE →
    Padawan, IMBATS, etc.) that weren't caught by earlier specific
    handlers. Hard-block only when: 1. DeckOracle confirms target
    NOT in Reserve (avoids deck reveal) 2. Force can't cover the
    action cost (defer to next turn) 3. This action has failed 2x
    in a row (shouldAvoidPulling)


  ════ V61 family ════

  V61 — EPIC EVENT SAGA CHOICE — "The Force Is Strong In My Family"
    Source: RandoCalAi.java
    FIXES Issue from is9j46shx6t0swby replay: Rando picked "My
    Father Has It" (for Anakin) in a Luke Saga Tatooine deck —
    Luke's power/defense boost was lost. The TFISMF decision
    surfaces as type=MULTIPLE_CHOICE with text 'Choose an option'
    (empty prompt) and the actual choices in the `results` param.
    The V29.15 ActionTextEvaluator check was looking in the prompt
    text instead of the options array, so it never triggered and
    Rando defaulted to index 0 = "My Father Has It". Luke deck →
    "I Have It" Anakin deck → "My Father Has It" Rey deck → "You
    Have That Power, Too"


  ════ V62 family ════

  V62 — DON'T DILUTE OUR OWN UNDERCOVER SPY
    Source: CardSelectionEvaluator.java
    Undercover spies block opponent force drains at their
    location WHILE they remain the only character there with
    visible presence. If we move non-spy characters to the same
    site, our characters reveal presence openly, the spy is no
    longer the sole drain-blocker, and the spy's special power is
    wasted. Better to keep Jedi/non-spies at safe sites and let
    the spy do its solo blocking job. FIXES fmz03bjz79k61img
    replay: Rando deployed Boushh as spy at Sith Temple Entrance
    (Emperor's location), then moved BOTH Jedi to the SAME site —
    revealing presence and making the spy useless.
    (Correction 2026-05-20: prior wording confused power with
    presence/ability; rewritten per BOTVHD PR #3260 review.)


  ════ V63 family ════

  V63 — ROUTING FIX: "Choose card to move to, or click 'Done' to cancel"
    Source: CardSelectionEvaluator.java
    is the DESTINATION-selection decision. It must route to
    evaluateMoveDestination BEFORE the generic "click 'done' to
    cancel" branch — otherwise move-destination decisions fall
    through to evaluateTargetSelection (which scores them as
    "target opponent's card" +50), bypassing V62 SPLIT SITE and
    V62 SPY DILUTION logic. V67d ADDITION: "Choose where to move
    <Luke> using landspeed" is ALSO destination selection — the
    cardHint here is the CHARACTER being moved, not the
    destination. The "where to move" prefix distinguishes it from
    character-selection text "card to move to <X>". FIXES
    awjc89tacm7cxvtv replay: Rando moved Luke STU↔STG repeatedly
    because both options scored +120 (generic target +50 +20)
    instead of running through evaluateMoveDestination's drain/BG-
    aware scoring.


  ════ V64 family ════

  V64 — POWER-AWARE MOVE DESTINATION — don't send Jedi to their death
    Source: CardSelectionEvaluator.java
    When transiting Jedi off Mapuzo, avoid sites where the
    opponent's total power exceeds what our available Jedi can
    match. Rando previously sent Kelleran (power 5) to Jabiim:
    Starship Hangar where Grand Inquisitor + Emperor Palpatine sat
    (combined 13+ power) — instant kill. Hidden Path Jedi are ~6-7
    power flipped, so destinations with opponent power ≥ 8 without
    our own support are suicide moves. FIXES z7qk4ap0b72e4uvm
    replay (msg 324): Kelleran moved into Grand Inquisitor +
    Emperor → Steve won battle at msg 451. Steve's preferred
    strategy: drain pressure via split-sites, not battle
    initiation into stronger enemies.


  ════ V65 family ════

  V65 — SMART WRONG-DIRECTION: Skip the hard-block when:
    Source: CardSelectionEvaluator.java
    (a) Our own undercover spy is at the "draining" site (spy
    neutralizes their drain — it's not actually a threat) (b) The
    "draining" site is suicide to enter (opponent power too high
    for our Jedi) FIXES qi99bkot034gso86 replay: Obi-Wan forced to
    join Boushh at Jabiim: Starship Hangar vs Lord Vader + DVL —
    spy was already blocking the drain, other BGs were safer drain
    targets.

  V65a — Our spy at the drain location blocks it. Skip.
    Source: CardSelectionEvaluator.java
    (No explanatory comment in source.)

  V65b — Suicide destination — opponent too strong for single Jedi.
    Source: CardSelectionEvaluator.java
    Treat Hidden Path flipped Jedi as ~6 power baseline.


  ════ V66 family ════

  V66 — MEMORY AUDIT: Unified pull validation via DeckOracle.
    Source: DeployEvaluator.java
    Catches pulls that the named-target/generic regexes miss, AND
    catches "WASTEFUL" pulls (target already in hand/play).
    Steve's feedback: "Rando doesn't seem to remember what's in
    his hand, force pile, reserve, used or lost pile." This runs
    AFTER the older named/generic guards so those more specific
    penalties still fire first.


  ════ V67 family ════

  V67aa (2026-05-03) — HIDDEN PATH JEDI SUICIDE BLOCK.
    Source: CardSelectionEvaluator.java
    When on Hidden Path pre-flip, Jedi survivors are power 3
    (Fallen Order Effect), and the V41 CONTEST DEST 'go fight'
    bonus would send them into power-8+ enemy sites where they get
    killed solo. Symptom: Rando moved both Jedi to Hoth (where
    Steve had power 8) instead of spreading to empty Jabiim, then
    sent solo Obi-Wan to Hoth and lost the game. Rule: on Hidden
    Path pre-flip, any destination with opp power ≥ 5 AND our
    power = 0 is suicide for the weak Jedi → hard-block.

  V67ab (2026-05-03) — Only stack ability at BATTLEGROUNDS.
    Source: DeployEvaluator.java
    V33 BUDDY FIX/BONUS was firing for non-battleground sites
    where battles can't happen — wasting characters on places they
    can't contribute. Symptom: Mira deployed to Coruscant: The
    Works (non-BG) to "buddy" with Sidious — but Sidius doesn't
    need protection there (no battles), and Mira got trapped. The
    buddy ability >= 7 threshold exists for BATTLE destiny; non-BG
    sites don't have battles, so don't reward stacking there.

  V67ac (2026-05-04) — FORCE-COST GUARD for card-action reserve pulls.
    Source: ActionTextEvaluator.java
    Symptom: Rando used Vader's Castle's 'deploy Vader from
    Reserve Deck' action with only 4 force in pile. Vader costs 7
    (6 with Castle reduction). Action FAILED but the search
    revealed Rando's reserve deck to opponent. V67h validates
    target EXISTS in zone but doesn't validate AFFORDABILITY.
    Approach: scan Rando's reserve deck for cards matching source
    card's parsed targets. Find the cheapest match. If even the
    cheapest exceeds available force pile size, hard-block (action
    would fail + leak reserve).

  V67ae — GAME-TEXT 'MOVE TO HERE' DRAIN GUARD
    Source: ActionTextEvaluator.java
    Steve's report: Rando moved Vader from CC Lower Corridor
    (3-drain battleground) to Mustafar: Vader's Castle (0 drain)
    using Castle's 'may move character to here' game-text action.
    V67g MOVE-FROM-DRAIN didn't fire because that's wired to
    landspeed/CardSelectionEvaluator, not card-action moves
    through ActionTextEvaluator. Rule: if the source card's
    location has zero drain potential (no opp icons) AND it's a
    'move <character> to here' action, penalize. The 'free move'
    attractiveness shouldn't outweigh losing drain pressure.

  V67af — RETURN-OWN-CHARACTER-TO-HAND BOUNCE BLOCK
    Source: ActionTextEvaluator.java
    Steve's report: Rando deploys General Grievous, then uses
    Grievous's 'Lose 1 Force to return Grievous to hand' game text
    to bounce him — wasting both the deploy cost AND the bounce
    cost. V29.7 BOUNCE only fires for 'Take X into hand' actions;
    Grievous and similar cards say 'Return X to hand', which V29.7
    misses entirely. Rule: when an action says 'Return <X> to
    hand' AND the source card is a character we own AND the action
    requires losing force, hard-block. The tactical use case
    (escape death) is too rare to justify Rando's pattern of
    deploy-then-bounce loops.

  V67ag (2026-05-04) — NON-BG STACKING PENALTY.
    Source: DeployEvaluator.java
    V67ab skipped the buddy BONUS at non-BG, but didn't penalize
    STACKING. Steve's report: 'Rando deployed Sidious to The Works
    (good — drains for 1) but then loaded extra characters there
    (useless — they can't battle anywhere they're stacked).' Rule:
    if non-BG already has any of our characters, additional
    characters wasted there.

  V67ah (2026-05-04) — 'Deploying to non
    Source: CardSelectionEvaluator.java
    battleground sites is mostly useless.' Old -60 was too weak.
    But Sidious-to-The-Works is OK as drain staging when the site
    has opp icons. Tiered penalty: - Non-BG with drain icons (opp
    force): -100 (acceptable first-character drain post; V67ag
    adds another -300 if a friendly is already there) - Non-BG
    with zero opp icons (truly useless): -350 (no battles AND no
    drain — pure waste)

  2026-06-03 V67ai BARE-DEPLOY GATE TIGHTENING (Steve, Mustafar Docking Bay):
    Replay: Rando used the Mustafar docking bay's game text "Deploy starfighter
    with 'Vader' in title here" — the OUTER DeployEvaluator action scored
    +1530 because V67ai's `isActualLocationDeploy` predicate mis-fired:
        category == LOCATION
        && (v24ActionLower.equals("deploy")
            || v24ActionLower.startsWith("deploy ")
                && !contains("from reserve")
                && !contains("padawan")
                && !contains("jedi survivor")
                && !contains("tala durith"));
    The denylist (padawan/jedi-survivor/tala-durith/from-reserve) missed
    "starfighter", "alien", and every other game-text-pull keyword. Rando
    treated the starfighter pull as a Tier-4 location-from-hand deploy and
    slapped +1400 on it, then committed the outer, then the sub-decision
    landed a 0-power ship in a docking bay (free kill).
    Fix: legitimate location deploys come through as bare "Deploy" (cardId
    resolves to the location — confirmed by the "V29 EARLY LOOKUP: Resolved
    bare 'Deploy' via cardId NNN" log lines). All "deploy <X>" variants are
    game-text pulls. Drop the startsWith branch entirely:
        boolean isActualLocationDeploy = category == CardCategory.LOCATION
            && v24ActionLower.equals("deploy");
    The else-branch (V60 V24 SKIP) catches everything else and logs the
    skip, so Rando's V67i / V60 / V67bg generic-pull logic handles the
    actual scoring. Edits old V67ai gate condition — no new V-tag, no new
    code, just a tighter predicate. Council verdict (engineer, rules_lawyer,
    voice_of_reason): unanimous "edits existing logic."

  V67ai (2026-05-07) — TIERED LOCATION DEPLOY ORDER.
    Source: ActionTextEvaluator.java
    Steve's rule: 'Rando should never under any circumstances
    avoid deploying locations.' Location-pull cards have a strict
    priority order so the cheapest source goes first and we keep
    the most future flexibility: Tier 1: Objective pull → +2000
    (free, mandatory effect) Tier 2: Effect-card pull → +1800
    (already on table, low cost) Tier 3: Interrupt pull → +1600
    (one-shot, save for after objective/effect) Tier 4: Hand
    deploy → +1400 (DeployEvaluator handles this) Determine source
    category from the source card's blueprint.

  V67aj (2026-05-07) — SPREAD-AWARE CHARACTER DEPLOY DESTINATION
    Source: DeployEvaluator.java
    Steve's rules: 1. Buddy system was over-firing: Rando stacked
    all characters on one location all game. 2. Where Rando
    deploys is critical — must check objective for flip-required
    locations (Endor Operations, Dark Deal, etc.). 3. ALWAYS favor
    battlegrounds (battles + drains). Tiered destination scoring
    layered on top of V51 OBJ FIRST: Objective-required + BG,
    empty: +500 (urgent — occupy now) Objective-required + BG,
    stack 1-2: +250 (reinforce) Objective-required, stack 3+: 0
    (sufficient — spread instead) BG (not obj-required), empty:
    +300 (open new front) BG (not obj-required), stack 1-2: +100
    (mild reinforce) BG (not obj-required), stack 3+: -300 (V67aj
    OVER-STACK) Non-BG: handled by V67ah (already in
    CardSelectionEvaluator) The OVER-STACK penalty fights the
    over-buddy clustering Steve called out. Combined with V51 OBJ
    FIRST (+300) and V29.7 (+80 for BG), an empty objective-
    required BG can score +880 across rules.

  V67ak (2026-05-07) — KEY-CHARACTER DEPLOY PRIORITY
    Source: DeployEvaluator.java
    Steve's rule: 'If the objective or epic event states a
    specific character or character type, Rando should favor
    deploying those characters first. Hunt Down V mentions Vader
    being deployed to flip the objective, so Vader must come out
    first. Universal mechanism — no hardcoded character lists per
    deck.' Implementation:
    ObjectiveAnalyzer.getStrategyCharacterTokens scans objective
    text + Epic Event game text + Effect game text on Rando's
    side, extracts capitalized persona-name tokens (filtered for
    generic words). Any character whose title contains a token
    gets +800. Skip if this character (or persona) is ALREADY on
    table — once Vader is out, additional Vader-named cards (which
    would be unique-blocked anyway) don't need the priority.

  V67al (2026-05-07) — POWER-STACK SPREAD PENALTY
    Source: DeployEvaluator.java
    Steve's rule: 'In the last games he had a site with like 25-40
    or so power, way more than enough to protect and spread.'
    Beyond ability/character-count stacking, RAW POWER stacking is
    the clearer signal: once a site has 20+ power of friendly
    characters, you have plenty for any battle there. Adding more
    to that site is sub-optimal — those characters could be
    opening new fronts elsewhere. Tiered power-stack penalty:
    20-24 friendly power: -200 (already strong, prefer spread)
    25-34 friendly power: -400 (heavily over-stacked) 35+ friendly
    power: -700 (catastrophically over-stacked) Skipped if
    location is objective-required (V67aj already tapers obj-req
    at stack 3+ to 0 — power doesn't override flip requirement).

  V67am (2026-05-07) — Bumped V67m weapon-pull bonus +200 → +600.
    Source: ActionTextEvaluator.java
    Steve's order: 'pull weapon from reserve via
    effect/interrupt/objective FIRST, then deploy from hand.' Old
    V67m at +200 was below hand-deploy bonuses (V29.11 LIGHTSABER
    +400-500), inverting Steve's priority. +600 ensures pull-from-
    reserve actions outscore hand-deploy of the same weapon class.
    Once-per-game/turn pull effects are precious — fire them first
    while available; hand cards can deploy any turn.

  V67an (2026-05-07) — WEAPON SWAP TO FREE MATCHING SLOT
    Source: ActionTextEvaluator.java
    Steve's rule: if Rando has a non-unique/non-matching weapon
    attached to a character (e.g., generic Dark Jedi Lightsaber on
    Vader) AND has a unique persona-matched weapon for that
    character in hand (e.g., Vader's Lightsaber), Rando should
    TRANSFER the wrong weapon to a buddy at the same site. After
    the transfer the matching character is unarmed, so the V67ad
    two-weapon hard-block lifts and the matching unique weapon can
    deploy on its persona — net result: 2 characters armed,
    persona bonuses active for the matching weapon (immune, fire-
    for-free, +power, etc.). Detection: action text starts with
    "Transfer" (rules-level transfer) or contains "Transfer
    device" / "Transfer weapon". Bonus +400 fires when: - The
    transfer source weapon is NOT unique OR has no
    matchingCharacter filter pointing at its current attachee -
    Rando has another weapon in hand whose matchingCharacter
    filter DOES target the current attachee (or whose title
    matches the persona) If we can't determine matchingCharacter
    unambiguously, fall back to a milder +150 ('transfers usually
    mean tactical swap').

  V67ao — Per Steve, no soft penalties for character pulls when locations
    Source: ActionTextEvaluator.java
    are in hand. The V67ai location tier bonuses (+1400 to +2000)
    already outscore character pulls (V67ak +800, others lower),
    so Combined Evaluator picks locations first naturally. The
    hard-block ordering gates only apply where the action would
    actually FAIL (weapon/device pull with no character host — see
    V67ao gates inside V67am blocks).

  V67aq (2026-05-08) — UNIVERSAL ONE-WEAPON RULE
    Source: DeployEvaluator.java
    Replaces the entire V29.11/V29.9/V67ad/V67ap stack of
    hardcoded character-name detection. Steve's rule, full stop:
    "No second weapon should deploy on ANY character. Period."
    Universal logic, no hardcoded names, no faction filters, no
    persona matching for the BLOCK side: 1. Iterate every Rando
    character in play. 2. Count how many are unarmed (no WEAPON
    attached). 3. If at least one unarmed Rando character exists →
    allow the weapon deploy and give +300 (someone good can take
    it). 4. If ZERO unarmed characters AND at least one armed
    character → hard-block (-9999): every character would be a
    2nd-weapon stack or the weapon would orphan. 5. If zero
    characters at all → V67ao gate elsewhere blocks.
    CardSelectionEvaluator handles which specific character to
    attach to (target-pick layer, V67an handles persona-matching
    for swaps).

  V67ar (2026-05-08) — UNIVERSAL ONE-WEAPON RULE for pull path.
    Source: ActionTextEvaluator.java
    Mirrors V67aq's logic from DeployEvaluator. Count UNARMED
    Rando characters on table — if zero unarmed (every char
    already armed), hard-block the pull because it would put a 2nd
    weapon on someone. Also blocks the 'no chars at all' case
    (V67ao original intent). No hardcoded character names. The
    same rule fires regardless of which weapon (Sidious'
    Lightsaber, Ventress' Lightsabers, Vader's Lightsaber,
    anything) and which character the pull would target.

  V67as (2026-05-08) — SPREAD-AWARE DEPLOY DESTINATION
    Source: CardSelectionEvaluator.java
    Mirrors V67aj+V67al (which live in DeployEvaluator) for the
    CardSelectionEvaluator path — needed because hand-deploy
    actions say "Deploy <character>" with NO location in the
    action text; the destination is picked here in
    evaluateDeployLocation via a sub-decision. V67aj/V67al never
    fired for hand-deploys because their
    actionText.contains(loc.getTitle()) check always failed.
    Steve's report: 59 of 59 character deploys went to one site
    (Hoth: Defensive Perimeter, 3rd Marker). Now scoring per
    candidate destination here: stack count: friendly characters
    at this destination power total: sum of friendly character
    power at destination Tiered spread bonus / anti-stack penalty:
    Empty obj-req BG: +500 Empty BG (not obj-req): +300 1-2
    friendlies + BG: +100 3+ friendlies + BG (not obj-req): -300 ←
    anti-stack 20-24 friendly power, non-obj: -200 ← V67al-style
    25-34 friendly power, non-obj: -400 35+ friendly power, non-
    obj: -700

  V67at (2026-05-08) — END-GAME FORCE PRESERVATION.
    Source: ForceActivationEvaluator.java
    Steve's refined spec: 'He needs to save at bare minimum 2
    force during activation in reserve if reserve, used and force
    pile total 10 or less.' Trigger: total life force (reserve +
    used + force pile) ≤ 10. Action: activate at most maxAvailable
    - 2 (save 2 from the generation). V43 minimum 1 still applies
    elsewhere — never zero. V57 ACTIVATE FULL preserved as default
    for early/mid-game when life force > 10.

  V67au (2026-05-08) — RETREAT-TO-DRAIN STRATEGY
    Source: CardSelectionEvaluator.java
    When Rando is at an over-contested battleground (enemy power
    exceeds Rando's), and the candidate move destination is a SAFE
    adjacent non-BG with friendly drain icons and no opponents,
    this is a 'deploy-then-move-to-drain' play: Rando deploys
    characters to a contested BG (because that's where his deck
    wants them), then moves them out next turn to an empty
    drainable adjacent site. Net effect: avoids battle suicide AND
    drains uncontested AND spreads pressure. Strict version
    (Steve's choice): only fire when there's a CONFIRMED escape
    route — destination has zero opponents AND friendly drain
    icons. Otherwise no bonus (don't reward arbitrary retreats).

  V67aw (2026-05-08) — DEFER concede until after the next battle phase.
    Source: RandoCalAi.java
    Steve's rule: 'Change Rando's Concede logic to only happen
    after the next battle phase has ended.' Reasons: lets the
    current turn's planned battle play out, lets opponent finish
    their attack cleanly, and avoids mid-decision concedes that
    look glitchy. The actual concede fires in trackGameState when
    the BATTLE → other-phase transition is observed.

  V67ax — DEPLOY PHASE SCRIPT: deterministic step ordering during DEPLOY phase.
    Source: RandoCalAi.java
    Walk steps 1→5; restrict the evaluator pipeline to actions
    qualifying for the first non-empty step. Existing scoring
    (V67ai/aj/ak/al/aq/ar/as) picks within the qualifying set.
    Active only for CARD_ACTION_CHOICE during DEPLOY.

  V67ay (2026-05-08) — UNIVERSAL ONE-WEAPON RULE for reserve-deck SELECT step.
    Source: CardSelectionEvaluator.java
    Rando's V67ar block in ActionTextEvaluator's "from reserve
    deck" branch SKIPS weapon-pull actions whose source card ALSO
    offers a location pull (because of the `!v67lAddsLocation`
    exclusion). Evil Is Everywhere is the canonical case:
    '[download] a mobile hallway or [Episode I] lightsaber'. V67ar
    lets the parent action through; Rando initiates it, GEMP
    shuffles reserve, then asks "pick a card to deploy" with a
    list of matching candidates (weapons + locations). At THAT
    prompt the weapon option needs to be hard-blocked when every
    Rando character on table is already armed — otherwise Rando
    picks Asajj's Lightsabers and stacks it on Sidious (already
    wielding Sidious' Lightsaber). This check fires
    UNCONDITIONALLY before the per-blueprint loop computes the
    all-friendly-chars-armed counters once, then the loop applies
    -9999 to every weapon-category candidate. Locations (the
    mobile hallway alternative) are unaffected — Rando still picks
    one if available.

  V67b — Check if the deploying card is a TRUE Jedi Survivor.
    Source: CardSelectionEvaluator.java
    Drop the previous persona-name fallback (which incorrectly
    matched "Ahsoka Tano With Lightsabers", "Obi-Wan With
    Lightsaber", "Luke With Lightsaber", etc. — those are Jedi but
    NOT Jedi Survivors and CAN'T transit off Mapuzo via
    Underground Corridor). Authoritative test: game text contains
    the literal phrase "Jedi Survivor" (the keyword that lets
    Underground Corridor's transit action target the card). FIXES
    xxhj3qwhxzmhrdym replay: Ahsoka Tano With Lightsabers deployed
    to Mapuzo: Mining Village and got stuck.

  V67ba (2026-05-08) — EXEMPT generic deploy-from-hand actions.
    Source: ActionTextEvaluator.java
    Action text "Play a card" / "Deploy" / "Deploy a card" is the
    ENTRY POINT to the deploy-from-hand sub-decision
    (CARD_SELECTION among hand cards). Penalizing it -800 means
    Rando never picks it, so the location in hand never gets
    deployed — the very thing V24.4 is trying to force. FIXES
    115yinsdp3t7t2q1.xml.gz: turn 2 had only 'Play a card' + 'Take
    Imperial Decree' as options; V24.4 penalized 'Play a card' to
    -840, Pass scored -168, Rando passed.

  V67bc — DPS HIERARCHY WALK: when DPS provided ordered step buckets,
    Source: CombinedEvaluator.java
    walk them top→bottom. For each bucket, pick the highest-
    scoring action. If that action's score is above the bad
    threshold, return it. Otherwise fall through to the next
    bucket. PASS only when ALL buckets exhausted with all-bad
    scores. This implements Steve's principle: "walk the full
    hierarchy every call, take first viable, only pass when
    nothing viable." Replaces the older single-set filter that
    wrongly forced PASS when STEP 1's only candidate was hard-
    blocked (e.g. K&D pull when no CC interior sites left in
    reserve → -9999 → PASS even though STEP 2/3 had 9 viable
    character deploys).

  V67bd (2026-05-09) — Attrition can ONLY be paid by
    Source: CardSelectionEvaluator.java
    forfeiting — reserve force cannot cover it. So a forfeit is
    GOING TO HAPPEN no matter what; doing it FIRST also absorbs
    battle damage that would otherwise burn reserve cards. The
    previous bonus (150 + fv*20) was too low — pile-loss with V67y
    +500 - V22.3 -40 - CANNOT-attrition -150 = +310 beat the +250
    a fv=5 forfeit got, so Rando burned 4 reserve cards in battle
    #1 before forfeiting Chiraneau anyway (replay
    jzhprmm64t32wz8g). New formula: each fv point absorbs 1
    attrition or 1 damage. Effective coverage = min(fv,
    attrition+damage). Bonus scales at 80/coverage so a fv=5
    forfeit covering 5 of 10 owed damage scores +400+ (decisively
    beating pile-loss +310). Plus a flat +200 floor since
    attrition is unavoidable.

  V67be (2026-05-09) — V67y REMOVED from this combined prompt.
    Source: CardSelectionEvaluator.java
    Steve's clarification: "V67y was only meant for moments when
    force is required to come from hand or reserves. In battle you
    still have the option to forfeit from site. V67y outweighs a
    very important logic [V22.3 forfeit-first]." V67y added +500
    to pile-loss / -500 to hand-loss. That dominated V22.3's
    -40/-80/-120 forfeit-first penalty, silently regressing the
    original "forfeit before burning reserve" rule. Replay
    jzhprmm64t32wz8g battles #1 & #2: Rando burned 4 reserve cards
    before forfeiting Chiraneau anyway. FIX: V67y stays in the
    STANDALONE evaluateForceLoss method (V29.8 already there with
    the same zone-aware semantics) for non-battle force-loss
    prompts. The combined battle prompt is governed by

  V67bg (2026-05-10) — TYPE-AWARE pull validation.
    Source: DeployEvaluator.java
    The old code substring-matched a generic noun ("location",
    "site", "weapon", "bay") against card TITLES. That always
    misses for category nouns because no card is literally titled
    "location" — the SWCCG vocabulary uses these words as TYPE
    indicators (CardCategory / CardSubtype / Icon / Keyword), not
    as titles. Symptom: Hunt Down's '[download] a Cloud City or
    Malachor battleground site' hard-blocked every turn. Same on
    IBS (docking bay). Fix: resolve the noun to a typed Filter via
    DeckOracle.resolveCommonNounToFilter(). The engine's own
    filter semantics then answer "is anything in reserve
    satisfying this filter?" — the same way the card's
    DeployCardFromReserveDeckEffect would search. Proper-noun
    targets like "Tala Durith" still hit the named-target matcher
    above (case-sensitive proper-noun regex). Memory:
    ~/.claude/projects/-Users-steve-gemp-swccg-public/
    memory/feedback_card_search_by_type_not_text.md

  V67bh (2026-05-10) — SMALL-DAMAGE PROTECTION FOR
    Source: CardSelectionEvaluator.java
    VALUABLE UN-HIT CHARACTERS. Steve's rule: damage 1-3 with a
    high-value (fv ≥ 4) un-hit character → keep the char, lose
    from reserve/hand instead. A hit char already had its fv reset
    to 0 by the weapon hit — forfeit costs nothing strategic, no
    protection. Supersedes the earlier V67t SMALL DAMAGE rule (≤2
    / fv≥2). Lower the bar to "fv ≥ 2 / damage ≤ 2" still applies
    as a secondary milder discouragement so even cheap chars
    aren't wasted on 1 damage.

  V67bi — FORCE LIGHTNING SELF-TARGET HARD-BLOCK (Steve, 2026-05-10)
    Source: ActionTextEvaluator.java
    Hard-block Force Lightning if there's no opponent character in
    play to target. The engine already requires the granting card
    (Emperor or equivalent) to be present for the action to even
    appear, so we don't need to look for Emperor — we just verify
    a valid OPPONENT target exists. Otherwise Rando burns 5 force
    to hit his own character. Pattern extends to any "target a
    character" Sith damage interrupt (Force Push, Lightsaber
    Combat, etc.) — add per card-title as they surface in replays.

  V67bj (2026-05-11) — THREAT-AWARE DESTINATION
    Source: CardSelectionEvaluator.java
    Don't pick a destination where opponent's power on the site
    exceeds Rando's TOTAL available power (already-here + the char
    being deployed + chars in hand still deployable AND leaving 2
    force for battle interrupts) by 4 or more. Replay
    6fqi4jm1kkp7e9i8: Stormtrooper Patrol (power 2) solo at Guest
    Quarters across from Rey + Jedi (15 power). Hand had no chars
    to swing the matchup. Should have refused. -400 magnitude:
    bigger than typical destination bonuses (drain +30, V67as +500
    obj-req, etc.) so this dominates when site is genuinely bad,
    but proportional to the post- V67bk score landscape (no V52
    SPEND FORCE +300 anymore).

  V67bk (2026-05-11) — V52 SPEND FORCE +300 REMOVED
    Source: DeployEvaluator.java
    Old rule: when force pile > 3, every deployable card got +300
    "deploy everything, don't hoard." Steve's complaint: "it sets
    him up for bad moves. Better to save force for interrupts,
    next turn, having force during opponent's turn to play
    interrupts." The +300 was overriding site-quality scoring, so
    Rando dumped low-power chars into bad sites (e.g.,
    Stormtrooper Patrol solo at Guest Quarters across from a Jedi
    stack) just because force was available. With this removed,
    weak-site deploys lose to PASS naturally when no good
    destination exists, and saved force is available for
    interrupts on the opponent's turn. V52 MOMENTUM (below)
    intentionally kept for now — Steve called out the SPEND FORCE
    rule specifically. Revisit if same symptom.

  V67bl (2026-05-11) — V29 PAIRED "solo OK" exception REMOVED.
    Source: DeployEvaluator.java
    Old rule: if any character in hand had force-cost numbers that
    allowed it to "follow" the solo char, V29 PAIRED gave 0
    penalty — full pass on solo deploy. But there's no guarantee
    the hypothetical buddy goes to the SAME site, and in replay
    6fqi4jm1kkp7e9i8 Stormtrooper Patrol got a free solo pass to
    Guest Quarters because "Vader in hand could follow" — Vader
    went to Docking Bay instead, Stormtrooper died to a Jedi
    stack. V38 SOLO CAUTION (-150) below now applies regardless of
    hand contents. The "buddy follows" credit is properly earned
    later by V38 REINFORCE STRONG ALLY (+300) when the buddy
    actually deploys to where the solo char IS — paying for actual
    co-location, not hypothetical plans.

  2026-06-03 STARSHIP TO DOCKING BAY MAGNITUDE (Steve, Mustafar replay):
    Source: ai/models/rando/evaluators/CardSelectionEvaluator.java ~line 1144
    The ported-from-Python rule "NEVER deploy starships to docking bays
    (0 power)" used VERY_BAD_DELTA (-150). Replay showed sub-decision
    totals of -100 per docking bay site after VERY_BAD_DELTA applied,
    which was not low enough to dominate other deploy options or push
    decisively below pass. Ship landed at 0 power, Steve's chars
    chunked it next turn.
    Steve's directive: edit existing logic, don't add new rules.
    Council-confirmed (engineer, rules_lawyer, voice_of_reason
    unanimous: this is a pure magnitude edit, no scope or gate
    expansion).
    Change: -150 → -1500 on this single addReasoning call only. Other
    rules sharing VERY_BAD_DELTA are untouched (constant kept at -150
    so unrelated callers don't shift). Same predicate (isStarship &&
    isDockingBay), same source, same trigger — bigger number.
    Effect: sub-decision totals around -1450 per docking bay site,
    every option decisively below pass. When the cancel-loop detector
    sees 3 consecutive cancels on the same outer decision (e.g. the
    docking bay's "Deploy starfighter here" game text), it blocks
    that outer action via blockLastActionOnCancel.

  2026-06-03 V67bn DEFICIT UPPER CAP (Steve, Audience Chamber underpowered fight):
    Steve: "Rando deployed guys to fight me but very underpowered."
    Replay: Audience Chamber, our 3 vs opp 12, deficit 9. V67bn fired +800
    "REINFORCE OUTGUNNED (Braveheart): NO ESCAPE, DEPLOY HERE to minimize
    overflow!" → Rando piled successive power-3 chars in, each ate forfeits
    in the overflow battle without closing the gap.
    Math: adding +3 power to a -9 deficit makes the deficit -6 instead of -9
    (save 3 Force from overflow) but loses the extra char's forfeit (~3) and
    ability — net loss vs just losing the original battle alone.
    The original gate was `v67bnOutgunned = (theirPower - ourPower) >= 4f` —
    no upper bound, so any deficit >=4 triggered the +800 reinforcement,
    including 9, 12, 20… braveheart-style piling at any scale.
    Fix: cap the deficit at ≤5 so the rule only fires when reinforcement
    can PLAUSIBLY close the gap (1 mid-power char absorbs 4, maybe 5 in
    deficit). Beyond that, gap is unclosable and reinforcing just hands
    the opponent more forfeits. Edited gate condition:
        float v67bnDeficit = theirPower - ourPower;
        boolean v67bnOutgunned = v67bnDeficit >= 4f && v67bnDeficit <= 5f;
    Edits old V67bn gate, same +800 magnitude, no new V-tag. Council
    verdict (engineer, rules_lawyer, voice_of_reason): unanimous "edits
    existing logic, threshold adjustment reasonable."

  V67bn (2026-05-11) — Extended the OLD V29 REINFORCE rule
    Source: CardSelectionEvaluator.java
    beyond its `ourPower <= 5` weakness gate. The old gate missed
    STRONG-but-outgunned solo chars — Vader (power 6) alone vs 2
    Jedi (power 8-13) failed the gate, so Yularen got pulled to a
    spy site (+940) instead of joining Vader (+180). Steve's rule:
    "deploy them with Vader and overpower the 2 jedi instead of
    spreading to bait Rey." V67bn fires whenever there's exactly
    ONE friendly char at the destination AND the opponent's power
    exceeds ours by 4+ (same deficit threshold V67bj uses). Bonus
    magnitude +800 dominates V24.14B SPY +300 and V67as OPEN-FRONT
    +300, ensuring REINFORCE wins over SPREAD when an ally needs
    help. V29 REINFORCE (weak char, no opponent or moderate
    opponent) kept as the secondary rule for the original case.

  V67br (2026-05-11) — TURN-BASED SPREAD DISCIPLINE
    Source: CardSelectionEvaluator.java
    Steve's rule: "We should likely not spread turn 1. Turn 2 can
    cautiously spread, turn three is fully ok to spread." GROUND
    vs ABOARD-SHIP distinction (Steve, 2026-05-11): "If on
    Executor he's safe. If by himself on a site he is not." Chars
    aboard ships at a SYSTEM are protected by the ship — they
    don't need ground reinforcement. So: - Concentration site
    count: only friendlies at SITES (ground). Friendlies at
    SYSTEMS (aboard ships) don't anchor V67br. - Destination
    penalty: only applies when destination is a SITE. SYSTEM
    destinations (deploying aboard a ship) are always safe. Turn
    1: -800 to non-concentration SITE destinations. Turn 2: -300
    (cautious). Turn 3+: no penalty. First deploy of turn 1 with
    no ground friendlies: unrestricted.

  V67bt (2026-05-11) — METHOD 2 REMOVED.
    Source: CardSelectionEvaluator.java
    The old heuristic was: "if location options include both our-
    side and opponent-side sites, must be a spy deploy." That's
    wrong — any non-pilot character is offered both-sides sites in
    normal deploy decisions; it's not a spy signal at all. Method
    2 false-positively tagged General Nevar, Myn Kyneugh, and
    similar non-spies as spies. The spy scoring then applied -2000
    at Rando's concentration sites ("spy blocks our drain") and
    +300 at opp-occupied sites ("ideal spy spot"). Result: Rando
    deployed Nevar (power 3) solo at Cloud City: Lower Corridor
    across from Rey (power 7) → 22 battle damage, concede. Steve's
    rule (and memory file feedback_card_search_by_type_not_text):
    detect spies BY GAME TEXT or KEYWORD, never by heuristic.
    Methods 1 (decision text "undercover"/"spy") and Method 3
    (blueprint game text "undercover") are the correct typed
    checks. Method 2 is permanently removed. Method 3: Check
    deploying card's blueprint game text for "undercover" keyword.
    User confirmed: spy cards have "undercover" in their game
    text. deployingBlueprintId is extracted from the decision text
    HTML earlier in this method.

  V67bu (2026-05-11) — extend V67bn to ANY committed-friendly
    Source: CardSelectionEvaluator.java
    count (was solo-only). Steve's "Braveheart" rule: when chars
    are already committed to an outgunned site AND can't escape,
    pile on reinforcements to MINIMIZE overflow damage — 15+ force
    overflow is game-ending, so even losing by less wins the war
    of attrition. Escape-route check: don't reinforce if outgunned
    chars can flee. 1. Adjacent site on same planet has Rando
    friendlies (consolidate) 2. Same parent system has Rando's
    starship (shuttle aboard) If escape exists → Move evaluator
    (V67au) handles retreat next phase.

  V67e — /V67g EXPECTED FORCE LOSS — TIE-BREAKER + DRAIN-AWARE PENALTY
    Source: CardSelectionEvaluator.java
    Steve's rule: "When there is a tie for points the default
    scoring should re-look at whether the decision will make
    opponent lose more or less force. Less force drain should be
    considered a bad move." V67g STRENGTHENED: −25 zero-drain
    wasn't enough to dominate tactical bonuses — Luke + Leia moved
    Guest Quarters (drain) → Upper Plaza Corridor (no drain) then
    back. Now penalty is much stronger AND a new MOVE-FROM-DRAIN
    penalty fires when we're abandoning a draining site for a non-
    draining one.

  V67f — 1: ACTUAL passenger check. The previous V49 logic ASSUMED any
    Source: MoveEvaluator.java
    capital/transport ship has passengers, which let Wild Karrde
    land alone at sites with high enemy power → instant overflow
    death. Fix: scan game state for any character "aboard" this
    ship via the Filters.aboard filter — only "has passengers" if
    at least one is. FIXES uarc0hmiai1i594y replay: Wild Karrde
    landed at Cloud City: Upper Walkway (Steve's stack) with power
    0 → overflow.

  V67g — MOVE-FROM-DRAIN — additional penalty when this is a MOVE
    Source: CardSelectionEvaluator.java
    (not deploy) and we're leaving a draining site for a worse
    one. The decision text "Choose where to move <X>" tells us
    this is a move. V67k EXEMPTION: skip when destination is a
    transit staging site.

  V67h — When the action text is generic ("Choose card to deploy from
    Source: DeployEvaluator.java
    Reserve Deck", "[Download] a matching weapon"), use the SOURCE
    CARD's game text to identify what filter the action targets.
    This catches the failures the regex-based V66 misses — e.g.,
    Yarna's "[download] Arleil, Doallyn, Tessek, Wild Karrde, or a
    Tatooine battleground" when none of those is in Reserve.
    Steve's expectation: "Rando is already aware of what's in his
    deck at the start of game and would know when he would have a
    successful search."
    UPDATE (2026-06-28): JUNK-TARGET PASS-THROUGH (mirror V177).
    The game-text parser collapses a multi-clause OR interrupt (e.g. We
    Must Accelerate Our Plans) into ONE garbage target ("3 force to take
    one effect... podracer s..."), which is never in Reserve, so V67h
    returned WILL_FAIL and slapped -9999 on a VALID location pull.
    Forensics: Rando passed 'Deploy a Blockade Flagship site' while
    Blockade Flagship: Bridge sat in his Reserve. Fix in DeckOracle
    validatePullFromSourceCard (rando + chosenone): before WILL_FAIL, if
    ANY parsed target is junk (length > 25 or contains a digit, V177's
    criterion) return UNKNOWN, so the call sites (DeployEvaluator:789,
    ActionTextEvaluator:4318) no longer veto it. Short clean dead-search
    targets (My Sister Has It, Weather Vane) still WILL_FAIL. V177 and
    V67h now agree. See AI_CHANGELOG.md 2026-06-28.

  V67i — GLOBAL LOCATION-FIRST PRIORITY
    Source: DeployEvaluator.java
    Steve's rule: "this should be global. Deploy locations first
    so he has more options for deploying characters. He needs to
    deploy locations first then characters every turn. Especially
    if he has an effect that lets him pull locations." V24 only
    fired for "Deploy <Location>" from hand. But many decks
    pull/download locations via effects: IMBATS [download] a farm
    Yarna [download] a Tatooine battleground I'm Sorry → Cloud
    City interior site Hidden Path → Jabiim site These should ALL
    beat character deploys, because EACH new location expands
    future deploy options + force generation. Detection: parse the
    source card's game text for location keywords in the
    [download]/deploy/take target list. If any extracted target
    names a location category, this action puts a location on the
    table.

  V67k — Some sites have 0 drain by design but are STRATEGIC
    Source: CardSelectionEvaluator.java
    staging sites — penalizing them blocks key plays. Currently
    recognized: Mapuzo: Underground Corridor (Hidden Path transit
    staging — Jedi MUST go here to fire "Move Jedi Survivor here
    to a site" and flip the objective). FIXES "Rando still moving
    from higher-drain to lower-drain": V67g was blocking Safehouse
    → Underground Corridor at −432 and Rando went Safehouse →
    Spaceport Docking Bay instead, never reaching the transit hub.

  V67l — UNIVERSAL LOCATION-PULL PRIORITY (mirrors DeployEvaluator V67i)
    Source: ActionTextEvaluator.java
    Steve's rule: "If an effect lets rando pull a location from
    his deck that should be a universal positive points move. He
    should do this as the first part of his deploy phase."
    Detection: action text or source-card game text contains a
    location keyword in its target list. Bonus is +1500 —
    dominates all other scoring so Rando ALWAYS fires location
    pulls before other deploys.

  V67m — UNIVERSAL WEAPON-PULL PRIORITY
    Source: DeployEvaluator.java
    Steve's rule: "There are other cards that pull weapons from
    reserve, after location pulls and character deploys, we should
    use those effects to deploy weapons from reserve with positive
    points." Score +200 — positive enough to fire over
    passing/idle, but well below character deploy peaks (+300-500)
    so chars deploy first. Mirrors V67l's dual-source detection
    (action text + game text fallback).

  V67n — Corridor needs to OUTSCORE other Mapuzo destinations,
    Source: CardSelectionEvaluator.java
    not just be exempt from penalty. Other Mapuzo sites have Dark
    icons (drain potential), giving them V67e + ICON_BONUS (~+30)
    — Corridor with 0 score loses to them. Then Rando ping-pongs
    Mining Village ↔ Safehouse and never reaches Corridor to flip
    Hidden Path / Fallen Order. +1500 dominates V67e/ICON_BONUS on
    other Mapuzo sites and matches V67l location-pull priority.
    Only fires when destination matches "underground corridor" —
    narrowly scoped.

  V67o — BATTLEGROUND STARTING LOCATION
    Source: CardSelectionEvaluator.java
    Steve's rule: starting location should be a BATTLEGROUND so
    force drains and battles can happen there from turn 1. Without
    this rule Rando picks non-battleground sites (e.g., Dooku deck
    starts at a non-BG site) and loses tempo from turn 1.
    Detection heuristic (matches V29.6 in the deploy path): 1.
    Game text contains "battleground" 2. Title contains
    "battleground" 3. Site has BOTH Light Force AND Dark Force
    icons (most battlegrounds have both — drainable + drainable-
    against) Score: +300 for battleground, -150 for non-
    battleground. Below Funeral Pyre/Epic Event (+1000) and CC
    Exterior (+500) so those specific overrides still win; above
    Force Gen (+25), Reserve Pull (+75), and Mention-in-Interrupt
    (+50) so battleground wins when no specific override applies.

  V67p — Tentacle is not a useful starting interrupt — it's a
    Source: CardSelectionEvaluator.java
    counter to Dianoga/garbage compactor scenarios, not a turn-0
    setup card. Picking it as starting effect wastes the turn-0
    slot.

  V67q — SITH DECK SPECIFIC TIGHTENING
    Source: CardSelectionEvaluator.java
    Steve's Dooku deck uses Rise Of The Sith / Revenge Of The
    Sith. Those starting Effects only function at a NON-PALACE
    battleground. If the deck has either of those cards anywhere
    in the player's pool (hand, reserve, used/lost/force pile, in-
    play, stacked, etc.), tighten the starting-location
    preference: - Non-Palace battleground: +600 ADDITIONAL (net
    ~+900 with V67o) - Palace battleground: -350 ADDITIONAL (net
    ~-50 — discouraged) - Non-battleground: -300 ADDITIONAL (net
    ~-450) This mirrors a previous K-2 session's design (lost when
    their session ended without committing).

  V67r — At turn 0 (PLAY_STARTING_CARDS), the starting interrupt asks
    Source: CardSelectionEvaluator.java
    "Choose where to deploy <card>" — NOT "starting location".
    Without this routing, V67o/p/q never fire and Rando picks non-
    battleground sites for Sith decks (Steve's Dooku deck bug,
    2026-05-03).

  V67t — WASTE-AWARE FORFEIT SCORING:
    Source: CardSelectionEvaluator.java
    Steve's rule: "if only need to lose 2 or less force from a
    battle, keep characters on location and just lose force from
    reserves." Old formula: efficiencyBonus = fv * 20 (gave
    Sidious fv=7 a +140 bonus for satisfying 1 damage — wasted 6
    forfeit. Lost Sidious to pay 1 damage instead of losing 1
    reserve card.) New formula: net = savings*20 - waste*50
    savings = min(fv, damage_remaining) — efficient damage covered
    waste = max(0, fv - damage_remaining) — over-payment Heavy
    waste penalty (-50/pt) outweighs savings (+20/pt) so high-fv
    characters never forfeit for tiny damage.

  V67u — catch source-detected Force Push exchange even when neither outer
    Source: ActionTextEvaluator.java
    condition matched (defense in depth)

  V67v (2026-05-03) — Routing precedence bug. This branch caught
    Source: CardSelectionEvaluator.java
    turn-0 starting-location decisions BEFORE V67r could route
    them to evaluateStartingLocation. Result: all V67o/p/q/r +
    V29.14 Funeral Pyre + V24.10 CC Exterior + V67q Sith logic was
    bypassed for the starting deploy. Steve's symptom: 'Rando
    picked a Tatooine site as his Luke Saga starting location
    instead of Endor: Funeral Pyre (V29.14 should give +1000).'

  V67w (2026-05-03) — Use the engine's Icon.MAINTENANCE
    Source: DrawEvaluator.java
    instead of hand-rolled title matching. SWCCG marks every
    maintenance character with this icon on the blueprint. OLD
    code only matched "lando calrissian, scoundrel" — missed every
    other maintenance card in the deck (Lando With Vibro-Ax, Han
    With Heavy Blaster Pistol, etc.). Steve: 'on light side he is
    still not saving enough force for maintenance cards like
    lando.'

  V67x (2026-05-03) — getAllPermanentCards() returns BOTH players'
    Source: CardSelectionEvaluator.java
    cards. If the OPPONENT plays Sith (Rise/Revenge Of The Sith),
    V67q wrongly fired for ME — adding +600 to Tatooine and -300
    to Funeral Pyre, causing me to pick Tatooine as my Light-side
    starting location. Filter to only my cards.

  V67z (2026-05-03) — EXEMPT Hidden Path split-sites.
    Source: CardSelectionEvaluator.java
    V41 was hard-blocking Jabiim destinations because opponents
    were at Coruscant. But on Hidden Path, the SMART play is to
    move ONE Jedi to a non-Mapuzo battleground (Jabiim) for the
    objective flip, even if opponents are elsewhere. Symptom:
    Rando moved both Jedi to the SAME Jabiim site instead of
    splitting because V41 -9999 swamped V62 SPLIT SITE +200.

    UPDATED 2026-06-18 (Steve, replay aj816vuaxukwoie2): the exemption only
    covered step 2 of the transit — moving to a non-Mapuzo BATTLEGROUND. But the
    transit is two steps: step 1 Mapuzo: Safehouse -> Mapuzo: Underground Corridor
    (a Mapuzo INTERIOR site, no BG icon), step 2 Corridor -> off-Mapuzo. On the
    step-1 Corridor move, V41 WRONG DIRECTION (-9999, "opponents draining at Hoth,
    go there!") still buried V67n's +1500 -> net -8481 -> Rando PASSED 3 turns and
    the crippled Jedi survivors (Fallen Order: power 3, forfeit 3, game text
    canceled at Mapuzo) rotted at Safehouse and got slaughtered (6-vs-14, 9-vs-14),
    objective never flipped. Fix: also exempt V41 when the destination is the
    Underground Corridor transit hub (v67zTransitHub, title contains "underground
    corridor"). Same V41-exemption mechanism V169 RETREAT already uses (proven to
    lift the Corridor to +2117 in the same replay's turn 4). Workflow wqcmfqduq
    independently confirmed root cause + this exact fix. Correct-by-construction;
    the V41-blocks-Corridor condition is a human-opponent dynamic (opponent
    draining elsewhere while Jedi stuck) that bot self-play didn't reproduce.
    (No new V-tag: UPDATES V67z per Steve's "adjust the old rule" rule.)

    UPDATED 2026-06-18 (step 2 — transit funding): getting the Jedi to the Corridor
    isn't enough — the off-Mapuzo move ("Move Jedi Survivor here to a site",
    MoveUsingLocationTextAction forFree=false baseCost 1) costs 1 Force PER Jedi in
    the MOVE phase. No reservation existed (Steve thought it did — it didn't), so
    Rando could spend all Force on deploy/activate and strand the Jedi crippled at
    the Corridor. Added to DrawEvaluator.calculateForceToReserve: while Hidden Path
    unflipped, reserve +1 Force per friendly character at Mapuzo: Underground
    Corridor, ON TOP of the defensive-reserve cap (mandatory transit funding, not a
    luxury). Verified no regression (V58 RESERVE still computes normally, 0
    exceptions); gated tightly so non-Hidden-Path decks are untouched.

    UPDATED 2026-06 (step 3 — DEPLOY-PHASE twin): the DrawEvaluator reserve (step 2)
    only keeps the Force in the pile across the draw; the DEPLOY phase runs BEFORE the
    MOVE phase and was still spending it all on Jedi Survivor / Jabiim deploys, so the
    move phase had ~1 Force and the transit never fired — Rando sat on Mapuzo, the
    objective never flipped, Jedi stayed crippled (HIDDEN PATH CHARGE replay, Steve).
    Added the deploy-phase reserve in DeployEvaluator (rando + chosenone), mirroring
    the V48 Vader / V79 Verge move-reserve pattern: on Hidden Path unflipped, count
    Jedi at Underground Corridor, reserve min(count, 3); any force-costing deploy that
    would drop Force below that reserve is penalized -1500 (not -500 like V48/V79 —
    Jedi Survivor deploys score ~950, so -500 wouldn't stop them; -1500 drops them
    below Pass so Rando actually holds the Force). Free [download] Jabiim locations
    (cost 0) are untouched. Verified self-play (HIDDEN PATH CHARGE vs DARK DEAL): the
    reserve fired 44x, force-eating deploys hit -8424 (blocked), transit moves happened
    (404), and the objective FLIPPED (1678 flipped=true log-states vs ZERO in the
    broken game). No exceptions; parity-verified; Rando won game 2.


  ════ V70 family ════

  V70 (2026-05-12) — ONE-WEAPON-PER-CHARACTER rule in
    Source: CardSelectionEvaluator.java
    evaluateUnknown path. The Dooku replay showed Asajj Ventress'
    Lightsabers landing on already-armed Lord Sidious via Evil Is
    Everywhere's reserve-deck pull. The decision text "Choose card
    to deploy from Reserve Deck" doesn't match any specific
    dispatch pattern, so the decision flows here. V70 in
    evaluateReserveDeckSelection never fires for cardIds-populated
    decisions. This block fixes that path. Per Steve's standing
    rule: "No character should ever have two weapons." Same helper
    as in evaluateReserveDeckSelection — see
    v70CheckWeaponDeviceBlock. Comprehensive criteria search
    across title, lore, gametext, dynamic card types (engine-
    aware), icons, keywords, persona.


  ════ V71 family ════

  V71 (2026-05-15) — For LOCATIONS, the base getGameText() often
    Source: CardSelectionEvaluator.java
    returns empty. The actual game text lives in
    getLocationLightSideGameText() and
    getLocationDarkSideGameText(). Concat ALL three so keyword
    checks (reserve / epic / force gen / battleground) work for
    locations regardless of which side stores the text. This fixes
    the Ajan Kloss bug: its "Epic Event" mention is in the Light
    Side text, which V29.14 was missing.


  ════ V72 family ════

  V72 (2026-05-15) — WEAPON REDISTRIBUTION.
    Source: ActionTextEvaluator.java
    If the source character has 2+ weapons attached AND there's an
    unarmed friendly at the same site, transferring redistributes
    weapons across the team. Massively preferred over swap-from-
    hand because it directly fixes the "one char has 2
    lightsabers, others have none" pattern.


  ════ V73 family ════

  V73 (2026-05-15) — MULTI-DRAIN SHUTTLE PATTERN
    Source: MoveEvaluator.java
    Documented Cantina ↔ Mos Eisley shuttle: deploy chars at one,
    move ONE to the other during Control phase via Mos Eisley's
    free-move game text, drain at BOTH sites, move back. Net: +1
    extra drain/turn from Tatooine. V29.13 alone would penalize
    the move from Cantina(drain 2-3) → Mos Eisley(drain 1) as "bad
    drain site", killing the shuttle. V73 detects the shuttle
    pattern by title and overrides with a +400 bonus that beats
    V29.13's penalty. Generalizes: same logic applies to ANY two
    Rando-controlled sites where the destination has its own drain
    value > 0 AND Rando still has chars at the source (preserving
    the source drain).


  ════ V74 family ════

  V74 — Maintenance Cost Satisfaction (replaces V22.3)
    Source: ActionTextEvaluator.java
    When a maintenance card's upkeep is due, Rando gets a choice:
    "Use X Force" (pay — KEEP the card) "Lose X Force ... Used
    Pile" (recyclable — keep blueprint, lose card from table)
    "Place out of play" (PERMANENT loss — worst option) V22.3's
    old check applied to the ACTION text, which is short ("Use 1
    Force" / "Place out of play") and never contains "maintenance"
    — so V22.3 never fired. Replay May 15 showed Rando picking
    "Place out of play" for Lando every turn (4 times). V74 fix:
    detect maintenance context from the DECISION text (which DOES
    contain "maintenance"), then score each action's OWN text
    accordingly.


  ════ V75 family ════

  V75 (2026-05-15) — KILL-BOX CHECK before applying V67br penalty.
    Source: CardSelectionEvaluator.java
    If the concentration site is overwhelmed (opp power > our
    power + 4), it's a sacrifice site — let Rando spread to fresh
    sites instead. Replay May 15: Lars Farm became a kill box vs
    Vader + Mara + Death Trooper, but V67br kept pushing Rando to
    deploy more chars there.


  ════ V76 family ════

  V76 (2026-05-15) — BATTLE PREDICTION GATE
    Source: BattleEvaluator.java
    Use the Monte Carlo BattlePredictor BEFORE the power-tier
    scoring. If the simulation projects bad outcomes: - winRate <
    35% → hard block (probable defeat) - avgDamageTaken >= 10 →
    hard block (even if winning, too costly) Otherwise, fall
    through to the V22.4/V29.7 power scoring. Replay May 15: Rando
    initiated battle at Lars Farm and took 13 attrition + 23
    battle damage. Raw power comparison passed CRUSH/FAVORABLE;
    destiny variance crushed him. BattlePredictor exists with 311
    lines of simulation logic but was never wired into
    BattleEvaluator. This wiring closes the gap.


  ════ V78 family ════

  V78 (2026-05-15) — Imperial Arrest Order & Secret Plans
    Source: DrawEvaluator.java
    forces opponent to "use 1 Force OR retrieval canceled" every
    retrieval attempt. Reserve +2 to absorb the tax and keep
    retrieval interrupts viable.


  ════ V79 family ════

  V79 (2026-05-15) — VERGE OF GREATNESS — MOVE DEATH STAR TOWARD SCARIF
    Source: MoveEvaluator.java
    Rando-as-Krennic must shepherd the Death Star from parsec 4 to
    orbit Scarif. Death Star (V) starts at parsec 4 with
    hyperspeed 2. Scarif is at parsec 7. Turn 1: parsec 4 → 6
    (closer to Scarif) Turn 2: parsec 6 → 7; engine then offers
    "orbit Scarif" option. Title check is just "death star" — the
    (V) is a Rarity.V marker, NOT in the title string (Death Star
    and Death Star (V) share Title.Death_Star). Since Verge of
    Greatness only enables the Set 16 Death Star, the title match
    is sufficient to identify the Krennic deck's Death Star.


  ════ V80 family ════

  V80 (2026-05-15) — SKYWALKER EPIC EVENT REQUIRED EFFECTS.
    Source: CardSelectionEvaluator.java
    The Rise Of Skywalker deploys "two Effects that deploy for
    free and are always immune to Alter." A Cunning Warrior and A
    Good Friend are the must-picks here — they require the
    Skywalker Epic Event on table to deploy, so they're meant for
    this slot. Cunning Warrior: "Where you have a Skywalker, you
    initiate battles for free" + Anakin's Lightsaber pull Good
    Friend: built-in weapon redistribution (relocate Anakin's
    Lightsaber) + Ben Solo recovery + multi-card pull Detection by
    title (these are unique starting effects).


  ════ V82 family ════

  V82 (2026-05-16) — EXPLICIT SOURCE-CARD SITE-PULL TRIGGER
    Source: ActionTextEvaluator.java
    Catches the case where the action text is GENERIC ("Deploy
    card from Reserve Deck") but the SOURCE CARD'S game text
    describes a site / location / battleground pull. This is what
    V67l's fallback was meant to do via
    DeckOracle.parseSourceCardPullTargets — but in the Invasion
    game (replay jdyn9tx3peavh6gd, 2026-05-16), action text was
    "Deploy card from Reserve Deck" with no Naboo/site keyword,
    the parser pipeline produced no firing, and Rando never used
    Invasion's once-per-deploy-phase Naboo-site pull. V82 reads
    the source card blueprint directly and pattern-matches
    "(site|location|battleground) [...] from reserve". When it
    matches, score +2500 — dominates V60+V67l and any competing
    deploy action. Per Steve: "If an effect lets rando pull a
    location from his deck that should be a universal positive
    points move."

  V82.1 (2026-05-16) — Drop the "deploy" anchor entirely.
    Source: DeckOracle.java
    Old "\\bdeploy\\s+(...)\\s+from\\s+reserve\\s+deck" matched
    the FIRST "deploy" — in Invasion ("once during your deploy
    phase may deploy a Naboo site from Reserve Deck") it captured
    "phase may deploy a naboo site" as target. V67h then hard-
    blocked with -9999. Per Steve: "Just do 'from reserve deck'
    and don't search for the text 'deploy' at all. This will cover
    any deploy from reserve." New approach: capture EVERYTHING in
    the same clause before "from Reserve Deck", then aggressively
    strip leading verb/article noise in the normalization loop.
    Works for any phrasing — "may deploy X", "take X into hand",
    "search for X", "[download] X", etc.

  V82.2 — Decide WILL_FAIL vs UNKNOWN.
    Source: DeckOracle.java
    - If ANY target had a recognized type-word (category OR
    predicate), we DID validate authoritatively → WILL_FAIL is
    safe. - If NO target had any recognized type-word AND
    substring also failed, target is fully proper-noun → WILL_FAIL
    is safe (substring would have matched if title were in zone).
    So WILL_FAIL is correct in both cases.

  V82.3 (2026-05-16) — strip leftover parens and
    Source: DeckOracle.java
    brackets. Begin Landing's text is "(or Coruscant) docking bay"
    — after or→comma split we get "[episode i] (" and "coruscant)
    docking bay". Paren-strip cleans both so the
    category/predicate fallback sees clean words like "coruscant
    docking bay" → can match docking_bay keyword.


  ════ V83 family ════

  V83 (2026-05-16) — MY LORD — SENATORS ONLY AT GALACTIC SENATE
    Source: DeployEvaluator.java
    Per Steve: "For the my lord objective, Rando should deploy
    senators only to the senate location. Anywhere else and they
    will get destroyed." My Lord, Is That Legal? / I Will Make It
    Legal sets weapon destiny -6 at Galactic Senate, protecting
    low-power senators from weapon fire. ANYWHERE ELSE, normal
    weapon destiny applies and senators (typically ability 2-3,
    low power) get killed. The objective also REQUIRES 2-3
    senators at Galactic Senate to stay flipped — deploying
    senators elsewhere strands the win condition. Type-by-API per
    Steve's standing rule: Filters.senator uses Keyword.SENATOR;
    Filters.Galactic_Senate uses Title match.

  V83.1 (2026-05-19) — only penalize when target is identifiable.
    Source: DeployEvaluator.java
    For generic "Deploy" actions (no location in text), the actual
    location is picked in a separate CardSelection step —
    penalizing here would block the deploy before location-
    selection runs.


  ════ V86 family ════

  V86 (2026-05-16) — INVASION — NEIMOIDIAN PILOTS ABOARD CAPITAL SHIP
    Source: DeployEvaluator.java
    Per Steve: "For Invasion objective deck, if a character is
    Neimoidian in lore and is a pilot they should deploy only to a
    capital ship if capital ship is on the table. If capital ship
    is not on table they can deploy anywhere else." Blockade
    Flagship (the deck's signature capital ship) gives permanent
    pilot ability +2 and once-per-game pulls a Neimoidian pilot
    aboard. Neimoidian pilots deployed to ground sites are wasted
    — they belong on the ship piloting it. Type-by-API per Steve's
    standing rule: Species.NEIMOIDIAN + Icon.PILOT detected via
    Filters, capital ship via CardSubtype.

  V86.1 (2026-05-19) — only act when action text is
    Source: DeployEvaluator.java
    SPECIFIC enough to identify the target. Generic "Deploy" gets
    a target picked in CardSelection later — don't pre-block.


  ════ V87 family ════

  V87 (2026-05-16) — HARD-BLOCK pilot/passenger capacity slot swaps
    Source: ActionTextEvaluator.java
    Replay tem28wtufcy7d08j: Sil Unch deployed aboard Blockade
    Flagship as pilot, then Rando got stuck in a 40+ iteration
    pilot↔passenger swap loop. DecisionTracker didn't catch it
    because the wrapping decision text varies ("Optional
    responses" vs "Use 2 Force - Optional responses"), breaking
    the key-match for loop detection. These capacity-slot swaps
    gain nothing for the AI — once a pilot is placed, swapping
    pilot↔passenger doesn't change combat/movement value. Hard-
    block both directions outright.


  ════ V88 family (TEXT-NAMED SITE generalization 2026-06-03; deficit-6 gate 2026-06-04) ════

  2026-06-04 V88 TEXT-NAMED SITE DEFICIT-6 GATE (Steve, Jabba-walks-into-Luke):
    Source: ai/models/rando/evaluators/CardSelectionEvaluator.java
    (inside the V88 TEXT-NAMED SITE positive branch added 2026-06-03,
    ~line 1497).
    Yesterday's V88 generalization added a +500 home-site bonus when a
    character's blueprint game text mentions the candidate site name.
    Steve's next replay: Jabba The Hutt deployed to Jabba's Palace:
    Audience Chamber even though Steve had 23 power there (us 10, gap 13)
    armed with weapons. Score breakdown line 15362:
      +200 PLANNED TARGET
      +50  V29.6 BG
      +150 OBJECTIVE LOCATION
      +500 V88 TEXT-NAMED SITE
      -220 V136 (S A -500 + S B +300 - S C 20)
      +30  V23 force-drain icon
      +5   V29.7 ability
      +200 V22.7 objective-critical
      +40  V29.5 home-field
      +80  V29.7 battleground
      = 1085 — top among Jabba's deploy candidates.
    The +500 home-site bonus was strong enough to drag Jabba into a fight
    he couldn't win. Same problem class my V67bn deficit-cap edit fixed:
    a strong-positive bonus with no contested-fight guard sends Rando
    into doomed sites.
    Edit: inside the existing V88 positive branch, before action
    .addReasoning(+500), compute (oppPower - ourPower) at the candidate
    site via game.getModifiersQuerying().getTotalPowerAtLocation. If
    gap >= 6, log "V88 TEXT-NAMED SITE SKIP" and skip the +500.
    The negative -500 branch (character text says "not at X" / "may not
    deploy at X" / "cannot deploy at X") is left untouched — that
    penalty applies regardless of contestation since the character
    fundamentally can't deploy there. The senator/Galactic Senate
    hardcoded block above is also untouched.
    Threshold 6 mirrors V67bn cap of 5 with a 1-point hysteresis buffer:
    V67bn fires at deficit ≤5 (will reinforce), V88 silences at deficit
    ≥6 (won't even bother trying for the home-site bonus).
    Council vote (engineer, rules_lawyer, voice_of_reason): split.
    Engineer called it an EDIT (consistent with V67bn precedent the
    council unanimously called EDIT 2026-06-03). Rules_lawyer and
    voice_of_reason called it "new conditional logic = new rule."
    Steve approved "Yes ship" directly; documenting the split here.

  2026-06-04 V33 BUDDY BREAK DEFICIT-6 GATE (Steve, same replay):
    Source: ai/models/rando/evaluators/MoveEvaluator.java ~line 638
    Same replay context. Jabba was at Audience Chamber with us 10 vs
    opp 23. The retreat-via-landspeed move scored:
      -60  V29 FORCE RESERVE low force
      +150 Strategic retreat - badly outmatched
      -150 V33 BUDDY BREAK (moving Jabba drops ability 9 -> 5)
      +10  Land (ground deployment)
      = -50 total
    Pass scored +24. Move would have lost to pass. V33 was trapping
    Jabba at a doomed site to preserve ability ≥7 — but the site is
    already lost, V33 was preserving a buddy-threshold that no longer
    mattered.
    Edit: extend the existing 4-condition gate with a 5th condition.
    Compute (oppPower - ourPower) at currentLocation via the same
    game.getModifiersQuerying().getTotalPowerAtLocation call already
    used elsewhere in MoveEvaluator (e.g. V32 line 580, V137 line 964).
    If gap >= 6, set v33SiteDoomed = true and the &&-chained gate
    fails — V33 doesn't fire and the retreat move keeps its +150.
    For triage, the else-branch logs "V33 BUDDY BREAK SKIP" when
    skipped.
    Same threshold 6 as the V88 gate above for consistency. Same
    V67bn precedent for the threshold choice.
    Net effect on the replay: Jabba's retreat move would have scored
    +100 instead of -50, beating pass (+24) and actually moving him
    out of the doomed site.

  2026-06-03 V88 TEXT-NAMED SITE BONUS (Steve, Jabba's Haven replay):
    Source: ai/models/rando/evaluators/CardSelectionEvaluator.java
    (immediately after the V88 MY LORD senator block at ~line 1445)
    Steve: "Jabba not deployed to Jabba's Palace???" Replay: V136 §A
    returned +500 for every objective-relevant battleground and V22 +150
    for every objective-relevant location, so all four Jabba's-deck deploy
    candidates tied at +1225:
        Tatooine: Desert Heart            1225
        Tatooine: Jabba's Palace          1225
        Jabba's Palace: Lower Passages    1225
        Jabba's Palace: Audience Chamber  1225
    Sub-decision picker took the first (Desert Heart), Jabba never went
    home. Jabba The Hutt (200_84) game text:
      "While at Audience Chamber, may [download] Scum And Villainy and
       immune to attrition < 4."
    The site name is right there in the character's text — no existing
    rule reads it. V88 SENATOR + GALACTIC SENATE +1500/-2000 hardcoded
    block (V88 (2026-05-18) entry below) is the closest pattern: per-
    character per-site bonus. Steve's directive (verified by council):
    "I want to think of a way to make a general rule to give bonues when
    a charactor metnions a specific site. So Jabba's game text says
    Jabba's Palace. If Jabba's palice is on table he gets a bonus to
    deploy there."
    Council vote (engineer, rules_lawyer, voice_of_reason on /vote
    endpoint): unanimous "this is an EDIT of V88, not a new rule".
    engineer + rules_lawyer say +500 magnitude is correct; voice_of_
    reason suggested higher but no concrete number, so +500 stands and
    we tune if practice proves it too soft.
    Universal detection (no card-name hardcoding):
      1. Resolve the deploying character's blueprint (already in scope
         as deployingBlueprintId).
      2. Build a lowercased string from getGameText() + " " + getLore().
      3. For the candidate site title, strip the parent prefix
         (everything up to and including ":") to get the BARE SITE NAME
         (e.g. "jabba's palace: audience chamber" → "audience chamber").
      4. If bare site name length >= 5 AND character text contains the
         bare site name → match.
      5. Negative-phrase filter: if the match overlaps with "not at X",
         "may not deploy at X", "cannot deploy at X", or "not at <full
         title>" → flip sign to -500 (Steve wanted symmetric handling
         since some characters explicitly say "may not deploy at X").
    Magnitude: +500 / -500. Tie-breaker scale: above the typical V22
    objective-relevant tie spread (0–150), below V59 SPY UNIVERSAL
    (-2000) and CharacterDeploySiteEvaluator's V136 §A SPY-BLOCKED
    (-1000) so safety rules still dominate when the site is dangerous.
    Logs prefixed "V88 TEXT-NAMED SITE" (positive) and "V88 TEXT-NAMED
    NEG" (negative) for triage.
    Edits the V88 rule family in place. No new V-tag. Hardcoded senator
    + Galactic-Senate block (V88 MY LORD, V99 NON-SENATOR-AT-SENATE)
    left untouched and continues to fire on the My Lord objective at
    its original +1500 / -2000 magnitudes.

  V88 (2026-05-18) — MY LORD — SENATOR → GALACTIC SENATE BONUS
    Source: DeployEvaluator.java
    Companion to V83 (which penalizes senators going elsewhere).
    Replay 3iogq7426gpetbny: Rando played Senate deck (My Lord
    objective), pulled Orn Free Taa and Edcel Bar Gane via
    Squabbling Delegates — but NEVER deployed them. Senators are
    low-power and get blocked by solo-deploy/buddy-system
    penalties (V47 etc.). At Galactic Senate the objective
    provides weapon destiny -6, so senators are SAFE there. This
    rule gives a strong positive bonus to senators going to
    Galactic Senate so they OVERRIDE the solo-deploy penalties
    that keep them in hand. Required to satisfy the flip condition
    (3 senators at Senate).


  ════ V89 family ════

  V89 (2026-05-18) — DR. EVAZAN — NEEDS ARMED PARTNER
    Source: DeployEvaluator.java
    Per Steve: "Dr. Evazan should be deployed with another
    character with a weapon. Should never deploy alone. This can
    be with a character that has a permanent weapon on them or a
    weapon card deployed on them." Dr. Evazan has low
    forfeit/power. Without an armed friend at the same site, he
    gets sniped. Catches both the solo "Dr. Evazan" and paired
    "Dr. Evazan & Ponda Baba" cards via title-prefix check.
    Filters.character_with_a_weapon covers BOTH deployed weapon
    cards AND permanent weapons (armedWith() includes permanents
    per its javadoc).


  ════ V90 family ════

  V90 (2026-05-19) — NO SOLO DEPLOY TO SITE WITH ENEMY WEAPON
    Source: DeployEvaluator.java
    Per Steve: replay d483o8y8rjen117p — Captain Phasma deployed
    to Starkiller Base: Shield Control four times in a row, each
    time sniped on weapons segment by Leia's Lightsaber. Once a
    character is hit, their ability counts as 0 → can't draw
    battle destiny → guaranteed losing battle. Rule: if the target
    location has an enemy character with a weapon AND we have NO
    friendly character with a weapon already there, hard-penalize
    this deploy. Forces Rando to either bring an armed buddy,
    deploy elsewhere, or wait. Filters.character_with_a_weapon
    covers deployed weapon cards AND permanent weapons
    (armedWith() includes both).


  ════ V91 family ════

  V91 (2026-05-19) — ESCAPE LANDED-SHIP TRAP
    Source: MoveEvaluator.java
    Per Steve: replay d483o8y8rjen117p — Rando deployed Kylo Ren's
    Command Shuttle to "Jakku: Niima Marketplace" (a SITE, not the
    Jakku system), then deployed Kylo aboard as pilot. Ship at a
    site is "landed" → contributes 0 power. Rando's move phase did
    NOT disembark Kylo or take off to system. Asdf clobbered the
    power-0 ship next turn. Rule: when our character is aboard a
    starship at a NON-SYSTEM location (i.e., landed at a site),
    score "take off" / "disembark" moves with a strong bonus so
    Rando escapes the trap. Either: - Take off → ship moves to
    related system, gets its power back - Disembark → pilot stays
    at site, uses ground combat Detected by action text patterns.
    SWCCG move actions for landed ships use phrases like "Take
    off", "Disembark", "Move to system".


  ════ V95 family ════

  V95 (2026-05-20) — SAVE DEAD INTERRUPTS WHEN RESERVES >= 15
    Source: ActionTextEvaluator.java
    Per Steve: "If Rando has an interrupt in hand and reserves are
    15 or above, and the card listed on the interrupt is already
    on table, Rando should save that card in his hand. Those
    interrupt cards are useful to lose when force loss is needed
    because they are basically dead cards already." When the
    interrupt's source card category is INTERRUPT, parse its
    pull/upload targets via DeckOracle.parseSourceCardPullTargets,
    check if every target is already on the table, and if reserve
    force (force pile + used pile + reserve deck card counts) is
    >= 15, apply -2000 to discourage playing the interrupt. Card
    stays in hand as future force-loss fodder. Example: My Sister
    Has It uploads Chief Chirpa's Hut OR Guest Quarters; with both
    in play, the upload is moot. Parser was extended in V95 to also
    recognize [upload] syntax (previously only [download] and "from
    reserve deck" patterns).


  ════ V96 family ════

  V96 (2026-05-20) — CONCENTRATE AT CONTESTED SITES
    Source: DeployEvaluator.java
    Per Steve: "I basically bombard characters. When it comes to
    places where a battle is likely going to happen, IE I have guys
    at the same location Rando has or is going to deploy, those are
    great places to put maximum amount of characters/and or
    vehicles to overpower me. Overpowering me in a battle is a very
    quick way to win because that causes overflow damage." V67al
    currently penalizes ALL deploys when friendly power at a site
    exceeds 20, regardless of opponent power. For character deploys
    where the target location has opponent presence and the
    friendly-vs-opponent power diff is within +/-10 (close
    battle), give +500 to deploy here — overrides V67al's spread
    penalty and pushes the bot to pile on for overflow battle
    damage. If already winning by more than 10, give +100 (finish
    them). Uncontested sites (opponentPower == 0) left to V67al's
    spread penalty.

    NOTE (2026-06-24, bytecode-verified): the V67al/V67aj "spread penalty"
    referenced above is DEAD CODE (inside `if (false /* SUPERSEDED V136 */)`
    in DeployEvaluator.java, absent from the live web.jar), so V96's +500
    stands alone and overrides nothing. The real uncontested over-stack /
    spread penalty now lives in V136 §B (CharacterDeploySiteEvaluator.
    evaluateSite). A 2026-06-24 "contested-site gate" edit to V67aj/V67al
    was REVERTED: it edited the dead block and could never run. See
    resources/BUILD_AND_DEPLOY.md §1.


  ════ V97 family ════

  V97 (2026-05-20) — PULL FROM RESERVE BEFORE ACTIVATING FORCE
    Source: ActionTextEvaluator.java
    Per Steve: "Before activating turn 1 if Bow To The First Order
    effect is on table use it to pull Finalizer. If Effect, Epic
    Event, Interrupt, or Objective lets us pull a card before
    activating, we should do so. This gives us a better chance of
    pulling the card we want without it getting put into the Force
    Pile which would be unacceptable for the pull." During Activate
    Phase, when the source card is an Effect, Epic Event,
    Interrupt, or Objective, and the action text indicates a
    Reserve Deck pull ("from reserve deck", "[upload]", "[download]",
    or "take... into hand"), score +1500. This dominates the
    default "activate force" action so pulls fire first. Excludes
    Knowledge And Defense (pulls from stacked cards, not Reserve
    Deck — ShieldStrategy already handles that source's logic).


  ════ V98 family ════

  V98 (2026-05-20) — INFRASTRUCTURE: stop swallowing /hall exceptions
    Source: HallRequestHandler.java
    The /hall catch block was discarding any exception silently.
    Now logs class + message + full stack to stderr (nohup.out).
    This is what surfaced the lockedDeckType DB schema bug.

  V98b (2026-05-20) — INFRASTRUCTURE: restore ImageProxyRequestHandler
    Source: ImageProxyRequestHandler.java + RootUriRequestHandler.java + newgui.html
    The CDN at res.starwarsccg.org blocks cross-origin fetch with
    Origin headers. The Unity client uses fetch() under the hood,
    so card images returned 503. The interceptor in newgui.html
    rewrites direct CDN URLs to /gemp-swccg/imageproxy. The proxy
    handler fetches server-to-server (no Origin) and returns the
    image with Access-Control-Allow-Origin: *. Both pieces were
    lost in an upstream master rebase; this restores them.


  ════ V99 family ════

  V99 (2026-05-20) — NON-SENATOR AT GALACTIC SENATE BLOCK
    Source: DeployEvaluator.java
    Inverse of V83 (senators-only-at-Senate) and V88 (senator →
    Senate +1500). Non-senator characters deploying to Coruscant:
    Galactic Senate get -1500 unless opponent power at Senate
    already exceeds friendly senator power (defensive reinforcement
    permitted). Real incident 2026-05-20 replay 15y8vh3qfc5vm8i8:
    Rando deployed Maarek Stele + Admiral Ozzel to Senate turn 1
    with zero opponent threat. Senate is for senators (weapon
    destiny -6 protection only applies to them); non-senators
    belong at sites where they actually matter.


  ════ V100 family ════

  V100 (2026-05-20) — PULL/DEPLOY LOCATIONS BEFORE CHARACTER DEPLOYS
    Source: ActionTextEvaluator.java
    During DEPLOY phase, scan the source card's GameText for
    Reserve-Deck location-pull or location-deploy patterns (e.g.,
    "deploy ... docking bay from Reserve", "take ... location into
    hand from Reserve", "deploy a ... system|site|sector from
    Reserve"). When such a source is firable AND we still have any
    CHARACTER or VEHICLE card in hand to deploy, +1500 boost so it
    fires first. Distinct from V97 (generic Reserve pulls during
    ACTIVATE phase): V100 is location-specific and fires during
    DEPLOY phase. Real incident 2026-05-20 replay
    kvwurqrwgv6wsliq: Rando deployed Begin Landing Your Troops &
    Fighters Straight Ahead AFTER deploying his characters,
    wasting the Effect's docking-bay pull because no character
    needed it. EXCLUDES Knowledge And Defense (stacked-card pull).


  ════ V101 family ════

  V101 (2026-05-20) — FORCE LOSS SOURCE PRIORITY: Used → Reserve → Hand
    Source: CardSelectionEvaluator.java
    When the AI must pick which card to lose for Force loss, prefer
    Used Pile (+500) > Reserve Deck (+300) > Hand (-500). Hand
    cards are the most valuable; losing them strips our ability to
    play next turn. Used Pile is already-cycled — its cards are
    fungible as Force fodder. Reserve Deck cards are accessible
    but losing them shrinks the remaining draw pool. Real incident
    2026-05-20 replay kvwurqrwgv6wsliq: Rando lost 4 critical hand
    cards (Ap'lek, Rise Of The Sith, We Must Accelerate Our Plans,
    Force Push V) consecutively while Used Pile had 2 cards and
    Reserve had 40 — entered next turn with a 2-card hand and no
    plays available.


  ════ V102 family ════

  V102 (2026-05-20) — K&D ACTIVATION CAP BY ACTIVATION COUNT
    Source: ShieldStrategy.java, ActionTextEvaluator.java, RandoCalAi.java
    Pre-V102 the K&D shield-pacing logic counted only
    CardCategory.DEFENSIVE_SHIELD deploys via the trackOwnShields
    SIDE_OF_TABLE scan. K&D's "Play a card" top-level action
    pulled OTHER categories (Effects, Interrupts, Objectives) from
    its stacked face-down pile, none of which triggered the
    counter — so the pacing cap never bit and Rando could fire K&D
    unlimited times per turn. V102 adds knDActivationsThisTurn /
    recordKnDActivation / knDActivationsThisTurn(turnNumber) /
    atKnDActivationCap(turnNumber) to ShieldStrategy. Counter
    increments on COMMIT (RandoCalAi.trackKnDActivations scans for
    new "Play a card" activations from K&D source cards). Beyond
    the cap, ActionTextEvaluator scores -2000 (hard block, was -40).
    Caps reuse existing SHIELD_PACING: turn 1 = 2, turn 2 = 3,
    turn 3+ = 4. Real incident 2026-05-20 replay
    kvwurqrwgv6wsliq: Rando fired K&D 4 times on turn 1 (cap is 2),
    burning the Effect/Interrupt stack on suboptimal picks.


  ════ V103 family ════

  V103 (2026-05-20) — PARSEC MULTIPLE_CHOICE VERGE DETECTION FIX
    Source: ActionTextEvaluator.java
    V79's parsec-distance scoring was correct, but during the
    "Choose parsec to move to" MULTIPLE_CHOICE evaluation,
    v79Verge returned false. The evaluator produced zero actions
    and the engine fell back to selecting option 0 (parsec 2,
    wrong direction). DeployEvaluator's V79 detection used
    pZone.isInPlay() as a guard; the ActionTextEvaluator version
    did not. V103 adds the isInPlay guard, loosens the owner check
    to also match without the ~prefix in case of playerId
    formatting drift, adds instrumentation logs (v79Verge,
    v79AtScarif, permanent card count), and a parsec-distance
    fallback so even if Verge detection fails entirely the AI
    scores by raw distance to Scarif rather than producing no
    actions. Real incident 2026-05-20 replay wngf49r9ot6315rz:
    Rando moved Death Star 4 → 2 → 0 instead of 4 → 6 → 7.


  ════ V104 family ════

  V104 (2026-05-20) — BLOCK DRAIN ≤1 UNDER BATTLE ORDER RULES
    Source: ActionTextEvaluator.java
    V52 said "after turn 3, always drain even under Battle Order /
    Plan rules — any damage is better than zero." Steve's
    correction: when the +3 cost trigger is active AND drain value
    is ≤ 1, the math is net -2 force per drain — strictly worse
    than passing. V104 hard-blocks (-2000) drains where ALL of:
    (1) under Battle Order or Battle Plan rules (the strategy
    controller's isUnderBattleOrderRules flag), (2) the drain at
    the target site is ≤ 1 Force, (3) we don't occupy
    system+site (so we're the ones paying +3). V52 still applies
    for drains ≥ 2 (net -1, marginal but worth it).


  ════ V105/V106/V107 family ════ (4th defensive shield content selection)

  V105 (2026-05-20) — 4TH SHIELD: BATTLE ORDER / BATTLE PLAN
    Source: ShieldStrategy.prefers4thSlot()
    Trigger A. When Rando friendly-occupies a SYSTEM battleground
    AND a SITE battleground, return "Battle Order" (Dark) or
    "Battle Plan" (Light) as the preferred 4th-shield content.
    Boost matching shield deploys / K&D-stack picks by +2000.
    Rationale: Battle Order/Plan makes Force drains cost +3 unless
    the drainer occupies system+site. When Rando is on both, his
    drains are unaffected; opponent pays the +3 penalty. One-sided
    hit. If Rando is NOT on system+site, deploying B.O./B.P.
    punishes Rando equally — do not deploy.

  V106 (2026-05-20) — 4TH SHIELD: COME HERE YOU BIG COWARD / SIMPLE TRICKS
    Source: ShieldStrategy.prefers4thSlot()
    Trigger B. When opponent has Force-drain-bonus sources on
    table (lightsabers, drain-bonus objectives detected via
    GameText scan for "force drain" + bonus modifier) AND opponent
    occupies fewer than 2 battlegrounds AND Rando occupies at
    least one battleground, return "Come Here You Big Coward"
    (Dark) or "Simple Tricks And Nonsense" (Light). Verified card
    text (Card13_061 / Card200_028): "cancel opponent's Force
    drains at non-battleground locations and Force retrieval."
    Exactly counters opponent's non-battleground drain stack.

  V107 (2026-05-20) — 4TH SHIELD: RESISTANCE / ULTIMATUM
    Source: ShieldStrategy.prefers4thSlot()
    Trigger C. When opponent can Force drain for 3+ at any site
    (counted via drain-bonus stack: lightsabers + drain-bonus
    objectives + character drain bonuses) AND the prerequisite is
    met (Rando occupies ≥3 battlegrounds OR opponent occupies 0
    battlegrounds — otherwise the shield is dead text), return
    "Resistance" (Dark) or "Ultimatum" (Light). Verified card text
    (Card6_147 / Card13_084 / Card6_058 / Card13_044): "you lose
    no more than 2 Force from each Force drain or 'insert' card."
    Caps incoming drain damage at 2 regardless of how many bonuses
    opponent stacks. Priority order at 4th-shield decision:
    A (V105) > C (V107) > B (V106). Rationale: A is the most
    powerful sustained advantage; C is the hardest damage cap; B
    is the most conditional.


  ════ V108 family ════

  V108 (2026-05-20) — MY LORD: PRIORITIZE DEPLOYING SENATORS FROM HAND
    Source: DeployEvaluator.java
    Real incident replay h3iqvadi8rxzha23: Edcel Bar Gane (senator,
    in hand all game) was never deployed because Rando preferred to
    deploy Maarek Stele / Admiral Ozzel / Darth Tyranus / Lord
    Sidious instead. V88's senator → Senate +1500 fires only when
    Rando CHOOSES to deploy a senator — it doesn't bias the queue.
    V108 fills the gap: when MLITL / Make It Legal objective is
    active AND the deploying CHARACTER card is a senator (by
    Keyword.SENATOR OR "senator" in lore), the deploy action gets
    +500. Senators get drafted to the front of the deploy queue.
    Deck verification confirmed: deck contains Aks Moe x2, Baskol
    Yeesrim, Edcel Bar Gane x2, Lott Dod x3, Tikkes, Toonbuck Toora
    x4, Orn Free Taa — 13 senator slots, only Orn Free Taa was
    being deployed pre-V108.


  ════ V109 family ════

  V109 (2026-05-20) — MY LORD: PROTECT SENATORS FROM LOSS/COST/FORFEIT
    Source: CardSelectionEvaluator.java
    Per Steve: "Let's never put a senator in used pile or lost pile
    for this deck if we can avoid. hard block like -300." When
    MLITL active AND the card being chosen for force-loss / cost
    payment / forfeit / discard is a senator (by lore OR keyword),
    score -300. Stops Rando from burning Aks Moe as With Thunderous
    Applause cost or losing senators from hand to battle damage when
    alternatives exist. Layered on top of V101 (Used > Reserve >
    Hand zone priority).


  ════ V110 family ════

  V110 (2026-05-20) — MY LORD: HOLD NON-SENATOR DEPLOYS UNTIL NON-SENATE SITE EXISTS
    Source: DeployEvaluator.java
    Real incident replay lcvfliepk63snbew: Admiral Ozzel kept
    deploying to Galactic Senate because no other valid site (e.g.,
    a docking bay) was on table at deploy time. V99 correctly
    penalized Senate as the LOCATION CHOICE (-1500), but the
    CardSelection step had only Senate as a candidate and the engine
    has noPass=true,min=0 — so Ozzel landed at Senate by elimination.
    V110 fix: also penalize the DEPLOY ACTION (-2000) when MLITL
    active AND card is non-senator AND no non-Senate SITE-subtype
    location is on table. Holds the non-senator in hand until a
    docking bay / interior / other site is deployed. Pairs with V99
    (location-choice block) — V110 stops the deploy from starting,
    V99 stops the destination pick.


  ════ V99 revision (2026-05-20) ════

  V99-CS (2026-05-20) — SENATE GUARD: CardSelection variant
    Source: CardSelectionEvaluator.java
    The original V99 in DeployEvaluator never fired because the
    deploy action text is generic ("Deploy"), not "Deploy to
    Galactic Senate." Mirror of V88's CardSelection pattern: when
    title contains "galactic senate" AND deploying CHARACTER is a
    non-senator AND opponent power at Senate <= friendly senator
    power, apply -1500. Senator detection uses Keyword.SENATOR OR
    "senator" in lore (feedback_senator_in_lore_not_keyword.md).


  ════ V88-LORE / V99-LORE (2026-05-20) ════

  Senator detection upgrade — lore OR keyword
    Source: CardSelectionEvaluator.java, DeployEvaluator.java
    Verified 2026-05-20: 29 of 35 senator cards add
    Keyword.SENATOR; 6+ identify senator status only in lore text.
    All V88 / V99 senator checks now match EITHER keyword OR
    "senator" substring in blueprint.getLore(). Steve's standing
    rule (feedback_senator_in_lore_not_keyword.md): "senator is a
    key word in lore not game text."


  ════ V105/V107 revision (2026-05-20) ════

  V106 dropped — 4th slot stays CLOSED INDEFINITELY
    Source: ShieldStrategy.prefers4thSlot(), CardSelectionEvaluator.java
    Per Steve: "We need to leave the 4th slot open indefinitely
    until Resistance or Battle Order conditions met." V106 (CHYBC /
    Simple Tricks) trigger was too easy to satisfy spuriously
    (any opponent lightsaber on table activated it without regard
    to whether a non-battleground drain threat existed). Removed
    V106 from prefers4thSlot — now returns Battle Order/Plan (V105)
    or Resistance/Ultimatum (V107) ONLY. When neither fires,
    returns null and the 4th-slot shield candidates score -5000
    (hard block) rather than -500.

    RE-ENABLED 2026-06-17 (Steve, Dooku replay sb2xzfjfpk5jxt8v): V106 was dropped
    for firing on "any opponent lightsaber" regardless of an actual non-BG drain.
    Now it's gated on exactly that missing condition: prefers4thSlot returns Simple
    Tricks And Nonsense (Light) / Come Here You Big Coward (Dark) only when triggerB
    (we occupy a BG, opp occupies < 2 BGs, opp has a drain bonus) AND the opponent is
    currently force-draining a NON-battleground (oppDrainsNonBg, computed in the
    existing top-locations scan) — exactly the threat those two shields cancel.
    Priority A > C > B. Replay: Rando held Simple Tricks all game while Dooku drained
    2/turn at Coruscant: The Works (non-BG) — ~10 Force handed over. Verified self-
    play (1 Rey vs 1 Dooku): V106 4TH SLOT (B) fired 189x and Simple Tricks entered
    play; Rando won both games (was losing badly before). (No new V-tag: this
    RE-ENABLES + tightens V106 per Steve's "adjust the old rule" standing rule.)


  ════ V89-CS family (2026-05-20) ════

  V89-CS (2026-05-20) — DR. EVAZAN: CardSelection variant
    Source: CardSelectionEvaluator.java
    Same architectural fix as V99-CS. Original V89 in DeployEvaluator
    only fired when actionText contained the target location title.
    Moved into CardSelectionEvaluator location-choice path: when the
    deploying card title starts with "Dr. Evazan" AND no friendly
    armed character is at the candidate location, apply -1500 on
    that target. Forces Dr. Evazan / Dr. Evazan & Ponda Baba to
    deploy with armed friends or hold in hand.


  ════ V111 family (2026-05-21) ════

  V111 (2026-05-21) — ADVANCE FROM NON-BG TO ADJACENT BG (+400)
    Source: MoveEvaluator.java (rando + chosenone)
    Real incident: Agents of Black Sun replay (asdf, 2026-05-21).
    Rando used the objective to deploy a character to Coruscant:
    Imperial City (non-battleground, DARK_FORCE 1 only) and never
    moved them to the adjacent Xizor's Palace (battleground). The
    V38.3 "wrong direction" rule hard-blocked the move because
    Xizor's Palace was unoccupied while opponents existed elsewhere
    — but moving to an empty BATTLEGROUND from a non-BG is the
    correct strategic play, not a retreat.
    V111 exception: inside the V38.3 hard-block branch, if current
    location is non-battleground AND destination is battleground,
    score +400 instead of -9999. Per Steve: "make sure that imperial
    city site gets deployed and stays next to xizor's palace. That
    way we can deploy from reserve deck to imperial city but move
    guys to the palace which is a battle ground."
    Uses `location` (method parameter) not `currentLocation` (which
    is only in scope inside the V37 block). Mirrored in both rando
    and chosenone MoveEvaluator at the V34/V38.3 destination scope.


  ════ V112 family (2026-05-21) ════

  V112 (2026-05-21) — BATTLE ORDER / BATTLE PLAN GATE
    (evaluateUnknown path)
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Same incident replay: Rando played Battle Order as a defensive
    shield without occupying a battleground SYSTEM AND a battleground
    SITE — violating the card's "if Dark Side controls battleground
    location" prerequisite. V51 already blocked this in the explicit
    `evaluateDefensiveShieldSelection` path, but K&D "play a card"
    with a mixed stack (shields + non-shields) routes through
    `evaluateUnknown` when isShieldSelectionByContent returns false
    (<50% shields in stacked pile). V51 was bypassed.
    V112 mirrors the V51 gate in `evaluateUnknown`: if action title
    matches "battle order" or "battle plan", iterate the table for
    friendly characters/starships/vehicles at battleground locations.
    Track v112BGSystem (SYSTEM subtype) and v112BGSite (SITE subtype).
    If either is missing → -9999 hard block. Same logic in both
    rando and chosenone CardSelectionEvaluator before the V22
    starting-effects block, after the V70 weapon check.


  ════ V113 family (2026-05-21) ════

  V113 (2026-05-21) — SOLO ABILITY-3+ CHARACTER VULNERABILITY (-300)
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Same incident replay: Rando deployed Dengar (ability 3) alone
    to a site, and Anakin + Chewie overwhelmed him next battle turn.
    V29.5 BUDDY already penalized solo deploys to OPPONENT locations,
    but Rando's OWN locations got +40 with no solo-vulnerability
    check. V113 closes the gap: any character with ability ≥ 3
    deployed alone (no friendly characters already at the location)
    takes -300 regardless of who owns the destination location.
    Rando's V113 lives inside the V29.5 buddy try-block (rando line
    ~2333) — easy because V29.5 already computed friendlyCharsHereBuddy.
    Chosenone's V113 sits in a different spot (chosenone line ~984)
    because chosenone lacks V29.5's buddy system; it counts friendly
    characters at the candidate location via gameState.getCardsAtLocation
    and applies the same -300 when count == 0 AND ability ≥ 3.


  ════ V114 family (2026-05-21) ════

  ════ VEHICLE-PILOT GENERIC RULE (2026-05-31; STARSHIP + affordability + embark + power-3 gate 2026-06-01; no V-tag) ════

  2026-06-01 POWER-3 GATE (Steve, follow-up #2):
    Steve: "If pilot is power 4 or more let's leave them disembarked from
    vehicles. Likely better as ground troops. Regular pilots are usually
    power 3 or less."
    Rationale: characters like Darth Vader (power 6+), Tarkin (power 4),
    General Veers (power 3 -- borderline), even some Imperial officers
    have Icon.PILOT but their per-character power is far more valuable
    contributing to ground battles than to a walker that still does its
    job on a power-2 trooper crew. Heavyweight piloting a walker swaps
    the ground impact for a vehicle's base power -- usually a downgrade.
    Two surgical gates added:
      • ActionTextEvaluator.evaluateEmbark — after the non-pilot early
        return, read embarkerBp.getPower(); if power != null AND power
        >= 4f, return 0 with reason
        "Embark action (skipped: power N — better as ground troop)".
        The Embark itself stays a legal action; we just don't BOOST it
        for the heavyweight, so other choices (drain, ground move,
        battle) win.
      • DeployEvaluator Path B — compute pathBPower at the top of the
        block and gate on (pathBPower == null || pathBPower < 4f).
        Path B's +400 "PILOT FOR UNMANNED VEHICLE/SHIP" boost now only
        fires for power-3-or-less characters. High-power pilots can
        still deploy normally; they just don't get the steer-toward-
        the-vehicle boost.
    Path A (vehicle-needs-pilot block) is intentionally LEFT ALONE because
    the vehicle still needs *some* pilot — the gate only changes WHICH
    pilot we steer toward, not whether the vehicle is blocked when no
    pilot is available. The Path A check already accepts any Icon.PILOT
    or Keyword.TROOPER character; that's correct (any pilot fixes the
    walker's power problem), but the new gate ensures Rando preferentially
    uses the cheap power-3 troopers for crew duty instead of his Vader.
    Magnitude unchanged for everything else: -1500 Path A solo block,
    +400 Path B deploy-aboard (power < 4 only), +500 embark (power < 4
    only). Trooper keyword + Icon.PILOT detection unchanged.



  2026-06-01 EMBARK BOOST (Steve, after the affordability ship):
    Steve: "But Rando already had pilots on the same site. He's not embarking
    them onto the walkers or vehicles." The replay logs (last game, line
    38725-38737) showed three 'Embark' actions offered by the engine, each
    scored 0 by ActionTextEvaluator's evaluateEmbark placeholder:
      [1] cardId=241, bp=inPlay, action='Embark'
      [2] cardId=242, bp=inPlay, action='Embark'
      [3] cardId=368, bp=inPlay, action='Embark'
      [ActionText] Embark: 0.0 - Embark action
    The method had a TODO comment ("could be improved with pilot detection")
    and just returned 0 for every Embark action. Pass / generic moves
    outscored it, the pilot stayed on the ground, walker stayed at 0 power.
    Implementation:
      • Threaded `cardId` from the outer loop in ActionTextEvaluator.evaluate
        (line 91 already had cardId in scope) into evaluateEmbark's signature.
      • In evaluateEmbark: resolve the embarker via gameState.findCardById,
        check Icon.PILOT or Keyword.TROOPER (same detection as Path A/B for
        consistency).
      • Find the embarker's location via
        ModifiersQuerying.getLocationThatCardIsAt.
      • Walk gameState.getAllPermanentCards for friendly VEHICLE or STARSHIP
        at the SAME location whose Filters.piloted.accepts is false (i.e.,
        unmanned).
      • Match → +500 with reason "EMBARK PILOT: 'X' boarding unmanned 'Y'
        — vehicle gets power & protection".
      • Non-pilot embarker / no-unmanned-target / null-context paths all
        return 0 (neutral) with descriptive sub-reason logs.
    Magnitude rationale: +500 is high enough to dominate any Pass (~6) or
    generic-move score (small positive), and is roughly on par with the
    Path B deploy-side boost (+400) — embarking a pilot from the ground is
    the move equivalent of deploying one from hand onto a ship. Slight
    bump above +400 because embark spends 0 Force (always strictly better
    than the equivalent deploy when both are available).
    Lives in evaluateEmbark only — no other evaluator changes. Embark is a
    MOVE action routed through ActionTextEvaluator (action text == "Embark"),
    not through MoveEvaluator's main move-using-landspeed branch.



  2026-06-01 AFFORDABILITY EXTENSION (Steve, both losing games):
    Replay analysis of the two losses Steve flagged ("Bring Him Before Me"
    Vader-hunting deck + "A Great Tactician" Hoth Walker deck) showed the
    pattern Steve called out: "I believe in both games Rando had Walkers and
    did not put pilots on them. Easy targets and some of the walkers were
    powerless with no pilot."
    Log evidence (last game, line 3717-3718):
      V40 SHIP ABILITY: Blizzard 2 — pilot exists but unaffordable —
        mild warning (-50, was -400)
      [DeployEvaluator] Scored 'Deploy' -> 335.0 (V38.4 DEPLOY URGENCY +160
        | IN DEPLOYMENT PLAN: reinforce +100 | Highest priority +50 | V40
        SHIP ABILITY pilot unaffordable -50 | Starship/Vehicle deployment
        +15 | Pilot character +10)
    Pilot was in hand, Force was already spent, V40 only mildly penalized,
    Blizzard 2 hit the table at 0 power because the Force was gone — opponent
    attacked it next turn with impunity. The original Path A only blocked
    "no pilot at all" because the variable was named hasPilotInHand and a
    truthy value (pilot exists) short-circuited the block.
    Why not just re-strengthen V40 to -1500? V40 was deliberately softened by
    Steve in an earlier session ("was -400" comment in source). Reverting it
    risks reverting whatever case Steve had in mind when softening — and
    Steve's standing rule is "OLD RULES DO NOT GO MISSING — THEY GET
    DOMINATED." So leave V40 alone, dominate from Path A with the proper
    affordability check.
    Path A revision:
      • Replaced single-boolean hasPilotInHand with TWO booleans:
        hasPilotInHand (any Icon.PILOT or Keyword.TROOPER character in hand)
        AND hasAffordablePilotInHand (such a character whose deployCost +
        vehicle's deployCost <= context.getForcePileSize()).
      • The -1500 block now fires when (NOT hasAffordablePilotInHand) AND
        (NOT hasFreePilotOnTable). Pilot-in-hand-but-unaffordable triggers
        the block the same as pilot-not-in-hand.
      • Affordability math mirrors the existing V35.6 affordability check
        at line 4840-4846 (base deploy cost, no modifier modeling). Matching-
        pilot reductions and other modifiers are not modeled — conservative
        on purpose: Rando holds the vehicle one extra turn rather than risk
        a 0-power kill.
      • Two log sub-cases for triage: "no Icon.PILOT or Trooper character
        available" vs "pilot in hand but unaffordable (vehicle=N, force=M)
        — wait for force".
    The block magnitude (-1500) dominates the +335 net deploy score, so
    Rando passes or picks something else instead of shipping a naked walker.



  2026-06-01 STARSHIP EXTENSION (Steve, First Light replay):
    Original rule scoped to CardCategory.VEHICLE only. First Light is
    CardCategory.STARSHIP (subtype CAPITAL), so the gate didn't fire and
    Rando shipped 5-power solo First Light into a contested 6-power site;
    battle attrition 13 / damage 12 wiped Rando's army (Dryden Vos, Hondo
    Ohnaka, Lady Proxima, Cad Bane all forfeited) and lost the game.
    Steve: "Rando lost from First Light battle at system was bad. He needed
    to deploy a pilot."
    Extension (3 surgical edits in DeployEvaluator):
      • Path A category gate: was `category == CardCategory.VEHICLE`, now
        `category == CardCategory.VEHICLE || category == CardCategory.STARSHIP`.
      • Path B unmanned-scan: was `getCardCategory() != CardCategory.VEHICLE`,
        now also accepts STARSHIP.
      • Log strings renamed VEHICLE → VEHICLE/SHIP for clarity.
    Covers Falcon, Slave I, First Light, X-Wing, TIE Fighter, Star Destroyer,
    Y-Wing — every starship without a named pilot match (V30 still handles
    named matches) now requires a generic pilot in hand or on table. No new
    V-tag; same rule, broader scope.



  Every vehicle needs a pilot — generic complement to V30 named pairs
    Source: ai/models/rando/evaluators/DeployEvaluator.java (after V30
    reverse rule at the old line 4688)
    Steve (multiple games, 2026-05-31): "In the last few games Rando does not
    seem to understand piloting a vehicle. In the Imperial Entanglements game
    Rando deployed speeder bike but no pilot for the bike. Bike is useless. In
    the last Hoth Walker game Rando deployed a walker without a pilot making
    the walker useless. Rando should try to deploy troopers to speeder bikes.
    And imperial pilots to Walkers. Of all other vehicles Rando should deploy
    a pilot on the vehicle. It makes them faster and protects them and pilots
    add power to the vehicles."
    V30 above only fires for NAMED pilot/ship pairs (Wedge+X-Wing,
    Piett+Executor) via SwccgCardBlueprint.getMatchingStarshipFilter(). For
    generic pilot+vehicle combos that lack a named match (any trooper +
    speeder bike, any Icon.PILOT character + AT-ST, etc.) V30 was silent and
    Rando happily deployed solo vehicles.
    New block:
      Path A (vehicle deploy gate): when scoring a VEHICLE deploy, scan hand
        and the on-table side for ANY character with Icon.PILOT or
        Keyword.TROOPER. If none found → -1500 reasoning "VEHICLE NEEDS
        PILOT". Soft block (not -9999) so degenerate cases can still ship a
        vehicle as the least-bad option; magnitude -1500 dominates the V67ai
        +1400 location-hand boost AND most character-deploy boosts, but
        leaves room for stronger overrides (V30 named pairs at +1000, V38.4
        deploy-urgency etc. compose normally).
      Path B (pilot deploy boost): when scoring a CHARACTER deploy and the
        character has Icon.PILOT or Keyword.TROOPER, walk all in-play
        vehicles on Rando's side and check Filters.piloted — if any vehicle
        is NOT piloted, +400 reasoning "PILOT FOR UNMANNED VEHICLE: '<title>'
        on table without a pilot — get this pilot aboard!". Magnitude +400 is
        on par with V30's +300 MATCHING SHIP IN PLAY, slightly higher because
        this is the generic BASE case Steve flagged as missing.
    "Pilot-capable" detection (UNIVERSAL, no card-name hardcoding):
    Icon.PILOT covers Imperial Pilots (Walker crews), generic squadron pilots
    (X-Wing/TIE Fighter crews), and any character explicitly marked as a
    pilot by the SWCCG icon system. Keyword.TROOPER covers Stormtroopers /
    Snowtroopers / Sandtroopers / Coruscant Guards etc. — the speeder-bike
    pool plus any future trooper-keyword characters. The OR composition means
    "trooper without Icon.PILOT" still counts as pilot-capable, since SWCCG
    mechanics sometimes let troopers ride speeder bikes via game-text grants.
    The engine's SwccgCardBlueprint.getValidPilotFilter handles fine-grained
    game-text exceptions during the actual deploy; this AI heuristic gets
    95%+ of cases right and the engine catches the rest by rejecting the
    illegal deploy (engine-side, not AI-side).
    No new V-tag (Steve's standing "avoid splintering off versions" + "do
    all three but append to existing rules"). Block lives directly after
    V30's reverse rule so V30 and this generic rule compose: V30 fires named
    bonuses (+1000) when a specific match exists, the generic rule provides
    the floor (-1500 solo block, +400 pilot-aboard boost) for everything
    else.

  ════ MOVEEVALUATOR BLOCKED-RESPONSE GATE (2026-06-02; no V-tag) ════

  Third evaluator wired into cancel-loop enforcement
    Source: ai/models/rando/evaluators/MoveEvaluator.java (top of action loop
    after the `isMoveAction` filter)
    Steve replay 2026-06-02 (described as "Rando locked up looking for a card
    in lost pile" — actual lockup was on MOVE phase): Rando, turn 6, was
    asked CARD_ACTION_CHOICE "Choose Move action or Pass" and kept picking
    actionId='2' = 'Move using landspeed' on Ponda Baba (V) (cardId 200_87,
    set 200 reflection). Sub-decision "Choose where to move Ponda Baba ...,
    or click 'Done' to cancel" scored every legal destination negative:
      [CardSelection] Move to Jabba's Palace: Dungeon: -432.5
        - V67g ZERO DRAIN: Jabba's Palace: Dungeon has no opponent force
          icons — wasted move! (-200.0)
        - V67g MOVE-FROM-DRAIN: leaving Jabba's Palace: Audience Chamber
          (drain 1) for Jabba's Palace: Dungeon (drain 0) — losing 1 drain!
          (-250.0)
      All actions bad (best: -432.5), choosing to PASS
    Rando responded empty ('Done'), DecisionTracker.consecutiveCancelCount
    incremented to 3 → blockLastActionOnCancel fired and added '2' to
    blockedResponses for the outer key. Log shows:
      Blocking action '2' for 'CARD_ACTION_CHOICE:Choose Move action or
        Pass' - target selection was cancelled
      CANCEL LOOP BROKEN (3 consecutive Dones on 'CARD_SELECTION:Choose
        where to move <div class='ca'): blockLastActionOnCancel returned
        true — Rando will pick differently next phase
    But the loop CONTINUED. CANCEL LOOP BROKEN fired 8 times in this game
    alone. Why: MoveEvaluator never consulted context.getBlockedResponses().
    On each phase tick, MoveEvaluator re-scored 'Move using landspeed'
    positively, the merged picker chose '2', sub-decision re-cancelled,
    cancel-loop re-fired and re-blocked, infinity ensued.
    ActionTextEvaluator had honored blockedResponses since the original
    cancel-loop work (line 86-99 in that file). DeployEvaluator was patched
    on 2026-05-31 (commit 5df527801 "DeployEvaluator: honor blockedResponses
    (cancel-loop enforcement)"). MoveEvaluator was the third and final
    evaluator with the same hole.
    Fix mirrors the DeployEvaluator patch exactly:
      java.util.Set<String> v160MoveBlocked = context.getBlockedResponses();
      // inside the per-action loop, after isMoveAction filter:
      if (v160MoveBlocked != null && !v160MoveBlocked.isEmpty()
              && (v160MoveBlocked.contains(actionId)
                  || v160MoveBlocked.contains(actionText))) {
          EvaluatedAction blockedMove = new EvaluatedAction(
              actionId, ActionType.MOVE, -9999.0f, actionText);
          blockedMove.addReasoning(
              "CANCEL-LOOP BLOCK: this move led to repeated Done-cancels "
              + "— try something else", -9999.0f);
          logger.warn("MoveEvaluator: actionId='{}' is in blockedResponses "
              + "→ -9999 (cancel-loop block)", actionId);
          actions.add(blockedMove);
          continue;
      }
    Variable name v160MoveBlocked echoes v159DeployBlocked from
    DeployEvaluator for consistency; not a new V-tag, just a local var
    namespace (the actual numbered V160 is reserved separately if/when
    Steve assigns one). Reason string and magnitude (-9999) match
    DeployEvaluator's pattern verbatim so all three evaluators behave
    identically on a blocked response.
    Why this happened: V67g (and other move-scoring rules) judge moves on
    their tactical merit (drain potential, force generation, battle
    setup). When every destination is bad, MoveEvaluator's "all actions
    bad → choose Pass" fallback returns score 0; the merged picker still
    sees '2' as a non-negative outer pick because ActionTextEvaluator
    scores 'Move using landspeed' neutrally and there's no -ve signal
    against it. The blockedResponses set IS the signal — every evaluator
    has to apply it on its own actions.
    Side gap noted (not fixed this commit): the same game log showed an
    earlier ARBITRARY_CARDS decision "Verify Lost Pile after unsuccessful
    attempt to 'Choose card to retrieve'" with 18 cards where
    "No evaluators produced actions". evaluateUnknown returns nothing for
    that decision-text family, leaving RandoCalAi to fall back to the
    heuristic. Not blocking the current MOVE lockup; tracked separately
    if it re-surfaces.

  ════ MULTI-SELECT RESPONSE FIX (2026-05-31; no V-tag) ════

  ARBITRARY_CARDS / multi-select min>1 response builder
    Source: ai/models/rando/RandoCalAi.java (tryEvaluators, after combinedEvaluator.evaluateDecision)
    Steve, Hoth-deck replay (2026-05-31): "Rando keeps getting stuck. He won't
    pass or move onto his next move. several games. Other K2 tried to solve
    but he's not fixed it yet." Live log shows the same decision repeating
    forever:
      [RandoCalAi] decide() called: type=ARBITRARY_CARDS, phase=ACTIVATE,
        text='Choose Walker Garrison and 3rd Marker to take into hand'
      Selection min=2, max=2, noPass=true
      [42 cards, 2 selectable: temp7 (Hoth: Defensive Perimeter, 3rd Marker)
       and temp25 (Walker Garrison)]
      Best action: Select Hoth: Defensive Perimeter (3rd Marker) (score: 40)
      [RandoCalAi] decide() result: 'temp7' ✅
      [...same decision re-prompted, Rando picks temp7 again, infinite loop...]
    This is the You May Start Your Landing turn-1 starting effect, which lets
    Rando set up two specific cards before turn 1 begins. The engine's
    ArbitraryCardsSelectionDecision.getSelectedCardsByResponse(String) splits
    the response on commas and validates length in [_minimum, _maximum]; a
    single-ID response with min=2 throws DecisionResultInvalidException and
    GEMP's outer decision loop simply re-prompts. Rando responds the same way
    and we burn forever.
    The cancel-loop detector (DecisionTracker) does NOT catch this because
    each response is non-empty. The sequence-loop detector (checkSequenceLoop)
    DOES detect the repeat ("In potential loop (Nx repeats), checking blocked
    responses") but its only mitigation is adding the response to
    blockedResponses — which Rando just re-picks anyway because the evaluator
    has no other ID to return.
    The bug is pre-existing — predates the cancel-loop work — but Steve was
    right to suspect a recent fix because the SYMPTOM matches the earlier
    stuck-loop class. It's an output-format gap: tryEvaluators returns
    bestAction.getActionId() (single string), with no path to comma-join
    multiple IDs when the engine requires min>1.
    Fix at the response boundary in tryEvaluators (single change site, every
    evaluator benefits): when context.getMin() > 1 AND cardIds/selectable are
    populated AND consistent, build a LinkedHashSet of card IDs:
      1. Seed with bestAction.getActionId() if it's actually a card ID in the
         offered cardIds list AND that card is selectable. Guards against
         CARD_ACTION_CHOICE action-index leakage (where the bestAction's "ID"
         is an action index like '0'/'1', not a temp-prefixed card ID).
      2. Fill remainder from selectable cards in list order until exactly
         `min` IDs are collected.
      3. Return the comma-joined string (response.split(",") on the engine
         side now sees exactly `min` IDs, validation passes).
    If somehow fewer than `min` selectable IDs are collected (degenerate case),
    log a fallback warning and return the original single ID — engine still
    rejects but at least we don't return wrong data silently.
    Magnitude rationale: NONE. This is a format fix, not a scoring rule. No
    new V-tag (Steve's standing "avoid splintering" directive). Lives in
    RandoCalAi at the boundary so any decision (ARBITRARY_CARDS, CARD_SELECTION
    with min>1, future multi-select types) gets the right response format
    without per-evaluator wiring.

  ════ PULL-TARGET: DOWNLOAD-ENABLER PRIORITY (2026-05-31; no V-tag) ════

  Universal deploy-chain rule for Take-from-Reserve picker
    Source: ai/models/rando/evaluators/CardSelectionEvaluator.java
    (evaluateTakeIntoHand, after V24.2 Lando/Lobot block)
    Steve, Xizor / Black Sun replay (2026-05-31): "Rando didn't deploy his
    locations. He needs to try and deploy Xizor: Palace first then he can
    deploy Xizor's Palace: Sewer from reserve using Xizor's Palace game text."
    Investigation confirmed Coruscant: Xizor's Palace (203_32) was in Rando's
    Reserve all game, never pulled into hand. None of his Effects / Objective
    can [download] a location of the Xizor's Palace family; the only path was
    the generic "Take card into hand from Reserve Deck" action followed by
    a hand-deploy. Steve refined: turn-1 chain is Vigo (200_91, "may use 1
    Force to [download] a non-war room battleground planet site (or system)
    not already on table") deploys cheap to Coruscant: Imperial City (already
    on table from Agents Of Black Sun objective), then Vigo [downloads] the
    Palace ON TABLE for 1 Force, then the Palace [downloads] Sewer ON TABLE.
    Three locations on table turn 1 = massive Force generation + drain options.
    Universal detection (no card-name hardcoding per Steve's standing rule):
    when evaluating a Take-from-Reserve target, scan its blueprint game text
    (concatenating getGameText + getLocationLightSideGameText +
    getLocationDarkSideGameText per the V71 location-text pattern at line
    6337) for "[download]" + any of {site, location, system, battleground}.
    If matched, +500 boost. Magnitude rationale: on par with V24.1 Gherant
    (+400 — strongest TDIGWATT-specific pull boost), because deploy-enablers
    COMPOUND — each one sets up 2-3 future location pulls. Universally covers
    Vigo, Coruscant: Xizor's Palace, Shadows Of The Empire, Cloud City
    download-source sites, Death Star II [download] sites, and any future card
    matching the pattern. Character-target downloads (e.g. Coruscant: Imperial
    City "[download] a Black Sun character") don't fire because the target
    word "character" isn't in the location-word set — guards against the
    rule mis-firing on character-only [download] cards.
    Appended into the existing take-into-hand block, no new V-tag (Steve:
    "avoid splintering off versions like before"). Note: this only addresses
    the Take-from-Reserve picker. Once the enabler is in hand, V67ai
    (LOCATION DEPLOY ORDER Tier 4 HAND, +1400) and V67i (global location-
    first priority) already boost the deploy-from-hand path; once it's on
    table, the engine offers the [download] action and V67i continues to
    favor it. So one rule, in one spot, drives the whole chain.

  ════ DecisionTracker: CANCEL-LOOP DETECTION (2026-05-29; counter-reset + block-enforcement fixes 2026-05-29) ════

  2026-05-29 BLOCK-ENFORCEMENT FIX in DeployEvaluator (Steve "Stuck again!!!"):
    With the counter-reset fix shipped, CANCEL LOOP BROKEN started firing
    correctly — log showed it tripping 10x in one game on U-3PO and other
    sub-cancels. But Rando STILL re-entered the loop because the block didn't
    bind. Root cause: blockedResponses is per-decisionKey, and the engine
    DOES pass it to evalContext (RandoCalAi line 931-933). ActionTextEvaluator
    honors it (line 86-97: hard-skips blocked actionIds/texts). DrawEvaluator
    honors it. **DeployEvaluator does NOT.** When the outer "Choose Deploy
    action or Pass" decision came back with action '1' blocked, DeployEvaluator
    re-scored Deploy at -50 (its only non-Pass option), Rando picked '1' again,
    sub-decision re-cancelled, loop persisted.
    Fix: in DeployEvaluator's action-evaluation loop, read
    context.getBlockedResponses() once at the top; for each candidate action,
    if its actionId or actionText is in the block set, emit a -9999
    EvaluatedAction with reason "CANCEL-LOOP BLOCK". Same pattern as
    ActionTextEvaluator. Verified jar contains "CANCEL-LOOP BLOCK".
    Note: this addresses the IMMEDIATE outer-decision binding. If
    CardSelectionEvaluator (sub-decisions) or other evaluators (BattleEvaluator,
    SpeedEvaluator) ever become the bottleneck for a blocked action, the same
    pattern should be propagated. Surgical change for now: only DeployEvaluator
    is patched because that was the actual stuck path in Steve's replay.



  2026-05-29 COUNTER-RESET FIX (Steve "Dude... still doing it" + U-3PO replay):
    The first cancel-loop install reset the consecutiveCancelCount on ANY
    non-empty response. That looked right ("Rando picked something real, streak
    over") but it misses the actual loop pattern: OUTER_PICK (non-empty, key A
    "Choose Deploy action or Pass" → response '3' Deploy U-3PO) → SUB_CANCEL
    (empty, key B "Choose where to deploy U-3PO, or click Done to cancel") →
    OUTER_PICK (non-empty, key A) → SUB_CANCEL (empty, key B) → ... The OUTER_PICK
    is non-empty so it wiped the SUB_CANCEL streak every cycle, counter stuck
    at 1, threshold of 3 never reached.
    Fix: only reset the cancel counter when the non-empty response is to the
    SAME key being tracked (key.equals(consecutiveCancelKey)). That means Rando
    finally picked a real sub-target instead of cancelling. A non-empty response
    to a DIFFERENT key (the outer pick that DRIVES the sub-cancel) preserves
    the streak, so SUB_CANCEL → SUB_CANCEL → SUB_CANCEL across interleaved
    OUTER_PICKs trips the threshold and blockLastActionOnCancel fires.
    With this fix, the U-3PO loop will block 'Deploy U-3PO' as the outer
    response after 3 consecutive sub-cancels — Rando picks Blast Door Controls
    or another action next phase. (Note: if the next pick ALSO has no legal
    sub-target, the same mechanism will block IT too, and Rando converges to
    passing the phase entirely. That's correct fallback behavior.)
    Also still relevant: U-3PO scored -1700 at every legal site (V59 SPY
    UNIVERSAL: only-us spy would block own drain). The deeper fix is to teach
    DeployEvaluator that a card scoring negative at every site shouldn't be
    PICKED as the outer Deploy choice — that's a separate ticket.



  Cancel-loop detection — wire blockLastActionOnCancel into recordDecision
    Source: ai/models/rando/DecisionTracker.java
    Replays 4g68 (Shadow Collective) and deuvei7 (Naboo): Rando got stuck turn 1,
    only deployed a location, never tried any of the multiple characters in hand.
    Root cause: the outer "what to play" pick kept choosing the same card (Dr.
    Evazan in 4g68); engine then asked "where to deploy, or click Done"; site-pick
    hard-blocked all sites (V89-CS Dr. Evazan no-armed-friend, etc.); Rando hit
    Done; next phase tick engine re-asked same outer decision; Rando picked same
    card again. Infinite Done loop.
    The existing loop detector explicitly skips empty responses (line 152:
    "CRITICAL: Only track NON-PASS responses for loop detection") so the counter
    never incremented. blockLastActionOnCancel existed with the exact docstring
    for this case ("Force Lightning → cancel target → action now blocked") but
    had no caller.
    Fix: in recordDecision, when the response is empty AND the decision is a
    cancelable sub-decision (CARD_SELECTION or ARBITRARY_CARDS with "cancel"/
    "done" in the text), increment a per-decision-key cancel counter. After 3
    consecutive Dones on the same key (Steve's threshold), invoke
    blockLastActionOnCancel, which blocks the outer action (e.g. "Play Dr.
    Evazan") for the remainder of the turn. Next outer pick chooses something
    else. Non-empty responses and genuine end-of-phase passes reset the counter.
    Constants: CANCEL_LOOP_THRESHOLD = 3.

  ════ V61c (2026-06-30): always keep 3 cards in the Reserve Deck for battle/weapon destiny ════

  V61c — DESTINY BUFFER (backfilled 2026-07-01; full entry in resources/AI_CHANGELOG.md 2026-06-30)
    Source: ForceActivationEvaluator.java (calculateActivationAmount ~186) + ActionTextEvaluator.java
    (V168 block ~166, V38.3 confirm ~1342) — rando only. Steve: "He should probably always keep 3
    cards in reserve for any battle that turn." Root cause was Steve's own V168 always-activate
    (+5000) draining the reserve to 0, so V61 blocked battles (no destiny to draw).
    Fix, two halves: (1) cap activation at reserveDeck - 3 (min 1); (2) when reserve <= 3, carve
    exceptions into V168 (score Activate Force -6000 so Pass wins) and V38.3 (honor the "have not
    activated - pass?" confirm with Yes) so Rando passes activation entirely instead of eroding
    3->2->1->0 (the engine forces >=1 per activation). Reserve > 3: activate down to exactly 3.
    Boundary: -6000 dominates the +5000 it replaces; Pass (~5-8) clears BAD_ACTION_THRESHOLD (-100).
    reserve >= 4 leaves V168/V38.3 untouched. Tradeoff Steve accepted: low-reserve turns get no new
    Force (future refinement: only protect the buffer on turns Rando intends to battle).
    Status: UNCOMMITTED in the working tree, live in the jar. PENDING live-game confirm
    (grep gemp-swccg.log for "V61c DESTINY BUFFER").

  ════ V179 FIX (2026-06-29, #4): don't rank a download of a held location above deploying it ════

  V179 FIX — A GOOD FRIEND / A CUNNING WARRIOR WASTED THEIR DOWNLOAD BEFORE THE LOCATION WAS DOWN
    Source: DeployPhaseScript.java (rando + chosenone), resolveSteps section D + new namedLocationInHand.
    Steve's report: A Good Friend never pulled the epic event Be With Me even though Ahch-To was on
    table. Replay-verified root cause (NOT engine/card): Rando's once-per-turn [download] (A Good
    Friend + A Cunning Warrior each download a location / epic event / weapon) was classified as a
    high-priority LOCATIONS pull because the target text contains "jedi village", outranking deploying
    the actual Ahch-To: Jedi Village from hand. So Rando fired the download FIRST, while Ahch-To was
    still in hand -> no legal target (Be With Me needs an Ahch-To location on table) -> engine
    reshuffled and the once-per-turn download was burned. Ahch-To deployed seconds later, too late;
    Be With Me never came out.
    Fix: in the DPS bucketer, a location-naming pull target classifies as LOCATIONS only if that
    location is NOT already in the bot's hand (namedLocationInHand). Dropping the LOCATIONS tag moves
    the download out of the LOCATIONS bucket, so the real hand-deploy wins the LOCATIONS step and lands
    first; the download fires later, once the location is down, and can then pull Be With Me.
    Boundary: only a specific named location (>=4 chars, not a bare category word) matched against a
    LOCATION card in hand gates; generic/objective location pulls and reserve-present locations are
    untouched. Adjusts V179 in place, no new V-tag. Compiles clean; live in the jar (namedLocationInHand
    in both bots). PENDING: live Saga game.

  ════ V120 FIX (2026-06-29): weapon-pull block no longer mis-fires on a character pull ════

  V120 FIX — "DEPLOY VADER FROM RESERVE DECK" WAS BLOCKED AS A WEAPON PULL
    Source: ActionTextEvaluator.java (rando + chosenone), the V120 weapon-title match (~1877).
    Steve's report (lost the Hunt Down game): Vader never deployed. The 2026-06-29 replay confirmed
    V120 hard-blocked "Deploy Vader from Reserve Deck" at -9999 every turn. Cause: the V125
    bidirectional contains() match also matched a CHARACTER pull whose name is a substring of a
    weapon title — it parsed "vader", and "darth vader's lightsaber" (in the deck) contains "vader",
    so the Vader character pull was classified as a weapon pull with no holder and blocked. Vader is
    the deck's flip engine, so the objective never flipped and Rando lost.
    Fix: for the loose "title contains parsed-name" direction, require the parsed name to cover the
    weapon title's last significant word (the weapon noun, e.g. "lightsaber"), not just the owner
    portion. "vader" lacks the noun -> no match. "vader's lightsaber" has it -> still matches, so the
    V125 abbreviated/prefixed-title case is preserved. Strips (V)/parenthetical suffixes; >=4-char
    noun threshold mirrors the V185/DeckOracle idiom.
    Boundary: V120 still blocks a real weapon pull with no holder. Adjusts V120/V125 in place, no new
    V-tag (update-old-rule rule). Compiles clean (mvn -pl gemp-swccg-server -am compile, EXIT=0).
    PENDING: reload-ai + a live Hunt Down game (Vader actually deploys; grep nohup.out that "Deploy
    Vader from Reserve Deck" is no longer V120-blocked).

  ════ V61b (2026-06-28): battle anyway when OVERPOWERING, even with an empty Reserve Deck ════

  V61b — OVERPOWER OVERRIDE ON THE V61 EMPTY-RESERVE PENALTIES (backfilled 2026-07-01)
    Source: BattleEvaluator.java (~627), rando only.
    Steve's report: Rando sat on a massively overpowered Hoth site (26 vs 4) and refused to battle
    all game because its Reserve Deck was empty (V61's -800/-400/-200 no-destiny penalties dominated).
    Fix: before the V61 chain, re-scan the best friendly-vs-opponent power margin across
    battle-eligible sites; margin >= 8 sets v61Overpowering and SKIPS the V61 penalties — battle
    on raw power, overflow damage wins without destiny.
    Boundary: margin < 8 leaves V61 fully intact (close battles still avoided with no destiny).
    Status: UNCOMMITTED in the working tree, live in the jar; fired live (18-vs-1 -> battled).

  ════ V79b (2026-06-28): Death Star parsec choice — steer Verge of Greatness to Scarif ════

  V79b — THE PARSEC NUMBER IS A SEPARATE MULTIPLE_CHOICE DECISION V79 NEVER SAW (backfilled 2026-07-01)
    Source: RandoCalAi.java (~692) + MoveEvaluator.java (~291), rando only.
    Steve's report (two games running): "the death star needs to move to the right parsec number.
    we never got that right in the previous fix." V79 (2026-05-15) steered the MOVE action, but the
    engine asks for the parsec NUMBER in a separate "Choose parsec to move to" MULTIPLE_CHOICE that
    fell through to the generic chooser — the Death Star loitered at parsec 4.
    Fix: a V79b MULTIPLE_CHOICE handler — when Verge of Greatness is on table, pick the option
    closest to parsec 7 (or an explicit orbit/Scarif option). Rider: MoveEvaluator's V79 parsec
    regex now takes the LAST parsec in the action text (inert for this decision, kept as
    robustness). Gated on Verge in play + the parsec prompt; all other MULTIPLE_CHOICEs untouched.
    Status: UNCOMMITTED in the working tree, live in the jar. PENDING live Verge game
    (4->6 turn 1, 6->7 turn 2, then orbit Scarif).

  ════ V187 (2026-06-28): -300 to duplicate starting effects (DeckOracle) ════

  V187 — PREFER ONE-OF EFFECTS AT STARTING-INTERRUPT SETUP (backfilled 2026-07-01)
    Source: CardSelectionEvaluator.java (~7937, inside the turn<=0 V22 starting-effect block) +
    DeckOracle.java new countCopiesByTitle (~389), rando only.
    Steve: "I want a -300 points to any effect that Rando has duplicates in the deck of. So use
    deck oracle to help score which starting effects should be chosen." Effects are unique-in-play;
    deploying one of a pair leaves a dead duplicate in the deck, deploying a one-of keeps it clean.
    Boundary: -300 re-orders only within the candidate pool; V22 preferred (+200) and V80/V186
    objective boosts (+1000) still dominate by design. Nothing is blocked, only demoted.
    Status: committed (rode into 8fd884375). PENDING a live game with a duplicated effect
    (grep "V187 DUPLICATE").

  ════ LOGGING (2026-06-28): decision log survives restarts (mainlog appender) ════

  LOGGING — com.gempukku LOGGER NOW WRITES TO logs/gemp-swccg.log (backfilled 2026-07-01)
    Source: src/gemp-swccg-async/src/main/resources/prod-log4j.xml (~31-36). Not a scoring rule.
    Steve: "Last replay is not readable?" The decision log lived on stdout only, which moved
    between nohup.out and the container TTY across restarts — every restart orphaned the previous
    game's decisions. Fix: AppenderRef ref="mainlog" level="info" on the com.gempukku logger
    (additivity=false), so V-tag lines also land in the mainlog RollingFile: logs/gemp-swccg.log,
    10MB rotation into logs/YYYY-MM/app-*.log.gz (macOS zcat is broken; use gunzip -c).
    Status: UNCOMMITTED in the working tree, live and verified (V61b/V61c lines observed post-restart).

  ════ V188 (2026-06-28) ════

  V188 — SET YOUR COURSE FOR ALDERAAN: don't deploy ability characters to Death Star sites
    Source: CharacterDeploySiteEvaluator.java (evaluateSite, shared common/ → both bots and both
    deploy paths: DeployEvaluator from-hand + CardSelectionEvaluator choose-target).
    Steve's report: "When Rando is playing 'Set Your Course For Alderaan' he should not deploy his
    characters with ability to the Death Star, he can't Force drain there. Wasted guys deployed
    there last game." The objective's FRONT text reads: "At Death Star sites, your Force drains and
    battle damage against you are canceled." An ability character at a Death Star site can't drain
    (its whole purpose), so it is wasted; steer it to a drainable battleground instead.
    Fix: an early gate at the top of evaluateSite returns -900 when (deploying card ability >= 1)
    AND (candidate site is Filters.Death_Star_site) AND (the player has "Set Your Course For
    Alderaan" in play, front). Front-only detection comes for free: PhysicalCardImpl.getBlueprint()
    (:497) returns _backBlueprint once flipped, so getTitles()/Filters.title stops matching the
    instant the card flips to "The Ultimate Power In The Universe", at which point the Death Star
    becomes the win condition and you WANT presence there, so the gate lifts on its own.
    Boundary (additive-domination): a NARROW early gate that fires ONLY for (ability >= 1 + Death
    Star site + this objective front). Ability-0 fodder is untouched (it may still hold a Death Star
    site for the flip/superlaser); every other objective, every non-Death-Star site, and the flipped
    back side run the normal §A/§B/§C/§D scoring. -900 sits clearly below a drainable battleground's
    net (~+600-800 = §A win +500 plus §B), so ability drivers route to drainable sites; it is not an
    absolute block. No clean engine "drains canceled here" query exists, so we key on objective+site.
    Verified: compiles clean (mvn -pl gemp-swccg-server -am compile, EXIT=0). NEW V-tag (V187 = the
    other K-2's concurrent work). PENDING, not done: NOT deployed (Steve is holding reload-ai to test
    a different deck first), then a live Set Your Course For Alderaan game (grep nohup.out for
    "V188 ALDERAAN DEATH-STAR").

  ════ V156 UPDATE (2026-06-25): smart solo-deploy hold (ability/weapon/battleground aware) ════

  V156 UPDATE — SOLO CHARACTERS AT BATTLEGROUND SITES NEED ABILITY 6+, OR 5 WITH A WEAPON (backfilled 2026-07-01)
    Source: CharacterDeploySiteEvaluator.java (~462, logging ~517/~522), shared common/ -> both bots.
    Supersedes the original V156 flat -300 turn<=2 solo guard (old code commented out in place).
    Steve (after Ozzel died solo turn 1): solo characters at battlegrounds are free kills unless
    they can defend themselves. Rules: battleground SITES only (not systems; ability-2 ships are
    often fine), CHARACTERS only, turns 1-2. Solo OK when ability >= 6, or ability >= 5 with a
    matching weapon in hand (the weapon's own getMatchingCharacterFilter() accepts the deployer —
    no hardcoded names). Else "V156 SOLO HOLD" -600; "V156 SOLO OK" +250 when a buddy can join.
    Objective flip-relevant sites are EXEMPT (seeding must stay legal) UNLESS the flip is not
    mechanically ready (v156FlipNotReady — Verge of Greatness: Death Star not yet orbiting Scarif,
    so a turn-1 solo seed is still a free kill). Added isObjectiveRelevantSite to computeTeamViability.
    Boundary: -600 keeps a lone weak body below the SectionA +500 uncontested reward without
    hard-blocking; +250 cannot flip a contested -1500 site positive.
    Status: committed (rode into d72ced949). Verified live ("V156 SOLO HOLD" fires in the log).

  ════ FIX #2 (2026-06-25): V136 SectionA -1500 ability penalty gated to contested sites ════

  FIX #2 — FODDER DEPLOYS AT UNCONTESTED SITES (backfilled 2026-07-01; full entry in AI_CHANGELOG.md)
    Source: CharacterDeploySiteEvaluator.java (~360-365), shared common/ -> both bots.
    A droid swarm has zero-ability bodies, so SectionA -1500'd EVERY ground deploy, contested or
    not — Rando couldn't build presence or seed the Invasion objective's Throne Room. Fix: the
    -1500 "almost never deploy a sub-ability-4 group" gate now requires oppPower > 0 (contested).
    Uncontested low-ability fodder falls through to V156, then +500 — it deploys to drain and seed.
    Boundary: contested sub-4 unchanged (-1500); ability >= 4 unchanged; V151/V181 contested-commit
    logic unreachable by sub-4 bodies. Status: committed (4f8ec16d3), live.

  ════ CANCEL-LOOP FIX (2026-06-25): turn-scope the decision-block maps ════

  CANCEL-LOOP — RANDO STOPPED DEPLOYING AFTER TURN 2 (backfilled 2026-07-01; full entry in AI_CHANGELOG.md)
    Source: DecisionTracker.java, BOTH rando and chosenone, write sites ~302 and ~443.
    Root cause: 3 cancels wrote the blocked deploy slot into the PERMANENT blockedResponses map
    (clears only at game start; mid-game onPhaseChange resets the WRONG tracker instance). The key
    is a POSITIONAL action ID the engine REUSES, so a slot blocked by an unplaceable droid on turn
    4 vetoed legitimate group deploys on turn 5; by turn 5 every Deploy action was -9999'd.
    Fix: both permanent writes redirected to the TURN-SCOPED turnBlockedActions (cleared every turn
    in updateState; getBlockedResponses() returns the union, so within-turn loop-breaking intact).
    Known residue: within a SINGLE turn the positional-ID collision survives; real cure is keying
    on blueprint/title (follow-up, not shipped). Status: committed (d1e2aa890), live, Steve
    confirmed it helped.

  ════ V186 (2026-06-23) ════

  V186 — I WANT THAT MAP STARTING SETUP (Starkiller Base system + The First Order Was Just The Beginning)
    Source: CardSelectionEvaluator.java (evaluateDeployLocation + evaluateUnknown) and
    ObjectiveAnalyzer.java (parseFlipCondition); rando only, chosenone mirror pending.
    Steve's report: on the dark objective "I Want That Map / And Now You'll Give It
    To Me" (208_57) Rando set up wrong. The objective deploys Tuanul Village, "any
    other [Episode VII] location," and I Will Finish What You Started, then flips when
    First Order characters control two battlegrounds with no Resistance Agent at a
    battleground site. Correct picks (Steve's strategy, confirmed against card text;
    the handoff/strategy docs are silent on this objective): Starkiller Base SYSTEM
    (208_51) as the other location, because its once-per-turn [download] fetches the
    SB battleground sites that build the two battlegrounds the flip needs; and The
    First Order Was Just The Beginning (214_12) as the starting Effect, which downloads
    Jakku/Kijimi battlegrounds (a second flip-feeder) and is immune to Alter as the
    starting interrupt You Know What I've Come For (208_46) requires.
    Root cause: the live ObjectiveAnalyzer is text-driven and the flip text names no
    card; the Starkiller SYSTEM has NO battleground icon, so a generic battleground
    heuristic misses it. The dead ObjectiveHandler.OBJECTIVE_REQUIREMENTS map has a
    208_57 entry but is never consulted anywhere in src/ (verified codebase-wide), so
    editing it is a no-op.
    Fix, three blocks, all gated to the objective title containing "i want that map":
    1. evaluateDeployLocation (the location fix). The turn-0 "Choose [Episode VII]
       location to deploy" is an ArbitraryCardsSelectionDecision whose cardIds are
       temp IDs ("temp0"...). The candidate loop resolves via findCardById(parseInt),
       which THROWS on temp IDs before any scoring, so every candidate kept only the
       +50 base and the pick was RANDOM (the actual bug Steve saw). Temp-safe block:
       resolve the blueprint from the index-parallel context.getBlueprints() list and
       add +400 to the Starkiller Base SYSTEM (208_51). Also gated to temp IDs so later
       real-id "deploy where" picks are not steered onto the system over its sites.
       System scores 450 vs 50 for every other candidate.
    2. evaluateUnknown starting-effect (the effect fix). +1000 (V80 magnitude) to
       "the first order was just the beginning"; it scores ~1100 vs ~100 for the next
       immune-to-Alter Effect.
    3. ObjectiveAnalyzer.parseFlipCondition (supporting). addLocationFragment
       ("starkiller base") gives Starkiller locations the +150 objective bonus on the
       LATER real-id deploy/move/protection paths (it is inert for the temp-id turn-0
       pick, which block 1 handles), and marks "the first order was just the beginning"
       required/pullable for loss/forfeit protection.
    Verified: compiles clean (mvn -pl gemp-swccg-server -am compile, BUILD SUCCESS) and
    two adversarial workflow passes. The first pass caught that an analyzer-fragment-only
    approach was inert (the parseInt throw) and that +150 would have lost the system-vs-
    site tie by +30; the corrected +400 evaluator approach was re-verified for fires-end-
    to-end, magnitude, and regression. setTestingTexts is never called, so the blueprint-
    id match (208_51) carries the fix.
    PENDING, not done: the live in-game run, since the running jar is old code until
    rebuilt. Grep nohup.out for "V186 STARKILLER SYSTEM (+400)" and "V186 PREFERRED
    START (+1000)" in a real I Want That Map game before calling it done.

  ════ V185 (2026-06-23) ════

  V185 — WEAPON-DEPLOYABILITY GATE (do not pull a weapon no one can hold)
    Source: DeckOracle.java + DeployEvaluator.java (rando; chosenone mirror pending)
    Steve's report (this lost Rando a game): A Good Friend (225_37) deploys one of a
    location, an epic event, or Leia's Lightsaber. Once the location and epic event are
    down only the lightsaber remains, and a weapon attaches only to the specific
    characters its own filter accepts; with no such character on the table the deploy
    has no legal target, so the pull FAILS, wasting the action and revealing/reshuffling
    the Reserve. V67h only verified that a pull target is IN the Reserve, not that it can
    actually DEPLOY.
    Fix: DeckOracle.reserveTargetsAreAllUnattachableWeapons plus hasInPlayCharacterAccepting.
    For each parsed pull-target still in Reserve, a non-weapon does not block; a weapon
    reads its OWN getMatchingCharacterFilter() and tests whether any in-play friendly
    character satisfies it. A weapon whose filter is Filters.none (deploys via game text)
    is deferred, not blocked. DeployEvaluator's V67h WILL_SUCCEED branch blocks the pull
    (-2000) only when EVERY remaining Reserve target is a weapon with no in-play holder.
    Universal: reads each weapon's own filter, no hardcoded names (Leia's Lightsaber
    wants Leia/Ben Solo/Rey ability > 4; Anakin's wants Skywalker ability > 3; F-11D
    Blaster Rifle uses game-text targets and is deferred).
    Verified: compiles clean plus a 6-agent traced/adversarial review (verdict GO).
    PENDING: the live in-game run (jar is old until rebuilt) and the chosenone mirror.
    (Recorded here 2026-06-23 to keep the history continuous; full detail in AI_CHANGELOG.md.)

  ════ V184 (2026-06) ════

  V184 — FIRE "WHEN DEPLOYED" FREE-VALUE TRIGGERS (Han's reveal-and-take)
    Source: ActionTextEvaluator.java (rando + chosenone)
    Replay xc19a289odmogph5: Rando deployed Han Solo, Optimistic General — "When
    deployed, may reveal the top two cards of your Reserve Deck; take one into hand."
    The trigger was offered as 'Reveal top two cards of Reserve Deck' and scored
    NOTHING, so Pass (6.0) won and the free card was thrown away. These triggers are
    "OF Reserve Deck" / "retrieve Force" — not "from Reserve Deck" pulls — so V116 /
    V60 / V97 all miss them, and they fall through to 0.
    Steve's rule: when a deployed card offers an optional "when deployed, may ..."
    trigger that grants free value, fire it, don't pass.
    Fix: score actions whose text is a free-value trigger above Pass (+300), GATED on
    the value actually existing:
      - reveal / look at + (reserve / top two / top card / top of) -> +300 if Reserve
        Deck is non-empty (the Han case).
      - retrieve + force (no "use" cost) -> +300 if Lost Pile is non-empty.
    Dead triggers (empty Reserve / empty Lost Pile) are never fired.
    Verified self-play (1 Rey vs DARK DEAL — the deck with Han): fired 12x; Han's
    'Reveal top two cards of Reserve Deck' now scores 300 (was 0, beats Pass 6);
    'Retrieve 1 Force' +300. No exceptions, games completed, parity-verified.
    WATCH: the retrieve pattern matches "retrieve Force" broadly (text can't tell a
    when-deployed trigger from a standalone retrieve interrupt), so it slightly
    overrides the mild "Low lost pile - save retrieve" (-30) preference -> net +270,
    taken. Retrieving Force is generally good; narrow the gate (raise the Lost-Pile
    threshold) if Rando burns a retrieve it should have saved as fodder.

  ════ V183 (2026-06) ════

  V183 — DECK ORACLE RETOOL: resolve dead-searches by deck TITLE + ZONE
    Source: DeckOracle.java + ActionTextEvaluator.java (rando + chosenone)
    Replay lj093gipnvjxs9py (Steve): Rando played Fall Of The Legend turn 1 to
    "Search your Reserve Deck, take one Weather Vane into hand" — but Weather Vane
    was already in his hand. The search failed and revealed his Reserve. The V177
    dead-search gate didn't catch it: its position parser only reads "[download] X"
    and "X from Reserve Deck"; Fall Of The Legend names its target inside "take one
    Weather Vane into hand" — no parseable slot — so the parser returned nothing and
    nothing was blocked.
    Retool (Steve's design): stop parsing verbs. The oracle already catalogs every
    TITLE in the deck with its category and live zone — so resolve the target by
    scanning the source card's game text for our OWN deck titles (>=6 chars, source
    card excluded), then judge by the matched card's real ZONE. If every named target
    is out of the Reserve (in hand / play / lost), the search is dead — block it.
    GATE: fires ONLY when the position parser found NOTHING (v177Targets empty). If
    the parser produced tokens, the pull has a parseable (often MULTI-target) clause
    we must not second-guess. (Caught in testing: a first cut that ran whenever the
    parser found no LIVE target falsely flagged multi-target pulls — the parser-silent
    gate is the safe boundary.)
    New oracle method: namedDeckCardsInText(text, excludeBpId) → the catalogued
    DeckCards whose title appears in the text.
    Verified self-play (1 Rey vs DARK DEAL, the actual decks): V183 blocked the
    Weather Vane dead-search 192x of ~196 (~98%; 4 leaked on oracle-refresh timing) —
    and correctly blocked Pray I Don't Alter It Any Further's Bespin search 128x
    (Bespin out of Reserve and the only one of its four targets in that deck). No
    false-blocks, no regression (V60/V97/V67h/V177 all firing, 6277 reserve actions,
    0 exceptions), games completed, parity-verified.
    Follow-ups: (a) ~4 Weather Vane leaks from oracle refresh timing at a specific
    decision; (b) the deploy-priority V179 still uses a location-keyword list rather
    than the oracle's category resolution (cosmetic — V179 works).

  ════ V182 (2026-06) ════

  V182 — OFFENSIVE FORCE-BANKING (save force for a bigger army next turn)
    Source: DrawEvaluator.java (rando + chosenone)
    Steve's report: Rando almost never leaves force in the pile during the draw
    phase, so "save force for a bigger army next turn" (the companion to V181) never
    actually happened. Root cause (verified): V58 DRAW-DOWN pays +80 per surplus
    card (cap +400) to draw the pile down to the reserve target, and that reserve
    target (calculateForceToReserve) only counts DEFENSIVE needs (DTF, First Strike,
    contested, maintenance, Secret Plans). There was NO offensive "I'm assembling an
    army" term, so the saved force got converted straight into hand cards — army in
    hand, empty fuel tank.
    Steve's bottleneck rule: the shortage decides the action.
      - Enough CHARACTERS in hand to win a fight we're losing, but short on FORCE
        → bank the force (stop drawing it away).
      - NOT enough characters → draw to find more (the existing default).
    computeOffensiveBank scans contested/relevant sites (battleground or opp drains)
    where the opponent out-powers us; greedily covers the gap with our strongest
    hand characters; if their combined power wins it but we can't afford the deploy
    this turn AND could within ~2 turns of banking, returns the army's cost. When set
    and forcePile < that cost (and handSize >= 4 so we never strand ourselves card-
    starved), V182 suppresses the draw (-300, early return skips the DRAW-DOWN bonus)
    so PASS wins and the force banks. Self-resolving: once forcePile reaches the cost
    the deploy fires and the trigger clears.
    Verified self-play (LUKE SAGA TATOOINE vs DARK DEAL): fired 24x with correct
    intent ("need 10 force, have 2 — hold it"), and RESOLVED — bank → accumulate →
    multi-character army deploy → battle. V182 games had MORE combat than the V181
    run (9 battles vs 3, 45 char deploys vs 36), normal drawing intact (V58 DRAW-DOWN
    still 109x), no exceptions, no draw-stall (872 draws, games completed), parity-
    verified. WATCH: V182 games ran longer (25/39 turns vs 10/14) — fewer, bigger
    commitments; if real games drag, cap the army cost V182 will bank for.
    Completes the V181 pair: V181 takes the fight winnable NOW; V182 banks fuel for
    the fight winnable NEXT turn.

  ════ V181 (2026-06) ════

  V181 — DRAIN-WEIGHTED FAIR-FIGHT COMMIT (take the close fight when the drain pays)
    Source: CharacterDeploySiteEvaluator.computeTeamViability (SHARED, both bots)
    Steve's concept: raw power decides who WINS a battle, but a small power gap means
    LOW attrition either way — the rest is battle destiny (we draw it at ability >= 4),
    and the extra body is forfeit fodder (pay attrition with a cheap card, not a key
    one) plus a weapon carrier. So a "within 3 power" contested fight is a coin-flip
    with small stakes, not a loss. The strict power gate over-vetoed it (Luke 6 vs
    Kylo 10, empty hand -> projection stalls at 6, gap 4 -> -2000, Rando passed and
    ceded the drain).
    The drain does the weighing (Steve): a coin-flip is only worth it if what we deny
    is worth more than the "little extra" we lose on a bad flip. Drain 1 = juice <
    squeeze, let them have it. Drain 3 = hemorrhaging, must act.
    Rule (fires inside the V151/V177 projection gate, AFTER the +400 clean-projection
    win, only when contested oppPower>0 + ability>=4):
      gap = oppPower - projectedPower
      if gap in 1..3 AND drain(site) >= 2 AND forfeit-OK:
          return min(300, drain*100)        # drain2 -> +200, drain3+ -> +300
    forfeit-OK = a one-sided cap: ourForfeit <= theirForfeit*1.25 (favorable AND even
    trades commit; only a clearly-worse trade — we'd forfeit >25% more value — holds).
    §2A boundary: the bonus (200-300) sits strictly BELOW the +400 coordinated-attack
    and +500 clean-win tiers (never steals a real win) and strictly ABOVE PASS (a
    worth-it fight beats ceding the drain). Mutually exclusive with the turn<=2 V156
    solo-protection (that's the uncontested oppPower==0 case), so nothing it guards is
    bypassed. The drain read reuses mq.getForceDrainAmount (same call as V166).
    Verified self-play (LUKE SAGA TATOOINE vs DARK DEAL): "V181 FAIR-FIGHT COMMIT:
    Lord Maul With Lightsaber -> Cloud City: Carbonite Chamber gap=3 drain=2
    ourForfeit=7 theirForfeit=16 -> +200" — armed Maul, out-powered by 3, favorable
    forfeit, commits to contest the drain. No exceptions; clean-win tiers still fire
    209x (not dominated). Shared file — no rando/chosenone mirror.
    FOUND-AND-FIXED in verification: an earlier symmetric ±25% parity window wrongly
    held the FAVORABLE trades (our 7 forfeit vs their 12 — we lose LESS). Corrected to
    the one-sided cap above. (Companion follow-up still open: draw-phase offensive
    force-banking, so "can't win now -> save force for a bigger army next turn"
    actually banks fuel instead of the V58 DRAW-DOWN converting it all to hand cards.)

  ════ V180 (2026-06) ════

  V180 — WIELDER DETECTION BY PERSONA, NOT TITLE (Luke fought bare-handed)
    Source: ActionTextEvaluator.java (rando + chosenone), V158 NO-WIELDER branch
    Replay aab2jiaa5sca (E1): Rando's deck pulls Luke's Lightsaber from Reserve via
    "Like My Father Before Me". The V158 NO-WIELDER guard blocks weapon pulls when
    the wielder isn't on table (good — avoids a saber bleeding out in hand). But it
    checked pc.getTitle().contains("luke") to find the wielder. Young Skywalker IS
    Luke (Persona.LUKE) but his TITLE has no "luke" substring — so V158 decided
    "luke not on table" and hard-blocked the pull (-9999) TWELVE times in one game.
    Luke fought bare-handed the entire game; no Rando lightsaber ever deployed.
    Exactly the senator-keyword lesson (feedback_senator_in_lore_not_keyword):
    identity lives in the persona set, not always the printed name.
    Fix: after the title check, also scan the character's getPersonas() — if any
    persona name (lowercased) contains the extracted wielder word, the wielder IS
    on table. Mirrors the persona scan already in CardSelectionEvaluator (~line
    8848). Verified self-play: NO-WIELDER blocks 12 -> 0, and "Luke's Lightsaber
    (WEAPON, owner Rando_Cal) enters play" — Rando now arms Luke (and Rey). V66
    still correctly skips the duplicate reserve-pull; the hand saber deploys
    instead. Parity-verified. Closes the last of the 5 last-game errors (E1-E5):
    E2/E4 = V177, E1-fodder = V178, E3 = V179, E1-block = V180; E5 was a
    false alarm (dead-retrieve guard already present).

  ════ V179 (2026-06) ════

  V179 — DPS LOCATION-KEYWORD PARITY (farm/planet pulls never deployed)
    Source: DeployPhaseScript.java (rando + chosenone), classifier vs V67ai
    Replay aab2jiaa5sca (E3): "I Must Be Allowed To Speak" lets Rando deploy
    Tatooine: Lars' Moisture Farm (a free drain site) from Reserve Deck. The action
    "Deploy a farm from Reserve Deck" scored 2050 in DeployEvaluator (V67ai gives
    location-from-reserve pulls +1800) — but it NEVER deployed. Root cause: the DPS
    WALK (V67bc) state machine, which overrides raw scoring, classifies each action
    into a Step bucket and walks LOCATIONS → KEY_CHARACTERS → ... The farm action's
    parsed pull-target token is 'farm'; DeployPhaseScript's location-keyword lists
    (stepForPullTargetText + classifyByKeywords) did NOT contain 'farm' (only "site,
    system, location, hallway, battleground, sector"). So the farm fell through to
    no bucket, no LOCATIONS bucket was built, and the walk picked KEY_CHARACTERS
    (Young Skywalker) every turn. The scorer ranked the farm +1800; the walk dropped
    it. Classic scorer/walk disagreement.
    Fix: give DeployPhaseScript the SAME location-keyword set V67ai uses (farm,
    cantina, planet names, docking bay, spaceport, city, palace, temple, village,
    outpost, ...), factored into one private namesLocation() used by both classifier
    paths. List MUST stay in sync with V67ai (DeployEvaluator ~line 3275) — comment
    cross-references it. This realizes the long-documented rule "pull/deploy a
    LOCATION from Reserve BEFORE any character in the same deploy phase" (the
    feedback_location_pull_before_character_deploy V100 candidate); the DPS STRICT
    ORDER already declared LOCATIONS as step 1 — the farm just never reached it.
    Verified self-play (LUKE SAGA TATOOINE vs DARK DEAL): "DPS WALK step=LOCATIONS
    picking 'Deploy a farm from Reserve Deck' 4100.0" → "Tatooine: Lars' Moisture
    Farm (type: LOCATION) enters play". Rando won. Parity-verified.

  ════ V178 (2026-06) ════

  V178 — ARMED CHARACTERS SLIGHTLY FORFEIT-PROTECTED (-10 tiebreaker)
    Source: CardSelectionEvaluator.java v159ForfeitScore (rando + chosenone)
    Lightsabers kept dying with their carriers (Tyranus/Sidious forfeits) — each
    lost saber costs the drain bonus + hit potential until re-pulled. Forensics
    showed the drain-add itself is perfect (914/914 taken when offered); the gap
    was sabers being in the lost pile. Steve: "maybe just a +10 weight though.
    Weapons are worth something but not everything."
    Fix: a character with an attached WEAPON gets -10 on its forfeit score in
    the two normal-path returns (attrition coverage + pure damage). Pure
    tiebreaker: prefers forfeiting the unarmed body when otherwise equal; never
    overrides forfeit value / hit / immunity factors (magnitudes 60-1500).
    Hit/dead branches unchanged (a hit carrier still forfeits — forfeit 0).

  ════ V178 (2026-06) ════

  V178 — PROTECT WEAPONS THAT HAVE A WIELDER (Luke's Lightsaber as fodder)
    Source: CardSelectionEvaluator.java (rando + chosenone), V153 force-loss order
    Replay aab2jiaa5sca: Luke's Lightsaber was lost from hand as force fodder (V153
    scored it 650, top pick) while Young Skywalker (its Luke-persona wielder) was in
    play — the deck's signature weapon thrown away, so Luke fought bare-handed all
    game. V175 protects battle INTERRUPTS from the fodder pile but not weapons.
    V178 extends it: a WEAPON in hand whose wielder exists (any non-undercover
    friendly character on table, or a character in hand to deploy it onto) gets -450
    on the loss-zone score in the protect tier (>=4 life force): 600 -> 150, lost
    near-last like a character. Turn-gated > 3 (V175a logic: turns 1-3 the deck is
    dense, prefer losing the known weapon over a blind reserve hit). Survival tier
    (<4) and duplicates unchanged. Verified self-play: fired 10x, "Jedi Lightsaber
    600 -> 150" so it stops being top fodder. Parity-verified.
    (Companion to V177's E2 fix: V177 gets the wielder deployed and the gear counted;
    V178 keeps the weapon alive to be deployed. E3 — deploying the farm from I Must
    Be Allowed To Speak — still open.)

  ════ V177 (2026-06) ════

  V177 — DECK ORACLE DEAD-SEARCH GATE (a real player never searches for what he
         knows isn't in his deck)
    Source: ActionTextEvaluator.java (rando + chosenone), gate before the V116
            reserve floor; uses DeckOracle.parseSourceCardPullTargets +
            hasTargetInZone(RESERVE_DECK, ...)
    Replay j6tf75kwbfh83lxo (Steve vs Rando, Rey deck): Rando fired Luke
    Skywalker, The Last Jedi's "take Force Projection into hand from Reserve
    Deck" 4+ times in one game. Force Projection was never in the deck. Each
    attempt wasted the action, revealed the Reserve Deck to Steve, and
    reshuffled. Steve: "Deck Oracle should have known that no such card title
    exists in his deck... A real player would know all the cards in his deck and
    would never search his reserve deck for a card he knew was not in it."
    Root cause: the knowledge already existed — DeckOracle catalogs the FULL
    deck by zone at game start (analyze()) and tracks every card's current zone
    (refresh()) — but the action-scoring layer never consulted it for game-text
    searches. V95 used it for interrupts-as-fodder, V155 for one specific card;
    nothing covered character/effect/objective game-text pulls. Worse, V116's
    +100 reserve floor actively BOOSTED dead searches.
    Fix: before any action whose text says "from Reserve Deck" / "[download]",
    parse the SOURCE card's game text for pull targets and classify each:
      ALIVE — matches a Reserve Deck card (strict matcher, or V177a word-rescue:
              any >=6-char word of the target matches a reserve title — keeps
              "lightsaber on rey" alive while sabers remain);
      JUNK  — parser garbage (>25 chars or contains digits, e.g. "3 force to
              take one effect of any kind") — an untrusted parse NEVER blocks;
      DEAD  — clean title-like string matching nothing in reserve.
    Block (-2000, skip all further scoring incl. V116) ONLY when: no ALIVE, at
    least one DEAD, no JUNK. Multi-target pulls live if ANY target remains.
    V177a (same session): the first cut blocked on any all-miss parse — live
    pulls with junk targets ("Take Effect into hand", Blockade Flagship site
    deploys) were false-blocked; the ALIVE/JUNK/DEAD classification fixed it.
    Verified (1-game self-play, Rey vs Dooku): Force Projection searches in the
    replay: 0 (was 4+), 176 evaluations blocked; the long-standing Petranaki
    Arena dead pull blocked 104x; Mara-already-in-hand pull blocked; 197
    junk-parse pass-throughs ("Take Effect into hand" fired 85x again); Evil Is
    Everywhere stayed alive while lightsabers remained in reserve.
    Supersedes (for parseable pulls) the old "fire every turn, stop after 2
    consecutive failures" heuristic — knowledge beats retry-counting.
    UPDATE (2026-07-01): CATEGORY RESCUE — detection-path alignment with V67h.
    TDIGWATT loss 2026-07-02 02:09 UTC (replay 7co2xviwqo5q3zac): I'm Sorry
    (V) may [download] an interior Cloud City
    site; Dining Room (V) + Security Tower (V) sat in Reserve ALL GAME, yet
    V177 blocked the download 19 times ("nothing in Reserve") while V67h's
    validatePullFromSourceCard said WILL_SUCCEED in the SAME evaluations
    (V82.1: "site" → CardCategory.LOCATION present in RESERVE_DECK). Root
    cause: the raw hasTargetInZone title matcher cannot match type-phrases
    ("interior cloud city site") against titles; the ≥6-char word-rescue only
    tries "interior", which no title contains. The dumber detector ran first
    and won — flip + Force income dead all game, game lost. Same false
    negative hit Piett's "[any commander]" search 27x on T4.
    Fix (in place, both bots): inside the block branch, consult
    validatePullFromSourceCard(RESERVE_DECK, sourceGameText) BEFORE applying
    -2000. WILL_SUCCEED → no block (logged "V177 CATEGORY RESCUE"); anything
    else → the block stands. Genuine dead searches (proper-noun targets) still
    fail the validator and still block. Accepted false positive: the category
    fallback ignores qualifiers, so any location left in Reserve un-blocks an
    interior-CC search whose real targets are gone — one no-op action per
    turn, bounded (and recordFailedPull, the 2-strikes backstop, has ZERO
    callers — dead wiring — so it repeats; separate fix if ever needed).
    LIVE-VERIFIED 2026-07-02: 2 self-play games, rescue fired 3x, zero false
    blocks, I'm Sorry downloaded an interior CC site turn 1 in both games
    (replays 94d9142hqg0zs0fr, x5cbzmegdjsx24zv).

  ════ V177 (2026-06) ════

  V177 — WINNABLE CONTESTS, NOT TIMID ONES (Luke vs Kylo) + survivability gate
    Source: CardSelectionEvaluator.java (rando + chosenone) +
            CharacterDeploySiteEvaluator.java (shared, both bots)
    Replay aab2jiaa5sca (Steve dark/Kylo vs Rando light/Luke). Two related errors:
    E2 — Rando left Luke (Young Skywalker, power 6) + Luke's Bionic Hand + Threepio +
    a lightsaber in hand instead of overpowering Kylo (power 10) at his site. Steve:
    "He could have overpowered Kylo for sure with those cards." Two layers each
    talked him out of it:
      (a) The V172/V173 winnability gate's reserve over-counted: force=10 but
          reserved=12 (full table maintenance + interrupt + battle fee), so the wave
          budget was 0 — no buddies projected, Luke gated as a 6-vs-10 loss.
      (b) The V136 §A team-viability score (CharacterDeploySiteEvaluator) returned
          its -2000 "ability passes but power+body fail" cap, because its V151
          co-deploy lookahead projects only CHARACTER power — it never counted Luke's
          Bionic Hand (a power-boosting Device) or the lightsaber, so projected power
          stayed at 6 < 10.
    E4 — Wild Karrde (ship) deployed to Tatooine to "contest a drain" then hyperspeed-
    moved to Jakku next phase, wasting 1 Force, because V166 CONTEST DRAIN (deploy)
    awarded its bonus with no survivability check.
    Fixes:
      1. RESERVE CAP (v173WaveProjection): reserved = min(reserved, forcePile -
         thisCost - 3) so upkeep reserves never starve the wave to zero. Maintenance
         is already handled at deploy-score time (V59/V64) — double-counting it in the
         winnability projection was the lockout. Full reserves still hold on surplus.
      2. V151 GEAR (CharacterDeploySiteEvaluator co-deploy lookahead): after projecting
         affordable character reinforcements, also project affordable weapons/devices in
         hand (device +2, lightsaber +3, other weapon +2) so an armed strike group is
         seen as winnable and §A returns the +400 coordinated-attack score instead of
         the -2000 cap.
      3. V166 SURVIVABILITY GATE (deploy path): only award the contest-drain bonus when
         ourPower + thisCard + affordable wave >= theirPower - 2; otherwise skip it so
         a winnable/safe site wins the deploy (no lure-then-relocate force waste).
    Verified self-play: V177 V166 GATED fired (E4 suppression working), contest +
    aggression stack still operative (V166 3x, V171 6x, V172 27x), both games
    completed, no new loops. Exact Luke-gear scenario needs uneven stacks (live games)
    to bind. Parity-verified.
    Still open from this game: E1 (Luke's Lightsaber lost as force fodder — weapons
    not protected like interrupts) and E3 (Lars' Moisture Farm not deployed from
    I Must Be Allowed To Speak, lost as fodder). E5 (dead Secret Plans retrieve) was a
    FALSE ALARM — guard already exists, and Secret Plans is the human's defensive shield.

  ════ V176 (2026-06) ════

  V176 — SAVE THE BATTLE-INITIATION FORCE (don't go broke before the fight)
    Source: CardSelectionEvaluator.java (wave reserve) + DeployEvaluator.java
            (deploy brake) (rando + chosenone)
    Replay c8o8f5pnjp5244ao (Steve vs Rando), turn 5: Steve's Yoda stood SOLO at
    Hoth: Defensive Perimeter (3rd Marker). Rando deployed Tyranus + Dr. Evazan &
    Ponda Baba + Dooku's Lightsaber straight onto him (V171 working) — and then
    never battled. Log: the engine offered ONLY the K&D shield in the battle
    phase; "Initiate battle" was absent and BattleEvaluator saw 0 battle actions.
    Cause: the deploys spent the LAST force (forcePile=0) and battle initiation
    costs 1 Force — the engine could not legally offer the battle. Steve
    reinforced with Han + Lando next turn and the free kill became a 4-character
    brawl (Rando still won it, but ate risk it never needed).
    Fix, two parts:
      (a) v173WaveProjection reserve += 1 (battle-initiation fee): a wave that
          exists to fight must keep the fee to start the fight. Affects both
          V172 gates (slightly more conservative wave estimates).
      (b) DeployEvaluator umbrella brake: when a WINNABLE battle is already on
          the table (we are present and out-power the opponent at a shared
          location) and force pile <= 2, further 'Deploy' actions get -800 —
          stop shopping, keep the battle money. Battle phase follows deploy
          immediately, so the saved force converts directly into the initiation.
    Verified self-play: 40 fires including at Hoth 3rd Marker itself ("winnable
    battle waiting (6 vs 5), pile=0 -> -800"); battles balanced 3-3 in both
    sanity games; no regressions.

  ════ V175 (2026-06) ════

  V175 — OFFENSIVE BATTLE INTERRUPTS (kill shots, substitute deltas, fodder protection)
    Source: ActionTextEvaluator.java + CardSelectionEvaluator.java (rando + chosenone)
    Steve (ROTS Dooku games): "Rando almost never uses interrupts that help him
    during battles... Welcome Home, Lord Tyranus to use Dooku's ability as a destiny
    draw... using Sniper after he's hit a character... he almost never uses
    interrupts offensively during battle." Log forensics found three stacked causes:
      1. KILL SHOTS UNSCORED: the engine offered FIVE "Make <Steve's character>
         lost" actions (Make Yoda / Rey / Ben Solo / Anakin / Han lost — the
         Sniper / Dark Strike / 'hit'-follow-up class) and ALL FIVE scored 0.0
         "Unknown action type", losing to Pass (7-16). Five kill-shots declined.
      2. FODDER BURN: the force-loss picker repeatedly chose "Lose Welcome Home,
         Lord Tyranus" (it ranked as hand junk, 600); the Sniper copies cycled
         reserve -> force pile -> spent as payments / lost. The tricks died before
         any battle could use them.
      3. The recognized pattern worked: "Substitute destiny" was taken 11/11 when
         offered (+30) — the endpoint was fine, the pipeline starved it.
    Fixes:
      (a) KILL SHOT (ActionTextEvaluator): new branch for "Make <X> lost" — parse
          the target, find it on table, check ownership. OPPONENT character:
          +400 + power*40 + forfeit*20 (cap 900) — dominates Pass. OUR character
          (self-target/sacrifice windows exist): -100 so Pass wins. Unknown: 0.
      (b) SUBSTITUTE DELTA (ActionTextEvaluator): "Substitute destiny" now scores
          (our best ability in the battle - the just-drawn destiny value) * 60,
          via getTopOfUnresolvedDestinyDraws (printed destiny of the drawn card)
          and getBattleLocation. Drawn 1 with Tyranus(7) in battle -> +360; drawn
          6 -> SKIP (-50, save the card). Falls back to the old flat +30 when
          either value is unreadable.
      (c) PROTECT BATTLE INTERRUPTS (CardSelectionEvaluator, V153 order): hand
          interrupts whose game text is battle-relevant (battle destiny / during
          battle / 'hit' / substitute / power +) get -450 on the loss-zone score in
          the protect tier (>=4 life force): 600 -> 150, i.e. lost near-last, right
          above HAND CHARACTERS (100), below Reserve (400). Survival tier (<4)
          unchanged (dump hand to live). Duplicates are still fodder.
    Verified self-play: protect fired 86x ("Lose Welcome Home" dropped to 200 from
    950+; Houjix, Sorry About The Mess, Nabrun Leids protected too); kill-shot and
    substitute windows didn't occur in the 2 sanity games (need 'hit' characters /
    destiny draws — common vs Steve, rare in self-play); games completed normally.

    V175a (Steve, 2026-06): TURN-GATED — the fodder protection (part c) starts on
    TURN 4. Steve: "Sometimes it's necessary for us to lose force from hand instead
    of unknown cards from reserves, particularly in the first 1-3 turns. There is a
    much higher chance of losing a crucial card in the first three turns from blind
    reserve loss." Turns 1-3 the deck is still dense with undeployed key cards, so
    a known hand interrupt is the cheaper loss than a blind reserve hit; after turn
    3 the engine is on the table and the in-battle tricks become the scarce
    resource. Gate: context.getTurnNumber() > 3 on the -450 protection. Verified:
    protect fires only at turns 4+ (observed 10-17); turn 1-3 force losses freely
    picked "Lose Force Field" from hand.

  ════ V174 (2026-06) ════

  V174 — WAVE BUDGET RESERVES: MAINTENANCE UPKEEP + BATTLE INTERRUPTS
    Source: CardSelectionEvaluator.java (rando + chosenone) — v173WaveProjection
    Steve: "Having a flat force number is not great. We need to account for saving
    force for maintenance cards on table / in hand to deploy with the army and any
    interrupts that would be useful in battle."
    The V173 wave budget spent the ENTIRE force pile and V171 still carried a
    vestigial flat force>=4 check. Now the budget reserves, off the top, BEFORE
    filling the wave:
      (a) upkeep for our MAINTENANCE-icon cards already on table (maintenance cost
          = deploy cost — the V22.3/V59 rule; they die unpaid),
      (b) the deploying card's own upkeep if IT has the MAINTENANCE icon,
      (c) 1 force if hand holds a battle interrupt, 2 if it holds 2+ (Steve's
          standing force-management rule: "1-2 for interrupts, +maintenance"),
      and maintenance BUDDIES joining the wave consume deploy cost + upkeep
      (double) from the budget.
    The flat force>=4 check is DELETED — replaced by "at least one buddy is
    genuinely affordable after reserves" (the projection's buddiesTaken >= 1).
    Helper now returns {wavePower, buddiesTaken, reservedForce}; reserves are
    logged in every V171/V169/V172 gate line ("reserved=N").
    Verified self-play (Dooku deck = maintenance-heavy): real decisions held
    reserves of 7-12 force; gating became more honest (62 contests rejected vs 13
    pre-V174) while affordable aggression survived (251 contact fires); both games
    fought to normal life-force endings.

  ════ V173 (2026-06) ════

  V173 — WHOLE-HAND WAVE PROJECTION (power + weapons vs force cost) FOR THE V172 GATES
    Source: CardSelectionEvaluator.java (rando + chosenone) — v173WaveProjection helper
    Steve: "does the new logic now account for the whole hand with power and weapons
    weights vs force cost when deciding where to deploy?" — V172's first cut did NOT
    (it counted ONE buddy, printed power only, no weapons, and a flat force>=4 check).
    V173 replaces that estimate in BOTH V172 gates with a real affordable-wave
    projection:
      - budget = force pile - the deploying card's printed deploy cost
      - every OTHER hand character, strongest first, joins the wave if its printed
        deploy cost fits the remaining budget (cheaper characters still join when a
        big hitter doesn't fit)
      - character weapons in hand add weight when budget allows (~1 force each):
        lightsaber +5, other weapons +3 (the V29.7 BattleEvaluator precedent), max 2
      - projection = sum of affordable buddy power + weapon weights
    Gates unchanged in shape, now fed by the full wave:
      V171 contact: ourPowerAt + thisCard + wave >= theirPower - 2
      V169 protect: thisCard + wave >= deficit - 4
    Known approximations (deliberate): printed deploy costs (no location/modifier
    cost adjustments), weapons weighted flat rather than simulating attachment, no
    ability term (V164a handles ability at battle time).
    Verified self-play: wave values ranged 0-11 with hand/budget as expected, 13
    contests gated as unwinnable, 251 V171 fires on winnable ones, games normal.

  ════ V172 (2026-06) ════

  V172 — WINNABILITY GATES ON THE AGGRESSION STACK (stop feeding the kill zone)
    Source: CardSelectionEvaluator.java (rando + chosenone) — gates inside V171 + V169
    Regression caught by Steve over two live games (replays n1xpnvm55tvrykhb +
    gkxmkp5p2xbp9ssc): after V171, Rando was "no challenge at all" — 0 battles
    initiated, under-deployed (17 hand-deploys vs Steve's 29), conceded early both
    games. The concede log told the story: V67aw fired on a lost-pile deficit of
    30-to-0. The aggression stack (V171 +600 contact, V169 +800..1100 protect,
    V166 contest) had no check against the opponent's ACTUAL stack size:
      1. V171 walked characters piecemeal into Steve's superior stacks (the claimed
         "danger terms past -600" guard was wrong — those penalties are flat, not
         scaled to the power gap; "Deploy to Hoth 3rd Marker: 695" won while badly
         outpowered).
      2. Steve initiated every battle (7-0) on his terms and wiped each installment.
      3. V169 PROTECT URGENT then demanded buddies for the now-endangered site
         (416 fires in one game) and fed the next batch into the same kill zone.
      A corpse conveyor. Note the bitter irony: the old "wasteful" deploy-adjacent-
      and-march pattern was accidentally SAFER — it assembled the full stack before
      walking in. V171 removed that discipline without adding a winnability check.
    Fix — one projected-power test, two gates:
      projected = our power at site + this card's power + best remaining hand char
      (a) V171 DEPLOY TO CONTACT only fires when projected >= theirPower - 2
          (near-parity walk-in). Otherwise NO bonus — assemble adjacent and move in
          as a group, which is the correct play when the stack can't be matched yet.
          Gated cases log "V172 CONTACT GATED".
      (b) V169 PROTECT (deploy-buddies) only fires when this card + best remaining
          hand character can bring the site's deficit within 4. Beyond that the
          site is unsavable by deploys — the V169 RETREAT path (move phase) takes
          over instead of feeding. Gated cases log "V172 PROTECT GATED".
    The V169 PROTECT URGENT umbrella (+500, keeps the deploy phase alive) is
    unchanged — it pushes deploys, the gated location chooser just sends them
    somewhere sane. Verified self-play: 7 CONTACT GATED rejections, V171 still
    fired 208x on winnable contests, games completed normally. Real validation is
    Steve's next live game — self-play bots have similar power curves, so the gates
    rarely bind there.

  ════ V171 (2026-06) ════

  V171 — DEPLOY TO CONTACT (don't deploy adjacent and march in)
    Source: CardSelectionEvaluator.java (rando + chosenone), deploy-location loop
    Replay 479h9miow1acggwb (Steve vs Rando): Rando repeatedly deployed
    Tyranus/Asajj/Savage to an EMPTY adjacent site (Guest Quarters / Beldon's) and
    then landspeed-marched them into Steve's occupied site — every cycle, sometimes
    same-turn deploy-then-move. Steve: "deployed and moved guys in front of my
    characters instead of just deploying to my occupied location. This is a waste
    of force. ... He didn't really initiate many battles."
    Root cause (from the actual decision log): the contested site ate FIRST-MOVER
    penalties — V113 SOLO (-300, the deploying character is momentarily "alone"),
    V29.5 BUDDY enemy-occupied (-100), V136 danger (~-300) — that V166's contest
    bonus (+400) couldn't overcome: empty Guest Quarters scored 340, Steve's Lower
    Corridor scored -200. The wave then piled onto the empty site, and the MOVE
    phase (which has no such fear) marched everyone over. Double cost, and worse:
    SWCCG turn order is Deploy -> BATTLE -> Move, so arriving by move forfeits
    battle initiative every time — Steve out-initiated Rando ~7-2.
    Fix: when the destination is opponent-occupied AND a deploy WAVE is coming this
    same phase (>= 2 characters in hand, force pile >= 4 to land a buddy), add +600
    "V171 DEPLOY TO CONTACT" — offsetting the first-mover penalties because the
    loneliness is temporary. Boundary math: observed case -200 + 600 = +400 > 340,
    contested site wins; genuinely suicidal sites carry danger terms past -600 and
    still lose; the wave gate keeps true solo-no-backup deploys penalized. Stacks
    with V166 (+contest) and V169 (+protect) at the same site by design — reinforce
    the endangered contested site is the strongest signal in the system.
    Verified (2-game self-play): 115 fires, contested-site deploys WINNING at
    1990-2950, battle initiation balanced 3-2/2-3 per game (was 7-2 against).

  ════ V170 (2026-06) ════

  V170 — UNDERCOVER SPY: THE CHEAP DRAIN BLOCKER
    Source: RandoCalAi.java / TheChosenOneAi.java (yes/no intercept)
            + CardSelectionEvaluator.java (spy location steering) (rando + chosenone)
    Steve's strategy directive (from the drain-balance design session): "Spies should
    always be a part of the strategy as well. Since spies cost much less to block a
    drain than deploying a bunch of characters to over power opponent."
    Mechanics: a spy with mayDeployAsUndercoverSpy gets a YesNoDecision from the
    engine at deploy time ("Do you want to deploy X as an Undercover spy?"). An
    undercover spy deploys to the OPPONENT's side of a site and breaks their control
    there — their Force drain at that site stops, for the cost of one cheap
    character. The bot previously had NO handler for that prompt (fell to heuristics).
    Part 1 — yes/no intercept (RandoCalAi/TheChosenOneAi, after the V44 revert
    intercept): YES when the opponent currently has ANY active drain to block
    (bonus-aware getForceDrainAmount summed over sites they occupy >= 1); NO when
    there is nothing to block yet — early game the spy stays a normal body with
    power/presence. V67j discipline: the Yes/No indexes are scanned from the results
    array, never assumed.
    Part 2 — spy location steering (evaluateDeployLocation): when the deploying card
    has Keyword.SPY and the destination is an opponent-occupied site with drain >= 1,
    +600 + min(300, drain*75) = +600..+900 — prefers their BIGGEST drain. No power
    requirement (spies don't fight; undercover is safe). Ordering by design:
    beats V166 contest (+250..400) and open-site totals (~315-600) even through a
    V113 solo penalty (-300); stays UNDER V169 PROTECT (+800..1100) — endangered
    allies outrank a cheap block.
    Verified in self-play (5-game Rey-vs-Dooku): 2 actual undercover deployments in
    the replays ("deploys ... as an Undercover spy"), both from the intercept
    answering YES at opponent-drain=4; V170 SPY BLOCK fired 34x; battles held at 18
    (vs 19 prior era, 8 baseline); no freezes, no cap hits; deck split Rey 3 / Dooku 2.

  ════ V169 (2026-06) ════

  V169 — PROTECT ENDANGERED CHARACTERS (reinforce-or-retreat)
    Source: CardSelectionEvaluator.java + DeployEvaluator.java + ActionTextEvaluator.java
            + MoveEvaluator.java (rando + chosenone)
    Replay lk6xgsokjcwrwxuu (Steve vs Rando, 2026-06-10): two fatal moves, same disease —
    NO rule anywhere knew a friendly character was endangered.
    FATAL 1 (Asajj): Rando won the turn-5 Guest Quarters battle but the forfeits gutted
    the site (Grievous + Sidious gone), leaving Asajj. Steve moved Luke INTO Guest
    Quarters. On Rando's turn 6 — 13 Force activated, Mara Jade taken into hand — the
    lone 'Deploy' action scored -140 (V64 maintenance penalty for Ap'lek dominated the
    urgency bonuses), fell below the DPS bad-action threshold (-100), and the WHOLE
    deploy phase was passed. In the move phase, retreat was impossible: V41
    wrong-direction had blocked every safe (empty) destination (-9999), the destination
    step cancelled out, and the cancel-loop guard + V163 hard veto killed the move
    action. Asajj was beaten 6v27 next turn (hit, forfeit reset to 0).
    FATAL 2 (Hoth): Steve's Odin Nesloor transport dropped a 5-character strike team
    (Yoda/Anakin/Han/Leia/Luke) on Hoth: Defensive Perimeter (3rd Marker) on top of
    Tyranus + Aurra Sing. Rando's turn-7 response: deployed Savage Opress + Nute Gunray
    to an OPEN Cloud City site (the engine didn't offer the Hoth site as a deploy
    target, and nothing scored "your allies are outnumbered" anyway). 16v37 next
    battle: Tyranus hit, forfeit reset to 0, Dooku's Lightsaber lost.
    Steve's rule: "deploy buddies to protect his characters" — even into a losing
    battle — or "move [the endangered character] to an adjacent safe site."
    THE FIX (5 coordinated parts, all x2 bots):
    (A) CardSelectionEvaluator.evaluateDeployLocation: v169OppPowerExcessAt(location) =
        opponent power minus our power where we have non-undercover presence. If > 0,
        deploying there gets +800 + min(300, excess*30) = +800..+1100 — dominates
        open-site totals (~315-600) and V166 contest (+250..400); only the -9999 hard
        blocks beat it. Undercover spies are not "presence to protect."
    (B) CardSelectionEvaluator.evaluateMoveDestination: RETREAT MODE — the mover is
        located from the decision-text blueprint hint; if its current site is
        endangered, (i) safe destinations (zero opponent power) get +600, and (ii) the
        V41 wrong-direction -9999 is GATED OFF (a retreat IS a move to an empty site;
        V41's assumption is wrong for endangered movers — it's what trapped Asajj).
        V41 stays fully active for non-endangered movers.
    (C) DeployEvaluator umbrella: when ANY location is endangered, the 'Deploy' action
        gets +500 (V169 PROTECT URGENT) so a maintenance/value penalty can never again
        sink the whole deploy phase below the DPS threshold while allies need help.
    (D) ActionTextEvaluator V163 branch: a blocked MOVE action whose mover is endangered
        is soft-blocked (-400) instead of hard-vetoed (-100000), so the retreat stays
        attemptable once the destination step is fixed by (B). -400 still loses to Pass
        when no safe destination exists (no Keder regression — Keder wasn't endangered,
        non-endangered movers keep the hard veto).
    (E) MoveEvaluator cancel-loop gate: same endangered-mover exemption (-400 soft
        instead of -9999).
    Verified in self-play immediately after deploy: "V169 PROTECT (deploy): Cloud City:
    Guest Quarters outpowered by 10 -> +1100" WON the location choice (1170), and
    "V169 PROTECT URGENT: Hoth: Defensive Perimeter (3rd Marker) (4 vs 8) -> +500" —
    the exact two sites from Steve's game. 12 umbrella fires, 0 loops, games completed.

  ════ V168 (2026-06) ════

  V168 — ALWAYS ACTIVATE FORCE (never pass activation)
    Source: ActionTextEvaluator.java (rando + chosenone)
    Steve (live game vs Rando): "Rando should always activate force and not pass
    activating." Force activation is the bot's entire economy — it pays for every
    deploy, drain, and battle. A guaranteed +5000 on the "Activate Force" action so
    it always beats Pass (and the V167 soft loop-block) whenever it is offered. Once
    the player's max Force is activated, the action stops being offered, so the bot
    still passes legitimately at the end of the Activate phase. Verified: Rando
    activated 48x in a test game (was 0 while stalled), 0 "you have not activated"
    prompts.

  ════ V167 (2026-06) ════

  V167 — NEVER HARD-VETO A PHASE-FUNDAMENTAL ACTION (fix the activate stall)
    Source: ActionTextEvaluator.java + DrawEvaluator.java (rando + chosenone)
    Regression caught when Steve played Rando: Rando beat him in 5 turns, then
    stalled — stopped activating Force and stopped draining despite a full Reserve
    Deck. Root cause: "Activate Force" landed in the cancel-loop blocked set (a
    transient activate-flow loop), and V163's -100000 HARD VETO then killed it
    permanently. Rando passed every Activate phase from ~turn 6 on, never got Force
    into its pile, and could no longer deploy or drain. Before V163 the block was a
    soft -200 that the high-scored Activate action still beat — so the hard veto was
    the regression.
    Fix: the loop-breaker must never make a MANDATORY action impossible. "Activate
    Force" (ActionTextEvaluator) and all draw actions (DrawEvaluator) are now
    soft-blocked (-200) instead of hard-vetoed when they land in the blocked set, so
    a loop is still nudged but the bot can always activate/draw. Tactical targets
    (move/deploy/battle) keep the V163 hard veto. Paired with V168 (which guarantees
    activation outright). Verified: 0 hard-vetoes on Activate Force in the test game.

  ════ V166 (2026-06) ════

  V166 — CONTEST THE OPPONENT'S DRAIN (deploy to break drain stalemates)
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Problem: self-play was a drain stalemate — 8 battles vs 417 drains across 5
    games, 20-38 turn grinds, because both bots deploy to their own uncontested
    sites and drain, never forcing a shared site to battle at. These are
    battle-heavy decks (Rey, Dooku) — they SHOULD fight.
    Steve's rule: evaluate the force-drain BALANCE (bonus-aware — weapon/lightsaber/
    objective/Effect drain bonuses included, not raw icons). When the opponent
    out-drains us by net >= 2, deploy/move to contest their SOFTEST drain site
    (fewest opponent cards = easiest to clear or spy-block).
    Implementation:
      - computeNetDrainBalance(game, gs, playerId): sums getForceDrainAmount (the
        engine's modifier-aware drain) over locations where each side has presence;
        returns oppTotal - ourTotal. (The old icon-based calculateForceDrainGap in
        DeployPhasePlanner missed all the bonuses — this is the bonus-aware fix.)
      - Contest boost when net >= 2 at an opponent drain site, weighted to prefer the
        softest site: deploy path (evaluateDeployLocation) +250..+400, move path
        (evaluateMoveDestination) +200..+350.
    KEY LESSON (two failed attempts before this worked): the boost MUST be in the
    DEPLOY path — deploy can target the opponent's site directly; MOVE only reaches
    adjacent sites, so the move-path version fired 0 times. And the trigger was never
    the problem: a diagnostic showed net >= 2 fires in 51% of deploy decisions (53 of
    104). It was placement + weight, exactly as Steve predicted.
    Verified (5-game Rey-vs-Dooku, V164a+V166): battles 8 -> 19 (avg 1.6 -> 3.8/game),
    V166 fired 62x, win split balanced 3-2, turns 6-20/side. Drains still dominate in
    absolute terms (some games stay drain-heavy) — further weight tuning is open.

  ════ V164 (2026-06) ════

  V164 — ABILITY-BASED BATTLE TRIGGER (V164a: equal-or-greater ability -> battle)
    Source: BattleEvaluator.java (rando + chosenone)
    Steve: "If the bots have equal or greater ability they should battle." The old
    gate required a POWER advantage (effectiveDiff = powerDiff + 2.5*abilityDiff >= 2),
    so even/slightly-behind-power sites were skipped even when ability was equal —
    feeding the drain stalemate. V164a adds: when abilityDiff >= 0 AND not outpowered
    by more than ABILITY_BATTLE_MAX_POWER_DEFICIT (2.0; tunable — set huge for "pure
    ability>=", 0 to require power parity), treat as a favorable battle (+40, same as a
    normal favorable battle so the V61 reserve guards and V22.4 catastrophic-power guard
    still dominate when a battle is unsafe). Lower power loses the battle in SWCCG, so
    we refuse a real power deficit; the spy/contest work (V166) handles unwinnable sites.
    Alone it was marginal (8 -> 11 battles) — it needs V166 to create the contested
    sites for it to act on. Together: 8 -> 19 battles.

  ════ V165 (2026-06) ════

  V165 — BOT-VS-BOT STALEMATE BREAKER (turn-20 cap, decided by life force)
    Source: SwccgGameMediator.java (shared game infrastructure — NOT an evaluator,
    so it applies to both bots automatically; there is no per-bot copy).
    Self-play can degenerate into a do-nothing stalemate — no drains, no resolved
    battles, no force loss — and run hundreds of turns (observed: a Rey-vs-Dooku
    game hit 237+ turns and pinned the CPU until killed). The AI cancel-loop guard
    (V163) doesn't catch it because each turn the bots make "valid" passing moves;
    nothing repeats, the turn counter just climbs forever.
    Steve: "stalemate breaker at turn 20 — most pro games end within 10 turns;
    whoever is the winner at turn 20 is the winner."
    Fix: in the bot-drive loop (startClocksForUsersPendingDecision), before each AI
    decision, maybeEndBotGameAtTurnCap() checks max(per-side turn number) >=
    BOT_GAME_TURN_CAP (20). If reached, it force-ends the game via
    playerLost(loser, LOSS__GAME_TIMEOUT) and awards the win to the higher life
    force (getPlayerLifeForce — the win condition's own metric). Exact tie -> Dark
    loses (deterministic). GATED on isBotGame() — real human games have no turn
    limit and are NEVER capped.
    Verified: Rey-vs-Dooku 5-game run — 2 games hit the cap (decided by life force,
    "ran out of time"), 3 ended naturally; whole tournament finished in ~165s with
    zero runaways (was 14+ min / 237+ turns before).
    Note: V164 (a BattleEvaluator ability-battle trigger) was attempted but proved a
    near-no-op — the existing favorable gate already rewarded ability advantages, so
    the new branch fired in too thin a window. Left uncommitted, pending rework.

  ════ V163 (2026-06) ════

  V163 — LOOP-BREAKER HARD VETO (cancel-loop block must DOMINATE)
    Source: ActionTextEvaluator.java + DrawEvaluator.java (rando + chosenone)
    Bot-vs-bot self-play froze again — but NOT the AMSD loop. Chosen One, turn 10,
    character "Keder The Black" in a MOVE-phase infinite loop (1000+ iterations
    before kill):
      1. "Choose Move action or Pass" -> picks "Move using landspeed" (score 50)
      2. "Choose where to move"       -> only destination Cloud City: Carbonite
         Chamber, BLOCKED by V41 wrong-direction (-9874) -> target step PASSES
      3. cancel re-offers step 1 -> picks Move again -> ∞
    Root cause is the §2A discipline failure (old rule dominated): the cancel-loop
    guard (DecisionTracker.blockLastActionOnCancel) correctly detected the loop and
    added the offending action to the blocked set, and ActionTextEvaluator applied
    its "BLOCKED (loop prevention)" penalty — but that penalty was an ADDITIVE
    -200, and V35.4 ("undercover spy blocking drain — move away to drain
    elsewhere!") added +250 on the same action. Net +50 > Pass (8), so the blocked
    Move kept winning every iteration. The loop-breaker got dominated by a movement
    bonus.
    Why it deadlocks: V35.4 says "flee the spy, move!", V41 says "that destination
    is wrong-direction, don't!", and it was the only legal destination. Neither
    yields; the -200 block couldn't tip it to Pass.
    Fix: the loop-breaker must DOMINATE by construction, not by a tunable margin.
    Both block sites now follow the existing V87 hard-block pattern: score the
    blocked action at -100000 AND `continue` (skip ALL further scoring), so no
    later positive rule (V35.4, V116, future rules) can ever stack it back above
    Pass. A response only enters the blocked set after the cancel-loop guard has
    proven it leads nowhere, so a hard veto is always correct; Pass (a separate
    evaluator, always present) then wins and the phase advances.
    The two sites with the additive -200 were ActionTextEvaluator:98 (where this
    loop lived, CARD_ACTION_CHOICE) and DrawEvaluator:138 (same broken pattern,
    fixed for consistency). Applied to both Rando and the synced Chosen One.
    Follow-up (strategy, not a loop): V35.4 boosts a move to flee a spy even when
    the only destination is V41-wrong-direction — the bot now correctly Passes
    instead of looping, but a smarter V35.4 would not boost the move at all when no
    acceptable destination exists. Flagged for Steve; separate from this loop fix.

  ════ V162 (2026-06) ════

  V162 — LOCATIONS DEPLOY FIRST (life-force gated) — fixes the AMSD/Bespin loop
    Source: DeployEvaluator.java (rando + chosenone), location-deploy block
    Bot-vs-bot self-play (synced Rando vs Chosen One) hit a 927%-CPU infinite
    loop: the Bespin SYSTEM sat in hand (cost 0) undeployed while the bot picked
    "Reveal pilot or Star Destroyer from hand" (AMSD, +600) over and over. AMSD
    needs the Bespin system already on the table to land the Star Destroyer, so
    it could never complete — the pilot-selection cancelled (Done), the action
    re-offered, repeat forever. V24 correctly blocked AMSD (-9999 "No Bespin
    system on table"), but the +600 deploy-side score won the DPS bucket walk;
    the cancel-loop guard blocked the wrong action (generic "Deploy" by index, not
    the AMSD reveal, since indices reshuffle). Root cause (Steve): the bot was
    holding the Bespin location in hand instead of deploying it first.
    Steve's rule: "Never hold locations (sites and systems) in hand unless life
    force <= 10 (reserve deck + force pile + used pile combined). Early game,
    deploy locations BEFORE anything else in the deploy phase — they're the
    foundation for force generation, drains, and deploy targets."
    Fix (DeployEvaluator location-deploy block, both bots):
      life force > 10 -> +500 (V162) on top of V67ai +1400 = locations deploy first
      life force <= 10 -> -200 (HOLD): keep the location in hand as force-loss fodder
    Life force = reserveDeck + forcePile + usedPile (hand/table excluded), same
    definition as V153. Deploying Bespin turn 1 means AMSD has its system and
    never loops. Applied to both Rando and the (now-synced) Chosen One copy.
    Follow-up noted: the cancel-loop guard blocks by action INDEX, which is
    fragile when the offered-action order reshuffles — should block by action
    identity/display-text. Separate fix.

  ════ V161 (2026-05-29) ════

  V161 — FORFEIT IMMUNE SHIP/CHARACTER FOR DAMAGE >= 4 (immune-attrition branch fix)
    Source: CardSelectionEvaluator.java (v159ForfeitScore step-4 immune branch)
    Last game: Rando "lost a lot of damage in the last battle" — could have
    forfeited a ship in the battle to cover most of it but didn't, instead
    burning pile cards. Steve: "He should choose to lose character or ship from
    battle to cover damage if damage is 4 or more even if immune to attrition."
    Root cause: the V159 picker's STEP-4 immune-attrition branch applied a flat
    -500 penalty to the immune forfeit score:
      score = savings * 60 - waste * 40 - 500
    At damage=4, fv=4 -> 240 - 0 - 500 = -260 (loses to pile +150).
    At damage=10, fv=7 -> 420 - 0 - 500 = -80 (still loses to pile).
    So Rando never forfeited the immune ship even when damage was huge.
    Fix: add a damage >= 4 (AND savings >= 3) branch that returns STEP-3-style
    positive scoring (1500 + savings*80 - waste*30) BEFORE the cautious -500
    branch. Matches STEP 3's coverage floor (savings >= 3) so a fv=1 immune
    card isn't forfeited for trivial coverage. Small damage / thin coverage
    keeps the old -500 cautious score (don't waste a board piece for tiny gain).
    Boundary math (damage=4, fv=4): 1500 + 320 - 0 = +1820 (beats pile +150
    decisively; immune ship forfeited for full damage coverage).

    UPDATED 2026-06-17 (Steve, Dooku replay sb2xzfjfpk5jxt8v): the damage-1-to-3
    case STILL scored a high-fv SOLO immune character negative (Yoda fv7 / dmg2 ->
    savings*60 - waste*40 - 500 = -580), so a solo immune Yoda bled Force one point
    at a time across a losing battle instead of forfeiting once. Immunity covers
    ATTRITION, but BATTLE DAMAGE is still Force lost every turn a solo immune body
    sits in a fight it loses — so forfeit a SOLO immune character scaled by how
    OUT-POWERED he is at the site (gap = opp power - our power), gated on damage > 0:
      gap <= 0 (holding) -> keep; gap 1-2 (solo-vs-solo) -> ~220-340, at/below the
      +350 pile (tiny lean, keeps him); gap 3+ -> 100 + gap*120 beats +350 (forfeit);
      capped 1200 (below the +1500 mandatory tier). Grouped immune chars and query
      failures fall through to the old cautious -580 return unchanged. (No new V-tag:
      this UPDATES V161 per Steve's "adjust the old rule, don't mint a new version".)
      Shield fix (below) made Rando win the 1 Rey vs 1 Dooku self-plays, so the solo-
      losing scenario didn't recur to bind in self-play; correct-by-construction +
      log-visible (the caller logs every v159 score) when it does.

  ════ V160 (2026-05-29) ════

  V160 — SHIELD WILL BE DOWN IN MOMENTS — RECOGNIZE THE DECK + PUSH TARGET THE MAIN GENERATOR
    Source: ObjectiveAnalyzer.java + ActionTextEvaluator.java
    Last replay: Rando played the dark Hoth invasion deck ("The Shield Will Be
    Down In Moments") and never deployed Target The Main Generator (the Epic
    Event) — so it never blew up Main Power Generators and the objective never
    flipped. Steve: "He needs to get the epic event on the table so he can blow
    up the hoth generator."
    Root cause: ObjectiveAnalyzer had NO recognition for this objective — no
    isShieldWillBeDown flag, no pullable cards, no objective-relevant locations.
    Rando treated the deck as generic and never pushed the engine.
    Fix:
      (a) ObjectiveAnalyzer: detect by title (objectiveTitle contains "shield
          will be down"). Populate requiredCardsOnTable = {target the main
          generator}, pullableCards = {target the main generator, at-at cannon,
          prepare for a surface attack}, location fragments = {hoth: defensive
          perimeter, hoth: ice plains, hoth: main power generators}. Mirrors
          the existing Hunt Down V / ISB Operations pattern.
      (b) ActionTextEvaluator: V160 push +800 on any action whose text contains
          "target the main generator" when the deck is recognized — covers the
          deploy from hand AND the AT-AT-fire response.
    Win path the deck now pursues: deploy Target The Main Generator on Ice
    Plains -> position AT-AT Cannon at 3rd Marker or lower -> fire each deploy
    phase (draw destiny + 1 per marker occupied + 2 per marker controlled) ->
    destiny > 8 -> Main Power Generators "blown away" -> objective flips to
    Imperial Troops Have Entered The Base! (snowtrooper / attrition engine).
    Originally tagged V159 in code; renumbered to V160 after a sibling fork
    landed V159 (forfeit picker) first.

  ════ V159 (2026-05-31; immune-threshold + capital-release + immune-subject + engine-immune fixes 2026-06-02) ════

  2026-06-02 ENGINE-BACKED IMMUNITY (Steve, same-day third pass):
    Replaced the bespoke regex with the engine's live modifier query — same
    call GuiUtils.isImmuneToRemainingAttrition uses to change the attrition
    icon in the UI. Removed ~23 lines of regex/threshold-parsing logic and
    the two prior fixes that patched the regex's substring-match holes
    (immune-threshold-parse from 2026-06-02 AM, immune-subject-clause-start
    from 2026-06-02 PM). The earlier fixes were correct stopgaps but
    fundamentally fragile — every new immune-text variant required another
    regex tightening. The engine call is authoritative.
    New detection (mirrors logic/timing/GuiUtils.java:158-171):
      boolean isImmune = false;
      if (game != null) try {
          ModifiersQuerying mq = game.getModifiersQuerying();
          GameState gs = game.getGameState();
          if (mq != null && gs != null) {
              float exactImmunity = mq.getImmunityToAttritionOfExactly(gs, card);
              if (exactImmunity > 0f) {
                  isImmune = (exactImmunity == attrition);
              } else {
                  float lessThanImmunity = mq.getImmunityToAttritionLessThan(gs, card);
                  isImmune = lessThanImmunity > attrition;
              }
          }
      } catch (Exception ignore) { /* assume not immune on error */ }
    The engine already resolves:
      • "Immune to attrition." → getImmunityToAttritionLessThan returns
        Float.MAX_VALUE → always > attrition → immune.
      • "Immune to attrition < N." → getImmunityToAttritionLessThan returns
        N → immune when N > attrition (note: engine uses STRICT > here,
        matching the card text "attrition LESS THAN N").
      • "Immune to attrition of exactly N." → getImmunityToAttritionOfExactly
        returns N → immune when attrition == N.
      • "Immunity to attrition capped at N." → engine clamps the
        less-than threshold to N internally; the same less-than query reads it.
      • Dynamic immunity from another card (e.g., a companion card grants
        "all Sith immune to attrition while X present") → modifier collector
        returns the active immunity, regardless of the card's own text.
      • Conditional immunity (e.g., "while X" with X currently true) → only
        appears in the modifier collector if the condition is met.
      • Other-character mentions (Bib Fortuna's "Jabba is immune to attrition"
        modifies Jabba's record, not Bib's) → the engine attributes immunity
        to the correct card.
    Import added: `com.gempukku.swccgo.logic.modifiers.querying.ModifiersQuerying`
    (resolved via fully-qualified name in code; the existing imports already
    pull `SwccgGame` and `GameState`).
    Three test cases re-verify against the live engine semantics:
      • Bossk (200_131) attrition 6, "Immune to attrition < 4" → engine returns
        4 for less-than → 4 > 6 is false → not immune → STEP 2 → +2100.
      • Bib Fortuna (225_1) attrition 6, "Jabba is immune to attrition" →
        engine returns 0 for Bib's own immunity (Jabba is the modifier subject)
        → 0 > 6 is false → not immune → STEP 2 → +2000.
      • Hypothetical Vader with "Immune to attrition" attrition 6 → engine
        returns Float.MAX_VALUE → MAX > 6 → immune → STEP 4 (protect with
        damage-savings formula).
    The earlier 2026-06-02 regex fixes are preserved in git history; their
    behavior is fully subsumed by the engine call.

  ════ V159 (2026-05-31; immune-threshold + capital-release + immune-subject fixes 2026-06-02) ════

  2026-06-02 IMMUNE-SUBJECT FIX (Steve, Bib Fortuna replay, same-day follow-up):
    Yesterday's threshold fix handled "Immune to attrition < N" properly but
    still relied on a substring `gt.contains("immune to attrition")`. New replay
    showed the second hole: Bib Fortuna (225_1) game text
      "While with Jabba, Bib is power +2 and Jabba is immune to attrition."
    The phrase describes JABBA's immunity, not Bib's. Substring match caught it,
    no `< N` qualifier so the unqualified branch set isImmune=true. V159 STEP 4
    returned 3*60 - 0*40 - 500 = -320 for the forfeit. Same 9-cycle pile-loss
    loop, same eventual forced forfeit.
    Fix: tighten the detection regex to require the immunity phrase to begin
    its own clause. Pattern:
      (?:^|[.,;:!?]\s+|^\s*)immune to attrition(\s*<\s*(\d+))?
    The non-capturing prefix alternation accepts start-of-text, sentence
    boundary (one of `.,;:!?` followed by whitespace), or leading-whitespace
    start. "Jabba is immune to attrition" is preceded by " is " — no boundary
    match → regex does not fire → isImmune = false → V159 takes the
    attrition-owed branch (STEP 2) and scores Bib's forfeit positively.
    Test matrix:
      "Immune to attrition < 4." (Bossk, after a period)         → match, threshold 4
      "Jabba is immune to attrition." (Bib's text)               → NO match
      "Immune to attrition." (unqualified self)                  → match, unqualified
      "Power +2. Immune to attrition < 5." (typical)             → match, threshold 5
      "He is immune to attrition." (pronoun subject)             → NO match
      "Some text. Immune to attrition < 3" (no terminal period)  → match, threshold 3
    No new V-tag — same V159 helper, single regex tightening. Threshold-
    parsing logic from yesterday's fix is preserved verbatim, just lifted
    inside the new boundary-aware regex.

  ════ V159 (2026-05-31; immune-threshold + capital-release fixes 2026-06-02) ════

  2026-06-02 TWO BUGS PATCHED (Steve, Bossk-In-Hound's-Tooth replay):
    Game evidence (line ~19440 in nohup): attrition=6, damage=9, sole in-battle
    character Bossk In Hound's Tooth (CAPITAL, fv=6, gameText:
    "Permanent pilot is Bossk... Immune to attrition < 4."). V159 scored
    Bossk's forfeit at -140; "Lose Force from pile" scored +150 on hand zone.
    Pile loss won, Rando bled 9 cards from Reserve over 9 cycles, attrition
    NEVER dropped (still owed 6), would forfeit Bossk anyway next time —
    net cost ~15 cards lost vs the ~3 he'd have lost by forfeiting Bossk
    immediately (fv=6 covers 6 of 15 = 3 remaining damage from reserve).
    Steve: "he lost a bunch of force from reserve instead of forfeiting his
    ship, which he had to do anyways since he owed attrition."

    BUG 1: Immune-detection substring match.
      Original (line 8334-8338):
        boolean isImmune = false;
        if (bp.getGameText() != null
                && bp.getGameText().toLowerCase().contains("immune to attrition")) {
            isImmune = true;
        }
      Bossk's "Immune to attrition < 4" contains "immune to attrition" →
      isImmune = true regardless of the attrition value. V159 then hit STEP 4:
        savings = min(fv, damage) = min(6, 9) = 6
        waste   = max(0, fv - damage) = 0
        return savings*60 - waste*40 - 500 = 360 - 0 - 500 = -140
      Exactly the observed score.
      Fix: parse the optional "< N" qualifier and check the actual attrition:
        Matcher m = Pattern.compile("immune to attrition\\s*<\\s*(\\d+)").matcher(gt);
        if (m.find()) { isImmune = attrition < Integer.parseInt(m.group(1)); }
        else { isImmune = true; }
      Bossk + attrition=6 + threshold=4 → 6 < 4 is false → isImmune = false
      → V159 falls through to STEP 2 attrition-owed.
      Unqualified "Immune to attrition" still works (no regex match → true).
      try/catch around the parse for safety (NumberFormatException → true).

    BUG 2: CAPITAL / AiPriorityCards release valve unconditional.
      Original (line 8382-8384):
        if (isCapitalShip || isPriority) {
            return -1000f;  // deck centerpiece — forfeit only when forced
        }
      After bug 1 fixed, Bossk falls through to STEP 2, hits this release valve,
      returns -1000. Lose Force from pile +150 still wins. Loop persists.
      Premise of the release valve (added 7cf78310d Jun 1): protect a centerpiece
      ship when there's a cheaper character that could forfeit instead. But
      when Bossk is the ONLY forfeit option AND fv covers attrition fully,
      the ship is going to be sacrificed anyway when attrition demand renews —
      forfeiting NOW absorbs damage too.
      Fix: narrow the gate. Only fire the -1000 release when
      `(isCapitalShip || isPriority) && fv < attrition`. When fv >= attrition,
      forfeit covers the attrition demand fully → fall through to the standard
      +1500 + coverage*100 scoring. For Bossk: fv=6 >= attr=6 → fall through →
      coverage=6 → score = 1500 + 600 = +2100. Forfeit wins decisively over
      pile loss +150.
      Preserves the Gall replay (360z5sh5jruys8p7) protection for First Light
      WHEN there's a cheaper character with fv < attrition to forfeit instead
      — those cheaper characters still score higher under cheap-bonus (+200 for
      fv 1-3) so the picker prefers them. Only when no cheap alternative exists
      does the centerpiece forfeit.

    Net effect: V159 now correctly scores partial-immunity ships (like Bossk
    at his < 4 boundary) and centerpiece ships with sufficient fv to cover
    attrition. Both fixes are surgical edits to V159's existing scoring; no
    new V-tag, no scope expansion.

  ════ V159 (2026-05-31) ════

  V159 — UNIFIED FORFEIT PICKER (implements /tmp/FORFEIT_SPEC.md v3)
    Source: CardSelectionEvaluator.java — shared helper v159ForfeitScore(),
    called from both evaluateForfeit AND evaluateForceLossOrForfeit so the
    same situation gets the same score (kills the +150-vs-+1500 hit-first
    drift the helper review flagged).
    Replay xifjb2j8dsn74kh1 (turn 5: 7 attr + 8 dmg; Rando burned 8 cards
    paying damage BEFORE forfeiting for attrition) and l3wvdgfkfyd2gdl9
    (pile +330 beat forfeit -135 because V139 over-protected Blizzard 1
    fv=7 at damage=11) confirmed two bug classes the old code never won:
      (a) battle-damage paid pile-first while attrition still owed
      (b) pure damage where pile beat an efficient forfeit because V139
          protections + small V67t savings couldn't outscore V153 zone bonuses.
    Per /tmp/FORFEIT_SPEC.md v3 (Steve's "damage >= 3 -> forfeit considered"):
      Step 1: hit/dead -> forfeit first (slight defer when attrition owed)
              +3000 / +2500 (no attrition);  +1500 / +1200 (attrition owed)
      Step 2: attrition owed -> forfeit MANDATORY; cheapest covering character;
              release valve: game-winner (power>=6 + ability>=4) + attrition<=2
              returns -1500 so we lose Force instead of sacrificing a Vader to
              a 1-point attrition (helper-flagged failure mode).
              Score: 1500 + coverage*100 + (300 covers-all) + (200 cheap-fv<=3).
      Step 3: pure damage (attrition==0):
                damage <  3 -> -3000 (protect, lose Force instead)
                damage >= 3 -> forfeit ON THE TABLE, V139 protection MUST yield:
                  savings <  3 -> -800 (forfeit doesn't soak enough)
                  savings >= 3 -> 1500 + savings*80 - waste*30 + (200 if savings>=damage/2)
      Step 4: immune-to-attrition + un-hit + attrition owed -> damage coverage
              only: savings*60 - waste*40 - 500, or -2500 if can't help.
    V159 is the SOLE forfeit-scoring path. Old V143 / V67bh / V67t / V139 /
    V146-hit-dead / V67bd / V145 blocks in BOTH evaluateForfeit and
    evaluateForceLossOrForfeit are wrapped in `if (false /* V159 SUPERSEDED */
    && ...)` per §2A — preserved for review/git-history but never execute.
    Verified: the old "V143 HARD BLOCK" / "V67bd" / "V146 ALREADY HIT" strings
    are stripped from the compiled bytecode by the javac dead-code pass. Logs
    will only show "V159 FORFEIT" for forfeit-side scoring (the helper review
    complaint "we're quoting several versions" goes away).
    Boundary math (bug case l3w...: 11 damage, Blizzard fv=7):
      Old: V67t +190 + V139 -375 = -135; pile +330 wins.
      V159: 1500 + 7*80 + 200 = +2260; net forfeit +2125; pile +330 -> forfeit wins.

  ════ V158 (2026-05-28; reserve-deploy bypass guard + no-wielder branch 2026-05-29; criteria-absent fix 2026-05-29) ════

    2026-05-29 CRITERIA-ABSENT FIX (Dooku-deck stuck loop, Steve: "Rando just did
    it again with the dooku deck. This must be a recent thing we deployed that is
    causing the error. It's happening with every deck."): the in-evaluator V158
    gate at DeployEvaluator.java had a hole. Three branches:
      1. v158Criteria != null && matchArmed > 0 && matchUnarmed == 0 → -9999
      2. lightsaber && unarmedWarrior4 == 0 → -9999
      3. totalUnarmed == 0 && totalArmed > 0 → -9999
      else (totalUnarmed > 0 || matchUnarmed > 0) → +300
    The bug: when criteria parsed but the named persona wasn't on the table at
    all (matchArmed == 0 AND matchUnarmed == 0), branch 1 didn't fire (it needed
    matchArmed > 0 as "proof of a real attribute hit, not a garbage parse"). The
    fall-through hit +300 because some OTHER non-matching character was unarmed.
    Rando picked "Play Dooku's Lightsaber" as the outer action, the sub-decision
    asked "where to attach?" — no legal criteria-matching target — Rando hit
    Done, engine re-asked → infinite Done loop. The 3-strike cancel-loop
    fallback (DecisionTracker) would have caught it eventually, but the +300
    was the root cause; killing it kills the loop cleanly.
    Fix: drop the matchArmed>0 condition. Branch 1 now fires whenever
    matchUnarmed == 0 regardless of matchArmed. Two log sub-cases for triage:
    "criteria all armed" (matchArmed > 0) vs "criteria absent" (matchArmed == 0).
    The original "garbage criteria false-blocks all weapons" worry is moot here
    because (a) the criteria-aware parse is robust enough that garbage criteria
    don't produce a clean matchArmed==0 / matchUnarmed==0 with the persona on
    table — if the persona IS there, one of the counts goes up; (b) if the
    persona isn't there, the deploy literally has no legal target, so blocking
    is correct anyway. Verified jar contains "criteria absent" log string. No
    new V-tag (appended into V158 per Steve: "avoid splintering off versions").



    2026-05-29 NO-WIELDER BRANCH (replay filx81 turn 2): the first reserve-deploy
    branch catches "<weapon> from Reserve Deck on <character>" + character armed.
    But filx81 showed a second variant: Rando pulled Vader's Lightsaber via I Am
    Your Father (V) on turn 2 — auto-targeted by the weapon's name, no "on X" in
    the action text. Lord Vader didn't deploy until turn 3. The saber sat in
    hand, then was lost as force-loss fodder = wasted pull. Added a second branch
    tagged into V158: action text matches "X's Lightsaber" + "from Reserve",
    extract the persona word before "'s lightsaber", check the table for that
    persona; if absent, block -9999 (no wielder = wasted pull). No new V-tag.


    2026-05-29 RESERVE-DEPLOY BYPASS GUARD (replay ss2jc7): the DeployEvaluator-
    level V158 gate catches normal weapon-deploy actions, but weapons coming FROM
    RESERVE via an effect (Evil Is Everywhere deploys [Ep1] lightsaber on Sidious;
    Sidious' Lightsaber from Reserve on Sidious) bypass it. Replay: Lord Sidious
    got Asajj Ventress' Lightsabers (t1) AND Sidious' Lightsaber (t3) — DOUBLE-ARMED.
    Defensive guard appended in ActionTextEvaluator (paired next to V155 since
    both target reserve-via-effect actions): when action text matches a weapon
    keyword ("lightsaber"/"blaster"/"rifle"/"bowcaster"/"weapon") AND contains
    "from Reserve Deck on", extract the target character name after "on", look it
    up on the table, and block -9999 if it already has a weapon attached. No new
    V-tag (appended into V158 per Steve: "avoid splintering off versions").


  V158 — UNIFIED WEAPON DEPLOY GATE (combines V33 + V67aq + V115)
    Source: DeployEvaluator.java
    Steve: "V33 and V67aq seem like they conflict / double up points — combine
    into one weapon rule, like we did for force-loss/locations." Plus the V115
    caution: "be careful we specify the criteria correctly, otherwise we'll
    never deploy weapons."
    Three old deploy-gate rules overlapped and fought: V33 (target parsed from
    action text already armed → -9999), V67aq (count unarmed/armed; all armed →
    -9999), V115 (criteria-aware; no criteria-matching unarmed → -9999). V33 +
    V67aq both hard-blocked the all-armed case (double -9999); V115 blocked on
    matchUnarmed==0 EVEN WHEN matchArmed==0, so a mis-parsed "deploys on X"
    criteria false-blocked EVERY weapon.
    V158 replaces all three with one gate. Hard-block -9999 when ANY of:
      1. criteria parsed AND >=1 matching ARMED AND 0 matching UNARMED (every
         legal wielder armed — e.g. 2nd Sidious' Lightsaber on armed Sidious).
      2. lightsaber AND 0 unarmed [Warrior] ability>=4 wielder (folds in V149's
         capable-wielder check for hand-deploys).
      3. 0 unarmed characters at all.
    Else +300. CRITERIA-SAFE: #1 requires matchArmed>0 (proof the criteria hit a
    real attribute); a garbage criteria yields 0 matching armed and falls through
    to the generic checks — never a false block. Verified (work-verifier PASS)
    across named / generic / garbage-criteria / all-armed / non-warrior cases.
    Stage-2 wielder-pick (CardSelectionEvaluator: armed -9999 / unarmed +20) and
    V149 (pull-side lightsaber check) are unchanged — V158 is the deploy-gate
    layer. V33 NAMED-WEAPON priority (+200 named, generic-waits) also still runs;
    the +200-on-top-of-+300 stacking is left to the weapons-after-characters
    ordering rule. Chosenone DeployEvaluator still has the old V115 (bot-vs-bot
    only) — deferred.

  ════ V157 (2026-05-28) ════

  V157 — DEPLOY CAP IS UNCONTESTED-ONLY; OVERWHELM WEAK CONTESTED SITES
    Source: common/strategy/CharacterDeploySiteEvaluator.java (V136 §B)
    Steve: "the cap should only be for uncontested sites. If opponent has 4 or
    less ability at a battleground and Rando can deploy 20+ ability to win, we
    should not limit him. As soon as opponent has characters at a site, try to
    overthrow if it makes sense." Replay lzz71zzoo72ttwcm: Rando under-spread /
    over-massed; the per-turn ability cap could also hold back a legit attack.
    Before: the §B ability-saturation cap (turns 1-3, thresholds 10/13/17) and
    the over-stack penalty fired at BOTH contested and uncontested sites, so they
    could limit a winnable attack.
    Fix: over-stack penalty AND ability cap now apply to UNCONTESTED sites only
    (don't over-pile an empty site). At a CONTESTED site there is no cap — massing
    to overwhelm is the point. Plus a +200 nudge to overthrow a WEAKLY-defended
    contested site (opponent's total character ability there <= 4). [Magnitude was
    +300 on first install; lowered to +200 per unanimous-ish council review
    (5 roles, 2026-05-28): +300 risked over-committing one site and starving Force-
    drain economy. Council also pushed a body-count threshold — REJECTED by Steve:
    a powerful pair like Vader+Tarkin already fails "<=4 total ability", and body-
    count would wrongly shield a weak swarm; ability-only <=4 stays.]
    The win/lose judgment still lives in §A (team viability: +500 when the deploy
    can win, negative when out-powered), so this does NOT re-enable piling into an
    unwinnable fight — §A's power check plus the weak-defender (<=4) gate keep the
    Jedi-wall pile out (that site's opp ability is high). Threshold (<=4) tunable.
    Lives in the COMMON evaluator (rando + chosenone).
    NOTE: V156 fired 0 times in the replay (it only touches uncontested solo
    deploys) — it was NOT the under-spread cause and is left untouched.

  ════ V156 (2026-05-28; SOLO-NO-BUDDY revision 2026-05-29) ════

    2026-05-29 SOLO-NO-BUDDY REVISION (Steve, after replay filx81): the earlier
    interim broadened the weak-defender gate to "power <=3 OR ability <=4" so it
    would catch Seventh Sister. Steve corrected the philosophy entirely: "They
    should not deploy solo. They should at minimum have a buddy move to them or
    deploy a buddy." So the weak-defender gate is GONE — the penalty now fires on
    ANY solo deploy turn <=2 when there is no buddy plan, where "buddy plan"
    means either (a) an affordable character in hand to co-deploy this turn, or
    (b) any friendly character already on the table at another site (potential
    mover next phase). Even Vader/Dooku/Sidious get held until a buddy is in
    place — Steve wants the buddy system enforced top-down. No new V-tag.


  V156 — DON'T LEAVE A WEAK CHARACTER SOLO ON TURN <= 2
    Source: common/strategy/CharacterDeploySiteEvaluator.java (V136 §A)
    Steve: "Turn two is dangerous to leave a 3-power 4-ability character by
    themselves. Either save force for a larger deploy [or] bolster a preexisting
    location if needed." Last game: Rando spread on turn 2 and left a weak
    character standing alone.
    Root cause: V136 §A scored a lone weak character at a fresh, uncontested site
    +500 (powerPass — power 3 >= opp 0; bodyPass — no opp weapon), actively
    REWARDING the vulnerable solo spread. V113's -300 solo-vulnerability penalty
    (CardSelectionEvaluator) wasn't enough to overcome the +500.
    Fix: in §A, when currentTurn <= 2 AND the deploy is solo (teamBodyCount == 1 —
    no friendly character already at the site) AND the site is uncontested
    (oppPower == 0) AND no buddy is co-deploying AND the character's power <= 3,
    return -300 instead of +500. The lone-weak spread now loses to bolstering an
    existing group (+500) and to PASS — so Rando reinforces, or saves the force
    for a larger combined deploy. Strong characters (power > 3, e.g. Vader/Dooku)
    can still spread solo. currentTurn was threaded into computeTeamViability.
    Lives in the COMMON evaluator, so rando + chosenone both inherit it.
    Pairs with V113 (solo-vulnerability) and V151 (contested co-deploy). Power
    threshold (<= 3) is Steve's number — tunable.

  ════ V155 (2026-05-28; gate fix 2026-05-29) ════

    2026-05-29 GATE FIX (replay ss2jc7): the original gate required "the works" OR
    "petranaki" in the action text, but the actual play-action text is generic
    ("Take location into hand from Reserve Deck") — the target names live in the
    card's game text, not the action text. V155 fired 0× in the replay even with
    The Works on the table. Gate now keys only on (a) "into hand from reserve" and
    (b) the source-card title contains "welcome home" — the source-card filter is
    specific enough; no need to also match target names in the action text. No
    new V-tag (appended per Steve: "avoid splintering off versions like before").


  V155 — WELCOME HOME, LORD TYRANUS: SAVE FOR BATTLE (un-parks "V152")
    Source: ActionTextEvaluator.java
    Welcome Home, Lord Tyranus (Lost Interrupt) has a premium ONCE-PER-GAME
    battle mode: if Darth Tyranus is in battle and about to draw a battle
    destiny, instead use his ABILITY NUMBER (a guaranteed high destiny). Its
    weak mode-1 just pulls Petranaki Arena or The Works into hand from Reserve.
    Screenshot 2026-05-28 (turn 1): Rando fired mode-1 to pull a location while
    The Works was ALREADY on the table — burning the premium battle interrupt
    on a near no-op. Steve (originally parked as "V152"): "save this card for
    battle with Dooku once The Works is in hand or on table. He keeps searching
    his reserve for The Works after it's already out. Very useful in battle."
    Fix (Oracle-driven, per Steve): on the Welcome Home mode-1 pull, ask the
    Deck Oracle whether EITHER target is actually pullable, and hard-block -2000
    (hold for battle) when:
      (a) DEAD PULL — neither The Works NOR Petranaki Arena is in the Reserve
          Deck. The Oracle tracks live zones AND the deck list, so a location
          not in the deck at all (this deck runs NO Petranaki Arena) reads as
          not-in-reserve, as does one already pulled out. Steve: "Deck Oracle
          should have caught that there are no Petranaki Arena in the deck
          before pulling." No-Petranaki + The Works already on table = nothing
          to fetch.
      (b) SAVE FOR BATTLE — The Works is already on table/in hand (hold even if
          Petranaki Arena is still pullable).
    Falls back to a gameState table/hand scan for The Works if the Oracle is
    unavailable.
    Why V95 didn't catch it: V95 (dead-interrupt fodder) requires ALL pull
    targets on table AND reserves >= 15, and only checks the table — it never
    consulted the deck list, so "Petranaki Arena not in deck" was invisible to
    it. V155 is a VALUE rule (premium battle interrupt) that uses the Oracle's
    deck knowledge. No broad card-name lists.

  ════ V153 (2026-05-28) ════

  V153 — UNIFIED FORCE-LOSS ORDER (character / life-force tiers, both handlers)
    Source: CardSelectionEvaluator.java — evaluateForceLoss (regular) AND
            evaluateForceLossOrForfeit (battle, force-loss side)
    Replay 6x8e5hyqgajpe045: Rando paid Force drains by losing General
    Grievous off the top of Reserve while spare interrupts sat in hand,
    and (battle path) dumped Lord Sidious + Ap'lek from hand before the
    Force Pile. Steve: "Still losing from hand and reserves before
    forfeiting characters."
    Root cause: the old V127/V29.8 healthy/low zone scoring was INVERTED
    (it dumped HAND first when LOW, and lost RESERVE before HAND when
    healthy — backwards).
    MECHANIC (verified in engine, corrects an earlier wrong claim): life
    force = Reserve + Force Pile + Used Pile (GameState.getPlayerLifeForce);
    HAND and TABLE are NOT counted. Defeat fires at life force <= 0
    (checkLifeForceDepleted). So losing from hand / forfeiting is FREE to the
    lose-condition; losing from reserve/used/force moves you toward defeat.
    (My first V153 cut claimed "every zone = 1 force" — false. Steve caught
    the implication and set the tiers below.)
    Fix — Steve's order (lose FIRST -> LAST), by life force tier:
      >= 4 (protect characters): Dup hand > Used > hand junk > Reserve
                                 > HAND CHARACTERS > Force pile
      <  4 (survival, save the life-force piles): Dup hand > hand junk
                                 > HAND CHARACTERS > Used > Reserve > Force pile
    Within hand, every non-character is lost before a character (chars are
    the comeback). At >=4 Reserve is spent to keep characters; below 4 the
    whole hand (junk then chars) is dumped to keep reserve/used/force off the
    deck-out line. Force pile ALWAYS last. Hand floor: keep >=4 cards in hand
    while life force >= 10 (-700 once handSize <= 4).
    Magnitudes (higher = lose first): dup 1000; hand junk 600(>=4)/850(<4);
    used 800(>=4)/400(<4); reserve 400(>=4)/300(<4); hand ship 500/750;
    hand char 100(>=4)/700(<4); force pile 50.
    Consolidated: replaced the V127/V29.8 healthy/low blocks in BOTH handlers
    with this one tiered rule. AiPriorityCards protection now also covers the
    Used pile; objective-critical -9999 added to the battle force-loss side.
    PRESERVED (all still fire): V109 senator -300, V28 Draw Their Fire force-
    pile protect, V21 objective-critical -9999, V25 Hunt-Down lightsaber -500,
    duplicate bonus; forfeit side (V146/V67bd/V139/V67t/V67bh/V143/V150) untouched.

  ════ V154 (2026-05-28) ════

  V154 — WEAPON-LOSS EDGE CASE (strip weapons before forfeiting)
    Source: CardSelectionEvaluator.java (evaluateForceLossOrForfeit)
    Steve: some effects (the Shadow Collective deck has one) let Rando lose a
    deployed WEAPON to satisfy battle damage/attrition. When that effect is
    active and a weapon appears as an option in the "lose Force or forfeit"
    decision, lose the WEAPON FIRST — ahead of everything, including hit
    characters. A hit character is forfeited anyway and its weapon would be
    lost for FREE with it; stripping the weapon first banks the extra coverage,
    then the hit character forfeits separately next.
    Detection is global by CardCategory.WEAPON (no hardcoded card names) — a
    weapon only appears as an option here when such an effect is active.
    Score +2000 (above V146 hit-forfeit +1500), or +2200 if the weapon's host
    is HIT (best case — strip the doomed character's weapon first). Skips the
    rest of the per-card scoring (continue) since the weapon option dominates.

  ════ V151 (2026-05-28) ════

  V151 — CO-DEPLOY POWER LOOKAHEAD (deploy into the fight, skip the move)
    Source: common/strategy/CharacterDeploySiteEvaluator.java (V136 §A)
    Steve: "Why not just deploy to the same site as opponent and attack?
    Save the move force." V136 §A blocked a direct deploy into a contested
    site whenever the SOLO unit was out-powered — so Vader deployed safe
    then moved (wasteful two-step, paired with the V137 move issue).
    Fix: when the candidate site is contested (oppPower > 0), ability
    passes (team can draw battle destiny), but solo power falls short,
    greedily project the hand reinforcements we can afford to co-deploy
    here this turn. If the combined power would win, score +400 (coordinated
    attack setup) so the strike group commits to the enemy site directly.
    The follow-on characters score +500 (team viable) once the first is
    there, so the pack assembles in a single deploy phase — no move force
    wasted.
    Pairs with V137 (move winnability) and V137b (Vader+Dooku hunt): now
    the preferred play is deploy-the-pack-into-the-fight; moving is the
    fallback. Buddy-follow risk: if force runs out mid-phase the first
    unit could be left solo — watch logs; bump if needed.

  ════ V137 (2026-05-28; ANTI-SOLO-BG extension 2026-05-29) ════

    2026-05-29 ANTI-SOLO-BG EXTENSION (replay ss2jc7): the original gate only fires
    when oppPower>0 at the destination AT MOVE TIME. Asajj deployed to Guest
    Quarters (drain), then moved SOLO to Beldon's Corridor (uncontested at move
    time); asdf reinforced Beldon's the next turn and overran her — 0 vs 10 power,
    ability 4 vs 12, forfeit + battle damage = -7 force. Appended an `else` branch:
    when oppPower==0 at the destination AND the destination is a battleground AND
    the projected friendly team there (mover + buddies at current location + chars
    already at dest) is <=1, penalize -500. Don't park a lone body in opp-reachable
    BG territory. No new V-tag.


  V137 — MOVE-SIDE WINNABILITY GATE (stop wasteful charges into losing battles)
    Source: MoveEvaluator.java
    Replay 37orjzqd6feo6igp: Rando deployed Vader (+ Third Sister) to a
    safe site, then MOVED Vader solo into Rey + Yoda at the adjacent
    Upper Chamber — wasting move force to walk into a battle he loses.
    Steve: "odd that he deployed then moved to the site with enemies.
    Waste of move force. Could have deployed there directly and battled,
    or just left the guys there."
    Root cause: V136 (deploy) correctly refused the direct deploy into
    Rey+Yoda (solo Vader can't win) so Vader deployed adjacent. Then V35
    HUNT JEDI (+350) moved him solo into the same losing fight anyway.
    Deploy and move logic contradicted.
    Fix: when a move targets a CONTESTED destination (opp power > 0),
    compute the projected friendly team there (mover + friendlies
    already present). If it can't win (power < opp OR total ability < 4),
    penalize -800 (-1500 if outmatched by 6+) — enough to cancel the
    V35/V34 contest/hunt bonuses so the character stays put. If the team
    CAN win, no penalty; and V136 would have permitted a direct deploy
    there, so the wasteful deploy-then-move two-step disappears either
    way.
    AGGRESSIVE version (Steve's call): the projected team counts the WHOLE
    group at the mover's current location (mover + buddies that can move
    together via V29.13 grouping), not just the mover. So Vader + a buddy
    who together CAN win are NOT blocked — they execute a coordinated
    attack. Only a true solo charge (no buddy) into a losing fight is
    blocked. Steve: "both times he deployed Vader with a buddy, those two
    absolutely had a chance of winning" — the bug was the SPLIT, not the
    matchup.

  V137b — HUNT + GROUPING EXTENDED TO ALL DARK JEDI (Vader AND Dooku)
    Source: MoveEvaluator.java
    Steve: "Vader Dooku deck — both need to aggressively attack opponent."
    The V35/V29.12 hunt trigger and V29.13 grouping anchor were Vader-only
    (title contains "vader"). Extended to any Dark Jedi (dark character,
    ability >= 6) plus title fallback for vader/tyranus/dooku. Now Dooku
    (Darth Tyranus) hunts Jedi too, and buddies group toward whichever
    Sith leads the strike. Lower-ability Inquisitors (Third Sister) remain
    buddies, not hunters. Pairs with the aggressive V137 winnability gate
    so the Sith pack commits together when they can win.

  ════ V150 (2026-05-28) ════

  V150 — FORFEIT COVERS ATTRITION+DAMAGE (fix forfeit-vs-pile regression)
    Source: CardSelectionEvaluator.java (evaluateForceLossOrForfeit)
    Replay gzv8mrd0rbtvcm9r: Rando paid 11 battle damage card-by-card
    from piles, THEN forfeited characters for 5 attrition — bleeding
    ~10 cards. When attrition is owed a forfeit is MANDATORY (pile loss
    can't satisfy attrition), and that mandatory forfeit's value also
    covers battle damage. Paying damage from pile while attrition is
    still owed wastes pile cards.
    Root cause was self-inflicted: this session's V139/V143/V145
    forfeit-protection work shrank the forfeit score so pile loss
    edged it out even at huge damage burdens.
    Fix: while attritionRemaining > 0, the pile-loss "CANNOT satisfy
    attrition" penalty is -500 (was VERY_BAD_DELTA -150). Forfeits now
    win until attrition is satisfied; V139 still picks the CHEAPEST
    character among forfeit options. Once attrition hits 0 (next
    decision cycle) normal V143/V139 pile preference resumes for small
    remaining damage.
    Edge case (all characters immune to attrition): the immune-forfeit
    V145 score stays below pile loss, so the bot still pays damage from
    pile — self-resolves correctly.

  ════ V149 (2026-05-28) ════

  V149 — LIGHTSABER PULL NEEDS A CAPABLE WIELDER ([Warrior] + ability >= 4)
    Source: DeployEvaluator.java + ActionTextEvaluator.java (both V67am paths)
    Active game: Evil Is Everywhere ([download] a lightsaber) scored +600
    because there was "1 unarmed character" — but that was Dr. Evazan, a
    cantina alien who can't wield a lightsaber. V67am/V67ar counted
    unarmed characters generically with no wield check.
    First cut used an [Episode I] icon match; Steve corrected: "lightsabers
    require specific warriors, not icon type. Warriors have a [Warrior]
    icon indicating they can carry weapons — warrior type with ability
    >= 4." Final rule: when the pulled weapon is a lightsaber (source
    download text contains "lightsaber"/"saber"), require at least one
    UNARMED character with BOTH the [Warrior] icon AND ability >= 4. None
    → hard-block the pull (-2000) instead of +600.
    Global icon+ability check per §2B — no set/persona/card-name
    hardcoding. Jedi/Sith/Dark Jedi carry [Warrior]; cantina aliens don't.

  ════ V148 (2026-05-28) ════

  V148 — ALWAYS ALLOW DONE/CANCEL WHEN ALL OPTIONS UNFAVORABLE
    Source: CombinedEvaluator.java + DecisionSafety.java
    Steve: "Rando should always have the option to hit done or cancel
    if he scores something and finds it not favorable. That's what a
    real player would do."
    Replay 37orjzqd6feo6igp / active game: Dr. Evazan deploy-location
    decision scored every site negative (Hoth -1330, Invisible Hand
    -2520) yet Rando deployed anyway. Root cause: the decision was
    CARD_SELECTION min=0 with a "click Done to cancel" button, but
    flagged noPass=true. The bot's canPass check (!isNoPass && min==0)
    and DecisionSafety.mustChoose() both read noPass=true and FORCED a
    pick — never selecting the Done/Cancel option (which is "select
    zero cards"). noPass here refers to the phase-level pass, not the
    in-selection Done button.
    Fix (two files, lockstep):
      - CombinedEvaluator: when best score < BAD_ACTION_THRESHOLD (-100),
        canPass = min==0 AND (!noPass OR prompt text offers
        done/cancel/optional/if-desired). Returns empty (cancel).
      - DecisionSafety.mustChoose(): returns false for min==0 decisions
        whose prompt offers Done/Cancel, so the empty (cancel) response
        isn't force-corrected back into a random pick. Card-name divs
        stripped before text-match to avoid false positives.
    Net: deploys/selections scoring below -100 with a Done button now
    abort cleanly instead of committing to the least-bad option. Fixes
    a whole class of "why did he deploy/pick that awful option" bugs.
    Guard rails: only fires when (a) best < -100, (b) min==0, (c) text
    actually offers a cancel. Forced decisions still force a pick.

  ════ V139–V147 family (2026-05-26 / 05-27 / 05-28) ════
  Battle-damage forfeit discipline, interrupt-timing gates, and a
  critical activation-suppression bugfix. All rando-side. Chosenone
  mirror pending (bot-vs-bot only). Replays cited inline.

  V139 (2026-05-26) — HIGH-VALUE CHARACTER FORFEIT PROTECTION (3 revisions)
    Source: CardSelectionEvaluator.java (evaluateForfeit + evaluateForceLossOrForfeit)
    Replay 1g9hxj4lh3cdknw6 T8: Darth Tyranus (P6 A5 unique) forfeited
    to satisfy attrition while cheap characters were available. Old
    protection (-25 valuable unique) was dwarfed by forfeit-efficiency
    bonuses.
    v1: bumped protections — valuable unique -25→-300, V37 high-pwr+abil
        -100→-400, low-power cheap-loss +15→+50.
    v2: bumped further (valuable unique -800, power≥5 -500, P6+A4 -1200)
        to dominate V67bd's +960 FORFEIT-COVERS-ALL.
    v3 (2026-05-27): DAMAGE-AWARE SCALE. v2 was too aggressive — Rando
        refused to forfeit even at 5+ damage, burning reserve cards
        instead. Now protections scale: damage burden ≤3 → full
        magnitude; >3 → 25% (forfeit-efficiency wins when a single
        high-fv forfeit beats burning 5+ reserve cards).
    Standing rule encoded: "always choose the least value characters
    to satisfy battle damage first."

  V140 (2026-05-26) — BATTLE ORDER COST-WAIVER
    Source: ActionTextEvaluator.java
    Battle Order text: drains cost 3 Force UNLESS you occupy a non-
    holosite battleground site AND a battleground system (or Battle
    Plan is on table). V104/V48 hard-blocked/penalized drains under
    Battle Order without checking this waiver. V140 fires before them:
    if waiver conditions met, drain is FREE (positive score, returns
    early). Steve: "if he satisfies battle order or battle plan he does
    not need to pay three force to drain."

  V141 (2026-05-26) — TRANSPORT INTERRUPT FLOOR (Elis Helrot, Nabrun Leids)
    Source: ActionTextEvaluator.java
    These interrupts: "draw destiny, use that much Force to transport
    or place in Lost Pile." Playing with insufficient force wastes the
    card. V141 hard-blocks (-2000) when forcePile < 4 OR reserveDeck < 1
    (can't draw destiny from empty reserve). Detection by title +
    generic game-text pattern. Includes v141ActionMatches guard so it
    only fires on actual transport plays, not generic actions sharing
    the cardId.

  V142 (2026-05-26, BUGFIX 2026-05-28) — WMAOP MODE GATING
    Source: ActionTextEvaluator.java
    We Must Accelerate Our Plans: deck-aware mode gating (deploy-phase
    only; block IAYF/Podracer/Effect modes when reserve lacks targets
    or BFS already on table). REPLACED the old hardcoded V29.7 "Blockade
    Flagship only" logic.
    CRITICAL BUGFIX 2026-05-28: V142's phase-gate fired on ANY action
    whose cardId mapped to WMAOP — including the generic "Activate
    Force" action (which inherits WMAOP's cardId when WMAOP is in hand).
    Result: Rando passed his ENTIRE activate phase every turn (Activate
    Force scored -1500 vs Pass +2). Replay j3k6sfd42gnyq40b. Fix:
    phase-gate now requires v142IsWmaopPlay (real WMAOP mode keyword in
    action text). Mode-specific blocks already required their mode flag.

  V143 (2026-05-26) — HARD BLOCK SMALL-DAMAGE FORFEIT
    Source: CardSelectionEvaluator.java (both forfeit paths)
    When attrition == 0 AND battle damage ≤ 2, NEVER forfeit a
    character — lose from pile instead. -9999 hard block dominates
    V67bd. Steve: "no need to kill a character on a site if 2 or less
    force is all that's needed."

  V144 (2026-05-26) — YOU ARE BEATEN MODE GATING
    Source: ActionTextEvaluator.java
    You Are Beaten reserved for battle-freeze (Mode 1, +500 in battle
    phase) or Cancel Uncontrollable Fury (Mode 3). Mode 2 (search
    Reserve for I Am Your Father) hard-blocked -2000 universally —
    Steve: "make sure it's used for its other game text, not to search
    for I Am Your Father."

  V145 (2026-05-26) — IMMUNE-TO-ATTRITION FORFEIT CORRECTION
    Source: CardSelectionEvaluator.java
    Replay 1g9hxj4lh3cdknw6 event 2003: Sidious (immune to attrition)
    forfeited for 2 battle damage because V67bd credited him with
    covering all 7 attrition+damage. But an attrition-immune character
    can only satisfy DAMAGE, not attrition. V145 detects "immune to
    attrition" in game text; when immune + attrition owed, V67bd's
    attrition-coverage bonus is skipped and only damage coverage scored.

  V146 (2026-05-27) — HIT CHARACTERS FORFEIT FIRST
    Source: CardSelectionEvaluator.java
    Steve: "Rando lost force from piles when he had 4 force to lose but
    one of his characters was hit. He has to lose hit characters first."
    ALREADY HIT bonus +200→+1500 (dominates pile loss ~+360). DEAD CARD
    +180→+1200. All V139 protections (forfeit/unique penalties) now
    gated on !card.isHit() — hit characters get no protection since
    they're already broken (fv reset to 0 by the weapon hit).

  V147 (2026-05-28) — I AM YOUR FATHER: DON'T SEARCH EMPTY LOST PILE
    Source: ActionTextEvaluator.java
    Replay 37orjzqd6feo6igp T2: Rando lost 1 Force to deploy Vader's
    Lightsaber from Lost Pile when the saber wasn't there (only Prepared
    Defenses was). V147 scans the actual Lost Pile; if Vader's
    Lightsaber isn't present, hard-blocks the Lost-Pile deploy mode
    (-2000). The free [download] from Reserve mode is preferred anyway.

  V134 GUARD (2026-05-28) — Odin Nesloor action-match guard
    Source: ActionTextEvaluator.java
    Same misfire class as V142: V134's Odin-Nesloor MOVE-phase block
    could fire on a generic move action carrying the cardId. Now
    requires action text to mention Odin Nesloor / transport / relocate.

  V29.7 REMOVED (2026-05-26) — WMAOP Blockade-Flagship stipulations
    Source: ActionTextEvaluator.java
    Deleted the hardcoded "WMAOP is for Blockade Flagship site ONLY"
    penalties (-400/-500) that universally blocked the card in any
    non-Blockade-themed deck. Superseded by V142 deck-aware gating.

  LOCAL: DECK LIST ORDERING FLIPPED TO LIGHT-FIRST (2026-05-26)
    Source: gemp-swccg-async DeckRequestHandler.java
    Local-only (not for upstream): emit light decks before dark in the
    deck-list XML so the game-start dropdown defaults to a light deck
    (Steve plays light vs Rando most often).

  ════ V136 family (2026-05-26; opp-undercover detection 2026-06-01) ════

  2026-06-01 OPP UNDERCOVER DETECTION (Steve, Jabba's Palace pile-in):
    Steve: "Rando deployed a heap of guys on Jabba's Palace while I had a
    spy blocking him."
    Replay: Jyn Erso (asdf's spy) at Jabba's Palace: Audience Chamber.
    Rando piled Hondo Ohnaka, Mara Jade, Jango Fett successively at
    Audience Chamber, each scoring +1155 in the deploy-site selection.
    Decomposition of the +1155:
      base 50 + V29 SHIP CHAR ON GROUND -200 + V29.6 BG drains +50 +
      OBJECTIVE LOCATION +150 + **V136 §A +500 + §B +300** +
      V23 FORCE DRAIN +30 + V29.7 ABILITY +5 + V29 REINFORCE SOLO +150 +
      V29.5 BUDDY +40 + V29.7 BATTLEGROUND +80 = +1155.
    The dominant chunk (V136 §A +500) came from line 324
    "if (powerPass && bodyPass) return 500f". powerPass evaluated as
    teamPower(3) >= oppPower(0) because the engine's
    ModifiersQuerying.getTotalPowerAtLocation excludes undercover
    characters from the power tally. Jyn Erso's presence is invisible
    to that call. V136 thought Audience Chamber was a free uncontested
    win.
    None of the three piled deploys could actually drain (Jyn's
    undercover blocked it), Force was wasted, Rando lost the game.
    Pre-existing detection of opponent spies at a site exists in two
    places:
      • V67f SPY-ONLY (CardSelectionEvaluator line ~5790) — at -100,
        and lives inside the V41 MOVE-DESTINATION block, never reached
        in deploy-site selection.
      • V62 SPY DILUTION (line ~5591) — for OUR spy at site, not
        opponent's.
    Neither covered V136 §A. Gap.
    Fix: surgical edit in CharacterDeploySiteEvaluator.computeTeamViability,
    immediately after computing oppPower. Walk friendliesAtSite (the
    misnamed "all cards at site" list already used by the oppHasWeapon
    scan below), filter to characters owned by opponentId, check
    isUndercover(). If any opponent undercover found AND oppPower == 0
    (meaning only spies, no real opponents at the site), return -1000f
    with a "V136 §A SPY-BLOCKED" LOG.warn. Math: Audience Chamber
    goes from +1155 to about +1155 - 500 (lost the +500) - 1000 (added
    -1000 instead) = wait that's wrong, let me redo. §A returns the
    -1000 (replacing what would have been +500), so the total swing on
    §A is -1500. New Audience Chamber total = +1155 - 1500 = -345.
    Pass scores ~5. -345 < 5 → Rando picks elsewhere or passes.
    Constraint: when oppPower > 0 (spy alongside real opponents), the
    branch DOES NOT fire — the existing contested-fight logic runs
    normally because we're going to battle anyway and the drain-blocking
    is moot.
    Constraint: when our own deploying card is itself an undercover
    spy, this branch still fires (-1000) — that's slightly aggressive
    because two spies at the same site is sometimes intentional (drain
    chain). Acceptable false-positive; if it bites in a future replay,
    add `!deployingCardIsSpy` to the gate condition.
    Edits old V136 §A logic only. No new V-tag (Steve's "look at old
    logic and edit" + "avoid splintering" directives).



  V136 (2026-05-26) — UNIFIED CHARACTER DEPLOY SITE EVALUATOR
    Source: NEW common/strategy/CharacterDeploySiteEvaluator.java
            + wiring in rando/evaluators/DeployEvaluator.java
            + wiring in rando/evaluators/CardSelectionEvaluator.java
    Consolidates V90 + V67aj + V67al + V122 + V67as into one
    static method evaluateSite(). Resolves §2A regressions caused
    by overlapping character→site rules silently dominating each
    other.
    Architecture: lives in ai/models/common/. Side-specific
    callers (rando + chosenone) resolve all dependencies into
    primitives and pass them in: playerId, isObjectiveRelevantSite,
    friendlyHand, availableForceForDeploys, currentTurn,
    deckShipCount, perSiteEffectActive. No rando.* / chosenone.*
    imports.
    Four scoring components:
      §A team viability (±2000): power ≥ opp, ability ≥ 4, body
         count vs opp weapon; buddy-in-hand lookahead with
         asymmetric resolver (lower-ability character "is the
         buddy" → scores 0 → deploys first).
      §B strategic position (±700): BG +100, obj-relevant +200,
         NBG penalty two-tier (-500 turn 1-2, -300 turn 3+) with
         override for isObjectiveRelevantSite || perSiteEffectActive,
         uncontested over-stack 5/10/15 power → -200/-400/-700,
         per-site ability saturation cap (turn 1 cap 10, turn 2
         cap 13, turn 3 cap 17, nullified turn 4+).
      §C modifiers (±10): opp weapon -10, our weapon +10.
      §D site-count gate (-700): max 2 ground BGs + max 2 systems,
         turn 1-2; overrides for isObjectiveRelevantSite (both
         caps) + deckShipCount >= 5 (systems only).
    Method signature primitives chosen for side-symmetry. Caller
    side-specific code maintains the per-site effect detection
    patterns: "for each location", "for each battleground",
    "for each battleground you occupy", "per site you control",
    "for each site", "for each system", "for each docking bay".
    Phase 1: rando-side wired (deploy phase + hand-deploy via
    CardSelection). V90/V67aj/V67al/V122/V67as commented out via
    `if (false /* SUPERSEDED V136 */ && ...)` so blocks remain
    in tree but become unreachable. Easy revert: remove the
    `false &&` prefix to restore.
    Phase 2 follow-ups: mirror to chosenone evaluators; wire the
    three stubbed primitives (isObjectiveRelevantSite,
    deckShipCount, perSiteEffectActive); sibling V137 for
    MoveEvaluator V34 power-comparison gate (Kylo→D'Qar bug);
    sibling V138 for ship/vehicle/pilot deploy logic.
    Review process: council engineer (qwen3-coder:30b via
    deliberate endpoint) APPROVE-WITH-CHANGES; subagent
    NEEDS-REWORK on v2 spec (7 fixes), all applied in v3 spec.
    Specs: /tmp/V136_SPEC_V3.md (full), /tmp/V136_RULE_CATALOG.md
    (15-rule catalog), V136_DEPLOY_LOG.md (revert plan +
    TODO list).

  ════ V130–V135 family (2026-05-26) ════

  V130 (2026-05-26) — DECK-AWARE PULL HELPERS IN DeckOracle (rando + chosenone)
    Source: DeckOracle.java (both)
    Added two static-style query methods to enable downstream deck-state
    decisions:
      - countMatchingInDeck(SwccgGame, playerId, Filter) — counts cards
        matching Filter across reserve + hand + used + force + lost +
        on-table. Backbone of V131 Tier 1 (hard-block-when-not-in-deck).
      - countMatchingInHandOrTable(SwccgGame, playerId, Filter) — counts
        matches in hand + table only. Backbone of V131 Tier 2 (soft-
        downgrade when target already satisfied).
    Pure helpers — no scoring side effects. Both sides mirrored.

  V131 (2026-05-26) — DECK-AWARE PULL DETECTION (three-tier gate on V67ai)
    Source: ActionTextEvaluator.java (rando + chosenone)
    Wraps V67ai LOCATION-tier pull bonuses in real deck-state checks
    instead of firing whenever a location-keyword substring matched.
    Three tiers:
      - Tier 1 HARD BLOCK (-9999): target proven not in deck at all
        (countMatchingInDeck == 0). Effect would fail and reveal reserve.
      - Tier 2 SOFT DOWNGRADE (-2000): target IS in deck but already
        satisfied (countMatchingInHandOrTable >= 1). Pulling more is
        wasted; neutralizes V67ai bonus.
      - Tier 3 EXISTING (no change): target needed → V67ai bonus fires
        as before.
    Also corrects V67l substring-match misfire: only fires LOCATION tier
    when parsed target actually resolves to a location-family noun (not
    weapon, character, etc). Fixed the "Cunning Warrior pulled a
    lightsaber while game text mentioned Cloud City Corridor" bug.
    FAIL-OPEN throughout — noun unparseable, filter null, or deck data
    missing → fall through to existing behavior. Never hard-block on
    ambiguity.

  V132 — DROPPED (was: lower allow-opponent-activate baseline 50 → 10)
    Per Steve (2026-05-26): allowing opponent to activate force is
    normal SWCCG play, not a last-resort. Original 50.0f baseline kept.
    Mirror revert applied to chosenone too.

  V133 — DROPPED (was: same-persona buddy bonus +1000)
    Per Steve (2026-05-26): narrow regex-on-game-text detection only
    caught ~5% of cards with explicit "deploys to same site as X" text.
    Broader buddy concept Steve actually wants lives in upcoming
    consolidated V136 master deploy rule (battle-math team viability +
    universal solo-low-ability gate in one evaluator). Comments in both
    rando and chosenone CardSelectionEvaluator point at V136 plan.

  V134 (2026-05-26) — ODIN NESLOOR 5-FORCE FLOOR (MOVE phase)
    Source: ActionTextEvaluator.java (rando + chosenone mirror)
    Steve's standing rule: must have 5 force in force pile to play Odin
    Nesloor during MOVE phase. Odin Nesloor & First Aid lets multiple
    characters reposition cheaply but is wasted when we lack force to
    actually drain at the destination next turn. Hard-block -9999 when
    forcePile < 5 during MOVE.
    TODO (Steve): migrate to Filters.persona(Persona.ODIN_NESLOOR) once
    Persona enum is verified for this card; title substring is the
    fallback.
    NOTE: Odin Nesloor is LIGHT-side; chosenone is the side that
    actually fires V134 in real play. The rando-side mirror is the
    dead-code symmetric copy, kept for V-tag symmetry.

  V135 (2026-05-26) — SELF-MOVE-TO-FRIEND REQUIRES COMPANION
    Source: MoveEvaluator.java (rando + chosenone mirror)
    Some characters' game text says "may move to same site as <X>" —
    a self-move intended to put them next to allies. Bug 7a: such a
    character was moved alone to a destination with zero friendlies,
    landing in danger isolated. V135 detects the self-move-to-friend
    pattern in cardToMove's game text and penalizes (-2000) when the
    destination has zero friendly characters (excluding cardToMove
    itself).
    Generalized beyond any specific persona — works universally.
    FAIL-OPEN: if blueprint or game text missing, no penalty.
    Rando version anchored inside V37 NO RETREAT block (destLoc37);
    chosenone version anchored inside V34 destination-aware contest
    block (v34Dest) — structural deviation reflects each bot's
    existing destination-parsing scaffolding.

  Review process (V130-V135 bundle):
    - Claude subagent code review of chosenone mirrors: SHIP verdict
    - Council engineer review (qwen3-coder:30b via deliberate endpoint
      with bridge tools): APPROVE
    - V132 reverted after Steve's gameplay-semantics correction
    - V133 dropped after V136 architectural decision (subagent flagged
      §2A regression risk in original spec; deferred to dedicated
      session)
    - V90, V122, V133, V67aj, V67as, V67al, V34, V35 are slated for
      consolidation into V136 BATTLE-MATH DEPLOY EVALUATOR in a later
      session — see /tmp/V136_DEPLOY_SITE_EVALUATOR_SPEC.md draft.

  ════ V129 family (2026-05-24) ════

  V129 (2026-05-24) — AFA DETECTION MIRROR FOR LIGHT-SIDE STACKED-PILE DISCIPLINE
    Source: ActionTextEvaluator.java (rando + chosenone)
    Real incident: Steve playing Rey deck (Skywalker Saga Epic Event)
    observed the light-side bot deploying 4 shields with zero pacing
    discipline. Root cause: V97 (pull-before-activate exclusion), V100
    (location-pull-before-character-deploy exclusion), V102 (K&D
    activation cap), and V124 (4th-slot hard-block) all gated their
    detection on the substring "Knowledge And Defense" only. The
    light-side equivalent — Anger, Fear, Aggression (V) — is the same
    stacked-pile mechanic but never triggered the gates. Net effect:
    light-side bot had zero 4th-shield discipline when running the
    AFA-based Rey/Skywalker Saga deck.
    V129 changes (symmetric in both bots):
      - V97 exclusion:  added !title.contains("Anger, Fear, Aggression")
      - V100 exclusion: same pattern
      - V102/V124 gate: changed to OR detection of K&D || AFA
      - Renamed local boolean isKnDShieldPlay -> isStackedPileShieldPlay
        for clarity (now covers both K&D and AFA)
    Review process (orchestrator pattern):
      - Claude subagent: AGREE, recommended rename + flagged sibling fix
      - Engineer (qwen3-coder:30b) via council vote: AGREE
      - Voice of reason (llama3.3:70b) via council vote: AGREE
      - Triple-verified before any code change.


  ════ V128 family (2026-05-22) ════

  V128 (2026-05-22) — REMOVE DEPLOY/BATTLE FROM SERVER AUTO-PASS DEFAULT
    Source: GameRequestHandler.java
    Per Steve's feedback_autopass_phases.md: Deploy and Battle are
    DECISION PHASES — the user (or bot) must make real choices there,
    never auto-pass. The upstream default included them; clients
    without an autoPassPhases cookie fell through to that default and
    every Deploy/Battle prompt became auto-pass-eligible.
    Three-way audit confirmed the bug exists in BOTH upstreams:
      - origin/master (PC public):    ACT+CTRL+DEPLOY+BATTLE+MOVE+DRAW
      - pc-private/master (source):   ACT+CTRL+DEPLOY+BATTLE+MOVE+DRAW
      - our fork (pre-V128):          ACT+CTRL+DEPLOY+BATTLE+MOVE+DRAW
    Old-GUI users masked the bug by actively clicking Pass buttons;
    Unity epic-duel users hit the server's timeout-and-auto-pass path
    before they could decide. V128 deletes DEPLOY and BATTLE from the
    default. MOVE intentionally kept — Steve's rule allows MOVE auto-
    pass with engine-side autoPassEligible=false override for the
    spy-move case. Could be PR'd upstream as a real bug fix.
    Note: this addresses SERVER-SIDE timeout auto-pass. The Unity
    client's separate "Skip Turn" client-side mechanism is a different
    bug — needs investigation of swccgpc/epicduel Unity source.


  ════ V127 family (2026-05-22) ════

  V127 (2026-05-22) — FORCE-LOSS CONSOLIDATION (V13 priority restored)
    Source: CardSelectionEvaluator.java (rando + chosenone partial)
    Real incident: Steve reported Rando consistently losing duplicate
    hand cards instead of pile cards even with reserves <= 10. Audit
    revealed a §2A regression — V101 (added 2026-05-20 with blanket
    -500 hand penalty) silently dominated V29.8's conditional
    duplicate-detection (+200) and life-force-low logic (+80).
    Score math at the failing boundary:
      Duplicate INTERRUPT in hand, reserves > 10:
        V101 HAND LAST: -500
        V29.8 HAND PROTECT (healthy): -500
        V29.8 HAND interrupt: +50
        V29.8 DUPLICATE: +200
        Net: -750
      Same scenario, used pile card: +1000
      -> Hand lost by 1750. Duplicate bonus utterly dominated.
    Pre-V21 era code (CardSelectionEvaluator.java.v13.backup) had the
    explicit V13 spec:
      1. Duplicates in Hand - BEST
      2. Used Pile - SECOND
      3. Reserve Deck - THIRD
      4. Hand non-duplicates - FOURTH
      5. Force Pile - LAST RESORT (need Force to deploy)
    V101's flat magnitudes silently inverted this. V127 restores it.
    Changes in rando/CardSelectionEvaluator.java:
      - DELETE V101 block (~lines 3320-3342)
      - DELETE V119 block in evaluateForceLossOrForfeit (V119 was V101's mirror)
      - V29.8 zone scoring updated:
          Used pile healthy: +400 (was V29.8 +500 + V101 +500 = +1000)
          Reserve healthy:   +300 (was V29.8 +500 + V101 +300 = +800)
          Force pile healthy: +100 (was +400)
          Hand healthy non-dup: -300 (was -500 + V101 -500 = -1000)
          Hand low (reserves<=10): +400 (was +80 + V101 -500 = -420)
      - Duplicate bonus: +200 -> +800 (so duplicate hand beats piles when healthy)
      - V29.8 mirrored into evaluateForceLossOrForfeit (combined battle handler)
    Chosenone partial: V101 deleted (matching rando), V25 zone scoring
    magnitude update PENDING as separate follow-up since chosenone uses
    V25 tags with different baseline magnitudes than rando's V29.8.



  V126 (2026-05-22) — EXPANDED STARTING-EFFECT BONUSES
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Per Steve: "Evil Is Everywhere should deploy if Revenge of the
    Sith on table. First Strike is a good choice. Any effect that
    adds to force generation should get a bump. I basically want to
    expand the starting effect bonus thing."
    Added three bonus paths to the existing V22 starting-effect
    block, all type-by-API detection where possible:
      V126a (+500): game text contains "initiate battles for free"
        OR title contains "first strike". Catches both Special
        Edition First Strike and the V Set 12 variant.
      V126b (+400): game text matches regex
        "force\s+generation\s*(?:is|are|of|by)?\s*[+]?\s*[1-9]"
        OR contains both "force generation" and "+". Bumps the
        prior V22 partial +25 to a meaningful +400.
      V126c (+600): game text references "[Episode I]" + "Dark
        Jedi" restrictions AND Revenge of the Sith is on table.
        Catches Evil Is Everywhere ↔ ROTS pairing.

  V22 ADDENDUM (2026-06-19) — SHADOW COLLECTIVE PAYOFF STARTING-EFFECTS
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Per Steve: "You'll Be Dead and Inconsequential Losses effects
    should be added to the list of effects to play when given the
    option as effects pulled from a starting interrupt."
    Background: in a live Shadow Collective game Rando never deployed
    his engine. The starting interrupt offers Effects from Reserve
    Deck at turn 0; Rando picked First Strike / Security Precautions
    but skipped You'll Be Dead! (the per-battleground blaster drain
    payoff, misread by V22 as a location-puller for only +130) and
    Inconsequential Losses. Both are now title-matched and given
    +500 — the same "strongly preferred" tier as V126a First Strike
    — so they win the turn-0 effect-selection slots.
      You'll Be Dead! (200_114): opponent loses 1 Force per
        battleground site you control with a non-permanent blaster.
      Inconsequential Losses (9_126): forfeit a non-lightsaber
        weapon at value 3; forfeited weapons go to Used Pile.
    Added under the V22 PREFERRED STARTING EFFECT block (NOT a new
    version — extends the existing V22 preferred list per the
    update-old-rule discipline). Gated to turnNumber <= 0. Mirrored
    in BOTH routing paths: evaluateUnknown (cardIds populated) and
    evaluateReserveDeckSelection ("deploy N Effects for free"
    reserve-pick). Mirrored in chosenone.


  ════ V125 family (2026-05-22) ════

  V125 (2026-05-22) — V120 EXACT-MATCH FIX (.equals → .contains)
    Source: ActionTextEvaluator.java (rando + chosenone)
    V120's title equality check searched for "vader's lightsaber"
    but the real card title is "•Darth Vader's Lightsaber (V)"
    (uniqueness bullet + Darth prefix + (V) suffix). Silent miss
    — V120 never logged in replay liuorncol0ku2qva. V125 switches
    to bidirectional contains() match (title contains action-text-
    name OR vice versa).


  ════ V124 family (2026-05-22) ════

  V124 (2026-05-22) — K&D PARENT-ACTION HARD-BLOCK AT 4TH SLOT
    Source: ActionTextEvaluator.java (rando + chosenone)
    Replay liuorncol0ku2qva: 4 shields deployed by turn 2. V105/
    V107 in the sub-decision correctly hard-blocked all 4th-slot
    candidates at -5000, but the K&D parent "Play a card" action
    was +50 ("slots available"), so the AI committed to playing
    K&D and the sub-decision was FORCED to pick the least-bad
    shield (Resistance at -5050).
    V124 closes the gap at the parent level: count friendly
    DEFENSIVE_SHIELD cards in play; if 3+ AND ShieldStrategy.
    prefers4thSlot() returns null (no V105/V107 trigger active),
    the K&D "Play a card" parent action gets -3000. AI never
    starts the sub-decision.


  ════ V123 family (2026-05-22) ════

  V123 (2026-05-22) — V66 STOPWORD GUARD (generic category words)
    Source: ActionTextEvaluator.java (rando + chosenone)
    Replay liuorncol0ku2qva diagnostic from /root/nohup.out:
      "V66 MEMORY: No match for 'location' in RESERVE_DECK —
       search will FAIL and reveal zone (-9999)"
    V66 MEMORY AUDIT was hard-blocking every Hunt Down V site
    pull. The generic regex
        "(?:Deploy|Take|\\[Download\\]) an? ([a-z]+) ?"
    captured "location" from "Deploy a location from Reserve Deck"
    and treated it as a card title. validatePull looked for a
    card LITERALLY titled "location" in reserve, found none,
    returned WILL_FAIL → -9999 hard-block.
    V123 adds a stopword list of generic category nouns:
      location, site, battleground, system, sector,
      ship, starship, vehicle, transport, fighter,
      weapon, lightsaber, blaster, bowcaster, device,
      character, alien, droid, jedi, sith, padawan, inquisitor,
      senator, pilot, warrior, soldier, leader, admiral, general,
      trooper, officer, rebel, imperial, scout, spy,
      effect, interrupt, objective, epic, shield, card
    When the captured keyword is a stopword, V66 defers to
    downstream V67ai (tiered objective bonus) and V82 (source-
    card site-pull match) — both do criteria-aware validation
    that V66's literal-title-lookup can't.
    Lesson learned: shipped code is not the same as working code.
    Verify via grep against /root/nohup.out after restart, look
    for the V-tag's log line. If the rule never fires it's not
    shipped, it's wishful thinking.


  ════ V122 family (2026-05-22) ════

  V122 (2026-05-22) — V90 NO-SUICIDE-DEPLOY MIRROR (CardSelection)
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Audit category 5 (Detection-path mismatch) finding from the
    K2-built rule audit xlsx. V90's actionText-contains-location-
    title requirement means it silently skips the location-pick step
    in CardSelectionEvaluator. Phasma-at-Shield-Control suicide loop
    could recur. Same fix pattern as V99-CS, V89-CS, V112, V117.
    V122 logic at the candidate-location level:
      - deploying card is CHARACTER
      - for the candidate location, scan all cards at that location
        through Filters.character_with_a_weapon
      - if any opponent-owned armed character exists AND no friendly-
        owned armed character exists → -1500
    Mirrored in chosenone.


  ════ V121 family (2026-05-22) ════

  V121 (2026-05-22) — V86 NEIMOIDIAN-PILOT MIRROR (CardSelection)
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Audit category 5 (Detection-path mismatch). V86's V86.1 guard
    requires actionText to contain "aboard"/" to "/" on " — generic
    "Deploy" actions skip this and the location-pick step has no V86
    mirror. Result: Neimoidian pilots can land on ground sites via
    the generic deploy path.
    V121 mirrors at the location-pick step:
      - under Invasion objective
      - deploying card is Species.NEIMOIDIAN + Icon.PILOT
      - friendly capital_starship exists on table (Filters)
      - candidate is NOT the capital ship → -1500
      - candidate IS the capital ship → +300
    Mirrored in chosenone.


  ════ V120 family (2026-05-22) ════

  V120 (2026-05-22) — UNIVERSAL WEAPON-PULL CRITERIA BLOCK
    Source: ActionTextEvaluator.java (rando + chosenone)
    Per Steve: "We need to hard block deploy from reserve deck or
    with an interrupt when a character already has a weapon."
    V115 closed the hand-deploy gap. V120 closes the FIFTH gap that
    the four-way one-weapon stack (V67ay + V67aq + V67ar + V70) plus
    V115 all missed: Effect / Interrupt / Objective top-level actions
    that deploy a weapon FROM RESERVE (e.g. "Deploy Vader's Lightsaber
    from Reserve Deck using •I Am Your Father (V)"). These score in
    ActionTextEvaluator.evaluate() before DeployEvaluator ever runs.
    Logic:
      1. Detect action text "Deploy <NAME> from reserve" via regex
      2. Find that title's blueprint in gameState.getAllPermanentCards
         (covers all of Rando's zones — hand, reserve, used, lost,
         table)
      3. Confirm category is WEAPON
      4. Parse "Deploys on X" criteria via
         CardSelectionEvaluator.v70ExtractDeployCriteria (helper
         widened to package-private static in V115)
      5. Count criteria-matching armed/unarmed Rando characters on
         table using v70CharacterMatchesCriteria
      6. Hard-block -9999 when matchingUnarmed == 0
    Hunt Down replay ig4n5m5nzc4gronn: Rando fired IAYF four times
    trying to pull Vader's Lightsaber while Vader was already armed
    with two Dark Jedi Lightsabers. Each attempt failed and revealed
    the reserve deck. V120 + V115 together close the loop on Steve's
    standing "no two weapons per character" rule (asked 5+ times
    over the project lifetime).


  ════ V119 family (2026-05-22) ════

  V119 (2026-05-22) — V101 ZONE PRIORITY MIRROR (combined battle handler)
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Per Steve's incident report on Hunt Down replay ig4n5m5nzc4gronn:
    "Rando did lose from hand in the last turn of this Hunt down
    replay. Recheck please."
    Confirmed: msgs 1492-1516 show 4 hand losses (Imperial
    Enforcement, First Strike, Tarkin's Bounty, Emperor's Personal
    Shuttle) interleaved with reserve and used-pile losses — wrong
    zone ordering.
    Root cause: V101 (Used > Reserve > Hand zone priority, added
    2026-05-20) lives in evaluateForceLoss only. The combined
    "Choose Force to lose or a card from battle to forfeit" decision
    routes through evaluateForceLossOrForfeit, which had no
    zone-priority logic. Audit category 5 ("Detection-path mismatch"
    in the xlsx audit), same architectural pattern as V99 → V99-CS.
    V119 mirrors V101's three-way bonus into the combined handler's
    isForceLosSOption branch:
      - zone contains "USED"    → +500
      - zone contains "RESERVE" → +300
      - zone contains "HAND"    → -500
    Layered on top of V22.3 (forfeit-first) and V118 (small-damage
    save-character). Net effect: Used Pile gets used up first, then
    Reserve, hand stays intact until both are dry.


  ════ V118 family (2026-05-22) ════

  V118 (2026-05-22) — SAVE CHARACTERS FROM SMALL BATTLE DAMAGE
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Per Steve: "Forfeit weapons and devices first. But when the
    battle damage, not attrition damage, is 2 or less, we need to
    lose from hand or reserves to save the character from being
    lost. Unless they are hit of course. Characters are typically
    worth more than 2 force to save from dying."
    In evaluateForceLossOrForfeit (the combined battle-end decision),
    when damageRemaining is 1 or 2 AND attritionRemaining is 0:
      - isForceLosSOption candidates (cards from HAND / RESERVE /
        FORCE_PILE / USED_PILE) → +200 boost
      - non-hit CHARACTER forfeit candidates → -500 penalty
    Hit characters skip the penalty since their fv is reset to 0 by
    the weapon hit — losing them is free anyway.
    Strengthens V67bh (damage ≤ 3, fv ≥ 4) and V67t (damage ≤ 2,
    fv ≥ 2) which only partially protected high-value characters.
    V118 protects ALL non-hit characters at the ≤ 2 damage threshold.
    Attrition damage still REQUIRES forfeit (cannot be paid by
    force loss in SWCCG rules), so V118 only fires for pure-damage
    situations.


  ════ V117 family (2026-05-22) ════

  V117 (2026-05-22) — UNIVERSAL 4TH-SHIELD HARD BLOCK (evaluateUnknown)
    Source: CardSelectionEvaluator.java (rando + chosenone)
    Per Steve: "We need to hard block deploy from Knowledge and
    Defense effect when 3 shields already on table. The conditions
    we set for that fourth shield must be met before deploying."
    V105/V107 already enforce this in the defensive-shield-selection
    path (line ~7100 rando). But K&D's "play a card" from a mixed
    stacked pile (shields + non-shields, isShieldSelectionByContent
    returns false at <50% shields) routes through evaluateUnknown
    instead — bypassing V105/V107 entirely. V112 had patched the
    Battle Order/Plan case but the gap remained open for other
    shields.
    V117 closes it: in evaluateUnknown, when the candidate's
    category is DEFENSIVE_SHIELD, count friendly defensive shields
    already in play (isInPlay zone). If count >= 3, consult
    ShieldStrategy.prefers4thSlot() to see if this exact shield is
    the V105 (Battle Order/Plan) or V107 (Resistance/Ultimatum)
    preferred 4th-slot pick. If preferred → +2000. Otherwise →
    -9999 hard block. No V105/V107 trigger active → -9999 hold (4th
    slot closed indefinitely per Steve's 2026-05-20 standing rule).


  ════ V116 family (2026-05-22) ════

  V116 (2026-05-22) — GUARANTEED +100 FLOOR FOR RESERVE-DECK PULLS
    Source: ActionTextEvaluator.java (rando + chosenone)
    Per Steve: "The game gives players an option to deploy anything
    from reserve deck should be +100 at least. In the case of the
    objective, it says it's an option to deploy from reserve. Same
    with some of the effects. Not sure why they aren't firing when
    they are lit up green as options to deploy."
    Safety net: at the TOP of evaluate() — before any rule has a
    chance to block or redirect — any action whose text contains
    "from reserve deck" or "[download]" gets an unconditional +100
    baseline. V60 / V67ai / V82 / V114 still apply additional
    positive scoring on top (V67ai objective tier = +2000, V82 site
    pull = +2500). Even if those downstream handlers fail to fire
    for any reason, the floor guarantees the AI sees the action as
    a positive option (above the pass score, above competing
    neutral actions).


  ════ V115 family (2026-05-22) ════

  V115 (2026-05-22) — CRITERIA-AWARE WEAPON HAND-DEPLOY BLOCK
    Source: DeployEvaluator.java (rando + chosenone),
            CardSelectionEvaluator.java (helper visibility widened)
    Real incident replay ig4n5m5nzc4gronn (2026-05-21): Lord Vader
    received TWO Dark Jedi Lightsaber V's in one deploy phase
    (cards 269 + 270 both attached to Vader cardId 267). Steve's
    "no character should have two weapons" rule had FIVE different
    enforcement layers, none of which fired correctly for this case:
      - V25 (CardSelectionEvaluator target-pick): fires per-target.
        Vader was the ONLY valid target for DJL V (criteria: "warrior
        of ability > 4"). Even with -9999 on Vader, AI was forced to
        pick him because no other candidate existed.
      - V33 (DeployEvaluator): requires actionText to contain target
        name. Hand deploys have generic action text "Deploy" — the
        contains() check always failed → skipped.
      - V67aq (DeployEvaluator universal): counted ALL armed/unarmed
        friendlies. Tarkin was unarmed → "unarmed > 0 → ALLOW +300."
        Tarkin (ability 3, Admiral) isn't a valid target for DJL V
        but V67aq didn't know that.
      - V67ar (DeployEvaluator): only fires for reserve-deck pulls,
        not hand deploys.
      - V70 (CardSelectionEvaluator): lives in reserve-pick +
        evaluateUnknown paths. Hand deploys never reach it.
    V115 fix: V67aq becomes criteria-aware. Parses the weapon's
    "Deploys on X" criteria from game text using V70's existing
    helpers (v70ExtractDeployCriteria + v70CharacterMatchesCriteria —
    visibility widened from private static to package-private static
    so DeployEvaluator can call them). Counts ONLY criteria-matching
    armed/unarmed friendlies. Hard-blocks -9999 when criteria parsed
    AND v115MatchUnarmed == 0. Two cases covered:
      (a) v115MatchArmed > 0 && v115MatchUnarmed == 0
          → every eligible target already armed (2nd weapon attempt)
      (b) v115MatchArmed == 0 && v115MatchUnarmed == 0
          → no eligible target on table (e.g. Vader's Lightsaber
          deploy attempt while Vader is still in hand or in
          destiny zone)
    Legacy V67aq paths (no parseable criteria) retained as fallback.
    Same fix mirrored in chosenone DeployEvaluator.


  ════ V114 family (2026-05-21) ════

  V114 (2026-05-21) — DELETED OBSOLETE "DEPLOY FROM RESERVE - RISKY" CATCH-ALL
    Source: ActionTextEvaluator.java (rando + chosenone)
    Real incident: Hunt Down V replay dc8n6dl9s88rqycz (2026-05-12).
    Rando ignored the objective's once-per-turn "Deploy a location
    from Reserve Deck" action for the entire game. Even though
    Cloud City / Malachor battleground sites were in the reserve
    deck and the V67ai tier-1 OBJECTIVE bonus (+2000) should have
    been awarded, the action never scored above competing K&D play
    actions.
    Root cause: a V21-era catch-all
        else if (actionText.contains("Deploy") &&
                 actionText.contains("from")) {
            action.addReasoning("Deploying from reserve - risky",
                                BAD_DELTA);  // -30 originally
        }
    appeared at line 2725 (rando) / 1921 (chosenone). It matched
    EVERY "Deploy X from Y" action text — including "Deploy a
    location from Reserve Deck" — and short-circuited the else-if
    chain BEFORE the V60/V67ai block at line 3120 could fire. So
    the +2000 OBJECTIVE-tier bonus never landed.
    History:
      V21 (2026-01-15, initial Rando) — score = BAD_DELTA (-30)
      V29.13 (2026-03-16, same author) — score = -10 (softened)
      V114 (2026-05-21) — DELETED ENTIRELY
    The original V21 intent was correct for its era (failed reserve
    searches reveal the deck). But between V21 and V114 we added:
      V60 — DeckOracle-aware reveal-risk guards (≤2 cards in reserve
            → -400, action failed 2× → -9999, named target absent →
            -9999)
      V66 — memory audit for generic pulls (target proven not in
            reserve → -9999)
      V67ai — tiered positive scoring (+2000 OBJECTIVE, +1800 EFFECT,
            +1600 INTERRUPT, +1400 hand)
      V82 — source-card site-pull matching (+2500 for site/location/
            battleground/docking-bay/system/sector pulls)
    All four use real DeckOracle data instead of a blanket guess.
    Steve's standing rule (feedback_reserve_deck_pulls.md): "Reserve
    Deck pull effects always fire every turn; only stop after 2
    consecutive failures" — exactly what V60+V66+V67ai+V82 enforce.
    V114 deletes the obsolete catch-all in both rando (line 2725)
    and chosenone (line 1921). Pull actions now reach V60/V67ai
    and score correctly.
