package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.PilotingCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Filterable;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.ImmuneToAttritionLessThanModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotAttachModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Imperial
 * Title: Captain Lennox (V)
 */
public class Card501_015 extends AbstractImperial {
    public Card501_015() {
        super(Side.DARK, 2, 2, 2, 2, 4, Title.Captain_Lennox, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setVirtualSuffix(true);
        setLore("Captain of the Imperial Star Destroyer Tyrant. An able leader. Unlike most Imperial officers, he is dedicated to his ship and crew. Finds political maneuvering distasteful.");
        setGameText("Adds 2 to power of anything he pilots. While piloting a [Hoth], [Dagobah], or [Cloud City] Star Destroyer, it is immune to attrition < 4 and starships here may not ‘attach.’ During battle, if opponent drew destiny to subtract from your total attrition, opponent loses 1 Force.");
        addKeywords(Keyword.CAPTAIN, Keyword.LEADER);
        addIcons(Icon.HOTH, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_22);
        setTestingText("Captain Lennox (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter here = Filters.here(self);
        List<Modifier> modifiers = new LinkedList<Modifier>();
        Filterable matchingStarDestroyer = Filters.and(Filters.Star_Destroyer, Filters.or(Icon.DAGOBAH, Icon.HOTH, Icon.CLOUD_CITY));
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));
        modifiers.add(new ImmuneToAttritionLessThanModifier(self, Filters.and(Filters.your(self), Filters.hasPiloting(self), matchingStarDestroyer), 4));
        modifiers.add(new MayNotAttachModifier(self, Filters.and(Filters.starship, Filters.here(self)), new PilotingCondition(self, matchingStarDestroyer)));

        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(final SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (GameConditions.isDuringBattleWithParticipant(game, self)
                && TriggerConditions.isDestinyToReduceAttritionJustDrawnBy(game, effectResult, opponent)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);

            action.setText("Make opponent lose 1 Force");
            action.setActionMsg("Make opponent lose 1 Force");

            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));

            return Collections.singletonList(action);
        }
        return null;
    }
}
