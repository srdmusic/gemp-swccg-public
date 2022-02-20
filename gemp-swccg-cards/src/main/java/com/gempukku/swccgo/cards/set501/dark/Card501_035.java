package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractSite;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.RetrieveCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToLocationFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeOneCardIntoHandFromOffTableEffect;
import com.gempukku.swccgo.logic.modifiers.DockingBayTransitFromCostModifier;
import com.gempukku.swccgo.logic.modifiers.DockingBayTransitToCostModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployOtherCardsAsReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Location
 * Subtype: Site
 * Title: Lothal: Imperial Complex
 */
public class Card501_035 extends AbstractSite {
    public Card501_035() {
        super(Side.DARK, "Lothal: Imperial Complex", Title.Lothal);
        setLocationDarkSideGameText("While you control with a leader, once per battle with an Imperial, may deploy a card as a 'react.'");
        setLocationLightSideGameText("If you just Force drained here, may take a card stacked on Thrawn's Art Collection into hand.");
        addIcon(Icon.DARK_FORCE, 2);
        addIcon(Icon.LIGHT_FORCE, 0);
        addIcons(Icon.INTERIOR_SITE, Icon.PLANET, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_18);
        setTestingText("Lothal: Imperial Complex");
    }

    @Override
    protected List<Modifier> getGameTextDarkSideWhileActiveModifiers(String playerOnDarkSideOfLocation, SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();

        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextLightSideOptionalAfterTriggers(String playerOnLightSideOfLocation, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.forceDrainCompleted(game, effectResult, playerOnLightSideOfLocation, Filters.here(self))
            && GameConditions.canSpot(game, self, Filters.and(Filters.Thrawns_Art_Collection, Filters.hasStacked(Filters.any)))) {

            Collection<PhysicalCard> stackedCards = Filters.filterStacked(game, Filters.stackedOn(self, Filters.Thrawns_Art_Collection));

            if (!stackedCards.isEmpty()) {
                List<PhysicalCard> list = new LinkedList<>(stackedCards);
                Collections.shuffle(list);
                PhysicalCard randomlySelected = list.get(0);

                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerOnLightSideOfLocation, gameTextSourceCardId, gameTextActionId);
                action.setText("Take an 'artwork' card into hand");
                action.setActionMsg("Randomly select an 'artwork' card to take into hand");
                // Perform result(s)
                action.appendEffect(
                        new TakeOneCardIntoHandFromOffTableEffect(action, playerOnLightSideOfLocation, randomlySelected, "Take an 'artwork' card into hand") {
                            @Override
                            protected void afterCardTakenIntoHand() {

                            }
                        });
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
