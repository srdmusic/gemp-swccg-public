package com.gempukku.swccgo.ai.models.common.phase;

import com.gempukku.swccgo.ai.models.common.policy.PolicyOperation;
import com.gempukku.swccgo.ai.models.common.policy.PolicyResult;
import com.gempukku.swccgo.ai.models.common.trace.TraceDomainId;
import com.gempukku.swccgo.ai.models.common.trace.TraceOutputKind;
import com.gempukku.swccgo.ai.models.common.trace.TraceRuleId;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.state.ForceDrainState;
import com.gempukku.swccgo.game.state.GameState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Shared typed memory and target policy for persistent public drain pressure.
 * Legal candidates and formation proof are supplied by existing planner and
 * tactical-policy owners.
 */
public final class PersistentResponsePolicy {
    private static final Logger LOG =
            LogManager.getLogger(PersistentResponsePolicy.class);
    public static final String PERSISTENT_RULE_ID =
            "deploy-persistent-response-selected";
    public static final String CRITICAL_RULE_ID =
            "deploy-objective-critical-eviction-selected";
    public static final int PERSISTENT_RESPONSE_BONUS = 300;
    public static final int CRITICAL_OCCUPATION_BONUS = 250;
    public static final String RESPONSE_BUCKET_LABEL =
            "PERSISTENT_RESPONSE";

    public enum CandidateKind {
        RESPONSE_TARGET,
        EXISTING_PLAN
    }

    public enum TargetRole {
        OBJECTIVE_HARD_LOSS_DEFENSE,
        ACTIVE_FLIP_GATE,
        POST_FLIP_PROTECTION,
        MISSING_REQUIRED_LOCATION,
        PERSISTENT_DAMAGE,
        NONE
    }

    public enum Mode {
        CONTEST,
        REINFORCE,
        SPY,
        MASS_TOWARD,
        RACE
    }

    private PersistentResponsePolicy() {
    }

    /**
     * Per-game opponent-drain ledger. A finished ForceDrainState remains
     * readable after GameState drops its pointer, so only final forcePaid is
     * recorded. Null ends one event but never closes the opponent turn.
     */
    public static final class State {
        private final Map<Integer, LocationLedger> locations =
                new LinkedHashMap<>();
        private ForceDrainState activeDrain;
        private int activeTurn;
        private LocationKey activeLocation;
        private GameState observedGameState;
        private int lastOpponentLatest = -1;
        private int completedOpponentTurn;
        private long revision;

        public void observe(GameState gameState, String botPlayerId) {
            Objects.requireNonNull(gameState, "gameState");
            botPlayerId = requireNonBlank(botPlayerId, "botPlayerId");
            String opponentId = requireNonBlank(
                    gameState.getOpponent(botPlayerId), "opponentId");
            int opponentLatest =
                    gameState.getPlayersLatestTurnNumber(opponentId);
            int completedThrough = Math.max(0, opponentLatest
                    - (opponentId.equals(gameState.getCurrentPlayerId())
                    ? 1 : 0));
            if (observedGameState != null
                    && (observedGameState != gameState
                    || opponentLatest < lastOpponentLatest
                    || completedThrough < completedOpponentTurn)) {
                resetForDiscontinuity();
            }
            observedGameState = gameState;
            lastOpponentLatest = opponentLatest;

            ForceDrainState current = gameState.getForceDrainState();
            if (current != activeDrain) {
                completeActive();
                if (current != null
                        && opponentId.equals(current.getPlayerId())
                        && opponentLatest > completedThrough
                        && !begin(current, opponentLatest)) {
                    failClosed(gameState, opponentLatest,
                            completedThrough,
                            "opponent drain had no stable location");
                    return;
                }
            } else if (current != null
                    && !activeMatches(current, opponentLatest)) {
                failClosed(gameState, opponentLatest, completedThrough,
                        "one ForceDrainState changed turn or location");
                return;
            }

            if (activeDrain != null && activeTurn <= completedThrough) {
                failClosed(gameState, opponentLatest, completedThrough,
                        "opponent drain remained active after its turn");
                return;
            }
            closeCompletedThrough(completedThrough);
        }

        public void reset() {
            locations.clear();
            clearActive();
            observedGameState = null;
            lastOpponentLatest = -1;
            completedOpponentTurn = 0;
            revision++;
        }

