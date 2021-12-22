package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.List;


/**
 * Set: Set 17
 * Type: Starship
 * Subtype: Capital
 * Title: Tyrant (V)
 */
public class Card501_025 extends AbstractCapitalStarship {
    public Card501_025() {
        super(Side.DARK, 1, 7, 8, 6, null, 3, 9, Title.Tyrant, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Assigned to Admiral Ozzel's Death Squadron. Attempted to capture Rebel starships fleeing the Hoth system.");
        setGameText("May add 6 pilots, 8 passengers, 2 vehicles, and 4 TIEs. Permanent pilot provides ability of 2. If Tyrant just moved to a system, may relocate an AT-AT aboard to a related site.");
        addIcons(Icon.HOTH, Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_17);
        addModelType(ModelType.IMPERIAL_CLASS_STAR_DESTROYER);
        addKeywords(Keyword.DEATH_SQUADRON);
        setPilotCapacity(6);
        setPassengerCapacity(8);
        setVehicleCapacity(2);
        setTIECapacity(4);
        setTestingText("[Set 18] Tyrant (V)");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (TriggerConditions.moved(game, effectResult, self)
                && GameConditions.canSpot(game, self, Filters.and(Filters.AT_AT, Filters.aboard(self), Filters.canBeRelocatedToLocation(Filters.relatedSite(self), 0)))
                && GameConditions.canSpot(game, self, Filters.relatedSite(self))) {
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Relocate AT-AT to related site");
            action.setActionMsg("Relocate an AT-AT aboard to a related site");
            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target AT-AT to be relocated", Filters.and(Filters.AT_AT, Filters.aboard(self), Filters.canBeRelocatedToLocation(Filters.relatedSite(self), 0))) {
                @Override
                protected void cardTargeted(final int targetGroupId1, PhysicalCard targetedCard) {
                    action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Target site", Filters.and(Filters.relatedSite(self), Filters.locationCanBeRelocatedTo(targetedCard, 0))) {
                        @Override
                        protected void cardTargeted(final int targetGroupId2, PhysicalCard targetedCard) {
                            action.allowResponses(new RespondableEffect(action) {
                                @Override
                                protected void performActionResults(Action targetingAction) {
                                    PhysicalCard atat = action.getPrimaryTargetCard(targetGroupId1);
                                    PhysicalCard site = action.getPrimaryTargetCard(targetGroupId2);

                                    action.appendEffect(
                                            new RelocateBetweenLocationsEffect(action, atat, site));
                                }
                            });
                        }
                    });
                }
            });

            return Collections.singletonList(action);
        }

        return null;
    }
}
