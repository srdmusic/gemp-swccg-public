# Codex verification: `79498fb5b` TDIGWATT Bespin fixes

Date: 2026-07-11 PDT  
Owner: Codex/Alfred, read-only verification for mailbox `m00173`  
Code changes by Codex: none

## Verdict

**FAIL.** The V185 return correction is sound and the turn-1 pull is statically
unblocked, but the new V21 parser does not produce the required Bespin key.
It stores `[special edition] bespin`; `isPullableCard("Bespin")` therefore
returns `false`. The stated never-lose repair remains incomplete for the exact
card whose loss caused the game.

| Requested boundary | Result | Evidence |
|---|---|---|
| (a) Rey incidents 2-4 remain blocked | PASS | Deployed-jar JShell fixture with real `Card225_050` Leia's Lightsaber in Reserve and no legal holder returns `true` from `reserveTargetsAreAllUnattachableWeapons`. The incidents share this real-dead-weapon path. `DeckOracle.java:479-535`. |
| (b) TDIGWATT upload outranks Activate | PASS, static | Real `226_12` targets plus real Bespin `223_8` and Tarkin's Bounty `208_41` return V185 veto `false`. V192 then scores `5500 + 1500 = 7000`: activate-phase base plus OBJECTIVE location tier from target `special edition bespin`. Activate Force scores `5000 + 500 = 5500`. `ActionTextEvaluator.java:219-250,4986-5027,5207-5215,5574-5666,5689-5698`. No post-fix TDIGWATT game has fired this branch yet. |
| (c) V21 extraction is exactly the requested list | **FAIL** | Deployed `ObjectiveAnalyzer` returns `[[special edition] bespin, dark deal, vader's bounty]`, not `[dark deal, vader's bounty, special edition bespin]`. `DeckOracle` independently returns the requested normalized list, but V21 does not use that output. |
| (d) rando/chosenone parity | PASS | Normalized `javap -c -p` output is bytecode-identical for both changed class pairs. Source has one pre-existing comment-only DeckOracle wording difference; executable code is identical. All four compiled classes byte-match their entries in deployed `web.jar`. |
| (e) no non-pull `[upload]` over-match | PASS | Four objective sources contain `[upload]`. The new regex matches two: `226_12` and `501_14`; both are actual Reserve pulls. It does not match back-side non-immediate forms in `301_2_BACK` or `219_1_BACK`. `501_14` yields inert generic key `any card`, not a non-pull match. |

## Blocking V21 reproduction

Actual source text is at
`src/gemp-swccg-cards/src/main/java/com/gempukku/swccgo/cards/set226/dark/Card226_012.java:45`.
The pull action's real filter is at lines 106-120.

The deployed parser run produced:

```text
OBJECTIVE_ANALYZER=[[special edition] bespin, dark deal, vader's bounty]
Dark Deal=true
Vader's Bounty=true
Bespin=false
Bespin (V)=false
```

Cause:

- `UPLOAD_FROM_RESERVE_PATTERN` correctly captures the list at
  `ObjectiveAnalyzer.java:168-169,1549-1561`.
- `cleanCardName` at `ObjectiveAnalyzer.java:1610-1615` removes only bullets and
  leading articles. It does not remove icon tokens such as `[Special Edition]`.
- `isPullableCard` at `ObjectiveAnalyzer.java:314-320` compares the real title
  directly with the stored key, so `bespin` cannot start with
  `[special edition] bespin`.
- V21 consumers pass real `PhysicalCard.getTitle()` values, including the
  force-loss path at `CardSelectionEvaluator.java:4393-4401`.

Minimum generic correction: normalize icon tokens in the same helper used for
both take and upload extraction. For title identity, `[Special Edition] Bespin`
must normalize to `Bespin`. Re-run this exact four-assertion fixture after the
change.

### Objective-upload corpus boundary

The card-source corpus contains only two front-side Objective texts with an
`[upload]` clause:

- `Card501_014.java:54`: generic `any card`, with no leading icon token.
- `Card226_012.java:45`: `Dark Deal, Vader's Bounty, or [Special Edition] Bespin`.

