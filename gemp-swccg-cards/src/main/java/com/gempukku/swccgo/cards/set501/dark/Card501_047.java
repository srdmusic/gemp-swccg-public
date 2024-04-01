package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractImperial;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Keyword;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.conditions.AndCondition;
import com.gempukku.swccgo.logic.conditions.PhaseCondition;
import com.gempukku.swccgo.logic.effects.FlipCardEffect;
import com.gempukku.swccgo.logic.modifiers.AddsPowerToPilotedBySelfModifier;
import com.gempukku.swccgo.logic.modifiers.BattleDamageLimitModifier;
import com.gempukku.swccgo.logic.modifiers.CancelOpponentsForceDrainBonusesModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.SuspendsCardModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Character
 * Subtype: Imperial
 * Title: Captain Sarkli (V)
 */
public class Card501_047 extends AbstractImperial {
    public Card501_047() {
        super(Side.DARK, 2, 2, 2, 2, 3, Title.Captain_Sarkli, Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.R);
        setVirtualSuffix(true);
        setLore("Piett's nephew. Once granted audience with Emperor. On fast-track to promotion. Absolutely fearless spy.");
        setGameText("[P] 2. While at opponent’s War Room, " +
                "opponent’s Force drain bonuses are canceled and battle damage here is limited to 2. " +
                "While at a ‘probed’ system, opponent’s [SE] objective is suspended during your draw phase. " +
                "Immune to Dodonna.");
        addIcons(Icon.DEATH_STAR_II, Icon.PILOT, Icon.WARRIOR, Icon.VIRTUAL_SET_23);
        addKeywords(Keyword.SPY, Keyword.CAPTAIN);
        addImmuneToCardTitle(Title.General_Dodonna);
        setTestingText("Captain Sarkli (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 2));

        AtCondition atOpponentsWarRoomCondition = new AtCondition(self, Filters.and(Filters.opponents(playerId), Filters.war_room));
        modifiers.add(new CancelOpponentsForceDrainBonusesModifier(self, atOpponentsWarRoomCondition));
        modifiers.add(new BattleDamageLimitModifier(self, Filters.here(self), atOpponentsWarRoomCondition, 2, playerId));
        modifiers.add(new BattleDamageLimitModifier(self, Filters.here(self), atOpponentsWarRoomCondition, 2, opponent));

        AtCondition atProbedSystemCondition = new AtCondition(self, Filters.and(Filters.system, Filters.hasStacked(Filters.probeCard)));
        PhaseCondition duringDrawPhaseCondition = new PhaseCondition(Phase.DRAW, playerId);
        AndCondition condition = new AndCondition(atProbedSystemCondition, duringDrawPhaseCondition);
        modifiers.add(new SuspendsCardModifier(self, Filters.and(Filters.opponents(playerId), Filters.icon(Icon.SPECIAL_EDITION), Filters.Objective), condition));
        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.occupiesLocation(game, self, Filters.Subjugated_system)) {
            PhysicalCard objective = Filters.findFirstActive(game, self, Filters.Liberation);
            if (objective != null
                    && GameConditions.canBeFlipped(game, objective)) {

                final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Flip Liberation");
                action.setActionMsg("Flip " + GameUtils.getCardLink(objective));
                // Perform result(s)
                action.appendEffect(
                        new FlipCardEffect(action, objective));
                return Collections.singletonList(action);
            }
        }
        return null;
    }
}
