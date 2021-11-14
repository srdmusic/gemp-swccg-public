package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.BlownAwayCondition;
import com.gempukku.swccgo.cards.conditions.CommencePrimaryIgnitionTargetingCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.TakeCardIntoHandFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.EpicEventDestinyDrawModifier;
import com.gempukku.swccgo.logic.modifiers.IconModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;

import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: An Effective Demonstration
 */
public class Card501_004 extends AbstractNormalEffect {
    public Card501_004() {
        super(Side.DARK, 2, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "An Effective Demonstration");
        setLore("");
        setGameText("Deploy on table. [A New Hope] Epic Event destinies are +5 when targeting Alderaan. If Alderaan has been 'blown away,' adds one [Light Side] icon at Death Star system and opponent's total battle destiny is -1. Once per game, may take Superlaser into hand from Reserve Deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("An Effective Demonstration");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new EpicEventDestinyDrawModifier(self, self.getOwner(), Filters.and(Icon.A_NEW_HOPE, Filters.Epic_Event), new CommencePrimaryIgnitionTargetingCondition(Filters.Alderaan_system), 5));
        modifiers.add(new EpicEventDestinyDrawModifier(self, game.getOpponent(self.getOwner()), Filters.and(Icon.A_NEW_HOPE, Filters.Epic_Event), new CommencePrimaryIgnitionTargetingCondition(Filters.Alderaan_system), 5));
        modifiers.add(new IconModifier(self, Filters.Death_Star_system, new BlownAwayCondition(Filters.title(Title.Alderaan, true)), Icon.LIGHT_FORCE, 1));
        modifiers.add(new TotalBattleDestinyModifier(self, new BlownAwayCondition(Filters.title(Title.Alderaan, true)), -1, game.getOpponent(self.getOwner())));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        GameTextActionId gameTextActionId = GameTextActionId.AN_EFFECTIVE_DEMONSTRATION__UPLOAD_SUPERLASER;

        // Check condition(s)

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canTakeCardsIntoHandFromReserveDeck(game, playerId, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take Superlaser into hand from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new TakeCardIntoHandFromReserveDeckEffect(action, playerId, Filters.Superlaser, true));
            actions.add(action);
        }
        return actions;
    }
}

