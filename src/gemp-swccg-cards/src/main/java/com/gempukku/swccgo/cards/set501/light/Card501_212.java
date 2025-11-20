package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.MoveAsReactEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Persona;
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
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotHavePowerReducedModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ResetDeployCostToLocationModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Effect
  * Title: Launching The Assault (V)
 */

public class Card501_212 extends AbstractNormalEffect {
    public Card501_212() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Launching_The_Assault, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("By recruiting the Mon Calamari, a race of master shipwrights, the Rebel starfleet gained capital starships rivaling the dreaded Imperial Star Destroyers.");
        setGameText("If your Endor (or Rebel Base) location on table, deploy on table. May [download] Home One or Sullust. Home One is deploy = 8 to an [Endor] or [Death Star II] system and its power may not be reduced. Once per game, Home One may move as a 'react' to [Death Star II] Falcon's location (or vice versa). [Immune to Alter.]");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_26);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("Launching The Assault (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpotFromTopLocationsOnTable(game, Filters.and(Filters.your(playerId), Filters.or(Filters.Endor_location, Filters.Rebel_Base_location)));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();

        modifiers.add(new ResetDeployCostToLocationModifier(self, Filters.Home_One, 8, Filters.and(Filters.or(Icon.ENDOR, Icon.DEATH_STAR_II), Filters.system)));
        modifiers.add(new MayNotHavePowerReducedModifier(self, Filters.Home_One, self.getOwner()));
        modifiers.add(new MayNotHavePowerReducedModifier(self, Filters.Home_One, game.getOpponent(self.getOwner())));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.LAUNCHING_THE_ASSAULT_V__DOWNLOAD_HOME_ONE_OR_SULLUST;
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Persona.HOME_ONE)
                || GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Sullust)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a card from Reserve Deck");
            action.setActionMsg("Deploy Home One or Sullust from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Home_One, Filters.Sullust_system), true));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(playerId);
        Filter ds2Falcon = Filters.and(Icon.DEATH_STAR_II, Filters.Falcon);
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.LAUNCHING_THE_ASSAULT_V__REACT_TO_FALCON;
        if ((TriggerConditions.battleInitiatedAt(game, effectResult, opponent, Filters.sameSystemAs(self, ds2Falcon))
                || TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent, Filters.sameSystemAs(self, ds2Falcon)))
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            Filter homeOneFilter = Filters.and(Filters.Home_One, Filters.canMoveAsReactAsActionFromOtherCard(self, false, 0 , false));
            if (GameConditions.canTarget(game, self, homeOneFilter)) {
                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Move Home One as 'react'");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerGameEffect(action));
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose Home One", homeOneFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard homeOne) {
                                action.addAnimationGroup(homeOne);
                                // Allow response(s)
                                action.allowResponses("Move " + GameUtils.getCardLink(homeOne) + " as a 'react'",
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new MoveAsReactEffect(action, homeOne, false));
                                            }
                                        }
                                );
                            }
                        }
                );
                actions.add(action);
            }
        }

        if ((TriggerConditions.battleInitiatedAt(game, effectResult, opponent, Filters.sameSystemAs(self, Filters.Home_One))
                || TriggerConditions.forceDrainInitiatedBy(game, effectResult, opponent, Filters.sameSystemAs(self, Filters.Home_One)))
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            Filter ds2FalconFilter = Filters.and(ds2Falcon, Filters.canMoveAsReactAsActionFromOtherCard(self, false, 0 , false));
            if (GameConditions.canTarget(game, self, ds2FalconFilter)) {
                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
                action.setText("Move Falcon as 'react'");
                // Update usage limit(s)
                action.appendUsage(
                        new OncePerGameEffect(action));
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose Falcon", ds2FalconFilter) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard ds2Falcon) {
                                action.addAnimationGroup(ds2Falcon);
                                // Allow response(s)
                                action.allowResponses("Move " + GameUtils.getCardLink(ds2Falcon) + " as a 'react'",
                                        new UnrespondableEffect(action) {
                                            @Override
                                            protected void performActionResults(Action targetingAction) {
                                                // Perform result(s)
                                                action.appendEffect(
                                                        new MoveAsReactEffect(action, ds2Falcon, false));
                                            }
                                        }
                                );
                            }
                        }
                );
                actions.add(action);
            }
        }

        return actions;
    }
}