        public Snapshot snapshot() {
            Map<Integer, DrainHistory> histories = new LinkedHashMap<>();
            for (Map.Entry<Integer, LocationLedger> entry
                    : locations.entrySet()) {
                histories.put(entry.getKey(), entry.getValue().snapshot(
                        completedOpponentTurn));
            }
            return new Snapshot(revision, completedOpponentTurn, histories);
        }

        private boolean begin(ForceDrainState drain, int opponentTurn) {
            if (opponentTurn <= 0 || drain.getLocation() == null) {
                return false;
            }
            PhysicalCard location = drain.getLocation();
            activeDrain = drain;
            activeTurn = opponentTurn;
            activeLocation = new LocationKey(location.getPermanentCardId(),
                    location.getTitle());
            return true;
        }

        private boolean activeMatches(ForceDrainState drain,
                                      int opponentTurn) {
            PhysicalCard location = drain.getLocation();
            return location != null && activeLocation != null
                    && activeTurn == opponentTurn
                    && activeLocation.permanentCardId()
                    == location.getPermanentCardId();
        }

        private void completeActive() {
            if (activeDrain == null) {
                return;
            }
            int paid = activeDrain.getForcePaid();
            requireNonnegative(paid, "forcePaid");
            if (paid > 0) {
                locations.computeIfAbsent(
                        activeLocation.permanentCardId(),
                        ignored -> new LocationLedger(activeLocation))
                        .record(activeTurn, paid, activeLocation.title());
            }
            clearActive();
        }

        private void closeCompletedThrough(int opponentTurn) {
            requireNonnegative(opponentTurn, "completedOpponentTurn");
            if (opponentTurn <= completedOpponentTurn) {
                return;
            }
            completedOpponentTurn = opponentTurn;
            revision++;
        }

        private void resetForDiscontinuity() {
            locations.clear();
            clearActive();
            completedOpponentTurn = 0;
            lastOpponentLatest = -1;
            revision++;
        }

        private void failClosed(GameState gameState, int opponentLatest,
                                int completedThrough, String reason) {
            LOG.warn("Persistent response ledger reset fail-closed: {}",
                    reason);
            resetForDiscontinuity();
            observedGameState = gameState;
            lastOpponentLatest = opponentLatest;
            closeCompletedThrough(completedThrough);
        }

        private void clearActive() {
            activeDrain = null;
            activeTurn = 0;
            activeLocation = null;
        }
    }

    private static final class LocationLedger {
        private final int permanentCardId;
        private String title;
        private final Map<Integer, Integer> paidByTurn = new TreeMap<>();

        private LocationLedger(LocationKey location) {
            permanentCardId = location.permanentCardId();
            title = location.title();
        }

        private void record(int turn, int paid, String currentTitle) {
            title = currentTitle;
            paidByTurn.merge(turn, paid, Integer::sum);
        }

        private DrainHistory snapshot(int completedTurn) {
            int latestPaid = paidByTurn.getOrDefault(completedTurn, 0);
            int previousPaid = completedTurn > 0
                    ? paidByTurn.getOrDefault(completedTurn - 1, 0) : 0;
            int consecutiveTurns = latestPaid <= 0 ? 0
                    : previousPaid > 0 ? 2 : 1;
            int projection = consecutiveTurns == 2
                    ? previousPaid + latestPaid
                    : consecutiveTurns == 1 ? latestPaid * 2 : 0;
            return new DrainHistory(permanentCardId, title, completedTurn,
                    consecutiveTurns, latestPaid, projection);
        }
    }

    public record LocationKey(int permanentCardId, String title) {
        public LocationKey {
            requirePositive(permanentCardId, "permanentCardId");
            title = requireNonBlank(title, "title");
        }
    }

    public record CandidateKey(String value)
            implements Comparable<CandidateKey> {
        public CandidateKey {
            value = requireNonBlank(value, "candidateKey");
        }

        @Override
        public int compareTo(CandidateKey other) {
            return value.compareTo(other.value);
        }
    }

    public record DeployActionKey(int permanentCardId, int currentCardId) {
        public DeployActionKey {
            requirePositive(permanentCardId, "permanentCardId");
            requirePositive(currentCardId, "currentCardId");
        }
    }

