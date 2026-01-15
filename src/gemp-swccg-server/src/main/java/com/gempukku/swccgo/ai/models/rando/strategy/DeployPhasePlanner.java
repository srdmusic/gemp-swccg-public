package com.gempukku.swccgo.ai.models.rando.strategy;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.rando.RandoConfig;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import com.gempukku.swccgo.ai.models.rando.RandoLogger;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Deploy Phase Planner - Creates holistic deployment plans for the entire phase.
 *
 * Strategic flow:
 * 1. DEPLOY LOCATIONS FIRST - opens new deployment options
 * 2. REDUCE HARM - reinforce contested locations where we're losing
 * 3. GAIN GROUND - establish at uncontested locations with opponent icons
 * 4. NEVER deploy to 0-icon uncontested locations
 *
 * The planner outputs SPECIFIC deployment instructions:
 * - Which cards to deploy
 * - Which location each card should go to
 * - Priority order for execution
 *
 * Ported from Python deploy_planner.py DeployPhasePlanner class.
 */
public class DeployPhasePlanner {
    private static final Logger LOG = RandoLogger.getStrategyLogger();

    // Config constants
    private final int deployThreshold;
    private final int battleForceReserve;

    // Current plan (cached)
    private DeploymentPlan currentPlan;
    private int lastPlanTurn = -1;

    public DeployPhasePlanner() {
        this(RandoConfig.DEPLOY_THRESHOLD, RandoConfig.BATTLE_FORCE_RESERVE);
    }

    public DeployPhasePlanner(int deployThreshold, int battleForceReserve) {
        this.deployThreshold = deployThreshold;
        this.battleForceReserve = battleForceReserve;
    }

    /**
     * Reset planner state for a new game.
     */
    public void reset() {
        currentPlan = null;
        lastPlanTurn = -1;
    }

