package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.HitCondition;
import com.gempukku.swccgo.cards.conditions.PresentAtCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.conditions.NotCondition;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeForfeitedInBattleModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 19
 * Type: Character
 * Subtype: Imperial
 * Title: Arihnda Pryce
 */
public class Card501_028 extends AbstractImperial {
    public Card501_028() {
        super(Side.DARK, 2, 3, 2, 3, 5, "Arihnda Pryce", Uniqueness.UNIQUE);
        setPolitics(1);
        setLore("Female miner. Leader.");
        setGameText("If you just captured a Rebel here (or stacked a card as 'artwork') you may take a card without ability into hand from Reserve Deck; reshuffle. If you just lost a battle here, Pryce may not be forfeited unless 'hit' or no other Imperials present. Immune to attrition < 4.");
        addKeywords(Keyword.FEMALE, Keyword.MINER, Keyword.LEADER);
        addIcons(Icon.VIRTUAL_SET_19);
        setTestingText("Arihnda Pryce");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(final SwccgGame game, final PhysicalCard self) {
        final String opponent = game.getOpponent(self.getOwner());
        Condition inLosingBattle = new Condition(){
            @Override
            public boolean isFulfilled(GameState gameState, ModifiersQuerying modifiersQuerying) {
                return GameConditions.isDuringBattleWonBy(game, opponent);
            }
        };
        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new MayNotBeForfeitedInBattleModifier(self, self, new AndCondition(inLosingBattle, new NotCondition(new HitCondition(self)), new PresentAtCondition(Filters.and(Filters.other(self), Filters.Imperial), Filters.here(self)))));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 4));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.ARIHNDA_PRYCE__UPLOAD_CARD;

        if (GameConditions.canSearchReserveDeck(game, playerId, self, gameTextActionId)
                && (TriggerConditions.captured(game, effectResult, playerId, Filters.and(Filters.Rebel, Filters.here(self)))
                || TriggerConditions.justStackedCardOn(game, effectResult, Filters.any, Filters.Thrawns_Art_Collection))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take card into hand from Reserve Deck");
            action.setActionMsg("Take a card without ability into hand from Reserve Deck");

            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.not(Filters.hasAbilityOrHasPermanentPilotWithAbility), true, true));
            return Collections.singletonList(action);

        }
        return null;
    }
}
