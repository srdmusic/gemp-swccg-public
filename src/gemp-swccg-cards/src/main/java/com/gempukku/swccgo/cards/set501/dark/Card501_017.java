package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractAlien;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.ConvertLocationByRaisingToTopEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Species;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.PutCardFromHandOnForcePileEffect;
import com.gempukku.swccgo.logic.effects.SendMessageEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotHaveGameTextCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Alien
 * Title: Salacious Crumb (V)
 */
public class Card501_017 extends AbstractAlien {
    public Card501_017() {
        super(Side.DARK, 3, 2, 1, 1, 3, Title.Salacious_Crumb, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Male Kowakian. Prankster. Humiliates others for Jabba's amusement. His life depends on making Jabba laugh at least once per day.");
        setGameText("Game text of your alien leaders here may not be canceled. If at a converted Jabba's Palace site, may raise yours to the top. If opponent just deployed a character here, may place a card from hand on Force Pile ('AH-hahahaha!').");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_25);
        setSpecies(Species.KOWAKIAN);
        setVirtualSuffix(true);
        setTestingText("Salacious Crumb (V)");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {

        // Check condition(s)
        if(TriggerConditions.justDeployedToLocation(game, effectResult, game.getOpponent(playerId), Filters.character, Filters.here(self))
            && GameConditions.hasHand(game, playerId)){

            OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place card from hand on Force Pile");
            action.setActionMsg("Place a card from hand on Force Pile");

            // Send Easter Egg Message
            action.appendCost(
                new SendMessageEffect(action, "Salacious Crumb: AH-hahahaha!"));

            // Perform result(s)
            action.appendEffect(new PutCardFromHandOnForcePileEffect(action, playerId));

            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        Filter yourAlienLeadersHere = Filters.and(Filters.your(self), Filters.alien, Filters.leader, Filters.here(self));
        modifiers.add(new MayNotHaveGameTextCanceledModifier(self, yourAlienLeadersHere));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)

        if (GameConditions.isAtLocation(game, self, Filters.and(Filters.canBeConvertedByRaisingYourLocationToTop(playerId), Filters.Jabbas_Palace_site))) {
            final PhysicalCard location = game.getModifiersQuerying().getLocationThatCardIsAt(game.getGameState(), self);
            if (location != null) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
                action.setText("Raise converted site to the top");
                action.setActionMsg("Raise converted site to the top to convert " + GameUtils.getCardLink(location));
                action.addAnimationGroup(location);
                // Perform result(s)
                action.appendEffect(
                        new ConvertLocationByRaisingToTopEffect(action, location, true));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
