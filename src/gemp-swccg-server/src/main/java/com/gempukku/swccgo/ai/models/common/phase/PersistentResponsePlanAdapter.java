package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.models.common.strategy.FormationSafety;
import com.gempukku.swccgo.ai.models.common.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Engine-aware fact adapter for the shared persistent-response policy.
 * Bot-specific planners supply only immutable plan and instruction views.
 */
public final class PersistentResponsePlanAdapter {
    private static final Logger LOG =
            LogManager.getLogger(PersistentResponsePlanAdapter.class);

    private PersistentResponsePlanAdapter() {
    }

    public record InstructionView(Integer permanentCardId,
                                  Integer currentCardId,
                                  String targetLocationCardId,
                                  int priority) {
    }

    /** One offered CARD_ACTION_CHOICE row after local deploy scoring. */
    public record OuterActionView(String actionId,
                                  String actionText,
                                  String sourceCardId,
                                  boolean selectable,
                                  boolean hardVetoed,
                                  boolean deferred) {
    }

    public record PlanView<P>(P source,
                              String domain,
                              String strategy,
                              List<InstructionView> instructions) {
        public PlanView {
            Objects.requireNonNull(source, "source");
            domain = domain != null ? domain : "unknown";
            strategy = strategy != null ? strategy : "unknown";
            instructions = List.copyOf(Objects.requireNonNull(
                    instructions, "instructions"));
        }
    }

    public record Input<P>(SwccgGame game,
                           String playerId,
                           ObjectiveAnalyzer objectiveAnalyzer,
                           PersistentResponsePolicy.Snapshot snapshot,
                           List<AiBoardAnalyzer.LocationAnalysis> locations,
                           int ordinaryBudgetAfterLocationPrelude,
                           int objectiveBudgetAfterLocationPrelude,
                           List<PlanView<P>> plans) {
        public Input {
            Objects.requireNonNull(game, "game");
            Objects.requireNonNull(playerId, "playerId");
            Objects.requireNonNull(snapshot, "snapshot");
            locations = List.copyOf(Objects.requireNonNull(
                    locations, "locations"));
            plans = List.copyOf(Objects.requireNonNull(plans, "plans"));
            if (ordinaryBudgetAfterLocationPrelude < 0
                    || objectiveBudgetAfterLocationPrelude < 0) {
                throw new IllegalArgumentException(
                        "post-prelude budgets must be nonnegative");
            }
        }
    }

    public record Selection<P>(P source,
                               PersistentResponsePolicy.Obligation obligation) {
        public Selection {
            Objects.requireNonNull(source, "source");
            Objects.requireNonNull(obligation, "obligation");
        }
    }

    private record Candidate<P>(P source,
                                PersistentResponsePolicy.CandidateFacts facts) {
    }

    /** Selects the exact source plan identified by the shared typed policy. */
    public static <P> Optional<Selection<P>> select(Input<P> input) {
        Objects.requireNonNull(input, "input");
        if (input.plans().isEmpty()) return Optional.empty();

        List<Candidate<P>> candidates = new ArrayList<>();
        for (PlanView<P> plan : input.plans()) {
            candidates.addAll(buildCandidates(input, plan));
        }

        Map<Integer, PersistentResponsePolicy.LocationKey> threats =
                new LinkedHashMap<>();
        for (Candidate<P> candidate : candidates) {
            if (candidate.facts().kind()
                    == PersistentResponsePolicy.CandidateKind
                    .RESPONSE_TARGET) {
                threats.putIfAbsent(
                        candidate.facts().threatLocation()
                                .permanentCardId(),
                        candidate.facts().threatLocation());
            }
        }
        for (PlanView<P> plan : input.plans()) {
            candidates.addAll(buildAlternatives(
                    input, plan, threats.values()));
        }

        Map<PersistentResponsePolicy.CandidateKey, Candidate<P>> byKey =
                new LinkedHashMap<>();
        for (Candidate<P> candidate : candidates) {
            if (byKey.putIfAbsent(candidate.facts().candidateKey(),
                    candidate) != null) {
                LOG.warn("Persistent response candidate-key collision; "
                                + "keeping legacy planner selection: {}",
                        candidate.facts().candidateKey().value());
                return Optional.empty();
            }
        }

        Optional<PersistentResponsePolicy.Obligation> obligation =
                PersistentResponsePolicy.select(candidates.stream()
                        .map(Candidate::facts).toList());
        if (obligation.isEmpty()) return Optional.empty();
        Candidate<P> selected = byKey.get(
                obligation.get().candidateKey());
        return selected != null
                ? Optional.of(new Selection<>(
                        selected.source(), obligation.get()))
                : Optional.empty();
    }

