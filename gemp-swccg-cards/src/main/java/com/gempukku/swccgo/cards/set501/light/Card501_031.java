package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.StackCardsFromOutsideDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.MayDeployAsIfFromHandModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

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
        setGameText("Deploy on Training Course. When deployed, stack [Set 4] Falcon face up here from outside your deck; it may deploy from here as if from hand. Once per turn, may lose 1 Force to target a card with Rey; target is defense value -4 for remainder of turn. [Immune to Alter.]");
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
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        Filter hasDefenseValue = new Filter() {
            @Override
            public boolean accepts(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard physicalCard) {
                return physicalCard.getBlueprint().hasAbilityAttribute()
                        || physicalCard.getBlueprint().hasArmorAttribute()
                        || physicalCard.getBlueprint().hasManeuverAttribute()
                        || physicalCard.getBlueprint().hasSpecialDefenseValueAttribute()
                        ;
            }
        };

        Filter targetFilter = Filters.and(Filters.with(self, Filters.Rey), hasDefenseValue);

        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && GameConditions.canTarget(game, self, Filters.Rey)
                && GameConditions.canTarget(game, self, targetFilter)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Reduce defense value by 4");

            action.appendUsage(
                    new OncePerTurnEffect(action));

            action.appendTargeting(new TargetCardOnTableEffect(action, playerId, "Choose target to reduce its defense value by 4 this turn", targetFilter) {
                @Override
                protected void cardTargeted(final int targetGroupId, PhysicalCard targetedCard) {
                    action.appendCost(
                            new LoseForceEffect(action, playerId, 1));
                    action.allowResponses(new RespondableEffect(action) {
                        @Override
                        protected void performActionResults(Action targetingAction) {
                            PhysicalCard finalTarget = action.getPrimaryTargetCard(targetGroupId);
                            action.appendEffect(
                                    new AddUntilEndOfTurnModifierEffect(action, new DefenseValueModifier(self, finalTarget, -4), "Reduce defense value by 4"));
                        }
                    });
                }
            });

            actions.add(action);
        }

        return actions;
    }
}