    public record DrainHistory(int locationPermanentId,
                               String locationTitle,
                               int latestCompletedOpponentTurn,
                               int consecutiveOpponentTurns,
                               int latestDamage,
                               int projectedTwoTurnDamage) {
        public DrainHistory {
            requirePositive(locationPermanentId, "locationPermanentId");
            locationTitle = requireNonBlank(locationTitle, "locationTitle");
            requireNonnegative(latestCompletedOpponentTurn,
                    "latestCompletedOpponentTurn");
            requireNonnegative(consecutiveOpponentTurns,
                    "consecutiveOpponentTurns");
            requireNonnegative(latestDamage, "latestDamage");
            requireNonnegative(projectedTwoTurnDamage,
                    "projectedTwoTurnDamage");
        }

        public boolean repeatedThreat() {
            return consecutiveOpponentTurns >= 2
                    && projectedTwoTurnDamage > 0;
        }
    }

    public record Snapshot(long revision,
                           int completedOpponentTurn,
                           Map<Integer, DrainHistory> histories) {
        public Snapshot {
            if (revision < 0) {
                throw new IllegalArgumentException(
                        "revision must be nonnegative");
            }
            requireNonnegative(completedOpponentTurn,
                    "completedOpponentTurn");
            histories = Map.copyOf(Objects.requireNonNull(
                    histories, "histories"));
        }

        public static Snapshot empty() {
            return new Snapshot(0, 0, Map.of());
        }

        public Optional<DrainHistory> historyAt(int locationPermanentId) {
            return Optional.ofNullable(histories.get(locationPermanentId));
        }

        public Optional<DrainHistory> repeatedThreatAt(
                int locationPermanentId) {
            return historyAt(locationPermanentId)
                    .filter(DrainHistory::repeatedThreat);
        }

    }

    /** Minimum immutable Draw-phase proof attached to a selected obligation. */
    public record ResponseBankDetails(
            int selectionTurn,
            long threatRevision,
            int wholeResponseForceCost,
            DeployTacticalPolicy.ResponseFormationRoute route,
            String planDomain) {
        public ResponseBankDetails {
            if (threatRevision < 0) {
                throw new IllegalArgumentException(
                        "threatRevision must be nonnegative");
            }
            requirePositive(selectionTurn, "selectionTurn");
            requirePositive(wholeResponseForceCost,
                    "wholeResponseForceCost");
            Objects.requireNonNull(route, "route");
            planDomain = requireNonBlank(planDomain, "planDomain");
        }
    }

    public record ExecutionProof(boolean legal,
                                 boolean available,
                                 boolean affordable,
                                 boolean timely,
                                 int exactTotalCost,
                                 int exactInstructionCount) {
        public ExecutionProof {
            requireNonnegative(exactTotalCost, "exactTotalCost");
            requireNonnegative(exactInstructionCount,
                    "exactInstructionCount");
        }

        public boolean executable() {
            return legal && available && affordable && timely;
        }
    }

    public record FormationProof(float existingFriendlyPower,
                                 float existingFriendlyAbility,
                                 float plannedWavePower,
                                 float plannedWaveAbility,
                                 DeployTacticalPolicy.ResponseFormationRoute route,
                                 boolean alreadyWinning) {
        public FormationProof {
            requireNonnegative(existingFriendlyPower,
                    "existingFriendlyPower");
            requireNonnegative(existingFriendlyAbility,
                    "existingFriendlyAbility");
            requireNonnegative(plannedWavePower, "plannedWavePower");
            requireNonnegative(plannedWaveAbility, "plannedWaveAbility");
            Objects.requireNonNull(route, "route");
        }

        public float projectedFriendlyPower() {
            return existingFriendlyPower + plannedWavePower;
        }

        public float projectedFriendlyAbility() {
            return existingFriendlyAbility + plannedWaveAbility;
        }

        public boolean responseViable() {
            return !alreadyWinning && switch (route) {
                case V170_SPY, V171_WAVE, V172_SOLO,
                        V296_SPACE_CONTACT,
                        EXISTING_FORMATION_REINFORCEMENT -> true;
                case EXISTING_LEGAL_ALTERNATIVE, NOT_APPLICABLE, NONE -> false;
            };
        }
    }

