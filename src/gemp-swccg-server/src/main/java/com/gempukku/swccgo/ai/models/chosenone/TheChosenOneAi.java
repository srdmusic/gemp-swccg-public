package com.gempukku.swccgo.ai.models.chosenone;

import com.gempukku.swccgo.ai.AiDecisionResult;
import com.gempukku.swccgo.ai.DecisionRejectionKind;
import com.gempukku.swccgo.ai.models.HeuristicAiBase;
import com.gempukku.swccgo.ai.models.common.decision.DecisionSnapshot;
import com.gempukku.swccgo.ai.models.common.decision.FactValue;
import com.gempukku.swccgo.ai.models.common.finalization.RejectionHistory;
import com.gempukku.swccgo.ai.models.common.phase.DrawPhaseOwner;
import com.gempukku.swccgo.ai.models.common.phase.DrawRoute;
import com.gempukku.swccgo.ai.models.common.phase.DrawRouteInput;
import com.gempukku.swccgo.ai.models.common.phase.DrawRouteResolver;
import com.gempukku.swccgo.ai.models.common.phase.RevertApprovalPhaseOwner;
import com.gempukku.swccgo.ai.models.common.phase.PullAssessment;
import com.gempukku.swccgo.ai.models.common.phase.PullFacts;
import com.gempukku.swccgo.ai.models.common.phase.PullPhaseOwner;
import com.gempukku.swccgo.ai.models.common.phase.PullRoute;
import com.gempukku.swccgo.ai.models.common.phase.PullRouteInput;
import com.gempukku.swccgo.ai.models.common.phase.PullRouteResolver;
import com.gempukku.swccgo.ai.models.common.trace.TraceFinalization;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.ContestStatus;
import com.gempukku.swccgo.ai.common.AiBoardAnalyzer.LocationAnalysis;
import com.gempukku.swccgo.ai.common.AiChatManager;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.chosenone.evaluators.CombinedEvaluator;
import com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionType;
import com.gempukku.swccgo.ai.models.chosenone.evaluators.DecisionContext;
import com.gempukku.swccgo.ai.models.chosenone.evaluators.EvaluatedAction;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.chosenone.strategy.DeployPhaseScript;
import com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveAnalyzer;
import com.gempukku.swccgo.ai.models.chosenone.strategy.ObjectiveHandler;
import com.gempukku.swccgo.ai.models.chosenone.strategy.ShieldStrategy;
import com.gempukku.swccgo.ai.models.chosenone.strategy.StrategyController;
import com.gempukku.swccgo.ai.models.chosenone.strategy.StrategyControllerTraceAccess;
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
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyControllerOwner;
import com.gempukku.swccgo.ai.models.common.trace.state.StrategyControllerSnapshot;
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
public class TheChosenOneAi extends HeuristicAiBase {

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
    private final ObjectiveAnalyzer objectiveAnalyzer;

    // Shield strategy for defensive shields
    private final ShieldStrategy shieldStrategy;

    // Deploy phase planner for holistic deployment plans
    private final DeployPhasePlanner deployPhasePlanner;

    // V67ax DEPLOY PHASE SCRIPT (deterministic step ordering)
    private final DeployPhaseScript deployPhaseScript;

    // V22.6: DeckOracle for full deck knowledge
    private final com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle deckOracle;

    // V24.7: OpponentDeckTracker for destiny intel from deck peeks
    private final com.gempukku.swccgo.ai.models.chosenone.strategy.OpponentDeckTracker opponentDeckTracker;

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

