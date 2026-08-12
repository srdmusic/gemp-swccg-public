# DB 72279 exact-match behavior audit

## Verdict

This is a valid autonomous-harness and natural-terminal proof. It is partial AI behavioral proof, not a blanket proof of every August 12 repair batch.

| Fact | Exact value |
|---|---|
| `game_history.id` | `72279` |
| Runtime game ID | `114f29e633c3-e789-91e1-b420-28691f10` |
| Match | `~The_Chosen_One`, Light `1 Rey`, versus `~Rando_Cal`, Dark `EOPS` |
| Result | Chosen One won; Rando lost by natural Life Force depletion |
| DB interval | `18:17:23.422` to `18:18:05.764`, 42.342 seconds |
| Audited log interval | `18:17:23.423` to `18:18:05.764` |
| Replays | `4y7m5x2e270vw15y`, `v7dl40tkgbxvyjjg` |
| Final public fingerprint | Both sides `83529003e3e30ea3297c40f9d355a08e301259ff05e1e1ae3816ae4cad643351` |

The controller registrations are anchored at `logs/2026-08/app-08-12-2026-1.log.gz:53607-53608`. The DB, controller, replay, Hall, and natural-terminal invariants are recorded in `evidence_reports/20260812-181806-053614Z-PASS-114f29e633c3-e789-91e1-b420-28691f10.json`.

## August 12 batch classification

| Repair | Classification | Exact evidence |
|---|---|---|
| Batch 1 persistent response | Reachable engine destination, but no executable response and no Batch 1 firing | After consecutive Light drains at Dark Forest, Rando replay `D657` (`ge=2849`) legally offered Prince Xizor deployment to `cardId=256`, Dark Forest. Logs `87918-87923` show V172 could not match the 15 effective-power stack and Formation Safety L4 rejected the weak solo. Rando selected Landing Platform at `87940-87943`. No `B1 PERSISTENT_RESPONSE`, `deploy-persistent-response-selected`, or critical-eviction marker occurs. This is fail-closed evidence, not a missed response and not Batch 1 positive proof. |
| Batch 2 exact formation versus immediate react | Not evidenced | No `Proven immediate react dominates empty target packet` marker occurs. The replay does not prove the exact public move-as-react exposure required by the rule. |
| Batch 3 retention telemetry | Fired 31 times | First anchor `62634`; later anchors include `74380`, `74384`, `88476`, `88481`, and current-log `9571`. Every occurrence is `RAW_PREDICTOR_ONLY`, `assessment=UNKNOWN`, `score=0`. This proves the telemetry seam ran. It does not prove any battle-selection change, because Batch 3 deliberately adds zero. |
| Batch 4 response bank | Not evidenced | No `V182 RESPONSE BANK` marker occurs. There was no logged Batch 1 funded response obligation to bank for. |
| Batch 5 exact MWYHL flip | Not applicable | Light used `1 Rey`, not MWYHL. No `OBJECTIVE.MWYHL.FLIP` marker occurs. |
| `601_87` Hunt Down Legacy | Not applicable | Dark used `8_167` Endor Operations. No `601_87` or Hunt Down Legacy marker occurs. |
| WMAOP boundary tests | Behavior-neutral | The WMAOP work was test-only. This game supplies no separate WMAOP behavioral claim. |

Replay `D` elements were used only to establish legal candidate parameters. They were not used to infer evaluator scores or returned choices. Scores and selected actions above come from the exact server log.

## Strong separate EOP proof

This match supplies strong live proof for the previously shipped Endor Operations flip-gate behavior:

- Logs `55684-55700`: V193 `(CS) FLIP-GATE CONTROL +2000` made Boba Fett to Bunker the selected destination at score `1135`.
- Final-segment public replay: Rando deployed Boba to Bunker (`M75`, `ge=243`), Establish Secret Base on Bunker (`M77`, `ge=255`), and Dark Forest from Reserve (`M80`, `ge=267`).
- Rando then deployed Ominous Rumors and the objective flipped to Imperial Outpost on Dark turn 2 (`M139-M141`, `ge=520-525`).

This traces source-visible reasoning through the selected action and public board consequence. It is genuine behavioral proof for that EOP branch.

## Dark Rando gameplay

| Area | Observed behavior |
|---|---|
| Objective | Fast and correct opening. Bunker was seized, Establish Secret Base deployed on turn 1, Ominous Rumors followed, and EOPS flipped on turn 2. |
| Deployment | Rando repeatedly reinforced Landing Platform and maintained Bunker. It also opened a remote attack at Cloud City: Lower Corridor with Vader and P-59 on turn 5. |
| Battles | Rando initiated five battles and won all five. Across all nine battles, Dark won six. The five selected attacks include clear favorable cases at log anchors `63548-63550`, `68809-68811`, and `74392-74395`; later must-fight or predictor-backed attacks at `79382-79385` and `88488-88491` also won. |
| Drains | Rando completed six drain-2 actions at Lower Corridor for 12 damage. Chosen One completed 19 drains for 26 damage, principally recurring Dark Forest and Endor lanes. |
| Movement | Rando made zero public movement actions. Chosen One made two. Rando's response was deployment and battle, not repositioning. |
| Strategic result | Good objective tempo and winning battle selection did not overcome the drain race. Rando held its EOP engine and fought efficiently, but never removed the recurring Dark Forest and Endor pressure. Final public state was Dark Life Force `0` versus Light Life Force `14`. |

## Runtime health

The exact window contains zero `ERROR`, `FATAL`, exception, out-of-memory, botgame abort, timeout, deadline, or AI-chain markers. The many `WARN` lines are normal AI decision telemetry. The match completed naturally and produced one paired DB row plus both recording-ID replays.
