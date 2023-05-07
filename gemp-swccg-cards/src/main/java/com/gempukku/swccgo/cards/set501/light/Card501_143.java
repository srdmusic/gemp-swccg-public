package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.effects.PreventEffectOnCardEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 21
 * Type: Effect
 * Title: Reflection (V)
 */
public class Card501_143 extends AbstractNormalEffect {
    public Card501_143() {
        super(Side.LIGHT, 3, PlayCardZoneOption.ATTACHED, Title.Reflection, Uniqueness.UNIQUE, ExpansionSet.SET_21, Rarity.R);
        setLore("It was hard to imagine the enormous losses the Alliance suffered during the Battle of Hoth. Leia contemplated what she could do to help the Rebellion recover.");
        setVirtualSuffix(true);
        setGameText("Deploy on [Cloud City] or [Endor] Leia. She is defense value +2. While at a battleground, Force drain +1 here and during opponent’s draw phase, you may retrieve 1 Force if a battle was not initiated this turn. If Leia about to be lost, may place this Effect in Used Pile. [Immune to Alter.]");
        setGameText("Deploy on [Cloud City] or [Endor] Leia. She is defense value +2. While at a battleground site: Force drain +1 here and retrieve 1 Force if a Rebel just initiated battle (2 if Leia initiated against an Imperial). If Effect about to be lost, may place it in Used Pile instead. [Immune to Alter.]");
        addKeywords(Keyword.DEPLOYS_ON_CHARACTERS);
        addIcons(Icon.DAGOBAH, Icon.VIRTUAL_SET_21);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Reflection (V)");
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.or(Icon.CLOUD_CITY, Icon.ENDOR), Filters.Leia);
    }

    @Override
    protected Filter getGameTextValidTargetFilterToRemainAttachedToAfterCrossingOver(final SwccgGame game, final PhysicalCard self, PlayCardOptionId playCardOptionId) {
        return Filters.Leia;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        Condition atBattlegroundCondition = new AtCondition(self, Filters.battleground_site);
        modifiers.add(new DefenseValueModifier(self, Filters.hasAttached(self), 2));
        modifiers.add(new ForceDrainModifier(self, Filters.here(self), atBattlegroundCondition, 1, self.getOwner()));
        return modifiers;
    }


    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();

        // Check condition(s)
        if (TriggerConditions.battleInitiated(game, effectResult, playerId)
                && GameConditions.isAtLocation(game, self, Filters.battleground_site)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.your(self), Filters.and(Filters.your(self), Filters.Rebel)))) {
            int numForceToRetrieve = GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.opponents(self), Filters.Imperial, Filters.with(self, Filters.Leia))) ? 2 : 1;

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Retrieve " + numForceToRetrieve + " Force");
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, numForceToRetrieve) {
                        @Override
                        public Collection<PhysicalCard> getAdditionalCardsInvolvedInForceRetrieval() {
                            return Filters.filterActive(game, null, Filters.and(Filters.your(self), Filters.participatingInBattle));
                        }
                        @Override
                        public boolean isDueToInitiatingBattle() {
                            return true;
                        }
                    });
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isAboutToBeLostIncludingAllCardsSituation(game, effectResult, self)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Place in Used Pile");
            action.setActionMsg("Place " + GameUtils.getCardLink(self) + " in Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new PreventEffectOnCardEffect(action, ((AboutToLeaveTableResult) effectResult).getPreventableCardEffect(), self, null));
            action.appendEffect(
                    new PlaceCardInUsedPileFromTableEffect(action, self));
            return Collections.singletonList(action);
        }
        return null;
    }
}