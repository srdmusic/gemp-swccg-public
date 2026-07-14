# Codex Phase-Reorganization Gate State

Date: 2026-07-13
Program: executable Rando phase kernel and legacy-owner retirement
Branch: `rando-consolidation-2026-06-23`
Current observed HEAD: `85eb0452a` (Opus continuity acknowledgement atop gated Trace 4B1 + F1 + F2)

## Roles

- K-2/Claude dispatches scoped implementation agents, owns production Java commits, maintains the
  live continuity handoff, and keeps its main context limited to coordination and commit gating.
- Codex/Alfred owns architecture contracts, preflights, boundary tables, fixture support,
  source/council audits, and independent `ADVANCE`, `HOLD`, or `ROLLBACK` gates.
- No push. No deployment over a live game. No later batch deploys before Codex `ADVANCE`.
- `HOLD` closes only the affected cutover and deploy gate. Both workstreams continue independent
  preparation and implementation that does not rely on the rejected behavior.

The local pre-program backup is `/Users/steve/gemp-swccg-public-backup-20260712-221311`.
It was previously verified as a complete local copy with `.git`; GitHub was not used.

## Deliverable

The deliverable is not a sequence of card-specific patches. It is:

1. One normalized immutable decision-fact model.
2. One explicit route to one primary phase/window pipeline.
3. One owner per semantic domain.
4. Typed constraints, ranks, and bounded scores.
5. Shared Rando/ChosenOne kernel with explicit personality overlays.
6. Objective adapters with typed consumers and no duplicate same-domain contribution.
7. Phase-by-phase shadow comparison and cutover.
8. Removal of superseded owners, compiled-out blocks, and commented rule corpses after proof.

## Batch 0

Codex fixture contract:

- `Handoffs/CODEX_RANDO_DECISION_FIXTURE_SPEC_2026-07-12.md`

K-2 owes the explicit domain registry. Each rule arm needs one owner, kind, trigger, typed
facts, output band/rank, callers, bot parity, replacement status, and retirement fixtures.

New structural finding: exact-score ties are currently nondeterministic because
`CombinedEvaluator` merges into a `HashMap` and selection compares score only. Baseline tie
fixtures are `UNSTABLE`. An early explicit intentional delta must preserve first-seen order and
retain the first-seen candidate on equal raw float scores in both bots.

Registry commit `68ae6c4ff` is accepted as a useful 357-row inventory, not yet as migration
authority. Gate `m00235` requires the 13 ambiguous arms to receive explicit owners, exact per-arm
anchors, separate semantic owners from fact/enforcement services, correct live/dead/no-score
classification, and per-arm parity/replacement/retirement/fixture links.

The 13 ambiguous rows resolve to 21 exact arms in
`Handoffs/CODEX_DOMAIN_REGISTRY_AMBIGUITY_RESOLUTION_2026-07-13.md`. Reused base V-tags must not be
assigned one owner or kind.

Registry correction commit `5e290559c` advances as research notes only. It remains held as
migration authority because the 13 summary rows still conceal 21 routed arms, V172 incorrectly
claims no score despite a live `+600`, the generic hard-veto finalizer is not formation ownership,
and the 357-arm marker/count sweep remains incomplete. Gate:
`Handoffs/CODEX_DOMAIN_REGISTRY_GATE_5E290559C_2026-07-13.md`. Mailbox directive: `m00270`.

Tie-determinism fixture commit `5240f36c6` advances. Detached-commit verification passes the four
focused cases in each bot, 8/8 total, with one-ULP controls and normalized mirror parity. This
clears the tie increment only, not aggregate deployment. Gate:
`Handoffs/CODEX_TIE_DETERMINISM_GATE_5DF276C1B_2026-07-13.md`.

Registry rewrite `631ed4c13` corrected the 21 ambiguous arms but still omitted three live V27
sibling arms. Commit `31b9f697c` minted those rows, and `f2bb32e95` corrected five stale labels and
counts found by the independent gate. The 22 domain totals and section summaries now agree at 367,
and the amendment advances. The registry is still not blanket retirement authority: 344 arms lack
verified stable markers and named retirement fixtures. Gates:

- `Handoffs/CODEX_DOMAIN_REGISTRY_GATE_631ED4C13_2026-07-13.md`
- `Handoffs/CODEX_DOMAIN_REGISTRY_GATE_F2BB32E95_2026-07-13.md`

