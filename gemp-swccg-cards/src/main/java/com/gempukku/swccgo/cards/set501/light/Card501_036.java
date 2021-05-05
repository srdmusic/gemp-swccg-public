package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebel;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.PlaceAtLocationFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Ben Kenobi (V)
 */
public class Card501_036 extends AbstractRebel {
    public Card501_036() {
        super(Side.LIGHT, 1, 5, 5, 6, 9, "Ben Kenobi", Uniqueness.UNIQUE);
        setLore("Served Bail Organa during the Clone Wars. Saved Anakin's lightsaber until he was able to give it to Luke. Hasn't gone by the name Obi-Wan for a long time.");
        setGameText("Opponent’s characters here are power -1 (-2 if [PW] Maul). Once per game, if a battle just ended here, may 'revive' (place here from Lost Pile) your character forfeited from same site this turn. Immune to attrition < 5.");
        setVirtualSuffix(true);
        addPersona(Persona.OBIWAN);
        addIcons(Icon.SPECIAL_EDITION, Icon.WARRIOR, Icon.VIRTUAL_SET_15);
        setTestingText("Ben Kenobi (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        Filter pwMaul = Filters.and(Filters.here(self), Filters.Maul, Filters.icon(Icon.PERMANENT_WEAPON));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, 5));
        modifiers.add(new PowerModifier(self, Filters.and(Filters.opponents(self), Filters.character, Filters.here(self)), new CardMatchesEvaluator(-1, -2, pwMaul)));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.BEN_KENOBI_V_REVIVE_CHARACTER;

        // Check condition(s)
        if (TriggerConditions.battleEndedAt(game, effectResult, Filters.sameSite(self))
                && GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
                && GameConditions.wasForfeitedFromLocationThisTurn(game, Filters.and(Filters.your(self), Filters.character), Filters.sameSite(self))) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("'Revive' a forfeited character");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new PlaceAtLocationFromLostPileEffect(action, playerId, Filters.and(Filters.your(self), Filters.character,
                            Filters.forfeitedFromLocationThisTurn(Filters.sameSite(self))), Filters.sameSite(self), false, false));
            return Collections.singletonList(action);
        }
        return null;
    }
}