package com.gempukku.swccgo.cards.set501.light;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardsInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.modifiers.DeployCostToLocationModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.LostInterruptModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

/**
 * Set: Playtesting
 * Type: Effect
 * Title: Do, Or Do Not & Wise Advice (V)
 */
public class Card501_207 extends AbstractNormalEffect {
    public Card501_207() {
        super(Side.LIGHT, 1, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Do, Or Do Not & Wise Advice", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        addComboCardTitles(Title.Do_Or_Do_Not, Title.Wise_Advice);
        setGameText("Deploy on table. Sense and Alter are Lost Interrupts. When any player makes a destiny draw for Sense or Alter, and that destiny draw is successful, that player loses 2 Force (may not be reduced). Grogu and [Dagobah] Luke are Padawans. Yoda and Padawans deploy -1 (except to Lothal) and are immune to Imperial Barrier. If your Padawan about to leave table, may lose 1 Force to place your cards on them in Used Pile. [Immune to Alter.]");
        addIcons(Icon.REFLECTIONS_III, Icon.VIRTUAL_SET_27);
        addImmuneToCardTitle(Title.Alter);
        setVirtualSuffix(true);
        setTestingText("Do, Or Do Not & Wise Advice (V)");
    }
    
    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new LostInterruptModifier(self, Filters.or(Filters.Sense, Filters.Alter)));
        // character filter enforces non-characters becoming padawans
        Filter groguCharacter = Filters.and(Filters.Grogu, Filters.character);
        Filter dagobahLuke = Filters.and(Filters.Luke, Filters.icon(Icon.DAGOBAH), Filters.character);
        Filter groguAndLuke = Filters.or(groguCharacter, dagobahLuke);
        modifiers.add(new KeywordModifier(self, groguAndLuke, Keyword.PADAWAN));
        Filter padawansAndYoda = Filters.or(Filters.padawan, Filters.Yoda);
        modifiers.add(new DeployCostToLocationModifier(self, padawansAndYoda, -1, Filters.not(Filters.Lothal_location)));
        modifiers.add(new ImmuneToTitleModifier(self, padawansAndYoda, Title.Imperial_Barrier));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.senseOrAlterDestinyDrawSuccessful(game, effectResult)) {
            final String playerId = effectResult.getPerformingPlayerId();

            RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make " + playerId + " lose 2 Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, playerId, 2, true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, Filters.and(Filters.your(self), Filters.padawan))) {
            PhysicalCard cardAboutToLeaveTable = ((AboutToLeaveTableResult) effectResult).getCardAboutToLeaveTable();
            Collection<PhysicalCard> yourCardsAttachedToPadawan = Filters.filterAllOnTable(game, Filters.and(Filters.your(self), Filters.attachedTo(cardAboutToLeaveTable)));
            if (!yourCardsAttachedToPadawan.isEmpty()) {
                final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
                action.setText("Lose 1 Force to place cards in Used Pile");
                // Perform result(s)
                action.appendEffect(
                        new LoseForceEffect(action, playerId, 1));
                action.appendEffect(
                        new PlaceCardsInUsedPileFromTableEffect(action, yourCardsAttachedToPadawan));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
