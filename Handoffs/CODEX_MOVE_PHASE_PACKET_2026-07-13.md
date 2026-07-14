# MOVE Behavioral Cutover Packet

Date: 2026-07-13
Architect/gate: Codex/Alfred
Implementer: K-2/Claude small agent
Baseline: `BATTLE_COMMIT_TBD`
Status: `FROZEN, PIPELINED, NOT RELEASED FOR JAVA`
Inputs: `CODEX_MOVE_ROUTE_AUDIT_2026-07-13.md` at `f2bb32e95` and
`CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`

This packet executes step 8 of `CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`. Release it only after
the accepted-response runtime and every preceding lane through BATTLE pass their independent gates.
The preceding lanes must preserve all three current V169 arms and their frozen operation ledger.
MOVE owns the atomic replacement of the parent physical-card guess, ActionText retry map, and child
blueprint guess described in section 4. No game or deployment belongs here.

## Goal

Cut over MOVE as a mediated family of typed subroutes, not as `phase == MOVE`. Carry one immutable
transaction from parent through child selections and retries, preserve the legacy operation ledger,
then apply only the named V47, V60, V169, and pseudo-veto corrections below.

## Hard Boundaries

- Extend the existing `DecisionSnapshot`; do not create another framework.
- Preserve candidate order, evaluator order, first-seen ties, score magnitudes, Pass/Done behavior,
  and raw float bits except for an explicitly named correction in this packet.
- Keep objective-granted movement eligible during CONTROL. Phase alone is never an exclusion key.
- Do not tune solo deploy weights, buddy-plan magnitudes, or battle policy. Those remain later work.
- Preserve unrelated dirty and untracked files. No DB, deck library, browser, server, or deployment work.

## 1. Typed Routes

The closed MOVE subroute set is:

| Subroute | Exact ownership boundary |
|---|---|
| `PARENT` | Top-level `CARD_ACTION_CHOICE` move action, usually optional. Capture the exact source action and physical mover when the engine supplies it. |
| `DESTINATION` | Ordinary `CARD_SELECTION` destination for a bound mover and origin. Preserve current no-Pass and V148 Done/Cancel behavior. |
| `LOCATION_SOURCE` | Location-text source selection. Record the selected physical origin from the engine's ordered legal candidates. |
| `LOCATION_DESTINATION` | Location-text destination selection. Use only facts invariant across every legal mover unless one physical mover is already exact. |
| `LOCATION_MOVER` | Location-text mover selection. Bind the selected physical mover to the existing source and destination transaction. |
| `PARSEC` | `MULTIPLE_CHOICE results` for parsec/orbit. Keep the Rando-only V79b direct interceptor waiver. |

Wrong type, missing provenance, incomplete candidate metadata, or ambiguous physical identity remains
legacy-unowned. Action text and `phase == MOVE` are compatibility evidence, never sufficient owners.
A typed objective-move origin may resolve `PARENT` or `DESTINATION` while phase is CONTROL; unrelated
CONTROL decisions remain with the CONTROL owner.

## 2. One MOVE Transaction

Add one immutable transaction view with these exact facts:

```text
MoveTransaction
  transactionKey
  subroute
  sourceAction { originalOrdinal, actionId, semantic, provenance }
  moverPhysicalCardId
  originPhysicalLocationId
  destinationPhysicalLocationId
  candidateLedger[] { subroute, orderedLegalCandidates[], selectedOrdinal }
  attemptOrdinal
  rejectionHistory
```

- Keep one stable `transactionKey` and trace id across the parent, source, destination, mover, and
  retry chain.
- Preserve each subroute's raw ordered source, destination, mover, or parsec candidates. Never sort or
  reconstruct them.
- A card link supplies a blueprint id, not a physical card id. Never choose the first matching copy.
- A fact may be unknown before the engine selects or emits it, but it may never be guessed. Any rule
  requiring an unknown fact stays legacy or defers to the later subroute.
- Mutation of attempt state occurs once through the accepted-response disposition owner, never while
  constructing facts, scoring candidates, tracing, or retrying a finalizer.

## 3. Frozen Pipeline

1. `M1 CAPTURE`: inside one uncommitted MOVE edit, add transaction, six subroutes, route facts, and
   Trace V2 in both bots. Shadow V27/FS-L1, V35, V47, V60, V169, and the V173 input with no owner or
   response change.
2. `M2 CORRECT`: in that same edit, install the V47 and V60 replacement owners and all three coupled
   V169 replacement arms. Preserve every predecessor source block for fixture comparison. Convert a
   pseudo-veto only under section 7. Leave every unrelated MOVE arm on legacy.
3. `M3 CUTOVER`: still without testing or committing, route only the closed owned arms in both bots,
   then disable their exact live predecessor call sites. Disable all three V169 callers atomically.
   Update only registry rows whose owner or description changed.

