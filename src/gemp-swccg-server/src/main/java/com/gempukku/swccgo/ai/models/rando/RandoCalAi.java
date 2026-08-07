package com.gempukku.swccgo.ai.models.rando;

import com.gempukku.swccgo.ai.models.HeuristicAiBase;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.ContestStatus;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.LocationAnalysis;
import com.gempukku.swccgo.ai.common.AiChatManager;
import com.gempukku.swccgo.ai.models.rando.evaluators.ActionType;
import com.gempukku.swccgo.ai.models.rando.evaluators.CombinedEvaluator;
import com.gempukku.swccgo.ai.models.rando.evaluators.DecisionContext;
import com.gempukku.swccgo.ai.models.rando.evaluators.EvaluatedAction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhaseScript;
import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer;
// V295 RETIRED: import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveHandler;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.ai.models.rando.strategy.StrategyController;
import com.gempukku.swccgo.ai.models.common.phase.ActivateDecisionRouting;
import com.gempukku.swccgo.ai.models.common.phase.AiActionSourceProvenance;
import com.gempukku.swccgo.ai.models.common.phase.BattleActionTextPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BattleWeaponsPolicy;
import com.gempukku.swccgo.ai.models.common.phase.BhbmForceDripUrgencyFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.CaptureDeployBudgetFactsReader;
import com.gempukku.swccgo.ai.models.common.phase.ControlDrainAssessment;
import com.gempukku.swccgo.ai.models.common.phase.CoordinatorPosturePolicy;
import com.gempukku.swccgo.ai.models.common.phase.DeployActionTextPolicy;
import com.gempukku.swccgo.ai.models.common.phase.MovePhysicalCardResolver;
import com.gempukku.swccgo.ai.models.common.phase.ResponsePolicy;
import com.gempukku.swccgo.ai.models.common.phase.SetupPolicy;
import com.gempukku.swccgo.ai.models.common.phase.ShieldPolicy;
import com.gempukku.swccgo.ai.models.common.phase.TdigwattObjectiveFactsReader;
import com.gempukku.swccgo.ai.models.common.trace.NoOpTraceSink;
import com.gempukku.swccgo.ai.models.common.trace.TraceCaptureFailure;
import com.gempukku.swccgo.ai.models.common.trace.TraceRoute;
import com.gempukku.swccgo.ai.models.common.trace.TraceSession;
import com.gempukku.swccgo.ai.models.common.trace.TraceSink;
import com.gempukku.swccgo.ai.models.common.trace.TraceSnapshots;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerLifecycleSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.DecisionTrackerSnapshot;
import com.gempukku.swccgo.ai.models.common.trace.state.EngineCallOutcome;
import com.gempukku.swccgo.ai.models.common.trace.state.PendingConcedeEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.PendingDeployEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerClearEvent;
import com.gempukku.swccgo.ai.models.common.trace.state.TrackerOwner;
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
// V295 RETIRED: import java.util.Random;
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

    // V295 RETIRED: private final ObjectiveHandler objectiveHandler;
    // Objective intelligence for phase and flip-aware scoring
    private final ObjectiveAnalyzer objectiveAnalyzer;

    // Shield strategy for defensive shields
    private final ShieldStrategy shieldStrategy;

    // Deploy phase planner for holistic deployment plans
    private final DeployPhasePlanner deployPhasePlanner;

    // V67ax DEPLOY PHASE SCRIPT (deterministic step ordering)
    private final DeployPhaseScript deployPhaseScript;

    // V22.6: DeckOracle for full deck knowledge
    private final com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle deckOracle;

    // V24.7: OpponentDeckTracker for destiny intel from deck peeks
    private final com.gempukku.swccgo.ai.models.rando.strategy.OpponentDeckTracker opponentDeckTracker;

    // Personality system (will be set via setter after construction)
    private AstrogatorPersonality personality;
    private HolidayOverlay holidayOverlay;

    // Game context (rebuilt each decision)
    private RandoContext context;
    private SwccgGame currentGame;
    private final ActivateDecisionRouting.AmountLatch activationAmountLatch =
        new ActivateDecisionRouting.AmountLatch();
    // V295 RETIRED: private Random random = new Random();

    // State tracking
    private String currentGameId;
    private int lastTurn = -1;
    private Phase lastPhase;  // Track phase for battle message detection
    private boolean battleMessageSentThisBattle = false;  // Track if we already sent a battle message
    private boolean gameEndMessageSent = false;  // Track if game end message was sent
    // V67aw (Steve, 2026-05-08): Concede defer flag.
    // Steve's rule: 'Change Rando's Concede logic to only happen after the
    // next battle phase has ended.' When concede conditions trigger
    // (Lost-Pile deficit ≥ 30, etc.), set this flag instead of conceding
    // immediately. trackGameState fires the actual concede when the next
    // BATTLE → other-phase transition occurs.
    private boolean pendingConcede = false;
    private String pendingConcedeReason = null;
    private Side mySide;
    private String opponentName;

    // Opponent tracking for strategy components
    private final Set<String> seenOpponentCards = new HashSet<>();
    // Own shield tracking for shield pacing
    private final Set<String> seenOwnShields = new HashSet<>();
    private String lastPendingDeployType = null;  // Track pending deploy for confirmation
    private Integer pendingMovePhysicalCardId;
    private Integer pendingMoveActionSourcePermanentCardId;
    private Integer pendingHiddenPathCorridorDestinationPermanentCardId;
    private Integer pendingTdigwattLandoPermanentCardId;
    private Integer pendingTdigwattActionSourcePermanentCardId;
    private Integer pendingTdigwattDestinationPermanentCardId;
    private Integer pendingObjectiveDeployingCardId;
    private Integer pendingDeployActionSourcePermanentCardId;
    private Integer pendingOnTheVergeVaderActionSourcePermanentCardId;

    // Bot stats DAO for record lookups (optional - set via setter)
    private com.gempukku.swccgo.db.BotStatsDAO botStatsDAO;

    // TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
    // "Trace ownership"): the bot entry point owns the per-decide() trace session — route
    // id, full frozen input, fallback/emergency path, final response. Production default
    // = NoOpTraceSink: no session is ever opened, every trace call short-circuits on a
    // thread-local null check, and behavior is byte-identical to un-instrumented code.
    // Capture stays DISABLED until the contract's landing increment 6 gate passes.
    private TraceSink decisionTraceSink = NoOpTraceSink.INSTANCE;

    /** TRACE ORACLE V2: package-visible pure-harness seam (JUnit only; production never calls). */
    void setDecisionTraceSinkForTesting(TraceSink sink) {
        this.decisionTraceSink = (sink != null) ? sink : NoOpTraceSink.INSTANCE;
    }

    // V29.15: Deck name for saga-aware Epic Event choices
    private String deckName;

    // =========================================================================
    // Keyword Weights - Higher than AdvancedAi for more aggressive play
    // =========================================================================
    // ═══════════════════════════════════════════════════════════
    // ═══ SECTION: SVC-SAFETY (reorg 2026-07-06) ═══
    // OWNED BY: SVC-SAFETY — the LEGACY FALLBACK BRAIN (int scale ~50-200).
    // FROZEN until reorg T4: editing weights or canEvaluate routing silently
    // moves decisions between brains (audit cross-brain-6).
    // Owns: ACTION_WEIGHTS / ACTION_PENALTIES / CHOICE_WEIGHTS / CHOICE_PENALTIES /
    //   CARD_HINTS keyword tables. Hub: none. KIND: BANDED (int scale 50-200,
    //   separate from the evaluators' float scale — do NOT compare across brains).
    // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
    // Cross-refs: SVC-SAFETY decide() pipeline (routes here only when no evaluator
    //   canEvaluate), DEPLOY-1/SVC-SAFETY in CombinedEvaluator (the primary brain).
    //   See resources/RANDO_REORG_PLAN_2026-07-02.md §3 + Rando_Section_Manifest_2026-07-06.xlsx.
    // ═══════════════════════════════════════════════════════════

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
        // V295 RETIRED: this.objectiveHandler = new ObjectiveHandler();
        this.objectiveAnalyzer = new ObjectiveAnalyzer();
        this.shieldStrategy = new ShieldStrategy();
        this.deployPhasePlanner = new DeployPhasePlanner();
        this.deployPhaseScript = new DeployPhaseScript();
        this.deckOracle = new com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle();
        this.opponentDeckTracker = new com.gempukku.swccgo.ai.models.rando.strategy.OpponentDeckTracker();
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
        // V295 RETIRED: LOG.info("   ✅ ObjectiveHandler: {}", objectiveHandler != null ? "OK" : "MISSING");
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
        // V295 RETIRED: LOG.info("   ✅ CHAOS_PERCENT: {}", RandoConfig.CHAOS_PERCENT);
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

    // V295 RETIRED: public ObjectiveHandler getObjectiveHandler() {
    // V295 RETIRED:     return objectiveHandler;
    // V295 RETIRED: }

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
    // ═══════════════════════════════════════════════════════════
    // ═══ SECTION: SVC-SAFETY (reorg 2026-07-06) ═══
    // Owns: the decide() pipeline's safety lanes — loop-block (DecisionTracker loop
    //   detection here; the V163/V167/V169 veto trio itself lives in
    //   ActionTextEvaluator, magnitudes frozen), revert handling, concede
    //   (V25 deficit >= 30, V67aw defer-until-after-battle), emergency/fallback
    //   response lanes. Hub: none. KIND mix: VETO + ORDERING; key magnitudes:
    //   loop veto -100000-class, concede trigger Lost-Pile deficit 30.
    // Absorbs (dead, commented below/nearby — revert path, do not delete): none.
    // Cross-refs: SVC-SAFETY keyword tables above (legacy fallback brain, frozen),
    //   DEPLOY-1 + SVC-SAFETY in CombinedEvaluator (primary scoring brain this
    //   pipeline dispatches to). See resources/RANDO_REORG_PLAN_2026-07-02.md §3
    //   + Rando_Section_Manifest_2026-07-06.xlsx.
    // ═══════════════════════════════════════════════════════════

    @Override
    public String decide(String playerId, AwaitingDecision decision, GameState gameState) {
        // Build context for this decision
        context = RandoContext.build(playerId, gameState, currentGame);

        String decisionType = decision.getDecisionType() != null ? decision.getDecisionType().name() : "UNKNOWN";
        String decisionText = decision.getText() != null ? decision.getText() : "";
        Phase phase = gameState != null ? gameState.getCurrentPhase() : null;
        int activationTurn = gameState != null
            ? gameState.getPlayersLatestTurnNumber(playerId) : 0;
        boolean activationAmountDecision = activationAmountLatch.consume(
            gameState, playerId, activationTurn, phase, decisionType);

        LOG.info("[RandoCalAi] decide() called: type={}, phase={}, text='{}'",
            decisionType, phase,
            decisionText.length() > 50 ? decisionText.substring(0, 50) + "..." : decisionText);

        // TRACE ORACLE V2 (2026-07-13, CODEX_TRACE_ORACLE_V2_CONTRACT "Trace ownership"):
        // open the bot-boundary session — full frozen raw input + shadow snapshot — when
        // the sink is enabled. OBSERVATION ONLY per the route map's shadow authority:
        // no interceptor return moves, no extra RNG draw, no behavior change. Production
        // default sink is disabled, so this whole block no-ops.
        boolean traceOpened = false;
        try {
            if (decisionTraceSink.isEnabled()) {
                traceOpened = openDecisionTraceSession(playerId, decision, gameState,
                    decisionType, decisionText, phase);
            }
        } catch (Throwable traceT) {
            traceOpened = false;
        }

        String result = null;
        try {
            // Track game/turn changes for chat
            trackGameState(playerId, gameState);

            // V25: AUTO-CONCEDE when losing by 30+ in Lost Pile
            // When the deficit is this large, the game is unwinnable. Conceding saves time
            // for both players instead of dragging out a lost game.
            //
            // V67aw (Steve, 2026-05-08): DEFER concede until after the next battle phase.
            // Steve's rule: 'Change Rando's Concede logic to only happen after the next
            // battle phase has ended.' Reasons: lets the current turn's planned battle
            // play out, lets opponent finish their attack cleanly, and avoids mid-decision
            // concedes that look glitchy. The actual concede fires in trackGameState when
            // the BATTLE → other-phase transition is observed.
            if (gameState != null && currentGame != null && !pendingConcede) {
                try {
                    String opponentId = gameState.getOpponent(playerId);
                    if (opponentId != null) {
                        int myLostPile = gameState.getLostPile(playerId).size();
                        int opponentLostPile = gameState.getLostPile(opponentId).size();
                        int lostPileDeficit = myLostPile - opponentLostPile;
                        if (lostPileDeficit >= 30) {
                            boolean tracePendingBefore = pendingConcede;  // structurally false (!pendingConcede guard)
                            String traceReasonBefore = pendingConcedeReason;
                            pendingConcede = true;
                            pendingConcedeReason = String.format(
                                "Lost Pile deficit %d (mine=%d, opponent=%d)",
                                lostPileDeficit, myLostPile, opponentLostPile);
                            LOG.warn("V67aw CONCEDE PENDING: {} — will concede after next battle phase ends",
                                pendingConcedeReason);
                            // TRACE 4A1: typed SET_PENDING observed AFTER the legacy writes,
                            // with the lost-pile inputs and exact before/after.
                            if (traceOpened) {
                                TraceSession.recordPendingConcede(
                                    PendingConcedeEvent.Operation.SET_PENDING,
                                    PendingConcedeEvent.Cause.LOST_PILE_DEFICIT, playerId,
                                    myLostPile, opponentLostPile, lostPileDeficit,
                                    tracePendingBefore, traceReasonBefore,
                                    pendingConcede, pendingConcedeReason);
                            }
                        }
                    }
                } catch (Exception e) {
                    LOG.debug("V67aw CONCEDE PENDING: Error checking lost pile: {}", e.getMessage());
                }
            }

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
            ResponsePolicy.Route responseRoute = ResponsePolicy.classify(
                    decisionType, decisionText);

            // V45: NEVER forfeit when all cards are immune to attrition
            {
                if (responseRoute == ResponsePolicy.Route.OPTIONAL_FORFEIT) {
                    LOG.warn("V45 IMMUNE FORFEIT: All cards immune — PASSING on optional forfeit! Text: '{}'", decisionText);
                    // TRACE ORACLE V2: route + final-response observation ONLY; the direct
                    // return below is untouched (it skips the common finalizer — recorded).
                    if (traceOpened) {
                        TraceSession.recordRoute(TraceRoute.V45_OPTIONAL_FORFEIT,
                            "decision text contains 'forfeit' + 'if desired'", null);
                        // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                        TraceSession.recordEvaluatorLaneNotApplicable(
                            "direct interceptor V45: evaluator lane never runs on this route");
                        TraceSession.recordFinalResponse("", true);
                    }
                    return "";  // Empty = select nothing = pass
                }
            }

            // V44/V67j: ALWAYS accept revert requests — never block the opponent
            // from reverting. Steve's rule: "Rando must always allow a revert. If
            // the gemp game has an error, I need to be able to always revert."
            // V67j: Don't assume index 0 = Yes. Inspect the `results` param and
            // find the actual "Yes/Allow/Accept" choice's index. Fallback to 0
            // if the array isn't available or no clear positive option found.
            if (responseRoute == ResponsePolicy.Route.REVERT_APPROVAL) {
                String[] revertResults = params != null ? params.get("results") : null;
                ResponsePolicy.IndexedChoice revert =
                        ResponsePolicy.revertApproval(revertResults);
                LOG.warn("V44/V67j REVERT: Accepting revert request (index={} = '{}') text: '{}'",
                    revert.index(), revert.label(), decisionText);
                // TRACE ORACLE V2: route + final-response observation ONLY.
                if (traceOpened) {
                    TraceSession.recordRoute(TraceRoute.V44_V67J_REVERT_APPROVAL,
                        "MULTIPLE_CHOICE + decision text contains 'revert'", null);
                    // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                    TraceSession.recordEvaluatorLaneNotApplicable(
                        "direct interceptor V44/V67j: evaluator lane never runs on this route");
                    TraceSession.recordFinalResponse(
                            String.valueOf(revert.index()), true);
                }
                return String.valueOf(revert.index());
            }

            // === V170 (Steve, 2026-06): UNDERCOVER SPY — the cheap drain blocker ===
            // Steve: "Spies should always be a part of the strategy. Spies cost much less
            // to block a drain than deploying a bunch of characters to overpower opponent."
            // An undercover spy deploys to the OPPONENT's side of a site and breaks their
            // control there — their Force drain at that site stops, for the cost of one
            // cheap character. The engine asks a YesNoDecision ("Do you want to deploy X
            // as an Undercover spy?") which previously fell to heuristics.
            // Rule: YES when the opponent currently has ANY active drain to block
            // (bonus-aware, getForceDrainAmount over sites they occupy); NO when there is
            // nothing to block yet — keep the spy as a normal body with power/presence.
            // V67j discipline: scan the results array for the actual Yes/No indexes.
            if (responseRoute == ResponsePolicy.Route.UNDERCOVER_SPY) {
                int v170OppDrain = 0;
                try {
                    com.gempukku.swccgo.game.state.GameState v170Gs =
                        currentGame != null ? currentGame.getGameState() : null;
                    if (v170Gs != null) {
                        String v170Opp = v170Gs.getOpponent(playerId);
                        for (com.gempukku.swccgo.game.PhysicalCard v170Loc : v170Gs.getTopLocations()) {
                            if (v170Loc == null) continue;
                            boolean v170OppHere = false;
                            for (com.gempukku.swccgo.game.PhysicalCard v170C : v170Gs.getCardsAtLocation(v170Loc)) {
                                if (v170C != null && v170Opp.equals(v170C.getOwner())) { v170OppHere = true; break; }
                            }
                            if (v170OppHere) {
                                v170OppDrain += (int) currentGame.getModifiersQuerying()
                                    .getForceDrainAmount(v170Gs, v170Loc, v170Opp);
                            }
                        }
                    }
                } catch (Exception v170E) {
                    LOG.debug("V170 drain check failed: {}", v170E.getMessage());
                }
                boolean v170GoUndercover =
                        ResponsePolicy.shouldDeployUndercover(v170OppDrain);
                String[] v170Results = params != null ? params.get("results") : null;
                ResponsePolicy.YesNoIndexes v170Indexes =
                        ResponsePolicy.yesNoIndexes(v170Results);
                int v170Pick = v170Indexes.choose(v170GoUndercover);
                LOG.warn("V170 UNDERCOVER SPY: opponent total drain={} -> {} (index={}) text: '{}'",
                    v170OppDrain, v170GoUndercover ? "YES, go undercover (block their drain)"
                        : "NO, deploy normally (nothing to block)", v170Pick, decisionText);
                // TRACE ORACLE V2: route + final-response observation ONLY.
                if (traceOpened) {
                    TraceSession.recordRoute(TraceRoute.V170_UNDERCOVER_CHOICE,
                        "MULTIPLE_CHOICE + decision text contains 'undercover spy'", null);
                    // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                    TraceSession.recordEvaluatorLaneNotApplicable(
                        "direct interceptor V170: evaluator lane never runs on this route");
                    TraceSession.recordFinalResponse(String.valueOf(v170Pick), true);
                }
                return String.valueOf(v170Pick);
            }

            // V61 EPIC EVENT SAGA CHOICE — "The Force Is Strong In My Family"
            // FIXES Issue from is9j46shx6t0swby replay: Rando picked "My Father Has It"
            // (for Anakin) in a Luke Saga Tatooine deck — Luke's power/defense boost was
            // lost. The TFISMF decision surfaces as type=MULTIPLE_CHOICE with text
            // 'Choose an option' (empty prompt) and the actual choices in the `results`
            // param. The V29.15 ActionTextEvaluator check was looking in the prompt text
            // instead of the options array, so it never triggered and Rando defaulted
            // to index 0 = "My Father Has It".
            //   Luke deck  → "I Have It"
            //   Anakin deck → "My Father Has It"
            //   Rey deck    → "You Have That Power, Too"
            if (decision.getDecisionType() == AwaitingDecisionType.MULTIPLE_CHOICE) {
                String[] results = params != null ? params.get("results") : null;
                // V61 amended (2026-07-27, Steve): persona COUNTS from the
                // deck are the primary signal (replay rgfogqxrh4uat4bo ran
                // 4x Rey / 3x Luke / 2x Anakin, the deck name arrived null,
                // and the old name law would have picked Luke regardless).
                // Typed getPersonas via DeckOracle; lazy-analyze because this
                // interceptor runs before the evaluator-context init.
                int sagaLuke = 0, sagaAnakin = 0, sagaRey = 0;
                try {
                    if (deckOracle != null) {
                        if (!deckOracle.isAnalyzed() && currentGame != null) {
                            deckOracle.analyze(currentGame, playerId, mySide);
                        }
                        sagaLuke = deckOracle.countCardsWithPersona(
                                com.gempukku.swccgo.common.Persona.LUKE);
                        sagaAnakin = deckOracle.countCardsWithPersona(
                                com.gempukku.swccgo.common.Persona.ANAKIN);
                        sagaRey = deckOracle.countCardsWithPersona(
                                com.gempukku.swccgo.common.Persona.REY);
                    }
                } catch (Exception sagaE) {
                    LOG.debug("V61 saga persona count failed: {}", sagaE.getMessage());
                }
                // V61 starting-location signal (2026-07-27, Steve): the
                // set-17 marker location on OUR side of the table pins the
                // saga by its own card text. Exact blueprint id first, exact
                // title equality as the reprint-safe fallback. UNKNOWN only
                // when the board cannot be read.
                SetupPolicy.SagaStartingLocation sagaStart =
                        SetupPolicy.SagaStartingLocation.UNKNOWN;
                try {
                    if (gameState != null) {
                        boolean sawOwnLocation = false;
                        for (PhysicalCard sagaLoc : gameState.getTopLocations()) {
                            if (sagaLoc == null
                                    || !playerId.equals(sagaLoc.getOwner())) {
                                continue;
                            }
                            sawOwnLocation = true;
                            String bp = sagaLoc.getBlueprintId(true);
                            String lt = sagaLoc.getTitle();
                            if ("217_27".equals(bp)
                                    || "Ajan Kloss: Training Course".equals(lt)) {
                                sagaStart = SetupPolicy.SagaStartingLocation
                                        .REY_LOCATION;
                                break;
                            }
                            if ("217_34".equals(bp)
                                    || "Endor: Anakin's Funeral Pyre".equals(lt)) {
                                sagaStart = SetupPolicy.SagaStartingLocation
                                        .LUKE_LOCATION;
                                break;
                            }
                        }
                        if (sagaStart == SetupPolicy.SagaStartingLocation.UNKNOWN
                                && sawOwnLocation) {
                            sagaStart = SetupPolicy.SagaStartingLocation
                                    .OTHER_LOCATION;
                        }
                    }
                } catch (Exception sagaLocE) {
                    LOG.debug("V61 saga location scan failed: {}",
                            sagaLocE.getMessage());
                }
                SetupPolicy.SagaSelection saga =
                        SetupPolicy.chooseSaga(deckName, results,
                                sagaLuke, sagaAnakin, sagaRey, sagaStart);
                if (saga.sagaChoice() && saga.index() >= 0) {
                    LOG.warn("V61 EPIC EVENT SAGA: deck='{}' choices={} → {} (index {})",
                            deckName, java.util.Arrays.asList(results),
                            saga.reason(), saga.index());
                    // TRACE ORACLE V2: route + final-response observation ONLY.
                    if (traceOpened) {
                        TraceSession.recordRoute(TraceRoute.V61_SAGA_CHOICE,
                                "MULTIPLE_CHOICE results contain TFISMF saga options", null);
                        // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                        TraceSession.recordEvaluatorLaneNotApplicable(
                                "direct interceptor V61: evaluator lane never runs on this route");
                        TraceSession.recordFinalResponse(
                                String.valueOf(saga.index()), true);
                    }
                    return String.valueOf(saga.index());
                }
            }

            // === V79b (Steve, 2026-06-28): DEATH STAR PARSEC CHOICE (Verge of Greatness) ===
            // The "Move using hyperspeed" action carries NO parsec; the engine then asks a SEPARATE
            // MULTIPLE_CHOICE "Choose parsec to move to" — which had NO handler, so Rando picked
            // arbitrarily and the Death Star wandered AWAY from Scarif (replay 4->2->0). V79 in
            // MoveEvaluator was scoring the wrong decision. When Verge of Greatness + the Death Star
            // are on our table, steer this choice toward Scarif (parsec 7): 4->6 turn 1, 6->7 turn 2,
            // and take an orbit/Scarif option immediately if offered. (Found via the now-readable
            // gemp-swccg.log: "No evaluators produced actions for decision: Choose parsec to move to".)
            // Retired 2026-08-05: the mirrored ActionTextEvaluator now owns
            // this child decision for both public bots. Keep the legacy rule
            // intact for audit history, but do not let Rando bypass parity.
            boolean useLegacyV79bDirectInterceptor = false;
            if (useLegacyV79bDirectInterceptor
                    && decision.getDecisionType() == AwaitingDecisionType.MULTIPLE_CHOICE
                    && decisionText.toLowerCase(java.util.Locale.ROOT).contains("parsec")) {
                String[] pResults = params != null ? params.get("results") : null;
                if (pResults != null && pResults.length > 0 && gameState != null) {
                    boolean vergeOnTable = false;
                    try {
                        for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                            if (pc == null || !playerId.equals(pc.getOwner()) || pc.getBlueprint() == null) continue;
                            if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                            String t = pc.getTitle() != null ? pc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                            if (t.contains("on the verge of greatness") || t.contains("taking control of the weapon")) {
                                vergeOnTable = true; break;
                            }
                        }
                    } catch (Exception ignore) { /* fall through to default */ }
                    if (vergeOnTable) {
                        // V79b source correction: from Scarif
                        // orbit the old closest-to-7 pick was the DEEP-SPACE EXIT — the engine excludes
                        // the currently-orbited system from re-orbit at the chosen parsec
                        // (MoveMobileSystemUsingHyperspeedAction:82, Filters.not(Filters.isOrbitedBy(card))),
                        // so answering '7' silently dropped the DS out of orbit (the turns-3/5 toggle).
                        // Pre-flip + orbiting Scarif this prompt should never appear because the
                        // objective still needs that orbit for its actor gate. Prefer an orbit/Scarif
                        // option, else answer the DS's CURRENT
                        // parsec (least-bad: stays at Scarif's parsec; from deep space at that parsec
                        // the engine auto-re-orbits the lone orbitable system, :91-96). Pre-flip and
                        // post-flip movement uses the normal closest-to-7 steering below. The back
                        // flips on loss of a Scarif leader, not on Death Star movement.
                        boolean v79bFrontHold = false;
                        Integer v79bCurrentParsec = null;
                        try {
                            objectiveAnalyzer.refreshFlipStatus(gameState, playerId);
                            if (objectiveAnalyzer.isAnalyzed() && !objectiveAnalyzer.isFlipped()) {
                                for (PhysicalCard pc : gameState.getAllPermanentCards()) {
                                    if (pc == null || !playerId.equals(pc.getOwner()) || pc.getBlueprint() == null) continue;
                                    if (pc.getZone() == null || !pc.getZone().isInPlay()) continue;
                                    if (pc.getBlueprint().getCardCategory() != CardCategory.LOCATION) continue;
                                    String dsT = pc.getTitle() != null ? pc.getTitle().toLowerCase(java.util.Locale.ROOT) : "";
                                    if (!dsT.contains("death star")) continue;
                                    v79bCurrentParsec = pc.getParsec();
                                    String orbited = pc.getSystemOrbited();
                                    v79bFrontHold = orbited != null
                                        && orbited.toLowerCase(java.util.Locale.ROOT).contains("scarif");
                                    break;
                                }
                            }
                        } catch (Exception ignore) { /* fall through to route steering */ }
                        if (v79bFrontHold) {
                            for (int i = 0; i < pResults.length; i++) {
                                String r = pResults[i] == null ? "" : pResults[i].toLowerCase(java.util.Locale.ROOT);
                                if (r.contains("scarif") || r.contains("orbit")) {
                                    LOG.warn("V79b FRONT FLIP HOLD: unflipped + orbiting Scarif, take orbit option: choices={} -> index {} ('{}')",
                                        java.util.Arrays.asList(pResults), i, pResults[i]);
                                    // TRACE ORACLE V2: route + final-response observation ONLY (Rando-only route).
                                    if (traceOpened) {
                                        TraceSession.recordRoute(TraceRoute.V79B_PARSEC_CHOICE,
                                            "parsec choice + Verge front at Scarif (hold orbit option)", null);
                                        // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                                        TraceSession.recordEvaluatorLaneNotApplicable(
                                            "direct interceptor V79b: evaluator lane never runs on this route");
                                        TraceSession.recordFinalResponse(String.valueOf(i), true);
                                    }
                                    return String.valueOf(i);
                                }
                            }
                            if (v79bCurrentParsec != null) {
                                for (int i = 0; i < pResults.length; i++) {
                                    String r = pResults[i] == null ? "" : pResults[i];
                                    java.util.regex.Matcher sm = java.util.regex.Pattern.compile("(\\d+)").matcher(r);
                                    if (sm.find()) {
                                        try {
                                            if (Integer.parseInt(sm.group(1)) == v79bCurrentParsec.intValue()) {
                                                LOG.warn("V79b FRONT FLIP HOLD: unflipped + orbiting Scarif, staying at parsec {} "
                                                    + "(choices={} -> index {}; NOTE this prompt should be unreachable, "
                                                    + "MoveEvaluator vetoes the move)",
                                                    v79bCurrentParsec, java.util.Arrays.asList(pResults), i);
                                                // TRACE ORACLE V2: route + final-response observation ONLY (Rando-only route).
                                                if (traceOpened) {
                                                    TraceSession.recordRoute(TraceRoute.V79B_PARSEC_CHOICE,
                                                        "parsec choice + Verge front at Scarif (stay parked)", null);
                                                    // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                                                    TraceSession.recordEvaluatorLaneNotApplicable(
                                                        "direct interceptor V79b: evaluator lane never runs on this route");
                                                    TraceSession.recordFinalResponse(String.valueOf(i), true);
                                                }
                                                return String.valueOf(i);
                                            }
                                        } catch (Exception e) { /* ignore */ }
                                    }
                                }
                            }
                            LOG.warn("V79b FRONT FLIP HOLD: unflipped + orbiting but current parsec {} not offered in {}, falling through to closest-to-7",
                                v79bCurrentParsec, java.util.Arrays.asList(pResults));
                        }
                        int v79bBest = -1, v79bBestDist = Integer.MAX_VALUE, v79bBestParsec = -1;
                        for (int i = 0; i < pResults.length; i++) {
                            String r = pResults[i] == null ? "" : pResults[i].toLowerCase(java.util.Locale.ROOT);
                            if (r.contains("scarif") || r.contains("orbit")) {
                                LOG.warn("V79b DEATH STAR ORBIT: choices={} -> index {} ('{}')",
                                    java.util.Arrays.asList(pResults), i, pResults[i]);
                                // TRACE ORACLE V2: route + final-response observation ONLY (Rando-only route).
                                if (traceOpened) {
                                    TraceSession.recordRoute(TraceRoute.V79B_PARSEC_CHOICE,
                                        "parsec choice + Verge on table (orbit/Scarif option)", null);
                                    // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                                    TraceSession.recordEvaluatorLaneNotApplicable(
                                        "direct interceptor V79b: evaluator lane never runs on this route");
                                    TraceSession.recordFinalResponse(String.valueOf(i), true);
                                }
                                return String.valueOf(i);
                            }
                            java.util.regex.Matcher mm = java.util.regex.Pattern.compile("(\\d+)").matcher(r);
                            if (mm.find()) {
                                try {
                                    int p = Integer.parseInt(mm.group(1));
                                    int d = Math.abs(p - 7);  // Scarif is at parsec 7
                                    if (d < v79bBestDist) { v79bBestDist = d; v79bBest = i; v79bBestParsec = p; }
                                } catch (Exception e) { /* ignore */ }
                            }
                        }
                        if (v79bBest >= 0) {
                            LOG.warn("V79b DEATH STAR PARSEC: choices={} -> index {} (parsec {}, closest to Scarif 7)",
                                java.util.Arrays.asList(pResults), v79bBest, v79bBestParsec);
                            // TRACE ORACLE V2: route + final-response observation ONLY (Rando-only route).
                            if (traceOpened) {
                                TraceSession.recordRoute(TraceRoute.V79B_PARSEC_CHOICE,
                                    "parsec choice + Verge on table (closest-to-7 steering)", null);
                                // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                                TraceSession.recordEvaluatorLaneNotApplicable(
                                    "direct interceptor V79b: evaluator lane never runs on this route");
                                TraceSession.recordFinalResponse(String.valueOf(v79bBest), true);
                            }
                            return String.valueOf(v79bBest);
                        }
                    }
                }
            }

            Phase currentPhase = gameState != null ? gameState.getCurrentPhase() : null;
            // V295: the disabled 0% chaos bypass was retired. Every decision now enters
            // the evaluator lane before the existing heuristic fallback.
            // V295 RETIRED: boolean isSafeForChaos = currentPhase != Phase.DEPLOY
            // V295 RETIRED:     && currentPhase != Phase.BATTLE;
            // V295 RETIRED: if (isSafeForChaos && shouldApplyChaos()) {
            // V295 RETIRED:     RandoLogger.debug("Chaos mode: selecting random action");
            // V295 RETIRED:     if (traceOpened) {
            // V295 RETIRED:         TraceSession.recordRoute(TraceRoute.CHAOS_FALLBACK,
            // V295 RETIRED:             "chaos gate passed (phase=" + currentPhase
            // V295 RETIRED:                 + ", outside deploy/battle)", null);
            // V295 RETIRED:         TraceSession.recordEvaluatorLaneNotApplicable(
            // V295 RETIRED:             "chaos fallback: heuristic base bypassed the evaluator lane");
            // V295 RETIRED:     }
            // V295 RETIRED:     result = super.decide(playerId, decision, gameState);
            // V295 RETIRED: } else {
            String evaluatorResult = tryEvaluators(
                playerId, decision, gameState, activationAmountDecision);
            if (evaluatorResult != null) {
                // TRACE ORACLE V2: normal CombinedEvaluator route.
                if (traceOpened) {
                    TraceSession.recordRoute(TraceRoute.COMBINED_EVALUATOR,
                        "evaluator lane handled decisionType=" + decisionType, null);
                }
                result = evaluatorResult;
            } else {
                // Fall back to keyword-based heuristics
                LOG.debug("Evaluators returned null, falling back to heuristics");
                // TRACE ORACLE V2: explicit heuristic-fallback route (no invisible side exit).
                if (traceOpened) {
                    TraceSession.recordRoute(TraceRoute.HEURISTIC_FALLBACK,
                        "no evaluator handled decisionType=" + decisionType, null);
                    // GATE P0-3: explicit n/a — a per-fact no-op when the evaluator
                    // lane DID run (and record its facts) before declining.
                    TraceSession.recordEvaluatorLaneNotApplicable(
                        "heuristic fallback: no evaluator lane facts for this decision");
                }
                result = super.decide(playerId, decision, gameState);
                // V191 (2026-07-06): TOP-N breadcrumb for the fallback path.
                // The per-candidate score loop (scoreAction + penalty stack)
                // lives in HeuristicAiBase.decide — private penalties there
                // make a faithful top-5 impossible from this subclass without
                // duplicating scoring, so log the path + final pick only.
                // Instrumentation only: zero scoring changes.
                LOG.warn("V191 TOPN: {} phase={} :: fallback-heuristic picked='{}' (top-5 n/a: pick loop in HeuristicAiBase)",
                    decision.getDecisionType(), currentPhase,
                    result != null ? result : "(pass)");
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
                String traceEmergencyWhy = (result == null)
                    ? "no result from evaluators or heuristics"
                    : "empty result with raw noPass=true";
                DecisionSafety.SafetyDecision emergency =
                    DecisionSafety.getEmergencyResponse(decision, actionIds, cardIds);
                result = emergency.value;
                LOG.warn("🚨 Emergency response: '{}' ({})", result, emergency.reason);
                // TRACE ORACLE V2: the raw-noPass emergency is an explicit route (the prior
                // lane stays in the ordered evidence) + the emergency response/reason land
                // in the finalization record. Observation only, recorded after the fact.
                if (traceOpened) {
                    TraceSession.recordRoute(TraceRoute.RAW_NOPASS_EMERGENCY,
                        traceEmergencyWhy, traceEmergencyWhy);
                    // GATE P0-3: explicit n/a backstop — a per-fact no-op when the
                    // evaluator lane recorded its facts before the emergency took over.
                    TraceSession.recordEvaluatorLaneNotApplicable(traceEmergencyWhy);
                    TraceSession.recordEmergencyResponse(result, emergency.reason);
                }
            }

            // === SAFETY LAYER 2: Response Validation ===
            // Validate the response is actually valid for this decision
            String[] availableOptions = actionIds != null && actionIds.length > 0 ? actionIds : cardIds;
            String[] validated = DecisionSafety.ensureValidResponse(decision, result, availableOptions);
            if (validated[1] != null && !validated[1].isEmpty()) {
                LOG.warn("🚨 Response corrected: {}", validated[1]);
            }
            result = validated[0];

            String stackedPileSourceCardId = ShieldPolicy.selectedTopLevelPlayCardSourceId(
                    decisionType, actionIds,
                    params != null ? params.get("actionText") : null,
                    cardIds, result);
            if (stackedPileSourceCardId != null && gameState != null) {
                try {
                    PhysicalCard sourceCard = gameState.findCardById(
                            Integer.parseInt(stackedPileSourceCardId));
                    if (sourceCard != null
                            && ShieldPolicy.isStackedPileShieldSource(sourceCard.getTitle())) {
                        shieldStrategy.recordKnDActivation(activationTurn);
                    }
                } catch (Exception v102e) {
                    LOG.debug("V102 K&D activation tracking error: {}", v102e.getMessage());
                }
            }

            if (ActivateDecisionRouting.selectedTopLevelActivate(
                    phase, decisionType,
                    params != null ? params.get("actionId") : null,
                    params != null ? params.get("actionText") : null,
                    result)) {
                activationAmountLatch.arm(
                    gameState, playerId, activationTurn, phase);
            }

            // Record the decision for loop tracking
            // TRACE 4A1 (m00372 Option A, accepted m00373; matrix correction): the
            // RECORD_RESPONSE observation is captured AFTER the legacy call, with the
            // complete decision-affecting owner snapshots before/after from the pure
            // traceSnapshot() seam. DISABLED capture calls NEITHER pure accessor: both
            // snapshot builds sit under the traceOpened guard. The legacy call itself
            // is byte-for-byte unchanged and runs exactly once either way.
            DecisionTrackerSnapshot traceTrackerBefore =
                traceOpened ? decisionTracker.traceSnapshot() : null;
            decisionTracker.recordDecision(decisionType, decisionText,
                String.valueOf(decision.getAwaitingDecisionId()), result != null ? result : "");
            if (traceOpened) {
                TraceSession.recordTrackerRecordResponse(TrackerOwner.OUTER_RANDO,
                    decisionType, String.valueOf(decision.getAwaitingDecisionId()),
                    decisionTracker.traceDecisionKey(decisionType, decisionText),
                    result != null ? result : "",
                    traceTrackerBefore, decisionTracker.traceSnapshot());
            }

            // Track strategic events for learning
            // TRACE 4A1: the wrapper-level strategic-intent event is REMOVED. State
            // events are emitted only at the actual direct writes inside
            // trackStrategicEvents (pending-deploy SET) and trackGameState (its CLEAR);
            // the wrapper's StrategyController calls stay unobserved until that owner's
            // increment (4B).
            trackStrategicEvents(decision, decisionText, result);

            LOG.info("[RandoCalAi] decide() result: '{}' ✅", result != null ? result : "(pass)");
            // TRACE ORACLE V2: the AI's ACTUAL final answer, after safety (contract
            // "Finalization record" item 6). The five direct interceptors record theirs
            // with skippedCommonFinalizer=true at their own return sites above.
            if (traceOpened) {
                TraceSession.recordFinalResponse(result, false);
            }
            return result;
        } finally {
            context = null;
            // TRACE ORACLE V2: only the opener closes and emits; the thread-local is
            // ALWAYS cleared (TraceSession.close clears in its own finally).
            // GATE P0-2 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): closeAndEmit is
            // the one typed emission channel — a finish() failure emits the fallback
            // INCOMPLETE envelope, a sink accept() failure re-offers the trace once
            // with a typed SINK failure appended. Never throws into the decision path.
            if (traceOpened) {
                TraceSession.closeAndEmit(decisionTraceSink);
            }
        }
    }

    /**
     * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
     * "Frozen input and candidate order"): open the bot-boundary session with the
     * COMPLETE raw decision arrays (verbatim, unfiltered — unlike buildEvaluatorContext,
     * which drops null/empty entries) plus the shadow DecisionSnapshot. Pure reads only:
     * decision params, DecisionTracker.getBlockedResponses (pure), and plain GameState
     * getters. No evaluator, strategy service, or cache-mutating call.
     */
    private boolean openDecisionTraceSession(String playerId, AwaitingDecision decision,
                                             GameState gameState, String decisionType,
                                             String decisionText, Phase phase) {
        Map<String, String[]> params = decision.getDecisionParameters();
        TraceSnapshots.Input in = new TraceSnapshots.Input();
        in.producerId = "bot-decide-boundary";
        // GATE P0-1 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): the COMPLETE verbatim
        // engine parameter map — every key preserved separately (presence = key exists;
        // present-empty arrays and blank entries verbatim). The parsed fields below stay
        // the typed-facts inputs; the raw evidence is never replaced by them.
        in.rawParameters = (params != null) ? params : java.util.Collections.emptyMap();
        in.decisionId = String.valueOf(decision.getAwaitingDecisionId());
        in.decisionTypeName = decisionType;
        in.decisionText = decisionText;
        in.phase = phase;
        in.turn = gameState != null ? gameState.getPlayersLatestTurnNumber(playerId) : 0;
        in.currentPlayer = playerId;
        in.side = gameState != null ? gameState.getSide(playerId) : mySide;
        if (params != null) {
            String[] noPassArr = params.get("noPass");
            if (noPassArr != null && noPassArr.length > 0) {
                in.noPassParam = Boolean.parseBoolean(noPassArr[0]);
            }
            in.minParam = traceParseIntOrNull(params.get("min"));
            in.maxParam = traceParseIntOrNull(params.get("max"));
            in.actionIds = traceRawList(params.get("actionId"));
            in.actionTexts = traceRawList(params.get("actionText"));
            in.cardIds = traceRawList(params.get("cardId"));
            in.blueprintIds = traceRawList(params.get("blueprintId"));
            in.testingTexts = traceRawList(params.get("testingText"));
            in.multipleChoiceResults = traceRawList(params.get("results"));
            String[] selectableArr = params.get("selectable");
            if (selectableArr != null) {
                java.util.List<Boolean> sel = new java.util.ArrayList<>(selectableArr.length);
                for (String s : selectableArr) {
                    sel.add("true".equalsIgnoreCase(s));
                }
                in.selectable = sel;
            }
        }
        try {
            in.blockedResponses = decisionTracker.getBlockedResponses(decisionType, decisionText);
        } catch (Exception ignore) {
            // leave null — facts model treats it as empty
        }
        if (gameState != null) {
            try {
                in.forcePileSize = gameState.getForcePileSize(playerId);
                in.lifeForceCardCount = gameState.getPlayerLifeForce(playerId);
                in.handSize = gameState.getHand(playerId).size();
                in.reserveDeckSize = gameState.getReserveDeckSize(playerId);
            } catch (Exception ignore) {
                // leave unknown — never fabricate an observation
            }
        }
        TraceSnapshots.Result snapshot = TraceSnapshots.build(in);
        return TraceSession.open(getClass().getPackageName(),
            in.decisionId, decisionType, decisionText,
            TraceSnapshots.rawCandidateIds(decisionType, in.actionIds, in.cardIds,
                in.multipleChoiceResults),
            snapshot.snapshot(), snapshot.issues(), true);
    }

    private static Integer traceParseIntOrNull(String[] vals) {
        if (vals == null || vals.length == 0 || vals[0] == null) return null;
        try {
            return Integer.parseInt(vals[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static java.util.List<String> traceRawList(String[] arr) {
        return arr != null ? java.util.Arrays.asList(arr) : null;
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

        // TRACE 4A2a (Handoffs/CODEX_TRACE_STAGE4_4A2A_OUTER_TRACKER_LIFECYCLE_2026-07-13.md
        // "Hook law"): observe the one outer UPDATE_STATE lifecycle call. Snapshots are
        // built only when a session is active, each under an instrumentation-only
        // try/catch that converts failure into a typed STATE_EVENT capture failure; the
        // legacy mutator below is byte-for-byte unchanged and runs exactly once either
        // way. The five ints are the EXACT legacy call arguments (zero defaults after a
        // caught getter exception above), recorded as call args, not asserted board truth.
        DecisionTrackerLifecycleSnapshot traceLifecycleBefore = null;
        if (TraceSession.isActive()) {
            try {
                traceLifecycleBefore = decisionTracker.traceLifecycleSnapshot();
            } catch (Throwable traceT) {
                TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                    traceT.getClass().getName(),
                    "UPDATE_STATE before-snapshot failed; legacy updateState unaffected");
            }
        }

        decisionTracker.updateState(handSize, forcePile, reserveDeck, turn, cardsInPlay);

        if (traceLifecycleBefore != null) {
            try {
                TraceSession.recordTrackerUpdateState(TrackerOwner.OUTER_RANDO,
                    handSize, forcePile, reserveDeck, turn, cardsInPlay,
                    traceLifecycleBefore, decisionTracker.traceLifecycleSnapshot());
            } catch (Throwable traceT) {
                TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                    traceT.getClass().getName(),
                    "UPDATE_STATE after-snapshot/record failed; legacy updateState already ran");
            }
        }
    }

    private void rememberSelectedMoveCard(
            DecisionContext context,
            EvaluatedAction selected,
            AwaitingDecision parentDecision) {
        boolean agentsOfBlackSunControlMove = context != null
                && context.getPhase() == Phase.CONTROL
                && context.getObjectiveAnalyzer() != null
                && context.getObjectiveAnalyzer()
                    .isActiveAgentsOfBlackSunBountyMoveAction(
                        currentGame, context.getPlayerId());
        if (context == null || selected == null
                || context.getPhase() != Phase.MOVE
                    && !agentsOfBlackSunControlMove
                || context.getDecisionType() == null) {
            return;
        }
        String normalizedPrompt = context.getDecisionText() != null
                ? context.getDecisionText().trim()
                    .toLowerCase(Locale.ROOT)
                : "";
        if (agentsOfBlackSunControlMove
                && "CARD_SELECTION".equals(
                    context.getDecisionType())
                && normalizedPrompt.startsWith(
                    "choose bounty hunter to move")) {
            pendingMovePhysicalCardId = null;
            pendingMoveActionSourcePermanentCardId = null;
            pendingHiddenPathCorridorDestinationPermanentCardId = null;
            try {
                PhysicalCard selectedHunter =
                        context.getGameState() != null
                            ? context.getGameState().findCardById(
                                Integer.parseInt(
                                    selected.getActionId()))
                            : null;
                if (selectedHunter != null) {
                    pendingMovePhysicalCardId =
                            selectedHunter.getCardId();
                }
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        if (agentsOfBlackSunControlMove
                && "MULTIPLE_CHOICE".equals(
                    context.getDecisionType())
                && normalizedPrompt.contains(
                    "choose regular move action")) {
            int selectedIndex = context.getActionIds().indexOf(
                    selected.getActionId());
            String selectedText = selectedIndex >= 0
                    && selectedIndex
                        < context.getActionTexts().size()
                ? context.getActionTexts().get(selectedIndex) : "";
            if (!"Move using landspeed".equals(selectedText)) {
                pendingMovePhysicalCardId = null;
            }
            return;
        }
        PhysicalCard pendingMoveSource = findCardByPermanentId(
                context.getGameState(),
                pendingMoveActionSourcePermanentCardId);
        boolean hiddenPathCorridorChild =
                "CARD_SELECTION".equals(
                    context.getDecisionType())
                && pendingMoveSource != null
                && context.getPlayerId().equals(
                    pendingMoveSource.getOwner())
                && "226_23".equals(
                    pendingMoveSource.getBlueprintId(true))
                && (normalizedPrompt.startsWith(
                        "choose card to move from")
                    || normalizedPrompt.startsWith(
                        "choose card to move to"));
        if (hiddenPathCorridorChild) {
            PhysicalCard selectedCard = null;
            try {
                selectedCard = context.getGameState() != null
                        ? context.getGameState().findCardById(
                            Integer.parseInt(
                                selected.getActionId()))
                        : null;
            } catch (NumberFormatException ignored) {
            }
            if (selectedCard == null
                    || selectedCard.getBlueprint() == null
                    || selectedCard.getBlueprint()
                        .getCardCategory()
                        != CardCategory.LOCATION) {
                pendingMoveActionSourcePermanentCardId = null;
                pendingHiddenPathCorridorDestinationPermanentCardId = null;
            } else if (normalizedPrompt.equals(
                    "choose card to move to")
                    || normalizedPrompt.startsWith(
                        "choose card to move to,")) {
                pendingHiddenPathCorridorDestinationPermanentCardId =
                        selectedCard.getPermanentCardId();
            } else if (normalizedPrompt.equals(
                    "choose card to move from")
                    || normalizedPrompt.startsWith(
                        "choose card to move from,")) {
                pendingHiddenPathCorridorDestinationPermanentCardId = null;
            }
            return;
        }
        if ("CARD_SELECTION".equals(
                    context.getDecisionType())
                && context.getDecisionText() != null
                && context.getDecisionText().trim()
                    .toLowerCase(Locale.ROOT)
                    .startsWith("choose jedi to relocate")
                && context.getObjectiveAnalyzer() != null
                && context.getObjectiveAnalyzer()
                    .isHiddenPathObjectiveFamily()
                && context.getObjectiveAnalyzer().isFlipped()) {
            pendingMovePhysicalCardId = null;
            try {
                PhysicalCard selectedJedi =
                        context.getGameState() != null
                            ? context.getGameState().findCardById(
                                Integer.parseInt(
                                    selected.getActionId()))
                            : null;
                if (selectedJedi != null) {
                    pendingMovePhysicalCardId =
                            selectedJedi.getCardId();
                }
            } catch (NumberFormatException ignored) {
            }
            return;
        }
        pendingMovePhysicalCardId = null;
        pendingMoveActionSourcePermanentCardId = null;
        pendingHiddenPathCorridorDestinationPermanentCardId = null;
        if (!context.getDecisionType()
                .contains("ACTION_CHOICE")) {
            return;
        }
        int index = context.getActionIds().indexOf(
                selected.getActionId());
        if (index < 0
                || index >= context.getActionTexts().size()
                || index >= context.getCardIds().size()) {
            return;
        }
        String actionText =
                context.getActionTexts().get(index);
        String actionTextLower = actionText != null
                ? actionText.toLowerCase(Locale.ROOT) : "";
        PhysicalCard actionSource =
                AiActionSourceProvenance
                    .selectedActionSource(
                        parentDecision,
                        selected.getActionId());
        boolean exactHiddenPathRelocation =
                context.getObjectiveAnalyzer() != null
                && context.getObjectiveAnalyzer()
                    .isHiddenPathBackRelocateAction(
                        currentGame, context.getPlayerId(),
                        actionSource, actionText);
        if (exactHiddenPathRelocation) {
            pendingMoveActionSourcePermanentCardId =
                    actionSource.getPermanentCardId();
            return;
        }
        boolean exactHiddenPathCorridor =
                context.getObjectiveAnalyzer() != null
                && context.getObjectiveAnalyzer()
                    .isHiddenPathObjectiveFamily()
                && !context.getObjectiveAnalyzer().isFlipped()
                && actionSource != null
                && context.getPlayerId().equals(
                    actionSource.getOwner())
                && "226_23".equals(
                    actionSource.getBlueprintId(true))
                && "Move Jedi Survivor here to a site"
                    .equals(actionText);
        if (exactHiddenPathCorridor) {
            pendingMoveActionSourcePermanentCardId =
                    actionSource.getPermanentCardId();
            pendingHiddenPathCorridorDestinationPermanentCardId = null;
            return;
        }
        if (!actionTextLower.contains("move using landspeed")
                && !actionTextLower.contains("move using hyperspeed")
                && !actionTextLower.contains("embark")
                && !actionTextLower.contains("disembark")
                && !actionTextLower.contains("shuttle")) {
            return;
        }
        try {
            PhysicalCard attachedCard =
                    context.getGameState() != null
                        ? context.getGameState().findCardById(
                            Integer.parseInt(
                                context.getCardIds().get(index)))
                        : null;
            if (attachedCard != null) {
                pendingMovePhysicalCardId =
                        attachedCard.getCardId();
            }
            if (actionSource != null) {
                pendingMoveActionSourcePermanentCardId =
                        actionSource.getPermanentCardId();
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private void rememberSelectedTdigwattLandoMove(
            DecisionContext context,
            EvaluatedAction selected,
            AwaitingDecision parentDecision) {
        if (context == null || selected == null
                || context.getDecisionType() == null) {
            return;
        }
        int index = context.getActionIds().indexOf(
                selected.getActionId());
        String selectedText = index >= 0
                && index < context.getActionTexts().size()
            ? context.getActionTexts().get(index) : "";
        String normalized = selectedText != null
            ? selectedText.trim()
                .toLowerCase(Locale.ROOT) : "";

        if ("MULTIPLE_CHOICE".equals(
                    context.getDecisionType())
                && context.getDecisionText() != null
                && context.getDecisionText()
                    .trim().toLowerCase(Locale.ROOT)
                    .contains("choose regular move action")) {
            if (!"move using landspeed"
                    .equals(normalized)) {
                clearPendingTdigwattLandoMove();
            }
            return;
        }

        if (!"CARD_ACTION_CHOICE".equals(
                    context.getDecisionType())
                || context.getPhase() != Phase.CONTROL
                || !"have your lando make a regular move"
                    .equals(normalized)
                || currentGame == null
                || context.getPlayerId() == null) {
            return;
        }

        clearPendingTdigwattLandoMove();
        PhysicalCard source =
            AiActionSourceProvenance
                .selectedActionSource(
                    parentDecision,
                    selected.getActionId());
        if (source == null) {
            return;
        }
        TdigwattObjectiveFactsReader
            .readUsefulVirtualLandoLandspeedRoute(
                currentGame,
                context.getPlayerId(),
                source,
                TdigwattObjectiveFactsReader
                    .Proof.PROVEN)
            .ifPresent(route -> {
                pendingTdigwattLandoPermanentCardId =
                    route.landoPermanentCardId();
                pendingTdigwattActionSourcePermanentCardId =
                    source.getPermanentCardId();
                pendingTdigwattDestinationPermanentCardId =
                    route.destinationPermanentCardId();
            });
    }

    private void clearPendingTdigwattLandoMove() {
        pendingTdigwattLandoPermanentCardId = null;
        pendingTdigwattActionSourcePermanentCardId = null;
        pendingTdigwattDestinationPermanentCardId = null;
    }

    private PhysicalCard findCardByPermanentId(
            GameState gameState,
            Integer permanentCardId) {
        if (gameState == null
                || permanentCardId == null) {
            return null;
        }
        for (PhysicalCard card
                : gameState.getAllPermanentCards()) {
            if (card != null
                    && card.getPermanentCardId()
                        == permanentCardId) {
                return card;
            }
        }
        return null;
    }

    private void rememberSelectedLostPileDeployCard(
            DecisionContext context,
            EvaluatedAction selected) {
        if (context == null || selected == null
                || context.getPhase() != Phase.DEPLOY
                || !"ARBITRARY_CARDS".equals(
                    context.getDecisionType())
                || context.getDecisionText() == null
                || !"choose card to deploy from lost pile"
                    .equals(context.getDecisionText()
                        .trim().toLowerCase(Locale.ROOT))) {
            return;
        }
        pendingObjectiveDeployingCardId = null;
        int index = context.getActionIds().indexOf(
                selected.getActionId());
        List<PhysicalCard> lostPile =
                context.getGameState() != null
                && context.getPlayerId() != null
                    ? context.getGameState()
                        .getLostPile(context.getPlayerId())
                    : null;
        List<String> offeredBlueprints =
                context.getBlueprints();
        if (index < 0 || lostPile == null
                || offeredBlueprints == null
                || index >= lostPile.size()
                || index >= offeredBlueprints.size()
                || !("temp" + index).equals(
                    selected.getActionId())) {
            return;
        }
        PhysicalCard selectedCard = lostPile.get(index);
        String selectedBlueprint =
                offeredBlueprints.get(index);
        if (selectedCard != null
                && selectedBlueprint != null
                && selectedBlueprint.equals(
                    selectedCard.getBlueprintId(true))) {
            pendingObjectiveDeployingCardId =
                    selectedCard.getPermanentCardId();
        }
    }

    private void rememberSelectedDeployCard(
            DecisionContext context,
            EvaluatedAction selected,
            AwaitingDecision parentDecision) {
        if (context != null && selected != null
                && context.getPhase() == Phase.DEPLOY
                && context.getDecisionType() != null
                && context.getDecisionType()
                    .contains("ACTION_CHOICE")
                && selected.getActionType()
                    == ActionType.DEPLOY) {
            pendingObjectiveDeployingCardId = null;
            pendingDeployActionSourcePermanentCardId = null;
            int index = context.getActionIds().indexOf(
                    selected.getActionId());
            if (index < 0
                    || index >= context.getCardIds().size()
                    || context.getGameState() == null) {
                return;
            }
            try {
                PhysicalCard selectedCard =
                        context.getGameState().findCardById(
                            Integer.parseInt(
                                context.getCardIds().get(index)));
                if (selectedCard != null) {
                    PhysicalCard actionSource =
                            AiActionSourceProvenance
                                .selectedActionSource(
                                    parentDecision,
                                    selected.getActionId());
                    if (actionSource != null) {
                        pendingDeployActionSourcePermanentCardId =
                                actionSource
                                    .getPermanentCardId();
                    }
                }
                if (selectedCard != null
                        && context.getPlayerId() != null
                        && selectedCard.getZone() == Zone.HAND
                        && context.getPlayerId().equals(
                            selectedCard.getOwner())) {
                    pendingObjectiveDeployingCardId =
                            selectedCard.getPermanentCardId();
                }
            } catch (NumberFormatException ignored) {
                // Non-card deploy sources must not become child provenance.
            }
            return;
        }
        rememberSelectedLostPileDeployCard(
                context, selected);
    }

    private void rememberSelectedOnTheVergeVaderReaction(
            DecisionContext context,
            EvaluatedAction selected,
            AwaitingDecision parentDecision) {
        pendingOnTheVergeVaderActionSourcePermanentCardId = null;
        if (context == null || selected == null
                || context.getObjectiveAnalyzer() == null
                || context.getDecisionType() == null
                || !context.getDecisionType().contains("ACTION_CHOICE")) {
            return;
        }
        int index = context.getActionIds().indexOf(
                selected.getActionId());
        if (index < 0 || index >= context.getActionTexts().size()) {
            return;
        }
        String actionText = context.getActionTexts().get(index);
        PhysicalCard source = AiActionSourceProvenance
                .selectedActionSource(
                    parentDecision, selected.getActionId());
        if (context.getObjectiveAnalyzer()
                .isOnTheVergeVaderBattleReactionAction(
                    currentGame, context.getPlayerId(),
                    source, actionText)) {
            pendingOnTheVergeVaderActionSourcePermanentCardId =
                    source.getPermanentCardId();
        }
    }

    /**
     * Try to use the evaluator system for this decision.
     *
     * @return result from evaluator, or null if evaluators don't handle this decision
     */
    private String tryEvaluators(String playerId, AwaitingDecision decision, GameState gameState,
                                 boolean activationAmountDecision) {
        // Build DecisionContext for evaluators
        DecisionContext evalContext = buildEvaluatorContext(
            playerId, decision, gameState, activationAmountDecision);
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
        rememberSelectedMoveCard(
                evalContext, bestAction, decision);
        rememberSelectedTdigwattLandoMove(
                evalContext, bestAction, decision);
        rememberSelectedDeployCard(
                evalContext, bestAction, decision);
        rememberSelectedOnTheVergeVaderReaction(
                evalContext, bestAction, decision);

        // === MULTI-SELECT FIX (Steve, 2026-05-31) ===
        // ArbitraryCardsSelectionDecision (and similar multi-select) expects a
        // comma-separated list of card IDs. The engine parses with
        // response.split(",") and validates cardIds.length is in [min, max] —
        // a single-ID response when min>1 throws DecisionResultInvalidException
        // and the engine re-prompts the same decision, forever.
        // Repro: "Choose Walker Garrison and 3rd Marker to take into hand"
        // (You May Start Your Landing turn-1 effect) with min=2 max=2 and 2
        // selectable cards (temp7, temp25). Rando sent 'temp7', engine rejected,
        // loop. The bug pre-dates the cancel-loop work — it's an output-format
        // gap, not a decision-tracking gap. Fix here at the response boundary
        // so every caller of tryEvaluators benefits, no per-evaluator changes.
        // Strategy for min>1: prefer the bestAction's ID first, then fill the
        // remainder from selectable card IDs in their list order until we have
        // exactly `min` IDs. (Picking exactly min is the safest count — never
        // exceeds max and always satisfies min. For min==max we get the
        // engine's exact required count; for min<max we don't over-commit.)
        // For min==1 or unknown, keep the original single-ID return.
        int multiMin = evalContext.getMin();
        int multiMax = evalContext.getMax();
        java.util.List<String> evalCardIds = evalContext.getCardIds();
        java.util.List<Boolean> evalSelectable = evalContext.getSelectable();
        boolean isMultiSelect = multiMin > 1
                && evalCardIds != null && !evalCardIds.isEmpty()
                && evalSelectable != null && evalSelectable.size() == evalCardIds.size();
        if (isMultiSelect) {
            java.util.LinkedHashSet<String> picked = new java.util.LinkedHashSet<>();
            String bestId = bestAction.getActionId();
            // Only seed with bestId if it's actually a card ID from the offered
            // list AND it's selectable. EvaluatedAction.getActionId() may sometimes
            // be an action index for CARD_ACTION_CHOICE; guard against that.
            if (bestId != null) {
                int bestIdx = evalCardIds.indexOf(bestId);
                if (bestIdx >= 0 && Boolean.TRUE.equals(evalSelectable.get(bestIdx))) {
                    picked.add(bestId);
                }
            }
            // Fill remainder from selectable cards in list order.
            for (int i = 0; i < evalCardIds.size() && picked.size() < multiMin; i++) {
                if (Boolean.TRUE.equals(evalSelectable.get(i))) {
                    picked.add(evalCardIds.get(i));
                }
            }
            if (picked.size() >= multiMin) {
                String multiResponse = String.join(",", picked);
                LOG.warn("MULTI-SELECT: min={} max={} → joining {} IDs: '{}' (best='{}', decision='{}')",
                    multiMin, multiMax, picked.size(), multiResponse, bestId,
                    bestAction.getDisplayText());
                // TRACE ORACLE V2: multi-select formatting result (finalization item 3).
                // No-op unless a bot-boundary session is open.
                TraceSession.recordMultiSelectResponse(multiResponse);
                return multiResponse;
            }
            LOG.warn("MULTI-SELECT FALLBACK: min={} but only {} selectable IDs collected — returning single bestId '{}' (engine likely rejects)",
                multiMin, picked.size(), bestId);
        }

        return bestAction.getActionId();
    }

    /**
     * Build a DecisionContext for evaluators from AwaitingDecision.
     */
    private DecisionContext buildEvaluatorContext(String playerId, AwaitingDecision decision,
                                                  GameState gameState,
                                                  boolean activationAmountDecision) {
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
        String promptLower = decision.getText() != null
            ? decision.getText().toLowerCase(Locale.ROOT) : "";
        if (pendingOnTheVergeVaderActionSourcePermanentCardId != null) {
            boolean vaderTargetChild =
                    "CARD_SELECTION".equals(decisionType.name())
                    && "choose vader, or click 'done' to cancel"
                        .equals(promptLower.trim());
            if (vaderTargetChild) {
                evalContext.setExtra(
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                    pendingOnTheVergeVaderActionSourcePermanentCardId);
            }
            pendingOnTheVergeVaderActionSourcePermanentCardId = null;
        }
        if (pendingTdigwattLandoPermanentCardId != null
                || pendingTdigwattActionSourcePermanentCardId
                    != null
                || pendingTdigwattDestinationPermanentCardId
                    != null) {
            boolean regularMoveChoice =
                "MULTIPLE_CHOICE".equals(
                    decisionType.name())
                && promptLower.contains(
                    "choose regular move action");
            boolean moveDestination =
                "CARD_SELECTION".equals(
                    decisionType.name())
                && promptLower.contains(
                    "choose where to move");
            if (regularMoveChoice || moveDestination) {
                evalContext.setExtra(
                    TdigwattObjectiveFactsReader
                        .LANDO_ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                    pendingTdigwattActionSourcePermanentCardId);
                evalContext.setExtra(
                    TdigwattObjectiveFactsReader
                        .LANDO_MOVER_PERMANENT_CARD_ID_EXTRA,
                    pendingTdigwattLandoPermanentCardId);
                evalContext.setExtra(
                    TdigwattObjectiveFactsReader
                        .LANDO_DESTINATION_PERMANENT_CARD_ID_EXTRA,
                    pendingTdigwattDestinationPermanentCardId);
                PhysicalCard pendingLando =
                    findCardByPermanentId(
                        gameState,
                        pendingTdigwattLandoPermanentCardId);
                if (pendingLando != null) {
                    evalContext.setExtra(
                        MovePhysicalCardResolver
                            .MOVER_CARD_ID_EXTRA,
                        pendingLando.getCardId());
                }
                if (moveDestination) {
                    clearPendingTdigwattLandoMove();
                }
            } else {
                clearPendingTdigwattLandoMove();
            }
        }
        boolean hiddenPathRelocationMoverChild =
                "CARD_SELECTION".equals(decisionType.name())
                && promptLower.trim().startsWith(
                    "choose jedi to relocate");
        PhysicalCard pendingMoveActionSource =
                findCardByPermanentId(
                    gameState,
                    pendingMoveActionSourcePermanentCardId);
        boolean hiddenPathCorridorChild =
                "CARD_SELECTION".equals(decisionType.name())
                && pendingMoveActionSource != null
                && playerId.equals(
                    pendingMoveActionSource.getOwner())
                && "226_23".equals(
                    pendingMoveActionSource
                        .getBlueprintId(true))
                && (promptLower.trim().startsWith(
                        "choose card to move from")
                    || promptLower.trim().startsWith(
                        "choose card to move to"));
        boolean agentsOfBlackSunMoveMechanismChild =
                "MULTIPLE_CHOICE".equals(decisionType.name())
                && promptLower.contains(
                    "choose regular move action")
                && objectiveAnalyzer != null
                && objectiveAnalyzer
                    .isActiveAgentsOfBlackSunBountyMoveAction(
                        currentGame, playerId);
        if (pendingMovePhysicalCardId != null
                || pendingMoveActionSourcePermanentCardId != null) {
            if ("CARD_SELECTION".equals(
                    decisionType.name())
                    && (promptLower.contains(
                        "choose where to move")
                        || promptLower.contains(
                            "choose where to embark")
                        || promptLower.contains(
                            "choose where to disembark")
                        || promptLower.contains(
                            "choose where to shuttle")
                        || promptLower.trim().startsWith(
                            "choose jedi to relocate")
                        || promptLower.trim().startsWith(
                            "choose site to relocate ")
                        || hiddenPathCorridorChild)) {
                if (pendingMovePhysicalCardId != null) {
                    evalContext.setExtra(
                        MovePhysicalCardResolver
                            .MOVER_CARD_ID_EXTRA,
                        pendingMovePhysicalCardId);
                }
                if (pendingMoveActionSourcePermanentCardId
                        != null) {
                    evalContext.setExtra(
                        BhbmForceDripUrgencyFactsReader
                            .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                        pendingMoveActionSourcePermanentCardId);
                }
                if (hiddenPathCorridorChild
                        && pendingHiddenPathCorridorDestinationPermanentCardId
                            != null) {
                    evalContext.setExtra(
                        com.gempukku.swccgo.ai.models.common.strategy
                            .ObjectiveAnalyzer
                            .HIDDEN_PATH_CORRIDOR_DESTINATION_PERMANENT_CARD_ID_EXTRA,
                        pendingHiddenPathCorridorDestinationPermanentCardId);
                }
            }
            // The selected top-level movement action's next decision owns this
            // provenance. If the expected child is absent or unreadable, fail
            // open instead of leaking the physical card into a later move.
            if (!agentsOfBlackSunMoveMechanismChild) {
                pendingMovePhysicalCardId = null;
            }
            if (!hiddenPathRelocationMoverChild
                    && !hiddenPathCorridorChild) {
                pendingMoveActionSourcePermanentCardId = null;
                pendingHiddenPathCorridorDestinationPermanentCardId = null;
            }
        }
        PhysicalCard pendingDeployActionSource =
                findCardByPermanentId(
                    gameState,
                    pendingDeployActionSourcePermanentCardId);
        boolean pendingFirstOrderNavyAction =
                pendingDeployActionSource != null
                && "225_24".equals(
                    pendingDeployActionSource
                        .getBlueprintId(true));
        boolean firstOrderNavyHandChild =
                pendingFirstOrderNavyAction
                && "CARD_SELECTION".equals(
                    decisionType.name())
                && "choose card from hand, or click 'done' to cancel"
                    .equals(promptLower.trim());
        boolean firstOrderNavyReserveChild =
                pendingFirstOrderNavyAction
                && "ARBITRARY_CARDS".equals(
                    decisionType.name())
                && promptLower.trim().startsWith(
                    "choose card to deploy from reserve deck simultaneously with");
        boolean firstOrderNavyResponseWindow =
                pendingFirstOrderNavyAction
                && decisionType.name()
                    .contains("ACTION_CHOICE")
                && promptLower.contains(
                    "optional responses");
        if (firstOrderNavyHandChild
                || firstOrderNavyReserveChild) {
            evalContext.setExtra(
                BhbmForceDripUrgencyFactsReader
                    .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                pendingDeployActionSourcePermanentCardId);
        } else if ((pendingObjectiveDeployingCardId != null
                || pendingDeployActionSourcePermanentCardId != null)
                && "CARD_SELECTION".equals(
                    decisionType.name())
                && promptLower.contains(
                    "choose where to deploy")) {
            if (pendingObjectiveDeployingCardId != null) {
                evalContext.setExtra(
                    ObjectiveAnalyzer
                        .OBJECTIVE_DEPLOYING_CARD_ID_EXTRA,
                    pendingObjectiveDeployingCardId);
            }
            if (pendingDeployActionSourcePermanentCardId
                    != null) {
                evalContext.setExtra(
                    BhbmForceDripUrgencyFactsReader
                        .ACTION_SOURCE_PERMANENT_CARD_ID_EXTRA,
                    pendingDeployActionSourcePermanentCardId);
            }
            pendingObjectiveDeployingCardId = null;
            pendingDeployActionSourcePermanentCardId = null;
        } else if ((pendingObjectiveDeployingCardId != null
                || pendingDeployActionSourcePermanentCardId != null)
                && (phase != Phase.DEPLOY
                    || decisionType.name()
                        .contains("ACTION_CHOICE")
                    && !firstOrderNavyResponseWindow)) {
            pendingObjectiveDeployingCardId = null;
            pendingDeployActionSourcePermanentCardId = null;
        }
        evalContext.setActivationAmountDecision(activationAmountDecision);

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
            // Parse selectable array (for CARD_SELECTION decisions)
            String[] selectableArr = params.get("selectable");
            if (selectableArr != null && selectableArr.length > 0) {
                List<Boolean> selectableList = new java.util.ArrayList<>();
                int selectableCount = 0;
                for (String sel : selectableArr) {
                    boolean isSelectable = "true".equalsIgnoreCase(sel);
                    selectableList.add(isSelectable);
                    if (isSelectable) selectableCount++;
                }
                evalContext.setSelectable(selectableList);
                LOG.debug("📋 Selectable: {} of {} cards selectable", selectableCount, selectableList.size());

                // Only warn if ALL are non-selectable (unusual case)
                if (selectableCount == 0 && selectableList.size() > 0) {
                    LOG.warn("⚠️ ALL {} CARDS NON-SELECTABLE (verify decision?)", selectableList.size());
                }
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

            if ("MULTIPLE_CHOICE".equals(decisionType.name())
                    && promptLower.contains("capacity slot")) {
                String[] capacityResults = params.get("results");
                if (capacityResults != null
                        && capacityResults.length > 0) {
                    List<String> capacityIds =
                            new java.util.ArrayList<>();
                    List<String> capacityTexts =
                            new java.util.ArrayList<>();
                    for (int i = 0;
                            i < capacityResults.length; i++) {
                        capacityIds.add(String.valueOf(i));
                        String result = capacityResults[i];
                        capacityTexts.add(
                            (result != null ? result : "")
                                + " capacity slot");
                    }
                    evalContext.setActionIds(capacityIds);
                    evalContext.setActionTexts(
                        capacityTexts);
                }
            }

            if ("MULTIPLE_CHOICE".equals(
                    decisionType.name())
                    && promptLower.contains(
                        "choose regular move action")) {
                String[] moveResults =
                    params.get("results");
                if (moveResults != null
                        && moveResults.length > 0) {
                    List<String> moveIds =
                        new java.util.ArrayList<>();
                    List<String> moveTexts =
                        new java.util.ArrayList<>();
                    for (int i = 0;
                            i < moveResults.length; i++) {
                        moveIds.add(
                            String.valueOf(i));
                        moveTexts.add(
                            moveResults[i] != null
                                ? moveResults[i] : "");
                    }
                    evalContext.setActionIds(moveIds);
                    evalContext.setActionTexts(
                        moveTexts);
                }
            }

            if ("MULTIPLE_CHOICE".equals(
                    decisionType.name())
                    && (promptLower.contains(
                            "choose parsec to move to")
                        || promptLower.contains(
                            "choose destination for")
                            && promptLower.contains("parsec"))) {
                String[] routeResults = params.get("results");
                if (routeResults != null
                        && routeResults.length > 0) {
                    List<String> routeIds =
                        new java.util.ArrayList<>();
                    List<String> routeTexts =
                        new java.util.ArrayList<>();
                    for (int i = 0;
                            i < routeResults.length; i++) {
                        routeIds.add(String.valueOf(i));
                        routeTexts.add(
                            routeResults[i] != null
                                ? routeResults[i] : "");
                    }
                    evalContext.setActionIds(routeIds);
                    evalContext.setActionTexts(routeTexts);
                }
            }

            if ("MULTIPLE_CHOICE".equals(
                    decisionType.name())
                    && "choose an option".equals(
                        promptLower.trim())) {
                String[] destinyResults =
                    params.get("results");
                if (destinyResults != null
                        && destinyResults.length == 2
                        && "Add 1".equals(
                            destinyResults[0])
                        && "Subtract 1".equals(
                            destinyResults[1])) {
                    evalContext.setActionIds(
                        java.util.List.of("0", "1"));
                    evalContext.setActionTexts(
                        java.util.List.of(
                            destinyResults[0],
                            destinyResults[1]));
                }
            }

            // For CARD_ACTION_CHOICE: parse per-action cardId and blueprintId arrays
            // These tell us which card each action is associated with
            if ("CARD_ACTION_CHOICE".equals(decisionType.name())) {
                // cardId array - each action's associated card ID (gempId)
                String[] actionCardIds = params.get("cardId");
                // blueprintId array - "inPlay" or actual blueprint for virtual actions
                String[] actionBlueprintIds = params.get("blueprintId");

                LOG.warn("📋 CARD_ACTION_CHOICE: cardId={} items, blueprintId={} items",
                    actionCardIds != null ? actionCardIds.length : "null",
                    actionBlueprintIds != null ? actionBlueprintIds.length : "null");

                if (actionCardIds != null && actionCardIds.length > 0) {
                    List<String> cardIdList = new java.util.ArrayList<>();
                    for (String cid : actionCardIds) {
                        cardIdList.add(cid != null ? cid : "");
                    }
                    evalContext.setCardIds(cardIdList);
                    LOG.warn("📋 cardIds (gempIds): {}", cardIdList.size() <= 10 ? cardIdList : cardIdList.subList(0, 10) + "...");
                }

                if (actionBlueprintIds != null && actionBlueprintIds.length > 0) {
                    List<String> bpList = new java.util.ArrayList<>();
                    for (String bp : actionBlueprintIds) {
                        bpList.add(bp != null ? bp : "");
                    }
                    evalContext.setBlueprints(bpList);
                    LOG.warn("📋 blueprintIds: {}", bpList.size() <= 10 ? bpList : bpList.subList(0, 10) + "...");
                }

                // Log action details with cardId and blueprintId for each action
                String[] logActionTexts = params.get("actionText");
                if (logActionTexts != null) {
                    LOG.warn("📋 {} actions to evaluate:", logActionTexts.length);
                    for (int i = 0; i < Math.min(logActionTexts.length, 10); i++) {
                        String cardId = (actionCardIds != null && i < actionCardIds.length) ? actionCardIds[i] : "n/a";
                        String bpId = (actionBlueprintIds != null && i < actionBlueprintIds.length) ? actionBlueprintIds[i] : "n/a";
                        String actionText = logActionTexts[i] != null ? logActionTexts[i] : "";
                        LOG.warn("   [{}] cardId={}, bp={}, action='{}'", i, cardId, bpId,
                            actionText.length() > 50 ? actionText.substring(0, 50) + "..." : actionText);
                    }
                }
            }

            ActivateDecisionRouting.ChoiceLabels zeroConfirmation =
                ActivateDecisionRouting.zeroConfirmationChoices(
                    decisionType.name(), decision.getText(), params.get("results"));
            if (zeroConfirmation.isPresent()) {
                evalContext.setActionIds(zeroConfirmation.actionIds());
                evalContext.setActionTexts(zeroConfirmation.actionTexts());
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
        // V295 RETIRED: evalContext.setObjectiveHandler(objectiveHandler);
        evalContext.setObjectiveAnalyzer(objectiveAnalyzer);

        if (!objectiveAnalyzer.isAnalyzed() && currentGame != null && mySide != null) {
            objectiveAnalyzer.analyze(currentGame, playerId, mySide);
        } else if (objectiveAnalyzer.isAnalyzed() && currentGame != null) {
            // V29.7: Refresh flip status each evaluation so we detect when objective actually flips
            objectiveAnalyzer.refreshFlipStatus(currentGame.getGameState(), playerId);
        }
        if (decisionType
                == AwaitingDecisionType.CARD_ACTION_CHOICE
                && phase == Phase.DEPLOY
                && currentGame != null) {
            evalContext.setExtra(
                CaptureDeployBudgetFactsReader
                    .ACTION_PAYMENTS_EXTRA,
                CaptureDeployBudgetFactsReader
                    .snapshotExactNormalDeployPayments(
                        decision, currentGame,
                        playerId));
        }
        evalContext.setShieldStrategy(shieldStrategy);
        deployPhasePlanner.setObjectiveAnalyzer(objectiveAnalyzer);
        evalContext.setDeployPhasePlanner(deployPhasePlanner);

        // V22.6: DeckOracle — full deck knowledge
        if (!deckOracle.isAnalyzed() && currentGame != null) {
            deckOracle.analyze(currentGame, playerId, mySide);
        }
        deckOracle.refresh(gameState, playerId);
        evalContext.setDeckOracle(deckOracle);

        // V24.7: OpponentDeckTracker — destiny intel from deck peeks
        evalContext.setOpponentDeckTracker(opponentDeckTracker);

        // V29.15: Pass deck name for saga-aware Epic Event choices
        evalContext.setDeckName(deckName);

        // V67ax DEPLOY PHASE SCRIPT: deterministic step ordering during DEPLOY phase.
        // Walk steps 1→5; restrict the evaluator pipeline to actions qualifying for
        // the first non-empty step. Existing scoring (V67ai/aj/ak/al/aq/ar/as) picks
        // within the qualifying set. Active only for CARD_ACTION_CHOICE during DEPLOY.
        if (phase == Phase.DEPLOY
                && "CARD_ACTION_CHOICE".equals(decisionType.name())
                && currentGame != null) {
            try {
                DeployPhaseScript.Result dpsResult = deployPhaseScript.selectAllowedActions(
                    decision, gameState, currentGame, playerId, objectiveAnalyzer);
                if (dpsResult != null) {
                    evalContext.setAllowedActionIds(dpsResult.allowedActionIds);
                    evalContext.setAllowedActionsReason(dpsResult.reason);
                    // V67bc: pass ordered hierarchy buckets so CombinedEvaluator
                    // can walk top→bottom and pick the first action above the
                    // bad threshold (instead of forcing PASS when STEP 1's only
                    // candidate is hard-blocked).
                    evalContext.setStepBuckets(dpsResult.stepBuckets);
                    evalContext.setStepBucketLabels(dpsResult.stepBucketLabels);
                    LOG.warn("V67bc DPS APPLIED: top-step={} buckets={} union={} reason='{}'",
                        dpsResult.step,
                        dpsResult.stepBucketLabels,
                        dpsResult.allowedActionIds != null ? dpsResult.allowedActionIds.size() : 0,
                        dpsResult.reason);
                }
            } catch (Exception e) {
                LOG.warn("V67ax DPS error (non-fatal, falling through to scoring): {}",
                    e.getMessage());
            }
        }

        Map<String, Integer> tdigwattDestinySources =
            TdigwattObjectiveFactsReader
                .readDestinyAdjustmentActionSources(
                    decision, currentGame, playerId);
        if (!tdigwattDestinySources.isEmpty()) {
            evalContext.setExtra(
                TdigwattObjectiveFactsReader
                    .DESTINY_ADJUSTMENT_ACTION_SOURCES_EXTRA,
                tdigwattDestinySources);
        }
        Map<String, Integer> tdigwattPullSources =
            TdigwattObjectiveFactsReader
                .readPullActionSources(
                    decision, currentGame, playerId);
        if (!tdigwattPullSources.isEmpty()) {
            evalContext.setExtra(
                TdigwattObjectiveFactsReader
                    .PULL_ACTION_SOURCES_EXTRA,
                tdigwattPullSources);
        }

        return evalContext;
    }

    /**
     * Set the current game reference for advanced analysis.
     * Called by game mediator before decisions.
     */
    public void setCurrentGame(SwccgGame game) {
        // V194: game-scoped planning facts must not survive a rematch.
        if (this.currentGame != game) {
            activationAmountLatch.reset();
            // V295 RETIRED: objectiveHandler.reset();
            objectiveAnalyzer.reset();
            deployPhasePlanner.reset();
            deckOracle.reset();
        }
        this.currentGame = game;
    }

    /**
     * Implementation of SwccgAiController interface method.
     * Delegates to setCurrentGame for backward compatibility.
     */
    @Override
    public void setGame(SwccgGame game) {
        setCurrentGame(game);
    }

    /**
     * Set the BotStatsDAO for record lookups in welcome messages.
     * @param dao the bot stats DAO
     */
    public void setBotStatsDAO(com.gempukku.swccgo.db.BotStatsDAO dao) {
        this.botStatsDAO = dao;
    }

    /**
     * V29.15: Set the deck name for saga-aware decisions (Epic Event choices).
     * @param deckName the deck name as stored in the database
     */
    @Override
    public void setDeckName(String deckName) {
        this.deckName = deckName;
        LOG.info("V29.15 Deck name set: '{}'", deckName);
    }

    /**
     * Get pending chat message if available.
     * @return message to send, or null
     */
    public String getChatMessage() {
        String msg = chatManager.getNextMessage();
        if (msg != null) {
            LOG.info("🗨️ getChatMessage returning: '{}'", msg.length() > 50 ? msg.substring(0, 50) + "..." : msg);
        }
        return msg;
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
    protected boolean isLegacyFailedSearchMemoryEnabled() {
        // V194: the engine's turn-scoped search modifier is authoritative for Rando.
        return false;
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
        score += ResponsePolicy.scorePriorityCards(actionLower, decisionText);

        score += CoordinatorPosturePolicy.score(
                context.behindOnLifeForce(),
                context.aheadOnBoard(),
                context.behindOnBoard(),
                actionLower.contains("force drain"),
                actionLower.contains("initiate battle"),
                actionLower.contains("deploy") || actionLower.contains("draw"),
                context.matchesHandTitle(actionLower));

        return score;
    }

    // =========================================================================
    // Phase-Specific Scoring
    // =========================================================================

    private int scoreDeployAction(String actionText, String decisionText) {
        int score = 0;

        // Deploying locations is high priority (opens options)
        if (actionText.contains("deploy") && actionText.contains("location")) {
            score += DeployActionTextPolicy.scoreLegacyFallbackDeployLocation(
                    RandoConfig.SCORE_DEPLOY_LOCATION);
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
                        float powerDiff = loc.getPowerAdvantage();
                        score += DeployActionTextPolicy.scoreLegacyFallbackReinforce(
                                RandoConfig.SCORE_REINFORCE_LOSING,
                                powerDiff, loc.isBattleground);
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
                    score += DeployActionTextPolicy.scoreLegacyFallbackGainGround(
                            RandoConfig.SCORE_GAIN_GROUND,
                            loc.theirForceIcons > 0,
                            loc.isBattleground,
                            loc.theirPower);
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

                boolean matchingDomain = false;
                if (isCharacterDeploy && loc.isGround()) {
                    matchingDomain = true;
                } else if (isStarshipDeploy && loc.isSpace()) {
                    matchingDomain = true;
                }
                score += DeployActionTextPolicy.scoreLegacyFallbackDomainMatch(
                        matchingDomain,
                        loc.status == ContestStatus.EMPTY && loc.ourForceIcons == 0);
                break;
            }
        }

        // Matching pilot bonus
        if (actionText.contains("pilot") && actionText.contains("matching")) {
            score += DeployActionTextPolicy.scoreLegacyFallbackMatchingPilot(
                    RandoConfig.SCORE_MATCHING_PILOT);
        }

        return score;
    }

    private int scoreControlAction(String actionText, String decisionText) {
        int score = 0;

        // Force drain is primary control phase action
        if (actionText.contains("force drain")) {
            int controlledBattlegrounds = 0;

            // Extra bonus if we control battlegrounds
            if (currentGame != null && context != null && mySide != null) {
                List<LocationAnalysis> controlled = AiBoardAnalyzer.getControlledBattlegrounds(
                    currentGame, context.playerId, context.opponentId, mySide);
                if (!controlled.isEmpty()) {
                    controlledBattlegrounds = controlled.size();
                }
            }

            score += ControlDrainAssessment.scoreLegacyFallback(
                RandoConfig.SCORE_FORCE_DRAIN, controlledBattlegrounds);
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
                    score += BattleActionTextPolicy.scoreLegacyFallbackLocation(
                        RandoConfig.SCORE_INITIATE_BATTLE,
                        RandoConfig.BATTLE_FAVORABLE_THRESHOLD,
                        RandoConfig.BATTLE_DANGER_THRESHOLD,
                        powerAdvantage,
                        loc.isBattleground,
                        loc.isContested() && loc.status == ContestStatus.WINNING);
                    break;
                }

                // Fallback to overall board advantage if no specific location found
                if (score == 0) {
                    float boardAdvantage = AiBoardAnalyzer.calculateBoardAdvantage(
                        currentGame, context.playerId, context.opponentId, mySide);
                    score += BattleActionTextPolicy.scoreLegacyFallbackBoard(
                        RandoConfig.SCORE_INITIATE_BATTLE,
                        RandoConfig.BATTLE_FAVORABLE_THRESHOLD,
                        RandoConfig.BATTLE_DANGER_THRESHOLD,
                        boardAdvantage);
                }
            }
        }

        // Weapon firing
        if (actionText.contains("fire") && actionText.contains("weapon")) {
            score += BattleWeaponsPolicy.scoreLegacyFallbackFireWeapon();
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
        if (gameState == null) {
            LOG.warn("🔴 trackGameState: gameState is NULL!");
            return;
        }

        // Log every call to understand tracking flow
        int rawTurn = gameState.getPlayersLatestTurnNumber(playerId);
        LOG.info("🔵 trackGameState called: playerId={}, rawTurn={}, lastTurn={}, lastPhase={}",
            playerId, rawTurn, lastTurn, lastPhase);

        // Detect new game by checking if side/opponent changed
        Side newSide = gameState.getSide(playerId);
        String newOpponent = gameState.getOpponent(playerId);

        // New game started (opponent or side changed)
        if (mySide == null || !newOpponent.equals(opponentName)) {
            lastTurn = -1;
            lastPhase = null;  // Reset phase tracking for new game
            boolean tracePendingBefore = pendingConcede;
            String traceReasonBefore = pendingConcedeReason;
            pendingConcede = false;  // V67aw: Reset concede defer flag for new game
            pendingConcedeReason = null;
            // TRACE 4A1: typed CLEAR_PENDING (new-game clear cause) observed after the
            // legacy writes. The clear runs unconditionally, so a NO_OP is a real
            // observation. TraceSession self-guards: no open session, no event.
            TraceSession.recordPendingConcede(PendingConcedeEvent.Operation.CLEAR_PENDING,
                PendingConcedeEvent.Cause.NEW_GAME_RESET, playerId,
                null, null, null, tracePendingBefore, traceReasonBefore, false, null);
            battleMessageSentThisBattle = false;  // Reset battle message tracking
            gameEndMessageSent = false;  // Reset game end message tracking
            mySide = newSide;
            opponentName = newOpponent;
            currentGameId = playerId + "_" + System.currentTimeMillis();

            chatManager.resetForGame(currentGameId);
            // TRACE 4A2a ("Hook law" + "For CLEAR"): observe the one outer new-game
            // tracker CLEAR without moving it — source order preserved (pending-concede
            // clear above, outer new-game writes, chat reset, THIS tracker clear,
            // seen-set clears, then strategy-component resets). Snapshot builds are
            // instrumentation-only; failure marks STATE_EVENT and never skips, repeats,
            // or alters the legacy clear. Fresh-tracker NO_OP is a real observation.
            DecisionTrackerLifecycleSnapshot traceClearBefore = null;
            if (TraceSession.isActive()) {
                try {
                    traceClearBefore = decisionTracker.traceLifecycleSnapshot();
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "CLEAR before-snapshot failed; legacy clear unaffected");
                }
            }
            decisionTracker.clear();  // Clear loop tracking for new game
            if (traceClearBefore != null) {
                try {
                    TraceSession.recordTrackerClear(TrackerOwner.OUTER_RANDO,
                        TrackerClearEvent.ClearCause.NEW_GAME_RESET,
                        traceClearBefore, decisionTracker.traceLifecycleSnapshot());
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "CLEAR after-snapshot/record failed; legacy clear already ran");
                }
            }
            seenOpponentCards.clear();  // Clear opponent card tracking
            seenOwnShields.clear();  // Clear own shield tracking for pacing

            // Reset and update strategy components with new side
            strategyController.setSide(mySide);
            strategyController.reset();
            // V295 RETIRED: objectiveHandler.reset();
            objectiveAnalyzer.reset();
            shieldStrategy.setSide(mySide);
            shieldStrategy.reset();
            deployPhasePlanner.reset();
            deckOracle.reset();  // V22.6: Reset deck knowledge for new game
            opponentDeckTracker.reset();  // V24.7: Reset opponent intel for new game
            LOG.debug("[RandoCalAi] All strategy components reset for new game as {} side", mySide);

            // Run game-start verification
            runGameStartVerification(playerId, gameState);

            // Queue welcome message
            if (personality != null && RandoConfig.CHAT_ENABLED) {
                // Try to get current records for welcome message
                String damageRecordHolder = null;
                int damageRecordValue = 0;
                String routeRecordHolder = null;
                int routeRecordValue = 0;

                if (botStatsDAO != null) {
                    try {
                        com.gempukku.swccgo.db.vo.LeaderboardEntry damageRecord = botStatsDAO.getBestDamageRecord();
                        if (damageRecord != null) {
                            damageRecordHolder = damageRecord.getPlayerName();
                            damageRecordValue = damageRecord.getValue();
                        }
                        com.gempukku.swccgo.db.vo.LeaderboardEntry routeRecord = botStatsDAO.getBestRouteScoreRecord();
                        if (routeRecord != null) {
                            routeRecordHolder = routeRecord.getPlayerName();
                            routeRecordValue = routeRecord.getValue();
                        }
                    } catch (Exception e) {
                        LOG.debug("Could not fetch records for welcome: {}", e.getMessage());
                    }
                }

                String welcome = personality.getWelcomeMessage(opponentName, mySide,
                    damageRecordHolder, damageRecordValue, routeRecordHolder, routeRecordValue);
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

            LOG.info("🎲 Turn changed to {} (was {})", currentTurn, lastTurn - 1);

            // Queue turn message with route score
            LOG.info("🗨️ Turn message check: personality={}, CHAT_ENABLED={}, turn={}",
                personality != null, RandoConfig.CHAT_ENABLED, currentTurn);
            if (personality != null && RandoConfig.CHAT_ENABLED && currentTurn >= 2) {
                // Get life force for route score calculation
                int myLifeForce = 0;
                int opponentLifeForce = 0;
                try {
                    myLifeForce = gameState.getPlayerLifeForce(playerId);
                    opponentLifeForce = gameState.getPlayerLifeForce(newOpponent);
                } catch (Exception e) {
                    LOG.warn("Could not get life force for turn message: {}", e.getMessage());
                }

                LOG.info("🗨️ Getting turn message: turn={}, myLF={}, theirLF={}",
                    currentTurn, myLifeForce, opponentLifeForce);
                String turnMessage = personality.getTurnMessage(currentTurn, myLifeForce, opponentLifeForce);
                if (turnMessage != null) {
                    chatManager.queueTurnMessage(turnMessage);
                    LOG.info("🗨️ Queued turn message: {}", turnMessage);
                } else {
                    LOG.info("🗨️ getTurnMessage returned null (random skip or turn < 3)");
                }
            }

            // Confirm any pending deploy from last turn succeeded (strategy learning)
            if (lastPendingDeployType != null) {
                strategyController.onSuccessfulDeploy(lastPendingDeployType);
                String traceDeployTypeBefore = lastPendingDeployType;
                lastPendingDeployType = null;
                // TRACE 4A1: typed PENDING_DEPLOY CLEAR at the direct-write site. The
                // onSuccessfulDeploy call above stays unobserved until the
                // StrategyController owner increment (4B).
                TraceSession.recordPendingDeploy(PendingDeployEvent.Operation.CLEAR,
                    traceDeployTypeBefore, null);
            }
        }

        // Track opponent cards for situational shield decisions
        trackOpponentCards(gameState, newOpponent);

        // Track our own shields for pacing (so we don't deploy all 4 on turn 1)
        trackOwnShields(gameState, playerId);

        // Check for Battle Order/Plan cards in play and update strategy
        // This enables proper force drain cost calculation (+3 when under Battle Order rules)
        strategyController.updateBattleOrderFromGameState(gameState);

        // Track phase changes for battle message
        Phase currentPhase = gameState.getCurrentPhase();

        // Reset battle message flag when exiting battle phase
        if (lastPhase == Phase.BATTLE && currentPhase != Phase.BATTLE) {
            LOG.info("🗨️ Exiting BATTLE phase, resetting battle message flag");
            battleMessageSentThisBattle = false;

            // V67aw: Battle phase just ended — fire any deferred concede now.
            if (pendingConcede && currentGame != null) {
                LOG.warn("V67aw CONCEDE FIRE: battle phase ended — conceding now ({})",
                    pendingConcedeReason);
                // TRACE 4A1 (m00381 consolidation): ONE post-try/catch recorder — the
                // outcome variable captures SUCCESS (call returned; playerLost is
                // internally idempotent, so this is call-outcome not state-proof) or
                // THREW (caught Exception; the legacy catch is Exception, not
                // Throwable, so an escaping Error skips both the event and the clear,
                // exactly as source does). TraceSession never throws.
                EngineCallOutcome traceLostOutcome;
                try {
                    currentGame.playerLost(playerId,
                        com.gempukku.swccgo.common.GameEndReason.LOSS__CONCEDED);
                    traceLostOutcome = EngineCallOutcome.SUCCESS;
                } catch (Exception e) {
                    LOG.warn("V67aw CONCEDE FIRE: error during concede: {}", e.getMessage());
                    traceLostOutcome = EngineCallOutcome.THREW;
                }
                TraceSession.recordEnginePlayerLost(playerId,
                    com.gempukku.swccgo.common.GameEndReason.LOSS__CONCEDED,
                    traceLostOutcome);
                boolean tracePendingBefore = pendingConcede;  // structurally true on this branch
                String traceReasonBefore = pendingConcedeReason;
                pendingConcede = false;
                pendingConcedeReason = null;
                // TRACE 4A1: CLEAR_PENDING recorded after the catch, preserving the real
                // source order PLAYER_LOST(SUCCESS|THREW) then CLEAR_PENDING.
                TraceSession.recordPendingConcede(PendingConcedeEvent.Operation.CLEAR_PENDING,
                    PendingConcedeEvent.Cause.POST_PLAYER_LOST, playerId,
                    null, null, null, tracePendingBefore, traceReasonBefore, false, null);
            }
        }

        // Try to send battle message when in BATTLE phase (BattleState might not exist on phase entry)
        if (currentPhase == Phase.BATTLE) {
            PhysicalCard battleLoc = gameState.getBattleLocation();
            LOG.info("🗨️ BATTLE phase check: alreadySent={}, battleLocation={}",
                battleMessageSentThisBattle, battleLoc != null ? battleLoc.getTitle() : "NULL");

            if (!battleMessageSentThisBattle && battleLoc != null) {
                sendBattleMessage(playerId, gameState);
                battleMessageSentThisBattle = true;
            }
        }

        // Check for game end and send message
        if (!gameEndMessageSent && currentGame != null) {
            String winner = currentGame.getWinner();
            if (winner != null || currentGame.isFinished()) {
                sendGameEndMessage(playerId, gameState, winner);
                gameEndMessageSent = true;
            }
        }

        lastPhase = currentPhase;
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
     * Track our own defensive shields as they are played.
     * This enables shield pacing - we don't want to deploy all 4 shields on turn 1.
     * ShieldStrategy uses this to limit how many shields we play early.
     */
    private void trackOwnShields(GameState gameState, String playerId) {
        if (gameState == null || playerId == null) return;

        try {
            for (PhysicalCard card : gameState.getAllPermanentCards()) {
                if (card == null) continue;
                if (!playerId.equals(card.getOwner())) continue;

                Zone zone = card.getZone();
                // Defensive shields go to the SIDE_OF_TABLE zone
                if (zone != Zone.SIDE_OF_TABLE) continue;

                String title = card.getTitle();
                if (title == null) continue;

                SwccgCardBlueprint blueprint = card.getBlueprint();
                if (blueprint == null || blueprint.getCardCategory() != CardCategory.DEFENSIVE_SHIELD) {
                    continue;
                }

                // Check if this is a new shield we haven't recorded
                String blueprintId = card.getBlueprintId(true);
                String shieldKey = card.getCardId() + "_" + blueprintId;
                if (!seenOwnShields.contains(shieldKey)) {
                    seenOwnShields.add(shieldKey);
                    shieldStrategy.recordShieldPlayed(blueprintId, title);
                    LOG.info("🛡️ Tracked own shield: {} ({})", title, blueprintId);
                }
            }
        } catch (Exception e) {
            LOG.debug("Error tracking own shields: {}", e.getMessage());
        }
    }

    /**
     * Send a battle commentary message when battle phase is entered.
     * Uses power totals to generate appropriate commentary.
     */
    private void sendBattleMessage(String playerId, GameState gameState) {
        if (personality == null || !RandoConfig.CHAT_ENABLED) {
            return;
        }

        try {
            // Get the battle location (caller should have verified this exists)
            PhysicalCard battleLocation = gameState.getBattleLocation();
            if (battleLocation == null) {
                return;
            }

            LOG.info("🗨️ Sending battle message for battle at {}", battleLocation.getTitle());

            // Get power totals at battle location
            String opponentId = gameState.getOpponent(playerId);
            float ourPower = 0;
            float theirPower = 0;

            if (currentGame != null) {
                // Use modifiers querying to get accurate power totals
                try {
                    ourPower = currentGame.getModifiersQuerying().getTotalPowerAtLocation(
                        gameState, battleLocation, playerId, false, false);
                    theirPower = currentGame.getModifiersQuerying().getTotalPowerAtLocation(
                        gameState, battleLocation, opponentId, false, false);
                } catch (Exception e) {
                    // Fallback to simple card counting
                    LOG.debug("Could not get power totals, using card count fallback: {}", e.getMessage());
                    for (PhysicalCard card : gameState.getAllPermanentCards()) {
                        if (card == null) continue;
                        Zone zone = card.getZone();
                        if (zone == null || !zone.isInPlay()) continue;

                        PhysicalCard cardLocation = card.getAtLocation();
                        if (cardLocation == null || !cardLocation.equals(battleLocation)) continue;

                        SwccgCardBlueprint bp = card.getBlueprint();
                        if (bp == null) continue;
                        CardCategory cat = bp.getCardCategory();
                        if (cat != CardCategory.CHARACTER && cat != CardCategory.STARSHIP && cat != CardCategory.VEHICLE) continue;

                        Float power = bp.getPower();
                        if (power == null) power = 0f;

                        if (playerId.equals(card.getOwner())) {
                            ourPower += power;
                        } else if (opponentId != null && opponentId.equals(card.getOwner())) {
                            theirPower += power;
                        }
                    }
                }
            }

            LOG.debug("Battle at {}: our power={}, their power={}",
                battleLocation.getTitle(), ourPower, theirPower);

            // Get battle message from personality
            String message = personality.getBattleMessage(ourPower, theirPower);
            if (message != null) {
                chatManager.queueBattleMessage(message);
                LOG.debug("Queued battle message: {}", message);
            }
        } catch (Exception e) {
            LOG.debug("Error sending battle message: {}", e.getMessage());
        }
    }

    /**
     * Send a game end message when the game finishes.
     * Calculates route score and sends personality-based message.
     */
    private void sendGameEndMessage(String playerId, GameState gameState, String winner) {
        if (personality == null || !RandoConfig.CHAT_ENABLED) {
            return;
        }

        try {
            // Determine if bot won
            boolean botWon = playerId.equals(winner);

            // Calculate route score: (opponent_lifeforce - my_lifeforce) - turns
            // Higher is better for opponent (they beat the bot more decisively)
            int myLifeForce = 0;
            int opponentLifeForce = 0;
            int turns = gameState != null ? gameState.getPlayersLatestTurnNumber(playerId) : 0;

            if (gameState != null) {
                try {
                    myLifeForce = gameState.getPlayerLifeForce(playerId);
                    String opponentId = gameState.getOpponent(playerId);
                    if (opponentId != null) {
                        opponentLifeForce = gameState.getPlayerLifeForce(opponentId);
                    }
                } catch (Exception e) {
                    LOG.debug("Could not get life force for game end message: {}", e.getMessage());
                }
            }

            int routeScore = (opponentLifeForce - myLifeForce) - turns;

            LOG.info("🏁 Game ended: winner={}, botWon={}, routeScore={} (oppLF={}, myLF={}, turns={})",
                winner, botWon, routeScore, opponentLifeForce, myLifeForce, turns);

            // Get game end message from personality
            String message = personality.getGameEndMessage(botWon, routeScore);
            if (message != null) {
                chatManager.queueGameEndMessage(message);
                LOG.info("🗨️ Queued game end message: {}", message);
            }
        } catch (Exception e) {
            LOG.warn("Error sending game end message: {}", e.getMessage());
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
            // TRACE 4A1: PENDING_DEPLOY SET is recorded at each actual direct write
            // (exact legacy value before/after); no branch write means no event. A
            // same-value rewrite is a real NO_OP SET.
            String traceDeployTypeBefore = lastPendingDeployType;
            // Determine card type from decision text
            if (textLower.contains("starship") || textLower.contains("capital ship")) {
                lastPendingDeployType = "starship";
                TraceSession.recordPendingDeploy(PendingDeployEvent.Operation.SET,
                    traceDeployTypeBefore, lastPendingDeployType);
            } else if (textLower.contains("vehicle")) {
                lastPendingDeployType = "vehicle";
                TraceSession.recordPendingDeploy(PendingDeployEvent.Operation.SET,
                    traceDeployTypeBefore, lastPendingDeployType);
            } else if (textLower.contains("character") || textLower.contains("alien") ||
                       textLower.contains("droid") || textLower.contains("jedi") ||
                       textLower.contains("imperial") || textLower.contains("rebel")) {
                lastPendingDeployType = "character";
                TraceSession.recordPendingDeploy(PendingDeployEvent.Operation.SET,
                    traceDeployTypeBefore, lastPendingDeployType);
            } else if (textLower.contains("site") || textLower.contains("system")) {
                lastPendingDeployType = "location";
                TraceSession.recordPendingDeploy(PendingDeployEvent.Operation.SET,
                    traceDeployTypeBefore, lastPendingDeployType);
            }
        }

        // Track battle results from decision text
        // Battle result prompts typically contain "won" or "lost"
        // TRACE 4A1: strategyController.onBattleResult stays UNOBSERVED here; its event lands with the StrategyController owner increment (4B).
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

    // V295 RETIRED: private boolean shouldApplyChaos() {
    // V295 RETIRED:     return random.nextInt(100) < RandoConfig.CHAOS_PERCENT;
    // V295 RETIRED: }

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
