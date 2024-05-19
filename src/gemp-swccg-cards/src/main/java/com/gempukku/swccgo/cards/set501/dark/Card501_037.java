package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromUsedPileEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class Card501_037 extends AbstractNormalEffect {
    public Card501_037() {
        super(Side.DARK, 3, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Dark_Deal, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("'Perhaps you think you're being treated unfairly?' 'No.' 'Good. It would be unfortunate if I had to leave a garrison here.'");
        setGameText("If you control two Cloud City sites and opponent controls none, deploy on Bespin. At sites where you have an alien/Imperial pair, your Force drains are +1 and, if you just won a battle there, may take any one card into hand from Used Pile; reshuffle.");
        addImmuneToCardTitle(Title.Alter);
        addIcons(Icon.CLOUD_CITY, Icon.VIRTUAL_SET_23);
        setVirtualSuffix(true);
        setTestingText("Dark Deal (V)");
    }
    
    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Bespin_system;
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.controls(game, playerId, 2, Filters.Cloud_City_site)
            && !GameConditions.controls(game, game.getOpponent(playerId), Filters.Cloud_City_site);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ForceDrainModifier(self, Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.alien, Filters.Imperial)), 1, playerId));
        return modifiers;
    }


    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.wonBattle(game, effectResult, Filters.and(Filters.your(self), Filters.alien, Filters.Imperial))) {
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Take a card into hand from Used Pile");
            action.setActionMsg("Take a card into handle from Used Pile; reshuffle");
            action.appendEffect(
                new TakeCardIntoHandFromUsedPileEffect(action, playerId, true));
            return Collections.singletonList(action);
        }
        return null;
    }

}
