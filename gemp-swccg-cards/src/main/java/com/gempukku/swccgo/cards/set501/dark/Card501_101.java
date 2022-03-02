package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.CardMatchesEvaluator;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotAttachModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.TriggerConditions;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


/**
 * Set: Hoth
 * Type: Character
 * Subtype: Imperial
 * Title: Captain Lennox
 */
public class Card501_101 extends AbstractImperial {
    public Card501_101() {
        super(Side.DARK, 2, 3, 2, 2, 5, Title.Captain_Lennox, Uniqueness.UNIQUE);
        setLore("Captain of the Imperial Star Destroyer Tyrant. An able leader. Unlike most Imperial officers, he is dedicated to his ship and crew. Finds political maneuvering distasteful.");
        setGameText("Adds 2 to power of anything he pilots. While piloting a [Hoth], [Dag] and [CC] Star Destroyer, it is immune to attrition < 4 and starships here may not ‘attach.’ During battle, if opponent drew destiny to subtract from your total attrition, opponent loses 1 Force.");
        addKeywords(Keyword.CAPTAIN, Keyword.LEADER);
        addIcons(Icon.HOTH, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_18);
        setMatchingStarshipFilter(Filters.Tyrant);
        setTestingText("Captain Lennox (v)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter here = Filters.here(self);
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Filterable matchingStarDestroyer = Filters.and(Filters.Star_Destroyer, Filters.or(Filters.icon(Icon.DAGOBAH), Filters.icon(Icon.HOTH), Filters.icon(Icon.CLOUD_CITY)));
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, Filters.and(Filters.your(self), Filters.hasPiloting(self), matchingStarDestroyer), 4));
        modifiers.add(new MayNotAttachModifier(self, Filters.and(Filters.starfighter, Filters.here(self)), new PilotingCondition(self, matchingStarDestroyer)));

        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (TriggerConditions.isDestinyToReduceAttritionJustDrawnBy(game, effectResult, opponent)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);

            action.setText("Make opponent lose 1 force");
            action.setActionMsg("Make opponent lose 1 force");

            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));

            return Collections.singletonList(action);
        }
        return null;
    }
}
