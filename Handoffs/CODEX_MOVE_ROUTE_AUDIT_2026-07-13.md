# MOVE Route Audit

Date: 2026-07-13
Owner: Codex/Alfred
Source snapshot: `f2bb32e95`; current dirty worktree excluded
Verdict: MOVE facts/assessment design `ADVANCE`; owner cutover/retirement `HOLD`

## Route Matrix

MOVE is a mediated family of semantic subroutes, not `phase == MOVE`:

| Semantic subroute | Wire shape | Current owners |
|---|---|---|
| Top-level move parent | `CARD_ACTION_CHOICE`, usually optional | Move + ActionText + Pass; Battle can cross-fire on text containing `battle`. |
| Ordinary destination | `CARD_SELECTION`, usually no-pass | CardSelection only; V148 may synthesize Done/Cancel when all destinations are below threshold or vetoed. |
| Location-text source | parent/child chain | Engine selects source, then destination, then mover. |
| Location-text destination | child selection | The prompt does not carry the physical mover, so generic mover safety cannot run. |
| Location-text mover | child selection | Mover identity arrives after destination selection. |
| Parsec/orbit | `MULTIPLE_CHOICE results` | Rando-only V79b interceptor. Evaluator V79/V103 sees no `results` candidates and is inert; ChosenOne lacks V79b. |
| Objective-granted move | can occur in CONTROL | Must retain MOVE semantics without inheriting a blanket phase lock. |

Card links encode blueprint id, not physical card id. Duplicate copies therefore cannot be resolved
to one mover from a card-link hint alone.

## Blocking Findings

### P0: V47 parent and child contradict each other

The parent Lando lock checks objective relevance and survivability. The destination child applies
`-9999` to every Lando destination without the same site, survivability, or phase gate. It can trap a
doomed Lando and can block the objective-granted regular move during CONTROL. One `MoveLockFacts`
record must feed both stages; the child cannot reconstruct policy from title alone.

### P0: V60 location-text movement bypasses mover safety

The engine asks source, destination, then mover. At destination time the physical mover is unknown,
so FS-L1/L4, V156, and V169 cannot evaluate the actual move. The parent also grants the strongest R4
band to broad Safehouse/Mapuzo text while the card Java filter can restrict which character and
destination are legal.

The transaction snapshot must carry the engine's legal source/mover/destination ids or preserve the
chain until the mover is known. Action-text inference is not exact route evidence.

### P0: pseudo-veto magnitudes are outvotable

MOVE's `-100000` ladder lines are additive unless `hardVeto()` is set. V25 is labeled a veto at
`-500` while R2 contributes `+6000`; V64 logs `HARD BLOCKED` at only `-1500`. These labels are not
enforcement. Any intentional conversion to a true invariant must be a separate behavior change,
tested against maximum positive stacks and V148 Done/Pass semantics.

### P1: V169 has three incompatible owners

- Parent: physical card id plus raw power.
- ActionText: retry map only for `move using|transport|relocate`, missing shuttle, takeoff, land, and
  some location-text paths.
- Child: first permanent matching a blueprint hint, which can select the wrong duplicate copy.

The route needs one physical mover id, origin, destination, source action, and attempt state. Retry
state belongs to the mediated transaction, not three local guesses.

### P1: buddy and rescue facts are optimistic and duplicated

Several callers treat printed-cost affordability or any friendly card anywhere as a deploy/move
plan. V173 projects the whole hand strongest-first without destination legality, restrictions,
effective cost, remaining Force, or reachable movement. These facts can falsely permit rescue,
suppress retreat, or excuse an unsafe solo.

`LegalSupportPlanFacts` must contain actual destination legality, effective cost, remaining Force,
ordered deploy steps, and reachable movement. Printed cost and inventory presence are not a plan.

### P1: bot parity is not route parity

Rando intercepts parsec `results` directly at V79b. ChosenOne lacks that interceptor and its
ActionText V79/V103 route has zero candidates because evaluator context drops `results`. ChosenOne is
currently not instantiated by the live factory, but source-mirror equality must not be cited as
behavioral parity for this route.

## Live Ownership

- Parent arms include V22.2/V22.5, V25, V27, V29 variants, V31-V38.3, V47, V49/V67f1, V53/V53b,
  V59/V60, V73, V79/V79b, V85, V91, V111, V135/V137, V156/V160/V169.
- Destination arms include V24 variants, V41, V47, V62, V64/V65, V67 variants, V156/V166/V169,
  Batch 1b, and FS-L1/L4.
- Cross-route inputs include ActionText V29.7/V35.4/V60/V67ae/V87/V134/V141/V163/V167/V169,
  Pass V27/V27.1, and deploy/formation V136/V156/V166/V169/V172-V177.
