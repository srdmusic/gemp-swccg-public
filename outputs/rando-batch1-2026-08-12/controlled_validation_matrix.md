# Batch 1 controlled post-deploy validation matrix

Date: 2026-08-12
Status: execution specification, not deployment permission
Supersedes the grouped-table plan previously in this file. `E1_E2_walkthrough.md` remains background material only where it does not require deck edits or combine incompatible proof windows.

## Acceptance boundary

This campaign validates four different things without treating them as interchangeable:

1. Batch 1 persistent-drain response and objective-home eviction behavior.
2. The already-shipped WMAOP lifecycle.
3. The already-shipped Phase 3 battle fixes.
4. Artifact, loaded-JVM, log, and replay identity.

Phase 1 and Phase 2 already have correlated live evidence in MariaDB game `72276`. Do not replay them merely to make the campaign look busier.

### Campaign inventory

| capture | isolation | purpose |
|---|---|---|
| S1 | standalone | immediate persistent-lane response below the V166 gate |
| S2 | standalone | funded mandatory objective plan outranks a remote lane response |
| S3 | standalone | viable objective-critical eviction wave |
| S4 | standalone | outmatched critical site receives no suicide instruction |
| S5 | standalone | one-off drain never creates a persistent response |
| S6 | standalone | ledger survives ordinary mid-game board churn |
| S7 | standalone | response obligation advances through every exact proved wave member |
| E1 | one or more standalone games | WMAOP deploy-only, Blockade-only, sanctioned pull, live hold, and fodder lifecycle |
| E2A | standalone | V76 confident predictor neutralizes a negative specific-battle band |
| E2B | standalone | L2 low-ability battle veto is waived only at weapon-adjusted 2x dominance |

The minimum campaign is ten separately labeled tables: S1 through S7, E1, E2A, and E2B. Each acceptance contract is deterministic once its opportunity gate is proved. E1 may require extra games because WMAOP must be drawn. Random failure to expose a required card or plan is inconclusive, not permission to combine scenarios.

Never group S5 with a later persistent drain. S5 is a whole-capture negative. Never group S2 with S7. A persistent Lower Corridor lane would contaminate S7's critical-only count. Never use one TPTR squat to stand in for both S3 and S7 unless the capture was declared as only one of those labels before the game.

## Safety and rollback gate

These are hard stops.

1. Keep `shutdown=true` until the candidate restart is complete and verified.
2. Observe zero WAITING and zero PLAYING tables twice, at least 15 seconds apart, immediately before replacement, restart, or rollback.
3. Verify the sealed baseline:
   - commit: `e877d4b6eb68b02fd25f6bdf90bf9f766662f4f7`
   - rollback tag: `rando-batch1-baseline-e877d4b6-2026-08-12`
   - jar size: `46002727`
   - MD5: `5fcfe59e32c16262f8637dbc0fcd503e`
   - SHA-256: `68721e70243b918b4b9ab881d25d626a713a4a9d5a4b2554250bc6daaf32f3f7`
   - immutable manifest: `/Users/steve/gemp-deploy-backups/rando-batch1-2026-08-12/predeploy-e877d4b6-5fcfe59e/manifest.txt`
4. Use the sealed no-auto-flip override:
   `/Users/steve/gemp-deploy-backups/rando-batch1-2026-08-12/predeploy-e877d4b6-5fcfe59e/compose.no-boot-flip.yml`
5. Verify candidate source, jar hashes, loaded JVM start time, settings, logs, and HTTP as separate facts.
6. Run one labeled controlled table at a time. Freeze its DB row, both replay streams, and log slice before opening the next table.
7. Do not edit decks, production source, card data, or database state to rescue staging.
8. Set `shutdown=false` only after all loaded-JVM gates pass. Return it to `true` immediately if process continuity, artifact identity, or one-table isolation becomes uncertain.

## Verdicts and the opportunity gate

