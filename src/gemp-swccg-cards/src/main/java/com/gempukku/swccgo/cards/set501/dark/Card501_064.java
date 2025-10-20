package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
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
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.SubstituteDestinyEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.GuiUtils;

import java.util.Collections;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Interrupt
 * Subtype: Used Or Lost
 * Title: Welcome Home, Lord Tyranus
 */
public class Card501_064 extends AbstractUsedOrLostInterrupt {
    public Card501_064() {
        super(Side.DARK, 4, "Welcome Home, Lord Tyranus", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("");
        setGameText("USED: If Dooku is your apprentice, [upload] The Works or Petranaki Arena. LOST: Once per game, if Darth Tyranus in battle at a site and you are about to draw a card for battle destiny, instead use his ability number.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Welcome Home, Lord Tyranus");
    }
    
    private boolean isDookuYourApprentice(SwccgGame game, PhysicalCard self) {
        PhysicalCard rots = Filters.findFirstActive(game, self, Filters.Revenge_Of_The_Sith);
        if (rots != null
                && GameConditions.cardHasWhileInPlayDataSet(rots)
                && rots.getWhileInPlayData().getTextValue() != null) {
            return "Dooku".equals(rots.getWhileInPlayData().getTextValue());
        }
        return false;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {

        if (isDookuYourApprentice(game, self)) {
            GameTextActionId gameTextActionId = GameTextActionId.WELCOME_HOME_LORD_TYRANUS__UPLOAD_SITE;

            // Check condition(s)
            if (GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, gameTextActionId, CardSubtype.USED);
                action.setText("Take location into hand from Reserve Deck");
                action.setActionMsg("Take Coruscant: The Works or Geonosis: Petranaki Arena into hand from Reserve Deck.");
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(
                                        new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.or(Filters.Coruscant_The_Works, Filters.Geonosis_Petranaki_Arena), true));
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }
        return null;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, final SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.isAboutToDrawBattleDestiny(game, effectResult, playerId)
                && GameConditions.canSubstituteDestiny(game)
                && GameConditions.isDuringBattleAt(game, Filters.site)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Darth_Tyranus)) {
            
            PhysicalCard tyranus = Filters.findFirstActive(game, self, Filters.Darth_Tyranus);
            final float abilityNumber = tyranus.getBlueprint().getAbility();
            // Perform result(s)
            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Substitute destiny");
            action.allowResponses("Substitute " + GameUtils.getCardLink(tyranus) + "'s ability number of " + GuiUtils.formatAsString(abilityNumber) + " for battle destiny",
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new SubstituteDestinyEffect(action, abilityNumber));
                        }
                    }
            );
            return Collections.singletonList(action);
        }
        return null;
    }
}