    /**
     * Create a deployment plan for this phase.
     *
     * @param game The current game
     * @param playerId Our player ID
     * @param side Our side (DARK or LIGHT)
     * @return The deployment plan
     */
    public DeploymentPlan createPlan(SwccgGame game, String playerId, Side side) {
        GameState gameState = game.getGameState();
        if (gameState == null) {
            return createHoldBackPlan("No game state available");
        }

        int currentTurn = gameState.getPlayersLatestTurnNumber(playerId);
        String opponentId = gameState.getOpponent(playerId);

        // If we already have a plan for this turn, return it
        if (currentPlan != null && lastPlanTurn == currentTurn) {
            LOG.debug("📋 Returning cached plan for turn {} ({} instructions remaining)",
                currentTurn, currentPlan.getInstructions().size());
            return currentPlan;
        }

        // === COMPREHENSIVE DEPLOYMENT PLANNING (like Python) ===
        LOG.info("📋 Creating comprehensive deployment plan...");

        // Get available resources
        int forceAvailable = gameState.getForcePileSize(playerId);
        int lifeForce = gameState.getPlayerLifeForce(playerId);
        List<PhysicalCard> hand = gameState.getHand(playerId);

        // === DEPLOY CONFIG (like Python) ===
        LOG.info("📊 DEPLOY CONFIG: early_game_threshold={}, early_game_turns={}, deploy_threshold={}, overkill_threshold={}",
                RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD, RandoConfig.DEPLOY_EARLY_GAME_TURNS,
                deployThreshold, RandoConfig.DEPLOY_OVERKILL_THRESHOLD);
        LOG.info("   🎭 My side: {}", side);
        LOG.info("   📊 Raw board_state: force_pile={}, cards_in_hand={}, turn={}", forceAvailable, hand.size(), currentTurn);

        // === FULL HAND LISTING (like Python) ===
        LOG.info("   🃏 Full hand ({} cards):", hand.size());
        int idx = 0;
        int weaponCount = 0;
        int effectCount = 0;
        int interruptCount = 0;
        for (PhysicalCard card : hand) {
            if (card == null || card.getBlueprint() == null) continue;
            SwccgCardBlueprint bp = card.getBlueprint();
            CardCategory cat = bp.getCardCategory();
            String catName = cat != null ? cat.name() : "UNKNOWN";

            // Power only exists on Character, Starship, Vehicle
            int powerVal = 0;
            if (bp.hasPowerAttribute()) {
                Float power = bp.getPower();
                powerVal = power != null ? power.intValue() : 0;
            }

            // DeployCost exists on most cards EXCEPT Interrupts
            int deployCostVal = 0;
            if (cat != CardCategory.INTERRUPT) {
                try {
                    Float deployCost = bp.getDeployCost();
                    deployCostVal = deployCost != null ? deployCost.intValue() : 0;
                } catch (UnsupportedOperationException e) {
                    // Card type doesn't support deployCost - use 0
                }
            }

            LOG.info("      [{}] {} ({}) - Power: {}, Deploy: {}",
                    idx++, card.getTitle(), catName, powerVal, deployCostVal);
            if (cat == CardCategory.WEAPON) weaponCount++;
            if (cat == CardCategory.EFFECT) effectCount++;
            if (cat == CardCategory.INTERRUPT) interruptCount++;
        }

        // === STEP 1: Categorize hand ===
        List<PhysicalCard> locationsInHand = new ArrayList<>();
        List<PhysicalCard> charactersInHand = new ArrayList<>();
        List<PhysicalCard> starshipsInHand = new ArrayList<>();
        List<PhysicalCard> vehiclesInHand = new ArrayList<>();

        categorizeHand(hand, locationsInHand, charactersInHand, starshipsInHand, vehiclesInHand);

        LOG.info("   Hand: {} locations, {} characters, {} starships, {} vehicles, {} weapons",
                locationsInHand.size(), charactersInHand.size(), starshipsInHand.size(),
                vehiclesInHand.size(), weaponCount);

        // Log characters with power and cost (like Python)
        if (!charactersInHand.isEmpty()) {
            StringBuilder sb = new StringBuilder("   📋 Characters available: [");
            for (PhysicalCard c : charactersInHand) {
                Float power = c.getBlueprint().getPower();
                Float cost = c.getBlueprint().getDeployCost();
                sb.append("('").append(c.getTitle()).append("', ");
                sb.append(power != null ? power.intValue() : 0).append(", ");
                sb.append(cost != null ? cost.intValue() : 0).append("), ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }
        if (!starshipsInHand.isEmpty()) {
            StringBuilder sb = new StringBuilder("   🚀 Starships available: [");
            for (PhysicalCard c : starshipsInHand) {
                Float power = c.getBlueprint().getPower();
                Float cost = c.getBlueprint().getDeployCost();
                sb.append("('").append(c.getTitle()).append("', ");
                sb.append(power != null ? power.intValue() : 0).append(", ");
                sb.append(cost != null ? cost.intValue() : 0).append("), ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        } else {
            LOG.info("   ⚠️ No starships in hand!");
        }
        if (!vehiclesInHand.isEmpty()) {
            StringBuilder sb = new StringBuilder("   🚗 Vehicles available: [");
            for (PhysicalCard c : vehiclesInHand) {
                Float power = c.getBlueprint().getPower();
                Float cost = c.getBlueprint().getDeployCost();
                sb.append("('").append(c.getTitle()).append("', ");
                sb.append(power != null ? power.intValue() : 0).append(", ");
                sb.append(cost != null ? cost.intValue() : 0).append("), ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }

        // === STEP 2: Analyze board state ===
        // ALWAYS analyze board state - don't return early for locations!
        // Get ALL locations for analysis (like Python)
        List<AiBoardAnalyzer.LocationAnalysis> allLocations = AiBoardAnalyzer.analyzeAllLocations(
            game, playerId, opponentId, side);

        LOG.info("   📍 Analyzed {} locations on board:", allLocations.size());
        for (AiBoardAnalyzer.LocationAnalysis loc : allLocations) {
            String locType = loc.isSpace() ? "SPACE" : "GROUND";
            LOG.info("      - {}: {}, my={}, their={}, my_icons={}, their_icons={}",
                    loc.location.getTitle(), locType,
                    (int) loc.ourPower, (int) loc.theirPower,
                    loc.ourForceIcons, loc.theirForceIcons);
        }

        // Categorize locations
        List<AiBoardAnalyzer.LocationAnalysis> losingLocations = AiBoardAnalyzer.getLosingLocations(
            game, playerId, opponentId, side);
        List<AiBoardAnalyzer.LocationAnalysis> winningLocations = AiBoardAnalyzer.getWinningLocations(
            game, playerId, opponentId, side);
        List<AiBoardAnalyzer.LocationAnalysis> opponentOnlyLocations = AiBoardAnalyzer.getOpponentOnlyLocations(
            game, playerId, opponentId, side);

        LOG.info("   📊 Location summary: {} losing, {} winning, {} opponent-only",
            losingLocations.size(), winningLocations.size(), opponentOnlyLocations.size());

        // Log opponent board state (like Python)
        if (!losingLocations.isEmpty() || !opponentOnlyLocations.isEmpty()) {
            LOG.info("   👁️ OPPONENT BOARD:");
            for (AiBoardAnalyzer.LocationAnalysis loc : losingLocations) {
                LOG.info("      - {}: {} power (CONTESTED, we're losing by {})",
                        loc.location.getTitle(), (int) loc.theirPower, (int) -loc.getPowerAdvantage());
            }
            for (AiBoardAnalyzer.LocationAnalysis loc : opponentOnlyLocations) {
                LOG.info("      - {}: {} power (UNCONTESTED)",
                        loc.location.getTitle(), (int) loc.theirPower);
            }
        }

        // === STEP 3: Calculate dynamic thresholds ===
        // Ground and space tracked SEPARATELY (like Python)
        int groundThreshold = getDynamicThreshold(allLocations, false, currentTurn, lifeForce);
        int spaceThreshold = getDynamicThreshold(allLocations, true, currentTurn, lifeForce);

        LOG.info("   📊 Dynamic thresholds: ground={}, space={} (turn {}, life={})",
                groundThreshold, spaceThreshold, currentTurn, lifeForce);

        // === STEP 4: Calculate what we can deploy ===
        int totalDeployablePower = calculateTotalDeployablePower(
            charactersInHand, starshipsInHand, vehiclesInHand, forceAvailable);

        // Calculate ground and space power separately
        int groundPower = 0;
        for (PhysicalCard c : charactersInHand) {
            if (c.getBlueprint().hasPowerAttribute()) {
                Float p = c.getBlueprint().getPower();
                groundPower += p != null ? p.intValue() : 0;
            }
        }
        for (PhysicalCard v : vehiclesInHand) {
            if (v.getBlueprint().hasPowerAttribute()) {
                Float p = v.getBlueprint().getPower();
                groundPower += p != null ? p.intValue() : 0;
            }
        }
        int spacePower = 0;
        for (PhysicalCard s : starshipsInHand) {
            if (s.getBlueprint().hasPowerAttribute()) {
                Float p = s.getBlueprint().getPower();
                spacePower += p != null ? p.intValue() : 0;
            }
        }

        LOG.info("📋 Deployable power: ground={}, space={} (thresholds: {}/{})",
                groundPower, spacePower, groundThreshold, spaceThreshold);

        // Check deploy threshold for each domain
        // NOTE: Locations are ALWAYS deployed regardless of threshold!
        boolean holdBackGround = groundPower < groundThreshold;
        boolean holdBackSpace = spacePower < spaceThreshold;

        if (holdBackGround && holdBackSpace) {
            LOG.info("📋 Will hold back both domains: ground {} < {}, space {} < {}",
                groundPower, groundThreshold, spacePower, spaceThreshold);
        } else if (holdBackGround) {
            LOG.info("📋 Will hold back ground: {} < {}", groundPower, groundThreshold);
        } else if (holdBackSpace) {
            LOG.info("📋 Will hold back space: {} < {}", spacePower, spaceThreshold);
        }

        // === STEP 5: Build COMPREHENSIVE deployment plan ===
        // Like Python: build ONE plan with locations AND characters, tracking force remaining
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.COMPREHENSIVE, "Comprehensive deployment plan");
        int forceRemaining = forceAvailable - battleForceReserve;

        // --- PRIORITY 0: Deploy locations first (opens new options) ---
        for (PhysicalCard location : locationsInHand) {
            SwccgCardBlueprint blueprint = location.getBlueprint();
            if (blueprint == null) continue;

            int cost = 0;
            try {
                Float deployCost = blueprint.getDeployCost();
                cost = deployCost != null ? deployCost.intValue() : 0;
            } catch (UnsupportedOperationException e) {
                // Skip cards without deploy cost
                continue;
            }

            if (cost <= forceRemaining) {
                String blueprintId = location.getBlueprintId(true);
                String title = location.getTitle();

                DeploymentInstruction inst = new DeploymentInstruction(
                    blueprintId, title, null, null, 0, "Deploy location to open new options"
                );
                inst.setDeployCost(cost);
                plan.addInstruction(inst);
                forceRemaining -= cost;

                LOG.info("   📍 Plan: Deploy location {} (cost {})", title, cost);

                // TODO: Create virtual LocationAnalysis for newly deployed locations
                // so we can plan character deployments there (like Python does)
            }
        }

        // If holding back characters, just return location-only plan
        if (holdBackCharacters) {
            if (plan.getInstructions().isEmpty()) {
                LOG.info("📋 FINAL PLAN: hold_back - Waiting for more power");
                currentPlan = createHoldBackPlan(String.format(
                    "Waiting for more power (have %d, need %d)", totalDeployablePower, deployThreshold));
            } else {
                LOG.info("📋 FINAL PLAN: locations only - holding back characters");
                plan.setStrategy(DeployStrategy.DEPLOY_LOCATIONS);
                plan.setReason("Deploy locations, hold characters");
                currentPlan = plan;
                logFinalPlan(plan);
            }
            lastPlanTurn = currentTurn;
            return currentPlan;
        }

        // --- PRIORITY 1: Reinforce losing locations ---
        // Make copies of lists since we'll be removing cards as we plan them
        List<PhysicalCard> availableChars = new ArrayList<>(charactersInHand);
        List<PhysicalCard> availableShips = new ArrayList<>(starshipsInHand);
        List<PhysicalCard> availableVehicles = new ArrayList<>(vehiclesInHand);

        // Sort losing locations by severity (biggest deficit first)
        losingLocations.sort((a, b) -> Double.compare(a.getPowerAdvantage(), b.getPowerAdvantage()));

        for (AiBoardAnalyzer.LocationAnalysis loc : losingLocations) {
            if (forceRemaining <= 0) break;

            PhysicalCard bestCard = findBestCardForLocation(loc, availableChars, availableShips,
                availableVehicles, forceRemaining);

            if (bestCard != null) {
                addCardToPlan(plan, bestCard, loc, 1, "Reinforce losing position", forceRemaining);
                forceRemaining -= getCardDeployCost(bestCard);
                availableChars.remove(bestCard);
                availableShips.remove(bestCard);
                availableVehicles.remove(bestCard);
            }
        }

        // --- PRIORITY 2: Establish at opponent-only locations ---
        // Sort by opponent force icons (highest value targets first)
        opponentOnlyLocations.sort((a, b) -> Integer.compare(b.theirForceIcons, a.theirForceIcons));

        int establishCount = 0;
        int maxEstablish = 2;  // Don't spread too thin

        for (AiBoardAnalyzer.LocationAnalysis loc : opponentOnlyLocations) {
            if (forceRemaining <= 0) break;
            if (establishCount >= maxEstablish) break;
            if (loc.theirForceIcons <= 0) continue;  // Skip 0-icon locations

            PhysicalCard bestCard = findBestCardForLocation(loc, availableChars, availableShips,
                availableVehicles, forceRemaining);

            if (bestCard != null) {
                addCardToPlan(plan, bestCard, loc, 2,
                    String.format("Establish at %s (%d opponent icons)", loc.location.getTitle(), loc.theirForceIcons),
                    forceRemaining);
                forceRemaining -= getCardDeployCost(bestCard);
                establishCount++;
                availableChars.remove(bestCard);
                availableShips.remove(bestCard);
                availableVehicles.remove(bestCard);
            }
        }

        // --- PRIORITY 3: Build up at winning locations (if not overkill) ---
        for (AiBoardAnalyzer.LocationAnalysis loc : winningLocations) {
            if (forceRemaining <= 0) break;
            // Skip if we already have overkill
            if (loc.getPowerAdvantage() >= RandoConfig.DEPLOY_OVERKILL_THRESHOLD) {
                continue;
            }

            PhysicalCard bestCard = findBestCardForLocation(loc, availableChars, availableShips,
                availableVehicles, forceRemaining);

            if (bestCard != null) {
                addCardToPlan(plan, bestCard, loc, 3,
                    String.format("Build up at %s", loc.location.getTitle()),
                    forceRemaining);
                forceRemaining -= getCardDeployCost(bestCard);
                availableChars.remove(bestCard);
                availableShips.remove(bestCard);
                availableVehicles.remove(bestCard);
            }
        }

        // Set final strategy based on what we planned
        if (plan.getInstructions().isEmpty()) {
            LOG.info("📋 FINAL PLAN: hold_back - No strategic deployment targets");
            currentPlan = createHoldBackPlan("No strategic deployment targets");
        } else {
            // Determine primary strategy from instructions
            boolean hasLocations = plan.getInstructions().stream().anyMatch(i -> i.getPriority() == 0);
            boolean hasReinforce = plan.getInstructions().stream().anyMatch(i -> i.getPriority() == 1);
            boolean hasEstablish = plan.getInstructions().stream().anyMatch(i -> i.getPriority() == 2);

            if (hasReinforce) {
                plan.setStrategy(DeployStrategy.REINFORCE);
                plan.setReason("Reinforce losing positions" + (hasLocations ? " + deploy locations" : ""));
            } else if (hasEstablish) {
                plan.setStrategy(DeployStrategy.ESTABLISH);
                plan.setReason("Establish at opponent locations" + (hasLocations ? " + deploy locations" : ""));
            } else if (hasLocations) {
                plan.setStrategy(DeployStrategy.DEPLOY_LOCATIONS);
                plan.setReason("Deploy locations");
            } else {
                plan.setStrategy(DeployStrategy.OVERWHELM);
                plan.setReason("Build up positions");
            }

            currentPlan = plan;
            logFinalPlan(plan);
        }

        lastPlanTurn = currentTurn;
        return currentPlan;
    }

    /**
     * Helper to add a card to the deployment plan.
     */
    private void addCardToPlan(DeploymentPlan plan, PhysicalCard card, AiBoardAnalyzer.LocationAnalysis loc,
                                int priority, String reason, int forceRemaining) {
        SwccgCardBlueprint blueprint = card.getBlueprint();
        int cost = getCardDeployCost(card);
        int power = 0;
        if (blueprint.hasPowerAttribute()) {
            Float p = blueprint.getPower();
            power = p != null ? p.intValue() : 0;
        }

        String blueprintId = card.getBlueprintId(true);
        String title = card.getTitle();
        String locId = String.valueOf(loc.location.getCardId());
        String locName = loc.location.getTitle();

        DeploymentInstruction inst = new DeploymentInstruction(
            blueprintId, title, locId, locName, priority, reason
        );
        inst.setDeployCost(cost);
        inst.setPowerContribution(power);
        plan.addInstruction(inst);

        LOG.info("   📋 Plan: {} -> {} (cost {}, power {}) - {}", title, locName, cost, power, reason);
    }

    /**
     * Helper to get deploy cost safely.
     */
    private int getCardDeployCost(PhysicalCard card) {
        try {
            Float cost = card.getBlueprint().getDeployCost();
            return cost != null ? cost.intValue() : 0;
        } catch (UnsupportedOperationException e) {
            return 0;
        }
    }

    /**
     * Get a score for deploying a specific card.
     *
     * @param blueprintId The card to check
     * @param currentForce Available force
     * @param availableBlueprints All blueprints in the current decision
     * @return (score, reason) tuple
     */
    public PlanScore getCardScore(String blueprintId, int currentForce, List<String> availableBlueprints) {
        if (currentPlan == null) {
            return new PlanScore(0.0f, "No active plan");
        }

        DeploymentInstruction instruction = currentPlan.getInstructionForCard(blueprintId);
        if (instruction != null) {
            // Card is in the plan!
            float score = 100.0f - (instruction.getPriority() * 10);  // Higher priority = higher score
            return new PlanScore(score, "IN PLAN: " + instruction.getReason());
        }

        // Card is not in the plan
        if (currentPlan.isPlanComplete() || currentPlan.isForceAllowExtras()) {
            // Plan is done, allow extra actions with modest score
            return new PlanScore(25.0f, "Extra action (plan complete)");
        }

        return new PlanScore(-50.0f, "NOT in plan - saving force for planned cards");
    }

    /**
     * Record that a card was deployed (remove from plan).
     */
    public void recordDeployment(String blueprintId) {
        if (currentPlan != null) {
            currentPlan.recordDeployment(blueprintId);
        }
    }

    /**
     * Get a summary of the current plan.
     */
    public String getPlanSummary() {
        if (currentPlan == null) {
            return "No plan";
        }
        return String.format("%s: %s (%d deployments)",
            currentPlan.getStrategy().getValue(),
            currentPlan.getReason(),
            currentPlan.getInstructions().size());
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Log the final deployment plan in detail (like Python).
     */
    private void logFinalPlan(DeploymentPlan plan) {
        String strategyName = plan.getStrategy() != null ? plan.getStrategy().getValue().toLowerCase() : "unknown";
        LOG.info("📋 FINAL PLAN: {} - {} deployments", strategyName, plan.getInstructions().size());

        int planNum = 1;
        for (DeploymentInstruction inst : plan.getInstructions()) {
            String targetName = inst.getTargetLocationName() != null ? inst.getTargetLocationName() : "table";
            String targetId = inst.getTargetLocationId() != null ? inst.getTargetLocationId() : "?";
            LOG.info("   {}. {} -> {} (id={}): {}",
                    planNum++,
                    inst.getCardName(),
                    targetName,
                    targetId,
                    inst.getReason());
        }
    }

    private void categorizeHand(List<PhysicalCard> hand,
                                 List<PhysicalCard> locations,
                                 List<PhysicalCard> characters,
                                 List<PhysicalCard> starships,
                                 List<PhysicalCard> vehicles) {
        for (PhysicalCard card : hand) {
            if (card == null) continue;
            SwccgCardBlueprint blueprint = card.getBlueprint();
            if (blueprint == null) continue;

            CardCategory category = blueprint.getCardCategory();
            if (category == CardCategory.LOCATION) {
                locations.add(card);
            } else if (category == CardCategory.CHARACTER) {
                characters.add(card);
            } else if (category == CardCategory.STARSHIP) {
                starships.add(card);
            } else if (category == CardCategory.VEHICLE) {
                vehicles.add(card);
            }
        }
    }

    private int calculateTotalDeployablePower(List<PhysicalCard> characters,
                                               List<PhysicalCard> starships,
                                               List<PhysicalCard> vehicles,
                                               int forceAvailable) {
        int totalPower = 0;
        int forceRemaining = forceAvailable;

        // Sort by power/cost ratio (best value first)
        List<PhysicalCard> allCards = new ArrayList<>();
        allCards.addAll(characters);
        allCards.addAll(starships);
        allCards.addAll(vehicles);

        allCards.sort((a, b) -> {
            float ratioA = getValueRatio(a);
            float ratioB = getValueRatio(b);
            return Float.compare(ratioB, ratioA);
        });

        for (PhysicalCard card : allCards) {
            SwccgCardBlueprint blueprint = card.getBlueprint();
            if (blueprint == null) continue;

            // Only get power if card has power attribute (defensive check)
            int powerVal = 0;
            if (blueprint.hasPowerAttribute()) {
                Float power = blueprint.getPower();
                powerVal = power != null ? power.intValue() : 0;
            }

            // Get deploy cost safely
            int cost = 0;
            try {
                Float deployCost = blueprint.getDeployCost();
                cost = deployCost != null ? deployCost.intValue() : 0;
            } catch (UnsupportedOperationException e) {
                // Card type doesn't support deployCost
            }

            if (cost <= forceRemaining) {
                totalPower += powerVal;
                forceRemaining -= cost;
            }
        }

        return totalPower;
    }

    private float getValueRatio(PhysicalCard card) {
        SwccgCardBlueprint blueprint = card.getBlueprint();
        if (blueprint == null) return 0;

        // Only get power if card has power attribute (Character, Starship, Vehicle)
        int powerVal = 0;
        if (blueprint.hasPowerAttribute()) {
            Float power = blueprint.getPower();
            powerVal = power != null ? power.intValue() : 0;
        }

        // Only get ability if card has ability attribute (Character, some Vehicles)
        int abilityVal = 0;
        if (blueprint.hasAbilityAttribute()) {
            Float ability = blueprint.getAbility();
            abilityVal = ability != null ? ability.intValue() : 0;
        }

        // Deploy cost - try to get it, default to 1 if not available
        int deployCost = 1;
        try {
            Float cost = blueprint.getDeployCost();
            deployCost = cost != null ? cost.intValue() : 1;
        } catch (UnsupportedOperationException e) {
            // Card type doesn't support deployCost
        }

        int value = powerVal + abilityVal;
        return deployCost > 0 ? (float) value / deployCost : value;
    }

    private DeploymentPlan createHoldBackPlan(String reason) {
        return new DeploymentPlan(DeployStrategy.HOLD_BACK, reason);
    }

    private DeploymentPlan createLocationDeployPlan(List<PhysicalCard> locations, int forceAvailable) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.DEPLOY_LOCATIONS,
            "Deploy locations first");

        for (PhysicalCard location : locations) {
            SwccgCardBlueprint blueprint = location.getBlueprint();
            if (blueprint == null) continue;

            Float deployCost = blueprint.getDeployCost();
            int cost = deployCost != null ? deployCost.intValue() : 0;

            if (cost <= forceAvailable) {
                String blueprintId = location.getBlueprintId(true);
                String title = location.getTitle();

                DeploymentInstruction inst = new DeploymentInstruction(
                    blueprintId, title, null, null, 0, "Deploy location first"
                );
                inst.setDeployCost(cost);
                plan.addInstruction(inst);

                forceAvailable -= cost;
            }
        }

        return plan.getInstructions().isEmpty() ? null : plan;
    }

    private DeploymentPlan createReinforcePlan(List<AiBoardAnalyzer.LocationAnalysis> losingLocations,
                                                List<PhysicalCard> characters,
                                                List<PhysicalCard> starships,
                                                List<PhysicalCard> vehicles,
                                                int forceAvailable,
                                                SwccgGame game) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Reinforce losing positions");

        // Sort by how badly we're losing (worst first)
        losingLocations.sort(Comparator.comparingDouble(loc -> loc.getPowerAdvantage()));

        for (AiBoardAnalyzer.LocationAnalysis loc : losingLocations) {
            if (forceAvailable <= battleForceReserve) break;

            // Try to find cards to deploy here
            PhysicalCard bestCard = findBestCardForLocation(loc, characters, starships, vehicles,
                forceAvailable - battleForceReserve);

            if (bestCard != null) {
                SwccgCardBlueprint blueprint = bestCard.getBlueprint();
                Float deployCost = blueprint.getDeployCost();
                Float power = blueprint.getPower();

                int cost = deployCost != null ? deployCost.intValue() : 0;
                int powerVal = power != null ? power.intValue() : 0;

                String blueprintId = bestCard.getBlueprintId(true);
                String title = bestCard.getTitle();
                String locId = String.valueOf(loc.location.getCardId());
                String locName = loc.location.getTitle();

                DeploymentInstruction inst = new DeploymentInstruction(
                    blueprintId, title, locId, locName, 1,
                    String.format("Reinforce %s (power diff: %d)", locName, (int) loc.getPowerAdvantage())
                );
                inst.setDeployCost(cost);
                inst.setPowerContribution(powerVal);
                plan.addInstruction(inst);

                forceAvailable -= cost;
                characters.remove(bestCard);
                starships.remove(bestCard);
                vehicles.remove(bestCard);
            }
        }

        return plan;
    }

    private DeploymentPlan createEstablishPlan(List<AiBoardAnalyzer.LocationAnalysis> opponentLocations,
                                                List<PhysicalCard> characters,
                                                List<PhysicalCard> starships,
                                                List<PhysicalCard> vehicles,
                                                int forceAvailable,
                                                SwccgGame game) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.ESTABLISH,
            "Establish presence at opponent locations");

        // Sort by opponent force icons (highest value targets first)
        opponentLocations.sort((a, b) -> Integer.compare(b.theirForceIcons, a.theirForceIcons));

        int establishCount = 0;
        int maxEstablish = 2;  // Don't spread too thin

        for (AiBoardAnalyzer.LocationAnalysis loc : opponentLocations) {
            if (forceAvailable <= battleForceReserve) break;
            if (establishCount >= maxEstablish) break;
            if (loc.theirForceIcons <= 0) continue;  // Skip 0-icon locations

            PhysicalCard bestCard = findBestCardForLocation(loc, characters, starships, vehicles,
                forceAvailable - battleForceReserve);

            if (bestCard != null) {
                SwccgCardBlueprint blueprint = bestCard.getBlueprint();
                Float deployCost = blueprint.getDeployCost();
                Float power = blueprint.getPower();

                int cost = deployCost != null ? deployCost.intValue() : 0;
                int powerVal = power != null ? power.intValue() : 0;

                String blueprintId = bestCard.getBlueprintId(true);
                String title = bestCard.getTitle();
                String locId = String.valueOf(loc.location.getCardId());
                String locName = loc.location.getTitle();

                DeploymentInstruction inst = new DeploymentInstruction(
                    blueprintId, title, locId, locName, 2,
                    String.format("Establish at %s (%d opponent icons)", locName, loc.theirForceIcons)
                );
                inst.setDeployCost(cost);
                inst.setPowerContribution(powerVal);
                plan.addInstruction(inst);

                forceAvailable -= cost;
                establishCount++;
                characters.remove(bestCard);
                starships.remove(bestCard);
                vehicles.remove(bestCard);
            }
        }

        return plan;
    }

    private DeploymentPlan createBuildUpPlan(List<AiBoardAnalyzer.LocationAnalysis> winningLocations,
                                              List<PhysicalCard> characters,
                                              List<PhysicalCard> starships,
                                              List<PhysicalCard> vehicles,
                                              int forceAvailable,
                                              SwccgGame game) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.OVERWHELM,
            "Build up winning positions");

        for (AiBoardAnalyzer.LocationAnalysis loc : winningLocations) {
            // Skip if we already have overkill
            if (loc.getPowerAdvantage() >= RandoConfig.DEPLOY_OVERKILL_THRESHOLD) {
                continue;
            }

            if (forceAvailable <= battleForceReserve) break;

            PhysicalCard bestCard = findBestCardForLocation(loc, characters, starships, vehicles,
                forceAvailable - battleForceReserve);

            if (bestCard != null) {
                SwccgCardBlueprint blueprint = bestCard.getBlueprint();
                Float deployCost = blueprint.getDeployCost();
                Float power = blueprint.getPower();

                int cost = deployCost != null ? deployCost.intValue() : 0;
                int powerVal = power != null ? power.intValue() : 0;

                String blueprintId = bestCard.getBlueprintId(true);
                String title = bestCard.getTitle();
                String locId = String.valueOf(loc.location.getCardId());
                String locName = loc.location.getTitle();

                DeploymentInstruction inst = new DeploymentInstruction(
                    blueprintId, title, locId, locName, 3,
                    String.format("Build up at %s (power: %d)", locName, (int) loc.getPowerAdvantage())
                );
                inst.setDeployCost(cost);
                inst.setPowerContribution(powerVal);
                plan.addInstruction(inst);

                forceAvailable -= cost;
                characters.remove(bestCard);
                starships.remove(bestCard);
                vehicles.remove(bestCard);
            }
        }

        return plan;
    }

    private PhysicalCard findBestCardForLocation(AiBoardAnalyzer.LocationAnalysis loc,
                                                   List<PhysicalCard> characters,
                                                   List<PhysicalCard> starships,
                                                   List<PhysicalCard> vehicles,
                                                   int maxCost) {
        // For ground locations, prefer characters
        // For space locations, prefer starships

        List<PhysicalCard> candidates = new ArrayList<>();
        if (loc.isGround()) {
            candidates.addAll(characters);
            candidates.addAll(vehicles);
        } else {
            candidates.addAll(starships);
        }

        PhysicalCard bestCard = null;
        float bestRatio = -1;

        for (PhysicalCard card : candidates) {
            SwccgCardBlueprint blueprint = card.getBlueprint();
            if (blueprint == null) continue;

            Float deployCost = blueprint.getDeployCost();
            int cost = deployCost != null ? deployCost.intValue() : 0;

            if (cost > maxCost) continue;

            float ratio = getValueRatio(card);
            if (ratio > bestRatio) {
                bestRatio = ratio;
                bestCard = card;
            }
        }

        return bestCard;
    }

    // =========================================================================
    // Dynamic Threshold Calculation (ported from Python _get_dynamic_threshold)
    // =========================================================================

    /**
     * Calculate dynamic deploy threshold based on game state.
     *
     * Threshold adjustments (applied in order):
     * 1. Early game (turn < 4) with no contested locations: -2 (relaxed)
     *    This allows 3-power characters to deploy and enable force drains.
     * 2. Late game with low life force: additional decay
     *    - life_force < 10: -2 (desperate - deploy anything)
     *    - life_force < 20: -1 (critical - very aggressive)
     *    - life_force < 30: -1 (urgent - somewhat aggressive)
     *
     * Ground and space are tracked SEPARATELY.
     *
     * @param locations All analyzed locations on board
     * @param isSpace True for space threshold, False for ground
     * @param turnNumber Current turn number
     * @param lifeForce Total remaining life force
     * @return Deploy threshold to use (minimum 1)
     */
    private int getDynamicThreshold(List<AiBoardAnalyzer.LocationAnalysis> locations,
                                     boolean isSpace, int turnNumber, int lifeForce) {
        int threshold = deployThreshold;
        String domain = isSpace ? "space" : "ground";

        // EARLY GAME RELAXATION: Before turn 4 with no contested locations
        boolean earlyGameRelaxed = false;
        if (turnNumber < 4) {
            // Check for contested locations in the relevant domain only
            boolean hasContested = false;
            for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                // Skip locations without both players present
                if (loc.ourPower <= 0 || loc.theirPower <= 0) {
                    continue;
                }

                // Check the appropriate domain
                if (isSpace && loc.isSystem) {
                    hasContested = true;
                    LOG.debug("   📊 Contested space found: {} ({} vs {})",
                        loc.location.getTitle(), (int)loc.ourPower, (int)loc.theirPower);
                    break;
                } else if (!isSpace && !loc.isSystem) {
                    hasContested = true;
                    LOG.debug("   📊 Contested ground found: {} ({} vs {})",
                        loc.location.getTitle(), (int)loc.ourPower, (int)loc.theirPower);
                    break;
                }
            }

            if (!hasContested) {
                // REACT THREAT CHECK: Before relaxing, check for large enemy buildups
                boolean hasReactThreat = false;
                for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                    // Check enemy power in the relevant domain
                    if (isSpace && loc.isSystem && loc.theirPower >= RandoConfig.REACT_THREAT_THRESHOLD) {
                        hasReactThreat = true;
                        LOG.debug("   ⚠️ React threat in space: {} has {} enemy power",
                            loc.location.getTitle(), (int)loc.theirPower);
                        break;
                    } else if (!isSpace && !loc.isSystem && loc.theirPower >= RandoConfig.REACT_THREAT_THRESHOLD) {
                        hasReactThreat = true;
                        LOG.debug("   ⚠️ React threat on ground: {} has {} enemy power",
                            loc.location.getTitle(), (int)loc.theirPower);
                        break;
                    }
                }

                if (hasReactThreat) {
                    LOG.debug("   📊 No threshold relaxation ({}): react threat exists", domain);
                } else {
                    // Safe to relax - no large enemy buildups
                    int minFloor = RandoConfig.MIN_ESTABLISH_POWER;
                    threshold = Math.max(minFloor, threshold - 2);
                    earlyGameRelaxed = true;
                }
            }
        }

