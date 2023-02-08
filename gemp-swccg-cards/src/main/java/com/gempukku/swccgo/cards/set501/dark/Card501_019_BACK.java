package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.ConvertLocationByRaisingToTopEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
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
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.ConvertLocationResult;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Objective
 * Title: More And More Systems Are Joining The Separatists / The Galaxy Torn Apart
 */
public class Card501_019_BACK extends AbstractObjective {
    public Card501_019_BACK() {
        super(Side.DARK, 7, "The Galaxy Torn Apart", ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("While this side up, during your control phase, opponent loses X Force, where X = number of systems you occupy where you also occupy a related site. Once per turn, may deploy a [Separatist] character from Reserve Deck to a site that is part of a system named in that character's game text; reshuffle. At systems related to sites you occupy, your [Separatist] starships are immune to attrition. \n" +
                "Flip this card if fewer than two [Separatist] systems on table.");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_21);
        setTestingText("The Galaxy Torn Apart");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.and(Filters.not(Icon.EPISODE_I), Filters.or(Filters.character, Filters.starship, Filters.vehicle)), self.getOwner()));
        modifiers.add(new ImmuneToAttritionModifier(self, Filters.and(Filters.your(self), Icon.SEPARATIST, Filters.starship, Filters.at(Filters.relatedSystemTo(self, Filters.and(Filters.site, Filters.occupies(self.getOwner())))))));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);

        List<TopLevelGameTextAction> actions = new LinkedList<>();

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
                actions.add(action);
            }
        }


        gameTextActionId = GameTextActionId.THE_GALAXY_TORN_APART__DEPLOY_CHARACTER_FROM_RESERVE_DECK;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canSpot(game, self, Filters.site)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a [Separatist] character");
            action.setActionMsg("Deploy a [Separatist] character from Reserve Deck to a site that is part of a system named in that character's game text");

            Collection<PhysicalCard> sites = Filters.filterTopLocationsOnTable(game, Filters.site);
            final Collection<String> systemNames = new HashSet<>();

            Filter characterFilter = Filters.none;

            for(PhysicalCard site: sites) {
                String systemName = site.getPartOfSystem();
                characterFilter = Filters.or(characterFilter, Filters.and(Filters.character, Filters.gameTextContains(systemName), Filters.deployableToLocation(self, Filters.and(Filters.site, Filters.partOfSystem(systemName)), false, 0)));
                systemNames.add(systemName);
            }


            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            action.appendEffect(
                    new ChooseCardFromReserveDeckEffect(action, playerId, characterFilter) {
                        @Override
                        protected void cardSelected(SwccgGame game, PhysicalCard selectedCard) {
                            Filter siteFilter = Filters.none;
                            for (String system: systemNames) {
                                if (Filters.gameTextContains(system).accepts(game, selectedCard)) {
                                    siteFilter = Filters.or(siteFilter, Filters.partOfSystem(system));
                                }
                            }

                            // Perform result(s)
                            action.appendEffect(
                                    new DeployCardToTargetFromReserveDeckEffect(action, selectedCard, Filters.and(Filters.site, siteFilter), false, false, true));
                        }
                    });
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)) {
            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.system, Filters.occupies(playerId), Filters.relatedSystemTo(self, Filters.and(Filters.site, Filters.occupies(playerId)))));
            if (numForce > 0) {

                final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerPhaseEffect(action));
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                return Collections.singletonList(action);
            }
        }

        return actions;
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
                && !GameConditions.canSpot(game, self, 2, Filters.and(Icon.SEPARATIST, Filters.system))) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setSingletonTrigger(true);
            action.setText("Flip");
            action.setActionMsg(null);
            // Perform result(s)
            action.appendEffect(
                    new FlipCardEffect(action, self));
            actions.add(action);
        }


        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;
        // Check condition(s)
        // Check if reached end of each control phase and action was not performed yet.
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.CONTROL, playerId)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)) {

            int numForce = Filters.countTopLocationsOnTable(game, Filters.and(Filters.system, Filters.occupies(playerId), Filters.relatedSystemTo(self, Filters.and(Filters.site, Filters.occupies(playerId)))));
            if (numForce > 0) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setPerformingPlayer(playerId);
                action.setText("Make opponent lose " + numForce + " Force");
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, opponent, numForce));
                actions.add(action);
            }
        }

        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_3;

        if (TriggerConditions.justConvertedLocation(game, effectResult)) {

            System.out.println("converted location");
            PhysicalCard newLocation = ((ConvertLocationResult)effectResult).getNewLocation();
            PhysicalCard oldLocation = ((ConvertLocationResult)effectResult).getOldLocation();

            System.out.println("new: "+newLocation.getTitle());
            System.out.println("old: "+oldLocation.getTitle());
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