| verdict | meaning |
|---|---|
| PASS | the required opportunity was proved and the exact selected behavior plus replay consequence occurred |
| PASS_BRANCH_ONLY | the branch passed before a timeout or concession; the game outcome is not a natural win |
| FAIL | the opportunity was proved legal, available, affordable, timely, and knowable, but required behavior was absent or prohibited behavior occurred |
| INCONCLUSIVE_NO_OPPORTUNITY | draw, Force, candidate set, planner state, or board never exposed the required opportunity |
| INVALID_STAGING | the scripted drain cadence, target role, side, power relation, or isolation boundary was not achieved |
| INFRASTRUCTURE_ABORT | exact DB/replay pairing, final segment, log slice, process continuity, or server continuity failed |

No missing tag is a failure until an opportunity packet proves all five gates:

| gate | minimum proof |
|---|---|
| legal | Rando-facing decision parameters or engine evidence show the exact card and target were legal |
| available | the exact physical card identity was in the usable zone at the decision |
| affordable | current Force, exact deploy cost, and whole-wave cost are anchored |
| timely | the candidate existed in the exact response decision window |
| knowable | every fact used was public or present in Rando's own legal decision state |

For positive response scenarios, also inventory every eligible funded mandatory objective plan in the same decision. A correctly selected mandatory plan is not a missed response.

Replay `D` elements expose legal candidate parameters. They do not expose evaluator scores or prove which rejected option would have won. The public replay consequence and the selected `Best action` plus following `Reasoning` log block establish the chosen behavior. Do not infer rejected choices from silence.

## Four delivery gates

| gate | required record | proves | does not prove |
|---|---|---|---|
| source and tests | candidate commit, scoped diff, focused and full-suite results | intended code and tests exist | jar contains it |
| artifact | candidate jar size and hashes, changed-class hashes, Rando and Chosen One needles | behavior is packaged | JVM loaded it |
| loaded JVM | process start, host/container jar identity, settings, HTTP, startup boundary | fresh process serves the jar | target branch fired |
| live behavior | DB game id, exact recording ids, final segments, bounded logs, selected reasoning, replay consequence | loaded behavior fired | unobserved branches work |

The host jar and bind-mounted container path are one artifact plus an alias, not two independent copies.

### Source anchors to freeze with the candidate

| behavior | path | exact symbol |
|---|---|---|
| mandatory-plan precedence and response selection | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/PersistentResponsePolicy.java` | `select` |
| exact-member response scoring | same | `scoreSelectedResponseAction` and `matchesSelectedResponseAction` |
| wave advance and stale-card clearing | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/rando/strategy/DeploymentPlan.java` plus Chosen One mirror | `recordDeployment`, `recordUnavailablePlannedCard`, and `advancePersistentResponse` |
| old V166 deploy gate | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/DeployTacticalPolicy.java` | `scoreV166ContestDrain` |
| WMAOP parent modes | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/PullActionPolicy.java` | `evaluateParent` |
| WMAOP child mode steering | Rando and Chosen One `evaluators/CardSelectionEvaluator.java` | `evaluateTakeIntoHand` WMAOP branch |
| WMAOP loss lifecycle | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/ForceLossPolicy.java` | standalone and combined-battle WMAOP arms |
| V76 reconciliation | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/phase/BattleDecisionPolicy.java` | confident-predictor compensation after `specificBattle` |
| L2 waiver | `src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/common/strategy/FormationSafety.java` | `vetoInitiateBattle` |

Record the candidate commit and exact line numbers for these symbols in the deployment packet after source stabilization. Symbol names are the durable contract; line numbers from an uncommitted worktree are not.

## Canonical evidence identity

Run the correlator immediately after each completed capture:

```text
python3 outputs/rando-batch1-2026-08-12/harness/correlate_evidence.py \
  --game-id <GAME_HISTORY_ID> \
  --label <CAPTURE_LABEL>
```

