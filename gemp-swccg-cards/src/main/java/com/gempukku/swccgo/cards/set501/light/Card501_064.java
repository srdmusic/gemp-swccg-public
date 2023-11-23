package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.ResetDestinyEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeStackedCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotBeFiredModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Luke's Acceptance
 */
public class Card501_064 extends AbstractUsedOrLostInterrupt {
    public Card501_064() {
        super(Side.LIGHT, 5, "Luke's Acceptance", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("USED: Once per game, if battle was just initiated where Luke is alone, weapons may not be fired." +
                "LOST: Once per game, if you have two cards stacked on I Feel The Conflict, " +
                "make a just drawn destiny = 2 or take a card into hand from Insignificant Rebellion.");
        addIcon(Icon.VIRTUAL_SET_24);
        setTestingText("Luke's Acceptance");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        GameTextActionId gameTextActionId = GameTextActionId.LUKES_ACCEPTANCE__RESET_DESTINY_OR_TAKE_CARD_INTO_HAND;

        int numCardsStacked = GameConditions.canSpot(game, self, Filters.I_Feel_The_Conflict) ? Filters.filterStacked(game, Filters.stackedOn(self, Filters.I_Feel_The_Conflict)).size() : 0;

        if(GameConditions.isOncePerGame(game, self, gameTextActionId)
                && numCardsStacked >= 2
                && Filters.canSpot(game, self, Filters.and(Filters.Insignificant_Rebellion, Filters.hasStacked(Filters.any)))){
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
            action.setText("Take stacked card into hand");
            action.appendUsage(
                    new OncePerGameEffect(action)
            );
            action.appendTargeting(
                    new ChooseStackedCardEffect(action, playerId, Filters.findFirstActive(game, self, Filters.Insignificant_Rebellion), Filters.any) {
                        @Override
                        protected void cardSelected(PhysicalCard selectedCard) {
                            action.allowResponses(
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new TakeStackedCardIntoHandEffect(action,playerId, Filters.Insignificant_Rebellion)
                                            );
                                        }
                                    }
                            );
                        }
                    }
            );

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        GameTextActionId gameTextActionId = GameTextActionId.LUKES_ACCEPTANCE__PREVENT_WEAPONS_FROM_BEING_FIRED;

        if(GameConditions.isOncePerGame(game, self, gameTextActionId)
            && TriggerConditions.battleInitiated(game, effectResult)
            && GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.Luke, Filters.alone))){
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Prevent all weapons from being fired this battle");
            action.appendUsage(
                    new OncePerGameEffect(action)
            );
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddUntilEndOfBattleModifierEffect(action,
                                            new MayNotBeFiredModifier(self, Filters.any), "Prevents all weapons from being fired"));
                        }
                    }
            );
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.LUKES_ACCEPTANCE__RESET_DESTINY_OR_TAKE_CARD_INTO_HAND;

        int numCardsStacked = GameConditions.canSpot(game, self, Filters.I_Feel_The_Conflict) ? Filters.filterStacked(game, Filters.stackedOn(self, Filters.I_Feel_The_Conflict)).size() : 0;

        if(GameConditions.isOncePerGame(game, self, gameTextActionId)
            && numCardsStacked >= 2
            && TriggerConditions.isDestinyJustDrawn(game, effectResult)){
            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.LOST);
            action.setText("Make a just drawn destiny = 2");
            action.appendUsage(
                    new OncePerGameEffect(action)
            );
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new ResetDestinyEffect(action, 2)
                            );
                        }
                    }
            );
            actions.add(action);
        }

        return actions;
    }
}
