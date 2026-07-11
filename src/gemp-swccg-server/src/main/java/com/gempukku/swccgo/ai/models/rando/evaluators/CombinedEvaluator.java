package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Combines multiple evaluators to make a final decision.
 *
 * Each applicable evaluator scores the actions, then we pick the best.
 */
public class CombinedEvaluator {
    private static final Logger LOG = RandoLogger.getEvaluatorLogger();
    // OWNED BY: SVC-SAFETY. Additive sum per actionId: score-neutral = IDENTICAL SCORES.
    private static final float BAD_ACTION_THRESHOLD = -100.0f;
    // V67bc UPDATED 2026-07-06 (audit deploy-sequencing-4): floor for the DPS
    // epilogue that considers NON-BUCKET actions before passing. +50 so junk
    // keyword-scored actions don't fire; real into-hand Reserve pulls carry
    // V116's +100 reserve-deck baseline (plus per-card bonuses) and clear it.
    private static final float NON_BUCKET_EPILOGUE_FLOOR = 50.0f;

    private final List<ActionEvaluator> evaluators;
    private final Random random = new Random();

    public CombinedEvaluator() {
        this.evaluators = new ArrayList<>();
        initializeEvaluators();
    }

    /**
     * Initialize all available evaluators.
     */
    private void initializeEvaluators() {
        // Add evaluators in priority order

        // Specific evaluators (handled first - they check decision type/text carefully)
        evaluators.add(new ForceActivationEvaluator());  // INTEGER decisions for force activation
        evaluators.add(new DeployEvaluator());           // Deploy decisions
        evaluators.add(new BattleEvaluator());           // Battle initiation
        evaluators.add(new MoveEvaluator());             // Movement decisions
        evaluators.add(new DrawEvaluator());             // Card draw decisions (draw phase only)
        evaluators.add(new CardSelectionEvaluator());    // Card selection from lists

        // General evaluators (catch-all for remaining actions)
        evaluators.add(new ActionTextEvaluator());       // Text-based action scoring
        evaluators.add(new PassEvaluator());             // Pass/cancel option scoring

        LOG.info("Initialized {} evaluators", evaluators.size());
    }