The correlator must:

1. Query only the exact `game_history.id` in a read-only transaction.
2. Require participants `asdf` and `~Rando_Cal`, with Steve Light and Rando Dark.
3. Resolve `win_recording_id` and `lose_recording_id` to the exact participant directories.
4. Require both files, unique recording ids, matching final-segment public fingerprints, and a replay terminal consistent with the DB row.
5. Use only the final segment beginning with the last `You're starting a game` marker.
6. Bound log candidates by DB `start_date` and `end_date`, with only documented small timestamp slack.
7. Fail closed on a missing stream, duplicate stream, mismatched fingerprint, ambiguous Rando game-start marker, absent log slice, or orientation mismatch.

Replay mtime and "newest replay" are never evidence identity.

The correlator produces an evidence packet. It does not certify opportunity gates or scenario PASS by itself. A human must attach the opportunity packet and scenario checklist.

## Log grammar and selected-action binding

Production uses `NoOpTraceSink`. Typed rule ids are not guaranteed to print. The two Batch 1 reason strings do print in the selected action's `Reasoning` line and include the target:

| key | accepted log regex |
|---|---|
| B1-PERSIST | `deploy-persistent-response-selected|Selected executable response to a two-turn drain lane; target=` |
| B1-CRITICAL | `deploy-objective-critical-eviction-selected|Selected executable response clears a typed objective-critical location; target=` |
| B1-FAILCLOSED | `Persistent response ledger reset fail-closed` |
| V166-DEPLOY | `V166 CONTEST DRAIN \(deploy\)` |
| V171-CONTACT | `V171 DEPLOY TO CONTACT` |
| FS-SAFETY | `FORMATION SAFETY` |
| WMAOP-DEPLOY-ONLY | `WMAOP\.DEPLOY_ONLY` |
| WMAOP-BLOCKADE-NEGATIVE | `WMAOP\.BLOCKADE_ONLY: only the Blockade Flagship site pull is sanctioned|WMAOP\.BLOCKADE_ONLY: .* is not the Blockade Flagship site|WMAOP\.BLOCKADE_ONLY: non-Blockade candidate offered by a WMAOP search` |
| WMAOP-BLOCKADE-POSITIVE | `WMAOP\.BLOCKADE_ONLY: prefer the Blockade Flagship site` |
| WMAOP-LIVE-HOLD | `WMAOP\.LIVE_HOLD` |
| WMAOP-FODDER-HOLD | `WMAOP\.FODDER_HOLD` |
| V76-PREDICT | `V76 BATTLE PREDICT` |
| V76-CONFIDENT | `V76 PREDICTOR CONFIDENT` |
| L2-WAIVED | `L2 WAIVED` |
| PHASE1-ATTACK | `ATTACK CANDIDATE` |
| PHASE1-CONCENTRATE | `V96 CONCENTRATE` |
| PHASE1-DOMINANCE | `V136 .*DOMINANCE PASS|V136 unified deploy-site score` |
| PHASE2-PASSENGER | `V29 PASSENGER ABOARD` |

For B1-PERSIST and B1-CRITICAL, bind the `Reasoning` line to the immediately preceding `Best action` line in the same decision block. Then bind that exact card and target to the replay deployment. A game-global tag count is never enough.

Candidate warnings such as WMAOP blocks, FormationSafety, V76 predictions, and V29 passenger penalties are opportunity or boundary evidence. They are not selected-action proof. Anchor every replay-side opportunity to the final-segment `D` event index, decision type, decision text, and exact candidate `cardId` parameters. Anchor the chosen consequence separately in the public `M` stream. A `D` candidate does not reveal its evaluator score or the returned choice.

## Current wave lifecycle

The response obligation is wave-scoped, not single-grant:

