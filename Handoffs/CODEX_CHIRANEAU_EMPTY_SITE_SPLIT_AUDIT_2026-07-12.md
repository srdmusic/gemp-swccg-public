# Codex Chiraneau Empty-Site Split Audit - 2026-07-12

## Decision

Add a heavy negative score at move-destination selection when a weak character voluntarily splits a two-character group into two weak solos by moving to an empty opponent-icon battleground. Do not make the class universally unplayable; explicit objective or survival plans may dominate the penalty.

## Log Evidence

`logs/gemp-swccg.log:25193-25312` records the full decision:

- Chiraneau and Ozzel were together at `Cloud City: Upper Walkway`, total power 5.
- Moving Chiraneau would leave Ozzel power 3 and ability 2 alone.
- Existing V27, V32, and V22.2 penalties fired, but V31 `POST-FLIP CONSOLIDATE` claimed R2 and supplied the `+6000` doctrine band.
- The source move still won at `5680` over Pass `4`.
- The only destination was empty `Cloud City: Guest Quarters`.
- Destination scoring added drain/icon value plus V24.9 `+200`, reaching `327.5`, so Rando moved Chiraneau there.

The move was not consolidation. It split one weak pair into two weak solos.

## Replay Evidence

Replay: `replays/asdf/95s10zqy7sl0c177.xml.gz`

The final full-history segment shows:

- Event 6501: Chiraneau deploys to Upper Walkway, joining Ozzel.
- Events 6631/6633: Leia and Yoda leave Guest Quarters, making it empty.
- Event 6677: Chiraneau moves alone from Upper Walkway to empty Guest Quarters.
- Event 6718: opponent deploys Rey, All Of The Jedi to Guest Quarters.
- Event 6735: opponent initiates battle there.
- Events 6746/6753: Rey fires at and hits Chiraneau.
- Events 6763-6765: Chiraneau is forfeited and placed on Lost Pile.

This is the exact log/replay consequence, not a hypothetical scenario.

## Source Boundary

`FormationSafety.java:122-146` only checks move destinations when opponent power is already present. Line 131 returns immediately for an empty destination.

`FormationSafety.java:150-185` only checks leaving a weak solo at origin when opponent power is already present there. Lines 178-180 return immediately for an uncontested origin.

Both returns are individually reasonable for their narrow L4/L1 rules, but together leave the empty-site split class uncovered. Both bot copies call these shared checks from `CardSelectionEvaluator.java:6268-6282`.

## Boundary Math

The recorded destination score was `327.5`. A `-800` weak-solo/no-plan move penalty yields `-472.5`, below the non-deploy cancel bar, so `Done` wins for this exact incident.

Suggested narrow predicate:

- mover is a non-undercover character;
- destination has zero friendly characters and zero opponent power;
- destination has opponent Force icons;
- mover is weak for solo duty (power below 6 and ability below 4, using existing Formation Safety thresholds);
- move leaves exactly one non-undercover friendly character at origin whose post-move ability is below 4;
- no survival-retreat exemption, because the origin is uncontested and not doomed.

Apply a heavy soft penalty such as `-800`, matching the deploy `L3 NO-PLAN SOLO` policy. This preserves Steve's allowed overpower/opportunity and objective-plan exceptions while defeating ordinary drain bait like the `+327.5` Guest Quarters score.

Also reject the V31 `POST-FLIP CONSOLIDATE` label for this outcome. Moving to an empty destination cannot reinforce a stronger position.

No Java files were edited by Codex.
