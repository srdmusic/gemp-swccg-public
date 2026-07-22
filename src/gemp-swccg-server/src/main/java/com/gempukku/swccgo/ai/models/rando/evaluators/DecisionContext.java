package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.models.rando.strategy.DeckOracle;
import com.gempukku.swccgo.ai.models.rando.strategy.OpponentDeckTracker;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveAnalyzer;
// V295 RETIRED: import com.gempukku.swccgo.ai.models.rando.strategy.ObjectiveHandler;
import com.gempukku.swccgo.ai.models.common.strategy.ShieldStrategy;
import com.gempukku.swccgo.ai.models.rando.strategy.StrategyController;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.*;

/**
 * Context information for evaluating a decision.
 *
 * Contains all information an evaluator needs to score actions:
 * - Current game state (board, resources, power)
 * - Available actions
 * - Decision type and text
 * - Phase information
 */
public class DecisionContext {
    // Game state
    private final GameState gameState;
    private final String playerId;
    private SwccgGame game;  // Full game reference for advanced analysis
    private Side side;  // Our side (DARK or LIGHT)

    // Decision info
    private final String decisionType;  // CARD_ACTION_CHOICE, CARD_SELECTION, INTEGER, etc.
    private final String decisionText;  // Human-readable prompt
    private final String decisionId;

    // Phase info
    private final Phase phase;
    private final int turnNumber;
    private final boolean isMyTurn;

    // Available actions (for ACTION_CHOICE decisions)
    private List<String> actionIds = new ArrayList<>();
    private List<String> actionTexts = new ArrayList<>();

    // For CARD_SELECTION decisions
    private List<String> cardIds = new ArrayList<>();
    private List<String> blueprints = new ArrayList<>();
    private List<Boolean> selectable = new ArrayList<>();
    private List<String> testingTexts = new ArrayList<>();  // Card titles from GEMP

    // Parameters from decision XML
    private boolean noPass = true;  // Can we pass/cancel?
    private int min = 0;  // Minimum selection required
    private int max = 1;  // Maximum selection allowed
    private boolean activationAmountDecision;

    // Additional context
    private Map<String, Object> extra = new HashMap<>();

    // Blocked responses (for loop prevention)
    private Set<String> blockedResponses = new HashSet<>();

    // V67ax DEPLOY PHASE SCRIPT: actions allowed for the current deploy step.
    // When non-null, evaluators / CombinedEvaluator must restrict the final
    // pick to this set. Null = no restriction (default).
    private Set<String> allowedActionIds = null;
    private String allowedActionsReason = null;

    // V67bc DPS HIERARCHY: ordered list of step buckets, highest priority first.
    // CombinedEvaluator walks these in order, picks first action above the bad
    // threshold. PASS only when all buckets exhausted with all-bad scores.
    // Null = no DPS hierarchy (legacy single-set or no DPS at all).
    private java.util.List<Set<String>> stepBuckets = null;
    private java.util.List<String> stepBucketLabels = null;

    // Strategy components (optional, set by AI)
    private StrategyController strategyController;
    // V295 RETIRED: private ObjectiveHandler objectiveHandler;
    private ShieldStrategy shieldStrategy;
    private DeployPhasePlanner deployPhasePlanner;
    private ObjectiveAnalyzer objectiveAnalyzer;
    private DeckOracle deckOracle;  // V22.6: Full deck knowledge
    private OpponentDeckTracker opponentDeckTracker;  // V24.7: Opponent destiny intel
    private String deckName;  // V29.15: Deck name for saga-aware Epic Event choices

    public DecisionContext(GameState gameState, String playerId, String decisionType,
                          String decisionText, String decisionId, Phase phase) {
        this.gameState = gameState;
        this.playerId = playerId;
        this.decisionType = decisionType;
        this.decisionText = decisionText;
        this.decisionId = decisionId;
        this.phase = phase;
        this.turnNumber = gameState != null ? gameState.getPlayersLatestTurnNumber(playerId) : 1;
        this.isMyTurn = gameState != null && playerId.equals(gameState.getCurrentPlayerId());
    }

    // Getters
    public GameState getGameState() {
        return gameState;
    }

    public String getPlayerId() {
        return playerId;
    }

    public SwccgGame getGame() {
        return game;
    }

    public void setGame(SwccgGame game) {
        this.game = game;
    }

    public Side getSide() {
        return side;
    }

    public void setSide(Side side) {
        this.side = side;
    }

    public String getOpponentId() {
        if (gameState == null || playerId == null) return null;
        return gameState.getOpponent(playerId);
    }

    public String getDecisionType() {
        return decisionType;
    }

    public String getDecisionText() {
        return decisionText;
    }

    public String getDecisionId() {
        return decisionId;
    }

