package com.gempukku.swccgo.ai.models.rando.evaluators;

import com.gempukku.swccgo.ai.common.AiCardHelper;
import com.gempukku.swccgo.ai.common.AiPriorityCards;
import com.gempukku.swccgo.ai.models.rando.strategy.DeployPhasePlanner;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentInstruction;
import com.gempukku.swccgo.ai.models.rando.strategy.DeploymentPlan;
import com.gempukku.swccgo.ai.models.rando.strategy.ShieldStrategy;
import com.gempukku.swccgo.common.CardCategory;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgCardBlueprint;
import com.gempukku.swccgo.game.state.GameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Evaluates CARD_SELECTION and ARBITRARY_CARDS decisions.
 *
 * These are decisions where the player must select one or more cards
 * from a list (e.g., choosing where to deploy, which card to forfeit,
 * targeting for weapons, etc.).
 *
 * Decision types handled:
 * - "choose card to set sabacc value" -> Random selection
 * - "choose where to deploy" -> Pick best location
 * - "choose force to lose" -> Pick best card to lose
 * - "choose a card from battle to forfeit" -> Pick lowest forfeit value
 * - "choose a pilot" -> Pick best pilot
 * - "choose card to cancel" -> Cancel opponent's cards, not ours
 * - "choose target" -> Weapon/ability targeting
 *
 * Ported from Python card_selection_evaluator.py
 */
public class CardSelectionEvaluator extends ActionEvaluator {

    // Score constants
    private static final float VERY_GOOD_DELTA = 150.0f;
    private static final float GOOD_DELTA = 10.0f;
    private static final float BAD_DELTA = -10.0f;
    private static final float VERY_BAD_DELTA = -150.0f;

    private final Random random = new Random();

    public CardSelectionEvaluator() {
        super("CardSelection");
    }

    @Override
    public boolean canEvaluate(DecisionContext context) {
        String decisionType = context.getDecisionType();
        return "CARD_SELECTION".equals(decisionType) || "ARBITRARY_CARDS".equals(decisionType);
    }

    @Override
    public List<EvaluatedAction> evaluate(DecisionContext context) {
        String text = context.getDecisionText();
        String textLower = text != null ? text.toLowerCase(Locale.ROOT) : "";

        logger.info("[CardSelectionEvaluator] Evaluating: {}",
            text != null && text.length() > 60 ? text.substring(0, 60) + "..." : text);

        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();
        List<Boolean> selectable = context.getSelectable();

        // Log selectable info - CRITICAL for debugging GEMP rejection issues
        if (selectable != null && !selectable.isEmpty()) {
            int selectableCount = 0;
            for (Boolean s : selectable) {
                if (s != null && s) selectableCount++;
            }
            logger.info("[CardSelectionEvaluator] {} cards total, {} selectable",
                       cardIds != null ? cardIds.size() : 0, selectableCount);
        }

        // For reserve deck selections, we may have blueprints but no cardIds
        if ((cardIds == null || cardIds.isEmpty()) &&
            (blueprints == null || blueprints.isEmpty())) {
            logger.warn("[CardSelectionEvaluator] No card IDs or blueprints in {} decision", context.getDecisionType());
            return new ArrayList<>();
        }

        // If we have blueprints but no cardIds, handle reserve deck selection
        if ((cardIds == null || cardIds.isEmpty()) && blueprints != null && !blueprints.isEmpty()) {
            logger.info("[CardSelectionEvaluator] Reserve deck selection with {} blueprints", blueprints.size());
            return evaluateReserveDeckSelection(context, textLower);
        }

        logger.debug("[CardSelectionEvaluator] {} cards to evaluate", cardIds.size());

        // Route to specific handlers based on decision text
        if (textLower.contains("choose card to set sabacc value")) {
            return evaluateSabaccSetValue(context);
        } else if (textLower.contains("choose") && textLower.contains("clone")) {
            return evaluateSabaccClone(context);
        } else if (textLower.contains("choose where to deploy")) {
            return evaluateDeployLocation(context);
        } else if (textLower.contains("choose force to lose")) {
            return evaluateForceLoss(context);
        } else if (textLower.contains("choose a card from battle to forfeit") ||
                   textLower.contains("forfeit")) {
            return evaluateForfeit(context);
        } else if (textLower.contains("choose a pilot")) {
            return evaluatePilotSelection(context);
        } else if (textLower.contains("choose card to cancel")) {
            return evaluateCancelSelection(context);
        } else if (textLower.contains("choose target") ||
                   textLower.contains("click 'done' to cancel")) {
            return evaluateTargetSelection(context);
        } else if (textLower.contains("choose") && textLower.contains("location")) {
            return evaluateLocationSelection(context);
        } else if (textLower.contains("starting location")) {
            return evaluateStartingLocation(context);
        } else if (textLower.contains("card to take into hand")) {
            return evaluateTakeIntoHand(context);
        } else if (textLower.contains("card to put on lost pile")) {
            return evaluateLostPileSelection(context);
        } else if (textLower.contains("defensive shield") ||
                   isShieldSelectionByContent(context)) {
            return evaluateShieldSelection(context);
        } else {
            // Unknown - create neutral scored actions
            return evaluateUnknown(context);
        }
    }

