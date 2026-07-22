  ==== V232 AI-ONLY MOVE WEAPON-HUNTER POLICY OWNER (2026-07-19, both bots) ====
Shared MoveWeaponHunterPolicy now owns the duplicated V29.7 attached-weapon classification,
effective-power facts, strict target viability, ordered scoring, first-best selection, exact reasons,
and selected-target ordinal. Both adapters retain the aggressive-solo gate, attachment/category and
IHYN hand reads, inner and outer catches, physical location/opponent/icon scans, target list, direct
score and logs, engine adjacency query, R2 claim, and position between V29.9 and SPREAD. Behavior is
unchanged: the last weapon title is displayed while any lightsaber gives +4; other weapons give +2;
Vader with IHYN gets +3; viability requires a positive power-bearing opponent count and strict power
advantage. Scores remain base +60, +40 at advantage >=6 or +20 at >=3, +15 per opposing icon, and
+150 for Vader at a Luke-title location. Strict ties keep the first target. The global foundLuke bug,
null-title suppression, attached-only weapon recognition, undercover counting, all-location scan,
opponent-weapon omission, and flat IHYN heuristic remain intact. Only an adjacent selected target
attempts the existing battle-seeking R2 claim; remote targets remain R1 fines. V29.9 readiness,
generic attack/SPREAD, MoveHuntTargetPolicy, and battle-side weapon owners are untouched. Production
edits are AI-only. Verification passed 272 focused MOVE tests and the full 1,449-test reactor with 0
failures, 0 errors, and 26 skipped. Package, strict-order, cross-owner, source-boundary,
forbidden-symbol, artifact, mirror, diff, and independent source-review gates passed; server jar
SHA-256 is 4cf28273d1f22c888e513fce2541d09b53c2424495a2687b3a05f2ee646d79c7 and packaged web.jar SHA-256
is faaf7aa546085d7ad346e4546d080f130546b09ddedca76dcc3cf115e1eaba28. Runtime load and live-game
proof remain separate gates. Revert the single V232 commit. See AI_CHANGELOG 2026-07-19.

  ==== V231 AI-ONLY MOVE UNARMED-VADER READINESS OWNER (2026-07-19, both bots) ====
Shared MoveUnarmedVaderPolicy now owns the duplicated V29.9 NONE, EQUIP_FIRST, and UNARMED
classification plus exact reasons and -250/-100 contributions. Both adapters retain the aggressive-
solo outer gate, Vader title read, attached-card WEAPON scan, conditional lightsaber-title hand scan,
broad catch, direct score application, logs, and fallthrough into V29.7. Behavior is unchanged: an
unarmed title-containing Vader with any lightsaber title in hand receives -250, otherwise -100;
armed Vader and non-Vader cards receive nothing. The rule remains additive R1 scoring with no early
exit, ladder claim, or veto. Its attached-card-only weapon limitation remains intact, so blueprint
permanent-weapon variants can still be classified as unarmed. The separate V29.7 MOVE weapon hunter
and battle-side V29.9 Barrier/Hunt Down owners are untouched. Production edits are AI-only.
Verification passed 251 focused MOVE tests and the full 1,428-test reactor with 0 failures, 0 errors,
and 26 skipped. Package, order, cross-owner guards, source-boundary, forbidden-symbol, artifact,
mirror, diff, and independent source-review gates passed; server jar SHA-256 is
652f5895c4318d3fed82b2ba6334fae2f0f7da084db3dba250f4cab562246456 and packaged web.jar SHA-256 is
a613f073814c0390580f5159097833ed27427d4745baba9673b7b899318a9ea0. Runtime load and live-game
proof remain separate gates. Revert the single V231 commit. See AI_CHANGELOG 2026-07-19.

  ==== V230 AI-ONLY MOVE VERGE STEERING POLICY OWNER (2026-07-19, both bots) ====
Shared MoveVergePolicy now owns the duplicated V79/V79b pure branch order, last-successful parsec
parse, exact reasons, raw contributions, and post-flip hard-veto fact. Both adapters retain the
Death Star title gate, owned in-play objective scan, blueprint and zone reads, getSystemOrbited
fact, conditional objective-analyzer read, catches, direct score and ladder mutation, logs, and
original position before V25. Behavior is unchanged: orbiting Scarif remains +1500; parsec 7 is
+1200; parsecs 6 or 8 are +1000; every other parsec above 4 is +700; parsecs 0 through 4 are -300;
and text without a parsec is +500. Orbit text still wins before parsing, the last successful parse
still wins, and a later overflow preserves the earlier destination. Already orbiting Scarif remains
unscored pre-flip and hard-vetoed post-flip. RandoCalAi V79b and both ActionText V79/V103 parsec
choice owners are untouched. Production edits are AI-only. Verification passed 240 focused MOVE
tests and the full 1,417-test reactor with 0 failures, 0 errors, and 26 skipped. Package, order,
cross-route guards, source-boundary, forbidden-symbol, artifact, mirror, diff, and independent
source-review gates passed; server jar SHA-256 is
022f8b95bd50368353b52070f7b7ec6c55f8a194a7aeb9254f3857a03b32b50d and packaged web.jar SHA-256 is
afaff8264e6ab8304731c5298864be4fc19dabc0489533aa31d068557456dfb3. Runtime load and live-game
proof remain separate gates. Revert the single V230 commit. See AI_CHANGELOG 2026-07-19.

  ==== V229 AI-ONLY MOVE BLOCKED-RESPONSE POLICY OWNER (2026-07-19, both bots) ====
Shared MoveBlockedResponsePolicy now owns the duplicated exact blocked-action match and the
NOT_BLOCKED, ENDANGERED_FALLTHROUGH, and HARD_BLOCK classification. Both adapters retain the
isMoveAction gate, all game/context/card/power reads, broad swallowed exception, zero-base MOVE
action construction, exact logs, action-list append, continue, and original position before
capacity-slot routing. Behavior is unchanged: matching remains an exact case-sensitive set lookup
against either nonempty action ID or action text. A match falls through only after both power reads
complete and opponent power is strictly greater. Missing facts, read failures, equal or lower power,
and NaN still create the zero-base action with an additive -100000 contribution, append, log, and
continue. The separate ActionTextEvaluator V169 owner remains untouched at three retries of -250,
then -100000, under its narrower move/using/transport/relocate predicate. General MOVE routing,
ladder setup/finalization, physical-card resolution, and capacity-slot routing are untouched.
Production edits are AI-only. Verification passed 222 focused MOVE tests and the full 1,399-test
reactor with 0 failures, 0 errors, and 26 skipped. Package, exact-order, source-boundary,
forbidden-symbol, artifact, mirror, ActionText V169 guard, diff, and independent source-review gates
passed; server jar SHA-256 is 6ab685023201906f67593ca06e1ca6554e931799a44d007ff8048f31c48ea399
and packaged web.jar SHA-256 is 334854bfe444163dd56a807da6ac6a61c246b321cadb50069b0042d47d6b5e41.
Runtime load and live-game proof remain separate gates. Revert the single V229 commit. See
AI_CHANGELOG 2026-07-19.

  ==== V228 AI-ONLY MOVE CAPACITY-SLOT ROUTE OWNER (2026-07-19, both bots) ====
Existing shared MoveTransitPolicy now also owns the duplicated passenger-first capacity-slot
classification plus the pilot branch's exact base score and contribution. Both adapters retain
V160/V169 ordering, passenger/pilot logs, EvaluatedAction construction, action-list mutation,
continue control flow, and the position before general MOVE ladder setup. Behavior is unchanged:
passenger-slot text emits no MoveEvaluator action and continues; pilot-slot text creates a MOVE
action at base 100, adds +50 with the exact reason, appends it, logs, and continues. Passenger still
wins when both substrings occur. The separate ActionTextEvaluator V87 -3000 no-swap rule remains
untouched for both directions, so the existing pilot +150 versus V87 -3000 contradiction is now
characterized rather than tuned. V160/V169, V87, general routing, ladder reset/finalization, and
physical-card resolution are untouched. Production edits are AI-only. Verification passed 208
focused MOVE tests and the full 1,385-test reactor with 0 failures, 0 errors, and 26 skipped.
Package, source-boundary, forbidden-symbol, artifact, mirror, V87 cross-evaluator guard, and
independent source-review gates passed; server jar SHA-256 is
b9d16accbf9e1b5cd77dca0db2c8f170ad73527e9dd0cb77bb9bf3f4438adf3c and packaged web.jar SHA-256 is
1617d962a3f2998d380659619da3c24f7fbd9a1c0acdf965867262e78f29d6f0. Runtime load and live-game
proof remain separate gates. Revert the single V228 commit. See AI_CHANGELOG 2026-07-19.

  ==== V227 AI-ONLY MOVE FLEE CONTRIBUTION OWNER (2026-07-19, both bots) ====
Existing shared MoveThreatPolicy now also owns the duplicated generic FLEE strict predicates,
disadvantage calculation, integer-truncated reason, caller-supplied threshold/base delta, and
capped raw contribution. Both adapters retain board-power scanning, direct score application,
the absence of logging or ladder mutation, and the original position after V85 and before attack
analysis. Behavior is unchanged: FLEE applies only when opponentPower - ourPower is strictly above
2 and opponent power is strictly positive; its score remains 10 * min(disadvantage / 2, 5), capped
at +50. There is still no rank claim, veto, log, early return, destination check, or reachability
check. Float truncation plus NaN, zero, negative, and exact-threshold behavior remain intact. V85,
attack opportunity, V29.7/V29.9, routing, finalization, and physical-card resolution are untouched.
Production edits are AI-only. Verification passed 201 focused MOVE tests and the full 1,378-test
reactor with 0 failures, 0 errors, and 26 skipped. Package, source-boundary, forbidden-symbol,
artifact, mirror, and independent source-review gates passed; server jar SHA-256 is
d97739b368db5d5b957ceb9c1264666ddc6f6d19c76f7ccf08084146af6e8998 and packaged web.jar SHA-256 is
203d1f5e917375aaba84abadcfc40b421ac599e9760e938400b9aaac8a32a279. Runtime load and live-game
proof remain separate gates. Revert the single V227 commit. See AI_CHANGELOG 2026-07-19.

  ==== V226 AI-ONLY MOVE HIDDEN-PATH TRANSIT OWNER (2026-07-19, both bots) ====
Existing shared MoveTransitPolicy now also owns the duplicated V53b/V60 Hidden Path parent MOVE
classification: objective-title gate, source-title branches, landspeed predicate, exact reasons,
raw +800 contributions, R4 claim identities, and Corridor hard-veto fact. Both adapters retain
analyzer access and isAnalyzed gating, direct score and ladder mutation, warnings, and the finalizer
position immediately after the block. Behavior is unchanged: Safehouse landspeed remains +800 plus
V53b SAFEHOUSE→CORRIDOR; broad Underground/Corridor landspeed remains a hard veto finalized at
-100000; and other Mapuzo landspeed remains +800 plus V53b MAPUZO EXIT. Exact `move`, branch
precedence, any-character behavior, null-source handling, source-only routing, and the lack of
destination or Jedi-legality validation remain intact. The separate positive V60 ActionText action
is untouched at +20000 for Hidden Path and +200 otherwise. V38.3 transit suppression, V67 selection,
routing, finalization, and physical-card resolution are untouched. Production edits are AI-only.
Verification passed 195 focused MOVE tests and the full 1,372-test reactor with 0 failures, 0 errors,
and 26 skipped. Package, source-boundary, forbidden-symbol, artifact, mirror, positive-ActionText
guard, and independent source-review gates passed; server jar SHA-256 is
ccdd66ad912888bd303db57d681d38c26593c054463b8f461112cdc132c90ba2 and packaged web.jar SHA-256 is
4d44ebffb9fb31cc6f4555038427c7df02f94991fb981475d399c65408303366. Runtime load and live-game
proof remain separate gates. Revert the single V226 commit. See AI_CHANGELOG 2026-07-19.

  ==== V225 AI-ONLY MOVE UNDERCOVER-SPY POLICY OWNER (2026-07-19, both bots) ====
Shared MoveSpyFollowPolicy now owns the duplicated V53 undercover-spy opponent lookup, direct
source-power read, first textual top-location destination scan, branch selection, exact reasons,
raw contributions, and doctrine-claim fact. Both adapters retain the undercover/game-state gate,
player-id callback, direct score application, warnings, R2 ladder claim, outer catch, and original
position after V27 maintenance and before V53b. Behavior is unchanged: moving from an empty source
to the first textual opponent destination remains FOLLOW +500, and leaving opponent power for an
empty destination remains STAY -300. The historical REPOSITION +400 arm remains unreachable behind
its identical earlier FOLLOW condition. Null-source handling, first-match stopping, direct-read
failure boundaries, and add/log/ladder order remain intact. V53b, V60, V137, routing, finalization,
and physical-card resolution are untouched. Production edits are AI-only. Verification passed 186
focused MOVE tests and the full 1,363-test reactor with 0 failures, 0 errors, and 26 skipped.
Package, source-boundary, forbidden-symbol, artifact, mirror, and independent source-review gates
passed; server jar SHA-256 is de491e818f45c630baa1ea2cd2cd81e974555109b58a77c01894b36dbae9dd66
and packaged web.jar SHA-256 is 41223078c60a769283e585b7d31013510a8c5ff72c36b9280dd1823dbced28ea.
Runtime load and live-game proof remain separate gates. Revert the single V225 commit. See
AI_CHANGELOG 2026-07-19.

  ==== V224 AI-ONLY MOVE OBJECTIVE-CONSOLIDATION POLICY OWNER (2026-07-19, both bots) ====
Shared MoveObjectiveConsolidationPolicy now owns the duplicated V22.5 pre-flip consolidation and
V22.2 post-flip protection scans, strict thresholds, partial-result behavior, exact reasons, raw
contributions, and doctrine-claim facts. Both adapters retain analyzer gating, direct score
application, logs, R2 ladder claims, and the original position after Hunt Down cohesion and
before movement-type scoring. Behavior is unchanged: pre-flip lone movement remains +100/+160
and small-group pressure +60; post-flip protection remains -30 or -80/-120/-160, lone
reinforcement remains +80/+120/+160, and severe non-lone reinforcement remains +60. The scan
still counts every owned power-bearing card, stays destination-blind, uses strict > thresholds
and first-best ties, and preserves partial facts after exceptions. V60, V137, routing,
finalization, and physical-card resolution are untouched. Production edits are AI-only.
Verification passed 172 focused MOVE tests and the full 1,349-test reactor with 0 failures, 0
errors, and 26 skipped. Package, source-boundary, forbidden-symbol, artifact, mirror, exact-diff,
and source-review gates passed; server jar SHA-256 is
bc3c1ed9ae014c17c42e53a6a7e3c9be4844da4945cc68822a1f8577c43cdbff and packaged web.jar SHA-256
is 19fa725adc9d434f1b89b558c6659a6c5efeec9b6bc109e64c53ab95025f5217. Runtime load and live-game
proof remain separate gates. Revert the single V224 commit. See AI_CHANGELOG 2026-07-19.

  ==== V223 AI-ONLY MOVE HUNT-TARGET POLICY OWNER (2026-07-19, both bots) ====
Shared MoveHuntTargetPolicy now owns the duplicated V29.12/V35 armed Dark Jedi target search:
Vader/Tyranus/Dooku title fallbacks, fail-open Dark Jedi classification, current opponent power,
attached-weapon detection, strongest generic and Jedi target scans, strict ties, partial-result
exceptions, exact reasons, and raw contributions. Both adapters retain objective gating, direct
score application, logs, R2 ladder claims, and the original position before V137. Behavior is
unchanged: an armed uncontested Hunt Down hunter receives +350 for any Jedi/Padawan target or
+200 for the strongest generic opponent location; Jedi preference still overrides a stronger
generic target. Strict > ties keep the first location, and a failed target read preserves earlier
results while stopping later scans. The rule remains destination-blind; V137 still owns the
independent unwinnable-move veto. V60, routing, finalization, and physical-card resolution are
untouched. Production edits are AI-only. Verification passed 154 focused MOVE tests and the full
1,331-test reactor with 0 failures, 0 errors, and 26 skipped. Package, source-boundary,
forbidden-symbol, artifact, mirror, exact-diff, and behavioral source-review gates passed; server
jar SHA-256 is b8ede4b1c35e0d40bbc764c9f55df470508b9c398c0d3e72d887aefc4222d76c.
Runtime load and live-game proof remain separate gates. Revert the single V223 commit. See
AI_CHANGELOG 2026-07-19.

  ==== V222 AI-ONLY MOVE HUNT-DOWN COHESION POLICY OWNER (2026-07-18, both bots) ====
Shared MoveHuntGroupPolicy now owns the duplicated V29.13 Hunt Down group-movement analysis:
title and Dark Jedi classification, strongest-ally scan, first textual opponent destination,
first in-play hunter anchor, branch selection, exact reasons, and raw contributions. Both bot
adapters retain objective gating, direct score application, logs, outer catch, R2 ladder claims,
and the original phase position between V137 and V22.5. Behavior is unchanged: hunter toward
allies remains +200 or +250 at ally power >= 8; hunter away remains -200 unless the first textual
destination has opponent power; non-hunter away/toward/elsewhere remains -250/+250/-100. Strict
best-location ties, first-anchor stopping, title fallbacks, object identity, null-power-as-zero,
first-match breaks, and fail-open classifier/power reads remain intact. V137, V60, routing,
finalization, and physical-card resolution are untouched. Production edits are AI-only.
Verification passed 138 focused MOVE tests and the full 1,315-test reactor with 0 failures, 0
errors, and 26 skipped. Package, source-boundary, forbidden-symbol, artifact, mirror, diff, and
source-review gates passed; server jar SHA-256 is
471b5ef5e1ff7035d67aa0359de87cca590d0799f07432bb5cdf0bc7961a089e. Runtime load and live-game
proof remain separate gates. Revert the single V222 commit. See AI_CHANGELOG 2026-07-18.

  ==== V221 AI-ONLY MOVE DESTINATION AND LANDED-SHIP POLICY OWNER (2026-07-18, both bots) ====
Shared MoveDestinationPolicy now owns the duplicated V91 landed-ship identity scan and the
V34/V36/V111/V38.3 destination-contest analysis. Both adapters keep direct score application,
logs, outer catches, R2/R3 ladder claims, deferred and hard veto mutation, and original call order.
Behavior is unchanged: V91 remains +800 for takeoff and +600 for disembark when a friendly
starship resolves by object identity to the current non-system site; V34 remains +250, plus +150
for an uncontested destination, +100 for an attached weapon, and +150 for Vader moving toward a
Jedi or Padawan; V111 remains +400 for a non-battleground to empty-battleground advance while
opponents are uncontested elsewhere; and V38.3 preserves its strongest-location strict comparison,
wrong-direction fact, and independent Castle hard-veto fact. First textual destination, partial
exception, fail-open, and coexistence semantics remain intact. V60, V137, source/weapon scans,
routing, finalization, and physical-card resolution are untouched. Production edits are AI-only.
Verification passed 117 focused MOVE tests and the full 1,294-test reactor with 0 failures, 0
errors, and 26 skipped. Package, execution-order, source-boundary, forbidden-symbol, artifact,
mirror, diff, and source-review gates passed; server jar SHA-256 is
3aade354cbb11e23eac32f39d1821bd92d6f7aba0076231eb4b8b27233c8717a. Runtime load and live-game
proof remain separate gates. Revert the single V221 commit. See AI_CHANGELOG 2026-07-18.

  ==== V220 AI-ONLY MOVE DRAIN-ROUTING POLICY OWNER (2026-07-18, both bots) ====
Shared MoveDrainRoutingPolicy now owns the duplicated V85 drain-retention analysis, V29.13 explicit
destination drain weighting, and V73 Cantina/Mos Eisley shuttle exception. Their three distinct scans
remain distinct: V85 locations-in-order plus adjacency, V29.13 first textual locations-in-order match
without adjacency, and V73 first textual top-location match. Both adapters retain score application,
logs, catches, ladder claims, and call order. Behavior is unchanged: V85 remains -800 for a strictly
lower best adjacent drain; V29.13 remains -40 per drain lost with an extra -80 at drain zero or +40 per
drain gained; V73 remains +400 when another owned character stays, with no new control, adjacency, or
drain checks. The shuttle boundary remains +5560. V34/V36/V60/V91/V111/V38.3, routing, finalization,
and physical-card resolution are unchanged. Production edits are AI-only. Verification passed 97
focused MOVE tests and the full 1,274-test reactor with 0 failures, 0 errors, and 26 skipped. Package,
source-boundary, forbidden-symbol, artifact, mirror, diff, and independent gates passed; server jar
SHA-256 is 6bc4cb167b756b32159671985069af2700b5346f988fac7210c39067a9929704. Runtime load and live-game
proof remain separate gates. Revert the single V220 commit. See AI_CHANGELOG 2026-07-18.

  ==== V219 AI-ONLY MOVE THREAT POLICY OWNER (2026-07-18, both bots) ====
Shared MoveThreatPolicy now owns the duplicated five-tier source-location threat classification,
exact reason strings, raw deltas, and the RETREAT survival-claim fact. Both bot adapters retain the
printed-power and weapon scans, direct score application, original logs, R3 ladder mutation, and
source order. Behavior is unchanged: +8 remains CRUSH -1500, +4 FAVORABLE -1500, -4 RISKY -500,
-6 DANGEROUS +20, and below -6 RETREAT +150 with R3. The opponent-power > 0 gate, float and NaN
comparison behavior, integer truncation, V47, V60, V169, routing, finalization, and physical-card
resolution are unchanged. Production edits are AI-only. Verification passed 77 focused MOVE tests
and the full 1,254-test reactor with 0 failures, 0 errors, and 26 skipped. Package, source-boundary,
forbidden-symbol, artifact, mirror, diff, and independent gates passed; server jar SHA-256 is
282db1335fbd276af4d010db5724fbb3664fd86c36407327db4fa85581b04b8c. Runtime load and live-game
proof remain separate gates. Revert the single V219 commit. See AI_CHANGELOG 2026-07-18.

  ==== V218 AI-ONLY MOVE PILOT AND TRANSIT POLICY OWNER (2026-07-18, both bots) ====
Shared MoveTransitPolicy now owns the duplicated V25 pilot-lock score, defensive-shuttle scan, docking-bay
transit bonus, and takeoff bonus. Both adapters keep direct score application and original logging at the
same positions. Behavior is unchanged: pilot lock remains additive -500 rather than a ladder veto;
defensive shuttle remains +20 only at the first location title found in action text when friendly printed
power is above zero and opponent printed power is at least 2x; no adjacency or exact destination is proven;
docking-bay transit remains +15; and takeoff remains +10. Null power, title fallbacks, action keywords,
operation order, ladder state, routing, retry state, and physical-card resolution are unchanged. Production
edits are AI-only. Verification passed 62 focused MOVE tests and the full 1,239-test reactor with 0
failures, 0 errors, and 26 skipped. Package, source-boundary, forbidden-symbol, artifact, mirror, diff, and
independent gates passed; server jar SHA-256 is
4197e8ae4b64e5275433e9c590aca1fbd4a84d1e5985bb6d4b20f5778aa092d2. Runtime load and live-game proof
remain separate gates. Revert the single V218 commit. See AI_CHANGELOG 2026-07-18.

  ==== V217 AI-ONLY MOVE LANDING SAFETY POLICY OWNER (2026-07-18, both bots) ====
Shared MoveLandingPolicy now owns the duplicated V49/V67f1 landing classification, actual-passenger
scan, name fallbacks, route selection, exact score deltas, and reasons. Both bot adapters keep the
existing contains("land") caller gate, ladder hard-veto mutation, logging order, and additive position.
Behavior is unchanged: no-passenger starships still reach the -100000 ladder veto; only blueprint
capital/transport cards receive the passenger scan; name-fallback ships remain vetoed without one;
crewed ships and generic ground landings remain +10; the dormant starfighter -100 arm stays behind the
earlier veto; and the broad caller still gives landspeed the generic landing +10. Passenger-scan
exceptions remain fail-open as zero passengers. Production edits are AI-only. Verification passed 48
focused MOVE tests and the full 1,225-test reactor with 0 failures, 0 errors, and 26 skipped. Package,
source-boundary, forbidden-symbol, artifact, mirror, diff, and independent gates passed; server jar
SHA-256 is a022b5c2c556deb247dac900bbafc7158eadef6b3a323db1e2e595945aa86ae4. Runtime load and live-game
proof remain separate gates. Revert the single V217 commit. See AI_CHANGELOG 2026-07-18.

  ==== V216 AI-ONLY MOVE OPPORTUNITY ANALYSIS OWNER (2026-07-18, both bots) ====
Shared MoveOpportunityPolicy now owns the duplicated ATTACK and SPREAD scans, printed-power math,
icon scoring, strict first-location tie behavior, undercover handling, reasons, and result types.
Both adapters keep the original source gates, direct score application, logging, ATTACK adjacency check,
R2 ladder claims, and additive positions. This is structural only: the destination-blind all-location
scan remains; ATTACK still excludes undercover cards while SPREAD counts them; caller-owned +15 weak
attack and -10 failed-spread scores remain unchanged. V29.7, V29.13, the MOVE ladder, routing,
objectives, physical-card resolution, and all engine source are unchanged. Verification passed 34
focused tests and the full 1,211-test reactor with 0 failures, 0 errors, and 26 skipped. Package and
independent gates passed; server jar SHA-256 is
1c26c8c591ebf2abfbd4e88573bdae1957b87d42952bf46f26c365bb71fd7f3f. Runtime load and live-game
proof remain separate gates. Revert the single V216 commit. See AI_CHANGELOG 2026-07-18.

  ==== V215 AI-ONLY MOVE FORCE-ECONOMY POLICY OWNER (2026-07-18, both bots) ====
Shared MoveForceEconomyPolicy now owns the duplicated V29 move-reserve and V27 move-maintenance score
bodies. Both bot adapters keep the original Force-pile reads, shared DTF/grabber/maintenance facts,
critical-interrupt scan, logging, catches, and exact additive positions. V29 remains -100, -150 with a
critical interrupt, or -60 at the mild boundary; V27 remains -80 at maintenance cost plus one or lower.
No MOVE ladder, routing, objective, physical-card resolution, movement helper, or engine source changed.
Verification passed 14 focused tests and the full 1,191-test affected reactor with 0 failures, 0 errors,
and 26 skipped. Package and source-boundary gates passed; server jar SHA-256 is
978f51d1cb00f4f77a5c62cc8fcafc256e58c985023964add000c25636e90cb0. Runtime load and live-game
proof remain separate gates. Revert the single V215 commit. See AI_CHANGELOG 2026-07-18.

  ==== V214 AI-ONLY DEPLOY OBJECTIVE-PROGRESS FACTS (2026-07-18, both bots) ====

Shared ObjectiveAnalyzer now assesses one exact physical deploy child against the active physical objective and
returns the typed ObjectiveProgressAssessment facts record. Both bots call the shared method only from the real
destination-selection route. Parent DeployEvaluator and ActionTextEvaluator remain unwired because text cannot
prove the future child. Endor Operations is the first source-verified pilot: a
missing named flip card advances the requirement set, and the final missing named card completes the modeled flip
requirements.

This phase is shadow-only. It adds no score, changes no action order, and does not connect the test-only pending
intent store. Duplicate physical copies, ambiguous destinations, stale analyzer state, post-flip protection, and
unmodeled objective families fail closed as UNPROVEN. Existing V22/V88/V99/V136/V193 score owners remain unchanged.
All production edits stay under the AI package; no engine metadata or game-engine source changed. Verification passed
58 focused tests and the full 1,181-test affected reactor with 0 failures, 0 errors, and 26 skipped. Package and source
boundary gates passed; server jar SHA-256 is 6bf9f58cb15d9331315cdde86748f352fbf9ce50c1601e231967c3fa789423f8.
Runtime load and live-game proof remain separate gates. Revert the single V214 commit. See AI_CHANGELOG 2026-07-18.

  ==== V213 AI-ONLY DEPLOY-3 WEAPON, PILOT, AND SHIP POLICY OWNERS (2026-07-18, both bots) ====
Shared DeployWeaponPolicy now owns the existing V158 direct/reserve attachment gates, V33 named-weapon priority,
and V120 reserve-pull criteria gate. Shared DeployPilotShipPolicy owns V30 matching pairs and generic crew rules,
V35.5/V35.6 ship safety, V121 objective-pilot destination scoring, and the existing V23/V24.6/V24.9/V24.10/
V40.1/V47 asset-tail contributions. Both bots retain stock identity, matching filters, board/hand/objective/oracle reads,
source routing, exception boundaries, and logging. Scores, additive order, outer predicates, catches, and early exits are
unchanged; historical -9999/-1500 arms remain additive rather than becoming structural vetoes. V185 remains exclusively
PULL-owned, while V212 siting and V211 Force budgeting stay with their existing owners. Reserve-child objective progress
and future-phase planning remain follow-on work. Production edits are AI-only. Gate: 30 focused tests; full affected
reactor 1,172 tests, 0 failures, 0 errors, 26 skipped; package, three-evaluator mirror, PULL ownership, AI boundary,
forbidden-symbol, artifact, and diff checks pass. Packaged web.jar SHA-256:
c616d6ec71505e02013f8c55e4bb3b84dc2a1ce6086893ea17b2d36cbaed1622. Runtime JVM loading and live-game proof remain
separate gates. Revert the single V213 commit. See AI_CHANGELOG 2026-07-18.

  ==== V212 AI-ONLY DEPLOY-2 SITING POLICY OWNERS (2026-07-18, both bots) ====
Shared DeploySitingPolicy, DeployTacticalPolicy, DeployObjectiveSitingPolicy, and DeployFormationSitingPolicy now
own the existing destination-safety, tactical-character, objective-location, and formation-topology contribution
streams formerly duplicated across both DeployEvaluator and CardSelectionEvaluator mirrors. CharacterDeploySiteEvaluator
remains the thin V136 board adapter. Both bots retain stock identity, target resolution, objective analysis, board reads,
and logging. V89/V136/V193/V96, V166/V169/V170/V171/V172, V22/V88/V99, and live V67bn/V29.5/V113 preserve their
prior values, contribution order, route asymmetry, and failure boundaries. V89 remains additive, and formation UNKNOWN
remains score-neutral. Retired V122 and V67as source blocks were removed; nested V67br/V75/V67bj remain retired.
Production edits are AI-only. Reserve-child objective-progress and future-force planning changes remain outside this
score-neutral extraction. Gate: 54 focused tests; full affected reactor 1,155 tests, 0 failures, 0 errors, 26 skipped;
package, mirror source tests, AI-boundary, forbidden-symbol, compiled-artifact, and diff checks pass. Packaged web.jar
SHA-256: 6955844df38fa6008bbc754ed40ec6a9f4d61c42038e711b9f3e522750c652a1. Runtime JVM loading and live-game proof remain
separate deployment gates. Revert the single V212 commit. See AI_CHANGELOG 2026-07-18.

  ==== V211 AI-ONLY DEPLOY-1 SEQUENCING AND FORCE-BUDGET POLICY OWNERS (2026-07-18, both bots) ====
Shared DeploySequencingPolicy, DeployBudgetPolicy, and DeployPlanPolicy now own the existing ordered DEPLOY-1
streams for phase urgency, current-plan application, Force reservations, location-first ordering, and opening scripts.
Shared immutable facts and a stock-state reader replace duplicated endangered-location, winnable-battle, and Anakin's
Funeral Pyre scans. Both bots retain thin adapters, their planner/script state, stock identity, objective analyzer,
and logging. V38.4/V56, V169, V176, plan membership/terminal branches, V59/V64/V24.5, affordability, independent
V48/V67z/V79/V29.13 obligations, V162/V67ai, V52/V54/V55/V52b, V53c, and V24.4 preserve their prior values,
additive order, and early continues. V196 identity, V201 routing, V207 objective scoring, V209 PULL, destinations,
character tactics, pilots, ships, and weapons remain outside this phase. Production edits are AI-only; no engine
metadata or second selector was added. Gate: 31 focused tests; full affected reactor 1,114 tests, 0 failures,
0 errors, 26 skipped; package, mirror, AI-boundary, forbidden-symbol, and diff checks pass. Independent review also
pins the separate V169/V176 exception boundaries, turn-3 Funeral Pyre guard/fail-open path, and empty title fallback.
Packaged web.jar SHA-256: 3c53fe81c6bb545653b254ae05d43b0909931d4089d8da0af16159584eac7578.
Docker was unavailable, so JVM-loaded and live-game proof remain pending. Revert the single V211 commit. See
AI_CHANGELOG 2026-07-18.

  ==== V210 AI-ONLY ACTIVATE ACTION POLICY OWNER (2026-07-18, both bots) ====
Shared ActivateActionPolicy now owns the remaining mirrored ACTIVATE action arms: V168 +5000 versus V61c -6000,
the V38.3/V61c zero-activation confirmation pair at +/-9999, and the independent exact-activate V38.3 +500.
Stock action/label recognition, local logging, Pass behavior, and the existing conservative battle-plausibility
fact remain in the bot adapters. That same fact still feeds all three required sites: action choice, the existing
V195 ActivateAmountPolicy path, and zero-confirmation. Unknown game facts therefore remain battle-plausible and keep
the three-card buffer. Normal exact activation remains +5000 +500; buffer-protected activation remains -6000 +500.
V197's amount latch, exact opponent-allowance prompt, generic INTEGER rejection, V57/V67at/V43 arithmetic, bounds,
and engine Yes/No labels are unchanged. The obsolete commented pre-2026 V61c fallback and duplicate helper were
removed. Separate top-level and confirmation ledgers pin malformed combined shapes against operation replay.
Production edits are AI-only. Gate: 31 focused tests; full affected reactor 1,083 tests, 0 failures, 0 errors,
26 skipped; package, mirror, source-boundary, forbidden-symbol, artifact, and diff checks pass. Packaged jar SHA-256:
af4e117667176c90f7dce39fe295d97aeec445f9072fe4acd7abbaf518129709.
Revert the single V210 commit. See AI_CHANGELOG 2026-07-18.

  ==== V209 AI-ONLY PULL PHASE POLICY OWNERS (2026-07-18, both bots) ====
