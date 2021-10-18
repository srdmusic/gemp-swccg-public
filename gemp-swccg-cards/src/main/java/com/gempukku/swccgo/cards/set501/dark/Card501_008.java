package com.gempukku.swccgo.cards.set501.dark;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.common.*;
import com.gempukku.swccgo.filters.Filter;
import com.gempukku.swccgo.filters.Filters;
import com.gempukku.swccgo.game.PhysicalCard;
import com.gempukku.swccgo.game.SwccgGame;
import com.gempukku.swccgo.game.state.WhileInPlayData;
import com.gempukku.swccgo.logic.TriggerConditions;
import com.gempukku.swccgo.logic.actions.RequiredGameTextTriggerAction;
import com.gempukku.swccgo.logic.decisions.MultipleChoiceAwaitingDecision;
import com.gempukku.swccgo.logic.effects.AddUntilEndOfGameModifierEffect;
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.DestinyModifier;
import com.gempukku.swccgo.logic.modifiers.KeywordModifier;
import com.gempukku.swccgo.logic.modifiers.MayNotPlayModifier;
import com.gempukku.swccgo.logic.modifiers.Modifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Set: Set 17
 * Type: Epic Event
 * Title: Revenge Of The Sith
 */
public class Card501_008 extends AbstractEpicEventDeployable {
    public Card501_008() {
        super(Side.DARK, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.Revenge_Of_The_Sith);
        setGameText("Deploys on table only at start of game; choose an apprentice:" +
                "Maul: Deploy Desert Landing Site and They Will Be No Match For You." +
                "Dooku: Deploy Invisible Hand: Bridge and Evil Is Everywhere." +
                "Vader: Deploy Vader's Castle and I Am Your Father." +
                "For remainder of game, you may not deploy Dark Jedi except Sidious and your chosen apprentice. " +
                "A Sith Legend, Always Two There Are, Sidious, and your apprentice are destiny +2. " +
                "If a Jedi just lost from same location as your Dark Jedi, opponent loses 1 Force.");
        addIcon(Icon.VIRTUAL_SET_17);
        setTestingText("Revenge Of The Sith");
    }

    @Override
    protected boolean checkGameTextDeployRequirements(String playerId, SwccgGame game, PhysicalCard self, PlayCardOptionId playCardOptionId, boolean asReact) {
        return GameConditions.isDuringStartOfGame(game);
    }

    @Override
    protected List<RequiredGameTextTriggerAction> getGameTextRequiredAfterTriggers(SwccgGame game, EffectResult effectResult, final PhysicalCard self, int gameTextSourceCardId) {
        List<RequiredGameTextTriggerAction> actions = new ArrayList<>();

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

            actions.add(action);
        }

        String opponent = game.getOpponent(self.getOwner());

        // Check condition(s)
        if (TriggerConditions.justLostFromLocation(game, effectResult, Filters.Jedi, Filters.sameSiteAs(self, Filters.Dark_Jedi))) {

            final RequiredGameTextTriggerAction action = new RequiredGameTextTriggerAction(self, gameTextSourceCardId);
            action.setText("Make opponent lose 1 Force");
            // Perform result(s)
            action.appendEffect(
                    new LoseForceEffect(action, opponent, 1));
            actions.add(action);
        }

        return actions;
    }

    @Override
    protected List<Modifier> getGameTextWhileActiveInPlayModifiers(SwccgGame game, PhysicalCard self) {
        List<Modifier> modifiers = new ArrayList<>();
        modifiers.add(new MayNotPlayModifier(self, Filters.and(Filters.Dark_Jedi, Filters.not(Filters.Sidious), Filters.not(Filters.Sith_Apprentice)), self.getOwner()));
        modifiers.add(new DestinyModifier(self, Filters.or(Filters.A_Sith_Legend, Filters.Always_Two_There_Are, Filters.Sidious, Filters.Sith_Apprentice), 2));
        return modifiers;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        return "Chosen Apprentice is " + self.getWhileInPlayData().getTextValue();
    }
}
