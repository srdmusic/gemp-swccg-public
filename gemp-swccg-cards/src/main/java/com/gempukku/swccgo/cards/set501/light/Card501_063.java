package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Lost
 * Title: A Jedi's Fury
 */
public class Card501_063 extends AbstractLostInterrupt {
    protected Card501_063() {
        super(Side.LIGHT, 4, "A Jedi's Fury", Uniqueness.UNIQUE);
        setLore("");
        setGameText("If Luke alone in a battle or duel, add 1 to a just drawn weapon, " +
                "battle or duel destiny for each card stacked on I Feel The Conflict (limit 3). " +
                "OR During your move phase, relocate Luke from a site to a battleground site " +
                "(or your site that opponent occupies).");
        addIcon(Icon.VIRTUAL_SET_20);
        setTestingText("A Jedi's Fury");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextOptionalAfterActions(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        Filter lukeAlone = Filters.and(Filters.Luke, Filters.alone);

        final int numCardsStacked = GameConditions.canSpot(game, self, Filters.I_Feel_The_Conflict) ?
                Math.min(3, Filters.filterStacked(game, Filters.stackedOn(self, Filters.I_Feel_The_Conflict)).size()) : 0;

        if((GameConditions.isDuringBattleWithParticipant(game, lukeAlone)
            || GameConditions.isDuringDuelWithParticipant(game, lukeAlone))
            && (TriggerConditions.isBattleDestinyJustDrawn(game, effectResult)
            || TriggerConditions.isDuelDestinyJustDrawn(game, effectResult)
            || TriggerConditions.isWeaponDestinyJustDrawn(game, effectResult))){
            final PlayInterruptAction action = new PlayInterruptAction(game, self);

            action.setText("Add " + numCardsStacked + " to your destiny");
            action.allowResponses(
                    new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            action.appendEffect(
                                    new ModifyDestinyEffect(action, numCardsStacked)
                            );
                        }
                    }
            );

            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, PhysicalCard self) {
        List<PlayInterruptAction> actions = new ArrayList<>();

        // Check condition(s)
        if (GameConditions.isDuringYourPhase(game, self, Phase.MOVE)) {
            PhysicalCard luke = Filters.findFirstActive(game, self,
                    Filters.and(Filters.Luke, Filters.at(Filters.site), Filters.canBeTargetedBy(self)));

            final Filter siteFilter = Filters.or(Filters.battleground_site, Filters.and(Filters.your(playerId), Filters.site, Filters.occupies(game.getOpponent(playerId))));

            if (luke != null
                    && Filters.canBeRelocatedToLocation(siteFilter, false, 0).accepts(game, luke)) {

                final PlayInterruptAction action = new PlayInterruptAction(game, self);
                action.setText("Relocate Luke to a site");
                // Choose target(s)
                action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Choose Luke", Filters.sameCardId(luke)) {
                            @Override
                            protected void cardTargeted(final int targetGroupId, final PhysicalCard luke) {
                                action.addAnimationGroup(luke);
                                Collection<PhysicalCard> otherSites = Filters.filterTopLocationsOnTable(game, Filters.and(siteFilter, Filters.locationCanBeRelocatedTo(luke, false, false, true, 0, false)));
                                action.appendTargeting(
                                        new ChooseCardOnTableEffect(action, playerId, "Choose a site", otherSites) {
                                            @Override
                                            protected void cardSelected(final PhysicalCard location) {
                                                action.addAnimationGroup(location);
                                                // Allow response(s)
                                                action.allowResponses("Relocate " + GameUtils.getCardLink(luke) + " to " + GameUtils.getCardLink(location),
                                                        new RespondablePlayCardEffect(action) {
                                                            @Override
                                                            protected void performActionResults(Action targetingAction) {
                                                                // Perform result(s)
                                                                action.appendEffect(
                                                                        new RelocateBetweenLocationsEffect(action, luke, location));
                                                            }
                                                        }
                                                );
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
