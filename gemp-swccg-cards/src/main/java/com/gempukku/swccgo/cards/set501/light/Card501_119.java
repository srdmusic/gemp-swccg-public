package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractUsedOrLostInterrupt;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.CardSubtype;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.actions.PlayInterruptAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.RespondablePlayCardEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.DrawsBattleDestinyIfUnableToOtherwiseModifier;
import com.gempukku.swccgo.logic.modifiers.EachWeaponDestinyModifier;
import com.gempukku.swccgo.logic.modifiers.ExtraForceCostToFireWeaponModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionModifier;
import com.gempukku.swccgo.logic.timing.Action;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 20
 * Type: Interrupt
 * Subtype: Used or Lost
 * Title: Antilles Maneuver & Moving To Attack Position
 */
public class Card501_119 extends AbstractUsedOrLostInterrupt {
    public Card501_119() {
        super(Side.LIGHT, 4, "Antilles Maneuver & Moving To Attack Position", Uniqueness.UNIQUE);
        addComboCardTitles("Antilles Maneuver", "Moving To Attack Position");
        setGameText("USED: For remainder of turn, opponent must first use 1 force to fire a weapon (and their weapon destiny draws targeting your starships are -1). " +
                "LOST: During battle at a system where you have two or more starships, draw two battle destiny if unable to otherwise. OR During battle your snub fighter is immune to attrition and draws battle destiny if unable to otherwise.");
        addIcons(Icon.VIRTUAL_SET_20);
        setTestingText("Antilles Maneuver & Moving To Attack Position");
    }

    @Override
    protected List<PlayInterruptAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self) {
        List<PlayInterruptAction> actions = new LinkedList<PlayInterruptAction>();

        final PlayInterruptAction action1 = new PlayInterruptAction(game, self, CardSubtype.USED);
        action1.setText("Affect opponent's weapons");
        // Allow response(s)
        action1.allowResponses("Make opponent use 1 Force to fire a weapon and make opponent's weapon destiny draws targeting your starships -1 for remainder of turn",
                new RespondablePlayCardEffect(action1) {
                    @Override
                    protected void performActionResults(Action targetingAction) {
                        // Perform result(s)
                        action1.appendEffect(
                                new AddUntilEndOfTurnModifierEffect(action1,
                                        new ExtraForceCostToFireWeaponModifier(self, Filters.opponents(self), 1),
                                        "Makes opponent first use 1 Force to fire a weapon"));
                        action1.appendEffect(
                                new AddUntilEndOfTurnModifierEffect(action1,
                                        new EachWeaponDestinyModifier(self, Filters.opponents(self), -1, Filters.and(Filters.your(self), Filters.starship)),
                                        "Makes opponent's weapon destiny draws targeting your starships -1"));
                    }
                }
        );
        actions.add(action1);

        final Filter starshipFilter = Filters.and(Filters.your(self), Filters.starship, Filters.piloted, Filters.participatingInBattle);
        if (GameConditions.isDuringBattleAt(game, Filters.system)
            && GameConditions.canTarget(game, self, Filters.and(starshipFilter, Filters.with(self, starshipFilter)))) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Draw two battle destiny if unable to otherwise");

            action.allowResponses(new RespondablePlayCardEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            // Perform result(s)
                            action.appendEffect(
                                    new AddUntilEndOfBattleModifierEffect(action,
                                            new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, starshipFilter, 2),
                                            "Draws two battle destiny if unable to otherwise"));

                        }
                    }
            );

            actions.add(action);
        }

        Filter filter = Filters.and(Filters.your(self), Filters.snub_fighter, Filters.piloted, Filters.participatingInBattle);
        // Check condition(s)
        if (GameConditions.isDuringBattle(game)
                && GameConditions.canTarget(game, self, filter)) {

            final PlayInterruptAction action = new PlayInterruptAction(game, self, CardSubtype.LOST);
            action.setText("Make a snub fighter immune to attrition");
            // Choose target(s)
            action.appendTargeting(
                    new TargetCardOnTableEffect(action, playerId, "Target snub fighter", filter) {
                        @Override
                        protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                            action.addAnimationGroup(targetedCard);
                            // Allow response(s)
                            action.allowResponses("Target " + GameUtils.getCardLink(targetedCard),
                                    new RespondablePlayCardEffect(action) {
                                        @Override
                                        protected void performActionResults(Action targetingAction) {
                                            // Get the targeted card(s) from the action using the targetGroupId.
                                            // This needs to be done in case the target(s) were changed during the responses.
                                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);

                                            // Perform result(s)
                                            action.appendEffect(
                                                    new AddUntilEndOfBattleModifierEffect(action,
                                                            new ImmuneToAttritionModifier(self, finalTarget),
                                                            "Makes " + GameUtils.getCardLink(finalTarget) + " immune to attrition"));
                                            action.appendEffect(
                                                    new AddUntilEndOfBattleModifierEffect(action,
                                                            new DrawsBattleDestinyIfUnableToOtherwiseModifier(self, finalTarget, 1),
                                                            "Makes " + GameUtils.getCardLink(finalTarget) + " draw battle destiny if unable to otherwise"));

                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }
}