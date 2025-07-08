package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AttachCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Supremacy
 */
public class Card501_113 extends AbstractCapitalStarship {
    public Card501_113() {
        super(Side.DARK, 1, 16, 13, 9, null, 2, 16, Title.Supremacy, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setAsHorizontal(true);
        setGameText("May add unlimited pilots, passengers, starfighters, and vehicles. Permanent pilot provides ability of 4. Once per game, during your move phase, may relocate Tracked Fleet here. Immune to attrition < 10.");
        addPersona(Persona.SUPREMACY);
        addIcons(Icon.SCOMP_LINK, Icon.EPISODE_VII, Icon.FIRST_ORDER, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_25);
        addIcon(Icon.PILOT, 1);
        addModelType(ModelType.MEGA_CLASS_DREADNAUGHT);
        setPilotCapacity(Integer.MAX_VALUE);
        setPassengerCapacity(Integer.MAX_VALUE);
        setStarfighterCapacity(Integer.MAX_VALUE);
        setVehicleCapacity(Integer.MAX_VALUE);
        setMatchingPilotFilter(Filters.Hux);
        setTestingText(Title.Supremacy);
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(4) {});
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, self, 10));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        Filter relocatableTrackedFleet = Filters.and(Filters.Tracked_Fleet, Filters.not(Filters.here(self)));
        GameTextActionId gameTextActionId = GameTextActionId.SUPREMACY__RELOCATE_TRACKED_FLEET;
        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.isDuringYourPhase(game, self, Phase.MOVE)
                && GameConditions.canSpot(game, self, relocatableTrackedFleet)) {

            PhysicalCard trackedFleet = Filters.findFirstActive(game, self, relocatableTrackedFleet);
            PhysicalCard thisSystem = Filters.findFirstActive(game, self, Filters.sameSystem(self));
            // Build action using common utility)
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Relocate Tracked Fleet here");
            // Append usage limit(s)
            action.appendUsage(
                new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new AttachCardFromTableEffect(action, trackedFleet, thisSystem));
            return Collections.singletonList(action);
        }
        return null;
    }
}
