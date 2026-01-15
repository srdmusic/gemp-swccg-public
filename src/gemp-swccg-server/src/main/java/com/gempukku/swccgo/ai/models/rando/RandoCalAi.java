package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.models.HeuristicAiBase;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.ContestStatus;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.LocationAnalysis;
import com.gempukku.swccgo.ai.common.AiChatManager;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.rando.evaluators.CombinedEvaluator;
import com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext;
import com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveHandler;
import com.gempukku.swccgo.ai.models.rando.strategy.ShieldStrategy;
import com.gempukku.swccgo.ai.models.rando.strategy.StrategyController;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.decisions.AwaitingDecision;
import com.gempukku.swccgo.logic.decisions.AwaitingDecisionType;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.Logger;

/**
 * Rando Cal AI - An advanced AI with personality.
 *
 * Features:
 * - Enhanced heuristics beyond AdvancedAi
 * - Location-aware deployment and battle decisions
 * - Priority card handling (Houjix, Sense, Barriers)
 * - Astrogator personality via chat messages
 * - Holiday message overlays
 *
 * Based on Python Rando Cal bot architecture, ported to GEMP Java.
 */
public class RandoCalAi extends HeuristicAiBase {

    private static final Logger LOG = RandoLogger.getLogger();

    // Chat manager for personality messages
    private final AiChatManager chatManager;

    // Evaluator system for sophisticated decision-making
    private final CombinedEvaluator combinedEvaluator;

    // Decision tracker for loop detection
    private final DecisionTracker decisionTracker;

    // Strategy controller for game-wide strategy
    private final StrategyController strategyController;

    // Objective handler for starting card requirements
    private final ObjectiveHandler objectiveHandler;

    // Shield strategy for defensive shields
    private final ShieldStrategy shieldStrategy;

    // Deploy phase planner for holistic deployment plans
    private final DeployPhasePlanner deployPhasePlanner;

    // Personality system (will be set via setter after construction)
    private AstrogatorPersonality personality;
    private HolidayOverlay holidayOverlay;

    // Game context (rebuilt each decision)
    private RandoContext context;
    private SwccgGame currentGame;
    private Random random = new Random();

    // State tracking
    private String currentGameId;
    private int lastTurn = -1;
    private Side mySide;
    private String opponentName;

    // Opponent tracking for strategy components
    private final Set<String> seenOpponentCards = new HashSet<>();
    private String lastPendingDeployType = null;  // Track pending deploy for confirmation

    // =========================================================================
    // Keyword Weights - Higher than AdvancedAi for more aggressive play
    // =========================================================================

    private static final KeywordWeight[] ACTION_WEIGHTS = new KeywordWeight[] {
        // Control phase actions (highest priority)
        new KeywordWeight("force drain", 200),

        // Battle actions
        new KeywordWeight("initiate battle", 180),
        new KeywordWeight("battle", 120),
        new KeywordWeight("weapon", 70),
        new KeywordWeight("fire", 65),

        // Deploy actions
        new KeywordWeight("deploy", 110),
        new KeywordWeight("play", 50),

        // Move actions
        new KeywordWeight("move", 60),

        // Activate/Draw
        new KeywordWeight("activate", 90),
        new KeywordWeight("retrieve", 50),
        new KeywordWeight("draw", 45),

        // Utility
        new KeywordWeight("steal", 45),
        new KeywordWeight("capture", 45),
        new KeywordWeight("download", 55),
        new KeywordWeight("search", 40),
        new KeywordWeight("react", 40),
        new KeywordWeight("cancel", 40),
        new KeywordWeight("take into hand", 45),

        // Priority card specific
        new KeywordWeight("barrier", 75),
        new KeywordWeight("sense", 70),
        new KeywordWeight("houjix", 100),
        new KeywordWeight("ghhhk", 100)
    };

    private static final KeywordWeight[] ACTION_PENALTIES = new KeywordWeight[] {
        new KeywordWeight("pass", -200),
        new KeywordWeight("forfeit", -120),
        new KeywordWeight("lose", -80),
        new KeywordWeight("place in lost pile", -110),
        new KeywordWeight("place in used pile", -50),
        new KeywordWeight("return to hand", -35),
        new KeywordWeight("sacrifice", -140),
        new KeywordWeight("revert", -80)
    };

    private static final KeywordWeight[] CHOICE_WEIGHTS = new KeywordWeight[] {
        new KeywordWeight("draw", 70),
        new KeywordWeight("retrieve", 55),
        new KeywordWeight("deploy", 50),
        new KeywordWeight("battle destiny", 60),
        new KeywordWeight("weapon destiny", 60),
        new KeywordWeight("activate", 50),
        new KeywordWeight("force drain", 70),
        new KeywordWeight("initiate", 50),
        new KeywordWeight("capture", 40),
        new KeywordWeight("steal", 40),
        new KeywordWeight("download", 40),
        new KeywordWeight("use", 15),
        new KeywordWeight("yes", 15)
    };

    private static final KeywordWeight[] CHOICE_PENALTIES = new KeywordWeight[] {
        new KeywordWeight("lose", -65),
        new KeywordWeight("forfeit", -80),
        new KeywordWeight("lost pile", -70),
        new KeywordWeight("used pile", -45),
        new KeywordWeight("return to hand", -30),
        new KeywordWeight("neither", -40),
        new KeywordWeight("cancel", -35),
        new KeywordWeight("pass", -50)
    };

    private static final String[] CARD_HINTS = new String[] {
        "pilot", "weapon", "character", "starship", "vehicle", "droid", "alien",
        "jedi", "sith", "effect", "interrupt", "location", "site", "system",
        "ability", "destiny", "force", "power", "forfeit", "battleground"
    };

    // =========================================================================
    // Constructor
    // =========================================================================

