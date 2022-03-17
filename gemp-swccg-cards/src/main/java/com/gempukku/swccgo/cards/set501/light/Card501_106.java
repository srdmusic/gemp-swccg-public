package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: Phoenix Squadron
 */
public class Card501_106 extends AbstractNormalEffect {
    public Card501_106() {
        super(Side.LIGHT, 4, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "Phoenix Squadron", Uniqueness.UNIQUE);
        setGameText("Deploy on table. Chopper, Ezra, Hera, Kanan, Sabine, and Zeb are Phoenix Squadron members. Once per turn, may deploy Hobbie, Wedge or an A-wing to a Lothal location from Reserve Deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.VIRTUAL_SET_18);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("Phoenix Squadron");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new KeywordModifier(self, Filters.or(Filters.Chopper, Filters.Ezra, Filters.Hera, Filters.Kanan, Filters.Sabine, Filters.Zeb), Keyword.PHOENIX_SQUADRON));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.PHOENIX_SQUADRON__DOWNLOAD_CARD;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                && GameConditions.canSpotLocation(game, Filters.Lothal_location)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Hobbie, Wedge, or an A-wing");
            action.setActionMsg("Deploy Hobbie, Wedge, or an A-wing to a Lothal location from Reserve Deck");
            action.appendUsage(
                    new OncePerTurnEffect(action));
            action.appendEffect(
                    new DeployCardToTargetFromReserveDeckEffect(action, Filters.or(Filters.Hobbie, Filters.Wedge, Filters.A_wing), Filters.locationAndCardsAtLocation(Filters.Lothal_location), true));

            return Collections.singletonList(action);
        }
        return null;
    }
}