1. The selected obligation stores every exact originally proved response member as `(permanentCardId, currentCardId)`.
2. A member receives the applicable Batch 1 band only when that exact physical card is still planned, offered, selectable, and aimed at the exact target row.
3. After that exact deployment succeeds, the member is removed and the obligation advances to the remaining members.
4. Every remaining exact member may receive its own band once.
5. A stale, unavailable, unrelated, wrong-target, hard-vetoed, or deferred card receives no band.
6. Proven unavailability clears the obligation. It does not silently promote an unproved replacement.
7. V166 suppresses B1-PERSIST on the same action. It does not replace Batch 1 live proof.

Therefore no scenario has a global `B1 <= 1` rule. The correct count is one selected reason per exact executed wave member in the scenario window.

## Existing deck fixtures

| id | deck | side | uses |
|---|---|---|---|
| 25636 | EOPS | Dark Rando | S1, S5, S6 |
| 25716 | Invasion | Dark Rando | S2, S3, S4, S7, E1, E2A, E2B |
| 25732 | MWYHL 25 | Light Steve | S1, S5, S6 |
| 25720 | 1 Rey | Light Steve | S2, S3, S4, S7, E1, E2A, E2B |

Relevant verified contents:

- EOPS starts Endor system, Bunker, and Landing Platform.
- MWYHL starts Beldon's Corridor. Carbonite Chamber is draw-dependent.
- Invasion starts Naboo system, Blockade Flagship, Swamp, and Droid Racks. The deck includes TPTR, Neimoidians, droids, three WMAOP copies, and Blockade Flagship: Bridge.
- 1 Rey contains TPTR-legal Light characters and both weak and heavy squatters.

The WMAOP owner-scope negative requires Light `Blockade Flagship: Docking Bay` (`14_48`). No current eligible Steve Light deck contains it. The existing source/unit boundary remains the evidence for owner scoping in this no-deck-edit campaign. Do not change a deck to manufacture that live negative.

## S1: immediate persistent-lane response

Label: `B1-S1-persistent-immediate`
Decks: EOPS 25636 versus MWYHL 25 25732

### Staging

1. Use Beldon's Corridor (`225_40`) as the primary lane. Carbonite Chamber (`225_41`) is allowed only if drawn before staging begins.
2. Put one modest Light character there and do not reinforce it.
3. Let Rando complete any funded mandatory EOPS setup first. Do not begin the two-drain window while an exact funded mandatory plan remains.
4. Before the second drain, anchor the current net drain balance below the V166 `>=2` gate.
5. Initiate a Force drain at that exact site on two consecutive completed Steve turns and let Rando pay at least 1 Force each time. A canceled or zero-paid attempt does not enter the ledger.
6. Avoid other repeated Light drain lanes and avoid contesting Endor.
7. Freeze the target window at the immediate Rando deploy phase after the second paid drain. There is no unconditional extra-turn slack.

### Opportunity packet

- Both resolved nonzero drain payments and their Steve turn numbers.
- Steve still present at the same permanent location id.
- Exact legal, available, affordable all-character response wave at that site.
- Exact decision window and target row.
- No eligible funded mandatory objective plan in the same decision.

### Pass contract

- Selected reasoning contains B1-PERSIST with the exact lane target.
- Replay shows the exact selected physical card deploy to that target in the same decision window.
- Each additional exact proved member deployed in that wave has its own single B1-PERSIST selected reason.
- No B1-FAILCLOSED occurs.
- B1-PERSIST and V166-DEPLOY do not occur on the same selected action.

V166-DEPLOY on the selected lane action makes this `INVALID_STAGING`. It proves the old route and suppresses the new persistent band. It is not an alternative PASS.

If no B1 reason appears, call FAIL only when the full opportunity packet also proves V166 was not the selected-action route. Otherwise use `INCONCLUSIVE_NO_OPPORTUNITY`.

## S2: funded mandatory Invasion plan outranks remote pressure

Label: `B1-S2-mandatory-gate-priority`
Decks: Invasion 25716 versus 1 Rey 25720

### Staging

