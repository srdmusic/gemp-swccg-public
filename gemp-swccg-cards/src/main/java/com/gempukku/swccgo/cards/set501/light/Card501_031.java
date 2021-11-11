package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractNormalEffect;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.*;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfTurnModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DefenseValueModifier;
import com.gempukku.swccgo.logic.modifiers.ModifiersQuerying;
import com.gempukku.swccgo.logic.timing.Action;

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
        setGameText("Deploy on Rey's Encampment. Once per game, may lose 1 Force to target a card with Rey; target is defense value -4 for remainder of turn. Once per turn, may deploy Hidden Recess, Jakku, or Ravager Crash Site from Reserve Deck; reshuffle. [Immune to Alter.]");
        addIcons(Icon.EPISODE_VII, Icon.VIRTUAL_SET_17);
        addImmuneToCardTitle(Title.Alter);
        setTestingText("My Parents Were Strong");
    }

    @Override
    protected Filter getValidDeployTargetFilter(String playerId, SwccgGame game, PhysicalCard self, PhysicalCard sourceCard, PlayCardOption playCardOption, boolean forFree, float changeInCost, DeploymentRestrictionsOption deploymentRestrictionsOption, DeployAsCaptiveOption deployAsCaptiveOption, ReactActionOption reactActionOption, boolean isSimDeployAttached, boolean ignorePresenceOrForceIcons) {
        return Filters.Reys_Encampment;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.MY_PARENTS_WERE_STRONG__REY_FORCE_LIGHTNING;

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

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.canTarget(game, self, Filters.Rey)
                && GameConditions.canTarget(game, self, targetFilter)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Reduce defense value by 4");

            action.appendUsage(
                    new OncePerGameEffect(action));

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


        gameTextActionId = GameTextActionId.MY_PARENTS_WERE_STRONG__DOWNLOAD_LOCATION;

        // Check condition(s)
        if (GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)
                && (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Jakku)
                || GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Hidden_Recess)
                || GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, Title.Ravager_Crash_Site))) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy location from Reserve Deck");
            action.setActionMsg("Deploy Hidden Recess, Jakku, or Ravager Crash Site from Reserve Deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerTurnEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.or(Filters.Jakku_system, Filters.title(Title.Hidden_Recess), Filters.title(Title.Ravager_Crash_Site)), true));
            actions.add(action);
        }

        return actions;
    }
}
