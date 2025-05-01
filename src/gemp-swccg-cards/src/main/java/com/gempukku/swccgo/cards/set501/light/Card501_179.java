package com.gempukku.swccgo.cards.set501.light;

import java.util.Collections;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.StackCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: Patience!
 */
public class Card501_179 extends AbstractEpicEventDeployable {
    public Card501_179() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Patience, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If your [Dagobah] objective on table, deploy on table and stack up to six Jedi Tests from your Reserve Deck face up here. Only Luke may attempt Jedi Tests. I Won't Fail You: You may deploy face up Jedi Tests from here as if from hand. I Saw A City In The Clouds: Once per turn, may [download] Bespin system or a Cloud City site. I've Got To Go To Them: Once per opponent's control phase, if you just lost Force and you do not occupy a battleground with a [Cloud City] Rebel, turn a Jedi Test here face down.");
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_25);
        setTestingText("Patience!");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.and(Filters.your(playerId), Icon.DAGOBAH, Filters.Objective));
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.justDeployed(game, effectResult, self)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerId);
            // Perform result(s)
            action.appendEffect(
                    new StackCardsFromReserveDeckEffect(action, playerId, 1, 6, self, Filters.Jedi_Test)
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}