1. Deploy Lower Corridor (`224_13`) with one modest Light character.
2. Initiate drains there on two consecutive Steve turns while Invasion remains pre-flip, and let Rando pay at least 1 Force each time.
3. Do not contest Naboo system or TPTR.
4. Stop and freeze the first Rando deploy decision where both a funded gate-progress plan and a viable Lower Corridor response are exact candidates.

### Opportunity packet

- The exact TPTR/Neimoidian gate requirement is still unmet.
- An exact gate-progress plan is legal, available, affordable, timely, and funded after location prelude.
- An exact Lower Corridor response plan is also legal, available, affordable, timely, and survivable.
- Both plans belong to the same Rando deploy decision.

`funded-mandatory-objective` is an internal obligation reason code, not a required production log tag.

### Pass contract

- No selected B1-PERSIST or B1-CRITICAL reason targets Cloud City in that decision.
- Replay shows the selected deploy advancing the Naboo gate, such as TPTR or an exact Neimoidian-to-TPTR step.
- No B1-FAILCLOSED occurs.

Only deploy selection is under test. A later move to Cloud City is a separate strategy question and does not fail S2. If the funded gate plan is not proved, a Cloud City response is not automatically wrong and the capture is inconclusive.

## S3: viable objective-critical eviction

Label: `B1-S3-critical-eviction`
Decks: Invasion 25716 versus 1 Rey 25720

### Staging

1. Wait for TPTR (`12_174`) to be on table while Invasion is pre-flip.
2. Deploy one weak, non-undercover Light character alone to TPTR.
3. Do not create any repeated drain lane in this game.
4. Do not reinforce or re-squat after removal.

### Opportunity packet

- TPTR is typed as the current ACTIVE_FLIP_GATE or another exact objective-critical role.
- Steve's battle participant remains at TPTR.
- The exact response plan consists only of characters. Rifles or other mixed card types do not count toward the formation proof.
- The whole character wave is legal, available, affordable, timely, and survivable against the weapon-adjusted public defender.
- No eligible funded mandatory plan correctly preempts it.

### Pass contract

- Deploy-policy component: each exact wave member selected for TPTR contains one B1-CRITICAL reason and then deploys there in replay.
- Band separation: B1-PERSIST count at TPTR is zero.
- Eviction component: Rando initiates battle at TPTR and the Light squatter is removed or forced away.
- No B1-FAILCLOSED occurs.

Report the deploy-policy and eviction components separately. Correct deployment followed by a declined battle is a deploy-policy PASS and a downstream battle FAIL. Do not misdiagnose it as a missed Batch 1 selection.

V76-CONFIDENT and L2-WAIVED are not required in S3. They have separate captures.

## S4: outmatched critical site, no suicide

Label: `B1-S4-critical-no-suicide`
Decks: Invasion 25716 versus 1 Rey 25720

### Staging

1. Put a heavy, weapon-adjusted Light stack at TPTR before Rando has massed a winning force.
2. Do not create persistent drains elsewhere.
3. Hold the stack through three Rando deploy phases or until one exact negative opportunity is proved.

### Opportunity packet

- At least one exact direct-deploy candidate to TPTR is offered and affordable. Otherwise the scenario tests no-response availability, not survivability.
- Public board plus exact Rando hand proves every offered response formation is nonviable or outmatched.
- Record raw power, weapon adjustments, ability, exact candidate members, cost, and current Force.
- FormationSafety or equivalent engine evidence identifies the losing row when available.

### Pass contract

- Zero selected B1-PERSIST and B1-CRITICAL reasons target TPTR in the negative window.
- Rando does not select a feed deploy to TPTR.
- Rando does not initiate battle there while the proved outmatched relation remains.
- No B1-FAILCLOSED occurs.

`V76 PREDICTOR CONFIDENT` is not prohibited merely because public power is high. It may be emitted while evaluating a different simulator state. The prohibited behavior is the selected suicide deploy or battle.

