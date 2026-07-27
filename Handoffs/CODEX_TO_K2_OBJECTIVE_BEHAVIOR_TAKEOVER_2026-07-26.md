# Codex to K-2 Objective Behavior Takeover

Date: 2026-07-26
Takeover window: now through 2026-08-01
Repository: `/Users/steve/gemp-swccg-public`
Primary branch lineage: `rando-consolidation-2026-06-23`
Author: Codex/Alfred
Recipient: the fresh K-2 lead session titled `Objective flip condition audit`

## 1. Purpose

K-2 is the lead integrator until Codex credits reset on August 1. Continue the objective audit and
behavior work without restarting the research, weakening the proof standard, or repeating the
high-overhead audit cadence that slowed delivery.

The operating rule is:

> Prove one objective family can reach and preserve its real source-defined state, seal that family,
> deploy it, tell Steve it is ready to test, then continue.

Do not wait for every objective to be researched before improving the next playable objective.
Do not call a data row behavioral proof. Do not require a universal architecture before implementing
a typed route that source and tests can prove.

## 2. Read Order For The Fresh K-2

Read in this order:

1. `AGENTS.md`
2. `Handoffs/K2_MASTER_ONBOARDING_2026-07-27.md`
3. `resources/BUILD_AND_DEPLOY.md`
4. This handoff
5. `.claude/skills/rando-objective-behavior-audit/SKILL.md`
6. `.claude/skills/rando-objective-behavior-audit/references/methodology.md`
7. `.claude/skills/rando-objective-behavior-audit/references/batch-checklist.md`
8. `Handoffs/CODEX_OBJECTIVE_FLIP_BEHAVIOR_DEEP_TEST_HANDOFF_2026-07-24.md`
9. `Handoffs/AI_PROTOCOL.md`

The new skill is the reusable operating procedure. This file owns current state, queue, and takeover
decisions. K-2's master handoff owns broad GEMP onboarding, memory, authentication, and project-wide
startup context.

## 3. User Instructions That Control This Takeover

- Audit every objective and prove its facts alter gameplay.
- Modify only Rando/Chosen One AI logic, AI tests, approved objective data, audit evidence, and
  required AI documentation.
- Never modify engine or card Java.
- Preserve unrelated dirty files.
- Keep Rando and Chosen One behavior mirrored.
- Use Rando versus Chosen One for automated games. Do not use `asdf` as the automated opponent.
- Treat Steve's live replay report as authoritative over earlier completion claims.
- Deploy after an objective or coherent objective family is fully edited and verified. Do not deploy
  every micro-change.
- Never deploy over a live game.
- Publish sealed safety branches only to `srdmusic/gemp-swccg`. Never push or merge a
  PlayersCommittee branch.
- Keep one writer/integrator. Lower agents may do read-only source, replay, test, or review packets.

## 4. GitHub Verification: Complete

K-2 independently verified the web publication in mailbox message `m01442`.

| Check | Verified state |
|---|---|
| Repository | `srdmusic/gemp-swccg` |
| Draft PR | `https://github.com/srdmusic/gemp-swccg/pull/1` |
| PR state | Open and draft |
| PR head | `rando-consolidation-2026-06-23` |
| PR head SHA | `d0f530ddedd0ba944d6e2051aca871bd27dac46b` |
| PR base | The fork's own `master` |
| PlayersCommittee | Untouched by Codex and K-2 |

PlayersCommittee's remote `master` advanced independently beyond the local snapshot. That is upstream
work, not one of our pushes. Do not mistake upstream movement for contamination.

## 5. Clean Branches On Steve's Fork

| Branch | SHA | Meaning |
|---|---|---|
| `rando-consolidation-2026-06-23` | `d0f530dde` | Main sealed objective history through TDIGWATT documentation |
| `codex/tdigwatt-shield-live` | `93f0fd2c0` | TDIGWATT plus Hoth replay repairs; this matches the current live code lineage |
| `codex/hoth-shield-pilot-repair` | `1e87b7af2` | Intermediate Hoth pilot repair; retained for recovery, not the preferred integration head |
| `codex/eop-replay-fix` | `68896470b` | Five Endor Operations replay-repair commits on the older `8887a0216` lineage |

The main working tree was already heavily dirty before this handoff. It contained 59 modified or
untracked entries before the new handoff and skill were added. Do not compile, package, commit, or
deploy from it.

Do not use `ours`, `theirs`, a whole-tree copy, or a blind branch merge to combine the clean lines.
The EOP branch diverged before Capture, TDIGWATT, and the Hoth repairs, and it overlaps shared policy
files. Integrate behavior and tests intentionally in a clean worktree.

