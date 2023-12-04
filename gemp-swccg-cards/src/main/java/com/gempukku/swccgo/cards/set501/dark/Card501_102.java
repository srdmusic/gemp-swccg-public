package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractFirstOrder;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AllAbilityAtLocationProvidedByCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.ArmorModifier;
import com.gempukku.swccgo.logic.modifiers.CancelImmunityToAttritionModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotReactFromLocationModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotReactToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Effect;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: First Order
 * Title: Captain Moden Canady
 */
public class Card501_102 extends AbstractFirstOrder {
    public Card501_102() {
        super(Side.DARK, 2, 3, 2, 2, 5, "Captain Moden Canady", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setLore("Leader.");
        setGameText("Adds 2 power to anything he pilots and 2 to armor of Fulminatrix. While all your ability here is provided by Fulminatrix and First Order TIE pilots: opponent’s immunity to attrition (and reacts), Hit And Run and Alternatives To Fighting are canceled here.\n" + //
                "");
        addIcons(Icon.EPISODE_VII, Icon.PILOT, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.LEADER);
        setMatchingStarshipFilter(Filters.Fulminatrix);
        setTestingText("Captain Moden Canady");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Condition allAbilityCondition = new AllAbilityAtLocationProvidedByCondition(self, playerId, Filters.here(self), Filters.or(Filters.Fulminatrix, Filters.piloting(Filters.Fulminatrix), Filters.First_Order_TIE, Filters.piloting(Filters.First_Order_TIE)));
        modifiers.add(new CancelImmunityToAttritionModifier(self, Filters.and(Filters.opponents(self), Filters.atSameLocation(self)), allAbilityCondition));
        modifiers.add(new MayNotReactToLocationModifier(self, Filters.here(self), allAbilityCondition, opponent));
        modifiers.add(new MayNotReactFromLocationModifier(self, Filters.here(self), allAbilityCondition, opponent));    
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new ArmorModifier(self, Filters.and(Filters.Fulminatrix, Filters.hasPiloting(self)), 2));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredBeforeTriggers(final SwccgGame game, Effect effect, final PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        // Check condition(s)
        if (TriggerConditions.isPlayingCard(game, effect, Filters.or(Filters.Hit_And_Run, Filters.Alternatives_To_Fighting))
                && GameConditions.isDuringBattleAt(game, Filters.here(self))
                && GameConditions.isAllAbilityAtLocationProvidedBy(game, self, playerId, Filters.here(self), Filters.or(Filters.Fulminatrix, Filters.piloting(Filters.Fulminatrix)))
                && GameConditions.canCancelCardBeingPlayed(game, self, effect)) {

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            // Build action using common utility
            CancelCardActionBuilder.buildCancelCardBeingPlayedAction(action, effect);
            return Collections.singletonList(action);
        }
        return null;
    }

}
