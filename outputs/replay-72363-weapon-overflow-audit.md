# Replay 72363: early deployment and solo hit removal

Evidence captured September 13, 2026 PDT. These games ran V311, not the
source-only V312 changes. No new behavior is claimed to have fired.

## Recordings

- Game 72363, September 14 04:08:13 to 04:16:37 UTC: asdf, Z H,
  versus dark Rando, 1 Dooku. The final replay ends in mutual cancellation;
  the database winner field is not evidence of a played-out victory.
- Rando: `/Users/steve/gemp-swccg-public/replays/~Rando_Cal/aaudbarbftuftrid.xml.gz`.
- asdf: `/Users/steve/gemp-swccg-public/replays/asdf/a2twwyiz7i9gvljl.xml.gz`.
- Raw zlib XML, canonical final segment begins at zero-based event 1713.
- Scores and returned choices come from
  `/Users/steve/gemp-swccg-public/logs/gemp-swccg.log`, not replay decision offers.

## Early deployment

| Own turn | Force at deployment | Evidence |
| --- | ---: | --- |
| 1 | 7 | Savage was planned at Lothal: Jedi Temple, but cost 8 there and that destination was not offered. Nute plus Asajj cost 8 even at Hoth, so no affordable two-character group existed. |
| 2 | 10 | Temple was offered, but the planner again chose Savage alone. V156 solo protection rejected that plan. Nute plus Aurra could have deployed at Hoth for 7, or Nute plus Asajj for 8. |
| 3 | 20 | Kanan occupied Temple, removing its surcharge. V308 concentrated Savage, Asajj, Aurra and P-59 there and initiated battle. |

Actual `Card219_041` imposes +2 deployment/movement cost on the opponent's
characters while Light does not occupy Temple. There is no turn-3 deployment ban.

On turn 2, establishment planning ranked Temple's drain 3 first. The combination
finder accepts one-card subsets, so Savage's power 8 satisfied its minimum-power
"group" requirement. Planning reserved that budget and did not construct a funded
Hoth pair. The destination evaluator then applied V156's -600 solo guard, leaving
Temple at -25 and causing cancellation. This is a planner/safety disagreement.

Log references: turn 1 resources and plan 2532-2550, offered destinations and
cancellation 2610-2628; turn 2 resources and one-card group 4838-4860, destinations
and cancellation 4947-4974; turn 3 resources 7691 and V308 concentration 7711-7718.

V312's paid legal-target query addresses the missing turn-1 target. It does not
fully address turn 2 because Temple is then legally offered. A separate future
repair should align establishment packages with existing solo safety and exact
site costs, then consider an affordable alternate site. Do not weaken V308's
successful own-turn battle concentration to solve this.

## Sniper and overflow

At final-segment events 2078 and 2092, Dooku's Lightsaber on Asajj drew 4 and 6.
Event 2105 hit Kanan and reset his forfeit to zero. Rando then played Sniper &
Dark Strike (10_47) and made Kanan immediately lost (2122, 2129). No battle-damage
resolution followed. Kanan was the lone enemy battle character.

The card's immediate-removal mode is distinct from its +3 weapon-destiny mode
and its control-phase weapon shot. The correction requested here protects the
last defender during a battle; ordinary control-phase shooting and removal of
one character from a larger opposing battle force need not change. No exact
counterfactual overflow total is asserted, because destiny resolution never ran.

## Related second-weapon failure

Game 72362, Rando recording `jo1nca111dzkilfo`, proves Asajj Ventress' Lightsabers
(221_9, card 228) and Mara Jade's Lightsaber (110_11, card 229) both attached to
Mara (226_8, card 227), at replay events 246 and 269. Evil Is Everywhere initiated
the first Reserve Deck deployment. Mara's abbreviated own ability initiated the
second, bypassing parent checks that required the words "from reserve".

The second parent won at +130, log 1340-1342. Its mandatory child received the
existing -9999 second-weapon penalty but still selected the only candidate,
log 1433-1436. The prevention must run before the optional parent is committed,
and must also keep armed hosts out of subsequent weapon-target choices.