    /**
     * Maps the next exact proved wave member to one currently offered direct
     * Deploy action. Legality and exact current cost are rechecked here; an
     * ambiguous or stale row fails closed and leaves the legacy DPS walk alone.
     */
    public static Optional<PersistentResponsePolicy.OfferedOuterAction>
    findNextOfferedResponseAction(
            SwccgGame game, String playerId,
            ObjectiveAnalyzer objectiveAnalyzer,
            PersistentResponsePolicy.Snapshot snapshot,
            PersistentResponsePolicy.Obligation obligation,
            InstructionView plannedInstruction,
            int availableForce,
            List<OuterActionView> offeredActions) {
        if (game == null || playerId == null || objectiveAnalyzer == null
                || snapshot == null || obligation == null
                || plannedInstruction == null || availableForce < 0
                || offeredActions == null
                || obligation.kind()
                != PersistentResponsePolicy.CandidateKind.RESPONSE_TARGET
                || obligation.responseAction() == null
                || plannedInstruction.permanentCardId() == null
                || plannedInstruction.currentCardId() == null
                || plannedInstruction.targetLocationCardId() == null
                || plannedInstruction.permanentCardId()
                != obligation.responseAction().permanentCardId()
                || plannedInstruction.currentCardId()
                != obligation.responseAction().currentCardId()) {
            return Optional.empty();
        }
        GameState gameState = game.getGameState();
        if (gameState == null
                || !playerId.equals(gameState.getCurrentPlayerId())
                || gameState.getCurrentPhase() != Phase.DEPLOY) {
            return Optional.empty();
        }
        PhysicalCard card = cardByIdentity(
                gameState, playerId, plannedInstruction);
        PhysicalCard target;
        try {
            target = gameState.findCardById(Integer.parseInt(
                    plannedInstruction.targetLocationCardId()));
        } catch (NumberFormatException ignored) {
            return Optional.empty();
        }
        if (card == null || target == null
                || target.getPermanentCardId()
                != obligation.responseTargetLocation().permanentCardId()
                || !hasCurrentResponseReason(
                        game, playerId, objectiveAnalyzer, snapshot,
                        obligation, target)
                || !canDeployPaidDirectly(game, card, target)) {
            return Optional.empty();
        }
        Integer exactCost = exactDeployCost(game, card, target);
        if (exactCost == null || exactCost > availableForce) {
            return Optional.empty();
        }

        List<OuterActionView> matches = new ArrayList<>();
        for (OuterActionView offered : offeredActions) {
            if (offered == null || offered.actionId() == null
                    || offered.actionId().isBlank()
                    || offered.actionText() == null
                    || !offered.actionText().toLowerCase(Locale.ROOT)
                    .contains("deploy")
                    || offered.sourceCardId() == null) {
                continue;
            }
            try {
                PhysicalCard offeredCard = gameState.findCardById(
                        Integer.parseInt(offered.sourceCardId()));
                if (offeredCard != null
                        && offeredCard.getPermanentCardId()
                        == card.getPermanentCardId()
                        && offeredCard.getCardId() == card.getCardId()) {
                    matches.add(offered);
                }
            } catch (NumberFormatException ignored) {
                // An opaque or malformed source id is not physical proof.
            }
        }
        if (matches.size() != 1) return Optional.empty();
        OuterActionView selected = matches.get(0);
        return Optional.of(
                new PersistentResponsePolicy.OfferedOuterAction(
                        selected.actionId(), obligation.responseAction(),
                        true, selected.selectable(), true, true, true,
                        selected.hardVetoed(), selected.deferred()));
    }

    /** Live target gate shared by outer-action and destination adapters. */
    public static boolean hasCurrentResponseReason(
            SwccgGame game, String playerId,
            ObjectiveAnalyzer objectiveAnalyzer,
            PersistentResponsePolicy.Snapshot snapshot,
            PersistentResponsePolicy.Obligation obligation,
            PhysicalCard responseTarget) {
        if (game == null || playerId == null || objectiveAnalyzer == null
                || snapshot == null || obligation == null
                || responseTarget == null
                || obligation.kind()
                != PersistentResponsePolicy.CandidateKind.RESPONSE_TARGET
                || !hasOpponentBattleParticipant(
                        game, playerId, objectiveAnalyzer,
                        responseTarget)) {
            return false;
        }
        boolean persistentCurrent = obligation.persistentBonus() > 0
                && snapshot.repeatedThreatAt(
                        obligation.threatLocation().permanentCardId())
                .isPresent();
        boolean criticalCurrent = obligation.criticalBonus() > 0
                && matchesCurrentTargetRole(
                        game, playerId, objectiveAnalyzer, responseTarget,
                        obligation.role());
        return persistentCurrent || criticalCurrent;
    }

