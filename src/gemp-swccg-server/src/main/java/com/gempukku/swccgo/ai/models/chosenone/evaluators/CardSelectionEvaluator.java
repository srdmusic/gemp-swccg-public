package com.gempukku.swccgo.ai.models.chosenone.evaluators;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitFacts;
import com.gempukku.swccgo.ai.models.common.phase.BattleForfeitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossFacts;
import com.gempukku.swccgo.ai.models.common.phase.ForceLossPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BattleWeaponsFacts;
import com.gempukku.swccgo.ai.models.common.phase.BattleWeaponsPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BhbmForceDripUrgencyFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.BhbmSetupPayoffFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectiveFacts;
import com.gempukku.swccgo.ai.models.common.phase.CaptureObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.phase.CaptureVirtualHutChoicePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployFormationSitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployCardValueFacts;
import com.gempukku.swccgo.ai.models.common.phase.DeployCardValuePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployPilotShipPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployPlanPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeploySitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployWeaponPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployObjectiveSitingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveAbilityPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDestinationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveDrainRoutingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveLandoStayPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveGateHoldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveObjectiveConsolidationPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MovePhysicalCardResolver;
import com.gempukku.swccgo.ai.models.common.phase.MoveSpyFollowPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MoveTransitPolicy;
import com.gempukku.swccgo.ai.models.common.phase.NabooDuelObjectivePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullDeployCandidatePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullSelectionCandidateFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullSelectionCandidatePolicy;
import com.gempukku.swccgo.ai.models.common.phase.PullTakeCandidateFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullTakeCandidatePolicy;
import com.gempukku.swccgo.ai.models.common.phase.ResponsePolicy;
import com.gempukku.swccgo.ai.models.common.phase.ShieldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.SetupFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.SetupPolicy;
import com.gempukku.swccgo.ai.models.common.phase.TargetSelectionFacts;
import com.gempukku.swccgo.ai.models.common.phase.TargetSelectionPolicy;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveScoringPolicy;
import com.gempukku.swccgo.ai.models.common.policy.PolicyContributionLedger;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.strategy.EndorOperationsTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldFacts;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentInstruction;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgCardBlueprintLibrary;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.logic.GameUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
// V295 RETIRED: import java.util.Random;

/**
 * Evaluates CARD_SELECTION and ARBITRARY_CARDS decisions.
 *
 * These are decisions where the player must select one or more cards
 * from a list (e.g., choosing where to deploy, which card to forfeit,
 * targeting for weapons, etc.).
 *
 * Decision types handled:
 * - "choose card to set sabacc value" -> Random selection
 * - "choose where to deploy" -> Pick best location
 * - "choose force to lose" -> Pick best card to lose
 * - "choose a card from battle to forfeit" -> Pick lowest forfeit value
 * - "choose a pilot" -> Pick best pilot
 * - "choose card to cancel" -> Cancel opponent's cards, not ours
 * - "choose target" -> Weapon/ability targeting
 *
 * Ported from Python card_selection_evaluator.py
 */
public class CardSelectionEvaluator extends ActionEvaluator {

    // Score constants
    private static final float VERY_GOOD_DELTA = 150.0f;
    private static final float VERY_BAD_DELTA = -150.0f;
    private static final SwccgCardBlueprintLibrary FALLBACK_LIBRARY = new SwccgCardBlueprintLibrary();

    // V29: All unique starship names that appear in character game text.
    // Characters referencing these should deploy aboard the named ship, not to ground.
    // "avenger" excluded — false-matches "scavenger" in unrelated card text.
    // Generic pilot text ("adds X to power if piloting") is NOT a ship reference.
    // All unique starship names from the card database (171 unique ships, ~130 core names).
    // Used to detect characters whose game text references a specific ship.
    // Compound "X in Y" variants excluded — core ship name is already in list.
    private static final String[] UNIQUE_SHIP_NAMES = {
        // Major capital ships
        "executor", "home one", "chimaera", "devastator",
        "blockade flagship", "finalizer", "fulminatrix",
        "judicator", "tyrant", "thunderflare", "resolute",
        "accuser", "dominator", "endurance", "falleen's fist",
        "intimidator & persecutor", "liberator", "profundity",
        "defiance", "independence", "liberty", "redemption",
        "stalker", "conquest", "supremacy", "steadfast",
        "flagship executor",
        // Freighters, transports, shuttles, personal ships
        "slave i", "hound's tooth", "outrider", "punishing one",
        "meson martinet", "invisible hand", "tantive iv",
        "pulsar skate", "radiant vii", "azure angel",
        "bestoon legacy", "night buzzard", "queen's royal starship",
        "tydirium", "vader's custom tie", "vader's personal shuttle",
        "millennium falcon", "the falcon", "ig-2000", "virago",
        "wild karrde", "mist hunter", "rogue one",
        "emperor's personal shuttle", "jabba's space cruiser",
        "ghost", "phantom", "libertine", "luminous", "masanya",
        "quantum storm", "spiral", "first light", "bright hope",
        "lightmaker", "liswarr", "binder", "overseer", "visage",
        "din djarin's modified n-1", "odd ball's torrent starfighter",
        "plo koon's jedi starfighter", "bo-katan's gauntlet starfighter",
        "maul's sith infiltrator", "leia's resistance transport",
        "kylo ren's command shuttle", "kylo ren's tie silencer",
        "the emperor's shield", "the emperor's sword",
        "stolen first order tie fighter",
        "blockade support ship",
        // Starfighter squadrons — Red
        "red 1", "red 2", "red 3", "red 5", "red 6", "red 7",
        "red 8", "red 9", "red 10", "red 12",
        "red squadron 1", "red squadron 4", "red squadron 6", "red squadron 7",
        // Starfighter squadrons — Gold
        "gold 1", "gold 2", "gold 3", "gold 4", "gold 5", "gold 6",
        "gold squadron 1",
        // Starfighter squadrons — Blue, Green, Gray
        "blue squadron 1", "blue squadron 5",
        "green squadron 1", "green squadron 3",
        "gray squadron 1", "gray squadron 2",
        // Starfighter squadrons — Black, Obsidian, Onyx
        "black 2", "black 3", "black 4", "black 5", "black 6", "black 11",
        "obsidian 7", "obsidian 8", "obsidian 10",
        "onyx 1", "onyx 2",
        // Starfighter squadrons — Saber, Scimitar, Scythe, Bravo, Tala
        "saber 1", "saber 2", "saber 3", "saber 4",
        "scimitar 1", "scimitar 2",
        "scythe 1", "scythe 3",
        "bravo 1", "bravo 2", "bravo 3", "bravo 4", "bravo 5",
        "bravo fighter",
        "tala 1", "tala 2",
        // Special / misc
        "death star assault squadron", "dfs-1015", "dfs-1308", "dfs-327",
        "stinger", "avenger", "vengeance",
        // Generic ship type references (still mean "deploy aboard a ship")
        "capital starship", "star destroyer", "super star destroyer"
    };

    // False-positive phrases: game text containing these should NOT count as a ship reference.
    // "scavenger" contains "avenger", "poison stinger" contains "stinger", etc.
    private static final String[] SHIP_NAME_FALSE_POSITIVES = {
        "scavenger",           // contains "avenger" — Jawas, Dathcha, etc.
        "poison stinger",      // contains "stinger" — Florn Lamproid ability
        "vengeance of the dark prince"  // contains "vengeance" — unrelated card reference
    };

    // V295 RETIRED (unused, zero reads): private final Random random = new Random();

    public CardSelectionEvaluator() {
        super("CardSelection");
    }

    private void applySetupContributions(
            EvaluatedAction action,
            List<SetupPolicy.Contribution> contributions) {
        for (SetupPolicy.Contribution contribution : contributions) {
            action.addReasoning(contribution.reason(), contribution.delta());
            logger.warn("SETUP {}: {} ({})",
                    contribution.branch(), contribution.reason(), contribution.delta());
        }
    }

    private void applySetupContribution(
            EvaluatedAction action, SetupPolicy.Contribution contribution) {
        if (contribution != null) {
            applySetupContributions(action, List.of(contribution));
        }
    }

    private boolean isCaptureSetupHutDecision(
            DecisionContext context) {
        return context != null
                && context.getTurnNumber() <= 0
                && context.getDecisionText() != null
                && "Choose Chief Chirpa's Hut to deploy"
                    .equalsIgnoreCase(
                        context.getDecisionText().trim())
                && CaptureObjectiveFacts.objectiveKind(
                    context.getObjectiveAnalyzer())
                    == CaptureObjectivePolicy.ObjectiveKind
                        .TIGIH;
    }

    private List<EvaluatedAction> evaluateCaptureSetupHut(
            DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        int count = Math.max(
                cardIds != null ? cardIds.size() : 0,
                blueprints != null ? blueprints.size() : 0);
        List<String> candidateBlueprints =
                new ArrayList<>();
        boolean virtualSelectable = false;
        boolean baseSelectable = false;
        for (int i = 0; i < count; i++) {
            String blueprintId =
                    captureSetupBlueprintAt(context, i);
            candidateBlueprints.add(blueprintId);
            if (!isCardSelectable(context, i)) {
                continue;
            }
            virtualSelectable |=
                    "214_19".equals(blueprintId);
            baseSelectable |=
                    "8_71".equals(blueprintId);
        }
        boolean bothHutPrintingsSelectable =
                virtualSelectable && baseSelectable;
        for (int i = 0; i < count; i++) {
            if (!isCardSelectable(context, i)) {
                continue;
            }
            String actionId =
                    cardIds != null && i < cardIds.size()
                            && cardIds.get(i) != null
                            && !cardIds.get(i).isBlank()
                        ? cardIds.get(i) : String.valueOf(i);
            String blueprintId =
                    candidateBlueprints.get(i);
            EvaluatedAction action = new EvaluatedAction(
                    actionId,
                    ActionType.DEPLOY,
                    50.0f,
                    "Deploy Chief Chirpa's Hut");
            PolicyContributionLedger ledger =
                    new PolicyContributionLedger(
                        "tigih-setup-hut-" + actionId);
            ledger.register(
                CaptureObjectivePolicy.scoreSetupHut(
                    new CaptureObjectivePolicy.SetupHutFacts(
                        actionId,
                        CaptureObjectivePolicy.ObjectiveKind
                            .TIGIH,
                        blueprintId != null
                            ? blueprintId : "",
                        bothHutPrintingsSelectable)));
            PolicyOperationAdapter.apply(action, ledger);
            actions.add(action);
        }
        return actions;
    }

    private String captureSetupBlueprintAt(
            DecisionContext context, int index) {
        List<String> blueprints = context.getBlueprints();
        if (blueprints != null && index >= 0
                && index < blueprints.size()
                && blueprints.get(index) != null
                && !blueprints.get(index).isBlank()) {
            return blueprints.get(index);
        }
        List<String> cardIds = context.getCardIds();
        if (context.getGameState() == null
                || cardIds == null || index < 0
                || index >= cardIds.size()) {
            return null;
        }
        try {
            PhysicalCard card =
                    context.getGameState().findCardById(
                        Integer.parseInt(cardIds.get(index)));
            return card != null
                    ? card.getBlueprintId(true) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean applyCaptureMoveDestination(
            DecisionContext context,
            EvaluatedAction action,
            PhysicalCard mover,
            PhysicalCard destination) {
        CaptureObjectivePolicy.ObjectiveKind objective =
                CaptureObjectiveFacts.objectiveKind(
                    context.getObjectiveAnalyzer());
        if (objective == null || action == null
                || mover == null || destination == null
                || action.getActionId() == null
                || action.getActionId().isBlank()) {
            return false;
        }
        boolean guaranteedImmediateCapture =
                CaptureObjectiveFacts
                    .guaranteesImmediateCaptureAt(
                        context.getGame(),
                        context.getPlayerId(),
                        context.getObjectiveAnalyzer(),
                        mover,
                        destination);
        boolean stableBack =
                CaptureObjectiveFacts.stableBackState(
                    context.getGame(),
                    context.getPlayerId(),
                    context.getObjectiveAnalyzer());
        boolean breaksStableBack =
                CaptureObjectiveFacts
                    .wouldBreakStableBackByMovingTo(
                        context.getGame(),
                        context.getPlayerId(),
                        context.getObjectiveAnalyzer(),
                        mover,
                        destination);
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                    "capture-move-destination-"
                        + action.getActionId());
        ledger.register(
            CaptureObjectivePolicy.scoreCaptureRoute(
                new CaptureObjectivePolicy.CaptureRouteFacts(
                    action.getActionId(),
                    objective,
                    CaptureObjectivePolicy.CaptureRouteStep
                        .DESTINATION,
                    guaranteedImmediateCapture)));
        ledger.register(
            CaptureObjectivePolicy.scoreStableBackHold(
                new CaptureObjectivePolicy.StableBackFacts(
                    action.getActionId(),
                    objective,
                    context.getObjectiveAnalyzer().isFlipped(),
                    stableBack,
                    breaksStableBack)));
        ledger.register(
            CaptureObjectivePolicy.scoreBhbmYourDestiny(
                new CaptureObjectivePolicy.BhbmYourDestinyFacts(
                    action.getActionId(),
                    BhbmSetupPayoffFactsReader
                        .rewardsVaderAtBattleground(
                            context.getGame(),
                            context.getPlayerId(),
                            context.getObjectiveAnalyzer(),
                            mover,
                            destination)
                        && BhbmSetupPayoffFactsReader
                            .projectedVaderMoveFormationSafe(
                                context.getGame(),
                                context.getPlayerId(),
                                mover,
                                destination))));
        PolicyOperationAdapter.apply(action, ledger);
        return guaranteedImmediateCapture;
    }

    private boolean applyCaptureDeployDestination(
            DecisionContext context,
            EvaluatedAction action,
            PhysicalCard deployingCard,
            PhysicalCard destination) {
        CaptureObjectivePolicy.ObjectiveKind objective =
                CaptureObjectiveFacts.objectiveKind(
                    context.getObjectiveAnalyzer());
        if (objective == null || action == null
                || deployingCard == null || destination == null
                || action.getActionId() == null
                || action.getActionId().isBlank()) {
            return false;
        }
        boolean guaranteedImmediateCapture =
                CaptureObjectiveFacts
                    .guaranteesImmediateCaptureAt(
                        context.getGame(),
                        context.getPlayerId(),
                        context.getObjectiveAnalyzer(),
                        deployingCard,
                        destination);
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                    "capture-deploy-destination-"
                        + action.getActionId());
        ledger.register(
            CaptureObjectivePolicy.scoreDeployCaptureRoute(
                new CaptureObjectivePolicy.DeployCaptureFacts(
                    action.getActionId(),
                    objective,
                    CaptureObjectivePolicy.CaptureRouteStep
                        .DESTINATION,
                    guaranteedImmediateCapture)));
        ledger.register(
            CaptureObjectivePolicy.scoreBhbmYourDestiny(
                new CaptureObjectivePolicy.BhbmYourDestinyFacts(
                    action.getActionId(),
                    BhbmSetupPayoffFactsReader
                        .rewardsVaderForDeployAt(
                            context.getGame(),
                            context.getPlayerId(),
                            context.getObjectiveAnalyzer(),
                            deployingCard,
                            destination))));
        PolicyOperationAdapter.apply(action, ledger);
        return guaranteedImmediateCapture;
    }

    private void applyBhbmForceDripUrgency(
            DecisionContext context,
            EvaluatedAction action,
            PhysicalCard candidate,
            PhysicalCard destination,
            boolean safe,
            BhbmForceDripUrgencyFactsReader.CandidateMechanism
                    mechanism) {
        if (context == null || action == null
                || action.getActionId() == null
                || action.getActionId().isBlank()) {
            return;
        }
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                    "bhbm-force-drip-"
                        + action.getActionId());
        ledger.register(
            CaptureObjectivePolicy
                .scoreBhbmForceDripUrgency(
                    BhbmForceDripUrgencyFactsReader.read(
                        action.getActionId(),
                        context.getGame(),
                        context.getPlayerId(),
                        context.getObjectiveAnalyzer(),
                        bhbmActionSource(context),
                        candidate,
                        destination,
                        safe,
                        mechanism)));
        PolicyOperationAdapter.apply(action, ledger);
    }

    private PhysicalCard bhbmActionSource(
            DecisionContext context) {
        if (context == null
                || context.getGameState() == null) {
            return null;
        }
        Object sourceId = context.getExtra(
                BhbmForceDripUrgencyFactsReader
                    .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA);
        if (sourceId == null) {
            return null;
        }
        try {
            return context.getGameState()
                    .findCardByPermanentId(
                        Integer.parseInt(
                            String.valueOf(sourceId)));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isFirstOrderReignsNavyRouteSelection(
            DecisionContext context) {
        if (context == null) {
            return false;
        }
        PhysicalCard source =
                bhbmActionSource(context);
        if (context.getPhase() != Phase.DEPLOY
                || context.getDecisionText() == null
                || context.getObjectiveAnalyzer() == null
                || source == null
                || !"225_24".equals(
                    source.getBlueprintId(true))) {
            return false;
        }
        String text =
                context.getDecisionText().trim();
        boolean navyPrompt =
                "CARD_SELECTION".equals(
                        context.getDecisionType())
                    && "Choose card from hand, or click 'Done' to cancel"
                        .equals(text)
                || "ARBITRARY_CARDS".equals(
                    context.getDecisionType())
                    && text.toLowerCase(Locale.ROOT)
                        .startsWith(
                            "choose card to deploy from reserve deck simultaneously with");
        return navyPrompt
                && context.getObjectiveAnalyzer()
                    .isFirstOrderReignsNavyRouteSelectionPrompt(
                        context.getGame(),
                        context.getPlayerId(),
                        source,
                        text);
    }

    private List<EvaluatedAction>
            evaluateFirstOrderReignsNavyRouteSelection(
                    DecisionContext context) {
        List<EvaluatedAction> actions =
                new ArrayList<>();
        List<String> cardIds =
                context.getCardIds();
        List<String> blueprints =
                context.getBlueprints();
        List<Boolean> selectable =
                context.getSelectable();
        boolean reserveSelection =
                "ARBITRARY_CARDS".equals(
                    context.getDecisionType());
        PhysicalCard source =
                bhbmActionSource(context);
        for (int index = 0;
                cardIds != null
                    && index < cardIds.size();
                index++) {
            String cardId =
                    cardIds.get(index);
            PhysicalCard candidate = null;
            if (reserveSelection
                    && blueprints != null
                    && index < blueprints.size()) {
                candidate =
                        findExactReservePullCandidate(
                            context, cardId,
                            blueprints.get(index));
            } else if (context.getGameState()
                    != null) {
                try {
                    candidate =
                            context.getGameState()
                                .findCardById(
                                    Integer.parseInt(
                                        cardId));
                } catch (NumberFormatException ignored) {
                    // Exact hand decisions use physical numeric ids.
                }
            }
            boolean advancesRoute =
                    candidate != null
                    && (reserveSelection
                        ? context.getObjectiveAnalyzer()
                            .isFirstOrderReignsNavyReserveRouteCandidate(
                                context.getGame(),
                                context.getPlayerId(),
                                source,
                                context.getDecisionText(),
                                candidate)
                        : context.getObjectiveAnalyzer()
                            .isFirstOrderReignsNavyHandRouteCandidate(
                                context.getGame(),
                                context.getPlayerId(),
                                source,
                                context.getDecisionText(),
                                candidate));
            EvaluatedAction action =
                    new EvaluatedAction(
                        cardId,
                        ActionType.DEPLOY,
                        50.0f,
                        "Navy Of The First Order route candidate");
            PolicyContributionLedger ledger =
                    new PolicyContributionLedger(
                        "first-order-navy-route-"
                            + cardId);
            ledger.register(
                DeployObjectiveSitingPolicy
                    .scoreFirstOrderReignsNavyRouteCandidate(
                        cardId, advancesRoute));
            PolicyOperationAdapter.apply(
                    action, ledger);
            if (selectable != null
                    && index < selectable.size()
                    && !Boolean.TRUE.equals(
                        selectable.get(index))) {
                action.hardVeto(
                    "Engine marked this Navy package card non-selectable");
            }
            actions.add(action);
        }
        return actions;
    }

    private boolean bhbmDeployFormationSafe(
            DecisionContext context,
            PhysicalCard candidate,
            PhysicalCard destination) {
        if (context == null
                || context.getGame() == null
                || context.getGameState() == null
                || context.getPlayerId() == null
                || candidate == null
                || candidate.getBlueprint() == null
                || candidate.getBlueprint()
                    .getCardCategory()
                        != CardCategory.CHARACTER
                || destination == null) {
            return false;
        }
        try {
            SwccgCardBlueprint blueprint =
                    candidate.getBlueprint();
            com.gempukku.swccgo.ai.models.common.strategy
                .FormationSafety.DeployVerdict verdict =
                    com.gempukku.swccgo.ai.models.common.strategy
                        .FormationSafety.assessCharacterDeploy(
                            context.getGame(),
                            context.getGameState(),
                            context.getPlayerId(),
                            candidate,
                            blueprint.hasPowerAttribute()
                                ? blueprint.getPower() : null,
                            blueprint.hasAbilityAttribute()
                                ? blueprint.getAbility() : null,
                            candidate.isUndercover(),
                            destination,
                            context.getGameState()
                                .getForcePileSize(
                                    context.getPlayerId()),
                            blueprint.getDeployCost(),
                            null,
                            null);
            return verdict.constraint()
                    == com.gempukku.swccgo.ai.models.common.strategy
                        .FormationSafety.DeployConstraint.ALLOW;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void applyPullSelectionPolicy(EvaluatedAction action,
                                          PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "pull-selection-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyDeployPilotPolicy(EvaluatedAction action,
                                        PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "deploy-pilot-selection-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyDeploySitingPolicy(EvaluatedAction action,
                                         PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "deploy-siting-selection-" + result.producerId()
                        + "-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyDeployTacticalPolicy(EvaluatedAction action,
                                            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "deploy-tactical-residual-" + result.producerId()
                        + "-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyDeployCardValuePolicy(EvaluatedAction action,
                                            PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "deploy-card-value-selection-" + result.producerId()
                        + "-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyDeployWeaponPolicy(EvaluatedAction action,
                                         PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "deploy-weapon-selection-" + result.producerId()
                        + "-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyDeployPlanDestinationPolicy(EvaluatedAction action,
                                                  PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "deploy-plan-destination-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyResponsePolicy(EvaluatedAction action, PolicyResult result) {
        PolicyContributionLedger ledger = new PolicyContributionLedger(
                "response-cancel-selection-" + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private void applyTdigwattPolicy(
            EvaluatedAction action,
            PolicyResult result) {
        PolicyContributionLedger ledger =
            new PolicyContributionLedger(
                "tdigwatt-card-selection-"
                    + action.getActionId());
        ledger.register(result);
        PolicyOperationAdapter.apply(action, ledger);
    }

    private static PullSelectionCandidateFacts.CloudCitySite pullCloudCitySite(
            String titleLower) {
        if (titleLower.contains("upper walkway")
                || titleLower.contains("exterior walkway")) {
            return PullSelectionCandidateFacts.CloudCitySite.UPPER_WALKWAY;
        }
        if (titleLower.contains("dining room")) {
            return PullSelectionCandidateFacts.CloudCitySite.DINING_ROOM;
        }
        if (titleLower.contains("security tower")) {
            return PullSelectionCandidateFacts.CloudCitySite.SECURITY_TOWER;
        }
        if (titleLower.contains("carbonite chamber")) {
            return PullSelectionCandidateFacts.CloudCitySite.CARBONITE_CHAMBER;
        }
        return PullSelectionCandidateFacts.CloudCitySite.OTHER;
    }

    private void applyUnknownPullSelectionPolicy(
            DecisionContext context, EvaluatedAction action,
            String cardTitle, String blueprintId, CardCategory category,
            boolean blueprintKnown, boolean lossDecision, String decisionLower) {
        String titleLower = cardTitle != null
                ? cardTitle.toLowerCase(Locale.ROOT) : "";
        var objective = context.getObjectiveAnalyzer();
        boolean huntDownLightsaber = false;
        if (!lossDecision && titleLower.contains("lightsaber")) {
            huntDownLightsaber = objective != null && objective.isAnalyzed()
                    && objective.isHuntDownV();
        }
        boolean activeObjectiveFlipGate = !lossDecision
                && objective != null
                && objective.isActiveFlipGateLocationTitle(cardTitle);

        PullSelectionCandidateFacts.CloudCityMode cloudCityMode =
                PullSelectionCandidateFacts.CloudCityMode.NONE;
        if (titleLower.contains("cloud city")
                && (decisionLower.contains("sorry")
                || decisionLower.contains("interior")
                || decisionLower.contains("cloud city")
                || decisionLower.contains("battleground"))) {
            boolean slipSliding = decisionLower.contains("slip")
                    || decisionLower.contains("battleground")
                    || context.getTurnNumber() <= 0;
            boolean imSorry = decisionLower.contains("sorry")
                    || decisionLower.contains("interior");
            if (imSorry && !slipSliding) {
                cloudCityMode = PullSelectionCandidateFacts.CloudCityMode.IM_SORRY;
            } else if (slipSliding) {
                cloudCityMode = PullSelectionCandidateFacts.CloudCityMode.SLIP_SLIDING;
            }
        }

        Integer priorityProtectionScore = blueprintId != null
                && AiPriorityCards.isPriorityCard(blueprintId)
                ? AiPriorityCards.getProtectionScore(blueprintId) : null;

        PullSelectionCandidateFacts.UnknownAmsdState amsdState =
                PullSelectionCandidateFacts.UnknownAmsdState.NONE;
        if (context.getPhase() == Phase.DEPLOY && blueprintKnown
                && category == CardCategory.CHARACTER) {
            var oracle = context.getDeckOracle();
            if (oracle != null && oracle.isAnalyzed()
                    && (oracle.isCardInPlay("Alert My Star Destroyer")
                    || oracle.isCardInPlay("Alert My Star Destroyer!"))) {
                amsdState = titleLower.contains("piett")
                        ? PullSelectionCandidateFacts.UnknownAmsdState.PIETT
                        : PullSelectionCandidateFacts.UnknownAmsdState.NON_PIETT;
            }
        }

        applyPullSelectionPolicy(action,
            PullSelectionCandidatePolicy.scoreUnknownPull(
                new PullSelectionCandidateFacts.UnknownPull(
                    action.getActionId(), cardTitle, category, !lossDecision,
                    huntDownLightsaber, activeObjectiveFlipGate, cloudCityMode,
                    pullCloudCitySite(titleLower), priorityProtectionScore,
                    amsdState)));

        if (huntDownLightsaber) {
            logger.warn("V25 HUNT DOWN UNKNOWN-GAIN: {} is a lightsaber — PRIORITY (+200)", cardTitle);
        }
        if (activeObjectiveFlipGate) {
            logger.warn("OBJECTIVE GATE PULL: {} is the exact open flip site (+300)", cardTitle);
        }
        if (cloudCityMode == PullSelectionCandidateFacts.CloudCityMode.SLIP_SLIDING
                && titleLower.contains("dining room")) {
            logger.warn("V24.10 SLIP SLIDING: Dining Room +300 — grab it as starting location!");
        }
        if (amsdState == PullSelectionCandidateFacts.UnknownAmsdState.PIETT) {
            logger.warn("V24.10 AMSD SAFETY NET: Piett detected — APPROVED (+500)");
        } else if (amsdState
                == PullSelectionCandidateFacts.UnknownAmsdState.NON_PIETT) {
            logger.warn("V24.10 AMSD SAFETY NET: {} is NOT Piett — HARD BLOCK (-9999)", cardTitle);
        }
    }

    private void applyBlueprintPullSelectionPolicy(
            DecisionContext context, EvaluatedAction action,
            String blueprintId, String cardTitle,
            String decisionLower, DeploymentPlan plan) {
        String titleLower = cardTitle != null
                ? cardTitle.toLowerCase(Locale.ROOT) : "";
        PullSelectionCandidateFacts.CloudCityMode cloudCityMode =
                PullSelectionCandidateFacts.CloudCityMode.NONE;
        if (titleLower.contains("cloud city")
                && (decisionLower.contains("sorry")
                || decisionLower.contains("interior")
                || decisionLower.contains("cloud city")
                || decisionLower.contains("battleground"))) {
            boolean objectivePick = context != null
                    && context.getObjectiveAnalyzer() != null
                    && context.getObjectiveAnalyzer().isTdigwattPreFlip()
                    && decisionLower.contains("choose")
                    && decisionLower.contains("site")
                    && decisionLower.contains("deploy")
                    && !decisionLower.contains("slip");
            boolean slipSliding = (decisionLower.contains("slip")
                    || decisionLower.contains("sliding")) && !objectivePick;
            boolean imSorry = decisionLower.contains("sorry")
                    || decisionLower.contains("interior");
            if (objectivePick) {
                cloudCityMode = PullSelectionCandidateFacts.CloudCityMode.OBJECTIVE;
            } else if (imSorry && !slipSliding) {
                cloudCityMode = PullSelectionCandidateFacts.CloudCityMode.IM_SORRY;
            } else if (slipSliding) {
                cloudCityMode = PullSelectionCandidateFacts.CloudCityMode.SLIP_SLIDING;
            }
        }

        PullSelectionCandidateFacts.PlanState planState =
                PullSelectionCandidateFacts.PlanState.NONE;
        String strategy = "";
        if (plan != null && !plan.getInstructions().isEmpty()) {
            if (plan.getInstructionForCard(blueprintId) != null) {
                planState = PullSelectionCandidateFacts.PlanState.IN_PLAN;
                strategy = String.valueOf(plan.getStrategy());
            } else if (plan.getHoldBackCards().contains(blueprintId)) {
                planState = PullSelectionCandidateFacts.PlanState.HOLD_BACK;
            }
        }

        applyPullSelectionPolicy(action,
            PullSelectionCandidatePolicy.scoreBlueprintPull(
                new PullSelectionCandidateFacts.BlueprintPull(
                    action.getActionId(), cloudCityMode,
                    pullCloudCitySite(titleLower), planState, strategy)));

        if (planState == PullSelectionCandidateFacts.PlanState.IN_PLAN) {
            logger.info("[ReserveDeck] {} IN PLAN - high priority", blueprintId);
        } else if (planState == PullSelectionCandidateFacts.PlanState.HOLD_BACK) {
            logger.debug("[ReserveDeck] {} should be held back", blueprintId);
        }
    }

    private void applyCountedObjectivePullPolicy(
            DecisionContext context, EvaluatedAction action,
            String cardId, String blueprintId,
            boolean lossDecision) {
        if (lossDecision || context.getGame() == null
                || context.getGameState() == null
                || context.getPlayerId() == null
                || context.getObjectiveAnalyzer() == null
                || !context.getObjectiveAnalyzer().isAnalyzed()) {
            return;
        }

        PhysicalCard candidate = findPullCandidate(
                context, cardId, blueprintId);
        PhysicalCard nativeLocationSource =
                readExactCountedLocationPullSource(context);
        PhysicalCard nativeLocationCandidate =
                nativeLocationSource != null
                    ? findExactReservePullCandidate(
                        context, cardId, blueprintId)
                    : null;
        PhysicalCard massassiPackageSource =
                readExactMassassiPackagePullSource(context);
        PhysicalCard massassiPackageCandidate =
                massassiPackageSource != null
                    ? findExactReservePullCandidate(
                        context, cardId, blueprintId)
                    : null;
        com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer
                .ObjectiveProgressCandidateRole role =
                context.getObjectiveAnalyzer()
                        .classifyPreFlipProgressCandidate(
                                context.getGame(),
                                context.getPlayerId(),
                                candidate);
        boolean dedicatedFlipGateLocation = candidate != null
                && candidate.getTitle() != null
                && context.getObjectiveAnalyzer()
                        .isActiveFlipGateLocationTitle(
                                candidate.getTitle());
        applyPullSelectionPolicy(action,
                PullSelectionCandidatePolicy.scoreMassassiWarRoom(
                        action.getActionId(),
                        nativeLocationCandidate != null
                        && context.getObjectiveAnalyzer()
                            .isPreferredMassassiWarRoomPullCandidate(
                                context.getGame(),
                                context.getPlayerId(),
                                nativeLocationCandidate)));
        applyPullSelectionPolicy(action,
                PullSelectionCandidatePolicy
                    .scoreImperialEntanglementsBackSite(
                        action.getActionId(),
                        nativeLocationCandidate != null
                        && context.getObjectiveAnalyzer()
                            .isImperialEntanglementsBackSiteExpansionCandidate(
                                context.getGame(),
                                context.getPlayerId(),
                                nativeLocationCandidate)));
        applyPullSelectionPolicy(action,
                PullSelectionCandidatePolicy
                    .scoreMassassiAttackRunPackage(
                        action.getActionId(),
                        massassiPackageCandidate != null
                        ? context.getObjectiveAnalyzer()
                            .getMassassiAttackRunPackagePullPriority(
                                context.getGame(),
                                context.getPlayerId(),
                                massassiPackageCandidate)
                        : 0));
        boolean nativeCountedLocation =
                nativeLocationSource == null
                || nativeLocationCandidate != null
                    && context.getObjectiveAnalyzer()
                        .isNativeObjectiveLocationRouteCandidate(
                            context.getGame(),
                            context.getPlayerId(),
                            nativeLocationSource,
                            nativeLocationCandidate);
        boolean countedHoldLocation =
                nativeLocationSource != null
                && nativeLocationCandidate != null
                && context.getObjectiveAnalyzer().isFlipped()
                && context.getObjectiveAnalyzer()
                    .isCountedOperativeObjectiveFamily()
                && context.getObjectiveAnalyzer()
                    .isNativeObjectiveLocationRouteCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        nativeLocationSource,
                        nativeLocationCandidate);
        applyPullSelectionPolicy(action,
                PullSelectionCandidatePolicy.scoreCountedObjectiveProgress(
                        action.getActionId(),
                        role == com.gempukku.swccgo.ai.models.common.strategy
                                .ObjectiveAnalyzer
                                .ObjectiveProgressCandidateRole
                                .REQUIRED_ACTOR,
                        role == com.gempukku.swccgo.ai.models.common
                                .strategy.ObjectiveAnalyzer
                                .ObjectiveProgressCandidateRole
                                .REQUIRED_COMPANION,
                        role == com.gempukku.swccgo.ai.models.common.strategy
                                .ObjectiveAnalyzer
                                .ObjectiveProgressCandidateRole
                                .REQUIRED_LOCATION
                                && !dedicatedFlipGateLocation
                                && nativeCountedLocation));
        applyPullSelectionPolicy(action,
                PullSelectionCandidatePolicy
                    .scoreCountedObjectiveHoldLocation(
                        action.getActionId(), countedHoldLocation));
        applyPullSelectionPolicy(action,
                PullSelectionCandidatePolicy.scoreRequiredOnTableCard(
                        action.getActionId(),
                        role == com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ON_TABLE_CARD,
                        context.getObjectiveAnalyzer()
                            .isRequiredOnTableCardPullRouteReady(
                                context.getGame(),
                                context.getPlayerId(),
                                candidate)));
        applyPullSelectionPolicy(action,
                PullSelectionCandidatePolicy
                    .scoreRequiredCardDeployEnabler(
                        action.getActionId(),
                        role == com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_ACTOR,
                        role == com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_CARD_DEPLOY_ENABLER_LOCATION));
    }

    private PhysicalCard readExactCountedLocationPullSource(
            DecisionContext context) {
        if (context == null || context.getGame() == null
                || context.getGameState() == null
                || context.getObjectiveAnalyzer() == null
                || !"ARBITRARY_CARDS".equals(context.getDecisionType())
                || context.getDecisionText() == null
                || !"choose card to deploy from reserve deck"
                    .equalsIgnoreCase(context.getDecisionText().trim())
                || context.getMin() != 1 || context.getMax() != 1) {
            return null;
        }
        try {
            var actionState = context.getGameState()
                    .getTopGameTextActionState();
            var liveAction = actionState != null
                    ? actionState.getGameTextAction() : null;
            PhysicalCard source = liveAction != null
                    ? liveAction.getActionSource() : null;
            GameTextActionId actionId = liveAction != null
                    ? liveAction.getGameTextActionId() : null;
            String actionText = liveAction != null
                    ? liveAction.getText() : null;
            var objective = context.getObjectiveAnalyzer();
            if (source == null
                    || !objective.usesObjectiveLocationPullSequence()) {
                return null;
            }
            boolean exactMassassi = objective
                    .isMassassiBaseOperationsFamily()
                    && actionId == GameTextActionId
                        .MASSASSI_BASE_OPERATIONS__DOWNLOAD_YAVIN_4_SITE
                    && "Deploy Yavin 4 site from Reserve Deck"
                        .equals(actionText);
            boolean exactEntanglements = objective
                    .isImperialEntanglementsFamily()
                    && actionId == GameTextActionId
                        .IMPERIAL_ENTANGLEMENTS__DOWNLOAD_TATOOINE_BATTLEGROUND_SITE
                    && "Deploy Tatooine battleground site from Reserve Deck"
                        .equals(actionText);
            boolean exactCountedOperative = objective
                    .isCountedOperativeSiteRouteAction(
                        context.getGame(), context.getPlayerId(),
                        source, actionText);
            return exactMassassi || exactEntanglements
                    || exactCountedOperative ? source : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private PhysicalCard readExactMassassiPackagePullSource(
            DecisionContext context) {
        if (context == null || context.getGame() == null
                || context.getGameState() == null
                || context.getObjectiveAnalyzer() == null
                || !"ARBITRARY_CARDS".equals(context.getDecisionType())
                || context.getDecisionText() == null
                || !"choose card to take into hand".equalsIgnoreCase(
                    context.getDecisionText().trim())
                || context.getMin() != 1 || context.getMax() != 1) {
            return null;
        }
        try {
            var actionState = context.getGameState()
                    .getTopGameTextActionState();
            var liveAction = actionState != null
                    ? actionState.getGameTextAction() : null;
            PhysicalCard source = liveAction != null
                    ? liveAction.getActionSource() : null;
            String actionText = liveAction != null
                    ? liveAction.getText() : null;
            var objective = context.getObjectiveAnalyzer();
            return source != null
                    && objective.isCurrentObjectiveSourceCard(source)
                    && liveAction.getGameTextActionId()
                        == GameTextActionId
                            .ONE_IN_A_MILLION__UPLOAD_CARD
                    && "Take card into hand from Reserve Deck"
                        .equals(actionText)
                    && objective.isMassassiAttackRunPackageUploadAction(
                        context.getGame(), context.getPlayerId(),
                        source, actionText)
                    ? source : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    /**
     * Check if gameText contains a ship name, filtering out known false positives.
     * First checks if the text contains the ship name at all. If it does,
     * verifies the match isn't actually part of a false-positive phrase
     * (e.g., "avenger" inside "scavenger").
     */
    private static boolean gameTextContainsShipName(String gameText, String shipName) {
        if (!gameText.contains(shipName)) {
            return false;
        }
        // Check if every occurrence of shipName is inside a false-positive phrase
        for (String falsePositive : SHIP_NAME_FALSE_POSITIVES) {
            if (falsePositive.contains(shipName) && gameText.contains(falsePositive)) {
                // Remove all false-positive occurrences and re-check
                String cleaned = gameText.replace(falsePositive, "");
                if (!cleaned.contains(shipName)) {
                    return false;  // Only had false-positive matches
                }
            }
        }
        return true;
    }

    /**
     * Look up card name from blueprintId using the blueprint library.
     * This is the CORRECT way to get card info - proves the bot can actually look up cards.
     */
    private String getCardNameFromBlueprint(DecisionContext context, String blueprintId) {
        if (blueprintId == null || blueprintId.isEmpty() || "inPlay".equals(blueprintId)) {
            return null;
        }

        SwccgCardBlueprintLibrary library = FALLBACK_LIBRARY;
        if (library == null) {
            logger.warn("⚠️ Cannot look up blueprint '{}' - library is null", blueprintId);
            return null;
        }

        try {
            SwccgCardBlueprint blueprint = library.getSwccgoCardBlueprint(blueprintId);
            if (blueprint != null) {
                String title = blueprint.getTitle();
                logger.info("✅ BLUEPRINT LOOKUP SUCCESS: '{}' -> '{}'", blueprintId, title);
                return title;
            } else {
                logger.warn("⚠️ Blueprint '{}' not found in library", blueprintId);
            }
        } catch (Exception e) {
            logger.warn("⚠️ Error looking up blueprint '{}': {}", blueprintId, e.getMessage());
        }
        return null;
    }

    /**
     * Get the blueprint object from blueprintId for accessing card properties.
     */
    private SwccgCardBlueprint getBlueprintFromId(DecisionContext context, String blueprintId) {
        if (blueprintId == null || blueprintId.isEmpty() || "inPlay".equals(blueprintId)) {
            return null;
        }

        SwccgCardBlueprintLibrary library = FALLBACK_LIBRARY;
        if (library == null) return null;

        try {
            return library.getSwccgoCardBlueprint(blueprintId);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();
        return "CARD_SELECTION".equals(decisionType) || "ARBITRARY_CARDS".equals(decisionType);
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        String text = context.getDecisionText();
        String textLower = text != null ? text.toLowerCase(Locale.ROOT) : "";

        // ========== CRITICAL DEBUG LOGGING ==========
        logger.warn("🚀🚀🚀 [CardSelectionEvaluator.evaluate] ENTRY POINT - JAR VERSION 2026-02-23-V21 🚀🚀🚀");
        logger.warn("🔍 Decision type: {}", context.getDecisionType());
        logger.warn("🔍 Decision text (FULL): {}", text);

        // V21 terminal decision remains ahead of every other card-selection route.
        if (SetupPolicy.isSetupTurn(context.getTurnNumber())) {
            GameState startGameState = context.getGameState();
            List<String> startCardIds = context.getCardIds();
            List<SetupPolicy.StartingCandidate> startCandidates = new ArrayList<>();
            if (startGameState != null && startCardIds != null) {
                for (String cid : startCardIds) {
                    String title = null;
                    try {
                        PhysicalCard pc = startGameState.findCardById(Integer.parseInt(cid));
                        if (pc != null) {
                            title = pc.getTitle();
                        }
                    } catch (Exception e) {
                        // Preserve the real-card-only V21 lookup.
                    }
                    startCandidates.add(new SetupPolicy.StartingCandidate(cid, title));
                }
            }
            SetupPolicy.EarlyBanEvaluation startBan =
                    SetupPolicy.earlyStartingEffectBan(startCandidates);
            if (startBan.terminalDecision()) {
                List<EvaluatedAction> startBanActions = new ArrayList<>();
                for (SetupPolicy.EarlyBanCandidate candidate : startBan.candidates()) {
                    startBanActions.add(new EvaluatedAction(
                        candidate.actionId(),
                        ActionType.UNKNOWN,
                        candidate.score(),
                        candidate.reason()));
                }
                logger.warn("V21 STARTING BAN: Returning {} scored actions", startBanActions.size());
                return startBanActions;
            }
        }

        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        List<Boolean> selectable = context.getSelectable();

        logger.warn("🔍 cardIds: {} items", cardIds != null ? cardIds.size() : "null");
        logger.warn("🔍 blueprints: {} items", blueprints != null ? blueprints.size() : "null");
        logger.warn("🔍 selectable array: {} items -> {}",
            selectable != null ? selectable.size() : "null",
            selectable != null && selectable.size() <= 10 ? selectable : (selectable != null ? selectable.subList(0, Math.min(10, selectable.size())) + "..." : "null"));

        // Log min/max for selection
        int min = context.getMin();
        int max = context.getMax();
        boolean noPass = context.isNoPass();
        logger.warn("🔍 Selection min={}, max={}, noPass={}", min, max, noPass);

        // Log first few card IDs and blueprints for debugging
        if (cardIds != null && !cardIds.isEmpty()) {
            logger.warn("🔍 First 5 cardIds: {}", cardIds.subList(0, Math.min(5, cardIds.size())));
        }
        if (blueprints != null && !blueprints.isEmpty()) {
            logger.warn("🔍 First 5 blueprints: {}", blueprints.subList(0, Math.min(5, blueprints.size())));
        }
        // Log testingTexts (CARD TITLES from GEMP - most reliable!)
        List<String> testingTexts = context.getTestingTexts();
        if (testingTexts != null && !testingTexts.isEmpty()) {
            logger.warn("🔍 testingTexts (CARD TITLES!): {} items", testingTexts.size());
            logger.warn("🔍 First 5 testingTexts: {}", testingTexts.subList(0, Math.min(5, testingTexts.size())));
        } else {
            logger.warn("🔍 testingTexts: null or empty - card titles unavailable!");
        }
        // ========== END CRITICAL DEBUG LOGGING ==========

        logger.info("[CardSelectionEvaluator] Evaluating: {}",
            text != null && text.length() > 60 ? text.substring(0, 60) + "..." : text);

        // Log selectable info - CRITICAL for debugging GEMP rejection issues
        if (selectable != null && !selectable.isEmpty()) {
            int selectableCount = 0;
            for (Boolean s : selectable) {
                if (s != null && s) selectableCount++;
            }
            logger.info("[CardSelectionEvaluator] {} cards total, {} selectable",
                       cardIds != null ? cardIds.size() : 0, selectableCount);

            // If NOTHING is selectable, this is likely a "verify" decision or a bug
            if (selectableCount == 0 && cardIds != null && !cardIds.isEmpty()) {
                logger.warn("⚠️⚠️⚠️ ALL {} CARDS ARE NON-SELECTABLE! This may be a 'verify' decision or GEMP bug.", cardIds.size());
                logger.warn("    Decision contains 'verify': {}", textLower.contains("verify"));
                logger.warn("    Decision contains 'unsuccessful': {}", textLower.contains("unsuccessful"));

                // === V24.7: OPPONENT DECK INTEL — SCAN DESTINY VALUES ===
                // When verifying opponent's deck, scan all visible cards for destiny values.
                // This gives us real data for BattlePredictor instead of random 0-6 guesses.
                GameState peekGameState = context.getGameState();
                com.gempukku.swccgo.ai.models.chosenone.strategy.OpponentDeckTracker tracker =
                    context.getOpponentDeckTracker();
                if (peekGameState != null && tracker != null) {
                    try {
                        float[] destinyValues = new float[cardIds.size()];
                        int idx = 0;
                        for (String peekCardId : cardIds) {
                            try {
                                PhysicalCard peekCard = peekGameState.findCardById(Integer.parseInt(peekCardId));
                                if (peekCard != null && peekCard.getBlueprint() != null) {
                                    Float destiny = peekCard.getBlueprint().getDestiny();
                                    destinyValues[idx] = (destiny != null) ? destiny : -1.0f;
                                    if (destiny != null) {
                                        logger.info("V24.7 PEEK: {} — destiny {}", peekCard.getTitle(), destiny);
                                    }
                                } else {
                                    destinyValues[idx] = -1.0f;
                                }
                            } catch (NumberFormatException nfe) {
                                destinyValues[idx] = -1.0f;
                            }
                            idx++;
                        }
                        tracker.recordPeek(destinyValues, cardIds.size());
                        logger.warn("V24.7 OPPONENT INTEL: Scanned {} cards — average destiny: {}",
                            cardIds.size(), tracker.getOpponentDestinyAverage());
                    } catch (Exception e) {
                        logger.debug("V24.7: Error scanning opponent deck: {}", e.getMessage());
                    }
                }
            }
        }

        // For reserve deck selections, we may have blueprints but no cardIds
        if ((cardIds == null || cardIds.isEmpty()) &&
            (blueprints == null || blueprints.isEmpty())) {
            logger.warn("[CardSelectionEvaluator] No card IDs or blueprints in {} decision", context.getDecisionType());
            return new ArrayList<>();
        }

        if (isCaptureSetupHutDecision(context)) {
            return evaluateCaptureSetupHut(context);
        }

        if (isFirstOrderReignsNavyRouteSelection(
                context)) {
            return evaluateFirstOrderReignsNavyRouteSelection(
                    context);
        }

        if ("ARBITRARY_CARDS".equals(context.getDecisionType())
                && ("choose subjugated planet".equals(textLower.trim())
                    || "choose renegade planet".equals(textLower.trim()))
                && context.getObjectiveAnalyzer() != null
                && context.getObjectiveAnalyzer()
                    .isCountedOperativeSetupSystemChoiceOpen(
                        context.getGame())
                && cardIds != null && blueprints != null
                && !cardIds.isEmpty()
                && cardIds.size() == blueprints.size()) {
            return evaluateStartingLocation(context);
        }

        // If we have blueprints but no cardIds, handle reserve deck selection
        if ((cardIds == null || cardIds.isEmpty()) && blueprints != null && !blueprints.isEmpty()) {
            logger.info("[CardSelectionEvaluator] Reserve deck selection with {} blueprints", blueprints.size());
            return evaluateReserveDeckSelection(context, textLower);
        }

        logger.debug("[CardSelectionEvaluator] {} cards to evaluate", cardIds.size());

        // Route to specific handlers based on decision text
        if (textLower.contains("choose card to set sabacc value")) {
            return evaluateSabaccSetValue(context);
        } else if (textLower.contains("choose") && textLower.contains("clone")) {
            return evaluateSabaccClone(context);
        } else if (textLower.contains("choose where to deploy")) {
            // V67v (Steve, 2026-05-03): Routing precedence bug. This branch caught
            // turn-0 starting-location decisions BEFORE V67r could route them to
            // evaluateStartingLocation. Result: all V67o/p/q/r + V29.14 Funeral Pyre +
            // V24.10 CC Exterior + V67q Sith logic was bypassed for the starting deploy.
            // Steve's symptom: 'Rando picked a Tatooine site as his Luke Saga starting
            // location instead of Endor: Funeral Pyre (V29.14 should give +1000).'
            if (context.getTurnNumber() <= 0) {
                logger.warn("V67v STARTING DEPLOY: turn 0 'where to deploy' → evaluateStartingLocation (was missed by precedence bug)");
                return evaluateStartingLocation(context);
            }
            return evaluateDeployLocation(context);
        } else if (textLower.contains("force to lose or") && textLower.contains("forfeit")) {
            // COMBINED decision: lose force OR forfeit card - MUST check before individual handlers!
            // Critical: Attrition MUST be satisfied by forfeiting, battle damage can be either
            return evaluateForceLossOrForfeit(context);
        } else if (textLower.contains("choose force to lose")) {
            return evaluateForceLoss(context);
        } else if (textLower.contains("choose a card from battle to forfeit") ||
                   textLower.contains("forfeit")) {
            return evaluateForfeit(context);
        } else if (textLower.contains("simultaneously deploy aboard")) {
            // Simultaneous pilot deployment - special handling
            return evaluateSimultaneousPilotSelection(context);
        } else if (textLower.contains(
                "choose where to embark")) {
            return evaluateObjectiveEmbarkTarget(context);
        } else if (textLower.contains("choose a pilot") ||
                   (textLower.contains("pilot") && (textLower.contains("choose") || textLower.contains("select"))) ||
                   (textLower.contains("matching") && textLower.contains("starship")
                        && !textLower.contains("into hand") && !textLower.contains("prison"))) {
            // V22.7: Broadened to catch AMSD pilot selection — GEMP text may say
            // "Choose a unique pilot character" which doesn't match "choose a pilot"
            // V22.7 ADJUSTED 2026-07-10 (AMN hang, replay 2jg1sj0l3qrlgy6a): the matching/starship
            // catch-all also matched "Choose a prison and a bounty hunter (may also choose a matching
            // weapon and/or starship)" — a take-INTO-HAND combination from Any Methods Necessary — and
            // routed it to pilot logic, which ignores selectable[] and answered a non-selectable card
            // (engine rejects, mediator swallows, game hangs). Exclude into-hand/prison texts; the
            // DecisionSafety SELECTABLE-CLAMP is the class-level backstop.
            return evaluatePilotSelection(context);
        } else if (textLower.contains("choose card to cancel")) {
            return evaluateCancelSelection(context);
        } else if (isHuntDownVaderRecallTargetDecision(context)) {
            return evaluateHuntDownVaderRecallTarget(context);
        } else if (isCaptureVirtualHutMoveDecision(context)) {
            return evaluateCaptureVirtualHutMove(context);
        } else if (isHuntDownCastleMoveDecision(context)) {
            return evaluateHuntDownCastleMove(context);
        } else if (isObjectiveDockingTransitDecision(context)) {
            return evaluateObjectiveDockingTransit(context);
        } else if (textLower.contains("move to,")
                   || textLower.contains("where to move")
                   || textLower.contains("where to shuttle")
                   || textLower.contains("where to disembark")
                   || (textLower.contains("move") && textLower.contains("to")
                       && !textLower.contains("choose target")
                       && !textLower.contains("cardhint"))) {
            // V63 ROUTING FIX: "Choose card to move to, or click 'Done' to cancel"
            // is the DESTINATION-selection decision. It must route to
            // evaluateMoveDestination BEFORE the generic "click 'done' to cancel"
            // branch — otherwise move-destination decisions fall through to
            // evaluateTargetSelection (which scores them as "target opponent's
            // card" +50), bypassing V62 SPLIT SITE and V62 SPY DILUTION logic.
            //
            // V67d ADDITION: "Choose where to move <Luke> using landspeed" is
            // ALSO destination selection — the cardHint here is the CHARACTER
            // being moved, not the destination. The "where to move" prefix
            // distinguishes it from character-selection text "card to move to <X>".
            // FIXES awjc89tacm7cxvtv replay: Rando moved Luke STU↔STG repeatedly
            // because both options scored +120 (generic target +50 +20) instead
            // of running through evaluateMoveDestination's drain/BG-aware scoring.
            return evaluateMoveDestination(context);
        } else if (textLower.contains("choose target") ||
                   textLower.contains("click 'done' to cancel")) {
            // === V42: SHIELD CHECK — must come before other routing in this branch ===
            // K&D shield selection uses "Choose card, or click 'Done' to cancel" which
            // matches this branch. Check if all choices are shields FIRST.
            if (isShieldSelectionByContent(context)) {
                logger.warn("V42 SHIELD ROUTING FIX: 'click done to cancel' text but content is shields → evaluateShieldSelection");
                return evaluateShieldSelection(context);
            }
            // === V24.11: AMSD ROUTING — CHECK BEFORE evaluateTargetSelection ===
            // "Choose card from hand, or click 'Done' to cancel" matches this branch,
            // but when AMSD is active and we're picking characters in deploy phase,
            // this is actually an AMSD pilot selection. Route to evaluatePilotSelection
            // so Piett-only enforcement fires. Without this, Vader gets picked and
            // the AMSD action fails because Executor isn't his matching ship.
            if (context.getPhase() == Phase.DEPLOY) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle amsdOracle = context.getDeckOracle();
                if (amsdOracle != null && amsdOracle.isAnalyzed()) {
                    boolean amsdOnTable = amsdOracle.isCardInPlay("Alert My Star Destroyer")
                        || amsdOracle.isCardInPlay("Alert My Star Destroyer!")
                        || amsdOracle.isCardInPlay("Alert My Star Destroyer! (V)");
                    if (amsdOnTable) {
                        boolean hasCharacterChoices = false;
                        GameState amsdGs = context.getGameState();
                        if (amsdGs != null && context.getCardIds() != null) {
                            for (String cid : context.getCardIds()) {
                                try {
                                    PhysicalCard rc = amsdGs.findCardById(Integer.parseInt(cid));
                                    if (rc != null && rc.getBlueprint() != null &&
                                        rc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        hasCharacterChoices = true;
                                        break;
                                    }
                                } catch (Exception e) { /* skip */ }
                            }
                        }
                        if (hasCharacterChoices) {
                            logger.warn("V24.11 AMSD ROUTING FIX: 'click done to cancel' branch but AMSD active + deploy phase + characters → routing to evaluatePilotSelection!");
                            return evaluatePilotSelection(context);
                        }
                    }
                }
            }
            return evaluateTargetSelection(context);
        } else if (textLower.contains("move") && textLower.contains("to")) {
            // Move destination selection
            return evaluateMoveDestination(context);
        } else if (textLower.contains("transit") || textLower.contains("transport")) {
            // Transit/transport destination selection
            return evaluateMoveDestination(context);
        } else if (textLower.contains("starting interrupt")) {
            // V43: Route starting interrupt selection
            return evaluateStartingInterrupt(context);
        } else if (textLower.contains("starting location")) {
            return evaluateStartingLocation(context);
        } else if (context.getTurnNumber() <= 0
                   && textLower.contains("where to deploy")) {
            // V67r: At turn 0 (PLAY_STARTING_CARDS), the starting interrupt asks
            // "Choose where to deploy <card>" — NOT "starting location". Without
            // this routing, V67o/p/q never fire and Rando picks non-battleground
            // sites for Sith decks (Steve's Dooku deck bug, 2026-05-03).
            logger.warn("V67r STARTING DEPLOY: routing 'where to deploy' on turn 0 to evaluateStartingLocation");
            return evaluateStartingLocation(context);
        } else if (textLower.contains("site") && textLower.contains("deploy")
                   && (textLower.contains("choose") || textLower.contains("battleground"))) {
            // V26: Catch TDIGWATT objective "Choose Cloud City battleground site to deploy"
            // and similar site selection decisions. Route to starting location evaluator
            // which has exterior/interior preference logic for TDIGWATT.
            logger.warn("V26: Routing site deploy choice to evaluateStartingLocation: '{}'",
                context.getDecisionText() != null && context.getDecisionText().length() > 80
                    ? context.getDecisionText().substring(0, 80) : context.getDecisionText());
            return evaluateStartingLocation(context);
        } else if (textLower.contains("choose") && textLower.contains("location")) {
            return evaluateLocationSelection(context);
        } else if (textLower.contains("card to take into hand")) {
            return evaluateTakeIntoHand(context);
        } else if (textLower.contains("card to put on lost pile")) {
            return evaluateLostPileSelection(context);
        } else if (textLower.contains("defensive shield") ||
                   isShieldSelectionByContent(context)) {
            return evaluateShieldSelection(context);
        } else {
            // === V24.10: AMSD ROUTING CATCH ===
            // If AMSD is in play and we're choosing characters during deploy phase,
            // this is almost certainly an AMSD pilot selection that wasn't caught by
            // the regular pilot routing (decision text didn't contain "pilot").
            // Route to evaluatePilotSelection to get full Piett-only enforcement.
            if (context.getPhase() == Phase.DEPLOY) {
                com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle routeOracle = context.getDeckOracle();
                if (routeOracle != null && routeOracle.isAnalyzed()) {
                    boolean amsdOnTable = routeOracle.isCardInPlay("Alert My Star Destroyer")
                        || routeOracle.isCardInPlay("Alert My Star Destroyer!")
                        || routeOracle.isCardInPlay("Alert My Star Destroyer! (V)");
                    if (amsdOnTable) {
                        // Check if the choices include characters (i.e., pilot candidates)
                        boolean hasCharacterChoices = false;
                        GameState routeGs = context.getGameState();
                        if (routeGs != null && context.getCardIds() != null) {
                            for (String cid : context.getCardIds()) {
                                try {
                                    PhysicalCard rc = routeGs.findCardById(Integer.parseInt(cid));
                                    if (rc != null && rc.getBlueprint() != null &&
                                        rc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        hasCharacterChoices = true;
                                        break;
                                    }
                                } catch (Exception e) { /* skip */ }
                            }
                        }
                        if (hasCharacterChoices) {
                            logger.warn("V24.10 AMSD ROUTING CATCH: AMSD in play + deploy phase + character choices → routing to evaluatePilotSelection!");
                            return evaluatePilotSelection(context);
                        }
                    }
                }
            }
            // Unknown - create neutral scored actions
            return evaluateUnknown(context);
        }
    }

    /**
     * Sabacc value setting - random selection to break loops.
     */
    private List<EvaluatedAction> evaluateSabaccSetValue(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        for (String cardId : context.getCardIds()) {
            // V24.5: No randomness — use deterministic score
            float sabaccScore = 0.0f;

            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                sabaccScore,
                "Set sabacc value (card " + cardId + ")"
            );
            action.addReasoning("Sabacc value (deterministic)", sabaccScore);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Sabacc clone - avoid cloning.
     */
    private List<EvaluatedAction> evaluateSabaccClone(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                VERY_BAD_DELTA,
                "Clone sabacc value"
            );
            action.addReasoning("Avoid cloning sabacc cards", VERY_BAD_DELTA);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose where to deploy - evaluate locations.
     *
     * CRITICAL RULES (ported from Python card_selection_evaluator.py lines 185-400):
     * 1. Starships should NEVER deploy to docking bays (0 power!)
     * 2. Starships without pilots (and no permanent pilot icon) are weak
     * 3. Always prefer space systems over docking bays for starships
     * 4. Vehicles need EXTERIOR ground locations
     * 5. Follow the deploy plan when available
     */
    private List<EvaluatedAction> evaluateDeployLocation(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();

        // =====================================================
        // Detect what type of card we're deploying
        // =====================================================
        boolean isStarship = false;
        boolean isVehicle = false;
        boolean isCharacter = false;
        boolean isWeapon = false;  // Weapon deployment (deploys ON a character)
        boolean isAttachedDeployment = false;
        String deployingCardName = "card";

        // V29.3: Only use decision text for weapon detection ("as attached" is reliable).
        // Card type detection (character, starship, vehicle) is handled below by the
        // game state blueprint lookup — much more reliable than matching text keywords
        // like "alien" or "imperial" which appear in card names, not just type descriptions.
        String decisionText = context.getDecisionText() != null ? context.getDecisionText().toLowerCase() : "";

        if (decisionText.contains("as attached")) {
            isAttachedDeployment = true;
            deployingCardName = "attached card";
            logger.info("Detected attached-card deployment");
        }

        // =====================================================
        // Check deploy planner for target location
        // Extract the card being deployed from decision text HTML
        // Format: <div class='cardHint' value='8_35'>
        // =====================================================
        String plannedTargetId = null;
        String plannedTargetName = null;
        String deployingBlueprintId = extractBlueprintFromDecisionText(context.getDecisionText());
        PhysicalCard objectiveProgressDeployingCard = findUniqueDeployingCard(
            context, gameState, playerId, deployingBlueprintId);
        DeploymentPlan deploymentPlanSnapshot = null;
        DeploymentInstruction plannedDeployInstruction = null;

        // V29.3: BLUEPRINT-BASED CARD TYPE DETECTION
        // The decision text "Choose where to deploy •Lobot, Lando's Broker" does NOT contain
        // type keywords like "character", "alien", "droid". We need the card's actual blueprint.
        //
        // PRIMARY: Use gameState to find the card — the game engine already has all cards loaded
        //          with correct blueprints. Search hand, reserve deck, and stacked cards.
        // FALLBACK: Use the standalone FALLBACK_LIBRARY (which loads classes via reflection
        //           and may silently fail for some card sets).
        // LAST RESORT: If we're in a "Choose where to deploy" decision and nothing else matched,
        //              assume CHARACTER — the only other ground deploys are vehicles/weapons which
        //              always have distinctive keywords.
        if (deployingBlueprintId != null && !isWeapon && !isStarship && !isVehicle && !isCharacter) {
            CardCategory detectedCategory = null;
            String detectedName = null;
            String detectionMethod = null;

            // --- Method 1: Search gameState for the card by blueprint ID ---
            GameState gsForType = context.getGameState();
            String pidForType = context.getPlayerId();
            if (gsForType != null && pidForType != null) {
                try {
                    // Check hand first (most common for deploys)
                    for (PhysicalCard hc : gsForType.getHand(pidForType)) {
                        if (hc != null && hc.getBlueprint() != null) {
                            String hcBpId = hc.getBlueprintId(true);
                            if (deployingBlueprintId.equals(hcBpId)) {
                                detectedCategory = hc.getBlueprint().getCardCategory();
                                detectedName = hc.getTitle();
                                detectionMethod = "gameState.hand";
                                break;
                            }
                        }
                    }
                    // Check reserve deck (for "deploy from Reserve Deck" actions)
                    if (detectedCategory == null) {
                        for (PhysicalCard rc : gsForType.getCardPile(pidForType, com.gempukku.swccgo.common.Zone.RESERVE_DECK)) {
                            if (rc != null && rc.getBlueprint() != null) {
                                String rcBpId = rc.getBlueprintId(true);
                                if (deployingBlueprintId.equals(rcBpId)) {
                                    detectedCategory = rc.getBlueprint().getCardCategory();
                                    detectedName = rc.getTitle();
                                    detectionMethod = "gameState.reserveDeck";
                                    break;
                                }
                            }
                        }
                    }
                    // Check stacked cards (for cards deployed from under other cards, e.g. K&D shields)
                    if (detectedCategory == null) {
                        for (PhysicalCard sc : gsForType.getAllStackedCards()) {
                            if (sc != null && sc.getBlueprint() != null) {
                                String scBpId = sc.getBlueprintId(true);
                                if (deployingBlueprintId.equals(scBpId)) {
                                    detectedCategory = sc.getBlueprint().getCardCategory();
                                    detectedName = sc.getTitle();
                                    detectionMethod = "gameState.stacked";
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warn("V29.3 gameState card type lookup failed: {}", e.getMessage());
                }
            }

            // --- Method 2: FALLBACK_LIBRARY (standalone blueprint library, uses reflection) ---
            if (detectedCategory == null) {
                try {
                    SwccgCardBlueprint deployingBp = getBlueprintFromId(context, deployingBlueprintId);
                    if (deployingBp != null) {
                        detectedCategory = deployingBp.getCardCategory();
                        detectedName = deployingBp.getTitle();
                        detectionMethod = "FALLBACK_LIBRARY";
                    } else {
                        logger.warn("V29.3 FALLBACK_LIBRARY returned NULL for blueprint {}", deployingBlueprintId);
                    }
                } catch (Exception e) {
                    logger.warn("V29.3 FALLBACK_LIBRARY error for {}: {}", deployingBlueprintId, e.getMessage());
                }
            }

            // Apply detected category
            if (detectedCategory != null) {
                logger.warn("V29.3 CARD TYPE: {} ({}) is {} (via {})", detectedName, deployingBlueprintId, detectedCategory, detectionMethod);
                if (detectedCategory == CardCategory.CHARACTER) {
                    isCharacter = true;
                    deployingCardName = detectedName != null ? detectedName : "character";
                } else if (detectedCategory == CardCategory.STARSHIP) {
                    isStarship = true;
                    deployingCardName = detectedName != null ? detectedName : "starship";
                } else if (detectedCategory == CardCategory.VEHICLE) {
                    isVehicle = true;
                    deployingCardName = detectedName != null ? detectedName : "vehicle";
                } else if (detectedCategory == CardCategory.WEAPON) {
                    isWeapon = true;
                    deployingCardName = detectedName != null ? detectedName : "weapon";
                } else if (isAttachedDeployment) {
                    deployingCardName = detectedName != null ? detectedName : "attached card";
                }
            } else {
                logger.warn("V29.3 CARD TYPE: ALL methods failed for blueprint {} — type unknown!", deployingBlueprintId);
            }
        }

        // V29.3 LAST RESORT: If we're in a "Choose where to deploy" decision and still no type,
        // assume CHARACTER. The only other ground deploys (vehicles, weapons) always have
        // distinctive keywords in the decision text.
        if (!isCharacter && !isStarship && !isVehicle && !isWeapon) {
            if (decisionText.contains("choose where to deploy") || decisionText.contains("choose location to deploy")) {
                if (!decisionText.contains("starship") && !decisionText.contains("capital ship")
                    && !decisionText.contains("vehicle") && !decisionText.contains("as attached")
                    && !decisionText.contains("effect") && !decisionText.contains("interrupt")) {
                    isCharacter = true;
                    deployingCardName = "character (V29.3 last-resort)";
                    logger.warn("V29.3 LAST RESORT: Assuming CHARACTER for deploy decision: {}",
                        context.getDecisionText() != null ? context.getDecisionText().substring(0, Math.min(100, context.getDecisionText().length())) : "?");
                }
            }
        }

        DeployPhasePlanner deployPhasePlanner = context.getDeployPhasePlanner();
        if (deployPhasePlanner != null) {
            DeploymentPlan currentPlan = deployPhasePlanner.getCurrentPlan();
            if (currentPlan != null && !currentPlan.getInstructions().isEmpty()) {
                // V201: evaluators read a detached plan. Blueprint fallback is accepted
                // only when it identifies one instruction; duplicate physical copies or
                // an unknown source never inherit the first unrelated destination.
                deploymentPlanSnapshot = currentPlan.assessmentCopy();
                if (deployingBlueprintId != null) {
                    plannedDeployInstruction = objectiveProgressDeployingCard != null
                        ? deploymentPlanSnapshot.getInstructionForPhysicalCard(
                            objectiveProgressDeployingCard.getPermanentCardId(),
                            objectiveProgressDeployingCard.getCardId(),
                            deployingBlueprintId)
                        : deploymentPlanSnapshot.getInstructionForCard(
                            deployingBlueprintId);
                    if (plannedDeployInstruction != null && plannedDeployInstruction.getTargetLocationId() != null) {
                        plannedTargetId = plannedDeployInstruction.getTargetLocationId();
                        plannedTargetName = plannedDeployInstruction.getTargetLocationName();
                        logger.info("📋 Deploy plan says: {} ({}) -> {}",
                            plannedDeployInstruction.getCardName(), deployingBlueprintId, plannedTargetName);
                    } else {
                        logger.info("📋 No unique matching instruction for blueprint {}", deployingBlueprintId);
                    }
                } else {
                    logger.warn("⚠️ Could not extract deploy blueprint; refusing blind plan-target fallback");
                }
            }
        }

        // V186 (Steve, 2026-06-23): I Want That Map starting LOCATION pick. This loop
        // resolves each candidate via findCardById(parseInt(cardId)) (~line 813), which
        // throws for ARBITRARY temp IDs ("temp0"...) — the form the objective's
        // "Choose [Episode VII] location to deploy" decision uses — so the normal +150
        // objective bonus (~line 1607) never fires and every candidate ties at the +50
        // base, making the pick arbitrary (the root cause of the wrong-location report).
        // Resolve temp-safely from the parallel blueprint / testing-text lists and steer
        // the pick to the Starkiller Base SYSTEM (208_51), whose once-per-turn [download]
        // fetches the SB battleground sites that feed the 2-battleground flip. Its sites
        // (208_52..55) are different blueprint IDs, so this names the download engine, not
        // a site.
        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v186oa = context.getObjectiveAnalyzer();
        java.util.List<String> v186Ids = context.getCardIds();
        java.util.List<String> v186Bps = context.getBlueprints();
        java.util.List<String> v186Tts = context.getTestingTexts();
        // V186 CONSOLIDATED (2026-07-07): identity from ObjectiveAnalyzer.isWantThatMap().
        boolean v186IsWantThatMap = v186oa != null && v186oa.isAnalyzed() && v186oa.isWantThatMap();
        SwccgCardBlueprint boardingDeployBlueprint =
                objectiveProgressDeployingCard != null
                        ? objectiveProgressDeployingCard.getBlueprint()
                        : findDeployingBlueprint(
                            context, gameState, playerId, deployingBlueprintId);
        boolean boardingPilot = boardingDeployBlueprint != null
                && boardingDeployBlueprint.getCardCategory() == CardCategory.CHARACTER
                && boardingDeployBlueprint.hasIcon(com.gempukku.swccgo.common.Icon.PILOT);
        Float boardingAbility = boardingDeployBlueprint != null
                && boardingDeployBlueprint.hasAbilityAttribute()
                ? boardingDeployBlueprint.getAbility() : null;
        boolean boardingOpenPilotDestinationOffered = false;
        if (gameState != null && game != null
                && game.getModifiersQuerying() != null
                && objectiveProgressDeployingCard != null) {
            java.util.List<String> boardingDestinationIds = context.getCardIds();
            java.util.List<Boolean> boardingSelectable = context.getSelectable();
            for (int destinationIndex = 0;
                    destinationIndex < boardingDestinationIds.size();
                    destinationIndex++) {
                if (boardingSelectable != null
                        && destinationIndex < boardingSelectable.size()
                        && !Boolean.TRUE.equals(
                            boardingSelectable.get(destinationIndex))) {
                    continue;
                }
                String destinationId =
                        boardingDestinationIds.get(destinationIndex);
                try {
                    PhysicalCard destination =
                            gameState.findCardById(Integer.parseInt(destinationId));
                    CardCategory destinationCategory = destination != null
                            && destination.getBlueprint() != null
                            ? destination.getBlueprint().getCardCategory() : null;
                    if ((destinationCategory == CardCategory.VEHICLE
                            || destinationCategory == CardCategory.STARSHIP)
                            && Filters.hasAvailablePilotCapacity(
                                objectiveProgressDeployingCard)
                                .accepts(
                                    gameState,
                                    game.getModifiersQuerying(),
                                    destination)) {
                        boardingOpenPilotDestinationOffered = true;
                        break;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Deploy to location " + cardId
            );

            // V186: temp-safe Starkiller Base SYSTEM preference (see note above the loop).
            // +400 over the +50 base is decisive vs the other [Episode VII] candidates (which
            // stay at +50 because their objective/battleground bonuses are also unreachable on
            // the temp-id path). Names ONLY the system (208_51 / title "Starkiller Base" with
            // no ":" site suffix), so its battleground sites are not picked here.
            // Gated to temp IDs (the ARBITRARY reserve-deck pick) so it does NOT fire for
            // later real-id "deploy where" decisions — otherwise it would over-prioritize
            // deploying onto the SYSTEM instead of the battleground SITES the flip needs.
            if (v186IsWantThatMap && cardId != null && cardId.startsWith("temp")) {
                int v186Idx = v186Ids != null ? v186Ids.indexOf(cardId) : -1;
                String v186Bp = (v186Bps != null && v186Idx >= 0 && v186Idx < v186Bps.size()) ? v186Bps.get(v186Idx) : null;
                String v186Tt = (v186Tts != null && v186Idx >= 0 && v186Idx < v186Tts.size()) ? v186Tts.get(v186Idx) : null;
                // V186 CONSOLIDATED (2026-07-07): system blueprint ids + title fragment come from
                // ObjectiveAnalyzer (was hardcoded "208_51"/"208_051"/"starkiller base" here).
                java.util.Set<String> v186SysIds = v186oa.getIwtmSystemBpIds();
                String v186SysFrag = v186oa.getIwtmSystemTitleFragment();
                boolean v186BpSystem = v186Bp != null && v186SysIds != null && v186SysIds.contains(v186Bp);
                boolean v186TtSystem = v186Tt != null && v186SysFrag != null
                        && v186Tt.toLowerCase(java.util.Locale.ROOT).contains(v186SysFrag)
                        && !v186Tt.contains(":");
                applyPullSelectionPolicy(action,
                    PullSelectionCandidatePolicy.scoreIwtmLocation(
                        new PullSelectionCandidateFacts.IwtmLocation(
                            cardId, v186BpSystem || v186TtSystem)));
                if (v186BpSystem || v186TtSystem) {
                    logger.warn("V186 STARKILLER SYSTEM: cardId={} bp={} title={} (+400)", cardId, v186Bp, v186Tt);
                }
            }

            // Try to get location info
            if (gameState != null) {
                try {
                    PhysicalCard location = gameState.findCardById(Integer.parseInt(cardId));
                    if (location != null) {
                        SwccgCardBlueprint blueprint = location.getBlueprint();
                        String title = location.getTitle();
                        String titleLower = title != null ? title.toLowerCase() : "";
                        action.setDisplayText("Deploy to " + (title != null ? title : "location"));
                        TdigwattObjectiveFactsReader
                            .readVirtualDeployProjection(
                                game, playerId,
                                objectiveProgressDeployingCard,
                                location)
                            .ifPresent(projection ->
                                applyTdigwattPolicy(
                                    action,
                                    TdigwattObjectiveScoringPolicy
                                        .scoreDeploy(
                                            action.getActionId(),
                                            projection.before(),
                                            projection.after(),
                                            true)
                                        .result()));
                        boolean guaranteedCaptureDeployDestination =
                                applyCaptureDeployDestination(
                                    context,
                                    action,
                                    objectiveProgressDeployingCard,
                                    location);
                        CardCategory destinationCategory = blueprint != null
                                ? blueprint.getCardCategory() : null;
                        boolean destinationIsAsset =
                                destinationCategory == CardCategory.VEHICLE
                                || destinationCategory == CardCategory.STARSHIP;
                        boolean destinationHasOpenPilotSlot =
                                destinationIsAsset
                                && game != null
                                && game.getModifiersQuerying() != null
                                && objectiveProgressDeployingCard != null
                                && Filters.hasAvailablePilotCapacity(
                                    objectiveProgressDeployingCard)
                                    .accepts(
                                        gameState,
                                        game.getModifiersQuerying(),
                                        location);
                        applyDeployPilotPolicy(action,
                                DeployPilotShipPolicy.evaluateLowAbilityPilotBoarding(
                                        new DeployPilotShipPolicy.LowAbilityPilotBoardingFacts(
                                                action.getActionId(), boardingPilot,
                                                boardingAbility,
                                                boardingOpenPilotDestinationOffered,
                                                destinationHasOpenPilotSlot)));

                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer
                            objectiveProgressAnalyzer = context.getObjectiveAnalyzer();
                        if (objectiveProgressAnalyzer != null) {
                            com.gempukku.swccgo.ai.models.common.playbook.ObjectiveProgressAssessment
                                objectiveProgress = objectiveProgressAnalyzer.assessDeployChild(
                                    game, playerId,
                                    objectiveProgressDeployingCard, location);
                            logger.debug("V214 DEPLOY CHILD OBJECTIVE FACTS: outcome={} evidence={}",
                                objectiveProgress.outcome(), objectiveProgress.evidence());
                            boolean actorLocationProgress =
                                    objectiveProgressAnalyzer
                                            .advancesPreFlipActorAtRuntimeLocation(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location);
                            boolean countedProgress =
                                    !actorLocationProgress
                                    &&
                                    objectiveProgressAnalyzer
                                            .advancesPreFlipRequirementAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location);
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreCountedObjectiveProgress(
                                        action.getActionId(),
                                        countedProgress));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreThreeSiteObjectiveCompletion(
                                        action.getActionId(),
                                        (objectiveProgressAnalyzer
                                            .isMassassiBaseOperationsFamily()
                                                || objectiveProgressAnalyzer
                                                    .isImperialEntanglementsFamily())
                                            && objectiveProgressAnalyzer
                                                .wouldCompletePreFlipRequirementAt(
                                                    game, playerId,
                                                    objectiveProgressDeployingCard,
                                                    location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreActorRuntimeLocation(
                                        action.getActionId(),
                                        actorLocationProgress));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreRequiredOnTableCard(
                                        action.getActionId(),
                                        objectiveProgress.outcome()));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreRequiredCardDeployEnabler(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesRequiredCardDeployPrerequisiteAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                NabooDuelObjectivePolicy
                                    .scoreFrontDeployDestination(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesNabooDuelFrontTargetRouteAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreRequiredCardRetentionDefense(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesRequiredCardRetentionDefenseAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreObjectiveHardLossDefense(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesObjectiveHardLossDefenseAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scorePostFlipObjectivePayoff(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .classifyPostFlipPayoffAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreFirstOrderReignsDrainPair(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesFirstOrderReignsDrainPairAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreFirstOrderReignsRouteCrew(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesFirstOrderReignsRouteCrewAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreMassassiRebelTechPackage(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesMassassiRebelTechPackageAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreMassassiAttackRunCarrier(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .advancesMassassiAttackRunCarrierAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreMassassiAttackRunTorpedoesTarget(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .isMassassiAttackRunTorpedoesAttachmentTarget(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .scoreFirstOrderReignsNavyRouteDestination(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .isFirstOrderReignsNavyRouteDestinationCandidate(
                                                game, playerId,
                                                bhbmActionSource(context),
                                                context.getDecisionText(),
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .blockFirstOrderReignsPreFlipGround(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .isFirstOrderReignsPrematureGroundDeploymentAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                            applyDeploySitingPolicy(action,
                                DeployObjectiveSitingPolicy
                                    .blockTerminalObjectiveExposure(
                                        action.getActionId(),
                                        objectiveProgressAnalyzer
                                            .isFirstOrderReignsTerminalDeploymentHazardAt(
                                                game, playerId,
                                                objectiveProgressDeployingCard,
                                                location)));
                        }

                        // === V166 (Steve, 2026-06): CONTEST THE OPPONENT'S DRAIN by deploying to it ===
                        // When the opponent out-drains us by net >= 2 (bonus-aware; verified to fire ~half
                        // the time in self-play), DEPLOY to their drain sites to create contested sites so
                        // V164a can battle and break the drain — instead of both sides parallel-draining for
                        // 20+ turns. Deploy (unlike move) can target the opponent's site directly, which is
                        // why the move-path version never fired. Prefer the SOFTEST site (fewest opponent
                        // cards = easiest to clear or spy-block). Weight is decisive over normal deploy
                        // scoring (planned target +200, uncontested +30) but stays under the -9999 hard
                        // blocks; V164a's own guards still stop a suicide battle once the site is contested.
                        if (game != null && playerId != null) {
                            try {
                                String v166Opp = gameState.getOpponent(playerId);
                                float v166TheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, location, v166Opp, false, false);
                                int v166OppDrain = (int) game.getModifiersQuerying()
                                    .getForceDrainAmount(gameState, location, v166Opp);
                                if (v166TheirPower > 0 && v166OppDrain > 0
                                        && computeNetDrainBalance(game, gameState, playerId) >= 2) {
                                    // V177 (Steve, 2026-06): SURVIVABILITY GATE. Replay aab2jiaa5sca:
                                    // V166 lured Wild Karrde to Tatooine to "contest a drain" with no
                                    // check it could hold the site — it couldn't, so it hyperspeed-moved
                                    // to Jakku next phase, wasting 1 Force. Only award the contest bonus
                                    // when our deploy can reach near-parity there: our power + this card +
                                    // the affordable wave >= their power - 2. Otherwise the contest is a
                                    // trap; skip it so a winnable/safe site wins the deploy instead.
                                    float v166OurPow = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, playerId, false, false);
                                    float v166ThisPow = 0f;
                                    SwccgCardBlueprint v166Bp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (v166Bp != null && v166Bp.hasPowerAttribute() && v166Bp.getPower() != null)
                                        v166ThisPow = v166Bp.getPower();
                                    // V177 ADJUSTED 2026-07-10 (Rey replay rbujmoc90br3uu4c, T2 Yoda solo into
                                    // Kylo+Mara+saber): the gate passed on OPTIMISTIC self math vs DISCOUNTED
                                    // enemy math — it counted a lightsaber-in-hand wave (+5) with ZERO affordable
                                    // character buddies, and used raw enemy power blind to their weapons (raw 10,
                                    // armed ~15). Two in-place corrections: (a) only count the wave when it has at
                                    // least one AFFORDABLE BUDDY (wave[1]>=1 — the same standard V172 uses);
                                    // (b) weapon-adjust their power (V29.7 heuristic: lightsaber +5, other +3,
                                    // attached or permanent). Boundary: T2 case now 0+3+0 vs (10+10)-2=18 → GATED
                                    // (was 8 vs 8 → "survivable" → +350 lure → Yoda died, 8 battle damage).
                                    float[] v166WaveArr = v173WaveProjection(gameState, playerId, deployingBlueprintId);
                                    float v166Wave = (v166WaveArr[1] >= 1f) ? v166WaveArr[0] : 0f;
                                    float v166OppWeapons = v177OppWeaponBonus(gameState, location, v166Opp);
                                    boolean v166Survivable = (v166OurPow + v166ThisPow + v166Wave)
                                        >= (v166TheirPower + v166OppWeapons) - 2f;
                                    if (v166Survivable) {
                                        int v166OppCards = 0;
                                        for (PhysicalCard c : gameState.getCardsAtLocation(location))
                                            if (c != null && v166Opp.equals(c.getOwner())) v166OppCards++;
                                        DeployTacticalPolicy.ContestDrainFacts v166Facts =
                                            new DeployTacticalPolicy.ContestDrainFacts(
                                                action.getActionId(), title, v166TheirPower,
                                                v166OppDrain, 2, v166OurPow, v166ThisPow,
                                                v166Wave, v166OppWeapons, v166OppCards);
                                        PolicyContributionLedger v166Ledger = new PolicyContributionLedger(
                                            "deploy-tactical-v166-" + action.getActionId());
                                        v166Ledger.register(
                                            DeployTacticalPolicy.scoreV166ContestDrain(v166Facts));
                                        PolicyOperationAdapter.apply(action, v166Ledger);
                                        float v166 = v166Ledger.orderedOperations().get(0).delta();
                                        logger.warn("V166 CONTEST DRAIN (deploy): target={} oppDrain={} oppCards={} oppPower={} -> +{}",
                                            title, v166OppDrain, v166OppCards, (int) v166TheirPower, (int) v166);
                                    } else {
                                        logger.warn("V177 V166 GATED: {} contest not survivable (proj {} vs {}) — deploy elsewhere",
                                            title, (int) (v166OurPow + v166ThisPow + v166Wave), (int) v166TheirPower);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V166 deploy error: {}", e.getMessage()); }
                        }

                        // === V169 (Steve, 2026-06): PROTECT ENDANGERED CHARACTERS — deploy buddies ===
                        // Replay lk6xgsokjcwrwxuu (Steve vs Rando), two fatal moves: (1) Asajj left
                        // solo at Guest Quarters with Luke AT her site — beaten 6v27 next turn;
                        // (2) Tyranus + Aurra on Hoth facing a 5-character strike team (16v37) while
                        // Rando deployed Savage + Nute to an OPEN Cloud City site. Steve: "he needed
                        // to deploy buddies to protect his characters" — EVEN into a losing battle.
                        // When our characters at this location are outpowered, deploying here gets a
                        // dominating bonus (+800..+1100): beats open-site totals (~315-600 observed),
                        // beats V166 contest (+250..400), loses only to the -9999 hard blocks.
                        if (game != null && playerId != null) {
                            try {
                                float v169Excess = v169OppPowerExcessAt(game, gameState, playerId, location);
                                if (v169Excess > 0) {
                                    // V172 (Steve, 2026-06): REINFORCEABILITY BRAKE. Unbraked, this
                                    // bonus was a corpse conveyor: each wave of "buddies" fed into a
                                    // site Steve's stack dominated, he initiated and wiped them, the
                                    // bonus re-fired (416x in one game), and the lost pile hit 30-to-0
                                    // before V67aw conceded. Only reinforce when reinforcement can
                                    // actually close the gap: this card + best remaining hand
                                    // character must bring the deficit within 4. Beyond that the site
                                    // is unsavable by deploys — the V169 RETREAT path (move phase)
                                    // handles it instead of feeding.
                                    // V173: reinforcement potential = this card + the WHOLE affordable
                                    // wave (every other hand character within the force budget, printed
                                    // deploy costs, plus weapon weights) — not just one buddy.
                                    float v172This = 0f;
                                    boolean v172SkippedSelf = false;
                                    for (PhysicalCard v172H : gameState.getHand(playerId)) {
                                        if (v172H != null && v172H.getBlueprint() != null
                                                && v172H.getBlueprint().getCardCategory() == CardCategory.CHARACTER
                                                && !v172SkippedSelf && deployingBlueprintId != null
                                                && deployingBlueprintId.equals(v172H.getBlueprintId(true))) {
                                            Float v172P = v172H.getBlueprint().hasPowerAttribute()
                                                ? v172H.getBlueprint().getPower() : null;
                                            v172This = v172P != null ? v172P : 0f;
                                            v172SkippedSelf = true;
                                        }
                                    }
                                    float[] v172WaveR = v173WaveProjection(gameState, playerId, deployingBlueprintId);
                                    float v172Wave = v172WaveR[0];
                                    if (v172This + v172Wave >= v169Excess - 4f) {
                                        DeployTacticalPolicy.ProtectEndangeredFacts v169Facts =
                                            new DeployTacticalPolicy.ProtectEndangeredFacts(
                                                action.getActionId(), title, v169Excess,
                                                v172This, v172Wave, v172WaveR[2]);
                                        PolicyContributionLedger v169Ledger = new PolicyContributionLedger(
                                            "deploy-tactical-v169-" + action.getActionId());
                                        v169Ledger.register(
                                            DeployTacticalPolicy.scoreV169ProtectEndangered(v169Facts));
                                        PolicyOperationAdapter.apply(action, v169Ledger);
                                        float v169 = v169Ledger.orderedOperations().get(0).delta();
                                        logger.warn("V169 PROTECT (deploy): {} outpowered by {} -> +{} (wave={} reserved={})",
                                            title, (int) v169Excess, (int) v169, (int) v172Wave, (int) v172WaveR[2]);
                                    } else {
                                        logger.warn("V172 PROTECT GATED: {} outpowered by {} but only +{} affordable reinforcement (reserved={}) — unsavable by deploys, retreat instead",
                                            title, (int) v169Excess, (int) (v172This + v172Wave), (int) v172WaveR[2]);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V169 deploy error: {}", e.getMessage()); }
                        }

                        // === V170 (Steve, 2026-06): SPY -> BLOCK THEIR BEST DRAIN SITE ===
                        // Steve: "Spies cost much less to block a drain than deploying a bunch
                        // of characters to overpower opponent." When the card being deployed is
                        // a SPY, deploying it AT an opponent-occupied drain site is the cheap
                        // block (the V170 yes/no intercept in RandoCalAi answers the undercover
                        // prompt; undercover breaks their control -> drain stops). No power
                        // requirement — spies don't fight, undercover is safe. Scaled by the
                        // drain it denies, preferring their BIGGEST drain. Magnitude: beats
                        // V166 contest (+250..400) and open-site totals (~315-600) even after a
                        // V113 solo penalty (-300), but stays under V169 PROTECT (+800..1100) —
                        // endangered allies outrank a cheap block.
                        if (game != null && playerId != null && deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint v170Bp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v170Bp != null && v170Bp.hasKeyword(com.gempukku.swccgo.common.Keyword.SPY)) {
                                    String v170Opp = gameState.getOpponent(playerId);
                                    boolean v170OppHere = false;
                                    for (PhysicalCard v170C : gameState.getCardsAtLocation(location)) {
                                        if (v170C != null && v170Opp.equals(v170C.getOwner())) { v170OppHere = true; break; }
                                    }
                                    int v170Drain = (int) game.getModifiersQuerying()
                                        .getForceDrainAmount(gameState, location, v170Opp);
                                    if (v170OppHere && v170Drain >= 1) {
                                        DeployTacticalPolicy.SpyDrainFacts v170Facts =
                                            new DeployTacticalPolicy.SpyDrainFacts(
                                                action.getActionId(), title, true,
                                                v170OppHere, v170Drain);
                                        PolicyContributionLedger v170Ledger = new PolicyContributionLedger(
                                            "deploy-tactical-v170-" + action.getActionId());
                                        v170Ledger.register(
                                            DeployTacticalPolicy.scoreV170SpyDrainBlock(v170Facts));
                                        PolicyOperationAdapter.apply(action, v170Ledger);
                                        float v170 = v170Ledger.orderedOperations().get(0).delta();
                                        logger.warn("V170 SPY BLOCK: {} drain={} -> +{}", title, v170Drain, (int) v170);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V170 error: {}", e.getMessage()); }
                        }

                        // === V171 (Steve, 2026-06): DEPLOY TO CONTACT — don't deploy adjacent and march ===
                        // Replay 479h9miow1acggwb (Steve vs Rando): Rando repeatedly deployed
                        // Tyranus/Asajj/Savage to an EMPTY adjacent site (Guest Quarters/Beldon's, 340)
                        // and then landspeed-marched them into Steve's occupied site next phase —
                        // because the contested site ate first-mover penalties (V113 SOLO -300,
                        // V29.5 BUDDY -100, V136 danger ~-300) that V166's +400 couldn't beat (-200).
                        // Steve: "deployed and moved guys in front of my characters instead of just
                        // deploying to my occupied location. This is a waste of force."
                        // It's worse than force waste: SWCCG turn order is Deploy -> BATTLE -> Move,
                        // so arriving by move forfeits battle initiative every time (Steve
                        // out-initiated ~7-2). When a deploy WAVE is coming this same phase (another
                        // character in hand + force to land it), the first mover's loneliness is
                        // temporary — offset the first-mover penalties so the wave STARTS at the
                        // contested site. +600 flips the observed case (-200 -> +400 > empty 340);
                        // genuinely suicidal sites keep their extra danger terms (stack past -600)
                        // and still lose. Wave gate keeps solo-with-no-backup deploys penalized.
                        if (game != null && playerId != null) {
                            try {
                                String v171Opp = gameState.getOpponent(playerId);
                                boolean v171OppHere = false;
                                for (PhysicalCard v171C : gameState.getCardsAtLocation(location)) {
                                    if (v171C != null && v171Opp.equals(v171C.getOwner()) && !v171C.isUndercover()) {
                                        v171OppHere = true; break;
                                    }
                                }
                                // V171/V172 ADJUSTED 2026-07-11c (Codex audit H3: V171 fired on the
                                // STARSHIP First Light, borrowing hand-character wave power, +600 into
                                // Falcon+Han): contact steers are CHARACTER logic only.
                                SwccgCardBlueprint v171DeployBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v171DeployBp == null
                                        || v171DeployBp.getCardCategory() != CardCategory.CHARACTER) {
                                    v171OppHere = false;
                                }
                                if (v171OppHere) {
                                    // Wave check: at least one MORE deployable character in hand
                                    // beyond the one being deployed, and force to plausibly land it.
                                    int v171HandChars = 0;
                                    float v171ThisPower = 0f;
                                    float v171MaxHandPower = 0f;  // V171 ADJUSTED 2026-07-10b: biggest body for hit discount
                                    String v171ThisBp = deployingBlueprintId;
                                    boolean v171SkippedSelf = false;
                                    for (PhysicalCard v171H : gameState.getHand(playerId)) {
                                        if (v171H != null && v171H.getBlueprint() != null
                                                && v171H.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                            v171HandChars++;
                                            Float v171HP = v171H.getBlueprint().hasPowerAttribute()
                                                ? v171H.getBlueprint().getPower() : null;
                                            if (v171HP != null && v171HP > v171MaxHandPower) v171MaxHandPower = v171HP;
                                            if (!v171SkippedSelf && v171ThisBp != null
                                                    && v171ThisBp.equals(v171H.getBlueprintId(true))) {
                                                v171ThisPower = v171HP != null ? v171HP : 0f;
                                                v171SkippedSelf = true;
                                            }
                                        }
                                    }
                                    // V172 (Steve, 2026-06): WINNABILITY GATE. V171 without this fed
                                    // characters piecemeal into Steve's superior stacks — he initiated
                                    // every battle and wiped each installment (lost pile 30-to-0,
                                    // V67aw conceded two games running). Only walk in the front door
                                    // when the projected wave reaches near-parity (their power - 2).
                                    // V173: the projection is the WHOLE affordable wave (all hand
                                    // characters within the force budget + weapon weights).
                                    // V174: the budget reserves maintenance upkeep (table + wave) and
                                    // 1-2 force for battle interrupts FIRST; the old flat force>=4
                                    // check is replaced by "at least one buddy is genuinely
                                    // affordable after reserves".
                                    float v171OurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, playerId, false, false);
                                    float v171TheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                        gameState, location, v171Opp, false, false);
                                    float[] v171WaveR = v173WaveProjection(gameState, playerId, deployingBlueprintId);
                                    float v171Wave = v171WaveR[0];
                                    // V171 ADJUSTED 2026-07-10b (replay f27ws5lgy0g58k5p, T4 Savage+Nute →
                                    // Carbonite Chamber suicide): the projection was whole-hand optimistic vs
                                    // RAW enemy power — same hole wave 1 closed in V166. (a) weapon-adjust
                                    // their power (v177OppWeaponBonus); (b) discount the projection by expected
                                    // HITS: each armed enemy character deletes ~one of our bodies pre-destiny
                                    // (min(armedOpps, our wave bodies) × our biggest body). Boundary: T4 case
                                    // 16−8=8 vs (10+3)−2=11 → GATED (was 16 vs 8 → +600 lure → board wipe).
                                    float v171OppWeap = v177OppWeaponBonus(gameState, location, v171Opp);
                                    int v171ArmedOpps = 0;
                                    try {
                                        for (PhysicalCard v171E : gameState.getCardsAtLocation(location)) {
                                            if (v171E == null || v171E.getBlueprint() == null) continue;
                                            if (v171E.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                            if (!v171Opp.equals(v171E.getOwner())) continue;
                                            boolean v171EA = false;
                                            java.util.List<PhysicalCard> v171Atts = gameState.getAttachedCards(v171E);
                                            if (v171Atts != null) {
                                                for (PhysicalCard att : v171Atts) {
                                                    if (att != null && att.getBlueprint() != null
                                                            && att.getBlueprint().getCardCategory() == CardCategory.WEAPON) { v171EA = true; break; }
                                                }
                                            }
                                            String v171EG = v171E.getBlueprint().getGameText();
                                            if (!v171EA && v171EG != null
                                                    && v171EG.toLowerCase(Locale.ROOT).contains("permanent weapon")) v171EA = true;
                                            if (v171EA) v171ArmedOpps++;
                                        }
                                    } catch (Exception e) { /* 0 armed */ }
                                    float v171TheirEff = v171TheirPower + v171OppWeap;
                                    // V172 SOLO DOMINANCE (Steve ruling 2026-07-11, replay f27ws5lgy0g58k5p T2:
                                    // Tyranus power 8 refused to contest a LONE Leia power 3 for lack of hand
                                    // buddies): "Rando should be taking every opportunity to overpower my
                                    // underpowered solo or low power sites — this should override other logic."
                                    // When THIS deploy alone (+ our power already there) reaches 2× their
                                    // weapon-adjusted power, the buddy/wave requirement is waived. Objective
                                    // hold rules are untouched (they score their own sites; additive).
                                    DeployTacticalPolicy.ContactFacts v171Facts =
                                        new DeployTacticalPolicy.ContactFacts(
                                            action.getActionId(), title, v171OppHere, true,
                                            v171HandChars, v171OurPower, v171ThisPower,
                                            v171Wave, v171WaveR[1], v171WaveR[2],
                                            v171TheirEff, v171MaxHandPower, v171ArmedOpps);
                                    PolicyContributionLedger v171Ledger = new PolicyContributionLedger(
                                        "deploy-tactical-v171-v172-" + action.getActionId());
                                    v171Ledger.register(
                                        DeployTacticalPolicy.scoreV171V172Contact(v171Facts));
                                    PolicyOperationAdapter.apply(action, v171Ledger);
                                    if (!v171Ledger.orderedOperations().isEmpty()
                                            && "V172".equals(v171Ledger.orderedOperations().get(0)
                                                .ruleArmId().id())) {
                                        logger.warn("V172 SOLO DOMINANCE: {} ({}+{} vs eff {}) -> +600 (buddy gate waived, Steve 2026-07-11)",
                                            title, (int) v171OurPower, (int) v171ThisPower, (int) v171TheirEff);
                                    } else if (!v171Ledger.orderedOperations().isEmpty()) {
                                        logger.warn("V171 DEPLOY TO CONTACT: {} (handChars={} wave={} buddies={} reserved={} theirsEff={}) -> +600",
                                            title, v171HandChars, (int) v171Wave,
                                            (int) v171WaveR[1], (int) v171WaveR[2], (int) v171TheirEff);
                                    } else if (v171HandChars >= 2) {
                                        logger.warn("V172 CONTACT GATED: {} wave={} buddies={} reserved={} vs eff {} — can't match their stack (or wave unaffordable after reserves), assemble adjacent instead",
                                            title, (int) v171Wave,
                                            (int) v171WaveR[1], (int) v171WaveR[2], (int) v171TheirEff);
                                    }
                                }
                            } catch (Exception e) { logger.debug("V171 error: {}", e.getMessage()); }
                        }

                        // === V64 MAPUZO JEDI-ONLY RULE ===
                        // On Hidden Path, only Jedi Survivors can transit off Mapuzo via the
                        // Underground Corridor game text. Non-Jedi characters deployed to any
                        // Mapuzo location get STUCK there — they can't follow the Jedi out to
                        // support them at battleground sites. Block non-Jedi character deploys
                        // to Mapuzo UNLESS the opponent is actively threatening Mapuzo with a
                        // drain or presence (in which case we need defenders).
                        // Steve's feedback: "The jedi are the only ones that can move off of
                        // Mapuzo, so deploying any other character except the fallen order
                        // jedi will result in trapping those characters on Mapuzo."
                        if (isCharacter && titleLower.contains("mapuzo")
                            && game != null && playerId != null) {
                            // Check if opponent is present at Mapuzo — defenders needed
                            String v64Opp = gameState.getOpponent(playerId);
                            float oppPowerAtMapuzo = 0;
                            try {
                                oppPowerAtMapuzo = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, location, v64Opp, false, false);
                            } catch (Exception e) { /* ignore */ }

                            // V67b: Check if the deploying card is a TRUE Jedi Survivor.
                            // Drop the previous persona-name fallback (which incorrectly
                            // matched "Ahsoka Tano With Lightsabers", "Obi-Wan With Lightsaber",
                            // "Luke With Lightsaber", etc. — those are Jedi but NOT Jedi
                            // Survivors and CAN'T transit off Mapuzo via Underground Corridor).
                            // Authoritative test: game text contains the literal phrase
                            // "Jedi Survivor" (the keyword that lets Underground Corridor's
                            // transit action target the card).
                            // FIXES xxhj3qwhxzmhrdym replay: Ahsoka Tano With Lightsabers
                            // deployed to Mapuzo: Mining Village and got stuck.
                            boolean isJediSurvivor = false;
                            if (deployingBlueprintId != null) {
                                try {
                                    SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (deployBp != null) {
                                        String gt = deployBp.getGameText();
                                        if (gt != null && gt.toLowerCase(java.util.Locale.ROOT).contains("jedi survivor")) {
                                            isJediSurvivor = true;
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }

                            applyDeploySitingPolicy(action,
                                DeploySitingPolicy.evaluateMapuzoDestination(
                                    new DeploySitingPolicy.MapuzoDestinationFacts(
                                        action.getActionId(), title,
                                        isJediSurvivor, oppPowerAtMapuzo)));
                            if (!isJediSurvivor) {
                                if (oppPowerAtMapuzo > 0) {
                                    logger.info("V64 MAPUZO DEFENSE: {} needs defender vs opponent power {} (+30)",
                                        title, (int)oppPowerAtMapuzo);
                                } else {
                                    logger.warn("V64 MAPUZO TRAP: Non-Jedi deploy to empty {} BLOCKED (-1500)", title);
                                }
                            }
                        }

                        // =====================================================
                        // FOLLOW THE DEPLOY PLAN!
                        // =====================================================
                        if (plannedTargetId != null) {
                            boolean isPlannedTarget = cardId.equals(plannedTargetId);
                            int plannedTargetIndex = context.getCardIds().indexOf(plannedTargetId);
                            boolean plannedTargetOffered = plannedTargetIndex >= 0
                                && isCardSelectable(context, plannedTargetIndex);
                            boolean plannedTargetSpyBlocked = plannedTargetOffered
                                && isOpponentUndercoverOnlyTarget(context, plannedTargetId);
                            // Hoth repair #4 (2026-07-27): a planned target that
                            // FormationSafety hard-blocks is offered but not
                            // admissible. Deferring every other row against it
                            // produced the V201 all-deferred synthetic Pass that
                            // armed the V163 cancel latch (Veers never deployed).
                            // Widen V297.1's release to the formation case.
                            boolean plannedTargetFormationBlocked = plannedTargetOffered
                                && isPlannedTargetFormationHardBlocked(context,
                                    plannedTargetId, deployingBlueprintId,
                                    plannedDeployInstruction, deploymentPlanSnapshot);
                            boolean plannedTargetBlocked = plannedTargetSpyBlocked
                                || plannedTargetFormationBlocked;
                            if (!isPlannedTarget || !plannedTargetBlocked) {
                                applyDeployPlanDestinationPolicy(action,
                                    DeployPlanPolicy.evaluateDestinationTarget(
                                        new DeployPlanPolicy.DestinationTargetFacts(
                                            action.getActionId(), isPlannedTarget,
                                            plannedTargetOffered && !plannedTargetBlocked,
                                            plannedTargetName)));
                            }
                            if (isPlannedTarget && plannedTargetSpyBlocked) {
                                logger.warn("V297.1 PLAN FALLBACK: {} is blocked only by an opponent undercover spy; release alternate destinations",
                                    title);
                            } else if (isPlannedTarget && plannedTargetFormationBlocked) {
                                logger.warn("V297.1 PLAN FALLBACK: planned target {} is FormationSafety hard-blocked; release alternate destinations",
                                    title);
                            } else if (isPlannedTarget) {
                                logger.info("✅ {} is the PLANNED target (+200)", title);
                            }
                        }

                        // =====================================================
                        // V24.14B: EARLY SPY DETECTION (UNIVERSAL)
                        // Check the deploying card's blueprint game text for "undercover".
                        // This catches ALL undercover spy cards, not just hardcoded names.
                        // Sets earlySpyDetected flag — deeper spy scoring handles location logic
                        // (including allowing spy at CC sites where OPPONENT has presence).
                        // =====================================================
                        boolean earlySpyDetected = false;
                        // Primary: Check deploying card's blueprint game text for "undercover"
                        if (deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint deployingBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (deployingBp != null) {
                                    String gameTextCheck = deployingBp.getGameText();
                                    if (gameTextCheck != null && gameTextCheck.toLowerCase(java.util.Locale.ROOT).contains("undercover")) {
                                        earlySpyDetected = true;
                                        logger.warn("V24.14B SPY DETECT: Blueprint game text contains 'undercover' — spy deploy!");
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V24.14B: Error checking deploying blueprint: {}", e.getMessage());
                            }
                        }
                        // Fallback: Check decision text for spy-related keywords
                        if (!earlySpyDetected) {
                            if (decisionText.contains("undercover") || decisionText.contains("as a spy")) {
                                earlySpyDetected = true;
                                logger.warn("V24.14B SPY DETECT: Decision text contains spy keyword — spy deploy!");
                            }
                        }

                        // =====================================================
                        // CRITICAL: Check if target is a STARSHIP
                        // V29: Characters deploying ABOARD ships as pilot/passenger is GOOD
                        //      (especially admirals on Executor). But ships deploying INTO
                        //      cargo bays of other ships = 0 power, which is terrible.
                        // =====================================================
                        if (blueprint != null
                                && (blueprint.getCardCategory() == CardCategory.STARSHIP
                                    || blueprint.getCardCategory() == CardCategory.VEHICLE)) {
                            boolean isExecutor = false;
                            String charGameText = "";
                            String matchedShipName = null;
                            boolean gameTextReferencesThisShip = false;
                            boolean addsForceDrain = false;

                            if (isCharacter) {
                                // V29: CHARACTER deploying ABOARD a ship — this is pilot/passenger deploy!
                                // If character's game text mentions a UNIQUE starship name, they should
                                // deploy aboard that ship to activate their abilities.
                                String shipTitle = titleLower;
                                isExecutor = shipTitle.contains("executor");
                                if (deployingBlueprintId != null) {
                                    try {
                                        SwccgCardBlueprint charBp = getBlueprintFromId(context, deployingBlueprintId);
                                        if (charBp != null) {
                                            charGameText = charBp.getGameText() != null ? charBp.getGameText().toLowerCase(java.util.Locale.ROOT) : "";
                                            for (String shipName : UNIQUE_SHIP_NAMES) {
                                                if (gameTextContainsShipName(charGameText, shipName)) {
                                                    matchedShipName = shipName;
                                                    break;
                                                }
                                            }
                                        }
                                    } catch (Exception e) { /* ignore */ }
                                }

                                // Check if THIS ship matches the referenced ship name
                                if (matchedShipName != null && shipTitle.contains(matchedShipName)) {
                                    gameTextReferencesThisShip = true;
                                }
                                // Also match generic types: "capital starship" matches any capital ship,
                                // "star destroyer" matches any SD
                                if (matchedShipName != null && !gameTextReferencesThisShip) {
                                    if (matchedShipName.equals("capital starship") || matchedShipName.equals("star destroyer")
                                        || matchedShipName.equals("super star destroyer")) {
                                        // Generic type — matches this ship if it's a capital starship
                                        com.gempukku.swccgo.common.CardSubtype shipSubtype = blueprint.getCardSubtype();
                                        if (shipSubtype == com.gempukku.swccgo.common.CardSubtype.CAPITAL) {
                                            gameTextReferencesThisShip = true;
                                        }
                                    }
                                }
                                addsForceDrain = charGameText.contains("adds 1 to force drain")
                                    || charGameText.contains("add 1 to force drain");
                            }

                            DeployPilotShipPolicy.Evaluation boardingEvaluation =
                                DeployPilotShipPolicy.evaluateShipBoarding(
                                    new DeployPilotShipPolicy.ShipBoardingFacts(
                                        action.getActionId(), isCharacter,
                                        isAttachedDeployment, matchedShipName,
                                        gameTextReferencesThisShip, isExecutor, addsForceDrain));
                            applyDeployPilotPolicy(action, boardingEvaluation.result());

                            if (isCharacter) {
                                if (gameTextReferencesThisShip) {
                                    int bonus = addsForceDrain ? 650 : 600;
                                    logger.warn("V29 SHIP-REF: {} references '{}' — deploying aboard {} (+{})",
                                        deployingCardName, matchedShipName, title, bonus);
                                } else if (matchedShipName != null) {
                                    logger.info("V29 ABOARD: {} references '{}' but boarding {} instead (+50)",
                                        deployingCardName, matchedShipName, title);
                                } else if (isExecutor) {
                                    logger.info("V29 ABOARD: {} boarding Executor (+100)", deployingCardName);
                                } else {
                                    logger.info("V29 ABOARD: {} boarding {} (+50)", deployingCardName, title);
                                }
                            } else if (!isAttachedDeployment) {
                                logger.warn("⚠️ BLOCKING deploy of {} into cargo bay of {} - ships in cargo contribute 0 power!",
                                    deployingCardName, title);
                            }

                            if (boardingEvaluation.adapterStep()
                                    == DeployPilotShipPolicy.AdapterStep.CONTINUE_CANDIDATE) {
                                actions.add(action);
                                continue;
                            }
                        }

                        // =====================================================
                        // CRITICAL: WEAPON DEPLOYMENT - check if target already has weapon
                        // Don't deploy a second weapon on a character that already has one!
                        // =====================================================
                        if (isWeapon && blueprint != null
                                && DeployWeaponPolicy.isAttachableWeaponHost(
                                    blueprint.getCardCategory())) {
                            PhysicalCard targetCharacter = location;  // 'location' is actually the target character
                            boolean alreadyHasWeapon = false;
                            String existingWeaponName = null;

                            // Check cards attached to this character
                            List<PhysicalCard> attachedCards = gameState.getAttachedCards(targetCharacter);
                            if (attachedCards != null) {
                                for (PhysicalCard attached : attachedCards) {
                                    if (attached != null && attached.getBlueprint() != null) {
                                        CardCategory attachedCategory = attached.getBlueprint().getCardCategory();
                                        if (attachedCategory == CardCategory.WEAPON) {
                                            alreadyHasWeapon = true;
                                            existingWeaponName = attached.getTitle();
                                            break;
                                        }
                                    }
                                }
                            }

                            applyDeployWeaponPolicy(action,
                                DeployWeaponPolicy.evaluateDestinationSlot(
                                    new DeployWeaponPolicy.DestinationSlotFacts(
                                        action.getActionId(), alreadyHasWeapon,
                                        existingWeaponName)));
                            if (alreadyHasWeapon) {
                                logger.warn("⚠️ V25 HARD BLOCK: {} already has weapon '{}' - NEVER deploy second weapon!",
                                    title, existingWeaponName);
                            }

                            try {
                                var gateAnalyzer = context.getObjectiveAnalyzer();
                                boolean activeActorGate = gateAnalyzer != null
                                        && gateAnalyzer.isAnalyzed()
                                        && !gateAnalyzer.isFlipped()
                                        && gateAnalyzer.hasFlipGateActorRequirement();
                                PhysicalCard targetLocation = activeActorGate
                                        ? game.getModifiersQuerying()
                                                .getLocationThatCardIsPresentAt(
                                                        gameState, targetCharacter)
                                        : null;
                                boolean targetAtGate = targetLocation != null
                                        && gateAnalyzer.isFlipGateLocation(
                                                game, playerId, targetLocation);
                                boolean actorAtGate = targetAtGate
                                        && gateAnalyzer.hasFlipGateActorAtLocation(
                                                game, playerId, targetLocation);
                                applyDeployWeaponPolicy(action,
                                        DeployWeaponPolicy.evaluateObjectiveGateTarget(
                                                new DeployWeaponPolicy.ObjectiveGateTargetFacts(
                                                        action.getActionId(),
                                                        activeActorGate,
                                                        targetAtGate,
                                                        actorAtGate)));
                            } catch (Exception e) {
                                logger.debug("V297 objective gate weapon target failed open: {}",
                                        e.getMessage());
                            }
                        }

                        // =====================================================
                        // V25: HUNT DOWN V — LIGHTSABER DEPLOY PRIORITY
                        // Lightsabers are critical for the Hunt Down deck engine.
                        // Boost any card with "lightsaber" in the title when deploying.
                        // BUT: Never deploy a second lightsaber on same character!
                        // =====================================================
                        if (deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint lsDeployBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (lsDeployBp != null && lsDeployBp.getTitle() != null) {
                                    String lsDeployTitle = lsDeployBp.getTitle().toLowerCase(java.util.Locale.ROOT);
                                    if (lsDeployTitle.contains("lightsaber")) {
                                        // V25: Check if target character already has a lightsaber/weapon
                                        boolean targetHasLightsaber = false;
                                        if (blueprint != null && blueprint.getCardCategory() == CardCategory.CHARACTER) {
                                            PhysicalCard targetChar = location;
                                            List<PhysicalCard> targetAttached = gameState.getAttachedCards(targetChar);
                                            if (targetAttached != null) {
                                                for (PhysicalCard att : targetAttached) {
                                                    if (att != null && att.getBlueprint() != null) {
                                                        CardCategory attCat = att.getBlueprint().getCardCategory();
                                                        String attTitle = att.getTitle();
                                                        if (attCat == CardCategory.WEAPON ||
                                                            (attTitle != null && attTitle.toLowerCase(java.util.Locale.ROOT).contains("lightsaber"))) {
                                                            targetHasLightsaber = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (targetHasLightsaber) {
                                            applyDeployWeaponPolicy(action,
                                                DeployWeaponPolicy.evaluateLightsaberDestination(
                                                    new DeployWeaponPolicy.LightsaberDestinationFacts(
                                                        action.getActionId(), true, false)));
                                            logger.warn("V25 HUNT DOWN: BLOCKED second lightsaber on {} — can only use one!", title);
                                        } else {
                                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer lsDeployOA =
                                                context.getObjectiveAnalyzer();
                                            boolean huntDownV = lsDeployOA != null
                                                && lsDeployOA.isAnalyzed() && lsDeployOA.isHuntDownV();
                                            applyDeployWeaponPolicy(action,
                                                DeployWeaponPolicy.evaluateLightsaberDestination(
                                                    new DeployWeaponPolicy.LightsaberDestinationFacts(
                                                        action.getActionId(), false, huntDownV)));
                                            if (huntDownV) {
                                                logger.warn("V25 HUNT DOWN: Lightsaber '{}' deploying — PRIORITY (+150)", lsDeployBp.getTitle());
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V25 HUNT DOWN: Error checking lightsaber deploy: {}", e.getMessage());
                            }
                        }

                        applyInvasionPilotDestination(
                            context, action, deployingBlueprintId, location);
                        if (destinationIsAsset) {
                            actions.add(action);
                            continue;
                        }

                        // =====================================================
                        // CRITICAL: Detect location type
                        // =====================================================
                        boolean isDockingBay = titleLower.contains("docking bay") || titleLower.contains("landing platform");
                        boolean isSpaceSystem = false;
                        boolean isGroundSite = false;

                        if (blueprint != null) {
                            com.gempukku.swccgo.common.CardSubtype subtype = blueprint.getCardSubtype();
                            isSpaceSystem = (subtype == com.gempukku.swccgo.common.CardSubtype.SYSTEM);
                            isGroundSite = (subtype == com.gempukku.swccgo.common.CardSubtype.SITE) && !isDockingBay;
                        }

                        // =====================================================
                        // V29: SHIP-REFERENCING CHARACTERS ON GROUND — PENALIZE
                        // If a character's game text mentions a unique starship name,
                        // they should be aboard that ship, not on a ground site.
                        // =====================================================
                        if (isCharacter && isGroundSite && deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint charBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (charBp != null) {
                                    String gt = charBp.getGameText() != null ? charBp.getGameText().toLowerCase(java.util.Locale.ROOT) : "";
                                    for (String shipName : UNIQUE_SHIP_NAMES) {
                                        if (gameTextContainsShipName(gt, shipName)) {
                                            applyDeploySitingPolicy(action,
                                                DeploySitingPolicy.evaluateShipReferenceGround(
                                                    new DeploySitingPolicy.ShipReferenceGroundFacts(
                                                        action.getActionId(), shipName)));
                                            logger.warn("V29 GROUND PENALTY: {} mentions '{}' but deploying to ground {} (-200)",
                                                deployingCardName, shipName, title);
                                            break;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V29 SHIP-REF GROUND: Error: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // CRITICAL: Starships at docking bays have 0 power!
                        // Ported from Python card_selection_evaluator.py lines 267-283
                        // =====================================================
                        if (isStarship) {
                            // V190 widening (Steve, 2026-07-04): "Only deploy starships to
                            // systems." The -1500 now covers ALL sites, not just title-matched
                            // docking bays (isGroundSite = SITE && !isDockingBay, so the union
                            // is every site; a null-blueprint docking bay still matches by
                            // title). Behavior change: non-docking-bay sites go from the old
                            // -10 "unusual" nudge (branch commented out below) to -1500.
                            // Sectors deliberately unpenalized pending Steve's ruling.
                            if (isDockingBay || isGroundSite) {
                                // NEVER deploy starships to docking bays!
                                // 2026-06-03 MAGNITUDE BUMP (Steve, Mustafar replay): the
                                // previous VERY_BAD_DELTA (-150) was the only block protecting
                                // ship-deploys from landing at a docking bay (0 power = free
                                // kill). Replay: Rando used Mustafar: Private Platform docking
                                // bay's "Deploy starfighter with 'Vader' in title here" ability,
                                // outer action scored +1530 because V67ai mis-applied +1400 (now
                                // fixed via stricter bare-deploy gate). Even so the -150 here
                                // was too weak to be a clean second line of defense — sub-decision
                                // totals were still about -100 per docking bay site, which beat
                                // many alternative deploys and let the ship land at 0 power.
                                // Bump to -1500: dominates ANY positive deploy-site score in
                                // CardSelectionEvaluator, so when a ship-deploy sub-decision
                                // resolves with only docking bay options, every option scores
                                // ~-1450, Rando picks the Done/cancel sub. The cancel-loop
                                // detector then blocks the outer action after 3 retries, and
                                // Rando stops invoking the docking bay's game text.
                                applyDeploySitingPolicy(action,
                                    DeploySitingPolicy.evaluateStarshipDestination(
                                        new DeploySitingPolicy.StarshipDestinationFacts(
                                            action.getActionId(),
                                            DeploySitingPolicy.StarshipDestinationState.SITE_BLOCKED,
                                            0.0f, 0.0f)));
                                logger.warn("⚠️ V190: {} would have 0 power at site {} → -1500 (widened 2026-07-04 from docking-bays-only)",
                                    deployingCardName, title);
                            } else if (isSpaceSystem) {
                                // Space system - starship has power here (if piloted)
                                // BUT check if we'd be at a power disadvantage!
                                if (game != null) {
                                    try {
                                        float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            game.getGameState(), location, playerId, false, false);
                                        String opponent = game.getOpponent(playerId);
                                        float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            game.getGameState(), location, opponent, false, false);

                                        if (theirPower > 0) {
                                            // Contested space location - check power differential
                                            // Get ship's power from blueprint
                                            int shipPower = 0;
                                            SwccgCardBlueprint deployingBlueprint = getBlueprintFromId(context, deployingBlueprintId);
                                            if (deployingBlueprint != null && deployingBlueprint.hasPowerAttribute()) {
                                                Float power = deployingBlueprint.getPower();
                                                shipPower = power != null ? power.intValue() : 0;
                                            }

                                            float projectedPower = ourPower + shipPower;
                                            if (projectedPower < theirPower) {
                                                // We'd still be losing after deployment!
                                                applyDeploySitingPolicy(action,
                                                    DeploySitingPolicy.evaluateStarshipDestination(
                                                        new DeploySitingPolicy.StarshipDestinationFacts(
                                                            action.getActionId(),
                                                            DeploySitingPolicy.StarshipDestinationState.SPACE_DISADVANTAGE,
                                                            projectedPower, theirPower)));
                                                logger.warn("⚠️ Deploying {} to {} would leave us at power disadvantage ({} vs {})",
                                                    deployingCardName, title, (int)projectedPower, (int)theirPower);
                                            } else if (projectedPower >= theirPower + 3) {
                                                // Good advantage
                                                applyDeploySitingPolicy(action,
                                                    DeploySitingPolicy.evaluateStarshipDestination(
                                                        new DeploySitingPolicy.StarshipDestinationFacts(
                                                            action.getActionId(),
                                                            DeploySitingPolicy.StarshipDestinationState.SPACE_ADVANTAGE,
                                                            projectedPower, theirPower)));
                                            } else {
                                                // Close fight
                                                applyDeploySitingPolicy(action,
                                                    DeploySitingPolicy.evaluateStarshipDestination(
                                                        new DeploySitingPolicy.StarshipDestinationFacts(
                                                            action.getActionId(),
                                                            DeploySitingPolicy.StarshipDestinationState.SPACE_CLOSE,
                                                            projectedPower, theirPower)));
                                            }
                                        } else {
                                            // Uncontested - good target
                                            applyDeploySitingPolicy(action,
                                                DeploySitingPolicy.evaluateStarshipDestination(
                                                    new DeploySitingPolicy.StarshipDestinationFacts(
                                                        action.getActionId(),
                                                        DeploySitingPolicy.StarshipDestinationState.SPACE_UNCONTESTED,
                                                        0.0f, 0.0f)));
                                        }
                                    } catch (Exception e) {
                                        // Fallback to basic bonus if we can't check power
                                        applyDeploySitingPolicy(action,
                                            DeploySitingPolicy.evaluateStarshipDestination(
                                                new DeploySitingPolicy.StarshipDestinationFacts(
                                                    action.getActionId(),
                                                    DeploySitingPolicy.StarshipDestinationState.SPACE_FALLBACK,
                                                    0.0f, 0.0f)));
                                        logger.debug("Could not check power at {}: {}", title, e.getMessage());
                                    }
                                } else {
                                    applyDeploySitingPolicy(action,
                                        DeploySitingPolicy.evaluateStarshipDestination(
                                            new DeploySitingPolicy.StarshipDestinationFacts(
                                                action.getActionId(),
                                                DeploySitingPolicy.StarshipDestinationState.SPACE_FALLBACK,
                                                0.0f, 0.0f)));
                                }
                            }
                            // V190's -1500 veto owns starship-to-site; the retired branch is in git history.
                        }

                        // V296: the existing V36/V51 drain-contest policy used to see only
                        // one-step deploy actions. Starships use a separate destination choice,
                        // so reconnect that owner here after proving the resulting space power
                        // can at least tie the opponent.
                        if (isStarship && isSpaceSystem && game != null && playerId != null) {
                            try {
                                String v296Opp = gameState.getOpponent(playerId);
                                float v296OppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, location, v296Opp, false, false);
                                float v296OurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    gameState, location, playerId, false, false);
                                float v296ShipPower = 0f;
                                SwccgCardBlueprint v296Bp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v296Bp != null && v296Bp.hasPowerAttribute()
                                        && v296Bp.getPower() != null) {
                                    v296ShipPower = v296Bp.getPower();
                                }
                                float v296OppDrain = game.getModifiersQuerying().getForceDrainAmount(
                                    gameState, location, v296Opp);
                                DeployTacticalPolicy.DrainContestEvaluation v296Contact =
                                    DeployTacticalPolicy.evaluateStarshipDrainContact(
                                        new DeployTacticalPolicy.StarshipDrainContactFacts(
                                            action.getActionId(), v296Opp, title,
                                            v296OppPower, v296OurPower, v296ShipPower,
                                            v296OppDrain));
                                applyDeployTacticalPolicy(action, v296Contact.result());
                                if (!v296Contact.outcomes().isEmpty()) {
                                    logger.warn("V296 SPACE DRAIN CONTACT: {} to {} projects {} vs {} and challenges drain {} ({})",
                                        deployingCardName, title,
                                        (int) (v296OurPower + v296ShipPower),
                                        (int) v296OppPower, (int) v296OppDrain,
                                        v296Contact.outcomes());
                                }
                            } catch (Exception e) {
                                logger.debug("V296 space drain contact error: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // CRITICAL: Vehicles need EXTERIOR ground locations
                        // Ported from Python card_selection_evaluator.py lines 287-302
                        // =====================================================
                        if (isVehicle) {
                            if (isSpaceSystem) {
                                // Space location - vehicles can't deploy here
                                applyDeploySitingPolicy(action,
                                    DeploySitingPolicy.evaluateVehicleDestination(
                                        new DeploySitingPolicy.VehicleDestinationFacts(
                                            action.getActionId(),
                                            DeploySitingPolicy.VehicleDestinationState.SPACE_INVALID)));
                            } else if (isGroundSite || isDockingBay) {
                                // Check if location has exterior icon
                                boolean hasExterior = true;  // Default to true if unknown
                                boolean hasInteriorOnly = false;

                                if (blueprint != null) {
                                    // Use hasIcon() method instead of getIcons()
                                    boolean foundExterior = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.EXTERIOR_SITE);
                                    boolean foundInterior = blueprint.hasIcon(com.gempukku.swccgo.common.Icon.INTERIOR_SITE);
                                    hasExterior = foundExterior;
                                    hasInteriorOnly = foundInterior && !foundExterior;
                                }

                                if (hasInteriorOnly) {
                                    applyDeploySitingPolicy(action,
                                        DeploySitingPolicy.evaluateVehicleDestination(
                                            new DeploySitingPolicy.VehicleDestinationFacts(
                                                action.getActionId(),
                                                DeploySitingPolicy.VehicleDestinationState.INTERIOR_INVALID)));
                                    logger.warn("⚠️ Vehicle cannot deploy to interior site {}", title);
                                } else if (hasExterior) {
                                    applyDeploySitingPolicy(action,
                                        DeploySitingPolicy.evaluateVehicleDestination(
                                            new DeploySitingPolicy.VehicleDestinationFacts(
                                                action.getActionId(),
                                                DeploySitingPolicy.VehicleDestinationState.EXTERIOR_VALID)));
                                }
                            }
                        }

                        // =====================================================
                        // V24.14B: WEAPON CHARACTERS/VEHICLES TO SPACE — PENALIZE
                        // Characters with weapons (lightsabers, blasters) can't fire them at
                        // system locations (space). They're mostly useless there.
                        // Also penalize vehicles going to space (already handled above with
                        // VERY_BAD_DELTA, but this adds reasoning for character+weapon combos).
                        // =====================================================
                        if (isCharacter && isSpaceSystem) {
                            // Check if this character has a permanent weapon.
                            // Characters with "permanent weapon" in game text have built-in weapons
                            // that can't fire at system (space) locations — they're useless there.
                            boolean hasPermanentWeapon = false;
                            // Primary: Check deploying card's blueprint game text for "permanent weapon"
                            if (deployingBlueprintId != null) {
                                try {
                                    SwccgCardBlueprint weaponCheckBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (weaponCheckBp != null) {
                                        String weaponGameText = weaponCheckBp.getGameText();
                                        if (weaponGameText != null) {
                                            String weaponTextLower = weaponGameText.toLowerCase(java.util.Locale.ROOT);
                                            if (weaponTextLower.contains("permanent weapon")) {
                                                hasPermanentWeapon = true;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V24.14B WEAPON CHECK: Error: {}", e.getMessage());
                                }
                            }
                            // Fallback: Check decision text for weapon keywords in card name
                            if (!hasPermanentWeapon) {
                                if (decisionText.contains("lightsaber") || decisionText.contains("blaster")
                                    || decisionText.contains("with rifle") || decisionText.contains("with cannon")) {
                                    hasPermanentWeapon = true;
                                }
                            }
                            if (hasPermanentWeapon) {
                                applyDeploySitingPolicy(action,
                                    DeploySitingPolicy.evaluatePermanentWeaponDestination(
                                        new DeploySitingPolicy.PermanentWeaponDestinationFacts(
                                            action.getActionId(),
                                            DeploySitingPolicy.PermanentWeaponDestinationState.SPACE)));
                                logger.warn("V24.14B WEAPON TO SPACE: Character with permanent weapon deploying to {} — penalized (-300)", title);
                            }
                        }
                        // V24.14B: Weapon characters are GREAT at ground sites — they win battles!
                        // Bonus for deploying to sites, especially objective locations or contested ones.
                        if (isCharacter && (isGroundSite || isDockingBay)) {
                            boolean hasPermanentWeaponGround = false;
                            if (deployingBlueprintId != null) {
                                try {
                                    SwccgCardBlueprint wgBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (wgBp != null) {
                                        String wgText = wgBp.getGameText();
                                        if (wgText != null && wgText.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                                            hasPermanentWeaponGround = true;
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }
                            if (!hasPermanentWeaponGround) {
                                if (decisionText.contains("lightsaber") || decisionText.contains("blaster")
                                    || decisionText.contains("with rifle") || decisionText.contains("with cannon")) {
                                    hasPermanentWeaponGround = true;
                                }
                            }
                            if (hasPermanentWeaponGround) {
                                applyDeploySitingPolicy(action,
                                    DeploySitingPolicy.evaluatePermanentWeaponDestination(
                                        new DeploySitingPolicy.PermanentWeaponDestinationFacts(
                                            action.getActionId(),
                                            DeploySitingPolicy.PermanentWeaponDestinationState.GROUND)));
                                logger.info("V24.14B WEAPON GROUND: Character with permanent weapon at site {} — bonus (+100)", title);
                            }
                        }

                        // =====================================================
                        // V29.7: DOCKING BAY CHARACTER DEPLOY — Protect empty bays
                        // If we own a docking bay with NO friendly characters,
                        // the opponent can freely deploy there. Boost character
                        // deployment to our own empty docking bays.
                        // =====================================================
                        if (isCharacter && isDockingBay && location != null) {
                            try {
                                String bayOwner = location.getOwner();
                                if (bayOwner != null && bayOwner.equals(playerId)) {
                                    // Our docking bay — check if empty of friendly characters
                                    boolean bayHasFriendly = false;
                                    java.util.List<PhysicalCard> bayCards = gameState.getCardsAtLocation(location);
                                    if (bayCards != null) {
                                        for (PhysicalCard bc : bayCards) {
                                            if (bc != null && playerId.equals(bc.getOwner())
                                                && bc.getBlueprint() != null
                                                && bc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                bayHasFriendly = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (!bayHasFriendly) {
                                        applyDeploySitingPolicy(action,
                                            DeploySitingPolicy.evaluateEmptyDockingBay(
                                                new DeploySitingPolicy.EmptyDockingBayFacts(
                                                    action.getActionId(), true)));
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // =====================================================
                        // V29.6: Battleground bonus for LOCATION DEPLOY
                        // Use real game engine API for accurate detection.
                        // Note: When deploying a location card, we check if the
                        // location being deployed IS a battleground.
                        // =====================================================
                        if (blueprint != null && blueprint.getCardCategory() == CardCategory.LOCATION) {
                            boolean isBgLoc = false;
                            // For location deploy, the card isn't in play yet, so we
                            // fall back to game text / title heuristic since the game
                            // engine can't check a card that's not on the table yet.
                            String gameTextBg = blueprint.getGameText();
                            if (gameTextBg != null && gameTextBg.toLowerCase(java.util.Locale.ROOT).contains("battleground")) {
                                isBgLoc = true;
                            } else if (titleLower.contains("battleground")) {
                                isBgLoc = true;
                            } else {
                                // Most sites with both LS and DS force icons are battlegrounds.
                                try {
                                    if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE)
                                        && blueprint.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)) {
                                        isBgLoc = true;
                                    }
                                } catch (Exception e) {
                                    // Fall through
                                }
                            }
                            if (isBgLoc) {
                                applyDeploySitingPolicy(action,
                                    DeploySitingPolicy.evaluateBattlegroundLocation(
                                        new DeploySitingPolicy.BattlegroundLocationFacts(
                                            action.getActionId(), true)));
                            }
                        }

                        // V22: OBJECTIVE LOCATION BONUS (boosted from +50 to +150)
                        // Deploy to locations relevant to our objective - critical for flipping
                        // V24.15: SKIP for spies — they don't contribute presence to objectives while undercover!
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer locObjAnalyzer =
                            context.getObjectiveAnalyzer();
                        if (!earlySpyDetected && locObjAnalyzer != null && locObjAnalyzer.isAnalyzed() && title != null) {
                            if (locObjAnalyzer.isObjectiveRelevantLocation(
                                    location, game, playerId)) {
                                float objLocBonus =
                                    locObjAnalyzer.getLocationObjectiveBonus(
                                        location, game, playerId);
                                if (objLocBonus != 0.0f) {
                                    DeployObjectiveSitingPolicy.Facts v22SitingFacts =
                                        new DeployObjectiveSitingPolicy.Facts(
                                            action.getActionId(), earlySpyDetected, true, true,
                                            objLocBonus, false, false, false, false,
                                            false, false, false, "", 0.0f, 0.0f);
                                    PolicyContributionLedger v22SitingLedger = new PolicyContributionLedger(
                                        "deploy-objective-v22-" + action.getActionId());
                                    v22SitingLedger.register(
                                        DeployObjectiveSitingPolicy.evaluate(v22SitingFacts));
                                    PolicyOperationAdapter.apply(action, v22SitingLedger);
                                    logger.warn("V22 OBJECTIVE DEPLOY: {} is objective-relevant (+{})", title, objLocBonus);
                                }
                            }
                        }

                        // === V88 (Steve, 2026-05-19): MY LORD — SENATOR → GALACTIC SENATE BONUS ===
                        // CardSelection-side variant. Replay y6fo7f84hln9kuo8: Toonbuck Toora
                        // (senator) was in hand under My Lord objective, but the outer
                        // "Deploy" action goes through CardSelection for location pick.
                        // V88's DeployEvaluator variant only fires when actionText already
                        // contains "Galactic Senate" — not the case for generic "Deploy".
                        // This CardSelection variant boosts the Galactic Senate CANDIDATE
                        // when the card being deployed is a senator + My Lord active.
                        if (locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                                && locObjAnalyzer.getObjectiveTitle() != null
                                && title != null
                                && deployingBlueprintId != null) {
                            // V88 CONSOLIDATED (2026-07-07): identity from ObjectiveAnalyzer.isMyLord().
                            boolean v88IsMyLord = locObjAnalyzer.isMyLord();
                            if (v88IsMyLord) {
                                try {
                                    SwccgCardBlueprint v88DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                    // V88-CS-LORE (Steve, 2026-05-20): "senator" is identified in
                                    // LORE text, not via Keyword.SENATOR — only ~29 of 35 senator
                                    // cards add the keyword. Check both.
                                    boolean isSenator = false;
                                    if (v88DepBp != null) {
                                        if (v88DepBp.hasKeyword(com.gempukku.swccgo.common.Keyword.SENATOR)) {
                                            isSenator = true;
                                        } else {
                                            String v88Lore = v88DepBp.getLore();
                                            if (v88Lore != null && v88Lore.toLowerCase(java.util.Locale.ROOT)
                                                    .contains("senator")) {
                                                isSenator = true;
                                            }
                                        }
                                    }
                                    if (isSenator) {
                                        String v88TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                                        boolean v88SenateDestination =
                                            v88TitleLower.contains("galactic senate");
                                        DeployObjectiveSitingPolicy.Facts v88SitingFacts =
                                            new DeployObjectiveSitingPolicy.Facts(
                                                action.getActionId(), false, true, false, 0.0f,
                                                true, true, true, v88SenateDestination,
                                                false, false, false, "", 0.0f, 0.0f);
                                        PolicyContributionLedger v88SitingLedger =
                                            new PolicyContributionLedger(
                                                "deploy-objective-v88-" + action.getActionId());
                                        v88SitingLedger.register(
                                            DeployObjectiveSitingPolicy.evaluate(v88SitingFacts));
                                        PolicyOperationAdapter.apply(action, v88SitingLedger);
                                        if (v88SenateDestination) {
                                            logger.warn("V88 MY LORD: senator location bonus +1500 for {}", title);
                                        } else {
                                            logger.warn("V88 MY LORD: senator BLOCK -2000 for non-Senate target {}", title);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V88 MY LORD CardSelection error: {}", e.getMessage());
                                }
                            }
                        }

                        // === V88 GENERALIZED (Steve, 2026-06-03): TEXT-NAMED SITE BONUS ===
                        // Council-verified EDIT of V88 (engineer + rules_lawyer + voice_of_reason
                        // unanimous on "edits existing rule, not new"). The hardcoded senator+
                        // Galactic-Senate logic above runs only on My Lord. This universal clause
                        // runs for ANY character + ANY objective: scan the deploying character's
                        // game text and lore for a substring match against the bare candidate
                        // site name (everything after ":" in the site title).
                        // Steve, Jabba's Haven replay: V136 §A returned +500 for all four
                        // objective-relevant battlegrounds, tied at +1225 each, Rando picked
                        // Tatooine: Desert Heart by list order instead of Jabba's Palace:
                        // Audience Chamber. Jabba The Hutt (200_84) game text:
                        //   "While at Audience Chamber, may [download] Scum And Villainy
                        //    and immune to attrition < 4."
                        // The text-scan picks up "audience chamber" and adds +500 to the
                        // matching candidate — clean tie-break to the character's thematic
                        // home. Generalizes to any character/site pair (Vader/Death Star,
                        // Boushh/Jabba's Palace site, etc.) without card-name hardcoding.
                        //
                        // Guards (council false-positive concerns):
                        //   • Skip when bare site name < 5 chars (avoids "cave"/"hall"/"bay"
                        //     matching anywhere).
                        //   • Negative-phrase detection: "not at X" / "may not deploy at X" /
                        //     "cannot deploy at X" → -500 instead of +500.
                        //   • Match against the BARE site name (post-colon) — avoids
                        //     false-positive on a parent planet prefix.
                        if (deployingBlueprintId != null && title != null) {
                            try {
                                SwccgCardBlueprint v88GenBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v88GenBp != null) {
                                    String v88GenChar = ((v88GenBp.getGameText() != null ? v88GenBp.getGameText() : "")
                                        + " " + (v88GenBp.getLore() != null ? v88GenBp.getLore() : ""))
                                        .toLowerCase(java.util.Locale.ROOT);
                                    String v88GenFullSite = title.toLowerCase(java.util.Locale.ROOT);
                                    String v88GenBareSite = v88GenFullSite.contains(":")
                                        ? v88GenFullSite.substring(v88GenFullSite.indexOf(":") + 1).trim()
                                        : v88GenFullSite;
                                    if (v88GenBareSite.length() >= 5
                                            && v88GenChar.contains(v88GenBareSite)) {
                                        boolean v88GenNegative =
                                            v88GenChar.contains("not at " + v88GenBareSite)
                                            || v88GenChar.contains("may not deploy at " + v88GenBareSite)
                                            || v88GenChar.contains("cannot deploy at " + v88GenBareSite)
                                            || v88GenChar.contains("not at " + v88GenFullSite);
                                        if (v88GenNegative) {
                                            DeployObjectiveSitingPolicy.Facts v88TextFacts =
                                                new DeployObjectiveSitingPolicy.Facts(
                                                    action.getActionId(), false, false, false, 0.0f,
                                                    false, false, false, false,
                                                    true, true, false, v88GenBareSite,
                                                    0.0f, 0.0f);
                                            PolicyContributionLedger v88TextLedger =
                                                new PolicyContributionLedger(
                                                    "deploy-objective-v88-text-" + action.getActionId());
                                            v88TextLedger.register(
                                                DeployObjectiveSitingPolicy.evaluate(v88TextFacts));
                                            PolicyOperationAdapter.apply(action, v88TextLedger);
                                            logger.warn("V88 TEXT-NAMED NEG: '{}' text avoids '{}' → -500",
                                                v88GenBp.getTitle(), v88GenBareSite);
                                        } else {
                                            // 2026-06-04 DEFICIT GATE (Steve, Jabba-walks-into-
                                            // Luke replay): without this gate, my +500 home-site
                                            // bonus dragged Jabba into Audience Chamber where opp
                                            // had 23 power with lightsaber vs us 10 (gap 13).
                                            // Walking a character into a doomed fight for a
                                            // thematic bonus is bad play. Skip the +500 when the
                                            // candidate site is hopelessly outgunned (deficit ≥ 6,
                                            // matching the V67bn cap of 5 with a one-point
                                            // hysteresis buffer). The negative -500 branch above
                                            // still fires for "may not deploy at X" text — that
                                            // applies regardless of site contestation.
                                            boolean v88GenDoomed = false;
                                            if (game != null && playerId != null) {
                                                try {
                                                    String v88GenOpp = game.getOpponent(playerId);
                                                    float v88GenOppPwr = game.getModifiersQuerying()
                                                        .getTotalPowerAtLocation(game.getGameState(),
                                                            location, v88GenOpp, false, false);
                                                    float v88GenOurPwr = game.getModifiersQuerying()
                                                        .getTotalPowerAtLocation(game.getGameState(),
                                                            location, playerId, false, false);
                                                    v88GenDoomed = (v88GenOppPwr - v88GenOurPwr) >= 6f;
                                                    if (v88GenDoomed) {
                                                        logger.warn("V88 TEXT-NAMED SITE SKIP: '{}' wanted '{}' but site is hopelessly outgunned (opp {} us {} gap {}) — no +500",
                                                            v88GenBp.getTitle(), v88GenBareSite,
                                                            (int) v88GenOppPwr, (int) v88GenOurPwr,
                                                            (int) (v88GenOppPwr - v88GenOurPwr));
                                                    }
                                                } catch (Exception ignore) { /* allow fall-through */ }
                                            }
                                            DeployObjectiveSitingPolicy.Facts v88TextFacts =
                                                new DeployObjectiveSitingPolicy.Facts(
                                                    action.getActionId(), false, false, false, 0.0f,
                                                    false, false, false, false,
                                                    true, false, v88GenDoomed, v88GenBareSite,
                                                    0.0f, 0.0f);
                                            PolicyContributionLedger v88TextLedger =
                                                new PolicyContributionLedger(
                                                    "deploy-objective-v88-text-" + action.getActionId());
                                            v88TextLedger.register(
                                                DeployObjectiveSitingPolicy.evaluate(v88TextFacts));
                                            PolicyOperationAdapter.apply(action, v88TextLedger);
                                            if (!v88GenDoomed) {
                                                logger.warn("V88 TEXT-NAMED SITE: '{}' text mentions '{}' → +500",
                                                    v88GenBp.getTitle(), v88GenBareSite);
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V88 TEXT-NAMED SITE error: {}", e.getMessage());
                            }
                        }

                        // === V99 (Steve, 2026-05-20): NON-SENATOR AT GALACTIC SENATE BLOCK ===
                        // CardSelection-side variant. The DeployEvaluator V99 fires only when
                        // actionText already contains "Galactic Senate" — but in practice the
                        // deploy action is generic ("Deploy") and the location is chosen here
                        // in CardSelectionEvaluator. So V99 must live where V88 lives.
                        // Block non-senators from picking Galactic Senate as their destination
                        // unless opponent power at Senate already exceeds friendly senator power
                        // (defensive reinforcement is allowed).
                        // NOTE (2026-07-07 consolidation): DELIBERATELY not isMyLord()-gated — this
                        // keys on the destination CANDIDATE being Galactic Senate, not the objective
                        // identity. Gating it would change behavior. Leave as a typed/title dest check.
                        if (title != null && deployingBlueprintId != null && gameState != null) {
                            String v99TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                            if (v99TitleLower.contains("galactic senate")) {
                                try {
                                    SwccgCardBlueprint v99DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                    boolean v99IsCharacter = v99DepBp != null
                                        && v99DepBp.getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER;
                                    // V99-LORE: senator detection via lore text + keyword (Steve, 2026-05-20)
                                    boolean v99IsSenator = false;
                                    if (v99DepBp != null) {
                                        if (v99DepBp.hasKeyword(com.gempukku.swccgo.common.Keyword.SENATOR)) {
                                            v99IsSenator = true;
                                        } else {
                                            String v99DepLore = v99DepBp.getLore();
                                            if (v99DepLore != null && v99DepLore.toLowerCase(java.util.Locale.ROOT)
                                                    .contains("senator")) {
                                                v99IsSenator = true;
                                            }
                                        }
                                    }
                                    if (v99IsCharacter && !v99IsSenator) {
                                        // Find the actual Senate location to query power
                                        com.gempukku.swccgo.game.PhysicalCard v99SenateLoc = null;
                                        for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getTopLocations()) {
                                            if (loc == null || loc.getTitle() == null) continue;
                                            if (loc.getTitle().toLowerCase(java.util.Locale.ROOT)
                                                    .contains("galactic senate")) {
                                                v99SenateLoc = loc;
                                                break;
                                            }
                                        }
                                        // Default-safe: if Senate isn't yet on table, block — Senate is being
                                        // deployed this same phase via My Lord and non-senators should wait.
                                        float v99FriendlySenatorPower = 0f;
                                        float v99OpponentPower = 0f;
                                        if (v99SenateLoc != null && context.getGame() != null) {
                                            String v99PlayerId = context.getPlayerId();
                                            String v99Opp = context.getGame().getOpponent(v99PlayerId);
                                            for (com.gempukku.swccgo.game.PhysicalCard pc :
                                                    gameState.getAllPermanentCards()) {
                                                if (pc == null) continue;
                                                if (!v99PlayerId.equals(pc.getOwner())) continue;
                                                com.gempukku.swccgo.game.PhysicalCard pcLoc = null;
                                                try {
                                                    pcLoc = context.getGame().getModifiersQuerying()
                                                        .getLocationThatCardIsAt(gameState, pc);
                                                } catch (Exception ignore) { /* */ }
                                                if (pcLoc != v99SenateLoc) continue;
                                                if (pc.getBlueprint() == null) continue;
                                                // V99-LORE: senator detection via lore + keyword
                                                boolean v99PcIsSenator = false;
                                                if (pc.getBlueprint().hasKeyword(
                                                        com.gempukku.swccgo.common.Keyword.SENATOR)) {
                                                    v99PcIsSenator = true;
                                                } else {
                                                    String v99PcLore = pc.getBlueprint().getLore();
                                                    if (v99PcLore != null && v99PcLore.toLowerCase(
                                                            java.util.Locale.ROOT).contains("senator")) {
                                                        v99PcIsSenator = true;
                                                    }
                                                }
                                                if (!v99PcIsSenator) continue;
                                                Float p = pc.getBlueprint().getPower();
                                                if (p != null) v99FriendlySenatorPower += p;
                                            }
                                            if (v99Opp != null) {
                                                v99OpponentPower = context.getGame().getModifiersQuerying()
                                                    .getTotalPowerAtLocation(gameState, v99SenateLoc,
                                                        v99Opp, false, false);
                                            }
                                        }
                                        DeployObjectiveSitingPolicy.Facts v99SitingFacts =
                                            new DeployObjectiveSitingPolicy.Facts(
                                                action.getActionId(), false, false, false, 0.0f,
                                                false, false, true, true,
                                                false, false, false, "galactic senate",
                                                v99OpponentPower, v99FriendlySenatorPower);
                                        PolicyContributionLedger v99SitingLedger =
                                            new PolicyContributionLedger(
                                                "deploy-objective-v99-" + action.getActionId());
                                        v99SitingLedger.register(
                                            DeployObjectiveSitingPolicy.evaluate(v99SitingFacts));
                                        PolicyOperationAdapter.apply(action, v99SitingLedger);
                                        if (v99OpponentPower <= v99FriendlySenatorPower) {
                                            logger.warn("V99 SENATE GUARD: BLOCK non-senator → Senate "
                                                + "(opp={} my-senators={}) -1500",
                                                (int)v99OpponentPower, (int)v99FriendlySenatorPower);
                                        } else {
                                            logger.info("V99 SENATE GUARD: ALLOW non-senator → Senate "
                                                + "(opp={} > my-senators={}) — defensive reinforcement",
                                                (int)v99OpponentPower, (int)v99FriendlySenatorPower);
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.debug("V99 SENATE GUARD CardSelection error: {}", e.getMessage());
                                }
                            }
                        }

                        boolean v212EvazanCsWithoutArmedFriend = false;
                        String v212EvazanCsSiteTitle = title != null ? title : "";

                        // === V89-CS (Steve, 2026-05-20): DR. EVAZAN — NEEDS ARMED PARTNER (CardSelection) ===
                        // The original V89 in DeployEvaluator only fires when actionText already
                        // contains the target location title — but in practice the deploy action
                        // is generic ("Deploy") and the location is chosen here. Same fix pattern
                        // as V99-CS.
                        if (title != null && deployingBlueprintId != null
                                && deployingCardName != null
                                && deployingCardName.startsWith("Dr. Evazan")
                                && gameState != null && context.getGame() != null) {
                            try {
                                com.gempukku.swccgo.game.PhysicalCard v89TargetLoc = null;
                                String v89TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                                for (com.gempukku.swccgo.game.PhysicalCard loc : gameState.getTopLocations()) {
                                    if (loc == null || loc.getTitle() == null) continue;
                                    if (loc.getTitle().toLowerCase(java.util.Locale.ROOT).equals(v89TitleLower)) {
                                        v89TargetLoc = loc;
                                        break;
                                    }
                                }
                                if (v89TargetLoc != null) {
                                    String v89PlayerId = context.getPlayerId();
                                    boolean armedFriendAtTarget = false;
                                    for (com.gempukku.swccgo.game.PhysicalCard pCard
                                            : gameState.getAllPermanentCards()) {
                                        if (pCard == null) continue;
                                        if (!v89PlayerId.equals(pCard.getOwner())) continue;
                                        com.gempukku.swccgo.game.PhysicalCard pCardLoc = null;
                                        try {
                                            pCardLoc = context.getGame().getModifiersQuerying()
                                                .getLocationThatCardIsAt(gameState, pCard);
                                        } catch (Exception ignore) { /* */ }
                                        if (pCardLoc != v89TargetLoc) continue;
                                        if (com.gempukku.swccgo.filters.Filters.character_with_a_weapon
                                                .accepts(gameState,
                                                    context.getGame().getModifiersQuerying(), pCard)) {
                                            armedFriendAtTarget = true;
                                            break;
                                        }
                                    }
                                    if (!armedFriendAtTarget) {
                                        v212EvazanCsWithoutArmedFriend = true;
                                        v212EvazanCsSiteTitle = v89TargetLoc.getTitle();
                                        logger.warn("V89-CS DR. EVAZAN: blocking {} → {} (no armed friend)",
                                            deployingCardName, v89TargetLoc.getTitle());
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V89-CS DR. EVAZAN CardSelection error: {}", e.getMessage());
                            }
                        }

                        DeploySitingPolicy.Facts v212EvazanCsFacts = new DeploySitingPolicy.Facts(
                            action.getActionId(), deployingCardName, v212EvazanCsSiteTitle,
                            v212EvazanCsWithoutArmedFriend, DeploySitingPolicy.FormationState.ALLOW, "",
                            0.0f, false, true, 400.0f, "", false, 0.0f, 0.0f);
                        PolicyContributionLedger v212EvazanCsLedger = new PolicyContributionLedger(
                            "deploy-siting-v89-cs-" + action.getActionId());
                        v212EvazanCsLedger.register(
                            DeploySitingPolicy.evaluateDestination(v212EvazanCsFacts));
                        PolicyOperationAdapter.apply(action, v212EvazanCsLedger);

                        applyBhbmForceDripUrgency(
                            context, action,
                            objectiveProgressDeployingCard,
                            location,
                            bhbmDeployFormationSafe(
                                context,
                                objectiveProgressDeployingCard,
                                location),
                            BhbmForceDripUrgencyFactsReader
                                .CandidateMechanism.DEPLOY);

                        // === V136 (Steve, 2026-05-26): UNIFIED CHARACTER DEPLOY SITE EVALUATOR (CardSelection route) ===
                        // Supersedes V122 (below) and V67as (later in this file).
                        // See CharacterDeploySiteEvaluator + V136_DEPLOY_LOG.md.
                        if (title != null && deployingBlueprintId != null && location != null
                                && gameState != null && context.getGame() != null
                                && context.getPlayerId() != null) {
                            try {
                                SwccgCardBlueprint v136DepBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (v136DepBp != null
                                        && v136DepBp.getCardCategory() == com.gempukku.swccgo.common.CardCategory.CHARACTER) {
                                    // Find the actual PhysicalCard in hand by matching blueprintId
                                    PhysicalCard v136DeployingCard = null;
                                    if (plannedDeployInstruction != null
                                            && plannedDeployInstruction.getCardPermanentCardId() != null
                                            && plannedDeployInstruction.getCardCurrentCardId() != null) {
                                        for (PhysicalCard h : gameState.getHand(context.getPlayerId())) {
                                            if (h != null
                                                    && h.getPermanentCardId() == plannedDeployInstruction.getCardPermanentCardId()
                                                    && h.getCardId() == plannedDeployInstruction.getCardCurrentCardId()) {
                                                v136DeployingCard = h;
                                                break;
                                            }
                                        }
                                    }
                                    for (PhysicalCard h : gameState.getHand(context.getPlayerId())) {
                                        if (v136DeployingCard != null) break;
                                        if (h == null) continue;
                                        if (deployingBlueprintId.equals(h.getBlueprintId(false))) {
                                            v136DeployingCard = h;
                                            break;
                                        }
                                    }
                                    if (v136DeployingCard != null) {
                                        DeploySitingPolicy.FormationState v212FormationState =
                                            DeploySitingPolicy.FormationState.ALLOW;
                                        String v212FormationReason = "";
                                        float v212V136CsScore = 0.0f;
                                        boolean v212V193CsEligible = false;
                                        boolean v212V193ActorGateCandidate = false;
                                        boolean v212V193FormationSupported = true;
                                        float v212V193CsWeight = 400.0f;
                                        String v212V193CsGateCard = "";

                                        // FORMATION SAFETY (2026-07-11c): L3/L4 deploy vetoes — un-outvotable.
                                        // (Codex audit incident 1: Greedo ability 1 deployed solo at 1420 while
                                        // two affordable buddies sat in hand; every guard was additive.)
                                        try {
                                            SwccgCardBlueprint fsDepBp = v136DeployingCard.getBlueprint();
                                            if (fsDepBp != null && fsDepBp.getCardCategory() == CardCategory.CHARACTER
                                                    && context.getGame() != null) {
                                                // V201: a companion is real only when the detached deployment
                                                // plan names another exact physical character at this destination.
                                                float fsForce = gameState.getForcePileSize(context.getPlayerId());
                                                Float fsThisCost = fsDepBp.getDeployCost();
                                                Float fsBuddyCost = null;
                                                java.util.Set<Integer> fsCharacterIdsInHand = new java.util.HashSet<>();
                                                for (PhysicalCard fsH : gameState.getHand(context.getPlayerId())) {
                                                    if (fsH == null || fsH.getBlueprint() == null) continue;
                                                    if (fsH.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                    fsCharacterIdsInHand.add(fsH.getPermanentCardId());
                                                }
                                                boolean fsExactPlanCard = plannedDeployInstruction != null
                                                    && plannedDeployInstruction.getCardPermanentCardId() != null
                                                    && plannedDeployInstruction.getCardCurrentCardId() != null
                                                    && v136DeployingCard.getPermanentCardId() == plannedDeployInstruction.getCardPermanentCardId()
                                                    && v136DeployingCard.getCardId() == plannedDeployInstruction.getCardCurrentCardId();
                                                if (fsExactPlanCard && deploymentPlanSnapshot != null) {
                                                    fsBuddyCost = deploymentPlanSnapshot.getCheapestPlannedCharacterBuddyCost(
                                                        plannedDeployInstruction, String.valueOf(location.getCardId()),
                                                        fsCharacterIdsInHand);
                                                }
                                                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer fsObj =
                                                    context.getObjectiveAnalyzer();
                                                boolean fsSenateProgress = fsObj != null
                                                    && fsObj.isSenateObjectiveSenatorDeployment(
                                                        context.getGame(), context.getPlayerId(),
                                                        v136DeployingCard, location);
                                                String fsFlipGate = (fsObj != null && fsObj.isAnalyzed())
                                                    ? fsObj.getFlipCriticalControlSite() : null;
                                                if (fsObj != null && fsObj.hasFlipGateActorRequirement()) {
                                                    v212V193ActorGateCandidate =
                                                        fsObj.advancesUnfilledFlipGateActorRequirement(
                                                            context.getGame(), context.getPlayerId(),
                                                            v136DeployingCard, location);
                                                    int fsFriendlyCharacters = com.gempukku.swccgo.ai.models.common.strategy
                                                        .FormationSafety.countFriendlyNonUndercoverCharacters(
                                                            gameState.getCardsAtLocation(location),
                                                            context.getPlayerId());
                                                    boolean fsFundedBuddy = fsBuddyCost != null
                                                        && fsThisCost != null
                                                        && fsThisCost + fsBuddyCost <= fsForce;
                                                    v212V193FormationSupported =
                                                        fsFriendlyCharacters > 0 || fsFundedBuddy;
                                                    if (!v212V193ActorGateCandidate
                                                            || !v212V193FormationSupported) {
                                                        fsFlipGate = null;
                                                    }
                                                } else {
                                                    v212V193FormationSupported = true;
                                                }
                                                if (fsObj != null
                                                        && fsObj.wouldCompletePreFlipRequirementAt(
                                                            context.getGame(),
                                                            context.getPlayerId(),
                                                            v136DeployingCard,
                                                            location)) {
                                                    fsFlipGate = location.getTitle();
                                                }
                                                com.gempukku.swccgo.ai.models.common.strategy.FormationSafety.DeployVerdict fsVerdict =
                                                    com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                                    .assessCharacterDeploy(context.getGame(), gameState, context.getPlayerId(),
                                                        v136DeployingCard,
                                                        fsDepBp.hasPowerAttribute() ? fsDepBp.getPower() : null,
                                                        fsDepBp.hasAbilityAttribute() ? fsDepBp.getAbility() : null,
                                                        v136DeployingCard.isUndercover(),
                                                        location, fsForce, fsThisCost, fsBuddyCost, fsFlipGate);
                                                if (fsVerdict.constraint() == com.gempukku.swccgo.ai.models.common.strategy.FormationSafety.DeployConstraint.HARD_BLOCK) {
                                                    v212FormationState = DeploySitingPolicy.FormationState.HARD_BLOCK;
                                                    v212FormationReason = fsVerdict.reason();
                                                    logger.warn("FORMATION SAFETY (deploy-site): {}", fsVerdict.reason());
                                                } else if (fsVerdict.constraint() == com.gempukku.swccgo.ai.models.common.strategy.FormationSafety.DeployConstraint.DEFER_UNSUPPORTED_SOLO) {
                                                    if (fsSenateProgress) {
                                                        action.addReasoning("SENATE OBJECTIVE FORMATION: allow senator to advance the Galactic Senate count");
                                                        logger.warn("SENATE OBJECTIVE FORMATION: releasing unsupported-solo defer at Galactic Senate");
                                                    } else {
                                                        v212FormationState = DeploySitingPolicy.FormationState.DEFER_UNSUPPORTED_SOLO;
                                                        v212FormationReason = fsVerdict.reason();
                                                        logger.warn("V201 DEPLOY DEFER: {}", fsVerdict.reason());
                                                    }
                                                } else if (fsVerdict.constraint() == com.gempukku.swccgo.ai.models.common.strategy.FormationSafety.DeployConstraint.UNKNOWN) {
                                                    v212FormationState = DeploySitingPolicy.FormationState.UNKNOWN;
                                                    v212FormationReason = fsVerdict.reason();
                                                }
                                            }
                                        } catch (Exception fsE) { /* fail-open */ }
                                        int v136Turn = gameState.getPlayersLatestTurnNumber(context.getPlayerId());
                                        java.util.List<PhysicalCard> v136Hand = gameState.getHand(context.getPlayerId());
                                        int v136ForceAvail = gameState.getForcePileSize(context.getPlayerId());
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v136Obj =
                                            context.getObjectiveAnalyzer();
                                        // step 3b (2026-07-10): filter-based relevance overload (rules) — for
                                        // objectives WITHOUT rules this equals the old title check (neutral).
                                        boolean v136CountedFormationExemption = true;
                                        if (v136Obj != null
                                                && v136Obj.hasCountedOperativeFormationRule()) {
                                            boolean v136ExactPlannedPair = false;
                                            if (deploymentPlanSnapshot != null
                                                    && deploymentPlanSnapshot.getReason() != null
                                                    && deploymentPlanSnapshot.getReason().startsWith(
                                                        "Objective counted-operative formations")
                                                    && plannedDeployInstruction != null
                                                    && plannedDeployInstruction.getCardPermanentCardId() != null
                                                    && plannedDeployInstruction.getCardCurrentCardId() != null
                                                    && v136DeployingCard.getPermanentCardId()
                                                        == plannedDeployInstruction.getCardPermanentCardId()
                                                    && v136DeployingCard.getCardId()
                                                        == plannedDeployInstruction.getCardCurrentCardId()) {
                                                java.util.Set<Integer> v136CharacterIdsInHand =
                                                    new java.util.HashSet<>();
                                                for (PhysicalCard v136HandCard : v136Hand) {
                                                    if (v136HandCard != null
                                                            && v136HandCard.getBlueprint() != null
                                                            && v136HandCard.getBlueprint().getCardCategory()
                                                                == CardCategory.CHARACTER) {
                                                        v136CharacterIdsInHand.add(
                                                            v136HandCard.getPermanentCardId());
                                                    }
                                                }
                                                Float v136BuddyCost = deploymentPlanSnapshot
                                                    .getCheapestPlannedCharacterBuddyCost(
                                                        plannedDeployInstruction,
                                                        String.valueOf(location.getCardId()),
                                                        v136CharacterIdsInHand);
                                                v136ExactPlannedPair = v136BuddyCost != null
                                                    && plannedDeployInstruction.getDeployCost()
                                                        + v136BuddyCost <= v136ForceAvail;
                                            }
                                            v136CountedFormationExemption =
                                                v136Obj.advancesPreFlipRequirementAt(
                                                    context.getGame(), context.getPlayerId(),
                                                    v136DeployingCard, location)
                                                || v136ExactPlannedPair;
                                        }
                                        boolean v136ObjRelevant = v136Obj != null && v136Obj.isAnalyzed()
                                            && title != null
                                            && v136Obj.isObjectiveRelevantLocation(location, context.getGame(), context.getPlayerId());
                                        float v136Score = com.gempukku.swccgo.ai.models.common.strategy
                                            .CharacterDeploySiteEvaluator.evaluateSite(
                                                context.getGame(), v136DeployingCard, location,
                                                context.getPlayerId(),
                                                v136ObjRelevant,
                                                v136ObjRelevant && v136CountedFormationExemption,
                                                v136Hand,
                                                v136ForceAvail,
                                                v136Turn,
                                                0 /* deckShipCount — TODO wire */,
                                                false /* perSiteEffectActive — TODO wire */);
                                        v212V136CsScore = v136Score;
                                        if (v136Score != 0f) {
                                            logger.info("V136 CS [{}]: {} → {} score={}",
                                                context.getPlayerId(), v136DeployingCard.getTitle(), title, v136Score);
                                        }

                                        // === V193 (CS) (Steve, 2026-07-09): FLIP-GATE CONTROL STEER on the CardSelection route ===
                                        // V193 (see DeployEvaluator) steers ONE body to the objective's flip-gate
                                        // control site (Endor: Bunker for Endor Operations) so the control-gated
                                        // flip card (Establish Secret Base (V) 207_25, "Deploy on Bunker if you
                                        // control that site") becomes a legal deploy and the objective can flip.
                                        // The original V193 lives ONLY in DeployEvaluator, but Endor character
                                        // deploys resolve through THIS CardSelection route (logged "V136 CS"), so
                                        // V193 fired 0 times in replay somykkwjy449xul4 and the objective never
                                        // flipped. This mirror puts the steer where the deploys actually are.
                                        //
                                        // TWO CORRECTIONS vs the DeployEvaluator copy, both evidence-backed:
                                        //  1. ABILITY GATE. Control needs presence; presence needs ability >= 1.
                                        //     That game put 4-LOM With Concussion Rifle (V) (a DROID, ability 0) on
                                        //     Bunker turn 5 — no presence, no control, ESB stayed illegal. So only
                                        //     steer a real character with ability >= 1 (droids have
                                        //     hasAbilityAttribute()==false and are skipped) and prefer a CHEAP spare
                                        //     body (deployCost <= 3, e.g. Admiral Ozzel (V) cost 2) so a bomber
                                        //     (Thrawn) is not wasted on a 0-drain site. Endor Shield (V) uploads an
                                        //     Imperial admiral twice per game, so a cheap legal body is available.
                                        //  2. MAGNITUDE. The CS route stacks anti-hold penalties the DeployEvaluator
                                        //     boundary never saw: V67ah NON-BG -350, V113 SOLO -300, V24.15
                                        //     ZERO-DRAIN ~-80, plus (for a Star-Destroyer pilot like Ozzel) V29
                                        //     GROUND -200 and V29 CONCENTRATE -100 — all fighting a hold that IS the
                                        //     objective's win condition, not a mistake. And the competing drain site
                                        //     runs HOT when a friendly is already there to REINFORCE (+150). Two
                                        //     replays: somykkwjy449xul4 t3 Thrawn->Bunker 135 vs Landing 905; the
                                        //     RE-TUNE replay vugpape5lw1bc7rq t2 Ozzel->Bunker 1240 vs Landing 1250 —
                                        //     the first +730-offset steer LOST BY 10. So the steer must DOMINATE (not
                                        //     merely nudge): playbook weight (400) + CS penalty offset (1600) = ~2000
                                        //     (V136 §A team-viability scale — seizing the flip gate unlocks the whole
                                        //     objective flip, a game-deciding tempo swing worth more than any single
                                        //     drain). That lifts Bunker to ~2100 > the ~1430-1555 hottest observed
                                        //     drain competitors by ~550. The large magnitude is safe because the guard
                                        //     is narrow (Endor flip-gate + holds ESB + Bunker uncontrolled + cheap
                                        //     ability body) and self-limiting: fires only
                                        //     while (a) analyzer named a flip-gate site, (b) Rando does NOT control
                                        //     it, (c) Rando holds the gate card. Once one body lands Rando controls
                                        //     Bunker -> guard (b) closes -> no per-body stacking; the rest of the
                                        //     pile reverts to drain sites and the lone body holds.
                                        if (v136Obj != null && v136Obj.isAnalyzed() && title != null) {
                                            String v193csGateSite = v136Obj.getFlipCriticalControlSite();
                                            if (v193csGateSite != null && v193csGateSite.equalsIgnoreCase(title)) {
                                                boolean v193csActorGateCandidate =
                                                    v212V193ActorGateCandidate
                                                        || v136Obj.advancesUnfilledFlipGateActorRequirement(
                                                            context.getGame(), context.getPlayerId(),
                                                            v136DeployingCard, location);
                                                Float v193csAbility = v136DepBp.hasAbilityAttribute() ? v136DepBp.getAbility() : null;
                                                Float v193csCost = v136DepBp.getDeployCost();
                                                boolean v193csGoodBody = v193csAbility != null && v193csAbility >= 1f
                                                    && v193csCost != null && v193csCost <= 4f;
                                                if (v193csGoodBody || v193csActorGateCandidate) {
                                                    boolean v193csControls = com.gempukku.swccgo.cards.GameConditions.controls(
                                                        context.getGame(), context.getPlayerId(), location);
                                                    java.util.Set<String> v193csGateIds = v136Obj.getFlipCriticalControlCardIds();
                                                    String v193csGateCard = v136Obj.getFlipCriticalControlCard();
                                                    if (v193csGateCard == null && v193csActorGateCandidate) {
                                                        v193csGateCard = v136Obj.getFlipGateActorRequirementLabel();
                                                    }
                                                    com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle v193csOracle = context.getDeckOracle();
                                                    boolean v193csHoldsGate = false;
                                                    if (v193csOracle != null) {
                                                        if (v193csGateIds != null && !v193csGateIds.isEmpty()) {
                                                            for (String v193csId : v193csGateIds) {
                                                                if (v193csOracle.isCardInHand(v193csId)
                                                                        || v193csOracle.isCardInReserve(v193csId)) {
                                                                    v193csHoldsGate = true;
                                                                    break;
                                                                }
                                                            }
                                                        } else if (v193csGateCard != null) {
                                                            v193csHoldsGate = v193csOracle.isCardInHand(v193csGateCard)
                                                                || v193csOracle.isCardInReserve(v193csGateCard);
                                                        }
                                                    }
                                                    boolean v193csCanAdvanceGate =
                                                        (v193csHoldsGate || v193csActorGateCandidate)
                                                            && v212V193FormationSupported;
                                                    // V297: Invasion's actor bonus is valid only when this exact
                                                    // phase plan also funds a Throne Room buddy, or one is present.
                                                    if ((!v193csControls || v193csActorGateCandidate)
                                                            && v193csCanAdvanceGate) {
                                                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer.ObjectivePlaybook
                                                            v193csPlaybook = v136Obj.getActivePlaybook();
                                                        float v193csWeight = (v193csPlaybook != null)
                                                            ? v193csPlaybook.weights.deployFlipGateSite : 400.0f;
                                                        // CS penalty offset: total steer ~= 2000 to DOMINATE the anti-hold
                                                        // stack (V67ah -350 + V113 -300 + V24.15 -80 + Ozzel V29 GROUND
                                                        // -200 + CONCENTRATE -100) AND a REINFORCED hot drain competitor.
                                                        // +730 lost Ozzel->Bunker by 10 (1240 vs 1250) in replay
                                                        // vugpape5lw1bc7rq t2; +1600 (total ~2000) wins by ~550.
                                                        float v193csBonus = v193csWeight + 1600.0f;
                                                        v212V193CsEligible = true;
                                                        v212V193CsWeight = v193csWeight;
                                                        v212V193CsGateCard = v193csGateCard;
                                                        logger.warn("V193 (CS) FLIP-GATE CONTROL [{}]: {} → {} +{} (seize flip-gate, card={})",
                                                            context.getPlayerId(), v136DeployingCard.getTitle(), title, v193csBonus, v193csGateCard);
                                                    }
                                                }
                                            }
                                        }

                                        DeploySitingPolicy.Facts v212SitingCsFacts =
                                            new DeploySitingPolicy.Facts(
                                                action.getActionId(), v136DeployingCard.getTitle(), title,
                                                false, v212FormationState, v212FormationReason,
                                                v212V136CsScore, v212V193CsEligible,
                                                v212V193FormationSupported,
                                                v212V193CsWeight, v212V193CsGateCard,
                                                false, 0.0f, 0.0f);
                                        PolicyContributionLedger v212SitingCsLedger =
                                            new PolicyContributionLedger(
                                                "deploy-siting-cs-" + action.getActionId());
                                        v212SitingCsLedger.register(
                                            DeploySitingPolicy.evaluateDestination(v212SitingCsFacts));
                                        PolicyOperationAdapter.apply(action, v212SitingCsLedger);

                                        boolean v212ActorRouteStaging =
                                            v136Obj != null
                                            && v136Obj.isAnalyzed()
                                            && v136Obj.stagesPreFlipActorRoute(
                                                context.getGame(),
                                                context.getPlayerId(),
                                                v136DeployingCard, location);
                                        applyDeploySitingPolicy(
                                            action,
                                            DeployObjectiveSitingPolicy
                                                .scoreActorRouteStaging(
                                                    action.getActionId(),
                                                    v212ActorRouteStaging,
                                                    v136DeployingCard.getTitle(),
                                                    title));
                                        if (v212ActorRouteStaging) {
                                            logger.warn(
                                                "OBJECTIVE ACTOR STAGING: {} to {} +1000",
                                                v136DeployingCard.getTitle(),
                                                title);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V136 CS error: {}", e.getMessage());
                            }
                        }

                        // V122 retired into V136 §A; V133 was dropped before V136.
                        // Their legacy bodies and rationale remain in git history.

                        // === V24.10: EXECUTOR MUST DEPLOY TO BESPIN ===
                        // When deploying Executor/Flagship, Bespin system is the ONLY correct target
                        // for TDIGWATT. Deploying to any other system is catastrophic — the entire
                        // deck engine depends on Executor occupying Bespin for Dark Deal + CC Occupation.
                        if (isStarship && isSpaceSystem && deployingCardName != null) {
                            String deployingNameLower = deployingCardName.toLowerCase(java.util.Locale.ROOT);
                            if (deployingNameLower.contains("executor") || deployingNameLower.contains("flagship")) {
                                String locTitleLower = title != null ? title.toLowerCase(java.util.Locale.ROOT) : "";
                                if (locTitleLower.contains("bespin")) {
                                    applyDeployPilotPolicy(action,
                                        DeployPilotShipPolicy.evaluateExecutorDestination(
                                            new DeployPilotShipPolicy.ExecutorDestinationFacts(
                                                action.getActionId(), true, title)));
                                    logger.warn("V24.10 EXECUTOR LOCATION: Bespin system selected — MASSIVE bonus (+500)!");
                                } else {
                                    // Any non-Bespin system is WRONG for Executor
                                    applyDeployPilotPolicy(action,
                                        DeployPilotShipPolicy.evaluateExecutorDestination(
                                            new DeployPilotShipPolicy.ExecutorDestinationFacts(
                                                action.getActionId(), false, title)));
                                    logger.warn("V24.10 EXECUTOR LOCATION: {} is NOT Bespin — HARD BLOCK! Executor must deploy to Bespin!", title);
                                }
                            }
                        }

                        // === V22.7: OBJECTIVE-CRITICAL LOCATION CONTESTATION ===
                        // If the opponent occupies a location our objective NEEDS us to control,
                        // we MUST contest it — even if we're currently losing there.
                        // The objective bonus alone may not override the contest penalty,
                        // so add an explicit "MUST CONTEST" bonus for ships at critical systems.
                        if (locObjAnalyzer != null && locObjAnalyzer.isAnalyzed() && title != null
                            && locObjAnalyzer.isObjectiveRelevantLocation(title)
                            && isSpaceSystem && isStarship && game != null) {
                            try {
                                float ourP = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, playerId, false, false);
                                String opp = game.getOpponent(playerId);
                                float theirP = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, opp, false, false);
                                DeployObjectiveSitingPolicy.MustContestEvaluation v279MustContest =
                                    DeployObjectiveSitingPolicy.evaluateMustContest(
                                        new DeployObjectiveSitingPolicy.MustContestFacts(
                                            action.getActionId(), title, ourP, theirP));
                                applyDeploySitingPolicy(action, v279MustContest.result());
                                if (v279MustContest.outcome()
                                        == DeployObjectiveSitingPolicy.MustContestOutcome.MUST_CONTEST) {
                                    logger.warn("V22.7 MUST CONTEST: {} — opponent has {} power, we have {} — MUST deploy ship here!",
                                        title, (int)theirP, (int)ourP);
                                }
                            } catch (Exception e) {
                                logger.debug("V22.7: Could not check objective-critical contest: {}", e.getMessage());
                            }
                        }

                        // === V23: OPPONENT FORCE ICON PREFERENCE (ALL OBJECTIVES) ===
                        // Locations with opponent force icons are better force drain targets.
                        // For Dark Side: Light Side force icons = more drain damage.
                        if (blueprint != null) {
                            Side mySide = context.getSide();
                            int opponentIcons = 0;
                            if (mySide == Side.DARK) {
                                opponentIcons = blueprint.getIconCount(Icon.LIGHT_FORCE);
                            } else {
                                opponentIcons = blueprint.getIconCount(Icon.DARK_FORCE);
                            }
                            if (opponentIcons > 0) {
                                float iconBonus = opponentIcons * 30.0f;
                                applyDeploySitingPolicy(action,
                                    DeploySitingPolicy.evaluateOpponentForceIcons(
                                        new DeploySitingPolicy.OpponentForceIconsFacts(
                                            action.getActionId(), opponentIcons)));
                                logger.info("V23 FORCE ICONS: {} has {} opponent icons (+{})", title, opponentIcons, (int)iconBonus);
                            }
                        }

                        // === V24.15: AVOID DEPLOYING CHARACTERS TO WORTHLESS-DRAIN LOCATIONS ===
                        // (UPDATED 2026-07-07, Steve — CONSOLIDATED: now covers EFFECTIVE drain, not
                        // just literal raw 0, per feedback_update_old_rule_not_new_version. The old
                        // block only read raw drain, so under an opponent Battle Plan (+3 drain-
                        // INITIATION tax) Rando piled bodies onto Endor: Landing Platform — raw drain
                        // 1, net 1-3 = -2, a drain there is a net LOSS that V189 blocks every time —
                        // yet the deploy path still scored it high (mistake 4, replay qgdridfo166f27r3).
                        // The effective-drain check is folded IN HERE (one rule) instead of a separate
                        // contradictory penalty.)
                        // Characters at worthless-drain sites add no drain pressure and are Surprise-
                        // Assault bait. Penalty scales with power — don't waste your best characters.
                        if (isCharacter && !earlySpyDetected && game != null && location != null) {
                            try {
                                float v2415RawDrain = game.getModifiersQuerying().getForceDrainAmount(
                                    game.getGameState(), location, playerId);
                                float v2415InitCost = game.getModifiersQuerying().getInitiateForceDrainCost(
                                    game.getGameState(), location, playerId);
                                boolean v2415ZeroDrain = v2415RawDrain <= 0;
                                // Net-negative EFFECTIVE drain: drain capped below its cost by a tax
                                // (Battle Plan / Battle Order) — the EXACT V189 predicate. cost 0 (no
                                // tax = the vast majority of games) => this arm can NEVER fire, so every
                                // existing game is byte-identical to before this consolidation.
                                boolean v2415NetNeg = v2415InitCost > 0f && (v2415RawDrain - v2415InitCost) <= -2f;
                                Float v2415CharPower = (blueprint != null && blueprint.hasPowerAttribute()) ? blueprint.getPower() : null;
                                float v2415PowerVal = (v2415CharPower != null) ? v2415CharPower : 3.0f;
                                boolean v2415ObjRelevant = false;
                                boolean v2415V166Contest = false;
                                if (!v2415ZeroDrain && v2415NetNeg) {
                                    v2415ObjRelevant = locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                                        && title != null && locObjAnalyzer.isObjectiveRelevantLocation(title);
                                    if (!v2415ObjRelevant) {
                                        String v2415Opp = gameState.getOpponent(playerId);
                                        float v2415TheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, location, v2415Opp, false, false);
                                        int v2415OppDrain = (int) game.getModifiersQuerying().getForceDrainAmount(
                                            gameState, location, v2415Opp);
                                        v2415V166Contest = (v2415TheirPower > 0 && v2415OppDrain > 0
                                            && computeNetDrainBalance(game, gameState, playerId) >= 2);
                                    }
                                }
                                DeployTacticalPolicy.V2415DrainEvaluation v2415Drain =
                                    DeployTacticalPolicy.evaluateV2415Drain(
                                        new DeployTacticalPolicy.V2415DrainFacts(
                                            action.getActionId(), title, v2415RawDrain,
                                            v2415InitCost, v2415PowerVal,
                                            v2415ObjRelevant, v2415V166Contest));
                                applyDeployTacticalPolicy(action, v2415Drain.result());
                                if (v2415Drain.outcome()
                                        == DeployTacticalPolicy.V2415DrainOutcome.ZERO_DRAIN) {
                                    logger.warn("V24.15 ZERO DRAIN: {} has 0 drain — penalizing {} (power {}) by {}",
                                        title, decisionText, v2415PowerVal, v2415Drain.delta());
                                } else if (v2415Drain.outcome()
                                        == DeployTacticalPolicy.V2415DrainOutcome.EFFECTIVE_DRAIN) {
                                    logger.warn("V24.15 EFFECTIVE DRAIN: {} raw={} cost={} power={} -> {}",
                                        title, (int) v2415RawDrain, (int) v2415InitCost, (int) v2415PowerVal,
                                        (int) v2415Drain.delta());
                                }
                            } catch (Exception e) {
                                logger.debug("V24.15: Error checking drain amount for deploy: {}", e.getMessage());
                            }
                        }

                        // === V29.7: ISB OPERATIONS DEPLOYMENT STRATEGY (enhanced from V25) ===
                        // FLIP CONDITION: 4 ISB agents on table OR ISB agents control 2 Rebel Base locations.
                        // FLIPPED BONUS: +1 drain at BG sites with non-Undercover ISB agent, -1 opponent drain.
                        // STRATEGY: Pre-flip, ISB agents get MASSIVE priority. Non-ISB agents heavily penalized.
                        //           Higher ability characters preferred for location control.
                        if (false /* SUPERSEDED 2026-07-27 Batch Nineteen — 7_299 profile flipLocationRules now own ISB steering; retired per gap_matrix prescription to prevent double-steer */
                            && isCharacter && !earlySpyDetected && locObjAnalyzer != null
                            && locObjAnalyzer.isAnalyzed() && locObjAnalyzer.isISBOperations()) {
                            try {
                                // Check if the deploying card is an ISB agent and get its ability
                                boolean deployingIsISBAgent = false;
                                float deployAbility = 0.0f;
                                if (deployingBlueprintId != null) {
                                    SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (deployBp != null) {
                                        String lore = deployBp.getLore();
                                        if (lore != null) {
                                            String loreLower = lore.toLowerCase(Locale.ROOT);
                                            deployingIsISBAgent = loreLower.contains("isb")
                                                || loreLower.contains("rebel") || loreLower.contains("rebellion");
                                        }
                                        // V29.7: Get ability value for ability-based scoring
                                        if (deployBp.hasAbilityAttribute()) {
                                            deployAbility = deployBp.getAbility();
                                        }
                                    }
                                }

                                int isbOnTable = locObjAnalyzer.countISBAgentsOnTable(gameState, playerId);
                                int isbNeeded = locObjAnalyzer.getISBFlipAgentCount();
                                boolean preFlip = !locObjAnalyzer.isFlipped();

                                // Check if this location is a battleground site (use real API)
                                boolean isBattleground = false;
                                try {
                                    if (location != null) {
                                        isBattleground = game.getModifiersQuerying().isBattleground(gameState, location, null);
                                    }
                                } catch (Exception bgE) {
                                    // Fallback to icon check
                                    if (blueprint != null) {
                                        if (blueprint.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                            && blueprint.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE)) {
                                            isBattleground = true;
                                        }
                                    }
                                }

                                DeployObjectiveSitingPolicy.IsbAgentEvaluation v279Isb =
                                    DeployObjectiveSitingPolicy.evaluateIsbAgent(
                                        new DeployObjectiveSitingPolicy.IsbAgentFacts(
                                            action.getActionId(), deployingIsISBAgent,
                                            deployAbility, isbOnTable, isbNeeded,
                                            preFlip, isBattleground));
                                applyDeploySitingPolicy(action, v279Isb.result());
                                if (v279Isb.outcome()
                                        == DeployObjectiveSitingPolicy.IsbAgentOutcome.ISB_AGENT) {
                                    logger.warn("V29.7 ISB: {} is ISB agent (ability {}) at {} ({}/{} on table, bg={}, +{})",
                                        decisionText, (int)deployAbility, title, isbOnTable, isbNeeded,
                                        isBattleground, (int)v279Isb.score());
                                } else {
                                    // V29.7: Non-ISB character — no penalty, just no ISB bonus.
                                    // ISB agents naturally win via their +200 bonus. Non-ISB still deployable
                                    // if no ISB agents are in hand.
                                    logger.info("V29.7 ISB: {} is non-ISB character — no bonus (ISB agents get +200 priority)",
                                        decisionText);
                                }
                            } catch (Exception e) {
                                logger.debug("V29.7 ISB: Error in ISB Operations scoring: {}", e.getMessage());
                            }
                        }

                        // === V29.7: ABILITY-BASED CHARACTER SCORING ===
                        // Higher ability characters are more valuable for location control.
                        // Ability >= 1 = presence, ability > opponent = control.
                        // Prefer deploying high-ability characters, especially at battleground sites.
                        if (isCharacter && deployingBlueprintId != null) {
                            try {
                                SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                if (deployBp != null && deployBp.hasAbilityAttribute()) {
                                    float charAbility = deployBp.getAbility();
                                    applyDeployCardValuePolicy(action,
                                        DeployCardValuePolicy.scoreDestinationAbility(
                                            new DeployCardValueFacts.DestinationAbility(
                                                action.getActionId(), charAbility)));
                                }
                            } catch (Exception e) {
                                logger.debug("V29.7 ABILITY: Error checking ability: {}", e.getMessage());
                            }
                        }

                        // === V25: HUNT DOWN V — VADER PRIORITY DEPLOYMENT ===
                        // When running Hunt Down V, Vader MUST be deployed to flip the objective.
                        // Without Vader: deck bleeds 1 Force/turn from Visage, can't flip, can't cancel drains.
                        // If Vader is not on table:
                        //   - Vader gets massive deploy bonus (+300) to any battleground site
                        //   - Non-Vader characters get heavy penalty (-200) to save Force for Vader
                        //   - Exception: Inquisitors still get a small allowance since they help battle destiny
                        if (isCharacter && !earlySpyDetected && locObjAnalyzer != null
                            && locObjAnalyzer.isAnalyzed() && locObjAnalyzer.isHuntDownV()) {
                            try {
                                boolean vaderOnTable = locObjAnalyzer.isVaderOnTable(gameState, playerId);
                                boolean preFlip = !locObjAnalyzer.isFlipped();

                                // Check if the deploying card IS Vader
                                boolean deployingIsVader = false;
                                boolean deployingIsInquisitor = false;
                                if (deployingBlueprintId != null) {
                                    SwccgCardBlueprint deployBp = getBlueprintFromId(context, deployingBlueprintId);
                                    if (deployBp != null) {
                                        deployingIsVader =
                                            objectiveProgressDeployingCard != null
                                                && com.gempukku.swccgo.filters
                                                    .Filters.Vader.accepts(
                                                    gameState,
                                                    game.getModifiersQuerying(),
                                                    objectiveProgressDeployingCard)
                                            || objectiveProgressDeployingCard
                                                == null
                                                && deployBp.hasPersona(
                                                    com.gempukku.swccgo.common
                                                        .Persona.VADER);
                                        // Check if Inquisitor — they have "inquisitor" in title or characteristics
                                        String depTitle = deployBp.getTitle();
                                        String depGameText = deployBp.getGameText();
                                        if (depTitle != null) {
                                            String depTitleLower = depTitle.toLowerCase(Locale.ROOT);
                                            deployingIsInquisitor = depTitleLower.contains("inquisitor")
                                                || depTitleLower.contains("fifth brother")
                                                || depTitleLower.contains("seventh sister")
                                                || depTitleLower.contains("eighth brother")
                                                || depTitleLower.contains("second sister")
                                                || depTitleLower.contains("grand inquisitor");
                                        }
                                    }
                                }

                                boolean v279HuntDownBattleground = false;
                                if (deployingIsVader) {
                                    v279HuntDownBattleground =
                                        objectiveProgressDeployingCard != null
                                            ? locObjAnalyzer
                                                .advancesPreFlipActorAtRuntimeLocation(
                                                    game, playerId,
                                                    objectiveProgressDeployingCard,
                                                    location)
                                            : locObjAnalyzer
                                                .isUnmetPreFlipRuntimeActorLocation(
                                                    game, playerId,
                                                    location);
                                }
                                DeployObjectiveSitingPolicy.HuntDownEvaluation v279HuntDown =
                                    DeployObjectiveSitingPolicy.evaluateHuntDownCharacter(
                                        new DeployObjectiveSitingPolicy.HuntDownFacts(
                                            action.getActionId(), deployingIsVader,
                                            deployingIsInquisitor, vaderOnTable, preFlip,
                                            v279HuntDownBattleground));
                                applyDeploySitingPolicy(action, v279HuntDown.result());
                                if (v279HuntDown.outcome()
                                        == DeployObjectiveSitingPolicy.HuntDownOutcome.VADER) {
                                    logger.warn("V25 HUNT DOWN: Vader deploy to {} — MASSIVE PRIORITY (+{})",
                                        title, v279HuntDownBattleground ? 400 : 300);
                                } else if (v279HuntDown.outcome()
                                        == DeployObjectiveSitingPolicy.HuntDownOutcome.INQUISITOR) {
                                        logger.warn("V25 HUNT DOWN: {} is Inquisitor — mild penalty while Vader not on table",
                                            decisionText);
                                } else if (v279HuntDown.outcome()
                                        == DeployObjectiveSitingPolicy.HuntDownOutcome.SAVE_FOR_VADER) {
                                        logger.warn("V25 HUNT DOWN: {} is NOT Vader — heavy penalty (-200) to save Force for Vader",
                                            decisionText);
                                }
                            } catch (Exception e) {
                                logger.debug("V25 HUNT DOWN: Error in Hunt Down scoring: {}", e.getMessage());
                            }
                        }

                        // === V25: CLOUD CITY ABILITY-BASED SPREAD STRATEGY (TDIGWATT) ===
                        // When TDIGWATT is active, spreading across Cloud City locations maximizes:
                        //   - Cloud City Occupation: +1 damage per CC location occupied
                        //   - Dark Deal: +1 to each force drain at CC locations
                        //   - Force drains at each occupied location
                        // V25: Use ABILITY (not character count) to decide when a site is secure.
                        // ~6 ability = can draw battle destiny and hold the site.
                        // Vader alone (ability 6-7) can hold a site. Lando alone (ability 2) cannot.
                        // V24.15: Skip CC spread scoring for spies — they don't contribute while undercover
                        if (isCharacter && !earlySpyDetected && locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                            && locObjAnalyzer.needsBespinSystemPresence()
                            && locObjAnalyzer.isObjectiveRelevantLocation(title)) {
                            try {
                                final float ABILITY_SECURE_THRESHOLD = 6.0f;

                                // Get our ability at THIS location
                                float ourAbilityHere = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                    gameState, playerId, location);

                                // V24.13: Check if Lando is alone at this location — he's a high-value target!
                                boolean landoAloneHere = false;
                                int ourCharsAtThisLoc = 0;
                                java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(location);
                                if (cardsAtLoc != null) {
                                    for (PhysicalCard c : cardsAtLoc) {
                                        if (c != null && playerId.equals(c.getOwner()) &&
                                            c.getBlueprint() != null &&
                                            c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                            ourCharsAtThisLoc++;
                                            String charTitle = c.getTitle();
                                            if (ourCharsAtThisLoc == 1 && charTitle != null &&
                                                charTitle.toLowerCase(java.util.Locale.ROOT).contains("lando")) {
                                                landoAloneHere = true;
                                            }
                                        }
                                    }
                                }
                                // Reset lando flag if more than 1 char
                                if (ourCharsAtThisLoc > 1) landoAloneHere = false;

                                // Check ALL objective-relevant locations for ability status
                                int locsEmpty = 0;
                                int locsInsecure = 0;  // Have presence but ability < threshold
                                int locsSecure = 0;    // Ability >= threshold
                                java.util.List<PhysicalCard> allLocs = gameState.getLocationsInOrder();
                                for (PhysicalCard checkLoc : allLocs) {
                                    if (checkLoc == null || checkLoc.getTitle() == null) continue;
                                    if (!locObjAnalyzer.isObjectiveRelevantLocation(checkLoc.getTitle())) continue;
                                    float abilityThere = game.getModifiersQuerying().getTotalAbilityAtLocation(
                                        gameState, playerId, checkLoc);
                                    if (abilityThere <= 0) {
                                        locsEmpty++;
                                    } else if (abilityThere < ABILITY_SECURE_THRESHOLD) {
                                        locsInsecure++;
                                    } else {
                                        locsSecure++;
                                    }
                                }

                                DeployObjectiveSitingPolicy.CloudCitySpreadEvaluation v279CcSpread =
                                    DeployObjectiveSitingPolicy.evaluateCloudCitySpread(
                                        new DeployObjectiveSitingPolicy.CloudCitySpreadFacts(
                                            action.getActionId(), ourAbilityHere, landoAloneHere,
                                            locsEmpty, locsInsecure, locsSecure));
                                applyDeploySitingPolicy(action, v279CcSpread.result());
                                if (v279CcSpread.outcome()
                                        == DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.LANDO_SUPPORT) {
                                    logger.warn("V24.13 LANDO ALONE: {} — Lando needs backup! (+250)", title);
                                } else if (v279CcSpread.outcome()
                                        == DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.REINFORCE) {
                                    float deficit = ABILITY_SECURE_THRESHOLD - ourAbilityHere;
                                    float reinforceBonus = 100.0f + (deficit * 15.0f);
                                    logger.warn("V25 ABILITY: {} has ability {} (need {}) — REINFORCE (+{})",
                                        title, String.format("%.0f", ourAbilityHere), String.format("%.0f", ABILITY_SECURE_THRESHOLD), (int)reinforceBonus);
                                } else if (v279CcSpread.outcome()
                                        == DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.SPREAD_DEFER) {
                                        logger.info("V25 ABILITY: {} unoccupied but {} sites insecure — moderate priority", title, locsInsecure);
                                } else if (v279CcSpread.outcome()
                                        == DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.SPREAD) {
                                        logger.warn("V25 ABILITY: {} unoccupied, all {} sites secure — SPREAD (+120)", title, locsSecure);
                                } else if (v279CcSpread.outcome()
                                        == DeployObjectiveSitingPolicy.CloudCitySpreadOutcome.SECURE_REDIRECT) {
                                        logger.info("V25 ABILITY: {} has ability {} (secure), {} sites need attention", title,
                                            String.format("%.0f", ourAbilityHere), (locsInsecure + locsEmpty));
                                }
                            } catch (Exception e) {
                                logger.debug("V25: Could not evaluate CC ability spread: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // V59 UNIVERSAL SPY SCORING — runs regardless of ObjectiveAnalyzer state.
                        // FIXES Issue #1 from peaceful-pike replay: Jyn Erso deployed to empty
                        // Upper Chamber (+165) instead of Entrance where opponent drains 2/turn,
                        // because the spy-aware scoring at line ~2201 was trapped inside
                        // `if (deployObjAnalyzer.isAnalyzed())`. When Rando's deck doesn't have
                        // an analyzed objective (e.g., "Like My Father Before Me" variants),
                        // spy placement fell back to generic icon-count scoring which ties every BG.
                        // This block scores spies BEFORE the objective-gated block and sets a flag
                        // to prevent double-counting downstream.
                        // =====================================================
                        boolean spyScoringApplied = false;
                        if (earlySpyDetected && game != null && location != null) {
                            try {
                                float ourPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, playerId, false, false);
                                String opp = game.getOpponent(playerId);
                                float oppPwr = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, opp, false, false);

                                DeployTacticalPolicy.UniversalSpyEvaluation v59Spy =
                                    DeployTacticalPolicy.evaluateV59UniversalSpy(
                                        new DeployTacticalPolicy.UniversalSpyFacts(
                                            action.getActionId(), title, oppPwr, ourPwr));
                                applyDeployTacticalPolicy(action, v59Spy.result());
                                if (v59Spy.outcome()
                                        == DeployTacticalPolicy.UniversalSpyOutcome.OPPONENT_ONLY) {
                                    logger.warn("V59 SPY UNIVERSAL: {} — opp {}, us 0 — IDEAL! (+600)", title, (int)oppPwr);
                                } else if (v59Spy.outcome()
                                        == DeployTacticalPolicy.UniversalSpyOutcome.BOTH_SIDES) {
                                    logger.warn("V59 SPY UNIVERSAL: {} — opp {}, us {} — hurts us (-200)",
                                        title, (int)oppPwr, (int)ourPwr);
                                } else if (v59Spy.outcome()
                                        == DeployTacticalPolicy.UniversalSpyOutcome.FRIENDLY_ONLY) {
                                    logger.warn("V59 SPY UNIVERSAL: {} — only us {} — BLOCKED (-2000)",
                                        title, (int)ourPwr);
                                } else {
                                    logger.warn("V59 SPY UNIVERSAL: {} — empty, wasted spy (-300)", title);
                                }
                                spyScoringApplied = true;
                            } catch (Exception e) {
                                logger.debug("V59 SPY UNIVERSAL: Error: {}", e.getMessage());
                            }
                        }

                        // =====================================================
                        // CRITICAL: Check power at location
                        // Don't deploy characters to contested locations we're losing!
                        // V22: Prefer own objective locations over opponent locations
                        // V24.15: EXEMPT SPIES from contest penalties!
                        // Spies deploy undercover — they don't fight battles.
                        // Contest penalty is meaningless for them. Their scoring is
                        // handled by the V24.14B spy scoring block below.
                        // =====================================================
                        if (isCharacter && game != null && !earlySpyDetected) {
                            try {
                                float ourPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, playerId, false, false);
                                String opponent = game.getOpponent(playerId);
                                float theirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                    game.getGameState(), location, opponent, false, false);

                                if (theirPower > 0) {
                                    Float deployPower = null;
                                    boolean v223ObjectiveRelevant = false;
                                    if (ourPower < theirPower) {
                                        deployPower = (blueprint != null && blueprint.hasPowerAttribute()) ? blueprint.getPower() : null;
                                        v223ObjectiveRelevant = locObjAnalyzer != null && locObjAnalyzer.isAnalyzed()
                                            && locObjAnalyzer.isObjectiveRelevantLocation(title);
                                    }
                                    float addedPower = (deployPower != null) ? deployPower : 0;
                                    DeployTacticalPolicy.ContestEvaluation v223Contest =
                                        DeployTacticalPolicy.evaluateV223Contest(
                                            new DeployTacticalPolicy.ContestFacts(
                                                action.getActionId(), ourPower, theirPower,
                                                addedPower, v223ObjectiveRelevant,
                                                plannedTargetId == null || !cardId.equals(plannedTargetId)));
                                    applyDeployTacticalPolicy(action, v223Contest.result());
                                    if (v223Contest.objectiveOverride() > 0.0f) {
                                        logger.warn("V22.7 OBJ CONTEST: {} is objective-critical — reducing contest penalty by {}",
                                            title, (int)v223Contest.objectiveOverride());
                                    }
                                    if (v223Contest.contestPenaltyApplied()) {
                                        logger.warn("V22.3 CONTEST: {} at {} losing {}-vs-{} penalty={}",
                                            title, (int)ourPower, (int)theirPower,
                                            v223Contest.contestPenalty());
                                    }
                                } else {
                                    // No opponent power - uncontested
                                    if (ourPower == 0
                                            && !guaranteedCaptureDeployDestination) {
                                        // V29: CHARACTER CONCENTRATION — don't deploy alone to empty locations
                                        // if there are solo friendlies at other locations that need backup.
                                        // Spreading characters thin gets them killed one by one.
                                        int soloFriendlyLocations = 0;
                                        int contestedSoloLocations = 0;
                                        try {
                                            java.util.List<PhysicalCard> allLocations = gameState.getTopLocations();
                                            if (allLocations != null) {
                                                for (PhysicalCard loc : allLocations) {
                                                    if (loc == null || loc.equals(location)) continue;
                                                    float locOurPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                        gameState, loc, playerId, false, false);
                                                    if (locOurPower > 0 && locOurPower <= 5) {
                                                        // Might be a solo character — count them
                                                        int charsHere = 0;
                                                        java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(loc);
                                                        if (cardsAtLoc != null) {
                                                            for (PhysicalCard c : cardsAtLoc) {
                                                                if (c != null && playerId.equals(c.getOwner())
                                                                    && c.getBlueprint() != null
                                                                    && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                                    charsHere++;
                                                                }
                                                            }
                                                        }
                                                        if (charsHere == 1) {
                                                            soloFriendlyLocations++;
                                                            String opponentId = game.getOpponent(playerId);
                                                            float locTheirPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                                gameState, loc, opponentId, false, false);
                                                            if (locTheirPower > 0) contestedSoloLocations++;
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            // Fallback — assume no solo friendlies
                                        }

                                        DeployFormationSitingPolicy.EmptyDestinationTopologyEvaluation
                                            v277EmptyTopology = DeployFormationSitingPolicy
                                                .evaluateEmptyDestinationTopology(
                                                    new DeployFormationSitingPolicy
                                                        .EmptyDestinationTopologyFacts(
                                                            action.getActionId(), title,
                                                            soloFriendlyLocations,
                                                            contestedSoloLocations));
                                        PolicyContributionLedger v277EmptyTopologyLedger =
                                            new PolicyContributionLedger(
                                                "deploy-formation-empty-topology-"
                                                    + action.getActionId());
                                        v277EmptyTopologyLedger.register(v277EmptyTopology.result());
                                        PolicyOperationAdapter.apply(action, v277EmptyTopologyLedger);

                                        if (v277EmptyTopology.outcome()
                                                == DeployFormationSitingPolicy
                                                    .EmptyDestinationTopologyOutcome.CONTESTED_SOLO) {
                                            logger.warn("V29 CONCENTRATE: Empty loc {} but {} contested solo friendlies — penalty {}", title, contestedSoloLocations, v277EmptyTopology.delta());
                                        } else if (v277EmptyTopology.outcome()
                                                == DeployFormationSitingPolicy
                                                    .EmptyDestinationTopologyOutcome.SOLO) {
                                            logger.info("V29 CONCENTRATE: Empty loc {} but {} solo friendlies elsewhere — penalty {}", title, soloFriendlyLocations, v277EmptyTopology.delta());
                                        }
                                    }
                                }

                                // V22.4 + V67bn: LONELY CHARACTER REINFORCEMENT
                                //
                                // V67bn (Steve, 2026-05-11): Extended the OLD V29 REINFORCE rule
                                // beyond its `ourPower <= 5` weakness gate. The old gate missed
                                // STRONG-but-outgunned solo chars — Vader (power 6) alone vs 2
                                // Jedi (power 8-13) failed the gate, so Yularen got pulled to a
                                // spy site (+940) instead of joining Vader (+180). Steve's rule:
                                // "deploy them with Vader and overpower the 2 jedi instead of
                                //  spreading to bait Rey."
                                //
                                // V67bn fires whenever there's exactly ONE friendly char at the
                                // destination AND the opponent's power exceeds ours by 4+ (same
                                // deficit threshold V67bj uses). Bonus magnitude +800 dominates
                                // V24.14B SPY +300 and V67as OPEN-FRONT +300, ensuring REINFORCE
                                // wins over SPREAD when an ally needs help.
                                //
                                // V29 REINFORCE (weak char, no opponent or moderate opponent) kept
                                // as the secondary rule for the original case.
                                if (ourPower > 0) {
                                    int ourCharsHere = 0;
                                    try {
                                        java.util.List<PhysicalCard> cardsHere = gameState.getCardsAtLocation(location);
                                        if (cardsHere != null) {
                                            for (PhysicalCard c : cardsHere) {
                                                if (c != null && playerId.equals(c.getOwner())) {
                                                    SwccgCardBlueprint cBp = c.getBlueprint();
                                                    if (cBp != null && cBp.getCardCategory() == CardCategory.CHARACTER) {
                                                        ourCharsHere++;
                                                    }
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        // Fallback
                                    }

                                    // 2026-06-03 DEFICIT UPPER CAP (Steve, Mustafar/Jabba replay):
                                    // "Rando deployed guys to fight me but very underpowered."
                                    // Replay: Audience Chamber, our 3 vs opp 12, deficit 9 → V67bn
                                    // fired +800 "REINFORCE OUTGUNNED" → Rando piled MORE power-3
                                    // chars in, each ate forfeits in the overflow battle without
                                    // closing the gap. Adding +3 to a -9 deficit still loses by 6
                                    // AND costs the extra char's forfeit/ability — net loss vs
                                    // just losing the original battle.
                                    // Braveheart rationale only holds when reinforcement can
                                    // PLAUSIBLY close the gap: 1 mid-power char (~4) absorbs a
                                    // 4-deficit, maybe a 5. Beyond that, the gap is unclosable
                                    // and reinforcing only hands the opponent more forfeits. Cap
                                    // the gate at deficit ≤ 5: trapped + reasonably close → pile
                                    // on; trapped + hopelessly outgunned → don't double down,
                                    // let the existing battle just resolve.
                                    float v67bnDeficit = theirPower - ourPower;
                                    boolean v67bnOutgunned = v67bnDeficit >= 4f && v67bnDeficit <= 5f;
                                    // V67bu (Steve, 2026-05-11): extend V67bn to ANY committed-friendly
                                    // count (was solo-only). Steve's "Braveheart" rule: when chars are
                                    // already committed to an outgunned site AND can't escape, pile on
                                    // reinforcements to MINIMIZE overflow damage — 15+ force overflow is
                                    // game-ending, so even losing by less wins the war of attrition.
                                    //
                                    // Escape-route check: don't reinforce if outgunned chars can flee.
                                    //   1. Adjacent site on same planet has Rando friendlies (consolidate)
                                    //   2. Same parent system has Rando's starship (shuttle aboard)
                                    // If escape exists → Move evaluator (V67au) handles retreat next phase.
                                    boolean v67buCanEscape = false;
                                    if (ourCharsHere >= 1 && v67bnOutgunned) {
                                        try {
                                            String locTitleLower = location.getTitle() != null
                                                ? location.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                            // Extract planet prefix (e.g., "cloud city" from "cloud city: upper walkway")
                                            String planetPrefix = locTitleLower.contains(":")
                                                ? locTitleLower.substring(0, locTitleLower.indexOf(":")).trim()
                                                : locTitleLower;
                                            // Escape case 1: same-planet site has Rando friendlies
                                            // Escape case 2: same parent (e.g. system or matching planet) has Rando starship
                                            for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                                if (pc == null || pc.getBlueprint() == null) continue;
                                                if (!playerId.equals(pc.getOwner())) continue;
                                                if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                                                CardCategory pcCat = pc.getBlueprint().getCardCategory();
                                                // Friendly char at adjacent same-planet site
                                                if (pcCat == CardCategory.CHARACTER) {
                                                    PhysicalCard pcLoc = pc.getAtLocation();
                                                    if (pcLoc == null || pcLoc == location) continue;
                                                    String pcLocTitle = pcLoc.getTitle();
                                                    if (pcLocTitle == null) continue;
                                                    String pcLocLower = pcLocTitle.toLowerCase(java.util.Locale.ROOT);
                                                    if (!planetPrefix.isEmpty() && pcLocLower.startsWith(planetPrefix)) {
                                                        v67buCanEscape = true;
                                                        break;
                                                    }
                                                }
                                                // Friendly starship at same parent system (shuttle aboard)
                                                if (pcCat == CardCategory.STARSHIP) {
                                                    PhysicalCard pcLoc = pc.getAtLocation();
                                                    if (pcLoc == null) continue;
                                                    String pcLocTitle = pcLoc.getTitle();
                                                    if (pcLocTitle == null) continue;
                                                    String pcLocLower = pcLocTitle.toLowerCase(java.util.Locale.ROOT);
                                                    if (!planetPrefix.isEmpty() && pcLocLower.startsWith(planetPrefix)) {
                                                        v67buCanEscape = true;
                                                        break;
                                                    }
                                                }
                                            }
                                        } catch (Exception eEsc) {
                                            logger.debug("V67bu escape-check error: {}", eEsc.getMessage());
                                        }
                                    }
                                    DeployFormationSitingPolicy.ReinforcementTopologyEvaluation
                                        v277Reinforcement = DeployFormationSitingPolicy
                                            .evaluateReinforcementTopology(
                                                new DeployFormationSitingPolicy
                                                    .ReinforcementTopologyFacts(
                                                        action.getActionId(), title,
                                                        ourCharsHere, ourPower, theirPower,
                                                        v67buCanEscape));
                                    PolicyContributionLedger v277ReinforcementLedger =
                                        new PolicyContributionLedger(
                                            "deploy-formation-reinforcement-"
                                                + action.getActionId());
                                    v277ReinforcementLedger.register(v277Reinforcement.result());
                                    PolicyOperationAdapter.apply(action, v277ReinforcementLedger);

                                    if (v277Reinforcement.outcome()
                                            == DeployFormationSitingPolicy
                                                .ReinforcementTopologyOutcome.V67BN_NO_ESCAPE) {
                                        logger.warn("V67bn REINFORCE OUTGUNNED: dest={} chars={} our={} opp={} deficit={} no-escape → +800",
                                            title, ourCharsHere, (int)ourPower, (int)theirPower, (int)(theirPower-ourPower));
                                    } else if (v277Reinforcement.outcome()
                                            == DeployFormationSitingPolicy
                                                .ReinforcementTopologyOutcome.V67BU_ESCAPE) {
                                        // V67bu — escape available, let Move evaluator handle it
                                        logger.info("V67bu ESCAPE AVAILABLE at {} (our {} vs opp {}) — skip reinforce, Move evaluator will retreat",
                                            title, (int)ourPower, (int)theirPower);
                                    } else if (v277Reinforcement.outcome()
                                            == DeployFormationSitingPolicy
                                                .ReinforcementTopologyOutcome.LEGACY_SOLO) {
                                        logger.info("V29 REINFORCE: Solo char at {} (power {}), opponent power {}, bonus={}",
                                            title, (int)ourPower, (int)theirPower,
                                            v277Reinforcement.delta());
                                    }
                                }

                                // === V29.5: GENERAL BUDDY SYSTEM — PREFER OWN LOCATIONS ===
                                // Characters should prefer deploying to locations they OWN or have
                                // friendly presence at. Deploying alone to opponent-controlled empty
                                // locations is bad — the opponent will likely reinforce and kill you.
                                // This applies to ALL decks, not just TDIGWATT.
                                //
                                // V29.6: EMPTY TABLE AWARENESS — If we have NO friendly characters
                                // anywhere on the table, someone has to go first! Reduce penalties
                                // so Rando doesn't stall. Still prefer own locations, but don't
                                // refuse to deploy just because only opponent locations exist.
                                if (isCharacter && location != null && playerId != null
                                        && !guaranteedCaptureDeployDestination) {
                                    try {
                                        // Check location ownership
                                        String locOwner = location.getOwner();
                                        String opponentIdBuddy = gameState.getOpponent(playerId);
                                        boolean isOurLocation = playerId.equals(locOwner);
                                        boolean isOpponentLocation = opponentIdBuddy != null && opponentIdBuddy.equals(locOwner);

                                        // Count friendly and opponent characters at this location
                                        int friendlyCharsHereBuddy = 0;
                                        int opponentCharsHereBuddy = 0;
                                        java.util.List<PhysicalCard> buddyCards = gameState.getCardsAtLocation(location);
                                        if (buddyCards != null) {
                                            for (PhysicalCard bc : buddyCards) {
                                                if (bc != null && bc.getBlueprint() != null
                                                    && bc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                    if (playerId.equals(bc.getOwner())) {
                                                        friendlyCharsHereBuddy++;
                                                    } else {
                                                        opponentCharsHereBuddy++;
                                                    }
                                                }
                                            }
                                        }

                                        // V29.6: Count TOTAL friendly characters on the entire table.
                                        // If zero, this is the FIRST deploy — penalties must be softer.
                                        int totalFriendlyCharsOnTable = 0;
                                        try {
                                            java.util.List<PhysicalCard> allLocations = gameState.getTopLocations();
                                            if (allLocations != null) {
                                                for (PhysicalCard loc : allLocations) {
                                                    java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(loc);
                                                    if (cardsAtLoc != null) {
                                                        for (PhysicalCard pc : cardsAtLoc) {
                                                            if (pc != null && playerId.equals(pc.getOwner())
                                                                && pc.getBlueprint() != null
                                                                && pc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                                totalFriendlyCharsOnTable++;
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V29.6 BUDDY: Error counting total friendlies: {}", e.getMessage());
                                        }
                                        boolean emptyTable = (totalFriendlyCharsOnTable == 0);

                                        float depAbility113 = 0.0f;
                                        if (friendlyCharsHereBuddy == 0 && !emptyTable) {
                                            try {
                                                if (deployingBlueprintId != null) {
                                                    SwccgCardBlueprint depBp113 = getBlueprintFromId(context, deployingBlueprintId);
                                                    if (depBp113 != null) depAbility113 = depBp113.getAbility();
                                                }
                                            } catch (Exception e113) { /* ignore */ }
                                        }

                                        DeployFormationSitingPolicy.CharacterFormationFacts v212BuddyFacts =
                                            new DeployFormationSitingPolicy.CharacterFormationFacts(
                                                true, location.getTitle(), isOurLocation,
                                                isOpponentLocation, friendlyCharsHereBuddy,
                                                opponentCharsHereBuddy, emptyTable,
                                                deployingCardName, depAbility113,
                                                0.0f, 0.0f, false);
                                        PolicyContributionLedger v212BuddyLedger =
                                            new PolicyContributionLedger(
                                                "deploy-formation-buddy-" + action.getActionId());
                                        v212BuddyLedger.register(
                                            DeployFormationSitingPolicy.evaluateBuddyTopology(
                                                action.getActionId(), v212BuddyFacts));
                                        PolicyOperationAdapter.apply(action, v212BuddyLedger);
                                    } catch (Exception e) {
                                        logger.debug("V29.5 BUDDY: Error checking location ownership: {}", e.getMessage());
                                    }
                                }

                                // === V29.7: BATTLEGROUND PREFERENCE FOR CHARACTER DEPLOYMENT ===
                                // Characters prefer deploying to battleground sites for meaningful
                                // force drains and battles. BONUS for battlegrounds, but only apply
                                // a penalty for non-battlegrounds when battleground alternatives exist.
                                // V29.7 FIX: Many decks (ISB, TDIGWATT) operate at non-BG interior
                                // sites. Penalizing non-BG when NO BG sites are on the table blocks
                                // ALL deploys! Only penalize when the player has BG options.
                                if (isCharacter && location != null && game != null && gameState != null) {
                                    try {
                                        boolean isBattlegroundSite = game.getModifiersQuerying().isBattleground(gameState, location, null);
                                        boolean anyBattlegroundExists = false;
                                        int v67ahOppIcons = 0;
                                        if (!isBattlegroundSite) {
                                            // V29.7: Check if ANY battleground sites are accessible before penalizing.
                                            // If no BG sites exist on the table, don't penalize — deploy somewhere!
                                            try {
                                                for (PhysicalCard bgLoc : gameState.getTopLocations()) {
                                                    if (bgLoc != null) {
                                                        boolean bgCheck = game.getModifiersQuerying().isBattleground(gameState, bgLoc, null);
                                                        if (bgCheck) {
                                                            anyBattlegroundExists = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception bgE) { /* ignore */ }

                                            if (anyBattlegroundExists) {
                                                // V67ah (Steve, 2026-05-04): 'Deploying to non
                                                // battleground sites is mostly useless.' Old -60 was
                                                // too weak. But Sidious-to-The-Works is OK as drain
                                                // staging when the site has opp icons. Tiered penalty:
                                                //   - Non-BG with drain icons (opp force):  -100
                                                //     (acceptable first-character drain post; V67ag
                                                //     adds another -300 if a friendly is already there)
                                                //   - Non-BG with zero opp icons (truly useless): -350
                                                //     (no battles AND no drain — pure waste)
                                                try {
                                                    com.gempukku.swccgo.common.Side mySide67ah = context.getSide();
                                                    com.gempukku.swccgo.game.SwccgCardBlueprint locBp67ah =
                                                        location.getBlueprint();
                                                    if (locBp67ah != null) {
                                                        v67ahOppIcons = (mySide67ah == com.gempukku.swccgo.common.Side.LIGHT)
                                                            ? locBp67ah.getIconCount(com.gempukku.swccgo.common.Icon.DARK_FORCE)
                                                            : locBp67ah.getIconCount(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                                                    }
                                                } catch (Exception e) { /* ignore */ }
                                            }
                                        }
                                        DeployFormationSitingPolicy.CharacterBattlegroundPreferenceFacts
                                            v281BattlegroundFacts = new DeployFormationSitingPolicy
                                                .CharacterBattlegroundPreferenceFacts(
                                                    action.getActionId(), isBattlegroundSite,
                                                    anyBattlegroundExists, v67ahOppIcons);
                                        applyDeploySitingPolicy(action,
                                            DeployFormationSitingPolicy
                                                .scoreCharacterBattlegroundPreference(
                                                    v281BattlegroundFacts));
                                    } catch (Exception e) {
                                        logger.debug("V29.7 BATTLEGROUND: Error checking battleground status: {}", e.getMessage());
                                    }
                                }

                                // === V24.3B: DR. EVAZAN WEAPON COMBO — DEPLOY LOCATION PREFERENCE ===
                                // Deploy Evazan to sites with weapon chars, and weapon chars to sites with Evazan.
                                // Evazan converts weapon hits into immediate character loss — devastating combo.
                                if (isCharacter && decisionText != null) {
                                    boolean deployingEvazan = decisionText.contains("evazan");
                                    boolean deployingWeaponChar = (decisionText.contains("maul") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("vader") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("mara") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("jade") && decisionText.contains("lightsaber"))
                                        || (decisionText.contains("aurra") && decisionText.contains("blaster"))
                                        || (decisionText.contains("sing") && decisionText.contains("blaster"));

                                    if (deployingEvazan || deployingWeaponChar) {
                                        // Scan cards at this location for combo partner
                                        boolean comboPartnerHere = false;
                                        try {
                                            java.util.List<PhysicalCard> cardsAtLoc = gameState.getCardsAtLocation(location);
                                            if (cardsAtLoc != null) {
                                                for (PhysicalCard c : cardsAtLoc) {
                                                    if (c == null || !playerId.equals(c.getOwner())) continue;
                                                    String cTitle = c.getTitle();
                                                    if (cTitle == null) continue;
                                                    String cTitleLower = cTitle.toLowerCase();

                                                    if (deployingEvazan) {
                                                        // Looking for weapon characters
                                                        if ((cTitleLower.contains("maul") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("vader") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("mara") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("jade") && cTitleLower.contains("lightsaber"))
                                                            || (cTitleLower.contains("aurra") && cTitleLower.contains("blaster"))
                                                            || (cTitleLower.contains("sing") && cTitleLower.contains("blaster"))) {
                                                            comboPartnerHere = true;
                                                            break;
                                                        }
                                                    } else {
                                                        // Deploying weapon char — looking for Evazan
                                                        if (cTitleLower.contains("evazan")) {
                                                            comboPartnerHere = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        } catch (Exception e) { /* ignore */ }

                                        DeployTacticalPolicy.V243BPartnerEvaluation v243bPartner =
                                            DeployTacticalPolicy.evaluateV243BPartner(
                                                new DeployTacticalPolicy.V243BPartnerFacts(
                                                    action.getActionId(), comboPartnerHere));
                                        applyDeployTacticalPolicy(action, v243bPartner.result());
                                        if (v243bPartner.outcome()
                                                == DeployTacticalPolicy.V243BPartnerOutcome.PARTNER_PRESENT) {
                                            logger.warn("V24.3 EVAZAN COMBO: {} — combo partner found at {} (+200)", decisionText, title);
                                        }
                                    }
                                }

                                boolean v279LandoDeploy = isCharacter && decisionText != null
                                    && decisionText.contains("lando");
                                DeployObjectiveSitingPolicy.LandoDestinationEvaluation v279LandoDestination =
                                    DeployObjectiveSitingPolicy.evaluateLandoDestination(
                                        new DeployObjectiveSitingPolicy.LandoDestinationFacts(
                                            action.getActionId(), v279LandoDeploy, title));
                                applyDeploySitingPolicy(action, v279LandoDestination.result());
                                if (v279LandoDestination.outcome()
                                        == DeployObjectiveSitingPolicy.LandoDestinationOutcome.DINING_ROOM) {
                                    logger.warn("V24.10 LANDO: Dining Room +300 — ideal deploy location for Lando!");
                                } else if (v279LandoDestination.outcome()
                                        == DeployObjectiveSitingPolicy.LandoDestinationOutcome.OTHER_CLOUD_CITY_SITE) {
                                    logger.warn("V24.10 LANDO: {} is CC but not Dining Room — mild penalty (-50)", title);
                                }

                                if (v279LandoDeploy) {

                                    // === V25: LANDO ALONE PROTECTION ===
                                    // NEVER deploy Lando to a CC site where he'd be alone and unprotected.
                                    // Lando alone (ability 2, power 3) is an easy kill for any Jedi.
                                    // Rey killed Lando alone EVERY TURN in testing — catastrophic Force losses.
                                    // Only deploy Lando if:
                                    //   (a) friendlies already at the site, OR
                                    //   (b) we have characters in hand we can deploy alongside him, OR
                                    //   (c) Turn 1 and opponent has no CC presence yet (early establish OK)
                                    if (game != null && gameState != null) {
                                        try {
                                            int friendlyCharsAtSite = 0;
                                            int opponentCharsAtAnyCCSite = 0;
                                            int charsInHand = 0;
                                            String opponentId = gameState.getOpponent(playerId);

                                            // Count friendlies AND opponents at THIS location
                                            int opponentCharsAtThisSite = 0;
                                            java.util.List<PhysicalCard> siteCards = gameState.getCardsAtLocation(location);
                                            if (siteCards != null) {
                                                for (PhysicalCard sc : siteCards) {
                                                    if (sc != null && sc.getBlueprint() != null &&
                                                        sc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                        if (playerId.equals(sc.getOwner())) {
                                                            friendlyCharsAtSite++;
                                                        } else if (opponentId != null && opponentId.equals(sc.getOwner())) {
                                                            opponentCharsAtThisSite++;
                                                        }
                                                    }
                                                }
                                            }

                                            // Count characters in hand (potential protectors)
                                            java.util.List<PhysicalCard> hand = gameState.getHand(playerId);
                                            if (hand != null) {
                                                for (PhysicalCard hc : hand) {
                                                    if (hc != null && hc.getBlueprint() != null &&
                                                        hc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                        charsInHand++;
                                                    }
                                                }
                                            }

                                            // Check opponent presence at CC sites
                                            for (PhysicalCard checkLoc : gameState.getLocationsInOrder()) {
                                                if (checkLoc == null || checkLoc.getTitle() == null) continue;
                                                if (checkLoc == location) continue; // skip this site
                                                String checkLocLower = checkLoc.getTitle().toLowerCase(java.util.Locale.ROOT);
                                                boolean isCCsite = checkLocLower.contains("cloud city") || checkLocLower.contains("upper walkway")
                                                    || checkLocLower.contains("carbonite") || checkLocLower.contains("security tower")
                                                    || checkLocLower.contains("dining room") || checkLocLower.contains("platform")
                                                    || checkLocLower.contains("lower corridor");
                                                if (!isCCsite) continue;
                                                java.util.List<PhysicalCard> ccCards = gameState.getCardsAtLocation(checkLoc);
                                                if (ccCards != null) {
                                                    for (PhysicalCard cc : ccCards) {
                                                        if (cc != null && cc.getOwner() != null) {
                                                            if (opponentId != null && opponentId.equals(cc.getOwner()) &&
                                                                cc.getBlueprint() != null &&
                                                                cc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                                opponentCharsAtAnyCCSite++;
                                                            }
                                                            // (could track friendly chars at other CC sites here if needed)
                                                        }
                                                    }
                                                }
                                            }

                                            boolean opponentThreatens = (opponentCharsAtAnyCCSite + opponentCharsAtThisSite) > 0;
                                            DeployObjectiveSitingPolicy.LandoSafetyEvaluation v279LandoSafety =
                                                DeployObjectiveSitingPolicy.evaluateLandoSafety(
                                                    new DeployObjectiveSitingPolicy.LandoSafetyFacts(
                                                        action.getActionId(), true, title,
                                                        friendlyCharsAtSite, opponentCharsAtThisSite,
                                                        charsInHand, opponentThreatens));
                                            applyDeploySitingPolicy(action, v279LandoSafety.result());
                                            if (v279LandoSafety.outcome()
                                                    == DeployObjectiveSitingPolicy.LandoSafetyOutcome.BLOCKED_ENEMY) {
                                                logger.warn("V41 LANDO INTO ENEMY: {} opponents at {} — HARD BLOCK! Lando would die!",
                                                    opponentCharsAtThisSite, title);
                                            } else if (v279LandoSafety.outcome()
                                                    == DeployObjectiveSitingPolicy.LandoSafetyOutcome.SAFE_FRIENDLY) {
                                                logger.info("V25 LANDO: {} — {} friendlies here — safe to deploy", title, friendlyCharsAtSite);
                                            } else if (v279LandoSafety.outcome()
                                                    == DeployObjectiveSitingPolicy.LandoSafetyOutcome.BLOCKED_ALONE) {
                                                logger.warn("V47 LANDO ALONE: {} — no friendlies, no hand chars — HARD BLOCK!", title);
                                            } else if (v279LandoSafety.outcome()
                                                    == DeployObjectiveSitingPolicy.LandoSafetyOutcome.CAUTION) {
                                                logger.warn("V25 LANDO: {} — alone + opponent at CC, but {} chars in hand (-100)",
                                                    title, charsInHand);
                                            } else if (v279LandoSafety.outcome()
                                                    == DeployObjectiveSitingPolicy.LandoSafetyOutcome.SAFE_HAND) {
                                                logger.info("V25 LANDO: {} — alone but {} chars in hand and no CC threats — OK", title, charsInHand);
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V25 LANDO ALONE CHECK: Error: {}", e.getMessage());
                                        }
                                    }
                                }

                                // V22/V22.2: Strongly prefer deploying to objective locations
                                // Post-flip: scale required power based on opponent threat
                                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer deployObjAnalyzer =
                                    context.getObjectiveAnalyzer();
                                if (deployObjAnalyzer != null && deployObjAnalyzer.isAnalyzed() && title != null) {
                                    boolean isObjLocation = deployObjAnalyzer.isObjectiveRelevantLocation(title);
                                    boolean isFlipBackLocation = deployObjAnalyzer.isFlipBackProtectionLocation(title);
                                    boolean objectiveIsFlipped = deployObjAnalyzer.isFlipped();

                                    // V24.2E (V24.9/V24.14B fix): Detect undercover spy deployment.
                                    // UNIVERSAL: Check deploying card's blueprint game text for "undercover".
                                    // This works for ALL spy cards without hardcoding names.
                                    // Also uses early detection result from V24.14B if available.
                                    boolean isUndercoverSpy = earlySpyDetected;  // Reuse V24.14B early detection
                                    // Method 1 (UNIVERSAL): Check deploying card's blueprint game text
                                    if (!isUndercoverSpy && deployingBlueprintId != null) {
                                        try {
                                            SwccgCardBlueprint spyCheckBp = getBlueprintFromId(context, deployingBlueprintId);
                                            if (spyCheckBp != null) {
                                                String spyCheckText = spyCheckBp.getGameText();
                                                if (spyCheckText != null && spyCheckText.toLowerCase(java.util.Locale.ROOT).contains("undercover")) {
                                                    isUndercoverSpy = true;
                                                    logger.warn("V24.14B SPY DETECT Method 1: Blueprint game text contains 'undercover'!");
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V24.14B SPY DETECT Method 1: Error: {}", e.getMessage());
                                        }
                                    }
                                    // Method 1b (fallback): Decision text keywords
                                    if (!isUndercoverSpy) {
                                        if (decisionText.contains("undercover") || decisionText.contains("as a spy")) {
                                            isUndercoverSpy = true;
                                            logger.warn("V24.14B SPY DETECT Method 1b: Decision text spy keyword!");
                                        }
                                    }
                                    // V67bt (Steve, 2026-05-11): METHOD 2 REMOVED.
                                    //
                                    // The old heuristic was: "if location options include both our-side
                                    // and opponent-side sites, must be a spy deploy." That's wrong —
                                    // any non-pilot character is offered both-sides sites in normal
                                    // deploy decisions; it's not a spy signal at all.
                                    //
                                    // Method 2 false-positively tagged General Nevar, Myn Kyneugh,
                                    // and similar non-spies as spies. The spy scoring then applied
                                    // -2000 at Rando's concentration sites ("spy blocks our drain")
                                    // and +300 at opp-occupied sites ("ideal spy spot"). Result:
                                    // Rando deployed Nevar (power 3) solo at Cloud City: Lower
                                    // Corridor across from Rey (power 7) → 22 battle damage, concede.
                                    //
                                    // Steve's rule (and memory file feedback_card_search_by_type_not_text):
                                    // detect spies BY GAME TEXT or KEYWORD, never by heuristic.
                                    // Methods 1 (decision text "undercover"/"spy") and Method 3
                                    // (blueprint game text "undercover") are the correct typed checks.
                                    // Method 2 is permanently removed.
                                    // Method 3: Check deploying card's blueprint game text for "undercover" keyword.
                                    // User confirmed: spy cards have "undercover" in their game text.
                                    // deployingBlueprintId is extracted from the decision text HTML earlier in this method.
                                    if (!isUndercoverSpy && deployingBlueprintId != null) {
                                        try {
                                            SwccgCardBlueprint deployingBp = getBlueprintFromId(context, deployingBlueprintId);
                                            if (deployingBp != null) {
                                                String spyGameText = deployingBp.getGameText();
                                                if (spyGameText != null && spyGameText.toLowerCase(java.util.Locale.ROOT).contains("undercover")) {
                                                    isUndercoverSpy = true;
                                                    logger.info("V24.9 SPY DETECT Method 3: Blueprint game text contains 'undercover' — this is a spy deploy!");
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V24.9 SPY DETECT Method 3: Error checking blueprint: {}", e.getMessage());
                                        }
                                    }

                                    if (isUndercoverSpy && !spyScoringApplied) {
                                        // V24.14B: SPY LOCATION SCORING — check WHO has presence.
                                        // Spy blocks force drains for BOTH sides at a location.
                                        // GOOD: Deploy spy where opponent has presence and we DON'T → blocks their drain.
                                        // BAD: Deploy spy where only WE have presence → blocks OUR drain.
                                        // CC/objective locations need same logic — opponent CAN deploy to our CC sites!
                                        // V59: Skipped when spyScoringApplied=true (universal scoring already ran).
                                        float oppPowerHere = 0;
                                        try {
                                            oppPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                game.getGameState(), location, opponent, false, false);
                                        } catch (Exception e) { /* ignore */ }

                                        // UPDATED (Steve, 2026-06): two gaps in this WHO-has-presence
                                        // scoring. (1) ourPower EXCLUDES undercover spies, so a 2nd spy
                                        // onto a site that already has a friendly spy read as "ideal"
                                        // (+300) — detect the existing spy and block the double-up
                                        // (Steve's mistake 2: 2nd spy stacked while Dooku drained 3
                                        // elsewhere). (2) the both-sides case was a flat -50, too weak
                                        // to stop a wasted spy on a site we already hold; condition it
                                        // on whether we could FLIP this spy to buddy-battle (Steve's
                                        // caveat): our character power + this spy's flipped power vs opp.
                                        boolean v24bSpyHere = false;
                                        try {
                                            for (PhysicalCard v24bC : game.getGameState().getCardsAtLocation(location)) {
                                                if (v24bC != null && playerId.equals(v24bC.getOwner())
                                                        && v24bC.isUndercover()) { v24bSpyHere = true; break; }
                                            }
                                        } catch (Exception ignore) { }
                                        float v24bSpyPow = 0f;
                                        try {
                                            SwccgCardBlueprint v24bBp = getBlueprintFromId(context, deployingBlueprintId);
                                            if (v24bBp != null && v24bBp.hasPowerAttribute() && v24bBp.getPower() != null)
                                                v24bSpyPow = v24bBp.getPower();
                                        } catch (Exception ignore) { }

                                        DeployTacticalPolicy.FallbackSpyEvaluation v2414bSpy =
                                            DeployTacticalPolicy.evaluateV2414BFallbackSpy(
                                                new DeployTacticalPolicy.FallbackSpyFacts(
                                                    action.getActionId(), title, v24bSpyHere,
                                                    oppPowerHere, ourPower, v24bSpyPow,
                                                    isObjLocation || isFlipBackLocation));
                                        applyDeployTacticalPolicy(action, v2414bSpy.result());
                                        if (v2414bSpy.outcome()
                                                == DeployTacticalPolicy.FallbackSpyOutcome.FRIENDLY_SPY_DOUBLED) {
                                            logger.warn("V24.14B SPY DOUBLED: {} — friendly spy already here — -1200 (route to open drain)", title);
                                        } else if (v2414bSpy.outcome()
                                                == DeployTacticalPolicy.FallbackSpyOutcome.OPPONENT_ONLY) {
                                            logger.warn("V24.14B SPY: {} — opp power {}, our power 0 — IDEAL spy location! (+300)", title, oppPowerHere);
                                        } else if (v2414bSpy.outcome()
                                                == DeployTacticalPolicy.FallbackSpyOutcome.FLIP_BUDDY) {
                                            logger.warn("V24.14B SPY FLIP-BUDDY: {} — char {} + spy {} >= opp {} — allow (flip to fight)", title, ourPower, v24bSpyPow, oppPowerHere);
                                        } else if (v2414bSpy.outcome()
                                                == DeployTacticalPolicy.FallbackSpyOutcome.BOTH_SIDES_OBJECTIVE) {
                                            logger.warn("V24.14B SPY: {} — both sides at CC, can't flip-win — bad (-500)", title);
                                        } else if (v2414bSpy.outcome()
                                                == DeployTacticalPolicy.FallbackSpyOutcome.BOTH_SIDES_NON_OBJECTIVE) {
                                            logger.warn("V24.14B SPY: {} — both sides, can't flip-win — wasted (-800)", title);
                                        } else if (v2414bSpy.outcome()
                                                == DeployTacticalPolicy.FallbackSpyOutcome.FRIENDLY_ONLY) {
                                            logger.warn("V24.14B SPY: {} — only our power {} — spy HURTS us! (-2000)", title, ourPower);
                                        } else if (v2414bSpy.outcome()
                                                == DeployTacticalPolicy.FallbackSpyOutcome.EMPTY_OBJECTIVE) {
                                            logger.warn("V24.14B SPY: {} — empty CC site, spy wastes potential drain (-300)", title);
                                        }
                                    } else if (!isObjLocation && !isFlipBackLocation) {
                                        boolean v279NeedsBespin = isCharacter
                                            && deployObjAnalyzer.needsBespinSystemPresence();
                                        String v279TitleLower = title.toLowerCase(java.util.Locale.ROOT);
                                        boolean v279OpponentPlanet = v279NeedsBespin
                                            && (v279TitleLower.contains("tatooine")
                                                || v279TitleLower.contains("endor")
                                                || v279TitleLower.contains("dagobah")
                                                || v279TitleLower.contains("naboo")
                                                || v279TitleLower.contains("yavin")
                                                || v279TitleLower.contains("hoth")
                                                || v279TitleLower.contains("jakku")
                                                || v279TitleLower.contains("chandrila"));
                                        DeployObjectiveSitingPolicy.TdgwattOffObjectiveEvaluation v279Tdgwatt =
                                            DeployObjectiveSitingPolicy.evaluateTdgwattOffObjective(
                                                new DeployObjectiveSitingPolicy.TdgwattOffObjectiveFacts(
                                                    action.getActionId(), isCharacter,
                                                    v279NeedsBespin, v279OpponentPlanet));
                                        applyDeploySitingPolicy(action, v279Tdgwatt.result());
                                        if (v279Tdgwatt.tdgwattBlocked()) {
                                            logger.warn("V29 TDIGWATT: Blocking character deploy to non-objective location {} (-500)", title);
                                        }
                                        if (v279Tdgwatt.opponentPlanet()) {
                                            logger.warn("V29 OPPONENT PLANET: {} is opponent's territory — extra -300", title);
                                        }
                                        // Non-objective location: penalize, scale by urgency
                                        boolean objLocationNeedsHelp = false;
                                        float worstDeficit = 0;
                                        java.util.List<PhysicalCard> allLocs = game.getGameState().getLocationsInOrder();
                                        // opponent already declared above in this scope
                                        for (PhysicalCard checkLoc : allLocs) {
                                            if (checkLoc == null) continue;
                                            String checkTitle = checkLoc.getTitle();
                                            if (checkTitle == null) continue;
                                            boolean needsProtection = objectiveIsFlipped
                                                ? deployObjAnalyzer.isFlipBackProtectionLocation(checkTitle)
                                                : deployObjAnalyzer.isObjectiveRelevantLocation(checkTitle);
                                            if (!needsProtection) continue;

                                            float ourPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                game.getGameState(), checkLoc, playerId, false, false);
                                            float theirPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                                game.getGameState(), checkLoc, opponent, false, false);

                                            // V22.2: Dynamic threshold — need MORE power when opponent is strong
                                            // Base threshold 8, plus match opponent power with a margin
                                            float requiredPower = Math.max(8.0f, theirPowerThere + 4.0f);
                                            if (ourPowerThere < requiredPower) {
                                                objLocationNeedsHelp = true;
                                                float deficit = requiredPower - ourPowerThere;
                                                if (deficit > worstDeficit) worstDeficit = deficit;
                                            }
                                        }
                                        DeployObjectiveSitingPolicy.ObjectiveTailEvaluation v279ObjectiveTail =
                                            DeployObjectiveSitingPolicy.evaluateObjectiveTail(
                                                new DeployObjectiveSitingPolicy.ObjectiveTailFacts(
                                                    action.getActionId(), objectiveIsFlipped,
                                                    false, false,
                                                    objLocationNeedsHelp, worstDeficit));
                                        applyDeploySitingPolicy(action, v279ObjectiveTail.result());
                                        if (v279ObjectiveTail.fortificationNeeded()) {
                                            float penalty = objectiveIsFlipped ? -180.0f : -120.0f;
                                            if (worstDeficit > 6) penalty -= 40.0f;
                                            logger.warn("V22.2 DEPLOY: Penalizing {} ({}), obj locs need +{} power{}",
                                                title, penalty, (int)worstDeficit,
                                                objectiveIsFlipped ? " [FLIPPED - PROTECT!]" : "");
                                        }
                                    } else if (objectiveIsFlipped && isFlipBackLocation) {
                                        DeployObjectiveSitingPolicy.ObjectiveTailEvaluation v279ObjectiveTail =
                                            DeployObjectiveSitingPolicy.evaluateObjectiveTail(
                                                new DeployObjectiveSitingPolicy.ObjectiveTailFacts(
                                                    action.getActionId(), true,
                                                    isObjLocation, true,
                                                    false, 0.0f));
                                        applyDeploySitingPolicy(action, v279ObjectiveTail.result());
                                        if (v279ObjectiveTail.postFlipProtected()) {
                                            logger.warn("V22.2 PROTECT: {} is flip-back protection location - bonus for deploying here", title);
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("Could not get power at {}: {}", title, e.getMessage());
                            }
                        }

                        // V67as, including nested V67br/V75/V67bj, became unreachable at the V136 cutover.
                        // The retired body remains in git history.
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse cardId: {}", cardId);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    // ═══════════════════════════════════════════════════════════
    // ═══ SECTION: FORCE-LOSS — Loss-Source Picker (reorg 2026-07-06) ═══
    // Owns: V153 two-tier zone order (protect characters when life force >= 4;
    // survival mode < 4) + bolt-ons V109 (senators -300), V175a (battle-interrupt
    // turn-4 gate), V178-loss (wielded-weapon zone rerank 600→150), V28-DTF
    // (Draw Their Fire force-pile protect), V21/V25 protections. Hub: V153 LIVE.
    // KIND mix + key magnitudes: ORDERING via zone bands; HAND FLOOR -700,
    // PRIORITY CARD -100, V21 additive veto bands on objective-critical cards.
    // Absorbed (V127, V101, V119, V29.8-zone): the old //-commented zone-scoring
    // blocks were DELETED 2026-07-12 batch 1.5 — revert path = git history.
    // V206: shared ForceLossFacts + ForceLossPolicy own the immutable snapshot and
    // exact route-specific contribution stream. This class only adapts stock choices.
    // Cross-refs: BATTLE-3 (forfeit side of the combined lose-or-forfeit prompt),
    // RESPONSE (pay-loss route). See resources/RANDO_REORG_PLAN_2026-07-02.md §3 +
    // Rando_Section_Manifest_2026-07-06.xlsx.
    // ═══════════════════════════════════════════════════════════
    /**
     * Choose force to lose - pick cards we want to lose least.
     */
    private List<EvaluatedAction> evaluateForceLoss(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        ForceLossFacts.DecisionFacts decisionFacts = ForceLossFacts.readDecision(
                gameState, playerId, context.getTurnNumber());
        String decisionId = context.getDecisionId();
        PolicyContributionLedger forceLossLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "force-loss-decision" : decisionId + "-force-loss");

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                    cardId,
                    ActionType.UNKNOWN,
                    50.0f,
                    "Lose force (card " + cardId + ")");

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        applyCaptureCriticalRetention(
                                context, action, card);
                        String title = card.getTitle();
                        if (title != null) {
                            action.setDisplayText("Lose " + title);
                        }
                        ForceLossFacts.CandidateFacts candidate =
                                ForceLossFacts.readCandidate(gameState, playerId, card);
                        forceLossLedger.register(ForceLossPolicy.score(
                                cardId,
                                ForceLossPolicy.Route.STANDALONE,
                                decisionFacts,
                                candidate,
                                forceLossObjectiveFlags(context, card, candidate,
                                        ForceLossPolicy.Route.STANDALONE)));
                        PolicyOperationAdapter.apply(action, forceLossLedger);
                    }
                } catch (NumberFormatException ignored) {
                    // Preserve the legacy neutral fallback for malformed ids.
                }
            }
            actions.add(action);
        }
        return actions;
    }

    private void applyCaptureCriticalRetention(
            DecisionContext context,
            EvaluatedAction action,
            PhysicalCard candidate) {
        if (context == null || action == null
                || candidate == null
                || action.getActionId() == null
                || action.getActionId().isBlank()) {
            return;
        }
        var analyzer = context.getObjectiveAnalyzer();
        CaptureObjectivePolicy.ObjectiveKind objective =
                CaptureObjectiveFacts.objectiveKind(analyzer);
        if (objective == null) {
            return;
        }
        CaptureObjectivePolicy.CriticalRole role =
                CaptureObjectiveFacts.preferredCriticalLossRole(
                        context.getGame(),
                        context.getPlayerId(),
                        analyzer,
                        candidate);
        if (role == null
                && CaptureObjectiveFacts
                    .wouldBreakStableBackIfRemoved(
                        context.getGame(),
                        context.getPlayerId(),
                        analyzer,
                        candidate)) {
            role = CaptureObjectivePolicy.CriticalRole
                    .CAPTURE_PIECE;
        }
        if (role == null) {
            return;
        }
        PolicyContributionLedger ledger =
                new PolicyContributionLedger(
                        "capture-critical-retention-"
                            + action.getActionId());
        ledger.register(
                CaptureObjectivePolicy.scoreCriticalRetention(
                    new CaptureObjectivePolicy.RetentionFacts(
                        action.getActionId(),
                        objective,
                        role,
                        true)));
        PolicyOperationAdapter.apply(action, ledger);
    }

    private ForceLossPolicy.ObjectiveFlags forceLossObjectiveFlags(
            DecisionContext context,
            PhysicalCard physicalCard,
            ForceLossFacts.CandidateFacts candidate,
            ForceLossPolicy.Route route) {
        var objectiveAnalyzer = context.getObjectiveAnalyzer();
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()) {
            return ForceLossPolicy.ObjectiveFlags.none();
        }

        String title = candidate.title();
        boolean myLord = false;
        boolean huntDown;
        boolean required;
        boolean pullable;
        var progressRole = !objectiveAnalyzer.isFlipped()
                && context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                        ? objectiveAnalyzer
                            .classifyPreFlipProgressCandidate(
                                context.getGame(),
                                context.getPlayerId(),
                                physicalCard)
                        : com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE;
        boolean preferredFirstOrderReignsRouteCard =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isPreferredFirstOrderReignsRouteForceLossCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        boolean firstOrderReignsRouteShip =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isFirstOrderReignsRouteShipCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        boolean requiredActor = !objectiveAnalyzer.isFlipped()
                && context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && (objectiveAnalyzer.matchesFlipGateActorRequirement(
                        context.getGame(),
                        context.getPlayerId(), physicalCard)
                    || progressRole
                        != com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole.NONE
                        && progressRole
                        != com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer
                            .ObjectiveProgressCandidateRole
                            .REQUIRED_ON_TABLE_CARD)
                && (!firstOrderReignsRouteShip
                    || preferredFirstOrderReignsRouteCard);
        boolean requiredOnTableCard =
                physicalCard != null
                && objectiveAnalyzer
                    .isRequiredCardForFlip(physicalCard);
        boolean preferredRequiredCard =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isPreferredRequiredCardForceLossCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        boolean preferredShieldRoutePackageCard =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isPreferredShieldRoutePackageForceLossCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        boolean preferredNabooDuelDuelist =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isPreferredNabooDuelForceLossCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        boolean preferredMassassiPackageCard =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isPreferredMassassiAttackRunPackageForceLossCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        boolean preferredCountedObjectiveLocation =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isPreferredCountedObjectiveLocationForceLossCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        boolean preferredCountedObjectivePresence =
                context.getGame() != null
                && context.getPlayerId() != null
                && physicalCard != null
                && objectiveAnalyzer
                    .isPreferredCountedObjectivePresenceForceLossCandidate(
                        context.getGame(),
                        context.getPlayerId(),
                        physicalCard);
        if (route == ForceLossPolicy.Route.STANDALONE) {
            myLord = objectiveAnalyzer.getObjectiveTitle() != null
                    && objectiveAnalyzer.isMyLord();
            required = preferredRequiredCard
                    || preferredShieldRoutePackageCard
                    || preferredFirstOrderReignsRouteCard
                    || preferredMassassiPackageCard
                    || preferredCountedObjectiveLocation
                    || preferredCountedObjectivePresence
                    || candidate.fromHand() && requiredActor;
            pullable = candidate.fromHand() && title != null && !required
                    && !requiredOnTableCard
                    && objectiveAnalyzer.isPullableCard(
                        physicalCard);
            huntDown = candidate.fromHand() && title != null
                    && objectiveAnalyzer.isHuntDownV();
        } else {
            huntDown = title != null && objectiveAnalyzer.isHuntDownV();
            required = preferredRequiredCard
                    || preferredShieldRoutePackageCard
                    || preferredFirstOrderReignsRouteCard
                    || preferredMassassiPackageCard
                    || preferredCountedObjectiveLocation
                    || preferredCountedObjectivePresence
                    || requiredActor;
            pullable = title != null && !required
                    && !requiredOnTableCard
                    && objectiveAnalyzer.isPullableCard(
                        physicalCard);
        }
        return new ForceLossPolicy.ObjectiveFlags(
                myLord,
                huntDown,
                required,
                pullable,
                preferredNabooDuelDuelist);
    }

    /**
     * Choose card to forfeit - smart forfeit selection.
     *
     * Priority order (highest first):
     * 1. Hit cards - MUST be forfeited anyway, might as well do it first
     * 2. Pilots on ships - forfeit pilot before ship (ship dying loses pilots too)
     * 3. Low forfeit value cards - satisfy damage efficiently
     * 4. Low power cards - keep high-power cards fighting
     *
     * Avoid:
     * - Ships with pilots aboard (forfeit pilots first!)
     * - High forfeit/power unique characters
     * - Cards with attrition immunity
     *
     * Ported from Python card_selection_evaluator.py _evaluate_forfeit()
     */
    private List<EvaluatedAction> evaluateForfeit(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String textLower = context.getDecisionText().toLowerCase(java.util.Locale.ROOT);
        boolean isOptional = textLower.contains("if desired");

        // V22.4 FIX: Get remaining damage directly from game state.
        // The decision text is always just "Choose a card from battle to forfeit (if desired)"
        // with NO damage numbers embedded, so text-parsing always returned 0 and optional
        // forfeits were always skipped. Query the battle state directly instead.
        int optionalDamageRemaining = 0;
        int optionalAttritionRemaining = 0;
        if (isOptional) {
            SwccgGame forfeitGame = context.getGame();
            String forfeitPlayerId = context.getPlayerId();
            if (forfeitGame != null && forfeitPlayerId != null) {
                try {
                    optionalDamageRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                        .getBattleDamageRemaining(forfeitGame, forfeitPlayerId);
                    optionalAttritionRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                        .getBattleAttritionRemaining(forfeitGame, forfeitPlayerId);
                } catch (Exception e) {
                    logger.debug("Could not read battle damage from game state: {}", e.getMessage());
                }
            }
            logger.info("V22.4 OPTIONAL FORFEIT (game state): isOptional={}, damageRemaining={}, attritionRemaining={}",
                isOptional, optionalDamageRemaining, optionalAttritionRemaining);
        }
        BattleForfeitFacts.DecisionFacts optionalForfeitDecision =
            new BattleForfeitFacts.DecisionFacts(
                optionalAttritionRemaining, optionalDamageRemaining,
                BattleForfeitFacts.CandidateSetFacts.empty());
        String optionalForfeitDecisionId = context.getDecisionId();
        PolicyContributionLedger optionalForfeitLedger = new PolicyContributionLedger(
            optionalForfeitDecisionId == null || optionalForfeitDecisionId.isBlank()
                ? "optional-battle-forfeit-decision"
                : optionalForfeitDecisionId + "-optional-battle-forfeit");
        BattleForfeitFacts.FlipGateFormationSelectionFacts
            flipGateFormationSelection =
                BattleForfeitFacts.readFlipGateFormationSelection(
                    context.getCardIds(), gameState, context.getGame(),
                    context.getPlayerId(), context.getObjectiveAnalyzer(),
                    isOptional, optionalAttritionRemaining);
        PolicyContributionLedger flipGateFormationLedger =
            new PolicyContributionLedger(
                optionalForfeitDecisionId == null
                        || optionalForfeitDecisionId.isBlank()
                    ? "battle-forfeit-flip-gate-formation"
                    : optionalForfeitDecisionId
                        + "-battle-forfeit-flip-gate-formation");
        int standaloneForfeitOptionCount = 0;
        if (gameState != null) {
            for (int index = 0;
                    index < context.getCardIds().size(); index++) {
                if (!isCardSelectable(context, index)) {
                    continue;
                }
                try {
                    if (gameState.findCardById(Integer.parseInt(
                            context.getCardIds().get(index))) != null) {
                        standaloneForfeitOptionCount++;
                    }
                } catch (NumberFormatException ignored) {
                    // Malformed ids cannot prove a legal alternative.
                }
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Forfeit card " + cardId
            );
            flipGateFormationLedger.register(
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                    cardId, flipGateFormationSelection.roleFor(cardId),
                    flipGateFormationSelection
                        .hasUnprotectedLegalAlternative()));
            PolicyOperationAdapter.apply(action, flipGateFormationLedger);
            PhysicalCard captureCriticalCandidate = null;
            if (gameState != null) {
                try {
                    captureCriticalCandidate =
                            gameState.findCardById(
                                Integer.parseInt(cardId));
                } catch (NumberFormatException ignored) {
                    // Preserve the legacy neutral fallback for malformed ids.
                }
            }
            applyCaptureCriticalRetention(
                    context, action, captureCriticalCandidate);
            if (captureCriticalCandidate != null
                    && captureCriticalCandidate.getBlueprint() != null) {
                CardCategory category = captureCriticalCandidate
                        .getBlueprint().getCardCategory();
                if (category == CardCategory.STARSHIP
                        || category == CardCategory.VEHICLE) {
                    int crewCount =
                            BattleForfeitFacts.countAboardCharacters(
                                    gameState, captureCriticalCandidate);
                    PolicyContributionLedger v48ForfeitLedger =
                            new PolicyContributionLedger(
                                    "standalone-battle-forfeit-v48-"
                                            + cardId);
                    v48ForfeitLedger.register(
                            BattleForfeitPolicy
                                .scoreStandaloneShipWithCrew(
                                    cardId,
                                    captureCriticalCandidate.getTitle(),
                                    crewCount,
                                    isOptional
                                        || standaloneForfeitOptionCount > 1));
                    PolicyOperationAdapter.apply(
                            action, v48ForfeitLedger);
                    if (crewCount > 0
                            && (isOptional
                                || standaloneForfeitOptionCount > 1)) {
                        logger.warn(
                                "V48 SHIP FORFEIT BLOCK: {} has {} crew; forfeit another card first",
                                captureCriticalCandidate.getTitle(),
                                crewCount);
                    }
                }
            }

            // V22.4: Optional forfeit handling — COMPLETELY REWORKED
            // Old bug: ALL optional forfeits were avoided (-150). This meant Rando would
            // NEVER voluntarily forfeit characters to satisfy battle damage, leading to
            // massive hand/reserve losses (Emperor Palpatine not forfeited, losing 16 cards instead)
            //
            // NEW LOGIC: If there's battle damage remaining, optional forfeits are GOOD!
            // A character with forfeit=6 satisfies 6 damage in 1 action vs 6 cards from reserve.
            // Only avoid optional forfeits when there's NO damage to satisfy.
            if (isOptional && optionalDamageRemaining <= 0) {
                // V29.13: No battle damage remaining — truly optional forfeit, MUST avoid!
                // Previous bug: VERY_BAD_DELTA (-150) + base (50) = -100, which exactly equals
                // BAD_ACTION_THRESHOLD (-100). The pass check is "< -100", so -100 didn't trigger
                // pass, and Rando forfeited characters immune to remaining attrition!
                // Fix: Use -500 to guarantee score falls well below threshold.
                BattleForfeitFacts.CandidateFacts candidate =
                    BattleForfeitFacts.readCandidate(
                        cardId, null, context.getGame(), context.getPlayerId(),
                        false, optionalAttritionRemaining, optionalDamageRemaining, false);
                BattleForfeitPolicy.Evaluation evaluation = BattleForfeitPolicy.evaluateOptional(
                    optionalForfeitDecision, candidate,
                    BattleForfeitFacts.ObjectiveFlags.none());
                optionalForfeitLedger.register(evaluation.beforeRoute());
                PolicyOperationAdapter.apply(action, optionalForfeitLedger);
                logger.warn("V29.13 SKIP FORFEIT: Optional with no damage — PASS! (dmg={}, attr={})",
                    optionalDamageRemaining, optionalAttritionRemaining);
                actions.add(action);
                continue;
            } else if (isOptional && optionalDamageRemaining > 0) {
                // V22.4: Battle damage still remaining! Forfeiting is MUCH better than losing from reserve!
                // This character's forfeit value will satisfy multiple points of damage
                if (gameState != null) {
                    try {
                        PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                        if (card != null) {
                            String fTitle = card.getTitle();
                            boolean requiredForFlip = context.getObjectiveAnalyzer() != null
                                && context.getObjectiveAnalyzer().isAnalyzed()
                                && context.getObjectiveAnalyzer()
                                    .isRequiredCardForFlip(card);
                            boolean pullable = context.getObjectiveAnalyzer() != null
                                && context.getObjectiveAnalyzer().isAnalyzed()
                                && !requiredForFlip
                                && context.getObjectiveAnalyzer()
                                    .isPullableCard(card);
                            BattleForfeitFacts.CandidateFacts candidate =
                                BattleForfeitFacts.readCandidate(
                                    cardId, card, context.getGame(), context.getPlayerId(),
                                    false, optionalAttritionRemaining, optionalDamageRemaining, false);
                            BattleForfeitPolicy.Evaluation evaluation =
                                BattleForfeitPolicy.evaluateOptional(
                                    optionalForfeitDecision, candidate,
                                    new BattleForfeitFacts.ObjectiveFlags(
                                        requiredForFlip, pullable));
                            optionalForfeitLedger.register(evaluation.beforeRoute());
                            PolicyOperationAdapter.apply(action, optionalForfeitLedger);
                            if (!evaluation.beforeRoute().operations().isEmpty()) {
                                logger.warn("V208 BATTLE-3 OPTIONAL FORFEIT: {} attr={} dmg={} operations={}",
                                    card.getTitle(), optionalAttritionRemaining,
                                    optionalDamageRemaining,
                                    evaluation.beforeRoute().operations().size());
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
                actions.add(action);
                continue;  // Skip normal scoring — optional forfeit has its own scoring above
            }

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        String title = card.getTitle();
                        if (title != null) {
                            action.setDisplayText("Forfeit " + title);
                        }


                        // =======================================================
                        // CRITICAL: Dead cards (persona already deployed) should
                        // be forfeited - they can never be played anyway!
                        // =======================================================
                        SwccgGame game = context.getGame();
                        String playerId = context.getPlayerId();
                        boolean deadCard = game != null && playerId != null
                            && AiCardHelper.isDeadCard(card, game, playerId);
                        if (deadCard) {
                            PolicyContributionLedger deadCardForfeitLedger =
                                new PolicyContributionLedger("standalone-battle-forfeit-dead-" + cardId);
                            deadCardForfeitLedger.register(
                                BattleForfeitPolicy.scoreStandalonePriority(
                                    BattleForfeitPolicy.StandaloneResidualFacts.priority(
                                        cardId, true, false)));
                            PolicyOperationAdapter.apply(action, deadCardForfeitLedger);
                            logger.info("☠️ {} is a DEAD CARD - prioritizing for forfeit", title);
                        }

                        // =======================================================
                        // Check if this is a pilot attached to a ship
                        // Pilots on ships should be forfeited BEFORE the ship!
                        // =======================================================
                        boolean pilotOnShip = false;
                        PhysicalCard attachedTo = card.getAttachedTo();
                        if (attachedTo != null) {
                            SwccgCardBlueprint attachedBlueprint = attachedTo.getBlueprint();
                            if (attachedBlueprint != null) {
                                CardCategory attachedCat = attachedBlueprint.getCardCategory();
                                if (attachedCat == CardCategory.STARSHIP || attachedCat == CardCategory.VEHICLE) {
                                    pilotOnShip = true;
                                    PolicyContributionLedger pilotForfeitLedger =
                                        new PolicyContributionLedger("standalone-battle-forfeit-pilot-" + cardId);
                                    pilotForfeitLedger.register(
                                        BattleForfeitPolicy.scoreStandalonePriority(
                                            BattleForfeitPolicy.StandaloneResidualFacts.priority(
                                                cardId, false, true)));
                                    PolicyOperationAdapter.apply(action, pilotForfeitLedger);
                                }
                            }
                        }

                        Float forfeit = null;
                        Float power = null;
                        boolean unique = false;
                        Float uniqueAbility = null;
                        Float uniquePower = null;
                        if (blueprint != null) {
                            // Forfeit value scoring - lower forfeit = better to forfeit (cheap loss).
                            // CRITICAL: hasForfeitAttribute() check first (weapons throw).
                            forfeit = blueprint.hasForfeitAttribute() ? blueprint.getForfeit() : null;

                            // Power scoring - bumped magnitudes (V139)
                            if (blueprint.hasPowerAttribute()) {
                                power = blueprint.getPower();
                            }

                            // V139: Protect unique high-value characters HARDER
                            unique = blueprint.getUniqueness() == Uniqueness.UNIQUE;
                            if (unique) {
                                uniqueAbility = blueprint.hasAbilityAttribute() ? blueprint.getAbility() : null;
                                uniquePower = blueprint.hasPowerAttribute() ? blueprint.getPower() : null;
                            }

                            // Characters with extra destiny draws are valuable
                            // TODO: Check for destiny draw bonuses when API available
                        }

                        // V21: OBJECTIVE-CRITICAL CARD PROTECTION (forfeit)
                        String fTitle = card.getTitle();
                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer fObjAnalyzer = context.getObjectiveAnalyzer();
                        boolean requiredForFlip = false;
                        boolean pullable = false;
                        if (fObjAnalyzer != null && fObjAnalyzer.isAnalyzed() && fTitle != null) {
                            if (fObjAnalyzer.isRequiredCardForFlip(card)) {
                                requiredForFlip = true;
                            } else if (fObjAnalyzer.isPullableCard(card)) {
                                pullable = true;
                            }
                        }
                        PolicyContributionLedger standaloneForfeitLedger =
                            new PolicyContributionLedger("standalone-battle-forfeit-" + cardId);
                        standaloneForfeitLedger.register(BattleForfeitPolicy.scoreStandaloneResidual(
                            new BattleForfeitPolicy.StandaloneResidualFacts(
                                cardId, deadCard, pilotOnShip,
                                forfeit, power, unique, uniqueAbility, uniquePower,
                                new BattleForfeitFacts.ObjectiveFlags(requiredForFlip, pullable))));
                        PolicyOperationAdapter.apply(action, standaloneForfeitLedger);
                        if (requiredForFlip) {
                            logger.warn("V21 HARD BAN: {} is REQUIRED for flip - never forfeit!", fTitle);
                        } else if (pullable) {
                            logger.warn("V21 HARD BAN: {} is objective pullable - never forfeit!", fTitle);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Handle combined "Force to lose OR forfeit" decisions.
     *
     * CRITICAL SWCCG RULES:
     * - ATTRITION can ONLY be satisfied by forfeiting cards (not Force loss)
     * - Battle damage can be satisfied by EITHER Force loss OR forfeiting
     *
     * Strategy:
     * - If attrition is remaining, MUST forfeit (prioritize hit cards)
     * - If only battle damage, prefer losing Force (saves cards)
     * - Exception: if we have hit cards, forfeit them first anyway
     *
     * Ported from Python card_selection_evaluator.py _evaluate_force_loss_or_forfeit()
     */
    private List<EvaluatedAction> evaluateForceLossOrForfeit(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String text = context.getDecisionText();
        String textLower = text.toLowerCase(java.util.Locale.ROOT);

        // Get attrition and damage remaining directly from game state.
        // Text-parsing is unreliable — the decision text does not embed damage counts.
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        int attritionRemaining = 0;
        int damageRemaining = 0;
        if (game != null && playerId != null) {
            try {
                attritionRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                    .getBattleAttritionRemaining(game, playerId);
                damageRemaining = (int) com.gempukku.swccgo.logic.timing.GuiUtils
                    .getBattleDamageRemaining(game, playerId);
            } catch (Exception e) {
                logger.debug("Could not read battle damage from game state: {}", e.getMessage());
            }
        }

        logger.info("🎯 Force loss OR forfeit (game state): attrition={}, damage={}", attritionRemaining, damageRemaining);

        ForceLossFacts.DecisionFacts forceLossDecision = ForceLossFacts.readCombinedDecision(
                gameState, playerId, context.getTurnNumber());
        String forceLossDecisionId = context.getDecisionId();
        PolicyContributionLedger forceLossLedger = new PolicyContributionLedger(
                forceLossDecisionId == null || forceLossDecisionId.isBlank()
                        ? "combined-force-loss-decision"
                        : forceLossDecisionId + "-combined-force-loss");
        PolicyContributionLedger battleForfeitBeforeLedger = new PolicyContributionLedger(
                forceLossDecisionId == null || forceLossDecisionId.isBlank()
                        ? "combined-battle-forfeit-before-decision"
                        : forceLossDecisionId + "-combined-battle-forfeit-before");
        PolicyContributionLedger battleForfeitAfterLedger = new PolicyContributionLedger(
                forceLossDecisionId == null || forceLossDecisionId.isBlank()
                        ? "combined-battle-forfeit-after-decision"
                        : forceLossDecisionId + "-combined-battle-forfeit-after");

        // Track if we have any hit cards or dead cards available for forfeit
        boolean hasHitCards = false;
        boolean hasDeadCards = false;
        String bestHitActionId = null;
        float bestHitForfeit = Float.MAX_VALUE;
        int combinedForfeitOptionCount = 0;
        boolean hasSelectableForceLossOption = false;
        // Note: game and playerId already declared above for battle state queries

        // First pass: identify hit cards, dead cards, and legal forfeit options.
        for (int index = 0; index < context.getCardIds().size(); index++) {
            String cardId = context.getCardIds().get(index);
            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        if (card.isHit()) {
                            hasHitCards = true;
                            SwccgCardBlueprint bp = card.getBlueprint();
                            // CRITICAL: Check hasForfeitAttribute() first - weapons throw exception!
                            float forfeit = bp != null && bp.hasForfeitAttribute() && bp.getForfeit() != null ? bp.getForfeit() : 0;
                            if (forfeit < bestHitForfeit) {
                                bestHitForfeit = forfeit;
                                bestHitActionId = cardId;
                            }
                        }
                        // Check for dead cards (persona already deployed)
                        if (game != null && playerId != null &&
                            AiCardHelper.isDeadCard(card, game, playerId)) {
                            hasDeadCards = true;
                        }
                        if (isCardSelectable(context, index)) {
                            if (ForceLossFacts.isForceLossZone(card)) {
                                hasSelectableForceLossOption = true;
                            } else {
                                combinedForfeitOptionCount++;
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }

        BattleForfeitFacts.CandidateSetFacts battleCandidateSet =
            new BattleForfeitFacts.CandidateSetFacts(
                hasHitCards, hasDeadCards,
                java.util.Optional.ofNullable(bestHitActionId), bestHitForfeit);
        BattleForfeitFacts.DecisionFacts battleForfeitDecision =
            new BattleForfeitFacts.DecisionFacts(
                attritionRemaining, damageRemaining, battleCandidateSet);
        BattleForfeitFacts.FlipGateFormationSelectionFacts
            flipGateFormationSelection =
                BattleForfeitFacts.readFlipGateFormationSelection(
                    context.getCardIds(), gameState, game, playerId,
                    context.getObjectiveAnalyzer(), false,
                    attritionRemaining);
        PolicyContributionLedger flipGateFormationLedger =
            new PolicyContributionLedger(
                forceLossDecisionId == null || forceLossDecisionId.isBlank()
                    ? "combined-battle-forfeit-flip-gate-formation"
                    : forceLossDecisionId
                        + "-combined-battle-forfeit-flip-gate-formation");

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Choose " + cardId
            );
            flipGateFormationLedger.register(
                BattleForfeitPolicy.scoreFlipGateFormationProtection(
                    cardId, flipGateFormationSelection.roleFor(cardId),
                    flipGateFormationSelection
                        .hasUnprotectedLegalAlternative()));
            PolicyOperationAdapter.apply(action, flipGateFormationLedger);
            PhysicalCard battleCandidate = null;
            if (gameState != null) {
                try {
                    battleCandidate = gameState.findCardById(Integer.parseInt(cardId));
                } catch (NumberFormatException e) { /* ignore */ }
            }
            applyCaptureCriticalRetention(
                    context, action, battleCandidate);
            boolean isForceLosSOption = ForceLossFacts.isForceLossZone(battleCandidate);
            BattleForfeitFacts.CandidateFacts battleForfeitCandidate =
                BattleForfeitFacts.readCandidate(
                    cardId, battleCandidate, game, playerId,
                    isForceLosSOption, attritionRemaining, damageRemaining, true);
            BattleForfeitPolicy.Evaluation battleForfeitEvaluation =
                BattleForfeitPolicy.evaluateCombined(
                    battleForfeitDecision, battleForfeitCandidate);
            battleForfeitBeforeLedger.register(battleForfeitEvaluation.beforeRoute());
            PolicyOperationAdapter.apply(action, battleForfeitBeforeLedger);

            if (battleForfeitEvaluation.adapterStep()
                    == BattleForfeitPolicy.AdapterStep.CONTINUE_CANDIDATE) {
                action.setDisplayText("Lose weapon "
                    + (battleCandidate != null && battleCandidate.getTitle() != null
                        ? battleCandidate.getTitle() : cardId));
                logger.warn("V154 WEAPON-LOSS: {} hostHit={} → +{}",
                    battleCandidate != null ? battleCandidate.getTitle() : cardId,
                    battleForfeitCandidate.attachedHostHit(),
                    battleForfeitCandidate.attachedHostHit() ? 2200.0f : 2000.0f);
                actions.add(action);
                continue;
            }

            if (battleForfeitDecision.smallPureDamage() && isForceLosSOption) {
                logger.info("V118 SMALL DAMAGE force-loss boost (+200) — damageRemaining={}", damageRemaining);
            } else if (battleForfeitDecision.smallPureDamage()
                    && battleForfeitCandidate.character()
                    && !battleForfeitCandidate.hit()) {
                logger.info("V118 SAVE CHARACTER (-500) — {} not-hit, damage={}",
                    battleCandidate != null ? battleCandidate.getTitle() : cardId,
                    damageRemaining);
            }

            if (isForceLosSOption) {
                // Force loss option — card from hand/reserve/force pile
                action.setDisplayText("Lose Force from pile");

                // V67be (Steve, 2026-05-09): V67y REMOVED from this combined prompt.
                //
                // Steve's clarification: "V67y was only meant for moments when force
                // is required to come from hand or reserves. In battle you still have
                // the option to forfeit from site. V67y outweighs a very important
                // logic [V22.3 forfeit-first]."
                //
                // V67y added +500 to pile-loss / -500 to hand-loss. That dominated
                // V22.3's -40/-80/-120 forfeit-first penalty, silently regressing
                // the original "forfeit before burning reserve" rule. Replay
                // jzhprmm64t32wz8g battles #1 & #2: Rando burned 4 reserve cards
                // before forfeiting Chiraneau anyway.
                //
                // FIX: V67y stays in the STANDALONE evaluateForceLoss method (V29.8
                // already there with the same zone-aware semantics) for non-battle
                // force-loss prompts. The combined battle prompt is governed by
                // V22.3 (forfeit-first) + V67bd (attrition forfeit bonus). Pile-vs-
                // hand within force-loss is handled by V25 character/ship hand
                // penalties further down — no zone bonus needed here.
                //
                // (No-op block: V67y deliberately not applied in this method.)

                if (gameState != null) {
                    try {
                        PhysicalCard lossCard =
                                gameState.findCardById(Integer.parseInt(cardId));
                        if (lossCard != null) {
                            ForceLossFacts.CandidateFacts candidate =
                                    ForceLossFacts.readCandidate(
                                            gameState, playerId, lossCard);
                            forceLossLedger.register(ForceLossPolicy.score(
                                    cardId,
                                    ForceLossPolicy.Route.COMBINED_BATTLE,
                                    forceLossDecision,
                                    candidate,
                                    forceLossObjectiveFlags(context, lossCard, candidate,
                                            ForceLossPolicy.Route.COMBINED_BATTLE)));
                            PolicyOperationAdapter.apply(action, forceLossLedger);
                        }
                    } catch (NumberFormatException ignored) {
                        // Preserve the legacy neutral fallback for malformed ids.
                    }
                }

                battleForfeitAfterLedger.register(battleForfeitEvaluation.afterRoute());
                PolicyOperationAdapter.apply(action, battleForfeitAfterLedger);
            } else {
                // Forfeit card option
                if (battleCandidate != null) {
                    String title = battleCandidate.getTitle();
                    action.setDisplayText("Forfeit " + (title != null ? title : cardId));
                    SwccgCardBlueprint blueprint =
                            battleCandidate.getBlueprint();
                    if (blueprint != null
                            && (blueprint.getCardCategory()
                                    == CardCategory.STARSHIP
                                || blueprint.getCardCategory()
                                    == CardCategory.VEHICLE)) {
                        int crewCount =
                                BattleForfeitFacts.countAboardCharacters(
                                        gameState, battleCandidate);
                        PolicyContributionLedger v48Ledger =
                                new PolicyContributionLedger(
                                        "combined-battle-forfeit-v48-"
                                                + cardId);
                        v48Ledger.register(
                                BattleForfeitPolicy
                                    .scoreCombinedShipWithCrew(
                                        cardId, title, crewCount,
                                        combinedForfeitOptionCount > 1
                                            || (attritionRemaining <= 0
                                                && hasSelectableForceLossOption)));
                        PolicyOperationAdapter.apply(action, v48Ledger);
                        if (crewCount > 0
                                && (combinedForfeitOptionCount > 1
                                    || (attritionRemaining <= 0
                                        && hasSelectableForceLossOption))) {
                            logger.warn(
                                    "V48 COMBINED SHIP FORFEIT BLOCK: {} has {} crew; forfeit another card first",
                                    title, crewCount);
                        }
                    }
                }
                battleForfeitAfterLedger.register(battleForfeitEvaluation.afterRoute());
                PolicyOperationAdapter.apply(action, battleForfeitAfterLedger);
                if (!battleForfeitEvaluation.afterRoute().operations().isEmpty()) {
                    logger.warn("V208 BATTLE-3 FORFEIT: {} attr={} dmg={} operations={}",
                        battleCandidate != null ? battleCandidate.getTitle() : cardId,
                        attritionRemaining, damageRemaining,
                        battleForfeitEvaluation.afterRoute().operations().size());
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Extract a number following a pattern in text.
     * E.g., "attrition remaining: 5" -> returns 5
     */
    private int extractNumberAfter(String text, String pattern) {
        int idx = text.indexOf(pattern);
        if (idx >= 0) {
            String afterPattern = text.substring(idx + pattern.length()).trim();
            // Extract first number
            StringBuilder num = new StringBuilder();
            for (char c : afterPattern.toCharArray()) {
                if (Character.isDigit(c)) {
                    num.append(c);
                } else if (num.length() > 0) {
                    break;
                }
            }
            if (num.length() > 0) {
                try {
                    return Integer.parseInt(num.toString());
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        return 0;
    }

    /**
     * Choose a pilot - pick best pilot by ability.
     * Enhanced with deploy cost consideration and matching pilot detection.
     * Ported from Python deploy_evaluator.py _evaluate_simultaneous_pilot_selection
     */
    private List<EvaluatedAction> evaluatePilotSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        // Detect if this pilot selection is for Alert My Star Destroyer (AMSD).
        // AMSD deploys a Star Destroyer — only Imperial/First Order pilots belong
        // on capital ships. Non-Imperial pilots like Jango Fett have matching ships
        // that are NOT Star Destroyers (Slave I, etc.), so they will always fail the
        // reserve deck search and waste the action entirely.
        String decisionText = context.getDecisionText() != null
            ? context.getDecisionText().toLowerCase(java.util.Locale.ROOT) : "";
        // V22.7: Broadened AMSD detection. GEMP may present pilot selection with text
        // like "Choose a unique pilot character" without mentioning "star destroyer".
        // If we're in Deploy phase choosing a unique pilot, it's likely AMSD.
        // V24.12: Also detect AMSD by checking if the card is actually on the table,
        // because the decision text for "Choose card from hand" doesn't mention AMSD at all.
        boolean isAmsdPilotChoice = decisionText.contains("alert my star destroyer")
            || decisionText.contains("star destroyer")
            || decisionText.contains("matching starship")
            || decisionText.contains("matching star destroyer")
            || (context.getPhase() == Phase.DEPLOY && decisionText.contains("unique")
                && decisionText.contains("pilot"));

        // V24.12: AMSD-on-table detection — if AMSD is deployed and we're choosing
        // characters during deploy phase, this IS an AMSD pilot pick even if the
        // decision text is generic ("Choose card from hand, or click 'Done' to cancel").
        if (!isAmsdPilotChoice && context.getPhase() == Phase.DEPLOY) {
            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle pilotOracle = context.getDeckOracle();
            if (pilotOracle != null && pilotOracle.isAnalyzed()) {
                boolean amsdDeployed = pilotOracle.isCardInPlay("Alert My Star Destroyer")
                    || pilotOracle.isCardInPlay("Alert My Star Destroyer!")
                    || pilotOracle.isCardInPlay("Alert My Star Destroyer! (V)");
                if (amsdDeployed) {
                    // Verify at least one choice is a character (not a location/effect)
                    GameState pilotGs = context.getGameState();
                    if (pilotGs != null && context.getCardIds() != null) {
                        for (String cid : context.getCardIds()) {
                            try {
                                PhysicalCard pc = pilotGs.findCardById(Integer.parseInt(cid));
                                if (pc != null && pc.getBlueprint() != null &&
                                    pc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                    isAmsdPilotChoice = true;
                                    logger.warn("V24.12 AMSD DETECTED: AMSD on table + deploy phase + character choices — forcing AMSD pilot mode!");
                                    break;
                                }
                            } catch (Exception e) { /* skip */ }
                        }
                    }
                }
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Select pilot " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        String title = card.getTitle();
                        if (blueprint != null) {
                            action.setDisplayText("Select pilot " + (title != null ? title : cardId));

                            PullSelectionCandidateFacts.PilotAmsdState amsdState =
                                    PullSelectionCandidateFacts.PilotAmsdState.NOT_AMSD;
                            if (isAmsdPilotChoice) {
                                String pilotLower = title != null
                                        ? title.toLowerCase(Locale.ROOT) : "";
                                if (!pilotLower.contains("piett")) {
                                    amsdState = PullSelectionCandidateFacts.PilotAmsdState.NON_PIETT;
                                } else {
                                    var oracle = context.getDeckOracle();
                                    if (oracle == null || !oracle.isAnalyzed()) {
                                        amsdState = PullSelectionCandidateFacts.PilotAmsdState.PIETT_ORACLE_UNAVAILABLE;
                                    } else if (oracle.isCardInReserve("Executor")
                                            || oracle.isCardInReserve("Flagship Executor")) {
                                        amsdState = PullSelectionCandidateFacts.PilotAmsdState.PIETT_EXECUTOR_PRESENT;
                                    } else {
                                        amsdState = PullSelectionCandidateFacts.PilotAmsdState.PIETT_EXECUTOR_MISSING;
                                    }
                                }
                            }

                            PullSelectionCandidatePolicy.Evaluation amsdEvaluation =
                                    PullSelectionCandidatePolicy.evaluateAmsdPilot(
                                            new PullSelectionCandidateFacts.AmsdPilot(
                                                    action.getActionId(), title, amsdState));
                            if (amsdEvaluation.resetToAmsdBlockScore()) {
                                action.setScore(PullSelectionCandidatePolicy.AMSD_BLOCK_SCORE);
                            }
                            applyPullSelectionPolicy(action, amsdEvaluation.result());
                            if (amsdState == PullSelectionCandidateFacts.PilotAmsdState.NON_PIETT) {
                                logger.warn("V24.10 AMSD HARD BLOCK: {} is NOT Piett — only Piett + Executor for AMSD!", title);
                            } else if (amsdState == PullSelectionCandidateFacts.PilotAmsdState.PIETT_EXECUTOR_MISSING) {
                                logger.warn("V24.10 AMSD: Piett but Executor not in reserve — HARD BLOCK");
                            } else if (amsdState == PullSelectionCandidateFacts.PilotAmsdState.PIETT_EXECUTOR_PRESENT) {
                                logger.warn("V24.10 AMSD: Piett + Executor in reserve — APPROVED (+300)");
                            } else if (amsdState == PullSelectionCandidateFacts.PilotAmsdState.PIETT_ORACLE_UNAVAILABLE) {
                                logger.warn("V24.10 AMSD: Piett selected, oracle unavailable — allowing (+200)");
                            }
                            if (amsdEvaluation.adapterStep()
                                    == PullSelectionCandidatePolicy.AdapterStep.CONTINUE_CANDIDATE) {
                                actions.add(action);
                                continue;
                            }

                            Float pilotAbility = null;
                            Float pilotPower = null;
                            Float pilotDeployCost = null;

                            // Adapter retains the exact blueprint read order.
                            if (blueprint.hasAbilityAttribute()) {
                                pilotAbility = blueprint.getAbility();
                            }

                            if (blueprint.hasPowerAttribute()) {
                                pilotPower = blueprint.getPower();
                            }

                            // V43: Wrap in try-catch — some cards (Interrupts, Effects like
                            // "Hidden Weapons") don't support getDeployCost() and throw
                            // UnsupportedOperationException, crashing the cleanup thread.
                            try {
                                pilotDeployCost = blueprint.getDeployCost();
                            } catch (UnsupportedOperationException e) {
                                // Card type doesn't support deployCost — skip
                            }

                            applyDeployPilotPolicy(action,
                                DeployPilotShipPolicy.evaluatePilotCandidate(
                                    new DeployPilotShipPolicy.PilotCandidateFacts(
                                        action.getActionId(), pilotAbility,
                                        pilotPower, pilotDeployCost)));
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Simultaneous pilot selection - when deploying a ship and choosing which pilot to put aboard.
     * The card_ids are pilot cards in hand, NOT locations.
     * Ported from Python deploy_evaluator.py _evaluate_simultaneous_pilot_selection lines 1193-1273
     */
    private List<EvaluatedAction> evaluateSimultaneousPilotSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String decisionText = context.getDecisionText();

        // Extract the ship name from decision text
        // Format: "Choose a pilot from hand to simultaneously deploy aboard •Ship Name"
        String shipName = extractShipNameFromText(decisionText);
        logger.info("🚀 Simultaneous pilot selection for {}", shipName != null ? shipName : "unknown ship");

        // Detect if the ship being piloted is a Star Destroyer (capital ship).
        // Only Imperial/First Order characters should pilot Star Destroyers.
        boolean isStarDestroyerDeploy = decisionText != null &&
            decisionText.toLowerCase(java.util.Locale.ROOT).contains("star destroyer");

        // Check deploy plan for a planned pilot for this ship
        String plannedPilotBlueprintId = null;
        Integer reservedEopGarrisonPermanentId = null;
        Integer reservedEopGarrisonCurrentId = null;
        DeployPhasePlanner planner = context.getDeployPhasePlanner();
        if (planner != null) {
            DeploymentPlan plan = planner.getCurrentPlan();
            if (plan != null) {
                for (DeploymentInstruction instruction : plan.getInstructions()) {
                    // Check if this instruction is for a pilot boarding a ship
                    String aboardShipName = instruction.getAboardShipName();
                    if (aboardShipName != null && shipName != null &&
                        aboardShipName.toLowerCase().contains(shipName.toLowerCase())) {
                        plannedPilotBlueprintId = instruction.getCardBlueprintId();
                        logger.info("   📋 Plan says pilot: {} (blueprint={})",
                            instruction.getCardName(), plannedPilotBlueprintId);
                        break;
                    }
                    if (EndorOperationsTacticalPolicy
                            .isBunkerGarrisonPlan(plan.getReason())
                            && "endor: bunker".equalsIgnoreCase(
                                instruction.getTargetLocationName())) {
                        reservedEopGarrisonPermanentId =
                                instruction.getCardPermanentCardId();
                        reservedEopGarrisonCurrentId =
                                instruction.getCardCurrentCardId();
                    }
                }
            }
        }

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Deploy pilot (card " + cardId + ")"
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        String title = card.getTitle();
                        String blueprintId = card.getBlueprintId(true);

                        if (blueprint != null) {
                            action.setDisplayText("Deploy pilot " + (title != null ? title : cardId));

                            boolean imperialPilot = false;
                            boolean firstOrderPilot = false;
                            if (isStarDestroyerDeploy) {
                                imperialPilot = blueprint.hasIcon(Icon.IMPERIAL);
                                firstOrderPilot = blueprint.hasIcon(Icon.FIRST_ORDER);
                            }
                            DeployPilotShipPolicy.Evaluation guardEvaluation =
                                DeployPilotShipPolicy.evaluateSimultaneousPilotGuard(
                                    new DeployPilotShipPolicy.SimultaneousPilotGuardFacts(
                                        action.getActionId(), isStarDestroyerDeploy,
                                        imperialPilot, firstOrderPilot));
                            if (guardEvaluation.resetScore() != null) {
                                action.setScore(guardEvaluation.resetScore());
                            }
                            applyDeployPilotPolicy(action, guardEvaluation.result());
                            if (guardEvaluation.adapterStep()
                                    == DeployPilotShipPolicy.AdapterStep.CONTINUE_CANDIDATE) {
                                logger.warn("🚫 SD GUARD: Blocking {} for Star Destroyer — not Imperial or FO", title);
                                actions.add(action);
                                continue;
                            }

                            boolean plannedPilot = plannedPilotBlueprintId != null
                                && blueprintId != null
                                && blueprintId.equals(plannedPilotBlueprintId);
                            boolean reservedEopBunkerGarrison =
                                reservedEopGarrisonPermanentId != null
                                && reservedEopGarrisonCurrentId != null
                                && reservedEopGarrisonPermanentId
                                    == card.getPermanentCardId()
                                && reservedEopGarrisonCurrentId
                                    == card.getCardId();
                            Float pilotDeployCost = null;
                            Float pilotAbility = null;
                            boolean matchingPilot = false;

                            if (!plannedPilot) {
                                // V43: preserve the fail-open deploy-cost read.
                                try {
                                    pilotDeployCost = blueprint.getDeployCost();
                                } catch (UnsupportedOperationException e) {
                                    // Card type doesn't support deployCost — skip
                                }

                                if (blueprint.hasAbilityAttribute()) {
                                    pilotAbility = blueprint.getAbility();
                                }

                                if (title != null && shipName != null) {
                                    String titleLower = title.toLowerCase();
                                    String shipNameLower = shipName.toLowerCase().replace("•", "").trim();
                                    matchingPilot = titleLower.contains(shipNameLower)
                                        || shipNameLower.contains(titleLower.replace(" ", ""));
                                }
                            }

                            applyDeployPilotPolicy(action,
                                DeployPilotShipPolicy.evaluateSimultaneousPilotChoice(
                                    new DeployPilotShipPolicy.SimultaneousPilotChoiceFacts(
                                        action.getActionId(), shipName, plannedPilot,
                                        pilotDeployCost, pilotAbility, matchingPilot,
                                        reservedEopBunkerGarrison)));
                            if (plannedPilot) {
                                logger.info("   ✅ {} is the PLANNED pilot (+200)", title);
                            } else if (matchingPilot) {
                                logger.info("   🎯 {} appears to be matching pilot for {}", title, shipName);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse cardId: {}", cardId);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Extract ship name from simultaneous deploy decision text.
     * Format: "...simultaneously deploy aboard •Ship Name" or "...aboard Ship Name"
     */
    private String extractShipNameFromText(String text) {
        if (text == null) return null;

        // Look for "aboard" followed by ship name
        int aboardIdx = text.toLowerCase().indexOf("aboard");
        if (aboardIdx >= 0) {
            String afterAboard = text.substring(aboardIdx + 6).trim();
            // Remove HTML tags if present
            afterAboard = afterAboard.replaceAll("<[^>]+>", " ").trim();
            // Take the first few words (ship names are usually 2-4 words)
            String[] words = afterAboard.split("\\s+");
            StringBuilder shipName = new StringBuilder();
            for (int i = 0; i < Math.min(words.length, 5); i++) {
                if (words[i].isEmpty()) continue;
                if (shipName.length() > 0) shipName.append(" ");
                shipName.append(words[i]);
            }
            return shipName.toString().trim();
        }
        return null;
    }

    /**
     * Evaluate move destination selection.
     * Ported from Python card_selection_evaluator.py _evaluate_move_destination
     *
     * Prefer:
     * - Locations with opponent icons (force drain potential!)
     * - Locations where we have power advantage
     * - Locations with our icons (force generation)
     * Avoid:
     * - Locations where enemy is much stronger
     * - Locations with fewer total icons than alternatives
     */
    /**
     * V166 (Steve, 2026-06): bonus-aware net force-drain balance = opponent's total drain
     * minus ours, across all locations where each side has presence. Uses getForceDrainAmount
     * so weapon/lightsaber/objective/Effect drain bonuses are counted (raw icon counts miss
     * them — the gap in the old icon-based calculateForceDrainGap). Positive = opponent is
     * out-draining us.
     */
    /**
     * V169 (Steve, 2026-06): how badly are OUR characters outpowered at this location?
     * Returns (opponent power - our power) when we have non-undercover presence there and
     * the opponent out-powers us; else 0. >0 = our characters are ENDANGERED — the opponent
     * can battle and beat them next turn (Asajj at Guest Quarters, Tyranus on Hoth).
     * Undercover spies don't count as presence to protect (they're safe undercover).
     */
    private static float v169OppPowerExcessAt(SwccgGame game, GameState gs, String playerId, PhysicalCard location) {
        try {
            String opp = gs.getOpponent(playerId);
            if (opp == null) return 0f;
            boolean weHere = false;
            for (PhysicalCard c : gs.getCardsAtLocation(location)) {
                if (c != null && playerId.equals(c.getOwner()) && !c.isUndercover()) { weHere = true; break; }
            }
            if (!weHere) return 0f;
            float our = game.getModifiersQuerying().getTotalPowerAtLocation(gs, location, playerId, false, false);
            float their = game.getModifiersQuerying().getTotalPowerAtLocation(gs, location, opp, false, false);
            return their > our ? (their - our) : 0f;
        } catch (Exception e) { return 0f; }
    }

    /**
     * V173 (Steve, 2026-06): whole-hand wave projection for the V172 winnability gates.
     * "Does the new logic account for the whole hand with power and weapons weights vs
     * force cost?" — now it does. Projects the ADDITIONAL power (beyond the card being
     * deployed) the bot could land this phase: every other hand character, taken in
     * descending power order, that fits the remaining force budget (force pile minus the
     * deploying card's printed cost, minus each buddy's printed cost as it's taken), plus
     * weapon weights for affordable character weapons in hand (lightsaber +5, other +3 —
     * the BattleEvaluator V29.7 precedent — max 2 counted, ~1 force each).
     * Estimates use PRINTED costs/power (no location cost modifiers) — a deliberate
     * approximation; the gates compare against live getTotalPowerAtLocation.
     */
    /**
     * V174 (Steve, 2026-06): the budget now RESERVES force before filling the wave.
     * Steve: "We need to account for saving force for maintenance cards on table / in
     * hand to deploy with the army and any interrupts that would be useful in battle."
     * Reserved off the top: (a) upkeep for our MAINTENANCE-icon cards already on table,
     * (b) the deploying card's own upkeep if IT is a maintenance card, (c) 1-2 force for
     * battle interrupts in hand (Steve's standing force-management rule). Maintenance
     * BUDDIES joining the wave consume deploy cost + upkeep from the budget.
     * T2 COMMIT-1 (2026-07-06, audit force-economy-1/-5, ruling H2): upkeep basis is now
     * the ENGINE's card-specific maintain cost (MaintenanceFacts, e.g. Lando 1 not 5) —
     * the old "maintenance cost = deploy cost, the V22.3/V59 rule" claim was refuted and
     * the buddy "double" (2x deploy cost) spend over-charged the wave; the table scan is
     * also Zone.isInPlay()-gated (getAllPermanentCards returns reserve-deck cards too).
     * Returns {wavePower, buddiesTaken, reservedForce}.
     */
    /** V177 helper (2026-07-10): estimate the opponent's WEAPON power at a location — the raw power
     *  totals are blind to weapons/hits, which is how Rando kept walking Jedi into armed stacks
     *  (Rey replay rbujmoc90br3uu4c). Same heuristic as BattleEvaluator V29.7: lightsaber +5,
     *  other weapon +3; counts ATTACHED WEAPON cards and PERMANENT weapons (game text). */
    private static float v177OppWeaponBonus(GameState gs, PhysicalCard location, String oppId) {
        float bonus = 0f;
        if (gs == null || location == null || oppId == null) return 0f;
        try {
            for (PhysicalCard c : gs.getCardsAtLocation(location)) {
                if (c == null || c.getBlueprint() == null) continue;
                if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                if (!oppId.equals(c.getOwner())) continue;
                java.util.List<PhysicalCard> atts = gs.getAttachedCards(c);
                if (atts != null) {
                    for (PhysicalCard att : atts) {
                        if (att == null || att.getBlueprint() == null) continue;
                        if (att.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                            String wt = att.getTitle() != null ? att.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            bonus += wt.contains("lightsaber") ? 5.0f : 3.0f;
                        }
                    }
                }
                String gt = c.getBlueprint().getGameText();
                if (gt != null && gt.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                    String ct = c.getTitle() != null ? c.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                    bonus += ct.contains("lightsaber") ? 5.0f : 3.0f;
                }
            }
        } catch (Exception e) { /* fail-open: 0 bonus */ }
        return bonus;
    }

    private static float[] v173WaveProjection(GameState gs, String playerId, String deployingBpId) {
        try {
            float thisCost = 0f;
            // T2 COMMIT-1 (2026-07-06): track the deploying card's ENGINE maintain cost
            // (was: boolean thisIsMaint reserving its full deploy cost).
            float thisMaintCost = 0f;
            boolean skippedSelf = false;
            java.util.List<float[]> buddies = new java.util.ArrayList<>(); // {power, cost, maintainCost}
            int sabers = 0, otherWeapons = 0, interrupts = 0;
            for (PhysicalCard h : gs.getHand(playerId)) {
                if (h == null || h.getBlueprint() == null) continue;
                SwccgCardBlueprint bp = h.getBlueprint();
                if (bp.getCardCategory() == CardCategory.CHARACTER) {
                    Float p = bp.hasPowerAttribute() ? bp.getPower() : null;
                    Float c = bp.getDeployCost();
                    float pv = p != null ? p : 0f, cv = c != null ? c : 0f;
                    // T2 COMMIT-1 (2026-07-06): engine maintain cost, not deploy cost
                    float maintCost = com.gempukku.swccgo.ai.models.common.strategy
                        .MaintenanceFacts.maintainCost(bp);
                    if (!skippedSelf && deployingBpId != null
                            && deployingBpId.equals(h.getBlueprintId(true))) {
                        thisCost = cv;
                        thisMaintCost = maintCost;
                        skippedSelf = true; // the deploying card is not its own buddy
                    } else {
                        buddies.add(new float[]{pv, cv, maintCost});
                    }
                } else if (bp.getCardCategory() == CardCategory.WEAPON) {
                    String wt = h.getTitle() != null
                        ? h.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                    if (wt.contains("lightsaber")) sabers++; else otherWeapons++;
                } else if (bp.getCardCategory() == CardCategory.INTERRUPT) {
                    interrupts++;
                }
            }
            // Upkeep for maintenance cards ALREADY on table (they die without it).
            float tableMaint = 0f;
            for (PhysicalCard t : gs.getAllPermanentCards()) {
                if (t == null) continue;
                // T2 COMMIT-1 (2026-07-06, audit force-economy-5): in-play gate —
                // getAllPermanentCards returns reserve-deck cards too; without this a
                // maintenance card still in Reserve Deck phantom-taxed the wave budget.
                com.gempukku.swccgo.common.Zone tZone = t.getZone();
                if (tZone == null || !tZone.isInPlay()) continue;
                if (playerId.equals(t.getOwner()) && t.getBlueprint() != null
                        && t.getBlueprint().hasIcon(Icon.MAINTENANCE)) {
                    tableMaint += com.gempukku.swccgo.ai.models.common.strategy
                        .MaintenanceFacts.maintainCost(t.getBlueprint());
                }
            }
            float interruptReserve = interrupts >= 2 ? 2f : (interrupts >= 1 ? 1f : 0f);
            // V176 (Steve, 2026-06): +1 battle-initiation fee. Turn-5 vs Steve: the wave
            // deployed onto solo Yoda and then could NOT battle him — the deploys spent
            // the last force and battle initiation costs 1. A wave that exists to fight
            // must keep the fee to start the fight.
            float reserved = tableMaint + interruptReserve + thisMaintCost + 1f;
            // V177 (Steve, 2026-06): RESERVE CAP — never let upkeep reserves starve the wave
            // to zero. Replay aab2jiaa5sca (Luke vs Kylo): force=10 but reserved=12 (full table
            // maintenance + interrupt + fee), so budget=0, wave=0, buddies=0 — Young Skywalker
            // projected solo (6) vs Kylo (10), V172 gated the contest, and Rando left Luke +
            // Bionic Hand + 3PO + saber in hand instead of overpowering Kylo. Maintenance only
            // bites at end of turn and is already handled at deploy-score time (V59/V64); double-
            // counting it here is what locked out the winnable attack. Cap the reserve so the
            // projection always keeps >=3 force of budget to assemble the strike group when force
            // allows — full reserves still hold when there is genuine surplus.
            float v177ForcePile = gs.getForcePileSize(playerId);
            reserved = Math.min(reserved, Math.max(0f, v177ForcePile - thisCost - 3f));
            float budget = Math.max(0f, v177ForcePile - thisCost - reserved);
            buddies.sort((a, b) -> Float.compare(b[0], a[0])); // strongest first
            float addPower = 0f;
            int taken = 0;
            for (float[] ch : buddies) {
                // a maintenance buddy must bring its own upkeep: deploy cost + upkeep
                // T2 COMMIT-1 (2026-07-06): upkeep = engine maintain cost (ch[2]), fixing
                // the double-spend that charged 2x deploy cost for maintenance buddies.
                float spend = ch[1] + ch[2];
                if (spend <= budget) { addPower += ch[0]; budget -= spend; taken++; }
                // unaffordable big hitter: skip and try the next (cheaper) character
            }
            int weaponsCounted = 0;
            while (weaponsCounted < 2 && budget >= 1f && (sabers > 0 || otherWeapons > 0)) {
                if (sabers > 0) { addPower += 5f; sabers--; }
                else { addPower += 3f; otherWeapons--; }
                budget -= 1f; weaponsCounted++;
            }
            return new float[]{addPower, taken, reserved};
        } catch (Exception e) { return new float[]{0f, 0f, 0f}; }
    }

    // Hoth repair #2 (2026-07-27): package-visible so ActionTextEvaluator can
    // feed the corrected battleOrderLive gate without duplicating drain math.
    static int computeNetDrainBalance(SwccgGame game, GameState gs, String playerId) {
        String oppId = gs.getOpponent(playerId);
        if (oppId == null) return 0;
        int oppTotal = 0, ourTotal = 0;
        for (PhysicalCard loc : gs.getLocationsInOrder()) {
            if (loc == null) continue;
            boolean weHere = false, oppHere = false;
            for (PhysicalCard c : gs.getCardsAtLocation(loc)) {
                if (c == null) continue;
                if (playerId.equals(c.getOwner())) weHere = true;
                else if (oppId.equals(c.getOwner())) oppHere = true;
            }
            try {
                if (oppHere) oppTotal += (int) game.getModifiersQuerying().getForceDrainAmount(gs, loc, oppId);
                if (weHere) ourTotal += (int) game.getModifiersQuerying().getForceDrainAmount(gs, loc, playerId);
            } catch (Exception ignore) { /* skip this location */ }
        }
        return oppTotal - ourTotal;
    }

    private boolean isHuntDownVaderRecallTargetDecision(
            DecisionContext context) {
        if (context == null || context.getDecisionText() == null
                || !"choose vader to take into hand".equalsIgnoreCase(
                    context.getDecisionText().trim())
                || context.getMin() != 1 || context.getMax() != 1
                || context.getGameState() == null
                || context.getObjectiveAnalyzer() == null) {
            return false;
        }
        try {
            var actionState =
                    context.getGameState().getTopGameTextActionState();
            var liveAction = actionState != null
                    ? actionState.getGameTextAction() : null;
            PhysicalCard source = liveAction != null
                    ? liveAction.getActionSource() : null;
            String sourceId = source != null
                    ? source.getBlueprintId(true) : null;
            String liveText = liveAction != null
                    ? liveAction.getText() : null;
            var objective = context.getObjectiveAnalyzer();
            return "take vader into hand".equalsIgnoreCase(
                        liveText != null ? liveText.trim() : "")
                    && ("7_297".equals(sourceId)
                        && objective.isClassicHuntDownObjective()
                        || ("213_31".equals(sourceId)
                            || "213_31_BACK".equals(sourceId))
                        && objective.isVirtualHuntDownObjective());
        } catch (Exception e) {
            return false;
        }
    }

    private List<EvaluatedAction> evaluateHuntDownVaderRecallTarget(
            DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<com.gempukku.swccgo.ai.models.common.strategy
                .ObjectiveAnalyzer.HuntDownRecallTargetAssessment>
                assessments = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        var objective = context.getObjectiveAnalyzer();
        boolean virtual = objective.isVirtualHuntDownObjective();
        boolean hasSafeOfferedTarget = false;

        for (int i = 0; i < context.getCardIds().size(); i++) {
            String cardId = context.getCardIds().get(i);
            com.gempukku.swccgo.ai.models.common.strategy
                .ObjectiveAnalyzer.HuntDownRecallTargetAssessment
                assessment = null;
            try {
                PhysicalCard candidate = gameState.findCardById(
                        Integer.parseInt(cardId));
                assessment = virtual
                        ? objective
                            .assessVirtualHuntDownVaderRecallTarget(
                                game, playerId, candidate)
                        : objective
                            .assessClassicHuntDownVaderRecallTarget(
                                game, playerId, candidate);
            } catch (Exception ignored) {
            }
            assessments.add(assessment);
            hasSafeOfferedTarget |= isCardSelectable(context, i)
                    && assessment != null
                    && assessment.applies()
                    && assessment.safeTarget();
        }

        for (int i = 0; i < context.getCardIds().size(); i++) {
            if (!isCardSelectable(context, i)) continue;
            String cardId = context.getCardIds().get(i);
            EvaluatedAction action = new EvaluatedAction(
                    cardId, ActionType.MOVE, 0.0f,
                    "Choose Vader recall target");
            var assessment = assessments.get(i);
            if (assessment != null && assessment.applies()) {
                if (!assessment.safeTarget()
                        && hasSafeOfferedTarget) {
                    action.hardVeto(
                        "OBJECTIVE.HUNT_DOWN.RECALL_TARGET_HOLD: preserve the required Vader");
                } else if (assessment.preservesRequiredVader()) {
                    action.addReasoning(
                        "OBJECTIVE.HUNT_DOWN.RECALL_TARGET_SAFE: another required Vader remains",
                        1000.0f);
                } else if (assessment.remoteBlockerChase()) {
                    action.addReasoning(
                        "OBJECTIVE.HUNT_DOWN.RECALL_TARGET_CHASE: redeploy this Vader toward the remote blocker",
                        600.0f);
                }
            }
            actions.add(action);
        }
        return actions;
    }

    private boolean isCaptureVirtualHutMoveDecision(
            DecisionContext context) {
        if (context == null || context.getDecisionText() == null
                || context.getGameState() == null
                || context.getPlayerId() == null
                || context.getObjectiveAnalyzer() == null
                || context.getObjectiveAnalyzer().isFlipped()
                || CaptureObjectiveFacts.objectiveKind(
                    context.getObjectiveAnalyzer())
                    != CaptureObjectivePolicy.ObjectiveKind.TIGIH) {
            return false;
        }
        String decision = context.getDecisionText().trim();
        if (!"choose card to move from".equalsIgnoreCase(decision)
                && !"choose card to move to".equalsIgnoreCase(decision)
                && !decision.toLowerCase(Locale.ROOT)
                    .startsWith("choose card to move to ")) {
            return false;
        }
        try {
            var actionState =
                    context.getGameState()
                        .getTopGameTextActionState();
            var liveAction = actionState != null
                    ? actionState.getGameTextAction() : null;
            PhysicalCard source = liveAction != null
                    ? liveAction.getActionSource() : null;
            String liveText = liveAction != null
                    ? liveAction.getText() : null;
            return CaptureObjectiveFacts.isOwnedExactSource(
                        source, context.getPlayerId(), "214_19")
                    && "move luke to landing platform"
                        .equalsIgnoreCase(
                            liveText != null
                                ? liveText.trim() : "");
        } catch (Exception e) {
            return false;
        }
    }

    private List<EvaluatedAction> evaluateCaptureVirtualHutMove(
            DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        var analyzer = context.getObjectiveAnalyzer();
        var actionState =
                gameState.getTopGameTextActionState();
        var liveAction = actionState != null
                ? actionState.getGameTextAction() : null;
        PhysicalCard hut = liveAction != null
                ? liveAction.getActionSource() : null;
        PhysicalCard target = null;
        if (game != null) {
            try {
                for (PhysicalCard card
                        : gameState.getAllPermanentCards()) {
                    if (CaptureObjectiveFacts
                            .isExactObjectiveTarget(
                                game, playerId, analyzer,
                                card)) {
                        target = card;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }
        PhysicalCard targetOrigin = null;
        if (game != null && target != null) {
            try {
                targetOrigin = game.getModifiersQuerying()
                        .getLocationThatCardIsAt(
                            gameState, target);
            } catch (Exception ignored) {
            }
        }
        boolean wholeRouteAvailable =
                CaptureObjectiveFacts
                    .isOwnedExactSource(
                        hut, playerId, "214_19")
                && CaptureObjectiveFacts
                    .isVirtualHutOrigin(hut)
                && samePhysicalCard(hut, targetOrigin)
                && CaptureObjectiveFacts
                    .virtualHutActionGuaranteesCapture(
                        game, playerId, analyzer);

        String decision = context.getDecisionText().trim();
        boolean chooseOrigin =
                "choose card to move from"
                    .equalsIgnoreCase(decision);
        boolean chooseDestination =
                "choose card to move to"
                    .equalsIgnoreCase(decision);
        PhysicalCard finalDestination =
                !chooseOrigin && !chooseDestination
                    ? resolveCastleFinalDestination(
                        gameState, decision) : null;

        List<Boolean> admissible = new ArrayList<>();
        boolean hasAdmissibleSelectableRoute = false;
        for (int i = 0;
                i < context.getCardIds().size(); i++) {
            String cardId = context.getCardIds().get(i);
            PhysicalCard candidate = null;
            try {
                candidate = gameState.findCardById(
                        Integer.parseInt(cardId));
            } catch (Exception ignored) {
            }
            boolean candidateAdmissible =
                    wholeRouteAvailable
                    && (chooseOrigin
                        ? samePhysicalCard(hut, candidate)
                            && CaptureObjectiveFacts
                                .isVirtualHutOrigin(
                                    candidate)
                        : chooseDestination
                            ? CaptureObjectiveFacts
                                .isGuaranteedVirtualHutDestination(
                                    game, playerId,
                                    analyzer, candidate)
                            : finalDestination != null
                                && CaptureObjectiveFacts
                                    .isGuaranteedVirtualHutDestination(
                                        game, playerId,
                                        analyzer,
                                        finalDestination)
                                && CaptureObjectiveFacts
                                    .isExactObjectiveTarget(
                                        game, playerId,
                                        analyzer, candidate));
            admissible.add(candidateAdmissible);
            hasAdmissibleSelectableRoute |=
                    isCardSelectable(context, i)
                    && candidateAdmissible;
        }

        for (int i = 0;
                i < context.getCardIds().size(); i++) {
            if (!isCardSelectable(context, i)) continue;
            String cardId = context.getCardIds().get(i);
            EvaluatedAction action = new EvaluatedAction(
                    cardId, ActionType.MOVE, 0.0f,
                    "Choose TIGIH virtual-Hut capture route");
            CaptureVirtualHutChoicePolicy.Choice choice =
                    CaptureVirtualHutChoicePolicy.choose(
                        new CaptureVirtualHutChoicePolicy.Facts(
                            admissible.get(i),
                            hasAdmissibleSelectableRoute));
            if (choice
                    == CaptureVirtualHutChoicePolicy
                        .Choice.PREFER) {
                PolicyContributionLedger captureLedger =
                        new PolicyContributionLedger(
                            "tigih-virtual-hut-child-"
                                + cardId);
                captureLedger.register(
                    CaptureObjectivePolicy.scoreCaptureRoute(
                        new CaptureObjectivePolicy
                            .CaptureRouteFacts(
                                cardId,
                                CaptureObjectivePolicy
                                    .ObjectiveKind.TIGIH,
                                CaptureObjectivePolicy
                                    .CaptureRouteStep
                                    .DESTINATION,
                                true)));
                PolicyOperationAdapter.apply(
                        action, captureLedger);
            } else if (choice
                    == CaptureVirtualHutChoicePolicy
                        .Choice.HARD_VETO) {
                action.hardVeto(
                    "TIGIH VIRTUAL HUT: choose the exact Hut, capture-capable Landing Platform, and objective Luke",
                    TraceRuleId.of(
                        "MOVE.OBJECTIVE.TIGIH.VIRTUAL_HUT_CHILD_HOLD"),
                    TraceDomainId.MOVE,
                    TraceOutputKind.VETO);
            }
            actions.add(action);
        }
        return actions;
    }

    private boolean isHuntDownCastleMoveDecision(
            DecisionContext context) {
        if (context == null || context.getDecisionText() == null
                || context.getGameState() == null
                || context.getObjectiveAnalyzer() == null
                || !context.getObjectiveAnalyzer().isHuntDownV()
                || context.getObjectiveAnalyzer().isFlipped()) {
            return false;
        }
        String decision = context.getDecisionText().trim();
        if (!"choose card to move from".equalsIgnoreCase(decision)
                && !"choose card to move to".equalsIgnoreCase(decision)
                && !decision.toLowerCase(Locale.ROOT)
                    .startsWith("choose card to move to ")) {
            return false;
        }
        try {
            var actionState =
                    context.getGameState().getTopGameTextActionState();
            var liveAction = actionState != null
                    ? actionState.getGameTextAction() : null;
            PhysicalCard source = liveAction != null
                    ? liveAction.getActionSource() : null;
            String liveText = liveAction != null
                    ? liveAction.getText() : null;
            return source != null
                    && "209_50".equals(
                        source.getBlueprintId(true))
                    && ("move from here to other battleground site"
                            .equalsIgnoreCase(
                                liveText != null
                                    ? liveText.trim() : "")
                        || "move from other battleground site to here"
                            .equalsIgnoreCase(
                                liveText != null
                                    ? liveText.trim() : ""));
        } catch (Exception e) {
            return false;
        }
    }

    private List<EvaluatedAction> evaluateHuntDownCastleMove(
            DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        var actionState = gameState.getTopGameTextActionState();
        var liveAction = actionState.getGameTextAction();
        PhysicalCard castle = liveAction.getActionSource();
        String liveText = liveAction.getText() != null
                ? liveAction.getText().trim() : "";
        boolean outbound =
                "move from here to other battleground site"
                    .equalsIgnoreCase(liveText);
        var objective = context.getObjectiveAnalyzer();
        List<com.gempukku.swccgo.ai.models.common.strategy
                .ObjectiveAnalyzer.VaderCastleRouteAssessment>
                routes = objective.assessVaderCastleRoutes(
                    context.getGame(), context.getPlayerId(),
                    castle, outbound);
        String decision = context.getDecisionText().trim();
        boolean chooseOrigin =
                "choose card to move from".equalsIgnoreCase(decision);
        boolean chooseDestination =
                "choose card to move to".equalsIgnoreCase(decision);
        PhysicalCard finalDestination =
                !chooseOrigin && !chooseDestination
                ? resolveCastleFinalDestination(
                        gameState, decision) : null;

        List<Boolean> admissible = new ArrayList<>();
        List<Boolean> preferred = new ArrayList<>();
        boolean hasAdmissibleOffered = false;
        for (int i = 0; i < context.getCardIds().size(); i++) {
            String cardId = context.getCardIds().get(i);
            PhysicalCard candidate = null;
            try {
                candidate = gameState.findCardById(
                        Integer.parseInt(cardId));
            } catch (Exception ignored) {
            }
            boolean candidateAdmissible = false;
            boolean candidatePreferred = false;
            for (var route : routes) {
                boolean bound = chooseOrigin
                        ? samePhysicalCard(
                            route.origin(), candidate)
                        : chooseDestination
                            ? samePhysicalCard(
                                route.destination(), candidate)
                            : finalDestination != null
                                && samePhysicalCard(
                                    route.destination(),
                                    finalDestination)
                                && samePhysicalCard(
                                    route.mover(), candidate);
                if (!bound) continue;
                candidateAdmissible |= route.admissible();
                candidatePreferred |= route.objectiveSafe();
            }
            admissible.add(candidateAdmissible);
            preferred.add(candidatePreferred);
            hasAdmissibleOffered |= isCardSelectable(context, i)
                    && candidateAdmissible;
        }

        for (int i = 0; i < context.getCardIds().size(); i++) {
            if (!isCardSelectable(context, i)) continue;
            String cardId = context.getCardIds().get(i);
            EvaluatedAction action = new EvaluatedAction(
                    cardId, ActionType.MOVE, 0.0f,
                    "Choose Vader's Castle move");
            if (preferred.get(i)) {
                action.addReasoning(
                    "OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE: safe route advances the battleground Vader leg",
                    1000.0f);
            } else if (!admissible.get(i)
                    && (hasAdmissibleOffered
                        || !routes.isEmpty())) {
                action.hardVeto(
                    "OBJECTIVE.HUNT_DOWN.CASTLE_ROUTE_HOLD: choose a safe Castle route");
            }
            actions.add(action);
        }
        return actions;
    }

    private boolean isObjectiveDockingTransitDecision(
            DecisionContext context) {
        if (context == null || context.getDecisionText() == null
                || context.getGameState() == null
                || context.getGame() == null
                || context.getObjectiveAnalyzer() == null
                || !context.getObjectiveAnalyzer()
                    .hasPreFlipRuntimeActorRule()) {
            return false;
        }
        String decision = normalizeDockingTransitPrompt(
                context.getDecisionText());
        if (!"choose docking bay to transit to"
                    .equalsIgnoreCase(decision)
                && !decision.toLowerCase(Locale.ROOT)
                    .startsWith(
                        "choose cards to docking bay transit to ")) {
            return false;
        }
        Object sourceId = context.getExtra(
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer
                    .DOCKING_TRANSIT_SOURCE_CARD_ID_EXTRA);
        if (sourceId == null) return false;
        try {
            PhysicalCard source =
                    context.getGameState().findCardById(
                        Integer.parseInt(
                            String.valueOf(sourceId)));
            return source != null
                    && Filters.docking_bay.accepts(
                        context.getGameState(),
                        context.getGame()
                            .getModifiersQuerying(),
                        source);
        } catch (Exception e) {
            return false;
        }
    }

    private List<EvaluatedAction>
            evaluateObjectiveDockingTransit(
                    DecisionContext context) {
        List<EvaluatedAction> actions =
                new ArrayList<>();
        GameState gameState = context.getGameState();
        String decision =
                normalizeDockingTransitPrompt(
                    context.getDecisionText());
        boolean chooseDestination =
                "choose docking bay to transit to"
                    .equalsIgnoreCase(decision);
        Object sourceId = context.getExtra(
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer
                    .DOCKING_TRANSIT_SOURCE_CARD_ID_EXTRA);
        PhysicalCard source = null;
        try {
            source = gameState.findCardById(
                    Integer.parseInt(
                        String.valueOf(sourceId)));
        } catch (Exception ignored) {
        }
        var objective = context.getObjectiveAnalyzer();
        List<com.gempukku.swccgo.ai.models.common.strategy
                .ObjectiveAnalyzer
                .DockingBayTransitRouteAssessment>
                routes = source != null
                    ? objective.assessDockingBayTransitRoutes(
                        context.getGame(),
                        context.getPlayerId(), source)
                    : List.of();
        if (chooseDestination && routes.isEmpty()) {
            return evaluateMoveDestination(context);
        }
        PhysicalCard finalDestination =
                chooseDestination ? null
                    : resolveDockingTransitDestination(
                        context, decision);

        for (int i = 0;
                i < context.getCardIds().size(); i++) {
            if (!isCardSelectable(context, i)) continue;
            String cardId =
                    context.getCardIds().get(i);
            PhysicalCard candidate = null;
            try {
                candidate = gameState.findCardById(
                        Integer.parseInt(cardId));
            } catch (Exception ignored) {
            }
            boolean admissible = false;
            boolean actorAdvance = false;
            boolean blockerChase = false;
            for (var route : routes) {
                boolean bound = chooseDestination
                        ? samePhysicalCard(
                            route.destination(), candidate)
                        : finalDestination != null
                            && samePhysicalCard(
                                route.destination(),
                                finalDestination)
                            && samePhysicalCard(
                                route.mover(), candidate);
                if (!bound || !route.admissible()) {
                    continue;
                }
                admissible = true;
                actorAdvance |=
                        route.objectiveAdvance();
                blockerChase |=
                        route.blockerChase();
            }

            EvaluatedAction action = new EvaluatedAction(
                    cardId, ActionType.MOVE, 0.0f,
                    chooseDestination
                        ? "Choose docking-bay transit destination"
                        : "Choose docking-bay transit mover");
            PhysicalCard destination =
                    chooseDestination
                        ? candidate : finalDestination;
            String destinationTitle =
                    destination != null
                        ? destination.getTitle() : null;
            String actorTitle =
                    !chooseDestination && candidate != null
                        ? candidate.getTitle() : "Vader";
            MoveDestinationPolicy.Contribution
                    objectiveMove = actorAdvance
                        ? MoveDestinationPolicy
                            .objectiveActorLocationDestination(
                                true, actorTitle,
                                destinationTitle)
                        : MoveDestinationPolicy
                            .objectiveBlockerChaseDestination(
                                blockerChase, actorTitle,
                                destinationTitle);
            if (objectiveMove.applies()) {
                action.addReasoning(
                        objectiveMove.reason(),
                        objectiveMove.delta());
            } else if (admissible) {
                action.addReasoning(
                    "MOVE.OBJECTIVE.DOCKING_TRANSIT_SAFE_COMPLETION: finish the selected transit without breaking the flip formation",
                    10.0f);
            } else if (!admissible
                    && !routes.isEmpty()) {
                action.hardVeto(
                    "OBJECTIVE.HUNT_DOWN.RUNTIME_ACTOR_TRANSIT_HOLD: choose a safe docking-bay transit route");
            }
            actions.add(action);
        }
        return actions;
    }

    private String normalizeDockingTransitPrompt(
            String decisionText) {
        if (decisionText == null) return "";
        String trimmed = decisionText.trim();
        String suffix =
                ", or click 'done' to cancel";
        if (trimmed.toLowerCase(Locale.ROOT)
                .endsWith(suffix)) {
            return trimmed.substring(
                    0, trimmed.length()
                        - suffix.length()).trim();
        }
        return trimmed;
    }

    private PhysicalCard resolveDockingTransitDestination(
            DecisionContext context,
            String decisionText) {
        GameState gameState = context != null
                ? context.getGameState() : null;
        if (gameState == null || decisionText == null) {
            return null;
        }
        Object destinationId = context.getExtra(
                com.gempukku.swccgo.ai.models.common.strategy
                    .ObjectiveAnalyzer
                    .DOCKING_TRANSIT_DESTINATION_CARD_ID_EXTRA);
        if (destinationId != null) {
            try {
                PhysicalCard exact =
                        gameState.findCardById(
                            Integer.parseInt(
                                String.valueOf(
                                    destinationId)));
                if (exact != null) return exact;
            } catch (Exception ignored) {
            }
        }
        PhysicalCard resolved = null;
        Collection<PhysicalCard> locations =
                gameState.getTopLocations();
        if (locations == null) return null;
        for (PhysicalCard location : locations) {
            if (location == null) continue;
            try {
                String exactPrompt =
                        "Choose cards to docking bay transit to "
                        + GameUtils.getCardLink(location);
                if (!exactPrompt.equalsIgnoreCase(
                        decisionText.trim())) {
                    continue;
                }
                if (resolved != null
                        && !samePhysicalCard(
                            resolved, location)) {
                    return null;
                }
                resolved = location;
            } catch (Exception ignored) {
            }
        }
        return resolved;
    }

    private PhysicalCard resolveCastleFinalDestination(
            GameState gameState, String decisionText) {
        if (gameState == null || decisionText == null) return null;
        PhysicalCard resolved = null;
        List<PhysicalCard> locations =
                gameState.getLocationsInOrder();
        if (locations == null) return null;
        for (PhysicalCard location : locations) {
            if (location == null) continue;
            try {
                String exactPrompt = "Choose card to move to "
                        + GameUtils.getCardLink(location);
                if (!exactPrompt.equalsIgnoreCase(
                        decisionText.trim())) {
                    continue;
                }
                if (resolved != null
                        && !samePhysicalCard(
                            resolved, location)) {
                    return null;
                }
                resolved = location;
            } catch (Exception ignored) {
            }
        }
        return resolved;
    }

    private boolean samePhysicalCard(
            PhysicalCard first, PhysicalCard second) {
        if (first == null || second == null) return false;
        if (first == second) return true;
        int firstId = first.getPermanentCardId();
        int secondId = second.getPermanentCardId();
        return firstId > 0 && firstId == secondId;
    }

    private List<EvaluatedAction> evaluateMoveDestination(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        Side mySide = context.getSide();
        Integer exactMoverCardId =
            movePhysicalCardId(context);

        // Icon bonus constant (same as MoveEvaluator/Python)
        // === V169 (Steve, 2026-06): RETREAT MODE — is the card being moved ENDANGERED? ===
        // Replay lk6xgsokjcwrwxuu fatal move 1: Asajj at Guest Quarters with Luke AT her site
        // could not retreat — every safe (empty) destination was V41-wrong-direction blocked
        // (-9999), the destination step cancelled out, the cancel-loop guard then hard-vetoed
        // the move action, and she was beaten 6v27 next turn. Steve: "Rando should have ...
        // moved Asajj to an adjacent safe site that I did not occupy."
        // Find the moving card from the decision-text blueprint hint; if its CURRENT location
        // is endangered (opponent out-powers us there), this whole decision is a RETREAT:
        // safe destinations get a big bonus and V41 is gated off (a retreat IS a move to an
        // empty site — V41's assumption is wrong for endangered movers).
        MoveDestinationPolicy.RetreatMode v169Retreat =
            MoveDestinationPolicy.retreatMode(null, 0.0f);
        if (game != null && gameState != null && playerId != null) {
            try {
                String v169MoverBp = extractBlueprintFromDecisionText(context.getDecisionText());
                MovePhysicalCardResolver.ResolvedMover v169Mover =
                    MovePhysicalCardResolver.resolveOnTable(
                        gameState.getAllPermanentCards(), playerId,
                        v169MoverBp, exactMoverCardId);
                if (v169Mover != null) {
                    PhysicalCard v169Pc = v169Mover.card();
                    PhysicalCard v169Loc = v169Mover.origin();
                    float v169PowerExcess =
                        v169OppPowerExcessAt(game, gameState, playerId, v169Loc);
                    v169Retreat = MoveDestinationPolicy.retreatMode(
                        v169Loc.getTitle(), v169PowerExcess);
                    if (v169Retreat.active()) {
                        logger.warn("V169 RETREAT MODE: mover '{}' is endangered at {} — safe destinations boosted, V41 gated",
                            v169Pc.getTitle(), v169Retreat.originTitle());
                    }
                }
            } catch (Exception e) { logger.debug("V169 retreat-mode error: {}", e.getMessage()); }
        }

        // === V156 JOIN-GROUP MODE (2026-07-07, destination arm; Fel-at-Beach loss, audit deploy-siting-2) ===
        // Twin of MoveEvaluator's V156 JOIN-GROUP R2 claim (same date). When the mover is a
        // weak (ability<4) SOLO character at an uncontested site, this destination decision
        // is a JOIN: friendly-stack destinations get a bonus below (largest stack preferred)
        // and V41 WRONG DIRECTION is gated off for them — a consolidate/join-allies move
        // toward OUR OWN stack is by definition not "wrong direction" (V41's 'empty' only
        // counts opponents; that -9999 is what stranded Fel at Scarif: Beach: the ladder's
        // R2 claim moved, the only join destination scored -10151, V160 broke the cancel
        // loop, and Fel rotted in place until battled and forfeited). Mirrors the V169
        // retreat-mode and V67z exemption pattern. Exempt: undercover spies (V170 parked
        // spies sit) and a solo doing READY objective work at a flip-relevant site (shared
        // CharacterDeploySiteEvaluator.isV156FlipNotReady predicate — same carve the deploy
        // side uses). Mutually exclusive with V169 retreat mode (that needs opponent excess
        // AT the mover's site; join mode needs opponent power 0 there).
        boolean v156JoinMode = false;
        String v156FromTitle = null;
        float v156MoverAbility = 0f;  // STACK-MATH (2026-07-07): mover's ability, for the defensible-join kicker below
        if (game != null && gameState != null && playerId != null) {
            try {
                String v156MoverBp = extractBlueprintFromDecisionText(context.getDecisionText());
                MovePhysicalCardResolver.ResolvedMover v156Mover =
                    MovePhysicalCardResolver.resolveOnTable(
                        gameState.getAllPermanentCards(), playerId,
                        v156MoverBp, exactMoverCardId);
                if (v156Mover != null) {
                    PhysicalCard v156Pc = v156Mover.card();
                    PhysicalCard v156Loc = v156Mover.origin();
                    SwccgCardBlueprint v156Bp = v156Pc.getBlueprint();
                    Float v156Ab = v156Bp != null
                        && v156Bp.getCardCategory() == CardCategory.CHARACTER
                        && !v156Pc.isUndercover()
                        && v156Bp.hasAbilityAttribute()
                            ? v156Bp.getAbility() : null;
                    if (v156Ab != null && v156Ab < 4f) {
                        boolean v156Alone = true;
                        for (PhysicalCard c : gameState.getCardsAtLocation(v156Loc)) {
                            if (c == null || c == v156Pc || !playerId.equals(c.getOwner())) continue;
                            if (c.getBlueprint() == null) continue;
                            if (c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                v156Alone = false;
                                break;
                            }
                        }
                        float v156OppPowerHere = game.getModifiersQuerying().getTotalPowerAtLocation(
                            gameState, v156Loc, gameState.getOpponent(playerId), false, false);
                        boolean v156AtReadyFlipSite = false;
                        try {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v156Oa =
                                context.getObjectiveAnalyzer();
                            v156AtReadyFlipSite = v156Oa != null && v156Oa.isAnalyzed()
                                && v156Loc.getTitle() != null
                                && v156Oa.isObjectiveRelevantLocation(v156Loc.getTitle())
                                && !com.gempukku.swccgo.ai.models.common.strategy.CharacterDeploySiteEvaluator
                                    .isV156FlipNotReady(gameState, playerId);
                        } catch (Exception ignore) { /* false */ }
                        if (v156Alone && v156OppPowerHere == 0f && !v156AtReadyFlipSite) {
                            v156JoinMode = true;
                            v156FromTitle = v156Loc.getTitle();
                            v156MoverAbility = v156Ab;
                            logger.warn("V156 JOIN-GROUP MODE: mover '{}' (ability {}) is a weak solo at {} — friendly-stack destinations boosted, V41 gated",
                                v156Pc.getTitle(), (int) v156Ab.floatValue(), v156FromTitle);
                        }
                    }
                }
            } catch (Exception e) { logger.debug("V156 join-mode error: {}", e.getMessage()); }
        }

        // FORMATION SAFETY (2026-07-11c): resolve the ACTUAL mover once for L1/L4 vetoes below.
        // Same extraction the V169/V156 modes use; null mover/origin => partial info => no vetoes
        // (council rule: never veto blind).
        PhysicalCard fsMover = null;
        PhysicalCard fsOrigin = null;
        if (game != null && gameState != null && playerId != null) {
            try {
                String fsMoverBp = extractBlueprintFromDecisionText(context.getDecisionText());
                MovePhysicalCardResolver.ResolvedMover fsResolved =
                    MovePhysicalCardResolver.resolveOnTable(
                        gameState.getAllPermanentCards(), playerId,
                        fsMoverBp, exactMoverCardId);
                if (fsResolved != null) {
                    fsMover = fsResolved.card();
                    fsOrigin = fsResolved.origin();
                }
            } catch (Exception e) { /* partial info — no veto */ }
        }
        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.MOVE,
                0.0f,  // Start at 0 for move decisions
                "Move to location " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard location = gameState.findCardById(Integer.parseInt(cardId));
                    if (location != null) {
                        String title = location.getTitle();
                        action.setDisplayText("Move to " + (title != null ? title : "location"));
                        boolean guaranteedCaptureMoveDestination =
                                applyCaptureMoveDestination(
                                    context,
                                    action,
                                    fsMover,
                                    location);

                        boolean objectiveTerminalEscapeCandidate =
                            fsMover != null && fsOrigin != null
                                && context.getObjectiveAnalyzer() != null
                                && context.getObjectiveAnalyzer()
                                    .isFirstOrderReignsTerminalExposureAt(
                                        game, playerId,
                                        fsMover, fsOrigin)
                                && !context.getObjectiveAnalyzer()
                                    .isFirstOrderReignsTerminalExposureAt(
                                        game, playerId,
                                        fsMover, location);
                        MoveDestinationPolicy.CompanionVeto terminalExposure =
                            MoveDestinationPolicy.terminalObjectiveExposure(
                                fsMover != null
                                    && context.getObjectiveAnalyzer() != null
                                    && context.getObjectiveAnalyzer()
                                        .isFirstOrderReignsTerminalExposureAt(
                                            game, playerId, fsMover, location));
                        if (terminalExposure.hardVeto()) {
                            action.hardVeto(terminalExposure.reason());
                            logger.warn("OBJECTIVE TERMINAL EXPOSURE (move-dest): {}",
                                terminalExposure.reason());
                        }

                        boolean bhbmFormationSafe = false;
                        // FORMATION SAFETY (2026-07-11c): L4 solo-charge + L1 abandon-solo vetoes —
                        // un-outvotable (Codex audit: V32 -300 + V22.2 -120 lost to R2 +6000 here).
                        if (fsMover != null && fsOrigin != null
                                && game != null) {
                            String fsV = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                .vetoMoveDestination(game, gameState, playerId, fsMover, location);
                            if (fsV == null && fsOrigin != null
                                    && !objectiveTerminalEscapeCandidate
                                    && fsOrigin.getCardId() != location.getCardId()) {
                                fsV = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                    .vetoMoveOrigin(game, gameState, playerId, fsMover, fsOrigin);
                            }
                            if (fsV != null) {
                                action.hardVeto(fsV);
                                logger.warn("FORMATION SAFETY (move-dest): {}", fsV);
                            } else {
                                bhbmFormationSafe = true;
                                // BATCH1b (2026-07-12, Codex m00199/m00209 — Chiraneau empty-site split):
                                // a WEAK (ability<4) mover relocating SOLO to an uncontested empty site
                                // while leaving a lone weak buddy behind creates TWO weak solos — L1
                                // requires enemy at origin and L4 exits on empty destinations, so both
                                // guards miss it. Heavy penalty (-800: 327.5 -> -472.5 loses to Pass),
                                // NOT a veto — genuine repositioning must stay possible.
                                try {
                                    SwccgCardBlueprint fsMbp = fsMover.getBlueprint();
                                    Float fsMa = (fsMbp != null && fsMbp.hasAbilityAttribute()) ? fsMbp.getAbility() : null;
                                    if (fsMa != null && fsMa < 4f && fsOrigin != null
                                            && fsOrigin.getCardId() != location.getCardId()) {
                                        String fsOpp = gameState.getOpponent(playerId);
                                        float fsDestOpp = game.getModifiersQuerying().getTotalPowerAtLocation(gameState, location, fsOpp, false, false);
                                        // BATCH1b-CORR (2026-07-13, Codex m00225 #2): power<=0 misreads a
                                        // PRESENT power-0 friendly as an empty site and still fires -800 —
                                        // count friendly non-undercover characters instead (shared helper,
                                        // pure-tested; extracted per m00262 fixture requirement).
                                        int fsDestOurChars = com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                                            .countFriendlyNonUndercoverCharacters(gameState.getCardsAtLocation(location), playerId);
                                        if (fsDestOpp <= 0 && fsDestOurChars == 0) {
                                            int fsRemain = 0; float fsRemainMaxAb = 0f;
                                            for (PhysicalCard oc : gameState.getCardsAtLocation(fsOrigin)) {
                                                if (oc == null || oc.getBlueprint() == null) continue;
                                                if (oc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                                if (!playerId.equals(oc.getOwner())) continue;
                                                if (oc.getCardId() == fsMover.getCardId()) continue;
                                                if (oc.isUndercover()) continue;
                                                fsRemain++;
                                                Float oa = oc.getBlueprint().hasAbilityAttribute() ? oc.getBlueprint().getAbility() : null;
                                                if (oa != null && oa > fsRemainMaxAb) fsRemainMaxAb = oa;
                                            }
                                            MoveAbilityPolicy.Evaluation weakSplit =
                                                MoveAbilityPolicy.weakSplit(
                                                    fsMa, true, fsDestOpp,
                                                    fsDestOurChars, fsRemain, fsRemainMaxAb);
                                            if (weakSplit.applies()) {
                                                action.addReasoning(
                                                    weakSplit.reason(), weakSplit.delta());
                                                logger.warn("FORMATION SAFETY (move-dest): L1/L4 SPLIT -800 — {} to {} leaves lone weak buddy at {}",
                                                    fsMover.getTitle(), location.getTitle(), fsOrigin.getTitle());
                                            }
                                        }
                                    }
                                } catch (Exception fsSplitE) { /* fail-open */ }
                            }
                        }
                        if (context.getDecisionText() != null
                                && context.getDecisionText()
                                    .toLowerCase(Locale.ROOT)
                                    .contains("using landspeed")) {
                            applyBhbmForceDripUrgency(
                                context, action,
                                fsMover,
                                location,
                                bhbmFormationSafe,
                                BhbmForceDripUrgencyFactsReader
                                    .CandidateMechanism.LANDSPEED);
                        }

                        boolean exactTdigwattLandoDestination =
                            false;
                        Integer tdigwattSourceId =
                            integerExtra(
                                context,
                                TdigwattObjectiveFactsReader
                                    .LANDO_ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA);
                        Integer tdigwattMoverId =
                            integerExtra(
                                context,
                                TdigwattObjectiveFactsReader
                                    .LANDO_MOVER_PERMANENT_CARD_ID_EXTRA);
                        Integer tdigwattDestinationId =
                            integerExtra(
                                context,
                                TdigwattObjectiveFactsReader
                                    .LANDO_DESTINATION_PERMANENT_CARD_ID_EXTRA);
                        if (game != null
                                && playerId != null
                                && fsMover != null
                                && tdigwattSourceId != null
                                && tdigwattMoverId != null
                                && tdigwattDestinationId != null
                                && fsMover
                                    .getPermanentCardId()
                                    == tdigwattMoverId
                                && location
                                    .getPermanentCardId()
                                    == tdigwattDestinationId) {
                            PhysicalCard tdigwattSource =
                                gameState
                                    .findCardByPermanentId(
                                        tdigwattSourceId);
                            var tdigwattRoute =
                                TdigwattObjectiveFactsReader
                                    .readUsefulVirtualLandoLandspeedRoute(
                                        game,
                                        playerId,
                                        tdigwattSource,
                                        TdigwattObjectiveFactsReader
                                            .Proof.PROVEN);
                            if (tdigwattRoute.isPresent()
                                    && tdigwattRoute.get()
                                        .landoPermanentCardId()
                                        == tdigwattMoverId
                                    && tdigwattRoute.get()
                                        .destinationPermanentCardId()
                                        == tdigwattDestinationId) {
                                exactTdigwattLandoDestination =
                                    true;
                                applyTdigwattPolicy(
                                    action,
                                    TdigwattObjectiveScoringPolicy
                                        .scoreVirtualLandoDestination(
                                            new TdigwattObjectiveScoringPolicy
                                                .LandoDestinationFacts(
                                                    new TdigwattObjectiveScoringPolicy
                                                        .LandoActionFacts(
                                                            cardId,
                                                            tdigwattRoute
                                                                .get()
                                                                .moveFacts(),
                                                            true,
                                                            context
                                                                .getForcePileSize()),
                                                    true))
                                        .result());
                            }
                        }

                        boolean objectiveActorRouteDestination = false;
                        boolean objectiveActorLocationDestination = false;
                        boolean objectiveRequiredCardEnablerDestination =
                                false;
                        boolean objectiveNabooDuelFrontRouteDestination =
                                false;
                        boolean objectiveNabooDuelFrontRouteCurrent = false;
                        boolean objectiveNabooDuelFrontRouteAtDestination =
                                false;
                        com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer.ObjectivePostFlipPayoffRole
                            objectivePostFlipPayoffDestination =
                                com.gempukku.swccgo.ai.models.common.strategy
                                    .ObjectiveAnalyzer
                                    .ObjectivePostFlipPayoffRole.NONE;
                        com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer.ObjectivePostFlipPayoffRole
                            objectivePostFlipPayoffCurrent =
                                com.gempukku.swccgo.ai.models.common.strategy
                                    .ObjectiveAnalyzer
                                    .ObjectivePostFlipPayoffRole.NONE;
                        com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer.ObjectivePostFlipPayoffRole
                            objectivePostFlipPayoffAtDestination =
                                com.gempukku.swccgo.ai.models.common.strategy
                                    .ObjectiveAnalyzer
                                    .ObjectivePostFlipPayoffRole.NONE;
                        boolean objectiveFirstOrderDrainPairDestination =
                                false;
                        boolean objectiveFirstOrderDrainPairBreak = false;
                        boolean objectiveTerminalEscapeDestination =
                                false;
                        if (fsMover != null && fsOrigin != null
                                && game != null && playerId != null) {
                            try {
                                var routeAnalyzer =
                                    context.getObjectiveAnalyzer();
                                objectiveActorRouteDestination =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    && (routeAnalyzer
                                        .advancesPreFlipActorRoute(
                                            game, playerId, fsMover,
                                            location)
                                        || routeAnalyzer
                                            .advancesShieldMainGeneratorRoute(
                                                game, playerId,
                                                fsMover, location));
                                objectiveActorLocationDestination =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    && (routeAnalyzer
                                        .advancesPreFlipActorAtRuntimeLocation(
                                            game, playerId, fsMover,
                                            location)
                                        || routeAnalyzer
                                            .advancesPreFlipPlainPresenceAtRequiredLocation(
                                                game, playerId, fsMover,
                                                location));
                                objectiveRequiredCardEnablerDestination =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    && routeAnalyzer
                                        .advancesRequiredCardDeployPrerequisiteByMovingTo(
                                                game, playerId, fsMover,
                                                location);
                                objectiveNabooDuelFrontRouteDestination =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    && routeAnalyzer
                                        .advancesNabooDuelFrontTargetRouteAt(
                                            game, playerId, fsMover,
                                            location);
                                objectivePostFlipPayoffDestination =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    ? routeAnalyzer
                                        .classifyPostFlipPayoffAt(
                                            game, playerId, fsMover,
                                            location)
                                    : com.gempukku.swccgo.ai.models.common
                                        .strategy.ObjectiveAnalyzer
                                        .ObjectivePostFlipPayoffRole.NONE;
                                if (routeAnalyzer != null
                                        && routeAnalyzer.isAnalyzed()) {
                                    objectiveNabooDuelFrontRouteCurrent =
                                        routeAnalyzer
                                            .qualifiesNabooDuelFrontTargetRouteAt(
                                                game, playerId,
                                                fsMover, fsOrigin);
                                    objectiveNabooDuelFrontRouteAtDestination =
                                        routeAnalyzer
                                            .qualifiesNabooDuelFrontTargetRouteAt(
                                                game, playerId,
                                                fsMover, location);
                                    objectivePostFlipPayoffCurrent =
                                        routeAnalyzer
                                            .classifyPostFlipPayoffRoleAt(
                                                game, playerId,
                                                fsMover, fsOrigin);
                                    objectivePostFlipPayoffAtDestination =
                                        routeAnalyzer
                                            .classifyPostFlipPayoffRoleAt(
                                                game, playerId,
                                                fsMover, location);
                                }
                                boolean preservesRuntimeActor =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    && routeAnalyzer
                                        .qualifiesPreFlipRuntimeActorAtLocation(
                                            game, playerId, fsMover,
                                            location);
                                boolean objectiveBlockerChaseDestination =
                                    !objectiveActorLocationDestination
                                    && preservesRuntimeActor
                                    && routeAnalyzer
                                        .isPreFlipGlobalBlockerAt(
                                            game, playerId, location);
                                var moverFormationRole =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    ? routeAnalyzer
                                        .classifyGateFormationPieceIfRemoved(
                                            game, playerId, fsMover)
                                    : com.gempukku.swccgo.ai.models
                                        .common.strategy
                                        .ObjectiveAnalyzer
                                        .FlipGateFormationRole.NONE;
                                if (moverFormationRole
                                        == com.gempukku.swccgo.ai.models
                                            .common.strategy
                                            .ObjectiveAnalyzer
                                            .FlipGateFormationRole
                                            .LAST_OBJECTIVE_SURVIVAL_ACTOR
                                        && context.getDecisionText() != null
                                        && context.getDecisionText()
                                            .toLowerCase(Locale.ROOT)
                                            .contains("using landspeed")) {
                                    String opponent =
                                        gameState.getOpponent(playerId);
                                    float friendlyPower =
                                        game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                playerId, false, false);
                                    float opponentPower = opponent != null
                                        ? game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                opponent, false, false)
                                            + com.gempukku.swccgo.ai.models
                                                .common.strategy
                                                .FormationSafety
                                                .weaponBonusAt(
                                                    gameState, fsOrigin,
                                                    opponent)
                                        : 0.0f;
                                    boolean safeRelocation =
                                        routeAnalyzer
                                            .preservesStayFlippedRequirementByMovingTo(
                                                game, playerId,
                                                fsMover, location);
                                    var survivalHold =
                                        MoveObjectiveGateHoldPolicy
                                            .evaluatePostFlipSurvivalActor(
                                                true, friendlyPower,
                                                opponentPower,
                                                safeRelocation);
                                    if (survivalHold.hardVeto()) {
                                        action.hardVeto(
                                            survivalHold.reason(),
                                            TraceRuleId.of(
                                                "MOVE.OBJECTIVE.POST_FLIP_SURVIVAL_HOLD"),
                                            TraceDomainId.MOVE,
                                            TraceOutputKind.VETO);
                                    }
                                }
                                // Hoth repair #3 (2026-07-27): anchor-
                                // concentration guard. Applies to EVERY move
                                // route (no "using landspeed" text gate; that
                                // gate is why the replay's walker move
                                // escaped the survival hold). Blocks moving
                                // INTO the sole unsafe stay-flipped anchor
                                // site when no other safe anchor site exists;
                                // otherwise a -250 ordering nudge prefers a
                                // separate anchor site. Data-scoped via the
                                // profile's stayFlipped rules.
                                if (routeAnalyzer != null
                                        && routeAnalyzer.isAnalyzed()
                                        && routeAnalyzer
                                            .holdsStayFlippedAnchor(
                                                game, playerId, location)) {
                                    String anchorOpp =
                                        gameState.getOpponent(playerId);
                                    float anchorFriendly =
                                        game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, location,
                                                playerId, false, false);
                                    float anchorOpposing = anchorOpp != null
                                        ? game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, location,
                                                anchorOpp, false, false)
                                            + com.gempukku.swccgo.ai.models
                                                .common.strategy
                                                .FormationSafety
                                                .weaponBonusAt(
                                                    gameState, location,
                                                    anchorOpp)
                                        : 0.0f;
                                    boolean anchorDestUnsafe =
                                        anchorOpposing > anchorFriendly
                                            + MoveObjectiveGateHoldPolicy
                                                .RETREATABLE_POWER_GAP;
                                    int safeOtherAnchorSites =
                                        routeAnalyzer
                                            .countSafeStayFlippedAnchorSites(
                                                game, playerId, location,
                                                MoveObjectiveGateHoldPolicy
                                                    .RETREATABLE_POWER_GAP);
                                    var concentrationHold =
                                        MoveObjectiveGateHoldPolicy
                                            .evaluatePostFlipAnchorConcentration(
                                                true,
                                                routeAnalyzer
                                                    .isSoleStayFlippedAnchorSite(
                                                        game, playerId,
                                                        location),
                                                anchorDestUnsafe,
                                                safeOtherAnchorSites);
                                    if (concentrationHold.hardVeto()) {
                                        action.hardVeto(
                                            concentrationHold.reason(),
                                            TraceRuleId.of(
                                                "MOVE.OBJECTIVE.POST_FLIP_ANCHOR_CONCENTRATION"),
                                            TraceDomainId.MOVE,
                                            TraceOutputKind.VETO);
                                    } else if (safeOtherAnchorSites > 0) {
                                        action.addReasoning(
                                            "MOVE.OBJECTIVE.ANCHOR_SPREAD: prefer a separate stay-flipped anchor site -250",
                                            -250.0f);
                                    }
                                }
                                boolean countedFormationActive =
                                    routeAnalyzer != null
                                    && routeAnalyzer.isAnalyzed()
                                    && !routeAnalyzer.isFlipped()
                                    && (routeAnalyzer
                                            .hasCountedPreFlipActorRule()
                                        || routeAnalyzer
                                            .hasActiveRequiredCardDeployActorRule(
                                                game, playerId)
                                        || routeAnalyzer
                                            .hasShieldMainGeneratorFormationRule());
                                if (countedFormationActive
                                        && context.getDecisionText() != null
                                        && context.getDecisionText()
                                            .toLowerCase(Locale.ROOT)
                                            .contains("using landspeed")) {
                                    String opponent =
                                        gameState.getOpponent(playerId);
                                    float friendlyPower =
                                        game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                playerId, false, false);
                                    float opponentPower = opponent != null
                                        ? game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                opponent, false, false)
                                            + com.gempukku.swccgo.ai.models
                                                .common.strategy
                                                .FormationSafety
                                                .weaponBonusAt(
                                                    gameState, fsOrigin,
                                                    opponent)
                                        : 0.0f;
                                    boolean preservesCount =
                                        routeAnalyzer
                                            .preservesRequiredCardDeployPrerequisiteByMovingTo(
                                                game, playerId,
                                                fsMover, location)
                                        || routeAnalyzer
                                            .advancesShieldMainGeneratorRoute(
                                                game, playerId,
                                                fsMover, location)
                                        || routeAnalyzer
                                            .hasCountedOperativeFormationRule()
                                            && preservesRuntimeActor;
                                    var countedDestinationHold =
                                        MoveObjectiveGateHoldPolicy
                                            .evaluateCountedFormation(
                                                countedFormationActive,
                                                moverFormationRole,
                                                friendlyPower,
                                                opponentPower,
                                                preservesCount);
                                    if (countedDestinationHold
                                            .hardVeto()) {
                                        action.hardVeto(
                                            countedDestinationHold
                                                .reason(),
                                            TraceRuleId.of(
                                                "MOVE.OBJECTIVE.COUNTED_FORMATION_HOLD"),
                                            TraceDomainId.MOVE,
                                            TraceOutputKind.VETO);
                                    }
                                }
                                if (moverFormationRole
                                        == com.gempukku.swccgo.ai.models
                                            .common.strategy
                                            .ObjectiveAnalyzer
                                            .FlipGateFormationRole
                                            .REQUIRED_CARD_RETENTION_DEFENDER) {
                                    String opponent =
                                        gameState.getOpponent(playerId);
                                    float friendlyPower =
                                        game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                playerId, false, false);
                                    float opponentPower = opponent != null
                                        ? game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                opponent, false, false)
                                            + com.gempukku.swccgo.ai.models
                                                .common.strategy
                                                .FormationSafety
                                                .weaponBonusAt(
                                                    gameState, fsOrigin,
                                                    opponent)
                                        : 0.0f;
                                    boolean safeRelocation =
                                        routeAnalyzer
                                            .preservesRequiredCardCancelPreventionAt(
                                                game, playerId,
                                                fsMover, location);
                                    var retentionHold =
                                        MoveObjectiveGateHoldPolicy
                                            .evaluateRequiredCardRetentionDefender(
                                                moverFormationRole,
                                                friendlyPower,
                                                opponentPower,
                                                safeRelocation);
                                    if (retentionHold.hardVeto()) {
                                        action.hardVeto(
                                            retentionHold.reason());
                                    }
                                }
                                if (routeAnalyzer != null
                                        && routeAnalyzer.isAnalyzed()
                                        && routeAnalyzer
                                            .hasPreFlipRuntimeActorRule()
                                        && context.getDecisionText() != null
                                        && context.getDecisionText()
                                            .toLowerCase(Locale.ROOT)
                                            .contains("using landspeed")
                                        && moverFormationRole
                                            == com.gempukku.swccgo.ai.models
                                                .common.strategy
                                                .ObjectiveAnalyzer
                                                .FlipGateFormationRole
                                                .LAST_REQUIRED_ACTOR
                                        && !preservesRuntimeActor) {
                                    String opponent =
                                        gameState.getOpponent(playerId);
                                    float friendlyPower =
                                        game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                playerId, false, false);
                                    float opponentPower = opponent != null
                                        ? game.getModifiersQuerying()
                                            .getTotalPowerAtLocation(
                                                gameState, fsOrigin,
                                                opponent, false, false)
                                            + com.gempukku.swccgo.ai.models
                                                .common.strategy
                                                .FormationSafety
                                                .weaponBonusAt(
                                                    gameState, fsOrigin,
                                                    opponent)
                                        : 0.0f;
                                    if (opponentPower
                                            <= friendlyPower + 6.0f) {
                                        action.hardVeto(
                                            "OBJECTIVE.HUNT_DOWN.RUNTIME_ACTOR_DESTINATION_HOLD: Vader must remain at a battleground site");
                                    }
                                }
                                MoveDestinationPolicy.Contribution
                                    objectiveRoute =
                                        MoveDestinationPolicy
                                            .objectiveActorRouteDestination(
                                                objectiveActorRouteDestination,
                                                fsMover.getTitle(), title);
                                if (objectiveRoute.applies()) {
                                    action.addReasoning(
                                        objectiveRoute.reason(),
                                        objectiveRoute.delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.ACTOR_ROUTE_DESTINATION"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.BANDED);
                                    logger.warn(
                                        "OBJECTIVE ACTOR ROUTE DEST: {} to {} +1000",
                                        fsMover.getTitle(), title);
                                }
                                MoveDestinationPolicy.Contribution
                                    objectiveRequiredCardEnabler =
                                        MoveDestinationPolicy
                                            .objectiveRequiredCardEnablerDestination(
                                                objectiveRequiredCardEnablerDestination,
                                                fsMover.getTitle(),
                                                title);
                                if (objectiveRequiredCardEnabler.applies()) {
                                    action.addReasoning(
                                        objectiveRequiredCardEnabler
                                            .reason(),
                                        objectiveRequiredCardEnabler
                                            .delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_DESTINATION"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.ORDERING);
                                    logger.warn(
                                        "OBJECTIVE REQUIRED-CARD ENABLER DEST: {} to {} +1000",
                                        fsMover.getTitle(), title);
                                }
                                MoveDestinationPolicy.Contribution
                                    objectiveActorLocation =
                                        MoveDestinationPolicy
                                            .objectiveActorLocationDestination(
                                                objectiveActorLocationDestination,
                                                fsMover.getTitle(), title);
                                if (objectiveActorLocation.applies()) {
                                    action.addReasoning(
                                        objectiveActorLocation.reason(),
                                        objectiveActorLocation.delta());
                                    logger.warn(
                                        "OBJECTIVE ACTOR LOCATION DEST: {} to {} +1000",
                                        fsMover.getTitle(), title);
                                }
                                MoveDestinationPolicy.Contribution
                                    nabooDuelFrontRoute =
                                        MoveDestinationPolicy
                                            .objectiveNabooDuelFrontRouteDestination(
                                                objectiveNabooDuelFrontRouteDestination,
                                                fsMover.getTitle(), title);
                                if (nabooDuelFrontRoute.applies()) {
                                    action.addReasoning(
                                        nabooDuelFrontRoute.reason(),
                                        nabooDuelFrontRoute.delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.NABOO_DUEL_FRONT_ROUTE_DESTINATION"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.BANDED);
                                }
                                MoveDestinationPolicy.Contribution
                                    nabooDuelFrontRouteRetention =
                                        MoveDestinationPolicy
                                            .objectiveNabooDuelFrontRouteRetention(
                                                objectiveNabooDuelFrontRouteCurrent,
                                                objectiveNabooDuelFrontRouteAtDestination,
                                                fsMover.getTitle(), title);
                                if (nabooDuelFrontRouteRetention.applies()) {
                                    action.addReasoning(
                                        nabooDuelFrontRouteRetention.reason(),
                                        nabooDuelFrontRouteRetention.delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.NABOO_DUEL_FRONT_ROUTE_HOLD"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.ORDERING);
                                }
                                MoveDestinationPolicy.Contribution
                                    postFlipPayoff =
                                        MoveDestinationPolicy
                                            .objectivePostFlipPayoffDestination(
                                                objectivePostFlipPayoffDestination,
                                                fsMover.getTitle(), title);
                                if (!guaranteedCaptureMoveDestination
                                        && postFlipPayoff.applies()) {
                                    action.addReasoning(
                                        postFlipPayoff.reason(),
                                        postFlipPayoff.delta(),
                                        TraceRuleId.of(
                                            objectivePostFlipPayoffDestination
                                                == com.gempukku.swccgo.ai.models
                                                    .common.strategy
                                                    .ObjectiveAnalyzer
                                                    .ObjectivePostFlipPayoffRole
                                                    .PRIMARY
                                                ? "MOVE.OBJECTIVE.POST_FLIP_PRIMARY_PAYOFF"
                                                : "MOVE.OBJECTIVE.POST_FLIP_SECONDARY_PAYOFF"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.BANDED);
                                    logger.warn(
                                        "OBJECTIVE POST-FLIP PAYOFF DEST: {} to {} +{}",
                                        fsMover.getTitle(), title,
                                        (int) postFlipPayoff.delta());
                                }
                                MoveDestinationPolicy.Contribution
                                    postFlipPayoffRetention =
                                        MoveDestinationPolicy
                                            .objectivePostFlipPayoffRetention(
                                                objectivePostFlipPayoffCurrent,
                                                objectivePostFlipPayoffAtDestination,
                                                fsMover.getTitle(), title);
                                if (postFlipPayoffRetention.applies()) {
                                    action.addReasoning(
                                        postFlipPayoffRetention.reason(),
                                        postFlipPayoffRetention.delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.POST_FLIP_PAYOFF_HOLD"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.ORDERING);
                                    logger.warn(
                                        "OBJECTIVE POST-FLIP PAYOFF HOLD: {} to {} {}",
                                        fsMover.getTitle(), title,
                                        (int) postFlipPayoffRetention.delta());
                                }
                                MoveDestinationPolicy.Contribution
                                    objectiveBlockerChase =
                                        MoveDestinationPolicy
                                            .objectiveBlockerChaseDestination(
                                                objectiveBlockerChaseDestination,
                                                fsMover.getTitle(), title);
                                if (objectiveBlockerChase.applies()) {
                                    action.addReasoning(
                                        objectiveBlockerChase.reason(),
                                        objectiveBlockerChase.delta());
                                    logger.warn(
                                        "OBJECTIVE BLOCKER CHASE DEST: {} to {} +1000",
                                        fsMover.getTitle(), title);
                                }
                            } catch (Exception e) {
                                logger.debug(
                                    "Objective actor-route destination assessment failed: {}",
                                    e.getMessage());
                            }
                            try {
                                var firstOrderAnalyzer =
                                    context.getObjectiveAnalyzer();
                                if (firstOrderAnalyzer != null
                                        && firstOrderAnalyzer.isAnalyzed()) {
                                    boolean terminalAtOrigin =
                                        firstOrderAnalyzer
                                            .isFirstOrderReignsTerminalExposureAt(
                                                game, playerId,
                                                fsMover, fsOrigin);
                                    objectiveTerminalEscapeDestination =
                                        terminalAtOrigin
                                        && !firstOrderAnalyzer
                                            .isFirstOrderReignsTerminalExposureAt(
                                                game, playerId,
                                                fsMover, location);
                                    objectiveFirstOrderDrainPairDestination =
                                        firstOrderAnalyzer
                                            .advancesFirstOrderReignsDrainPairAt(
                                                game, playerId,
                                                fsMover, location);
                                    objectiveFirstOrderDrainPairBreak =
                                        !terminalAtOrigin
                                        && firstOrderAnalyzer
                                            .breaksFirstOrderReignsDrainPairIfMoved(
                                                game, playerId,
                                                fsMover, location);
                                }
                                MoveDestinationPolicy.Contribution
                                    terminalEscape =
                                        MoveDestinationPolicy
                                            .objectiveTerminalActorEscapeDestination(
                                                objectiveTerminalEscapeDestination,
                                                fsMover.getTitle(), title);
                                if (terminalEscape.applies()) {
                                    action.addReasoning(
                                        terminalEscape.reason(),
                                        terminalEscape.delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.TERMINAL_ACTOR_ESCAPE_DESTINATION"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.BANDED);
                                }
                                MoveDestinationPolicy.Contribution
                                    firstOrderDrainPair =
                                        MoveDestinationPolicy
                                            .objectiveFirstOrderDrainPairDestination(
                                                objectiveFirstOrderDrainPairDestination,
                                                fsMover.getTitle(), title);
                                if (firstOrderDrainPair.applies()) {
                                    action.addReasoning(
                                        firstOrderDrainPair.reason(),
                                        firstOrderDrainPair.delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.BANDED);
                                }
                                MoveDestinationPolicy.Contribution
                                    firstOrderDrainPairHold =
                                        MoveDestinationPolicy
                                            .objectiveFirstOrderDrainPairHold(
                                                objectiveFirstOrderDrainPairBreak,
                                                fsMover.getTitle(), title);
                                if (firstOrderDrainPairHold.applies()) {
                                    action.addReasoning(
                                        firstOrderDrainPairHold.reason(),
                                        firstOrderDrainPairHold.delta(),
                                        TraceRuleId.of(
                                            "MOVE.OBJECTIVE.FIRST_ORDER_DRAIN_PAIR_HOLD"),
                                        TraceDomainId.MOVE,
                                        TraceOutputKind.ORDERING);
                                }
                            } catch (Exception e) {
                                logger.debug(
                                    "First Order objective move-destination assessment failed: {}",
                                    e.getMessage());
                            }
                        }

                        // Get power at destination
                        float ourPower = 0;
                        float theirPower = 0;

                        if (game != null && playerId != null) {
                            String opponentId = gameState.getOpponent(playerId);

                            // Calculate our power at destination
                            for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
                                if (card == null) continue;
                                String owner = card.getOwner();
                                SwccgCardBlueprint bp = card.getBlueprint();
                                if (bp == null || !bp.hasPowerAttribute()) continue;

                                Float power = bp.getPower();
                                if (power == null) continue;

                                if (playerId.equals(owner)) {
                                    ourPower += power;
                                } else if (opponentId != null && opponentId.equals(owner)) {
                                    theirPower += power;
                                }
                            }
                        }

                        // V156 JOIN-GROUP (2026-07-07): friendly presence at this destination.
                        // v156DestFriendlyChars (count) gates the V41 wrong-direction exemption;
                        // v156DestAbilityTotal (STACK-MATH) ranks join destinations by the shared predicate.
                        int v156DestFriendlyChars = 0;
                        float v156DestAbilityTotal = 0f;
                        if (v156JoinMode) {
                            try {
                                for (PhysicalCard c : gameState.getCardsAtLocation(location)) {
                                    if (c == null || !playerId.equals(c.getOwner())) continue;
                                    if (c.getBlueprint() == null) continue;
                                    if (c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                        v156DestFriendlyChars++;
                                    }
                                }
                                v156DestAbilityTotal = com.gempukku.swccgo.ai.models.common.strategy.MovePredicates
                                    .siteAbilityTotal(gameState, location, playerId);
                            } catch (Exception ignore) { /* 0 */ }
                        }

                        // === ICON-BASED SCORING ===
                        SwccgCardBlueprint bp = location.getBlueprint();
                        int myIcons = 0;
                        int theirIcons = 0;

                        if (bp != null) {
                            // Get force icons based on our side
                            int lightIcons = bp.getIconCount(Icon.LIGHT_FORCE);
                            int darkIcons = bp.getIconCount(Icon.DARK_FORCE);

                            if (mySide == Side.LIGHT) {
                                myIcons = lightIcons;
                                theirIcons = darkIcons;
                            } else {
                                myIcons = darkIcons;
                                theirIcons = lightIcons;
                            }

                            MoveDestinationPolicy.IconScoring iconScoring =
                                MoveDestinationPolicy.icons(myIcons, theirIcons);
                            if (iconScoring.opponentIcons().applies()) {
                                action.addReasoning(
                                    iconScoring.opponentIcons().reason(),
                                    iconScoring.opponentIcons().delta());
                                logger.debug("Move dest {}: +{} for {} opponent icons",
                                    title, iconScoring.opponentIcons().delta(), theirIcons);
                            }

                            // Smaller bonus for our icons (force generation)
                            if (iconScoring.ownIcons().applies()) {
                                action.addReasoning(
                                    iconScoring.ownIcons().reason(),
                                    iconScoring.ownIcons().delta());
                            }

                            // Penalty for no icons at all
                            if (iconScoring.noIcons().applies()) {
                                action.addReasoning(
                                    iconScoring.noIcons().reason(),
                                    iconScoring.noIcons().delta());
                            }

                            // === V169 RETREAT BONUS: endangered mover -> safe destination ===
                            // Only fires in retreat mode (mover's current site is outpowered).
                            // +600 dominates the zero-drain penalty (V67g -200) and normal drain
                            // tiebreakers (~+15-48), so the destination step picks a safe site
                            // instead of cancelling out (the cancel-out is what got Asajj killed).
                            MoveDestinationPolicy.Contribution v169Destination =
                                MoveDestinationPolicy.safeRetreatDestination(
                                    v169Retreat, title, theirPower);
                            if (v169Destination.applies()) {
                                action.addReasoning(
                                    v169Destination.reason(),
                                    v169Destination.delta());
                                logger.warn("V169 RETREAT (move dest): {} -> +600 (fleeing {})", title, v169Retreat.originTitle());
                            }

                            // === V156 JOIN-GROUP DEST (2026-07-07, STACK-MATH refit): weak solo → defensible stack ===
                            // Rank join destinations by ABILITY-TOTAL, not headcount: base +250 for any
                            // friendly group, a small ability-total lean (up to +100), and a +150 kicker
                            // when this join makes the stack destiny-capable (dest total + mover ability >= 4),
                            // cap +450. Fel boundary preserved: Citadel Tower (Vader+Tagge+Trooper = ability 11,
                            // already defensible) scores the +450 cap, well above the ~-152 non-join PASS.
                            if (v156JoinMode && v156DestFriendlyChars > 0) {
                                boolean v156Defensible = (v156DestAbilityTotal + v156MoverAbility)
                                    >= com.gempukku.swccgo.ai.models.common.strategy.MovePredicates.DEFENSIBLE_ABILITY;
                                MoveAbilityPolicy.Evaluation v156JoinDestination =
                                    MoveAbilityPolicy.joinDestination(
                                        title, v156DestAbilityTotal,
                                        v156MoverAbility, v156Defensible,
                                        v156FromTitle);
                                action.addReasoning(
                                    v156JoinDestination.reason(),
                                    v156JoinDestination.delta());
                                logger.warn("V156 JOIN-GROUP DEST: {} (stack ability {}->{}) -> +{} (mover from {})",
                                    title, (int) v156DestAbilityTotal, (int) (v156DestAbilityTotal + v156MoverAbility),
                                    (int) v156JoinDestination.delta(), v156FromTitle);
                            }

                            // MoveDrainRoutingPolicy owns V166 drain-contest scoring.
                            if (game != null && playerId != null && theirPower > 0) {
                                try {
                                    String v166Opp = gameState.getOpponent(playerId);
                                    int v166OppDrainHere = (int) game.getModifiersQuerying()
                                        .getForceDrainAmount(gameState, location, v166Opp);
                                    int v166NetDrainBalance = Integer.MIN_VALUE;
                                    int v166OppCards = 0;
                                    if (v166OppDrainHere > 0) {
                                        v166NetDrainBalance = computeNetDrainBalance(
                                            game, gameState, playerId);
                                    }
                                    if (v166NetDrainBalance >= 2) {
                                        for (PhysicalCard c : gameState.getCardsAtLocation(location))
                                            if (c != null && v166Opp.equals(c.getOwner())) v166OppCards++;
                                    }
                                    MoveDrainRoutingPolicy.Contribution v166 =
                                        MoveDrainRoutingPolicy.contestOpponentDrain(
                                            title, theirPower, v166OppDrainHere,
                                            v166NetDrainBalance, v166OppCards);
                                    if (v166.applies()) {
                                        action.addReasoning(v166.reason(), v166.delta());
                                        logger.warn("V166 CONTEST DRAIN: target={} oppDrainHere={} oppCards={} -> +{}",
                                            title, v166OppDrainHere, v166OppCards, (int) v166.delta());
                                    }
                                } catch (Exception e) { logger.debug("V166 error: {}", e.getMessage()); }
                            }

                            // MoveDrainRoutingPolicy owns V67e/V67g destination drain scoring.
                            float v67eExpectedDrain = theirIcons;
                            try {
                                if (game.getModifiersQuerying().isBattleground(gameState, location, null)) {
                                    v67eExpectedDrain *= 1.25f;
                                }
                            } catch (Exception e) { /* ignore */ }
                            boolean v67kIsTransitStagingSite =
                                MoveTransitPolicy.isDrainTransitStagingSite(title);
                            MoveDrainRoutingPolicy.DestinationDrain v67DestinationDrain =
                                MoveDrainRoutingPolicy.destinationDrain(
                                    title, v67eExpectedDrain,
                                    v67kIsTransitStagingSite);
                            action.addReasoning(
                                v67DestinationDrain.contribution().reason(),
                                v67DestinationDrain.contribution().delta());
                            switch (v67DestinationDrain.branch()) {
                                case DRAIN_POTENTIAL:
                                    logger.info("V67e DRAIN POTENTIAL: {} drain={} → +{} (tiebreaker: prefer max drain)",
                                        title, v67eExpectedDrain,
                                        (int) v67DestinationDrain.contribution().delta());
                                    break;
                                case TRANSIT_STAGING:
                                    logger.warn("V67n TRANSIT STAGING DEST: {} → +1500 (dominates other Mapuzo destinations)", title);
                                    break;
                                case ZERO_DRAIN:
                                    logger.warn("V67g ZERO DRAIN: {} no drain — strong penalty (-200)", title);
                                    break;
                            }

                            // Adapter retains V67g decision parsing and physical-card lookup.
                            try {
                                if (v67kIsTransitStagingSite) {
                                    logger.info("V67k MOVE-FROM-DRAIN exempt: {} is transit staging site", title);
                                } else {
                                    String dt = context.getDecisionText() != null
                                        ? context.getDecisionText().toLowerCase(java.util.Locale.ROOT) : "";
                                    boolean isMoveDecision = dt.contains("where to move") || dt.contains("move to,");
                                    if (isMoveDecision && playerId != null) {
                                        String dtForName = context.getDecisionText() != null
                                            ? context.getDecisionText() : "";
                                        java.util.regex.Matcher mvNameMatch = java.util.regex.Pattern.compile(
                                            "value='([^']+)'>").matcher(dtForName);
                                        if (mvNameMatch.find()) {
                                            String mvBp = mvNameMatch.group(1);
                                            for (PhysicalCard cur : gameState.getAllPermanentCards()) {
                                                if (cur == null || cur.getBlueprintId(true) == null) continue;
                                                if (!playerId.equals(cur.getOwner())) continue;
                                                if (!mvBp.equals(cur.getBlueprintId(true))) continue;
                                                PhysicalCard fromLoc = cur.getAtLocation();
                                                if (fromLoc == null || fromLoc == location) break;
                                                SwccgCardBlueprint fromBp = fromLoc.getBlueprint();
                                                if (fromBp == null) break;
                                                int fromTheirIcons = (mySide == Side.LIGHT)
                                                    ? fromBp.getIconCount(Icon.DARK_FORCE)
                                                    : fromBp.getIconCount(Icon.LIGHT_FORCE);
                                                MoveDrainRoutingPolicy.Contribution v67gMoveFromDrain =
                                                    MoveDrainRoutingPolicy.moveFromDrain(
                                                        isMoveDecision,
                                                        v67kIsTransitStagingSite,
                                                        fromLoc.getTitle(),
                                                        fromTheirIcons, title, theirIcons);
                                                if (v67gMoveFromDrain.applies()) {
                                                    action.addReasoning(
                                                        v67gMoveFromDrain.reason(),
                                                        v67gMoveFromDrain.delta());
                                                    logger.warn("V67g MOVE-FROM-DRAIN: leaving {} drain {} for {} drain {} → {}",
                                                        fromLoc.getTitle(), fromTheirIcons, title, theirIcons,
                                                        (int) v67gMoveFromDrain.delta());
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // MoveDestinationPolicy owns V67au retreat-to-drain scoring.
                        try {
                            String dtForRetreat = context.getDecisionText() != null
                                ? context.getDecisionText() : "";
                            java.util.regex.Matcher mvForRetreatMatch = java.util.regex.Pattern.compile(
                                "value='([^']+)'>").matcher(dtForRetreat);
                            if (mvForRetreatMatch.find()) {
                                String retBp = mvForRetreatMatch.group(1);
                                PhysicalCard retFromLoc = null;
                                for (PhysicalCard cur : gameState.getAllPermanentCards()) {
                                    if (cur == null || cur.getBlueprintId(true) == null) continue;
                                    if (!playerId.equals(cur.getOwner())) continue;
                                    if (!retBp.equals(cur.getBlueprintId(true))) continue;
                                    retFromLoc = cur.getAtLocation();
                                    break;
                                }
                                if (retFromLoc != null && retFromLoc != location && game != null) {
                                    String oppId = gameState.getOpponent(playerId);
                                    float fromOppPower = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, retFromLoc, oppId, false, false);
                                    float fromOurPower = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, retFromLoc, playerId, false, false);
                                    boolean fromIsBg = game.getModifiersQuerying()
                                        .isBattleground(gameState, retFromLoc, null);

                                    boolean destIsBg = game.getModifiersQuerying()
                                        .isBattleground(gameState, location, null);
                                    float destOppPower = game.getModifiersQuerying()
                                        .getTotalPowerAtLocation(gameState, location, oppId, false, false);
                                    int destFriendlyDrainIcons = 0;
                                    SwccgCardBlueprint destBp = location.getBlueprint();
                                    if (destBp != null) {
                                        // Friendly drain icons = MY side icons at destination
                                        if (mySide == Side.LIGHT) {
                                            destFriendlyDrainIcons = destBp.getIconCount(Icon.LIGHT_FORCE);
                                        } else {
                                            destFriendlyDrainIcons = destBp.getIconCount(Icon.DARK_FORCE);
                                        }
                                    }

                                    MoveDestinationPolicy.Contribution v67auDecision =
                                        MoveDestinationPolicy.retreatToDrain(
                                            retFromLoc.getTitle(), fromOppPower, fromOurPower,
                                            fromIsBg, title, destOppPower, destIsBg,
                                            destFriendlyDrainIcons);
                                    if (v67auDecision.applies()) {
                                        action.addReasoning(
                                            v67auDecision.reason(), v67auDecision.delta());
                                        logger.warn("V67au RETREAT-TO-DRAIN: from={} (opp {}, ours {}) → to {} (non-BG, empty, {} icons) → +400",
                                            retFromLoc.getTitle(), (int) fromOppPower, (int) fromOurPower,
                                            title, destFriendlyDrainIcons);
                                    }
                                }
                            }
                        } catch (Exception e) { logger.debug("V67au error: {}", e.getMessage()); }

                        // === POWER-BASED SCORING ===
                        MoveDestinationPolicy.Contribution powerScoring =
                            MoveDestinationPolicy.power(
                                ourPower, theirPower, myIcons, theirIcons);
                        action.addReasoning(
                            powerScoring.reason(), powerScoring.delta());

                        // V29.7: Bonus for battleground locations (move preference)
                        // Use real game engine API for accurate battleground detection.
                        // Only penalize non-BG if BG alternatives exist on the table.
                        if (location != null && game != null && gameState != null) {
                            try {
                                boolean isBG = game.getModifiersQuerying()
                                    .isBattleground(gameState, location, null);
                                applyMoveBattlegroundPolicy(action, isBG, false);
                            } catch (Exception e) {
                                // Fallback to old heuristic
                                boolean titleContainsBattleground = bp != null
                                    && title != null
                                    && title.toLowerCase().contains("battleground");
                                applyMoveBattlegroundPolicy(
                                    action, null, titleContainsBattleground);
                            }
                        }

                        // === V24.9: PREFER UNOCCUPIED CC SITES (ESCAPE SPY-BLOCKED LOCATIONS) ===
                        // If the destination is an objective-relevant CC site with no opponent presence,
                        // moving here means we can force drain uncontested. Big bonus.
                        {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer moveObjCheck =
                                context.getObjectiveAnalyzer();
                            boolean bespinPresenceObjective = moveObjCheck != null
                                && moveObjCheck.needsBespinSystemPresence();
                            String destTitle = title != null ? title : "";
                            boolean isObjLoc = bespinPresenceObjective
                                && moveObjCheck.isObjectiveRelevantLocation(destTitle);
                            MoveObjectiveConsolidationPolicy.Contribution cloudCityDestination =
                                MoveObjectiveConsolidationPolicy.cloudCityDestination(
                                    bespinPresenceObjective, isObjLoc,
                                    theirPower, ourPower);
                            if (cloudCityDestination.applies()) {
                                action.addReasoning(
                                    cloudCityDestination.reason(),
                                    cloudCityDestination.delta());
                                if (ourPower == 0.0f) {
                                    logger.info("V24.9: Move dest {} is unoccupied CC — big bonus (+200)", title);
                                }
                            }
                        }

                        // MoveDestinationPolicy owns V64/V65 Hidden Path power safety.
                        {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v64Obj =
                                context.getObjectiveAnalyzer();
                            boolean v64HiddenPath = v64Obj != null && v64Obj.isAnalyzed()
                                && v64Obj.getObjectiveTitle() != null
                                && v64Obj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path");
                            MoveDestinationPolicy.PowerAwareDestination v64Decision =
                                MoveDestinationPolicy.powerAwareHiddenPathDestination(
                                    v64HiddenPath && game != null && gameState != null,
                                    title, theirPower, ourPower);
                            if (v64Decision.contribution().applies()) {
                                action.addReasoning(
                                    v64Decision.contribution().reason(),
                                    v64Decision.contribution().delta());
                                if (v64Decision.disposition()
                                        == MoveDestinationPolicy.PowerAwareDisposition.SUICIDE) {
                                    logger.warn("V64 SUICIDE MOVE: {} enemy={} our projected={} — HARD BLOCKED ({})",
                                        title, (int)theirPower,
                                        (int)v64Decision.projectedOurPower(),
                                        (int)v64Decision.contribution().delta());
                                } else if (v64Decision.disposition()
                                        == MoveDestinationPolicy.PowerAwareDisposition.SAFE_DRAIN) {
                                    logger.info("V64 SAFE DRAIN: {} empty — ideal drain destination (+150)", title);
                                }
                            }
                        }

                        // === V62 HIDDEN PATH SPLIT-SITE ===
                        // Hidden Path flips when we have 2 Jedi Survivors at 2 DIFFERENT
                        // battleground/opponent sites outside Mapuzo. If we've already
                        // placed a Jedi at one non-Mapuzo battleground, the 2nd Jedi must
                        // go to a DIFFERENT battleground to trigger the flip. Moving both
                        // to the same site wastes a turn (no flip progress).
                        // FIXES fmz03bjz79k61img replay: Rando moved Kelleran + Quinlan
                        // from Corridor to the same Malachor: Sith Temple Upper Chamber,
                        // delaying the objective flip.
                        {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v62Obj =
                                context.getObjectiveAnalyzer();
                            boolean onHiddenPath = v62Obj != null && v62Obj.isAnalyzed()
                                && v62Obj.getObjectiveTitle() != null
                                && v62Obj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path")
                                && !v62Obj.isFlipped();
                            if (onHiddenPath && game != null && gameState != null && title != null
                                && !title.toLowerCase(java.util.Locale.ROOT).contains("mapuzo")) {
                                try {
                                    boolean isBGDest = game.getModifiersQuerying().isBattleground(gameState, location, null);
                                    if (isBGDest) {
                                        // Count our OWN Jedi Survivors already at this destination
                                        int ourJediHere = 0;
                                        java.util.List<PhysicalCard> hereCards = gameState.getCardsAtLocation(location);
                                        if (hereCards != null) {
                                            for (PhysicalCard hc : hereCards) {
                                                if (hc != null && playerId.equals(hc.getOwner())
                                                    && hc.getBlueprint() != null
                                                    && hc.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                                                    String hcText = hc.getBlueprint().getGameText();
                                                    String hcTitle = hc.getTitle() != null
                                                        ? hc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                                    boolean isJediSurv = (hcText != null
                                                            && hcText.toLowerCase(java.util.Locale.ROOT).contains("jedi survivor"))
                                                        || hcTitle.contains("obi-wan") || hcTitle.contains("kelleran")
                                                        || hcTitle.contains("quinlan") || hcTitle.contains("ahsoka")
                                                        || hcTitle.contains("cal kestis") || hcTitle.contains("cere");
                                                    if (isJediSurv) ourJediHere++;
                                                }
                                            }
                                        }
                                        MoveObjectiveConsolidationPolicy.Contribution hiddenPathSplit =
                                            MoveObjectiveConsolidationPolicy.hiddenPathSplit(
                                                onHiddenPath, true, isBGDest,
                                                ourJediHere, title);
                                        action.addReasoning(
                                            hiddenPathSplit.reason(),
                                            hiddenPathSplit.delta());
                                        if (ourJediHere >= 1) {
                                            logger.warn("V62 SPLIT SITE: {} has {} friendly Jedi — penalize duplicate dest (-500)",
                                                title, ourJediHere);
                                        } else {
                                            // Empty BG outside Mapuzo — ideal split-site destination
                                            logger.info("V62 SPLIT SITE: {} is ideal split-site for Hidden Path (+200)",
                                                title);
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }
                            }
                        }

                        // === V62 DON'T DILUTE OUR OWN UNDERCOVER SPY ===
                        // Undercover spies block force drains at their location WHILE they
                        // stay undercover. If we move non-spy characters to the same site,
                        // the spy's purpose is wasted — we now occupy openly and can drain
                        // ourselves (if opponent has 0 power), OR if opponent still has
                        // power, the spy is redundant. Better to keep Jedi at safe sites
                        // and let the spy do its solo blocking job.
                        // FIXES fmz03bjz79k61img replay: Rando deployed Boushh as spy at
                        // Sith Temple Entrance (Emperor's location), then moved BOTH Jedi
                        // to the SAME site — making the spy useless.
                        if (game != null && gameState != null && location != null) {
                            try {
                                java.util.List<PhysicalCard> siteCards = gameState.getCardsAtLocation(location);
                                boolean ourSpyHere = false;
                                if (siteCards != null) {
                                    for (PhysicalCard sc : siteCards) {
                                        if (sc != null && playerId.equals(sc.getOwner())
                                            && sc.isUndercover()) {
                                            ourSpyHere = true;
                                            break;
                                        }
                                    }
                                }
                                // Only penalize if the card being moved is NOT itself a spy
                                // (spies moving to other spies is fine; non-spy joining a spy is bad)
                                boolean movingCardIsSpy = false;
                                // The card being moved isn't directly in context at this point,
                                // but since this is a destination-selection decision, it's a move
                                // of a SPECIFIC character chosen in a prior step. We approximate:
                                // check if the decision text mentions a known spy name.
                                String dt = context.getDecisionText() != null
                                    ? context.getDecisionText().toLowerCase(java.util.Locale.ROOT) : "";
                                if (dt.contains("jyn erso") || dt.contains("boushh")
                                    || dt.contains("orrimaarko")) {
                                    movingCardIsSpy = true;
                                }
                                if (ourSpyHere && !movingCardIsSpy) {
                                    // V65: Strengthened from -400 to -1500. Previous -400 was
                                    // getting overridden by +300 V41 CONTEST DEST + +300 contest
                                    // bonus from the spy's enemy presence. -1500 ensures spy
                                    // dilution is a near-hard block when safer alternatives exist.
                                    MoveSpyFollowPolicy.Contribution spyDilution =
                                        MoveSpyFollowPolicy.dilution(
                                            ourSpyHere, movingCardIsSpy, title);
                                    action.addReasoning(
                                        spyDilution.reason(), spyDilution.delta());
                                    logger.warn("V62 SPY DILUTION: {} has our spy — don't dilute (-1500)", title);
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // === V24.13: LANDO ALONE DETECTION — MOVE TO SUPPORT ===
                        // If Lando is the only friendly character at this CC site, big bonus
                        // to move here and protect him. Lando alone = easy kill for opponent.
                        if (game != null && playerId != null) {
                            try {
                                java.util.List<PhysicalCard> destCards = gameState.getCardsAtLocation(location);
                                if (destCards != null) {
                                    boolean landoAlone = false;
                                    int ourCharCount = 0;
                                    for (PhysicalCard c : destCards) {
                                        if (c == null || !playerId.equals(c.getOwner())) continue;
                                        if (c.getBlueprint() == null) continue;
                                        if (c.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                                        ourCharCount++;
                                        String cTitle = c.getTitle();
                                        if (cTitle != null && cTitle.toLowerCase(java.util.Locale.ROOT).contains("lando")) {
                                            landoAlone = true;
                                        }
                                    }
                                    MoveLandoStayPolicy.Contribution landoSupport =
                                        MoveLandoStayPolicy.destinationSupport(
                                            landoAlone, ourCharCount);
                                    if (landoSupport.applies()) {
                                        action.addReasoning(
                                            landoSupport.reason(),
                                            landoSupport.delta());
                                        logger.warn("V24.13 LANDO ALONE AT {}: Moving here to support (+250)", title);
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }
                        }

                        // === V47: LANDO MOVEMENT — STAY AT DINING ROOM ===
                        // Lando should NOT move from Dining Room. He establishes occupation there
                        // and moving wastes force / loses presence. Only move if we have 3+ friendlies
                        // at his current location (he's redundant) and destination is unoccupied CC site.
                        String moveDecisionText = context.getDecisionText() != null
                            ? context.getDecisionText().toLowerCase() : "";
                        if (!exactTdigwattLandoDestination
                                && moveDecisionText.contains("lando")) {
                            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer moveObjAnalyzer =
                                context.getObjectiveAnalyzer();
                            boolean bespinPresenceObjective = moveObjAnalyzer != null
                                && moveObjAnalyzer.needsBespinSystemPresence();
                            MoveLandoStayPolicy.Contribution landoStay =
                                MoveLandoStayPolicy.destinationStay(
                                    true, bespinPresenceObjective);
                            if (landoStay.applies()) {
                                // V47: Block most Lando moves — he stays where he is
                                action.addReasoning(
                                    landoStay.reason(), landoStay.delta());
                                logger.warn("V47 LANDO STAY: Blocking Lando move to {} — stay at current location!", title);
                            }
                        }

                        // === V24.14B: WEAPON CHARACTERS TO SPACE — MOVEMENT PENALTY ===
                        // Characters with "permanent weapon" in game text shouldn't shuttle/move
                        // to system locations (space) — their weapons can't fire there.
                        {
                            boolean destIsSpace = false;
                            if (bp != null) {
                                com.gempukku.swccgo.common.CardSubtype destSubtype = bp.getCardSubtype();
                                destIsSpace = (destSubtype == com.gempukku.swccgo.common.CardSubtype.SYSTEM);
                            }
                            if (destIsSpace) {
                                // Check if the character being moved has a permanent weapon
                                boolean movingCharHasWeapon = false;
                                // Check decision text for weapon keywords in card name (fallback)
                                if (moveDecisionText.contains("lightsaber") || moveDecisionText.contains("blaster")
                                    || moveDecisionText.contains("with rifle") || moveDecisionText.contains("with cannon")) {
                                    movingCharHasWeapon = true;
                                }
                                // Check blueprint game text for "permanent weapon" (universal)
                                if (!movingCharHasWeapon) {
                                    // Try to extract the moving card's blueprint from decision text
                                    String moveBpId = extractBlueprintFromDecisionText(context.getDecisionText());
                                    if (moveBpId != null) {
                                        try {
                                            SwccgCardBlueprint moveBp = getBlueprintFromId(context, moveBpId);
                                            if (moveBp != null) {
                                                String moveGameText = moveBp.getGameText();
                                                if (moveGameText != null &&
                                                    moveGameText.toLowerCase(java.util.Locale.ROOT).contains("permanent weapon")) {
                                                    movingCharHasWeapon = true;
                                                }
                                            }
                                        } catch (Exception e) {
                                            logger.debug("V24.14B: Error checking move blueprint: {}", e.getMessage());
                                        }
                                    }
                                }
                                boolean movingVehicle = moveDecisionText.contains("vehicle");
                                MoveTransitPolicy.SpaceDestinationPenalties spaceDestination =
                                    MoveTransitPolicy.spaceDestination(
                                        destIsSpace, movingCharHasWeapon,
                                        movingVehicle);
                                if (spaceDestination.permanentWeaponCharacter().applies()) {
                                    action.addReasoning(
                                        spaceDestination.permanentWeaponCharacter().reason(),
                                        spaceDestination.permanentWeaponCharacter().delta());
                                    logger.warn("V24.14B WEAPON MOVE: Char with permanent weapon moving to space {} — penalized (-300)", title);
                                }
                                // Also penalize vehicles moving to space
                                if (spaceDestination.vehicle().applies()) {
                                    action.addReasoning(
                                        spaceDestination.vehicle().reason(),
                                        spaceDestination.vehicle().delta());
                                    logger.warn("V24.14B VEHICLE MOVE: Vehicle moving to space {} — penalized (-300)", title);
                                }
                            }
                        }

                        // Policy owns V41/V65/V67aa/V67f2 destination scoring and exemptions.
                        if (game != null && playerId != null) {
                            try {
                                String opponentId = gameState.getOpponent(playerId);

                                // V67f2: Recompute opponent power EXCLUDING undercover spies.
                                float v67fNonSpyOpponentPower = 0;
                                int v67fSpiesHere = 0;
                                try {
                                    java.util.List<PhysicalCard> hereCards =
                                        gameState.getCardsAtLocation(location);
                                    if (hereCards != null) {
                                        for (PhysicalCard hc : hereCards) {
                                            if (hc == null) continue;
                                            if (!opponentId.equals(hc.getOwner())) continue;
                                            if (hc.isUndercover()) {
                                                v67fSpiesHere++;
                                                continue;
                                            }
                                            SwccgCardBlueprint hcBp = hc.getBlueprint();
                                            if (hcBp != null && hcBp.hasPowerAttribute()) {
                                                Float p = hcBp.getPower();
                                                if (p != null) v67fNonSpyOpponentPower += p;
                                            }
                                        }
                                    }
                                } catch (Exception e) { /* ignore */ }

                                // Adapter retains objective reads and V67aa's terminal continue.
                                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v67aaObj =
                                    context.getObjectiveAnalyzer();
                                boolean v67aaOnHiddenPath = v67aaObj != null && v67aaObj.isAnalyzed()
                                    && v67aaObj.getObjectiveTitle() != null
                                    && v67aaObj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path")
                                    && !v67aaObj.isFlipped();
                                MoveDestinationPolicy.Contribution v67aaDecision =
                                    MoveDestinationPolicy.hiddenPathPreFlipSuicide(
                                        v67aaOnHiddenPath, title,
                                        v67fNonSpyOpponentPower, ourPower);
                                if (v67aaDecision.applies()) {
                                    action.addReasoning(
                                        v67aaDecision.reason(), v67aaDecision.delta());
                                    logger.warn("V67aa SUICIDE BLOCK: {} opp={} our=0 on Hidden Path pre-flip — BLOCK transit (-9999)",
                                        title, v67fNonSpyOpponentPower);
                                    // Skip V41 CONTEST DEST bonus — we already hard-blocked
                                    actions.add(action);
                                    continue;
                                }

                                boolean jediAtDest = false;
                                if (v67fNonSpyOpponentPower > 0) {
                                    for (PhysicalCard c : gameState.getCardsAtLocation(location)) {
                                        if (c == null || playerId.equals(c.getOwner())) continue;
                                        String cTitle = c.getTitle() != null ? c.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                        if (ActionEvaluator.isJediOrPadawan(cTitle)) {
                                            jediAtDest = true;
                                            break;
                                        }
                                    }
                                }

                                MoveDestinationPolicy.SpyAwareContest v41Contest =
                                    MoveDestinationPolicy.spyAwareContest(
                                        title, v67fNonSpyOpponentPower, v67fSpiesHere,
                                        ourPower, jediAtDest);
                                if (v41Contest.disposition()
                                        == MoveDestinationPolicy.ContestDisposition.CONTEST) {
                                    if (ourPower == 0) {
                                        logger.warn("V41 MOVE DEST CONTEST: {} is UNCONTESTED by us — urgent! (+500)", title);
                                    }
                                    if (jediAtDest) {
                                        logger.warn("V41 HUNT JEDI DEST: Jedi at {} — Vader must go here! (+{})",
                                            title, (int)v41Contest.contribution().delta());
                                    }
                                    action.addReasoning(
                                        v41Contest.contribution().reason(),
                                        v41Contest.contribution().delta());
                                } else if (v41Contest.disposition()
                                        == MoveDestinationPolicy.ContestDisposition.SPY_ONLY) {
                                    action.addReasoning(
                                        v41Contest.contribution().reason(),
                                        v41Contest.contribution().delta());
                                    logger.warn("V67f SPY-ONLY: {} has only opp spies (no real characters) — penalize move-in (-100)", title);
                                } else {
                                    // Adapter retains board scans; policy owns V65 threat classification.
                                    boolean opponentsElsewhere = false;
                                    String worstDrainLoc = null;
                                    float worstDrainPower = 0;
                                    for (PhysicalCard otherLoc : gameState.getTopLocations()) {
                                        if (otherLoc == null || otherLoc == location) continue;
                                        float oppPower = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, otherLoc, opponentId, false, false);
                                        float ourPowerThere = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, otherLoc, playerId, false, false);
                                        if (oppPower > 0 && ourPowerThere == 0) {
                                            // V65a: Our spy at the drain location blocks it. Skip.
                                            boolean ourSpyBlocksIt = false;
                                            try {
                                                java.util.List<PhysicalCard> cardsAtOther = gameState.getCardsAtLocation(otherLoc);
                                                if (cardsAtOther != null) {
                                                    for (PhysicalCard osc : cardsAtOther) {
                                                        if (osc != null && playerId.equals(osc.getOwner())
                                                            && osc.isUndercover()) {
                                                            ourSpyBlocksIt = true;
                                                            break;
                                                        }
                                                    }
                                                }
                                            } catch (Exception e) { /* ignore */ }
                                            MoveDestinationPolicy.DrainThreatDisposition v65Threat =
                                                MoveDestinationPolicy.drainThreat(
                                                    oppPower, ourPowerThere, ourSpyBlocksIt);
                                            if (v65Threat
                                                    == MoveDestinationPolicy.DrainThreatDisposition.SPY_NEUTRALIZED) {
                                                logger.info("V65a SPY-NEUTRALIZED: Not marking {} as wrong-direction — our spy blocks {} drain",
                                                    title, otherLoc.getTitle());
                                                continue;
                                            } else if (v65Threat
                                                    == MoveDestinationPolicy.DrainThreatDisposition.TOO_DANGEROUS) {
                                                logger.info("V65b SUICIDE-WRONG-DIR: Not marking {} as wrong-direction — {} has enemy power {} (suicide for Jedi)",
                                                    title, otherLoc.getTitle(), (int)oppPower);
                                                continue;
                                            } else if (v65Threat
                                                    == MoveDestinationPolicy.DrainThreatDisposition.ACTIVE) {
                                                opponentsElsewhere = true;
                                            }
                                            if (v65Threat
                                                    == MoveDestinationPolicy.DrainThreatDisposition.ACTIVE
                                                    && oppPower > worstDrainPower) {
                                                worstDrainPower = oppPower;
                                                worstDrainLoc = otherLoc.getTitle();
                                            }
                                        }
                                    }
                                    if (opponentsElsewhere) {
                                        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer v67zObj =
                                            context.getObjectiveAnalyzer();
                                        boolean v67zOnHiddenPath = v67zObj != null && v67zObj.isAnalyzed()
                                            && v67zObj.getObjectiveTitle() != null
                                            && v67zObj.getObjectiveTitle().toLowerCase(java.util.Locale.ROOT).contains("hidden path")
                                            && !v67zObj.isFlipped();
                                        boolean v67zNonMapuzoBG = false;
                                        if (v67zOnHiddenPath && title != null
                                                && !title.toLowerCase(java.util.Locale.ROOT).contains("mapuzo")) {
                                            try {
                                                v67zNonMapuzoBG = game.getModifiersQuerying()
                                                    .isBattleground(gameState, location, null);
                                            } catch (Exception e) { /* ignore */ }
                                        }
                                        boolean v67zTransitHub = v67zOnHiddenPath && title != null
                                                && title.toLowerCase(java.util.Locale.ROOT).contains("underground corridor");

                                        MoveDestinationPolicy.WrongDirectionEvaluation v41Direction =
                                            MoveDestinationPolicy.wrongDirection(
                                                opponentsElsewhere, title, worstDrainLoc,
                                                v67zNonMapuzoBG || v67zTransitHub,
                                                MoveDestinationPolicy.retreatExemptsWrongDirection(v169Retreat),
                                                v156JoinMode && v156DestFriendlyChars > 0,
                                                objectiveActorRouteDestination
                                                    || objectiveActorLocationDestination
                                                    || objectiveFirstOrderDrainPairDestination
                                                    || objectiveTerminalEscapeDestination
                                                    || objectiveNabooDuelFrontRouteDestination
                                                    || exactTdigwattLandoDestination
                                                    || objectivePostFlipPayoffDestination
                                                        != com.gempukku.swccgo.ai.models
                                                            .common.strategy
                                                            .ObjectiveAnalyzer
                                                            .ObjectivePostFlipPayoffRole.NONE);
                                        if (v41Direction.disposition()
                                                == MoveDestinationPolicy.WrongDirectionDisposition.HIDDEN_PATH_EXEMPT) {
                                            logger.info("V67z HIDDEN PATH {} EXEMPT: {} on Hidden Path — V41 WRONG DIRECTION skipped",
                                                v67zTransitHub ? "TRANSIT-HUB" : "SPLIT", title);
                                        } else if (v41Direction.disposition()
                                                == MoveDestinationPolicy.WrongDirectionDisposition.RETREAT_EXEMPT) {
                                            logger.warn("V169 RETREAT EXEMPT: {} — V41 wrong-direction skipped (mover fleeing {})",
                                                title, v169Retreat.originTitle());
                                        } else if (v41Direction.disposition()
                                                == MoveDestinationPolicy.WrongDirectionDisposition.JOIN_GROUP_EXEMPT) {
                                            logger.warn("V156 JOIN-GROUP EXEMPT: {} has {} friendly character(s) — V41 wrong-direction skipped (weak solo joining from {})",
                                                title, v156DestFriendlyChars, v156FromTitle);
                                        } else if (v41Direction.disposition()
                                                == MoveDestinationPolicy.WrongDirectionDisposition.OBJECTIVE_ROUTE_EXEMPT) {
                                            logger.warn("OBJECTIVE ACTOR ROUTE EXEMPT: {} advances the exact flip gate — V41 wrong-direction skipped",
                                                title);
                                        } else if (v41Direction.disposition()
                                                == MoveDestinationPolicy.WrongDirectionDisposition.VETO) {
                                            action.addReasoning(
                                                v41Direction.contribution().reason(),
                                                v41Direction.contribution().delta());
                                            logger.warn("V41 WRONG DIRECTION: {} is empty, opponents at {} — BLOCKED", title, worstDrainLoc);
                                        }
                                    }
                                }

                                if (MoveDestinationPolicy.isCastleDestination(title)) {
                                    boolean anyOpponents = false;
                                    for (PhysicalCard otherLoc : gameState.getTopLocations()) {
                                        if (otherLoc == null) continue;
                                        float op = game.getModifiersQuerying().getTotalPowerAtLocation(
                                            gameState, otherLoc, opponentId, false, false);
                                        if (op > 0) { anyOpponents = true; break; }
                                    }
                                    MoveDestinationPolicy.Contribution v41Castle =
                                        MoveDestinationPolicy.castleRetreat(
                                            title, anyOpponents);
                                    if (v41Castle.applies()) {
                                        action.addReasoning(
                                            v41Castle.reason(), v41Castle.delta());
                                        logger.warn("V41 CASTLE RETREAT BLOCKED in move destination selection");
                                    }
                                }
                            } catch (Exception e) {
                                logger.debug("V41 MOVE DEST: Error: {}", e.getMessage());
                            }
                        }

                        // === V24.3C: DR. EVAZAN WEAPON COMBO — MOVEMENT PREFERENCE ===
                        // Move Evazan toward weapon characters, and weapon chars toward Evazan.
                        boolean movingEvazan = moveDecisionText.contains("evazan");
                        boolean movingWeaponChar = (moveDecisionText.contains("maul") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("vader") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("mara") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("jade") && moveDecisionText.contains("lightsaber"))
                            || (moveDecisionText.contains("aurra") && moveDecisionText.contains("blaster"))
                            || (moveDecisionText.contains("sing") && moveDecisionText.contains("blaster"));

                        if (movingEvazan || movingWeaponChar) {
                            boolean comboPartnerAtDest = false;
                            try {
                                java.util.List<PhysicalCard> destCards = gameState.getCardsAtLocation(location);
                                if (destCards != null) {
                                    for (PhysicalCard c : destCards) {
                                        if (c == null || !playerId.equals(c.getOwner())) continue;
                                        String cTitle = c.getTitle();
                                        if (cTitle == null) continue;
                                        String cLower = cTitle.toLowerCase();

                                        if (movingEvazan) {
                                            if ((cLower.contains("maul") && cLower.contains("lightsaber"))
                                                || (cLower.contains("vader") && cLower.contains("lightsaber"))
                                                || (cLower.contains("mara") && cLower.contains("lightsaber"))
                                                || (cLower.contains("jade") && cLower.contains("lightsaber"))
                                                || (cLower.contains("aurra") && cLower.contains("blaster"))
                                                || (cLower.contains("sing") && cLower.contains("blaster"))) {
                                                comboPartnerAtDest = true;
                                                break;
                                            }
                                        } else {
                                            if (cLower.contains("evazan")) {
                                                comboPartnerAtDest = true;
                                                break;
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) { /* ignore */ }

                            MoveDestinationPolicy.Contribution evazanCombo =
                                MoveDestinationPolicy.evazanCombo(
                                    movingEvazan, movingWeaponChar,
                                    comboPartnerAtDest);
                            if (evazanCombo.applies()) {
                                action.addReasoning(
                                    evazanCombo.reason(), evazanCombo.delta());
                                logger.warn("V24.3 EVAZAN COMBO MOVE: Partner found at {} (+200)", title);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    logger.debug("Could not parse cardId for move destination: {}", cardId);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    private List<EvaluatedAction>
            evaluateObjectiveEmbarkTarget(
                    DecisionContext context) {
        List<EvaluatedAction> actions =
                new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        String playerId = context.getPlayerId();
        PhysicalCard embarker = null;
        Integer exactCardId =
                movePhysicalCardId(context);
        if (gameState != null && exactCardId != null) {
            embarker = gameState.findCardById(
                    exactCardId);
        }
        if (embarker == null && gameState != null) {
            String blueprintId =
                    extractBlueprintFromDecisionText(
                        context.getDecisionText());
            MovePhysicalCardResolver.ResolvedMover resolved =
                    MovePhysicalCardResolver.resolveOnTable(
                        gameState.getAllPermanentCards(),
                        playerId, blueprintId);
            embarker = resolved != null
                    ? resolved.card() : null;
        }
        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                    cardId, ActionType.MOVE, 0.0f,
                    "Choose embark target");
            PhysicalCard target = null;
            try {
                target = gameState != null
                        ? gameState.findCardById(
                            Integer.parseInt(cardId))
                        : null;
            } catch (NumberFormatException ignored) {
            }
            boolean objectiveTarget =
                    game != null && embarker != null
                    && target != null
                    && context.getObjectiveAnalyzer() != null
                    && context.getObjectiveAnalyzer()
                        .isAnalyzed()
                    && context.getObjectiveAnalyzer()
                        .advancesRequiredCardDeployPrerequisiteAt(
                            game, playerId, embarker,
                            target);
            applyCaptureMoveDestination(
                    context, action, embarker, target);
            if (objectiveTarget) {
                action.addReasoning(
                    "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_EMBARK_TARGET: board "
                        + target.getTitle()
                        + " to create the required deploy actor",
                    1000.0f,
                    TraceRuleId.of(
                        "MOVE.OBJECTIVE.REQUIRED_CARD_ENABLER_EMBARK_TARGET"),
                    TraceDomainId.MOVE,
                    TraceOutputKind.ORDERING);
            }
            actions.add(action);
        }
        return actions;
    }

    private void applyMoveBattlegroundPolicy(
            EvaluatedAction action,
            Boolean engineBattleground,
            boolean titleContainsBattleground) {
        MoveDestinationPolicy.Contribution battleground =
            MoveDestinationPolicy.battleground(
                engineBattleground, titleContainsBattleground);
        if (battleground.applies()) {
            action.addReasoning(
                battleground.reason(), battleground.delta());
        }
    }

    /**
     * Choose card to cancel - cancel opponent's cards.
     */
    private List<EvaluatedAction> evaluateCancelSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Cancel card " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        String owner = card.getOwner();

                        // Cancel opponent's cards, not ours!
                        applyResponsePolicy(action, ResponsePolicy.scoreCancelSelection(
                                cardId, true, !playerId.equals(owner)));
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Check if the decision text indicates we're playing a beneficial card
     * that should target our OWN cards (not opponent's).
     *
     * Examples:
     * - A Few Maneuvers (adds +2 hyperspeed and maneuver to your starship)
     * - Hyper Escape (allows your ship to escape)
     * - Various buff/enhancement cards
     */
    private boolean isBeneficialTargetingCard(String decisionText) {
        if (decisionText == null) return false;
        String textLower = decisionText.toLowerCase();

        // Cards that buff your own cards
        String[] beneficialCards = {
            "a few maneuvers",      // +2 hyperspeed and maneuver
            "hyper escape",         // Escape action
            "evasive action",       // Escape/dodge
            "rebel barrier",        // Defense
            "narrow escape",        // Escape
            "darklighter spin",     // Combat bonus
            "hear me baby",         // Buff
            "all power to weapons", // Attack buff
            "full throttle",        // Speed buff
            "punch it",             // Speed/escape
            "alert my star destroyer", // Defense buff
            "i have you now"        // Attack buff (targets your TIE)
        };

        for (String card : beneficialCards) {
            if (textLower.contains(card)) {
                logger.info("🎯 Detected beneficial card '{}' - targeting own cards", card);
                return true;
            }
        }

        return false;
    }

    /**
     * Target selection for weapons/abilities - must select, don't cancel.
     *
     * IMPORTANT: Some cards target your OWN cards (beneficial buffs like A Few Maneuvers)
     * while others target OPPONENT cards (weapons, disruptions). We detect this from
     * the decision text which shows what card is being played.
     */
    private List<EvaluatedAction> evaluateTargetSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        String decisionText = context.getDecisionText();
        String targetDecisionId = context.getDecisionId();

        // Target The Main Generator's child chooses our weapon source. It is not
        // a harmful target selection, despite offering our attached cannons.
        boolean shieldGeneratorCannonChoice = decisionText != null
            && "choose at-at cannon, or click 'done' to cancel"
                .equals(decisionText.trim().toLowerCase(Locale.ROOT))
            && context.getObjectiveAnalyzer() != null
            && context.getObjectiveAnalyzer()
                .hasShieldMainGeneratorFormationRule();
        boolean targetOwnCards = isBeneficialTargetingCard(decisionText)
            || shieldGeneratorCannonChoice;

        for (String cardId : context.getCardIds()) {
            TargetSelectionPolicy.InitialScore initial =
                TargetSelectionPolicy.initialScore(cardId);
            EvaluatedAction action = new EvaluatedAction(
                initial.actionId(),
                ActionType.UNKNOWN,
                initial.score(),
                initial.displayText(),
                initial.ruleArmId(),
                initial.domainId(),
                initial.outputKind(),
                null);
            PolicyContributionLedger targetLedger = new PolicyContributionLedger(
                targetDecisionId == null || targetDecisionId.isBlank()
                    ? "target-selection-candidate-" + actions.size()
                    : targetDecisionId + "-target-candidate-" + actions.size());

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        String owner = card.getOwner();
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        boolean isOurCard = playerId.equals(owner);
                        TargetSelectionFacts.Intent intent = targetOwnCards
                            ? TargetSelectionFacts.Intent.BENEFICIAL
                            : TargetSelectionFacts.Intent.HARMFUL;
                        TargetSelectionFacts.Ownership ownership = isOurCard
                            ? TargetSelectionFacts.Ownership.OWN
                            : TargetSelectionFacts.Ownership.OPPONENT;
                        targetLedger.register(TargetSelectionPolicy.scoreOwnership(
                            new TargetSelectionFacts.OwnershipFacts(
                                cardId, intent, ownership)));

                        if (targetOwnCards) {
                            // Beneficial card - target OUR cards, not opponent's
                            if (isOurCard) {
                                // Prefer high-value targets for buffs
                                if (blueprint != null) {
                                    boolean highPower = false;
                                    if (blueprint.hasPowerAttribute()) {
                                        Float power = blueprint.getPower();
                                        if (power != null && power >= 5) {
                                            highPower = true;
                                        }
                                    }
                                    targetLedger.register(TargetSelectionPolicy.scoreValue(
                                        new TargetSelectionFacts.ValueFacts(
                                            cardId, intent, ownership, false, highPower,
                                            blueprint.getUniqueness() == Uniqueness.UNIQUE)));
                                }
                            }
                        } else {
                            // Harmful card (weapon, etc.) - target OPPONENT cards
                            if (!isOurCard) {
                                // V51: Don't waste weapons on already-hit characters
                                if (card.isHit()) {
                                    targetLedger.register(BattleWeaponsPolicy.scoreTarget(
                                        new BattleWeaponsFacts.TargetFacts(
                                            cardId, card.getTitle(), true,
                                            BattleWeaponsFacts.DestinyAssessment.unavailable(),
                                            false, false, false, false)));
                                    logger.warn("V51 ALREADY HIT: Weapon targeting {} but already hit — -500", card.getTitle());
                                }

                                // V51: Force Lightning / Trample — prioritize opponent spies
                                boolean undercover = card.isUndercover();
                                targetLedger.register(TargetSelectionPolicy.scoreUndercover(
                                    new TargetSelectionFacts.UndercoverFacts(
                                        cardId, intent, ownership, undercover)));
                                if (undercover) {
                                    logger.warn("V51 KILL SPY: Targeting spy {} — +500!", card.getTitle());
                                }

                                if (blueprint != null) {
                                    boolean highPower = false;
                                    // === V36: DESTINY-BASED WEAPON TARGETING ===
                                    // Calculate hit probability: avgDestiny * numDraws vs defense value
                                    // Lightsaber draws 2 destiny. Other weapons draw 1-2.
                                    // Only fire at targets we can actually hit!
                                    SwccgGame targetGame = context.getGame();
                                    boolean battleMode = targetGame != null && gameState != null && context.getPhase() == Phase.BATTLE;
                                    if (battleMode) {
                                        try {
                                            // Get target's defense value (ability for characters)
                                            float defenseValue = targetGame.getModifiersQuerying().getDefenseValue(gameState, card);

                                            // Get average destiny in reserve deck
                                            com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle destOracle = context.getDeckOracle();
                                            double avgDestiny = 3.0; // fallback
                                            if (destOracle != null && destOracle.isAnalyzed()) {
                                                avgDestiny = destOracle.getAverageDestinyInReserve();
                                            }

                                            // Lightsaber draws 2 destiny, most other weapons draw 1
                                            int numDraws = 2; // assume lightsaber
                                            float expectedTotal = (float)(avgDestiny * numDraws);
                                            float hitMargin = expectedTotal - defenseValue;

                                            String targetTitle = card.getTitle() != null ? card.getTitle() : "?";
                                            String targetLower = targetTitle.toLowerCase(java.util.Locale.ROOT);

                                            targetLedger.register(BattleWeaponsPolicy.scoreTarget(
                                                new BattleWeaponsFacts.TargetFacts(
                                                    cardId,
                                                    targetTitle,
                                                    false,
                                                    BattleWeaponsFacts.DestinyAssessment.available(
                                                        defenseValue, expectedTotal),
                                                    targetLower.contains("padme") || targetLower.contains("naberrie"),
                                                    targetLower.contains("lando") || targetLower.contains("boba fett")
                                                        || targetLower.contains("wedge") || targetLower.contains("chewie"),
                                                    isJediOrPadawan(targetLower),
                                                    false)));

                                            if (hitMargin < 0.0f) {
                                                logger.warn("V36 WEAPON TARGET: {} defense {} vs expected {} — LIKELY MISS",
                                                    targetTitle, (int)defenseValue, String.format("%.1f", expectedTotal));
                                            }

                                        } catch (Exception e) {
                                            logger.debug("V36 WEAPON TARGET: Error calculating hit probability: {}", e.getMessage());
                                        }
                                    } else {
                                        // Not in battle — basic targeting
                                        if (blueprint.hasPowerAttribute()) {
                                            Float power = blueprint.getPower();
                                            if (power != null && power >= 5) {
                                                highPower = true;
                                            }
                                        }
                                    }
                                    targetLedger.register(TargetSelectionPolicy.scoreValue(
                                        new TargetSelectionFacts.ValueFacts(
                                            cardId, intent, ownership, battleMode, highPower,
                                            blueprint.getUniqueness() == Uniqueness.UNIQUE)));
                                }
                            } else {
                                // V38.3: HARD BLOCK targeting own cards with harmful effects!
                                // Force Lightning on own Vader, weapon fire at own characters, etc.
                                // -200 wasn't enough — other bonuses could override it.
                                targetLedger.register(BattleWeaponsPolicy.scoreTarget(
                                    new BattleWeaponsFacts.TargetFacts(
                                        cardId, card.getTitle(), false,
                                        BattleWeaponsFacts.DestinyAssessment.unavailable(),
                                        false, false, false, true)));
                                logger.warn("V38.3 SELF-TARGET BLOCKED: Harmful effect targeting own card!");
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            PolicyOperationAdapter.apply(action, targetLedger);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Location selection - pick battlegrounds and force icon locations.
     */
    private List<EvaluatedAction> evaluateLocationSelection(DecisionContext context) {
        return evaluateDeployLocation(context);  // Same logic
    }

    /**
     * V43: Starting interrupt selection.
     * Prefer interrupts that deploy the Epic Event ("Force Is Strong In My Family")
     * over generic starting interrupts like "The Signal".
     */
    private List<EvaluatedAction> evaluateStartingInterrupt(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<String> cardIds = context.getCardIds();
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());
        GameState gameState = context.getGameState();

        logger.warn("V43 STARTING INTERRUPT: Evaluating {} choices", cardIds != null ? cardIds.size() : 0);

        if (cardIds == null) return actions;

        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            EvaluatedAction action = new EvaluatedAction(cardId, ActionType.UNKNOWN, 50.0f, "Starting interrupt candidate");

            try {
                SwccgCardBlueprint blueprint = null;
                String title = "?";

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    blueprint = getBlueprintFromId(context, blueprintIds.get(idx));
                } else if (gameState != null) {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) blueprint = card.getBlueprint();
                }

                if (blueprint != null) {
                    title = blueprint.getTitle() != null ? blueprint.getTitle() : "?";
                    applySetupContribution(
                            action,
                            SetupPolicy.startingInterrupt(blueprint.getGameText()));
                    logger.warn("V43 STARTING INTERRUPT: {} scored by shared SETUP policy", title);
                }
            } catch (Exception e) {
                logger.debug("V43 STARTING INTERRUPT: Error evaluating card {}: {}", cardId, e.getMessage());
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Starting location selection.
     * V22: Objective-aware + bonus for locations that pull from reserve deck.
     * Base +50 only if the location is mentioned in the starting interrupt's text.
     */
    private List<EvaluatedAction> evaluateStartingLocation(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer startLocObjAnalyzer =
            context.getObjectiveAnalyzer();

        // V22: Get the decision text which should reference the starting interrupt
        String decisionText = context.getDecisionText();
        String decisionTextLower = decisionText != null ? decisionText.toLowerCase(java.util.Locale.ROOT) : "";

        // V28: Get blueprint IDs for ARBITRARY_CARDS decisions (temp IDs can't be parsed as ints)
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());

        List<String> cardIds = context.getCardIds();
        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                10.0f,
                "Starting location " + cardId
            );

            // V22/V28: Look up the card to check game text and title
            // V28: For ARBITRARY_CARDS, card IDs are "temp0" etc. — use blueprint lookup instead.
            try {
                String locTitle = null;
                String locTitleLower = "";
                SwccgCardBlueprint locBp = null;

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    // V28: ARBITRARY_CARDS path — look up by blueprint ID
                    String bpId = blueprintIds.get(idx);
                    locBp = getBlueprintFromId(context, bpId);
                    if (locBp != null) {
                        locTitle = locBp.getTitle();
                        locTitleLower = locTitle != null ? locTitle.toLowerCase(java.util.Locale.ROOT) : "";
                        logger.warn("V28 ARBITRARY_CARDS: Resolved card '{}' via blueprint '{}' → '{}'", cardId, bpId, locTitle);
                    } else {
                        logger.warn("V28 ARBITRARY_CARDS: Could not resolve blueprint '{}' for card '{}'", bpId, cardId);
                    }
                } else if (gameState != null) {
                    // Standard path — look up by integer card ID
                    PhysicalCard locCard = gameState.findCardById(Integer.parseInt(cardId));
                    if (locCard != null) {
                        locTitle = locCard.getTitle();
                        locTitleLower = locTitle != null ? locTitle.toLowerCase(java.util.Locale.ROOT) : "";
                        locBp = locCard.getBlueprint();
                    }
                }

                if (locTitle != null && locBp != null) {
                    applySetupContribution(
                            action,
                            SetupPolicy.startingLocationMention(
                                    locTitle, decisionTextLower));

                    if (startLocObjAnalyzer != null && startLocObjAnalyzer.isAnalyzed()) {
                        boolean objectiveRelevant =
                                startLocObjAnalyzer.isObjectiveRelevantLocation(locTitle);
                        if (objectiveRelevant) {
                            applySetupContribution(
                                    action,
                                    SetupPolicy.startingLocationObjective(
                                            locTitle,
                                            true,
                                            startLocObjAnalyzer.getLocationObjectiveBonus(locTitle)));
                        }
                        int matchingOperatives = startLocObjAnalyzer
                                .countMatchingOperativesAvailableForSetupSystem(
                                    context.getGame(),
                                    context.getPlayerId(), locBp);
                        if (matchingOperatives > 0) {
                            float packageScore = matchingOperatives * 500.0f;
                            action.addReasoning(
                                "OPERATIVE OBJECTIVE SETUP: "
                                    + matchingOperatives
                                    + " matching operatives support "
                                    + locTitle,
                                packageScore);
                            logger.warn("OPERATIVE OBJECTIVE SETUP: {} has {} matching operative(s), +{}",
                                locTitle, matchingOperatives,
                                (int) packageScore);
                        }
                        if (isArbitrary
                                && "choose site to deploy".equals(
                                    decisionTextLower.trim())
                                && blueprintIds != null
                                && idx < blueprintIds.size()
                                && startLocObjAnalyzer
                                    .isCountedOperativeSetupSiteRouteCandidate(
                                        context.getGame(),
                                        context.getPlayerId(),
                                        blueprintIds.get(idx))) {
                            action.addReasoning(
                                "OBJECTIVE.COUNTED_OPERATIVE.SETUP_SITE_ROUTE",
                                2000.0f);
                        }
                    }

                    if (locTitleLower.contains("cloud city")) {
                        boolean isExterior = locBp.hasIcon(com.gempukku.swccgo.common.Icon.EXTERIOR_SITE);
                        boolean isInterior = locBp.hasIcon(com.gempukku.swccgo.common.Icon.INTERIOR_SITE);
                        applySetupContribution(
                                action,
                                SetupPolicy.startingLocationCloudCity(
                                        locTitle, isExterior, isInterior));
                    }

                    applySetupContributions(
                            action,
                            SetupPolicy.startingLocationText(
                                    locTitle,
                                    SetupFactsReader.allLocationText(locBp)));

                    // === V67o BATTLEGROUND STARTING LOCATION ===
                    // Steve's rule: starting location should be a BATTLEGROUND so force
                    // drains and battles can happen there from turn 1. Without this rule
                    // Rando picks non-battleground sites (e.g., Dooku deck starts at a
                    // non-BG site) and loses tempo from turn 1.
                    //
                    // Detection heuristic (matches V29.6 in the deploy path):
                    //   1. Game text contains "battleground"
                    //   2. Title contains "battleground"
                    //   3. Site has BOTH Light Force AND Dark Force icons
                    //      (most battlegrounds have both — drainable + drainable-against)
                    //
                    // Score: +300 for battleground, -150 for non-battleground.
                    // Below Funeral Pyre/Epic Event (+1000) and CC Exterior (+500) so
                    // those specific overrides still win; above Force Gen (+25), Reserve
                    // Pull (+75), and Mention-in-Interrupt (+50) so battleground wins
                    // when no specific override applies.
                    String v67oGt = locBp.getGameText();
                    boolean v67oLightForce = false;
                    boolean v67oDarkForce = false;
                    if ((v67oGt == null || !v67oGt.toLowerCase(java.util.Locale.ROOT).contains("battleground"))
                            && !locTitleLower.contains("battleground")) {
                        try {
                            v67oLightForce = locBp.hasIcon(com.gempukku.swccgo.common.Icon.LIGHT_FORCE);
                            if (v67oLightForce) {
                                v67oDarkForce = locBp.hasIcon(com.gempukku.swccgo.common.Icon.DARK_FORCE);
                            }
                        } catch (Exception e) { /* ignore */ }
                    }
                    SetupPolicy.BattlegroundEvaluation v67o =
                            SetupPolicy.startingLocationBattleground(
                                    locTitle,
                                    v67oGt,
                                    v67oLightForce,
                                    v67oDarkForce);
                    applySetupContribution(action, v67o.contribution());
                    boolean v67oIsBg = v67o.battleground();

                    // === V67q SITH DECK SPECIFIC TIGHTENING ===
                    // Steve's Dooku deck uses Rise Of The Sith / Revenge Of The Sith.
                    // Those starting Effects only function at a NON-PALACE battleground.
                    // If the deck has either of those cards anywhere in the player's
                    // pool (hand, reserve, used/lost/force pile, in-play, stacked, etc.),
                    // tighten the starting-location preference:
                    //   - Non-Palace battleground: +600 ADDITIONAL (net ~+900 with V67o)
                    //   - Palace battleground:     -350 ADDITIONAL (net ~-50 — discouraged)
                    //   - Non-battleground:        -300 ADDITIONAL (net ~-450)
                    // This mirrors a previous K-2 session's design (lost when their session
                    // ended without committing).
                    boolean v67qHasSithStart =
                            SetupFactsReader.hasOwnedSithStartingEffect(
                                    gameState, context.getPlayerId());
                    applySetupContribution(
                            action,
                            SetupPolicy.startingLocationSith(
                                    locTitle, v67oIsBg, v67qHasSithStart));
                }
            } catch (Exception e) {
                logger.warn("V28 STARTING LOC: Error looking up card {}: {}", cardId, e.getMessage());
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Take into hand - prefer high-value cards.
     * Handles both in-play cards (by card ID) and reserve deck cards (by blueprint).
     * CRITICAL: Only score selectable cards - GEMP rejects non-selectable selections!
     */
    private List<EvaluatedAction> evaluateTakeIntoHand(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        String decisionId = context.getDecisionId();
        PolicyContributionLedger pullLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "pull-take-decision" : decisionId + "-pull-take");
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        List<String> testingTexts = context.getTestingTexts();  // CARD TITLES from GEMP!
        PhysicalCard exactTdigwattPullSource =
                readExactTdigwattPullSource(context);

        logger.info("🔍 evaluateTakeIntoHand: {} cards, {} blueprints, {} testingTexts",
                   cardIds != null ? cardIds.size() : 0,
                   blueprints != null ? blueprints.size() : 0,
                   testingTexts != null ? testingTexts.size() : 0);

        for (int i = 0; i < cardIds.size(); i++) {
            String cardId = cardIds.get(i);
            String blueprintId = (blueprints != null && i < blueprints.size()) ? blueprints.get(i) : null;

            // LOOK UP CARD NAME FROM BLUEPRINT LIBRARY - this PROVES we can identify cards!
            String cardTitle = null;
            SwccgCardBlueprint blueprint = null;

            // Method 1: For regular cardIds, look up the card in game state
            if (gameState != null && cardId != null && !cardId.startsWith("temp")) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        cardTitle = card.getTitle();
                        blueprint = card.getBlueprint();
                        logger.info("✅ CARD LOOKUP[{}]: cardId={} -> '{}'", i, cardId, cardTitle);
                    }
                } catch (NumberFormatException e) {
                    // Card ID is not a number - expected for temp IDs
                }
            }

            // Method 2: For temp IDs or if Method 1 failed, look up from blueprintId in library
            if (cardTitle == null && blueprintId != null && !blueprintId.isEmpty() && !"inPlay".equals(blueprintId)) {
                cardTitle = getCardNameFromBlueprint(context, blueprintId);
                if (cardTitle != null) {
                    blueprint = getBlueprintFromId(context, blueprintId);
                }
            }

            // Fallback: use blueprintId as display name if we still don't have a title
            if (cardTitle == null) {
                cardTitle = (blueprintId != null && !blueprintId.isEmpty()) ? "bp=" + blueprintId : cardId;
                logger.warn("⚠️ Could not look up card name for [{}]: cardId={}, bp={}", i, cardId, blueprintId);
            }

            // CRITICAL: Skip non-selectable cards! But still log the REAL card name
            if (!isCardSelectable(context, i)) {
                logger.info("⚠️ Skipping non-selectable[{}]: '{}' (cardId={}, bp={})", i, cardTitle, cardId, blueprintId);
                continue;
            }

            PullTakeCandidateFacts pullFacts = PullPolicyAdapter.readTakeCandidate(
                    context, cardId, cardTitle, blueprintId, blueprint);

            EvaluatedAction action = new EvaluatedAction(
                    cardId,
                    ActionType.SELECT_CARD,
                    50.0f,
                    "Take " + cardTitle + " into hand");
            action.setCardName(cardTitle);
            if (blueprintId != null) {
                action.setBlueprintId(blueprintId);
            }

            if (exactTdigwattPullSource != null) {
                PhysicalCard exactCandidate =
                        findExactReservePullCandidate(
                                context, cardId, blueprintId);
                if (exactCandidate != null) {
                    TdigwattObjectiveFactsReader
                            .readObjectivePullCandidate(
                                    game,
                                    context.getPlayerId(),
                                    exactTdigwattPullSource,
                                    exactCandidate)
                            .ifPresent(facts ->
                                    applyTdigwattPolicy(
                                            action,
                                            TdigwattObjectiveScoringPolicy
                                                    .scorePullChild(
                                                        new TdigwattObjectiveScoringPolicy
                                                            .PullChildFacts(
                                                                action.getActionId(),
                                                                facts,
                                                                true,
                                                                true))
                                                    .result()));
                }
            }

            pullLedger.register(PullTakeCandidatePolicy.evaluate(pullFacts));
            PullDeployCandidatePolicy.Evaluation pullCandidate =
                    PullDeployCandidatePolicy.evaluate(
                            PullPolicyAdapter.readDeployCandidate(
                                    context,
                                    cardId,
                                    cardId,
                                    blueprintId,
                                    cardTitle,
                                    blueprint != null
                                            ? blueprint.getCardCategory()
                                            : null,
                                    blueprint));
            pullLedger.register(pullCandidate.result());
            PolicyOperationAdapter.apply(action, pullLedger);
            applyCountedObjectivePullPolicy(
                    context, action, cardId,
                    blueprintId, false);

            logger.debug("🎯 {} ({}): score={}, destiny={}, power={}",
                    cardTitle, blueprintId != null ? blueprintId : cardId,
                    action.getScore(),
                    pullFacts.destiny() != null ? pullFacts.destiny() : "?",
                    pullFacts.power() != null ? pullFacts.power() : "?");

            actions.add(action);
        }

        // Sort by score descending for logging
        actions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        if (!actions.isEmpty()) {
            logger.info("✅ Best take into hand: {} (score: {})",
                       actions.get(0).getCardName(), actions.get(0).getScore());
        }

        return actions;
    }

    private PhysicalCard readExactTdigwattPullSource(
            DecisionContext context) {
        if (context == null
                || context.getGame() == null
                || context.getGameState() == null
                || !"ARBITRARY_CARDS".equals(
                        context.getDecisionType())
                || context.getDecisionText() == null
                || !"choose card to take into hand".equalsIgnoreCase(
                        context.getDecisionText().trim())
                || context.getMin() != 1
                || context.getMax() != 1) {
            return null;
        }
        try {
            var identity =
                    TdigwattObjectiveFactsReader
                            .readObjectiveIdentity(
                                    context.getGame(),
                                    context.getPlayerId());
            var actionState =
                    context.getGameState()
                            .getTopGameTextActionState();
            var liveAction = actionState != null
                    ? actionState.getGameTextAction() : null;
            PhysicalCard source = liveAction != null
                    ? liveAction.getActionSource() : null;
            if (identity.isEmpty()
                    || source == null
                    || source.getPermanentCardId()
                        != identity.get().physicalCardId()
                    || !"Take card into hand from Reserve Deck"
                            .equals(liveAction.getText())) {
                return null;
            }
            GameTextActionId expectedActionId =
                    identity.get().printing()
                        == com.gempukku.swccgo.ai.models.common.phase
                            .TdigwattObjectiveFacts.Printing.CLASSIC
                    ? GameTextActionId
                        .THIS_DEAL_IS_GETTING_WORSE_ALL_THE_TIME__UPLOAD_CARD
                    : GameTextActionId
                        .THIS_DEAL_IS_GETTING_WORSE_ALL_THE_TIME_V__UPLOAD_CARD;
            return liveAction.getGameTextActionId()
                    == expectedActionId ? source : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    static PhysicalCard findExactReservePullCandidate(
            DecisionContext context,
            String cardId,
            String blueprintId) {
        if (context == null
                || context.getGameState() == null
                || context.getPlayerId() == null
                || blueprintId == null
                || blueprintId.isBlank()
                || "inPlay".equals(blueprintId)) {
            return null;
        }
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        if (cardId != null) {
            try {
                PhysicalCard exact =
                        gameState.findCardById(
                                Integer.parseInt(cardId));
                if (exact != null
                        && playerId.equals(exact.getOwner())
                        && blueprintId.equals(
                                exact.getBlueprintId(true))) {
                    return exact;
                }
            } catch (NumberFormatException ignored) {
                // The stock Reserve decision uses strict tempN ordering.
            }
        }
        if (cardId == null || !cardId.startsWith("temp")) {
            return null;
        }
        List<String> offeredCardIds = context.getCardIds();
        List<String> offeredBlueprints =
                context.getBlueprints();
        List<PhysicalCard> reserveCards =
                gameState.getCardPile(
                        playerId,
                        com.gempukku.swccgo.common.Zone
                                .RESERVE_DECK);
        if (offeredCardIds == null
                || offeredBlueprints == null
                || reserveCards == null
                || offeredCardIds.size()
                    != offeredBlueprints.size()
                || offeredBlueprints.size()
                    != reserveCards.size()) {
            return null;
        }
        int offeredIndex = -1;
        for (int index = 0;
                index < offeredCardIds.size();
                index++) {
            PhysicalCard reserveCard =
                    reserveCards.get(index);
            if (!("temp" + index).equals(
                        offeredCardIds.get(index))
                    || reserveCard == null
                    || !offeredBlueprints.get(index).equals(
                            reserveCard.getBlueprintId(true))) {
                return null;
            }
            if (cardId.equals(
                    offeredCardIds.get(index))) {
                offeredIndex = index;
            }
        }
        if (offeredIndex < 0) {
            return null;
        }
        PhysicalCard exact =
                reserveCards.get(offeredIndex);
        return exact != null
                && playerId.equals(exact.getOwner())
                && blueprintId.equals(
                        exact.getBlueprintId(true))
                ? exact : null;
    }

    /**
     * Lost pile selection - prefer low-value cards.
     */
    private List<EvaluatedAction> evaluateLostPileSelection(DecisionContext context) {
        return evaluateForceLoss(context);  // Same logic
    }

    /**
     * Unknown decision type - neutral scoring with card name lookup.
     * CRITICAL: Only score selectable cards - GEMP rejects non-selectable selections!
     *
     * Ported from Python _evaluate_unknown() which scores based on card type.
     */
    private List<EvaluatedAction> evaluateUnknown(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        String shieldDecisionId = context.getDecisionId();
        PolicyContributionLedger pullCandidateLedger = new PolicyContributionLedger(
                shieldDecisionId == null || shieldDecisionId.isBlank()
                        ? "pull-deploy-candidate-unknown-decision"
                        : shieldDecisionId + "-pull-deploy-candidate");
        PolicyContributionLedger forceLossLedger = new PolicyContributionLedger(
                shieldDecisionId == null || shieldDecisionId.isBlank()
                        ? "force-loss-unknown-decision"
                        : shieldDecisionId + "-force-loss-unknown");
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        List<String> testingTexts = context.getTestingTexts();  // CARD TITLES from GEMP!

        // Determine base score - higher for gain/select decisions
        String textLower = context.getDecisionText() != null ? context.getDecisionText().toLowerCase(Locale.ROOT) : "";
        boolean isLossDecision = textLower.contains("lose") || textLower.contains("lost") ||
                                 textLower.contains("place in") || textLower.contains("put on");

        logger.info("🔍 evaluateUnknown: {} cards, {} blueprints, {} testingTexts for '{}' (loss={})",
                   cardIds != null ? cardIds.size() : 0,
                   blueprints != null ? blueprints.size() : 0,
                   testingTexts != null ? testingTexts.size() : 0,
                   context.getDecisionText(),
                   isLossDecision);

        int selectableCount = 0;
        int skippedCount = 0;

        for (int i = 0; i < cardIds.size(); i++) {
            String cardId = cardIds.get(i);
            String blueprintId = (blueprints != null && i < blueprints.size()) ? blueprints.get(i) : null;

            // LOOK UP CARD NAME FROM BLUEPRINT LIBRARY - this PROVES we can identify cards!
            String cardTitle = null;
            SwccgCardBlueprint blueprint = null;

            // Method 1: For regular cardIds, look up the card in game state
            if (gameState != null && cardId != null && !cardId.startsWith("temp")) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        cardTitle = card.getTitle();
                        blueprint = card.getBlueprint();
                        logger.info("✅ CARD LOOKUP[{}]: cardId={} -> '{}'", i, cardId, cardTitle);
                    }
                } catch (NumberFormatException e) {
                    // Card ID is not a number - expected for temp IDs
                }
            }

            // Method 2: For temp IDs or if Method 1 failed, look up from blueprintId in library
            if (cardTitle == null && blueprintId != null && !blueprintId.isEmpty() && !"inPlay".equals(blueprintId)) {
                cardTitle = getCardNameFromBlueprint(context, blueprintId);
                if (cardTitle != null) {
                    blueprint = getBlueprintFromId(context, blueprintId);
                }
            }

            // Fallback: use blueprintId as display name if we still don't have a title
            if (cardTitle == null) {
                cardTitle = (blueprintId != null && !blueprintId.isEmpty()) ? "bp=" + blueprintId : cardId;
                logger.warn("⚠️ Could not look up card name for [{}]: cardId={}, bp={}", i, cardId, blueprintId);
            }

            // CRITICAL: Skip non-selectable cards! But still log the REAL card name
            if (!isCardSelectable(context, i)) {
                skippedCount++;
                logger.info("⚠️ Skipping non-selectable[{}]: '{}' (cardId={}, bp={})", i, cardTitle, cardId, blueprintId);
                continue;
            }
            selectableCount++;

            // Now we have card info
            Float destiny = null;
            Float power = null;
            CardCategory category = null;

            // Log the final card title we determined
            logger.info("📋 evaluateUnknown[{}]: cardId='{}', blueprintId='{}', TITLE='{}'",
                i, cardId, blueprintId, cardTitle);

            // Extract card properties from blueprint (if we have one)
            if (blueprint != null) {
                try {
                    destiny = blueprint.getDestiny();
                } catch (UnsupportedOperationException e) {
                    // Card type doesn't support destiny
                }
                if (blueprint.hasPowerAttribute()) {
                    power = blueprint.getPower();
                }
                category = blueprint.getCardCategory();
            }

            // Base score beats PassEvaluator (~5-20)
            float baseScore = 30.0f;
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                baseScore,
                "Select " + cardTitle
            );
            action.setCardName(cardTitle);
            if (blueprintId != null) {
                action.setBlueprintId(blueprintId);
            }

            PullDeployCandidatePolicy.Evaluation pullCandidate =
                    PullDeployCandidatePolicy.evaluate(
                            PullPolicyAdapter.readDeployCandidate(
                                    context, cardId, cardId,
                                    blueprintId, cardTitle,
                                    category, blueprint));
            pullCandidateLedger.register(pullCandidate.result());
            PolicyOperationAdapter.apply(action, pullCandidateLedger);
            if (pullCandidate.adapterStep()
                    == PullDeployCandidatePolicy.AdapterStep.CONTINUE_CANDIDATE) {
                actions.add(action);
                continue;
            }

            PolicyContributionLedger shieldLedger = new PolicyContributionLedger(
                    shieldDecisionId == null || shieldDecisionId.isBlank()
                            ? "shield-unknown-" + cardId
                            : shieldDecisionId + "-shields-" + cardId);

            // === V112: BATTLE ORDER / BATTLE PLAN GATE (evaluateUnknown path) ===
            // When K&D plays Battle Order/Plan from stacked, the decision may route
            // through evaluateUnknown rather than evaluateDefensiveShieldSelection
            // (where V51 lives). Mirror the BG-occupation check here to close that gap.
            // Battle Order/Plan are only useful if Rando already occupies BOTH a
            // battleground site AND a battleground system simultaneously. Otherwise
            // Battle Order/Plan normally requires Rando to use 3 Force per drain; occupation
            // waives that cost, and Battle Plan also suppresses the Battle Order modifier.
            boolean v112IsBattleOrder = ShieldPolicy.isBattleOrderOrPlan(cardTitle);
            // Hoth repair #2 (2026-07-27): corrected battleOrderLive law.
            // OLD: ShieldFacts.occupiesBothTheaters(game, playerId)
            boolean v112BattleOrderLive = !v112IsBattleOrder
                    || ShieldFacts.battleOrderLive(
                        context.getGame(), context.getPlayerId(),
                        computeNetDrainBalance(context.getGame(),
                            context.getGameState(),
                            context.getPlayerId()) >= 2);
            shieldLedger.register(ShieldPolicy.unknownBattleOrderGate(
                    cardId, cardTitle, v112BattleOrderLive));
            shieldLedger.register(
                    ShieldPolicy.battleOrderPlanRedundancyGate(
                            cardId, cardTitle,
                            ShieldFacts
                                .battleOrderPlanEquivalentOnTable(
                                    context.getGameState())));
            if (v112IsBattleOrder && !v112BattleOrderLive) {
                logger.warn("V112 BATTLE ORDER GATE: '{}' blocked - battleOrderLive=false",
                        cardTitle);
            }

            // === V117 (Steve, 2026-05-22): UNIVERSAL 4TH-SHIELD HARD BLOCK (evaluateUnknown) ===
            // Per Steve: "We need to hard block deploy from Knowledge and Defense effect
            // when 3 shields already on table. The conditions we set for that fourth
            // shield must be met before deploying."
            //
            // V105/V107 at line ~7100 already does this for the defensive-shield-selection
            // path. But K&D plays a card from a MIXED stacked pile (shields + non-shields)
            // route through evaluateUnknown when isShieldSelectionByContent() returns false
            // (<50% shields in stacked). V112 covered Battle Order/Plan specifically.
            // V117 closes the gap for ALL shields: when 3 defensive shields are already on
            // Rando's table, hard-block any 4th shield unless the shared fourth-slot policy
            // returns this specific shield title (V105 Battle Order/Plan or V107 Resistance/
            // Ultimatum trigger active).
            if (category == CardCategory.DEFENSIVE_SHIELD && cardTitle != null) {
                int shieldsOnTable = ShieldFacts.shieldsOnTable(
                        context.getGameState(), context.getPlayerId());
                ShieldStrategy shieldStrategy = context.getShieldStrategy();
                ShieldPolicy.FourthSlotPick fourthSlot =
                        new ShieldPolicy.FourthSlotPick(null, false,
                                ShieldPolicy.FourthSlotTrigger.CLOSED);
                if (shieldsOnTable >= 3 && shieldStrategy != null) {
                    ShieldFacts.FourthSlotFacts fourthSlotFacts =
                            ShieldFacts.fourthSlotFacts(context.getGameState(),
                                    context.getGame(), context.getPlayerId());
                    fourthSlot = shieldStrategy.fourthSlotPick(fourthSlotFacts,
                            preferred -> preferredShieldInCandidates(context, preferred));
                }
                shieldLedger.register(ShieldPolicy.unknownFourthSlot(
                        cardId, shieldsOnTable, cardTitle, fourthSlot));
                if (shieldsOnTable >= 3) {
                    logger.warn("V117 4TH SHIELD: '{}' evaluated with preferred={} pursue={}",
                            cardTitle, fourthSlot.preferred(), fourthSlot.pursue());
                }
            }
            PolicyOperationAdapter.apply(action, shieldLedger);

            // SETUP tree: V22/V25/V43/V80/V126/V186/V187, preserving legacy order.
            if (SetupPolicy.isSetupTurn(context.getTurnNumber()) && cardTitle != null) {
                SetupPolicy.StartingEffectEvaluation setupBan =
                        SetupPolicy.startingEffectBan(cardTitle);
                applySetupContributions(action, setupBan.contributions());
                if (setupBan.terminalCandidate()) {
                    actions.add(action);
                    continue;
                }

                boolean copyCountKnown = context.getDeckOracle() != null
                        && context.getDeckOracle().isAnalyzed();
                int copyCount = copyCountKnown
                        ? context.getDeckOracle().countCopiesByTitle(cardTitle) : 0;
                String iwtmEffect = context.getObjectiveAnalyzer() != null
                        ? context.getObjectiveAnalyzer().getIwtmPreferredStartingEffect() : null;
                boolean iwtmPreferred = context.getObjectiveAnalyzer() != null
                        && context.getObjectiveAnalyzer().isAnalyzed()
                        && context.getObjectiveAnalyzer().isWantThatMap()
                        && iwtmEffect != null
                        && cardTitle.toLowerCase(Locale.ROOT).contains(iwtmEffect);
                applySetupContributions(action, SetupPolicy.startingEffectIdentity(
                        cardTitle, copyCountKnown, copyCount, iwtmPreferred));

                String setupGameText = blueprint != null ? blueprint.getGameText() : null;
                boolean rotsOnTable = SetupPolicy.isRotsPairingCandidate(setupGameText)
                        && SetupFactsReader.hasOwnedInPlayRevengeOfTheSith(
                        gameState, context.getPlayerId());
                applySetupContributions(action, SetupPolicy.startingEffectText(
                        cardTitle, setupGameText, rotsOnTable));

                boolean huntDown = context.getObjectiveAnalyzer() != null
                        && context.getObjectiveAnalyzer().isAnalyzed()
                        && context.getObjectiveAnalyzer().isHuntDownV();
                applySetupContributions(action,
                        SetupPolicy.startingEffectDeck(cardTitle, huntDown));

                boolean objectiveAnalyzed = context.getObjectiveAnalyzer() != null
                        && context.getObjectiveAnalyzer().isAnalyzed();
                List<String> locationFragments = objectiveAnalyzed
                        ? new ArrayList<>(context.getObjectiveAnalyzer()
                        .getFlipConditionLocationFragments()) : List.of();
                List<String> requiredCards = objectiveAnalyzed
                        ? new ArrayList<>(context.getObjectiveAnalyzer()
                        .getRequiredCardsOnTable()) : List.of();
                applySetupContributions(action, SetupPolicy.startingEffectObjective(
                        setupGameText, objectiveAnalyzed,
                        locationFragments, requiredCards));
            }
            // V24.5: No randomness — deterministic decisions only

            // Score based on card type (like Python)
            if (isLossDecision) {
                // === V25: HUNT DOWN V — LIGHTSABER PRIORITY (evaluateUnknown path) ===
                // For Hunt Down V, lightsabers are critical for the deck engine:
                // - Vader + lightsaber cancels drain bonuses (back side)
                // - Hatred engine needs lightsabers stacked
                // - "I Am Your Father" pulls Vader's Lightsaber
                boolean huntDownLightsaber = false;
                com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer lsObjAnalyzer =
                    context.getObjectiveAnalyzer();
                if (cardTitle != null && lsObjAnalyzer != null
                    && lsObjAnalyzer.isAnalyzed() && lsObjAnalyzer.isHuntDownV()
                    && cardTitle.toLowerCase(java.util.Locale.ROOT).contains("lightsaber")) {
                    huntDownLightsaber = true;
                    logger.warn("V25 HUNT DOWN UNKNOWN-LOSS: {} is a lightsaber — PROTECT (-300)", cardTitle);
                }
                forceLossLedger.register(ForceLossPolicy.scoreUnknownLoss(
                        cardId, category, huntDownLightsaber));
                PhysicalCard unknownLossCard = null;
                try {
                    if (gameState != null && cardId != null
                            && !cardId.startsWith("temp")) {
                        unknownLossCard = gameState.findCardById(
                                Integer.parseInt(cardId));
                    }
                } catch (NumberFormatException ignored) {
                    // Unknown physical identities receive no package protection.
                }
                var unknownLossAnalyzer =
                        context.getObjectiveAnalyzer();
                forceLossLedger.register(
                        ForceLossPolicy
                            .scoreUnknownObjectiveRetention(
                                cardId,
                                unknownLossAnalyzer != null
                                    && unknownLossAnalyzer
                                        .isPreferredShieldRoutePackageForceLossCandidate(
                                            game,
                                            context.getPlayerId(),
                                            unknownLossCard)));
                PolicyOperationAdapter.apply(action, forceLossLedger);
            }

            // Shared PULL child policy owns gain value, Hunt Down lightsaber,
            // Cloud City route-specific ordering, priority, and AMSD safety.
            applyUnknownPullSelectionPolicy(context, action, cardTitle,
                    blueprintId, category, blueprint != null,
                    isLossDecision, textLower);
            applyCountedObjectivePullPolicy(
                    context, action, cardId,
                    blueprintId, isLossDecision);

            // V28/V47 RESERVE SOLO BLOCK — RETIRED 2026-07-12 (batch 1d; Codex m00206 wrong-facts
            // audit CODEX_V47_WRONG_FACTS_AUDIT_2026-07-12.md): it applied Cloud City board facts to
            // EVERY "deploy...reserve" character prompt regardless of the real forced destination
            // (Krennic->Scarif, Praji, Snoke all false-blocked at -9999 on forced noPass nodes).
            // Replacement owner: FormationSafety pull-route guard (destination-aware, ActionText
            // V192 block). DELETED per Steve's 2026-07-12 migration ruling (backup + git = undo).

            actions.add(action);
        }

        logger.info("🔍 evaluateUnknown: {} selectable, {} skipped (non-selectable)",
                   selectableCount, skippedCount);

        if (actions.isEmpty()) {
            logger.warn("⚠️ evaluateUnknown: No selectable cards! Decision may fail.");
        }

        return actions;
    }

    /**
     * Check if a location is likely a battleground.
     */
    private boolean isLikelyBattleground(SwccgCardBlueprint blueprint) {
        // Sites and systems are often battlegrounds
        // This is a heuristic - actual battleground status comes from card data
        return blueprint != null && blueprint.getCardCategory() == CardCategory.LOCATION;
    }

    /**
     * Check if a card at given index is selectable.
     * CRITICAL: GEMP rejects selection of non-selectable cards!
     */
    private boolean isCardSelectable(DecisionContext context, int index) {
        List<Boolean> selectable = context.getSelectable();
        if (selectable == null || selectable.isEmpty()) {
            // No selectable info - assume all are selectable
            return true;
        }
        if (index >= selectable.size()) {
            // Index out of bounds - assume selectable
            return true;
        }
        Boolean isSelectable = selectable.get(index);
        return isSelectable == null || isSelectable;
    }

    private boolean isOpponentUndercoverOnlyTarget(DecisionContext context, String targetCardId) {
        try {
            if (context.getGameState() == null || context.getGame() == null
                    || context.getPlayerId() == null) {
                return false;
            }
            PhysicalCard target = context.getGameState().findCardById(Integer.parseInt(targetCardId));
            String opponentId = context.getGameState().getOpponent(context.getPlayerId());
            if (target == null || opponentId == null) {
                return false;
            }
            boolean opponentUndercover = false;
            for (PhysicalCard card : context.getGameState().getCardsAtLocation(target)) {
                if (card != null && opponentId.equals(card.getOwner())
                        && card.isUndercover()) {
                    opponentUndercover = true;
                    break;
                }
            }
            return opponentUndercover
                    && context.getGame().getModifiersQuerying().getTotalPowerAtLocation(
                            context.getGameState(), target, opponentId,
                            false, false) <= 0.0f;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Hoth repair #4 (2026-07-27): true when the PLANNED deploy target would
     * receive a FormationSafety HARD_BLOCK for the deploying character.
     * Mirrors the per-row verdict computation (fsForce/fsThisCost/fsBuddyCost/
     * flip-gate refinement then assessCharacterDeploy) — keep the two in sync.
     * Fails closed to false so an uncertain verdict never suppresses the
     * plan-following defer.
     */
    private boolean isPlannedTargetFormationHardBlocked(
            DecisionContext context, String plannedTargetId,
            String deployingBlueprintId,
            DeploymentInstruction plannedDeployInstruction,
            DeploymentPlan deploymentPlanSnapshot) {
        try {
            GameState gameState = context.getGameState();
            if (gameState == null || context.getGame() == null
                    || context.getPlayerId() == null
                    || deployingBlueprintId == null) {
                return false;
            }
            PhysicalCard location = gameState.findCardById(
                    Integer.parseInt(plannedTargetId));
            if (location == null) return false;
            PhysicalCard deployingCard = null;
            for (PhysicalCard h : gameState.getHand(context.getPlayerId())) {
                if (h != null
                        && deployingBlueprintId.equals(h.getBlueprintId(false))) {
                    deployingCard = h;
                    break;
                }
            }
            if (deployingCard == null) return false;
            SwccgCardBlueprint fsDepBp = deployingCard.getBlueprint();
            if (fsDepBp == null
                    || fsDepBp.getCardCategory() != CardCategory.CHARACTER) {
                return false;
            }
            float fsForce = gameState.getForcePileSize(context.getPlayerId());
            Float fsThisCost = fsDepBp.getDeployCost();
            Float fsBuddyCost = null;
            java.util.Set<Integer> fsCharacterIdsInHand = new java.util.HashSet<>();
            for (PhysicalCard fsH : gameState.getHand(context.getPlayerId())) {
                if (fsH == null || fsH.getBlueprint() == null) continue;
                if (fsH.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                fsCharacterIdsInHand.add(fsH.getPermanentCardId());
            }
            boolean fsExactPlanCard = plannedDeployInstruction != null
                && plannedDeployInstruction.getCardPermanentCardId() != null
                && plannedDeployInstruction.getCardCurrentCardId() != null
                && deployingCard.getPermanentCardId() == plannedDeployInstruction.getCardPermanentCardId()
                && deployingCard.getCardId() == plannedDeployInstruction.getCardCurrentCardId();
            if (fsExactPlanCard && deploymentPlanSnapshot != null) {
                fsBuddyCost = deploymentPlanSnapshot.getCheapestPlannedCharacterBuddyCost(
                    plannedDeployInstruction, String.valueOf(location.getCardId()),
                    fsCharacterIdsInHand);
            }
            var fsObj = context.getObjectiveAnalyzer();
            String fsFlipGate = (fsObj != null && fsObj.isAnalyzed())
                    ? fsObj.getFlipCriticalControlSite() : null;
            if (fsObj != null && fsObj.hasFlipGateActorRequirement()) {
                boolean actorGateCandidate =
                    fsObj.advancesUnfilledFlipGateActorRequirement(
                        context.getGame(), context.getPlayerId(),
                        deployingCard, location);
                int fsFriendlyCharacters = com.gempukku.swccgo.ai.models.common.strategy
                    .FormationSafety.countFriendlyNonUndercoverCharacters(
                        gameState.getCardsAtLocation(location),
                        context.getPlayerId());
                boolean fsFundedBuddy = fsBuddyCost != null
                    && fsThisCost != null
                    && fsThisCost + fsBuddyCost <= fsForce;
                if (!actorGateCandidate
                        || !(fsFriendlyCharacters > 0 || fsFundedBuddy)) {
                    fsFlipGate = null;
                }
            }
            if (fsObj != null
                    && fsObj.wouldCompletePreFlipRequirementAt(
                        context.getGame(), context.getPlayerId(),
                        deployingCard, location)) {
                fsFlipGate = location.getTitle();
            }
            com.gempukku.swccgo.ai.models.common.strategy.FormationSafety.DeployVerdict fsVerdict =
                com.gempukku.swccgo.ai.models.common.strategy.FormationSafety
                .assessCharacterDeploy(context.getGame(), gameState, context.getPlayerId(),
                    deployingCard,
                    fsDepBp.hasPowerAttribute() ? fsDepBp.getPower() : null,
                    fsDepBp.hasAbilityAttribute() ? fsDepBp.getAbility() : null,
                    deployingCard.isUndercover(),
                    location, fsForce, fsThisCost, fsBuddyCost, fsFlipGate);
            return fsVerdict.constraint()
                    == com.gempukku.swccgo.ai.models.common.strategy
                        .FormationSafety.DeployConstraint.HARD_BLOCK;
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * Reserve deck selection - evaluate cards by blueprint.
     * Used when selecting from Reserve Deck (e.g., deploying shields via starting effect).
     * Uses DeployPhasePlanner when available to select cards that fit the deployment plan.
     */
    private boolean isVaderCastleReserveSelection(
            DecisionContext context, String textLower) {
        if (context == null || textLower == null
                || !"choose card to deploy from reserve deck"
                        .equals(textLower.trim())
                || context.getGameState() == null
                || context.getObjectiveAnalyzer() == null
                || !context.getObjectiveAnalyzer().isHuntDownV()) {
            return false;
        }
        try {
            var state =
                    context.getGameState()
                        .getTopGameTextActionState();
            var liveAction = state != null
                    ? state.getGameTextAction() : null;
            PhysicalCard source = liveAction != null
                    ? liveAction.getActionSource() : null;
            String actionText = liveAction != null
                    ? liveAction.getText() : null;
            return source != null
                    && "209_50".equals(
                        source.getBlueprintId(true))
                    && "deploy vader from reserve deck"
                        .equalsIgnoreCase(
                            actionText != null
                                ? actionText.trim() : "");
        } catch (Exception e) {
            return false;
        }
    }

    private List<EvaluatedAction> evaluateReserveDeckSelection(DecisionContext context, String textLower) {
        List<EvaluatedAction> actions = new ArrayList<>();
        String decisionId = context.getDecisionId();
        PolicyContributionLedger pullCandidateLedger = new PolicyContributionLedger(
                decisionId == null || decisionId.isBlank()
                        ? "pull-deploy-candidate-reserve-decision"
                        : decisionId + "-pull-deploy-candidate-reserve");
        List<String> blueprints = context.getBlueprints();
        ShieldStrategy shieldStrategy = context.getShieldStrategy();
        DeployPhasePlanner planner = context.getDeployPhasePlanner();
        SwccgGame game = context.getGame();
        Side side = context.getSide();
        String playerId = context.getPlayerId();
        int turnNumber = context.getTurnNumber();
        boolean castleVaderSelection =
                isVaderCastleReserveSelection(
                        context, textLower);
        List<com.gempukku.swccgo.ai.models.common.strategy
                .ObjectiveAnalyzer.VaderCastleDeployCandidateAssessment>
                castleAssessments = new ArrayList<>();
        boolean hasCastleMoveCandidate = false;
        if (castleVaderSelection) {
            for (int i = 0; i < blueprints.size(); i++) {
                var assessment = context.getObjectiveAnalyzer()
                        .assessVaderCastleDeployCandidate(
                                game, playerId,
                                blueprints.get(i));
                castleAssessments.add(assessment);
                hasCastleMoveCandidate |=
                        isCardSelectable(context, i)
                        && assessment.preservesMoveCost();
            }
        }

        logger.info("[CardSelectionEvaluator] Evaluating Reserve Deck selection: {}", textLower);

        // Get deployment plan if available
        DeploymentPlan plan = null;
        if (planner != null && game != null && side != null && playerId != null) {
            plan = planner.createPlan(game, playerId, side);
            if (plan != null) {
                logger.info("[CardSelectionEvaluator] Using deployment plan: strategy={}, instructions={}",
                    plan.getStrategy(), plan.getInstructions().size());
            }
        }

        // V24.10: Get card titles for smarter reserve deck selection
        List<String> reserveTestingTexts = context.getTestingTexts();
        List<String> reserveCardIds = context.getCardIds();

        for (int i = 0; i < blueprints.size(); i++) {
            String blueprintId = blueprints.get(i);
            String candidateCardId =
                    reserveCardIds != null
                        && i < reserveCardIds.size()
                            ? reserveCardIds.get(i) : null;

            String testingTitle = null;
            if (reserveTestingTexts != null && i < reserveTestingTexts.size()) {
                testingTitle = reserveTestingTexts.get(i);
            }

            SwccgCardBlueprint pullBlueprint = null;
            CardCategory pullCategory = null;
            String cardTitle = null;
            try {
                pullBlueprint = getBlueprintFromId(context, blueprintId);
                if (pullBlueprint != null) {
                    pullCategory = pullBlueprint.getCardCategory();
                    cardTitle = pullBlueprint.getTitle();
                }
            } catch (Exception e) { /* ignore */ }
            if (cardTitle == null) {
                cardTitle = testingTitle;
            }

            // Use index as action ID for blueprint-based selections
            EvaluatedAction action = new EvaluatedAction(
                String.valueOf(i),
                ActionType.DEPLOY,
                50.0f,
                "Deploy " + (cardTitle != null ? cardTitle : blueprintId)
            );
            if (castleVaderSelection) {
                var assessment = castleAssessments.get(i);
                if (assessment.preservesMoveCost()) {
                    action.addReasoning(
                        "DEPLOY.OBJECTIVE.VADERS_CASTLE_CANDIDATE: this Vader preserves the exact outbound move cost",
                        1000.0f);
                } else if (hasCastleMoveCandidate) {
                    action.hardVeto(
                        "DEPLOY.OBJECTIVE.VADERS_CASTLE_CANDIDATE_HOLD: choose the Vader that preserves the outbound move");
                } else if (assessment.legalDeploy()) {
                    action.addReasoning(
                        "DEPLOY.OBJECTIVE.VADERS_CASTLE_CANDIDATE_DEPLOY_ONLY: legal Vader, but no offered candidate preserves the move",
                        250.0f);
                } else {
                    action.hardVeto(
                        "DEPLOY.OBJECTIVE.VADERS_CASTLE_CANDIDATE_ILLEGAL: offered blueprint no longer has a legal Castle deploy");
                }
            }
            PullDeployCandidatePolicy.Evaluation pullCandidate =
                    PullDeployCandidatePolicy.evaluate(
                            PullPolicyAdapter.readDeployCandidate(
                                    context,
                                    action.getActionId(),
                                    candidateCardId,
                                    blueprintId,
                                    cardTitle != null ? cardTitle : blueprintId,
                                    pullCategory,
                                    pullBlueprint));
            pullCandidateLedger.register(pullCandidate.result());
            PolicyOperationAdapter.apply(action, pullCandidateLedger);
            if (pullCandidate.adapterStep()
                    == PullDeployCandidatePolicy.AdapterStep.CONTINUE_CANDIDATE) {
                actions.add(action);
                continue;
            }

            applySetupContributions(action, SetupPolicy.reserveStartingEffect(
                    cardTitle, SetupPolicy.isSetupTurn(context.getTurnNumber())));

            // Shared PULL child policy owns route-specific Cloud City ordering
            // and reserve-deck deployment-plan scoring.
            applyBlueprintPullSelectionPolicy(
                    context, action, blueprintId, cardTitle,
                    textLower, plan);
            applyCountedObjectivePullPolicy(
                    context, action, candidateCardId,
                    blueprintId, false);
            if (context.getObjectiveAnalyzer() != null
                    && pullBlueprint != null) {
                int matchingOperatives = context.getObjectiveAnalyzer()
                        .countMatchingOperativesAvailableForSetupSystem(
                            context.getGame(), context.getPlayerId(),
                            pullBlueprint);
                if (matchingOperatives > 0) {
                    float packageScore = matchingOperatives * 500.0f;
                    action.addReasoning(
                        "OPERATIVE OBJECTIVE SETUP: "
                            + matchingOperatives
                            + " matching operatives support "
                            + cardTitle,
                        packageScore);
                }
            }

            // === Shield scoring ===
            if (pullCategory == CardCategory.DEFENSIVE_SHIELD) {
                float shieldScore = 0.0f;
                int minTurnToPlay = 0;
                if (shieldStrategy != null) {
                    shieldScore = shieldStrategy.scoreShield(
                            blueprintId, cardTitle, turnNumber);
                    action.addReasoning("Shield scoring", shieldScore);
                    String description = shieldStrategy.getShieldDescription(
                            blueprintId, cardTitle);
                    logger.info("[ReserveDeck] Shield {}: score={} ({})",
                            cardTitle, shieldScore, description);
                    minTurnToPlay = shieldStrategy.minTurnToPlay(
                            blueprintId, cardTitle);
                }

                int shieldsOnTable = ShieldFacts.shieldsOnTable(
                        context.getGameState(), playerId);
                ShieldPolicy.FourthSlotPick fourthSlot =
                        new ShieldPolicy.FourthSlotPick(null, false,
                                ShieldPolicy.FourthSlotTrigger.CLOSED);
                if (shieldsOnTable >= 3) {
                    ShieldFacts.FourthSlotFacts fourthSlotFacts =
                            ShieldFacts.fourthSlotFacts(context.getGameState(),
                                    game, playerId);
                    if (shieldStrategy != null) {
                        fourthSlot = shieldStrategy.fourthSlotPick(fourthSlotFacts,
                                preferred -> preferredShieldInCandidates(context, preferred));
                    } else {
                        fourthSlot = ShieldPolicy.fourthSlotPick(side, fourthSlotFacts,
                                preferred -> preferredShieldInCandidates(context, preferred));
                    }
                }

                // Hoth repair #2 (2026-07-27): corrected battleOrderLive law.
                // OLD: ShieldFacts.occupiesBothTheaters(game, playerId)
                boolean battleOrderLive = !ShieldPolicy.isBattleOrderOrPlan(cardTitle)
                        || ShieldFacts.battleOrderLive(game, playerId,
                            computeNetDrainBalance(game,
                                context.getGameState(), playerId) >= 2);
                PolicyContributionLedger shieldLedger = new PolicyContributionLedger(
                        decisionId == null || decisionId.isBlank()
                                ? "shield-reserve-" + action.getActionId()
                                : decisionId + "-shield-reserve-" + action.getActionId());
                shieldLedger.register(ShieldPolicy.shieldCandidateAdjustments(
                        action.getActionId(), cardTitle, shieldScore, minTurnToPlay,
                        turnNumber, shieldsOnTable, fourthSlot, battleOrderLive,
                        ShieldFacts.battleOrderPlanEquivalentOnTable(
                            context.getGameState()),
                        ShieldPolicy.CandidateRoute.RESERVE));
                PolicyOperationAdapter.apply(action, shieldLedger);
                if (shieldsOnTable >= 3) {
                    logger.warn("V105/V107 4TH SLOT (reserve): '{}' preferred={} pursue={}",
                            cardTitle, fourthSlot.preferred(), fourthSlot.pursue());
                }
                if (ShieldPolicy.isBattleOrderOrPlan(cardTitle)) {
                    logger.warn("V51 BATTLE ORDER (reserve): battleOrderLive={} base={}",
                            battleOrderLive, shieldScore);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Check if this is a shield selection by examining the available cards.
     * Similar to Python's approach of checking if majority of options are shields.
     * V29.5: Also handles ARBITRARY_CARDS decisions with temp IDs (e.g., K&D shield plays)
     * by looking up cards via blueprint IDs instead of integer card IDs.
     */
    private boolean isShieldSelectionByContent(DecisionContext context) {
        GameState gameState = context.getGameState();
        List<String> cardIds = context.getCardIds();
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());

        if (cardIds == null || cardIds.isEmpty()) {
            return false;
        }

        int shieldCount = 0;
        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            try {
                SwccgCardBlueprint blueprint = null;

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    // V29.5: ARBITRARY_CARDS — use blueprint ID lookup (temp IDs can't be parsed as ints)
                    String bpId = blueprintIds.get(idx);
                    blueprint = getBlueprintFromId(context, bpId);
                } else if (gameState != null) {
                    // Standard path — integer card ID
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        blueprint = card.getBlueprint();
                    }
                }

                if (blueprint != null &&
                    blueprint.getCardCategory() == CardCategory.DEFENSIVE_SHIELD) {
                    shieldCount++;
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // If majority are shields, treat as shield selection
        boolean isShield = ShieldPolicy.isShieldSelection(shieldCount, cardIds.size());
        if (isShield) {
            logger.warn("V29.5 isShieldSelectionByContent: YES — {}/{} cards are shields (isArbitrary={})",
                shieldCount, cardIds.size(), isArbitrary);
        }
        return isShield;
    }

    /**
     * Defensive shield selection - use ShieldStrategy scoring.
     */
    /**
     * V105/V117: is the fourth-slot preferred shield title
     * actually offered among THIS decision's candidates? The Verge game held the
     * fourth shield slot hostage all game: the policy returned "Battle Order"
     * (Rando occupied both theaters) but Battle Order was never in the candidate
     * list, so the old code hard-blocked every real shield at -5000 (2760 fires)
     * and the slot deployed nothing. When the preferred card is not on the menu,
     * the caller holds the slot closed instead of spamming a block for a card that
     * can't be picked. Scans the full candidate list from context (both the
     * ARBITRARY_CARDS blueprint path and the standard card-id path). Fails to
     * false (= treat as not offered = hold slot closed), the conservative side.
     */
    private boolean preferredShieldInCandidates(DecisionContext context, String preferredTitle) {
        if (context == null || preferredTitle == null) return false;
        String want = preferredTitle.toLowerCase(java.util.Locale.ROOT);
        try {
            List<String> pcIds = context.getCardIds();
            List<String> pcBps = context.getBlueprints();
            GameState pcGs = context.getGameState();
            int n = Math.max(pcIds != null ? pcIds.size() : 0,
                    pcBps != null ? pcBps.size() : 0);
            for (int i = 0; i < n; i++) {
                String t = null;
                if (pcGs != null && pcIds != null && i < pcIds.size()) {
                    try {
                        PhysicalCard c = pcGs.findCardById(Integer.parseInt(pcIds.get(i)));
                        if (c != null) t = c.getTitle();
                    } catch (NumberFormatException nfe) { /* skip unparseable */ }
                }
                if (t == null && pcBps != null && i < pcBps.size()) {
                    SwccgCardBlueprint bp = getBlueprintFromId(context, pcBps.get(i));
                    if (bp != null) t = bp.getTitle();
                }
                if (t != null && t.toLowerCase(java.util.Locale.ROOT).contains(want)) return true;
            }
        } catch (Exception e) {
            logger.debug("preferredShieldInCandidates error: {}", e.getMessage());
        }
        return false;
    }

    private List<EvaluatedAction> evaluateShieldSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        String decisionId = context.getDecisionId();
        GameState gameState = context.getGameState();
        ShieldStrategy shieldStrategy = context.getShieldStrategy();
        int turnNumber = context.getTurnNumber();
        List<String> blueprintIds = context.getBlueprints();
        boolean isArbitrary = "ARBITRARY_CARDS".equals(context.getDecisionType());

        logger.warn("[CardSelectionEvaluator] V29.5 Evaluating DEFENSIVE SHIELD selection (isArbitrary={}, shieldStrategy={})",
            isArbitrary, shieldStrategy != null ? "SET" : "NULL");

        List<String> cardIds = context.getCardIds();
        for (int idx = 0; idx < cardIds.size(); idx++) {
            String cardId = cardIds.get(idx);
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,  // Base score
                "Deploy shield"
            );

            try {
                String title = null;
                String blueprintId = null;
                SwccgCardBlueprint blueprint = null;

                if (isArbitrary && blueprintIds != null && idx < blueprintIds.size()) {
                    // V29.5: ARBITRARY_CARDS path — use blueprint ID (temp IDs can't be parsed)
                    blueprintId = blueprintIds.get(idx);
                    blueprint = getBlueprintFromId(context, blueprintId);
                    if (blueprint != null) {
                        title = blueprint.getTitle();
                        logger.warn("V29.5 SHIELD ARBITRARY: Resolved '{}' → '{}' (bp={})", cardId, title, blueprintId);
                    }
                } else if (gameState != null) {
                    // Standard path — integer card ID
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        title = card.getTitle();
                        blueprintId = card.getBlueprintId(true);
                        blueprint = card.getBlueprint();
                    }
                }

                if (title != null) {
                    action.setDisplayText("Shield: " + title);
                }

                // Verify it's actually a defensive shield
                if (blueprint != null &&
                    blueprint.getCardCategory() == CardCategory.DEFENSIVE_SHIELD) {

                    float shieldScore = 50.0f;
                    int minTurnToPlay = 0;
                    // Use ShieldStrategy for scoring
                    if (shieldStrategy != null && blueprintId != null && title != null) {
                        shieldScore = shieldStrategy.scoreShield(
                            blueprintId, title, turnNumber);

                        // Set score directly (ShieldStrategy fully controls priority)
                        action.setScore(shieldScore);
                        String description = shieldStrategy.getShieldDescription(blueprintId, title);
                        action.addReasoning("Shield: " + description, 0.0f);
                        minTurnToPlay = shieldStrategy.minTurnToPlay(blueprintId, title);

                        logger.warn("V29.5 [Shield] {}: score={} ({})", title, shieldScore, description);
                    } else {
                        // Fallback if no shield strategy
                        action.addReasoning("Defensive shield (no strategy)", 50.0f);
                        logger.warn("V29.5 [Shield] {}: NO STRATEGY — fallback score 100", title);
                    }

                    int shieldsOnTable = ShieldFacts.shieldsOnTable(
                            context.getGameState(), context.getPlayerId());
                    ShieldPolicy.FourthSlotPick fourthSlot =
                            new ShieldPolicy.FourthSlotPick(null, false,
                                    ShieldPolicy.FourthSlotTrigger.CLOSED);
                    if (shieldsOnTable >= 3) {
                        ShieldFacts.FourthSlotFacts fourthSlotFacts =
                                ShieldFacts.fourthSlotFacts(context.getGameState(),
                                        context.getGame(), context.getPlayerId());
                        if (shieldStrategy != null) {
                            fourthSlot = shieldStrategy.fourthSlotPick(fourthSlotFacts,
                                    preferred -> preferredShieldInCandidates(context, preferred));
                        } else {
                            fourthSlot = ShieldPolicy.fourthSlotPick(context.getSide(),
                                    fourthSlotFacts,
                                    preferred -> preferredShieldInCandidates(context, preferred));
                        }
                    }
                    // Hoth repair #2 (2026-07-27): corrected battleOrderLive law.
                    // OLD: ShieldFacts.occupiesBothTheaters(game, playerId)
                    boolean battleOrderLive = !ShieldPolicy.isBattleOrderOrPlan(title)
                            || ShieldFacts.battleOrderLive(
                                context.getGame(), context.getPlayerId(),
                                computeNetDrainBalance(context.getGame(),
                                    context.getGameState(),
                                    context.getPlayerId()) >= 2);
                    PolicyContributionLedger shieldLedger = new PolicyContributionLedger(
                            decisionId == null || decisionId.isBlank()
                                    ? "shield-selection-" + cardId
                                    : decisionId + "-shield-selection-" + cardId);
                    shieldLedger.register(ShieldPolicy.shieldCandidateAdjustments(
                            cardId, title, shieldScore, minTurnToPlay, turnNumber,
                            shieldsOnTable, fourthSlot, battleOrderLive,
                            ShieldFacts.battleOrderPlanEquivalentOnTable(
                                context.getGameState()),
                            ShieldPolicy.CandidateRoute.DEDICATED));
                    PolicyOperationAdapter.apply(action, shieldLedger);

                    if (shieldsOnTable >= 3) {
                        logger.warn("V105/V107 4TH SLOT: '{}' preferred={} pursue={}",
                                title, fourthSlot.preferred(), fourthSlot.pursue());
                    }
                    if (ShieldPolicy.isBattleOrderOrPlan(title)) {
                        logger.warn("V51 BATTLE ORDER (shield): battleOrderLive={} base={}",
                                battleOrderLive, shieldScore);
                    }
                } else if (blueprint != null) {
                    // Not a shield - low priority
                    action.addReasoning("Not a defensive shield", -50.0f);
                } else {
                    logger.warn("V29.5 [Shield] Could not resolve card '{}' (bp={})", cardId, blueprintId);
                    action.addReasoning("Unresolved shield card", 0.0f);
                }
            } catch (NumberFormatException e) {
                // V29.5: This shouldn't happen anymore for ARBITRARY_CARDS
                logger.warn("V29.5 [Shield] NumberFormatException for cardId '{}' — should use blueprint path", cardId);
                action.addReasoning("Invalid card ID (should not happen with V29.5)", -100.0f);
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Extract blueprint ID from decision text HTML.
     * GEMP decision text for deploy location includes the card being deployed
     * in format: <div class='cardHint' value='8_35'>CardName</div>
     *
     * @param decisionText the decision text which may contain HTML
     * @return the blueprint ID, or null if not found
     */
    private String extractBlueprintFromDecisionText(String decisionText) {
        if (decisionText == null || decisionText.isEmpty()) {
            return null;
        }

        // Pattern: value='8_35' or value="8_35"
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "value=['\"]([0-9]+_[0-9]+)['\"]",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        java.util.regex.Matcher matcher = pattern.matcher(decisionText);
        if (matcher.find()) {
            String blueprintId = matcher.group(1);
            logger.debug("Extracted blueprint {} from decision text", blueprintId);
            return blueprintId;
        }

        return null;
    }

    private Integer movePhysicalCardId(
            DecisionContext context) {
        if (context == null) return null;
        Object value = context.getExtra(
                MovePhysicalCardResolver
                    .MOVER_CARD_ID_EXTRA);
        if (value == null) return null;
        try {
            return Integer.valueOf(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer integerExtra(
            DecisionContext context,
            String key) {
        if (context == null || key == null) {
            return null;
        }
        Object value = context.getExtra(key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(
                String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private PhysicalCard findUniqueDeployingCard(
            DecisionContext context, GameState gameState,
            String playerId, String blueprintId) {
        if (gameState == null || playerId == null || blueprintId == null) return null;

        Object selectedPhysicalId = context != null
                ? context.getExtra(
                    com.gempukku.swccgo.ai.models.common.strategy
                        .ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA)
                : null;
        if (selectedPhysicalId != null) {
            try {
                PhysicalCard selected = gameState.findCardByPermanentId(
                        Integer.parseInt(
                            String.valueOf(selectedPhysicalId)));
                if (selected != null
                        && playerId.equals(selected.getOwner())
                        && blueprintId.equals(
                            selected.getBlueprintId(true))) {
                    return selected;
                }
            } catch (NumberFormatException ignored) {
                // Invalid provenance fails into the zone-safe fallback.
            }
        }

        java.util.Set<PhysicalCard> seen = java.util.Collections.newSetFromMap(
            new java.util.IdentityHashMap<>());
        java.util.List<PhysicalCard> candidates = new java.util.ArrayList<>();
        java.util.List<PhysicalCard> handCards = gameState.getHand(playerId);
        java.util.List<PhysicalCard> reserveCards = gameState.getCardPile(
            playerId, com.gempukku.swccgo.common.Zone.RESERVE_DECK);
        java.util.List<PhysicalCard> lostPileCards =
                gameState.getLostPile(playerId);
        java.util.List<PhysicalCard> stackedCards = gameState.getAllStackedCards();
        if (handCards != null) candidates.addAll(handCards);
        if (reserveCards != null) candidates.addAll(reserveCards);
        if (lostPileCards != null) candidates.addAll(lostPileCards);
        if (stackedCards != null) candidates.addAll(stackedCards);

        PhysicalCard match = null;
        for (PhysicalCard candidate : candidates) {
            if (candidate == null || !seen.add(candidate)) continue;
            if (!blueprintId.equals(candidate.getBlueprintId(true))) continue;
            if (match != null) return null;
            match = candidate;
        }
        return match;
    }

    private void applyInvasionPilotDestination(
            DecisionContext context, EvaluatedAction action,
            String deployingBlueprintId, PhysicalCard destination) {
        if (context == null || action == null || deployingBlueprintId == null
                || destination == null || destination.getTitle() == null
                || context.getGameState() == null || context.getGame() == null) {
            return;
        }
        try {
            com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer analyzer =
                    context.getObjectiveAnalyzer();
            if (analyzer == null || !analyzer.isAnalyzed()
                    || !analyzer.isInvasion()) {
                return;
            }
            SwccgCardBlueprint deployingBlueprint =
                    findDeployingBlueprint(
                        context, context.getGameState(),
                        context.getPlayerId(), deployingBlueprintId);
            if (deployingBlueprint == null
                    || deployingBlueprint.getSpecies()
                        != com.gempukku.swccgo.common.Species.NEIMOIDIAN
                    || !deployingBlueprint.hasIcon(
                        com.gempukku.swccgo.common.Icon.PILOT)) {
                return;
            }
            PhysicalCard capitalShip = null;
            for (PhysicalCard card : context.getGameState()
                    .getAllPermanentCards()) {
                if (card == null || card.getBlueprint() == null
                        || !context.getPlayerId().equals(card.getOwner())) {
                    continue;
                }
                if (com.gempukku.swccgo.filters.Filters.capital_starship.accepts(
                        context.getGameState(),
                        context.getGame().getModifiersQuerying(), card)) {
                    capitalShip = card;
                    break;
                }
            }
            if (capitalShip == null) {
                return;
            }
            String capitalTitle = capitalShip.getTitle() != null
                    ? capitalShip.getTitle() : "";
            boolean correctDestination =
                    destination.getTitle().toLowerCase(java.util.Locale.ROOT)
                        .contains(capitalTitle.toLowerCase(java.util.Locale.ROOT));
            applyDeployPilotPolicy(
                action,
                DeployPilotShipPolicy.evaluateObjectivePilotDestination(
                    new DeployPilotShipPolicy.ObjectivePilotDestinationFacts(
                        action.getActionId(), true, capitalTitle,
                        destination.getTitle(), correctDestination)));
            if (!correctDestination) {
                logger.warn(
                    "V121 INVASION CS: blocking Neimoidian pilot → {} (not aboard {}) -1500",
                    destination.getTitle(), capitalTitle);
            }
        } catch (Exception e) {
            logger.debug("V121 INVASION CS error: {}", e.getMessage());
        }
    }

    private SwccgCardBlueprint findDeployingBlueprint(
            DecisionContext context, GameState gameState,
            String playerId, String blueprintId) {
        if (gameState != null && playerId != null && blueprintId != null) {
            java.util.List<PhysicalCard> handCards = gameState.getHand(playerId);
            if (handCards != null) {
                for (PhysicalCard card : handCards) {
                    if (card != null && card.getBlueprint() != null
                            && (blueprintId.equals(card.getBlueprintId(true))
                                || blueprintId.equals(card.getBlueprintId(false)))) {
                        return card.getBlueprint();
                    }
                }
            }
        }
        return getBlueprintFromId(context, blueprintId);
    }

    static PhysicalCard findPullCandidate(
            DecisionContext context,
            String cardId, String blueprintId) {
        if (context == null) return null;
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();
        if (gameState == null || playerId == null || blueprintId == null
                || blueprintId.isBlank() || "inPlay".equals(blueprintId)) {
            return null;
        }
        if (cardId != null) {
            try {
                PhysicalCard exact = gameState.findCardById(
                        Integer.parseInt(cardId));
                if (exact != null
                        && playerId.equals(exact.getOwner())
                        && blueprintId.equals(
                            exact.getBlueprintId(true))) {
                    return exact;
                }
            } catch (NumberFormatException ignored) {
                // Temporary arbitrary-choice ids need the blueprint fallback.
            }
        }
        java.util.List<PhysicalCard> reserveCards = gameState.getCardPile(
                playerId, com.gempukku.swccgo.common.Zone.RESERVE_DECK);
        if (cardId != null && cardId.startsWith("temp")) {
            List<String> offeredCardIds = context.getCardIds();
            List<String> offeredBlueprints = context.getBlueprints();
            int offeredIndex = offeredCardIds != null
                    ? offeredCardIds.indexOf(cardId) : -1;
            if (offeredIndex < 0 || offeredBlueprints == null
                    || offeredIndex >= offeredBlueprints.size()
                    || !cardId.equals("temp" + offeredIndex)) {
                return null;
            }
            boolean reserveOrderMatches = reserveCards != null
                    && reserveCards.size() == offeredBlueprints.size();
            for (int index = 0;
                    reserveOrderMatches
                            && index < reserveCards.size();
                    index++) {
                PhysicalCard reserveCard = reserveCards.get(index);
                reserveOrderMatches = reserveCard != null
                        && offeredBlueprints.get(index).equals(
                                reserveCard.getBlueprintId(true));
            }
            if (reserveOrderMatches) {
                PhysicalCard exact = reserveCards.get(offeredIndex);
                if (exact != null
                        && playerId.equals(exact.getOwner())
                        && blueprintId.equals(
                                exact.getBlueprintId(true))) {
                    return exact;
                }
            }
        }
        java.util.List<PhysicalCard> candidates = new java.util.ArrayList<>();
        java.util.List<PhysicalCard> handCards = gameState.getHand(playerId);
        if (reserveCards != null) candidates.addAll(reserveCards);
        if (handCards != null) candidates.addAll(handCards);
        PhysicalCard unique = null;
        for (PhysicalCard candidate : candidates) {
            if (candidate != null
                    && blueprintId.equals(candidate.getBlueprintId(true))) {
                if (unique != null) return null;
                unique = candidate;
            }
        }
        return unique;
    }

    // ====================================================================
    // V70 helpers (Steve, 2026-05-12) — Universal one-weapon-per-character.
    //
    // Per Steve: when a card's game text pulls or deploys a weapon or device,
    // extract the criteria from that game text (don't hardcode keyword maps).
    // Search Rando friendlies comprehensively (title, lore, gametext, dynamic
    // card types, subtype, icons, keywords, persona) and check whether the
    // pull would safely land on an unarmed character. If every "applicable"
    // friendly is armed → block.
    // ====================================================================

    /**
     * Extracts the deploy-restriction criteria word from a card's game text.
     * Matches patterns like "Deploy on a Sith.", "Deploys only on Vader."
     * Returns the lower-cased criteria word, or null if none found.
     */
    // V115 (Steve, 2026-05-22): widened visibility so DeployEvaluator's V67aq can
    // reuse the same criteria parser for hand-deploy weapon scoring.
    static String v70ExtractDeployCriteria(String gameText) {
        if (gameText == null) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
            "(?i)deploys?\\s+(?:only\\s+)?on\\s+(?:a|an|the|your)?\\s*([a-z][a-z\\s'-]{2,30}?)\\s*[.,;]"
        ).matcher(gameText);
        if (m.find()) {
            String c = m.group(1).trim();
            if (!c.isEmpty()) return c.toLowerCase(java.util.Locale.ROOT);
        }
        return null;
    }

    /**
     * Returns true if the given character's attributes contain the criteria
     * word in title, lore, game text, dynamic card types (engine-aware),
     * subtype, icons, keywords, or personas.
     */
    // V115 (Steve, 2026-05-22): widened visibility so DeployEvaluator's V67aq can
    // reuse the same criteria-matcher for hand-deploy weapon scoring.
    static boolean v70CharacterMatchesCriteria(SwccgGame game, GameState gs, PhysicalCard pc, String criteria) {
        if (pc == null || pc.getBlueprint() == null || criteria == null || criteria.isEmpty()) return false;
        SwccgCardBlueprint bp = pc.getBlueprint();
        String c = criteria.toLowerCase(java.util.Locale.ROOT);

        // 1. Title
        if (bp.getTitle() != null && bp.getTitle().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
        // 2. Lore
        if (bp.getLore() != null && bp.getLore().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
        // 3. Game text
        if (bp.getGameText() != null && bp.getGameText().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
        // 4. Dynamic card types (includes modifier-added types — e.g., Revenge Of
        //    The Sith adds CardType.SITH to Lord Sidious at runtime)
        java.util.Set<com.gempukku.swccgo.common.CardType> types = null;
        try {
            if (game != null && gs != null) {
                types = game.getModifiersQuerying().getCardTypes(gs, pc);
            }
        } catch (Exception ignored) { }
        if (types == null) types = bp.getCardTypes();
        if (types != null) {
            for (com.gempukku.swccgo.common.CardType ct : types) {
                String n = (ct.getHumanReadable() != null) ? ct.getHumanReadable() : ct.name();
                if (n.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').contains(c)) return true;
            }
        }
        // 5. Subtype
        com.gempukku.swccgo.common.CardSubtype st = bp.getCardSubtype();
        if (st != null) {
            String n = (st.getHumanReadable() != null) ? st.getHumanReadable() : st.name();
            if (n.toLowerCase(java.util.Locale.ROOT).replace('_', ' ').contains(c)) return true;
        }
        // 6. Personas
        java.util.Set<com.gempukku.swccgo.common.Persona> personas = bp.getPersonas();
        if (personas != null) {
            for (com.gempukku.swccgo.common.Persona p : personas) {
                if (p.name().toLowerCase(java.util.Locale.ROOT).contains(c)) return true;
            }
        }
        // 7. Icons (iterate enum, check hasIcon)
        for (com.gempukku.swccgo.common.Icon icon : com.gempukku.swccgo.common.Icon.values()) {
            if (icon.name().toLowerCase(java.util.Locale.ROOT).contains(c)) {
                if (bp.hasIcon(icon)) return true;
            }
        }
        // 8. Keywords (iterate enum, check hasKeyword)
        for (com.gempukku.swccgo.common.Keyword kw : com.gempukku.swccgo.common.Keyword.values()) {
            if (kw.name().toLowerCase(java.util.Locale.ROOT).contains(c)) {
                if (bp.hasKeyword(kw)) return true;
            }
        }
        return false;
    }

    /**
     * Counts friendlies for the V70 weapon block.
     * Returns int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed}
     * where "matching" means the character's attributes contain the criteria
     * word (or, if criteria is null, every friendly counts as matching).
     */
    private static int[] v70CountFriendlies(SwccgGame game, String playerId, String criteria) {
        int matchingArmed = 0, matchingUnarmed = 0, totalArmed = 0, totalUnarmed = 0;
        if (game == null || playerId == null) {
            return new int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed};
        }
        GameState gs = game.getGameState();
        if (gs == null) {
            return new int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed};
        }
        try {
            for (PhysicalCard pc : gs.getAllPermanentCards()) {
                if (pc == null || pc.getBlueprint() == null) continue;
                if (!playerId.equals(pc.getOwner())) continue;
                com.gempukku.swccgo.common.Zone z = pc.getZone();
                if (z == null || !z.isInPlay()) continue;
                if (pc.getBlueprint().getCardCategory() != CardCategory.CHARACTER) continue;
                boolean armed = false;
                java.util.List<PhysicalCard> atts = gs.getAttachedCards(pc);
                if (atts != null) {
                    for (PhysicalCard a : atts) {
                        if (a != null && a.getBlueprint() != null
                                && a.getBlueprint().getCardCategory() == CardCategory.WEAPON) {
                            armed = true;
                            break;
                        }
                    }
                }
                if (armed) totalArmed++; else totalUnarmed++;
                if (criteria == null || v70CharacterMatchesCriteria(game, gs, pc, criteria)) {
                    if (armed) matchingArmed++; else matchingUnarmed++;
                }
            }
        } catch (Exception ignored) { }
        return new int[]{matchingArmed, matchingUnarmed, totalArmed, totalUnarmed};
    }

    /**
     * Returns the V70 block reason if the candidate (weapon or device) should
     * be hard-blocked, else null. The candidate's game text is parsed for
     * deploy criteria; matching friendlies are checked for armed status.
     */
    static String v70CheckWeaponDeviceBlock(SwccgGame game, String playerId,
                                            CardCategory candidateCategory,
                                            SwccgCardBlueprint candidateBp) {
        if (candidateCategory != CardCategory.WEAPON && candidateCategory != CardCategory.DEVICE) return null;
        if (candidateBp == null || game == null || playerId == null) return null;

        String criteria = v70ExtractDeployCriteria(candidateBp.getGameText());
        int[] counts = v70CountFriendlies(game, playerId, criteria);
        int matchingArmed = counts[0], matchingUnarmed = counts[1], totalArmed = counts[2], totalUnarmed = counts[3];

        // STRICT: criteria parsed AND all matching are armed → BLOCK
        if (criteria != null && matchingArmed > 0 && matchingUnarmed == 0) {
            return String.format("every applicable '%s' friendly (%d) already armed", criteria, matchingArmed);
        }
        // DEFENSIVE: criteria parsed AND no friendlies matched, but some armed (engine may have broader interp)
        if (criteria != null && matchingArmed == 0 && matchingUnarmed == 0 && totalArmed > 0) {
            return String.format("no '%s' friendly matched our comprehensive search but %d friendly char(s) armed (defensive — engine may have broader interpretation)", criteria, totalArmed);
        }
        // V72 (Steve, 2026-05-15): RELAXED criteria==null fallback.
        // Previous rule blocked ALL weapon pulls once any friendly was armed,
        // even when unarmed friendlies existed. That left Yoda/Leia/Hera/Obi
        // unarmed in the May 15 game once Sabine got Ahsoka's Shoto. Now:
        //   - If any unarmed friendly exists → ALLOW (weapon will land on
        //     an unarmed char, or if it lands on an armed char V72 redistribute
        //     transfers it to the unarmed buddy next turn)
        //   - If ALL friendlies are armed → BLOCK (truly no place to put it)
        if (criteria == null && totalUnarmed == 0 && totalArmed > 0) {
            return String.format("no parseable deploy criteria and ALL %d friendly char(s) armed — no unarmed target", totalArmed);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════
    // V208: shared BattleForfeitFacts + BattleForfeitPolicy own BATTLE-3 scoring.
}
