package com.gempukku.swccgo.ai.models.rando.strategy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The complete deployment plan for a deploy phase.
 *
 * Contains:
 * - Overall strategy (HOLD_BACK, ESTABLISH, REINFORCE, etc.)
 * - Specific deployment instructions in priority order
 * - Budget tracking
 *
 * Ported from Python deploy_planner.py DeploymentPlan dataclass.
 */
public class DeploymentPlan {
    private DeployStrategy strategy;
    private String reason;

    // SPECIFIC deployment instructions in priority order
    private List<DeploymentInstruction> instructions = new ArrayList<>();

    // Cards we explicitly should NOT deploy
    private Set<String> holdBackCards = new HashSet<>();

    // Budget tracking
    private int totalForceAvailable = 0;
    private int forceReservedForBattle = 2;  // Reserve some for battle destiny/effects
    private int forceToSpend = 0;

    // Phase state
    private boolean phaseStarted = false;
    private int deploymentsMade = 0;

    // Flag set by evaluator when planned cards aren't available
    private boolean forceAllowExtras = false;

    // Flag set when planned cards are in hand but not deployable (e.g., can't afford)
    // This indicates we should PASS and save force rather than deploying random stuff
    private boolean waitingForPlannedCards = false;

    // Original plan cost (before any deployments)
    private int originalPlanCost = 0;

    public DeploymentPlan(DeployStrategy strategy, String reason) {
        this.strategy = strategy;
        this.reason = reason;
    }

    /** Detached evaluator view. Mutations cannot leak into the accepted phase state. */
    public DeploymentPlan assessmentCopy() {
        DeploymentPlan copy = new DeploymentPlan(strategy, reason);
        copy.instructions = new ArrayList<>();
        for (DeploymentInstruction instruction : instructions) {
            copy.instructions.add(new DeploymentInstruction(instruction));
        }
        copy.holdBackCards = new HashSet<>(holdBackCards);
        copy.totalForceAvailable = totalForceAvailable;
        copy.forceReservedForBattle = forceReservedForBattle;
        copy.forceToSpend = forceToSpend;
        copy.phaseStarted = phaseStarted;
        copy.deploymentsMade = deploymentsMade;
        copy.forceAllowExtras = forceAllowExtras;
        copy.waitingForPlannedCards = waitingForPlannedCards;
        copy.originalPlanCost = originalPlanCost;
        return copy;
    }

    /**
     * Check if a card is in our deployment plan.
     */
    public boolean shouldDeployCard(String blueprintId) {
        return instructions.stream()
            .anyMatch(inst -> blueprintId.equals(inst.getCardBlueprintId()));
    }

    /**
     * Get the deployment instruction for a specific card.
     */
    public DeploymentInstruction getInstructionForCard(String blueprintId) {
        DeploymentInstruction match = null;
        for (DeploymentInstruction instruction : instructions) {
            if (!blueprintId.equals(instruction.getCardBlueprintId())) {
                continue;
            }
            if (match != null) {
                return null;
            }
            match = instruction;
        }
        return match;
    }

    /**
     * Find the instruction for one physical copy. Blueprint fallback exists only
     * for plans created before physical identity was recorded.
     */
    public DeploymentInstruction getInstructionForPhysicalCard(int permanentCardId,
                                                               int currentCardId,
                                                               String blueprintId) {
        DeploymentInstruction exact = instructions.stream()
            .filter(inst -> inst.getCardPermanentCardId() != null
                && inst.getCardCurrentCardId() != null
                && inst.getCardPermanentCardId() == permanentCardId
                && inst.getCardCurrentCardId() == currentCardId)
            .findFirst()
            .orElse(null);
        if (exact != null) {
            return exact;
        }
        DeploymentInstruction legacyMatch = null;
        for (DeploymentInstruction instruction : instructions) {
            if (instruction.getCardPermanentCardId() != null
                    || blueprintId == null
                    || !blueprintId.equals(instruction.getCardBlueprintId())) {
                continue;
            }
            if (legacyMatch != null) {
                return null;
            }
            legacyMatch = instruction;
        }
        return legacyMatch;
    }

