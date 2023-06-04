package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.actions.ObjectiveDeployedTriggerAction;
import com.gempukku.swccgo.cards.effects.ConvertLocationByRaisingToTopEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ConvertLocationResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Objective
 * Title: More Systems Will Rally To Our Cause / The Galaxy Torn Apart
 */
public class Card501_019 extends AbstractObjective {
    public Card501_019() {
        super(Side.DARK, 0, "More Systems Will Rally To Our Cause", ExpansionSet.PLAYTESTING, Rarity.V);
        setFrontOfDoubleSidedCard(true);
        setGameText("Deploy [Separatist] Geonosis system, two [Clone Army] systems, and Droid Racks. \n" +
                "For remainder of game, you may not deploy non-[Episode I] characters, non-[Episode I] starships, or non-[Episode I] vehicles. If your [Episode I] system was just converted, raise it to the top. Once per turn, may deploy a battleground site related to your [Clone Army] or [Separatist] system from Reserve Deck; reshuffle. \n" +
                "Flip this card if two [Separatist] systems on table.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        setTestingText("More Systems Will Rally To Our Cause");
    }

    @Override
    protected ObjectiveDeployedTriggerAction getGameTextWhenDeployedAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        final ObjectiveDeployedTriggerAction action = new ObjectiveDeployedTriggerAction(self);
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.SEPARATIST, Filters.Geonosis_system), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Chose a [Separatist] Geonosis to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.CLONE_ARMY, Filters.system), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Chose a [Clone Army] system  to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.and(Icon.CLONE_ARMY, Filters.system), true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Chose a [Clone Army] system  to deploy";
                    }
                });
        action.appendRequiredEffect(
                new DeployCardFromReserveDeckEffect(action, Filters.Droid_Racks, true, false) {
                    @Override
                    public String getChoiceText() {
                        return "Deploy Droid Racks";
                    }
                });
        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String opponent = game.getOpponent(self.getOwner());
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.not(Icon.EPISODE_I), Filters.or(Filters.character, Filters.starship, Filters.vehicle)), self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.MORE_AND_MORE_SYSTEMS_ARE_JOINING_THE_SEPARATISTS__DEPLOY_BATTLEGROUND_SITE;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {

            Filter systemFilter = Filters.and(Filters.your(self), Filters.or(Icon.SEPARATIST, Icon.CLONE_ARMY), Filters.system);

            if (GameConditions.canSpotLocation(game, systemFilter)) {
                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Deploy a site from Reserve Deck");
                action.setActionMsg("Deploy a battleground site related to your [Separatist] or [Clone Army] system from Reserve Deck");

                Filter siteFilter = Filters.none;
                for(PhysicalCard system: Filters.filterTopLocationsOnTable(game, systemFilter)) {
                    siteFilter = Filters.or(siteFilter, Filters.relatedLocationEvenWhenNotInPlay(system));
                }

                // Update usage limit(s)
                action.appendUsage(
                        new OncePerTurnEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.title("Separatist Command Center"), Filters.and(Filters.site, siteFilter)), Filters.battleground, true));
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canBeFlipped(game, self)
                && GameConditions.canSpot(game, self, 2, Filters.and(Icon.SEPARATIST, Filters.system))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_3;

        if (TriggerConditions.justConvertedLocation(game, effectResult)) {
            PhysicalCard newLocation = ((ConvertLocationResult)effectResult).getNewLocation();
            PhysicalCard oldLocation = ((ConvertLocationResult)effectResult).getOldLocation();
            if (oldLocation != null
                    && Filters.and(Filters.your(self.getOwner()), Icon.EPISODE_I, Filters.system).accepts(game, oldLocation)) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Raise converted location to top");
                action.setPerformingPlayer(self.getOwner());
                action.appendEffect(
                        new ConvertLocationByRaisingToTopEffect(action, newLocation, true));
                actions.add(action);
            }
        }
        return actions;
    }
}