package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractDroid;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DrawCardIntoHandFromUsedPileEffect;
import com.gempukku.swccgo.logic.modifiers.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Character
 * Subtype: Droid
 * Title: C-3PO (See-Threepio) (V)
 */
public class Card501_025 extends AbstractDroid {
    public Card501_025() {
        super(Side.LIGHT, 3, 2, 1, 4, "C-3PO (See-Threepio)", Uniqueness.UNIQUE);
        setLore("Cybot Galactica 3PO human-cyborg relations droid. Fluent in over six million forms of communication. 112 years old. Has never been memory-wiped... as far as he knows.");
        setGameText("At battleground sites where you have a droid and a Rebel, opponent may not cancel or modify your Force drains. If on Death Star, cards from A Power Loss go to owner’s Lost Pile instead of Used Pile. Once per game may draw top card of Used Pile.");
        addPersona(Persona.C3PO);
        addModelType(ModelType.PROTOCOL);
        addIcon(Icon.VIRTUAL_SET_15);
        setTestingText("C-3PO (V)");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        Filter location = Filters.and(Filters.battleground_site, Filters.sameLocationAs(self, Filters.droid), Filters.sameLocationAs(self, Filters.Rebel));
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new ForceDrainsMayNotBeCanceledModifier(self, location, opponent, playerId));
        modifiers.add(new ForceDrainsMayNotBeModifiedModifier(self, location, opponent, playerId));
        modifiers.add(new ModifyGameTextModifier(self, Filters.A_Power_Loss, new OnCondition(self, Title.Death_Star), ModifyGameTextType.A_POWER_LOSS__CARDS_GO_LOST_INSTEAD_OF_USED));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        // Card action 1
        GameTextActionId gameTextActionId = GameTextActionId.C_3PO_V_DRAW_TOP_CARD_OF_USED_PILE;

        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.hasUsedPile(game, playerId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Draw top card of Used Pile");
            action.setActionMsg("Draw top card of Used Pile");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DrawCardIntoHandFromUsedPileEffect(action, playerId));
            actions.add(action);
        }

        return actions;
    }
}
