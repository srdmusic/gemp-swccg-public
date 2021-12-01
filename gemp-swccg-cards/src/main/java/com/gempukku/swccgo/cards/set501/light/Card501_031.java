package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.StackCardsFromOutsideDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.TakeDestinyCardIntoHandEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.results.DestinyDrawnResult;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Effect
 * Title: My Parents Were Strong
 */
public class Card501_031 extends AbstractNormalEffect {
    public Card501_031() {
        super(Side.LIGHT, 4, PlayCardZoneOption.ATTACHED, Title.My_Parents_Were_Strong, Uniqueness.UNIQUE);
        setLore("");
        setGameText("Deploy on Training Course. When deployed, stack [Set 4] Falcon face up here from outside your deck; it may deploy from here as if from hand. Rey is a Skywalker. During battle with Rey, may take one of your just drawn destiny into hand. [Immune to Alter.]");
        addIcons(Icon.SKYWALKER, Icon.EPISODE_VII, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("My Parents Were Strong");
    }

    @Override
    protected Filter getValidDeployTargetFilter(String playerId, SwccgGame game, PhysicalCard self, PhysicalCard sourceCard, PlayCardOption playCardOption, boolean forFree, float changeInCost, DeploymentRestrictionsOption deploymentRestrictionsOption, DeployAsCaptiveOption deployAsCaptiveOption, ReactActionOption reactActionOption, boolean isSimDeployAttached, boolean ignorePresenceOrForceIcons) {
        return Filters.title("Ajan Kloss: Training Course");
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {

        if (TriggerConditions.justDeployed(game, effectResult, self)) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Stack [Set 4] Falcon here");
            action.setActionMsg("Stack [Set 4] Falcon here from outside your deck");
            action.appendEffect(
                    new StackCardsFromOutsideDeckEffect(action, self.getOwner(), 1, 1, self, false, Filters.and(Icon.VIRTUAL_SET_4, Filters.Falcon)));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayDeployAsIfFromHandModifier(self, Filters.and(Filters.stackedOn(self), Filters.Falcon)));
        modifiers.add(new KeywordModifier(self, Filters.Rey, Keyword.SKYWALKER));
        return modifiers;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (TriggerConditions.isDestinyJustDrawnBy(game, effectResult, playerId)
                && GameConditions.isOncePerBattle(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Rey)
                && GameConditions.canTakeDestinyCardIntoHand(game, playerId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Take destiny card into hand");
            action.setActionMsg("Take just drawn destiny card, " + GameUtils.getCardLink(((DestinyDrawnResult) effectResult).getCard()) + ", into hand");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerBattleEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new TakeDestinyCardIntoHandEffect(action));
            return Collections.singletonList(action);
        }
        return null;
    }
}
