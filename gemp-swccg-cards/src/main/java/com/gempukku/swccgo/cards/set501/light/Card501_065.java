package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.OrCondition;
import com.gempukku.swccgo.logic.effects.ModifyDestinyEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.UseForceEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalPowerModifier;
import com.gempukku.swccgo.logic.modifiers.TotalTrainingDestinyModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: Reflection (V)
 */
public class Card501_065 extends AbstractNormalEffect {
    public Card501_065() {
        super(Side.LIGHT, 3, PlayCardZoneOption.ATTACHED, Title.Reflection, Uniqueness.UNIQUE);
        setVirtualSuffix(true);
        setLore("It was hard to imagine the enormous losses the Alliance suffered during the Battle of Hoth. Leia contemplated what she could do to help the Rebellion recover.");
        setGameText("Deploy on [Cloud City] Leia. If at Guest Quarters (or a [Skywalker] Objective on table): your total power is +2 here and, once per turn, may add 1 to a just drawn weapon or battle destiny (and once per turn, may subtract 1 from a just drawn weapon or battle destiny) at another location.");
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] Reflection (V)");
        hideFromDeckBuilder();
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Icon.CLOUD_CITY, Filters.Leia);
    }

    @Override
    protected Filter getGameTextValidTargetFilterToRemainAttachedToAfterCrossingOver(final SwccgGame game, final PhysicalCard self, PlayCardOptionId playCardOptionId) {
        return Filters.Leia;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new TotalPowerModifier(self, Filters.here(self),new OrCondition(new AtCondition(self, Filters.title("Cloud City: Guest Quarters")), new OnTableCondition(self, Filters.and(Icon.SKYWALKER, Filters.Objective))), 2, self.getOwner()));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<OptionalGameTextTriggerAction>();

        if (GameConditions.isAtLocation(game, self, Filters.title("Cloud City: Guest Quarters"))
            || GameConditions.canSpot(game, self, Filters.and(Icon.SKYWALKER, Filters.Objective))) {

            GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

            // Check condition(s)
            if ((TriggerConditions.isWeaponDestinyJustDrawn(game, effectResult, Filters.not(Filters.here(self)))
                    || (TriggerConditions.isBattleDestinyJustDrawn(game, effectResult))
                        && !GameConditions.isDuringBattleAt(game, Filters.here(self)))
                    && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

                OptionalGameTextTriggerAction action1 = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action1.setText("Add 1 to destiny draw");
                action1.appendUsage(
                        new OncePerTurnEffect(action1)
                );
                // Perform result(s)
                action1.appendEffect(
                        new ModifyDestinyEffect(action1, 1));
                actions.add(action1);
            }

            gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

            // Check condition(s)
            if ((TriggerConditions.isWeaponDestinyJustDrawn(game, effectResult, Filters.not(Filters.here(self)))
                    || (TriggerConditions.isBattleDestinyJustDrawn(game, effectResult))
                        && !GameConditions.isDuringBattleAt(game, Filters.here(self)))
                    && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

                OptionalGameTextTriggerAction action2 = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
                action2.setText("Subtract 1 from destiny draw");
                action2.appendUsage(
                        new OncePerTurnEffect(action2)
                );
                // Perform result(s)
                action2.appendEffect(
                        new ModifyDestinyEffect(action2, -1));
                actions.add(action2);
            }
        }
        return actions;
    }
}