M1 through M3 are internal stages of one coherent edit. Run no tests and create no commits between
them. After the final disabled state is complete, run the one phase-boundary gate in section 10 and
make one MOVE commit. Physical predecessor deletion remains step 10. Aggregate deployment remains
step 12.

## 4. Required Corrections

### V47 Parent And Child

Create one pure `MoveLockFacts` assessment consumed by both stages. It must include exact Lando,
physical origin, objective relevance, site relevance, survivability, doomed-origin escape, and typed
objective transit. The destination child may not rebuild policy from a title scan.

- Preserve the survivable objective stay behavior and its current contribution order.
- A doomed Lando must retain a legal retreat.
- A typed objective-granted regular move during CONTROL must remain eligible.
- V47 and V41 may not bury an otherwise legal V169 retreat.

### V60 Location-Text Chain

Bind source, destination, and mover through one transaction using engine-emitted legal ids and the
card Java filter. Broad Safehouse/Mapuzo wording does not prove mover or destination legality and may
not receive the R4 band by itself.

- At `LOCATION_DESTINATION`, do not run FS-L1/L4, V156, V169, or other mover-specific policy unless
  exactly one physical mover is bound.
- If mover identity is still ambiguous, preserve only mover-invariant destination operations and
  defer mover policy to `LOCATION_MOVER`; never select a duplicate copy by blueprint.
- Cover Jedi and non-Jedi candidates, restricted destinations, and duplicate blueprints.

### V169 Retry

Replace the parent physical-card guess, ActionText retry map, and child blueprint guess with the one
MOVE transaction. Typed source semantics cover ordinary move, transport, relocate, shuttle, takeoff,
land, and location-text paths without a text allowlist.

- Key retry state by the stable transaction and exact physical mover, origin, destination, and source.
- Attempts 1 through 3 preserve the exact V169 soft `-250.0f` operation and safe-destination
  `+600.0f` operation.
- Attempt 4 preserves the V163 exhaustion `-100000.0f` operation and cannot restart under another
  text key. Any typed veto delta still requires section 7 proof.
- Reset or advance state only from the accepted-response transaction disposition.

The closed operation-ownership ledger is:

| Operation | Live predecessor caller | Replacement emitter | Preserved position | Final MOVE state |
|---|---|---|---|---|
| V47 parent stay/escape policy | `MoveEvaluator` parent arm | `PARENT` owner consuming `MoveLockFacts` | Existing Move-evaluator operation slot | Predecessor call site disabled; source retained for step 10 |
| V47 destination policy | `CardSelectionEvaluator` destination arm | `DESTINATION` owner consuming the same `MoveLockFacts` | Existing CardSelection operation slot | Predecessor call site disabled; source retained for step 10 |
| V60 physical landspeed/transit policy | `MoveEvaluator`, `ActionTextEvaluator`, and location-chain CardSelection arms | Typed `PARENT` or exact location-chain subroute owner | Each operation stays in its frozen evaluator-order slot | Exact replaced call sites disabled together; unrelated V60 pull owners remain live |
| V169 parent veto suppression | `MoveEvaluator` parent arm | MOVE transaction parent owner | Existing Move-evaluator slot | Disabled atomically with both other V169 callers |
| V169 retry `-250.0f` and exhaustion `-100000.0f` | `ActionTextEvaluator` retry map | MOVE transaction retry owner | Existing ActionText operation slot | Disabled atomically with both other V169 callers |
| V169 safe-destination `+600.0f` | `CardSelectionEvaluator` destination arm | MOVE transaction destination owner | Existing CardSelection operation slot | Disabled atomically with both other V169 callers |

The accepted-disposition owner is the only state mutator:

| Disposition | Transaction effect |
|---|---|
| Accepted parent or child that opens the next MOVE stage | Advance the exact transaction once |
| Accepted final MOVE stage | Complete and reset the exact transaction once |
| Cancel, typed rejection, engine rejection, finalizer retry, or trace on/off | No attempt, cursor, or retry-state mutation |

## 5. Objective And Support Facts

- Consume objective movement intent from the preceding objective adapter. MOVE still owns exact mover,
  origin, destination, legality, sequence, and can-win assessment.
- Consume BATTLE's immutable feasibility result without recomputing battle policy. Consume DEPLOY's
  existing formation assessment, obligation vector, and exact plan facts without rescoring deploy.
- Define `LegalSupportPlanFacts` as a MOVE-local immutable projection of those upstream facts: exact
  legal destination, effective cost, remaining Force after obligations, ordered deploy steps, and
  engine-backed reachable movement. The projection emits no score and owns no upstream state.
- If any required support, battle, deploy, or movement fact is `UNKNOWN`, preserve the corresponding
  legacy-unowned MOVE path. Never infer `safe`, `blocked`, or `reachable` from absence.
