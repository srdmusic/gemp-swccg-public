package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractRebelResistance;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.AddDestinyToTotalPowerEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Rebel
 * Title: Han Solo, Optimistic General
 */
public class Card501_088 extends AbstractRebelResistance {
    public Card501_088() {
        super(Side.LIGHT, 1, 4, 4, 3, 6, "Han Solo, Optimistic General", Uniqueness.UNIQUE);
        setLore("Leader. Scout.");
        setGameText("Adds 3 to power of anything he pilots. Your [Endor] scouts are destiny +1. Kylo's game text is canceled here. During battle with Chewie or Leia (or while piloting Tydirium) adds one battle destiny.");
        addPersona(Persona.HAN);
        addIcons(Icon.WARRIOR, Icon.PILOT, Icon.ENDOR, Icon.VIRTUAL_SET_15);
        addKeywords(Keyword.LEADER, Keyword.SCOUT, Keyword.GENERAL);
        setMatchingStarshipFilter(Filters.Tydirium);
        setTestingText("Han Solo, Optimistic General (ERRATA)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new AddsPowerToPilotedBySelfModifier(self, 3));
        modifiers.add(new CancelsGameTextModifier(self, Filters.and(Filters.Kylo, Filters.here(self))));
        modifiers.add(new DestinyModifier(self, Filters.and(Filters.your(self), Icon.ENDOR, Filters.scout), 1));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (GameConditions.isOncePerBattle(game, self, gameTextSourceCardId, gameTextActionId)
                && GameConditions.isDuringBattleWithParticipant(game, self)
                && (GameConditions.isDuringBattleWithParticipant(game, Filters.or(Filters.Chewie, Filters.Leia))
                    || GameConditions.isPiloting(game, self, Filters.Tydirium))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Add one destiny to total power");
            action.appendUsage(
                    new OncePerBattleEffect(action));
            action.appendEffect(
                    new AddDestinyToTotalPowerEffect(action, 1, playerId));

            return Collections.singletonList(action);
        }

        return null;
    }
}