    public record CandidateFacts(CandidateKey candidateKey,
                                 CandidateKind kind,
                                 List<DeployActionKey> responseActions,
                                 LocationKey threatLocation,
                                 LocationKey responseTargetLocation,
                                 Mode mode,
                                 TargetRole role,
                                 int consecutiveOpponentTurns,
                                 int projectedAvoidedDamage,
                                 int strategicIncome,
                                 int currentFriendlyDrainIncome,
                                 int raceValue,
                                 boolean opponentPresent,
                                 boolean fundedMandatoryObjective,
                                 ExecutionProof execution,
                                 FormationProof formation) {
        public CandidateFacts {
            Objects.requireNonNull(candidateKey, "candidateKey");
            Objects.requireNonNull(kind, "kind");
            responseActions = List.copyOf(Objects.requireNonNull(
                    responseActions, "responseActions"));
            if (kind == CandidateKind.RESPONSE_TARGET
                    && responseActions.isEmpty()) {
                throw new IllegalArgumentException(
                        "responseActions are required for a response target");
            }
            if (responseActions.stream().distinct().count()
                    != responseActions.size()) {
                throw new IllegalArgumentException(
                        "responseActions must be unique");
            }
            Objects.requireNonNull(threatLocation, "threatLocation");
            Objects.requireNonNull(responseTargetLocation,
                    "responseTargetLocation");
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(execution, "execution");
            Objects.requireNonNull(formation, "formation");
            requireNonnegative(consecutiveOpponentTurns,
                    "consecutiveOpponentTurns");
            requireNonnegative(projectedAvoidedDamage,
                    "projectedAvoidedDamage");
            requireNonnegative(strategicIncome, "strategicIncome");
            requireNonnegative(currentFriendlyDrainIncome,
                    "currentFriendlyDrainIncome");
            requireNonnegative(raceValue, "raceValue");
        }

        public DeployActionKey responseAction() {
            return responseActions.isEmpty() ? null : responseActions.get(0);
        }
    }

    public record Obligation(CandidateKey candidateKey,
                             CandidateKind kind,
                             List<DeployActionKey> responseActions,
                             LocationKey threatLocation,
                             LocationKey responseTargetLocation,
                             TargetRole role,
                             Mode mode,
                             int persistentBonus,
                             int criticalBonus,
                             String reasonCode,
                             ResponseBankDetails responseBank) {
        public Obligation(CandidateKey candidateKey,
                          CandidateKind kind,
                          List<DeployActionKey> responseActions,
                          LocationKey threatLocation,
                          LocationKey responseTargetLocation,
                          TargetRole role,
                          Mode mode,
                          int persistentBonus,
                          int criticalBonus,
                          String reasonCode) {
            this(candidateKey, kind, responseActions, threatLocation,
                    responseTargetLocation, role, mode, persistentBonus,
                    criticalBonus, reasonCode, null);
        }

        public Obligation {
            Objects.requireNonNull(candidateKey, "candidateKey");
            Objects.requireNonNull(kind, "kind");
            responseActions = List.copyOf(Objects.requireNonNull(
                    responseActions, "responseActions"));
            if (kind == CandidateKind.RESPONSE_TARGET
                    && responseActions.isEmpty()) {
                throw new IllegalArgumentException(
                        "responseActions are required for a response target");
            }
            if (responseActions.stream().distinct().count()
                    != responseActions.size()) {
                throw new IllegalArgumentException(
                        "responseActions must be unique");
            }
            Objects.requireNonNull(threatLocation, "threatLocation");
            Objects.requireNonNull(responseTargetLocation,
                    "responseTargetLocation");
            Objects.requireNonNull(role, "role");
            Objects.requireNonNull(mode, "mode");
            requireNonnegative(persistentBonus, "persistentBonus");
            requireNonnegative(criticalBonus, "criticalBonus");
            reasonCode = requireNonBlank(reasonCode, "reasonCode");
        }

        public int totalNewBonus() {
            return persistentBonus + criticalBonus;
        }

        public DeployActionKey responseAction() {
            return responseActions.isEmpty() ? null : responseActions.get(0);
        }

        public Obligation withRemainingResponseActions(
                List<DeployActionKey> remaining) {
            return new Obligation(candidateKey, kind, remaining,
                    threatLocation, responseTargetLocation, role, mode,
                    persistentBonus, criticalBonus, reasonCode, null);
        }

        public Obligation withResponseBank(ResponseBankDetails details) {
            return new Obligation(candidateKey, kind, responseActions,
                    threatLocation, responseTargetLocation, role, mode,
                    persistentBonus, criticalBonus, reasonCode, details);
        }
    }

