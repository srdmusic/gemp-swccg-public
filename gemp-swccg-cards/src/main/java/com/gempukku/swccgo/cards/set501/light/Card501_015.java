package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.LookAtUsedPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DrawCardIntoHandFromUsedPileEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Character
 * Subtype: Rebel
 * Title: Kanan Jarrus, Jedi Knight
 */
public class Card501_015 extends AbstractRebel {
    public Card501_015() {
        super(Side.LIGHT, 2, 5, 4, 6, 6, "Kanan Jarrus, Jedi Knight", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Whenever you deploy Chopper, Ezra, Hera, Sabine, or Zeb, may draw top card of your Used Pile. " +
                "During your draw phase, if Kanan present at a battleground and he did not move this turn, may peek at your Used Pile. " +
                "Immune to attrition < 5.");
        addPersona(Persona.KANAN);
        addIcons(Icon.WARRIOR, Icon.VIRTUAL_SET_16);
        setTestingText("[Set 17] Kanan Jarrus, Jedi Knight");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        if (GameConditions.isDuringYourPhase(game, playerId, Phase.DRAW)
                && GameConditions.isPresentAt(game, self, Filters.battleground)
                && !GameConditions.hasPerformedRegularMoveThisTurn(game, self)
                && GameConditions.hasUsedPile(game, playerId)) {

            TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId);
            action.setText("Peek at your Used Pile");
            action.appendEffect(
                    new LookAtUsedPileEffect(action, playerId, playerId)
            );
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        Filter filter = Filters.or(Filters.Chopper, Filters.Ezra, Filters.Hera, Filters.Sabine, Filters.Zeb);

        if (TriggerConditions.justDeployed(game, effectResult, playerId, filter)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Draw top card of Used Pile");
            // Perform result(s)
            action.appendEffect(
                    new DrawCardIntoHandFromUsedPileEffect(action, playerId));
            return Collections.singletonList(action);
        }
        return null;
    }
}
