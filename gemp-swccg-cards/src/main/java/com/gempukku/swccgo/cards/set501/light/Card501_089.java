package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractAlienRepublic;
import com.gempukku.swccgo.cards.AbstractRepublic;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.choose.PlayInterruptFromLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Character
 * Subtype: Alien/Republic
 * Title: Wullffwarro
 */
public class Card501_089 extends AbstractAlienRepublic {
    public Card501_089() {
        super(Side.LIGHT, 2, 3, 4, 2, 4, "Wullffwarro", Uniqueness.UNIQUE);
        setArmor(4);
        setLore("Wookiee. Slave.");
        setGameText("When drawn for destiny, may play an Interrupt with 'Wookiee' in title from Lost Pile as if from hand (then place that card out of play). Adds 2 to power of anything he pilots (3 if a gunship). Forfeit +2 at a Kashyyyk location.");
        setSpecies(Species.WOOKIEE);
        addIcons(Icon.PILOT, Icon.WARRIOR, Icon.EPISODE_I, Icon.VIRTUAL_SET_18);
        setTestingText("[Set 19] Wullffwarro");
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalDrawnAsDestinyTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.WULLFFWARRO__PLAY_INTERRUPT_FROM_LOST_PILE;
        if (GameConditions.isDestinyCardMatchTo(game, self)
            && GameConditions.canPlayInterruptFromLostPile(game, playerId, self, gameTextActionId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Play Interrupt from Lost Pile");
            action.setActionMsg("Play an Interrupt with Wookiee in title from Lost Pile as if from hand, then place that card out of play");

            action.appendEffect(
                    new PlayInterruptFromLostPileEffect(action, Filters.or(Filters.titleContains("Wookiee"), Filters.titleContains("Wookiees")), false, true));

            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, new CardMatchesEvaluator(2, 3, Filters.gunship)));
        modifiers.add(new ForfeitModifier(self, new AtCondition(self, Filters.Kashyyyk_location), 2));
        return modifiers;
    }
}
