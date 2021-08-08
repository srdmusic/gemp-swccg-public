package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.RetrieveCardIntoHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayBeTargetedByModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 16
 * Type: Effect
 * Title: Tragedy Of Plagueis
 */
public class Card501_090 extends AbstractNormalEffect {
    public Card501_090() {
        super(Side.DARK, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Tragedy Of Plagueis ", Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on table. Once per turn may deploy a lightsaber on Sidious. " +
                "If Sidious with a Dark Jedi, once per game may retrieve a character into hand. Sidious may be targeted by Force Lightning. [Immune to Alter.]");
        addIcons(Icon.EPISODE_I, Icon.VIRTUAL_SET_16);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("[Set 17] Tragedy Of Plagueis ");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new MayBeTargetedByModifier(self, Filters.Sidious, Title.Force_Lightning));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.TRAGEDY_OF_PLAGEUS__DOWNLOAD_LIGHTSABER_ON_SIDIOUS;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canSpot(game, self, Filters.Sidious)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a lightsaber on Sidious from Reserve Deck");
            action.setActionMsg("Deploy a lightsaber on Sidious from Reserve Deck");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));

            // Perform result(s)
            action.appendEffect(
                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.lightsaber, Filters.Sidious, true));

            actions.add(action);
        }

        if (GameConditions.canSpot(game, self, Filters.and(Filters.Sidious, Filters.with(self, Filters.Dark_Jedi)))
                && GameConditions.canSearchLostPile(game, playerId, self, gameTextActionId)
                && GameConditions.isOncePerGame(game, self, gameTextActionId)) {
            TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.appendEffect(
                    new RetrieveCardIntoHandEffect(action, playerId, Filters.character)
            );
            actions.add(action);
        }

        return actions;
    }
}