    /** One exact offered outer Deploy action for the next proved wave member. */
    public record OfferedOuterAction(String actionId,
                                     DeployActionKey deployAction,
                                     boolean directDeploy,
                                     boolean selectable,
                                     boolean legal,
                                     boolean affordable,
                                     boolean timely,
                                     boolean hardVetoed,
                                     boolean deferred) {
        public OfferedOuterAction {
            actionId = requireNonBlank(actionId, "actionId");
            Objects.requireNonNull(deployAction, "deployAction");
        }

        public boolean admissible() {
            return directDeploy && selectable && legal && affordable
                    && timely && !hardVetoed && !deferred;
        }
    }

    /** Updated DPS hierarchy after adding the one exact response action. */
    public record ResponseBucket(List<Set<String>> buckets,
                                 List<String> labels,
                                 String actionId) {
        public ResponseBucket {
            buckets = List.copyOf(Objects.requireNonNull(
                    buckets, "buckets"));
            labels = List.copyOf(Objects.requireNonNull(labels, "labels"));
            actionId = requireNonBlank(actionId, "actionId");
            if (buckets.size() != labels.size()) {
                throw new IllegalArgumentException(
                        "bucket labels must align with buckets");
            }
        }
    }

    /**
     * Prepends a one-action response bucket without changing the existing DPS
     * hierarchy. A vetoed, deferred, missing, or ambiguous mapping falls
     * through untouched, so this seam can never force Pass.
     */
    public static Optional<ResponseBucket> prependResponseBucket(
            Obligation obligation,
            List<OfferedOuterAction> offeredActions,
            List<Set<String>> existingBuckets,
            List<String> existingLabels) {
        if (obligation == null
                || obligation.kind() != CandidateKind.RESPONSE_TARGET
                || obligation.mode() == Mode.SPY
                || obligation.totalNewBonus() <= 0
                || obligation.responseAction() == null
                || existingBuckets == null
                || existingBuckets.isEmpty()) {
            return Optional.empty();
        }
        Objects.requireNonNull(offeredActions, "offeredActions");
        DeployActionKey next = obligation.responseAction();
        List<OfferedOuterAction> matches = offeredActions.stream()
                .filter(offered -> offered.deployAction().equals(next))
                .toList();
        if (matches.size() != 1 || !matches.get(0).admissible()) {
            return Optional.empty();
        }

        OfferedOuterAction selected = matches.get(0);
        List<Set<String>> buckets = new ArrayList<>(
                existingBuckets.size() + 1);
        List<String> labels = new ArrayList<>(
                existingBuckets.size() + 1);
        buckets.add(Set.of(selected.actionId()));
        labels.add(RESPONSE_BUCKET_LABEL);
        for (int index = 0; index < existingBuckets.size(); index++) {
            Set<String> bucket = existingBuckets.get(index);
            if (bucket == null) {
                return Optional.empty();
            }
            buckets.add(Set.copyOf(bucket));
            String label = existingLabels != null
                    && index < existingLabels.size()
                    && existingLabels.get(index) != null
                    && !existingLabels.get(index).isBlank()
                    ? existingLabels.get(index) : "step#" + index;
            labels.add(label);
        }
        return Optional.of(new ResponseBucket(
                buckets, labels, selected.actionId()));
    }