## 6. Current Live Artifact

K-2's bytecode forensics in mailbox message `m01439` established:

- The current live jar contains the `codex/tdigwatt-shield-live` behavior.
- TDIGWATT and the Hoth repair are present.
- The later EOP replay repair from `codex/eop-replay-fix` is absent.
- A game played while the EOP jar was no longer loaded cannot prove the EOP repair.

Keep artifact claims separate:

```text
source/test pass
  != packaged jar
  != running JVM
  != rule fired
  != replay observed
  != objective remained healthy after flip
```

## 7. Objective Progress At Takeover

The project has 58 runtime profiles and 66 front-side objective printings. Completed behavior batches
cover 12 objective families and 16 printings.

| Family | Printing(s) | Sealed commit | Strongest honest evidence |
|---|---|---|---|
| Invasion | `14_113` | `ef8790b73` | Native flip chain tests plus Steve live-observed flip |
| Dantooine Base Operations | `7_135` | `fef64a826` | Engine contract and regional decision tests |
| Ralltiir Operations | `7_300` | `fef64a826` | Engine contract and regional decision tests |
| Zero Hour | `219_48` | `fef64a826` | Engine contract and regional decision tests |
| We Have A Plan | `14_52` | `cf788f8c8` | Native trigger and public-bot candidate decisions |
| Hunt Down And Destroy The Jedi | `7_297`, `213_31` | `dc3d996fc` | Native triggers and cross-phase decision tests |
| Endor Operations | `8_167` | `ee64e6f3b`; replay branch `68896470b` | Base family sealed; replay repair proven separately but not in current live jar |
| The Shield Will Be Down In Moments | `222_14`, `222_30` | `000cbcf1b`; live repair `93f0fd2c0` | Live generator route and flip observed; adjacent regressions remain |
| The First Order Reigns | `225_32`, `501_60` | `108830a8b` | Both native flips, full decision suite, package and clean deployment |
| Bring Him Before Me | `9_151` | `78f465fea` | Native capture/flip plus persistent route tests |
| There Is Good In Him | `9_61` | `78f465fea` | Native capture/flip plus persistent route tests |
| This Deal Is Getting Worse All The Time | `109_12`, `226_12` | `7f2b29067` | Both native flips, 74-test focused gate, full reactor, current JVM lineage |

Do not turn this table into a claim that every family is `PRODUCTION_VERIFIED`. Each row retains its
actual proof ceiling.

## 8. Latest Live Shield Replay: Preserve What Worked

K-2 closed the Shield replay audit in mailbox message `m01441`.

Replay: `replays/asdf/93r5wrrnbo3q91j0.xml.gz`
Objective: `222_30` The Shield Will Be Down In Moments
Live flip event: `6160`

All five intended Hoth repair behaviors fired correctly:

- Baron and Ozzel chose offered walker boarding destinations without site-only penalty leakage.
- Electro-Rangefinder was treated as an attached device, not cargo or a weapon.
- Veers' empty Reserve search was blocked without a dead-search loop.
- The first AT-AT Cannon received the exact objective route credit.
- Target The Main Generator followed the walker, fired twice, blew away Main Power Generators,
  caused five Force loss, and triggered the real objective flip.

Preserve those behaviors. Do not rewrite the successful route.

`PRODUCTION_VERIFIED` remains blocked by four adjacent defects:

1. The Battle Order gate uses Rando's own battleground-site/system coverage. The card taxes the
   player initiating the drain unless that draining player occupies both. The decision must inspect
   the opponent's coverage.
2. Generic V153 Force-loss ranking discarded all three starships from hand, leaving Hoth system
   empty and killing the space plan.
3. Post-flip V41/V166 movement evacuated the remaining objective anchor into a late doomed stack.
   The flipped objective went out of play one opponent turn after the flip.
4. V201 deferred Veers' valid `+3000` vehicle destination behind a planned site already vetoed by
   Formation Safety. The resulting Pass fed the V163 cancel latch, and Veers never deployed.

Correction from the replay:

- The second Cannon was not an AI preference defect. The engine offered only the already-armed
  Marquand walker. V25 correctly refused a second weapon. The other walker was unpiloted, so it
  could not use or fire the Cannon. Close this item. Do not add an AI carrier-preference rule.

K-2 closed the design packet in mailbox message `m01445`. The full evidence is preserved at
`resources/objective_flip_audit/HOTH_REPAIR_DESIGN_2026-07-27.txt`. Treat it as bounded design
input, not permission to rewrite working logic. Its repair boundaries are:

