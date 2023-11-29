package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractCapitalStarship;
import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.MovingResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Starship
 * Subtype: Capital
 * Title: Fulminatrix
 */
public class Card501_112 extends AbstractCapitalStarship {
    public Card501_112() {
        super(Side.DARK, 3, 9, 10, 8, null, 3, 9, Title.Fulminatrix, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("May add 5 pilots, 4 passengers, 3 TIEs, and 2 vehicles. Permanent pilot provides ability of 2. Deploy -3 if Tracked Fleet on table. Opponent must lose 1 force to move a starship from here.");
        addIcons(Icon.NAV_COMPUTER, Icon.EPISODE_VII, Icon.SCOMP_LINK, Icon.FIRST_ORDER, Icon.VIRTUAL_SET_23);
        addIcon(Icon.PILOT, 1);
        addModelType(ModelType.MANDATOR_IV_CLASS_DREADNAUGHT);
        setPilotCapacity(5);
        setPassengerCapacity(4);
        setStarfighterCapacity(3, Filters.First_Order_TIE);
        setVehicleCapacity(2);
        setTestingText(Title.Fulminatrix);
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<>();
        permanentsAboard.add(new AbstractPermanentPilot(2) {});
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextAlwaysOnModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostModifier(self, new OnTableCondition(self, Filters.Tracked_Fleet), -3));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.movingFromLocation(game, effectResult, Filters.and(Filters.opponents(self), Filters.starship), Filters.here(self))) {
            String opponent = game.getOpponent(self.getOwner());
            final PhysicalCard starship = ((MovingResult) effectResult).getCardMoving();

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Lose 1 Force to move " + GameUtils.getFullName(starship));
            action.setActionMsg("Make " + opponent + " lose 1 Force to move " + GameUtils.getCardLink(starship));
            // Build action using common utility
            action.appendEffect(new LoseForceEffect(action, opponent, 1));
            return Collections.singletonList(action);
        }
        return null;
    }
}