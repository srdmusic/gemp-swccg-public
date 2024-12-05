package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.PayRelocateBetweenLocationsCostEffect;
import com.gempukku.swccgo.cards.effects.PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerPhaseEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerTurnEffect;
import com.gempukku.swccgo.common.ExpansionSet;
import com.gempukku.swccgo.common.GameTextActionId;
import com.gempukku.swccgo.common.Icon;
import com.gempukku.swccgo.common.Phase;
import com.gempukku.swccgo.common.PlayCardOptionId;
import com.gempukku.swccgo.common.PlayCardZoneOption;
import com.gempukku.swccgo.common.Rarity;
import com.gempukku.swccgo.common.Side;
import com.gempukku.swccgo.common.Uniqueness;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.OptionalGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.effects.FlipSingleSidedStackedCard;
import com.gempukku.swccgo.logic.effects.RelocateBetweenLocationsEffect;
import com.gempukku.swccgo.logic.effects.RespondableEffect;
import com.gempukku.swccgo.logic.effects.SendMessageEffect;
import com.gempukku.swccgo.logic.effects.ShuffleReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.TargetCardOnTableEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseStackedCardEffect;
import com.gempukku.swccgo.logic.effects.choose.PlayInterruptFromOutsideTheGameEffect;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.modifiers.NoForceLossFromCardModifier;
import com.gempukku.swccgo.logic.timing.Action;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Playtesting
 * Type: Epic Event
 * Title: His Destiny
 */