- Printed cost, a friendly card somewhere on table, or a card merely present in hand is not a rescue.
- V173/deploy magnitude tuning remains step 11. This lane only prevents false support facts from
  suppressing retreat or excusing an unsafe solo.

## 6. Parsec Waiver

Retain V79b as a documented Rando-only direct interceptor. Its evaluator V79/V103 arms remain inert
on `results`, and ChosenOne remains legacy until a real parity owner is approved. Do not copy V79b
into ChosenOne, retire it, or describe source-mirror equality as behavioral parity. Correct the false
`FLIP-BACK GUARD` description in migrated metadata without changing V79b behavior.

## 7. Pseudo-Veto Rule

Freeze every existing additive operation before changing enforcement. Labels such as `VETO`,
`HARD BLOCKED`, `-100000`, `V25 -500`, or `V64 -1500` do not create an invariant.

- For each arm, name whether it is a strategic penalty or a true invariant.
- A strategic penalty stays additive and receives truthful wording.
- A true invariant may add typed `hardVeto` only inside its replacement owner after max-positive-stack
  boundary math and exact V148 Done/Pass fixtures prove the old and intended outcomes.
- Preserve the legacy contribution operation alongside any intentional hard-veto delta so the trace
  shows both the frozen ledger and the named enforcement change.
- No blanket conversion, magnitude sweep, or deletion is allowed.

## 8. Both-Bot Fixture Matrix

Run every fixture through Rando and ChosenOne with one shared abstract harness and thin adapters:

1. `MOVE_V25_V35_MAX_STACK`: pilot penalty against the strongest legal R2 hunt/contest stack.
2. `MOVE_V27_FS_L1_DOOM`: contested gap 5, doomed gap 6, and uncontested origin.
3. `MOVE_V47_ESCAPE_CONTROL`: survivable stay, doomed retreat, and CONTROL objective movement.
4. `MOVE_V60_ROUTE_MOVER`: Jedi/non-Jedi, full source/destination/mover chain, restrictions, and
   duplicate blueprints.
5. `MOVE_V169_RETRY`: safe destination on attempts 1 through 3, exhaustion on attempt 4, and no
   V47/V41 suppression of retreat.
6. `MOVE_PARSEC_PARITY`: execute both bots and record the intentional V79b waiver. Rando preserves
   closest-to-7/orbit output; ChosenOne remains legacy. Cross-bot wire equality is not a gate here.
7. `DEPLOY_BUDDY_V173_LEGALITY`: illegal cheap buddy, legal affordable buddy, unreachable table
   buddy, and legal rescue wave, with no deploy-weight tuning.

Card-Java assertions accompany V47, V60, V79b, and battle-destiny facts. Ability `>=4` alone is not
engine-exact destiny entitlement.

For every candidate and both bots, assert raw candidate order, ordered contribution operations,
exact float bits, hard-veto state, pre-safety winner, V148/Pass/Done behavior, final response wire and
original ordinal, one post-accept mutation stream, and one complete stable-id trace lifecycle.
Trace-enabled and trace-disabled responses must be identical. The only cross-bot exception is the
explicit V79b waiver and bot identity fields.

## 9. Retirement Boundary

Do not delete MOVE owners, constant-false blocks, registry rows, policy-bearing ActionText
predecessors, retry state, comments, imports, or fields in this phase. During cutover, disable only
the exact live caller for an arm with an exclusive replacement owner. Disable the three V169 callers
atomically. Keep every predecessor source block, V79b, and every unowned legacy path until step 10.

## 10. Verification And Return

After the complete M1 through M3 edit reaches its final disabled state, run one focused both-bot
route, ledger, lifecycle, retry, trace, and V148 phase command. In the same gate, run `git diff
--check`, exact changed-path proof, normalized mirror proof, single-transaction/single-finalizer
searches, atomic three-caller V169 disablement proof, and unrelated-file exclusion proof.

Return baseline and result SHAs, exact paths, focused test command/counts, fixture matrix, ordered
operation and bit parity, intentional correction deltas, final responses, trace ids, mutation counts,
retired arms, retained legacy arms, and explicit V79b waiver status.

## Hard Stops

- Any predecessor lane or all-three-arm V169 ownership gate is incomplete.
- A route requires phase or decision-text inference instead of typed provenance.
- A physical mover, origin, destination, source action, legal candidate, or attempt is guessed.
- CONTROL objective movement becomes ineligible.
- Scoring, tie order, Pass/Done, response, mutation, or trace changes outside a named correction.
- A pseudo-veto changes enforcement without boundary math and V148 proof.
- Only one or two of the three V169 callers are disabled, or any predecessor source is deleted before
  step 10.
- Rando/ChosenOne differ outside the documented V79b waiver and identity fields.
- Any broad deletion, unrelated source change, game, browser, server, or deployment action is required.