    public TheChosenOneAi() {
        this.chatManager = new AiChatManager();
        this.combinedEvaluator = new CombinedEvaluator();
        this.decisionTracker = new DecisionTracker();
        this.strategyController = new StrategyController();
        this.objectiveHandler = new ObjectiveHandler();
        this.objectiveAnalyzer = new ObjectiveAnalyzer();
        this.shieldStrategy = new ShieldStrategy();
        this.deployPhasePlanner = new DeployPhasePlanner();
        this.deployPhaseScript = new DeployPhaseScript();
        this.deckOracle = new com.gempukku.swccgo.ai.models.chosenone.strategy.DeckOracle();
        this.opponentDeckTracker = new com.gempukku.swccgo.ai.models.chosenone.strategy.OpponentDeckTracker();
        this.personality = new AstrogatorPersonality();
        this.holidayOverlay = HolidayOverlay.getInstance();
        LOG.info("TheChosenOneAi initialized with {} evaluators", combinedEvaluator.getEvaluators().size());

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
            if (eval instanceof com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionEvaluator) {
                String name = ((com.gempukku.swccgo.ai.models.chosenone.evaluators.ActionEvaluator) eval).getName();
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

    // ═══════════════════════════════════════════════════════════
    // ═══ FINALIZER RUNTIME (2026-07-13,
    //     Handoffs/CODEX_FINALIZER_RUNTIME_PREREQUISITE_PACKET_2026-07-13.md §4) ═══
    // Exact mirror of RandoCalAi's split (typed owner label OUTER_CHOSENONE aside): decide()
    // computes + applies the outer common mutation inline + closes the trace inline;
    // decideForEngine() defers both to the synchronous disposition callback. Trace close is
    // CALL-PATH-aware, never mutation-mode-aware — a mediator-facing NONE interceptor keeps
    // the trace open until a disposition callback closes it.
    // ═══════════════════════════════════════════════════════════

    /** Immutable carrier from the shared computation: the wire plus its accepted-mutation mode. */
    private static final class OuterDecision {
        final String wireResponse;
        final AiDecisionResult.MutationMode mode;
        final AiDecisionResult engineResult;
        OuterDecision(String wireResponse, AiDecisionResult.MutationMode mode) {
            this.wireResponse = wireResponse;
            this.mode = mode;
            this.engineResult = null;
        }
        OuterDecision(AiDecisionResult engineResult) {
            this.engineResult = engineResult;
            this.wireResponse = engineResult.status() == AiDecisionResult.Status.WIRE_RESPONSE
                ? engineResult.wireResponse() : null;
            this.mode = engineResult.mutationMode();
        }
    }

    @Override
    public String decide(String playerId, AwaitingDecision decision, GameState gameState) {
        OuterDecision computed = computeDecision(playerId, decision, gameState,
            RejectionHistory.empty(), false);
        if (computed.engineResult != null
                && computed.engineResult.status() == AiDecisionResult.Status.TYPED_REJECTION) {
            throw new IllegalStateException("typed decision rejection: "
                + computed.engineResult.rejectionCode() + ": "
                + computed.engineResult.rejectionDetail());
        }
        return computed.wireResponse;
    }

    @Override
    public AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision,
                                            GameState gameState) {
        return decideForEngine(playerId, decision, gameState, RejectionHistory.empty());
    }

    @Override
    public AiDecisionResult decideForEngine(String playerId, AwaitingDecision decision,
                                            GameState gameState, RejectionHistory history) {
        // Owned DRAW, PULL, and V44/V67j routes consume the exact immutable history.
        // Legacy routes carry it without modifying or persisting it.
        OuterDecision computed = computeDecision(playerId, decision, gameState, history, true);
        if (computed.engineResult != null) {
            return computed.engineResult;
        }
        return AiDecisionResult.wire(computed.wireResponse, computed.mode,
            String.valueOf(decision.getAwaitingDecisionId()));
    }

    @Override
    public void onDecisionAccepted(String playerId, AwaitingDecision decision,
                                   GameState gameState, AiDecisionResult result) {
        applyAcceptedDisposition(decision, result);
    }

    @Override
    public void onDecisionRejected(String playerId, AwaitingDecision decision,
                                   GameState gameState, AiDecisionResult result,
                                   DecisionRejectionKind kind, String detail) {
        applyRejectedDisposition(result, kind, detail);
    }

    @Override
    public void onDecisionAttemptFailed(String playerId, AwaitingDecision decision,
                                        GameState gameState, String detail) {
        applyAttemptFailedDisposition(detail);
    }

    /** FINALIZER RUNTIME §4: accepted callback — mirror of RandoCalAi.applyAcceptedDisposition. */
    private void applyAcceptedDisposition(AwaitingDecision decision, AiDecisionResult result) {
        boolean traceActive = TraceSession.isActive();
        boolean outerCommon = result.mutationMode() == AiDecisionResult.MutationMode.OUTER_COMMON;
        String wire = result.wireResponse();
        String decisionType = decision.getDecisionType() != null
            ? decision.getDecisionType().name() : "UNKNOWN";
        String decisionText = decision.getText() != null ? decision.getText() : "";
        boolean mutationCompleted = false;
        RuntimeException mutationFault = null;
        try {
            if (outerCommon) {
                applyOuterCommonTrackerAndStrategic(decision, decisionType, decisionText, wire, traceActive);
                mutationCompleted = true;
            }
        } catch (RuntimeException e) {
            mutationFault = e;
        } finally {
            if (traceActive) {
                try {
                    TraceSession.recordProposedWire(wire);
                    TraceSession.recordEngineDisposition(TraceFinalization.Disposition.ENGINE_ACCEPTED,
                        outerCommon ? TraceFinalization.MutationMode.OUTER_COMMON
                                    : TraceFinalization.MutationMode.NONE,
                        mutationCompleted,
                        mutationFault == null ? null
                            : "outer accepted mutation failed: " + mutationFault.getClass().getName());
                    TraceSession.recordFinalResponse(wire, !outerCommon);
                    if (mutationFault != null) {
                        TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                            mutationFault.getClass().getName(),
                            "outer accepted mutation threw; accepted trace marked incomplete (mutation outcome false)");
                    }
                } finally {
                    TraceSession.closeAndEmit(decisionTraceSink);
                }
            }
            context = null;
        }
        if (mutationFault != null) {
            throw mutationFault;  // rethrow for mediator logging; disposition remains ENGINE_ACCEPTED
        }
    }

    /** FINALIZER RUNTIME §4: rejection callback — mirror of RandoCalAi.applyRejectedDisposition. */
    private void applyRejectedDisposition(AiDecisionResult result, DecisionRejectionKind kind,
                                          String detail) {
        boolean traceActive = TraceSession.isActive();
        try {
            if (traceActive) {
                boolean hasWire = result != null
                    && result.status() == AiDecisionResult.Status.WIRE_RESPONSE;
                if (hasWire) {
                    TraceSession.recordProposedWire(result.wireResponse());
                }
                TraceFinalization.Disposition disposition;
                switch (kind) {
                    case ENGINE_REJECTED: disposition = TraceFinalization.Disposition.ENGINE_REJECTED; break;
                    case TYPED_REJECTION: disposition = TraceFinalization.Disposition.TYPED_REJECTION; break;
                    default:              disposition = TraceFinalization.Disposition.ATTEMPT_FAILED; break;
                }
                TraceSession.recordEngineDisposition(disposition, null, false,
                    nonBlankDetail(detail, kind.name()));
            }
        } finally {
            if (traceActive) {
                TraceSession.closeAndEmit(decisionTraceSink);
            }
            context = null;
        }
    }

