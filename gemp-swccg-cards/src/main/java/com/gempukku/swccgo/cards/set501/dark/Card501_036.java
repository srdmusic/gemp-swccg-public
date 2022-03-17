package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OccupiesCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeOneCardIntoHandFromOffTableEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ArtworkCardRevealedResult;

import java.util.*;

/**
 * Set: Set 18
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Capital City
 */
public class Card501_036 extends AbstractSite {
    public Card501_036() {
        super(Side.DARK, "Lothal: Capital City", Title.Lothal);
        setLocationDarkSideGameText("If you just revealed an 'artwork' card during battle here, may retrieve a card of the same card type.");
        setLocationLightSideGameText("While occupied, related spaceport sites are immune to No Escape and Ounee Ta.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 2);
        addIcons(Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_18);
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
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.relatedSite(self), Filters.spaceport_site), new OrCondition(new OccupiesCondition(playerOnLightSideOfLocation, self), new OccupiesCondition(game.getOpponent(playerOnLightSideOfLocation), self)), Title.No_Escape));
        modifiers.add(new ImmuneToTitleModifier(self, Filters.and(Filters.relatedSite(self), Filters.spaceport_site), new OrCondition(new OccupiesCondition(playerOnLightSideOfLocation, self), new OccupiesCondition(game.getOpponent(playerOnLightSideOfLocation), self)), Title.Ounee_Ta));
        return modifiers;
    }
}