If the exact TPTR battle action itself has a safe V76 prediction at or above `0.75`, the battle-safety portion is not a proved suicide opportunity. Score the deploy-policy component separately and classify the battle component `INCONCLUSIVE_NO_OPPORTUNITY`.

## S5: one-off drain negative

Label: `B1-S5-one-off-negative`
Decks: EOPS 25636 versus MWYHL 25 25732

### Staging

1. Initiate exactly one drain at Beldon's Corridor and let Rando pay at least 1 Force.
2. Do not drain that site on the following Steve turn.
3. Create no other consecutive drain lane and occupy no Dark objective-critical site.
4. Freeze the next Rando deploy phase and end the capture before any later persistent lane can form.

### Pass contract

- Replay proves no site was drained on two consecutive completed Steve turns.
- B1-PERSIST and B1-CRITICAL are both zero in the capture.
- B1-FAILCLOSED is zero.

Any B1 positive reason is FAIL because the prerequisite fact is absent. Do not assert V166 silence. V166 uses current global drain balance and is independent of the two-turn ledger.

## S6: mid-game ledger continuity

Label: `B1-S6-midgame-ledger`
Decks: EOPS 25636 versus MWYHL 25 25732

### Staging

1. Play at least three ordinary turns with location deploys, character deploys, draws, and objective actions.
2. During the prelude, create no repeated Light drain lane and occupy no Dark critical site.
3. Later put one modest Light character at Beldon's Corridor or the drawn Carbonite Chamber.
4. Initiate a drain there on two consecutive completed Steve turns and let Rando pay at least 1 Force each time.
5. Anchor the current net drain balance below the V166 `>=2` gate before the second drain resolves.
6. Freeze the immediate following Rando deploy phase.

### Opportunity and pass contract

Use S1's exact opportunity packet and selected-action contract. In addition:

- No B1 positive reason appears before the response window.
- No B1-FAILCLOSED appears anywhere. A fail-closed reset is the specific S6 failure.
- V166 on the selected lane action is `INVALID_STAGING`, not PASS.

## S7: exact multi-member wave lifecycle

Label: `B1-S7-wave-lifecycle`
Decks: Invasion 25716 versus 1 Rey 25720

### Staging

1. Wait for pre-flip TPTR.
2. Put one mid-power Light character there, strong enough that a solo droid is nonviable but weak enough for a two-or-more-character Rando wave.
3. Create no persistent drain lane.
4. Do not reinforce or re-squat.

### Opportunity packet

- One exact selected response obligation contains at least two physical character members.
- Every member is legal, available, affordable, timely, selectable, and aimed at TPTR.
- Opponent presence and the objective-critical role remain current through both deploy decisions.

### Pass contract

- At least two consecutive selected deploy decisions each contain exactly one B1-CRITICAL reason for TPTR.
- Replay shows the corresponding two or more exact physical cards deploy to TPTR in that wave.
- No one selected decision contains the B1-CRITICAL reason twice.
- B1-PERSIST is zero.
- Unrelated, stale, or wrong-target deploys receive no B1 reason when their non-membership is independently proved.
- No B1-FAILCLOSED occurs.

The global expected B1-CRITICAL count is the number of exact proved wave members actually executed while the obligation remains current. A count of one in a proved two-member wave is FAIL. A second member never offered or no longer affordable is inconclusive unless its continuing availability was proved.

## E1: WMAOP lifecycle

Label: `E1-WMAOP-lifecycle`
Decks: Invasion 25716 versus an existing Light Steve deck, normally 1 Rey 25720

No deck edits are allowed. Repeat in a fresh labeled game if WMAOP is not drawn.

