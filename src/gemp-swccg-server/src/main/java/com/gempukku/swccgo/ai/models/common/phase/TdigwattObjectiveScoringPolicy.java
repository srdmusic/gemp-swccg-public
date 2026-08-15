package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.common.DestinyType;

import java.util.List;
import java.util.Objects;

/**
 * Pure score translation for the source-backed TDIGWATT facts and policy.
 *
 * <p>Every veto requires exact physical-source provenance. Unknown adapters,
 * incomplete Reserve scans, and title-like near matches remain neutral.</p>
 */
public final class TdigwattObjectiveScoringPolicy {
    public static final String PRODUCER_ID =
            "TDIGWATT_OBJECTIVE_SCORING_POLICY";

    public static final float PULL_PARENT_BONUS = 300.0f;
    public static final float PULL_CHILD_BONUS = 300.0f;
    public static final float DEPLOY_ADVANCE_BONUS = 300.0f;
    public static final float DEPLOY_COMPLETE_BONUS = 300.0f;
    public static final float DEPLOY_STABLE_BACK_BONUS = 300.0f;
    public static final float ENGINE_DEPLOY_BONUS = 300.0f;
    /**
     * Exact safe objective movement uses the bounded +300 objective preference.
     * It cannot defeat a categorical hard veto.
     */
    public static final float LANDO_PARENT_BONUS = 300.0f;
    public static final float LANDO_DESTINATION_BONUS = 300.0f;
    public static final float BATTLE_DESTINY_POINT_BONUS = 40.0f;
    public static final float LANDO_DESTINY_ADJUSTMENT_BONUS = 20.0f;
    public static final float MAX_BATTLE_PAYOFF_BONUS = 200.0f;

    public enum Outcome {
        NEUTRAL,
        PULL_PARENT_READY,
        PULL_PARENT_EXHAUSTED,
        PULL_CHILD_READY,
        PULL_CHILD_WRONG_PRINT,
        DEPLOY_ADVANCE_FRONT,
        DEPLOY_COMPLETE_FRONT,
        DEPLOY_PROTECT_STABLE_BACK,
        ENGINE_DARK_DEAL,
        ENGINE_CLOUD_CITY_OCCUPATION,
        ENGINE_DARK_DEAL_CANCELS,
        ENGINE_CLOUD_CITY_OCCUPATION_CANCELS,
        LANDO_PARENT_READY,
        LANDO_DESTINATION_READY,
        CONTROL_LANDO_FORCE_RESERVED,
        BATTLE_PAYOFF,
        LANDO_DESTINY_PARENT,
        LANDO_DESTINY_DIRECTION,
        FORCE_LOSS_RETAIN,
        FORFEIT_RETAIN
    }

    public enum RetentionRoute {
        FORCE_LOSS,
        FORFEIT
    }

    public record Evaluation(PolicyResult result, Outcome outcome) {
        public Evaluation {
            Objects.requireNonNull(result, "result");
            Objects.requireNonNull(outcome, "outcome");
        }
    }

