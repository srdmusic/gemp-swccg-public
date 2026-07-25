package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.common.phase.ActivateActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BattleActionTextFacts;
import com.gempukku.swccgo.ai.models.common.phase.BattleActionTextPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployActionTextFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeployActionTextPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeploySequencingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployWeaponPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BattleTargetResolver;
import com.gempukku.swccgo.ai.models.common.phase.BattleWeaponsFacts;
import com.gempukku.swccgo.ai.models.common.phase.BattleWeaponsPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ControlActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ControlDrainAssessment;
import com.gempukku.swccgo.ai.models.common.phase.ControlDrainFacts;
import com.gempukku.swccgo.ai.models.common.phase.MoveBlockedResponsePolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDrainRoutingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveForceEconomyPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveSpyFollowPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveTransitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveVergePolicy;
import com.gempukku.swccgo.ai.models.common.phase.ObjectiveHardLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullSpecificActionFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullSpecificActionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ResponsePolicy;
import com.gempukku.swccgo.ai.models.common.phase.ShieldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.SetupPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldFacts;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Action Text Evaluator
 *
 * Handles text-based action ranking by pattern matching action text.
 * Ported from Python action_text_evaluator.py (~1350 lines)
 *
 * This evaluator provides baseline rankings for common SWCCG actions
 * based on analyzing the action text.
 */
public class ActionTextEvaluator extends ActionEvaluator {

    // Rank deltas (from Python)
    private static final float VERY_GOOD_DELTA = 50.0f;
    private static final float GOOD_DELTA = 30.0f;
    private static final float VERY_BAD_DELTA = -50.0f;

    // Pattern for extracting blueprint ID from action text HTML
    private static final Pattern BLUEPRINT_PATTERN = Pattern.compile("value='([^']+)'");

    // Track barriered targets to avoid playing multiple barriers on same card
    private Set<String> barrieredTargets = new HashSet<>();
    private int barrierTurn = 0;

    // V169 UPDATED 2026-07-06 (audit cross-brain-1): per-turn budget of soft excusals for a
    // blocked endangered mover's retreat. Now that the soft block is small enough to beat
    // Pass (see the V169 branch in evaluate()), a retreat whose destination step keeps
    // cancelling must eventually fall back to the V163 hard veto, or the Keder-style
    // re-pick loop returns. MoveBlockedResponsePolicy owns the exact retry boundary and
    // deltas; this adapter retains one policy-state instance and all game-state reads.
    private final MoveBlockedResponsePolicy.RetryBudget v169RetryBudget =
        new MoveBlockedResponsePolicy.RetryBudget();

