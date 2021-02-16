package com.gempukku.swccgo.draft2;

import com.gempukku.swccgo.game.CardCollection;

public interface SoloDraft {
    CardCollection initializeNewCollection(long seed);

    Iterable<DraftChoice> getAvailableChoices(long seed, int stage, CardCollection currentCards, String currentChoice);

    CardCollection getCardsForChoiceId(String choiceId, long seed, int stage, CardCollection currentCards);

    boolean hasNextStage(long seed, int stage);

    int stageCount();

    String draftSide(int stage);

    int fixedCardCount();

    int currentStage(CardCollection currentCards);

    String getFormat();

    interface DraftChoice {
        String getChoiceId();
        String getBlueprintId();
        String getChoiceUrl();
        String getObjPackDescription();
    }
}
