package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.evaluators.OnTableEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfBattleModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseCardFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Effect
 * Title: Order 66
 */
public class Card501_086 extends AbstractNormalEffect {
    public Card501_086() {
        super(Side.DARK, 6, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Order 66", Uniqueness.UNIQUE);
        setLore("Vader's hologram exacts loyalty from his legions.");
        setGameText("Deploy on table." +
                "Jedi are deploy +1 for each other Jedi on table." +
                "Opponent must use 2 Force to initiate a Force drain with two Jedi." +
                "Once per game, during battle, may lose Effect;" +
                "[E1] Jedi immunity to attrition is limited to < 5" +
                "and if your non-[E7] trooper present," +
                "Jedi Masters may not swing a lightsaber at non-troopers. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_19);
        addKeywords(Keyword.HOLOGRAM);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Order 66");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new DeployCostModifier(self, Filters.Jedi, new OnTableEvaluator(self, Filters.Jedi)));
        modifiers.add(new InitiateForceDrainCostModifier(self, Filters.sameLocationAs(self, Filters.and(Filters.Jedi, Filters.with(self, Filters.Jedi))), 2, opponent));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        //"Once per game, during battle, may lose Effect;" +
        //                "[E1] Jedi immunity to attrition is limited to < 5" +
        //                "and if your non-[E7] trooper present," +
        //                "Jedi Masters may not swing a lightsaber at non-troopers.

        // Card action 1
        GameTextActionId gameTextActionId = GameTextActionId.ORDER_66__AFFECT_JEDI;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
            && GameConditions.isDuringBattle(game)) {

            Filter jediMasterPresentWithYourNonE7Trooper = Filters.and(Filters.Jedi_Master, Filters.presentWith(self,
                    Filters.and(Filters.your(playerId), Filters.not(Filters.icon(Icon.EPISODE_VII)), Filters.trooper)));

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Affect Jedi");
            action.setActionMsg("Affect Jedi");
            // Pay cost(s)
            action.appendCost(
                    new LoseCardFromTableEffect(action, self));
            // Perform result(s)
            action.appendEffect(new AddUntilEndOfBattleModifierEffect(
                    action, new ImmunityToAttritionLimitedToModifier(self, Filters.and(Filters.icon(Icon.EPISODE_I), Filters.Jedi), 5), "[E1] Jedi immunity to attrition is limited to < 5") {
            });
            action.appendEffect(
                    new AddUntilEndOfBattleModifierEffect(action,
                            new MayNotBeTargetedByModifier(self, Filters.not(Filters.trooper), Filters.and(Filters.lightsaber, Filters.or(Filters.permanentWeaponOf(jediMasterPresentWithYourNonE7Trooper), Filters.attachedTo(jediMasterPresentWithYourNonE7Trooper)))),
                            "Prevents characters from being targeted by lightsabers"));
            actions.add(action);
        }
        return actions;
    }
}