## Batch 1 Verdict

`HOLD`. Commit `5ab16f8ac` compiled and mirrored, and the intended V47 Reserve Solo Block was
removed while Lando Stay/Pull remain. It is not accepted for these reasons:

1. The actual Krennic source path remains unreachable. `Card216_016` stores its pull text in
   `getLocationDarkSideGameText()`, while the guard reads generic `getGameText()` and receives
   null. Deployed `web.jar` JShell proved generic null and dark-side text present.
2. `Director Orson Krennic` has typed `Persona.KRENNIC`, but first-title-token matching tests
   `director` and fails the objective exemption. The other Krennic printing happens to pass.
3. Pull-target suffix normalization is local to one ActionText consumer. Deployed
   `DeckOracle.parseSourceCardPullTargets` still returns `[krennic here]`; the parser has many
   other consumers. Normalization and side-aware source game text need one shared owner.
4. The empty-destination split penalty tests total friendly power, not friendly-character
   presence. A friendly power-0 character can be present and still receive the penalty.
5. `AI_VERSION_HISTORY.md` was not updated, and `AI_CHANGELOG.md` describes a gate/deploy order
   that did not occur.

Independent report: `.claude/skills/work-verifier/history.md`, latest `5ab16f8ac` entry.
Mailbox correction directives: `m00225`, `m00229`.

Correction commit `e17422f86` passes independent functional verification: exact diff and
diff-check, affected-module package, 28/28 focused tests, normalized bot parity, side-aware source
text consumers, actual blueprint probes, both Krennic printings, negative persona controls, and
FormationSafety helper behavior. Live `web.jar` predates the correction and nothing was deployed.
One non-blocking history sentence says the corrected entry is below when it is above; K-2 received
that wording fix in `m00271`.

## Batch 2 Pre-Spec Verdict

`HOLD` on schema commit `b94af20e1`, without stopping fixture or router work. Scalar threat,
survivability, economic-impact, and objective-alignment verdicts are policy outputs, not normalized
facts. The correction is to keep immutable observations and rule-independent measurements in
`DecisionFacts`/`ActionFacts`, then produce separate typed domain assessments with one owner each.
Unknown values must retain producer, provenance, and an unknown reason; `Optional<T>` alone does
not satisfy the frozen fixture contract. Mailbox directive: `m00236`.

Corrected architecture contract:
`Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md`.

Verified runtime route and non-mutating shadow contract:
`Handoffs/CODEX_RANDO_RUNTIME_ROUTE_MAP_2026-07-13.md`.

Fixture-harness review:
`Handoffs/CODEX_FIXTURE_HARNESS_REVIEW_E5B393955_2026-07-13.md`. Commit `e5b393955`
passes as a log/replay evidence harvester but is held as the executable fixture oracle after three
synthetic false-parity results. Mailbox directive: `m00241`.

Minimal exact trace-hook design:
`Handoffs/CODEX_MINIMAL_DECISION_TRACE_HOOK_2026-07-13.md`.

Inert snapshot scaffold commit `e4e0aa213` advances only as a no-consumer foundation. Shadow wiring
is held pending typed decision/route enums, typed candidate references, structured route evidence,
unknown-preserving observations, complete obligation arrays, validation, and real builder parity.
Gate: `Handoffs/CODEX_B2_INCREMENT1_GATE_E4E0AA213_2026-07-13.md`.

Type-hardening commit `fa0f254ac` passes 43/43 focused tests, affected-module package, diff-check,
and zero-consumer proof. The inert types advance. Builder/consumer wiring remains held because an
unset builder turn fabricates zero, selected route is not cross-validated with its evidence,
derived obligations can contradict noPass/minimum, and candidate-shape evidence is not tied to the
snapshot rows. Gate: `Handoffs/CODEX_B2_TYPE_HARDENING_GATE_FA0F254AC_2026-07-13.md`. Mailbox
directive: `m00277`.

No-op trace-hook commit `55c22fdde` advances only as inert instrumentation. Capture/oracle wiring is
held pending complete raw candidate order, defensive copies, schema/version/full-input/route/pass
and post-safety response fields, explicit invalid/incomplete traces, exception-safe evaluator
lifecycle, typed migrated ids, and comparator coverage. Gate:
`Handoffs/CODEX_TRACE_HOOK_GATE_55C22FDDE_2026-07-13.md`. Mailbox directive: `m00268`.

