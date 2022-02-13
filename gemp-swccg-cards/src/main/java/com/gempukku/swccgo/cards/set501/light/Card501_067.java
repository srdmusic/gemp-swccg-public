package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedInterrupt;
import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ModifyTotalPowerUntilEndOfBattleEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.SubstituteDestinyEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: My Sister Has It
 */
public class Card501_067 extends AbstractUsedOrLostInterrupt {
    public Card501_067() {
        super(Side.LIGHT, 5, "My Sister Has It", Uniqueness.UNIQUE);
        setGameText("USED: If your [Skywalker] Epic Event on table, take [Set 14] Chief Chirpa's Hut or [Cloud City] Leia into hand from Reserve Deck; reshuffle. LOST: If you are about to draw a card for battle destiny, may instead use Leia's ability number.");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_18);
        setTestingText("My Sister Has It");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.MY_SISTER_HAS_IT__UPLOAD_CARD;

        if (GameConditions.canSpot(game, self, Filters.and(Filters.your(self), Icon.SKYWALKER, Filters.Epic_Event))
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
            action.setText("Take card into hand from Reserve Deck");

            // Allow response(s)
            action.allowResponses("Take [Set 14] Chief Chirpa's Hut or [Cloud City] Leia into hand from Reserve Deck",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.and(Icon.VIRTUAL_SET_14, Filters.Chief_Chirpas_Hut), Filters.and(Icon.CLOUD_CITY, Filters.Leia)), true));
                        }
                    }
            );
            actions.add(action);
        }


        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        final Filter yourCharacterInBattle = Filters.and(Filters.your(self), Filters.character, Filters.participatingInBattle, Filters.Leia);

        // Check condition(s)
        if (TriggerConditions.isAboutToDrawBattleDestiny(game, effectResult, playerId)
                && GameConditions.canSubstituteDestiny(game)
                && GameConditions.canSpot(game, self, yourCharacterInBattle)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Substitute destiny");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose Leia", yourCharacterInBattle) {
                        @Override
                        protected boolean getUseShortcut() {
                            return true;
                        }

                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard character) {
                            action.addAnimationGroup(character);
                            final float ability = game.getModifiersQuerying().getAbility(game.getGameState(), character);
                            // Allow response(s)
                            action.allowResponses("Substitute " + GameUtils.getCardLink(character) + "'s ability value of " + GuiUtils.formatAsString(ability) + " for battle destiny",
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            float finalAbility = game.getModifiersQuerying().getAbility(game.getGameState(), action.getPrimaryTargetCard(targetGroupId));
                                            // Perform result(s)
                                            action.appendEffect(
                                                    new SubstituteDestinyEffect(action, finalAbility));
                                        }
                                    }
                            );
                        }
                    }


            );
            return Collections.singletonList(action);
        }
        return null;
    }
}