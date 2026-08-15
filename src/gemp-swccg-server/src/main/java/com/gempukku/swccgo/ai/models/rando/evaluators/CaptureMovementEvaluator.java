package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.common.phase.CaptureMovementMechanismFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.BhbmSetupPayoffFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MovePhysicalCardResolver;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Narrow adapter for source-defined Capture objective movement mechanisms
 * that do not travel through the ordinary landspeed route.
 */
public class CaptureMovementEvaluator extends ActionEvaluator {
    private static final String DOCKING_SOURCE_EXTRA =
            ObjectiveAnalyzer.DOCKING_TRANSIT_SOURCE_CARD_ID_EXTRA;

    public CaptureMovementEvaluator() {
        super("CaptureMovement");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        if (!hasCaptureContext(context)) {
            return false;
        }
        if (isActionChoice(context)) {
            List<String> texts = context.getActionTexts();
            List<String> cardIds = context.getCardIds();
            for (int i = 0; i < texts.size(); i++) {
                PhysicalCard card = cardAt(
                        context,
                        i < cardIds.size()
                            ? cardIds.get(i) : null);
                if (CaptureMovementMechanismFactsReader
                        .classifyParent(card, texts.get(i))
                        != CaptureMovementMechanismFactsReader
                            .Mechanism.UNKNOWN) {
                    return true;
                }
            }
            return false;
        }
        return "CARD_SELECTION".equals(
                    context.getDecisionType())
                && childMechanism(context).mechanism()
                    != CaptureMovementMechanismFactsReader
                        .Mechanism.UNKNOWN;
    }

    @Override
    public List<EvaluatedAction> evaluate(
            DecisionContext context) {
        return isActionChoice(context)
                ? evaluateParents(context)
                : evaluateChildren(context);
    }

    private List<EvaluatedAction> evaluateParents(
            DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<String> ids = context.getActionIds();
        List<String> texts = context.getActionTexts();
        List<String> cardIds = context.getCardIds();
        int count = Math.min(ids.size(), texts.size());
        for (int i = 0; i < count; i++) {
            PhysicalCard actionCard = cardAt(
                    context,
                    i < cardIds.size()
                        ? cardIds.get(i) : null);
            var mechanism =
                    CaptureMovementMechanismFactsReader
                        .classifyParent(
                            actionCard, texts.get(i));
            if (mechanism
                    == CaptureMovementMechanismFactsReader
                        .Mechanism.UNKNOWN) {
                continue;
            }
            var assessment =
                    CaptureMovementMechanismFactsReader.assess(
                        context.getGame(),
                        context.getPlayerId(),
                        context.getObjectiveAnalyzer(),
                        mechanism, actionCard,
                        texts.get(i));
            if (!assessment.factsKnown()) {
                continue;
            }
            EvaluatedAction action = new EvaluatedAction(
                    ids.get(i), ActionType.MOVE,
                    0.0f, texts.get(i));

            // Ordinary landspeed already owns its R4 parent claim in
            // MoveEvaluator. Every other exact mechanism needs this bridge.
            if (ownsSpecialParentCaptureCredit(mechanism)
                    && assessment
                        .hasAdmissibleCaptureRoute()) {
                applyCaptureCredit(
                        context, action,
                        CaptureObjectivePolicy
                            .CaptureRouteStep.PARENT);
            } else if (ownsSpecialParentApproachCredit(
                            mechanism)
                    && assessment
                        .hasAdmissibleApproachRoute()) {
                applyApproachStart(action, assessment.routes());
            }
            applyBhbmYourDestiny(
                    context, action,
                    assessment.forceBudgetReady(),
                    true,
                    assessment.routes());

            if (CaptureMovementMechanismFactsReader
                    .parentCommitsObjectiveMover(
                        mechanism)) {
                List<CaptureMovementMechanismFactsReader.Route>
                        relevant = assessment.routes().stream()
                            .filter(route ->
                                route.objectiveRelevantMover())
                            .toList();
                if (!relevant.isEmpty()
                        && relevant.stream().allMatch(
                            CaptureMovementMechanismFactsReader
                                .Route::breaksStableBack)) {
                    applyStableBackHold(
                            context, action, true);
                } else if (!relevant.isEmpty()
                        && relevant.stream().allMatch(
                            CaptureMovementMechanismFactsReader
                                .Route::formationBlocked)) {
                    applyFormationHold(action);
                } else if (!relevant.isEmpty()
                        && relevant.stream().allMatch(
                            CaptureMovementMechanismFactsReader
                                .Route::hardBlocked)) {
                    applyCombinedRouteHold(action);
                }
            }
            actions.add(action);
        }
        return actions;
    }