    /** FINALIZER RUNTIME §4: attempt-failed callback — mirror of RandoCalAi.applyAttemptFailedDisposition. */
    private void applyAttemptFailedDisposition(String detail) {
        boolean traceActive = TraceSession.isActive();
        try {
            if (traceActive) {
                TraceSession.recordEngineDisposition(TraceFinalization.Disposition.ATTEMPT_FAILED,
                    null, false, nonBlankDetail(detail, "attempt failed before a result"));
            }
        } finally {
            if (traceActive) {
                TraceSession.closeAndEmit(decisionTraceSink);
            }
            context = null;
        }
    }

    private static String nonBlankDetail(String detail, String fallback) {
        return (detail != null && !detail.isBlank()) ? detail : fallback;
    }

    /** FINALIZER RUNTIME §4: outer common mutation — mirror of RandoCalAi (owner OUTER_CHOSENONE). */
    private void applyOuterCommonTrackerAndStrategic(AwaitingDecision decision, String decisionType,
                                                     String decisionText, String result,
                                                     boolean traceActive) {
        DecisionTrackerSnapshot traceTrackerBefore =
            traceActive ? decisionTracker.traceSnapshot() : null;
        decisionTracker.recordDecision(decisionType, decisionText,
            String.valueOf(decision.getAwaitingDecisionId()), result != null ? result : "");
        if (traceActive) {
            TraceSession.recordTrackerRecordResponse(TrackerOwner.OUTER_CHOSENONE,
                decisionType, String.valueOf(decision.getAwaitingDecisionId()),
                decisionTracker.traceDecisionKey(decisionType, decisionText),
                result != null ? result : "",
                traceTrackerBefore, decisionTracker.traceSnapshot());
        }
        trackStrategicEvents(decision, decisionText, result);
    }

    /** FINALIZER RUNTIME §4: direct-interceptor result (mode NONE) — mirror of RandoCalAi. */
    private OuterDecision interceptorResult(String wire, boolean mediatorFacing) {
        if (!mediatorFacing) {
            TraceSession.recordFinalResponse(wire, true);
        }
        return new OuterDecision(wire, AiDecisionResult.MutationMode.NONE);
    }

