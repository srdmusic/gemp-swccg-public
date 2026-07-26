package com.gempukku.swccgo.ai.models.chosenone.strategy;

import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.models.common.phase.DeployPlanRankingPolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployTacticalPolicy;
import com.gempukku.swccgo.ai.models.common.strategy.EndorOperationsTacticalPolicy;
import com.gempukku.swccgo.ai.models.chosenone.RandoConfig;
import com.gempukku.swccgo.ai.models.chosenone.RandoLogger;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.filters.Filters;

import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

// ═══════════════════════════════════════════════════════════
// ═══ SECTION: DEPLOY-1 (Sequencing & Budget) (reorg 2026-07-06) ═══
// Owns: deploy-phase planning layer: what to deploy this turn, hold-back, persona dead-card skip,
// V22 combo/turn planning, V22.3 pacing, V59 shuttle safety, V21 objective-critical holds. Hub: none.
// KIND mix (DEPLOY-1 overall): 11 ORDERING / 9 BANDED / 4 VETO.
// PARITY PAIR WARNING: V179 (DeployPhaseScript) <-> V67ai (deploy-from-reserve magnitudes in
// DeployEvaluator/ActionTextEvaluator) must stay in lockstep — re-check both when touching either.
// Absorbs (dead, commented below/nearby — revert path, do not delete): none.
// Cross-refs: DEPLOY-1 twin DeployPhaseScript (the DPS bucket walk), DEPLOY-2/DEPLOY-3 (siting/weapons),
// CombinedEvaluator DPS lines (co-owned with SVC-SAFETY), PULL-ENGINE. See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
// ═══════════════════════════════════════════════════════════
/**
 * Deploy Phase Planner - Creates holistic deployment plans for the entire phase.
 *
 * COMPREHENSIVE PORT from Python deploy_planner.py (7000+ lines).
 *
 * Key features:
 * 1. MULTIPLE PLAN GENERATION - generates ground, space, combined plans and picks best
 * 2. OPTIMAL COMBINATION FINDING - finds best card combinations within budget
 * 3. FORCE DRAIN GAP - tracks drain economy to prioritize stop-bleeding
 * 4. DYNAMIC THRESHOLDS - adjusts thresholds based on life force and game state
 * 5. PLAN SCORING - scores all plans and selects highest
 * 6. NEXT-TURN OPPORTUNITY - checks if holding would enable better plays
 *
 * Strategic priority order:
 * 1. DEPLOY LOCATIONS FIRST - opens new deployment options
 * 2. STOP BLEEDING - contest locations where opponent drains us
 * 3. REINFORCE LOSING - add power where we're being beaten
 * 4. ESTABLISH - take uncontested high-value locations
 * 5. BUILD UP - reinforce winning positions (but not overkill)
 */
public class DeployPhasePlanner {
    private static final Logger LOG = RandoLogger.getStrategyLogger();

    // Config constants
    private final int deployThreshold;
    private final int battleForceReserve;

    // Current plan (cached)
    private DeploymentPlan currentPlan;
    private int lastPlanTurn = -1;
    private boolean lastPlanHadActiveFlipGate = false;
    private boolean lastPlanHadFlipGateActorInHand = false;
    private String lastPlanPreFlipProgressFingerprint = "";

    // Board state reference for scoring
    private SwccgGame currentGame;
    private String currentPlayerId;
    private Side currentSide;

