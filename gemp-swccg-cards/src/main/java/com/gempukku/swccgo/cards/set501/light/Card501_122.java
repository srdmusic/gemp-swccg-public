package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.InitiateBattleAction;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ModifyTotalBattleDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.StackActionEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotDeployUsingDejarikRulesModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifierFlag;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.modifiers.SpecialFlagModifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;


/**
 * Set: Endor
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Take The Initiative (V)
 */
public class Card501_122 extends AbstractUsedOrLostInterrupt {
    public Card501_122() {
        super(Side.LIGHT, 3, Title.Take_The_Initiative, Uniqueness.UNIQUE, ExpansionSet.ENDOR, Rarity.C);
        setVirtualSuffix(true);
        setLore("The ability to think and act independently gave the Rebels an advantage over their Imperial foes.");
        setGameText("USED: If all your ability in battle is provided by spies, your total battle destiny is +2. LOST: During opponent’s deploy phase, initiate a battle at a Scarif site where you have no weapons and opponent has more power than you (opponent may not deploy cards this battle).");
        addIcons(Icon.ENDOR, Icon.VIRTUAL_SET_21);
        setTestingText("Take The Initiative (V)");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && GameConditions.isAllAbilityInBattleProvidedBy(game, playerId, Filters.spy)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Add 2 to total battle destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyTotalBattleDestinyEffect(action, playerId, 2));
                        }
                    }
            );
            actions.add(action);
        }



        // Check condition(s)
        if (GameConditions.isDuringOpponentsPhase(game, playerId, Phase.DEPLOY)) {
            final String opponent = game.getOpponent(playerId);

            Filter siteFilter = Filters.and(Filters.Scarif_site, Filters.not(Filters.sameLocationAs(self, Filters.and(Filters.your(self), Filters.weapon))));
            List<PhysicalCard> potentialBattleLocations = new LinkedList<>();
            for(PhysicalCard site:Filters.filterTopLocationsOnTable(game, siteFilter)) {
                float ownPower = game.getModifiersQuerying().getTotalPowerAtLocation(game.getGameState(), site, playerId, false, false);
                float opponentPower = game.getModifiersQuerying().getTotalPowerAtLocation(game.getGameState(), site, opponent, false, false);

                if (ownPower < opponentPower
                        && (GameConditions.canInitiateBattleAtLocation(playerId, game, site, false, true)
                        || GameConditions.canInitiateBattleAtLocation(playerId, game, site, true, true)))
                    potentialBattleLocations.add(site);
            }

            if (!potentialBattleLocations.isEmpty()) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
                action.setText("Initiate battle");

                action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose a site", Filters.in(potentialBattleLocations)) {
                    @Override
                    protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                        action.allowResponses("Initiate battle at " + GameUtils.getCardLink(targetedCard), new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                                action.appendEffect(
                                        new StackActionEffect(action,
                                                new InitiateBattleAction(playerId, finalTarget, false) {
                                                    @Override
                                                    public List<Modifier> getAddedModifiers() {
                                                        List<Modifier> modifiers = new LinkedList<>();
                                                        Filter deploys = new Filter() {
                                                            @Override
                                                            public boolean accepts(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard physicalCard) {
                                                                return physicalCard.getBlueprint().isCardTypeDeployed();
                                                            }
                                                        };
                                                        modifiers.add(new MayNotDeployModifier(self, deploys, opponent));
                                                        modifiers.add(new MayNotDeployUsingDejarikRulesModifier(self, Filters.any, opponent));
                                                        modifiers.add(new SpecialFlagModifier(self, ModifierFlag.MAY_NOT_INITIATE_DEPLOYMENT_ACTIONS_THAT_SEARCH_PILES, opponent));
                                                        return modifiers;
                                                    }
                                                }));
                            }
                        });
                    }
                });
                actions.add(action);
            }
        }

        return actions;
    }
}