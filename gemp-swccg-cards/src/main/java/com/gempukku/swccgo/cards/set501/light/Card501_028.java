package com.gempukku.swccgo.cards.set501.light;

import com.gempukku.swccgo.cards.AbstractEpicEventDeployable;
import com.gempukku.swccgo.cards.GameConditions;
import com.gempukku.swccgo.cards.effects.SetWhileInPlayDataEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerBattleEffect;
import com.gempukku.swccgo.cards.effects.usage.OncePerForceDrainEffect;
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
import com.gempukku.swccgo.logic.effects.LoseForceEffect;
import com.gempukku.swccgo.logic.effects.PlayoutDecisionEffect;
import com.gempukku.swccgo.logic.effects.RetrieveForceEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromLostPileEffect;
import com.gempukku.swccgo.logic.effects.choose.DeployCardFromReserveDeckEffect;
import com.gempukku.swccgo.logic.modifiers.MayNotPlayModifier;
import com.gempukku.swccgo.logic.timing.EffectResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Set: Set 16
 * Type: Epic Event
 * Title: The Force Is Strong In My Family
 */
public class Card501_028 extends AbstractEpicEventDeployable {
    public Card501_028() {
        super(Side.LIGHT, PlayCardZoneOption.YOUR_SIDE_OF_TABLE, Title.The_Force_Is_Strong_In_My_Family);
        setGameText("Deploys on table only at start of game; choose one:" +
                "My Father Has It: Deploy Slave Quarters and Your Thoughts Dwell On Your Mother. You may not deploy Jedi (except [Episode I] Jedi)." +
                "I Have It: Deploy Chief Chirpa's Hut and Like My Father Before Me. You may not deploy Jedi (except Luke)." +
                "You Have That Power Too: Deploy Rey's Encampment and My Parents Were Strong. You may not deploy Jedi (except [Episode VII] Jedi)." +
                "Now It Calls To You: May deploy Anakin’s Lightsaber from Reserve Deck (reshuffle) or Lost Pile (lose 1 Force from Life Force). " +
                "If Anakin's Lightsaber present during a battle or Force drain at Lars’ Moisture Farm, may retrieve 1 Force.");
        addIcons(Icon.EPISODE_I, Icon.EPISODE_VII, Icon.DEATH_STAR_II, Icon.VIRTUAL_SET_16);
        setTestingText("The Force Is Strong In My Family");
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

            final String MY_FATHER_HAS_IT = "My Father Has It";
            final String I_HAVE_IT = "I Have It";
            final String YOU_HAVE_THAT_POWER_TOO = "You Have That Power Too";

            String[] possibleResults = new String[]{MY_FATHER_HAS_IT, I_HAVE_IT, YOU_HAVE_THAT_POWER_TOO};

            action.appendTargeting(
                    new PlayoutDecisionEffect(action, self.getOwner(), new MultipleChoiceAwaitingDecision("Choose an option", possibleResults) {
                        @Override
                        protected void validDecisionMade(int index, String result) {
                            Filter siteFilter = null;
                            Filter effectFilter = null;
                            Filter cardsThatMayNotDeployFilter = null;

                            switch (result) {
                                case MY_FATHER_HAS_IT:
                                    //Deploy Slave Quarters and Your Thoughts Dwell On Your Mother. You may not deploy Jedi (except [Episode I] Jedi)
                                    siteFilter = Filters.Slave_Quarters;
                                    effectFilter = Filters.title(Title.Your_Thoughts_Dwell_On_Your_Mother);
                                    cardsThatMayNotDeployFilter = Filters.and(Filters.Jedi, Filters.except(Icon.EPISODE_I));
                                    break;
                                case I_HAVE_IT:
                                    //Deploy Chief Chirpa's Hut and Like My Father Before Me. You may not deploy Jedi (except Luke).
                                    siteFilter = Filters.Chief_Chirpas_Hut;
                                    effectFilter = Filters.title(Title.Like_My_Father_Before_Me);
                                    cardsThatMayNotDeployFilter = Filters.and(Filters.Jedi, Filters.except(Filters.Luke));
                                    break;
                                case YOU_HAVE_THAT_POWER_TOO:
                                    //Deploy Rey's Encampment and My Parents Were Strong. You may not deploy Jedi (except [Episode VII] Jedi).
                                    siteFilter = Filters.Im_Here_To_Rescue_You;
                                    effectFilter = Filters.title(Title.My_Parents_Were_Strong);
                                    cardsThatMayNotDeployFilter = Filters.and(Filters.Jedi, Filters.except(Icon.EPISODE_VII));
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
                                            new MayNotPlayModifier(self, cardsThatMayNotDeployFilter, self.getOwner()), "")
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
    protected List<TopLevelGameTextAction> getGameTextTopLevelActions(String playerId, SwccgGame game, PhysicalCard self, int gameTextSourceCardId) {
        List<TopLevelGameTextAction> actions = new ArrayList<>();
        //May deploy Anakin’s Lightsaber from Reserve Deck (reshuffle) or Lost Pile (lose 1 Force from Life Force)
        GameTextActionId gameTextActionId = GameTextActionId.THE_FORCE_IS_STRONG_IN_MY_FAMILY__DOWNLOAD_ANAKINS_SABER;

        if (GameConditions.canDeployCardFromReserveDeck(game, playerId, self, gameTextActionId, false, Persona.ANAKINS_LIGHTSABER)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Anakin’s Lightsaber from Reserve Deck");
            action.setActionMsg("Deploy Anakin’s Lightsaber from Reserve Deck");
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromReserveDeckEffect(action, Filters.persona(Persona.ANAKINS_LIGHTSABER), true));
            actions.add(action);
        }

        //May deploy Anakin’s Lightsaber from Lost Pile (lose 1 Force from Life Force)
        if (GameConditions.canDeployCardFromLostPile(game, playerId, self, gameTextActionId, false, Persona.ANAKINS_LIGHTSABER)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Deploy Anakin’s Lightsaber from Lost Pile");
            action.setActionMsg("Deploy Anakin’s Lightsaber from Lost Pile");
            // Pay Costs
            action.appendCost(
                    new LoseForceEffect(action, playerId, 1, true, true)
            );
            // Perform result(s)
            action.appendEffect(
                    new DeployCardFromLostPileEffect(action, Filters.persona(Persona.ANAKINS_LIGHTSABER), false));
            actions.add(action);
        }

        //If Anakin's Lightsaber present during a battle at Lars’ Moisture Farm, may retrieve 1 Force.
        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_1;

        if (GameConditions.isDuringBattleWithParticipant(game, Filters.and(Filters.persona(Persona.ANAKINS_LIGHTSABER)))
                && GameConditions.isDuringBattleAt(game, Filters.Lars_Moisture_Farm)
                && GameConditions.isOncePerBattle(game, self, gameTextSourceCardId, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve 1 Force");
            action.setActionMsg("Retrieve 1 Force");
            // Add Usages
            action.appendUsage(
                    new OncePerBattleEffect(action)
            );
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(self, action, playerId, 1));
            actions.add(action);
        }

        //If Anakin's Lightsaber present during a Force drain at Lars’ Moisture Farm, may retrieve 1 Force.
        gameTextActionId = GameTextActionId.OTHER_CARD_ACTION_2;

        if (GameConditions.isDuringForceDrainAt(game, Filters.and(Filters.Lars_Moisture_Farm))
                && GameConditions.canSpot(game, self, Filters.and(Filters.persona(Persona.ANAKINS_LIGHTSABER), Filters.at(Filters.Lars_Moisture_Farm)))
                && GameConditions.isOncePerForceDrain(game, self, gameTextActionId)) {
            final TopLevelGameTextAction action = new TopLevelGameTextAction(self, gameTextSourceCardId, gameTextActionId);
            action.setText("Retrieve 1 Force");
            action.setActionMsg("Retrieve 1 Force");
            // Add Usages
            action.appendUsage(
                    new OncePerForceDrainEffect(action)
            );
            // Perform result(s)
            action.appendEffect(
                    new RetrieveForceEffect(self, action, playerId, 1));
            actions.add(action);
        }

        return actions;
    }

    @Override
    public String getDisplayableInformation(SwccgGame game, PhysicalCard self) {
        return "Chosen option: " + self.getWhileInPlayData().getTextValue();
    }
}