    // V21: Objective awareness for location prioritization
    private ObjectiveAnalyzer objectiveAnalyzer;

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
        lastPlanHadActiveFlipGate = false;
        lastPlanHadFlipGateActorInHand = false;
        lastPlanPreFlipProgressFingerprint = "";
        currentGame = null;
        currentPlayerId = null;
    }

    /**
     * V21: Set the objective analyzer for location prioritization.
     */
    public void setObjectiveAnalyzer(ObjectiveAnalyzer analyzer) {
        this.objectiveAnalyzer = analyzer;
    }

    /**
     * Get the current deployment plan, if any.
     */
    public DeploymentPlan getCurrentPlan() {
        return currentPlan;
    }

    // =========================================================================
    // MAIN ENTRY POINT - createPlan
    // =========================================================================

    /**
     * Create a deployment plan for this phase.
     *
     * This is the main entry point, ported from Python create_plan().
     * Generates MULTIPLE plans and picks the best one.
     */
    public DeploymentPlan createPlan(SwccgGame game, String playerId, Side side) {
        this.currentGame = game;
        this.currentPlayerId = playerId;
        this.currentSide = side;

        GameState gameState = game.getGameState();
        if (gameState == null) {
            return createHoldBackPlan("No game state available");
        }

        int currentTurn = gameState.getPlayersLatestTurnNumber(playerId);
        String opponentId = gameState.getOpponent(playerId);
        boolean hasActiveFlipGate = hasActiveFlipGateLocation(
                game, playerId);
        boolean hasFlipGateActorInHand = hasFlipGateActorInHand(
                game, playerId);
        String preFlipProgressFingerprint = objectiveAnalyzer == null
                ? ""
                : objectiveAnalyzer.getPreFlipProgressFingerprint(
                        game, playerId);
        if (preFlipProgressFingerprint == null) {
            preFlipProgressFingerprint = "";
        }

        // If we already have a plan for this turn, return it
        if (currentPlan != null && lastPlanTurn == currentTurn) {
            if (!lastPlanHadActiveFlipGate && hasActiveFlipGate) {
                LOG.warn("OBJECTIVE.DEPLOY.REPLAN_GATE_APPEARED: refreshing turn {} plan "
                        + "because the exact pre-flip actor gate entered play",
                    currentTurn);
            } else if (!lastPlanHadFlipGateActorInHand
                    && hasFlipGateActorInHand) {
                LOG.warn("OBJECTIVE.DEPLOY.REPLAN_ACTOR_ARRIVED: refreshing turn {} plan "
                        + "because the missing flip-gate actor entered hand",
                    currentTurn);
            } else if (!lastPlanPreFlipProgressFingerprint.equals(
                    preFlipProgressFingerprint)) {
                LOG.warn("OBJECTIVE.DEPLOY.REPLAN_PROGRESS_CHANGED: refreshing turn {} plan "
                        + "because structured pre-flip progress changed from {} to {}",
                    currentTurn, lastPlanPreFlipProgressFingerprint,
                    preFlipProgressFingerprint);
            } else {
                LOG.debug("📋 Returning cached plan for turn {} ({} instructions remaining)",
                    currentTurn, currentPlan.getInstructions().size());
                return currentPlan;
            }
        }

        LOG.info("📋 ═══════════════════════════════════════════════════════════════");
        LOG.info("📋 CREATING COMPREHENSIVE DEPLOYMENT PLAN (Turn {})", currentTurn);
        LOG.info("📋 ═══════════════════════════════════════════════════════════════");

        // === GET RESOURCES ===
        int forceAvailable = gameState.getForcePileSize(playerId);
        int lifeForce = gameState.getPlayerLifeForce(playerId);
        int opponentLifeForce = gameState.getPlayerLifeForce(opponentId);
        List<PhysicalCard> hand = gameState.getHand(playerId);

        LOG.info("📊 Resources: force={}, life={}, opponent_life={}, hand_size={}",
            forceAvailable, lifeForce, opponentLifeForce, hand.size());

        // === CATEGORIZE HAND ===
        List<CardInfo> allCards = new ArrayList<>();
        List<CardInfo> locations = new ArrayList<>();
        List<CardInfo> characters = new ArrayList<>();
        List<CardInfo> starships = new ArrayList<>();
        List<CardInfo> vehicles = new ArrayList<>();
        List<CardInfo> deadCards = new ArrayList<>();

        for (PhysicalCard card : hand) {
            if (card == null || card.getBlueprint() == null) continue;
            CardInfo info = new CardInfo(card);
            allCards.add(info);

            // === PERSONA CHECK: Skip dead cards (persona already deployed) ===
            if (AiCardHelper.isDeadCard(card, game, playerId)) {
                deadCards.add(info);
                LOG.info("☠️ DEAD CARD: {} - persona already on table, skipping deployment planning",
                    info.name);
                continue;  // Don't add to deployable categories
            }

            if (info.isLocation) locations.add(info);
            else if (info.isCharacter) characters.add(info);
            else if (info.isStarship) starships.add(info);
            else if (info.isVehicle) vehicles.add(info);
        }

        LOG.info("📋 Hand: {} locations, {} characters, {} starships, {} vehicles, {} dead cards",
            locations.size(), characters.size(), starships.size(), vehicles.size(), deadCards.size());
        if (!deadCards.isEmpty()) {
            LOG.info("☠️ Dead cards (persona in play): {}", deadCards.stream()
                .map(c -> c.name).collect(Collectors.joining(", ")));
        }

        // Log hand details
        logHandDetails(characters, starships, vehicles);

        // === ANALYZE BOARD ===
        List<AiBoardAnalyzer.LocationAnalysis> allLocations = AiBoardAnalyzer.analyzeAllLocations(
            game, playerId, opponentId, side);

        logBoardAnalysis(allLocations);

        // === CATEGORIZE LOCATIONS ===
        LocationCategories categories = categorizeLocations(allLocations, playerId);

        LOG.info("📊 Location categories: {} losing, {} winning, {} bleed, {} establish, {} attack, {} weak",
            categories.losingLocations.size(), categories.winningLocations.size(),
            categories.bleedLocations.size(), categories.establishTargets.size(),
            categories.attackTargets.size(), categories.weakPresenceLocations.size());

        // === CALCULATE FORCE DRAIN GAP ===
        DrainGapResult drainGap = calculateForceDrainGap(allLocations);
        LOG.info("💧 Drain economy: we drain {}, they drain {} = gap {:+d}",
            drainGap.ourDrain, drainGap.theirDrain, drainGap.drainGap);

        // === CALCULATE DYNAMIC THRESHOLDS ===
        int groundThreshold = getDynamicThreshold(allLocations, false, currentTurn, lifeForce);
        int spaceThreshold = getDynamicThreshold(allLocations, true, currentTurn, lifeForce);

        LOG.info("📊 Dynamic thresholds: ground={}, space={}", groundThreshold, spaceThreshold);

        // === GENERATE MULTIPLE PLANS ===
        List<ScoredPlan> allPlans = new ArrayList<>();
        // V22.3: Reserve Force for maintenance cards already in play
        // T2 COMMIT-1 (2026-07-06, audit force-economy-1): reserve the ENGINE's
        // card-specific maintain cost (MaintenanceFacts, parsed from game text;
        // e.g. Lando Scoundrel maintains for 1, deploys for 5). The old comment
        // "Maintenance cost in SWCCG = card's deploy cost" was the audit-refuted
        // claim — it OVER-reserved 2-5x and starved the deploy budget.
        // T2 MOVE #1 COMMIT-2 (2026-07-06): the planner has no DecisionContext, so it
        // calls the shared ForceReserveService directly (plan creation is already
        // once-per-turn). The service is the sole live scan: in-play-gated, on the
        // MaintenanceFacts basis, replacing the old inline loop's deploy-cost basis
        // and its incorrect `if (allCards != null)` guard, which tested the WRONG
        // variable (loop removed in cleanup batch 1.6, 2026-07-13).
        // V22.3 updated in place; effectiveForce math unchanged.
        int maintenanceReserve = 0;
        try {
            maintenanceReserve = com.gempukku.swccgo.ai.models.common.strategy
                .ForceReserveService.compute(game, gameState, playerId).maintenanceObligation;
            if (maintenanceReserve > 0) {
                LOG.warn("V22.3 MAINTENANCE: Reserving {} total Force for maintenance upkeep", maintenanceReserve);
            }
        } catch (Exception e) {
            LOG.debug("V22.3 MAINTENANCE: Error counting maintenance cards: {}", e.getMessage());
        }
        int effectiveForce = forceAvailable - battleForceReserve - maintenanceReserve;

        // Track location deploys (apply to all plans)
        List<CardInfo> locationDeploys = planLocationDeploys(locations, effectiveForce);
        int forceAfterLocations = effectiveForce;
        int objectiveFormationForceAfterLocations =
                forceAvailable - maintenanceReserve;
        for (CardInfo loc : locationDeploys) {
            forceAfterLocations -= loc.cost;
            objectiveFormationForceAfterLocations -= loc.cost;
        }
        DeploymentPlan endorSystemPlan = generateEopEndorSystemPlan(
            starships,
            characters.stream().filter(card -> card.isPilot)
                .collect(Collectors.toList()),
            allLocations,
            Math.max(0, objectiveFormationForceAfterLocations));

        // Generate ground plans
        List<ScoredPlan> groundPlans = generateGroundPlans(
            characters, vehicles, endorSystemPlan, categories, forceAfterLocations,
            Math.max(0, objectiveFormationForceAfterLocations), groundThreshold,
            allLocations, currentTurn, drainGap);
        allPlans.addAll(groundPlans);

        // Generate space plans
        List<ScoredPlan> spacePlans = generateSpacePlans(
            starships, characters, categories, forceAfterLocations, spaceThreshold,
            allLocations, currentTurn, drainGap, game, playerId, endorSystemPlan);
        allPlans.addAll(spacePlans);

        // Generate combined plans (best of ground + space within budget)
        List<ScoredPlan> combinedPlans = generateCombinedPlans(
            characters, starships, vehicles, categories, forceAfterLocations,
            groundThreshold, spaceThreshold, allLocations, currentTurn, drainGap);
        allPlans.addAll(combinedPlans);

        // === SELECT BEST PLAN ===
        DeploymentPlan bestPlan = selectBestPlan(allPlans, locationDeploys, currentTurn, lifeForce);

        // === EARLY GAME HOLD-BACK CHECK (at the END, like Python) ===
        if (currentTurn <= RandoConfig.DEPLOY_EARLY_GAME_TURNS && bestPlan != null) {
            float planScore = isObjectiveFlipGateFormationPlan(bestPlan)
                ? scoreObjectiveFlipGateFormationPlan(bestPlan, allLocations, currentTurn)
                : scorePlan(bestPlan, allLocations, currentTurn);
            if (!allPlans.isEmpty()) {
                planScore = allPlans.get(0).score;
            }
            if (planScore < RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD) {
                LOG.info("📋 EARLY GAME HOLD: plan score {} < threshold {} - holding back",
                    (int)planScore, RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD);
                bestPlan = createHoldBackPlan(String.format(
                    "Early game (turn %d) - plan score %.0f below threshold %d",
                    currentTurn, planScore, RandoConfig.DEPLOY_EARLY_GAME_THRESHOLD));
            }
        }

        if (bestPlan == null || bestPlan.getInstructions().isEmpty()) {
            bestPlan = createHoldBackPlan("No strategic deployment targets");
        }

        currentPlan = bestPlan;
        lastPlanTurn = currentTurn;
        lastPlanHadActiveFlipGate = hasActiveFlipGate;
        lastPlanHadFlipGateActorInHand = hasFlipGateActorInHand;
        lastPlanPreFlipProgressFingerprint = preFlipProgressFingerprint;
        logFinalPlan(bestPlan);

        return currentPlan;
    }

    private boolean hasActiveFlipGateLocation(
            SwccgGame game, String playerId) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || objectiveAnalyzer.isFlipped()
                || !objectiveAnalyzer.hasFlipGateActorRequirement()
                || game == null || game.getGameState() == null) {
            return false;
        }
        List<PhysicalCard> locations =
                game.getGameState().getLocationsInOrder();
        if (locations == null) return false;
        for (PhysicalCard location : locations) {
            if (location != null && objectiveAnalyzer.isFlipGateLocation(
                    game, playerId, location)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasFlipGateActorInHand(
            SwccgGame game, String playerId) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || objectiveAnalyzer.isFlipped()
                || !objectiveAnalyzer.hasFlipGateActorRequirement()
                || game == null || game.getGameState() == null) {
            return false;
        }
        List<PhysicalCard> hand = game.getGameState().getHand(playerId);
        if (hand == null) return false;
        for (PhysicalCard card : hand) {
            if (objectiveAnalyzer.matchesFlipGateActorRequirement(
                    game, playerId, card)) {
                return true;
            }
        }
        return false;
    }

    // =========================================================================
    // FORCE DRAIN GAP CALCULATION (Item #3)
    // =========================================================================

    /**
     * Calculate force drain economy.
     *
     * Ported from Python _calculate_force_drain_gap().
     */
    private DrainGapResult calculateForceDrainGap(List<AiBoardAnalyzer.LocationAnalysis> locations) {
        int theirDrain = 0;
        int ourDrain = 0;
        List<AiBoardAnalyzer.LocationAnalysis> bleedLocations = new ArrayList<>();

        for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
            // Opponent drains us: they have presence, we don't
            // They drain for OUR icons (ourForceIcons)
            if (loc.theirPower > 0 && loc.ourPower == 0 && loc.ourForceIcons > 0) {
                theirDrain += loc.ourForceIcons;

                // Add to bleed locations if enemy power is low enough to contest
                if (loc.theirPower <= RandoConfig.LOW_ENEMY_THRESHOLD) {
                    bleedLocations.add(loc);
                    String domain = loc.isSpace() ? "space" : "ground";
                    LOG.debug("   🩸 BLEED ({}, contestable): {} - they drain {} icons, enemy power {}",
                        domain, loc.location.getTitle(), loc.ourForceIcons, (int)loc.theirPower);
                }
            }

            // We drain opponent: we have presence, they don't
            if (loc.ourPower > 0 && loc.theirPower == 0 && loc.theirForceIcons > 0) {
                ourDrain += loc.theirForceIcons;
                LOG.debug("   💧 DRAIN: {} - we drain {} icons", loc.location.getTitle(), loc.theirForceIcons);
            }
        }

        return new DrainGapResult(theirDrain, ourDrain, ourDrain - theirDrain, bleedLocations);
    }

    // =========================================================================
    // DYNAMIC THRESHOLD CALCULATION (Item #4, #5)
    // =========================================================================

    /**
     * Calculate dynamic deploy threshold based on game state.
     *
     * Ported from Python _get_dynamic_threshold().
     */
    private int getDynamicThreshold(List<AiBoardAnalyzer.LocationAnalysis> locations,
                                     boolean isSpace, int turnNumber, int lifeForce) {
        int threshold = deployThreshold;
        String domain = isSpace ? "space" : "ground";

        // EARLY GAME RELAXATION: Before turn 4 with no contested locations
        boolean earlyGameRelaxed = false;
        if (turnNumber < 4) {
            boolean hasContested = false;
            for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                if (loc.ourPower <= 0 || loc.theirPower <= 0) continue;

                if (isSpace && loc.isSpace()) {
                    hasContested = true;
                    break;
                } else if (!isSpace && loc.isGround()) {
                    hasContested = true;
                    break;
                }
            }

            if (!hasContested) {
                // Check for react threats
                boolean hasReactThreat = false;
                for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                    boolean matchesDomain = isSpace ? loc.isSpace() : loc.isGround();
                    if (matchesDomain && loc.theirPower >= RandoConfig.REACT_THREAT_THRESHOLD) {
                        hasReactThreat = true;
                        break;
                    }
                }

                if (!hasReactThreat) {
                    threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER, threshold - 2);
                    earlyGameRelaxed = true;
                }
            }
        }

        // LIFE FORCE DECAY: Lower threshold when losing badly
        int lifeForceDecay = 0;
        if (lifeForce < 10) {
            lifeForceDecay = 2;
            threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER - 1, threshold - lifeForceDecay);
        } else if (lifeForce < 20) {
            lifeForceDecay = 1;
            threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER, threshold - lifeForceDecay);
        } else if (lifeForce < 30) {
            lifeForceDecay = 1;
            threshold = Math.max(RandoConfig.MIN_ESTABLISH_POWER, threshold - lifeForceDecay);
        }

        LOG.debug("   📊 Dynamic threshold ({}): {} (early={}, life_decay={})",
            domain, threshold, earlyGameRelaxed, lifeForceDecay);

        return threshold;
    }

    // =========================================================================
    // LOCATION CATEGORIZATION (Items #15, #16, #17)
    // =========================================================================

    /**
     * Categorize all locations into strategic groups.
     */
    private LocationCategories categorizeLocations(List<AiBoardAnalyzer.LocationAnalysis> locations,
                                                    String playerId) {
        LocationCategories cats = new LocationCategories();

        for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
            // Skip locations we can't deploy to (no icons)
            boolean hasOurIcons = loc.ourForceIcons > 0;
            boolean hasTheirIcons = loc.theirForceIcons > 0;

            if (loc.ourPower > 0 && loc.theirPower > 0) {
                // CONTESTED - both have presence
                if (loc.ourPower < loc.theirPower) {
                    cats.losingLocations.add(loc);
                } else if (loc.ourPower > loc.theirPower) {
                    cats.winningLocations.add(loc);
                    // Check if below reinforcement target
                    if (loc.ourPower < RandoConfig.REINFORCE_TARGET_POWER) {
                        cats.weakPresenceLocations.add(loc);
                    }
                }

                // Check if crushable (we have big advantage)
                if (loc.getPowerAdvantage() >= RandoConfig.BATTLE_FAVORABLE_THRESHOLD) {
                    cats.crushableLocations.add(loc);
                }
            } else if (loc.theirPower > 0 && loc.ourPower == 0 && hasOurIcons) {
                // BLEEDING - they have presence, we don't, but we have icons (they drain us)
                cats.bleedLocations.add(loc);
                if (loc.theirPower <= RandoConfig.LOW_ENEMY_THRESHOLD) {
                    cats.attackTargets.add(loc);  // Low enemy = attack target
                }
            } else if (loc.theirPower == 0 && loc.ourPower == 0 && hasTheirIcons) {
                // ESTABLISH - neither has presence but they have icons
                cats.establishTargets.add(loc);
            } else if (loc.ourPower > 0 && loc.theirPower == 0 && hasTheirIcons) {
                // DRAINING - we control and can drain them
                cats.drainingLocations.add(loc);
                // Check if we're below reinforcement target
                if (loc.ourPower < RandoConfig.REINFORCE_TARGET_POWER) {
                    cats.weakPresenceLocations.add(loc);
                }
            } else if (loc.ourPower > 0 && loc.theirPower == 0) {
                // CONTROLLED - we have presence, they don't
                if (loc.ourPower < RandoConfig.REINFORCE_TARGET_POWER) {
                    cats.weakPresenceLocations.add(loc);
                }
            }
        }

        return cats;
    }

    // =========================================================================
    // OPTIMAL COMBINATION FINDING (Item #13)
    // =========================================================================

    /**
     * Find optimal combination of cards within budget.
     *
     * Ported from Python _find_optimal_combination().
     */
    private OptimalCombination findOptimalCombination(List<CardInfo> cards, int budget,
                                                       int powerGoal, boolean mustExceed) {
        if (cards.isEmpty() || budget <= 0) {
            return OptimalCombination.empty();
        }

        // Filter to affordable cards
        List<CardInfo> affordable = cards.stream()
            .filter(c -> c.cost <= budget)
            .collect(Collectors.toList());

        if (affordable.isEmpty()) {
            return OptimalCombination.empty();
        }

        // For small card counts, try all combinations
        if (affordable.size() <= 8) {
            return findOptimalBruteForce(affordable, budget, powerGoal, mustExceed);
        }

        // For larger hands, use greedy approach
        return findOptimalGreedy(affordable, budget, powerGoal, mustExceed);
    }

    /**
     * Brute force optimal combination (for small hand sizes).
     */
    private OptimalCombination findOptimalBruteForce(List<CardInfo> cards, int budget,
                                                      int powerGoal, boolean mustExceed) {
        List<PhysicalCard> bestCombo = new ArrayList<>();
        int bestPower = 0;
        int bestCost = Integer.MAX_VALUE;
        int bestAbility = 0;
        boolean bestHasAbility = false;
        boolean bestAchievesGoal = false;

        // Try all subset sizes
        for (int size = 1; size <= cards.size(); size++) {
            for (List<CardInfo> combo : combinations(cards, size)) {
                int totalCost = combo.stream().mapToInt(c -> c.cost).sum();
                if (totalCost > budget) continue;

                int totalPower = combo.stream().mapToInt(c -> c.power).sum();
                int totalAbility = combo.stream().mapToInt(c -> c.ability).sum();
                boolean hasAbility = totalAbility >= RandoConfig.ABILITY_THRESHOLD;

                // Ability compensation for battles
                int effectiveGoal = powerGoal;
                if (!hasAbility && mustExceed) {
                    effectiveGoal = powerGoal + RandoConfig.ABILITY_POWER_COMPENSATION;
                }

                boolean achievesGoal = mustExceed ?
                    totalPower > effectiveGoal : totalPower >= effectiveGoal;

                // Check if this is better
                boolean isBetter = false;
                if (achievesGoal && !bestAchievesGoal) {
                    isBetter = true;
                } else if (achievesGoal == bestAchievesGoal) {
                    if (hasAbility && !bestHasAbility) {
                        isBetter = true;
                    } else if (hasAbility == bestHasAbility) {
                        if (achievesGoal) {
                            if (mustExceed && totalPower > bestPower) isBetter = true;
                            else if (!mustExceed && totalCost < bestCost) isBetter = true;
                        } else if (totalPower > bestPower) {
                            isBetter = true;
                        }
                    }
                }

                if (isBetter) {
                    bestCombo = combo.stream().map(c -> c.card).collect(Collectors.toList());
                    bestPower = totalPower;
                    bestCost = totalCost;
                    bestAbility = totalAbility;
                    bestHasAbility = hasAbility;
                    bestAchievesGoal = achievesGoal;
                }
            }
        }

        return new OptimalCombination(bestCombo, bestPower,
            bestCost == Integer.MAX_VALUE ? 0 : bestCost,
            bestAbility, bestHasAbility, bestAchievesGoal);
    }

    /**
     * Greedy optimal combination (for larger hand sizes).
     */
    private OptimalCombination findOptimalGreedy(List<CardInfo> cards, int budget,
                                                  int powerGoal, boolean mustExceed) {
        // Sort by efficiency (power/cost) with ability bonus
        List<CardInfo> sorted = new ArrayList<>(cards);
        sorted.sort((a, b) -> {
            float scoreA = a.getValueRatio() + (a.ability >= 3 ? a.ability * 0.5f : 0);
            float scoreB = b.getValueRatio() + (b.ability >= 3 ? b.ability * 0.5f : 0);
            return Float.compare(scoreB, scoreA);
        });

        List<PhysicalCard> selected = new ArrayList<>();
        int totalPower = 0;
        int totalCost = 0;
        int totalAbility = 0;

        for (CardInfo card : sorted) {
            if (totalCost + card.cost <= budget) {
                selected.add(card.card);
                totalPower += card.power;
                totalCost += card.cost;
                totalAbility += card.ability;

                // Check if we've met the goal with ability
                boolean hasAbility = totalAbility >= RandoConfig.ABILITY_THRESHOLD;
                int effectiveGoal = hasAbility ? powerGoal :
                    powerGoal + (mustExceed ? RandoConfig.ABILITY_POWER_COMPENSATION : 0);

                if (mustExceed ? totalPower > effectiveGoal : totalPower >= effectiveGoal) {
                    break;  // Goal met
                }
            }
        }

        boolean hasAbility = totalAbility >= RandoConfig.ABILITY_THRESHOLD;
        int effectiveGoal = hasAbility ? powerGoal :
            powerGoal + (mustExceed ? RandoConfig.ABILITY_POWER_COMPENSATION : 0);
        boolean achievesGoal = mustExceed ? totalPower > effectiveGoal : totalPower >= effectiveGoal;

        return new OptimalCombination(selected, totalPower, totalCost,
            totalAbility, hasAbility, achievesGoal);
    }

    // =========================================================================
    // PLAN GENERATION (Items #1, #9)
    // =========================================================================

    /**
     * Plan location deploys (always prioritized).
     * V22: Sort objective-relevant locations first.
     * V22: Throttle location deploys if force gen >= 15 AND reserve < 7.
     */
    private List<CardInfo> planLocationDeploys(List<CardInfo> locations, int forceAvailable) {
        List<CardInfo> deploys = new ArrayList<>();
        if (locations.isEmpty()) return deploys;

        // V22: Throttle check - skip ALL location deploys if already generating
        // 15+ force AND reserve deck is dangerously low (< 7 cards)
        if (currentGame != null && currentPlayerId != null) {
            try {
                GameState gs = currentGame.getGameState();
                int reserveSize = gs.getReserveDeckSize(currentPlayerId);
                int forceGen = gs.getForcePileSize(currentPlayerId)
                    + gs.getReserveDeckSize(currentPlayerId); // rough proxy for generation capacity
                // More accurate: count our force icons on locations we control
                int ourForceIcons = 0;
                String opponentId = gs.getOpponent(currentPlayerId);
                List<AiBoardAnalyzer.LocationAnalysis> locs = AiBoardAnalyzer.analyzeAllLocations(
                    currentGame, currentPlayerId, opponentId, currentSide);
                for (AiBoardAnalyzer.LocationAnalysis la : locs) {
                    ourForceIcons += la.ourForceIcons;
                }
                if (ourForceIcons >= 15 && reserveSize < 7) {
                    LOG.warn("V22 LOCATION THROTTLE: Force icons={}, reserve={} - skipping location deploys to conserve reserve",
                        ourForceIcons, reserveSize);
                    return deploys;
                }
            } catch (Exception e) {
                LOG.debug("V22 LOCATION THROTTLE: Error checking force/reserve: {}", e.getMessage());
            }
        }

        // V22: Sort locations - objective-relevant first, then by cost (cheaper first)
        List<CardInfo> sorted = new ArrayList<>(locations);
        sorted.sort((a, b) -> {
            boolean aRelevant = false;
            boolean bRelevant = false;
            if (objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()) {
                aRelevant = objectiveAnalyzer.isObjectiveRelevantLocation(a.name);
                bRelevant = objectiveAnalyzer.isObjectiveRelevantLocation(b.name);
            }
            if (aRelevant && !bRelevant) return -1;
            if (!aRelevant && bRelevant) return 1;
            return Integer.compare(a.cost, b.cost);
        });

        int remaining = forceAvailable;
        for (CardInfo loc : sorted) {
            if (loc.cost <= remaining) {
                deploys.add(loc);
                remaining -= loc.cost;
                boolean objRelevant = objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()
                    && objectiveAnalyzer.isObjectiveRelevantLocation(loc.name);
                LOG.info("   📍 Location deploy: {} (cost {}){}", loc.name, loc.cost,
                    objRelevant ? " ⭐ OBJECTIVE-RELEVANT" : "");
            }
        }

        return deploys;
    }

    /**
     * Generate ground deployment plans.
     */
    private List<ScoredPlan> generateGroundPlans(List<CardInfo> characters, List<CardInfo> vehicles,
                                                  DeploymentPlan fundedEndorPackage,
                                                  LocationCategories categories, int forceAvailable,
                                                  int objectiveFormationForceAvailable,
                                                  int threshold, List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                  int turn, DrainGapResult drainGap) {
        List<ScoredPlan> plans = new ArrayList<>();

        // Get ground cards
        List<CardInfo> groundCards = new ArrayList<>(characters);
        groundCards.addAll(vehicles);

        if (groundCards.isEmpty()) {
            return plans;
        }

        // V297: A control-gated actor objective needs a formation, not a sacrificial
        // one-body score. Build the exact actor-plus-buddy plan before generic siting.
        DeploymentPlan flipGateFormation = generateFlipGateFormationPlan(
            characters, allLocations, objectiveFormationForceAvailable);
        if (flipGateFormation != null && !flipGateFormation.getInstructions().isEmpty()) {
            float score = scoreObjectiveFlipGateFormationPlan(
                flipGateFormation, allLocations, turn);
            plans.add(new ScoredPlan(
                flipGateFormation, score, "ground_objective_flip_gate_formation"));
        }

        // Plan 1: Stop bleeding (highest priority if losing drain war)
        if (drainGap.isLosing() && !categories.bleedLocations.isEmpty()) {
            DeploymentPlan bleedPlan = generateStopBleedingPlan(
                groundCards, categories.bleedLocations.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, threshold, "ground");
            if (!bleedPlan.getInstructions().isEmpty()) {
                float score = scorePlan(bleedPlan, allLocations, turn);
                plans.add(new ScoredPlan(bleedPlan, score, "ground_bleed"));
            }
        }

        // Plan 2: Reinforce losing positions
        if (!categories.losingLocations.isEmpty()) {
            DeploymentPlan reinforcePlan = generateReinforcePlan(
                groundCards, categories.losingLocations.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, "ground");
            if (!reinforcePlan.getInstructions().isEmpty()) {
                float score = scorePlan(reinforcePlan, allLocations, turn);
                score += endorPostFlipPlanAdjustment(reinforcePlan, true, false);
                plans.add(new ScoredPlan(reinforcePlan, score, "ground_reinforce"));
            }
        }

        // Plan 3: Establish at uncontested locations
        if (!categories.establishTargets.isEmpty()) {
            DeploymentPlan establishPlan = generateEstablishPlan(
                groundCards,
                categories.establishTargets.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, threshold, "ground");
            if (!establishPlan.getInstructions().isEmpty()) {
                if (EndorOperationsTacticalPolicy
                        .shouldSuppressEmptyEndorGroundEstablish(
                                occupiesEndorBattlegroundSite(allLocations)
                                    && fundedEndorPackage != null
                                    && !fundedEndorPackage.getInstructions().isEmpty(),
                                targetsEmptyEndorSite(establishPlan))) {
                    LOG.warn("EOP SPACE FIRST: suppressing another empty Endor site while Endor system is uncontrolled");
                } else {
                    float score = scorePlan(establishPlan, allLocations, turn);
                    score += endorPostFlipPlanAdjustment(establishPlan, false, true);
                    plans.add(new ScoredPlan(establishPlan, score, "ground_establish"));
                }
            }
        }

        // Plan 4: Attack enemy positions
        if (!categories.attackTargets.isEmpty()) {
            DeploymentPlan attackPlan = generateAttackPlan(
                groundCards, categories.attackTargets.stream()
                    .filter(AiBoardAnalyzer.LocationAnalysis::isGround)
                    .collect(Collectors.toList()),
                forceAvailable, "ground");
            if (!attackPlan.getInstructions().isEmpty()) {
                float score = scorePlan(attackPlan, allLocations, turn);
                score += endorPostFlipPlanAdjustment(attackPlan, true, false);
                plans.add(new ScoredPlan(attackPlan, score, "ground_attack"));
            }
        }

        return plans;
    }

    private float endorPostFlipPlanAdjustment(
            DeploymentPlan plan,
            boolean reinforceOrAttack,
            boolean establishesEmptySite) {
        boolean endorOperations = objectiveAnalyzer != null
            && objectiveAnalyzer.isAnalyzed()
            && EndorOperationsTacticalPolicy.isEndorOperations(
                objectiveAnalyzer.getObjectiveBlueprintId(),
                objectiveAnalyzer.getObjectiveTitle());
        boolean targetsEndorSite = plan != null
            && plan.getInstructions().stream()
                .map(DeploymentInstruction::getTargetLocationName)
                .filter(Objects::nonNull)
                .anyMatch(title -> title.toLowerCase(Locale.ROOT)
                    .startsWith("endor:"));
        float adjustment =
            EndorOperationsTacticalPolicy.postFlipPlanAdjustment(
                endorOperations,
                objectiveAnalyzer != null && objectiveAnalyzer.isFlipped(),
                reinforceOrAttack,
                targetsEndorSite,
                establishesEmptySite);
        if (adjustment != 0.0f) {
            LOG.warn("EOP POST-FLIP PLAN: {} receives {} for {}",
                plan.getStrategy(), adjustment, plan.getReason());
        }
        return adjustment;
    }

    /**
     * V297: create a defensible objective-gate formation. Pre-flip actor gates
     * require the named actor plus an existing or funded buddy. Post-flip, keep
     * two characters at the gate so a single battle cannot immediately undo it.
     */
    private DeploymentPlan generateFlipGateFormationPlan(
            List<CardInfo> characters,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int forceAvailable) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || !objectiveAnalyzer.hasFlipGateActorRequirement()
                || currentGame == null || currentPlayerId == null
                || forceAvailable <= 0 || characters.isEmpty()) {
            return null;
        }

        String gateTitle = objectiveAnalyzer.getFlipCriticalControlSite();
        if (gateTitle == null) return null;

        AiBoardAnalyzer.LocationAnalysis gate = allLocations.stream()
            .filter(loc -> loc != null && loc.isGround()
                && loc.location != null && loc.location.getTitle() != null
                && gateTitle.equalsIgnoreCase(loc.location.getTitle()))
            .findFirst().orElse(null);
        if (gate == null) return null;

        List<CardInfo> deployable = filterDeployableCards(characters, gate.location);
        if (deployable.isEmpty()) return null;

        int friendlyCharacters = com.gempukku.swccgo.ai.models.common.strategy
            .FormationSafety.countFriendlyNonUndercoverCharacters(
                currentGame.getGameState().getCardsAtLocation(gate.location),
                currentPlayerId);
        boolean actorRequired = !objectiveAnalyzer.isFlipped();
        boolean actorPresent = objectiveAnalyzer.hasFlipGateActorAtLocation(
            currentGame, currentPlayerId, gate.location);
        boolean controlsGate = gate.weControl();
        List<CardInfo> selected = new ArrayList<>(2);

        if (actorRequired && !actorPresent) {
            List<CardInfo> actorCandidates = deployable.stream()
                .filter(card -> objectiveAnalyzer.matchesFlipGateActorRequirement(
                    currentGame, currentPlayerId, card.card, gate.location))
                .collect(Collectors.toList());
            if (actorCandidates.isEmpty()) {
                CardInfo enabler = deployable.stream()
                    .filter(card -> {
                        Integer futureActorCost =
                            objectiveAnalyzer.getFlipGateActorEnablerFutureDeployCost(
                                currentGame, currentPlayerId, card.card);
                        return futureActorCost != null
                            && card.cost + futureActorCost <= forceAvailable;
                    })
                    .max(Comparator
                        .comparingInt((CardInfo card) -> card.power + card.ability)
                        .thenComparingDouble(CardInfo::getValueRatio)
                        .thenComparingInt(card -> -card.cost))
                    .orElse(null);
                if (enabler == null) return null;
                selected.add(enabler);
                LOG.warn("OBJECTIVE.DEPLOY.ENABLER_PLAN: {} -> {} with future actor funded",
                    enabler.name, gateTitle);
            } else if (friendlyCharacters == 0) {
                selected.addAll(selectBestFormationPair(
                    actorCandidates, deployable, forceAvailable));
                if (selected.size() != 2) return null;
            } else {
                CardInfo actor = selectBestFormationCard(
                    actorCandidates, forceAvailable);
                if (actor == null) return null;
                selected.add(actor);
            }
        } else {
            int bodiesNeeded = Math.max(0, 2 - friendlyCharacters);
            if (bodiesNeeded == 0 && !controlsGate) bodiesNeeded = 1;
            if (bodiesNeeded == 0) return null;
            if (bodiesNeeded == 1) {
                CardInfo buddy = selectBestFormationCard(
                    deployable, forceAvailable);
                if (buddy == null) return null;
                selected.add(buddy);
            } else {
                selected.addAll(selectBestFormationPair(
                    deployable, deployable, forceAvailable));
                if (selected.size() != 2) return null;
            }
        }

        int selectedPower = selected.stream().mapToInt(card -> card.power).sum();
        int selectedAbility = selected.stream().mapToInt(card -> card.ability).sum();
        float projectedAbility = gate.ourAbility + selectedAbility;
        boolean opponentPresent = gate.theirCardCount > 0
            || gate.theirPower > 0.0f || gate.theirAbility > 0.0f;
        if (opponentPresent) {
            CardInfo lead = selected.get(0);
            int formationBodies = selected.size() + friendlyCharacters;
            DeployTacticalPolicy.ContactAssessment contact =
                DeployTacticalPolicy.assessV171V172Contact(
                    new DeployTacticalPolicy.ContactFacts(
                        "objective-formation-plan", gateTitle,
                        true, true, formationBodies, gate.ourPower,
                        lead.power, selectedPower - lead.power,
                        Math.max(0, formationBodies - 1), 0.0f,
                        gate.theirPower,
                        selected.stream().mapToInt(card -> card.power).max().orElse(0),
                        0));
            if (!contact.viable() || projectedAbility < 4.0f) {
                LOG.warn("OBJECTIVE FORMATION REJECTED: {} at {} projects power {}/ability {} "
                        + "into power {}/ability {} (V171/V172 contact={}, battle destiny={})",
                    selected.stream().map(card -> card.name).collect(Collectors.joining(" + ")),
                    gateTitle, contact.projectedPower(), projectedAbility,
                    gate.theirPower, gate.theirAbility,
                    contact.viable(), projectedAbility >= 4.0f);
                return null;
            }
        }

        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "V297 objective flip-gate formation at " + gateTitle);
        int priority = 1;
        for (CardInfo card : selected) {
            addCardToPlan(plan, card.card, gate, priority++,
                "V297 secure objective gate with actor-and-buddy formation");
            plan.getInstructions().get(plan.getInstructions().size() - 1)
                .setAbilityContribution(card.ability);
        }
        LOG.warn("V297 OBJECTIVE FORMATION: {} -> {} with {} funded character(s), {} already present",
            selected.stream().map(card -> card.name).collect(Collectors.joining(" + ")),
            gateTitle, selected.size(), friendlyCharacters);
        return plan;
    }

    private List<CardInfo> selectBestFormationPair(
            List<CardInfo> firstCandidates, List<CardInfo> allCandidates,
            int budget) {
        CardInfo bestFirst = null;
        CardInfo bestSecond = null;
        int bestScore = Integer.MIN_VALUE;
        for (CardInfo first : firstCandidates) {
            for (CardInfo second : allCandidates) {
                if (first.card == second.card || first.cost + second.cost > budget) continue;
                int combinedAbility = first.ability + second.ability;
                int combinedPower = first.power + second.power;
                int score = (combinedAbility >= 4 ? 100000 : 0)
                    + combinedPower * 100 + combinedAbility * 25
                    - first.cost - second.cost;
                if (score > bestScore) {
                    bestScore = score;
                    bestFirst = first;
                    bestSecond = second;
                }
            }
        }
        return bestFirst == null
            ? Collections.emptyList() : List.of(bestFirst, bestSecond);
    }

    private CardInfo selectBestFormationCard(
            List<CardInfo> candidates, int budget) {
        return candidates.stream()
            .filter(card -> card.cost <= budget)
            .max(Comparator
                .comparingInt((CardInfo card) -> card.ability >= 4 ? 1 : 0)
                .thenComparingInt(card -> card.power + card.ability)
                .thenComparingDouble(CardInfo::getValueRatio)
                .thenComparingInt(card -> -card.cost))
            .orElse(null);
    }

    /**
     * Generate space deployment plans.
     */
    private List<ScoredPlan> generateSpacePlans(List<CardInfo> starships, List<CardInfo> characters,
                                                 LocationCategories categories, int forceAvailable,
                                                 int threshold, List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                 int turn, DrainGapResult drainGap,
                                                 SwccgGame game, String playerId,
                                                 DeploymentPlan endorSystemPlan) {
        List<ScoredPlan> plans = new ArrayList<>();

        if (starships.isEmpty()) {
            return plans;
        }

        // Get pilots for ship combos
        List<CardInfo> pilots = characters.stream()
            .filter(c -> c.isPilot)
            .collect(Collectors.toList());

        if (endorSystemPlan != null
                && !endorSystemPlan.getInstructions().isEmpty()) {
            float score = scorePlan(endorSystemPlan, allLocations, turn)
                    + EndorOperationsTacticalPolicy.POST_FLIP_REINFORCE_BONUS;
            plans.add(new ScoredPlan(
                    endorSystemPlan, score, "objective_endor_system"));
            LOG.warn("EOP SPACE FIRST: added funded Endor system package at +{}",
                    EndorOperationsTacticalPolicy.POST_FLIP_REINFORCE_BONUS);
        }

        // Plan 1: Stop bleeding in space
        List<AiBoardAnalyzer.LocationAnalysis> spaceBleed = categories.bleedLocations.stream()
            .filter(AiBoardAnalyzer.LocationAnalysis::isSpace)
            .collect(Collectors.toList());

        if (drainGap.isLosing() && !spaceBleed.isEmpty()) {
            DeploymentPlan bleedPlan = generateStopBleedingPlan(
                new ArrayList<>(starships), spaceBleed, forceAvailable, threshold, "space");
            if (!bleedPlan.getInstructions().isEmpty()) {
                float score = scorePlan(bleedPlan, allLocations, turn);
                plans.add(new ScoredPlan(bleedPlan, score, "space_bleed"));
            }
        }

        // Plan 2: Reinforce losing space
        List<AiBoardAnalyzer.LocationAnalysis> spaceLosing = categories.losingLocations.stream()
            .filter(AiBoardAnalyzer.LocationAnalysis::isSpace)
            .collect(Collectors.toList());

        if (!spaceLosing.isEmpty()) {
            DeploymentPlan reinforcePlan = generateReinforcePlan(
                new ArrayList<>(starships), spaceLosing, forceAvailable, "space");
            if (!reinforcePlan.getInstructions().isEmpty()) {
                float score = scorePlan(reinforcePlan, allLocations, turn);
                plans.add(new ScoredPlan(reinforcePlan, score, "space_reinforce"));
            }
        }

        // Plan 3: Establish in space
        List<AiBoardAnalyzer.LocationAnalysis> spaceEstablish = categories.establishTargets.stream()
            .filter(AiBoardAnalyzer.LocationAnalysis::isSpace)
            .collect(Collectors.toList());

        if (!spaceEstablish.isEmpty()) {
            DeploymentPlan establishPlan = generateEstablishPlan(
                new ArrayList<>(starships), spaceEstablish, forceAvailable, threshold, "space");
            if (!establishPlan.getInstructions().isEmpty()) {
                float score = scorePlan(establishPlan, allLocations, turn);
                plans.add(new ScoredPlan(establishPlan, score, "space_establish"));
            }
        }

        // Plan 4: RE-PILOT unpiloted ships in play (Item #6)
        DeploymentPlan repilotPlan = generateRepilotPlan(pilots, game, playerId, forceAvailable);
        if (!repilotPlan.getInstructions().isEmpty()) {
            float score = scorePlan(repilotPlan, allLocations, turn);
            plans.add(new ScoredPlan(repilotPlan, score, "space_repilot"));
        }

        // V22: OBJECTIVE CAPITAL SHIP PRIORITY
        // If the objective wants Bespin control, prioritize deploying a capital ship there.
        if (objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()) {
            Set<String> fragments = objectiveAnalyzer.getFlipConditionLocationFragments();
            boolean objectiveWantsBespin = fragments.contains("bespin") || fragments.contains("cloud city");
            if (objectiveWantsBespin) {
                DeploymentPlan executorPlan = generateObjectiveCapitalPlan(
                    starships, characters, allLocations, forceAvailable, game, playerId);
                if (!executorPlan.getInstructions().isEmpty()) {
                    float score = scoreObjectiveCapitalPlan(executorPlan, allLocations, turn);
                    plans.add(new ScoredPlan(executorPlan, score, "objective_capital_bespin"));
                    LOG.warn("📋 V22: Added objective capital ship plan for Bespin (score boost +200)");
                }
            }
        }

        return plans;
    }

    private DeploymentPlan generateEopEndorSystemPlan(
            List<CardInfo> starships,
            List<CardInfo> pilots,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations,
            int forceAvailable) {
        DeploymentPlan plan = new DeploymentPlan(
                DeployStrategy.ESTABLISH,
                "EOP: occupy Endor system before further ground expansion");
        if (!endorSystemOccupationPending(allLocations)
                || currentGame == null
                || currentGame.getGameState() == null
                || currentGame.getModifiersQuerying() == null
                || currentPlayerId == null) {
            return plan;
        }
        AiBoardAnalyzer.LocationAnalysis endor = allLocations.stream()
                .filter(loc -> loc != null && loc.isSpace()
                        && loc.location != null
                        && "endor".equalsIgnoreCase(loc.location.getTitle())
                        && loc.theirPower <= 0)
                .findFirst().orElse(null);
        if (endor == null) {
            return plan;
        }
        CardInfo reservedBunkerGarrison =
                findReservedEopBunkerGarrison(pilots, allLocations);
        EndorSystemPackage best = null;
        for (CardInfo ship : starships) {
            Integer soloCost = exactDeployCostAt(ship.card, endor.location);
            float shipAbility = exactAbility(ship.card, true);
            if (soloCost != null
                    && soloCost <= forceAvailable
                    && shipAbility > 0.0f
                    && canDeployDirectly(ship.card, endor.location)) {
                EndorSystemPackage candidate = new EndorSystemPackage(
                        ship, null, soloCost, shipAbility,
                        ship.power);
                if (isBetterEndorSystemPackage(candidate, best)) {
                    best = candidate;
                }
            }
            for (CardInfo pilot : pilots) {
                if (reservedBunkerGarrison != null
                        && isSamePhysicalCard(
                                pilot.card, reservedBunkerGarrison.card)) {
                    continue;
                }
                float pilotAbility = exactAbility(pilot.card, false);
                if (pilotAbility <= 0.0f
                        || !canDeployShipAndPilotTogether(
                                ship.card, pilot.card, endor.location)) {
                    continue;
                }
                Integer pairCost = exactSimultaneousDeployCost(
                        ship.card, pilot.card, endor.location);
                if (pairCost == null || pairCost > forceAvailable) {
                    continue;
                }
                EndorSystemPackage candidate = new EndorSystemPackage(
                        ship, pilot, pairCost, pilotAbility,
                        ship.power + pilot.power);
                if (isBetterEndorSystemPackage(candidate, best)) {
                    best = candidate;
                }
            }
        }
        if (best == null) {
            return plan;
        }

        addCardToPlan(plan, best.ship.card, endor, 1,
                "EOP: deploy a legal ship package to occupy Endor system");
        DeploymentInstruction shipInstruction =
                plan.getInstructions().get(plan.getInstructions().size() - 1);
        shipInstruction.setDeployCost(best.cost);
        shipInstruction.setAbilityContribution(
                (int) Math.ceil(best.combinedAbility));
        if (best.pilot != null) {
            shipInstruction.setVerifiedCrewPackage(true);
            addCardToPlan(plan, best.pilot.card, endor, 2,
                    "EOP: deploy the legal pilot aboard the Endor system ship");
            DeploymentInstruction pilotInstruction =
                    plan.getInstructions().get(plan.getInstructions().size() - 1);
            pilotInstruction.setDeployCost(0);
            pilotInstruction.setAboardShipName(best.ship.name);
            pilotInstruction.setAboardShipBlueprintId(best.ship.blueprintId);
            pilotInstruction.setAboardShipCardId(
                    String.valueOf(best.ship.card.getCardId()));
        }
        return plan;
    }

    private CardInfo findReservedEopBunkerGarrison(
            List<CardInfo> pilots,
            List<AiBoardAnalyzer.LocationAnalysis> allLocations) {
        AiBoardAnalyzer.LocationAnalysis bunker = allLocations.stream()
                .filter(loc -> loc != null && loc.location != null
                        && "endor: bunker".equalsIgnoreCase(
                                loc.location.getTitle()))
                .findFirst().orElse(null);
        if (bunker == null) {
            return null;
        }
        List<PhysicalCard> cardsAtBunker =
                currentGame.getGameState().getCardsAtLocation(bunker.location);
        if (cardsAtBunker != null && cardsAtBunker.stream()
                .anyMatch(card -> currentPlayerId.equals(card.getOwner())
                        && isImperialAdmiral(card))) {
            return null;
        }
        CardInfo best = null;
        int bestCost = Integer.MAX_VALUE;
        for (CardInfo candidate : pilots) {
            Integer cost = exactDeployCostAt(
                    candidate.card, bunker.location);
            if (cost == null || cost > 2
                    || !isImperialAdmiral(candidate.card)
                    || !canDeployDirectly(
                            candidate.card, bunker.location)) {
                continue;
            }
            int permanentId = candidate.card.getPermanentCardId();
            int bestPermanentId = best == null
                    ? Integer.MAX_VALUE
                    : best.card.getPermanentCardId();
            if (cost < bestCost
                    || cost == bestCost
                        && permanentId < bestPermanentId) {
                best = candidate;
                bestCost = cost;
            }
        }
        return best;
    }

    private boolean isSamePhysicalCard(
            PhysicalCard first, PhysicalCard second) {
        return first != null && second != null
                && first.getPermanentCardId()
                        == second.getPermanentCardId()
                && first.getCardId() == second.getCardId();
    }

    private boolean isImperialAdmiral(PhysicalCard card) {
        try {
            return Filters.and(Filters.Imperial, Filters.admiral)
                    .accepts(currentGame.getGameState(),
                            currentGame.getModifiersQuerying(), card);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canDeployDirectly(
            PhysicalCard card, PhysicalCard location) {
        try {
            return Filters.deployableToLocation(
                    card, Filters.sameCardId(location), true, 0.0f)
                    .accepts(currentGame.getGameState(),
                            currentGame.getModifiersQuerying(), card);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canDeployShipAndPilotTogether(
            PhysicalCard ship,
            PhysicalCard pilot,
            PhysicalCard location) {
        try {
            GameState gameState = currentGame.getGameState();
            if (!ship.getBlueprint()
                    .getValidPilotFilter(
                            currentPlayerId, currentGame, ship, true)
                    .accepts(gameState,
                            currentGame.getModifiersQuerying(), pilot)
                    || !Filters.hasAvailablePilotCapacity(pilot)
                    .accepts(gameState,
                            currentGame.getModifiersQuerying(), ship)) {
                return false;
            }
            return Filters.deployableToLocationSimultaneouslyWith(
                    ship, pilot, true, 0.0f,
                    Filters.sameCardId(location), true, 0.0f)
                    .accepts(gameState,
                            currentGame.getModifiersQuerying(), ship);
        } catch (Exception e) {
            return false;
        }
    }

    private Integer exactDeployCostAt(
            PhysicalCard card, PhysicalCard location) {
        try {
            float cost = currentGame.getModifiersQuerying().getDeployCost(
                    currentGame.getGameState(), card, card, location,
                    false, null, false, 0.0f, null, true);
            return (int) Math.ceil(Math.max(0.0f, cost));
        } catch (Exception e) {
            return null;
        }
    }

    private Integer exactSimultaneousDeployCost(
            PhysicalCard ship,
            PhysicalCard pilot,
            PhysicalCard location) {
        try {
            float cost = currentGame.getModifiersQuerying()
                    .getSimultaneousDeployCost(
                            currentGame.getGameState(),
                            ship, ship, false, 0.0f,
                            pilot, false, 0.0f,
                            location, null, true);
            return (int) Math.ceil(Math.max(0.0f, cost));
        } catch (Exception e) {
            return null;
        }
    }

    private float exactAbility(
            PhysicalCard card, boolean includePermanentPilots) {
        try {
            return currentGame.getModifiersQuerying().getAbility(
                    currentGame.getGameState(), card,
                    includePermanentPilots);
        } catch (Exception e) {
            return 0.0f;
        }
    }

    private boolean isBetterEndorSystemPackage(
            EndorSystemPackage candidate,
            EndorSystemPackage current) {
        if (current == null || candidate.cost != current.cost) {
            return current == null || candidate.cost < current.cost;
        }
        int candidateCards = candidate.pilot == null ? 1 : 2;
        int currentCards = current.pilot == null ? 1 : 2;
        if (candidateCards != currentCards) {
            return candidateCards < currentCards;
        }
        int abilityOrder = Float.compare(
                candidate.combinedAbility, current.combinedAbility);
        if (abilityOrder != 0) {
            return abilityOrder > 0;
        }
        if (candidate.combinedPower != current.combinedPower) {
            return candidate.combinedPower > current.combinedPower;
        }
        int shipOrder = Integer.compare(
                candidate.ship.card.getPermanentCardId(),
                current.ship.card.getPermanentCardId());
        if (shipOrder != 0) {
            return shipOrder < 0;
        }
        int candidatePilotId = candidate.pilot == null
                ? Integer.MAX_VALUE
                : candidate.pilot.card.getPermanentCardId();
        int currentPilotId = current.pilot == null
                ? Integer.MAX_VALUE
                : current.pilot.card.getPermanentCardId();
        return candidatePilotId < currentPilotId;
    }

    private boolean endorSystemOccupationPending(
            List<AiBoardAnalyzer.LocationAnalysis> allLocations) {
        if (objectiveAnalyzer == null || !objectiveAnalyzer.isAnalyzed()
                || !objectiveAnalyzer.isFlipped()
                || !EndorOperationsTacticalPolicy.isEndorOperations(
                        objectiveAnalyzer.getObjectiveBlueprintId(),
                        objectiveAnalyzer.getObjectiveTitle())) {
            return false;
        }
        AiBoardAnalyzer.LocationAnalysis endor = allLocations.stream()
                .filter(loc -> loc != null && loc.isSpace()
                        && loc.location != null
                        && "endor".equalsIgnoreCase(loc.location.getTitle()))
                .findFirst().orElse(null);
        return EndorOperationsTacticalPolicy.shouldPursueEndorSystem(
                true, true, endor != null,
                endor != null && endor.weControl(),
                endor == null ? 0.0f : endor.theirPower);
    }

    private boolean targetsEmptyEndorSite(DeploymentPlan plan) {
        return plan.getInstructions().stream()
                .map(DeploymentInstruction::getTargetLocationName)
                .filter(Objects::nonNull)
                .anyMatch(title -> title.toLowerCase(Locale.ROOT).startsWith("endor:"));
    }

    private boolean occupiesEndorBattlegroundSite(
            List<AiBoardAnalyzer.LocationAnalysis> allLocations) {
        return allLocations.stream()
                .anyMatch(loc -> loc != null
                        && loc.isGround()
                        && loc.isBattleground
                        && loc.location != null
                        && loc.location.getTitle() != null
                        && loc.location.getTitle().toLowerCase(Locale.ROOT)
                                .startsWith("endor:")
                        && loc.weControl());
    }

    private static final class EndorSystemPackage {
        private final CardInfo ship;
        private final CardInfo pilot;
        private final int cost;
        private final float combinedAbility;
        private final int combinedPower;

        private EndorSystemPackage(
                CardInfo ship,
                CardInfo pilot,
                int cost,
                float combinedAbility,
                int combinedPower) {
            this.ship = ship;
            this.pilot = pilot;
            this.cost = cost;
            this.combinedAbility = combinedAbility;
            this.combinedPower = combinedPower;
        }
    }

    /**
     * Generate combined ground+space plans.
     */
    private List<ScoredPlan> generateCombinedPlans(List<CardInfo> characters, List<CardInfo> starships,
                                                    List<CardInfo> vehicles, LocationCategories categories,
                                                    int forceAvailable, int groundThreshold, int spaceThreshold,
                                                    List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                    int turn, DrainGapResult drainGap) {
        List<ScoredPlan> plans = new ArrayList<>();

        // Combined: Best ground + best space within budget
        List<CardInfo> groundCards = new ArrayList<>(characters);
        groundCards.addAll(vehicles);

        // Find best single ground target
        AiBoardAnalyzer.LocationAnalysis bestGroundTarget = findBestTarget(
            categories, true, groundThreshold);
        AiBoardAnalyzer.LocationAnalysis bestSpaceTarget = findBestTarget(
            categories, false, spaceThreshold);

        if (bestGroundTarget != null && bestSpaceTarget != null) {
            DeploymentPlan combinedPlan = new DeploymentPlan(
                DeployStrategy.COMPREHENSIVE, "Combined ground + space deployment");

            int remaining = forceAvailable;

            // Try ground first - must BEAT enemy power, not just have cards
            int groundPowerNeeded = (int) bestGroundTarget.theirPower + RandoConfig.BATTLE_FAVORABLE_THRESHOLD;
            OptimalCombination groundCombo = findOptimalCombination(
                groundCards, remaining / 2, groundPowerNeeded, true);

            // CRITICAL: Only add if we can actually beat them!
            if (groundCombo.achievesGoal && !groundCombo.isEmpty()) {
                for (PhysicalCard card : groundCombo.cards) {
                    addCardToPlan(combinedPlan, card, bestGroundTarget, 1,
                        "Combined: ground attack");
                }
                remaining -= groundCombo.totalCost;
            }

            // Then space - must BEAT enemy power
            int spacePowerNeeded = (int) bestSpaceTarget.theirPower + RandoConfig.BATTLE_FAVORABLE_THRESHOLD;
            OptimalCombination spaceCombo = findOptimalCombination(
                starships, remaining, spacePowerNeeded, true);

            // CRITICAL: Only add if we can actually beat them!
            if (spaceCombo.achievesGoal && !spaceCombo.isEmpty()) {
                for (PhysicalCard card : spaceCombo.cards) {
                    addCardToPlan(combinedPlan, card, bestSpaceTarget, 1,
                        "Combined: space attack");
                }
            }

            if (!combinedPlan.getInstructions().isEmpty()) {
                float score = scorePlan(combinedPlan, allLocations, turn);
                plans.add(new ScoredPlan(combinedPlan, score, "combined"));
            }
        }

        return plans;
    }

    // =========================================================================
    // SPECIFIC PLAN GENERATORS
    // =========================================================================

    /**
     * Generate stop-bleeding plan (Item #8 - presence-only).
     */
    private DeploymentPlan generateStopBleedingPlan(List<CardInfo> cards,
                                                     List<AiBoardAnalyzer.LocationAnalysis> bleedLocations,
                                                     int forceAvailable, int threshold, String domain) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Stop bleeding in " + domain);

        // Sort by our icons (highest drain first)
        bleedLocations.sort((a, b) -> Integer.compare(b.ourForceIcons, a.ourForceIcons));

        int remaining = forceAvailable;
        List<CardInfo> available = new ArrayList<>(cards);

        for (AiBoardAnalyzer.LocationAnalysis loc : bleedLocations) {
            if (remaining <= 0 || available.isEmpty()) break;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            // Calculate power needed accounting for ability differential
            // If enemy has more ability, they draw more destiny (avg ~2.5 per ability)
            // So we need extra power margin to compensate
            float abilityPenalty = Math.max(0, loc.theirAbility - 4) * 2.5f; // Penalize if they have >4 ability
            int effectivePowerNeeded = (int) (loc.theirPower + abilityPenalty);

            // Find optimal combination to beat enemy effective power
            OptimalCombination combo = findOptimalCombination(
                deployableHere, remaining, effectivePowerNeeded, true);

            // CRITICAL FIX: Only deploy if we can ACTUALLY beat them!
            // Also check ability - if they have significantly more ability, they'll draw
            // more destiny and likely win even with equal power.
            boolean abilityOk = combo.totalAbility >= loc.theirAbility ||
                                combo.totalPower >= loc.theirPower + 3; // Need power margin if ability disadvantage

            if (!combo.isEmpty() && combo.achievesGoal && abilityOk) {
                for (PhysicalCard card : combo.cards) {
                    CardInfo info = findCardInfo(available, card);
                    if (info != null) {
                        addCardToPlan(plan, card, loc, 1,
                            String.format("Stop bleed at %s (prevent %d drain)",
                                loc.location.getTitle(), loc.ourForceIcons));
                        remaining -= info.cost;
                        available.removeIf(c -> c.card == card);
                    }
                }
            } else if (!combo.isEmpty() && !abilityOk) {
                LOG.info("📋 Skipping stop-bleed at {} - ability disadvantage (ours: {}, theirs: {}) with only {} power margin",
                    loc.location.getTitle(), combo.totalAbility, (int) loc.theirAbility,
                    combo.totalPower - (int) loc.theirPower);
            } else if (!combo.isEmpty()) {
                LOG.info("📋 Skipping stop-bleed at {} - can't beat enemy power {} with available cards (best: {})",
                    loc.location.getTitle(), (int) loc.theirPower, combo.totalPower);
            }
        }

        return plan;
    }

    /**
     * Generate reinforce plan for losing locations.
     */
    private DeploymentPlan generateReinforcePlan(List<CardInfo> cards,
                                                  List<AiBoardAnalyzer.LocationAnalysis> losingLocations,
                                                  int forceAvailable, String domain) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Reinforce losing " + domain);

        // Sort by power deficit (worst first)
        losingLocations.sort((a, b) -> Double.compare(a.getPowerAdvantage(), b.getPowerAdvantage()));

        int remaining = forceAvailable;
        List<CardInfo> available = new ArrayList<>(cards);

        for (AiBoardAnalyzer.LocationAnalysis loc : losingLocations) {
            if (remaining <= 0 || available.isEmpty()) break;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            int deficit = (int) (loc.theirPower - loc.ourPower);

            // Find cards to close the gap
            OptimalCombination combo = findOptimalCombination(deployableHere, remaining, deficit, false);

            if (!combo.isEmpty()) {
                for (PhysicalCard card : combo.cards) {
                    CardInfo info = findCardInfo(available, card);
                    if (info != null) {
                        addCardToPlan(plan, card, loc, 1,
                            String.format("Reinforce %s (deficit: %d)",
                                loc.location.getTitle(), deficit));
                        remaining -= info.cost;
                        available.removeIf(c -> c.card == card);
                    }
                }
            }
        }

        return plan;
    }

    /**
     * Generate establish plan for uncontested locations.
     */
    private DeploymentPlan generateEstablishPlan(List<CardInfo> cards,
                                                  List<AiBoardAnalyzer.LocationAnalysis> establishTargets,
                                                  int forceAvailable, int threshold, String domain) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.ESTABLISH,
            "Establish in " + domain);

        // Sort by opponent icons (highest value first), with V22 objective bonus
        establishTargets.sort((a, b) -> {
            int scoreA = a.theirForceIcons;
            int scoreB = b.theirForceIcons;
            // V22: Objective-relevant locations get MAJOR boost in sort priority
            // Changed from +5 to +50 - objective locations should be top establish targets
            if (objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()) {
                String titleA = a.location.getTitle();
                String titleB = b.location.getTitle();
                if (objectiveAnalyzer.isObjectiveRelevantLocation(titleA)) {
                    scoreA += 50;
                    LOG.warn("📋 V22: Boosting {} in establish plan (objective-relevant, +50)", titleA);
                }
                if (objectiveAnalyzer.isObjectiveRelevantLocation(titleB)) {
                    scoreB += 50;
                    LOG.warn("📋 V22: Boosting {} in establish plan (objective-relevant, +50)", titleB);
                }
            }
            return Integer.compare(scoreB, scoreA);
        });

        int remaining = forceAvailable;
        int establishCount = 0;
        List<CardInfo> available = new ArrayList<>(cards);

        for (AiBoardAnalyzer.LocationAnalysis loc : establishTargets) {
            if (remaining <= 0 || available.isEmpty()) break;
            if (establishCount >= RandoConfig.MAX_ESTABLISH_LOCATIONS) break;
            if (loc.theirForceIcons <= 0) continue;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            // Find cards with good ability (can defend against counter-deploy)
            List<CardInfo> withAbility = deployableHere.stream()
                .filter(c -> c.ability >= RandoConfig.ABILITY_THRESHOLD)
                .collect(Collectors.toList());

            if (withAbility.isEmpty()) {
                // No ability cards - need optimal combination
                OptimalCombination combo = findOptimalCombination(deployableHere, remaining, threshold, false);
                if (combo.hasAbility && !combo.isEmpty()) {
                    for (PhysicalCard card : combo.cards) {
                        CardInfo info = findCardInfo(available, card);
                        if (info != null) {
                            addCardToPlan(plan, card, loc, 2,
                                String.format("Establish at %s (%d icons)",
                                    loc.location.getTitle(), loc.theirForceIcons));
                            remaining -= info.cost;
                            available.removeIf(c -> c.card == card);
                        }
                    }
                    establishCount++;
                }
            } else {
                // Pick best ability card
                final int budget = remaining;  // Capture for lambda
                CardInfo best = withAbility.stream()
                    .filter(c -> c.cost <= budget)
                    .max(Comparator.comparingDouble(CardInfo::getValueRatio))
                    .orElse(null);

                if (best != null) {
                    // SOLO DEPLOY GUARD: Only send a character alone if they're powerful enough
                    // to survive a counter-deploy + battle. Characters below MIN_SOLO_DEPLOY_POWER
                    // (e.g., Jango at 4, Mara at 5) will get isolated and overwhelmed.
                    // Instead, fall through to find an optimal multi-character combo.
                    if (best.power >= RandoConfig.MIN_SOLO_DEPLOY_POWER) {
                        addCardToPlan(plan, best.card, loc, 2,
                            String.format("Establish at %s (%d icons, ability %d, power %d - solo OK)",
                                loc.location.getTitle(), loc.theirForceIcons, best.ability, best.power));
                        remaining -= best.cost;
                        available.remove(best);
                        establishCount++;
                    } else {
                        // Character is too weak to stand alone — try a group combo instead
                        LOG.info("📋 SOLO GUARD: {} (power {}) below MIN_SOLO_DEPLOY_POWER {} at {} — seeking group",
                            best.name, best.power, RandoConfig.MIN_SOLO_DEPLOY_POWER,
                            loc.location.getTitle());
                        OptimalCombination combo = findOptimalCombination(
                            deployableHere, remaining, RandoConfig.MIN_SOLO_DEPLOY_POWER, false);
                        if (!combo.isEmpty() && combo.totalPower >= RandoConfig.MIN_SOLO_DEPLOY_POWER) {
                            for (PhysicalCard card : combo.cards) {
                                CardInfo info = findCardInfo(available, card);
                                if (info != null) {
                                    addCardToPlan(plan, card, loc, 2,
                                        String.format("Establish at %s (group, power %d)",
                                            loc.location.getTitle(), combo.totalPower));
                                    remaining -= info.cost;
                                    available.removeIf(c -> c.card == card);
                                }
                            }
                            establishCount++;
                        } else {
                            // V59 SOLO GUARD FALLBACK: Before giving up on this weak character,
                            // try to route them to an OWN location where we already have a friendly.
                            // Deploying alongside an existing friendly converts a "solo deploy"
                            // into a reinforcement — the weak character arrives with backup.
                            // FIXES Issue #5 from peaceful-pike replay: Obi-Wan (power 5) stayed
                            // in hand all game because Mustafar was the only establish target
                            // and no group could deploy there.
                            PhysicalCard reinforcementSite = findFriendlyReinforcementSite(
                                best.card, loc.location);
                            if (reinforcementSite != null) {
                                AiBoardAnalyzer.LocationAnalysis reinLoc = null;
                                try {
                                    String oppId = currentGame.getOpponent(currentPlayerId);
                                    reinLoc = AiBoardAnalyzer.analyzeLocation(
                                        currentGame, currentPlayerId, oppId,
                                        reinforcementSite, currentSide);
                                } catch (Exception e) { /* ignore */ }
                                if (reinLoc != null) {
                                    addCardToPlan(plan, best.card, reinLoc, 2,
                                        String.format("V59 REINFORCE: %s joining friendlies at %s",
                                            best.name, reinforcementSite.getTitle()));
                                    remaining -= best.cost;
                                    available.remove(best);
                                    establishCount++;
                                    LOG.warn("📋 V59 SOLO GUARD REROUTE: {} (power {}) → {} (own site with friendlies)",
                                        best.name, best.power, reinforcementSite.getTitle());
                                } else {
                                    LOG.info("📋 SOLO GUARD: No valid group for {} — skipping location to avoid lone deployment",
                                        loc.location.getTitle());
                                }
                            } else {
                                LOG.info("📋 SOLO GUARD: No valid group for {} — skipping location to avoid lone deployment",
                                    loc.location.getTitle());
                                // Skip — deploying solo here is too risky
                            }
                        }
                    }
                }
            }
        }

        return plan;
    }

    /**
     * V59: Find an own location where we already have a friendly character,
     * where `card` can legally deploy. Used as a fallback when SOLO GUARD
     * fails at an establish target — reroute the weak character to where
     * it auto-joins an existing group.
     *
     * @param card the card we want to deploy
     * @param skipLocation skip this location (it's the failed establish target)
     * @return an own location with friendly presence, or null
     */
    private PhysicalCard findFriendlyReinforcementSite(PhysicalCard card, PhysicalCard skipLocation) {
        if (card == null || currentGame == null) return null;
        try {
            GameState gs = currentGame.getGameState();
            List<PhysicalCard> tops = gs.getTopLocations();
            if (tops == null) return null;
            PhysicalCard best = null;
            float bestOurPower = -1;
            for (PhysicalCard loc : tops) {
                if (loc == null || loc == skipLocation) continue;
                // Only sites (not systems) make sense for character reinforcement
                if (loc.getBlueprint() == null) continue;
                if (loc.getBlueprint().getCardSubtype() == null) continue;
                if (loc.getBlueprint().getCardSubtype() != com.gempukku.swccgo.common.CardSubtype.SITE) continue;
                // Must have own friendly character here
                float ourPower = 0;
                boolean hasFriendly = false;
                List<PhysicalCard> atLoc = gs.getCardsAtLocation(loc);
                if (atLoc != null) {
                    for (PhysicalCard c : atLoc) {
                        if (c != null && currentPlayerId.equals(c.getOwner())
                            && c.getBlueprint() != null
                            && c.getBlueprint().getCardCategory() == CardCategory.CHARACTER) {
                            hasFriendly = true;
                            if (c.getBlueprint().hasPowerAttribute()) {
                                Float p = c.getBlueprint().getPower();
                                if (p != null) ourPower += p;
                            }
                        }
                    }
                }
                if (!hasFriendly) continue;
                // Verify the deploying card can actually reach this location
                if (!canCardDeployHere(card, loc)) continue;
                // Prefer site with highest own power (most-consolidated group)
                if (ourPower > bestOurPower) {
                    bestOurPower = ourPower;
                    best = loc;
                }
            }
            return best;
        } catch (Exception e) {
            LOG.debug("V59 findFriendlyReinforcementSite error: {}", e.getMessage());
            return null;
        }
    }

    /**
     * V59 helper: best-effort check that `card` can deploy to `location`.
     * Uses the same filter logic as filterDeployableCards but for a single card.
     */
    private boolean canCardDeployHere(PhysicalCard card, PhysicalCard location) {
        try {
            if (card == null || card.getBlueprint() == null) return false;
            List<CardInfo> single = new ArrayList<>();
            single.add(new CardInfo(card));
            List<CardInfo> deployable = filterDeployableCards(single, location);
            return !deployable.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate attack plan for enemy-held locations.
     */
    private DeploymentPlan generateAttackPlan(List<CardInfo> cards,
                                               List<AiBoardAnalyzer.LocationAnalysis> attackTargets,
                                               int forceAvailable, String domain) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Attack enemy " + domain);

        // Sort by our icons (stopping highest drains first)
        attackTargets.sort((a, b) -> Integer.compare(b.ourForceIcons, a.ourForceIcons));

        int remaining = forceAvailable;
        List<CardInfo> available = new ArrayList<>(cards);

        for (AiBoardAnalyzer.LocationAnalysis loc : attackTargets) {
            if (remaining <= 0 || available.isEmpty()) break;

            // CRITICAL: Filter cards to only those that can deploy to this location
            List<CardInfo> deployableHere = filterDeployableCards(available, loc.location);
            if (deployableHere.isEmpty()) {
                LOG.debug("📋 No cards can deploy to {} - skipping", loc.location.getTitle());
                continue;
            }

            // Need to beat enemy power with favorable threshold
            int powerNeeded = (int) loc.theirPower + RandoConfig.BATTLE_FAVORABLE_THRESHOLD;

            OptimalCombination combo = findOptimalCombination(deployableHere, remaining, powerNeeded, true);

            if (combo.achievesGoal && !combo.isEmpty()) {
                for (PhysicalCard card : combo.cards) {
                    CardInfo info = findCardInfo(available, card);
                    if (info != null) {
                        addCardToPlan(plan, card, loc, 1,
                            String.format("Attack %s (%d vs %d power)",
                                loc.location.getTitle(), combo.totalPower, (int)loc.theirPower));
                        remaining -= info.cost;
                        available.removeIf(c -> c.card == card);
                    }
                }
            }
        }

        return plan;
    }

    /**
     * V22: Generate plan to deploy a capital ship to the objective-relevant system (e.g., Bespin).
     * For TDIGWATT, getting the Executor to Bespin system is a top strategic priority.
     */
    private DeploymentPlan generateObjectiveCapitalPlan(List<CardInfo> starships,
                                                         List<CardInfo> characters,
                                                         List<AiBoardAnalyzer.LocationAnalysis> allLocations,
                                                         int forceAvailable,
                                                         SwccgGame game, String playerId) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.ESTABLISH,
            "Deploy capital ship to objective system (Bespin)");

        // Find Bespin system on the board
        AiBoardAnalyzer.LocationAnalysis bespinSystem = null;
        for (AiBoardAnalyzer.LocationAnalysis loc : allLocations) {
            String title = loc.location.getTitle();
            if (title != null && (title.toLowerCase().contains("bespin") ||
                                   title.toLowerCase().contains("cloud city")) && loc.isSpace()) {
                bespinSystem = loc;
                break;
            }
        }

        if (bespinSystem == null) {
            LOG.warn("📋 V22 CAPITAL: Bespin system not found on board yet - skipping capital plan");
            return plan;
        }

        int remaining = forceAvailable;
        final int budget = remaining;
        List<CardInfo> affordable = starships.stream()
            .filter(s -> s.cost <= budget)
            .sorted(Comparator.comparingInt((CardInfo s) -> s.power).reversed())
            .collect(Collectors.toList());

        if (affordable.isEmpty()) {
            LOG.warn("📋 V22 CAPITAL: No affordable capital ships in hand");
            return plan;
        }

        CardInfo bestShip = affordable.get(0);
        addCardToPlan(plan, bestShip.card, bespinSystem, 1,
            String.format("V22: Deploy %s to Bespin for objective (power %d)", bestShip.name, bestShip.power));
        remaining -= bestShip.cost;

        // Try to add a pilot
        final int pilotBudget = remaining;
        List<CardInfo> affordablePilots = characters.stream()
            .filter(c -> c.isPilot && c.cost <= pilotBudget)
            .sorted(Comparator.comparingInt((CardInfo c) -> c.ability).reversed())
            .collect(Collectors.toList());

        if (!affordablePilots.isEmpty()) {
            CardInfo pilot = affordablePilots.get(0);
            addCardToPlan(plan, pilot.card, bespinSystem, 1,
                String.format("V22: Deploy pilot %s for %s", pilot.name, bestShip.name));
            LOG.warn("📋 V22 CAPITAL: Planning {} + pilot {} to Bespin", bestShip.name, pilot.name);
        } else {
            LOG.warn("📋 V22 CAPITAL: Planning {} to Bespin (no affordable pilot)", bestShip.name);
        }

        return plan;
    }

    /**
     * Generate RE-PILOT plan for unpiloted ships (Item #6).
     */
    private DeploymentPlan generateRepilotPlan(List<CardInfo> pilots, SwccgGame game,
                                                String playerId, int forceAvailable) {
        DeploymentPlan plan = new DeploymentPlan(DeployStrategy.REINFORCE,
            "Re-pilot unpiloted ships");

        if (pilots.isEmpty()) return plan;

        // Find unpiloted ships in play
        List<PhysicalCard> unpilotedShips = findUnpilotedShipsInPlay(game, playerId);
        if (unpilotedShips.isEmpty()) return plan;

        int remaining = forceAvailable;
        List<CardInfo> availablePilots = new ArrayList<>(pilots);

        for (PhysicalCard ship : unpilotedShips) {
            if (remaining <= 0 || availablePilots.isEmpty()) break;

            // Find best pilot for this ship
            final int budget = remaining;  // Capture for lambda
            CardInfo bestPilot = availablePilots.stream()
                .filter(p -> p.cost <= budget)
                .max(Comparator.comparingInt(p -> p.ability))
                .orElse(null);

            if (bestPilot != null) {
                // Create instruction to deploy pilot to ship
                DeploymentInstruction inst = new DeploymentInstruction(
                    bestPilot.blueprintId, bestPilot.name,
                    String.valueOf(ship.getCardId()), ship.getTitle(),
                    1, String.format("Re-pilot %s", ship.getTitle())
                );
                inst.setCardPermanentCardId(bestPilot.card.getPermanentCardId());
                inst.setCardCurrentCardId(bestPilot.card.getCardId());
                inst.setDeployCost(bestPilot.cost);
                inst.setPowerContribution(bestPilot.power);
                plan.addInstruction(inst);

                remaining -= bestPilot.cost;
                availablePilots.remove(bestPilot);
            }
        }

        return plan;
    }

    // =========================================================================
    // PLAN SCORING (Item #21)
    // =========================================================================

    /**
     * Score a deployment plan.
     *
     * Ported from Python _score_plan().
     */
    private float scorePlan(DeploymentPlan plan, List<AiBoardAnalyzer.LocationAnalysis> locations, int turn) {
        if (plan == null || plan.getInstructions().isEmpty()) {
            return 0.0f;
        }

        List<DeployPlanRankingPolicy.InstructionFacts> instructionFacts = new ArrayList<>();
        Map<String, Integer> powerByLocation = new HashMap<>();
        Map<String, Integer> abilityByLocation = new HashMap<>();

        int instructionIndex = 0;
        for (DeploymentInstruction inst : plan.getInstructions()) {
            instructionFacts.add(new DeployPlanRankingPolicy.InstructionFacts(
                "instruction-" + instructionIndex++, inst.getPowerContribution()));

            // Track by location
            if (inst.getTargetLocationId() != null) {
                powerByLocation.merge(inst.getTargetLocationId(), inst.getPowerContribution(), Integer::sum);

                // V32: Use actual ability contribution instead of estimating from power.
                // Previous code used MIN(power, 4) which is wrong — a character with
                // power 7 and ability 1 would be estimated as ability 4, causing the
                // planner to think it reached the battle destiny threshold when it didn't.
                int ability = inst.getAbilityContribution();
                if (ability == 0) {
                    // Fallback: if ability wasn't set, use conservative estimate
                    ability = Math.min(inst.getPowerContribution(), 3);
                }
                abilityByLocation.merge(inst.getTargetLocationId(), ability, Integer::sum);
            }
        }

        // Analyze each target location
        List<DeployPlanRankingPolicy.LocationFacts> locationFacts = new ArrayList<>();
        int locationIndex = 0;
        for (Map.Entry<String, Integer> entry : powerByLocation.entrySet()) {
            String locId = entry.getKey();
            int ourPower = entry.getValue();
            int ourAbility = abilityByLocation.getOrDefault(locId, 0);

            // Find location
            AiBoardAnalyzer.LocationAnalysis targetLoc = null;
            for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
                if (String.valueOf(loc.location.getCardId()).equals(locId)) {
                    targetLoc = loc;
                    break;
                }
            }

            if (targetLoc == null) continue;

            // V22: Objective-relevant location bonus for plan scoring
            boolean objectiveRelevant = false;
            float objectiveBonus = 0.0f;
            if (objectiveAnalyzer != null && objectiveAnalyzer.isAnalyzed()) {
                String locTitle = targetLoc.location.getTitle();
                if (locTitle != null && objectiveAnalyzer
                        .isObjectiveRelevantLocation(
                                targetLoc.location, currentGame,
                                currentPlayerId)) {
                    float objBonus = objectiveAnalyzer
                            .getLocationObjectiveBonus(
                                    targetLoc.location, currentGame,
                                    currentPlayerId);
                    objectiveRelevant = objBonus != 0.0f;
                    objectiveBonus = objBonus;
                    LOG.warn("V22 PLAN SCORE: {} is objective-relevant, +{} to plan score", locTitle, objBonus);
                }
            }

            locationFacts.add(new DeployPlanRankingPolicy.LocationFacts(
                "location-" + locationIndex++, ourPower, ourAbility,
                targetLoc.theirPower, targetLoc.ourForceIcons,
                targetLoc.theirForceIcons, objectiveRelevant, objectiveBonus));
        }

        return DeployPlanRankingPolicy.apply(0.0f,
            DeployPlanRankingPolicy.evaluate(instructionFacts, locationFacts));
    }

    private float scoreObjectiveCapitalPlan(DeploymentPlan plan,
                                             List<AiBoardAnalyzer.LocationAnalysis> locations,
                                             int turn) {
        float score = scorePlan(plan, locations, turn);
        return DeployPlanRankingPolicy.apply(score,
            DeployPlanRankingPolicy.evaluateAdjunct(
                new DeployPlanRankingPolicy.AdjunctFacts(
                    "objective-capital-bespin", true)));
    }

    private float scoreObjectiveFlipGateFormationPlan(
            DeploymentPlan plan,
            List<AiBoardAnalyzer.LocationAnalysis> locations,
            int turn) {
        float score = scorePlan(plan, locations, turn);
        ObjectiveAnalyzer.ObjectivePlaybook playbook =
            objectiveAnalyzer != null ? objectiveAnalyzer.getActivePlaybook() : null;
        float objectiveBonus = playbook != null
            ? playbook.weights.deployFlipGateSite : 0.0f;
        return DeployPlanRankingPolicy.apply(score,
            DeployPlanRankingPolicy.evaluateFlipGateFormation(
                new DeployPlanRankingPolicy.FlipGateFormationFacts(
                    "objective-flip-gate-formation", true, objectiveBonus)));
    }

    private boolean isObjectiveFlipGateFormationPlan(DeploymentPlan plan) {
        return plan != null && plan.getReason() != null
            && plan.getReason().startsWith("V297 objective flip-gate formation");
    }

    /**
     * Select the best plan from all generated plans.
     */
    private DeploymentPlan selectBestPlan(List<ScoredPlan> allPlans, List<CardInfo> locationDeploys,
                                           int turn, int lifeForce) {
        if (allPlans.isEmpty()) {
            // Just deploy locations if nothing else
            if (!locationDeploys.isEmpty()) {
                DeploymentPlan locationPlan = new DeploymentPlan(
                    DeployStrategy.DEPLOY_LOCATIONS, "Deploy locations");
                for (CardInfo loc : locationDeploys) {
                    DeploymentInstruction inst = new DeploymentInstruction(
                        loc.blueprintId, loc.name, null, null, 0, "Deploy location");
                    inst.setCardPermanentCardId(loc.card.getPermanentCardId());
                    inst.setCardCurrentCardId(loc.card.getCardId());
                    inst.setDeployCost(loc.cost);
                    locationPlan.addInstruction(inst);
                }
                return locationPlan;
            }
            return null;
        }

        // Sort by score (highest first)
        Collections.sort(allPlans);

        LOG.info("📋 PLAN COMPARISON ({} plans generated):", allPlans.size());
        for (int i = 0; i < Math.min(5, allPlans.size()); i++) {
            ScoredPlan sp = allPlans.get(i);
            LOG.info("   {}. {} - score: {:.0f}, cards: {}",
                i + 1, sp.domain, sp.score, sp.plan.getInstructions().size());
        }

        ScoredPlan best = allPlans.get(0);

        // Add location deploys to the best plan
        DeploymentPlan finalPlan = new DeploymentPlan(best.plan.getStrategy(), best.plan.getReason());
        for (CardInfo loc : locationDeploys) {
            DeploymentInstruction inst = new DeploymentInstruction(
                loc.blueprintId, loc.name, null, null, 0, "Deploy location first");
            inst.setCardPermanentCardId(loc.card.getPermanentCardId());
            inst.setCardCurrentCardId(loc.card.getCardId());
            inst.setDeployCost(loc.cost);
            finalPlan.addInstruction(inst);
        }
        for (DeploymentInstruction inst : best.plan.getInstructions()) {
            finalPlan.addInstruction(inst);
        }

        return finalPlan;
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void addCardToPlan(DeploymentPlan plan, PhysicalCard card,
                               AiBoardAnalyzer.LocationAnalysis loc, int priority, String reason) {
        SwccgCardBlueprint bp = card.getBlueprint();
        int cost = 0;
        int power = 0;

        if (bp != null) {
            try {
                Float c = bp.getDeployCost();
                cost = c != null ? c.intValue() : 0;
            } catch (UnsupportedOperationException e) {}

            if (bp.hasPowerAttribute()) {
                Float p = bp.getPower();
                power = p != null ? p.intValue() : 0;
            }
        }

        DeploymentInstruction inst = new DeploymentInstruction(
            card.getBlueprintId(true), card.getTitle(),
            String.valueOf(loc.location.getCardId()), loc.location.getTitle(),
            priority, reason
        );
        inst.setCardPermanentCardId(card.getPermanentCardId());
        inst.setCardCurrentCardId(card.getCardId());
        inst.setDeployCost(cost);
        inst.setPowerContribution(power);
        plan.addInstruction(inst);
    }

    private CardInfo findCardInfo(List<CardInfo> cards, PhysicalCard card) {
        return cards.stream().filter(c -> c.card == card).findFirst().orElse(null);
    }

    private AiBoardAnalyzer.LocationAnalysis findBestTarget(LocationCategories categories,
                                                             boolean isGround, int threshold) {
        // Priority: bleed > losing > attack > establish
        List<AiBoardAnalyzer.LocationAnalysis> targets = new ArrayList<>();
        targets.addAll(categories.bleedLocations);
        targets.addAll(categories.losingLocations);
        targets.addAll(categories.attackTargets);
        targets.addAll(categories.establishTargets);

        return targets.stream()
            .filter(loc -> isGround ? loc.isGround() : loc.isSpace())
            .max(Comparator.comparingInt(loc -> loc.theirForceIcons + loc.ourForceIcons))
            .orElse(null);
    }

    private List<PhysicalCard> findUnpilotedShipsInPlay(SwccgGame game, String playerId) {
        List<PhysicalCard> unpiloted = new ArrayList<>();

        // Check all locations for unpiloted ships
        GameState gameState = game.getGameState();

        // Get all locations in play and check cards at each
        for (PhysicalCard location : gameState.getLocationsInOrder()) {
            if (location == null) continue;

            // Get all cards at this location
            List<PhysicalCard> cardsAtLocation = gameState.getCardsAtLocation(location);
            for (PhysicalCard card : cardsAtLocation) {
                if (card.getOwner().equals(playerId) &&
                    card.getBlueprint() != null &&
                    card.getBlueprint().getCardCategory() == CardCategory.STARSHIP) {

                    // Check if ship has a pilot aboard
                    List<PhysicalCard> aboard = gameState.getAboardCards(card, false);
                    boolean hasPilot = aboard.stream().anyMatch(c ->
                        c.getBlueprint() != null &&
                        c.getBlueprint().getCardCategory() == CardCategory.CHARACTER);

                    if (!hasPilot) {
                        unpiloted.add(card);
                    }
                }
            }
        }

        return unpiloted;
    }

    private void logHandDetails(List<CardInfo> characters, List<CardInfo> starships, List<CardInfo> vehicles) {
        if (!characters.isEmpty()) {
            StringBuilder sb = new StringBuilder("   📋 Characters: [");
            for (CardInfo c : characters) {
                sb.append(c.toString()).append(", ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }
        if (!starships.isEmpty()) {
            StringBuilder sb = new StringBuilder("   🚀 Starships: [");
            for (CardInfo s : starships) {
                sb.append(s.toString()).append(", ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }
        if (!vehicles.isEmpty()) {
            StringBuilder sb = new StringBuilder("   🚗 Vehicles: [");
            for (CardInfo v : vehicles) {
                sb.append(v.toString()).append(", ");
            }
            sb.append("]");
            LOG.info(sb.toString());
        }
    }

    private void logBoardAnalysis(List<AiBoardAnalyzer.LocationAnalysis> locations) {
        LOG.info("   📍 Board locations ({}):", locations.size());
        for (AiBoardAnalyzer.LocationAnalysis loc : locations) {
            String domain = loc.isSpace() ? "SPACE" : "GROUND";
            String status = "";
            if (loc.ourPower > 0 && loc.theirPower > 0) {
                status = loc.ourPower > loc.theirPower ? "WINNING" : "LOSING";
            } else if (loc.ourPower > 0) {
                status = "CONTROLLED";
            } else if (loc.theirPower > 0) {
                status = "ENEMY";
            } else {
                status = "EMPTY";
            }
            LOG.info("      {} [{}] {} - us:{} them:{} icons:{}/{}",
                loc.location.getTitle(), domain, status,
                (int)loc.ourPower, (int)loc.theirPower,
                loc.ourForceIcons, loc.theirForceIcons);
        }
    }

    private void logFinalPlan(DeploymentPlan plan) {
        String strategyName = plan.getStrategy() != null ?
            plan.getStrategy().getValue().toLowerCase() : "unknown";
        LOG.info("📋 ═══════════════════════════════════════════════════════════════");
        LOG.info("📋 FINAL PLAN: {} ({} deployments)", strategyName, plan.getInstructions().size());
        LOG.info("📋 ═══════════════════════════════════════════════════════════════");

        int num = 1;
        for (DeploymentInstruction inst : plan.getInstructions()) {
            String target = inst.getTargetLocationName() != null ?
                inst.getTargetLocationName() : "table";
            LOG.info("   {}. {} -> {} : {}", num++, inst.getCardName(), target, inst.getReason());
        }
    }

    private DeploymentPlan createHoldBackPlan(String reason) {
        return new DeploymentPlan(DeployStrategy.HOLD_BACK, reason);
    }

    /**
     * Generate all combinations of size k from list.
     */
    private <T> List<List<T>> combinations(List<T> list, int k) {
        List<List<T>> result = new ArrayList<>();
        combinationsHelper(list, k, 0, new ArrayList<>(), result);
        return result;
    }

    private <T> void combinationsHelper(List<T> list, int k, int start,
                                         List<T> current, List<List<T>> result) {
        if (current.size() == k) {
            result.add(new ArrayList<>(current));
            return;
        }
        for (int i = start; i < list.size(); i++) {
            current.add(list.get(i));
            combinationsHelper(list, k, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }

    // =========================================================================
    // PUBLIC API (for DeployEvaluator)
    // =========================================================================

    /**
     * Get score for a specific card deployment.
     */
    public PlanScore getCardScore(String blueprintId, int currentForce, List<String> availableBlueprints) {
        if (currentPlan == null) {
            return new PlanScore(0.0f, "No active plan");
        }

        DeploymentInstruction instruction = currentPlan.getInstructionForCard(blueprintId);
        if (instruction != null) {
            float score = 100.0f - (instruction.getPriority() * 10);
            return new PlanScore(score, "IN PLAN: " + instruction.getReason());
        }

        if (currentPlan.isPlanComplete() || currentPlan.isForceAllowExtras()) {
            return new PlanScore(25.0f, "Extra action (plan complete)");
        }

        return new PlanScore(-50.0f, "NOT in plan - saving force for planned cards");
    }

    /**
     * Record that a card was deployed.
     */
    public void recordDeployment(String blueprintId) {
        if (currentPlan != null) {
            currentPlan.recordDeployment(blueprintId);
        }
    }

    public void recordDeployment(int permanentCardId, int currentCardId, String blueprintId) {
        if (currentPlan != null) {
            currentPlan.recordDeployment(permanentCardId, currentCardId, blueprintId);
        }
    }

    /**
     * Get plan summary.
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

    /**
     * Check if a card can deploy to a specific location based on its gametext restrictions.
     * Returns true if the card appears to have no restrictions OR if it can deploy to this location type.
     * Returns false if the card has "Deploys only" restrictions that don't match the location.
     */
    private boolean canDeployToLocation(PhysicalCard card, PhysicalCard location) {
        if (card == null || card.getBlueprint() == null) return true;
        if (location == null) return true;

        String gametext = card.getBlueprint().getGameText();
        if (gametext == null || gametext.isEmpty()) return true;

        String gametextLower = gametext.toLowerCase();

        // If no deployment restriction, card can deploy anywhere appropriate
        if (!gametextLower.contains("deploys only")) {
            return true;
        }

        // Card has deployment restrictions - check if location matches
        String locationTitle = location.getTitle();
        if (locationTitle == null) return false;
        String locationLower = locationTitle.toLowerCase();

        // Extract the restriction part (e.g., "Deploys only on Falcon, Hoth or Cloud City")
        int deploysOnlyIdx = gametextLower.indexOf("deploys only");
        String restrictionPart = gametextLower.substring(deploysOnlyIdx);
        int periodIdx = restrictionPart.indexOf('.');
        if (periodIdx > 0) {
            restrictionPart = restrictionPart.substring(0, periodIdx);
        }

        LOG.debug("📋 Checking deploy restriction for {}: '{}' vs location '{}'",
            card.getTitle(), restrictionPart, locationTitle);

        // =======================================================
        // SPECIAL CASE: Cards that deploy ON characters/ships, not TO locations
        // These cards (like Elom, weapons, devices) deploy on other cards,
        // not directly to locations. Allow them - the game engine will present
        // the correct deployment options (e.g., deploy on a character at location).
        // =======================================================
        if (restrictionPart.contains("deploys only on")) {
            String afterOn = restrictionPart.substring(restrictionPart.indexOf("deploys only on") + 15).trim();
            // Check if it's deploying on a character/ship type rather than a location
            // Character types: rebel, alien, imperial, droid, jedi, sith, warrior, pilot, etc.
            // Ship types: starship, capital, squadron, etc.
            boolean deploysOnCard = afterOn.startsWith("a ") || afterOn.startsWith("an ") ||
                afterOn.startsWith("your ") || afterOn.startsWith("opponent") ||
                afterOn.contains("rebel") || afterOn.contains("alien") ||
                afterOn.contains("imperial") || afterOn.contains("droid") ||
                afterOn.contains("jedi") || afterOn.contains("sith") ||
                afterOn.contains("character") || afterOn.contains("warrior") ||
                afterOn.contains("pilot") || afterOn.contains("smuggler") ||
                afterOn.contains("starship") || afterOn.contains("vehicle") ||
                afterOn.contains("capital") || afterOn.contains("squadron");

            if (deploysOnCard) {
                // This card deploys on another card, not directly to a location
                // Return true to allow it - game engine will handle the actual deployment
                LOG.debug("📋 {} deploys ON a card - allowing (game engine will handle)",
                    card.getTitle());
                return true;
            }
        }

        // Check common location types
        if (restrictionPart.contains("hoth") && !locationLower.contains("hoth")) {
            LOG.info("📋 {} cannot deploy to {} (requires Hoth)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("cloud city") && !locationLower.contains("cloud city")) {
            LOG.info("📋 {} cannot deploy to {} (requires Cloud City)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("tatooine") && !locationLower.contains("tatooine")) {
            LOG.info("📋 {} cannot deploy to {} (requires Tatooine)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("endor") && !locationLower.contains("endor")) {
            LOG.info("📋 {} cannot deploy to {} (requires Endor)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("dagobah") && !locationLower.contains("dagobah")) {
            LOG.info("📋 {} cannot deploy to {} (requires Dagobah)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("death star") && !locationLower.contains("death star")) {
            LOG.info("📋 {} cannot deploy to {} (requires Death Star)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("coruscant") && !locationLower.contains("coruscant")) {
            LOG.info("📋 {} cannot deploy to {} (requires Coruscant)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("naboo") && !locationLower.contains("naboo")) {
            LOG.info("📋 {} cannot deploy to {} (requires Naboo)", card.getTitle(), locationTitle);
            return false;
        }
        if (restrictionPart.contains("bespin") && !locationLower.contains("bespin")) {
            LOG.info("📋 {} cannot deploy to {} (requires Bespin)", card.getTitle(), locationTitle);
            return false;
        }

        // Check for starship-only deployments (can't deploy to site)
        if ((restrictionPart.contains("falcon") || restrictionPart.contains("starship") ||
             restrictionPart.contains("capital starship")) &&
            !restrictionPart.contains("hoth") && !restrictionPart.contains("cloud city") &&
            !restrictionPart.contains("tatooine") && !restrictionPart.contains("site")) {
            // Card only deploys to starships, not locations
            LOG.info("📋 {} cannot deploy to {} (requires starship)", card.getTitle(), locationTitle);
            return false;
        }

        // If we get here and the card has restrictions we didn't recognize,
        // be conservative and allow it (let the game engine handle it)
        return true;
    }

    /**
     * Filter cards to only those that can deploy to the target location.
     */
    private List<CardInfo> filterDeployableCards(List<CardInfo> cards, PhysicalCard targetLocation) {
        List<CardInfo> deployable = new ArrayList<>();
        for (CardInfo info : cards) {
            if (canDeployToLocation(info.card, targetLocation)) {
                deployable.add(info);
            }
        }
        return deployable;
    }

    /**
     * Simple score/reason holder.
     */
    public static class PlanScore {
        public final float score;
        public final String reason;

        public PlanScore(float score, String reason) {
            this.score = score;
            this.reason = reason;
        }
    }

    /**
     * Location categories for planning.
     */
    private static class LocationCategories {
        List<AiBoardAnalyzer.LocationAnalysis> losingLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> winningLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> bleedLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> establishTargets = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> attackTargets = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> weakPresenceLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> crushableLocations = new ArrayList<>();
        List<AiBoardAnalyzer.LocationAnalysis> drainingLocations = new ArrayList<>();
    }
}
