package com.gempukku.swccgo.cards.set217.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.OnTableCondition;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.MayBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.PowerModifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Tragedy Of Plagueis
 */
public class Card217_023 extends AbstractNormalEffect {
    public Card217_023() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Tragedy Of Plagueis", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on table. While Sidious alone, Jedi with him are power and defense value -1. If Revenge Of The Sith on table, your Sidious may be targeted by Force Lightning. Once per game, if Sidious with a Dark Jedi, may retrieve a character into hand. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new PowerModifier(self, Filters.and(Filters.Jedi, Filters.with(self, Filters.Sidious)), new OnTableCondition(self, Filters.and(Filters.Sidious, Filters.alone)), -1));
        modifiers.add(new DefenseValueModifier(self, Filters.and(Filters.Jedi, Filters.with(self, Filters.Sidious)), new OnTableCondition(self, Filters.and(Filters.Sidious, Filters.alone)), -1));
        modifiers.add(new MayBeTargetedByModifier(self, Filters.and(Filters.your(self), Filters.Sidious), new OnTableCondition(self, Filters.title(Title.Revenge_Of_The_Sith)), Title.Force_Lightning));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.TRAGEDY_OF_PLAGUEIS__RETRIEVE_CHARACTER_INTO_HAND_FROM_LOST_PILE;

        if (GameConditions.canSpot(game, self, Filters.and(Filters.Sidious, Filters.with(self, Filters.Dark_Jedi)))
                && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve a character into hand");
            action.appendUsage(new OncePerGameEffect(action));
            action.appendEffect(
                    new RetrieveCardIntoHandEffect(action, playerId, Filters.character)
            );
            actions.add(action);
        }

        return actions;
    }
}