- Preserve exactly one affordable objective-theater ship and pilot pair when the named battleground
  system is on table and Rando occupies no battleground system. Do not protect every ship.
- Open Battle Order when the opponent lacks both theaters and either Rando is self-exempt or the
  drain balance justifies accepting the self-tax. Preserve Allegations and Secret Plans as slots one
  and two, and re-key the third-slot reserve so it does not block Battle Order itself.
- Guard against concentrating every valid post-flip Shield anchor at one unsafe site. This is an
  anchor-concentration defect, not merely a last-actor departure defect.
- Release V201 deferral when the planned destination is already hard-blocked by Formation Safety,
  allowing Veers' valid `+3000` vehicle destination to compete.

The packet also found a prerequisite defect: all 37 later Knowledge And Defense decisions logged
turn `1`, so V102 capped shields at two for the entire game. Probe the AI turn-number input before
implementing the Battle Order repair.

## 9. Immediate Takeover Queue

### Priority 0: Probe and repair the AI turn-number defect

Reproduce why `DecisionContext.getTurnNumber()` reported `1` across turns two through seven in the
Shield replay. The suspected AI-side source is `gameState.getPlayersLatestTurnNumber(playerId)`.

Success criteria:

- A focused runtime or deterministic AI test proves the wrong turn value before the fix.
- The fix stays inside Rando/Chosen One AI logic and tests. Do not modify engine or card Java.
- Knowledge And Defense's per-turn cap resets on later turns.
- Adjacent turn-number policies receive the real current player turn.
- Rando and Chosen One remain mirrored.

Do this first because a stuck turn number distorts every later live objective test.

### Priority 1: Build one clean consolidated lineage

Use K-2's staged integration plan. The safe target must contain:

- Main sealed objective history through TDIGWATT.
- The three Hoth repair commits ending at `93f0fd2c0`.
- The EOP replay behavior ending at `68896470b`.
- No main-tree dirty hand merge.

Recommended base is `93f0fd2c0` because it matches current live behavior. Bring in the docs-only
`d0f530dde` delta and port EOP behavior commit by commit, resolving shared policies by tested intent.
Do not blindly merge the EOP branch.

Required combined gate:

- EOP focused and replay-shaped tests.
- Shield/Hoth focused and replay-shaped tests.
- TDIGWATT focused tests.
- Capture compatibility tests for shared policy surfaces.
- Rando/Chosen One normalized parity.
- Clean full reactor because shared common policies overlap.
- Clean async package and byte parity.

### Priority 2: Patch the four Shield live regressions

Use replay `93r5wrrnbo3q91j0` as the fixture. Preserve the live flip route.

Implement one coherent adjacent-repair batch, not four disconnected experiments:

- Correct Battle Order using both the draining opponent's theater coverage and Rando's self-tax
  exposure. Keep the global rule global. Preserve first- and second-shield ordering and compose the
  third-slot reserve with the corrected gate.
- Protect exactly one affordable objective-theater ship and pilot pair from generic Force-loss
  ranking when the objective's named battleground system is open and unoccupied by Rando.
- Prevent unsafe concentration of every exact source-valid post-flip Shield anchor while still
  releasing an anchor that must retreat from a hopeless site.
- Prevent a Formation-Safety-blocked planned destination from deferring a valid vehicle
  destination, and prevent the resulting cancel latch.

Do not add an AI fix for the second Cannon. That item is closed as working as intended.

### Priority 3: Finish Batch Nine, Bespin control family

WIP worktree:

`/private/tmp/gemp-batch-nine-bespin-control`

Branch:

`codex/batch-nine-bespin-control` at clean parent `d0f530dde`

Objectives:

- Quiet Mining Colony / Independent Operation, `109_4`
- City In The Clouds / You Truly Belong Here With Us, `301_2`

Current WIP:

- Eight modified production/data files.
- Four untracked engine/behavior/parity test files.
- No commit.
- No changelog or final audit-record update.
- No trusted compile, full sequence gate, package, or deployment.

Source law already rechecked:

- Quiet Mining Colony front requires no opponent control of any Bespin location, owner control of
  Bespin: Cloud City, and either two controlled Cloud City sites or one controlled Cloud City site
  while Lando or Lobot is on Cloud City. Its back flips for opponent control of Bespin system or
  three controlled Cloud City sites and/or Bespin cloud sectors. Either side leaves play if Bespin
  is blown away.
- City In The Clouds front requires two controlled Cloud City battleground sites, occupation of
  Bespin system, and no opponent control of any Cloud City site. Its back flips only when the
  opponent controls strictly more Cloud City sites than the owner.

