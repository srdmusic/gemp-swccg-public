# Rebel Strike Team family (8_78 + 501_94): DEFERRED for designed schema extensions (2026-07-27)

NOT TWINS (third false pair caught tonight; the gap matrix's "twin of 8_78" note is wrong
beyond the single shared Bunker-blown-away front leg). Both flip laws are largely
inexpressible in the current flipLocationRules schema. Required primitives, each real:

1. BLOWN-AWAY EVENT relation: classic front route A and the V's SOLE flip route trigger on
   isBlownAwayLastStep(Bunker), an event, not a spottable state. The schema has no event
   relation; the classic BACK additionally needs the isBlownAway STATE check (permanent
   back lock) which IS spottable but has no key.
2. PHASE-WINDOW field: classic front route B fires only during YOUR move phase; the classic
   flip-back only during the OPPONENT's move phase. FlipLocationRule.phase is
   preFlip|postFlip only; there is no game-phase gate.
3. PER-LOCATION ACTOR-PAIR minimum: "control 3 exterior Endor sites, each with TWO of your
   Rebel scouts" — a per-site actor-count floor with a co-actor requirement and a spotting
   asymmetry (the inner with() takes no SpotOverride, so the second scout must be fully
   active). Not expressible as onTable (global count) or controlWith (single actor).

The complete extraction packet is preserved in the session record (agent output
2026-07-27): full citations for both printings, the V's playtest caveats (NO_PROFILE,
setTestingText, event-only flip, dead download until an unfetchable Bunker arrives, the
back flip-back reachable only via opponent Bunker conversion and permanently stable after
blow-away), the V back's attrition immunity covering OPPONENT scouts (gameplay landmine),
supporting-card ids, and near-miss fixtures for every leg.

Design note for the next session: primitive 1 could generalize the utinniEffectCompleted
pattern (state-reader relations); primitive 2 is a new optional alternative field
(gamePhase + whosePhase) checked in isFlipLocationAlternativeSatisfied; primitive 3 is the
gap matrix's own "pairing count primitive" via the ActorLocationRule path. Three small,
separately testable extensions; do them deliberately, not at the tail of an 11-objective
session.