Exact trace-envelope and staged landing contract:
`Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md`. It requires one frozen raw snapshot, typed
route/operations/finalization/mutation events, separate pre- and post-safety results, deep copies,
and an `INCOMPLETE` status for every swallowed capture error. Mailbox directive: `m00278`.

Trace V2 increment `97d2cb65a` adds substantial inert infrastructure and passes 40/40 independently
run focused tests. Fixture capture, executable parity claims, cutover, and deployment remain held:
the snapshot still loses raw parameter presence/arrays, construction and sink failures can vanish,
`COMPLETE` is not route-complete, operation identity remains nullable, and route authority is not
validated against frozen facts. Gate:
`Handoffs/CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md`. Mailbox directive: `m00303`.

B2 consistency commit `d558248cf` closes the four `fa0f254ac` gaps at the inert facts/snapshot
boundary. Detached verification passes 97/97 focused tests, the affected-module package, and
diff-check. The immutable model and shared adapter advance. Trace capture, executable oracle,
cutover, and deployment remain held behind the complete raw-array schema and Trace 2b semantics.
Gate: `Handoffs/CODEX_B2_CONSISTENCY_GATE_D558248CF_2026-07-13.md`. Mailbox result: `m00308`.

Trace 2b commit `dde6488e0` advances as inert schema and capture-completeness infrastructure after
112/112 focused tests, affected-module package, diff-check, and semantic review. It closes the raw
decision map, typed construction/sink failure, route-completeness, mandatory operation identity,
and frozen wire-shape validation gaps. Enabled capture, semantic-route authority, stages 4 and 5,
owner cutover, and deployment remain held. The shared engine-fixture and pure-finalizer lane is now
open. Gate: `Handoffs/CODEX_TRACE2B_GATE_DDE6488E0_2026-07-13.md`.

Stage 4 source audit confirms only three outer observations per bot, two of them pre-call intent, and
separate outer/private trackers. The first typed-event draft is superseded after its
source-cardinality pass found omitted strategy/shield writes and incorrectly included Move
per-candidate scratch. K-2 source verification and council accepted the replacement 4A0
owner/mutator matrix in `m00363` with seven exact corrections now incorporated. The narrow,
capture-disabled 4A1 outer observation slice is open. Later owner families, behavior repairs,
enabled capture, cutover, and deployment remain held. Packets:

- `Handoffs/CODEX_TRACE_STAGE4_MUTATION_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_TRACE_STAGE4_EVENT_SCHEMA_2026-07-13.md`
- `Handoffs/CODEX_TRACE_STAGE4_4A0_MUTATOR_EVENT_MATRIX_2026-07-13.md`

Stage 4A1 final commit `01f821e87` advances after the complete-snapshot amendment, the
single-recorder hook correction, two changelog-boundary corrections, a fresh work-verifier pass,
detached parent/candidate packages, 181 focused tests with one expected F1 skip, and diff-check.
Capture remains disabled. Later owner families, enabled capture, cutover, and deployment remain
held. Gate: `Handoffs/CODEX_TRACE_STAGE4_4A1_GATE_01F821E87_2026-07-13.md`.

Stage 4A2a commit `08e544f50` advances as inert trace infrastructure. Exact outer update/clear
cardinality, normalized bot parity, unchanged legacy mutator bytecode, affected-module package,
171-test expanded verification, and an independent 787-test module run pass. Capture remains
disabled. One nonblocking debt is pinned: inject snapshot/append failures and prove the legacy
mutator still runs exactly once before capture enablement. Gate:
`Handoffs/CODEX_TRACE_STAGE4_4A2A_GATE_08E544F50_2026-07-13.md`.

Stage 4A2b implementation commit `f6d00e1da` and its documentation-only correction chain through
`7098f9b33` advance as inert shared-tracker observation. Exact direct-call cardinality, fallback
ordering, zero shared events on primary routes, normalized mirror parity, four unchanged legacy
mutator bytecode bodies, fault injection, 198 focused tests with one expected F1 skip, affected
module package, and diff-check pass. The bot-boundary fault fixture reaches `BLOCK_RESPONSE` only
as false/`NO_OP`; the armed TRUE/mutating proof is correctly limited to the tracker-level fixture.
Capture remains disabled. Gate:
`Handoffs/CODEX_TRACE_STAGE4_4A2B_GATE_7098F9B33_2026-07-13.md`.

