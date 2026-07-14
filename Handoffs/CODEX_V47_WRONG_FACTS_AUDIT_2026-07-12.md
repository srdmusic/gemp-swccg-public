# Codex V47 Wrong-Facts Audit - 2026-07-12

## Decision

Retire the current V47 reserve-selection hard block. It does not know the forced deploy destination and therefore applies Cloud City board facts to unrelated reserve pulls.

## Exact Failure

During the second `Scarif: Command Center` Krennic pull:

- `logs/gemp-swccg.log:78092-78110` selects `Deploy Krennic from Reserve Deck`, then opens `Choose card to deploy from Reserve Deck`.
- `logs/gemp-swccg.log:78121-78130` shows Krennic is the only selectable card, but V47 labels him `alone at CC, enemy=true` and applies `-9999`.
- `logs/gemp-swccg.log:78144-78149` shows the engine must still select Krennic because `noPass=true`, producing a final score of `-9959`.

The destination was Scarif, not Cloud City. The same false V47 hit also appears for the first Krennic pull at log lines 74879/74904 and for Commander Praji at 77726/77763.

## Source Boundary

Both bot copies have identical logic:

- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/evaluators/CardSelectionEvaluator.java:8826-8875`
- `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/chosenone/evaluators/CardSelectionEvaluator.java:8826-8875`

The rule triggers for every character-selection prompt whose text merely contains `deploy` and `reserve`. It then:

1. Ignores the source card and forced destination.
2. Scans every top location whose title contains `cloud city`.
3. Aggregates friendly and enemy presence across all Cloud City locations.
4. Applies `-9999` when no friendly character exists anywhere at Cloud City.

`rsObjAnalyzer` is fetched but never used. There is no Dining Room, Cloud City source, destination, optional-selection, or legal-alternative gate.

## Corpus Evidence

The current log contains false or ungrounded V47 hits on unrelated forced reserve selections, including Krennic, Commander Praji, Supreme Leader Snoke, and numerous Court characters. The Krennic case is conclusive because the card source itself forces deployment to `Scarif: Command Center`.

## Repair Boundary

Formation Safety should own destination-aware deploy protection after the source action resolves an exact forced destination. Card-selection scoring cannot safely infer destination from the generic prompt `Choose card to deploy from Reserve Deck`.

The old rule's intent must be preserved during the handoff. `AI_VERSION_HISTORY.md:413-419` describes V28 as protection for reserve deployments such as Dining Room. Actual `Card226_001.java:40,60-66` confirms Dining Room says `May [download] Lando here` and uses `DeployCardToLocationFromReserveDeckEffect(..., Filters.Lando, Filters.here(self), true)`. That is the same typed source-action class as Krennic and belongs in the repaired forced-location Formation Safety path.

Minimum safe correction:

- First repair the forced-location resolver so literal targets such as `Krennic here` and `Lando here` resolve to the real character and run Formation Safety at the source location.
- Then remove or disable V47 in both bot copies in the same change set.
- Do not replace it with another title or prompt-text guess.
- Keep any protection at the source-action layer where source card, destination, cancellability, and legal alternatives are known.

No Java files were edited by Codex.