    /**
     * Sabacc value setting - random selection to break loops.
     */
    private List<EvaluatedAction> evaluateSabaccSetValue(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        for (String cardId : context.getCardIds()) {
            // Randomize scores to pick different cards each time
            float randomScore = VERY_BAD_DELTA + random.nextFloat() * 10;

            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                randomScore,
                "Set sabacc value (card " + cardId + ")"
            );
            action.addReasoning("Sabacc value (randomized)", randomScore);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Sabacc clone - avoid cloning.
     */
    private List<EvaluatedAction> evaluateSabaccClone(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                VERY_BAD_DELTA,
                "Clone sabacc value"
            );
            action.addReasoning("Avoid cloning sabacc cards", VERY_BAD_DELTA);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose where to deploy - evaluate locations.
     */
    private List<EvaluatedAction> evaluateDeployLocation(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Deploy to location " + cardId
            );

            // Try to get location info
            if (gameState != null) {
                PhysicalCard location = gameState.findCardById(Integer.parseInt(cardId));
                if (location != null) {
                    SwccgCardBlueprint blueprint = location.getBlueprint();
                    String title = location.getTitle();
                    action.setDisplayText("Deploy to " + (title != null ? title : "location"));

                    // Battleground bonus
                    if (blueprint != null && blueprint.getCardCategory() == CardCategory.LOCATION) {
                        if (title != null && (title.toLowerCase().contains("battleground") ||
                            isLikelyBattleground(blueprint))) {
                            action.addReasoning("Battleground location", 30.0f);
                        }
                    }
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose force to lose - pick cards we want to lose least.
     */
    private List<EvaluatedAction> evaluateForceLoss(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Lose force (card " + cardId + ")"
            );

            // Try to get card info to pick worst cards to lose
            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        if (blueprint != null) {
                            // Prefer losing low-destiny cards
                            Float destiny = blueprint.getDestiny();
                            if (destiny != null) {
                                if (destiny <= 2) {
                                    action.addReasoning("Low destiny - good to lose", 30.0f);
                                } else if (destiny >= 5) {
                                    action.addReasoning("High destiny - keep for draws", -40.0f);
                                }
                            }

                            // Don't want to lose priority cards
                            String title = card.getTitle();
                            if (title != null && AiPriorityCards.isPriorityCardByTitle(title)) {
                                action.addReasoning("Priority card - protect!", -100.0f);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose card to forfeit - pick lowest forfeit value.
     */
    private List<EvaluatedAction> evaluateForfeit(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Forfeit card " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        if (blueprint != null) {
                            // Prefer forfeiting low-forfeit cards
                            Float forfeit = blueprint.getForfeit();
                            if (forfeit != null) {
                                if (forfeit <= 2) {
                                    action.addReasoning(
                                        String.format("Low forfeit (%.0f) - good to forfeit", forfeit),
                                        40.0f
                                    );
                                } else if (forfeit >= 6) {
                                    action.addReasoning(
                                        String.format("High forfeit (%.0f) - keep alive", forfeit),
                                        -50.0f
                                    );
                                }
                            }

                            // Don't forfeit high-power cards
                            if (blueprint.hasPowerAttribute()) {
                                Float power = blueprint.getPower();
                                if (power != null && power >= 5) {
                                    action.addReasoning("High power - keep fighting", -30.0f);
                                }
                            }

                            // Don't forfeit unique characters lightly
                            if (blueprint.getUniqueness() == Uniqueness.UNIQUE) {
                                action.addReasoning("Unique card - protect", -20.0f);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose a pilot - pick best pilot by ability.
     */
    private List<EvaluatedAction> evaluatePilotSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Select pilot " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        if (blueprint != null) {
                            // Prefer high-ability pilots
                            if (blueprint.hasAbilityAttribute()) {
                                Float ability = blueprint.getAbility();
                                if (ability != null) {
                                    if (ability >= 4) {
                                        action.addReasoning("High ability pilot", 40.0f);
                                    } else if (ability >= 2) {
                                        action.addReasoning("Decent ability pilot", 20.0f);
                                    }
                                }
                            }

                            // Prefer pilots that add power
                            if (blueprint.hasPowerAttribute()) {
                                Float power = blueprint.getPower();
                                if (power != null && power >= 3) {
                                    action.addReasoning("Good power bonus", 20.0f);
                                }
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Choose card to cancel - cancel opponent's cards.
     */
    private List<EvaluatedAction> evaluateCancelSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Cancel card " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        String owner = card.getOwner();

                        // Cancel opponent's cards, not ours!
                        if (!playerId.equals(owner)) {
                            action.addReasoning("Opponent's card - cancel!", 100.0f);
                        } else {
                            action.addReasoning("Our card - don't cancel!", -200.0f);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Target selection for weapons/abilities - must select, don't cancel.
     */
    private List<EvaluatedAction> evaluateTargetSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        String playerId = context.getPlayerId();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                50.0f,
                "Target " + cardId
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        String owner = card.getOwner();
                        SwccgCardBlueprint blueprint = card.getBlueprint();

                        // Target opponent's cards
                        if (!playerId.equals(owner)) {
                            action.addReasoning("Target opponent's card", 50.0f);

                            // Prefer high-value targets
                            if (blueprint != null) {
                                if (blueprint.hasPowerAttribute()) {
                                    Float power = blueprint.getPower();
                                    if (power != null && power >= 5) {
                                        action.addReasoning("High-power target", 30.0f);
                                    }
                                }

                                if (blueprint.getUniqueness() == Uniqueness.UNIQUE) {
                                    action.addReasoning("Unique target", 20.0f);
                                }
                            }
                        } else {
                            action.addReasoning("Our card - don't target!", -200.0f);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Location selection - pick battlegrounds and force icon locations.
     */
    private List<EvaluatedAction> evaluateLocationSelection(DecisionContext context) {
        return evaluateDeployLocation(context);  // Same logic
    }

    /**
     * Starting location selection.
     */
    private List<EvaluatedAction> evaluateStartingLocation(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,
                "Starting location " + cardId
            );

            // All starting locations are generally good
            action.addReasoning("Starting location", 50.0f);
            actions.add(action);
        }

        return actions;
    }

    /**
     * Take into hand - prefer high-value cards.
     * Handles both in-play cards (by card ID) and reserve deck cards (by blueprint).
     * CRITICAL: Only score selectable cards - GEMP rejects non-selectable selections!
     */
    private List<EvaluatedAction> evaluateTakeIntoHand(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();

        logger.info("🔍 evaluateTakeIntoHand: {} cards, {} blueprints",
                   cardIds != null ? cardIds.size() : 0,
                   blueprints != null ? blueprints.size() : 0);

        for (int i = 0; i < cardIds.size(); i++) {
            String cardId = cardIds.get(i);
            String blueprintId = (blueprints != null && i < blueprints.size()) ? blueprints.get(i) : null;

            // CRITICAL: Skip non-selectable cards!
            if (!isCardSelectable(context, i)) {
                logger.debug("⚠️ Skipping non-selectable card at index {}: {}", i, cardId);
                continue;
            }

            // Get card info - try game state first, then use blueprint ID for priority card lookup
            String cardTitle = "Unknown";
            Float destiny = null;
            Float power = null;
            Float ability = null;
            CardCategory category = null;

            // Try to get card from game state first
            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        cardTitle = card.getTitle();
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        if (blueprint != null) {
                            try {
                                destiny = blueprint.getDestiny();
                            } catch (UnsupportedOperationException e) {
                                // Card type doesn't support destiny
                            }
                            if (blueprint.hasPowerAttribute()) {
                                power = blueprint.getPower();
                            }
                            if (blueprint.hasAbilityAttribute()) {
                                ability = blueprint.getAbility();
                            }
                            category = blueprint.getCardCategory();
                        }
                    }
                } catch (NumberFormatException e) {
                    // Card ID is temporary (like "temp0") - this is expected for reserve deck
                    // Use blueprint ID for display
                    if (blueprintId != null) {
                        cardTitle = "Card " + blueprintId;
                    }
                }
            }

            // Check for priority cards by blueprintId if we have one but couldn't get card info
            boolean isPriorityByBlueprint = false;
            int priorityScoreByBlueprint = 0;
            if (blueprintId != null) {
                isPriorityByBlueprint = AiPriorityCards.isPriorityCard(blueprintId);
                priorityScoreByBlueprint = AiPriorityCards.getProtectionScore(blueprintId);
            }

            // Create action with proper display text
            float baseScore = 50.0f;
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.SELECT_CARD,
                baseScore,
                "Take " + cardTitle + " into hand"
            );
            action.setCardName(cardTitle);
            if (blueprintId != null) {
                action.setBlueprintId(blueprintId);
            }

            // === SCORING LOGIC ===

            // High destiny cards are VERY valuable - they're used for destiny draws
            if (destiny != null) {
                if (destiny >= 6) {
                    action.addReasoning("Excellent destiny (" + destiny + ")", 60.0f);
                } else if (destiny >= 5) {
                    action.addReasoning("High destiny (" + destiny + ")", 40.0f);
                } else if (destiny >= 4) {
                    action.addReasoning("Good destiny (" + destiny + ")", 20.0f);
                } else if (destiny >= 3) {
                    action.addReasoning("Decent destiny (" + destiny + ")", 5.0f);
                } else if (destiny <= 1) {
                    action.addReasoning("Low destiny (" + destiny + ")", -20.0f);
                }
            }

            // Priority cards (Houjix, Sense, etc.) are always good
            // Check by title first (when we have the actual card)
            if (!cardTitle.equals("Unknown") && !cardTitle.startsWith("Card ") &&
                AiPriorityCards.isPriorityCardByTitle(cardTitle)) {
                int priorityScore = AiPriorityCards.getProtectionScoreByTitle(cardTitle);
                action.addReasoning("Priority card: " + cardTitle, priorityScore * 0.5f);
            } else if (isPriorityByBlueprint) {
                // Check by blueprint ID (when we only have the blueprintId)
                action.addReasoning("Priority card (by ID: " + blueprintId + ")", priorityScoreByBlueprint * 0.5f);
            }

            // Prefer characters with high power
            if (category == CardCategory.CHARACTER && power != null) {
                if (power >= 6) {
                    action.addReasoning("High power character (" + power + ")", 30.0f);
                } else if (power >= 4) {
                    action.addReasoning("Strong character (" + power + ")", 15.0f);
                }
            }

            // Prefer characters with high ability (can draw battle destiny)
            if (category == CardCategory.CHARACTER && ability != null && ability >= 4) {
                action.addReasoning("High ability (" + ability + ") - draws battle destiny", 25.0f);
            }

            // Locations are often good early game
            if (category == CardCategory.LOCATION) {
                int turnNumber = context.getTurnNumber();
                if (turnNumber <= 3) {
                    action.addReasoning("Location (good early game)", 20.0f);
                } else {
                    action.addReasoning("Location", 5.0f);
                }
            }

            // Log the decision
            logger.debug("🎯 {} ({}): score={}, destiny={}, power={}",
                        cardTitle, blueprintId != null ? blueprintId : cardId,
                        action.getScore(),
                        destiny != null ? destiny : "?",
                        power != null ? power : "?");

            actions.add(action);
        }

        // Sort by score descending for logging
        actions.sort((a, b) -> Float.compare(b.getScore(), a.getScore()));
        if (!actions.isEmpty()) {
            logger.info("✅ Best take into hand: {} (score: {})",
                       actions.get(0).getCardName(), actions.get(0).getScore());
        }

        return actions;
    }

    /**
     * Lost pile selection - prefer low-value cards.
     */
    private List<EvaluatedAction> evaluateLostPileSelection(DecisionContext context) {
        return evaluateForceLoss(context);  // Same logic
    }

    /**
     * Unknown decision type - neutral scoring with card name lookup.
     * CRITICAL: Only score selectable cards - GEMP rejects non-selectable selections!
     *
     * Ported from Python _evaluate_unknown() which scores based on card type.
     */
    private List<EvaluatedAction> evaluateUnknown(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        SwccgGame game = context.getGame();
        List<String> cardIds = context.getCardIds();
        List<String> blueprints = context.getBlueprints();

        // Determine base score - higher for gain/select decisions
        String textLower = context.getDecisionText() != null ? context.getDecisionText().toLowerCase(Locale.ROOT) : "";
        boolean isLossDecision = textLower.contains("lose") || textLower.contains("lost") ||
                                 textLower.contains("place in") || textLower.contains("put on");

        logger.info("🔍 evaluateUnknown: {} cards, {} blueprints for '{}' (loss={})",
                   cardIds != null ? cardIds.size() : 0,
                   blueprints != null ? blueprints.size() : 0,
                   context.getDecisionText(),
                   isLossDecision);

        int selectableCount = 0;
        int skippedCount = 0;

        for (int i = 0; i < cardIds.size(); i++) {
            String cardId = cardIds.get(i);
            String blueprintId = (blueprints != null && i < blueprints.size()) ? blueprints.get(i) : null;

            // CRITICAL: Skip non-selectable cards!
            if (!isCardSelectable(context, i)) {
                skippedCount++;
                logger.debug("⚠️ Skipping non-selectable card at index {}: {}", i, cardId);
                continue;
            }
            selectableCount++;

            String cardTitle = cardId;  // Default to cardId if we can't find name
            Float destiny = null;
            Float power = null;
            CardCategory category = null;

            // Try to get card info from game state
            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        if (card.getTitle() != null) {
                            cardTitle = card.getTitle();
                        }
                        SwccgCardBlueprint blueprint = card.getBlueprint();
                        if (blueprint != null) {
                            try {
                                destiny = blueprint.getDestiny();
                            } catch (UnsupportedOperationException e) {
                                // Card type doesn't support destiny
                            }
                            if (blueprint.hasPowerAttribute()) {
                                power = blueprint.getPower();
                            }
                            category = blueprint.getCardCategory();
                        }
                    }
                } catch (NumberFormatException e) {
                    // cardId may be temp0, temp1, etc. - use blueprintId for display
                    if (blueprintId != null) {
                        cardTitle = "Card " + blueprintId;
                    }
                }
            }

            // Base score beats PassEvaluator (~5-20)
            float baseScore = 30.0f;
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.UNKNOWN,
                baseScore,
                "Select " + cardTitle
            );
            action.setCardName(cardTitle);
            if (blueprintId != null) {
                action.setBlueprintId(blueprintId);
            }

            // Add randomization to avoid predictable patterns (like Python)
            float randomFactor = random.nextFloat() * 25.0f - 10.0f;  // -10 to +15
            action.addReasoning("Random factor", randomFactor);

            // Score based on card type (like Python)
            if (isLossDecision) {
                // For loss decisions: prefer effects/interrupts
                if (category == CardCategory.EFFECT || category == CardCategory.INTERRUPT) {
                    action.addReasoning("Effect/Interrupt - OK to lose", 25.0f);
                } else if (category == CardCategory.CHARACTER) {
                    action.addReasoning("Character - avoid losing", -15.0f);
                } else if (category == CardCategory.STARSHIP) {
                    action.addReasoning("Starship - avoid losing", -15.0f);
                } else if (category == CardCategory.VEHICLE) {
                    action.addReasoning("Vehicle - avoid losing", -10.0f);
                } else if (category == CardCategory.LOCATION) {
                    action.addReasoning("Location - avoid losing", -20.0f);
                }
            } else {
                // For gain/select decisions: prefer deployables
                if (category == CardCategory.CHARACTER) {
                    action.addReasoning("Character - valuable", 10.0f);
                } else if (category == CardCategory.STARSHIP) {
                    action.addReasoning("Starship - valuable", 8.0f);
                } else if (category == CardCategory.LOCATION) {
                    action.addReasoning("Location - valuable", 10.0f);
                }
            }

            // Check for priority cards
            if (blueprintId != null && AiPriorityCards.isPriorityCard(blueprintId)) {
                int priorityScore = AiPriorityCards.getProtectionScore(blueprintId);
                action.addReasoning("Priority card", priorityScore * 0.3f);
            }

            actions.add(action);
        }

        logger.info("🔍 evaluateUnknown: {} selectable, {} skipped (non-selectable)",
                   selectableCount, skippedCount);

        if (actions.isEmpty()) {
            logger.warn("⚠️ evaluateUnknown: No selectable cards! Decision may fail.");
        }

        return actions;
    }

