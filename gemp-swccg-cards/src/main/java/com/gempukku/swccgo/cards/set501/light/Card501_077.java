package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.ShowCardOnScreenEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromHandEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckSimultaneouslyWithCardEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardToTargetFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DestinyModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 18
 * Type: Effect
 * Title: A Cunning Warrior
 */
public class Card501_077 extends AbstractNormalEffect {
    public Card501_077() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, "A Cunning Warrior", Uniqueness.UNIQUE);
        setGameText("Deploy on your [Skywalker] Epic Event at start of game; may deploy Lars' Moisture Farm. Once per turn, may deploy Lower Corridor or a weapon on your lone Skywalker from Reserve Deck; reshuffle. Courage Of A Skywalker and Higher Ground are destiny +2. [Immune to Alter.]");
        addIcons(Icon.SKYWALKER, Icon.VIRTUAL_SET_18);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("A Cunning Warrior");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.isDuringStartOfGame(game);
    }

    @Override
    protected Filter getGameTextValidDeployTargetFilter(SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.and(Filters.your(self), Icon.SKYWALKER, Filters.Epic_Event);
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new DestinyModifier(self, Filters.or(Filters.title("Courage Of A Skywalker"), Filters.title("Higher Ground")), 2));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_LARS_MOISTURE_FARM;
        if (TriggerConditions.justDeployed(game, effectResult, self)
            && GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Lars_Moisture_Farm, true)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Lars' Moisture Farm from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.Lars_Moisture_Farm, true));
            return Collections.singletonList(action);

        }

        return null;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.A_CUNNING_WARRIOR__DEPLOY_CARD;

        Filter loneSkywalkerFilter = Filters.and(Filters.your(self), Filters.alone, Filters.Skywalker);
        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Lower_Corridor)
                    || (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId)
                        && GameConditions.canTarget(game, self, loneSkywalkerFilter)))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy card from Reserve Deck");
            action.setActionMsg("Deploy Lower Corridor or a weapon on your lone Skywalker from Reserve Deck");

            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));

            action.appendEffect(new DeployCardToTargetFromReserveDeckEffect(action, Filters.or(Filters.title(Title.Lower_Corridor), Filters.weapon), loneSkywalkerFilter, Filters.title(Title.Lower_Corridor), Filters.none, false, true));

            actions.add(action);
        }

        return actions;
    }
}