    private OuterDecision computeDecision(String playerId, AwaitingDecision decision,
                                          GameState gameState, RejectionHistory history,
                                          boolean mediatorFacing) {
        // Build context for this decision
        context = RandoContext.build(playerId, gameState, currentGame);

        String decisionType = decision.getDecisionType() != null ? decision.getDecisionType().name() : "UNKNOWN";
        String decisionText = decision.getText() != null ? decision.getText() : "";
        Phase phase = gameState != null ? gameState.getCurrentPhase() : null;

        LOG.info("[TheChosenOneAi] decide() called: type={}, phase={}, text='{}'",
            decisionType, phase,
            decisionText.length() > 50 ? decisionText.substring(0, 50) + "..." : decisionText);

        // TRACE ORACLE V2 (2026-07-13, CODEX_TRACE_ORACLE_V2_CONTRACT "Trace ownership"):
        // open the bot-boundary session — full frozen raw input + shadow snapshot — when
        // the sink is enabled. OBSERVATION ONLY per the route map's shadow authority:
        // no interceptor return moves, no extra RNG draw, no behavior change. Production
        // default sink is disabled, so this whole block no-ops.
        boolean traceOpened = false;
        TraceSnapshots.Result boundarySnapshot = null;
        try {
            if (decisionTraceSink.isEnabled()) {
                boundarySnapshot = captureDecisionSnapshot(playerId, decision, gameState,
                    decisionType, decisionText, phase);
                traceOpened = openDecisionTraceSession(decision, decisionType, decisionText,
                    boundarySnapshot);
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

            // V45: NEVER forfeit when all cards are immune to attrition
            {
                String dtLower = decisionText.toLowerCase(java.util.Locale.ROOT);
                if (dtLower.contains("forfeit") && dtLower.contains("if desired")) {
                    LOG.warn("V45 IMMUNE FORFEIT: All cards immune — PASSING on optional forfeit! Text: '{}'", decisionText);
                    // TRACE ORACLE V2: route + final-response observation ONLY; the direct
                    // return below is untouched (it skips the common finalizer — recorded).
                    if (traceOpened) {
                        TraceSession.recordRoute(TraceRoute.V45_OPTIONAL_FORFEIT,
                            "decision text contains 'forfeit' + 'if desired'", null);
                        // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                        TraceSession.recordEvaluatorLaneNotApplicable(
                            "direct interceptor V45: evaluator lane never runs on this route");
                    }
                    return interceptorResult("", mediatorFacing);  // Empty = select nothing = pass
                }
            }

            // V44/V67j: ALWAYS accept revert requests — never block the opponent
            // from reverting. Steve's rule: "Rando must always allow a revert. If
            // the gemp game has an error, I need to be able to always revert."
            // V67j: Don't assume index 0 = Yes. Inspect the `results` param and
            // find the actual "Yes/Allow/Accept" choice's index. Fallback to 0
            // if the array isn't available or no clear positive option found.
            if (decision.getDecisionType() == AwaitingDecisionType.MULTIPLE_CHOICE
                    && decisionText.toLowerCase(java.util.Locale.ROOT).contains("revert")) {
                String[] revertResults = params != null ? params.get("results") : null;
                RevertApprovalPhaseOwner.LegacySelection selection =
                    RevertApprovalPhaseOwner.legacySelection(revertResults);
                LOG.warn("V44/V67j REVERT: Accepting revert request (index={} = '{}') text: '{}'",
                    selection.ordinal(), selection.resultText(), decisionText);
                // TRACE ORACLE V2: route + final-response observation ONLY.
                if (traceOpened) {
                    TraceSession.recordRoute(TraceRoute.V44_V67J_REVERT_APPROVAL,
                        "MULTIPLE_CHOICE + decision text contains 'revert'", null);
                    TraceSession.recordEvaluatorLaneNotApplicable(
                        mediatorFacing
                            ? "typed finalizer owner V44/V67j: evaluator lane never runs on this route"
                            : "direct interceptor V44/V67j: evaluator lane never runs on this route");
                }
                String revertWire = String.valueOf(selection.ordinal());
                if (!mediatorFacing) {
                    return interceptorResult(revertWire, mediatorFacing);
                }
                if (boundarySnapshot == null) {
                    boundarySnapshot = captureDecisionSnapshot(playerId, decision, gameState,
                        decisionType, decisionText, phase);
                }
                return new OuterDecision(RevertApprovalPhaseOwner.decide(
                    boundarySnapshot.snapshot(), history, selection));
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
            if (decision.getDecisionType() == AwaitingDecisionType.MULTIPLE_CHOICE
                    && decisionText.toLowerCase(java.util.Locale.ROOT).contains("undercover spy")) {
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
                boolean v170GoUndercover = v170OppDrain >= 1;
                int v170YesIdx = 0, v170NoIdx = 1;
                String[] v170Results = params != null ? params.get("results") : null;
                if (v170Results != null) {
                    for (int ri = 0; ri < v170Results.length; ri++) {
                        String r = v170Results[ri] != null
                            ? v170Results[ri].toLowerCase(java.util.Locale.ROOT) : "";
                        if (r.equals("yes")) v170YesIdx = ri;
                        else if (r.equals("no")) v170NoIdx = ri;
                    }
                }
                int v170Pick = v170GoUndercover ? v170YesIdx : v170NoIdx;
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
                }
                return interceptorResult(String.valueOf(v170Pick), mediatorFacing);
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
                if (results != null && results.length > 0) {
                    boolean isTfismfChoice = false;
                    for (String r : results) {
                        if (r == null) continue;
                        String rLower = r.toLowerCase(java.util.Locale.ROOT);
                        if (rLower.contains("i have it") || rLower.contains("my father has it")
                            || rLower.contains("you have that power")) {
                            isTfismfChoice = true;
                            break;
                        }
                    }
                    if (isTfismfChoice) {
                        String deckLower = deckName != null
                            ? deckName.toLowerCase(java.util.Locale.ROOT) : "";
                        int luke = -1, anakin = -1, rey = -1;
                        for (int i = 0; i < results.length; i++) {
                            String rLower = results[i] != null
                                ? results[i].toLowerCase(java.util.Locale.ROOT) : "";
                            if (rLower.contains("my father has it")) anakin = i;
                            else if (rLower.contains("you have that power")) rey = i;
                            else if (rLower.contains("i have it")) luke = i;
                        }
                        int pick = -1;
                        String why = "";
                        if (deckLower.contains("luke") && luke >= 0) {
                            pick = luke; why = "Luke deck → 'I Have It'";
                        } else if (deckLower.contains("anakin") && anakin >= 0) {
                            pick = anakin; why = "Anakin deck → 'My Father Has It'";
                        } else if (deckLower.contains("rey") && rey >= 0) {
                            pick = rey; why = "Rey deck → 'You Have That Power, Too'";
                        } else if (luke >= 0) {
                            // Default to Luke — most common, matches deck-name fallback in V29.15
                            pick = luke; why = "Default (no deck match) → 'I Have It'";
                        }
                        if (pick >= 0) {
                            LOG.warn("V61 EPIC EVENT SAGA: deck='{}' choices={} → {} (index {})",
                                deckName, java.util.Arrays.asList(results), why, pick);
                            // TRACE ORACLE V2: route + final-response observation ONLY.
                            if (traceOpened) {
                                TraceSession.recordRoute(TraceRoute.V61_SAGA_CHOICE,
                                    "MULTIPLE_CHOICE results contain TFISMF saga options", null);
                                // GATE P0-3: direct interceptor — evaluator-lane facts explicitly n/a.
                                TraceSession.recordEvaluatorLaneNotApplicable(
                                    "direct interceptor V61: evaluator lane never runs on this route");
                            }
                            return interceptorResult(String.valueOf(pick), mediatorFacing);
                        }
                    }
                }
            }

            // Maybe apply chaos (random action)
            // CRITICAL: Never use chaos mode during DEPLOY phase - deploy decisions are strategic
            // and random deploys can waste resources or violate the deployment plan
            Phase currentPhase = gameState != null ? gameState.getCurrentPhase() : null;
            boolean isSafeForChaos = currentPhase != Phase.DEPLOY && currentPhase != Phase.BATTLE;
            if (isSafeForChaos && shouldApplyChaos()) {
                RandoLogger.debug("Chaos mode: selecting random action");
                // TRACE ORACLE V2: explicit chaos route (recorded AFTER shouldApplyChaos()
                // consumed its one RNG draw — the trace never draws).
                if (traceOpened) {
                    TraceSession.recordRoute(TraceRoute.CHAOS_FALLBACK,
                        "chaos gate passed (phase=" + currentPhase + ", outside deploy/battle)", null);
                    // GATE P0-3: chaos bypasses the evaluator lane — facts explicitly n/a.
                    TraceSession.recordEvaluatorLaneNotApplicable(
                        "chaos fallback: heuristic base bypassed the evaluator lane");
                }
                result = super.decide(playerId, decision, gameState);
            } else {
                DrawRoute drawRoute = DrawRouteResolver.resolve(
                    DrawRouteInput.capture(currentPhase, decision));
                if (drawRoute == DrawRoute.DRAW_TOP_LEVEL) {
                    if (boundarySnapshot == null) {
                        boundarySnapshot = captureDecisionSnapshot(playerId, decision, gameState,
                            decisionType, decisionText, phase);
                    }
                    return decideOwnedDraw(playerId, decision, gameState, history,
                        boundarySnapshot.snapshot(), traceOpened, mediatorFacing,
                        decisionType, decisionText);
                }

                PullRouteInput pullInput = PullRouteInput.capture(decision);
                PullRoute pullRoute = PullRouteResolver.resolve(pullInput);
                if (pullRoute != PullRoute.LEGACY_UNOWNED) {
                    if (boundarySnapshot == null) {
                        boundarySnapshot = captureDecisionSnapshot(playerId, decision, gameState,
                            decisionType, decisionText, phase);
                    }
                    return decideOwnedPull(playerId, decision, gameState, history,
                        boundarySnapshot.snapshot(), pullInput, pullRoute, traceOpened,
                        mediatorFacing, decisionType, decisionText);
                }

                // Try evaluator system for supported decision types
                String evaluatorResult = tryEvaluators(playerId, decision, gameState);
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

            // FINALIZER RUNTIME §4: the COMMON BOUNDARY (OUTER_COMMON). Mirror of RandoCalAi.
            //   - mediator-facing: DEFER the outer mutation AND the trace close to the accepted
            //     disposition callback; return the wire only, trace stays open.
            //   - direct decide(): apply the outer mutation inline (before-snapshot taken inside
            //     applyOuterCommonTrackerAndStrategic, immediately before recordDecision) and record
            //     the final response, exactly as before; the finally closes and emits inline.
            if (mediatorFacing) {
                return new OuterDecision(result, AiDecisionResult.MutationMode.OUTER_COMMON);
            }
            applyOuterCommonTrackerAndStrategic(decision, decisionType, decisionText, result, traceOpened);
            LOG.info("[TheChosenOneAi] decide() result: '{}' ✅", result != null ? result : "(pass)");
            // TRACE ORACLE V2: the AI's ACTUAL final answer, after safety (contract
            // "Finalization record" item 6). The five direct interceptors record theirs
            // with skippedCommonFinalizer=true at their own return sites above.
            if (traceOpened) {
                TraceSession.recordFinalResponse(result, false);
            }
            return new OuterDecision(result, AiDecisionResult.MutationMode.OUTER_COMMON);
        } finally {
            // FINALIZER RUNTIME §4/§7: trace close is CALL-PATH-aware. Direct decide() closes +
            // emits inline on every path including a computation exception; a mediator-facing call
            // never closes here — the synchronous disposition callback owns the close, and a
            // mediator-facing computation exception is closed by onDecisionAttemptFailed.
            // GATE P0-2 (CODEX_TRACE_V2_GATE_97D2CB65A_2026-07-13.md): closeAndEmit is the one typed
            // emission channel — a finish() failure emits the fallback INCOMPLETE envelope, a sink
            // accept() failure re-offers the trace once. Never throws into the decision path.
            context = null;  // per-decision context; the disposition callbacks never read it
            if (!mediatorFacing && traceOpened) {
                TraceSession.closeAndEmit(decisionTraceSink);
            }
        }
    }

    /**
     * TRACE ORACLE V2 (2026-07-13, Handoffs/CODEX_TRACE_ORACLE_V2_CONTRACT_2026-07-13.md
     * "Frozen input and candidate order"): capture the bot-boundary snapshot with the
     * COMPLETE raw decision arrays (verbatim, unfiltered — unlike buildEvaluatorContext,
     * which drops null/empty entries) plus the shadow DecisionSnapshot. Pure reads only:
     * decision params, DecisionTracker.getBlockedResponses (pure), and plain GameState
     * getters. No evaluator, strategy service, or cache-mutating call.
     */
    private TraceSnapshots.Result captureDecisionSnapshot(String playerId,
                                                          AwaitingDecision decision,
                                                          GameState gameState,
                                                          String decisionType,
                                                          String decisionText,
                                                          Phase phase) {
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
        return TraceSnapshots.build(in);
    }

    /** Open the trace with the same immutable snapshot later consumed by an owned route. */
    private boolean openDecisionTraceSession(AwaitingDecision decision,
                                             String decisionType,
                                             String decisionText,
                                             TraceSnapshots.Result snapshot) {
        Map<String, String[]> params = decision.getDecisionParameters();
        java.util.List<String> actionIds = params != null
            ? traceRawList(params.get("actionId")) : null;
        java.util.List<String> cardIds = params != null
            ? traceRawList(params.get("cardId")) : null;
        java.util.List<String> results = params != null
            ? traceRawList(params.get("results")) : null;
        return TraceSession.open(getClass().getPackageName(),
            String.valueOf(decision.getAwaitingDecisionId()), decisionType, decisionText,
            TraceSnapshots.rawCandidateIds(decisionType, actionIds, cardIds, results),
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
                TraceSession.recordTrackerUpdateState(TrackerOwner.OUTER_CHOSENONE,
                    handSize, forcePile, reserveDeck, turn, cardsInPlay,
                    traceLifecycleBefore, decisionTracker.traceLifecycleSnapshot());
            } catch (Throwable traceT) {
                TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                    traceT.getClass().getName(),
                    "UPDATE_STATE after-snapshot/record failed; legacy updateState already ran");
            }
        }
    }

    /**
     * Execute the one typed canonical DRAW owner. No owned result re-enters the
     * legacy safety, fallback, or evaluator lanes.
     */
    private OuterDecision decideOwnedDraw(String playerId,
                                          AwaitingDecision decision,
                                          GameState gameState,
                                          RejectionHistory history,
                                          DecisionSnapshot snapshot,
                                          boolean traceOpened,
                                          boolean mediatorFacing,
                                          String decisionType,
                                          String decisionText) {
        if (traceOpened) {
            TraceSession.recordRoute(TraceRoute.DRAW_TOP_LEVEL,
                "typed canonical Force-Pile draw action", null);
        }

        DecisionContext evalContext = buildEvaluatorContext(playerId, decision, gameState);
        AiDecisionResult ownedResult;
        if (evalContext == null) {
            ownedResult = AiDecisionResult.typedRejection(
                com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                "owned DRAW route could not build the evaluator context",
                String.valueOf(decision.getAwaitingDecisionId()));
        } else {
            ownedResult = DrawPhaseOwner.decide(snapshot, history, () -> {
                EvaluatedAction bestAction = combinedEvaluator.evaluateDecision(evalContext);
                if (bestAction == null) {
                    return null;
                }
                return bestAction.getActionType() == ActionType.PASS
                    ? DrawPhaseOwner.Evaluation.passResult()
                    : DrawPhaseOwner.Evaluation.candidate(bestAction.getActionId());
            });
        }

        if (ownedResult.status() == AiDecisionResult.Status.TYPED_REJECTION) {
            return new OuterDecision(ownedResult);
        }

        String wire = ownedResult.wireResponse();
        if (!mediatorFacing) {
            applyOuterCommonTrackerAndStrategic(decision, decisionType, decisionText, wire, traceOpened);
            LOG.info("[TheChosenOneAi] owned DRAW result: '{}'", wire.isEmpty() ? "(pass)" : wire);
            if (traceOpened) {
                TraceSession.recordFinalResponse(wire, false);
            }
        }
        return new OuterDecision(ownedResult);
    }

    /**
     * Execute the one typed PULL owner. Owned results and typed rejections do not
     * re-enter the legacy fallback or safety lanes.
     */
    private OuterDecision decideOwnedPull(String playerId,
                                          AwaitingDecision decision,
                                          GameState gameState,
                                          RejectionHistory history,
                                          DecisionSnapshot snapshot,
                                          PullRouteInput pullInput,
                                          PullRoute pullRoute,
                                          boolean traceOpened,
                                          boolean mediatorFacing,
                                          String decisionType,
                                          String decisionText) {
        if (traceOpened) {
            TraceSession.recordRoute(traceRouteForPull(pullRoute),
                "typed PULL transaction stage " + pullRoute, null);
            if (pullRoute == PullRoute.PULL_FAILED_VERIFY) {
                TraceSession.recordEvaluatorLaneNotApplicable(
                    "failed PULL verification finalizes an empty selection without scoring");
            }
        }

        FactValue<PullFacts> factsValue = PullFacts.parse(snapshot, pullInput, pullRoute);
        AiDecisionResult ownedResult;
        if (factsValue.isUnknown()) {
            ownedResult = AiDecisionResult.typedRejection(
                com.gempukku.swccgo.ai.models.common.finalization.FinalizedResponse.RejectReason.CONTRACT_FACT_UNKNOWN,
                "owned PULL facts are unknown: " + factsValue.unknownReason(),
                String.valueOf(decision.getAwaitingDecisionId()));
        } else {
            PullFacts facts = factsValue.value();
            PullAssessment assessment = PullAssessment.compatibility(facts);
            ownedResult = PullPhaseOwner.decide(snapshot, history, pullRoute, facts, assessment,
                (route, ignoredFacts, ignoredAssessment) ->
                    tryEvaluators(playerId, decision, gameState));
        }

        if (ownedResult.status() == AiDecisionResult.Status.TYPED_REJECTION) {
            return new OuterDecision(ownedResult);
        }

        String wire = ownedResult.wireResponse();
        if (!mediatorFacing) {
            applyOuterCommonTrackerAndStrategic(decision, decisionType, decisionText, wire, traceOpened);
            LOG.info("[TheChosenOneAi] owned PULL {} result: '{}'", pullRoute,
                wire.isEmpty() ? "(pass)" : wire);
            if (traceOpened) {
                TraceSession.recordFinalResponse(wire, false);
            }
        }
        return new OuterDecision(ownedResult);
    }

    private static TraceRoute traceRouteForPull(PullRoute route) {
        return switch (route) {
            case PULL_PARENT -> TraceRoute.PULL_PARENT;
            case PULL_DEPLOY_CHILD -> TraceRoute.PULL_DEPLOY_CHILD;
            case PULL_TAKE_CHILD -> TraceRoute.PULL_TAKE_CHILD;
            case PULL_DESTINATION -> TraceRoute.PULL_DESTINATION;
            case PULL_FAILED_VERIFY -> TraceRoute.PULL_FAILED_VERIFY;
            default -> throw new IllegalArgumentException("unowned PULL route " + route);
        };
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
        evalContext.setObjectiveAnalyzer(objectiveAnalyzer);

        if (!objectiveAnalyzer.isAnalyzed() && currentGame != null && mySide != null) {
            objectiveAnalyzer.analyze(currentGame, playerId, mySide);
        } else if (objectiveAnalyzer.isAnalyzed() && currentGame != null) {
            // V29.7: Refresh flip status each evaluation so we detect when objective actually flips
            objectiveAnalyzer.refreshFlipStatus(currentGame.getGameState(), playerId);
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
                    TraceSession.recordTrackerClear(TrackerOwner.OUTER_CHOSENONE,
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
            // TRACE 4B2 (Handoffs/CODEX_TRACE_STAGE4_4B2_STRATEGY_CONTROLLER_PREFLIGHT_2026-07-13.md
            // "Exact Reachability, Guards, and Order" item 4): observe the new-game
            // controller SIDE_SET then RESET, each at its unchanged source position, each
            // its own event. Snapshots build only under an active session; failure marks
            // STATE_EVENT and never skips, repeats, or alters the legacy call.
            StrategyControllerSnapshot traceSideBefore = null;
            if (TraceSession.isActive()) {
                try {
                    traceSideBefore = StrategyControllerTraceAccess.snapshot(strategyController);
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "SIDE_SET before-snapshot failed; legacy setSide unaffected");
                }
            }
            strategyController.setSide(mySide);
            if (traceSideBefore != null) {
                try {
                    TraceSession.recordStrategySideSet(StrategyControllerOwner.CHOSENONE, mySide,
                        traceSideBefore, StrategyControllerTraceAccess.snapshot(strategyController));
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "SIDE_SET after-snapshot/record failed; legacy setSide already ran");
                }
            }
            StrategyControllerSnapshot traceResetBefore = null;
            if (TraceSession.isActive()) {
                try {
                    traceResetBefore = StrategyControllerTraceAccess.snapshot(strategyController);
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "RESET before-snapshot failed; legacy reset unaffected");
                }
            }
            strategyController.reset();
            if (traceResetBefore != null) {
                try {
                    TraceSession.recordStrategyReset(StrategyControllerOwner.CHOSENONE,
                        traceResetBefore, StrategyControllerTraceAccess.snapshot(strategyController));
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "RESET after-snapshot/record failed; legacy reset already ran");
                }
            }
            objectiveHandler.reset();
            objectiveAnalyzer.reset();
            shieldStrategy.setSide(mySide);
            shieldStrategy.reset();
            deployPhasePlanner.reset();
            deckOracle.reset();  // V22.6: Reset deck knowledge for new game
            opponentDeckTracker.reset();  // V24.7: Reset opponent intel for new game
            LOG.debug("[TheChosenOneAi] All strategy components reset for new game as {} side", mySide);

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
            // TRACE 4B2 (packet item 5): observe the controller START_TURN at its
            // unchanged source position; snapshots build only under an active session.
            StrategyControllerSnapshot traceStartTurnBefore = null;
            if (TraceSession.isActive()) {
                try {
                    traceStartTurnBefore = StrategyControllerTraceAccess.snapshot(strategyController);
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "START_TURN before-snapshot failed; legacy startNewTurn unaffected");
                }
            }
            strategyController.startNewTurn(currentTurn);
            if (traceStartTurnBefore != null) {
                try {
                    TraceSession.recordStrategyStartTurn(StrategyControllerOwner.CHOSENONE, currentTurn,
                        traceStartTurnBefore, StrategyControllerTraceAccess.snapshot(strategyController));
                } catch (Throwable traceT) {
                    TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                        traceT.getClass().getName(),
                        "START_TURN after-snapshot/record failed; legacy startNewTurn already ran");
                }
            }

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
                // TRACE 4B2 (packet item 5): observe the optional controller
                // FOCUS_DEPLOY_RECORD at its unchanged source position, BEFORE the outer
                // pending-deploy CLEAR below; the exact card-type argument is captured
                // while lastPendingDeployType is still non-null.
                StrategyControllerSnapshot traceFocusBefore = null;
                if (TraceSession.isActive()) {
                    try {
                        traceFocusBefore = StrategyControllerTraceAccess.snapshot(strategyController);
                    } catch (Throwable traceT) {
                        TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                            traceT.getClass().getName(),
                            "FOCUS_DEPLOY_RECORD before-snapshot failed; legacy onSuccessfulDeploy unaffected");
                    }
                }
                strategyController.onSuccessfulDeploy(lastPendingDeployType);
                if (traceFocusBefore != null) {
                    try {
                        TraceSession.recordStrategyFocusDeployRecord(StrategyControllerOwner.CHOSENONE,
                            lastPendingDeployType, traceFocusBefore,
                            StrategyControllerTraceAccess.snapshot(strategyController));
                    } catch (Throwable traceT) {
                        TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                            traceT.getClass().getName(),
                            "FOCUS_DEPLOY_RECORD after-snapshot/record failed; legacy onSuccessfulDeploy already ran");
                    }
                }
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
        // TRACE 4B2 (packet item 6): observe the once-per-decision controller
        // BATTLE_ORDER_REFRESH at its unchanged source position. No GameState reference is
        // ever stored; the internal setUnderBattleOrderRules write stays folded into this
        // single event, whose outcome follows controller-snapshot equality.
        StrategyControllerSnapshot traceBattleOrderBefore = null;
        if (TraceSession.isActive()) {
            try {
                traceBattleOrderBefore = StrategyControllerTraceAccess.snapshot(strategyController);
            } catch (Throwable traceT) {
                TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                    traceT.getClass().getName(),
                    "BATTLE_ORDER_REFRESH before-snapshot failed; legacy updateBattleOrderFromGameState unaffected");
            }
        }
        strategyController.updateBattleOrderFromGameState(gameState);
        if (traceBattleOrderBefore != null) {
            try {
                TraceSession.recordStrategyBattleOrderRefresh(StrategyControllerOwner.CHOSENONE,
                    traceBattleOrderBefore, StrategyControllerTraceAccess.snapshot(strategyController));
            } catch (Throwable traceT) {
                TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                    traceT.getClass().getName(),
                    "BATTLE_ORDER_REFRESH after-snapshot/record failed; legacy updateBattleOrderFromGameState already ran");
            }
        }

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
                    // V102 (Steve, 2026-05-20): K&D activation tracking.
                    // Each new shield committed to SIDE_OF_TABLE corresponds to one K&D
                    // activation (the activation is what fires the "play a card" effect
                    // that places the shield). Bump the per-turn K&D activation counter
                    // so atKnDActivationCap() correctly hard-blocks further K&D plays.
                    try {
                        int v102Turn = gameState.getPlayersLatestTurnNumber(playerId);
                        shieldStrategy.recordKnDActivation(v102Turn);
                    } catch (Exception v102e) {
                        LOG.debug("V102 K&D activation tracking error: {}", v102e.getMessage());
                    }
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
        // TRACE 4B2 (packet item 9): the two win/loss lexical hooks share one operation
        // kind; each observes onBattleResult at its unchanged source position AFTER the
        // outer pending-deploy SET above, snapshots built only under an active session.
        if (textLower.contains("battle")) {
            if (textLower.contains("you won") || textLower.contains("you have won")) {
                StrategyControllerSnapshot traceBattleWonBefore = null;
                if (TraceSession.isActive()) {
                    try {
                        traceBattleWonBefore = StrategyControllerTraceAccess.snapshot(strategyController);
                    } catch (Throwable traceT) {
                        TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                            traceT.getClass().getName(),
                            "BATTLE_RESULT_RECORD before-snapshot failed; legacy onBattleResult unaffected");
                    }
                }
                strategyController.onBattleResult(true);
                if (traceBattleWonBefore != null) {
                    try {
                        TraceSession.recordStrategyBattleResultRecord(StrategyControllerOwner.CHOSENONE, true,
                            traceBattleWonBefore, StrategyControllerTraceAccess.snapshot(strategyController));
                    } catch (Throwable traceT) {
                        TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                            traceT.getClass().getName(),
                            "BATTLE_RESULT_RECORD after-snapshot/record failed; legacy onBattleResult already ran");
                    }
                }
                LOG.debug("Battle won - updating strategy controller");
            } else if (textLower.contains("you lost") || textLower.contains("you have lost")) {
                StrategyControllerSnapshot traceBattleLostBefore = null;
                if (TraceSession.isActive()) {
                    try {
                        traceBattleLostBefore = StrategyControllerTraceAccess.snapshot(strategyController);
                    } catch (Throwable traceT) {
                        TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                            traceT.getClass().getName(),
                            "BATTLE_RESULT_RECORD before-snapshot failed; legacy onBattleResult unaffected");
                    }
                }
                strategyController.onBattleResult(false);
                if (traceBattleLostBefore != null) {
                    try {
                        TraceSession.recordStrategyBattleResultRecord(StrategyControllerOwner.CHOSENONE, false,
                            traceBattleLostBefore, StrategyControllerTraceAccess.snapshot(strategyController));
                    } catch (Throwable traceT) {
                        TraceSession.markCaptureFailure(TraceCaptureFailure.Stage.STATE_EVENT,
                            traceT.getClass().getName(),
                            "BATTLE_RESULT_RECORD after-snapshot/record failed; legacy onBattleResult already ran");
                    }
                }
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
