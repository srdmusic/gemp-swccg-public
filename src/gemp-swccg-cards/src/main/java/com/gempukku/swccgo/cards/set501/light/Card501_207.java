package com.gempukku.swccgo.cards.set501.light;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Persona;
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
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlaceCardsInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.LostInterruptModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.MovesForFreeUsingLandspeedModifier;
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
        setGameText("Deploy on table. Sense and Alter are Lost Interrupts. When any player makes a destiny draw for Sense or Alter, and that destiny draw is successful, that player loses 2 Force (may not be reduced). May [download] Bog Clearing. [Dagobah] Luke is a Padawan. Yoda and Padawans are immune to Imperial Barrier and Stunning Leader and may move for free using landspeed. Once per game, may deploy Anakin's Lightsaber from Lost Pile. [Immune to Alter.]");
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
        Filter dagobahLuke = Filters.and(Icon.DAGOBAH, Filters.Luke, Filters.character);
        modifiers.add(new KeywordModifier(self, dagobahLuke, Keyword.PADAWAN));
        Filter padawansAndYoda = Filters.or(Filters.padawan, Filters.Yoda);
        modifiers.add(new ImmuneToTitleModifier(self, padawansAndYoda, Title.Imperial_Barrier));
        modifiers.add(new ImmuneToTitleModifier(self, padawansAndYoda, Title.Stunning_Leader));
        modifiers.add(new MovesForFreeUsingLandspeedModifier(self, padawansAndYoda));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.DO_OR_DO_NOT_WISE_ADVICE__DOWNLOAD_BOG_CLEARING;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Dagobah_Bog_Clearing)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Bog Clearing from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Dagobah_Bog_Clearing, true));
            actions.add(action);
        }

        gameTextActionId = GameTextActionId.DO_OR_DO_NOT_WISE_ADVICE__DEPLOY_ANAKINS_LIGHTSABER_FROM_LOST_PILE;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                    && GameConditions.canDeployCardFromLostPile(game, playerId, self, gameTextActionId, Persona.ANAKINS_LIGHTSABER)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Deploy Anakin's Lightsaber from Lost Pile");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromLostPileEffect(action, Filters.Anakins_Lightsaber, false));
            actions.add(action);
        }

        return actions;
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
