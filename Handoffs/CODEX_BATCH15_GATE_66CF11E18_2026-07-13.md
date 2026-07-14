# Batch 1.5 Gate: 66cf11e18

Date: 2026-07-13
Reviewer: Codex/Alfred
Scope: local commit `66cf11e18`
Code verdict: `ADVANCE`
Program deploy verdict: `HOLD`

## Commit shape

- Six files changed: two changelog/history files and four mirrored evaluator files.
- Java delta per bot:
  - `CardSelectionEvaluator.java`: `+10/-498`
  - `DeployEvaluator.java`: `+9/-345`
- Total commit: `+62/-1686`.
- `git diff --check 66cf11e18^ 66cf11e18`: clean.

## Source gate

- Rando and ChosenOne evaluator diffs contain only expected package/strategy namespace changes.
- All eleven V159 constant-false branches per bot are absent after the purge.
- The old constant-false V67t branch was removed while its always-executed former `else` body
  remains unconditional:
  `action.addReasoning("Optional forfeit but zero forfeit value", -80.0f)`.
- Four live objective-protection call sites remain per bot at current lines 4370, 4500, 4633,
  and 4962.
- V122, V67as, the Deploy objective-consolidation comment region, and the live V193 gate are
  unchanged by the deletion hunks.
- Current main-tree compile passed with exit `0`.

## Independent bytecode gate

Two isolated full source archives were created directly from `66cf11e18^` and `66cf11e18`.
Each archive contained 7,323 files. Both were built with the same Maven and JDK:

```text
mvn -q -pl gemp-swccg-server -am clean compile -DskipTests
Maven 3.9.14
OpenJDK 25.0.2
```

Both builds exited `0`.

For each changed class, Codex generated:

```text
javap -classpath gemp-swccg-server/target/classes -p -c -s -constants <FQCN>
```

The complete normalized instruction/signature/constant listings match byte-for-byte pre/post:

| Class | Pre and post SHA-256 |
|---|---|
| Rando `CardSelectionEvaluator` | `79c756b318ebb2624cac170495642f221acdf754e6ee957614206a959df5f759` |
| ChosenOne `CardSelectionEvaluator` | `66ff724739c0bc097b07dc6a83886c30b8bbff656ef65ebc2468b87de40d3dae` |
| Rando `DeployEvaluator` | `33dcf207e0b1d5e3f506a912c10f16a732cb036126c9db03b28758cdbf319a21` |
| ChosenOne `DeployEvaluator` | `dd8f60beddcf89c00ce97505a3da241357c0cb3b01596f5a07ba448b613e698c` |

Raw class or JAR hashes were not used because source-line and archive metadata intentionally
change during comment deletion.

## Documentation warning

The Batch 1.5 changelog groups the artifact deletions with this commit, but those five files were
actually deleted by earlier commit `e5b393955`. The Java snapshots are valid artifacts. The two
game logs were evidence inputs and must be restored or durably relocated if an active handoff still
cites them. Correct the provenance before final program release.

## Verdict boundary

`66cf11e18` is accepted as an executable-behavior-neutral cleanup commit and may remain in the local
program branch. This does not authorize deployment. Batch 1 correctness defects, the exact fixture
oracle, route shadowing, and the broader program cutover remain held independently.