    private static <P> List<Candidate<P>> buildCandidates(
            Input<P> input, PlanView<P> plan) {
        if (plan.instructions().isEmpty()) return List.of();
        Map<String, List<InstructionView>> byTarget =
                new LinkedHashMap<>();
        for (InstructionView instruction : plan.instructions()) {
            if (instruction.targetLocationCardId() != null) {
                byTarget.computeIfAbsent(
                        instruction.targetLocationCardId(),
                        ignored -> new ArrayList<>()).add(instruction);
            }
        }

        List<Candidate<P>> candidates = new ArrayList<>();
        for (Map.Entry<String, List<InstructionView>> entry
                : byTarget.entrySet()) {
            AiBoardAnalyzer.LocationAnalysis target =
                    findLocation(input.locations(), entry.getKey());
            if (target == null || target.location == null
                    || target.location.getPermanentCardId() <= 0) {
                continue;
            }
            List<PhysicalCard> targetCards = exactCards(
                    input, entry.getValue());
            if (targetCards.size() != entry.getValue().size()) continue;

            PersistentResponsePolicy.TargetRole role = targetRole(
                    input.game(), input.playerId(),
                    input.objectiveAnalyzer(), target.location);
            boolean mandatory = completesMandatoryNeed(
                    input, role, target.location, targetCards);
            PersistentResponsePolicy.ExecutionProof execution =
                    proveExecution(input, entry.getValue(),
                            mandatory
                                    ? input.objectiveBudgetAfterLocationPrelude()
                                    : input.ordinaryBudgetAfterLocationPrelude(),
                            plan.instructions().size());
            boolean opponentPresent = hasOpponentBattleParticipant(
                    input.game(), input.playerId(),
                    input.objectiveAnalyzer(), target.location);
            PersistentResponsePolicy.DrainHistory threat =
                    input.snapshot().repeatedThreatAt(
                            target.location.getPermanentCardId())
                            .orElse(null);
            int strategicIncome = projectedDrain(
                    input.game(), input.playerId(), target.location);
            PersistentResponsePolicy.FormationProof formation =
                    assessFormation(input.game(), input.playerId(),
                            target, plan, entry.getValue(), targetCards,
                            opponentPresent);
            InstructionView lead = firstInstruction(entry.getValue());
            PersistentResponsePolicy.DeployActionKey actionKey =
                    actionKey(lead);
            List<PersistentResponsePolicy.DeployActionKey> actionKeys =
                    responseActionKeys(entry.getValue());
            boolean responseFact = actionKey != null
                    && actionKeys.size() == entry.getValue().size()
                    && opponentPresent
                    && (threat != null || isCritical(role));
            boolean responseEligible = responseFact
                    && formation.responseViable()
                    && (formation.route()
                    != DeployTacticalPolicy.ResponseFormationRoute.V170_SPY
                    || threat != null);

            if (responseEligible) {
                PersistentResponsePolicy.TargetRole effectiveRole = role;
                if (!isCritical(effectiveRole)) {
                    effectiveRole = PersistentResponsePolicy.TargetRole
                            .PERSISTENT_DAMAGE;
                }
                PersistentResponsePolicy.LocationKey locationKey =
                        locationKey(target.location);
                PersistentResponsePolicy.CandidateFacts facts =
                        new PersistentResponsePolicy.CandidateFacts(
                                candidateKey("response",
                                        target.location.getPermanentCardId(),
                                        target.location, plan, actionKey,
                                        input.locations()),
                                PersistentResponsePolicy.CandidateKind
                                        .RESPONSE_TARGET,
                                actionKeys, locationKey, locationKey,
                                responseMode(target, formation),
                                effectiveRole,
                                threat != null
                                        ? threat.consecutiveOpponentTurns()
                                        : 0,
                                threat != null
                                        ? threat.projectedTwoTurnDamage()
                                        : 0,
                                strategicIncome, 0, 0,
                                true, mandatory, execution, formation);
                candidates.add(new Candidate<>(plan.source(), facts));
            } else if (mandatory) {
                PersistentResponsePolicy.LocationKey locationKey =
                        locationKey(target.location);
                PersistentResponsePolicy.CandidateFacts facts =
                        new PersistentResponsePolicy.CandidateFacts(
                                candidateKey("mandatory",
                                        target.location.getPermanentCardId(),
                                        target.location, plan, null,
                                        input.locations()),
                                PersistentResponsePolicy.CandidateKind
                                        .EXISTING_PLAN,
                                List.of(), locationKey, locationKey,
                                PersistentResponsePolicy.Mode.MASS_TOWARD,
                                role, 0, 0, strategicIncome, 0, 0,
                                false, true, execution,
                                emptyFormation());
                candidates.add(new Candidate<>(plan.source(), facts));
            }
        }
        return candidates;
    }