Shared PullActionPolicy now owns the existing parent action-text stream, PullDeployPolicy owns the V60/V66/V67h/
V185/V190 deploy-search stream, PullTakeCandidatePolicy owns take-card candidates, and PullDeployCandidatePolicy
owns V70 Reserve-deploy candidates. Both bots retain thin stock-decision, identity, board, objective, and DeckOracle
adapters. CombinedEvaluator remains the only selector in its original Deploy, CardSelection, ActionText order;
first-seen ties, selectable filtering, stable descending candidate sort, populated-id versus blueprint-only child
routing, additive duplicate guards, V66 WASTEFUL fallthrough/WILL_FAIL stop, V192 local suppression, V67ak outside
the clamp, and parent-versus-destination formation ownership are unchanged. Missing or unanalyzed oracle facts remain
unknown, source validation requires an analyzed oracle, missing game state records Reserve as -1, and staged fact
reads preserve the legacy short-circuit sequence before late hand/battle queries. V194 failed-search memory remains
disabled. Production edits are AI-only. Gate: 45 focused tests; full affected reactor 1,075 tests, 0 failures,
0 errors, 26 skipped; package, mirror, source-boundary, forbidden-symbol, artifact, and diff checks pass. Packaged jar
SHA-256: 8272c2c0a0868ad4b833682354e7c28c0429a96db342efeaf758a6501935fe14.
Revert the single V209 commit. See AI_CHANGELOG 2026-07-18.

  ==== V208 AI-ONLY BATTLE PHASE POLICY OWNERS (2026-07-18, both bots) ====
Shared BattleDecisionPolicy now owns the former 928-line BattleEvaluator score stream; both bots retain thin stock
context/predictor adapters, and V198 still runs exactly once per candidate. The independent V25 ActionText initiation
ladder remains in place and still adds to the BATTLE-1 result. Shared BattleWeaponsPolicy owns the existing Force Push,
fire-before-throw, redraw, generic fire/cancel/draw, V51 already-hit, V36 targeting, and final V38.3 self-target arms;
other card-specific battle interrupts remain in their mirrored adapters. Shared BattleForfeitFacts and
BattleForfeitPolicy own optional V22.4/V29.13 plus V154, V118, V150, V22.3, and the full V159/V161/V178 ladder.
The combined prompt keeps V154/V118 before routing, V206 FORCE-LOSS at the middle seam, and V150/V22.3 or V159
afterward. The two obsolete v159ForfeitScore copies are deleted. Standalone mandatory-forfeit nudges and V45 remain
with their existing owners. Scores, predicates, reasons, candidate order, Pass legality, and early control flow are
unchanged. Production edits are AI-only. Gate: 47 focused tests; full affected reactor 1,052 tests, 0 failures,
0 errors, 26 skipped; compile, package, mirror, source-boundary, forbidden-symbol, score-owner, and diff checks pass.
Revert the single V208 commit. See AI_CHANGELOG 2026-07-18.

  ==== V207 AI-ONLY SHARED OBJECTIVE/PLAYBOOK ANALYZER (2026-07-18, both bots) ====
The two 1,936-line bot-local ObjectiveAnalyzer copies now have one exact shared implementation under
ai/models/common/strategy and two 12-line no-argument compatibility facades. Each facade supplies its existing
strategy logger and still creates a separate mutable analyzer instance. Public methods, inherited static nested
playbook types, constants, consumers, score notes, call positions, early returns, profiles, and reason strings remain
unchanged. The two behavior-identical lazy JSON caches become one logically immutable shared cache; its contract
remains 58 profiles, 15 enabled, 43 disabled, with blueprint-first then title-fragment lookup. ObjectiveHandler,
ObjectiveProgressAssessment, disabled profiles, setup/pull hydration, and every phase-local scorer remain untouched.
Repository Java-source consumers remain compatible through inheritance, including six fully qualified bot-facade
nested-type references in the two DeployEvaluator and CardSelectionEvaluator copies. The nested types now have the
shared class as their binary owner, and the cache list is not structurally wrapped; repository scans found no
reflection, serialization, precompiled external, or cache-mutation consumer. Those accepted boundaries are
documented rather than hidden.
Production edits are AI-only. Eight focused tests and the 1,023-test affected reactor passed with zero failures
or errors (26 skipped). Package, normalized source-equivalence, inherited source API, separate-state, profile-contract,
source-boundary, forbidden-symbol, artifact, and diff gates passed. Revert the single V207 commit.
See AI_CHANGELOG 2026-07-18.

  ==== V206 AI-ONLY FORCE-LOSS PAYMENT POLICY OWNER (2026-07-18, both bots) ====
Shared ForceLossFacts now owns the immutable decision/candidate reads and shared ForceLossPolicy owns the exact
ordered standalone and combined-battle payment streams for V109, V153, V175a, V178-loss, V28-DTF, V21, and V25.
The common V153 healthy/survival table, duplicate override, thin-Reserve -335, hand floor -700, priority -100,
thresholds, order, and reasons are unchanged. Deliberate route differences remain: V109/V175/V178/V28 and
hand-only objective/V25 -500 are standalone-only; combined battle keeps all-zone objective protection and V25 -400.
V154/V118 remain before the shared policy; V150/V22.3, the forfeit branch, and v159ForfeitScore remain after it in
BATTLE-3. Lost-pile alias routing remains; unknown loss is untouched. The registry's stale byte-identical claim and
V175 weapon mislabel is corrected. Production edits are AI-only. Gate: 28 focused tests; full affected reactor
1,017 tests, 0 failures, 0 errors, 26 skipped; package, mirror, source-boundary, forbidden-symbol, artifact, and diff
checks pass. Revert the single V206 commit.
See AI_CHANGELOG 2026-07-18.

  ==== V205 AI-ONLY SHIELDS PHASE POLICY OWNER (2026-07-18, both bots) ====
One shared ShieldStrategy now owns the unchanged shield tables, state, pacing, activation counts, and base score.
ShieldFacts owns the existing board reads, while ShieldPolicy owns the typed V124/V102/V29.1 parent stream,
defensive response window, V112/V117 mixed-menu gates, V105/V107 fourth-slot selection, and V51 Battle
Order/Plan adjustments. Both bots keep thin stock-decision adapters and apply the same ordered operations through
PolicyContributionLedger. Score deltas, order, routing, early returns, title matching, menu checks, A then C then B
fourth-slot priority, additive VETO labels, Pass behavior, and existing reasons are unchanged. The live turn-0 pacing
result, unconsulted minTurnToPlay/playIfWeHave fields, indiscriminate activation tracking, reserve scoreShield
cross-talk, V124 menu ignorance, V105/V117 menu dependency, and unknown Simple Tricks +50 fallback are pinned
bug-for-bug, not corrected. Resistance, Ultimatum, Knowledge And Defense, and Anger, Fear, Aggression aliases were
verified against actual Java card sources. Production edits are AI-only. Gate: 29 focused tests; full affected
reactor 996 tests, 0 failures, 0 errors, 26 skipped; package, mirror, source-boundary, forbidden-symbol, and diff
checks pass. Revert the single V205 commit.
See AI_CHANGELOG 2026-07-18.

  ==== V204 AI-ONLY CONTROL PHASE POLICY OWNER (2026-07-18, both bots) ====
The shared CONTROL drain assessment now emits the ordered typed policy stream directly, and a small shared
ControlActionPolicy owns the existing V29.14 No Escape retrieval, V24.2 optional +1 drain, and V52 self-cancel
contributions that still lived in both ActionTextEvaluator mirrors. Stock action recognition and response positions
remain in each bot adapter. Score deltas, raw float order, reasoning, logger calls, early returns, V104 suppression,
lazy query order, candidate order, and Pass behavior are unchanged. Registry VETO labels remain additive historical
scores, not structural hard vetoes. PULL, SHIELDS, BATTLE, playbook, generic retrieval, and maintenance logic remain
with their later owners. Production edits are AI-only and contain no engine decision metadata. Gate: 24 focused
CONTROL/policy/ledger/parity tests; full affected reactor 974 tests, 0 failures, 0 errors, 26 skipped; package,
mirror, source-boundary, forbidden-symbol, and diff checks pass.
Revert the single V204 commit.
See AI_CHANGELOG 2026-07-18.

  ==== V203 AI-ONLY DRAW PHASE POLICY OWNER (2026-07-18, both bots) ====
The two 813-line mirrored DRAW evaluators now use one shared AI-only DrawPhasePolicy for the exact ordered score
stream and one DrawPhaseFactsReader for duplicated force-generation, V182 offensive-bank, expensive-card, and
force-starved reads. Each bot retains stock recognition, bot-specific fact/oracle adaptation, the existing reserve
reader, and typed operation application. Candidate filtering, raw float deltas, reasoning and logger order, early
returns, reserve arithmetic, Pass behavior, and candidate order are preserved. V182 remains additive -300 plus an
early return, not a hard veto. CombinedEvaluator remains the only selector. Production changes are confined to
gemp-swccg-server AI source; no engine or decision metadata changed. Gate: 61 focused tests; full affected reactor
967 tests, 0 failures, 0 errors, 26 skipped; package, mirror, forbidden-symbol, source-boundary, and diff checks pass.
Revert the single V203 commit.
See AI_CHANGELOG 2026-07-18.

  ==== V202 AI-ONLY SELECTION FOUNDATION (2026-07-18, both bots, shadow only) ====
Phase extraction now has a shared typed operation stream before any live rule arm moves. PolicyOperation and
PolicyResult preserve action, rule-arm, domain, manifest kind, operation kind, raw float delta, reason, and order;
PolicyContributionLedger requires one exact decision/action/rule contribution and rejects repeats even from the same
producer. Mirrored adapters require that validated ledger and apply
ADD, HARD_VETO, and DEFER through the existing EvaluatedAction choke points. ObjectiveProgressAssessment and
TurnResourcePlan are facts-only. PendingAiIntent stores exact action or physical-card constraints only inside AI
memory and clears on terminal, expiry, missing, ambiguous, or consumed-child paths. It stores no score, blueprint-only
identity, engine object, or decision parameter. The existing DecisionSnapshot remains the immutable stock-decision
snapshot and CombinedEvaluator is unchanged. Nothing is wired to a live policy route in V202. Gate: 110 focused
foundation/selection/trace tests and the full affected reactor at 951 tests, 0 failures, 0 errors, 26 skipped; mirror, source-boundary,
forbidden-symbol, and diff checks pass. Production changes are AI-only. Revert the single V202 commit.
See AI_CHANGELOG 2026-07-18.

  ==== V201 AI-ONLY UNSUPPORTED-SOLO DEFER TIER (2026-07-15, both bots) ====
The L3 weak-solo rule's additive -800 could be revived by existing destination and forced-pull bonus stacks. V201
adds an OR-merged DEFER marker and makes both normal and ordered-bucket selection compare constraint class before
score. Any admissible action or legal Pass beats DEFER; mandatory prompts choose admissible, then the best deferred,
then the least-bad hard block. A companion now requires another exact physical character in the detached deployment
plan, still in hand, assigned to the same destination, with enough printed plan budget after the first deploy.
Duplicate/unknown blueprint identity, mere hand presence, forced Reserve pulls, and speculative movement do not claim
a plan. Existing undercover, objective flip-gate, friendly-support, ability-4, and dominance allowances remain.
The retained -800 ranks only mandatory all-deferred fallbacks. This is deliberately narrower than the full frozen
deploy-weight contract: engine-backed sequence legality, movement, retreat obligations, and V169/contact/overpower
owner migration remain later AI-only work. Gate: 65 focused tests; full affected reactor 928 tests, 0 failures,
0 errors, 26 skipped; mirror, forbidden-metadata, production-boundary, and diff checks pass. Revert the single V201
commit. See AI_CHANGELOG 2026-07-15.

  ════ V200 AI-ONLY OBJECTIVE FRONT/BACK NORMALIZATION (2026-07-15, both bots) ════
ObjectiveAnalyzer previously analyzed whichever blueprint side was currently face up and searched that text for a
literal [Back Side] marker that no objective blueprint contains. Shared AI-only ObjectiveSideBlueprints now derives
stable front/back identity from the stock getBlueprint, getOtherSideBlueprint, and isFlipped APIs. Both bots always
use the front title/text and send the real back text through the existing flip-back parser. Single-sided cards keep
front analysis. No score or threshold changed, but real back-side classification can now select existing protection,
deploy, and move branches; the boundary audit recorded a theoretical +1080 deploy swing and the already-existing
V47 -100000 veto when its survivability and location gates pass. Gate: 6 focused tests plus the full affected reactor,
908 tests, 0 failures, 0 errors, 26 skipped. Production changes are AI-only. Revert the single V200 commit.
See AI_CHANGELOG 2026-07-15.

  ════ V199 AI-ONLY MOVE PHYSICAL-MOVER CONSOLIDATION (2026-07-15, both bots) ════
V169 retreat, V156 join-group, and Formation Safety had three different loops for recovering a physical mover from
the stock blueprint hint. An earlier off-table duplicate could abort V156 or feed Formation Safety missing origin,
undercover, and weapon facts, allowing a false hard veto. Shared AI-only MovePhysicalCardResolver now returns the
first owned, matching, on-table physical card plus its origin in existing stock iteration order. All three consumers
use it in both bots. No scores, thresholds, candidate order, Pass/Done behavior, or engine decision data changed.
Gate: 10 focused resolver/formation tests; full affected reactor 902 tests, 0 failures, 0 errors, 26 skipped; mirrored
CardSelectionEvaluator streams normalize identically. Production changes are AI-only. Revert the single V199 commit.
See AI_CHANGELOG 2026-07-15.

  ════ V198 AI-ONLY BATTLE TARGET AND CARD-FACT CORRECTION (2026-07-15, both bots) ════
Stock CardActionSelectionDecision already carries the selected battle location's cardId at the same ordinal as each
action, but BattleEvaluator and V25 ignored it and parsed display text. Generic "Initiate battle" therefore lost target
identity and allowed V22.4/V25/V61/Formation/V76 facts from another site to influence the candidate. A shared AI-only
BattleTargetResolver now resolves the aligned stock cardId first, with named text retained only as a legacy fallback.
V61 uses the selected target's margin when known. Weapon facts now use getPermanentWeapon plus active attachments;
Vader/Inquisitor/Hatred use stock filters, so disarmed permanent weapons, another character's weapon, IHYN alone,
arbitrary stacks, and opponent-stacked cards cannot produce their old false positives. All score magnitudes and thresholds
are unchanged. Focused gate: 11 resolver/weapon/parity tests. Full affected reactor: 898 tests, 0 failures,
0 errors, 26 skipped; bot streams normalize identically.
Production changes are AI-only. Revert the single V198 commit. See AI_CHANGELOG 2026-07-15.

  ════ V197 AI-ONLY ACTIVATE AMOUNT CORRELATION AND ZERO-CONFIRM LABELS (2026-07-15, both bots) ════
The activation amount evaluator no longer claims every INTEGER decision. Selecting the exact top-level Activate Force
action arms one game/player/turn/phase-scoped latch, and only the immediate matching INTEGER consumes it. Drift or an
intervening decision clears the latch and fails closed; unrelated value decisions continue through the inherited
heuristic route. The unchanged engine's exact opponent-allowance prompt remains directly recognized. The exact
zero-activation confirmation maps its unique Yes and No labels to ordinal action ids, preserving V38.3 and V61c when
the engine reorders labels. Ambiguous and unrelated choices remain unclaimed. Seam traces use those same labels for
ordinal candidate order. V57, V61c, V67at, V43, V38.3, and every amount-policy magnitude remain unchanged. Production
changes remain inside the AI package. Gate: 39 focused tests plus the full affected reactor totaled 891 tests with 0
failures and 0 errors (26 skipped); mirrored bot changes normalize identically and diff check is clean. Revert the
single V197 commit. See AI_CHANGELOG 2026-07-15.

  ════ V196 AI-ONLY DEPLOY PHYSICAL IDENTITY AND COPY ISOLATION (2026-07-15, both bots) ════
Deployment plans now distinguish permanent and current physical card ids instead of allowing duplicate blueprint
copies to share the first matching instruction. Every planner-created instruction records exact identity; action
matching, hand-departure reconciliation, scoring, and deployment recording consume it. Blueprint-only lookup now
requires one unambiguous match, while one unique legacy instruction remains compatible. Unknown exact records do not
mutate the plan or deployment count. Evaluators receive a deep assessment copy after accepted reconciliation and
before stale-plan scoring flags, preventing score-time mutation of accepted planner state. Deployment weights,
formation policy, candidate order, response wires, and Pass scoring are unchanged. Production changes remain inside
the AI package. Gate: 5 focused tests plus the full affected reactor totaled 881 tests with 0 failures and 0 errors
(26 skipped); mirrored bot changes normalize identically and diff check is clean. Revert the single V196 commit.
See AI_CHANGELOG 2026-07-15.

  ════ V195 AI-ONLY ACTIVATE AND CONTROL CONSOLIDATION (2026-07-15, both bots) ════
After the engine-metadata rollback, Rando and Chosen One again carried mirrored copies of ACTIVATE amount arithmetic
and CONTROL drain scoring. V195 keeps the normal engine boundary and consolidates only those calculations inside the
AI package. ActivateAmountPolicy owns the unchanged V57, V61c, V67at, V43, and bounds arithmetic.
ControlDrainAssessment plus lazy ControlDrainFacts own the unchanged V24.15, V189, V25, Battle Order, V140, V104,
V52, V48, multi-drain, and Hunt Down operation order. Both bots delegate to the same implementations. Universal
legacy INTEGER routing, response wires, score magnitudes, terminal exits, candidate order, and fact-query order are
unchanged. No origin stamp, wire DTO, mediator callback, card action, game logic, or client source is restored. The
historical typed ACTIVATE and CONTROL owner below remains retired. Gate: 17 focused tests plus the full affected
reactor totaled 876 tests with 0 failures and 0 errors (26 skipped); mirrored evaluator streams normalize exactly,
the affected reactor packages, and diff check is clean. Revert the single V195 commit. See AI_CHANGELOG 2026-07-15.

  ════ V194 AI-ONLY RECOVERY AFTER ENGINE METADATA ROLLBACK (2026-07-14, both bots) ════
Steve's permanent boundary keeps the normal GEMP engine, card scripts, logic, mediator, and client unchanged. The
typed phase-owner work below remains historical because its engine decision metadata caused internal AI routing values
to appear as player choices, including Jyn Erso's undercover Yes/No prompt. V194 restores only behavior that compiles
and runs entirely under the AI package: shared DRAW reserve arithmetic and reads; engine-authoritative failed-search
handling for Rando/Chosen One; new-game objective/planner/deck state reset; physical ability-zero body presence for
DEPLOY formation safety; Force Push versus Stunning Leader and cancel-redraw branch separation in BATTLE; and an
INTEGER pass guard so optional value prompts do not silently choose zero. Beginner/Advanced failed-search memory is
unchanged. No score magnitude changes. Legacy evaluators remain runtime phase owners outside these narrow corrections.
No DecisionOrigin, DecisionActionSemantic, phase wire DTO, mediator lifecycle, card action, decision, or client source
is restored. The affected reactor passed 859 tests with 0 failures and 0 errors (26 skipped), then packaged
successfully. Source outside AI is byte-identical to ec886934b; the metadata scan is empty and diff check is clean.
Revert the single V194 commit. See AI_CHANGELOG 2026-07-14.

  ════ BATTLE PHASE OWNER CUTOVER (2026-07-14, Steve-approved, both bots) ════
Origin-stamped battle initiation, weapon fire, next-action, power, destiny-redraw, and optional-forfeit wires now
route through one typed owner before the V45 compatibility path. Each initiation candidate receives one immutable
BattleAssessment containing exact target identity, power/ability, permanent-weapon, formation, objective, same-turn
DEPLOY-intent, and exactly one predictor result. Typed UNKNOWN prediction cannot rerun the predictor. Objective battle
contributions migrate through ObjectiveBattleAdapter. Force Push no longer cross-triggers the Stunning Leader
exclusion penalty; legacy permanent-lightsaber +5 versus other permanent-weapon +3 weighting is preserved. V76 and
Force Push penalties remain additive, not hard vetoes. Rando/ChosenOne changed hunks normalize identically. Gate:
130 focused + 185 regression tests, zero failures/errors; package and compiled-marker checks pass; no game, browser,
VTS, sandbox, push, or deployment during verification. See AI_CHANGELOG and CODEX_BATTLE_PHASE_GATE_2026-07-14.md.

  ════ DEPLOY PHASE OWNER CUTOVER (2026-07-14, Steve-approved, both bots) ════
One origin-stamped DEPLOY owner now carries an exact physical transaction across parent, destination, buddy,
undercover, capacity, confirmation, forced auto-selection, and expected deployment completion. The immutable
transaction binds an opaque attempt id, diagnostic parent id and ordinal, current/permanent source identity,
ordered typed destinations, buddy candidates and selection, one formation assessment, and one unchanged
ForceObligationVector. State mutates only after engine acceptance. Permanent identity survives current-id rotation;
accepted placement covers attached, at-location, related, system, orbit, and converted-location relations. Exact
cancellation, rejection, identity/zone, game/phase/turn, and affordability failures terminate only that attempt.
Cancellation blocks only the exact opaque attempt while rejection leaves replay available. Character actions preflight legal
destinations without mutation, so a one-destination engine shortcut still receives parent safety. Zone deployment,
simultaneous buddy, ARBITRARY temp-wire mapping, and pre-send confirmation identity are typed. PULL keeps search,
target, and outcome ownership and no longer has its origin overwritten by DEPLOY; formation is deferred to its first
exact destination child and then bound once. ObjectiveDeployAdapter is the
exclusive live owner of V83/V88/V108/V110, objective-site +200, and V193 +400/+2000; predecessors and both prompt-
text V170 interceptors remain present but disabled. V99, V86/V121, formation, rescue, overpower, and Rando-only
V79b remain separate. Direct calls accept their selected parent/PULL-child once; mediator calls defer that same
transition to the engine callback. Optional all-veto keeps Pass, mandatory all-veto keeps the exact compatibility
wire, and ties keep insertion order. Physical body presence is tri-state, including ability-zero PRESENT and missing
facts UNKNOWN. One common forced-destination assessment covers the named first-pull exemption, exact unsupported
repeat and weak-solo penalties, UNKNOWN identity, and hard blocks. One ForceObligationVector is reused by parent,
child, Pass, and Move. Assessment copies deep-copy instructions, and actual destination drift terminates once. Direct
entry without a bound game returns an accepted assessment copy or unknown instead of dereferencing null. Named rescue,
Tyranus/direct-contact, safe solo/establish, drain-denial, and legal-overpower intents remain explicit. The complete
V170 direct/mediator/order/unknown/malformed matrix is pinned. Boundary: 285 tests passed across focused DEPLOY plus
mediator/bot/trace/PULL regressions, six repaired mirror streams remained identical on top of the original nine-pair
gate, server reactor package passed, and diff check is clean. Deploy-weight tuning remains a separate phase.
No game simulation, push, or deployment occurred during the gate. Revert the single phase commit. See AI_CHANGELOG
2026-07-14.

  ════ OBJECTIVE FACTS AND ADAPTER PHASE (2026-07-14, Steve-approved, both bots) ════
One immutable ObjectiveFacts view is now built before trace selection for every mediated decision and carried by
DecisionSnapshot v4. Trace, CombinedEvaluator, and every objective adapter consume that exact instance. Physical
identity uses current plus opposite blueprints and exact ids, so canonical front/back truth survives a flip while
current/opposite orientation swaps. Objective state resets on a new SwccgGame object reference, not opponent name.
Profiles resolve by blueprint id first, title compatibility second, then compiled My Lord/Endor fallback when no
usable JSON profile exists. Common DEPLOY, MOVE, BATTLE, PULL, and SETUP adapters translate typed facts without
general legality. DEPLOY/MOVE/BATTLE/SETUP remain shadow. DEPLOY covers the closed V83/V88/V108/V110, objective-site
+200, and V193 +400/+2000 set without absorbing V99/V86/V121 or formation policy. ObjectivePullAdapter is the sole
live owner of objective-source parent +1500, child-location +500, and canonical failed-verify intent; ARBITRARY temp
wire ids resolve by ordinal through typed physical-card identities. If immutable identity is unavailable or its ids
disagree but physical type proves the source is an objective, the adapter preserves the exact legacy parent score and the predecessor child route
preserves its exact legacy rank; blocked assessments never fall back. Replaced PULL emitters remain disabled in place for
later caller proof. Hidden Path, Corridor, Hunt, and starting-process inputs are typed. UNKNOWN stays unknown; facts
carry no score/rank/veto/weights. Boundary: 173/0/0/0 focused tests, affected reactor package success, diff clean, six
mirrored evaluator/analyzer files normalized identical. Independent audits caught and closed the temp-wire child seam
and identity-mismatch fallback gap before deployment. Revert the single phase commit. No game simulation and no push.
See AI_CHANGELOG 2026-07-14.

  ════ ACTIVATE + CONTROL PHASE B CUTOVER (2026-07-14, Steve-approved, both bots) ════
Six stamped ACTIVATE and CONTROL routes now have one shared typed owner after the existing chaos gate and before PULL
or legacy evaluation. Amount arithmetic is consolidated in ActivateAmountPolicy; CONTROL drain scoring is consolidated
in ControlDrainAssessment over a lazy collector returning immutable fact records. Existing score magnitudes, operation order, candidate order, and logs
remain pinned. ForceActivationEvaluator now requires ACTIVATE_AMOUNT or ACTIVATE_ALLOWANCE instead of claiming every
INTEGER. PassEvaluator also excludes INTEGER, closing the optional-min-zero regression K2 found in m00634; unstamped
gain amounts now reach the shared heuristic value picker instead of silently choosing zero. Normal zero activation
selects the exact No label, while the V61c keep-three state selects Yes. Malformed direct calls preserve the legacy raw
fallback and mediator calls return typed rejection. Accepted owned intents call ResponseFinalizer once with OUTER_COMMON;
pre-finalizer rejection calls it zero times. Replaced zero-confirm, amount, and drain branches were deleted. Boundary:
186/0/0/0 focused tests, affected reactor BUILD SUCCESS, diff clean, exactly two production consumers each for resolver,
amount policy, and drain policy, normalized mirrored bot streams, no universal INTEGER owner. Revert the single phase
commit. Local reload approved after the no-live-game gate; no simulation and no push. See AI_CHANGELOG 2026-07-14.

  ════ V44/V67j REVERT-APPROVAL FINALIZER PILOT (2026-07-13, Steve-approved, both bots) ════
The opponent-revert interceptor now has one shared RevertApprovalPhaseOwner for Rando and ChosenOne. Its shared
legacySelection preserves the exact old positive-label predicate and ordinal-zero fallback; direct decide() remains
wire-identical. The mediator-facing path captures one immutable snapshot, forwards the exact RejectionHistory, calls
ResponseFinalizer once with a no-RNG generator, and preserves the exact ordinal wire. Accepted results use explicit
typed-finalizer mutation mode NONE, so the route carries no tracker descriptor and applies no outer tracker or
strategic mutation. AiDecisionResult and FinalizedResponseAdapter retain the existing two-argument OUTER_COMMON
default while enforcing NONE carries no descriptor. Absent/empty results now produce typed ORDINAL_OUT_OF_BOUNDS
before engine submission instead of the legacy invalid wire "0"; this is the only intentional behavior correction.
The earlier runtime note saying ResponseFinalizer had no production caller is historical and now superseded by DRAW,
PULL, and this shared revert owner. Revert the single pilot commit to restore the direct block and old adapter
invariant. Phase boundary: 77/0/0/0 focused tests, diff clean, one shared owner implementation, normalized-identical
bot route blocks. The first run caught and fixed a fixture-side snapshot gap before commit. No game simulation and
no push. See AI_CHANGELOG 2026-07-13.

  ════ PULL PHASE CUTOVER (2026-07-13, Steve-approved, both bots) ════
Standard pull chains now carry one closed typed transaction across parent action, deploy/take child, destination,
and failed verification. The immutable evidence includes opaque transaction id, accepted parent id/ordinal, exact
source and selected-card current/permanent identity, GTA, source zone/owner, and ordered destination candidates.
ARBITRARY temp wire ids remain distinct from physical ids. Five prompt-text-free routes are owned: PULL_PARENT,
PULL_DEPLOY_CHILD, PULL_TAKE_CHILD, PULL_DESTINATION, and PULL_FAILED_VERIFY; incomplete/conflicting metadata stays
legacy with exact response parity. PullPhaseOwner invokes the existing CombinedEvaluator compatibility lane once
and ResponseFinalizer once; failed verify returns empty without evaluator scoring, and typed rejection never re-enters
fallback. Prompted destination candidates are never mislabeled forced; only the engine no-prompt auto-select records
forced evidence. Rando/ChosenOne disable inherited failed-search writes, penalties, and FAILED_SEARCH_ADD because the
engine CantSearchCardPileModifier is sole end-of-turn authority; Beginner/Advanced are unchanged, and the opponent
verification prompt stays legacy. Boundary: no legacy scorer, formation/objective guard, custom-search exception, or
shared predecessor body is deleted. DEPLOY later consumes PullDeployRef. Revert the single phase commit to restore
the pre-cutover owner; retained legacy routes remain available. Boundary: 178/0/0/0 focused tests, affected package
gate exit 0, diff clean, prompt-free routing, failed-memory ownership proof, and normalized-identical bot owners. The
first gate caught and fixed an ordinary-action query leak before commit. Immediate local reload authorized; no games
and no push. See AI_CHANGELOG 2026-07-13.

  ════ DRAW PHASE CUTOVER (2026-07-13, Steve-approved, both bots) ════
Canonical top-level Force-Pile draw actions now carry the closed engine semantic DRAW_CARD_INTO_HAND_FROM_FORCE_PILE;
CardActionSelectionDecision emits one aligned actionSemantic per offered action and UNKNOWN for unstamped actions. The
pure resolver owns only PHASE_ACTION + DRAW + current player's turn + CARD_ACTION_CHOICE + complete recognized metadata;
all optional/child/failed-search/malformed shapes stay legacy. DrawPhaseOwner calls the existing CombinedEvaluator once,
preserves its Pass or unique action-id winner, calls ResponseFinalizer once with exact immutable rejection history, uses
no RNG, and has no owned fallback/safety re-entry. Rando/ChosenOne use one boundary snapshot and normalized-identical
owner blocks. Legacy score math is pinned exactly: blocked draw -200 + -100000 = -100200, first-offered ties, Pass/noPass
and one-ULP boundaries, mixed evaluator ordering, and raw trace bits. Reserve math has one pure shared owner with cap 10
BEFORE Corridor characters; the shared legacy reader preserves scan/read/log/exception ordering and each evaluator is a
thin delegate. Focused phase boundary 220/0/0/0 plus diff/parity/single-owner/no-fallback proofs. Immediate local reload
authorized after commit; no game simulation and no push. Codex packet 9f2a40ac. See AI_CHANGELOG 2026-07-13.

  ════ FINALIZER-RUNTIME + ACCEPTED-RESPONSE LIFECYCLE PREREQUISITE (2026-07-13, ENGINE, Steve-approved, both bots) ════
FIRST behavioral-migration ENGINE phase (Codex packet bc430fee). Fixes record-before-acceptance: the AI wrote its
decision into its own memory inside decide() BEFORE the engine validated it, so a checked rejection + F2 retry left
the tracker mutated by a move that never happened. Moves the outer Rando/ChosenOne tracker + strategic commit to
AFTER engine acceptance. NEW AiDecisionResult (typed WIRE_RESPONSE/TYPED_REJECTION, mutation mode NONE/OUTER_COMMON)
+ DecisionRejectionKind + pure FinalizedResponseAdapter. SwccgAiController: decide() kept; +decideForEngine (3-arg +
history-aware 4-arg) + onDecisionAccepted/Rejected/AttemptFailed callbacks; RejectionHistory.append + ENGINE_DECISION_INVALID.
SwccgGameMediator AI-path only (human path byte-for-byte unchanged): calls only decideForEngine; loop-local immutable
RejectionHistory (counts 0->1, no map/field/ThreadLocal); acceptance latches then onDecisionAccepted BEFORE carryOut/
startClocks; accepted-callback fault logged + continues (no retry); ATTEMPT_FAILED replaces TraceSession.abandon().
Rando/ChosenOne decide/decideForEngine split (outer mutation deferred to the accepted callback; direct decide() inline
unchanged; before-snapshot moved with recordDecision); trace close CALL-PATH-aware (mediator-facing NONE defers close;
direct closes inline). TraceFinalization disposition-aware completeness (ENGINE_ACCEPTED/REJECTED/TYPED_REJECTION/
ATTEMPT_FAILED; no fabricated finalResponse). Curator forwards same history + callbacks; injected ctor + pure applyOverride;
no network in tests. HeuristicAiBase residual FROZEN pre-accept. BEHAVIOR-NEUTRAL (no wire/score/route/RNG/retry/policy
change); NoOpTraceSink default; ResponseFinalizer still no production caller (V44/V67j pilot step 1b is first). Codex gate
caught 4 contract contradictions pre-code (m00579), council-confirmed + amplified, corrected packet AGREE (m00585). 12
modified + 9 new files, all gemp-swccg-server. Codex m00603 then caught two lifecycle leaks: requeue dispatch failure
could skip ENGINE_REJECTED, and accepted outer-mutation failure could lose the accepted wire. Amendment closes rejection
in a requeue-then-callback finally block and records the accepted wire before marking mutation capture incomplete. Three
exact regressions added. Focused phase pass 216/0/0/0 BUILD SUCCESS; guard/mediator/parity(149 lines)/NoOpTraceSink
static proofs pass; diff-check clean. Watch-point: post-accept
recordDecision omits an engine-invalid attempt from the OUTER tracker; checkSequenceLoop short-circuits <4 so no loop-
detection flip on any tested path; only an already-looping game differs, where not-recording is intended. Codex aggregate
lean independent phase gate pending; per-phase deployment follows immediately on PASS. No push. See AI_CHANGELOG 2026-07-13.

  ════ ACTIVATE+CONTROL DECIDE-EQUIVALENT HARNESS (2026-07-13, TEST-ONLY, both bots) ════
