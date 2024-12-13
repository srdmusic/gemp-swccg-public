package com.gempukku.swccgo.cards.set501.dark;

import java.util.Collections;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.TargetingReason;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.CaptureCharacterOnTableEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.RestoreCardToNormalEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToForfeitCardFromTableResult;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: All Wrapped Up (V)
 */
public class Card501_066 extends AbstractNormalEffect {
    public Card501_066() {
        super(Side.DARK, 2, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.All_Wrapped_Up, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("A capture cable is a quick and effective way for bounty hunters to suddenly snare their target.");
        setGameText("Deploy on table. Unless Court Of The Vile Gangster on table, [Dagobah] and [Cloud City] bounty hunters are forfeit +2. May [download] [Jabba's Palace] Ord Mantell. If opponent's character is about to be forfeited, your bounty hunter present may capture that character (character is first restored to normal). [Immune to Alter.]");
        addIcons(Icon.JABBAS_PALACE, Icon.VIRTUAL_SET_24);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("All Wrapped Up (V)");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        TargetingReason targetingReason = TargetingReason.TO_BE_CAPTURED;
        Filter opponentsCharacterFilter = Filters.and(Filters.opponents(self), Filters.character, Filters.canBeTargetedBy(self, targetingReason),
                Filters.at(Filters.wherePresent(self, Filters.and(Filters.your(self), Filters.bounty_hunter))));

        // Check condition(s)
        if (TriggerConditions.isAboutToBeForfeited(game, effectResult, opponentsCharacterFilter)) {
            final AboutToForfeitCardFromTableResult result = (AboutToForfeitCardFromTableResult) effectResult;
            final PhysicalCard cardToBeForfeited = result.getCardToBeForfeited();

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Capture " + GameUtils.getFullName(cardToBeForfeited));
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Choose character", targetingReason, cardToBeForfeited) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Capture " + GameUtils.getCardLink(targetedCard),
                                    new RespondableEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            result.getForfeitCardEffect().preventEffectOnCard(finalTarget);
                                            action.appendEffect(
                                                    new RestoreCardToNormalEffect(action, finalTarget));
                                            action.appendEffect(
                                                    new CaptureCharacterOnTableEffect(action, finalTarget));
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
