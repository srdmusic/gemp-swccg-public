# Codex Cleanup 2.7 gate: 0a529f495

Date: 2026-07-13
Commit: `0a529f495f084c5922ecbf602de6b59ba542168c`
Parent: `bff87f859afaf2277e084d3e37a1fc6cf847acbd`
Verdict: `ADVANCE`
Deployment: not performed
Push: not performed

## Scope

- Only the two mirrored `CardSelectionEvaluator.java` files and their Cleanup 2.7
  changelog/history entries changed.
- Each Java file is exactly 0 insertions and 6 deletions.
- Every changed Java line is a comment. The live wave projection remains unchanged.
- Complete candidate sources are identical after package normalization.
- The two history files add only their Cleanup 2.7 sections and delete no prior content.
- `git diff --check bff87f859 0a529f495` passes.

## Pinned streams

- Each six-line deletion stream: `8bf5b897d67d7aa69e3ac673fab2c2895417b45722e09b592900699777d328c8`.
- Rando then ChosenOne stream: `a3cead56289b112e05e6276b3b98fb6477da68628a25c08182d0c2c36c5fc10c`.
- Self maintenance flag: `0696dd1fea9506e4a9a0a7ccb74e16cc73aa114a62bcfbd05df179ac8b7514dc`.
- Buddy maintenance flag: `976b6403672e60b3d0e3533dfcfdcaf7990102929927d6d536bcd3025a80ae27`.
- Table upkeep pair: `25d5142a3afa2b3ad95c2c334ad27b16b0e901aba2aca085612865fbef99600f`.
- Candidate reserve expression: `1c514ffc8680c782a4416ef5b89987d3fb1610ec230c98b2aca6bbc919a83442`.
- Buddy double-spend expression: `cfb6df97ab528bb2cdf1faecba42f54b0bf901844d6fb7e84e00ee0db5c2f497`.

## Replacement owners

- The deploying card stores exact `thisMaintCost` from `MaintenanceFacts.maintainCost(...)`.
- Buddy tuples store exact maintain cost in `ch[2]`.
- Table upkeep sums exact maintain cost under the existing owner and in-play gates.
- `reserved` adds `thisMaintCost` instead of deploy cost.
- Buddy spend remains deploy cost plus exact maintain cost.

## Build and tests

- Detached parent affected-module package: pass.
- Detached candidate affected-module package: pass.
- Parent/candidate Rando `CardSelectionEvaluator` javap SHA:
  `4c62e0d22f5c9bba210ece81193fd89487acacafd466ab861ceb3fc82eb5bb38`.
- Parent/candidate ChosenOne `CardSelectionEvaluator` javap SHA:
  `fc5b3bf958ae560820511fc45ec71b6fb072bf761ab74519f4dfa520d55f050d`.
- Final detached expanded focused suite: 181 tests, 0 failures, 0 errors, 1 expected F1 skip.