| beat | opportunity proof | required log or selected behavior | replay consequence |
|---|---|---|---|
| E1a deploy-only | WMAOP in hand, Bridge still in Reserve, genuine WMAOP mode offered outside Dark deploy phase | WMAOP-DEPLOY-ONLY candidate block | WMAOP is not played |
| E1b Blockade-only negative | WMAOP in hand during Dark deploy, Effect or Podracer mode offered | WMAOP-BLOCKADE-NEGATIVE | non-Blockade mode is not selected |
| E1c sanctioned pull | WMAOP in hand, owned Bridge absent from table and present in Reserve, Dark deploy phase | selected WMAOP-BLOCKADE-POSITIVE reasoning or equivalent selected pull preference | Rando plays WMAOP and deploys Blockade Flagship: Bridge |
| E1d live hold | before Bridge is out, a Force-loss decision includes an exact WMAOP-in-hand candidate plus another legal loss card | Rando selects a different loss card; WMAOP-LIVE-HOLD is collected if production logging exposes the rejected candidate | replay shows another card lost and WMAOP remains live |
| E1e fodder hold | after owned Bridge is on table, a Force-loss decision includes WMAOP | selected WMAOP-FODDER-HOLD reasoning | replay shows WMAOP selected as preferred loss fodder |
| E1f unrelated-action negative | a non-WMAOP objective pull is offered while WMAOP remains in hand | no WMAOP rule is attached to that exact selected action | objective pull remains live and resolves normally |

Important observability limit: successful `WMAOP.LIVE_HOLD` is a negative score on the WMAOP loss candidate. Production normally logs only the selected best action, and trace is disabled. Therefore the exact LIVE_HOLD reason may be absent when the rule works. Preserve the legal WMAOP candidate from the Rando replay decision, the selected different loss card from public replay, and the green focused boundary test. Do not turn absence of the LIVE_HOLD string into a false FAIL.

Owner scoping remains unit/source proof only in this campaign because no eligible current Light deck has `14_48`. Record this as `LIVE_FIXTURE_UNAVAILABLE_NO_DECK_EDIT`, not PASS and not FAIL.

## E2A: V76 confident-predictor reconciliation

Label: `E2A-V76-confident`
Decks: existing Invasion 25716 versus 1 Rey 25720

This is not a 2x-dominance scenario.

The source boundary pin is equal raw power `8` versus `8`, total ability `4` versus `4`, and an injected safe prediction of `0.98`. That exact source fixture produces the negative `V29: UNFAVORABLE` band and an equal positive V76 compensation. Live staging must reproduce those semantics, not pretend an ordinary equal-power random predictor is deterministic.

### Required board relation

- Rando total ability is at least 4, so the separate L2 veto is not involved.
- Raw/ability-weighted specific-battle evaluation is negative or unfavorable.
- The predictor runs safely and reports win rate at least `0.75` because of a modeled edge, such as tracked destiny distribution, that does not remove the negative specific-battle band.

### Pass contract

- Log contains V76-PREDICT with the exact site and win rate at least `0.75`.
- The same battle action contains V76-CONFIDENT after a negative specific-battle band.
- The immediately associated selected action is to initiate that battle.
- Replay shows Rando initiates battle at the same site.

If the specific-battle band is already nonnegative, V76-CONFIDENT is not expected because there is nothing to neutralize. A 2x-favorable board is one obvious invalid E2A staging shape.

## E2B: L2 low-ability 2x waiver

Label: `E2B-L2-waived`
Decks: existing Invasion 25716 versus 1 Rey 25720

### Required board relation

- Rando total ability is below 4, so normal battle destiny is unavailable.
- Opponent weapon-adjusted effective power is greater than zero.
- Rando weapon-adjusted effective power is at least two times opponent effective power.

Aim for the smallest clean relation, such as weapon-adjusted `6` versus `3` with Rando total ability `3`. Larger margins are valid, but they make it harder to prove that the L2 waiver, rather than unrelated battle enthusiasm, was the decisive boundary.

### Pass contract

