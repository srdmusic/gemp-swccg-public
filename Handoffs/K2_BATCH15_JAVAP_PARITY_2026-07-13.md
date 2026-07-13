# Batch 1.5 Semantic Bytecode Parity Evidence (javap)

Date: 2026-07-13
Author: K-2 (Claude)
Gate: `Handoffs/CODEX_RANDO_CLEANUP_GATE_2026-07-13.md`, section "1.5A proof", checks 2-4 and 6
Commits compared: baseline `e5b393955` (Batch 0, pre-purge) vs candidate `66cf11e18` (Batch 1.5 dead-code purge)

## Verdict: PASS

All four affected classes are semantically IDENTICAL between baseline and candidate. Not
"identical after normalization": the raw `javap -p -c -s -constants` output is byte-for-byte
identical with NO normalization applied at all. Method descriptors, signatures, instruction
streams, exception tables, and every referenced constant (including absolute constant-pool
indexes) are unchanged. The one anticipated delta (V67t if-arm removal) turned out to be a
zero-delta; see section "The V67t delta" below.

## Scope

`git diff --name-only e5b393955 66cf11e18` touches exactly 6 files: 2 changelogs (non-source)
and these 4 sources, all under
`src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/models/`:

- `rando/evaluators/CardSelectionEvaluator.java`
- `rando/evaluators/DeployEvaluator.java`
- `chosenone/evaluators/CardSelectionEvaluator.java`
- `chosenone/evaluators/DeployEvaluator.java`

Each source file produces exactly one `.class` file. No inner or anonymous classes exist for
any of the four (verified with a `find` glob `CardSelectionEvaluator*.class` /
`DeployEvaluator*.class`, which would also have matched `$`-suffixed nested classes).

## Toolchain

- JDK: Amazon Corretto 21.0.11 (`javac 21.0.11`, `javap 21.0.11`), inside the running
  `gemp_swccg_app_1` container. Same JDK for both builds.
- Maven: Apache Maven 3.9.6, same shared local repo, builds run sequentially.
- Host has no usable JDK (macOS `/usr/bin/javac` stubs with no runtime), hence in-container.

## Build method (gate check 2: isolated clean trees, same JDK, same command)

1. Two clean git worktrees created from the repo:
   `git worktree add <scratch>/pre e5b393955` and `git worktree add <scratch>/post 66cf11e18`.
2. Each worktree's `src/` copied into the container at `/tmp/parity/pre` and
   `/tmp/parity/post` via `docker cp`. The live bind-mounted `/opt/gemp-swccg/src` was NOT
   touched.
3. Identical build command in each tree, run sequentially:
   `mvn -q -pl gemp-swccg-server -am compile -DskipTests`
   (compiles gemp-swccg-common, gemp-swccg-logic, gemp-swccg-cards, gemp-swccg-server).
   Exit code 0 for both trees (`PRE_MVN_EXIT=0`, `POST_MVN_EXIT=0`).

## Comparison method (gate check 3)

Primary oracle, exactly as the gate specifies:

    javap -p -c -s -constants -cp <tree>/gemp-swccg-server/target/classes \
        com.gempukku.swccgo.ai.models.<bot>.evaluators.<Class>

This output contains method descriptors, internal signatures (`-s`), full instruction streams
with resolved constant comments, exception tables, and `static final` constants (`-constants`).
It contains NO LineNumberTable, LocalVariableTable, or SourceFile-path data (those require `-l`
/ `-v`), so the debug-metadata exclusion the gate demands is inherent to the command.

### Normalization pipeline applied: NONE

The plan allowed for stripping constant-pool indexes (`sed -E 's/#[0-9]+/#N/g'`) if raw diffs
showed pure index-shift noise. This was not needed. The raw dumps are byte-identical
(`cmp` clean), meaning even absolute constant-pool indexes did not shift. That is itself
evidence the deleted text emitted nothing: had any `if (false ...)` branch emitted code or
pooled its string literals pre-purge, the post-purge pool would have compacted and every
subsequent `#N` reference would have shifted.

## Per-class verdict table

| Class (per tree) | javap dump size | Raw diff pre vs post | Verdict |
|---|---|---|---|
| rando/CardSelectionEvaluator | 21,741 lines | empty (byte-identical, sha256 `9a3bdc63...`) | IDENTICAL |
| rando/DeployEvaluator | 13,698 lines | empty (byte-identical, sha256 `657e3042...`) | IDENTICAL |
| chosenone/CardSelectionEvaluator | 21,741 lines | empty (byte-identical, sha256 `9063cfb6...`) | IDENTICAL |
| chosenone/DeployEvaluator | 13,698 lines | empty (byte-identical, sha256 `9a1d74bb...`) | IDENTICAL |

No EXPLAINED-DELTA rows and no UNEXPLAINED rows. Full sha256 values of all 8 dumps are in the
archived evidence (path below).

## The V67t delta: expected one, found zero

The one intentional structural change in Batch 1.5 was in `CardSelectionEvaluator` (both bots),
method `private java.util.List<EvaluatedAction> evaluateForfeit(DecisionContext)`: the dead arm

    if (false /* V159 SUPERSEDED — step 3 handles fv-based forfeit scoring */ && fv > 0) {
        ... V67t/V67bh/V37 block ...
    } else {
        // Zero forfeit value — not worth it
        action.addReasoning("Optional forfeit but zero forfeit value", -80.0f);
    }

