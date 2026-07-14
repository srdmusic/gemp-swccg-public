package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.strategy.ForceObligationVector;
import com.gempukku.swccgo.common.DeployDestinationRef;
import com.gempukku.swccgo.common.DeployPhysicalCardRef;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Zone;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable accepted-response lifecycle for one exact physical DEPLOY attempt. */
public record DeployTransaction(
        Key key,
        Stage stage,
        DeployPhysicalCardRef sourceCard,
        Zone sourceZone,
        int parentActionOrdinal,
        String parentActionWireId,
        List<DeployDestinationRef> orderedDestinations,
        DeployDestinationRef selectedDestination,
        List<DeployPhysicalCardRef> orderedBuddyCandidates,
        DeployPhysicalCardRef selectedBuddy,
        DeployFormationAssessment formation,
        ForceObligationVector forceObligations,
        Float deployCost,
        List<Stage> history,
        List<String> acceptedWires,
        String terminalReason) {

    public enum Stage {
        SNAPSHOT,
        PARENT_PENDING,
        CHILD_PENDING,
        COMMITTED,
        COMPLETED
    }

    public record Key(
            long gameEpoch,
            int gameIdentity,
            int turn,
            Phase phase,
            String playerId,
            String attemptId,
            int diagnosticParentDecisionId) {
        public Key {
            if (gameEpoch <= 0 || turn < 0
                    || diagnosticParentDecisionId < 0) {
                throw new IllegalArgumentException("invalid DEPLOY transaction key numbers");
            }
            if (playerId == null || playerId.isBlank()
                    || attemptId == null || attemptId.isBlank()) {
                throw new IllegalArgumentException("DEPLOY transaction key strings must be nonblank");
            }
            Objects.requireNonNull(phase, "phase");
        }
    }

    public DeployTransaction {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(sourceCard, "sourceCard");
        Objects.requireNonNull(sourceZone, "sourceZone");
        if (parentActionOrdinal < 0) {
            throw new IllegalArgumentException("parentActionOrdinal must be >= 0");
        }
        if (parentActionWireId == null || parentActionWireId.isBlank()) {
            throw new IllegalArgumentException("parentActionWireId must be nonblank");
        }
        orderedDestinations = List.copyOf(orderedDestinations);
        orderedBuddyCandidates = List.copyOf(orderedBuddyCandidates);
        if (selectedDestination != null && !orderedDestinations.isEmpty()
                && !orderedDestinations.contains(selectedDestination)) {
            throw new IllegalArgumentException(
                    "selected destination was not an offered candidate");
        }
        if (selectedBuddy != null && !orderedBuddyCandidates.isEmpty()
                && !orderedBuddyCandidates.contains(selectedBuddy)) {
            throw new IllegalArgumentException("selected buddy was not an offered candidate");
        }
        Objects.requireNonNull(formation, "formation");
        Objects.requireNonNull(forceObligations, "forceObligations");
        history = List.copyOf(history);
        acceptedWires = List.copyOf(acceptedWires);
        if (history.isEmpty() || history.get(history.size() - 1) != stage) {
            throw new IllegalArgumentException("transaction history must end at current stage");
        }
        if (terminalReason != null && terminalReason.isBlank()) {
            throw new IllegalArgumentException("terminalReason must be null or nonblank");
        }
    }

    public static DeployTransaction snapshot(Key key,
                                             DeployPhysicalCardRef sourceCard,
                                             Zone sourceZone,
                                             int parentActionOrdinal,
                                             String parentActionWireId,
                                             List<DeployDestinationRef> orderedDestinations,
                                             List<DeployPhysicalCardRef> orderedBuddyCandidates,
                                             DeployFormationAssessment formation,
                                             ForceObligationVector forceObligations,
                                             Float deployCost) {
        return new DeployTransaction(key, Stage.SNAPSHOT, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, orderedDestinations, null,
                orderedBuddyCandidates, null,
                formation, forceObligations, deployCost, List.of(Stage.SNAPSHOT),
                List.of(), null);
    }

    public DeployTransaction parentAccepted(String wire) {
        return transition(Stage.PARENT_PENDING, singleKnownDestination(), wire);
    }

    public DeployTransaction destinationAccepted(DeployDestinationRef destination,
                                                  String wire) {
        Objects.requireNonNull(destination, "destination");
        if (stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING) {
            throw new IllegalStateException(
                    "destination acceptance requires an accepted deploy cursor");
        }
        if (!orderedDestinations.isEmpty() && !orderedDestinations.contains(destination)) {
            throw new IllegalArgumentException("accepted destination was not in the ordered legal set");
        }
        if (selectedDestination != null && !selectedDestination.equals(destination)) {
            throw new IllegalArgumentException("accepted destination drifted from the selected destination");
        }
        if (stage == Stage.PARENT_PENDING) {
            return transition(Stage.CHILD_PENDING, destination, wire);
        }
        return sameStageUpdate(destination, selectedBuddy, wire);
    }

    public DeployTransaction buddyAccepted(DeployPhysicalCardRef buddy, String wire) {
        Objects.requireNonNull(buddy, "buddy");
        if (stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING) {
            throw new IllegalStateException(
                    "buddy acceptance requires an accepted deploy cursor");
        }
        if (!orderedBuddyCandidates.contains(buddy)) {
            throw new IllegalArgumentException("accepted buddy was not an offered candidate");
        }
        if (selectedBuddy != null && !selectedBuddy.equals(buddy)) {
            throw new IllegalArgumentException("accepted buddy drifted from the selected buddy");
        }
        if (stage == Stage.PARENT_PENDING) {
            return transition(Stage.CHILD_PENDING, selectedDestination, buddy, wire);
        }
        return sameStageUpdate(selectedDestination, buddy, wire);
    }

    /** Carries an engine-forced destination exposed by a later child choice. */
    public DeployTransaction destinationObserved(List<DeployDestinationRef> destinations,
                                                 DeployDestinationRef destination) {
        if (stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING) {
            throw new IllegalStateException(
                    "destination observation requires an accepted deploy cursor");
        }
        List<DeployDestinationRef> exact = List.copyOf(destinations);
        if (destination == null || !exact.contains(destination)) {
            throw new IllegalArgumentException(
                    "observed destination must be an exact candidate");
        }
        if (!orderedDestinations.isEmpty() && !orderedDestinations.equals(exact)) {
            throw new IllegalArgumentException("observed destination candidates drifted");
        }
        if (selectedDestination != null && !selectedDestination.equals(destination)) {
            throw new IllegalArgumentException("observed destination drifted from the selected destination");
        }
        return new DeployTransaction(key, stage, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, exact, destination,
                orderedBuddyCandidates, selectedBuddy, formation, forceObligations,
                deployCost, history, acceptedWires, null);
    }

    /** Carries a forced buddy selection exposed by a later engine child. */
    public DeployTransaction buddyObserved(List<DeployPhysicalCardRef> candidates,
                                            DeployPhysicalCardRef buddy) {
        if (stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING) {
            throw new IllegalStateException("buddy observation requires an accepted deploy cursor");
        }
        List<DeployPhysicalCardRef> exact = List.copyOf(candidates);
        if (buddy == null || !exact.contains(buddy)) {
            throw new IllegalArgumentException("observed buddy must be an exact candidate");
        }
        if (!orderedBuddyCandidates.isEmpty() && !orderedBuddyCandidates.equals(exact)) {
            throw new IllegalArgumentException("observed buddy candidates drifted");
        }
        if (selectedBuddy != null && !selectedBuddy.equals(buddy)) {
            throw new IllegalArgumentException("observed buddy drifted from the selected buddy");
        }
        return new DeployTransaction(key, stage, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, orderedDestinations,
                selectedDestination, exact, buddy, formation, forceObligations,
                deployCost, history, acceptedWires, null);
    }

    /** Records the engine's exact child candidate order without recomputing formation. */
    public DeployTransaction destinationsObserved(List<DeployDestinationRef> destinations) {
        if ((stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING)
                || selectedDestination != null) {
            throw new IllegalStateException(
                    "destination candidates require an unresolved accepted deploy cursor");
        }
        List<DeployDestinationRef> exact = List.copyOf(destinations);
        if (!orderedDestinations.isEmpty() && !orderedDestinations.equals(exact)) {
            throw new IllegalArgumentException("observed destination candidates drifted");
        }
        return new DeployTransaction(key, stage, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, exact, null,
                orderedBuddyCandidates, selectedBuddy,
                formation, forceObligations, deployCost, history,
                acceptedWires, null);
    }

    public DeployTransaction childChoiceAccepted(String wire) {
        if (stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING) {
            throw new IllegalStateException("child choice requires an accepted parent or destination");
        }
        return appendAcceptedWire(wire);
    }

    /** Binds a deferred PULL formation only after the first DEPLOY child is accepted. */
    public DeployTransaction formationAccepted(DeployFormationAssessment accepted) {
        Objects.requireNonNull(accepted, "accepted");
        if (stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING) {
            throw new IllegalStateException(
                    "formation acceptance requires an accepted deploy cursor");
        }
        if (!sourceCard.equals(accepted.sourceCard())) {
            throw new IllegalArgumentException(
                    "accepted formation source drifted from the deploy cursor");
        }
        if (!orderedDestinations.isEmpty()
                && !orderedDestinations.equals(accepted.orderedDestinations())) {
            throw new IllegalArgumentException(
                    "accepted formation destinations drifted from the deploy cursor");
        }
        if (formation.equals(accepted)) {
            return this;
        }
        if (formation.verdict() != DeployFormationAssessment.Verdict.UNKNOWN) {
            throw new IllegalStateException(
                    "an established formation assessment cannot be replaced");
        }
        List<DeployDestinationRef> exactDestinations = orderedDestinations.isEmpty()
                ? accepted.orderedDestinations() : orderedDestinations;
        return new DeployTransaction(key, stage, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, exactDestinations,
                selectedDestination, orderedBuddyCandidates, selectedBuddy,
                accepted, forceObligations, deployCost, history,
                acceptedWires, null);
    }

    public DeployTransaction committed() {
        if (stage == Stage.COMMITTED) {
            return this;
        }
        if (stage != Stage.PARENT_PENDING && stage != Stage.CHILD_PENDING) {
            throw new IllegalStateException("commit requires an accepted parent or child");
        }
        return transition(Stage.COMMITTED, selectedDestination, null);
    }

    public DeployTransaction completed() {
        DeployTransaction committed = stage == Stage.COMMITTED ? this : committed();
        return committed.transition(Stage.COMPLETED, committed.selectedDestination, null);
    }

    public DeployTransaction terminated(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("termination reason must be nonblank");
        }
        return new DeployTransaction(key, stage, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, orderedDestinations,
                selectedDestination, orderedBuddyCandidates, selectedBuddy,
                formation, forceObligations, deployCost,
                history, acceptedWires, reason);
    }

    private DeployTransaction transition(Stage next,
                                         DeployDestinationRef destination,
                                         String acceptedWire) {
        return transition(next, destination, selectedBuddy, acceptedWire);
    }

    private DeployTransaction transition(Stage next,
                                         DeployDestinationRef destination,
                                         DeployPhysicalCardRef buddy,
                                         String acceptedWire) {
        boolean legal = (stage == Stage.SNAPSHOT && next == Stage.PARENT_PENDING)
                || (stage == Stage.PARENT_PENDING
                        && (next == Stage.CHILD_PENDING || next == Stage.COMMITTED))
                || (stage == Stage.CHILD_PENDING && next == Stage.COMMITTED)
                || (stage == Stage.COMMITTED && next == Stage.COMPLETED);
        if (!legal) {
            throw new IllegalStateException("illegal DEPLOY lifecycle transition "
                    + stage + " -> " + next);
        }
        ArrayList<Stage> nextHistory = new ArrayList<>(history);
        nextHistory.add(next);
        ArrayList<String> nextAcceptedWires = new ArrayList<>(acceptedWires);
        if (acceptedWire != null) {
            if (acceptedWire.isBlank() && next != Stage.PARENT_PENDING) {
                throw new IllegalArgumentException("accepted child wire must be nonblank");
            }
            nextAcceptedWires.add(acceptedWire);
        }
        return new DeployTransaction(key, next, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, orderedDestinations,
                destination, orderedBuddyCandidates, buddy,
                formation, forceObligations, deployCost,
                nextHistory, nextAcceptedWires, null);
    }

    private DeployTransaction appendAcceptedWire(String wire) {
        if (wire == null || wire.isBlank()) {
            throw new IllegalArgumentException("accepted child wire must be nonblank");
        }
        ArrayList<String> nextAcceptedWires = new ArrayList<>(acceptedWires);
        nextAcceptedWires.add(wire);
        return new DeployTransaction(key, stage, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, orderedDestinations,
                selectedDestination, orderedBuddyCandidates, selectedBuddy,
                formation, forceObligations, deployCost,
                history, nextAcceptedWires, null);
    }

    private DeployTransaction sameStageUpdate(DeployDestinationRef destination,
                                              DeployPhysicalCardRef buddy,
                                              String acceptedWire) {
        if (acceptedWire == null || acceptedWire.isBlank()) {
            throw new IllegalArgumentException("accepted child wire must be nonblank");
        }
        ArrayList<String> nextAcceptedWires = new ArrayList<>(acceptedWires);
        nextAcceptedWires.add(acceptedWire);
        return new DeployTransaction(key, stage, sourceCard, sourceZone,
                parentActionOrdinal, parentActionWireId, orderedDestinations,
                destination, orderedBuddyCandidates, buddy,
                formation, forceObligations, deployCost,
                history, nextAcceptedWires, null);
    }

    private DeployDestinationRef singleKnownDestination() {
        return orderedDestinations.size() == 1 ? orderedDestinations.get(0) : null;
    }
}