    public RandoCalAi() {
        this.chatManager = new AiChatManager();
        this.combinedEvaluator = new CombinedEvaluator();
        this.decisionTracker = new DecisionTracker();
        this.strategyController = new StrategyController();
        this.objectiveHandler = new ObjectiveHandler();
        this.shieldStrategy = new ShieldStrategy();
        this.deployPhasePlanner = new DeployPhasePlanner();
        this.personality = new AstrogatorPersonality();
        this.holidayOverlay = HolidayOverlay.getInstance();
        LOG.info("RandoCalAi initialized with {} evaluators", combinedEvaluator.getEvaluators().size());

        // Run startup self-tests
        runStartupSelfTests();
    }

    /**
     * Run self-tests at startup to verify AI configuration.
     * Logs comprehensive information about the AI state.
     */
    private void runStartupSelfTests() {
        LOG.info("========================================");
        LOG.info("🔧 RANDO CAL AI STARTUP SELF-TESTS");
        LOG.info("========================================");

        // Test 1: Verify all evaluators are registered
        LOG.info("🔧 Test 1: Evaluator Registration");
        List<String> evaluatorNames = new java.util.ArrayList<>();
        for (Object eval : combinedEvaluator.getEvaluators()) {
            if (eval instanceof com.gempukku.swccgo.ai.models.rando.evaluators.ActionEvaluator) {
                String name = ((com.gempukku.swccgo.ai.models.rando.evaluators.ActionEvaluator) eval).getName();
                evaluatorNames.add(name);
                LOG.info("   ✅ Evaluator: {}", name);
            }
        }
        if (evaluatorNames.isEmpty()) {
            LOG.error("   ❌ NO EVALUATORS REGISTERED - AI WILL NOT FUNCTION!");
        } else {
            LOG.info("   ✅ Total evaluators: {}", evaluatorNames.size());
        }

        // Test 2: Verify strategy components
        LOG.info("🔧 Test 2: Strategy Components");
        LOG.info("   ✅ StrategyController: {}", strategyController != null ? "OK" : "MISSING");
        LOG.info("   ✅ ObjectiveHandler: {}", objectiveHandler != null ? "OK" : "MISSING");
        LOG.info("   ✅ ShieldStrategy: {}", shieldStrategy != null ? "OK" : "MISSING");
        LOG.info("   ✅ DeployPhasePlanner: {}", deployPhasePlanner != null ? "OK" : "MISSING");

        // Test 3: Verify decision tracker
        LOG.info("🔧 Test 3: Decision Safety");
        LOG.info("   ✅ DecisionTracker: {}", decisionTracker != null ? "OK" : "MISSING");
        LOG.info("   ✅ ChatManager: {}", chatManager != null ? "OK" : "MISSING");

        // Test 4: Verify personality system
        LOG.info("🔧 Test 4: Personality System");
        LOG.info("   ✅ AstrogatorPersonality: {}", personality != null ? "OK" : "MISSING");
        LOG.info("   ✅ HolidayOverlay: {} (active: {})",
                holidayOverlay != null ? "OK" : "MISSING",
                holidayOverlay != null ? holidayOverlay.isHolidayActive() : false);

        // Test 5: Configuration values
        LOG.info("🔧 Test 5: Configuration Values");
        LOG.info("   ✅ DEPLOY_THRESHOLD: {}", RandoConfig.DEPLOY_THRESHOLD);
        LOG.info("   ✅ BATTLE_FAVORABLE_THRESHOLD: {}", RandoConfig.BATTLE_FAVORABLE_THRESHOLD);
        LOG.info("   ✅ BATTLE_DANGER_THRESHOLD: {}", RandoConfig.BATTLE_DANGER_THRESHOLD);
        LOG.info("   ✅ CHAOS_PERCENT: {}", RandoConfig.CHAOS_PERCENT);
        LOG.info("   ✅ CHAT_ENABLED: {}", RandoConfig.CHAT_ENABLED);

        LOG.info("========================================");
        LOG.info("🔧 SELF-TESTS COMPLETE - AI READY");
        LOG.info("========================================");
    }

    /**
     * Run game-start verification when a new game begins.
     * Verifies that we can access game state, cards, etc.
     */
    private void runGameStartVerification(String playerId, GameState gameState) {
        LOG.info("========================================");
        LOG.info("🎮 GAME START VERIFICATION");
        LOG.info("========================================");

        // Test 1: Basic game state access
        LOG.info("🎮 Test 1: Game State Access");
        try {
            int forcePile = gameState.getForcePileSize(playerId);
            int reserveDeck = gameState.getReserveDeckSize(playerId);
            int lifeForce = gameState.getPlayerLifeForce(playerId);
            LOG.info("   ✅ Force pile: {}", forcePile);
            LOG.info("   ✅ Reserve deck: {}", reserveDeck);
            LOG.info("   ✅ Life force: {}", lifeForce);
        } catch (Exception e) {
            LOG.error("   ❌ Failed to access game state: {}", e.getMessage());
        }

        // Test 2: Hand access
        LOG.info("🎮 Test 2: Hand Access");
        try {
            List<PhysicalCard> hand = gameState.getHand(playerId);
            LOG.info("   ✅ Hand size: {}", hand.size());

            // Try to get card info from first card
            if (!hand.isEmpty()) {
                PhysicalCard firstCard = hand.get(0);
                LOG.info("   ✅ First card: {} (id={})",
                        firstCard.getTitle(), firstCard.getCardId());

                SwccgCardBlueprint blueprint = firstCard.getBlueprint();
                if (blueprint != null) {
                    LOG.info("   ✅ Blueprint access: OK (category={})",
                            blueprint.getCardCategory());
                } else {
                    LOG.warn("   ⚠️ Blueprint is null for first card");
                }
            }
        } catch (Exception e) {
            LOG.error("   ❌ Failed to access hand: {}", e.getMessage());
        }

        // Test 3: Current game reference
        LOG.info("🎮 Test 3: Game Reference");
        if (currentGame != null) {
            LOG.info("   ✅ Current game reference: OK");

            // Test card lookup by blueprintId
            LOG.info("🎮 Test 3b: Card Blueprint Lookup");
            testBlueprintLookup();
        } else {
            LOG.warn("   ⚠️ Current game reference is NULL - some features may not work");
            LOG.warn("   ⚠️ Card blueprint lookup will not work without game reference");
        }

        // Test 4: Phase detection
        LOG.info("🎮 Test 4: Phase Detection");
        try {
            Phase phase = gameState.getCurrentPhase();
            LOG.info("   ✅ Current phase: {}", phase);
        } catch (Exception e) {
            LOG.error("   ❌ Failed to get current phase: {}", e.getMessage());
        }

        LOG.info("========================================");
        LOG.info("🎮 VERIFICATION COMPLETE - Playing as {} vs {}",
                mySide, opponentName);
        LOG.info("========================================");
    }