    /**
     * Check if a location is likely a battleground.
     */
    private boolean isLikelyBattleground(SwccgCardBlueprint blueprint) {
        // Sites and systems are often battlegrounds
        // This is a heuristic - actual battleground status comes from card data
        return blueprint != null && blueprint.getCardCategory() == CardCategory.LOCATION;
    }

    /**
     * Check if a card at given index is selectable.
     * CRITICAL: GEMP rejects selection of non-selectable cards!
     */
    private boolean isCardSelectable(DecisionContext context, int index) {
        List<Boolean> selectable = context.getSelectable();
        if (selectable == null || selectable.isEmpty()) {
            // No selectable info - assume all are selectable
            return true;
        }
        if (index >= selectable.size()) {
            // Index out of bounds - assume selectable
            return true;
        }
        Boolean isSelectable = selectable.get(index);
        return isSelectable == null || isSelectable;
    }

    /**
     * Reserve deck selection - evaluate cards by blueprint.
     * Used when selecting from Reserve Deck (e.g., deploying shields via starting effect).
     * Uses DeployPhasePlanner when available to select cards that fit the deployment plan.
     */
    private List<EvaluatedAction> evaluateReserveDeckSelection(DecisionContext context, String textLower) {
        List<EvaluatedAction> actions = new ArrayList<>();
        List<String> blueprints = context.getBlueprints();
        ShieldStrategy shieldStrategy = context.getShieldStrategy();
        DeployPhasePlanner planner = context.getDeployPhasePlanner();
        SwccgGame game = context.getGame();
        Side side = context.getSide();
        String playerId = context.getPlayerId();
        int turnNumber = context.getTurnNumber();

        logger.info("[CardSelectionEvaluator] Evaluating Reserve Deck selection: {}", textLower);

        // Get deployment plan if available
        DeploymentPlan plan = null;
        if (planner != null && game != null && side != null && playerId != null) {
            plan = planner.createPlan(game, playerId, side);
            if (plan != null) {
                logger.info("[CardSelectionEvaluator] Using deployment plan: strategy={}, instructions={}",
                    plan.getStrategy(), plan.getInstructions().size());
            }
        }

        for (int i = 0; i < blueprints.size(); i++) {
            String blueprintId = blueprints.get(i);

            // Use index as action ID for blueprint-based selections
            EvaluatedAction action = new EvaluatedAction(
                String.valueOf(i),
                ActionType.DEPLOY,
                50.0f,
                "Deploy " + blueprintId
            );

            // === Check deployment plan first ===
            if (plan != null && !plan.getInstructions().isEmpty()) {
                DeploymentInstruction instruction = plan.getInstructionForCard(blueprintId);
                if (instruction != null) {
                    // Card is in deployment plan - high priority
                    action.addReasoning("IN DEPLOYMENT PLAN: " + plan.getStrategy(), 100.0f);
                    logger.info("[ReserveDeck] {} IN PLAN - high priority", blueprintId);
                } else if (plan.getHoldBackCards().contains(blueprintId)) {
                    // Card should be held back
                    action.addReasoning("HOLD BACK: save for later", -50.0f);
                    logger.debug("[ReserveDeck] {} should be held back", blueprintId);
                }
            }

            // === Shield scoring ===
            if (shieldStrategy != null) {
                // Check if this is a defensive shield by blueprint pattern
                float shieldScore = shieldStrategy.scoreShield(blueprintId, blueprintId, turnNumber);

                if (shieldScore > -50) {
                    // Likely a shield - use shield scoring
                    // Add to existing score rather than replacing
                    action.addReasoning("Shield scoring", shieldScore);
                    String description = shieldStrategy.getShieldDescription(blueprintId, blueprintId);
                    logger.info("[ReserveDeck] Shield {}: score={} ({})", blueprintId, shieldScore, description);
                }
            }

            actions.add(action);
        }

        return actions;
    }