    /**
     * Get the target location for a card, if any.
     */
    public String getTargetForCard(String blueprintId) {
        DeploymentInstruction inst = getInstructionForCard(blueprintId);
        return inst != null ? inst.getTargetLocationId() : null;
    }

    /**
     * Check if all planned deployments have been executed.
     */
    public boolean isPlanComplete() {
        return instructions.isEmpty() && deploymentsMade > 0;
    }

    /**
     * Calculate how much extra force is available for non-planned actions.
     *
     * Extra actions are allowed when:
     * 1. Plan is complete (all instructions executed)
     * 2. We have force above the reserved amount
     */
    public int getExtraForceBudget(int currentForce) {
        if (!isPlanComplete() && !forceAllowExtras) {
            return 0;  // Still executing plan
        }
        return Math.max(0, currentForce - forceReservedForBattle);
    }

    /**
     * Remove a deployment instruction (after card is deployed).
     */
    public void recordDeployment(String blueprintId) {
        DeploymentInstruction instruction = getInstructionForCard(blueprintId);
        if (instruction != null) {
            instructions.remove(instruction);
            deploymentsMade++;
        }
    }

    /** Remove only the accepted physical copy; duplicate blueprints remain planned. */
    public void recordDeployment(int permanentCardId, int currentCardId, String blueprintId) {
        DeploymentInstruction exact = getInstructionForPhysicalCard(
            permanentCardId, currentCardId, blueprintId);
        if (exact != null) {
            instructions.remove(exact);
            deploymentsMade++;
        }
    }

    /**
     * Add a deployment instruction.
     */
    public void addInstruction(DeploymentInstruction instruction) {
        instructions.add(instruction);
    }

    // Getters and setters
    public DeployStrategy getStrategy() { return strategy; }
    public void setStrategy(DeployStrategy strategy) { this.strategy = strategy; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public List<DeploymentInstruction> getInstructions() { return instructions; }
    public void setInstructions(List<DeploymentInstruction> instructions) { this.instructions = instructions; }

    public Set<String> getHoldBackCards() { return holdBackCards; }
    public void setHoldBackCards(Set<String> holdBackCards) { this.holdBackCards = holdBackCards; }

    public int getTotalForceAvailable() { return totalForceAvailable; }
    public void setTotalForceAvailable(int totalForceAvailable) { this.totalForceAvailable = totalForceAvailable; }

    public int getForceReservedForBattle() { return forceReservedForBattle; }
    public void setForceReservedForBattle(int forceReservedForBattle) { this.forceReservedForBattle = forceReservedForBattle; }

    public int getForceToSpend() { return forceToSpend; }
    public void setForceToSpend(int forceToSpend) { this.forceToSpend = forceToSpend; }

    public boolean isPhaseStarted() { return phaseStarted; }
    public void setPhaseStarted(boolean phaseStarted) { this.phaseStarted = phaseStarted; }

    public int getDeploymentsMade() { return deploymentsMade; }
    public void setDeploymentsMade(int deploymentsMade) { this.deploymentsMade = deploymentsMade; }

    public boolean isForceAllowExtras() { return forceAllowExtras; }
    public void setForceAllowExtras(boolean forceAllowExtras) { this.forceAllowExtras = forceAllowExtras; }

    public boolean isWaitingForPlannedCards() { return waitingForPlannedCards; }
    public void setWaitingForPlannedCards(boolean waitingForPlannedCards) { this.waitingForPlannedCards = waitingForPlannedCards; }

    public int getOriginalPlanCost() { return originalPlanCost; }
    public void setOriginalPlanCost(int originalPlanCost) { this.originalPlanCost = originalPlanCost; }
}