- Log contains L2-WAIVED with the exact power numbers and site.
- The associated selected action is to initiate that battle.
- Replay shows Rando initiates battle at the same site.

V76-CONFIDENT is not required. If Rando ability reaches 4, the L2 veto never applies and the capture is invalid staging rather than a missed waiver.

## Phase 1 and Phase 2 evidence already accepted

MariaDB game `72276`:

- Steve recording: `pd4emldbzpvhtduh`
- Rando recording: `5vgvkjr4wo3ofq8y`
- Fresh post-`e877d4b6e` natural game, not a controlled scenario
- Phase 1 anchors: PHASE1-ATTACK, selected V96-CONCENTRATE plus V136 unified deploy-site reasoning, V136 dominance, and replay deployment/battle consequence
- Phase 2 anchors: PHASE2-PASSENGER candidate penalties plus replay-selected ground deployment and battle appetite

Do not use V29 candidate-penalty presence alone as outcome proof. The replay must show the selected deployment avoided a nonpilot passenger when a legal ground alternative was available.

## Machine-capture checklist

Counts apply only inside the exact DB-bounded log slice and the scenario's explicit decision window.

| label | required | forbidden or exact boundary | replay anchor |
|---|---|---|---|
| B1-S1-persistent-immediate | B1-PERSIST on every executed exact member | B1-FAILCLOSED; V166 on same selected action | two consecutive nonzero drain payments, then exact lane deploy |
| B1-S2-mandatory-gate-priority | selected gate-progress deploy | Cloud City B1 reason in the proved dual-opportunity decision | TPTR/Neimoidian progress |
| B1-S3-critical-eviction | B1-CRITICAL on executed exact members | B1-PERSIST at TPTR; B1-FAILCLOSED | TPTR deploys, battle, squatter removal |
| B1-S4-critical-no-suicide | exact negative opportunity packet | any selected B1 at TPTR, feed deploy, or battle | heavy stack persists through window |
| B1-S5-one-off-negative | exact one-off staging | any B1 positive reason | no consecutive paid drain |
| B1-S6-midgame-ledger | B1-PERSIST after churn | pre-window B1; B1-FAILCLOSED; V166 substitute | ordinary prelude, two drains, lane deploy |
| B1-S7-wave-lifecycle | at least two B1-CRITICAL selected decisions | duplicate reason on one action; B1-PERSIST | at least two exact TPTR deploys |
| E1-WMAOP-lifecycle | E1a through E1f evidence as observable | WMAOP spent on non-Blockade mode or lost while live | WMAOP/Bridge/loss consequences |
| E2A-V76-confident | V76-PREDICT and V76-CONFIDENT | already-favorable 2x staging | selected matching battle |
| E2B-L2-waived | L2-WAIVED | ability at least 4 or opponent effective power zero | selected matching battle |

## Timeout and outcome discipline

- A timeout, concession, or cancellation is never a natural Rando win.
- A branch completed before a later timeout or concession may receive `PASS_BRANCH_ONLY` when its selected log block and replay consequence both precede the terminal event.
- A timeout before the consequence is `INFRASTRUCTURE_ABORT` for that scenario.
- Never use a manual human match, lobby state, HTTP response, jar needle, or evaluator unit test as a substitute for live Rando branch evidence.

## Known live limitations

1. Random hands, draws, and Force can prevent the exact opportunity in any live scenario and may require reruns.
2. Exact rejected evaluator scores are unavailable from replay `D` elements.
3. Successful WMAOP LIVE_HOLD may be behaviorally visible but log-string invisible because the penalized WMAOP candidate is rejected.
4. WMAOP opponent-owner scoping cannot be staged without a current Light deck containing `14_48`; deck edits are prohibited.
5. E2A predictor outcome depends on random destiny modeling. Equal or slightly unfavorable public power improves staging but does not guarantee a `>=0.75` prediction.
6. These limitations produce explicit inconclusive or unavailable classifications. They never justify silently lowering the proof standard.