    private List<EvaluatedAction> evaluateChildren(
            DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        ChildMechanism child = childMechanism(context);
        if (child.mechanism()
                == CaptureMovementMechanismFactsReader
                    .Mechanism.UNKNOWN
                || child.step()
                    == CaptureMovementMechanismFactsReader
                        .ChoiceStep.UNKNOWN) {
            return actions;
        }
        var assessment =
                CaptureMovementMechanismFactsReader.assess(
                    context.getGame(),
                    context.getPlayerId(),
                    context.getObjectiveAnalyzer(),
                    child.mechanism(),
                    child.actionCard(),
                    child.actionText());
        if (!assessment.factsKnown()) {
            return actions;
        }
        PhysicalCard selectedDestination =
                selectedDestination(
                    context, child.liveAction(),
                    child.mechanism(), child.step());
        PhysicalCard selectedOrigin =
                selectedOrigin(context);
        List<String> cardIds = context.getCardIds();
        List<Boolean> selectable = context.getSelectable();
        for (int i = 0; i < cardIds.size(); i++) {
            if (!isSelectable(selectable, i)) {
                continue;
            }
            PhysicalCard candidate =
                    cardAt(context, cardIds.get(i));
            if (candidate == null) {
                continue;
            }
            List<CaptureMovementMechanismFactsReader.Route>
                    bound =
                        CaptureMovementMechanismFactsReader.bind(
                            assessment, child.step(),
                            candidate,
                            selectedOrigin,
                            selectedDestination);
            if (bound.isEmpty()) {
                continue;
            }
            EvaluatedAction action = new EvaluatedAction(
                    cardIds.get(i), ActionType.MOVE,
                    0.0f, context.getDecisionText());
            boolean admissibleCapture =
                    assessment.forceBudgetReady()
                    && bound.stream().anyMatch(route ->
                        route.objectiveRelevantMover()
                            && route.admissible()
                            && route.guaranteesImmediateCapture());
            if (admissibleCapture
                    && ownsSpecialChildCaptureCredit(
                        child.mechanism())) {
                applyCaptureCredit(
                        context, action,
                        CaptureObjectivePolicy
                            .CaptureRouteStep.DESTINATION);
            } else if (ownsSpecialChildCaptureCredit(
                            child.mechanism())) {
                applyApproachDestination(action, bound);
            }
            if (ownsSpecialChildCaptureCredit(
                    child.mechanism())) {
                applyBhbmYourDestiny(
                        context, action,
                        assessment.forceBudgetReady(),
                        false,
                        bound);
            }

            boolean hasAdmissible =
                    bound.stream().anyMatch(
                        CaptureMovementMechanismFactsReader
                            .Route::admissible);
            if (!hasAdmissible
                    && bound.stream().allMatch(
                        CaptureMovementMechanismFactsReader
                            .Route::breaksStableBack)) {
                applyStableBackHold(
                        context, action, true);
            } else if (!hasAdmissible
                    && bound.stream().allMatch(
                        CaptureMovementMechanismFactsReader
                            .Route::formationBlocked)) {
                applyFormationHold(action);
            } else if (!hasAdmissible
                    && bound.stream().allMatch(
                        CaptureMovementMechanismFactsReader
                            .Route::hardBlocked)) {
                applyCombinedRouteHold(action);
            }
            actions.add(action);
        }
        return actions;
    }