    /**
     * Selects one already-built executable plan. No internal rank value leaves
     * this method. Equal avoided damage preserves the executable alternative.
     */
    public static Optional<Obligation> select(List<CandidateFacts> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        List<CandidateFacts> eligible = new ArrayList<>();
        for (CandidateFacts candidate : candidates) {
            Objects.requireNonNull(candidate, "candidate");
            if (isEligible(candidate)) {
                eligible.add(candidate);
            }
        }

        List<CandidateFacts> mandatory = eligible.stream()
                .filter(candidate -> candidate.fundedMandatoryObjective()
                        && isTerminalHardLoss(candidate.role()))
                .toList();
        if (!mandatory.isEmpty()) {
            return Optional.of(toObligation(bestOverall(mandatory),
                    "objective-hard-loss-defense"));
        }

        Map<Integer, List<CandidateFacts>> responsesByThreat =
                new LinkedHashMap<>();
        for (CandidateFacts candidate : eligible) {
            if (candidate.kind() == CandidateKind.RESPONSE_TARGET) {
                responsesByThreat.computeIfAbsent(
                        candidate.threatLocation().permanentCardId(),
                        ignored -> new ArrayList<>()).add(candidate);
            }
        }
        if (responsesByThreat.isEmpty()) {
            return Optional.empty();
        }

        List<CandidateFacts> threatWinners = new ArrayList<>();
        for (Map.Entry<Integer, List<CandidateFacts>> entry
                : responsesByThreat.entrySet()) {
            CandidateFacts response = bestResponse(entry.getValue());
            CandidateFacts alternative = bestAlternative(eligible,
                    entry.getKey());
            if (alternative != null
                    && response.projectedAvoidedDamage()
                    <= alternativeValue(alternative)) {
                threatWinners.add(alternative);
            } else {
                threatWinners.add(response);
            }
        }

        CandidateFacts selected = bestOverall(threatWinners);
        String reason = selected.kind() == CandidateKind.RESPONSE_TARGET
                ? "selected-executable-response"
                : "executable-alternative-preserved";
        return Optional.of(toObligation(selected, reason));
    }

    /** Adds only the two grounded Batch 1 response bands. */
    public static PolicyResult scoreSelectedResponse(
            String actionId, Obligation obligation,
            boolean existingV166ContestScore,
            boolean opponentStillPresent,
            boolean persistentFactStillCurrent,
            boolean criticalRoleStillCurrent) {
        Objects.requireNonNull(actionId, "actionId");
        Objects.requireNonNull(obligation, "obligation");
        List<PolicyOperation> operations = new ArrayList<>(2);
        String target = "target="
                + obligation.responseTargetLocation().title()
                + "#"
                + obligation.responseTargetLocation().permanentCardId();
        if (obligation.kind() == CandidateKind.RESPONSE_TARGET
                && obligation.mode() != Mode.SPY
                && opponentStillPresent) {
            if (obligation.persistentBonus() > 0
                    && persistentFactStillCurrent
                    && !existingV166ContestScore) {
                operations.add(add(actionId, PERSISTENT_RULE_ID,
                        obligation.persistentBonus(),
                        "Selected executable response to a two-turn drain lane; "
                                + target));
            }
            if (obligation.criticalBonus() > 0
                    && criticalRoleStillCurrent) {
                operations.add(addObjective(actionId, CRITICAL_RULE_ID,
                        obligation.criticalBonus(),
                        "Selected executable response clears a typed objective-critical location; "
                                + target));
            }
        }
        return new PolicyResult("DEPLOY_PERSISTENT_RESPONSE_POLICY",
                operations);
    }

    public static boolean matchesSelectedResponseAction(
            Obligation obligation, int permanentCardId, int currentCardId,
            int targetLocationPermanentId) {
        Objects.requireNonNull(obligation, "obligation");
        return obligation.kind() == CandidateKind.RESPONSE_TARGET
                && obligation.responseActions().stream().anyMatch(
                    selected -> selected.permanentCardId()
                            == permanentCardId
                        && selected.currentCardId() == currentCardId)
                && obligation.responseTargetLocation().permanentCardId()
                == targetLocationPermanentId;
    }

    /**
     * Destination adapter boundary. Only the selected physical instruction at
     * its exact offered target row may receive the Batch 1 contribution. The
     * bands are per proved wave member: each remaining exact member may emit
     * its own tags on its own destination decision.
     */
    public static PolicyResult scoreSelectedResponseAction(
            String actionId, Obligation obligation,
            int permanentCardId, int currentCardId,
            int targetLocationPermanentId,
            boolean exactPlannedInstruction,
            boolean exactPlannedTargetRow,
            boolean selectable,
            boolean existingV166ContestScore,
            boolean opponentStillPresent,
            boolean persistentFactStillCurrent,
            boolean criticalRoleStillCurrent) {
        if (obligation == null || !exactPlannedInstruction
                || !exactPlannedTargetRow || !selectable
                || !matchesSelectedResponseAction(obligation,
                    permanentCardId, currentCardId,
                    targetLocationPermanentId)) {
            return new PolicyResult(
                    "DEPLOY_PERSISTENT_RESPONSE_POLICY", List.of());
        }
        return scoreSelectedResponse(actionId, obligation,
                existingV166ContestScore, opponentStillPresent,
                persistentFactStillCurrent, criticalRoleStillCurrent);
    }

