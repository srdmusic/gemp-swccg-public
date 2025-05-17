package com.gempukku.swccgo.framework;

import com.gempukku.swccgo.game.ActionProxy;
import com.gempukku.swccgo.logic.actions.SystemQueueAction;
import com.gempukku.swccgo.logic.decisions.CardActionSelectionDecision;
import com.gempukku.swccgo.logic.decisions.DecisionResultInvalidException;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.Effect;
import com.gempukku.swccgo.logic.timing.StandardEffect;

import static org.junit.Assert.assertTrue;

/**
 * Sometimes, there just isn't a card that has the precise action or effect that you want to use (especially when
 * developing new mechanics), so these functions permit the tester to staple actions, effects, and modifiers onto
 * physical cards as needed.  This is also useful when testing fundamental existing mechanics without any
 * confounding variables.
 *
 * Be warned: just because an action or effect has been added to the game does not automatically mean that Gemp will
 * respect it or be aware of it just yet.  In nearly all cases, after you have used any of these functions you will
 * need to execute a decision so that Gemp's game loop will process what's on the table (at which point it will
 * become aware of the changes you have made).  While awaiting a decision Gemp is effectively paused and thus needs
 * that moment to become aware of changes.
 */
public interface AdHocEffects extends TestBase, Decisions {
	default void carryOutEffectInPhaseActionByPlayer(String playerId, StandardEffect effect) throws DecisionResultInvalidException {
		var action = new SystemQueueAction();
		action.appendEffect(effect);
		carryOutEffectInPhaseActionByPlayer(playerId, action);
	}

	default void carryOutEffectInPhaseActionByPlayer(String playerId, Action action) throws DecisionResultInvalidException {
		var awaitingDecision = (CardActionSelectionDecision) userFeedback().getAwaitingDecision(playerId);
		awaitingDecision.addAction(action);

		PlayerDecided(playerId, "0");
	}

	default void ApplyAdHocModifier(Modifier mod)
    {
        game().getModifiersEnvironment().addAlwaysOnModifier(mod);
    }

	default void ApplyAdHocAction(ActionProxy action)
    {
        game().getActionsEnvironment().addUntilEndOfGameActionProxy(action);
    }

	default void DSExecuteAdHocEffect(StandardEffect effect) throws DecisionResultInvalidException { ExecuteAdHocEffect(DS, effect); }
	default void LSExecuteAdHocEffect(StandardEffect effect) throws DecisionResultInvalidException { ExecuteAdHocEffect(LS, effect); }
	default void ExecuteAdHocEffect(String playerId, StandardEffect effect) throws DecisionResultInvalidException {
        carryOutEffectInPhaseActionByPlayer(playerId, effect);
        assertTrue(effect.wasCarriedOut());
    }


}
