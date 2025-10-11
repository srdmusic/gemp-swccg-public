package com.gempukku.swccgo.cards.set501.light;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import com.gempukku.swccgo.cards.AbstractObjective;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.evaluators.ConditionEvaluator;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Title;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.conditions.InBattleCondition;
import com.gempukku.swccgo.logic.effects.PlaceCardInUsedPileFromTableEffect;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.UnrespondableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardOnTableEffect;
import com.gempukku.swccgo.logic.modifiers.ForceDrainModifiersMayNotBeCanceledModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.TotalBattleDestinyModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;
import com.gempukku.swccgo.logic.timing.PassthruEffect;
import com.gempukku.swccgo.logic.timing.results.AboutToLeaveTableResult;
import com.gempukku.swccgo.logic.timing.Action;

/**
 * Set: Playtesting
 * Type: Objective
 * Title: The Hidden Path / Gather Allies And Train
 */

public class Card501_201_BACK extends AbstractObjective {
    public Card501_201_BACK() {
        super(Side.LIGHT, 7, Title.Gather_Allies_And_Train, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("While this side up, Force drain bonuses from your lightsabers may not be canceled. Opponent's total battle destiny where they have a character of ability > 4 is -1 (-2 if an Inquisitor). If your holocron is about to leave table, place it in Used Pile. When you initiate battle with a Jedi, may retrieve 1 Force. During your move phase, may relocate a Jedi between a Jabiim site and a battleground site as a regular move. At the end of your turn, opponent loses 1 Force. Flip this card if Jedi do not occupy two locations.");
        addIcons(Icon.VIRTUAL_SET_26);
        setTestingText("Gather Allies And Train");
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new LinkedList<Modifier>();

        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);
        
        modifiers.add(new ForceDrainModifiersMayNotBeCanceledModifier(self, Filters.and(Filters.your(playerId), Filters.lightsaber)));

        Filter opponentsHighAbilityCharacter = Filters.and(Filters.opponents(self), Filters.character, Filters.abilityMoreThan(4));
        Filter opponentsHighAbilityInquisitor = Filters.and(opponentsHighAbilityCharacter, Filters.inquisitor);

        modifiers.add(new TotalBattleDestinyModifier(self, 
                new InBattleCondition(self, opponentsHighAbilityCharacter),
                new ConditionEvaluator(-1, -2, new InBattleCondition(self, opponentsHighAbilityInquisitor)),
                opponent, true));

        return modifiers;
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();
        String playerId = self.getOwner();

        // Check condition(s)
        if (TriggerConditions.isAboutToLeaveTable(game, effectResult, Filters.and(Filters.your(playerId), Filters.holocron))) {
            final AboutToLeaveTableResult result = (AboutToLeaveTableResult) effectResult;
            final PhysicalCard cardToLeaveTable = result.getCardAboutToLeaveTable();

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Place " + GameUtils.getFullName(cardToLeaveTable) + " in Used Pile");

            // Perform result(s)
            action.appendEffect(
                    new PassthruEffect(action) {
                        @Override
                        protected void doPlayEffect(SwccgGame game) {
                            result.getPreventableCardEffect().preventEffectOnCard(cardToLeaveTable);
                            action.appendEffect(
                                    new PlaceCardInUsedPileFromTableEffect(action, result.getCardAboutToLeaveTable()));
                        }
                    });
            actions.add(action);
        }
        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(String playerId, SwccgGame game, final EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        // Check condition(s)
        if (TriggerConditions.battleInitiated(game, effectResult, playerId)
                && GameConditions.isDuringBattleWithParticipant(game, Filters.Jedi)) {
            
            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Retrieve 1 Force");
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(action, playerId, 1));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new LinkedList<TopLevelGameTextAction>();

        // A Jedi who can relocate from a battleground site to a Jabiim site
        Filter jediValidFromBattleground = Filters.and(Filters.Jedi, Filters.presentAt(Filters.battleground_site), Filters.canBeRelocatedToLocation(Filters.Jabiim_site, true, 0), Filters.hasNotPerformedRegularMove);
        // A Jedi who can relocate from a Jabiim site to a battleground site
        Filter jediValidFromJabiim = Filters.and(Filters.Jedi, Filters.presentAt(Filters.Jabiim_site), Filters.canBeRelocatedToLocation(Filters.battleground_site, true, 0), Filters.hasNotPerformedRegularMove);

        Filter jediValidToRelocate = Filters.or(jediValidFromBattleground, jediValidFromJabiim);

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        // Check condition(s)
        if (GameConditions.canSpot(game, self, jediValidToRelocate)
                && GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.MOVE)) {

            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Relocate a Jedi");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerPhaseEffect(action));
            // Perform result(s)
            action.appendTargeting(
                    new ChooseCardOnTableEffect(action, playerId, "Choose Jedi to relocate", jediValidToRelocate) {
                        @Override
                        protected void cardSelected(final PhysicalCard jediSelected) {
                            Filter validDestination = Filters.none;
                            if (jediValidFromBattleground.accepts(game, jediSelected) && jediValidFromJabiim.accepts(game, jediSelected)) {
                                validDestination = Filters.or(Filters.battleground_site, Filters.Jabiim_site);
                            }
                            else if (jediValidFromBattleground.accepts(game, jediSelected)) {
                                validDestination = Filters.Jabiim_site;
                            }
                            else if (jediValidFromJabiim.accepts(game, jediSelected)) {
                                validDestination = Filters.battleground_site;
                            }

                            Collection<PhysicalCard> destinationSites = Filters.filterTopLocationsOnTable(game, Filters.and(validDestination, Filters.locationCanBeRelocatedTo(jediSelected, true, 0)));

                            action.appendTargeting(
                                    new ChooseCardOnTableEffect(action, playerId, "Choose site to relocate " + GameUtils.getCardLink(jediSelected) + " to", Filters.in(destinationSites)) {
                                        @Override
                                        protected void cardSelected(final PhysicalCard siteSelected) {
                                            action.addAnimationGroup(self);
                                            action.addAnimationGroup(jediSelected);
                                            // Allow response(s)
                                            action.allowResponses("Relocate " + GameUtils.getCardLink(jediSelected) + " to " + GameUtils.getCardLink(siteSelected),
                                                    new UnrespondableEffect(action) {
                                                        @Override
                                                        protected void performActionResults(Action targetingAction) {
                                                            // Perform result(s)
                                                            action.appendEffect(
                                                                    new RelocateBetweenLocationsEffect(action, jediSelected, siteSelected, true));
                                                        }
                                                    }
                                            );
                                        }
                                    }
                            );
                        }
                    }
            );
            actions.add(action);
        }
        return actions;
    }
}
