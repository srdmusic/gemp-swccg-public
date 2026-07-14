# Codex Verification: d92bc3a3c

Date: 2026-07-12 PDT

Owner: Codex/Alfred, read-only verification for K-2 mailbox `m00200`

## Verdict

**FAIL.** The new pull-route block is mirrored, bundled, and running at HTTP 200, but its
target resolver does not resolve Krennic from the exact `216_16` source text. The safety code
therefore never executes on the route it was added to close.

## Blocking Reproduction

Actual source:

- `Card216_016.java:40`: `May [download] Krennic here.`
- `Card216_016.java:52-58`: the engine action deploys persona Krennic from Reserve Deck to
  `Filters.here(self)`.

Deployed-jar JShell output:

```text
DeckOracle.parseSourceCardPullTargets("May [download] Krennic here.")
=> [krennic here]
```

The new `ActionTextEvaluator` block compares each parsed target directly with the Reserve card
title:

```text
rt = "krennic, death star commandant"
t  = "krennic here"
rt.contains(t) = false
t.contains(rt) = false
```

`fsPulled` remains null. The flip-plan exemption, `vetoCharacterDeploy()`, and the `-800`
no-plan score are all skipped.

Result: the stated turn-4 boundary does not exist in this build. The action remains `350`,
which clears the `50` non-bucket epilogue floor, instead of becoming `350 - 800 = -450`.

## Replay And Log Grounding

Replay `replays/asdf/ocffe8duo7yxh7fh.xml.gz` contains five full-history segments. In the last
segment, beginning at event 3096:

- Events 3436-3442: Command Center pulls Krennic; objective `216_11` flips immediately. This is
  the legitimate first-pull exemption case.
- Events 3461-3551: Krennic moves to Citadel Tower, is hit, forfeits, and is lost.
- Events 3607-3624: with the objective already flipped, Command Center pulls Krennic again.
- Events 3749-3873: the opponent attacks Command Center and Krennic is lost again.

Pre-fix logs show the causal arithmetic:

- `logs/gemp-swccg.log:78083`: V192 deploy-grade base is `150`.
- `logs/gemp-swccg.log:78092-78093`: merged score `350` clears the epilogue floor and Krennic
  is deployed again.

## Requested Checks

| Check | Result |
|---|---|
| Rando/chosenone parity | PASS, normalized commit hunks match. |
| `216_11`/`216_16` source semantics | PASS for intended design, but unreachable because target resolution fails. `216_11:41` names Krennic in the front-side flip condition; `216_16:40,52-58` forces Krennic to this site. |
| First pull exemption | PASS, static after resolver repair. Parsed flip text contains `krennic` while the objective is unflipped. |
| Turn-4 hold | FAIL. No `-800` reaches the action. |
| Pull landing with friendlies | PASS for friendlies contributing positive total ability; `landsSolo` is false. Ability-zero friendlies remain an edge case. |
| Jar and health | PASS. Both bot class markers are in `web.jar`; HTTP is 200. |

## Minimum Correction

Normalize forced-location syntax before title matching. For this route, stripping a terminal
` here` converts the parser output to the intended identity key:

```text
krennic here -> krennic
```

Required deployed assertion:

```text
parsed target = krennic
resolved Reserve character = Krennic, Death Star Commandant
unflipped first pull = flip-plan exemption
flipped re-pull with no buddy = 350 - 800 = -450 < 50 floor
```

P1 follow-up: the flip exemption currently accepts any four-letter title word found anywhere
in the flip condition. That is broader than identity matching and can false-exempt titles that
share words such as `death`, `star`, `jedi`, or `commander`.

## Forced-Location Pull Corpus Sample

The same deployed parser was run against representative location text:

```text
KRENNIC=[krennic here]
REY=[rey here]
EMPEROR=[episode vii emperor here]
PADAWAN=[padawan except anakin here]
LANDO=[lando here]
BOBA=[from lost pile boba fett here]
```

A source-tree scan found 42 location-game-text lines containing `here` plus `[download]` or
`from Reserve Deck`. With the deployed parser:

```text
candidateLines=42
emptyTargets=1
onlyHereTarget=3
targetEndingHere=35
```

Trailing-`here` normalization repairs literal-name routes such as Krennic, Rey, and Lando. It
does not make the current title-substring resolver generic: icon-qualified, category, exception,
and Lost-Pile-alternative targets still need typed/filter-aware resolution. Examples include
`Card214_004` Emperor, `Card213_029` Padawan-except-Anakin, and `Card221_034` Boba Fett.

No post-`d92bc3a3c` game was active when this verification completed.
