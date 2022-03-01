package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardsOnTableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;
import java.util.ArrayList;


/**
 * Set: Set 18
 * Type: Starship
 * Subtype: Capital
 * Title: Tyrant (V)
 */
public class Card501_025 extends AbstractCapitalStarship {
    public Card501_025() {
        super(Side.DARK, 1, 7, 8, 6, null, 3, 9, Title.Tyrant, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("Assigned to Admiral Ozzel's Death Squadron. Attempted to capture Rebel starships fleeing the Hoth system.");
        setGameText("May add 6 pilots, 8 passengers, 2 vehicles, and 4 TIEs. Permanent pilot provides ability of 2. During your move phase, AT-ATs aboard may relocate to a related planet site (if able). Immune to attrition < 4 at Hoth.");
        addIcons(Icon.HOTH, Icon.PILOT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_18);
        addModelType(ModelType.IMPERIAL_CLASS_STAR_DESTROYER);
        addKeywords(Keyword.DEATH_SQUADRON);
        setPilotCapacity(6);
        setPassengerCapacity(8);
        setVehicleCapacity(2);
        setTIECapacity(4);
        setTestingText("Tyrant (V)");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        return Collections.singletonList(new AbstractPermanentPilot(2) {});
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, Filters.at(Filters.Hoth_system), 3));

        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.canSpot(game, self, Filters.and(Filters.AT_AT, Filters.aboard(self), Filters.canBeRelocatedToLocation(Filters.relatedSite(self), 0)))
            && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId)
            && GameConditions.canSpot(game, self, Filters.relatedSite(self))) {
            
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Relocate AT-AT(s) to related site");
            action.setActionMsg("Relocate AT-AT(s) aboard to a related site");

            action.appendUsage(
                    new OncePerTurnEffect(action));

            action.appendTargeting(new TargetCardsOnTableEffect(action, playerId, "Target AT-ATs to be relocated", 1, Integer.MAX_VALUE, Filters.and(Filters.AT_AT, Filters.aboard(self), Filters.canBeRelocatedToLocation(Filters.relatedSite(self), 0))) {
                @Override
                protected void cardsTargeted(final int targetGroupId1, Collection<PhysicalCard> targetedCards) {

                    List<Filter> relocatedFilters = new ArrayList<Filter>();
                    for (PhysicalCard card : targetedCards) {
                        Filter cardCanRelocate = Filters.locationCanBeRelocatedTo(card, 0);
                        relocatedFilters.add(cardCanRelocate);
                    }

                    action.appendTargeting(
                        new TargetCardOnTableEffect(action, playerId, "Target site", Filters.and(Filters.relatedSite(self), Filters.and(relocatedFilters.toArray(new Filter[0])))) {
                            @Override
                            protected void cardTargeted(final int targetGroupId2, PhysicalCard targetedCard) {
                                action.allowResponses(new RespondableEffect(action) {
                                    @Override
                                    protected void performActionResults(Action targetingAction) {
                                        PhysicalCard atats = action.getPrimaryTargetCard(targetGroupId1);
                                        PhysicalCard site = action.getPrimaryTargetCard(targetGroupId2);

                                        action.appendEffect(
                                                new RelocateBetweenLocationsEffect(action, atats, site));
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