The WIP is useful but not sealed. Before adopting it:

- Rebase or replay it onto the clean consolidated head.
- Re-run the unchanged-engine contract tests.
- Add real candidate-selection tests against Pass and plausible Bespin distractions.
- Prove pull, deploy, move, sole-presence Force loss, native flip, and back-side hold.
- Verify exact `SpotOverride` semantics and the QMC two-route disjunction.
- Update both history files and the audit matrix.

### Priority 4: Finish the simple existing-engine-fit families

Prefer these before inventing new primitives:

1. Massassi Base Operations `111_4`
2. My Lord, Is That Legal? `12_179`
3. Imperial Entanglements `201_39`
4. Old Allies `204_32`
5. They Have No Idea We're Coming `209_29`
6. The Empire Knows We're Here `222_27`
7. Twin Suns Of Tatooine `301_4`
8. More Systems Will Rally To Our Cause `501_19`
9. Hunt Down And Destroy The Jedi (V) `601_87`, only after source proves it is a true sibling

Then take small shared extensions in source-compatible pairs:

- Watch Your Step `10_26` and `601_146`
- Rebel Strike Team `8_78` and `501_94`
- You Can Either Profit By This `110_4` and No Money, No Parts, No Deal `12_180`
- Local Uprising `7_137`, Imperial Occupation `7_298`, and ISB Operations `7_299`
- The Hidden Path `226_28`
- Set Your Course For Alderaan `111_6`

Defer true new-primitives until existing mechanisms and WIP families are exhausted.

## 10. The Method That Produced The Successful Batches

### Step 1: Read the executable law

Read front and back card Java, exact filters, timing, and supporting route cards. Do not stop at
printed prose. Find the action Rando can choose one level upstream from a passive trigger.

Effort bar:

- Simple location/count law: 30 to 60 minutes.
- Dynamic, capture, Epic Event, or multi-card route: 1 to 2 hours.

### Step 2: Turn the law into current missing state

Represent the objective as:

```text
requirements satisfied now
requirements missing now
next legal advancing candidate
downstream Force and card obligations
sole enablers that must survive
native trigger timing
post-flip hold and hazard
```

Do not give a generic objective bonus to every thematically related card. Score only the next
source-backed missing leg.

Effort bar: 30 to 60 minutes.

### Step 3: Find the actual losing decision

Use the replay or construct the smallest deterministic candidate set:

- Advancing action.
- Pass.
- Plausible tactical distraction.
- Objective-looking near miss.

Trace every live evaluator contribution. Identify the real winner, strongest safety veto, and call
site ordering. Do not adjust numbers until this comparison is known.

Effort bar: 30 to 90 minutes.

### Step 4: Write the failing behavior contract

Mandatory:

- Native engine trigger test.
- Positive and near-miss source cases.
- Candidate winner test.
- Cross-phase sequence test when more than one phase is involved.
- Rando/Chosen One winner parity.

A facts-presence test is useful but not sufficient.

Effort bar:

- Simple family: 1 to 2 hours.
- Multi-phase or dynamic family: 2 to 4 hours.

### Step 5: Implement the smallest typed route

Use `ObjectiveAnalyzer` or a typed reader for identity and state. Use the live phase policy for
scores, ordering, reserves, releases, and vetoes. Keep mirrored adapters thin.

Gate on:

- Exact objective and side.
- Exact missing state.
- Legal action family.
- Exact physical candidate identity.
- Affordability.
- Tactical viability.

Close the rule after its obligation is complete.

Effort bar: 1 to 3 hours after the failing contract exists.

### Step 6: Do boundary math

Require both:

```text
viable objective action beats strongest distraction and Pass
doomed objective action still loses to the real safety veto
```

This is the Invasion lesson. Rando must decisively take the empty viable Throne Room route and still
decline a hopeless attack after the opponent establishes an overwhelming stack.

Do not make safety weaker globally. Make the objective route viable, exact, and conditional.

Effort bar: 15 to 45 minutes.

### Step 7: Prove the persistent chain

Test that earlier actions preserve what later phases need:

- Pull does not select the wrong printing.
- Deploy does not starve movement or battle Force.
- Formation does not strand a weak solo.
- Movement does not evacuate a completed gate.
- Force loss and forfeit do not discard the sole enabler.
- Native source performs the real flip.
- Post-flip play holds the exact source-defined state.

Effort bar: 1 to 2 hours, longer for capture or Epic Event families.

### Step 8: Seal and deploy the family

There is no mandatory test-count number. Coverage defines readiness.

Minimum shippable focused gate:

