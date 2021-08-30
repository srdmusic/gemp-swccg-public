package com.gempukku.swccgo.cards.set601.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtSameLocationAsCondition;
import com.gempukku.swccgo.cards.conditions.DuringBattleAtCondition;
import com.gempukku.swccgo.cards.conditions.HereCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.CancelCardActionBuilder;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.LoseCardsFromTableEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.rules.LeavesTableCardRule;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Block 6
 * Type: Effect
 * Title: Revenge Of The Sith
 */
public class Card601_094 extends AbstractNormalEffect {
    public Card601_094() {
        super(Side.DARK, 6, PlayCardZoneOption.ATTACHED, "Revenge Of The Sith", Uniqueness.UNIQUE);
        setLore("A symbol of the Dark Lord of the Sith, and of the seductive power of the dark side. Hologram.");
        setGameText("Deploy on Vader. [Coruscant] holograms are lost. Opponent may not add destiny draws to total power or attrition here. While with a Skywalker or Jedi, Vader's defense value is +2 (to a maximum of 8) and his immunity to attrition is +2.  When Vader leaves table, place Effect in Used Pile. (Immune to Alter.)");
        addIcons(Icon.CLOUD_CITY, Icon.LEGACY_BLOCK_6);
        addKeyword(Keyword.HOLOGRAM);
        addImmuneToCardTitle(Title.Alter);
        setAsLegacy(true);
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.Vader;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotAddDestinyDrawsToPowerModifier(self, new DuringBattleAtCondition(Filters.here(self)), opponent));
        modifiers.add(new MayNotAddDestinyDrawsToAttritionModifier(self, new DuringBattleAtCondition(Filters.here(self)), opponent));
        modifiers.add(new DefenseValueModifier(self, Filters.and(Filters.Vader, Filters.with(self, Filters.or(Filters.Skywalker, Filters.Jedi))), 2));
        //TODO "maximum 8"
        modifiers.add(new ImmunityToAttritionChangeModifier(self, Filters.and(Filters.Vader, Filters.with(self, Filters.or(Filters.Skywalker, Filters.Jedi))), 2));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        Filter filter = Filters.and(Icon.CORUSCANT, Filters.hologram);
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canSpot(game, self, filter)) {
            Collection<PhysicalCard> toBeLost = Filters.filterAllOnTable(game, filter);

            if (!toBeLost.isEmpty()) {
                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Make [Coruscant] holograms lost");
                // Build action using common utility
                action.appendEffect(
                        new LoseCardsFromTableEffect(action, toBeLost));
                actions.add(action);
            }
        }

        System.out.println("[while in play] self just left table: " + TriggerConditions.leavesTable(game, effectResult, self));
        System.out.println("[while in play] Vader just left table: " + TriggerConditions.leavesTable(game, effectResult, Filters.Vader));
        System.out.println("Was this attached to something? " + (self.getAttachedTo() == null?"no":"yes: "+self.getAttachedTo().getTitle()));

        if (TriggerConditions.leavesTable(game, effectResult, Filters.Vader)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place in Used Pile");
            action.setActionMsg("Place " + GameUtils.getCardLink(self) + " in Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new PlaceCardInUsedPileFromTableEffect(action, self));
            actions.add(action);
        }
        return actions;
    }

    protected List<RequiredGameTextTriggerAction> getGameTextLeavesTableRequiredTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        //TODO this doesn't work
        System.out.println("[leaves table] self just left table: " + TriggerConditions.leavesTable(game, effectResult, self));
        System.out.println("[leaves table] Vader just left table: " + TriggerConditions.leavesTable(game, effectResult, Filters.Vader));
        System.out.println("Was this attached to something? " + (self.getAttachedTo() == null?"no":"yes: "+self.getAttachedTo().getTitle()));
        if (TriggerConditions.leavesTable(game, effectResult, Filters.Vader)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place in Used Pile");
            action.setActionMsg("Place " + GameUtils.getCardLink(self) + " in Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new PlaceCardInUsedPileFromTableEffect(action, self));
            actions.add(action);
        }
        return actions;
    }
}