Prereq for deferred Phase B (the ACTIVATE/CONTROL live cutover). Freezes the CURRENT decision boundary as
executable baseline evidence before any route is wired to a bot entry point. Three NEW test files only (zero
src/**/main change): AbstractActivateControlDecisionHarnessTest (contract + scripted AwaitingDecision + minimal
GameState stub + 6 @Test fixtures) + thin Rando/ChosenOne adapters (package-visible setDecisionTraceSinkForTesting,
no reflection). Six pure fixtures freeze exact ops (ordinal/ids/rule-domain-kind/raw float bits/veto/detail) from
the real DecisionTrace — no new production accessor needed (the trace oracle already exposes everything).
activateZeroConfirmLegacy freezes 0/Yes as the KNOWN DEFECT baseline, not policy. Codex source-audit corrections
(m00552, corrected packet sha 40fff3d1): activateAmount real engine min=0 (AbstractSwccgCardBlueprint:2243);
controlTopLevel carries one aligned source cardId (CardActionSelectionDecision:69, stub→null, routing/merge smoke).
Allowance asserts recipient!=turn player DIRECTLY (recipient-valued trace can't prove it). Focused pass 64/0/0/0,
DUMP=false, both bots candidate/score/veto/route/response parity with operation streams byte-identical (botModel
intentionally bot-specific), NoOpTraceSink default intact, deferred seed fixtures untracked. Codex gates.
NOT deployed. See AI_CHANGELOG 2026-07-13.

  ════ FORMATION SAFETY (2026-07-11c, both bots): the four laws become un-outvotable vetoes ════
Shared common/strategy/FormationSafety.java (one copy, no mirror drift) + EvaluatedAction.hardVeto (OR-merged) +
CombinedEvaluator veto-aware selection. L1 no abandoning weak solos / L2 no destiny-less battles (engine ability>=4)
/ L3 no weak-solo deploys with affordable buddy in hand / L4 no weak-solo charges into enemy sites. Exemptions:
2x dominance, flip-gate steers, spies, destiny-eligible solos, doomed-origin retreat. Typed Icon.PERMANENT_WEAPON
weapon math. V171/V172 character-gated. Wired: CS deploy-site, CS move-destination, BattleEvaluator both branches.
Root cause: Codex audit CODEX_SOLO_ABILITY_ROOT_CAUSE_AUDIT_2026-07-11.md. See AI_CHANGELOG 2026-07-11.

  ════ TDIGWATT BESPIN FIXES (2026-07-11b, replay kxn8bvydcd803p2j, both bots) ════
V185 reserveTargetsAreAllUnattachableWeapons returned anyMatched (fuzzy non-weapon hits counted as dead weapons)
→ vetoed TDIGWATT's objective upload 16x while Bespin sat in Reserve; now returns anyDeadWeaponMatched. V21
parser gains UPLOAD_FROM_RESERVE_PATTERN ("may [upload] X, Y, or Z") so pullableCards + the objective-critical
never-lose protection arm on V-card wording (Bespin/Dark Deal were pitched as fodder). See AI_CHANGELOG 2026-07-11.

  ════ LOSS-ANALYSIS FIXES (2026-07-11, replay f27ws5lgy0g58k5p, both bots) ════
V171 contact projection weapon-adjusted + hit-discounted; V172 SOLO DOMINANCE (Steve ruling: >=2x weapon-adjusted
enemy waives the buddy gate — overpower weak solos/sites, override other logic except objective holds); V76
fallback pyrrhic bar also RELATIVE (hitLoss > 0.5x committed forfeit); V24.10 Piett dig requires positive
force/used-pile presence (was firing in Piett-less decks). Parked: Elis Helrot V35.4-vs-abandon dominance,
draw-most vs drain engines. See AI_CHANGELOG 2026-07-11.

  ════ REY-GAME FIXES WAVE 2 (2026-07-10, replay rbujmoc90br3uu4c, both bots) ════
Battle + retreat weapon/hit awareness: V29.7/V76 count OPPONENT weapons (predictor gets theirPower+oppWeaponBonus);
V76 hit-economics now runs on the V22.4 fallback route (armed-opp × avg-forfeit >= 10 → -500 pyrrhic, never
"favorable"); V47+V37.1 threat tiers weapon-adjusted (oppWeaponBonusAt); V67ae mirrors V33's gap>=6 retreat
exemption; V177 word-rescue skips generic type words; V185 no-holder veto mirrored into ATE V67h (-9999, ends the
+1575 dilution). All adjust-in-place. See AI_CHANGELOG 2026-07-10.

  ════ REY-GAME FIXES WAVE 1 (2026-07-10, replay rbujmoc90br3uu4c, both bots) ════
V177/V166 survivability gate: wave needs >=1 affordable buddy + weapon-adjusted enemy power (v177OppWeaponBonus);
V22.3 contested tiers inclusive (>=); V148 deploy-cancel bar 0 for where-to-deploy prompts; V67s+V185 possessive/
fuzzy pull-match guards (isPossessiveTypeTarget); V82.1 parser +'search your Reserve Deck…into hand' pattern
(Clash Of Sabers dead search); V82.2 all-words-recognized + partial→UNKNOWN + V82.2b persona rescue (Chewie
false-blocks). All adjust-in-place. See AI_CHANGELOG 2026-07-10.

  ════ SAFETY CLAMP + V22.7 ADJUST (2026-07-10): "Any Methods Necessary" setup hang fixed (both bots) ════
AMN (109_7) iterative take-combination decision was misrouted by the V22.7 matching+starship catch-all to
evaluatePilotSelection (ignores selectable[]), answered a non-selectable card; engine rejected, mediator swallowed,
game hung (replay 2jg1sj0l3qrlgy6a). FIX: DecisionSafety.ensureValidResponse SELECTABLE-CLAMP (drop unknown/
non-selectable tokens, rebuild preselected+first-selectable, one new/round when returnAnyChange) + V22.7 routing
excludes into-hand/prison texts. Mediator swallow (engine) flagged for Steve, not edited. See AI_CHANGELOG 2026-07-10.

  ════ V193 (CS) EXTENSION (2026-07-09): Endor Bunker flip-gate steer onto the CardSelection deploy route ════
V193 (Bunker-control +400) was DeployEvaluator-only, but Endor deploys route through CardSelectionEvaluator
(V136 CS), so it fired 0 times and Endor Operations never flipped (replay somykkwjy449xul4). Added a V193 (CS)
mirror in {rando,chosenone}/evaluators/CardSelectionEvaluator.java after the V136 CS score, with TWO corrections
from that replay: (1) ability gate — only steer a real character ability>=1 (droids/ability-0 like 4-LOM give no
presence → no control → Establish Secret Base stays illegal) AND deployCost<=3 (cheap body, e.g. Ozzel, not a
wasted bomber); (2) magnitude — playbook deployFlipGateSite 400 + CS penalty offset 730 = ~1130 to DOMINATE the
CS-route anti-hold stack (V67ah -350 + V113 -300 + V24.15 ~-80). Self-limiting (holds gate card, does not control
yet); one body seizes Bunker then guard closes. Extends V193 (no new tag). Both bots, MVN_EXIT=0, DEPLOYED.
RE-TUNE (same day, replay vugpape5lw1bc7rq): first cut steered Ozzel→Bunker but lost by 10 (1240 vs a
REINFORCE-hot Landing Platform 1250, + Ozzel V29 GROUND/CONCENTRATE penalties); cost≤3 also excluded Xizor
(cost 4). Fixed: offset 730→1600 (total steer ~2000, dominates ~1430-1555 competitors) + cost gate ≤3→≤4.
Objective DID flip that game (turn 6); re-tune makes the seizure early/clean. See AI_CHANGELOG.md 2026-07-09.

  ════ SIX ENDOR-GAME FIXES (2026-07-07): maintenance floor, thin-reserve, effective-drain, Endor Bunker plan ════
V58/V67w maintenance floor hardened (DrawEvaluator); V153 THIN RESERVE guard (reserve<=10 → lose hand not
reserve); V24.15 EFFECTIVE DRAIN arm CONSOLIDATED (was V189b, folded in — no contradictory new version);
V193 Endor Operations playbook + Bunker-control bonus (unlock Establish Secret Base). Both bots. See AI_CHANGELOG.md 2026-07-07.

  ════ ObjectivePlaybook #2 (2026-07-07): Endor Operations → typed analyzer-owned playbook (behavior-preserving) ════
ENDOR_PLAYBOOK added to ObjectiveAnalyzer (both bots): identity {8_167,_BACK}, keyCharacter Filters.biker_scout,
keySite Filters.Bunker, weight deployFlipGateSite = the existing V193 +400. DeployEvaluator V193 now reads that
weight instead of the literal. No behavior change (V193 fires only for Endor, which selects ENDOR_PLAYBOOK, weight
== old literal). Same typed shape as MY_LORD_PLAYBOOK. Flip-gate CARD V-scoping (8_124 base vs 207_25 V) is a
separate follow-up. See AI_CHANGELOG.md 2026-07-07.
  ════ ObjectivePlaybook JSON loader (2026-07-08): single runtime objective-data source, Phase 0 behavior-neutral ════
objective_playbooks.json bundled as a jar resource = the ONE place for objective scoring inputs; ObjectiveAnalyzer
(both bots) parses it once (Gson) and hydrates its scoring/setup slots from the active objective's profile (id then
title match). ADDITIVE + idempotent + hard fallback to the text parser. No hardcode replaced yet (comment-out is LAST,
after boundary math). New setup slots: startingLocations/Effects/Interrupts. Pilots My Lord + Endor; Codex delivers 58.
See AI_CHANGELOG.md 2026-07-08.

V193 FIX (same day): Bunker flip-gate steer scoped to Bunker-GATED Establish Secret Base ids {207_25,601_260}
only (base 8_124 gates on 3 Endor sites, not Bunker). New flipCriticalControlCardIds set; DeployEvaluator detects
by id, falls back to title. Behavior-narrowing, both bots. See AI_CHANGELOG.md 2026-07-07.

  ════ ObjectivePlaybook JSON cleanup (2026-07-08): one live enable flag, loaderEnabled ════
Removed the duplicate inert rollout fields from objective_playbooks.json. ObjectiveAnalyzer only reads
loaderEnabled, so runtime behavior is unchanged. Enabled profiles remain exactly Dantooine 7_135, Ralltiir
7_300, Endor 8_167, and My Lord 12_179. See AI_CHANGELOG.md 2026-07-08.

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

  ════ V177/V82.1 PARSER FIX (2026-07-07): objective pull-targets anchored on the pull verb ════
parseSourceCardPullTargets now captures the object AFTER the last pull-verb before 'from
Reserve Deck' (not the whole clause). Fixes Endor Operations (leaked 'while this side up' →
V177 false-DEAD block) and the recurring 'Rando won't pull with his objective' class.
Regression-tested vs Invasion/capital-ship/battleground cases. Both bots. See AI_CHANGELOG.md.

  ════ V156 STACK-MATH refit (2026-07-07): JOIN-GROUP by ability-total, not headcount ════
Shared MovePredicates.siteAbilityTotal/isDefensibleStack/bestJoinDestination (site total
friendly ability >= 4 = destiny-capable). Weak solos join the site that REACHES ability 4,
not the one with the most bodies. Both bots. Deploy-hold + V41 override + V79b Verge guard
from draft c1d5ced8c unchanged. See AI_CHANGELOG.md 2026-07-07 for boundary math.

  ════ V43 UPDATE (2026-07-07): starting-interrupt pick reads game text — effect-deployers beat duds ════
in place: starting-interrupt pick reads what the interrupt DOES (effect-deployers beat duds)
- MOD `server/.../ai/models/rando/evaluators/CardSelectionEvaluator.java` (~7048, inside the V43 STARTING INTERRUPT block) + chosenone mirror (~7003): the generic-tie branch now scans the interrupt's game text — deploys Effects/defensive shields from Reserve = +300/+250/+200 (scaled by "up to three/two"), deploys anything from Reserve = +100, else the old flat 0. Epic Event +1500 top tier unchanged.
- Why: game 2a999777 (2026-07-07, Rando DARK Verge vs Steve). V43 scored Prepared Defenses (deploy up to 3 Effects) and Surface Defense (V) both at flat 50; the engine's shuffle order put Surface Defense first, Rando picked it, zero starting Effects deployed. The morning game (same deck) had the reverse order and looked fine — the pick was a coin flip all along. NOT a regression: pre/post logs show identical 50/50 scoring; the flaw predates today's work.
- Boundary: 50+300=350 vs 50 — Prepared Defenses class wins deterministically. Epic Event 1550 still dominates everything. No other rule scores this ARBITRARY_CARDS surface (turn-0, CSE-only), so no additive-domination risk.
- Verified: compiles clean both bots (real mvn exit checked). PENDING deploy + a setup where the deck carries both interrupt classes: expect `V43 STARTING INTERRUPT: ... deploys Effects from Reserve — PREFER (+300)`.
- Revert: restore the single flat-0 else branch in both files (old lines commented in place).

  ════ T4.1 (2026-07-06): MOVE clobber ladder — rank bands R1-R4 + veto matrix ════
