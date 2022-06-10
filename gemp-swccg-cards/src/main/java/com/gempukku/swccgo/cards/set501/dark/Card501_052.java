package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractPermanentAboard;
import com.gempukku.swccgo.cards.AbstractPermanentPilot;
import com.gempukku.swccgo.cards.AbstractStarfighter;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddDestinyToAttritionEffect;
import com.gempukku.swccgo.cards.effects.AddDestinyToTotalPowerEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 19
 * Type: Starship
 * Subtype: Starfighter
 * Title: Jango Fett & Boba Fett In Slave I (V)
 */
public class Card501_052 extends AbstractStarfighter {
    public Card501_052() {
        super(Side.DARK, 1, 7, 7, null, 6, 4, 8, "Jango Fett & Boba Fett In Slave I", Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setComboCard(true);
        setGameText("May add 2 passengers. Permanent pilots are •Jango and •Boba, who provide ability of 6. While in battle, may lose 2 Force to add one destiny to total attrition. Immune to Eject, Eject! and attrition < 5.");
        addPersonas(Persona.SLAVE_I);
        setPassengerCapacity(2);
        addIcons(Icon.TRADE_FEDERATION, Icon.EPISODE_I, Icon.INDEPENDENT, Icon.NAV_COMPUTER, Icon.SCOMP_LINK, Icon.VIRTUAL_SET_19);
        addIcon(Icon.PILOT, 2);
        addModelType(ModelType.FIRESPRAY_CLASS_ATTACK_SHIP);
        setTestingText("Jango Fett & Boba Fett In Slave I");
    }

    @Override
    protected List<? extends AbstractPermanentAboard> getGameTextPermanentsAboard() {
        List<AbstractPermanentAboard> permanentsAboard = new ArrayList<AbstractPermanentAboard>();
        permanentsAboard.add(new AbstractPermanentPilot(Persona.JANGO_FETT, 3) {
        });
        permanentsAboard.add(new AbstractPermanentPilot(Persona.BOBA_FETT, 3) {
        });
        return permanentsAboard;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToTitleModifier(self, Title.Eject_Eject));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.isInBattle(game, self)
                && GameConditions.canAddDestinyDrawsToAttrition(game, playerId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Add one destiny to attrition");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));

            action.appendCost(
                    new LoseForceEffect(action, playerId, 2, true));
            // Perform result(s)
            action.appendEffect(
                    new AddDestinyToAttritionEffect(action, 1));
            actions.add(action);

        }
        return actions;
    }
}
