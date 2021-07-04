package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerGameEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.actions.TopLevelGameTextAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.effects.choose.ExchangeCardsInHandWithCardInLostPileEffect;
import com.gempukku.swccgo.logic.modifiers.DestinyModifier;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotPlayModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Epic Event
 * Title: Rule Of Two
 */
public class Card501_008 extends AbstractEpicEventDeployable {
    public Card501_008() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Rule_Of_Two);
        setGameText("Deploy on table at start of game. Choose an apprentice:" +
                "Maul: Deploy Desert Landing Site and They Will Be No Match For You." +
                "Dooku: Deploy Invisible Hand: Bridge and Evil Is Everywhere." +
                "Vader: Deploy Vader's Castle and I Am Your Father." +
                "For remainder of game, you may not deploy Dark Jedi except Sidious and your chosen apprentice. Always Two There Are," +
                "A Sith Legend and your apprentice are destiny +2. Once per game may exchange 2 cards from hand with one character from Lost Pile.");
        addIcon(Icon.VIRTUAL_SET_16);
        setTestingText("Rule Of Two");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.isDuringStartOfGame(game);
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        if (TriggerConditions.justDeployed(game, effectResult, self)) {
            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setPerformingPlayer(self.getOwner());

            final String MAUL = "Maul";
            final String DOOKU = "Dooku";
            final String VADER = "Vader";

            String[] possibleResults = new String[]{MAUL, DOOKU, VADER};

            action.appendTargeting(
                    new PlayoutDecisionEffect(action, self.getOwner(), new MultipleChoiceAwaitingDecision("Choose an apprentice", possibleResults) {
                        @Override
                        protected void validDecisionMade(int index, String result) {
                            Filter siteFilter = null;
                            Filter effectFilter = null;
                            Filter apprenticeFilter = null;

                            switch (result) {
                                case MAUL:
                                    siteFilter = Filters.Desert_Landing_Site;
                                    effectFilter = Filters.They_Will_Be_No_Match_For_You;
                                    apprenticeFilter = Filters.Maul;
                                    break;
                                case DOOKU:
                                    siteFilter = Filters.Invisible_Hand_Bridge;
                                    effectFilter = Filters.Evil_Is_Everywhere;
                                    apprenticeFilter = Filters.Dooku;
                                    break;
                                case VADER:
                                    siteFilter = Filters.Vaders_Castle;
                                    effectFilter = Filters.I_Am_Your_Father;
                                    apprenticeFilter = Filters.Vader;
                                    break;
                            }
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, siteFilter, false)
                            );
                            action.appendEffect(
                                    new DeployCardFromReserveDeckEffect(action, effectFilter, false)
                            );
                            action.appendEffect(
                                    new AddUntilEndOfGameModifierEffect(action,
                                            new KeywordModifier(self, apprenticeFilter, Keyword.SITH_APPRENTICE), " chooses " + result + " as the apprentice")
                            );
                            action.appendEffect(
                                    new SetWhileInPlayDataEffect(action, self, new WhileInPlayData(result))
                            );
                        }
                    })
            );

            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new MayNotPlayModifier(self, Filters.and(Filters.Dark_Jedi, Filters.not(Filters.Sidious), Filters.not(Filters.Sith_Apprentice)), self.getOwner()));
        modifiers.add(new DestinyModifier(self, Filters.or(Filters.Always_Two_There_Are, Filters.A_Sith_Legend, Filters.Sith_Apprentice), 2));
        return modifiers;
    }

    @Override
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        //Once per game may exchange 2 cards from hand with one character from Lost Pile.
        GameTextActionId gameTextActionId = GameTextActionId.RULE_OF_TWO__EXCHANGE_CARDS;

        if (GameConditions.isOncePerGame(game, self, gameTextActionId)
                && GameConditions.numCardsInHand(game, playerId) >= 2
                && GameConditions.hasLostPile(game, playerId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Exchange cards with character in Lost Pile");
            action.setActionMsg("Exchange two cards in hand with a character in Lost Pile");
            // Update usage limit(s)
            action.appendUsage(
                    new OncePerGameEffect(action));
            // Perform result(s)
            action.appendEffect(
                    new ExchangeCardsInHandWithCardInLostPileEffect(action, playerId, 2, 2, Filters.character));
            return Collections.singletonList(action);
        }

        return null;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        return "Chosen Apprentice is " + self.getWhileInPlayData().getTextValue();
    }
}