- Every new test.
- Every directly impacted shared-policy suite.
- At least one regression test for each earlier objective using the same shared owner.
- Exact Rando/Chosen One parity.
- Clean compile and async package.
- Byte verification for changed classes and objective data.

Run the full reactor before deployment when:

- Shared `common` policy behavior changed.
- A core evaluator ordering point changed.
- Multiple objective families share the edited owner.
- The batch integrates divergent branches.

A truly objective-local adapter or data correction may ship on the focused impact matrix without a
full reactor only when no shared owner changed and the reason is recorded.

Effort bar:

- Focused verification: 30 to 60 minutes.
- Full reactor and clean package: 45 to 120 minutes.
- Zero-table deploy and health proof: 15 to 30 minutes.

### Step 9: Continue before live replay

After deterministic native-flip, decision, parity, package, and fresh-JVM gates pass:

- Tell Steve the family is ready to test.
- Continue to the next family.
- Do not claim live replay proof until it happens.

This preserves speed without lying about evidence.

## 11. Why This Cadence Avoids The Prior Snags

K-2's prior rigor was valuable for catching source and scope defects. The slowdown came from applying
the deepest audit cadence to every intermediate step.

Use strictness for:

- Source truth.
- Typed legality.
- Real tactical suicide or hard-loss protection.
- Scope.
- Artifact integrity.
- Honest claims.

Do not stop for:

- A missing universal abstraction when a typed policy is enough.
- An unrelated theoretical overlap not present in the target decision.
- Lack of a live replay after deterministic flip and decision proof.
- A desire to model every possible board state.
- An audit document that can be updated after the behavior packet is green.

Operational adjustments:

- One coherent objective-family commit, not many micro-packets.
- One final independent review, not a gate after every assertion.
- One unknown produces one targeted negative test.
- A lower agent returns evidence, not competing production edits.
- Preserve safety rules, but do not let them make every objective route impossible.
- Treat user live failures as patch-forward evidence, not proof the whole family should be discarded.

## 12. Failed Live Replay Procedure

When Steve reports a failure:

1. Lower the proof claim immediately.
2. Preserve every behavior that fired correctly.
3. Reconstruct the actual replay board state and offered candidates.
4. Classify the failure:
   `SOURCE`, `FACT`, `CONSUMER`, `RANKING`, `SEQUENCE`, `ARTIFACT`, or `ENGINE_OFFER`.
5. Write one replay-shaped failing test.
6. Amend the existing live owner when it owns the defect.
7. Re-run the family plus shared-owner compatibility gate.
8. Deploy the sealed correction when no table is active.
9. Re-test the original successful route and the new correction.

Do not add AI policy for an engine-offer problem until the offer boundary is proven.

## 13. What K-2 Owns Until August 1

- Act as sole production writer and integrator.
- Maintain clean branch and artifact truth.
- Keep the mailbox current.
- Complete the consolidated live line.
- Repair the four live Shield regressions without breaking the flip.
- Finish Batch Nine using the new skill.
- Continue through existing-engine-fit families.
- Deploy each sealed objective family and tell Steve it is ready.
- Push sealed safety branches only to Steve's fork.
- Leave a compact evidence note after every family:
  commit, tests, package hash, live hash, replay ID, proof ceiling, and next target.

Lower agents may:

- Read card source.
- Reconstruct replay timelines.
- Search existing owners.
- Review tests and boundary math.

Lower agents may not:

- Edit production concurrently.
- Merge branches.
- Package or deploy.
- Publish remote branches.

## 14. August 1 Return Plan

When Codex returns:

1. Read K-2's latest master handoff and mailbox evidence.
2. Verify the current fork branch and live jar.
3. Review only batches completed during the credit gap.
4. Resume the remaining objective queue using the same skill.
5. Do not redo source research that K-2 already cited and tested.

The goal is continuity, not another ceremonial reboot.

## 15. Definition Of Done Per Family

A family is ready to deploy when:

- Front and back law are source-verified.
- The current missing state has an active runtime consumer.
- A real advancing candidate beats Pass and a plausible distraction.
- Near misses, wrong printings, and unrelated objectives receive no credit.
- Downstream Force, actors, cards, and locations survive the full sequence.
- The unchanged objective card performs the actual flip in the harness when feasible.
- Rando and Chosen One choose equivalently.
- Focused and required shared tests pass.
- The clean package contains exact tested bytes.
- Scope contains no engine or card Java change.
- The audit record and both AI history files are current.

Live replay and `PRODUCTION_VERIFIED` remain later proof layers. They do not block the next family
after the sealed deployment, but they do control what may honestly be claimed.