    /**
     * Check if this is a shield selection by examining the available cards.
     * Similar to Python's approach of checking if majority of options are shields.
     */
    private boolean isShieldSelectionByContent(DecisionContext context) {
        GameState gameState = context.getGameState();
        List<String> cardIds = context.getCardIds();

        if (gameState == null || cardIds == null || cardIds.isEmpty()) {
            return false;
        }

        int shieldCount = 0;
        for (String cardId : cardIds) {
            try {
                PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                if (card != null) {
                    SwccgCardBlueprint blueprint = card.getBlueprint();
                    if (blueprint != null &&
                        blueprint.getCardCategory() == CardCategory.DEFENSIVE_SHIELD) {
                        shieldCount++;
                    }
                }
            } catch (NumberFormatException e) {
                // Ignore
            }
        }

        // If majority are shields, treat as shield selection
        return shieldCount > 0 && shieldCount >= cardIds.size() * 0.5;
    }

    /**
     * Defensive shield selection - use ShieldStrategy scoring.
     */
    private List<EvaluatedAction> evaluateShieldSelection(DecisionContext context) {
        List<EvaluatedAction> actions = new ArrayList<>();
        GameState gameState = context.getGameState();
        ShieldStrategy shieldStrategy = context.getShieldStrategy();
        int turnNumber = context.getTurnNumber();

        logger.info("[CardSelectionEvaluator] Evaluating DEFENSIVE SHIELD selection");

        for (String cardId : context.getCardIds()) {
            EvaluatedAction action = new EvaluatedAction(
                cardId,
                ActionType.DEPLOY,
                50.0f,  // Base score
                "Deploy shield"
            );

            if (gameState != null) {
                try {
                    PhysicalCard card = gameState.findCardById(Integer.parseInt(cardId));
                    if (card != null) {
                        String title = card.getTitle();
                        String blueprintId = card.getBlueprintId(true);
                        SwccgCardBlueprint blueprint = card.getBlueprint();

                        action.setDisplayText("Shield: " + (title != null ? title : cardId));

                        // Verify it's actually a defensive shield
                        if (blueprint != null &&
                            blueprint.getCardCategory() == CardCategory.DEFENSIVE_SHIELD) {

                            // Use ShieldStrategy for scoring
                            if (shieldStrategy != null && blueprintId != null && title != null) {
                                float shieldScore = shieldStrategy.scoreShield(
                                    blueprintId, title, turnNumber);

                                // Set score directly (ShieldStrategy fully controls priority)
                                action.setScore(shieldScore);
                                String description = shieldStrategy.getShieldDescription(blueprintId, title);
                                action.addReasoning("Shield: " + description, 0.0f);

                                logger.info("[Shield] {}: score={} ({})", title, shieldScore, description);
                            } else {
                                // Fallback if no shield strategy
                                action.addReasoning("Defensive shield (no strategy)", 50.0f);
                            }
                        } else {
                            // Not a shield - low priority
                            action.addReasoning("Not a defensive shield", -50.0f);
                        }
                    }
                } catch (NumberFormatException e) {
                    action.addReasoning("Invalid card ID", -100.0f);
                }
            }

            actions.add(action);
        }

        return actions;
    }
}
