package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSystem;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Location
 * Subtype: System
 * Title: Geonosis
 */
public class Card501_020 extends AbstractSystem {
    public Card501_020() {
        super(Side.DARK, Title.Geonosis, 7, ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setLocationDarkSideGameText("If opponent just initiated a Force drain (or won a battle) here and no Geonosis sites on table, they 'conquer' (flip) Geonosis.");
        setLocationLightSideGameText("If you just initiated a Force drain (or won a battle) at a Geonosis site, 'conquer' (flip) Geonosis.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.SEPARATIST, Icon.EPISODE_I, Icon.PLANET, Icon.VIRTUAL_SET_24);
        setMayNotBePlacedInReserveDeck(true);
        setTestingText("Geonosis (Separatist controlled)");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextLightSideRequiredAfterTriggers(String playerOnLightSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        if (GameConditions.canBeFlipped(game, self)
                && (TriggerConditions.forceDrainInitiatedBy(game, effectResult,  playerOnLightSideOfLocation, Filters.Geonosis_site)
                || TriggerConditions.wonBattleAt(game, effectResult, playerOnLightSideOfLocation, Filters.Geonosis_site))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Conquer Geonosis");
            action.appendEffect(new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextDarkSideRequiredAfterTriggers(String playerOnDarkSideOfLocation, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerOnDarkSideOfLocation);
        if (GameConditions.canBeFlipped(game, self)
                && !Filters.canSpotFromTopLocationsOnTable(game, Filters.Geonosis_site)
                && (TriggerConditions.forceDrainInitiatedBy(game, effectResult,  opponent, self)
                || TriggerConditions.wonBattleAt(game, effectResult, opponent, self))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Conquer Geonosis");
            action.appendEffect(new FlipCardEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }
}