    public ActionTextEvaluator() {
        super("ActionText");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();

        // Handle CARD_ACTION_CHOICE and ACTION_CHOICE
        if ("CARD_ACTION_CHOICE".equals(decisionType) || "ACTION_CHOICE".equals(decisionType)) {
            return true;
        }

        // Also handle MULTIPLE_CHOICE for capacity slot decisions, Epic Event choices,
        // and the critical "not activated Force" confirmation
        if ("MULTIPLE_CHOICE".equals(decisionType)) {
            String decisionText = context.getDecisionText();
            if (decisionText != null) {
                String dtLower = decisionText.toLowerCase();
                if (dtLower.contains("capacity slot") || dtLower.contains("choose an option")
                    || dtLower.contains("not activated force") || dtLower.contains("have not activated")) {
                    return true;
                }
                // V79 (Steve, 2026-05-15): Death Star hyperspace destination decisions.
                // After Rando picks "Move using hyperspeed" the engine asks:
                //   1. "Choose parsec to move to " — options are parsec numbers
                //   2. "Choose destination for Death Star at parsec X" — orbit options
                if (dtLower.contains("choose parsec to move to")
                    || (dtLower.contains("choose destination for") && dtLower.contains("parsec"))) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> actionIds = context.getActionIds();
        List<String> actionTexts = context.getActionTexts();
        List<String> cardIds = context.getCardIds();
        Set<String> blocked = context.getBlockedResponses();
        String decisionId = context.getDecisionId();
        PolicyContributionLedger controlLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "control-action-decision" : decisionId);
        PolicyContributionLedger battleForcePushLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "battle-force-push-decision" : decisionId + "-battle-force-push");
        PolicyContributionLedger battleFireLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "battle-fire-decision" : decisionId + "-battle-fire");
        PolicyContributionLedger battleThrowLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "battle-throw-decision" : decisionId + "-battle-throw");
        PolicyContributionLedger battleRedrawLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "battle-redraw-decision" : decisionId + "-battle-redraw");
        PolicyContributionLedger battleInitiationTextLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "battle-initiation-text-decision"
                        : decisionId + "-battle-initiation-text");
        PolicyContributionLedger pullLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "pull-action-decision" : decisionId + "-pull-action");
        PolicyContributionLedger activateTopLevelLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "activate-top-level" : decisionId + "-activate-top-level");
        PolicyContributionLedger activateConfirmationLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "activate-confirmation" : decisionId + "-activate-confirmation");
        PolicyContributionLedger activateBaseLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "activate-base" : decisionId + "-activate-base");

        for (int i = 0; i < actionIds.size(); i++) {
            String actionId = actionIds.get(i);
            String actionText = i < actionTexts.size() ? actionTexts.get(i) : "";
            String cardId = i < cardIds.size() ? cardIds.get(i) : null;
            String textLower = actionText.toLowerCase();

            EvaluatedAction action = new EvaluatedAction(actionId, ActionType.UNKNOWN, 0.0f, actionText);

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: SVC-SAFETY — loop-prevention veto trio (reorg 2026-07-06) ═══
            // Routes: V163 hard veto -100000 and V167 phase-fundamental soft -200 remain here;
            // MoveBlockedResponsePolicy owns V169 endangered-mover -250 / -100000 retry scoring
            // per turn, then falls back to the V163 hard veto. Magnitudes FROZEN (plan: do not retune before T4).
            // Retired: V169 old single-shot -400 is preserved in git history, not executable comments.
            // Cross-refs: SVC-SAFETY peers DecisionSafety (V148 all-bad pass), MOVE (V169-retreat +600 pairing). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // Check if this response is blocked (loop prevention)
            // V163 (2026-06): HARD VETO, not a nudge. The old additive -200 got
            // swamped by later positive rules (e.g. V35.4 spy-flee +250), so a
            // known loop-causing action stayed on top and the game looped forever:
            // Chosen One "Keder The Black" move-loop, turn 10 — Move-using-landspeed
            // scored +250 (V35.4 "flee undercover spy"), its ONLY destination was
            // V41-wrong-direction-blocked, so the target step cancelled, the action
            // re-offered, and -200 < +250 meant Move kept winning. 1000+ iterations.
            // Loop-breakers must DOMINATE (master discipline §2A). Follow the V87
            // hard-block pattern: huge negative + skip all further scoring so no
            // later rule can stack the action back above Pass.
            if (blocked.contains(actionId) || blocked.contains(actionText)) {
                // V167 (Steve, 2026-06): NEVER hard-veto a phase-fundamental action.
                // Live-game regression: "Activate Force" landed in the blocked set (a transient
                // activate-flow cancel-loop) and V163's -100000 hard veto then killed it
                // permanently — Rando passed every Activate phase from turn ~6 on, never got
                // Force in its pile, and stalled (could not deploy or drain) despite a full
                // Reserve Deck. Activating Force is mandatory to play at all; a loop-breaker must
                // never make it impossible. Soft-discourage instead (-200, the pre-V163 value the
                // high-scored Activate action still beats) so the loop is nudged but the bot can
                // still activate. Tactical targets (move/deploy/battle) keep the hard veto.
                String v167tl = actionText != null ? actionText.toLowerCase(java.util.Locale.ROOT) : "";
                // V169 (Steve, 2026-06): a blocked MOVE whose mover is ENDANGERED (outpowered
                // at its current site) must stay attemptable — retreat is how it survives.
                // Replay lk6xgsokjcwrwxuu: Asajj's 'Move using landspeed' was cancel-blocked
                // (V41 had blocked every safe destination), the hard veto made retreat
                // impossible, and she was beaten 6v27 next turn. Non-endangered movers keep
                // the veto (Keder himself wasn't endangered).
                // V169 UPDATED 2026-07-06 (audit cross-brain-1): the old -400 could NEVER let
                // the retreat retry: MoveEvaluator applied a second copy at double strength
                // (ctor -400 PLUS addReasoning -400 = -800, both add per EvaluatedAction) on
                // the same actionId, so the merged score was ~-1050 vs Pass +5. This branch is
                // now has one shared policy owner (MoveEvaluator carries no penalty copy),
                // resized -400 -> -250 so a badly-outmatched retreat can actually win:
                // -250 (here) + V35.4 enemy-presence +150 + MoveEvaluator RETREAT tier +150
                // = +50 > Pass (~5-8). Guarded by the shared 3-attempt budget per turn: if the
                // destination step keeps cancelling (no safe destination), the V163 hard veto
                // resumes instead of re-looping.
                boolean v169EndangeredMover = false;
                if ((v167tl.contains("move using") || v167tl.contains("transport") || v167tl.contains("relocate"))
                        && cardId != null && context.getGameState() != null && context.getGame() != null
                        && context.getPlayerId() != null) {
                    try {
                        com.gempukku.swccgo.game.state.GameState v169Gs = context.getGameState();
                        String v169Pid = context.getPlayerId();
                        PhysicalCard v169Mover = v169Gs.findCardById(Integer.parseInt(cardId));
                        PhysicalCard v169At = v169Mover != null ? v169Mover.getAtLocation() : null;
                        if (v169At != null) {
                            String v169Opp = v169Gs.getOpponent(v169Pid);
                            float v169Our = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(v169Gs, v169At, v169Pid, false, false);
                            float v169Their = context.getGame().getModifiersQuerying()
                                .getTotalPowerAtLocation(v169Gs, v169At, v169Opp, false, false);
                            v169EndangeredMover = MoveBlockedResponsePolicy.isEndangered(
                                true, v169Our, v169Their);
                        }
                    } catch (Exception ignore) { }
                }
                if (ActivateActionPolicy.isActivationChoiceText(actionText)) {
                    action.addReasoning("BLOCKED (loop prevention) — soft (V167: Activate Force never hard-vetoed)", -200.0f);
                    logger.warn("V167: soft-block (not hard veto) on essential action: {}", actionText);
                } else if (v169EndangeredMover) {
                    // V169: shared policy owns the -250 / -100000 retry boundary.
                    String v169Key = (actionText != null && !actionText.isEmpty()) ? actionText : actionId;
                    MoveBlockedResponsePolicy.RetryEvaluation v169Retry =
                        v169RetryBudget.evaluate(context.getTurnNumber(), v169Key);
                    action.addReasoning(v169Retry.reason(), v169Retry.delta());
                    if (!v169Retry.hardBlock()) {
                        logger.warn("V169: soft-block (not hard veto) on endangered mover's action: {} (excusal {}/{} this turn)",
                            actionText, v169Retry.attempt(), v169Retry.retryBudget());
                    } else {
                        logger.warn("V169: retry budget exhausted for '{}' this turn, reverting to V163 hard veto", actionText);
                        actions.add(action);
                        continue;
                    }
                } else {
                    action.addReasoning("BLOCKED (loop prevention) — hard veto", -100000.0f);
                    logger.warn("Blocked action (V163 hard veto): {}", actionText);
                    actions.add(action);
                    continue;
                }
            }

            var hardLossAnalyzer = context.getObjectiveAnalyzer();
            boolean classicHuntDownActive =
                hardLossAnalyzer != null
                && hardLossAnalyzer.isAnalyzed()
                && hardLossAnalyzer.isClassicHuntDownObjective();
            if (classicHuntDownActive) {
                PhysicalCard hardLossSource = null;
                try {
                    if (gameState != null && cardId != null) {
                        hardLossSource = gameState.findCardById(
                            Integer.parseInt(cardId));
                    }
                } catch (Exception ignore) { }
                ObjectiveHardLossPolicy.Threat threat =
                    ObjectiveHardLossPolicy.Threat.NONE;
                try {
                    if (hardLossSource != null && game != null
                            && com.gempukku.swccgo.filters.Filters
                                .Scanning_Crew.accepts(
                                    gameState,
                                    game.getModifiersQuerying(),
                                    hardLossSource)) {
                        threat = ObjectiveHardLossPolicy.Threat
                            .SCANNING_CREW;
                    } else if ((textLower.trim()
                                    .startsWith("duel ")
                                || textLower.trim()
                                    .startsWith("initiate ")
                                    && textLower.contains(" duel"))
                            && !textLower.contains("epic duel")) {
                        threat = ObjectiveHardLossPolicy.Threat
                            .NON_EPIC_DUEL;
                    }
                } catch (Exception ignore) { }
                boolean maulException =
                    threat == ObjectiveHardLossPolicy.Threat
                        .NON_EPIC_DUEL
                    && hardLossAnalyzer
                        .hasClassicHuntDownMaulDuelException(
                            game, context.getPlayerId(),
                            hardLossSource);
                PolicyResult hardLoss =
                    ObjectiveHardLossPolicy.score(
                        new ObjectiveHardLossPolicy.Facts(
                            actionId, true, threat,
                            maulException));
                if (!hardLoss.operations().isEmpty()) {
                    PolicyContributionLedger hardLossLedger =
                        new PolicyContributionLedger(
                            "objective-hard-loss-" + actionId);
                    hardLossLedger.register(hardLoss);
                    PolicyOperationAdapter.apply(
                        action, hardLossLedger);
                    actions.add(action);
                    continue;
                }
            }

            if ("disembark".equals(textLower.trim())
                    && cardId != null && gameState != null
                    && game != null
                    && context.getPlayerId() != null) {
                try {
                    PhysicalCard disembarking =
                            gameState.findCardById(
                                Integer.parseInt(cardId));
                    var objective =
                            context.getObjectiveAnalyzer();
                    var role = objective != null
                            && objective.isAnalyzed()
                            && !objective.isFlipped()
                            && objective
                                .hasActiveRequiredCardDeployActorRule(
                                    game, context.getPlayerId())
                                    ? objective
                                        .classifyGateFormationPieceIfRemoved(
                                            game,
                                            context.getPlayerId(),
                                            disembarking)
                                    : com.gempukku.swccgo.ai.models.common
                                        .strategy.ObjectiveAnalyzer
                                        .FlipGateFormationRole.NONE;
                    PhysicalCard location = disembarking != null
                            ? game.getModifiersQuerying()
                                .getLocationThatCardIsAt(
                                    gameState, disembarking)
                            : null;
                    String opponent =
                            gameState.getOpponent(
                                context.getPlayerId());
                    float friendlyPower = location != null
                            ? game.getModifiersQuerying()
                                .getTotalPowerAtLocation(
                                    gameState, location,
                                    context.getPlayerId(),
                                    false, false)
                            : 0.0f;
                    float opponentPower = location != null
                            && opponent != null
                            ? game.getModifiersQuerying()
                                .getTotalPowerAtLocation(
                                    gameState, location,
                                    opponent, false, false)
                            : 0.0f;
                    MoveObjectiveGateHoldPolicy.Evaluation hold =
                            MoveObjectiveGateHoldPolicy
                                .evaluateCountedFormation(
                                    true, role,
                                    friendlyPower,
                                    opponentPower);
                    if (hold.hardVeto()) {
                        action.setActionType(ActionType.MOVE);
                        action.hardVeto(
                            hold.reason(),
                            TraceRuleId.of(
                                "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_FORMATION_HOLD"),
                            TraceDomainId.MOVE,
                            TraceOutputKind.VETO);
                        actions.add(action);
                        continue;
                    }
                } catch (Exception e) {
                    logger.debug(
                        "Required-card pilot disembark hold failed: {}",
                        e.getMessage());
                }
            }

            if ("move from other battleground site to here"
                    .equals(textLower.trim())
                    && cardId != null && gameState != null
                    && game != null
                    && context.getPlayerId() != null) {
                try {
                    PhysicalCard castle = gameState.findCardById(
                            Integer.parseInt(cardId));
                    var castleObjective =
                            context.getObjectiveAnalyzer();
                    boolean holdAll = castle != null
                            && "209_50".equals(
                                castle.getBlueprintId(true))
                            && castleObjective != null
                            && castleObjective
                                .mustHoldAllVaderCastleReturnMovers(
                                    game, context.getPlayerId(),
                                    castle);
                    MoveObjectiveGateHoldPolicy.Evaluation hold =
                        MoveObjectiveGateHoldPolicy
                            .evaluateVaderCastleReturn(
                                castleObjective != null
                                    && castleObjective
                                        .hasPreFlipRuntimeActorRule(),
                                holdAll);
                    if (hold.hardVeto()) {
                        action.setActionType(ActionType.MOVE);
                        action.hardVeto(
                            hold.reason(),
                            TraceRuleId.of(
                                "MOVE.OBJECTIVE.VADERS_CASTLE_RETURN_HOLD"),
                            TraceDomainId.MOVE,
                            TraceOutputKind.VETO);
                        actions.add(action);
                        continue;
                    }
                } catch (Exception e) {
                    logger.debug(
                        "Hunt Down Castle return hold failed: {}",
                        e.getMessage());
                }
            }

            if ("move from here to other battleground site"
                    .equals(textLower.trim())
                    && cardId != null && gameState != null
                    && game != null && context.getPlayerId() != null) {
                try {
                    PhysicalCard source = gameState.findCardById(
                            Integer.parseInt(cardId));
                    com.gempukku.swccgo.ai.models.chosenone.strategy
                        .ObjectiveAnalyzer objective =
                            context.getObjectiveAnalyzer();
                    if (source != null
                            && "209_50".equals(
                                source.getBlueprintId(true))
                            && objective != null
                            && objective
                                .hasPreFlipRuntimeActorRule()) {
                        if (objective.mustHoldVaderCastleRoutes(
                                game, context.getPlayerId(),
                                source, true)) {
                            action.setActionType(ActionType.MOVE);
                            action.hardVeto(
                                "OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE_HOLD: no safe outbound Castle route",
                                TraceRuleId.of(
                                    "MOVE.OBJECTIVE.VADERS_CASTLE_ROUTE_HOLD"),
                                TraceDomainId.MOVE,
                                TraceOutputKind.VETO);
                            actions.add(action);
                            continue;
                        }
                        boolean safeRoute = objective
                            .hasSafeVaderCastleOutboundRoute(
                                game, context.getPlayerId());
                        MoveDestinationPolicy.Contribution contribution =
                            MoveDestinationPolicy
                                .objectiveActorLocationStart(
                                    safeRoute, "Vader");
                        if (contribution.applies()) {
                            action.setActionType(ActionType.MOVE);
                            action.addReasoning(
                                contribution.reason(),
                                contribution.delta());
                            logger.warn(
                                "HUNT DOWN CASTLE MOVE: safe Vader route from Castle +600");
                        }
                    }
                } catch (Exception e) {
                    logger.debug(
                        "Hunt Down Castle move assessment failed: {}",
                        e.getMessage());
                }
            }

            // ACTIVATE action choice: shared policy owns the V168/V61c four-card floor.
            if (ActivateActionPolicy.isActivationChoiceText(actionText)) {
                int v61cReserve = context.getReserveDeckSize();
                ActivateActionPolicy.Evaluation activation = ActivateActionPolicy.topLevel(
                        actionId, v61cReserve);
                activateTopLevelLedger.register(activation.result());
                PolicyOperationAdapter.apply(action, activateTopLevelLedger);
                switch (activation.mode()) {
                    case TOP_LEVEL_KEEP_BUFFER -> logger.warn(
                            "V61c DESTINY BUFFER: reserve={} <= 4; passing activation (no V168 +5000) on '{}'",
                            v61cReserve, actionText);
                    case TOP_LEVEL_ACTIVATE -> logger.warn(
                            "V168 ALWAYS ACTIVATE: +5000 on '{}'", actionText);
                    default -> { }
                }
            }

            // === V116 (Steve, 2026-05-22): GUARANTEED +100 FLOOR FOR RESERVE-DECK PULLS ===
            // Per Steve: "The game gives players an option to deploy anything from
            // reserve deck should be +100 at least. In the case of the objective,
            // it says it's an option to deploy from reserve. Same with some of the
            // effects. Not sure why they aren't firing when they are lit up green
            // as options to deploy."
            //
            // Safety net: any action whose text indicates a Reserve Deck deploy or
            // download gets an unconditional +100 baseline AT THE TOP of evaluation,
            // before any other rule runs. V60/V67ai/V82 still apply additional
            // positive scoring on top — but even if those handlers fail to reach
            // this action for any reason, the floor guarantees the AI sees it as a
            // positive option. Mirrored in chosenone.
            // V209 PULL preflight: evaluate dead-search knowledge before any positive scorer.
            // Stock action/card ids remain the route boundary; shared code only reads AI facts.
            if (textLower.contains("from reserve deck")
                    || textLower.contains("[download]")) {
                PullActionPolicy.Evaluation earlyPull = PullActionPolicy.evaluateEarlySearch(
                        PullPolicyAdapter.readEarlySearch(
                                context, actionId, actionText, cardId));
                pullLedger.register(earlyPull.result());
                PolicyOperationAdapter.apply(action, pullLedger);
                if (earlyPull.adapterStep()
                        == PullActionPolicy.AdapterStep.CONTINUE_ACTION) {
                    actions.add(action);
                    continue;
                }
            }

            // === V184 (Steve, 2026-06): FIRE "WHEN DEPLOYED" FREE-VALUE TRIGGERS ===
            // When a card deploys with an optional "when deployed, may ..." trigger granting
            // free value, Rando should fire it, not pass. Replay xc19a289odmogph5: Han Solo,
            // Optimistic General — "When deployed, may reveal the top two cards of your
            // Reserve Deck; take one into hand" — was offered as 'Reveal top two cards of
            // Reserve Deck' and scored NOTHING, so Pass (6.0) won and the free card was
            // thrown away. These aren't "from Reserve Deck" pulls ("OF Reserve Deck" /
            // "retrieve Force"), so V116/V60/V97 miss them. Score them above Pass here,
            // GATED on the value actually existing (don't reveal an empty Reserve, don't
            // retrieve from an empty Lost Pile) so a dead trigger is never fired. Free,
            // optional, upside-only — take it whenever it's live.
            if (gameState != null && context.getPlayerId() != null) {
                try {
                    String v184Pid = context.getPlayerId();
                    boolean v184Reveal = (textLower.contains("reveal") || textLower.contains("look at"))
                            && (textLower.contains("reserve") || textLower.contains("top two")
                                || textLower.contains("top card") || textLower.contains("top of"));
                    boolean v184Retrieve = textLower.contains("retrieve") && textLower.contains("force")
                            && !textLower.contains("use ");   // skip cost-bearing retrieves
                    boolean v184Fire = false; String v184Why = null;
                    if (v184Reveal) {
                        boolean v184HasReserve = false;
                        try { v184HasReserve = gameState.getReserveDeckSize(v184Pid) > 0; } catch (Exception ignore) { }
                        if (v184HasReserve) { v184Fire = true; v184Why = "reveal/look at Reserve, take a card"; }
                    } else if (v184Retrieve) {
                        boolean v184HasLost = false;
                        try {
                            java.util.List<PhysicalCard> v184Lp = gameState.getLostPile(v184Pid);
                            v184HasLost = (v184Lp != null && !v184Lp.isEmpty());
                        } catch (Exception ignore) { }
                        if (v184HasLost) { v184Fire = true; v184Why = "retrieve Force from Lost Pile"; }
                    }
                    if (v184Fire) {
                        applyResponsePolicy(action,
                            ResponsePolicy.scoreWhenDeployedFreeTrigger(
                                actionId, v184Why));
                        logger.warn("V184 WHEN-DEPLOYED TRIGGER: '{}' → +300 ({})", actionText, v184Why);
                    }
                } catch (Exception e) { logger.debug("V184 error: {}", e.getMessage()); }
            }

            // === V87 (Steve, 2026-05-16): HARD-BLOCK pilot/passenger capacity slot swaps ===
            // Replay tem28wtufcy7d08j: Sil Unch deployed aboard Blockade Flagship as
            // pilot, then Rando got stuck in a 40+ iteration pilot↔passenger swap loop.
            // DecisionTracker didn't catch it because the wrapping decision text varies
            // ("Optional responses" vs "Use 2 Force - Optional responses"), breaking
            // the key-match for loop detection.
            //
            // These capacity-slot swaps gain nothing for the AI — once a pilot is
            // placed, swapping pilot↔passenger doesn't change combat/movement value.
            // Hard-block both directions outright.
            MoveTransitPolicy.Contribution v87CapacitySwap =
                    MoveTransitPolicy.capacitySlotSwap(textLower);
            if (v87CapacitySwap.applies()) {
                action.addReasoning(v87CapacitySwap.reason(),
                        v87CapacitySwap.delta());
                logger.warn("V87 NO SWAP blocking: '{}' → -3000", actionText);
                actions.add(action);
                continue;
            }

            // V95: dead-interrupt veto now lives in shared PullActionPolicy/PullActionFactsReader.

            // === V134 (Steve, 2026-05-25): ODIN NESLOOR 5-FORCE FLOOR (MOVE phase) ===
            //
            // Steve's standing rule: "must have 5 force in force pile to play
            // Odin Nesloor during move phase." Odin Nesloor & First Aid lets us
            // move multiple characters off one site to another, useful for
            // blocking opponent's force drains next turn — but it costs force
            // and is wasted when force pile is too low to actually drain at
            // the destination next turn.
            //
            // TODO (Steve): fuller proper-use logic later. Should only fire
            // when (a) we plan to block opponent force drain next turn AND
            // (b) we can battle opponent at destination site. The 5-force
            // floor is the simple hotfix; the strategic condition is a
            // future V-tag once we have the gameplay-state predicate.
            //
            // Detection: source card persona is ODIN_NESLOOR. Using a title
            // substring as fallback because the Persona constant may not be
            // registered in the engine enum yet — verify and migrate to
            // Filters.persona(Persona.ODIN_NESLOOR) when available.
            // MoveForceEconomyPolicy owns the title/action match, threshold,
            // score, and exact reason. This adapter retains phase/card reads,
            // scoring mutation, logging, and fail-open exception boundaries.
            if (cardId != null && gameState != null
                    && context.getPhase() == Phase.MOVE) {
                try {
                    PhysicalCard v134Src = gameState.findCardById(Integer.parseInt(cardId));
                    if (v134Src != null
                            && MoveForceEconomyPolicy.isOdinNesloorAction(
                                    v134Src.getTitle(), textLower)) {
                        int v134ForcePile = context.getForcePileSize();
                        MoveForceEconomyPolicy.ActionGate v134Gate =
                                MoveForceEconomyPolicy.odinNesloorFloor(
                                        v134ForcePile);
                        if (v134Gate.applies()) {
                            action.addReasoning(v134Gate.reason(),
                                    v134Gate.delta());
                            logger.warn("V134 ODIN NESLOOR BLOCK: forcePile={} < 5 — block in MOVE phase (-100000)",
                                v134ForcePile);
                        }
                    }
                } catch (NumberFormatException nfe) { /* */ }
                catch (Exception e) { logger.debug("V134 error: {}", e.getMessage()); }
            }

            // === V141 (Steve, 2026-05-26): TRANSPORT INTERRUPT 4-FORCE FLOOR ===
            //
            // Elis Helrot (dark), Nabrun Leids (light), Odin Nesloor (light, move-phase
            // variant — already covered by V134) all share the "draw destiny, use that
            // much Force to transport, or place Interrupt in Lost Pile" mechanic.
            // If we play the interrupt without enough force to cover a destiny draw,
            // the interrupt is WASTED (goes to Lost Pile, no transport happens).
            //
            // Steve's rule 2026-05-26: "needs probably 4+ force to move characters
            // with that card." Below 4 force in pile, hard-block the play.
            //
            // Detection: source card title is Elis Helrot or Nabrun Leids, with a
            // generic three-marker game-text fallback. That fallback also preserves
            // the legacy V134+V141 stack for Odin Nesloor & First Aid.
            // MoveForceEconomyPolicy owns classification, thresholds, score, and
            // exact reason. This adapter retains card/game-text/Force reads,
            // scoring mutation, logging, and fail-open exception boundaries.
            if (cardId != null && context.getGameState() != null) {
                try {
                    com.gempukku.swccgo.game.state.GameState v141Gs = context.getGameState();
                    PhysicalCard v141Src = v141Gs.findCardById(Integer.parseInt(cardId));
                    if (v141Src != null && v141Src.getTitle() != null) {
                        String v141GameText = null;
                        if (!MoveForceEconomyPolicy.isNamedTransportInterrupt(
                                v141Src.getTitle())
                                && v141Src.getBlueprint() != null) {
                            v141GameText = v141Src.getBlueprint().getGameText();
                        }
                        if (MoveForceEconomyPolicy.isTransportInterruptAction(
                                v141Src.getTitle(), v141GameText,
                                textLower)) {
                            int v141ForcePile = context.getForcePileSize();
                            int v141Reserve = context.getReserveDeckSize();
                            MoveForceEconomyPolicy.ActionGate v141Gate =
                                    MoveForceEconomyPolicy.transportInterruptFloor(
                                            v141ForcePile, v141Reserve);
                            if (v141Gate.applies()) {
                                action.addReasoning(v141Gate.reason(),
                                        v141Gate.delta());
                                logger.warn("V141 TRANSPORT BLOCK: {} forcePile={} reserve={} → -2000",
                                    v141Src.getTitle(), v141ForcePile, v141Reserve);
                            }
                        }
                    }
                } catch (NumberFormatException nfe) { /* not numeric */ }
                catch (Exception e) { logger.debug("V141 error: {}", e.getMessage()); }
            }

            // === V142 (Steve, 2026-05-26): WMAOP MODE-SPECIFIC GATING ===
            //
            // We Must Accelerate Our Plans (WMAOP) has three modes:
            //   1. Use 3 Force to take one Effect of any kind from Reserve into hand
            //   2. Deploy a Blockade Flagship site from Reserve Deck
            //   3. Take one Interrupt with the word 'Podracer(s)' from Reserve into hand
            //
            // V29.7 used to hardcode "Blockade Flagship site is the ONLY good use."
            // That was removed earlier this session for over-restricting other modes.
            // Now V142 replaces it with deck-aware preconditions:
            //
            //   - Deploy phase only (avoid using outside deploy phase per Steve 2026-05-26)
            //   - Mode 2 (deploy BFS): only if Blockade Flagship site is NOT yet on
            //     table AND a BFS is in our reserve deck
            //   - Mode 3 (Podracer interrupt): only if a Podracer interrupt is in
            //     reserve (DeckOracle check)
            //   - Mode 1 (Effect pull): only if at least one Effect is in reserve
            //     (DeckOracle check)
            //
            // If no precondition matches → hard block. We don't fire WMAOP just
            // because we can; we fire when it actually delivers value.
            if (cardId != null && context.getGameState() != null) {
                try {
                    com.gempukku.swccgo.game.state.GameState v142Gs = context.getGameState();
                    PhysicalCard v142Src = v142Gs.findCardById(Integer.parseInt(cardId));
                    if (v142Src != null && v142Src.getTitle() != null
                            && v142Src.getTitle().toLowerCase(java.util.Locale.ROOT)
                                .contains("accelerate our plans")) {
                        boolean v142Block = false;
                        String v142Reason = null;

                        // Mode detection via action text
                        boolean v142IsLocationMode = textLower.contains("blockade flagship site")
                            || textLower.contains("blockade flagship: ");
                        boolean v142IsEffectMode = textLower.contains("effect of any kind")
                            || (textLower.contains("effect") && textLower.contains("into hand"));
                        boolean v142IsInterruptMode = textLower.contains("podracer")
                            || (textLower.contains("interrupt") && textLower.contains("into hand"));

                        // BUGFIX 2026-05-28: only gate when this action is actually a
                        // WMAOP play. Previously the phase gate fired on ANY action whose
                        // cardId mapped to WMAOP — including the generic "Activate Force"
                        // action (which carried WMAOP's cardId). That blocked force
                        // activation entirely (Rando stopped activating). Require a WMAOP
                        // mode keyword or "accelerate our plans" in the action text.
                        // (Mode-specific blocks below already require their mode flag, so
                        // only the phase gate needed this guard.)
                        boolean v142IsWmaopPlay = v142IsLocationMode || v142IsEffectMode
                            || v142IsInterruptMode
                            || textLower.contains("accelerate our plans");

                        // Phase gate — only for genuine WMAOP plays
                        if (v142IsWmaopPlay && context.getPhase() != Phase.DEPLOY) {
                            v142Block = true;
                            v142Reason = "not deploy phase";
                        }

                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle v142Oracle =
                            context.getDeckOracle();

                        if (!v142Block && v142IsLocationMode) {
                            // Block if BFS already on table
                            for (PhysicalCard loc : v142Gs.getTopLocations()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                if (loc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                        .contains("blockade flagship")) {
                                    v142Block = true;
                                    v142Reason = "Blockade Flagship site already on table";
                                    break;
                                }
                            }
                        }

                        if (!v142Block && v142IsEffectMode && v142Oracle != null
                                && v142Oracle.isAnalyzed()) {
                            // Block if no effects in reserve
                            java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> v142Effects =
                                v142Oracle.getCardsByCategory(
                                    com.gempukku.swccgo.common.CardCategory.EFFECT,
                                    com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                            if (v142Effects == null || v142Effects.isEmpty()) {
                                v142Block = true;
                                v142Reason = "no Effects in reserve for Effect-pull mode";
                            }
                        }

                        if (!v142Block && v142IsInterruptMode && v142Oracle != null
                                && v142Oracle.isAnalyzed()) {
                            // Block if no podracer interrupts in reserve
                            boolean v142HasPodracer = false;
                            java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> v142Interrupts =
                                v142Oracle.getCardsByCategory(
                                    com.gempukku.swccgo.common.CardCategory.INTERRUPT,
                                    com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                            if (v142Interrupts != null) {
                                for (com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard dc : v142Interrupts) {
                                    if (dc == null) continue;
                                    String dcText = dc.getGameText() != null
                                        ? dc.getGameText().toLowerCase(java.util.Locale.ROOT) : "";
                                    if (dcText.contains("podracer")) {
                                        v142HasPodracer = true;
                                        break;
                                    }
                                }
                            }
                            if (!v142HasPodracer) {
                                v142Block = true;
                                v142Reason = "no Podracer interrupts in reserve";
                            }
                        }

                        applyPullSpecificActionPolicy(action,
                            PullSpecificActionPolicy.scoreWmaopGate(
                                new PullSpecificActionFacts.Gate(
                                    actionId, v142Block,
                                    v142Reason != null ? v142Reason : "")));
                        if (v142Block) {
                            logger.warn("V142 WMAOP BLOCK: {} ({})",
                                v142Src.getTitle(), v142Reason);
                        }
                    }
                } catch (NumberFormatException nfe) { /* not numeric */ }
                catch (Exception e) { logger.debug("V142 error: {}", e.getMessage()); }
            }

            // === V147 (Steve, 2026-05-28): I AM YOUR FATHER — DON'T SEARCH EMPTY LOST PILE ===
            //
            // I Am Your Father: "Once per turn, may [download] Vader's Lightsaber
            // (or lose 1 Force to deploy it from Lost Pile)."
            //
            // Replay 37orjzqd6feo6igp turn 2: Rando lost 1 Force to deploy Vader's
            // Lightsaber from Lost Pile — but the only card in Lost Pile was Prepared
            // Defenses. Saber was in Reserve. Search failed, 1 Force wasted. He should
            // have used the FREE [download] from Reserve mode (which he did correctly
            // at event 869).
            //
            // V147: if the action is the Lost-Pile deploy mode AND Vader's Lightsaber
            // is NOT actually in our Lost Pile, hard-block it. The Reserve [download]
            // mode is free and preferred anyway.
            if (gameState != null && context.getPlayerId() != null
                    && textLower.contains("from lost pile")
                    && (textLower.contains("vader's lightsaber")
                        || textLower.contains("vader’s lightsaber"))) {
                try {
                    boolean v147SaberInLostPile = false;
                    java.util.List<PhysicalCard> v147Lost = gameState.getLostPile(context.getPlayerId());
                    if (v147Lost != null) {
                        for (PhysicalCard lc : v147Lost) {
                            if (lc == null || lc.getTitle() == null) continue;
                            if (lc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                    .contains("vader's lightsaber")
                                || lc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                    .contains("vader’s lightsaber")) {
                                v147SaberInLostPile = true;
                                break;
                            }
                        }
                    }
                    applyPullSpecificActionPolicy(action,
                        PullSpecificActionPolicy.scoreLostPileLightsaberGate(
                            new PullSpecificActionFacts.Gate(
                                actionId, !v147SaberInLostPile, "")));
                    if (!v147SaberInLostPile) {
                        logger.warn("V147 IAYF BLOCK: saber not in Lost Pile — failed search would waste 1 Force");
                    }
                } catch (Exception e) { logger.debug("V147 error: {}", e.getMessage()); }
            }

            // === V155 (Steve, 2026-05-28): WELCOME HOME, LORD TYRANUS — SAVE FOR BATTLE ===
            // (Implements the previously-parked "V152" Welcome Home idea.)
            // Welcome Home, Lord Tyranus (Lost Interrupt), 3 modes:
            //   1. (Dooku apprentice) take Petranaki Arena OR The Works into hand from Reserve
            //   2. (Dooku + Sidious on table) cancel Sense
            //   3. ONCE PER GAME: if Darth Tyranus in battle and about to draw battle destiny,
            //      instead use his ABILITY NUMBER — a guaranteed high battle destiny. Premium.
            // Steve: "save this card for battle with Dooku once The Works is in hand or on table.
            // He keeps searching his reserve for The Works after it's already out. Very useful
            // in battle." Screenshot 2026-05-28 (turn 1): Rando fired mode 1 to pull a location
            // while The Works was already on the table — burning a premium battle interrupt on
            // a near no-op.
            // FIX: if the action is the mode-1 location pull AND The Works is already on the
            // table OR in hand, hard-block (-2000) so the card is held for the battle mode.
            // Universal text + title detection — no card-name lists beyond this one card's modes.
            //
            // V155 GATE FIX (Steve 2026-05-29, after replay ss2jc7): the original gate also
            // required "the works"/"petranaki" in the action text, but the play-action text is
            // actually "Take location into hand from Reserve Deck" (generic — names are in the
            // card's game text, not the action text). That made V155 fire 0× in the replay
            // even though The Works was on the table. Gate now keys on:
            //   (a) action is a reserve pull-into-hand ("into hand from reserve")
            //   (b) source card title contains "welcome home" (checked just below)
            // The source-card check is the specific filter — no need to match target names too.
            if (cardId != null && gameState != null
                    && textLower.contains("into hand from reserve")) {
                try {
                    PhysicalCard v155Src = gameState.findCardById(Integer.parseInt(cardId));
                    if (v155Src != null && v155Src.getTitle() != null
                            && v155Src.getTitle().toLowerCase(java.util.Locale.ROOT).contains("welcome home")) {
                        // Use the Deck Oracle to decide if the pull is worth firing. Block
                        // (save the card for battle) when EITHER:
                        //   (a) DEAD PULL — neither Petranaki Arena NOR The Works is currently
                        //       in the Reserve Deck. The Oracle tracks live zones AND the deck
                        //       list, so a location not in the deck at all (e.g. NO Petranaki
                        //       Arena in this deck) reads as not-in-reserve, as does one already
                        //       pulled out. Steve's case: no Petranaki Arena + The Works already
                        //       on table = nothing to fetch.
                        //   (b) SAVE FOR BATTLE — The Works is already on the table or in hand
                        //       (even if Petranaki Arena is still pullable, hold for the battle mode).
                        // Falls back to a gameState table/hand scan for The Works if the Oracle
                        // is unavailable.
                        boolean v155Block = false;
                        String v155Why = null;
                        com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle v155Oracle = context.getDeckOracle();
                        if (v155Oracle != null && v155Oracle.isAnalyzed()) {
                            boolean worksInReserve = v155Oracle.isCardInReserve("The Works");
                            boolean petranakiInReserve = v155Oracle.isCardInReserve("Petranaki Arena");
                            boolean worksOut = v155Oracle.isCardInPlay("The Works") || v155Oracle.isCardInHand("The Works");
                            if (!worksInReserve && !petranakiInReserve) {
                                v155Block = true;
                                v155Why = "DEAD PULL — neither The Works nor Petranaki Arena is in the Reserve Deck (nothing to fetch)";
                            } else if (worksOut) {
                                v155Block = true;
                                v155Why = "The Works already on table/in hand (save for battle, Petranaki still pullable)";
                            }
                        } else {
                            // Fallback (no Oracle): scan table/hand for The Works.
                            boolean worksOut = false;
                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                if (pc == null || pc.getTitle() == null) continue;
                                if (pc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("the works")
                                        && pc.getZone() != null && pc.getZone().isInPlay()) { worksOut = true; break; }
                            }
                            if (!worksOut) {
                                java.util.List<PhysicalCard> v155Hand = gameState.getHand(context.getPlayerId());
                                if (v155Hand != null) {
                                    for (PhysicalCard hc : v155Hand) {
                                        if (hc != null && hc.getTitle() != null
                                                && hc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("the works")) { worksOut = true; break; }
                                    }
                                }
                            }
                            if (worksOut) { v155Block = true; v155Why = "The Works already on table/in hand (no Oracle)"; }
                        }
                        applyPullSpecificActionPolicy(action,
                            PullSpecificActionPolicy.scoreWelcomeHome(
                                new PullSpecificActionFacts.WelcomeHome(
                                    actionId, v155Block, v155Why != null ? v155Why : "")));
                        if (v155Block) {
                            logger.warn("V155 WELCOME HOME BLOCK: {} — save for battle, block mode-1 location pull", v155Why);
                        }
                    }
                } catch (NumberFormatException nfe) { /* not a numeric cardId */ }
                catch (Exception e) { logger.debug("V155 error: {}", e.getMessage()); }
            }

            // === V160 (Steve, 2026-05-29): SHIELD WILL BE DOWN IN MOMENTS — PUSH TARGET THE MAIN GENERATOR ===
            // The deck's flip condition is Main Power Generators "blown away" — and only
            // Target The Main Generator (Epic Event, deploys on Ice Plains) lets the AT-AT
            // Cannon fire at the generators. Without TtMG on the table, the deck CAN'T win.
            // Steve: "He needs to get the epic event on the table so he can blow up the hoth
            // generator." Push any action involving Target The Main Generator when the deck
            // is recognized (covers the deploy and the fire-AT-AT response).
            if (textLower.contains("target the main generator")) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v160OA = context.getObjectiveAnalyzer();
                if (v160OA != null && v160OA.isAnalyzed() && v160OA.isShieldWillBeDown()) {
                    applyDeployActionTextPolicy(action,
                        DeployActionTextPolicy.scoreMainGenerator(
                            new DeployActionTextFacts.MainGeneratorFacts(actionId)));
                    logger.warn("V160 SHIELD WILL BE DOWN: pushing '{}' — Target The Main Generator action (+800)", actionText);
                }
            }

            // === V158 RESERVE-DEPLOY BYPASS GUARD (Steve 2026-05-29, replay ss2jc7) ===
            // The fork's V158 (DeployEvaluator) catches normal weapon-deploy actions but is
            // bypassed when a weapon comes FROM RESERVE via an effect (Evil Is Everywhere
            // deploys [Episode I] lightsaber on Sidious; Sidious' Lightsaber from Reserve
            // on Sidious). Replay: Lord Sidious got Asajj Ventress' Lightsabers (t1) AND
            // Sidious' Lightsaber (t3) — DOUBLE-ARMED, breaking the one-weapon-per-char
            // rule. Defensive guard at the action-text layer: when an action text matches
            // "<weapon-word> from Reserve Deck on <character>", look up the named character
            // on the table and block (-9999) if it already has a weapon attached. Appended
            // into V158 (no new tag) — Steve: "avoid splintering off versions like before."
            if (cardId != null && gameState != null
                    && (textLower.contains("from reserve deck on") || textLower.contains("from reserve on"))
                    && (textLower.contains("lightsaber") || textLower.contains("blaster")
                        || textLower.contains("rifle") || textLower.contains("bowcaster")
                        || textLower.contains("weapon"))) {
                try {
                    int onIdx = textLower.lastIndexOf(" on ");
                    if (onIdx > 0) {
                        String targetSubstr = textLower.substring(onIdx + 4).trim();
                        for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                            if (pc == null || pc.getTitle() == null) continue;
                            if (pc.getBlueprint() == null
                                    || pc.getBlueprint().getCardCategory()
                                        != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                            if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                            String pcTitle = pc.getTitle().toLowerCase(java.util.Locale.ROOT);
                            String pcFirstPart = pcTitle.split(",")[0].trim();
                            if (!targetSubstr.contains(pcTitle) && !targetSubstr.contains(pcFirstPart)) continue;
                            // Found the target character — check if armed.
                            boolean v158Armed = false;
                            java.util.List<PhysicalCard> v158Att = gameState.getAttachedCards(pc);
                            if (v158Att != null) {
                                for (PhysicalCard att : v158Att) {
                                    if (att != null && att.getBlueprint() != null
                                            && att.getBlueprint().getCardCategory()
                                                == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                        v158Armed = true; break;
                                    }
                                }
                            }
                            PolicyContributionLedger v213ReserveTargetLedger =
                                new PolicyContributionLedger("deploy-weapon-reserve-target-" + actionId);
                            v213ReserveTargetLedger.register(DeployWeaponPolicy.evaluateReserveTarget(
                                new DeployWeaponPolicy.ReserveTargetFacts(
                                    actionId, pc.getTitle(), v158Armed)));
                            PolicyOperationAdapter.apply(action, v213ReserveTargetLedger);
                            if (v158Armed) {
                                logger.warn("V158 RESERVE-DEPLOY BLOCK: target {} already armed → -9999", pc.getTitle());
                            }
                            break;
                        }
                    }
                } catch (Exception e) { logger.debug("V158 RESERVE-DEPLOY GUARD error: {}", e.getMessage()); }
            }

            // V158 RESERVE-DEPLOY GUARD — NO-WIELDER branch (appended 2026-05-29, replay
            // filx81 turn 2): the first branch above catches "<weapon> from Reserve Deck
            // on <character>" + character armed. This second branch catches the auto-
            // targeted case where the action text lacks "on X" because the persona is
            // implied by the weapon's name. filx81: Rando pulled Vader's Lightsaber via
            // I Am Your Father (V) on turn 2, but Lord Vader didn't deploy until turn 3
            // — the saber landed in hand, then was lost as force-loss fodder. Pattern
            // detection: action text contains "X's Lightsaber" + "from Reserve". Extract
            // X (the persona word before "'s lightsaber"), check the table; if X isn't
            // present, block -9999 (no wielder = wasted pull). No new V-tag.
            if (cardId != null && gameState != null && textLower != null
                    && textLower.contains("from reserve")
                    && textLower.contains("'s lightsaber")) {
                try {
                    int v158nwIdx = textLower.indexOf("'s lightsaber");
                    if (v158nwIdx > 0) {
                        String v158nwBefore = textLower.substring(0, v158nwIdx).trim();
                        String[] v158nwParts = v158nwBefore.split("[\\s•·]+");
                        String v158nwPersona = v158nwParts.length > 0
                            ? v158nwParts[v158nwParts.length - 1].trim() : "";
                        if (v158nwPersona.length() > 1) {
                            boolean v158nwOnTable = false;
                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                if (pc == null || pc.getTitle() == null) continue;
                                if (pc.getBlueprint() == null
                                        || pc.getBlueprint().getCardCategory()
                                            != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                                // V180 (2026-06): match the wielder by PERSONA, not just printed
                                // title. "Young Skywalker" carries Persona.LUKE but his title has
                                // no "luke" — the old title-only check wrongly fired NO-WIELDER and
                                // blocked Luke's own saber 12x in one game (E1, replay aab2jiaa5sca),
                                // so Luke fought bare-handed all game. Same lesson as senators:
                                // identity lives in the persona set, not always the printed name.
                                if (pc.getTitle().toLowerCase(java.util.Locale.ROOT).contains(v158nwPersona)) {
                                    v158nwOnTable = true; break;
                                }
                                java.util.Set<com.gempukku.swccgo.common.Persona> v158nwPersonas =
                                    pc.getBlueprint().getPersonas();
                                if (v158nwPersonas != null) {
                                    for (com.gempukku.swccgo.common.Persona v158nwP : v158nwPersonas) {
                                        if (v158nwP != null && v158nwP.name()
                                                .toLowerCase(java.util.Locale.ROOT).contains(v158nwPersona)) {
                                            v158nwOnTable = true; break;
                                        }
                                    }
                                    if (v158nwOnTable) break;
                                }
                            }
                            PolicyContributionLedger v213ReserveWielderLedger =
                                new PolicyContributionLedger("deploy-weapon-reserve-wielder-" + actionId);
                            v213ReserveWielderLedger.register(DeployWeaponPolicy.evaluateReserveWielder(
                                new DeployWeaponPolicy.ReserveWielderFacts(
                                    actionId, v158nwPersona, v158nwOnTable)));
                            PolicyOperationAdapter.apply(action, v213ReserveWielderLedger);
                            if (!v158nwOnTable) {
                                logger.warn("V158 NO-WIELDER BLOCK: '{}' not on table — block weapon pull -9999", v158nwPersona);
                            }
                        }
                    }
                } catch (Exception e) { logger.debug("V158 NO-WIELDER GUARD error: {}", e.getMessage()); }
            }

            // === V144 (Steve, 2026-05-26): YOU ARE BEATEN MODE GATING ===
            //
            // You Are Beaten (Lost Interrupt) has three modes:
            //   1. Use 2 Force, target char present with our warrior+lightsaber →
            //      can't move/battle until end of our next turn (BATTLE FREEZE)
            //   2. Use 1 Force, search Reserve for I Am Your Father, into hand
            //   3. Cancel Uncontrollable Fury
            //
            // Steve's rule: save You Are Beaten in hand for BATTLE use (Mode 1).
            // Especially block Mode 2 (search IAYF) if I Am Your Father is
            // already on table — that mode is then useless. Also bias against
            // Mode 2 outside of needing IAYF.
            if (cardId != null && context.getGameState() != null) {
                try {
                    com.gempukku.swccgo.game.state.GameState v144Gs = context.getGameState();
                    PhysicalCard v144Src = v144Gs.findCardById(Integer.parseInt(cardId));
                    if (v144Src != null && v144Src.getTitle() != null
                            && v144Src.getTitle().toLowerCase(java.util.Locale.ROOT)
                                .contains("you are beaten")) {
                        // Mode 2 detection: action text mentions "I Am Your Father" search.
                        // Per Steve 2026-05-26: NEVER use You Are Beaten for the IAYF
                        // search mode. The card is for battle freeze (Mode 1) or Cancel
                        // Uncontrollable Fury (Mode 3). Hard-block Mode 2 universally.
                        boolean v144IsIayfSearch = textLower.contains("i am your father")
                            || (textLower.contains("father") && textLower.contains("into hand"));
                        if (v144IsIayfSearch) {
                            logger.warn("V144 YOU ARE BEATEN: blocking IAYF search mode universally");
                        }
                        // Mode 1 (battle freeze) — encourage when in battle phase
                        boolean v144IsBattleFreeze = textLower.contains("cannot move or battle")
                            || textLower.contains("target a character present");
                        applyPullSpecificActionPolicy(action,
                            PullSpecificActionPolicy.scoreYouAreBeatenSearch(
                                new PullSpecificActionFacts.YouAreBeatenSearch(
                                    actionId, v144IsIayfSearch)));
                        applyBattleActionTextPolicy(action,
                            BattleActionTextPolicy.scoreYouAreBeatenMode(
                                new BattleActionTextFacts.YouAreBeatenModeFacts(
                                    actionId, v144IsBattleFreeze,
                                    context.getPhase() == Phase.BATTLE)));
                    }
                } catch (NumberFormatException nfe) { /* not numeric */ }
                catch (Exception e) { logger.debug("V144 error: {}", e.getMessage()); }
            }

            // V97: pull-before-activate ordering now lives in shared PullActionPolicy.

            // V100: location-pull sequencing now lives in shared PullActionPolicy/PullActionFactsReader.

            // V79 (Steve, 2026-05-15): VERGE — DEATH STAR PARSEC / ORBIT MULTIPLE_CHOICE
            // After picking "Move using hyperspeed" the engine fires a
            // MULTIPLE_CHOICE: "Choose parsec to move to ". Options are parsec
            // numbers as strings. Score the one closest to 7 (Scarif).
            // Then a second MULTIPLE_CHOICE may fire: "Choose destination for
            // Death Star at parsec X" with orbit options. Pick Scarif.
            //
            // V103 (Steve, 2026-05-20): MULTIPLE_CHOICE Verge detection was failing.
            // Bug: v79Verge returned false even when the DeployEvaluator scan correctly
            // identified Verge on the same turn. Fix:
            //   - mirror DeployEvaluator's pZone.isInPlay() guard so we skip cards in
            //     piles (which can carry titles that match objectives but aren't active).
            //   - log v79Verge/v79AtScarif + iteration count for fast debug next time.
            //   - loosen owner match (~Rando_Cal vs Rando_Cal can differ); also treat a
            //     pure decision-text fallback ("Choose parsec to move to" + any Death
            //     Star we own) as Verge.
            //   - ALWAYS produce a scored action so the engine doesn't fall back to
            //     option-0 (parsec 2). If V79 path doesn't fire, score by distance to
            //     parsec 7 anyway (smaller bonus +300).
            {
                String v79DtLower = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase() : "";
                boolean v79IsParsecChoice = v79DtLower.contains("choose parsec to move to");
                boolean v79IsDestChoice = v79DtLower.contains("choose destination for")
                    && v79DtLower.contains("parsec");
                if ((v79IsParsecChoice || v79IsDestChoice) && gameState != null
                        && context.getPlayerId() != null) {
                    // Confirm Verge of Greatness active + Death Star not at Scarif
                    boolean v79Verge = false;
                    boolean v79AtScarif = false;
                    boolean v79HaveDeathStar = false;
                    int v79IteratedCards = 0;
                    String v79PlayerId = context.getPlayerId();
                    String v79PidNorm = v79PlayerId != null
                        ? v79PlayerId.replace("~", "") : "";
                    try {
                        for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                            if (pc == null) continue;
                            // V103: loosen owner match — accept ~Rando_Cal vs Rando_Cal,
                            // and accept null owner (objective-on-table edge cases).
                            String pOwner = pc.getOwner();
                            String pOwnerNorm = pOwner != null
                                ? pOwner.replace("~", "") : "";
                            boolean ownerMatches = pOwner == null
                                || pOwner.equals(v79PlayerId)
                                || pOwnerNorm.equals(v79PidNorm);
                            if (!ownerMatches) continue;
                            if (pc.getBlueprint() == null) continue;
                            // V103: skip cards not in play (mirror DeployEvaluator guard)
                            com.gempukku.swccgo.common.Zone pZone = pc.getZone();
                            if (pZone == null || !pZone.isInPlay()) continue;
                            v79IteratedCards++;
                            String t = pc.getTitle() != null
                                ? pc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            if (t.contains("on the verge of greatness")
                                    || t.contains("taking control of the weapon")) {
                                v79Verge = true;
                            }
                            if (t.contains("death star")
                                    && pc.getBlueprint().getCardCategory() == CardCategory.LOCATION) {
                                v79HaveDeathStar = true;
                                // V79 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681):
                                // getAtLocation() is ALWAYS null for the Death Star mobile-system
                                // LOCATION card, so v79AtScarif never went true and this arm kept
                                // paying to steer a parked Death Star (from orbit the parsec-7 pick
                                // is the DEEP-SPACE EXIT — the engine excludes the orbited system,
                                // MoveMobileSystemUsingHyperspeedAction:82). Use the engine's orbit
                                // primitive getSystemOrbited() (same check as the flip condition,
                                // Filters.isOrbiting(Title.Scarif), Card216_011:122). With
                                // v79AtScarif true the steering branch below is skipped and the V103
                                // PARSEC FALLBACK's closest-to-7 pick IS the stay pick (Scarif =
                                // parsec 7) — no extra !flipped gate needed; post-flip deep-space
                                // recovery steering intentionally stays live so the DS re-orbits.
                                // PhysicalCard dsLoc = pc.getAtLocation();
                                // if (dsLoc != null && dsLoc.getTitle() != null
                                //         && dsLoc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("scarif")) {
                                //     v79AtScarif = true;
                                // }
                                String dsOrbited = pc.getSystemOrbited();
                                if (dsOrbited != null
                                        && dsOrbited.toLowerCase(java.util.Locale.ROOT).contains("scarif")) {
                                    v79AtScarif = true;
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    logger.warn("V103 PARSEC DETECT: verge={} atScarif={} haveDeathStar={} iterated={} dt='{}'",
                        v79Verge, v79AtScarif, v79HaveDeathStar, v79IteratedCards,
                        context.getDecisionText());

                    // V103: fallback — if scan didn't find Verge but we DO own a Death Star
                    // and the decision text is the parsec/destination prompt, treat as Verge.
                    if (!v79Verge && v79HaveDeathStar) {
                        v79Verge = true;
                        logger.warn("V103 PARSEC FALLBACK: Verge implied by Death Star ownership + parsec prompt");
                    }

                    if (v79Verge && !v79AtScarif) {
                        if (v79IsParsecChoice) {
                            // actionText is the parsec number (e.g., "2" or "6")
                            Integer parsec = null;
                            try { parsec = Integer.parseInt(actionText.trim()); }
                            catch (Exception e) {
                                // Some implementations might prefix the number
                                java.util.regex.Matcher pm = java.util.regex.Pattern
                                    .compile("(\\d+)").matcher(actionText);
                                if (pm.find()) {
                                    try { parsec = Integer.parseInt(pm.group(1)); }
                                    catch (Exception ee) { /* ignore */ }
                                }
                            }
                            if (parsec != null) {
                                MoveVergePolicy.ParsecChoiceEvaluation v79ParsecChoice =
                                    MoveVergePolicy.evaluateParsecChoice(parsec);
                                action.addReasoning(
                                    v79ParsecChoice.contribution().reason(),
                                    v79ParsecChoice.contribution().delta());
                                if (v79ParsecChoice.branch()
                                        == MoveVergePolicy.ParsecChoiceBranch.PARSEC_SEVEN) {
                                    logger.warn("V79 PARSEC CHOICE: parsec 7 (Scarif) → +1500");
                                } else if (v79ParsecChoice.branch()
                                        == MoveVergePolicy.ParsecChoiceBranch.ONE_HOP_FROM_SCARIF) {
                                    logger.warn("V79 PARSEC CHOICE: parsec {} → +1200", parsec);
                                } else if (v79ParsecChoice.branch()
                                        == MoveVergePolicy.ParsecChoiceBranch.TOWARD_SCARIF) {
                                    logger.warn("V79 PARSEC CHOICE: parsec {} → +800", parsec);
                                } else {
                                    logger.warn("V79 PARSEC CHOICE WRONG WAY: parsec {} → -800", parsec);
                                }
                            }
                        } else if (v79IsDestChoice) {
                            // actionText is the destination — pick Scarif over deep space
                            MoveVergePolicy.ParsecChoiceEvaluation v79DestinationChoice =
                                MoveVergePolicy.evaluateDestinationChoice(
                                    textLower.contains("scarif"));
                            action.addReasoning(
                                v79DestinationChoice.contribution().reason(),
                                v79DestinationChoice.contribution().delta());
                            if (v79DestinationChoice.branch()
                                    == MoveVergePolicy.ParsecChoiceBranch.ORBIT_SCARIF) {
                                logger.warn("V79 DESTINATION: orbit Scarif → +1500");
                            } else {
                                logger.warn("V79 DESTINATION: '{}' (not Scarif) → -200", actionText);
                            }
                        }
                        // V79 (Steve, 2026-05-15): MUST add action to output list.
                        // The default ActionTextEvaluator flow only appends actions
                        // when a specific pattern branch matches the action text.
                        // For parsec-number action texts ("2", "6") none of those
                        // pattern branches match, so the action would be dropped
                        // and the engine would report "No evaluators produced actions".
                        actions.add(action);
                        continue;
                    }

                    // V103 PARSEC FALLBACK: if no Verge/DS detected but the engine
                    // is still asking us to choose a parsec, score by distance to 7
                    // anyway so the AI picks the better option instead of defaulting
                    // to the first option (typically parsec 2).
                    if (v79IsParsecChoice) {
                        Integer fparsec = null;
                        try { fparsec = Integer.parseInt(actionText.trim()); }
                        catch (Exception e) {
                            java.util.regex.Matcher pm = java.util.regex.Pattern
                                .compile("(\\d+)").matcher(actionText);
                            if (pm.find()) {
                                try { fparsec = Integer.parseInt(pm.group(1)); }
                                catch (Exception ee) { /* ignore */ }
                            }
                        }
                        if (fparsec != null) {
                            MoveVergePolicy.ParsecChoiceEvaluation v103ParsecFallback =
                                MoveVergePolicy.evaluateParsecFallback(fparsec);
                            action.addReasoning(
                                v103ParsecFallback.contribution().reason(),
                                v103ParsecFallback.contribution().delta());
                            logger.warn("V103 PARSEC FALLBACK: parsec {} dist {} → +{}",
                                fparsec, v103ParsecFallback.distanceFromScarif(),
                                (int)v103ParsecFallback.contribution().delta());
                            actions.add(action);
                            continue;
                        }
                    }
                }
            }

            // V67bi FORCE LIGHTNING SELF-TARGET HARD-BLOCK (Steve, 2026-05-10)
            // ===================================================================
            // Hard-block Force Lightning if there's no opponent character in
            // play to target. The engine already requires the granting card
            // (Emperor or equivalent) to be present for the action to even
            // appear, so we don't need to look for Emperor — we just verify a
            // valid OPPONENT target exists. Otherwise Rando burns 5 force to
            // hit his own character.
            //
            // Pattern extends to any "target a character" Sith damage interrupt
            // (Force Push, Lightsaber Combat, etc.) — add per card-title as they
            // surface in replays.
            {
                GameState v67biGs = context.getGameState();
                String v67biPid = context.getPlayerId();
                if (cardId != null && v67biGs != null && v67biPid != null) {
                    try {
                        PhysicalCard v67biSource = v67biGs.findCardById(Integer.parseInt(cardId));
                        if (v67biSource != null && v67biSource.getTitle() != null
                                && v67biSource.getTitle().toLowerCase(java.util.Locale.ROOT)
                                       .contains("force lightning")) {
                            int v67biOpps = 0;
                            for (PhysicalCard pc : v67biGs.getAllPermanentCards()) {
                                if (pc == null || pc.getBlueprint() == null) continue;
                                if (v67biPid.equals(pc.getOwner())) continue;
                                if (pc.getBlueprint().getCardCategory()
                                        != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                                v67biOpps++;
                                if (v67biOpps > 0) break;
                            }
                            if (v67biOpps == 0) {
                                applyBattleWeaponsPolicy(action,
                                    BattleWeaponsPolicy.scoreForceLightning(
                                        new BattleWeaponsFacts.ForceLightningFacts(
                                            actionId, false)));
                                logger.warn("V67bi FORCE LIGHTNING BLOCK: 0 opponent chars in play — hard-block '{}'",
                                    actionText);
                            } else {
                                logger.info("V67bi FORCE LIGHTNING OK: opponent char(s) in play — allow targeting");
                            }
                        }
                    } catch (NumberFormatException nfe) { /* ignore */ }
                      catch (Exception e) {
                        logger.debug("V67bi check error: {}", e.getMessage());
                    }
                }
            }

            // ACTIVATE zero-confirmation choice: shared policy owns the V38.3/V61c pair.
            {
                String decisionTextCheck = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase() : "";
                if (decisionTextCheck.contains("not activated force") || decisionTextCheck.contains("have not activated")) {
                    int v38cReserve = context.getReserveDeckSize();
                    ActivateActionPolicy.Evaluation activation = ActivateActionPolicy.zeroConfirmation(
                            actionId, textLower, v38cReserve);
                    activateConfirmationLedger.register(activation.result());
                    PolicyOperationAdapter.apply(action, activateConfirmationLedger);
                    switch (activation.mode()) {
                        case CONFIRM_KEEP_BUFFER -> logger.warn(
                                "V61c DESTINY BUFFER: reserve={} <= 4; confirming pass (skip activation)",
                                v38cReserve);
                        case REJECT_BUFFER_REACTIVATION -> logger.warn(
                                "V61c DESTINY BUFFER: rejecting reactivation with reserve={}",
                                v38cReserve);
                        case CONFIRM_REACTIVATION -> logger.warn(
                                "V38.3 MUST ACTIVATE: Choosing 'No' to go back and activate Force");
                        case REJECT_SKIP -> logger.warn(
                                "V38.3 BLOCKED: Refusing to skip Force activation");
                        default -> { }
                    }
                }
            }

            // ========== V53c: BLOCK WOKLING EFFECT SEARCH (EARLY CHECK) ==========
            // Wokling (V) costs 3 Force to search for an Effect from Reserve Deck.
            // Action text: "Take an Effect into hand from Reserve Deck"
            // MUST check EARLY before V29.7 PULL FIRST gives it +250.
            // Check source card ID — if it's Wokling (bp 200_47), hard block.
            boolean woklingSearch = textLower.contains("effect")
                && textLower.contains("reserve deck") && textLower.contains("take");
            if (woklingSearch) {
                boolean isWoklingSource = false;
                if (cardId != null && gameState != null) {
                    try {
                        PhysicalCard wokSrc = gameState.findCardById(Integer.parseInt(cardId));
                        if (wokSrc != null && wokSrc.getTitle() != null
                            && wokSrc.getTitle().toLowerCase(Locale.ROOT).contains("wokling")) {
                            isWoklingSource = true;
                        }
                        // Also check blueprint ID
                        if (wokSrc != null && wokSrc.getBlueprintId(true) != null
                            && wokSrc.getBlueprintId(true).equals("200_47")) {
                            isWoklingSource = true;
                        }
                    } catch (Exception e) { /* ignore */ }
                }
                DeploySequencingPolicy.Evaluation wokling =
                    DeploySequencingPolicy.woklingEarlySearch(
                        actionId, true, isWoklingSource, context.getTurnNumber());
                if (wokling.scoreOverride() != null) {
                    action.setScore(wokling.scoreOverride());
                }
                PolicyContributionLedger woklingLedger = new PolicyContributionLedger(
                    (decisionId == null || decisionId.isBlank()
                        ? "deploy-wokling" : decisionId + "-deploy-wokling") + "-" + actionId);
                woklingLedger.register(wokling.result());
                PolicyOperationAdapter.apply(action, woklingLedger);
                if (wokling.adapterStep()
                        == DeploySequencingPolicy.AdapterStep.CONTINUE_ACTION) {
                    actions.add(action);
                    continue;
                }
            }

            // ========== Skip ALL Deploy Actions ==========
            // Deploy actions should be handled EXCLUSIVELY by DeployEvaluator.
            if (actionText.equals("Deploy") ||
                (actionText.startsWith("Deploy ") && !textLower.contains("from"))) {
                // Skip this action - let DeployEvaluator handle it
                continue;
            }

            // ========== V24.4: LOCATIONS FIRST — DEPLOY LOCATIONS BEFORE ANYTHING ELSE ==========
            // Locations MUST be deployed before activating effects (AMSD, K&D, etc.).
            // If the bot has ANY location in hand, penalize all non-deploy actions heavily
            // so that deploy actions (handled by DeployEvaluator) always win priority.
            if (gameState != null && context.getPhase() == Phase.DEPLOY) {
                java.util.List<com.gempukku.swccgo.game.PhysicalCard> hand = context.getHand();
                if (hand != null) {
                    boolean hasLocationInHand = false;
                    for (com.gempukku.swccgo.game.PhysicalCard handCard : hand) {
                        if (handCard != null && handCard.getBlueprint() != null &&
                            handCard.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.LOCATION) {
                            hasLocationInHand = true;
                            break;
                        }
                    }
                    if (hasLocationInHand) {
                        // Check if this action is a search that PULLS locations (TDIGWATT, I'm Sorry, etc.)
                        // Those are OK — they help GET locations. But effect activations like AMSD should wait.
                        // V24.9: Added "sorry" — I'm Sorry deploys interior CC sites from reserve!
                        boolean isLocationSearch = textLower.contains("bespin") || textLower.contains("location")
                            || textLower.contains("cloud city") || textLower.contains("site")
                            || textLower.contains("sorry");
                        // V24.15: Exempt AMSD from LOCATIONS FIRST penalty!
                        // AMSD deploys a Star Destroyer — it's effectively a deploy action, not an "effect".
                        // When Bespin is already on the table, AMSD should fire immediately to get Executor there.
                        boolean isAmsdAction = textLower.contains("alert my star destroyer") ||
                            textLower.contains("amsd") ||
                            (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")) ||
                            (textLower.contains("star destroyer") && textLower.contains("deploy both"));
                        // V60 RESERVE PULL EXEMPTION: NEVER penalize Reserve Deck pulls.
                        // Steve's rule (feedback_reserve_deck_pulls.md): "[Download]" and
                        // "from Reserve Deck" actions are free value — thin the deck, bring
                        // key cards into play. Fire them every turn. They complement location
                        // deploys, they don't replace them. FIXES Issue #D from peaceful-pike
                        // replay: Sai'torr Kal Fas never fired Obi-Wan's Lightsaber because
                        // V24.4 blocked `[Download] a matching weapon` at -800.
                        boolean isReservePull = textLower.contains("[download]")
                            || textLower.contains("from reserve deck")
                            || textLower.contains("take an effect into hand")
                            || textLower.contains("take a character into hand");
                        // V67ba (Steve, 2026-05-08): EXEMPT generic deploy-from-hand actions.
                        // Action text "Play a card" / "Deploy" / "Deploy a card" is the ENTRY
                        // POINT to the deploy-from-hand sub-decision (CARD_SELECTION among
                        // hand cards). Penalizing it -800 means Rando never picks it, so
                        // the location in hand never gets deployed — the very thing V24.4
                        // is trying to force. FIXES 115yinsdp3t7t2q1.xml.gz: turn 2 had
                        // only 'Play a card' + 'Take Imperial Decree' as options; V24.4
                        // penalized 'Play a card' to -840, Pass scored -168, Rando passed.
                        boolean isDeployEntry = textLower.equals("play a card")
                            || textLower.equals("deploy")
                            || textLower.equals("deploy a card")
                            || textLower.startsWith("deploy ")
                            || textLower.startsWith("play a card ");
                        boolean locationFirstExempt = isLocationSearch || isAmsdAction
                            || isReservePull || isDeployEntry;
                        DeploySequencingPolicy.Evaluation locationFirst =
                            DeploySequencingPolicy.locationsFirstNonDeploy(
                                actionId, true, locationFirstExempt);
                        PolicyContributionLedger locationFirstLedger = new PolicyContributionLedger(
                            (decisionId == null || decisionId.isBlank()
                                ? "deploy-location-first-action"
                                : decisionId + "-deploy-location-first-action") + "-" + actionId);
                        locationFirstLedger.register(locationFirst.result());
                        PolicyOperationAdapter.apply(action, locationFirstLedger);
                        if (!locationFirstExempt) {
                            logger.warn("V24.4 LOCATIONS FIRST: Penalizing '{}' — location in hand needs deploying first! (-800)", actionText);
                        } else if (isAmsdAction) {
                            logger.warn("V24.15 AMSD EXEMPT: Not penalizing AMSD with LOCATIONS FIRST — AMSD deploys a Star Destroyer!");
                        } else if (isReservePull) {
                            logger.warn("V60 RESERVE PULL EXEMPT: '{}' is a Reserve Deck pull — NEVER penalize, always fire!", actionText);
                        } else if (isDeployEntry) {
                            logger.warn("V67ba DEPLOY-ENTRY EXEMPT: '{}' is the deploy-from-hand entry point — NEVER penalize!", actionText);
                        }
                    }
                }
            }

            // ========== V23: EMPTY PILE GUARD ==========
            // Block interrupts/actions that search piles which are empty.
            // Sith Fury on turn 1 wastes 4 force searching an empty Lost Pile.
            if (gameState != null) {
                String pid = context.getPlayerId();

                // === V29.14: NO ESCAPE — "Take top card of Lost Pile into hand" ===
                // This is FREE card advantage (not a search), works with any pile size >= 1.
                // Must be checked BEFORE the V23 empty pile guard so it doesn't get penalized.
                if (textLower.contains("take top card") && textLower.contains("lost pile")) {
                    int lostSize = gameState.getLostPile(pid).size();
                    if (lostSize > 0) {
                        controlLedger.register(ControlActionPolicy.noEscapeRetrieval(actionId));
                        PolicyOperationAdapter.apply(action, controlLedger);
                        logger.warn("V29.14 NO ESCAPE: '{}' — Lost Pile has {} cards, taking top card!", actionText, lostSize);
                        actions.add(action);
                        continue;
                    }
                }

                // Lost Pile searches
                if (textLower.contains("lost pile") && (textLower.contains("take") ||
                    textLower.contains("search") || textLower.contains("retrieve"))) {
                    int lostSize = gameState.getLostPile(pid).size();
                    PullSpecificActionPolicy.Evaluation pileSearch =
                        PullSpecificActionPolicy.scorePileSearch(
                            new PullSpecificActionFacts.PileSearch(
                                actionId, PullSpecificActionFacts.PileKind.LOST,
                                lostSize));
                    applyPullSpecificActionPolicy(action, pileSearch.result());
                    if (pileSearch.adapterStep()
                            == PullSpecificActionPolicy.AdapterStep.CONTINUE_ACTION) {
                        logger.warn("V23 EMPTY PILE GUARD: Blocking '{}' — Lost Pile is empty!", actionText);
                        actions.add(action);
                        continue;
                    }
                    if (lostSize <= 2) {
                        logger.warn("V23 LOW PILE: '{}' — Lost Pile only has {} cards", actionText, lostSize);
                    }
                }
                // Used Pile searches
                if (textLower.contains("used pile") && (textLower.contains("take") ||
                    textLower.contains("search"))) {
                    int usedSize = gameState.getUsedPile(pid).size();
                    PullSpecificActionPolicy.Evaluation pileSearch =
                        PullSpecificActionPolicy.scorePileSearch(
                            new PullSpecificActionFacts.PileSearch(
                                actionId, PullSpecificActionFacts.PileKind.USED,
                                usedSize));
                    applyPullSpecificActionPolicy(action, pileSearch.result());
                    if (pileSearch.adapterStep()
                            == PullSpecificActionPolicy.AdapterStep.CONTINUE_ACTION) {
                        logger.warn("V23 EMPTY PILE GUARD: Blocking '{}' — Used Pile is empty!", actionText);
                        actions.add(action);
                        continue;
                    }
                }
            }

            // ========== V256: DEPLOY ACTION-TEXT — AMSD ==========
            // The adapter owns table, DeckOracle, Force, logging, and mutation reads.
            // DeployActionTextPolicy owns the ordered scores and early-exit outcome.
            if (gameState != null && (textLower.contains("alert my star destroyer") ||
                textLower.contains("amsd") ||
                (textLower.contains("star destroyer") && textLower.contains("deploy both")) ||
                (textLower.contains("star destroyer") && textLower.contains("pilot") && textLower.contains("deploy")) ||
                (textLower.contains("reveal") && textLower.contains("pilot") && textLower.contains("star destroyer")))) {
                boolean bespinSystemOnTable = false;
                try {
                    for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                        if (loc != null && loc.getTitle() != null &&
                            loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                            loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                            loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                            bespinSystemOnTable = true;
                            break;
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V24 AMSD gate: Error checking Bespin: {}", e.getMessage());
                }
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle amsdOracle = context.getDeckOracle();
                int currentTurn = context.getTurnNumber();
                boolean alreadyFailedThisTurn = bespinSystemOnTable && amsdOracle != null
                    && amsdOracle.hasAmsdFailedThisTurn(currentTurn);
                boolean isGenericReveal = textLower.contains("reveal") && !textLower.contains("piett")
                    && !textLower.contains("vader") && !textLower.contains("chiraneau")
                    && !textLower.contains("ozzel") && !textLower.contains("motti");
                DeployActionTextFacts.AmsdActionKind amsdActionKind = isGenericReveal
                    ? DeployActionTextFacts.AmsdActionKind.GENERIC_REVEAL
                    : textLower.contains("piett")
                        ? DeployActionTextFacts.AmsdActionKind.PIETT_SPECIFIC
                        : DeployActionTextFacts.AmsdActionKind.OTHER_SPECIFIC;
                boolean oracleAnalyzed = bespinSystemOnTable && !alreadyFailedThisTurn
                    && amsdActionKind != DeployActionTextFacts.AmsdActionKind.OTHER_SPECIFIC
                    && amsdOracle != null && amsdOracle.isAnalyzed();
                boolean piettInHand = false;
                boolean executorInReserve = false;
                boolean executorInHand = false;
                if (oracleAnalyzed) {
                    piettInHand = amsdOracle.isCardInHand("Admiral Piett")
                        || amsdOracle.isCardInHand("Piett");
                    executorInReserve = amsdOracle.isCardInReserve("Executor")
                        || amsdOracle.isCardInReserve("Flagship Executor");
                    executorInHand = amsdOracle.isCardInHand("Executor")
                        || amsdOracle.isCardInHand("Flagship Executor");
                    logger.warn("V29.4 AMSD DIAGNOSTIC: piettInHand={}, executorInReserve={}, executorInHand={}, executorAvailable={}",
                        piettInHand, executorInReserve, executorInHand,
                        executorInReserve || executorInHand);
                }
                int amsdForceAvailable = oracleAnalyzed && piettInHand
                    && (executorInReserve || executorInHand)
                        ? context.getForcePileSize() : 0;

                DeployActionTextPolicy.Evaluation amsdEvaluation =
                    DeployActionTextPolicy.evaluateAmsd(
                        new DeployActionTextFacts.AmsdFacts(
                            actionId, bespinSystemOnTable, alreadyFailedThisTurn,
                            amsdActionKind, oracleAnalyzed, piettInHand,
                            executorInHand, executorInReserve, currentTurn,
                            amsdForceAvailable));
                applyDeployActionTextPolicy(action, amsdEvaluation.result());
                if (amsdEvaluation.recordFailedTurn() && amsdOracle != null) {
                    amsdOracle.recordAmsdFailedOnTurn(currentTurn);
                }
                if (amsdEvaluation.adapterStep()
                        == DeployActionTextPolicy.AdapterStep.CONTINUE_ACTION) {
                    actions.add(action);
                    continue;
                }
            }

            // ========== V24: TDIGWATT EXHAUSTED SEARCH GUARD ==========
            // TDIGWATT searches for "Cloud City Occupation, Dark Deal, Vader's Bounty, or Bespin".
            // Once all targets have been pulled, every search fails — stop wasting the action.
            if (textLower.contains("cloud city occupation") && textLower.contains("dark deal") &&
                textLower.contains("bespin")) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle tdigOracle = context.getDeckOracle();
                if (tdigOracle != null && tdigOracle.isAnalyzed()) {
                    boolean anyTargetInReserve =
                        tdigOracle.isCardInReserve("Bespin") ||
                        tdigOracle.isCardInReserve("Dark Deal") ||
                        tdigOracle.isCardInReserve("Cloud City Occupation") ||
                        tdigOracle.isCardInReserve("Vader's Bounty");
                    PullSpecificActionPolicy.Evaluation tdigwatt =
                        PullSpecificActionPolicy.scoreTdigwatt(
                            new PullSpecificActionFacts.ExhaustedSearch(
                                actionId, anyTargetInReserve));
                    applyPullSpecificActionPolicy(action, tdigwatt.result());
                    if (tdigwatt.adapterStep()
                            == PullSpecificActionPolicy.AdapterStep.CONTINUE_ACTION) {
                        logger.warn("V24 TDIGWATT EXHAUSTED: All 4 targets (Bespin, Dark Deal, CC Occupation, Vader's Bounty) already pulled — blocking search!");
                        actions.add(action);
                        continue;
                    }
                    logger.info("V24 TDIGWATT: Targets still in reserve — search OK");
                }
            }

            // ========== V24.6B: I'M SORRY LOCATION PULL — USE UNTIL CC SITES EXHAUSTED ==========
            // I'm Sorry (V) deploys interior Cloud City sites from reserve deck.
            // Use EVERY turn until all CC interior sites are pulled from reserve.
            // DeckOracle tracks what's left — stop wasting the action when reserve is empty.
            if (textLower.contains("sorry") || textLower.contains("i'm sorry") ||
                (textLower.contains("interior") && textLower.contains("cloud city") && textLower.contains("site"))) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer sorryObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (sorryObjAnalyzer != null && sorryObjAnalyzer.isAnalyzed()
                    && sorryObjAnalyzer.needsBespinSystemPresence()) {
                    // Use DeckOracle to check if any CC interior sites remain in reserve
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle sorryOracle = context.getDeckOracle();
                    boolean ccSitesInReserve = true; // default to true if oracle unavailable
                    if (sorryOracle != null && sorryOracle.isAnalyzed()) {
                        ccSitesInReserve = sorryOracle.isCardInReserve("Cloud City: Upper Walkway")
                            || sorryOracle.isCardInReserve("Cloud City: Carbonite Chamber")
                            || sorryOracle.isCardInReserve("Cloud City: Dining Room")
                            || sorryOracle.isCardInReserve("Cloud City: Lower Corridor")
                            || sorryOracle.isCardInReserve("Cloud City: Security Tower")
                            || sorryOracle.isCardInReserve("Cloud City: West Gallery")
                            || sorryOracle.isCardInReserve("Cloud City: North Corridor")
                            || sorryOracle.isCardInReserve("Cloud City: Platform")
                            || sorryOracle.isCardInReserve("Cloud City: Incinerator")
                            || sorryOracle.isCardInReserve("Cloud City: Guest Quarters")
                            || sorryOracle.isCardInReserve("Cloud City")  // partial match catches any CC site
                            ;
                        logger.warn("V24.6 I'M SORRY: CC interior sites still in reserve? {}", ccSitesInReserve);
                    }
                    applyPullSpecificActionPolicy(action,
                        PullSpecificActionPolicy.scoreSorryLocation(
                            new PullSpecificActionFacts.SorryLocation(
                                actionId, ccSitesInReserve)));
                    if (ccSitesInReserve) {
                        logger.warn("V24.6 I'M SORRY: Boosting +250 — CC interior sites available in reserve!");
                    } else {
                        logger.warn("V24.6 I'M SORRY: BLOCKING — no more CC interior sites in reserve deck! (-300)");
                    }
                }
            }

            // ========== V29.7 WMAOP: STIPULATIONS REMOVED 2026-05-26 (Steve) ==========
            // Original V29.7 hardcoded "WMAOP is for Blockade Flagship site ONLY,"
            // penalizing all other modes -400 and adding -400 for "BFS already on
            // table" + -500 for "no locations in reserve." That worked for one
            // specific Blockade-Flagship-themed deck but blocked WMAOP universally
            // in any other deck that had it (Podracer interrupt mode, Effect pull
            // mode, or location pull mode targeting non-BFS sites).
            //
            // Steve's directive: "This logic must work for all decks regardless of
            // objective. Remove the stipulations so it fires for all decks."
            //
            // Current behavior: V29.7 no longer adds any scoring for WMAOP. Generic
            // rules carry the load:
            //   - V100 LOCATION PULL BEFORE CHARACTERS: +1500 when WMAOP's location-
            //     pull mode fires during deploy phase with chars/vehicles in hand.
            //   - V67ai TIERED LOCATION DEPLOY ORDER: scales by source category.
            //   - V67ak KEY-CHARACTER PULL: scales by named-persona match.
            //   - V60 RESERVE PULL: +150 generic for any reserve pull.
            //
            // If a deck-specific WMAOP rule is needed later (e.g., Blockade-themed
            // decks should prefer location mode), reintroduce here with DeckOracle
            // gating instead of hardcoding card titles.

            // ========== V29 / V67u: FORCE PUSH — BATTLE USE ONLY ==========
            // Force Push has two modes:
            //   1. BATTLE: "use 2 Force to target your Dark Jedi and opponent's character...
            //      Both targets are excluded from battle" — GOOD, removes threat
            //   2. FORCE PILE EXCHANGE: "Exchange two cards from hand with any one card
            //      from Force Pile" — BAD, especially in DRAW PHASE: you'd draw those
            //      cards anyway, and you're trading 2 hand cards for 1.
            //
            // V67u FIX (Steve, 2026-05-03): The OLD V29 check was `textLower.contains("force push")`
            // — but the action text for the exchange is just "Exchange cards with card in
            // Force Pile" which does NOT contain "force push". So V29 never fired and Rando
            // happily played the exchange during draw phase, wasting Force.
            //
            // New V67u: detect by SOURCE CARD title (when cardId resolvable) OR by action
            // text mentioning "force pile" + "exchange" (which uniquely identifies this
            // wasteful action regardless of source).
            String v67uSourceTitle = null;
            if (cardId != null && gameState != null) {
                try {
                    PhysicalCard srcPc = gameState.findCardById(Integer.parseInt(cardId));
                    if (srcPc != null && srcPc.getTitle() != null) {
                        v67uSourceTitle = srcPc.getTitle().toLowerCase(java.util.Locale.ROOT);
                    }
                } catch (Exception e) { /* ignore */ }
            }
            boolean v67uIsForcePushSource = v67uSourceTitle != null
                && v67uSourceTitle.contains("force push");
            boolean v67uIsExchangeAction = textLower.contains("exchange")
                && (textLower.contains("force pile") || textLower.contains("hand"));
            boolean v67uIsBattleAction = textLower.contains("exclude") && textLower.contains("battle");

            BattleWeaponsFacts.ForcePushMode forcePushMode = BattleWeaponsFacts.ForcePushMode.NONE;
            if (textLower.contains("force push") || v67uIsForcePushSource) {
                if (v67uIsBattleAction && !v67uIsExchangeAction) {
                    forcePushMode = BattleWeaponsFacts.ForcePushMode.BATTLE_EXCLUSION;
                    logger.info("V29 FORCE PUSH: Battle use — exclude characters from battle (+80)");
                } else if (v67uIsExchangeAction) {
                    forcePushMode = BattleWeaponsFacts.ForcePushMode.FORCE_PILE_EXCHANGE;
                    logger.warn("V67u FORCE PUSH BLOCKED: '{}' source='{}' — exchange is waste, especially in draw phase (-500)",
                        actionText, v67uSourceTitle);
                }
            }
            if (forcePushMode != BattleWeaponsFacts.ForcePushMode.NONE) {
                battleForcePushLedger.register(BattleWeaponsPolicy.scoreActionText(
                    new BattleWeaponsFacts.ActionTextFacts(
                        actionId, forcePushMode,
                        BattleWeaponsFacts.FireMode.NONE,
                        BattleWeaponsFacts.ThrowMode.NONE,
                        BattleWeaponsFacts.RedrawFacts.none())));
                PolicyOperationAdapter.apply(action, battleForcePushLedger);
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: DEPLOY-3 — weapon-pull criteria gate (reorg 2026-07-06) ═══
            // Owns: V120 universal weapon-pull criteria block (-9999 when no in-play character satisfies the weapon's
            // OWN matching filter; V125 contains() fix + 2026-06-29 strict-match fix folded in). Deliberately SEPARATE
            // from the V185 oracle-side attach gate in DeckOracle — keep both.
            // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
            // Cross-refs: DEPLOY-3 (V158/V115/V67aq in DeployEvaluator), SVC-ORACLE (V185). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== V120 (Steve, 2026-05-22): UNIVERSAL WEAPON-PULL CRITERIA BLOCK ==========
            // Per Steve: "We need to hard block deploy from reserve deck or with an
            // interrupt when a character already has a weapon."
            //
            // V115 closed the hand-deploy gap via V67aq criteria-awareness in DeployEvaluator.
            // V120 closes the FIFTH gap that the four-way one-weapon stack still missed:
            // Effect/Interrupt/Objective top-level actions that deploy a weapon FROM RESERVE
            // (e.g. "Deploy Vader's Lightsaber from Reserve Deck using •I Am Your Father (V)").
            // These score in ActionTextEvaluator and never reach V67aq, V70, V67ar, or V115.
            //
            // Logic: parse the weapon's title from the action text, find its blueprint anywhere
            // in Rando's known cards (hand/reserve/used/lost/table — gameState.getAllPermanentCards
            // covers all of these), extract its deploy criteria via the V70 helper, and count
            // criteria-matching armed/unarmed friendlies. Block when matchingUnarmed == 0.
            // Hunt Down replay ig4n5m5nzc4gronn: Rando fired IAYF four times trying to pull
            // Vader's Lightsaber from reserve while Vader was already armed with two Dark Jedi
            // Lightsabers (V115 was added the same session). Each attempt revealed the reserve.
            if (textLower.contains("from reserve") && actionText != null) {
                try {
                    // Parse weapon title from "Deploy <NAME> from Reserve" pattern
                    java.util.regex.Matcher v120m = java.util.regex.Pattern.compile(
                        "(?i)deploy\\s+([\\w'\\.\\(\\) -]+?)\\s+from\\s+reserve"
                    ).matcher(actionText);
                    if (v120m.find()) {
                        String v120WeaponName = v120m.group(1).trim();
                        // Strip leading bullet/dot markers
                        v120WeaponName = v120WeaponName.replaceAll("^[•·∙\\.]+\\s*", "").trim();
                        // V125 (Steve, 2026-05-22): V120 EXACT-MATCH BUG FIX — use contains() not equals().
                        // V120's original equals() comparison silently failed when the action text
                        // says "Vader's Lightsaber" but the actual card title is "•Darth Vader's
                        // Lightsaber (V)" (uniqueness bullet + Darth prefix + (V) suffix). Replay
                        // liuorncol0ku2qva 2026-05-22 confirmed: V120 never logged for IAYF's
                        // weapon-pull attempt. Switch to bidirectional contains() match — title
                        // contains action-text-name OR action-text-name contains title (handles
                        // both abbreviated and prefixed titles).
                        SwccgCardBlueprint v120WeaponBp = null;
                        if (gameState != null) {
                            String v120WeaponLower = v120WeaponName.toLowerCase(java.util.Locale.ROOT);
                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                if (pc == null || pc.getBlueprint() == null) continue;
                                if (!context.getPlayerId().equals(pc.getOwner())) continue;
                                if (pc.getBlueprint().getCardCategory() != CardCategory.WEAPON) continue;
                                String pTitle = pc.getTitle();
                                if (pTitle == null) continue;
                                String pTitleLower = pTitle.toLowerCase(java.util.Locale.ROOT);
                                // V120 FIX (#1, Steve 2026-06-29): the loose "title contains parsed-name"
                                // match caught a CHARACTER pull whose name sits inside a weapon title —
                                // "Deploy Vader from Reserve Deck" parses "vader", and "darth vader's
                                // lightsaber".contains("vader") = true, so the Vader CHARACTER pull was
                                // mis-blocked as a weapon pull (-9999), losing Steve the Hunt Down game.
                                // A real weapon pull names the WEAPON (its noun, e.g. "...lightsaber");
                                // a character pull names only the owner ("vader"). So for the loose
                                // direction require the parsed name to cover the weapon title's last
                                // significant word (the noun), not just the owner portion. The exact /
                                // parsed-name-contains-full-title directions (the V125 abbreviated/
                                // prefixed-title cases) are unchanged.
                                boolean v120Match;
                                if (pTitleLower.equals(v120WeaponLower)
                                        || v120WeaponLower.contains(pTitleLower)) {
                                    v120Match = true;
                                } else if (pTitleLower.contains(v120WeaponLower)) {
                                    String v120TitleCore = pTitleLower
                                        .replaceAll("\\([^)]*\\)", " ").replaceAll("\\s+", " ").trim();
                                    String v120Noun = v120TitleCore.contains(" ")
                                        ? v120TitleCore.substring(v120TitleCore.lastIndexOf(' ') + 1)
                                        : v120TitleCore;
                                    v120Match = v120Noun.length() >= 4
                                        && v120WeaponLower.contains(v120Noun);
                                } else {
                                    v120Match = false;
                                }
                                if (v120Match) {
                                    v120WeaponBp = pc.getBlueprint();
                                    break;
                                }
                            }
                        }
                        if (v120WeaponBp != null) {
                            String v120Criteria = com.gempukku.swccgo.ai.models.chosenone.evaluators
                                .CardSelectionEvaluator.v70ExtractDeployCriteria(v120WeaponBp.getGameText());
                            if (v120Criteria != null) {
                                int v120MatchArmed = 0, v120MatchUnarmed = 0;
                                for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                    if (tc == null || tc.getBlueprint() == null) continue;
                                    if (!context.getPlayerId().equals(tc.getOwner())) continue;
                                    if (tc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                    com.gempukku.swccgo.common.Zone tz = tc.getZone();
                                    if (tz == null || !tz.isInPlay()) continue;
                                    boolean v120Match = false;
                                    try {
                                        v120Match = com.gempukku.swccgo.ai.models.chosenone.evaluators
                                            .CardSelectionEvaluator.v70CharacterMatchesCriteria(
                                                game, gameState, tc, v120Criteria);
                                    } catch (Exception ignore) { /* false */ }
                                    if (!v120Match) continue;
                                    boolean v120Armed = false;
                                    java.util.List<PhysicalCard> v120Atts = gameState.getAttachedCards(tc);
                                    if (v120Atts != null) {
                                        for (PhysicalCard a : v120Atts) {
                                            if (a != null && a.getBlueprint() != null
                                                    && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                v120Armed = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (v120Armed) v120MatchArmed++; else v120MatchUnarmed++;
                                }
                                PolicyContributionLedger v213WeaponPullLedger =
                                    new PolicyContributionLedger("deploy-weapon-pull-" + actionId);
                                v213WeaponPullLedger.register(DeployWeaponPolicy.evaluatePullCriteria(
                                    new DeployWeaponPolicy.PullCriteriaFacts(
                                        actionId, v120WeaponName, v120Criteria,
                                        v120MatchArmed, v120MatchUnarmed)));
                                PolicyOperationAdapter.apply(action, v213WeaponPullLedger);
                                if (v120MatchUnarmed == 0) {
                                    logger.warn("V120 WEAPON-PULL BLOCK: '{}' (weapon '{}', criteria '{}') matchArmed={} matchUnarmed={} → HARD BLOCK (-9999)",
                                        actionText, v120WeaponName, v120Criteria, v120MatchArmed, v120MatchUnarmed);
                                }
                            }
                        }
                    }
                } catch (Exception e120) {
                    logger.debug("V120 weapon-pull check error: {}", e120.getMessage());
                }
            }

            // ========== V29.8: IAYF VADER-ON-TABLE CHECK (ANY SOURCE) ==========
            // IAYF can deploy Vader's Lightsaber from RESERVE or LOST PILE.
            // The reserve-only check below misses the Lost Pile case.
            // This broader check catches both: if source is IAYF and action involves
            // lightsaber, Vader MUST be on table.
            if (textLower.contains("lightsaber") && cardId != null && gameState != null) {
                try {
                    PhysicalCard iaySourceCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (iaySourceCard != null && iaySourceCard.getTitle() != null
                        && iaySourceCard.getTitle().toLowerCase(java.util.Locale.ROOT).contains("i am your father")) {
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer iayObj = context.getObjectiveAnalyzer();
                        boolean vaderPresent = iayObj != null && iayObj.isVaderOnTable(gameState, context.getPlayerId());
                        applyPullSpecificActionPolicy(action,
                            PullSpecificActionPolicy.scoreIayfPresence(
                                new PullSpecificActionFacts.IayfPresence(
                                    actionId, true, vaderPresent)));
                        if (!vaderPresent) {
                            logger.warn("V29.8 IAYF BLOCKED: Vader not on table — lightsaber deploy from {} impossible!",
                                textLower.contains("lost") ? "Lost Pile" : "Reserve/other");
                        }
                    }
                } catch (Exception iayE) {
                    logger.debug("V29.8: Error checking IAYF vader: {}", iayE.getMessage());
                }
            }

            // ========== V29.8: SENSE & UNCERTAIN — BLOCK REDRAW HAND USAGE ==========
            // Sense & Uncertain Is The Future has two functions:
            //   1. As Sense: cancel an opponent's interrupt (GOOD — save for this!)
            //   2. As Uncertain: make each player redraw hand (TERRIBLE — helps opponent too,
            //      costs 3 Force, loses cards currently in hand, is a Lost Interrupt)
            // Rando must NEVER use the redraw hand function. Save Sense for defense.
            if (textLower.contains("redraw") && textLower.contains("hand")) {
                applyResponsePolicy(action,
                    ResponsePolicy.scoreSenseRedraw(actionId, true, false));
                logger.warn("V29.8 SENSE REDRAW BLOCKED: Attempted to redraw hand — massive penalty (-600)");
            }
            // Also catch the "make each player" variant
            if (textLower.contains("each player") && (textLower.contains("redraw") || textLower.contains("shuffle"))) {
                applyResponsePolicy(action,
                    ResponsePolicy.scoreSenseRedraw(actionId, false, true));
                logger.warn("V29.8 SENSE UNCERTAIN BLOCKED: Attempted mutual redraw — massive penalty (-600)");
            }

            // ========== V29.7: UNIVERSAL RESERVE DECK PULL VALIDATION ==========
            // PROBLEM: Many cards produce GENERIC action texts like "Deploy card from Reserve Deck"
            // or "Take card into hand from Reserve Deck". The V25 checks looked for card names
            // like "crush the rebellion" in action text — but those names were NEVER in the text!
            // FIX: Look up the SOURCE CARD via cardId to identify what's generating the action,
            // then check DeckOracle for valid targets based on the source card's identity.
            if (textLower.contains("from reserve") && cardId != null && gameState != null) {
                String sourceTitle = null;
                PhysicalCard reserveSourceCard = null;
                try {
                    reserveSourceCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (reserveSourceCard != null
                            && reserveSourceCard.getTitle() != null) {
                        sourceTitle = reserveSourceCard.getTitle();
                    }
                } catch (Exception e) { /* ignore parse errors */ }

                if (sourceTitle != null) {
                    String sourceLower = sourceTitle.toLowerCase(java.util.Locale.ROOT);
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle pullOracle = context.getDeckOracle();

                    // --- CRUSH THE REBELLION: pulls I Have You Now or Evader ---
                    if (sourceLower.contains("crush") && sourceLower.contains("rebellion")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.isCardInReserve("I Have You Now")
                                || pullOracle.isCardInReserve("Evader");
                            // V29.9: Check if IHYN/Evader already in hand — don't pull duplicates!
                            boolean ihynInHand = pullOracle.isCardInHand("I Have You Now");
                            boolean evaderInHand = pullOracle.isCardInHand("Evader");
                            boolean ihynInReserve = pullOracle.isCardInReserve("I Have You Now");
                            boolean evaderInReserve = pullOracle.isCardInReserve("Evader");
                            PullSpecificActionFacts.DuplicateState duplicateState =
                                ihynInHand && evaderInHand
                                    ? PullSpecificActionFacts.DuplicateState.BOTH_IN_HAND
                                    : ihynInHand && !evaderInReserve
                                        ? PullSpecificActionFacts.DuplicateState.FIRST_IN_HAND_SECOND_MISSING
                                        : evaderInHand && !ihynInReserve
                                            ? PullSpecificActionFacts.DuplicateState.SECOND_IN_HAND_FIRST_MISSING
                                            : PullSpecificActionFacts.DuplicateState.NONE;
                            applyPullSpecificActionPolicy(action,
                                PullSpecificActionPolicy.scoreNamedReserveSource(
                                    new PullSpecificActionFacts.NamedReserveSource(
                                        actionId,
                                        PullSpecificActionFacts.ReserveSourceKind.CRUSH_THE_REBELLION,
                                        hasTarget, duplicateState)));
                            if (!hasTarget) {
                                logger.warn("V29.7 CRUSH BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                            if (ihynInHand && evaderInHand) {
                                // Both targets already in hand — this pull is useless
                                logger.warn("V29.9 CRUSH DUPLICATE: Both targets in hand — blocking (-300)");
                            } else if (ihynInHand && !evaderInReserve) {
                                // IHYN in hand and no Evader in reserve — would pull a second IHYN
                                logger.warn("V29.9 CRUSH DUPLICATE: IHYN in hand, no Evader in reserve — blocking (-250)");
                            } else if (evaderInHand && !ihynInReserve) {
                                // Evader in hand and no IHYN in reserve — would pull a second Evader
                                logger.warn("V29.9 CRUSH DUPLICATE: Evader in hand, no IHYN in reserve — blocking (-250)");
                            }
                        }
                    }

                    // --- I AM YOUR FATHER: deploys Vader's Lightsaber ---
                    // V35.8: IAYF can pull from Reserve Deck (free) OR Lost Pile (lose 1 Force).
                    // Both should score EXTREMELY high when Vader is on table unarmed.
                    // The Lost Pile retrieval is a KEY mechanic of Hunt Down — Vader throws
                    // his lightsaber every battle, then retrieves it for the next battle.
                    else if (sourceLower.contains("i am your father")) {
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer objA = context.getObjectiveAnalyzer();
                        boolean vaderOnTable = objA != null && objA.isVaderOnTable(gameState, context.getPlayerId());

                        if (!vaderOnTable && textLower.contains("lightsaber")) {
                            applyPullSpecificActionPolicy(action,
                                PullSpecificActionPolicy.scoreIayfReserve(
                                    new PullSpecificActionFacts.IayfReserve(
                                        actionId, false, false, false,
                                        false, false, false)));
                            logger.warn("V29.7 IAYF BLOCKED: Vader not on table");
                        } else if (vaderOnTable && textLower.contains("lightsaber")) {
                            // V37: USE DECKORACLE to check WHERE the lightsaber actually is!
                            // IAYF can pull from Reserve Deck (free) or Lost Pile (lose 1 Force).
                            // The action text tells us which zone — don't try Reserve if it's in Lost.
                            boolean pullFromReserve = textLower.contains("reserve");
                            boolean pullFromLost = textLower.contains("lost");

                            boolean saberInReserve = false;
                            boolean saberInLost = false;
                            try {
                                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle iayOracle = context.getDeckOracle();
                                if (iayOracle != null && iayOracle.isAnalyzed()) {
                                    saberInReserve = iayOracle.isCardInReserve("Darth Vader's Lightsaber");
                                    saberInLost = iayOracle.isCardLost("Darth Vader's Lightsaber");
                                    logger.info("V37 IAYF ZONE CHECK: saber in reserve={}, in lost={}, action={}",
                                        saberInReserve, saberInLost, pullFromReserve ? "RESERVE" : pullFromLost ? "LOST" : "UNKNOWN");
                                }
                            } catch (Exception e) { /* ignore */ }

                            // V37: Block if trying to pull from wrong zone
                            if (pullFromReserve && !saberInReserve) {
                                applyPullSpecificActionPolicy(action,
                                    PullSpecificActionPolicy.scoreIayfReserve(
                                        new PullSpecificActionFacts.IayfReserve(
                                            actionId, true, pullFromReserve,
                                            pullFromLost, saberInReserve,
                                            saberInLost, false)));
                                logger.warn("V37 IAYF BLOCKED: Trying reserve but saber not there! (in lost={})", saberInLost);
                            } else if (pullFromLost && !saberInLost) {
                                applyPullSpecificActionPolicy(action,
                                    PullSpecificActionPolicy.scoreIayfReserve(
                                        new PullSpecificActionFacts.IayfReserve(
                                            actionId, true, pullFromReserve,
                                            pullFromLost, saberInReserve,
                                            saberInLost, false)));
                                logger.warn("V37 IAYF BLOCKED: Trying lost pile but saber not there! (in reserve={})", saberInReserve);
                            } else {
                                // Lightsaber IS in the target zone — check if Vader is armed
                                boolean vaderArmed = false;
                                try {
                                    String iayPid = context.getPlayerId();
                                    for (PhysicalCard tc : gameState.getAllPermanentCards()) {
                                        if (tc == null || !iayPid.equals(tc.getOwner())) continue;
                                        if (tc.getBlueprint() == null) continue;
                                        String tcTitle = tc.getTitle() != null ? tc.getTitle().toLowerCase(Locale.ROOT) : "";
                                        if (!tcTitle.contains("vader")) continue;
                                        if (tc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        com.gempukku.swccgo.common.Zone tcZ = tc.getZone();
                                        if (tcZ == null || !tcZ.isInPlay()) continue;
                                        java.util.List<PhysicalCard> atts = gameState.getAttachedCards(tc);
                                        if (atts != null) {
                                            for (PhysicalCard att : atts) {
                                                if (att != null && att.getBlueprint() != null
                                                    && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                    vaderArmed = true;
                                                    break;
                                                }
                                            }
                                        }
                                        break;
                                    }
                                } catch (Exception e) { /* ignore */ }

                                applyPullSpecificActionPolicy(action,
                                    PullSpecificActionPolicy.scoreIayfReserve(
                                        new PullSpecificActionFacts.IayfReserve(
                                            actionId, true, pullFromReserve,
                                            pullFromLost, saberInReserve,
                                            saberInLost, vaderArmed)));
                                if (!vaderArmed) {
                                    logger.warn("V37 IAYF: Vader unarmed, saber in {} — TOP PRIORITY (+600)",
                                        pullFromLost ? "Lost" : "Reserve");
                                }
                            }
                        }
                    }

                    // --- YOU ARE BEATEN: pulls IAYF or specific card from reserve ---
                    else if (sourceLower.contains("you are beaten")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasIAYF = pullOracle.isCardInReserve("I Am Your Father");
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.YOU_ARE_BEATEN,
                                hasIAYF);
                            if (!hasIAYF) {
                                logger.warn("V29.7 YOU ARE BEATEN BLOCKED: No IAYF in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- BLAST POINTS: pulls Ghhhk or Hyperwave Scan ---
                    else if (sourceLower.contains("blast points")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.isCardInReserve("Ghhhk")
                                || pullOracle.isCardInReserve("Hyperwave Scan");
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.BLAST_POINTS,
                                hasTarget);
                            if (!hasTarget) {
                                logger.warn("V29.7 BLAST POINTS BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- HUNT DOWN (objective): deploys location from reserve ---
                    else if (sourceLower.contains("hunt down") && textLower.contains("location")) {
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                                huntObjective = context.getObjectiveAnalyzer();
                        boolean exactVirtualObjectiveAction =
                                reserveSourceCard != null
                                && "213_31".equals(
                                    reserveSourceCard.getBlueprintId(true))
                                && "deploy a location from reserve deck".equals(
                                    textLower.trim())
                                && huntObjective != null
                                && huntObjective.isAnalyzed()
                                && huntObjective.isVirtualHuntDownObjective();
                        if (exactVirtualObjectiveAction) {
                            boolean hasTarget = huntObjective
                                .hasVirtualHuntDownLocationDownloadInReserve(
                                    game, context.getPlayerId());
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.HUNT_DOWN,
                                hasTarget);
                            applyPullSpecificActionPolicy(action,
                                PullSpecificActionPolicy
                                    .scoreHuntDownLocationDownload(
                                        new PullSpecificActionFacts
                                            .HuntDownLocationDownload(
                                                actionId, hasTarget)));
                            if (!hasTarget) {
                                logger.warn("HUNT DOWN OBJECTIVE BLOCKED: No eligible Cloud City or Malachor battleground site in reserve");
                            }
                        } else if (pullOracle != null
                                && pullOracle.isAnalyzed()) {
                            java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> locsInReserve =
                                pullOracle.getCardsByCategory(com.gempukku.swccgo.common.CardCategory.LOCATION,
                                    com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.HUNT_DOWN,
                                !locsInReserve.isEmpty());
                            if (locsInReserve.isEmpty()) {
                                logger.warn("V29.7 HUNT DOWN BLOCKED: No locations in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- IMPERIAL COMMAND: pulls admiral or general ---
                    else if (sourceLower.contains("imperial command")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("admiral", "general");
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.IMPERIAL_COMMAND,
                                hasTarget);
                            if (!hasTarget) {
                                logger.warn("V29.7 IMPERIAL COMMAND BLOCKED: No targets in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- ENDOR SHIELD: pulls admiral ---
                    else if (sourceLower.contains("endor shield")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("admiral");
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.ENDOR_SHIELD,
                                hasTarget);
                            if (!hasTarget) {
                                logger.warn("V29.7 ENDOR SHIELD BLOCKED: No admirals in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- VISAGE OF THE EMPEROR: pulls lightsaber ---
                    else if (sourceLower.contains("visage") && textLower.contains("lightsaber")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("lightsaber");
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.VISAGE,
                                hasTarget);
                            if (!hasTarget) {
                                logger.warn("V29.7 VISAGE BLOCKED: No lightsabers in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // --- KIR KANOS: pulls Royal Guard ---
                    else if (sourceLower.contains("kir kanos")) {
                        if (pullOracle != null && pullOracle.isAnalyzed()) {
                            boolean hasTarget = pullOracle.hasTargetInReserve("royal guard", "kanos", "kyneugh");
                            applyNamedReserveSourcePolicy(action,
                                PullSpecificActionFacts.ReserveSourceKind.KIR_KANOS,
                                hasTarget);
                            if (!hasTarget) {
                                logger.warn("V29.7 KIR KANOS BLOCKED: No Royal Guards in reserve (source: {})", sourceTitle);
                            }
                        }
                    }

                    // === V37: UNIVERSAL RESERVE SEARCH SAFETY NET ===
                    // Any "from reserve" action that wasn't caught by a specific rule above
                    // should still be cautious. Failed searches give opponent free deck intel.
                    // If DeckOracle shows reserve deck is very small, penalize searches
                    // because they reveal more information proportionally.
                    if (pullOracle != null && pullOracle.isAnalyzed()) {
                        java.util.List<com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle.DeckCard> reserveCards =
                            pullOracle.getCardsInZone(com.gempukku.swccgo.common.Zone.RESERVE_DECK);
                        applyPullSpecificActionPolicy(action,
                            PullSpecificActionPolicy.scoreReserveRisk(
                                new PullSpecificActionFacts.ReserveRisk(
                                    actionId, reserveCards.size())));
                        if (reserveCards.size() <= 3) {
                            logger.warn("V37 RESERVE RISK: {} cards in reserve — search gives opponent too much intel (-200)",
                                reserveCards.size());
                        }
                    }
                }
            }

            // ========== V24.9: MASTERFUL MOVE EARLY-GAME GUARD ==========
            // Masterful Move searches reserve for Ghhhk (damage cancel combo card).
            // On turns 1-3, force should go to deploying Executor + characters, NOT searching for Ghhhk.
            // Only play Masterful Move when characters are on the table and need protecting.
            if (textLower.contains("masterful move")) {
                int mmTurn = context.getTurnNumber();
                boolean hasCharsOnTable = false;
                if (gameState != null) {
                    try {
                        for (PhysicalCard loc : gameState.getLocationsInOrder()) {
                            java.util.List<PhysicalCard> cardsHere = gameState.getCardsAtLocation(loc);
                            if (cardsHere != null) {
                                for (PhysicalCard c : cardsHere) {
                                    if (c != null && context.getPlayerId().equals(c.getOwner()) &&
                                        c.getBlueprint() != null &&
                                        c.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                        hasCharsOnTable = true;
                                        break;
                                    }
                                }
                            }
                            if (hasCharsOnTable) break;
                        }
                    } catch (Exception e) {
                        logger.debug("V24.9 MM guard: Error scanning for characters: {}", e.getMessage());
                    }
                }
                applyPullSpecificActionPolicy(action,
                    PullSpecificActionPolicy.scoreMasterfulMove(
                        new PullSpecificActionFacts.MasterfulMove(
                            actionId, hasCharsOnTable, mmTurn)));
                if (!hasCharsOnTable) {
                    logger.warn("V24.9 MASTERFUL MOVE: BLOCKED — no characters on table, save force for Executor! (-500)");
                } else if (mmTurn <= 2) {
                    logger.warn("V24.9 MASTERFUL MOVE: Penalized on turn {} — save force for Executor deployment! (-300)", mmTurn);
                }
            }

            // ========== Capacity Slot Selection (Pilot vs Passenger) ==========
            if (textLower.contains("capacity slot")) {
                boolean pilotCapacity =
                    textLower.contains("pilot capacity slot")
                    || textLower.contains("driver capacity slot");
                boolean passengerCapacity = textLower.contains("passenger capacity slot");
                MoveTransitPolicy.CapacityChoice capacity =
                    MoveTransitPolicy.capacityChoice(
                        pilotCapacity, passengerCapacity);
                if (capacity.branch()
                        == MoveTransitPolicy.CapacitySlotBranch.PILOT_PREFER) {
                    action.setScore(capacity.replacementScore());
                    action.addReasoning(
                        capacity.contribution().reason(),
                        capacity.contribution().delta());
                    action.setActionType(ActionType.MOVE);
                    logger.info("PILOT SLOT: Strongly preferring pilot capacity (+100)");
                } else if (capacity.branch()
                        == MoveTransitPolicy.CapacitySlotBranch.PASSENGER_SKIP) {
                    action.setScore(capacity.replacementScore());
                    action.addReasoning(
                        capacity.contribution().reason(),
                        capacity.contribution().delta());
                    action.setActionType(ActionType.MOVE);
                    logger.warn("PASSENGER SLOT: Penalizing - no power contribution ({})",
                        capacity.contribution().delta());
                }
                actions.add(action);
                continue;
            }

            SetupPolicy.SagaEvaluation saga =
                    SetupPolicy.sagaChoice(context.getDeckName(), actionText);
            if (saga.sagaChoice()) {
                if (saga.contribution() != null) {
                    action.addReasoning(
                            saga.contribution().reason(), saga.contribution().delta());
                    logger.warn("SETUP {}: {} ({})", saga.contribution().branch(),
                            saga.contribution().reason(), saga.contribution().delta());
                }
                actions.add(action);
                continue;
            }

            // ========== Force Activation ==========
            if (ActivateActionPolicy
                    .isSimpleActivationActionText(actionText)) {
                action.setActionType(ActionType.ACTIVATE_FORCE);
                try {
                    ActivateActionPolicy.Evaluation activation =
                            ActivateActionPolicy.alwaysActivate(actionId);
                    activateBaseLedger.register(activation.result());
                    PolicyOperationAdapter.apply(action, activateBaseLedger);
                    logger.info("V38.3 ACTIVATE FORCE: Scored +500 — always activate");
                } catch (Exception e) {
                    // V29.13: NEVER skip activation due to exceptions.
                    // Default to high score so Rando always activates Force.
                    logger.warn("V29.13: Exception in evaluateActivateForce, defaulting to ACTIVATE: {}", e.getMessage());
                    action.addReasoning("V29.13 SAFE DEFAULT: Always activate Force", VERY_GOOD_DELTA);
                }
            }

            // ========== V53b: STACK JEDI HERE — Save Jedi Survivors ==========
            // Fallen Order lets you lose 1 force to stack a Jedi Survivor back on it,
            // saving them from being lost. ALWAYS do this — losing 1 force to save a
            // Jedi is the best trade in the game. They can redeploy next turn.
            else if (textLower.contains("stack") && textLower.contains("here")
                     && (textLower.contains("jedi") || textLower.contains("obi-wan")
                         || textLower.contains("quinlan") || textLower.contains("kelleran")
                         || textLower.contains("cal kestis") || textLower.contains("ezra")
                         || textLower.contains("ahsoka") || textLower.contains("cere")
                         || textLower.contains("sabine") || textLower.contains("luke"))) {
                applyResponsePolicy(action,
                    ResponsePolicy.scoreSaveJedi(actionId));
                logger.warn("V53b SAVE JEDI: '{}' — +500, always save Jedi Survivors!", actionText);
            }

            // ========== V53: BLOCK WOKLING EFFECT SEARCH ==========
            // Wokling (V) costs 3 Force to search for an Effect from Reserve Deck.
            // This wastes force — the search often fails (no valid targets) and even
            // when it succeeds, 3 force is better spent deploying characters.
            // Block Wokling from searching for effects entirely.
            else if (textLower.contains("effect") && textLower.contains("reserve deck")
                     && textLower.contains("deploy cost")) {
                // Check if source card is Wokling
                boolean isWokling = textLower.contains("wokling");
                if (!isWokling && cardId != null && gameState != null) {
                    try {
                        PhysicalCard wokSrc = gameState.findCardById(Integer.parseInt(cardId));
                        if (wokSrc != null && wokSrc.getTitle() != null
                            && wokSrc.getTitle().toLowerCase(Locale.ROOT).contains("wokling")) {
                            isWokling = true;
                        }
                    } catch (Exception e) { /* ignore */ }
                }
                applyPullSpecificActionPolicy(action,
                    PullSpecificActionPolicy.scoreEffectSearch(
                        new PullSpecificActionFacts.EffectSearch(
                            actionId, isWokling)));
                if (isWokling) {
                    logger.warn("V53 WOKLING BLOCKED: Wokling Effect search — 3 force wasted, HARD BLOCK!");
                }
            }

            // ========== Force Drain ==========
            else if (actionText.equals("Force drain")) {
                action.setActionType(ActionType.FORCE_DRAIN);
                evaluateForceDrain(action, context, cardId, controlLedger);
            }

            // ========== Race Destiny ==========
            else if (actionText.equals("Draw race destiny")) {
                action.setActionType(ActionType.RACE_DESTINY);
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreRaceDestiny(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== Play a Card ==========
            // V29.1: If the source card is Knowledge And Defense (V), this is a shield play.
            // Apply shield pacing — play 2 shields on turn 1, hold the rest to scout opponent.
            else if (actionText.equals("Play a card")) {
                action.setActionType(ActionType.PLAY_CARD);
                // V129 (Steve, 2026-05-24): Renamed isKnDShieldPlay → isStackedPileShieldPlay
                // and expanded detection to include AFA (Anger, Fear, Aggression — light
                // side equivalent of K&D, same stacked-pile mechanic). Symmetric with
                // chosenone — both bots now apply V102 (activation cap) and V124 (4th-slot
                // hard-block) regardless of which stacked-pile source they are running.
                String stackedPileSourceTitle = null;
                if (cardId != null && gameState != null) {
                    try {
                        PhysicalCard sourceCard = gameState.findCardById(Integer.parseInt(cardId));
                        stackedPileSourceTitle = sourceCard != null ? sourceCard.getTitle() : null;
                    } catch (Exception ignored) {
                    }
                }
                if (ShieldPolicy.isStackedPileShieldSource(stackedPileSourceTitle)) {
                    ShieldStrategy shieldStrategy = context.getShieldStrategy();
                    int turnNumber = context.getTurnNumber();
                    int shieldsOnTable = ShieldFacts.shieldsOnTable(
                            gameState, context.getPlayerId());
                    ShieldPolicy.FourthSlotPick fourthSlot =
                            new ShieldPolicy.FourthSlotPick(null, false,
                                    ShieldPolicy.FourthSlotTrigger.CLOSED);
                    if (shieldsOnTable >= 3 && shieldStrategy != null) {
                        ShieldFacts.FourthSlotFacts fourthSlotFacts =
                                ShieldFacts.fourthSlotFacts(gameState, context.getGame(),
                                        context.getPlayerId());
                        fourthSlot = shieldStrategy.fourthSlotPick(fourthSlotFacts, null);
                    }
                    boolean activationCap = shieldStrategy != null
                            && shieldStrategy.atKnDActivationCap(turnNumber);
                    int activationCount = activationCap
                            ? shieldStrategy.knDActivationsThisTurn(turnNumber) : 0;
                    boolean pacingCap = shieldStrategy != null && !activationCap
                            && shieldStrategy.atPacingCap(turnNumber);

                    controlLedger.register(ShieldPolicy.stackedPileParent(
                            actionId, shieldsOnTable, fourthSlot, activationCap,
                            activationCount, pacingCap, turnNumber));
                    PolicyOperationAdapter.apply(action, controlLedger);

                    if (shieldsOnTable >= 3 && !fourthSlot.pursue()) {
                        logger.warn("V124 K&D 4TH-SLOT BLOCK: {} shields on table, no trigger active - parent action blocked",
                                shieldsOnTable);
                    }
                    if (activationCap) {
                        logger.warn("V102 K&D ACTIVATION CAP: turn {} count {} - hard block",
                                turnNumber, activationCount);
                    }
                } else {
                    evaluatePlayCard(action, context);
                }
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: BATTLE-2 — weapons-segment window (reorg 2026-07-06) ═══
            // Owns: the weapons-segment dispatch head: fire-before-throw (V29.12 fire +300 must beat throw's 200-250),
            // Add Battle Destiny, V29.10 lightsaber throw. The wider battle-interrupt suite (V35.x hatred lifecycle,
            // V144, V155, V175, V67u Force Push) sits scattered ABOVE in this file — same section, one owner.
            // KIND mix (BATTLE-2 overall): 11 VETO / 5 BANDED / 2 ORDERING.
            // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
            // Cross-refs: BATTLE-1 (BattleEvaluator + this file's V25 power-tier block — the SUM is the
            // behavior), TARGETING (V36 weapon targeting in CardSelectionEvaluator). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== Fire Weapons ==========
            // V29.12: Fire MUST score higher than throw (250) so Rando fires the
            // lightsaber BEFORE throwing it. Throwing sacrifices the weapon (places it
            // in Lost Pile), so if throw happens first, fire becomes impossible.
            // Fire first = hit target + THEN throw for attrition destiny = double trouble.
            else if (actionText.contains("Fire")) {
                action.setActionType(ActionType.FIRE_WEAPON);
                // Check if there are valid (non-HIT) targets before firing
                // Ported from Python action_text_evaluator.py - don't fire at already-hit targets
                boolean hasValidTargets = checkForValidWeaponTargets(context);
                BattleWeaponsFacts.FireMode fireMode;
                if (hasValidTargets) {
                    if (context.getPhase() == Phase.BATTLE) {
                        fireMode = BattleWeaponsFacts.FireMode.VALID_TARGET_IN_BATTLE;
                        logger.warn("V29.12 FIRE WEAPON: Battle phase fire — must happen before throw (+300)");
                    } else {
                        fireMode = BattleWeaponsFacts.FireMode.VALID_TARGET_OUTSIDE_BATTLE;
                    }
                } else {
                    fireMode = BattleWeaponsFacts.FireMode.NO_VALID_TARGET;
                    logger.debug("Skipping weapon fire - no valid (unhit) targets");
                }
                battleFireLedger.register(BattleWeaponsPolicy.scoreActionText(
                    new BattleWeaponsFacts.ActionTextFacts(
                        actionId, BattleWeaponsFacts.ForcePushMode.NONE, fireMode,
                        BattleWeaponsFacts.ThrowMode.NONE,
                        BattleWeaponsFacts.RedrawFacts.none())));
                PolicyOperationAdapter.apply(action, battleFireLedger);
            }

            // ========== Add Battle Destiny ==========
            else if (textLower.contains("add") && textLower.contains("battle destiny")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreAddBattleDestiny(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== V29.10/V29.12: LIGHTSABER THROW — ADD DESTINY TO ATTRITION ==========
            // After firing a lightsaber, Vader can also 'throw' it to add destiny to attrition.
            // This is a SEPARATE action from firing — both can be done in the same battle.
            // The throw adds extra attrition damage which can be decisive.
            // Action text: "'Throw' to add destiny to attrition"
            //
            // V29.12 CRITICAL: Throw MUST score LOWER than Fire (300).
            // Throwing places the lightsaber in Lost Pile — if Rando throws first,
            // he can NEVER fire it. The correct sequence is:
            //   1. FIRE lightsaber at target (hit them, reduce forfeit) — score 300
            //   2. THROW lightsaber (sacrifice it for attrition destiny) — score 200
            // This gives "double trouble" — hit + extra attrition in the same battle.
            if (textLower.contains("throw") && textLower.contains("add destiny to attrition")) {
                BattleWeaponsFacts.ThrowMode throwMode;
                if (context.getPhase() == Phase.BATTLE) {
                    throwMode = BattleWeaponsFacts.ThrowMode.IN_BATTLE;
                    logger.warn("V29.12 LIGHTSABER THROW: Battle phase throw (+200, below fire's +300)");
                } else {
                    throwMode = BattleWeaponsFacts.ThrowMode.OUTSIDE_BATTLE;
                }
                battleThrowLedger.register(BattleWeaponsPolicy.scoreActionText(
                    new BattleWeaponsFacts.ActionTextFacts(
                        actionId, BattleWeaponsFacts.ForcePushMode.NONE,
                        BattleWeaponsFacts.FireMode.NONE, throwMode,
                        BattleWeaponsFacts.RedrawFacts.none())));
                PolicyOperationAdapter.apply(action, battleThrowLedger);
            }

            // ========== V29.10: HATRED CARD — CANCEL OPPONENT GAME TEXT ==========
            // Stacking a Hatred Card on an opponent's character cancels their game text.
            // This is CRITICAL because it removes attrition immunity and other protections.
            // Without Hatred, winning a battle does NOTHING if opponent is immune to attrition.
            // Action text variants:
            //   "Stack a 'Hatred Card'" (previous game)
            //   "USED: Stack 'Hatred' card on opponent's character" (this game)
            // BEST TIMING: Deploy phase — stack Hatred BEFORE initiating battle.
            // This way opponent's immunities are already gone when battle starts.
            if (textLower.contains("hatred")) {
                // V37.1: Only place hatred on OUR turn — placing during opponent's turn
                // wastes it because we can't follow up with a battle this turn.
                if (gameState != null && !context.isMyTurn()) {
                    applyBattleActionTextPolicy(action,
                        BattleActionTextPolicy.scoreHatred(
                            new BattleActionTextFacts.HatredFacts(
                                actionId, true, false, false, false, false)));
                    logger.warn("V37.1 HATRED: Opponent's turn — blocking hatred placement (-600)");
                } else {

                String decisionText = context.getDecisionText() != null
                    ? context.getDecisionText().toLowerCase(Locale.ROOT) : "";
                boolean isDeployPhase = context.getPhase() == Phase.DEPLOY
                    || decisionText.contains("deploy");
                boolean isBattlePhase = context.getPhase() == Phase.BATTLE
                    || decisionText.contains("battle") || decisionText.contains("weapons segment");

                // V35.3: STRICT hatred scoring — ONLY place hatred when Vader or Inquisitor
                // is at the SAME SITE as an opponent character. No proactive/remote hatred.
                boolean v35VaderOrInqWithOpponents = false;
                boolean v35InqOnTable = false;
                boolean v35JediAtSameSite = false;
                try {
                    if (gameState != null) {
                        String v35Pid = context.getPlayerId();
                        String v35Oid = gameState.getOpponent(v35Pid);
                        for (PhysicalCard tCard : gameState.getAllPermanentCards()) {
                            if (tCard == null || !v35Pid.equals(tCard.getOwner())) continue;
                            if (tCard.getBlueprint() == null) continue;
                            if (tCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                            com.gempukku.swccgo.common.Zone tz = tCard.getZone();
                            if (tz == null || !tz.isInPlay()) continue;
                            String tTitle = tCard.getTitle() != null ? tCard.getTitle().toLowerCase(Locale.ROOT) : "";
                            // V35.7: Hatred requires INQUISITOR only (NOT Vader alone).
                            // The card "There Are Many Hunting You Now" requires "your Inquisitor"
                            // at the same location. Vader alone cannot use hatred.
                            if (isInquisitor(tTitle)) {
                                v35InqOnTable = true;
                                PhysicalCard charLoc = tCard.getAtLocation();
                                if (charLoc != null) {
                                    float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, charLoc, v35Oid, false, false);
                                    if (oppPower > 0) {
                                        v35VaderOrInqWithOpponents = true;
                                        for (PhysicalCard lc : gameState.getCardsAtLocation(charLoc)) {
                                            if (lc == null || !v35Oid.equals(lc.getOwner())) continue;
                                            String lcT = lc.getTitle() != null ? lc.getTitle().toLowerCase(Locale.ROOT) : "";
                                            if (isJediOrPadawan(lcT)) { v35JediAtSameSite = true; break; }
                                        }
                                    }
                                }
                                if (v35VaderOrInqWithOpponents) break;
                            }
                        }
                    }
                } catch (Exception e) { /* ignore */ }

                PolicyResult hatredResult = BattleActionTextPolicy.scoreHatred(
                    new BattleActionTextFacts.HatredFacts(
                        actionId, false, v35InqOnTable,
                        v35VaderOrInqWithOpponents, v35JediAtSameSite,
                        isDeployPhase));
                applyBattleActionTextPolicy(action, hatredResult);
                if (!v35InqOnTable) {
                    // V35.7: No Inquisitor on table — hatred requires Inquisitor, BLOCK
                    logger.warn("V35.7 HATRED: No Inquisitor — hard block (-500)");
                } else if (v35VaderOrInqWithOpponents) {
                    // V35.7: Inquisitor AT SAME SITE as opponent — hatred is useful!
                    logger.warn("V35.7 HATRED: Inquisitor with opponents (jedi={}) — score +{}",
                        v35JediAtSameSite,
                        (int) hatredResult.operations().get(0).delta());
                } else {
                    // V35.3: Vader/Inquisitor NOT at same site as any opponent — DON'T waste hatred
                    logger.warn("V35.3 HATRED: No Vader/Inq co-located with opponents — blocked (-300)");
                }
            } // end V37.1 isMyTurn else block
            }

            // ========== V29.9: I HAVE YOU NOW — PLAY DURING BATTLE ==========
            // IHYN adds extra battle destiny draws when Vader is in the battle.
            // This is DEVASTATING — 2-3 extra destiny draws can turn any battle into a win.
            // Must be played DURING a battle. Check if we're in battle phase and Vader is present.
            // Also catch "i have you now" in source card check for generic action texts.
            if (textLower.contains("i have you now") || textLower.contains("ihyn")) {
                boolean vaderInBattle = false;
                if (context.getPhase() == Phase.BATTLE) {
                    // In battle — check if Vader is participating
                    try {
                        if (gameState != null && gameState.getBattleState() != null) {
                            PhysicalCard battleLoc = gameState.getBattleState().getBattleLocation();
                            if (battleLoc != null) {
                                String ihynPlayerId = context.getPlayerId();
                                for (PhysicalCard bCard : gameState.getCardsAtLocation(battleLoc)) {
                                    if (bCard == null || !ihynPlayerId.equals(bCard.getOwner())) continue;
                                    if (bCard.getTitle() != null && bCard.getTitle().toLowerCase(java.util.Locale.ROOT).contains("vader")) {
                                        vaderInBattle = true;
                                        break;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V29.9 IHYN: Error checking Vader in battle: {}", e.getMessage());
                    }

                    if (vaderInBattle) {
                        logger.warn("V29.9 IHYN: Vader in battle — mega boost (+300) for I Have You Now!");
                    } else {
                        // Still good even without Vader — adds destiny draws
                        logger.info("V29.9 IHYN: Playing during battle without Vader (+100)");
                    }
                } else {
                    // Not in battle — save IHYN for when we need it
                    logger.info("V29.9 IHYN: Not in battle — save for later (-200)");
                }
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreIHaveYouNow(
                        new BattleActionTextFacts.IHaveYouNowFacts(
                            actionId, true, context.getPhase() == Phase.BATTLE,
                            vaderInBattle, false)));
            }
            // Also check source card for IHYN when action text is generic
            else if (context.getPhase() == Phase.BATTLE && cardId != null && gameState != null) {
                try {
                    PhysicalCard ihynSource = gameState.findCardById(Integer.parseInt(cardId));
                    if (ihynSource != null && ihynSource.getTitle() != null
                        && ihynSource.getTitle().toLowerCase(java.util.Locale.ROOT).contains("i have you now")) {
                        applyBattleActionTextPolicy(action,
                            BattleActionTextPolicy.scoreIHaveYouNow(
                                new BattleActionTextFacts.IHaveYouNowFacts(
                                    actionId, false, true, false, true)));
                        logger.warn("V29.9 IHYN (source): I Have You Now detected via source card — boost +200");
                    }
                } catch (Exception e) { /* ignore */ }
            }

            // ========== V35: FAR MORE FRIGHTENING THAN DEATH ==========
            // FMFTD has two modes:
            // USED: Stack hatred on opponent's leader/ability>3 at battleground
            // LOST: Add 1-2 battle destiny if Inquisitor with Jedi/Padawan/Hatred
            // Detect via testingTexts or action text containing "far more frightening"
            if (textLower.contains("far more frightening") || textLower.contains("fmftd")) {
                boolean isFmftdBattle = context.getPhase() == Phase.BATTLE;
                boolean isFmftdUsedMode = textLower.contains("stack") || (textLower.contains("hatred") && !textLower.contains("destiny"));
                boolean isFmftdLostMode = textLower.contains("destiny") || textLower.contains("add");

                if (isFmftdLostMode && isFmftdBattle) {
                    // LOST mode during battle — check for Inquisitor + Jedi + Hatred synergy
                    boolean v35FmInq = false;
                    boolean v35FmJedi = false;
                    boolean v35FmHatred = false;
                    try {
                        if (gameState != null && gameState.getBattleState() != null) {
                            PhysicalCard fmBattleLoc = gameState.getBattleState().getBattleLocation();
                            if (fmBattleLoc != null) {
                                String fmPid = context.getPlayerId();
                                String fmOid = gameState.getOpponent(fmPid);
                                for (PhysicalCard bc : gameState.getCardsAtLocation(fmBattleLoc)) {
                                    if (bc == null) continue;
                                    String bcTitle = bc.getTitle() != null ? bc.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (fmPid.equals(bc.getOwner()) && isInquisitor(bcTitle)) v35FmInq = true;
                                    if (fmOid != null && fmOid.equals(bc.getOwner())) {
                                        if (isJediOrPadawan(bcTitle)) v35FmJedi = true;
                                        java.util.List<PhysicalCard> st = gameState.getStackedCards(bc);
                                        if (st != null && !st.isEmpty()) v35FmHatred = true;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    int synCount = (v35FmInq ? 1 : 0) + (v35FmJedi ? 1 : 0) + (v35FmHatred ? 1 : 0);
                    PolicyResult fmftdResult = BattleActionTextPolicy.scoreFmftd(
                        new BattleActionTextFacts.FmftdFacts(
                            actionId, BattleActionTextFacts.FmftdMode.LOST,
                            true, false, v35FmInq, v35FmJedi, v35FmHatred));
                    applyBattleActionTextPolicy(action, fmftdResult);
                    if (synCount >= 3) {
                        logger.warn("V35 FMFTD: Full synergy! +{}",
                            (int) fmftdResult.operations().get(0).delta());
                    }
                } else if (isFmftdUsedMode) {
                    // USED mode — place hatred card
                    applyBattleActionTextPolicy(action,
                        BattleActionTextPolicy.scoreFmftd(
                            new BattleActionTextFacts.FmftdFacts(
                                actionId, BattleActionTextFacts.FmftdMode.USED,
                                isFmftdBattle,
                                context.getPhase() == Phase.DEPLOY
                                    || context.getPhase() == Phase.MOVE,
                                false, false, false)));
                } else if (isFmftdBattle) {
                    // Generic FMFTD during battle — likely the LOST mode
                    applyBattleActionTextPolicy(action,
                        BattleActionTextPolicy.scoreFmftd(
                            new BattleActionTextFacts.FmftdFacts(
                                actionId, BattleActionTextFacts.FmftdMode.GENERIC,
                                true, false, false, false, false)));
                } else {
                    applyBattleActionTextPolicy(action,
                        BattleActionTextPolicy.scoreFmftd(
                            new BattleActionTextFacts.FmftdFacts(
                                actionId, BattleActionTextFacts.FmftdMode.GENERIC,
                                false, false, false, false, false)));
                }
            }

            // ========== V35: VADER SELF-RECALL (Hunt Down V once-per-game) ==========
            // "Take Vader into hand" — allows redeploying Vader to hunt Jedi elsewhere
            // "Return an Inquisitor here to hand" — Eighth Brother repositioning
            else if (textLower.contains("take vader into hand")
                        && (isClassicHuntDownActionSource(
                                gameState, cardId)
                            || isVirtualHuntDownActionSource(
                                gameState, cardId))
                    || textLower.contains("return")
                        && textLower.contains("inquisitor")
                        && textLower.contains("hand")) {
                if (textLower.contains("vader")) {
                    if (isVirtualHuntDownActionSource(
                            gameState, cardId)) {
                        boolean preservesPostFlipVader = false;
                        try {
                            var recallObjective =
                                context.getObjectiveAnalyzer();
                            preservesPostFlipVader =
                                recallObjective != null
                                && recallObjective
                                    .hasSafeVirtualHuntDownVaderRecallTarget(
                                        game, context.getPlayerId());
                        } catch (Exception e) { /* ignore */ }
                        PolicyResult recallGuard =
                            ObjectiveHardLossPolicy.scoreRecall(
                                new ObjectiveHardLossPolicy.RecallFacts(
                                    actionId,
                                    ObjectiveHardLossPolicy.RecallKind
                                        .VIRTUAL,
                                    preservesPostFlipVader));
                        if (!recallGuard.operations().isEmpty()) {
                            PolicyContributionLedger recallGuardLedger =
                                new PolicyContributionLedger(
                                    "objective-recall-" + actionId);
                            recallGuardLedger.register(recallGuard);
                            PolicyOperationAdapter.apply(
                                action, recallGuardLedger);
                            actions.add(action);
                            continue;
                        }
                        applyBattleActionTextPolicy(action,
                            BattleActionTextPolicy
                                .scoreVirtualVaderRecall(
                                    new BattleActionTextFacts
                                        .ActionFacts(actionId)));
                        actions.add(action);
                        continue;
                    }
                    boolean v35JediElsewhere = false;
                    boolean preservesObjectiveActor = false;
                    try {
                        var recallObjective =
                            context.getObjectiveAnalyzer();
                        if (recallObjective != null) {
                            var recallAssessment = recallObjective
                                .assessClassicHuntDownVaderRecall(
                                    game, context.getPlayerId());
                            preservesObjectiveActor =
                                recallAssessment.safeTarget();
                            v35JediElsewhere =
                                recallAssessment
                                    .remoteJediOrLukeBlocker();
                        }
                    } catch (Exception e) { /* ignore */ }

                    PolicyResult classicRecallGuard =
                        ObjectiveHardLossPolicy.scoreRecall(
                            new ObjectiveHardLossPolicy.RecallFacts(
                                actionId,
                                ObjectiveHardLossPolicy.RecallKind
                                    .CLASSIC,
                                preservesObjectiveActor));
                    if (!classicRecallGuard.operations().isEmpty()) {
                        PolicyContributionLedger recallGuardLedger =
                            new PolicyContributionLedger(
                                "objective-recall-" + actionId);
                        recallGuardLedger.register(
                            classicRecallGuard);
                        PolicyOperationAdapter.apply(
                            action, recallGuardLedger);
                        actions.add(action);
                        continue;
                    }

                    if (v35JediElsewhere) {
                        logger.warn("V35 VADER RECALL: Jedi detected elsewhere — recalling Vader to redeploy (+300)");
                    }
                    applyBattleActionTextPolicy(action,
                        BattleActionTextPolicy.scoreVaderRecall(
                            new BattleActionTextFacts.VaderRecallFacts(
                                actionId, v35JediElsewhere)));
                } else {
                    // V35.1: Inquisitor recall — DON'T recall if opponents are nearby!
                    // Eighth Brother's ability returns an Inquisitor to hand. Only do this
                    // if there are NO opponents at adjacent sites. If opponents are nearby,
                    // keep the Inquisitor to fight!
                    boolean opponentsNearby = false;
                    try {
                        if (gameState != null) {
                            String recallPid = context.getPlayerId();
                            String recallOid = gameState.getOpponent(recallPid);
                            for (PhysicalCard loc : gameState.getTopLocations()) {
                                if (loc == null) continue;
                                float oppPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, recallOid, false, false);
                                if (oppPwr > 0) { opponentsNearby = true; break; }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }

                    if (opponentsNearby) {
                        logger.warn("V35.1 INQUISITOR RECALL BLOCKED: Opponents present — don't pull back (-400)");
                    }
                    applyBattleActionTextPolicy(action,
                        BattleActionTextPolicy.scoreInquisitorRecall(
                            new BattleActionTextFacts.InquisitorRecallFacts(
                                actionId, opponentsNearby)));
                }
            }

            // ========== V37.2: STUNNING LEADER — DEFENSIVE ONLY ==========
            // Stunning Leader excludes characters from battle. Good when DEFENDING
            // against a stronger opponent (saves Vader from certain death).
            // BAD when WE initiated (we started the fight to WIN).
            // V194: Force Push has its own battle/exchange policy below.
            else if (textLower.contains("stunning leader")
                    || (!textLower.contains("force push")
                        && !v67uIsForcePushSource
                        && textLower.contains("exclude")
                        && textLower.contains("from battle"))) {
                BattleActionTextFacts.StunningLeaderMode stunningLeaderMode =
                    BattleActionTextFacts.StunningLeaderMode.OUTSIDE_BATTLE;
                float stunningLeaderOurPower = 0.0f;
                float stunningLeaderTheirPower = 0.0f;
                if (context.getPhase() == Phase.BATTLE && gameState != null) {
                    stunningLeaderMode = BattleActionTextFacts.StunningLeaderMode.UNRESOLVED;
                    try {
                        com.gempukku.swccgo.game.state.BattleState bState = gameState.getBattleState();
                        if (bState != null) {
                            String slPlayerId = context.getPlayerId();
                            String slInitiator = bState.getPlayerInitiatedBattle();
                            boolean weInitiated = slPlayerId != null && slPlayerId.equals(slInitiator);

                            if (weInitiated) {
                                // WE started this battle — NEVER cancel our own attack!
                                stunningLeaderMode = BattleActionTextFacts.StunningLeaderMode.OWN_INITIATED;
                                logger.warn("V37.2 STUNNING LEADER: HARD BLOCK — we initiated this battle!");
                            } else {
                                // Opponent initiated — check if we're outmatched
                                PhysicalCard slBattleLoc = bState.getBattleLocation();
                                if (slBattleLoc != null) {
                                    String slOpp = gameState.getOpponent(slPlayerId);
                                    stunningLeaderOurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, slBattleLoc, slPlayerId, false, false);
                                    stunningLeaderTheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, slBattleLoc, slOpp, false, false);
                                    stunningLeaderMode = BattleActionTextFacts.StunningLeaderMode.DEFENDING;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V37.2 STUNNING LEADER: Error: {}", e.getMessage());
                    }
                }
                PolicyResult stunningLeaderResult = BattleActionTextPolicy.scoreStunningLeader(
                    new BattleActionTextFacts.StunningLeaderFacts(
                        actionId, stunningLeaderMode,
                        stunningLeaderOurPower, stunningLeaderTheirPower));
                applyBattleActionTextPolicy(action, stunningLeaderResult);
                if (stunningLeaderMode == BattleActionTextFacts.StunningLeaderMode.DEFENDING
                        && !stunningLeaderResult.operations().isEmpty()
                        && stunningLeaderResult.operations().get(0).delta() > 0.0f) {
                    logger.warn("V37.2 STUNNING LEADER: Defensive use — saving characters from {} vs {}",
                        (int) stunningLeaderOurPower, (int) stunningLeaderTheirPower);
                }
            }

            // ========== V35.4: YOU ARE BEATEN — DON'T WASTE ON UNDERCOVER SPIES ==========
            // You Are Beaten targets opponent characters. But undercover spies appear on OUR side
            // and aren't valid targets for combat effects. Don't waste this interrupt.
            // Also: only use during battle or when it will lead to meaningful attrition.
            else if (textLower.contains("you are beaten")) {
                boolean v354Battle = context.getPhase() == Phase.BATTLE;
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreGenericYouAreBeaten(
                        new BattleActionTextFacts.GenericYouAreBeatenFacts(
                            actionId, v354Battle)));
                if (!v354Battle) {
                    // Outside battle — this is usually a waste
                    logger.info("V35.4 YOU ARE BEATEN: Not in battle — penalizing (-200)");
                }
            }

            // ========== Battle Destiny Modifier (+1 to battle destiny) ==========
            else if ((actionText.contains("+1") || actionText.contains("+ 1") || textLower.contains("add 1"))
                     && textLower.contains("battle destiny")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreBattleDestinyModifier(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== V24.2: Force Drain Modifier (+1 to force drain) ==========
            // Cards like Lord Maul With Lightsaber add +1 to force drain as an optional response.
            // This should ALWAYS be accepted — free extra damage!
            else if ((actionText.contains("+1") || actionText.contains("+ 1") || textLower.contains("add 1"))
                     && textLower.contains("force drain")) {
                action.setActionType(ActionType.FORCE_DRAIN);
                controlLedger.register(ControlActionPolicy.forceDrainModifier(actionId));
                PolicyOperationAdapter.apply(action, controlLedger);
                logger.warn("V24.2 DRAIN BONUS: Accepting +1 force drain — '{}'", actionText);
            }

            // ========== Weapon Destiny Modifier ==========
            else if (textLower.contains("weapon destiny") &&
                     (actionText.contains("+3") || actionText.contains("+2") || textLower.contains("add"))) {
                action.setActionType(ActionType.FIRE_WEAPON);
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreWeaponDestinyModifier(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== Protect Battle Destiny Draws ==========
            else if (textLower.contains("prevent") && textLower.contains("cancel") &&
                     textLower.contains("battle destiny") && textLower.contains("draw")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                Phase destinyProtectionPhase = context.getPhase();
                BattleActionTextFacts.DestinyProtectionPhase policyPhase =
                    destinyProtectionPhase == Phase.BATTLE
                        ? BattleActionTextFacts.DestinyProtectionPhase.BATTLE
                        : destinyProtectionPhase == Phase.ACTIVATE
                            ? BattleActionTextFacts.DestinyProtectionPhase.ACTIVATE
                            : destinyProtectionPhase == Phase.CONTROL
                                ? BattleActionTextFacts.DestinyProtectionPhase.CONTROL
                                : destinyProtectionPhase == Phase.DEPLOY
                                    ? BattleActionTextFacts.DestinyProtectionPhase.DEPLOY
                                    : BattleActionTextFacts.DestinyProtectionPhase.OTHER;
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreProtectDestiny(
                        new BattleActionTextFacts.ProtectDestinyFacts(
                            actionId, context.getTurnNumber(), policyPhase)));
            }

            // ========== Prevent Opponent Adding Battle Destiny ==========
            else if (textLower.contains("prevent") && textLower.contains("battle destiny") &&
                     !textLower.contains("cancel")) {
                action.setActionType(ActionType.BATTLE_DESTINY);
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scorePreventOpponentBattleDestiny(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== Take Admiral/General Into Hand ==========
            // For TDIGWATT/Bespin objectives: an Imperial admiral pulled here is likely
            // a pilot (e.g., Admiral Chiraneau). That pilot enables deploying the Executor
            // to Bespin cheaply — the Executor + pilot simultaneous deploy is the critical
            // Turn 1 play for Cloud City objectives. Prioritise VERY highly when we have
            // no ship at Bespin yet.
            else if (textLower.contains("take") && textLower.contains("into hand") &&
                     (textLower.contains("admiral") || textLower.contains("general"))) {
                // Check if we're running a Bespin/Cloud City objective with no ship there yet
                boolean bespinChainActive = false;
                try {
                    com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer objAnalyzer =
                        context.getObjectiveAnalyzer();
                    if (objAnalyzer != null && objAnalyzer.isAnalyzed() &&
                        objAnalyzer.needsBespinSystemPresence()) {
                        // Check if we already have a ship at Bespin system
                        boolean hasBespinShip = false;
                        if (gameState != null) {
                            String pid = context.getPlayerId();
                            for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc != null && loc.getTitle() != null &&
                                    loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                                    loc.getBlueprint() != null &&
                                    loc.getBlueprint().getCardSubtype() ==
                                        com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                    float ourPower = context.getGame().getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, loc, pid, false, false);
                                    if (ourPower > 0) hasBespinShip = true;
                                    break;
                                }
                            }
                        }
                        if (!hasBespinShip) {
                            bespinChainActive = true;
                        }
                    }
                } catch (Exception e) {
                    // Ignore — fall back to default scoring
                }

                // V29.7: Check if there are actually admirals/generals left in Reserve
                boolean hasValidTarget = true;
                try {
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle oracle = context.getDeckOracle();
                    if (oracle != null && oracle.isAnalyzed()) {
                        hasValidTarget = oracle.hasTargetInReserve("admiral", "general");
                        if (!hasValidTarget) {
                            logger.warn("V29.7 PULL CHECK: No admirals/generals left in Reserve — blocking pull!");
                        }
                    }
                } catch (Exception pullCheckE) {
                    // Can't check — assume target exists
                }

                applyPullSpecificActionPolicy(action,
                    PullSpecificActionPolicy.scoreAdmiralGeneralPull(
                        new PullSpecificActionFacts.AdmiralGeneralPull(
                            actionId, hasValidTarget, bespinChainActive)));
                if (hasValidTarget && bespinChainActive) {
                    logger.warn("EXECUTOR CHAIN: Admiral pull with no Bespin ship — boosting to 300 (enables Executor pipeline)");
                }
            }

            // ========== V175: KILL SHOT — "Make <character> lost" ==========
            // Steve (live ROTS Dooku games): "Rando almost never uses interrupts
            // offensively during battle." Log forensics: the engine offered FIVE
            // kill-shots on Steve's characters ("Make Yoda lost", "Make Rey lost",
            // "Make Ben Solo lost", "Make Anakin lost", "Make Han lost" — the
            // Sniper / Dark Strike / 'hit'-follow-up class) and ALL FIVE scored 0.0
            // "Unknown action type" and lost to Pass (7-16). Now: parse the target,
            // check ownership — an OPPONENT character scores a big kill-shot bonus
            // scaled by its power+forfeit; our OWN character (some windows list
            // self-targets / sacrifice modes) gets a small negative so Pass wins.
            else if (textLower.contains("make ") && textLower.endsWith(" lost")) {
                try {
                    int v175Mi = textLower.indexOf("make ");
                    int v175Li = textLower.lastIndexOf(" lost");
                    String v175Target = actionText.substring(v175Mi + 5, v175Li).trim();
                    com.gempukku.swccgo.game.state.GameState v175Gs = context.getGameState();
                    String v175Pid = context.getPlayerId();
                    PhysicalCard v175Found = null;
                    if (v175Gs != null && !v175Target.isEmpty()) {
                        for (PhysicalCard v175C : v175Gs.getAllPermanentCards()) {
                            if (v175C != null && v175C.getTitle() != null
                                    && v175C.getTitle().equalsIgnoreCase(v175Target)) {
                                v175Found = v175C; break;
                            }
                        }
                    }
                    BattleActionTextFacts.KillShotTarget v175TargetType =
                        BattleActionTextFacts.KillShotTarget.UNRESOLVED;
                    float v175Pow = 0f;
                    float v175Forf = 0f;
                    if (v175Found != null && v175Pid != null
                            && !v175Pid.equals(v175Found.getOwner())) {
                        v175TargetType = BattleActionTextFacts.KillShotTarget.OPPONENT;
                        SwccgCardBlueprint v175Bp = v175Found.getBlueprint();
                        if (v175Bp != null) {
                            if (v175Bp.hasPowerAttribute() && v175Bp.getPower() != null) v175Pow = v175Bp.getPower();
                            if (v175Bp.hasForfeitAttribute() && v175Bp.getForfeit() != null) v175Forf = v175Bp.getForfeit();
                        }
                    } else if (v175Found != null) {
                        v175TargetType = BattleActionTextFacts.KillShotTarget.OWN;
                    }
                    PolicyResult v175KillShot = BattleActionTextPolicy.scoreKillShot(
                        new BattleActionTextFacts.KillShotFacts(
                            actionId, v175Target, v175TargetType, v175Pow, v175Forf));
                    applyBattleActionTextPolicy(action, v175KillShot);
                    if (v175TargetType == BattleActionTextFacts.KillShotTarget.OPPONENT) {
                        float v175Score = v175KillShot.operations().get(0).delta();
                        logger.warn("V175 KILL SHOT: '{}' (pow={} forf={}) -> +{}",
                            v175Target, (int) v175Pow, (int) v175Forf, (int) v175Score);
                    }
                } catch (Exception v175E) {
                    logger.debug("V175 kill-shot parse error: {}", v175E.getMessage());
                }
            }

            // ========== Substitute Destiny ==========
            else if (textLower.contains("substitute destiny")) {
                action.setActionType(ActionType.SUBSTITUTE_DESTINY);
                // V175 (Steve, 2026-06): score the DELTA, not a flat +30. Welcome Home
                // substitutes Tyranus's ability 7 for a just-drawn destiny — brilliant
                // when the draw was a 1, a waste when it was a 6. The just-drawn card
                // sits in unresolved destiny draws (its printed destiny = drawn value);
                // the substitute value is approximated by our best ability in the
                // battle. delta*60 (a 6-point swing = +360 > Pass); non-positive delta
                // -> -50 (save the card). Falls back to the old flat +30 when either
                // value is unreadable.
                float v175Drawn = -1f, v175BestAb = -1f;
                try {
                    com.gempukku.swccgo.game.state.GameState v175SGs = context.getGameState();
                    String v175SPid = context.getPlayerId();
                    if (v175SGs != null && v175SPid != null) {
                        PhysicalCard v175DrawnCard = v175SGs.getTopOfUnresolvedDestinyDraws(v175SPid);
                        if (v175DrawnCard != null && v175DrawnCard.getBlueprint() != null
                                && v175DrawnCard.getBlueprint().getDestiny() != null) {
                            v175Drawn = v175DrawnCard.getBlueprint().getDestiny();
                        }
                        PhysicalCard v175BLoc = v175SGs.getBattleLocation();
                        if (v175BLoc != null) {
                            for (PhysicalCard v175BC : v175SGs.getCardsAtLocation(v175BLoc)) {
                                if (v175BC != null && v175SPid.equals(v175BC.getOwner())
                                        && v175BC.getBlueprint() != null
                                        && v175BC.getBlueprint().hasAbilityAttribute()
                                        && v175BC.getBlueprint().getAbility() != null) {
                                    v175BestAb = Math.max(v175BestAb, v175BC.getBlueprint().getAbility());
                                }
                            }
                        }
                    }
                } catch (Exception ignore) { }
                boolean v175Readable = v175Drawn >= 0f && v175BestAb > 0f;
                PolicyResult v175Substitute = BattleActionTextPolicy.scoreSubstituteDestiny(
                    new BattleActionTextFacts.SubstituteDestinyFacts(
                        actionId,
                        v175Readable
                            ? BattleActionTextFacts.SubstituteReadStatus.READ
                            : BattleActionTextFacts.SubstituteReadStatus.READ_FAILED,
                        v175Drawn, v175BestAb));
                applyBattleActionTextPolicy(action, v175Substitute);
                if (v175Readable && v175Substitute.operations().get(0).delta() > 0.0f) {
                    logger.warn("V175 SUBSTITUTE: drawn={} bestAbility={} -> +{}",
                        (int) v175Drawn, (int) v175BestAb,
                        (int) v175Substitute.operations().get(0).delta());
                }
            }

            // ========== React ==========
            else if (textLower.contains("react")) {
                action.setActionType(ActionType.REACT);
                applyResponsePolicy(action,
                    ResponsePolicy.scoreReact(actionId));
            }

            // ========== Steal ==========
            else if (textLower.contains("steal")) {
                action.setActionType(ActionType.STEAL);
                controlLedger.register(ControlActionPolicy.steal(actionId));
                PolicyOperationAdapter.apply(action, controlLedger);
            }

            // ========== Sabacc ==========
            else if (textLower.contains("play sabacc")) {
                action.setActionType(ActionType.SABACC);
                action.addReasoning("Playing sabacc", GOOD_DELTA);
            }

            // ========== Cancel Own Cards (Bad!) ==========
            else if (textLower.contains("cancel your")) {
                action.setActionType(ActionType.CANCEL);
                applyResponsePolicy(action,
                    ResponsePolicy.scoreCancelOwn(actionId));
            }

            // ========== Cancel Opponent's Interrupt (Sense/Control) ==========
            else if (textLower.contains("cancel") &&
                     (textLower.contains("interrupt") || textLower.contains("sense") ||
                      textLower.contains("alter") || textLower.contains("effect") ||
                      textLower.contains("force drain")) &&
                     !textLower.contains("your")
                     // V194: let the dedicated cancel-and-redraw branch score this action.
                     && !(textLower.contains("redraw") && textLower.contains("destiny"))) {
                action.setActionType(ActionType.CANCEL);
                evaluateSenseCancel(action, context, actionText, controlLedger);
            }

            // ========== V37: Cancel/Redraw Destiny — CHECK CURRENT VALUE FIRST ==========
            // Imperial Enforcement and similar cards cancel a destiny draw and cause a redraw.
            // Only use if the current destiny is LOW (< 3). A 6-destiny character draw is
            // essentially the best possible — NEVER cancel that.
            // Use DeckOracle average to decide if redraw is likely to improve.
            else if (textLower.contains("cancel") && textLower.contains("redraw") && textLower.contains("destiny")) {
                // Try to extract the current destiny value from the action text
                // Format often includes the drawn card name — check for high destiny numbers
                float currentDestinyDrawn = -1;
                BattleWeaponsFacts.RedrawFacts redrawFacts;
                try {
                    // The action text often says "cancel X's battle destiny draw of <CardName>"
                    // We can check DeckOracle for average destiny to decide
                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle redrawOracle = context.getDeckOracle();
                    double avgDest = 3.0;
                    if (redrawOracle != null && redrawOracle.isAnalyzed()) {
                        avgDest = redrawOracle.getAverageDestinyInReserve();
                    }

                    // Extract destiny number from action text if present (e.g., "draw of X as a 6")
                    java.util.regex.Matcher destMatcher = java.util.regex.Pattern.compile("as a (\\d+)").matcher(textLower);
                    if (destMatcher.find()) {
                        currentDestinyDrawn = Float.parseFloat(destMatcher.group(1));
                    }

                    if (currentDestinyDrawn >= 0) {
                        if (currentDestinyDrawn >= 3) {
                            logger.warn("V37 REDRAW BLOCKED: Destiny {} is >= 3 (avg {}) — don't cancel!",
                                (int)currentDestinyDrawn, String.format("%.1f", avgDest));
                        }
                        redrawFacts = BattleWeaponsFacts.RedrawFacts.known(
                            currentDestinyDrawn, avgDest);
                    } else {
                        redrawFacts = BattleWeaponsFacts.RedrawFacts.unknown(avgDest);
                    }
                } catch (Exception e) {
                    redrawFacts = BattleWeaponsFacts.RedrawFacts.readFailed();
                }
                battleRedrawLedger.register(BattleWeaponsPolicy.scoreActionText(
                    new BattleWeaponsFacts.ActionTextFacts(
                        actionId, BattleWeaponsFacts.ForcePushMode.NONE,
                        BattleWeaponsFacts.FireMode.NONE,
                        BattleWeaponsFacts.ThrowMode.NONE, redrawFacts)));
                PolicyOperationAdapter.apply(action, battleRedrawLedger);
            }

            // ========== Cancel Weapon Targeting ==========
            else if (textLower.contains("cancel") && textLower.contains("weapon") && textLower.contains("target")) {
                action.setActionType(ActionType.CANCEL);
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreCancelWeaponTargeting(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== Immune to Attrition ==========
            else if (textLower.contains("immune to attrition")) {
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreImmuneToAttrition(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== Protect Forfeit ==========
            else if (textLower.contains("forfeit") &&
                     (textLower.contains("protect") || textLower.contains("preserved"))) {
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreProtectForfeit(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== Re-target Weapon ==========
            else if (textLower.contains("re-target") || textLower.contains("retarget")) {
                applyBattleActionTextPolicy(action,
                    BattleActionTextPolicy.scoreRetargetWeapon(
                        new BattleActionTextFacts.ActionFacts(actionId)));
            }

            // ========== Cancel Battle Damage (Houjix/Ghhhk) ==========
            else if (actionText.contains("Cancel all remaining battle damage")) {
                action.setActionType(ActionType.CANCEL_DAMAGE);
                evaluateHoujixGhhhk(action, context);
            }

            // ========== V67af: RETURN-OWN-CHARACTER-TO-HAND BOUNCE BLOCK ==========
            // Steve's report: Rando deploys General Grievous, then uses Grievous's
            // 'Lose 1 Force to return Grievous to hand' game text to bounce him —
            // wasting both the deploy cost AND the bounce cost. V29.7 BOUNCE only
            // fires for 'Take X into hand' actions; Grievous and similar cards say
            // 'Return X to hand', which V29.7 misses entirely.
            //
            // Rule: when an action says 'Return <X> to hand' AND the source card is
            // a character we own AND the action requires losing force, hard-block.
            // The tactical use case (escape death) is too rare to justify Rando's
            // pattern of deploy-then-bounce loops.
            else if (textLower.contains("return") && textLower.contains("to hand")
                    && cardId != null && gameState != null && context.getPlayerId() != null) {
                boolean v67afBlock = false;
                String v67afDetail = null;
                try {
                    PhysicalCard srcPc = gameState.findCardById(Integer.parseInt(cardId));
                    if (srcPc != null && srcPc.getBlueprint() != null
                            && context.getPlayerId().equals(srcPc.getOwner())
                            && srcPc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                        v67afBlock = true;
                        v67afDetail = srcPc.getTitle();
                    }
                } catch (Exception e) { /* ignore */ }
                if (v67afBlock) {
                    action.addReasoning(String.format(
                        "V67af RETURN-TO-HAND BLOCK: bouncing %s wastes deploy cost — DON'T undo your deploy!",
                        v67afDetail), -9999.0f);
                    logger.warn("V67af RETURN BLOCK: source={} action='{}' — HARD BLOCK (-9999)",
                        v67afDetail, actionText);
                } else {
                    // Default: still discourage but lighter touch (handles edge cases
                    // like opponent-effect-induced returns we haven't classified yet).
                    action.addReasoning("V67af RETURN-TO-HAND: unclassified return action — light penalty",
                        -150.0f);
                    logger.info("V67af RETURN-TO-HAND: '{}' source unclassifiable — -150",
                        actionText);
                }
            }

            // ========== V67an (Steve, 2026-05-07): WEAPON SWAP TO FREE MATCHING SLOT ==========
            //
            // Steve's rule: if Rando has a non-unique/non-matching weapon attached to
            // a character (e.g., generic Dark Jedi Lightsaber on Vader) AND has a
            // unique persona-matched weapon for that character in hand (e.g., Vader's
            // Lightsaber), Rando should TRANSFER the wrong weapon to a buddy at the
            // same site. After the transfer the matching character is unarmed, so the
            // V67ad two-weapon hard-block lifts and the matching unique weapon can
            // deploy on its persona — net result: 2 characters armed, persona bonuses
            // active for the matching weapon (immune, fire-for-free, +power, etc.).
            //
            // Detection: action text starts with "Transfer" (rules-level transfer) or
            // contains "Transfer device" / "Transfer weapon".
            //
            // Bonus +400 fires when:
            //   - The transfer source weapon is NOT unique OR has no matchingCharacter
            //     filter pointing at its current attachee
            //   - Rando has another weapon in hand whose matchingCharacter filter
            //     DOES target the current attachee (or whose title matches the persona)
            //
            // If we can't determine matchingCharacter unambiguously, fall back to
            // a milder +150 ('transfers usually mean tactical swap').
            else if (actionText.contains("Transfer")
                    && (actionText.contains("weapon") || actionText.contains("device")
                        || textLower.startsWith("transfer "))) {
                action.setActionType(ActionType.UNKNOWN);
                float v67anBonus = 150.0f;
                String v67anReason = "transfer action — usually a tactical swap";
                try {
                    if (cardId != null && gameState != null && context.getPlayerId() != null) {
                        PhysicalCard transferSrc = gameState.findCardById(Integer.parseInt(cardId));
                        if (transferSrc != null && transferSrc.getBlueprint() != null
                                && transferSrc.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                            // Identify current attachee
                            PhysicalCard attachee = transferSrc.getAttachedTo();
                            if (attachee != null && attachee.getBlueprint() != null
                                    && context.getPlayerId().equals(attachee.getOwner())) {
                                String attacheeTitleLower = attachee.getTitle() != null
                                    ? attachee.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                // Is the current weapon non-unique OR not matched to attachee?
                                boolean weaponIsNonUnique = transferSrc.getBlueprint().getUniqueness()
                                    != com.gempukku.swccgo.common.Uniqueness.UNIQUE;
                                boolean weaponMatchesAttachee = false;
                                try {
                                    com.gempukku.swccgo.filters.Filter mcFilter =
                                        transferSrc.getBlueprint().getMatchingCharacterFilter();
                                    if (mcFilter != null) {
                                        weaponMatchesAttachee = mcFilter.accepts(context.getGame(), attachee);
                                    }
                                } catch (Exception e) { /* ignore */ }

                                // Do we have a UNIQUE matching weapon for the attachee in hand?
                                // Steve's clarification: only swap when a UNIQUE persona-matched
                                // weapon is waiting (e.g. Ahsoka's Shoto Lightsaber for Ahsoka,
                                // Luke's Hunting Rifle for Luke). Generic-for-generic swaps don't
                                // gain anything.
                                boolean haveMatchingInHand = false;
                                String matchingTitle = null;
                                try {
                                    for (PhysicalCard hc : gameState.getHand(context.getPlayerId())) {
                                        if (hc == null || hc.getBlueprint() == null) continue;
                                        if (hc.getBlueprint().getCardCategory() != CardCategory.WEAPON) continue;
                                        // STRICT: only consider UNIQUE weapons.
                                        if (hc.getBlueprint().getUniqueness()
                                                != com.gempukku.swccgo.common.Uniqueness.UNIQUE) continue;
                                        // Persona-match the hand weapon to the attachee.
                                        try {
                                            com.gempukku.swccgo.filters.Filter handMc =
                                                hc.getBlueprint().getMatchingCharacterFilter();
                                            if (handMc != null
                                                    && handMc.accepts(context.getGame(), attachee)) {
                                                haveMatchingInHand = true;
                                                matchingTitle = hc.getTitle();
                                                break;
                                            }
                                        } catch (Exception e) { /* ignore */ }
                                        // Persona-name fallback for unique weapons whose
                                        // matchingCharacterFilter we couldn't query (rare).
                                        // E.g. "Ahsoka's Shoto Lightsaber" title contains "ahsoka"
                                        // → matches an Ahsoka attachee.
                                        if (!haveMatchingInHand && hc.getTitle() != null
                                                && !attacheeTitleLower.isEmpty()) {
                                            String htl = hc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                            String[] parts = attacheeTitleLower.split("\\s+");
                                            for (String p : parts) {
                                                if (p.length() >= 4 && htl.contains(p)) {
                                                    haveMatchingInHand = true;
                                                    matchingTitle = hc.getTitle();
                                                    break;
                                                }
                                            }
                                            if (haveMatchingInHand) break;
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                if ((weaponIsNonUnique || !weaponMatchesAttachee) && haveMatchingInHand) {
                                    v67anBonus = 400.0f;
                                    v67anReason = String.format(
                                        "transfer wrong/generic weapon off %s (have matching '%s' in hand) — frees slot for persona-matched deploy!",
                                        attachee.getTitle(), matchingTitle);
                                }

                                // V72 (Steve, 2026-05-15): WEAPON REDISTRIBUTION.
                                // If the source character has 2+ weapons attached AND there's an
                                // unarmed friendly at the same site, transferring redistributes
                                // weapons across the team. Massively preferred over swap-from-hand
                                // because it directly fixes the "one char has 2 lightsabers,
                                // others have none" pattern.
                                try {
                                    int weaponsOnAttachee = 0;
                                    java.util.List<PhysicalCard> atts = gameState.getAttachedCards(attachee);
                                    if (atts != null) {
                                        for (PhysicalCard a : atts) {
                                            if (a != null && a.getBlueprint() != null
                                                    && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                weaponsOnAttachee++;
                                            }
                                        }
                                    }
                                    if (weaponsOnAttachee >= 2) {
                                        // Look for unarmed friendly at same site
                                        PhysicalCard attacheeLocation = attachee.getAtLocation();
                                        boolean unarmedBuddyExists = false;
                                        String buddyTitle = null;
                                        if (attacheeLocation != null) {
                                            java.util.Collection<PhysicalCard> sameSiteCards =
                                                gameState.getCardsAtLocation(attacheeLocation);
                                            if (sameSiteCards != null) {
                                                for (PhysicalCard sc : sameSiteCards) {
                                                    if (sc == null || sc.getBlueprint() == null) continue;
                                                    if (sc == attachee) continue;
                                                    if (!context.getPlayerId().equals(sc.getOwner())) continue;
                                                    if (sc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                    // Check if this char is unarmed
                                                    boolean scArmed = false;
                                                    java.util.List<PhysicalCard> scAtts = gameState.getAttachedCards(sc);
                                                    if (scAtts != null) {
                                                        for (PhysicalCard sa : scAtts) {
                                                            if (sa != null && sa.getBlueprint() != null
                                                                    && sa.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                                                                scArmed = true;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (!scArmed) {
                                                        unarmedBuddyExists = true;
                                                        buddyTitle = sc.getTitle();
                                                        break;
                                                    }
                                                }
                                            }
                                        }
                                        if (unarmedBuddyExists) {
                                            v67anBonus = 500.0f;
                                            v67anReason = String.format(
                                                "V72 REDISTRIBUTE: %s has %d weapons; transfer one to unarmed buddy '%s' at same site",
                                                attachee.getTitle(), weaponsOnAttachee, buddyTitle);
                                        }
                                    }
                                } catch (Exception e) { logger.debug("V72 redistribute check error: {}", e.getMessage()); }
                            }
                        }
                    }
                } catch (Exception e) { logger.debug("V67an error: {}", e.getMessage()); }
                action.addReasoning("V67an WEAPON SWAP: " + v67anReason, v67anBonus);
                logger.warn("V67an WEAPON TRANSFER: '{}' → +{} ({})",
                    actionText, (int) v67anBonus, v67anReason);
            }

            // ========== Take Card Into Hand ==========
            // V192 (2026-07-06): reserve-deck takes now FALL THROUGH to the merged pull
            // scorer branch below. Non-reserve takes (V29.7 BOUNCE class,
            // lost/used/force pile) keep routing here.
            else if (actionText.contains("Take") && actionText.contains("into hand")
                     && !textLower.contains("reserve deck") && !textLower.contains("[upload]")) {
                evaluateTakeIntoHand(action, context, actionText, textLower);
            }

            // ========== Prevent Battle/Move (Barrier Cards) ==========
            else if (actionText.contains("Prevent") && actionText.contains("from battling or moving")) {
                evaluateBarrier(action, context, actionText);
            }

            // ========== Monnok-type (Reveal Hand) ==========
            else if (actionText.contains("LOST: Reveal opponent's hand")) {
                int theirHandSize = gameState != null ? gameState.getHand(context.getOpponentId()).size() : 0;
                controlLedger.register(ControlActionPolicy.revealOpponentHand(
                        actionId, theirHandSize));
                PolicyOperationAdapter.apply(action, controlLedger);
            }

            // ========== Dangerous Cards ==========
            else if (textLower.contains("stardust") || textLower.contains("on the edge")) {
                controlLedger.register(ControlActionPolicy.dangerousCard(actionId));
                PolicyOperationAdapter.apply(action, controlLedger);
            }

            // ========== Draw Card Into Hand ==========
            else if (actionText.equals("Draw card into hand from Force Pile")) {
                action.setActionType(ActionType.DRAW);
                action.addReasoning("Draw option (see DrawEvaluator)", 0.0f);
            }

            // ═══════════════════════════════════════════════════════════
            // ═══ REGION: MOVE — movement guards + dispatch (reorg 2026-07-06) ═══
            // Owns: V67ae 'move to here' drain guard (-300) + the Movement Actions dispatch below (V35.4 spy-flee,
            // landspeed/shuttle scoring). The full stay/flee/hunt ladder lives in MoveEvaluator (V136<->V137 parity).
            // NOTE: the V79 parse in MoveEvaluator is INERT; live parsec steering = V79b in RandoCalAi
            // (+ the V103 fallback near line ~1216 in this file).
            // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
            // Cross-refs: MOVE (MoveEvaluator), SVC-SAFETY (V169 endangered movers), CONTROL (drain-before-move
            // interleave: moving a participant first forfeits that card's drain). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
            // ═══════════════════════════════════════════════════════════
            // ========== V67ae: GAME-TEXT 'MOVE TO HERE' DRAIN GUARD ==========
            // Steve's report: Rando moved Vader from CC Lower Corridor (3-drain
            // battleground) to Mustafar: Vader's Castle (0 drain) using Castle's
            // 'may move character to here' game-text action. V67g MOVE-FROM-DRAIN
            // didn't fire because that's wired to landspeed/CardSelectionEvaluator,
            // not card-action moves through ActionTextEvaluator.
            //
            // Rule: if the destination site has zero drain potential (no opp
            // icons) AND it's a 'move <character> to here' action, penalize. The
            // 'free move' attractiveness shouldn't outweigh losing drain pressure.
            else if (MoveDrainRoutingPolicy.isMoveToHereAction(textLower)) {
                action.setActionType(ActionType.MOVE);
                if (cardId != null && gameState != null && context.getPlayerId() != null) {
                    try {
                        PhysicalCard srcLoc = gameState.findCardById(Integer.parseInt(cardId));
                        if (srcLoc != null && srcLoc.getBlueprint() != null) {
                            // The action source is the destination site itself.
                            String oppId = gameState.getOpponent(context.getPlayerId());
                            int destOppIcons = 0;
                            try {
                                if (context.getSide() == com.gempukku.swccgo.common.Side.LIGHT) {
                                    destOppIcons = srcLoc.getBlueprint().getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE);
                                } else {
                                    destOppIcons = srcLoc.getBlueprint().getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                }
                            } catch (Exception e) { /* ignore */ }

                            if (destOppIcons == 0) {
                                // V67ae ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, T5: Lando
                                // trapped at 6v11-armed Lower Corridor; the only escape was taxed -300
                                // and Pass won): mirror V33 BUDDY BREAK's "hopelessly outgunned →
                                // allow retreat" exemption (MoveEvaluator: gap >= 6 = site doomed).
                                // Location-sourced move actions never reach MoveEvaluator's threat
                                // tiers, so the exemption must live HERE: if ANY friendly-occupied
                                // location (other than this destination) is doomed, a retreat to a
                                // 0-drain site is legitimate — skip the penalty.
                                boolean v67aeRetreatExempt = false;
                                String v67aeDoomedLoc = null;
                                try {
                                    if (context.getGame() != null) {
                                        // ADJUSTED 2026-07-10b (Codex m00137 hole 2 + m00128): (a) scope the
                                        // scan to the DESTINATION's system — the mover of a location-sourced
                                        // "move to here" comes from a related site, so an unrelated doomed
                                        // site must not exempt this move; (b) weapon-adjust the enemy side
                                        // (raw 6v8 hid the armed 6v11+ reality — same V29.7 heuristic).
                                        String v67aeDestSys = srcLoc.getPartOfSystem();
                                        for (PhysicalCard rl : gameState.getTopLocations()) {
                                            if (rl == null || rl.getCardId() == srcLoc.getCardId()) continue;
                                            if (v67aeDestSys != null && rl.getPartOfSystem() != null
                                                    && !v67aeDestSys.equals(rl.getPartOfSystem())) continue;
                                            float rOur = context.getGame().getModifiersQuerying()
                                                .getTotalPowerAtLocation(gameState, rl, context.getPlayerId(), false, false);
                                            if (rOur <= 0) continue;
                                            float rOpp = context.getGame().getModifiersQuerying()
                                                .getTotalPowerAtLocation(gameState, rl, oppId, false, false);
                                            try {
                                                for (PhysicalCard rc : gameState.getCardsAtLocation(rl)) {
                                                    if (rc == null || rc.getBlueprint() == null) continue;
                                                    if (rc.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;
                                                    if (!oppId.equals(rc.getOwner())) continue;
                                                    java.util.List<PhysicalCard> rAtts = gameState.getAttachedCards(rc);
                                                    if (rAtts != null) {
                                                        for (PhysicalCard att : rAtts) {
                                                            if (att == null || att.getBlueprint() == null) continue;
                                                            if (att.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.WEAPON) {
                                                                String wt = att.getTitle() != null ? att.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                                rOpp += wt.contains("lightsaber") ? 5.0f : 3.0f;
                                                            }
                                                        }
                                                    }
                                                    String rgt = rc.getBlueprint().getGameText();
                                                    if (rgt != null && rgt.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                                                        String rct = rc.getTitle() != null ? rc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                        rOpp += rct.contains("lightsaber") ? 5.0f : 3.0f;
                                                    }
                                                }
                                            } catch (Exception we) { /* raw power */ }
                                            if (rOpp - rOur >= 6f) {
                                                v67aeRetreatExempt = true;
                                                v67aeDoomedLoc = rl.getTitle();
                                                break;
                                            }
                                        }
                                    }
                                } catch (Exception e) { /* fail-open: no exemption */ }
                                MoveDrainRoutingPolicy.MoveToHereDrain v67aeDrain =
                                    MoveDrainRoutingPolicy.moveToHereDrain(
                                        srcLoc.getTitle(), destOppIcons,
                                        v67aeRetreatExempt, v67aeDoomedLoc);
                                action.addReasoning(
                                    v67aeDrain.contribution().reason(),
                                    v67aeDrain.contribution().delta());
                                if (v67aeDrain.branch()
                                        == MoveDrainRoutingPolicy.MoveToHereDrainBranch.RETREAT_EXEMPT) {
                                    logger.warn("V67ae RETREAT EXEMPT: doomed={} dest={} — skipping -300",
                                        v67aeDoomedLoc, srcLoc.getTitle());
                                } else if (v67aeDrain.branch()
                                        == MoveDrainRoutingPolicy.MoveToHereDrainBranch.ZERO_DRAIN_PENALTY) {
                                    logger.warn("V67ae MOVE-TO-NON-DRAIN: action='{}' dest={} 0-drain — penalize free retreat (-300)",
                                        actionText, srcLoc.getTitle());
                                }
                            }
                        }
                    } catch (Exception e) { logger.debug("V67ae error: {}", e.getMessage()); }
                }
                action.addReasoning("V67ae move-to-here action — see drain analysis", 0.0f);
            }

            // ========== Movement Actions ==========
            else if (actionText.contains("Move using") || actionText.contains("Shuttle") ||
                     actionText.contains("Docking bay transit") || actionText.contains("Transport")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Movement option (see MoveEvaluator)", 0.0f);

                // === V35.4: BOOST MOVEMENT WHEN ENEMY SPY/PRESENCE BLOCKS OUR DRAIN ===
                // If our character is at ANY location where an opponent (including undercover spy)
                // has presence, our force drain is blocked. Moving away lets us drain elsewhere.
                // Undercover spies deploy on OUR side but count as opponent presence!
                // V35.4 UPDATED 2026-07-06 (audit row move-7): two fixes —
                //   1. OWNERSHIP: an opponent undercover spy is owner == OPPONENT &&
                //      isUndercover (the engine never flips owner on undercover). The old
                //      test flagged OUR OWN spy (owner == us) as the "opponent spy", so our
                //      V170 drain-block spy paid +250 to EVERY move action on the table,
                //      including its own move-away (fighting V53 SPY STAY -300 and losing).
                //   2. SCOPE: bonus only for actions whose MOVER (this action's cardId) is
                //      at the blocked location, and never for an undercover mover (V53/V170
                //      doctrine: the spy stays put). If the mover can't be resolved, fall
                //      back to the old any-location scan (keeps the rule alive).
                if (gameState != null && context.getPlayerId() != null) {
                    try {
                        String opponentId = gameState.getOpponent(context.getPlayerId());
                        // Resolve this action's mover and its current location (may stay null)
                        PhysicalCard v354Mover = null;
                        PhysicalCard v354MoverLoc = null;
                        if (cardId != null) {
                            try {
                                v354Mover = gameState.findCardById(Integer.parseInt(cardId));
                            } catch (NumberFormatException nfe) { /* temp id — mover unknown */ }
                        }
                        if (v354Mover != null) {
                            v354MoverLoc = v354Mover.getAtLocation();
                            if (v354MoverLoc == null && v354Mover.getAttachedTo() != null) {
                                v354MoverLoc = v354Mover.getAttachedTo().getAtLocation();
                            }
                        }
                        boolean v354MoverIsUndercover = v354Mover != null && v354Mover.isUndercover();
                        // V35.4 UPDATED 2026-07-07 (VERGE post-flip fix, Game9f3c46b00681): the mover
                        // can be a mobile-system LOCATION card (the Death Star). 'Move away to drain
                        // elsewhere' is character/starship doctrine — a location neither drains nor
                        // unblocks a drain by moving, yet this +150 attached to the Death Star's
                        // hyperspeed move at T4/T5 and cemented the pointless orbit-exit toggles
                        // (boundary: with V79's +500 orbit-gated, +40 base +150 would still beat
                        // Pass ~+28). Skip location movers entirely.
                        boolean v354MoverIsLocation = v354Mover != null && v354Mover.getBlueprint() != null
                            && v354Mover.getBlueprint().getCardCategory() == CardCategory.LOCATION;
                        if (MoveDrainRoutingPolicy.allowsBlockedDrainEscapeMover(
                                v354MoverIsUndercover, v354MoverIsLocation)) {
                            for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                                if (loc == null || loc.getTitle() == null) continue;
                                // Scope to the mover's own location when we know it
                                if (v354MoverLoc != null && loc.getCardId() != v354MoverLoc.getCardId()) continue;

                                boolean weHavePresence = false;
                                boolean oppHasPresence = false;
                                boolean oppHasUndercoverSpy = false;
                                for (com.gempukku.swccgo.game.PhysicalCard card : gameState.getCardsAtLocation(loc)) {
                                    if (card == null) continue;
                                    if (context.getPlayerId().equals(card.getOwner())) {
                                        weHavePresence = true;
                                    } else if (opponentId != null && opponentId.equals(card.getOwner())) {
                                        oppHasPresence = true;
                                        // V35.4 UPDATED 2026-07-06: opponent spy = OPPONENT-owned
                                        // undercover card at a location we occupy
                                        if (card.isUndercover()) {
                                            oppHasUndercoverSpy = true;
                                        }
                                    }
                                }

                                // If opponent has presence (or undercover spy) at our location, drain is blocked
                                MoveDrainRoutingPolicy.BlockedDrainEscape v354Escape =
                                    MoveDrainRoutingPolicy.blockedDrainEscape(
                                        loc.getTitle(), weHavePresence,
                                        oppHasPresence, oppHasUndercoverSpy);
                                if (v354Escape.contribution().applies()) {
                                    action.addReasoning(
                                        v354Escape.contribution().reason(),
                                        v354Escape.contribution().delta());
                                    logger.warn("V35.4: {} at {} blocking our drain — boosting movement (+{})",
                                        v354Escape.undercoverSpyBlock()
                                            ? "UNDERCOVER SPY" : "Enemy",
                                        loc.getTitle(),
                                        (int)v354Escape.contribution().delta());
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V35.4: Error checking spy-blocked sites: {}", e.getMessage());
                    }
                }

                // === V29.7: VADER'S CASTLE RETREAT PENALTY ===
                // Mustafar: Vader's Castle can teleport Vader back to Mustafar.
                // This is TERRIBLE when Vader is at a location where he can force drain!
                // Mustafar has 0 opponent icons = no drain value. Moving Vader there
                // means losing a turn of draining at the current location.
                // This arm only prices lost drain. Separate retreat and danger rules
                // retain their own scoring and veto boundaries.
                if (MoveDrainRoutingPolicy.isVaderCastleRetreatAction(
                        textLower)) {
                    try {
                        // Find Vader's current location and check drain potential
                        String pid = context.getPlayerId();
                        if (gameState != null && pid != null) {
                            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                                if (card == null || !pid.equals(card.getOwner())) continue;
                                com.gempukku.swccgo.common.Zone zone = card.getZone();
                                if (zone == null || !zone.isInPlay()) continue;
                                String cTitle = card.getTitle();
                                if (cTitle == null || !cTitle.toLowerCase(java.util.Locale.ROOT).contains("vader")) continue;
                                if (card.getBlueprint() == null || card.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.CHARACTER) continue;

                                // Found Vader — check his current location
                                PhysicalCard vaderLoc = card.getAtLocation();
                                if (vaderLoc == null && card.getAttachedTo() != null) {
                                    // Vader might be aboard a vehicle/starship — get the vehicle's location
                                    vaderLoc = card.getAttachedTo().getAtLocation();
                                }
                                if (vaderLoc != null && vaderLoc.getTitle() != null) {
                                    String vLocTitle = vaderLoc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                    if (MoveDrainRoutingPolicy.isMustafarLocation(
                                            vLocTitle)) {
                                        // Vader is already at Mustafar — this is a move OUT, which is fine
                                        break;
                                    }
                                    // Vader is at a non-Mustafar location — check if it has drain value
                                    SwccgCardBlueprint locBp = vaderLoc.getBlueprint();
                                    if (locBp != null) {
                                        int oppIcons = 0;
                                        if (context.getSide() == Side.DARK) {
                                            oppIcons = locBp.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                        } else {
                                            oppIcons = locBp.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE);
                                        }
                                        MoveDrainRoutingPolicy.Contribution v297Retreat =
                                            MoveDrainRoutingPolicy.vaderCastleRetreat(
                                                vaderLoc.getTitle(), oppIcons);
                                        if (v297Retreat.applies()) {
                                            // Vader is at a location with drain value — DON'T retreat!
                                            action.addReasoning(
                                                v297Retreat.reason(),
                                                v297Retreat.delta());
                                            logger.warn("V29.7 VADER RETREAT BLOCKED: Vader at {} with {} drain — retreating to Mustafar is terrible! (-300)",
                                                vaderLoc.getTitle(), oppIcons);
                                        }
                                    }
                                }
                                break; // Found Vader, done
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V29.7: Error checking Vader retreat: {}", e.getMessage());
                    }
                }
            }
            else if (actionText.equals("Take off") || actionText.equals("Land")) {
                action.setActionType(ActionType.MOVE);
                action.addReasoning("Take off/Land option (see MoveEvaluator)", 0.0f);
            }

            // ========== Make Opponent Lose Force ==========
            else if (actionText.contains("Make opponent lose")) {
                controlLedger.register(ControlActionPolicy.makeOpponentLose(actionId));
                PolicyOperationAdapter.apply(action, controlLedger);
            }

            // ========== V29.7: Deploy Docking Bay — Smart Strategy ==========
            // Docking bays are SHARED — opponent can deploy characters to YOUR docking bays!
            // Only deploy a docking bay if we don't already have empty ones on the table.
            // Empty docking bays = free locations for the opponent.
            else if (actionText.contains("Deploy docking bay") || textLower.contains("deploy a docking bay")) {
                int emptyBayCount = 0;
                int totalOurBays = 0;
                GameState bayGs = context.getGameState();
                String bayPlayerId = context.getPlayerId();
                if (bayGs != null && bayPlayerId != null) {
                    try {
                        for (PhysicalCard loc : bayGs.getTopLocations()) {
                            if (loc == null || loc.getTitle() == null) continue;
                            String locTitle = loc.getTitle().toLowerCase(java.util.Locale.ROOT);
                            // Check if this is a docking bay we own
                            if (locTitle.contains("docking bay") || locTitle.contains("landing platform")) {
                                // Check if we control it (our card)
                                if (bayPlayerId.equals(loc.getOwner())) {
                                    totalOurBays++;
                                    // Check if any of OUR characters are there
                                    boolean hasFriendlyChar = false;
                                    java.util.List<PhysicalCard> cardsHere = bayGs.getCardsAtLocation(loc);
                                    if (cardsHere != null) {
                                        for (PhysicalCard pc : cardsHere) {
                                            if (pc != null && bayPlayerId.equals(pc.getOwner())
                                                && pc.getBlueprint() != null
                                                && pc.getBlueprint().getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                                hasFriendlyChar = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!hasFriendlyChar) {
                                        emptyBayCount++;
                                    }
                                }
                            }
                        }
                    } catch (Exception e) { /* ignore */ }
                }

                applyDeployActionTextPolicy(action,
                    DeployActionTextPolicy.scoreDockingBay(
                        new DeployActionTextFacts.DockingBayFacts(
                            actionId, emptyBayCount, totalOurBays)));
            }

            // ========== V25: HUNT DOWN V — VADER CASTLE DEPLOY ACTION ==========
            // If the action deploys Vader from Reserve Deck (via Vader's Castle), and
            // Hunt Down V is the objective, this is THE most important action in the game.
            // Vader must be on table for the deck to function.
            else if ((actionText.contains("Deploy Vader from Reserve Deck")
                    || actionText.contains("Deploy Vader here"))
                    && isVadersCastleActionSource(gameState, cardId)) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer vaderObjAnalyzer =
                    context.getObjectiveAnalyzer();
                boolean objectiveAnalyzed = vaderObjAnalyzer != null
                    && vaderObjAnalyzer.isAnalyzed();
                boolean huntDownVActive = objectiveAnalyzed
                    && vaderObjAnalyzer.isHuntDownV();
                boolean vaderOnTable = false;
                boolean legalVaderDeploy = false;
                boolean preservesCastleMoveForce = false;
                int forceAvailable = 0;
                if (huntDownVActive && context.getGame() != null
                        && context.getGame().getGameState() != null) {
                    com.gempukku.swccgo.game.state.GameState vaderGs =
                        context.getGame().getGameState();
                    vaderOnTable = vaderObjAnalyzer.isVaderOnTable(
                        vaderGs, context.getPlayerId());
                    legalVaderDeploy =
                        vaderObjAnalyzer.hasLegalVaderCastleDeployInReserve(
                            context.getGame(), context.getPlayerId());
                    preservesCastleMoveForce =
                        vaderObjAnalyzer
                            .hasVaderCastleDeployWithMoveReserve(
                                context.getGame(),
                                context.getPlayerId());
                    forceAvailable = vaderGs.getForcePileSize(context.getPlayerId());
                }
                applyDeployActionTextPolicy(action,
                    DeployActionTextPolicy.scoreVaderCastle(
                        new DeployActionTextFacts.VaderCastleFacts(
                            actionId, objectiveAnalyzed, huntDownVActive,
                            vaderOnTable, legalVaderDeploy,
                            preservesCastleMoveForce,
                            forceAvailable)));
            }

            // ========== V26/V29.6: Dining Room — Deploy Lando (TDIGWATT) ==========
            // Dining Room's game text deploys Lando from Reserve Deck — a key TDIGWATT piece.
            // DeployEvaluator can't find the card (it's in reserve, not hand), so we boost
            // here in ActionTextEvaluator.
            //
            // V29.6 FIX: Check if Lando would be ALONE at Dining Room. If no friendly
            // characters are already there, deploying Lando alone is suicide — opponent
            // will drop a character + weapon and kill him immediately. Defer until we
            // have a buddy at Dining Room first.
            else if ((textLower.contains("dining room") || textLower.contains("deploy lando"))
                     && textLower.contains("reserve")) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer drLandoAnalyzer =
                    context.getObjectiveAnalyzer();

                // V29.6: Check if there are friendly characters at Dining Room
                int friendlyCountAtDR = 0;
                try {
                    GameState drGameState = context.getGameState();
                    String drPlayerId = context.getPlayerId();
                    if (drGameState != null && drPlayerId != null) {
                        // Find Dining Room on the table
                        java.util.List<PhysicalCard> allLocs = drGameState.getTopLocations();
                        PhysicalCard diningRoomCard = null;
                        if (allLocs != null) {
                            for (PhysicalCard loc : allLocs) {
                                if (loc != null && loc.getTitle() != null
                                    && loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("dining room")) {
                                    diningRoomCard = loc;
                                    break;
                                }
                            }
                        }
                        if (diningRoomCard != null) {
                            java.util.List<PhysicalCard> cardsAtDR = drGameState.getCardsAtLocation(diningRoomCard);
                            if (cardsAtDR != null) {
                                for (PhysicalCard c : cardsAtDR) {
                                    if (c != null && drPlayerId.equals(c.getOwner())
                                        && c.getBlueprint() != null
                                        && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        friendlyCountAtDR++;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.debug("V29.6 DINING ROOM: Error checking friendlies at DR: {}", e.getMessage());
                }

                boolean diningObjectiveAnalyzed = drLandoAnalyzer != null
                    && drLandoAnalyzer.isAnalyzed();
                boolean needsBespinPresence = diningObjectiveAnalyzed
                    && drLandoAnalyzer.needsBespinSystemPresence();
                applyDeployActionTextPolicy(action,
                    DeployActionTextPolicy.scoreDiningRoomLando(
                        new DeployActionTextFacts.DiningRoomLandoFacts(
                            actionId, diningObjectiveAnalyzed,
                            needsBespinPresence, friendlyCountAtDR)));
            }

            // ========== Deploy From Reserve (Risky) ==========
            // V114 (Steve, 2026-05-21): DELETED the generic "Deploy ... from ..." catch-all.
            // It assigned -10 to ALL "Deploy X from Y" action texts before the V60/V67ai
            // block (line 3120) could award the +2000 OBJECTIVE-tier location-pull bonus.
            // This caused Rando to IGNORE Hunt Down V's once-per-turn "Deploy a [Cloud City]
            // or Malachor battleground site from Reserve Deck" every single turn in
            // replay dc8n6dl9s88rqycz (2026-05-12). Same bug affected EVERY non-specific
            // reserve/lost/used/stacked-pile deploy action.
            //
            // Per Steve: pull actions are net positive — they thin the deck, bring value
            // into play, and are usually free or low-cost (feedback_reserve_deck_pulls.md).
            // No "Deploy X from Y" action deserves an unconditional -10. Specific bad
            // cases are already handled upstream (V67u Force Push exchange, V35.2 weapon
            // rack outside battle, etc.). Letting these actions fall through to V60/V67ai
            // gives the correct +150 baseline plus +2000 objective bonus for Hunt Down V
            // and similar location/site/character pulls.
            // (Mirrored in chosenone ActionTextEvaluator.java)

            // ========== Embark ==========
            else if (actionText.contains("Embark")) {
                action.setActionType(ActionType.MOVE);
                evaluateEmbark(action, context, actionText, cardId);
            }

            // ========== Disembark/Relocate/Transfer ==========
            else if (actionText.contains("Disembark") || actionText.contains("Relocate") ||
                     actionText.contains("Transfer")) {
                action.setActionType(ActionType.MOVE);
                MoveTransitPolicy.Contribution residualTransfer =
                    MoveTransitPolicy.residualTransfer();
                action.addReasoning(
                    residualTransfer.reason(), residualTransfer.delta());
            }

            // ========== Ship-dock ==========
            else if (actionText.contains("Ship-dock")) {
                MoveTransitPolicy.Contribution shipDock =
                    MoveTransitPolicy.shipDock();
                action.addReasoning(shipDock.reason(), shipDock.delta());
            }

            // ========== Place in Lost Pile ==========
            else if (actionText.contains("Place in Lost Pile")) {
                applyForceLossActionTextPolicy(action,
                        ForceLossPolicy.scoreActionTextChoice(actionId,
                                ForceLossPolicy.ActionTextChoice.PLACE_IN_LOST_PILE));
            }

            // ========== Grab ==========
            else if (actionText.contains("Grab")) {
                evaluateGrab(action, context, actionText);
            }

            // ========== Break Cover ==========
            else if (actionText.contains("Break cover")) {
                evaluateBreakCover(action, context, actionText);
            }

            // ========== Retrieve Force ==========
            else if (textLower.contains("retrieve") || actionText.contains("Place out of play to retrieve")) {
                int lostPileSize = gameState != null ? gameState.getLostPile(context.getPlayerId()).size() : 0;
                controlLedger.register(ControlActionPolicy.retrieve(
                        actionId, lostPileSize));
                PolicyOperationAdapter.apply(action, controlLedger);
            }

            // ========== Defensive Shields ==========
            // V29.1: Shield pacing — don't burn all 4 shield slots immediately.
            // Play 2 shields on turn 1 to get basic protection, then WAIT to see
            // what the opponent is running before committing the remaining slots.
            // This lets us pick targeted counters instead of generic shields.
            else if (actionText.contains("Play a Defensive Shield")) {
                ShieldStrategy shieldStrategy = context.getShieldStrategy();
                int turnNumber = context.getTurnNumber();
                boolean pacingCap = context.isMyTurn() && shieldStrategy != null
                        && shieldStrategy.atPacingCap(turnNumber);
                controlLedger.register(ShieldPolicy.defensiveShieldWindow(
                        actionId, context.isMyTurn(), pacingCap, turnNumber));
                PolicyOperationAdapter.apply(action, controlLedger);
            }

            // ========== Deploy on table/location ==========
            else if (actionText.startsWith("Deploy on")) {
                DeployActionTextFacts.GenericDeployKind kind =
                    textLower.contains("projection") && textLower.contains("side")
                        ? DeployActionTextFacts.GenericDeployKind.PROJECTION_ON_SIDE
                        : DeployActionTextFacts.GenericDeployKind.DEPLOY_ON;
                applyDeployActionTextPolicy(action,
                    DeployActionTextPolicy.scoreGenericDeploy(
                        new DeployActionTextFacts.GenericDeployFacts(
                            actionId, kind)));
            }

            // ========== Deploy unique ==========
            else if (actionText.startsWith("Deploy unique")) {
                applyDeployActionTextPolicy(action,
                    DeployActionTextPolicy.scoreGenericDeploy(
                        new DeployActionTextFacts.GenericDeployFacts(
                            actionId,
                            DeployActionTextFacts.GenericDeployKind.DEPLOY_UNIQUE)));
            }

            // ========== USED: Peek at top ==========
            else if (actionText.startsWith("USED: Peek at top")) {
                controlLedger.register(ControlActionPolicy.peekAtTop(actionId));
                PolicyOperationAdapter.apply(action, controlLedger);
            }

            // ========== Force Drain Cancellation ==========
            else if (actionText.contains("Cancel Force drain")) {
                ResponsePolicy.CancelEvaluation cancelEvaluation =
                    ResponsePolicy.scoreLateForceDrainCancel(
                        actionId, context.isMyTurn());
                if (cancelEvaluation.delegatesSelfCancelDrain()) {
                    controlLedger.register(ControlActionPolicy.selfCancelDrain(actionId,
                            "V52 NEVER SELF-CANCEL: Don't cancel own force drain!"));
                    PolicyOperationAdapter.apply(action, controlLedger);
                    logger.warn("V52 SELF-CANCEL BLOCKED: Cancel Force drain on own turn — HARD BLOCKED!");
                } else {
                    applyResponsePolicy(action, cancelEvaluation.result());
                }
            }

            // ========== V74: Maintenance Cost Satisfaction (replaces V22.3) ==========
            // When a maintenance card's upkeep is due, Rando gets a choice:
            //   "Use X Force"          (pay — KEEP the card)
            //   "Lose X Force ... Used Pile" (recyclable — keep blueprint, lose card from table)
            //   "Place out of play"   (PERMANENT loss — worst option)
            //
            // V22.3's old check applied to the ACTION text, which is short
            // ("Use 1 Force" / "Place out of play") and never contains
            // "maintenance" — so V22.3 never fired. Replay May 15 showed Rando
            // picking "Place out of play" for Lando every turn (4 times).
            //
            // V74 fix: detect maintenance context from the DECISION text
            // (which DOES contain "maintenance"), then score each action's
            // OWN text accordingly.
            else if (context.getDecisionText() != null
                     && context.getDecisionText().toLowerCase(java.util.Locale.ROOT)
                        .contains("maintenance")) {
                if (textLower.contains("use ") && textLower.contains(" force")) {
                    // PAY option — strongly prefer
                    applyForceLossActionTextPolicy(action,
                            ForceLossPolicy.scoreActionTextChoice(actionId,
                                    ForceLossPolicy.ActionTextChoice.MAINTENANCE_PAY));
                    logger.warn("V74 MAINTENANCE PAY: '{}' → +400", actionText);
                } else if (textLower.contains("out of play")) {
                    // PERMANENT LOSS — avoid heavily
                    applyForceLossActionTextPolicy(action,
                            ForceLossPolicy.scoreActionTextChoice(actionId,
                                    ForceLossPolicy.ActionTextChoice.MAINTENANCE_OUT_OF_PLAY));
                    logger.warn("V74 MAINTENANCE SACRIFICE: '{}' → -800", actionText);
                } else if (textLower.contains("lose ") && textLower.contains(" force")
                           && (textLower.contains("used pile") || textLower.contains("place in used"))) {
                    // Recyclable — better than out-of-play but worse than paying
                    applyForceLossActionTextPolicy(action,
                            ForceLossPolicy.scoreActionTextChoice(actionId,
                                    ForceLossPolicy.ActionTextChoice.MAINTENANCE_USED_PILE));
                    logger.warn("V74 MAINTENANCE USED-PILE: '{}' → -200", actionText);
                } else if (textLower.contains("sacrifice")) {
                    applyForceLossActionTextPolicy(action,
                            ForceLossPolicy.scoreActionTextChoice(actionId,
                                    ForceLossPolicy.ActionTextChoice.MAINTENANCE_SACRIFICE));
                    logger.warn("V74 MAINTENANCE SACRIFICE: '{}' → -800", actionText);
                }
            }

            // ========== Use/Lose Force Actions ==========
            else if (textLower.startsWith("use ") && textLower.contains(" force ")) {
                // V22.3: Check if this might be a maintenance payment
                // Maintenance decisions often just say "Use X Force" without "maintenance" keyword
                // If the decision context involves a maintenance card, prefer paying
                if (textLower.contains("cost") || textLower.contains("upkeep")) {
                    applyForceLossActionTextPolicy(action,
                            ForceLossPolicy.scoreActionTextChoice(actionId,
                                    ForceLossPolicy.ActionTextChoice.GENERIC_USE_UPKEEP));
                    logger.warn("V22.3 MAINTENANCE: Likely upkeep payment - '{}'", actionText);
                } else {
                    // V24.5: No randomness — generic use force should be avoided
                    applyForceLossActionTextPolicy(action,
                            ForceLossPolicy.scoreActionTextChoice(actionId,
                                    ForceLossPolicy.ActionTextChoice.GENERIC_USE));
                }
            }
            else if (textLower.startsWith("lose ") && textLower.contains(" force ")) {
                // V24.5: No randomness — losing force is almost always bad
                applyForceLossActionTextPolicy(action,
                        ForceLossPolicy.scoreActionTextChoice(actionId,
                                ForceLossPolicy.ActionTextChoice.GENERIC_LOSE));
            }
            // V22.3: Catch generic sacrifice options that aren't tagged as maintenance
            else if (textLower.contains("sacrifice") || textLower.contains("place out of play")) {
                applyForceLossActionTextPolicy(action,
                        ForceLossPolicy.scoreActionTextChoice(actionId,
                                ForceLossPolicy.ActionTextChoice.GENERIC_SACRIFICE));
                logger.info("V22.3 SACRIFICE PENALTY: '{}'", actionText);
            }

            // ========== V22.5: Alert My Star Destroyer / Ship Deployment Priority ==========
            // "Alert My Star Destroyer" deploys Executor + pilot for cheap.
            // This is CRITICAL for TDIGWATT — Bespin system occupation enables Dark Deal
            // and Cloud City Occupation, which are the deck's primary damage engines.
            else if (textLower.contains("reveal") && (textLower.contains("star destroyer") || textLower.contains("pilot"))) {
                // Check if we have a ship at Bespin system already
                boolean hasBespinShip = false;
                if (gameState != null) {
                    try {
                        String pid = context.getPlayerId();
                        for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getLocationsInOrder()) {
                            if (loc != null && loc.getTitle() != null &&
                                loc.getTitle().toLowerCase(java.util.Locale.ROOT).contains("bespin") &&
                                loc.getBlueprint() != null && loc.getBlueprint().getCardSubtype() != null &&
                                loc.getBlueprint().getCardSubtype() == com.gempukku.swccgo.common.CardSubtype.SYSTEM) {
                                float ourPower = context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, loc, pid, false, false);
                                if (ourPower > 0) hasBespinShip = true;
                                break;
                            }
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                }
                applyDeployActionTextPolicy(action,
                    DeployActionTextPolicy.scoreBespinShip(
                        new DeployActionTextFacts.BespinShipFacts(
                            actionId, hasBespinShip)));
            }
            // V22.5: Generic "deploy simultaneously" or ship+pilot combos
            else if (textLower.contains("deploy") && textLower.contains("simultaneously")) {
                applyDeployActionTextPolicy(action,
                    DeployActionTextPolicy.scoreSimultaneousDeploy(
                        new DeployActionTextFacts.ActionFacts(actionId)));
            }

            // ========== V25: INITIATE BATTLE ==========
            // Battle initiation was previously unhandled (fell to default 0.0f) which
            // meant Rando NEVER chose to initiate battles because other actions always
            // outscored them. Now we evaluate the specific location's power differential.
            else if (actionText.contains("Initiate battle") || actionText.contains("initiate battle")) {
                action.setActionType(ActionType.BATTLE);
                boolean battleScored = false;
                String battleLocationTitle = "";
                float battleOurPower = 0.0f;
                float battleTheirPower = 0.0f;
                float battleOurAbility = 0.0f;
                float battleTheirAbility = 0.0f;

                SwccgGame battleGame = context.getGame();
                if (battleGame != null && context.getGame().getGameState() != null) {
                    com.gempukku.swccgo.game.state.GameState bGs = battleGame.getGameState();
                    String bPlayerId = context.getPlayerId();
                    String bOpponentId = bGs.getOpponent(bPlayerId);

                    if (bOpponentId != null) {
                        try {
                            PhysicalCard bLoc = BattleTargetResolver.resolve(
                                    bGs.getTopLocations(), cardId, actionText);
                            if (bLoc != null) {
                                battleLocationTitle = bLoc.getTitle();
                                battleOurPower = battleGame.getModifiersQuerying().getTotalPowerAtLocation(
                                    bGs, bLoc, bPlayerId, false, false);
                                battleTheirPower = battleGame.getModifiersQuerying().getTotalPowerAtLocation(
                                    bGs, bLoc, bOpponentId, false, false);
                                battleOurAbility = battleGame.getModifiersQuerying().getTotalAbilityAtLocation(
                                    bGs, bPlayerId, bLoc);
                                battleTheirAbility = battleGame.getModifiersQuerying().getTotalAbilityAtLocation(
                                    bGs, bOpponentId, bLoc);
                                float effectiveDiff = BattleActionTextPolicy.effectivePowerDifference(
                                        battleOurPower, battleTheirPower,
                                        battleOurAbility, battleTheirAbility);

                                logger.warn("V25 BATTLE EVAL at {}: our power={} ability={}, their power={} ability={}, effectiveDiff={}",
                                    battleLocationTitle, (int)battleOurPower, (int)battleOurAbility,
                                    (int)battleTheirPower, (int)battleTheirAbility, (int)effectiveDiff);
                                battleScored = true;
                            }
                        } catch (Exception e) {
                            logger.warn("V25 BATTLE: Error evaluating battle: {}", e.getMessage());
                        }
                    }
                }

                // Check reserve for destiny draws
                int battleReserve = 0;
                if (context.getGame() != null && context.getGame().getGameState() != null) {
                    battleReserve = context.getGame().getGameState().getReserveDeckSize(context.getPlayerId());
                }
                battleInitiationTextLedger.register(BattleActionTextPolicy.scoreInitiation(
                        new BattleActionTextFacts.InitiationFacts(
                                actionId, battleScored, battleLocationTitle,
                                battleOurPower, battleTheirPower,
                                battleOurAbility, battleTheirAbility,
                                battleReserve)));
                PolicyOperationAdapter.apply(action, battleInitiationTextLedger);

                logger.warn("V25 BATTLE: '{}' scored {}", actionText.length() > 60 ? actionText.substring(0,60) + "..." : actionText,
                    String.format("%.1f", action.getScore()));
            }

            // ========== V29.6/V29.11: BLASTER RACK — ONLY RACK TO SAVE WEAPONS FROM DYING CHARACTERS ==========
            // Blaster Rack stacks a weapon on it. This is ONLY useful at the END of a battle
            // when a character carrying the weapon has been HIT or is about to be forfeited
            // to satisfy attrition/battle damage. Proactively racking weapons outside of battle
            // damage resolution is terrible — it strips characters of weapons before they can fire.
            // Example: Vader had lightsaber, Rando racked it, Vader went to battle unarmed.
            // Action text can be "Stack character weapon" OR contain "rack" + "stack"
            else if ((textLower.contains("rack") && textLower.contains("stack"))
                || (textLower.contains("stack") && textLower.contains("character weapon"))) {
                Phase rackPhase = context.getPhase();
                // Check if we're in battle damage/attrition resolution
                // During battle damage, the decision text often references damage/attrition/forfeit
                boolean duringBattleDamage = false;
                try {
                    GameState rackGs = context.getGameState();
                    if (rackGs != null && rackGs.isDuringBattle()) {
                        // We're in a battle — check if damage is being resolved
                        // If the game is asking us to use rack during battle, it's likely
                        // because we're about to lose the character carrying the weapon.
                        duringBattleDamage = true;
                    }
                } catch (Exception e) {
                    logger.debug("V29.6 RACK: Error checking battle state: {}", e.getMessage());
                }

                if (duringBattleDamage) {
                    // V35.2: During battle — but ONLY rack weapons from characters AT the battle!
                    // Bug: Rando racked Vader's Lightsaber from Mustafar while battle was at Mos Eisley.
                    boolean weaponCharAtBattle = false;
                    try {
                        GameState rackGs2 = context.getGameState();
                        if (rackGs2 != null && rackGs2.getBattleState() != null) {
                            PhysicalCard battleLoc = rackGs2.getBattleState().getBattleLocation();
                            if (battleLoc != null) {
                                String rackPid = context.getPlayerId();
                                for (PhysicalCard tableCard : rackGs2.getAllPermanentCards()) {
                                    if (tableCard == null || !rackPid.equals(tableCard.getOwner())) continue;
                                    if (tableCard.getBlueprint() == null) continue;
                                    if (tableCard.getBlueprint().getCardCategory() != com.gempukku.swccgo.common.CardCategory.WEAPON) continue;
                                    com.gempukku.swccgo.common.Zone wz = tableCard.getZone();
                                    if (wz == null || !wz.isInPlay()) continue;
                                    String wTitle = tableCard.getTitle() != null ? tableCard.getTitle().toLowerCase(Locale.ROOT) : "";
                                    if (wTitle.isEmpty() || !textLower.contains(wTitle)) continue;
                                    PhysicalCard parentChar = tableCard.getAttachedTo();
                                    if (parentChar != null) {
                                        PhysicalCard charLoc = parentChar.getAtLocation();
                                        if (charLoc != null && charLoc == battleLoc) {
                                            weaponCharAtBattle = true;
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.debug("V35.2 RACK: Error checking weapon location: {}", e.getMessage());
                        weaponCharAtBattle = true; // Default to allow if check fails
                    }

                    if (weaponCharAtBattle) {
                        applyBattleWeaponsPolicy(action,
                            BattleWeaponsPolicy.scoreBlasterRack(
                                new BattleWeaponsFacts.BlasterRackFacts(
                                    actionId, true, true)));
                        logger.warn("V35.2 RACK: Weapon's character AT battle — saving '{}'", actionText);
                    } else {
                        applyBattleWeaponsPolicy(action,
                            BattleWeaponsPolicy.scoreBlasterRack(
                                new BattleWeaponsFacts.BlasterRackFacts(
                                    actionId, true, false)));
                        logger.warn("V35.2 RACK: BLOCKED — weapon's character not at battle! '{}'", actionText);
                    }
                } else {
                    // Outside battle — proactive racking is TERRIBLE
                    applyBattleWeaponsPolicy(action,
                        BattleWeaponsPolicy.scoreBlasterRack(
                            new BattleWeaponsFacts.BlasterRackFacts(
                                actionId, false, false)));
                    logger.warn("V29.6 BLASTER RACK: BLOCKED proactive racking outside battle — '{}'", actionText);
                }
            }

            // ========== V60 HIDDEN PATH TRANSIT — Underground Corridor game text ==========
            // "Move Jedi Survivor here to a site" is Underground Corridor's game-text action
            // that transits Jedi Survivors from Corridor to a Jabiim site or opponent's
            // battleground. This is THE action that flips the Hidden Path objective.
            // Previously scored 0.0 ("Unknown action type") while landspeed (which goes
            // backward to Safehouse) got +9999 from V53b. FIXES Issue #C from peaceful-pike.
            else if (MoveTransitPolicy.isPositiveHiddenPathTransitAction(
                    textLower)) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer hpTransit =
                    context.getObjectiveAnalyzer();
                boolean onHiddenPath = hpTransit != null && hpTransit.isAnalyzed()
                    && hpTransit.getObjectiveTitle() != null
                    && hpTransit.getObjectiveTitle().toLowerCase(Locale.ROOT).contains("hidden path");
                MoveTransitPolicy.Contribution v60Transit =
                    MoveTransitPolicy.positiveHiddenPathTransit(onHiddenPath);
                action.addReasoning(v60Transit.reason(), v60Transit.delta());
                if (onHiddenPath) {
                    logger.warn("V60 HIDDEN PATH TRANSIT: '{}' — +20000 (R4 band; CORRECT outward move, unlike landspeed)", actionText);
                }
            }

            // === REGION: PULL ===
            // V209 consolidates parent reserve-search guards, phase weighting, objective
            // key-character priority, DeckOracle facts, and forced-here formation safety.
            // Route recognition stays on the stock action text and source card id.
            else if (textLower.contains("[download]")
                     || (textLower.contains("from reserve deck")
                         && !textLower.contains("shuffle"))
                     || textLower.contains("[upload]")
                     || (textLower.contains("take")
                         && textLower.contains("into hand"))) {
                PullActionPolicy.Evaluation pull = PullActionPolicy.evaluateParent(
                        PullPolicyAdapter.readParent(
                                context, actionId, actionText, cardId));
                pullLedger.register(pull.result());
                PolicyOperationAdapter.apply(action, pullLedger);
            }

            // ========== Default/Unknown ==========
            else {
                action.addReasoning("Unknown action type", 0.0f);
                logger.trace("Unrecognized action: {}", actionText);
            }

            actions.add(action);
        }

        return actions;
    }

    // ========== Helper Methods ==========

    private void applyResponsePolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
            "response-action-text-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyBattleActionTextPolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
            "battle-action-text-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyBattleWeaponsPolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
            "battle-weapons-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyDeployActionTextPolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
            "deploy-action-text-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyPullSpecificActionPolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
            "pull-specific-action-text-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyPullActionPolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
            "pull-action-text-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyForceLossActionTextPolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
            "force-loss-action-text-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyNamedReserveSourcePolicy(
            EvaluatedAction action,
            PullSpecificActionFacts.ReserveSourceKind sourceKind,
            boolean targetAvailable) {
        applyPullSpecificActionPolicy(action,
            PullSpecificActionPolicy.scoreNamedReserveSource(
                new PullSpecificActionFacts.NamedReserveSource(
                    action.getActionId(), sourceKind, targetAvailable,
                    PullSpecificActionFacts.DuplicateState.NONE)));
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ REGION: CONTROL — force drain scoring (reorg 2026-07-06) ═══
    // Owns: drain go/no-go and sizing: V52-drain +50 drain-anyway (+100..300 multi-site), V48 early-turn
    // deferral -50, V189 net -1 drain budget gate (turn spend forecast), V104 / V24.15 zero-drain guards,
    // V140 ordering, V29.9 Hunt Down drain priority. Drain-before-move interleave rule: each card drains
    // once/turn — moving a participant first forfeits the drain.
    // KIND mix (CONTROL overall): 5 VETO / 4 BANDED / 1 ORDERING.
    // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
    // Cross-refs: MOVE (interleave), PLAYBOOKS (V24.x TDIGWATT drain rules), RESPONSE (the two
    // drain-response timings). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
    // ═══════════════════════════════════════════════════════════
    private void evaluateForceDrain(EvaluatedAction action, DecisionContext context,
                                    String locationCardId,
                                    PolicyContributionLedger controlLedger) {
        ControlDrainFacts facts = new ControlDrainFacts(
            context.getGameState(),
            context.getGame(),
            context.getPlayerId(),
            locationCardId,
            context.getTurnNumber(),
            () -> context.getStrategyController() != null
                && context.getStrategyController().isUnderBattleOrderRules(),
            () -> context.getObjectiveAnalyzer() != null
                && context.getObjectiveAnalyzer().isAnalyzed()
                && context.getObjectiveAnalyzer().isHuntDownV(),
            () -> context.getObjectiveAnalyzer() != null
                && context.getObjectiveAnalyzer().isAnalyzed()
                && context.getObjectiveAnalyzer()
                    .isClassicHuntDownObjective());
        controlLedger.register(ControlDrainAssessment.assess(action.getActionId(), facts));
        PolicyOperationAdapter.apply(action, controlLedger);
    }

    private void evaluatePlayCard(EvaluatedAction action, DecisionContext context) {
        int forcePile = context.getForcePileSize();
        applyDeployActionTextPolicy(action,
            DeployActionTextPolicy.scoreGenericPlayCard(
                new DeployActionTextFacts.PlayCardFacts(
                    action.getActionId(), forcePile)));
    }

    private void evaluateSenseCancel(EvaluatedAction action, DecisionContext context,
                                     String actionText,
                                     PolicyContributionLedger controlLedger) {
        String textLower = actionText.toLowerCase();
        boolean isDestinyBased = textLower.contains("draw destiny") || textLower.contains("if destiny");

        // V37.3: NEVER cancel your OWN interrupts!
        // Rando played FMFTD then Sensed his own FMFTD — self-sabotage.
        // Check if the interrupt being canceled was played by US.
        // Clue: if the action text mentions a card that we just played this turn,
        // or if we're the active player and the interrupt belongs to us.
        GameState senseGs = context.getGameState();
        if (senseGs != null) {
            try {
                String sensePid = context.getPlayerId();
                // Check if the interrupt target name matches one of OUR cards
                // Hunt Down specific: FMFTD, Force Lightning, Force Push are ours
                String[] ourInterrupts = {"far more frightening", "force lightning", "force push",
                    "stunning leader", "i have you now", "sniper", "dark strike",
                    "we must accelerate", "ghhhk", "force field", "no escape"};
                for (String ourInt : ourInterrupts) {
                    if (textLower.contains(ourInt)) {
                        applyResponsePolicy(action,
                            ResponsePolicy.scoreSenseSelfCancel(action.getActionId()));
                        logger.warn("V37.3 SENSE SELF-CANCEL: Tried to cancel our own '{}' — HARD BLOCKED!", ourInt);
                        return;
                    }
                }
            } catch (Exception e) { /* ignore */ }
        }

        // Check priority cards system for target value
        AiPriorityCards.SenseTargetResult senseResult = AiPriorityCards.getSenseTargetValue(actionText);

        ResponsePolicy.CancelEvaluation cancelEvaluation =
            ResponsePolicy.scoreSenseCancel(
                action.getActionId(), isDestinyBased,
                senseResult.isHighValue, senseResult.score,
                senseResult.matchedPattern,
                textLower.contains("force drain"), context.isMyTurn());
        if (cancelEvaluation.delegatesSelfCancelDrain()) {
            // V52: NEVER cancel your OWN force drain! Surprise Assault on own drain = self-sabotage.
            controlLedger.register(ControlActionPolicy.selfCancelDrain(action.getActionId(),
                    "V52 NEVER SELF-CANCEL DRAIN: Canceling own force drain is suicide!"));
            PolicyOperationAdapter.apply(action, controlLedger);
            logger.warn("V52 SELF-CANCEL BLOCKED: Tried to cancel OWN force drain — HARD BLOCKED!");
        } else {
            applyResponsePolicy(action, cancelEvaluation.result());
        }
    }

    private void evaluateHoujixGhhhk(EvaluatedAction action, DecisionContext context) {
        // These are CRITICAL survival cards
        // For now, give moderate positive score - ideally we'd check damage remaining
        applyResponsePolicy(action,
            ResponsePolicy.scoreRemainingBattleDamageCancel(
                action.getActionId()));

        // TODO: Add proper damage analysis when we have access to battle state
        // Check attrition/damage remaining and cards available to forfeit
    }

    private void evaluateTakeIntoHand(EvaluatedAction action, DecisionContext context, String actionText, String textLower) {
        if (textLower.contains("palpatine")) {
            applyPullActionPolicy(action,
                    PullActionPolicy.scoreTakeIntoHand(
                            new PullActionPolicy.TakeIntoHandFacts(
                                    action.getActionId(),
                                    PullActionPolicy.TakeIntoHandKind.PALPATINE)));
            return;
        }

        // V29.7: Detect RETURN-TO-HAND (bouncing own card from table) vs RETRIEVE (from deck).
        // Retrieval actions always specify the source: "from Reserve Deck", "from Force Pile", etc.
        // If no source pile is mentioned, the card is being RETURNED from table — that's BAD!
        // Example: Corporal Vandolay's "Take an ISB agent into hand" = bounce deployed character.
        // EXCEPTION: "destiny" / "re-draw" actions are battle destiny management, NOT bounces.
        boolean isFromDeck = textLower.contains("from reserve") || textLower.contains("from force pile")
            || textLower.contains("from used pile") || textLower.contains("from lost pile");
        boolean isDestinyAction = textLower.contains("destiny") || textLower.contains("re-draw")
            || textLower.contains("redraw");

        if (!isFromDeck && !isDestinyAction) {
            // This is a bounce/return from table — VERY bad! We just paid to deploy that character.
            applyPullActionPolicy(action,
                    PullActionPolicy.scoreTakeIntoHand(
                            new PullActionPolicy.TakeIntoHandFacts(
                                    action.getActionId(),
                                    PullActionPolicy.TakeIntoHandKind.BOUNCE)));
            logger.warn("V29.7 BOUNCE BLOCKED: '{}' would return deployed card to hand (-300)", actionText);
        } else if (isFromDeck && textLower.contains("from reserve")) {
            // V29.7: PULL FIRST RULE — retrievals from Reserve Deck are FREE actions
            // from effects like Endor Shield, Mobilization Points, etc.
            // These should ALWAYS fire before locations (+200) and characters.
            // Getting cards into hand first = better deploy decisions.
            // V192 (2026-07-06): generic +250 ABSORBED into the V192 pull scorer base —
            // "Reserve Deck" takes now route to the PULL-ENGINE branch (dispatch gate),
            // so this arm only sees leftover "from Reserve" phrasings without "Deck".
            // Grant commented out (feedback_comment_out_old_rules); the TDIGWATT-specific
            // admiral/general +250/+300 branch earlier in the chain is untouched.
            // action.addReasoning("V29.7 PULL FIRST: Get cards into hand before deploying!", 250.0f);
            applyPullActionPolicy(action,
                    PullActionPolicy.scoreTakeIntoHand(
                            new PullActionPolicy.TakeIntoHandFacts(
                                    action.getActionId(),
                                    PullActionPolicy.TakeIntoHandKind.RESERVE_LOG_ONLY)));
            logger.info("V29.7 PULL FIRST (absorbed by V192): '{}' — no standalone +250", actionText);
        } else if (isFromDeck && textLower.contains("from lost pile")) {
            // V63 LOST PILE GUARD: "take a character into hand from Lost Pile"
            // (Jedi Levitation etc.) needs a matching card in Lost Pile. If there
            // isn't one, the search FAILS and opponent sees our entire Lost Pile.
            // FIXES djme704a2jn60z5c replay: Rando fired Jedi Levitation twice
            // with no character in Lost Pile — wasted 8 force + revealed deck.
            GameState lpGs = context.getGameState();
            String lpPid = context.getPlayerId();
            if (lpGs != null && lpPid != null) {
                int matchingInLostPile = 0;
                try {
                    java.util.List<PhysicalCard> lp = lpGs.getLostPile(lpPid);
                    if (lp != null) {
                        boolean wantsCharacter = textLower.contains("character");
                        boolean wantsJedi = textLower.contains("jedi");
                        boolean wantsAlien = textLower.contains("alien");
                        for (PhysicalCard c : lp) {
                            if (c == null || c.getBlueprint() == null) continue;
                            CardCategory cat = c.getBlueprint().getCardCategory();
                            if (wantsCharacter && cat != CardCategory.CHARACTER) continue;
                            matchingInLostPile++;
                        }
                    }
                } catch (Exception e) { /* ignore */ }
                if (matchingInLostPile == 0) {
                    applyPullActionPolicy(action,
                            PullActionPolicy.scoreTakeIntoHand(
                                    new PullActionPolicy.TakeIntoHandFacts(
                                            action.getActionId(),
                                            PullActionPolicy.TakeIntoHandKind.LOST_PILE_NO_MATCH)));
                    logger.warn("V63 LOST PILE EMPTY: '{}' has 0 matching targets — hard-blocked", actionText);
                    return;
                }
                logger.info("V63 LOST PILE OK: '{}' — {} matching targets in Lost Pile",
                    actionText, matchingInLostPile);
            }
            applyPullActionPolicy(action,
                    PullActionPolicy.scoreTakeIntoHand(
                            new PullActionPolicy.TakeIntoHandFacts(
                                    action.getActionId(),
                                    PullActionPolicy.TakeIntoHandKind.LOST_PILE_MATCH)));
        } else {
            // From force pile, used pile, or destiny management — normal priority
            applyPullActionPolicy(action,
                    PullActionPolicy.scoreTakeIntoHand(
                            new PullActionPolicy.TakeIntoHandFacts(
                                    action.getActionId(),
                                    PullActionPolicy.TakeIntoHandKind.GENERIC)));
        }
    }

    /**
     * Evaluate barrier card (Imperial/Rebel Barrier) usage.
     * Ported from Python action_text_evaluator.py lines 973-1055
     *
     * Use barriers when:
     *   - Location IS contested (both players present)
     *   - Target is a significant threat (high power)
     *   - We're not already winning overwhelmingly
     * Save barriers when:
     *   - Location not contested (no point)
     *   - We're already dominating the location
     *   - Target already has a barrier on it this turn!
     */
    private void evaluateBarrier(EvaluatedAction action, DecisionContext context, String actionText) {
        String targetCardName = extractCardNameFromPreventText(actionText);
        int currentTurn = context.getTurnNumber();

        // Reset barrier tracking on new turn
        if (currentTurn != barrierTurn) {
            barrieredTargets.clear();
            barrierTurn = currentTurn;
        }

        // Check if we already barriered this target
        if (targetCardName != null && barrieredTargets.contains(targetCardName.toLowerCase())) {
            ResponsePolicy.BarrierEvaluation barrierEvaluation = ResponsePolicy.scoreBarrier(
                    action.getActionId(), targetCardName, true, false,
                    false, false, 0.0f, 0.0f, 0.0f);
            applyResponsePolicy(action, barrierEvaluation.result());
            return;
        }

        // V35.1: NEVER barrier our OWN characters! "You Are Beaten" can target any character,
        // but preventing our OWN character from battling/moving is self-sabotage.
        // Check if the target belongs to us — if so, HARD BLOCK.
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        if (gameState != null && playerId != null && targetCardName != null) {
            String targetLower = targetCardName.toLowerCase();
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null || card.getTitle() == null) continue;
                if (card.getTitle().toLowerCase().contains(targetLower) || targetLower.contains(card.getTitle().toLowerCase())) {
                    if (playerId.equals(card.getOwner())) {
                        ResponsePolicy.BarrierEvaluation barrierEvaluation = ResponsePolicy.scoreBarrier(
                                action.getActionId(), targetCardName, false, true,
                                false, false, 0.0f, 0.0f, 0.0f);
                        applyResponsePolicy(action, barrierEvaluation.result());
                        logger.warn("V35.1 SELF-BARRIER: Blocking barrier on OWN character {} (-9999)", targetCardName);
                        return;
                    }
                    break;
                }
            }
        }
        float targetPower = 0;
        float ourPower = 0;
        float theirPower = 0;
        boolean locationContested = false;

        if (gameState != null && playerId != null && targetCardName != null) {
            String opponentId = gameState.getOpponent(playerId);

            // Find the target card and analyze location
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;
                String title = card.getTitle();
                if (title == null) continue;

                // Match by name
                if (title.toLowerCase().contains(targetCardName.toLowerCase()) ||
                    targetCardName.toLowerCase().contains(title.toLowerCase())) {

                    // Found the target - check its power
                    SwccgCardBlueprint blueprint = card.getBlueprint();
                    if (blueprint != null && blueprint.hasPowerAttribute()) {
                        Float power = blueprint.getPower();
                        if (power != null) {
                            targetPower = power;
                        }
                    }

                    // Find location and calculate power
                    PhysicalCard location = card.getAtLocation();
                    if (location != null) {
                        boolean hasOurPresence = false;
                        boolean hasTheirPresence = false;

                        for (PhysicalCard locCard : gameState.getCardsAtLocation(location)) {
                            if (locCard == null) continue;
                            String owner = locCard.getOwner();
                            SwccgCardBlueprint bp = locCard.getBlueprint();
                            if (bp == null) continue;

                            // Check presence
                            if (playerId.equals(owner)) {
                                hasOurPresence = true;
                                if (bp.hasPowerAttribute()) {
                                    Float power = bp.getPower();
                                    if (power != null) ourPower += power;
                                }
                            } else if (opponentId != null && opponentId.equals(owner)) {
                                hasTheirPresence = true;
                                if (bp.hasPowerAttribute()) {
                                    Float power = bp.getPower();
                                    if (power != null) theirPower += power;
                                }
                            }
                        }
                        locationContested = hasOurPresence && hasTheirPresence;
                    }
                    break;
                }
            }
        }

        logger.debug("🚧 Barrier analysis: {} (power {}) contested={}, our={}, their={}",
            targetCardName, targetPower, locationContested, ourPower, theirPower);

        // V48: Check if WE have any presence at the target's location
        // Barrier prevents battling/moving. If we have nobody there, it serves no purpose.
        boolean weHavePresence = ourPower > 0;

        ResponsePolicy.BarrierEvaluation barrierEvaluation = ResponsePolicy.scoreBarrier(
                action.getActionId(), targetCardName, false, false,
                weHavePresence, locationContested, targetPower, ourPower, theirPower);
        applyResponsePolicy(action, barrierEvaluation.result());
        if (!weHavePresence) {
            logger.warn("V48 BARRIER BLOCK: No friendly presence at target location — HARD BLOCK!");
        }
        if (barrierEvaluation.rememberTarget() && targetCardName != null) {
            barrieredTargets.add(targetCardName.toLowerCase());
        }
    }

    private void evaluateEmbark(EvaluatedAction action, DecisionContext context, String actionText, String cardId) {
        // 2026-06-01 EMBARK BOOST (Steve, Walker games):
        // "Rando already had pilots on the same site. He's not embarking them
        // onto the walkers or vehicles." The engine offers an 'Embark' action
        // per pilot at a site where one of Rando's vehicles/ships is parked;
        // the prior placeholder scored it 0 and other moves (or pass) won.
        // Fix: when the embarker is a pilot (Icon.PILOT or Keyword.TROOPER)
        // AND at least one unmanned VEHICLE/STARSHIP shares the site with
        // them, +500. Puts the embark above generic moves and pass so Rando
        // gets the walker manned.
        try {
            com.gempukku.swccgo.game.state.GameState embarkGs = context.getGameState();
            com.gempukku.swccgo.game.SwccgGame embarkGame = context.getGame();
            if (embarkGs == null || embarkGame == null || cardId == null) {
                applyEmbarkPolicy(action, false, false, false, null,
                    false, false, false, null, null);
                return;
            }
            com.gempukku.swccgo.game.PhysicalCard embarker = null;
            try {
                embarker = embarkGs.findCardById(Integer.parseInt(cardId));
            } catch (NumberFormatException nfe) { /* temp ids — skip */ }
            if (embarker == null || embarker.getBlueprint() == null) {
                applyEmbarkPolicy(action, true, false, false, null,
                    false, false, false, null, null);
                return;
            }
            SwccgCardBlueprint embarkerBp = embarker.getBlueprint();
            boolean embarkerIsPilot =
                embarkerBp.hasIcon(com.gempukku.swccgo.common.Icon.PILOT)
                || embarkerBp.hasKeyword(com.gempukku.swccgo.common.Keyword.TROOPER);
            if (!embarkerIsPilot) {
                // Non-pilot character embarking is usually a passenger move — neutral.
                applyEmbarkPolicy(action, true, true, false, null,
                    false, false, false, embarker.getTitle(), null);
                return;
            }
            // 2026-06-01 POWER-3 GATE (Steve): "If pilot is power 4 or more
            // let's leave them disembarked from vehicles. Likely better as
            // ground troops. Regular pilots are usually power 3 or less."
            // Skip the embark boost for power-4+ characters — they're more
            // valuable hitting people on the ground than crewing a vehicle.
            Float embarkerPower = embarkerBp.hasPowerAttribute() ? embarkerBp.getPower() : null;
            // Find the embarker's current location.
            com.gempukku.swccgo.game.PhysicalCard embarkLoc = null;
            try {
                embarkLoc = embarkGame.getModifiersQuerying()
                    .getLocationThatCardIsAt(embarkGs, embarker);
            } catch (Exception ignore) { /* */ }
            if (embarkLoc == null) {
                applyEmbarkPolicy(action, true, true, true, embarkerPower,
                    false, false, false, embarker.getTitle(), null);
                return;
            }
            // Walk permanents at the same site for an unmanned vehicle/ship owned by us.
            String embarkPid = context.getPlayerId();
            String unmannedTitle = null;
            String objectiveEnablerTitle = null;
            for (com.gempukku.swccgo.game.PhysicalCard pc : embarkGs.getAllPermanentCards()) {
                if (pc == null || !embarkPid.equals(pc.getOwner())) continue;
                if (pc.getBlueprint() == null) continue;
                com.gempukku.swccgo.common.CardCategory cat = pc.getBlueprint().getCardCategory();
                if (cat != com.gempukku.swccgo.common.CardCategory.VEHICLE
                        && cat != com.gempukku.swccgo.common.CardCategory.STARSHIP) continue;
                com.gempukku.swccgo.game.PhysicalCard pcLoc = null;
                try {
                    pcLoc = embarkGame.getModifiersQuerying()
                        .getLocationThatCardIsAt(embarkGs, pc);
                } catch (Exception ignore) { /* */ }
                if (pcLoc != embarkLoc) continue;
                // Unmanned check via Filters.piloted.
                boolean piloted = com.gempukku.swccgo.filters.Filters.piloted.accepts(
                    embarkGs, embarkGame.getModifiersQuerying(), pc);
                if (!piloted) {
                    if (unmannedTitle == null) {
                        unmannedTitle = pc.getTitle();
                    }
                    if (context.getObjectiveAnalyzer() != null
                            && context.getObjectiveAnalyzer()
                                .isAnalyzed()
                            && context.getObjectiveAnalyzer()
                                .advancesRequiredCardDeployPrerequisiteAt(
                                    embarkGame, embarkPid,
                                    embarker, pc)) {
                        objectiveEnablerTitle = pc.getTitle();
                    }
                }
            }
            if (unmannedTitle != null) {
                applyEmbarkPolicy(action, true, true, true, embarkerPower,
                    true, true, false, embarker.getTitle(), unmannedTitle);
                logger.warn("EMBARK PILOT: {} boarding unmanned {} → +500",
                    embarker.getTitle(), unmannedTitle);
            } else {
                applyEmbarkPolicy(action, true, true, true, embarkerPower,
                    true, false, false, embarker.getTitle(), null);
            }
            if (objectiveEnablerTitle != null) {
                action.addReasoning(
                    "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_EMBARK_START: "
                        + embarker.getTitle()
                        + " can pilot " + objectiveEnablerTitle
                        + " for the required-card deploy route",
                    600.0f,
                    TraceRuleId.of(
                        "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_EMBARK_START"),
                    TraceDomainId.MOVE,
                    TraceOutputKind.BANDED);
            }
        } catch (Exception e) {
            logger.debug("evaluateEmbark error: {}", e.getMessage());
            applyEmbarkPolicy(action, true, true, true, null,
                true, false, true, null, null);
        }
    }

    private void applyEmbarkPolicy(
            EvaluatedAction action,
            boolean contextAvailable,
            boolean cardAvailable,
            boolean eligiblePilotOrTrooper,
            Float power,
            boolean locationAvailable,
            boolean unmannedTargetAtSite,
            boolean readFailed,
            String embarkerTitle,
            String unmannedTargetTitle) {
        MoveTransitPolicy.EmbarkEvaluation embark = MoveTransitPolicy.embark(
            contextAvailable, cardAvailable, eligiblePilotOrTrooper, power,
            locationAvailable, unmannedTargetAtSite, readFailed,
            embarkerTitle, unmannedTargetTitle);
        action.addReasoning(
            embark.contribution().reason(), embark.contribution().delta());
    }

    private void evaluateGrab(EvaluatedAction action, DecisionContext context, String actionText) {
        // V53: Grabber shields (Allegations / A Tragedy) must ONLY grab OPPONENT's interrupts.
        // NEVER grab your own interrupts — that's self-sabotage.
        // Use game state to check card ownership when possible, fall back to name matching.

        Side mySide = context.getSide();
        GameState grabGs = context.getGameState();
        String textLower = actionText.toLowerCase();

        // V53: Try to determine ownership from game state (most reliable)
        boolean confirmedOwnCard = false;
        boolean confirmedOpponentCard = false;
        if (grabGs != null && context.getPlayerId() != null) {
            try {
                // Check if any card IDs in context belong to us
                String pid = context.getPlayerId();
                String oid = grabGs.getOpponent(pid);
                for (String cardId : context.getCardIds()) {
                    PhysicalCard grabCard = grabGs.findCardById(Integer.parseInt(cardId));
                    if (grabCard != null) {
                        if (pid.equals(grabCard.getOwner())) confirmedOwnCard = true;
                        if (oid != null && oid.equals(grabCard.getOwner())) confirmedOpponentCard = true;
                    }
                }
            } catch (Exception e) { /* fall through to name matching */ }
        }

        if (confirmedOwnCard || confirmedOpponentCard) {
            ResponsePolicy.GrabEvaluation grabEvaluation = ResponsePolicy.scoreGrab(
                    action.getActionId(), confirmedOwnCard, confirmedOpponentCard, mySide,
                    false, false, context.isMyTurn());
            if (grabEvaluation.setScoreBeforeAdd()) {
                action.setScore(-9999.0f);
            }
            applyResponsePolicy(action, grabEvaluation.result());
            if (grabEvaluation.outcome() == ResponsePolicy.GrabOutcome.CONFIRMED_OWN) {
                logger.warn("V53 GRAB BLOCKED: Confirmed own card — HARD BLOCKED! {}", actionText);
            } else {
                logger.warn("V53 GRAB: Confirmed opponent card — grabbing! {}", actionText);
            }
            return;
        }

        // Fallback: name-based side detection
        boolean looksLightSide = textLower.contains("rebel") || textLower.contains("jedi") ||
                                  textLower.contains("alliance") || textLower.contains("luke") ||
                                  textLower.contains("leia") || textLower.contains("han solo") ||
                                  textLower.contains("chewie") || textLower.contains("yoda") ||
                                  textLower.contains("obi-wan") || textLower.contains("padme");
        boolean looksDarkSide = textLower.contains("imperial") || textLower.contains("sith") ||
                                 textLower.contains("vader") || textLower.contains("emperor") ||
                                 textLower.contains("stormtrooper") || textLower.contains("death star") ||
                                 textLower.contains("maul") || textLower.contains("dooku") ||
                                 textLower.contains("boba fett") || textLower.contains("jango");
        ResponsePolicy.GrabEvaluation grabEvaluation = ResponsePolicy.scoreGrab(
                action.getActionId(), false, false, mySide,
                looksLightSide, looksDarkSide, context.isMyTurn());
        if (grabEvaluation.setScoreBeforeAdd()) {
            action.setScore(-9999.0f);
        }
        applyResponsePolicy(action, grabEvaluation.result());
        if (grabEvaluation.outcome() == ResponsePolicy.GrabOutcome.NAME_OWN_DARK) {
            logger.warn("V53 GRAB BLOCKED: Likely own Dark card — {}", actionText);
        } else if (grabEvaluation.outcome() == ResponsePolicy.GrabOutcome.NAME_OWN_LIGHT) {
            logger.warn("V53 GRAB BLOCKED: Likely own Light card — {}", actionText);
        } else if (grabEvaluation.outcome() == ResponsePolicy.GrabOutcome.UNKNOWN_OWN_TURN) {
            logger.info("V53 GRAB CAUTION: Unknown owner on our turn, avoiding: {}", actionText);
        }
    }

    private void evaluateBreakCover(EvaluatedAction action, DecisionContext context, String actionText) {
        // V53: Breaking spy cover depends on context:
        // - Break OPPONENT's spy: always good (expose their spy)
        // - Break OWN spy when we have a friendly character at that location: +500
        //   (flip the spy to protect our deployed character — instant buddy system)
        // - Break OWN spy when we have NO friendly character there: -500
        //   (don't blow cover for nothing)

        Side mySide = context.getSide();
        GameState gameState = context.getGameState();

        // V59 OWNER RESOLUTION: Look up the spy's actual owner via cardId first.
        // FIXES Issue #6 from peaceful-pike replay: actionText was just "Break cover"
        // with no card name, so regex matching failed and we fell through to
        // the "unknown owner" -30 branch. Now we resolve via PhysicalCard.
        Boolean ownerIsUs = null;  // null = unknown, true = our spy, false = opponent's
        try {
            List<String> ctxCardIds = context.getCardIds();
            if (ctxCardIds != null && !ctxCardIds.isEmpty() && gameState != null) {
                String cardIdStr = ctxCardIds.get(0);
                PhysicalCard spyCard = gameState.findCardById(Integer.parseInt(cardIdStr));
                if (spyCard != null && spyCard.getOwner() != null) {
                    ownerIsUs = spyCard.getOwner().equals(context.getPlayerId());
                    logger.info("V59 BREAK COVER OWNER: spy {} owner={} (we are {})",
                        spyCard.getTitle(), spyCard.getOwner(), context.getPlayerId());
                }
            }
        } catch (Exception e) {
            logger.debug("V59 BREAK COVER: Error resolving owner: {}", e.getMessage());
        }

        // Fallback: Determine side from card name patterns in action text
        String textLower = actionText.toLowerCase();
        boolean looksLightSide = textLower.contains("rebel") || textLower.contains("bothan") ||
                                  textLower.contains("alliance") || textLower.contains("leia") ||
                                  textLower.contains("mon mothma") || textLower.contains("orrimaarko");
        boolean looksDarkSide = textLower.contains("imperial") || textLower.contains("ism-agent") ||
                                 textLower.contains("empire") || textLower.contains("probe droid") ||
                                 textLower.contains("mara jade");

        boolean isOwnSpy = (ownerIsUs != null && ownerIsUs)
            || (ownerIsUs == null && ((mySide == Side.DARK && looksDarkSide) || (mySide == Side.LIGHT && looksLightSide)));
        boolean isOpponentSpy = (ownerIsUs != null && !ownerIsUs)
            || (ownerIsUs == null && ((mySide == Side.DARK && looksLightSide) || (mySide == Side.LIGHT && looksDarkSide)));

        boolean friendlyCharAtSpyLocation = false;
        if (isOwnSpy && !isOpponentSpy) {
            // V53: Check if we have a non-spy friendly character at the spy's location.
            // If yes, flip the spy to fight alongside them (+500).
            // If no, don't blow cover for nothing (-500).
            if (gameState != null) {
                try {
                    String pid = context.getPlayerId();
                    // Find our undercover spies and check their locations for friendly characters
                    for (PhysicalCard loc : gameState.getTopLocations()) {
                        if (loc == null) continue;
                        boolean hasOurSpy = false;
                        boolean hasOurCharacter = false;
                        for (PhysicalCard c : gameState.getCardsAtLocation(loc)) {
                            if (c == null || !pid.equals(c.getOwner())) continue;
                            if (c.isUndercover()) {
                                hasOurSpy = true;
                            } else if (c.getBlueprint() != null
                                && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                hasOurCharacter = true;
                            }
                        }
                        if (hasOurSpy && hasOurCharacter) {
                            friendlyCharAtSpyLocation = true;
                            break;
                        }
                    }
                } catch (Exception e) { /* ignore */ }
            }
        }

        MoveSpyFollowPolicy.Contribution breakCover =
            MoveSpyFollowPolicy.breakCover(
                isOpponentSpy, isOwnSpy, friendlyCharAtSpyLocation);
        action.addReasoning(breakCover.reason(), breakCover.delta());
        if (isOwnSpy && !isOpponentSpy && friendlyCharAtSpyLocation) {
            logger.warn("V53 FLIP SPY: Breaking own spy cover — friendly character present, +500!");
        } else if (isOwnSpy && !isOpponentSpy) {
            logger.warn("V53 KEEP COVER: No friendly at spy location — blocking break cover, -500");
        } else if (!isOpponentSpy) {
            // Unknown spy - check for friendly presence as tiebreaker
            logger.info("Break cover owner unknown, avoiding: {}", actionText);
        }
    }

    // ========== Utility Methods ==========

    private boolean isVadersCastleActionSource(
            GameState gameState, String cardId) {
        if (gameState == null || cardId == null) return false;
        try {
            PhysicalCard source =
                    gameState.findCardById(Integer.parseInt(cardId));
            return source != null
                    && "209_50".equals(source.getBlueprintId(true));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isClassicHuntDownActionSource(
            GameState gameState, String cardId) {
        if (gameState == null || cardId == null) return false;
        try {
            PhysicalCard source =
                    gameState.findCardById(Integer.parseInt(cardId));
            return source != null
                    && "7_297".equals(source.getBlueprintId(true));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isVirtualHuntDownActionSource(
            GameState gameState, String cardId) {
        if (gameState == null || cardId == null) return false;
        try {
            PhysicalCard source =
                    gameState.findCardById(Integer.parseInt(cardId));
            return source != null
                    && ("213_31".equals(source.getBlueprintId(true))
                        || "213_31_BACK".equals(
                            source.getBlueprintId(true)));
        } catch (Exception e) {
            return false;
        }
    }

    private String extractBlueprintFromText(String actionText) {
        if (actionText == null) return null;
        Matcher matcher = BLUEPRINT_PATTERN.matcher(actionText);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractCardNameFromPreventText(String actionText) {
        // Pattern: "Prevent <CARD NAME> from battling or moving"
        if (actionText != null && actionText.contains("Prevent") &&
            actionText.contains("from battling or moving")) {
            int startIdx = actionText.indexOf("Prevent") + "Prevent ".length();
            int endIdx = actionText.indexOf(" from battling or moving");
            if (startIdx > 0 && endIdx > startIdx) {
                return actionText.substring(startIdx, endIdx).trim();
            }
        }
        return null;
    }

    /**
     * Check if there are valid (non-HIT) weapon targets at the battle location.
     *
     * In SWCCG, firing at already-hit targets is wasteful since they're
     * already damaged. This method returns true only if there are unhit
     * enemy cards at the battle location.
     *
     * Ported from Python action_text_evaluator.py valid target check.
     */
    private boolean checkForValidWeaponTargets(DecisionContext context) {
        GameState gameState = context.getGameState();
        if (gameState == null) {
            return true;  // Default to allowing fire if we can't check
        }

        try {
            // Get the battle location
            PhysicalCard battleLocation = gameState.getBattleLocation();
            if (battleLocation == null) {
                return true;  // Not in battle, allow fire
            }

            // Find enemy cards at battle location
            String playerId = context.getPlayerId();
            String opponentId = gameState.getOpponent(playerId);
            if (opponentId == null) {
                return true;  // Can't determine opponent
            }

            // Check all enemy cards at battle location
            boolean foundUnhitEnemy = false;
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;

                // Must be enemy card
                if (!opponentId.equals(card.getOwner())) continue;

                // Must be at battle location
                PhysicalCard cardLocation = card.getAtLocation();
                if (cardLocation == null || !cardLocation.equals(battleLocation)) continue;

                // Must be a valid weapon target (character, starship, vehicle)
                SwccgCardBlueprint bp = card.getBlueprint();
                if (bp == null) continue;
                CardCategory cat = bp.getCardCategory();
                if (cat != CardCategory.CHARACTER && cat != CardCategory.STARSHIP && cat != CardCategory.VEHICLE) {
                    continue;
                }

                // Check if this card is NOT hit
                if (!card.isHit()) {
                    foundUnhitEnemy = true;
                    logger.debug("Found unhit enemy target: {}", card.getTitle());
                    break;  // Found at least one valid target
                }
            }

            if (!foundUnhitEnemy) {
                logger.info("🎯 All enemy targets at battle location are HIT - no valid weapon targets");
            }

            return foundUnhitEnemy;

        } catch (Exception e) {
            logger.debug("Error checking weapon targets: {}", e.getMessage());
            return true;  // Default to allowing fire on error
        }
    }
}