Other Objective upload clauses found by source search are on back-side classes
(`Card301_002_BACK.java`, `Card219_001_BACK.java`). TDIGWATT is therefore the
only front-side Objective upload target in the current source corpus that
requires leading set-icon normalization. The Rando and Chosen One
`cleanCardName` implementations are identical and both need the same fix.

Acceptance boundary: strip one or more leading bracketed icon tokens from a
captured card target, not arbitrary bracketed text in the middle of a title.
The fixture must continue to return `Dark Deal=true`, `Vader's Bounty=true`,
and an unrelated `Bespin (V)=false`, while changing plain `Bespin` to `true`.

## V185 and score evidence

The deployed-jar fixtures used actual card blueprints:

```text
dead Leia saber=true
targets=[dark deal, vader's bounty, special edition bespin]
TDIGWATT veto=false
```

Pre-fix replay/log evidence identifies the exact decision:

- `logs/gemp-swccg.log:903-926`: turn-1 candidates include TDIGWATT's `Take
  card into hand from Reserve Deck`; false V185 blocks it and Activate wins
  `5500`.
- `replays/asdf/kxn8bvydcd803p2j.xml.gz`, final game segment events 47-56:
  Rando starts turn 1 and activates all seven Force without using the objective.
- `DeckOracle.java:488,532-535`: the new tail is driven only by a real matched
  dead weapon. Fuzzy non-weapon matches alone can no longer return `true`.

This proves the gate and arithmetic. A fresh TDIGWATT game is still required
for gate 4, the runtime `V192 PULL SCORER (ACTIVATE)` fire and actual Bespin
selection.

## Adjacent V67h null-zone finding

The proposed transient-zone concern is structurally real, but it is fixture-only
evidence. The target replay does not reproduce it.

The replay's suspicious Power Of The Hutt transition is legitimate:

- `logs/gemp-swccg.log:49266-49268`: Rando explicitly loses the unique
  Jabba's Sail Barge from the top of Reserve Deck.
- `logs/gemp-swccg.log:51478`: Power Of The Hutt correctly reports
  `WILL_FAIL`, because its barge target is no longer in Reserve.
- `logs/gemp-swccg.log:52509-52511`: Rando accepts Steve's revert to before
  the C-3PO Force-loss resolution.
- `logs/gemp-swccg.log:53336`: after restoration, Power Of The Hutt correctly
  reports the barge in Reserve.

Therefore the observed `WILL_FAIL -> OK` change is revert restoration, not a
null-zone false negative. Do not treat this replay as runtime justification for
a V67h fallback change.

- `DeckOracle.refresh` clears every catalog zone to `null` before rebuilding it
  (`DeckOracle.java:226-280`). A card not matched in that refresh remains null.
- `validatePullFromSourceCard` treats absence from the requested zone as
  authoritative failure after its rescue chain.
- Deployed-jar fixture with the same unique real Bespin card:

```text
currentZone=RESERVE_DECK -> WILL_SUCCEED
currentZone=null         -> WILL_FAIL
```

Narrow correction: after ordinary validation finds no requested-zone target,
return `UNKNOWN`, not `WILL_FAIL`, only when a target-matching, unique original
deck card exists in the catalog with `currentZone == null`. Do not let an
unrelated null card weaken a verdict, and do not downgrade a card positively
known in Hand/Lost/Used/Force/In-play. Add a distinct log marker so a replay can
prove the fallback fired. Longer term, refresh should compute zone assignments
before committing them so an interrupted or failed refresh cannot strand the
catalog in its cleared state. This remains a defensive test boundary, not a
replay-proven defect.

## Build and deployment gates

| Gate | Result |
|---|---|
| Compile | `MVN_EXIT=0`, `ERROR_COUNT=0` |
| Bundled | All four changed `target/classes` files byte-match `web.jar` |
| Loaded | `web.jar` mtime `2026-07-11T20:35:40-0700`; JVM start `2026-07-12T03:35:41.540Z` |
| Health | `http://127.0.0.1:17001/gemp-swccg/` returned HTTP 200 |
| Runtime behavior | V185/V192 post-fix TDIGWATT fire not yet observed |