    /**
     * Test blueprint lookup capabilities.
     * Since we can't directly look up blueprints by ID, we test with cards from hand/in play.
     */
    private void testBlueprintLookup() {
        if (currentGame == null) {
            LOG.warn("   ⚠️ Cannot test blueprint lookup - no game reference");
            return;
        }

        GameState gameState = currentGame.getGameState();
        if (gameState == null) {
            LOG.warn("   ⚠️ Cannot test blueprint lookup - no game state");
            return;
        }

        // Try to get a card from anywhere in the game to test blueprint access
        PhysicalCard testCard = null;

        // First try cards in play
        for (PhysicalCard card : gameState.getAllPermanentCards()) {
            if (card != null && card.getBlueprint() != null) {
                testCard = card;
                break;
            }
        }

        // If no cards in play yet, try reserve deck
        if (testCard == null && mySide != null) {
            String playerId = currentGame.getPlayer(mySide);
            if (playerId != null) {
                List<PhysicalCard> reserveDeck = gameState.getReserveDeck(playerId);
                if (reserveDeck != null && !reserveDeck.isEmpty()) {
                    testCard = reserveDeck.get(0);
                }
            }
        }

        if (testCard == null) {
            LOG.warn("   ⚠️ No cards available to test blueprint lookup");
            return;
        }

        // Test blueprint property access
        try {
            SwccgCardBlueprint blueprint = testCard.getBlueprint();
            if (blueprint == null) {
                LOG.error("   ❌ Blueprint is NULL for card {}", testCard.getCardId());
                return;
            }

            String title = blueprint.getTitle();
            CardCategory category = blueprint.getCardCategory();
            String blueprintId = testCard.getBlueprintId(true);

            LOG.info("   ✅ Blueprint test card: {} ({})", title, blueprintId);
            LOG.info("   ✅ Category: {}", category);

            // Only get stats for cards that have them (characters, starships, vehicles)
            if (category == CardCategory.CHARACTER || category == CardCategory.STARSHIP ||
                category == CardCategory.VEHICLE) {
                Float destiny = blueprint.getDestiny();
                Float power = blueprint.getPower();
                Float deployCost = blueprint.getDeployCost();
                LOG.info("   ✅ Stats: Destiny={}, Power={}, Deploy={}", destiny, power, deployCost);
            } else {
                // For other cards, just show destiny if available
                try {
                    Float destiny = blueprint.getDestiny();
                    LOG.info("   ✅ Destiny: {}", destiny);
                } catch (Exception e) {
                    LOG.info("   ✅ (Card type {} has no destiny)", category);
                }
            }
            LOG.info("   ✅ Blueprint lookup: WORKING");

        } catch (Exception e) {
            LOG.error("   ❌ Blueprint lookup failed: {}", e.getMessage());
        }
    }

    /**
     * Get the strategy controller for evaluators to use.
     */
    public StrategyController getStrategyController() {
        return strategyController;
    }

    /**
     * Get the objective handler for starting card selection.
     */
    public ObjectiveHandler getObjectiveHandler() {
        return objectiveHandler;
    }

    /**
     * Get the shield strategy for defensive shield decisions.
     */
    public ShieldStrategy getShieldStrategy() {
        return shieldStrategy;
    }

    /**
     * Get the deploy phase planner for holistic deployment decisions.
     */
    public DeployPhasePlanner getDeployPhasePlanner() {
        return deployPhasePlanner;
    }

    // =========================================================================
    // Main Decision Method
    // =========================================================================

    @Override
    public String decide(String playerId, AwaitingDecision decision, GameState gameState) {
        // Build context for this decision
        context = RandoContext.build(playerId, gameState, currentGame);

        String decisionType = decision.getDecisionType() != null ? decision.getDecisionType().name() : "UNKNOWN";
        String decisionText = decision.getText() != null ? decision.getText() : "";
        Phase phase = gameState != null ? gameState.getCurrentPhase() : null;

        LOG.info("[RandoCalAi] decide() called: type={}, phase={}, text='{}'",
            decisionType, phase,
            decisionText.length() > 50 ? decisionText.substring(0, 50) + "..." : decisionText);

        String result = null;
        try {
            // Track game/turn changes for chat
            trackGameState(playerId, gameState);

            // Update decision tracker state for loop detection
            updateDecisionTrackerState(gameState, playerId);

            // Check for loop and handle if detected
            int[] loopCheck = decisionTracker.checkForLoop(decisionType, decisionText, DecisionTracker.LOOP_RANDOMIZE_THRESHOLD);
            boolean inLoop = loopCheck[0] == 1;

            if (inLoop) {
                RandoLogger.loopDetected("In potential loop ({} repeats), checking blocked responses", loopCheck[1]);

                // Check if we should force a different choice or consider conceding
                if (decisionTracker.shouldConsiderConcede()) {
                    RandoLogger.critical("Loop critical threshold reached! Consider conceding.");
                }
            }

            // Get decision parameters for safety fallback
            Map<String, String[]> params = decision.getDecisionParameters();
            String[] actionIds = params != null ? params.get("actionId") : null;
            String[] cardIds = params != null ? params.get("cardId") : null;

            // Maybe apply chaos (random action)
            if (shouldApplyChaos()) {
                RandoLogger.debug("Chaos mode: selecting random action");
                result = super.decide(playerId, decision, gameState);
            } else {
                // Try evaluator system for supported decision types
                String evaluatorResult = tryEvaluators(playerId, decision, gameState);
                if (evaluatorResult != null) {
                    result = evaluatorResult;
                } else {
                    // Fall back to keyword-based heuristics
                    LOG.debug("Evaluators returned null, falling back to heuristics");
                    result = super.decide(playerId, decision, gameState);
                }
            }

            // === SAFETY LAYER 1: Emergency Fallback ===
            // If we still have no result, use emergency response
            // NOTE: Empty string is VALID for pass - only use fallback if result is null
            // or if we got empty string but noPass=true (meaning empty string is invalid)
            boolean mustChoose = false;
            if (params != null) {
                String[] noPassArr = params.get("noPass");
                mustChoose = noPassArr != null && noPassArr.length > 0 && Boolean.parseBoolean(noPassArr[0]);
            }
            boolean needsEmergencyFallback = (result == null) || (result.isEmpty() && mustChoose);

            if (needsEmergencyFallback) {
                LOG.warn("🚨 No result from evaluators or heuristics, using emergency fallback");
                DecisionSafety.SafetyDecision emergency =
                    DecisionSafety.getEmergencyResponse(decision, actionIds, cardIds);
                result = emergency.value;
                LOG.warn("🚨 Emergency response: '{}' ({})", result, emergency.reason);
            }

            // === SAFETY LAYER 2: Response Validation ===
            // Validate the response is actually valid for this decision
            String[] availableOptions = actionIds != null && actionIds.length > 0 ? actionIds : cardIds;
            String[] validated = DecisionSafety.ensureValidResponse(decision, result, availableOptions);
            if (validated[1] != null && !validated[1].isEmpty()) {
                LOG.warn("🚨 Response corrected: {}", validated[1]);
            }
            result = validated[0];

            // Record the decision for loop tracking
            decisionTracker.recordDecision(decisionType, decisionText,
                String.valueOf(decision.getAwaitingDecisionId()), result != null ? result : "");

            // Track strategic events for learning
            trackStrategicEvents(decision, decisionText, result);

            LOG.info("[RandoCalAi] decide() result: '{}' ✅", result != null ? result : "(pass)");
            return result;
        } finally {
            context = null;
        }
    }

