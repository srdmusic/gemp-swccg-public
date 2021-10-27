package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.conditions.AtCondition;
import com.gempukku.swccgo.cards.conditions.DuringPlayersTurnNumberCondition;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.GameState;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.GameUtils;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.*;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.ChooseCardsFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.*;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Epic Event
 * Title: The Force Is Strong In My Family
 */
public class Card501_028 extends AbstractEpicEventDeployable {
    public Card501_028() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.The_Force_Is_Strong_In_My_Family);
        setGameText("Deploy on table (only at start of game). Choose one: " +
                "My Father Has It: Reveal Anakin (may also reveal Obi-Wan) from Reserve Deck. \n" +
                "I Have It: Reveal Luke from Reserve Deck. \n" +
                "You Have That Power Too: Reveal Rey (may also reveal [Episode VII] Luke) from Reserve Deck. \n" +
                "Light Side goes first. During your first turn, you may not deploy cards with ability. You may not deploy Jedi (except Yoda and the revealed cards) or [Maintenance] cards. If Leia at a battleground site, flip Their Fire Has Gone Out Of The Universe (may not flip back).");
        addIcons(Icon.EPISODE_I, Icon.EPISODE_VII, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_17);
        setTestingText("The Force Is Strong In My Family");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.isDuringStartOfGame(game);
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new LinkedList<>();

        final String playerId = self.getOwner();
        if (TriggerConditions.justDeployed(game, effectResult, self)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setPerformingPlayer(self.getOwner());

            final String MY_FATHER_HAS_IT = "My Father Has It";
            final String I_HAVE_IT = "I Have It";
            final String YOU_HAVE_THAT_POWER_TOO = "You Have That Power Too";

            String[] possibleResults = new String[]{MY_FATHER_HAS_IT, I_HAVE_IT, YOU_HAVE_THAT_POWER_TOO};

            action.appendEffect(
                    new PlayoutDecisionEffect(action, self.getOwner(), new MultipleChoiceAwaitingDecision("Choose an option", possibleResults) {
                        @Override
                        protected void validDecisionMade(int index, final String result) {
                            Filter filter = null;
                            Filter alternateFilter = null;

                            switch (result) {
                                case MY_FATHER_HAS_IT:
                                    //Reveal Anakin (may also reveal Obi-Wan) from Reserve Deck.
                                    filter = Filters.Anakin;
                                    alternateFilter = Filters.ObiWan;
                                    break;
                                case I_HAVE_IT:
                                    //Reveal Luke from Reserve Deck.
                                    filter = Filters.Luke;
                                    alternateFilter = Filters.none;
                                    break;
                                case YOU_HAVE_THAT_POWER_TOO:
                                    //Reveal Rey (may also reveal [Episode VII] Luke) from Reserve Deck.
                                    filter = Filters.Rey;
                                    alternateFilter = Filters.and(Icon.EPISODE_VII, Filters.Luke);
                                    break;
                            }
                            action.appendEffect(
                                    new ChooseCardFromReserveDeckEffect(action, playerId, filter) {
                                        @Override
                                        protected void cardSelected(SwccgGame game, PhysicalCard selectedCard) {
                                            action.appendEffect(new ShowCardOnScreenEffect(action, selectedCard));
                                            action.appendEffect(new SendMessageEffect(action, self.getOwner() + " revealed " + GameUtils.getCardLink(selectedCard)
                                                + " with " + GameUtils.getCardLink(self)));

                                            self.setWhileInPlayData(new WhileInPlayData(result, Collections.singletonList(selectedCard)));
                                        }
                                    }
                            );
                            action.appendEffect(
                                    new ChooseCardsFromReserveDeckEffect(action, playerId, playerId, 0, 1, alternateFilter) {
                                        @Override
                                        protected void cardsSelected(SwccgGame game, Collection<PhysicalCard> selectedCards) {
                                            if(selectedCards.size()>0) {
                                                for(PhysicalCard c:selectedCards) {
                                                    action.appendEffect(new ShowCardOnScreenEffect(action, c));
                                                    action.appendEffect(new SendMessageEffect(action, self.getOwner() + " revealed " + GameUtils.getCardLink(c)
                                                            + " with " + GameUtils.getCardLink(self)));
                                                }
                                                List<PhysicalCard> cards = new LinkedList<>();
                                                cards.addAll(self.getWhileInPlayData().getPhysicalCards());
                                                cards.addAll(selectedCards);

                                                self.setWhileInPlayData(new WhileInPlayData(result, cards));
                                            }
                                        }
                                    }
                            );
                            action.appendEffect(
                                    new LightSideGoesFirstEffect(action));
                        }
                    })
            );

            actions.add(action);
        }

        // Check condition(s)
        if (TriggerConditions.isTableChanged(game, effectResult)
                && GameConditions.canSpot(game, self, Filters.and(Filters.Leia, Filters.presentAt(Filters.battleground)))) {
            PhysicalCard theirFireHasGoneOutOfTheUniverse = Filters.findFirstActive(game, self, Filters.Their_Fire_Has_Gone_Out_Of_The_Universe);
            if (theirFireHasGoneOutOfTheUniverse != null
                    && GameConditions.canBeFlipped(game, theirFireHasGoneOutOfTheUniverse)) {

                RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
                action.setSingletonTrigger(true);
                action.setText("Flip " + GameUtils.getFullName(theirFireHasGoneOutOfTheUniverse));
                action.setActionMsg("Flip " + GameUtils.getCardLink(theirFireHasGoneOutOfTheUniverse));
                // Perform result(s)
                action.appendEffect(
                        new FlipCardEffect(action, theirFireHasGoneOutOfTheUniverse));
                actions.add(action);
            }
        }

        return actions;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, final PhysicalCard self) {
        Filter revealedCardsFilter = new Filter() {
            @Override
            public boolean accepts(GameState gameState, ModifiersQuerying modifiersQuerying, PhysicalCard physicalCard) {
                List<PhysicalCard> cardsInData = self.getWhileInPlayData() != null ? self.getWhileInPlayData().getPhysicalCards() : null;
                if (cardsInData != null) {
                    for (PhysicalCard c: cardsInData) {
                        if (Filters.sameTitle(physicalCard).accepts(gameState, modifiersQuerying, c))
                            return true;
                    }
                }
                return false;
            }
        };

        List<Modifier> modifiers = new LinkedList<>();
        modifiers.add(new MayNotDeployModifier(self, Filters.hasAbilityOrHasPermanentPilotWithAbility, new DuringPlayersTurnNumberCondition(self.getOwner(), 1), self.getOwner()));
        modifiers.add(new MayNotDeployModifier(self,
                Filters.or(Filters.and(Filters.Jedi, Filters.except(Filters.or(Filters.Yoda, revealedCardsFilter))),
                        Filters.icon(Icon.MAINTENANCE)), self.getOwner()));
        modifiers.add(new MayNotBeFlippedModifier(self, new AtCondition(self, Filters.Leia, Filters.battleground_site), Filters.Hunt_Down_And_Destroy_The_Jedi));
        return modifiers;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        if(self.getWhileInPlayData()==null)
            return null;

        return "Chosen option: " + self.getWhileInPlayData().getTextValue();
    }
}