Stage 4B1 heuristic-memory commit `ec886934b` advances as inert trace infrastructure after an exact
detached-commit server-module run of 842 tests with 0 failures, 0 errors, and 26 expected skips,
affected-module package, normalized Rando/ChosenOne fixture parity, diff-check, guarded source
review, and capture-disabled proof. Gate:
`Handoffs/CODEX_TRACE_STAGE4_4B1_GATE_EC886934B_2026-07-13.md`. Mailbox result: `m00484`.
Trace 4B2 is next in frozen order. Codex's current-HEAD source agent held the old packet, then
Codex corrected its exact new-game guard, owner reachability, retained-state wording,
canonicalization, source ordering, and embedded Agent-Ready Brief. A second continuity agent then
held the first corrected draft for renaming Fable operations, six-versus-seven lexical-site drift,
missing operation invariants, unspecified enum serialization, and an incomplete bytecode gate.
Codex invalidated that hash in `m00490`; revised hash `1631df9e...` restored the Fable contract.
K-2's independent source agent and council returned `AGREE` in `m00493`. Codex released final
packet hash `8f5a0438...` for one verbatim background Java agent in `m00494`; independent
implementation gate remains pending. Capture enablement, behavior repair, owner consolidation,
cutover, deployment, and push remain held.

Finalizer commit `92965934b` received a split gate: F0 real-engine fixtures advanced and F3 was held
for two contract defects. Correction commit `4a5e7d6b8` now advances F3 as inert infrastructure after
detached packages and 139 passing tests plus the one named post-F1 skip. `Pass` consumes
`policyPassAllowed`, `Acknowledge` is restricted to `EMPTY` and `CARD_ACTION_CHOICE`, and every
`FORCED` result carries a typed reason. F1 commit `5bd89ac68` advances the checked multiple-choice
bounds repair after 6 logic and 8 server-contract tests plus package/diff review. F2 commit
`a095db834` advances the bounded AI retry, clock, and terminal-visibility repair after the exact
128-test combined suite, an expanded 172-test corpus, package, diff, and AI-only source review.
Runtime finalizer consumers, interceptor cutover, and deployment remain held. Gates:

- `Handoffs/CODEX_FINALIZER_F0F3_GATE_92965934B_2026-07-13.md`
- `Handoffs/CODEX_FINALIZER_F3_CORRECTION_GATE_4A5E7D6B8_2026-07-13.md`
- `Handoffs/CODEX_FINALIZER_F1_GATE_5BD89AC68_2026-07-13.md`
- `Handoffs/CODEX_FINALIZER_F2_GATE_A095DB834_2026-07-13.md`

Cleanup 2.2 commit `2a6241edf` independently advances as behavior-neutral comment cleanup. It
removes only the mirrored commented V60 `+100` baseline statements in `DeployEvaluator`; V192 and
every live guard remain unchanged. Detached packages, exact mirror and deletion hashes, identical
parent/candidate bytecode listings, diff-check, and 168 focused tests with one expected F1 skip
pass. Gate: `Handoffs/CODEX_CLEANUP22_GATE_2A6241EDF_2026-07-13.md`.

Source-grounded SETUP and DRAW audits now freeze the real subroutes, additive evaluator overlap,
state mutations, registry errors, extraction seams, and required fixtures:

- `Handoffs/CODEX_SETUP_ROUTE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_DRAW_ROUTE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_CONTROL_ROUTE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_ACTIVATE_ROUTE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_BATTLE_ROUTE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_OBJECTIVE_ADAPTER_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_PULL_ROUTE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_MOVE_ROUTE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_FINALIZER_RESPONSE_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_DEPLOY_ROUTE_AUDIT_2026-07-13.md`

CONTROL, ACTIVATE, BATTLE, and objective-adapter cutovers are held independently. Their typed facts
and fixture work may continue. CONTROL and BATTLE are phase windows containing all seven wire
decision shapes, not evaluator-file routes. ACTIVATE has a zero-activation Yes/No stall and
universal INTEGER overcapture. BATTLE initiation has duplicate BattleEvaluator/ActionText owners,
target and predictor fact errors, and several delegated subroutes that cannot be flattened.
Objective handling has duplicate policy owners plus wrong back-side and rematch-reset facts that
must be corrected before score ownership moves.