    private void applyCaptureCredit(
            DecisionContext context,
            EvaluatedAction action,
            CaptureObjectivePolicy.CaptureRouteStep step) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                CaptureObjectiveFacts.objectiveKind(
                    context.getObjectiveAnalyzer());
        if (kind == null) {
            return;
        }
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                    "capture-mechanism-"
                        + action.getActionId()
                        + "-" + step);
        ledger.register(
            CaptureObjectivePolicy.scoreCaptureRoute(
                new CaptureObjectivePolicy.CaptureRouteFacts(
                    action.getActionId(), kind,
                    step, true)));
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyBhbmYourDestiny(
            DecisionContext context,
            EvaluatedAction action,
            boolean forceBudgetReady,
            boolean requireNewPayoff,
            List<CaptureMovementMechanismFactsReader.Route>
                    routes) {
        boolean ready = forceBudgetReady
                && routes != null
                && routes.stream().anyMatch(route ->
                    route != null
                        && route.admissible()
                        && (!requireNewPayoff
                            || !BhbmSetupPayoffFactsReader
                                .currentlyRewardsVader(
                                    context.getGame(),
                                    context.getPlayerId(),
                                    context.getObjectiveAnalyzer(),
                                    route.mover()))
                        && BhbmSetupPayoffFactsReader
                            .projectedVaderMoveFormationSafe(
                                context.getGame(),
                                context.getPlayerId(),
                                route.mover(),
                                route.destination())
                        && BhbmSetupPayoffFactsReader
                            .rewardsVaderAtBattleground(
                                context.getGame(),
                                context.getPlayerId(),
                                context.getObjectiveAnalyzer(),
                                route.mover(),
                                route.destination()));
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                    "bhbm-your-destiny-"
                        + action.getActionId());
        ledger.register(
            CaptureObjectivePolicy.scoreBhbmYourDestiny(
                new CaptureObjectivePolicy.BhbmYourDestinyFacts(
                    action.getActionId(), ready)));
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyStableBackHold(
            DecisionContext context,
            EvaluatedAction action,
            boolean allRoutesBreak) {
        CaptureObjectivePolicy.ObjectiveKind kind =
                CaptureObjectiveFacts.objectiveKind(
                    context.getObjectiveAnalyzer());
        if (kind == null) {
            return;
        }
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                    "capture-mechanism-stable-"
                        + action.getActionId());
        ledger.register(
            CaptureObjectivePolicy.scoreStableBackHold(
                new CaptureObjectivePolicy.StableBackFacts(
                    action.getActionId(), kind,
                    context.getObjectiveAnalyzer()
                        .isFlipped(),
                    CaptureObjectiveFacts.stableBackState(
                        context.getGame(),
                        context.getPlayerId(),
                        context.getObjectiveAnalyzer()),
                    allRoutesBreak)));
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyApproachStart(
            EvaluatedAction action,
            List<CaptureMovementMechanismFactsReader.Route>
                    routes) {
        CaptureMovementMechanismFactsReader.Route route =
                routes.stream()
                    .filter(candidate ->
                        candidate.objectiveRelevantMover()
                            && candidate.admissible()
                            && candidate
                                .advancesCaptureApproach())
                    .findFirst()
                    .orElse(null);
        if (route == null) {
            return;
        }
        MoveDestinationPolicy.Contribution contribution =
                MoveDestinationPolicy
                    .objectiveActorRouteStart(
                        true, route.mover().getTitle());
        if (contribution.applies()) {
            action.addReasoning(
                contribution.reason(),
                contribution.delta(),
                TraceRuleId.of(
                    "MOVE.OBJECTIVE.ACTOR_ROUTE_START"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED);
        }
    }

    private void applyApproachDestination(
            EvaluatedAction action,
            List<CaptureMovementMechanismFactsReader.Route>
                    routes) {
        CaptureMovementMechanismFactsReader.Route route =
                routes.stream()
                    .filter(candidate ->
                        candidate.objectiveRelevantMover()
                            && candidate.admissible()
                            && candidate
                                .advancesCaptureApproach())
                    .findFirst()
                    .orElse(null);
        if (route == null) {
            return;
        }
        PhysicalCard destination =
                route.effectiveDestination();
        MoveDestinationPolicy.Contribution contribution =
                MoveDestinationPolicy
                    .objectiveActorRouteDestination(
                        true, route.mover().getTitle(),
                        destination != null
                            ? destination.getTitle() : null);
        if (contribution.applies()) {
            action.addReasoning(
                contribution.reason(),
                contribution.delta(),
                TraceRuleId.of(
                    "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED);
        }
    }

    private void applyFormationHold(
            EvaluatedAction action) {
        action.hardVeto(
            "OBJECTIVE.CAPTURE_MECHANISM.FORMATION_HOLD:"
                + " every exact route violates FormationSafety",
            TraceRuleId.of(
                "MOVE.OBJECTIVE.CAPTURE_MECHANISM_FORMATION_HOLD"),
            TraceDomainId.MOVE,
            TraceOutputKind.VETO);
    }

    private void applyCombinedRouteHold(
            EvaluatedAction action) {
        action.addReasoning(
            "OBJECTIVE.CAPTURE_MECHANISM.ROUTE_HOLD:"
                + " every exact route breaks stable-back"
                + " or violates FormationSafety",
            -300.0f,
            TraceRuleId.of(
                "MOVE.OBJECTIVE.CAPTURE_MECHANISM_ROUTE_HOLD"),
            TraceDomainId.OBJECTIVE_INTENT,
            TraceOutputKind.BANDED);
    }

    private ChildMechanism childMechanism(
            DecisionContext context) {
        String prompt = context.getDecisionText();

        for (var standard : List.of(
                CaptureMovementMechanismFactsReader
                    .Mechanism.LANDSPEED,
                CaptureMovementMechanismFactsReader
                    .Mechanism.SHUTTLE,
                CaptureMovementMechanismFactsReader
                    .Mechanism.EMBARK,
                CaptureMovementMechanismFactsReader
                    .Mechanism.DISEMBARK)) {
            var step =
                    CaptureMovementMechanismFactsReader
                        .classifyChild(standard, prompt);
            if (step
                    != CaptureMovementMechanismFactsReader
                        .ChoiceStep.UNKNOWN) {
                PhysicalCard mover = moverFromExtra(context);
                return mover != null
                    ? new ChildMechanism(
                        standard, step, mover,
                        standardParentText(standard), null)
                    : ChildMechanism.unknown();
            }
        }

        var dockingStep =
                CaptureMovementMechanismFactsReader
                    .classifyChild(
                        CaptureMovementMechanismFactsReader
                            .Mechanism.DOCKING_BAY_TRANSIT,
                        prompt);
        if (dockingStep
                != CaptureMovementMechanismFactsReader
                    .ChoiceStep.UNKNOWN) {
            PhysicalCard source = cardAt(
                    context,
                    String.valueOf(
                        context.getExtra(
                            DOCKING_SOURCE_EXTRA)));
            return source != null
                ? new ChildMechanism(
                    CaptureMovementMechanismFactsReader
                        .Mechanism.DOCKING_BAY_TRANSIT,
                    dockingStep, source,
                    "Docking bay transit", null)
                : ChildMechanism.unknown();
        }

        Action live = currentLiveAction(context);
        if (live == null) {
            return ChildMechanism.unknown();
        }
        PhysicalCard source = live.getActionSource();
        String actionText = live.getText();
        var mechanism =
                CaptureMovementMechanismFactsReader
                    .classifyParent(source, actionText);
        var step =
                CaptureMovementMechanismFactsReader
                    .classifyChild(mechanism, prompt);
        return step
                    != CaptureMovementMechanismFactsReader
                        .ChoiceStep.UNKNOWN
                ? new ChildMechanism(
                    mechanism, step, source,
                    actionText, live)
                : ChildMechanism.unknown();
    }

    private Action currentLiveAction(
            DecisionContext context) {
        try {
            var play = context.getGameState()
                    .getTopPlayCardState(null);
            if (play != null
                    && play.getPlayCardAction() != null) {
                Action action = play.getPlayCardAction();
                if (CaptureMovementMechanismFactsReader
                        .classifyParent(
                            action.getActionSource(),
                            action.getText())
                        != CaptureMovementMechanismFactsReader
                            .Mechanism.UNKNOWN) {
                    return action;
                }
            }
            var gameText = context.getGameState()
                    .getTopGameTextActionState();
            return gameText != null
                    ? gameText.getGameTextAction() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private PhysicalCard selectedDestination(
            DecisionContext context,
            Action live,
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism,
            CaptureMovementMechanismFactsReader.ChoiceStep
                    step) {
        if (step
                != CaptureMovementMechanismFactsReader
                    .ChoiceStep.MOVER) {
            return null;
        }
        if (mechanism
                == CaptureMovementMechanismFactsReader
                    .Mechanism.TRANSPORT
                && live != null) {
            try {
                for (Map<PhysicalCard, Set<com.gempukku.swccgo.common
                        .TargetingReason>> group
                        : live.getAllPrimaryTargetCards()
                            .values()) {
                    for (PhysicalCard card : group.keySet()) {
                        if (card != null
                                && card.getBlueprint() != null
                                && card.getBlueprint()
                                    .getCardCategory()
                                    == CardCategory.LOCATION) {
                            return card;
                        }
                    }
                }
            } catch (Exception ignored) {
                return null;
            }
        }
        String prompt = normalizePrompt(
                context.getDecisionText());
        Collection<PhysicalCard> locations =
                context.getGameState()
                    .getTopLocations();
        if (locations == null) {
            return null;
        }
        PhysicalCard found = null;
        for (PhysicalCard location : locations) {
            if (location == null) {
                continue;
            }
            String link = GameUtils.getCardLink(location);
            if (!prompt.endsWith(
                    link.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (found != null
                    && found.getPermanentCardId()
                        != location.getPermanentCardId()) {
                return null;
            }
            found = location;
        }
        return found;
    }

    private PhysicalCard moverFromExtra(
            DecisionContext context) {
        Object id = context.getExtra(
                MovePhysicalCardResolver
                    .MOVER_CARD_ID_EXTRA);
        return id != null
                ? cardAt(context, String.valueOf(id))
                : null;
    }

    private PhysicalCard selectedOrigin(
            DecisionContext context) {
        Object id = context.getExtra(
                CaptureMovementMechanismFactsReader
                    .SELECTED_ORIGIN_CARD_ID_EXTRA);
        return id != null
                ? cardAt(context, String.valueOf(id))
                : null;
    }

    private PhysicalCard cardAt(
            DecisionContext context,
            String cardId) {
        if (context == null
                || context.getGameState() == null
                || cardId == null
                || "null".equals(cardId)) {
            return null;
        }
        try {
            return context.getGameState().findCardById(
                    Integer.parseInt(cardId));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean hasCaptureContext(
            DecisionContext context) {
        return context != null
                && context.getGame() != null
                && context.getGameState() != null
                && context.getPlayerId() != null
                && CaptureObjectiveFacts.objectiveKind(
                    context.getObjectiveAnalyzer()) != null;
    }

    private boolean isActionChoice(
            DecisionContext context) {
        return "CARD_ACTION_CHOICE".equals(
                    context.getDecisionType())
                || "ACTION_CHOICE".equals(
                    context.getDecisionType());
    }

    private boolean isSelectable(
            List<Boolean> selectable,
            int index) {
        return selectable == null
                || selectable.isEmpty()
                || index >= selectable.size()
                || Boolean.TRUE.equals(
                    selectable.get(index));
    }

    private boolean ownsSpecialChildCaptureCredit(
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism) {
        return mechanism
                == CaptureMovementMechanismFactsReader
                    .Mechanism.DOCKING_BAY_TRANSIT
                || mechanism
                    == CaptureMovementMechanismFactsReader
                        .Mechanism.VADERS_CASTLE
                || mechanism
                    == CaptureMovementMechanismFactsReader
                        .Mechanism.MACHINATION_RELOCATE
                || mechanism
                    == CaptureMovementMechanismFactsReader
                        .Mechanism.TRANSPORT;
    }

    private boolean ownsSpecialParentCaptureCredit(
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism) {
        return mechanism
                == CaptureMovementMechanismFactsReader
                    .Mechanism.SHUTTLE
                || mechanism
                    == CaptureMovementMechanismFactsReader
                        .Mechanism.EMBARK
                || mechanism
                    == CaptureMovementMechanismFactsReader
                        .Mechanism.DISEMBARK
                || ownsSpecialChildCaptureCredit(mechanism);
    }

    private boolean ownsSpecialParentApproachCredit(
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism) {
        return ownsSpecialParentCaptureCredit(mechanism);
    }

    private String standardParentText(
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism) {
        return switch (mechanism) {
            case LANDSPEED -> "Move using landspeed";
            case SHUTTLE -> "Shuttle";
            case EMBARK -> "Embark";
            case DISEMBARK -> "Disembark";
            default -> "";
        };
    }

    private String normalizePrompt(String text) {
        String normalized = text == null ? ""
                : text.trim().toLowerCase(Locale.ROOT);
        String suffix = ", or click 'done' to cancel";
        return normalized.endsWith(suffix)
                ? normalized.substring(
                    0, normalized.length()
                        - suffix.length()).trim()
                : normalized;
    }

    private record ChildMechanism(
            CaptureMovementMechanismFactsReader.Mechanism
                    mechanism,
            CaptureMovementMechanismFactsReader.ChoiceStep
                    step,
            PhysicalCard actionCard,
            String actionText,
            Action liveAction) {
        private static ChildMechanism unknown() {
            return new ChildMechanism(
                CaptureMovementMechanismFactsReader
                    .Mechanism.UNKNOWN,
                CaptureMovementMechanismFactsReader
                    .ChoiceStep.UNKNOWN,
                null, "", null);
        }
    }
}