    /**
     * Update the decision tracker's state for loop detection.
     */
    private void updateDecisionTrackerState(GameState gameState, String playerId) {
        if (gameState == null) return;

        int handSize = 0;
        int forcePile = 0;
        int reserveDeck = 0;
        int turn = 0;
        int cardsInPlay = 0;

        try {
            handSize = gameState.getHand(playerId).size();
            forcePile = gameState.getForcePileSize(playerId);
            reserveDeck = gameState.getReserveDeckSize(playerId);
            turn = gameState.getPlayersLatestTurnNumber(playerId);

            // Count cards in play
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card != null && card.getZone() != null && card.getZone().isInPlay()) {
                    if (playerId.equals(card.getOwner())) {
                        cardsInPlay++;
                    }
                }
            }
        } catch (Exception e) {
            // Ignore errors in state tracking
        }

        decisionTracker.updateState(handSize, forcePile, reserveDeck, turn, cardsInPlay);
    }

    /**
     * Try to use the evaluator system for this decision.
     *
     * @return result from evaluator, or null if evaluators don't handle this decision
     */
    private String tryEvaluators(String playerId, AwaitingDecision decision, GameState gameState) {
        // Build DecisionContext for evaluators
        DecisionContext evalContext = buildEvaluatorContext(playerId, decision, gameState);
        if (evalContext == null) {
            return null;
        }

        // Check if evaluators can handle this decision
        if (!combinedEvaluator.canHandle(evalContext)) {
            return null;
        }

        // Run evaluators
        EvaluatedAction bestAction = combinedEvaluator.evaluateDecision(evalContext);
        if (bestAction == null) {
            LOG.debug("Evaluators returned no action, falling back to heuristics");
            return null;
        }

        LOG.info("Evaluator decision: {} (score: {})", bestAction.getDisplayText(), bestAction.getScore());
        return bestAction.getActionId();
    }

    /**
     * Build a DecisionContext for evaluators from AwaitingDecision.
     */
    private DecisionContext buildEvaluatorContext(String playerId, AwaitingDecision decision, GameState gameState) {
        if (decision == null) {
            return null;
        }

        AwaitingDecisionType decisionType = decision.getDecisionType();
        if (decisionType == null) {
            return null;
        }

        Phase phase = gameState != null ? gameState.getCurrentPhase() : null;
        DecisionContext evalContext = new DecisionContext(
            gameState,
            playerId,
            decisionType.name(),  // "INTEGER", "CARD_ACTION_CHOICE", etc.
            decision.getText(),
            String.valueOf(decision.getAwaitingDecisionId()),
            phase
        );

        // Parse parameters from decision
        Map<String, String[]> params = decision.getDecisionParameters();
        if (params != null) {
            // For INTEGER decisions
            String[] minVal = params.get("min");
            if (minVal != null && minVal.length > 0) {
                try {
                    evalContext.setMin(Integer.parseInt(minVal[0]));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            String[] maxVal = params.get("max");
            if (maxVal != null && maxVal.length > 0) {
                try {
                    evalContext.setMax(Integer.parseInt(maxVal[0]));
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            // noPass flag
            String[] noPass = params.get("noPass");
            if (noPass != null && noPass.length > 0) {
                evalContext.setNoPass(Boolean.parseBoolean(noPass[0]));
            }

            // For CARD_SELECTION and ARBITRARY_CARDS decisions - parse card IDs
            String[] cardIds = params.get("cardId");
            if (cardIds != null && cardIds.length > 0) {
                List<String> cardIdList = new java.util.ArrayList<>();
                for (String cid : cardIds) {
                    if (cid != null && !cid.isEmpty()) {
                        cardIdList.add(cid);
                    }
                }
                evalContext.setCardIds(cardIdList);
                LOG.debug("Parsed {} card IDs for decision", cardIdList.size());
            }

            // Also parse blueprint IDs if available (for reserve deck selections)
            String[] blueprintIds = params.get("blueprintId");
            if (blueprintIds != null && blueprintIds.length > 0) {
                List<String> bpList = new java.util.ArrayList<>();
                for (String bp : blueprintIds) {
                    if (bp != null && !bp.isEmpty()) {
                        bpList.add(bp);
                    }
                }
                evalContext.setBlueprints(bpList);
                LOG.debug("Parsed {} blueprint IDs for decision", bpList.size());
            }

            // CRITICAL: Parse selectable array - GEMP rejects selection of non-selectable cards!
            String[] selectableArr = params.get("selectable");
            if (selectableArr != null && selectableArr.length > 0) {
                List<Boolean> selectableList = new java.util.ArrayList<>();
                for (String sel : selectableArr) {
                    selectableList.add("true".equalsIgnoreCase(sel));
                }
                evalContext.setSelectable(selectableList);
                int selectableCount = 0;
                for (Boolean s : selectableList) {
                    if (s) selectableCount++;
                }
                LOG.debug("Parsed {} selectable flags ({} selectable)", selectableList.size(), selectableCount);
            }

            // Parse action IDs for CARD_ACTION_CHOICE
            String[] actionIds = params.get("actionId");
            if (actionIds != null && actionIds.length > 0) {
                List<String> actionList = new java.util.ArrayList<>();
                for (String aid : actionIds) {
                    if (aid != null && !aid.isEmpty()) {
                        actionList.add(aid);
                    }
                }
                evalContext.setActionIds(actionList);
                LOG.debug("Parsed {} action IDs for decision", actionList.size());
            }

            // Parse action text for CARD_ACTION_CHOICE
            String[] actionTexts = params.get("actionText");
            if (actionTexts != null && actionTexts.length > 0) {
                List<String> textList = new java.util.ArrayList<>();
                for (String txt : actionTexts) {
                    textList.add(txt != null ? txt : "");
                }
                evalContext.setActionTexts(textList);
            }

            // For CARD_ACTION_CHOICE: parse per-action cardId and blueprintId arrays
            // These tell us which card each action is associated with
            if ("CARD_ACTION_CHOICE".equals(decisionType.name())) {
                // cardId array - each action's associated card ID
                String[] actionCardIds = params.get("cardId");
                if (actionCardIds != null && actionCardIds.length > 0) {
                    List<String> cardIdList = new java.util.ArrayList<>();
                    for (String cid : actionCardIds) {
                        cardIdList.add(cid != null ? cid : "");
                    }
                    evalContext.setCardIds(cardIdList);
                    LOG.debug("Parsed {} action-associated card IDs", cardIdList.size());
                }

                // blueprintId array - "inPlay" or actual blueprint for virtual actions
                String[] actionBlueprintIds = params.get("blueprintId");
                if (actionBlueprintIds != null && actionBlueprintIds.length > 0) {
                    List<String> bpList = new java.util.ArrayList<>();
                    for (String bp : actionBlueprintIds) {
                        bpList.add(bp != null ? bp : "");
                    }
                    evalContext.setBlueprints(bpList);
                    LOG.debug("Parsed {} action-associated blueprint IDs", bpList.size());
                }
            }
        }

        // Set blocked responses for loop prevention
        // This allows evaluators to penalize previously-cancelled actions
        String decisionText = decision.getText() != null ? decision.getText() : "";
        Set<String> blocked = decisionTracker.getBlockedResponses(decisionType.name(), decisionText);
        if (!blocked.isEmpty()) {
            evalContext.setBlockedResponses(blocked);
            LOG.debug("🚫 {} blocked responses for this decision", blocked.size());
        }

        // Set game context for advanced analysis
        evalContext.setGame(currentGame);
        evalContext.setSide(mySide);

        // Set strategy components so evaluators can use them
        evalContext.setStrategyController(strategyController);
        evalContext.setObjectiveHandler(objectiveHandler);
        evalContext.setShieldStrategy(shieldStrategy);
        evalContext.setDeployPhasePlanner(deployPhasePlanner);

        return evalContext;
    }

    /**
     * Set the current game reference for advanced analysis.
     * Called by game mediator before decisions.
     */
    public void setCurrentGame(SwccgGame game) {
        this.currentGame = game;
    }

    /**
     * Get pending chat message if available.
     * @return message to send, or null
     */
    public String getChatMessage() {
        return chatManager.getNextMessage();
    }

    // =========================================================================
    // Overridden Scoring Methods
    // =========================================================================

    @Override
    protected int getPassPenalty() {
        return RandoConfig.SCORE_PENALTY_PASS;
    }

    @Override
    protected boolean shouldSkipOptionalResponses() {
        return false;  // Rando Cal handles optional responses
    }

    @Override
    protected int scoreActionContext(String playerId, GameState gameState, String decisionText,
            String actionText, Phase phase, Map<String, String[]> params) {

        if (context == null || actionText == null || actionText.isEmpty()) {
            return 0;
        }

        int score = 0;
        String actionLower = actionText.toLowerCase(Locale.ROOT);

        // =====================================================================
        // Deploy Phase Scoring
        // =====================================================================
        if (phase == Phase.DEPLOY) {
            score += scoreDeployAction(actionLower, decisionText);
        }

        // =====================================================================
        // Control Phase Scoring
        // =====================================================================
        if (phase == Phase.CONTROL) {
            score += scoreControlAction(actionLower, decisionText);
        }

        // =====================================================================
        // Battle Phase Scoring
        // =====================================================================
        if (phase == Phase.BATTLE) {
            score += scoreBattleAction(actionLower, decisionText);
        }

        // =====================================================================
        // Priority Card Handling
        // =====================================================================
        score += scorePriorityCards(actionLower, decisionText);

        // =====================================================================
        // Situational Adjustments
        // =====================================================================

        // Desperate play when behind on life force
        if (context.behindOnLifeForce()) {
            if (actionLower.contains("force drain") || actionLower.contains("initiate battle")) {
                score += 40;
            }
        }

        // Aggressive when ahead
        if (context.aheadOnBoard()) {
            if (actionLower.contains("initiate battle")) {
                score += 30;
            }
        }

        // Conservative when behind on board
        if (context.behindOnBoard()) {
            if (actionLower.contains("initiate battle")) {
                score -= 30;
            }
            if (actionLower.contains("deploy") || actionLower.contains("draw")) {
                score += 20;
            }
        }

        // Hand title matching (like AdvancedAi)
        if (context.matchesHandTitle(actionLower)) {
            score += 60;
        }

        return score;
    }

    // =========================================================================
    // Phase-Specific Scoring
    // =========================================================================

    private int scoreDeployAction(String actionText, String decisionText) {
        int score = 0;

        // Deploying locations is high priority (opens options)
        if (actionText.contains("deploy") && actionText.contains("location")) {
            score += RandoConfig.SCORE_DEPLOY_LOCATION;
        }

        // Use board analyzer if game is available
        if (currentGame != null && context != null && mySide != null) {
            List<LocationAnalysis> losingLocations = AiBoardAnalyzer.getLosingLocations(
                currentGame, context.playerId, context.opponentId, mySide);

            // Bonus for deploying to locations where we're losing
            if (!losingLocations.isEmpty()) {
                for (LocationAnalysis loc : losingLocations) {
                    String locName = loc.location.getTitle();
                    if (locName != null && actionText.contains(locName.toLowerCase(Locale.ROOT))) {
                        score += RandoConfig.SCORE_REINFORCE_LOSING;

                        // Extra bonus based on how badly we're losing (use power advantage)
                        float powerDiff = loc.getPowerAdvantage();
                        if (powerDiff < -5) {
                            score += 15;  // Critical location
                        }

                        // Battleground locations are higher priority
                        if (loc.isBattleground) {
                            score += 10;
                        }
                        break;
                    }
                }
            }

            // Check for deploying to opponent-only locations (gain ground)
            List<LocationAnalysis> opponentOnly = AiBoardAnalyzer.getOpponentOnlyLocations(
                currentGame, context.playerId, context.opponentId, mySide);

            for (LocationAnalysis loc : opponentOnly) {
                String locName = loc.location.getTitle();
                if (locName != null && actionText.contains(locName.toLowerCase(Locale.ROOT))) {
                    // Only if location has opponent force icons (worth fighting for)
                    if (loc.theirForceIcons > 0) {
                        score += RandoConfig.SCORE_GAIN_GROUND;

                        // More valuable if battleground (can force drain after control)
                        if (loc.isBattleground) {
                            score += 15;
                        }

                        // Lower priority if they have much more power there
                        if (loc.theirPower > 8) {
                            score -= 10;  // Risky deploy
                        }
                    }
                    break;
                }
            }

            // Domain-specific bonuses for characters vs starships
            boolean isStarshipDeploy = actionText.contains("starship") || actionText.contains("ship");
            boolean isCharacterDeploy = actionText.contains("character") ||
                (actionText.contains("deploy") && !isStarshipDeploy && !actionText.contains("vehicle"));

            for (LocationAnalysis loc : AiBoardAnalyzer.analyzeAllLocations(
                    currentGame, context.playerId, context.opponentId, mySide)) {
                String locName = loc.location != null ? loc.location.getTitle() : null;
                if (locName == null || !actionText.contains(locName.toLowerCase(Locale.ROOT))) {
                    continue;
                }

                // Match domain: characters go to ground, starships to space
                if (isCharacterDeploy && loc.isGround()) {
                    score += 5;
                } else if (isStarshipDeploy && loc.isSpace()) {
                    score += 5;
                }

                // Avoid deploying to empty uncontested locations (wasteful)
                if (loc.status == ContestStatus.EMPTY && loc.ourForceIcons == 0) {
                    score -= 20;  // Discourage wasteful deploys
                }
                break;
            }
        }

        // Matching pilot bonus
        if (actionText.contains("pilot") && actionText.contains("matching")) {
            score += RandoConfig.SCORE_MATCHING_PILOT;
        }

        return score;
    }

    private int scoreControlAction(String actionText, String decisionText) {
        int score = 0;

        // Force drain is primary control phase action
        if (actionText.contains("force drain")) {
            score += RandoConfig.SCORE_FORCE_DRAIN;

            // Extra bonus if we control battlegrounds
            if (currentGame != null && context != null && mySide != null) {
                List<LocationAnalysis> controlled = AiBoardAnalyzer.getControlledBattlegrounds(
                    currentGame, context.playerId, context.opponentId, mySide);
                if (!controlled.isEmpty()) {
                    score += 20 * controlled.size();
                }
            }
        }

        return score;
    }

    private int scoreBattleAction(String actionText, String decisionText) {
        int score = 0;

        if (actionText.contains("initiate battle")) {
            // Check if battle would be favorable
            if (currentGame != null && context != null && mySide != null) {
                // Find which location this battle is at
                for (LocationAnalysis loc : AiBoardAnalyzer.analyzeAllLocations(
                        currentGame, context.playerId, context.opponentId, mySide)) {
                    String locName = loc.location != null ? loc.location.getTitle() : null;
                    if (locName == null) continue;

                    // Check if this action mentions this location
                    if (!actionText.contains(locName.toLowerCase(Locale.ROOT))) {
                        continue;
                    }

                    // Use LocationAnalysis to determine if battle is favorable
                    float powerAdvantage = loc.getPowerAdvantage();

                    if (powerAdvantage >= RandoConfig.BATTLE_FAVORABLE_THRESHOLD) {
                        score += RandoConfig.SCORE_INITIATE_BATTLE;

                        // Extra bonus for big power advantage (likely to win)
                        if (powerAdvantage >= 8) {
                            score += 20;
                        }

                        // Battlegrounds are more valuable to fight at
                        if (loc.isBattleground) {
                            score += 10;
                        }
                    } else if (powerAdvantage <= RandoConfig.BATTLE_DANGER_THRESHOLD) {
                        score -= 60;  // Avoid unfavorable battles
                    } else {
                        // Close battle - moderate bonus
                        score += 20;
                    }

                    // If contested and we're winning, definitely fight
                    if (loc.isContested() && loc.status == ContestStatus.WINNING) {
                        score += 25;
                    }
                    break;
                }

                // Fallback to overall board advantage if no specific location found
                if (score == 0) {
                    float boardAdvantage = AiBoardAnalyzer.calculateBoardAdvantage(
                        currentGame, context.playerId, context.opponentId, mySide);

                    if (boardAdvantage >= RandoConfig.BATTLE_FAVORABLE_THRESHOLD) {
                        score += RandoConfig.SCORE_INITIATE_BATTLE;
                    } else if (boardAdvantage <= RandoConfig.BATTLE_DANGER_THRESHOLD) {
                        score -= 60;  // Avoid unfavorable battles
                    }
                }
            }
        }

        // Weapon firing
        if (actionText.contains("fire") && actionText.contains("weapon")) {
            score += 50;
        }

        return score;
    }

    private int scorePriorityCards(String actionText, String decisionText) {
        int score = 0;

        // Damage cancel cards (Houjix/Ghhhk) - very high priority when appropriate
        if (actionText.contains("houjix") || actionText.contains("ghhhk")) {
            if (decisionText.contains("battle damage") || decisionText.contains("cancel")) {
                score += RandoConfig.SCORE_DAMAGE_CANCEL;
            }
        }

        // Barrier usage
        if (actionText.contains("barrier")) {
            if (decisionText.contains("deploy") || decisionText.contains("character")) {
                score += RandoConfig.SCORE_BARRIER_USE;
            }
        }

        // Sense usage
        if (actionText.contains("sense") && actionText.contains("cancel")) {
            // Check if target is worth sensing
            AiPriorityCards.SenseTargetResult senseResult =
                AiPriorityCards.getSenseTargetValue(decisionText);
            if (senseResult.isHighValue) {
                score += senseResult.score;
            } else {
                score += RandoConfig.SCORE_SENSE_USE / 2;
            }
        }

        return score;
    }

    // =========================================================================
    // Weight Implementations
    // =========================================================================

    @Override
    protected KeywordWeight[] getActionWeights() {
        return ACTION_WEIGHTS;
    }

    @Override
    protected KeywordWeight[] getActionPenalties() {
        return ACTION_PENALTIES;
    }

    @Override
    protected KeywordWeight[] getChoiceWeights() {
        return CHOICE_WEIGHTS;
    }

    @Override
    protected KeywordWeight[] getChoicePenalties() {
        return CHOICE_PENALTIES;
    }

    @Override
    protected String[] getCardHints() {
        return CARD_HINTS;
    }

    // =========================================================================
    // Game State Tracking
    // =========================================================================

    private void trackGameState(String playerId, GameState gameState) {
        if (gameState == null) return;

        // Detect new game by checking if side/opponent changed
        Side newSide = gameState.getSide(playerId);
        String newOpponent = gameState.getOpponent(playerId);

        // New game started (opponent or side changed)
        if (mySide == null || !newOpponent.equals(opponentName)) {
            lastTurn = -1;
            mySide = newSide;
            opponentName = newOpponent;
            currentGameId = playerId + "_" + System.currentTimeMillis();

            chatManager.resetForGame(currentGameId);
            decisionTracker.clear();  // Clear loop tracking for new game
            seenOpponentCards.clear();  // Clear opponent card tracking

            // Reset and update strategy components with new side
            strategyController.setSide(mySide);
            strategyController.reset();
            objectiveHandler.reset();
            shieldStrategy.setSide(mySide);
            shieldStrategy.reset();
            deployPhasePlanner.reset();
            LOG.debug("[RandoCalAi] All strategy components reset for new game as {} side", mySide);

            // Run game-start verification
            runGameStartVerification(playerId, gameState);

            // Queue welcome message
            if (personality != null && RandoConfig.CHAT_ENABLED) {
                String welcome = personality.getWelcomeMessage(opponentName, mySide);
                if (holidayOverlay != null && holidayOverlay.isHolidayActive()) {
                    welcome = holidayOverlay.getGreeting().orElse(welcome);
                }
                chatManager.queueWelcome(welcome);
            }

            LOG.info("New game started vs {} as {}", opponentName, mySide);
        }

        // Turn changed
        int currentTurn = gameState.getPlayersLatestTurnNumber(playerId);
        if (currentTurn > lastTurn) {
            lastTurn = currentTurn;
            chatManager.setCurrentTurn(currentTurn);
            strategyController.startNewTurn(currentTurn);
            LOG.debug("[StrategyController] Starting turn {}, phase={}", currentTurn, strategyController.getPhase());

            // Queue turn message (on opponent's turn, when it's not our turn to act)
            String currentPlayer = gameState.getCurrentPlayerId();
            boolean isOpponentsTurn = currentPlayer != null && !currentPlayer.equals(playerId);
            if (personality != null && RandoConfig.CHAT_ENABLED
                    && currentTurn >= 2 && isOpponentsTurn) {
                String turnMessage = personality.getTurnMessage(currentTurn, context);
                if (turnMessage != null) {
                    chatManager.queueTurnMessage(turnMessage);
                }
            }

            // Confirm any pending deploy from last turn succeeded (strategy learning)
            if (lastPendingDeployType != null) {
                strategyController.onSuccessfulDeploy(lastPendingDeployType);
                lastPendingDeployType = null;
            }
        }

        // Track opponent cards for situational shield decisions
        trackOpponentCards(gameState, newOpponent);
    }

    /**
     * Track opponent cards as they appear for strategic decisions.
     * ShieldStrategy uses this to trigger situational shields.
     */
    private void trackOpponentCards(GameState gameState, String opponentId) {
        if (gameState == null || opponentId == null) return;

        try {
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;
                if (!opponentId.equals(card.getOwner())) continue;

                Zone zone = card.getZone();
                if (zone == null || !zone.isInPlay()) continue;

                String title = card.getTitle();
                if (title == null) continue;

                // Check if this is a new card we haven't seen
                String cardKey = card.getCardId() + "_" + title;
                if (!seenOpponentCards.contains(cardKey)) {
                    seenOpponentCards.add(cardKey);

                    // Notify shield strategy about opponent card
                    shieldStrategy.recordOpponentCard(title);

                    // Check for opponent objective
                    SwccgCardBlueprint blueprint = card.getBlueprint();
                    if (blueprint != null && blueprint.getCardCategory() == CardCategory.OBJECTIVE) {
                        shieldStrategy.setOpponentObjective(title);
                        LOG.info("Detected opponent objective: {}", title);
                    }

                    // Check for opponent defensive shields
                    if (blueprint != null && blueprint.getCardCategory() == CardCategory.DEFENSIVE_SHIELD) {
                        String blueprintId = card.getBlueprintId(true);
                        shieldStrategy.recordOpponentShield(blueprintId, title);
                    }
                }
            }
        } catch (Exception e) {
            LOG.debug("Error tracking opponent cards: {}", e.getMessage());
        }
    }

    /**
     * Track strategic events from decisions for strategy learning.
     * - Deploy decisions: Track for focus confidence
     * - Battle results: Track wins/losses
     */
    private void trackStrategicEvents(AwaitingDecision decision, String decisionText, String result) {
        if (decision == null || result == null || result.isEmpty()) return;

        String textLower = decisionText != null ? decisionText.toLowerCase(Locale.ROOT) : "";

        // Track deploy decisions - we'll confirm success on next turn
        if (textLower.contains("deploy")) {
            // Determine card type from decision text
            if (textLower.contains("starship") || textLower.contains("capital ship")) {
                lastPendingDeployType = "starship";
            } else if (textLower.contains("vehicle")) {
                lastPendingDeployType = "vehicle";
            } else if (textLower.contains("character") || textLower.contains("alien") ||
                       textLower.contains("droid") || textLower.contains("jedi") ||
                       textLower.contains("imperial") || textLower.contains("rebel")) {
                lastPendingDeployType = "character";
            } else if (textLower.contains("site") || textLower.contains("system")) {
                lastPendingDeployType = "location";
            }
        }

        // Track battle results from decision text
        // Battle result prompts typically contain "won" or "lost" or "battle damage"
        if (textLower.contains("battle")) {
            if (textLower.contains("you won") || textLower.contains("you have won")) {
                strategyController.onBattleResult(true);
                LOG.debug("Battle won - updating strategy controller");
            } else if (textLower.contains("you lost") || textLower.contains("you have lost")) {
                strategyController.onBattleResult(false);
                LOG.debug("Battle lost - updating strategy controller");
            }
        }
    }

    private boolean shouldApplyChaos() {
        return random.nextInt(100) < RandoConfig.CHAOS_PERCENT;
    }

    // =========================================================================
    // Context Class
    // =========================================================================

    /**
     * Decision context with board analysis.
     */
    private static final class RandoContext {
        final String playerId;
        final String opponentId;
        final int selfLifeForce;
        final int opponentLifeForce;
        final int selfUnitsInPlay;
        final int opponentUnitsInPlay;
        final Set<String> handTitles;
        final float boardAdvantage;

        private RandoContext(String playerId, String opponentId, int selfLifeForce,
                int opponentLifeForce, int selfUnitsInPlay, int opponentUnitsInPlay,
                Set<String> handTitles, float boardAdvantage) {
            this.playerId = playerId;
            this.opponentId = opponentId;
            this.selfLifeForce = selfLifeForce;
            this.opponentLifeForce = opponentLifeForce;
            this.selfUnitsInPlay = selfUnitsInPlay;
            this.opponentUnitsInPlay = opponentUnitsInPlay;
            this.handTitles = handTitles;
            this.boardAdvantage = boardAdvantage;
        }

        static RandoContext build(String playerId, GameState gameState, SwccgGame game) {
            if (playerId == null || gameState == null) {
                return null;
            }

            String opponent = gameState.getOpponent(playerId);
            int selfLifeForce = safeLifeForce(gameState, playerId);
            int opponentLifeForce = opponent != null ? safeLifeForce(gameState, opponent) : selfLifeForce;

            int selfUnits = countUnitsInPlay(gameState, playerId);
            int opponentUnits = opponent != null ? countUnitsInPlay(gameState, opponent) : selfUnits;

            Set<String> handTitles = new HashSet<>();
            buildHandTitles(gameState, playerId, handTitles);

            float boardAdvantage = 0;
            if (game != null) {
                Side side = gameState.getSide(playerId);
                boardAdvantage = AiBoardAnalyzer.calculateBoardAdvantage(game, playerId, opponent, side);
            }

            return new RandoContext(playerId, opponent, selfLifeForce, opponentLifeForce,
                selfUnits, opponentUnits, handTitles, boardAdvantage);
        }

        private static int safeLifeForce(GameState gameState, String playerId) {
            try {
                return gameState.getPlayerLifeForce(playerId);
            } catch (RuntimeException e) {
                return 0;
            }
        }

        private static void buildHandTitles(GameState gameState, String playerId, Set<String> handTitles) {
            try {
                for (PhysicalCard card : gameState.getHand(playerId)) {
                    if (card == null) continue;
                    for (String title : card.getTitles()) {
                        if (title != null && title.length() >= 4) {
                            handTitles.add(title.toLowerCase(Locale.ROOT).trim());
                        }
                    }
                }
            } catch (RuntimeException e) {
                // Ignore
            }
        }

        private static int countUnitsInPlay(GameState gameState, String playerId) {
            if (playerId == null || gameState == null) return 0;
            int count = 0;
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;
                Zone zone = card.getZone();
                if (zone == null || !zone.isInPlay()) continue;
                if (!playerId.equals(card.getOwner())) continue;

                SwccgCardBlueprint blueprint = card.getBlueprint();
                if (blueprint == null) continue;
                CardCategory category = blueprint.getCardCategory();
                if (category == CardCategory.CHARACTER || category == CardCategory.STARSHIP
                        || category == CardCategory.VEHICLE) {
                    count++;
                }
            }
            return count;
        }

        boolean matchesHandTitle(String text) {
            for (String title : handTitles) {
                if (text.contains(title)) return true;
            }
            return false;
        }

        boolean behindOnBoard() {
            return selfUnitsInPlay + 1 < opponentUnitsInPlay;
        }

        boolean aheadOnBoard() {
            return selfUnitsInPlay > opponentUnitsInPlay + 1;
        }

        boolean behindOnLifeForce() {
            return selfLifeForce + 5 < opponentLifeForce;
        }
    }
}
