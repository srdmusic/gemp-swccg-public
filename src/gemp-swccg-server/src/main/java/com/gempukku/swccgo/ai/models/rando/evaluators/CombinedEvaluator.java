package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import com.gempukku.swccgo.ai.models.common.trace.NoOpTraceSink;
import com.gempukku.swccgo.ai.models.common.trace.TraceCaptureFailure;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceSink;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
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
    // TRACE HOOK (2026-07-13, CODEX_MINIMAL_DECISION_TRACE_HOOK): per-decision trace sink.
    // Production default = NoOpTraceSink: no session is opened, EvaluatedAction's guards
    // short-circuit on the absent thread-local, and behavior/score bits/winner/V191 output
    // are byte-identical to the un-instrumented code.
    private final TraceSink traceSink;

    public CombinedEvaluator() {
        this.traceSink = NoOpTraceSink.INSTANCE;
        this.evaluators = new ArrayList<>();
        initializeEvaluators();
    }

    /**
     * TRACE HOOK (2026-07-13): package-visible pure-harness seam. JUnit injects scripted
     * evaluators (in order) plus a capture sink; no server, log parsing, or replay files.
     * Production never calls this; the public constructor keeps the normal evaluator list
     * and the no-op sink.
     */
    CombinedEvaluator(List<ActionEvaluator> orderedEvaluators, TraceSink traceSink) {
        this.traceSink = (traceSink != null) ? traceSink : NoOpTraceSink.INSTANCE;
        this.evaluators = new ArrayList<>(orderedEvaluators);
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
        // TRACE ORACLE V2 (2026-07-13, CODEX_TRACE_ORACLE_V2_CONTRACT): session ownership.
        // The bot entry point (RandoCalAi) owns the per-decide() session; when one is
        // already active this method only RECORDS into it and never closes it. The pure
        // JUnit seam owns its own session: it opens one here (raw candidate arrays +
        // shadow snapshot from the DecisionContext) only when none is active, and only
        // the opener closes and emits. Instrumentation must never throw into the
        // decision path, hence the blanket guards.
        boolean externallyOwned = false;
        boolean opened = false;
        try {
            externallyOwned = TraceSession.isActive();
            if (!externallyOwned && traceSink.isEnabled()) {
                opened = openSeamSession(context);
            }
        } catch (Throwable t) {
            opened = false;
        }
        boolean traced = externallyOwned || opened;
        if (!traced) {
            return evaluateDecisionCore(context, false);
        }
        EvaluatedAction traceResult = null;
        boolean completedNormally = false;
        try {
            traceResult = evaluateDecisionCore(context, true);
            completedNormally = true;
            return traceResult;
        } finally {
            try {
                if (completedNormally) {
                    // The pre-SAFETY winner (contract "Finalization record" item 1) —
                    // explicitly NOT the AI's final answer; DecisionSafety and the bot
                    // boundary record the rest of the finalization record.
                    TraceSession.recordPreSafetyWinner(
                        (traceResult != null) ? traceResult.getActionId() : null,
                        (traceResult != null) ? Float.valueOf(traceResult.getScore()) : null,
                        traceResult != null && traceResult.isHardVetoed(),
                        (traceResult != null) ? traceResult.getVetoReason() : null);
                } else {
                    // Evaluation threw: the record must say so, never truncate silently.
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.EVALUATOR,
                        "evaluator-exception",
                        "evaluateDecisionCore exited exceptionally; trace is truncated");
                }
                if (opened) {
                    // GATE P0-2 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): the one
                    // typed emission channel — finish() failures emit the fallback
                    // envelope, sink failures are re-offered once with a typed SINK
                    // failure. Never throws into the decision path.
                    TraceSession.closeAndEmit(traceSink);
                }
            } catch (Throwable t) {
                if (opened) {
                    TraceSession.abandon();
                }
            }
        }
    }

    /**
     * TRACE ORACLE V2 (2026-07-13): open a seam-owned session from the DecisionContext —
     * COMPLETE raw candidate arrays (per decision shape) + shadow DecisionSnapshot. Pure
     * reads only. Used only when no bot-boundary session is active.
     */
    private boolean openSeamSession(DecisionContext context) {
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.producerId = "combined-evaluator-seam";
        in.decisionId = context.getDecisionId();
        in.decisionTypeName = context.getDecisionType();
        in.decisionText = context.getDecisionText();
        in.phase = context.getPhase();
        in.turn = context.getTurnNumber();
        in.currentPlayer = context.getPlayerId();
        in.side = context.getSide();
        // Context values are the EFFECTIVE values the legacy pipeline decides with
        // (parsed-or-default); the bot-boundary session records raw param presence.
        // GATE P0-1: DecisionContext defaults its arrays to EMPTY lists, so it cannot
        // represent present-empty-versus-absent — contextListOrAbsent stages empty as
        // ABSENT here, and build() marks the raw record Source.CONTEXT_EFFECTIVE. The
        // verbatim raw distinction is owned by the bot boundary's rawParameters.
        in.noPassParam = context.isNoPass();
        in.minParam = context.getMin();
        in.maxParam = context.getMax();
        in.blockedResponses = context.getBlockedResponses();
        in.actionIds = TraceSnapshots.contextListOrAbsent(context.getActionIds());
        in.actionTexts = TraceSnapshots.contextListOrAbsent(context.getActionTexts());
        in.cardIds = TraceSnapshots.contextListOrAbsent(context.getCardIds());
        in.blueprintIds = TraceSnapshots.contextListOrAbsent(context.getBlueprints());
        in.testingTexts = TraceSnapshots.contextListOrAbsent(context.getTestingTexts());
        in.selectable = TraceSnapshots.contextListOrAbsent(context.getSelectable());
        TraceSnapshots.Result snapshot = TraceSnapshots.build(in);
        boolean opened = TraceSession.open(getClass().getPackageName(),
            context.getDecisionId(), context.getDecisionType(), context.getDecisionText(),
            TraceSnapshots.rawCandidateIds(context.getDecisionType(),
                context.getActionIds(), context.getCardIds(), context.getActionTexts()),
            snapshot.snapshot(), snapshot.issues(), false);
        if (opened) {
            TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR,
                "seam session: decisionType=" + context.getDecisionType(), null);
        }
        return opened;
    }

    private EvaluatedAction evaluateDecisionCore(DecisionContext context, boolean traced) {
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
        // B0-TIE-DETERMINISM (2026-07-13, Codex m00228 — INTENTIONAL fixture-contract delta):
        // HashMap iteration made exact-score ties depend on unspecified map order.
        // LinkedHashMap = first-seen insertion order (evaluator registration order, then
        // offered-action order), and every winner selection below breaks ties by KEEPING
        // the earlier candidate (strict Float.compare(candidate, best) > 0 to replace).
        Map<String, EvaluatedAction> actionMap = new LinkedHashMap<>();

        // TRACE ORACLE V2 (2026-07-13): semantic pass/cancel eligibility with the exact
        // facts used — the SAME V148 cancellability expression the pass gates below use.
        // Pure reads; recorded once per decision, traced runs only.
        if (traced) {
            String peText = context.getDecisionText() != null
                ? context.getDecisionText().toLowerCase() : "";
            boolean peTextCancel = peText.contains("done") || peText.contains("cancel")
                || peText.contains("if desired") || peText.contains("optional");
            boolean peEligible = context.getMin() == 0
                && (!context.isNoPass() || peTextCancel);
            TraceSession.recordPassEligibility(peEligible,
                "min=" + context.getMin() + " noPass=" + context.isNoPass()
                    + " textOffersCancel=" + peTextCancel + " (V148 semantics)");
        }

        for (ActionEvaluator evaluator : evaluators) {
            if (!evaluator.isEnabled()) {
                continue;
            }

            if (evaluator.canEvaluate(context)) {
                LOG.debug("Running evaluator: {}", evaluator.getName());
                // TRACE HOOK (2026-07-13): bind this evaluator's id to every op recorded during
                // its evaluate() call (and the merges of its returned actions) before moving on.
                // ORACLE V2 lifecycle law: beginEvaluator is paired with endEvaluator in
                // finally, so an evaluator exception cannot leak the binding.
                if (traced) TraceSession.beginEvaluator(evaluator.getName());
                try {
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
                            // TRACE ORACLE V2 (2026-07-13): record the first-seen MERGE
                            // insertion order (reorder detector). Ordinals bind to the
                            // RAW candidate order frozen at session open, NOT to this map.
                            if (traced) TraceSession.registerCandidate(actionId);
                        }

                        // Log this evaluator's contribution
                        evaluator.logEvaluation(action);
                    }
                } finally {
                    if (traced) TraceSession.endEvaluator();
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
                // B0-TIE-DETERMINISM (m00228): explicit first-seen tie retention.
                EvaluatedAction bestInBucket = null;
                for (EvaluatedAction ba : bucketActions) {
                    if (ba.isHardVetoed()) continue;  // FORMATION SAFETY 2026-07-11c
                    if (bestInBucket == null || Float.compare(ba.getScore(), bestInBucket.getScore()) > 0) {
                        bestInBucket = ba;
                    }
                }
                if (bestInBucket == null) {
                    // TRACE HOOK (2026-07-13): empty rank, bucket had no eligible action.
                    if (traced) TraceSession.recordRank(null, null, null,
                        "V67bc bucket step=" + label + ": no eligible (all vetoed)");
                    continue;
                }
                // TRACE HOOK (2026-07-13): bucket-walk rank.
                if (traced) TraceSession.recordRank(bestInBucket, bestInBucket.getActionId(),
                    Float.valueOf(bestInBucket.getScore()), "V67bc bucket step=" + label + " best");
                LOG.warn("V67bc DPS WALK: step={} best={} score={}",
                    label, bestInBucket.getDisplayText(), bestInBucket.getScore());
                if (bestInBucket.getScore() >= BAD_ACTION_THRESHOLD) {
                    LOG.warn("V67bc DPS WALK: step={} viable → picking '{}' (score {})",
                        label, bestInBucket.getDisplayText(), bestInBucket.getScore());
                    // TRACE HOOK (2026-07-13): bucket winner.
                    if (traced) TraceSession.recordSelect(bestInBucket, bestInBucket.getActionId(),
                        Float.valueOf(bestInBucket.getScore()), false, null,
                        "V67bc bucket winner step=" + label);
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
                if (ea.isHardVetoed()) continue;  // FORMATION SAFETY 2026-07-12 (Codex m00194 P0#1): epilogue must not resurrect vetoed actions
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
                // TRACE HOOK (2026-07-13): epilogue winner.
                if (traced) TraceSession.recordSelect(v67bcBestNonBucket,
                    v67bcBestNonBucket.getActionId(), Float.valueOf(v67bcBestNonBucket.getScore()),
                    false, null, "V67bc non-bucket epilogue winner");
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
            // TRACE HOOK (2026-07-13): synthetic Pass, explicit ordinal + source marker.
            if (traced) {
                TraceSession.markSynthetic(passAction, "V67BC_DPS_PASS");
                TraceSession.recordSelect(passAction, passAction.getActionId(),
                    Float.valueOf(passAction.getScore()), false, null, "V67bc DPS pass (synthetic)");
            }
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
                // TRACE HOOK (2026-07-13): synthetic Pass, explicit ordinal + source marker.
                if (traced) {
                    TraceSession.markSynthetic(passAction, "V67AX_DPS_PASS");
                    TraceSession.recordSelect(passAction, passAction.getActionId(),
                        Float.valueOf(passAction.getScore()), false, null, "V67ax DPS pass (synthetic)");
                }
                return passAction;
            } else {
                LOG.warn("V67ax DPS FILTER: allowed set non-empty but no scored action matched — falling back to unfiltered");
            }
        }

        // Pick the best action (highest merged score)
        // FORMATION SAFETY (2026-07-11c): hard-vetoed actions are UN-SELECTABLE regardless of
        // score — Steve's four basics can no longer be outvoted by bonus stacks. If everything
        // is vetoed, fall back to the least-bad vetoed action ONLY when passing is impossible
        // (never hang: a bad decision still beats no decision — DecisionSafety doctrine).
        java.util.List<EvaluatedAction> nonVetoed = new java.util.ArrayList<>();
        for (EvaluatedAction a : allActions) {
            if (a.isHardVetoed()) {
                LOG.warn("FORMATION SAFETY: '{}' vetoed — {}", a.getDisplayText(), a.getVetoReason());
            } else {
                nonVetoed.add(a);
            }
        }
        boolean fsAllVetoed = nonVetoed.isEmpty() && !allActions.isEmpty();
        // B0-TIE-DETERMINISM (m00228): explicit first-seen tie retention.
        EvaluatedAction bestAction = null;
        for (EvaluatedAction ca : (fsAllVetoed ? allActions : nonVetoed)) {
            if (bestAction == null || Float.compare(ca.getScore(), bestAction.getScore()) > 0) {
                bestAction = ca;
            }
        }
        // TRACE HOOK (2026-07-13): pre-final winner rank (before the pass gates below).
        if (traced && bestAction != null) {
            TraceSession.recordRank(bestAction, bestAction.getActionId(),
                Float.valueOf(bestAction.getScore()),
                fsAllVetoed ? "pre-final best (all vetoed, least-bad)" : "pre-final best");
        }
        if (fsAllVetoed && bestAction != null) {
            // ADJUSTED 2026-07-12 (Codex m00194 P0#2): use V148's cancellability semantics — optional
            // CARD_SELECTION prompts commonly carry noPass=true with min=0 + a Done/Cancel button;
            // the old (!noPass) test would have FORCED a vetoed destination instead of cancelling.
            String fsDtext = context.getDecisionText() != null ? context.getDecisionText().toLowerCase() : "";
            boolean fsTextCancel = fsDtext.contains("done") || fsDtext.contains("cancel")
                || fsDtext.contains("if desired") || fsDtext.contains("optional");
            boolean fsCanPass = context.getMin() == 0 && (!context.isNoPass() || fsTextCancel);
            if (fsCanPass) {
                LOG.warn("FORMATION SAFETY: ALL actions vetoed and pass is legal — passing instead of '{}'",
                    bestAction.getDisplayText());
                EvaluatedAction fsPass = new EvaluatedAction("", ActionType.PASS, 0.0f,
                    "Pass (all actions formation-vetoed)");
                fsPass.addReasoning("FORMATION SAFETY: every action violated a basic law; passing");
                // TRACE HOOK (2026-07-13): synthetic Pass, explicit ordinal + source marker.
                if (traced) {
                    TraceSession.markSynthetic(fsPass, "FORMATION_SAFETY_PASS");
                    TraceSession.recordSelect(fsPass, fsPass.getActionId(),
                        Float.valueOf(fsPass.getScore()), false, null,
                        "formation-safety pass (synthetic)");
                }
                return fsPass;
            }
            LOG.warn("FORMATION SAFETY: ALL actions vetoed but must choose — taking least-bad '{}'",
                bestAction.getDisplayText());
        }

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
                // TRACE HOOK (2026-07-13): synthetic Pass, explicit ordinal + source marker.
                if (traced) {
                    TraceSession.markSynthetic(passAction, "V148_ALL_BAD_PASS");
                    TraceSession.recordSelect(passAction, passAction.getActionId(),
                        Float.valueOf(passAction.getScore()), false, null,
                        "V148 all-bad pass (synthetic)");
                }
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

        // TRACE ORACLE V2 (2026-07-13): selected winner op. evaluateDecision() records the
        // same value into the finalization record as the PRE-SAFETY winner; the bot entry
        // point and DecisionSafety record the emergency/corrections/final-response fields.
        if (traced) TraceSession.recordSelect(bestAction, bestAction.getActionId(),
            Float.valueOf(bestAction.getScore()), bestAction.isHardVetoed(),
            bestAction.getVetoReason(), "winner");
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