- MOD `server/.../ai/models/rando/evaluators/MoveEvaluator.java` + chosenone mirror — NEW ladder machinery: per-action rank (R4 MANDATORY TRANSIT +20000 / R3 SURVIVAL +12000 / R2 DOCTRINE +6000 / R1 0, max-of-claims) + typed veto flags, applied by `ladderFinalize()` immediately before `actions.add`. Fines clamped to ±2800 ("LADDER CLAMP"); a NEGATIVE clamp hit demotes one band R2→R1 / R3→R2, R4 exempt ("LADDER DEMOTE", ruling L1). R2 claims need strength: own fine >= +200 OR drain-delta >= 2 ("LADDER: R2 claim ... REJECTED — weak" otherwise, ruling L2). First-use band assertion recomputes R2-floor (6000-2800-550=+2650) vs R1-ceiling (1670+250=+1920) from live constants and `logger.error`s on inversion, no crash (ruling L4; "LADDER BANDS OK" line). All 8 early returns in `rankMoveFromLocation` removed (every block always runs). IN-PLACE conversions, old lines commented `// OLD:` at each site: V37.1 CRUSH/FAVORABLE -9999+return → -1500 R1 weight; V85 -2000+return → -800 R1 weight + drain via shared `MovePredicates.drainAt`; V53b Safehouse→Corridor `setScore(9999)` → R4 claim + 800 fine (+20800, order-independent); V53b Mapuzo-exit +800 → R4 claim; V60 Corridor-landspeed `setScore(-9999)` → hard veto; V135 -2000 → hard veto (strengthened); V47 / V49 -9999 → hard veto (gates unchanged); V160 cancel-loop -9999 → -100000 flat (beats everything incl. R4, ruling L3); V38.3 wrong-direction -9999 → DEFERRED veto suppressed ONLY by the V53b transit claim identities ("V38.3 wrong-direction suppressed (R4 mandatory transit)", NOT rank==R4, ruling L3 — move-3f unstall), Castle arm → hard veto; V137 winnability → shared `MovePredicates.canWinAt` with mover-side forfeit sum, VETO applied ONLY to battle-seeking R2 claims (hunt/contest/attack — never R4/R3/non-battle-R2, ruling L3), old -800/-1500 RETAINED as live R1 weights so non-battle paths keep the deterrent; V29.13 drain both endpoints via `drainAt`. Rank claims: R3 = threat RETREAT, V59 DOOMED, V91; R2 battle-seeking = V35/V29.12 hunt, V34/V36 contest, ATTACK + V29.7 (NEWLY gated on `isAdjacentSites` to the best target — "no R2 claim (target not adjacent)" keeps the Rey class at R1); R2 non-battle = V29.13 toward-group + GOOD DRAIN (delta>=2), V31, V73 shuttle, V111, V53 spy follow/reposition, SPREAD, V22.2/V22.5 (attempt kept, fails L2 at today's +160 max). Default -50 moved into the finalizer (rank==R1 + rankMoveFromLocation ran).
- MOD (NEW FILE) `server/.../ai/models/common/strategy/MovePredicates.java` — THE shared graded winnability predicate + drain metric, homed in common/ so CDSE and BOTH bots reach it (orchestrator re-home of the spec's RandoMovePredicates): `canWinAt` = clean win (power >= oppPower && ability >= 4, CDSE:253-254 parity) OR V181 tolerance (0 < gap <= 3 && opp drain at site >= 2 && ability >= 4 && forfeit parity ourForfeit <= theirForfeit*1.25); `drainAt`/`drainDelta` on engine `getForceDrainAmount` (move-6 one-metric consolidation). Fail-open (never veto blind). T2 HANDSHAKE (record in the reorg baton §11): T2's reserved WinnabilityCalculator graded overload must DELEGATE here — do not fork the predicate (header comment in the class).
- MOD `server/.../ai/models/common/strategy/CharacterDeploySiteEvaluator.java` — V181 block now calls the shared `canWinAt` (core gs/mq overload; behavior-identical port: at that point projectedPower < oppPower always, so the clean arm cannot fire; tolerance arm byte-equivalent incl. the one-sided 1.25x forfeit cap; deploy bonus +min(300, drain*100) kept exactly; drain-0 fail-open guard falls through like the old chain; old inline chain commented for revert). Kills the move-4a deploy/move same-tag drift permanently (V136/V137 parity pair).
- MOD `server/.../ai/models/rando/evaluators/ActionTextEvaluator.java` + chosenone mirror — ONLY two alignment edits (nothing else in ATE touched): V60 Hidden Path transit +9999 → +20000 (shares the R4 band with ME's transit arms, beats any ME R3 stack <= ~15350 by construction, move-3e; V60 keeps ONLY this transit arm per ruling P3); V134 Odin Nesloor -9999 → -100000 (its "transport" text co-sums with ME-scored actions, must stay veto-class across the new bands).
- Why: the MOVE audit's clobber class. V85's -2000 + bare return killed the V73 Cantina↔Mos Eisley shuttle (move-1) and Hunt-Down Vader's hunt (move-2); V37.1's -9999 + return stalled Hidden Path forever at a FAVORABLE Mining Village (move-3a); the setScore(±9999) hacks were one-appended-rule away from silent re-decides (move-3c/3d); V38.3 stalled the Mapuzo exit to an empty site (move-3f); V137 refused the exact contested move V181 committed as a deploy (move-4a contradiction).
- Boundary: committed BEFORE implementation in resources/T4_Boundary_Tables_2026-07-06.md §T4.1 (move-1 +5560, move-2 ~+6160, move-3a +19300, 3b +20000, 3c +20800, 3d/-135/-47 vetoes -100000, 3e +20000, 3f suppressed→+20800, 4a +6650, 4b veto; V85-protect ~-720, V37.1-protect ~-1550; band margin +730). Per-row re-verification against the FINAL code: all rows PASS (see session verification; move-2/3c/V85-protect totals differ by the retained V29.7 fine (+~265) / pre-existing +10 "Land (ground deployment)" quirk that setScore used to wipe — outcomes identical).
- Verified: static this session (no local JDK — orchestrator compiles + jar byte-check per BUILD_AND_DEPLOY): zero live setScore/-9999 in either MoveEvaluator; scanner brace-balance on all 6 files; mirror wiring parity rando vs chosenone (3×R4, 4×R3, 16×R2, 5×hard-veto, 1× each wrongDir/canWin/finalize); no `if (false` around any edited block (liveness rule).
- Revert: whole ladder = revert the 6 files (MovePredicates.java is new — delete). Per-tag revert: every conversion keeps its OLD lines commented in place, tagged `UPDATED 2026-07-06 T4.1`; grep the tag to walk all sites.

  ════ MAINT BASIS (2026-07-06): isInPlay gate + engine maintain-cost across the reservation family ════
- ADD `server/.../ai/models/common/strategy/MaintenanceFacts.java` — shared static `maintainCost(blueprint)`: 0 without Icon.MAINTENANCE; else parses game text with case-insensitive regex `end of your turn:.*?use (\d+)` (verified against ALL 8 maintenance cards in the codebase: Lando Scoundrel 1, Han/Chewie/Falcon 3, Chewie Enraged 2, Boba Fett BH 2, Stormtrooper Garrison 1, Thok And Thug 2, Blizzard 4 1, Ap'lek "Use 1 or [Skull]" 1); fallback on no-match = deployCost (orchestrator ruling H3, conservative); memoized by blueprint. Ground truth: the engine charges the card's OWN maintain cost (AbstractNonLocationPlaysToTable :1833/:1894 per-card overrides), NOT deploy cost.
- MOD `rando/evaluators/DrawEvaluator.java` `calculateForceToReserve` (~:483) + chosenone mirror — V58/V78/V79 UPDATED in place: `Zone.isInPlay()` gate at loop top covering ALL FIVE detections (DTF / First Strike / IAO / maintenance / Verge) — `getAllPermanentCards` returns every card ever created including RESERVE_DECK (GameState.java:2800-2802), so an opponent DTF still in his deck phantom-taxed our draws from turn 1 (audit force-economy-5). Same gate on the nested V79 Death Star scan (an undeployed Death Star can't be moved). V67w UPDATED in place: maintenance reserve `+1/card` → `MaintenanceFacts.maintainCost` (+1 was UNDER for the Falcon's 3F upkeep). Lying javadoc fixed.
- MOD `rando/evaluators/PassEvaluator.java` V27 (~:242) + chosenone mirror — basis deployCost → maintainCost; weights 25/50 untouched.
- MOD `rando/strategy/DeployPhasePlanner.java` V22.3 (~:225) + chosenone mirror — basis swap; the audit-refuted "Maintenance cost in SWCCG = card's deploy cost" comment corrected.
- MOD `rando/evaluators/DeployEvaluator.java` + chosenone mirror, FOUR basis sites: V59 self-maint (~:2101), V24.5 sum (~:2188), V29.13 self-compare (~:2312, `< cost` → `< maintainCost`), V38 maintObligation both arms (~:2417). All weights (-2000/-1500/-400/-50/-500/-30) untouched — only the obligation NUMBER changes.
- MOD `rando/evaluators/CardSelectionEvaluator.java` `v173WaveProjection` (~:5703) + chosenone mirror (~:5659) — V173/V174 UPDATED in place per ruling H2: (a) tableMaint scan gets the same in-play gate, (b) tableMaint + the deploying card's own upkeep switch to maintainCost, (c) maintenance-buddy DOUBLE-SPEND fixed: spend was `2x deploy cost`, now `deploy cost + maintainCost`.
- Why: audit force-economy-5 (V58 reserved for threats still IN THE DECK — never-played DTF flipped draw-vs-pass to Pass every turn) + force-economy-1 (deploy-cost basis over-reserved 2-5x — Lando Scoundrel effectively undeployable below pile ~10 on a phantom 5F obligation).
- Boundary (spec rows 1-8 re-verified against the edited code): row 1 reserve 3→2, draw-vs-pass flips to Draw; row 2 surplus +320→+400; row 3 V27 +50→0; row 4 V59 HARD -2000 → V64 TIGHT -400; row 5 V29.13 -50→0; row 6 Falcon +320→+160; row 7 planner effectiveForce 3→7; row 8 v173 budget 3→4. Every flip is the intended bug fix; all rule WEIGHTS unchanged.
- SCOPE CARVE-OUT: MoveEvaluator V27 basis site (~:1577) deliberately NOT swapped — another crew owns MoveEvaluator/ActionTextEvaluator this wave; its deploy-cost basis (soft -60/-80) stays until their pass or COMMIT-2.
- Verified: all edited blocks live (no `if (false` tape); mirrors symmetric; regex hand-verified against all 8 card texts; typed javac compile (web.jar classpath + sourcepath) of all 11 files: 0 errors; full mvn left to orchestrator.
- Revert: delete `MaintenanceFacts.java`; in the 10 touched files un-comment the `superseded T2 COMMIT-1 2026-07-06` lines above each replacement and delete the replacement lines + the two `Zone.isInPlay()` gate blocks per DrawEvaluator and one per CardSelectionEvaluator.

  ════ V192 (2026-07-06): THE pull scorer — pile collapsed to one tiered hub ════
- NEW HUB TAG V192 (precedent V136/V153/V158/V159, orchestrator ruling P3): every reserve-deck pull is now scored by ONE scorer in ActionTextEvaluator's PULL-ENGINE branch. Absorbed tags (old code commented out in place, // per line, tag+date breadcrumbs — revert path): V60-pull baseline (+150/+250), V82 site-pull grant (+2500), V95 dead-interrupt (standalone additive → hardBlock in the veto chain), V97 pull-before-activate (+1500), V100 location-pull-before-chars (+1500), V116 reserve floor (+100), V67l/V67ai location tiers (2000/1800/1600/1500 → 1500/1400/1300/1200), V67m/V67am weapon +600 / device +400 grants (values kept, single-emit), generic V29.7 PULL FIRST +250. V60 keeps ONLY its Hidden Path transit arm (+20000 R4, T4.1 band); the V60-tagged reveal-risk guards keep their historical log tags inside the veto chain for replay-grep continuity.
- MOD rando/evaluators/ActionTextEvaluator.java + chosenone mirror — PULL-ENGINE branch: (a) trigger widened to the union of the absorbed V97/V95/V116 triggers ([upload] + generic take-into-hand; Take-into-hand dispatch above now EXCLUDES reserve-deck takes so they fall through — single owner); (b) ALL vetoes run first and short-circuit via hardBlocked (Guard1 reserve<=2 -9999, Guard2 fail-stop, Guard3 named-miss, V66/V123, V67h, V67ac, V95 folded as hardBlock, V131 not-in-deck, V67ar/V67ao/V149 weapon-holder gates); (c) ONE positive emit: BASE (+150 deploy-grade; +5500 PULL_BASE_ACTIVATE under the old V97 scope — Phase==ACTIVATE, static source EFFECT/EPIC_EVENT/INTERRUPT/OBJECTIVE, K&D + Anger, Fear, Aggression excluded — chained ABOVE V168's +5000) + TYPE TIER (exactly one: LOCATION 1500/1400/1300/1200 by source category via the shared isLocationPull predicate = V67l 34-keyword list ∪ V82 source-text regex ∪ V100's "planet"/"sector"; WEAPON 600; DEVICE 400) + CONTEXT (+50 [download]; +25 chars-or-vehicles-in-hand during DEPLOY on location pulls) — clamped 1750 deploy / 7100 activate; (d) V131 Tier-2 downgrade RE-WIRED from additive -2000 to STRUCTURAL: suppresses ALL positives (incl. V67ak, now gated on !hardBlocked && !downgrade) and emits -200; (e) P1 STAND-DOWN (orchestrator ruling): at reserve<=3 AND DecisionContext.isBattlePlausibleThisTurn() (the SAME shared V61c predicate, no second copy) the activate base stands down to deploy grade so pulls stop eroding the destiny buffer V61c is protecting. Log handle: V192 PULL SCORER (ACTIVATE|DEPLOY-GRADE): base X + tier Y [...] + ctx Z = N [absorbs ...].
- MOD rando/evaluators/DeployEvaluator.java + chosenone mirror — double-count side killed (each commented out in place): V60 +100 baseline (~:873), V67ai Tier 1-3 DE copy (ds-2: the ATE copy where V131 gates it is the single owner; V67i detection KEPT LIVE as predicate for the weapon-gate routing), V67am +600 weapon grant (~:3668). The V162/V67ai-Tier4-HAND hand-location block (+1900 total) is UNTOUCHED — it is the anchor the pull scorer must stay below (V179 lesson). ALL DE vetoes stay (duplicate -9999s harmless).
- CHOSENONE MIRROR NOTE: same merge, one structural absence — chosenone has no V61c destiny buffer and no isBattlePlausibleThisTurn(), so the P1 stand-down is ABSENT there by design (noted in-file; add if V61c is ever mirrored).
- Why: audit rows deploy-sequencing-1 / ds-2 / ds-3 / ds-7 — the pull pile double- and triple-counted across two evaluators (+6250 known, ~+8000 worst case), so (1) activate-window pulls LOST to V168 and fired after activation with a shrunken pool (feedback_pull_before_activate violated), (2) downloads outranked held locations by 6050 (the A Good Friend replay class), (3) V131's downgrade and V95's dead-interrupt save were drowned by the pile they were supposed to kill.
- Boundary (T4_Boundary_Tables_2026-07-06.md §T4.2, re-verified against final code): row 1a pull 5500 > V168 5000 (pull first, was 2000 vs 5000 — FLIP as intended); row 1b objective download 7050 > 5000 (preserved); row 2 hand-location 1950 > pull 1625-1775 > chars ~550 (FLIP: order restored, was pull +8000 first); row 3 downgraded pull -150 < Pass +6 in the activate window (FLIP; deploy-window residual: DE's uniform V38.4 urgency (+50..+500 on every deploy action) can lift a downgraded pull back above Pass when hand >= 9 and NOTHING else is deployable — bounded corner, flagged for follow-up); row 4 reserve<=2 -9999 flat, zero resurrection surface; row 5 V95 dead interrupt -2000 flat (was net +100 and FIRED); row 6 blaster pull single-counted +800 (was +1600).
- Corridor audits (orchestrator ruling P2): ACTIVATE window — live positives >= 2000 are exactly V168 +5000 (activate action), V192 pull 5500-7050, V60 transit +20000 (R4 mandatory, correctly dominates); V79/V29.15/V61c-confirm hits are MULTIPLE_CHOICE/CONFIRM surfaces outside the ACTION_CHOICE corridor. DEPLOY window — generic char/vehicle stacks ~250-550+urgency stay below the pull tier; playbook corners (V52 TDIGWATT T1 +850..+1500, V52b Hidden Path +800, V30 combos +1000, V67ak +800) can stack to ~1650-2350 and edge out UNKNOWN-source (+1200-tier) pulls; V67ak +800 survives as a separate line on pulls (max pull 2525 deploy / 7850 activate in the rare loc+key-char multi-clause corner). Pre-existing relationships, not introduced by the merge; flagged for the playtest queue.
- Verified: all edited blocks live (no `if (false` tape); brace/paren balance clean on all 4 files; grep-zero LIVE emitters of the absorbed lines in both bots — the TDIGWATT admiral/general V29.7 +250/+300 playbook nudge deliberately untouched; exactly ONE positive emit path per pull action. Compile deferred to the orchestrator per wave protocol.
- Revert: in both ATE files un-comment the V116/V95/V97/V100/V82/V67ai/V67am/V29.7 blocks tagged 2026-07-06 and delete the V192 SINGLE EMIT + P1 stand-down + trigger widening (restore the commented old trigger/dispatch); in both DE files un-comment the V60 baseline / V67ai tier switch / V67am grant. All old code is commented in place at each edit site.
(Matching V192 block also inserted in resources/k2-resources/originals/02-rando-history/AI_VERSION_HISTORY.md before the V189 block.)

  ════ SHIELDS (2026-07-06): occupation predicate unification completed (ShieldStrategy trigger A) ════
- MOD: ADD server/.../ai/models/common/strategy/ShieldFacts.java — shared statics: occupiesBothTheaters(game, pid) (VERBATIM body of the private helper ee0a1b435 added to rando CardSelectionEvaluator :8746-8762 — engine canSpot(occupies + battleground_site/system), fails closed), occupiesAnyBattleground(game, pid) (canSpot(occupies + battleground), presence-based, fails closed), shieldsOnTable(gs, pid) (verbatim V117 count scan: owner + DEFENSIVE_SHIELD + Zone.isInPlay()). MOD rando/strategy/ShieldStrategy.java + chosenone mirror — V105 UPDATED in place: prefers4thSlot trigger A's own getTotalPowerAtLocation power>0 theater scan replaced by ShieldFacts.occupiesBothTheaters (old scan commented out in place); V106 UPDATED in place: weOccupyAnyBg trigger-B input → ShieldFacts.occupiesAnyBattleground; oppBgCount/myBgCount stay power-based with a divergence comment (T4 candidate); HOLD log sysBg/siteBg → bothTheaters/anyBg; NEW fourthSlotPick(gs, game, playerId, Predicate<String>) → {preferred, pursue} consolidating the pick+menu dance (null predicate = V124's current preferred!=null semantics; adopters wire in a later wave).
- Why: audit shields-response-2 — after ee0a1b435 unified V51/V112 onto the engine occupies-predicate, prefers4thSlot trigger A still used power>0. The two disagree on zero-power occupation: an unpiloted ship at a battleground system OCCUPIES it but has power 0 (Power.java:49-51), so Battle Order's OWN condition (Card13_054 OccupiesCondition) could be LIVE while trigger A said no — 4th slot dead (V117 -9999 / V105 -5000 / V124 -3000) though the card would fire. Boundary rows 13/15 in resources/T4_Boundary_Tables_2026-07-06.md.
- Boundary: the trigger-A/B predicate swap is the ONE intended behavior change (the audit's deliberate low-severity unification); row 14: preferred card not on the menu → HOLD lines fire exactly as before, zero swing. ALL weights stay per-caller, untouched: -9999/+2000 V117, -5000/+2000 V105, -3000 V124. fourthSlotPick is score-neutral by construction and currently UNCALLED (CSE/ATE off-limits this wave).
- Verified: statically — both prefers4thSlot blocks live pre-edit (no if(false) tape) in both bots; no live references to the commented-out scan variables remain; brace/paren balance clean in all three files; ShieldFacts bodies diff-checked verbatim against CSE :8746-8762 and the V117 scan; chosenone mirror diff-verified identical modulo package/import. Compile deferred to orchestrator. Live handle: unpiloted-ship-only BG system + BG site with characters → 'V105 4TH SLOT (A): Battle Order — we occupy system+site battlegrounds' where the pre-fix log showed sysBg=false.
- Revert: in both ShieldStrategy files un-comment the 'SUPERSEDED 2026-07-06 (T2 MOVE #3)' power-scan block + the old weOccupyAnyBg line, delete the two ShieldFacts call lines + the occupyBothTheaters local + the fourthSlotPick/FourthSlotPick additions, restore the old HOLD log line; delete ShieldFacts.java.

  ════ MAINT CACHE (2026-07-06): one per-decision maintenance/DTF computation, five consumers ════
- MOD/ADD: ADD common/strategy/ForceReserveService.java — immutable Facts {dtfActive, firstStrikeActive, iaoActive, grabberUnused, maintenanceObligation, maintenanceCardCount, undercoverSpyCount, vergeNeedsDeathStarMove} + static compute(game, gs, playerId): ONE Zone.isInPlay()-gated pass over getAllPermanentCards, detection copied verbatim from the five consumer sites (post-COMMIT-1 text, MaintenanceFacts basis). MOD rando + chosenone DecisionContext — lazy getForceReserveFacts() (one compute per decide) with the soak instrument: every 20th decision, every cache READ recomputes fresh and logs `MAINT CACHE MISMATCH` at WARN on divergence. MOD the five consumers in both bots, inline scans commented out in place, weights untouched: DrawEvaluator V58/V67w/V78/V79 → Facts; PassEvaluator V27.1 (20/40/60) + V27 (25/50) → Facts; MoveEvaluator V29 (-100/-150/-60) + V27 (-80) → Facts (closes COMMIT-1's MoveEvaluator deploy-cost-basis carve-out — the only value change); DeployEvaluator V24.5 (-50/-50) + V29.13 (-30) + V38/V53 → Facts (V59 pendingDeployCost + this-card maintain adds stay local); DeployPhasePlanner V22.3 → static compute at plan creation (retires the `if (allCards != null)` wrong-variable guard with the block).
- Why: T2 plan move #1 — five independent copies of the same scan drifted (the MoveEvaluator basis miss was the live example); one cached computation per decision ends the drift class and cuts per-action rescans. All V-tags updated in place, no new tag.
- Boundary: score-neutral by construction — every consumer reads values identical to its post-bug-fix inline scan. Only deltas: MoveEvaluator V27's completed basis swap (soft -60/-80 now fire on the engine maintain number) and 3 documented unifications where the old copies disagreed with each other (exact-opponent ownership; ANY-unused-grabber = MoveEvaluator semantic; Zone-gated spy count) — all no-op on real boards. NOT moved: DeployEvaluator's V48/V79 Vader+Verge combined scan (Vader-position isn't a shared fact).
- Verified: all edited blocks live pre-edit (no `if (false` tape); rando/chosenone blocks diff-verified IDENTICAL pre-edit and edited symmetrically; no orphaned refs to commented-out locals; full javac compile of all 13 files with symbol resolution against the built engine jars in the app container (syntax AND symbols clean — mvn package still deferred to orchestrator). Live handle: grep `MAINT CACHE MISMATCH` across 2 full games = zero hits ⇒ cache certified neutral, then comment out the soak branch.
- Revert: delete ForceReserveService.java + the getForceReserveFacts() blocks in both DecisionContexts; in the 10 consumer files un-comment each `T2 MOVE #1 COMMIT-2` commented block and delete the Facts-read lines (+ breadcrumb comments) directly above it.

  ════ V29 UPDATE (2026-07-06): BESPIN-FIRST released when objective forbids Executor or no capital path (TDIGWATT bug B) ════
- MOD `server/.../ai/models/rando/evaluators/DeployEvaluator.java` (~1234, inside the V29 non-exempt branch) + `chosenone/evaluators/DeployEvaluator.java` (~1185): before applying the V29 -500 "deploy Executor first" penalty, run two release checks: (a) `ObjectiveAnalyzer.objectiveForbidsDeployingExecutor()`; (b) DeckOracle scan for any STARSHIP/CAPITAL DeckCard in HAND/RESERVE_DECK/FORCE_PILE/USED_PILE. Either releases the gate (LOG.info "V29 BESPIN-FIRST RELEASED", no penalty); otherwise the original -500 + LOG.warn fire verbatim (kept unchanged in the else branch).
- MOD `server/.../ai/models/rando/strategy/ObjectiveAnalyzer.java` + `chosenone/strategy/ObjectiveAnalyzer.java`: new field `objectiveGameText` (stored in analyze() ~126, cleared in reset() ~412) + new method `objectiveForbidsDeployingExecutor()` (rando ~1052 / chosenone ~1032): sentence-scan of the objective blueprint's game text for "executor" plus a deploy-forbid phrase ("may not deploy" / "cannot deploy" / "may not be deployed") in the same sentence. Universal — no card ids or name lists.
- Why: game 2026-07-02 02:09, Rando DARK on TDIGWATT (V) (Card226_012, deploys with I'm Sorry). Its game text — verified from `src/gemp-swccg-cards/.../set226/dark/Card226_012.java:45` — reads "For remainder of game, you may not deploy Admiral's Orders or [Death Star II] Executor." The deck can never occupy Bespin space, so the V29 gate never released and EVERY bare character deploy ate -500 all game; Rando flooded weak solos, held 1 CC site vs the 3 needed, never flipped.
- Boundary: variant character deploys return to natural V38/V136 magnitudes (~+50..+400 > Pass 5-8); locations still exit earlier at the +200 LOCATION bucket; exempt actions (ship/AMSD/Executor/Bespin) untouched — the release code sits inside the non-exempt branch. Classic TDIGWATT (Card109_012) keeps the gate whole: no "may not deploy" clause on either side (back-side "may not cancel"/"may not be modified" don't match) and Executor is an accessible capital in Reserve. Release adds NO positive score, so nothing new can dominate.
- Verified: statically only — V29 branch confirmed live (no `if (false`) in both bots; card text read from source; forbid-scan checked against classic front+back and variant front+back texts; single diff hunk per DeployEvaluator, 4 per ObjectiveAnalyzer. Compile + reload-ai deferred to orchestrator. PENDING live check: a TDIGWATT (V) game should show zero "V29 BESPIN-FIRST: BLOCKING" lines (INFO "RELEASED" lines if level permits) with characters deploying to CC sites; a classic TDIGWATT game should still show BLOCKING lines pre-Executor.
- Revert: in both DeployEvaluators delete the `bfGateReleased` block and un-nest the original -500 addReasoning/LOG.warn (kept verbatim in the else); in both ObjectiveAnalyzers delete the `objectiveGameText` field/store/reset lines and `objectiveForbidsDeployingExecutor()`.

  ════ V61c UPDATE (2026-07-06): battle-intent bypass — keep-3 buffer only when a contested location exists ════
- MOD `server/.../ai/models/rando/evaluators/DecisionContext.java` (~line 309) — ADD `isBattlePlausibleThisTurn()`: the ONE shared battle-intent predicate. Conservative v1 per Steve: battle is "plausible" if ANY location is CONTESTED (both sides' total power > 0 there), the same scan V61b uses in BattleEvaluator (~630). Null game/gameState/playerId/opponent or any exception → true (keep 3). Zero contested locations → false (deploy-and-end turn).
- MOD `server/.../ai/models/rando/evaluators/ForceActivationEvaluator.java` `calculateActivationAmount` (~line 193) — the V61c keep-3 cap (`min(maxAvailable, reserveDeck - 3)`) now applies ONLY when the predicate is true; otherwise activate `maxAvailable` in full. Old always-on line kept commented above. V67at end-game clamp still runs after the branch, unchanged, and still wins when more conservative.
- MOD `server/.../ai/models/rando/evaluators/ActionTextEvaluator.java` V168 carve-out (~line 185) + V38.3 confirm carve-out (~line 1404) — both `reserve <= 3` conditions become `reserve <= 3 && isBattlePlausibleThisTurn()`. No-contested turns fall through to normal V168 ALWAYS ACTIVATE (+5000) and normal V38.3 bounce-back (No +9999). Old conditions kept commented. The second V38.3 arm (`evaluateActivateForce` ~5060, flat +500, no carve-out) untouched — dominated either way.
- Why: Steve (2026-07-01): "If Rando intends to battle that turn, he needs to save 3. If he intends to deploy and end turn without battling he can activate all force." The 2026-06-29 V61c protected the buffer EVERY turn, throttling activation on turns with no possible battle. Activate phase precedes Deploy/Battle, so intent is PREDICTED: contested location exists → battle plausible → keep 3 (old behavior); zero contested → activate all. Bias toward keeping 3 when unsure — a false "no battle" re-opens the no-destiny bug; a false "battle" only costs a little activation.
- Boundary: no new score magnitudes — the change only selects between the two pre-existing regimes (V61c buffer vs V168/V38.3/V57 full-activate). All three sites share the SAME predicate (the original V61c bug was these sites disagreeing), and board presence can't change during the Activate phase, so they cannot split within a turn. V61b (overpower exception) and V67at (end-game reserve-2) untouched. Known blind spot (accepted, v1): Rando can CREATE a contested site by deploying onto opponent presence after activating full; the BattleEvaluator V61 reserve guard (-800/-400/-200 at reserve 0/1/2) still discourages destiny-dependent battles on such turns.
- Verified: marker line `V61c BATTLE-INTENT: no contested location — activating full` logs at each site when the bypass actually changes the outcome (FAE: only when the cap would have clamped; V38.3: once, on the "No" option). Grep-confirmed zero `if (false` in the edited branches (live code) and zero V61c/DESTINY BUFFER logic in chosenone/ (nothing to mirror — chosenone never got V61c). Compile + live-jar byte-check pending with the orchestrator's build.
- Revert: in all three files, delete the `V61c UPDATED 2026-07-06` blocks and un-comment the `V61c pre-2026-07-06 (always-on buffer)` lines directly above each replacement; remove `isBattlePlausibleThisTurn()` from DecisionContext.

  ════ V169 + V47 UPDATE (2026-07-06): single-owner retreat soft-block with retry budget; Lando stay-lock gated on CC objective + survivability ════
- MOD `server/.../ai/models/rando/evaluators/ActionTextEvaluator.java` (fields ~41-48, V169 branch ~128-186) + chosenone mirror — V169 UPDATED: ATE is now the SINGLE owner of the endangered-mover soft block, resized -400 → -250, with a per-turn retry budget (3 soft excusals per action, keyed by actionText, reset on turn change like blockedResponses) after which the V163 hard veto (-100000) resumes.
- MOD `server/.../ai/models/rando/evaluators/MoveEvaluator.java` (~164-213) + chosenone mirror — V169 UPDATED: the duplicate soft block (EvaluatedAction ctor -400 PLUS addReasoning -400 = -800, both add per EvaluatedAction.java:30/:44) commented out in place; the `continue` removed so endangered movers fall through to rankMoveFromLocation and their retreat bonuses actually attach; the -9999 hard block for non-endangered movers unchanged, wrapped in else.
- MOD same MoveEvaluators (rando ~378-445, chosenone ~367-434) — V47 UPDATED: the Lando -9999 stay-lock now requires (a) an active Bespin/Cloud City occupation objective (ObjectiveAnalyzer isAnalyzed + needsBespinSystemPresence; pre-flip isObjectiveRelevantLocation, post-flip isFlipBackProtectionLocation — the site must still serve the objective) AND (b) survivability (skip when powerDiff < RandoConfig.BATTLE_DANGER_THRESHOLD = -6, the same RETREAT boundary calculateThreatLevel uses; getTotalPowerAtLocation both sides). The generic 'platform' site fragment commented out in place (Title.java proof: Endor: Landing Platform (Docking Bay), Coruscant: Private Platform, Kashyyyk: Skyhook Platform all matched; every CC site title starts "Cloud City: " so the 'cloud city' fragment alone keeps full CC coverage incl. East Platform).
- Why: audit rows cross-brain-1 + move-8 (Rando_Overlap_Audit_2026-07-04.xlsx, both CONFIRMED-high, re-verified untouched at HEAD 2026-07-06). cross-brain-1: V169 was implemented twice for the same actionId; the merged score on the Asajj-class boundary was ~-1050 vs Pass +5, so the retreat retry the tag was written to enable (replay lk6xgsokjcwrwxuu, 6v27, beaten next turn) was mathematically impossible — both code comments claiming "-400 lets the retreat retry" were false. move-8: any *lando* at any site containing 'platform' froze at -9999 on any deck with no danger exit, overriding RETREAT +150 / V22.5 +160 / V53 +500; false-positive sites are real cards.
- Boundary (additive-domination, done before code): Asajj case AFTER = ATE(-250 + V35.4 +150) + MoveEval RETREAT +150 = +50 > Pass 8 → retreat retries (spy variant +150). No-safe-destination case: 3 excusals then hard veto → Pass wins, turn terminates (Keder guard preserved; worst case 3 extra cancel cycles). DANGEROUS-tier endangered (-80) and RISKY-tier (-600) still lose to Pass. Non-endangered blocked: -100000 + -19998 unchanged. V47: TDIGWATT Lando at a live CC site with diff >= -6 locks exactly as before; diff < -6 or non-CC deck or irrelevant site → lock stands down and the normal leave doctrines decide. Only penalties were reduced; nothing new dominates.
- Known limits: the -9999/-19998 ctor+addReasoning double-add on the HARD block paths (MoveEvaluator + audit-noted DeployEvaluator) is NOT fixed here — hard vetoes dominate at either magnitude; separate consolidation item. V47 now requires OUR objective to be a CC objective: a Lando sitting at CC sites in a non-objective deck no longer gets the lock (deliberate narrowing per the audit).
- Mirrored to chosenone (both rules exist there; line-for-line identical, chosenone package types).
- Verified: all touched branches live (no `if (false` tape in any of the 4 files); typed compile of all 4 files clean (0 errors) via in-container javac 21 against web.jar + sourcepath; full mvn build left to the orchestrator. Live-game grep handles: `V169: soft-block` / `V169: retry budget exhausted` / `V169 MoveEvaluator: endangered mover ... blocked-but-excused` / `V47 LANDO STAY skipped`.
- Revert: in both ATEs delete the retry-budget fields and if/else, un-comment the old -400 addReasoning line; in both MoveEvaluators un-comment the old softBlockedMove block (restoring its `continue`), unwrap the hard block from the else, and restore the V47 one-liner by un-commenting `|| locLower.contains("platform")` and deleting the two gate blocks plus the if/else around the -9999 addReasoning.

  ════ V67bc UPDATE + V191 (2026-07-06): non-bucket epilogue rescues into-hand pulls; V191 TOPN candidate logging ════

- MOD src/.../ai/models/rando/evaluators/CombinedEvaluator.java — V67bc updated in place: EPILOGUE before the DPS-walk PASS return (+ NON_BUCKET_EPILOGUE_FLOOR=+50 constant); V191 top-5 candidate log at the decision site.
- MOD src/.../ai/models/chosenone/evaluators/CombinedEvaluator.java — exact mirror of both.
- MOD src/.../ai/models/rando/RandoCalAi.java — V191 breadcrumb on the fallback-heuristic branch (path + final pick; top-5 lives in HeuristicAiBase's private pick loop, not instrumented).
- MOD src/.../ai/models/chosenone/TheChosenOneAi.java — mirror of the breadcrumb.

Why: (audit deploy-sequencing-4) DeployPhaseScript deliberately excludes "take X into hand / into pile" Reserve pulls from every bucket (they don't put a card on table), but the V67bc walk could only ever return FROM a bucket — when all buckets were exhausted it returned PASS, so a well-scored into-hand pull (V116 +100 baseline and up) was unpickable forever. The epilogue picks the best NON-bucket, non-PASS-typed action when it clears +50; otherwise the original PASS runs. V191 logs one line per CombinedEvaluator decision ('V191 TOPN: <decisionType> phase=<phase> :: actionId=score x5', sorted on a COPY so live tie-breaking never moves) — the dominance-regression detector for future rule additions.

Boundary: epilogue is only reachable after every bucket's best is < -100, so it can only replace PASS, never a bucket winner (a +900 non-bucket pull still loses to a +420 STEP 3 deploy). Junk below +50 (e.g. keyword-only ~+45) still passes; -60 non-bucket still passes. PassEvaluator's Cancel-id pass (V27/V27.1 bonuses can stack past +50) is excluded by ActionType. V191 is instrumentation only, zero scoring changes.

Verified: no 'if (false' guard in either CombinedEvaluator (walk is live); rando/chosenone mirrors diff-identical except package/import; brace/paren balance clean; compile deferred to orchestrator.

Revert: delete the V67bc EPILOGUE block + NON_BUCKET_EPILOGUE_FLOOR constant and the V191 blocks in the four files (all-additive change, zero lines removed — plain deletion of the tagged blocks restores prior behavior exactly).

  ════ V82/V60 + V25 + V35.4 UPDATE (2026-07-06): site-pull respects hardBlocked (reserve<=2 now -9999); Simple Tricks uses engine isBattleground; spy detection ownership fixed ════
- MOD `server/.../ai/models/rando/evaluators/ActionTextEvaluator.java` + `chosenone/evaluators/ActionTextEvaluator.java`, three in-place V-tag updates (no new tags):
  (1) V82/V60 UPDATED (rando ~4325/~4372/~4611; chosenone ~4273/~4321/~4560): V82 SITE PULL +2500 moved INSIDE the `if (!hardBlocked)` region (old pre-guard placement commented out above the guards); V60 Guard 1 (reserve <= 2) magnitude -400 → -9999, matching the DeployEvaluator copy of the same guard.
  (2) V25 UPDATED (rando ~5318; chosenone ~5243): Simple Tricks battleground test switched from static printed icons (`hasIcon DARK_FORCE && LIGHT_FORCE`) to the engine's `modifiersQuerying.isBattleground(gameState, drainLoc, null)` — the same call pattern V140 used before its 2026-07-04 rework; static icons kept only as fallback when the game object is unavailable.
  (3) V35.4 UPDATED (rando ~3569; chosenone ~3517): opponent-spy detection fixed to owner == OPPONENT && isUndercover (old inverted test flagged OUR OWN spy); the +250/+150 move bonus is now scoped to actions whose MOVER (the action's cardId) is at the blocked location; an undercover mover never collects it; unresolvable mover falls back to the old any-location scan.
- Why: Rando_Overlap_Audit_2026-07-04 rows deploy-sequencing-1, control-drain-5, move-7 (all CONFIRMED, high). (1) A reserve=2 live-target pull scored +2200..+3700 against Guard 1's -400, so Rando fired the search and revealed his last 2 reserve cards — exactly what the guard exists to prevent. (2) A dual-icon site dynamically made non-battleground still passed the static check, so Rando drained into a guaranteed Simple Tricks cancel — reopening the incident V25 was written to fix. (3) Our V170 drain-block spy paid +250 to every move on the table, including its own move-away: V53 -300 + 250 + RETREAT +150 = +100 > Pass → spy abandoned its post.
- Boundary (additive-domination math done pre-code): (1) reserve=2 pull now V116 +100 [+V100 +1500] -9999 = -9899/-8399 < Pass ~5-8 (was +2200/+3700 > Pass); reserve >= 3 unblocked pulls score IDENTICALLY to before (V82 unchanged inside the open path); max ungated positive stack (+1600) is dominated by -9999. (2) Dynamic non-BG + Simple Tricks: -9999 + return (was drain fires and gets cancelled); dynamic-BG site no longer falsely blocked; no Simple Tricks on table → no change. (3) Spy move-away: -300 + RETREAT 150 = -150 < Pass (was +100 > Pass); opp-spy-at-our-site true positive upgraded +150 → +250 and scoped to movers at that site.
- Mirrored to chosenone 1:1 (regions were byte-identical apart from the DeckOracle package). NOT yet compiled or live-verified — orchestrator compiles after the batch. Verify in game logs: `V60 RESERVE RISK ... too risky (-9999)`, `V25 SIMPLE TRICKS: BLOCKING`, `V35.4: UNDERCOVER SPY at ...` firing only on genuine opponent spies.
- Revert: (1) restore -400 in Guard 1 (old line commented in place) and un-comment the old V82 block above the guards / delete the moved copy inside `!hardBlocked`; (2) delete the `context.getGame()` branch — the old static check survives as the else-if fallback; (3) restore the commented-out inverted ownership branch and delete the `v354Mover` resolution/scoping block. All three old versions are commented out at the edit sites in both files.

  ════ V58/V67w/V79 + V27 + V22.3 + V59 + V24.5 + V29.13 + V38 + V173/V174 UPDATE (2026-07-06, T2 helper COMMIT-1): in-play gate kills phantom reserves; engine maintain-cost basis via new shared MaintenanceFacts ════
- ADD `common/strategy/MaintenanceFacts.java` (shared, both bots — CharacterDeploySiteEvaluator precedent): `maintainCost(bp)` = 0 without Icon.MAINTENANCE, else regex `end of your turn:.*?use (\d+)` (case-insensitive) over game text, fallback deployCost (ruling H3), memoized. Ground truth: the ENGINE charges each card's own maintain cost (AbstractNonLocationPlaysToTable :1833/:1894), not deploy cost. Verified against the full 8-card maintenance universe: Lando Scoundrel 1 (deploys 5), Han/Chewie/Falcon 3, Chewie Enraged 2, Boba Fett BH 2, Stormtrooper Garrison 1, Thok And Thug 2, Blizzard 4 1, Ap'lek 1 ("Use 1 or [Skull]").
- V58 family (DrawEvaluator `calculateForceToReserve` ~:483, both bots): `Zone.isInPlay()` gate at loop top of the permanent-card scan, covering ALL FIVE detections — DTF, First Strike, IAO, maintenance, Verge (ruling H1). Premise (audit force-economy-5, verifier-confirmed): getAllPermanentCards returns EVERY card ever created incl. RESERVE_DECK, so a never-played opponent DTF taxed our draw from turn 1. Nested V79 Death Star scan gets the same gate (an undeployed Death Star needs no move reserve). V67w basis +1/card → maintainCost (was UNDER for the Falcon's 3F upkeep — the original V67w complaint, correct number this time).
- Basis swaps deployCost → maintainCost, weights untouched at every site: PassEvaluator V27 (~:242, 25/50), DeployPhasePlanner V22.3 (~:225, refuted "maintenance = deploy cost" comment corrected), DeployEvaluator V59 self (~:2101, -2000/-1500), V64 tight (-400), V24.5 (~:2188, -50), V29.13 self-compare (~:2312, -50/-500), V38 maintObligation both arms (~:2417).
- v173WaveProjection (CardSelectionEvaluator ~:5703 rando / ~:5659 chosenone), ruling H2: tableMaint scan in-play-gated + maintainCost basis; the deploying card's own reserve uses its maintainCost (was full deploy cost); maintenance-buddy DOUBLE-SPEND fixed (spend was 2x deploy cost, now deploy cost + maintainCost). V176 fee and V177 cap logic untouched.
- Boundary rows 1-8 from the T2 helper spec (resources/T4_Boundary_Tables_2026-07-06.md) re-verified against the edited code — every flip intended: phantom-DTF draw-vs-pass flips to Draw (+95); reserve-deck Lando surplus +320→+400; V27 pass-bonus 50→0 at pile 4; Lando deploy at pile 8: V59 HARD -2000 → V64 TIGHT -400; V29.13 -50→0; Falcon draw pressure +320→+160; planner effectiveForce 3→7; v173 budget 3→4.
- SCOPE: MoveEvaluator's V27 maintenance copy (~:1577) NOT swapped — orchestrator assigned MoveEvaluator/ActionTextEvaluator to another crew this wave; its deploy-cost basis (soft -60/-80) stays until their pass or COMMIT-2.
- Old lines commented out in place (`superseded T2 COMMIT-1 2026-07-06`) at every edit site, both bots. Compile deferred to orchestrator. Live-game grep handles: `V58 RESERVE: DTF=false` vs a deck containing unplayed DTF; `V22.3 MAINTENANCE: Lando Calrissian, Scoundrel requires 1 Force upkeep` (was 5); `V58 RESERVE: ... maint=3` with the Falcon on table; `V59 MAINTENANCE OK`/`V64 MAINTENANCE TIGHT` where `V59 MAINTENANCE HARD` used to fire.

  ════ V58/V67w/V78/V79 + V27/V27.1 + V29 + V22.3 + V24.5 + V29.13 + V38/V53 UPDATE (2026-07-06, T2 helper COMMIT-2): ONE shared per-decision force-reserve computation — new ForceReserveService + DecisionContext cache ════
- ADD `common/strategy/ForceReserveService.java` (shared, both bots — MaintenanceFacts/ShieldFacts precedent): immutable `Facts {dtfActive, firstStrikeActive, iaoActive, grabberUnused, maintenanceObligation, maintenanceCardCount, undercoverSpyCount, vergeNeedsDeathStarMove}` + static `compute(game, gs, playerId)` — ONE Zone.isInPlay()-gated pass over getAllPermanentCards, detection copied from the five consumer sites at their post-COMMIT-1 text (MaintenanceFacts basis).
- DecisionContext (both bots): lazy `getForceReserveFacts()` = one compute per decide() call. SOAK: every 20th decision, every cache READ recomputes fresh and `soakCompare` logs `MAINT CACHE MISMATCH` (WARN) on any field divergence. Remove after 2 clean full games.
- Consumers rewired, inline scans commented out in place (`T2 MOVE #1 COMMIT-2 (2026-07-06)`), ALL weights untouched: DrawEvaluator V58/V67w/V78/V79 five-detection scan (+ nested Death Star scan) → Facts; PassEvaluator V27.1 (20/40/60) + V27 (25/50) → Facts; MoveEvaluator V29 DTF/grabber (-100/-150/-60) + V27 maintenance (-80) → Facts — closes COMMIT-1's declared MoveEvaluator carve-out (last deploy-cost basis site, audit force-economy-1; the only value change in this commit); DeployEvaluator V24.5 (-50/-50) + V29.13 DTF/grabber (-30) + V38 maintObligation/interruptReserve/V53 spy → Facts (this-card maintain adds + V59 plan-aware pendingDeployCost stay local per-candidate); DeployPhasePlanner V22.3 → static compute at plan creation (no DecisionContext; the old loop's `if (allCards != null)` wrong-variable guard retires with the block).
- Documented unifications (only where the five old copies disagreed with each other; all no-op on real boards): DTF/FS/IAO ownership = exact-opponent match (DrawEvaluator alone used any-non-friendly); grabberUnused = ANY unused grabber (MoveEvaluator semantic; DeployEvaluator broke on first grabber card regardless of state); spy count Zone-gated (isUndercover only true in play). NOT moved: DeployEvaluator's V48/V79 Vader+Verge combined scan (Vader-position isn't a shared fact).
- Verified statically: all blocks live pre-edit (no `if (false` tape), rando/chosenone blocks diff-identical pre-edit and edited symmetrically, no orphaned refs to commented-out locals, brace balance clean. Compile deferred to orchestrator. Live handle: grep `MAINT CACHE MISMATCH` across 2 full games = zero hits ⇒ cache certified score-neutral; then comment out the soak branch.
- Revert: delete ForceReserveService.java + both getForceReserveFacts() blocks; un-comment each `T2 MOVE #1 COMMIT-2` block in the 10 consumer files and delete the Facts-read lines above it.

  ════ V105/V106/V107 UPDATE (2026-07-06, T2 helper MOVE-3 remainder): trigger-A/B occupation predicates unified into new shared ShieldFacts; fourthSlotPick consolidation helper ════
- ADD `common/strategy/ShieldFacts.java` (shared, both bots — MovePredicates/MaintenanceFacts precedent, T2 spec placement decision): `occupiesBothTheaters(game, pid)` — VERBATIM body of the private helper ee0a1b435 added to rando CardSelectionEvaluator (:8746-8762), engine `canSpot(occupies + battleground_site/system)`, fails closed; `occupiesAnyBattleground(game, pid)` — engine `canSpot(occupies + battleground)`, fails closed; `shieldsOnTable(gs, pid)` — verbatim V117 on-table count scan (owner + DEFENSIVE_SHIELD + Zone.isInPlay()).
- V105 UPDATED in place (ShieldStrategy.prefers4thSlot, both bots): trigger A's own `getTotalPowerAtLocation` power>0 theater scan — the audit shields-response-2 Status@HEAD residue left after ee0a1b435 — replaced by `ShieldFacts.occupiesBothTheaters`; old scan commented out in place. V106 UPDATED in place: `weOccupyAnyBg` (trigger-B input) switches from the power-scan derivation to `ShieldFacts.occupiesAnyBattleground`. `oppBgCount`/`myBgCount` (triggers B/C) deliberately STAY power-based with a divergence comment (T4 candidate). HOLD log placeholders `sysBg/siteBg` → `bothTheaters/anyBg`.
- Why (boundary rows 13/15, resources/T4_Boundary_Tables_2026-07-06.md): the two predicates disagree on ZERO-POWER occupation — an unpiloted ship at a battleground system OCCUPIES it but has power 0 (Power.java:49-51), so Battle Order's OWN condition (Card13_054 OccupiesCondition) could be LIVE while trigger A said no → prefers4thSlot null → 4th slot dead (V117 -9999 / V105 -5000 / V124 -3000) though the card would fire. After: gate and card can never disagree — the slot opens exactly when the card itself is live. Trigger-B swap is the same class (presence-based occupy matches the shields' play conditions, feedback_fourth_shield_conditional).
- NEW `fourthSlotPick(gs, game, playerId, Predicate<String> preferredOnMenu)` → `{preferred, pursue}` on ShieldStrategy (both bots): consolidates the pick+menu dance V117/V105/V124 each hand-roll; `pursue = preferred != null && (predicate == null || predicate.test(preferred))`; null predicate reproduces V124's current `preferred != null` semantics. Score-neutral by construction, currently UNCALLED — CardSelectionEvaluator (V117/V105) and ActionTextEvaluator (V124) adopt it in a LATER wave (both files off-limits this wave per orchestrator; CSE's private occupiesBothTheaters copy delegates/retires then).
- Boundary: the trigger-A/B swap is the ONE intended behavior change (the audit's deliberate low-severity unification); row 14: preferred card not on the menu → HOLD lines fire exactly as before, zero swing. ALL weights stay per-caller, untouched: -9999/+2000 (V117), -5000/+2000 (V105), -3000 (V124).
- Mirrored to chosenone 1:1 (twins were byte-identical modulo package/RandoLogger import; regenerated + diff-verified). Verified statically: both prefers4thSlot blocks live pre-edit (no `if (false` tape), no live refs to the commented-out scan vars, brace/paren balance clean, ShieldFacts bodies diff-checked verbatim against their sources. Compile deferred to orchestrator. Live handle: unpiloted-ship-only BG system + BG site with characters → `V105 4TH SLOT (A): Battle Order — we occupy system+site battlegrounds` where the pre-fix log showed `sysBg=false`.
- Revert: in both ShieldStrategy files un-comment the `SUPERSEDED 2026-07-06 (T2 MOVE #3)` power-scan block + old weOccupyAnyBg line, delete the ShieldFacts calls / occupyBothTheaters local / fourthSlotPick + FourthSlotPick additions, restore the old HOLD log; delete ShieldFacts.java.

  ════ V192 (2026-07-06, T4.2 pull-engine merge): ONE reserve-deck pull scorer — vetoes first, single tier table, PULL_BASE_ACTIVATE +5500 ════
- NEW HUB TAG (precedent V136/V153/V158/V159, orchestrator ruling P3). Home: ActionTextEvaluator PULL-ENGINE branch, both bots. Absorbed (old code commented out in place at every site, revert path): V60-pull baseline +150/+250, V82 +2500 site-pull grant (regex survives inside the shared predicate), V95 dead-interrupt (additive → hardBlock in the veto chain), V97 +1500 pull-before-activate (scope predicate survives as the activate-base gate), V100 +1500 location-pull-first (vocabulary → predicate, chars-in-hand check → +25 context bonus), V116 +100 floor, V67l/V67ai location tiers re-sized 2000/1800/1600/1500 → 1500/1400/1300/1200, V67m/V67am weapon +600 / device +400 (values kept, single emit), generic V29.7 PULL FIRST +250. V60 keeps ONLY its Hidden Path transit arm (+20000 R4). Veto-chain lines keep historical V-tags for replay-grep continuity.
- Shape: trigger widened to "[download]" ∪ "from reserve deck"(non-shuffle) ∪ "[upload]" ∪ generic take+into-hand (Take-into-hand dispatch now excludes reserve-deck takes — single owner). Vetoes short-circuit via hardBlocked: Guard1 reserve<=2 -9999 / Guard2 fail-stop / Guard3 named-miss / V66+V123 / V67h / V67ac / V95 / V131 not-in-deck / V67ar/V67ao/V149. Then ONE emit: BASE (+150 deploy-grade; +5500 activate under the V97 scope, K&D + AFA excluded, strictly above V168 +5000) + TIER (LOCATION 1500/1400/1300/1200 by source cat via isLocationPull = V67l list ∪ V82 regex ∪ "planet"/"sector"; WEAPON 600; DEVICE 400) + CONTEXT (+50 dl, +25 chars-in-hand DEPLOY) — clamp 1750/7100. V131 Tier-2 → STRUCTURAL downgrade (all positives suppressed incl. V67ak, emit -200). P1 STAND-DOWN (ruling): reserve<=3 AND DecisionContext.isBattlePlausibleThisTurn() (the SAME V61c shared predicate) → activate base stands down to deploy grade.
- DeployEvaluator double-count side killed, both bots (commented in place): V60 +100 baseline, V67ai Tier 1-3 DE copy (ds-2 — ATE copy behind V131 is the single owner; V67i detection kept live as predicate for weapon-gate routing), V67am +600 grant. V162/V67ai-Tier4-HAND (+1900 hand-location anchor) untouched — pull totals stay below it (V179 lesson). All DE vetoes stay.
- CHOSENONE: same build (spec open-Q3 resolved by orchestrator), minus the P1 stand-down — no V61c / no isBattlePlausibleThisTurn there (noted in-file).
- Boundary rows re-verified against final code: 1a pull 5500 > V168 5000 (flip, feedback_pull_before_activate restored); 1b obj download 7050 > 5000 (preserved); 2 hand-loc 1950 > pull 1625-1775 > chars ~550 (order restored); 3 downgraded -150 < Pass (activate window; DEPLOY-window residual: DE V38.4 urgency is uniform on deploy actions and can lift a downgraded pull above Pass when hand >= 9 and nothing else is deployable — bounded corner, follow-up); 4 reserve<=2 -9999 flat; 5 V95 -2000 flat (was net +100, fired); 6 blaster pull +800 single-counted (was +1600).
- P2 corridor audits: ACTIVATE ACTION_CHOICE corridor holds — >=2000 positives are exactly V168 +5000 / V192 5500-7050 / V60 transit +20000 (intended dominance); V79/V29.15/V61c-confirm are other decision surfaces. DEPLOY corridor: generic char stacks ~250-550+urgency < pull tier; playbook corners (V52 T1, V52b, V30, V67ak) stack to ~1650-2350 and can edge unknown-source (+1200) pulls; V67ak +800 rides on top of pull emits in the rare loc+key-char multi-clause corner (max 2525/7850). Pre-existing relationships, flagged for playtest.
- Verified: liveness (no if(false)) at every edit site pre-edit; brace balance clean ×4 files; grep-zero live absorbed emitters in both bots; TDIGWATT admiral/general V29.7 nudge untouched. Compile deferred to orchestrator.
- Revert: un-comment the 2026-07-06-tagged blocks (V116/V95/V97/V100/V82/V67ai/V67am/V29.7 in both ATEs; V60 baseline/V67ai tiers/V67am grant in both DEs), delete the V192 SINGLE EMIT + P1 stand-down + widened trigger (old trigger/dispatch commented in place).

  ════ V189 (2026-07-04) ════

  V189 — NET-VALUE DRAIN GATE (never pay more to initiate a drain than it's worth)
    Source: ActionTextEvaluator.java evaluateForceDrain (rando + chosenone),
            immediately after the V24.15 zero-drain check.
    Steve, game 20jqtseod148of4y (2026-07-04): "He should not have paid to
    drain for 1 with battle plan or battle order on the board." With asdf's
    Battle Plan on table, Rando used 3 Force then drained 1 at Audience
    Chamber, TWICE. Two offenders: the untagged "no deployables — drain is our
    only pressure" +70 boost (returns before any net math) and V140's false
    "Battle Plan on table = universal waiver" (+60; Battle Plan actually
    imposes its OWN 3-Force drain tax, Card8_035).
    Fix: query getInitiateForceDrainCost(gameState, location, playerId) — the
    identical query the engine charges via PayInitiateForceDrainCostEffect,
    summing every INITIATE_FORCE_DRAIN_COST modifier, no card-name hardcoding.
    Cost > drain → -2000 + early return (skips the +70 boost / V140 / V52 /
    V29.9 — the offenders). Boundary: cost-0 games untouched; net 0 (pay 3
    drain 3) allowed — permanent Life Force damage for recycling Force; pay-3-
    drain-2 blocked, deliberately SUPERSEDING V52's old "net -1 marginal but
    worth it" stance. Blocked drain = exactly -2000, loses to Pass (+5).
    V104 (drain ≤ 1 under BO) becomes a redundant backstop fronted by V189.
    V140 UPDATED in place same session: detection replaced with the same
    engine query (old hand-rolled scan commented out); its reasoning string
    now claims only what the engine confirms ("engine initiate-cost is 0").
    Known remaining gap (pre-existing): the untagged forceAvailable<3 branch
    still -50s genuinely free drains before V140 runs — under-drain direction,
    fail-safe, separate item.
    UPDATED 2026-07-06 in place (Steve, 2026-07-04): "We should still allow
    drain 2 for 3 force if there is enough force to deploy and move everything
    that rando wants to do that turn." Two tiers: net <= -2 (pay 3 drain 1,
    the actual 07-04 offender) stays flat-blocked; net -1 (pay 3 drain 2) is
    budget-gated — allowed iff forcePile - initCost >= sum(live deployable
    hand costs, persona-dead excluded) + 2 move allowance. Drains are CONTROL
    phase, so the budget is a forecast recomputed from live gameState at every
    drain decision ("recheck on every spend" by construction). The literal
    "prime DeployPhasePlanner after activation" was rejected with evidence:
    the planner caches per turn, and a Control-time plan would be budgeted
    with pre-drain Force (07-04 log: drain paid 17:16:50, plan created
    17:16:57 with force=9 not 12), feeding the V38.4 hold-back machinery a
    stale plan. V52's old "net -1 marginal but worth it" stance is RESTORED
    only while the turn plan stays funded. Boundary: 12F/no-plan drain-2-
    cost-3 fires (+70); 12F/5-plan fires (ceiling ~+340); 6F/5-plan blocked
    (-2000); turns 1-2 under BO an allowed net -1 scores V48 -50 + multi-
    drain +200 = +150 and fires (pre-V189 behavior, intended). Over-allow
    gaps (bounded): Effects/weapons/devices/pull costs and opponent reacts
    uncounted; budget-loop exception fails open at logger.debug. Grep:
    "V189 NET -1 DRAIN ALLOWED" / "V189 DRAIN NET-1 BUDGET BLOCK".

  ════ V51/V105/V112/V117 (in-place UPDATE 2026-07-06) ════

  Battle Order 4th-slot deadlock + occupation-predicate unification.
    Verge of Greatness game (replay unli50oa1ur8bdux, 2026-07-06): Rando
    controlled Scarif (battleground SYSTEM) + Scarif battleground sites, drained
    there 70+ times, qualified for his own Battle Order (dark Effect 13_54, a K&D
    defensive shield taxing the OPPONENT's drains +3 while Rando occupies both
    theaters) all game — and never deployed it.
    Two faults: (1) prefers4thSlot returned "Battle Order" all game (occupation
    true), and the V105/V107 4th-slot code hard-blocked every OTHER offered shield
    at -5000 (2760 fires) to force a Battle Order that was never in the candidate
    list — 4th slot deployed nothing (prefer-an-unavailable-card deadlock). (2)
    latent: the three occupation checks (V51 deploy, V51 shield, V112) hand-rolled
    hasBGSite/hasBGSystem via getCardsAtLocation + owner-match, disagreeing with
    V105's power-based scan (same detection-mismatch class as the V140 fix).
    Fix (both bots, CardSelectionEvaluator, in place, no new V-tag): two helpers.
    occupiesBothTheaters(game, pid) = canSpot(and(occupies(pid), battleground_site))
    && ...battleground_system — the engine's own OccupiesCondition, exactly what
    Card13_054 checks, so gate and card can never disagree; replaces the 3 hand
    loops (commented out in place). preferredShieldInCandidates(context, title)
    scans the decision's full candidate list; V105 + V117 now only pursue/hard-block
    for the preferred 4th-slot card when it is actually on the menu, else HOLD the
    slot closed (Steve's closed-by-default 4th slot; his call 2026-07-06 was "just
    stop the spam", not fall to a random shield). Kills the 2760-fire deadlock.
    Boundary: no magnitude changes (only the booleans feeding +2000/-5000/-9999
    change). Occupy-not-control (matches the card). Known unmodeled edge (over-
    deploy, not self-tax): the helper omits Card13_054's battlePlanOnTable waiver.
    UNVERIFIED until self-play shows Battle Order actually deploying when offered +
    qualifying (K&D's 4x/game shield-play cap is a separate limiter).
    EARLY-DEPLOY EXTENSION (2026-07-06, Steve: "Battle Order can be deployed
    during turn 1 or 2 as well if a battleground site and system is occupied by
    Rando... just have to occupy, does not matter if opponent also occupies"):
    the base fix un-blocks Battle Order when qualifying but it still scored only
    80 (SITUATIONAL_HIGH untriggered) outside the turn-3 4th slot, losing to
    auto-play shields (200). Both V51 branches (shield-selection + deploy path,
    both bots) now add +200 when occupiesBothTheaters is true → 280, beating the
    auto-plays so it deploys within turn 1-2 pacing. GUARDED on shieldScore > -50
    so V43 redundancy (-100), pacing (-50) and not-played (-100) still win; 4th
    slot unchanged (V105 +2000 still dominates, +200 rides on top). Occupy-only
    (no opponent clause per Steve). Title tests extended to "battle plan" (light
    twin). Known gap: the evaluateUnknown/V112 route (mixed K&D pile) has no
    positive branch, so the boost only fires on the primary shield-selection +
    deploy routes. Self-play still owed.

  ════ V190 (2026-07-04) ════

  V190 — STARSHIPS DEPLOY TO SYSTEMS, NOT DOCKING BAYS (pull gate + destination widening)
    Source: DeckOracle.java (both bots): NEW reservePullFetchesOnlyStarships +
            static spaceLocationOnTable. DeployEvaluator.java (both bots): gate
            in the V67h WILL_SUCCEED branch (rando: after the V185 gate).
            CardSelectionEvaluator.java (both bots): the old untagged -1500
            docking-bay destination rule WIDENED in place to all SITEs.
    Steve, game 20jqtseod148of4y (2026-07-04): "He should not have deployed
    starships to a docking bay. Only deploy starships to systems." Court Of
    The Vile Gangster's pull fetched Elis In Hinthra (later Dengar In
    Punishing One) and parked them at Executor: Docking Bay at 0 power — and
    the 4 Force it burned starved the V29 PAIRED buddy deploy (Wooof never
    joined Greedo at the Great Pit).
    Fix, two layers sharing the ONE-predicate discipline:
    (1) PULL GATE: when every fetchable Reserve target left for a pull is a
        STARSHIP and no SYSTEM/SECTOR is on table, block the pull (-12000 +
        continue; lands ~-6400 after skipped downstream bonuses, vs the
        observed +9380 offending stack — dominated). Target resolution:
        docking-bay targets match docking-bay TITLES only (the V82.3
        'docking bay'→LOCATION category fallback let the SYSTEM Nal Hutta
        satisfy a docking-bay check — not repeated); type-words by category;
        exact phrase by title; NO last-word fallback (parser junk resolves to
        nothing). Turns 1-2 of the same deck unaffected (docking bays still in
        Reserve → LOCATION fetch → helper false → pull fires, correct).
    (2) DESTINATION: -1500 now covers ALL sites (was title-matched docking
        bays only; the dead -10 "STARSHIP TO GROUND" branch commented out).
    Known limits, deliberate: a space location the ship can't legally deploy
    to still stands the gate down (v2: icons/ownership); the gate does not
    itself make Rando deploy the system first (the "Deploy Nal Hutta" action
    is unclassified by the DPS walk — follow-up item); SECTORS count as space
    and are unpenalized pending Steve's ruling; a deck with no system keeps
    its ships in Reserve permanently (consistent with the rule). Vehicles
    untouched. Chosenone mirrored (note: chosenone still has NO V185 block —
    pre-existing mirror debt, not ported here).

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

  ════ V156 UPDATE (2026-07-07): weak-band hold on ALL turns + NEW JOIN-GROUP move arm ════

  V156 UPDATE — ABILITY<4 SOLO HOLD NO LONGER EXPIRES AFTER TURN 2; MOVE-SIDE JOIN-GROUP ARM ADDED
    Source: CharacterDeploySiteEvaluator.java (gate ~:563, new shared helper isV156FlipNotReady),
    MoveEvaluator.java (V32 SOLO ESCAPE branch) + CardSelectionEvaluator.java
    (evaluateMoveDestination) in BOTH rando and chosenone. Full entry in AI_CHANGELOG.md.
    Incident (2026-07-07 game, Rando DARK Verge vs asdf): turn 2 the hold correctly -600'd Tagge
    and Death Trooper at every empty battleground and both joined Vader. Turn 3 the SAME deploy
    sailed through — the turn<=2 gate was the ONLY stopper: Baron Soontir Fel (ability 3, power 2)
    went solo to Scarif: Beach (+1065; V136 SectionA +500 uncontested reward, only V38 -150 and a
    self-cancelling V113 -300 against). The move ladder then claimed the fix (V31 R2 DOCTRINE) but
    V41 WRONG DIRECTION -9999'd the only join target because its "empty" counts only OPPONENTS —
    consolidating onto Vader's own stack read as wrong direction. V160 broke the cancel loop and
    Fel rotted at Beach until battled + forfeited. Audit rows deploy-siting-1/-2 (confirmed, high)
    named this exact deploy/move contradiction.
    DEPLOY arm (in place): gate is now (currentTurn <= 2 || (ability < 4 && buddy plan exists)).
    Weak band holds -600 on ALL turns at uncontested BG sites when another character is on table
    or affordable in hand; no buddy anywhere -> the lone body still deploys. The ability>=4 class
    keeps turn<=2 + all exemptions (ability>=6, armed-5, flip carve-back) untouched.
    MOVE arm (new, both surfaces): MoveEvaluator claims R2 DOCTRINE "V156 JOIN-GROUP" (fine +250
    passes L2; NON-battle-seeking so the V137 canWinAt veto never applies; R3/R4 still outrank)
    for a weak solo at an uncontested site with an adjacent friendly stack. CardSelectionEvaluator
    JOIN-GROUP MODE (V169 retreat-mode pattern): friendly-stack destinations +250 +50/extra body
    (cap +400), and V41 WRONG DIRECTION gated off for them (new exemption beside V169/V67z).
    Exempt everywhere: undercover spies (V170 parked spies sit), opponent presence at the solo's
    site, and a solo doing READY objective work at a flip-relevant site (isV156FlipNotReady —
    the old inline Verge scan extracted verbatim to a public static, shared by all arms).
    Boundary: deploy replay 1065 -> -35 < Tower 615 (joins the stack = turn-2 behavior); turn<=2
    byte-identical; destination replay -10151.5 -> +197.5 (> -100 BAD_ACTION floor -> executes);
    action ~+5380 > Pass 6; R2 6000 < R3 12000 < R4 20000; retreat mode mutually exclusive.
    Status: typed javac compile clean in the app container (no mvn per house rules).
    PENDING: rebuild + live game (grep "V156 JOIN-GROUP" / turn-3+ "V156 SOLO HOLD").

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
    actually drain at the destination next turn. Originally -9999 when
    forcePile < 5 during MOVE; T4.1 raised the live contribution to the
    current -100000 ladder-veto class on 2026-07-06.
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

  ════ V79 family UPDATE + V79b FLIP-BACK GUARD (2026-07-07): Verge post-flip — Death Star STAYS in Scarif orbit ════

  V79/V79b — POST-FLIP ORBIT TOGGLE: THE AT-SCARIF CHECK WAS READING A FIELD THAT IS ALWAYS NULL
    Source game: Game9f3c46b00681 (Rando DS Verge, conceded T5 down 39-9 lost-pile).
    Root cause: getAtLocation() is ALWAYS null for the Death Star mobile-system LOCATION card, so
    every "DS at Scarif" check in the V79 family (6 copies across MoveEvaluator, ActionTextEvaluator,
    DeployEvaluator, ForceReserveService, CharacterDeploySiteEvaluator.isV156FlipNotReady, both bots)
    was permanently false. Post-flip the V79 +500 default kept the hyperspeed move top-ranked forever,
    and V79b's closest-to-7 answer FROM ORBIT is the deep-space EXIT (the engine excludes the orbited
    system from re-orbit at the chosen parsec — MoveMobileSystemUsingHyperspeedAction:82). The DS
    toggled out of/into orbit turns 3-5; V35.4 +150 misfired onto the system move; a perpetual
    1-Force VERGE MOVE RESERVE suppressed a real Mara Jade deploy.
    Fix (all V-tags updated in place, no new tag): all six at-Scarif checks → getSystemOrbited()
    (the engine's own orbit primitive; the flip condition itself is Filters.isOrbiting(Title.Scarif),
    Card216_011:122); old getAtLocation lines commented in place. NEW V79b FLIP-BACK GUARD in both
    MoveEvaluators: objective FLIPPED + DS orbiting Scarif → T4.1 ladder HARD VETO (-100000) on the
    hyperspeed move. RandoCalAi V79b belt-and-suspenders: flipped + orbiting → prefer orbit/Scarif
    option, else answer the CURRENT parsec (stay). ATE V35.4 skips mobile-system LOCATION movers.
    Card-text note: this objective's printed flip-back is LEADER-based (Card216_011_BACK), so the
    toggle never unflipped it — the guard is V22.2 protection-class (keeps the flipped side's
    'system it orbits' battle-destiny umbrella; an orbit-based flip-back objective would lose outright).
    Boundary: post-flip+orbiting = veto -100000 vs Pass +28; pre-flip+orbiting ≈ -110 vs Pass +28
    (Pass wins, DS holds); pre-flip transit + post-flip deep-space recovery steering UNCHANGED.
    Status: UNCOMMITTED in the working tree; compile + live Verge game PENDING (expect
    'V79b FLIP-BACK GUARD ... VETOED' each post-flip move phase and NO 'V79 VERGE MOVE RESERVE'
    while orbiting). Full details in resources/AI_CHANGELOG.md 2026-07-07 entry.

  ════ OBJECTIVE-LOGIC CONSOLIDATION (2026-07-07): identity → ObjectiveAnalyzer (pure REFACTOR) ════
    NOT a new V-tag. Consolidates existing V86/V121 (Invasion), V83/V88/V108/V110/V109 (My Lord),
    V186 (I Want That Map). Objective-IDENTITY detection each branch re-matched inline now lives on
    ObjectiveAnalyzer as title-derived getters isInvasion()/isMyLord()/isWantThatMap() (set in
    analyze() before the flip-parse early return, reset in reset()), plus typed IWTM steer slots
    getIwtmSystemBpIds()={208_51,208_051} / getIwtmSystemTitleFragment()="starkiller base" /
    getIwtmPreferredStartingEffect()="the first order was just the beginning". Deploy + CardSelection
    branches read the getters; scoring bodies UNMOVED (additive-order preserved). V99 (both bots,
    Deploy+CardSel) left DELIBERATELY ungated. Getter values byte-identical to the old inline checks.
    Codex/Alfred review PASS on all three objectives (no score/ordering/branch drift). Compiles clean
    both bots. SEPARATE companion commit: chosenone full-clone from rando (erase 7-file mirror drift;
    entry points RandoCalAi/TheChosenOneAi untouched). Full details in resources/AI_CHANGELOG.md
    2026-07-07 entries.

  ════ OBJECTIVE PLAYBOOK PILOT (2026-07-07): My Lord weights → typed analyzer-owned playbook ════
    NOT a new V-tag. First analyzer-owned ObjectivePlaybook per Steve's ruling (ObjectiveAnalyzer owns
    objective identity + typed facts + scoring weights; evaluators consume at existing call sites).
    ObjectiveAnalyzer gains nested types NamedCardRef / ObjectiveWeights / ObjectivePlaybook + static
    MY_LORD_PLAYBOOK (identity 12_179/_BACK; keyCharacter Filters.senator; keySite Filters.Galactic_Senate;
    weights V88 +1500 / V83 -2000 / V108 +500 / V110 -2000) + activePlaybook field + getActivePlaybook().
    The four My Lord deploy magnitudes in getDeployObjectiveAdjustments now read MY_LORD_PLAYBOOK.weights.*
    instead of inline literals. Behavior byte-identical (weights == old literals; V99/V86 untouched).
    Facts from Codex batch resources/Objective_Playbook_Facts_2026-07-07.json, source-verified (My Lord=GO).
    Compiles both bots; chosenone mirrored. Full details in resources/AI_CHANGELOG.md 2026-07-07 entry.

  ════ PHASE-REORG BATCH 1.5 (2026-07-12): dead-code purge — CSE + DE, both bots, NO behavior ════
    NOT a new V-tag. Second deletion wave under Steve's migration ruling (byte-verified backup
    gemp-swccg-public-backup-20260712-221311 + git history = undo). Deleted ONLY compiled-out
    (`if (false ...)`) or fully-commented code: CSE all 11 V159-superseded taped-off branches,
    V127 + V29.8 commented corpses, the V37/V139 dead block incl. its nested trapped V21 copy
    (redundant — 4 live V21 call sites survive at ~4370/4500/4633/4962); V67t dead if-arm removed,
    live else body (zero-forfeit −80) now unconditional, behavior-identical. DE: V33/V67aq/V115
    corpse, V67aj + nested V67al if(false) (the dead block a prior K-2 lost a day on), V90 if(false).
    Artifacts: game_log2/game_log_latest, CSE .bak/.v13.backup/.v24.11.fix, .DS_Store x3 removed.
    (2026-07-13: game_log_latest.txt content restored to resources/evidence/game_log_latest.txt —
    the AMN/Rey audits cite it by line number. game_log2.txt uncited, history-only.)
    HELD on danger list: V122 + V67as if(false) (batch-6/7 owners), DE 1424-1794 objective corpse,
    Endor V193 dead block, ObjectiveHandler, ActionAudit (batch 11). CSE 9966→9478, DE 6206→5870
    lines per bot; +10/−498 and +9/−345 each, perfectly mirrored. MVN_EXIT=0 both bots. Tombstones
    at every deletion site. Local commit only — Codex jar byte-parity gate BEFORE deploy (m00222).
    Full details in resources/AI_CHANGELOG.md 2026-07-12 entry.

  ════ PHASE-REORG BATCH 1 (2026-07-12, commit 5ab16f8ac): four live-defect hotfixes, both bots ════
    (Entry added 2026-07-13 per Codex m00225 — it was missing at push time, a bookkeeping violation.)
    1a pull-target "here" suffix strip (ATE pull-route guard, consumer-local); 1b two-weak-solos
    -800 move-dest split (CSE, Chiraneau/Ozzel escape); 1c flip-exemption first-name token;
    1d V28/V47 RESERVE SOLO BLOCK deleted (CSE ~8861; wrong Cloud City facts on forced nodes;
    first deletion under Steve's migration ruling). PROCESS NOTE: deployed BEFORE the independent
    Codex gate; gate returned HOLD (m00225/m00229) — 1a was UNREACHABLE on the real Krennic card
    (side-text blindness), 1c regressed 'Director Orson Krennic', 1b overpenalized power-0
    friendlies. See the BATCH 1 CORRECTIONS entry below. Full details in resources/AI_CHANGELOG.md
    2026-07-12 entry.

  ════ PHASE-REORG BATCH 1 CORRECTIONS (2026-07-13): Codex HOLD m00225/m00229, both bots ════
    NEW single side-aware owner DeckOracle.getSourceCardFullGameText(bp, actingSide): base
    getGameText() + the ACTING player's getLocationDark/LightSideGameText() — location pull text
    (Card216_016 "May [download] Krennic here.", dark side only) was invisible to getGameText()
    readers, leaving the Batch-1a guard unreachable. ALL pull-text consumers rewired: ATE (v177,
    v183, V67h, V95, V131, V82 predicate, pull-route guard, 3 generic), DE (V67h/V67i/V67m),
    DeployPhaseScript, ActionAudit x2. here/there/at-that-location strip CENTRALIZED into
    parseSourceCardPullTargets (ATE-local copy removed). Flip exemption now matches typed
    blueprint Personas (getHumanReadable, word-boundary) against flip text — fixes 'Director
    Orson Krennic'. Batch-1b no-overpenalty gate counts friendly non-undercover characters at
    destination (power<=0 misread a present power-0 friendly as an empty site). Compiles clean both bots
    (MVN_EXIT=0), mirrored. NOT deployed — awaiting Codex re-gate. Full details in
    resources/AI_CHANGELOG.md 2026-07-13 entry.

  ════ B0 TIE-DETERMINISM (2026-07-13, Codex m00228): LinkedHashMap + strict Float.compare ════
    INTENTIONAL fixture-contract delta (the one sanctioned early behavior change): exact-score
    winners previously depended on unspecified HashMap iteration in CombinedEvaluator's merge map.
    Now LinkedHashMap first-seen order + explicit winner loops (DPS bucket walk, final selection)
    with strict Float.compare(candidate,best)>0 — ties keep the earlier candidate, both bots
    identical. Non-tied decisions bit-identical. NOT deployed pending Codex gate. Full details in
    resources/AI_CHANGELOG.md 2026-07-13 entry.

  ════ BATCH 1 CORRECTION 2 (2026-07-13, Codex gate FAIL m00262): title-case grammar strip ════
    The central here/there suffix strip (correction 1) removed terminal Here/There from REAL card
    titles ("I've Got A Problem Here" -> "i've got a problem"). Fix: Decipher writes titles in
    Title Case but the location-forcing adverb lowercase — strip now runs CASE-SENSITIVELY on the
    raw captured text BEFORE lowercasing in parseSourceCardPullTargets. Fixtures added per the
    gate (28 green): DeckOraclePullTargetParseTest x11 in BOTH bot test packages (strip vs title
    preservation, mixed list, persona exemption pos/neg/substring) + FormationSafetyCountTest x6
    (power-0 friendly presence, undercover-only, exclusions). Helpers extracted for pure tests:
    DeckOracle.personaNamedInText, FormationSafety.countFriendlyNonUndercoverCharacters (logic
    unmoved). Record fixes: ATE consumer count 12/bot, "Deployed" wording, wrong power-0 Ozzel
    example dropped (Ozzel printed power 3 — m00225's own example was wrong). Also corrected
    the prior entry's Ozzel mention above (the BATCH 1 CORRECTIONS entry; m00271 wording fix). NOT deployed. Full details in resources/AI_CHANGELOG.md.

  ════ B2 TRACE HOOKS (2026-07-13, Codex m00243 spec): choke-point instrumentation, no-op default ════
    NOT a V-tag. Shared ai/models/common/trace/ (TraceOp/TraceOperation raw-float-bits/DecisionTrace/
    TraceSink/NoOpTraceSink/TraceCollector/TraceSession thread-local, swallow-all guards). Both bots'
    EvaluatedAction records INITIAL/ADD/SET/HARD_VETO as LEGACY_UNTAGGED (+tagged overloads for
    migrated arms); mergeFrom = MERGE boundary only. CombinedEvaluator: package-visible scripted-
    evaluator+sink seam for pure JUnit; core records evaluator binding, first-seen candidate index,
    RANK/SELECT/FINALIZE, synthetic-pass markers. Tie-determinism semantics untouched. 12 tests green
    (6/bot, all six Codex gate cases). RandoCalAi/DecisionSafety hooks = next increment. NOT deployed.
    Full details in resources/AI_CHANGELOG.md 2026-07-13 entry.

  ════ B2 TYPE HARDENING (2026-07-13, Codex m00263 deltas): typed snapshot contract v2 ════
    NOT a V-tag. common/decision types hardened per the B2 increment-1 gate: engine
    AwaitingDecisionType; FactValue-wrapped ObligationFlag set / noPass / min / max / selectable
    (no fabricated defaults); minimal DecisionRoute enum + machine-checked RouteSelectionEvidence;
    sealed ActionRef/CardRef/SourceRef/DestinationRef; exact field names (forcePileSize,
    lifeForceCardCount, friendly/opposingNonUndercoverCharacterCount, basePower+weaponBonus);
    UNKNOWN-never-zero on failed power components; blank/range/version validation. Version 2.
    43 tests green (was 22). Zero production consumers. Builder parity = increment 2. NOT deployed.
    Full details in resources/AI_CHANGELOG.md 2026-07-13 entry.

  ════ CLEANUP BATCH 1.6 (2026-07-13, Codex packet): 194 comment-only lines, both bots ════
    Per CODEX_CLEANUP_BATCH16_CANDIDATES manifest: DeckOracle V185 first-pass comment block,
    DeployPhasePlanner commented V22.3 inline scan (ForceReserveService is the sole live scan),
    ShieldStrategy superseded power-theater scan comments. Comment-only assertions dual-layer
    PASS; bytecode identical; surviving banners rewritten (revert path = git history). Held
    items untouched. NOT deployed. Full details in resources/AI_CHANGELOG.md 2026-07-13 entry.

  ════ CLEANUP BATCH 1.7 (2026-07-13, Codex packet): 226 comment-only lines, both bots ════
    DrawEvaluator (78/bot) + PassEvaluator (35/bot) old inline scans superseded by the live
    ForceReserveService cache. Dual-layer comment assertions PASS; isolated javap normalized
    dumps byte-identical pre/post (module maven blocked by concurrent trace-lane dirty state;
    isolated gate is stronger for comment-only). Surviving comments point at git history.
    NOT deployed. Full details in resources/AI_CHANGELOG.md 2026-07-13 entry.

  ════ TRACE INCREMENT 2 (2026-07-13, Codex V2 oracle contract): typed envelope, capture off ════
    NOT a V-tag. common/trace rewritten to the V2 envelope (schema v2, shadow DecisionSnapshot,
    typed route incl. all five interceptors, raw-vs-merge candidate order, typed ops/rule/domain/
    kind ids, TraceFinalization with pre-safety winner + typed DecisionSafety corrections,
    intended state events, COMPLETE/INCOMPLETE with typed failures — no silent truncation).
    Observation-only hooks in RandoCalAi/TheChosenOneAi/DecisionSafety, zero control-flow change.
    91 tests green. m00290/m00291 closed. Stage 4/5 (inner mutation observation, real-decide
    fixtures) deferred. Capture DISABLED. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ B2 CONSISTENCY GAPS (2026-07-13, Codex m00277): four validation fixes, snapshot types ════
    NOT a V-tag. Builder.turn unset = failure (no fabricated 0); RouteSelectionEvidence carries +
    cross-validates selectedRoute; KNOWN obligation flags checked against KNOWN noPass/minimum;
    CandidateShape tied to actual ActionFacts rows (ghost rows lawful). TraceSnapshots one-line
    adaptation. 97 tests green. Zero consumers. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ CLEANUP BATCH 1.8 (2026-07-13, Codex packet, AMENDED): 24 comment-only lines, both bots ════
    ActionTextEvaluator V35.4 OLD ownership-inverted undercover-spy detection block (12/bot):
    flagged OUR undercover card as the drain-blocker; the live 2026-07-06 scan (opponent-owner
    check before isUndercover) is the sole valid implementation, so the predecessor is not a
    rollback path. Packet author WITHDREW the originally-proposed V192 old take-dispatch line
    mid-implementation (PULL audit: absorbed V192 predecessors stay held as rollback evidence);
    V192 region restored verbatim, byte-identical to pre-batch HEAD. Dual-layer comment-only
    assertions PASS; isolated same-JDK javap normalized dumps identical pre/post both classes.
    NOT deployed. Full details in resources/AI_CHANGELOG.md 2026-07-13 entry.

  ════ TRACE INCREMENT 2b (2026-07-13, Codex m00303): five semantic gaps, snapshot v3 ════
    NOT a V-tag. RawDecision verbatim engine-param capture (presence/present-empty/absent
    distinct); typed-INCOMPLETE on every open/finish/sink failure path (single closeAndEmit
    channel); route-required COMPLETE matrix; mandatory op producer/rule/domain/kind with
    COMBINED_EVALUATOR sentinels; route/wire-shape cross-validation (phase never implies
    route, per amended route map). 112 tests green. Stages 4-5 still open. Capture DISABLED.
    NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ CLEANUP BATCH 1.9 (2026-07-13, Codex packet): MoveEvaluator −28 comment lines, both bots ════
    V169 soft-block / V160 old −9999 / V79 getAtLocation predecessors deleted per manifest
    (ranges hash-verified to the packet's SHA-256). javap identical, 46/46 fixtures green,
    live successors confirmed. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ FINALIZER F0+F3 (2026-07-13, Codex packet m00328): real-engine fixtures + pure shadow ════
    NOT a V-tag. common/finalization: F0 corpus over REAL engine decisions (MC unchecked-AIOOBE
    P0 pinned executable; ARBITRARY locked-preselected red contract; empty-wire truth per type)
    + F3 ResponseFinalizer pure seam (one V148 pass semantic, typed corrections, one recorded
    RNG draw max). Seven engine-verified divergences where legacy strands and shadow's answer
    is engine-accepted. F1/F2 = engine files, HELD FOR STEVE. 137 tests green. NOT deployed.

  ════ CLEANUP BATCH 2.0 (2026-07-13, Codex packet): MoveEvaluator −90 comment lines, both bots ════
    V29 DTF/grabber + V27 maintenance commented predecessor scans deleted (SHA-gated ranges);
    live cache reads + weights untouched; javap identical in detached worktrees; 60/60 fixtures.
    NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ CLEANUP BATCH 2.1 (2026-07-13, Codex packet): MoveEvaluator T4.1 predecessors, −52 total ════
    12 mirrored comment regions (SHA-gated streams both bots), 2 preface corrections. RAW javap
    byte-identical. 60/60 fixtures. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ F3 CORRECTION (2026-07-13, Codex m00336): pass policy governs, typed ForceReason ════
    NOT a V-tag. ResponseFinalizer: Pass judged by policyPassAllowed FIRST (was inert — the
    gate's P0); Acknowledge caged to declared shapes; FinalizedResponse.ForceReason with
    both-direction FORCED invariant on every forced path. 139 green + 1 named skip. Inert.
    NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ CLEANUP BATCH 2.2 (2026-07-13, Codex packet): DeployEvaluator V60 corpse, −4 total ════
    Two commented +100 baseline lines (SHA-gated) + preface sentence rewrite; V192 live owner,
    guards untouched. RAW javap identical. 55/55 fixtures. NOT deployed. Details in AI_CHANGELOG.

  ════ CLEANUP BATCH 2.3 (2026-07-13, Codex packet): the DE objective corpse, −742 both bots ════
    The held DE 1424-1794 commented V83/V110/V108/V86/V88/V99 region deleted (SHA-gated streams;
    method cross-verified against the 2.2 pin). Live ObjectiveAnalyzer call + six arms + weights
    proven untouched. RAW javap identical. 60/60 fixtures. NOT deployed. Details in AI_CHANGELOG.

  ════ CLEANUP BATCH 2.4 (2026-07-13, Codex packet): DE force-economy corpse, −104 total ════
    Four SHA-gated comment ranges + five preface lines per bot. Live MaintenanceFacts/
    ForceReserveService successors + weights proven intact. RAW javap identical (equal to the
    2.3 gate record). 60/60 fixtures. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ CLEANUP BATCH 2.5 (2026-07-13, Codex packet): DE V38/V53 diagnostics, −88 net ════
    Four SHA-gated ranges + 11 preface lines per bot. Live ForceReserveService.Facts
    successors proven. RAW javap identical. 60/60 fixtures. NOT deployed. Details in AI_CHANGELOG.
  ════ STAGE 4A1 (2026-07-13, amended matrix + Option A m00372): typed state events, envelope v3 ════
    NOT a V-tag. Four sealed families (tracker RECORD_RESPONSE w/ complete snapshot seam,
    pending-concede, engine PLAYER_LOST w/ distinct EngineCallOutcome, pending-deploy);
    TraceIntendedStateEvent deleted; SCHEMA_VERSION 3; 10 observation-only hooks/bot; pure
    traceSnapshot()/traceDecisionKey() seams, mutators byte-unchanged; m00380+m00381 gate items
    closed; council dissent on file. 153 tests green. Capture DISABLED. NOT deployed.
  ════ CLEANUP BATCH 2.6 (2026-07-13, Codex packet): DE V67ai/V67am duplicate scorers, −76 net ════
    Two SHA-gated ranges + 9 preface lines per bot. V192 (ActionTextEvaluator) proven sole live
    owner of the location tier + weapon +600; V67i/V67m detection, V67ar/V67ao/V149 vetoes, and
    the V162 hand anchor retained. RAW javap identical. 60/60 fixtures. NOT deployed. Details in
    AI_CHANGELOG.

  ════ CLEANUP BATCH 2.7 (2026-07-13, Codex packet): CSE force-economy predecessors, −12 total ════
    Six SHA-pinned comment lines per bot beside their live MaintenanceFacts successors.
    RAW javap identical. 61/61 fixtures. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ CLEANUP BATCH 2.8 (2026-07-13, Codex packet): ATE V140 corpse, −128 total ════
    64-line OLD-detection block + preface swap (all three SHA pins matched). Live drain-cost
    waiver successor intact. RAW javap identical. 61/61 fixtures. NOT deployed. Details in
    AI_CHANGELOG 2026-07-13.

  ════ FACTUAL COMMENT REPAIR (2026-07-13, Codex packet + m00410): 3 files, +6/−4 ════
    MaintenanceFacts header card-id swap (Blizzard 4 vs Stormtrooper Garrison) + both CSEs'
    Battle Order comment corrected to card truth (3 Force/drain; occupation waives; Battle Plan
    suppresses the modifier). Packet's own wording corrected mid-flight by author. javap
    identical. 61/61 fixtures. Comment deletion STOPS here (24 pairs/840 lines held). NOT deployed.

  ════ TRACE 4A2A (2026-07-13, Codex m00417/m00426): outer tracker lifecycle events ════
    UPDATE_STATE/CLEAR records w/ wrapped lifecycle snapshot (lastPhase excluded by ownership);
    one event per legacy call proven at real decide(); pure seam; mirrors exact. 143 tests green.
    Capture DISABLED. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ TRACE 4A2B (2026-07-13, Codex packet + 6 live corrections): shared tracker events ════
    PHASE_CHANGE/BLOCK_RESPONSE families w/ snapshot-consistency invariants; direct-call-site
    hooks only; failure injection proves all four call/append boundaries execute once (armed TRUE block proven at tracker level only, per m00465);
    COMBINED_EVALUATOR route fixture; mutator bytecode equality verified. 198 green + 1 skip.
    Capture DISABLED. NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ TRACE 4B1 (2026-07-13, Codex released packet): heuristic memory observation ════
    Canonical HeuristicMemorySnapshot + six closed families; six direct owner hooks; suppression
    vs NO_OP law; insert-only guarded diff; 842 module tests green; parity OK. Capture DISABLED.
    NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ F1 (2026-07-13, ENGINE, Steve-approved): MultipleChoice checked ordinal bounds ════
    Range check before indexing; checked DecisionResultInvalidException replaces the escaping
    AIOOBE; red contract test flipped green; old defect pin converted (m00431). 6+8 tests green.
    NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ F2 (2026-07-13, ENGINE, Steve-approved): mediator AI retry/clock/visible terminal ════
    One retry then visible failure (object-identity budget); AI clock credited on success;
    human path untouched. 5/5 + 128 combined green. NOT deployed. Details in AI_CHANGELOG.

  ════ TRACE 4B2 (2026-07-13, Codex released packet): StrategyController observation ════
    21-field canonical snapshot + six operation records (two-family) + seven lexical hooks/bot
    (onBattleResult win/loss split); read-only bridges; setUnderBattleOrderRules folded. 58
    focused + 883 module green; seven-method javap identity; parity faithful. Capture DISABLED.
    NOT deployed. Details in AI_CHANGELOG 2026-07-13.

  ════ ACTIVATE+CONTROL OPTION-2 SHADOW (2026-07-13, Codex packet 3a2edded): groundwork, no behavior ════
    DecisionOrigin enum + 5 engine stamps + pure shadow six-route resolver (LEGACY_UNOWNED bypass, no
    production consumer) + ActivateControlRouteInput carrying recipient AND turn-player as distinct fields
    (no trace-schema expansion). clean-tree gate 900 server / 906 reactor (deferred untracked fixtures excluded; K-2 in-tree 909 was contaminated, corrected per Codex m00538). Live cutover/owners/drain/finalizer/deletion
    DEFERRED to Phase B (needs a verification harness, no sandbox). Capture off. NOT deployed.

  ════ EMERGENCY ENGINE RESTORATION (2026-07-14): phase wire removed after player-choice leak ════
    Jyn Erso's Undercover-spy Yes/No prompt exposed internal deploy metadata as extra choices.
    Root cause: DEPLOY commit 494d6f4bc wrote AI provenance into the player-visible awaiting-
    decision params map. Complete src/ restored to ec886934b, the last AI-only boundary before
    F1/F2 and phase engine-wire work. DRAW/PULL/ACTIVATE-CONTROL/OBJECTIVE/DEPLOY/BATTLE cutovers
    are therefore removed from the live source together, while their branches and local backup
    remain preserved. Offline package and logic tests pass; YesNoDecision exposes only Yes/No.
    Permanent boundary: AI work may not modify common, logic, cards, async/client, mediator, or
    player-decision wire code. Details in AI_CHANGELOG 2026-07-14.

  ════ V240 (2026-07-18): shared AI-only SETUP phase policy (both bots) ════
    Added common SetupPolicy + read-only SetupFactsReader and routed both bots' CardSelection,
    ActionText, and real V61 saga interceptor through them. Existing V21/V22/V25/V43/V61/V67o/
    V67q/V80/V126/V186/V187 magnitudes, order, terminal behavior, candidate order, temporary-ID
    handling, and fail-open adapter boundaries are preserved. Duplicate inline scores and option
    scans were deleted. AI-only boundary holds; no GEMP engine, card, wire, or client source changed.
    Verified: focused 24/0/0/0; full reactor 1555/0/0/26; independent parity audit PASS; package
    PASS; server/web jar hashes and artifact contents recorded in AI_CHANGELOG 2026-07-18.
    Runtime reload is pending Docker availability and an empty game hall.

  ════ V241 (2026-07-18): retire unused response-finalizer detour scaffolding ════
    Deleted the isolated common/finalization package, PendingAiIntent, PendingAiIntentStore,
    ResponseFinalizerContractTest, and PendingAiIntentStoreTest after a complete production
    reference audit proved the production classes had no caller outside their own definitions.
    Retained the independent stock-engine contract fixtures/tests, common/decision, and common/trace.
    This is a structural deletion only: no evaluator, score, ordering, interceptor, tracker,
    response wire, engine, card, or client behavior changed. Clean full reactor passed 1528/0/0/26;
    production reference scan and diff checks passed. Runtime reload remains pending.

  ════ V242 (2026-07-18): shared AI-only RESPONSE router (both bots) ════
    Added pure common ResponsePolicy and routed both direct AI adapters through it for V45
    optional-forfeit classification, V44/V67j revert option lookup, V170 Undercover-spy Yes/No
    lookup and drain boundary, and duplicated fallback priority-card scoring. Adapters retain
    game reads, logs, traces, terminal returns, and wire responses. V61 SETUP, Rando-only V79b,
    HeuristicAiBase, deep phase policies, and unknown legacy response shapes are unchanged.
    Exact legacy order, fallbacks, and +100/+80/Sense-or-35 scores are preserved. Focused
    10/0/0/0 and clean full reactor 1538/0/0/26 passed; clean async package, source parity,
    forbidden-symbol, and artifact-content gates passed. Runtime reload remains pending.

  ════ V243 (2026-07-19): ownership-audit tooling and retired-comment cleanup ════
    Replaced only the inert V95, V97, and V100 executable comment blocks in both ActionTextEvaluator
    mirrors with one-line breadcrumbs to their shared PULL owners, and removed one stale commented
    dispatch line. Executable Java is unchanged after comments are stripped. Replaced the stale
    fixed-range manifest refresher with comment-aware owner-audit/version builders and a reproducible
    comments-only source gate. Retained legacy scorers remain visibly marked for migration instead of
    being certified as extracted owners. START-OF-TURN and END-OF-TURN remain named empty metadata
    slots, not engine phases or invented policy classes. Focused PULL 20/0/0/0, clean full reactor
    1538/0/0/26, clean async package, source allowlist, forbidden-symbol, and packaged-class absence
    gates passed. The source audit reports 169 retained legacy-owner candidates and 2 section
    fallbacks. Workbook emission and runtime reload remain pending on their external runtimes.

  ════ V244 (2026-07-19): consolidate MOVE V169 retreat retries and destination safety ════
    MoveBlockedResponsePolicy now owns V169's per-turn/per-action retry ledger and exact first-three
    -250 soft blocks versus fourth-and-later -100000 hard veto. MoveDestinationPolicy now owns
    retreat-mode classification, the +600 safe-destination contribution, and the V41 retreat
    exemption. Both ActionTextEvaluator and CardSelectionEvaluator mirrors retain all blocked-set,
    mover, game-state, power, action, logging, and control-flow responsibilities while duplicate
    score and retry mechanics leave the adapters. Exact attempt boundaries, turn reset, action key,
    endangered fallthrough, opponent-power == 0 safety test, candidate order, and all adjacent
    V163/V167/V156/V67z/ladder behavior are preserved. Focused V169 47/0/0/0, complete MOVE-named
    suite 360/0/0/0, full reactor 1544/0/0/26, normalized mirror parity, AI-only boundary, forbidden-
    symbol scan, and clean package passed. Independent review found no correctness issues. Packaged
    server jar SHA-256 194d6640b71b57e92a1242e5f322f29b76f973011415f5e9b5e43dea9a1cb21a and
    web jar SHA-256 90fb6b7b115e0e2eba04c144a6386f9dffc85bda1a54339a050b0ccb243b4bc3; all
    nine changed policy/adapter classes are present in web.jar; exact live mirror byte parity passed.
    Runtime reload remains pending on the external runtime. AI source only; no GEMP engine or
    player-decision changes.

  ════ V245 (2026-07-19): consolidate MOVE destination scoring and veto order ════
    MoveDestinationPolicy now owns the coupled CardSelection destination tree: V67au retreat-to-
    drain; V64/V65 Hidden Path power safety; V67aa pre-flip suicide; V41/V67f2 spy-aware contest;
    V65a/V65b threat exemptions; V67z/V169/V156 wrong-direction priority; and the independent V41
    Castle veto. Both adapters retain decision-text parsing, first physical-card lookup, objective
    state, printed and engine power reads, undercover/Jedi scans, logging, action mutation, candidate
    order, V67aa's terminal append/continue, and every surrounding scorer. Verification exposed and
    fixes one dormant AI bug: V67au's invalid `%0.f` format tokens threw after qualification and
    silently discarded the documented +400; valid `%.0f` tokens now allow that score to apply. Other
    behavior remains unchanged. Exact oddities remain:
    V41 is not Hunt Down-gated; V64/V67aa do not verify the mover is Jedi; printed and engine power
    facts are not normalized; V67au uses own-side icons; V67aa remains after V64; and all documented
    additive stacks remain possible. V67f1 passenger logic remains in MoveLandingPolicy. Focused
    destination 42/0/0/0, MOVE-named 371/0/0/0, full reactor 1555/0/0/26, normalized mirror parity,
    AI-only boundary, forbidden-symbol scan, and clean package passed. Server jar SHA-256
    ae57f654159f1f516b1f4d87efbd444ce8963ed7303a5282f81bd73c03da0fe4 and web jar SHA-256
    b9b2aa8d11192d6a3aa7ca2b7d9414afdcc7c0000d6ffc7eef7439b3b943911c contain the shared
    policy, all nested decision types, and both adapters. Independent review found no correctness
    issues; exact live mirror byte parity passed. Runtime reload remains pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V246 (2026-07-19): consolidate MOVE drain pressure and transit routing ════
    MoveDrainRoutingPolicy now owns the pure CardSelection scoring decisions for V166 MOVE drain
    contest, V67e drain potential, the V67n/V67g destination-drain branch, and V67g move-from-drain.
    MoveTransitPolicy owns V67k's narrow case-insensitive Underground Corridor title classifier.
    Both bot adapters retain printed-power and icon reads, engine drain and battleground reads,
    bonus-aware net-drain aggregation, opponent-card counting, decision-text parsing, first matching
    physical-card resolution, exception boundaries, exact logs, action mutation, and legacy order
    after V169/V156 and before V67au. Behavior remains structural-only: V166 still scores
    +350/+300/+250/+200 for 1/2/3/4+ cards behind its exact printed-power, engine-drain, and net>=2
    gates; V67e still uses printed opponent icons plus the engine battleground multiplier; V67k/V67n
    remain title-only; ordinary zero drain remains -200; and move-from-drain remains -250 per printed-
    icon drop with blueprint-first mover resolution. Existing additive mismatches and fail-open catches
    remain unchanged. Focused 46/0/0/0, MOVE-named 385/0/0/0, full reactor 1569/0/0/26,
    normalized mirror parity, AI-only boundary, forbidden-symbol scan, and clean package passed.
    Server jar SHA-256 de7b0dacaf88baf0e229eae6f5f0c599ba0b7a8694f49c5b8e0e309a5d4e2bbe and
    web jar SHA-256 4a78c1c27531d5f8c879f1b873dd89413c45622a607869e62e63eea736540586 contain the
    shared policies, nested branch types, and both adapters. Independent review found no remaining
    correctness issues. Runtime reload and live-game proof remain pending on the external runtime.
    AI source only; no GEMP engine or player-decision changes.

  ════ V247 (2026-07-19): consolidate MOVE ActionText capacity and transport gates ════
    MoveTransitPolicy now owns V87's exact pilot/passenger capacity-slot swap classifier, -3000
    contribution, and reason. MoveForceEconomyPolicy now owns V134 Odin action classification,
    V141 named and three-marker game-text transport classification, and the exact 5-Force, 4-Force,
    empty-Reserve thresholds, reasons, and -100000/-2000 contributions. Both ActionTextEvaluator
    mirrors retain phase/card gates, card and blueprint reads, lazy Force/Reserve reads, exception
    boundaries, score mutation, logging, V87's terminal append/continue, and V134-before-V141 order.
    Behavior remains structural-only. V87 pilot can still stack MoveEvaluator's +100/+50 to net
    -2850; V134 remains dominant against the current ladder ceiling; V141 intentionally remains an
    additive -2000 rather than a true veto; low Force still wins its reason precedence; and Odin's
    actual three-marker game text still allows the legacy V134+V141 stack. Focused 52/0/0/0,
    MOVE-named 396/0/0/0, full reactor 1580/0/0/26, normalized mirror parity, AI-only boundary,
    forbidden-symbol scan, and clean package passed. Independent review found no behavior drift or
    blocking test gaps. Server jar SHA-256
    5bed23372867e7b9b34894aaeeb6fac00690d627570253c76d159b37db1a6ae6 and web jar SHA-256
    03c292b4aac056204354b1b3876e42799aef18d33d8c5fe07f0bee8a4aca39dc contain the
    shared policies, nested gate types, and both adapters. V67ae remains separate because its icon,
    system, weapon, and retreat-exemption reads have different failure boundaries. Runtime reload and
    live-game proof remain pending on the external runtime. AI source only; no GEMP engine or
    player-decision changes.

  ════ V248 (2026-07-19): consolidate MOVE V67ae move-to-here drain guard ════
    MoveDrainRoutingPolicy now owns V67ae's exact action-text classifier and final nonzero-drain,
    hopeless-retreat, and zero-drain scoring branches. Both ActionTextEvaluator mirrors retain the
    destination-site lookup, icon reads, same-system location scan, engine power reads, weapon
    estimates, score mutation, logs, neutral append, and all failure boundaries. Behavior remains
    structural-only: icon-read failure still defaults to zero; weapon failure keeps raw power;
    exemption-scan failure still applies the -300 penalty; outer failure still skips it; the doomed
    threshold remains >= 6; and the score remains independently additive. Focused 34/0/0/0,
    MOVE-named 403/0/0/0, full reactor 1587/0/0/26, normalized mirror parity, AI-only boundary,
    forbidden-symbol scan, and clean package passed. Independent review found no behavior drift,
    ordering change, failure-boundary change, or blocking test gap. Server jar SHA-256
    ce8ce4fbf35d436fd4bc2bab8c7ea3bd468825e9103265c966907f3d1a47a8dc and web jar SHA-256
    80774089bcc52a0451938effb422361b758e76d99302a7ce5430647eefb022ff contain the shared
    policy, nested branch/result types, and both adapters. Runtime reload and live-game proof remain
    pending on the external runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V249 (2026-07-19): consolidate MOVE V60 positive Hidden Path transit ════
    MoveTransitPolicy now owns V60's exact positive game-text transit matcher and the Hidden Path
    +20000 versus generic +200 contributions and reasons. Both ActionTextEvaluator mirrors retain
    the bot-specific objective-analyzer read, branch order after rack handling and before PULL,
    direct score mutation, Hidden Path-only log, and legacy UNKNOWN action type. The broad matcher,
    null/unanalyzed/non-Hidden-Path +200 fallback, and no-MoveEvaluator behavior remain unchanged.
    The R4 score remains 4650 above the documented strongest R3 stack. Focused 35/0/0/0,
    MOVE-named 405/0/0/0, full reactor 1589/0/0/26, normalized mirror parity, AI-only boundary,
    forbidden-symbol scan, and clean package passed. Independent review found no behavior drift.
    Server jar SHA-256 d7fe6a4369fac3cd0a7c8db5e6ae17c7646bf0a43289b081168bc8ee4a6e170f and web jar
    SHA-256 9a34ace2faac102fd57841ff171691884d441e47fcef9b83e712ab209c1dc195 contain the
    shared policy and both adapters. Runtime reload and live-game proof remain pending on the
    external runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V250 (2026-07-19): consolidate MOVE blocked-drain escape and Vader Castle retreat ════
    MoveDrainRoutingPolicy now owns the pure V35.4 blocked-drain escape mover gate, first-match
    +250 Undercover-spy versus +150 ordinary-enemy contribution, and the adjacent V29.7
    Vader/Castle/Mustafar classifier and -300 retreat contribution. Both ActionTextEvaluator mirrors
    retain movement-action gating, mover resolution, global-location fallback, all game and icon
    reads, first-match scans, score mutation, logs, exception boundaries, and V35.4-before-V29.7
    order. Behavior remains structural-only. Undercover and LOCATION movers still skip the escape
    scan; unresolved mover ids still use the global scan; first friendly occupancy with opponent
    presence still wins. V29.7 remains independently additive, stops on the first owned in-play
    Vader, contributes zero at Mustafar before icon reads, and otherwise contributes -300 only with
    opponent icons. Legacy combined totals remain -50 for a spy and -150 for an ordinary enemy. The
    local V35.4 arm remains distinct from the Elis Helrot V35.4-versus-abandon conflict, and V29.7
    remains distinct from the V38.3 Castle hard veto. A stale local comment was corrected without an
    executable change. Focused 43/0/0/0, MOVE-named 414/0/0/0, full reactor 1598/0/0/26,
    normalized mirror parity, AI-only boundary, forbidden-symbol scan, and clean package passed.
    Independent audit found no behavior drift or blocking test gap. Server jar SHA-256
    aef4597a360c6b4efea34fa819d678152f54fee0f103db0e558db3f02b1c7f92 and web jar SHA-256
    31b004bb566d772406dee9b1a9d6a96d8026c9370273e4b883036edbf0f32042 contain the shared
    policy, BlockedDrainEscape result type, and both adapters. Runtime reload and live-game proof
    remain pending on the external runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V251 (2026-07-19): complete MOVE ladder and opportunity ownership ════
    New MoveLadderPolicy owns T4.1's R4/R3/R2 bands, R2 claim-strength gate, band math,
    veto matrix, fine clamp/demotion sequence, rank bases, exact reasons, and default R1 penalty.
    MoveOpportunityPolicy now also owns the final strong/weak attack and successful/failed spread
    contributions, including the legacy +15 weak attack and -10 failed spread. Both MoveEvaluator
    mirrors retain per-action state, rule-specific claim sites, engine reads, adjacency checks,
    one-time-per-bot logging, action mutation, and original branch order. The unused
    VERY_GOOD_DELTA and VERY_BAD_DELTA declarations were removed; the missing-location -10 and
    neutral phase marker stay adapter-owned diagnostics. Behavior is structural-only. The ladder
    veto remains additive -100000 rather than a true hardVeto; R2 still requires fine >= 200 or
    drain delta >= 2; V53b transit suppresses only the deferred wrong-direction veto; V137 remains
    limited to battle-seeking R2; +/-2800 clamp, R2/R3-only demotion, R4 exemption, and the unclaimed
    ranked-move -50 remain exact. Band margin remains 730. Focused 36/0/0/0, MOVE-named
    430/0/0/0, full reactor 1614/0/0/26, normalized mirror parity, AI-only boundary,
    forbidden-symbol scan, and clean package passed. Direct adapter fixtures prove both bots keep
    additive -100000 without setting hardVeto. Independent review found no behavior or ordering
    drift. Server jar SHA-256 1010dafad4b17d948bf396c64fa6bbe3f24adf39fd4537304a3f86654d6c6b1c
    and web jar SHA-256 65a43a38c6471ceff156f7d069b93341860f70b8b85b62f7294a515dd4880000
    contain the shared ladder, opportunity result types, and both adapters. Runtime reload and
    live-game proof remain pending on the external runtime. AI source only; no GEMP engine or
    player-decision changes.

  ════ V252 (2026-07-19): consolidate BATTLE-1 initiation policy ownership ════
    New BattleInitiationPolicy owns the pure predicates, thresholds, exact contributions, and
    reasons for Barrier risk, Hunt Down armed-Vader aggression, Inquisitor destiny, V76 prediction,
    specific and fallback battle scoring, scan outcome, must-fight, V61 reserve, V27/DTF Force, and
    life-force posture. BattleDecisionPolicy retains every engine and card read, predictor/oracle
    call, FormationSafety check, exception boundary, log, state flag, and additive mutation. The
    unused CRUSH_THRESHOLD and RISKY_THRESHOLD declarations were removed; active constants moved
    intact. Behavior is structural-only: prediction precedence, all power/weapon bands, fallback
    order, reserve overpower margin 8, DTF Force threshold 3, and additive semantics remain exact.
    FormationSafety remains the only true hard veto. Two MOVE cross-phase source tests now follow
    V29.9 ownership to the new policy; no MOVE production source changed. Focused 11/0/0/0,
    BATTLE-named 51/0/0/0, isolated full reactor 1625/0/0/26, shared-adapter runtime parity,
    AI-only boundary, forbidden-symbol scan, and isolated package passed. Independent review found
    no behavior or ownership drift. Server jar SHA-256
    c0aa7262f49b82aa79f92e15d775765e69d2bb6f154124d70b92841edf362260 and web jar SHA-256
    0fe74952e1d1e9a28b992c686dfba0a574da4632217e7e575f07f7f0497659de contain the shared policy,
    coordinator, and both adapters. Runtime reload and live-game proof remain pending on the
    external runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V253 (2026-07-19): consolidate BATTLE-3 standalone forfeit scoring ════
    BattleForfeitPolicy now owns V48 ship-with-crew and the ordered standalone V139/V21
    forfeit-value, power, uniqueness, and objective-protection contributions. Both
    CardSelectionEvaluator mirrors retain every card, blueprint, attachment, crew, and objective
    read; exception boundaries; logs; display text; and direct additive mutation. Exact legacy
    scoring remains V48 -9999, max(0,100-forfeit*10), low power +50, high power -100, valuable
    unique -300, generic unique -100, and first-match objective required/pullable -9999. The
    -9999 arms remain additive and do not set hardVeto or defer. V45 direct interception, V154
    weapon early-continue, V67y's combined-route no-op, and combined force-loss/forfeit routing are
    untouched. Review caught and corrected an initial scope drift that placed V21 inside the
    blueprint gate; a direct null-blueprint fixture now proves both bots retain -9949 and no true
    veto. Focused 16/0/0/0, BATTLE-named 56/0/0/0, full reactor 1630/0/0/26, normalized mirror
    parity, AI-only boundary, forbidden-symbol scan, and clean package passed. Independent review
    found no remaining behavior drift. Server jar SHA-256
    e1670eb6774754f0ba711c9c89696d1570f3b2c645b588ab2eb03e67995042af and web jar SHA-256
    a010576c75a53a9b2d3c0b48ad6bf64bd741d0f4afbbfcb71d4aee7089878984 contain the shared policy
    and both adapters. Runtime reload and live-game proof remain pending on the external runtime.
    AI source only; no GEMP engine or player-decision changes.

  ════ V254 (2026-07-19): consolidate V25 action-text battle initiation scoring ════
    New BattleActionTextFacts and BattleActionTextPolicy own V25's pure effective-power formula,
    exact branch order, strict suicide predicate, score thresholds, reasons, locationless fallback,
    and independently additive reserve penalty. Both ActionTextEvaluator mirrors retain the battle
    text gate, action type, target resolution, game/opponent/power/ability/reserve reads, exception
    boundary, logs, and direct policy-operation application. Behavior is structural-only. V25
    remains no-opponent -100; suicide -500 only above both strict thresholds; effective difference
    remains power difference plus 2.5 per ability difference; bands remain +200/+120/+60/+20 and
    -60/-120/-250; unresolved location remains +30; reserve below 3 remains an independent -50.
    All contributions remain additive, and the combined score remains raw-float exact as
    BattleDecision base 100 plus BATTLE-1 plus V25. Focused 26/0/0/0, BATTLE-named 91/0/0/0,
    full reactor 1641/0/0/26, direct two-bot CombinedEvaluator merge fixtures, stock ordinal
    action-ID contract, normalized mirror parity, raw-float stack fixtures, AI-only boundary,
    forbidden-symbol scan, compiled-artifact gate, diff check, and async-reactor package passed.
    Independent review found and closed the direct combined-merge proof gap, then returned PASS.
    Server jar SHA-256 1a8c05c62310c0c4db27017bd0ac785fc0b0ab4090d1c95cbf94d8680df9462a and web jar
    SHA-256 3b64d95f9a9c1c6f4a5707fe27e97b596335bb680d5f176b4145bedf9a262fac contain the shared policy,
    facts, and both adapters. Runtime reload and live-game proof remain pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V255 (2026-07-19): consolidate remaining BATTLE-2 action-text tactics ════
    BattleActionTextFacts and BattleActionTextPolicy originally owned the remaining pure scoring for
    Welcome Home, both You Are Beaten routes, add-destiny, Hatred, I Have You Now, FMFTD,
    Vader/Inquisitor recall, Stunning Leader, destiny modifiers/protection, V175 kill-shot and
    substitute-destiny, weapon-target cancel, attrition immunity, forfeit protection, and
    retargeting. Both ActionTextEvaluator mirrors retain all action/source/phase gates,
    DeckOracle, card, battle, power, ability, destiny, forfeit, ownership, and stack reads;
    exception boundaries; action types; exact logs; branch order; and direct policy application.
    Adapter logs now consume policy outputs instead of repeating scoring formulas. V268 supersedes
    the cross-phase ownership: Welcome Home's Reserve pull and You Are Beaten's IAYF search are
    PULL_SEARCH ADD operations, while the independently stackable +500 battle freeze remains a
    BATTLE_WEAPONS ADD here. All other V255 contributions remain ADD operations in BATTLE_WEAPONS,
    including additive -9999. Force Push,
    fire, throw, and redraw remain solely in BattleWeaponsPolicy; BATTLE-1, V25, and BATTLE-3 stay
    independent. Exact legacy score bands and independent stacking are unchanged; unresolved
    kill-shot retains its zero-score diagnostic ADD. Focused 27/0/0/0, BATTLE-named 103/0/0/0, full reactor
    1653/0/0/26, normalized mirror parity, AI-only boundary, forbidden-symbol scan, source
    ownership, raw-float fixtures, compiled-artifact gate, diff check, and clean package passed.
    Independent review caught and closed the unresolved V175 zero-delta trace omission, then
    returned PASS.
    Server jar SHA-256 214dbb04b689dae71e5c3f3f3399341fad76ae72848b3d827176bf48cd822982 and web jar
    SHA-256 404e8b352d50e159544b72d1f4c7cb30938105b8a854c5dccc34812662fd30ee contain the shared policy,
    facts, and both adapters. Runtime reload and live-game proof remain pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V256 (2026-07-19): consolidate DEPLOY action-text scripts ════
    New DeployActionTextFacts and DeployActionTextPolicy own the pure decisions for AMSD's Bespin,
    failed-turn, Piett/Executor, Force, and early-turn gates; docking-bay expansion; Vader Castle;
    Dining Room Lando buddy safety; Bespin ship priority; and simultaneous ship/pilot deployment.
    Both ActionTextEvaluator mirrors retain all table, DeckOracle, Force, objective, Vader, docking-
    bay, Dining Room, friendly-power, mutation, exception, logging, branch-order, and terminal-
    continue behavior. Exact additive scores remain AMSD -9999 or +1500/+500 with later V22.5
    +300/+100 fallthrough; docking bay -200/-50/+200/+30; Vader Castle +50/0/-500/+550; Dining
    Room +150/-30/+30/-20; and simultaneous deploy +120. Missing Piett/Executor and non-Piett
    specific actions remain the only AMSD paths that record a failed turn; unanalyzed Oracle paths
    still fail open. All extracted operations remain ADD in DEPLOY_SEQUENCING. Focused 25/0/0/0,
    DEPLOY-named 129/0/0/0, full reactor 1671/0/0/26, normalized mirror parity, AI-only boundary,
    forbidden-symbol scan, source ownership, direct two-bot AMSD stacking/mutation fixtures,
    compiled-artifact gate, diff check, and clean package passed. Independent review returned PASS.
    Server jar SHA-256 eb390ecc28d1c85bb76905f1369bf688239c7ea4cd2ba811463aff978db4561e and web jar
    SHA-256 081a7ef295a8e9e044be678a5e3252db9506f4db8c961c1dd86601166578ba02 contain the shared policy,
    facts, and both adapters. Runtime reload and live-game proof remain pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V257 (2026-07-19): consolidate DEPLOY parent routing and generic card value ════
    New DeployActionEnvelopeFacts/Policy own initial deploy-action scoring, cancel-loop and
    persona terminal routes, the turn-one title gate, and unknown-card fallback. New
    DeployCardValueFacts/Policy own ratio, destiny, elite-character, high-ability, board-behind,
    and critical-life scoring at their original interleaved positions. Both DeployEvaluator
    mirrors retain all GEMP, card, blueprint, plan, board, life-force, objective, and DeckOracle
    reads; V209/V211/V212/V213 calls; specialized location/Bespin, character, destination, pull,
    weapon, pilot, and ship logic; exception and terminal boundaries. Exact live totals remain
    cancel-loop -19998, persona -1000, title block base 50-9999, normal base +50, ratio
    +40/+20/0/0, destiny +15, elite +100, ability +25, urgency +20/+30, and neutral unknown
    routes with the existing +200 unknown-location exception. All extracted operations remain
    ADD in their original order. Focused 61/0/0/0, DEPLOY-named 145/0/0/0, full reactor
    1687/0/0/26, normalized mirror parity, source ownership/order/read-boundary, AI-only boundary, forbidden-
    symbol scan, raw-float and direct two-bot terminal fixtures, compiled-artifact gate, diff
    check, and clean package passed. Independent re-review confirmed exception/read boundaries,
    exact reason text, mirror parity, and AI-only scope. Server jar SHA-256
    ec15ac783a4c8dbc80554430ddee5b138b08b00fcf55e6f65ab975d05c393b19 and web jar SHA-256
    308f1efb49bfef5f444496405f111ccfaa9ca14aa9403ae0b5681569b6e1f4c9 contain both pure
    owners and mirrored adapters. Runtime reload and live-game proof remain pending on the
    external runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V258 (2026-07-19): consolidate DEPLOY early-location and Bespin-first sequencing ════
    New DeployObjectiveSequencingFacts/Policy own the legacy text-only location +200, Piett-dig
    +150, Bespin +800/+400, broad V29 exemptions, objective/no-capital release decisions, and
    additive -500 Bespin-first route. Both DeployEvaluator mirrors retain all action-text,
    resolved-card, ObjectiveAnalyzer, DeckOracle, zone, board, subtype, and power reads; lazy
    release checks; logs; and the early terminal continue. This is structural-only: the known
    text-only false positive and later objective-score dominance of -500 remain unchanged for a
    separate repair packet. All operations remain ADD in DEPLOY_SEQUENCING. Focused 13/0/0/0,
    DEPLOY-named 154/0/0/0, full reactor 1696/0/0/26, normalized mirror parity, branch/reason/raw-
    float matrices, source ownership/lazy-read order, AI-only boundary, forbidden-symbol scan,
    compiled-artifact gates, diff check, and clean package passed. Independent review returned
    PASS on totals, reasons, lazy reads, terminal/fallthrough routing, mirror parity, and scope.
    Server jar SHA-256
    d9e25981428603112732102bd6203bc22da7d5c0e02e4651a22e98938f86c963 and web jar SHA-256
    68a911a1915bdbf7b59311c1d7e9a8af68801a44de3afd46340d95ae0ada8893 contain the pure owner
    and both mirrored adapters. Runtime reload and live-game proof remain pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V259 (2026-07-19): repair TDIGWATT objective identity and pre-flip routing ════
    Shared AI ObjectiveAnalyzer now exposes one title-derived isTdigwatt flag for classic and
    virtual TDIGWATT. Both DeployEvaluator mirrors use it for HOLD_BACK diagnostics, the turn-one
    DEPLOY_LOCATIONS plan gate, and V52 tail scripts; the tail additionally requires !isFlipped.
    Actual TDIGWATT retains additive -1000 plus terminal continue and its unflipped turn-one V52
    script. Other objectives now take neutral V40 fallthrough, a deliberate +1000 raw difference
    plus restored downstream evaluation. Post-flip TDIGWATT no longer receives a pre-flip script.
    General Bespin-aware rules remain unchanged. Focused 22/0/0/0, DEPLOY-named 158/0/0/0, full
    reactor 1701/0/0/26, objective identity, direct flipped/reset, and plan boundary fixtures, source gate checks,
    normalized mirror parity, AI-only boundary, forbidden-symbol scan, compiled-artifact gates,
    diff check, and clean package passed. Independent review passed after its only coverage note
    was closed with direct post-flip and identity-reset fixtures. Server jar SHA-256
    47a60ad0c670b9a02c7e93d7923c9cfb29c2b29b64c5a007485129a71bc0fa82 and web jar SHA-256
    1bc3bab174b0ec209ab207bd2385f9c2b2a3f59bd2ae8d2c81e831db8119ad0b contain the canonical
    identity and both mirrored adapters. Runtime reload and live-game proof remain pending on the
    external runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V260 (2026-07-19): repair DEPLOY early-location subject classification ════
    The shared sequencing owner now classifies early location actions from the resolved deployed
    card category whenever available. Both DeployEvaluator mirrors resolve a classification-only
    card reference even when GEMP already supplied a title, while preserving the legacy earlyCard
    title-null gate consumed by V29 and unknown-card routing. Only unresolved actions use text
    fallback, and that fallback examines the direct deployed subject before destination/source prepositions.
    Consequently, a character deployed to a site/system no longer receives location +200 and a
    terminal continue that bypasses affordability, buddy, objective, and downstream scoring. A
    real resolved LOCATION retains terminal +200 plus existing Piett/Bespin additions even when
    its action text is bare Deploy. Unresolved direct-object forms such as Deploy a battleground
    site from Reserve Deck remain supported. V29 Bespin-first, its additive -500, objective
    weights, and every other deploy score are unchanged. Focused 16/0/0/0, DEPLOY-named
    161/0/0/0, full reactor 1704/0/0/26, classifier/source/two-bot fixtures, a source gate proving
    the classification-only card cannot enter V29, normalized mirror parity, AI-only boundary,
    compiled-artifact gate, diff check, and clean package passed. Server jar SHA-256
    51c1a483ace20250c7b5a28f6a4dba2d60c893ed362268663f9376a40d3d86ae and web jar SHA-256
    44d887af117368807544c1811df0116aa0b9e7617588cbb83707034054d63992
    contain the shared classifier and both mirrored adapters. Runtime reload and live-game proof
    remain pending on the external runtime. Independent review caught and closed a possible V29
    state leak before commit; re-review passed. AI source only; no GEMP engine or player-decision changes.

  ════ V261 (2026-07-19): make TDIGWATT Bespin-first a real DEPLOY gate ════
    V29 now runs only for canonical ObjectiveAnalyzer.isTdigwatt identity. Resolved card category
    distinguishes characters from locations, ships/vehicles, and support cards; unresolved action
    text examines the direct deployed subject, so a character deployed aboard Executor/Bespin is
    not falsely exempted by its destination. Objective-forbidden Executor and analyzed-no-capital
    routes still release and fall through. A live candidate retains the legacy additive -500, then
    terminates before downstream bonuses. Ordinary base 50 therefore remains -450 instead of being
    erased by later scores such as V169's +1100 protection bonus (which alone previously produced
    +650 before other additions). The operation remains ADD, not a hard veto. Focused 15/0/0/0,
    DEPLOY-named 162/0/0/0, full reactor 1705/0/0/26, resolved/unresolved classification matrices,
    release/penalty raw-float and adapter-route fixtures, canonical identity and terminal-order
    source gates, normalized mirror parity, AI-only boundary, diff check, and clean package passed.
    Server jar SHA-256 46e367ee5178b65f645e052de8053e9f76080daf5ea19f9e1cc712f70fdbae18
    and web jar SHA-256 e59bc6ad94b80283687cf84e7476b7aee127d34d7f383fa47ba1a0be877ec26d
    contain the gated owner and mirrored adapters. Runtime reload and live-game proof remain pending
    on the external runtime. Independent review passed identity, classification, route, mirror, and
    engine-free ownership gates. AI source only; no GEMP engine or player-decision changes.

  ════ V262 (2026-07-19): consolidate the DEPLOY phase-script walker ════
    One shared AI-only DeployPhaseScript now owns the exact V67bb/V67bc/V179 deploy-phase walk
    that previously existed as two 517-line bot copies. Rando and ChosenOne keep thin compatibility
    facades that provide their original logger and DeckOracle calls, so caller class names and the
    inherited Step/Result types remain compatible. This packet is structural-only: bucket order,
    action exclusions, card and hand classification, source-card parsing, keyword fallback, named-
    location-in-hand guard, battleground qualification, no-opinion and empty-pass routing, logs,
    and ordered results are unchanged. Focused ownership/characterization 14/0/0/0, DEPLOY-named
    164/0/0/0, full reactor 1707/0/0/26, exact old-to-shared implementation diff, thin-facade source
    gates, inherited caller compilation, AI-only boundary, forbidden-symbol scan, compiled-artifact
    gates, and clean package passed. Server jar SHA-256
    2da1021d445be7544c4cff55fcb8b1f8ace5d6858aa8ebf799cde431c266d6d2 and web jar SHA-256
    fab34a7f123517ca0e3fcbed53c6cd964edd99909496a6fe86dc77eacaf9be20 contain the shared owner
    plus both facades. Independent review passed behavior, caller types, logger/oracle routing,
    reflection lookup, bot parity, AI-only scope, and metadata boundaries. Runtime reload and
    live-game proof remain pending on the external runtime. AI source only; no GEMP engine or
    player-decision changes.

  ════ V263 (2026-07-19): consolidate DEPLOY formation and siting scoring ════
    Shared AI-only DeployFormationSitingPolicy now owns the duplicated legacy solo/objective-flip,
    staging and solo-caution, Vader/strong-ally reinforcement, buddy-seek battleground gate, Hunt
    grouping, high/good-drain, and positive fortify/establish/reinforce/buddy/armed score ladders.
    Rando and ChosenOne retain all game-state reads, location-loop control, exception boundaries,
    objective lookup, and diagnostics in mirrored adapters. Scores, thresholds, reasons, operation
    order, and terminal behavior are unchanged. The unused V67bl paired-deploy calculation and its
    private support reads were removed because they had no score or control-flow consumer. First
    independent review caught a live V53 undercover-spy reserve diagnostic; the exact read and log
    were restored in both adapters before the final PASS. Focused policy/source/parity 22/0/0/0,
    DEPLOY-named 169/0/0/0, full reactor 1712/0/0/26, source parity and characterization, AI-only
    boundary, forbidden-symbol scan, diff check, compiled-artifact gates, and clean package passed.
    Server jar SHA-256 aceabe45911facdcb6a7a6e83941e8a594291e66b29793b5d376fec9d56e523b and web
    jar SHA-256 c4c0033c502b7c8f5f571c4c9b9feda341bbdf1e3319744ddb6dc38dc93e89c7 contain the shared
    policy. Independent re-review passed restored diagnostics, mirror parity, score/reason order,
    thresholds, lazy reads, exception boundaries, and dead-read removal. Runtime reload and live-game
    proof remain pending on the external runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V264 (2026-07-19): consolidate remaining tactical DEPLOY scoring ════
    Shared AI-only DeployTacticalPolicy now owns V53/V51 opponent-drain contesting, V51 Vader
    flip deployment, V50 early/late power danger, V34 direct engagement, V36 zero-score empty
    deployment, and V51/V43 undercover-spy placement. Both bot adapters retain every power, drain,
    objective, location, Jedi/Hatred, and spy-target read plus diagnostics, exception boundaries,
    and location-loop control. Scores, thresholds, reasons, and operation order are unchanged. V50's
    early-danger continue still skips only the current location; late danger keeps its zero-score
    reason. Repeated high-drain spy targets retain +1000 each using ordered typed arms V51, V51#2,
    V51#3, and so on. The dead V37.4 canDeployToOpponents scan was removed because it had no consumer.
    Focused tactical/source/parity 32/0/0/0, DEPLOY-named 177/0/0/0, full reactor 1720/0/0/26,
    normalized mirror parity, loop-order source gate, repeated-target ledger registration, AI-only
    boundary, forbidden-symbol scan, diff check, compiled-artifact gates, and clean package passed.
    First independent review caught that duplicate V51 rule-arm IDs would make the ledger reject the
    second spy target and the adapter catch would erase the score; indexed rule arms and a ledger
    regression test fixed it before final independent review returned PASS. Server jar SHA-256
    544e5c445c5092f74f4cf285c327e4bb46d8026a196fb5f87f7de2533f0a10c4 and web jar SHA-256
    329467f22f4807c7524ef3937bbc40f00468fb8f2e565e383cfbc0df45a94518 contain the shared policy.
    Runtime reload and live-game proof remain pending on the external runtime. AI source only; no
    GEMP engine or player-decision changes.

  ════ V265 (2026-07-19): consolidate objective-specific DEPLOY scoring ════
    Shared AI-only DeployObjectiveSitingPolicy now owns V51 Cloud City army and objective-first,
    V67ak key-character, V22.7/V24 Cloud City engine, V24.1 Gherant, V29.2/V47 Lando-Lobot,
    and V31/V36/V40 pre/post-flip scores and exact reasons. Both bot adapters retain every game,
    card, analyzer, Oracle, power, location, hand, and action-text read plus first-match loops,
    strict top-two hold-location selection, exception boundaries, logs, and action-list control.
    Behavior is structural-only. V22.7 remains additive -800 plus outer append/continue before a
    lazy Oracle read; Oracle failure still gives V24 +300. Lando retains precedence over Lobot,
    failed backup reads retain -9999 without continue, and the pre-flip defense ladder remains
    +250/+500/+800/+1000 with mutually exclusive V31/V36/V40 routes. Distinct typed arms prevent
    objective-score ledger collisions. Focused objective/source/parity 26/0/0/0, DEPLOY-named
    185/0/0/0, full reactor 1728/0/0/26, normalized mirror parity, blocked-control and lazy-read
    source gates, score/outcome fixtures, AI-only boundary, forbidden-symbol scan, diff check,
    compiled-artifact gates, and clean package passed. Independent review passed scores, reasons,
    order, control and failure boundaries, tie handling, rule IDs, mirror parity, and scope. Server
    jar SHA-256 a0b9ec1165f36e937705e7187764ba510cda51049b16b83427f3621372eaae27 and web jar SHA-256
    cfdde6e782fe38933131bd0cbcac46462471f1dc71ed76af076f2dd603da06bc contain the shared owner
    and both mirrored adapters. Runtime reload and live-game proof remain pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ════ V266 (2026-07-19): consolidate DEPLOY ability and buddy thresholds ════
    Shared AI-only DeployFormationSitingPolicy now also owns V32/V40 ability-four decisions and
    V33/V67ag battleground buddy-ability decisions. Both adapters retain ability, site,
    battleground, friendly-card, and hand reads plus logs, catches, and first-site exits. V32
    remains +150 only when an occupied site's ability crosses four; solo/shared totals below four
    retain zero-score reasons, and the hand remains lazy to solo totals below four. V67ag still
    treats a battleground-query failure as non-battleground, applies -300 only when a friendly is
    found, and exits before ability scans. V33 remains +150 crossing the configured threshold and
    +100 reinforcing below it. Focused formation/source/parity 24/0/0/0, DEPLOY-named 187/0/0/0,
    full reactor 1730/0/0/26, normalized mirror parity, lazy-hand and early-break source gates,
    branch/reason fixtures, AI-only boundary, forbidden-symbol scan, diff check, compiled-artifact
    gates, and clean package passed. Independent review passed scores, reasons, precedence, reads,
    breaks, exception behavior, logs, rule IDs, mirror parity, and scope. Server jar SHA-256
    8ad42ef06d96813108ed17469efe37884c68b743d37b67e07999ce3857987f29 and web jar SHA-256
    989e454773e52f64a702dd1972d27b74114ad83bb6b0618c87b0cec965535b97 contain the shared
    owner and both adapters. Runtime reload and live-game proof remain pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ==== V267 (2026-07-19): consolidate DEPLOY weapon-order and Evazan combo scoring ====
    Shared AI-only PullActionPolicy now exposes one weapon-order decision owner used by both the
    direct DEPLOY route and the existing V192 parent scorer. It preserves V67ar all-armed -9999,
    V67ao no-character -9999, V149 no-capable-lightsaber-wielder -2000, exact reasons, and branch
    precedence. Ready remains neutral in DeployEvaluator while V192 independently owns its +600
    weapon tier. Shared DeployTacticalPolicy now owns V24.3A Evazan +150 and weapon-character +100.
    Both bot adapters retain all action, card, icon, ability, host, and DeckOracle reads; the exact
    six partner-title checks; short-circuit order; logs; catches; and separate contribution streams.
    Focused 62/0/0/0, DEPLOY-named 191/0/0/0, full reactor 1740/0/0/26, normalized mirror parity,
    exact boundary fixtures, lazy-Oracle source gates, AI-only scope, forbidden-symbol scan, diff
    check, compiled-artifact gates, and clean package passed. Independent review returned PASS with
    no P0-P2 findings on precedence, scores, reasons, read order, mirror parity, or scope. Server jar SHA-256
    a9383f3c559fa148292d9aedd8296c53f81120d11c520e17896b822e901b72cf and web jar SHA-256
    158c37f6a91ed639f25e71f216bd8b1bef1446d95548a0024d8832f112b6019e contain both shared
    owners and mirrored adapters. Runtime reload and live-game proof remain pending on the external
    runtime. AI source only; no GEMP engine or player-decision changes.

  ==== V268 (2026-07-19): consolidate remaining PULL action and child-selection scoring ====
    Shared AI-only PullSpecificActionPolicy now owns the remaining card-specific ActionText PULL
    scores, including V142, V147, V155 Reserve mode, V144 search mode, V23 pile guards, V24,
    V24.6, IAYF, named-source checks, V37, V24.9, V53, and admiral/general pulls. Shared
    PullSelectionCandidatePolicy owns the route-specific child decisions for V186, unknown gain
    values, Hunt Down lightsabers, Cloud City route choices, priority cards, AMSD safety/pilot
    routing, and Reserve-child plan weights. Its operations retain their semantic tree domains:
    SETUP_STARTING, PULL_SEARCH or FORCE_LOSS_PAYMENT, DECK_PLAYBOOK, and DEPLOY_SEQUENCING. Both
    ActionText and CardSelection mirrors retain every GEMP read, stock identity, phase/source gate,
    exception, log, score reset, early continue, and action-list mutation. V155's -2000 and V144's
    search -2000 now trace in PULL_SEARCH; V144's independent battle-freeze +500 remains in
    BATTLE_WEAPONS, preserving search-then-freeze order and the exact -1500 combined total. V23
    empty and low-pile routes, V24.6+V37+V192 stacking, Wokling+V192 stacking, AMSD's legacy double
    block, and non-Reserve retrieval ownership are pinned by direct two-bot fixtures. Focused
    85/0/0/0, full reactor 1789/0/0/26, normalized mirror parity, semantic-domain/output-kind
    assertions, direct V186/unknown/Reserve-blueprint/AMSD adapter fixtures, source ownership,
    AI-only scope, compiled-owner, diff, and clean package gates passed. Server jar SHA-256
    78a22cd1f00abe7c821eb4a2ff15d9c90c468e66e2e2283d5a91e38172186a75 and web jar SHA-256
    5ba857f29c4a434d49538392981d0ffc4d1244823e5df58bb9b57fa0b5271d26 contain both shared owners
    and mirrored adapters. Runtime reload and live-game proof remain pending. AI source only; no
    GEMP engine or player-decision changes.

  ==== V269 (2026-07-19): consolidate Pass scoring and typed baseline ====
    Shared AI-only PassPolicy now owns the baseline constants, early-game reduction, battle and
    follow-through terminal outcomes, low-Force/Reserve and hand-size conservation, V37.4 hand
    bloat, Move-phase draw conservation, V27.1 DTF reserve, and V27 maintenance reserve. Both bot
    adapters retain decision-text classification, GameState and ForceReserveService reads, logs,
    catches, action construction, and list control. Both AI-only EvaluatedAction mirrors expose a
    typed-initial constructor so the unchanged 5.0 Default pass option baseline records
    PASS-baseline/PASS_CANCEL/BANDED as its INITIAL operation; the old constructor still delegates
    with identical score, reasoning, and legacy trace behavior for every other action.
    Scores, multipliers, thresholds, reasons, order, terminal returns, read gates, and catches are
    unchanged. Focused Pass/evaluator 15/0/0/0, full reactor 1802/0/0/26, exact mirror parity,
    direct baseline/full-stack/terminal-read/fail-open fixtures, canonical identity checks, typed
    INITIAL capture, false-side boundaries, AI-only scope, diff check, compiled-byte equality,
    and clean package passed. K2's reference-spec gate passed the V269-proper content and excluded
    the quarantined branch's stale V268 policy. Server jar SHA-256
    b5694ced51258ec4a04b03bd831f6ede04920152a8bdb8c7183bb32629b87fe5 and web jar SHA-256
    0240271557b3dc04b170ab5c63c5061c067252c729f2132d88904e389d419927 contain the shared owner
    and mirrored adapters. Runtime reload and live-game proof remain pending. Quarantined commits
    2219f4760 and descendant 0b4045805 are superseded references and must never land. AI source
    only; no GEMP engine or player-decision changes.

  ==== V270 (2026-07-19): consolidate DEPLOY pilot-candidate scoring ====
    Shared AI-only DeployPilotShipPolicy now owns generic pilot ability, power, and deploy-cost
    ranking plus simultaneous-pilot Star Destroyer eligibility, planned-pilot priority, and
    matching-pilot ranking. Both bot adapters retain decision and phase classification, AMSD
    handling, GameState, card, blueprint, plan, icon, cost, ability, title, and matching reads;
    exact logs; catches; score reset; candidate control; and lazy read boundaries. Generic scoring
    remains ability then power then cost. Invalid Star Destroyer pilots still set -500, add -500,
    and continue before plan or quality reads; valid pilots get +100 first. Planned pilots get only
    +200; unplanned pilots keep cost, ability, matching order. Focused policy/source/parity
    18/0/0/0, DEPLOY-named 200/0/0/0, full reactor 1811/0/0/26, exact mirror parity, source-order
    gates, AI-only scope, diff check, and clean package passed. Server jar SHA-256
    4d0ba1ffaf3c0c14b9f534dba4d74026d8a160ec46fa1afba9c24ca4e1d4a888 and web jar SHA-256
    212def7df852d489551833900db3677ed3163ec8b902694130acbe5a1f304c14 contain the shared owner
    and both adapters. Runtime reload and live-game proof remain separate deployment gates.
    Clean V270 is based on clean V269 75ec7cfcf; quarantined reference 4397472ed and its ancestors
    remain reference-only. AI source only; no GEMP engine or player-decision changes.

  ==== V271 (2026-07-19): consolidate DEPLOY destination compatibility scoring ====
    Shared AI-only DeploySitingPolicy now owns the contiguous destination-compatibility ladder:
    V29 ship-reference ground -200; V190 starship site -1500 and space -80/+30/+10/+30 with
    +20 fallback; vehicle -150/-150/+10; V24.14B permanent-weapon space/ground -300/+100;
    V29.7 empty bay +80; and V29.6 battleground +50. Both CardSelectionEvaluator mirrors retain
    all GEMP, card, blueprint, title, text, icon, owner, location, power, attachment, and candidate
    reads plus logs, catches, loop breaks, and contribution positions. V190 remains additive with
    VETO trace identity, not a hard veto, and no continue was added. Direct policy/source/parity
    19/0/0/0, DEPLOY-named 210/0/0/0, full reactor 1821/0/0/26, exact mirror parity, boundary and
    lazy-read gates, AI-only scope, diff check, and clean package passed. Server jar SHA-256
    de4b6a9a4ce2348c0cba548b7f04bf9f68b88f684c8a2ba9310350837d5fca29 and web jar SHA-256
    067d38050e0aa7e0b4e103e24f7411d10e2268e1fc803a3f50451b43bd779345 contain the shared owner
    and both adapters. Runtime reload and live-game proof remain separate deployment gates.
    Clean V271 is based on live V270 6912b5f99; quarantined reference d683fd3b0 and its ancestors
    remain reference-only. AI source only; no GEMP engine or player-decision changes.

  ==== V272 (2026-07-19): consolidate scalar DEPLOY destination scores ====
    Existing shared AI-only owners now own three residual CardSelection destination scores:
    DeployPilotShipPolicy owns V24.10 Executor/Flagship destination (+500 Bespin, additive -9999
    any other system); DeploySitingPolicy owns V23 opponent Force icons (icons * 30 above zero);
    and DeployCardValuePolicy owns the exclusive V29.7 destination-ability ladder (+50/+25/+5,
    silent from 1 through below 3, -30 below 1). Both adapters retain all card, location, side,
    icon, blueprint, and ability reads plus guards, catches, logs, and contribution positions.
    Exact reason strings and integer ability rendering are unchanged; no candidate continue was
    added. Focused policy/source/parity 46/0/0/0, DEPLOY-named 217/0/0/0, full reactor
    1828/0/0/26, exact mirror parity, boundary and read-order gates, AI-only scope, diff check,
    and clean package passed. K2's clean-base gate passed the temp-id and additive-VETO boundaries.
    Server jar SHA-256 2f4bb38ebe8f790d98c10baa049f60db711f4f7278ddacd74c4516681fed89cc and web jar SHA-256
    36b9f75d4278a8b2156e5d351e6f50acc862a8c48bb7d5ce648b2cb3ed2657b7 contain the shared owners
    and both adapters. Runtime reload and live-game proof remain separate deployment gates.
    Clean V272 is based on live V271 8469a915f; quarantined reference b41749d20 and its ancestors
    remain reference-only. AI source only; no GEMP engine or player-decision changes.

  ==== V273 (2026-07-19): consolidate DEPLOY ship boarding and cargo scoring ====
    Shared AI-only DeployPilotShipPolicy now owns the V29 destination score ladder for characters
    boarding ships and non-character cards targeting ship cargo. Both CardSelectionEvaluator
    mirrors retain the destination, blueprint, title, game-text, unique-ship-name, and subtype
    reads; declaration-order first-match break; generic capital match; catches; exact logs; and
    cargo actions.add plus candidate continue. Exact outcomes remain +600 for the referenced ship,
    +650 with the force-drain text, +50 for a different referenced ship, +100 for an unreferenced
    character aboard Executor, +50 aboard another ship, and additive -300 for cargo before the
    existing continue. Temporary destination IDs still exit through the physical-card parse path
    before V29 boarding can run. Focused policy/source/parity 29/0/0/0, DEPLOY-named 224/0/0/0,
    full reactor 1835/0/0/26, exact mirror parity, score and control fixtures, read-order and
    first-break gates, AI-only scope, diff check, and clean package passed. Server jar SHA-256
    cd902a1b76d03bcf1bc578a7f19594491e54eddf59234a702219bbe3359c9aa8 and web jar SHA-256
    a3563017b7028722d4ad02b82cb8e96661b6b8b6b054851b6aab367ef90e7f4e contain the shared owner
    and both adapters. Runtime reload and live-game proof remain separate deployment gates.
    Clean V273 is based on live V272 d8c75cdcf. AI source only; no GEMP engine or player-decision
    changes.

  ==== V274 (2026-07-19): consolidate DEPLOY weapon and lightsaber destination scoring ====
    Shared AI-only DeployWeaponPolicy now owns the V25 destination weapon-slot and Hunt Down
    lightsaber outcomes. Both CardSelectionEvaluator mirrors retain both attachment scans and
    first-match breaks, deploying-card blueprint/title reads, target category/title checks, lazy
    ObjectiveAnalyzer acquisition, catches, exact logs, and contribution positions. An armed
    weapon target remains additive -9999 and an unarmed target +20. A lightsaber on an armed
    target independently contributes another additive -9999, preserving the legacy -19998 total;
    only an unarmed target reads ObjectiveAnalyzer, and analyzed Hunt Down V remains +150. No
    candidate continue was added. Focused policy/source/parity 20/0/0/0, DEPLOY-named 231/0/0/0,
    full reactor 1842/0/0/26, exact mirror parity, score/reason/double-addition fixtures, read-order,
    lazy-analyzer, AI-only, diff, and clean package gates passed. Server jar SHA-256
    f07ed50e707e5e76267d8eaeaf575918d2a1dc02590dae23f2cb710d58df85da and web jar SHA-256
    da71a830d2941d83c6e56e02379e762579fa01d4a4fdabcae1bb92ee235f7431 contain the shared owner
    and both adapters. Runtime reload and live-game proof remain separate deployment gates.
    Clean V274 is based on live V273 124fa575b. AI source only; no GEMP engine or player-decision
    changes.

  ==== V275 (2026-07-19): consolidate Mapuzo and planned-destination scoring ====
    Shared AI-only DeploySitingPolicy now owns the V64 Mapuzo outcome after each adapter gathers
    opponent power and literal Jedi Survivor text; DeployPlanPolicy now owns the physical planned-
    target match after each adapter compares candidate and planned IDs. Both CardSelectionEvaluator
    mirrors retain every Mapuzo/character guard, opponent and power read with zero fallback,
    blueprint/game-text read, physical ID comparison, catch, exact log, and contribution position.
    Jedi Survivors remain silent; non-Jedi score +30 only when opponent power is strictly above
    zero, otherwise additive -1500. Planned target remains +200 and other physical targets -100.
    No continue or temp-ID behavior changed. Focused policy/source/parity 34/0/0/0, DEPLOY-named
    237/0/0/0, full reactor 1848/0/0/26, exact mirror parity, threshold/reason/domain fixtures,
    read-order, failed-read, physical-ID, temp-route, AI-only, diff, and clean package gates passed.
    Server jar SHA-256 eb7af3dc53b4661a521c3446e3d05cf048e27e8377de4b01ea1dc6f7f0c42c52 and web jar SHA-256
    edb5748ae2d759d48e5ef24f2577d30e5445d3fdd74a778843ce39e35d2697b8 contain both owners and
    adapters. Runtime reload and live-game proof remain separate deployment gates. Clean V275 is
    based on live V274 fcf4c3efc. AI source only; no GEMP engine or player-decision changes.

  ==== V276 (2026-07-19): repair Invasion flip gate and Naboo battleground accounting ====
    Invasion's AI playbook now owns the exact pre-flip requirement: control Naboo system and Theed
    Palace Throne Room with a Neimoidian there. Shared ObjectiveAnalyzer exposes the unfilled exact
    actor-at-site fact; both evaluator mirrors let V193 steer only that actor to that site, bypass
    V201 for that objective obligation, and self-close once a Neimoidian is present. Replay
    Game93e317128d5e-f769-7e51-61c6-4c381f10 proved Nute chose a tied Swamp and V121/V201 later
    displaced Sil Unch from the Throne Room. Direct V193 is +1600, net +100 over V121 -1500; the
    destination arm is +3200, net +1700 after V121 and still +900 under a conservative retained
    V201 -800 stack. Shared AI-only ShieldFacts also excludes Naboo system from Battle Order/Plan
    battleground accounting while 14_113 is on table on either face, per Advanced Rulebook 2023
    p.145-146; Naboo sites and other systems are unchanged. Focused 42/0/0/0, DEPLOY-named
    238/0/0/0, full reactor 1856/0/0/26, mirror parity, exact gate boundary fixtures, the
    Invasion/Naboo table-state matrix, JSON, AI-only, forbidden-symbol, diff, and clean package
    gates passed. Server jar SHA-256 959cd4fb2ccd293a3b64883beb5d311f7cdfa12cd49992b703786e9ecc839d9f
    and web jar SHA-256 da86408f28af70dc4fe2b9bfbbcc19523b6212f5c2970356985dc40eb65b8909
    contain V276. Runtime reload and live-game proof remain separate deployment gates.
    Clean V276 is based on live V275 814751680. Production changes are AI source plus the AI-owned
    objective playbook only; no engine, card, decision metadata, client, database, or deck-library
    source changed.

  ==== V277 (2026-07-19): consolidate generic DEPLOY formation topology ====
    Shared AI-only DeployFormationSitingPolicy now owns empty-destination concentration and the
    full committed-reinforcement ladder. Both CardSelectionEvaluator mirrors retain every GEMP,
    location, character, owner, blueprint, power, permanent-card, planet-prefix, and escape-route
    read plus catches, scan continues, first-found breaks, logs, and contribution positions.
    Contested solos remain first and scale -200 each; uncontested solos scale -100 each; no solos
    gives +20. V67bn remains +800 for any committed formation with an inclusive 4..5 deficit and
    no escape. V67bu escape remains silent and suppresses every fallback. Outside that band, a
    power <=5 solo retains +150 without an opponent or +250 with any positive opponent, including
    the legacy deficit-above-five case; a pair retains +100 only strictly above 1.5x. Existing
    V29.5 buddy and V113 contributions remain later and independent. Focused formation/source/
    parity 22/0/0/0, DEPLOY-named 244/0/0/0, full reactor 1862/0/0/26, exact mirror parity,
    boundary/control/source gates, AI-only scope, forbidden-symbol scan, diff, compiled-policy,
    and clean package gates passed. Server jar SHA-256
    0b635131739a1dd8ee796740ef0d52432a5078392a43ce80355afc99650dfb13 and web jar SHA-256
    95fb22de3d26ff5ef4080654bf7f7148874e9a94728e6479ca416dcf94da3a98 contain V277. Runtime
    reload and live-game proof remain separate deployment gates. Clean V277 is based on live V276
    8a2720d09. Reference 72976ca77 was a patch oracle only; no quarantined ancestry was landed. AI
    source only; no GEMP engine or player-decision changes.

  ==== V278 (2026-07-19): consolidate tactical DEPLOY residual scoring ====
    Shared AI-only DeployTacticalPolicy now also owns V24.15 zero/effective-drain siting, V59
    universal spy siting, the V22.3 contest ladder, V24.14B fallback-spy siting, and V24.3B
    destination partner scoring. Both CardSelectionEvaluator mirrors retain every GEMP, blueprint,
    objective, power, drain, hand, location, undercover-spy, and partner read plus nested catches,
    lazy exemptions, scans, first-match breaks, logs, fallback state, and contribution positions.
    The zero-point V22.3 tip trace uses a distinct internal arm identity so the contribution ledger
    preserves both legacy additions; user-facing reasons and totals remain unchanged. TDIGWATT and
    the V22 objective tail are untouched. Focused tactical/source/characterization 35/0/0/0,
    DEPLOY-named 251/0/0/0, full reactor 1869/0/0/26, exact mirror parity, ledger, lazy-read,
    catch/scan/control gates, AI-only scope, forbidden-symbol scan, diff, and clean package gates
    passed. Server jar SHA-256 22e51a8ce909975817dee37d6b2f56d615113d0a16aa4ebba1ccd8a9f716eb99
    and web jar SHA-256 2262feb19b209bbd60b4da235d7368d2dce65033c822476226daea17c5e27d10
    contain V278. Runtime reload and live-game proof remain separate gates. Clean V278 is based on
    live V277 ebb679cb5. AI source only; no GEMP engine or player-decision changes.

  ==== V279 (2026-07-19): consolidate objective-aware DEPLOY siting ====
    Shared AI-only DeployObjectiveSitingPolicy now also owns V22.7 objective-system contesting,
    V29.7 ISB agent deployment, Hunt Down character priority, Cloud City ability spread, Lando
    destination and safety, and the final objective/TDIGWATT siting tail. Both CardSelectionEvaluator
    mirrors retain every GameState, blueprint, ObjectiveAnalyzer, battleground, hand, character,
    power, opponent, location, and objective read plus all catches, scans, breaks, diagnostics, and
    contribution positions. Score magnitudes, branch precedence, fail-open behavior, and additive
    ordering are unchanged. Production scope is AI source only; no GEMP engine, decision metadata,
    card, action, mediator, serializer, client, database, deck-library, playbook-data, or player-choice
    source changed. Focused combined tactical/objective/source/parity 65/0/0/0, DEPLOY-named
    268/0/0/0, full reactor 1886/0/0/26, exact mirror parity, pre-scan TDIGWATT failure semantics,
    cross-phase source characterization, policy/source/control gates, AI-only scope,
    forbidden-symbol scan, diff, and clean package gates passed. Server jar SHA-256
    8274b29172eefc5dab55047120e0bb1f389afc9b7beed2d708edc88d4fd1e019 and web jar SHA-256
    00de4969c004049b40bc40d90e75783300a2baecddf86b920a12450d8cbd858f contain V279.
    Independent review, runtime reload, and live-game proof remain separate gates.
    Clean V279 is based on live V278 4d1b825a2. Quarantined references f1278bb48 and fe06e20da were
    file-scoped patch oracles only; none of their ancestry will be merged.

  ==== V280 (2026-07-19): consolidate MOVE parsec-choice scoring ====
    Shared AI-only MoveVergePolicy now also owns V79 parsec and Scarif-destination option weights
    plus the V103 parsec fallback distance weight. Both ActionTextEvaluator mirrors retain prompt
    recognition, permanent-card/owner/zone scans, Verge/Death Star/orbit observations, integer and
    regex parsing, catches, logs, actions.add, and continue control. Parsec 7 remains +1500, one hop
    +1200, other values above 4 +800, wrong direction -800, Scarif +1500, other destinations -200,
    and V103 fallback max(0, 300 - 50*distance). Rando-only V79b interception is untouched. Focused MOVE
    policy/source 25/0/0/0, MOVE-named 437/0/0/0, full reactor 1893/0/0/26, exact mirror parity,
    boundary/parsing/control gates, AI-only scope, forbidden-symbol scan, diff, and clean package
    gates passed. Server jar SHA-256 8904208ed1af0cb6339a45a80533179f31d2a21d0aeb0290acd596c3e16482ed
    and web jar SHA-256 8968c5271c00b892ff312029fc0c80fc65f42ba076ec0641ba503b98496fcfa0
    contain V280. Runtime reload and live-game proof remain separate gates. Clean V280 is based on
    live V279 0c780fac5. AI source only; no GEMP engine or player-decision changes.

  ==== V281 (2026-07-19): consolidate DEPLOY character battleground preference ====
    Shared AI-only DeployFormationSitingPolicy now also owns the mirrored V29.7 battleground
    preference and V67ah non-battleground penalty ladder. Both CardSelectionEvaluator adapters
    retain battleground checks, top-location scans, blueprint and side reads, opponent force-icon
    extraction, catches, and contribution placement. A battleground remains +80; an available
    non-battleground remains -100 with opponent force icons and -350 without them; no available
    battleground retains the original zero-point contribution. Focused formation/source 19/0/0/0,
    DEPLOY-named 273/0/0/0, full reactor 1898/0/0/26, exact mirror parity, policy/source boundaries,
    AI-only scope, diff, and clean package gates passed. Server jar SHA-256
    3d1efc3e901ff920c8db660b17488239deea70a9fb86064a934a749e7e89e4bf and web jar SHA-256
    55bbe08c8fc2302d2febb23648069c00cd0b971ddffe01b63d848ee90cf98f05 contain V281.
    Independent review, runtime reload, and live-game proof remain separate gates. Clean V281 is
    based on live V280 e9f194fdf. AI source only; no GEMP engine or player-decision changes.

  ==== V282 (2026-07-19): consolidate CONTROL legacy fallback scoring ====
    Shared AI-only ControlDrainAssessment now also owns the mirrored top-level force-drain fallback
    arithmetic. RandoCalAi and TheChosenOneAi retain action recognition, phase routing, guarded
    AiBoardAnalyzer observation, controlled-location counting, exception behavior, and contribution
    order. The adapter-supplied RandoConfig.SCORE_FORCE_DRAIN base and +20 per controlled
    battleground are unchanged. Focused CONTROL fallback/source 5/0/0/0, CONTROL-named 22/0/0/0,
    full reactor 1903/0/0/26, exact fallback mirror parity, boundary/source/order gates, AI-only
    scope, diff, and clean package gates passed. Server jar SHA-256
    f40fc0b1cd6259f2505872c8e3d027e31f4c8ade60a890cfc7e5db84ccbfb56c and web jar SHA-256
    d08703349113f147895c308166291f7652e9bc1df248298101b47e22caac5924 contain V282.
    Independent review, runtime reload, and live-game proof remain separate gates. Clean V282 is
    based on live V281 a09099cbd. AI source only; no GEMP engine or player-decision changes.

  ==== V283 (2026-07-19): consolidate BATTLE legacy fallback scoring ====
    Shared AI-only BattleActionTextPolicy and BattleWeaponsPolicy now also own the mirrored
    top-level BATTLE initiation, board-fallback, and fire-weapon arithmetic. RandoCalAi and
    TheChosenOneAi retain action recognition, guarded location and board observations, title
    matching, first-match control, fallback gating, battle-state reads, and contribution order.
    Favorable, danger, middle, contested-winning, board fallback, and +50 fire-weapon values and
    boundaries are unchanged. Focused BATTLE owner/fallback/source 29/0/0/0, BATTLE-named
    114/0/0/0, full reactor 1911/0/0/26, exact fallback mirror parity, policy/source/order gates,
    AI-only scope, diff, and clean package gates passed. Server jar SHA-256
    e08f3ab3e31bdc6c3cc052a71e86cce7076169a88be404adb8fd3c03fbcb002f and web jar SHA-256
    7ed42e9b1eedf9a39bdd7d8fb6b7badb8f9331ee1be19128da20fa643e013901 contain V283.
    Independent review, runtime reload, and live-game proof remain separate gates. Clean V283 is
    based on V282 candidate 1c8d0ffaa. AI source only; no GEMP engine or player-decision changes.

  ==== V284 (2026-07-19): consolidate RESPONSE fixed action-text scoring ====
    Shared AI-only ResponsePolicy now also owns the mirrored fixed-score V184 when-deployed,
    V29.8 Sense redraw-hand and mutual-redraw, V53b save-Jedi, react, cancel-own, and Houjix/Ghhhk
    action-text operations. Both ActionTextEvaluator adapters retain every classifier, game read,
    catch, action type, diagnostic, branch, and contribution position. Values remain +300, two
    independently additive -600 Sense arms, +500, -30, -50, and +30 respectively. Focused RESPONSE
    policy/source 17/0/0/0, RESPONSE-named 33/0/0/0, full reactor 1918/0/0/26, exact mirror parity,
    fixed-score/additive boundaries, adapter source gates, AI-only scope, diff, and clean package
    gates passed. Server jar SHA-256 8333636dd502f9a06781603e999f2b6b849fb7ac808b926f1f4c8599a0d47b0c
    and web jar SHA-256 bdcade186719081be4ddd6f12f0a295814b9f1fadfaa86095920a51c53846434
    contain V284. Independent review, runtime reload, and live-game proof remain separate gates.
    Clean V284 is based on live V283 92edabf5d. AI source only; no GEMP engine or player-decision
    changes.

  ==== V285 (2026-07-19): consolidate remaining DEPLOY action-text scoring ====
    Shared AI-only DeployActionTextPolicy/Facts now own V160 Target The Main Generator +800, late
    generic Deploy-on/projection/unique +30/-50/+30, and generic Play-a-card Force 0/1/>1
    -50/-30/+5. Both ActionTextEvaluator mirrors retain exact text and objective recognition,
    logs, action-type mutation, shield classification/routing, Force reads, and the ordinary-
    Deploy skip gate. V184 remains solely ResponsePolicy-owned, so V285 adds no duplicate score
    or cross-phase owner. Focused action-text/source/mirror 27/0/0/0, DEPLOY-named 282/0/0/0,
    full reactor 1927/0/0/26, exact mirror parity, policy and mirrored adapter-route fixtures,
    final DEPLOY hidden-score gate, AI-only scope, forbidden-symbol scan, diff, clean package, and
    compiled-jar gates passed. Independent review passed with no P0-P3 findings and confirmed
    single V184 ownership. Server jar SHA-256
    460c947a2be5c913619da0f87a95f1f457320b087046b761866fcbdeca461d58 and web jar SHA-256
    c0caf73ab734954cd9f972f1150927191216eb2555717656357d12e209cac77a contain V285. Runtime
    reload and live-game proof remain separate gates. Clean V285 is based on live V284 d97c1d90f.
    Divergent 349ff34a7 was a file-scoped patch oracle only; none of its alternate ancestry or V184
    ownership was merged. AI source only; no GEMP engine or player-decision changes.

  ==== V286 (2026-07-19): consolidate remaining RESPONSE scoring ====
    Shared AI-only ResponsePolicy now also owns the mirrored Sense/Alter cancel bands, shadowed
    late force-drain twin, Barrier scoring ladder, Grab ownership ladder, and cancel-target
    card-selection operations. Both bot adapters retain route recognition, AiPriorityCards and
    GEMP observations, catches, logs, action construction, Barrier turn state, Grab setScore
    behavior, early returns, and state-mutation order. Exact legacy bands and precedence remain
    unchanged, including V194's carve-out, opponent-drain +35 versus the shadowed +30 twin,
    Barrier target memory, confirmed-both Grab precedence, and own-Grab total -19998. Focused V286
    RESPONSE 32/0/0/0, all RESPONSE-named 48/0/0/0, merged RESPONSE plus V285 DEPLOY regression
    75/0/0/0, full reactor 1942/0/0/26, exact mirror parity, route/state/source boundaries, V285
    adapter preservation, AI-only scope, forbidden-symbol scan, diff, and clean package gates
    passed. Server jar SHA-256 034f4cfc6b1574abc55ef63e815e5141b3d71302065775a4ce6d9b3d93c07680
    and web jar SHA-256 90647ad3f28e934fb3859f9213c9fe6ff9e0c1dce8aec11ec9172eb337f08a1e
    contain V286 and V285. Independent review passed before rebase; production hunks applied
    without conflict and the combined regression/full gates passed after rebase. Runtime reload
    and live-game proof remain separate gates. Clean V286 is rebased onto live V285 4019e4ed0.
    AI source only; no GEMP engine or player-decision changes.

  ==== V287 (2026-07-19): close the legacy coordinator scoring lane ====
    Shared AI-only CoordinatorPosturePolicy now owns the mirrored life-force, board-posture, and
    hand-title bands +40/+30/-30/+20/+60. DeployActionTextPolicy now also owns the top-level
    DEPLOY fallback arithmetic: location +100; reinforce +80 with strict below--5 +15 and
    battleground +10; gain-ground +60 with icons, battleground +15, above-8-risk -10; domain +5;
    empty/no-friendly-icons -20; matching pilot +40. Both bot coordinators retain phase and text
    recognition, every context and board read, null behavior, independent scans, first-match
    breaks, and contribution order. Focused policy/source 24/0/0/0, combined DEPLOY/RESPONSE/
    legacy regression 336/0/0/0, full clean reactor 1948/0/0/26, exact mirror parity, boundary
    matrices, adapter-order and owner-purity gates, AI-only scope, diff, and clean package passed.
    Server jar SHA-256 07d2af4156534f3d36940366fdbcf09da461346f4f213296fccd899c8b0c9075
    and web jar SHA-256 fd01a8394fdd35ef93438c4cce4b0c8c84ca901f58b7a6ff3bad9ff5caf5182c
    contain V287. Independent review, runtime reload, and live-game proof remain separate gates.
    Clean V287 is based on live V286 fad46d13e. AI source only; no GEMP engine or player-decision
    changes.

  ==== V288 (2026-07-19): consolidate final BATTLE residual scoring ====
    Shared AI-only BattleWeaponsPolicy/Facts now own V67bi Force Lightning -9999 and Blaster
    Rack +80/-500/-500; BattleActionTextPolicy owns race destiny +50; BattleForfeitPolicy owns
    standalone dead-card +140 and pilot-on-ship +50 through StandaloneResidualFacts flags. Both
    ActionTextEvaluator and CardSelectionEvaluator mirrors retain source/action recognition, all
    GameState, card, attachment, and battle-location reads, catches, logs, action types, early
    returns, and contribution order. Dead-card and pilot contributions remain before V48, then
    the unchanged V139/V21 residual ladder. The Rack allow/block gap remains 580 points and all
    values are exact structural moves. After rebasing onto V287, the candidate passed focused
    policy/source/order 48/0/0/0, BATTLE-named 95/0/0/0, clean full reactor 1953/0/0/26, exact
    mirror parity, false-guard and boundary fixtures, AI-only scope, diff, clean package, and
    compiled-jar gates. Server jar SHA-256 is
    7b13644a10f6f4601077ec6bf07d1b69760244930f4524f2f240ed3ce7b9b26b and web jar SHA-256 is
    26f013587348da56c89561b93b0ed9fc28ea85997b281d9215cb43fbec482751. Clean V288 is rebased
    onto live V287 79de68635. AI source only; no GEMP engine or player-decision changes.

  ==== V289 (2026-07-19): consolidate force economy and utility residuals ====
    Shared AI-only ForceLossPolicy now owns the residual action-text loss/cost stream and the
    unknown-selection loss-category stream followed by V25; unmatched categories emit no operation
    or reasoning and preserve the legacy neutral base. ControlActionPolicy now owns Monnok reveal,
    make-opponent-lose, generic retrieve, and USED peek. PullActionPolicy owns the parent action-text
    take-into-hand residuals; PullSpecificActionPolicy remains card-specific and PullTakeCandidatePolicy
    remains child-only. Both bot
    adapters retain exact recognition; all GEMP, objective, card, and pile reads; scans; catches;
    logs; returns/continues; candidate base 30; and first-match positions. ResponsePolicy remains
    the sole owner of opponent-drain Sense +35 and late +30; CONTROL still owns only delegated
    V52 self-drain vetoes. Standalone/battle lightsaber protections remain -500/-400, V184 remains
    additive before retrieve, V23 still exits truly empty piles at -300, one-card no-match remains
    -10099, and Reserve Deck takes still bypass to V192. Every moved operation is ADD. Focused
    Packet E plus unchanged V286 RESPONSE tests passed 125/0/0/0; clean full reactor passed
    1972/0/0/26; normalized mirrors, all nine FORCE-LOSS action-text adapter routes, CSE category-before-V25 order,
    complete ownership/comment gates, matrices, unmatched-category no-op, thresholds, stacking, E0 nondup,
    AI-only scope, diff, package, and bundled-class/marker gates passed. Server jar SHA-256 is
    99e3ce7d2260e4e2c87f7bb36c191cccdcb789f45b7aef285c4a85553e0c2427 and web jar SHA-256 is
    f3b91b11fb733bb23d7233d5d7ddf60e987f9fc4453c8b6e4fda3cc6faf382cb. Runtime reload and live
    proof were not performed. Clean V289 is based exactly on V288 e47bb188f04a1122f332d6a2e3a98997ccdf89b2.
    Nothing was pushed or deployed. AI source only; no engine or player-decision changes.

  ==== V290 (2026-07-19): consolidate target selection scoring ====
    Shared AI-only TargetSelectionFacts/Policy now own the generic target base 50 initial score,
    beneficial own/opponent ownership, beneficial power and unique value, harmful opponent ownership,
    V51 undercover-spy priority, outside-battle power value, and harmful unique value. BattleWeaponsPolicy
    and BattleWeaponsFacts are unchanged and remain the sole owners of V51 already-hit -500, V36 destiny
    margins/priorities, and V38.3 harmful self -9999. Both CardSelectionEvaluator mirrors retain routing,
    beneficial-card recognition, all physical-card/blueprint/game/destiny/title reads, catches, logs,
    candidate order, and append behavior. Each candidate registers contributions in legacy order and
    applies one ledger once. TARGET-base is tagged initial state with no new reasoning line. Exact legacy
    totals remain: unresolved 50, beneficial own 100..150, beneficial opponent -150, harmful opponent
    outside battle 100..150, undercover 600..650, already hit -400..-350, hit plus undercover 100..150,
    and harmful self -9949. Focused TARGETING/BATTLE tests passed 28/0/0/0; the clean full reactor passed
    1985/0/0/26; normalized bot parity, raw-float and reason-byte matrices, ordered cross-owner composition,
    once-only application, source ownership, AI-only scope, forbidden exclusions, diff, package, and compiled
    class gates passed. Server jar SHA-256 is
    a5bc4b313a530201d4be29b10b437964ccbe7631deda24766af7b93398316229 and web jar SHA-256 is
    ab3fce731acd22fca673c98741c8ce6af32dd10fc6eef95b447fe6831786b969. Independent review, runtime,
    deployment, and live-game proof were not performed. Clean V290 is based exactly on V289
    08f05b05dafe5ceb9467ea9f0830cb359560d916. Nothing was pushed or deployed. AI source only; no engine
    or player-decision changes.

  ==== V291 (2026-07-19): consolidate shield candidate policy and fix residuals ====
    Shared AI-only ShieldStrategy remains the sole catalog/state/pacing/base-score owner and performs
    no board reads; it now receives ShieldFacts.FourthSlotFacts, reaches the existing turn-0 allowance
    0 -> 4, exposes null-safe minTurnToPlay metadata, and retires unused playIfWeHave executable plumbing;
    a concise V291 commented marker remains with zero live usages. Shared
    ShieldPolicy now owns one ordered dedicated/reserve candidate result: min-turn first, fourth-slot
    second, V51 Battle Order/Plan last. V53-shield-min-turn is additive VETO-labeled -5000.0f; the
    turn-1 both-theaters Battle Order/Plan exception skips it. Board shieldsOnTable >= 3 is the sole
    fourth-slot trigger, with -5000 closed/unavailable, +2000 preferred, and -5000 all other titles.
    Trigger B retains the card-function gate opponentDrainsNonBattleground in addition to own battleground,
    opponent battleground-count, and opponent drain-bonus facts. Priority remains A then C then B.
    V51 stays -9999 without both theaters, dedicated +200 above -50, and reserve +50 ready then +200
    above -50. All operations remain ADD.

    Both CardSelectionEvaluator mirrors now resolve reserve blueprint/category/canonical title before
    SHIELDS. Old -> new: blueprintId-as-title cross-talk could score a non-shield as an unknown shield;
    non-shields now remain base 50 with no SHIELDS reason. Actual reserve shields apply exact strategy
    scalars including -50/-100, producing 0/-50 from base 50. Dedicated actual shields retain SET
    semantics; null strategy is 100 before policy. Both routes apply one per-candidate SHIELDS ledger
    once, enforce fourth-slot policy from authoritative board count, and discover preferred shields in
    card-id, arbitrary-blueprint, and blueprint-only menus. Unknown actual shields remain +50. Exact
    turn-1 both-theaters totals remain dedicated 280 and reserve 380. Existing V112 then V117 mixed-menu
    order is unchanged, and the early -5000 remains effective even beside preferred +2000.

    Activation provenance is corrected in both bot entry points. Own-shield observation still records
    successful shields but no longer increments K&D/AFA activation count. Exactly one increment occurs
    only after DecisionSafety validates an aligned top-level Play a card result and the resolved physical
    source title is Knowledge And Defense or Anger, Fear, Aggression. The mirrored write-only
    hasShieldsToPlay fields are retired from executable state; concise V291 commented markers remain
    with zero live usages. All adapter routing, engine reads, catches, logs, candidate order,
    response format, base categories, V29/V42/V43/V53 behavior, state order, and normalized parity remain;
    evaluateGrab/V53 grab targeting is excluded.

    Clean focused SHIELDS tests passed 42/0/0/0; full reactor passed 2001/0/0/26. Raw-float/order,
    both-bot parity, no-double-application, source ownership, activation provenance, AI-only path,
    retired-field live-usage/forbidden-symbol, diff, clean async package, and bundled class/marker gates passed. Server jar
    SHA-256 is b1386a6eb693f4e29013e5480abf11b21588b3b2f0c46a4e9d15f1a14bc44a52 and web jar SHA-256 is
    23546bb85e393bbc6d64385b77cdc75eed06210ff7d4bba560b8c50bedf6d314. Clean V291 is based exactly
    on V290 49704129c5b59a1c5c4ea59ca035a97062510ec6. Nothing was pushed, deployed, or run in a game.
    AI source only; no engine or player-decision routing changes. Exact revert boundary: revert the single
    V291 commit to that parent, restoring only this packet's AI, SHIELDS tests, and assigned docs.
  ==== V292 (2026-07-19): consolidate MOVE residual scoring ====
    Existing shared MOVE owners now own the remaining live scalar arithmetic from MoveEvaluator,
    ActionTextEvaluator, and CardSelectionEvaluator: missing source; capacity replacement plus
    contribution; embark, residual transfer, and ship-dock; weak split and join destination;
    ordered icons, power, and battleground tri-state; Cloud City and Hidden Path objectives;
    break-cover and spy dilution; Lando support/stay; independently stacking system penalties;
    and Evazan partner movement. Both bot adapters retain recognition, all GEMP/card/objective
    reads, loops, first breaks, catches, logs, setScore, action types, candidate order, append,
    terminal continue, contribution positions, and the battleground scoring catch envelope.
    Exact normalized mirrors and source ownership are enforced. Focused tests passed 117/0/0/0;
    the clean full reactor passed 2028/0/0/26; async packaging passed. The complete forbidden-symbol,
    AI-only scope, diff, destination-order, catch-envelope, and adapter-control-flow gates passed.
    Server jar SHA-256 is 1c033bb2ff2344c102ef9865fbccea95736e06c2adea3e56b163f7457376927a and web jar SHA-256 is
    d201ce1d7ebe74c47a3186c742fb902e846b3de0986ae3aa56abc40fe7913a3b. Clean V292 is based exactly
    on V291 b75bec70d286279de06216c9198e0a645bfbe386. Nothing was pushed, deployed, or run in a game.
    AI source only; no engine or player-decision routing changes. Exact revert boundary: revert the
    single V292 commit to that parent, restoring only this packet's MOVE AI and assigned docs/tests.
  ==== V293 (2026-07-19): consolidate deploy plan ranking policy ====
    Shared AI-only DeployPlanRankingPolicy now owns the pure scalar DEPLOY-1 candidate-plan stream:
    per-instruction power*2; ordered V22 objective-location bonus; favorable, marginal, or losing
    tactical delta; contested destiny or vulnerability delta; one composite establish delta; and the
    separate V22 Bespin-capital +200 adjunct. Every operation remains ADD in DEPLOY_SEQUENCING with a
    unique internal contribution identity. The internal ledger sums contributions without emitting
    policy reasons to action reasoning or logger output. The additive -500 BLOCKED annotation did not
    become a hard veto.

    Both DeployPhasePlanner mirrors retain null/empty handling, instruction scan and maps, V32 fallback
    per instruction, card/location matching, all board and ObjectiveAnalyzer reads, exact V22 warning,
    generation and equal-score insertion order, catches, and early-game behavior. Only the
    objective_capital_bespin candidate receives the adjunct after scorePlan; early rescore remains 281
    in the +150 objective fixture. Existing apply-side DeployPlanPolicy is byte-unchanged. Exact raw-bit
    vector [38,65,131,-452,50,75,481] and sorted-domain parity passed for both bots. Focused policy,
    adapter, ownership, phase, apply-side, and ledger tests passed 31/0/0/0; the full reactor passed
    2039/0/0/26 across 282 suites; normalized mirrors, scope, forbidden exclusions, diff, async package,
    and packaged-class gates passed. Server jar SHA-256 is
    08648bf4de37c4f453323aea1c4e7932d2ce8e2e9f5a89e362aa7db2d8e77453 and web jar SHA-256 is
    6d017d849b1f1e41d7910efb34c33a4d96ac06abd0bd23868224e02d613208a9. Source ownership, tests,
    compilation, and packaged-jar presence are complete proof layers. JVM load, deployment, replay,
    sandbox-game, and live-game proof remain separate and were deliberately not performed. Clean V293
    is based exactly on V292 54e53bf7fd251f4923a84c1b0e6012c734c5a37f. Nothing was pushed or deployed.
    Revert the single V293 commit; V292 and the unchanged apply-side owner remain independent. AI source
    only; no engine or player-decision changes.

  ==== V294 (2026-07-19): consolidate final CONTROL utility scoring ====
    Shared AI-only ControlActionPolicy now owns the final two live CONTROL action-text utility leaves:
    CONTROL-steal is additive ORDERING +30 with reason "Stealing is good"; CONTROL-dangerous-card is
    additive ORDERING -50 with reason "Known dangerous card". Both mirrored ActionTextEvaluator adapters
    retain the exact steal, Stardust, and On The Edge classifiers and their first-match positions. Steal
    still assigns ActionType.STEAL. Only the duplicate inline scalars moved into the existing shared owner.

    Behavior is structural-only: exact scores remain 30/-50; action type, order, case handling, reason bytes,
    adjacent CONTROL routes, GEMP reads, response format, and player-choice routing are unchanged. Focused
    shared-policy, adapter-parity, and source-ownership tests passed 11/0/0/0; the clean full reactor passed
    2041/0/0/26 across 283 suites; async packaging passed. Normalized mirrors, exact raw floats and trace
    fields, one-owner call counts, source order, forbidden-symbol, AI-only scope, and diff gates passed.
    Server jar SHA-256 is 0a88052ce2cb3e60474758d7aa9ab7c03e3643dc7e453d365d5bf1a32c17d1ac and web jar SHA-256 is
    5e7470121f7fdf626d5b87497af7038ea2e6ae94cbbd072f0076823a5c11f78f. Clean V294 is based exactly
    on V293 716d3efaa298649312d3fcd1aaeb77dcfadedff6. Nothing was pushed, deployed, or run in a game.
    AI source only; no engine or player-decision changes. Revert the single V294 commit; V293 and every
    earlier CONTROL owner remain independent.

  ==== V295 (2026-07-19): retire inert AI scaffolding ====
    Both AI entry points and DecisionContext mirrors now retain the dead ObjectiveHandler transport
    lifecycle only as V295 comment markers: import, field, construction, startup log, public getter,
    context injection, and resets. The two ObjectiveHandler classes remain packaged as explicitly marked
    historical recovery references; ObjectiveAnalyzer remains the only live objective intelligence owner.

    The configured-0% chaos bypass is likewise comment-only in both entry points and configs. Its dedicated
    Random, startup log, helper, and route-recording call sites are retired together. The active path now
    directly executes the same evaluator-then-heuristic branch that the constant-zero guard always selected.
    TraceRoute.CHAOS_FALLBACK remains in the shared enum vocabulary with a V295 retired marker. Both
    CardSelectionEvaluator and CombinedEvaluator mirrors also retain their zero-read Random imports and
    fields only as V295 comments. Active personality, holiday, battle-prediction, and safety RNG owners stay live.

    DecisionTracker.blockedResponses, turnBlockedActions, DecisionContext.blockedResponses, and evaluator
    loop-protection checks remain untouched. offeredConcedeThisGame, finalization fixtures, V67 comments,
    StrategyController, and all other residuals are deferred. No score, weight, classifier, action order,
    response, phase route, GEMP observation, card rule, objective rule, engine decision, or player-choice
    contract changed. Focused V295 source ownership passed 5/0/0/0; the clean full reactor passed
    2046/0/0/26 across 284 suites; async packaging passed. Comment treatment, enum stability, active RNG,
    blocked-response preservation, normalized mirrors, forbidden exclusions, AI-only scope, and diff gates
    passed. Server jar SHA-256 is a4b00f7d8f2314ef22903e43e867c13f645a3c19f5462c5ba5b848418c89fe5a
    and web jar SHA-256 is 8170fbb7059ce0c5d05b40dba222cbb91f1d308441273960216834e90eb489b0.
    Clean V295 is based exactly on deployed V294 e29a12be91cf8e2b3488e26e802d1e438e436642.
    Nothing was pushed or run in a game. Revert the single V295 commit; every live phase owner remains.

  ==== V296 (2026-07-20): restore deploy-plan execution and drain contact ====
    Replay d77g2od7m8irp1mm exposed four AI-only execution faults. I Want That Map's setup fragment
    incorrectly made every live Starkiller Base location objective-relevant, giving Finalizer a false
    +150 at a non-battleground. FormationSafety hard-vetoed the first ability-below-4 body of an exact
    affordable five-character Night Club wave. Apply-side plan weights could still lose to unrelated
    destination arithmetic. Two-step starship destination selection did not reuse the existing V36/V51
    drain-contact logic, and remote attack targets could score unrelated parent moves before adjacency.

    V296 keeps the exact Starkiller Base setup choice but requires a battleground for live physical
    objective relevance. A contested weak first body is allowed only with an exact affordable planned
    companion after it. Selectable exact plan destinations defer alternatives, while unavailable targets
    preserve fallback behavior. Survivable starship entries into an opponent drain reuse V36/V51 only when
    current plus deploying power is at least raw opposing power. Move attack scoring now requires adjacency.

    Finalizer changes from false Starkiller Base 230 to 80; the survivable power-10 entry against Rey/Red 5
    at Jakku power 8 and drain 2 receives the unchanged V51 +500. Losing entries receive no bonus. The exact
    Night Club wave can start, but no-companion and unaffordable weak solo deployments remain blocked. Grand
    Admiral Thrawn was ability 4, not below 4; plan dominance fixes his separate wrong Forest destination.

    Focused replay regression passed 33/0/0/0; the expanded policy/parity suite passed; the clean full reactor
    passed 2051/0/0/26 across 284 suites. Exact normalized Rando/ChosenOne card-selection and move parity,
    source characterization, AI-only scope, and diff gates passed. Clean async packaging passed, and the
    compiled V296 classes and markers are present in web.jar. Server jar SHA-256 is
    3470eeeb352a7bb0950b74902e8179ea1f50502ea7827c93d76809d1a666118f; web jar SHA-256 is
    b87e7db1596d8a606d7facdd908c850c3f730f57585f0e4f8fa429f8afdc771c. Runtime reload and live-game
    proof are separate gates. No engine, card, action, decision metadata, objective data, player-choice,
    database, client, or deck-library source changed. Revert the single V296 commit.

  ==== V297 (2026-07-21): require a supported Invasion Throne Room formation ====
    An unflipped actor-gated objective now builds one exact formation at its exact control site. For
    Invasion, Rando must deploy a Neimoidian to Naboo: Theed Palace Throne Room with an existing friendly
    character or a second exact affordable planned character. An unsupported Neimoidian no longer receives
    the V193 objective steer. A complete formation receives the existing +1600 Invasion playbook weight and
    survives the early-game hold check. After the actor arrives or the objective flips, the planner maintains
    at least two friendly characters there and reinforces again when it still does not control the site.

    Endor's existing one-body Bunker behavior remains unchanged. Unsupported actor-only plans retain V201
    solo safety, while direct and destination V193 scores require the same support fact. Focused tests passed;
    the clean full reactor passed 2055/0/0/26 across 284 suites; normalized bot parity, exact target, funded
    buddy, existing buddy, actor-only rejection, post-flip reinforcement, early-hold, diff, and AI-only scope
    gates passed. Async packaging and deployment passed. Deployed server jar SHA-256 is
    37091a8e6710947bdf1a5cbc22d3527d292d1f1e2813f763d09a0d0614bc347b and deployed web jar SHA-256 is
    4796c60c562248128dc4a9e8078ea0098f09d8600c43c68bc6633fbe7e926cde. A fresh container loaded the jar
    after its build timestamp and returned HTTP 200. No engine, card, action,
    decision metadata, objective data, player-choice, database, client, or deck-library source changed.
    Revert the single V297 commit; V296 and all earlier AI-only behavior remain independent.

  ==== V297 follow-up (2026-07-22): keep and recover the Invasion gate formation ====
    Replay ew2j3ds3qnkx55ks showed the original V297 deploy contract working: Nute Gunray, Darth Sidious,
    and two droids reached Naboo: Theed Palace Throne Room. MOVE then evacuated that formation one card at
    a time because survival +12000, consolidation +600, and drain routing +150 did not preserve the shared
    objective plan. Later force-loss choices discarded backup Neimoidians, and an equal weapon-target tie
    sent the rifle away from the Throne Room formation.

    A new shared MOVE gate-hold policy keeps a defensible contested exact actor gate together while the
    objective is unflipped. An uncontested gate keeps its last required actor and that actor's last buddy.
    Opposing effective power more than 6 above friendly power, including the existing weapon heuristic,
    still permits retreat. The ladder hard veto resolves to -100000, so the replay's +12000/+600/+150
    contributions cannot dismantle a formation the objective planner deliberately built.

    ObjectiveAnalyzer now exposes actor-only, exact-gate, and actor-count facts from the same V297 filter.
    Both CardSelection mirrors use the physical loss candidate, so the existing V21 -9999 objective-critical
    protection dominates duplicate-zone +1000 and produces -8949 with the unchanged base. Legal weapon
    targets at the active exact gate receive a shared +250 tie-break when the required actor is present.
    The bonus cannot bypass weapon legality or second-weapon protection.

    Focused policy, adapter, objective-gate, force-loss, deploy-weapon, and parity suites passed. The isolated
    clean full reactor passed 2064/0/0/26 across 286 suites; diff and async packaging passed; and the shared
    policy plus both mirrored adapters are present in web.jar. Built server jar SHA-256 is
    176e8473289474c3ae396201e2fd0260d6c3117fc22073da4542e933f0ff740a and deployed web jar SHA-256 is
    98b5f946fbd2eafc1e41058fda1f01725292b14c83310b656da23fe8e1adf83b. A fresh container matched the host
    jar hash, started after the build, and returned HTTP 200. Fresh live-game branch firing remains a
    separate gate. No engine, card, action, decision metadata, objective data, player-choice,
    database, client, or deck-library source changed. Revert this V297 follow-up commit independently.