        // LATE GAME LIFE FORCE DECAY: Lower threshold when losing badly
        int minFloor = RandoConfig.MIN_ESTABLISH_POWER;
        int lifeForceDecay = 0;
        if (lifeForce < 10) {
            lifeForceDecay = 2;  // Desperate: can go slightly below floor
            threshold = Math.max(minFloor - 1, threshold - lifeForceDecay);
        } else if (lifeForce < 20) {
            lifeForceDecay = 1;  // Critical: stay at floor minimum
            threshold = Math.max(minFloor, threshold - lifeForceDecay);
        } else if (lifeForce < 30) {
            lifeForceDecay = 1;  // Urgent: slightly more aggressive but respect floor
            threshold = Math.max(minFloor, threshold - lifeForceDecay);
        }

        // Build log message
        StringBuilder adjustments = new StringBuilder();
        if (earlyGameRelaxed) {
            adjustments.append("early game -2");
        }
        if (lifeForceDecay > 0) {
            if (adjustments.length() > 0) adjustments.append(", ");
            adjustments.append("life force ").append(lifeForce).append(" -").append(lifeForceDecay);
        }

        if (adjustments.length() > 0) {
            LOG.debug("   📊 Dynamic threshold ({}): {} ({})", domain, threshold, adjustments);
        } else {
            LOG.debug("   📊 Dynamic threshold ({}): {} (full threshold, turn {})", domain, threshold, turnNumber);
        }

        return threshold;
    }

    /**
     * Simple score/reason holder for getCardScore return value.
     */
    public static class PlanScore {
        public final float score;
        public final String reason;

        public PlanScore(float score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }
}