PULL is a cross-phase parent/child/destination transaction and is also held independently. Failed-
search memory is disconnected, broad category heuristics can contradict source-card filters,
V192's clamp does not cap the route total, and several advertised blocks are additive scores. Typed
`PullFacts` plus a pure assessment may proceed, but existing contribution adapters and order remain
authoritative until full transaction fixtures pass.

MOVE owner cutover is held independently. Parent and destination policies contradict each other,
location-text destination prompts omit the physical mover, V169 has three incompatible owners,
support-plan facts are optimistic, and several logged "hard" rules are outvotable additive scores.
Typed MOVE subroutes and pure assessments may proceed while current contribution adapters remain
authoritative.

DEPLOY owner cutover is held independently. All seven wire shapes occur in the DEPLOY window, the
parent action has no typed binding to destination/undercover/capacity children, planner evaluation
mutates lifecycle state, physical-card identity is reduced to blueprint identity, and Force
obligations have three policy owners. Several advertised hard blocks remain additive score
costumes. Shadow `DeployWindowRoute` capture, pure assessments, exact parent references, and
deterministic fixtures may proceed. L3 parent and child ownership must cut over atomically.

Shared response-finalizer cutover is held independently and is now the first execution
prerequisite after the raw snapshot/Trace V2 gate. The mediator removes a pending AI decision
before engine validation, then silently requeues an invalid response without restarting AI
scheduling. PASS legality has incompatible owners across `DecisionContext`, `PassEvaluator`,
`CombinedEvaluator`, both outer bots, both `DecisionSafety` copies, and the concrete engine
decision classes. Five Rando direct interceptor families and four chosenone families bypass the
common safety/tracker path. Engine-contract fixtures, a pure shadow finalizer, fixed RNG injection,
and one bounded mediator retry may proceed. No direct route or duplicated safety/tracker owner may
retire until the shared corpus passes against real `decisionMade` implementations.

## Cleanup Sequence

Steve approved deleting superseded residue. Cleanup is split so visibility does not outrun
proof:

- Early 1.5A: comments containing old rule corpses, provably compile-time-false superseded
  blocks, and non-source backup/log artifacts. Inventory every deletion.
- Later cutover cleanup: compiled methods/classes such as `ObjectiveHandler` or `ActionAudit`,
  only after caller/reflection proof and route fixture coverage.

Raw class hashes are not a valid comments-only gate because line-number/source debug metadata
changes. Compare normalized `javap` method descriptors/instructions/exception tables/constants,
excluding debug metadata, plus fixtures and V191 no-delta. Mailbox directive: `m00227`.

Full cleanup contract: `Handoffs/CODEX_RANDO_CLEANUP_GATE_2026-07-13.md`.

Current cleanup inventory:
`Handoffs/CODEX_RANDO_CLEANUP_INVENTORY_2026-07-13.md`.

Batch 1.5 commit `66cf11e18` removes 1,686 lines across the mirrored CardSelection and Deploy
evaluators and adds 24 changelog/history lines. Current-tree compile, mirrored source, V159 branch
shape, and four retained V21 call sites pass the first source gate. Deployment remains held pending
isolated pre/post normalized `javap` parity and fixture/V191 evidence. K-2 received the comparison
method in `m00246` and continues independent work while the gate runs.

The isolated bytecode gate later passed for `66cf11e18`, so cleanup 1.5 advances as behavior-neutral.
Cleanup 1.6 (`224ba9423`) and cleanup 1.7 (`e447d306d`) also advance after detached parent/candidate
builds, normalized bytecode equality, mirror checks, and diff-check. Cleanup 1.8 (`21dda1a67`)
advances under the same gate after its mid-task scope correction: only the invalid V35.4 predecessor
was removed; the V192 rollback line was restored exactly. Cleanup 1.9 (`9c9ea3a1c`) advances after
the exact MoveEvaluator deletion hashes, mirrored additions, parent/candidate packages, normalized
bytecode, 60 focused tests, live-successor proof, and diff-check pass. Cleanup 2.0 (`82e4bc6ec`) and
cleanup 2.1 (`c6695168b`) also advance after their exact mirrored streams, detached packages,
identical bytecode, and 60 focused tests pass. The seven cleanup increments may remain on the
branch; none authorizes aggregate
deployment. Gates:

