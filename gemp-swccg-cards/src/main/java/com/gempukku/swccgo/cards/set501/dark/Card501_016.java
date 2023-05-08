package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.ControlsCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 21
 * Type: Location
 * Subtype: Site
 * Title: Tatooine: Great Pit Of Carkoon (V)
 */
public class Card501_016 extends AbstractSite {
    public Card501_016() {
        super(Side.DARK, Title.Great_Pit_Of_Carkoon, Title.Tatooine, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLocationDarkSideGameText("If you control while Jabba's Sail Barge here, Scum And Villainy may not be canceled.");
        setLocationLightSideGameText("Once per game, may deploy Boba here from Reserve Deck (reshuffle) or Lost Pile.");
        addIcon(Icon.DARK_FORCE, 1);
        addIcon(Icon.LIGHT_FORCE, 1);
        addIcons(Icon.JABBAS_PALACE, Icon.EXTERIOR_SITE, Icon.PLANET, Icon.VIRTUAL_SET_21);
        addKeywords(Keyword.PIT);
        setTestingText("Tatooine: Great Pit Of Carkoon (V)");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotBeCanceledModifier(self, Filters.Scum_And_Villainy, new AndCondition(new HereCondition(self, Filters.Jabbas_Sail_Barge), new ControlsCondition(playerOnDarkSideOfLocation, self))));
        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextLightSideTopLevelActions(String playerOnLightSideOfLocation, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.TATOOINE_GREAT_PIT_OF_CARKOON__DEPLOY_BOBA;
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)) {

                if (GameConditions.canDeployCardFromReserveDeck(game, playerOnLightSideOfLocation, self, gameTextActionId, Persona.BOBA_FETT)) {
                    TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
                    action.setText("Deploy Boba from Reserve Deck");
                    action.appendUsage(
                            new OncePerGameEffect(action));
                    action.appendEffect(
                            new DeployCardToLocationFromReserveDeckEffect(action, Filters.Boba_Fett, Filters.here(self), true));
                    actions.add(action);
                }

            if (GameConditions.canDeployCardFromLostPile(game, playerOnLightSideOfLocation, self, gameTextActionId, Persona.BOBA_FETT)) {
                TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
                action.setText("Deploy Boba from Lost Pile");
                action.appendUsage(
                        new OncePerGameEffect(action));
                action.appendEffect(
                        new DeployCardToLocationFromLostPileEffect(action, Filters.Boba_Fett, Filters.here(self), false));
                actions.add(action);
            }
        }
        return actions;
    }

}