# Verify AI evaluator edit

## When to run

After Claude edited any file under:
`src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/`

Especially when:
- Adding a new V-version rule (V<n>)
- Mirroring a rule between Rando and Chosen One
- Modifying DeckOracle, ObjectiveAnalyzer, or other strategy files

## Prompt template Claude should use

```
You are the work-verifier agent for the GEMP-SWCCG project. Steve made
multi-file edits to AI evaluator code. Verify the changes compile, the
new rule strings are in the deployed .class files, and Rando + Chosen
One are properly mirrored.

CONTEXT (filled by Claude):
- New V-versions added (or modified): <e.g., V90, V91>
- Files edited (full list): <comma-separated relative paths>
- Expected mirror pairs: <list of rando file → chosenone file pairs>
- Was a Docker rebuild also done? <yes / no>

VERIFICATION STEPS (run all):

1. Compile:
     cd /Users/steve/gemp-swccg-public/src
     mvn -q -pl gemp-swccg-server -am compile 2>&1 | tail -20
   Exit must be 0. If non-zero or compile errors visible → FAIL with
   the error excerpt.

2. For each V-version, confirm the reasoning string appears in the
   compiled .class file:
     Identify which evaluator the rule belongs to from the file edited.
     find /Users/steve/gemp-swccg-public/src/gemp-swccg-server/target/classes/com/gempukku/swccgo/ai -name "*.class" -newer <reference timestamp> | xargs -I{} sh -c 'echo "== {} =="; grep -c "V<n> " {}'
   For each expected V-version, EVERY claimed-edited class file should
   show count >= 1. If zero, the change didn't compile in (likely
   missing actions.add(action) or similar).

3. Mirror check (Rando vs Chosen One):
   For each (rando, chosenone) file pair:
     rando_count=$(grep -c "V<n> " <rando .class>)
     chosenone_count=$(grep -c "V<n> " <chosenone .class>)
   Counts must match within 1. If they don't → WARN (asymmetric mirror).
   Common cause: Claude forgot to copy the rule to the second bot.

4. Type-by-API discipline (Steve's standing rule):
   For each edited file, grep for forbidden patterns:
     grep -nE 'getTitle\(\)\.contains\("(weapon|character|jedi|sith|imperial|rebel|location|site|battleground|pilot|leader|warrior|droid)"' <file>
   These are forbidden — Claude should use Filters.<x> / hasIcon /
   hasKeyword instead. If any found → WARN with the line numbers.

5. Existing test suite did not regress:
     mvn -q -pl gemp-swccg-server test -Dtest='*AI*' 2>&1 | tail -10
   (Optional / time-consuming. Run only if Claude specifically added
   tests or if explicitly requested.)

6. (If Docker rebuild was also done) Verify the .class files in the
   CONTAINER also have the new rule strings — the local target/classes
   might match, but if the container is running an older JAR the rule
   isn't actually live:
     docker exec gemp_swccg_app_1 grep -c "V<n> " /opt/gemp-swccg/src/gemp-swccg-server/target/classes/com/gempukku/swccgo/ai/models/rando/evaluators/<evaluator>.class

7. AI_CHANGELOG.md updated:
     grep -c "V<n>" /Users/steve/gemp-swccg-public/AI_CHANGELOG.md
   Must be >= 1 for each new V-version added. If 0 → WARN with
   reminder that Claude must append a one-line entry to the matching
   section in AI_CHANGELOG.md before declaring done.

8. AI_CHANGELOG.md DESCRIPTION-PARITY check (added 2026-05-20):
   For EACH V-tag mentioned in the changelog (not just the new ones),
   confirm the description matches the actual code comment header.
   Procedure:
     a. Extract: grep -oE "V[0-9]+(\.[0-9]+)?[a-z]*" AI_CHANGELOG.md
     b. For each V-tag: find the code comment block(s):
          grep -rn "// === V<n>\b\|// V<n>:" src/gemp-swccg-server/src/main/java/com/gempukku/swccgo/ai/
     c. Compare the changelog claim against the code's comment header.
     d. Classify each as PASS / WARN (oversold or partial) / FAIL (wrong
        topic). FAIL examples: V79 listed under "Hidden Path / Jakku" when
        it's actually Verge of Greatness; V51 cited for buddy-path when
        buddy-path is V38.
   This catches the failure mode where Claude wrote the changelog from
   working memory after a long session and pattern-matched citations
   that don't actually exist or describe different rules.

9. V-tag IN PR DIFF check (added 2026-05-20):
   If a V-tag in the changelog touches code OUTSIDE the AI evaluators
   (e.g., UI files like gameUi.js, server handlers like WebRequestHandler),
   confirm the actual edit is in the PR branch's diff:
     git diff --name-only <PR-base>..<head> | grep -i <expected-file>
   If the file isn't in the diff, REMOVE the V-tag from the changelog
   for THIS PR. (Failure mode: V84 cache-control edit was on a different
   branch and got dropped during rebase, but the changelog still cited
   it.)

10. NO-FABRICATION check on SWCCG content (added 2026-05-20):
   Any user-facing document (changelog, PR comment, summary) MUST be
   scanned for invented SWCCG content. Common slop patterns:
   a. Deck acronyms with made-up expansions (TDIGWATT, IAYF, AMSD,
      IBS, IHYN, EOPS, etc.). Each is a quote from movies/cards.
      Verify against Steve's knowledge or grep the card database.
      Example slop: "TDIGWATT = They Don't Igo Want That Tibanna".
      Correct: "This Deal Is Getting Worse All The Time" (Lando).
   b. Card names not in card_blueprint_database. Search:
        grep -i "<title>" /Users/steve/gemp-swccg-public/src/gemp-swccg-cards/src/main/resources/card_blueprint_database*.json
      No hits = invented or misspelled.
   c. Persona names not in com.gempukku.swccgo.common.Persona enum.
   d. V-tag descriptions that don't match the comment header in code
      (covered by step 8).
   If ANY of these are detected, FLAG them in the report. Claude
   must ask Steve before publishing the document if content can't
   be verified.

   See: /Users/steve/.claude/projects/-Users-steve-gemp-swccg-public/memory/feedback_no_fabrication.md

KNOWN PAST FAILURES (cross-check):
- V79 (May 15 2026): action.addReasoning() was called but actions.add(action)
  was never invoked, so the EvaluatedAction never made it to
  CombinedEvaluator. Looked like the rule fired in logs but had no
  effect. Fix: add actions.add(action); continue;
  → Step 2 catches this only indirectly. ALSO grep for the pattern
    inside each new rule: must see actions.add(action) after the
    final addReasoning call in that V<n> branch.
- V83 (May 18 2026): rule fired with "unknown site" because action
  text didn't contain a location name. The penalty applied even when
  the target wasn't identifiable. Fix: null-guard mlTargetLoc.
  → Watch for new rules that don't null-guard a found-from-text location.

REPORT FORMAT:
PASS / WARN / FAIL: <one-line summary>

Details:
- Compile: PASS / FAIL (<error if fail>)
- Per V-version, per file:
    V<n> in <file>: <count>
    V<n> in <mirror file>: <count>
    Mirror match: YES / NO (off by <n>)
- Type-by-API violations found: <count> (<file:line for each>)
- actions.add() present in new branches: YES / NO (<file:line>)
- Container .class confirmation (if rebuild done): <count> matches local? YES / NO

If FAIL, list specific files and lines to fix.

Append this report to:
  /Users/steve/gemp-swccg-public/.claude/skills/work-verifier/history.md
with a heading line like:
  ## 2026-MM-DD HH:MM — AI edit (V<n>...) → <PASS|WARN|FAIL>
```
