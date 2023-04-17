package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractLostOrStartingInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.ExpansionSet;
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
import com.gempukku.swccgo.logic.effects.PutCardFromVoidInLostPileEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeOneCardIntoHandFromOffTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.RetrieveForceResult;

import java.util.Collections;
import java.util.List;

/**
 * Set: Set 21
 * Type: Interrupt
 * Subtype: Lost or Starting
 * Title: ComScan Detection (V)
 */
public class Card501_136 extends AbstractLostOrStartingInterrupt {
    public Card501_136() {
        super(Side.DARK, 4, "ComScan Detection", Uniqueness.UNRESTRICTED, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("The Imperial Nacy boasts the best communications network in the galaxy. Sophisticated control technology allows the Empire to dispatch armed forces without delay.");
        setGameText("LOST: Take a just retrieved ISB Agent into hand. STARTING: If ISB Operations and Coruscant system on table, deploy ISB Central Headquarters and 3 Effects that deploy for free and are always [Immune to Alter.]. Place Interrupt in Lost Pile.");
        addIcons(Icon.HOTH, Icon.VIRTUAL_SET_21);
        setTestingText("ComScan Detection (V)");
    }


    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self) {
        // Check condition(s)
        if (TriggerConditions.justRetrievedForce(game, effectResult, playerId)) {
            final PhysicalCard retrievedCard = ((RetrieveForceResult)effectResult).getMostRecentCardRetrieved();
            if (Filters.ISB_agent.accepts(game, retrievedCard)) {
                final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
                action.setText("Take retrieved ISB Agent into hand");
                // Allow response(s)
                action.allowResponses(
                        new RespondablePlayCardEffect(action) {
                            @Override
                            protected void performActionResults(Action targetingAction) {
                                // Perform result(s)
                                action.appendEffect(new TakeOneCardIntoHandFromOffTableEffect(action, playerId, retrievedCard, "Take "+ GameUtils.getCardLink(retrievedCard)+" into hand") {
                                    protected void afterCardTakenIntoHand() {}
                                });
                            }
                        }
                );
                return Collections.singletonList(action);
            }
        }

        return null;
    }

    @Override
    protected PlayInterruptAction getGameTextStartingAction(final String playerId, final SwccgGame game, final PhysicalCard self) {
        if (GameConditions.canTarget(game, self, Filters.ISB_Operations)
                && GameConditions.canTarget(game, self, Filters.Coruscant_system)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.STARTING);
            action.setText("Deploy ISB Central Headquarters and Effects from Reserve Deck");
            // Allow response(s)
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, Filters.title("Coruscant: ISB Central Headquarters"), true, false));
                            action.appendEffect(
                                    new DeployCardsFromReserveDeckEffect(action, Filters.and(Filters.Effect, Filters.deploysForFree, Filters.always_immune_to_Alter), 3, 3, true, false));
                            action.appendEffect(
                                    new PutCardFromVoidInLostPileEffect(action, playerId, self));
                        }
                    }
            );
            return action;
        }
        return null;
    }
}