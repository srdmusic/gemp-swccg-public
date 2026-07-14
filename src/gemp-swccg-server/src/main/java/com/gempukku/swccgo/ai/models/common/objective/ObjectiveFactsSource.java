package com.gempukku.swccgo.ai.models.common.objective;

import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;

import java.util.Set;

/** Boundary view of the live per-bot ObjectiveAnalyzer; refresh once, then project facts read-only. */
public interface ObjectiveFactsSource {

    void analyze(SwccgGame game, String playerId, Side side);

    void refreshFlipStatus(GameState gameState, String playerId);

    boolean isAnalyzed();

    boolean isFlipped();

    String getObjectiveTitle();

    String getObjectiveBlueprintId();

    String getObjectiveGameText();

    ObjectiveFacts.ProfileResolution getProfileResolution();

    Set<String> getFlipConditionLocationFragments();

    Set<String> getFlipBackLocationFragments();

    Set<String> getRequiredCardsOnTable();

    Set<String> getPullableCards();

    Set<String> getStartingLocationIds();

    Set<String> getStartingLocationFragments();

    Set<String> getStartingEffectIds();

    Set<String> getStartingEffectFragments();

    Set<String> getStartingInterruptIds();

    Set<String> getStartingInterruptFragments();

    Set<String> getFlipCriticalControlCardIds();

    String getFlipCriticalControlSite();

    String getFlipCriticalControlCard();

    String getFlipConditionText();

    String getFlipBackConditionText();

    boolean requiresOccupy();

    boolean requiresControl();

    boolean flipBackRequiresOccupy();

    boolean flipBackRequiresControl();

    boolean isMyLord();

    boolean isEndorOperations();

    boolean isInvasion();

    boolean isHuntDownV();

    boolean isWantThatMap();

    Set<String> getStrategyCharacterTokens(SwccgGame game, String playerId);

    boolean isObjectiveRelevantLocation(PhysicalCard location, SwccgGame game, String playerId);

    boolean isFlipBackProtectionLocation(String locationTitle);
}