- V63 and V67d are routing changes, not score producers. Evaluator V79/V103 parsec arms are inert.

The registry's MOVE count is not migration-authoritative. It duplicates V31/V169, counts comment or
routing rows, omits several live arms, and carries stale descriptions for V25, V47, V53b, V60, V85,
V103, FS-L1/L4, V38.3, and V49. Correct these rows atomically with the route fixtures, not as a
summary-count patch.

## Card And Engine Truth

- Hidden Path flips when Jedi occupy two non-Mapuzo sites.
- Verge flips back for no leader at a Scarif site, not for leaving Scarif orbit. V79b may still be
  strategically useful, but its `FLIP-BACK GUARD` description is false card truth.
- Ability `>=4` is only the default normal battle-destiny threshold. Modifiers can raise the
  threshold or deny normal/added draws. FormationSafety's scalar test is not an engine-exact destiny
  entitlement fact.
- Some location-text move filters are global; others are theater-scoped. The AI cannot recover the
  exact legal-origin set from flattened action text. `CODEX_V67AE_RETREAT_SCOPE_AUDIT_2026-07-11.md`
  records the engine/API boundary.

## Replay And Decision-Log Grounding

- `replays/asdf/95s10zqy7sl0c177.xml.gz`, turn 3: Chiraneau leaves Ozzel and moves alone to Guest
  Quarters. V27/V32/V22.2 penalties are known, but R2 `+6000` yields parent score `5680`; destination
  independently adds `327.5`. Chiraneau is then lost in a `10-2` battle.
- Same replay, turn 4: Tarkin leaves Boba and moves alone into Rey. Parent penalties lose to R2,
  producing `5863.5`; destination V41 produces `777.5` with only `-30` for the stronger enemy. Tarkin
  is lost and Rando loses three additional Force.
- `replays/asdf/jc20n39malc6komb.xml.gz`, turn 3: Hondo moves alone into Anakin plus Yoda. Parent
  scoring is mover-insensitive; destination reaches `1045` from V166/V41 and only `-40` enemy power.
  Steve attacks and wins `23-8`.

These incidents prove both route gaps and dominance failures. They are not hypothetical card-text
edge cases.

## Smallest Safe Seam

Extend the shared `DecisionSnapshot`; do not create another framework.

1. Add MOVE semantic subroutes: `PARENT`, `DESTINATION`, `LOCATION_SOURCE`,
   `LOCATION_DESTINATION`, `LOCATION_MOVER`, and `PARSEC`.
2. Carry physical mover, origin, destination, source action, legal candidate ids, and provenance.
3. Produce pure assessments for endangered/doomed origin, abandon-solo, solo charge, legal support
   plan, objective transit, and can-win.
4. Keep route-local contribution adapters and preserve current magnitudes/order first.
5. Start shadow extraction with V27/FS-L1, V169/V173, V47, V60, and V35. Do not move every MOVE arm
   at once.

## Paired Fixtures

1. `MOVE_V25_V35_MAX_STACK`: pilot penalty versus the strongest legal R2 hunt/contest stack.
2. `MOVE_V27_FS_L1_DOOM`: contested gap 5, doomed gap 6, and uncontested origin.
3. `MOVE_V47_ESCAPE_CONTROL`: survivable stay, doomed retreat, and CONTROL objective move.
4. `MOVE_V60_ROUTE_MOVER`: Jedi/non-Jedi, complete location-text chain, duplicate blueprints.
5. `MOVE_V169_RETRY`: safe destination on retries 1-3; retry 4; V47/V41 cannot bury retreat.
6. `MOVE_PARSEC_PARITY`: Rando and ChosenOne select identical closest-to-7/orbit results.
7. `DEPLOY_BUDDY_V173_LEGALITY`: illegal cheap buddy, legal affordable buddy, unreachable table
   buddy, and legal rescue wave.

Every fixture must compare raw candidate order, ordered operations with exact float bits, hard-veto
state, pre-safety winner, V148/pass behavior, final response, and intended mutation events in both
bots. Card-Java assertions accompany V47, V60, V79, and battle-destiny cases.

## Retirement Boundary

Do not retire MOVE owners, constant-false blocks, registry rows, policy-bearing ActionText
predecessors, or local retry state yet. The source-proven invalid ownership-inverted V35.4 comment
corpse is separately approved for comment-only cleanup; it is not a valid policy rollback. A true
invariant may move to `hardVeto` only as a named intentional delta after the shadow ledger proves the
previous additive result and the new V148/Done outcome. One stable trace id and one passing
replacement fixture are required per retired arm.