    public Phase getPhase() {
        return phase;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public boolean isMyTurn() {
        return isMyTurn;
    }

    public List<String> getActionIds() {
        return actionIds;
    }

    public void setActionIds(List<String> actionIds) {
        this.actionIds = actionIds;
    }

    public List<String> getActionTexts() {
        return actionTexts;
    }

    public void setActionTexts(List<String> actionTexts) {
        this.actionTexts = actionTexts;
    }

    public List<String> getCardIds() {
        return cardIds;
    }

    public void setCardIds(List<String> cardIds) {
        this.cardIds = cardIds;
    }

    public List<String> getBlueprints() {
        return blueprints;
    }

    public void setBlueprints(List<String> blueprints) {
        this.blueprints = blueprints;
    }

    public List<Boolean> getSelectable() {
        return selectable;
    }

    public void setSelectable(List<Boolean> selectable) {
        this.selectable = selectable;
    }

    public List<String> getTestingTexts() {
        return testingTexts;
    }

    public void setTestingTexts(List<String> testingTexts) {
        this.testingTexts = testingTexts;
    }

    /**
     * Get the card title for a given index from testingTexts.
     * This is the card name GEMP provides, which is more reliable than parsing action text.
     */
    public String getCardTitleAt(int index) {
        if (testingTexts == null || index < 0 || index >= testingTexts.size()) {
            return null;
        }
        String title = testingTexts.get(index);
        // testingText may have format like "•Card Name" - strip leading •
        if (title != null && title.startsWith("•")) {
            title = title.substring(1);
        }
        // May also have "(V)" virtual marker
        if (title != null && title.contains("(V)")) {
            title = title.replace("(V)", "").trim();
        }
        return title;
    }

    public boolean isNoPass() {
        return noPass;
    }

    public void setNoPass(boolean noPass) {
        this.noPass = noPass;
    }

    public int getMin() {
        return min;
    }

    public void setMin(int min) {
        this.min = min;
    }

    public int getMax() {
        return max;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public boolean isActivationAmountDecision() {
        return activationAmountDecision;
    }

    public void setActivationAmountDecision(boolean activationAmountDecision) {
        this.activationAmountDecision = activationAmountDecision;
    }

    public Map<String, Object> getExtra() {
        return extra;
    }

    public void setExtra(String key, Object value) {
        this.extra.put(key, value);
    }

    public Object getExtra(String key) {
        return extra.get(key);
    }

    public Set<String> getBlockedResponses() {
        return blockedResponses;
    }

    public void addBlockedResponse(String response) {
        this.blockedResponses.add(response);
    }

    public void setBlockedResponses(Set<String> blocked) {
        this.blockedResponses.clear();
        if (blocked != null) {
            this.blockedResponses.addAll(blocked);
        }
    }

    // Convenience methods for game state queries
    public int getForceAvailable() {
        // Force pile size represents available force
        if (gameState == null) return 0;
        return gameState.getForcePileSize(playerId);
    }

    public int getReserveDeckSize() {
        if (gameState == null) return 0;
        return gameState.getReserveDeckSize(playerId);
    }

    public int getUsedPileSize() {
        if (gameState == null) return 0;
        return gameState.getUsedPile(playerId).size();
    }

    public int getForcePileSize() {
        if (gameState == null) return 0;
        return gameState.getForcePileSize(playerId);
    }

    public int getLifeForce() {
        return getReserveDeckSize() + getUsedPileSize() + getForcePileSize();
    }

    public List<PhysicalCard> getHand() {
        if (gameState == null) return Collections.emptyList();
        return gameState.getHand(playerId);
    }

    public int getHandSize() {
        if (gameState == null) return 0;
        return gameState.getHand(playerId).size();
    }

    // Shared contested-location predictor retained for late pull timing and other battle-intent
    // readers. V297.1 activation no longer uses this snapshot: a deploy or move after activation
    // can create a battle, so the four-card destiny floor is unconditional.
    public boolean isBattlePlausibleThisTurn() {
        if (game == null || gameState == null || playerId == null) return true;
        try {
            String opponentId = gameState.getOpponent(playerId);
            if (opponentId == null) return true;
            for (PhysicalCard location : gameState.getTopLocations()) {
                float ourPower = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(gameState, location, playerId, false, false);
                float theirPower = game.getModifiersQuerying()
                    .getTotalPowerAtLocation(gameState, location, opponentId, false, false);
                if (ourPower > 0 && theirPower > 0) return true;  // contested — battle plausible
            }
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    // Strategy component getters and setters
    public StrategyController getStrategyController() {
        return strategyController;
    }

    public void setStrategyController(StrategyController strategyController) {
        this.strategyController = strategyController;
    }

    // V295 RETIRED: public ObjectiveHandler getObjectiveHandler() {
    // V295 RETIRED:     return objectiveHandler;
    // V295 RETIRED: }
    // V295 RETIRED: public void setObjectiveHandler(ObjectiveHandler objectiveHandler) {
    // V295 RETIRED:     this.objectiveHandler = objectiveHandler;
    // V295 RETIRED: }

    public ShieldStrategy getShieldStrategy() {
        return shieldStrategy;
    }

    public void setShieldStrategy(ShieldStrategy shieldStrategy) {
        this.shieldStrategy = shieldStrategy;
    }

    public DeployPhasePlanner getDeployPhasePlanner() {
        return deployPhasePlanner;
    }

    public void setDeployPhasePlanner(DeployPhasePlanner deployPhasePlanner) {
        this.deployPhasePlanner = deployPhasePlanner;
    }

    public ObjectiveAnalyzer getObjectiveAnalyzer() {
        return objectiveAnalyzer;
    }

    public void setObjectiveAnalyzer(ObjectiveAnalyzer objectiveAnalyzer) {
        this.objectiveAnalyzer = objectiveAnalyzer;
    }

    // V22.6: DeckOracle — full deck knowledge
    public DeckOracle getDeckOracle() {
        return deckOracle;
    }

    public void setDeckOracle(DeckOracle deckOracle) {
        this.deckOracle = deckOracle;
    }

    // V24.7: OpponentDeckTracker — destiny intel from deck peeks
    public OpponentDeckTracker getOpponentDeckTracker() {
        return opponentDeckTracker;
    }

    public void setOpponentDeckTracker(OpponentDeckTracker opponentDeckTracker) {
        this.opponentDeckTracker = opponentDeckTracker;
    }

    // V29.15: Deck name for saga-aware Epic Event choices
    public String getDeckName() {
        return deckName;
    }

    public void setDeckName(String deckName) {
        this.deckName = deckName;
    }

    // ═══ T2 MOVE #1 COMMIT-2 (2026-07-06): per-decision force-reserve facts ═══
    // ONE cached ForceReserveService.compute() per decide() call, shared by
    // DrawEvaluator/PassEvaluator/MoveEvaluator/DeployEvaluator (DeployPhasePlanner
    // has no DecisionContext and calls the static compute at plan creation).
    // SOAK INSTRUMENT: every 20th decision (static counter across the JVM), every
    // cache READ re-runs compute() and logs "MAINT CACHE MISMATCH" on divergence —
    // remove the soak branch after 2 clean full games.
    private com.gempukku.swccgo.ai.models.common.strategy.ForceReserveService.Facts reserveFacts;
    private boolean reserveFactsSoak;
    private static final java.util.concurrent.atomic.AtomicLong RESERVE_FACTS_DECISIONS =
        new java.util.concurrent.atomic.AtomicLong();

    public com.gempukku.swccgo.ai.models.common.strategy.ForceReserveService.Facts getForceReserveFacts() {
        if (reserveFacts == null) {
            reserveFacts = com.gempukku.swccgo.ai.models.common.strategy
                .ForceReserveService.compute(game, gameState, playerId);
            reserveFactsSoak = (RESERVE_FACTS_DECISIONS.incrementAndGet() % 20 == 0);
        } else if (reserveFactsSoak) {
            // SOAK: compare the cached facts against a fresh compute at read time —
            // exactly what the old per-caller inline scan would have produced here.
            com.gempukku.swccgo.ai.models.common.strategy.ForceReserveService.Facts fresh =
                com.gempukku.swccgo.ai.models.common.strategy
                    .ForceReserveService.compute(game, gameState, playerId);
            com.gempukku.swccgo.ai.models.common.strategy
                .ForceReserveService.soakCompare(reserveFacts, fresh, playerId);
        }
        return reserveFacts;
    }

    // V67ax DEPLOY PHASE SCRIPT
    public Set<String> getAllowedActionIds() {
        return allowedActionIds;
    }

    public void setAllowedActionIds(Set<String> allowedActionIds) {
        this.allowedActionIds = allowedActionIds;
    }

    public String getAllowedActionsReason() {
        return allowedActionsReason;
    }

    public void setAllowedActionsReason(String reason) {
        this.allowedActionsReason = reason;
    }

    // V67bc DPS HIERARCHY accessors
    public java.util.List<Set<String>> getStepBuckets() {
        return stepBuckets;
    }

    public void setStepBuckets(java.util.List<Set<String>> buckets) {
        this.stepBuckets = buckets;
    }

    public java.util.List<String> getStepBucketLabels() {
        return stepBucketLabels;
    }

    public void setStepBucketLabels(java.util.List<String> labels) {
        this.stepBucketLabels = labels;
    }
}