    private static boolean isEligible(CandidateFacts candidate) {
        if (!candidate.execution().executable()) {
            return false;
        }
        if (candidate.kind() == CandidateKind.EXISTING_PLAN) {
            if (candidate.fundedMandatoryObjective()
                    && isTerminalHardLoss(candidate.role())) {
                return true;
            }
            return candidate.formation().route()
                    == DeployTacticalPolicy.ResponseFormationRoute
                    .EXISTING_LEGAL_ALTERNATIVE
                    && candidate.threatLocation().permanentCardId()
                    != candidate.responseTargetLocation().permanentCardId()
                    && alternativeValue(candidate) > 0;
        }
        if (!candidate.opponentPresent()
                || !candidate.formation().responseViable()) {
            return false;
        }
        boolean persistent = candidate.consecutiveOpponentTurns() >= 2
                && candidate.projectedAvoidedDamage() > 0;
        boolean critical = isTerminalHardLoss(candidate.role());
        if (!persistent && !critical) {
            return false;
        }
        return candidate.formation().route()
                != DeployTacticalPolicy.ResponseFormationRoute.V170_SPY
                || persistent;
    }

    private static CandidateFacts bestResponse(
            List<CandidateFacts> candidates) {
        CandidateFacts best = null;
        for (CandidateFacts candidate : candidates) {
            if (best == null || compareResponse(candidate, best) > 0) {
                best = candidate;
            }
        }
        return Objects.requireNonNull(best, "bestResponse");
    }

    private static CandidateFacts bestAlternative(
            List<CandidateFacts> candidates, int threatPermanentId) {
        CandidateFacts best = null;
        for (CandidateFacts candidate : candidates) {
            if (candidate.kind() != CandidateKind.EXISTING_PLAN
                    || candidate.threatLocation().permanentCardId()
                    != threatPermanentId
                    || candidate.fundedMandatoryObjective()
                    && isTerminalHardLoss(candidate.role())) {
                continue;
            }
            if (best == null || compareAlternative(candidate, best) > 0) {
                best = candidate;
            }
        }
        return best;
    }

    private static CandidateFacts bestOverall(List<CandidateFacts> candidates) {
        CandidateFacts best = null;
        for (CandidateFacts candidate : candidates) {
            if (best == null || compareOverall(candidate, best) > 0) {
                best = candidate;
            }
        }
        return Objects.requireNonNull(best, "bestCandidate");
    }

    private static int compareResponse(CandidateFacts left,
                                       CandidateFacts right) {
        int comparison = Integer.compare(left.projectedAvoidedDamage(),
                right.projectedAvoidedDamage());
        if (comparison != 0) return comparison;
        comparison = Boolean.compare(isCriticalCandidate(left),
                isCriticalCandidate(right));
        if (comparison != 0) return comparison;
        comparison = Integer.compare(left.strategicIncome(),
                right.strategicIncome());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(
                right.execution().exactTotalCost(),
                left.execution().exactTotalCost());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(
                right.execution().exactInstructionCount(),
                left.execution().exactInstructionCount());
        if (comparison != 0) return comparison;
        return right.candidateKey().compareTo(left.candidateKey());
    }

    private static int compareAlternative(CandidateFacts left,
                                          CandidateFacts right) {
        int comparison = Integer.compare(alternativeValue(left),
                alternativeValue(right));
        if (comparison != 0) return comparison;
        comparison = Integer.compare(left.currentFriendlyDrainIncome(),
                right.currentFriendlyDrainIncome());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(
                right.execution().exactTotalCost(),
                left.execution().exactTotalCost());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(
                right.execution().exactInstructionCount(),
                left.execution().exactInstructionCount());
        if (comparison != 0) return comparison;
        return right.candidateKey().compareTo(left.candidateKey());
    }

