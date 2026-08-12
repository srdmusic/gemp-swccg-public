# DB 72282 Batch 1 live audit

## Verdict

DB `72282` is positive live proof that Batch 1's objective-critical band can fire on the selected physical deployment and reach the intended public board consequence. It is not positive proof of the persistent two-turn-drain bonus. The Dining Room lane met that exact history precondition one turn later, but Rando had no viable response formation and the policy failed closed.

| Fact | Exact value |
|---|---|
| Runtime game ID | `4bfb6bf5b40a-52f9-7cd1-0c12-f7691f10` |
| Match | `~Rando_Cal`, Dark `26SD D1 Tashima DS TDIGWATTv`, versus `~The_Chosen_One`, Light `1 Rey` |
| Result | Rando won by natural Light Life Force depletion |
| DB interval | `18:24:51.173` to `18:25:00.841`, 9.668 seconds |
| Replays | Rando `u8l2wwlozk6qby13`; Chosen One `azhk63rhjapg25kg` |
| Final public fingerprint | Both sides `f24489dfa1215c2507fb514fd818f601ec1a2c2778c12e3a6b3828cce99ffc6e` |
| Exact log | `logs/2026-08/app-08-12-2026-3.log.gz`, lines `16592-44647` |

The evidence contract and natural terminal are recorded in `evidence_reports/20260812-182501-152563Z-PASS-4bfb6bf5b40a-52f9-7cd1-0c12-f7691f10.json`. Both recording-ID replays contain one 715-message final segment. Replay `D` elements were not used to infer scores or returned choices.

## Exact persistent-lane precondition

Batch 1 records final `ForceDrainState.getForcePaid()` by the location's permanent ID, then requires positive payment on the latest and immediately previous completed opponent turns. See `PersistentResponsePolicy.java:167-177`, `226-241`, and `317-320` at source commit `970851a7`.

Chosen One paid and completed the same Dining Room drain on three consecutive Light turns:

| Light turn | Final-segment public replay | Paid | Completed drain |
|---|---|---:|---:|
| 4 | `M465-M467` | 3 | 2 |
| 5 | `M589-M591` | 3 | 2 |
| 6 | `M664-M666` | 3 | 2 |

The lane first qualified when Light turn 5 completed. Rando's Dark turn 5 Lower Corridor deployment happened after only the turn-4 drain, so the two-consecutive-completed-turn precondition was not yet true.

On Dark turn 6, the lane was current, but the response was not executable. Exact logs `42452-42460` show Dining Room at effective power 18, Rando's projected wave only 7, and Formation Safety rejecting Sergeant Merril as a weak solo. Logs `42480-42488` show Rando instead selected the legal planned deployment to West Gallery, increasing the virtual objective's stable back-side cushion. This is why no `deploy-persistent-response-selected` reason appears. The rule requires both the repeated lane and a viable exact formation (`PersistentResponsePlanAdapter.java:407-444`; `PersistentResponsePolicy.java:763-775`). The third paid drain occurred after that deploy window, and Dark won during its next control phase before another deploy phase existed.

## Batch 1 markers and V182

One Batch 1 positive marker fired:

- `app-08-12-2026-3.log.gz:38673`: Aurra Sing With Blaster Rifle to Lower Corridor received `+250`, reason `Selected executable response clears a typed objective-critical location; target=Cloud City: Lower Corridor#164`. The source rule ID is `deploy-objective-critical-eviction-selected` (`PersistentResponsePolicy.java:31-36`, `669-699`).

Its exact precondition was: the TDIGWATT objective was already flipped, Lower Corridor was classified as `POST_FLIP_PROTECTION`, Han was still an opposing battle participant there, the selected plan's exact formation was viable, and Aurra's exact planned destination remained current (`PersistentResponsePlanAdapter.java:775-795`). Logs `38656-38673` show the resulting 14-versus-4 effective formation and selected score 3105. Public replay `M524-M525` confirms Aurra deployed to Lower Corridor and Rando immediately battled. The `+250` fired, but was not independently decisive: removing it still leaves that logged destination above the 2055 runner-up.

The post-deploy warning at log `38779` says Aurra left hand without being proven at the exact target, so the obligation was cleared. Public replay proves she did land at Lower Corridor. This is a bookkeeping-proof warning after successful execution, not evidence that the deployment failed.

No `deploy-persistent-response-selected` marker and no `V182 RESPONSE BANK` marker occurred. Rando did emit ordinary, pre-existing `V182 BANK FORCE` at Dark turns 4 and 6:

- `35807`: need 5, Force pile 4, generation 13.
- `43021`: need 9, Force pile 8, generation 13, after the Dining Room history qualified.

Those lines are the generic offensive-army bank in `DrawPhasePolicy.java:150-160`. They are not the exact selected-response bank, whose distinct code and log text are `V182-response-bank` / `V182 RESPONSE BANK` at lines `165-195`.

## Why Rando won

Rando flipped TDIGWATT on Dark turn 2 (`M204-M205`) and kept the back side stable. The decisive action was the Dark turn-5 Lower Corridor eviction: Garindan, Vader, and Aurra deployed there (`M520`, `M522`, `M524`), Rando initiated battle (`M525`), hit Han to forfeit zero, won 17 to 7 (`M537-M539`), and dealt 10 battle damage (`M548-M571`).

Rando then converted board control into 21 completed drain damage across Upper Walkway, Bespin, Lower Corridor, and West Gallery. Chosen One produced only 6 completed drain damage at Dining Room. Dark turn 7 delivered drains of 2, 1, 1, and 3, then Light depleted naturally (`M686-M715`).

Behavioral classification: strong proof that the Batch 1 critical band fired and reached deployment plus battle; strong fail-closed proof once the paid lane qualified; no positive proof of the persistent bonus or Batch 1 response-bank branch. The exact window contains no error, fatal, exception, timeout, abort, deadline, or AI-chain failure.
