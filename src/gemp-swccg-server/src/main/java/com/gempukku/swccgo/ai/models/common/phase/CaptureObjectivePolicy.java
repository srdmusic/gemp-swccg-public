package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;

import java.util.List;
import java.util.Objects;

/**
 * Shared pure scoring for the capture-state objectives There Is Good In Him
 * and Bring Him Before Me. Adapters own all engine and card-state reads.
 */
public final class CaptureObjectivePolicy {
    private static final String PRODUCER = "CAPTURE_OBJECTIVE_POLICY";
    private static final String VIRTUAL_HUT_BLUEPRINT = "214_19";
    private static final float SETUP_TIE_BREAK = 100.0f;
    private static final float MANDATORY_SCORE = 300.0f;
    private static final float TIMING_PREFERENCE_SCORE = -300.0f;
    private static final float SAFE_CONFLICT_BONUS = 80.0f;
    private static final float BHBM_YOUR_DESTINY_BONUS = 300.0f;
    private static final float BHBM_BATTLE_WIN_BONUS = 80.0f;
    private static final float BHBM_FORCE_DRIP_URGENCY_PER_TURN =
            100.0f;
    private static final float BHBM_FORCE_DRIP_URGENCY_CAP =
            300.0f;

    public enum ObjectiveKind {
        TIGIH,
        BHBM
    }

    public enum CaptureRouteStep {
        PARENT,
        DESTINATION
    }

    public enum CriticalRole {
        CAPTURE_PIECE,
        PAYOFF_CARD
    }