    private static int compareOverall(CandidateFacts left,
                                      CandidateFacts right) {
        int comparison = Boolean.compare(
                left.fundedMandatoryObjective()
                        && isTerminalHardLoss(left.role()),
                right.fundedMandatoryObjective()
                        && isTerminalHardLoss(right.role()));
        if (comparison != 0) return comparison;
        comparison = Integer.compare(publicValue(left), publicValue(right));
        if (comparison != 0) return comparison;
        comparison = Boolean.compare(isCriticalCandidate(left),
                isCriticalCandidate(right));
        if (comparison != 0) return comparison;
        comparison = Integer.compare(left.projectedAvoidedDamage(),
                right.projectedAvoidedDamage());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(left.strategicIncome(),
                right.strategicIncome());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(left.currentFriendlyDrainIncome(),
                right.currentFriendlyDrainIncome());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(
                right.execution().exactTotalCost(),
                left.execution().exactTotalCost());
        if (comparison != 0) return comparison;
        comparison = Integer.compare(
                right.execution().exactInstructionCount(),
                left.execution().exactInstructionCount());
        if (comparison != 0) return comparison;
        return right.candidateKey().compareTo(left.candidateKey());
    }

    private static int publicValue(CandidateFacts candidate) {
        return candidate.kind() == CandidateKind.RESPONSE_TARGET
                ? candidate.projectedAvoidedDamage()
                : alternativeValue(candidate);
    }

    private static int alternativeValue(CandidateFacts candidate) {
        int preservedTwoTurnIncome = candidate.currentFriendlyDrainIncome()
                * 2;
        return switch (candidate.mode()) {
            case RACE -> Math.max(preservedTwoTurnIncome,
                    candidate.raceValue());
            case CONTEST, REINFORCE, SPY, MASS_TOWARD -> 0;
        };
    }

    private static boolean isCriticalCandidate(CandidateFacts candidate) {
        return candidate.formation().route()
                != DeployTacticalPolicy.ResponseFormationRoute.V170_SPY
                && isTerminalHardLoss(candidate.role());
    }

    private static boolean isTerminalHardLoss(TargetRole role) {
        return role == TargetRole.OBJECTIVE_HARD_LOSS_DEFENSE;
    }

    private static boolean isObjectivePreferenceRole(TargetRole role) {
        return isTerminalHardLoss(role)
                || role == TargetRole.ACTIVE_FLIP_GATE
                || role == TargetRole.POST_FLIP_PROTECTION
                || role == TargetRole.MISSING_REQUIRED_LOCATION;
    }

    private static Obligation toObligation(CandidateFacts selected,
                                           String reasonCode) {
        boolean scoredResponse = selected.kind()
                == CandidateKind.RESPONSE_TARGET
                && selected.formation().route()
                != DeployTacticalPolicy.ResponseFormationRoute.V170_SPY;
        int persistentBonus = scoredResponse
                && selected.consecutiveOpponentTurns() >= 2
                && selected.projectedAvoidedDamage() > 0
                ? PERSISTENT_RESPONSE_BONUS : 0;
        int criticalBonus = scoredResponse
                && isObjectivePreferenceRole(selected.role())
                ? CRITICAL_OCCUPATION_BONUS : 0;
        return new Obligation(selected.candidateKey(), selected.kind(),
                selected.responseActions(),
                selected.threatLocation(), selected.responseTargetLocation(),
                selected.role(), selected.mode(), persistentBonus,
                criticalBonus, reasonCode);
    }

    private static PolicyOperation add(String actionId, String ruleId,
                                       float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.DEPLOY_SITING, TraceOutputKind.BANDED,
                delta, reason);
    }

    private static PolicyOperation addObjective(
            String actionId, String ruleId, float delta, String reason) {
        return PolicyOperation.add(actionId, TraceRuleId.of(ruleId),
                TraceDomainId.OBJECTIVE_INTENT, TraceOutputKind.BANDED,
                delta, reason);
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    private static void requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
    }

    private static void requireNonnegative(int value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must be nonnegative");
        }
    }

    private static void requireNonnegative(float value, String name) {
        if (value < 0.0f) {
            throw new IllegalArgumentException(name + " must be nonnegative");
        }
    }
}
