package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.DoNothingEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.ShowCardOnScreenEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromForcePileEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromForcePileEffect;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 17
 * Type: Character
 * Subtype: Republic
 * Title: Lott Dod (V)
 */
public class Card501_016 extends AbstractRepublic {
    public Card501_016() {
        super(Side.DARK, 2, 3, 1, 3, 5, "Lott Dod", Uniqueness.UNIQUE);
        setPolitics(4);
        setLore("Primary Neimoidian senator who represents the Trade Federation in the Galactic Senate. Thwarted attempts by Amidala to end the blockade of Naboo.");
        setGameText("While present at Theed Palace Throne Room, during your deploy phase, may search your Force pile to reveal a [Presence] droid; if you do, may take that card (or another) into hand; reshuffle.");
        addIcons(Icon.CORUSCANT, Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        addKeywords(Keyword.SENATOR);
        setSpecies(Species.NEIMOIDIAN);
        addPersona(Persona.LOTT);
        setTestingText("Lott Dodd (V)");
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.LOTT_DOD__UPLOAD_PRESENCE_DROID_FROM_FORCE_PILE;
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.DEPLOY)
                && GameConditions.hasForcePile(game, playerId)
                && GameConditions.isPresentAt(game, self, Filters.Theed_Palace_Throne_Room)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Reveal card from Force Pile");
            action.setText("Reveal [Presence] droid from Force Pile");
            action.appendUsage(
                    new OncePerPhaseEffect(action)
            );
            action.appendTargeting(
                    new ChooseCardFromForcePileEffect(action, playerId, Filters.and(Filters.droid, Filters.icon(Icon.PRESENCE))) {
                        @Override
                        protected void cardSelected(SwccgGame game, final PhysicalCard selectedCard) {
                            if (selectedCard != null) {
                                action.appendEffect(
                                        new ShowCardOnScreenEffect(action, selectedCard)
                                );
                                action.appendEffect(
                                        new PlayoutDecisionEffect(action, playerId, new YesNoDecision("Take " + GameUtils.getCardLink(selectedCard) + " into hand?") {
                                            @Override
                                            protected void yes() {
                                                action.appendEffect(
                                                        new TakeCardIntoHandFromForcePileEffect(action, playerId, selectedCard, true)
                                                );
                                            }

                                            @Override
                                            protected void no() {
                                                action.appendEffect(
                                                        new TakeCardIntoHandFromForcePileEffect(action, playerId, Filters.not(selectedCard), true)
                                                );
                                            }
                                        })
                                );
                            } else {
                                action.appendEffect(
                                        new DoNothingEffect(action)
                                );
                            }
                        }
                    }
            );
            return Collections.singletonList(action);
        }

        return null;
    }
}