was removed and the `else` body made unconditional. This produced NO bytecode change. javac had
already elided the constant-false arm and its guard branch in the BASELINE build, compiling only
the else body, unconditionally, in both trees. Proof, from the baseline (pre) javap dumps:

- None of the dead-arm-only string literals exist anywhere in the pre dump: "V143 HARD BLOCK",
  "V37/V67t PROTECT", "V67t SMALL DAMAGE" each occur 0 times in pre AND post (rando CSE).
  Same for the DeployEvaluator dead-arm literals "V90 NO SUICIDE DEPLOY" and
  "V67aj DEPLOY DEST" (0 in pre and post).
- The else-body instruction sequence is identical in pre and post, same offsets, same pool
  indexes (excerpt, identical on both sides):

```
 463: aload         10
 465: ldc_w         #1850   // String Optional forfeit but zero forfeit value
 468: ldc_w         #1006   // float -80.0f
 471: invokevirtual #559    // Method ...EvaluatedAction.addReasoning:(Ljava/lang/String;F)V
```

So the anticipated EXPLAINED-DELTA is vacuous: the compiler's view of the method never changed.

## Why raw class hashes differ anyway (validates the gate's oracle choice)

Raw `.class` files are NOT byte-identical between the trees, exactly as the gate predicted:

| Class | size pre = post | sha256 pre / post |
|---|---|---|
| rando/CSE | 206,381 B | `b75971fa...` / `6334ee37...` |
| rando/DE | 153,248 B | `985c8e7b...` / `064fefb3...` |
| chosenone/CSE | 206,589 B | `d19994549...` / `4241c5a1...` |
| chosenone/DE | 153,464 B | `ef41b9f0...` / `171f8a9b...` |

To prove the byte difference is 100% debug metadata, a supplementary full-verbose comparison was
run: `javap -v -p` on all 8 classes, unified diff, then every changed line classified. Results
per class pair (rando/CSE shown; all four have the same shape):

- CSE: 5,272 changed lines total; DE: 4,362. After excluding (a) `line N: M` LineNumberTable
  entries, (b) the javap `Classfile /tmp/parity/{pre,post}/...` path header, and (c) the
  `SHA-256 checksum` header line, the residual is 0 lines for all four classes.
- Why each excluded element is semantics-free: LineNumberTable maps bytecode offsets to source
  line numbers purely for stack traces and debuggers (deleting ~350-500 comment/dead lines above
  live code shifts every subsequent mapping value); the Classfile path and checksum headers are
  properties of the dump invocation and the raw file bytes, not of the class. File sizes are
  identical because only line-number VALUES changed, not the entry counts.
- The constant pool sections of the `-v` dumps (every entry, with indexes) are identical, as are
  all StackMapTable frames, access flags, and attribute counts.

## Bonus: Rando/ChosenOne compiled mirror parity (gate check 6, compiled half)

After substituting only the package path (`models/rando/` -> `models/chosenone/`, dot and slash
forms) in the post-purge rando dumps, they are byte-identical to the post-purge chosenone dumps
for both classes. Diff output: 0 lines. The two bots' compiled evaluators differ by package name
alone.

## Reproduction commands

```
git worktree add <scratch>/pre e5b393955
git worktree add <scratch>/post 66cf11e18
docker cp <scratch>/pre/src  gemp_swccg_app_1:/tmp/parity/pre
docker cp <scratch>/post/src gemp_swccg_app_1:/tmp/parity/post
docker exec gemp_swccg_app_1 bash -lc 'cd /tmp/parity/pre  && mvn -q -pl gemp-swccg-server -am compile -DskipTests'
docker exec gemp_swccg_app_1 bash -lc 'cd /tmp/parity/post && mvn -q -pl gemp-swccg-server -am compile -DskipTests'
# for each of {pre,post} x {rando,chosenone} x {CardSelectionEvaluator,DeployEvaluator}:
docker exec gemp_swccg_app_1 javap -p -c -s -constants \
  -cp /tmp/parity/<t>/gemp-swccg-server/target/classes \
  com.gempukku.swccgo.ai.models.<bot>.evaluators.<Class> > <t>_<bot>_<Class>.raw.txt
cmp pre_<bot>_<Class>.raw.txt post_<bot>_<Class>.raw.txt   # clean for all 4
```

Archived evidence (8 raw dumps + 8 verbose dumps) saved during this run at:
`/private/tmp/claude-501/-Users-steve-gemp-swccg-public/da4cbb59-f57a-48a1-8f83-483dd0a5052e/scratchpad/javap-evidence/`
(session-scoped scratchpad; the reproduction commands above regenerate them exactly, byte-for-byte).

Worktrees and container `/tmp/parity` were removed after evidence collection. The live working
tree, the running container, and `/opt/gemp-swccg/src` were not modified.

## Gate coverage map

- Check 2 (isolated clean trees, same JDK + command): DONE, this report.
- Check 3 (normalized javap comparison, no raw hashes, no line tables): DONE, this report.
- Check 4 (identical descriptors/instructions/exception tables/constants): DONE, all IDENTICAL.
- Check 6, compiled half (mirrored-file compiled parity): DONE, package-name-only delta.
- Checks 1, 5, 6 source half, 7, 8 (inventory freeze, replacement fixtures, source parity,
  V191 corpus, full-module build beyond `-am compile`): NOT covered by this report; still owed
  before the gate can read `ADVANCE`.

Verdict for the semantic-bytecode-parity portion of the 1.5A proof: **PASS**.