    /**
     * Run all applicable evaluators and return the best action.
     *
     * @param context Decision context
     * @return Best evaluated action, or null if no evaluators apply
     */
    public EvaluatedAction evaluateDecision(DecisionContext context) {
        // ═══════════════════════════════════════════════════════════
        // ═══ SECTION: SVC-SAFETY (reorg 2026-07-06) ═══
        // Owns: the score-merge loop below + the all-bad/V148 pass gate + BAD_ACTION_THRESHOLD.
        //   Hub: none. KIND mix: VETO (V148 cancellable all-bad pass) over BANDED sums;
        //   key magnitudes: threshold -100, merge is ADDITIVE per actionId.
        // Additive sum per actionId: score-neutral = IDENTICAL SCORES.
        //   A bigger-magnitude new rule silently dominates an old one inside the same sum.
        // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
        // Cross-refs: DEPLOY-1 (DPS walk mid-method), SVC-SAFETY V191 TOPN block.
        //   See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
        // ═══════════════════════════════════════════════════════════
        // Use a map to merge scores for actions with the same ID
        // This prevents a generic evaluator from overriding a specific evaluator's score
        Map<String, EvaluatedAction> actionMap = new HashMap<>();

        for (ActionEvaluator evaluator : evaluators) {
            if (!evaluator.isEnabled()) {
                continue;
            }

            if (evaluator.canEvaluate(context)) {
                LOG.debug("Running evaluator: {}", evaluator.getName());
                List<EvaluatedAction> actions = evaluator.evaluate(context);

                for (EvaluatedAction action : actions) {
                    String actionId = action.getActionId();

                    // Merge with existing action if same ID, otherwise add new
                    if (actionMap.containsKey(actionId)) {
                        EvaluatedAction existing = actionMap.get(actionId);
                        // Merge: add the scores together and combine reasoning
                        existing.mergeFrom(action);
                        LOG.debug("Merged scores for '{}': now {}", actionId, existing.getScore());
                    } else {
                        actionMap.put(actionId, action);
                    }

                    // Log this evaluator's contribution
                    evaluator.logEvaluation(action);
                }
            }
        }

        List<EvaluatedAction> allActions = new ArrayList<>(actionMap.values());

        if (allActions.isEmpty()) {
            LOG.warn("No evaluators produced actions for decision: {}", context.getDecisionText());
            return null;
        }

        // OWNED BY: SVC-SAFETY (instrumentation; log from copy only, never mutate live list)
        // V191 (2026-07-06): TOP-N CANDIDATE LOGGING — the dominance-regression
        // detector. One line per decision showing the top-5 merged candidates,
        // so a future rule that silently out-dominates an old one shows up in
        // log deltas. Instrumentation ONLY — zero scoring changes; sorts a COPY
        // of the candidate list (the live list's order, and therefore stream
        // tie-breaking below, is never touched).
        {
            List<EvaluatedAction> v191Copy = new ArrayList<>(allActions);
            v191Copy.sort(Comparator.comparing(EvaluatedAction::getScore).reversed());
            StringBuilder v191Top = new StringBuilder();
            int v191N = Math.min(5, v191Copy.size());
            for (int i = 0; i < v191N; i++) {
                if (i > 0) v191Top.append(", ");
                EvaluatedAction v191Ea = v191Copy.get(i);
                v191Top.append(v191Ea.getActionId()).append('=').append(v191Ea.getScore());
            }
            LOG.warn("V191 TOPN: {} phase={} :: {}",
                context.getDecisionType(), context.getPhase(), v191Top);
        }

        // ═══════════════════════════════════════════════════════════
        // ═══ SECTION: DEPLOY-1 (reorg 2026-07-06) ═══
        // OWNED BY: DEPLOY-1 (bucket walk V67bc/V67ax + non-bucket epilogue)
        // Owns: DPS hierarchy walk + epilogue + legacy V67ax single-set filter (through
        //   the "Legacy single-set filter" block below). Hub: none. KIND: ORDERING
        //   (first-viable-bucket wins); key magnitudes: viability floor -100,
        //   non-bucket epilogue floor +50.
        // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
        // Cross-refs: SVC-SAFETY (merge loop above / all-bad gate below), DEPLOY-2
        //   (V67bc buckets feed siting). See resources/RANDO_REORG_PLAN_2026-07-02.md §3
        //   + Rando_Section_Manifest_2026-07-06.xlsx.
        // ═══════════════════════════════════════════════════════════
        // V67bc DPS HIERARCHY WALK: when DPS provided ordered step buckets,
        // walk them top→bottom. For each bucket, pick the highest-scoring
        // action. If that action's score is above the bad threshold, return
        // it. Otherwise fall through to the next bucket. PASS only when ALL
        // buckets exhausted with all-bad scores.
        //
        // This implements Steve's principle: "walk the full hierarchy every
        // call, take first viable, only pass when nothing viable." Replaces
        // the older single-set filter that wrongly forced PASS when STEP 1's
        // only candidate was hard-blocked (e.g. K&D pull when no CC interior
        // sites left in reserve → -9999 → PASS even though STEP 2/3 had 9
        // viable character deploys).
        java.util.List<java.util.Set<String>> buckets = context.getStepBuckets();
        java.util.List<String> bucketLabels = context.getStepBucketLabels();
        if (buckets != null && !buckets.isEmpty()) {
            for (int b = 0; b < buckets.size(); b++) {
                java.util.Set<String> bucket = buckets.get(b);
                String label = (bucketLabels != null && b < bucketLabels.size()) ? bucketLabels.get(b) : ("step#" + b);
                List<EvaluatedAction> bucketActions = new ArrayList<>();
                for (EvaluatedAction ea : allActions) {
                    if (bucket.contains(ea.getActionId())) {
                        bucketActions.add(ea);
                    }
                }
                if (bucketActions.isEmpty()) {
                    LOG.warn("V67bc DPS WALK: step={} bucket has 0 scored actions, skipping", label);
                    continue;
                }
                EvaluatedAction bestInBucket = bucketActions.stream()
                    .max(Comparator.comparing(EvaluatedAction::getScore))
                    .orElse(null);
                if (bestInBucket == null) continue;
                LOG.warn("V67bc DPS WALK: step={} best={} score={}",
                    label, bestInBucket.getDisplayText(), bestInBucket.getScore());
                if (bestInBucket.getScore() >= BAD_ACTION_THRESHOLD) {
                    LOG.warn("V67bc DPS WALK: step={} viable → picking '{}' (score {})",
                        label, bestInBucket.getDisplayText(), bestInBucket.getScore());
                    return bestInBucket;
                }
                LOG.warn("V67bc DPS WALK: step={} all bad (best score {}) → falling through to next step",
                    label, bestInBucket.getScore());
            }
            // V67bc UPDATED 2026-07-06 (audit deploy-sequencing-4): EPILOGUE —
            // before passing, consider positively-scored NON-BUCKET actions.
            // DeployPhaseScript.resolveSteps deliberately excludes "take X into
            // hand / into pile" Reserve pulls from EVERY bucket (they don't put
            // a card on table), so the old walk could never pick them: once all
            // buckets were exhausted it returned PASS no matter how well the
            // pull scored. Now: gather the evaluated actions that appear in NO
            // bucket; if the best of them clears NON_BUCKET_EPILOGUE_FLOOR
            // (+50), pick it instead of passing. Bucketed decisions are
            // untouched — any viable bucket already returned inside the loop
            // above, so this epilogue only ever replaces PASS, never a bucket
            // winner.
            java.util.Set<String> v67bcAllBucketIds = new java.util.HashSet<>();
            for (java.util.Set<String> v67bcBkt : buckets) {
                v67bcAllBucketIds.addAll(v67bcBkt);
            }
            EvaluatedAction v67bcBestNonBucket = null;
            for (EvaluatedAction ea : allActions) {
                // Skip pass-like entries: PassEvaluator emits actionId "" OR a
                // real "Cancel" actionId (ACTION_CHOICE) typed PASS — its
                // conserve bonuses (V27/V27.1) can stack past the floor, and
                // picking Cancel here would just be PASS with extra steps.
                if (ea.getActionType() == ActionType.PASS) continue;
                if (ea.getActionId() == null || ea.getActionId().isEmpty()) continue;
                if (v67bcAllBucketIds.contains(ea.getActionId())) continue;
                if (v67bcBestNonBucket == null || ea.getScore() > v67bcBestNonBucket.getScore()) {
                    v67bcBestNonBucket = ea;
                }
            }
            if (v67bcBestNonBucket != null
                    && v67bcBestNonBucket.getScore() >= NON_BUCKET_EPILOGUE_FLOOR) {
                LOG.warn("V67bc DPS EPILOGUE: buckets exhausted, but non-bucket action '{}' scored {} (>= floor {}) → picking it over PASS",
                    v67bcBestNonBucket.getDisplayText(), v67bcBestNonBucket.getScore(),
                    NON_BUCKET_EPILOGUE_FLOOR);
                return v67bcBestNonBucket;
            }
            if (v67bcBestNonBucket != null) {
                LOG.warn("V67bc DPS EPILOGUE: best non-bucket action '{}' scored {} < floor {} → still PASS",
                    v67bcBestNonBucket.getDisplayText(), v67bcBestNonBucket.getScore(),
                    NON_BUCKET_EPILOGUE_FLOOR);
            }

            // All buckets exhausted with all-bad scores — true PASS time.
            LOG.warn("V67bc DPS WALK: every bucket all-bad → PASS");
            EvaluatedAction passAction = new EvaluatedAction(
                "",
                ActionType.PASS,
                0.0f,
                "V67bc DPS: every hierarchy step had only bad-scored actions, passing");
            passAction.addReasoning(context.getAllowedActionsReason() != null
                ? context.getAllowedActionsReason() : "DPS hierarchy exhausted");
            return passAction;
        }

        // Legacy single-set filter (no DPS hierarchy provided).
        java.util.Set<String> allowed = context.getAllowedActionIds();
        if (allowed != null) {
            int before = allActions.size();
            List<EvaluatedAction> filtered = new ArrayList<>();
            for (EvaluatedAction ea : allActions) {
                if (allowed.contains(ea.getActionId())) {
                    filtered.add(ea);
                }
            }
            LOG.warn("V67ax DPS FILTER (legacy): {}/{} actions allowed (reason: {})",
                filtered.size(), before, context.getAllowedActionsReason());
            if (!filtered.isEmpty()) {
                allActions = filtered;
            } else if (allowed.isEmpty()) {
                LOG.warn("V67ax DPS FILTER: empty allowed set → PASS");
                EvaluatedAction passAction = new EvaluatedAction(
                    "",
                    ActionType.PASS,
                    0.0f,
                    "V67ax DPS: no step qualified, passing");
                passAction.addReasoning(context.getAllowedActionsReason() != null
                    ? context.getAllowedActionsReason() : "DPS empty allowed");
                return passAction;
            } else {
                LOG.warn("V67ax DPS FILTER: allowed set non-empty but no scored action matched — falling back to unfiltered");
            }
        }

        // Pick the best action (highest merged score)
        EvaluatedAction bestAction = allActions.stream()
            .max(Comparator.comparing(EvaluatedAction::getScore))
            .orElse(null);

        if (bestAction == null) {
            return null;
        }

        // OWNED BY: SVC-SAFETY. Additive sum per actionId: score-neutral = IDENTICAL SCORES.
        // If ALL actions are terrible, consider passing
        // V148 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, T2: Luke TLJ deployed to a −10 site
        // when ALL offered sites scored negative and Done was available): for "where to deploy"
        // selections the cancel bar was −100, so mildly-negative best sites still got committed.
        // Deploying a character to a NEGATIVE-value site is strictly worse than holding the card
        // (deploy is optional) — for deploy-location prompts the bar is now < 0. Everything else
        // keeps BAD_ACTION_THRESHOLD (−100) unchanged.
        float v148PassBar = BAD_ACTION_THRESHOLD;
        String v148Dtext = context.getDecisionText() != null ? context.getDecisionText().toLowerCase() : "";
        if (v148Dtext.contains("where to deploy")) v148PassBar = 0.0f;
        if (bestAction.getScore() < v148PassBar) {
            // V148 (Steve, 2026-05-28): "Rando should always have the option to hit
            // Done/Cancel if he scores something and finds it not favorable. That's
            // what a real player would do."
            //
            // Many CARD_SELECTION decisions ("Choose where to deploy X, or click Done
            // to cancel") carry noPass=true even though min=0 and the prompt offers a
            // Done/Cancel button. The noPass flag here refers to the phase-level pass,
            // NOT the in-selection Done button (which is "select zero cards"). So the
            // old canPass (!isNoPass && min==0) wrongly forced Rando to commit to a
            // terrible deploy (e.g. Dr. Evazan to a -1330 site) instead of cancelling.
            //
            // New rule: a decision is cancellable if min==0 AND either it's normally
            // passable (!noPass) OR the prompt text offers Done/Cancel/optional. The
            // DecisionSafety.mustChoose() guard is updated in lockstep so the empty
            // response isn't force-corrected back into a pick.
            String dtext = context.getDecisionText() != null
                ? context.getDecisionText().toLowerCase() : "";
            boolean textOffersCancel = dtext.contains("done")
                || dtext.contains("cancel")
                || dtext.contains("if desired")
                || dtext.contains("optional");
            boolean canPass = context.getMin() == 0
                && (!context.isNoPass() || textOffersCancel);

            // V24.5: No randomness — always pass when all actions are bad
            if (canPass) {
                LOG.info("All actions bad (best: {}), choosing to PASS", bestAction.getScore());
                EvaluatedAction passAction = new EvaluatedAction(
                    "",  // Empty = pass
                    ActionType.PASS,
                    0.0f,
                    "Pass (all actions were bad)"
                );
                passAction.addReasoning(String.format("Best action was %.1f, deciding to pass instead", bestAction.getScore()));
                return passAction;
            } else if (canPass) {
                LOG.info("All actions bad (best: {}), but taking least-bad action anyway", bestAction.getScore());
            } else {
                LOG.info("All actions bad (best: {}), but MUST choose (noPass={}, min={})",
                        bestAction.getScore(), context.isNoPass(), context.getMin());
            }
        }

        LOG.info("Best action: {} (score: {})", bestAction.getDisplayText(), bestAction.getScore());
        LOG.info("   Reasoning: {}", bestAction.getReasoningString());

        return bestAction;
    }

    /**
     * Check if any evaluator can handle this decision type.
     */
    public boolean canHandle(DecisionContext context) {
        for (ActionEvaluator evaluator : evaluators) {
            if (evaluator.isEnabled() && evaluator.canEvaluate(context)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the list of evaluators (for testing/debugging).
     */
    public List<ActionEvaluator> getEvaluators() {
        return evaluators;
    }

    /**
     * Add a custom evaluator.
     */
    public void addEvaluator(ActionEvaluator evaluator) {
        evaluators.add(evaluator);
    }
}
