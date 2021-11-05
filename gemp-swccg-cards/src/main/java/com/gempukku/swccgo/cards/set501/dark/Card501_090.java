package com.gempukku.swccgo.cards.set501.dark;

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
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: Tragedy Of Plagueis
 */
public class Card501_090 extends AbstractNormalEffect {
    public Card501_090() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Tragedy Of Plagueis", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on table. May deploy Sidious's Lightsaber from Reserve Deck; reshuffle. Once per game, If Sidious with a Dark Jedi, may retrieve a character into hand. If Revenge Of The Sith on table, Sidious may be targeted by Force Lightning. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Tragedy Of Plagueis ");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new MayBeTargetedByModifier(self, Filters.or(Filters.Sidious, Filters.Emperor), new OnTableCondition(self, Filters.title(Title.Revenge_Of_The_Sith)), Title.Force_Lightning));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.TRAGEDY_OF_PLAGUEIS__DOWNLOAD_LIGHTSABER_ON_SIDIOUS;

        // Check condition(s)
        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, "Sidious's Lightsaber")) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Sidious's Lightsaber");
            action.setActionMsg("Deploy Sidious's Lightsaber from Reserve Deck");

            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.title("Sidious's Lightsaber"), true));

            actions.add(action);
        }

        gameTextActionId = GameTextActionId.TRAGEDY_OF_PLAGUEIS__RETRIEVE_CHARACTER_INTO_HAND_FROM_LOST_PILE;

        if (GameConditions.canSpot(game, self, Filters.and(Filters.or(Filters.Sidious, Filters.Emperor), Filters.with(self, Filters.Dark_Jedi)))
                && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
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

