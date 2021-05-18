package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.ImmuneToTitleModifier;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 15
 * Type: Effect
 * Title: Cell 2187 (V)
 */
public class Card501_043 extends AbstractNormalEffect {
    public Card501_043() {
        super(Side.LIGHT, 5, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Cell_2187, Uniqueness.UNIQUE);
        setLore("");
        setGameText("If Leia imprisoned, deploy on table. [Set 8] Luke is a spy and stormtrooper. Han, Leia, and Luke are immune to Nevar Yalnal and Put All Sections On Alert. Once per turn, may deploy a Death Star site from Reserve Deck; reshuffle. Immune to Alter and This Is Some Rescue!");
        addIcons(Icon.A_NEW_HOPE, Icon.VIRTUAL_SET_15);
        setVirtualSuffix(true);
        addImmuneToCardTitle(Title.Alter);
        addImmuneToCardTitle(Title.This_Is_Some_Rescue);
        setTestingText("Cell 2187 (V)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return !Filters.canSpot(game, self, Filters.and(Filters.Leia, Filters.imprisoned));
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        Filter set8Luke = Filters.and(Filters.Luke, Icon.VIRTUAL_SET_8);
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new KeywordModifier(self, set8Luke, Keyword.SPY));
        modifiers.add(new KeywordModifier(self, set8Luke, Keyword.STORMTROOPER));
        Filter hanLeiaAndLuke = Filters.or(Filters.Han, Filters.Leia, Filters.Luke);
        modifiers.add(new ImmuneToTitleModifier(self, hanLeiaAndLuke, Title.Nevar_Yalnal));
        modifiers.add(new ImmuneToTitleModifier(self, hanLeiaAndLuke, Title.Put_All_Sections_On_Alert));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.CELL_2187__DOWNLOAD_DEATH_STAR_SITE;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy a Death Star site from Reserve Deck");
            action.appendUsage(
                    new OncePerTurnEffect(action)
            );
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Death_Star_site, true)
            );

            actions.add(action);
        }

        return actions;
    }
}