    /**
     * Parent action facts. Exhaustion is actionable only after an exact,
     * complete enumeration of the source-defined Reserve candidates.
     */
    public record PullParentFacts(
            String actionId,
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            int actionSourcePhysicalCardId,
            boolean exactObjectivePullAction,
            boolean reserveEnumerationComplete,
            List<TdigwattObjectiveFacts.PullFacts> reserveCandidates) {
        public PullParentFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(
                    reserveCandidates, "reserveCandidates");
            reserveCandidates = List.copyOf(reserveCandidates);
        }
    }

    /**
     * Child facts keep adapter classification provenance separate from the
     * typed target. A guessed title classification must remain neutral.
     */
    public record PullChildFacts(
            String actionId,
            TdigwattObjectiveFacts.PullFacts candidate,
            boolean exactObjectivePullChildDecision,
            boolean sourceFilterClassificationExact) {
        public PullChildFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(candidate, "candidate");
        }
    }

    /**
     * The engine has already proved the card's own deploy requirements. This
     * policy therefore needs exact objective provenance and target identity,
     * but deliberately invents no shared Bespin occupation prerequisite.
     */
    public record EngineDeployFacts(
            String actionId,
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            int objectiveSourcePhysicalCardId,
            TdigwattObjectiveFacts.PullTarget target,
            boolean exactEngineOfferedDeploy,
            boolean targetClassificationExact,
            boolean sourcePersistenceExact,
            boolean sourceEffectPersists) {
        public EngineDeployFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(target, "target");
        }
    }

    public record LandoActionFacts(
            String actionId,
            TdigwattObjectiveFacts.LandoMoveFacts move,
            boolean exactObjectiveMoveAction,
            int liveForce) {
        public LandoActionFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(move, "move");
        }
    }

    public record LandoDestinationFacts(
            LandoActionFacts parent,
            boolean exactKnownDestination) {
        public LandoDestinationFacts {
            Objects.requireNonNull(parent, "parent");
        }
    }

    public record ControlSpendFacts(
            String actionId,
            TdigwattObjectiveFacts.LandoMoveFacts reservedMove,
            boolean exactUnrelatedOptionalSpend,
            boolean landoSourceAction,
            Integer exactSpendCost,
            int liveForce) {
        public ControlSpendFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(reservedMove, "reservedMove");
        }
    }

    public record BattleScoringFacts(
            String actionId,
            TdigwattObjectiveFacts.BattleFacts battle,
            boolean exactObjectiveBattle,
            boolean baseBattleSafe,
            boolean unsafeBattleVeto) {
        public BattleScoringFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(battle, "battle");
        }
    }

    public enum DestinyAdjustmentChoice {
        PARENT,
        ADD_ONE,
        SUBTRACT_ONE
    }

    public record DestinyAdjustmentScoringFacts(
            String actionId,
            TdigwattObjectiveFacts.DestinyAdjustmentFacts adjustment,
            boolean exactObjectiveDecision,
            DestinyAdjustmentChoice choice) {
        public DestinyAdjustmentScoringFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(adjustment, "adjustment");
            Objects.requireNonNull(choice, "choice");
        }
    }

    private TdigwattObjectiveScoringPolicy() {
    }

    public static Evaluation scorePullParent(PullParentFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactObjectivePullAction()
                || facts.actionSourcePhysicalCardId()
                    != facts.objective().physicalCardId()) {
            return neutral();
        }

        boolean legalTargetExists = facts.reserveCandidates().stream()
                .filter(candidate -> facts.objective()
                        .isSamePhysicalObjective(
                                candidate.objective()))
                .filter(candidate ->
                        candidate.actionSourcePhysicalCardId()
                            == facts.actionSourcePhysicalCardId())
                .anyMatch(TdigwattObjectivePolicy::sourceLegalPull);
        if (legalTargetExists) {
            return add(
                    facts.actionId(),
                    facts.objective(),
                    "PULL.PARENT.READY",
                    TraceDomainId.PULL_SEARCH,
                    TraceOutputKind.ORDERING,
                    PULL_PARENT_BONUS,
                    "Exact " + sourceClass(facts.objective())
                            + " objective upload has a source-legal Reserve target",
                    Outcome.PULL_PARENT_READY);
        }

        if (!facts.reserveEnumerationComplete()) {
            return neutral();
        }
        return hardVeto(
                facts.actionId(),
                facts.objective(),
                "PULL.PARENT.EXHAUSTED",
                TraceDomainId.PULL_SEARCH,
                "Exact " + sourceClass(facts.objective())
                        + " objective upload has no source-legal Reserve target",
                Outcome.PULL_PARENT_EXHAUSTED);
    }

    public static Evaluation scorePullChild(PullChildFacts facts) {
        Objects.requireNonNull(facts, "facts");
        TdigwattObjectiveFacts.PullFacts candidate =
                facts.candidate();
        boolean exactPhysicalSource =
                candidate.actionSourcePhysicalCardId()
                    == candidate.objective().physicalCardId();
        if (!facts.exactObjectivePullChildDecision()
                || !facts.sourceFilterClassificationExact()
                || !exactPhysicalSource
                || !candidate.candidateInReserve()) {
            return neutral();
        }
        if (candidate.objective().printing()
                    == TdigwattObjectiveFacts.Printing.CLASSIC
                && candidate.objective().backSideUp()) {
            return neutral();
        }

        if (TdigwattObjectivePolicy.sourceLegalPull(candidate)) {
            return add(
                    facts.actionId(),
                    candidate.objective(),
                    "PULL.CHILD.LEGAL",
                    TraceDomainId.DECK_PLAYBOOK,
                    TraceOutputKind.BANDED,
                    PULL_CHILD_BONUS,
                    "Select the exact source-legal "
                            + sourceClass(candidate.objective())
                            + " objective upload target",
                    Outcome.PULL_CHILD_READY);
        }

        return hardVeto(
                facts.actionId(),
                candidate.objective(),
                "PULL.CHILD.PRINT_ISOLATION",
                TraceDomainId.DECK_PLAYBOOK,
                "Reject a target that belongs to the other TDIGWATT printing",
                Outcome.PULL_CHILD_WRONG_PRINT);
    }

    public static Evaluation scoreDeploy(
            String actionId,
            TdigwattObjectiveFacts.ClassicState before,
            TdigwattObjectiveFacts.ClassicState after,
            boolean exactProjection) {
        Objects.requireNonNull(actionId, "actionId");
        if (!exactProjection) {
            return neutral();
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        return scoreDeployPriority(
                actionId,
                before.objective(),
                TdigwattObjectivePolicy.deployPriority(
                        before, after));
    }

    public static Evaluation scoreDeploy(
            String actionId,
            TdigwattObjectiveFacts.VirtualState before,
            TdigwattObjectiveFacts.VirtualState after,
            boolean exactProjection) {
        Objects.requireNonNull(actionId, "actionId");
        if (!exactProjection) {
            return neutral();
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        return scoreDeployPriority(
                actionId,
                before.objective(),
                TdigwattObjectivePolicy.deployPriority(
                        before, after));
    }

    public static Evaluation scoreEngineDeploy(
            EngineDeployFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactEngineOfferedDeploy()
                || !facts.targetClassificationExact()
                || facts.objectiveSourcePhysicalCardId()
                    != facts.objective().physicalCardId()) {
            return neutral();
        }

        if (facts.target()
                == TdigwattObjectiveFacts.PullTarget.DARK_DEAL) {
            if (!facts.sourcePersistenceExact()) {
                return neutral();
            }
            if (!facts.sourceEffectPersists()) {
                return hardVeto(
                        facts.actionId(),
                        facts.objective(),
                        "DEPLOY.ENGINE.DARK_DEAL.CANCELS",
                        TraceDomainId.DEPLOY_SITING,
                        "Reject the exact Dark Deal deploy because its source cancellation condition is already met",
                        Outcome.ENGINE_DARK_DEAL_CANCELS);
            }
            if (facts.objective().printing()
                    != TdigwattObjectiveFacts.Printing.CLASSIC) {
                return neutral();
            }
            return add(
                    facts.actionId(),
                    facts.objective(),
                    "DEPLOY.ENGINE.DARK_DEAL",
                    TraceDomainId.DEPLOY_SITING,
                    TraceOutputKind.BANDED,
                    ENGINE_DEPLOY_BONUS,
                    "Prioritize the exact engine-offered Dark Deal deploy",
                    Outcome.ENGINE_DARK_DEAL);
        }
        if (facts.target()
                    == TdigwattObjectiveFacts.PullTarget
                        .CLOUD_CITY_OCCUPATION
                && facts.objective().printing()
                    == TdigwattObjectiveFacts.Printing.CLASSIC) {
            if (!facts.sourcePersistenceExact()) {
                return neutral();
            }
            if (!facts.sourceEffectPersists()) {
                return hardVeto(
                        facts.actionId(),
                        facts.objective(),
                        "DEPLOY.ENGINE.CLOUD_CITY_OCCUPATION.CANCELS",
                        TraceDomainId.DEPLOY_SITING,
                        "Reject the exact Cloud City Occupation deploy because opponent controls Bespin",
                        Outcome
                            .ENGINE_CLOUD_CITY_OCCUPATION_CANCELS);
            }
            return add(
                    facts.actionId(),
                    facts.objective(),
                    "DEPLOY.ENGINE.CLOUD_CITY_OCCUPATION",
                    TraceDomainId.DEPLOY_SITING,
                    TraceOutputKind.BANDED,
                    ENGINE_DEPLOY_BONUS,
                    "Prioritize the exact engine-offered classic Cloud City Occupation deploy",
                    Outcome.ENGINE_CLOUD_CITY_OCCUPATION);
        }
        return neutral();
    }

    public static Evaluation scoreVirtualLandoParent(
            LandoActionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!exactSafeUsefulVirtualLandoRoute(facts)) {
            return neutral();
        }
        return add(
                facts.actionId(),
                facts.move().objective(),
                "LANDO.PARENT",
                TraceDomainId.MOVE,
                TraceOutputKind.ORDERING,
                LANDO_PARENT_BONUS,
                "Use the exact source-granted virtual Lando move for a safe objective route",
                Outcome.LANDO_PARENT_READY);
    }

    public static Evaluation scoreVirtualLandoDestination(
            LandoDestinationFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactKnownDestination()
                || !exactSafeUsefulVirtualLandoRoute(
                        facts.parent())) {
            return neutral();
        }
        return add(
                facts.parent().actionId(),
                facts.parent().move().objective(),
                "LANDO.DESTINATION",
                TraceDomainId.MOVE,
                TraceOutputKind.BANDED,
                LANDO_DESTINATION_BONUS,
                "Choose the exact known safe destination that advances or protects the virtual objective",
                Outcome.LANDO_DESTINATION_READY);
    }

    public static Evaluation preserveVirtualLandoControlForce(
            ControlSpendFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactUnrelatedOptionalSpend()
                || facts.landoSourceAction()
                || facts.exactSpendCost() == null
                || facts.exactSpendCost() < 0
                || facts.liveForce() < 0
                || !exactSafeUsefulVirtualLandoRoute(
                        facts.reservedMove())) {
            return neutral();
        }

        int reserve = TdigwattObjectivePolicy
                .virtualLandoMoveForceReserve(
                        facts.reservedMove());
        if (reserve <= 0
                || facts.liveForce() < reserve
                || facts.liveForce() - facts.exactSpendCost()
                    >= reserve) {
            return neutral();
        }
        return add(
                facts.actionId(),
                facts.reservedMove().objective(),
                "CONTROL.LANDO_FORCE_RESERVE",
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f,
                "Preserve the exact positive Force reserve for the source-granted virtual Lando move",
                Outcome.CONTROL_LANDO_FORCE_RESERVED);
    }

    public static Evaluation scoreBackSideBattle(
            BattleScoringFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactObjectiveBattle()
                || !facts.baseBattleSafe()
                || facts.unsafeBattleVeto()) {
            return neutral();
        }

        TdigwattObjectivePolicy.BattlePayoff payoff =
                TdigwattObjectivePolicy.battlePayoff(
                        facts.battle());
        if (!payoff.eligible()) {
            return neutral();
        }
        float bonus = Math.min(
                MAX_BATTLE_PAYOFF_BONUS,
                payoff.totalBattleDestinyBonus()
                        * BATTLE_DESTINY_POINT_BONUS
                    + payoff.landoDestinyAdjustments()
                        * LANDO_DESTINY_ADJUSTMENT_BONUS);
        return add(
                facts.actionId(),
                facts.battle().objective(),
                "BACK.BATTLE_PAYOFF",
                TraceDomainId.BATTLE_INITIATION,
                TraceOutputKind.BANDED,
                bonus,
                "Safe battle activates the exact back-side destiny payoff",
                Outcome.BATTLE_PAYOFF);
    }

    public static Evaluation scoreVirtualLandoDestinyAdjustment(
            DestinyAdjustmentScoringFacts facts) {
        Objects.requireNonNull(facts, "facts");
        TdigwattObjectiveFacts.DestinyAdjustmentFacts
                adjustment = facts.adjustment();
        if (!facts.exactObjectiveDecision()
                || adjustment.actionSourcePhysicalCardId()
                    != adjustment.objective()
                        .physicalCardId()
                || !adjustment.battle()
                    .yourLandoInBattle()
                || adjustment.destinyType()
                    != DestinyType.BATTLE_DESTINY) {
            return neutral();
        }
        if (facts.choice()
                == DestinyAdjustmentChoice.PARENT) {
            return add(
                    facts.actionId(),
                    adjustment.objective(),
                    "BACK.LANDO_DESTINY.PARENT",
                    TraceDomainId.BATTLE_WEAPONS,
                    TraceOutputKind.ORDERING,
                    LANDO_DESTINY_ADJUSTMENT_BONUS,
                    "Use the exact virtual back-side Lando destiny adjustment",
                    Outcome.LANDO_DESTINY_PARENT);
        }

        boolean addOwnDraw =
                adjustment.drawOwner()
                    == TdigwattObjectiveFacts
                        .DestinyDrawOwner.YOURS
                && facts.choice()
                    == DestinyAdjustmentChoice.ADD_ONE;
        boolean subtractOpponentDraw =
                adjustment.drawOwner()
                    == TdigwattObjectiveFacts
                        .DestinyDrawOwner.OPPONENTS
                && facts.choice()
                    == DestinyAdjustmentChoice.SUBTRACT_ONE;
        if (addOwnDraw || subtractOpponentDraw) {
            return add(
                    facts.actionId(),
                    adjustment.objective(),
                    "BACK.LANDO_DESTINY.DIRECTION",
                    TraceDomainId.BATTLE_WEAPONS,
                    TraceOutputKind.ORDERING,
                    LANDO_DESTINY_ADJUSTMENT_BONUS,
                    addOwnDraw
                        ? "Add 1 to your exact live destiny draw"
                        : "Subtract 1 from the opponent's exact live destiny draw",
                    Outcome.LANDO_DESTINY_DIRECTION);
        }
        return hardVeto(
                facts.actionId(),
                adjustment.objective(),
                "BACK.LANDO_DESTINY.WRONG_DIRECTION",
                TraceDomainId.BATTLE_WEAPONS,
                "Reject the exact Lando destiny adjustment in the harmful direction",
                Outcome.LANDO_DESTINY_DIRECTION);
    }

    public static Evaluation scoreForceLoss(
            String actionId,
            TdigwattObjectiveFacts.ClassicState before,
            TdigwattObjectiveFacts.ClassicState after,
            boolean exactCandidateProjection,
            boolean hasUnprotectedLegalAlternative) {
        Objects.requireNonNull(actionId, "actionId");
        if (!exactCandidateProjection) {
            return neutral();
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        TdigwattObjectivePolicy.RetentionMargin margin =
                TdigwattObjectivePolicy.retentionMargins(
                        before, after, before).forceLoss();
        return scoreRetention(
                actionId, before.objective(), RetentionRoute.FORCE_LOSS,
                margin, hasUnprotectedLegalAlternative);
    }

    public static Evaluation scoreForceLoss(
            String actionId,
            TdigwattObjectiveFacts.VirtualState before,
            TdigwattObjectiveFacts.VirtualState after,
            boolean exactCandidateProjection,
            boolean hasUnprotectedLegalAlternative) {
        Objects.requireNonNull(actionId, "actionId");
        if (!exactCandidateProjection) {
            return neutral();
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        TdigwattObjectivePolicy.RetentionMargin margin =
                TdigwattObjectivePolicy.retentionMargins(
                        before, after, before).forceLoss();
        return scoreRetention(
                actionId, before.objective(), RetentionRoute.FORCE_LOSS,
                margin, hasUnprotectedLegalAlternative);
    }

    public static Evaluation scoreForfeit(
            String actionId,
            TdigwattObjectiveFacts.ClassicState before,
            TdigwattObjectiveFacts.ClassicState after,
            boolean exactCandidateProjection,
            boolean hasUnprotectedLegalAlternative) {
        Objects.requireNonNull(actionId, "actionId");
        if (!exactCandidateProjection) {
            return neutral();
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        TdigwattObjectivePolicy.RetentionMargin margin =
                TdigwattObjectivePolicy.retentionMargins(
                        before, before, after).forfeit();
        return scoreRetention(
                actionId, before.objective(), RetentionRoute.FORFEIT,
                margin, hasUnprotectedLegalAlternative);
    }

    public static Evaluation scoreForfeit(
            String actionId,
            TdigwattObjectiveFacts.VirtualState before,
            TdigwattObjectiveFacts.VirtualState after,
            boolean exactCandidateProjection,
            boolean hasUnprotectedLegalAlternative) {
        Objects.requireNonNull(actionId, "actionId");
        if (!exactCandidateProjection) {
            return neutral();
        }
        Objects.requireNonNull(before, "before");
        Objects.requireNonNull(after, "after");
        TdigwattObjectivePolicy.RetentionMargin margin =
                TdigwattObjectivePolicy.retentionMargins(
                        before, before, after).forfeit();
        return scoreRetention(
                actionId, before.objective(), RetentionRoute.FORFEIT,
                margin, hasUnprotectedLegalAlternative);
    }

    private static Evaluation scoreDeployPriority(
            String actionId,
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            TdigwattObjectivePolicy.DeployPriority priority) {
        return switch (priority) {
            case NONE -> neutral();
            case ADVANCE_FRONT -> add(
                    actionId, objective, "DEPLOY.ADVANCE_FRONT",
                    TraceDomainId.DEPLOY_SITING,
                    TraceOutputKind.BANDED,
                    DEPLOY_ADVANCE_BONUS,
                    "Advance one exact source-defined front-side requirement",
                    Outcome.DEPLOY_ADVANCE_FRONT);
            case COMPLETE_FRONT -> add(
                    actionId, objective, "DEPLOY.COMPLETE_FRONT",
                    TraceDomainId.DEPLOY_SITING,
                    TraceOutputKind.BANDED,
                    DEPLOY_COMPLETE_BONUS,
                    "Complete the exact source-defined front-side flip law",
                    Outcome.DEPLOY_COMPLETE_FRONT);
            case PROTECT_STABLE_BACK -> add(
                    actionId, objective,
                    "DEPLOY.PROTECT_STABLE_BACK",
                    TraceDomainId.DEPLOY_SITING,
                    TraceOutputKind.BANDED,
                    DEPLOY_STABLE_BACK_BONUS,
                    "Increase the exact virtual back-side control cushion",
                    Outcome.DEPLOY_PROTECT_STABLE_BACK);
        };
    }

    private static boolean exactSafeUsefulVirtualLandoRoute(
            LandoActionFacts facts) {
        TdigwattObjectiveFacts.LandoMoveFacts move = facts.move();
        int policyReserve =
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(move);
        return facts.exactObjectiveMoveAction()
                && exactSafeUsefulVirtualLandoRoute(move)
                && policyReserve == move.requiredForceCost()
                && facts.liveForce() >= 0
                && facts.liveForce() >= policyReserve;
    }

    private static boolean exactSafeUsefulVirtualLandoRoute(
            TdigwattObjectiveFacts.LandoMoveFacts move) {
        int policyReserve =
                TdigwattObjectivePolicy
                        .virtualLandoMoveForceReserve(move);
        return move.objective().printing()
                    == TdigwattObjectiveFacts.Printing.VIRTUAL
                && move.actionSourcePhysicalCardId()
                    == move.objective().physicalCardId()
                && move.sourceActionAvailable()
                && move.exactRouteKnown()
                && move.legalDestinationExists()
                && move.advancesOrProtectsObjective()
                && move.formationSafe()
                && policyReserve == move.requiredForceCost();
    }

    private static Evaluation scoreRetention(
            String actionId,
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            RetentionRoute route,
            TdigwattObjectivePolicy.RetentionMargin margin,
            boolean hasUnprotectedLegalAlternative) {
        int positiveExactMargin =
                margin.frontProgressLost()
                    + margin.stableBackCushionLost();
        if (positiveExactMargin <= 0
                || !hasUnprotectedLegalAlternative) {
            return neutral();
        }

        String suffix = route == RetentionRoute.FORCE_LOSS
                ? "FORCE_LOSS.RETAIN" : "FORFEIT.RETAIN";
        Outcome outcome = route == RetentionRoute.FORCE_LOSS
                ? Outcome.FORCE_LOSS_RETAIN
                : Outcome.FORFEIT_RETAIN;
        return add(
                actionId,
                objective,
                suffix,
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f,
                "Prefer to preserve positive exact objective margin "
                        + positiveExactMargin
                        + " while an unprotected legal loss exists",
                outcome);
    }

    private static Evaluation add(
            String actionId,
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            String suffix,
            TraceDomainId domain,
            TraceOutputKind outputKind,
            float delta,
            String reason,
            Outcome outcome) {
        TraceDomainId scoreDomain = switch (outcome) {
            case BATTLE_PAYOFF,
                 LANDO_DESTINY_PARENT,
                 LANDO_DESTINY_DIRECTION -> domain;
            default -> TraceDomainId.OBJECTIVE_INTENT;
        };
        PolicyOperation operation = PolicyOperation.add(
                actionId,
                ruleId(objective, suffix),
                scoreDomain,
                outputKind,
                delta,
                reason);
        return new Evaluation(
                new PolicyResult(PRODUCER_ID, List.of(operation)),
                outcome);
    }

    private static Evaluation hardVeto(
            String actionId,
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            String suffix,
            TraceDomainId domain,
            String reason,
            Outcome outcome) {
        PolicyOperation operation = PolicyOperation.hardVeto(
                actionId,
                ruleId(objective, suffix),
                domain,
                TraceOutputKind.VETO,
                reason);
        return new Evaluation(
                new PolicyResult(PRODUCER_ID, List.of(operation)),
                outcome);
    }

    private static Evaluation neutral() {
        return new Evaluation(
                new PolicyResult(PRODUCER_ID, List.of()),
                Outcome.NEUTRAL);
    }

    private static TraceRuleId ruleId(
            TdigwattObjectiveFacts.ObjectiveIdentity objective,
            String suffix) {
        return TraceRuleId.of(
                "TDIGWATT." + objective.frontBlueprintId()
                        + "." + suffix);
    }

    private static String sourceClass(
            TdigwattObjectiveFacts.ObjectiveIdentity objective) {
        return objective.printing()
                == TdigwattObjectiveFacts.Printing.CLASSIC
                ? "Card109_012" : "Card226_012";
    }
}
