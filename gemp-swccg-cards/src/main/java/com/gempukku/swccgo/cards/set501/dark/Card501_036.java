package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * Set: Set 19
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Capital City
 */
public class Card501_036 extends AbstractSite {
    public Card501_036() {
        super(Side.DARK, Title.Lothal_Capital_City, Title.Lothal);
        setLocationDarkSideGameText("If you just revealed an 'artwork' card during battle here, may retrieve a card of the same card type.");
        setLocationLightSideGameText("If you control, your total power is +1 at related locations.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_19);
        setTestingText("Lothal: Capital City (DARK)");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextDarkSideOptionalAfterTriggers(String playerOnDarkSideOfLocation, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isDuringBattleAt(game, self)
                && effectResult.getType() == EffectResult.Type.ARTWORK_CARD_REVEALED) {

            PhysicalCard artwork = ((ArtworkCardRevealedResult)effectResult).getCard();

            if (artwork != null) {
                Set<CardType> cardTypes = game.getModifiersQuerying().getCardTypes(game.getGameState(), artwork);
                Filter filterForCardInLostPile = Filters.none;
                for (CardType cardType : cardTypes) {
                    filterForCardInLostPile = Filters.or(filterForCardInLostPile, Filters.type(cardType));
                }

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerOnDarkSideOfLocation, gameTextSourceCardId, gameTextActionId);
                action.setText("Retrieve a card with a matching type");
                action.setActionMsg("Retrieve a card with a type matching " + GameUtils.getCardLink(artwork));
                // Perform result(s)
                action.appendEffect(
                        new RetrieveCardEffect(action, playerOnDarkSideOfLocation, filterForCardInLostPile));
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextLightSideWhileActiveModifiers(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new TotalPowerModifier(self, Filters.relatedLocation(self), new ControlsCondition(playerOnLightSideOfLocation, self), 1, playerOnLightSideOfLocation));
        return modifiers;
    }
}