- `Handoffs/CODEX_BATCH15_GATE_66CF11E18_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP16_GATE_224BA9423_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP17_GATE_E447D306D_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP18_GATE_21DDA1A67_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP19_GATE_9C9EA3A1C_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP20_GATE_82E4BC6EC_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP21_GATE_C6695168B_2026-07-13.md`

Cleanup 2.2 through 2.8 also advance as behavior-neutral comment cleanup. Cleanup 2.6 consumes the
last two approved `DeployEvaluator` predecessor groups; Cleanup 2.7 consumes the approved
`CardSelectionEvaluator` maintenance-basis predecessors; Cleanup 2.8 consumes the final reserved
`ActionTextEvaluator` V140 group. Exact deletion streams, mirrored sources, detached packages, raw
bytecode equality, 181 focused tests with one expected F1 skip, and diff-check pass. The safe
deletion chain now stops at 24 mirrored pairs and 840 physical lines until route fixtures and
cutovers prove new literal ownership. Latest gates:

- `Handoffs/CODEX_CLEANUP26_GATE_BFF87F859_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP27_GATE_0A529F495_2026-07-13.md`
- `Handoffs/CODEX_CLEANUP28_GATE_15B776301_2026-07-13.md`

Factual-comment repair commit `978b58e1d` also advances. It corrects two crossed maintenance-card
labels and replaces the Battle Order cost comment with the source-proven 3-Force rule plus its
occupation and Battle Plan exceptions. The exact committed Java blobs pass detached package,
identical parent/candidate bytecode for all three affected classes, 181 focused tests with one
expected F1 skip, and diff-check. The comment program is complete. Gate:
`Handoffs/CODEX_COMMENT_FACT_REPAIR_GATE_978B58E1D_2026-07-13.md`.

## Cross-Prompt Deploy Finding

The July 12 replay proves parent and child deploy logic can contradict each other. Parent V29 found
affordable FN-2003/Cardo/Phasma buddy pairs, while child FormationSafety vetoed every first deploy
because a buddy was still in hand. The engine opened and canceled nine child prompts before moving
on. This requires one shared typed deploy-sequence assessment, not another score.

Evidence and contract:

- `Handoffs/CODEX_PARENT_CHILD_DEPLOY_PLAN_AUDIT_2026-07-13.md`
- `Handoffs/CODEX_RANDO_FACTS_ASSESSMENTS_CONTRACT_2026-07-13.md`
- Fixtures `B0_L3_ParentDeploy_AllDestinationsVeto_NoReopen` and
  `B0_L3_ParentDeploy_ValidBuddySequence_Allowed`
- Mailbox directive `m00247`

## Next Required Order

1. The inert Batch 2 raw snapshot and Trace 2b schema are gated through `dde6488e0`. Keep capture
   disabled while completing mutation observation, real-decision fixtures, and normalized cross-bot
   parity. No owner cutover or production shadow consumer advances from schema evidence alone.
2. F0 engine fixtures and corrected inert F3 finalization now advance. Complete F1 checked engine
   bounds and F2 bounded mediator retry with visible terminal failure before any runtime finalizer,
   interceptor, or phase-owner cutover.
3. Build the exact pure JUnit trace oracle and narrow `VirtualTableScenario` layer. The two
   parent/child formation fixtures plus SETUP and DRAW route matrices are mandatory.
4. Keep `DeployPhasePlanner`/`DeploymentPlan` as the sole deploy-sequence lifecycle owner; add typed
   card identity, ordered destination preflight, and parent/child reconciliation rather than a
   second mediator.
5. Correct the domain registry into exact per-route arms before any owner moves or legacy retirements.
6. Typed decision facts, shared truth services, and pure shadow router proceed. Legacy stays
   authoritative until exact route/candidate/contribution/veto/winner/post-safety-response and
   intended-mutation comparison passes.
7. Small comment-only cleanup commits may proceed in parallel, each through the normalized
   bytecode and fixture gate. Constant-false and compiled owners remain held.
8. Follow the canonical owner order in `Handoffs/CODEX_PHASE_CUTOVER_ORDER_2026-07-13.md`:
   interceptors, typed DRAW, cross-phase PULL, CONTROL, objective adapters, ACTIVATE, atomic DEPLOY
   parent/child, BATTLE, MOVE, then complex SETUP last. Each route advances only after its fixtures.
9. Retire duplicate owners and fallback routes only when observed coverage and fixtures prove
   the replacement.

Wide-scope directive to K-2: `m00226`. Fixture/tie directive: `m00228`.