public class Card501_166 extends AbstractEpicEventDeployable {
    public Card501_166() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, "His Destiny", Uniqueness.UNIQUE, ExpansionSet.PLAYTESTING, Rarity.V);
        setGameText("If He Is The Chosen One on table, deploy on table. " +
                "Indeed You Are Powerful: Once per turn, if you just won a battle or duel, may peek at the top two cards of your Reserve Deck; take one into hand and shuffle your Reserve Deck. " +
                "I Have To Face Him: During your move phase, may turn a card stacked on I Feel The Conflict face up to relocate Luke between a battleground site and your [Endor] or [Death Star II] site. " +
                "Father, Please!: Non-[Set 13] Anakin Skywalker does not cause Force loss and, once per game, you may play it from outside your deck.");
        addIcons(Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_21);
        setTestingText("His Destiny (ERRATA)");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return Filters.canSpot(game, self, Filters.He_Is_The_Chosen_One);
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new ArrayList<>();

        // Relocate Luke
        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        final Filter battlegroundFilter = Filters.battleground_site;
        final Filter dsiisiteFilter = Filters.and(Filters.your(self), Filters.Death_Star_II_site);
        final Filter lukeFilter = Filters.Luke;

        if (GameConditions.isOnceDuringYourPhase(game, self, playerId, gameTextSourceCardId, gameTextActionId, Phase.MOVE)
                && !Filters.filterStacked(game, Filters.and(Filters.stackedOn(self, Filters.I_Feel_The_Conflict), Filters.face_down)).isEmpty()) {

            if (GameConditions.canTarget(game, self, Filters.and(lukeFilter, Filters.at(battlegroundFilter), Filters.canBeRelocatedToLocation(dsiisiteFilter, 0)))) {
                actions.add(getRelocationAction(playerId, game, self, gameTextSourceCardId, gameTextActionId, "Relocate Luke to a Death Star II site", battlegroundFilter, dsiisiteFilter, lukeFilter));
            }

            if (GameConditions.canTarget(game, self, Filters.and(lukeFilter, Filters.at(dsiisiteFilter), Filters.canBeRelocatedToLocation(battlegroundFilter, 0)))) {
                actions.add(getRelocationAction(playerId, game, self, gameTextSourceCardId, gameTextActionId, "Relocate Luke to a battleground site", dsiisiteFilter, battlegroundFilter, lukeFilter));
            }
        }

        return actions;
    }

    @Override
    protected List<OptionalGameTextTriggerAction> getGameTextOptionalAfterTriggers(final String playerId, SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<OptionalGameTextTriggerAction> actions = new LinkedList<>();

        GameTextActionId gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;
        if((TriggerConditions.wonBattle(game, effectResult, playerId) || TriggerConditions.wonDuel(game, effectResult, playerId))
                && GameConditions.hasReserveDeck(game, playerId)
                && GameConditions.isOncePerTurn(game, self, playerId, gameTextSourceCardId, gameTextActionId)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, playerId, gameTextSourceCardId, gameTextActionId);
            action.setText("Peek at top two cards of Reserve Deck");
            action.setActionMsg("Peek at top two cards of Reserve Deck, take one into hand, and shuffle your Reserve Deck");

            action.appendUsage(
                    new OncePerTurnEffect(action));
            boolean twoCardsInReserve = GameConditions.numCardsInReserveDeck(game, playerId) >= 2;
            action.appendEffect(
                    new PeekAtTopCardsOfReserveDeckAndChooseCardsToTakeIntoHandEffect(action, playerId, 2, 1, 1));
            if (twoCardsInReserve) {
                action.appendEffect(
                        new ShuffleReserveDeckEffect(action, playerId));
            }
            actions.add(action);
        }

        GameTextActionId gameTextActionId2 = GameTextActionId.HIS_DESTINY__PLAY_ANAKIN_SKYWALKER_FROM_OUTSIDE_OF_DECK;
        // Check condition(s)
        if (GameConditions.isOncePerGame(game, self, gameTextActionId2)
                && GameConditions.canPlayInterruptAsResponseFromOutsideOfDeck(game, playerId, self, effectResult, gameTextActionId2)) {

            final OptionalGameTextTriggerAction action = new OptionalGameTextTriggerAction(self, gameTextSourceCardId, gameTextActionId2);
            action.setText("Play Anakin Skywalker from outside your deck");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Send Easter Egg Message (copied from Scomp Link Access V)
            action.appendCost(
                new SendMessageEffect(action, "Luke: Father, please!"));
            // Perform result(s)
            action.appendEffect(
                    new PlayInterruptFromOutsideTheGameEffect(action, Filters.and(Filters.Anakin_Skywalker, Filters.not(Icon.VIRTUAL_SET_13)), effectResult, false));
            actions.add(action);
        }

        return actions;
    }

    private TopLevelGameTextAction getRelocationAction(final String playerId, final SwccgGame game, final PhysicalCard self, int gameTextSourceCardId, GameTextActionId gameTextActionId, String actionText, final Filter currentLocationFilter, final Filter relocateLocationFilter, final Filter characterFilter) {
        final TopLevelGameTextAction action = new TopLevelGameTextAction(self, playerId, gameTextSourceCardId, gameTextActionId);
        action.setText(actionText);

        action.appendUsage(
                new OncePerPhaseEffect(action));
        // Choose target(s)
        action.appendTargeting(
                new TargetCardOnTableEffect(action, playerId, "Choose character", Filters.and(characterFilter, Filters.at(currentLocationFilter), Filters.canBeRelocatedToLocation(relocateLocationFilter, 0))) {
                    @Override
                    protected void cardTargeted(final int targetGroupIdCharacter, final PhysicalCard characterToRelocate) {
                        action.appendTargeting(
                                new TargetCardOnTableEffect(action, playerId, "Choose site to relocate " + GameUtils.getCardLink(characterToRelocate) + " to",
                                        Filters.and(relocateLocationFilter, Filters.locationCanBeRelocatedTo(characterToRelocate, 0))) {
                                    @Override
                                    protected void cardTargeted(final int targetGroupIdSite, final PhysicalCard siteSelected) {
                                        action.appendTargeting(new ChooseStackedCardEffect(action, playerId, Filters.I_Feel_The_Conflict, Filters.face_down, true) {
                                            @Override
                                            protected void cardSelected(PhysicalCard selectedCard) {
                                                action.addAnimationGroup(characterToRelocate);
                                                action.addAnimationGroup(siteSelected);

                                                // Pay cost(s)
                                                action.appendCost(
                                                        new FlipSingleSidedStackedCard(action, selectedCard));
                                                action.appendCost(
                                                        new PayRelocateBetweenLocationsCostEffect(action, playerId, characterToRelocate, siteSelected, 0));
                                                action.allowResponses(new RespondableEffect(action) {
                                                    @Override
                                                    protected void performActionResults(Action targetingAction) {
                                                        PhysicalCard finalSite = action.getPrimaryTargetCard(targetGroupIdSite);
                                                        PhysicalCard finalCharacter = action.getPrimaryTargetCard(targetGroupIdCharacter);

                                                        // Perform result(s)
                                                        action.appendEffect(
                                                                new RelocateBetweenLocationsEffect(action, finalCharacter, finalSite));
                                                    }
                                                });
                                            }
                                        });
                                    }

                                }
                        );
                    }
                }
        );
        return action;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        String playerId = self.getOwner();
        String opponent = game.getOpponent(playerId);

        List<Modifier> modifiers = new LinkedList<Modifier>();
        modifiers.add(new NoForceLossFromCardModifier(self, Filters.and(Filters.Anakin_Skywalker, Filters.not(Icon.VIRTUAL_SET_13)), opponent));
        return modifiers;
    }


}