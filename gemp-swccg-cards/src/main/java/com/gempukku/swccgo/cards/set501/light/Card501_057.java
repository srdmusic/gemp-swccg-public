package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.FiredWeaponsInBattleCondition;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.common.Zone;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.decisions.YesNoDecision;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.DeployCardsSimultaneouslyEffect;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardCombinationFromHandAndOrReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromHandEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotBeFiredModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.StandardEffect;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 22
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: I’m Ready For Anything
 */
public class Card501_057 extends AbstractUsedOrLostInterrupt {
    public Card501_057() {
        super(Side.LIGHT, 5, "I’m Ready For Anything", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("For remainder of turn, players may only fire one weapon at Cantina and [Ref2] sites. " +
                "OR If Mara in battle add one to a just drawn destiny (two if at a [Ref2] site). " +
                "LOST: Deploy Mara (with Anakin’s Lightsaber on her) from hand and/or Reserve Deck (reshuffle).");
        addIcons(Icon.VIRTUAL_SET_22);
        setTestingText("I’m Ready For Anything");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawnBy(game, effectResult, opponent)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Mara_Jade)) {

            final int modifierAmount = GameConditions.isDuringBattleAt(game, Filters.and(Filters.icon(Icon.REFLECTIONS_II), Filters.site)) ? 2 : 1;

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.USED);
            action.setText("Add " + modifierAmount + " to destiny");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ModifyDestinyEffect(action, modifierAmount));
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        final String opponent = game.getOpponent(playerId);

        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        final PlayInterruptAction action1 = new PlayInterruptAction(game, self, CardSubtype.USED);
        action1.setText("Affect weapons");
        // Allow response(s)
        action1.allowResponses("Prevent players from firing more than one weapon at Cantina or [Ref2] sites for remainder of turn",
                new RespondablePlayCardEffect(action1) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        Filter siteFilter = Filters.or(Filters.Cantina, Filters.and(Filters.site, Filters.icon(Icon.REFLECTIONS_II)));
                        // Perform result(s)
                        action1.appendEffect(
                                new AddUntilEndOfTurnModifierEffect(action1,
                                        new MayNotBeFiredModifier(self, Filters.and(Filters.your(playerId), Filters.at(siteFilter)),
                                                new FiredWeaponsInBattleCondition(playerId, 1, Filters.any)),
                                        "You may not fire more than one weapon"));
                        action1.appendEffect(
                                new AddUntilEndOfTurnModifierEffect(action1,
                                        new MayNotBeFiredModifier(self, Filters.and(Filters.opponents(playerId), Filters.at(siteFilter)),
                                                new FiredWeaponsInBattleCondition(opponent, 1, Filters.any)),
                                        "Opponent may not fire more than one weapon"));
                    }
                }
        );
        actions.add(action1);

        final PlayInterruptAction action2 = new PlayInterruptAction(game, self, CardSubtype.LOST);
        action2.setText("Deploy Mara (with Anakin’s Lightsaber on her)");
        // Allow response(s)
        action2.allowResponses("Deploy Mara (with Anakin’s Lightsaber on her)",
                new RespondablePlayCardEffect(action2) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        final Collection<PhysicalCard> cardsInHand = game.getGameState().getHand(playerId);
                        final Collection<PhysicalCard> maraInHand = Filters.filter(cardsInHand, game, Filters.Mara_Jade);
                        final Collection<PhysicalCard> anakinsLightsaberInHand = Filters.filter(cardsInHand, game, Filters.title(Title.Anakins_Lightsaber));

                        if (!maraInHand.isEmpty() && !anakinsLightsaberInHand.isEmpty()) {
                            action2.appendTargeting(
                                    new PlayoutDecisionEffect(action2, playerId,
                                            new YesNoDecision("You have both Mara Jade and Anakin's Lightsaber in hand. Do you want to search Reserve Deck as well?") {
                                                @Override
                                                protected void yes() {
                                                    action2.setActionMsg("Choose Mara Jade and Anakin's Lightsaber from hand and/or Reserve Deck");
                                                    // Perform result(s)
                                                    action2.appendEffect(getChooseCardsEffect(action2));
                                                }

                                                @Override
                                                protected void no() {
                                                    action2.setActionMsg("Deploy Mara Jade and Anakin's Lightsaber from hand");
                                                    action2.appendTargeting(
                                                            new ChooseCardFromHandEffect(action2, playerId, Filters.Mara_Jade) {
                                                                @Override
                                                                public String getChoiceText(int numCardsToChoose) {
                                                                    return "Choose Mara Jade";
                                                                }

                                                                @Override
                                                                protected void cardSelected(SwccgGame game, final PhysicalCard maraJade) {
                                                                    action2.appendTargeting(
                                                                            new ChooseCardFromHandEffect(action2, playerId, Filters.title(Title.Anakins_Lightsaber)) {
                                                                                @Override
                                                                                public String getChoiceText(int numCardsToChoose) {
                                                                                    return "Choose Anakin's Lightsaber";
                                                                                }

                                                                                @Override
                                                                                protected void cardSelected(SwccgGame game, PhysicalCard anakinsLightsaber) {
                                                                                    action2.appendEffect(
                                                                                            new DeployCardsSimultaneouslyEffect(action2, maraJade, false, 0, anakinsLightsaber, false, 0)
                                                                                    );
                                                                                }
                                                                            }
                                                                    );
                                                                }
                                                            }
                                                    );
                                                }
                                            }
                                    )
                            );
                        } else {
                            action2.setActionMsg("Deploy Mara Jade and Anakin's Lightsaber from hand and/or Reserve Deck");
                            // Perform result(s)
                            action2.appendEffect(getChooseCardsEffect(action2));
                        }
                    }
                }
        );
        actions.add(action2);

        return actions;
    }

    private StandardEffect getChooseCardsEffect(final Action action) {
        return new ChooseCardCombinationFromHandAndOrReserveDeckEffect(action) {
            @Override
            public String getChoiceText(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                return "Choose Mara Jade and Anakin's Lightsaber from hand and/or Reserve Deck";
            }

            @Override
            public Filter getValidToSelectFilter(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                String playerId = action.getPerformingPlayer();
                GameState gameState = game.getGameState();
                Collection<PhysicalCard> cardsToChooseFrom = new LinkedList<PhysicalCard>(gameState.getHand(playerId));
                cardsToChooseFrom.addAll(gameState.getCardPile(playerId, Zone.RESERVE_DECK));

                if (cardsSelected.isEmpty()) {
                    Collection<PhysicalCard> maraJades = Filters.filter(cardsToChooseFrom, game, Filters.Mara_Jade);
                    return Filters.in(maraJades);
                } else if (cardsSelected.size() == 1) {
                    PhysicalCard maraJade = cardsSelected.iterator().next();
                    return Filters.and(Filters.title(Title.Anakins_Lightsaber), Filters.deployableSimultaneouslyWith(action.getActionSource(), maraJade, false, 0, false, 0));
                }
                return Filters.none;
            }

            @Override
            public boolean isSelectionValid(SwccgGame game, Collection<PhysicalCard> cardsSelected) {
                return (cardsSelected.size() == 2);
            }

            @Override
            protected void cardsChosen(List<PhysicalCard> cardsChosen) {
                PhysicalCard maraJade = cardsChosen.get(0);
                PhysicalCard anakinsLightsaber = cardsChosen.get(1);

                // Perform result(s)
                action.appendEffect(
                        new DeployCardsSimultaneouslyEffect(action, maraJade, false, 0, anakinsLightsaber, false, 0));
            }
        };
    }
}