    private static <P> List<Candidate<P>> buildAlternatives(
            Input<P> input, PlanView<P> plan,
            Collection<PersistentResponsePolicy.LocationKey> threats) {
        if (plan.instructions().isEmpty() || threats.isEmpty()) {
            return List.of();
        }
        Set<String> targetIds = plan.instructions().stream()
                .map(InstructionView::targetLocationCardId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (targetIds.size() != 1) return List.of();
        AiBoardAnalyzer.LocationAnalysis target = findLocation(
                input.locations(), targetIds.iterator().next());
        if (target == null || target.location == null
                || hasOpponentBattleParticipant(input.game(),
                input.playerId(), input.objectiveAnalyzer(),
                target.location)) {
            return List.of();
        }
        PersistentResponsePolicy.ExecutionProof execution =
                proveExecution(input, plan.instructions(),
                        input.ordinaryBudgetAfterLocationPrelude(),
                        plan.instructions().size());
        int currentIncome = currentFriendlyDrain(
                input.game(), input.playerId(), target.location);
        int raceValue = Math.max(0, projectedDrain(
                input.game(), input.playerId(), target.location) * 2);
        if (currentIncome <= 0 && raceValue <= 0) return List.of();

        List<Candidate<P>> alternatives = new ArrayList<>();
        for (PersistentResponsePolicy.LocationKey threat : threats) {
            if (threat.permanentCardId()
                    == target.location.getPermanentCardId()) {
                continue;
            }
            PersistentResponsePolicy.CandidateFacts facts =
                    new PersistentResponsePolicy.CandidateFacts(
                            candidateKey("race", threat.permanentCardId(),
                                    target.location, plan, null,
                                    input.locations()),
                            PersistentResponsePolicy.CandidateKind
                                    .EXISTING_PLAN,
                            List.of(), threat,
                            locationKey(target.location),
                            PersistentResponsePolicy.Mode.RACE,
                            PersistentResponsePolicy.TargetRole.NONE,
                            0, 0, 0, currentIncome, raceValue,
                            false, false, execution,
                            new PersistentResponsePolicy.FormationProof(
                                    0.0f, 0.0f, 0.0f, 0.0f,
                                    DeployTacticalPolicy
                                            .ResponseFormationRoute
                                            .EXISTING_LEGAL_ALTERNATIVE,
                                    false));
            alternatives.add(new Candidate<>(plan.source(), facts));
        }
        return alternatives;
    }

    private static <P> PersistentResponsePolicy.ExecutionProof
    proveExecution(Input<P> input,
                   List<InstructionView> responseGroup,
                   int budget,
                   int sourcePlanInstructionCount) {
        GameState gameState = input.game().getGameState();
        boolean timely = gameState != null
                && input.playerId().equals(gameState.getCurrentPlayerId())
                && gameState.getCurrentPhase() == Phase.DEPLOY;
        boolean available = gameState != null
                && responseGroup != null && !responseGroup.isEmpty();
        boolean legal = available;
        boolean affordable = available;
        int totalCost = 0;
        Set<String> seenCards = new HashSet<>();
        if (available) {
            for (InstructionView instruction : responseGroup) {
                PhysicalCard card = exactCard(input, instruction);
                AiBoardAnalyzer.LocationAnalysis target = findLocation(
                        input.locations(),
                        instruction.targetLocationCardId());
                String identity = instruction.permanentCardId()
                        + ":" + instruction.currentCardId();
                if (card == null || !seenCards.add(identity)) {
                    available = false;
                }
                if (card == null || target == null
                        || !canDeployPaidDirectly(input.game(),
                        card, target.location)) {
                    legal = false;
                    continue;
                }
                Integer exactCost = exactDeployCost(
                        input.game(), card, target.location);
                if (exactCost == null) {
                    affordable = false;
                } else {
                    totalCost += exactCost;
                    if (totalCost > budget) affordable = false;
                }
            }
        }
        return new PersistentResponsePolicy.ExecutionProof(
                legal, available, affordable, timely, totalCost,
                Math.max(0, sourcePlanInstructionCount));
    }

    /**
     * Whole-formation proof shared by both planner mirrors. Planned ground
     * power and ability use public printed attributes only. Unsupported card
     * categories and modifier-dependent packages fail closed. Space admission
     * is intentionally narrow: one permanently piloted ship in a shipped V296
     * bleed/reinforce plan. Mixed, multi-ship, and inferred crew packages fail.
     */
    public static PersistentResponsePolicy.FormationProof assessFormation(
            SwccgGame game, String playerId,
            AiBoardAnalyzer.LocationAnalysis target,
            PlanView<?> plan, List<InstructionView> instructions,
            List<PhysicalCard> plannedCards, boolean opponentPresent) {
        if (game == null || playerId == null || target == null
                || target.location == null || plan == null
                || instructions == null || instructions.isEmpty()
                || plannedCards == null || plannedCards.isEmpty()) {
            return emptyFormation();
        }
        InstructionView leadInstruction = firstInstruction(instructions);
        PhysicalCard lead = cardByIdentity(
                game.getGameState(), playerId, leadInstruction);
        if (lead == null || lead.getBlueprint() == null) {
            return emptyFormation();
        }

        int plannedPower = 0;
        int plannedAbility = 0;
        int maxPower = 0;
        boolean allCharacters = true;
        boolean allStarships = true;
        for (PhysicalCard card : plannedCards) {
            int power = printedPower(card);
            int ability = printedAbility(card);
            plannedPower += power;
            plannedAbility += ability;
            maxPower = Math.max(maxPower, power);
            CardCategory category = card.getBlueprint() != null
                    ? card.getBlueprint().getCardCategory() : null;
            allCharacters &= category == CardCategory.CHARACTER;
            allStarships &= category == CardCategory.STARSHIP;
        }

        float opponentEffective = target.theirPower
                + opponentWeaponBonus(game, playerId, target.location);
        boolean alreadyWinning = opponentEffective > 0.0f
                && target.ourPower >= opponentEffective;
        DeployTacticalPolicy.ResponseFormationRoute route =
                DeployTacticalPolicy.ResponseFormationRoute.NONE;

        if (allCharacters && plannedCards.size() == 1
                && lead.getBlueprint().hasKeyword(Keyword.SPY)) {
            route = DeployTacticalPolicy.ResponseFormationRoute.V170_SPY;
        } else if (allCharacters) {
            int armedOpponents = countArmedOpponents(
                    game, playerId, target.location);
            DeployTacticalPolicy.ResponseFormationAssessment assessment =
                    DeployTacticalPolicy.assessPersistentResponseFormation(
                            new DeployTacticalPolicy.ContactFacts(
                                    "persistent-plan",
                                    target.location.getTitle(),
                                    opponentPresent, true,
                                    plannedCards.size(), target.ourPower,
                                    printedPower(lead),
                                    Math.max(0, plannedPower
                                            - printedPower(lead)),
                                    Math.max(0,
                                            plannedCards.size() - 1),
                                    0.0f, opponentEffective, maxPower,
                                    armedOpponents),
                            target.ourAbility, plannedAbility);
            route = assessment.route();
        } else if (allStarships && plannedCards.size() == 1
                && instructions.size() == 1
                && plan.instructions().size() == 1
                && ("space_bleed".equals(plan.domain())
                || "space_reinforce".equals(plan.domain()))) {
            PhysicalCard ship = plannedCards.get(0);
            boolean permanentPilot = hasPermanentPilot(game, ship);
            float operationalAbility = permanentPilot
                    ? exactAbility(game, ship, true) : 0.0f;
            DeployTacticalPolicy.ResponseFormationAssessment assessment =
                    DeployTacticalPolicy.assessPersistentSpaceResponse(
                            new DeployTacticalPolicy
                                    .StarshipDrainContactFacts(
                                    "persistent-space-plan",
                                    game.getGameState()
                                            .getOpponent(playerId),
                                    target.location.getTitle(),
                                    target.theirPower, target.ourPower,
                                    printedPower(ship),
                                    projectedOpponentDrain(
                                            game, playerId,
                                            target.location)),
                            permanentPilot && operationalAbility > 0.0f,
                            target.ourAbility, operationalAbility);
            route = assessment.route();
            plannedAbility = Math.round(operationalAbility);
        }

        return new PersistentResponsePolicy.FormationProof(
                target.ourPower, target.ourAbility,
                plannedPower, plannedAbility, route, alreadyWinning);
    }

    public static PersistentResponsePolicy.Mode responseMode(
            AiBoardAnalyzer.LocationAnalysis target,
            PersistentResponsePolicy.FormationProof formation) {
        if (formation.route()
                == DeployTacticalPolicy.ResponseFormationRoute.V170_SPY) {
            return PersistentResponsePolicy.Mode.SPY;
        }
        return target.ourPower > 0.0f
                ? PersistentResponsePolicy.Mode.REINFORCE
                : PersistentResponsePolicy.Mode.CONTEST;
    }

    /** Exact current-state role check used before the action bonus fires. */
    public static boolean matchesCurrentTargetRole(
            SwccgGame game, String playerId,
            ObjectiveAnalyzer objectiveAnalyzer, PhysicalCard location,
            PersistentResponsePolicy.TargetRole expectedRole) {
        return targetRole(game, playerId, objectiveAnalyzer, location)
                == expectedRole;
    }

    public static boolean hasOpponentBattleParticipant(
            SwccgGame game, String playerId,
            ObjectiveAnalyzer objectiveAnalyzer, PhysicalCard location) {
        if (game == null || playerId == null
                || objectiveAnalyzer == null || location == null) {
            return false;
        }
        try {
            return objectiveAnalyzer.hasOpponentBattleParticipantAt(
                    game, playerId, location);
        } catch (Exception ignored) {
            return false;
        }
    }

    /** Exact auto-cleanup proof. Mere absence from hand is never enough. */
    public static boolean isExactCardInPlayAtPlannedTarget(
            SwccgGame game, int cardPermanentId, int cardCurrentId,
            String targetCurrentCardId, int targetPermanentId) {
        if (game == null || game.getGameState() == null
                || targetCurrentCardId == null) {
            return false;
        }
        try {
            GameState gameState = game.getGameState();
            PhysicalCard card = gameState.findCardByPermanentId(
                    cardPermanentId);
            PhysicalCard target = gameState.findCardById(
                    Integer.parseInt(targetCurrentCardId));
            if (card == null || target == null
                    || card.getCardId() != cardCurrentId
                    || target.getPermanentCardId() != targetPermanentId
                    || card.getZone() == null
                    || !card.getZone().isInPlay()) {
                return false;
            }
            PhysicalCard actualLocation = game.getModifiersQuerying()
                    .getLocationThatCardIsAt(gameState, card);
            return actualLocation != null
                    && actualLocation.getPermanentCardId()
                    == targetPermanentId;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static PersistentResponsePolicy.TargetRole targetRole(
            SwccgGame game, String playerId,
            ObjectiveAnalyzer objectiveAnalyzer, PhysicalCard location) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || location == null || game == null || playerId == null) {
            return PersistentResponsePolicy.TargetRole.NONE;
        }
        try {
            if (objectiveAnalyzer.isObjectiveHardLossDefenseLocation(
                    game, playerId, location)) {
                return PersistentResponsePolicy.TargetRole
                        .OBJECTIVE_HARD_LOSS_DEFENSE;
            }
            if (objectiveAnalyzer.isFlipped()
                    && (objectiveAnalyzer.isFlipBackProtectionLocation(
                    location, game, playerId)
                    || objectiveAnalyzer.assessPostFlipLocationRisk(
                    game, playerId, location).requiresProtection())) {
                return PersistentResponsePolicy.TargetRole
                        .POST_FLIP_PROTECTION;
            }
            if (!objectiveAnalyzer.isFlipped()
                    && objectiveAnalyzer.isFlipGateLocation(
                    game, playerId, location)
                    && objectiveAnalyzer.isMissingPreFlipRequirementAt(
                    game, playerId, location)) {
                return PersistentResponsePolicy.TargetRole
                        .ACTIVE_FLIP_GATE;
            }
            if (!objectiveAnalyzer.isFlipped()
                    && (objectiveAnalyzer.isMissingPreFlipRequirementAt(
                    game, playerId, location)
                    || objectiveAnalyzer
                    .isActiveRequiredCardControlEnablerLocation(
                            game, playerId, location)
                    || objectiveAnalyzer
                    .isMissingRequiredCardDeployEnablerAt(
                            game, playerId, location))) {
                return PersistentResponsePolicy.TargetRole
                        .MISSING_REQUIRED_LOCATION;
            }
        } catch (Exception ignored) {
        }
        return PersistentResponsePolicy.TargetRole.NONE;
    }

    private static <P> boolean completesMandatoryNeed(
            Input<P> input, PersistentResponsePolicy.TargetRole role,
            PhysicalCard location, List<PhysicalCard> plannedCards) {
        ObjectiveAnalyzer objective = input.objectiveAnalyzer();
        if (!isCritical(role) || objective == null
                || !objective.isAnalyzed() || objective.isFlipped()) {
            return false;
        }
        for (PhysicalCard card : plannedCards) {
            try {
                if (objective.wouldCompletePreFlipRequirementAt(
                        input.game(), input.playerId(), card, location)) {
                    return true;
                }
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private static boolean isCritical(
            PersistentResponsePolicy.TargetRole role) {
        return role == PersistentResponsePolicy.TargetRole
                .OBJECTIVE_HARD_LOSS_DEFENSE
                || role == PersistentResponsePolicy.TargetRole.ACTIVE_FLIP_GATE
                || role == PersistentResponsePolicy.TargetRole
                .POST_FLIP_PROTECTION
                || role == PersistentResponsePolicy.TargetRole
                .MISSING_REQUIRED_LOCATION;
    }

    private static int projectedDrain(
            SwccgGame game, String playerId, PhysicalCard location) {
        try {
            return Math.max(0, (int) Math.ceil(
                    game.getModifiersQuerying().getForceDrainAmount(
                            game.getGameState(), location, playerId)));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int projectedOpponentDrain(
            SwccgGame game, String playerId, PhysicalCard location) {
        try {
            return projectedDrain(game,
                    game.getGameState().getOpponent(playerId), location);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int currentFriendlyDrain(
            SwccgGame game, String playerId, PhysicalCard location) {
        try {
            if (!game.getModifiersQuerying().controlsLocation(
                    game.getGameState(), location, playerId)) {
                return 0;
            }
            return projectedDrain(game, playerId, location);
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static <P> List<PhysicalCard> exactCards(
            Input<P> input, List<InstructionView> instructions) {
        List<PhysicalCard> cards = new ArrayList<>();
        for (InstructionView instruction : instructions) {
            PhysicalCard card = exactCard(input, instruction);
            if (card == null) return List.of();
            cards.add(card);
        }
        return cards;
    }

    private static <P> PhysicalCard exactCard(
            Input<P> input, InstructionView instruction) {
        return cardByIdentity(input.game().getGameState(),
                input.playerId(), instruction);
    }

    private static PhysicalCard cardByIdentity(
            GameState gameState, String playerId,
            InstructionView instruction) {
        if (gameState == null || playerId == null || instruction == null
                || instruction.permanentCardId() == null
                || instruction.currentCardId() == null) {
            return null;
        }
        List<PhysicalCard> hand = gameState.getHand(playerId);
        if (hand == null) return null;
        for (PhysicalCard card : hand) {
            if (card != null && card.getPermanentCardId()
                    == instruction.permanentCardId()
                    && card.getCardId() == instruction.currentCardId()) {
                return card;
            }
        }
        return null;
    }

    private static AiBoardAnalyzer.LocationAnalysis findLocation(
            List<AiBoardAnalyzer.LocationAnalysis> locations,
            String currentCardId) {
        if (currentCardId == null) return null;
        for (AiBoardAnalyzer.LocationAnalysis location : locations) {
            if (location != null && location.location != null
                    && currentCardId.equals(String.valueOf(
                    location.location.getCardId()))) {
                return location;
            }
        }
        return null;
    }

    private static InstructionView firstInstruction(
            List<InstructionView> instructions) {
        return instructions.stream().min(
                Comparator.comparingInt(InstructionView::priority)
                        .thenComparing(instruction ->
                                instruction.permanentCardId() != null
                                        ? instruction.permanentCardId()
                                        : Integer.MAX_VALUE)
                        .thenComparing(instruction ->
                                instruction.currentCardId() != null
                                        ? instruction.currentCardId()
                                        : Integer.MAX_VALUE))
                .orElse(null);
    }

    private static PersistentResponsePolicy.DeployActionKey actionKey(
            InstructionView instruction) {
        if (instruction == null || instruction.permanentCardId() == null
                || instruction.currentCardId() == null
                || instruction.permanentCardId() <= 0
                || instruction.currentCardId() <= 0) {
            return null;
        }
        return new PersistentResponsePolicy.DeployActionKey(
                instruction.permanentCardId(),
                instruction.currentCardId());
    }

    private static List<PersistentResponsePolicy.DeployActionKey>
    responseActionKeys(List<InstructionView> instructions) {
        List<InstructionView> ordered = new ArrayList<>(instructions);
        ordered.sort(Comparator
                .comparingInt(InstructionView::priority)
                .thenComparing(instruction ->
                        instruction.permanentCardId() != null
                                ? instruction.permanentCardId()
                                : Integer.MAX_VALUE)
                .thenComparing(instruction ->
                        instruction.currentCardId() != null
                                ? instruction.currentCardId()
                                : Integer.MAX_VALUE));
        List<PersistentResponsePolicy.DeployActionKey> keys =
                new ArrayList<>();
        for (InstructionView instruction : ordered) {
            PersistentResponsePolicy.DeployActionKey key =
                    actionKey(instruction);
            if (key == null) return List.of();
            keys.add(key);
        }
        return List.copyOf(keys);
    }

    private static PersistentResponsePolicy.LocationKey locationKey(
            PhysicalCard location) {
        return new PersistentResponsePolicy.LocationKey(
                location.getPermanentCardId(), location.getTitle());
    }

    private static <P> PersistentResponsePolicy.CandidateKey candidateKey(
            String kind, int threatPermanentId, PhysicalCard target,
            PlanView<P> plan,
            PersistentResponsePolicy.DeployActionKey action,
            List<AiBoardAnalyzer.LocationAnalysis> locations) {
        String cards = plan.instructions().stream()
                .map(instruction -> canonicalInstructionKey(
                        instruction, locations))
                .sorted().collect(Collectors.joining(","));
        return new PersistentResponsePolicy.CandidateKey(
                kind + "|threat=" + threatPermanentId
                        + "|target=" + target.getPermanentCardId()
                        + "|domain=" + plan.domain()
                        + "|strategy=" + plan.strategy()
                        + "|action=" + (action != null
                        ? action.permanentCardId() + ":"
                        + action.currentCardId() : "none")
                        + "|cards=" + cards);
    }

    private static String canonicalInstructionKey(
            InstructionView instruction,
            List<AiBoardAnalyzer.LocationAnalysis> locations) {
        AiBoardAnalyzer.LocationAnalysis target = findLocation(
                locations, instruction.targetLocationCardId());
        int targetPermanentId = target != null && target.location != null
                ? target.location.getPermanentCardId() : 0;
        return instruction.permanentCardId() + ":"
                + instruction.currentCardId() + "@"
                + targetPermanentId;
    }

    private static boolean canDeployPaidDirectly(
            SwccgGame game, PhysicalCard card, PhysicalCard location) {
        try {
            return Filters.deployableToLocation(
                    card, Filters.sameCardId(location), false, 0.0f)
                    .accepts(game.getGameState(),
                            game.getModifiersQuerying(), card);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static Integer exactDeployCost(
            SwccgGame game, PhysicalCard card, PhysicalCard location) {
        try {
            float cost = game.getModifiersQuerying().getDeployCost(
                    game.getGameState(), card, card, location,
                    false, null, false, 0.0f, null, true);
            return (int) Math.ceil(Math.max(0.0f, cost));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int printedPower(PhysicalCard card) {
        try {
            SwccgCardBlueprint blueprint = card.getBlueprint();
            Float power = blueprint != null && blueprint.hasPowerAttribute()
                    ? blueprint.getPower() : null;
            return power != null ? power.intValue() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static int printedAbility(PhysicalCard card) {
        try {
            SwccgCardBlueprint blueprint = card.getBlueprint();
            Float ability = blueprint != null
                    && blueprint.hasAbilityAttribute()
                    ? blueprint.getAbility() : null;
            return ability != null ? ability.intValue() : 0;
        } catch (Exception ignored) {
            return 0;
        }
    }

    private static float exactAbility(
            SwccgGame game, PhysicalCard card,
            boolean includePermanentPilots) {
        try {
            return game.getModifiersQuerying().getAbility(
                    game.getGameState(), card, includePermanentPilots);
        } catch (Exception ignored) {
            return 0.0f;
        }
    }

    private static boolean hasPermanentPilot(
            SwccgGame game, PhysicalCard ship) {
        try {
            return game.getModifiersQuerying().hasPermanentPilot(
                    game.getGameState(), ship);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static float opponentWeaponBonus(
            SwccgGame game, String playerId, PhysicalCard location) {
        try {
            GameState gameState = game.getGameState();
            return FormationSafety.weaponBonusAt(gameState, location,
                    gameState.getOpponent(playerId));
        } catch (Exception ignored) {
            return 0.0f;
        }
    }

    private static int countArmedOpponents(
            SwccgGame game, String playerId, PhysicalCard location) {
        int armed = 0;
        try {
            GameState gameState = game.getGameState();
            String opponent = gameState.getOpponent(playerId);
            for (PhysicalCard card : gameState.getCardsAtLocation(location)) {
                if (card != null && opponent.equals(card.getOwner())
                        && card.getBlueprint() != null
                        && card.getBlueprint().getCardCategory()
                        == CardCategory.CHARACTER
                        && FormationSafety.weaponBonusOf(
                        gameState, card) > 0.0f) {
                    armed++;
                }
            }
        } catch (Exception ignored) {
        }
        return armed;
    }

    private static PersistentResponsePolicy.FormationProof emptyFormation() {
        return new PersistentResponsePolicy.FormationProof(
                0.0f, 0.0f, 0.0f, 0.0f,
                DeployTacticalPolicy.ResponseFormationRoute.NONE, false);
    }
}
