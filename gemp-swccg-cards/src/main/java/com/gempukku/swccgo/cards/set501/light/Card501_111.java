package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.CapturedOnlyCondition;
import com.gempukku.swccgo.cards.conditions.WithCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.ModelType;
import com.gempukku.swccgo.common.Persona;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.SpotOverride;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.Condition;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Set 4
 * Type: Character
 * Subtype: Droid
 * Title: BB-8 (Beebee-Ate)
 */
public class Card501_111 extends AbstractDroid {
    public Card501_111() {
        super(Side.LIGHT, Math.PI, 1, 1, 4, "BB-8 (Beebee-Ate)", Uniqueness.UNIQUE);
        setAlternateDestiny(2 * Math.PI);
        addPersona(Persona.BB8);
        setGameText("Elis Helrot may not target this site. Adds 1 to Force drains at same site with a Resistance character (or if a captive). During your control phase, if opponent's Undercover spy here, opponent loses 2 force (cannot be reduced).");
        addIcons(Icon.EPISODE_VII, Icon.NAV_COMPUTER, Icon.VIRTUAL_SET_4);
        addModelType(ModelType.ASTROMECH);
        setTestingText("BB-8 (Beebee-Ate) (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new ForceDrainModifier(self, Filters.sameSite(self), new WithCondition(self, Filters.Resistance_character), 1, playerId));
        modifiers.add(new ForceDrainModifier(self, Filters.sameSite(self), new WithCondition(self, Filters.Resistance_character), 1, opponent));
        modifiers.add(new MayNotBeTargetedByModifier(self, Filters.sameSite(self), Filters.Elis_Helrot));
        return modifiers;
    }

    @Override
    protected List<Modifier> getGameTextWhileInactiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        Condition isCaptive = new CapturedOnlyCondition(self);
        Filter sameSite = Filters.sameSite(self);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new ForceDrainModifier(self, sameSite, isCaptive, 1, playerId));
        modifiers.add(new ForceDrainModifier(self, sameSite, isCaptive, 1, opponent));
        return modifiers;
    }


    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        String opponent = game.getOpponent(playerId);

        // Check condition(s)
        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.isWith(game, self, SpotOverride.INCLUDE_UNDERCOVER, Filters.and(Filters.opponents(self), Filters.undercover_spy))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Make opponent lose 2 Force");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 2, true));
            return Collections.singletonList(action);
        }
        return null;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        // Check if reached end of each control phase and action was not performed yet.
        if (TriggerConditions.isEndOfYourPhase(game, effectResult, Phase.CONTROL, playerId)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.CONTROL)
                && GameConditions.isWith(game, self, SpotOverride.INCLUDE_UNDERCOVER, Filters.and(Filters.opponents(self), Filters.undercover_spy))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setPerformingPlayer(playerId);
            action.setText("Make opponent lose 2 Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 2, true));
            return Collections.singletonList(action);
        }
        return null;
    }
}