    public record SetupHutFacts(
            String actionId,
            ObjectiveKind objective,
            String blueprintId,
            boolean bothHutPrintingsOffered) {
        public SetupHutFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(blueprintId, "blueprintId");
        }
    }

    public record CaptureRouteFacts(
            String actionId,
            ObjectiveKind objective,
            CaptureRouteStep step,
            boolean guaranteedImmediateCapture) {
        public CaptureRouteFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(step, "step");
        }
    }

    public record DeployCaptureFacts(
            String actionId,
            ObjectiveKind objective,
            CaptureRouteStep step,
            boolean guaranteedImmediateCapture) {
        public DeployCaptureFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(step, "step");
        }
    }

    public record EmperorDownloadFacts(
            String actionId,
            ObjectiveKind objective,
            boolean backSideUp,
            boolean affordable) {
        public EmperorDownloadFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
        }
    }

    public record PayoffFacts(
            String actionId,
            ObjectiveKind objective,
            boolean exactActionReady,
            boolean safeTimingReached,
            float guaranteedCrossoverTotal) {
        public PayoffFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
            if (!Float.isFinite(guaranteedCrossoverTotal)) {
                throw new IllegalArgumentException(
                        "guaranteedCrossoverTotal must be finite");
            }
        }
    }

    /**
     * The adapter counts only source-legal duel pieces at Dark's own Throne
     * Room: Vader, Emperor, and the current source-modified target
     * (Luke, Leia, or Kanan) when that target is non-frozen and targetable.
     * The Throne Room fact uses the source's "present at" semantics, so an
     * enclosed card aboard a vehicle or starship does not satisfy it. Safety
     * is an AI policy gate, not card law.
     * Both action source and candidate ownership are explicit. Card10_010 is
     * Light-owned even though its action relocates Dark-owned Vader, so it
     * cannot be treated as Dark objective progress.
     */
    public record BhbmForceDripUrgencyFacts(
            String actionId,
            ObjectiveKind objective,
            boolean backSideUp,
            boolean forceDripActive,
            boolean flipAgeKnown,
            int turnsObservedSinceFlip,
            boolean factsKnown,
            boolean safe,
            boolean candidateWillBePresentAtThroneRoom,
            boolean actingPlayerOwnsActionSource,
            boolean actingPlayerOwnsCandidate,
            boolean stableBackCurrentlyHeld,
            int duelTrioPieceCountBefore,
            int duelTrioPieceCountAfter,
            boolean preservesStableBackState) {
        public BhbmForceDripUrgencyFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
        }
    }

    public record ConflictBattleFacts(
            String actionId,
            ObjectiveKind objective,
            boolean iFeelTheConflictActive,
            boolean projectionAlreadySafe) {
        public ConflictBattleFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
        }
    }

    public record CaptureEnablerBattleFacts(
            String actionId,
            ObjectiveKind objective,
            boolean soleVirtualCaptureEnablerAtBattleLocation) {
        public CaptureEnablerBattleFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
        }
    }

    public record BhbmYourDestinyFacts(
            String actionId,
            boolean exactThreeForceClockReady) {
        public BhbmYourDestinyFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record BhbmBattleWinFacts(
            String actionId,
            boolean originalInsignificantRebellionActive,
            boolean projectionAlreadySafe) {
        public BhbmBattleWinFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    public record StableBackFacts(
            String actionId,
            ObjectiveKind objective,
            boolean backSideUp,
            boolean currentlyStable,
            boolean wouldBreakLastStableState) {
        public StableBackFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
        }
    }

    public record RetentionFacts(
            String actionId,
            ObjectiveKind objective,
            CriticalRole role,
            boolean protectionRequired) {
        public RetentionFacts {
            Objects.requireNonNull(actionId, "actionId");
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(role, "role");
        }
    }

    public record TransferLukeToVaderFacts(
            String actionId,
            boolean legalObjectiveTransfer,
            boolean crossoverModifierPressureKnown,
            float crossoverModifierPressure) {
        public TransferLukeToVaderFacts {
            Objects.requireNonNull(actionId, "actionId");
            if (!Float.isFinite(crossoverModifierPressure)) {
                throw new IllegalArgumentException(
                        "crossoverModifierPressure must be finite");
            }
        }
    }

    public record BhbmOpponentTargetDownloadFacts(
            String actionId,
            boolean legalObjectiveDownload) {
        public BhbmOpponentTargetDownloadFacts {
            Objects.requireNonNull(actionId, "actionId");
        }
    }

    private CaptureObjectivePolicy() {
    }

    public static PolicyResult scoreSetupHut(SetupHutFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.objective() != ObjectiveKind.TIGIH
                || !facts.bothHutPrintingsOffered()
                || !VIRTUAL_HUT_BLUEPRINT.equals(facts.blueprintId())) {
            return none();
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of("SETUP.TIGIH.PREFER_VIRTUAL_HUT"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.ORDERING,
                SETUP_TIE_BREAK,
                "TIGIH SETUP: prefer virtual Chief Chirpa's Hut"
                        + " because it carries the free capture route"));
    }

    public static PolicyResult scoreCaptureRoute(
            CaptureRouteFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.guaranteedImmediateCapture()) {
            return none();
        }

        String ruleId = facts.step() == CaptureRouteStep.PARENT
                ? "MOVE.OBJECTIVE.CAPTURE_ROUTE_PARENT"
                : "MOVE.OBJECTIVE.CAPTURE_ROUTE_DESTINATION";
        String step = facts.step() == CaptureRouteStep.PARENT
                ? "parent action" : "destination";
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(ruleId),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                MANDATORY_SCORE,
                facts.objective() + " CAPTURE ROUTE: " + step
                        + " guarantees the exact-site capture"));
    }

    public static PolicyResult scoreDeployCaptureRoute(
            DeployCaptureFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.guaranteedImmediateCapture()) {
            return none();
        }

        boolean parent =
                facts.step() == CaptureRouteStep.PARENT;
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(parent
                        ? "DEPLOY.OBJECTIVE.CAPTURE_ROUTE_PARENT"
                        : "DEPLOY.OBJECTIVE.CAPTURE_ROUTE_DESTINATION"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                MANDATORY_SCORE,
                facts.objective() + " CAPTURE DEPLOY: "
                        + (parent ? "parent action" : "destination")
                        + " guarantees the exact-site capture"));
    }

    public static PolicyResult scoreEmperorDownload(
            EmperorDownloadFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.objective() != ObjectiveKind.BHBM) {
            return none();
        }
        if (!facts.affordable()) {
            return one(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(
                        "PULL.OBJECTIVE.BHBM.EMPEROR_RESERVE"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    -300.0f,
                    "BHBM EMPEROR RESERVE: prefer to leave Force"
                        + " for the next exact capture move"));
        }
        if (!facts.backSideUp()) {
            return one(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(
                        "PULL.OBJECTIVE.BHBM.EMPEROR_SETUP"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    MANDATORY_SCORE,
                    "BHBM SETUP: deploy Emperor from Reserve Deck"
                        + " for the later Throne Room duel"));
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "PULL.OBJECTIVE.BHBM.EMPEROR"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                MANDATORY_SCORE,
                "BHBM PAYOFF: deploy Emperor from Reserve Deck"
                        + " for the source-defined Throne Room duel"));
    }

    public static PolicyResult scorePayoff(PayoffFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactActionReady()) {
            return none();
        }

        if (facts.objective() == ObjectiveKind.TIGIH
                && !facts.safeTimingReached()
                && !(facts.guaranteedCrossoverTotal() > 14.0f)) {
            return one(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(
                            "OBJECTIVE.TIGIH.CROSSOVER_TIMING_DEFER"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    TIMING_PREFERENCE_SCORE,
                    "TIGIH CROSSOVER: prefer to wait for remaining safe"
                            + " I Feel The Conflict buildup"));
        }

        String ruleId = facts.objective() == ObjectiveKind.TIGIH
                ? "OBJECTIVE.TIGIH.CROSSOVER_ATTEMPT"
                : "OBJECTIVE.BHBM.DUEL_ATTEMPT";
        String reason = facts.objective() == ObjectiveKind.TIGIH
                ? "TIGIH CROSSOVER: attempt now because timing is safe"
                    + " or the guaranteed total already exceeds 14"
                : "BHBM PAYOFF: initiate the legal Vader duel";
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(ruleId),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                MANDATORY_SCORE,
                reason));
    }

    public static PolicyResult scoreBhbmForceDripUrgency(
            BhbmForceDripUrgencyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.objective() != ObjectiveKind.BHBM
                || !facts.backSideUp()
                || !facts.forceDripActive()
                || !facts.flipAgeKnown()
                || facts.turnsObservedSinceFlip() <= 0
                || !facts.factsKnown()
                || !facts.safe()
                || !facts.candidateWillBePresentAtThroneRoom()
                || !facts.actingPlayerOwnsActionSource()
                || !facts.actingPlayerOwnsCandidate()
                || !facts.stableBackCurrentlyHeld()
                || !facts.preservesStableBackState()
                || facts.duelTrioPieceCountBefore() < 0
                || facts.duelTrioPieceCountAfter() != 3
                || facts.duelTrioPieceCountAfter()
                    <= facts.duelTrioPieceCountBefore()) {
            return none();
        }

        float urgency = Math.min(
                BHBM_FORCE_DRIP_URGENCY_CAP,
                BHBM_FORCE_DRIP_URGENCY_PER_TURN
                    * (float) facts.turnsObservedSinceFlip());
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "OBJECTIVE.BHBM.FORCE_DRIP_TRIO_URGENCY"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                urgency,
                "BHBM BACK: safe action completes the source-legal"
                        + " duel trio at Throne Room after "
                        + facts.turnsObservedSinceFlip()
                        + " observed own-turn transitions while the source"
                        + " charges 1 Force at each own end turn"));
    }

    public static PolicyResult scoreConflictBattle(
            ConflictBattleFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.objective() != ObjectiveKind.TIGIH
                || !facts.iFeelTheConflictActive()
                || !facts.projectionAlreadySafe()) {
            return none();
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "BATTLE.OBJECTIVE.TIGIH.CONFLICT_BUILDUP"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                SAFE_CONFLICT_BONUS,
                "TIGIH I FEEL THE CONFLICT: projected-safe battle"
                        + " can add a stack and 3 to crossover total"));
    }

    public static PolicyResult scoreBhbmYourDestiny(
            BhbmYourDestinyFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.exactThreeForceClockReady()) {
            return none();
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "OBJECTIVE.BHBM.YOUR_DESTINY_BATTLEGROUND"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                BHBM_YOUR_DESTINY_BONUS,
                "BHBM YOUR DESTINY: put Vader at a battleground site"
                    + " to arm the source's start-turn 3-Force loss"));
    }

    public static PolicyResult scoreBhbmBattleWin(
            BhbmBattleWinFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.originalInsignificantRebellionActive()
                || !facts.projectionAlreadySafe()) {
            return none();
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "BATTLE.OBJECTIVE.BHBM.INSIGNIFICANT_REBELLION"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                BHBM_BATTLE_WIN_BONUS,
                "BHBM INSIGNIFICANT REBELLION: projected-safe battle"
                    + " can cost 1 Force and add a +3 crossover stack"));
    }

    public static PolicyResult holdSoleVirtualCaptureEnablerBattle(
            CaptureEnablerBattleFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (facts.objective() != ObjectiveKind.TIGIH
                || !facts.soleVirtualCaptureEnablerAtBattleLocation()) {
            return none();
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "BATTLE.OBJECTIVE.TIGIH.VIRTUAL_HUT_ENABLER_HOLD"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f,
                "TIGIH VIRTUAL HUT: prefer not to battle away the sole"
                        + " Imperial enabling Luke's free capture move"));
    }

    public static PolicyResult scoreStableBackHold(
            StableBackFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.backSideUp()
                || !facts.currentlyStable()
                || !facts.wouldBreakLastStableState()) {
            return none();
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "OBJECTIVE.CAPTURE_STATE.STABLE_BACK_HOLD"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f,
                facts.objective() + " HOLD: prefer to preserve the last captive"
                        + " or present-with-Vader state"));
    }

    public static PolicyResult scoreTransferLukeToVader(
            TransferLukeToVaderFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.legalObjectiveTransfer()
                || !facts.crossoverModifierPressureKnown()) {
            return none();
        }
        if (facts.crossoverModifierPressure() >= 8.0f) {
            return one(PolicyOperation.add(
                    facts.actionId(),
                    TraceRuleId.of(
                        "OBJECTIVE.TIGIH.OPPONENT_TRANSFER_HOLD"),
                    TraceDomainId.OBJECTIVE_INTENT,
                    TraceOutputKind.BANDED,
                    -300.0f,
                    "TIGIH BACK: keep Luke with the non-Vader escort"
                        + " because modifier pressure 8 or more plus"
                        + " maximum destiny 7 can exceed 14"));
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "OBJECTIVE.TIGIH.OPPONENT_TRANSFER_BLEED_STOP"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                250.0f,
                "TIGIH BACK: transfer captive Luke to Vader"
                        + " to stop the end-turn 2-Force loss while"
                        + " crossover pressure remains below lethal"));
    }

    public static PolicyResult scoreBhbmOpponentTargetDownload(
            BhbmOpponentTargetDownloadFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.legalObjectiveDownload()) {
            return none();
        }
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                    "OBJECTIVE.BHBM.OPPONENT_TARGET_DOWNLOAD"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                250.0f,
                "BHBM OPPONENT TEXT: use the exact objective"
                    + " download to put its current Luke, Leia, or Kanan"
                    + " target into play"));
    }

    public static PolicyResult scoreCriticalRetention(
            RetentionFacts facts) {
        Objects.requireNonNull(facts, "facts");
        if (!facts.protectionRequired()) {
            return none();
        }
        String role = facts.role() == CriticalRole.CAPTURE_PIECE
                ? "capture piece" : "payoff card";
        return one(PolicyOperation.add(
                facts.actionId(),
                TraceRuleId.of(
                        "FORCE_LOSS.OBJECTIVE.CAPTURE_CRITICAL"),
                TraceDomainId.OBJECTIVE_INTENT,
                TraceOutputKind.BANDED,
                -300.0f,
                facts.objective() + " CRITICAL: prefer to preserve the " + role));
    }

    private static PolicyResult one(PolicyOperation operation) {
        return new PolicyResult(PRODUCER, List.of(operation));
    }

    private static PolicyResult none() {
        return new PolicyResult(PRODUCER, List.of());
    }
}
